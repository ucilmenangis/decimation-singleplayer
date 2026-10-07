package net.decimation.fixes;

import java.util.Random;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Decimation armor against gunfire: NPC shots and helmets.
 *
 * Armor protection in this mod is {@code ItemArmorDeci.damageMultiplier},
 * and the mod applies it in exactly one place: the handler for a PLAYER's gun
 * hit on another player (obfuscated {@code deci.aE.a$z$a}), which multiplies
 * the damage by every worn piece EXCEPT the helmet before calling
 * attackEntityFrom with the "gunDeci" source. Bandits and armed traders shoot
 * with the plain "human" source and turrets with "turret", straight into
 * attackEntityFrom, and every Decimation damage source bypasses vanilla
 * armor, so against those shots armor counted for nothing, and helmets never
 * counted against any shot.
 *
 * Helmets carry a much stronger multiplier (0.3 before the 35% buff) than the
 * other pieces (0.8 to 0.85), so applying them to every hit would make a full
 * set nearly invulnerable. They count on headshots only (user decision
 * 2026-10-07):
 * - "gunDeci" (a player's gun): the hit handler already applied the other
 *   pieces, so only the helmet is added here, when the shooter's aim line
 *   passes the victim at head height;
 * - "human" / "turret" (NPC guns): body pieces always; the helmet on a random
 *   HEADSHOT_CHANCE of hits, because NPC shots carry no hit position and NPCs
 *   always aim at eye level.
 */
public class ArmorGunfireHandler
{
    /** Share of NPC hits treated as headshots. */
    public static final double HEADSHOT_CHANCE = 0.2;
    /** Height above the feet where the head starts, as a share of entity height (1.35 of 1.8). */
    private static final double HEAD_FROM = 0.75;
    private static final int HELMET = 3; // armor slot index: 0 boots .. 3 helmet

    private final Random random = new Random();

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityPlayer))
        {
            return;
        }
        EntityPlayer victim = (EntityPlayer) event.entityLiving;
        String type = event.source.getDamageType();
        if ("gunDeci".equals(type))
        {
            if (isHeadshot(event.source.getEntity(), victim))
            {
                event.ammount *= multiplier(victim, HELMET);
            }
            return;
        }
        if (!"human".equals(type) && !"turret".equals(type))
        {
            return;
        }
        float damage = event.ammount;
        for (int slot = 0; slot < HELMET; slot++)
        {
            damage *= multiplier(victim, slot);
        }
        if (random.nextDouble() < HEADSHOT_CHANCE)
        {
            damage *= multiplier(victim, HELMET);
        }
        event.ammount = damage;
    }

    /** Damage multiplier of the Decimation armor piece in a slot, 1 if none. */
    private static float multiplier(EntityPlayer player, int slot)
    {
        ItemStack piece = player.getCurrentArmor(slot);
        if (piece != null && piece.getItem() instanceof ItemArmorDeci)
        {
            return ((ItemArmorDeci) piece.getItem()).getDamageMultiplier();
        }
        return 1.0f;
    }

    /**
     * Where the shooter's aim line passes the victim: take the point on the
     * line closest to the victim's vertical axis and compare its height with
     * the head band. Public for the dev autotest.
     */
    public static boolean isHeadshot(Entity shooter, Entity victim)
    {
        if (shooter == null || victim == null)
        {
            return false;
        }
        // boundingBox.minY is the feet on both sides (client players keep posY at the eyes)
        Vec3 eye = Vec3.createVectorHelper(shooter.posX,
            shooter.boundingBox.minY + shooter.getEyeHeight(), shooter.posZ);
        Vec3 look = shooter.getLookVec();
        double dx = victim.posX - eye.xCoord, dz = victim.posZ - eye.zCoord;
        double horiz = look.xCoord * look.xCoord + look.zCoord * look.zCoord;
        if (horiz < 1.0E-6)
        {
            return false; // aiming straight up or down
        }
        double t = (dx * look.xCoord + dz * look.zCoord) / horiz;
        if (t <= 0)
        {
            return false; // victim is behind the shooter
        }
        double hitY = eye.yCoord + look.yCoord * t;
        double feet = victim.boundingBox.minY;
        double height = victim.boundingBox.maxY - feet;
        return hitY >= feet + height * HEAD_FROM && hitY <= victim.boundingBox.maxY + 0.1;
    }
}
