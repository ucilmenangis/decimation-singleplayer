package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.decimation.fixes.HumanLayers;
import net.decimation.fixes.ModelBatch;
import net.minecraft.client.renderer.texture.TextureManager;
import net.decimation.worldgen.SafezoneSites;
import net.decimation.worldgen.city.LcCity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

/**
 * First visit of a city refuge (docs/performance.md 10): the keeper (SafezoneSites) spawns all 22 residents in one tick
 * and the client draws them for the first time. The test finds a refuge the generator plans on land this world never
 * made, holds the keeper (SafezoneSites.hold) while the land around a player 40 blocks south of the refuge's middle is
 * made and drawn, records 5 s of server ticks and client frames, then lets the keeper go and records the fill (time per
 * resident kind), the server's tick to tick times and the client's frames from the first resident it receives.
 * One launch (`devtest.py refugevisit`): a fresh seed 1 world in a fresh game, two refuges one after the other: the first
 * visit draws the residents' models, textures and looks for the first time in the session, the second only new looks.
 */
public class RefugeVisitTest extends DevTestMode
{
    public String name() { return "refugevisit"; }

    @Override
    public boolean freshWorld()
    {
        return true;
    }

    private static final int BASE = 100, AFTER = 100;      // client ticks measured before the release / after arrival
    private static final double BACK = 40, Y = 100;        // the player: blocks south of the middle, height
    private static final int VISITS = 2;
    private int visit = 1;

    private volatile int phase;                            // 0 find, 1 settle, 2 baseline, 3 released, 4 report, 5 done
    private volatile int view, cx, cz;
    private volatile String site;
    private volatile boolean ready, release, filledSeen, restored;
    private int calm, settleTicks, clientTicks, arrivedAt = -1, allAt = -1, baseFrom, baseTo, afterFrom, afterTo, most, strays;
    private long lastServer, arrivalWall, releaseWall;
    private EnumDifficulty difficulty;
    private String spawning;
    private final List<Long> frames = new ArrayList<Long>();
    private final List<long[]> serverGaps = new ArrayList<long[]>();   // {tick to tick ns, phase}
    private GenWalkTest.Frames counter;
    private final Probe probe = new Probe();

    /**
     * The first frames from the residents' arrival, one line each: frame time and what it made for the first time
     * (textures loaded by Minecraft, look pictures and old way draws of HumanLayers, model parts of ModelBatch, layer
     * files read, time making pictures).
     */
    public static final class Probe
    {
        volatile boolean on;
        int left;
        long last, layers, old, parts, files, make, capture;
        int textures;
        final StringBuilder log = new StringBuilder();
        /** Textures Minecraft loaded while probing (their names). */
        final java.util.Set<String> newTextures = new java.util.TreeSet<String>();
        private java.util.Set<Object> known;

        void start(int frames)
        {
            last = System.nanoTime();
            textures = textures();
            layers = HumanLayers.built;
            old = HumanLayers.oldWay;
            parts = ModelBatch.built;
            files = HumanLayers.filesRead;
            make = HumanLayers.makeNanos;
            capture = ModelBatch.buildNanos;
            known = new java.util.HashSet<Object>(map().keySet());
            ModelBatch.callers = new ArrayList<String>();
            newTextures.clear();
            left = frames;
            on = true;
        }

        @SubscribeEvent
        public void frame(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START || !on)
            {
                return;
            }
            long now = System.nanoTime();
            int tex = textures();
            log.append(log.length() == 0 ? "" : " | ").append(String.format("%.0f ms: %d textures, %d looks, %d old way, %d parts"
                       + " (capturing %.1f ms), %d files, making %.1f ms", (now - last) / 1e6, tex - textures,
                       HumanLayers.built - layers, HumanLayers.oldWay - old, ModelBatch.built - parts,
                       (ModelBatch.buildNanos - capture) / 1e6, HumanLayers.filesRead - files, (HumanLayers.makeNanos - make) / 1e6));
            last = now;
            textures = tex;
            layers = HumanLayers.built;
            old = HumanLayers.oldWay;
            parts = ModelBatch.built;
            files = HumanLayers.filesRead;
            make = HumanLayers.makeNanos;
            capture = ModelBatch.buildNanos;
            for (Object k : map().keySet())
            {
                if (!known.contains(k))
                {
                    known.add(k);
                    newTextures.add(String.valueOf(k));
                }
            }
            on = --left > 0;
            if (!on)
            {
                callers = ModelBatch.callers;
                ModelBatch.callers = null;
            }
        }

