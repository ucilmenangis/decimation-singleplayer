package net.decimation.worldgen.military;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * A US style forward base (docs/military_base.md), built into a {@link Canvas}: combat outpost
 * (SMALL), forward operating base (MEDIUM) or large FOB (LARGE). Plan coordinates: x east, z
 * south, y 0 = the ground layer; the gate (entry control point) is on the south side.
 *
 * Rings from outside in: cleared standoff ground with concertina wire, the entry control point
 * on the south (T-wall lane, serpentine of jersey barriers, hedgehogs), the HESCO wall (2 thick,
 * 3 high) with guard towers at the corners and beside the gate, a gravel ring road, then the
 * areas: TOC (north), living area of B-huts with bunkers, latrines and showers (west), motor
 * pool, ammunition supply point and fuel point (east), aid station, dining tent, gym and helipad
 * around the main road. Everything is a pure function of the seed.
 */
public final class MilitaryBase
{
    public static final int SMALL = 0, MEDIUM = 1, LARGE = 2;
    public static final String[] SIZE_NAME = {"combat outpost", "forward operating base", "large FOB"};
    /** Footprint (x, z) per size, including the standoff ring and the gate lane. */
    static final int[][] SIZE = {{50, 58}, {78, 84}, {112, 118}};
    static final int HEIGHT = 20;

    // facing of Decimation props: their FRONT points east 2, south 3, west 4, north 5
    static final int E = 2, S = 3, W = 4, N = 5;

    final Canvas c;
    final int size;
    private final Random r;
    private final B b = B.get();

    /** Camera points for the dev test (canvas coordinates): name -> {x, y, z, yaw, pitch}. */
    final java.util.Map<String, float[]> poi = new java.util.LinkedHashMap<String, float[]>();

    private void poi(String name, double x, double y, double z, float yaw, float pitch)
    {
        if (!poi.containsKey(name))
        {
            poi.put(name, new float[] {(float) x, (float) y, (float) z, yaw, pitch});
        }
    }

    /** Plan cells (x + z * w): 0 free, 1 road / wall (margins may touch it), 2 a module. */
    private byte[] occ;

    /**
     * Takes the rectangle for a module if it and a 1 block margin around it are free and inside
     * the inner area; false (nothing taken) otherwise.
     */
    /** What was placed and refused (dev test). */
    final java.util.List<String> log = new java.util.ArrayList<String>();

    boolean reserve(int x0, int z0, int x1, int z1)
    {
        if (x0 - 1 < ix0 || z0 - 1 < iz0 || x1 + 1 > ix1 || z1 + 1 > iz1)
        {
            return false;
        }
        for (int z = z0 - 1; z <= z1 + 1; z++)
        {
            for (int x = x0 - 1; x <= x1 + 1; x++)
            {
                boolean foot = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                byte o = occ[x + z * c.w];
                if (o == 2 || (foot && o != 0))
                {
                    return false;
                }
            }
        }
        mark(x0, z0, x1, z1, (byte) 2);
        return true;
    }

    /** Marks road / wall ground: modules may border it but not stand on it. */
    private void take(int x0, int z0, int x1, int z1)
    {
        mark(x0, z0, x1, z1, (byte) 1);
    }

    private void mark(int x0, int z0, int x1, int z1, byte v)
    {
        for (int z = Math.max(0, z0); z <= Math.min(c.l - 1, z1); z++)
        {
            for (int x = Math.max(0, x0); x <= Math.min(c.w - 1, x1); x++)
            {
                if (occ[x + z * c.w] < v)
                {
                    occ[x + z * c.w] = v;
                }
            }
        }
    }

    /** HESCO ring: outer x0 / x1 / z0 / z1 (inclusive), gate centre x. */
    int wx0, wx1, wz0, wz1, gx;
    /** Inner area (inside the wall). */
    int ix0, ix1, iz0, iz1;

    MilitaryBase(int size, long seed)
    {
        this.size = size;
        this.r = new Random(seed);
        int w = SIZE[size][0], l = SIZE[size][1];
        c = new Canvas(w, HEIGHT, l);
        occ = new byte[w * l];
        int m = 6;                                   // standoff ring outside the wall (wire at 2, 3)
        int gateRoom = size == SMALL ? 12 : size == MEDIUM ? 16 : 18;
        wx0 = m;
        wx1 = w - 1 - m;
        wz0 = m;
        wz1 = l - 1 - gateRoom;
        gx = w / 2;
        ix0 = wx0 + 2;
        ix1 = wx1 - 2;
        iz0 = wz0 + 2;
        iz1 = wz1 - 2;
    }

    /** North end of the main road (the TOC's front). */
    int mainZ0;

    /** Builds the whole base. */
    Canvas build()
    {
        int tocW = size == SMALL ? 7 : 11, tocL = size == SMALL ? 9 : 11;
        int tocZ0 = iz0 + (size == SMALL ? 5 : 6);
        mainZ0 = tocZ0 + tocL + (size == SMALL ? 2 : 4);
        ground();
        wire();
        hescoWall();
        gate();
        towers();
        roads();
        // the ring road, the main road and the gate area are not building ground
        take(ix0, iz0, ix1, iz0 + 2);
        take(ix0, iz1 - 2, ix1, iz1);
        take(ix0, iz0, ix0 + 2, iz1);
        take(ix1 - 2, iz0, ix1, iz1);
        take(gx - 2, mainZ0, gx + 2, wz1);
        take(gx - 8, wz1 - 8, gx + 11, wz1);         // guard shack, search area, gate towers
        for (int[] t : towerSpots)
        {
            take(t[0] - 1, t[1] - 1, t[0] + 4, t[1] + 4);
        }
        // zones: west of the road (living), east (logistics), the whole inside for the rest
        int zx0 = ix0 + 3, zx1 = ix1 - 3, zz0 = iz0 + 3, zz1 = wz1 - 10;
        int wx1 = gx - 4, ex0 = gx + 4;
        if (size == SMALL)
        {
            tocCore(gx - tocW / 2, tocZ0, tocW, tocL);
            bhut(ix0 + 4, iz0 + 5, 0, true);
            bhut(ix0 + 4, iz0 + 20, 2, false);
            fit("aid tent", ex0, zz0, zx1, zz1, true, false, (x, z) -> tent(x, z, 7, 9, 0));
            fit("mortar pit", ex0, zz0, zx1, zz1, true, false, (x, z) -> mortarPit(x, z));
            if (!fit("vehicles", ex0, zz0, zx1, zz1, true, true, (x, z) -> vehiclePark(x, z, 2, true)))
            {
                fit("vehicle", zx0, zz0, zx1, zz1, true, true, (x, z) -> vehiclePark(x, z, 1, false));
            }
            fit("porta-johns", zx0, zz0, wx1, zz1, false, true, (x, z) -> portaJohns(x, z, 3));
            fit("generators", zx0, mainZ0, zx1, zz1, false, false, (x, z) -> generatorFarm(x, z));
        }
        else
        {
            toc(gx - tocW / 2, tocZ0, tocW, tocL);
            lsa(zx0 + 1, zz0 + 2, wx1, zz1, size == LARGE ? 4 : 2);
            fit("ASP", ex0, zz0, zx1, zz1, true, false, (x, z) -> asp(x, z, x + 9, z + 9));
            fit("fuel point", ex0, zz0, zx1, zz1, true, false, (x, z) -> fuelPoint(x, z, x + 8, z + 7));
            int mw = size == LARGE ? 30 : 20, ml = size == LARGE ? 18 : 14;
            fit("motor pool", ex0, zz0, zx1, zz1, true, true, (x, z) -> motorPool(x, z, x + mw - 1, z + ml - 1));
            fit("dining tent", ex0, zz0, zx1, zz1, false, false, (x, z) -> tent(x, z, 7, 13, 1));
            fit("helipad", ex0, zz0, zx1, zz1, true, false, (x, z) -> helipad(x, z, size == LARGE));
            if (size == LARGE)
            {
                fit("helipad 2", zx0, zz0, zx1, zz1, false, true, (x, z) -> helipad(x, z, false));
            }
            fit("aid tent", zx0, zz0, zx1, zz1, false, true, (x, z) -> tent(x, z, 7, 11, 0));
            fit("latrine", zx0, zz0, wx1, zz1, false, true, (x, z) -> latrine(x, z));
            fit("porta-johns", zx0, zz0, wx1, zz1, false, true, (x, z) -> portaJohns(x, z, 4));
            fit("shower", zx0, zz0, wx1, zz1, false, true, (x, z) -> conexAt(x, z, 2));
            if (size == LARGE)
            {
                fit("gym", zx0, zz0, zx1, zz1, false, true, (x, z) -> tent(x, z, 7, 11, 2));
                fit("burn pit", ex0, zz0, zx1, zz1, true, true, (x, z) -> burnPit(x, z));
                fit("hangar", ex0, zz0, zx1, zz1, true, true, (x, z) -> hangar(x, z));
                fit("conex yard", zx0, zz0, zx1, zz1, true, true, (x, z) -> conexYard(x, z, 4));
                fit("conex yard 2", zx0, zz0, zx1, zz1, false, true, (x, z) -> conexYard(x, z, 3));
                for (int k = 0; k < 3; k++)
                {
                    fit("storage conex", ex0, zz0, zx1, zz1, true, false, (x, z) -> conexAt(x, z, 1));
                }
            }
            fit("mortar pit", zx0, zz0, zx1, zz1, false, false, (x, z) -> mortarPit(x, z));
            if (size == MEDIUM)
            {
                fit("conex yard", zx0, zz0, zx1, zz1, true, true, (x, z) -> conexYard(x, z, 3));
            }
            if (!fit("vehicles", zx0, mainZ0, zx1, zz1, false, false, (x, z) -> vehiclePark(x, z, 2, false)))
            {
                fit("vehicle", zx0, zz0, zx1, zz1, false, false, (x, z) -> vehiclePark(x, z, 1, false));
            }
            fit("generators", zx0, mainZ0, zx1, zz1, true, false, (x, z) -> generatorFarm(x, z));
            for (int k = 0; k < (size == LARGE ? 4 : 2); k++)
            {
                fit("bunker", zx0, zz0, zx1, zz1, false, true, (x, z) -> bunker(x, z, x + 4));
            }
        }
        roadside();
        decay();
        // overview cameras
        poi("aerial_sw", -18, 40, c.l + 14, 225, 38);
        poi("aerial_ne", c.w + 16, 44, -16, 45, 40);
        poi("top", c.w / 2.0, 110, c.l / 2.0, 180, 90);
        poi("gate_approach", gx + 0.5, 2.6, c.l + 6, 180, 4);
        poi("main_road", gx + 0.5, 2.6, wz1 - 4, 180, 2);
        return c;
    }

