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

import static net.decimation.worldgen.city.LcCity.*;

/** A street chunk: road at the ground, optional stairs part above it. */
final class StreetPlan implements net.decimation.worldgen.FixedBase
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
    /** The district's street part (slice 0 = street surface) and its turns, or null for our own street. */
    private final LcContent.Shape piece;
    private final int pieceTurns;

    StreetPlan(String id, int minX, int minZ, int ground, Block road, LcContent.Shape stairs, int turns,
               int kind, StreetProps props, long seed, LcCity city, String style, LcContent.Shape scene,
               LcContent.Shape piece, int pieceTurns)
    {
        this.piece = piece;
        this.pieceTurns = pieceTurns;
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
    public int height() { return stairs != null ? stairs.height + 1 : Math.max(13, piece != null ? piece.height : 0); }
    public int clearAbove() { return 14; }
    public int maxSpread() { return 255; }
    public Block foundation() { return Blocks.stone; }
    public net.decimation.worldgen.ZoneKind zone() { return null; }
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
        if (piece != null)
        {
            return pieceAt(lx, ly, lz, across, along, meta);
        }
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

    /**
     * A street of the district's own parts: the part (its lamps, benches,
     * sidewalks, paint) with scenes and fronts on top, and a wreck now
     * and then in its 6 wide road (lanes 6 / 9, 7% each).
     */
    private Block pieceAt(int lx, int ly, int lz, int across, int along, int[] meta)
    {
        if (ly == 0)
        {
            Block b = partAt(piece, pieceTurns, lx, 0, lz, meta);
            if (b == null)
            {
                meta[0] = 0;
                return road;
            }
            return b;
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
        Block b = partAt(piece, pieceTurns, lx, ly, lz, meta);
        if (b != null)
        {
            return b;
        }
        meta[0] = 0;
        if (ly != 1 || kind == 2 || scene != null || props.cars.length == 0
            || (across != 6 && across != 9) || along != 7)
        {
            return null;
        }
        long hv = hl(seed ^ 0x5354524545L, minX + lx, minZ + lz);
        if ((hv & 0xFF) >= 18)
        {
            return null;
        }
        meta[0] = kind == 0 ? (across == 6 ? 5 : 3) : (across == 6 ? 4 : 2);
        return props.cars[(int) ((hv >>> 8) % props.cars.length)];
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
