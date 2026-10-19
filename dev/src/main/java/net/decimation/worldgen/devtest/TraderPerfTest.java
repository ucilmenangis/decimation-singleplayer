package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * Safezones lag (bug.md, 19 Oktober 2026): every armed trader and trader guard looked for a target every tick, on the
 * server and on each client's copy, ray tracing to every living thing within 32 blocks before asking whether it is
 * hostile. 20 trader guards stand around the player on the arena: the server's milliseconds per tick and the client's
 * frame time with them, against the same spot without them (-Pphase seconds each, default 6).
 */
public class TraderPerfTest extends DevTestMode
{
    public String name() { return "traderperf"; }

    private static final int COUNT = 20;
    private final String npc = System.getProperty("deciworldgen.autotest.npc", "TraderGuard");
    private final int phaseTicks = Integer.getInteger("deciworldgen.autotest.phase", 6) * 20;
    private static final List<Long> frames = new ArrayList<Long>();
    private static volatile long last;
    private static volatile boolean recording, listening;
    private volatile int phase; // 0 without, 1 with, 2 an infected comes, 3 done
    private volatile EntityLiving infected;
    private volatile int infectedTicks;
    private float infectedHealth;
    private volatile boolean spawned, cleared, built;
    private int ticks;
    private final double[] server = new double[2], client = new double[2];
    private final List<Entity> guards = new ArrayList<Entity>();

    public static final class Clock
    {
        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START)
            {
                return;
            }
            long now = System.nanoTime();
            if (recording && last != 0)
            {
                synchronized (frames)
                {
                    frames.add(now - last);
                }
            }
            last = now;
        }
    }

    public boolean client(Minecraft mc)
    {
        if (!listening)
        {
            FMLCommonHandler.instance().bus().register(new Clock());
            listening = true;
        }
        if (mc.thePlayer == null || !built || (phase == 1 && !spawned))
        {
            return true;
        }
        if (phase >= 2)
        {
            return !cleared;
        }
        ticks++;
        recording = ticks > 40; // 2 s to settle
        if (ticks < phaseTicks + 40)
        {
            return true;
        }
        recording = false;
        client[phase] = medianMs();
        server[phase] = serverTickMs();
        DevTestResults.value(name(), phase == 0 ? "without traders" : COUNT + " " + npc + " near",
                             String.format("client median %.2f ms a frame, server %.2f ms a tick", client[phase], server[phase]));
        if (phase == 1)
        {
            double s = server[1] - server[0], c = client[1] - client[0];
            DevTestResults.check(name(), COUNT + " " + npc + " cost the server", String.format("%+.2f ms a tick", s), s < 0.5,
                                 "under 0.5 ms a tick");
            DevTestResults.value(name(), COUNT + " " + npc + " cost the client", String.format("%+.2f ms a frame", c));
        }
        phase++;
        ticks = 0;
        synchronized (frames)
        {
            frames.clear();
        }
        return true;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null)
        {
            return;
        }
        if (!built)
        {
            DevTestArena.build(w, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = true;
            p.sendPlayerAbilities();
            built = true;
        }
        if (phase == 1 && !spawned)
        {
            for (int i = 0; i < COUNT; i++)
            {
                Entity e = EntityList.createEntityByName(npc, w);
                if (e instanceof EntityLiving)
                {
                    e.setLocationAndAngles(DevTestArena.X - 6 + (i % 5) * 3 + 0.5, DevTestArena.Y, DevTestArena.Z - 6 + (i / 5) * 3 + 0.5, 0, 0);
                    if (w.spawnEntityInWorld(e))
                    {
                        guards.add(e);
                    }
                }
            }
            DevTestResults.value(name(), npc + " spawned", guards.size());
            spawned = true;
        }
        if (phase == 2)
        {
            // the guards still defend: an infected at night, 8 blocks from them, must be shot within 5 s
            if (infected == null)
            {
                w.setWorldTime(18000);
                EntityLiving z = net.decimation.fixes.Deci.newInfected(w);
                z.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 14.5, 0, 0);
                if (w.spawnEntityInWorld(z))
                {
                    infected = z;
                    infectedHealth = z.getHealth();
                }
                return;
            }
            infectedTicks++;
            boolean hit = infected.isDead || infected.getHealth() < infectedHealth;
            if (hit || infectedTicks >= 100)
            {
                DevTestResults.check(name(), "the " + npc + " shoot an infected that comes near", hit ? "shot after " + infectedTicks
                                     + " ticks" : "not shot in 5 s", hit, "shot within 5 s");
                infected.setDead();
                for (Entity e : guards)
                {
                    e.setDead();
                }
                cleared = true;
                phase = 3;
            }
        }
    }

    private static double serverTickMs()
    {
        long[] t = MinecraftServer.getServer().tickTimeArray;
        long sum = 0;
        for (long x : t)
        {
            sum += x;
        }
        return sum / (double) t.length / 1e6;
    }

    private static double medianMs()
    {
        long[] s;
        synchronized (frames)
        {
            s = new long[frames.size()];
            for (int i = 0; i < s.length; i++)
            {
                s[i] = frames.get(i);
            }
        }
        if (s.length == 0)
        {
            return 0;
        }
        Arrays.sort(s);
        return s[s.length / 2] / 1e6;
    }
}
