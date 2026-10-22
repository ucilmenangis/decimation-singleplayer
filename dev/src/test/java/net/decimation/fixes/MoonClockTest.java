package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The moon phase from our day counter (WorldClock), not world time: Decimation's DayLengthHandler puts world time back to
 * 0 after 24000, so vanilla's phase (world time / 24000 % 8) was a full moon every night. Vanilla's cycle kept: day 1 a
 * full moon (phase 0), day 5 a new moon (phase 4), eight days round.
 */
class MoonClockTest
{
    @Test
    void dayOneIsAFullMoonDayFiveANewOne()
    {
        assertEquals(0, MoonClock.phaseOf(1));
        assertEquals(4, MoonClock.phaseOf(5));
        assertEquals(7, MoonClock.phaseOf(8));
        assertEquals(0, MoonClock.phaseOf(9));
        assertEquals(4, MoonClock.phaseOf(13));
    }

    @Test
    void anUnknownDayIsAFullMoon()
    {
        assertEquals(0, MoonClock.phaseOf(0));
        assertEquals(0, MoonClock.phaseOf(-3));
    }
}
