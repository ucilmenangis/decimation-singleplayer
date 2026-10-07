package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.World;

/**
 * Procedural city blocks for CITY sectors.
 *
 * The street grid (StructureGenerator.paintStreets) runs along every cell's
 * west and north edge, so each cell's interior (x/z offsets 5..63) is one
 * city block. Inside it, a 2 block sidewalk ring, then 4 lots of 26x26
 * ({@link #LOT_OFFSETS}). Each lot gets, deterministically from the world
 * seed: nothing (rubble yard), an apartment block, an office or a shop, sized
 * and placed flush to the street on its side, door facing that street.
 *
 * Placement is slice by slice: {@link #populate} is called for every chunk's
 * population window (the 16x16 area at +8/+8, which tiles the world) and
 * writes only the columns of each building that fall inside it. The first
 * slice of a building fixes its floor height in {@link StructureData}.
 */
public class CityDistrict
{
    private static final int CELL_BLOCKS = 64;
    private static final int[] LOT_OFFSETS = {7, 36};
    private static final int LOT_SIZE = 26;

    private final Building.Props props;

    public CityDistrict(Building.Props props)
    {
        this.props = props;
    }

    /**
     * All buildings of one cell. Deterministic: world seed, cell coords and
     * the biome generator (which answers for chunks not generated yet).
     */
    public List<Building> plan(World world, int cellX, int cellZ)
    {
        List<Building> out = new ArrayList<Building>();
        Random r = new Random(world.getSeed() ^ (cellX * 7123479127L + cellZ * 912374191L) ^ 0x5EEDC17EL);
        int baseX = cellX * CELL_BLOCKS, baseZ = cellZ * CELL_BLOCKS;
        int style = style(world, baseX + CELL_BLOCKS / 2, baseZ + CELL_BLOCKS / 2);
        for (int lot = 0; lot < 4; lot++)
        {
            int lotX = baseX + LOT_OFFSETS[lot & 1];
            int lotZ = baseZ + LOT_OFFSETS[lot >> 1];
            float roll = r.nextFloat();
            float sizeRoll = r.nextFloat();
            int kind, w, l, floors;
            if (roll < 0.10f)
            {
                r.nextInt(); // keep the stream aligned whatever the roll
                continue; // empty lot
            }
            if (roll < 0.52f)
            {
                kind = Building.APARTMENT;
                w = 11 + r.nextInt(14);
                l = 11 + r.nextInt(16);
                floors = sizeRoll < 0.75f ? 2 + r.nextInt(4) : 6 + r.nextInt(4);
            }
            else if (roll < 0.80f)
            {
                kind = Building.OFFICE;
                if (sizeRoll < 0.60f)
                {
                    floors = 3 + r.nextInt(4);
                }
                else if (sizeRoll < 0.85f)
                {
                    floors = 7 + r.nextInt(5);
                }
                else
                {
                    floors = 12 + r.nextInt(9); // towers, up to 20
                }
                w = 15 + r.nextInt(12);
                l = 15 + r.nextInt(12);
            }
            else
            {
                kind = Building.SHOP;
                w = 9 + r.nextInt(12);
                l = 9 + r.nextInt(14);
                floors = 1 + r.nextInt(2);
            }
            w = Math.min(w, LOT_SIZE);
            l = Math.min(l, LOT_SIZE);
            // left lots face the cell's own west street, right lots the next cell's
            int front = (lot & 1) == 0 ? Building.FRONT_WEST : Building.FRONT_EAST;
            // a 2 block side yard where the lot allows, so the ground can ramp
            int zSpace = LOT_SIZE - l, inset = Math.min(zSpace / 2, 2);
            int z = lotZ + inset + r.nextInt(zSpace - 2 * inset + 1);
            long seed = r.nextLong();
            // setback from the street: a front yard (parking, path) where the
            // lot has room, taken from the seed so the stream stays aligned;
            // at least 3 blocks stay behind it so the back can ramp too
            int avail = LOT_SIZE - w, setback = Math.min(avail / 2, 3);
            if (avail >= Building.MIN_YARD + 3 && ((seed >>> 3) & 3) != 0)
            {
                int most = Math.min(avail - 3, 9);
                setback = Building.MIN_YARD + (int) ((seed >>> 5) % (most - Building.MIN_YARD + 1));
            }
            int x = front == Building.FRONT_WEST ? lotX + setback : lotX + LOT_SIZE - w - setback;
            out.add(new Building("b" + cellX + "_" + cellZ + "_" + lot, x, z, w, l, floors,
                                 kind, front, style, seed, props, lotX, lotZ, LOT_SIZE));
        }
        return out;
    }

    /** Overgrowth style from the biome at a point. */
    static int style(World world, int x, int z)
    {
        net.minecraft.world.biome.BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        if (biome == null)
        {
            return Building.TEMPERATE;
        }
        if (net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.SNOWY)
            || biome.getEnableSnow())
        {
            return Building.COLD;
        }
        if (net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.SANDY)
            || net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.MESA)
            || net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.SAVANNA)
            || biome.rainfall < 0.15f)
        {
            return Building.DRY;
        }
        if (net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.JUNGLE)
            || net.minecraftforge.common.BiomeDictionary.isBiomeOfType(biome, net.minecraftforge.common.BiomeDictionary.Type.SWAMP))
        {
            return Building.LUSH;
        }
        return Building.TEMPERATE;
    }

    /** Write every building slice inside this chunk's population window. */
    public void populate(World world, int chunkX, int chunkZ, StructureGenerator gen)
    {
        int[] w = Slices.window(chunkX, chunkZ);
        int cx0 = Math.floorDiv(w[0], CELL_BLOCKS), cx1 = Math.floorDiv(w[2], CELL_BLOCKS);
        int cz0 = Math.floorDiv(w[1], CELL_BLOCKS), cz1 = Math.floorDiv(w[3], CELL_BLOCKS);
        for (int cx = cx0; cx <= cx1; cx++)
        {
            for (int cz = cz0; cz <= cz1; cz++)
            {
                // a cell lies in exactly one sector; ask with any of its chunks
                if (gen.sectorOf(world, cx * 4, cz * 4) != StructureGenerator.CITY)
                {
                    continue;
                }
                for (Building b : plan(world, cx, cz))
                {
                    Slices.place(world, b, w);
                }
            }
        }
    }
}
