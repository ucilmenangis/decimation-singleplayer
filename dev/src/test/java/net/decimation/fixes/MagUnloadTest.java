package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"; then "make it same
 * like load ammo process code ... its reverse"): Decimation's AmmoItem.onItemRightClick backwards, one round a click.
 */
class MagUnloadTest
{
    @Test
    void oneMagazineWithRoundsGivesOneRound()
    {
        assertEquals(MagUnload.Step.UNLOAD, MagUnload.step(20, 1));
        assertEquals(MagUnload.Step.UNLOAD, MagUnload.step(1, 1));
        assertNull(MagUnload.message(MagUnload.Step.UNLOAD, 1));
    }

    @Test
    void anEmptyMagazineSaysSo()
    {
        assertEquals(MagUnload.Step.EMPTY, MagUnload.step(0, 1));
        assertEquals("This Magazine is empty!", MagUnload.message(MagUnload.Step.EMPTY, 1));
    }

    @Test
    void aStackOfMagazinesIsRefusedLikeLoading()
    {
        assertEquals(MagUnload.Step.MANY, MagUnload.step(20, 3));
        assertEquals(MagUnload.Step.MANY, MagUnload.step(0, 2));
        assertEquals("You can only unload 1 magazine at a time! Not 3!", MagUnload.message(MagUnload.Step.MANY, 3));
    }
}
