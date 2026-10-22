package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.item.ItemStack;

/**
 * Tarkov style search (feature 12 C, user 22 Oktober 2026: "loot system where it need to be analyzed one by one before
 * taking it. so the item will show loading icon animation then if it done it can be take it"; chosen: by value, 1 to 3 s
 * an item).
 */
class LootSearchTest
{
    @Test
    void revealsFilledSlotsInSlotOrderEachAfterItsTime()
    {
        LootSearch.Search s = new LootSearch.Search(new int[] {2, 5, 9}, new int[] {20, 40, 60});
        assertTrue(s.hidden(2) && s.hidden(5) && s.hidden(9));
        assertFalse(s.hidden(0), "an empty slot hides nothing");
        assertEquals(2, s.searching());
        for (int t = 0; t < 19; t++)
        {
            assertFalse(s.tick(), "nothing before its 20 ticks");
        }
        assertTrue(s.tick(), "slot 2 at tick 20");
        assertFalse(s.hidden(2));
        assertTrue(s.hidden(5));
        assertEquals(5, s.searching());
        for (int t = 0; t < 39; t++)
        {
            s.tick();
        }
        assertTrue(s.hidden(5));
        assertTrue(s.tick(), "slot 5 forty ticks later");
        for (int t = 0; t < 60; t++)
        {
            s.tick();
        }
        assertFalse(s.hidden(9));
        assertEquals(-1, s.searching(), "all searched");
        assertFalse(s.tick(), "nothing more to reveal");
        assertTrue(s.finished());
    }

    @Test
    void anEmptyInventoryIsFinishedAtOnce()
    {
        LootSearch.Search s = new LootSearch.Search(new int[0], new int[0]);
        assertTrue(s.finished());
        assertEquals(-1, s.searching());
    }

    @Test
    void progressOfTheSlotBeingSearched()
    {
        LootSearch.Search s = new LootSearch.Search(new int[] {0}, new int[] {40});
        for (int t = 0; t < 10; t++)
        {
            s.tick();
        }
        assertEquals(10, s.done());
        assertEquals(40, s.total());
    }

    @Test
    void secondsByValue()
    {
        // the registries are empty in a unit test (Items.* are null): items made here
        assertEquals(1, LootSearch.seconds(new ItemStack(new net.minecraft.item.ItemFood(4, false))), "food and the rest 1 s");
        assertEquals(2, LootSearch.seconds(new ItemStack(new net.minecraft.item.ItemArmor(net.minecraft.item.ItemArmor.ArmorMaterial.IRON, 0, 1))),
                     "armor 2 s");
        assertEquals(1, LootSearch.seconds(null));
    }

    @Test
    void aSharedSearchAdvancesOnceAServerTickHoweverManyWatch()
    {
        LootSearch.Search s = new LootSearch.Search(new int[] {0}, new int[] {20});
        for (int t = 1; t <= 10; t++)
        {
            s.tickAt(t);
            s.tickAt(t);                                    // a second player looking at the same body
        }
        assertEquals(10, s.done(), "10 server ticks, not 20");
    }

    @Test
    void theServerFailsClosedBeforeTheFirstTick()
    {
        net.minecraft.inventory.InventoryBasic inv = new net.minecraft.inventory.InventoryBasic("t", false, 4);
        inv.setInventorySlotContents(2, new ItemStack(new net.minecraft.item.ItemFood(4, false)));
        assertEquals(null, LootSearch.progress(inv), "no search yet");
        assertTrue(LootSearch.hiddenOnServer(inv, 2), "a filled slot is hidden before the tick handler saw the screen");
        assertFalse(LootSearch.hiddenOnServer(inv, 1), "an empty slot is not");
        assertEquals(1, LootSearch.progress(inv)[3], "the search started there");
    }
}
