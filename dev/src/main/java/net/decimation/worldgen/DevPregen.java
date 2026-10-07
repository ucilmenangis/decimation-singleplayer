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
 * -Ppregen=x,z,r`, see tools/servertest.py). Server side only classes.
 */
public class DevPregen
{
    public static final String PROPERTY = "deciworldgen.pregen";
    private boolean done;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (done || event.phase != TickEvent.Phase.END)
        {
            return;
        }
        done = true;
        String[] p = System.getProperty(PROPERTY, "").split(",");
        if (p.length != 3)
        {
            return;
        }
        int cx = Integer.parseInt(p[0].trim()) >> 4, cz = Integer.parseInt(p[1].trim()) >> 4;
        int r = Integer.parseInt(p[2].trim());
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        int n = 0;
        // one ring wider than asked, so every asked chunk gets populated
        for (int x = cx - r - 1; x <= cx + r + 1; x++)
        {
            for (int z = cz - r - 1; z <= cz + r + 1; z++)
            {
                world.theChunkProviderServer.loadChunk(x, z);
                n++;
            }
        }
        world.theChunkProviderServer.saveChunks(true, null);
        FMLLog.info("[%s] PREGEN done: %d chunks around %s", DecimationWorldGen.MODID, n, System.getProperty(PROPERTY));
    }
}
