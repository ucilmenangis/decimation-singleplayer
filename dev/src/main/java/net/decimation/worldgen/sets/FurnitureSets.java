package net.decimation.worldgen.sets;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.assets.AssetDir;
import net.decimation.worldgen.assets.Condition;
import net.decimation.worldgen.assets.Palettes;

/**
 * Registry of furniture sets. Built-in sets live in the mod jar
 * (assets/deciworldgen/sets/, listed in index.txt) and are copied to
 * config/decimation_worldgen/sets/ on first start; from then on the
 * config copies are what is loaded, so the user can edit them and reload
 * in game (/deciworldgen reload). A file named like a built-in replaces it.
 * Palettes and styles (assets.Palettes) are loaded alongside.
 */
public final class FurnitureSets
{
    public static final AssetDir SETS = new AssetDir("sets");
    private static volatile Map<String, List<FurnitureSet>> byRoom = new HashMap<String, List<FurnitureSet>>();
    private static volatile List<FurnitureSet> all = new ArrayList<FurnitureSet>();

    private FurnitureSets()
    {
    }

    public static void init(File configDir)
    {
        SETS.init(configDir);
        Palettes.PALETTES.init(configDir);
        Palettes.STYLES.init(configDir);
        reload();
    }

    /** Re-reads every set, palette and style; returns a short report. */
    public static synchronized String reload()
    {
        String palettes = Palettes.reload();
        Map<String, List<FurnitureSet>> rooms = new HashMap<String, List<FurnitureSet>>();
        List<FurnitureSet> sets = new ArrayList<FurnitureSet>();
        int errors = 0;
        // AssetDir.load is sorted by file name: stable numbering for previews and seeds
        for (Map.Entry<String, JsonObject> e : SETS.load().entrySet())
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
        java.util.Comparator<FurnitureSet> byName = new java.util.Comparator<FurnitureSet>()
        {
            public int compare(FurnitureSet a, FurnitureSet b)
            {
                return a.name.compareTo(b.name);
            }
        };
        java.util.Collections.sort(sets, byName);
        for (List<FurnitureSet> l : rooms.values())
        {
            java.util.Collections.sort(l, byName);
        }
        byRoom = rooms;
        all = sets;
        String report = sets.size() + " furniture sets loaded, " + errors + " with errors; " + palettes;
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
        return new FurnitureSet(name, rooms, weight, layers,
            Palettes.palette(o.has("palette") ? o.getAsJsonObject("palette") : null, "set " + name),
            Condition.parse(o.has("when") ? o.getAsJsonObject("when") : null),
            o.has("base") ? o.get("base").getAsString() : null,
            o.has("style") ? o.get("style").getAsString() : null);
    }
}
