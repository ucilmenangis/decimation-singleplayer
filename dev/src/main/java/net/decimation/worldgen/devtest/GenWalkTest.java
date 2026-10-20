package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.worldgen.BlockBatch;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

/**
 * New land while flying (bug.md "Server stalls while new city areas generate", docs/performance.md 9): the player flies
 * east at 10 blocks a second over city land this world never generated, and the integrated server's tick times (median,
 * slowest 1 %, worst, ticks over 100 / 250 ms, the slowest ticks with where they happened) and the client's frames are
 * recorded; at the end two shots of the land just made, at noon and at midnight (light in and around the buildings).
 * A one launch run makes a fresh seed 1 world, so two runs fly the same path: `devtest.py genwalk -Pbulklight=false`
 * (structure writes lit block by block, the old way) against `devtest.py genwalk`. Live: `light=old|new` picks the way
 * for the flight (a live world moves on to land it never made, so live flights do not share a path).
 * -Ddeciworldgen.autotest.chunks=N: chunks to fly (default 30).
 */
public class GenWalkTest extends DevTestMode
{
    public String name() { return "genwalk"; }

    @Override
    public boolean freshWorld()
    {
        return true;
    }

    private static final double SPEED = 0.5;    // blocks a client tick
    private static final int SETTLE = 40;        // client ticks at the start of the flight before measuring
    private final int chunks = Integer.parseInt(System.getProperty("deciworldgen.autotest.chunks", "30"));

    private volatile int phase;                  // 0 find, 1 fly, 2 shots, 3 done
    private volatile boolean flying, arrived, settled;
    private volatile double startX, startZ;
    private volatile int clientTicks, view;
    private final double y = 120;
    private int row, fromCx, calm;
    private boolean wasOn;
    private final List<Long> ticks = new ArrayList<Long>();
    private final List<Long> frames = new ArrayList<Long>();
    private final List<String> slow = new ArrayList<String>();
    private Frames counter;

    /** Client frame times (render tick starts) while the flight is measured. */
    public static final class Frames
    {
        volatile boolean on;
        long last;
        final List<Long> into;

        Frames(List<Long> into)
        {
            this.into = into;
        }

