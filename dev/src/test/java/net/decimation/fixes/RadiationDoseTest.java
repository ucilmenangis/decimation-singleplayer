package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Radiation dose (user, 22 Oktober 2026: "radiation status below player status"; chosen: a dose in percent, always on the
 * HUD, Decimation's zone damage kept and the dose fast, a high dose hurts after leaving the zone until it falls back).
 */
class RadiationDoseTest
{
    @Test
    void risesToFullInTwentySecondsWithoutAMask()
    {
        int d = 0;
        for (int t = 0; t < 399; t++)
        {
            d = RadiationDose.next(d, true);
        }
        assertTrue(d < RadiationDose.MAX, "not full after 399 ticks: " + d);
        assertEquals(RadiationDose.MAX, RadiationDose.next(d, true));
        assertEquals(RadiationDose.MAX, RadiationDose.next(RadiationDose.MAX, true));
    }

    @Test
    void fallsToNothingInTwentyMinutes()
    {
        int d = RadiationDose.MAX;
        for (int t = 0; t < 23999; t++)
        {
            d = RadiationDose.next(d, false);
        }
        assertTrue(d > 0, "already gone after 23999 ticks");
        assertEquals(0, RadiationDose.next(d, false));
        assertEquals(0, RadiationDose.next(0, false));
    }

    @Test
    void percentAsShown()
    {
        assertEquals(0, RadiationDose.percent(0));
        assertEquals(0, RadiationDose.percent(RadiationDose.MAX / 100 - 1));
        assertEquals(1, RadiationDose.percent(RadiationDose.MAX / 100));
        assertEquals(50, RadiationDose.percent(RadiationDose.MAX / 2));
        assertEquals(100, RadiationDose.percent(RadiationDose.MAX));
    }

    @Test
    void hurtsFromHalfFasterFromEightyNeverBelow()
    {
        assertEquals(0, RadiationDose.hurtEvery(0));
        assertEquals(0, RadiationDose.hurtEvery(49));
        assertEquals(160, RadiationDose.hurtEvery(50));
        assertEquals(160, RadiationDose.hurtEvery(79));
        assertEquals(80, RadiationDose.hurtEvery(80));
        assertEquals(80, RadiationDose.hurtEvery(100));
    }

    @Test
    void tenSecondsUnmaskedIsHalfADose()
    {
        int d = 0;
        for (int t = 0; t < 200; t++)
        {
            d = RadiationDose.next(d, true);
        }
        assertEquals(50, RadiationDose.percent(d));
    }

    @Test
    void theSaveKeepsTheDoseAndANewLifeStartsAtNothing()
    {
        RadiationDose.Dose d = new RadiationDose.Dose();
        d.dose = 12345;
        NBTTagCompound tag = new NBTTagCompound();
        d.saveNBTData(tag);
        RadiationDose.Dose back = new RadiationDose.Dose();
        back.loadNBTData(tag);
        assertEquals(12345, back.dose);
        assertEquals(-1, back.sent);                  // the client gets told at once after a load
        assertEquals(0, new RadiationDose.Dose().dose);
    }

    @Test
    void aPillTakesFortyPointsNeverBelowNothing()
    {
        // radiation pills (user, 22 Oktober 2026, idea 41): 40 percentage points off at once
        assertEquals(RadiationDose.MAX * 50 / 100, RadiationDose.lowered(RadiationDose.MAX * 90 / 100, 40));
        assertEquals(50, RadiationDose.percent(RadiationDose.lowered(RadiationDose.MAX * 90 / 100, 40)));
        assertEquals(RadiationDose.MAX * 60 / 100, RadiationDose.lowered(RadiationDose.MAX, 40));
        assertEquals(0, RadiationDose.lowered(RadiationDose.MAX * 20 / 100, 40));
        assertEquals(0, RadiationDose.lowered(0, 40));
    }

    @Test
    void aBrokenSaveIsClamped()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(RadiationDose.KEY, 999999);
        RadiationDose.Dose d = new RadiationDose.Dose();
        d.loadNBTData(tag);
        assertEquals(RadiationDose.MAX, d.dose);
        tag.setInteger(RadiationDose.KEY, -5);
        d.loadNBTData(tag);
        assertEquals(0, d.dose);
    }
}
