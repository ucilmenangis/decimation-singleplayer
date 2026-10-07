package net.decimation.worldgen;

import java.util.Arrays;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Writes one slice of a {@link Plan}: the part inside a chunk's population
 * window. The first slice decides the floor height from the terrain it can
 * see (median of 9 samples) or rejects the site (water, too steep) and stores
 * that in {@link StructureData}, so every later slice agrees.
 */
public final class Slices
{
    private static final int MAX_FOUNDATION = 12;

    private Slices()
    {
    }

    /** Population window of a chunk: 16x16 at +8/+8, tiles the world. */
    public static int[] window(int chunkX, int chunkZ)
    {
        int x0 = (chunkX << 4) + 8, z0 = (chunkZ << 4) + 8;
        return new int[] {x0, z0, x0 + 15, z0 + 15};
    }

    public static boolean intersects(Plan p, int[] w)
    {
        return p.minX() <= w[2] && p.maxX() >= w[0] && p.minZ() <= w[3] && p.maxZ() >= w[1];
    }

    /** Plan bounds, widened to the lot for a {@link Graded} plan: {x0, z0, x1, z1}. */
    private static int[] extent(Plan p)
    {
        if (p instanceof Graded)
        {
            Graded g = (Graded) p;
            return new int[] {Math.min(p.minX(), g.lotMinX()), Math.min(p.minZ(), g.lotMinZ()),
                              Math.max(p.maxX(), g.lotMaxX()), Math.max(p.maxZ(), g.lotMaxZ())};
        }
        return new int[] {p.minX(), p.minZ(), p.maxX(), p.maxZ()};
    }

    public static void place(World world, Plan p, int[] w)
    {
        int[] e = extent(p);
        int x0 = Math.max(w[0], e[0]), x1 = Math.min(w[2], e[2]);
        int z0 = Math.max(w[1], e[1]), z1 = Math.min(w[3], e[3]);
        if (x0 > x1 || z0 > z1)
        {
            return;
        }
        StructureData data = StructureData.get(world);
        Integer baseY = data.baseY(p.id());
        if (baseY == null)
        {
            // window samples from the building itself when this slice has any
            int sx0 = Math.max(x0, p.minX()), sx1 = Math.min(x1, p.maxX());
            int sz0 = Math.max(z0, p.minZ()), sz1 = Math.min(z1, p.maxZ());
            baseY = sx0 <= sx1 && sz0 <= sz1
                ? decideBase(world, p, sx0, sz0, sx1, sz1)
                : decideBase(world, p, x0, z0, x1, z1);
            data.setBaseY(p.id(), baseY);
            if (baseY != StructureData.CANCELLED)
            {
                if (p.zone() != null)
                {
                    ZoneStore.add(p.zone(), p.minX() - 6, baseY - 4, p.minZ() - 6,
                                  p.maxX() + 6, baseY + p.height() + 12, p.maxZ() + 6);
                }
                FMLLog.info("[%s] %s %s at %d,%d,%d", DecimationWorldGen.MODID,
                            p.describe(), p.id(), p.minX(), baseY, p.minZ());
            }
        }
        if (baseY != StructureData.CANCELLED)
        {
            write(world, p, baseY, x0, z0, x1, z1);
        }
    }

    /**
     * Floor height from the terrain, or CANCELLED. Samples a 5x5 grid over
     * the WHOLE footprint, but only where the chunk already exists (never
     * forces generation); the first slice's own window is always loaded, so
     * there are at least those samples. Median of the samples; rejected on
     * water or a spread above maxSpread.
     */
    private static int decideBase(World world, Plan p, int x0, int z0, int x1, int z1)
    {
        int[] ys = new int[25 + 9];
        int n = 0;
        for (int i = 0; i < 5; i++)
        {
            for (int k = 0; k < 5; k++)
            {
                int x = p.minX() + (p.maxX() - p.minX()) * i / 4;
                int z = p.minZ() + (p.maxZ() - p.minZ()) * k / 4;
                if (!world.blockExists(x, 64, z))
                {
                    continue;
                }
                int y = sample(world, x, z);
                if (y == Integer.MIN_VALUE)
                {
                    return StructureData.CANCELLED;
                }
                ys[n++] = y;
            }
        }
        for (int i = 0; i < 3; i++)
        {
            for (int k = 0; k < 3; k++)
            {
                int y = sample(world, x0 + (x1 - x0) * i / 2, z0 + (z1 - z0) * k / 2);
                if (y == Integer.MIN_VALUE)
                {
                    return StructureData.CANCELLED;
                }
                ys[n++] = y;
            }
        }
        Arrays.sort(ys, 0, n);
        if (ys[n - 1] - ys[0] > p.maxSpread())
        {
            return StructureData.CANCELLED;
        }
        return ys[n / 2];
    }

    /** Soil height at a column, or MIN_VALUE for water / void. */
    private static int sample(World world, int x, int z)
    {
        int y = StructureGenerator.soilTop(world, x, z);
        if (y < 5 || StructureGenerator.waterAbove(world, x, y, z))
        {
            return Integer.MIN_VALUE;
        }
        return y;
    }

    private static void write(World world, Plan p, int baseY, int x0, int z0, int x1, int z1)
    {
        int[] meta = new int[1];
        int top = baseY + p.height();
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                if (p instanceof Graded)
                {
                    ((Graded) p).grade(world, x, z, baseY);
                }
                if (x < p.minX() || x > p.maxX() || z < p.minZ() || z > p.maxZ())
                {
                    continue;
                }
                int lx = x - p.minX(), lz = z - p.minZ();
                meta[0] = 0;
                Block floor = p.blockAt(lx, 0, lz, meta);
                boolean skipColumn = floor == null && meta[0] == Plan.SKIP;
                if (!skipColumn)
                {
                    for (int y = baseY - 1; y > baseY - 1 - MAX_FOUNDATION && y > 0; y--)
                    {
                        Block below = world.getBlock(x, y, z);
                        if (!below.getMaterial().isReplaceable() && !below.getMaterial().isLiquid()
                            && below != Blocks.leaves && below != Blocks.leaves2)
                        {
                            break;
                        }
                        world.setBlock(x, y, z, p.foundation(), 0, 2);
                    }
                }
                for (int y = baseY; y <= top + p.clearAbove(); y++)
                {
                    meta[0] = 0;
                    Block block = y <= top ? p.blockAt(lx, y - baseY, lz, meta) : null;
                    if (block == null)
                    {
                        if (meta[0] == Plan.SKIP || (y > top && skipColumn))
                        {
                            continue;
                        }
                        if (!world.isAirBlock(x, y, z))
                        {
                            world.setBlock(x, y, z, Blocks.air, 0, 2);
                        }
                    }
                    else
                    {
                        world.setBlock(x, y, z, block, meta[0], 2);
                        if (block.hasTileEntity(meta[0]))
                        {
                            // multiblock props are drawn only by their master part
                            net.decimation.fixes.MultiblockRepairHandler.repair(world.getTileEntity(x, y, z));
                        }
                    }
                }
            }
        }
    }
}
