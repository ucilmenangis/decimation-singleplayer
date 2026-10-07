package net.decimation.worldgen.building;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Apartment storey: a corridor along fx through the middle (deep blocks)
 * or along the low z side, flats on one or both sides, each furnished with
 * furniture sets. Interior starts at 2 (ring 1 is the wall lining).
 */
final class ApartmentPlanner
{
    private final Building b;

    ApartmentPlanner(Building b)
    {
        this.b = b;
    }

    void plan(StoreyPlan sp)
    {
        sp.markCore(b.coreFx, b.coreFz, b.coreLen);
        // corridor rows: middle (double loaded) or along the low z side
        int c0 = b.doubleLoaded ? b.length / 2 - 1 : 2, c1 = b.doubleLoaded ? b.length / 2 : 3;
        int end = b.coreFx >= 0 ? b.coreFx - 1 : b.width - 3;
        sp.markRoom(1, c0, end, c1, StoreyPlan.R_CORRIDOR);
        if (b.doubleLoaded)
        {
            sp.wallLine(2, c0 - 1, end, c0 - 1, StoreyPlan.WALL);
        }
        sp.wallLine(2, c1 + 1, end, c1 + 1, StoreyPlan.WALL);
        int fx = 2;
        int unitNo = 0;
        while (fx < end - 2)
        {
            int len = Math.min(5 + (int) (b.unit(fx, unitNo, 41) * 4), end - fx);
            int ux0 = fx, ux1 = fx + len - 1;
            if (ux1 < end - 1)
            {
                sp.wallLine(ux1 + 1, 2, ux1 + 1, c0 - 2, StoreyPlan.WALL);
                sp.wallLine(ux1 + 1, c1 + 2, ux1 + 1, b.length - 3, StoreyPlan.WALL);
            }
            // entry door near a corner (as in real flats): leaves a long free
            // wall for the kitchen run; the side alternates per flat
            boolean entryLow = b.unit(ux0, unitNo, 42) < 0.5;
            int door = entryLow ? ux0 : ux1;
            if (b.doubleLoaded)
            {
                sp.cells[door][c0 - 1] = StoreyPlan.DOOR;
                unit(sp, ux0, ux1, 2, c0 - 2, false, sp.ground && fx <= 3, door);
            }
            sp.cells[door][c1 + 1] = StoreyPlan.DOOR;
            // lobby on one side of the entrance only, a furnished flat opposite
            unit(sp, ux0, ux1, c1 + 2, b.length - 3, true, sp.ground && fx <= 3 && !b.doubleLoaded, door);
            fx = ux1 + 2;
            unitNo++;
        }
    }

