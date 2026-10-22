package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.decimation.worldgen.CityLayout;
import net.decimation.worldgen.Plan;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.Slices;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Lost Cities style city (docs/city_engine.md): city sectors are cut into
 * cells of 4 x 4 chunks; each cell's first chunk row and column are street
 * chunks, the other 3 x 3 chunks hold converted Lost Cities buildings
 * (LcContent: DeceasedCraft current and legacy packs) picked from the
 * cell's district style. Every cell has a city level (0..2): street
 * surface, sidewalks and ground floors at BASE + level * LEVEL, cellars
 * below; where a street meets a higher street of the next cell, a stairs
 * part of the district climbs the 6 blocks (Lost Cities rules, read from
 * its source: LostCityTerrainFeature.generateStreet / generateBuilding /
 * generateStreetDecorations). Cells next to a non city sector stay at
 * level 0 so the city meets the land.
 */
public final class LcCity
{
    public static final int CELL = 4;      // chunks per cell side (= StructureGenerator.CELL)
    public static final int BASE = 64;     // street surface at level 0
    public static final int LEVEL = 6;     // Lost Cities FLOORHEIGHT
    public static final int MAX_LEVEL = 2;
    /** Share of aligned 2 x 2 city cell groups that become one superblock. */
    static final float SUPER_CHANCE = 0.3f;
    /** City cells this close (in cells) to a military sector form the wasteland district. */
    static final int DEADZONE_RANGE = 2;
    /** Width of the ramp graded into the land around the city. */
    /** Widest ramp between a city cell and the land outside (blocks). */
    static final int EDGE = 24;
    /** Share of building chunks left as open lots (parks where the district has park parts). */
    static final float LOT_CHANCE = 0.10f;
    /** Street scene (the packs' "fountains": bus, ambulance, roadblock...) per straight street chunk. */
    static final double SCENE_CHANCE = 0.06;
    /** Front part per street side facing a building at the same level. */
    static final double FRONT_CHANCE = 0.5;
    /** Sidewalk width on each side of a street chunk (flush with the road). */
    static final int SIDEWALK = 3;
    static final int TOP = 250;

    private final Block road;
    StreetProps props = new StreetProps();
    final Highways highways = new Highways(this);

    public void setStreetProps(StreetProps p)
    {
        props = p;
    }
    /** Bumped when the layout rules change at run time (city rivers switched): cached plans are dropped. */
    private static volatile int plansEpoch;
    private int epoch;

    static void clearPlans()
    {
        plansEpoch++;
    }

    /** Dev scan of the wasteland gates around a point (WastelandFence.scan; DevPregen, -Ddeciworldgen.gatescan). */
    /** Metadata of the wire on top of the inside fence (WastelandFence.column) for a line along x or z (dev tests). */
    public static int fenceWireMeta(net.minecraft.block.Block wire, boolean alongX)
    {
        return Math.max(2, PropFacing.along(wire, alongX, 0));
    }

    public static void gateScan(long seed, int x, int z, int r)
    {
        WastelandFence.scan(seed, x, z, r);
        // the city refuges there by the generator's own choice (docs/safezone.md section 7): one line per sector with one
        int reg = Sectors.REGION_BLOCKS;
        for (int rx = Math.floorDiv(x - r, reg); rx <= Math.floorDiv(x + r, reg); rx++)
        {
            for (int rz = Math.floorDiv(z - r, reg); rz <= Math.floorDiv(z + r, reg); rz++)
            {
                int[] ref = refuge(seed, rx, rz);
                if (ref != null)
                {
                    CityLayout.Plan plan = CityLayout.on() ? CityLayout.planAt(seed, rx, rz) : null;
                    cpw.mods.fml.common.FMLLog.info("[deciworldgen] lc refuge in sector %d,%d: block %d,%d at %d,%d (turns %d), "
                                                    + "city of %s districts, wasteland here %s", rx, rz, ref[0], ref[1],
                                                    ref[0] * CELL * 16 + 16, ref[1] * CELL * 16 + 16, ref[2],
                                                    plan == null ? "?" : String.valueOf(plan.size * 4),
                                                    Wasteland.sector(seed, rx, rz));
                }
            }
        }
    }

    /** Layout rules changed outside the city package (city refuges switched): cached plans are dropped. */
    public static void layoutChanged()
    {
        plansEpoch++;
    }

    private final Map<Long, List<Plan>> cache = new LinkedHashMap<Long, List<Plan>>(64, 0.75f, true)
    {
        protected boolean removeEldestEntry(Map.Entry<Long, List<Plan>> e)
        {
            return size() > 256;
        }
    };

    /**
     * On when converted content is installed, unless switched off with
     * -Ddeciworldgen.lccity=false (then the procedural CityDistrict runs).
     */
    public static boolean enabled()
    {
        return LcContent.active() && !"false".equals(System.getProperty("deciworldgen.lccity"));
    }

    public LcCity(Block road)
    {
        this.road = road != null ? road : Blocks.stone;
        instance = this;
    }

    /** The generator's city (DecimationWorldGen.init), for questions asked outside generation (Places). */
    private static volatile LcCity instance;

