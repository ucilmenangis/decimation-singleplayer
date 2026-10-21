package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/** Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"). */
class MagUnloadTest
{
    @Test
    void roundsComeOutInFullStacks()
    {
        assertArrayEquals(new int[] {20}, MagUnload.stacks(20, 64));
        assertArrayEquals(new int[] {64, 36}, MagUnload.stacks(100, 64));
        assertArrayEquals(new int[] {30, 30, 15}, MagUnload.stacks(75, 30));
        assertArrayEquals(new int[0], MagUnload.stacks(0, 64));
    }
}
