package net.decimation.worldgen.military;

import net.minecraft.block.Block;

/**
 * A military base built in memory before it is written slice by slice: per cell a block, its
 * metadata and how that metadata turns when the base is rotated. Coordinates are the base's own
 * (x east, y up from the ground layer 0, z south), the gate on the south side.
 */
final class Canvas
{
    /** How a cell's metadata turns: not at all, as a vanilla block (Rotation), as a door, as a Decimation prop. */
    static final byte PLAIN = 0, VANILLA = 1, DOOR = 2, PROP = 3;

    final int w, h, l;
    private final Block[] block;
    private final byte[] meta, kind;

    Canvas(int w, int h, int l)
    {
        this.w = w;
        this.h = h;
        this.l = l;
        block = new Block[w * h * l];
        meta = new byte[w * h * l];
        kind = new byte[w * h * l];
    }

    boolean inside(int x, int y, int z)
    {
        return x >= 0 && y >= 0 && z >= 0 && x < w && y < h && z < l;
    }

    private int i(int x, int y, int z)
    {
        return (y * l + z) * w + x;
    }

    void set(int x, int y, int z, Block b, int m, byte k)
    {
        if (b == null || !inside(x, y, z))
        {
            return;
        }
        int n = i(x, y, z);
        block[n] = b;
        meta[n] = (byte) m;
        kind[n] = k;
    }

    void set(int x, int y, int z, Block b)
    {
        set(x, y, z, b, 0, PLAIN);
    }

    void set(int x, int y, int z, Block b, int m)
    {
        set(x, y, z, b, m, m == 0 ? PLAIN : VANILLA);
    }

    /** A Decimation prop facing east 2, south 3, west 4, north 5 (its front). */
    void prop(int x, int y, int z, Block b, int facing)
    {
        set(x, y, z, b, facing, PROP);
    }

    /** Air on purpose (cleared even if the canvas had something). */
    void clear(int x, int y, int z)
    {
        if (inside(x, y, z))
        {
            int n = i(x, y, z);
            block[n] = null;
            meta[n] = 0;
            kind[n] = PLAIN;
        }
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b, int m)
    {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
        {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
            {
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                {
                    set(x, y, z, b, m);
                }
            }
        }
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b)
    {
        fill(x0, y0, z0, x1, y1, z1, b, 0);
    }

    void clearBox(int x0, int y0, int z0, int x1, int y1, int z1)
    {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
        {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
            {
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                {
                    clear(x, y, z);
                }
            }
        }
    }

    Block get(int x, int y, int z)
    {
        return inside(x, y, z) ? block[i(x, y, z)] : null;
    }

    int meta(int x, int y, int z)
    {
        return inside(x, y, z) ? meta[i(x, y, z)] & 0xFF : 0;
    }

    byte kind(int x, int y, int z)
    {
        return inside(x, y, z) ? kind[i(x, y, z)] : PLAIN;
    }

