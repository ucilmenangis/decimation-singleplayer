package net.decimation.worldgen;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Many block writes lit once (bug.md "Server stalls while new city areas generate", docs/performance.md 9).
 * World.setBlock lights every block it sets: a light check at the block and, when the column's height changes, a
 * relight of the column and its four neighbours. A structure written column by column re-lit its half built rooms
 * with every new column (their open side moved one column on): 93 % of a structure write was light.
 * {@link #set} does what World.setBlock(x, y, z, block, meta, 2) does without the light (the same block hooks, tile
 * entities, height map, client update); {@link #light} then lights the written columns once, on the final blocks,
 * with vanilla's own light update: the column's sky light as Chunk.relightBlock leaves it, then
 * World.updateLightByType for every cell of the ring around the written columns and of the written columns (sky), at
 * every written cell whose own light changed and at every written cell whose opacity changed next to block light that
 * was already there (block). Next to chunks that do not exist yet vanilla's update does nothing, as it did block by
 * block. Server side only (a client world writes through World.setBlock).
 */
public final class BlockBatch
{
    /** -Ddeciworldgen.bulklight=false: every write through World.setBlock, lit block by block (to compare). */
    public static volatile boolean on = !"false".equals(System.getProperty("deciworldgen.bulklight"));
    private final World world;
    /** Per written column ({@link Keys#pair}): {height before the batch, lowest written y, highest written y, x, z}. */
    private final Map<Long, int[]> columns = new HashMap<Long, int[]>();
    /** Written cells whose opacity changed (x, y, z packed): their block light is checked at the end if any is near. */
    private long[] cells = new long[1024];
    private int ncells;
    /** Written cells whose own light changed (a lamp placed or taken away): their block light is always checked. */
    private long[] lights = new long[64];
    private int nlights;

    public BlockBatch(World world)
    {
        this.world = world;
    }

    /** World.setBlock(x, y, z, block, meta, 2) without the light work; true if the block changed. */
    public boolean set(int x, int y, int z, Block block, int meta)
    {
        if (world.isRemote || !on)
        {
            return world.setBlock(x, y, z, block, meta, 2);
        }
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000 || y < 0 || y >= 256)
        {
            return false;
        }
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        int lx = x & 15, lz = z & 15, i = lz << 4 | lx;
        if (y >= chunk.precipitationHeightMap[i] - 1)
        {
            chunk.precipitationHeightMap[i] = -999;
        }
        int height = chunk.heightMap[i];
        Block old = chunk.getBlock(lx, y, lz);
        int oldMeta = chunk.getBlockMetadata(lx, y, lz);
        if (old == block && oldMeta == meta)
        {
            return false;
        }
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        ExtendedBlockStorage section = storage[y >> 4];
        boolean newSection = false;
        if (section == null)
        {
            if (block == Blocks.air)
            {
                return false;
            }
            section = storage[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !world.provider.hasNoSky);
            newSection = y >= height;
        }
        Long key = Keys.pair(x, z);
        int[] col = columns.get(key);
        if (col == null)
        {
            columns.put(key, new int[] {height, y, y, x, z});
        }
        else
        {
            col[1] = Math.min(col[1], y);
            col[2] = Math.max(col[2], y);
        }
        int oldOpacity = old.getLightOpacity(world, x, y, z), oldLight = old.getLightValue(world, x, y, z);
        // the steps of Chunk.func_150807_a, light left out
        old.onBlockPreDestroy(world, x, y, z, oldMeta);
        section.func_150818_a(lx, y & 15, lz, block);
        section.setExtBlockMetadata(lx, y & 15, lz, meta);
        old.breakBlock(world, x, y, z, old, oldMeta);
        TileEntity te = chunk.getTileEntityUnsafe(lx, y, lz);
        if (te != null && te.shouldRefresh(old, chunk.getBlock(lx, y, lz), oldMeta, chunk.getBlockMetadata(lx, y, lz), world, x, y, z))
        {
            chunk.removeTileEntity(lx, y, lz);
        }
        if (section.getBlockByExtId(lx, y & 15, lz) != block)
        {
            return false;
        }
        section.setExtBlockMetadata(lx, y & 15, lz, meta);
        int opacity = block.getLightOpacity(world, x, y, z);
        if (newSection)
        {
            chunk.generateSkylightMap(); // as vanilla does for a new section at the top (sets the new section's sky)
        }
        else if (opacity > 0 ? y >= height : y == height - 1)
        {
            // the height map as Chunk.relightBlock moves it
            int top = opacity > 0 ? y + 1 : y;
            while (top > 0 && chunk.func_150808_b(lx, top - 1, lz) == 0)
            {
                top--;
            }
            chunk.heightMap[i] = top;
            if (top < chunk.heightMapMinimum)
            {
                chunk.heightMapMinimum = top;
            }
        }
        block.onBlockAdded(world, x, y, z);
        if (block.hasTileEntity(meta))
        {
            TileEntity made = chunk.func_150806_e(lx, y, lz);
            if (made != null)
            {
                made.updateContainingBlockInfo();
                made.blockMetadata = meta;
            }
        }
        chunk.isModified = true;
        if (block.getLightValue(world, x, y, z) != oldLight)
        {
            if (nlights == lights.length)
            {
                lights = java.util.Arrays.copyOf(lights, nlights * 2);
            }
            lights[nlights++] = pack(x, y, z);
        }
        else if (opacity != oldOpacity)
        {
            if (ncells == cells.length)
            {
                cells = java.util.Arrays.copyOf(cells, ncells * 2);
            }
            cells[ncells++] = pack(x, y, z);
        }
        world.markAndNotifyBlock(x, y, z, chunk, null, block, 2);
        return true;
    }

    /** Lights everything written since the last call (vanilla's light update on the final blocks). */
    public void light()
    {
        if (columns.isEmpty())
        {
            return;
        }
        boolean sky = !world.provider.hasNoSky;
        for (int[] col : columns.values())
        {
            int x = col[3], z = col[4];
            Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
            int h0 = col[0], h1 = chunk.heightMap[(z & 15) << 4 | (x & 15)];
            int lo = col[1], hi = col[2] + 1;                 // the written cells and one above
            if (h1 != h0)
            {
                // the cells between the two heights changed their sky, and the ones the light decays through below
                lo = Math.min(lo, Math.min(h0, h1));
                hi = Math.max(hi, Math.max(h0, h1));
                if (sky)
                {
                    lo = Math.min(lo, columnSky(chunk, x & 15, z & 15, h0, h1));
                }
            }
            col[1] = Math.max(0, lo - 1);
            col[2] = Math.min(255, hi);
        }
        if (sky)
        {
            // the ring first: a cell outside lit through what is now a wall goes dark before the rooms light from it.
            // A ring column takes the heights of the written columns beside it.
            for (int[] col : columns.values())
            {
                for (int side = 0; side < 4; side++)
                {
                    int nx = col[3] + (side == 0 ? -1 : side == 1 ? 1 : 0), nz = col[4] + (side == 2 ? -1 : side == 3 ? 1 : 0);
                    Long nkey = Keys.pair(nx, nz);
                    if (!columns.containsKey(nkey))
                    {
                        int[] r = ring.get(nkey);
                        if (r == null)
                        {
                            ring.put(nkey, new int[] {col[1], col[2], nx, nz});
                        }
                        else
                        {
                            r[0] = Math.min(r[0], col[1]);
                            r[1] = Math.max(r[1], col[2]);
                        }
                    }
                }
            }
            for (int[] r : ring.values())
            {
                skyChecks += r[1] - r[0] + 1;
                for (int y = r[0]; y <= r[1]; y++)
                {
                    world.updateLightByType(EnumSkyBlock.Sky, r[2], y, r[3]);
                }
            }
            for (int[] col : columns.values())
            {
                skyChecks += col[2] - col[1] + 1;
                for (int y = col[1]; y <= col[2]; y++)
                {
                    world.updateLightByType(EnumSkyBlock.Sky, col[3], y, col[4]);
                }
            }
        }
        // block light: where a block now stops or lets through light that was already there (from before this batch:
        // the lamps' own light comes next), then every lamp placed or taken away
        for (int c = 0; c < ncells; c++)
        {
            long p = cells[c];
            int x = unpackX(p), y = unpackY(p), z = unpackZ(p);
            if (blockLightNear(x, y, z))
            {
                blockChecks++;
                world.updateLightByType(EnumSkyBlock.Block, x, y, z);
            }
        }
        blockChecks += nlights;
        for (int c = 0; c < nlights; c++)
        {
            long p = lights[c];
            world.updateLightByType(EnumSkyBlock.Block, unpackX(p), unpackY(p), unpackZ(p));
        }
        columns.clear();
        ring.clear();
        ncells = 0;
        nlights = 0;
        cached = null;
    }

    /** Vanilla light updates asked so far, sky and block (DevPregen reports them). */
    public static long skyChecks, blockChecks;

    /** Ring columns ({@link Keys#pair}): {lowest y, highest y, x, z} of the written columns beside them. */
    private final Map<Long, int[]> ring = new HashMap<Long, int[]>();
    private Chunk cached;

    /** Block light in the cell or one of its six neighbours (chunks that are not loaded count as dark). */
    private boolean blockLightNear(int x, int y, int z)
    {
        return blockLight(x, y, z) > 0 || blockLight(x - 1, y, z) > 0 || blockLight(x + 1, y, z) > 0
            || blockLight(x, y - 1, z) > 0 || blockLight(x, y + 1, z) > 0 || blockLight(x, y, z - 1) > 0
            || blockLight(x, y, z + 1) > 0;
    }

    private int blockLight(int x, int y, int z)
    {
        if (y < 0 || y > 255)
        {
            return 0;
        }
        Chunk c = cached;
        if (c == null || c.xPosition != x >> 4 || c.zPosition != z >> 4)
        {
            if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4))
            {
                return 0;
            }
            c = cached = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        }
        return c.getSavedLightValue(EnumSkyBlock.Block, x & 15, y, z & 15);
    }

    /**
     * A column's sky light after its height moved from h0 to h1, as Chunk.relightBlock sets it: newly covered cells
     * dark, newly open cells 15, then from the top down 15 minus each block's opacity (at least 1) until it is 0.
     * Returns the lowest cell it set.
     */
    private static int columnSky(Chunk chunk, int lx, int lz, int h0, int h1)
    {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        for (int y = Math.min(h0, h1); y < Math.max(h0, h1); y++)
        {
            ExtendedBlockStorage s = storage[y >> 4];
            if (s != null)
            {
                s.setExtSkylightValue(lx, y & 15, lz, h1 > h0 ? 0 : 15);
            }
        }
        int l = 15, y = h1 - 1;
        for (; y >= 0 && l > 0; y--)
        {
            l = Math.max(0, l - Math.max(1, chunk.func_150808_b(lx, y, lz)));
            ExtendedBlockStorage s = storage[y >> 4];
            if (s != null)
            {
                s.setExtSkylightValue(lx, y & 15, lz, l);
            }
        }
        return Math.max(0, y + 1); // the lowest cell set
    }

    static long pack(int x, int y, int z)
    {
        return ((long) (x + 30000000) << 34) | ((long) (z + 30000000) << 8) | y;
    }

    static int unpackX(long p)
    {
        return (int) (p >>> 34) - 30000000;
    }

    static int unpackZ(long p)
    {
        return (int) ((p >>> 8) & 0x3FFFFFFL) - 30000000;
    }

    static int unpackY(long p)
    {
        return (int) (p & 255);
    }
}
