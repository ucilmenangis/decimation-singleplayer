package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.decimation.worldgen.Plan;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.Slices;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import static net.decimation.worldgen.city.LcCity.*;

/**
 * The land just outside a city cell: ramps from the city ground at the
 * cell edge to the natural height (smoothstep over a band 2 blocks per
 * block of height difference, 6 .. EDGE wide), cutting or filling,
 * keeping the column's own surface block. Distance is Euclidean, so
 * outer corners are rounded. A column belongs to the nearest city cell
 * only (ties: lowest cell), so no column is graded twice. Writes no
 * blocks of its own (blockAt is SKIP everywhere).
 */
final class EdgePlan implements net.decimation.worldgen.FixedBase, net.decimation.worldgen.Graded
{
    private final String id;
    private final int cellX, cellZ, ground;
    /** City cells around this one, [dx + 1][dz + 1]. */
    private final boolean[][] city;

    EdgePlan(String id, int cellX, int cellZ, int ground, boolean[][] city)
    {
        this.id = id;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.ground = ground;
        this.city = city;
    }

    public int fixedBaseY() { return ground; }
    public String id() { return id; }
    public int minX() { return cellX * CELL * 16 - EDGE; }
    public int minZ() { return cellZ * CELL * 16 - EDGE; }
    public int maxX() { return (cellX + 1) * CELL * 16 - 1 + EDGE; }
    public int maxZ() { return (cellZ + 1) * CELL * 16 - 1 + EDGE; }
    public int lotMinX() { return minX(); }
    public int lotMinZ() { return minZ(); }
    public int lotMaxX() { return maxX(); }
    public int lotMaxZ() { return maxZ(); }
    public int height() { return 0; }
    public int clearAbove() { return 0; }
    public int maxSpread() { return 255; }
    public Block foundation() { return Blocks.dirt; }
    public net.decimation.worldgen.ZoneKind zone() { return null; }
    public String describe() { return null; }

    public Block blockAt(int lx, int ly, int lz, int[] meta)
    {
        meta[0] = SKIP;
        return null;
    }

    /** Squared distance from (x, z) to city cell (cx, cz); 0 inside. */
    private static long dist2(int cx, int cz, int x, int z)
    {
        int x0 = cx * CELL * 16, z0 = cz * CELL * 16;
        long dx = Math.max(0, Math.max(x0 - x, x - (x0 + CELL * 16 - 1)));
        long dz = Math.max(0, Math.max(z0 - z, z - (z0 + CELL * 16 - 1)));
        return dx * dx + dz * dz;
    }

    /** Distance to this cell when it is the column's nearest city cell, else -1. */
    double owner(int x, int z)
    {
        int ox = Math.floorDiv(x, CELL * 16) - cellX + 1, oz = Math.floorDiv(z, CELL * 16) - cellZ + 1;
        if (ox < 0 || ox > 2 || oz < 0 || oz > 2 || city[ox][oz])
        {
            return -1; // inside the city, or out of reach
        }
        long own = dist2(cellX, cellZ, x, z);
        if (own > (long) EDGE * EDGE)
        {
            return -1;
        }
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                if (!city[i][j] || (i == 1 && j == 1))
                {
                    continue;
                }
                long d = dist2(cellX + i - 1, cellZ + j - 1, x, z);
                // ties go to the lower cell (x first), the same order every plan uses
                if (d < own || (d == own && (i < 1 || (i == 1 && j < 1))))
                {
                    return -1;
                }
            }
        }
        return Math.sqrt(own);
    }

    /** Smooth value noise in -1 .. 1, 12 block lattice. */
    static double wobble(int x, int z)
    {
        double gx = x / 12.0, gz = z / 12.0;
        int ix = (int) Math.floor(gx), iz = (int) Math.floor(gz);
        double fx = gx - ix, fz = gz - iz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = lattice(ix, iz), b = lattice(ix + 1, iz), c = lattice(ix, iz + 1), e = lattice(ix + 1, iz + 1);
        return (a + (b - a) * fx) * (1 - fz) + (c + (e - c) * fx) * fz;
    }

    private static double lattice(int x, int z)
    {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return ((h >>> 11) / (double) (1L << 53)) * 2 - 1;
    }

    public void grade(World world, int x, int z, int baseY)
    {
        double d = owner(x, z);
        if (d <= 0)
        {
            return;
        }
        if (Highways.at(world.getSeed(), x >> 4, z >> 4))
        {
            return; // the highway levels its own chunk
        }
        int natural = StructureGenerator.soilTop(world, x, z);
        if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z))
        {
            return;
        }
        int width = Math.max(6, Math.min(EDGE, 2 * Math.abs(natural - ground) + 4));
        // contours wander +-4 blocks so the 1 block steps do not run parallel to the street
        double t = Math.max(0, d + 4 * wobble(x, z)) / (width + 1);
        if (t >= 1)
        {
            return;
        }
        t = t * t * (3 - 2 * t);
        reshape(world, x, z, natural, ground + (int) Math.round((natural - ground) * t));
    }
}
