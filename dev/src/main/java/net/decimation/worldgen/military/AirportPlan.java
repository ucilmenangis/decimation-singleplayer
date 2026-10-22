package net.decimation.worldgen.military;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.decimation.worldgen.FixedBase;
import net.decimation.worldgen.Garrisons;
import net.decimation.worldgen.ZoneKind;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * An {@link Airport} on its city block (city/Airport, docs/airport.md): the block's inside (its first chunk
 * column and row stay streets), ground at the city ground (no grading), turned so the entry gate faces the
 * block's street: turns 0 for a runway along z (gate north), 3 for a runway along x (gate west). The wasteland
 * around it is the military zone already. Its garrison (Garrisons): posts in the towers, the ECP, hangars, HQ and
 * apron positions, the burnt hangar's spawner.
 */
public final class AirportPlan extends CanvasPlan implements FixedBase, Garrisons.Garrisoned
{
    private static final Map<Long, Airport> CACHE = new LinkedHashMap<Long, Airport>(4, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Airport> e)
        {
            return size() > 3;
        }
    };

    private static final Block LINE = Block.getBlockFromName("deci:BlockRoad_CenterLine");

    private final long seed;
    private final int ground;

    public AirportPlan(String id, long seed, int minX, int minZ, int turns, int ground)
    {
        super(id, minX, minZ, turns);
        this.seed = seed;
        this.ground = ground;
    }

    private Airport airport()
    {
        synchronized (CACHE)
        {
            Airport a = CACHE.get(seed);
            if (a == null)
            {
                a = new Airport(seed);
                a.build();
                CACHE.put(seed, a);
                for (String line : a.log)
                {
                    cpw.mods.fml.common.FMLLog.info("[deciworldgen] airport %s", line);   // clashes / refusals (servertest)
                }
            }
            return a;
        }
    }

    /** Clash log (dev test). */
    public List<String> debugLog()
    {
        return airport().log;
    }

    Canvas canvas() { return airport().c; }
    Map<String, float[]> canvasPoints() { return airport().poi; }
    int canvasWidth() { return Airport.W; }
    int canvasLength() { return Airport.L; }
    int canvasHeight() { return Airport.H; }

    public int fixedBaseY() { return ground; }
    public int maxSpread() { return 255; }
    public int clearAbove() { return 6; }
    public Block foundation() { return Blocks.stone; }
    public ZoneKind zone() { return null; }

    public int lotMinX() { return minX(); }
    public int lotMinZ() { return minZ(); }
    public int lotMaxX() { return maxX(); }
    public int lotMaxZ() { return maxZ(); }

    @Override
    public void grade(World world, int x, int z, int baseY)
    {
    }

    public String describe()
    {
        return "military airport rot " + turns * 90;
    }

    public int[] garrison(int baseY)
    {
        Airport a = airport();
        int[] out = new int[1 + 3 * (a.spawners.size() + a.posts.size())];
        out[0] = a.spawners.size();
        int i = 1;
        for (List<int[]> l : java.util.Arrays.asList(a.spawners, a.posts))
        {
            for (int[] p : l)
            {
                double[] w = world(p[0] + 0.5, p[2] + 0.5);
                out[i++] = (int) Math.floor(w[0]);
                out[i++] = baseY + p[1];
                out[i++] = (int) Math.floor(w[1]);
            }
        }
        return out;
    }

    @Override
    public Block blockAt(int rx, int ly, int rz, int[] meta)
    {
        Block b = super.blockAt(rx, ly, rz, meta);
        if (b != null && b == BlockBoomArm.instance && meta[0] < 2 && (turns & 1) == 1)
        {
            meta[0] ^= 1;                                      // an arm lying along x lies along z
        }
        if (b != null && b == LINE && (turns & 1) == 1)
        {
            meta[0] ^= 6;                                      // centre line: 4 runs along z, 2 along x
        }
        return b;
    }

    /** Shelves of a base hold military loot (fixes/LootBlocks; its weapon cabinets already do). */
    @Override
    int lootPlace()
    {
        return net.decimation.fixes.LootBlocks.MILITARY;
    }
}
