package net.decimation.worldgen.terrain;

import net.minecraft.world.WorldType;

/**
 * "Decimation" on the create world screen (More World Options, World Type),
 * level-type=decimation in server.properties. Vanilla terrain generator
 * (caves, ores, ravines stay) driven by our own biome map; see
 * {@link TerrainEvents} for the biome layer swap and docs/terrain.md.
 */
public class DecimationWorldType extends WorldType
{
    public static DecimationWorldType INSTANCE;

    public DecimationWorldType()
    {
        super("decimation");
    }

    @Override
    public String getTranslateName()
    {
        return "Decimation"; // shown as is when no lang entry exists
    }
}
