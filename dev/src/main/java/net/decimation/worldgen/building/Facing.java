package net.decimation.worldgen.building;

/**
 * Facing metadata for a direction given in plan coordinates (dfx, dz).
 * Plans are front normalised: in a building facing east fx runs against
 * world x, so dfx is flipped first.
 */
final class Facing
{
    private Facing()
    {
    }

    /** World x component of a plan direction. */
    static int worldDx(int front, int dfx)
    {
        return front == Building.FRONT_WEST ? dfx : -dfx;
    }

    /**
     * Decimation BlockProp (PropRenderer rotation = meta % 4 * 90): its FRONT
     * points east at 2, south 3, west 4, north 5 (prop gallery: chairs, TVs,
     * vending machines face south at 3). Before 0.18 this table was
     * inverted: chairs faced away from tables.
     */
    static int prop(int front, int dfx, int dz)
    {
        int dx = worldDx(front, dfx);
        if (dx < 0) return 4;
        if (dx > 0) return 2;
        return dz < 0 ? 5 : 3;
    }

    /** Vanilla facing metadata (chest, furnace): front toward (dfx, dz); 2 N, 3 S, 4 W, 5 E. */
    static int vanilla(int front, int dfx, int dz)
    {
        int dx = worldDx(front, dfx);
        if (dx < 0) return 4;
        if (dx > 0) return 5;
        return dz < 0 ? 2 : 3;
    }

    /** Stairs used as a seat (sofa, toilet): the sitter faces (dfx, dz), the high side is behind. */
    static int seat(int front, int dfx, int dz)
    {
        int dx = worldDx(front, dfx);
        if (dx < 0) return 0; // high side east, faces west
        if (dx > 0) return 1;
        return dz < 0 ? 2 : 3;
    }

    /** Vanilla bed direction foot -> head: 0 south, 1 west, 2 north, 3 east. */
    static int bed(int front, int dfx, int dz)
    {
        int dx = worldDx(front, dfx);
        if (dx < 0) return 1;
        if (dx > 0) return 3;
        return dz < 0 ? 2 : 0;
    }
}
