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

import static net.decimation.worldgen.StructureGenerator.*;

/**
 * The procedural city street painter (v0.8..0.15): a STREET_WIDTH wide road
 * along each city cell's first chunk row / column, levelled sidewalks,
 * centre line, street lights, benches, bins, trash bags and parked wrecks.
 * Used only in CITY sectors when no converted city content is installed
 * (LcCity.enabled() false); moved out of StructureGenerator 2026-10-09.
 */
final class LegacyStreets
{
    /** City street width in blocks, along each cell's west and north edge. */
    static final int STREET_WIDTH = 5;
    /** Sidewalk width on every side of a city block. */
    static final int SIDEWALK = 2;
    /** One car slot every this many blocks along a lane, filled with CAR_CHANCE / 256. */
    static final int CAR_SPACING = 9;
    static final int CAR_CHANCE = 64;
    /** How far above a street plants and tree parts are cleared. */
    static final int STREET_CLEARANCE = 8;

    /** Street surface; null disables street painting. */
    private final Block streetBlock;
    /** Car wreck props parked on city streets (may be empty). */
    Block[] cars = new Block[0];
    Block streetLight, bench, bin, centreLine;
    Block[] trashBags = new Block[0];

    LegacyStreets(Block streetBlock)
    {
        this.streetBlock = streetBlock;
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
    void paintStreets(World world, int chunkX, int chunkZ)
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
                y = levelStreet(world, wx, wz, ox, oz, y);
                int line = street ? centreLineMeta(ox, oz) : -1;
                if (line >= 0)
                {
                    world.setBlock(wx, y, wz, centreLine, line, 2);
                }
                else
                {
                    world.setBlock(wx, y, wz, street ? streetBlock : Blocks.double_stone_slab, 0, 2);
                }
                for (int above = y + 1; above <= y + STREET_CLEARANCE; above++)
                {
                    if (clearable(world.getBlock(wx, above, wz)))
                    {
                        world.setBlock(wx, above, wz, Blocks.air, 0, 2);
                    }
                }
                if (sidewalk)
                {
                    furnish(world, ox, oz, wx, y + 1, wz);
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

    /** Largest cut or fill used to level a street cross-section. */
    private static final int MAX_LEVEL = 6;

    /**
     * Levels a street / sidewalk column to its street's centre line, so the
     * cross-section is flat instead of following the terrain column by column
     * (seen in game: a 3 block staircase across one street). The reference is
     * the centre column (offset 2) of the nearest street at the same position
     * along it; intersections and corners use the intersection centre. That
     * column lies in this chunk or its +x / +z neighbour, which always exist
     * while this chunk populates, and a centre column is never changed by
     * levelling, so every chunk computes the same height. Returns the new top.
     */
    private int levelStreet(World world, int wx, int wz, int ox, int oz, int y)
    {
        int cellBlocks = CELL * 16;
        int band = STREET_WIDTH + SIDEWALK;
        boolean nsBand = ox < band || ox >= cellBlocks - SIDEWALK;
        boolean ewBand = oz < band || oz >= cellBlocks - SIDEWALK;
        int rx = wx - ox + (ox < cellBlocks / 2 ? 0 : cellBlocks) + STREET_WIDTH / 2;
        int rz = wz - oz + (oz < cellBlocks / 2 ? 0 : cellBlocks) + STREET_WIDTH / 2;
        int refX = nsBand ? rx : wx, refZ = ewBand ? rz : wz;
        if (refX == wx && refZ == wz)
        {
            return y;
        }
        int target = soilTop(world, refX, refZ);
        if (target <= 4 || isWater(world, refX, target + 1, refZ) || Math.abs(target - y) > MAX_LEVEL)
        {
            return y;
        }
        for (int fill = y + 1; fill < target; fill++)
        {
            world.setBlock(wx, fill, wz, Blocks.dirt, 0, 2);
        }
        for (int cut = target + 1; cut <= y; cut++)
        {
            world.setBlock(wx, cut, wz, Blocks.air, 0, 2);
        }
        return target;
    }

    /**
     * Dashed centre line (3 on, 3 off) along the middle of a street, never in
     * an intersection or the 2 blocks next to it; -1 elsewhere.
     * BlockRoad_CenterLine (DeciTexturedBlock) picks its top texture by
     * metadata % 4: 0 and 1 have the stripe running down the texture, 2 and 3
     * across it. The top face maps texture u to x and v to z, so north-south
     * streets use 4, east-west streets 2 [inferred from the textures, to be
     * checked in game like the cars were].
     */
    private int centreLineMeta(int ox, int oz)
    {
        if (centreLine == null)
        {
            return -1;
        }
        boolean northSouth = ox < STREET_WIDTH && oz >= STREET_WIDTH;
        boolean eastWest = oz < STREET_WIDTH && ox >= STREET_WIDTH;
        int across = northSouth ? ox : oz, along = northSouth ? oz : ox;
        if (!northSouth && !eastWest || across != STREET_WIDTH / 2
            || along < STREET_WIDTH + SIDEWALK || along >= CELL * 16 - SIDEWALK)
        {
            return -1;
        }
        return (along / 3) % 2 == 0 ? (northSouth ? 4 : 2) : -1;
    }

    /**
     * Street furniture on a sidewalk column (y = the block above the slab).
     * Which way the road lies decides the facing: BlockProp metadata 2..5
     * with PropRenderer's transform (180 deg flip about x, then metadata % 4
     * * 90 about y, then the prop's extra rotation) puts a street light's
     * arm (model +x, extra rotation 180) and a bench's front (backrest at
     * model -x, extra rotation 180) to the east for 2, south 3, west 4,
     * north 5. Derived from deobf PropRenderer + the .bmodel / ModelStreetBench
     * geometry, matching the in game car calibration.
     *   - road side column: a street light every 20 blocks, the two sides
     *     staggered by 10, 25% missing;
     *   - inner column: benches (35% of slots) and bins (40%) every 20
     *     blocks, scattered trash bags (4%).
     * Corners (where two sidewalks cross) stay empty.
     */
    private void furnish(World world, int ox, int oz, int wx, int y, int wz)
    {
        int last = CELL * 16 - 1;
        int facing, along;
        boolean roadSide;
        int lotStart = STREET_WIDTH + SIDEWALK, lotEnd = CELL * 16 - SIDEWALK; // along range off the corners
        if ((ox == STREET_WIDTH || ox == STREET_WIDTH + 1 || ox >= last - 1) && oz >= lotStart && oz < lotEnd)
        {
            facing = ox < STREET_WIDTH + SIDEWALK ? 4 : 2; // road to the west / east
            roadSide = ox == STREET_WIDTH || ox == last;
            along = oz;
        }
        else if ((oz == STREET_WIDTH || oz == STREET_WIDTH + 1 || oz >= last - 1) && ox >= lotStart && ox < lotEnd)
        {
            facing = oz < STREET_WIDTH + SIDEWALK ? 5 : 3; // road to the north / south
            roadSide = oz == STREET_WIDTH || oz == last;
            along = ox;
        }
        else
        {
            return; // corner
        }
        if (!world.isAirBlock(wx, y, wz))
        {
            return;
        }
        long h = hash(world.getSeed() ^ 0x57EE7L, wx, wz);
        int side = facing == 4 || facing == 5 ? 0 : 10; // stagger the two sides
        int slot = Math.floorMod(along - side, 20);
        Block prop = null;
        int meta = facing;
        if (roadSide)
        {
            if (slot == 0 && (h & 3) != 0)
            {
                prop = streetLight;
            }
        }
        else if (slot == 10 && (h & 0xFF) < 90)
        {
            prop = bench;
        }
        else if (slot == 3 && (h & 0xFF) < 102)
        {
            prop = bin;
            meta = 2 + (int) ((h >>> 8) & 3);
        }
        else if (trashBags.length > 0 && (h & 0xFF) < 10 && slot != 10 && slot != 3)
        {
            prop = trashBags[(int) ((h >>> 8) % trashBags.length)];
            meta = 2 + (int) ((h >>> 16) & 3);
        }
        if (prop != null)
        {
            world.setBlock(wx, y, wz, prop, meta, 2);
        }
    }

    private static long hash(long seed, int x, int z)
    {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h & Long.MAX_VALUE;
    }
}
