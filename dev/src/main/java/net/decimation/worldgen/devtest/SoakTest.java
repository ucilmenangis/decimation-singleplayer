package net.decimation.worldgen.devtest;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * Frame rate over a long session (bug.md "FPS falls minute by minute", user 19 Oktober 2026): the player flies straight
 * on through the world (new chunks all the time, like exploring) for -Pminutes (default 10) and every 10 s the frame
 * rate, the heap left after the last collection, threads, entities, tile entities, particles and loaded chunks are
 * written ("t=..." values). Whatever keeps growing while the frame rate falls is the lead. -Pspeed blocks a second
 * (default 6), -Pstill=true stays in place.
 */
public class SoakTest extends DevTestMode
{
    public String name() { return "soak"; }

    private final int minutes = Integer.getInteger("deciworldgen.autotest.minutes", 10);
    private final double speed = Double.parseDouble(System.getProperty("deciworldgen.autotest.speed", "6"));
    private final boolean still = Boolean.getBoolean("deciworldgen.autotest.still");
    private int ticks;
    private volatile boolean done, started;
    private double x, z, y;
    private long t0;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (!started)
        {
            return true;
        }
        ticks++;
        if (ticks % 200 == 1)
        {
            sample(mc);
        }
        return !done;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || done)
        {
            return;
        }
        if (!started)
        {
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
            x = p.posX;
            z = p.posZ;
            y = Math.max(p.posY, 90);
            t0 = System.currentTimeMillis();
            started = true;
            return;
        }
        if (!still)
        {
            x += speed / 20.0;
            p.setPositionAndUpdate(x, y, z);
            p.rotationYaw = -90; // looking along +x, the way it flies
        }
        if (System.currentTimeMillis() - t0 >= minutes * 60_000L)
        {
            done = true;
        }
    }

    private void sample(Minecraft mc)
    {
        long s = (System.currentTimeMillis() - t0) / 1000;
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        String v = "fps " + fpsNow() + ", heap after gc " + afterGc() + " MB, used " + used() + " MB, threads "
                   + ManagementFactory.getThreadMXBean().getThreadCount() + ", client entities " + mc.theWorld.loadedEntityList.size()
                   + ", server entities " + w.loadedEntityList.size() + ", client tile entities " + mc.theWorld.loadedTileEntityList.size()
                   + ", server tile entities " + w.loadedTileEntityList.size() + ", particles " + mc.effectRenderer.getStatistics()
                   + ", client chunks " + mc.theWorld.getChunkProvider().getLoadedChunkCount() + ", server chunks "
                   + w.theChunkProviderServer.getLoadedChunkCount();
        DevTestResults.value(name(), "t=" + s + "s", v);
    }

    /** Minecraft's frame counter of the last second (debugFPS). */
    static int fpsNow()
    {
        try
        {
            java.lang.reflect.Field f = Minecraft.class.getDeclaredField("debugFPS");
            f.setAccessible(true);
            return f.getInt(null);
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    private static long used()
    {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) >> 20;
    }

    /** Old generation in use right after its last collection: what really stays. */
    private static long afterGc()
    {
        long sum = 0;
        for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans())
        {
            MemoryUsage u = b.getCollectionUsage();
            String n = b.getName();
            if (u != null && (n.contains("Old") || n.contains("Tenured")))
            {
                sum += u.getUsed();
            }
        }
        return sum >> 20;
    }
}
