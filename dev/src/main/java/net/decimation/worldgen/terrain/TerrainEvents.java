package net.decimation.worldgen.terrain;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.WorldTypeEvent;

/** TERRAIN_GEN_BUS hooks for the Decimation world type. */
public class TerrainEvents
{
    /** Swap in our biome layers (fired from the WorldChunkManager constructor). */
    @SubscribeEvent
    public void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event)
    {
        if (event.worldType != DecimationWorldType.INSTANCE)
        {
            return;
        }
        BiomeMap map = new BiomeMap(event.seed);
        event.newBiomeGens = new GenLayer[] {new DeciGenLayer(event.seed, map, 4),
                                             new DeciGenLayer(event.seed, map, 1)};
    }

    /**
     * Caves and ravines that stay below y 50 under cities and military zones.
     * Only replaces the exact vanilla generators (another mod's stays).
     */
    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event)
    {
        if (event.type == InitMapGenEvent.EventType.CAVE
            && event.originalGen.getClass() == net.minecraft.world.gen.MapGenCaves.class)
        {
            event.newGen = new SealedCaves.Caves();
        }
        else if (event.type == InitMapGenEvent.EventType.RAVINE
                 && event.originalGen.getClass() == net.minecraft.world.gen.MapGenRavine.class)
        {
            event.newGen = new SealedCaves.Ravines();
        }
    }

    /**
     * Lakes: none in the city or the military zone (buildings cancel on
     * water), one in four of vanilla's in the other dead biomes (dry land),
     * vanilla's rate in the overgrown ones. No surface lava pools anywhere.
     */
    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event)
    {
        if (event.type != PopulateChunkEvent.Populate.EventType.LAKE
            && event.type != PopulateChunkEvent.Populate.EventType.LAVA)
        {
            return;
        }
        BiomeGenBase b = event.world.getBiomeGenForCoords(event.chunkX * 16 + 16, event.chunkZ * 16 + 16);
        if (!(b instanceof DeciBiome))
        {
            return;
        }
        boolean dead = b == DecimationBiomes.urban || b == DecimationBiomes.military
            || b == DecimationBiomes.suburb || b == DecimationBiomes.wasteland || b == DecimationBiomes.burntForest;
        boolean city = b == DecimationBiomes.urban || b == DecimationBiomes.military;
        if (event.type == PopulateChunkEvent.Populate.EventType.LAVA || city
            || dead && ((event.chunkX * 31 + event.chunkZ * 17) & 3) != 0)
        {
            event.setResult(Event.Result.DENY);
        }
    }
}
