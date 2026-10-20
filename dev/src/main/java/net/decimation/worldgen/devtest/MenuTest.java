package net.decimation.worldgen.devtest;

import java.io.File;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;

/**
 * Decimation's home menu never asks for an update (tools/patches/PatchNoUpdate.java, bug.md): run it
 * with mods/DecimationVoiceChat.jar moved aside to see what a player without the voice chat jar gets.
 * Records whether the jar is there and what the update check answers (OFF in a dev or test build: only releases look
 * for updates), and photographs the home menu (menu_home: "Play", not "Update").
 */
public class MenuTest extends DevTestMode
{
    public String name() { return "menu"; }

    private int frames;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (frames == 0)
        {
            boolean voice = new File("mods/DecimationVoiceChat.jar").exists();
            DevTestResults.value(name(), "mods/DecimationVoiceChat.jar present", voice);
            DevTestResults.check(name(), "update required", Deci.updateRequired(), !Deci.updateRequired(), "false");
            // only release builds look for updates (fixes/Updater.offersUpdates, user 20 Oktober 2026): the dev client and test
            // jars built from main or prerelease stay OFF, so a main test jar never takes a pre-release for an update
            if (net.decimation.fixes.Updater.state() == net.decimation.fixes.Updater.State.IDLE)
            {
                net.decimation.fixes.Updater.check();     // an update test before this one reset it
            }
            boolean looks = net.decimation.fixes.Updater.offersUpdates();
            net.decimation.fixes.Updater.State s = net.decimation.fixes.Updater.state();
            DevTestResults.check(name(), "update check", s + " (" + net.decimation.fixes.Updater.message() + ")",
                                 looks == (s != net.decimation.fixes.Updater.State.OFF),
                                 looks ? "a release build: it asks GitHub" : "OFF: a dev or test build never offers an update");
            // the jar an update replaces (fixes/Updater.target): the running jar in a shipped game, none in a dev folder
            boolean dev = Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"));
            File target = net.decimation.fixes.Updater.target();
            DevTestResults.check(name(), "update target", target == null ? "none" : target.getPath(),
                                 dev ? target == null : target != null && target.isFile() && target.getName().endsWith(".jar"),
                                 dev ? "none (dev folder)" : "the running jar (mods/Decimation.jar)");
            // our logo in Decimation's banners (tools/menu_assets.py logos(): twice the old size; user 17 Oktober 2026)
            DevTestResults.check(name(), "menu logo is ours", texture(mc, "banner") + ", " + texture(mc, "banner_intro"),
                                 "922 x 202, 1024 x 480".equals(texture(mc, "banner") + ", " + texture(mc, "banner_intro")),
                                 "922 x 202, 1024 x 480 (Decimation's: 461 x 101, 512 x 240)");
            net.decimation.worldgen.DevAutoTest.keepScreen = true;
            mc.displayGuiScreen(Deci.homeMenu());
        }
        if (++frames == 60)
        {
            DevTestUtil.screenshot(mc, name(), "menu_home");
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            mc.displayGuiScreen(null);
            done = true;
        }
        return !done;
    }

    /** Size of a texture of Decimation's menu as the game loads it ("w x h"). */
    private static String texture(Minecraft mc, String name)
    {
        try
        {
            java.io.InputStream in = mc.getResourceManager().getResource(
                new net.minecraft.util.ResourceLocation("deci", "textures/gui/menu/" + name + ".png")).getInputStream();
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(in);
            in.close();
            return im.getWidth() + " x " + im.getHeight();
        }
        catch (Exception e)
        {
            return e.toString();
        }
    }
}
