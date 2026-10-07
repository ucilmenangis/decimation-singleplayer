package net.decimation.worldgen.building;

import net.decimation.worldgen.Graded;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * A procedurally generated ruined building on one city lot.
 *
 * Fully determined by its plan (position, size, floors, kind, front, seed,
 * biome style): {@link #blockAt} answers "what is at this local position",
 * so a building spanning several chunks is written slice by slice and
 * still comes out whole. Design rules: docs/building_design.md,
 * docs/interior_spec.md; code structure: docs/worldgen_architecture.md.
 *
 * Made of parts (this package): Shell (facade, outer walls, stairs, roof,
 * vines), StoreyPlan per storey type filled by a planner (Apartment /
 * Office / ShopPlanner) and the Furnisher (furniture sets), Surfaces
 * (panels, floors, ceilings, doors), Interior (plan cells to blocks),
 * Ruins (decay, collapse, debris, overgrowth), Yard (lot grading).
 *
 * Coordinates: the plan's bounds include a 1 block margin ring outside the
 * walls (exterior vines). Inside, everything is computed in "front
 * normalised" coordinates: fx = 0 is the front wall (street side), fx = W-1
 * the back wall, fz across the front. A building facing east is mirrored.
 */
public class Building implements Graded
{
    public static final int APARTMENT = 0, OFFICE = 1, SHOP = 2;
    public static final String[] KIND_NAME = {"apartment", "office", "shop"};
    public static final int FRONT_WEST = 0, FRONT_EAST = 1;
    /** Overgrowth styles, from the biome at the building's centre. */
    public static final int TEMPERATE = 0, LUSH = 1, COLD = 2, DRY = 3;
    public static final String[] STYLE_NAME = {"temperate", "lush", "cold", "dry"};
    /** Smallest setback that gets a front yard (room for a nose-in car). */
    public static final int MIN_YARD = 6;

    public final String id;
    public final int minX, minZ, width, length, floors, kind, front, style;
    /**
     * Storey height: floor layer (within 0), air (1 .. H-2), the storey's own
     * ceiling layer (within H-1). 5 since 0.19 (with 4 the floor block was
     * also the ceiling below). Per building since 0.23: public buildings
     * will get 6 (user decision 2026-10-08), all 5 for now.
     */
    public final int storeyHeight;
    public final long seed;
    /** The lot this building stands on; the ground around it is graded. */
    public final int lotX, lotZ, lotSize;

    final Props props;
    /** z of the front entrance (and the front path): the corridor's row. */
    final int entranceZ;
    /** Apartments: corridor down the middle with flats on both sides (deep buildings only). */
    final boolean doubleLoaded;
    /** Stair core origin (fx, fz) or -1 for a ladder shaft. */
    final int coreFx, coreFz;

    final Shell shell;
    final Ruins ruins;
    final Surfaces surfaces;
    final Furnisher furnisher;
    private final Interior interior;
    private final Yard yard;
    private final StoreyPlan groundPlan, upperPlan;

    /** Resolved Decimation props; arrays may be empty if missing from the jar. */
    public static class Props
    {
        public Block[] crates = new Block[0];
        public Block[] furniture = new Block[0];
        public Block table, chair, officeChair, metalTable, shelf, cabinet, cooking, washer,
            trashcan, vending, mailbox, cardboard, woodCrate, medicalCrate, policeCrate, ammoCrate;
        public Block road;
        public Block[] cars = new Block[0];
        /** Doors (DeciDoorBlock, vanilla door metadata): unit entrances per building, office, metal. */
        public Block[] unitDoors = new Block[0];
        public Block officeDoor, metalDoor;
        public Block[] trashBags = new Block[0];
        /** Any Decimation block by registry name without "deci:" (surfaces, lights...). */
        public final java.util.Map<String, Block> named = new java.util.HashMap<String, Block>();
    }

    public Building(String id, int minX, int minZ, int width, int length, int floors,
                    int kind, int front, int style, long seed, Props props,
                    int lotX, int lotZ, int lotSize)
    {
        this.lotX = lotX;
        this.lotZ = lotZ;
        this.lotSize = lotSize;
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.width = width;
        this.length = length;
        this.floors = floors;
        this.kind = kind;
        this.front = front;
        this.style = style;
        this.seed = seed;
        this.props = props;
        this.storeyHeight = 5;

        // ---- vertical circulation and corridor layout
        // apartments need the width for flats: the 7 long stair core left a
        // 12 wide block no room for a single flat (empty storeys, review
        // 0.21); narrow blocks climb by the corner ladder shaft instead
        boolean core = width >= (kind == APARTMENT ? 16 : 12) && length >= 10 && kind != SHOP;
        coreFx = core ? width - 8 : -1;
        // shallow apartment blocks get a corridor along one side (flats ~6
        // deep) instead of a middle corridor leaving 2 to 3 deep flats
        doubleLoaded = kind != APARTMENT || length >= 18;
        coreFz = !core ? -1 : doubleLoaded ? length / 2 - 2 : 2;
        entranceZ = doubleLoaded ? length / 2 : 2;

        ruins = new Ruins(this);
        shell = new Shell(this);
        surfaces = new Surfaces(this);
        furnisher = new Furnisher(this);
        interior = new Interior(this);
        yard = new Yard(this);

        // ---- storey plans
        groundPlan = new StoreyPlan(width, length, true, coreFx < 0);
        upperPlan = new StoreyPlan(width, length, false, coreFx < 0);
        switch (kind)
        {
            case OFFICE:
                OfficePlanner office = new OfficePlanner(this);
                office.plan(groundPlan);
                office.plan(upperPlan);
                break;
            case SHOP:
                ShopPlanner shop = new ShopPlanner(this);
                shop.plan(groundPlan);
                shop.plan(upperPlan);
                break;
            default:
                groundPlan.lining();
                upperPlan.lining();
                ApartmentPlanner apartment = new ApartmentPlanner(this);
                apartment.plan(groundPlan);
                apartment.plan(upperPlan);
        }
        // cells outside any room (around the stairs, ends of rows) take the main kind
        byte fallback = kind == OFFICE ? StoreyPlan.R_OFFICE : kind == SHOP ? StoreyPlan.R_SHOP : StoreyPlan.R_CORRIDOR;
        groundPlan.fillRooms(fallback);
        upperPlan.fillRooms(fallback);
    }

    /** The plan of a storey (0 = ground, every other storey shares the upper plan). */
    StoreyPlan plan(int storey)
    {
        return storey == 0 ? groundPlan : upperPlan;
    }

    /** Within index of a storey's own ceiling layer. */
    int ceil()
    {
        return storeyHeight - 1;
    }

    Block deci(String name)
    {
        return props.named.get(name);
    }

    // ------------------------------------------------------------ Plan

    public String id() { return id; }
    public int minX() { return minX - 1; }
    public int minZ() { return minZ - 1; }
    public int maxX() { return minX + width; }
    public int maxZ() { return minZ + length; }
    public int height() { return floors * storeyHeight + 2; }
    public int clearAbove() { return 5; }
    public int maxSpread() { return 12; } // a stone brick plinth reads fine on slopes
    public Block foundation() { return Blocks.stonebrick; } // reads as a basement plinth

    public int lotMinX() { return lotX; }
    public int lotMinZ() { return lotZ; }
    public int lotMaxX() { return lotX + lotSize - 1; }
    public int lotMaxZ() { return lotZ + lotSize - 1; }

    public void grade(World world, int x, int z, int baseY)
    {
        yard.grade(world, x, z, baseY);
    }

    public net.decimation.mod.server.zones.a zone()
    {
        return net.decimation.mod.server.zones.a.POLICE;
    }

    public String describe()
    {
        return "city " + KIND_NAME[kind] + " " + width + "x" + length + ", " + floors + " floor(s), "
            + STYLE_NAME[style] + ", footprint " + minX + "," + minZ;
    }

    public Block blockAt(int px, int ly, int pz, int[] meta)
    {
        meta[0] = 0;
        int x = px - 1, z = pz - 1; // footprint coordinates, -1 / W = margin ring
        if (x < 0 || z < 0 || x >= width || z >= length)
        {
            return shell.margin(x, ly, z, meta);
        }
        int fx = front == FRONT_WEST ? x : width - 1 - x;
        int top = floors * storeyHeight;
        // the way up (ladder shaft or stair core) survives a collapse, so
        // every storey stays reachable
        boolean ladderShaft = coreFx < 0 && z == length - 2 && fx >= width - 2;
        boolean stairCore = coreFx >= 0 && fx >= coreFx - 1 && fx <= coreFx + 7 && z >= coreFz - 1 && z <= coreFz + 4;
        if (!ladderShaft && !stairCore && ruins.collapsed(fx, ly, z))
        {
            return ruins.rubbleBelowCollapse(fx, ly, z, meta);
        }
        if (ly > top)
        {
            return shell.roof(fx, ly, z, meta, top);
        }
        int storey = ly / storeyHeight, within = ly % storeyHeight;
        boolean edge = fx == 0 || fx == width - 1 || z == 0 || z == length - 1;

        // stair core first: it cuts through floor layers
        if (coreFx >= 0 && fx >= coreFx && fx <= coreFx + 6 && z >= coreFz && z <= coreFz + 3)
        {
            Block b = shell.core(fx - coreFx, z - coreFz, storey, within, meta);
            if (b != null || meta[0] != Integer.MIN_VALUE)
            {
                if (meta[0] == Integer.MIN_VALUE)
                {
                    meta[0] = 0;
                }
                return b;
            }
            meta[0] = 0;
        }
        if (within == 0)
        {
            if (edge)
            {
                meta[0] = shell.trimMeta;
                return shell.trim;
            }
            if (coreFx < 0 && fx == width - 2 && z == length - 2 && storey > 0)
            {
                return shell.ladder(meta);
            }
            return surfaces.floor(meta, storey, fx, z);
        }
        if (edge)
        {
            if (coreFx < 0 && fx == width - 1 && z == length - 2)
            {
                // the ladder hangs on this wall cell: never a window or a
                // decay hole, or the ladder pops off at the first block update
                meta[0] = shell.wallMeta;
                return shell.wall;
            }
            return shell.outerWall(fx, ly, z, storey, within, meta);
        }
        if (coreFx < 0 && fx == width - 2 && z == length - 2)
        {
            return shell.ladder(meta);
        }
        return interior.cell(fx, ly, z, storey, within, meta);
    }

    // ------------------------------------------------------------ dev audit cameras

    /**
     * Dev audit: a standing spot inside a storey, {world x, world z, yaw}
     * looking from the front third towards the back wall. Null if the
     * storey plan has no open cell.
     */
    public int[] viewCell(int storey)
    {
        return viewCell(storey, kind == APARTMENT ? StoreyPlan.R_LIVING
            : kind == OFFICE ? StoreyPlan.R_OFFICE : StoreyPlan.R_SHOP);
    }

    /**
     * Dev audit: a camera spot in a room of type want: the open cell at a
     * room edge with the longest straight view across the room, {world x,
     * world z, yaw}; null if the storey has no such room.
     */
    public int[] lookCell(int storey, byte want)
    {
        StoreyPlan sp = plan(storey);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int best = 0, bx = -1, bz = -1, bd = 0;
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                if (sp.rooms[fx][z] != want || sp.cells[fx][z] != StoreyPlan.OPEN)
                {
                    continue;
                }
                for (int d = 0; d < 4; d++)
                {
                    int run = 0, x = fx, zz = z;
                    while (true)
                    {
                        x += dirs[d][0];
                        zz += dirs[d][1];
                        if (x <= 0 || zz <= 0 || x >= width - 1 || zz >= length - 1 || sp.rooms[x][zz] != want
                            || sp.cells[x][zz] == StoreyPlan.WALL || sp.cells[x][zz] == StoreyPlan.LINING)
                        {
                            break;
                        }
                        run++;
                    }
                    // stand at the room's edge: the cell behind must not be the same room
                    int back = sp.rooms[Math.max(0, Math.min(width - 1, fx - dirs[d][0]))]
                        [Math.max(0, Math.min(length - 1, z - dirs[d][1]))];
                    if (back != want && run > best)
                    {
                        best = run;
                        bx = fx;
                        bz = z;
                        bd = d;
                    }
                }
            }
        }
        if (bx < 0)
        {
            return null;
        }
        int dx = front == FRONT_WEST ? dirs[bd][0] : -dirs[bd][0], dz = dirs[bd][1];
        int yaw = dx > 0 ? 270 : dx < 0 ? 90 : dz > 0 ? 0 : 180;
        int x = front == FRONT_WEST ? minX + bx : minX + width - 1 - bx;
        return new int[] {x, minZ + bz, yaw};
    }

    public int[] viewCell(int storey, byte want)
    {
        StoreyPlan sp = plan(storey);
        int best = Integer.MAX_VALUE, bx = -1, bz = -1;
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                // inside a room of the building's main kind, as central as possible
                int d = Math.abs(fx - width / 4) * 2 + Math.abs(z - length / 2) + (sp.rooms[fx][z] == want ? 0 : 1000);
                if (sp.cells[fx][z] == StoreyPlan.OPEN && d < best)
                {
                    best = d;
                    bx = fx;
                    bz = z;
                }
            }
        }
        if (bx < 0)
        {
            return null;
        }
        int x = front == FRONT_WEST ? minX + bx : minX + width - 1 - bx;
        return new int[] {x, minZ + bz, front == FRONT_WEST ? 270 : 90};
    }

    // ------------------------------------------------------------ hash

    /** Deterministic pseudo random number in [0, 1) from position and seed. */
    double unit(int x, int y, int z)
    {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 31) * 0xBF58476D1CE4E5B9L;
        h ^= y * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 29) * 0xBF58476D1CE4E5B9L;
        h ^= z * 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }
}
