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
    private static final int TOP = 250;

    private final Block road;
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
        int c0x = Math.floorDiv(w[0] >> 4, CELL), c1x = Math.floorDiv(w[2] >> 4, CELL);
        int c0z = Math.floorDiv(w[1] >> 4, CELL), c1z = Math.floorDiv(w[3] >> 4, CELL);
        for (int cx = c0x; cx <= c1x; cx++)
        {
            for (int cz = c0z; cz <= c1z; cz++)
            {
                if (!isCity(world.getSeed(), cx, cz))
                {
                    continue;
                }
                for (Plan p : plan(world.getSeed(), cx, cz))
                {
                    if (Slices.intersects(p, w))
                    {
                        Slices.place(world, p, w);
                    }
                }
            }
        }
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

    private static double h(long seed, int x, int z)
    {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ 0x4C43495459L;
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (v >>> 11) * 0x1.0p-53;
    }

    /** District style ("<pack>:<style>") of a cell: shared by 2 x 2 cell blocks. */
    public static String style(long seed, int cellX, int cellZ)
    {
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
        double v = h(seed ^ 0x5354594C45L, Math.floorDiv(cellX, 2), Math.floorDiv(cellZ, 2));
        return styles.get((int) (v * styles.size()));
    }

    public static int ground(long seed, int cellX, int cellZ)
    {
        return BASE + level(seed, cellX, cellZ) * LEVEL;
    }

    /** Every piece of one cell: 7 street chunks and the buildings / lots of the 3 x 3 block. */
    public synchronized List<Plan> plan(long seed, int cellX, int cellZ)
    {
        long key = ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
        List<Plan> out = cache.get(key);
        if (out != null)
        {
            return out;
        }
        out = new ArrayList<Plan>();
        int g = ground(seed, cellX, cellZ);
        String style = style(seed, cellX, cellZ);
        int bx = cellX * CELL * 16, bz = cellZ * CELL * 16;
        // street chunks: the cell's first column (rows 0..3) and first row (columns 1..3)
        for (int k = 0; k < 2 * CELL - 1; k++)
        {
            int lx = k < CELL ? 0 : k - CELL + 1, lz = k < CELL ? k : 0;
            out.add(street(seed, cellX, cellZ, lx, lz, cellX * CELL + lx, cellZ * CELL + lz, g, style));
        }
        // the 3 x 3 building block
        Random r = new Random(seed ^ (cellX * 341873128712L + cellZ * 132897987541L) ^ 0x4C4342L);
        boolean[][] used = new boolean[CELL][CELL];
        List<LcContent.Building> pool = new ArrayList<LcContent.Building>();
        for (LcContent.Building b : LcContent.buildings())
        {
            if (b.styles.containsKey(style) && b.cx < CELL && b.cz < CELL
                && g - b.groundY >= 4 && g - b.groundY + b.height <= TOP)
            {
                pool.add(b);
            }
        }
        net.decimation.mod.server.zones.a zone = style.endsWith(":deadzone")
            ? net.decimation.mod.server.zones.a.MILITARY : net.decimation.mod.server.zones.a.POLICE;
        for (int i = 1; i < CELL; i++)
        {
            for (int j = 1; j < CELL; j++)
            {
                if (used[i][j])
                {
                    continue;
                }
                LcContent.Building pick = r.nextFloat() < 0.06f ? null : pick(pool, style, used, i, j, r);
                int minX = bx + i * 16, minZ = bz + j * 16;
                if (pick == null)
                {
                    used[i][j] = true;
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g));
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
                    out.add(new LotPlan("lcl_" + cellX + "_" + cellZ + "_" + i + "_" + j, minX, minZ, g));
                    continue;
                }
                out.add(new BuildingPlan("lcb_" + cellX + "_" + cellZ + "_" + i + "_" + j, pick, s, minX, minZ,
                                         g - pick.groundY, zone));
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
            if (i + b.cx > CELL || j + b.cz > CELL)
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
            boolean nStreet = Math.floorMod(nx, CELL) == 0 || Math.floorMod(nz, CELL) == 0;
            if ((ncx == cellX && ncz == cellZ) || !nStreet || !isCity(seed, ncx, ncz))
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
        return new StreetPlan("lcs_" + chx + "_" + chz, chx << 4, chz << 4, g, road, stairs, Math.max(0, turns));
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

        BuildingPlan(String id, LcContent.Building b, LcContent.Shape s, int minX, int minZ, int baseY,
                     net.decimation.mod.server.zones.a zone)
        {
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

        StreetPlan(String id, int minX, int minZ, int ground, Block road, LcContent.Shape stairs, int turns)
        {
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
        public int height() { return stairs != null ? stairs.height + 1 : 3; }
        public int clearAbove() { return 14; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.stone; }
        public net.decimation.mod.server.zones.a zone() { return null; }
        public String describe() { return null; }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = 0;
            if (ly == 0)
            {
                return road;
            }
            if (stairs == null || ly - 1 >= stairs.height)
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

    /** An empty lot (no building fits or a gap): grass at the ground. */
    static final class LotPlan implements net.decimation.worldgen.FixedBase
    {
        private final String id;
        private final int minX, minZ, ground;

        LotPlan(String id, int minX, int minZ, int ground)
        {
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
        public int height() { return 2; }
        public int clearAbove() { return 14; }
        public int maxSpread() { return 255; }
        public Block foundation() { return Blocks.dirt; }
        public net.decimation.mod.server.zones.a zone() { return null; }
        public String describe() { return null; }

        public Block blockAt(int lx, int ly, int lz, int[] meta)
        {
            meta[0] = 0;
            if (ly == 0)
            {
                return Blocks.grass;
            }
            if (ly == 1 && ((lx * 7 + lz * 13 + minX + minZ) & 15) == 0)
            {
                return Blocks.deadbush;
            }
            return null;
        }
    }
}
