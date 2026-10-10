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
