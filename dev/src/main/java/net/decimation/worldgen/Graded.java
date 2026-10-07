package net.decimation.worldgen;

import net.minecraft.world.World;

/**
 * A {@link Plan} that also owns the ground around it (a city lot). Slices
 * calls {@link #grade} for every lot column of a population window before
 * writing the plan's own blocks there.
 */
public interface Graded extends Plan
{
    int lotMinX();

    int lotMinZ();

    int lotMaxX();

    int lotMaxZ();

    /** Shape the ground of one world column; must ignore the footprint. */
    void grade(World world, int x, int z, int baseY);
}
