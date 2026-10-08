package net.decimation.worldgen.city;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.DecimationWorldGen;
import net.decimation.worldgen.Rotation;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Highways between cities (Lost Cities style highway parts from the content
 * pack). Each region row has one highway row of chunks (region start + 0 or
 * 8 chunks: always a city street row, superblocks included); where that row
 * leaves a city region and the next city region east of it lies at most
 * MAX_GAP regions away, every chunk between carries an east west highway.
 * Region columns get north south highways the same way; where both cross,
 * a crossing part. A city region left without any (no city next to it, no
 * straight highway) is joined to its nearest diagonal city by an L of the
 * same lines (link()). Pure function of the seed, like the sector map.
 *
 * The deck is at the city's level 0 street surface (DECK), so a highway
 * runs on flush from the city's street. Per chunk the first slice samples
 * the terrain it can see and stores the kind (StructureData, id
 * "hw_X_Z"): TUNNEL when the median ground is TUNNEL_DEPTH or more above
 * the deck, BRIDGE (deck on pillars) over water or land more than 2 below,
 * OPEN otherwise (cut and filled to the deck).
 */
public final class Highways
{
    static final int DECK = LcCity.BASE;
    /** Most non city regions a highway crosses between two cities. */
    static final int MAX_GAP = 3;
    static final int TUNNEL_DEPTH = 6;
    /** Air kept above an open deck / bridge (trees and hills cut). */
    static final int CLEAR = 20;
    static final int OPEN = 0, BRIDGE = 1, TUNNEL = 2;
    /** Widest side ramp beside an open highway chunk (blocks). */
    static final int SIDE = 8;
    private static final String[] KIND = {"open", "bridge", "tunnel"};

    private final LcCity city;

    Highways(LcCity city)
    {
        this.city = city;
    }

    static boolean cityRegion(long seed, int rx, int rz)
    {
        return Sectors.regionSector(seed, rx, rz) == StructureGenerator.CITY;
    }

    /** The highway chunk row of a region row (chunk z), or column of a region column (chunk x). */
    static int line(long seed, int region, long salt)
    {
        return region * Sectors.REGION + (int) (LcCity.hl(seed ^ salt, region, 0) & 1) * 8;
    }

    /** True when the regions a (exclusive) .. b (exclusive) along a line hold no city and the ends are cities. */
    private static boolean spans(long seed, int r, int other, boolean alongX)
    {
        int lo = r - 1, hi = r + 1;
        while (lo >= r - MAX_GAP && !(alongX ? cityRegion(seed, lo, other) : cityRegion(seed, other, lo)))
        {
            lo--;
        }
        while (hi <= r + MAX_GAP && !(alongX ? cityRegion(seed, hi, other) : cityRegion(seed, other, hi)))
        {
            hi++;
        }
        boolean a = alongX ? cityRegion(seed, lo, other) : cityRegion(seed, other, lo);
        boolean b = alongX ? cityRegion(seed, hi, other) : cityRegion(seed, other, hi);
        return a && b && hi - lo - 1 <= MAX_GAP;
    }

    static final long SALT_X = 0x48575958L, SALT_Z = 0x4857595AL;

    /** Chunk carries an east west highway. */
    public static boolean xHighway(long seed, int cx, int cz)
    {
        int rz = Math.floorDiv(cz, Sectors.REGION), rx = Math.floorDiv(cx, Sectors.REGION);
        return cz == line(seed, rz, SALT_X) && !cityRegion(seed, rx, rz) && spans(seed, rx, rz, true)
            || onLink(seed, cx, cz, true);
    }

    /** Chunk carries a north south highway. */
    public static boolean zHighway(long seed, int cx, int cz)
    {
        int rz = Math.floorDiv(cz, Sectors.REGION), rx = Math.floorDiv(cx, Sectors.REGION);
        return cx == line(seed, rx, SALT_Z) && !cityRegion(seed, rx, rz) && spans(seed, rz, rx, false)
            || onLink(seed, cx, cz, false);
    }

