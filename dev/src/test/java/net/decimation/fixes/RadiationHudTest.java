package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The radiation percent sits centred on its icon (user, 23 Oktober 2026: "the text is not centered on radiaton hud").
 * Decimation draws its own percents from a fixed start, which centres only a text as wide as "100%"; at half scale the 30
 * pixel icon spans 60, its middle at 2x + 30.
 */
class RadiationHudTest
{
    @Test
    void thePercentIsCentredOnTheIcon()
    {
        assertEquals(216, RadiationHud.textX(100, 28));                    // "100%" in bold: Decimation's own start is 217
        assertEquals(223, RadiationHud.textX(100, 14));                    // "0%"
        assertEquals(30, RadiationHud.textX(0, 0));
    }
}
