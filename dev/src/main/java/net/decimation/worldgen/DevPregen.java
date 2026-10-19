package net.decimation.worldgen;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * Dev only: generate (load and populate) the chunks around a point, so large
 * structures can be checked in the world files without a player walking
 * there. Inert unless the JVM runs with -Ddeciworldgen.pregen=x,z,radius
 * (block coordinates, radius in chunks; set by `gradlew runServer
 * -Ppregen=x,z,r`, see tools/servertest.py). Each chunk load is timed (it generates the chunk and populates
 * the ones it completes): the log gets the median, the slowest 1 % and the ten slowest with their sector
 * (docs/performance.md, city stalls). -Ddeciworldgen.gatescan=x,z,r (blocks) first logs the
 * wasteland gates there (city/WastelandFence.scan). Server side only classes.
 */
public class DevPregen
{
    public static final String PROPERTY = "deciworldgen.pregen";

    /** WorldServer's pending block tick set (private; found by its type). */
    static java.util.Set<?> pendingTicks(WorldServer world)
    {
        for (java.lang.reflect.Field f : WorldServer.class.getDeclaredFields())
        {
            if (f.getType() == java.util.TreeSet.class)
            {
                try
                {
                    f.setAccessible(true);
                    return (java.util.Set<?>) f.get(world);
                }
                catch (Exception e)
                {
                    return null;
                }
            }
        }
        return null;
    }
    private boolean done;

    /**
     * -Ddeciworldgen.pregenwalk=x,z,steps,view: loads chunks as a player walking east from x, z does (a square of
     * chunks within view around the chunk it stands in, one chunk on per step: a new row of 2 view + 1 chunks), and
     * times each step: the stall that player feels when a row of new land generates (docs/performance.md 9).
     */
    static void walk(WorldServer world, int cx, int cz, int steps, int view)
    {
        java.lang.management.ThreadMXBean cpu = java.lang.management.ManagementFactory.getThreadMXBean();
        long[] ms = new long[steps];
        int[] sectorAt = new int[steps];
        for (int x = cx - view; x <= cx + view; x++)
        {
            for (int z = cz - view; z <= cz + view; z++)
            {
                world.theChunkProviderServer.loadChunk(x, z); // where the player starts (not timed)
            }
        }
        long all = System.nanoTime(), allCpu = cpu.getCurrentThreadCpuTime();
        for (int s = 0; s < steps; s++)
        {
            long t0 = System.nanoTime();
            for (int x = cx + 1 + s - view; x <= cx + 1 + s + view; x++)
            {
                for (int z = cz - view; z <= cz + view; z++)
                {
                    world.theChunkProviderServer.loadChunk(x, z);
                }
            }
            ms[s] = (System.nanoTime() - t0) / 1000000;
            sectorAt[s] = Sectors.sector(world.getSeed(), cx + 1 + s + view, cz);
        }
        all = System.nanoTime() - all;
        allCpu = cpu.getCurrentThreadCpuTime() - allCpu;
        long[] sorted = ms.clone();
        java.util.Arrays.sort(sorted);
        long sum = 0;
        StringBuilder each = new StringBuilder();
        for (int s = 0; s < steps; s++)
        {
            sum += ms[s];
            each.append(s == 0 ? "" : " ").append(ms[s]).append(sectorAt[s] == 2 ? "C" : "");
        }
        FMLLog.info("[%s] WALK %s: %d steps, view %d, %.1f s (server thread CPU %.1f s); per step median %d ms, 90%% %d ms,"
                    + " max %d ms, mean %d ms, steps over 250 ms %d; each step (C = leading row in a city): %s",
                    DecimationWorldGen.MODID, BlockBatch.on ? "lit once" : "lit block by block", steps, view, all / 1e9,
                    allCpu / 1e9, sorted[steps / 2], sorted[Math.min(steps - 1, steps * 9 / 10)], sorted[steps - 1],
                    sum / steps, countOver(ms, 250), each);
        FMLLog.info("[%s] WALK done", DecimationWorldGen.MODID);
    }

    private static int countOver(long[] ms, long limit)
    {
        int n = 0;
        for (long v : ms)
        {
            n += v > limit ? 1 : 0;
        }
        return n;
    }

