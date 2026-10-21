package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Sights need a mount (user, 22 Oktober 2026: "same rule applied on sight too. for red dot i think its better to put only
 * on pistol"; chosen: the red dot on pistols with an optic cut or rail, the other sights on a top rail or a side mount).
 */
class SightMountsTest
{
    @Test
    void redDotOnlyOnPistolsWithAMount()
    {
        SightMounts.load();
        assertTrue(SightMounts.fits("glock17", "reddot"));
        assertTrue(SightMounts.fits("deagle", "reddot"));
        assertFalse(SightMounts.fits("makarov", "reddot"));
        assertFalse(SightMounts.fits("m4a4", "reddot"));
        assertFalse(SightMounts.fits("akm", "reddot"));
        assertFalse(SightMounts.fits("mp5a3", "reddot"));
    }

    @Test
    void otherSightsOnARailOrASideMount()
    {
        SightMounts.load();
        assertTrue(SightMounts.fits("m4a4", "eotech558"));
        assertTrue(SightMounts.fits("akm", "ta11acog"));          // side mount
        assertTrue(SightMounts.fits("svd", "dragunovScope"));
        assertTrue(SightMounts.fits("mp5a3", "2x"));              // HK claw mount points
        assertFalse(SightMounts.fits("kar98k", "4x"));
        assertFalse(SightMounts.fits("m16a2", "specterdr"));
        assertFalse(SightMounts.fits("glock17", "4x"));           // a pistol's mount takes the red dot only
        assertFalse(SightMounts.fits("uzi", "eotech558"));
    }

    @Test
    void pistolsWithAMountAreTheTableOnes()
    {
        SightMounts.load();
        assertEquals(new TreeSet<String>(Arrays.asList("barettam9", "deagle", "glock17")), SightMounts.guns("slide"));
        Set<String> side = SightMounts.guns("side");
        assertTrue(side.containsAll(Arrays.asList("ak74", "akm", "rpk", "svd", "pkm")));
    }
}
