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
    void forgesOwnWearOfAPiece()
    {
        // ArmorProperties.ApplyArmor: absorbs damage x points / 25, at most durability left + 1, wears that / 25, at least 1
        assertEquals(4, Durability.forgeWear(20f, 5, 256, 100));           // 20 hp on a chest piece: 4
        assertEquals(1, Durability.forgeWear(6f, 4, 256, 100));            // a small hit: 1
        assertEquals(1, Durability.forgeWear(20f, 5, 256, 253));           // 3 left: absorbs at most 4 / 25 hp: 1
        assertEquals(1, Durability.forgeWear(20f, 5, 256, 256));           // used up: 1, which would destroy it
    }

    @Test
    void vanillaWearStopsAtTheMaximumWithoutRepairing()
    {
        assertEquals(100, Durability.beforeVanillaWear(100, 256, 20f, 5)); // room left: untouched
        assertEquals(255, Durability.beforeVanillaWear(256, 256, 20f, 5)); // used up: one back, Forge's 1 brings it to 256
        assertEquals(254, Durability.beforeVanillaWear(254, 256, 30f, 5)); // 2 left: Forge wears 1 there, untouched
        int d = Durability.beforeVanillaWear(256, 256, 40f, 5);
        assertEquals(256, d + Durability.forgeWear(40f, 5, 256, d));        // a big hit on a used up piece: no free repair
    }

    @Test
    void aVestWearsOnAMeleeHitAsForgeWearsAPiece()
    {
        // user, 23 Oktober 2026: "vest armor still doesnt affected when hit by zombie": Forge wears only the four armor slots
        assertEquals(104, Durability.meleeWear(100, 512, 20f, 5));          // Forge's wear of a piece: 4
        assertEquals(1, Durability.meleeWear(0, 512, 1f, 0));               // at least 1
        assertEquals(512, Durability.meleeWear(511, 512, 20f, 5));          // the last point
        assertEquals(512, Durability.meleeWear(512, 512, 40f, 5));          // used up: kept on the maximum, never past it
    }

    @Test
    void theBarShowsOnlyOnceWornAndEmptiesAtTheMaximum()
    {
        // user, 23 Oktober 2026: "the icon gun item has progress bar in there showing durability like native progress bar"
        assertFalse(Durability.barShown(0, 1500));                          // new: no bar, as a new vanilla tool
        assertTrue(Durability.barShown(1, 1500));
        assertFalse(Durability.barShown(5, 0));                             // nothing that wears
        assertEquals(0.5, Durability.barFraction(750, 1500), 1e-9);         // the share used up, as Forge's getDurabilityForDisplay
        assertEquals(1.0, Durability.barFraction(1600, 1500), 1e-9);
        assertEquals(0.0, Durability.barFraction(-3, 1500), 1e-9);
        assertEquals(0.0, Durability.barFraction(5, 0), 1e-9);
    }
}
