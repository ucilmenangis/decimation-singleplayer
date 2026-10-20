package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Closes the game without breaking the world it is in. mc.shutdown() from inside a world let the integrated server's
 * save and the shutdown hook's save write level.dat at the same moment (11 Oktober 2026: a level.dat that missed 603 item
 * ids, the next start failed; DevAutoTest.quitSafely). Since 21 Oktober 2026 the update can be started from the Esc
 * menu inside a world (UpdatePrompt), so the update screens' Quit game comes here: leave the world as Disconnect does,
 * wait until the integrated server has saved and stopped (60 s at most), then close. Client only (FML bus).
 */
public class SafeQuit
{
    private static volatile boolean asked, reached;
    private static IntegratedServer stopping;
    private static int ticks;

    /** Leave the world (if any), then close the game once it is saved. */
    public static void quit()
    {
        Minecraft mc = Minecraft.getMinecraft();
        asked = true;
        reached = false;
        ticks = 0;
        stopping = null;
        if (mc.theWorld != null)
        {
            stopping = mc.getIntegratedServer();
            FMLLog.info("[deciworldgen] quit: leaving the world before closing");
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld((net.minecraft.client.multiplayer.WorldClient) null);
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || !asked)
        {
            return;
        }
        if (stopping != null && !stopping.isServerStopped() && ticks++ < 20 * 60)
        {
            return;                                       // still saving
        }
        asked = false;
        reached = true;
        if (Boolean.getBoolean("deciworldgen.update.noquit"))
        {
            return;                                       // dev test: stop here, the game stays open
        }
        Minecraft.getMinecraft().shutdown();
    }

    /** True once a quit got to the point of closing the game (dev tests). */
    public static boolean reached()
    {
        return reached;
    }

    /** Forget a quit (dev tests). */
    public static void reset()
    {
        asked = false;
        reached = false;
        stopping = null;
    }
}
