package net.decimation.worldgen;

/**
 * A {@link Plan} whose floor height is known in advance (Lost Cities style
 * city levels, see city.LcCity): Slices uses it instead of sampling the
 * terrain, and the plan is never cancelled for slope or water.
 */
public interface FixedBase extends Plan
{
    /** World y of the plan's local y 0. */
    int fixedBaseY();
}
