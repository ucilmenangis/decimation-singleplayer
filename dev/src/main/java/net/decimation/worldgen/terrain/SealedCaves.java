package net.decimation.worldgen.terrain;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;

/**
 * Vanilla caves and ravines, except that under the Decimated City and the
 * Irradiated Military Zone nothing is dug above {@link #CEILING}: a ravine
 * across a flat city cut 40 block trenches, and the street painter laid
 * sidewalks at their bottom. Other biomes and world types are untouched.
 */
public final class SealedCaves
{
    static final int CEILING = 50;

    private SealedCaves()
    {
    }

    static boolean sealed(BiomeGenBase biome, int y)
    {
        return y >= CEILING && (biome == DecimationBiomes.urban || biome == DecimationBiomes.military);
    }

    public static class Caves extends MapGenCaves
    {
        @Override
        protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop)
        {
            if (!sealed(worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16), y))
            {
                super.digBlock(data, index, x, y, z, chunkX, chunkZ, foundTop);
            }
        }
    }

    public static class Ravines extends MapGenRavine
    {
        @Override
        protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop)
        {
            if (!sealed(worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16), y))
            {
                super.digBlock(data, index, x, y, z, chunkX, chunkZ, foundTop);
            }
        }
    }
}
