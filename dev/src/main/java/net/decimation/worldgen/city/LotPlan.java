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

/** An empty lot (no building fits or a gap): grass at the ground. */
final class LotPlan implements net.decimation.worldgen.FixedBase
{
    private final String id;
    private final int minX, minZ, ground;
    /** Park part on the grass (layer 1 up), or null for a plain lot. */
    private final LcContent.Shape park;

    LotPlan(String id, int minX, int minZ, int ground, LcContent.Shape park)
    {
        this.park = park;
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.ground = ground;
    }

    public int fixedBaseY() { return ground; }
    public String id() { return id; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + 15; }
    public int maxZ() { return minZ + 15; }
    public int height() { return park != null ? park.height + 1 : 2; }
    public int clearAbove() { return 14; }
    public int maxSpread() { return 255; }
    public Block foundation() { return Blocks.dirt; }
    public net.decimation.worldgen.ZoneKind zone() { return null; }
    public String describe() { return park != null ? "lc park" : null; }

    public Block blockAt(int lx, int ly, int lz, int[] meta)
    {
        meta[0] = 0;
        if (ly == 0)
        {
            return Blocks.grass;
        }
        if (park != null)
        {
            return partAt(park, 0, lx, ly - 1, lz, meta);
        }
        if (ly == 1 && ((lx * 7 + lz * 13 + minX + minZ) & 15) == 0)
        {
            return Blocks.deadbush;
        }
        return null;
    }
}
