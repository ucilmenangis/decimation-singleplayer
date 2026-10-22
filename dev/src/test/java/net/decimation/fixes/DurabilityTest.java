package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Durability (feature 10, user 22 Oktober 2026; docs/superpowers/specs/2026-10-22-durability-design.md): armor 256, vests
 * 512, guns by kind; a part repairs a quarter; kept at 0 but working no more; guns jam below 25 %; the stronger of vest
 * and chest counts.
 */
class DurabilityTest
{
    @Test
    void theMaxima()
    {
        assertEquals(256, Durability.ARMOR);
        assertEquals(512, Durability.VEST);
        assertEquals(1500, Durability.gunMax("rifle"));
        assertEquals(1800, Durability.gunMax("smg"));
        assertEquals(2500, Durability.gunMax("pistol"));
        assertEquals(2500, Durability.gunMax("revolver"));
        assertEquals(1200, Durability.gunMax("shotgun"));
        assertEquals(1000, Durability.gunMax("mg"));
        assertEquals(0, Durability.gunMax("rocket"));      // launchers and crossbows do not wear
        assertEquals(0, Durability.gunMax("crossbow"));
    }

    @Test
    void aPartRepairsAQuarterAndNoMore()
    {
        assertEquals(192, Durability.repaired(256, 256));   // broken: three quarters left to repair
        assertEquals(0, Durability.repaired(50, 256));      // never past new
        assertEquals(0, Durability.repaired(128, 512));
        assertEquals(1125, Durability.repaired(1500, 1500));
    }

    @Test
    void brokenAtTheMaximumNotBefore()
    {
        assertTrue(Durability.broken(256, 256));
        assertFalse(Durability.broken(255, 256));
        assertFalse(Durability.broken(0, 0));               // no durability: never broken
    }

    @Test
    void jamsOnlyBelowAQuarterLeft()
    {
        assertFalse(Durability.jamZone(1125, 1500));        // exactly 25 % left
        assertTrue(Durability.jamZone(1126, 1500));
        assertFalse(Durability.jamZone(0, 1500));
        assertFalse(Durability.jamZone(10, 0));             // no wear at all
    }

    @Test
    void theStrongerOfVestAndChestCounts()
    {
        assertEquals(0.455f, Durability.bodyFactor(0.5525f, 0.455f), 1e-6);   // military vest over a marine top
        assertEquals(0.52f, Durability.bodyFactor(0.52f, 0.6175f), 1e-6);     // press vest weaker than the top
        assertEquals(0.455f, Durability.bodyFactor(1f, 0.455f), 1e-6);       // no chest piece
        assertEquals(1f, Durability.bodyFactor(1f, 1f), 1e-6);
    }

    @Test
    void vanillaWearStopsAtTheMaximum()
    {
        assertEquals(100, Durability.beforeVanillaWear(100, 256, 1));       // room left: untouched
        assertEquals(255, Durability.beforeVanillaWear(256, 256, 1));       // broken: lands on the maximum again
        assertEquals(253, Durability.beforeVanillaWear(255, 256, 3));
        assertEquals(0, Durability.beforeVanillaWear(5, 4, 9));            // never below 0
    }
}
