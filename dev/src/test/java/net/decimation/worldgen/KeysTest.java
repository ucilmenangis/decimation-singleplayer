package net.decimation.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Keys of int pairs: unique, and spread over a HashMap's buckets (a Long hashes to its high ^ low half). */
class KeysTest
{
    @Test
    void pairsAreUnique()
    {
        Set<String> pairs = new HashSet<String>();
        Set<Long> keys = new HashSet<Long>();
        int[] edges = {Integer.MIN_VALUE, -30000000, -1, 0, 1, 30000000, Integer.MAX_VALUE};
        for (int a : edges)
        {
            for (int b : edges)
            {
                pairs.add(a + "," + b);
                keys.add(Keys.pair(a, b));
            }
        }
        for (int a = -40; a < 40; a++)
        {
            for (int b = -40; b < 40; b++)
            {
                pairs.add(a + "," + b);
                keys.add(Keys.pair(a, b));
            }
        }
        assertEquals(pairs.size(), keys.size(), "one key per pair");
    }

    @Test
    void aWindowFillsTheBuckets()
    {
        // one population window (16 x 16 columns at +8) and a block of 32 x 32 regions: the hash's low bits pick the
        // bucket, so count distinct low 8 bits (a HashMap of 256 buckets)
        for (int[] at : new int[][] {{8, 8}, {-1016, 4104}, {456, -1016}})
        {
            Set<Integer> buckets = new HashSet<Integer>();
            for (int x = at[0]; x < at[0] + 16; x++)
            {
                for (int z = at[1]; z < at[1] + 16; z++)
                {
                    buckets.add(Long.valueOf(Keys.pair(x, z)).hashCode() & 255);
                }
            }
            assertTrue(buckets.size() >= 128, "window at " + at[0] + "," + at[1] + ": " + buckets.size() + " of 256 buckets");
        }
        Set<Integer> buckets = new HashSet<Integer>();
        for (int rx = -16; rx < 16; rx++)
        {
            for (int rz = -16; rz < 16; rz++)
            {
                buckets.add(Long.valueOf(Keys.pair(rx, rz)).hashCode() & 1023);
            }
        }
        assertTrue(buckets.size() >= 512, "regions: " + buckets.size() + " of 1024 buckets");
    }
}
