package net.decimation.fixes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScopeZoom's learned sight positions, kept between launches in config/deciworldgen_sights.txt (user, 21 Oktober 2026:
 * "the scope aiming still need more optimizing cause sometime it has delay on it"). ScopeZoom holds the zoom at 1.5x on
 * the first aim of each gun, sight and window shape until it has measured where the sight sits (dev test aimdelay: the
 * ACOG reached 95 % of its zoom after 431 ms, 187 ms once learned); the positions lived for the session only, so every
 * launch waited again. Lines "gun|sight|aspect x y"; only learned ones are kept; a file of another build is ignored
 * (models and sight offsets may have moved). Plain Java: unit tests load it without Minecraft.
 */
public final class SightMemory
{
    private SightMemory()
    {
    }

    /** The learned entries (n >= learn) as file lines, the build first. */
    static List<String> format(Map<String, double[]> sights, String build, int learn)
    {
        List<String> out = new ArrayList<String>();
        out.add("build " + build);
        for (Map.Entry<String, double[]> e : sights.entrySet())
        {
            double[] s = e.getValue();
            if (s[2] >= learn && !e.getKey().contains(" "))
            {
                out.add(e.getKey() + " " + s[0] + " " + s[1]);
            }
        }
        return out;
    }

    /** File lines to sight positions (counted as just learned); empty for another build. */
    static Map<String, double[]> parse(List<String> lines, String build, int learn)
    {
        Map<String, double[]> out = new LinkedHashMap<String, double[]>();
        if (lines.isEmpty() || !lines.get(0).trim().equals("build " + build))
        {
            return out;
        }
        for (String line : lines.subList(1, lines.size()))
        {
            String[] f = line.trim().split(" ");
            if (f.length != 3)
            {
                continue;
            }
            try
            {
                out.put(f[0], new double[] {Double.parseDouble(f[1]), Double.parseDouble(f[2]), learn});
            }
            catch (NumberFormatException e)
            {
                // a broken line: that sight is learned again
            }
        }
        return out;
    }

    public static Map<String, double[]> load(File file, String build, int learn)
    {
        try
        {
            return file.isFile() ? parse(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8), build, learn)
                                 : new LinkedHashMap<String, double[]>();
        }
        catch (Exception e)
        {
            return new LinkedHashMap<String, double[]>();
        }
    }

    public static void save(File file, Map<String, double[]> sights, String build, int learn)
    {
        try
        {
            Files.write(file.toPath(), format(sights, build, learn), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            // not kept: the next launch learns again, as before
        }
    }
}
