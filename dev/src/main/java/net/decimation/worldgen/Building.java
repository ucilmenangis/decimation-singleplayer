package net.decimation.worldgen;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * A procedurally generated ruined building on one city lot (v2).
 *
 * Fully determined by its plan (position, size, floors, kind, front, seed,
 * biome style): {@link #blockAt} answers "what is at this local position"
 * from precomputed floor plans, so a building spanning several chunks is
 * written slice by slice and still comes out whole. Design rules and the
 * research behind them: docs/building_design.md.
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
    /**
     * Storey height: floor layer (within 0), 3 air (1..3), ceiling layer
     * (within 4). 5 since 0.19: with 4 the floor block was also the ceiling
     * of the storey below (user review 0.18: plank / checker ceilings).
     */
    public static final int FLOOR = 5;
    /** Within index of a storey's own ceiling layer. */
    private static final int CEIL = FLOOR - 1;
    public static final int FRONT_WEST = 0, FRONT_EAST = 1;
    /** Overgrowth styles, from the biome at the building's centre. */
    public static final int TEMPERATE = 0, LUSH = 1, COLD = 2, DRY = 3;
    public static final String[] STYLE_NAME = {"temperate", "lush", "cold", "dry"};
    /** Smallest setback that gets a front yard (room for a nose-in car). */
    public static final int MIN_YARD = 6;

    // floor plan cell codes
    private static final byte OPEN = 0, WALL = 1, DOOR = 2, GLASS = 3, FURN = 4, CORE = 5;
    /** Plaster lining on the inner face of an outer wall (outer walls are 2 thick where used). */
    private static final byte LINING = 6;
    // room codes (room grid beside each plan): decide floors, lights, later room programs
    static final byte R_NONE = 0, R_CORRIDOR = 1, R_LIVING = 2, R_BEDROOM = 3, R_LOBBY = 4, R_OFFICE = 5,
        R_MEETING = 6, R_STORAGE = 7, R_SHOP = 8, R_STOCK = 9, R_KITCHEN = 10, R_BATH = 11;

    public final String id;
    public final int minX, minZ, width, length, floors, kind, front, style;
    /** z of the front entrance (and the front path): the corridor's row. */
    private final int entranceZ;
    /** Apartments: corridor down the middle with flats on both sides (deep buildings only). */
    private final boolean doubleLoaded;
    public final long seed;
    private final Props props;
    /** Door used for this building's unit / room doors (null: none resolved). */
    private Block roomDoor;

    /** The lot this building stands on; the ground around it is graded. */
    public final int lotX, lotZ, lotSize;

    // palette
    private final Block wall, trim, ground;
    private final int wallMeta, trimMeta, groundMeta;
    private final Block window;
    private final int windowMeta;
    private final double decay;
    /** Collapsed corner (0..3) and how many top storeys it reaches, or -1. */
    private final int collapseCorner, collapseStoreys;

    // floor plans [fx][fz] for the ground storey and the upper storeys
    private final byte[][] groundPlan, upperPlan;
    private final Block[][] groundFurn, upperFurn;
    /** Furniture layer at within 2 (2 high pieces: wardrobes, bookcases), same cells as FURN. */
    private final Block[][] groundFurn2, upperFurn2;
    private final byte[][] groundFurn2Meta, upperFurn2Meta;
    /** Furniture layer at within 3, under the ceiling (wall cabinets). */
    private final Block[][] groundFurn3, upperFurn3;
    private final byte[][] groundFurn3Meta, upperFurn3Meta;
    private final byte[][] groundFurnMeta, upperFurnMeta;
    private final byte[][] groundRoom, upperRoom;
    // surfaces chosen per building (Decimation blocks, null = fall back to vanilla)
    private Block wallBottom, wallTop, corridorFloor, bedroomCarpet, meetingCarpet, lobbyFloor, shopFloor, stockFloor,
        kitchenFloor, bathFloor;
    /** Stair core origin (fx, fz) or -1 for a ladder shaft. */
    private final int coreFx, coreFz;

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

        // ---- palette: wall / trim / ground storey
        Block[] walls = {Blocks.brick_block, Blocks.stained_hardened_clay, Blocks.stained_hardened_clay,
            Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, Blocks.stonebrick, Blocks.quartz_block,
            Blocks.sandstone, Blocks.hardened_clay, Blocks.stained_hardened_clay, Blocks.stained_hardened_clay};
        int[] wallMetas = {0, 0, 8, 7, 1, 0, 0, 2, 0, 9, 12};
        Block[] trims = {Blocks.stonebrick, Blocks.stained_hardened_clay, Blocks.stonebrick,
            Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, Blocks.double_stone_slab, Blocks.quartz_block,
            Blocks.sandstone, Blocks.stonebrick, Blocks.stained_hardened_clay, Blocks.stonebrick};
        int[] trimMetas = {0, 8, 0, 15, 12, 0, 2, 1, 0, 0, 0};
        int p;
        double r = unit(7, 7, 7);
        if (style == DRY && r < 0.5)
        {
            p = r < 0.3 ? 7 : 8; // sandstone or plain clay in deserts
        }
        else if (kind == OFFICE)
        {
            p = new int[] {1, 2, 3, 6, 9, 5}[(int) (unit(8, 8, 8) * 6)];
        }
        else if (kind == SHOP)
        {
            p = new int[] {0, 4, 5, 8, 10, 2}[(int) (unit(8, 8, 8) * 6)];
        }
        else
        {
            p = new int[] {0, 0, 1, 2, 4, 5, 10, 8}[(int) (unit(8, 8, 8) * 8)];
        }
        wall = walls[p];
        wallMeta = wallMetas[p];
        trim = trims[p];
        trimMeta = trimMetas[p];
        boolean groundTrim = unit(9, 9, 9) < (kind == APARTMENT ? 0.4 : 0.7);
        ground = groundTrim ? trim : wall;
        groundMeta = groundTrim ? trimMeta : wallMeta;
        if (kind == OFFICE && unit(10, 10, 10) < 0.5)
        {
            window = Blocks.stained_glass_pane;
            windowMeta = unit(11, 11, 11) < 0.5 ? 7 : 3; // gray / light blue tint
        }
        else
        {
            window = Blocks.glass_pane;
            windowMeta = 0;
        }
        decay = 0.15 + unit(12, 12, 12) * 0.40;
        roomDoor = kind == OFFICE ? props.officeDoor
            : kind == SHOP ? props.metalDoor
            : props.unitDoors.length > 0 ? props.unitDoors[(int) (unit(16, 16, 16) * props.unitDoors.length)]
            : props.officeDoor;
        if (floors >= 2 && unit(13, 13, 13) < 0.45)
        {
            collapseCorner = (int) (unit(14, 14, 14) * 4);
            collapseStoreys = 1 + (int) (unit(15, 15, 15) * Math.min(3, floors - 1));
        }
        else
        {
            collapseCorner = -1;
            collapseStoreys = 0;
        }

        // ---- floor plans
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
        groundPlan = new byte[width][length];
        upperPlan = new byte[width][length];
        groundFurn = new Block[width][length];
        upperFurn = new Block[width][length];
        groundFurn2 = new Block[width][length];
        upperFurn2 = new Block[width][length];
        groundFurn2Meta = new byte[width][length];
        upperFurn2Meta = new byte[width][length];
        groundFurn3 = new Block[width][length];
        upperFurn3 = new Block[width][length];
        groundFurn3Meta = new byte[width][length];
        upperFurn3Meta = new byte[width][length];
        groundFurnMeta = new byte[width][length];
        upperFurnMeta = new byte[width][length];
        groundRoom = new byte[width][length];
        upperRoom = new byte[width][length];
        chooseSurfaces();
        switch (kind)
        {
            case OFFICE:
                planOffice(groundPlan, groundFurn, groundFurnMeta, true);
                planOffice(upperPlan, upperFurn, upperFurnMeta, false);
                break;
            case SHOP:
                planShop(groundPlan, groundFurn, groundFurnMeta, true);
                planShop(upperPlan, upperFurn, upperFurnMeta, false);
                break;
            default:
                lining(groundPlan);
                lining(upperPlan);
                planApartment(groundPlan, groundFurn, groundFurnMeta, true);
                planApartment(upperPlan, upperFurn, upperFurnMeta, false);
        }
        // apartment cells outside any flat (around the stairs, ends of rows) are hall
        byte fallback = kind == OFFICE ? R_OFFICE : kind == SHOP ? R_SHOP : R_CORRIDOR;
        for (byte[][] rooms : new byte[][][] {groundRoom, upperRoom})
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
    }

    // ------------------------------------------------------------ Plan

    public String id() { return id; }
    public int minX() { return minX - 1; }
    public int minZ() { return minZ - 1; }
    public int maxX() { return minX + width; }
    public int maxZ() { return minZ + length; }
    public int height() { return floors * FLOOR + 2; }
    public int clearAbove() { return 5; }
    public int maxSpread() { return 12; } // a stone brick plinth reads fine on slopes
    public Block foundation() { return Blocks.stonebrick; } // reads as a basement plinth

    public int lotMinX() { return lotX; }
    public int lotMinZ() { return lotZ; }
    public int lotMaxX() { return lotX + lotSize - 1; }
    public int lotMaxZ() { return lotZ + lotSize - 1; }

    /**
     * Grades one lot column outside the walls. The ground ramps from the
     * floor level next to the building to the natural height at the lot
     * edge (smoothstep), so a building on a slope sits in a levelled yard
     * instead of on a plinth or in a pit. The column keeps its own surface
     * block (grass, sand, snow...), so the yard matches the biome. In front
     * of offices and shops with a setback: an asphalt parking strip (where
     * the ground is near floor level) with a slab walkway to the door and
     * some nose-in wrecks; apartments get a gravel path to the door.
     */
    public void grade(World world, int x, int z, int baseY)
    {
        int bx1 = minX + width - 1, bz1 = minZ + length - 1;
        if (x < lotX || z < lotZ || x > lotMaxX() || z > lotMaxZ()
            || x >= minX && x <= bx1 && z >= minZ && z <= bz1)
        {
            return;
        }
        int natural = StructureGenerator.soilTop(world, x, z);
        if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z))
        {
            return;
        }
        // 0 on the margin ring, 0 at the lot edge
        int d = Math.max(Math.max(minX - x, x - bx1), Math.max(minZ - z, z - bz1)) - 1;
        int e = Math.min(Math.min(x - lotX, lotMaxX() - x), Math.min(z - lotZ, lotMaxZ() - z));
        double t = d + e == 0 ? 0 : (double) d / (d + e);
        t = t * t * (3 - 2 * t);
        int target = baseY + (int) Math.round((natural - baseY) * t);

        Block surface = world.getBlock(x, natural, z);
        int surfaceMeta = world.getBlockMetadata(x, natural, z);
        Block cap = world.getBlock(x, natural + 1, z);
        if (surface == Blocks.air || surface.getMaterial().isLiquid())
        {
            return;
        }
        // sand and gravel fall into any cave under the yard (onBlockAdded
        // schedules the fall even during generation): never use them as fill
        Block filler = surface == Blocks.grass || surface == Blocks.mycelium ? Blocks.dirt
            : surface instanceof net.minecraft.block.BlockFalling
                ? (surface == Blocks.sand ? Blocks.sandstone : Blocks.stone)
            : surface;
        int fillerMeta = filler == surface ? surfaceMeta : 0;

        // front yard: between the front wall and the street edge of the lot
        boolean frontYard = front == FRONT_WEST ? x < minX : x > bx1;
        int setback = front == FRONT_WEST ? minX - lotX : lotMaxX() - bx1;
        boolean path = frontYard && Math.abs(z - (minZ + entranceZ)) <= 1;
        // asphalt only where the ground is near the floor level: on a slope
        // the strip stays a grassy embankment instead of a tilted car park
        boolean parking = frontYard && setback >= MIN_YARD && kind != APARTMENT && props.road != null
            && Math.abs(natural - baseY) <= 2;
        boolean paved = path || parking;
        if (path)
        {
            surface = kind == APARTMENT ? Blocks.gravel : Blocks.double_stone_slab;
            surfaceMeta = 0;
        }
        else if (parking)
        {
            surface = props.road;
            surfaceMeta = 0;
        }

        for (int y = natural; y < target; y++)
        {
            world.setBlock(x, y, z, filler, fillerMeta, 2);
        }
        for (int y = target + 1; y <= natural; y++)
        {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
        }
        if (surface instanceof net.minecraft.block.BlockFalling
            && !world.getBlock(x, target - 1, z).getMaterial().isSolid())
        {
            surface = filler; // a gravel path or sand over a cut into a cave
            surfaceMeta = fillerMeta;
        }
        world.setBlock(x, target, z, surface, surfaceMeta, 2);
        // plants and trees left hanging over a cut, or on new paving
        if (target < natural || paved)
        {
            for (int y = Math.max(target, natural) + 1, n = 0; n < 12; y++, n++)
            {
                Block b = world.getBlock(x, y, z);
                if (b == Blocks.air || !StructureGenerator.clearable(b))
                {
                    break;
                }
                world.setBlock(x, y, z, Blocks.air, 0, 2);
            }
        }
        if (cap == Blocks.snow_layer && !paved)
        {
            world.setBlock(x, target + 1, z, Blocks.snow_layer, 0, 2);
        }

        // nose-in wrecks across the middle of the parking strip
        int stripMid = front == FRONT_WEST ? lotX + (setback - 1) / 2 : lotMaxX() - (setback - 1) / 2;
        if (parking && !path && x == stripMid && props.cars.length > 0
            && (z - lotZ) % 4 == 2 && Math.abs(z - (minZ + entranceZ)) >= 3
            && Math.abs(target - baseY) <= 1 && unit(x, 77, z) < 0.45)
        {
            Block car = props.cars[(int) (unit(x, 78, z) * props.cars.length)];
            // long axis along x at rotation 0 / 180: metadata 4 or 2
            world.setBlock(x, target + 1, z, car, unit(x, 79, z) < 0.5 ? 4 : 2, 2);
            net.decimation.fixes.MultiblockRepairHandler.repair(world.getTileEntity(x, target + 1, z));
        }
    }

    /**
     * Dev audit: a standing spot inside a storey, {world x, world z, yaw}
     * looking from the front third towards the back wall. Null if the
     * storey plan has no open cell.
     */
    int[] viewCell(int storey)
    {
        return viewCell(storey, kind == APARTMENT ? R_LIVING : kind == OFFICE ? R_OFFICE : R_SHOP);
    }

    /**
     * Dev audit: a camera spot in a room of type want: the open cell at a
     * room edge with the longest straight view across the room, {world x,
     * world z, yaw}; null if the storey has no such room.
     */
    int[] lookCell(int storey, byte want)
    {
        byte[][] plan = storey == 0 ? groundPlan : upperPlan;
        byte[][] rooms = storey == 0 ? groundRoom : upperRoom;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int best = 0, bx = -1, bz = -1, bd = 0;
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                if (rooms[fx][z] != want || plan[fx][z] != OPEN)
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
                        if (x <= 0 || zz <= 0 || x >= width - 1 || zz >= length - 1 || rooms[x][zz] != want
                            || plan[x][zz] == WALL || plan[x][zz] == LINING)
                        {
                            break;
                        }
                        run++;
                    }
                    // stand at the room's edge: the cell behind must not be the same room
                    int back = rooms[Math.max(0, Math.min(width - 1, fx - dirs[d][0]))][Math.max(0, Math.min(length - 1, z - dirs[d][1]))];
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

    int[] viewCell(int storey, byte want)
    {
        byte[][] plan = storey == 0 ? groundPlan : upperPlan;
        byte[][] rooms = storey == 0 ? groundRoom : upperRoom;
        int best = Integer.MAX_VALUE, bx = -1, bz = -1;
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                // inside a room of the building's main kind, as central as possible
                int d = Math.abs(fx - width / 4) * 2 + Math.abs(z - length / 2) + (rooms[fx][z] == want ? 0 : 1000);
                if (plan[fx][z] == OPEN && d < best)
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
            return margin(x, ly, z, meta);
        }
        int fx = front == FRONT_WEST ? x : width - 1 - x;
        int top = floors * FLOOR;
        // the way up (ladder shaft or stair core) survives a collapse, so
        // every storey stays reachable
        boolean ladderShaft = coreFx < 0 && z == length - 2 && fx >= width - 2;
        boolean stairCore = coreFx >= 0 && fx >= coreFx - 1 && fx <= coreFx + 7 && z >= coreFz - 1 && z <= coreFz + 4;
        if (!ladderShaft && !stairCore && collapsed(fx, ly, z))
        {
            return rubbleBelowCollapse(fx, ly, z, meta);
        }
        if (ly > top)
        {
            return roof(fx, ly, z, meta, top);
        }
        int storey = ly / FLOOR, within = ly % FLOOR;
        boolean edge = fx == 0 || fx == width - 1 || z == 0 || z == length - 1;

        // stair core first: it cuts through floor layers
        if (coreFx >= 0 && fx >= coreFx && fx <= coreFx + 6 && z >= coreFz && z <= coreFz + 3)
        {
            Block b = core(fx - coreFx, z - coreFz, storey, within, meta);
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
                meta[0] = trimMeta;
                return trim;
            }
            if (coreFx < 0 && fx == width - 2 && z == length - 2 && storey > 0)
            {
                return ladder(meta);
            }
            return floorBlock(meta, storey, fx, z);
        }
        if (edge)
        {
            if (coreFx < 0 && fx == width - 1 && z == length - 2)
            {
                // the ladder hangs on this wall cell: never a window or a
                // decay hole, or the ladder pops off at the first block update
                meta[0] = wallMeta;
                return wall;
            }
            return outerWall(fx, ly, z, storey, within, meta);
        }
        if (coreFx < 0 && fx == width - 2 && z == length - 2)
        {
            return ladder(meta);
        }
        byte[][] plan = storey == 0 ? groundPlan : upperPlan;
        if (within == CEIL)
        {
            return ceilingLayer(plan[fx][z], (storey == 0 ? groundRoom : upperRoom)[fx][z], fx, storey, z, meta);
        }
        switch (plan[fx][z])
        {
            case WALL:
                // decay takes out whole wall columns (a breach), never single
                // blocks: those left plaster lumps floating at mid height and
                // made flats see-through (critic review 0.20)
                if (within == 3 || unit(fx, storey * 7 + 11, z) > decay * 0.18)
                {
                    Block panel = within == 1 ? wallBottom : wallTop;
                    if (panel != null)
                    {
                        meta[0] = 0;
                        return panel;
                    }
                    meta[0] = kind == OFFICE ? 0 : 2;
                    return kind == OFFICE ? Blocks.stonebrick : Blocks.planks;
                }
                return null;
            case GLASS:
                if (within <= 2 && unit(fx, ly, z) > 0.5 + decay * 0.3)
                {
                    return Blocks.glass_pane;
                }
                return within == 3 ? lintel(meta) : null;
            case DOOR:
                if (within == 3)
                {
                    return lintel(meta);
                }
                return door(plan, fx, z, storey, within, meta);
            case LINING:
                if (liningOpen(fx, z, storey, within))
                {
                    return null; // window recess / entrance passage
                }
                meta[0] = 0;
                return within == 1 ? (wallBottom != null ? wallBottom : Blocks.planks)
                    : (wallTop != null ? wallTop : Blocks.planks);
            case FURN:
                if (within == 3)
                {
                    Block b3 = (storey == 0 ? groundFurn3 : upperFurn3)[fx][z];
                    if (b3 != null)
                    {
                        meta[0] = (storey == 0 ? groundFurn3Meta : upperFurn3Meta)[fx][z];
                        return b3;
                    }
                    return ceiling((storey == 0 ? groundRoom : upperRoom)[fx][z], fx, z, storey, meta);
                }
                if (within == 2)
                {
                    return furniture2(fx, z, storey, meta);
                }
                if (within != 1)
                {
                    return null;
                }
                return furniture(fx, z, storey, meta);
            default:
                if (within == 1)
                {
                    return debris(fx, ly, z, meta);
                }
                if (within == 3)
                {
                    return ceiling((storey == 0 ? groundRoom : upperRoom)[fx][z], fx, z, storey, meta);
                }
                return null;
        }
    }

    // ------------------------------------------------------------ parts

    private Block floorBlock(int[] meta, int storey, int fx, int z)
    {
        byte room = (storey == 0 ? groundRoom : upperRoom)[fx][z];
        Block b = null;
        switch (room)
        {
            case R_CORRIDOR: b = corridorFloor; break;
            case R_BEDROOM: b = null; meta[0] = 1; return Blocks.planks; // warm spruce (grey carpet read as concrete)
            case R_LOBBY: b = lobbyFloor; break;
            case R_OFFICE: b = deci("BlockFloorCarpet_" + (1 + (int) (unit(storey, 22, 22) * 4))); break;
            case R_MEETING: b = meetingCarpet; break;
            case R_STORAGE:
            case R_STOCK: b = stockFloor; break;
            case R_SHOP: b = shopFloor; break;
            // checker tiles would show as a checkerboard ceiling in the flat
            // below (audit 0.18): tiles on the ground storey, light stone above
            case R_KITCHEN: b = storey == 0 ? kitchenFloor : lobbyFloor; break;
            case R_BATH: b = bathFloor; break;
            default: break; // living rooms keep planks
        }
        if (b != null)
        {
            meta[0] = 0;
            return b;
        }
        return floorBlock(meta, storey);
    }

    private Block floorBlock(int[] meta, int storey)
    {
        if (kind == OFFICE || (kind == SHOP && storey == 0))
        {
            meta[0] = 0;
            return Blocks.double_stone_slab;
        }
        meta[0] = (int) (unit(1, storey, 1) * 3); // oak / spruce / birch
        return Blocks.planks;
    }

    private Block outerWall(int fx, int ly, int z, int storey, int within, int[] meta)
    {
        boolean corner = (fx == 0 || fx == width - 1) && (z == 0 || z == length - 1);
        if (corner)
        {
            meta[0] = trimMeta;
            return trim;
        }
        int along = (fx == 0 || fx == width - 1) ? z : fx;
        int span = (fx == 0 || fx == width - 1) ? length : width;
        boolean frontWall = fx == 0;
        if (frontWall && storey == 0 && within <= 2
            && Math.abs(along - entranceZ) <= (kind == SHOP ? 1 : kind == OFFICE ? 1 : 0))
        {
            if (kind == APARTMENT && within <= 2 && props.officeDoor != null && unit(3, storey, along) > decay)
            {
                // apartment entrance: a door in the 1 wide gap (wall along z)
                meta[0] = within == 1 ? (unit(4, 4, along) < 0.3 ? 4 : 0) : 8;
                return props.officeDoor;
            }
            return null; // entrance
        }
        if (isWindow(along, within, storey, frontWall))
        {
            if (unit(fx, ly, z) < 0.35 + decay * 0.6)
            {
                return null; // broken
            }
            if (kind == SHOP && storey == 0)
            {
                return Blocks.iron_bars;
            }
            meta[0] = windowMeta;
            return window;
        }
        double d = unit(fx, ly, z);
        double holes = storey == 0 ? decay * 0.08 : decay * 0.25 * (1 + storey / (double) floors);
        if (d < holes)
        {
            return null;
        }
        if (d < holes + mossChance())
        {
            meta[0] = 0;
            return style == DRY ? Blocks.cobblestone : Blocks.mossy_cobblestone;
        }
        if (d < holes + mossChance() + 0.06)
        {
            meta[0] = 2; // cracked stone brick
            return Blocks.stonebrick;
        }
        if (storey == 0)
        {
            meta[0] = groundMeta;
            return ground;
        }
        if (within == 0)
        {
            meta[0] = trimMeta;
            return trim;
        }
        meta[0] = wallMeta;
        return wall;
    }

    private double mossChance()
    {
        switch (style)
        {
            case LUSH: return 0.18;
            case TEMPERATE: return 0.08;
            case COLD: return 0.03;
            default: return 0.01;
        }
    }

    private boolean isWindow(int along, int within, int storey, boolean frontWall)
    {
        if (kind == SHOP && storey == 0)
        {
            return frontWall && within >= 1 && within <= 2 && along % 4 != 0;
        }
        if (kind == OFFICE)
        {
            return within >= 1 && within <= 2 && along % 3 != 0;
        }
        return within == 2 && along % 3 == 1 || (within == 1 && along % 3 == 1 && unit(along, storey, 3) < 0.3);
    }

    /**
     * Stair core: 7 long (a, along fx) x 4 wide (b). Landings at a = 0 and
     * a = 6. Storey s climbs on lane b 0..1 (even s, from a = 1 up to a = 5)
     * or b 2..3 (odd s, from a = 5 down to a = 1), FLOOR steps; the ceiling
     * layer is open above the run's steps 1+; step j sits at height
     * s*FLOOR + 1 + j, so the top step lies in the next floor layer and the
     * floor above the three lower steps is left open. Returns with
     * meta[0] == Integer.MIN_VALUE for "plain floor here".
     */
    private Block core(int a, int b, int storey, int within, int[] meta)
    {
        int ly = storey * FLOOR + within;
        if (within == 0)
        {
            int s = storey - 1; // the run arriving into this floor layer
            if (s >= 0 && s < floors && onLane(s, b))
            {
                int j = stepIndex(s, a);
                if (j == FLOOR - 1)
                {
                    meta[0] = stairMeta((s & 1) == 0);
                    return kind == OFFICE ? Blocks.stone_brick_stairs : Blocks.oak_stairs;
                }
                if (j >= 0)
                {
                    return null; // opening above the arriving run
                }
            }
            meta[0] = Integer.MIN_VALUE;
            return null;
        }
        if (storey < floors && onLane(storey, b))
        {
            int j = stepIndex(storey, a);
            if (j >= 0 && ly == storey * FLOOR + 1 + j)
            {
                meta[0] = stairMeta((storey & 1) == 0);
                return kind == OFFICE ? Blocks.stone_brick_stairs : Blocks.oak_stairs;
            }
            if (within == CEIL && j >= 1)
            {
                return null; // headroom over the climbing run
            }
        }
        if (within == CEIL)
        {
            meta[0] = 0;
            Block c = deci("BlockWallOffice_Top");
            return c != null ? c : Blocks.stone;
        }
        return null; // core interior stays open
    }

    private static boolean onLane(int storey, int b)
    {
        return ((storey & 1) == 0) == (b <= 1);
    }

    /** Step index 0..FLOOR-1 of storey's run at core position a, or -1. */
    private static int stepIndex(int storey, int a)
    {
        int j = (storey & 1) == 0 ? a - 1 : FLOOR - a;
        return j >= 0 && j <= FLOOR - 1 ? j : -1;
    }

    private int stairMeta(boolean even)
    {
        // even runs climb toward +fx, odd toward -fx; +fx is world +x when
        // the front faces west. Stair meta: 0 ascends east, 1 west.
        boolean east = even == (front == FRONT_WEST);
        return east ? 0 : 1;
    }

    private Block ladder(int[] meta)
    {
        // ladder at fx = W-2 against the back wall (fx = W-1)
        meta[0] = front == FRONT_WEST ? 4 : 5;
        return Blocks.ladder;
    }

    private Block roof(int fx, int ly, int z, int[] meta, int top)
    {
        boolean edge = fx == 0 || fx == width - 1 || z == 0 || z == length - 1;
        if (ly == top + 1)
        {
            if (edge)
            {
                if (unit(fx, ly, z) < 0.12 + decay * 0.3)
                {
                    return null;
                }
                meta[0] = trimMeta;
                return trim;
            }
            return overgrowthOnSurface(fx, ly, z, meta, 0.10);
        }
        return null;
    }

    /** Leaves, grass, snow or sand lying on a roof or floor. */
    private Block overgrowthOnSurface(int fx, int ly, int z, int[] meta, double base)
    {
        double d = unit(fx * 3, ly, z * 5);
        switch (style)
        {
            case COLD:
                if (d < base * 3)
                {
                    meta[0] = d < base ? 1 : 0;
                    return Blocks.snow_layer;
                }
                return null;
            case DRY:
                if (d < base * 1.5)
                {
                    return Blocks.sand;
                }
                return null;
            case LUSH:
                if (d < base * 2.5)
                {
                    meta[0] = 3 | 4; // jungle leaves, no decay
                    return Blocks.leaves;
                }
                return null;
            default:
                if (d < base)
                {
                    meta[0] = 4; // oak leaves, no decay
                    return Blocks.leaves;
                }
                if (d < base * 1.6)
                {
                    meta[0] = 1;
                    return Blocks.tallgrass;
                }
                return null;
        }
    }

    /** Margin ring: exterior vines (not in cold or dry biomes), else untouched. */
    private Block margin(int x, int ly, int z, int[] meta)
    {
        meta[0] = Plan.SKIP;
        if (style == COLD || style == DRY || ly < 1 || ly > floors * FLOOR)
        {
            return null;
        }
        boolean side = (x == -1 || x == width) && z >= 0 && z < length
            || (z == -1 || z == length) && x >= 0 && x < width;
        if (!side)
        {
            return null;
        }
        // vines hang in strands from a random height downward
        double strand = unit(x * 17, 99, z * 13);
        double density = style == LUSH ? 0.45 : 0.18;
        if (strand > density)
        {
            return null;
        }
        int from = (int) (unit(x, 98, z) * (floors * FLOOR)) + 2;
        if (ly > from || ly < from - 3 - (int) (unit(x, 97, z) * 10))
        {
            return null;
        }
        // vine meta: which side the supporting wall is on (1 south, 2 west, 4 north, 8 east)
        meta[0] = x == -1 ? 8 : x == width ? 2 : z == -1 ? 1 : 4;
        return Blocks.vine;
    }

    private boolean collapsed(int fx, int ly, int z)
    {
        if (collapseCorner < 0)
        {
            return false;
        }
        int fromStorey = floors - collapseStoreys;
        int storey = ly / FLOOR;
        if (ly <= fromStorey * FLOOR)
        {
            return false; // the floor of the first collapsed storey holds the rubble
        }
        int cx = (collapseCorner & 1) == 0 ? 0 : width - 1;
        int cz = (collapseCorner & 2) == 0 ? 0 : length - 1;
        // the hole widens upward: a cone from the corner
        double reach = (Math.min(width, length) / 2.0) * (0.5 + 0.5 * (storey - fromStorey + 1) / (double) collapseStoreys);
        double dist = Math.hypot(fx - cx, z - cz);
        return dist < reach + (unit(fx, storey, z) - 0.5) * 2;
    }

    private Block rubbleBelowCollapse(int fx, int ly, int z, int[] meta)
    {
        int fromStorey = floors - collapseStoreys;
        if (ly == fromStorey * FLOOR + 1 && unit(fx, ly, z) < 0.5)
        {
            return debrisBlock(fx, ly, z, meta);
        }
        return null;
    }

    /**
     * A door in a DOOR cell of the plan (both halves: within 1 lower, 2
     * upper). Decimation doors copy vanilla door metadata: lower 0 / 2 for
     * a door in a wall running north-south (plan z axis), 1 / 3 for a wall
     * running along x, +4 = open; upper 8. Missing with a chance growing
     * with decay; about a quarter of the rest stand open.
     */
    private Block door(byte[][] plan, int fx, int z, int storey, int within, int[] meta)
    {
        if (roomDoor == null || unit(fx, storey * 7 + 3, z) < 0.2 + decay * 0.5)
        {
            return null;
        }
        if (within == 2)
        {
            meta[0] = 8;
            return roomDoor;
        }
        boolean wallAlongZ = (z > 0 && plan[fx][z - 1] == WALL) || (z < length - 1 && plan[fx][z + 1] == WALL);
        int base = wallAlongZ ? 0 : 1;
        meta[0] = base | (unit(fx, storey * 7 + 5, z) < 0.25 ? 4 : 0);
        return roomDoor;
    }

    private boolean nearWall(byte[][] plan, int fx, int z)
    {
        return fx <= 1 || z <= 1 || fx >= width - 2 || z >= length - 2
            || plan[fx - 1][z] == WALL || plan[fx + 1][z] == WALL || plan[fx][z - 1] == WALL || plan[fx][z + 1] == WALL;
    }

    /** True for the cells in front of / behind a door: no debris there. */
    private boolean byDoor(byte[][] plan, int fx, int z)
    {
        return (fx > 0 && plan[fx - 1][z] == DOOR) || (fx < width - 1 && plan[fx + 1][z] == DOOR)
            || (z > 0 && plan[fx][z - 1] == DOOR) || (z < length - 1 && plan[fx][z + 1] == DOOR);
    }

    private Block debris(int fx, int ly, int z, int[] meta)
    {
        double d = unit(fx, ly, z);
        byte[][] plan = ly / FLOOR == 0 ? groundPlan : upperPlan;
        // debris gathers along walls and in corners, not in the middle of a room
        if (d < decay * 0.07 && nearWall(plan, fx, z) && !byDoor(plan, fx, z))
        {
            return lowDebris(fx, ly, z, meta);
        }
        boolean nearOutside = fx <= 1 || z <= 1 || fx >= width - 2 || z >= length - 2;
        if (nearOutside)
        {
            Block o = overgrowthOnSurface(fx, ly, z, meta, 0.06 + decay * 0.1);
            if (o != null && o != Blocks.leaves)
            {
                return o;
            }
            meta[0] = 0;
        }
        return null;
    }

    /**
     * Debris on a walkable floor: never a full block (the audit found full
     * mossy cobble cubes standing in corridors and doorways). Slabs of
     * cobble / stone brick / brick, a cobweb, a trash bag.
     */
    private Block lowDebris(int fx, int ly, int z, int[] meta)
    {
        double d = unit(z, ly, fx);
        byte[][] plan = ly / FLOOR == 0 ? groundPlan : upperPlan;
        if (d < 0.12 && nearWall(plan, fx, z))
        {
            meta[0] = 0;
            return Blocks.web; // cobwebs in corners and along walls, not mid-room
        }
        if (d < 0.22 && props.trashBags.length > 0)
        {
            meta[0] = 2 + (int) (unit(fx, 9, z) * 4);
            return props.trashBags[(int) (unit(z, 9, fx) * props.trashBags.length)];
        }
        meta[0] = d < 0.6 ? 3 : d < 0.85 ? 5 : 4; // cobble / stone brick / brick slab
        return Blocks.stone_slab;
    }

    /** Heavy rubble: only under a collapsed ceiling, where full blocks make sense. */
    private Block debrisBlock(int fx, int ly, int z, int[] meta)
    {
        double d = unit(z, ly, fx);
        if (d < 0.5)
        {
            meta[0] = 3; // cobblestone slab
            return Blocks.stone_slab;
        }
        meta[0] = 0;
        return d < 0.75 ? Blocks.cobblestone : style == DRY ? Blocks.sand : Blocks.mossy_cobblestone;
    }

    private Block furniture(int fx, int z, int storey, int[] meta)
    {
        Block[][] f = storey == 0 ? groundFurn : upperFurn;
        byte[][] m = storey == 0 ? groundFurnMeta : upperFurnMeta;
        Block b = f[fx][z];
        if (b == null)
        {
            return null;
        }
        // looted / thrown around: some furniture missing, loot varies per storey
        // (never half a bed or the base of a 2 high piece)
        if (b != Blocks.bed && f2(storey)[fx][z] == null && removed(fx, z, storey))
        {
            return unit(fx, storey * 33, z) < 0.5 ? lowDebris(fx, storey * FLOOR + 1, z, meta) : null;
        }
        if (isCrate(b) && unit(storey, fx, z) < 0.45)
        {
            return null;
        }
        meta[0] = m[fx][z];
        return b;
    }

    private boolean removed(int fx, int z, int storey)
    {
        return unit(fx, storey * 31, z) < 0.12 + decay * 0.25;
    }

    private Block[][] f2(int storey)
    {
        return storey == 0 ? groundFurn2 : upperFurn2;
    }

    private Block furniture2(int fx, int z, int storey, int[] meta)
    {
        Block b = f2(storey)[fx][z];
        if (b != null)
        {
            meta[0] = (storey == 0 ? groundFurn2Meta : upperFurn2Meta)[fx][z];
        }
        return b;
    }

    /** A 2 high piece: base in the FURN layer, top in the within 2 layer. */
    private void put2(byte[][] plan, Block[][] furn, byte[][] fm, int fx, int z, Block b, int meta)
    {
        if (b == null || fx <= 0 || z <= 0 || fx >= width - 1 || z >= length - 1 || plan[fx][z] != OPEN)
        {
            return;
        }
        put(plan, furn, fm, fx, z, b, meta);
        if (plan[fx][z] == FURN && furn[fx][z] == b)
        {
            boolean ground = plan == groundPlan;
            (ground ? groundFurn2 : upperFurn2)[fx][z] = b;
            (ground ? groundFurn2Meta : upperFurn2Meta)[fx][z] = (byte) meta;
        }
    }

    /** Vanilla facing metadata (chest, furnace): front toward (dfx, dz); 2 N, 3 S, 4 W, 5 E. */
    private int vanillaFacing(int dfx, int dz)
    {
        int dx = front == FRONT_WEST ? dfx : -dfx;
        if (dx < 0) return 4;
        if (dx > 0) return 5;
        return dz < 0 ? 2 : 3;
    }

    /** Stairs used as a seat (sofa, toilet): the sitter faces (dfx, dz), the high side is behind. */
    private int seatStairs(int dfx, int dz)
    {
        int dx = front == FRONT_WEST ? dfx : -dfx;
        if (dx < 0) return 0; // high side east, faces west
        if (dx > 0) return 1;
        return dz < 0 ? 2 : 3;
    }

    /** Vanilla bed direction foot -> head: 0 south, 1 west, 2 north, 3 east. */
    private int bedDir(int dfx, int dz)
    {
        int dx = front == FRONT_WEST ? dfx : -dfx;
        if (dx < 0) return 1;
        if (dx > 0) return 3;
        return dz < 0 ? 2 : 0;
    }

    private boolean isCrate(Block b)
    {
        return b == props.woodCrate || b == props.medicalCrate || b == props.policeCrate || b == props.ammoCrate;
    }

    // ------------------------------------------------------------ floor plans

    private void put(byte[][] plan, Block[][] furn, byte[][] fm, int fx, int z, Block b, int meta)
    {
        if (b == null || fx <= 0 || z <= 0 || fx >= width - 1 || z >= length - 1 || plan[fx][z] != OPEN)
        {
            return;
        }
        if (coreFx < 0 && Math.abs(fx - (width - 2)) + Math.abs(z - (length - 2)) <= 1)
        {
            // nothing next to the ladder: a multiblock completed there sends
            // block updates, and if the wall behind the ladder belongs to a
            // window not written yet, the unsupported ladder pops off
            return;
        }
        plan[fx][z] = FURN;
        furn[fx][z] = b;
        fm[fx][z] = (byte) meta;
    }

    /** Prop facing (PropRenderer rotation = meta % 4 * 90) toward a world direction. */
    private int propFacing(int dfx, int dz)
    {
        // normalise to world: mirrored buildings flip x
        // the FRONT of a BlockProp model points east at 2, south 3, west 4,
        // north 5 (prop gallery: chairs, TVs, vending machines face south at
        // 3). Before 0.18 this table was inverted: chairs faced away from tables.
        int dx = front == FRONT_WEST ? dfx : -dfx;
        if (dx < 0) return 4;
        if (dx > 0) return 2;
        return dz < 0 ? 5 : 3;
    }

    private void wallLine(byte[][] plan, int fx0, int z0, int fx1, int z1, byte type)
    {
        for (int fx = Math.max(1, fx0); fx <= Math.min(width - 2, fx1); fx++)
        {
            for (int z = Math.max(1, z0); z <= Math.min(length - 2, z1); z++)
            {
                if (plan[fx][z] == OPEN)
                {
                    plan[fx][z] = type;
                }
            }
        }
    }

    private byte[][] roomsOf(byte[][] plan)
    {
        return plan == groundPlan ? groundRoom : upperRoom;
    }

    private void markRoom(byte[][] plan, int fx0, int z0, int fx1, int z1, byte room)
    {
        byte[][] rooms = roomsOf(plan);
        for (int fx = Math.max(0, fx0); fx <= Math.min(width - 1, fx1); fx++)
        {
            for (int z = Math.max(0, z0); z <= Math.min(length - 1, z1); z++)
            {
                rooms[fx][z] = room;
            }
        }
    }

    /**
     * The storey's own ceiling (within CEIL): walls continue as their top
     * panel, rooms get plaster (homes), ceiling tiles (offices, shops,
     * kitchens, bathrooms) or bare concrete (storage). Decayed buildings
     * lose some ceiling cells, showing the slab above.
     */
    private Block ceilingLayer(byte cell, byte room, int fx, int storey, int z, int[] meta)
    {
        meta[0] = 0;
        if (cell == WALL || cell == DOOR || cell == GLASS || cell == LINING)
        {
            return wallTop != null ? wallTop : Blocks.planks;
        }
        if (unit(fx, storey * 17 + 3, z) < decay * 0.12)
        {
            return null; // fallen ceiling panel
        }
        // white plaster everywhere: the underside of a block is shaded dark,
        // so ceiling tiles (BlockCeiling_3/4) read as a dark lid (audit 0.19)
        Block b = room == R_STORAGE || room == R_STOCK ? deci("BlockStone_1") : deci("BlockWallOffice_Top");
        return b != null ? b : Blocks.quartz_block;
    }

    /** Above doors and glass partitions: the wall's own top panel. */
    private Block lintel(int[] meta)
    {
        meta[0] = 0;
        return wallTop != null ? wallTop : Blocks.stonebrick;
    }

    private Block deci(String name)
    {
        return props.named.get(name);
    }

    /**
     * Interior surfaces per building (docs/interior_spec.md section 2): one
     * wall panel colour set (Decimation BlockWallOffice_*: Bottom course
     * with a skirting stripe, Top above), floors per room type.
     */
    private void chooseSurfaces()
    {
        String[][] sets = kind == OFFICE
            ? new String[][] {{"BlockWallOffice_Bottom_3", "BlockWallOffice_Top"},
                              {"BlockWallOffice_4_Bottom_2", "BlockWallOffice_4_Top"},
                              {"BlockWallOffice_3_Bottom_2", "BlockWallOffice_3_Top"}}
            : kind == SHOP
            ? new String[][] {{"BlockWallOffice_Bottom_3", "BlockWallOffice_Top"},
                              {"BlockWallOffice_2_Bottom_1", "BlockWallOffice_2_Top"}}
            : new String[][] {{"BlockWallOffice_2_Bottom_1", "BlockWallOffice_2_Top"},
                              {"BlockWallOffice_3_Bottom_2", "BlockWallOffice_3_Top"},
                              {"BlockWallOffice_4_Bottom_1", "BlockWallOffice_4_Top"},
                              {"BlockWallOffice_Bottom_1", "BlockWallOffice_Top"},
                              {"BlockWallOffice_Bottom_2", "BlockWallOffice_Top"}};
        String[] set = sets[(int) (unit(17, 17, 17) * sets.length)];
        wallBottom = deci(set[0]);
        wallTop = deci(set[1]);
        // light floors only: a floor is also the ceiling of the storey below
        // (black tiles made corridors read as a dark tunnel, audit 0.17)
        corridorFloor = deci(unit(18, 18, 18) < 0.5 ? "BlockStone_7" : "BlockStone_6");
        bedroomCarpet = deci("BlockFloorCarpet_" + (2 + (int) (unit(19, 19, 19) * 5)));
        meetingCarpet = deci(unit(20, 20, 20) < 0.5 ? "BlockFloorCarpet_5" : "BlockFloorCarpet_6");
        lobbyFloor = deci("BlockStone_6");
        shopFloor = deci(unit(21, 21, 21) < 0.6 ? "BlockStone_6" : "BlockFloorTiles_2");
        stockFloor = deci("BlockStone_1");
        kitchenFloor = deci(unit(23, 23, 23) < 0.5 ? "BlockFloorTiles_1" : "BlockFloorTiles_2");
        bathFloor = deci(unit(24, 24, 24) < 0.5 ? "BlockFloorTiles_2" : "BlockFloorTiles_1");
    }

    /**
     * Ceiling fixtures at within 3 of an open cell: light panels (Decimation
     * BlockLight has its box at the top of the cell, so it hangs from the
     * floor above; BlockLight glows, BlockLightOff is dead), vents in
     * offices and shops. Most lights are dead, some missing with decay.
     */
    private Block ceiling(byte room, int fx, int z, int storey, int[] meta)
    {
        boolean grid;
        switch (room)
        {
            case R_CORRIDOR:
                grid = fx % 4 == 1;
                break;
            case R_OFFICE:
            case R_SHOP:
            case R_MEETING:
                if (fx % 6 == 5 && z % 6 == 5 && deci("BlockCeilingVent") != null)
                {
                    meta[0] = 2;
                    return deci("BlockCeilingVent");
                }
                grid = fx % 4 == 2 && z % 4 == 2;
                break;
            default:
                grid = fx % 5 == 2 && z % 5 == 2;
        }
        if (!grid || unit(fx, storey * 13 + 7, z) < 0.15 + decay * 0.3)
        {
            return null;
        }
        meta[0] = 2;
        return unit(fx, storey * 13 + 8, z) < 0.08 ? deci("BlockLight") : deci("BlockLightOff");
    }

    private void markCore(byte[][] plan)
    {
        if (coreFx < 0)
        {
            return;
        }
        for (int fx = coreFx; fx <= coreFx + 6 && fx < width - 1; fx++)
        {
            for (int z = coreFz; z <= coreFz + 3; z++)
            {
                plan[fx][z] = CORE;
            }
        }
        markRoom(plan, coreFx - 1, coreFz - 1, coreFx + 6, coreFz + 4, R_CORRIDOR);
        // side walls of the core, open toward the front landing
        wallLine(plan, coreFx + 1, coreFz - 1, coreFx + 6, coreFz - 1, WALL);
        wallLine(plan, coreFx + 1, coreFz + 4, coreFx + 6, coreFz + 4, WALL);
    }

    /** Marks the ring just inside the outer walls as plaster lining (2 thick outer walls). */
    private void lining(byte[][] plan)
    {
        for (int fx = 1; fx < width - 1; fx++)
        {
            for (int z = 1; z < length - 1; z++)
            {
                if (fx == 1 || z == 1 || fx == width - 2 || z == length - 2)
                {
                    plan[fx][z] = LINING;
                }
            }
        }
    }

    /**
     * A lining cell stays open where the outer wall next to it is open at
     * that height (window, broken window, entrance): a recess, not a wall.
     */
    private boolean liningOpen(int fx, int z, int storey, int within)
    {
        int[] tmp = new int[1];
        int ly = storey * FLOOR + within;
        int[][] around = {{fx - 1, z}, {fx + 1, z}, {fx, z - 1}, {fx, z + 1}};
        for (int[] n : around)
        {
            if (n[0] == 0 || n[1] == 0 || n[0] == width - 1 || n[1] == length - 1)
            {
                Block w = outerWall(n[0], ly, n[1], storey, within, tmp);
                if (w == null || w == Blocks.glass_pane || w == Blocks.stained_glass_pane)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Apartment storey: a corridor along fx through the middle, flats on one
     * or both sides. Interior starts at 2 (ring 1 is the wall lining).
     */
    private void planApartment(byte[][] plan, Block[][] furn, byte[][] fm, boolean ground)
    {
        markCore(plan);
        // corridor rows: middle (double loaded) or along the low z side
        int c0 = doubleLoaded ? length / 2 - 1 : 2, c1 = doubleLoaded ? length / 2 : 3;
        int end = coreFx >= 0 ? coreFx - 1 : width - 3;
        markRoom(plan, 1, c0, end, c1, R_CORRIDOR);
        if (doubleLoaded)
        {
            wallLine(plan, 2, c0 - 1, end, c0 - 1, WALL);
        }
        wallLine(plan, 2, c1 + 1, end, c1 + 1, WALL);
        int fx = 2;
        int unitNo = 0;
        while (fx < end - 2)
        {
            int len = Math.min(5 + (int) (unit(fx, unitNo, 41) * 4), end - fx);
            int ux0 = fx, ux1 = fx + len - 1;
            if (ux1 < end - 1)
            {
                wallLine(plan, ux1 + 1, 2, ux1 + 1, c0 - 2, WALL);
                wallLine(plan, ux1 + 1, c1 + 2, ux1 + 1, length - 3, WALL);
            }
            // entry door near a corner (as in real flats): leaves a long free
            // wall for the kitchen run; the side alternates per flat
            boolean entryLow = unit(ux0, unitNo, 42) < 0.5;
            int door = entryLow ? ux0 : ux1;
            if (doubleLoaded)
            {
                plan[door][c0 - 1] = DOOR;
                apartmentUnit(plan, furn, fm, ux0, ux1, 2, c0 - 2, false, ground && fx <= 3, door);
            }
            plan[door][c1 + 1] = DOOR;
            // lobby on one side of the entrance only, a furnished flat opposite
            apartmentUnit(plan, furn, fm, ux0, ux1, c1 + 2, length - 3, true, ground && fx <= 3 && !doubleLoaded, door);
            fx = ux1 + 2;
            unitNo++;
        }
    }

    /**
     * One flat, furnished with furniture SETS (docs/furniture_sets.md):
     * living part on the corridor side (kitchen set, living set, dining
     * set), bedroom on the window side (bed, wardrobe, desk), a bathroom in
     * wider flats. Rows k count from the corridor. The walking line from the
     * entry door to the bedroom (and bathroom) door is kept free.
     */
    private void apartmentUnit(byte[][] plan, Block[][] furn, byte[][] fm, int fx0, int fx1,
                               int z0, int z1, boolean corridorAtLowZ, boolean lobby, int entry)
    {
        if (z1 - z0 < 1 || fx1 - fx0 < 2)
        {
            return;
        }
        boolean[][] keep = new boolean[width][length];
        int salt = fx0 * 7 + z0 * 13 + (plan == groundPlan ? 0 : 1000);
        keep[entry][row(z0, z1, corridorAtLowZ, 0)] = true;
        if (lobby)
        {
            markRoom(plan, fx0, z0, fx1, z1, R_LOBBY);
            furnish(plan, furn, fm, keep, fx0, z0, fx1, z1, "lobby", salt);
            furnish(plan, furn, fm, keep, fx0, z0, fx1, z1, "lobby", salt + 1);
            return;
        }
        final int depth = z1 - z0 + 1, w = fx1 - fx0 + 1;
        if (w < 4 || depth < 3)
        {
            // a leftover sliver is no flat: a storage closet
            markRoom(plan, fx0, z0, fx1, z1, R_STORAGE);
            furnish(plan, furn, fm, keep, fx0, z0, fx1, z1, "closet", salt);
            return;
        }
        int ks = depth >= 5 ? depth / 2 : -1;    // split wall row (k), -1 = studio
        boolean bath = ks >= 0 && w >= 5 && depth - ks - 1 >= 2;
        boolean entryLow = entry == fx0;
        int bedDoor = entry; // bedroom door in line with the entry; the bathroom takes the far end
        markRoom(plan, fx0, z0, fx1, z1, R_LIVING);
        int livingRows = ks >= 0 ? ks : depth;
        for (int k = 0; k < livingRows; k++)
        {
            keep[entry][row(z0, z1, corridorAtLowZ, k)] = true; // straight line to the bedroom door
        }
        int lz0 = Math.min(row(z0, z1, corridorAtLowZ, 0), row(z0, z1, corridorAtLowZ, livingRows - 1));
        int lz1 = Math.max(row(z0, z1, corridorAtLowZ, 0), row(z0, z1, corridorAtLowZ, livingRows - 1));
        if (ks >= 0)
        {
            int zs = row(z0, z1, corridorAtLowZ, ks);
            wallLine(plan, fx0, zs, fx1, zs, WALL);
            plan[bedDoor][zs] = DOOR;
            keep[bedDoor][row(z0, z1, corridorAtLowZ, ks + 1)] = true;
            keep[bedDoor][row(z0, z1, corridorAtLowZ, ks - 1)] = true;
            if (bedDoor != entry)
            {
                int zr = row(z0, z1, corridorAtLowZ, ks - 1);
                for (int fx = Math.min(entry, bedDoor); fx <= Math.max(entry, bedDoor); fx++)
                {
                    keep[fx][zr] = true;
                }
            }
            for (int k = ks + 1; k < depth; k++)
            {
                markRoom(plan, fx0, row(z0, z1, corridorAtLowZ, k), fx1, row(z0, z1, corridorAtLowZ, k), R_BEDROOM);
            }
        }
        // ---- living part: kitchen run first (it needs a long wall); a studio
        // places its bed next, then living and dining fill what is left
        furnish(plan, furn, fm, keep, fx0, lz0, fx1, lz1, "kitchen", salt);
        markKitchenFloor(plan, fx0, lz0, fx1, lz1);
        if (ks < 0)
        {
            furnish(plan, furn, fm, keep, fx0, lz0, fx1, lz1, "bed", salt + 4);
        }
        furnish(plan, furn, fm, keep, fx0, lz0, fx1, lz1, "living", salt + 2);
        furnish(plan, furn, fm, keep, fx0, lz0, fx1, lz1, "dining", salt + 3);
        if (ks < 0)
        {
            return;
        }
        // ---- bedroom (+ bathroom in the 2 far columns)
        int bz0 = Math.min(row(z0, z1, corridorAtLowZ, ks + 1), row(z0, z1, corridorAtLowZ, depth - 1));
        int bz1 = Math.max(row(z0, z1, corridorAtLowZ, ks + 1), row(z0, z1, corridorAtLowZ, depth - 1));
        int bedFrom = fx0, bedTo = fx1;
        if (bath)
        {
            // bathroom: the 2 columns at the end away from the entry
            int wx = entryLow ? fx1 - 2 : fx0 + 2;
            int b0 = entryLow ? fx1 - 1 : fx0, b1 = entryLow ? fx1 : fx0 + 1;
            wallLine(plan, wx, bz0, wx, bz1, WALL);
            int zd = row(z0, z1, corridorAtLowZ, ks + 1);
            plan[wx][zd] = DOOR;
            keep[wx - 1][zd] = true;
            keep[wx + 1][zd] = true;
            markRoom(plan, b0, bz0, b1, bz1, R_BATH);
            furnish(plan, furn, fm, keep, b0, bz0, b1, bz1, "bath", salt + 5);
            furnish(plan, furn, fm, keep, b0, bz0, b1, bz1, "bath", salt + 6);
            if (entryLow)
            {
                bedTo = wx - 1;
            }
            else
            {
                bedFrom = wx + 1;
            }
        }
        furnish(plan, furn, fm, keep, bedFrom, bz0, bedTo, bz1, "bed", salt + 7);
        furnish(plan, furn, fm, keep, bedFrom, bz0, bedTo, bz1, "storage", salt + 8);
        furnish(plan, furn, fm, keep, bedFrom, bz0, bedTo, bz1, "desk", salt + 9);
    }

    /** Tiles under the kitchen run: the cells where a kitchen set stood (fridge, counters, oven, sink). */
    private void markKitchenFloor(byte[][] plan, int fx0, int z0, int fx1, int z1)
    {
        Block[][] f = plan == groundPlan ? groundFurn : upperFurn;
        for (int fx = fx0; fx <= fx1; fx++)
        {
            for (int z = z0; z <= z1; z++)
            {
                Block b = f[fx][z];
                if (b == Blocks.furnace || b == Blocks.cauldron || b == Blocks.double_stone_slab
                    || b == Blocks.quartz_block || b == deci("BlockElectricBoxBin") || b == deci("BlockWashingMachine"))
                {
                    markRoom(plan, fx - 1, z - 1, fx + 1, z + 1, R_KITCHEN);
                }
            }
        }
    }

    /** z of row k of a flat, counted from the corridor side. */
    private static int row(int z0, int z1, boolean corridorAtLowZ, int k)
    {
        return corridorAtLowZ ? z0 + k : z1 - k;
    }

    // ------------------------------------------------------------ furniture sets

    // sides of a room rectangle the set's back (row 0) can stand against:
    // 0 = low z wall, 1 = high z wall, 2 = low fx wall, 3 = high fx wall
    private static final int[][] SIDE_OUT = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    private static final int[][] SIDE_ALONG = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    /**
     * Places one set for this room kind into the rectangle, against one of
     * its walls, all cells free and off the kept walkway; tries the sets
     * (weighted, seeded order), every wall side and every offset. Returns
     * whether one was placed.
     */
    private boolean furnish(byte[][] plan, Block[][] furn, byte[][] fm, boolean[][] keep,
                            int fx0, int z0, int fx1, int z1, String kind, int salt)
    {
        java.util.List<net.decimation.worldgen.sets.FurnitureSet> sets =
            net.decimation.worldgen.sets.FurnitureSets.forRoom(kind);
        if (sets.isEmpty() || fx1 < fx0 || z1 < z0)
        {
            return false;
        }
        java.util.List<net.decimation.worldgen.sets.FurnitureSet> order =
            new java.util.ArrayList<net.decimation.worldgen.sets.FurnitureSet>(sets);
        // weighted shuffle: sort by -log(u) / weight
        final java.util.Map<net.decimation.worldgen.sets.FurnitureSet, Double> key =
            new java.util.HashMap<net.decimation.worldgen.sets.FurnitureSet, Double>();
        for (int i = 0; i < order.size(); i++)
        {
            double u = Math.max(1e-6, unit(salt, i * 31 + 7, fx0 + z0));
            key.put(order.get(i), -Math.log(u) / Math.max(1, order.get(i).weight));
        }
        java.util.Collections.sort(order, new java.util.Comparator<net.decimation.worldgen.sets.FurnitureSet>()
        {
            public int compare(net.decimation.worldgen.sets.FurnitureSet a, net.decimation.worldgen.sets.FurnitureSet b)
            {
                return Double.compare(key.get(a), key.get(b));
            }
        });
        int sideStart = (int) (unit(fx0, salt, z0) * 4);
        for (net.decimation.worldgen.sets.FurnitureSet set : order)
        {
            for (int si = 0; si < 4; si++)
            {
                int side = (sideStart + si) % 4;
                int len = side < 2 ? fx1 - fx0 + 1 : z1 - z0 + 1;
                int span = len - set.width + 1;
                if (span <= 0)
                {
                    continue;
                }
                int offStart = (int) (unit(fx0 + si, salt + 1, z0) * span);
                for (int oi = 0; oi < span; oi++)
                {
                    int off = (offStart + oi) % span;
                    if (fits(plan, keep, set, side, off, fx0, z0, fx1, z1))
                    {
                        placeSet(plan, furn, fm, keep, set, side, off, fx0, z0, fx1, z1, salt);
                        if (DEBUG_SETS)
                        {
                            cpw.mods.fml.common.FMLLog.info("[deciworldgen] sets: ok %s %s %s room %dx%d (fx %d..%d z %d..%d)",
                                kind, set.name, id, fx1 - fx0 + 1, z1 - z0 + 1, fx0, fx1, z0, z1);
                        }
                        return true;
                    }
                }
            }
        }
        if (DEBUG_SETS)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] sets: no %s fits %s room %dx%d (fx %d..%d z %d..%d)",
                                            kind, id, fx1 - fx0 + 1, z1 - z0 + 1, fx0, fx1, z0, z1);
        }
        return false;
    }

    private static final boolean DEBUG_SETS = System.getProperty("deciworldgen.debugsets") != null;

    /** Plan cell of set position (r, c) for a side and offset: {fx, z}. */
    private static int[] setCell(int side, int r, int c, int off, int fx0, int z0, int fx1, int z1)
    {
        switch (side)
        {
            case 0: return new int[] {fx0 + off + c, z0 + r};
            case 1: return new int[] {fx1 - off - c, z1 - r};
            case 2: return new int[] {fx0 + r, z1 - off - c};
            default: return new int[] {fx1 - r, z0 + off + c};
        }
    }

    private boolean fits(byte[][] plan, boolean[][] keep, net.decimation.worldgen.sets.FurnitureSet set, int side,
                         int off, int fx0, int z0, int fx1, int z1)
    {
        for (int y = 0; y < set.layers.length; y++)
        {
            for (int r = 0; r < set.depth; r++)
            {
                for (int c = 0; c < set.width; c++)
                {
                    char ch = set.at(y, r, c);
                    if (ch == ' ')
                    {
                        continue;
                    }
                    int[] p = setCell(side, r, c, off, fx0, z0, fx1, z1);
                    if (p[0] < fx0 || p[0] > fx1 || p[1] < z0 || p[1] > z1)
                    {
                        return false;
                    }
                    byte cell = plan[p[0]][p[1]];
                    char base = set.at(0, r, c);
                    boolean ownBase = base != ' ' && base != '.';
                    if (cell != OPEN && !(y > 0 && ownBase))
                    {
                        return false;
                    }
                    if (ch != '.' && keep[p[0]][p[1]] && y == 0)
                    {
                        return false;
                    }
                    if (y == 0 && r == 0 && ch != '.')
                    {
                        // the back must stand against a wall, never in front of a door or opening
                        int[] o = SIDE_OUT[side];
                        int bx = p[0] - o[0], bz = p[1] - o[1];
                        byte back = bx <= 0 || bz <= 0 || bx >= width - 1 || bz >= length - 1 ? WALL : plan[bx][bz];
                        if (back != WALL && back != LINING && back != GLASS)
                        {
                            return false;
                        }
                        if (back == LINING && set.at(2, r, c) != ' ' && set.at(2, r, c) != '.' && wallOpening(bx, bz))
                        {
                            return false; // full height pieces (wardrobe, wall cabinets) never cover a window
                        }
                    }
                }
            }
        }
        return true;
    }

    /** True when a lining cell is a window recess or passage on any storey height (checked on storey 1). */
    private boolean wallOpening(int fx, int z)
    {
        return liningOpen(fx, z, 1, 1) || liningOpen(fx, z, 1, 2) || liningOpen(fx, z, 0, 1) || liningOpen(fx, z, 0, 2);
    }

    private void placeSet(byte[][] plan, Block[][] furn, byte[][] fm, boolean[][] keep,
                          net.decimation.worldgen.sets.FurnitureSet set, int side, int off,
                          int fx0, int z0, int fx1, int z1, int salt)
    {
        boolean ground = plan == groundPlan;
        int[] o = SIDE_OUT[side], a = SIDE_ALONG[side];
        for (int y = 0; y < set.layers.length && y < 3; y++)
        {
            for (int r = 0; r < set.depth; r++)
            {
                for (int c = 0; c < set.width; c++)
                {
                    char ch = set.at(y, r, c);
                    if (ch == ' ')
                    {
                        continue;
                    }
                    int[] p = setCell(side, r, c, off, fx0, z0, fx1, z1);
                    if (ch == '.')
                    {
                        keep[p[0]][p[1]] = true;
                        continue;
                    }
                    net.decimation.worldgen.sets.FurnitureSet.Entry e = set.palette.get(ch);
                    if (e == null || e.blocks.isEmpty())
                    {
                        continue;
                    }
                    Block b = e.blocks.get((int) (unit(p[0] + salt, y * 7 + 3, p[1]) * e.blocks.size()));
                    int meta = setMeta(e, o, a);
                    plan[p[0]][p[1]] = FURN;
                    keep[p[0]][p[1]] = true;
                    if (y == 0)
                    {
                        furn[p[0]][p[1]] = b;
                        fm[p[0]][p[1]] = (byte) meta;
                    }
                    else if (y == 1)
                    {
                        (ground ? groundFurn2 : upperFurn2)[p[0]][p[1]] = b;
                        (ground ? groundFurn2Meta : upperFurn2Meta)[p[0]][p[1]] = (byte) meta;
                    }
                    else
                    {
                        (ground ? groundFurn3 : upperFurn3)[p[0]][p[1]] = b;
                        (ground ? groundFurn3Meta : upperFurn3Meta)[p[0]][p[1]] = (byte) meta;
                    }
                }
            }
        }
    }

    /** Metadata of a palette entry placed with wall-out vector o and along vector a (plan coords). */
    private int setMeta(net.decimation.worldgen.sets.FurnitureSet.Entry e, int[] o, int[] a)
    {
        if (e.face == null)
        {
            return e.meta;
        }
        int dfx, dz;
        if ("in".equals(e.face)) { dfx = -o[0]; dz = -o[1]; }
        else if ("right".equals(e.face)) { dfx = a[0]; dz = a[1]; }
        else if ("left".equals(e.face)) { dfx = -a[0]; dz = -a[1]; }
        else { dfx = o[0]; dz = o[1]; }
        String type = e.type != null ? e.type : "prop";
        if ("vanilla".equals(type)) return vanillaFacing(dfx, dz);
        if ("seat".equals(type)) return seatStairs(dfx, dz);
        if ("bed".equals(type)) return bedDir(dfx, dz) | ("head".equals(e.part) ? 8 : 0);
        if ("meta".equals(type)) return e.meta;
        if ("trapdoor".equals(type) || "hook".equals(type))
        {
            // face = where the supporting block is; trapdoor: open panel
            // flat against it (cabinet door), hook: a tap on the wall
            int wx = front == FRONT_WEST ? dfx : -dfx;
            int side = "trapdoor".equals(type)
                ? (dz > 0 ? 0 : dz < 0 ? 1 : wx > 0 ? 2 : 3)
                : (dz < 0 ? 0 : wx > 0 ? 1 : dz > 0 ? 2 : 3);
            return "trapdoor".equals(type) ? side | 4 : side;
        }
        return propFacing(dfx, dz);
    }

    private void planOffice(byte[][] plan, Block[][] furn, byte[][] fm, boolean ground)
    {
        markCore(plan);
        int coreEnd = coreFx >= 0 ? coreFx - 2 : width - 3;
        if (ground)
        {
            markRoom(plan, 1, 1, 7, length - 2, R_LOBBY);
            // reception desk facing the entrance, waiting chairs, vending machine
            int mid = length / 2;
            for (int z = mid - 2; z <= mid + 2; z++)
            {
                put(plan, furn, fm, 5, z, Blocks.double_stone_slab, 0);
            }
            put(plan, furn, fm, 6, mid, props.officeChair, propFacing(-1, 0));
            put(plan, furn, fm, 2, 1, props.chair, propFacing(1, 0));
            put(plan, furn, fm, 3, 1, props.chair, propFacing(1, 0));
            put(plan, furn, fm, 2, length - 2, props.vending, propFacing(0, -1));
            put(plan, furn, fm, 1, 1, Blocks.flower_pot, 0);
            put(plan, furn, fm, 1, length - 2, Blocks.flower_pot, 0);
            deskRows(plan, furn, fm, 8, coreEnd);
            return;
        }
        // meeting room in the front corner, glass partition, long table
        int mx1 = Math.min(6, coreEnd), mz1 = Math.min(5, length / 2 - 2);
        if (mx1 >= 4 && mz1 >= 3)
        {
            markRoom(plan, 1, 1, mx1, mz1, R_MEETING);
            wallLine(plan, 1, mz1 + 1, mx1, mz1 + 1, GLASS);
            wallLine(plan, mx1 + 1, 1, mx1 + 1, mz1 + 1, GLASS);
            plan[mx1 + 1][mz1 / 2 + 1] = DOOR;
            for (int fx = 2; fx <= mx1 - 1; fx++)
            {
                put(plan, furn, fm, fx, (1 + mz1) / 2 + 1, props.metalTable, 2);
                put(plan, furn, fm, fx, (1 + mz1) / 2, props.officeChair, propFacing(0, 1));
                put(plan, furn, fm, fx, (1 + mz1) / 2 + 2, props.officeChair, propFacing(0, -1));
            }
        }
        // storage room beside the core: shelves and crates
        if (coreFx >= 0 && coreFz >= 4)
        {
            markRoom(plan, coreFx + 1, 1, width - 2, coreFz - 2, R_STORAGE);
            wallLine(plan, coreFx, coreFz - 3, coreFx, coreFz - 2, WALL);
            for (int fx = coreFx + 1; fx <= width - 2; fx++)
            {
                put(plan, furn, fm, fx, 1, props.shelf, propFacing(0, 1));
            }
            put(plan, furn, fm, coreFx + 2, coreFz - 2, props.policeCrate, 2);
            put(plan, furn, fm, coreFx + 4, coreFz - 2, props.woodCrate, 2);
        }
        deskRows(plan, furn, fm, mx1 + 3, coreEnd);
    }

    /** Open plan: desk + chair pairs in rows, a gap every few desks. */
    private void deskRows(byte[][] plan, Block[][] furn, byte[][] fm, int fxFrom, int fxTo)
    {
        for (int fx = fxFrom; fx <= fxTo; fx += 4)
        {
            for (int z = 2; z <= length - 3; z++)
            {
                if (z % 4 == 0 || z % 4 == 3)
                {
                    continue; // desks in pairs, then an aisle
                }
                put(plan, furn, fm, fx, z, props.metalTable != null && unit(fx, z, 61) < 0.6 ? props.metalTable : props.table, 2);
                put(plan, furn, fm, fx + 1, z, props.officeChair, propFacing(-1, 0));
            }
        }
    }

    private void planShop(byte[][] plan, Block[][] furn, byte[][] fm, boolean ground)
    {
        int stockWall = Math.max(5, width - 5);
        markRoom(plan, 1, 1, stockWall - 1, length - 2, ground ? R_SHOP : R_STOCK);
        markRoom(plan, stockWall + 1, 1, width - 2, length - 2, R_STOCK);
        wallLine(plan, stockWall, 1, stockWall, length - 2, WALL);
        plan[stockWall][length / 2] = DOOR;
        // stockroom: crates, boxes, shelves on the back wall
        for (int z = 1; z <= length - 2; z++)
        {
            double d = unit(width, z, 71);
            if (d < 0.25)
            {
                put(plan, furn, fm, width - 2, z, props.cardboard, (int) (d * 16) % 4 + 2);
            }
            else if (d < 0.45)
            {
                put(plan, furn, fm, width - 2, z, props.shelf, propFacing(-1, 0));
            }
        }
        put(plan, furn, fm, stockWall + 2, 2, props.woodCrate, 2);
        put(plan, furn, fm, stockWall + 2, length - 3, unit(3, 3, 72) < 0.5 ? props.medicalCrate : props.ammoCrate, 2);
        if (!ground)
        {
            put(plan, furn, fm, 2, 2, props.table, 2);
            put(plan, furn, fm, 3, 2, props.officeChair, propFacing(-1, 0));
            put(plan, furn, fm, 2, length - 3, props.woodCrate, 2);
            return;
        }
        // checkout at the front left of the entrance, aisles behind a free zone
        put(plan, furn, fm, 2, 1, Blocks.double_stone_slab, 0);
        put(plan, furn, fm, 2, 2, Blocks.double_stone_slab, 0);
        put(plan, furn, fm, 3, 1, props.officeChair, propFacing(-1, 0));
        put(plan, furn, fm, 1, length - 2, props.vending, propFacing(1, 0));
        for (int z = 3; z <= length - 3; z += 3)
        {
            for (int fx = 4; fx <= stockWall - 2; fx++)
            {
                put(plan, furn, fm, fx, z, props.shelf, propFacing(0, 1));
            }
        }
        put(plan, furn, fm, stockWall - 1, 1, props.trashcan, 2);
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
