package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.world.World;

/**
 * Large schematics (any size up to MAX_SIZE), from
 * config/decimation_worldgen/large/. The world is split into SITE x SITE chunk
 * sites (128 blocks); each site deterministically hosts at most one large
 * structure, picked by sector from the filename prefix like the small ones
 * (civ_ / city_ / mil_, untagged = any sector), rotated, and placed fully
 * inside the site. Written slice by slice through {@link Slices}. City sectors
 * keep their procedural blocks and get no large sites.
 */
public class LargeSites
{
    public static final int SITE = 8;               // chunks
    public static final int MAX_SIZE = SITE * 16 - 8;
    private static final float[] CHANCE = {0.15f, 0.30f, 0.0f, 0.50f}; // wild civ city mil

    private final List<Schematic> schematics;
    private final Map<Integer, StructureGenerator.Sub> subs;

    public LargeSites(List<Schematic> schematics, Map<Integer, StructureGenerator.Sub> subs)
    {
        this.schematics = schematics;
        this.subs = subs;
    }

    /** Never empty: military sectors always have their procedural bases (worldgen/military). */
    public boolean isEmpty()
    {
        return false;
    }

    /** The plan of one site, or null. Pure function of seed + site + sector. */
    public Plan plan(World world, int siteX, int siteZ, StructureGenerator gen)
    {
        int sector = gen.sectorOf(world, siteX * SITE, siteZ * SITE);
        Random r = new Random(world.getSeed() ^ (siteX * 192837465L + siteZ * 564738291L) ^ 0x1A26E5L);
        if (r.nextFloat() >= CHANCE[sector])
        {
            return null;
        }
        if (sector == StructureGenerator.MIL)
        {
            return militaryBase(world, siteX, siteZ, r);
        }
        if (schematics.isEmpty())
        {
            return null;
        }
        List<Schematic> pool = new ArrayList<Schematic>();
        for (Schematic s : schematics)
        {
            boolean civ = s.name.startsWith("civ_"), city = s.name.startsWith("city_"), mil = s.name.startsWith("mil_");
            if (!(civ || city || mil) || (sector == StructureGenerator.CIV && civ)
                || (sector == StructureGenerator.MIL && mil))
            {
                pool.add(s);
            }
        }
        if (pool.isEmpty())
        {
            return null;
        }
        Schematic s = pool.get(r.nextInt(pool.size()));
        int turns = r.nextInt(4);
        int w = (turns & 1) != 0 ? s.length : s.width;
        int l = (turns & 1) != 0 ? s.width : s.length;
        int span = SITE * 16;
        int x = siteX * span + 4 + r.nextInt(Math.max(1, span - 8 - w));
        int z = siteZ * span + 4 + r.nextInt(Math.max(1, span - 8 - l));
        if (net.decimation.worldgen.city.LcCity.enabled()
            && (net.decimation.worldgen.city.LcCity.nearCity(world.getSeed(), x, z, x + w - 1, z + l - 1)
                || net.decimation.worldgen.city.Highways.near(world.getSeed(), x, z, x + w - 1, z + l - 1)))
        {
            return null; // the city's edge ramp or a highway would cut the ground under it
        }
        ZoneKind zone = s.name.startsWith("mil_")
            ? ZoneKind.MILITARY
            : s.name.startsWith("city_") ? ZoneKind.POLICE : null;
        return new SchematicPlan("L" + siteX + "_" + siteZ, s, x, z, turns, subs, zone);
    }

    /**
     * A US style forward base of our own (docs/military_base.md): 45 % combat outpost, 35 % FOB,
     * 20 % large FOB, turned 0..270, placed inside the site; the mil_ schematics are not used.
     */
    private Plan militaryBase(World world, int siteX, int siteZ, Random r)
    {
        float roll = r.nextFloat();
        int size = roll < 0.45f ? net.decimation.worldgen.military.MilitaryBase.SMALL
            : roll < 0.80f ? net.decimation.worldgen.military.MilitaryBase.MEDIUM
            : net.decimation.worldgen.military.MilitaryBase.LARGE;
        int turns = r.nextInt(4);
        int[] f = net.decimation.worldgen.military.MilitaryBasePlan.footprint(size, turns);
        int span = SITE * 16;
        int x = siteX * span + 4 + r.nextInt(Math.max(1, span - 8 - f[0]));
        int z = siteZ * span + 4 + r.nextInt(Math.max(1, span - 8 - f[1]));
        if (net.decimation.worldgen.city.LcCity.enabled()
            && (net.decimation.worldgen.city.LcCity.nearCity(world.getSeed(), x - 10, z - 10, x + f[0] + 9, z + f[1] + 9)
                || net.decimation.worldgen.city.Highways.near(world.getSeed(), x - 10, z - 10, x + f[0] + 9, z + f[1] + 9)))
        {
            return null;
        }
        long seed = world.getSeed() ^ (siteX * 341873128712L + siteZ * 132897987541L) ^ 0xF0B5L;
        return new net.decimation.worldgen.military.MilitaryBasePlan("M" + siteX + "_" + siteZ, size, seed, x, z, turns);
    }

    /** Write the slices of every large plan crossing this chunk's window. */
    public void populate(World world, int chunkX, int chunkZ, StructureGenerator gen)
    {
        int[] w = Slices.window(chunkX, chunkZ);
        for (Plan p : plansAround(world, w, gen))
        {
            Slices.place(world, p, w);
        }
    }

    /** True if a large plan covers any of this rectangle (to keep small ruins out). */
    public boolean covers(World world, int x0, int z0, int x1, int z1, StructureGenerator gen)
    {
        for (Plan p : plansAround(world, new int[] {x0, z0, x1, z1}, gen))
        {
            if (Slices.intersects(p, new int[] {x0, z0, x1, z1}))
            {
                return true;
            }
        }
        return false;
    }

    private List<Plan> plansAround(World world, int[] w, StructureGenerator gen)
    {
        List<Plan> out = new ArrayList<Plan>();
        int span = SITE * 16;
        for (int sx = Math.floorDiv(w[0], span); sx <= Math.floorDiv(w[2], span); sx++)
        {
            for (int sz = Math.floorDiv(w[1], span); sz <= Math.floorDiv(w[3], span); sz++)
            {
                Plan p = plan(world, sx, sz, gen);
                if (p != null && Slices.intersects(p, w))
                {
                    out.add(p);
                }
            }
        }
        return out;
    }
}
