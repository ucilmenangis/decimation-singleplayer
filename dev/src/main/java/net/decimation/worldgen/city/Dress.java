package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.decimation.worldgen.military.PropBoxes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWeb;
import net.minecraft.init.Blocks;

/**
 * Props added on top of a plan's own blocks (city dressing, docs/city_engine.md "City dressing"):
 * a sparse overlay in the plan's local coordinates. Every prop goes in through {@link #place},
 * which accepts it only when it fits as tools/props/propclash.py would judge it: its cell empty,
 * its drawn box (PropBoxes, at the metadata given) clear of every block and of every prop already
 * there (the plan's and the overlay's), nothing drawn outside the plan (no prop reaches into a
 * neighbour chunk written by another plan), the cells above a tall prop free, standing on
 * something, never in a door's walkway. A prop on a bottom half slab gets the slab under it
 * made whole (a raised sidewalk is half slabs; a prop on one would float half a block).
 */
final class Dress
{
    /** The plan's own blocks in its local coordinates (null = air or outside). */
    interface Base
    {
        Block block(int x, int y, int z);

        int meta(int x, int y, int z);
    }

    /** City dressing switch: on (since v0.50.0); -Ddeciworldgen.dress=false turns it off. */
    static final boolean ON = !"false".equals(System.getProperty("deciworldgen.dress"));

    private static final int[][] SIDES = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    final int w, h, l;
    private final Base base;
    private final Map<Integer, Block> blocks = new HashMap<Integer, Block>();
    private final Map<Integer, Integer> metas = new HashMap<Integer, Integer>();
    private final List<float[]> boxes = new ArrayList<float[]>();
    /** Per placed box: its block and anchor {x, y, z}. */
    private final List<Object[]> boxOwners = new ArrayList<Object[]>();
    /** Props placed, refused (did not fit) per prop name: the dev test and the log read it. */
    final Map<String, int[]> tally = new HashMap<String, int[]>();
    /** A garrison the dressing laid out (wasteland bunker), local cells: {spawners, s..., posts...}; null none. */
    int[] garrison;
    /** Refusal reasons (index = fits() result): placed, taken, out of plan, no support, doorway, block, prop, faces a wall. */
    static final String[] WHY = {"placed", "taken", "outside", "unsupported", "doorway", "block", "prop", "faces wall"};
    private static final Map<String, int[]> TOTAL = new java.util.TreeMap<String, int[]>();
    private static int built;

    /** Counts an event in the totals log (debugging the rules: chunks per kind, sides without walk). */
    static void note(String key)
    {
        synchronized (TOTAL)
        {
            int[] all = TOTAL.get(key);
            if (all == null)
            {
                all = new int[WHY.length];
                TOTAL.put(key, all);
            }
            all[0]++;
        }
    }

