package net.decimation.worldgen.building;

import net.decimation.worldgen.Plan;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * The outside of a building: facade palette (wall, trim, ground storey,
 * windows), outer walls with windows and the entrance, the stair core or
 * ladder shaft, the roof and the vines on the margin ring.
 */
final class Shell
{
    private final Building b;
    final Block wall, trim, ground, window;
    final int wallMeta, trimMeta, groundMeta, windowMeta;

    Shell(Building b)
    {
        this.b = b;
        Block[] walls = {Blocks.brick_block, Blocks.stained_hardened_clay, Blocks.stained_hardened_clay,
            Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, Blocks.stonebrick, Blocks.quartz_block,
            Blocks.sandstone, Blocks.hardened_clay, Blocks.stained_hardened_clay, Blocks.stained_hardened_clay};
        int[] wallMetas = {0, 0, 8, 7, 1, 0, 0, 2, 0, 9, 12};
        Block[] trims = {Blocks.stonebrick, Blocks.stained_hardened_clay, Blocks.stonebrick,
            Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, Blocks.double_stone_slab, Blocks.quartz_block,
            Blocks.sandstone, Blocks.stonebrick, Blocks.stained_hardened_clay, Blocks.stonebrick};
        int[] trimMetas = {0, 8, 0, 15, 12, 0, 2, 1, 0, 0, 0};
        int p;
        double r = b.unit(7, 7, 7);
        if (b.style == Building.DRY && r < 0.5)
        {
            p = r < 0.3 ? 7 : 8; // sandstone or plain clay in deserts
        }
        else if (b.kind == Building.OFFICE)
        {
            p = new int[] {1, 2, 3, 6, 9, 5}[(int) (b.unit(8, 8, 8) * 6)];
        }
        else if (b.kind == Building.SHOP)
        {
            p = new int[] {0, 4, 5, 8, 10, 2}[(int) (b.unit(8, 8, 8) * 6)];
        }
        else
        {
            p = new int[] {0, 0, 1, 2, 4, 5, 10, 8}[(int) (b.unit(8, 8, 8) * 8)];
        }
        wall = walls[p];
        wallMeta = wallMetas[p];
        trim = trims[p];
        trimMeta = trimMetas[p];
        boolean groundTrim = b.unit(9, 9, 9) < (b.kind == Building.APARTMENT ? 0.4 : 0.7);
        ground = groundTrim ? trim : wall;
        groundMeta = groundTrim ? trimMeta : wallMeta;
        if (b.kind == Building.OFFICE && b.unit(10, 10, 10) < 0.5)
        {
            window = Blocks.stained_glass_pane;
            windowMeta = b.unit(11, 11, 11) < 0.5 ? 7 : 3; // gray / light blue tint
        }
        else
        {
            window = Blocks.glass_pane;
            windowMeta = 0;
        }
    }

    Block outerWall(int fx, int ly, int z, int storey, int within, int[] meta)
    {
        boolean corner = (fx == 0 || fx == b.width - 1) && (z == 0 || z == b.length - 1);
        if (corner)
        {
            meta[0] = trimMeta;
            return trim;
        }
        int along = (fx == 0 || fx == b.width - 1) ? z : fx;
        boolean frontWall = fx == 0;
        double decay = b.ruins.decay;
        if (frontWall && storey == 0 && within <= 2
            && Math.abs(along - b.entranceZ) <= (b.kind == Building.SHOP ? 1 : b.kind == Building.OFFICE ? 1 : 0))
        {
            if (b.kind == Building.APARTMENT && within <= 2 && b.props.officeDoor != null
                && b.unit(3, storey, along) > decay)
            {
                // apartment entrance: a door in the 1 wide gap (wall along z)
                meta[0] = within == 1 ? (b.unit(4, 4, along) < 0.3 ? 4 : 0) : 8;
                return b.props.officeDoor;
            }
            return null; // entrance
        }
        if (isWindow(along, within, storey, frontWall))
        {
            if (b.unit(fx, ly, z) < 0.35 + decay * 0.6)
            {
                return null; // broken
            }
            if (b.kind == Building.SHOP && storey == 0)
            {
                return Blocks.iron_bars;
            }
            meta[0] = windowMeta;
            return window;
        }
        double d = b.unit(fx, ly, z);
        double holes = storey == 0 ? decay * 0.08 : decay * 0.25 * (1 + storey / (double) b.floors);
        if (d < holes)
        {
            return null;
        }
        double moss = b.ruins.mossChance();
        if (d < holes + moss)
        {
            meta[0] = 0;
            return b.style == Building.DRY ? Blocks.cobblestone : Blocks.mossy_cobblestone;
        }
        if (d < holes + moss + 0.06)
        {
            meta[0] = 2; // cracked stone brick
            return Blocks.stonebrick;
        }
        if (storey == 0)
        {
            meta[0] = groundMeta;
            return ground;
        }
        if (within == 0)
        {
            meta[0] = trimMeta;
            return trim;
        }
        meta[0] = wallMeta;
        return wall;
    }

    private boolean isWindow(int along, int within, int storey, boolean frontWall)
    {
        if (b.kind == Building.SHOP && storey == 0)
        {
            return frontWall && within >= 1 && within <= 2 && along % 4 != 0;
        }
        if (b.kind == Building.OFFICE)
        {
            return within >= 1 && within <= 2 && along % 3 != 0;
        }
        return within == 2 && along % 3 == 1 || (within == 1 && along % 3 == 1 && b.unit(along, storey, 3) < 0.3);
    }