    /**
     * First origin in the zone where the module fits (its own reserve decides), scanning rows
     * from the north or the south and columns from the west or the east.
     */
    private boolean fit(String name, int zx0, int zz0, int zx1, int zz1, boolean fromEast, boolean fromSouth,
                        java.util.function.BiPredicate<Integer, Integer> place)
    {
        for (int i = 0; i <= zz1 - zz0; i++)
        {
            int z = fromSouth ? zz1 - i : zz0 + i;
            for (int j = 0; j <= zx1 - zx0; j++)
            {
                int x = fromEast ? zx1 - j : zx0 + j;
                if (place.test(x, z))
                {
                    log.add(name + " ok " + x + "," + z);
                    return true;
                }
            }
        }
        log.add(name + " DID NOT FIT");
        return false;
    }

    /** Parked Humvees (jeep wrecks) in a row, nose to the road; n vehicles, 4 apart. */
    private boolean vehiclePark(int x0, int z0, int n, boolean withApc)
    {
        int x1 = x0 + n * 4 + (withApc ? 2 : 1), z1 = z0 + (withApc ? 8 : 5);
        if (!reserve(x0, z0, x1, z1))
        {
            return false;
        }
        for (int k = 0; k < n; k++)
        {
            boolean apc = withApc && k == n - 1;
            c.prop(x0 + 2 + k * 4 + (apc ? 1 : 0), 1, z0 + (apc ? 5 : 3), apc ? b.apc : b.jeep, S);
        }
        return true;
    }

    /** Generators on a gravel pad behind a sandbag wall, fuel drums, a light tower. */
    private boolean generatorFarm(int x0, int z0)
    {
        if (!reserve(x0, z0, x0 + 5, z0 + 3))
        {
            return false;
        }
        for (int x = x0; x <= x0 + 5; x++)
        {
            for (int z = z0; z <= z0 + 3; z++)
            {
                c.set(x, 0, z, Blocks.gravel);
            }
            c.set(x, 1, z0, b.sandbag);
        }
        c.prop(x0 + 1, 1, z0 + 2, b.generator, S);
        c.prop(x0 + 3, 1, z0 + 2, b.generator, S);
        c.prop(x0 + 5, 1, z0 + 2, b.barrel, S);
        c.prop(x0 + 5, 1, z0 + 3, b.barrel, S);
        c.prop(x0, 1, z0 + 3, b.lightTower, S);
        return true;
    }

    /**
     * Conex storage yard: n containers side by side (long axis north-south, doors south), every
     * other one with a second container stacked on it, pallets and a forklift-less mess of
     * crates in front; the middle ones hold loot.
     */
    private boolean conexYard(int x0, int z0, int n)
    {
        int x1 = x0 + n * 4 - 2, z1 = z0 + 10;
        if (!reserve(x0, z0, x1, z1))
        {
            return false;
        }
        for (int k = 0; k < n; k++)
        {
            int cx = x0 + k * 4;
            conex(cx, z0, false, k % 2 == 1 ? 1 : 0);
            if (k % 2 == 0)
            {
                // stacked: a second box on top, its own skin
                Block skin = k % 4 == 0 ? b.metalRust : b.metalGreen;
                c.fill(cx, 4, z0, cx + 2, 6, z0 + 6, skin);
            }
            c.prop(cx + 1, 1, z0 + 8, k % 2 == 0 ? b.woodCrate : b.cardboard, S);
        }
        c.prop(x0, 1, z1, b.tireStack, S);
        c.prop(x1, 1, z1, b.barrel, S);
        return true;
    }

    /**
     * Clamshell maintenance hangar: a big tan vault (11 wide, 7 high, 15 long) open on the south
     * end, a vehicle being worked on inside, benches, tyres, a generator and a light tower.
     */
    private boolean hangar(int x0, int z0)
    {
        int w = 11, l = 15, x1 = x0 + w - 1, z1 = z0 + l - 1, mx = x0 + w / 2;
        if (!reserve(x0 - 1, z0, x1 + 1, z1 + 2))
        {
            return false;
        }
        for (int z = z0; z <= z1; z++)
        {
            boolean back = z == z0;
            for (int x = x0; x <= x1; x++)
            {
                int d = Math.min(x - x0, x1 - x);
                int top = d == 0 ? 3 : d == 1 ? 5 : d == 2 ? 6 : 7;
                c.set(x, 0, z, Blocks.stone);
                for (int y = 1; y <= top; y++)
                {
                    boolean skin = y == top || d == 0 || back;
                    c.set(x, y, z, skin ? Blocks.sandstone : null, skin ? 2 : 0);
                }
            }
            if (z % 4 == 0)
            {
                // frame ribs
                for (int x = x0; x <= x1; x++)
                {
                    int d = Math.min(x - x0, x1 - x);
                    int top = d == 0 ? 3 : d == 1 ? 5 : d == 2 ? 6 : 7;
                    c.set(x, top, z, Blocks.planks, 1);
                }
            }
        }
        c.prop(mx, 1, z0 + 7, b.apc, S);
        c.prop(x0 + 1, 1, z0 + 2, b.metalTable, E);
        c.prop(x0 + 1, 1, z0 + 5, b.storageCrate, E);
        c.prop(x1 - 1, 1, z0 + 2, b.tireStack, W);
        c.prop(x1 - 1, 1, z0 + 4, b.tire, W);
        c.prop(x1 - 1, 1, z0 + 11, b.generator, W);
        c.prop(x0 + 1, 1, z1 - 1, b.barrel, E);
        c.prop(x1 + 1, 1, z1 + 1, b.lightTower, S);
        for (int x = x0; x <= x1; x++)
        {
            c.set(x, 0, z1 + 1, Blocks.gravel);
            c.set(x, 0, z1 + 2, Blocks.gravel);
        }
        poi("hangar", mx + 0.5, 2.6, z1 + 5.5, 180, 6);
        return true;
    }

    /** A free standing conex (reserves its own ground); content as conex(). */
    private boolean conexAt(int x0, int z0, int content)
    {
        if (!reserve(x0, z0, x0 + 2, z0 + 7))
        {
            return false;
        }
        conex(x0, z0, false, content);
        return true;
    }

    /** Along the main road: light towers, water pallets by the TOC front. */
    private void roadside()
    {
        for (int z = mainZ0 + 4; z <= wz1 - 10; z += 12)
        {
            if (c.get(gx + 3, 1, z) == null && occ[gx + 3 + z * c.w] != 2)
            {
                c.prop(gx + 3, 1, z, b.lightTower, W);
            }
        }
        if (c.get(gx - 3, 1, mainZ0) == null)
        {
            c.prop(gx - 3, 1, mainZ0, b.waterPalletTarp, S);
        }
    }

    // ================================================================ ground, wire, wall

    private void ground()
    {
        for (int z = 0; z < c.l; z++)
        {
            for (int x = 0; x < c.w; x++)
            {
                double n = noise(x, z);
                boolean inside = x >= wx0 && x <= wx1 && z >= wz0 && z <= wz1;
                // bulldozed ground: packed dirt with gravel patches, a little coarse dirt, grass
                // left in the outer ring (coarse dirt is bright orange in Decimation's pack: sparing)
                Block g = n < 0.58 ? Blocks.gravel : n > 0.88 && !inside ? Blocks.grass : Blocks.dirt;
                c.set(x, 0, z, g);
            }
        }
    }

    private double noise(int x, int z)
    {
        long h = (x * 73856093L) ^ (z * 19349663L) ^ (r.hashCode() * 0L) ^ 0x5DEECE66DL;
        h ^= (h >>> 13);
        h *= 0x2545F4914F6CDD1DL;
        double a = ((h >>> 11) & 0xFFFF) / 65535.0;
        // blotches: average with the 4 neighbours' cells of a 3 block grid
        long g = ((x / 3) * 83492791L) ^ ((z / 3) * 297121507L);
        g ^= g >>> 17;
        g *= 0x9E3779B97F4A7C15L;
        double b2 = ((g >>> 20) & 0xFFFF) / 65535.0;
        return a * 0.35 + b2 * 0.65;
    }

