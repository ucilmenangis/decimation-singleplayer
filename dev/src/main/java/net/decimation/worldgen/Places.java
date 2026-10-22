package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Which kind of place a block is, for the place spawn table (fixes/PlaceSpawns; user, 22 Oktober 2026, idea 35: "the
 * wasteland irradiated and military infected, demons at night; bases and airports soldiers; hospitals medic infected;
 * police stations riot infected; forests infected dogs; highways bandit groups"). Known at spawn time without searching
 * the world:
 * - airport: the airport block of a wasteland sector (city/Airport.site, from the seed);
 * - base: the named places of the military in StructureData (PlaceNames stores a base's box when its first slice is
 *   written), a handful of boxes;
 * - police station, hospital: the city building planned on that chunk (LcCity.buildingKind, BuildingDress's kinds from
 *   the building's name: "police", "hospital" or "polyclinic"), from the seed;
 * - highway: Highways.at, per chunk from the seed;
 * - wasteland: Wasteland.chunk, per chunk from the seed;
 * - forest: the biome at the block is of Forge's FOREST type (vanilla forests and taigas, our Overgrown Forest and Hills,
 *   the Burnt Forest).
 * The first that holds wins (Kind order). The seed's places of a chunk are memoised (a direct mapped table of chunks, no
 * allocation on a hit), cleared at every world load (the layout switches are per world).
 * Not known: city buildings in worlds without the converted city content (the procedural city has no police or
 * hospital buildings), buildings of other mods or of hand made maps, a base before its first slice is written (no
 * spawns there yet anyway), a forest's trees outside a forest biome.
 */
public final class Places
{
    /** Place kinds, the most specific first: the first that holds is the place. */
    public enum Kind
    {
        AIRPORT("airport"), BASE("base"), POLICE_STATION("police_station"), HOSPITAL("hospital"), HIGHWAY("highway"),
        WASTELAND("wasteland"), FOREST("forest");

        /** Its name in the config file. */
        public final String id;

        Kind(String id)
        {
            this.id = id;
        }

        public static Kind byId(String id)
        {
            for (Kind k : values())
            {
                if (k.id.equals(id))
                {
                    return k;
                }
            }
            return null;
        }
    }

    /** A direct mapped table chunk -> byte (0: not known), so a hit allocates nothing. */
    static final class Memo
    {
        private final long[] keys;
        private final byte[] values;
        private final int mask;

        Memo(int size)
        {
            keys = new long[size];
            values = new byte[size];
            mask = size - 1;
        }

        private int slot(long key)
        {
            return (int) (key ^ key >>> 29) & mask;
        }

        byte get(int chx, int chz)
        {
            long key = Keys.pair(chx, chz);
            int i = slot(key);
            return keys[i] == key ? values[i] : 0;
        }

        void put(int chx, int chz, byte value)
        {
            long key = Keys.pair(chx, chz);
            int i = slot(key);
            keys[i] = key;
            values[i] = value;
        }

        void clear()
        {
            java.util.Arrays.fill(values, (byte) 0);
        }
    }

    private static final byte NONE = 1;
    private static final Kind[] KINDS = Kind.values();                 // values() copies its array on every call
    private static final Memo MEMO = new Memo(8192);
    private static long memoSeed = Long.MIN_VALUE;
    private static final List<int[]> BASES = new ArrayList<int[]>();
    private static int basesFrom = -1;
    private static final byte[] FOREST_BIOME = new byte[256];   // 0 not known, 1 no, 2 yes

    /** For the dev test: lookups and their time in nanoseconds. */
    static long lookups, lookupNanos;

    /** The place kind at a block, or null for none of them (server). */
    public static synchronized Kind at(World w, int x, int z)
    {
        long start = System.nanoTime();
        long seed = w.getSeed();
        if (seed != memoSeed)
        {
            MEMO.clear();
            memoSeed = seed;
        }
        int chx = x >> 4, chz = z >> 4;
        byte m = MEMO.get(chx, chz);
        if (m == 0)
        {
            Kind k = chunkKind(seed, chx, chz);
            m = k == null ? NONE : (byte) (k.ordinal() + 2);
            MEMO.put(chx, chz, m);
        }
        Kind k = m == NONE ? null : KINDS[m - 2];
        if (k != Kind.AIRPORT && inBase(w, x, z))
        {
            k = Kind.BASE;
        }
        if (k == null && forest(w.getBiomeGenForCoords(x, z)))
        {
            k = Kind.FOREST;
        }
        lookups++;
        lookupNanos += System.nanoTime() - start;
        return k;
    }

