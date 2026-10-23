package net.decimation.worldgen.devtest;

import java.util.Arrays;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * Frame time bench (-Ptest=fpsbench), for FPS changes that are smaller than cityfps's run to run spread (the same
 * build read 117 and 138 there): in the last autotest world (seed 1 city around x 8, z 120), noon, no weather,
 * unlimited frame rate. Three still views (down the street north, south, over the roofs), each 8 s to let the chunks
 * build then 10 s measured, and a walk north along the street at 5 blocks a second for 15 s, measured whole (chunks
 * building while moving). Every frame is timed (render tick start to start): average fps and the 1 % low (fps of the
 * 99th percentile frame). Run it a few times and compare medians.
 */
public class FpsBenchTest extends DevTestMode
{
    /** x, y, z, yaw, pitch of the still views. */
    private static final double[][] VIEWS = {{8.5, 68, 120.5, 180, 3}, {8.5, 68, 120.5, 0, 3}, {8.5, 115, 140.5, 180, 30}};
    private static final String[] NAMES = {"street north", "street south", "roofs"};
    private static final int SETTLE = 160, MEASURE = 200, WALK = 300;
    private static final double SPEED = 0.25;   // blocks a tick

    /** Public: FML's event bus calls it from a generated class. */
    public static final class Frames
    {
        final long[] ms = new long[20000];
        int n;
        long last;
        boolean on;

        @SubscribeEvent
        public void onFrame(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START)
            {
                return;
            }
            long now = System.nanoTime();
            if (on && last != 0 && n < ms.length)
            {
                ms[n++] = (now - last) / 1000;   // microseconds
            }
            last = now;
        }
    }

    private final Frames frames = new Frames();
    private int view = 0, ticks = 0;
    private boolean registered, done;

    public String name() { return "fpsbench"; }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (!registered)
        {
            FMLCommonHandler.instance().bus().register(frames);
            mc.gameSettings.limitFramerate = 260;   // unlimited
            mc.gameSettings.enableVsync = false;
            registered = true;
        }
        ticks++;
        boolean walk = view == VIEWS.length;
        double[] v = walk ? new double[] {8.5, 68, 120.5 - SPEED * Math.max(0, ticks - SETTLE), 180, 3} : VIEWS[view];
        place(mc, v, ticks == 1 || walk);
        if (ticks == SETTLE)
        {
            frames.n = 0;
            frames.last = 0;
            frames.on = true;
        }
        int end = SETTLE + (walk ? WALK : MEASURE);
        if (ticks == end)
        {
            frames.on = false;
            report(walk ? "walk north" : NAMES[view]);
            if (!walk)
            {
                DevTestUtil.screenshot(mc, name(), "fpsbench_" + view);
            }
            view++;
            ticks = 0;
            done = view > VIEWS.length;
        }
        return !done;
    }

    /** The player at a view (flying, noon, clear), on both sides when it moves. */
    private static void place(Minecraft mc, double[] v, boolean server)
    {
        if (server)
        {
            EntityPlayerMP sp = (EntityPlayerMP) MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            sp.worldObj.setWorldTime(6000);
            sp.worldObj.getWorldInfo().setRaining(false);
            sp.worldObj.getWorldInfo().setThundering(false);
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(v[0], v[1], v[2]);
        }
        mc.thePlayer.capabilities.isFlying = true;
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = (float) v[3];
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = (float) v[4];
    }

    private void report(String what)
    {
        long[] f = Arrays.copyOf(frames.ms, frames.n);
        long total = 0;
        for (long x : f)
        {
            total += x;
        }
        Arrays.sort(f);
        double avg = f.length == 0 ? 0 : f.length * 1e6 / total;
        double low = f.length == 0 ? 0 : 1e6 / f[Math.min(f.length - 1, (int) (f.length * 0.99))];
        DevTestResults.value(name(), what, String.format("%.0f fps, 1%% low %.0f (%d frames)%s", avg, low, f.length, culling()));
    }

    /** DeciRemnant Optimized's counts (Fabric only, by reflection): EntityCulling's last pass, ChunkVBO's last frame. */
    private static String culling()
    {
        StringBuilder s = new StringBuilder();
        try
        {
            Class<?> c = Class.forName("net.decimation.optimized.entityculling.Culler");
            s.append(", culled ").append(c.getField("culled").get(null)).append(" of ").append(c.getField("checked").get(null));
        }
        catch (ReflectiveOperationException ex)
        {
            // EntityCulling not there
        }
        try
        {
            Class<?> c = Class.forName("net.decimation.optimized.chunkvbo.ChunkVbo");
            s.append(", sections from buffers ").append(c.getField("fromBuffers").get(null))
             .append(" / lists ").append(c.getField("fromLists").get(null));
        }
        catch (ReflectiveOperationException ex)
        {
            // ChunkVBO not there
        }
        s.append(", window focused ").append(org.lwjgl.opengl.Display.isActive());   // Dynamic FPS caps unfocused
        return s.toString();
    }
}