    /** Links per city region ({bx, bz, x first 1 / 0}, or NONE), cached: asked for every column. */
    private static final int[] NONE = new int[0];
    private static final java.util.Map<Long, int[]> LINKS = new java.util.HashMap<Long, int[]>();
    private static long linkSeed;

    /**
     * The L highway leaving city region (rx, rz), or NONE. Only an isolated
     * city gets one (no city region within MAX_GAP + 1 straight east, west,
     * north or south, so neither streets nor a straight highway reach it):
     * to the nearest city at most 2 regions away on both axes, along the
     * region row's x line to the target's z line (x first) or the column's
     * z line to the target's x line, whichever passes only non city regions
     * (x first tried first). Nearest = smallest |dx| + |dz|, first found in
     * the order dx, dz = -2, -1, 1, 2 (tools/hwmap.py does the same).
     */
    static int[] link(long seed, int rx, int rz)
    {
        synchronized (LINKS)
        {
            if (linkSeed != seed || LINKS.size() > 65536)
            {
                LINKS.clear();
                linkSeed = seed;
            }
            Long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
            int[] l = LINKS.get(key);
            if (l == null)
            {
                l = findLink(seed, rx, rz);
                LINKS.put(key, l);
            }
            return l;
        }
    }

    private static int[] findLink(long seed, int ax, int az)
    {
        if (!cityRegion(seed, ax, az))
        {
            return NONE;
        }
        for (int d = 1; d <= MAX_GAP + 1; d++)
        {
            if (cityRegion(seed, ax + d, az) || cityRegion(seed, ax - d, az)
                || cityRegion(seed, ax, az + d) || cityRegion(seed, ax, az - d))
            {
                return NONE;
            }
        }
        int[] best = NONE;
        int bestD = Integer.MAX_VALUE;
        int[] steps = {-2, -1, 1, 2};
        for (int dx : steps)
        {
            for (int dz : steps)
            {
                int bx = ax + dx, bz = az + dz;
                if (Math.abs(dx) + Math.abs(dz) >= bestD || !cityRegion(seed, bx, bz))
                {
                    continue;
                }
                int first = clear(seed, ax, az, bx, bz, true) ? 1 : clear(seed, ax, az, bx, bz, false) ? 0 : -1;
                if (first >= 0)
                {
                    best = new int[] {bx, bz, first};
                    bestD = Math.abs(dx) + Math.abs(dz);
                }
            }
        }
        return best;
    }

    /** True when every region the L from a to b passes (ends excluded) holds no city. */
    private static boolean clear(long seed, int ax, int az, int bx, int bz, boolean xFirst)
    {
        int sx = bx > ax ? 1 : -1, sz = bz > az ? 1 : -1;
        int kx = xFirst ? bx : ax, kz = xFirst ? az : bz; // the corner region
        for (int x = ax; x != bx + sx; x += sx)
        {
            int z = xFirst ? az : bz;
            if (!(x == ax && z == az) && !(x == bx && z == bz) && cityRegion(seed, x, z))
            {
                return false;
            }
        }
        for (int z = az; z != bz + sz; z += sz)
        {
            int x = xFirst ? bx : ax;
            if (!(x == ax && z == az) && !(x == bx && z == bz) && cityRegion(seed, x, z))
            {
                return false;
            }
        }
        return !cityRegion(seed, kx, kz);
    }

