package net.decimation.fixes;

import java.util.Random;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.decimation.mod.common.item.armor.ItemMaskDeci;
import net.decimation.mod.common.item.armor.ItemVestDeci;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
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
 *
 * Durability (feature 10, user 22 Oktober 2026; docs/superpowers/specs/2026-10-22-durability-design.md): every piece
 * that counted against a shot loses 1 (body pieces on every shot, the helmet on a headshot, the vest or the chest piece:
 * whichever counted); a used up piece (Durability.broken: damage at the maximum) is kept but protects nothing, so its
 * factor is taken back out of Decimation's player hit; vests (ItemVestDeci, PlayerData's vest slot), which no code
 * applied before, protect body shots: the stronger of vest and chest piece counts (Durability.bodyFactor). Melee hits
 * wear armor the vanilla way (Forge's ArmorProperties.ApplyArmor, at least 1 a piece, absorbing at most what is left);
 * meleeLast keeps that wear from destroying a piece: it stops on the maximum.
 */
public class ArmorGunfireHandler
{
    /** Share of NPC hits treated as headshots. */
    public static final double HEADSHOT_CHANCE = 0.2;
    /** Height above the feet where the head starts, as a share of entity height (1.35 of 1.8). */
    private static final double HEAD_FROM = 0.75;
    private static final int HELMET = 3; // armor slot index: 0 boots .. 3 helmet
    /** Share of each piece's protection left against NPC fire (player shots keep it whole). */
    static final float NPC_PROTECTION = 0.8f;

    private final Random random = new Random();

    private static final int CHEST = 2;
    /** For the dev test: shots handled and their time in nanoseconds. */
    static long shots, shotNanos;

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityPlayer))
        {
            return;
        }
        long start = System.nanoTime();
        handle(event);
        shots++;
        shotNanos += System.nanoTime() - start;
    }

    /** For the dev test: {hurt events handled, their nanoseconds}. */
    public static long[] totals()
    {
        return new long[] {shots, shotNanos};
    }

    /** For the dev test: average time of a hurt event of a player, microseconds. */
    public static double averageMicros()
    {
        return shots == 0 ? 0 : shotNanos / 1000.0 / shots;
    }

    private void handle(LivingHurtEvent event)
    {
        EntityPlayer victim = (EntityPlayer) event.entityLiving;
        String type = event.source.getDamageType();
        ItemStack vest = Deci.playerVest(victim);
        float vestFactor = working(vest) && vest.getItem() instanceof ItemVestDeci ? ((ItemVestDeci) vest.getItem()).getDamageMultiplier() : 1f;
        if ("gunDeci".equals(type))
        {
            // Decimation's hit handler multiplied boots, legs and chest, used up or not: those factors out again, the vest in
            float amount = event.ammount;
            for (int slot = 0; slot < HELMET; slot++)
            {
                if (broken(victim.getCurrentArmor(slot)))
                {
                    amount /= rawMultiplier(victim, slot);
                }
            }
            float chest = multiplier(victim, CHEST);
            if (vestFactor < chest)
            {
                amount *= vestFactor / chest;
            }
            boolean head = isHeadshot(event.source.getEntity(), victim);
            if (head)
            {
                amount *= multiplier(victim, HELMET);
            }
            event.ammount = amount;
            wear(victim, vest, chest, vestFactor, head);
            return;
        }
        if (!"human".equals(type) && !"turret".equals(type))
        {
            return;
        }
        float damage = event.ammount;
        float armor = 1;
        for (int slot = 0; slot < CHEST; slot++)
        {
            armor *= againstNpc(multiplier(victim, slot));
        }
        float chest = multiplier(victim, CHEST);
        armor *= againstNpc(Durability.bodyFactor(chest, vestFactor));
        boolean head = random.nextDouble() < HEADSHOT_CHANCE;
        if (head)
        {
            armor *= againstNpc(multiplier(victim, HELMET));
        }
        // a .50 BMG NPC hit (NpcShots.armorPiercing): armor at half strength
        damage *= NpcShots.armorPiercing ? (float) Math.sqrt(armor) : armor;
        event.ammount = damage;
        wear(victim, vest, chest, vestFactor, head);
    }

    /** One durability off each piece that counted against a shot (feature 10). */
    private static void wear(EntityPlayer victim, ItemStack vest, float chest, float vestFactor, boolean head)
    {
        for (int slot = 0; slot < CHEST; slot++)
        {
            wear(victim.getCurrentArmor(slot));
        }
        if (vestFactor < chest)
        {
            wear(vest);
        }
        else
        {
            wear(victim.getCurrentArmor(CHEST));
        }
        if (head)
        {
            wear(victim.getCurrentArmor(HELMET));
        }
    }

    /** One durability off a working Decimation armor piece or vest (masks and anything else untouched). */
    static void wear(ItemStack piece)
    {
        if (working(piece))
        {
            piece.setItemDamage(piece.getItemDamage() + 1);
        }
    }

    /** A Decimation armor piece or vest with durability left. */
    static boolean working(ItemStack piece)
    {
        return worn(piece) && !Durability.broken(piece.getItemDamage(), piece.getMaxDamage());
    }

    /** A used up Decimation armor piece or vest. */
    static boolean broken(ItemStack piece)
    {
        return worn(piece) && Durability.broken(piece.getItemDamage(), piece.getMaxDamage());
    }

    /** A Decimation armor piece or vest that wears (masks do not). */
    private static boolean worn(ItemStack piece)
    {
        return piece != null && piece.getItem() instanceof ItemArmorDeci && !(piece.getItem() instanceof ItemMaskDeci)
               && piece.getMaxDamage() > 0;
    }

    /**
     * Melee and other blockable hits, last: Forge's ApplyArmor wears each armor piece (Durability.forgeWear) and destroys
     * one that passes its maximum; a piece this hit would push past it is first set to the maximum less 1, where Forge's
     * wear is exactly 1 (Durability.beforeVanillaWear): it ends on the maximum, kept, and never gains durability.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void meleeLast(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityPlayer) || event.source.isUnblockable()
            || event.ammount <= 0)
        {
            return;
        }
        EntityPlayer victim = (EntityPlayer) event.entityLiving;
        for (int slot = 0; slot <= HELMET; slot++)
        {
            ItemStack piece = victim.getCurrentArmor(slot);
            if (worn(piece))
            {
                int points = ((ItemArmor) piece.getItem()).damageReduceAmount;
                piece.setItemDamage(Durability.beforeVanillaWear(piece.getItemDamage(), piece.getMaxDamage(), event.ammount, points));
            }
        }
    }

    /** Every Decimation armor piece 256, every vest 512 (masks keep theirs): at preInit, after Decimation's items exist. */
    public static void setMaxima()
    {
        for (Object o : Item.itemRegistry)
        {
            if (o instanceof ItemArmorDeci && !(o instanceof ItemMaskDeci))
            {
                ((Item) o).setMaxDamage(o instanceof ItemVestDeci ? Durability.VEST : Durability.ARMOR);
            }
        }
    }

    /** Damage multiplier of the Decimation armor piece in a slot, 1 if none. */
    /**
     * A piece's multiplier against NPC fire: its protection (1 - multiplier) at NPC_PROTECTION (user, 22 Oktober 2026:
     * "nerf all armor buff by -20% that affected to player when they shooted by npc"; a piece blocking 50 % blocks 40 %).
     */
    static float againstNpc(float multiplier)
    {
        return 1f - NPC_PROTECTION * (1f - multiplier);
    }

    /** A slot's factor as it protects now: 1 for none or a used up piece. */
    private static float multiplier(EntityPlayer player, int slot)
    {
        ItemStack piece = player.getCurrentArmor(slot);
        return broken(piece) ? 1.0f : rawMultiplier(player, slot);
    }

    /** A slot's factor as Decimation's hit handler applies it (used up or not). */
    private static float rawMultiplier(EntityPlayer player, int slot)
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
