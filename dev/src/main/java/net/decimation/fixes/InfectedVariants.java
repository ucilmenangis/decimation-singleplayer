package net.decimation.fixes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Zombie variants (user choice 2026-10-09; new_feature.md "Zombie variants
 * design"): a plain infected (InfectedEntity, Decimation's common look) may
 * become a runner, a riot zombie or a screamer when it first joins a world;
 * at night every infected walks faster and hits harder. Config
 * deciworldgen_zombies.cfg. Decimation resets an infected's walk speed BASE
 * value every tick, so speed changes are attribute modifiers (saved with the
 * mob, like its max health).
 */
public class InfectedVariants
{
    public static final String TAG = "deciworldgen_zvariant", SCREAM = "deciworldgen_scream";
    private static final UUID SPEED = UUID.fromString("5d1c1f6e-3b0a-4f0e-9a1d-7f2a3c4b5e01");
    private static final UUID NIGHT_SPEED = UUID.fromString("5d1c1f6e-3b0a-4f0e-9a1d-7f2a3c4b5e02");
    private static final UUID NIGHT_HIT = UUID.fromString("5d1c1f6e-3b0a-4f0e-9a1d-7f2a3c4b5e03");

    /** One variant: how often, its health, speed, the share of player gun damage it takes, its clothes. */
    public static final class Variant
    {
        public final String name;
        public int weight;
        public float health, taken, speed;
        final String[][] armor; // helmet, chest, legs, boots: registry names (null slot: nothing)
        final boolean screamer;

        Variant(String name, int weight, float health, float taken, float speed, boolean screamer, String[][] armor)
        {
            this.name = name;
            this.weight = weight;
            this.health = health;
            this.taken = taken;
            this.speed = speed;
            this.screamer = screamer;
            this.armor = armor;
        }
    }

    final List<Variant> variants = new ArrayList<Variant>();
    private final Random random = new Random();
    private boolean nightFrenzy = true;
    private float nightSpeed = 0.2f, nightHit = 2, screamRadius = 32;
    private int screamCooldown = 200;
    private static InfectedVariants instance;
    /** Dev tests: infected the last scream reached. */
    public static volatile int lastAlerted;

    public static InfectedVariants instance()
    {
        return instance;
    }

    public InfectedVariants(File configDir)
    {
        instance = this;
        // new variants go last: a spawn egg's metadata is its index
        variants.add(new Variant("runner", 12, 14, 1.0f, 1.55f, false, new String[][] {null,
            {"hoodie1", "hoodie2", "hoodie3", "hoodie4", "hoodie5", "casual1Vest", "casual2Vest", "casual3Vest"},
            {"casual1Pants", "casual2Pants", "casual3Pants"}, {"casual1Boots", "casual2Boots", "casual3Boots"}}));
        variants.add(new Variant("riot", 8, 30, 0.5f, 0.85f, false, new String[][] {{"marineHelm", "uahdHelmet"},
            {"nypdCoat"}, {"nypdPants"}, {"nypdBoots"}}));
        variants.add(new Variant("screamer", 5, 16, 1.0f, 1.0f, true, new String[][] {{"hazmatHelm"}, {"hazmatVest"},
            {"hazmatPants"}, {"hazmatBoots"}}));
        Configuration cfg = new Configuration(new File(configDir, "deciworldgen_zombies.cfg"));
        for (Variant v : variants)
        {
            String cat = "variant_" + v.name;
            v.weight = cfg.getInt("weight", cat, v.weight, 0, 1000, "percent of plain infected that become one");
            v.health = cfg.getFloat("health", cat, v.health, 1, 1000, "max health (common infected 20)");
            v.taken = cfg.getFloat("damageTaken", cat, v.taken, 0.01f, 1, "share of a player's gun damage it takes");
            v.speed = cfg.getFloat("speed", cat, v.speed, 0.1f, 5, "walk speed x this");
        }
        nightFrenzy = cfg.getBoolean("nightFrenzy", "night", nightFrenzy, "infected are faster and hit harder at night");
        nightSpeed = cfg.getFloat("speed", "night", nightSpeed, 0, 5, "night walk speed +this share (0.2 = +20%)");
        nightHit = cfg.getFloat("attack", "night", nightHit, 0, 20, "night attack damage +this (infected hit 3)");
        screamRadius = cfg.getFloat("radius", "screamer", screamRadius, 1, 128,
            "a screamer's scream sends every infected within this many blocks after its target");
        screamCooldown = cfg.getInt("cooldownTicks", "screamer", screamCooldown, 20, 12000, "ticks between screams");
        cfg.save();
    }

    public Variant byName(String name)
    {
        for (Variant v : variants)
        {
            if (v.name.equals(name))
            {
                return v;
            }
        }
        return null;
    }

    public Variant byIndex(int i)
    {
        return i >= 0 && i < variants.size() ? variants.get(i) : null;
    }

    /** After the zone handlers (they pick the military / police looks first). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJoin(EntityJoinWorldEvent event)
    {
        Entity e = event.entity;
        if (event.world.isRemote || event.isCanceled() || !Deci.isPlainInfected(e) || e.getEntityData().hasKey(TAG))
        {
            return;
        }
        if (Deci.infectedVariant(e) != 0)
        {
            e.getEntityData().setString(TAG, "none"); // a zone look (military / police) stays as it is
            return;
        }
        int r = random.nextInt(100);
        for (Variant v : variants)
        {
            if ((r -= v.weight) < 0)
            {
                apply((EntityLiving) e, v);
                return;
            }
        }
        e.getEntityData().setString(TAG, "none");
    }

    /** Clothes, health and speed of a variant; remembered in the entity data (saved with it). */
    public void apply(EntityLiving z, Variant v)
    {
        for (int slot = 0; slot < 4; slot++)
        {
            String[] pool = v.armor[slot];
            Item piece = pool == null ? null : GameRegistry.findItem("deci", pool[random.nextInt(pool.length)]);
            z.setCurrentItemOrArmor(4 - slot, piece == null ? null : new ItemStack(piece));
        }
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(v.health);
        z.setHealth(v.health);
        IAttributeInstance speed = z.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speed.getModifier(SPEED) != null)
        {
            speed.removeModifier(speed.getModifier(SPEED));
        }
        if (v.speed != 1)
        {
            speed.applyModifier(new AttributeModifier(SPEED, "zombie variant speed", v.speed - 1, 1));
        }
        z.getEntityData().setString(TAG, v.name);
    }

    /** Every second per infected (server): night frenzy on / off, the screamer's scream. */
    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase e = event.entityLiving;
        if (e.worldObj.isRemote || !Deci.isInfected(e) || (e.ticksExisted + e.getEntityId()) % 20 != 0)
        {
            return;
        }
        boolean night = nightFrenzy && !e.worldObj.isDaytime() && e.worldObj.provider.dimensionId == 0;
        modifier(e.getEntityAttribute(SharedMonsterAttributes.movementSpeed), NIGHT_SPEED, "night frenzy speed",
                 night ? nightSpeed : 0, 2); // operation 2: x (1 + amount) on top of a variant's speed
        modifier(e.getEntityAttribute(SharedMonsterAttributes.attackDamage), NIGHT_HIT, "night frenzy attack",
                 night ? nightHit : 0, 0);
        Variant v = byName(e.getEntityData().getString(TAG));
        if (v != null && v.screamer && e instanceof EntityLiving)
        {
            scream((EntityLiving) e);
        }
    }

    private static void modifier(IAttributeInstance a, UUID id, String name, double amount, int op)
    {
        if (a == null)
        {
            return;
        }
        AttributeModifier m = a.getModifier(id);
        if (m != null && (amount == 0 || m.getAmount() != amount))
        {
            a.removeModifier(m);
            m = null;
        }
        if (m == null && amount != 0)
        {
            a.applyModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    /** A screamer with a target screams and sends every infected around after it (once per cooldown). */
    void scream(EntityLiving s)
    {
        EntityLivingBase target = s.getAttackTarget();
        long now = s.worldObj.getTotalWorldTime();
        if (target == null || target.isDead || now < s.getEntityData().getLong(SCREAM))
        {
            return;
        }
        s.getEntityData().setLong(SCREAM, now + screamCooldown);
        s.worldObj.playSoundAtEntity(s, "deci:mob.frail.scream", 4.0f, 1.0f);
        double r = screamRadius;
        int n = 0;
        for (Object o : s.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(
            s.posX - r, s.posY - 16, s.posZ - r, s.posX + r, s.posY + 16, s.posZ + r)))
        {
            EntityLiving z = (EntityLiving) o;
            if (z != s && Deci.isInfected(z) && z.getDistanceSqToEntity(s) <= r * r)
            {
                z.setAttackTarget(target);
                z.getNavigator().tryMoveToEntityLiving(target, 1.2);
                n++;
            }
        }
        lastAlerted = n;
    }

    /** A player's gun ("gunDeci") hitting a variant: its share of the damage (riot 50%). */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !"gunDeci".equals(event.source.getDamageType()))
        {
            return;
        }
        Variant v = byName(event.entityLiving.getEntityData().getString(TAG));
        if (v != null)
        {
            event.ammount *= v.taken;
        }
    }
}
