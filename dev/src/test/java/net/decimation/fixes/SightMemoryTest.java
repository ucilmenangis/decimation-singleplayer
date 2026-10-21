package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * ScopeZoom's learned sight positions kept between launches (user, 21 Oktober 2026: "the scope aiming still need more
 * optimizing cause sometime it has delay on it"): the first aim of each gun and sight in every launch held the zoom at
 * 1.5x until the sight was learned again (dev test aimdelay: ACOG 431 ms to 95 % zoom against 187 ms once learned).
 */
class SightMemoryTest
{
    @Test
    void keepsLearnedSightsOfThisBuild()
    {
        Map<String, double[]> m = new LinkedHashMap<String, double[]>();
        m.put("deci:m4a4|ta11acog|177", new double[] {0.0049, -0.2189, 37});
        m.put("deci:akm|reddot|177", new double[] {0.01, -0.3, 4});          // not learned yet: not kept
        List<String> lines = SightMemory.format(m, "0.65.1", 10);
        Map<String, double[]> back = SightMemory.parse(lines, "0.65.1", 10);
        assertEquals(1, back.size());
        assertArrayEquals(new double[] {0.0049, -0.2189, 10}, back.get("deci:m4a4|ta11acog|177"), 1e-9);
    }

    @Test
    void forgetsSightsOfAnotherBuild()
    {
        Map<String, double[]> m = new LinkedHashMap<String, double[]>();
        m.put("deci:m4a4|ta11acog|177", new double[] {0.0049, -0.2189, 12});
        assertTrue(SightMemory.parse(SightMemory.format(m, "0.65.0", 10), "0.65.1", 10).isEmpty());
    }

    @Test
    void skipsBrokenLines()
    {
        Map<String, double[]> back = SightMemory.parse(Arrays.asList("build 0.65.1", "deci:m4a4|ta11acog|177 x 1",
                                                                     "deci:m4a4|2x|177 0.1 -0.2", "", "only"), "0.65.1", 10);
        assertEquals(1, back.size());
        assertArrayEquals(new double[] {0.1, -0.2, 10}, back.get("deci:m4a4|2x|177"), 1e-9);
    }
}
