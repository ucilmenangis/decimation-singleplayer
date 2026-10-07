package net.decimation.worldgen.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.sets.FurnitureSet;
import net.decimation.worldgen.sets.FurnitureSets;
import net.minecraft.block.Block;

/**
 * Places furniture SETS (docs/furniture_sets.md) into room rectangles of a
 * storey plan: against a wall, on free cells, off the kept walkway.
 */
final class Furnisher
{
    // sides of a room rectangle the set's back (row 0) can stand against:
    // 0 = low z wall, 1 = high z wall, 2 = low fx wall, 3 = high fx wall
    private static final int[][] SIDE_OUT = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    private static final int[][] SIDE_ALONG = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
    private static final boolean DEBUG_SETS = System.getProperty("deciworldgen.debugsets") != null;

    private final Building b;

    Furnisher(Building b)
    {
        this.b = b;
    }

    /**
     * Places one set for this room kind into the rectangle, against one of
     * its walls, all cells free and off the kept walkway; tries the sets
     * (weighted, seeded order), every wall side and every offset. Returns
     * whether one was placed.
     */
    boolean furnish(StoreyPlan sp, boolean[][] keep, int fx0, int z0, int fx1, int z1, String room, int salt)
    {
        List<FurnitureSet> sets = new ArrayList<FurnitureSet>();
        String kindName = Building.KIND_NAME[b.kind];
        for (FurnitureSet set : FurnitureSets.forRoom(room))
        {
            if (set.when.test(kindName, sp.ground, b.floors))
            {
                sets.add(set);
            }
        }
        if (sets.isEmpty() || fx1 < fx0 || z1 < z0)
        {
            return false;
        }
        List<FurnitureSet> order = new ArrayList<FurnitureSet>(sets);
        // weighted shuffle: sort by -log(u) / weight
        final Map<FurnitureSet, Double> key = new HashMap<FurnitureSet, Double>();
        for (int i = 0; i < order.size(); i++)
        {
            double u = Math.max(1e-6, b.unit(salt, i * 31 + 7, fx0 + z0));
            key.put(order.get(i), -Math.log(u) / Math.max(1, order.get(i).weight));
        }
        Collections.sort(order, new Comparator<FurnitureSet>()
        {
            public int compare(FurnitureSet x, FurnitureSet y)
            {
                return Double.compare(key.get(x), key.get(y));
            }
        });
        int sideStart = (int) (b.unit(fx0, salt, z0) * 4);
        for (FurnitureSet set : order)
        {
            for (int si = 0; si < 4; si++)
            {
                int side = (sideStart + si) % 4;
                int len = side < 2 ? fx1 - fx0 + 1 : z1 - z0 + 1;
                int span = len - set.width + 1;
                if (span <= 0)
                {
                    continue;
                }
                int offStart = (int) (b.unit(fx0 + si, salt + 1, z0) * span);
                for (int oi = 0; oi < span; oi++)
                {
                    int off = (offStart + oi) % span;
                    if (fits(sp, keep, set, side, off, fx0, z0, fx1, z1))
                    {
                        placeSet(sp, keep, set, side, off, fx0, z0, fx1, z1, salt);
                        if (DEBUG_SETS)
                        {
                            FMLLog.info("[deciworldgen] sets: ok %s %s %s room %dx%d (fx %d..%d z %d..%d)",
                                        room, set.name, b.id, fx1 - fx0 + 1, z1 - z0 + 1, fx0, fx1, z0, z1);
                        }
                        return true;
                    }
                }
            }
        }
        if (DEBUG_SETS)
        {
            FMLLog.info("[deciworldgen] sets: no %s fits %s room %dx%d (fx %d..%d z %d..%d)",
                        room, b.id, fx1 - fx0 + 1, z1 - z0 + 1, fx0, fx1, z0, z1);
        }
        return false;
    }

    /** Plan cell of set position (r, c) for a side and offset: {fx, z}. */
    private static int[] setCell(int side, int r, int c, int off, int fx0, int z0, int fx1, int z1)
    {
        switch (side)
        {
            case 0: return new int[] {fx0 + off + c, z0 + r};
            case 1: return new int[] {fx1 - off - c, z1 - r};
            case 2: return new int[] {fx0 + r, z1 - off - c};
            default: return new int[] {fx1 - r, z0 + off + c};
        }
    }

