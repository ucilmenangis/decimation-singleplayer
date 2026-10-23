package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Decimation's food, water, blood and health percents centred on their icons (user, 23 Oktober 2026: "centre decimation
 * hud percents too"). IngameHud starts each 17 half pixels into its 60 half pixel icon, so the icon's middle is the start
 * plus 13; the same middle as the radiation percent's (RadiationHud.textX).
 */
class HudPercentsTest
{
    @Test
    void aPercentIsCentredOnItsIcon()
    {
        assertEquals(216, HudPercents.centred(217, 28));                 // "100%" bold: one half pixel left of Decimation's 217
        assertEquals(223, HudPercents.centred(217, 14));                 // "5%"
        assertEquals(RadiationHud.textX(100, 21), HudPercents.centred(2 * 100 + 17, 21));
    }

    @Test
    void onlyTheFourPercentRows()
    {
        assertEquals(0, HudPercents.row(60));                            // food
        assertEquals(1, HudPercents.row(129));                           // water
        assertEquals(2, HudPercents.row(199));                           // blood
        assertEquals(3, HudPercents.row(266));                           // health
        assertEquals(-1, HudPercents.row(61));                           // anything else IngameHud outlines: names, ids
    }
}
