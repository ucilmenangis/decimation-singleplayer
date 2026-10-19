package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.decimation.fixes.Deci;
import net.decimation.worldgen.ZoneKind;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * Safezone lag (user's friend, 19 Oktober 2026: "when in safezone its really laggy, i think its caused by traders or
 * npc"): run `refuge` (or `camp`) first, it leaves its site populated. Then -Pphase seconds (default 10) in the middle of
 * the nearest test safezone and as long 200 blocks away at the same height: client frame times (median, slowest 1 %),
 * the server's milliseconds per tick, and the entities within 48 blocks by kind. A safezone that costs a lot shows as
 * the difference between the two.
 */
public class ZonePerfTest extends DevTestMode
{
    public String name() { return "zoneperf"; }

    private final int phaseTicks = Integer.getInteger("deciworldgen.autotest.phase", 10) * 20;
    private static final List<Long> frames = new ArrayList<Long>();
    private static volatile long last;
    private static volatile boolean recording, listening;
    private volatile int phase = -1; // 0 inside, 1 away
    private volatile double cx = Double.NaN, cy, cz;
    private int ticks;
    private final double[] serverMs = new double[2];
    private final double[][] client = new double[2][];
    private volatile boolean moved;
    private final int settle = Integer.getInteger("deciworldgen.autotest.settle", 3) * 20;
    private int updates, seconds;

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
        if (mc.thePlayer == null || Double.isNaN(cx) || !moved)
        {
            return true;
        }
        ticks++;
        recording = ticks > settle; // to settle (chunks, residents in view)
        if (recording && ticks % 20 == 0)
        {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) chunk updates").matcher(String.valueOf(mc.debug));
            if (m.find())
            {
                updates += Integer.parseInt(m.group(1));
                seconds++;
            }
        }
        if (ticks < phaseTicks + settle)
        {
            return true;
        }
        recording = false;
        client[phase] = summary();
        serverMs[phase] = serverTickMs();
        DevTestResults.value(name(), phase == 0 ? "inside the safezone" : "200 blocks away", String.format(
            "client median %.1f ms, slowest 1%% %.1f ms; server %.2f ms a tick; chunk updates %.1f a second", client[phase][0], client[phase][1],
            serverMs[phase], seconds == 0 ? 0 : updates / (double) seconds));
        updates = 0;
        seconds = 0;
        if (phase == 1)
        {
            DevTestResults.value(name(), "safezone costs the client", String.format("%+.1f ms a frame (median)", client[0][0] - client[1][0]));
            DevTestResults.value(name(), "safezone costs the server", String.format("%+.2f ms a tick", serverMs[0] - serverMs[1]));
            return false;
        }
        phase = 1;
        moved = false;
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
        if (Double.isNaN(cx))
        {
            double[] c = null;
            double best = 2000 * 2000;
            for (int[] b : Deci.zoneBoxes(ZoneKind.SAFEZONE)) // the camp / refuge test sites are near -20000, -19700
            {
                double x = (b[0] + b[2]) / 2.0, z = (b[1] + b[3]) / 2.0, d = (x + 20000) * (x + 20000) + (z + 19700) * (z + 19700);
                if (d < best)
                {
                    best = d;
                    c = new double[] {x, 231, z};
                }
            }
            if (c == null)
            {
                DevTestResults.check(name(), "a test safezone to stand in", "none", false, "run refuge or camp first");
                cx = 0;
                phase = 1;
                return;
            }
            cx = c[0];
            cy = 231;
            cz = c[2];
            phase = 0;
        }
        if (!moved)
        {
            p.setGameType(WorldSettings.GameType.SURVIVAL); // Decimation's NPCs ignore a creative player
            p.capabilities.disableDamage = true;
            p.capabilities.isFlying = phase == 1;
            p.sendPlayerAbilities();
            double x = phase == 0 ? cx : cx + 200;
            p.playerNetServerHandler.setPlayerLocation(x + 0.5, cy, cz + 0.5, 0, 0);
            if (phase == 0)
            {
                count(w, p, "near the player inside");
            }
            moved = true;
        }
    }

    private static void count(WorldServer w, EntityPlayerMP p, String label)
    {
        Map<String, Integer> m = new TreeMap<String, Integer>();
        for (Object o : w.loadedEntityList.toArray())
        {
            Entity e = (Entity) o;
            if (e != p && e.getDistanceToEntity(p) < 48)
            {
                String k = EntityList.getEntityString(e);
                k = k == null ? e.getClass().getSimpleName() : k;
                Integer v = m.get(k);
                m.put(k, v == null ? 1 : v + 1);
            }
        }
        DevTestResults.value("zoneperf", label, m.toString());
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

    private static double[] summary()
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
}
