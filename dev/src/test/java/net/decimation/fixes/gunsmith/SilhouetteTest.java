package net.decimation.fixes.gunsmith;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Where the Gunsmith screen's lines end, found on the gun as drawn (a depth mask, muzzle to the right). */
class SilhouetteTest
{
    /** A rifle side on: stock, body, barrel, magazine, scope, fore end (200 x 80). */
    static final boolean[] RIFLE = rifle();
    /** A pistol: slide over a grip (100 x 70). */
    static final boolean[] PISTOL = pistol();

    static boolean[] rifle()
    {
        boolean[] m = new boolean[200 * 80];
        rect(m, 200, 0, 39, 30, 49);      // stock
        rect(m, 200, 40, 119, 25, 44);    // body
        rect(m, 200, 120, 199, 32, 35);   // barrel
        rect(m, 200, 80, 95, 45, 69);     // magazine
        rect(m, 200, 60, 99, 12, 24);     // scope
        rect(m, 200, 120, 159, 30, 39);   // fore end
        return m;
    }

    static boolean[] pistol()
    {
        boolean[] m = new boolean[100 * 70];
        rect(m, 100, 0, 99, 10, 29);      // slide
        rect(m, 100, 10, 29, 30, 69);     // grip
        return m;
    }

    static void rect(boolean[] m, int w, int x0, int x1, int y0, int y1)
    {
        for (int y = y0; y <= y1; y++)
        {
            for (int x = x0; x <= x1; x++)
            {
                m[y * w + x] = true;
            }
        }
    }

    static boolean between(int v, int lo, int hi)
    {
        return v >= lo && v <= hi;
    }

    @Test
    void emptyMaskIsNull()
    {
        assertNull(Silhouette.measure(new boolean[100], 10, 10));
    }

    @Test
    void boundsAndMuzzle()
    {
        Silhouette s = Silhouette.measure(RIFLE, 200, 80);
        assertEquals(0, s.minX);
        assertEquals(199, s.maxX);
        assertEquals(12, s.minY);
        assertEquals(69, s.maxY);
        assertEquals(199, s.points[GunSlot.BARREL.ordinal()][0]);
        assertTrue(between(s.points[GunSlot.BARREL.ordinal()][1], 32, 35));
    }

    @Test
    void sightOnTheScopeMagazineOnTheMag()
    {
        Silhouette s = Silhouette.measure(RIFLE, 200, 80);
        assertEquals(12, s.points[GunSlot.SIGHT.ordinal()][1]);
        int[] t = s.points[GunSlot.TRACER.ordinal()];
        assertTrue(between(t[0], 80, 95) && between(t[1], 60, 69), Arrays.toString(t));
        int[] g = s.points[GunSlot.GRIP.ordinal()];
        assertTrue(between(g[0], 120, 159) && between(g[1], 36, 39), Arrays.toString(g));   // under the fore end
        int[] st = s.points[GunSlot.STOCK.ordinal()];
        assertTrue(between(st[0], 0, 39), Arrays.toString(st));
    }

    @Test
    void everyPointOnTheGun()
    {
        Silhouette s = Silhouette.measure(RIFLE, 200, 80);
        for (int[] p : s.points)
        {
            assertTrue(RIFLE[p[1] * 200 + p[0]], Arrays.toString(p));
        }
    }

    @Test
    void pistolShapeStillOnTheGun()
    {
        Silhouette s = Silhouette.measure(PISTOL, 100, 70);
        for (int[] p : s.points)
        {
            assertTrue(PISTOL[p[1] * 100 + p[0]], Arrays.toString(p));
        }
    }
}
