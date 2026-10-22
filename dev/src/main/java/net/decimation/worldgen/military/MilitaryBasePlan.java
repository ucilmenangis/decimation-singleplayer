package net.decimation.worldgen.military;

import java.util.LinkedHashMap;
import java.util.Map;

import net.decimation.worldgen.ZoneKind;

/**
 * A {@link MilitaryBase} as a slice placed plan (docs/military_base.md): a CanvasPlan (turned,
 * graded) with a MILITARY zone. The canvas is built once per base and cached (plans are asked for
 * by every chunk around them).
 */
public final class MilitaryBasePlan extends CanvasPlan implements net.decimation.worldgen.NamedPlace
{
    /** US style callsigns for base names ("FOB Viper"). */
    private static final String[] CALLSIGNS = {"Viper", "Raven", "Falcon", "Hammer", "Bastion", "Warrior", "Eagle",
        "Lightning", "Phoenix", "Sentinel", "Wolfhound", "Ironhorse", "Thunder", "Spartan", "Gator", "Liberty", "Anvil",
        "Cobra", "Durango", "Outlaw", "Patriot", "Reaper", "Saber", "Tomahawk"};
    private static final Map<String, MilitaryBase> CACHE = new LinkedHashMap<String, MilitaryBase>(16, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, MilitaryBase> e)
        {
            return size() > 6;
        }
    };

    private final int size;
    private final long seed;

    public MilitaryBasePlan(String id, int size, long seed, int minX, int minZ, int turns)
    {
        super(id, minX, minZ, turns);
        this.size = size;
        this.seed = seed;
    }

    /** Footprint (x, z) after turning, for the site placement. */
    public static int[] footprint(int size, int turns)
    {
        int w = MilitaryBase.SIZE[size][0], l = MilitaryBase.SIZE[size][1];
        return (turns & 1) != 0 ? new int[] {l, w} : new int[] {w, l};
    }

    private MilitaryBase base()
    {
        synchronized (CACHE)
        {
            MilitaryBase m = CACHE.get(id);
            if (m == null)
            {
                m = new MilitaryBase(size, seed);
                m.build();
                CACHE.put(id, m);
            }
            return m;
        }
    }

    /** Placed / refused modules (dev test). */
    public java.util.List<String> debugLog()
    {
        return base().log;
    }

    Canvas canvas()
    {
        return base().c;
    }

    Map<String, float[]> canvasPoints()
    {
        return base().poi;
    }

    int canvasWidth() { return MilitaryBase.SIZE[size][0]; }
    int canvasLength() { return MilitaryBase.SIZE[size][1]; }
    int canvasHeight() { return MilitaryBase.HEIGHT; }

    public int maxSpread() { return size == MilitaryBase.SMALL ? 9 : 11; }
    public ZoneKind zone() { return ZoneKind.MILITARY; }

    /** COP (outpost), FOB, Camp (large) + a callsign from the seed and position. */
    public String placeName()
    {
        String kind = size == MilitaryBase.SMALL ? "COP" : size == MilitaryBase.SIZE_NAME.length - 1 ? "Camp" : "FOB";
        long h = (seed ^ minX * 341873128712L ^ minZ * 132897987541L) * 0x9E3779B97F4A7C15L;
        return kind + " " + CALLSIGNS[(int) ((h >>> 33) % CALLSIGNS.length)];
    }

    public String placeOwner()
    {
        return "Military";
    }

    public String describe()
    {
        return "military base (" + MilitaryBase.SIZE_NAME[size] + ") " + (maxX() - minX + 1) + "x" + (maxZ() - minZ + 1)
            + " rot " + turns * 90;
    }

    /** Shelves of a base hold military loot (fixes/LootBlocks; its weapon cabinets already do). */
    @Override
    int lootPlace()
    {
        return net.decimation.fixes.LootBlocks.MILITARY;
    }
}
