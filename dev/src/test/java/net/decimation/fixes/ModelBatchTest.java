package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Model parts in one draw: quads moved into the part's own space by GL's column major matrices. */
class ModelBatchTest
{
    /** Column major GL matrix: rotation about y by deg, uniform scale s, then translation. */
    static float[] matrix(double deg, float s, float tx, float ty, float tz)
    {
        float c = (float) Math.cos(Math.toRadians(deg)) * s, n = (float) Math.sin(Math.toRadians(deg)) * s;
        return new float[] {c, 0, -n, 0, 0, s, 0, 0, n, 0, c, 0, tx, ty, tz, 1};
    }

    @Test
    void inverseUndoesTheMatrix()
    {
        float[] m = matrix(37, 2.5f, 3, -4, 7), inv = new float[16], id = new float[16];
        assertTrue(ModelBatch.invert(m, inv));
        ModelBatch.mul(inv, m, id);
        for (int i = 0; i < 16; i++)
        {
            assertEquals(i % 5 == 0 ? 1 : 0, id[i], 1e-5, "element " + i);
        }
        assertFalse(ModelBatch.invert(new float[16], inv)); // all zero: no inverse
    }

    @Test
    void pointsAndNormalsInTheBaseSpace()
    {
        float[] base = matrix(90, 1, 10, 0, 0), part = matrix(90, 1, 0, 0, 5), cur = new float[16], inv = new float[16];
        ModelBatch.mul(base, part, cur);            // what GL holds while the part draws: base, then the part
        ModelBatch.invert(base, inv);
        float[] loc = new float[16], p = new float[3];
        ModelBatch.mul(inv, cur, loc);              // back to the part's transform alone
        ModelBatch.point(loc, 1, 2, 3, p);          // part: rotate 90 about y (x -> -z, z -> x), then z + 5
        assertEquals(3, p[0], 1e-5);
        assertEquals(2, p[1], 1e-5);
        assertEquals(4, p[2], 1e-5);
        ModelBatch.normal(matrix(90, 3, 1, 1, 1), 1, 0, 0, p); // scaled and moved: only the direction, unit length
        assertEquals(0, p[0], 1e-5);
        assertEquals(0, p[1], 1e-5);
        assertEquals(-1, p[2], 1e-5);
    }
}
