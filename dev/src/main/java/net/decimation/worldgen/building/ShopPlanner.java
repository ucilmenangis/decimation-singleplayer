package net.decimation.worldgen.building;

import net.minecraft.init.Blocks;

/**
 * Shop storey: ground = sales floor (checkout, shelf aisles) with a
 * stockroom behind a wall at the back; upper storeys = stock and an office
 * corner.
 */
final class ShopPlanner
{
    private final Building b;

    ShopPlanner(Building b)
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
        int width = b.width, length = b.length;
        boolean ground = sp.ground;
        int stockWall = Math.max(5, width - 5);
        sp.markRoom(1, 1, stockWall - 1, length - 2, ground ? StoreyPlan.R_SHOP : StoreyPlan.R_STOCK);
        sp.markRoom(stockWall + 1, 1, width - 2, length - 2, StoreyPlan.R_STOCK);
        sp.wallLine(stockWall, 1, stockWall, length - 2, StoreyPlan.WALL);
        sp.cells[stockWall][length / 2] = StoreyPlan.DOOR;
        // stockroom: crates, boxes, shelves on the back wall
        for (int z = 1; z <= length - 2; z++)
        {
            double d = b.unit(width, z, 71);
            if (d < 0.25)
            {
                sp.put(width - 2, z, props.cardboard, (int) (d * 16) % 4 + 2);
            }
            else if (d < 0.45)
            {
                sp.put(width - 2, z, props.shelf, face(-1, 0));
            }
        }
        sp.put(stockWall + 2, 2, props.woodCrate, 2);
        sp.put(stockWall + 2, length - 3, b.unit(3, 3, 72) < 0.5 ? props.medicalCrate : props.ammoCrate, 2);
        if (!ground)
        {
            sp.put(2, 2, props.table, 2);
            sp.put(3, 2, props.officeChair, face(-1, 0));
            sp.put(2, length - 3, props.woodCrate, 2);
            return;
        }
        // checkout at the front left of the entrance, aisles behind a free zone
        sp.put(2, 1, Blocks.double_stone_slab, 0);
        sp.put(2, 2, Blocks.double_stone_slab, 0);
        sp.put(3, 1, props.officeChair, face(-1, 0));
        sp.put(1, length - 2, props.vending, face(1, 0));
        for (int z = 3; z <= length - 3; z += 3)
        {
            for (int fx = 4; fx <= stockWall - 2; fx++)
            {
                sp.put(fx, z, props.shelf, face(0, 1));
            }
        }
        sp.put(stockWall - 1, 1, props.trashcan, 2);
    }
}
