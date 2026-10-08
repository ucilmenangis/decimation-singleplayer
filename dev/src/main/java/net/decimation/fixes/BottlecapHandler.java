package net.decimation.fixes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

/**
 * Singleplayer fix: bottlecaps picked up as plain items instead of being
 * credited as currency.
 *
 * The mod's own pickup handler (obfuscated {@code deci.aK.e}) does exactly
 * this, but its event method is annotated {@code @SideOnly(Side.SERVER)} -
 * FML strips it when the physical side is CLIENT, so in singleplayer the
 * handler never fires (same ServerProxy disease as every other singleplayer
 * bug in this mod). This class replicates its bottlecap logic 1:1 - balance
 * cap, chat message, pickup sound - calling the mod's own player-data class
 * ({@code deci.Q.b}) directly, so the caps land in the same balance the HUD
 * reads.
 *
 * On a dedicated server the original handler IS registered, so this one
 * steps aside to avoid double-crediting.
 */
public class BottlecapHandler
{
    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event)
    {
        if (FMLCommonHandler.instance().getSide().isServer())
        {
            return; // dedicated server: original deci.aK.e handles it
        }
        if (Deci.itemPickupFlag())
        {
            return; // same global kill-switch the original respects
        }
        if (!(event.entity instanceof EntityPlayer))
        {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        ItemStack stack = event.item.getEntityItem(); // getEntityItem
        if (stack == null || stack.stackSize <= 0) // stackSize
        {
            return;
        }

        if (stack.getItem() == Deci.bottlecap())
        {
            Deci.Player data = Deci.player(player);
            if (data != null && data.bottlecaps() < data.maxBottlecaps())
            {
                int n = stack.stackSize;
                data.addBottlecaps(n);
                player.addChatComponentMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "+" + n
                    + EnumChatFormatting.GRAY + " bottlecaps"));
                stack.stackSize = 0;
                player.worldObj.playSoundAtEntity(player,
                    "deci:misc.bottlecap.pickup", 2.0F, 1.0F);
            }
        }
        else if (stack.getItem() == Deci.goldBottlecap())
        {
            Deci.Player data = Deci.player(player);
            if (data != null && data.goldBottlecaps() < data.maxBottlecaps())
            {
                int n = stack.stackSize;
                data.addGoldBottlecaps(n);
                player.addChatComponentMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "+" + n
                    + EnumChatFormatting.GRAY + " gold bottlecaps"));
                stack.stackSize = 0;
                player.worldObj.playSoundAtEntity(player,
                    "deci:misc.bottlecap.pickupgold", 2.0F, 1.0F);
            }
        }
    }
}