    /** True when an L link runs through the chunk along x (alongX) or along z. */
    private static boolean onLink(long seed, int cx, int cz, boolean alongX)
    {
        int rx = Math.floorDiv(cx, Sectors.REGION), rz = Math.floorDiv(cz, Sectors.REGION);
        for (int ax = rx - 2; ax <= rx + 2; ax++)
        {
            for (int az = rz - 2; az <= rz + 2; az++)
            {
                int[] l = link(seed, ax, az);
                if (l.length > 0 && onL(seed, ax, az, l[0], l[1], l[2] == 1, cx, cz, alongX))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** The chunks of one L: from a's edge along its first line to the corner, then to b's edge. */
    static boolean onL(long seed, int ax, int az, int bx, int bz, boolean xFirst, int cx, int cz, boolean alongX)
    {
        int R = Sectors.REGION;
        int row = line(seed, xFirst ? az : bz, SALT_X), col = line(seed, xFirst ? bx : ax, SALT_Z);
        if (alongX)
        {
            if (cz != row)
            {
                return false;
            }
            // x first: a's edge to the corner; z first: the corner to b's edge
            int lo = xFirst ? (bx > ax ? (ax + 1) * R : col) : (bx > ax ? col : (bx + 1) * R);
            int hi = xFirst ? (bx > ax ? col : ax * R - 1) : (bx > ax ? bx * R - 1 : col);
            return cx >= lo && cx <= hi;
        }
        if (cx != col)
        {
            return false;
        }
        int lo = xFirst ? (bz > az ? row : (bz + 1) * R) : (bz > az ? (az + 1) * R : row);
        int hi = xFirst ? (bz > az ? bz * R - 1 : row) : (bz > az ? row : az * R - 1);
        return cz >= lo && cz <= hi;
    }

    public static boolean at(long seed, int cx, int cz)
    {
        return xHighway(seed, cx, cz) || zHighway(seed, cx, cz);
    }

    /** True when the box comes within 4 blocks of a highway chunk (sites stay off the road). */
    public static boolean near(long seed, int x0, int z0, int x1, int z1)
    {
        for (int cx = (x0 - 4) >> 4; cx <= (x1 + 4) >> 4; cx++)
        {
            for (int cz = (z0 - 4) >> 4; cz <= (z1 + 4) >> 4; cz++)
            {
                if (at(seed, cx, cz))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Writes the highway chunks this population window touches (only their part inside it). */
    void populate(World world, int[] w)
    {
        if (LcContent.highways("open").isEmpty())
        {
            return;
        }
        long seed = world.getSeed();
        for (int cx = w[0] >> 4; cx <= w[2] >> 4; cx++)
        {
            for (int cz = w[1] >> 4; cz <= w[3] >> 4; cz++)
            {
                boolean x = xHighway(seed, cx, cz), z = zHighway(seed, cx, cz);
                if (x || z)
                {
                    place(world, seed, cx, cz, x, z, w);
                }
            }
        }
        sides(world, seed, w);
    }

    /**
     * Land beside open highway chunks: ramps from the deck at the road's
     * edge to the natural height over SIDE blocks (2 per block of height
     * difference), so the road neither sits on a dirt wall nor in a
     * trench. Beside a bridge only land above the deck is cut down (the
     * gap under it stays). Columns inside a city or its edge band are the
     * city's.
     */
    private void sides(World world, long seed, int[] w)
    {
        for (int x = w[0]; x <= w[2]; x++)
        {
            for (int z = w[1]; z <= w[3]; z++)
            {
                int cx = x >> 4, cz = z >> 4;
                if (at(seed, cx, cz))
                {
                    continue;
                }
                // nearest road edge across: a z highway west / east, an x highway north / south
                int best = SIDE + 1, hx = 0, hz = 0;
                int[][] nb = {{-1, 0, (x & 15) + 1}, {1, 0, 16 - (x & 15)}, {0, -1, (z & 15) + 1}, {0, 1, 16 - (z & 15)}};
                for (int[] n : nb)
                {
                    boolean road = n[0] != 0 ? zHighway(seed, cx + n[0], cz) : xHighway(seed, cx, cz + n[1]);
                    if (road && n[2] < best)
                    {
                        best = n[2];
                        hx = cx + n[0];
                        hz = cz + n[1];
                    }
                }
                if (best > SIDE || LcCity.nearCity(seed, x, z, x, z))
                {
                    continue;
                }
                int k = kind(world, seed, hx, hz);
                if (k == TUNNEL)
                {
                    continue;
                }
                int natural = StructureGenerator.soilTop(world, x, z);
                if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z)
                    || k == BRIDGE && natural <= DECK)
                {
                    continue;
                }
                int width = Math.max(3, Math.min(SIDE, 2 * Math.abs(natural - DECK) + 1));
                double t = best / (double) (width + 1);
                if (t >= 1)
                {
                    continue;
                }
                t = t * t * (3 - 2 * t);
                LcCity.reshape(world, x, z, natural, DECK + (int) Math.round((natural - DECK) * t));
            }
        }
    }

    /** The stored kind of a highway chunk, decided now when no slice has done it yet. */
    private static int kind(World world, long seed, int cx, int cz)
    {
        String id = "hw_" + cx + "_" + cz;
        StructureData data = StructureData.get(world);
        Integer k = data.baseY(id);
        if (k == null)
        {
            k = decide(world, seed, cx, cz);
            data.setBaseY(id, k);
            log(k, xHighway(seed, cx, cz), zHighway(seed, cx, cz), cx, cz);
        }
        return k;
    }

    private static void log(int kind, boolean xh, boolean zh, int cx, int cz)
    {
        FMLLog.info("[%s] highway %s%s %s at %d,%d,%d", DecimationWorldGen.MODID, KIND[kind],
                    xh && zh ? " crossing" : "", xh ? "x" : "z", cx << 4, DECK, cz << 4);
    }

    private void place(World world, long seed, int cx, int cz, boolean xh, boolean zh, int[] w)
    {
        int kind = kind(world, seed, cx, cz);
        boolean bi = xh && zh;
        java.util.List<String> files = LcContent.highways(KIND[kind] + (bi ? "_bi" : ""));
        if (files.isEmpty())
        {
            files = LcContent.highways(bi ? "open_bi" : "open");
        }
        long hv = LcCity.hl(seed ^ 0x48574159L, cx, cz);
        LcContent.Shape part = LcContent.shape(files.get((int) (hv % files.size())));
        if (part == null)
        {
            return;
        }
        int turns = xh ? 0 : 1; // parts run along x unturned
        int x0 = Math.max(w[0], cx << 4), x1 = Math.min(w[2], (cx << 4) + 15);
        int z0 = Math.max(w[1], cz << 4), z1 = Math.min(w[3], (cz << 4) + 15);
        int base = DECK - 1; // part slice 0 is the deck's underside, slice 1 the road
        int[] meta = new int[1];
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                int lx = x - (cx << 4), lz = z - (cz << 4);
                // part position (inverse of a clockwise turn, as LcCity's stairs)
                int px = turns == 1 ? lz : lx, pz = turns == 1 ? 15 - lx : lz;
                int top = kind == TUNNEL ? base + part.height - 1 : DECK + CLEAR;
                for (int y = base; y <= top; y++)
                {
                    Block b = null;
                    meta[0] = 0;
                    int ly = y - base;
                    if (ly < part.height && px < part.width && pz < part.length)
                    {
                        int i = part.index(px, ly, pz);
                        if (part.skip[i])
                        {
                            continue;
                        }
                        b = part.blocks[i];
                        if (b != null)
                        {
                            meta[0] = Rotation.rotateMeta(Block.getIdFromBlock(b), part.meta[i] & 15, turns);
                        }
                    }
                    if (b == null && y == DECK + 1 && kind != TUNNEL)
                    {
                        b = wreck(hv, lx, lz, xh, meta);
                    }
                    world.setBlock(x, y, z, b != null ? b : Blocks.air, b != null ? meta[0] : 0, 2);
                }
                below(world, x, z, base - 1, kind, px, pz);
            }
        }
        if (bi && kind != TUNNEL)
        {
            hedges(world, seed, cx, cz, x0, z0, x1, z1);
        }
    }

    /**
     * A crossing part has no rails; where it is an L's corner (or a line's
     * end) some sides face open land: a leaf hedge on the deck there, like
     * the hedge rows of the straight parts.
     */
    private static void hedges(World world, long seed, int cx, int cz, int x0, int z0, int x1, int z1)
    {
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : sides)
        {
            int nx = cx + d[0], nz = cz + d[1];
            if (at(seed, nx, nz) || cityRegion(seed, Math.floorDiv(nx, Sectors.REGION), Math.floorDiv(nz, Sectors.REGION)))
            {
                continue;
            }
            for (int i = 2; i <= 13; i++)
            {
                int lx = d[0] == 0 ? i : d[0] < 0 ? 1 : 14, lz = d[1] == 0 ? i : d[1] < 0 ? 1 : 14;
                int x = (cx << 4) + lx, z = (cz << 4) + lz;
                if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && world.isAirBlock(x, DECK + 1, z))
                {
                    world.setBlock(x, DECK + 1, z, Blocks.leaves, 4, 2);
                }
            }
        }
    }

