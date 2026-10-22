package net.decimation.worldgen.devtest;

import cpw.mods.fml.client.FMLClientHandler;
import net.decimation.fixes.Deci;
import net.decimation.fixes.RadiationDose;
import net.decimation.fixes.RadiationHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Our channel (fixes/Net) off the integrated server: the client joins a dedicated server (-Ptest server=HOST:PORT,
 * default localhost:25577; an operator there) and checks both directions. Client to server: a magazine with 20 rounds
 * and 5 loose rounds by /give, one sneaking right click as the mouse makes it: 19 and 6 as the server sends them back
 * (MagUnload). Server to client: a radiation zone made with Decimation's own commands around the player (/deci pos1,
 * pos2, /zone create radiation; removed at the end), survival without a mask for 10 s: Decimation's irradiated flag on,
 * the HUD's dose (RadiationDose, received by message) up to about half, drawn whole on screen; out of the zone it falls.
 * Client only.
 */
public class ChannelRemoteTest extends DevTestMode
{
    public String name() { return "channelmp"; }

    private final String host = System.getProperty("deciworldgen.autotest.server", "localhost:25577");
    private int step, ticks, mark;
    private long framesBefore;

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
                    FMLClientHandler.instance().connectToServer(parent, new ServerData("channel test", host));
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
            case 2: // a magazine and loose rounds, given in creative (a dedicated server's Decimation lets only creative
                    // players pick up what /give drops), then survival with the magazine in hand
                String me = mc.thePlayer.getCommandSenderName();
                if (ticks == 100) mc.thePlayer.sendChatMessage("/gamemode 1");
                if (ticks == 110) mc.thePlayer.sendChatMessage("/clear");
                if (ticks == 120) mc.thePlayer.sendChatMessage("/give " + me + " deci:ak74Mag 1 0 {ammo:20}");
                if (ticks == 130) mc.thePlayer.sendChatMessage("/give " + me + " deci:5_45x39mm 5");
                if (ticks == 160) mc.thePlayer.sendChatMessage("/gamemode 0");
                if (ticks == 170)
                {
                    mc.thePlayer.inventory.currentItem = slotOf(mc, "deci:ak74Mag");
                    DevTestResults.value(name(), "before the unload", rounds(mc) + " in the magazine, " + loose(mc) + " loose");
                }
                if (ticks == 180) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                if (ticks == 186) DevTestUtil.rightClick(mc);
                if (ticks == 196) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                if (ticks == 230)
                {
                    int r = rounds(mc), l = loose(mc);
                    DevTestResults.check(name(), "unload on a dedicated server: one round out", r + " in the magazine, " + l + " loose",
                        r == 19 && l == 6, "19 in the magazine, 6 loose");
                    next();
                }
                break;
            case 3: // a radiation zone around the player with Decimation's commands
                if (ticks == 1) mc.thePlayer.sendChatMessage("/gamemode 1");
                if (ticks == 10) mc.thePlayer.sendChatMessage("/tp ~-3 ~-1 ~-3");
                if (ticks == 20) mc.thePlayer.sendChatMessage("/deci pos1");
                if (ticks == 30) mc.thePlayer.sendChatMessage("/tp ~6 ~5 ~6");
                if (ticks == 40) mc.thePlayer.sendChatMessage("/deci pos2");
                if (ticks == 50) mc.thePlayer.sendChatMessage("/tp ~-3 ~-4 ~-3");
                if (ticks == 60) mc.thePlayer.sendChatMessage("/zone create radiation");
                if (ticks == 70) mc.thePlayer.sendChatMessage("/deci heal");   // full health: the zone hurts about 1 a second
                if (ticks == 80)
                {
                    mc.thePlayer.sendChatMessage("/gamemode 0");
                    framesBefore = RadiationHud.framesDrawn();
                }
                if (ticks == 290)
                {
                    int pct = RadiationDose.shown();
                    boolean flag = Deci.player(mc.thePlayer).irradiated();
                    DevTestResults.check(name(), "Decimation's irradiated flag on (server's zone tick, synced)", flag, flag, "true");
                    DevTestResults.check(name(), "the HUD's dose after 10 s without a mask", pct + " %", pct >= 30 && pct <= 60,
                        "30 to 60 % (about half; the zone starts a moment after survival)");
                    DevTestResults.check(name(), "the dose icon drawn whole", RadiationHud.lastPlace() + ", "
                        + (RadiationHud.framesDrawn() - framesBefore) + " frames", RadiationHud.lastOnScreen()
                        && RadiationHud.framesDrawn() - framesBefore > 50, "on screen, drawn every frame");
                    DevTestUtil.screenshot(mc, name(), "dose_on_server");
                    mc.thePlayer.sendChatMessage("/gamemode 1");
                }
                if (ticks == 300) mc.thePlayer.sendChatMessage("/zone remove");
                if (ticks == 310) mc.thePlayer.sendChatMessage("/tp ~12 ~ ~");
                if (ticks == 330)
                {
                    mc.thePlayer.sendChatMessage("/gamemode 0");
                    mark = RadiationDose.shown();
                }
                if (ticks == 530)
                {
                    int now = RadiationDose.shown();
                    boolean flag = Deci.player(mc.thePlayer).irradiated();
                    DevTestResults.check(name(), "out of the zone (removed) the dose falls slowly", mark + " % then " + now
                        + " % in 10 s, irradiated " + flag, now <= mark && now >= mark - 2 && !flag, "1 % less or so, not irradiated");
                    mc.thePlayer.sendChatMessage("/gamemode 1");
                    mc.thePlayer.sendChatMessage("/clear");
                }
                if (ticks == 560)
                {
                    return end(mc);
                }
                break;
            default:
                return end(mc);
        }
        return true;
    }

    private static int slotOf(Minecraft mc, String name)
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && name.equals(Item.itemRegistry.getNameForObject(s.getItem())))
            {
                return i;
            }
        }
        return 0;
    }

    private static int rounds(Minecraft mc)
    {
        ItemStack s = mc.thePlayer.inventory.getStackInSlot(slotOf(mc, "deci:ak74Mag"));
        return s == null || !s.hasTagCompound() ? -1 : s.getTagCompound().getInteger("ammo");
    }

    private static int loose(Minecraft mc)
    {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory)
        {
            n += s != null && "deci:5_45x39mm".equals(Item.itemRegistry.getNameForObject(s.getItem())) ? s.stackSize : 0;
        }
        return n;
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
