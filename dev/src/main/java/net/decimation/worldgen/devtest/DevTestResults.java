package net.decimation.worldgen.devtest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.DecimationWorldGen;

/**
 * Compact results of a dev test launch: run/client/devtest/results.txt, one
 * line per value, "mode  key  value  [PASS|FAIL expected ...]", plus the
 * screenshots taken ("mode  shot  file"). Written as it goes (a crash still
 * leaves what was measured) and also logged as AUTOTEST lines.
 * tools/devtest.py prints it and builds one contact sheet per mode.
 */
public final class DevTestResults
{
    private static File file;

    private DevTestResults()
    {
    }

    /** Starts a fresh results file (once per launch). */
    public static synchronized void start(File gameDir)
    {
        File dir = new File(gameDir, "devtest");
        dir.mkdirs();
        file = new File(dir, "results.txt");
        file.delete();
        line("# dev test results " + new java.util.Date());
    }

    /** A measured value, no expectation. */
    public static void value(String mode, String key, Object value)
    {
        line(String.format("%-8s %-28s %s", mode, key, value));
    }

    /** A checked value: PASS / FAIL with the expectation. */
    public static void check(String mode, String key, Object value, boolean pass, String expected)
    {
        line(String.format("%-8s %-28s %-14s %s (%s)", mode, key, value, pass ? "PASS" : "FAIL", expected));
    }

    public static void shot(String mode, String name)
    {
        line(String.format("%-8s %-28s %s", mode, "shot", name));
    }

    private static synchronized void line(String s)
    {
        FMLLog.info("[%s] AUTOTEST %s", DecimationWorldGen.MODID, s);
        if (file == null)
        {
            return;
        }
        try
        {
            Writer w = new FileWriter(file, true);
            w.write(s + "\n");
            w.close();
        }
        catch (IOException e)
        {
            FMLLog.info("[%s] AUTOTEST cannot write %s: %s", DecimationWorldGen.MODID, file, e);
        }
    }
}