    private void wire()
    {
        // concertina ring at 2 (and 3 on the large base), open where the gate lane crosses it
        int[] rings = size == LARGE ? new int[] {2, 3} : new int[] {2};
        for (int k : rings)
        {
            int x0 = k, x1 = c.w - 1 - k, z0 = k, z1 = c.l - 1 - k;
            for (int x = x0; x <= x1; x++)
            {
                c.set(x, 1, z0, b.wire);
                if (Math.abs(x - gx) > 3)
                {
                    c.set(x, 1, z1, b.wire);
                }
            }
            for (int z = z0; z <= z1; z++)
            {
                c.set(x0, 1, z, b.wire);
                c.set(x1, 1, z, b.wire);
            }
        }
        // a second coil row just outside the HESCO, broken in places (wind, decay)
        for (int x = wx0 - 2; x <= wx1 + 2; x++)
        {
            if (r.nextFloat() < 0.8f)
            {
                c.set(x, 1, wz0 - 2, b.wire);
            }
        }
        for (int z = wz0 - 2; z <= wz1 + 2; z++)
        {
            if (r.nextFloat() < 0.8f)
            {
                c.set(wx0 - 2, 1, z, b.wire);
            }
            if (r.nextFloat() < 0.8f)
            {
                c.set(wx1 + 2, 1, z, b.wire);
            }
        }
    }

    private void hescoWall()
    {
        for (int t = 0; t < 2; t++)
        {
            for (int x = wx0; x <= wx1; x++)
            {
                hesco(x, wz0 + t);
                if (Math.abs(x - gx) > 2)
                {
                    hesco(x, wz1 - t);
                }
            }
            for (int z = wz0; z <= wz1; z++)
            {
                hesco(wx0 + t, z);
                hesco(wx1 - t, z);
            }
        }
        // sandbag cap on some stretches (a fighting position on top of the cells), inner row
        for (int x = wx0 + 5; x <= wx1 - 5; x++)
        {
            if (((x - wx0) / 6) % 3 == 1)
            {
                c.set(x, 4, wz0 + 1, b.sandbagBeige);
            }
        }
        // concertina on top of the outer row in long stretches (the wall top is not a walkway)
        for (int x = wx0 + 4; x <= wx1 - 4; x++)
        {
            if (((x - wx0) / 9) % 2 == 0)
            {
                c.set(x, 4, wz0, b.wire);
            }
        }
        for (int z = wz0 + 4; z <= wz1 - 4; z++)
        {
            if (((z - wz0) / 9) % 2 == 1)
            {
                c.set(wx0, 4, z, b.wire);
                c.set(wx1, 4, z, b.wire);
            }
        }
        // sandbag fighting positions (U shapes) against the inner face of the side walls
        for (int z = wz0 + 14; z <= wz1 - 14; z += size == SMALL ? 40 : 22)
        {
            fightingPosition(wx0 + 2, z, 1);
            fightingPosition(wx1 - 4, z, -1);
        }
        // firing step behind the wall on the gate side: sandbags 1 high along the inner face
        for (int x = wx0 + 5; x <= wx1 - 5; x++)
        {
            if (Math.abs(x - gx) > 8 && r.nextFloat() < 0.5f)
            {
                c.set(x, 1, wz1 - 2, b.sandbag);
            }
        }
    }

    /** Sandbag U (3 wide, 2 deep, 2 high) open toward the base; an ammo crate inside. */
    private void fightingPosition(int x0, int z0, int dir)
    {
        for (int k = 0; k < 3; k++)
        {
            c.set(x0 + (dir > 0 ? 0 : 2), 1, z0 + k, b.sandbag);
            c.set(x0 + (dir > 0 ? 0 : 2), 2, z0 + k, b.sandbagBeige);
        }
        c.set(x0 + 1, 1, z0, b.sandbag);
        c.set(x0 + 1, 1, z0 + 2, b.sandbag);
        c.prop(x0 + 1, 1, z0 + 1, b.ammoCrate, dir > 0 ? E : W);
    }

    private void hesco(int x, int z)
    {
        c.fill(x, 1, z, x, 3, z, b.hesco);
    }

    // ================================================================ entry control point

    private void gate()
    {
        int z0 = wz1 + 1, z1 = c.l - 4;     // the ECP lane outside the wall
        // lane ground: gravel road
        for (int z = wz1 - 1; z < c.l; z++)
        {
            for (int x = gx - 2; x <= gx + 2; x++)
            {
                c.set(x, 0, z, Blocks.gravel);
                c.clearBox(x, 1, z, x, 3, z);
            }
        }
        // T-walls along both sides of the lane (blast walls guiding the traffic)
        for (int z = z0; z <= z1; z++)
        {
            c.prop(gx - 4, 1, z, b.tWall, E);
            c.prop(gx + 4, 1, z, b.tWall, W);
        }
        // serpentine: jersey barriers from alternate sides, every 3 blocks
        boolean left = true;
        for (int z = z0 + 3; z <= z1 - 2; z += 3)
        {
            c.prop(left ? gx - 2 : gx + 2, 1, z, b.jersey, S);
            c.prop(left ? gx - 1 : gx + 1, 1, z, b.jersey, S);
            left = !left;
        }
        // hedgehogs and sawhorses outside the T-wall lines, a warning sawhorse at the lane mouth
        for (int z = z0 + 2; z <= z1; z += 4)
        {
            c.prop(gx - 7, 1, z, b.hedgehog, S);
            c.prop(gx + 7, 1, z, b.hedgehog, S);
        }
        c.prop(gx - 1, 1, c.l - 2, b.sawhorse, S);
        c.prop(gx + 1, 1, c.l - 2, b.sawhorse, S);
        // final denial barrier in the gate: drop arm barriers on the sides, sawhorses across
        // (one pushed open)
        c.prop(gx - 2, 1, wz1, b.streetBarrier, S);
        c.prop(gx + 2, 1, wz1, b.streetBarrier, S);
        c.prop(gx - 1, 1, wz1 - 3, b.sawhorse, S);
        c.prop(gx + 1, 1, wz1 - 3, b.sawhorse, S);
        poi("ecp_inside_lane", gx + 0.5, 2.6, wz1 + 2.5, 0, 8);
        // guard shack inside the gate (west of the lane): sandbag booth, plank roof
        int sx = gx - 7, sz = wz1 - 6;
        sandbagBooth(sx, sz);
        // search area inside the gate (east of the lane) on the bigger bases
        if (size != SMALL)
        {
            int ax0 = gx + 3, ax1 = gx + 10, az0 = wz1 - 9, az1 = wz1 - 2;
            for (int x = ax0; x <= ax1; x++)
            {
                for (int z = az0; z <= az1; z++)
                {
                    c.set(x, 0, z, Blocks.gravel);
                }
            }
            for (int z = az0; z <= az1; z++)
            {
                c.prop(ax1 + 1, 1, z, b.tWall, W);
            }
            for (int x = ax0; x <= ax1 + 1; x++)
            {
                c.prop(x, 1, az0 - 1, b.tWall, S);
            }
            c.prop(ax0 + 3, 1, az0 + 2, b.lightTower, S);
            c.prop(ax1 - 1, 1, az1 - 1, b.barrel, S);
            c.prop(ax1 - 1, 1, az1 - 2, b.barrel, S);
            c.prop(ax0 + 1, 1, az0 + 1, b.tireStack, S);
        }
    }

    /** 3 x 3 sandbag guard booth: walls 2 high, door gap on the east, window gap south, roof. */
    private void sandbagBooth(int x0, int z0)
    {
        for (int x = x0; x <= x0 + 3; x++)
        {
            for (int z = z0; z <= z0 + 3; z++)
            {
                boolean edge = x == x0 || x == x0 + 3 || z == z0 || z == z0 + 3;
                if (edge)
                {
                    c.set(x, 1, z, b.sandbagBeige);
                    boolean window = z == z0 + 3 && (x == x0 + 1 || x == x0 + 2);
                    if (!window)
                    {
                        c.set(x, 2, z, b.sandbagBeige);
                    }
                }
                c.set(x, 3, z, Blocks.wooden_slab, 1);  // spruce slab roof
            }
        }
        c.clear(x0 + 3, 1, z0 + 1);                     // door to the lane
        c.clear(x0 + 3, 2, z0 + 1);
        c.prop(x0 + 1, 1, z0 + 2, b.chair, S);
        c.prop(x0 + 2, 1, z0 + 2, b.radioSmall, S);
        c.prop(x0 + 1, 1, z0 + 1, b.ammoCrate, E);
        c.prop(x0 + 3, 4, z0 + 3, b.redLight, S);
    }

    // ================================================================ towers

    private void towers()
    {
        tower(wx0, wz0, 1, 1);
        tower(wx1 - 3, wz0, -1, 1);
        tower(wx0, wz1 - 3, 1, -1);
        tower(wx1 - 3, wz1 - 3, -1, -1);
        // overwatch beside the gate
        tower(gx - 7, wz1 - 3, 1, -1);
        if (size != SMALL)
        {
            tower(gx + 4, wz1 - 3, -1, -1);
        }
        if (size == LARGE)
        {
            // mid wall towers on the long sides
            tower(wx0, (wz0 + wz1) / 2 - 2, 1, 1);
            tower(wx1 - 3, (wz0 + wz1) / 2 - 2, -1, 1);
            tower(gx - 2, wz0, 1, 1);
        }
    }

    /**
     * Guard tower, 4 x 4 on the wall: HESCO filled base up to the wall top, a plank deck at 4,
     * sandbag parapet with firing gaps, corner posts, a slab roof at 8, a ladder inside the
     * base corner up through a hatch, spotlight / ammo / radio on the deck. (ix, iz) point into
     * the base (+1 / -1).
     */
    /** Tower corners (x0, z0), for the occupancy grid. */
    private final java.util.List<int[]> towerSpots = new java.util.ArrayList<int[]>();

