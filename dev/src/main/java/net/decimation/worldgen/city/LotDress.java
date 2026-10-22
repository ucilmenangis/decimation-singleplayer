package net.decimation.worldgen.city;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import static net.decimation.worldgen.city.LcCity.hl;

/**
 * The city dressing of an open lot (docs/city_dressing.md "Parks and lots"): a park part gets what
 * a real park has beside its benches (bins, a chess table, a lamp, a bicycle) and, now and then, a
 * body; a bare lot gets one scene or none: cars left in rows (all the same way: research "lot
 * wrecks stay aligned in their stalls"), a survivor's spot (boxes, a fire can, trash), a dumping
 * ground (tyres, barrels, crates, a dumpster against the edge). Every prop through Dress.place with
 * a metadata from PropFacing.
 */
final class LotDress
{
    private final LotPlan p;
    private final Dress d;
    private final int[][] stand = new int[16][16];
    private final int district;
    private int salt;

    private LotDress(LotPlan p, Dress d)
    {
        this.p = p;
        this.d = d;
        this.district = StreetDress.district(p.style);
    }

    static Dress dress(final LotPlan p)
    {
        final int[] tmp = new int[1];
        Dress.Base base = new Dress.Base()
        {
            public Block block(int x, int y, int z)
            {
                return x < 0 || z < 0 || x > 15 || z > 15 ? null : p.baseAt(x, y, z, tmp);
            }

            public int meta(int x, int y, int z)
            {
                p.baseAt(x, y, z, tmp);
                return tmp[0];
            }
        };
        Dress d = new Dress(base, 16, Math.max(4, p.height() + 2), 16);
        LotDress l = new LotDress(p, d);
        l.spots();
        if (p.hasPark())
        {
            l.park();
        }
        else
        {
            l.bare();
        }
        if (l.district == StreetDress.WASTELAND)
        {
            d.lootKinds(net.decimation.fixes.LootBlocks.MILITARY);   // shelves of the military area hold military loot
        }
        Dress.logTotals();
        return d;
    }

    private int roll()
    {
        return (int) (hl(p.seed ^ 0x4C4F5444L ^ (++salt) * 0x9E3779B97F4A7C15L, p.minX(), p.minZ()) % 1000);
    }

    private static Block deci(String name)
    {
        Block b = Block.getBlockFromName("deci:" + name);
        return b == Blocks.air ? null : b;
    }

