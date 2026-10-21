package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Foregrips need a rail under the handguard (user, 21 Oktober 2026: "for RPK and some guns that doesnt have picatiny
 * rail, they dont support any grip on it"; docs/references/grip_rails.md). The shipped grip_offsets.txt is the list.
 */
class GripRailsTest
{
    /** The railed rifles of docs/references/grip_rails.md section 4 (snipers' bipod rails do not count). */
    static final Set<String> RAILED = new TreeSet<String>(Arrays.asList(
        "acr", "ak12", "ar15", "ar15beowulf", "aug3", "aug3para", "fnscar", "fnscarhamr", "fnscarl", "fnscarsc",
        "fnscarssr", "g36c", "honeybadger", "m110", "m4a4", "mpt76", "hk416", "hk416tan", "mk18"));

    @Test
    void foregripsNeedARailLightsDoNot()
    {
        assertTrue(GripRails.needsRail("foregrip"));
        assertTrue(GripRails.needsRail("rk1"));
        assertFalse(GripRails.needsRail("laser"));
        assertFalse(GripRails.needsRail("flashlight"));
    }

    @Test
    void railedRiflesTakeForegripsOthersDoNot()
    {
        GripRails.load();
        assertTrue(GripRails.fits("m4a4", "rk1"));
        assertTrue(GripRails.fits("fnscar", "foregrip"));
        assertFalse(GripRails.fits("rpk", "foregrip"));
        assertFalse(GripRails.fits("rpk74", "rk1"));
        assertFalse(GripRails.fits("ak74", "foregrip"));
        assertFalse(GripRails.fits("barrett", "foregrip"));     // bipod rail only
        assertTrue(GripRails.fits("rpk", "laser"));              // lights stay on every gun
        assertTrue(GripRails.fits("glock17", "flashlight"));
    }

    @Test
    void theTableListsForegripsOnRailedRiflesOnly()
    {
        GripRails.load();
        assertEquals(RAILED, GripRails.railed("foregrip"));
        assertEquals(RAILED, GripRails.railed("rk1"));
    }
}