    private void tower(int x0, int z0, int ix, int iz)
    {
        towerSpots.add(new int[] {x0, z0});
        c.fill(x0, 1, z0, x0 + 3, 3, z0 + 3, b.hesco);
        // the ladder in the inner corner cell (attached to the cell beside it, toward the wall)
        int lx = ix > 0 ? x0 + 3 : x0, lz = iz > 0 ? z0 + 3 : z0;
        c.clearBox(lx, 1, lz, lx, 4, lz);
        int ladderMeta = ix > 0 ? 5 : 4;               // vanilla ladder: 5 faces east (block west of it)
        for (int y = 1; y <= 4; y++)
        {
            c.set(lx, y, lz, Blocks.ladder, ladderMeta);
        }
        // deck
        for (int x = x0; x <= x0 + 3; x++)
        {
            for (int z = z0; z <= z0 + 3; z++)
            {
                if (x == lx && z == lz)
                {
                    continue;
                }
                c.set(x, 4, z, Blocks.planks, 1);
            }
        }
        // parapet: sandbags on the edge, a gap every other block on the outer sides (firing
        // ports at 6), none over the hatch side
        for (int x = x0; x <= x0 + 3; x++)
        {
            for (int z = z0; z <= z0 + 3; z++)
            {
                boolean edge = x == x0 || x == x0 + 3 || z == z0 || z == z0 + 3;
                if (!edge || (x == lx && z == lz) || (x == lx && Math.abs(z - lz) == 1) || (z == lz && Math.abs(x - lx) == 1))
                {
                    continue;
                }
                c.set(x, 5, z, b.sandbag);
                boolean outer = (ix > 0 ? x == x0 : x == x0 + 3) || (iz > 0 ? z == z0 : z == z0 + 3);
                if (outer && ((x + z) & 1) == 0)
                {
                    c.set(x, 6, z, b.sandbag);         // a second course with firing gaps
                }
            }
        }
        // corner posts and roof
        int[][] posts = {{x0, z0}, {x0 + 3, z0}, {x0, z0 + 3}, {x0 + 3, z0 + 3}};
        for (int[] p : posts)
        {
            for (int y = 5; y <= 7; y++)
            {
                if (c.get(p[0], y, p[1]) == null || y == 7)
                {
                    c.set(p[0], y, p[1], Blocks.fence);
                }
            }
        }
        for (int x = x0 - 1; x <= x0 + 4; x++)
        {
            for (int z = z0 - 1; z <= z0 + 4; z++)
            {
                c.set(x, 8, z, Blocks.wooden_slab, 1);
            }
        }
        poi("tower_deck", x0 + 1.5 + (ix > 0 ? 0.5 : 0), 6.6, z0 + 1.5 + (iz > 0 ? 0.5 : 0), ix > 0 ? 315 : 45, 18);
        // on the deck: spotlight looking out, ammo, radio
        int cx = x0 + (ix > 0 ? 1 : 2), cz = z0 + (iz > 0 ? 1 : 2);
        int look = iz > 0 ? N : S;
        c.prop(cx, 5, cz, b.spotlight, look);
        int ax = x0 + (ix > 0 ? 2 : 1), az = z0 + (iz > 0 ? 2 : 1);
        if (!(ax == lx && az == lz))
        {
            c.prop(ax, 5, az, r.nextBoolean() ? b.ammoCrate : b.militaryCrate, ix > 0 ? E : W);
        }
    }

    // ================================================================ roads

    private void roads()
    {
        // ring road inside the wall (3 wide) and the main road from the gate to the TOC
        for (int x = ix0; x <= ix1; x++)
        {
            for (int k = 0; k < 3; k++)
            {
                road(x, iz0 + k);
                road(x, iz1 - k);
            }
        }
        for (int z = iz0; z <= iz1; z++)
        {
            for (int k = 0; k < 3; k++)
            {
                road(ix0 + k, z);
                road(ix1 - k, z);
            }
        }
        for (int z = mainZ0; z <= wz1; z++)
        {
            for (int x = gx - 2; x <= gx + 2; x++)
            {
                road(x, z);
            }
        }
    }

    private void road(int x, int z)
    {
        Block g = c.get(x, 0, z);
        // gravel with the odd packed dirt patch (tyre tracks)
        c.set(x, 0, z, noise(x * 3, z * 3) > 0.82 ? Blocks.dirt : Blocks.gravel);
    }

    // ================================================================ TOC

    /**
     * Tactical operations centre: concrete building (light grey stone, flat roof with two
     * courses of sandbags), a HESCO blast ring 2 out with an opening, metal door with a keycard
     * screen, inside map tables, radio and computer desks on the walls, a weapon cabinet and an
     * ammo crate by the door, ceiling lights. Outside: flag pole, antenna mast, generators,
     * light tower, a wall flag.
     */
    private void toc(int x0, int z0, int w, int l)
    {
        int x1 = x0 + w - 1, z1 = z0 + l - 1, dx = x0 + w / 2;
        if (!reserve(x0 - 3, z0 - 3, x1 + 3, z1 + 3))
        {
            return;
        }
        // HESCO ring 2 high, 2 out, opening in front of the door (south)
        for (int x = x0 - 3; x <= x1 + 3; x++)
        {
            for (int z = z0 - 3; z <= z1 + 3; z++)
            {
                boolean ring = x == x0 - 3 || x == x1 + 3 || z == z0 - 3 || z == z1 + 3;
                if (ring && !(z == z1 + 3 && Math.abs(x - dx) <= 1))
                {
                    c.fill(x, 1, z, x, 2, z, b.hesco);
                }
                if (!ring && (x < x0 || x > x1 || z < z0 || z > z1))
                {
                    c.set(x, 0, z, Blocks.gravel);
                }
            }
        }
        tocBuilding(x0, z0, w, l);
    }

    /** The COP's TOC: the building with sandbags along its walls, no ring. */
    private void tocCore(int x0, int z0, int w, int l)
    {
        int x1 = x0 + w - 1, z1 = z0 + l - 1;
        if (!reserve(x0 - 3, z0 - 1, x1 + 3, z1 + 3))
        {
            return;
        }
        for (int x = x0 - 1; x <= x1 + 1; x++)
        {
            for (int z = z0 - 1; z <= z1 + 1; z++)
            {
                boolean ring = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                if (ring && !(z == z1 + 1 && Math.abs(x - (x0 + w / 2)) <= 1))
                {
                    c.set(x, 1, z, b.sandbag);
                    if ((x + z) % 4 != 0)
                    {
                        c.set(x, 2, z, b.sandbagBeige);
                    }
                }
            }
        }
        tocBuilding(x0, z0, w, l);
    }

