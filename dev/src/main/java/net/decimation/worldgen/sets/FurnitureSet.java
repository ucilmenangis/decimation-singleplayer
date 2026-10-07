package net.decimation.worldgen.sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;

/**
 * A furniture set: a small group of blocks and props designed together
 * (a TV wall with sofa and rug, a kitchen run, a bed with nightstands),
 * placed as one unit against a room wall. Loaded from JSON
 * (docs/furniture_sets.md describes the format, user editable in
 * config/decimation_worldgen/sets/).
 *
 * Grid: layers[y][r][c]; y 0 stands on the floor (within 1), 1 one block
 * up, 2 under the ceiling; r = depth from the wall the set stands against
 * (r 0 touches it), c = along that wall. ' ' = nothing / don't care, '.' =
 * must stay free (walking space), other chars = palette entries.
 */
public final class FurnitureSet
{
    public final String name;
    public final List<String> rooms;
    public final int weight;
    /** layers[y][r] = row string (length = width). */
    public final char[][][] layers;
    public final Map<Character, Entry> palette;
    public final int depth, width;
    /** Where the set may be used (building kind, ground / upper storey, floors). */
    public final net.decimation.worldgen.assets.Condition when;
    /** Named palette and style (weighted palettes) under the inline one; null = none. */
    public final String paletteRef, style;

    public FurnitureSet(String name, List<String> rooms, int weight, char[][][] layers, Map<Character, Entry> palette,
                        net.decimation.worldgen.assets.Condition when, String paletteRef, String style)
    {
        this.name = name;
        this.rooms = rooms;
        this.weight = weight;
        this.layers = layers;
        this.palette = palette;
        this.when = when;
        this.paletteRef = paletteRef;
        this.style = style;
        int d = 0, w = 0;
        for (char[][] layer : layers)
        {
            d = Math.max(d, layer.length);
            for (char[] row : layer)
            {
                w = Math.max(w, row.length);
            }
        }
        this.depth = d;
        this.width = w;
    }

    /** Char at (y, r, c), ' ' outside the grid. */
    public char at(int y, int r, int c)
    {
        if (y >= layers.length || r >= layers[y].length || c >= layers[y][r].length)
        {
            return ' ';
        }
        return layers[y][r][c];
    }

    /**
     * Palette entry of a char for one placement: the inline palette wins,
     * then the named palette, then the palette the style picks for u
     * (u is fixed per placement, so a whole set uses one style palette).
     */
    public Entry entry(char ch, double u)
    {
        Entry e = palette.get(ch);
        if (e != null)
        {
            return e;
        }
        Map<Character, Entry> named = net.decimation.worldgen.assets.Palettes.palette(paletteRef);
        if (named != null && named.containsKey(ch))
        {
            return named.get(ch);
        }
        Map<Character, Entry> styled = net.decimation.worldgen.assets.Palettes.fromStyle(style, u);
        return styled != null ? styled.get(ch) : null;
    }

    /** One palette letter. */
    public static final class Entry
    {
        /** Candidate blocks (one picked per placement by the seed); null entries were not found. */
        public final List<Block> blocks = new ArrayList<Block>();
        /** How metadata is derived: prop, vanilla (chest / furnace), seat (stairs), bed, door, meta. */
        public final String type;
        /** Direction relative to the wall: out, in, left, right (null = none). */
        public final String face;
        /** Fixed metadata for type "meta"; bed part ("head" / "foot") for type "bed". */
        public final int meta;
        public final String part;

        public Entry(String type, String face, int meta, String part)
        {
            this.type = type;
            this.face = face;
            this.meta = meta;
            this.part = part;
        }
    }
}
