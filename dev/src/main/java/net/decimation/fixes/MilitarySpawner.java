package net.decimation.fixes;

import java.io.File;
import java.util.Random;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Configuration;

/**
 * Enemy military (Decimation's Soviets, hostile to every player; tier
 * "military" in NpcLoadouts) in military areas: every INTERVAL ticks, for
 * each player standing in a military sector, a group of 2 to 3 may spawn
 * 24 to 48 blocks away on open ground, as long as fewer than CAP of them
 * are within 64 blocks. Never in peaceful (Decimation's humans are mobs).
 * Settings: config/deciworldgen_npc.cfg, category "military_spawner".
 */
public class MilitarySpawner
{
    private final boolean enabled;
    private final int interval, cap;
    private final float chance;
    private final Random random = new Random();
    private int ticks;

    public MilitarySpawner(File configDir)
    {
        Configuration cfg = new Configuration(new File(configDir, "deciworldgen_npc.cfg"));
        String cat = "military_spawner";
        enabled = cfg.getBoolean("enabled", cat, true, "enemy military groups in military sectors");
        interval = cfg.getInt("interval", cat, 400, 20, 72000, "ticks between tries per player (20 = 1 s)");
        chance = cfg.getFloat("chance", cat, 0.5f, 0, 1, "chance a try spawns a group");
        cap = cfg.getInt("cap", cat, 4, 0, 64, "no new group while this many are within 64 blocks");
        cfg.save();
    }

    public boolean enabled()
    {
        return enabled;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || ++ticks % interval != 0)
        {
            return;
        }
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        if (world == null || world.difficultySetting == EnumDifficulty.PEACEFUL)
        {
            return;
        }
        for (Object o : world.playerEntities)
        {
            EntityPlayer player = (EntityPlayer) o;
            if (NpcLoadouts.military(world, player.posX, player.posZ) && random.nextFloat() < chance
                && nearby(world, player) < cap)
            {
                spawnGroup(world, player);
            }
        }
    }

    /** Enemy military within 64 blocks of the player. */
    static int nearby(WorldServer world, EntityPlayer player)
    {
        int n = 0;
        for (Object o : world.getEntitiesWithinAABB(EntityLiving.class,
            AxisAlignedBB.getBoundingBox(player.posX - 64, 0, player.posZ - 64, player.posX + 64, 256, player.posZ + 64)))
        {
            n += Deci.npcKind((EntityLiving) o) == NpcKind.SOVIET ? 1 : 0;
        }
        return n;
    }

    /** A group of 2 to 3 at one open spot 24 to 48 blocks from the player; false when no spot was found. */
    public boolean spawnGroup(WorldServer world, EntityPlayer player)
    {
        for (int attempt = 0; attempt < 8; attempt++)
        {
            double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextDouble() * 24;
            int x = (int) Math.floor(player.posX + Math.cos(a) * d), z = (int) Math.floor(player.posZ + Math.sin(a) * d);
            if (!world.blockExists(x, 64, z))
            {
                continue;
            }
            int y = world.getTopSolidOrLiquidBlock(x, z);
            if (world.getBlock(x, y - 1, z).getMaterial().isLiquid() || !world.isAirBlock(x, y, z)
                || !world.isAirBlock(x, y + 1, z) || world.getClosestPlayer(x, y, z, 20) != null)
            {
                continue;
            }
            int size = 2 + random.nextInt(2);
            for (int i = 0; i < size; i++)
            {
                EntityLiving npc = Deci.newSoviet(world);
                npc.setLocationAndAngles(x + 0.5 + random.nextInt(3) - 1, y, z + 0.5 + random.nextInt(3) - 1,
                                         random.nextFloat() * 360, 0);
                world.spawnEntityInWorld(npc);
            }
            FMLLog.info("[deciworldgen] enemy military group of %d at %d,%d,%d", size, x, y, z);
            return true;
        }
        return false;
    }
}
