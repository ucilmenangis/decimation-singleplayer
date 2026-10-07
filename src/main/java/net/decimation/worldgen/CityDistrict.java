package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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
    /** Ground height spread a building's first slice tolerates. */
    private static final int MAX_SPREAD = 8;
    private static final int MAX_FOUNDATION = 12;

    private final Building.Props props;

    public CityDistrict(Building.Props props)
    {
        this.props = props;
    }

    /** All buildings of one cell, from the world seed only (no world access). */
    public List<Building> plan(long worldSeed, int cellX, int cellZ)
    {
        List<Building> out = new ArrayList<Building>();
        Random r = new Random(worldSeed ^ (cellX * 7123479127L + cellZ * 912374191L) ^ 0x5EED_C17EL);
        int baseX = cellX * CELL_BLOCKS, baseZ = cellZ * CELL_BLOCKS;
        for (int lot = 0; lot < 4; lot++)
        {
            int lotX = baseX + LOT_OFFSETS[lot & 1];
            int lotZ = baseZ + LOT_OFFSETS[lot >> 1];
            float roll = r.nextFloat();
            int kind, w, l, floors;
            if (roll < 0.12f)
            {
                r.nextInt(); // keep the stream aligned whatever the roll
                continue; // empty lot
            }
            if (roll < 0.55f)
            {
                kind = Building.APARTMENT;
                w = 13 + r.nextInt(12);
                l = 13 + r.nextInt(12);
                floors = 2 + r.nextInt(4);
            }
            else if (roll < 0.80f)
            {
                kind = Building.OFFICE;
                w = 15 + r.nextInt(10);
                l = 15 + r.nextInt(10);
                floors = 3 + r.nextInt(4);
            }
            else
            {
                kind = Building.SHOP;
                w = 9 + r.nextInt(10);
                l = 9 + r.nextInt(12);
                floors = 1 + r.nextInt(2);
            }
            w = Math.min(w, LOT_SIZE);
            l = Math.min(l, LOT_SIZE);
            // left lots face the cell's own west street, right lots the next cell's
            int front = (lot & 1) == 0 ? Building.FRONT_WEST : Building.FRONT_EAST;
            int x = front == Building.FRONT_WEST ? lotX : lotX + LOT_SIZE - w;
            int z = lotZ + r.nextInt(LOT_SIZE - l + 1);
            long seed = r.nextLong();
            out.add(new Building("b" + cellX + "_" + cellZ + "_" + lot, x, z, w, l, floors,
                                 kind, front, seed, props));
        }
        return out;
    }

    /** Write every building slice inside this chunk's population window. */
    public void populate(World world, int chunkX, int chunkZ, StructureGenerator gen)
    {
        int wx0 = (chunkX << 4) + 8, wz0 = (chunkZ << 4) + 8;
        int wx1 = wx0 + 15, wz1 = wz0 + 15;
        StructureData data = StructureData.get(world);
        int cx0 = Math.floorDiv(wx0, CELL_BLOCKS), cx1 = Math.floorDiv(wx1, CELL_BLOCKS);
        int cz0 = Math.floorDiv(wz0, CELL_BLOCKS), cz1 = Math.floorDiv(wz1, CELL_BLOCKS);
        for (int cx = cx0; cx <= cx1; cx++)
        {
            for (int cz = cz0; cz <= cz1; cz++)
            {
                // a cell lies in exactly one sector; ask with any of its chunks
                if (gen.sectorOf(world, cx * 4, cz * 4) != StructureGenerator.CITY)
                {
                    continue;
                }
                for (Building b : plan(world.getSeed(), cx, cz))
                {
                    int x0 = Math.max(wx0, b.minX), x1 = Math.min(wx1, b.maxX());
                    int z0 = Math.max(wz0, b.minZ), z1 = Math.min(wz1, b.maxZ());
                    if (x0 > x1 || z0 > z1)
                    {
                        continue;
                    }
                    Integer baseY = data.baseY(b.id);
                    if (baseY == null)
                    {
                        baseY = decideBase(world, x0, z0, x1, z1);
                        data.setBaseY(b.id, baseY);
                        if (baseY != StructureData.CANCELLED)
                        {
                            ZoneStore.add(net.decimation.mod.server.zones.a.POLICE,
                                          b.minX - 6, baseY - 4, b.minZ - 6,
                                          b.maxX() + 6, baseY + b.height() + 12, b.maxZ() + 6);
                            cpw.mods.fml.common.FMLLog.info("[%s] city %s %s %dx%d, %d floor(s) at %d,%d,%d",
                                DecimationWorldGen.MODID, Building.KIND_NAME[b.kind], b.id,
                                b.width, b.length, b.floors, b.minX, baseY, b.minZ);
                        }
                    }
                    if (baseY == StructureData.CANCELLED)
                    {
                        continue;
                    }
                    writeSlice(world, b, baseY, x0, z0, x1, z1);
                }
            }
        }
    }

    /** Floor height from the terrain the first slice can see, or CANCELLED. */
    private int decideBase(World world, int x0, int z0, int x1, int z1)
    {
        int[] ys = new int[9];
        int n = 0;
        for (int i = 0; i < 3; i++)
        {
            for (int k = 0; k < 3; k++)
            {
                int x = x0 + (x1 - x0) * i / 2, z = z0 + (z1 - z0) * k / 2;
                int y = StructureGenerator.soilTop(world, x, z);
                if (y < 5 || StructureGenerator.waterAbove(world, x, y, z))
                {
                    return StructureData.CANCELLED;
                }
                ys[n++] = y;
            }
        }
        java.util.Arrays.sort(ys);
        if (ys[8] - ys[0] > MAX_SPREAD)
        {
            return StructureData.CANCELLED;
        }
        return ys[4]; // median: the ground floor replaces the median surface block
    }

    private void writeSlice(World world, Building b, int baseY, int x0, int z0, int x1, int z1)
    {
        int[] meta = new int[1];
        int top = baseY + b.height();
        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                int lx = x - b.minX, lz = z - b.minZ;
                // foundation down to the ground
                for (int y = baseY - 1; y > baseY - 1 - MAX_FOUNDATION && y > 0; y--)
                {
                    Block below = world.getBlock(x, y, z);
                    if (!below.getMaterial().isReplaceable() && !below.getMaterial().isLiquid()
                        && below != Blocks.leaves && below != Blocks.leaves2)
                    {
                        break;
                    }
                    world.setBlock(x, y, z, Blocks.stonebrick, 0, 2);
                }
                for (int y = baseY; y <= top + 6; y++)
                {
                    Block block = y <= top ? b.blockAt(lx, y - baseY, lz, meta) : null;
                    if (block == null)
                    {
                        if (!world.isAirBlock(x, y, z))
                        {
                            world.setBlock(x, y, z, Blocks.air, 0, 2);
                        }
                    }
                    else
                    {
                        world.setBlock(x, y, z, block, meta[0], 2);
                    }
                }
            }
        }
    }
}
