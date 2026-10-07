package net.decimation.worldgen.terrain;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/**
 * Replaces vanilla's whole biome layer stack for the Decimation world type.
 * scale 4 = the 1:4 layer the terrain generator samples (cell centre), scale
 * 1 = the per block layer. Both read the same {@link BiomeMap}, so they agree.
 */
public class DeciGenLayer extends GenLayer
{
    private final BiomeMap map;
    private final int scale;

    public DeciGenLayer(long seed, BiomeMap map, int scale)
    {
        super(seed);
        this.map = map;
        this.scale = scale;
    }

    @Override
    public int[] getInts(int areaX, int areaZ, int width, int height)
    {
        int[] out = IntCache.getIntCache(width * height);
        int half = scale / 2;
        for (int k = 0; k < height; k++)
        {
            for (int i = 0; i < width; i++)
            {
                out[i + k * width] = map.biomeAt((areaX + i) * scale + half, (areaZ + k) * scale + half).biomeID;
            }
        }
        return out;
    }
}