    /** Under the deck: open ground filled to it, bridge pillars down to the ground. */
    private static void below(World world, int x, int z, int y, int kind, int px, int pz)
    {
        if (kind == TUNNEL)
        {
            return;
        }
        boolean pillar = px <= 1 && (pz == 2 || pz == 3 || pz == 12 || pz == 13);
        if (kind == BRIDGE && !pillar)
        {
            return;
        }
        Block fill = kind == BRIDGE ? Blocks.stonebrick : Blocks.dirt;
        for (int n = 0; y > 4 && n < 80; y--, n++)
        {
            Block b = world.getBlock(x, y, z);
            if (!StructureGenerator.clearable(b) && !b.getMaterial().isLiquid())
            {
                break;
            }
            world.setBlock(x, y, z, fill, 0, 2);
        }
    }

    /** A wreck now and then in a lane (8% per lane per chunk), parked along the road. */
    private Block wreck(long hv, int lx, int lz, boolean xh, int[] meta)
    {
        Block[] cars = city.props.cars;
        int across = xh ? lz : lx, along = xh ? lx : lz;
        if (cars.length == 0 || along != 7 || (across != 4 && across != 11))
        {
            return null;
        }
        long r = hv >>> (across == 4 ? 8 : 24);
        if ((r & 0xFF) >= 20)
        {
            return null;
        }
        // east west roads 4 / 2, north south 5 / 3 (CLAUDE.md, v0.12.2)
        meta[0] = xh ? (across == 4 ? 4 : 2) : (across == 4 ? 5 : 3);
        return cars[(int) ((r >>> 8) % cars.length)];
    }

