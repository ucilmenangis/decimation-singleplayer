package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Vehicles (jeep, tank, helicopter...) vanished with one punch in survival.
 *
 * Decimation's vehicle body ({@code deci.ad.e}, subclassed per vehicle type)
 * treats ANY player hit on an empty vehicle as "pick it up": it removes
 * itself and drops its item, survival included; only gun damage takes the
 * health path. Its seat and hitbox parts ({@code deci.ad.b}, {@code
 * deci.ad.a}) forward every hit to the body, so clicking any part of it
 * counts.
 *
 * This cancels survival melee on vehicles and their parts unless the player
 * is sneaking, so sneak + punch still recovers an empty vehicle as an item
 * and a stray click no longer does. Creative keeps the original behaviour;
 * gunfire and explosions still go through the vehicle's health. Fires on
 * both sides (client attack and server packet), so both are cancelled.
 */
public class VehicleHitHandler
{
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event)
    {
        EntityPlayer player = event.entityPlayer;
        Entity target = event.target;
        if (player == null || player.capabilities.isCreativeMode || player.isSneaking())
        {
            return;
        }
        if (target instanceof deci.ad.e || target instanceof deci.ad.b)
        {
            event.setCanceled(true);
        }
    }
}