        /** Who compiled model parts while probing (ModelBatch.callers). */
        List<String> callers;

        /** Textures Minecraft's TextureManager holds (each first bind of a new one loads it). */
        static int textures()
        {
            return map().size();
        }

        static Map<?, ?> map()
        {
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            return ReflectionHelper.getPrivateValue(TextureManager.class, tm, "mapTextureObjects", "field_110585_a");
        }
    }

    public boolean client(Minecraft mc)
    {
        if (counter == null)
        {
            counter = new GenWalkTest.Frames(frames);
            FMLCommonHandler.instance().bus().register(counter);
            FMLCommonHandler.instance().bus().register(probe);
            view = mc.gameSettings.renderDistanceChunks;
        }
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return phase != 5;
        }
        if (phase >= 1 && phase <= 3)
        {
            // hold still, facing the refuge (north), a little down
            mc.thePlayer.capabilities.isFlying = true;
            mc.thePlayer.setPositionAndRotation(cx + 0.5, Y, cz + BACK + 0.5, 180, 30);
            mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
        }
        if (phase == 1 && ready && ++clientTicks > 100)
        {
            // 5 s more for the client to draw the land, then the baseline
            clientTicks = 0;
            counter.on = true;
            synchronized (frames)
            {
                baseFrom = frames.size();
            }
            mark("baseline from");
            phase = 2;
        }
        else if (phase == 2 && ++clientTicks >= BASE)
        {
            synchronized (frames)
            {
                baseTo = frames.size();
            }
            strays = residents(mc);                       // mobs in the box before anyone was spawned
            releaseWall = mark("release");
            clientTicks = 0;
            release = true;
            phase = 3;
        }
        else if (phase == 3)
        {
            clientTicks++;
            int n = residents(mc) - strays;
            most = Math.max(most, n);
            if (n > 0 && arrivedAt < 0)
            {
                arrivedAt = clientTicks;
                synchronized (frames)
                {
                    afterFrom = frames.size();
                }
                arrivalWall = mark("arrival of " + n);
                probe.start(12);
            }
            if (n >= 22 && allAt < 0)
            {
                allAt = clientTicks;
            }
            if (arrivedAt >= 0 && clientTicks == arrivedAt + 60)
            {
                DevTestUtil.screenshot(mc, name(), "refugevisit.png");
            }
            if (arrivedAt >= 0 && clientTicks >= arrivedAt + AFTER || clientTicks > 400)
            {
                synchronized (frames)
                {
                    afterTo = frames.size();
                    afterFrom = arrivedAt < 0 ? baseTo : afterFrom;
                }
                counter.on = false;
                mark("end");
                phase = 4;
            }
        }
        else if (phase == 4 && restored)
        {
            report();
            if (visit < VISITS)
            {
                visit++;
                next();
            }
            else
            {
                phase = 5;
            }
        }
        return phase != 5;
    }

    /** Living NPCs the client holds inside the refuge's box. */
    private int residents(Minecraft mc)
    {
        int n = 0;
        for (Object o : mc.theWorld.loadedEntityList)
        {
            Entity e = (Entity) o;
            if (e instanceof EntityLiving && Math.abs(e.posX - cx) < 27 && Math.abs(e.posZ - cz) < 27)
            {
                n++;
            }
        }
        return n;
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
        long now = System.nanoTime();
        if (lastServer != 0 && phase >= 2 && phase <= 3)
        {
            synchronized (serverGaps)
            {
                serverGaps.add(new long[] {now - lastServer, phase});
            }
        }
        lastServer = now;
        if (phase == 0)
        {
            if (!find(w))
            {
                DevTestResults.check(name(), "never generated refuge found", "none", false, "a refuge on new land");
                phase = 5;
                return;
            }
            SafezoneSites.hold = true;
            if (visit == 1)
            {
                difficulty = w.difficultySetting;
                spawning = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            }
            w.difficultySetting = EnumDifficulty.NORMAL;      // residents are mobs: peaceful removes them
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false"); // no strays in the counts
            w.setWorldTime(6000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
            p.playerNetServerHandler.setPlayerLocation(cx + 0.5, Y, cz + BACK + 0.5, 180, 30);
            phase = 1;
            return;
        }
        if (phase == 1 && !ready)
        {
            SafezoneSites.Site s = SafezoneSites.find(site);
            long tick = server.tickTimeArray[server.getTickCounter() % 100];
            calm = s != null && SafezoneSites.ready(w, s) && tick < 50000000L ? calm + 1 : 0;
            ready = calm >= 40;
            if (++settleTicks > 20 * 180 && !ready)
            {
                DevTestResults.check(name(), "refuge ready", s == null ? "site never written" : "chunks not ready", false,
                                     "the site and its chunks within 3 minutes");
                restore(w);
                restored = true;
                phase = 5;
            }
            return;
        }
        if (phase == 4 && !restored)
        {
            if (visit == VISITS)
            {
                restore(w);
            }
            restored = true;
            return;
        }
        if (phase == 3 && release && SafezoneSites.hold)
        {
            SafezoneSites.lastFillTick = -1;
            SafezoneSites.hold = false;
        }
        if (phase == 3 && !filledSeen && SafezoneSites.lastFillTick >= 0)
        {
            filledSeen = true;
            synchronized (serverGaps)
            {
                serverGaps.add(new long[] {-1, 3});           // marks the tick that filled the refuge
            }
        }
    }

    /**
     * The first refuge, going east from x 4096 (far from the spawn area), whose middle and everything within view of the
     * player's spot this world never generated.
     */
    private boolean find(WorldServer w)
    {
        long seed = w.getSeed();
        AnvilChunkLoader loader = w.theChunkProviderServer.currentChunkLoader instanceof AnvilChunkLoader
            ? (AnvilChunkLoader) w.theChunkProviderServer.currentChunkLoader : null;
        for (int rx = 16; rx < 400; rx++)
        {
            for (int rz = -8; rz <= 8; rz++)
            {
                int[] ref = LcCity.refuge(seed, rx, rz);
                if (ref == null)
                {
                    continue;
                }
                int x = ref[0] * LcCity.CELL * 16 + 40, z = ref[1] * LcCity.CELL * 16 + 40;
                int pcx = x >> 4, pcz = (int) (z + BACK) >> 4;
                boolean fresh = true;
                for (int a = pcx - view; a <= pcx + view && fresh; a += 3)
                {
                    for (int b = pcz - view; b <= pcz + view && fresh; b += 3)
                    {
                        fresh = !GenWalkTest.generated(w, loader, a, b);
                    }
                }
                if (fresh)
                {
                    cx = x;
                    cz = z;
                    site = "lcz_" + ref[0] + "_" + ref[1];
                    DevTestResults.value(name(), "visit " + visit + " refuge", String.format("%s at %d,%d (region %d,%d, turns %d), player %.0f south at"
                                         + " y %.0f, view %d", site, x, z, rx, rz, ref[2], BACK, Y, view));
                    return true;
                }
            }
        }
        return false;
    }

    /** Fresh state for the next visit (the frame list goes on; its windows are kept by index). */
    private void next()
    {
        ready = release = filledSeen = restored = false;
        calm = settleTicks = clientTicks = most = strays = 0;
        arrivedAt = allAt = -1;
        lastServer = 0;
        synchronized (serverGaps)
        {
            serverGaps.clear();
        }
        probe.log.setLength(0);
        phase = 0;
    }

    private void restore(WorldServer w)
    {
        SafezoneSites.hold = false;
        if (difficulty != null)
        {
            w.difficultySetting = difficulty;
        }
        if (spawning != null)
        {
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning);
        }
    }

    private void report()
    {
        StringBuilder kinds = new StringBuilder();
        synchronized (SafezoneSites.lastFillKinds)
        {
            for (Map.Entry<String, long[]> k : SafezoneSites.lastFillKinds.entrySet())
            {
                kinds.append(String.format(", %s %d x %.1f ms", k.getKey(), k.getValue()[0], k.getValue()[1] / 1e6 / k.getValue()[0]));
            }
        }
        boolean filled = SafezoneSites.lastFillTick >= 0;
        DevTestResults.value(name(), "visit " + visit + " " + "fill", filled ? String.format("%d residents in %.1f ms%s", SafezoneSites.lastFillSpawned,
                             SafezoneSites.lastFillNanos / 1e6, kinds) : "never filled");
        DevTestResults.check(name(), "visit " + visit + " refuge filled", filled ? SafezoneSites.lastFillSpawned : 0,
                             filled && SafezoneSites.lastFillSpawned == 22, "22 residents");
        // server: tick to tick time (50 ms when the server keeps up), before and after the release
        List<Long> before = new ArrayList<Long>(), after = new ArrayList<Long>();
        long fillGap = 0;
        synchronized (serverGaps)
        {
            for (int i = 0; i < serverGaps.size(); i++)
            {
                long[] g = serverGaps.get(i);
                if (g[0] < 0)
                {
                    fillGap = i > 0 ? serverGaps.get(i - 1)[0] : 0;
                }
                else
                {
                    (g[1] == 2 ? before : after).add(g[0]);
                }
            }
        }
        DevTestResults.value(name(), "visit " + visit + " " + "server before", gaps(before));
        DevTestResults.value(name(), "visit " + visit + " " + "server slow ticks after release", slow(after, 80, releaseWall));
        DevTestResults.value(name(), "visit " + visit + " " + "server after release", gaps(after) + String.format(", the fill's tick %.0f ms", fillGap / 1e6));
        long[] f;
        synchronized (frames)
        {
            f = slice(baseFrom, baseTo);
            DevTestResults.value(name(), "visit " + visit + " " + "client before", frames(f));
            f = slice(afterFrom, afterTo);
        }
        DevTestResults.value(name(), "visit " + visit + " " + "client after arrival", frames(f));
        List<Long> fl = new ArrayList<Long>();
        for (long v : f)
        {
            fl.add(v);
        }
        DevTestResults.value(name(), "visit " + visit + " " + "client slow frames after arrival", slow(fl, 80, arrivalWall));
        DevTestResults.value(name(), "visit " + visit + " " + "first frames after arrival", probe.log.length() == 0 ? "none" : probe.log.toString());
        DevTestResults.value(name(), "visit " + visit + " parts compiled by", probe.callers == null || probe.callers.isEmpty() ? "none"
                             : String.join(" || ", probe.callers));
        DevTestResults.value(name(), "visit " + visit + " textures loaded after arrival", probe.newTextures.size() + ": "
                             + String.join(" ", probe.newTextures).replace("deci:textures/", ""));
        int deci = 0;
        for (String n : probe.newTextures)
        {
            deci += n.startsWith("deci:") ? 1 : 0;
        }
        DevTestResults.check(name(), "visit " + visit + " no Decimation texture loaded at arrival", deci, deci == 0,
                             "0 (fixes/ModelWarmup loaded them ahead)");
        DevTestResults.value(name(), "visit " + visit + " warm up", String.format("%s: %d gear models, %d textures, %.0f ms so far%s",
                             net.decimation.fixes.ModelWarmup.enabled ? "on" : "off", net.decimation.fixes.ModelWarmup.gear,
                             net.decimation.fixes.ModelWarmup.textures, net.decimation.fixes.ModelWarmup.nanos / 1e6,
                             net.decimation.fixes.ModelWarmup.finished ? ", done" : ""));
        DevTestResults.value(name(), "visit " + visit + " memory", String.format("process %s MB resident, Java heap %d MB used, %d quads in"
                             + " ModelBatch lists", rss(), (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20,
                             ModelBatch.quads));
        DevTestResults.value(name(), "visit " + visit + " " + "client residents", String.format("%d at most (%d strays in the box left out); first %s, all 22 %s", most, strays,
                             arrivedAt < 0 ? "never" : "after " + arrivedAt + " client ticks",
                             allAt < 0 ? "never" : "after " + allAt + " client ticks"));
        DevTestResults.check(name(), "visit " + visit + " measured", f.length, f.length > 20 && baseTo > baseFrom, "frames before and after");
    }

    /**
     * This process's resident memory in MB (ps), and on macOS its physical footprint and the graphics driver's share
     * (vmmap -summary: "Physical footprint", IOAccelerator regions; display lists and textures live there).
     */
    private static String rss()
    {
        StringBuilder out = new StringBuilder();
        try
        {
            String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            Process p = new ProcessBuilder("ps", "-o", "rss=", "-p", pid).start();
            String s = new java.util.Scanner(p.getInputStream()).useDelimiter("\\A").next().trim();
            out.append(Long.parseLong(s) >> 10);
            if (System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                Process v = new ProcessBuilder("vmmap", "-summary", pid).redirectErrorStream(true).start();
                java.util.Scanner sc = new java.util.Scanner(v.getInputStream());
                while (sc.hasNextLine())
                {
                    String l = sc.nextLine().trim();
                    if (l.startsWith("Physical footprint:") || l.startsWith("IOAccelerator"))
                    {
                        out.append(" | ").append(l.replaceAll("\\s+", " "));
                    }
                }
            }
        }
        catch (Exception e)
        {
            out.append(" ?");
        }
        return out.toString();
    }

    /** Logs a moment with the wall clock (to cut a JFR recording); returns the wall clock in ms. */
    private long mark(String what)
    {
        long now = System.currentTimeMillis();
        FMLLog.info("[deciworldgen] REFUGEVISIT %s at %s", what, new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(now));
        return now;
    }

    /** The times over ms in a run of times, each with its end after the first one began and its wall clock. */
    private static String slow(List<Long> l, int ms, long wall)
    {
        StringBuilder b = new StringBuilder();
        long at = 0;
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        for (long v : l)
        {
            at += v;
            if (v > ms * 1000000L)
            {
                b.append(b.length() == 0 ? "" : ", ").append(String.format("%.0f ms ending +%.2f s (%s)", v / 1e6, at / 1e9,
                                                                          f.format(wall + at / 1000000L)));
            }
        }
        return b.length() == 0 ? "none" : b.toString();
    }

    private long[] slice(int from, int to)
    {
        long[] a = new long[Math.max(0, to - from)];
        for (int i = 0; i < a.length; i++)
        {
            a[i] = frames.get(from + i);
        }
        return a;
    }

    private static String gaps(List<Long> l)
    {
        long[] a = new long[l.size()];
        int over = 0;
        for (int i = 0; i < a.length; i++)
        {
            a[i] = l.get(i);
            over += a[i] > 100000000L ? 1 : 0;
        }
        Arrays.sort(a);
        return String.format("%d ticks: median %.1f ms, worst %.1f ms, over 100 ms %d", a.length, ms(a, 0.5), ms(a, 1), over);
    }

    private static String frames(long[] f)
    {
        long[] a = f.clone();
        Arrays.sort(a);
        int over50 = 0;
        for (long v : a)
        {
            over50 += v > 50000000L ? 1 : 0;
        }
        StringBuilder top = new StringBuilder();
        for (int i = a.length - 1; i >= Math.max(0, a.length - 5); i--)
        {
            top.append(i == a.length - 1 ? "" : " ").append(String.format("%.0f", a[i] / 1e6));
        }
        return String.format("%d frames: median %.1f ms, slowest 1 %% %.1f ms, worst %.1f ms, over 50 ms %d (top %s)", a.length,
                             ms(a, 0.5), ms(a, 0.99), ms(a, 1), over50, top);
    }

    private static double ms(long[] sorted, double q)
    {
        return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) (sorted.length * q))] / 1e6;
    }
}