        @SubscribeEvent
        public void frame(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START)
            {
                return;
            }
            long now = System.nanoTime();
            if (on && last != 0)
            {
                synchronized (into)
                {
                    into.add(now - last);
                }
            }
            last = now;
        }
    }

    private static String how()
    {
        return BlockBatch.on ? "lit once" : "lit block by block";
    }

    public boolean client(Minecraft mc)
    {
        if (counter == null)
        {
            counter = new Frames(frames);
            FMLCommonHandler.instance().bus().register(counter);
            view = mc.gameSettings.renderDistanceChunks;
            wasOn = BlockBatch.on;
            String light = System.getProperty("deciworldgen.autotest.light", "");
            if (!light.isEmpty())
            {
                BlockBatch.on = !light.equals("old");
            }
        }
        if (mc.thePlayer == null)
        {
            return phase != 3;
        }
        if (phase == 1)
        {
            mc.thePlayer.capabilities.isFlying = true;
            if (!arrived || !settled)
            {
                return true;                     // the land around the start is still being made
            }
            clientTicks++;
            double x = startX + Math.max(0, clientTicks - SETTLE) * SPEED;
            mc.thePlayer.setPositionAndRotation(x, y, startZ, 270, 15); // facing east, a little down
            mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
            counter.on = clientTicks > SETTLE;
            flying = clientTicks > SETTLE;
            if (x >= startX + chunks * 16)
            {
                counter.on = false;
                flying = false;
                report();
                clientTicks = 0;
                phase = 2;
            }
        }
        else if (phase == 2)
        {
            clientTicks++;
            mc.thePlayer.setPositionAndRotation(startX + chunks * 16, y - 40, startZ, 90, 25); // looking back west
            if (clientTicks == 60)
            {
                DevTestUtil.screenshot(mc, name(), "genwalk_noon.png");
            }
            if (clientTicks == 120)
            {
                DevTestUtil.screenshot(mc, name(), "genwalk_midnight.png");
            }
            if (clientTicks > 125)
            {
                BlockBatch.on = wasOn;
                phase = 3;
            }
        }
        return phase != 3;
    }

    @Override
    public void server()
    {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer w = server.worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || view == 0)
        {
            return;
        }
        long tick = server.tickTimeArray[server.getTickCounter() % 100];
        if (phase == 0)
        {
            if (!find(w))
            {
                DevTestResults.check(name(), "never generated city land found", "none", false, "a row of new land");
                phase = 3;
                return;
            }
            // to the start of the flight, noon, clear sky, creative (no mobs bother a flying test)
            startX = fromCx * 16 + 8;
            startZ = row * 16 + 8;
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
            w.setWorldTime(6000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            p.playerNetServerHandler.setPlayerLocation(startX, y, startZ, 270, 15);
            calm = 0;
            arrived = true;
            phase = 1;
            return;
        }
        if (phase == 1 && !settled)
        {
            // settled once 40 ticks in a row took under 50 ms: the jump's own land is made
            calm = tick < 50000000L ? calm + 1 : 0;
            settled = calm >= 40;
        }
        if (flying)
        {
            synchronized (ticks)
            {
                ticks.add(tick);
            }
            if (tick > 150000000L)
            {
                // with its time and place, to match the log's placements and a JFR recording
                String at = String.format("%.0f ms at x %.0f", tick / 1e6, p.posX);
                FMLLog.info("[deciworldgen] GENWALK slow tick %s, %s", at, how());
                synchronized (slow)
                {
                    slow.add(at);
                }
            }
        }
        if (phase == 2)
        {
            w.setWorldTime(clientTicks < 90 ? 6000 : 18000);
        }
    }

    /**
     * A row of chunks on which the flight path and everything within view of it was never generated, at least 40 % of
     * the path city. In a fresh seed 1 world the first row found is the same every run.
     */
    private boolean find(WorldServer w)
    {
        long seed = w.getSeed();
        AnvilChunkLoader loader = w.theChunkProviderServer.currentChunkLoader instanceof AnvilChunkLoader
            ? (AnvilChunkLoader) w.theChunkProviderServer.currentChunkLoader : null;
        for (int cx = 1500; cx < 6000; cx += chunks + 2 * view + 2)
        {
            for (int cz = -600; cz < 600; cz += 7)
            {
                int city = 0;
                boolean fresh = true;
                for (int x = cx - view; x <= cx + chunks + view && fresh; x++)
                {
                    if (x >= cx && x < cx + chunks && Sectors.sector(seed, x, cz) == StructureGenerator.CITY)
                    {
                        city++;
                    }
                    for (int z = cz - view; z <= cz + view && fresh; z += 4)
                    {
                        fresh = !generated(w, loader, x, z);
                    }
                }
                if (fresh && city * 10 >= chunks * 4)
                {
                    fromCx = cx;
                    row = cz;
                    DevTestResults.value(name(), "flight", String.format("east from chunk %d on row %d, %d chunks, view %d,"
                                                                         + " %s", cx, cz, chunks, view, how()));
                    return true;
                }
            }
        }
        return false;
    }

    /** The chunk is in memory or saved (never opens a region file that does not exist: opening one creates it). */
    static boolean generated(WorldServer w, AnvilChunkLoader loader, int x, int z)
    {
        if (w.theChunkProviderServer.chunkExists(x, z))
        {
            return true;
        }
        java.io.File region = new java.io.File(w.getSaveHandler().getWorldDirectory(), "region/r." + (x >> 5) + "." + (z >> 5) + ".mca");
        return region.exists() && loader != null && loader.chunkExists(w, x, z);
    }

    private void report()
    {
        long[] t, f;
        synchronized (ticks)
        {
            t = toArray(ticks);
        }
        synchronized (frames)
        {
            f = toArray(frames);
        }
        Arrays.sort(t);
        Arrays.sort(f);
        int over100 = 0, over250 = 0;
        for (long v : t)
        {
            over100 += v > 100000000L ? 1 : 0;
            over250 += v > 250000000L ? 1 : 0;
        }
        DevTestResults.value(name(), "server " + how(), String.format("%d ticks: median %.1f ms, slowest 1 %% %.1f ms, worst %.1f"
                             + " ms, over 100 ms %d, over 250 ms %d", t.length, ms(t, 0.5), ms(t, 0.99), ms(t, 1), over100, over250));
        DevTestResults.value(name(), "client " + how(), String.format("%d frames: median %.1f ms, slowest 1 %% %.1f ms, worst %.1f"
                             + " ms", f.length, ms(f, 0.5), ms(f, 0.99), ms(f, 1)));
        synchronized (slow)
        {
            DevTestResults.value(name(), "slow ticks", slow.isEmpty() ? "none" : String.join(", ", slow));
        }
        DevTestResults.check(name(), "flight measured", t.length, t.length > 100 && ms(t, 1) < 2000,
                             "the flight ran (over 100 ticks) and no tick froze the server for 2 s");
    }

    private static long[] toArray(List<Long> l)
    {
        long[] a = new long[l.size()];
        for (int i = 0; i < a.length; i++)
        {
            a[i] = l.get(i);
        }
        return a;
    }

    private static double ms(long[] sorted, double q)
    {
        return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) (sorted.length * q))] / 1e6;
    }
}
