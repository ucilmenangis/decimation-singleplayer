package net.decimation.worldgen;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * A procedurally generated ruined building on one city lot.
 *
 * Fully determined by its plan (position, size, floors, kind, front, seed):
 * {@link #blockAt} answers "what is at this local position" without any state,
 * so a building that spans several chunks can be written slice by slice, each
 * chunk writing only its own columns, and still come out whole.
 *
 * Layout: FLOOR blocks per storey. Each storey has a floor layer, outer walls
 * with corner pillars and windows, one partition wall with a doorway, and a
 * ladder shaft in the back corner joining all storeys. Ground floor door in
 * the middle of the front wall (facing the street). Flat roof with a parapet.
 * Decay is a deterministic hash: missing and mossy wall blocks, broken
 * windows, rubble, and on some buildings a collapsed top corner.
 */
public class Building
{
    public static final int APARTMENT = 0, OFFICE = 1, SHOP = 2;
    public static final String[] KIND_NAME = {"apartment", "office", "shop"};
    /** Blocks per storey, floor layer included. */
    public static final int FLOOR = 4;
    /** Front wall: the side facing the street. */
    public static final int FRONT_WEST = 0, FRONT_EAST = 1;

    public final String id;
    public final int minX, minZ, width, length, floors, kind, front;
    public final long seed;
    private final Props props;
    private final Block wall;
    private final int wallMeta;
    private final boolean collapsed;

    /** Resolved Decimation props; any entry may be null if missing from the jar. */
    public static class Props
    {
        public Block[] crates = new Block[0];
        public Block[] furniture = new Block[0];
    }

    public Building(String id, int minX, int minZ, int width, int length, int floors,
                    int kind, int front, long seed, Props props)
    {
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.width = width;
        this.length = length;
        this.floors = floors;
        this.kind = kind;
        this.front = front;
        this.seed = seed;
        this.props = props;
        int pick = (int) (unit(0, 999, 0) * 4);
        switch (kind)
        {
            case OFFICE:
                // white / light gray / cyan / gray hardened clay
                wall = Blocks.stained_hardened_clay;
                wallMeta = new int[] {0, 8, 9, 7}[pick];
                break;
            case SHOP:
                wall = pick < 2 ? Blocks.stonebrick : Blocks.stained_hardened_clay;
                wallMeta = pick < 2 ? 0 : new int[] {0, 0, 4, 1}[pick];
                break;
            default:
                wall = pick < 2 ? Blocks.brick_block : Blocks.stained_hardened_clay;
                wallMeta = pick < 2 ? 0 : new int[] {0, 0, 12, 1}[pick];
        }
        collapsed = floors >= 2 && unit(1, 999, 1) < 0.35;
    }

    public int maxX()
    {
        return minX + width - 1;
    }

    public int maxZ()
    {
        return minZ + length - 1;
    }

    /** Height of the tallest block above the base (roof parapet). */
    public int height()
    {
        return floors * FLOOR + 1;
    }

    /** Block and metadata, packed as (Block, meta) via the out array; null = air. */
    public Block blockAt(int lx, int ly, int lz, int[] meta)
    {
        meta[0] = 0;
        int top = floors * FLOOR;
        boolean edgeX = lx == 0 || lx == width - 1;
        boolean edgeZ = lz == 0 || lz == length - 1;
        boolean edge = edgeX || edgeZ;
        boolean corner = edgeX && edgeZ;

        if (collapsed && ly > top - FLOOR && inCollapse(lx, lz))
        {
            return null; // the top corner fell in
        }
        if (ly > top)
        {
            // parapet around the roof
            if (ly == top + 1 && edge && unit(lx, ly, lz) > 0.15)
            {
                return trim(meta);
            }
            return null;
        }
        int within = ly % FLOOR;
        int storey = ly / FLOOR;
        boolean ladderCol = lx == width - 2 && lz == length - 2;

        if (within == 0)
        {
            // floor / ceiling layer; ground layer is solid, upper ones have a ladder hole
            if (edge)
            {
                return trim(meta);
            }
            if (ladderCol && storey > 0)
            {
                return ladder(meta);
            }
            if (storey > 0 && collapsed && storey == floors - 1 && nearCollapse(lx, lz))
            {
                return null;
            }
            if (kind == OFFICE)
            {
                meta[0] = 0;
                return Blocks.double_stone_slab;
            }
            meta[0] = kind == SHOP ? 0 : 1; // oak / spruce planks
            return Blocks.planks;
        }

        if (edge)
        {
            if (corner)
            {
                return trim(meta);
            }
            int along = edgeX ? lz : lx;
            int span = edgeX ? length : width;
            boolean frontWall = (front == FRONT_WEST && lx == 0) || (front == FRONT_EAST && lx == width - 1);
            if (frontWall && storey == 0 && within <= 2 && Math.abs(along - span / 2) <= (kind == SHOP ? 1 : 0))
            {
                return null; // entrance
            }
            if (isWindow(along, within, storey, frontWall))
            {
                if (unit(lx, ly, lz) < 0.45)
                {
                    return null; // broken
                }
                return kind == SHOP && storey == 0 ? Blocks.iron_bars : Blocks.glass_pane;
            }
            double d = unit(lx, ly, lz);
            if (d < 0.05 && storey > 0)
            {
                return null; // hole in the wall
            }
            if (d < 0.12)
            {
                meta[0] = 0;
                return d < 0.09 ? Blocks.cobblestone : Blocks.mossy_cobblestone;
            }
            meta[0] = wallMeta;
            return wall;
        }

        if (ladderCol)
        {
            return ladder(meta); // from the ground floor up through the roof hatch
        }
        // partition wall across the middle with a doorway
        int mid = width / 2;
        if (lx == mid && width >= 9)
        {
            if (within <= 2 && Math.abs(lz - length / 2) <= 0)
            {
                return null;
            }
            meta[0] = kind == OFFICE ? 0 : 2;
            return kind == OFFICE ? Blocks.stonebrick : Blocks.planks;
        }
        if (within == 1)
        {
            return furnish(lx, ly, lz, meta);
        }
        return null;
    }

    private Block trim(int[] meta)
    {
        meta[0] = 0;
        return Blocks.stonebrick;
    }

    private Block ladder(int[] meta)
    {
        // ladder in column (width-2, length-2), attached to the east wall:
        // facing west = meta 4
        meta[0] = 4;
        return Blocks.ladder;
    }

    private boolean isWindow(int along, int within, int storey, boolean frontWall)
    {
        if (kind == SHOP && storey == 0)
        {
            return frontWall && within >= 1 && within <= 2 && along % 4 != 0;
        }
        if (kind == OFFICE)
        {
            return within >= 1 && within <= 2 && along % 3 != 0;
        }
        return within == 2 && along % 3 == 1;
    }

    private Block furnish(int lx, int ly, int lz, int[] meta)
    {
        // keep the walkways from the entrance and to the ladder clear
        if (lz == length / 2 || lx == width - 2 || lx == width - 3 || lz == length - 2)
        {
            return null;
        }
        double d = unit(lx, ly, lz);
        meta[0] = 2 + (int) (unit(lz, ly, lx) * 4); // prop facing 2..5
        if (d < 0.035 && props.crates.length > 0)
        {
            return props.crates[(int) (unit(ly, lx, lz) * props.crates.length)];
        }
        if (d < 0.09 && props.furniture.length > 0)
        {
            return props.furniture[(int) (unit(ly, lz, lx) * props.furniture.length)];
        }
        if (d < 0.12)
        {
            meta[0] = 3; // cobblestone slab
            return Blocks.stone_slab;
        }
        return null;
    }

    private boolean inCollapse(int lx, int lz)
    {
        return lx >= width / 2 && lz >= length / 2;
    }

    private boolean nearCollapse(int lx, int lz)
    {
        return lx >= width / 2 - 1 && lz >= length / 2 - 1;
    }

    /** Deterministic pseudo random number in [0, 1) from position and seed. */
    double unit(int x, int y, int z)
    {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 31) * 0xBF58476D1CE4E5B9L;
        h ^= y * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 29) * 0xBF58476D1CE4E5B9L;
        h ^= z * 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }
}