    private boolean fits(StoreyPlan sp, boolean[][] keep, FurnitureSet set, int side, int off,
                         int fx0, int z0, int fx1, int z1)
    {
        for (int y = 0; y < set.layers.length; y++)
        {
            for (int r = 0; r < set.depth; r++)
            {
                for (int c = 0; c < set.width; c++)
                {
                    char ch = set.at(y, r, c);
                    if (ch == ' ')
                    {
                        continue;
                    }
                    int[] p = setCell(side, r, c, off, fx0, z0, fx1, z1);
                    if (p[0] < fx0 || p[0] > fx1 || p[1] < z0 || p[1] > z1)
                    {
                        return false;
                    }
                    byte cell = sp.cells[p[0]][p[1]];
                    char base = set.at(0, r, c);
                    boolean ownBase = base != ' ' && base != '.';
                    if (cell != StoreyPlan.OPEN && !(y > 0 && ownBase))
                    {
                        return false;
                    }
                    if (ch != '.' && keep[p[0]][p[1]] && y == 0)
                    {
                        return false;
                    }
                    if (y == 0 && r == 0 && ch != '.')
                    {
                        // the back must stand against a wall, never in front of a door or opening
                        int[] o = SIDE_OUT[side];
                        int bx = p[0] - o[0], bz = p[1] - o[1];
                        byte back = bx <= 0 || bz <= 0 || bx >= b.width - 1 || bz >= b.length - 1
                            ? StoreyPlan.WALL : sp.cells[bx][bz];
                        if (back != StoreyPlan.WALL && back != StoreyPlan.LINING && back != StoreyPlan.GLASS)
                        {
                            return false;
                        }
                        if (back == StoreyPlan.LINING && set.at(2, r, c) != ' ' && set.at(2, r, c) != '.'
                            && wallOpening(bx, bz))
                        {
                            return false; // full height pieces (wardrobe, wall cabinets) never cover a window
                        }
                    }
                }
            }
        }
        return true;
    }

    /** True when a lining cell is a window recess or passage on any storey height (checked on storey 1). */
    private boolean wallOpening(int fx, int z)
    {
        return b.shell.liningOpen(fx, z, 1, 1) || b.shell.liningOpen(fx, z, 1, 2)
            || b.shell.liningOpen(fx, z, 0, 1) || b.shell.liningOpen(fx, z, 0, 2);
    }

    private void placeSet(StoreyPlan sp, boolean[][] keep, FurnitureSet set, int side, int off,
                          int fx0, int z0, int fx1, int z1, int salt)
    {
        int[] o = SIDE_OUT[side], a = SIDE_ALONG[side];
        double styleU = b.unit(salt + side, off * 5 + 17, fx0 * 31 + z0); // one style palette per placement
        for (int y = 0; y < set.layers.length && y < 3; y++)
        {
            for (int r = 0; r < set.depth; r++)
            {
                for (int c = 0; c < set.width; c++)
                {
                    char ch = set.at(y, r, c);
                    if (ch == ' ')
                    {
                        continue;
                    }
                    int[] p = setCell(side, r, c, off, fx0, z0, fx1, z1);
                    if (ch == '.')
                    {
                        keep[p[0]][p[1]] = true;
                        continue;
                    }
                    FurnitureSet.Entry e = set.entry(ch, styleU);
                    if (e == null || e.blocks.isEmpty())
                    {
                        continue;
                    }
                    Block blk = e.blocks.get((int) (b.unit(p[0] + salt, y * 7 + 3, p[1]) * e.blocks.size()));
                    int meta = setMeta(e, o, a);
                    sp.cells[p[0]][p[1]] = StoreyPlan.FURN;
                    keep[p[0]][p[1]] = true;
                    if (y == 0)
                    {
                        sp.furn[p[0]][p[1]] = blk;
                        sp.furnMeta[p[0]][p[1]] = (byte) meta;
                    }
                    else if (y == 1)
                    {
                        sp.furn2[p[0]][p[1]] = blk;
                        sp.furn2Meta[p[0]][p[1]] = (byte) meta;
                    }
                    else
                    {
                        sp.furn3[p[0]][p[1]] = blk;
                        sp.furn3Meta[p[0]][p[1]] = (byte) meta;
                    }
                }
            }
        }
    }

    /** Metadata of a palette entry placed with wall-out vector o and along vector a (plan coords). */
    private int setMeta(FurnitureSet.Entry e, int[] o, int[] a)
    {
        if (e.face == null)
        {
            return e.meta;
        }
        int dfx, dz;
        if ("in".equals(e.face)) { dfx = -o[0]; dz = -o[1]; }
        else if ("right".equals(e.face)) { dfx = a[0]; dz = a[1]; }
        else if ("left".equals(e.face)) { dfx = -a[0]; dz = -a[1]; }
        else { dfx = o[0]; dz = o[1]; }
        String type = e.type != null ? e.type : "prop";
        if ("vanilla".equals(type)) return Facing.vanilla(b.front, dfx, dz);
        if ("seat".equals(type)) return Facing.seat(b.front, dfx, dz);
        if ("bed".equals(type)) return Facing.bed(b.front, dfx, dz) | ("head".equals(e.part) ? 8 : 0);
        if ("meta".equals(type)) return e.meta;
        if ("trapdoor".equals(type) || "hook".equals(type))
        {
            // face = where the supporting block is; trapdoor: open panel
            // flat against it (cabinet door), hook: a tap on the wall
            int wx = Facing.worldDx(b.front, dfx);
            int side = "trapdoor".equals(type)
                ? (dz > 0 ? 0 : dz < 0 ? 1 : wx > 0 ? 2 : 3)
                : (dz < 0 ? 0 : wx > 0 ? 1 : dz > 0 ? 2 : 3);
            return "trapdoor".equals(type) ? side | 4 : side;
        }
        return Facing.prop(b.front, dfx, dz);
    }
}
