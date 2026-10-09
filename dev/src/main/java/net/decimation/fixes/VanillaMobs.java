package net.decimation.fixes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * No vanilla mobs in the overworld (user request 2026-10-09: "remove vanilla
 * mobs since this is a zombie mod"). Decimation already strips vanilla
 * monsters from the biomes that exist in its init, but vanilla animals,
 * squid, bats, villages, dungeon spawners and old saves still bring them.
 *
 * Two layers, overworld only (the Nether keeps its mobs):
 * - removeFromBiomes (postInit, after DecimationBiomes.copySpawns): their
 *   spawn entries leave every biome except Hell and Sky;
 * - onJoin: anything of those classes joining dimension 0 is refused
 *   (spawners, villages, spawn eggs, breeding, mobs saved in old chunks).
 * Exact class match, so mod mobs that extend a vanilla class (Decimation's
 * infected, boar, buck, dog) stay. Config deciworldgen_mobs.cfg: switch it
 * off, or keep some by name (Horse, Wolf ...). Dev tests tag the entities
 * they spawn on purpose with KEEP.
 */
public class VanillaMobs
{
    public static final String KEEP = "deciworldgen_keep";
    private static final Class<?>[] VANILLA = {
        EntityZombie.class, EntitySkeleton.class, EntityCreeper.class, EntitySpider.class, EntityCaveSpider.class,
        EntityEnderman.class, EntitySlime.class, EntityWitch.class, EntitySilverfish.class, EntityPigZombie.class,
        EntityPig.class, EntityCow.class, EntitySheep.class, EntityChicken.class, EntityHorse.class, EntityWolf.class,
        EntityOcelot.class, EntityMooshroom.class, EntitySquid.class, EntityBat.class, EntityVillager.class,
        EntityIronGolem.class, EntitySnowman.class};

    private final boolean enabled;
    private final Set<Class<?>> removed = new HashSet<Class<?>>();

    public VanillaMobs(File configDir)
    {
        Configuration cfg = new Configuration(new File(configDir, "deciworldgen_mobs.cfg"));
        enabled = cfg.getBoolean("removeVanillaMobs", "vanilla", true,
            "no vanilla mobs or animals in the overworld (Decimation's own mobs and the Nether are not touched)");
        Set<String> keep = new HashSet<String>(Arrays.asList(cfg.getStringList("keep", "vanilla", new String[0],
            "vanilla mobs to keep anyway, by entity name: Horse, Wolf, Pig, Cow, Sheep, Chicken, Squid, Bat, "
            + "Villager, Zombie, Skeleton, Creeper, Spider ...")));
        cfg.save();
        for (Class<?> c : VANILLA)
        {
            if (!keep.contains(EntityList.classToStringMapping.get(c)))
            {
                removed.add(c);
            }
        }
    }

    public boolean enabled()
    {
        return enabled;
    }

    /** True when this entity is a removed vanilla mob (exact class). */
    public boolean removed(Entity e)
    {
        return removed.contains(e.getClass());
    }

    /** Takes the removed classes out of every biome's spawn lists but Hell and Sky. */
    public void removeFromBiomes()
    {
        int n = 0;
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray())
        {
            if (b == null || b == BiomeGenBase.hell || b == BiomeGenBase.sky)
            {
                continue;
            }
            for (EnumCreatureType type : EnumCreatureType.values())
            {
                List<BiomeGenBase.SpawnListEntry> list = b.getSpawnableList(type);
                if (list != null)
                {
                    int before = list.size();
                    list.removeIf(entry -> removed.contains(entry.entityClass));
                    n += before - list.size();
                }
            }
        }
        FMLLog.info("[deciworldgen] removed %d vanilla spawn entries from the overworld biomes", n);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event)
    {
        if (event.world.isRemote || event.world.provider.dimensionId != 0 || !removed(event.entity)
            || event.entity.getEntityData().getBoolean(KEEP))
        {
            return;
        }
        event.setCanceled(true);
        event.entity.setDead(); // a mob from an old chunk stays in the chunk's list: dead ones are not saved
    }
}
