package net.decimation.worldgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Which kind of place a block is (specific mobs at specific places, idea 35): the parts that need no world. */
class PlacesTest
{
    @Test
    void theMostSpecificPlaceComesFirst()
    {
        // the airport lies in the wasteland city, hospitals and police stations in cities: the first that holds wins
        assertEquals(Arrays.asList(Places.Kind.AIRPORT, Places.Kind.BASE, Places.Kind.POLICE_STATION, Places.Kind.HOSPITAL,
                                   Places.Kind.HIGHWAY, Places.Kind.WASTELAND, Places.Kind.FOREST), Arrays.asList(Places.Kind.values()));
        assertEquals(Places.Kind.POLICE_STATION, Places.Kind.byId("police_station"));
        assertEquals("forest", Places.Kind.FOREST.id);
        assertNull(Places.Kind.byId("moon"));
    }

    @Test
    void basesAreTheNamedPlacesOfTheMilitary()
    {
        // StructureData's place entries, as PlaceNames writes them: "x0/0/z0=x1/255/z1=owner=name"
        assertArrayEquals(new int[] {-128, 256, -1, 383}, Places.baseBox("-128/0/256=-1/255/383=Military=FOB_Viper"));
        assertNull(Places.baseBox("0/0/0=15/255/15=unknown=Ashford"));
        assertNull(Places.baseBox("broken"));
    }

    @Test
    void theChunkMemoRemembersAndForgets()
    {
        Places.Memo m = new Places.Memo(16);
        assertEquals(0, m.get(3, -7));
        m.put(3, -7, (byte) 4);
        assertEquals(4, m.get(3, -7));
        assertEquals(0, m.get(-7, 3));
        for (int i = 0; i < 64; i++)
        {
            m.put(i, i, (byte) 2);                                       // more chunks than slots: older ones go
        }
        int kept = 0;
        for (int i = 0; i < 64; i++)
        {
            kept += m.get(i, i) == 2 ? 1 : 0;
        }
        assertEquals(true, kept >= 1 && kept <= 16, "a slot holds one chunk: " + kept);
        assertEquals(2, m.get(63, 63));                                  // the last one put is there
        m.clear();
        assertEquals(0, m.get(63, 63));
    }
}