    /** Heights the light check covers (ground, cities and their buildings). */
    static final int LIGHT_Y0 = 30, LIGHT_Y1 = 160;

    /**
     * Light as vanilla's own rule says it must be (World.computeLightValue): sky 15 at or above the height map, an
     * opaque block 0, anything else the brightest neighbour minus its opacity (at least 1); block light the same from
     * the block's own light. Counts the cells of the asked chunks whose stored light breaks the rule: {cells, sky wrong,
     * sky off by 3 or more, block wrong, block off by 3 or more, first wrong sky x, y, z}. The correctness gate of the
     * structure writes' bulk lighting (docs/performance.md 9): never more wrong cells than vanilla's own block by block
     * lighting left.
     */
    static long[] lightCheck(WorldServer world, int cx, int cz, int r)
    {
        long[] out = {0, 0, 0, 0, 0, 0, -1, 0};
        out[5] = Integer.MIN_VALUE;
        for (int chx = cx - r; chx <= cx + r; chx++)
        {
            for (int chz = cz - r; chz <= cz + r; chz++)
            {
                for (int lx = 0; lx < 16; lx++)
                {
                    for (int lz = 0; lz < 16; lz++)
                    {
                        int x = chx * 16 + lx, z = chz * 16 + lz;
                        for (int y = LIGHT_Y0; y <= LIGHT_Y1; y++)
                        {
                            out[0]++;
                            for (net.minecraft.world.EnumSkyBlock type : net.minecraft.world.EnumSkyBlock.values())
                            {
                                int want = expectedLight(world, type, x, y, z);
                                int have = world.getSavedLightValue(type, x, y, z);
                                if (want != have)
                                {
                                    boolean sky = type == net.minecraft.world.EnumSkyBlock.Sky;
                                    out[sky ? 1 : 3]++;
                                    if (Math.abs(want - have) >= 3)
                                    {
                                        out[sky ? 2 : 4]++;
                                    }
                                    if (sky && out[5] == Integer.MIN_VALUE)
                                    {
                                        out[5] = x;
                                        out[6] = y;
                                        out[7] = z;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    /** World.computeLightValue (private there), the same steps. */
    static int expectedLight(WorldServer world, net.minecraft.world.EnumSkyBlock type, int x, int y, int z)
    {
        if (type == net.minecraft.world.EnumSkyBlock.Sky && world.canBlockSeeTheSky(x, y, z))
        {
            return 15;
        }
        net.minecraft.block.Block block = world.getBlock(x, y, z);
        int own = block.getLightValue(world, x, y, z);
        int l = type == net.minecraft.world.EnumSkyBlock.Sky ? 0 : own;
        int op = block.getLightOpacity(world, x, y, z);
        if (op >= 15 && own > 0)
        {
            op = 1;
        }
        op = Math.max(1, op);
        if (op >= 15)
        {
            return 0;
        }
        if (l >= 14)
        {
            return l;
        }
        for (int side = 0; side < 6; side++)
        {
            int v = world.getSavedLightValue(type, x + net.minecraft.util.Facing.offsetsXForSide[side],
                                             y + net.minecraft.util.Facing.offsetsYForSide[side],
                                             z + net.minecraft.util.Facing.offsetsZForSide[side]) - op;
            if (v > l)
            {
                l = v;
            }
            if (l >= 14)
            {
                return l;
            }
        }
        return l;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (done || event.phase != TickEvent.Phase.END)
        {
            return;
        }
        done = true;
        String scan = System.getProperty("deciworldgen.gatescan");
        if (scan != null)
        {
            // the wasteland gates around a point by the generator's own decisions, nothing generated
            String[] s = scan.split(",");
            WorldServer w0 = MinecraftServer.getServer().worldServerForDimension(0);
            net.decimation.worldgen.city.LcCity.gateScan(w0.getSeed(), Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()),
                                                         Integer.parseInt(s[2].trim()));
            FMLLog.info("[%s] GATESCAN done around %s", DecimationWorldGen.MODID, scan);
        }
        String walk = System.getProperty("deciworldgen.pregenwalk");
        if (walk != null)
        {
            String[] w = walk.split(",");
            walk(MinecraftServer.getServer().worldServerForDimension(0), Integer.parseInt(w[0].trim()) >> 4,
                 Integer.parseInt(w[1].trim()) >> 4, Integer.parseInt(w[2].trim()), Integer.parseInt(w[3].trim()));
        }
        String[] p = System.getProperty(PROPERTY, "").split(",");
        if (p.length != 3)
        {
            return;
        }
        int cx = Integer.parseInt(p[0].trim()) >> 4, cz = Integer.parseInt(p[1].trim()) >> 4;
        int r = Integer.parseInt(p[2].trim());
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        int n = 0;
        java.util.List<long[]> times = new java.util.ArrayList<long[]>(); // {ns, x, z}
        java.lang.management.ThreadMXBean cpu = java.lang.management.ManagementFactory.getThreadMXBean();
        long all = System.nanoTime(), allCpu = cpu.getCurrentThreadCpuTime();
        // one ring wider than asked, so every asked chunk gets populated
        for (int x = cx - r - 1; x <= cx + r + 1; x++)
        {
            for (int z = cz - r - 1; z <= cz + r + 1; z++)
            {
                long t0 = System.nanoTime();
                world.theChunkProviderServer.loadChunk(x, z);
                times.add(new long[] {System.nanoTime() - t0, x, z});
                n++;
            }
        }
        all = System.nanoTime() - all;
        allCpu = cpu.getCurrentThreadCpuTime() - allCpu;
        long gen = StructureGenerator.populateNanos, write = Slices.writeNanos, blocks = Slices.setBlocks, falling = Slices.fallingSet;
        java.util.Set<?> pending = pendingTicks(world);
        java.util.Map<String, Integer> kinds = new java.util.HashMap<String, Integer>();
        if (pending != null)
        {
            for (Object o : pending)
            {
                String k = String.valueOf(net.minecraft.block.Block.blockRegistry.getNameForObject(
                    ((net.minecraft.world.NextTickListEntry) o).func_151351_a()));
                kinds.put(k, kinds.containsKey(k) ? kinds.get(k) + 1 : 1);
            }
        }
        long save = System.nanoTime();
        world.theChunkProviderServer.saveChunks(true, null);
        save = System.nanoTime() - save;
        FMLLog.info("[%s] PREGEN parts: our population %.2f s (structure writes %s %.2f s, CPU %.2f s, %d blocks set, %d"
                    + " falling; batch light checks sky %d, block %d), saving %.2f s; pending block ticks %d %s",
                    DecimationWorldGen.MODID, gen / 1e9, BlockBatch.on ? "lit once" : "lit block by block", write / 1e9,
                    Slices.writeCpu / 1e9, blocks, falling, BlockBatch.skyChecks, BlockBatch.blockChecks, save / 1e9,
                    pending == null ? -1 : pending.size(), kinds);
        java.util.Collections.sort(times, new java.util.Comparator<long[]>()
        {
            public int compare(long[] a, long[] b)
            {
                return Long.compare(b[0], a[0]);
            }
        });
        StringBuilder worst = new StringBuilder();
        for (int i = 0; i < Math.min(10, times.size()); i++)
        {
            long[] t = times.get(i);
            worst.append(String.format(" %d,%d:%.0fms(%s)", t[1], t[2], t[0] / 1e6,
                                       new String[] {"wild", "civ", "city", "mil"}[Sectors.sector(world.getSeed(), (int) t[1], (int) t[2])]));
        }
        FMLLog.info("[%s] PREGEN timing: %d chunks in %.1f s (server thread CPU %.1f s), median %.1f ms, slowest 1%% %.1f ms;"
                    + " slowest:%s", DecimationWorldGen.MODID, n, all / 1e9, allCpu / 1e9, times.get(times.size() / 2)[0] / 1e6,
                    times.get(Math.max(0, times.size() / 100))[0] / 1e6, worst);
        long[] light = lightCheck(world, cx, cz, r);
        FMLLog.info("[%s] PREGEN light: %d cells (y %d..%d), sky wrong %d (off by 3 or more %d), block wrong %d (off by 3 or"
                    + " more %d); first wrong sky at %d,%d,%d", DecimationWorldGen.MODID, light[0], LIGHT_Y0, LIGHT_Y1,
                    light[1], light[2], light[3], light[4], light[5], light[6], light[7]);
        FMLLog.info("[%s] PREGEN done: %d chunks around %s", DecimationWorldGen.MODID, n, System.getProperty(PROPERTY));
    }
}
