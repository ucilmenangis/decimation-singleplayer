package net.decimation.fixes.gunsmith;

/**
 * Where the Gunsmith screen puts things, in GUI units (spec section 3, mockup A "Tarkov lines"): the area the gun is
 * scaled into, each slot's box near its part, and the list panel beside an open box. Computed on open and on change,
 * never per frame. Fits every GUI size from 854 x 480 (427 x 240 units) up. Plain Java.
 */
public final class GunsmithLayout
{
    public static final int BOX = 24, TOP = 30, BOTTOM = 52, LIST_W = 120, ROW = 18, MAX_ROWS = 6;
    /** The label band under a box and the gap kept between boxes. */
    private static final int LABEL = 10, GAP = 4;
    /** Preferred box offset from its part, by GunSlot ordinal (Sight, Barrel, Grip, Stock, Paint, Tracer). */
    private static final int[][] OFFSET = {{-12, -48}, {18, -36}, {10, 22}, {-30, -44}, {-40, 26}, {-12, 34}};

    private GunsmithLayout()
    {
    }

    /** The rectangle the gun is fitted into: {x, y, w, h}. */
    public static int[] gunArea(int w, int h)
    {
        int x = Math.max(40, w / 8), y = TOP + 12;
        return new int[] {x, y, w - 2 * x, h - y - BOTTOM - 20};
    }

    /** Each slot's box (top left), near its part's point, on screen and apart from the others. */
    public static int[][] boxes(int w, int h, int[][] points)
    {
        int[][] b = new int[points.length][];
        for (int i = 0; i < b.length; i++)
        {
            b[i] = clamp(w, h, points[i][0] + OFFSET[i][0], points[i][1] + OFFSET[i][1]);
        }
        for (int pass = 0; pass < 10; pass++)
        {
            boolean moved = false;
            for (int i = 0; i < b.length; i++)
            {
                for (int j = 0; j < i; j++)
                {
                    if (overlap(b[i], b[j]))
                    {
                        push(w, h, b[i], b[j]);
                        moved = true;
                    }
                }
            }
            if (!moved)
            {
                break;
            }
        }
        return b;
    }

    /** The list panel {x, y, w, h} for `rows` rows beside the box at {x, y}: right of it when it fits, else left. */
    public static int[] list(int w, int h, int[] box, int rows)
    {
        int ph = 14 + ROW * Math.min(rows, MAX_ROWS);
        int x = box[0] + BOX + GAP;
        if (x + LIST_W > w)
        {
            x = Math.max(0, box[0] - GAP - LIST_W);
        }
        int y = Math.max(0, Math.min(box[1], h - ph));
        return new int[] {x, y, LIST_W, ph};
    }

    private static int[] clamp(int w, int h, int x, int y)
    {
        return new int[] {Math.max(4, Math.min(w - 4 - BOX, x)), Math.max(TOP, Math.min(h - BOTTOM - BOX - LABEL, y))};
    }

    private static boolean overlap(int[] a, int[] b)
    {
        return Math.abs(a[0] - b[0]) < BOX + GAP && Math.abs(a[1] - b[1]) < BOX + LABEL + GAP;
    }

    /** Moves box a off box b along x; along y when the screen edge stops it. */
    private static void push(int w, int h, int[] a, int[] b)
    {
        int x = a[0] >= b[0] ? b[0] + BOX + GAP : b[0] - BOX - GAP;
        int[] c = clamp(w, h, x, a[1]);
        if (overlap(c, b))
        {
            int y = a[1] >= b[1] ? b[1] + BOX + LABEL + GAP : b[1] - BOX - LABEL - GAP;
            c = clamp(w, h, a[0], y);
        }
        a[0] = c[0];
        a[1] = c[1];
    }
}
