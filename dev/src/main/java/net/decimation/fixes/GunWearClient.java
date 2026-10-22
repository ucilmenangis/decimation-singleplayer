package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.decimation.mod.common.item.armor.ItemMaskDeci;
import net.decimation.mod.common.item.armor.ItemVestDeci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/**
 * The client side of durability (feature 10; GunWear, ArmorGunfireHandler): a jammed or worn out gun clicks instead of
 * firing, with a line on Decimation's toast now and then; R with a jammed gun in hand racks it (Decimation's Rack
 * animation, no firing for its length, Net message 3 to the server; the press is taken in the START phase of the client
 * tick, before Decimation's key handler reads R in END); tooltips show the durability of guns, armor and vests.
 */
public final class GunWearClient
{
    /** About the length of a Rack animation. */
    private static final long RACK_MILLIS = 1200;
    private static long rackUntil, lastClick, lastLine;
    /** For the dev test: racks done. */
    static int racks;

    public static int rackCount()
    {
        return racks;
    }

    static boolean racking()
    {
        return System.currentTimeMillis() < rackUntil;
    }

    /** A shot that did not happen: Decimation's empty click, and why, now and then. */
    static void click(EntityPlayer player, boolean jam, boolean used)
    {
        long now = System.currentTimeMillis();
        if (now - lastClick > 250)
        {
            lastClick = now;
            player.playSound("deci:item.gun.empty", 0.2f, 1.0f);
        }
        if ((jam || used) && now - lastLine > 2500)
        {
            lastLine = now;
            Deci.toast(jam ? "Jammed: press R to rack" : "Worn out: repair with Gun Parts");
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.currentScreen != null)
        {
            return;
        }
        ItemStack gun = mc.thePlayer.getHeldItem();
        if (GunWear.max(gun) <= 0 || !GunWear.jammed(gun.stackTagCompound))
        {
            return;
        }
        KeyBinding reload = Deci.reloadKey();
        if (reload != null && reload.isPressed())         // taken: Decimation's handler (END) sees no press
        {
            rackUntil = System.currentTimeMillis() + RACK_MILLIS;
            racks++;
            Deci.playRack(gun);
            Net.CHANNEL.sendToServer(new GunWear.Rack());
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent e)
    {
        ItemStack s = e.itemStack;
        int gunMax = GunWear.max(s);
        if (gunMax > 0)
        {
            int left = gunMax - GunWear.wear(s.stackTagCompound);
            e.toolTip.add(EnumChatFormatting.GRAY + "Durability " + left + " / " + gunMax);
            if (GunWear.jammed(s.stackTagCompound))
            {
                e.toolTip.add(EnumChatFormatting.RED + "Jammed: press R to rack");
            }
            else if (left <= 0)
            {
                e.toolTip.add(EnumChatFormatting.RED + "Worn out: repair with Gun Parts");
            }
            return;
        }
        if (s.getItem() instanceof ItemArmorDeci && !(s.getItem() instanceof ItemMaskDeci) && s.getMaxDamage() > 0)
        {
            int left = s.getMaxDamage() - s.getItemDamage();
            e.toolTip.add(EnumChatFormatting.GRAY + "Durability " + Math.max(0, left) + " / " + s.getMaxDamage());
            if (left <= 0)
            {
                e.toolTip.add(EnumChatFormatting.RED + (s.getItem() instanceof ItemVestDeci ? "Worn out: repair with an Armor Plate"
                                                                                          : "Worn out: repair with an Armor Repair Kit"));
            }
        }
    }
}
