package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.decimation.fixes.Deci;
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
                                            "aiming over iron sights"};
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
            listening = true;
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
                if (phase == 1)
                {
                    DevTestResults.check(name(), "no glass measurements while the scoped gun is only held", String.valueOf(measures),
                                         measures == 0, "0 (the glass is copied only while aiming)");
                }
            }
            phase++;
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
