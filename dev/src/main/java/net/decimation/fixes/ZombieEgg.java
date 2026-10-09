package net.decimation.fixes;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * One spawn egg per zombie variant (InfectedVariants; metadata = index in
 * its list): a plain infected with that variant. Vanilla spawn egg look,
 * zombie green with spots per variant. Creative tab Misc.
 */
public class ZombieEgg extends Item
{
    private final InfectedVariants variants;
    @SideOnly(Side.CLIENT)
    private IIcon overlay;

    public ZombieEgg(InfectedVariants variants)
    {
        this.variants = variants;
        setHasSubtypes(true);
        setUnlocalizedName("deciworldgen.zombieEgg");
        setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        InfectedVariants.Variant v = variants.byIndex(stack.getItemDamage());
        return v == null ? "Spawn Infected" : "Spawn Infected (" + v.name + ")";
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                             float hx, float hy, float hz)
    {
        if (world.isRemote)
        {
            return true;
        }
        InfectedVariants.Variant v = variants.byIndex(stack.getItemDamage());
        if (v == null)
        {
            return false;
        }
        x += Facing.offsetsXForSide[side];
        y += Facing.offsetsYForSide[side];
        z += Facing.offsetsZForSide[side];
        EntityLiving infected = Deci.newInfected(world);
        variants.apply(infected, v);
        infected.setLocationAndAngles(x + 0.5, y, z + 0.5, player.rotationYaw + 180, 0);
        world.spawnEntityInWorld(infected);
        if (!player.capabilities.isCreativeMode)
        {
            stack.stackSize--;
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubItems(Item item, CreativeTabs tab, List list)
    {
        for (int i = 0; variants.byIndex(i) != null; i++)
        {
            list.add(new ItemStack(item, 1, i));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg)
    {
        itemIcon = reg.registerIcon("spawn_egg");
        overlay = reg.registerIcon("spawn_egg_overlay");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses()
    {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int meta, int pass)
    {
        return pass > 0 ? overlay : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass)
    {
        InfectedVariants.Variant v = variants.byIndex(stack.getItemDamage());
        if (pass == 0 || v == null)
        {
            return 0x3E5B3A; // zombie green
        }
        return v.name.equals("runner") ? 0xD8C04A : v.name.equals("riot") ? 0x1F2F5A : v.name.equals("screamer")
            ? 0xE0E0E0 : 0x8B0000;
    }
}