    private void tocBuilding(int x0, int z0, int w, int l)
    {
        int x1 = x0 + w - 1, z1 = z0 + l - 1, dx = x0 + w / 2;
        // shell
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                c.set(x, 0, z, b.concreteFloor);
                for (int y = 1; y <= 4; y++)
                {
                    c.set(x, y, z, edge ? b.concrete : null);
                }
                c.set(x, 5, z, b.concrete);
                // two layers of sandbags on the roof slab, the upper one in loose piles
                c.set(x, 6, z, noise(x * 5, z * 5) < 0.5 ? b.sandbag : b.sandbagBeige);
                if (!edge && noise(x * 7 + 3, z * 7 + 1) > 0.55)
                {
                    c.set(x, 7, z, noise(x, z * 3) < 0.5 ? b.sandbagBeige : b.sandbag);
                }
            }
        }
        poi("toc_inside", dx + 0.5, 2.6, z1 - 1.5, 180, 12);
        poi("toc_outside", dx + 0.5, 2.6, z1 + 7.5, 180, 4);
        // door (south), keycard screen, slit windows high on the sides
        door(dx, 1, z1, b.metalDoor, S);
        c.prop(dx + 1, 2, z1 + 1, b.keycard, S);
        for (int z = z0 + 2; z <= z1 - 2; z += 3)
        {
            c.set(x0, 3, z, Blocks.iron_bars);
            c.set(x1, 3, z, Blocks.iron_bars);
        }
        // inside: map table in the middle (two tables end to end), chairs around
        int mz = z0 + l / 2 - 1;
        c.prop(dx, 1, mz, b.woodTable, S);
        c.prop(dx, 1, mz + 1, b.woodTable, S);
        c.prop(dx - 2, 1, mz, b.chair, E);
        c.prop(dx + 2, 1, mz + 1, b.chair, W);
        // radio and computer desks along the west and east walls
        for (int z = z0 + 1; z <= z1 - 2; z += 2)
        {
            c.prop(x0 + 1, 1, z, b.metalTable, E);
            c.prop(x0 + 1, 2, z, (z / 2) % 2 == 0 ? b.radio : b.monitor, E);
            c.prop(x0 + 2, 1, z, b.officeChair, W);
            c.prop(x1 - 1, 1, z, b.metalTable, W);
            c.prop(x1 - 1, 2, z, (z / 2) % 2 == 1 ? b.radioSmall : b.monitor, W);
            c.prop(x1 - 2, 1, z, b.officeChair, E);
        }
        // north wall: flag, the commander's desk
        c.prop(dx, 3, z0 + 1, b.wallFlag, S);
        c.prop(dx - 1, 1, z0 + 1, b.metalTable, S);
        c.prop(dx - 1, 2, z0 + 1, b.monitor, S);
        c.prop(dx - 1, 1, z0 + 2, b.officeChair, N);
        // by the door: weapon cabinet, ammo
        c.prop(dx - 2, 1, z1 - 1, b.weaponCabinet, E);
        c.prop(dx + 2, 1, z1 - 1, b.ammoCrateLarge, W);
        // ceiling lights
        for (int z = z0 + 2; z <= z1 - 2; z += 3)
        {
            c.prop(dx, 4, z, b.light, S);
        }
        // outside: flag pole, antenna mast, generators, light tower, wall flag
        c.prop(dx - 3, 1, z1 + 2, b.flagPole, S);
        for (int y = 1; y <= 9; y++)
        {
            c.prop(x1 + 2, y, z0 - 1, b.radioTower, S);
        }
        c.set(x1 + 2, 10, z0 - 1, Blocks.fence);
        c.prop(x1 + 2, 1, z1 - 1, b.generator, W);
        c.prop(x1 + 2, 1, z1 - 2, b.generator, W);
        c.prop(x1 + 2, 1, z1 - 4, b.barrel, W);
        c.prop(x0 - 2, 1, z1 + 1, b.lightTower, S);
        c.prop(dx + 1, 3, z1 + 1, b.wallFlag, S);
    }

    /** A Decimation (vanilla metadata) door, both halves; front = the side it opens to. */
    void door(int x, int y, int z, Block d, int front)
    {
        // vanilla door: lower half 0 west edge, 1 north edge, 2 east edge, 3 south edge
        int m = front == S ? 3 : front == N ? 1 : front == E ? 2 : 0;
        c.set(x, y, z, d, m, Canvas.DOOR);
        c.set(x, y + 1, z, d, 8, Canvas.DOOR);
    }

    // ================================================================ living area

    /**
     * B-huts in rows (long axis north-south), doors on the road side, sandbag blast walls between
     * pairs, a duck and cover bunker per row, latrine and shower at the end. Hut kinds: barracks
     * (bunks), office, arms room (loot), chapel.
     */
    private void lsa(int x0, int z0, int x1, int z1, int maxRows)
    {
        // rows of huts in pairs facing each other across a company street: row A doors south,
        // row B doors north, the street between them 9 wide with duck and cover bunkers between
        // the door columns; sandbag blast walls between neighbouring huts
        int hutW = 5, hutL = 10, pitch = hutW + 4;
        int cols = Math.max(1, (x1 - x0 + 1 + 4) / pitch);
        int n = 0, z = z0;
        boolean doorSouth = true;
        for (int row = 0; row < maxRows && z + hutL + 2 <= z1; row++)
        {
            for (int col = 0; col < cols; col++)
            {
                int x = x0 + col * pitch;
                if (x + hutW + 1 > x1)
                {
                    break;
                }
                int kind = n == 1 ? 2 : n == 4 ? 1 : (n == 9 && size == LARGE ? 3 : 0);
                if (bhut(x, z, kind, doorSouth))
                {
                    n++;
                    if (col < cols - 1 && x + pitch + hutW + 1 <= x1)
                    {
                        for (int zz = z + 1; zz <= z + hutL - 2; zz++)
                        {
                            c.set(x + hutW + 1, 1, zz, b.sandbagBeige);
                            c.set(x + hutW + 1, 2, zz, (zz & 1) == 0 ? b.sandbagBeige : b.sandbag);
                        }
                    }
                }
            }
            if (doorSouth)
            {
                for (int col = 0; col < cols - 1; col++)
                {
                    int bx = x0 + col * pitch + hutW - 1;
                    if (bx + 4 <= x1)
                    {
                        bunker(bx, z + hutL + 3, bx + 4);
                    }
                }
                z += hutL + 9;
            }
            else
            {
                z += hutL + 6;
            }
            doorSouth = !doorSouth;
        }
    }

    /**
     * B-hut, 5 x 10 (16 x 32 ft): raised plywood floor, birch plank walls on a spruce frame,
     * low gable roof of spruce stairs, door at one end with a step, windows (some shuttered).
     * kind: 0 barracks, 1 office, 2 arms room, 3 chapel.
     */
    private boolean bhut(int x0, int z0, int kind, boolean doorSouth)
    {
        int x1 = x0 + 4, z1 = z0 + 9;
        if (!reserve(x0 - 1, z0 - 2, x1 + 1, z1 + 2))
        {
            return false;
        }
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                c.set(x, 1, z, Blocks.planks, 2);         // raised floor (birch plywood)
                for (int y = 2; y <= 4; y++)
                {
                    if (edge)
                    {
                        c.set(x, y, z, corner ? Blocks.log : Blocks.planks, corner ? 1 : 2);
                    }
                }
            }
        }
        // roof: stairs on the eaves, planks, a ridge of slabs; overhang at the gable ends
        for (int z = z0 - 1; z <= z1 + 1; z++)
        {
            c.set(x0, 5, z, Blocks.spruce_stairs, 0);     // high side east
            c.set(x1, 5, z, Blocks.spruce_stairs, 1);     // high side west
            for (int x = x0 + 1; x <= x1 - 1; x++)
            {
                c.set(x, 5, z, Blocks.planks, 1);
            }
            c.set(x0 + 1, 6, z, Blocks.spruce_stairs, 0);
            c.set(x1 - 1, 6, z, Blocks.spruce_stairs, 1);
            c.set(x0 + 2, 6, z, Blocks.wooden_slab, 1);
        }
        // door, step, a light over it
        int dz = doorSouth ? z1 : z0;
        door(x0 + 2, 2, dz, Blocks.wooden_door, doorSouth ? S : N);
        c.set(x0 + 2, 1, doorSouth ? z1 + 1 : z0 - 1, Blocks.spruce_stairs, doorSouth ? 3 : 2);
        c.prop(x0 + 3, 4, doorSouth ? z1 + 1 : z0 - 1, b.light, doorSouth ? S : N);
        // windows along the long sides, some boarded (trapdoor shutters closed outside)
        for (int z = z0 + 2; z <= z1 - 2; z += 3)
        {
            for (int x : new int[] {x0, x1})
            {
                c.set(x, 3, z, Blocks.glass_pane);
                if (r.nextFloat() < 0.4f)
                {
                    c.set(x == x0 ? x - 1 : x + 1, 3, z, Blocks.trapdoor, x == x0 ? 3 : 2);
                }
            }
        }
        // interior y 2..4, x0+1..x1-1, z0+1..z1-1
        int in0 = z0 + 1, in1 = z1 - 1;
        String[] kindName = {"hut_barracks", "hut_office", "hut_arms", "hut_chapel"};
        poi(kindName[kind] + "_inside", x0 + 2.5, 3.6, doorSouth ? z1 - 1.0 : z0 + 2.0, doorSouth ? 180 : 0, 10);
        poi("lsa_street", x0 + 2.5, 2.6, doorSouth ? z1 + 4.5 : z0 - 3.5, doorSouth ? 225 : 315, 6);
        switch (kind)
        {
            case 1: // office
                for (int z = in0 + 1; z <= in1 - 2; z += 2)
                {
                    c.prop(x0 + 1, 2, z, b.metalTable, E);
                    c.prop(x0 + 1, 3, z, z % 4 == 0 ? b.monitor : b.radioSmall, E);
                    c.prop(x0 + 2, 2, z, b.officeChair, W);
                }
                c.prop(x1 - 1, 2, in0 + 1, b.weaponCabinet, W);
                c.prop(x1 - 1, 2, in0 + 3, b.storageCrate, W);
                c.prop(x1 - 1, 3, in0 + 5, b.wallFlag, W);
                c.prop(x1 - 1, 2, in1 - 1, b.cardboard, W);
                break;
            case 2: // arms room: cabinets and crates, the loot
                for (int z = in0; z <= in1; z++)
                {
                    if (z == (doorSouth ? in1 : in0))
                    {
                        continue;
                    }
                    c.prop(x0 + 1, 2, z, z % 2 == 0 ? b.weaponCabinet : b.militaryCrate, E);
                    c.prop(x1 - 1, 2, z, z % 3 == 0 ? b.ammoCrateLarge : (z % 3 == 1 ? b.ammoCrate : b.militaryCrate), W);
                }
                c.prop(x0 + 2, 2, (in0 + in1) / 2, b.metalTable, S);
                break;
            case 3: // chapel: benches facing the altar end
                for (int z = in0 + 1; z <= in1 - 2; z += 2)
                {
                    c.set(x0 + 1, 2, z, Blocks.spruce_stairs, doorSouth ? 3 : 2);
                    c.set(x1 - 1, 2, z, Blocks.spruce_stairs, doorSouth ? 3 : 2);
                }
                c.prop(x0 + 2, 2, doorSouth ? in0 : in1, b.woodTable, S);
                c.prop(x0 + 2, 3, doorSouth ? in0 : in1, b.radioSmall, S);
                c.prop(x0 + 2, 3, doorSouth ? z0 + 1 : z1 - 1, b.wallFlag, doorSouth ? S : N);
                break;
            default: // barracks: bunks on both long walls, footlockers, a table
                for (int z = in0; z + 1 <= in1; z += 3)
                {
                    if (z <= (doorSouth ? in1 : in0) && z + 1 >= (doorSouth ? in1 : in0))
                    {
                        continue;
                    }
                    bunk(x0 + 1, z, doorSouth);
                    bunk(x1 - 1, z, doorSouth);
                    int lz = z + 2;
                    if (lz <= in1 && lz != (doorSouth ? in1 : in0))
                    {
                        c.prop(x0 + 1, 2, lz, b.storageCrate, E);
                        if (r.nextFloat() < 0.6f)
                        {
                            c.prop(x1 - 1, 2, lz, b.storageCrate, W);
                        }
                    }
                }
                c.prop(x0 + 2, 4, (in0 + in1) / 2, b.light, S);
                break;
        }
        return true;
    }

    /** A bunk: bed (foot toward the door end) with an upper bunk slab and a mattress carpet. */
    private void bunk(int x, int z, boolean doorSouth)
    {
        // bed foot at z (door side), head at z + 1 away... beds point foot -> head: 0 south, 2 north
        int footZ = doorSouth ? z + 1 : z, headZ = doorSouth ? z : z + 1;
        int dir = doorSouth ? 2 : 0;          // foot -> head: toward the far end
        c.set(x, 2, footZ, Blocks.bed, dir);
        c.set(x, 2, headZ, Blocks.bed, dir | 8);
        c.set(x, 3, z, Blocks.wooden_slab, 9);       // spruce slab, top half: the upper bunk
        c.set(x, 3, z + 1, Blocks.wooden_slab, 9);
        c.set(x, 4, z, Blocks.carpet, 8);            // light grey mattress
        c.set(x, 4, z + 1, Blocks.carpet, 8);
    }

    /** Duck and cover bunker: concrete tube 3 wide, sandbags on the roof, open both ends. */
    private boolean bunker(int x0, int z0, int x1)
    {
        if (!reserve(x0, z0, x1, z0 + 2))
        {
            return false;
        }
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z0 + 2; z++)
            {
                boolean wall = z == z0 || z == z0 + 2;
                if (wall)
                {
                    c.fill(x, 1, z, x, 2, z, b.concrete);
                }
                c.set(x, 3, z, b.concrete);
                c.set(x, 4, z, b.sandbag);
            }
        }
        c.prop(x0 + 1, 1, z0 + 1, b.waterPallet, S);
        return true;
    }

    /** Plywood latrine shed (birch walls, door, 3 seats) beside the huts. */
    private boolean latrine(int x0, int z0)
    {
        if (!reserve(x0, z0, x0 + 6, z0 + 4))
        {
            return false;
        }
        int x1 = x0 + 6, z1 = z0 + 3;
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                c.set(x, 1, z, Blocks.planks, 2);
                for (int y = 2; y <= 3; y++)
                {
                    if (edge)
                    {
                        c.set(x, y, z, Blocks.planks, 2);
                    }
                }
                c.set(x, 4, z, Blocks.wooden_slab, 1);
            }
        }
        door(x0 + 3, 2, z1, Blocks.wooden_door, S);
        for (int x = x0 + 1; x <= x1 - 1; x += 2)
        {
            c.set(x, 2, z0 + 1, Blocks.cauldron, 0);   // seats
        }
        c.set(x0 + 1, 3, z0 + 1, Blocks.trapdoor, 4); // a vent
        return true;
    }

    /** A row of porta-johns: blue plastic boxes, an open door panel on the front (south), white roof. */
    private boolean portaJohns(int x0, int z0, int n)
    {
        if (!reserve(x0, z0, x0 + 2 * n, z0 + 1))
        {
            return false;
        }
        for (int k = 0; k < n; k++)
        {
            int x = x0 + k * 2;
            if (c.get(x, 1, z0) != null || c.get(x, 1, z0 + 1) != null)
            {
                continue;
            }
            c.fill(x, 1, z0, x, 2, z0, Blocks.wool, 11);
            c.set(x, 3, z0, Blocks.carpet, 0);
            c.set(x, 2, z0 + 1, Blocks.trapdoor, 4 | 1);     // open trapdoor against the front: the door
        }
        return true;
    }

    /**
     * Conex container (20 ft): 3 wide, 3 high, 7 long, metal walls, open end with doors (one hanging
     * open) or closed; content 0 empty, 1 loot, 2 shower (water pallet, drains).
     */
    private void conex(int x0, int z0, boolean alongX, int content)
    {
        Block skin = (x0 + z0) % 3 == 0 ? b.metalGreen : ((x0 + z0) % 3 == 1 ? b.metalRust : b.metalPlate);
        int lx = alongX ? 7 : 3, lz = alongX ? 3 : 7;
        for (int x = x0; x < x0 + lx; x++)
        {
            for (int z = z0; z < z0 + lz; z++)
            {
                boolean edge = x == x0 || x == x0 + lx - 1 || z == z0 || z == z0 + lz - 1;
                c.set(x, 0, z, Blocks.gravel);
                for (int y = 1; y <= 3; y++)
                {
                    c.set(x, y, z, edge || y == 3 ? skin : null);
                }
            }
        }
        // open end (south or east): doorway 1 wide 2 high
        int ox = alongX ? x0 + lx - 1 : x0 + 1, oz = alongX ? z0 + 1 : z0 + lz - 1;
        c.clear(ox, 1, oz);
        c.clear(ox, 2, oz);
        if (content == 1)
        {
            int ix = alongX ? x0 + 1 : x0 + 1, iz = alongX ? z0 + 1 : z0 + 1;
            for (int k = 0; k < 4; k++)
            {
                int x = alongX ? ix + k : ix, z = alongX ? iz : iz + k;
                c.prop(x, 1, z, k % 2 == 0 ? b.militaryCrate : b.woodCrate, alongX ? W : N);
                if (k < 2)
                {
                    c.prop(x, 2, z, b.cardboard, alongX ? W : N);
                }
            }
        }
        else if (content == 2)
        {
            c.prop(alongX ? x0 + 1 : x0 + 1, 1, alongX ? z0 + 1 : z0 + 1, b.waterPalletTarp, S);
            c.set(alongX ? x0 + 3 : x0 + 1, 1, alongX ? z0 + 1 : z0 + 3, Blocks.cauldron, 0);
        }
    }

    // ================================================================ east side

    /**
     * Ammunition supply point: a sloped earth berm (3 wide: 1, 2, 1 high) around a gravel pad of
     * crates, entry gap on the west.
     */
    private boolean asp(int x0, int z0, int x1, int z1)
    {
        if (!reserve(x0, z0, x1, z1))
        {
            return false;
        }
        int ez = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                int d = Math.min(Math.min(x - x0, x1 - x), Math.min(z - z0, z1 - z));   // 0 outer ring
                boolean entry = x <= x0 + 2 && Math.abs(z - ez) <= 1;
                if (d <= 2 && !entry)
                {
                    int top = d == 1 ? 2 : 1;
                    for (int y = 1; y <= top; y++)
                    {
                        c.set(x, y, z, Blocks.dirt, noise(x * 9, z * 9) > 0.7 ? 1 : 0);
                    }
                    if (d == 1 && noise(x * 4, z * 4) > 0.8)
                    {
                        c.set(x, 3, z, Blocks.tallgrass, 1);              // weeds on the crest
                    }
                }
                else
                {
                    c.set(x, 0, z, Blocks.gravel);
                }
            }
        }
        x0 += 2;
        z0 += 2;
        x1 -= 2;
        z1 -= 2;
        // crate rows on pallets (the loot), a care package, a warning light
        for (int x = x0 + 2; x <= x1 - 2; x += 2)
        {
            for (int z = z0 + 2; z <= z1 - 2; z += 3)
            {
                Block crate = (x + z) % 3 == 0 ? b.ammoCrateLarge : ((x + z) % 3 == 1 ? b.ammoCrate : b.militaryCrate);
                c.prop(x, 1, z, crate, S);
            }
        }
        c.prop(x1 - 2, 1, z1 - 1, b.carePackage, S);
        c.prop(x0 + 1, 1, z0 + 1, b.redLight, S);
        return true;
    }

    /** Fuel point: fuel bladders (black wool mounds) in a low sandbag berm, drums, a pump generator. */
    private boolean fuelPoint(int x0, int z0, int x1, int z1)
    {
        if (!reserve(x0, z0, x1, z1))
        {
            return false;
        }
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (edge && !(z == z1 && x <= x0 + 1))
                {
                    c.set(x, 1, z, b.sandbag);
                }
            }
        }
        for (int x = x0 + 1; x <= x1 - 4; x++)
        {
            for (int z = z0 + 1; z <= z0 + 4; z++)
            {
                c.set(x, 1, z, Blocks.wool, 15);                 // the bladder
            }
        }
        for (int x = x0 + 2; x <= x1 - 5; x++)
        {
            c.set(x, 2, z0 + 2, Blocks.carpet, 15);              // its rounded top
            c.set(x, 2, z0 + 3, Blocks.carpet, 15);
        }
        c.prop(x1 - 2, 1, z0 + 1, b.barrel, S);
        c.prop(x1 - 1, 1, z0 + 1, b.barrel, S);
        c.prop(x1 - 2, 1, z0 + 2, b.barrel, S);
        c.prop(x1 - 1, 1, z1 - 2, b.generator, W);
        c.prop(x0 + 2, 1, z1 - 1, b.barrel, S);
        return true;
    }

    /**
     * Motor pool: gravel lot, vehicles in a row nose to the road, conex containers along the
     * back, tyres, drums, a maintenance shelter, light towers, oil stains.
     */
    private boolean motorPool(int x0, int z0, int x1, int z1)
    {
        if (!reserve(x0 - 1, z0, x1, z1))
        {
            return false;
        }
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                c.set(x, 0, z, noise(x, z) > 0.75 ? Blocks.dirt : Blocks.gravel);
                if (noise(x * 7, z * 5) > 0.93)
                {
                    c.set(x, 0, z, Blocks.coal_block);              // oil stain
                }
            }
        }
        poi("motor_pool", x0 + 0.5, 3.6, z1 + 1.5, 200, 14);
        // vehicles: APCs and jeeps, long axis north-south (meta 3 / 5), spaced 6
        int vz = z0 + 2;
        int count = size == SMALL ? 2 : size == MEDIUM ? 3 : 5;
        for (int i = 0; i < count; i++)
        {
            int vx = x0 + 3 + i * 6;
            if (vx + 3 > x1)
            {
                break;
            }
            boolean apc = i % 2 == 1 || (size == LARGE && i == 4);
            c.prop(vx, 1, vz + (apc ? 4 : 2), apc ? b.apc : b.jeep, r.nextBoolean() ? S : N);
        }
        // camo net on posts over the first two vehicles (green wool, holes in it)
        int nx0 = x0 + 1, nx1 = Math.min(x1 - 4, x0 + 13), nz0 = z0, nz1 = z0 + 8;
        for (int x = nx0; x <= nx1; x++)
        {
            for (int z = nz0; z <= nz1; z++)
            {
                if (noise(x * 11, z * 11) > 0.25)
                {
                    c.set(x, 5, z, Blocks.wool, noise(x * 5, z * 7) > 0.6 ? 12 : 13);
                }
            }
        }
        for (int[] p : new int[][] {{nx0, nz0}, {nx1, nz0}, {nx0, nz1}, {nx1, nz1}})
        {
            c.fill(p[0], 1, p[1], p[0], 4, p[1], Blocks.fence);
        }
        for (int z = z0; z <= z1; z++)
        {
            if (z % 6 != 0)
            {
                c.prop(x0 - 1, 1, z, b.tWall, W);           // T-wall screen toward the road
            }
        }
        // conex row along the back (east edge), with loot in some
        int cz = z0;
        for (int k = 0; cz + 6 <= z1 && k < (size == SMALL ? 1 : size == MEDIUM ? 2 : 3); k++, cz += 8)
        {
            conex(x1 - 2, cz, false, k % 2 == 0 ? 1 : 0);
        }
        // maintenance shelter: open sided, posts and a tan roof, a work bench
        int sx = x0 + 1, sz = z1 - 6;
        if (sz > vz + 8)
        {
            for (int x = sx; x <= sx + 5; x++)
            {
                for (int z = sz; z <= sz + 4; z++)
                {
                    c.set(x, 4, z, (x + z) % 2 == 0 ? b.metalGreen : Blocks.wooden_slab, (x + z) % 2 == 0 ? 0 : 1);
                }
            }
            for (int[] p : new int[][] {{sx, sz}, {sx + 5, sz}, {sx, sz + 4}, {sx + 5, sz + 4}})
            {
                c.fill(p[0], 1, p[1], p[0], 3, p[1], Blocks.fence);
            }
            c.prop(sx + 2, 1, sz + 1, b.metalTable, S);
            c.prop(sx + 4, 1, sz + 3, b.tireStack, S);
            c.prop(sx + 1, 1, sz + 3, b.tire, S);
            c.prop(sx + 3, 1, sz + 3, b.storageCrate, S);
        }
        c.prop(x0 + 1, 1, z0, b.lightTower, S);
        c.prop(x1 - 4, 1, z1, b.tireStack, S);
        c.prop(x1 - 5, 1, z1, b.barrel, S);
        return true;
    }

    /** Mortar pit: sandbag ring 5 x 5, 2 high, gap to the road, ammunition inside. */
    private boolean mortarPit(int x0, int z0)
    {
        if (!reserve(x0, z0, x0 + 4, z0 + 4))
        {
            return false;
        }
        for (int x = x0; x <= x0 + 4; x++)
        {
            for (int z = z0; z <= z0 + 4; z++)
            {
                boolean edge = x == x0 || x == x0 + 4 || z == z0 || z == z0 + 4;
                if (edge && !(z == z0 + 4 && x == x0 + 2))
                {
                    c.set(x, 1, z, b.sandbag);
                    c.set(x, 2, z, (x + z) % 2 == 0 ? b.sandbag : b.sandbagBeige);
                }
            }
        }
        c.prop(x0 + 1, 1, z0 + 1, b.ammoCrateLarge, S);
        c.prop(x0 + 3, 1, z0 + 1, b.ammoCrate, S);
        c.prop(x0 + 2, 1, z0 + 2, b.militaryCrate, S);
        return true;
    }

    /** Burn pit: a scorched patch, charred logs, burning barrels, trash bags. */
    private boolean burnPit(int x0, int z0)
    {
        if (!reserve(x0, z0, x0 + 5, z0 + 4))
        {
            return false;
        }
        for (int x = x0; x <= x0 + 5; x++)
        {
            for (int z = z0; z <= z0 + 4; z++)
            {
                c.set(x, 0, z, (x + z) % 3 == 0 ? Blocks.coal_block : Blocks.netherrack);
            }
        }
        c.prop(x0 + 1, 1, z0 + 1, b.canFire, S);
        c.prop(x0 + 4, 1, z0 + 3, b.canFire, S);
        c.prop(x0 + 2, 1, z0 + 3, b.trashBag, S);
        c.prop(x0 + 3, 1, z0 + 1, b.trashBag2, S);
        c.set(x0 + 2, 1, z0 + 2, Blocks.log, 5);
        return true;
    }

    // ================================================================ centre

    /**
     * Tan TEMPER tent (hardened clay vault, plywood floor), long axis north-south, entrances on
     * both ends (south one by the road), kind: 0 aid station, 1 dining, 2 gym.
     */
    private boolean tent(int x0, int z0, int w, int l, int kind)
    {
        int x1 = x0 + w - 1, z1 = z0 + l - 1, mx = x0 + w / 2;
        if (!reserve(x0 - 2, z0 - 1, x1 + 2, z1 + 1))
        {
            return false;
        }
        for (int z = z0; z <= z1; z++)
        {
            boolean end = z == z0 || z == z1;
            for (int x = x0; x <= x1; x++)
            {
                int d = Math.min(x - x0, x1 - x);          // 0 at the sides
                c.set(x, 0, z, Blocks.planks, 2);
                int top = d == 0 ? 2 : d == 1 ? 3 : 4;      // the vault
                for (int y = 1; y <= top; y++)
                {
                    boolean skin = y == top || d == 0 || end;
                    c.set(x, y, z, skin ? Blocks.sandstone : null, skin ? 2 : 0);   // tan canvas
                }
            }
        }
        // entrances on both ends, a light inside every 3 blocks
        for (int z : new int[] {z0, z1})
        {
            c.clear(mx, 1, z);
            c.clear(mx, 2, z);
        }
        for (int z = z0 + 2; z <= z1 - 2; z += 3)
        {
            c.prop(mx, 3, z, b.light, S);
        }
        // guy ropes stand-in: fence posts at the corners outside
        for (int[] p : new int[][] {{x0 - 1, z0}, {x1 + 1, z0}, {x0 - 1, z1}, {x1 + 1, z1}})
        {
            c.set(p[0], 1, p[1], Blocks.fence);
        }
        String[] tentName = {"aid_inside", "dfac_inside", "gym_inside"};
        poi(tentName[kind], mx + 0.5, 1.6, z1 - 0.5, 180, 8);
        switch (kind)
        {
            case 0: // aid station / casualty collection point
                for (int z = z0 + 2; z <= z1 - 2; z += 2)
                {
                    c.prop(x0 + 1, 1, z, b.stretcher, S);
                    c.prop(x1 - 1, 1, z, z % 4 == 0 ? b.medicalCrate : b.stretcher, S);
                }
                c.prop(mx, 1, z0 + 2, b.metalTable, S);
                c.prop(mx, 2, z0 + 2, b.medicalCrate, S);
                c.prop(x1 + 2, 1, z0 + 2, b.bodyBag, S);
                c.prop(x1 + 2, 1, z0 + 5, b.bodyBag, S);
                c.prop(x0 - 2, 1, z1 - 1, b.generator, S);
                break;
            case 1: // DFAC: table rows, serving line, supplies
                for (int z = z0 + 2; z <= z1 - 5; z += 2)
                {
                    c.prop(x0 + 2, 1, z, b.woodTable, S);
                    c.prop(x0 + 1, 1, z, b.chair, E);
                    c.prop(x1 - 2, 1, z, b.woodTable, S);
                    c.prop(x1 - 1, 1, z, b.chair, W);
                }
                for (int x = x0 + 1; x <= x1 - 1; x++)
                {
                    if (x != mx)
                    {
                        c.prop(x, 1, z1 - 2, x % 2 == 0 ? b.cooking : b.metalTable, N);
                    }
                }
                c.prop(x0 + 1, 1, z1 - 1, b.cardboard, S);
                c.prop(x1 - 1, 1, z1 - 1, b.waterPallet, S);
                c.prop(x1 + 2, 1, z1, b.trashcan, S);
                c.prop(x1 + 2, 1, z1 - 1, b.trashBag, S);
                break;
            default: // gym / MWR
                for (int z = z0 + 2; z <= z1 - 2; z += 3)
                {
                    c.set(x0 + 1, 1, z, Blocks.anvil, 0);       // weights
                    c.prop(x1 - 1, 1, z, b.chessTable, S);
                    c.prop(x1 - 2, 1, z, b.chair, E);
                }
                c.prop(mx, 2, z1 - 1, b.wallFlag, N);
                break;
        }
        return true;
    }

    /** Helipad: concrete square with a white H, landing lights, windsock; a wreck on the big base. */
    private boolean helipad(int x0, int z0, boolean wreck)
    {
        int s = 9;
        if (!reserve(x0, z0, x0 + s + 2, z0 + s - 1))
        {
            return false;
        }
        for (int x = x0; x < x0 + s; x++)
        {
            for (int z = z0; z < z0 + s; z++)
            {
                c.set(x, 0, z, Blocks.stone);
            }
        }
        // the H
        for (int z = z0 + 2; z <= z0 + 6; z++)
        {
            c.set(x0 + 2, 0, z, Blocks.quartz_block);
            c.set(x0 + 6, 0, z, Blocks.quartz_block);
        }
        for (int x = x0 + 3; x <= x0 + 5; x++)
        {
            c.set(x, 0, z0 + 4, Blocks.quartz_block);
        }
        for (int[] p : new int[][] {{x0, z0}, {x0 + s - 1, z0}, {x0, z0 + s - 1}, {x0 + s - 1, z0 + s - 1}})
        {
            c.prop(p[0], 1, p[1], b.redLight, S);
        }
        poi("helipad", x0 + 4.5, 6.6, z0 + s + 6.5, 180, 30);
        // windsock: a pole with an orange sleeve
        int wx = x0 + s + 1, wz = z0;
        c.fill(wx, 1, wz, wx, 4, wz, Blocks.fence);
        c.set(wx + 1, 4, wz, Blocks.wool, 1);
        if (wreck)
        {
            c.prop(x0 + 4, 1, z0 + 4, b.helicopter, S);
        }
        else
        {
            c.prop(x0 + s - 2, 1, z0 + 1, b.carePackage, S);
        }
        return true;
    }

    // ================================================================ decay

    /**
     * The base was overrun: a breach in one wall (cells burst, dirt spilled, wire gone), a few
     * cells slumped elsewhere, trash, the odd body bag and open crate. Cosmetic, never on roads.
     */
    private void decay()
    {
        // breach on the north or a side wall
        int side = r.nextInt(3);
        int len = size == SMALL ? 3 : 4;
        if (side == 0)
        {
            int x = wx0 + 8 + r.nextInt(Math.max(1, wx1 - wx0 - 16 - len));
            breach(x, wz0, len, true);
        }
        else
        {
            int z = wz0 + 8 + r.nextInt(Math.max(1, wz1 - wz0 - 16 - len));
            breach(side == 1 ? wx0 : wx1 - 1, z, len, false);
        }
        // slumped cells: lose their top course
        int slumps = size == SMALL ? 3 : size == MEDIUM ? 6 : 10;
        for (int i = 0; i < slumps; i++)
        {
            int x = wx0 + 5 + r.nextInt(Math.max(1, wx1 - wx0 - 10));
            int z = r.nextBoolean() ? wz0 : wz0 + 1;
            if (c.get(x, 3, z) == b.hesco && c.get(x, 4, z) == null)
            {
                c.clear(x, 3, z);
                c.set(x, 0, z + (z == wz0 ? -1 : 1), Blocks.dirt, 1);
            }
        }
        // trash and bags along the inner ring road edges
        int litter = size == SMALL ? 6 : size == MEDIUM ? 12 : 20;
        for (int i = 0; i < litter; i++)
        {
            int x = ix0 + 3 + r.nextInt(Math.max(1, ix1 - ix0 - 6));
            int z = iz0 + 3 + r.nextInt(Math.max(1, iz1 - iz0 - 6));
            if (c.get(x, 1, z) == null && c.get(x, 0, z) != Blocks.gravel && c.get(x, 0, z) != null)
            {
                float roll = r.nextFloat();
                c.prop(x, 1, z, roll < 0.5f ? b.trashBag : roll < 0.8f ? b.trashBag2 : roll < 0.9f ? b.woodCrateOpen : b.barrel, r.nextInt(4) + 2);
            }
        }
    }

    private void breach(int x0, int z0, int len, boolean alongX)
    {
        for (int k = 0; k < len; k++)
        {
            for (int t = 0; t < 2; t++)
            {
                int x = alongX ? x0 + k : x0 + t, z = alongX ? z0 + t : z0 + k;
                c.clearBox(x, 1, z, x, 3, z);
                c.set(x, 0, z, Blocks.dirt, 1);
                if (k == 0 || k == len - 1)
                {
                    c.set(x, 1, z, b.hesco);               // stumps at the ends
                }
            }
            // spilled fill on both sides, the wire there gone
            int ox = alongX ? x0 + k : x0 - 1, oz = alongX ? z0 - 1 : z0 + k;
            int px = alongX ? x0 + k : x0 + 2, pz = alongX ? z0 + 2 : z0 + k;
            c.set(ox, 1, oz, Blocks.dirt, 1);
            c.set(px, 1, pz, Blocks.gravel);
            int wx = alongX ? x0 + k : x0 - 2, wz = alongX ? z0 - 2 : z0 + k;
            c.clear(wx, 1, wz);
        }
    }

    // ================================================================ blocks

    /** Decimation blocks by registry name (resolved once). */
    static final class B
    {
        private static B instance;

        Block hesco, sandbag, sandbagBeige, wire, tWall, jersey, hedgehog, sawhorse, streetBarrier;
        Block militaryCrate, ammoCrate, ammoCrateLarge, medicalCrate, woodCrate, woodCrateOpen, storageCrate,
            weaponCabinet, carePackage;
        Block radio, radioSmall, radioTower, spotlight, generator, lightTower, flagPole, wallFlag, keycard,
            redLight, light, monitor;
        Block waterPallet, waterPalletTarp, barrel, tire, tireStack, cardboard, trashBag, trashBag2, trashcan,
            canFire, bodyBag, stretcher, cooking, chessTable;
        Block metalTable, woodTable, chair, officeChair;
        Block jeep, apc, helicopter;
        Block metalGreen, metalRust, metalPlate, concrete, concreteFloor, metalDoor;

        static B get()
        {
            if (instance == null)
            {
                instance = new B();
            }
            return instance;
        }

        private B()
        {
            hesco = d("BlockMilitaryBarrier", Blocks.sandstone);
            sandbag = d("BlockSandbagStack", Blocks.sandstone);
            sandbagBeige = d("BlockSandbagStackBeige", Blocks.sandstone);
            wire = d("BlockConcertinaWire", Blocks.web);
            tWall = d("BlockBarrierTall", Blocks.stone);
            jersey = d("BlockBarrier", Blocks.stone_slab);
            hedgehog = d("BlockHedgehog", null);
            sawhorse = d("BlockHazardbarrier", Blocks.fence);
            streetBarrier = d("BlockStreetBarrier", null);
            militaryCrate = d("BlockMilitaryCrate", Blocks.chest);
            ammoCrate = d("BlockAmmoCrate", Blocks.chest);
            ammoCrateLarge = d("BlockAmmoCrateLarge", Blocks.chest);
            medicalCrate = d("BlockMedicalCrate", Blocks.chest);
            woodCrate = d("BlockWoodCrate", Blocks.chest);
            woodCrateOpen = d("BlockWoodCrateOpen", null);
            storageCrate = d("BlockStorageCrate", Blocks.chest);
            weaponCabinet = d("BlockWeaponCabinet", null);
            carePackage = d("BlockCarePackage", null);
            radio = d("BlockMilitaryRadio", null);
            radioSmall = d("BlockMilitaryRadioSmall", null);
            radioTower = d("BlockRadioTower", Blocks.iron_bars);
            spotlight = d("BlockSpotlight", null);
            generator = d("BlockPowerGenerator", null);
            lightTower = d("BlockHazardLight", null);
            flagPole = d("BlockFlagPollUAHD", null);
            wallFlag = d("BlockWallflag", null);
            keycard = d("BlockKeycardScreenMilitary", null);
            redLight = d("BlockRedlight", null);
            light = d("BlockLight", null);
            monitor = d("BlockMonitor", null);
            waterPallet = d("BlockWaterPallet", null);
            waterPalletTarp = d("BlockWaterPalletTarp", null);
            barrel = d("BlockBarrel", null);
            tire = d("BlockTire", null);
            tireStack = d("BlockTireStack", null);
            cardboard = d("BlockCardboardBoxes1", null);
            trashBag = d("BlockTrashBag1", null);
            trashBag2 = d("BlockTrashBag2", null);
            trashcan = d("BlockTrashcan", null);
            canFire = d("BlockCanFire", null);
            bodyBag = d("BlockBodyBagGreen", null);
            stretcher = d("BlockStretcher", null);
            cooking = d("BlockCookingStation", null);
            chessTable = d("BlockChesstable", null);
            metalTable = d("BlockMetalTable", null);
            woodTable = d("BlockWoodTable", null);
            chair = d("BlockChair", null);
            officeChair = d("BlockOfficeChair", null);
            jeep = d("BlockWreckageMilitary1", null);
            apc = d("BlockWreckageMilitary2", null);
            helicopter = d("BlockWreckageMilitary3", null);
            metalGreen = d("BlockMetalWall", Blocks.iron_block);
            metalRust = d("BlockMetal_3", Blocks.iron_block);
            metalPlate = d("BlockMetal_2", Blocks.iron_block);
            concrete = d("BlockStone_7", Blocks.stone);
            concreteFloor = d("BlockStone_6", Blocks.stone);
            metalDoor = d("Door_Metal_3", Blocks.iron_door);
        }

        private static Block d(String name, Block fallback)
        {
            Block b = Block.getBlockFromName("deci:" + name);
            return b != null && b != Blocks.air ? b : fallback;
        }
    }
}
