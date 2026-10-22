package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Lootable copies and loot kinds (feature 12 B, user 22 Oktober 2026: "shelf can be civilian loot or military loot or police
 * loot, weapon cabinet now have 2 version its civilian loot with name cabinet (without weapon word) and police loot and
 * military loot"; trucks, water pallets and the two military radios lootable, a plain wooden crate).
 */
class LootBlocksTest
{
    @Test
    void shelvesByPlace()
    {
        assertEquals("deci:BlockMetalShelf", LootBlocks.forPlace("deci:BlockMetalShelf", LootBlocks.CIVILIAN));
        assertEquals("deciworldgen:shelf_police", LootBlocks.forPlace("deci:BlockMetalShelf", LootBlocks.POLICE));
        assertEquals("deciworldgen:shelf_military", LootBlocks.forPlace("deci:BlockMetalShelf", LootBlocks.MILITARY));
    }

    @Test
    void cabinetsByPlaceTheOriginalStaysMilitary()
    {
        assertEquals("deciworldgen:cabinet", LootBlocks.forPlace("deci:BlockWeaponCabinet", LootBlocks.CIVILIAN));
        assertEquals("deciworldgen:cabinet_police", LootBlocks.forPlace("deci:BlockWeaponCabinet", LootBlocks.POLICE));
        assertEquals("deci:BlockWeaponCabinet", LootBlocks.forPlace("deci:BlockWeaponCabinet", LootBlocks.MILITARY));
    }

    @Test
    void anythingElseStaysAsItIs()
    {
        for (int p = 0; p < 3; p++)
        {
            assertEquals("deci:BlockMetalShelf_Empty", LootBlocks.forPlace("deci:BlockMetalShelf_Empty", p));
            assertEquals("deci:BlockMilitaryCrate", LootBlocks.forPlace("deci:BlockMilitaryCrate", p));
        }
    }

    @Test
    void everyKindNamesACopyThatExists()
    {
        Set<String> ours = new HashSet<String>();
        for (LootBlocks.Copy c : LootBlocks.COPIES)
        {
            assertTrue(ours.add("deciworldgen:" + c.name), "one name once: " + c.name);
        }
        for (int p = 0; p < 3; p++)
        {
            for (String n : new String[] {"deci:BlockMetalShelf", "deci:BlockWeaponCabinet"})
            {
                String to = LootBlocks.forPlace(n, p);
                assertTrue(to.equals(n) || ours.contains(to), to + " is a copy");
            }
        }
    }

    @Test
    void theUsersCopiesAndTheirLists()
    {
        int trucks = 0, lootable = 0;
        for (LootBlocks.Copy c : LootBlocks.COPIES)
        {
            trucks += c.original.startsWith("deci:BlockTruckWreckage") ? 1 : 0;
            lootable += c.like != null ? 1 : 0;
            if (c.name.equals("woodcrate_plain"))
            {
                assertEquals(null, c.like, "the plain wooden crate has no list");
            }
            if (c.name.startsWith("militaryradio"))
            {
                assertEquals("deci:BlockMilitaryCrate", c.like);
            }
        }
        assertEquals(6, trucks, "six pickup trucks");
        assertEquals(14, lootable);
        assertEquals(15, LootBlocks.COPIES.size());
    }
}