    /**
     * Terrain under one chunk, from what is loaded: TUNNEL, BRIDGE or OPEN.
     * Never a tunnel within a city's edge band: the band grades the hill
     * away, which left a short free standing underpass.
     */
    private static int decide(World world, long seed, int cx, int cz)
    {
        int[] ys = new int[25];
        int n = 0;
        boolean water = false;
        for (int i = 0; i < 5; i++)
        {
            for (int k = 0; k < 5; k++)
            {
                int x = (cx << 4) + i * 15 / 4, z = (cz << 4) + k * 15 / 4;
                if (!world.blockExists(x, 64, z))
                {
                    continue;
                }
                int y = StructureGenerator.soilTop(world, x, z);
                water |= StructureGenerator.waterAbove(world, x, y, z);
                ys[n++] = y;
            }
        }
        if (n == 0)
        {
            return OPEN;
        }
        java.util.Arrays.sort(ys, 0, n);
        if (ys[n / 2] >= DECK + TUNNEL_DEPTH && !LcCity.nearCity(seed, cx << 4, cz << 4, (cx << 4) + 15, (cz << 4) + 15))
        {
            return TUNNEL;
        }
        if (water || ys[0] < DECK - 2)
        {
            return BRIDGE;
        }
        return OPEN;
    }
}
