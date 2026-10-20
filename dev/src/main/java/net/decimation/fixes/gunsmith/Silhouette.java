package net.decimation.fixes.gunsmith;

/**
 * Where the Gunsmith screen's lines end, read from the gun as drawn: a mask of the pixels the gun covered (the depth
 * buffer after drawing it alone, muzzle to the right, y down). Works for every gun without a hand placed point: the
 * barrel line ends at the muzzle, the sight on top of the middle, the grip under the fore end, the tracer on the
 * magazine, the stock at the back, the paint on the body. Measured once per change, never per frame. Plain Java.
 */
public final class Silhouette
{
    public final int minX, minY, maxX, maxY;
    /** One {x, y} per GunSlot, by ordinal, each on a pixel of the gun. */
    public final int[][] points;

    private Silhouette(int minX, int minY, int maxX, int maxY, int[][] points)
    {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.points = points;
    }

    /** The silhouette of a w x h mask (row major), or null when nothing was drawn. */
    public static Silhouette measure(boolean[] mask, int w, int h)
    {
        int minX = w, minY = h, maxX = -1, maxY = -1;
        long sumX = 0, sumY = 0, n = 0;
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (mask[y * w + x])
                {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    sumX += x;
                    sumY += y;
                    n++;
                }
            }
        }
        if (n == 0)
        {
            return null;
        }
        int width = maxX - minX + 1;
        int[][] p = new int[GunSlot.values().length][];
        p[GunSlot.BARREL.ordinal()] = new int[] {maxX, meanRow(mask, w, h, maxX - Math.max(1, width / 50), maxX)};
        p[GunSlot.STOCK.ordinal()] = new int[] {minX + width / 20, meanRow(mask, w, h, minX, minX + width / 10)};
        p[GunSlot.SIGHT.ordinal()] = topmost(mask, w, h, minX + width * 30 / 100, minX + width * 60 / 100);
        int[] grip = lowest(mask, w, h, minX + width * 60 / 100, minX + width * 80 / 100);
        p[GunSlot.GRIP.ordinal()] = new int[] {minX + width * 70 / 100, grip[1]};
        int[] mag = lowest(mask, w, h, minX + width * 35 / 100, minX + width * 60 / 100);
        p[GunSlot.TRACER.ordinal()] = new int[] {mag[0], mag[1] - 2};
        p[GunSlot.PAINT.ordinal()] = new int[] {(int) (sumX / n), (int) (sumY / n)};
        for (int i = 0; i < p.length; i++)
        {
            p[i] = snap(mask, w, h, p[i], Math.max(1, width / 4));
        }
        return new Silhouette(minX, minY, maxX, maxY, p);
    }

    /** Mean row of the set pixels in columns x0 .. x1. */
    private static int meanRow(boolean[] mask, int w, int h, int x0, int x1)
    {
        long sum = 0, n = 0;
        for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++)
        {
            for (int y = 0; y < h; y++)
            {
                if (mask[y * w + x])
                {
                    sum += y;
                    n++;
                }
            }
        }
        return n == 0 ? h / 2 : (int) (sum / n);
    }

    /** The topmost set pixel in columns x0 .. x1 (the leftmost on a tie), or the band's middle when it is empty. */
    private static int[] topmost(boolean[] mask, int w, int h, int x0, int x1)
    {
        for (int y = 0; y < h; y++)
        {
            for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++)
            {
                if (mask[y * w + x])
                {
                    return new int[] {x, y};
                }
            }
        }
        return new int[] {(x0 + x1) / 2, h / 2};
    }

    /** The lowest set pixel in columns x0 .. x1 (the leftmost on a tie), or the band's middle when it is empty. */
    private static int[] lowest(boolean[] mask, int w, int h, int x0, int x1)
    {
        for (int y = h - 1; y >= 0; y--)
        {
            for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++)
            {
                if (mask[y * w + x])
                {
                    return new int[] {x, y};
                }
            }
        }
        return new int[] {(x0 + x1) / 2, h / 2};
    }

    /** The point itself when it is on the gun, else the nearest pixel of the gun within r. */
    private static int[] snap(boolean[] mask, int w, int h, int[] p, int r)
    {
        int px = Math.max(0, Math.min(w - 1, p[0])), py = Math.max(0, Math.min(h - 1, p[1]));
        if (mask[py * w + px])
        {
            return new int[] {px, py};
        }
        int[] best = {px, py};
        long bestD = Long.MAX_VALUE;
        for (int y = Math.max(0, py - r); y <= Math.min(h - 1, py + r); y++)
        {
            for (int x = Math.max(0, px - r); x <= Math.min(w - 1, px + r); x++)
            {
                long d = (long) (x - px) * (x - px) + (long) (y - py) * (y - py);
                if (mask[y * w + x] && d < bestD)
                {
                    bestD = d;
                    best = new int[] {x, y};
                }
            }
        }
        return best;
    }
}
