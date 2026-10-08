package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.decimation.worldgen.Plan;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.Slices;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import static net.decimation.worldgen.city.LcCity.*;

/** A converted building: its ground floor layer at the city ground. */
final class BuildingPlan implements net.decimation.worldgen.FixedBase
{
    private final String id;
    private final LcContent.Building b;
    private final LcContent.Shape s;
    private final int minX, minZ, baseY;
    private final net.decimation.worldgen.ZoneKind zone;
    /** District style and street level of the block (fronts of the street beside it). */
    final String style;
    final int ground;

    BuildingPlan(String id, LcContent.Building b, LcContent.Shape s, int minX, int minZ, int baseY,
                 net.decimation.worldgen.ZoneKind zone, String style, int ground)
    {
        this.style = style;
        this.ground = ground;
        this.id = id;
        this.b = b;
        this.s = s;
        this.minX = minX;
        this.minZ = minZ;
        this.baseY = baseY;
        this.zone = zone;
    }

    public int fixedBaseY() { return baseY; }
    public String id() { return id; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + s.width - 1; }
    public int maxZ() { return minZ + s.length - 1; }
    public int height() { return s.height - 1; }
    public int clearAbove() { return 12; }
    public int maxSpread() { return 255; }
    public Block foundation() { return Blocks.stone; }
    public net.decimation.worldgen.ZoneKind zone() { return zone; }

    public String describe()
    {
        return "lc " + b.pack + "/" + b.name + " (" + b.cx + "x" + b.cz + " chunks, " + s.height + " high, cellars "
            + b.groundY + ")";
    }

    public Block blockAt(int lx, int ly, int lz, int[] meta)
    {
        meta[0] = 0;
        if (lx < 0 || lz < 0 || lx >= s.width || lz >= s.length || ly < 0 || ly >= s.height)
        {
            meta[0] = SKIP;
            return null;
        }
        int i = s.index(lx, ly, lz);
        if (s.skip[i])
        {
            meta[0] = SKIP;
            return null;
        }
        meta[0] = s.meta[i];
        return s.blocks[i];
    }
}
