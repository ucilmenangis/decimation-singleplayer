package net.decimation.fixes;

import static net.decimation.fixes.InventoryShift.Region.ARMOR;
import static net.decimation.fixes.InventoryShift.Region.BACKPACK;
import static net.decimation.fixes.InventoryShift.Region.GEAR;
import static net.decimation.fixes.InventoryShift.Region.HOTBAR;
import static net.decimation.fixes.InventoryShift.Region.LOOT;
import static net.decimation.fixes.InventoryShift.Region.MAIN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/** bug.md "Inventory: no shift click": where a shift click sends a stack, region by region. */
class InventoryShiftTest
{
    @Test
    void wornThingsAndOtherInventoriesGoToTheInventoryMainFirst()
    {
        for (InventoryShift.Region from : new InventoryShift.Region[] {ARMOR, GEAR, BACKPACK, LOOT})
        {
            assertArrayEquals(new InventoryShift.Region[] {MAIN, HOTBAR}, InventoryShift.targets(from, false));
            assertArrayEquals(new InventoryShift.Region[] {MAIN, HOTBAR}, InventoryShift.targets(from, true));
        }
    }

    @Test
    void inventoryStacksArePutOnFirstThenSwapRows()
    {
        assertArrayEquals(new InventoryShift.Region[] {ARMOR, GEAR, HOTBAR}, InventoryShift.targets(MAIN, false));
        assertArrayEquals(new InventoryShift.Region[] {ARMOR, GEAR, MAIN}, InventoryShift.targets(HOTBAR, false));
    }

    @Test
    void aWornBackpackTakesInventoryStacksBeforeTheOtherRow()
    {
        assertArrayEquals(new InventoryShift.Region[] {ARMOR, GEAR, BACKPACK, HOTBAR}, InventoryShift.targets(MAIN, true));
        assertArrayEquals(new InventoryShift.Region[] {ARMOR, GEAR, BACKPACK, MAIN}, InventoryShift.targets(HOTBAR, true));
    }
}
