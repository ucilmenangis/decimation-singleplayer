package net.decimation.fixes;

import java.io.File;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Kill and death counters kept on this computer (user request 11 Oktober 2026: "stats on hud
 * replace with local kills", "players kill change to humans kill").
 *
 * Decimation's main menu and in game HUD read the player's stats from a profile its dead backend
 * server used to send (BackendProfileCache, always empty now: 0 / 0 / 0), and its bottom line
 * shows the server's global casualty totals. Here every kill and death in singleplayer is
 * counted per player UUID across all worlds in config/deciworldgen_stats.cfg:
 * - human kills: players and every human NPC (bandits, soldiers, Soviets, hazmat soldiers,
 *   civilians, traders);
 * - infected kills: every infected, including the special ones and infected dogs;
 * - deaths.
 * The client puts the local player's numbers into that profile and the totals line once a
 * second, so every screen that reads them shows them (labels renamed by
 * tools/patches/PatchMenuStats.java: Human Kills, Total Human Kills, Total Infected Kills).
 */
public final class LocalStats
{
    private static Configuration cfg;
    private int tick;

    public LocalStats(File configDir)
    {
        cfg = new Configuration(new File(configDir, "deciworldgen_stats.cfg"));
        cfg.load();
    }

    private static synchronized long get(String uuid, String key)
    {
        try
        {
            return Long.parseLong(cfg.get(uuid, key, "0").getString().trim());   // 1.7.10 config has no long
        }
        catch (NumberFormatException e)
        {
            return 0L;
        }
    }

    private static synchronized void add(String uuid, String key)
    {
        cfg.get(uuid, key, "0").set(String.valueOf(get(uuid, key) + 1));
        cfg.save();
    }

    /** The integrated server: counts kills of and deaths of players. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event)
    {
        if (event.entityLiving.worldObj.isRemote || FMLCommonHandler.instance().getSide().isServer())
        {
            return;              // singleplayer / LAN host only: a dedicated server has its own backend
        }
        if (event.entityLiving instanceof EntityPlayer)
        {
            add(((EntityPlayer) event.entityLiving).getUniqueID().toString(), "deaths");
        }
        Entity killer = event.source.getEntity();
        if (killer instanceof EntityPlayer && killer != event.entityLiving)
        {
            String uuid = ((EntityPlayer) killer).getUniqueID().toString();
            if (Deci.isHuman(event.entityLiving))
            {
                add(uuid, "humanKills");
            }
            else if (Deci.isInfectedKind(event.entityLiving))
            {
                add(uuid, "infectedKills");
            }
        }
    }

    /** The client: the local player's numbers into the menu / HUD profile, once a second. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && tick++ % 20 == 0)
        {
            show();
        }
    }

    /** {human kills, deaths, infected kills} of a player UUID (dev test stats). */
    public static long[] counts(String uuid)
    {
        return new long[] {get(uuid, "humanKills"), get(uuid, "deaths"), get(uuid, "infectedKills")};
    }

    /** Puts a player's counters back (the dev test's own kills must not stay in the user's stats). */
    public static synchronized void restore(String uuid, long[] c)
    {
        cfg.get(uuid, "humanKills", "0").set(String.valueOf(c[0]));
        cfg.get(uuid, "deaths", "0").set(String.valueOf(c[1]));
        cfg.get(uuid, "infectedKills", "0").set(String.valueOf(c[2]));
        cfg.save();
        show();
    }

    public static void show()
    {
        String uuid = Deci.sessionUuid();
        if (uuid == null || cfg == null)
        {
            return;
        }
        long human = get(uuid, "humanKills"), infected = get(uuid, "infectedKills");
        Deci.setLocalProfile(uuid, human, get(uuid, "deaths"), infected);
        Deci.setStatTotals(human, infected);
    }
}