    /** Logs the placed / refused totals of all props so far (every 100 dressed plans). */
    static void logTotals()
    {
        synchronized (TOTAL)
        {
            if (++built % 100 != 0)
            {
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, int[]> e : TOTAL.entrySet())
            {
                sb.append(' ').append(e.getKey().replace("deci:", "")).append('=');
                for (int k = 0; k < WHY.length; k++)
                {
                    if (e.getValue()[k] > 0)
                    {
                        sb.append(k == 0 ? "" : "/" + WHY[k] + ":").append(e.getValue()[k]);
                    }
                }
            }
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] dress totals after %d plans:%s", built, sb);
        }
    }

    Dress(Base base, int w, int h, int l)
    {
        this.base = base;
        this.w = w;
        this.h = h;
        this.l = l;
    }

    private int key(int x, int y, int z)
    {
        return (y * l + z) * w + x;
    }

    private boolean inside(int x, int y, int z)
    {
        return x >= 0 && y >= 0 && z >= 0 && x < w && y < h && z < l;
    }

    /** The overlay's block at a cell, or null (then the plan's own block stands). */
    Block at(int x, int y, int z, int[] meta)
    {
        if (blocks.isEmpty() || !inside(x, y, z))
        {
            return null;
        }
        int k = key(x, y, z);
        Block b = blocks.get(k);
        if (b != null)
        {
            meta[0] = metas.get(k);
        }
        return b;
    }

    boolean isEmpty()
    {
        return blocks.isEmpty();
    }

    /** What stands at a cell now: the overlay over the plan. */
    Block block(int x, int y, int z)
    {
        if (!inside(x, y, z))
        {
            return null;
        }
        Block b = blocks.get(key(x, y, z));
        return b != null ? (b == Blocks.air ? null : b) : base.block(x, y, z);
    }

    int meta(int x, int y, int z)
    {
        Integer m = metas.get(key(x, y, z));
        return m != null ? m : base.meta(x, y, z);
    }

    /** Puts a block without checks (support pads, plain blocks the rules chose). */
    void put(Block b, int m, int x, int y, int z)
    {
        if (inside(x, y, z))
        {
            blocks.put(key(x, y, z), b == null ? Blocks.air : b);
            metas.put(key(x, y, z), m);
        }
    }

    /** A full block, as a wall or a floor. */
    boolean solid(int x, int y, int z)
    {
        Block b = block(x, y, z);
        return b != null && (b.isOpaqueCube() || PropGeom.fullGlass(b)
                             || b instanceof BlockSlab && (meta(x, y, z) & 8) != 0);
    }

    /** Something a front must not look straight into, as propclash.py's FACE: any block over 0.6 high
     * that is not glass (a seat may look out of a window), not a prop. */
    boolean blocksFront(int x, int y, int z)
    {
        Block k = block(x, y, z);
        if (k == null || PropBoxes.box(k, meta(x, y, z)) != null || k.getMaterial() == net.minecraft.block.material.Material.glass)
        {
            return false;
        }
        float[] q = PropGeom.shape(k, meta(x, y, z));
        return q != null && q[3] - q[2] > 0.6f;
    }

    /** Something a standing prop rests on (as propclash.py counts it). */
    boolean supports(int x, int y, int z)
    {
        Block b = block(x, y, z);
        if (b == null)
        {
            return false;
        }
        float[] box = PropBoxes.box(b, meta(x, y, z));
        if (box != null)
        {
            return box[3] >= 0.7f;
        }
        return floorBlock(b, meta(x, y, z)) || b instanceof BlockFence || b instanceof BlockWall || b instanceof BlockChest;
    }

    /**
     * A block with a top a prop can stand on: a full block, a slab, stairs, glass, leaves, a closed
     * trapdoor. Not an open trapdoor (it stands upright against a side), wire, a fence gate, a plant.
     */
    static boolean floorBlock(Block b, int m)
    {
        if (b instanceof net.minecraft.block.BlockTrapDoor)
        {
            return (m & 4) == 0;
        }
        if (b == Blocks.bedrock)
        {
            return false;                                 // BuildingDress's "keep the world" marker: unknown ground
        }
        return b.isOpaqueCube() || b instanceof BlockSlab || b instanceof BlockStairs || PropGeom.fullGlass(b)
            || b instanceof net.minecraft.block.BlockLeaves || b instanceof net.minecraft.block.BlockGrass
            || b instanceof net.minecraft.block.BlockFarmland || b instanceof net.minecraft.block.BlockSoulSand;
    }

    /** Is (x, y, z) in front of or behind a door (its walkway)? */
    boolean inDoorway(int x, int y, int z)
    {
        for (int[] f : SIDES)
        {
            for (int dy = 0; dy >= -1; dy--)
            {
                Block k = block(x + f[0], y + dy, z + f[1]);
                if (k == null || !PropGeom.isDoor(k))
                {
                    continue;
                }
                int m = meta(x + f[0], y + dy, z + f[1]);
                if ((m & 8) != 0)
                {
                    Block low = block(x + f[0], y + dy - 1, z + f[1]);
                    m = low != null && PropGeom.isDoor(low) ? meta(x + f[0], y + dy - 1, z + f[1]) : 0;
                }
                if (((m & 1) == 0) == (f[0] != 0))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Shelves and cabinets in the plan, its own (converted buildings) and the dressing's, as this kind of place's loot
     * (fixes/LootBlocks, feature 12 B: police, military, else civilian); returns how many changed.
     */
    int lootKinds(int place)
    {
        int n = 0;
        for (int y = 0; y < h; y++)
        {
            for (int z = 0; z < l; z++)
            {
                for (int x = 0; x < w; x++)
                {
                    Block b = block(x, y, z);
                    Block to = net.decimation.fixes.LootBlocks.forPlace(b, place);
                    if (to != b)
                    {
                        put(to, meta(x, y, z), x, y, z);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * Places a prop if it fits (see the class comment). mount: 0 standing (needs support below),
     * 1 hung on a wall or a ceiling (no support needed; the caller picked the wall / ceiling and its
     * metadata), 2 lying in the road (support below, may touch nothing). Returns true when placed.
     */
    boolean place(Block b, int m, int x, int y, int z, int mount)
    {
        String name = String.valueOf(Block.blockRegistry.getNameForObject(b));
        int[] t = tally.get(name);
        if (t == null)
        {
            t = new int[2];
            tally.put(name, t);
        }
        int why = b == null ? 1 : fits(b, m, x, y, z, mount);
        synchronized (TOTAL)
        {
            int[] all = TOTAL.get(name);
            if (all == null)
            {
                all = new int[WHY.length];
                TOTAL.put(name, all);
            }
            all[why]++;
        }
        if (why != 0)
        {
            t[1]++;
            return false;
        }
        if (mount != 1)
        {
            Block below = block(x, y - 1, z);
            if (below instanceof BlockSlab && (meta(x, y - 1, z) & 8) == 0 && !below.isOpaqueCube())
            {
                Block whole = wholeSlab(below);
                if (whole != null)
                {
                    put(whole, meta(x, y - 1, z) & 7, x, y - 1, z);   // a pad under it, flush with the raised walk
                }
            }
        }
        put(b, m, x, y, z);
        float[] d = PropBoxes.box(b, m);
        if (d != null)
        {
            boxes.add(PropGeom.box(b, m, x, y, z));
            boxOwners.add(new Object[] {b, new int[] {x, y, z}});
        }
        t[0]++;
        return true;
    }

    private static Block wholeSlab(Block slab)
    {
        if (slab == Blocks.stone_slab)
        {
            return Blocks.double_stone_slab;
        }
        if (slab == Blocks.wooden_slab)
        {
            return Blocks.double_wooden_slab;
        }
        return null;
    }

    /** A prop beside (x, y, z) at the same height whose front looks straight at it. */
    private boolean faced(int x, int y, int z)
    {
        for (int k = 0; k < 4; k++)
        {
            int px = x - SIDES[k][0], pz = z - SIDES[k][1];
            Block p = block(px, y, pz);
            if (p != null && PropBoxes.box(p, meta(px, y, pz)) != null && PropFacing.front(p, meta(px, y, pz)) == k)
            {
                return true;
            }
        }
        return false;
    }

    private int fits(Block b, int m, int x, int y, int z, int mount)
    {
        Block here = inside(x, y, z) ? block(x, y, z) : null;
        if (!inside(x, y, z) || here != null && !BuildingLayout.cover(here))
        {
            return 1;                                      // taken (a carpet, a plate, a plant is replaced)
        }
        float[] d = PropBoxes.box(b, m);
        if (d == null)
        {
            // a plain block or a flat picture: its own cell, which no prop may reach into
            float[] cell = {x, x + 1, y, y + 1, z, z + 1};
            for (float[] q : boxes)
            {
                if (PropGeom.overlap(cell, q[0], q[1], q[2], q[3], q[4], q[5], PropGeom.SMALL))
                {
                    return 6;
                }
            }
            for (int cy = y - 3; cy <= y; cy++)
            {
                for (int cz = z - 2; cz <= z + 2; cz++)
                {
                    for (int cx = x - 2; cx <= x + 2; cx++)
                    {
                        Block k = block(cx, cy, cz);
                        float[] q = k == null || PropBoxes.box(k, meta(cx, cy, cz)) == null ? null : PropGeom.shape(k, meta(cx, cy, cz));
                        if (q != null && (cx != x || cy != y || cz != z)
                            && PropGeom.overlap(cell, cx + q[0], cx + q[1], cy + q[2], cy + q[3], cz + q[4], cz + q[5], PropGeom.SMALL))
                        {
                            return 6;
                        }
                    }
                }
            }
            // a solid block (sandbags) right in front of a prop's face: propclash FACE for the prop beside it
            // (v0.54 first run: a fighting position bagged a traffic light's lenses)
            float[] own = PropGeom.shape(b, m);
            if (own != null && own[3] - own[2] > 0.6f && b.getMaterial() != net.minecraft.block.material.Material.glass
                && faced(x, y, z))
            {
                return 7;
            }
            return 0;
        }
        float[] p = PropGeom.box(b, m, x, y, z);
        if (p[0] < -PropGeom.SMALL || p[4] < -PropGeom.SMALL || p[1] > w + PropGeom.SMALL || p[5] > l + PropGeom.SMALL)
        {
            return 2;                                      // drawn into the next chunk's plan
        }
        if (mount != 1)
        {
            Block below = block(x, y - 1, z);
            boolean halfSlab = below instanceof BlockSlab && (meta(x, y - 1, z) & 8) == 0 && wholeSlab(below) != null;
            if (!supports(x, y - 1, z) && !halfSlab)
            {
                return 3;
            }
            if (inDoorway(x, y, z))
            {
                return 4;
            }
        }
        // a front (screen, seat, door, sign face) never looks straight into a wall
        int front = PropFacing.front(b, m);
        int bx = Integer.MIN_VALUE, bz = 0;
        if (front >= 0)
        {
            int fx = SIDES[front][0], fz = SIDES[front][1];
            Block above = block(x + fx, y + 1, z + fz);
            if (blocksFront(x + fx, y, z + fz)
                && (mount == 1 || above != null && PropGeom.shape(above, meta(x + fx, y + 1, z + fz)) != null))
            {
                return 7;
            }
            bx = x - fx;
            bz = z - fz;
        }
        for (int cy = Math.max(0, (int) Math.floor(p[2])); cy <= Math.min(h - 1, (int) Math.floor(p[3])); cy++)
        {
            for (int cz = Math.max(0, (int) Math.floor(p[4]) - 1); cz <= Math.min(l - 1, (int) Math.floor(p[5]) + 1); cz++)
            {
                for (int cx = Math.max(0, (int) Math.floor(p[0]) - 1); cx <= Math.min(w - 1, (int) Math.floor(p[1]) + 1); cx++)
                {
                    if (cx == x && cy == y && cz == z || cy < y)
                    {
                        continue;                          // its own cell, the ground it stands in
                    }
                    Block k = block(cx, cy, cz);
                    if (k == null)
                    {
                        continue;
                    }
                    if (mount == 1 && PropBoxes.box(k, meta(cx, cy, cz)) == null
                        && (cx == x && cz == z && cy == y + 1                      // the ceiling over it
                            || cy == y && (bx == Integer.MIN_VALUE || cx == bx && cz == bz)))   // the wall behind it
                    {
                        continue;
                    }
                    float[] q = PropGeom.shape(k, meta(cx, cy, cz));
                    if (q != null && PropBoxes.box(k, meta(cx, cy, cz)) != null)
                    {
                        float[] s = {cx + q[0], cx + q[1], cy + q[2], cy + q[3], cz + q[4], cz + q[5]};
                        if (PropGeom.propsOverlap(p, b, x, y, z, s, k, cx, cy, cz))
                        {
                            return 6;
                        }
                    }
                    else if (q != null && PropGeom.overlap(p, cx + q[0], cx + q[1], cy + q[2], cy + q[3], cz + q[4], cz + q[5],
                                                      PropGeom.isDoor(k) ? 0 : PropGeom.SMALL))
                    {
                        return 5;
                    }
                }
            }
        }
        for (int i = 0; i < boxes.size(); i++)
        {
            float[] q = boxes.get(i);
            int[] a = (int[]) boxOwners.get(i)[1];
            if (PropGeom.propsOverlap(p, b, x, y, z, q, (Block) boxOwners.get(i)[0], a[0], a[1], a[2]))
            {
                return 6;
            }
        }
        // the plan's own props further away whose drawn box is long (a car reaches 3.4 from its block)
        for (int cy = Math.max(0, y - 1); cy <= Math.min(h - 1, y + 2); cy++)
        {
            for (int cz = Math.max(0, z - 5); cz <= Math.min(l - 1, z + 5); cz++)
            {
                for (int cx = Math.max(0, x - 5); cx <= Math.min(w - 1, x + 5); cx++)
                {
                    Block k = base.block(cx, cy, cz);
                    float[] q = k == null ? null : PropBoxes.box(k, base.meta(cx, cy, cz));
                    if (q != null && (cx != x || cy != y || cz != z))
                    {
                        float[] s = PropGeom.shrink(q, cx, cy, cz);
                        if (PropGeom.propsOverlap(p, b, x, y, z, s, k, cx, cy, cz))
                        {
                            return 6;
                        }
                    }
                }
            }
        }
        return 0;
    }
}
