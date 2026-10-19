package net.decimation.worldgen;

/** Map keys for pairs of ints (columns, regions, cells). */
public final class Keys
{
    private Keys()
    {
    }

    /**
     * a and b packed into one long and spread by an odd multiplier (one key per pair still). A Long hashes to its
     * high ^ low half: a and b packed alone hash to a ^ b, which puts a window of columns or a block of regions in a
     * few buckets, and the map searches trees (Highways' link cache cost 8 % of chunk generation; docs/performance.md 9).
     */
    public static long pair(int a, int b)
    {
        return (((long) a << 32) ^ (b & 0xFFFFFFFFL)) * 0x9E3779B97F4A7C15L;
    }
}
