package net.decimation.worldgen.devtest;

import java.util.List;

import cpw.mods.fml.client.FMLClientHandler;
import net.decimation.fixes.Deci;
import net.decimation.fixes.gunsmith.GunSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.item.ItemStack;

/**
 * The new attachments off the integrated server (plan 2026-10-21-new-attachments, Task 7): the client joins a dedicated
 * server (-Ptest server=HOST:PORT, default localhost:25577; an operator there for /give), fits the RK-1 and the
 * SpecterDR on an M4A4 through the Gunsmith screen's clicks and reads them as the server sent them back, then the recoil
 * ratios (AttachmentsTest.recoilChecks: Decimation's clientHeldTick on the joined client), the G switch, and one real
 * shot (a round less, as the server counts it). Client only.
 */
public class AttachmentsRemoteTest extends DevTestMode
{
    public String name() { return "attachmentsmp"; }

    private final String host = System.getProperty("deciworldgen.autotest.server", "localhost:25577");
    private int step, ticks, ammo0, clicks0;

    public boolean client(Minecraft mc)
    {
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
                    FMLClientHandler.instance().connectToServer(parent, new ServerData("attachments test", host));
                }
                if (mc.currentScreen instanceof GuiDisconnected)
                {
                    DevTestResults.check(name(), "joined " + host, "refused", false, "in the server's world");
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
            case 2: // items by commands, given in creative (a dedicated server's Decimation lets only creative players
                    // pick up what /give drops), a loaded M4A4 in hand
                String me = mc.thePlayer.getCommandSenderName();
                if (ticks == 100) mc.thePlayer.sendChatMessage("/gamemode 1");
                if (ticks == 110) mc.thePlayer.sendChatMessage("/clear");
                if (ticks == 120) mc.thePlayer.sendChatMessage("/give " + me + " deci:m4a4 1 0 {ammo:30}");
                if (ticks == 130) mc.thePlayer.sendChatMessage("/give " + me + " deci:rk1 1");
                if (ticks == 140) mc.thePlayer.sendChatMessage("/give " + me + " deci:specterdr 1");
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
                    GunsmithTest.pressAttach(mc);
                }
                if (ticks == 260)
                {
                    String s = GunsmithTest.screenName(mc);
                    DevTestResults.check(name(), "the attach key opens the Gunsmith screen", s, "GunsmithScreen".equals(s), "GunsmithScreen");
                    next();
                }
                break;
            case 3: // fit the RK-1, then the SpecterDR, from the lists, the way a mouse clicks
                if (ticks == 1) GunsmithTest.clickBox(mc, GunSlot.GRIP);
                if (ticks == 6) pick(mc, "deci:rk1");
                if (ticks == 40)
                {
                    String g = part(mc, GunSlot.GRIP);
                    DevTestResults.check(name(), "RK-1 fit, as the server sent it back", g, "rk1".equals(g), "gripAttach rk1");
                    GunsmithTest.clickBox(mc, GunSlot.SIGHT);
                }
                if (ticks == 45) pick(mc, "deci:specterdr");
                if (ticks == 80)
                {
                    String g = part(mc, GunSlot.SIGHT);
                    DevTestResults.check(name(), "SpecterDR fit, as the server sent it back", g, "specterdr".equals(g), "sightAttach specterdr");
                    GunsmithTest.typeAttach(mc);
                }
                if (ticks == 100)
                {
                    DevTestResults.check(name(), "the attach key closes it", GunsmithTest.screenName(mc), mc.currentScreen == null, "closed");
                    next();
                }
                break;
            case 4: // recoil on the joined client, the SpecterDR's switch
                if (ticks == 5)
                {
                    AttachmentsTest.recoilChecks(name(), mc, mc.thePlayer.getHeldItem());
                    float m = net.decimation.fixes.ScopeZoom.magnificationFor(mc.thePlayer.getHeldItem());
                    clicks0 = net.decimation.fixes.SpecterKey.clicks();
                    DevTestResults.check(name(), "SpecterDR at 4x", String.format("%.2f", m), Math.abs(m - 4f) < 0.01f, "4.00");
                    AttachmentsTest.press();
                }
                if (ticks == 10)
                {
                    float m = net.decimation.fixes.ScopeZoom.magnificationFor(mc.thePlayer.getHeldItem());
                    int clicks = net.decimation.fixes.SpecterKey.clicks() - clicks0;
                    DevTestResults.check(name(), "the key switches it to 1x", String.format("%.2f, %d click", m, clicks),
                                         Math.abs(m - 1.25f) < 0.01f && clicks == 1, "1.25, 1 click");
                    AttachmentsTest.press();
                }
                if (ticks == 15)
                {
                    next();
                }
                break;
            case 5: // one real shot, at the sky
                if (ticks == 1)
                {
                    mc.thePlayer.rotationPitch = -40;
                    ItemStack held = mc.thePlayer.getHeldItem();
                    ammo0 = held == null || held.stackTagCompound == null ? -1 : held.stackTagCompound.getInteger("ammo");
                    boolean fired = Deci.fireHeldGun(mc.thePlayer);
                    DevTestResults.value(name(), "shot", "fired " + fired + ", " + ammo0 + " rounds before");
                }
                if (ticks == 60)
                {
                    ItemStack held = mc.thePlayer.getHeldItem();
                    int ammo = held == null || held.stackTagCompound == null ? -1 : held.stackTagCompound.getInteger("ammo");
                    String grip = part(mc, GunSlot.GRIP), sight = part(mc, GunSlot.SIGHT);
                    DevTestResults.check(name(), "a shot with both fitted", String.format("%d -> %d rounds, %s, %s", ammo0, ammo, grip, sight),
                                         ammo0 > 0 && ammo == ammo0 - 1 && "rk1".equals(grip) && "specterdr".equals(sight),
                                         "one round less, rk1, specterdr");
                    return end(mc);
                }
                break;
            default:
                return end(mc);
        }
        return true;
    }

    /** Clicks the list row of this item (the list's first line is its title, rows follow). */
    private void pick(Minecraft mc, String item)
    {
        List<String> lines = GunsmithTest.lines(mc);
        String n = GunsmithTest.name(item);
        DevTestResults.check(name(), "the list offers " + n, String.join(" | ", lines), lines.contains(n), n);
        GunsmithTest.clickRow(mc, lines.indexOf(n) - 1);
    }

    private static String part(Minecraft mc, GunSlot slot)
    {
        ItemStack h = mc.thePlayer.getHeldItem();
        return h != null && h.hasTagCompound() ? h.getTagCompound().getString(slot.nbtKey) : "";
    }

    private void next()
    {
        step++;
        ticks = 0;
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
