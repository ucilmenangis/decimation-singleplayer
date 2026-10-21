package net.decimation.fixes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Some props still broke with one punch (user report 22 Oktober 2026, bug.md): Decimation registers a few blocks
 * without a hardness, so it stays 0 and one click of a survival hand breaks them. The dev test `hardness` found eight:
 * the multiblock props BlockStreetBarrier, BlockBarrierTall, BlockMetalShelf and BlockMetalShelf_Empty (BlockContainers
 * like the military wrecks of WreckHardness), the elevator platform and its button, and the two rope blocks.
 *
 * Every block of Decimation and ours with hardness 0 gets BlockProp's 10 here (resistance follows: 50), once all are
 * registered, unless MEANT_TO_BREAK names it: the ropes are a player's climbing tool that a click takes down again,
 * like vanilla string (the user can say otherwise). Blocks that already have a hardness keep it.
 */
public final class PropHardness
{
    public static final float HARDNESS = 10f;

    /** Blocks that keep breaking with one click on purpose (registry names). */
    private static final Set<String> MEANT_TO_BREAK = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "deci:BlockRope", "deci:BlockRopeTop")));

    private PropHardness()
    {
    }

    public static boolean meantToBreak(String key)
    {
        return MEANT_TO_BREAK.contains(key);
    }

    /** True when a block of this name and hardness gets HARDNESS. */
    public static boolean needs(String key, float hardness)
    {
        return (key.startsWith("deci:") || key.startsWith("deciworldgen:")) && hardness == 0f && !meantToBreak(key);
    }

    public static void apply()
    {
        StringBuilder done = new StringBuilder();
        for (Object o : Block.blockRegistry.getKeys())
        {
            String key = (String) o;
            Block b = (Block) Block.blockRegistry.getObject(key);
            if (b == null || b == Blocks.air)
            {
                continue;
            }
            float h;
            try
            {
                h = b.getBlockHardness(null, 0, 0, 0);
            }
            catch (RuntimeException e)
            {
                continue; // a hardness that reads the world: not one of Decimation's plain zeros
            }
            if (needs(key, h))
            {
                b.setHardness(HARDNESS);
                done.append(' ').append(key.substring(key.indexOf(':') + 1));
            }
        }
        FMLLog.info("[deciworldgen] prop hardness %s:%s", HARDNESS, done.length() == 0 ? " none needed" : done.toString());
    }
}
