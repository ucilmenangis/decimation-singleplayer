package net.decimation.worldgen.city;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.Schematic;
import net.minecraft.block.Block;

/**
 * Converted Lost Cities content for {@link LcCity}: buildings, multi
 * buildings and stairs parts made by tools/lcpack.py into
 * config/decimation_worldgen/lc/ (index.json + schematics). Local content
 * only (DeceasedCraft's buildings; never shipped). Schematic ids are the
 * ids of the world lcpack used; index.json "names" maps them to block names
 * and every schematic is remapped to this world's blocks on load.
 */
public final class LcContent
{
    /** One building or multi building, in chunks; ground floor layer = groundY. */
    public static final class Building
    {
        public final String file, pack, name;
        public final Map<String, Integer> styles = new HashMap<String, Integer>();
        public final int cx, cz, groundY, height;

        Building(String file, String pack, String name, int cx, int cz, int groundY, int height)
        {
            this.file = file;
            this.pack = pack;
            this.name = name;
            this.cx = cx;
            this.cz = cz;
            this.groundY = groundY;
            this.height = height;
        }
    }

    /** A loaded schematic with its blocks resolved for this world. */
    public static final class Shape
    {
        public final int width, height, length;
        public final Block[] blocks;
        public final byte[] meta;
        /** True where the schematic says "keep the world" (bedrock marker). */
        public final boolean[] skip;

        Shape(int width, int height, int length)
        {
            this.width = width;
            this.height = height;
            this.length = length;
            int n = width * height * length;
            blocks = new Block[n];
            meta = new byte[n];
            skip = new boolean[n];
        }

        public int index(int x, int y, int z)
        {
            return (y * length + z) * width + x;
        }
    }

    private static File dir;
    private static final List<Building> buildings = new ArrayList<Building>();
    private static final Map<String, List<String>> stairs = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> highways = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> decor = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> streets = new HashMap<String, List<String>>();
    private static final Map<Integer, Block> remap = new HashMap<Integer, Block>();
    private static final Map<String, Shape> shapes = new HashMap<String, Shape>();

    private LcContent()
    {
    }

    public static boolean active()
    {
        return !buildings.isEmpty();
    }

    public static List<Building> buildings()
    {
        return buildings;
    }

    /** Stairs part files for "<pack>:<style>" (empty when the style has none). */
    public static List<String> stairs(String style)
    {
        List<String> l = stairs.get(style);
        return l != null ? l : new ArrayList<String>();
    }

    /** Highway part files of a kind: open, open_bi, bridge, bridge_bi, tunnel, tunnel_bi. */
    public static List<String> highways(String kind)
    {
        List<String> l = highways.get(kind);
        return l != null ? l : new ArrayList<String>();
    }

    /** Decor part files of "<pack>:<style>": parks, fountains (street scenes) or fronts. */
    public static List<String> decor(String style, String kind)
    {
        List<String> l = decor.get(style + "/" + kind);
        return l != null ? l : new ArrayList<String>();
    }

    /** Street part files of "<pack>:<style>" for a connection kind: straight, end, bend, t, all, none. */
    public static List<String> streets(String style, String kind)
    {
        List<String> l = streets.get(style + "/" + kind);
        return l != null ? l : new ArrayList<String>();
    }

    public static void init(File configDir)
    {
        dir = new File(configDir, "lc");
        File index = new File(dir, "index.json");
        if (!index.exists())
        {
            FMLLog.info("[deciworldgen] no Lost Cities content (%s): city blocks stay procedural", index);
            return;
        }
        try
        {
            JsonObject o = new JsonParser().parse(new InputStreamReader(new FileInputStream(index), "UTF-8"))
                .getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("names").entrySet())
            {
                Block b = Block.getBlockFromName(e.getValue().getAsString());
                if (b != null)
                {
                    remap.put(Integer.parseInt(e.getKey()), b);
                }
            }
            for (JsonElement el : o.getAsJsonArray("buildings"))
            {
                JsonObject b = el.getAsJsonObject();
                Building bd = new Building(b.get("file").getAsString(), b.get("pack").getAsString(),
                    b.get("name").getAsString(), b.get("cx").getAsInt(), b.get("cz").getAsInt(),
                    b.get("groundY").getAsInt(), b.get("height").getAsInt());
                for (Map.Entry<String, JsonElement> s : b.getAsJsonObject("styles").entrySet())
                {
                    bd.styles.put(bd.pack + ":" + s.getKey(), Math.max(1, Math.round(s.getValue().getAsFloat() * 10)));
                }
                buildings.add(bd);
            }
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("stairs").entrySet())
            {
                List<String> files = new ArrayList<String>();
                for (JsonElement f : (JsonArray) e.getValue())
                {
                    files.add(f.getAsString());
                }
                stairs.put(e.getKey(), files);
            }
            byStyle(o, "decor", decor);
            byStyle(o, "streets", streets);
            if (o.has("highways"))
            {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("highways").entrySet())
                {
                    List<String> files = new ArrayList<String>();
                    for (JsonElement f : (JsonArray) e.getValue())
                    {
                        files.add(f.getAsString());
                    }
                    highways.put(e.getKey(), files);
                }
            }
            FMLLog.info("[deciworldgen] Lost Cities content: %d buildings, %d stairs styles, %d highway kinds",
                        buildings.size(), stairs.size(), highways.size());
        }
        catch (Exception e)
        {
            buildings.clear();
            FMLLog.warning("[deciworldgen] Lost Cities content not loaded: %s", e);
        }
    }

    /** {"<pack>:<style>": {kind: [file, ...]}} into map "<pack>:<style>/<kind>" -> files. */
    private static void byStyle(JsonObject o, String key, Map<String, List<String>> into)
    {
        if (!o.has(key))
        {
            return;
        }
        for (Map.Entry<String, JsonElement> st : o.getAsJsonObject(key).entrySet())
        {
            for (Map.Entry<String, JsonElement> e : st.getValue().getAsJsonObject().entrySet())
            {
                List<String> files = new ArrayList<String>();
                for (JsonElement f : (JsonArray) e.getValue())
                {
                    files.add(f.getAsString());
                }
                into.put(st.getKey() + "/" + e.getKey(), files);
            }
        }
    }

    /** The schematic of a file, remapped to this world's blocks (cached). */
    public static synchronized Shape shape(String file)
    {
        Shape s = shapes.get(file);
        if (s != null || shapes.containsKey(file))
        {
            return s;
        }
        try
        {
            Schematic raw = Schematic.load(new File(dir, file));
            s = new Shape(raw.width, raw.height, raw.length);
            for (int i = 0; i < raw.blocks.length; i++)
            {
                int id = raw.blocks[i];
                if (id == 7)
                {
                    s.skip[i] = true;
                }
                else if (id != 0)
                {
                    s.blocks[i] = remap.get(id);
                    s.meta[i] = raw.data[i];
                }
            }
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] Lost Cities schematic %s: %s", file, e);
            s = null;
        }
        shapes.put(file, s);
        return s;
    }
}
