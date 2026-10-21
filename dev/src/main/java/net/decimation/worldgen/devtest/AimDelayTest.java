package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.decimation.fixes.ScopeZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The delay when aiming through a scope (user, 21 Oktober 2026: "the scope aiming still need more optimizing cause
 * sometime it has delay on it. due to loading the scope render"). Per gun and sight, a first aim (ScopeZoom's learned
 * sight positions forgotten first, as after a launch) and a second aim: the time from the aim until the zoom is 95 % in,
 * the worst frame in the first second and the frames over 50 ms. Pairs: -Ppairs=gun:sight,... (default the M4A4 with our
 * ACOG, EOTech and SpecterDR and Decimation's 2x, the AKM with its red dot). Values first; the checks follow the measure.
 */
public class AimDelayTest extends DevTestMode
{
    public String name() { return "aimdelay"; }

    private static final String[][] DEFAULT = {{"m4a4", "ta11acog"}, {"m4a4", "eotech558"}, {"m4a4", "specterdr"}, {"m4a4", "2x"},
                                               {"akm", "reddot"}};
    private final String[][] pairs = pairs(System.getProperty("deciworldgen.autotest.pairs", ""));
    private int pair, phase, ticks;
    /** Time to 95 % zoom of the second aim (learned), per pair, to compare with the aim after a relaunch. */
    private long learnedT95;
    private volatile Runnable onServer;
    private static Watch watch;

    /** Frame times and zoom per frame while recording. */
    public static final class Watch
    {
        volatile boolean on;
        final List<long[]> frames = new ArrayList<long[]>();     // {nanoTime, zoom x 1000}
        private long last;

        @SubscribeEvent
        public void tick(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START)
            {
                return;
            }
            long now = System.nanoTime();
            if (on)
            {
                synchronized (frames)
                {
                    frames.add(new long[] {now, now - last, Math.round(ScopeZoom.currentZoom() * 1000)});
                }
            }
            last = now;
        }
    }

    public boolean client(Minecraft mc)
    {
        if (watch == null)
        {
            watch = new Watch();
            FMLCommonHandler.instance().bus().register(watch);
        }
        if (pair >= pairs.length)
        {
            return false;
        }
        ticks++;
        final String[] p = pairs[pair];
        if (phase == 0 && ticks == 1)
        {
            onServer = () -> {
                EntityPlayerMP pl = DevTestUtil.player();
                ItemStack gun = new ItemStack((Item) Item.itemRegistry.getObject("deci:" + p[0]));
                gun.stackTagCompound = new NBTTagCompound();
                gun.stackTagCompound.setInteger("ammo", 10);
                gun.stackTagCompound.setString("sightAttach", p[1]);
                pl.inventory.setInventorySlotContents(0, gun);
                pl.inventory.currentItem = 0;
                pl.rotationPitch = -30;
            };
            mc.thePlayer.inventory.currentItem = 0;
            mc.thePlayer.rotationPitch = -30;
            ScopeZoom.forgetSights();                      // a first aim, as after a launch
        }
        // phase 0: first aim, phase 1: second aim; each: 20 ticks at the hip, aim and record 60 ticks, lower
        int t = ticks - 20;
        if (t == 0)
        {
            synchronized (watch.frames)
            {
                watch.frames.clear();
            }
            watch.on = true;
            aim(mc, 1);
        }
        if (phase == 2 && ticks == 2)
        {
            ScopeZoom.relaunchSights();                    // as after a new launch: kept sight positions read back
        }
        if (t == 60)
        {
            watch.on = false;
            aim(mc, 0);
            float mag = ScopeZoom.magnificationFor(mc.thePlayer.getHeldItem());
            long t95 = report(p, phase == 0 ? "first" : phase == 1 ? "second" : "relaunch", mag);
            if (phase == 1)
            {
                learnedT95 = t95;
            }
            if (phase == 2)
            {
                long budget = learnedT95 * 5 / 4 + 30_000_000L;
                DevTestResults.check(name(), p[0] + " " + p[1] + ": after a relaunch zooms like a learned sight",
                                     String.format("%.0f ms (learned %.0f ms)", t95 / 1e6, learnedT95 / 1e6),
                                     t95 >= 0 && learnedT95 >= 0 && t95 <= budget, String.format("at most %.0f ms", budget / 1e6));
            }
        }
        if (t == 90)
        {
            phase++;
            ticks = 1;                                     // later aims keep the gun (tick 1 of phase 1 and 2 sets nothing)
            if (phase == 3)
            {
                phase = 0;
                ticks = 0;
                pair++;
            }
        }
        return true;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    /** Writes the aim's values; returns its time to 95 % zoom in ns (-1: never). */
    private long report(String[] p, String which, float mag)
    {
        List<long[]> f;
        synchronized (watch.frames)
        {
            f = new ArrayList<long[]>(watch.frames);
        }
        if (f.isEmpty())
        {
            DevTestResults.value(name(), p[0] + " " + p[1] + " " + which, "no frames");
            return -1;
        }
        long t0 = f.get(0)[0], t95 = -1;
        double target = 1 + (mag - 1) * 0.95;
        long worst = 0;
        int slow = 0;
        for (long[] x : f)
        {
            if (t95 < 0 && x[2] / 1000.0 >= target)
            {
                t95 = x[0] - t0;
            }
            if (x[0] - t0 < 1_000_000_000L)
            {
                worst = Math.max(worst, x[1]);
                slow += x[1] > 50_000_000L ? 1 : 0;
            }
        }
        DevTestResults.value(name(), p[0] + " " + p[1] + " " + which + " aim", String.format(
            "%.2fx: 95%% zoom after %s, worst frame %.1f ms, %d frames over 50 ms in the first second, %d frames",
            mag, t95 < 0 ? "never" : String.format("%.0f ms", t95 / 1e6), worst / 1e6, slow, f.size()));
        return t95;
    }

    private void aim(Minecraft mc, int mode)
    {
        Deci.setAimMode(mc.thePlayer, mode);
        onServer = () -> {
            EntityPlayerMP pl = DevTestUtil.player();
            if (pl != null)
            {
                Deci.setAimMode(pl, mode);
            }
        };
    }

    private static String[][] pairs(String s)
    {
        if (s.trim().isEmpty())
        {
            return DEFAULT;
        }
        List<String[]> out = new ArrayList<String[]>();
        for (String x : s.split(","))
        {
            String[] f = x.trim().split(":");
            if (f.length == 2)
            {
                out.add(f);
            }
        }
        return out.toArray(new String[0][]);
    }
}
