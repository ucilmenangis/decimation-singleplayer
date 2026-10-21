package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** tools/guns/gripfit.py writes grip_offsets.txt lines "gun grip dx dy dz"; GripPlacement reads them. */
class GripPlacementTest
{
    @Test
    void parsesLines()
    {
        Map<String, float[]> m = GripPlacement.parse(Arrays.asList("# gun grip dx dy dz", "", "m4a4 rk1 0.1 -0.2 0.0",
                                                                  "glock17 laser -3.5 1.25 -0.15", "broken line", "ak74 foregrip x 1 2"));
        assertEquals(2, m.size());
        assertArrayEquals(new float[] {0.1f, -0.2f, 0f}, m.get("m4a4 rk1"), 1e-6f);
        assertArrayEquals(new float[] {-3.5f, 1.25f, -0.15f}, m.get("glock17 laser"), 1e-6f);
    }

    @Test
    void hidesOnlyTheForegripsStrayPlate()
    {
        // Decimation's Foregrip hangs its 8 parts (grip_1 .. grip_8) from a parent "grip" whose own plate sits 7.5
        // behind them; the magazine in that file never loads (children of an ammoModel0 it never declares)
        assertEquals(true, GripPlacement.stray("foregrip", "grip"));
        assertEquals(false, GripPlacement.stray("foregrip", "grip_4"));
        assertEquals(false, GripPlacement.stray("rk1", "gripModel0"));
        assertEquals(false, GripPlacement.stray("laser", "grip"));
    }
}
