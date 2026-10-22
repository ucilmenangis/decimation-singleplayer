package net.decimation.worldgen;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Per-world memory for large structures that are written slice by slice.
 *
 * A large structure spans several chunks and every chunk writes only its own
 * slice when it populates, possibly minutes apart. All slices must agree on
 * the floor height, so the first slice decides it from the terrain it can see
 * and stores it here; later slices read it back. A stored CANCELLED marks a
 * structure rejected by its first slice (water, steep or broken terrain), so
 * the other slices skip it too. Saved in the world's data folder.
 */
public class StructureData extends WorldSavedData
{
    public static final String NAME = "deciworldgen_structures";
    public static final int CANCELLED = Integer.MIN_VALUE;

    private final Map<String, Integer> baseY = new HashMap<String, Integer>();
    /** Named places of this world (military bases): id -> turf entry "x1/y1/z1=x2/y2/z2=owner=name". */
    private final Map<String, String> places = new HashMap<String, String>();
    /** Int lists by id (bunker garrisons: city/Garrisons). */
    private final Map<String, int[]> lists = new HashMap<String, int[]>();

    public StructureData(String name)
    {
        super(name);
    }

    public static StructureData get(World world)
    {
        StructureData data = (StructureData) world.mapStorage.loadData(StructureData.class, NAME);
        if (data == null)
        {
            data = new StructureData(NAME);
            world.mapStorage.setData(NAME, data);
        }
        return data;
    }

    /** Stored floor height, CANCELLED, or null when no slice was placed yet. */
    public Integer baseY(String id)
    {
        return baseY.get(id);
    }

    public void setBaseY(String id, int y)
    {
        baseY.put(id, y);
        markDirty();
    }

    public void setPlace(String id, String turf)
    {
        places.put(id, turf);
        markDirty();
    }

    public java.util.Collection<String> places()
    {
        return places.values();
    }

    /** Takes a named place out (dev tests); true when it was there. */
    public boolean removePlace(String id)
    {
        boolean had = places.remove(id) != null;
        if (had)
        {
            markDirty();
        }
        return had;
    }

    public int[] list(String id)
    {
        return lists.get(id);
    }

    public void setList(String id, int[] v)
    {
        lists.put(id, v);
        markDirty();
    }

    /** Ids of the int lists starting with prefix. */
    public java.util.List<String> lists(String prefix)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String k : lists.keySet())
        {
            if (k.startsWith(prefix))
            {
                out.add(k);
            }
        }
        return out;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        places.clear();
        NBTTagCompound pl = tag.getCompoundTag("places");
        for (Object key : pl.func_150296_c())
        {
            places.put((String) key, pl.getString((String) key));
        }
        lists.clear();
        NBTTagCompound li = tag.getCompoundTag("lists");
        for (Object key : li.func_150296_c())
        {
            lists.put((String) key, li.getIntArray((String) key));
        }
        baseY.clear();
        NBTTagCompound map = tag.getCompoundTag("baseY");
        for (Object key : map.func_150296_c()) // getKeySet
        {
            baseY.put((String) key, map.getInteger((String) key));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        NBTTagCompound map = new NBTTagCompound();
        for (Map.Entry<String, Integer> e : baseY.entrySet())
        {
            map.setInteger(e.getKey(), e.getValue());
        }
        tag.setTag("baseY", map);
        NBTTagCompound pl = new NBTTagCompound();
        for (Map.Entry<String, String> e : places.entrySet())
        {
            pl.setString(e.getKey(), e.getValue());
        }
        tag.setTag("places", pl);
        NBTTagCompound li = new NBTTagCompound();
        for (Map.Entry<String, int[]> e : lists.entrySet())
        {
            li.setIntArray(e.getKey(), e.getValue());
        }
        tag.setTag("lists", li);
    }
}
