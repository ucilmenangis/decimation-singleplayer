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

    /**
     * A multiblock prop just placed by world generation: make it a whole
     * multiblock, as player placement does (BlockMetalShelf.onBlockPlacedBy
     * calls deci.W.e.a = MultiblockHelper.formMultiblock: every cell of the
     * size box gets the block, all pointing at the master). Only column
     * multiblocks (1x1 footprint: metal shelves, vending machines, phone
     * box, all 1x2x1) and only when the cells above are air; anything else
     * just becomes its own master. Must run after the generator has written
     * the cells above: overwriting a part with air breaks the WHOLE
     * multiblock (BlockMetalShelf.breakBlock removes every part).
     */
    public static void complete(net.minecraft.world.World world, int x, int y, int z)
    {
        TileEntity te = world.getTileEntity(x, y, z);
        int[] size = Deci.multiblockSize(te);
        if (!repair(te) || size == null)
        {
            return;
        }
        if (size[0] != 1 || size[2] != 1 || size[1] < 2)
        {
            return;
        }
        for (int i = 1; i < size[1]; i++)
        {
            if (!world.isAirBlock(x, y + i, z))
            {
                return;
            }
        }
        Deci.buildMultiblock(world, x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    /** Make an orphaned multiblock part its own master; true if repaired. */
    public static boolean repair(TileEntity te)
    {
        if (!Deci.isMultiblockPart(te) || Deci.isMultiblockMaster(te))
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
        Deci.setMultiblockSelfMaster(te);
        te.markDirty();
        return true;
    }
}
