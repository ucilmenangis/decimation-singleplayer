package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Gas mask filters (user, 22 Oktober 2026, idea 42): a filter wears only while its wearer is in radiation, about 10 minutes
 * of radiation a filter; an empty filter protects no more until a new Gas Mask Filter is fitted (right click it).
 */
class GasMaskFilterTest
{
    @Test
    void aFullFilterLastsTenMinutesOfRadiation()
    {
        assertEquals(12000, GasMaskFilter.LIFE);                   // 10 minutes at 20 ticks a second
        int life = GasMaskFilter.LIFE;
        for (int t = 0; t < 11999; t++)
        {
            life = GasMaskFilter.wear(life, true);
        }
        assertEquals(1, life);
        assertTrue(GasMaskFilter.protects(life));
        life = GasMaskFilter.wear(life, true);
        assertEquals(0, life);
        assertFalse(GasMaskFilter.protects(life));
        assertEquals(0, GasMaskFilter.wear(0, true));               // never below nothing
    }

    @Test
    void itWearsOnlyInRadiation()
    {
        assertEquals(GasMaskFilter.LIFE, GasMaskFilter.wear(GasMaskFilter.LIFE, false));
        assertEquals(500, GasMaskFilter.wear(500, false));
        assertEquals(0, GasMaskFilter.wear(0, false));
    }

    @Test
    void thePercentShownRoundsUpSoAnyFilterLeftShowsAtLeastOne()
    {
        assertEquals(0, GasMaskFilter.percent(0));
        assertEquals(1, GasMaskFilter.percent(1));
        assertEquals(1, GasMaskFilter.percent(GasMaskFilter.STEP));
        assertEquals(2, GasMaskFilter.percent(GasMaskFilter.STEP + 1));
        assertEquals(50, GasMaskFilter.percent(GasMaskFilter.LIFE / 2));
        assertEquals(100, GasMaskFilter.percent(GasMaskFilter.LIFE - 1));
        assertEquals(100, GasMaskFilter.percent(GasMaskFilter.LIFE));
    }

    @Test
    void aMaskWithoutTheTagHasAFullFilter()
    {
        assertEquals(GasMaskFilter.LIFE, GasMaskFilter.life((NBTTagCompound) null));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("uniqueID", "abc");                            // Decimation's own tags on a worn mask
        assertEquals(GasMaskFilter.LIFE, GasMaskFilter.life(tag));
        tag.setInteger(GasMaskFilter.KEY, 4321);
        assertEquals(4321, GasMaskFilter.life(tag));
        tag.setInteger(GasMaskFilter.KEY, 0);
        assertEquals(0, GasMaskFilter.life(tag));
    }

    @Test
    void aBrokenTagIsClamped()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(GasMaskFilter.KEY, -7);
        assertEquals(0, GasMaskFilter.life(tag));
        tag.setInteger(GasMaskFilter.KEY, 999999);
        assertEquals(GasMaskFilter.LIFE, GasMaskFilter.life(tag));
    }

    @Test
    void theTagIsWrittenAtEachPercentStep()
    {
        assertTrue(GasMaskFilter.step(GasMaskFilter.LIFE - GasMaskFilter.STEP));
        assertFalse(GasMaskFilter.step(GasMaskFilter.LIFE - 1));
        assertTrue(GasMaskFilter.step(0));
        // between two steps the percent shown stays the same, so a tag one step old shows what the wearer has
        assertEquals(GasMaskFilter.percent(GasMaskFilter.LIFE - 1),
                     GasMaskFilter.percent(GasMaskFilter.LIFE - GasMaskFilter.STEP + 1));
    }

    @Test
    void fittingAFilter()
    {
        assertEquals(GasMaskFilter.Fit.FIT, GasMaskFilter.fit(true, 1, 0));
        assertEquals(GasMaskFilter.Fit.FIT, GasMaskFilter.fit(true, 1, 99));
        assertEquals(GasMaskFilter.Fit.FULL, GasMaskFilter.fit(true, 1, 100));    // a full filter is never thrown away
        assertEquals(GasMaskFilter.Fit.MANY, GasMaskFilter.fit(true, 2, 0));      // one filter, one mask
        assertEquals(GasMaskFilter.Fit.NO_MASK, GasMaskFilter.fit(false, 1, 0));
    }

    @Test
    void theShareOfOpensWithOneOrMoreOfAnItem()
    {
        // an open rolls 1 .. n times, each as likely (Decimation's IntRange with PatchLootRolls)
        assertEquals(0.0, GasMaskFilter.perOpen(0.0, 2), 1e-12);
        assertEquals(1.0, GasMaskFilter.perOpen(1.0, 2), 1e-12);
        assertEquals(0.1, GasMaskFilter.perOpen(0.1, 1), 1e-12);
        assertEquals(1 - (0.9 + 0.81) / 2, GasMaskFilter.perOpen(0.1, 2), 1e-12);
    }

    @Test
    void filterCopiesForAboutThreePercentOfMilitaryAndSevenOfPoliceCrates()
    {
        // user, 22 Oktober 2026: filters in about 3 % of military crate opens, police as built (about 7 %)
        assertEquals(8, GasMaskFilter.copiesFor(184.7, 0.5, 2, 0.03));   // 7 copies: 2.8 %, 8: 3.2 %
        assertEquals(1, GasMaskFilter.copiesFor(10.1, 0.5, 2, 0.07));    // 7.0 %
        assertEquals(1, GasMaskFilter.copiesFor(1000, 0.5, 2, 0.00001)); // never none
    }

    @Test
    void decimationsTearGasReach()
    {
        // TearGasGrenadeEntity: players whose box meets x, z within 6 and y -3 .. +5 of the gas and under 5 blocks away
        assertTrue(GasMaskFilter.inGas(0, 0, 0));
        assertTrue(GasMaskFilter.inGas(4.9, 0, 0));
        assertFalse(GasMaskFilter.inGas(5.0, 0, 0));
        assertTrue(GasMaskFilter.inGas(0, 4.9, 0));
        assertFalse(GasMaskFilter.inGas(0, -4.9, 0));                    // the feet 4.9 below: the box ends at 3 below
        assertTrue(GasMaskFilter.inGas(0, -4.7, 0));
    }

    @Test
    void anEmptyFilterLetsTearGasThrough()
    {
        // Decimation's own check spares any gas mask item (and poisons a player without one): ours only for empty filters
        assertTrue(GasMaskFilter.poisonDue(false, false, true, 0));
        assertFalse(GasMaskFilter.poisonDue(false, false, true, 1));
        assertFalse(GasMaskFilter.poisonDue(false, false, false, 0));     // no mask: Decimation's own path
        assertFalse(GasMaskFilter.poisonDue(true, false, true, 0));       // creative
        assertFalse(GasMaskFilter.poisonDue(false, true, true, 0));       // already poisoned, as Decimation's check
    }

    @Test
    void warnedOnceAtTenPercentAndOnceWhenEmpty()
    {
        assertEquals(GasMaskFilter.Warn.LOW, GasMaskFilter.warning(GasMaskFilter.LOW + 1, GasMaskFilter.LOW));
        assertEquals(GasMaskFilter.Warn.EMPTY, GasMaskFilter.warning(1, 0));
        assertNull(GasMaskFilter.warning(GasMaskFilter.LOW, GasMaskFilter.LOW - 1));
        assertNull(GasMaskFilter.warning(5000, 4999));
        assertNull(GasMaskFilter.warning(0, 0));
        assertNull(GasMaskFilter.warning(GasMaskFilter.LOW, GasMaskFilter.LOW));
    }
}
