package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * NPC tracers (user, 22 Oktober 2026: "npc who shot gun can randomly use other tracer instead white only, all guns now
 * shot full tracer"; chosen: each armed NPC one random colour for life, every NPC shot a tracer). Decimation's client
 * draws an NPC shot white 40 % of the time and nothing otherwise, unless the gun carries a tracer colour
 * (GunItem.getTracerColor, NBT tracerColorID): then that colour on every shot. The NPC's gun spec carries it.
 */
class NpcTracerTest
{
    @Test
    void aSpecWithoutTracerGetsANamedColour()
    {
        Random r = new Random(1);
        Set<Integer> seen = new HashSet<Integer>();
        for (int i = 0; i < 200; i++)
        {
            String spec = NpcLoadouts.withTracer("m4a4;sight=4x;grip=laser", r);
            int c = NpcLoadouts.tracerOf(spec);
            assertTrue(c >= 1 && c <= 7, spec);                 // WHITE 1 .. YELLOW 7: never NONE 0, GENERIC 8, INVIS 9
            assertTrue(spec.startsWith("m4a4;sight=4x;grip=laser;tracer="), spec);
            seen.add(c);
        }
        assertEquals(7, seen.size());
    }

    @Test
    void aSpecKeepsItsColour()
    {
        String spec = "akm;tracer=3";
        assertSame(spec, NpcLoadouts.withTracer(spec, new Random()));
        assertEquals(3, NpcLoadouts.tracerOf(spec));
        assertEquals(0, NpcLoadouts.tracerOf("akm;sight=reddot"));
        assertEquals(0, NpcLoadouts.tracerOf(""));
    }
}
