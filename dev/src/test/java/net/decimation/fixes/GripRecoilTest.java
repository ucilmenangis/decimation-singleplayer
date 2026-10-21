package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** docs/superpowers/specs/2026-10-21-new-attachments-design.md section 2: each grip's {up, sideways} recoil factors. */
class GripRecoilTest
{
    @Test
    void factorsMatchTheSpec()
    {
        assertArrayEquals(new float[] {1 / 3f, 1 / 3f}, GripRecoil.factors("foregrip"), 1e-6f); // Decimation's, unchanged
        assertArrayEquals(new float[] {0.5f, 0.25f}, GripRecoil.factors("rk1"), 1e-6f);
        assertArrayEquals(new float[] {1, 1}, GripRecoil.factors("laser"), 1e-6f);
        assertArrayEquals(new float[] {1, 1}, GripRecoil.factors("flashlight"), 1e-6f);
        assertArrayEquals(new float[] {1, 1}, GripRecoil.factors(null), 1e-6f);
    }

    @Test
    void calmsViewForBothGrips()
    {
        assertTrue(GripRecoil.calmsViewName("foregrip"));
        assertTrue(GripRecoil.calmsViewName("rk1"));
        assertFalse(GripRecoil.calmsViewName("laser"));
        assertFalse(GripRecoil.calmsViewName(null));
    }
}
