package net.decimation.optimized;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * config/deciremnant_optimized.properties: one switch per module, written with the defaults on the first start. Read
 * by the mixin plugin, so a module that is off is not even patched in. Dev tests (-Ddeciworldgen.autotest=true) run
 * in a window that is often unfocused and many leave a world expecting the save done when loadWorld returns, so
 * Dynamic FPS and FastQuit stay off for them (a test forces one on with its system property).
 */
public final class OptimizedConfig
{
    private static Properties props;

    private OptimizedConfig()
    {
    }

    /** A module switch (entityculling, dynamicfps, ...); a system property deciremnant.optimized.<name> overrides. */
    public static boolean on(String module)
    {
        String forced = System.getProperty("deciremnant.optimized." + module);
        if (forced != null)
        {
            return Boolean.parseBoolean(forced);
        }
        if ((module.equals("dynamicfps") || module.equals("fastquit")) && Boolean.getBoolean("deciworldgen.autotest"))
        {
            return false;
        }
        return Boolean.parseBoolean(load().getProperty(module, "true"));
    }

    /** A number setting, its default written to the file when missing. */
    public static int number(String key, int fallback)
    {
        return Integer.parseInt(load().getProperty(key, String.valueOf(fallback)).trim());
    }

    private static synchronized Properties load()
    {
        if (props != null)
        {
            return props;
        }
        props = new Properties();
        props.setProperty("entityculling", "true");
        props.setProperty("entityculling.blockEntityDistance", "64");
        props.setProperty("chunkvbo", "true");
        props.setProperty("dynamicfps", "true");
        props.setProperty("dynamicfps.unfocusedFps", "15");
        props.setProperty("dynamicfps.hiddenFps", "1");
        props.setProperty("fastquit", "true");
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "deciremnant_optimized.properties");
        try
        {
            if (file.isFile())
            {
                InputStream in = new FileInputStream(file);
                props.load(in);
                in.close();
            }
            else
            {
                file.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(file);
                props.store(out, "DeciRemnant Optimized: our Fabulously Optimized style modules for 1.7.10 (true / false each)");
                out.close();
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("DeciRemnant Optimized: config " + file + " unreadable", ex);
        }
        return props;
    }
}