    /**
     * A lining cell stays open where the outer wall next to it is open at
     * that height (window, broken window, entrance): a recess, not a wall.
     */
    boolean liningOpen(int fx, int z, int storey, int within)
    {
        int[] tmp = new int[1];
        int ly = storey * b.storeyHeight + within;
        int[][] around = {{fx - 1, z}, {fx + 1, z}, {fx, z - 1}, {fx, z + 1}};
        for (int[] n : around)
        {
            if (n[0] == 0 || n[1] == 0 || n[0] == b.width - 1 || n[1] == b.length - 1)
            {
                Block w = outerWall(n[0], ly, n[1], storey, within, tmp);
                if (w == null || w == Blocks.glass_pane || w == Blocks.stained_glass_pane)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Stair core: 7 long (a, along fx) x 4 wide (b). Landings at a = 0 and
     * a = 6. Storey s climbs on lane b 0..1 (even s, from a = 1 up to a = 5)
     * or b 2..3 (odd s, from a = 5 down to a = 1), one step per block of
     * storey height; the ceiling layer is open above the run's steps 1+;
     * step j sits at height s*H + 1 + j, so the top step lies in the next
     * floor layer and the floor above the lower steps is left open. Returns
     * with meta[0] == Integer.MIN_VALUE for "plain floor here".
     */
    Block core(int a, int lane, int storey, int within, int[] meta)
    {
        int h = b.storeyHeight;
        int ly = storey * h + within;
        if (within == 0)
        {
            int s = storey - 1; // the run arriving into this floor layer
            if (s >= 0 && s < b.floors && onLane(s, lane))
            {
                int j = stepIndex(s, a);
                if (j == h - 1)
                {
                    meta[0] = stairMeta((s & 1) == 0);
                    return b.kind == Building.OFFICE ? Blocks.stone_brick_stairs : Blocks.oak_stairs;
                }
                if (j >= 0)
                {
                    return null; // opening above the arriving run
                }
            }
            meta[0] = Integer.MIN_VALUE;
            return null;
        }
        if (storey < b.floors && onLane(storey, lane))
        {
            int j = stepIndex(storey, a);
            if (j >= 0 && ly == storey * h + 1 + j)
            {
                meta[0] = stairMeta((storey & 1) == 0);
                return b.kind == Building.OFFICE ? Blocks.stone_brick_stairs : Blocks.oak_stairs;
            }
            if (within == b.ceil() && j >= 1)
            {
                return null; // headroom over the climbing run
            }
        }
        if (within == b.ceil())
        {
            meta[0] = 0;
            Block c = b.deci("BlockWallOffice_Top");
            return c != null ? c : Blocks.stone;
        }
        return null; // core interior stays open
    }

    private static boolean onLane(int storey, int lane)
    {
        return ((storey & 1) == 0) == (lane <= 1);
    }

    /** Step index 0..H-1 of storey's run at core position a, or -1. */
    private int stepIndex(int storey, int a)
    {
        int h = b.storeyHeight;
        int j = (storey & 1) == 0 ? a - 1 : h - a;
        return j >= 0 && j <= h - 1 ? j : -1;
    }

    private int stairMeta(boolean even)
    {
        // even runs climb toward +fx, odd toward -fx; +fx is world +x when
        // the front faces west. Stair meta: 0 ascends east, 1 west.
        boolean east = even == (b.front == Building.FRONT_WEST);
        return east ? 0 : 1;
    }

    Block ladder(int[] meta)
    {
        // ladder at fx = W-2 against the back wall (fx = W-1)
        meta[0] = b.front == Building.FRONT_WEST ? 4 : 5;
        return Blocks.ladder;
    }

    Block roof(int fx, int ly, int z, int[] meta, int top)
    {
        boolean edge = fx == 0 || fx == b.width - 1 || z == 0 || z == b.length - 1;
        if (ly == top + 1)
        {
            if (edge)
            {
                if (b.unit(fx, ly, z) < 0.12 + b.ruins.decay * 0.3)
                {
                    return null;
                }
                meta[0] = trimMeta;
                return trim;
            }
            return b.ruins.overgrowthOnSurface(fx, ly, z, meta, 0.10);
        }
        return null;
    }

    /** Margin ring: exterior vines (not in cold or dry biomes), else untouched. */
    Block margin(int x, int ly, int z, int[] meta)
    {
        meta[0] = Plan.SKIP;
        int top = b.floors * b.storeyHeight;
        if (b.style == Building.COLD || b.style == Building.DRY || ly < 1 || ly > top)
        {
            return null;
        }
        boolean side = (x == -1 || x == b.width) && z >= 0 && z < b.length
            || (z == -1 || z == b.length) && x >= 0 && x < b.width;
        if (!side)
        {
            return null;
        }
        // vines hang in strands from a random height downward
        double strand = b.unit(x * 17, 99, z * 13);
        double density = b.style == Building.LUSH ? 0.45 : 0.18;
        if (strand > density)
        {
            return null;
        }
        int from = (int) (b.unit(x, 98, z) * top) + 2;
        if (ly > from || ly < from - 3 - (int) (b.unit(x, 97, z) * 10))
        {
            return null;
        }
        // vine meta: which side the supporting wall is on (1 south, 2 west, 4 north, 8 east)
        meta[0] = x == -1 ? 8 : x == b.width ? 2 : z == -1 ? 1 : 4;
        return Blocks.vine;
    }
}
