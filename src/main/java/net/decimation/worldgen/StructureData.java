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

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
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
    }
}
