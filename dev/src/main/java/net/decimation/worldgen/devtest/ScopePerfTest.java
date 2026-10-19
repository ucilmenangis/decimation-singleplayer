package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.decimation.fixes.Deci;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Scope stutter (user 19 Oktober 2026: "our new scope methods really buggy and often drop fps"): frame times in four
 * phases of -Pphase seconds each (default 5): a plain AKM at the hip, the AKM with -Psight (default ta11acog) at the hip,
 * aiming through that sight, aiming over iron sights. Per phase: the median, the slowest 1 %, the worst frame, the spikes
 * (frames over 2.5 x the median, and how many fell on a frame with a glass measurement) and the glass measurements
 * (PatchScope's GL feedback pass). Check: no glass measurement while the scoped gun is only held (the glass copy runs
 * only while aiming). Frame times are recorded, not judged: on the M4 the dev world's own noise moved them more than
 * the scope did.
 */
public class ScopePerfTest extends DevTestMode
{
    public String name() { return "scopeperf"; }

    private static final String[] PHASES = {"plain gun at the hip", "scoped gun at the hip", "aiming through the scope",
                                            "aiming over iron sights", "scoped gun at the hip after aiming"};
    private final int phaseTicks = Integer.getInteger("deciworldgen.autotest.phase", 5) * 20;
    private final String sight = System.getProperty("deciworldgen.autotest.sight", "ta11acog");
    private static volatile long last, lastGlass;
    private static volatile int measures;
    private static final List<Long> frames = new ArrayList<Long>();
    /** Per recorded frame: did a glass measurement happen in the frame before it (the time recorded covers that frame). */
    private static final List<Boolean> measured = new ArrayList<Boolean>();
    private static volatile boolean measuredLast;
    private static volatile boolean recording;
    private static boolean listening;
    private int ticks, phase = -1;
    private final double[][] stats = new double[PHASES.length][];

    /**
     * GL errors and stack depths at five points of each frame, per phase (the brother's laptop, NVIDIA, logged "Stack
     * overflow" every frame once a gun with the EOTech 558 was held, 19 Oktober 2026): which stack, in which part.
     */
    public static final class Probe
    {
        static final String[] POINTS = {"frame start", "after the world", "before the hand", "after the hand", "frame end"};
        static final String[] STACKS = {"modelview", "projection", "texture", "attrib"};
        static volatile int phase = -1;
        static final int[][][] depth = new int[PHASES.length][POINTS.length][4];   // largest seen
        static final int[][] errors = new int[PHASES.length][POINTS.length];
        /** Samples with the matrix mode not modelview / the active texture unit not 0. */
        static final int[][] badMode = new int[PHASES.length][POINTS.length], badUnit = new int[PHASES.length][POINTS.length];
        static final String[] first = new String[PHASES.length];

