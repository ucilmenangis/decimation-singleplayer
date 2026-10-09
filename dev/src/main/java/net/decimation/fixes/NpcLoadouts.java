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
import net.minecraft.entity.EntityLivingBase;
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
        /** Aim spread, degrees (sigma of a gaussian per axis; NpcShots); machine guns x MG_SPREAD. */
        public float spread = 1.2f;
        /** Armor pools hold whole sets: one index for all 4 slots (juggernaut colours stay matched). */
        boolean matchedSet;
        /** Walk speed (Decimation's humans 0.25) and knockback resistance; 0 = unchanged. */
        double speed, knockbackResistance;
        /** NPC gun damage on a player x this, on top of npcDamageToPlayer (NpcShots.damageScale). */
        public float damageDealt = 1;
        /** Attachments per gun class ("sight=4x", "barrel=mgSuppressor", "grip=laser") and a mask item. */
        String[] mgKit = {}, sniperKit = {}, rifleKit = {};
        String mask;
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

        Tier spread(float degrees)
        {
            spread = degrees;
            return this;
        }

        Tier elite(float damageDealt, String mask, String[] mgKit, String[] sniperKit)
        {
            this.damageDealt = damageDealt;
            this.mask = mask;
            this.mgKit = mgKit;
            this.sniperKit = sniperKit;
            return this;
        }

        Tier heavy(double speed, double knockbackResistance)
        {
            this.speed = speed;
            this.knockbackResistance = knockbackResistance;
            matchedSet = true;
            return this;
        }
    }

    static final String[] MG = {"pkm", "pkp", "m240", "m60", "rpd", "mk48", "mg3", "m1919a6"};
    /** Rocket launchers NPCs fire as real rockets (NpcShots); grenade launchers are not used. */
    static final String[] ROCKET = {"rpg7", "rpg18"};
    static final String[] SNIPER = {"sv98", "svd", "mosinnagant", "kar98k", "l115a3", "jng90", "barrett",
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
    /** NPC gun hits ("human") on a player: damage factor; every hit counts (no vanilla hit cooldown). */
    private float npcDamageToPlayer = 5;
    /** NPC gun hits on a player are ignored this many ticks after a hit (vanilla 10). */
    private int hitCooldown = 5;
    /** Automatic guns fire bursts (NpcShots); spread grows by recoilSpread per shot of a burst. */
    private boolean autoFire = true;
    private float recoilSpread = 0.35f;
    /** Ticks a reload takes once the magazine is empty (user 2026-10-09: about 4 s). */
    private int reloadTicks = 80;
    /** Spread factor for machine guns; sniper rifles use SNIPER_SPREAD degrees. */
    static final float MG_SPREAD = 1.4f, SNIPER_SPREAD = 0.25f, UNTIERED_SPREAD = 1.2f;
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
            "ak12", "m14", "mosinnagant").delay(5, 20).spread(1.6f)); // Decimation's own bandit rate
        tiers.add(new Tier("bandit_medium", NpcKind.BANDIT, 35, 45, 26, 0.8f,
            new String[][] {{"militiaHelm", "banditHelm"}, {"militiaVest", "banditVest"},
                            {"militiaPants", "banditPants"}, {"militiaBoots", "banditBoots"}},
            "akm", "akms", "sks", "vz58", "rpk", "fal", "g3a3", "m14", "ak74", "svt40", "mpi40").spread(1.3f));
        tiers.add(new Tier("bandit_heavy", NpcKind.BANDIT, 10, 30, 32, 0.7f,
            new String[][] {{"militiaHelm", "marineHelm", "uahdHelmet"}, {"militiaVest"}, {"militiaPants"},
                            {"militiaBoots", "marineBoots"}},
            "pkm", "pkm", "rpk74", "rpd", "sv98", "svd", "fal", "g3a4").spread(1.1f));
        String[] camo = {"marine", "marineforest", "marineurban", "marineblack"};
        for (String c : camo)
        {
            tiers.add(new Tier("soldier_" + c, NpcKind.SOLDIER, 1, 1, 24, 0.8f,
                new String[][] {{c + "Helm", c + "Helm", c + "Hat"}, {c + "Vest"}, {c + "Pants"}, {c + "Boots"}},
                "m4a4", "m4a4", "m16a2", "g36c", "fnscarl", "famas", "l85a1", "acr", "m240", "m110").delay(2, 6).spread(1.0f));
        }
        tiers.add(new Tier("military", NpcKind.SOVIET, 1, 1, 40, 0.6f,
            new String[][] {{"spetsnazHelm"}, {"spetsnazVest"}, {"spetsnazPants"}, {"spetsnazBoots"}},
            "ak74", "ak74", "ak12", "aks74u", "rpk74", "asval", "pkp", "svd").delay(3, 9).spread(0.9f));
        tiers.get(tiers.size() - 1).weight = 9;
        tiers.get(tiers.size() - 1).militaryWeight = 9;
        tiers.add(new Tier("military_rpg", NpcKind.SOVIET, 1, 1, 40, 0.6f,
            new String[][] {{"spetsnazHelm"}, {"spetsnazVest"}, {"spetsnazPants"}, {"spetsnazBoots"}},
            "rpg18").spread(1.2f));
        // new tiers go last: a spawn egg's metadata is its index in this list
        tiers.add(new Tier("bandit_rpg", NpcKind.BANDIT, 3, 8, 30, 0.75f,
            new String[][] {{"militiaHelm", "banditHelm"}, {"militiaVest", "banditVest"}, {"militiaPants"},
                            {"militiaBoots"}},
            "rpg7").spread(1.6f));
        // juggernaut: never rolled (weights 0), only MilitarySpawner (juggernautChance) and its egg
        tiers.add(new Tier("juggernaut", NpcKind.SOVIET, 0, 0, 200, 0.25f,
            new String[][] {{"juggernautHelm", "juggernautHelmGray"}, {"juggernautVest", "juggernautVestGray"},
                            {"juggernautPants", "juggernautPantsGray"}, {"juggernautBoots", "juggernautBootsGray"}},
            "pkm", "pkp", "m240", "mk48").spread(1.0f).heavy(0.18, 1.0));
        // elite military (user 2026-10-09): marine black + night vision, heavy machine guns or sniper
        // rifles with every attachment, about 2 magazines to kill, x2 damage; only MilitarySpawner
        // (eliteChance) and its egg
        tiers.add(new Tier("elite_military", NpcKind.SOVIET, 0, 0, 150, 0.16f,
            new String[][] {{"marineblackHelm"}, {"marineblackVest"}, {"marineblackPants"}, {"marineblackBoots"}},
            "pkp", "m240", "mk48", "mg3", "pkm").spread(0.8f).delay(2, 6)
            .heavy(0.27, 0.5).elite(2.0f, "nvgoggles",
                new String[] {"sight=4x", "barrel=mgSuppressor", "grip=laser"},
                new String[] {"sight=8x", "barrel=arSuppressor", "grip=laser"}));
        // sniper versions (user 2026-10-09: "split eggs for sniper on juggernaut and elite, don't make it
        // rare"): MilitarySpawner picks them for sniperShare of its juggernauts / elites
        tiers.add(new Tier("juggernaut_sniper", NpcKind.SOVIET, 0, 0, 200, 0.25f,
            new String[][] {{"juggernautHelm", "juggernautHelmGray"}, {"juggernautVest", "juggernautVestGray"},
                            {"juggernautPants", "juggernautPantsGray"}, {"juggernautBoots", "juggernautBootsGray"}},
            "barrett").spread(1.0f).heavy(0.18, 1.0).elite(1.0f, null, new String[0], new String[] {"sight=8x"}));
        tiers.add(new Tier("elite_sniper", NpcKind.SOVIET, 0, 0, 150, 0.16f,
            new String[][] {{"marineblackHelm"}, {"marineblackVest"}, {"marineblackPants"}, {"marineblackBoots"}},
            "barrett", "barrett", "barrett", "barrett", "l115a3", "jng90", "sv98", "m110").spread(0.8f)
            .heavy(0.27, 0.5).elite(2.0f, "nvgoggles", new String[0],
                new String[] {"sight=8x", "barrel=arSuppressor", "grip=laser"}));

        load(new File(configDir, "deciworldgen_npc.cfg"));
    }

    private void load(File file)
    {
        Configuration cfg = new Configuration(file, "2");
        // v0.30.4 added rocket tiers and re-balanced the military weight (1 -> 9 against military_rpg 1):
        // an older file keeps its weights otherwise, so take the new defaults once
        boolean oldFile = !"2".equals(cfg.getLoadedConfigVersion());
        for (Tier t : tiers)
        {
            String cat = "tier_" + t.name;
            if (oldFile && cfg.hasCategory(cat))
            {
                cfg.getCategory(cat).remove("weight");
                cfg.getCategory(cat).remove("militaryWeight");
            }
            t.health = cfg.getFloat("health", cat, t.health, 1, 1000, "max health (vanilla player 20)");
            t.taken = cfg.getFloat("damageTaken", cat, t.taken, 0.01f, 1, "share of a player's gun damage it takes");
            t.weight = cfg.getInt("weight", cat, t.weight, 0, 1000, "chance against the other tiers of its kind");
            t.militaryWeight = cfg.getInt("militaryWeight", cat, t.militaryWeight, 0, 1000,
                                          "the same inside military areas");
            t.damageDealt = cfg.getFloat("damageDealt", cat, t.damageDealt, 0, 100,
                "its gun hits on a player x this (on top of npcDamageToPlayer)");
            t.spread = cfg.getFloat("spread", cat, t.spread, 0, 45,
                "aim spread in degrees: about 85% hits at 10 blocks with 1.2, 50% at 20 (machine guns x1.4)");
        }
        npcDamageToPlayer = cfg.getFloat("npcDamageToPlayer", "player", npcDamageToPlayer, 0, 100,
            "NPC gun hits on a player: damage x this, after armor (user 2026-10-09: 5, hardcore; 1 = Decimation)");
        // v0.30.2 / 0.30.3 keys; user 2026-10-09: 0.25 s (v0.30.4)
        cfg.getCategory("player").remove("everyNpcHitCounts");
        cfg.getCategory("player").remove("npcHitsSkipCooldown");
        autoFire = cfg.getBoolean("autoFire", "npc_fire", autoFire,
            "automatic guns fire bursts (3..6 rounds, machine guns 6..12) at the gun's own rate of fire");
        recoilSpread = cfg.getFloat("recoilSpread", "npc_fire", recoilSpread, 0, 5,
            "each shot of a burst spreads this much more than the first (0.35: the 6th shot 2.75x)");
        reloadTicks = cfg.getInt("reloadTicks", "npc_fire", reloadTicks, 0, 1200,
            "an NPC fires its gun's magazine (M16 30, PKM 250), then reloads this many ticks (20 = 1 s)");
        hitCooldown = cfg.getInt("npcHitCooldownTicks", "player", hitCooldown, 0, 10,
            "after a hit, NPC gun hits are ignored for this many ticks (20 = 1 s): 10 = vanilla's 0.5 s, "
            + "5 = 0.25 s, 0 = every hit of a group lands");
        cfg.save();
    }

    public float npcDamageToPlayer()
    {
        return npcDamageToPlayer;
    }

    public boolean autoFire()
    {
        return autoFire;
    }

    public float recoilSpread()
    {
        return recoilSpread;
    }

    public int reloadTicks()
    {
        return reloadTicks;
    }

    /** Ticks between single shots of this tier with this gun (min, max). */
    static int[] shotDelay(Tier tier, String gunName)
    {
        if (contains(MG, gunName))
        {
            return new int[] {3, 8};
        }
        if (contains(ROCKET, gunName))
        {
            return new int[] {60, 100}; // one rocket, then a reload
        }
        if (contains(SNIPER, gunName))
        {
            return new int[] {25, 45};
        }
        return tier == null ? new int[] {4, 12} : new int[] {tier.delayMin, tier.delayMax};
    }

    /** Ticks an NPC of this tier waits after a burst. */
    static int[] burstPause(Tier t)
    {
        return t == null ? new int[] {15, 40} : new int[] {Math.max(10, t.delayMin * 3), Math.max(20, t.delayMax * 3)};
    }

    public int npcHitCooldownTicks()
    {
        return hitCooldown;
    }

    /** Aim spread in degrees of an NPC with this gun (its tier's, adjusted for machine guns / sniper rifles). */
    public float spread(Entity npc, ItemStack gun)
    {
        String name = gun == null ? "" : GameRegistry.findUniqueIdentifierFor(gun.getItem()).name;
        if (contains(SNIPER, name))
        {
            return SNIPER_SPREAD;
        }
        Tier t = byName(npc.getEntityData().getString(TAG));
        float s = t == null ? UNTIERED_SPREAD : t.spread;
        return contains(MG, name) ? s * MG_SPREAD : s;
    }

    /**
     * Vanilla drops a hit while the victim's hit cooldown (hurtResistantTime,
     * 20 right after a hit, counting down) is above half (10 ticks = 0.5 s)
     * and the hit is not bigger than the last one; NPC shots never reset it,
     * so a group lands at most 2 hits a second. Here an NPC hit counts again
     * once hitCooldown ticks have passed: the timer is cleared before the check.
     */
    @SubscribeEvent
    public void onAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event)
    {
        EntityLivingBase v = event.entityLiving;
        if (hitCooldown < 10 && !v.worldObj.isRemote && v instanceof net.minecraft.entity.player.EntityPlayer
            && "human".equals(event.source.getDamageType()) && v.hurtResistantTime <= v.maxHurtResistantTime - hitCooldown)
        {
            v.hurtResistantTime = 0;
        }
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
        Item g = item(gunName(gun));
        if (g != null)
        {
            arm(npc, tier, gun, g);
        }
    }

    /** The gun's registry name in a spec ("pkm;sight=4x;..."). */
    public static String gunName(String spec)
    {
        int i = spec.indexOf(';');
        return i < 0 ? spec : spec.substring(0, i);
    }

    /** The gun of a spec with its attachments (Decimation's NBT keys sightAttach, barrelAttach, ...). */
    public static ItemStack gunStack(String spec)
    {
        Item g = item(gunName(spec));
        if (g == null)
        {
            return null;
        }
        ItemStack stack = new ItemStack(g);
        for (String part : spec.split(";"))
        {
            int eq = part.indexOf('=');
            if (eq > 0 && !part.startsWith("mask="))
            {
                if (stack.stackTagCompound == null)
                {
                    stack.stackTagCompound = new NBTTagCompound();
                }
                stack.stackTagCompound.setString(part.substring(0, eq) + "Attach", part.substring(eq + 1));
            }
        }
        return stack;
    }

    /** Gun (with attachments) and mask of a spec onto an NPC, either side. */
    static void applySpec(Entity npc, String spec)
    {
        ItemStack gun = gunStack(spec);
        if (gun != null)
        {
            Deci.setNpcGun(npc, gun);
        }
        for (String part : spec.split(";"))
        {
            if (part.startsWith("mask=") && item(part.substring(5)) != null)
            {
                Deci.setNpcMask(npc, new ItemStack(item(part.substring(5))));
            }
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
        Item g = item(gunName(name));
        if (g != null && (held == null || held.getItem() != g))
        {
            applySpec(e, name);
        }
    }

    /**
     * A player's gun ("gunDeci") hitting a tiered NPC: the tier's share of the
     * damage. An NPC's gun ("human") hitting a player: x npcDamageToPlayer
     * (armor is applied by ArmorGunfireHandler; the factors multiply).
     */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event)
    {
        if (event.entityLiving.worldObj.isRemote)
        {
            return;
        }
        if ("human".equals(event.source.getDamageType())
            && event.entityLiving instanceof net.minecraft.entity.player.EntityPlayer)
        {
            event.ammount *= npcDamageToPlayer * NpcShots.damageScale;
            return;
        }
        if (!"gunDeci".equals(event.source.getDamageType()))
        {
            return;
        }
        Tier tier = byName(event.entityLiving.getEntityData().getString(TAG));
        if (tier != null)
        {
            event.ammount *= tier.taken;
        }
    }

    /** Tier by position in the list (spawn egg metadata), or null. */
    public Tier byIndex(int i)
    {
        return i >= 0 && i < tiers.size() ? tiers.get(i) : null;
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
        int set = random.nextInt(tier.armor[0].length);
        for (int slot = 0; slot < 4; slot++)
        {
            String[] pool = tier.armor[slot];
            Item piece = item(pool[tier.matchedSet ? set % pool.length : random.nextInt(pool.length)]);
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
        // the gun spec "name;sight=4x;barrel=...;mask=nvgoggles" (gunStack / applySpec read it)
        String[] kit = contains(MG, gun) ? tier.mgKit : contains(SNIPER, gun) ? tier.sniperKit : tier.rifleKit;
        StringBuilder spec = new StringBuilder(gun);
        for (String k : kit)
        {
            spec.append(';').append(k);
        }
        if (tier.mask != null)
        {
            spec.append(";mask=").append(tier.mask);
        }
        gun = spec.toString();
        npc.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(tier.health);
        if (tier.speed > 0)
        {
            npc.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(tier.speed);
        }
        if (tier.knockbackResistance > 0)
        {
            npc.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(tier.knockbackResistance);
        }
        npc.setHealth(tier.health);
        data.setString(TAG, tier.name);
        data.setString(GUN_TAG, gun);
    }

    /** Gun in hand (server field + watcher for the client) and a fire rate that fits it. */
    private static void arm(EntityLiving npc, Tier tier, String spec, Item gun)
    {
        applySpec(npc, spec);
        npc.getDataWatcher().updateObject(GUN_SLOT, spec);
        int[] d = shotDelay(tier, gunName(spec));
        Deci.setNpcShotDelay(npc, d[0], d[1]);
    }

    static boolean contains(String[] list, String s)
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
