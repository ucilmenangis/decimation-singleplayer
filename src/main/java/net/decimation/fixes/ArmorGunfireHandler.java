package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Decimation armor did nothing against NPC gunfire.
 *
 * Armor protection in this mod is {@code ItemArmorDeci.damageMultiplier},
 * and the mod applies it in exactly one place: the handler for a PLAYER's gun
 * hit on another player (obfuscated {@code deci.aE.a$z$a}), which multiplies
 * the damage by every worn piece except the helmet slot before calling
 * attackEntityFrom with the "gunDeci" source. Bandits and armed traders shoot
 * with the plain "human" source and turrets with "turret", straight into
 * attackEntityFrom, and every Decimation damage source bypasses vanilla
 * armor, so against those shots armor counted for nothing.
 *
 * This applies the same rule (same pieces, same helmet exclusion) to "human"
 * and "turret" damage. "gunDeci" is left alone, it already got the
 * multiplier in the hit handler, so nothing is applied twice.
 */
public class ArmorGunfireHandler
{
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityPlayer))
        {
            return;
        }
        String type = event.source.getDamageType();
        if (!"human".equals(type) && !"turret".equals(type))
        {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        float damage = event.ammount;
        for (int slot = 0; slot < 4; slot++)
        {
            if (slot == 3)
            {
                continue; // helmet, excluded exactly like the player hit handler
            }
            ItemStack piece = player.getCurrentArmor(slot);
            if (piece != null && piece.getItem() instanceof ItemArmorDeci)
            {
                damage *= ((ItemArmorDeci) piece.getItem()).getDamageMultiplier();
            }
        }
        event.ammount = damage;
    }
}
