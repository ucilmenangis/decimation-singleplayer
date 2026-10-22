package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * NPC tracers: Decimation's client draws an NPC shot white 40 % of the time and nothing otherwise, unless the gun carries a
 * tracer colour (GunItem.getTracerColor, NBT tracerColorID): then that colour on every shot. The NPC's gun spec carries
 * it. User, 22 Oktober 2026: a random colour per NPC and a tracer on every shot; 23 Oktober 2026: "make the npc shot white
 * tracer again, just changing the color only, the tracer each shot still intact": every NPC GENERIC (8, #FFF6E8, the near
 * white Decimation drew NPC tracers in), every shot.
 */
class NpcTracerTest
{
    @Test
    void everySpecGetsDecimationsWhiteTracer()
    {
        assertEquals("m4a4;sight=4x;grip=laser;tracer=8", NpcLoadouts.withTracer("m4a4;sight=4x;grip=laser"));
        assertEquals("akm;tracer=8", NpcLoadouts.withTracer("akm;tracer=3"));           // a colour saved since 22 Oktober
        assertEquals("akm;mask=gas;tracer=8", NpcLoadouts.withTracer("akm;tracer=5;mask=gas"));
        assertEquals("akm;tracer=8", NpcLoadouts.withTracer("akm;tracer=1"));
        String white = "akm;tracer=8";
        assertSame(white, NpcLoadouts.withTracer(white));                            // nothing to save again
    }

    @Test
    void theTracerIdOfASpec()
    {
        assertEquals(3, NpcLoadouts.tracerOf("akm;tracer=3"));
        assertEquals(0, NpcLoadouts.tracerOf("akm;sight=reddot"));
        assertEquals(0, NpcLoadouts.tracerOf(""));
    }
}
