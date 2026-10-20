package net.decimation.worldgen.devtest;

import java.util.List;

import cpw.mods.fml.client.FMLClientHandler;
import net.decimation.fixes.gunsmith.GunSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

/**
 * The Gunsmith screen off the integrated server (plan 2026-10-21-gunsmith-screen, Task 6, Review Focus 5): the client
 * joins a dedicated server (-Ptest server=HOST:PORT, default localhost:25577; the player must be an operator there for
 * /give), then opens the screen with the attach key, fits a 4x from the list and takes it off by clicks the way a mouse
 * makes them, reads the gun's parts as the server sent them back, closes with the key, and times frames closed and
 * open. Client only: a joined client has no integrated server to look into.
 */
public class GunsmithRemoteTest extends DevTestMode
{
    public String name() { return "gunsmithmp"; }

    private final String host = System.getProperty("deciworldgen.autotest.server", "localhost:25577");
    private int step, ticks;
    private final GunsmithTest.Clock clock = new GunsmithTest.Clock();
    private boolean clockOn;
    private double closedMs;

    public boolean client(Minecraft mc)
    {
        if (!clockOn)
        {
            clockOn = true;
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(clock);
        }
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        ticks++;
        switch (step)
        {
            case 0: // leave a singleplayer world for the menu
                if (mc.theWorld != null && mc.isSingleplayer())
                {
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    mc.displayGuiScreen(new GuiMainMenu());
                }
                if (mc.theWorld == null && ticks > 40)
                {
                    next();
                }
                break;
            case 1: // join
                if (ticks == 1)
                {
                    GuiMultiplayer parent = new GuiMultiplayer(new GuiMainMenu());
                    mc.displayGuiScreen(parent);
                    FMLClientHandler.instance().setupServerList();
                    FMLClientHandler.instance().connectToServer(parent, new ServerData("gunsmith test", host));
                }
                if (mc.currentScreen instanceof GuiDisconnected)
                {
                    DevTestResults.check(name(), "joined " + host, "refused: " + reason((GuiDisconnected) mc.currentScreen), false,
                                         "in the server's world");
                    return end(mc);
                }
                if (mc.theWorld != null && mc.thePlayer != null && !mc.isSingleplayer())
                {
                    DevTestResults.check(name(), "joined " + host, "in the world", true, "in the server's world");
                    next();
                }
                if (ticks > 1200)
                {
                    DevTestResults.check(name(), "joined " + host, "no world after 60 s", false, "in the server's world");
                    return end(mc);
                }
                break;
            case 2: // items by commands (an operator), given in creative: a dedicated server's Decimation only lets
                    // creative players pick items up (ItemPickupHandler), and /give drops the item at the player
                String me = mc.thePlayer.getCommandSenderName();
                if (ticks == 100) mc.thePlayer.sendChatMessage("/gamemode 1");
                if (ticks == 110) mc.thePlayer.sendChatMessage("/clear");
                if (ticks == 120) mc.thePlayer.sendChatMessage("/give " + me + " deci:m4a4 1");
                if (ticks == 130) mc.thePlayer.sendChatMessage("/give " + me + " deci:4x 1");
                if (ticks == 140) mc.thePlayer.sendChatMessage("/give " + me + " deci:reddot 1");
                if (ticks == 170) mc.thePlayer.sendChatMessage("/gamemode 0");
                if (ticks == 180)
                {
                    for (int i = 0; i < 9; i++)
                    {
                        ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
                        if (s != null && "deci:m4a4".equals(net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem())))
                        {
                            mc.thePlayer.inventory.currentItem = i;
                        }
                    }
                }
                if (ticks == 200)
                {
                    ItemStack held = mc.thePlayer.getHeldItem();
                    DevTestResults.value(name(), "holding", held == null ? "nothing" : held.getDisplayName());
                    GunsmithTest.pressAttach(mc);
                }
                if (ticks == 260)
                {
                    String s = GunsmithTest.screenName(mc);
                    DevTestResults.check(name(), "the attach key opens the Gunsmith screen", s, "GunsmithScreen".equals(s), "GunsmithScreen");
                    next();
                }
                break;
            case 3: // fit the 4x from the list, the way a mouse clicks
                if (ticks == 1) GunsmithTest.clickBox(mc, GunSlot.SIGHT);
                if (ticks == 6)
                {
                    List<String> lines = GunsmithTest.lines(mc);
                    String fourX = GunsmithTest.name("deci:4x");
                    DevTestResults.check(name(), "list of what fits", String.join(" | ", lines), lines.contains(fourX), fourX);
                    GunsmithTest.clickRow(mc, lines.indexOf(fourX) - 1);
                }
                if (ticks == 40)
                {
                    DevTestResults.check(name(), "fit, as the server sent it back", sight(mc), "4x".equals(sight(mc)), "sightAttach 4x");
                    GunsmithTest.clickBox(mc, GunSlot.SIGHT);
                }
                if (ticks == 45)
                {
                    GunsmithTest.clickRow(mc, 0); // Take off
                }
                if (ticks == 80)
                {
                    DevTestResults.check(name(), "take off, as the server sent it back", sight(mc), "".equals(sight(mc)), "sightAttach empty");
                    GunsmithTest.typeAttach(mc);
                }
                if (ticks == 100)
                {
                    DevTestResults.check(name(), "the attach key closes it", GunsmithTest.screenName(mc), mc.currentScreen == null, "closed");
                    int loose = mc.theWorld.getEntitiesWithinAABB(EntityItem.class, mc.thePlayer.boundingBox.expand(4, 4, 4)).size();
                    DevTestResults.check(name(), "closing drops nothing", loose, loose == 0, "0 items on the ground");
                    DevTestUtil.unlimitedFps(mc);
                    clock.start();
                    next();
                }
                break;
            case 4: // frames closed, then open
                if (ticks > 1 && clock.frames() >= 220 && closedMs == 0)
                {
                    closedMs = clock.stop();
                    GunsmithTest.pressAttach(mc);
                }
                if (closedMs > 0 && mc.currentScreen != null && clock.idle() && ticks > 60)
                {
                    clock.start();
                }
                if (closedMs > 0 && clock.frames() >= 220)
                {
                    double openMs = clock.stop();
                    DevTestResults.check(name(), "frame cost open", String.format("closed %.2f ms, open %.2f ms (median of 200)", closedMs, openMs),
                                         openMs - closedMs <= 1.0, "at most 1 ms more");
                    mc.thePlayer.closeScreen();
                    return end(mc);
                }
                if (ticks > 2400)
                {
                    DevTestResults.check(name(), "frame cost open", "not measured", false, "at most 1 ms more");
                    return end(mc);
                }
                break;
            default:
                return end(mc);
        }
        return true;
    }

    private void next()
    {
        step++;
        ticks = 0;
    }

    private static String sight(Minecraft mc)
    {
        ItemStack h = mc.thePlayer.getHeldItem();
        return h != null && h.hasTagCompound() ? h.getTagCompound().getString(GunSlot.SIGHT.nbtKey) : "";
    }

    /** The text a disconnect screen shows (its message field: SRG in the shipped game, which this test runs in). */
    private static String reason(GuiDisconnected gui)
    {
        for (String n : new String[] {"field_146304_f", "message"})
        {
            try
            {
                java.lang.reflect.Field f = GuiDisconnected.class.getDeclaredField(n);
                f.setAccessible(true);
                Object c = f.get(gui);
                return c instanceof net.minecraft.util.IChatComponent ? ((net.minecraft.util.IChatComponent) c).getUnformattedText() : String.valueOf(c);
            }
            catch (ReflectiveOperationException e)
            {
                // the other name
            }
        }
        return "unknown";
    }

    /** Leaves the server for the menu and ends the mode. */
    private boolean end(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = false;
        if (mc.theWorld != null)
        {
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }
        mc.displayGuiScreen(new GuiMainMenu());
        return false;
    }
}
