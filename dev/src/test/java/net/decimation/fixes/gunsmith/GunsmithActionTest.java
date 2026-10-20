package net.decimation.fixes.gunsmith;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The Gunsmith screen's six slots and the list clicks it sends as container button ids (spec 2026-10-21 section 2). */
class GunsmithActionTest
{
    @Test
    void slotsMatchDecimation()
    {
        // deciIndex = the slot's index in Decimation's WeaponModifyContainer (FilteredSlot rules key on it)
        assertEquals(2, GunSlot.SIGHT.deciIndex);
        assertEquals(1, GunSlot.BARREL.deciIndex);
        assertEquals(4, GunSlot.GRIP.deciIndex);
        assertEquals(3, GunSlot.STOCK.deciIndex);
        assertEquals(5, GunSlot.PAINT.deciIndex);
        assertEquals(6, GunSlot.TRACER.deciIndex);
        assertEquals("sightAttach", GunSlot.SIGHT.nbtKey);
        assertEquals("skin", GunSlot.PAINT.nbtKey);
        assertEquals("tracerColorID", GunSlot.TRACER.nbtKey);
        assertEquals("Grips you carry", GunSlot.GRIP.listTitle());
        assertTrue(GunSlot.PAINT.usedUp());
        assertTrue(GunSlot.TRACER.usedUp());
        assertFalse(GunSlot.SIGHT.usedUp());
        assertFalse(GunSlot.STOCK.usedUp());
    }

    /** What the id becomes on a dedicated server: C11PacketEnchantItem writes it as one byte and reads it signed. */
    static int overTheWire(int id)
    {
        return (byte) id;
    }

    @Test
    void everyIdSurvivesTheNetwork()
    {
        for (GunSlot s : GunSlot.values())
        {
            for (int inv = 0; inv < 36; inv++)
            {
                int id = GunsmithAction.fit(s, inv);
                assertEquals(id, overTheWire(id), s + " " + inv);
                GunsmithAction a = GunsmithAction.decode(overTheWire(id));
                assertNotNull(a, s + " " + inv);
                assertFalse(a.takeOff);
                assertEquals(s, a.slot);
                assertEquals(inv, a.invIndex);
            }
            if (!s.usedUp())
            {
                int id = GunsmithAction.takeOff(s);
                assertEquals(id, overTheWire(id), "take off " + s);
                GunsmithAction a = GunsmithAction.decode(overTheWire(id));
                assertTrue(a.takeOff);
                assertEquals(s, a.slot);
            }
        }
    }

    @Test
    void everyIdIsDistinct()
    {
        java.util.Set<Integer> ids = new java.util.HashSet<Integer>();
        for (GunSlot s : GunSlot.values())
        {
            for (int inv = 0; inv < 36; inv++)
            {
                assertTrue(ids.add(GunsmithAction.fit(s, inv)), s + " " + inv);
            }
            assertTrue(ids.add(GunsmithAction.takeOff(s)), "take off " + s);
        }
    }

    @Test
    void badIdsDecodeToNull()
    {
        // past the last take off, and the take off of a used up part (paint, tracer)
        for (int id : new int[] {overTheWire(222), overTheWire(240), overTheWire(255), GunsmithAction.takeOff(GunSlot.PAINT),
                                 GunsmithAction.takeOff(GunSlot.TRACER)})
        {
            assertNull(GunsmithAction.decode(id), "id " + id);
        }
    }

    @Test
    void textsHaveNoDashes()
    {
        for (GunSlot s : GunSlot.values())
        {
            assertFalse(s.listTitle().matches(".*[-\\u2013\\u2014].*"), s.listTitle());
            assertFalse(s.label.matches(".*[-\\u2013\\u2014].*"), s.label);
        }
    }
}