    /** The layer a prop stands at on each column: the first open cell over a real top, -1 none. */
    private void spots()
    {
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                stand[x][z] = -1;
                for (int y = 1; y < d.h - 1; y++)
                {
                    Block here = d.block(x, y, z), below = d.block(x, y - 1, z);
                    if ((here == null || BuildingLayout.cover(here)) && d.block(x, y + 1, z) == null && below != null
                        && Dress.floorBlock(below, d.meta(x, y - 1, z)))
                    {
                        stand[x][z] = y;
                        break;
                    }
                }
            }
        }
    }

    private boolean put(String name, int x, int z, int meta)
    {
        Block b = deci(name);
        return b != null && meta >= 0 && x >= 0 && z >= 0 && x < 16 && z < 16 && stand[x][z] > 0
            && d.place(b, meta, x, stand[x][z], z, 0);
    }

    private boolean face(String name, int x, int z, int dir)
    {
        Block b = deci(name);
        return b != null && put(name, x, z, PropFacing.meta(b, dir));
    }

    /** Tries a prop on a few random cells until it fits. */
    private boolean somewhere(String name, int dir, int tries)
    {
        for (int k = 0; k < tries; k++)
        {
            int x = 1 + roll() % 14, z = 1 + roll() % 14;
            if (dir < 0 ? put(name, x, z, 2 + (roll() & 3)) : face(name, x, z, dir))
            {
                return true;
            }
        }
        return false;
    }

    /** Park: a bin beside each bench (2 along), one chess table, a lamp, a bicycle, maybe a body. */
    private void park()
    {
        Block bench = deci("BlockStreetBench");
        int bins = 0;
        for (int x = 0; x < 16 && bins < 3; x++)
        {
            for (int z = 0; z < 16 && bins < 3; z++)
            {
                for (int y = 1; y < d.h - 1; y++)
                {
                    if (d.block(x, y, z) == bench && bench != null)
                    {
                        for (int[] o : new int[][] {{2, 0}, {-2, 0}, {0, 2}, {0, -2}})
                        {
                            if (put("BlockStreetBin", x + o[0], z + o[1], 2))
                            {
                                bins++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (roll() < 350)
        {
            somewhere("BlockChesstable", -1, 12);
        }
        if (roll() < 450)
        {
            somewhere("BlockStreetLight", roll() & 3, 12);
        }
        if (district != StreetDress.WASTELAND && roll() < 250)
        {
            somewhere("BlockBicycle", -1, 10);
        }
        if (roll() < (district == StreetDress.WASTELAND ? 350 : 100))
        {
            somewhere("BlockSkeletonGround", roll() & 3, 10);
        }
        if (roll() < 300)
        {
            for (int k = 0; k < 2 + roll() % 3; k++)
            {
                somewhere(roll() < 500 ? "BlockTrashBag1" : "BlockTrashBag2", -1, 6);
            }
        }
    }

    /** Bare lot: one scene or nothing. */
    private void bare()
    {
        if (district == StreetDress.WASTELAND)
        {
            militaryLot();
            return;
        }
        int r = roll();
        cpw.mods.fml.common.FMLLog.info("[deciworldgen] dress lot %s at %d,%d", r < 280 ? "cars" : r < 450 ? "camp"
            : r < 620 ? "dump" : "empty", p.minX(), p.minZ());
        if (r < 280)
        {
            parkedCars();
        }
        else if (r < 450)
        {
            // a survivor's spot: boxes against each other, a fire can, bags, in the wasteland a body
            int x = 4 + roll() % 8, z = 4 + roll() % 8;
            put("BlockCanFire", x, z, 2);
            String[] around = {"BlockCardboardBoxes1", "BlockCardboardBoxes3", "BlockTrashBag1", "BlockTrashBag2",
                               "BlockWoodCrateOpen", "BlockLantern"};
            int[][] o = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
            for (int k = 0; k < o.length; k++)
            {
                put(around[(k + roll()) % around.length], x + o[k][0], z + o[k][1], 2 + (roll() & 3));
            }
            if (district == StreetDress.WASTELAND && roll() < 500)
            {
                face("BlockSkeletonGround", x + 3, z, roll() & 3);
            }
        }
        else if (r < 620)
        {
            // a dumping ground against one edge: tyres, barrels, crates, a dumpster with its lid edge out
            String[] junk = {"BlockTireStack", "BlockTire", "BlockBarrel", "BlockBarrel", "BlockWoodCrate",
                             "BlockCardboardBoxes2", "BlockTrashBag1", "BlockTrashBag2"};
            int edge = roll() & 3;
            for (int k = 0; k < 8; k++)
            {
                int a = 2 + roll() % 12, b = 1 + roll() % 3;
                int x = edge == 0 ? 15 - b : edge == 2 ? b : a, z = edge == 1 ? 15 - b : edge == 3 ? b : a;
                put(junk[roll() % junk.length], x, z, 2 + (roll() & 3));
            }
            int a = 4 + roll() % 8;
            int x = edge == 0 ? 12 : edge == 2 ? 3 : a, z = edge == 1 ? 12 : edge == 3 ? 3 : a;
            face("BlockDumpster", x, z, (edge + 2) & 3);
        }
    }

    /** Two rows of cars left in a lot, every car the same way (noses toward the middle line). */
    private void parkedCars()
    {
        String[] cars = {"BlockWreckage1", "BlockWreckage2", "BlockWreckage3", "BlockWreckage4", "BlockWreckage5",
                         "BlockTruckWreckage1", "BlockTruckWreckage3", "BlockTruckWreckage6"};
        boolean rowsAlongX = roll() < 500;               // cars stand side by side along x, long along z
        for (int row = 0; row < 2; row++)
        {
            int across = row == 0 ? 4 : 11;               // rows' anchors across, noses toward the middle
            int dir = rowsAlongX ? (row == 0 ? 1 : 3) : (row == 0 ? 0 : 2);
            for (int k = 0; k < 4; k++)
            {
                if (roll() < 350)
                {
                    continue;                             // an empty stall
                }
                int along = 2 + k * 4;
                int x = rowsAlongX ? along : across, z = rowsAlongX ? across : along;
                String name = cars[roll() % cars.length];
                face(name, x, z, dir);
            }
        }
    }

    // ------------------------------------------------------------------ the military area

    /**
     * A bare lot of the wasteland district, the city's military area (docs/wasteland.md): a motor
     * pool (vehicles side by side, all one way, fuel and spares at the edge), a supply dump (rows of
     * military crates, ammo cases, care packages, water pallets, a spotlight), a crashed
     * helicopter with its dead, a casualty collection point (body bags in rows, stretchers, medical
     * crates, a hazard light), or nothing. Crates and cases are Decimation loot.
     */
    /** Dev test only (Wasteland.previewLot): the military lot scene to build, -1 = by the seed. */
    static int force = -1;

    private void militaryLot()
    {
        int r = force >= 0 ? new int[] {100, 300, 600, 760}[force & 3] : roll();
        String scene = r < 250 ? "motorpool" : r < 520 ? "supplies" : r < 680 ? "helicopter" : r < 850 ? "casualties" : "empty";
        cpw.mods.fml.common.FMLLog.info("[deciworldgen] dress lot military %s at %d,%d", scene, p.minX(), p.minZ());
        if (r < 850)
        {
            groundWork(r < 520);                          // a concrete pad under the vehicles and the stores
        }
        if (r < 250)
        {
            motorPool();
        }
        else if (r < 520)
        {
            supplyDump();
        }
        else if (r < 680)
        {
            helicopter();
        }
        else if (r < 850)
        {
            casualties();
        }
    }

    /**
     * The army cleared the lot it used: the dead bushes and flowers on it go (a drawn vehicle or crate
     * box would run into them); a motor pool and a supply dump stand on a concrete pad (double stone
     * slab, the sidewalk's block) with a grass border. Never gravel: it falls once a block update
     * reaches it over a cellar or a cave (the sky test lost its whole gravel ground that way).
     */
    private void groundWork(boolean pad)
    {
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int y = stand[x][z];
                if (y <= 0)
                {
                    continue;
                }
                Block here = d.block(x, y, z);
                if (here != null && BuildingLayout.cover(here))
                {
                    d.put(null, 0, x, y, z);
                }
                Block ground = d.block(x, y - 1, z);
                if (pad && x > 0 && z > 0 && x < 15 && z < 15 && (ground == Blocks.grass || ground == Blocks.dirt))
                {
                    d.put(Blocks.double_stone_slab, 0, x, y - 1, z);
                }
            }
        }
    }

    /**
     * The side of the lot a city street runs along (0 E, 1 S, 2 W, 3 N: the chunk beyond it is a
     * street chunk), from a random first side; a random side when none does (a superblock's inner lot,
     * the dev test's sky lot).
     */
    private int streetSide()
    {
        int chx = p.minX() >> 4, chz = p.minZ() >> 4, first = roll() & 3;
        for (int t = 0; t < 4; t++)
        {
            int s = (first + t) & 3;
            int nx = chx + (s == 0 ? 1 : s == 2 ? -1 : 0), nz = chz + (s == 1 ? 1 : s == 3 ? -1 : 0);
            if (LcCity.isCity(p.seed, Math.floorDiv(nx, LcCity.CELL), Math.floorDiv(nz, LcCity.CELL))
                && LcCity.isStreetChunk(p.seed, nx, nz))
            {
                return s;
            }
        }
        return first;
    }

    /** Lot x of the cell u along the street edge on side s and v in from it (u, v 0..15). */
    private static int ux(int s, int u, int v)
    {
        return s == 0 ? 15 - v : s == 2 ? v : s == 1 ? 15 - u : u;
    }

    /** Lot z of the cell u along the street edge on side s and v in from it. */
    private static int uz(int s, int u, int v)
    {
        return s == 1 ? 15 - v : s == 3 ? v : s == 0 ? u : 15 - u;
    }

    /** A prop at (u, v) from the street side s, its front toward dir. */
    private boolean faceUv(String name, int s, int u, int v, int dir)
    {
        return face(name, ux(s, u, v), uz(s, u, v), dir);
    }

    /** A long prop at (u, v) from the street side s, its long side along the street edge. */
    private boolean alongUv(String name, int s, int u, int v)
    {
        Block b = deci(name);
        return b != null && put(name, ux(s, u, v), uz(s, u, v), PropFacing.along(b, (s & 1) == 1, roll() & 1));
    }

    /**
     * A motor pool as the army lays one out (vehicles in a line, noses toward the way out for a fast
     * dispatch; maintenance and fuel behind them): three bays along the street side, noses to the
     * street (now and then an empty bay, an APC in the middle one), a workshop row along the back edge
     * (parts shelves and a workbench along the edge, tyres, a generator, a work light), a fuel point of
     * drums in one back corner; a second line of jeeps behind the first (6 in 10 pools), else at times
     * a towed missile launcher parked there. v0.54.1 had the bays only, in the middle: two jeeps on an
     * empty pad.
     */
    private void motorPool()
    {
        int s = streetSide(), back = (s + 2) & 3;
        int vehicles = 0;
        int[] bays = {3, 8, 13};
        for (int k = 0; k < bays.length; k++)
        {
            if (roll() < 150)
            {
                continue;                                 // a bay out on a run
            }
            boolean apc = k == 1 && roll() < 500;
            boolean ok = false;
            // drawn 4 (jeep) and 8.9 (APC) long, reaching about 1.8 / 4.3 ahead of their block
            for (int v : apc ? new int[] {5, 6} : new int[] {3, 4})
            {
                ok = ok || faceUv(apc ? "BlockWreckageMilitary2" : "BlockWreckageMilitary1", s, bays[k], v, s);
            }
            if (!ok && apc)
            {
                ok = faceUv("BlockWreckageMilitary1", s, bays[k], 3, s);
            }
            vehicles += ok ? 1 : 0;
        }
        if (vehicles == 0)
        {
            faceUv("BlockWreckageMilitary1", s, 8, 3, s);
        }
        // a busy pool: a second line of jeeps behind the first, the same way (an APC's tail refuses one)
        boolean second = roll() < 600;
        for (int k = 0; second && k < bays.length; k++)
        {
            if (roll() < 700 && !faceUv("BlockWreckageMilitary1", s, bays[k], 9, s))
            {
                faceUv("BlockWreckageMilitary1", s, bays[k], 10, s);
            }
        }
        // the fuel point: drums close together in a back corner, a cone at its corner
        boolean fuelLow = roll() < 500;
        int fu = fuelLow ? 1 : 13;
        for (int[] o : new int[][] {{0, 13}, {1, 13}, {0, 14}, {1, 14}, {0, 12}})
        {
            if (roll() < 800)
            {
                faceUv("BlockBarrel", s, fu + o[0], o[1], s);
            }
        }
        faceUv("BlockCone", s, fuelLow ? fu + 2 : fu - 1, 12, s);
        // the workshop along the back edge from the other corner: each part steps by its drawn length
        String[] shop = roll() < 500
            ? new String[] {"BlockMetalShelf", "BlockMetalTable", "BlockMetalShelf_Empty", "BlockTireStack", "BlockPowerGenerator"}
            : new String[] {"BlockTireStack", "BlockMetalShelf", "BlockMetalTable", "BlockPowerGenerator", "BlockMetalShelf"};
        int u = fuelLow ? 4 : 1, end = fuelLow ? 14 : 11;
        for (int k = 0; k < shop.length && u <= end; )
        {
            boolean longOne = shop[k].startsWith("BlockMetal");
            boolean ok = longOne ? alongUv(shop[k], s, u, 14) : faceUv(shop[k], s, u, 14, s);
            if (ok)
            {
                u += longOne ? 3 : 2;
                k++;
            }
            else
            {
                u++;
            }
        }
        // a work light over the workshop, lenses into the yard (it only turns north or south)
        int lu = fuelLow ? 14 : 1;
        if (!faceUv("BlockHazardLight", s, lu, 12, back) && !faceUv("BlockHazardLight", s, lu, 12, uz(s, lu, 12) < 8 ? 1 : 3))
        {
            Dress.note("~wasteland motor pool work light did not fit");
        }
        if (!second && roll() < 500)
        {
            faceUv("BlockMissileLauncher", s, 8, 11, s);   // towed in, noses to the street like the rest
        }
        faceUv("BlockTire", s, fuelLow ? 6 : 9, 12, s);
    }

    /** Two rows of military crates, ammo cases, care packages, water pallets, a spotlight. */
    private void supplyDump()
    {
        boolean alongX = roll() < 500;
        int placed = 0;
        for (int row = 0; row < 2; row++)
        {
            int across = row == 0 ? 5 : 10;
            int front = alongX ? (row == 0 ? 3 : 1) : (row == 0 ? 2 : 0);   // labels toward the aisle between the rows
            for (int a = 3; a <= 12; a++)
            {
                if (roll() < 250)
                {
                    continue;
                }
                int x = alongX ? a : across, z = alongX ? across : a;
                // never stacked: a crate is drawn 1.15 high, one on another would overlap it by 0.15
                placed += face("BlockMilitaryCrate", x, z, front) ? 1 : 0;
            }
        }
        somewhere("BlockAmmoCrateLarge", roll() & 3, 10);
        somewhere("BlockAmmoCrate", roll() & 3, 10);
        for (int k = 0; k < 2; k++)
        {
            somewhere("BlockCarePackage", -1, 10);
        }
        somewhere("BlockWaterPalletTarp", -1, 10);
        somewhere("BlockSpotlight", roll() & 3, 10);
        if (placed == 0)
        {
            somewhere("BlockMilitaryCrate", roll() & 3, 10);
        }
    }

    /**
     * A crash site: the helicopter where it came down, the ground burnt and torn up along its hull
     * (stained clay black, dark brown, grey: podzol read orange from above, and grass never spreads onto
     * clay), its crew and cargo thrown out (two dead, a body bag, a military crate, an ammo case, a
     * wheel), and the army's cordon (sawhorse barriers and cones on the street side, on the hull's long
     * sides when the rotor fills that edge) with a work light. v0.54.1 left the wreck on clean grass: a
     * helicopter parked on a lawn. No rubble lumps: the drawn box of the rotor disc covers the ground
     * round the hull (propclash counted them as inside the wreck).
     */
    private void helicopter()
    {
        // drawn 9.4 x 14 and lopsided (facing north or west it reaches 8.3 past its block, south or east 6.7):
        // every heading from a random one, the anchor shifted so the long side stays on the lot
        Block heli = deci("BlockWreckageMilitary3");
        float[] box = null;
        int first = roll() & 3, hx = 0, hz = 0, hdir = 0;
        for (int t = 0; t < 4 && box == null && heli != null; t++)
        {
            int dir = (first + t) & 3;
            for (int a : new int[] {8, 7, 9})
            {
                int x = dir == 2 ? a - 1 : a, z = dir == 3 ? a - 1 : a;
                if (box == null && face("BlockWreckageMilitary3", x, z, dir))
                {
                    box = PropGeom.box(heli, PropFacing.meta(heli, dir), x, stand[x][z], z);
                    hx = x;
                    hz = z;
                    hdir = dir;
                }
            }
        }
        if (box == null)
        {
            Dress.note("~wasteland helicopter did not fit");
            return;
        }
        // torn ground along the hull (the drawn box is mostly rotor disc): most cells within 1.5 of the
        // hull's line, fewer out to 3.5, a short skid past the nose and the tail
        boolean alongZ = (hdir & 1) == 1;
        float lo = alongZ ? box[4] : box[0], hi = alongZ ? box[5] : box[1], mid = (alongZ ? hx : hz) + 0.5f;
        for (int x = 1; x < 15; x++)
        {
            for (int z = 1; z < 15; z++)
            {
                int y = stand[x][z];
                float a = (alongZ ? z : x) + 0.5f, c = Math.abs((alongZ ? x : z) + 0.5f - mid);
                float past = Math.max(lo - a, a - hi);
                int chance = past <= 0 ? (c <= 1.5f ? 800 : c <= 3.5f ? 400 : 0) : past <= 2 && c <= 2.5f ? 300 : 0;
                Block ground = y > 0 ? d.block(x, y - 1, z) : null;
                if ((ground == Blocks.grass || ground == Blocks.dirt) && chance > 0 && roll() < chance)
                {
                    // burnt black along the hull, dark brown and grey round it
                    int r = roll();
                    d.put(Blocks.stained_hardened_clay, past <= 0 && c <= 1.5f && r < 600 ? 15 : r < 650 ? 12 : 7, x, y - 1, z);
                }
            }
        }
        for (int k = 0; k < 2; k++)
        {
            somewhere("BlockSkeletonGround", roll() & 3, 12);
        }
        somewhere("BlockBodyBagGreen", roll() & 3, 20);
        somewhere("BlockMilitaryCrate", roll() & 3, 12);
        somewhere("BlockAmmoCrate", roll() & 3, 12);
        somewhere("BlockTire", -1, 8);
        // the cordon: a sawhorse, a cone, a gap, along the street side, and along the hull's long sides
        // where the rotor leaves room when the street side is mostly taken (the hull across the street)
        int s = streetSide();
        int barriers = 0;
        int[] sides = alongZ ? new int[] {s, 0, 2} : new int[] {s, 1, 3};
        for (int i = 0; i < sides.length && barriers < 4; i++)
        {
            if (i > 0 && sides[i] == s)
            {
                continue;
            }
            for (int u = 2; u <= 13; u++)
            {
                barriers += u % 3 == 2 && alongUv("BlockHazardbarrier", sides[i], u, 1) ? 1 : 0;
                if (u % 3 == 0)
                {
                    faceUv("BlockCone", sides[i], u, 1, sides[i]);
                }
            }
        }
        int lu = roll() < 500 ? 1 : 14;
        if (!faceUv("BlockHazardLight", s, lu, 2, (s + 2) & 3) && !faceUv("BlockHazardLight", s, lu, 2, uz(s, lu, 2) < 8 ? 1 : 3))
        {
            Dress.note("~wasteland crash site work light did not fit");
        }
        if (barriers == 0)
        {
            Dress.note("~wasteland crash site cordon did not fit");
        }
    }

    /** Body bags in rows, stretchers, medical crates and a hazard light by the edge. */
    private void casualties()
    {
        String[] bags = {"BlockBodyBagBlack", "BlockBodyBagGreen", "BlockBodyBagBlue", "BlockBodyBagOrange"};
        boolean alongX = roll() < 500;
        for (int row = 0; row < 3; row++)
        {
            int across = 4 + row * 3;
            for (int a = 3; a <= 12; a += 2)
            {
                if (roll() < 300)
                {
                    continue;
                }
                int x = alongX ? a : across, z = alongX ? across : a;
                face(bags[roll() % bags.length], x, z, alongX ? 1 : 0);
            }
        }
        for (int k = 0; k < 2; k++)
        {
            somewhere("BlockStretcher", roll() & 3, 10);
        }
        somewhere("BlockMedicalCrate", roll() & 3, 10);
        somewhere("BlockMedicalCrate", roll() & 3, 10);
        somewhere("BlockHazardLight", roll() & 3, 10);
        if (roll() < 500)
        {
            somewhere("BlockSkeletonGround", roll() & 3, 10);
        }
    }
}