    /**
     * Removes every prop whose DRAWN box (PropBoxes, from Decimation's own renderers) goes into a
     * full block, a door, a fence post, a slab, or another prop's drawn box (the smaller of two
     * clashing props goes), and logs each one ("clash ..."). Cells below a prop (the ground it
     * stands in) and touching within 0.08 do not count. Run once the canvas is complete; the dev
     * test lists the log, the design is then fixed until it is empty (docs/prop_placement.md).
     */
    void validateProps(java.util.List<String> log)
    {
        final float tol = 0.08f;
        java.util.List<int[]> props = new java.util.ArrayList<int[]>();
        java.util.List<float[]> boxes = new java.util.ArrayList<float[]>();
        for (int y = 0; y < h; y++)
        {
            for (int z = 0; z < l; z++)
            {
                for (int x = 0; x < w; x++)
                {
                    int n = i(x, y, z);
                    if (kind[n] != PROP || block[n] == null)
                    {
                        continue;
                    }
                    float[] b = PropBoxes.box(block[n], meta[n]);
                    if (b == null)
                    {
                        continue;
                    }
                    props.add(new int[] {x, y, z});
                    boxes.add(new float[] {x + b[0] + tol, x + b[1] - tol, y + b[2] + tol, y + b[3] - tol,
                                           z + b[4] + tol, z + b[5] - tol});
                }
            }
        }
        boolean[] gone = new boolean[props.size()];
        for (int k = 0; k < props.size(); k++)
        {
            int[] p = props.get(k);
            float[] b = boxes.get(k);
            String hit = null;
            for (int cy = Math.max(p[1], (int) Math.floor(b[2])); cy <= (int) Math.floor(b[3]) && hit == null; cy++)
            {
                for (int cz = (int) Math.floor(b[4]); cz <= (int) Math.floor(b[5]) && hit == null; cz++)
                {
                    for (int cx = (int) Math.floor(b[0]); cx <= (int) Math.floor(b[1]) && hit == null; cx++)
                    {
                        if ((cx == p[0] && cy == p[1] && cz == p[2]) || !inside(cx, cy, cz))
                        {
                            continue;
                        }
                        if (mounted(block[i(p[0], p[1], p[2])]) && cy == p[1] && isBack(meta[i(p[0], p[1], p[2])], cx - p[0], cz - p[2]))
                        {
                            continue;                    // a keycard / wall flag hangs on the wall behind it
                        }
                        int n = i(cx, cy, cz);
                        if (block[n] != null && kind[n] != PROP && blocks(block[n], meta[n], b, cx, cy, cz))
                        {
                            hit = Block.blockRegistry.getNameForObject(block[n]) + " at " + cx + "," + cy + "," + cz;
                        }
                    }
                }
            }
            if (hit != null)
            {
                gone[k] = true;
                log.add("clash " + name(p) + " into " + hit);
            }
        }
        for (int a = 0; a < props.size(); a++)
        {
            for (int c = a + 1; c < props.size() && !gone[a]; c++)
            {
                if (gone[c] || Math.abs(props.get(a)[0] - props.get(c)[0]) > 12
                    || Math.abs(props.get(a)[2] - props.get(c)[2]) > 12)
                {
                    continue;
                }
                float[] p = boxes.get(a), q = boxes.get(c);
                if (p[0] < q[1] && q[0] < p[1] && p[2] < q[3] && q[2] < p[3] && p[4] < q[5] && q[4] < p[5])
                {
                    int smaller = volume(p) < volume(q) ? a : c;
                    gone[smaller] = true;
                    log.add("clash " + name(props.get(smaller)) + " overlaps " + name(props.get(smaller == a ? c : a)));
                }
            }
        }
        for (int k = 0; k < props.size(); k++)
        {
            if (gone[k])
            {
                int[] p = props.get(k);
                clear(p[0], p[1], p[2]);
            }
        }
    }

    private String name(int[] p)
    {
        int n = i(p[0], p[1], p[2]);
        String s = Block.blockRegistry.getNameForObject(block[n]);
        return (s == null ? "?" : s.replace("deci:", "")) + " meta " + meta[n] + " at " + p[0] + "," + p[1] + "," + p[2];
    }

    /** Props hung on the wall behind their front (2 E, 3 S, 4 W, 5 N): their box enters that wall. */
    private static boolean mounted(Block b)
    {
        String n = String.valueOf(Block.blockRegistry.getNameForObject(b));
        return n.contains("KeycardScreen") || n.contains("Wallflag") || n.contains("ElectricBox1");
    }

    private static boolean isBack(int meta, int dx, int dz)
    {
        switch (meta)
        {
            case 2: return dx == -1 && dz == 0;
            case 3: return dx == 0 && dz == -1;
            case 4: return dx == 1 && dz == 0;
            case 5: return dx == 0 && dz == 1;
            default: return false;
        }
    }

    private static float volume(float[] b)
    {
        return (b[1] - b[0]) * (b[3] - b[2]) * (b[5] - b[4]);
    }

    /** Does a non prop block in cell (cx, cy, cz) stand in the way of the (shrunk) box b? */
    private static boolean blocks(Block k, int m, float[] b, int cx, int cy, int cz)
    {
        float[] s;
        String name = String.valueOf(Block.blockRegistry.getNameForObject(k));
        if (name.contains("door") || name.contains("Door"))
        {
            return true;                                 // never draw into a doorway
        }
        if (k instanceof net.minecraft.block.BlockFence)
        {
            s = new float[] {0.375f, 0.625f, 0, 1, 0.375f, 0.625f};
        }
        else if (k instanceof net.minecraft.block.BlockSlab)
        {
            s = (m & 8) != 0 ? new float[] {0, 1, 0.5f, 1, 0, 1} : new float[] {0, 1, 0, 0.5f, 0, 1};
        }
        else if (k.isOpaqueCube() || k instanceof net.minecraft.block.BlockGlass)
        {
            s = new float[] {0, 1, 0, 1, 0, 1};
        }
        else
        {
            return false;                                // wire, carpet, ladder, panes, plants
        }
        return b[0] < cx + s[1] && cx + s[0] < b[1] && b[2] < cy + s[3] && cy + s[2] < b[3]
            && b[4] < cz + s[5] && cz + s[4] < b[5];
    }

    /** True when every cell of the box at ground level + 1 .. y1 is empty (for placing modules). */
    boolean free(int x0, int z0, int x1, int z1, int y1)
    {
        for (int z = z0; z <= z1; z++)
        {
            for (int x = x0; x <= x1; x++)
            {
                for (int y = 1; y <= y1; y++)
                {
                    if (!inside(x, y, z) || get(x, y, z) != null)
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
