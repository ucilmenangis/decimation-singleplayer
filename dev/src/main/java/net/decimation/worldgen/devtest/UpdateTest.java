package net.decimation.worldgen.devtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import net.decimation.fixes.Deci;
import net.decimation.fixes.Updater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * Auto update end to end (fixes/Updater, UpdateProgress, UpdateSwap, PatchNoUpdate v2) against
 * tools/fake_release_server.py (start it first; port from -Pport / port=, default 8765): the check finds
 * the fake release, Decimation's update check answers true, the home menu shows its Update button
 * (update_menu), the button opens Decimation's own progress screen (deci.i.m), the download runs with its
 * bar showing our numbers (update_progress), is checked and ready with Quit game / Later on that screen
 * (update_ready), and the swap helper replaces a stand in jar
 * (run/client/devtest/update_target/Decimation.jar) with the download.
 * real=true: against our real GitHub releases instead, the running version pretended to be `from`
 * (default 0.58.0), expecting `expect` (default 0.59.0): the real jar downloads, is checked against
 * GitHub's sha256 and swapped into the stand in.
 * Fallback (rate limit): the fake server started with LIMITED answers the API with 403 and the check must
 * come from the release feed (`source=feed`); `feedonly=true` points the API at a dead address (works with
 * real=true against GitHub's own feed). A fake run that got its answer from the API asks again and must get
 * "api (not modified)" (ETag, not counted by GitHub). jar=true (with real=true, in the scratchpad production client):
 * the swap replaces the running mods/Decimation.jar itself, found by Updater.target.
 */
public class UpdateTest extends DevTestMode
{
    public String name() { return "update"; }

    private int frames, step, at;
    private boolean progressShot, done;
    private File target;
    private long started;
    private String versionBefore;
    private final boolean real = "true".equals(System.getProperty("deciworldgen.autotest.real"));
    /** jar=true: replace the running jar itself (a shipped game only), not the stand in. */
    private final boolean useJar = "true".equals(System.getProperty("deciworldgen.autotest.jar"));
    private final String expect = prop("expect", real ? "0.59.0" : "9.9.9");   // `expect=` on a live run clears it
    private final boolean feedOnly = "true".equals(System.getProperty("deciworldgen.autotest.feedonly"));
    private final String source = prop("source", feedOnly ? "feed" : "api");

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        frames++;
        Updater.State s = Updater.state();
        switch (step)
        {
            case 0:
                String port = System.getProperty("deciworldgen.autotest.port", "8765");
                if (useJar)
                {
                    // the game's own jar (Updater.target): only in a shipped game, the scratchpad production client
                    target = Updater.target();
                    DevTestResults.check(name(), "the running jar as target", target, target != null, "mods/Decimation.jar");
                    if (target == null)
                    {
                        step = 99;
                        return false;
                    }
                }
                else
                {
                    target = new File(mc.mcDataDir, "devtest/update_target/Decimation.jar");
                    target.getParentFile().mkdirs();
                    write(target, "OLDJAR".getBytes());
                    new File(target.getPath() + ".download").delete();
                    new File(target.getParentFile(), "update.log").delete();
                    System.setProperty("deciworldgen.update.target", target.getPath());
                }
                Updater.reset();
                Updater.forgetCache();
                String dead = "http://127.0.0.1:9/none";
                if (real)
                {
                    cpw.mods.fml.common.ModMetadata m = cpw.mods.fml.common.Loader.instance().getIndexedModList().get("deciworldgen").getMetadata();
                    versionBefore = m.version;
                    m.version = prop("from", "0.58.0");
                    Updater.setApi(feedOnly ? dead : Updater.DEFAULT_API, Updater.DEFAULT_WEB);
                }
                else
                {
                    String base = "http://127.0.0.1:" + port;
                    Updater.setApi(feedOnly ? dead : base + "/api/releases", base + "/web/releases");
                }
                started = System.currentTimeMillis();
                Updater.check();
                next(1);
                break;
            case 1:
                if (s == Updater.State.AVAILABLE || s == Updater.State.NONE || frames - at > 400)
                {
                    DevTestResults.check(name(), "check finds the release", s + " " + Updater.latest() + " (" + Updater.message() + ")",
                                         s == Updater.State.AVAILABLE && expect.equals(Updater.latest()), "AVAILABLE " + expect);
                    DevTestResults.check(name(), "answer from", Updater.source(), source.equals(Updater.source()), source);
                    DevTestResults.check(name(), "Decimation's update check", Deci.updateRequired(), Deci.updateRequired(), "true");
                    if (!real && "api".equals(Updater.source()))
                    {
                        Updater.reset();                   // ask again: the ETag must give a 304
                        Updater.check();
                        next(10);
                    }
                    else
                    {
                        menu(mc, s);
                    }
                }
                break;
            case 10:
                if (s == Updater.State.AVAILABLE || s == Updater.State.NONE || frames - at > 400)
                {
                    DevTestResults.check(name(), "second check not counted", Updater.source() + " " + Updater.latest(),
                                         "api (not modified)".equals(Updater.source()) && expect.equals(Updater.latest()), "api (not modified) " + expect);
                    menu(mc, s);
                }
                break;
            case 2:
                if (frames - at == 40)
                {
                    DevTestUtil.screenshot(mc, name(), "update_menu");
                    GuiButton b = button(mc.currentScreen, 8);
                    DevTestResults.check(name(), "menu has the Update button", b == null ? "none" : b.displayString, b != null, "button 8");
                    if (b != null)
                    {
                        try
                        {
                            DevTestUtil.press(mc.currentScreen, b);       // MCP or SRG name: dev and shipped game
                        }
                        catch (Exception e)
                        {
                            DevTestResults.value(name(), "click", e.toString());
                        }
                    }
                    DevTestResults.check(name(), "Update opens Decimation's progress screen", mc.currentScreen == null ? "none"
                                         : mc.currentScreen.getClass().getName(), Deci.isUpdateProgress(mc.currentScreen),
                                         "GuiUpdateProgress (deci.i.m), user 17 Oktober 2026");
                    next(3);
                }
                break;
            case 3:
                if (!progressShot && s == Updater.State.DOWNLOADING && Updater.downloaded() > Updater.size() / 3)
                {
                    progressShot = true;
                    DevTestUtil.screenshot(mc, name(), "update_progress");
                    String shown = Deci.updateProgress();
                    DevTestResults.check(name(), "progress screen shows our download", shown,
                                         shown.contains("| true |") && !shown.startsWith("0 |"), "a percent above 0, downloading");
                    // the lines as a player reads them (user, 17 Oktober 2026: update screen polish): version and size,
                    // the percent where Decimation's own updater put it, done before total
                    String[] lines = Deci.updateLines();
                    String total = mb(Updater.size()) + "mb";
                    DevTestResults.check(name(), "progress lines", String.join(" | ", lines),
                                         lines[0].equals("Downloading: DeciRemnant " + expect + " (" + total + ")")
                                         && lines[1].matches("Downloading: [1-9][0-9]?%") && lines[2].endsWith("mb/" + total)
                                         && !lines[2].startsWith(total),
                                         "Downloading: DeciRemnant " + expect + " (" + total + ") | Downloading: N% | done/" + total);
                }
                if (s == Updater.State.READY || s == Updater.State.FAILED || System.currentTimeMillis() - started > 20 * 60 * 1000L)
                {
                    DevTestResults.check(name(), "download checked", s + " (" + Updater.message() + ")", s == Updater.State.READY, "READY");
                    DevTestResults.value(name(), "download time", (System.currentTimeMillis() - started) / 1000 + " s for " + (Updater.size() >> 20) + " MB");
                    next(4);
                }
                break;
            case 4:
                if (frames - at == 20)
                {
                    DevTestUtil.screenshot(mc, name(), "update_ready");
                    StringBuilder labels = new StringBuilder();
                    for (GuiButton b : DevTestUtil.buttons(mc.currentScreen))
                    {
                        labels.append(b.visible ? DevTestUtil.label(b) + ";" : "");
                    }
                    DevTestResults.check(name(), "ready: Quit game and Later", Deci.updateProgress() + ", buttons " + labels,
                                         Deci.isUpdateProgress(mc.currentScreen) && labels.toString().contains("Quit game")
                                         && labels.toString().contains("Later") && Deci.updateProgress().contains("| false |"),
                                         "Decimation's screen, safe to close, Quit game and Later");
                    // bug.md "Update screen bar stops short of full": the length Decimation draws for the field we set
                    int gw = new net.minecraft.client.gui.ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
                    int bar = gw / 4 * 2, field = Integer.parseInt(Deci.updateProgress().split(" ")[0]);
                    DevTestResults.check(name(), "ready bar full", (bar / 100 * field) + " of " + bar + " px (GUI width " + gw + ")",
                                         Math.abs(bar / 100 * field - bar) <= Math.max(1, bar / 100), "the whole bar");
                    String[] lines = Deci.updateLines();
                    String total = mb(Updater.size()) + "mb";
                    DevTestResults.check(name(), "ready lines", String.join(" | ", lines),
                                         lines[1].equals("Downloading: Complete!") && lines[2].equals(total + "/" + total),
                                         "Downloading: Complete! | " + total + "/" + total);
                    int code = Updater.swap(true);
                    byte[] head = head(target, 6);
                    String want = real ? "PK" : "NEWJAR";            // a real release is a zip
                    boolean swapped = new String(head).startsWith(want) && target.length() == Updater.size();
                    DevTestResults.check(name(), "swap helper replaced the jar", "exit " + code + ", starts " + new String(head) + ", "
                        + target.length() + " bytes", swapped, want + ", " + Updater.size() + " bytes");
                    next(9);
                }
                break;
            default:
                if (frames - at > 5)
                {
                    mc.displayGuiScreen(null);
                    net.decimation.worldgen.DevAutoTest.keepScreen = false;
                    System.clearProperty("deciworldgen.update.target");
                    if (versionBefore != null)
                    {
                        cpw.mods.fml.common.Loader.instance().getIndexedModList().get("deciworldgen").getMetadata().version = versionBefore;
                    }
                    Updater.reset();
                    done = true;
                }
        }
        return !done;
    }

    /** Megabytes with one decimal, as the screen shows them. */
    private static double mb(long bytes)
    {
        return Math.round(bytes / 104857.6) / 10.0;
    }

    /** A test setting; empty (cleared with `key=` on a live run) counts as unset. */
    private static String prop(String key, String fallback)
    {
        String v = System.getProperty("deciworldgen.autotest." + key);
        return v == null || v.isEmpty() ? fallback : v;
    }

    private void menu(Minecraft mc, Updater.State s)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        mc.displayGuiScreen(Deci.homeMenu());
        next(s == Updater.State.AVAILABLE ? 2 : 9);
    }

    private void next(int s)
    {
        step = s;
        at = frames;
    }

    private static GuiButton button(GuiScreen screen, int id)
    {
        return DevTestUtil.byId(screen, id);            // buttonList by MCP or SRG name
    }

    private static void write(File f, byte[] b)
    {
        try
        {
            FileOutputStream o = new FileOutputStream(f);
            o.write(b);
            o.close();
        }
        catch (Exception e)
        {
            DevTestResults.value("update", "write", e.toString());
        }
    }

    private static byte[] head(File f, int n)
    {
        byte[] b = new byte[n];
        try
        {
            FileInputStream in = new FileInputStream(f);
            int r = in.read(b);
            in.close();
            return r == n ? b : new byte[0];
        }
        catch (Exception e)
        {
            return new byte[0];
        }
    }
}
