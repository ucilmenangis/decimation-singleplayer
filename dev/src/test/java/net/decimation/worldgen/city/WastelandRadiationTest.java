package net.decimation.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The wasteland city one radiation zone (user, 22 Oktober 2026, idea 43: "wasteland city become full radiation zone on all
 * place"): one box over a whole sector wasteland city (worlds since v0.57.1), else one over each district, every height.
 */
class WastelandRadiationTest
{
    @Test
    void aDistrictBoxIsTheGroundOfItsMilitaryBox()
    {
        // district of cells (2, -4): blocks 128 .. 255 and -256 .. -129, every height (Wasteland.zone's military box)
        assertArrayEquals(new int[] {128, 0, -256, 255, 255, -129}, WastelandRadiation.box(2, -4, false));
        assertArrayEquals(new int[] {-128, 0, 0, -1, 255, 127}, WastelandRadiation.box(-2, 0, false));
    }

    @Test
    void aSectorBoxCoversTheWholeWastelandCity()
    {
        // whole sector wasteland cities: the region's 4 x 4 cells, 256 x 256
        assertArrayEquals(new int[] {0, 0, -256, 255, 255, -1}, WastelandRadiation.box(2, -4, true));
        assertArrayEquals(new int[] {-256, 0, -256, -1, 255, -1}, WastelandRadiation.box(-2, -2, true));
        // the 4 districts of one sector share one box and one marker
        assertArrayEquals(WastelandRadiation.box(0, 0, true), WastelandRadiation.box(2, 2, true));
        assertEquals(WastelandRadiation.marker(0, 0, true), WastelandRadiation.marker(2, 2, true));
        assertNotEquals(WastelandRadiation.marker(0, 0, false), WastelandRadiation.marker(2, 2, false));
        assertNotEquals(WastelandRadiation.marker(0, 0, true), WastelandRadiation.marker(0, 0, false));
    }

    @Test
    void wastelandMilitaryBoxesAreRecognised()
    {
        assertArrayEquals(new int[] {2, -4}, WastelandRadiation.district(128, 0, -256, 255, 255, -129));
        assertArrayEquals(new int[] {-2, 0}, WastelandRadiation.district(-128, 0, 0, -1, 255, 127));
        assertNull(WastelandRadiation.district(128, 60, -256, 255, 255, -129));    // a base's box: not every height
        assertNull(WastelandRadiation.district(192, 0, -256, 319, 255, -129));     // not on the district grid
        assertNull(WastelandRadiation.district(128, 0, -256, 200, 255, -129));     // not 128 wide
    }
}
