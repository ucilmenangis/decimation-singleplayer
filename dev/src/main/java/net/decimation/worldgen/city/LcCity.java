package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private static final int TOP = 250;

    private final Block road;
    /** Street furniture (same blocks and facing rules as StructureGenerator's streets). */
    public static final class StreetProps
    {
        public Block lamp, bench, bin, centreLine, sidewalk = Blocks.double_stone_slab;
        public Block[] trashBags = new Block[0], cars = new Block[0];
    }

    StreetProps props = new StreetProps();
    final Highways highways = new Highways(this);

    public void setStreetProps(StreetProps p)
    {
        props = p;
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
        return b;
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

    static boolean isCity(long seed, int cellX, int cellZ)
    {
        return Sectors.sector(seed, cellX * CELL, cellZ * CELL) == StructureGenerator.CITY;
    }

    /** City level of a cell: smooth noise over a 3 cell lattice, 0 at the city's edge, rising inward. */
    public static int level(long seed, int cellX, int cellZ)
    {
        // distance (in cells, up to MAX_LEVEL) to the nearest non city cell
        int edge = MAX_LEVEL;
        for (int r = 1; r <= MAX_LEVEL && edge == MAX_LEVEL; r++)
        {
            for (int dx = -r; dx <= r && edge == MAX_LEVEL; dx++)
            {
                for (int dz = -r; dz <= r; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r && !isCity(seed, cellX + dx, cellZ + dz))
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

    private static double lerp(double a, double b, double t)
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

    private static double h(long seed, int x, int z)
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
        List<String> styles = new ArrayList<String>();
        for (LcContent.Building b : LcContent.buildings())
        {
            for (String s : b.styles.keySet())
            {
                if (!styles.contains(s))
                {
                    styles.add(s);
                }
            }
        }
        java.util.Collections.sort(styles);
        if (styles.isEmpty())
        {
            return "";
        }
        String dead = styles.contains("legacy:deadzone") ? "legacy:deadzone" : null;
        if (dead != null)
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

    /**
     * The block a cell belongs to: {origin cell x, origin cell z, size in
     * cells}. Aligned 2 x 2 cell groups become one SUPERBLOCK (the inner
     * streets dropped, 7 x 7 building chunks) with SUPER_CHANCE when all 4
     * cells are city: room for the big multi buildings (towers, school).
     */
    public static int[] block(long seed, int cellX, int cellZ)
    {
        int ox = Math.floorDiv(cellX, 2) * 2, oz = Math.floorDiv(cellZ, 2) * 2;
        if (h(seed ^ 0x5355504552L, ox, oz) < SUPER_CHANCE && isCity(seed, ox, oz) && isCity(seed, ox + 1, oz)
            && isCity(seed, ox, oz + 1) && isCity(seed, ox + 1, oz + 1))
        {
            return new int[] {ox, oz, 2};
        }
        return new int[] {cellX, cellZ, 1};
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
        if (b[2] == 2)
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
        long key = ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
        List<Plan> out = cache.get(key);
        if (out != null)
        {
            return out;
        }
        out = new ArrayList<Plan>();
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
        net.decimation.mod.server.zones.a zone = style.endsWith(":deadzone")
            ? net.decimation.mod.server.zones.a.MILITARY : net.decimation.mod.server.zones.a.POLICE;
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
                LcContent.Building pick = from == pool && r.nextFloat() < LOT_CHANCE ? null
                    : pick(from, style, used, i, j, r);
                int minX = bx + i * 16, minZ = bz + j * 16;
                if (pick == null)
                {
                    used[i][j] = true;
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g,
                                        decor(seed, style, "parks", minX >> 4, minZ >> 4, 0x5041524BL)));
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
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g, null));
                    continue;
                }
                out.add(new BuildingPlan("lcb_" + cellX + "_" + cellZ + "_" + i + "_" + j, pick, s, minX, minZ,
                                         g - pick.groundY, zone, style, g));
            }
        }
        cache.put(key, out);
        return out;
    }

    /** Weighted pick of a building whose chunks all fit free at (i, j) in the block. */
    private static LcContent.Building pick(List<LcContent.Building> pool, String style, boolean[][] used,
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
                total += b.styles.get(style);
            }
        }
        if (fit.isEmpty())
        {
            return null;
        }
        int t = r.nextInt(total);
        for (LcContent.Building b : fit)
        {
            t -= b.styles.get(style);
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
        // neighbours along the street grid: the street continues west / north
        // across the cell edge (and east / south from the cell's last street chunk)
        int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}}; // west, north, east, south
        int turns = -1;
        for (int d = 0; d < 4; d++)
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
        LcContent.Shape scene = kind != 2 && stairs == null && h(seed ^ 0x5343454EL, chx, chz) < SCENE_CHANCE
            ? decor(seed, style, "fountains", chx, chz, 0x464F554EL) : null;
        if (scene != null)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] lc street scene (%s) at %d,%d,%d", style, chx << 4, g,
                                            chz << 4);
        }
        return new StreetPlan("lcs_" + chx + "_" + chz, chx << 4, chz << 4, g, road, stairs, Math.max(0, turns),
                              kind, props, seed, this, style, scene);
    }

    // ------------------------------------------------------------ plans

    /** A converted building: its ground floor layer at the city ground. */
    static final class BuildingPlan implements net.decimation.worldgen.FixedBase
    {
        private final String id;
        private final LcContent.Building b;
        private final LcContent.Shape s;
        private final int minX, minZ, baseY;
        private final net.decimation.mod.server.zones.a zone;
        /** District style and street level of the block (fronts of the street beside it). */
        final String style;
        final int ground;

        BuildingPlan(String id, LcContent.Building b, LcContent.Shape s, int minX, int minZ, int baseY,
                     net.decimation.mod.server.zones.a zone, String style, int ground)
        {
            this.style = style;
            this.ground = ground;
            this.id = id;
            this.b = b;
            this.s = s;
            this.minX = minX;
            this.minZ = minZ;
            this.baseY = baseY;
            this.zone = zone;
        }

        public int fixedBaseY() { return baseY; }
        public String id() { return id; }
        public int minX() { return minX; }
        public int minZ() { return minZ; }
        public int maxX() { return minX + s.width - 1; }
        public int maxZ() { return minZ + s.length - 1; }
        public int height() { return s.height - 1; }
        public int clearAbove() { return 12; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.stone; }
        public net.decimation.mod.server.zones.a zone() { return zone; }

        public String describe()
        {
            return "lc " + b.pack + "/" + b.name + " (" + b.cx + "x" + b.cz + " chunks, " + s.height + " high, cellars "
                + b.groundY + ")";
        }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = 0;
            if (lx < 0 || lz < 0 || lx >= s.width || lz >= s.length || ly < 0 || ly >= s.height)
            {
                meta[0] = SKIP;
                return null;
            }
            int i = s.index(lx, ly, lz);
            if (s.skip[i])
            {
                meta[0] = SKIP;
                return null;
            }
            meta[0] = s.meta[i];
            return s.blocks[i];
        }
    }

    /** A street chunk: road at the ground, optional stairs part above it. */
    static final class StreetPlan implements net.decimation.worldgen.FixedBase
    {
        private final String id;
        private final int minX, minZ, ground, turns;
        private final Block road;
        private final LcContent.Shape stairs;

        private final int kind;
        private final StreetProps props;
        private final long seed;
        private final LcCity city;
        private final String style;
        /** Street scene in the road (turned along it), or null. */
        private final LcContent.Shape scene;
        /** Front parts per side (west, north, east, south: turns 0..3), resolved on first use. */
        private LcContent.Shape[] fronts;

        StreetPlan(String id, int minX, int minZ, int ground, Block road, LcContent.Shape stairs, int turns,
                   int kind, StreetProps props, long seed, LcCity city, String style, LcContent.Shape scene)
        {
            this.city = city;
            this.style = style;
            this.scene = scene;
            this.kind = kind;
            this.props = props;
            this.seed = seed;
            this.id = id;
            this.minX = minX;
            this.minZ = minZ;
            this.ground = ground;
            this.road = road;
            this.stairs = stairs;
            this.turns = turns;
        }

        public int fixedBaseY() { return ground; }
        public String id() { return id; }
        public int minX() { return minX; }
        public int minZ() { return minZ; }
        public int maxX() { return minX + 15; }
        public int maxZ() { return minZ + 15; }
        public int height() { return stairs != null ? stairs.height + 1 : 13; }
        public int clearAbove() { return 14; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.stone; }
        public net.decimation.mod.server.zones.a zone() { return null; }
        public String describe() { return null; }

        /**
         * Fronts toward the buildings beside a straight street: the
         * neighbour chunk's building (looked up in its block's plans, so
         * the other side of the street works too) at this street's level
         * gets one of its district's fronts half the time. Resolved at
         * write time, when no block plan is being built (no recursion).
         */
        private LcContent.Shape[] fronts()
        {
            if (fronts != null)
            {
                return fronts;
            }
            fronts = new LcContent.Shape[4];
            if (kind == 2 || stairs != null)
            {
                return fronts;
            }
            int chx = minX >> 4, chz = minZ >> 4;
            int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}}; // west, north, east, south
            for (int d = 0; d < 4; d++)
            {
                if ((kind == 0) != (dirs[d][0] != 0))
                {
                    continue; // only the sides along the street
                }
                int nx = chx + dirs[d][0], nz = chz + dirs[d][1];
                int ncx = Math.floorDiv(nx, CELL), ncz = Math.floorDiv(nz, CELL);
                if (!isCity(seed, ncx, ncz) || hl(seed ^ 0x46524F4EL, nx * 4 + d, nz) % 1000 >= FRONT_CHANCE * 1000)
                {
                    continue;
                }
                int[] nb = block(seed, ncx, ncz);
                for (Plan p : city.plan(seed, nb[0], nb[1]))
                {
                    if (p instanceof BuildingPlan && p.minX() <= nx * 16 && p.maxX() >= nx * 16 + 15
                        && p.minZ() <= nz * 16 && p.maxZ() >= nz * 16 + 15)
                    {
                        BuildingPlan b = (BuildingPlan) p;
                        if (b.ground == ground)
                        {
                            fronts[d] = decor(seed, b.style, "fronts", nx, nz, 0x46524EL + d);
                        }
                        break;
                    }
                }
            }
            return fronts;
        }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = 0;
            // across = position across the street (0..15), along = along it
            int across = kind == 1 ? lz : lx, along = kind == 1 ? lx : lz;
            boolean sidewalk = kind == 2 ? (lx < SIDEWALK || lx > 15 - SIDEWALK) && (lz < SIDEWALK || lz > 15 - SIDEWALK)
                : across < SIDEWALK || across > 15 - SIDEWALK;
            if (ly == 0)
            {
                if (sidewalk)
                {
                    return props.sidewalk;
                }
                // dashed centre line (2 on, 2 off) on straight streets
                if (kind != 2 && props.centreLine != null && across == 7 && (along & 3) < 2)
                {
                    meta[0] = kind == 0 ? 4 : 2;
                    return props.centreLine;
                }
                return road;
            }
            if (stairs != null)
            {
                return stairsAt(lx, ly, lz, meta);
            }
            if (scene != null)
            {
                Block b = partAt(scene, kind == 1 ? 1 : 0, lx, ly - 1, lz, meta);
                if (b != null)
                {
                    return b;
                }
            }
            LcContent.Shape[] f = fronts();
            for (int d = 0; d < 4; d++)
            {
                if (f[d] != null)
                {
                    Block b = partAt(f[d], d, lx, ly - 1, lz, meta);
                    if (b != null)
                    {
                        return b;
                    }
                }
            }
            meta[0] = 0;
            if (ly != 1 || kind == 2)
            {
                return null;
            }
            Block b = furniture(across, along, sidewalk, meta);
            return scene != null && b != null && contains(props.cars, b) ? null : b;
        }

        /** Lamps on the road edge of each sidewalk, benches / bins / bags inside, a wreck now and then. */
        private Block furniture(int across, int along, boolean sidewalk, int[] meta)
        {
            int wx = minX + (kind == 1 ? along : across), wz = minZ + (kind == 1 ? across : along);
            long hv = hl(seed ^ 0x5354524545L, wx, wz); // all 64 bits random (low bits drive the chances)
            boolean west = across < SIDEWALK; // west / north sidewalk; the road lies toward +across
            // toward the road: across is x for kind 0 (east 2 / west 4), z for kind 1 (south 3 / north 5)
            int towardRoad = kind == 0 ? (west ? 2 : 4) : (west ? 3 : 5);
            if (sidewalk)
            {
                boolean roadEdge = across == SIDEWALK - 1 || across == 16 - SIDEWALK;
                if (roadEdge && along == (west ? 4 : 12) && props.lamp != null && (hv & 3) != 0)
                {
                    meta[0] = towardRoad;
                    return props.lamp;
                }
                if (across == (west ? 1 : 14))
                {
                    if (along == (west ? 10 : 2) && props.bench != null && (hv & 0xFF) < 110)
                    {
                        meta[0] = towardRoad;
                        return props.bench;
                    }
                    if (along == (west ? 1 : 9) && props.bin != null && (hv & 0xFF) < 102)
                    {
                        meta[0] = 2 + (int) ((hv >>> 8) & 3);
                        return props.bin;
                    }
                }
                if (props.trashBags.length > 0 && (hv & 0xFF) < 6)
                {
                    meta[0] = 2 + (int) ((hv >>> 16) & 3);
                    return props.trashBags[(int) ((hv >>> 8) % props.trashBags.length)];
                }
                return null;
            }
            // wrecks: lanes 5 and 10, one slot per chunk, 7% each
            if ((across == 5 || across == 10) && along == 7 && props.cars.length > 0 && (hv & 0xFF) < 18)
            {
                meta[0] = kind == 0 ? (across == 5 ? 5 : 3) : (across == 5 ? 4 : 2);
                return props.cars[(int) ((hv >>> 8) % props.cars.length)];
            }
            return null;
        }

        private static boolean contains(Block[] l, Block b)
        {
            for (Block x : l)
            {
                if (x == b)
                {
                    return true;
                }
            }
            return false;
        }

        private Block stairsAt(int lx, int ly, int lz, int[] meta)
        {
            if (ly - 1 >= stairs.height)
            {
                return null;
            }
            // world local -> part position (inverse of a clockwise turn, as SchematicPlan)
            int x, z;
            switch (turns)
            {
                case 1:  x = lz; z = stairs.length - 1 - lx; break;
                case 2:  x = stairs.width - 1 - lx; z = stairs.length - 1 - lz; break;
                case 3:  x = stairs.width - 1 - lz; z = lx; break;
                default: x = lx; z = lz; break;
            }
            if (x < 0 || z < 0 || x >= stairs.width || z >= stairs.length)
            {
                return null;
            }
            int i = stairs.index(x, ly - 1, z);
            Block b = stairs.blocks[i];
            if (b == null)
            {
                return null;
            }
            meta[0] = net.decimation.worldgen.Rotation.rotateMeta(Block.getIdFromBlock(b), stairs.meta[i] & 15, turns);
            return b;
        }
    }

    /**
     * The land just outside a city cell: ramps from the city ground at the
     * cell edge to the natural height (smoothstep over a band 2 blocks per
     * block of height difference, 6 .. EDGE wide), cutting or filling,
     * keeping the column's own surface block. Distance is Euclidean, so
     * outer corners are rounded. A column belongs to the nearest city cell
     * only (ties: lowest cell), so no column is graded twice. Writes no
     * blocks of its own (blockAt is SKIP everywhere).
     */
    static final class EdgePlan implements net.decimation.worldgen.FixedBase, net.decimation.worldgen.Graded
    {
        private final String id;
        private final int cellX, cellZ, ground;
        /** City cells around this one, [dx + 1][dz + 1]. */
        private final boolean[][] city;

        EdgePlan(String id, int cellX, int cellZ, int ground, boolean[][] city)
        {
            this.id = id;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.ground = ground;
            this.city = city;
        }

        public int fixedBaseY() { return ground; }
        public String id() { return id; }
        public int minX() { return cellX * CELL * 16 - EDGE; }
        public int minZ() { return cellZ * CELL * 16 - EDGE; }
        public int maxX() { return (cellX + 1) * CELL * 16 - 1 + EDGE; }
        public int maxZ() { return (cellZ + 1) * CELL * 16 - 1 + EDGE; }
        public int lotMinX() { return minX(); }
        public int lotMinZ() { return minZ(); }
        public int lotMaxX() { return maxX(); }
        public int lotMaxZ() { return maxZ(); }
        public int height() { return 0; }
        public int clearAbove() { return 0; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.dirt; }
        public net.decimation.mod.server.zones.a zone() { return null; }
        public String describe() { return null; }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = SKIP;
            return null;
        }

        /** Squared distance from (x, z) to city cell (cx, cz); 0 inside. */
        private static long dist2(int cx, int cz, int x, int z)
        {
            int x0 = cx * CELL * 16, z0 = cz * CELL * 16;
            long dx = Math.max(0, Math.max(x0 - x, x - (x0 + CELL * 16 - 1)));
            long dz = Math.max(0, Math.max(z0 - z, z - (z0 + CELL * 16 - 1)));
            return dx * dx + dz * dz;
        }

        /** Distance to this cell when it is the column's nearest city cell, else -1. */
        double owner(int x, int z)
        {
            int ox = Math.floorDiv(x, CELL * 16) - cellX + 1, oz = Math.floorDiv(z, CELL * 16) - cellZ + 1;
            if (ox < 0 || ox > 2 || oz < 0 || oz > 2 || city[ox][oz])
            {
                return -1; // inside the city, or out of reach
            }
            long own = dist2(cellX, cellZ, x, z);
            if (own > (long) EDGE * EDGE)
            {
                return -1;
            }
            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    if (!city[i][j] || (i == 1 && j == 1))
                    {
                        continue;
                    }
                    long d = dist2(cellX + i - 1, cellZ + j - 1, x, z);
                    // ties go to the lower cell (x first), the same order every plan uses
                    if (d < own || (d == own && (i < 1 || (i == 1 && j < 1))))
                    {
                        return -1;
                    }
                }
            }
            return Math.sqrt(own);
        }

        /** Smooth value noise in -1 .. 1, 12 block lattice. */
        static double wobble(int x, int z)
        {
            double gx = x / 12.0, gz = z / 12.0;
            int ix = (int) Math.floor(gx), iz = (int) Math.floor(gz);
            double fx = gx - ix, fz = gz - iz;
            fx = fx * fx * (3 - 2 * fx);
            fz = fz * fz * (3 - 2 * fz);
            double a = lattice(ix, iz), b = lattice(ix + 1, iz), c = lattice(ix, iz + 1), e = lattice(ix + 1, iz + 1);
            return (a + (b - a) * fx) * (1 - fz) + (c + (e - c) * fx) * fz;
        }

        private static double lattice(int x, int z)
        {
            long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
            h ^= h >>> 31;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 29;
            return ((h >>> 11) / (double) (1L << 53)) * 2 - 1;
        }

        public void grade(World world, int x, int z, int baseY)
        {
            double d = owner(x, z);
            if (d <= 0)
            {
                return;
            }
            if (Highways.at(world.getSeed(), x >> 4, z >> 4))
            {
                return; // the highway levels its own chunk
            }
            int natural = StructureGenerator.soilTop(world, x, z);
            if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z))
            {
                return;
            }
            int width = Math.max(6, Math.min(EDGE, 2 * Math.abs(natural - ground) + 4));
            // contours wander +-4 blocks so the 1 block steps do not run parallel to the street
            double t = Math.max(0, d + 4 * wobble(x, z)) / (width + 1);
            if (t >= 1)
            {
                return;
            }
            t = t * t * (3 - 2 * t);
            reshape(world, x, z, natural, ground + (int) Math.round((natural - ground) * t));
        }
    }

    /** An empty lot (no building fits or a gap): grass at the ground. */
    static final class LotPlan implements net.decimation.worldgen.FixedBase
    {
        private final String id;
        private final int minX, minZ, ground;
        /** Park part on the grass (layer 1 up), or null for a plain lot. */
        private final LcContent.Shape park;

        LotPlan(String id, int minX, int minZ, int ground, LcContent.Shape park)
        {
            this.park = park;
            this.id = id;
            this.minX = minX;
            this.minZ = minZ;
            this.ground = ground;
        }

        public int fixedBaseY() { return ground; }
        public String id() { return id; }
        public int minX() { return minX; }
        public int minZ() { return minZ; }
        public int maxX() { return minX + 15; }
        public int maxZ() { return minZ + 15; }
        public int height() { return park != null ? park.height + 1 : 2; }
        public int clearAbove() { return 14; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.dirt; }
        public net.decimation.mod.server.zones.a zone() { return null; }
        public String describe() { return park != null ? "lc park" : null; }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = 0;
            if (ly == 0)
            {
                return Blocks.grass;
            }
            if (park != null)
            {
                return partAt(park, 0, lx, ly - 1, lz, meta);
            }
            if (ly == 1 && ((lx * 7 + lz * 13 + minX + minZ) & 15) == 0)
            {
                return Blocks.deadbush;
            }
            return null;
        }
    }
}
