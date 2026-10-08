package net.decimation.worldgen.assets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * In-game capture (docs/furniture_sets.md "Capture"): build furniture in
 * creative, mark two corners, save it as a furniture set JSON (or a raw
 * part for the later part planner). Corners are per player.
 *
 * Set capture: the box between the corners (at least 3 high from the
 * lower corner, the storey's clear height); the set's back (row 0) is the
 * box side with the most solid blocks just outside it, or the given side.
 * Facing metadata is turned into "face" values relative to that wall, so
 * the set works against any wall. Sponge marks "keep free" ('.').
 */
public final class Capture
{
    private static final Map<String, int[]> POS1 = new HashMap<String, int[]>();
    private static final Map<String, int[]> POS2 = new HashMap<String, int[]>();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /** Plan vectors of the four back sides (same as Building.SIDE_OUT / SIDE_ALONG). */
    private static final int[][] OUT = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    private static final int[][] ALONG = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
    private static final String[] SIDE_NAMES = {"north", "south", "west", "east"};

    private Capture()
    {
    }

    public static void setPos(String player, int which, int x, int y, int z)
    {
        (which == 1 ? POS1 : POS2).put(player, new int[] {x, y, z});
    }

    /** Box {x0, y0, z0, x1, y1, z1} of a player's corners, null if one is missing. */
    public static int[] box(String player)
    {
        int[] a = POS1.get(player), b = POS2.get(player);
        if (a == null || b == null)
        {
            return null;
        }
        return new int[] {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                          Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])};
    }

    /**
     * Captures a furniture set; side: "north" | "south" | "west" | "east" or
     * null (auto). Returns a report; throws IllegalArgumentException with a
     * message for the player on bad input.
     */
    public static String captureSet(World world, int[] box, String name, String room, String sideName, int weight,
                                    File dir) throws Exception
    {
        int x0 = box[0], y0 = box[1], z0 = box[2], x1 = box[3], z1 = box[5];
        int height = 3; // a set spans the storey's clear height; empty top layers are dropped
        int side = sideName == null ? autoSide(world, box) : indexOf(sideName);
        if (side < 0)
        {
            throw new IllegalArgumentException("no wall found behind the box: give the side its back stands against "
                                               + "(north, south, west, east)");
        }
        int width = side < 2 ? x1 - x0 + 1 : z1 - z0 + 1;
        int depth = side < 2 ? z1 - z0 + 1 : x1 - x0 + 1;
        char[][][] grid = new char[height][depth][width];
        Map<String, Character> keys = new LinkedHashMap<String, Character>();
        Map<Character, JsonObject> palette = new LinkedHashMap<Character, JsonObject>();
        for (int y = 0; y < height; y++)
        {
            for (int r = 0; r < depth; r++)
            {
                for (int c = 0; c < width; c++)
                {
                    int[] p = cell(side, r, c, x0, z0, x1, z1);
                    int wy = y0 + y;
                    Block b = world.getBlock(p[0], wy, p[1]);
                    int meta = world.getBlockMetadata(p[0], wy, p[1]);
                    char ch = ' ';
                    if (b == Blocks.sponge)
                    {
                        ch = '.';
                    }
                    else if (b != Blocks.air && !upperPart(world, p[0], wy, p[1]))
                    {
                        JsonObject e = entry(world, b, meta, p[0], wy, p[1], OUT[side], ALONG[side]);
                        String k = e.toString();
                        Character got = keys.get(k);
                        if (got == null)
                        {
                            got = freeChar(name(b), palette);
                            keys.put(k, got);
                            palette.put(got, e);
                        }
                        ch = got;
                    }
                    grid[y][r][c] = ch;
                }
            }
        }
        if (palette.isEmpty())
        {
            throw new IllegalArgumentException("the box is empty");
        }
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        JsonArray rooms = new JsonArray();
        rooms.add(new JsonPrimitive(room));
        o.add("rooms", rooms);
        o.addProperty("weight", weight);
        o.addProperty("comment", "Captured in game against the " + SIDE_NAMES[side] + " wall at " + x0 + "," + y0
                                 + "," + z0 + ".");
        o.add("layers", layers(grid));
        JsonObject pal = new JsonObject();
        for (Map.Entry<Character, JsonObject> e : palette.entrySet())
        {
            pal.add(String.valueOf(e.getKey()), e.getValue());
        }
        o.add("palette", pal);
        File f = new File(dir, name + ".json");
        write(f, o);
        return "set " + name + " (" + width + " wide, " + depth + " deep, back on the " + SIDE_NAMES[side]
               + " side, " + palette.size() + " palette entries) saved to " + f.getName();
    }

    /** Raw part: every block with absolute metadata, layer by layer from the lower corner. */
    public static String capturePart(World world, int[] box, String name, File dir) throws Exception
    {
        int x0 = box[0], y0 = box[1], z0 = box[2], x1 = box[3], y1 = box[4], z1 = box[5];
        Map<String, Character> keys = new LinkedHashMap<String, Character>();
        Map<Character, JsonObject> palette = new LinkedHashMap<Character, JsonObject>();
        JsonArray layers = new JsonArray();
        for (int y = y0; y <= y1; y++)
        {
            JsonArray rows = new JsonArray();
            for (int z = z0; z <= z1; z++)
            {
                StringBuilder row = new StringBuilder();
                for (int x = x0; x <= x1; x++)
                {
                    Block b = world.getBlock(x, y, z);
                    if (b == Blocks.air)
                    {
                        row.append(' ');
                        continue;
                    }
                    JsonObject e = new JsonObject();
                    e.addProperty("block", name(b));
                    int meta = world.getBlockMetadata(x, y, z);
                    if (meta != 0)
                    {
                        e.addProperty("meta", meta);
                    }
                    String k = e.toString();
                    Character got = keys.get(k);
                    if (got == null)
                    {
                        got = freeChar(name(b), palette);
                        keys.put(k, got);
                        palette.put(got, e);
                    }
                    row.append(got.charValue());
                }
                rows.add(new JsonPrimitive(row.toString()));
            }
            layers.add(rows);
        }
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("comment", "Raw capture (absolute metadata, rows = z from north, columns = x from west) at "
                                 + x0 + "," + y0 + "," + z0 + "; for the part planner (not used yet).");
        o.addProperty("xsize", x1 - x0 + 1);
        o.addProperty("zsize", z1 - z0 + 1);
        o.add("layers", layers);
        JsonObject pal = new JsonObject();
        for (Map.Entry<Character, JsonObject> e : palette.entrySet())
        {
            pal.add(String.valueOf(e.getKey()), e.getValue());
        }
        o.add("palette", pal);
        if (!dir.isDirectory())
        {
            dir.mkdirs();
        }
        File f = new File(dir, name + ".json");
        write(f, o);
        return "part " + name + " (" + (x1 - x0 + 1) + "x" + (y1 - y0 + 1) + "x" + (z1 - z0 + 1) + ", "
               + palette.size() + " palette entries) saved to " + f.getName();
    }

    /** World {x, z} of set cell (r, c) for a back side (as Building.setCell, x = plan fx). */
    static int[] cell(int side, int r, int c, int x0, int z0, int x1, int z1)
    {
        switch (side)
        {
            case 0: return new int[] {x0 + c, z0 + r};
            case 1: return new int[] {x1 - c, z1 - r};
            case 2: return new int[] {x0 + r, z1 - c};
            default: return new int[] {x1 - r, z0 + c};
        }
    }

    private static int indexOf(String side)
    {
        for (int i = 0; i < 4; i++)
        {
            if (SIDE_NAMES[i].startsWith(side.toLowerCase()))
            {
                return i;
            }
        }
        return -1;
    }

    /** The side with the most solid blocks right outside it (at least half), else -1. */
    private static int autoSide(World world, int[] box)
    {
        int best = -1;
        double bestShare = 0.5;
        for (int side = 0; side < 4; side++)
        {
            int solid = 0, n = 0;
            for (int y = box[1]; y < box[1] + 3; y++)
            {
                if (side < 2)
                {
                    int z = side == 0 ? box[2] - 1 : box[5] + 1;
                    for (int x = box[0]; x <= box[3]; x++, n++)
                    {
                        solid += world.getBlock(x, y, z).isNormalCube() ? 1 : 0;
                    }
                }
                else
                {
                    int x = side == 2 ? box[0] - 1 : box[3] + 1;
                    for (int z = box[2]; z <= box[5]; z++, n++)
                    {
                        solid += world.getBlock(x, y, z).isNormalCube() ? 1 : 0;
                    }
                }
            }
            double share = n == 0 ? 0 : solid / (double) n;
            if (share > bestShare)
            {
                bestShare = share;
                best = side;
            }
        }
        return best;
    }

    /** True for a non master cell of a Decimation multiblock (the generator rebuilds those). */
    private static boolean upperPart(World world, int x, int y, int z)
    {
        TileEntity te = world.getTileEntity(x, y, z);
        return net.decimation.fixes.Deci.isMultiblockPart(te) && !net.decimation.fixes.Deci.isMultiblockMaster(te);
    }

    /** Palette entry with facing converted to a face relative to the wall (out, in, left, right). */
    private static JsonObject entry(World world, Block b, int meta, int x, int y, int z, int[] out, int[] along)
    {
        JsonObject e = new JsonObject();
        e.addProperty("block", name(b));
        String type = null;
        int[] dir = null;
        String part = null;
        if (b instanceof BlockStairs && meta < 4)
        {
            type = "seat";
            dir = new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}[meta];
        }
        else if (b instanceof BlockBed)
        {
            type = "bed";
            dir = new int[][] {{0, 1}, {-1, 0}, {0, -1}, {1, 0}}[meta & 3];
            part = (meta & 8) != 0 ? "head" : "foot";
        }
        else if ((b instanceof BlockChest || b instanceof BlockFurnace || b instanceof BlockEnderChest)
                 && meta >= 2 && meta <= 5)
        {
            type = "vanilla";
            dir = new int[][] {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}[meta - 2];
        }
        else if (b instanceof BlockTrapDoor && (meta & 4) != 0 && meta < 8)
        {
            type = "trapdoor";
            dir = new int[][] {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}[meta & 3];
        }
        else if (b instanceof BlockTripWireHook && meta < 4)
        {
            type = "hook";
            dir = new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}[meta];
        }
        else if (name(b).startsWith("deci:") && b.hasTileEntity(meta) && meta >= 2 && meta <= 5
                 && !name(b).contains("Door"))
        {
            type = "prop";
            dir = new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}[meta - 2];
        }
        String face = dir == null ? null : face(dir, out, along);
        if (face == null)
        {
            if (meta != 0)
            {
                e.addProperty("meta", meta);
            }
            return e;
        }
        e.addProperty("type", type);
        e.addProperty("face", face);
        if (part != null)
        {
            e.addProperty("part", part);
        }
        return e;
    }

    private static String face(int[] d, int[] out, int[] along)
    {
        if (d[0] == out[0] && d[1] == out[1]) return "out";
        if (d[0] == -out[0] && d[1] == -out[1]) return "in";
        if (d[0] == along[0] && d[1] == along[1]) return "right";
        if (d[0] == -along[0] && d[1] == -along[1]) return "left";
        return null;
    }

    static String name(Block b)
    {
        Object n = Block.blockRegistry.getNameForObject(b);
        return n == null ? "minecraft:air" : n.toString();
    }

    /** A free palette char, preferring the first letter of the block's short name. */
    private static char freeChar(String blockName, Map<Character, JsonObject> used)
    {
        String shortName = blockName.substring(blockName.indexOf(':') + 1).replace("Block", "");
        if (!shortName.isEmpty())
        {
            char f = shortName.charAt(0);
            for (char c : new char[] {Character.toUpperCase(f), Character.toLowerCase(f)})
            {
                if (CHARS.indexOf(c) >= 0 && !used.containsKey(c))
                {
                    return c;
                }
            }
        }
        for (char c : CHARS.toCharArray())
        {
            if (!used.containsKey(c))
            {
                return c;
            }
        }
        throw new IllegalArgumentException("more than " + CHARS.length() + " different blocks");
    }

    /** layers[y] = rows (depth), trailing empty rows and layers dropped. */
    private static JsonArray layers(char[][][] grid)
    {
        int top = grid.length;
        while (top > 1 && empty(grid[top - 1], 0))
        {
            top--;
        }
        JsonArray out = new JsonArray();
        for (int y = 0; y < top; y++)
        {
            int rows = grid[y].length;
            while (rows > 1 && blank(grid[y][rows - 1]))
            {
                rows--;
            }
            JsonArray l = new JsonArray();
            for (int r = 0; r < rows; r++)
            {
                l.add(new JsonPrimitive(new String(grid[y][r])));
            }
            out.add(l);
        }
        return out;
    }

    private static boolean empty(char[][] layer, int from)
    {
        for (int r = from; r < layer.length; r++)
        {
            if (!blank(layer[r]))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean blank(char[] row)
    {
        for (char c : row)
        {
            if (c != ' ')
            {
                return false;
            }
        }
        return true;
    }

    private static void write(File f, JsonObject o) throws Exception
    {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try
        {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o, w);
        }
        finally
        {
            w.close();
        }
    }
}
