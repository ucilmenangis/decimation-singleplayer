package net.decimation.worldgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A parsed MCEdit/WorldEdit {@code .schematic} file.
 *
 * Standard format: gzipped NBT compound with Width/Height/Length shorts, a
 * Blocks byte array (low 8 bits of each block id), an optional AddBlocks
 * nibble array (high 4 bits, needed for mod blocks with ids > 255 - all of
 * Decimation's own blocks land there), and a Data byte array (metadata).
 * Index layout is (y * Length + z) * Width + x.
 *
 * Caveat inherited from the format itself: block ids are raw numbers, and in
 * 1.7.10 FML assigns mod-block ids per save based on the mod list. Schematics
 * containing Decimation blocks must therefore be created in this same
 * instance (same mod list) to keep ids stable. Vanilla ids (0-197) are always
 * safe. TileEntities/Entities sections are ignored - Decimation's loot system
 * is position-based, so loot containers need no tile entity data.
 */
public final class Schematic
{
    public final String name;
    public final int width;   // x
    public final int height;  // y
    public final int length;  // z
    public final short[] blocks;
    public final byte[] data;

    private Schematic(String name, int width, int height, int length,
                      short[] blocks, byte[] data)
    {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = blocks;
        this.data = data;
    }

    public int index(int x, int y, int z)
    {
        return (y * length + z) * width + x;
    }

    public static Schematic load(File file) throws Exception
    {
        NBTTagCompound root;
        InputStream in = new FileInputStream(file);
        try
        {
            root = CompressedStreamTools.func_74796_a(in); // readCompressed
        }
        finally
        {
            in.close();
        }

        int width = root.func_74765_d("Width");   // getShort
        int height = root.func_74765_d("Height");
        int length = root.func_74765_d("Length");
        byte[] rawBlocks = root.func_74770_j("Blocks"); // getByteArray
        byte[] data = root.func_74770_j("Data");
        byte[] add = root.func_74764_b("AddBlocks") // hasKey
                   ? root.func_74770_j("AddBlocks") : null;

        int volume = width * height * length;
        if (volume <= 0 || rawBlocks.length != volume || data.length != volume)
        {
            throw new Exception("inconsistent dimensions " + width + "x" + height
                                + "x" + length + " vs " + rawBlocks.length
                                + " blocks / " + data.length + " data");
        }

        short[] blocks = new short[volume];
        for (int i = 0; i < volume; i++)
        {
            int id = rawBlocks[i] & 0xFF;
            if (add != null && (i >> 1) < add.length)
            {
                // WorldEdit nibble packing: even index = low nibble, odd = high
                int nibble = (i & 1) == 0 ? (add[i >> 1] & 0x0F)
                                          : ((add[i >> 1] & 0xF0) >> 4);
                id |= nibble << 8;
            }
            blocks[i] = (short) id;
        }

        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return new Schematic(dot > 0 ? name.substring(0, dot) : name,
                             width, height, length, blocks, data);
    }
}
