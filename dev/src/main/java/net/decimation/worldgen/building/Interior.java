package net.decimation.worldgen.building;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Turns a storey plan cell into blocks above the floor layer: inner walls
 * (breached by decay), glass partitions, doors with lintels, the lining,
 * furniture layers, the ceiling layer, lights and floor debris.
 */
final class Interior
{
    private final Building b;

    Interior(Building b)
    {
        this.b = b;
    }

    /** Block at an inner cell (not edge, not core, not ladder), within 1 .. ceil. */
    Block cell(int fx, int ly, int z, int storey, int within, int[] meta)
    {
        StoreyPlan sp = b.plan(storey);
        Surfaces s = b.surfaces;
        if (within == b.ceil())
        {
            return s.ceilingLayer(sp.cells[fx][z], sp.rooms[fx][z], fx, storey, z, meta);
        }
        int top = b.ceil() - 1; // the layer under the ceiling
        switch (sp.cells[fx][z])
        {
            case StoreyPlan.WALL:
                // decay takes out whole wall columns (a breach), never single
                // blocks: those left plaster lumps floating at mid height and
                // made flats see-through (critic review 0.20)
                if (within == top || b.unit(fx, storey * 7 + 11, z) > b.ruins.decay * 0.18)
                {
                    return s.wallPanel(within, meta);
                }
                return null;
            case StoreyPlan.GLASS:
                if (within <= 2 && b.unit(fx, ly, z) > 0.5 + b.ruins.decay * 0.3)
                {
                    return Blocks.glass_pane;
                }
                return within == top ? s.lintel(meta) : null;
            case StoreyPlan.DOOR:
                if (within == top)
                {
                    return s.lintel(meta);
                }
                return s.door(sp, fx, z, storey, within, meta);
            case StoreyPlan.LINING:
                if (b.shell.liningOpen(fx, z, storey, within))
                {
                    return null; // window recess / entrance passage
                }
                return s.lining(within, meta);
            case StoreyPlan.FURN:
                if (within == top)
                {
                    Block b3 = sp.furn3[fx][z];
                    if (b3 != null)
                    {
                        meta[0] = sp.furn3Meta[fx][z];
                        return b3;
                    }
                    return s.fixture(sp.rooms[fx][z], fx, z, storey, meta);
                }
                if (within == 2)
                {
                    Block b2 = sp.furn2[fx][z];
                    if (b2 != null)
                    {
                        meta[0] = sp.furn2Meta[fx][z];
                    }
                    return b2;
                }
                if (within != 1)
                {
                    return null;
                }
                return furniture(sp, fx, z, storey, meta);
            default:
                if (within == 1)
                {
                    return b.ruins.debris(fx, ly, z, meta);
                }
                if (within == top)
                {
                    return s.fixture(sp.rooms[fx][z], fx, z, storey, meta);
                }
                return null;
        }
    }

    private Block furniture(StoreyPlan sp, int fx, int z, int storey, int[] meta)
    {
        Block f = sp.furn[fx][z];
        if (f == null)
        {
            return null;
        }
        // looted / thrown around: some furniture missing, loot varies per storey
        // (never half a bed or the base of a 2 high piece)
        if (f != Blocks.bed && sp.furn2[fx][z] == null && b.ruins.removed(fx, z, storey))
        {
            return b.unit(fx, storey * 33, z) < 0.5 ? b.ruins.lowDebris(fx, storey * b.storeyHeight + 1, z, meta) : null;
        }
        if (isCrate(f) && b.unit(storey, fx, z) < 0.45)
        {
            return null;
        }
        meta[0] = sp.furnMeta[fx][z];
        return f;
    }

    private boolean isCrate(Block f)
    {
        Building.Props p = b.props;
        return f == p.woodCrate || f == p.medicalCrate || f == p.policeCrate || f == p.ammoCrate;
    }
}