    /** The seed's place on a chunk: airport, police station, hospital, highway or wasteland (null for none). */
    static Kind chunkKind(long seed, int chx, int chz)
    {
        int cell = net.decimation.worldgen.city.LcCity.CELL;
        int cx = Math.floorDiv(chx, cell), cz = Math.floorDiv(chz, cell);
        if (net.decimation.worldgen.city.LcCity.enabled() && net.decimation.worldgen.city.LcCity.isCity(seed, cx, cz))
        {
            int per = Sectors.REGION / cell;
            int[] a = net.decimation.worldgen.city.Airport.site(seed, Math.floorDiv(cx, per), Math.floorDiv(cz, per));
            if (a != null && cx >= a[0] && cz >= a[1] && cx < a[0] + a[2] && cz < a[1] + a[3])
            {
                return Kind.AIRPORT;
            }
            String b = net.decimation.worldgen.city.LcCity.buildingKind(seed, chx * 16 + 8, chz * 16 + 8);
            if ("police".equals(b))
            {
                return Kind.POLICE_STATION;
            }
            if ("medical".equals(b))
            {
                return Kind.HOSPITAL;
            }
            return net.decimation.worldgen.city.Wasteland.chunk(seed, chx, chz) ? Kind.WASTELAND : null;
        }
        return net.decimation.worldgen.city.Highways.at(seed, chx, chz) ? Kind.HIGHWAY : null;
    }

    /** Inside a base: the military's named places (re-read when StructureData holds a new one). */
    private static boolean inBase(World w, int x, int z)
    {
        Collection<String> places = StructureData.get(w).places();
        if (places.size() != basesFrom)
        {
            BASES.clear();
            for (String p : places)
            {
                int[] b = baseBox(p);
                if (b != null)
                {
                    BASES.add(b);
                }
            }
            basesFrom = places.size();
        }
        for (int i = 0; i < BASES.size(); i++)
        {
            int[] b = BASES.get(i);
            if (x >= b[0] && z >= b[1] && x <= b[2] && z <= b[3])
            {
                return true;
            }
        }
        return false;
    }

    /** The box {x0, z0, x1, z1} of a place entry "x0/0/z0=x1/255/z1=owner=name" held by the military, else null. */
    static int[] baseBox(String entry)
    {
        String[] p = entry.split("=");
        if (p.length < 4 || !"Military".equals(p[2]))
        {
            return null;
        }
        try
        {
            String[] a = p[0].split("/"), b = p[1].split("/");
            int x0 = Integer.parseInt(a[0]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(b[0]), z1 = Integer.parseInt(b[2]);
            return new int[] {Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)};
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static boolean forest(BiomeGenBase b)
    {
        if (b == null || b.biomeID < 0 || b.biomeID > 255)
        {
            return false;
        }
        byte f = FOREST_BIOME[b.biomeID];
        if (f == 0)
        {
            f = BiomeDictionary.isBiomeOfType(b, BiomeDictionary.Type.FOREST) ? (byte) 2 : 1;
            FOREST_BIOME[b.biomeID] = f;
        }
        return f == 2;
    }

    /** Forget what is memoised (world load: the layout switches are per world; dev tests). */
    public static synchronized void reset()
    {
        MEMO.clear();
        memoSeed = Long.MIN_VALUE;
        BASES.clear();
        basesFrom = -1;
    }

    /** After the world's layout switches are set (Wasteland, Airport, city sizes: normal priority handlers). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldEvent.Load e)
    {
        if (!e.world.isRemote && e.world.provider != null && e.world.provider.dimensionId == 0)
        {
            reset();
        }
    }

    /** For the dev test: average lookup time in microseconds. */
    public static double averageLookupMicros()
    {
        return lookups == 0 ? 0 : lookupNanos / 1000.0 / lookups;
    }
}
