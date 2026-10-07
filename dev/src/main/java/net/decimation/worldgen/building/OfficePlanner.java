package net.decimation.worldgen.building;

import net.minecraft.init.Blocks;

/**
 * Office storey: ground = lobby with reception and desk rows; upper =
 * meeting room behind glass in the front corner, a storage room beside the
 * stair core, open plan desk rows.
 */
final class OfficePlanner
{
    private final Building b;

    OfficePlanner(Building b)
    {
        this.b = b;
    }

    private int face(int dfx, int dz)
    {
        return Facing.prop(b.front, dfx, dz);
    }

    void plan(StoreyPlan sp)
    {
        Building.Props props = b.props;
        sp.markCore(b.coreFx, b.coreFz, b.coreLen);
        int coreEnd = b.coreFx >= 0 ? b.coreFx - 2 : b.width - 3;
        int length = b.length;
        if (sp.ground)
        {
            sp.markRoom(1, 1, 7, length - 2, StoreyPlan.R_LOBBY);
            // reception desk facing the entrance, waiting chairs, vending machine
            int mid = length / 2;
            for (int z = mid - 2; z <= mid + 2; z++)
            {
                sp.put(5, z, Blocks.double_stone_slab, 0);
            }
            sp.put(6, mid, props.officeChair, face(-1, 0));
            sp.put(2, 1, props.chair, face(1, 0));
            sp.put(3, 1, props.chair, face(1, 0));
            sp.put(2, length - 2, props.vending, face(0, -1));
            sp.put(1, 1, Blocks.flower_pot, 0);
            sp.put(1, length - 2, Blocks.flower_pot, 0);
            deskRows(sp, 8, coreEnd);
            return;
        }
        // meeting room in the front corner, glass partition, long table
        int mx1 = Math.min(6, coreEnd), mz1 = Math.min(5, length / 2 - 2);
        if (mx1 >= 4 && mz1 >= 3)
        {
            sp.markRoom(1, 1, mx1, mz1, StoreyPlan.R_MEETING);
            sp.wallLine(1, mz1 + 1, mx1, mz1 + 1, StoreyPlan.GLASS);
            sp.wallLine(mx1 + 1, 1, mx1 + 1, mz1 + 1, StoreyPlan.GLASS);
            sp.cells[mx1 + 1][mz1 / 2 + 1] = StoreyPlan.DOOR;
            for (int fx = 2; fx <= mx1 - 1; fx++)
            {
                sp.put(fx, (1 + mz1) / 2 + 1, props.metalTable, 2);
                sp.put(fx, (1 + mz1) / 2, props.officeChair, face(0, 1));
                sp.put(fx, (1 + mz1) / 2 + 2, props.officeChair, face(0, -1));
            }
        }
        // storage room beside the core: shelves and crates
        if (b.coreFx >= 0 && b.coreFz >= 4)
        {
            sp.markRoom(b.coreFx + 1, 1, b.width - 2, b.coreFz - 2, StoreyPlan.R_STORAGE);
            sp.wallLine(b.coreFx, b.coreFz - 3, b.coreFx, b.coreFz - 2, StoreyPlan.WALL);
            for (int fx = b.coreFx + 1; fx <= b.width - 2; fx++)
            {
                sp.put(fx, 1, props.shelf, face(0, 1));
            }
            sp.put(b.coreFx + 2, b.coreFz - 2, props.policeCrate, 2);
            sp.put(b.coreFx + 4, b.coreFz - 2, props.woodCrate, 2);
        }
        deskRows(sp, mx1 + 3, coreEnd);
    }

    /** Open plan: desk + chair pairs in rows, a gap every few desks. */
    private void deskRows(StoreyPlan sp, int fxFrom, int fxTo)
    {
        Building.Props props = b.props;
        for (int fx = fxFrom; fx <= fxTo; fx += 4)
        {
            for (int z = 2; z <= b.length - 3; z++)
            {
                if (z % 4 == 0 || z % 4 == 3)
                {
                    continue; // desks in pairs, then an aisle
                }
                sp.put(fx, z, props.metalTable != null && b.unit(fx, z, 61) < 0.6 ? props.metalTable : props.table, 2);
                sp.put(fx + 1, z, props.officeChair, face(-1, 0));
            }
        }
    }
}
