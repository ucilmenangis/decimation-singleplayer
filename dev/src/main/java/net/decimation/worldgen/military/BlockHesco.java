package net.decimation.worldgen.military;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

/**
 * HESCO barrier cell (deciworldgen:hesco): a plain full block with our own texture
 * (tools/props/hesco_textures.py). Replaces Decimation's BlockMilitaryBarrier in our bases: that
 * one is a prop, one tile entity and one model render per cell, and a base has thousands of
 * cells (the user measured the frame rate drop, 10 Oktober 2026).
 */
public final class BlockHesco extends Block
{
    public static BlockHesco instance;

    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockHesco()
    {
        super(Material.ground);
        setHardness(2.0F);
        setResistance(20.0F);
        setStepSound(soundTypeGravel);
        setBlockName("deciworldgen.hesco");
        setBlockTextureName("deciworldgen:hesco_side");
        setCreativeTab(CreativeTabs.tabBlock);
        setHarvestLevel("shovel", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg)
    {
        blockIcon = reg.registerIcon("deciworldgen:hesco_side");
        top = reg.registerIcon("deciworldgen:hesco_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta)
    {
        return side == 0 || side == 1 ? top : blockIcon;
    }
}
