package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * NPC and zombie benchmark (user 19 Oktober 2026: "do benchmark ... when they standby, shooting, wars and test out on
 * zombie too. make it really really stable even there so many entities"). Scenarios on the arena at night, each
 * run -Prepeat (2) times with mob spawning off and everything else cleared within 96 blocks first, -Psettle (3) seconds to settle and -Pmeasure (8) seconds measured, cleared in between: standby (40 idle armed NPCs, no
 * enemy in range), shooting (20 soldiers, infected keep coming), war (20 soldiers against 20 bandits, 30 blocks apart),
 * zombies (a horde of 80 around a survival player who takes no damage), crowd (all of it at once), loaded (30
 * civilians near and 1500 items 40 to 90 blocks away: a world full of entities). Per scenario: the
 * server's mean and worst milliseconds a tick, the client's median and slowest 1 % frame, entities on the server, and the
 * wall clock start / end (to cut a JFR recording of the run into scenarios). -Ponly=war,zombies runs some.
 */
public class BenchTest extends DevTestMode
{
    public String name() { return "bench"; }

    private static final String[] ALL = {"standby", "shooting", "war", "zombies", "crowd", "loaded"};
    private final int settle = Integer.getInteger("deciworldgen.autotest.settle", 3) * 20;
    private final int measure = Integer.getInteger("deciworldgen.autotest.measure", 8) * 20;
    private final List<String> scenarios = new ArrayList<String>();
    private static final List<Long> frames = new ArrayList<Long>();
    private static volatile long last;
    private static volatile boolean recording, listening;

    private volatile int index = -1, step; // step 0 set up (server), 1 running, 2 cleared
    private volatile boolean ready;
    private int ticks, serverTicks;
    private long wallStart;
    private double worstTick;
    private final List<Entity> spawned = new ArrayList<Entity>();
    private String spawning;

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

    public BenchTest()
    {
        String only = System.getProperty("deciworldgen.autotest.only", "");
        int repeat = Integer.getInteger("deciworldgen.autotest.repeat", 2);
        for (int r = 0; r < repeat; r++)
        {
            for (String s : ALL)
            {
                if (only.isEmpty() || Arrays.asList(only.split(",")).contains(s))
                {
                    scenarios.add(s);
                }
            }
        }
    }

    public boolean client(Minecraft mc)
    {
        if (!listening)
        {
            FMLCommonHandler.instance().bus().register(new Clock());
            listening = true;
        }
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (index < 0)
        {
            index = 0;
            step = 0;
            ready = false;
            return true;
        }
        if (index >= scenarios.size())
        {
            return false;
        }
        if (!ready)
        {
            return true; // the server sets the scenario up
        }
        ticks++;
        recording = ticks > settle;
        if (ticks == settle + 1)
        {
            wallStart = System.currentTimeMillis();
        }
        if (ticks < settle + measure)
        {
            return true;
        }
        recording = false;
        double[] f = frameStats();
        double[] s = serverStats();
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        DevTestResults.value(name(), scenarios.get(index), String.format(
            "server mean %.2f ms, worst %.1f ms a tick; client median %.1f ms, slowest 1%% %.1f ms; server entities %d, client %d;"
            + " wall %d..%d", s[0], Math.max(s[1], worstTick), f[0], f[1], w.loadedEntityList.size(), mc.theWorld.loadedEntityList.size(),
            wallStart, System.currentTimeMillis()));
        DevTestResults.check(name(), scenarios.get(index) + ": the server keeps 20 ticks a second", String.format("%.1f ms a tick", s[0]),
                             s[0] < 50, "under 50 ms a tick on average");
        ticks = 0;
        ready = false;
        step = 2; // the server clears it
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
        if (p == null || index < 0)
        {
            return;
        }
        if (index >= scenarios.size())
        {
            if (spawning != null)
            {
                w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning); // as the world had it
                spawning = null;
            }
            return;
        }
        if (spawning == null)
        {
            spawning = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false"); // no stray mobs: runs compare (also our spawners)
        }
        String s = scenarios.get(index);
        if (step == 2)
        {
            for (Entity e : spawned)
            {
                e.setDead();
            }
            spawned.clear();
            if (index + 1 >= scenarios.size() && spawning != null)
            {
                w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning); // the last one: as the world had it
                spawning = null;
            }
            index++;
            step = 0;
            return;
        }
        if (step == 0)
        {
            clearAround(w, 96); // everything but players within 96 blocks: the same start for every run
            DevTestArena.build(w, p); // clean floor, no leftovers (also corpses and items of the last scenario)
            w.setWorldTime(18000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            boolean survival = s.equals("zombies") || s.equals("crowd");
            p.setGameType(survival ? WorldSettings.GameType.SURVIVAL : WorldSettings.GameType.CREATIVE);
            p.capabilities.disableDamage = true;
            p.sendPlayerAbilities();
            if (s.equals("standby") || s.equals("crowd"))
            {
                ring(w, "Soldier", 14, 0, 0, 6);
                ring(w, "TraderGuard", 13, 0, 0, 9);
                ring(w, "Human", 13, 0, 0, 12);
            }
            if (s.equals("shooting") || s.equals("crowd"))
            {
                ring(w, "Soldier", 20, -30, 0, 4);
            }
            if (s.equals("war") || s.equals("crowd"))
            {
                ring(w, "Soldier", 20, -15, 0, 4);
                ring(w, "Bandit", 20, 15, 0, 4);
            }
            if (s.equals("loaded"))
            {
                // a world with many entities: 30 civilians near, 1500 items 40 to 90 blocks away (every human NPC walked
                // the whole loaded entity list every tick; bug.md "Every human NPC walked all entities")
                ring(w, "Human", 30, 0, 0, 8);
                java.util.Random r = new java.util.Random(7);
                for (int i = 0; i < 1500; i++)
                {
                    double a = r.nextDouble() * Math.PI * 2, d = 40 + r.nextDouble() * 50;
                    net.minecraft.entity.item.EntityItem it = new net.minecraft.entity.item.EntityItem(w, DevTestArena.X + Math.cos(a) * d,
                        DevTestArena.Y + 2, DevTestArena.Z + Math.sin(a) * d, new net.minecraft.item.ItemStack(net.minecraft.init.Items.stick));
                    it.delayBeforeCanPickup = 32767;
                    it.lifespan = 72000;
                    if (w.spawnEntityInWorld(it))
                    {
                        spawned.add(it);
                    }
                }
            }
            if (s.equals("zombies") || s.equals("crowd"))
            {
                for (int i = 0; i < 80; i++)
                {
                    double a = i * Math.PI * 2 / 80, r = 14 + (i % 4) * 3;
                    infected(w, DevTestArena.X + 0.5 + Math.cos(a) * r, DevTestArena.Z + 0.5 + Math.sin(a) * r);
                }
            }
            serverTicks = 0;
            worstTick = 0;
            step = 1;
            ready = true;
            return;
        }
        serverTicks++;
        if (serverTicks > settle)
        {
            long[] t = MinecraftServer.getServer().tickTimeArray;
            worstTick = Math.max(worstTick, t[MinecraftServer.getServer().getTickCounter() % 100] / 1e6);
        }
        if ((s.equals("shooting") || s.equals("crowd")) && serverTicks % 20 == 0)
        {
            for (int i = 0; i < 3; i++) // infected keep coming, 25 blocks from the soldiers
            {
                infected(w, DevTestArena.X - 5 + 0.5, DevTestArena.Z - 9 + i * 9 + 0.5);
            }
        }
    }

    private static void clearAround(WorldServer w, double r)
    {
        for (Object o : w.loadedEntityList.toArray())
        {
            Entity e = (Entity) o;
            if (!(e instanceof net.minecraft.entity.player.EntityPlayer) && Math.abs(e.posX - DevTestArena.X) < r
                && Math.abs(e.posZ - DevTestArena.Z) < r)
            {
                e.setDead();
            }
        }
    }

    /** n NPCs of a kind on a ring of radius r around (X + dx, Z + dz). */
    private void ring(WorldServer w, String kind, int n, int dx, int dz, double r)
    {
        for (int i = 0; i < n; i++)
        {
            double a = i * Math.PI * 2 / n;
            for (int tries = 0; tries < 5; tries++) // Decimation turns some spawns into other mobs: retry
            {
                Entity e = EntityList.createEntityByName(kind, w);
                if (!(e instanceof EntityLiving))
                {
                    break;
                }
                e.setLocationAndAngles(DevTestArena.X + dx + 0.5 + Math.cos(a) * r, DevTestArena.Y,
                                       DevTestArena.Z + dz + 0.5 + Math.sin(a) * r, (float) (a * 57.3), 0);
                if (w.spawnEntityInWorld(e))
                {
                    spawned.add(e);
                    break;
                }
            }
        }
    }

    private void infected(WorldServer w, double x, double z)
    {
        for (int tries = 0; tries < 5; tries++)
        {
            EntityLiving e = Deci.newInfected(w);
            e.setLocationAndAngles(x, DevTestArena.Y, z, 0, 0);
            if (w.spawnEntityInWorld(e))
            {
                spawned.add(e);
                return;
            }
        }
    }

    /** median, slowest 1 % (ms). */
    private static double[] frameStats()
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
            return new double[] {0, 0};
        }
        Arrays.sort(s);
        return new double[] {s[s.length / 2] / 1e6, s[Math.min(s.length - 1, (int) (s.length * 0.99))] / 1e6};
    }

    /** mean and worst of the server's last 100 ticks (ms). */
    private static double[] serverStats()
    {
        long[] t = MinecraftServer.getServer().tickTimeArray;
        long sum = 0, max = 0;
        for (long x : t)
        {
            sum += x;
            max = Math.max(max, x);
        }
        return new double[] {sum / (double) t.length / 1e6, max / 1e6};
    }
}
