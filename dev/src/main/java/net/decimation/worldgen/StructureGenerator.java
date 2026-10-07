package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Places ruined structures from loaded schematics into the overworld.
 *
 * Placement model: the world is divided into cells of CELL x CELL chunks.
 * Each cell deterministically (world seed + cell coords) decides whether it
 * hosts a structure, which schematic, and in which of its chunks. Every chunk
 * in the cell computes the same answer, so placement is idempotent and needs
 * no cross-chunk state; the structure is placed when its anchor chunk
 * populates. Cell spacing also guarantees structures never overlap.
 *
 * Sectors: the world is further divided into REGION x REGION-chunk sectors
 * (wilderness / civilian / city / military), each with its own schematic pool
 * (by filename prefix: civ_ / city_ / mil_, untagged = everywhere) and its
 * own structure density.
 *
 * Blending: schematic block id 7 (bedrock) is the SKIP marker - those cells
 * never touch the world, so terrain and vegetation survive around and above
 * the ruin. Explicit air in the schematic still clears (rooms, doorways).
 * The structure floor replaces the top ground layer (flush, not perched).
 *
 * The whole structure is written in one call, anchored at +8/+8 with a
 * footprint capped at 24x24 - that keeps every write inside the 2x2 chunk
 * region guaranteed loaded during population, so no cascading chunk
 * generation, ever.
 *
 * Same defensive contract as always: a generator that throws during
 * population crashes the game, so every failure path returns silently or
 * logs and self-disables.
 */
public class StructureGenerator implements IWorldGenerator
{
    /** Cell edge in chunks; one potential structure per cell. */
    private static final int CELL = 4;
    /** Sector edge in chunks (16 chunks = 256 blocks). */
    /** Max footprint that stays inside the safe population window. */
    public static final int MAX_FOOTPRINT = 24;
    /** Corner-to-corner ground height spread beyond which the site is rejected. */
    private static final int MAX_SLOPE = 6;
    /** How deep the dirt foundation may reach below the floor. */
    private static final int MAX_FOUNDATION = 8;
    /** Width of the graded terrain ring around the footprint. */
    private static final int RING = 3;
    /** City street width in blocks, along each cell's west and north edge. */
    private static final int STREET_WIDTH = 5;
    /** Sidewalk width on every side of a city block. */
    private static final int SIDEWALK = 2;
    /** One car slot every this many blocks along a lane, filled with CAR_CHANCE / 256. */
    private static final int CAR_SPACING = 9;
    private static final int CAR_CHANCE = 64;
    /** How far above a street plants and tree parts are cleared. */
    private static final int STREET_CLEARANCE = 8;
    /** Zone extends this many blocks past the footprint on every side. */
    private static final int ZONE_MARGIN = 8;
    /** Schematic block id that means "leave the world untouched here". */
    private static final int SKIP_ID = 7; // bedrock, never used in ruins

    // sector kinds
    public static final int WILD = 0, CIV = 1, CITY = 2, MIL = 3;
    private static final float[] SECTOR_CHANCE = {0.2f, 0.55f, 0.9f, 0.45f};
    private static final String[] SECTOR_NAME = {"wild", "civ", "city", "mil"};

    /** Substitution target: prop candidates + how to orient them. */
    public static final class Sub
    {
        public static final int PLAIN = 0;      // meta 0, e.g. road surface
        public static final int RANDOM = 1;     // random facing per position
        public static final int FIXED = 2;      // south + structure rotation
        public static final int FACE_ROAD = 3;  // toward adjacent road, else FIXED
        public static final int FACE_ROAD_AWAY = 4; // away from road (street lights)

        final Block[] targets;
        final int mode;

        public Sub(int mode, Block[] targets)
        {
            this.mode = mode;
            this.targets = targets;
        }
    }