        static void check(int point)
        {
            int ph = phase;
            if (ph < 0 || ph >= PHASES.length)
            {
                return;
            }
            int err = GL11.glGetError();
            if (err != 0)
            {
                errors[ph][point]++;
                if (first[ph] == null)
                {
                    first[ph] = POINTS[point] + " error " + err;
                }
            }
            int[] d = {GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH), GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH),
                       GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH), GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH)};
            for (int k = 0; k < 4; k++)
            {
                depth[ph][point][k] = Math.max(depth[ph][point][k], d[k]);
            }
            if (GL11.glGetInteger(GL11.GL_MATRIX_MODE) != GL11.GL_MODELVIEW)
            {
                badMode[ph][point]++;
            }
            if (GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE) != org.lwjgl.opengl.GL13.GL_TEXTURE0)
            {
                badUnit[ph][point]++;
            }
        }

        static String report(int ph)
        {
            StringBuilder s = new StringBuilder();
            for (int p = 0; p < POINTS.length; p++)
            {
                s.append(String.format("%s %d/%d/%d/%d%s%s%s; ", POINTS[p], depth[ph][p][0], depth[ph][p][1], depth[ph][p][2],
                                       depth[ph][p][3], errors[ph][p] > 0 ? " ERR x" + errors[ph][p] : "",
                                       badMode[ph][p] > 0 ? " MODE x" + badMode[ph][p] : "", badUnit[ph][p] > 0 ? " UNIT x" + badUnit[ph][p] : ""));
            }
            return s.toString();
        }

        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.HIGHEST)
        public void tick(TickEvent.RenderTickEvent e)
        {
            check(e.phase == TickEvent.Phase.START ? 0 : 4);
        }

        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.HIGHEST)
        public void world(net.minecraftforge.client.event.RenderWorldLastEvent e)
        {
            check(1);
        }

        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.HIGHEST)
        public void hand(net.minecraftforge.client.event.RenderHandEvent e)
        {
            check(2);
        }

        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.HIGHEST)
        public void overlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Pre e)
        {
            if (e.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL)
            {
                check(3);
            }
        }
    }

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
            long g = Deci.scopeGlassTime(); // a new glass measurement (PatchScope's feedback pass) in the frame that just ended
            boolean m = g != lastGlass;
            if (recording && last != 0)
            {
                synchronized (frames)
                {
                    frames.add(now - last);
                    measured.add(m);
                }
            }
            last = now;
            if (recording && m)
            {
                measures++;
            }
            lastGlass = g;
        }
    }

    public boolean client(Minecraft mc)
    {
        if (!listening)
        {
            FMLCommonHandler.instance().bus().register(new Clock());
            Probe probe = new Probe();
            FMLCommonHandler.instance().bus().register(probe);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(probe);
            listening = true;
            DevTestResults.value(name(), "GL stack limits", String.format("modelview %d, projection %d, texture %d, attrib %d",
                GL11.glGetInteger(GL11.GL_MAX_MODELVIEW_STACK_DEPTH), GL11.glGetInteger(GL11.GL_MAX_PROJECTION_STACK_DEPTH),
                GL11.glGetInteger(GL11.GL_MAX_TEXTURE_STACK_DEPTH), GL11.glGetInteger(GL11.GL_MAX_ATTRIB_STACK_DEPTH)));
        }
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (phase < 0 || ticks >= phaseTicks)
        {
            if (phase >= 0)
            {
                recording = false;
                stats[phase] = summary();
                DevTestResults.value(name(), PHASES[phase], String.format("median %.1f ms, slowest 1%% %.1f ms, worst %.1f ms, spikes %d of %d frames (%d on a measuring frame), glass measurements %d",
                                     stats[phase][0], stats[phase][1], stats[phase][2], (int) stats[phase][3], (int) stats[phase][4], (int) stats[phase][5], measures));
                DevTestResults.value(name(), PHASES[phase] + " GL", Probe.report(phase)
                                     + (Probe.first[phase] == null ? "no GL error" : "first " + Probe.first[phase]));
                int errs = 0;
                for (int n : Probe.errors[phase])
                {
                    errs += n;
                }
                // OptiFine posts RenderHandEvent inside renderHand: ScopeZoom's own renderHand call came back into its
                // handler until Java's stack ran out (the brother's laptop, 19 Oktober 2026: gun gone while aiming,
                // "Stack overflow" every frame); run in the production client with OptiFine in mods/
                DevTestResults.check(name(), PHASES[phase] + ": no GL error", String.valueOf(errs), errs == 0,
                                     "0 (GL errors at the five points of each frame)");
                if (phase == 1)
                {
                    DevTestResults.check(name(), "no glass measurements while the scoped gun is only held", String.valueOf(measures),
                                         measures == 0, "0 (the glass is copied only while aiming)");
                }
            }
            phase++;
            if (phase == 0)
            {
                for (int i = 0; i < PHASES.length; i++)
                {
                    Probe.first[i] = null;
                    Arrays.fill(Probe.errors[i], 0);
                    Arrays.fill(Probe.badMode[i], 0);
                    Arrays.fill(Probe.badUnit[i], 0);
                    for (int[] d : Probe.depth[i])
                    {
                        Arrays.fill(d, 0);
                    }
                }
            }
            Probe.phase = phase;
            ticks = 0;
            if (phase >= PHASES.length)
            {
                Deci.setAimMode(mc.thePlayer, 0);
                mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, null);
                // frame times are recorded, not judged: the dev world's own noise (rain, NPCs, chunk work) moved them more than
                // the scope did on this Mac (19 Oktober 2026); the count of glass measurements above is the check
                DevTestResults.value(name(), "slowest 1% aiming through the scope vs over iron sights",
                                     String.format("%.1f ms vs %.1f ms", stats[2][1], stats[3][1]));
                return false;
            }
            hold(mc, phase == 0 || phase == 3 ? null : sight);
            synchronized (frames)
            {
                frames.clear();
                measured.clear();
            }
            measures = 0;
            recording = ticks > 0; // starts after the gun is up (below)
            return true;
        }
        ticks++;
        if (ticks == phaseTicks / 2)
        {
            DevTestUtil.screenshot(mc, name(), "scopeperf_" + phase + "_" + sight + ".png");
            if (phase == 4)
            {
                // the brother's laptop: back at the hip the sight's glass still showed the last aimed view (a frozen
                // "new camera"): the scope texture must hold no picture once the copying stopped
                double plain = plainShare(net.decimation.fixes.ScopeZoom.LENS);
                DevTestResults.check(name(), "glass after aiming holds no picture", String.format("%.1f %% plain lens", plain * 100),
                                     plain > 0.99, "the whole scope texture the lens tint (99 % or more)");
            }
        }
        if (phase == 2 || phase == 3)
        {
            Deci.setAimMode(mc.thePlayer, 1); // the server may send the mode back each tick
        }
        else
        {
            Deci.setAimMode(mc.thePlayer, 0);
        }
        recording = ticks > 20; // the first second: the gun comes up, the scope zooms in
        return true;
    }

    /** Noon, no rain: the glass is judged by eye on the screenshots. */
    @Override
    public void server()
    {
        if (phase == 0 && ticks == 1)
        {
            net.minecraft.world.WorldServer w = net.minecraft.server.MinecraftServer.getServer().worldServerForDimension(0);
            w.setWorldTime(6000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
        }
    }

    private void hold(Minecraft mc, String sightName)
    {
        ItemStack s = new ItemStack((Item) Item.itemRegistry.getObject("deci:akm"));
        s.stackTagCompound = new NBTTagCompound();
        s.stackTagCompound.setInteger("ammo", 30);
        if (sightName != null)
        {
            s.stackTagCompound.setString("sightAttach", sightName);
        }
        mc.gameSettings.thirdPersonView = 0;
        mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, s);
    }

    /** Share of the scope texture's texels that are exactly rgb (read back from the GL). */
    private static double plainShare(int rgb)
    {
        int tex = Deci.scopeTexture();
        if (tex <= 0)
        {
            return 0;
        }
        int was = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        java.nio.ByteBuffer b = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, b);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, was);
        long same = 0;
        for (int i = 0; i < w * h; i++)
        {
            int c = (b.get(i * 4) & 255) << 16 | (b.get(i * 4 + 1) & 255) << 8 | (b.get(i * 4 + 2) & 255);
            same += c == rgb ? 1 : 0;
        }
        return w * h == 0 ? 0 : same / (double) (w * h);
    }

    /** median, slowest 1 %, worst (ms), spikes, frames. */
    private static double[] summary()
    {
        long[] f;
        boolean[] mf;
        synchronized (frames)
        {
            f = new long[frames.size()];
            mf = new boolean[f.length];
            for (int i = 0; i < f.length; i++)
            {
                f[i] = frames.get(i);
                mf[i] = measured.get(i);
            }
        }
        if (f.length == 0)
        {
            return new double[] {0, 0, 0, 0, 0, 0};
        }
        long[] s = f.clone();
        Arrays.sort(s);
        double median = s[s.length / 2] / 1e6, p99 = s[Math.min(s.length - 1, (int) (s.length * 0.99))] / 1e6, worst = s[s.length - 1] / 1e6;
        int spikes = 0, onMeasure = 0;
        for (int i = 0; i < f.length; i++)
        {
            if (f[i] / 1e6 > median * 2.5)
            {
                spikes++;
                if (mf[i])
                {
                    onMeasure++;
                }
            }
        }
        return new double[] {median, p99, worst, spikes, f.length, onMeasure};
    }
}
