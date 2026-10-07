package net.decimation.worldgen.terrain;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

/**
 * A dead, leafless tree: a 4 to 8 block trunk (oak or dark oak bark) with a
 * few short bare branches, sometimes snapped off low.
 */
public class DeadTree extends WorldGenAbstractTree
{
    public DeadTree()
    {
        super(false);
    }

    @Override
    public boolean generate(World world, Random rand, int x, int y, int z)
    {
        Block soil = world.getBlock(x, y - 1, z);
        if (soil != Blocks.grass && soil != Blocks.dirt)
        {
            return false;
        }
        int height = rand.nextInt(6) == 0 ? 2 + rand.nextInt(2) : 4 + rand.nextInt(5);
        int wood = rand.nextBoolean() ? 0 : 1; // oak / spruce bark
        for (int i = 0; i < height; i++)
        {
            if (!isReplaceable(world, x, y + i, z))
            {
                return i > 0;
            }
        }
        for (int i = 0; i < height; i++)
        {
            setBlockAndNotifyAdequately(world, x, y + i, z, Blocks.log, wood);
        }
        if (height < 4)
        {
            return true; // a snapped stump
        }
        int branches = 1 + rand.nextInt(3);
        for (int b = 0; b < branches; b++)
        {
            int by = y + height / 2 + rand.nextInt(height - height / 2);
            int dir = rand.nextInt(4);
            int dx = dir == 0 ? 1 : dir == 1 ? -1 : 0, dz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
            int len = 1 + rand.nextInt(2);
            // log axis metadata: 4 = along x, 8 = along z
            int axis = dx != 0 ? 4 : 8;
            for (int i = 1; i <= len; i++)
            {
                int bx = x + dx * i, bz = z + dz * i, yy = by + (i == len && rand.nextBoolean() ? 1 : 0);
                if (isReplaceable(world, bx, yy, bz))
                {
                    setBlockAndNotifyAdequately(world, bx, yy, bz, Blocks.log, wood | axis);
                }
            }
        }
        return true;
    }
}
