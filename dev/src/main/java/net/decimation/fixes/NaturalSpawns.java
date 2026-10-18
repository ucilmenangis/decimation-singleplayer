package net.decimation.fixes;

import net.minecraft.world.World;

/**
 * Minecraft's game rule doMobSpawning (`/gamerule doMobSpawning false`) for OUR automatic spawns too: vanilla stops the
 * biome spawns with it (Decimation's infected and humans come from the biome lists), and the enemy military spawner,
 * garrison posts and garrison spawner cages follow it. Spawns from code, eggs and /summon still work (user, 18 Oktober
 * 2026: tests without NPCs or zombies walking in).
 */
public final class NaturalSpawns
{
    public static final String RULE = "doMobSpawning";

    private NaturalSpawns()
    {
    }

    /** True while the world spawns on its own. */
    public static boolean on(World w)
    {
        return w == null || w.getGameRules().getGameRuleBooleanValue(RULE);
    }
}
