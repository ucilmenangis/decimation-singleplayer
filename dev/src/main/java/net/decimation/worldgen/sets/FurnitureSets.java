package net.decimation.worldgen.sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;

/**
 * Registry of furniture sets. Built-in sets live in the mod jar
 * (assets/deciworldgen/sets/, listed in index.txt) and are copied to
 * config/decimation_worldgen/sets/ on first start; from then on the
 * config copies are what is loaded, so the user can edit them and reload
 * in game (/deciworldgen reload). A file named like a built-in replaces it.
 */
public final class FurnitureSets
{
    private static final String RES = "/assets/deciworldgen/sets/";
    private static volatile Map<String, List<FurnitureSet>> byRoom = new HashMap<String, List<FurnitureSet>>();
    private static volatile List<FurnitureSet> all = new ArrayList<FurnitureSet>();
    private static File dir;

    private FurnitureSets()
    {
    }

    public static void init(File configDir)
    {
        dir = new File(configDir, "sets");
        if (!dir.isDirectory() && dir.mkdirs())
        {
            for (String name : builtinNames())
            {
                copyBuiltin(name, new File(dir, name));
            }
        }
        reload();
    }

    /** Re-reads every set; returns a short report. */
    public static synchronized String reload()
    {
        Map<String, List<FurnitureSet>> rooms = new HashMap<String, List<FurnitureSet>>();
        List<FurnitureSet> sets = new ArrayList<FurnitureSet>();
        Map<String, JsonObject> sources = new HashMap<String, JsonObject>();
        for (String name : builtinNames())
        {
            JsonObject o = parse(FurnitureSets.class.getResourceAsStream(RES + name), name);
            if (o != null)
            {
                sources.put(name, o);
            }
        }
        File[] files = dir != null ? dir.listFiles() : null;
        if (files != null)
        {
            for (File f : files)
            {
                if (f.getName().endsWith(".json"))
                {
                    try
                    {
                        JsonObject o = parse(new FileInputStream(f), f.getName());
                        if (o != null)
                        {
                            sources.put(f.getName(), o); // config file wins over the built-in
                        }
                    }
                    catch (Exception e)
                    {
                        FMLLog.warning("[deciworldgen] set %s: %s", f.getName(), e);
                    }
                }
            }
        }
        int errors = 0;
        for (Map.Entry<String, JsonObject> e : sources.entrySet())
        {
            try
            {
                FurnitureSet s = build(e.getValue());
                sets.add(s);
                for (String room : s.rooms)
                {
                    if (!rooms.containsKey(room))
                    {
                        rooms.put(room, new ArrayList<FurnitureSet>());
                    }
                    rooms.get(room).add(s);
                }
            }
            catch (Exception ex)
            {
                errors++;
                FMLLog.warning("[deciworldgen] set %s not loaded: %s", e.getKey(), ex);
            }
        }
        byRoom = rooms;
        all = sets;
        String report = sets.size() + " furniture sets loaded, " + errors + " with errors";
        FMLLog.info("[deciworldgen] %s", report);
        return report;
    }

    public static List<FurnitureSet> forRoom(String room)
    {
        List<FurnitureSet> l = byRoom.get(room);
        return l != null ? l : new ArrayList<FurnitureSet>();
    }

    public static List<FurnitureSet> all()
    {
        return all;
    }

    private static List<String> builtinNames()
    {
        List<String> out = new ArrayList<String>();
        InputStream in = FurnitureSets.class.getResourceAsStream(RES + "index.txt");
        if (in == null)
        {
            return out;
        }
        try
        {
            java.io.BufferedReader r = new java.io.BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (line.endsWith(".json"))
                {
                    out.add(line);
                }
            }
            r.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] set index: %s", e);
        }
        return out;
    }

    private static void copyBuiltin(String name, File to)
    {
        InputStream in = FurnitureSets.class.getResourceAsStream(RES + name);
        if (in == null)
        {
            return;
        }
        try
        {
            OutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0)
            {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] copy set %s: %s", name, e);
        }
    }

    private static JsonObject parse(InputStream in, String name)
    {
        if (in == null)
        {
            return null;
        }
        try
        {
            JsonElement e = new JsonParser().parse(new InputStreamReader(in, "UTF-8"));
            in.close();
            return e.getAsJsonObject();
        }
        catch (Exception ex)
        {
            FMLLog.warning("[deciworldgen] set %s is not valid JSON: %s", name, ex);
            return null;
        }
    }

    private static FurnitureSet build(JsonObject o)
    {
        String name = o.get("name").getAsString();
        List<String> rooms = new ArrayList<String>();
        for (JsonElement e : o.getAsJsonArray("rooms"))
        {
            rooms.add(e.getAsString());
        }
        int weight = o.has("weight") ? o.get("weight").getAsInt() : 10;
        JsonArray ls = o.getAsJsonArray("layers");
        char[][][] layers = new char[ls.size()][][];
        for (int y = 0; y < ls.size(); y++)
        {
            JsonArray rows = ls.get(y).getAsJsonArray();
            layers[y] = new char[rows.size()][];
            for (int r = 0; r < rows.size(); r++)
            {
                layers[y][r] = rows.get(r).getAsString().toCharArray();
            }
        }
        Map<Character, FurnitureSet.Entry> palette = new HashMap<Character, FurnitureSet.Entry>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("palette").entrySet())
        {
            JsonObject p = e.getValue().getAsJsonObject();
            String type = p.has("type") ? p.get("type").getAsString() : null;
            FurnitureSet.Entry entry = new FurnitureSet.Entry(
                type, p.has("face") ? p.get("face").getAsString() : null,
                p.has("meta") ? p.get("meta").getAsInt() : 0,
                p.has("part") ? p.get("part").getAsString() : null);
            List<String> names = new ArrayList<String>();
            if (p.has("block"))
            {
                names.add(p.get("block").getAsString());
            }
            if (p.has("pick"))
            {
                for (JsonElement b : p.getAsJsonArray("pick"))
                {
                    names.add(b.getAsString());
                }
            }
            for (String n : names)
            {
                Block b = Block.getBlockFromName(n);
                if (b == null)
                {
                    FMLLog.warning("[deciworldgen] set %s: unknown block %s", name, n);
                }
                else
                {
                    entry.blocks.add(b);
                }
            }
            palette.put(e.getKey().charAt(0), entry);
        }
        return new FurnitureSet(name, rooms, weight, layers, palette);
    }
}
