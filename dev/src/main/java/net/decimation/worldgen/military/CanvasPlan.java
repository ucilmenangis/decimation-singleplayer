package net.decimation.worldgen.military;

import java.util.LinkedHashMap;
import java.util.Map;

import net.decimation.worldgen.Graded;
import net.decimation.worldgen.Rotation;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * A compound built on a {@link Canvas} (military base, survivor camp) as a slice placed plan: the
 * canvas turned by 0 / 90 / 180 / 270 degrees clockwise, the ground around it ramped from the
 * levelled pad to the natural height over LOT blocks (Graded). Subclasses hand over the canvas
 * (built once and cached: plans are asked for by every chunk around them) and its camera points.
 */
public abstract class CanvasPlan implements Graded
{
    static final int LOT = 10;

    protected final String id;
    protected final int minX, minZ, turns;

    protected CanvasPlan(String id, int minX, int minZ, int turns)
    {
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.turns = turns & 3;
    }

    abstract Canvas canvas();

    /** Clash / refusal log of the generator (dev tests); empty when the generator keeps none. */
    public java.util.List<String> debugLog()
    {
        return new java.util.ArrayList<String>();
    }

    /** Registers the zone and residents of a plan placed by hand (dev tests, commands); none by default. */
    public void register(int baseY)
    {
    }

    /** Canvas size before turning, known without building it (extents are asked for often). */
    abstract int canvasWidth();

    abstract int canvasLength();

    abstract int canvasHeight();

    /** Camera points in canvas coordinates: name -> {x, y, z, yaw, pitch}. */
    abstract Map<String, float[]> canvasPoints();

    /** A canvas point (x, z) in world coordinates, turned with the plan. */
    protected double[] world(double x, double z)
    {
        int w = canvasWidth(), l = canvasLength();
        double rx, rz;
        switch (turns)
        {
            case 1:  rx = l - z; rz = x; break;
            case 2:  rx = w - x; rz = l - z; break;
            case 3:  rx = z; rz = w - x; break;
            default: rx = x; rz = z; break;
        }
        return new double[] {minX + rx, minZ + rz};
    }

    /**
     * Camera points (dev test): name -> {world x, y above the floor, world z, yaw, pitch}, turned
     * with the plan.
     */
    public Map<String, float[]> pointsOfInterest()
    {
        Map<String, float[]> out = new LinkedHashMap<String, float[]>();
        for (Map.Entry<String, float[]> e : canvasPoints().entrySet())
        {
            float[] p = e.getValue();
            double[] w = world(p[0], p[2]);
            out.put(e.getKey(), new float[] {(float) w[0], p[1], (float) w[1], p[3] + 90 * turns, p[4]});
        }
        return out;
    }

    private boolean swapped()
    {
        return (turns & 1) != 0;
    }

    public String id() { return id; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + (swapped() ? canvasLength() : canvasWidth()) - 1; }
    public int maxZ() { return minZ + (swapped() ? canvasWidth() : canvasLength()) - 1; }
    public int height() { return canvasHeight() - 1; }
    public int clearAbove() { return 10; }
    public Block foundation() { return Blocks.dirt; }
    public int lotMinX() { return minX - LOT; }
    public int lotMinZ() { return minZ - LOT; }
    public int lotMaxX() { return maxX() + LOT; }
    public int lotMaxZ() { return maxZ() + LOT; }

    public Block blockAt(int rx, int ly, int rz, int[] meta)
    {
        meta[0] = 0;
        Canvas c = canvas();
        // world footprint position -> canvas position (inverse of a clockwise turn)
        int x, z;
        switch (turns)
        {
            case 1:  x = rz; z = c.l - 1 - rx; break;
            case 2:  x = c.w - 1 - rx; z = c.l - 1 - rz; break;
            case 3:  x = c.w - 1 - rz; z = rx; break;
            default: x = rx; z = rz; break;
        }
        if (!c.inside(x, ly, z))
        {
            return null;
        }
        Block b = c.get(x, ly, z);
        if (b == null)
        {
            return null;
        }
        int m = c.meta(x, ly, z);
        switch (c.kind(x, ly, z))
        {
            case Canvas.VANILLA:
                m = Rotation.rotateMeta(Block.getIdFromBlock(b), m, turns);
                break;
            case Canvas.DOOR:
                m = Rotation.rotateMeta(64, m, turns);       // Decimation doors use vanilla door metadata
                break;
            case Canvas.PROP:
                // a quarter turn clockwise per turn; per prop, since some renderers have their
                // own metadata tables (military wrecks: 2 -> 5 -> 3 -> 4), docs/prop_placement.md
                for (int t = 0; t < turns; t++)
                {
                    m = PropBoxes.turn(b, m);
                }
                break;
            default:
                break;
        }
        meta[0] = m;
        return net.decimation.fixes.LootBlocks.forPlace(b, lootPlace());
    }

    /** The loot kind of this plan's shelves and cabinets (fixes/LootBlocks), -1 for none: they stay as placed. */
    int lootPlace()
    {
        return -1;
    }

    /** Ramp from the pad (floor height at the footprint) to the natural ground at the lot edge. */
    public void grade(World world, int x, int z, int baseY)
    {
        int x1 = maxX(), z1 = maxZ();
        if (x >= minX && x <= x1 && z >= minZ && z <= z1)
        {
            return;
        }
        int natural = StructureGenerator.soilTop(world, x, z);
        if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z))
        {
            return;
        }
        int d = Math.max(Math.max(minX - x, x - x1), Math.max(minZ - z, z - z1));   // 1 next to it
        double t = Math.min(1.0, (d - 1) / (double) LOT);
        t = t * t * (3 - 2 * t);
        int target = baseY + (int) Math.round((natural - baseY) * t);
        if (target == natural)
        {
            return;
        }
        Block surface = world.getBlock(x, natural, z);
        int surfaceMeta = world.getBlockMetadata(x, natural, z);
        if (surface == Blocks.air || surface.getMaterial().isLiquid())
        {
            return;
        }
        Block filler = surface == Blocks.grass || surface == Blocks.mycelium ? Blocks.dirt
            : surface instanceof net.minecraft.block.BlockFalling
                ? (surface == Blocks.sand ? Blocks.sandstone : Blocks.stone)
            : surface;
        int fillerMeta = filler == surface ? surfaceMeta : 0;
        for (int y = natural; y < target; y++)
        {
            world.setBlock(x, y, z, filler, fillerMeta, 2);
        }
        for (int y = target + 1; y <= natural; y++)
        {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
        }
        if (surface instanceof net.minecraft.block.BlockFalling)
        {
            surface = filler;
            surfaceMeta = fillerMeta;
        }
        world.setBlock(x, target, z, surface, surfaceMeta, 2);
        if (target < natural)
        {
            for (int y = target + 1, n = 0; n < 12; y++, n++)
            {
                Block above = world.getBlock(x, y, z);
                if (above == Blocks.air || !StructureGenerator.clearable(above))
                {
                    break;
                }
                world.setBlock(x, y, z, Blocks.air, 0, 2);
            }
        }
    }
}
