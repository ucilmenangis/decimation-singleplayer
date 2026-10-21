package net.decimation.fixes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Foregrips need a rail (user, 21 Oktober 2026: "for RPK and some guns that doesnt have picatiny rail, they dont support
 * any grip on it"; docs/references/grip_rails.md). Decimation fits its Foregrip on every rifle; a vertical grip (its
 * Foregrip, our RK-1) now goes only on a gun with a rail or slots under its handguard. The list is the shipped
 * grip_offsets.txt: tools/guns/gripfit.py writes foregrip rows only for its RAILED rifles, so a gun takes a foregrip
 * exactly where the table places one. Lights (laser, flashlight) keep every gun, as Decimation has them. Both sides:
 * the Gunsmith's fit check, NPC loadouts and the grip taken off a rail-less gun (fixes/GripStrip) ask here. Plain Java
 * apart from the class path read: unit tests load it without Minecraft.
 */
public final class GripRails
{
    private static final String TABLE = "/assets/deciworldgen/grip_offsets.txt";
    private static volatile Set<String> pairs;

    private GripRails()
    {
    }

    /** A vertical grip: it bolts onto a rail under the handguard. */
    public static boolean needsRail(String grip)
    {
        return "foregrip".equals(grip) || "rk1".equals(grip);
    }

    /** Whether this grip may go on this gun (registry names). Lights and anything not a foregrip: always. */
    public static boolean fits(String gun, String grip)
    {
        return !needsRail(grip) || table().contains(gun + " " + grip);
    }

    /** The guns that take this grip (unit tests compare it with the doc's list). */
    static Set<String> railed(String grip)
    {
        Set<String> out = new TreeSet<String>();
        for (String k : table())
        {
            String[] f = k.split(" ");
            if (f[1].equals(grip))
            {
                out.add(f[0]);
            }
        }
        return out;
    }

    /** Reads the table now (startup); later calls use it. */
    public static void load()
    {
        pairs = read();
    }

    private static Set<String> table()
    {
        Set<String> t = pairs;
        if (t == null)
        {
            t = pairs = read();
        }
        return t;
    }

    private static Set<String> read()
    {
        List<String> lines = new ArrayList<String>();
        try
        {
            InputStream in = GripRails.class.getResourceAsStream(TABLE);
            if (in != null)
            {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                for (String line; (line = r.readLine()) != null; )
                {
                    lines.add(line);
                }
                r.close();
            }
        }
        catch (Exception e)
        {
            // no table: no foregrip fits anywhere, lights still do
        }
        Map<String, float[]> m = GripPlacement.parse(lines);
        return new TreeSet<String>(m.keySet());
    }
}
