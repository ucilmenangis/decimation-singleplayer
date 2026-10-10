package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Military wrecks broke with one punch (user report 10 Oktober 2026, bug.md): Decimation's
 * BlockWreckageMilitary1 / 2 / 3 (jeep, APC, helicopter) are BlockContainers whose constructors set
 * no hardness, so it stays 0. Its other wrecks are BlockProps, which set hardness 10. Here the
 * three get the same 10 (resistance follows: 50) once Decimation has registered them.
 */
public final class WreckHardness
{
    private WreckHardness()
    {
    }

    public static void apply()
    {
        StringBuilder done = new StringBuilder();
        for (String name : new String[] {"BlockWreckageMilitary1", "BlockWreckageMilitary2", "BlockWreckageMilitary3"})
        {
            Block b = Block.getBlockFromName("deci:" + name);
            if (b != null && b != Blocks.air && b.getBlockHardness(null, 0, 0, 0) < 10f)
            {
                b.setHardness(10f);
                done.append(' ').append(name);
            }
        }
        FMLLog.info("[deciworldgen] wreck hardness 10:%s", done.length() == 0 ? " none needed" : done.toString());
    }
}
