package net.decimation.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** BlockBatch keeps the cells to light as packed longs: every world position comes back as it went in. */
class BlockBatchTest
{
    @Test
    void cellsComeBack()
    {
        int[] xs = {-30000000, -29999999, -1016, -1, 0, 1, 455, 29999999};
        int[] ys = {0, 1, 63, 64, 128, 254, 255};
        for (int x : xs)
        {
            for (int z : xs)
            {
                for (int y : ys)
                {
                    long p = BlockBatch.pack(x, y, z);
                    assertEquals(x, BlockBatch.unpackX(p), "x of " + x + "," + y + "," + z);
                    assertEquals(y, BlockBatch.unpackY(p), "y of " + x + "," + y + "," + z);
                    assertEquals(z, BlockBatch.unpackZ(p), "z of " + x + "," + y + "," + z);
                }
            }
        }
    }
}
