package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The SpecterDR's 1x / 4x mode per gun for the session: by Decimation's "uniqueID" of the gun, else by its slot. */
class SpecterModeTest
{
    @BeforeEach
    void clear()
    {
        SpecterMode.reset();
    }

    @Test
    void aNewGunIsFourTimes()
    {
        assertTrue(SpecterMode.isFourById("gunA", 0));
    }

    @Test
    void toggleFlips()
    {
        assertFalse(SpecterMode.toggleById("gunA", 0));
        assertFalse(SpecterMode.isFourById("gunA", 0));
        assertTrue(SpecterMode.toggleById("gunA", 0));
        assertTrue(SpecterMode.isFourById("gunA", 0));
    }

    @Test
    void gunsWithTheirOwnIdsKeepTheirOwnModes()
    {
        SpecterMode.toggleById("gunA", 0);
        assertFalse(SpecterMode.isFourById("gunA", 0));
        assertTrue(SpecterMode.isFourById("gunB", 0));
        assertFalse(SpecterMode.isFourById("gunA", 5)); // moved to another slot: still its own mode
    }

    @Test
    void aGunWithoutAnIdFallsBackToItsSlot()
    {
        SpecterMode.toggleById(null, 3);
        assertFalse(SpecterMode.isFourById(null, 3));
        assertFalse(SpecterMode.isFourById("", 3));
        assertTrue(SpecterMode.isFourById(null, 4));
        assertTrue(SpecterMode.isFourById("gunA", 3));
    }
}
