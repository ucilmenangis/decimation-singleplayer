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
    public static final int FLOOR = 4;
    public static final int FRONT_WEST = 0, FRONT_EAST = 1;
    /** Overgrowth styles, from the biome at the building's centre. */
    public static final int TEMPERATE = 0, LUSH = 1, COLD = 2, DRY = 3;
    public static final String[] STYLE_NAME = {"temperate", "lush", "cold", "dry"};
    /** Smallest setback that gets a front yard (room for a nose-in car). */
    public static final int MIN_YARD = 6;

    // floor plan cell codes
    private static final byte OPEN = 0, WALL = 1, DOOR = 2, GLASS = 3, FURN = 4, CORE = 5;

    public final String id;
    public final int minX, minZ, width, length, floors, kind, front, style;
    public final long seed;
    private final Props props;
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
    private final byte[][] groundFurnMeta, upperFurnMeta;
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
        boolean core = width >= 12 && length >= 10 && kind != SHOP;
        coreFx = core ? width - 8 : -1;
        coreFz = core ? length / 2 - 2 : -1;
        groundPlan = new byte[width][length];
        upperPlan = new byte[width][length];
        groundFurn = new Block[width][length];
        upperFurn = new Block[width][length];
        groundFurnMeta = new byte[width][length];
        upperFurnMeta = new byte[width][length];
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
                planApartment(groundPlan, groundFurn, groundFurnMeta, true);
                planApartment(upperPlan, upperFurn, upperFurnMeta, false);
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
        boolean path = frontYard && Math.abs(z - (minZ + length / 2)) <= 1;
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
            && (z - lotZ) % 4 == 2 && Math.abs(z - (minZ + length / 2)) >= 3
            && Math.abs(target - baseY) <= 1 && unit(x, 77, z) < 0.45)
        {
            Block car = props.cars[(int) (unit(x, 78, z) * props.cars.length)];
            // long axis along x at rotation 0 / 180: metadata 4 or 2
            world.setBlock(x, target + 1, z, car, unit(x, 79, z) < 0.5 ? 4 : 2, 2);
            net.decimation.fixes.MultiblockRepairHandler.repair(world.getTileEntity(x, target + 1, z));
        }
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
        if (collapsed(fx, ly, z))
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
            return floorBlock(meta, storey);
        }
        if (edge)
        {
            return outerWall(fx, ly, z, storey, within, meta);
        }
        if (coreFx < 0 && fx == width - 2 && z == length - 2)
        {
            return ladder(meta);
        }
        byte[][] plan = storey == 0 ? groundPlan : upperPlan;
        switch (plan[fx][z])
        {
            case WALL:
                if (within == 3 || unit(fx, ly, z) > decay * 0.3)
                {
                    meta[0] = kind == OFFICE ? 0 : 2;
                    return kind == OFFICE ? Blocks.stonebrick : Blocks.planks;
                }
                return null;
            case GLASS:
                if (within <= 2 && unit(fx, ly, z) > 0.5 + decay * 0.3)
                {
                    return Blocks.glass_pane;
                }
                return within == 3 ? Blocks.stonebrick : null;
            case DOOR:
                return within == 3 ? (kind == OFFICE ? Blocks.stonebrick : Blocks.planks) : null;
            case FURN:
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
                return null;
        }
    }

    // ------------------------------------------------------------ parts

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
            && Math.abs(along - span / 2) <= (kind == SHOP ? 1 : kind == OFFICE ? 1 : 0))
        {
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
     * a = 6. Storey s climbs on lane b 0..1 (even s, from a = 1 up to a = 4)
     * or b 2..3 (odd s, from a = 4 down to a = 1); step j sits at height
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
                if (j == 3)
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
        }
        return null; // core interior stays open
    }

    private static boolean onLane(int storey, int b)
    {
        return ((storey & 1) == 0) == (b <= 1);
    }

    /** Step index 0..3 of storey's run at core position a, or -1. */
    private static int stepIndex(int storey, int a)
    {
        int j = (storey & 1) == 0 ? a - 1 : 4 - a;
        return j >= 0 && j <= 3 ? j : -1;
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

    private Block debris(int fx, int ly, int z, int[] meta)
    {
        double d = unit(fx, ly, z);
        if (d < decay * 0.10)
        {
            return debrisBlock(fx, ly, z, meta);
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
        if (unit(fx, storey * 31, z) < 0.12 + decay * 0.25)
        {
            return debris(fx, storey * FLOOR + 1, z, meta);
        }
        if (isCrate(b) && unit(storey, fx, z) < 0.45)
        {
            return null;
        }
        meta[0] = m[fx][z];
        return b;
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
        plan[fx][z] = FURN;
        furn[fx][z] = b;
        fm[fx][z] = (byte) meta;
    }

    /** Prop facing (PropRenderer rotation = meta % 4 * 90) toward a world direction. */
    private int propFacing(int dfx, int dz)
    {
        // normalise to world: mirrored buildings flip x
        int dx = front == FRONT_WEST ? dfx : -dfx;
        if (dx < 0) return 2;
        if (dz < 0) return 3;
        if (dx > 0) return 4;
        return 5;
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
        // side walls of the core, open toward the front landing
        wallLine(plan, coreFx + 1, coreFz - 1, coreFx + 6, coreFz - 1, WALL);
        wallLine(plan, coreFx + 1, coreFz + 4, coreFx + 6, coreFz + 4, WALL);
    }

    private void planApartment(byte[][] plan, Block[][] furn, byte[][] fm, boolean ground)
    {
        markCore(plan);
        int c0 = length / 2 - 1, c1 = length / 2;      // corridor rows
        int end = coreFx >= 0 ? coreFx - 1 : width - 2;
        boolean doubleLoaded = length >= 11;
        // corridor walls
        if (doubleLoaded)
        {
            wallLine(plan, 1, c0 - 1, end, c0 - 1, WALL);
        }
        wallLine(plan, 1, c1 + 1, end, c1 + 1, WALL);
        // units along the corridor
        int fx = 1;
        int unitNo = 0;
        while (fx < end - 2)
        {
            int len = Math.min(5 + (int) (unit(fx, unitNo, 41) * 4), end - fx);
            int ux0 = fx, ux1 = fx + len - 1;
            if (ux1 < end - 1)
            {
                wallLine(plan, ux1 + 1, 1, ux1 + 1, c0 - 2, WALL);
                wallLine(plan, ux1 + 1, c1 + 2, ux1 + 1, length - 2, WALL);
            }
            int door = (ux0 + ux1) / 2;
            if (doubleLoaded)
            {
                plan[door][c0 - 1] = DOOR;
                apartmentUnit(plan, furn, fm, ux0, ux1, 1, c0 - 2, false, ground && fx <= 2);
            }
            plan[door][c1 + 1] = DOOR;
            apartmentUnit(plan, furn, fm, ux0, ux1, c1 + 2, length - 2, true, ground && fx <= 2);
            fx = ux1 + 2;
            unitNo++;
        }
        if (ground)
        {
            put(plan, furn, fm, 1, c0 - (doubleLoaded ? 0 : 0), props.mailbox, propFacing(0, 1));
        }
    }

    /** One flat: living part on the corridor side, bedroom on the window side. */
    private void apartmentUnit(byte[][] plan, Block[][] furn, byte[][] fm, int fx0, int fx1,
                               int z0, int z1, boolean corridorAtLowZ, boolean lobby)
    {
        if (z1 - z0 < 1 || fx1 - fx0 < 2)
        {
            return;
        }
        if (lobby)
        {
            return; // ground floor front unit stays an open lobby
        }
        int depth = z1 - z0 + 1;
        int split = depth >= 5 ? (corridorAtLowZ ? z0 + depth / 2 : z1 - depth / 2) : -1;
        if (split >= 0)
        {
            wallLine(plan, fx0, split, fx1, split, WALL);
            plan[(fx0 + fx1) / 2 + ((fx1 - fx0) > 3 ? 1 : 0)][split] = DOOR;
        }
        int livingNear = corridorAtLowZ ? z0 : z1;
        int bedFar = corridorAtLowZ ? z1 : z0;
        int dirIn = corridorAtLowZ ? 1 : -1;
        // kitchen corner on the unit's far fx wall: counter, stove, sink
        put(plan, furn, fm, fx1, livingNear + dirIn, props.cooking != null ? props.cooking : Blocks.furnace,
            props.cooking != null ? propFacing(-1, 0) : 4);
        put(plan, furn, fm, fx1, livingNear + 2 * dirIn, Blocks.cauldron, 0);
        put(plan, furn, fm, fx1 - 1, livingNear + dirIn, Blocks.double_stone_slab, 0);
        // dining: table with chairs
        int tx = (fx0 + fx1) / 2 - 1, tz = livingNear + (split >= 0 ? dirIn : 0);
        put(plan, furn, fm, tx, tz, props.table, 2);
        put(plan, furn, fm, tx - 1, tz, props.chair, propFacing(1, 0));
        put(plan, furn, fm, tx + 1, tz, props.chair, propFacing(-1, 0));
        // bedroom: bed along the exterior wall, wardrobe, maybe a crate
        if (split >= 0)
        {
            int bz = bedFar;
            int bx = fx0 + 1;
            if (plan[bx][bz] == OPEN && plan[bx + 1][bz] == OPEN)
            {
                // bed: foot at bx, head at bx + 1 (meta: direction + 8 for head)
                int dir = front == FRONT_WEST ? 3 : 1; // head toward +fx in world terms
                plan[bx][bz] = FURN; furn[bx][bz] = Blocks.bed; fm[bx][bz] = (byte) dir;
                plan[bx + 1][bz] = FURN; furn[bx + 1][bz] = Blocks.bed; fm[bx + 1][bz] = (byte) (dir | 8);
            }
            put(plan, furn, fm, fx1, bz, unit(fx0, z0, 51) < 0.5 ? Blocks.bookshelf : props.shelf, propFacing(-1, 0));
            put(plan, furn, fm, fx1, bz - dirIn, unit(fx0, z0, 52) < 0.5 ? props.woodCrate : props.medicalCrate, 2);
        }
        else
        {
            // studio: the bed stands against the window wall in the one room
            int bx = fx0;
            if (bx + 1 <= fx1 && plan[bx][bedFar] == OPEN && plan[bx + 1][bedFar] == OPEN)
            {
                int dir = front == FRONT_WEST ? 3 : 1;
                plan[bx][bedFar] = FURN; furn[bx][bedFar] = Blocks.bed; fm[bx][bedFar] = (byte) dir;
                plan[bx + 1][bedFar] = FURN; furn[bx + 1][bedFar] = Blocks.bed; fm[bx + 1][bedFar] = (byte) (dir | 8);
            }
            put(plan, furn, fm, fx1, bedFar, props.woodCrate, 2);
        }
    }

    private void planOffice(byte[][] plan, Block[][] furn, byte[][] fm, boolean ground)
    {
        markCore(plan);
        int coreEnd = coreFx >= 0 ? coreFx - 2 : width - 3;
        if (ground)
        {
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
