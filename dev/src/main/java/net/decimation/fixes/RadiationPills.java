package net.decimation.fixes;

import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/**
 * Radiation pills, deciworldgen:radiation_pills (user, 22 Oktober 2026, idea 41: a pill takes 40 percentage points off the
 * radiation dose at once, fixes/RadiationDose; found in medical loot). Used the way Decimation's own pills are (Pain
 * Killers HealingSyringeItem, Vitamin Capsules DrugSyringeItem; docs/decimation_code/items.md): 4 to a stack, a held right
 * click in the block pose for 32 ticks with the syringe "open" sound at most every 8 s (PlayerData's long action timer),
 * then on the server the effect, one pill gone and the syringe "use" sound, and on the client the green chat line.
 * Decimation's medical creative tab; in loot where Vitamin Capsules are (medical crates), with the same weight 0.9.
 * The icon is ours (tools/item_icons.py).
 */
public class RadiationPills extends Item
{
    /** Decimation's medical use time, stack size; the user's 40 points. */
    static final int USE_TICKS = 32, STACK = 4, POINTS = 40;
    public static Item instance;
    /** For the dev test: pills taken on the server. */
    static volatile int taken;

    public RadiationPills()
    {
        setMaxStackSize(STACK);
        setUnlocalizedName("deciworldgen.radiationPills");
        setTextureName("deciworldgen:radiation_pills");
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.block;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return USE_TICKS;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced)
    {
        list.add(EnumChatFormatting.RED + "Medical Item (For Radiation -" + POINTS + "%)");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (Deci.actionTimerElapsed(player))
        {
            world.playSoundAtEntity(player, "deci:item.syringe.open", 1.0f, 1.0f);
            Deci.markActionTime(player);
        }
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player)
    {
        if (world.isRemote)
        {
            player.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.GREEN + "You've used the "
                + stack.getDisplayName() + " to lower your radiation!"));
        }
        else
        {
            RadiationDose.lower(player, POINTS);
            --stack.stackSize;
            world.playSoundAtEntity(player, "deci:item.syringe.use", 3.0f, 1.0f);
            taken++;
        }
        return stack;
    }

    /** preInit, after Decimation's: the item, and an entry in every loot list holding Vitamin Capsules. */
    public static void register()
    {
        instance = new RadiationPills().setCreativeTab(Deci.medicalTab());
        GameRegistry.registerItem(instance, "radiation_pills");
        Item like = GameRegistry.findItem("deci", "ItemVitaminCapsules");
        int pools = like == null ? 0 : Deci.addLootLike(like, new ItemStack(instance));
        FMLLog.info("[deciworldgen] radiation pills: %d loot pools (like Vitamin Capsules)", pools);
    }

    /** For the dev test: pills taken on the server so far. */
    public static int taken()
    {
        return taken;
    }
}
