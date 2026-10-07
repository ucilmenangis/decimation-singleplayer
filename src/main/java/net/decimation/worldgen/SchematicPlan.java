package net.decimation.worldgen;

import java.util.Map;

import net.minecraft.block.Block;

/**
 * A (large) schematic placed as a {@link Plan}: any size, written slice by
 * slice. Rotation, the SKIP marker (bedrock), unknown ids and the placeholder
 * to Decimation prop substitution behave as in StructureGenerator.place; road
 * facing props fall back to FIXED (a slice cannot look at neighbouring
 * chunks that may not exist yet).
 */
public class SchematicPlan implements Plan
{
    private static final int SKIP_ID = 7;

    private final String id;
    private final Schematic s;
    private final int minX, minZ, turns;
    private final Map<Integer, StructureGenerator.Sub> subs;
    private final net.decimation.mod.server.zones.a zone;

    public SchematicPlan(String id, Schematic s, int minX, int minZ, int turns,
                         Map<Integer, StructureGenerator.Sub> subs,
                         net.decimation.mod.server.zones.a zone)
    {
        this.id = id;
        this.s = s;
        this.minX = minX;
        this.minZ = minZ;
        this.turns = turns;
        this.subs = subs;
        this.zone = zone;
    }

    private boolean swapped()
    {
        return (turns & 1) != 0;
    }

    public String id() { return id; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + (swapped() ? s.length : s.width) - 1; }
    public int maxZ() { return minZ + (swapped() ? s.width : s.length) - 1; }
    public int height() { return s.height - 1; }
    public int clearAbove() { return 0; }
    public int maxSpread() { return 10; }
    public net.decimation.mod.server.zones.a zone() { return zone; }

    public String describe()
    {
        return "large '" + s.name + "' " + s.width + "x" + s.height + "x" + s.length + " rot " + turns * 90;
    }

    public Block blockAt(int rx, int ly, int rz, int[] meta)
    {
        meta[0] = 0;
        // world footprint position -> schematic position (inverse of the
        // clockwise rotation used by StructureGenerator.place)
        int x, z;
        switch (turns)
        {
            case 1:  x = rz; z = s.length - 1 - rx; break;
            case 2:  x = s.width - 1 - rx; z = s.length - 1 - rz; break;
            case 3:  x = s.width - 1 - rz; z = rx; break;
            default: x = rx; z = rz; break;
        }
        if (x < 0 || z < 0 || x >= s.width || z >= s.length || ly >= s.height)
        {
            meta[0] = SKIP;
            return null;
        }
        int i = s.index(x, ly, z);
        int id = s.blocks[i];
        int data = s.data[i] & 0xFF;
        if (id == SKIP_ID)
        {
            meta[0] = SKIP;
            return null;
        }
        if (id == 0)
        {
            return null;
        }
        StructureGenerator.Sub sub = subs.get((id << 4) | (data & 15));
        if (sub != null)
        {
            int pick = ((x * 31 + z) * 31 + ly) & 0x7fffffff;
            Block b = sub.targets[pick % sub.targets.length];
            switch (sub.mode)
            {
                case StructureGenerator.Sub.PLAIN:
                    meta[0] = 0;
                    break;
                case StructureGenerator.Sub.RANDOM:
                    meta[0] = 2 + (((pick >> 4) + turns) & 3);
                    break;
                default:
                    meta[0] = 2 + ((3 + turns) & 3);
            }
            return b;
        }
        Block b = Block.getBlockById(id);
        if (b == null)
        {
            meta[0] = SKIP;
            return null;
        }
        meta[0] = Rotation.rotateMeta(id, data, turns);
        return b;
    }
}
