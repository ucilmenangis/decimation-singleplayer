package net.decimation.fixes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sights need a mount (user, 22 Oktober 2026: "same rule applied on sight too. for red dot i think its better to put only
 * on pistol ... cause its very small"; docs/references/grip_rails.md section 6). Decimation fits every sight on every gun
 * (category all). Now Decimation's small red dot goes only on a pistol with an optic cut or rail on its slide, and every
 * other sight only on a gun with a top rail (or HK claw mount points) or an AK / SVD style side mount. The list is the
 * shipped sight_mounts.txt ("gun rail|side|slide"); a gun not listed takes no sight. Asked by the Gunsmith's fit check,
 * NPC loadouts and fixes/MountStrip (a sight on a gun without its mount comes off). Plain Java apart from the class path
 * read: unit tests load it without Minecraft.
 */
public final class SightMounts
{
    private static final String TABLE = "/assets/deciworldgen/sight_mounts.txt";
    private static volatile Map<String, String> mounts;

    private SightMounts()
    {
    }

    /** Whether this sight may go on this gun (registry names). */
    public static boolean fits(String gun, String sight)
    {
        String m = table().get(gun);
        if (m == null || sight == null || sight.isEmpty())
        {
            return false;
        }
        return "reddot".equals(sight) ? "slide".equals(m) : "rail".equals(m) || "side".equals(m);
    }

    /** The guns with this mount (unit tests). */
    static Set<String> guns(String mount)
    {
        Set<String> out = new TreeSet<String>();
        for (Map.Entry<String, String> e : table().entrySet())
        {
            if (e.getValue().equals(mount))
            {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** Reads the table now (startup); later calls use it. */
    public static void load()
    {
        mounts = read();
    }

    private static Map<String, String> table()
    {
        Map<String, String> t = mounts;
        if (t == null)
        {
            t = mounts = read();
        }
        return t;
    }

    private static Map<String, String> read()
    {
        Map<String, String> out = new HashMap<String, String>();
        try
        {
            InputStream in = SightMounts.class.getResourceAsStream(TABLE);
            if (in != null)
            {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                for (String line; (line = r.readLine()) != null; )
                {
                    String[] f = line.trim().split("\\s+");
                    if (f.length == 2 && !line.trim().startsWith("#"))
                    {
                        out.put(f[0], f[1]);
                    }
                }
                r.close();
            }
        }
        catch (Exception e)
        {
            // no table: no sight fits anywhere
        }
        return out;
    }
}
