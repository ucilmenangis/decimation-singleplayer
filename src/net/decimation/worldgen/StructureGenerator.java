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
    private static final int REGION = 16;
    /** Max footprint that stays inside the safe population window. */
    public static final int MAX_FOOTPRINT = 24;
    /** Corner-to-corner ground height spread beyond which the site is rejected. */
    private static final int MAX_SLOPE = 6;
    /** How deep the dirt foundation may reach below the floor. */
    private static final int MAX_FOUNDATION = 8;
    /** Width of the graded terrain ring around the footprint. */
    private static final int RING = 3;
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
    private boolean disabled;
    private int errors;
    private boolean loggedBadId;

    public StructureGenerator(List<Schematic> schematics,
                              java.util.Map<Integer, Sub> substitutions,
                              Set<Block> roadBlocks)
    {
        this.schematics = schematics;
        this.substitutions = substitutions;
        this.roadBlocks = roadBlocks;
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
        int regionX = Math.floorDiv(chunkX, REGION);
        int regionZ = Math.floorDiv(chunkZ, REGION);
        Random r = new Random(world.func_72905_C()
                              ^ (regionX * 875949887L + regionZ * 656887297L));
        float roll = r.nextFloat();
        if (roll < 0.40f) return WILD;
        if (roll < 0.65f) return CIV;
        if (roll < 0.80f) return CITY;
        return MIL;
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
        if (world.field_73011_w == null || world.field_73011_w.field_76574_g != 0)
        {
            return; // overworld only
        }

        int sector = sector(world, chunkX, chunkZ);
        List<Schematic> pool = pool(sector);
        if (pool.isEmpty())
        {
            return;
        }

        int cellX = Math.floorDiv(chunkX, CELL);
        int cellZ = Math.floorDiv(chunkZ, CELL);
        // world.getSeed(); vanilla-style cell hash keeps this stable per world
        Random r = new Random(world.func_72905_C()
                              ^ (cellX * 341873128712L + cellZ * 132897987541L));
        if (r.nextFloat() >= SECTOR_CHANCE[sector])
        {
            return;
        }
        int anchorX = cellX * CELL + r.nextInt(CELL);
        int anchorZ = cellZ * CELL + r.nextInt(CELL);
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
        int[] samples = {
            ground(world, originX, originZ),
            ground(world, maxX, originZ),
            ground(world, originX, maxZ),
            ground(world, maxX, maxZ),
            ground(world, (originX + maxX) / 2, (originZ + maxZ) / 2),
        };
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int y : samples)
        {
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

        if (isWater(world, originX, groundY, originZ)
            || isWater(world, maxX, groundY, maxZ)
            || isWater(world, (originX + maxX) / 2, groundY, (originZ + maxZ) / 2))
        {
            return; // ocean/lake/river site
        }

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
                            if (!world.func_147437_c(wx, fy, wz)) // isAirBlock
                            {
                                break;
                            }
                            world.func_147465_d(wx, fy, wz,
                                                Blocks.field_150346_d, 0, 2);
                        }
                    }

                    if (substitutions.containsKey((id << 4) | (s.data[i] & 15)))
                    {
                        // props are placed in pass 2, after the roads and
                        // walls they orient against exist
                        props.add(new int[] {wx, wy, wz, i});
                        continue;
                    }

                    Block block = Block.func_149729_e(id); // getBlockById
                    if (block == null)
                    {
                        skippedIds++;
                        continue; // id not registered in this save
                    }
                    int meta = Rotation.rotateMeta(id, s.data[i] & 0xFF, turns);
                    world.func_147465_d(wx, wy, wz, block, meta, 2);
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
            world.func_147465_d(p[0], p[1], p[2], block,
                                propMeta(world, sub.mode, p[0], p[1], p[2],
                                         pick, turns), 2);
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
        FMLLog.info("[%s] placed '%s' at %d,%d,%d (rot %d, sector %s)",
                    DecimationWorldGen.MODID, s.name, originX, groundY, originZ,
                    turns * 90, sectorName);
    }

    /**
     * BlockProp facing lives in metadata 2..5; the clockwise cycle is
     * 2 -> 3 -> 4 -> 5 -> 2 (decompiled from BlockProp.func_149689_a).
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
                        Block b = world.func_147439_a(x + dirs[step][0] * dist,
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
                Block surface = world.func_147439_a(x, naturalTop, z);
                if (surface == Blocks.field_150355_j
                    || surface == Blocks.field_150358_i)
                {
                    continue; // never cut into or fill over water
                }
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
                        world.func_147465_d(x, y, z, Blocks.field_150350_a, 0, 2);
                    }
                    world.func_147465_d(x, targetTop, z, surface, 0, 2);
                }
                else
                {
                    // fill up with dirt, cap with the original surface block
                    for (int y = naturalTop + 1; y < targetTop; y++)
                    {
                        world.func_147465_d(x, y, z, Blocks.field_150346_d, 0, 2);
                    }
                    world.func_147465_d(x, targetTop, z, surface, 0, 2);
                }
            }
        }
    }

    /** First air Y above the topmost solid/liquid block. */
    private static int ground(World world, int x, int z)
    {
        return world.func_72825_h(x, z); // getTopSolidOrLiquidBlock
    }

    private static boolean isWater(World world, int x, int y, int z)
    {
        Block b = world.func_147439_a(x, y, z); // getBlock
        return b == Blocks.field_150355_j || b == Blocks.field_150358_i; // water / flowing_water
    }
}
