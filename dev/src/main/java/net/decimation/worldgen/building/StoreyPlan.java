package net.decimation.worldgen.building;

import net.minecraft.block.Block;

/**
 * The plan of one storey type (the ground storey, or every upper storey):
 * a cell grid (what stands in each column), a room grid beside it (which
 * room a cell belongs to: decides floors, lights, sets) and three
 * furniture layers (within 1, 2, 3). Indexed [fx][z] in front normalised
 * coordinates (fx = 0 is the front wall). Filled by a planner
 * (ApartmentPlanner, OfficePlanner, ShopPlanner) and the Furnisher, read
 * by Interior when blocks are written.
 */
public final class StoreyPlan
{
    // cell codes
    public static final byte OPEN = 0, WALL = 1, DOOR = 2, GLASS = 3, FURN = 4, CORE = 5;
    /** Plaster lining on the inner face of an outer wall (outer walls are 2 thick where used). */
    public static final byte LINING = 6;
    // room codes: decide floors, lights, room programs
    public static final byte R_NONE = 0, R_CORRIDOR = 1, R_LIVING = 2, R_BEDROOM = 3, R_LOBBY = 4, R_OFFICE = 5,
        R_MEETING = 6, R_STORAGE = 7, R_SHOP = 8, R_STOCK = 9, R_KITCHEN = 10, R_BATH = 11;

    public final boolean ground;
    final int width, length;
    /** No stair core: a ladder shaft in the back corner; nothing may stand next to it. */
    private final boolean ladderShaft;

    final byte[][] cells, rooms;
    /** Furniture at within 1 (same cells as FURN), within 2 (2 high pieces) and within 3 (wall cabinets). */
    final Block[][] furn, furn2, furn3;
    final byte[][] furnMeta, furn2Meta, furn3Meta;

    StoreyPlan(int width, int length, boolean ground, boolean ladderShaft)
    {
        this.width = width;
        this.length = length;
        this.ground = ground;
        this.ladderShaft = ladderShaft;
        cells = new byte[width][length];
        rooms = new byte[width][length];
        furn = new Block[width][length];
        furn2 = new Block[width][length];
        furn3 = new Block[width][length];
        furnMeta = new byte[width][length];
        furn2Meta = new byte[width][length];
        furn3Meta = new byte[width][length];
    }

    /** One piece of furniture on an open inner cell (within 1). */
    void put(int fx, int z, Block b, int meta)
    {
        if (b == null || fx <= 0 || z <= 0 || fx >= width - 1 || z >= length - 1 || cells[fx][z] != OPEN)
        {
            return;
        }
        if (ladderShaft && Math.abs(fx - (width - 2)) + Math.abs(z - (length - 2)) <= 1)
        {
            // nothing next to the ladder: a multiblock completed there sends
            // block updates, and if the wall behind the ladder belongs to a
            // window not written yet, the unsupported ladder pops off
            return;
        }
        cells[fx][z] = FURN;
        furn[fx][z] = b;
        furnMeta[fx][z] = (byte) meta;
    }

    /** A 2 high piece: base in the FURN layer, top in the within 2 layer. */
    void put2(int fx, int z, Block b, int meta)
    {
        if (b == null || fx <= 0 || z <= 0 || fx >= width - 1 || z >= length - 1 || cells[fx][z] != OPEN)
        {
            return;
        }
        put(fx, z, b, meta);
        if (cells[fx][z] == FURN && furn[fx][z] == b)
        {
            furn2[fx][z] = b;
            furn2Meta[fx][z] = (byte) meta;
        }
    }

    /** Sets every OPEN inner cell of the rectangle to type. */
    void wallLine(int fx0, int z0, int fx1, int z1, byte type)
    {
        for (int fx = Math.max(1, fx0); fx <= Math.min(width - 2, fx1); fx++)
        {
            for (int z = Math.max(1, z0); z <= Math.min(length - 2, z1); z++)
            {
                if (cells[fx][z] == OPEN)
                {
                    cells[fx][z] = type;
                }
            }
        }
    }

    void markRoom(int fx0, int z0, int fx1, int z1, byte room)
    {
        for (int fx = Math.max(0, fx0); fx <= Math.min(width - 1, fx1); fx++)
        {
            for (int z = Math.max(0, z0); z <= Math.min(length - 1, z1); z++)
            {
                rooms[fx][z] = room;
            }
        }
    }

    /** Marks the ring just inside the outer walls as plaster lining (2 thick outer walls). */
    void lining()
    {
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                if (fx == 1 || z == 1 || fx == width - 2 || z == length - 2)
                {
                    cells[fx][z] = LINING;
                }
            }
        }
    }

    /** Stair core cells (len along fx x 4) with side walls, open toward the front landing. */
    void markCore(int coreFx, int coreFz, int len)
    {
        if (coreFx < 0)
        {
            return;
        }
        int last = coreFx + len - 1;
        for (int fx = coreFx; fx <= last && fx < width - 1; fx++)
        {
            for (int z = coreFz; z <= coreFz + 3; z++)
            {
                cells[fx][z] = CORE;
            }
        }
        markRoom(coreFx - 1, coreFz - 1, last, coreFz + 4, R_CORRIDOR);
        wallLine(coreFx + 1, coreFz - 1, last, coreFz - 1, WALL);
        wallLine(coreFx + 1, coreFz + 4, last, coreFz + 4, WALL);
    }

    /** Cells no planner gave a room get the building's main room kind. */
    void fillRooms(byte fallback)
    {
        for (int fx = 0; fx < width; fx++)
        {
            for (int z = 0; z < length; z++)
            {
                if (rooms[fx][z] == R_NONE)
                {
                    rooms[fx][z] = fallback;
                }
            }
        }
    }

    /** True next to a wall (or the outer ring): debris and cobwebs gather there. */
    boolean nearWall(int fx, int z)
    {
        return fx <= 1 || z <= 1 || fx >= width - 2 || z >= length - 2
            || cells[fx - 1][z] == WALL || cells[fx + 1][z] == WALL || cells[fx][z - 1] == WALL
            || cells[fx][z + 1] == WALL;
    }

    /** True for the cells in front of / behind a door: no debris there. */
    boolean byDoor(int fx, int z)
    {
        return (fx > 0 && cells[fx - 1][z] == DOOR) || (fx < width - 1 && cells[fx + 1][z] == DOOR)
            || (z > 0 && cells[fx][z - 1] == DOOR) || (z < length - 1 && cells[fx][z + 1] == DOOR);
    }
}