    /**
     * One flat, furnished with furniture SETS (docs/furniture_sets.md):
     * living part on the corridor side (kitchen set, living set, dining
     * set), bedroom on the window side (bed, wardrobe, desk), a bathroom in
     * wider flats. Rows k count from the corridor. The walking line from the
     * entry door to the bedroom (and bathroom) door is kept free.
     */
    private void unit(StoreyPlan sp, int fx0, int fx1, int z0, int z1, boolean corridorAtLowZ, boolean lobby,
                      int entry)
    {
        if (z1 - z0 < 1 || fx1 - fx0 < 2)
        {
            return;
        }
        Furnisher f = b.furnisher;
        boolean[][] keep = new boolean[b.width][b.length];
        int salt = fx0 * 7 + z0 * 13 + (sp.ground ? 0 : 1000);
        keep[entry][row(z0, z1, corridorAtLowZ, 0)] = true;
        if (lobby)
        {
            sp.markRoom(fx0, z0, fx1, z1, StoreyPlan.R_LOBBY);
            f.furnish(sp, keep, fx0, z0, fx1, z1, "lobby", salt);
            f.furnish(sp, keep, fx0, z0, fx1, z1, "lobby", salt + 1);
            return;
        }
        final int depth = z1 - z0 + 1, w = fx1 - fx0 + 1;
        if (w < 4 || depth < 3)
        {
            // a leftover sliver is no flat: a storage closet
            sp.markRoom(fx0, z0, fx1, z1, StoreyPlan.R_STORAGE);
            f.furnish(sp, keep, fx0, z0, fx1, z1, "closet", salt);
            return;
        }
        int ks = depth >= 5 ? depth / 2 : -1;    // split wall row (k), -1 = studio
        boolean bath = ks >= 0 && w >= 5 && depth - ks - 1 >= 2;
        boolean entryLow = entry == fx0;
        int bedDoor = entry; // bedroom door in line with the entry; the bathroom takes the far end
        sp.markRoom(fx0, z0, fx1, z1, StoreyPlan.R_LIVING);
        int livingRows = ks >= 0 ? ks : depth;
        for (int k = 0; k < livingRows; k++)
        {
            keep[entry][row(z0, z1, corridorAtLowZ, k)] = true; // straight line to the bedroom door
        }
        int lz0 = Math.min(row(z0, z1, corridorAtLowZ, 0), row(z0, z1, corridorAtLowZ, livingRows - 1));
        int lz1 = Math.max(row(z0, z1, corridorAtLowZ, 0), row(z0, z1, corridorAtLowZ, livingRows - 1));
        if (ks >= 0)
        {
            int zs = row(z0, z1, corridorAtLowZ, ks);
            sp.wallLine(fx0, zs, fx1, zs, StoreyPlan.WALL);
            sp.cells[bedDoor][zs] = StoreyPlan.DOOR;
            keep[bedDoor][row(z0, z1, corridorAtLowZ, ks + 1)] = true;
            keep[bedDoor][row(z0, z1, corridorAtLowZ, ks - 1)] = true;
            if (bedDoor != entry)
            {
                int zr = row(z0, z1, corridorAtLowZ, ks - 1);
                for (int fx = Math.min(entry, bedDoor); fx <= Math.max(entry, bedDoor); fx++)
                {
                    keep[fx][zr] = true;
                }
            }
            for (int k = ks + 1; k < depth; k++)
            {
                sp.markRoom(fx0, row(z0, z1, corridorAtLowZ, k), fx1, row(z0, z1, corridorAtLowZ, k),
                            StoreyPlan.R_BEDROOM);
            }
        }
        // ---- living part: kitchen run first (it needs a long wall); a studio
        // places its bed next, then living and dining fill what is left
        f.furnish(sp, keep, fx0, lz0, fx1, lz1, "kitchen", salt);
        markKitchenFloor(sp, fx0, lz0, fx1, lz1);
        if (ks < 0)
        {
            f.furnish(sp, keep, fx0, lz0, fx1, lz1, "bed", salt + 4);
        }
        f.furnish(sp, keep, fx0, lz0, fx1, lz1, "living", salt + 2);
        f.furnish(sp, keep, fx0, lz0, fx1, lz1, "dining", salt + 3);
        if (ks < 0)
        {
            return;
        }
        // ---- bedroom (+ bathroom in the 2 far columns)
        int bz0 = Math.min(row(z0, z1, corridorAtLowZ, ks + 1), row(z0, z1, corridorAtLowZ, depth - 1));
        int bz1 = Math.max(row(z0, z1, corridorAtLowZ, ks + 1), row(z0, z1, corridorAtLowZ, depth - 1));
        int bedFrom = fx0, bedTo = fx1;
        if (bath)
        {
            // bathroom: the 2 columns at the end away from the entry
            int wx = entryLow ? fx1 - 2 : fx0 + 2;
            int b0 = entryLow ? fx1 - 1 : fx0, b1 = entryLow ? fx1 : fx0 + 1;
            sp.wallLine(wx, bz0, wx, bz1, StoreyPlan.WALL);
            int zd = row(z0, z1, corridorAtLowZ, ks + 1);
            sp.cells[wx][zd] = StoreyPlan.DOOR;
            keep[wx - 1][zd] = true;
            keep[wx + 1][zd] = true;
            sp.markRoom(b0, bz0, b1, bz1, StoreyPlan.R_BATH);
            f.furnish(sp, keep, b0, bz0, b1, bz1, "bath", salt + 5);
            f.furnish(sp, keep, b0, bz0, b1, bz1, "bath", salt + 6);
            if (entryLow)
            {
                bedTo = wx - 1;
            }
            else
            {
                bedFrom = wx + 1;
            }
        }
        f.furnish(sp, keep, bedFrom, bz0, bedTo, bz1, "bed", salt + 7);
        f.furnish(sp, keep, bedFrom, bz0, bedTo, bz1, "storage", salt + 8);
        f.furnish(sp, keep, bedFrom, bz0, bedTo, bz1, "desk", salt + 9);
    }

    /** Tiles under the kitchen run: the cells where a kitchen set stood (fridge, counters, oven, sink). */
    private void markKitchenFloor(StoreyPlan sp, int fx0, int z0, int fx1, int z1)
    {
        for (int fx = fx0; fx <= fx1; fx++)
        {
            for (int z = z0; z <= z1; z++)
            {
                Block k = sp.furn[fx][z];
                if (k == Blocks.furnace || k == Blocks.cauldron || k == Blocks.double_stone_slab
                    || k == Blocks.quartz_block || k == b.deci("BlockElectricBoxBin") || k == b.deci("BlockWashingMachine"))
                {
                    sp.markRoom(fx - 1, z - 1, fx + 1, z + 1, StoreyPlan.R_KITCHEN);
                }
            }
        }
    }

    /** z of row k of a flat, counted from the corridor side. */
    private static int row(int z0, int z1, boolean corridorAtLowZ, int k)
    {
        return corridorAtLowZ ? z0 + k : z1 - k;
    }
}
