package net.decimation.worldgen.building;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * The ruin layer of a building: how decayed it is, the collapsed corner,
 * rubble, low debris, overgrowth lying on floors and roofs, looted
 * furniture. Per cell rules for now; the architecture plans a smooth decay
 * field with structural support here (docs/worldgen_architecture.md,
 * migration step 5).
 */
final class Ruins
{
    private final Building b;
    /** 0.15 .. 0.55: share of broken windows, missing doors, lost furniture... */
    final double decay;
    /** Collapsed corner (0..3) and how many top storeys it reaches, or -1. */
    private final int collapseCorner, collapseStoreys;

    Ruins(Building b)
    {
        this.b = b;
        decay = 0.15 + b.unit(12, 12, 12) * 0.40;
        if (b.floors >= 2 && b.unit(13, 13, 13) < 0.45)
        {
            collapseCorner = (int) (b.unit(14, 14, 14) * 4);
            collapseStoreys = 1 + (int) (b.unit(15, 15, 15) * Math.min(3, b.floors - 1));
        }
        else
        {
            collapseCorner = -1;
            collapseStoreys = 0;
        }
    }

    boolean collapsed(int fx, int ly, int z)
    {
        if (collapseCorner < 0)
        {
            return false;
        }
        int fromStorey = b.floors - collapseStoreys;
        int storey = ly / b.storeyHeight;
        if (ly <= fromStorey * b.storeyHeight)
        {
            return false; // the floor of the first collapsed storey holds the rubble
        }
        int cx = (collapseCorner & 1) == 0 ? 0 : b.width - 1;
        int cz = (collapseCorner & 2) == 0 ? 0 : b.length - 1;
        // the hole widens upward: a cone from the corner
        double reach = (Math.min(b.width, b.length) / 2.0)
            * (0.5 + 0.5 * (storey - fromStorey + 1) / (double) collapseStoreys);
        double dist = Math.hypot(fx - cx, z - cz);
        return dist < reach + (b.unit(fx, storey, z) - 0.5) * 2;
    }

    Block rubbleBelowCollapse(int fx, int ly, int z, int[] meta)
    {
        int fromStorey = b.floors - collapseStoreys;
        if (ly == fromStorey * b.storeyHeight + 1 && b.unit(fx, ly, z) < 0.5)
        {
            return debrisBlock(fx, ly, z, meta);
        }
        return null;
    }

    double mossChance()
    {
        switch (b.style)
        {
            case Building.LUSH: return 0.18;
            case Building.TEMPERATE: return 0.08;
            case Building.COLD: return 0.03;
            default: return 0.01;
        }
    }

    /** Leaves, grass, snow or sand lying on a roof or floor. */
    Block overgrowthOnSurface(int fx, int ly, int z, int[] meta, double base)
    {
        double d = b.unit(fx * 3, ly, z * 5);
        switch (b.style)
        {
            case Building.COLD:
                if (d < base * 3)
                {
                    meta[0] = d < base ? 1 : 0;
                    return Blocks.snow_layer;
                }
                return null;
            case Building.DRY:
                if (d < base * 1.5)
                {
                    return Blocks.sand;
                }
                return null;
            case Building.LUSH:
                if (d < base * 2.5)
                {
                    meta[0] = 3 | 4; // jungle leaves, no decay
                    return Blocks.leaves;
                }
                return null;
            default:
                if (d < base)
                {
                    meta[0] = 4; // oak leaves, no decay
                    return Blocks.leaves;
                }
                if (d < base * 1.6)
                {
                    meta[0] = 1;
                    return Blocks.tallgrass;
                }
                return null;
        }
    }

    /** Floor level of an open cell (within 1): low debris along walls, overgrowth near the outside. */
    Block debris(int fx, int ly, int z, int[] meta)
    {
        double d = b.unit(fx, ly, z);
        StoreyPlan sp = b.plan(ly / b.storeyHeight);
        // debris gathers along walls and in corners, not in the middle of a room
        if (d < decay * 0.07 && sp.nearWall(fx, z) && !sp.byDoor(fx, z))
        {
            return lowDebris(fx, ly, z, meta);
        }
        boolean nearOutside = fx <= 1 || z <= 1 || fx >= b.width - 2 || z >= b.length - 2;
        if (nearOutside)
        {
            Block o = overgrowthOnSurface(fx, ly, z, meta, 0.06 + decay * 0.1);
            if (o != null && o != Blocks.leaves)
            {
                return o;
            }
            meta[0] = 0;
        }
        return null;
    }

    /**
     * Debris on a walkable floor: never a full block (the audit found full
     * mossy cobble cubes standing in corridors and doorways). Slabs of
     * cobble / stone brick / brick, a cobweb, a trash bag.
     */
    Block lowDebris(int fx, int ly, int z, int[] meta)
    {
        double d = b.unit(z, ly, fx);
        StoreyPlan sp = b.plan(ly / b.storeyHeight);
        if (d < 0.12 && sp.nearWall(fx, z))
        {
            meta[0] = 0;
            return Blocks.web; // cobwebs in corners and along walls, not mid-room
        }
        if (d < 0.22 && b.props.trashBags.length > 0)
        {
            meta[0] = 2 + (int) (b.unit(fx, 9, z) * 4);
            return b.props.trashBags[(int) (b.unit(z, 9, fx) * b.props.trashBags.length)];
        }
        meta[0] = d < 0.6 ? 3 : d < 0.85 ? 5 : 4; // cobble / stone brick / brick slab
        return Blocks.stone_slab;
    }

    /** Heavy rubble: only under a collapsed ceiling, where full blocks make sense. */
    private Block debrisBlock(int fx, int ly, int z, int[] meta)
    {
        double d = b.unit(z, ly, fx);
        if (d < 0.5)
        {
            meta[0] = 3; // cobblestone slab
            return Blocks.stone_slab;
        }
        meta[0] = 0;
        return d < 0.75 ? Blocks.cobblestone : b.style == Building.DRY ? Blocks.sand : Blocks.mossy_cobblestone;
    }

    /** Looted / thrown around: some furniture is missing, varying per storey. */
    boolean removed(int fx, int z, int storey)
    {
        return b.unit(fx, storey * 31, z) < 0.12 + decay * 0.25;
    }
}