    /**
     * The kind of the city building planned at (x, z) (BuildingDress.KIND_NAMES: "police", "medical", "military", ...), or
     * null outside one (streets, lots, parks, land outside cities, the procedural city). From the seed's plan of the cell
     * (the cached one generation uses); for Places, which memoises it per chunk.
     */
    public static String buildingKind(long seed, int x, int z)
    {
        LcCity c = instance;
        int cx = Math.floorDiv(x >> 4, CELL), cz = Math.floorDiv(z >> 4, CELL);
        if (c == null || !enabled() || !isCity(seed, cx, cz))
        {
            return null;
        }
        List<Plan> plans = c.plan(seed, cx, cz);
        for (int i = 0; i < plans.size(); i++)
        {
            Plan p = plans.get(i);
            if (p instanceof BuildingPlan && x >= p.minX() && x <= p.maxX() && z >= p.minZ() && z <= p.maxZ())
            {
                return BuildingDress.KIND_NAMES[((BuildingPlan) p).dressKind()];
            }
        }
        return null;
    }

    /** Writes every city piece of the cells this chunk's population window touches. */
    public void populate(World world, int chunkX, int chunkZ, StructureGenerator gen)
    {
        int[] w = Slices.window(chunkX, chunkZ);
        // cells within EDGE of the window: their edge ramp may reach into it
        int c0x = Math.floorDiv((w[0] - EDGE) >> 4, CELL), c1x = Math.floorDiv((w[2] + EDGE) >> 4, CELL);
        int c0z = Math.floorDiv((w[1] - EDGE) >> 4, CELL), c1z = Math.floorDiv((w[3] + EDGE) >> 4, CELL);
        java.util.Set<Long> done = new java.util.HashSet<Long>();
        long seed = world.getSeed();
        for (int cx = c0x; cx <= c1x; cx++)
        {
            for (int cz = c0z; cz <= c1z; cz++)
            {
                if (!isCity(seed, cx, cz))
                {
                    continue;
                }
                Wasteland.zone(world, cx, cz);
                int[] b = block(seed, cx, cz);
                if (!done.add(((long) b[0] << 32) ^ (b[1] & 0xffffffffL)))
                {
                    continue; // a superblock already handled through another of its cells
                }
                for (Plan p : plan(seed, b[0], b[1]))
                {
                    if (Slices.intersects(p, w))
                    {
                        Slices.place(world, p, w);
                    }
                }
            }
        }
        highways.populate(world, w);
        Rails.populate(world, w);
    }