    private final List<Schematic> schematics;
    /** key = (schematic block id << 4) | schematic metadata */
    private final java.util.Map<Integer, Sub> substitutions;
    /** every Decimation road-surface block, for FACE_ROAD detection */
    private final Set<Block> roadBlocks;
    /** Surface for generated city streets; null disables street painting. */
    private final Block streetBlock;
    /** Procedural city blocks for CITY sectors; null keeps the old city_ schematics. */
    private final CityDistrict city;
    /** Large schematics, any size, written slice by slice. */
    private final LargeSites large;
    /** Car wreck props parked on city streets (may be empty). */
    private Block[] cars = new Block[0];

    public void setCars(Block[] cars)
    {
        this.cars = cars;
    }
    private boolean disabled;
    private int errors;
    private boolean loggedBadId;

    public StructureGenerator(List<Schematic> schematics,
                              java.util.Map<Integer, Sub> substitutions,
                              Set<Block> roadBlocks,
                              Block streetBlock,
                              CityDistrict city,
                              LargeSites large)
    {
        this.schematics = schematics;
        this.substitutions = substitutions;
        this.roadBlocks = roadBlocks;
        this.streetBlock = streetBlock;
        this.city = city;
        this.large = large;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        if (disabled || schematics.isEmpty())
        {
            return;
        }
        try
        {
            generateImpl(chunkX, chunkZ, world);
        }
        catch (Throwable t)
        {
            errors++;
            if (errors <= 3)
            {
                FMLLog.info("[%s] structure generator error in chunk %d,%d: %s",
                            DecimationWorldGen.MODID, chunkX, chunkZ, t.toString());
            }
            if (errors >= 3)
            {
                disabled = true;
                FMLLog.info("[%s] structure generator disabled after %d errors",
                            DecimationWorldGen.MODID, errors);
            }
        }
    }

    /** Which sector a chunk belongs to - deterministic per world seed. */
    private int sector(World world, int chunkX, int chunkZ)
    {
        return Sectors.sector(world.getSeed(), chunkX, chunkZ);
    }

    /** Sector of a chunk, for the city planner. */
    int sectorOf(World world, int chunkX, int chunkZ)
    {
        return sector(world, chunkX, chunkZ);
    }

    /** Y of the top soil block: ground() minus trunks, leaves and plants. */
    static int soilTop(World world, int x, int z)
    {
        int y = ground(world, x, z) - 1;
        while (y > 4 && clearable(world.getBlock(x, y, z)))
        {
            y--;
        }
        return y;
    }

    /** Water directly above a soil block (ground() skips water). */
    static boolean waterAbove(World world, int x, int y, int z)
    {
        return isWater(world, x, y + 1, z);
    }

    /** Schematics allowed in a sector: its own prefix + untagged ones. */
    private List<Schematic> pool(int sector)
    {
        List<Schematic> out = new ArrayList<Schematic>();
        for (Schematic s : schematics)
        {
            boolean civ = s.name.startsWith("civ_");
            boolean city = s.name.startsWith("city_");
            boolean mil = s.name.startsWith("mil_");
            boolean tagged = civ || city || mil;
            if (!tagged
                || (sector == CIV && civ)
                || (sector == CITY && city)
                || (sector == MIL && mil))
            {
                out.add(s);
            }
        }
        return out;
    }

