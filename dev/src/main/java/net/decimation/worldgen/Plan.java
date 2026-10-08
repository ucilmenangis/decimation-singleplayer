package net.decimation.worldgen;

import net.minecraft.block.Block;

/**
 * A structure planned ahead from the world seed and written slice by slice
 * (see {@link Slices}): every chunk writes only the columns inside its own
 * population window. Implementations must be pure functions of their plan.
 */
public interface Plan
{
    /** Unique per world, used as the key for the stored floor height. */
    String id();

    int minX();

    int minZ();

    int maxX();

    int maxZ();

    /** Highest local y written (0 = floor level). */
    int height();

    /**
     * Block at a local position, metadata in meta[0]. Returns null for air;
     * null with meta[0] == SKIP means "leave the world untouched here".
     */
    Block blockAt(int lx, int ly, int lz, int[] meta);

    int SKIP = -1;

    /** Extra layers above height() to clear (hills, trees), 0 for none. */
    int clearAbove();

    /** Ground height spread the first slice tolerates before rejecting the site. */
    int maxSpread();

    /** Block that fills the gap between the floor and lower ground. */
    net.minecraft.block.Block foundation();

    /** Decimation zone to tag the site with, or null. */
    ZoneKind zone();

    /** Short description for the log. */
    String describe();
}
