package net.decimation.fixes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Tiers for Decimation's armed humans (bandits, soldiers, Soviets): gear,
 * gun, health, fire rate and how much of a player's gun damage they take
 * (new_feature.md "Step 1 design: NPC tiers").
 *
 * Decimation rolls an NPC's gun and armor in its constructor, on the server
 * and the client separately, and never saves or syncs the gun. So:
 * - the tier is rolled once on the server when the NPC first joins a world
 *   and stored in its entity data (saved with it); a reloaded NPC gets its
 *   gun and fire rate back from there;
 * - all 4 armor slots are always filled (vanilla sends worn armor to the
 *   client, an empty slot would show the client's own roll);
 * - the gun's registry name goes to the client through data watcher slot
 *   GUN_SLOT and is put back on the client NPC every tick.
 * Decimation applies armor only to players, so a player's gun hits an NPC
 * for full damage whatever it wears; the tier's "taken" share replaces that.
 * Defaults: config/deciworldgen_npc.cfg.
 */
public class NpcLoadouts
{
    public static final String TAG = "deciworldgen_tier", GUN_TAG = "deciworldgen_gun";
    /** Data watcher slot for the gun name (vanilla mobs use up to 12, Decimation's humans none above 20). */
    public static final int GUN_SLOT = 26;

    /** One tier: who gets it and with what. */
    public static final class Tier
    {
        public final String name;
        public final NpcKind kind;
        public int weight, militaryWeight;
        public float health, taken;
        /** Ticks between shots for rifles, pistols, SMGs (machine guns and sniper rifles have their own). */
        int delayMin = 4, delayMax = 12;
        final String[][] armor; // helmet, chest, legs, boots: registry names to pick from
        final String[] guns;

        Tier(String name, NpcKind kind, int weight, int militaryWeight, float health, float taken,
             String[][] armor, String... guns)
        {
            this.name = name;
            this.kind = kind;
            this.weight = weight;
            this.militaryWeight = militaryWeight;
            this.health = health;
            this.taken = taken;
            this.armor = armor;
            this.guns = guns;
        }

        Tier delay(int min, int max)
        {
            delayMin = min;
            delayMax = max;
            return this;
        }
    }

    private static final String[] MG = {"pkm", "pkp", "m240", "m60", "rpd", "mk48", "mg3", "m1919a6"};
    private static final String[] SNIPER = {"sv98", "svd", "mosinnagant", "kar98k", "l115a3", "jng90", "barrett",
                                            "m110", "m1garand", "svt40"};

    private static final String[] CAPS = {"banditHelm", "militiaHelm", "capRed", "capBlue", "capGreen", "banditHelm"};
    private static final String[] CASUAL_CHEST = {"banditVest", "militiaVest", "casual1Vest", "casual2Vest",
                                                  "casual3Vest", "casual4Vest", "casual5Vest", "hoodie1", "hoodie2",
                                                  "hoodie3", "hoodie5", "hoodie7", "hoodie9"};
    private static final String[] CASUAL_LEGS = {"banditPants", "militiaPants", "casual1Pants", "casual2Pants",
                                                 "casual3Pants"};
    private static final String[] CASUAL_BOOTS = {"banditBoots", "militiaBoots", "casual1Boots", "casual2Boots",
                                                  "casual3Boots"};

    final List<Tier> tiers = new ArrayList<Tier>();
    private final Random random = new Random();
    private static NpcLoadouts instance;

    /** The registered instance (dev tests). */
    public static NpcLoadouts instance()
    {
        return instance;
    }

    public NpcLoadouts(File configDir)
    {
        instance = this;
        tiers.add(new Tier("bandit_light", NpcKind.BANDIT, 55, 25, 20, 1.0f,
            new String[][] {CAPS, CASUAL_CHEST, CASUAL_LEGS, CASUAL_BOOTS},
            "makarov", "colt", "glock17", "browninghp", "uzi", "mp5a3", "r870", "dbarrel", "sks", "insas",
            "ak12", "m14", "mosinnagant").delay(5, 20)); // Decimation's own bandit rate
        tiers.add(new Tier("bandit_medium", NpcKind.BANDIT, 35, 45, 26, 0.8f,
            new String[][] {{"militiaHelm", "banditHelm"}, {"militiaVest", "banditVest"},
                            {"militiaPants", "banditPants"}, {"militiaBoots", "banditBoots"}},
            "akm", "akms", "sks", "vz58", "rpk", "fal", "g3a3", "m14", "ak74", "svt40", "mpi40"));
        tiers.add(new Tier("bandit_heavy", NpcKind.BANDIT, 10, 30, 32, 0.7f,
            new String[][] {{"militiaHelm", "marineHelm", "uahdHelmet"}, {"militiaVest"}, {"militiaPants"},
                            {"militiaBoots", "marineBoots"}},
            "pkm", "pkm", "rpk74", "rpd", "sv98", "svd", "fal", "g3a4"));
        String[] camo = {"marine", "marineforest", "marineurban", "marineblack"};
        for (String c : camo)
        {
            tiers.add(new Tier("soldier_" + c, NpcKind.SOLDIER, 1, 1, 24, 0.8f,
                new String[][] {{c + "Helm", c + "Helm", c + "Hat"}, {c + "Vest"}, {c + "Pants"}, {c + "Boots"}},
                "m4a4", "m4a4", "m16a2", "g36c", "fnscarl", "famas", "l85a1", "acr", "m240", "m110").delay(2, 6));
        }
        tiers.add(new Tier("military", NpcKind.SOVIET, 1, 1, 40, 0.6f,
            new String[][] {{"spetsnazHelm"}, {"spetsnazVest"}, {"spetsnazPants"}, {"spetsnazBoots"}},
            "ak74", "ak74", "ak12", "aks74u", "rpk74", "asval", "pkp", "svd").delay(3, 9));
        load(new File(configDir, "deciworldgen_npc.cfg"));
    }

    private void load(File file)
    {
        Configuration cfg = new Configuration(file);
        for (Tier t : tiers)
        {
            String cat = "tier_" + t.name;
            t.health = cfg.getFloat("health", cat, t.health, 1, 1000, "max health (vanilla player 20)");
            t.taken = cfg.getFloat("damageTaken", cat, t.taken, 0.01f, 1, "share of a player's gun damage it takes");
            t.weight = cfg.getInt("weight", cat, t.weight, 0, 1000, "chance against the other tiers of its kind");
            t.militaryWeight = cfg.getInt("militaryWeight", cat, t.militaryWeight, 0, 1000,
                                          "the same inside military areas");
        }
        cfg.save();
    }

    static boolean military(net.minecraft.world.World world, double x, double z)
    {
        return Sectors.sector(world.getSeed(), (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4)
            == StructureGenerator.MIL;
    }

    static Item item(String name)
    {
        return GameRegistry.findItem("deci", name);
    }

    @SubscribeEvent
    public void onConstructing(EntityEvent.EntityConstructing event)
    {
        if (Deci.npcKind(event.entity) != null)
        {
            try
            {
                event.entity.getDataWatcher().addObject(GUN_SLOT, "");
            }
            catch (IllegalArgumentException e)
            {
                FMLLog.info("[deciworldgen] data watcher slot %d taken on %s", GUN_SLOT, event.entity);
            }
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event)
    {
        NpcKind kind = Deci.npcKind(event.entity);
        if (event.world.isRemote || kind == null || kind == NpcKind.HAZMAT)
        {
            return;
        }
        EntityLiving npc = (EntityLiving) event.entity;
        NBTTagCompound data = npc.getEntityData();
        Tier tier = byName(data.getString(TAG));
        if (tier == null)
        {
            tier = roll(kind, military(event.world, npc.posX, npc.posZ));
            if (tier == null)
            {
                return;
            }
            equip(npc, tier, data);
        }
        String gun = data.getString(GUN_TAG);
        Item g = item(gun);
        if (g != null)
        {
            arm(npc, tier, gun, g);
        }
    }

    /** Client: the gun the server chose (data watcher), in hand and in BanditEntity's own field. */
    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event)
    {
        Entity e = event.entity;
        if (!e.worldObj.isRemote || Deci.npcKind(e) == null)
        {
            return;
        }
        String name;
        try
        {
            name = e.getDataWatcher().getWatchableObjectString(GUN_SLOT);
        }
        catch (RuntimeException ex)
        {
            return;
        }
        if (name == null || name.isEmpty())
        {
            return;
        }
        ItemStack held = Deci.npcGun(e);
        Item g = item(name);
        if (g != null && (held == null || held.getItem() != g))
        {
            Deci.setNpcGun(e, new ItemStack(g));
        }
    }

    /** A player's gun ("gunDeci") hitting a tiered NPC: the tier's share of the damage. */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || !"gunDeci".equals(event.source.getDamageType()))
        {
            return;
        }
        Tier tier = byName(event.entityLiving.getEntityData().getString(TAG));
        if (tier != null)
        {
            event.ammount *= tier.taken;
        }
    }

    public Tier byName(String name)
    {
        for (Tier t : tiers)
        {
            if (t.name.equals(name))
            {
                return t;
            }
        }
        return null;
    }

    Tier roll(NpcKind kind, boolean military)
    {
        int total = 0;
        for (Tier t : tiers)
        {
            total += t.kind == kind ? (military ? t.militaryWeight : t.weight) : 0;
        }
        if (total <= 0)
        {
            return null;
        }
        int r = random.nextInt(total);
        for (Tier t : tiers)
        {
            if (t.kind == kind && (r -= military ? t.militaryWeight : t.weight) < 0)
            {
                return t;
            }
        }
        return null;
    }

    /** Applies a tier to a new NPC: armor, gun, health; remembers it in the entity data. */
    public void equip(EntityLiving npc, Tier tier, NBTTagCompound data)
    {
        for (int slot = 0; slot < 4; slot++)
        {
            String[] pool = tier.armor[slot];
            Item piece = item(pool[random.nextInt(pool.length)]);
            if (piece != null)
            {
                npc.setCurrentItemOrArmor(4 - slot, new ItemStack(piece)); // 4 helmet .. 1 boots
            }
        }
        String gun = tier.guns[random.nextInt(tier.guns.length)];
        if (item(gun) == null)
        {
            gun = "ak74";
        }
        npc.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(tier.health);
        npc.setHealth(tier.health);
        data.setString(TAG, tier.name);
        data.setString(GUN_TAG, gun);
    }

    /** Gun in hand (server field + watcher for the client) and a fire rate that fits it. */
    private static void arm(EntityLiving npc, Tier tier, String name, Item gun)
    {
        Deci.setNpcGun(npc, new ItemStack(gun));
        npc.getDataWatcher().updateObject(GUN_SLOT, name);
        if (contains(MG, name))
        {
            Deci.setNpcShotDelay(npc, 3, 8);
        }
        else if (contains(SNIPER, name))
        {
            Deci.setNpcShotDelay(npc, 25, 45);
        }
        else
        {
            Deci.setNpcShotDelay(npc, tier.delayMin, tier.delayMax);
        }
    }

    private static boolean contains(String[] list, String s)
    {
        for (String x : list)
        {
            if (x.equals(s))
            {
                return true;
            }
        }
        return false;
    }
}