    private void generateImpl(int chunkX, int chunkZ, World world)
    {
        if (world.provider == null || world.provider.dimensionId != 0)
        {
            return; // overworld only
        }

        int sector = sector(world, chunkX, chunkZ);
        if (sector == CITY && streetBlock != null)
        {
            paintStreets(world, chunkX, chunkZ);
        }
        // Slice writers run for EVERY chunk: a population window reaches 8
        // blocks into the neighbouring chunks, so it can cross a sector
        // border, and the structure it touches may belong to the other
        // sector. Both writers check the sector of each structure themselves.
        // (Gating them on this chunk's sector left wall strips unwritten.)
        if (city != null)
        {
            city.populate(world, chunkX, chunkZ, this);
        }
        if (large != null && !large.isEmpty())
        {
            large.populate(world, chunkX, chunkZ, this);
        }
        if (sector == CITY && city != null)
        {
            return; // city blocks replace the small city_ schematics
        }
        List<Schematic> pool = pool(sector);
        if (pool.isEmpty())
        {
            return;
        }

        int cellX = Math.floorDiv(chunkX, CELL);
        int cellZ = Math.floorDiv(chunkZ, CELL);
        if (large != null && !large.isEmpty()
            && large.covers(world, cellX * CELL * 16, cellZ * CELL * 16,
                            cellX * CELL * 16 + CELL * 16 - 1, cellZ * CELL * 16 + CELL * 16 - 1, this))
        {
            return; // a large structure owns this cell
        }
        // world.getSeed(); vanilla-style cell hash keeps this stable per world
        Random r = new Random(world.getSeed()
                              ^ (cellX * 341873128712L + cellZ * 132897987541L));
        if (r.nextFloat() >= SECTOR_CHANCE[sector])
        {
            return;
        }
        // anchors stay in the first CELL-1 chunks: origin +8 plus a 24 wide
        // footprint then ends before the next cell, whose first chunk
        // carries the city street
        int anchorX = cellX * CELL + r.nextInt(CELL - 1);
        int anchorZ = cellZ * CELL + r.nextInt(CELL - 1);
        Schematic schematic = pool.get(r.nextInt(pool.size()));
        int turns = r.nextInt(4); // 0/90/180/270 clockwise
        if (anchorX != chunkX || anchorZ != chunkZ)
        {
            return; // this cell's structure belongs to another chunk
        }

        place(world, schematic, (chunkX << 4) + 8, (chunkZ << 4) + 8, turns,
              SECTOR_NAME[sector], chunkX << 4, chunkZ << 4);
    }

    private void place(World world, Schematic s, int originX, int originZ,
                       int turns, String sectorName, int safeMinX, int safeMinZ)
    {
        // 90/270 swap the footprint's world-space extents
        boolean swap = (turns & 1) != 0;
        int footW = swap ? s.length : s.width;
        int footL = swap ? s.width : s.length;
        int maxX = originX + footW - 1;
        int maxZ = originZ + footL - 1;

        // sample ground at the corners and centre of the footprint
        int[][] points = {
            {originX, originZ}, {maxX, originZ}, {originX, maxZ}, {maxX, maxZ},
            {(originX + maxX) / 2, (originZ + maxZ) / 2},
        };
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int[] p : points)
        {
            int y = ground(world, p[0], p[1]);
            // ground() skips water (it does not block movement), so the
            // first block above the seabed is where water shows up
            if (isWater(world, p[0], y, p[1]))
            {
                return; // ocean/lake/river site
            }
            lo = Math.min(lo, y);
            hi = Math.max(hi, y);
        }
        if (lo < 4 || hi > 200 || hi - lo > MAX_SLOPE)
        {
            return; // broken sample, or site too steep
        }
        // floor REPLACES the top ground layer - flush with the terrain
        // instead of perched one block above it
        int groundY = lo - 1;

        // pass 1: foundation + blocks, bottom-up. SKIP cells never touch the
        // world. Flag 2 (send to client, no neighbour updates) so attached
        // blocks never pop during placement.
        int skippedIds = 0;
        List<int[]> props = new ArrayList<int[]>(); // deferred: x,y,z,index
        for (int y = 0; y < s.height; y++)
        {
            for (int z = 0; z < s.length; z++)
            {
                for (int x = 0; x < s.width; x++)
                {
                    int i = s.index(x, y, z);
                    int id = s.blocks[i];
                    if (id == SKIP_ID)
                    {
                        continue;
                    }
                    // rotate the source coordinate into the world footprint
                    int rx, rz;
                    switch (turns)
                    {
                        case 1:  rx = s.length - 1 - z; rz = x; break;
                        case 2:  rx = s.width - 1 - x; rz = s.length - 1 - z; break;
                        case 3:  rx = z; rz = s.width - 1 - x; break;
                        default: rx = x; rz = z; break;
                    }
                    int wx = originX + rx, wy = groundY + y, wz = originZ + rz;

                    if (y == 0)
                    {
                        // dirt foundation under every real ground-level column
                        for (int fy = wy - 1; fy >= wy - MAX_FOUNDATION; fy--)
                        {
                            if (!world.isAirBlock(wx, fy, wz)) // isAirBlock
                            {
                                break;
                            }
                            world.setBlock(wx, fy, wz,
                                                Blocks.dirt, 0, 2);
                        }
                    }

                    if (substitutions.containsKey((id << 4) | (s.data[i] & 15)))
                    {
                        // props are placed in pass 2, after the roads and
                        // walls they orient against exist
                        props.add(new int[] {wx, wy, wz, i});
                        continue;
                    }

                    Block block = Block.getBlockById(id); // getBlockById
                    if (block == null)
                    {
                        skippedIds++;
                        continue; // id not registered in this save
                    }
                    int meta = Rotation.rotateMeta(id, s.data[i] & 0xFF, turns);
                    world.setBlock(wx, wy, wz, block, meta, 2);
                }
            }
        }

