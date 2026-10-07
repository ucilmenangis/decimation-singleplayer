package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.world.ChunkEvent;

/**
 * Invisible multiblock props (metal shelves etc).
 *
 * Multiblock props (tile entities extending {@code deci.W.a},
 * MultiblockPart) are drawn only by their master part: e.g. MetalShelfRenderer
 * returns early unless {@code isMaster()}, i.e. the stored master position
 * equals the part's own. The master is set by the block's placement code; a
 * block placed any other way (world generation, schematics, commands) keeps
 * the default master 0,0,0 and is never drawn. This repairs such parts when
 * their chunk loads: master never set -> the part becomes its own master.
 */
public class MultiblockRepairHandler
{
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
        for (Object o : event.getChunk().chunkTileEntityMap.values())
        {
            repair((TileEntity) o);
        }
    }

    /** Make an orphaned multiblock part its own master; true if repaired. */
    public static boolean repair(TileEntity te)
    {
        if (!(te instanceof deci.W.a) || ((deci.W.a) te).isMaster())
        {
            return false;
        }
        NBTTagCompound tag = new NBTTagCompound();
        te.writeToNBT(tag);
        boolean unset = tag.getInteger("multibl.mx") == 0 && tag.getInteger("multibl.my") == 0
            && tag.getInteger("multibl.mz") == 0;
        if (!unset)
        {
            return false; // a real part of a real multiblock: leave it
        }
        ((deci.W.a) te).setSelfMaster();
        te.markDirty();
        return true;
    }
}