    /**
     * Cuts or fills one column from its natural soil height to target,
     * keeping the column's own surface block (shared by the city edge and
     * the highway sides).
     */
    static void reshape(World world, int x, int z, int natural, int target)
    {
        if (target == natural)
        {
            return;
        }
        Block surface = world.getBlock(x, natural, z);
        int surfaceMeta = world.getBlockMetadata(x, natural, z);
        if (surface == Blocks.air || surface.getMaterial().isLiquid())
        {
            return;
        }
        Block filler = surface == Blocks.grass || surface == Blocks.mycelium ? Blocks.dirt
            : surface instanceof net.minecraft.block.BlockFalling ? Blocks.stone : surface;
        for (int y = natural; y < target; y++)
        {
            world.setBlock(x, y, z, filler, 0, 2);
        }
        for (int y = target + 1; y <= natural + 12; y++)
        {
            Block b = world.getBlock(x, y, z);
            if (y <= natural || StructureGenerator.clearable(b))
            {
                if (b != Blocks.air)
                {
                    world.setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
            else
            {
                break;
            }
        }
        if (surface instanceof net.minecraft.block.BlockFalling
            && !world.getBlock(x, target - 1, z).getMaterial().isSolid())
        {
            surface = filler;
            surfaceMeta = 0;
        }
        world.setBlock(x, target, z, surface, surfaceMeta, 2);
    }

    /** A decor part of the district (falling back to the town styles), picked by chunk hash; null if none. */
    static LcContent.Shape decor(long seed, String style, String kind, int chx, int chz, long salt)
    {
        List<String> files = LcContent.decor(style, kind);
        if (files.isEmpty() && !style.endsWith(":deadzone"))
        {
            files = LcContent.decor("legacy:standardcity", kind);
        }
        if (files.isEmpty())
        {
            return null;
        }
        return LcContent.shape(files.get((int) (hl(seed ^ salt, chx, chz) % files.size())));
    }

    /**
     * Block of a part turned clockwise by turns (0..3) at chunk local x / z
     * and part layer y, metadata turned with it; null where the part is
     * empty or out of its bounds (inverse of a clockwise turn, as SchematicPlan).
     */
    static Block partAt(LcContent.Shape s, int turns, int lx, int y, int lz, int[] meta)
    {
        if (y < 0 || y >= s.height)
        {
            return null;
        }
        int x, z;
        switch (turns)
        {
            case 1:  x = lz; z = s.length - 1 - lx; break;
            case 2:  x = s.width - 1 - lx; z = s.length - 1 - lz; break;
            case 3:  x = s.width - 1 - lz; z = lx; break;
            default: x = lx; z = lz; break;
        }
        if (x < 0 || z < 0 || x >= s.width || z >= s.length)
        {
            return null;
        }
        int i = s.index(x, y, z);
        Block b = s.blocks[i];
        if (b == null || s.skip[i])
        {
            return null;
        }
        meta[0] = net.decimation.worldgen.Rotation.rotateMeta(Block.getIdFromBlock(b), s.meta[i] & 15, turns);
        if (net.decimation.worldgen.military.PropBoxes.box(b, meta[0]) != null)
        {
            for (int k = 0; k < turns; k++)
            {
                meta[0] = net.decimation.worldgen.military.PropBoxes.turn(b, meta[0]);   // Rotation leaves props
            }
        }
        if ((turns & 1) == 1 && b == roadLine())
        {
            meta[0] ^= 2; // painted line: meta % 4 0 / 1 runs north south, 2 / 3 east west
        }
        return b;
    }

    static Block roadLine;

    static Block roadLine()
    {
        if (roadLine == null)
        {
            Block b = Block.getBlockFromName("deci:BlockRoad_CenterLine");
            roadLine = b != null ? b : Blocks.bedrock;
        }
        return roadLine;
    }

    /** True when the box comes within EDGE of a city cell (the city's ramp may cut it). */
    public static boolean nearCity(long seed, int x0, int z0, int x1, int z1)
    {
        for (int cx = Math.floorDiv(x0 - EDGE, CELL * 16); cx <= Math.floorDiv(x1 + EDGE, CELL * 16); cx++)
        {
            for (int cz = Math.floorDiv(z0 - EDGE, CELL * 16); cz <= Math.floorDiv(z1 + EDGE, CELL * 16); cz++)
            {
                if (isCity(seed, cx, cz))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A city cell: on a city sector, and (worlds since v0.56, Wasteland.apart) not in the ring of open land
     * that sets a wasteland district apart from the rest of its city.
     */
    public static boolean isCity(long seed, int cellX, int cellZ)
    {
        return sectorCity(seed, cellX, cellZ) && !Wasteland.ring(seed, cellX, cellZ);
    }

    /** On a city sector (the city before the wasteland's ring is cut out of it). */
    static boolean sectorCity(long seed, int cellX, int cellZ)
    {
        return Sectors.sector(seed, cellX * CELL, cellZ * CELL) == StructureGenerator.CITY;
    }

    /** The installed content has the wasteland style (legacy deadzone). */
    static boolean hasDeadzone()
    {
        return styleList().contains("legacy:deadzone");
    }

    /** City level of a cell: smooth noise over a 3 cell lattice, 0 at the city's edge, rising inward. */
    public static int level(long seed, int cellX, int cellZ)
    {
        if (CityRivers.cell(seed, cellX, cellZ))
        {
            return 0; // bridges stay at level 0 (Lost Cities: no bridges at higher levels)
        }
        // distance (in cells, up to MAX_LEVEL) to the nearest non city or river cell
        int edge = MAX_LEVEL;
        for (int r = 1; r <= MAX_LEVEL && edge == MAX_LEVEL; r++)
        {
            for (int dx = -r; dx <= r && edge == MAX_LEVEL; dx++)
            {
                for (int dz = -r; dz <= r; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r && (!isCity(seed, cellX + dx, cellZ + dz)
                        || CityRivers.cell(seed, cellX + dx, cellZ + dz)))
                    {
                        edge = r - 1;
                        break;
                    }
                }
            }
        }
        double gx = cellX / 3.0, gz = cellZ / 3.0;
        int ix = (int) Math.floor(gx), iz = (int) Math.floor(gz);
        double fx = gx - ix, fz = gz - iz;
        double v = lerp(lerp(h(seed, ix, iz), h(seed, ix + 1, iz), fx), lerp(h(seed, ix, iz + 1), h(seed, ix + 1, iz + 1), fx), fz);
        return Math.min(edge, Math.min(MAX_LEVEL, (int) (v * (MAX_LEVEL + 1))));
    }

    static double lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }

    /** Integer hash of a position (every bit usable). */
    static long hl(long seed, int x, int z)
    {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        v *= 0x94D049BB133111EBL;
        v ^= v >>> 32;
        return v & Long.MAX_VALUE;
    }

    static double h(long seed, int x, int z)
    {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ 0x4C43495459L;
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (v >>> 11) * 0x1.0p-53;
    }

    /**
     * District style ("<pack>:<style>") of a cell, shared by 2 x 2 cell
     * blocks: the wasteland district (legacy deadzone: factories, bunkers,
     * military base) within DEADZONE_RANGE cells of a military sector;
     * elsewhere a weighted pick, the current beta districts twice as likely
     * as the legacy town styles.
     */
    public static String style(long seed, int cellX, int cellZ)
    {
        int gx = Math.floorDiv(cellX, 2) * 2, gz = Math.floorDiv(cellZ, 2) * 2;
        List<String> styles = new ArrayList<String>(styleList());
        if (styles.isEmpty())
        {
            return "";
        }
        String dead = styles.contains("legacy:deadzone") ? "legacy:deadzone" : null;
        if (dead != null && Wasteland.sectors())
        {
            if (Wasteland.deadCell(seed, cellX, cellZ))
            {
                return dead;                              // a whole wasteland city sector
            }
            styles.remove(dead);
        }
        else if (dead != null)
        {
            for (int dx = -DEADZONE_RANGE; dx <= DEADZONE_RANGE + 1; dx++)
            {
                for (int dz = -DEADZONE_RANGE; dz <= DEADZONE_RANGE + 1; dz++)
                {
                    if (Sectors.sector(seed, (gx + dx) * CELL, (gz + dz) * CELL) == StructureGenerator.MIL)
                    {
                        return dead;
                    }
                }
            }
            styles.remove(dead);
        }
        int total = 0;
        for (String st : styles)
        {
            total += st.startsWith("dc:") ? 2 : 1;
        }
        double v = h(seed ^ 0x5354594C45L, gx, gz) * total;
        for (String st : styles)
        {
            v -= st.startsWith("dc:") ? 2 : 1;
            if (v < 0)
            {
                return st;
            }
        }
        return styles.get(styles.size() - 1);
    }

    private static volatile List<String> styleList;
    private static volatile int styleListOf = -1;

    /** Every district style of the installed content, sorted (built once: the NPC checks ask often). */
    private static List<String> styleList()
    {
        List<LcContent.Building> all = LcContent.buildings();
        List<String> l = styleList;
        if (l != null && styleListOf == all.size())
        {
            return l;
        }
        l = new ArrayList<String>();
        for (LcContent.Building b : all)
        {
            for (String s : b.styles.keySet())
            {
                if (!l.contains(s))
                {
                    l.add(s);
                }
            }
        }
        java.util.Collections.sort(l);
        styleList = l;
        styleListOf = all.size();
        return l;
    }

    /**
     * The block a cell belongs to: {origin cell x, origin cell z, size in
     * cells}. Aligned 2 x 2 cell groups become one SUPERBLOCK (the inner
     * streets dropped, 7 x 7 building chunks) with SUPER_CHANCE when all 4
     * cells are city: room for the big multi buildings (towers, school).
     */
    public static int[] block(long seed, int cellX, int cellZ)
    {
        int[] air = Airport.block(seed, cellX, cellZ);          // {ox, oz, KIND, cells x, cells z}
        if (air != null)
        {
            return air;
        }
        int ox = Math.floorDiv(cellX, 2) * 2, oz = Math.floorDiv(cellZ, 2) * 2;
        if (h(seed ^ 0x5355504552L, ox, oz) < SUPER_CHANCE && isCity(seed, ox, oz) && isCity(seed, ox + 1, oz)
            && isCity(seed, ox, oz + 1) && isCity(seed, ox + 1, oz + 1))
        {
            return new int[] {ox, oz, 2};
        }
        return new int[] {cellX, cellZ, 1};
    }

    /** Street chunk of a city (dev tests). */
    public static boolean streetChunk(long seed, int chx, int chz)
    {
        return isStreetChunk(seed, chx, chz);
    }

    /** True for a chunk in the first chunk row or column of its block (a street chunk). */
    static boolean isStreetChunk(long seed, int chx, int chz)
    {
        int[] b = block(seed, Math.floorDiv(chx, CELL), Math.floorDiv(chz, CELL));
        return chx - b[0] * CELL == 0 || chz - b[1] * CELL == 0;
    }

    /** City ground of a cell (a superblock uses its lowest member level, so one level for all). */
    public static int ground(long seed, int cellX, int cellZ)
    {
        int[] b = block(seed, cellX, cellZ);
        int l = level(seed, b[0], b[1]);
        if (b[2] == Airport.KIND)
        {
            for (int i = 0; i < b[3]; i++)
            {
                for (int j = 0; j < b[4]; j++)
                {
                    l = Math.min(l, level(seed, b[0] + i, b[1] + j));   // one ground for the whole airport
                }
            }
        }
        else if (b[2] == 2)
        {
            l = Math.min(Math.min(l, level(seed, b[0] + 1, b[1])),
                         Math.min(level(seed, b[0], b[1] + 1), level(seed, b[0] + 1, b[1] + 1)));
        }
        return BASE + l * LEVEL;
    }

    /** Every piece of one cell: 7 street chunks and the buildings / lots of the 3 x 3 block. */
    public synchronized List<Plan> plan(long seed, int cellX, int cellZ)
    {
        int[] blk = block(seed, cellX, cellZ);
        if (blk[0] != cellX || blk[1] != cellZ)
        {
            return plan(seed, blk[0], blk[1]); // a superblock is planned from its origin cell
        }
        if (epoch != plansEpoch)
        {
            cache.clear();
            epoch = plansEpoch;
        }
        long key = ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
        List<Plan> out = cache.get(key);
        if (out != null)
        {
            return out;
        }
        out = new ArrayList<Plan>();
        if (blk[2] == Airport.KIND)
        {
            planAirport(seed, blk, out);
            cache.put(key, out);
            return out;
        }
        int size = blk[2] * CELL; // chunks per block side: 4, or 8 for a superblock
        int g = ground(seed, cellX, cellZ);
        String style = style(seed, cellX, cellZ);
        int bx = cellX * CELL * 16, bz = cellZ * CELL * 16;
        // street chunks: the block's first column and first row
        for (int k = 0; k < 2 * size - 1; k++)
        {
            int lx = k < size ? 0 : k - size + 1, lz = k < size ? k : 0;
            out.add(street(seed, cellX, cellZ, lx, lz, cellX * CELL + lx, cellZ * CELL + lz, g, style));
        }
        // land next to the city: a band ramping from street level to the natural height
        for (int mx = 0; mx < blk[2]; mx++)
        {
            for (int mz = 0; mz < blk[2]; mz++)
            {
                int ccx = cellX + mx, ccz = cellZ + mz;
                boolean[][] city = new boolean[3][3];
                boolean open = false;
                for (int i = 0; i < 3; i++)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        city[i][j] = isCity(seed, ccx + i - 1, ccz + j - 1);
                        open |= !city[i][j];
                    }
                }
                if (open)
                {
                    out.add(new EdgePlan("lce_" + ccx + "_" + ccz, ccx, ccz, g, city));
                }
            }
        }
        // the building block (3 x 3 chunks, 7 x 7 in a superblock)
        Random r = new Random(seed ^ (cellX * 341873128712L + cellZ * 132897987541L) ^ 0x4C4342L);
        boolean[][] used = new boolean[size][size];
        // river chunks: a canal, no building (multi buildings fit around them)
        for (int i = 1; i < size; i++)
        {
            for (int j = 1; j < size; j++)
            {
                int chx = cellX * CELL + i, chz = cellZ * CELL + j;
                if (CityRivers.chunk(seed, chx, chz))
                {
                    used[i][j] = true;
                    out.add(new RiverPlan("lcr_" + chx + "_" + chz, seed, chx, chz, g, null, false));
                }
            }
        }
        // the city refuge of this region (docs/safezone.md): our fortified building on the whole block
        int[] refuge = blk[2] == 1 && net.decimation.worldgen.SafezoneSites.refuges()
            ? refuge(seed, Math.floorDiv(cellX * CELL, Sectors.REGION), Math.floorDiv(cellZ * CELL, Sectors.REGION)) : null;
        if (refuge != null && refuge[0] == cellX && refuge[1] == cellZ)
        {
            for (int i = 1; i < size; i++)
            {
                for (int j = 1; j < size; j++)
                {
                    used[i][j] = true;
                }
            }
            out.add(new net.decimation.worldgen.military.CityRefugePlan("lcz_" + cellX + "_" + cellZ,
                seed ^ hl(seed ^ 0x52454655L, cellX, cellZ), bx + 16, bz + 16, refuge[2], g));
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] city refuge lcz_%d_%d planned at %d,%d (turns %d)",
                                            cellX, cellZ, bx + 16, bz + 16, refuge[2]);
        }
        List<LcContent.Building> pool = new ArrayList<LcContent.Building>();
        List<LcContent.Building> big = new ArrayList<LcContent.Building>();
        for (LcContent.Building b : LcContent.buildings())
        {
            if (b.styles.containsKey(style) && b.cx < size && b.cz < size
                && g - b.groundY >= 4 && g - b.groundY + b.height <= TOP)
            {
                pool.add(b);
                if (b.cx * b.cz > 9)
                {
                    big.add(b);
                }
            }
        }
        // the wasteland district is one military zone as a whole (Wasteland.zone), not per building
        net.decimation.worldgen.ZoneKind zone = style.endsWith(":deadzone") ? null : net.decimation.worldgen.ZoneKind.POLICE;
        for (int i = 1; i < size; i++)
        {
            for (int j = 1; j < size; j++)
            {
                if (used[i][j])
                {
                    continue;
                }
                // a superblock gets its landmark (a multi building over 3 x 3) first
                List<LcContent.Building> from = i == 1 && j == 1 && !big.isEmpty() ? big : pool;
                LcContent.Building pick = from == pool && r.nextFloat() < Wasteland.lotChance(style) ? null
                    : pick(from, style, used, i, j, r);
                int minX = bx + i * 16, minZ = bz + j * 16;
                if (pick == null)
                {
                    used[i][j] = true;
                    // a third of the open lots stay bare (parking, a camp, a dump: LotDress) when the city
                    // dressing is on; the rest carry a park part of the district
                    boolean bare = Dress.ON && hl(seed ^ 0x42415245L, minX >> 4, minZ >> 4) % 100 < Wasteland.bareShare(style);
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g,
                                        bare ? null : decor(seed, style, "parks", minX >> 4, minZ >> 4, 0x5041524BL),
                                        seed, style));
                    continue;
                }
                for (int a = 0; a < pick.cx; a++)
                {
                    for (int b = 0; b < pick.cz; b++)
                    {
                        used[i + a][j + b] = true;
                    }
                }
                LcContent.Shape s = LcContent.shape(pick.file);
                if (s == null)
                {
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g, null, seed, style));
                    continue;
                }
                out.add(new BuildingPlan("lcb_" + cellX + "_" + cellZ + "_" + i + "_" + j, pick, s, minX, minZ,
                                         g - pick.groundY, zone, style, g));
            }
        }
        cache.put(key, out);
        return out;
    }

    /**
     * The city refuge of a city region (docs/safezone.md): {cell x, cell z, turns}, or null. Old layout:
     * every city region has one. City sizes (CityLayout): only the regions refugeSectors picks.
     */
    public static int[] refuge(long seed, int rx, int rz)
    {
        if (!Highways.cityRegion(seed, rx, rz) || Wasteland.sectors() && Wasteland.sector(seed, rx, rz))
        {
            return null;                                  // no safezone in a wasteland city
        }
        if (CityLayout.on() && !refugeSectors(seed, rx, rz).contains(CityLayout.key(rx, rz)))
        {
            return null;
        }
        return refugeBlock(seed, rx, rz);
    }

    private static final Map<Long, Set<Long>> REFUGES = new ConcurrentHashMap<Long, Set<Long>>();
    private static volatile long refugesSeed;
    private static volatile int refugesStamp;

    /**
     * The regions of a city (CityLayout) that hold a refuge (user, 13 Oktober 2026: "1 per 4 sectors"):
     * round(size / 4), at least 1, never a wasteland sector, spread apart: the first nearest the city's
     * middle, each next one farthest from those taken. A region without a block that qualifies is skipped
     * for the next candidate.
     */
    static Set<Long> refugeSectors(long seed, int rx, int rz)
    {
        int stamp = CityLayout.epoch() * 31 + plansEpoch;
        if (seed != refugesSeed || stamp != refugesStamp || REFUGES.size() > 4096)
        {
            REFUGES.clear();
            refugesSeed = seed;
            refugesStamp = stamp;
        }
        long cell = CityLayout.key(Math.floorDiv(rx, CityLayout.CELL), Math.floorDiv(rz, CityLayout.CELL));
        Set<Long> got = REFUGES.get(cell);
        if (got == null)
        {
            got = pickRefuges(seed, CityLayout.planAt(seed, rx, rz));
            REFUGES.put(cell, got);
        }
        return got;
    }

    private static Set<Long> pickRefuges(long seed, CityLayout.Plan p)
    {
        Set<Long> out = new java.util.HashSet<Long>();
        List<int[]> left = new ArrayList<int[]>();
        double mx = 0, mz = 0;
        for (long k : p.city)
        {
            int x = (int) (k >> 32), z = (int) k;
            mx += x;
            mz += z;
            if (!p.waste.contains(k))
            {
                left.add(new int[] {x, z});
            }
        }
        if (p.city.isEmpty())
        {
            return out;
        }
        mx /= p.city.size();
        mz /= p.city.size();
        int want = Math.max(1, Math.round(p.size / 4f));
        List<int[]> taken = new ArrayList<int[]>();
        while (taken.size() < want && !left.isEmpty())
        {
            int[] best = null;
            double bestScore = 0;
            long bestTie = 0;
            for (int[] c : left)
            {
                double score;
                if (taken.isEmpty())
                {
                    score = -((c[0] - mx) * (c[0] - mx) + (c[1] - mz) * (c[1] - mz));
                }
                else
                {
                    score = Double.MAX_VALUE;
                    for (int[] t : taken)
                    {
                        score = Math.min(score, (c[0] - t[0]) * (c[0] - t[0]) + (c[1] - t[1]) * (c[1] - t[1]));
                    }
                }
                long tie = hl(seed ^ 0x52454648L, c[0], c[1]);
                if (best == null || score > bestScore || score == bestScore && tie < bestTie)
                {
                    best = c;
                    bestScore = score;
                    bestTie = tie;
                }
            }
            left.remove(best);
            if (refugeBlock(seed, best[0], best[1]) != null)
            {
                taken.add(best);
                out.add(CityLayout.key(best[0], best[1]));
            }
        }
        return out;
    }

    /**
     * The refuge block of a city region: {cell x, cell z, turns} of the building block nearest the
     * region's middle that is a plain block (no superblock) with no river chunk; turns put the gate
     * (canvas south) on a street at the block's own ground, toward the region's middle when possible
     * (0 south, 1 west, 2 north, 3 east). Null when no block qualifies.
     */
    private static int[] refugeBlock(long seed, int rx, int rz)
    {
        int cells = Sectors.REGION / CELL;
        List<int[]> order = new ArrayList<int[]>();
        for (int i = 0; i < cells; i++)
        {
            for (int j = 0; j < cells; j++)
            {
                order.add(new int[] {i, j});
            }
        }
        final double mid = cells / 2.0;
        final long s = seed;
        final int bx = rx * cells, bz = rz * cells;
        java.util.Collections.sort(order, new java.util.Comparator<int[]>()
        {
            public int compare(int[] a, int[] b)
            {
                double da = Math.hypot(a[0] + 0.5 - mid, a[1] + 0.5 - mid), db = Math.hypot(b[0] + 0.5 - mid, b[1] + 0.5 - mid);
                if (Math.abs(da - db) > 1e-9)
                {
                    return da < db ? -1 : 1;
                }
                return Long.compare(hl(s ^ 0x52454647L, bx + a[0], bz + a[1]), hl(s ^ 0x52454647L, bx + b[0], bz + b[1]));
            }
        });
        for (int[] o : order)
        {
            int cx = bx + o[0], cz = bz + o[1];
            int[] blk = block(seed, cx, cz);
            if (blk[2] != 1 || !isCity(seed, cx, cz))
            {
                continue;
            }
            boolean river = false;
            for (int i = 1; i < CELL && !river; i++)
            {
                for (int j = 1; j < CELL; j++)
                {
                    if (CityRivers.chunk(seed, cx * CELL + i, cz * CELL + j))
                    {
                        river = true;
                        break;
                    }
                }
            }
            if (river)
            {
                continue;
            }
            // sides with a street at this block's ground: west / north are its own streets, east /
            // south the next cells' (they may sit a level up or down)
            int g = ground(seed, cx, cz);
            boolean[] ok = {isCity(seed, cx, cz + 1) && ground(seed, cx, cz + 1) == g, true, true,
                isCity(seed, cx + 1, cz) && ground(seed, cx + 1, cz) == g};      // south, west, north, east
            boolean[] toward = {o[1] + 0.5 < mid, o[0] + 0.5 > mid, o[1] + 0.5 > mid, o[0] + 0.5 < mid};
            List<Integer> best = new ArrayList<Integer>(), any = new ArrayList<Integer>();
            for (int t = 0; t < 4; t++)
            {
                if (ok[t])
                {
                    any.add(t);
                    if (toward[t])
                    {
                        best.add(t);
                    }
                }
            }
            List<Integer> from = best.isEmpty() ? any : best;
            return new int[] {cx, cz, from.get((int) (hl(seed ^ 0x47415445L, cx, cz) % from.size()))};
        }
        return null;
    }

    /**
     * An airport block (city/Airport, docs/airport.md): the block's first chunk column and row are streets as in
     * any block, the edge ramps of its cells as usual, the rest the airport (military/AirportPlan), its gate
     * toward the street (turns 0: runway along z, gate north; 3: along x, gate west).
     */
    private void planAirport(long seed, int[] blk, List<Plan> out)
    {
        int cellX = blk[0], cellZ = blk[1], sx = blk[3] * CELL, sz = blk[4] * CELL;
        int g = ground(seed, cellX, cellZ);
        String style = style(seed, cellX, cellZ);
        for (int lz = 0; lz < sz; lz++)
        {
            out.add(street(seed, cellX, cellZ, 0, lz, cellX * CELL, cellZ * CELL + lz, g, style));
        }
        for (int lx = 1; lx < sx; lx++)
        {
            out.add(street(seed, cellX, cellZ, lx, 0, cellX * CELL + lx, cellZ * CELL, g, style));
        }
        for (int mx = 0; mx < blk[3]; mx++)
        {
            for (int mz = 0; mz < blk[4]; mz++)
            {
                int ccx = cellX + mx, ccz = cellZ + mz;
                boolean[][] city = new boolean[3][3];
                boolean open = false;
                for (int i = 0; i < 3; i++)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        city[i][j] = isCity(seed, ccx + i - 1, ccz + j - 1);
                        open |= !city[i][j];
                    }
                }
                if (open)
                {
                    out.add(new EdgePlan("lce_" + ccx + "_" + ccz, ccx, ccz, g, city));
                }
            }
        }
        boolean alongZ = blk[4] > blk[3];
        out.add(new net.decimation.worldgen.military.AirportPlan("lca_" + cellX + "_" + cellZ,
            seed ^ hl(seed ^ 0x41495250L, cellX, cellZ), (cellX * CELL + 1) * 16, (cellZ * CELL + 1) * 16, alongZ ? 0 : 3, g));
    }

    /** Weighted pick of a building whose chunks all fit free at (i, j) in the block. */
    static LcContent.Building pick(List<LcContent.Building> pool, String style, boolean[][] used,
                                           int i, int j, Random r)
    {
        List<LcContent.Building> fit = new ArrayList<LcContent.Building>();
        int total = 0;
        for (LcContent.Building b : pool)
        {
            if (i + b.cx > used.length || j + b.cz > used.length)
            {
                continue;
            }
            boolean free = true;
            for (int a = 0; a < b.cx && free; a++)
            {
                for (int c = 0; c < b.cz; c++)
                {
                    if (used[i + a][j + c])
                    {
                        free = false;
                        break;
                    }
                }
            }
            if (free)
            {
                fit.add(b);
                total += Wasteland.weight(b, style);
            }
        }
        if (fit.isEmpty())
        {
            return null;
        }
        int t = r.nextInt(total);
        for (LcContent.Building b : fit)
        {
            t -= Wasteland.weight(b, style);
            if (t < 0)
            {
                return b;
            }
        }
        return fit.get(fit.size() - 1);
    }

    /**
     * A street chunk at the cell's ground. Where the street continues into
     * a neighbouring cell that is one level higher, a stairs part of the
     * district goes into this chunk, its high side toward that neighbour.
     */
    private Plan street(long seed, int cellX, int cellZ, int lx, int lz, int chx, int chz, int g, String style)
    {
        // a street on the river: the same street part as on land, carried over the canal on a deck
        boolean river = CityRivers.chunk(seed, chx, chz);
        // neighbours along the street grid: the street continues west / north
        // across the cell edge (and east / south from the cell's last street chunk)
        int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}}; // west, north, east, south
        int turns = -1;
        for (int d = 0; d < 4 && !river; d++)
        {
            int nx = chx + dirs[d][0], nz = chz + dirs[d][1];
            int ncx = Math.floorDiv(nx, CELL), ncz = Math.floorDiv(nz, CELL);
            int[] nb = block(seed, ncx, ncz);
            if ((nb[0] == cellX && nb[1] == cellZ) || !isStreetChunk(seed, nx, nz) || !isCity(seed, ncx, ncz))
            {
                continue;
            }
            if (ground(seed, ncx, ncz) == g + LEVEL)
            {
                turns = d; // the part's high side is west (x min); turn it toward d
                break;
            }
        }
        LcContent.Shape stairs = null;
        if (turns >= 0)
        {
            List<String> files = LcContent.stairs(style);
            if (files.isEmpty())
            {
                for (String k : new String[] {"legacy:standardcity", "dc:suburb_residential"})
                {
                    if (!LcContent.stairs(k).isEmpty())
                    {
                        files = LcContent.stairs(k);
                        break;
                    }
                }
            }
            if (!files.isEmpty())
            {
                stairs = LcContent.shape(files.get((int) (h(seed, chx, chz) * files.size())));
            }
        }
        // 0 = north-south street (cell column 0), 1 = east-west (row 0), 2 = crossing
        int kind = lx == 0 && lz == 0 ? 2 : lx == 0 ? 0 : 1;
        // the district's own street part (Lost Cities street parts), chosen by connections
        LcContent.Shape piece = null;
        int pieceTurns = 0;
        if (stairs == null && !LcContent.streets(style, "straight").isEmpty())
        {
            boolean[] c = new boolean[4]; // west, north, east, south
            int n = 0;
            for (int d = 0; d < 4; d++)
            {
                int nx = chx + dirs[d][0], nz = chz + dirs[d][1];
                int ncx = Math.floorDiv(nx, CELL), ncz = Math.floorDiv(nz, CELL);
                c[d] = Highways.at(seed, nx, nz) ? g == Highways.DECK
                    : isCity(seed, ncx, ncz) && isStreetChunk(seed, nx, nz) && ground(seed, ncx, ncz) == g;
                n += c[d] ? 1 : 0;
            }
            // turns as Lost Cities' generateNormalStreetSection (its ROTATE_90 = one clockwise turn)
            String pk;
            if (n == 0)
            {
                pk = "none";
            }
            else if (n == 1)
            {
                pk = "end";
                pieceTurns = c[0] ? 0 : c[2] ? 2 : c[1] ? 1 : 3;
            }
            else if (n == 2 && (c[0] == c[2]))
            {
                pk = "straight";
                pieceTurns = c[0] ? 0 : 1;
            }
            else if (n == 2)
            {
                pk = "bend";
                pieceTurns = c[0] && c[1] ? 0 : c[0] ? 3 : c[1] ? 1 : 2;
            }
            else if (n == 3)
            {
                pk = "t";
                pieceTurns = !c[0] ? 1 : !c[2] ? 3 : !c[1] ? 2 : 0;
            }
            else
            {
                pk = "all";
            }
            List<String> files = LcContent.streets(style, pk);
            if (!files.isEmpty())
            {
                piece = LcContent.shape(files.get((int) (hl(seed ^ 0x53545250L, chx, chz) % files.size())));
            }
        }
        LcContent.Shape scene = !river && kind != 2 && stairs == null && h(seed ^ 0x5343454EL, chx, chz) < SCENE_CHANCE
            ? decor(seed, style, "fountains", chx, chz, 0x464F554EL) : null;
        if (scene != null)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] lc street scene (%s) at %d,%d,%d", style, chx << 4, g,
                                            chz << 4);
        }
        StreetPlan street = new StreetPlan("lcs_" + chx + "_" + chz, chx << 4, chz << 4, g, road, stairs,
                                           Math.max(0, turns), kind, props, seed, this, style, scene, piece,
                                           pieceTurns, river, turns >= 0);
        if (!river)
        {
            return street;
        }
        // the wasteland district's bridges have lost the middle of their road
        boolean broken = style.endsWith(":deadzone");
        cpw.mods.fml.common.FMLLog.info("[deciworldgen] lc bridge%s%s at %d,%d,%d", kind == 2 ? " crossing" : "",
                                        broken ? " broken" : "", (chx << 4) + 8, g, (chz << 4) + 8);
        return new RiverPlan("lcr_" + chx + "_" + chz, seed, chx, chz, g, street, broken);
    }

    // ------------------------------------------------------------ plans

}