        // pass 2: substituted props, oriented per their mode
        for (int[] p : props)
        {
            int i = p[3];
            int key = (s.blocks[i] << 4) | (s.data[i] & 15);
            Sub sub = substitutions.get(key);
            int pick = (p[0] * 31 + p[2]) * 31 + p[1];
            Block block = sub.targets[(pick & 0x7fffffff) % sub.targets.length];
            world.setBlock(p[0], p[1], p[2], block,
                                propMeta(world, sub.mode, p[0], p[1], p[2],
                                         pick, turns), 2);
            net.decimation.fixes.MultiblockRepairHandler.repair(world.getTileEntity(p[0], p[1], p[2]));
        }

        gradeRing(world, originX, originZ, maxX, maxZ, groundY,
                  safeMinX, safeMinZ);

        if (skippedIds > 0 && !loggedBadId)
        {
            loggedBadId = true;
            FMLLog.info("[%s] schematic '%s' referenced %d unregistered block ids"
                        + " - was it created with a different mod list?",
                        DecimationWorldGen.MODID, s.name, skippedIds);
        }
        net.decimation.mod.server.zones.a zone = zoneFor(s);
        if (zone != null)
        {
            ZoneStore.add(zone,
                          originX - ZONE_MARGIN, groundY - 4, originZ - ZONE_MARGIN,
                          maxX + ZONE_MARGIN, groundY + s.height + 16, maxZ + ZONE_MARGIN);
        }
        FMLLog.info("[%s] placed '%s' at %d,%d,%d (rot %d, sector %s, zone %s)",
                    DecimationWorldGen.MODID, s.name, originX, groundY, originZ,
                    turns * 90, sectorName, zone == null ? "none" : zone.name());
    }

    /**
     * Zone tag by schematic prefix: military sites get Decimation's MILITARY
     * zone, city blocks its POLICE zone (both bias infected spawns toward
     * their uniformed variants). Civilian and untagged ruins stay unzoned.
     */
    private static net.decimation.mod.server.zones.a zoneFor(Schematic s)
    {
        if (s.name.startsWith("mil_"))
        {
            return net.decimation.mod.server.zones.a.MILITARY;
        }
        if (s.name.startsWith("city_"))
        {
            return net.decimation.mod.server.zones.a.POLICE;
        }
        return null;
    }

    /**
     * City streets: every cell's first chunk row/column carries a
     * STREET_WIDTH wide road along its west / north edge, so CITY sectors get
     * a street grid with one block per cell. Each chunk paints only its own
     * columns, which are always loaded while it populates, so this never
     * triggers neighbouring chunk generation. The surface follows the terrain
     * (stepping one block at a time), skips water, and clears plants and
     * tree parts standing on it.
     */
    private void paintStreets(World world, int chunkX, int chunkZ)
    {
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int wx = baseX + x, wz = baseZ + z;
                int ox = Math.floorMod(wx, CELL * 16), oz = Math.floorMod(wz, CELL * 16);
                boolean street = ox < STREET_WIDTH || oz < STREET_WIDTH;
                boolean sidewalk = !street && (ox < STREET_WIDTH + SIDEWALK || oz < STREET_WIDTH + SIDEWALK
                    || ox >= CELL * 16 - SIDEWALK || oz >= CELL * 16 - SIDEWALK);
                if (!street && !sidewalk)
                {
                    continue;
                }
                int y = soilTop(world, wx, wz);
                if (y <= 4 || y > 250 || isWater(world, wx, y + 1, wz))
                {
                    continue;
                }
                Block top = world.getBlock(wx, y, wz);
                if (top.getMaterial().isLiquid())
                {
                    continue;
                }
                world.setBlock(wx, y, wz, street ? streetBlock : Blocks.double_stone_slab, 0, 2);
                for (int above = y + 1; above <= y + STREET_CLEARANCE; above++)
                {
                    if (clearable(world.getBlock(wx, above, wz)))
                    {
                        world.setBlock(wx, above, wz, Blocks.air, 0, 2);
                    }
                }
                if (street && cars.length > 0)
                {
                    int carMeta = carMeta(world, ox, oz, wx, wz);
                    if (carMeta >= 0)
                    {
                        Block car = cars[(int) ((hash(world.getSeed(), wx, wz) >>> 8) % cars.length)];
                        world.setBlock(wx, y + 1, wz, car, carMeta, 2);
                    }
                }
            }
        }
    }

    /**
     * Parked / wrecked car on this street column, or -1. Lanes are 1 and 3
     * across the 5 wide street, one car slot every CAR_SPACING blocks along
     * it, never in intersections. Facing follows the street (PropRenderer
     * turns props by metadata % 4 * 90 degrees): north-south streets get
     * 5 or 3, east-west streets 4 or 2, the two lanes opposite ways.
     * See docs/building_design.md.
     */
    private int carMeta(World world, int ox, int oz, int wx, int wz)
    {
        boolean northSouth = ox < STREET_WIDTH && oz >= STREET_WIDTH;
        boolean eastWest = oz < STREET_WIDTH && ox >= STREET_WIDTH;
        if (!northSouth && !eastWest)
        {
            return -1; // intersection
        }
        int across = northSouth ? ox : oz;
        int along = northSouth ? oz : ox;
        if ((across != 1 && across != 3) || (along - STREET_WIDTH) % CAR_SPACING != CAR_SPACING / 2)
        {
            return -1;
        }
        if ((hash(world.getSeed(), wx, wz) & 0xFF) >= CAR_CHANCE)
        {
            return -1;
        }
        // The wreck model's long axis lies along x at 0 degrees (seen in game
        // 2026-10-07: 4/2 put cars ACROSS a north-south street), so
        // north-south streets need 90/270 degrees (5/3) and east-west 0/180 (4/2).
        if (northSouth)
        {
            return across == 1 ? 3 : 5;
        }
        return across == 1 ? 2 : 4;
    }

    private static long hash(long seed, int x, int z)
    {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h & Long.MAX_VALUE;
    }

    /** Plants, snow and tree parts: things a street may remove or look through. */
    static boolean clearable(Block b)
    {
        net.minecraft.block.material.Material m = b.getMaterial();
        return m == net.minecraft.block.material.Material.air
            || m == net.minecraft.block.material.Material.plants
            || m == net.minecraft.block.material.Material.vine
            || m == net.minecraft.block.material.Material.leaves
            || m == net.minecraft.block.material.Material.wood
            || m == net.minecraft.block.material.Material.snow
            || m == net.minecraft.block.material.Material.gourd
            || m == net.minecraft.block.material.Material.cactus;
    }

    /**
     * BlockProp facing lives in metadata 2..5; the clockwise cycle is
     * 2 -> 3 -> 4 -> 5 -> 2 (decompiled from BlockProp.onBlockPlacedBy).
     * Base steps: 0=meta2, 1=meta3, 2=meta4, 3=meta5.
     */
    private int propMeta(World world, int mode, int x, int y, int z,
                         int pick, int turns)
    {
        switch (mode)
        {
            case Sub.PLAIN:
                return 0;
            case Sub.RANDOM:
                return 2 + (((pick >> 4) + turns) & 3);
            case Sub.FACE_ROAD:
            case Sub.FACE_ROAD_AWAY:
            {
                // W, N, E, S in cycle order; look for road surface at foot level
                int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
                for (int step = 0; step < 4; step++)
                {
                    for (int dist = 1; dist <= 2; dist++)
                    {
                        Block b = world.getBlock(x + dirs[step][0] * dist,
                                                      y - 1,
                                                      z + dirs[step][1] * dist);
                        if (roadBlocks.contains(b))
                        {
                            // street-light arms render opposite their facing
                            // meta, so AWAY mode flips 180
                            int flip = mode == Sub.FACE_ROAD_AWAY ? 2 : 0;
                            return 2 + ((step + flip) & 3);
                        }
                    }
                }
                // fall through to FIXED when no road is nearby
            }
            case Sub.FIXED:
            default:
                return 2 + ((3 + turns) & 3); // south + structure rotation
        }
    }

    /**
     * Grades a RING-wide band of terrain around the footprint into a smooth
     * slope between the structure's floor level and the natural terrain,
     * keeping each column's own surface block (sand stays sand, grass stays
     * grass). This is what stops buildings from sitting in sharp-walled pits
     * or on sheer plateaus. Writes are clamped to the safe population window
     * so the ring can never touch an unloaded chunk.
     */
    private void gradeRing(World world, int minX, int minZ, int maxX, int maxZ,
                           int groundY, int safeMinX, int safeMinZ)
    {
        for (int x = minX - RING; x <= maxX + RING; x++)
        {
            for (int z = minZ - RING; z <= maxZ + RING; z++)
            {
                boolean inside = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
                if (inside)
                {
                    continue; // only the band around the footprint
                }
                if (x < safeMinX || x > safeMinX + 31
                    || z < safeMinZ || z > safeMinZ + 31)
                {
                    continue; // outside the guaranteed-loaded 2x2 chunk region
                }
                // distance from the footprint edge, 1..RING
                int dx = x < minX ? minX - x : x > maxX ? x - maxX : 0;
                int dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0;
                int dist = Math.max(dx, dz);

                int naturalTop = ground(world, x, z) - 1; // top solid block
                if (naturalTop <= 0 || naturalTop > 250)
                {
                    continue;
                }
                if (isWater(world, x, naturalTop + 1, z))
                {
                    continue; // underwater column: never cut into or fill over water
                }
                Block surface = world.getBlock(x, naturalTop, z);
                // interpolate: floor level at the wall, natural at ring edge
                int targetTop = groundY
                    + Math.round((naturalTop - groundY) * dist / (float) (RING + 1));
                if (targetTop == naturalTop)
                {
                    continue;
                }
                if (Math.abs(naturalTop - targetTop) > MAX_FOUNDATION)
                {
                    continue; // cliff face - leave it alone
                }
                if (naturalTop > targetTop)
                {
                    // cut down, then re-cap with the column's own surface block
                    for (int y = naturalTop; y > targetTop; y--)
                    {
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                    world.setBlock(x, targetTop, z, surface, 0, 2);
                }
                else
                {
                    // fill up with dirt, cap with the original surface block
                    for (int y = naturalTop + 1; y < targetTop; y++)
                    {
                        world.setBlock(x, y, z, Blocks.dirt, 0, 2);
                    }
                    world.setBlock(x, targetTop, z, surface, 0, 2);
                }
            }
        }
    }

    /**
     * Y just above the topmost movement-blocking block. Despite its name,
     * getTopSolidOrLiquidBlock skips water, so over a lake this is the
     * block above the lake bed, which is water.
     */
    private static int ground(World world, int x, int z)
    {
        return world.getTopSolidOrLiquidBlock(x, z); // getTopSolidOrLiquidBlock
    }

    private static boolean isWater(World world, int x, int y, int z)
    {
        Block b = world.getBlock(x, y, z); // getBlock
        return b == Blocks.water || b == Blocks.flowing_water; // water / flowing_water
    }
}
