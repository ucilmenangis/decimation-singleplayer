package net.decimation.fixes.gunsmith;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** The Gunsmith screen's layout at every GUI size from 854 x 480 (427 x 240 units) up, and the smallest one (320 x 240). */
class GunsmithLayoutTest
{
    static final int[][] SIZES = {{427, 240}, {480, 270}, {640, 360}, {960, 540}, {320, 240}};
    static final int B = GunsmithLayout.BOX;

    /** The rifle of SilhouetteTest drawn at 90 % of the gun area's width, centred, its points in screen units. */
    static int[][] rifleAt(int[] area)
    {
        Silhouette s = Silhouette.measure(SilhouetteTest.RIFLE, 200, 80);
        double k = area[2] * 0.9 / 200;
        double x0 = area[0] + area[2] * 0.05, y0 = area[1] + area[3] / 2.0 - 80 * k / 2;
        int[][] p = new int[s.points.length][];
        for (int i = 0; i < p.length; i++)
        {
            p[i] = new int[] {(int) (x0 + s.points[i][0] * k), (int) (y0 + s.points[i][1] * k)};
        }
        return p;
    }

    /** Two boxes with the label band under each (10 units) and a 4 unit gap. */
    static boolean boxesOverlap(int[] a, int[] b)
    {
        return rectsOverlap(new int[] {a[0] - 2, a[1] - 2, B + 4, B + 14}, new int[] {b[0] - 2, b[1] - 2, B + 4, B + 14});
    }

    static boolean rectsOverlap(int[] a, int[] b)
    {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    static int[][] corners(int[] s)
    {
        int right = s[0] - 4 - B, bottom = s[1] - GunsmithLayout.BOTTOM - B - 10;
        return new int[][] {{4, GunsmithLayout.TOP}, {right, GunsmithLayout.TOP}, {4, bottom}, {right, bottom},
                            {s[0] / 2 - B / 2, s[1] / 2 - B / 2}};
    }

    @Test
    void gunAreaLeavesRoomForBoxesAndHotbar()
    {
        for (int[] s : SIZES)
        {
            int[] a = GunsmithLayout.gunArea(s[0], s[1]);
            assertTrue(a[0] >= 40 && a[0] + a[2] <= s[0] - 40, Arrays.toString(s));
            assertTrue(a[1] > GunsmithLayout.TOP && a[1] + a[3] <= s[1] - GunsmithLayout.BOTTOM, Arrays.toString(s));
            assertTrue(a[2] > 0 && a[3] > 0, Arrays.toString(s));
        }
    }

    @Test
    void boxesStayOnScreenAndApart()
    {
        for (int[] s : SIZES)
        {
            int[][] b = GunsmithLayout.boxes(s[0], s[1], rifleAt(GunsmithLayout.gunArea(s[0], s[1])));
            for (int[] x : b)
            {
                assertTrue(x[0] >= 4 && x[0] + B <= s[0] - 4 && x[1] >= GunsmithLayout.TOP
                           && x[1] + B + 10 <= s[1] - GunsmithLayout.BOTTOM, Arrays.toString(s) + " " + Arrays.toString(x));
            }
            for (int i = 0; i < 6; i++)
            {
                for (int j = i + 1; j < 6; j++)
                {
                    assertFalse(boxesOverlap(b[i], b[j]), s[0] + " " + GunSlot.values()[i] + "/" + GunSlot.values()[j]);
                }
            }
        }
    }

    @Test
    void boxesStayNearTheirParts()
    {
        for (int[] s : SIZES)
        {
            int[][] pts = rifleAt(GunsmithLayout.gunArea(s[0], s[1]));
            int[][] b = GunsmithLayout.boxes(s[0], s[1], pts);
            for (int i = 0; i < 6; i++)
            {
                double d = Math.hypot(b[i][0] + B / 2 - pts[i][0], b[i][1] + B / 2 - pts[i][1]);
                assertTrue(d <= 120, s[0] + " " + GunSlot.values()[i] + " " + d);
            }
        }
    }

    @Test
    void listOnScreenAndBesideItsBox()
    {
        for (int[] s : SIZES)
        {
            for (int[] box : corners(s))
            {
                int[] l = GunsmithLayout.list(s[0], s[1], box, 7);
                assertEquals(GunsmithLayout.LIST_W, l[2]);
                assertEquals(14 + GunsmithLayout.ROW * GunsmithLayout.MAX_ROWS, l[3]);
                assertTrue(l[0] >= 0 && l[1] >= 0 && l[0] + l[2] <= s[0] && l[1] + l[3] <= s[1],
                           Arrays.toString(s) + " " + Arrays.toString(l));
                assertFalse(rectsOverlap(l, new int[] {box[0], box[1], B, B}), Arrays.toString(s) + " " + Arrays.toString(box));
            }
        }
    }

    @Test
    void shortListIsShort()
    {
        assertEquals(14 + GunsmithLayout.ROW, GunsmithLayout.list(427, 240, new int[] {100, 100}, 1)[3]);
    }
}
