package net.decimation.worldgen.military;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;

/**
 * Where every Decimation prop is DRAWN, per metadata 2..5, relative to its block corner
 * ({x0, x1, y0, y1, z0, z1}; 0..1 = inside its own cell). Computed from Decimation's renderers
 * and models by tools/props/propgeom.py into assets/deciworldgen/prop_boxes.tsv; checked against
 * top down photos (204 of 221 props within 0.2 blocks, the rest are thin parts photos miss).
 * docs/prop_placement.md explains the numbers and the placement rules.
 */
final class PropBoxes
{
    private static Map<String, float[][]> boxes;
    private static Map<String, int[]> turns;

    /** The drawn box of a prop block at a metadata (2..5), or null when it is not a known prop. */
    static float[] box(Block b, int meta)
    {
        if (boxes == null)
        {
            load();
        }
        String name = Block.blockRegistry.getNameForObject(b);
        if (name == null || !name.startsWith("deci:"))
        {
            return null;
        }
        float[][] all = boxes.get(name.substring(5));
        return all == null ? null : all[meta >= 2 && meta <= 5 ? meta - 2 : 1];
    }

    /**
     * The metadata that draws this prop turned a quarter clockwise (north -> east). Plain props
     * step +1 (2 -> 3 -> 4 -> 5 -> 2); renderers with their own tables do not (military wrecks
     * 2 -> 5 -> 3 -> 4 -> 2, the storage crate, hazard screens).
     */
    static int turn(Block b, int meta)
    {
        if (boxes == null)
        {
            load();
        }
        String name = Block.blockRegistry.getNameForObject(b);
        int[] t = name == null || !name.startsWith("deci:") ? null : turns.get(name.substring(5));
        if (meta < 2 || meta > 5)
        {
            return meta;
        }
        return t != null && t[meta - 2] != 0 ? t[meta - 2] : 2 + ((meta - 2 + 1) & 3);
    }

    private static synchronized void load()
    {
        Map<String, float[][]> m = new HashMap<String, float[][]>();
        Map<String, int[]> tm = new HashMap<String, int[]>();
        InputStream in = PropBoxes.class.getResourceAsStream("/assets/deciworldgen/prop_boxes.tsv");
        if (in != null)
        {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8")))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    if (line.startsWith("#") || line.trim().isEmpty())
                    {
                        continue;
                    }
                    String[] f = line.trim().split("\\s+");
                    float[][] all = m.get(f[0]);
                    if (all == null)
                    {
                        all = new float[4][];
                        m.put(f[0], all);
                    }
                    float[] v = new float[6];
                    for (int i = 0; i < 6; i++)
                    {
                        v[i] = Float.parseFloat(f[2 + i]);
                    }
                    all[Integer.parseInt(f[1]) - 2] = v;
                    if (f.length > 8)
                    {
                        int[] t = tm.get(f[0]);
                        if (t == null)
                        {
                            t = new int[4];
                            tm.put(f[0], t);
                        }
                        t[Integer.parseInt(f[1]) - 2] = Integer.parseInt(f[8]);
                    }
                }
            }
            catch (Exception e)
            {
                // an empty table: no prop is checked
            }
        }
        turns = tm;
        boxes = m;
    }
}
