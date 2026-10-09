package net.decimation.worldgen.terrain;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.DecimationWorldGen;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

/**
 * The biomes of the Decimation world type (docs/terrain.md).
 *
 * Names matter: Decimation's AmbientMusicPlayer picks the ambient track by
 * keywords in the biome name ("forest", "river", "plains", "hills",
 * "decimated", "irrated"); a name without one asks for a sound that does not
 * exist. Ids are fixed (saved in every chunk), 110..118.
 *
 * Must be created in preInit: Decimation adds its NPC / infected spawns in
 * its own preInit (before ours, so ours miss them) and strips vanilla
 * monsters from every biome in init (after ours, so ours are included).
 * {@link #copySpawns} fills the gap in postInit.
 */
public final class DecimationBiomes
{
    public static DeciBiome urban, suburb, military, wasteland, burntForest, plains, forest, hills, river;
    private static final List<DeciBiome> ALL = new ArrayList<DeciBiome>();

    // dead palette: faded olive grass, grey haze sky, murky water
    private static final int DEAD_SKY = 0x9EA3A6, DEAD_WATER = 0x8A9474;

    private DecimationBiomes()
    {
    }

    public static void create()
    {
        urban = add(new DeciBiome(110, "Decimated City").height(0.1F, 0.01F)
            .colours(0x8C8A6B, 0x7D7A55, DEAD_SKY, DEAD_WATER).decor(0, 2, 1, 0).trees(1F, false));
        suburb = add(new DeciBiome(111, "Decimated Suburbs").height(0.125F, 0.04F)
            .colours(0x9A9862, 0x86844F, 0xA9B4C0, DEAD_WATER).decor(1, 6, 1, 1).trees(0.5F, false));
        military = add(new DeciBiome(112, "Irradiated Military Zone").height(0.1F, 0.02F)
            .colours(0x8E8460, 0x7C7450, DEAD_SKY, DEAD_WATER).decor(0, 3, 2, 0).trees(1F, false)
            .patches(0.25F));
        wasteland = add(new DeciBiome(113, "Decimated Plains").height(0.125F, 0.06F)
            .colours(0xA39B63, 0x8F8650, 0xA4ABB0, DEAD_WATER).decor(0, 5, 3, 0).trees(0.9F, false));
        burntForest = add(new DeciBiome(114, "Burnt Forest").height(0.15F, 0.12F)
            .colours(0x7E7556, 0x6B6345, 0xA0A4A6, DEAD_WATER).decor(4, 2, 2, 0).trees(1F, false));
        plains = add(new DeciBiome(115, "Overgrown Plains").height(0.125F, 0.05F)
            .colours(0x7DB24A, 0x5E9C35, -1, -1).decor(1, 15, 0, 4).trees(0F, true));
        forest = add(new DeciBiome(116, "Overgrown Forest").height(0.1F, 0.2F)
            .colours(0x6AA33E, 0x4F8F2C, -1, -1).decor(10, 4, 0, 2).trees(0.05F, true));
        hills = add(new DeciBiome(117, "Overgrown Hills").height(0.45F, 0.3F)
            .colours(0x77AC48, 0x5A9634, -1, -1).decor(3, 8, 0, 2).trees(0.05F, true));
        river = add(new DeciBiome(118, "Murky River").height(-0.5F, 0.0F)
            .colours(-1, -1, -1, 0x9FB08A).decor(0, 3, 0, 0).trees(0F, false));

        dictionary(urban, Type.PLAINS, Type.DEAD, Type.WASTELAND, Type.SPARSE);
        dictionary(suburb, Type.PLAINS, Type.SPARSE);
        dictionary(military, Type.PLAINS, Type.DEAD, Type.WASTELAND, Type.SPARSE);
        dictionary(wasteland, Type.PLAINS, Type.DEAD, Type.WASTELAND, Type.DRY, Type.SPARSE);
        dictionary(burntForest, Type.FOREST, Type.DEAD, Type.SPOOKY);
        dictionary(plains, Type.PLAINS, Type.LUSH);
        dictionary(forest, Type.FOREST, Type.LUSH, Type.DENSE);
        dictionary(hills, Type.HILLS, Type.FOREST, Type.LUSH);
        dictionary(river, Type.RIVER, Type.WET);

        // world spawn search
        WorldChunkManager.allowedBiomes.add(suburb);
        WorldChunkManager.allowedBiomes.add(wasteland);
        WorldChunkManager.allowedBiomes.add(plains);
        WorldChunkManager.allowedBiomes.add(forest);
    }

    private static DeciBiome add(DeciBiome b)
    {
        BiomeGenBase old = BiomeGenBase.getBiomeGenArray()[b.biomeID];
        if (old != null && old != b)
        {
            FMLLog.warning("[%s] biome id %d already used by '%s', overwritten by '%s'",
                           DecimationWorldGen.MODID, b.biomeID, old.biomeName, b.biomeName);
        }
        ALL.add(b);
        return b;
    }

    private static void dictionary(BiomeGenBase b, Type... types)
    {
        BiomeDictionary.registerBiomeType(b, types);
    }

    /**
     * Gives our biomes the spawn lists vanilla plains has by now: Decimation's
     * infected, bandits, soldiers... (added in its preInit, vanilla monsters
     * already removed in its init) plus animals, except in the city and the
     * military zone.
     */
    public static void copySpawns()
    {
        for (DeciBiome b : ALL)
        {
            for (EnumCreatureType type : EnumCreatureType.values())
            {
                List<BiomeGenBase.SpawnListEntry> to = b.getSpawnableList(type);
                List<BiomeGenBase.SpawnListEntry> from = BiomeGenBase.plains.getSpawnableList(type);
                if (to == null || from == null)
                {
                    continue;
                }
                to.clear();
                for (BiomeGenBase.SpawnListEntry e : from)
                {
                    // vanilla animals (by package: obfuscated in the shipped game, so also by class)
                    boolean animal = e.entityClass.getName().startsWith("net.minecraft.entity.passive.")
                        || net.minecraft.entity.passive.EntityAnimal.class.isAssignableFrom(e.entityClass)
                        && e.entityClass.getName().indexOf('.') < 0;
                    if (animal && (b == urban || b == military))
                    {
                        continue;
                    }
                    to.add(new BiomeGenBase.SpawnListEntry(e.entityClass, e.itemWeight, e.minGroupCount,
                                                           e.maxGroupCount));
                }
            }
        }
        FMLLog.info("[%s] Decimation world type: %d biomes, spawns copied from plains (%d monster entries)",
                    DecimationWorldGen.MODID, ALL.size(),
                    BiomeGenBase.plains.getSpawnableList(EnumCreatureType.monster).size());
    }
}
