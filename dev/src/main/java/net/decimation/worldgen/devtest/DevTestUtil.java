package net.decimation.worldgen.devtest;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Small helpers shared by the dev test modes. */
public final class DevTestUtil
{
    private DevTestUtil()
    {
    }

    /** The singleplayer server's player, or null. */
    public static EntityPlayerMP player()
    {
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.getConfigurationManager() == null || s.getConfigurationManager().playerEntityList.isEmpty())
        {
            return null;
        }
        return (EntityPlayerMP) s.getConfigurationManager().playerEntityList.get(0);
    }

    /** Current fps from the debug line ("N fps, M chunk updates"). */
    public static int fps(Minecraft mc)
    {
        String dbg = mc.debug;
        return Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
    }

    /** Unlimited frame rate, no vsync (fps measurements). */
    public static void unlimitedFps(Minecraft mc)
    {
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        org.lwjgl.opengl.Display.setVSyncEnabled(false);
    }

    /** Saves screenshots/<name>.png and records it in the results. */
    public static void screenshot(Minecraft mc, String mode, String name)
    {
        net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight,
                                                           mc.getFramebuffer());
        DevTestResults.shot(mode, name);
    }

    public static void deleteRecursive(File f)
    {
        File[] kids = f.listFiles();
        if (kids != null)
        {
            for (File k : kids)
            {
                deleteRecursive(k);
            }
        }
        f.delete();
    }
}
