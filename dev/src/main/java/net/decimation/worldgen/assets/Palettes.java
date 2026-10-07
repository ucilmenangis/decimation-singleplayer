package net.decimation.worldgen.assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.FMLLog;
import net.decimation.worldgen.sets.FurnitureSet;
import net.minecraft.block.Block;

/**
 * Named palettes and styles (docs/worldgen_architecture.md "Assets").
 * A palette maps chars to blocks (same entry format as a furniture set's
 * inline palette); a style is a weighted list of palettes, one picked per
 * placement, so one layout gets several looks. Looked up by name at
 * placement time, so /deciworldgen reload applies at once. Loaded by
 * FurnitureSets.init / reload.
 */
public final class Palettes
{
    public static final AssetDir PALETTES = new AssetDir("palettes");
    public static final AssetDir STYLES = new AssetDir("styles");

    private static volatile Map<String, Map<Character, FurnitureSet.Entry>> palettes =
        new HashMap<String, Map<Character, FurnitureSet.Entry>>();
    private static volatile Map<String, Style> styles = new HashMap<String, Style>();

    private Palettes()
    {
    }

    /** Re-reads palettes and styles; returns a short report. */
    public static synchronized String reload()
    {
        Map<String, Map<Character, FurnitureSet.Entry>> p = new HashMap<String, Map<Character, FurnitureSet.Entry>>();
        int errors = 0;
        for (Map.Entry<String, JsonObject> e : PALETTES.load().entrySet())
        {
            try
            {
                JsonObject o = e.getValue();
                String name = o.get("name").getAsString();
                p.put(name, palette(o.getAsJsonObject("palette"), name));
            }
            catch (Exception ex)
            {
                errors++;
                FMLLog.warning("[deciworldgen] palette %s not loaded: %s", e.getKey(), ex);
            }
        }
        Map<String, Style> s = new HashMap<String, Style>();
        for (Map.Entry<String, JsonObject> e : STYLES.load().entrySet())
        {
            try
            {
                JsonObject o = e.getValue();
                Style st = new Style(o.get("name").getAsString());
                for (JsonElement el : o.getAsJsonArray("palettes"))
                {
                    JsonObject po = el.getAsJsonObject();
                    st.names.add(po.get("palette").getAsString());
                    st.weights.add(po.has("weight") ? Math.max(1, po.get("weight").getAsInt()) : 1);
                }
                s.put(st.name, st);
            }
            catch (Exception ex)
            {
                errors++;
                FMLLog.warning("[deciworldgen] style %s not loaded: %s", e.getKey(), ex);
            }
        }
        palettes = p;
        styles = s;
        return p.size() + " palettes, " + s.size() + " styles" + (errors > 0 ? ", " + errors + " with errors" : "");
    }

    /** A named palette (null when unknown). */
    public static Map<Character, FurnitureSet.Entry> palette(String name)
    {
        return name == null ? null : palettes.get(name);
    }

    /** The palette a style picks for u in [0, 1) (null when unknown). */
    public static Map<Character, FurnitureSet.Entry> fromStyle(String style, double u)
    {
        Style s = style == null ? null : styles.get(style);
        if (s == null || s.names.isEmpty())
        {
            return null;
        }
        int total = 0;
        for (int w : s.weights)
        {
            total += w;
        }
        double t = u * total;
        for (int i = 0; i < s.names.size(); i++)
        {
            t -= s.weights.get(i);
            if (t < 0)
            {
                return palettes.get(s.names.get(i));
            }
        }
        return palettes.get(s.names.get(s.names.size() - 1));
    }

    /** Parses a palette object: char -> entry. */
    public static Map<Character, FurnitureSet.Entry> palette(JsonObject o, String owner)
    {
        Map<Character, FurnitureSet.Entry> out = new HashMap<Character, FurnitureSet.Entry>();
        if (o == null)
        {
            return out;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet())
        {
            out.put(e.getKey().charAt(0), entry(e.getValue().getAsJsonObject(), owner));
        }
        return out;
    }

    /** One palette entry: block or pick list, type, face, meta, part. */
    public static FurnitureSet.Entry entry(JsonObject p, String owner)
    {
        FurnitureSet.Entry entry = new FurnitureSet.Entry(
            p.has("type") ? p.get("type").getAsString() : null,
            p.has("face") ? p.get("face").getAsString() : null,
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
                FMLLog.warning("[deciworldgen] %s: unknown block %s", owner, n);
            }
            else
            {
                entry.blocks.add(b);
            }
        }
        return entry;
    }

    private static final class Style
    {
        final String name;
        final List<String> names = new ArrayList<String>();
        final List<Integer> weights = new ArrayList<Integer>();

        Style(String name)
        {
            this.name = name;
        }
    }
}
