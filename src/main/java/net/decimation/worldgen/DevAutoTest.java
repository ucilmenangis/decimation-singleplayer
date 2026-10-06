package net.decimation.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.mod.server.zones.ObjectZone;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/**
 * Unattended singleplayer smoke test, dev only. Does nothing unless the JVM
 * runs with -Ddeciworldgen.autotest=true (set by `gradlew runClient
 * -Pautotest`), so it never touches normal play.
 *
 * Creates a throwaway world "deciworldgen_autotest" (seed 1, which generates
 * POLICE-zoned city structures near spawn), spawns infected inside the first
 * generated zone and far outside any zone, logs the variant counts, then
 * quits the game. Inside a POLICE zone roughly a quarter should come out as
 * the police variant; outside none should.
 */
public class DevAutoTest
{
    public static final String PROPERTY = "deciworldgen.autotest";
    private static final String SAVE = "deciworldgen_autotest";
    private static final int SPAWNS = 40;

    private int clientTicks;
    private int worldTicks;
    private boolean launched;
    private volatile boolean requested;
    private volatile boolean finished;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        clientTicks++;
        // an unfocused window opens the pause menu, and a paused
        // singleplayer server stops ticking
        mc.gameSettings.pauseOnLostFocus = false;
        if (launched && mc.theWorld != null && mc.currentScreen != null && !finished)
        {
            mc.displayGuiScreen(null);
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null && clientTicks > 100)
        {
            launched = true;
            deleteRecursive(new File(mc.mcDataDir, "saves/" + SAVE));
            FMLLog.info("[%s] AUTOTEST creating world %s", DecimationWorldGen.MODID, SAVE);
            mc.launchIntegratedServer(SAVE, SAVE, new WorldSettings(1L,
                WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT));
            return;
        }
        if (launched && mc.theWorld != null && !requested && ++worldTicks > 100)
        {
            requested = true; // the server tick picks this up
        }
        if (finished)
        {
            FMLLog.info("[%s] AUTOTEST done, shutting down", DecimationWorldGen.MODID);
            mc.shutdown();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !requested || finished)
        {
            return;
        }
        try
        {
            if (capCheckIn < 0)
            {
                run(MinecraftServer.getServer().worldServerForDimension(0));
                dropBottlecaps();
                capCheckIn = 60; // give the player a few seconds to pick them up
                return;
            }
            if (--capCheckIn > 0)
            {
                return;
            }
            checkBottlecaps();
        }
        catch (Throwable t)
        {
            FMLLog.info("[%s] AUTOTEST error: %s", DecimationWorldGen.MODID, t);
        }
        finished = true;
    }

    private int capCheckIn = -1;
    private long capsBefore;
    private static final int CAPS = 5;

    private static net.minecraft.entity.player.EntityPlayerMP player()
    {
        List<?> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        return players.isEmpty() ? null : (net.minecraft.entity.player.EntityPlayerMP) players.get(0);
    }

    /** Bottlecap fix: drop caps on the player, they must become balance. */
    private void dropBottlecaps()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            FMLLog.info("[%s] AUTOTEST bottlecaps: no player", DecimationWorldGen.MODID);
            return;
        }
        capsBefore = deci.Q.b.e(p).cb();
        net.minecraft.entity.item.EntityItem item = new net.minecraft.entity.item.EntityItem(
            p.worldObj, p.posX, p.posY, p.posZ,
            new net.minecraft.item.ItemStack(deci.aD.k.aln, CAPS));
        item.delayBeforeCanPickup = 0;
        p.worldObj.spawnEntityInWorld(item);
    }

    private void checkBottlecaps()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        long after = deci.Q.b.e(p).cb();
        int inInventory = 0;
        for (net.minecraft.item.ItemStack st : p.inventory.mainInventory)
        {
            if (st != null && st.getItem() == deci.aD.k.aln)
            {
                inInventory += st.stackSize;
            }
        }
        FMLLog.info("[%s] AUTOTEST bottlecaps: balance %d -> %d (dropped %d), caps left as items: %d",
                    DecimationWorldGen.MODID, capsBefore, after, CAPS, inInventory);
    }

    private void run(WorldServer world)
    {
        FMLLog.info("[%s] AUTOTEST generated zones in this world: %d",
                    DecimationWorldGen.MODID, ZoneStore.size());
        ObjectZone zone = null;
        if (deci.aJ.b.aAc != null)
        {
            for (ObjectZone z : deci.aJ.b.aAc.zoneList)
            {
                if (z.zoneType == net.decimation.mod.server.zones.a.POLICE
                    || z.zoneType == net.decimation.mod.server.zones.a.MILITARY)
                {
                    zone = z;
                    break;
                }
            }
        }
        if (zone == null)
        {
            FMLLog.info("[%s] AUTOTEST no military/police zone found, nothing to test",
                        DecimationWorldGen.MODID);
            return;
        }
        int cx = (zone.zoneX1 + zone.zoneX2) / 2;
        int cz = (zone.zoneZ1 + zone.zoneZ2) / 2;
        int[] inside = spawnBatch(world, cx, cz);
        int[] outside = spawnBatch(world, cx + 3000, cz + 3000);
        FMLLog.info("[%s] AUTOTEST %s zone at %d,%d: inside common=%d military=%d police=%d"
                    + " | outside common=%d military=%d police=%d",
                    DecimationWorldGen.MODID, zone.zoneType.name(), cx, cz,
                    inside[0], inside[1], inside[2], outside[0], outside[1], outside[2]);
    }

    /** Spawns SPAWNS infected at x,z and returns variant counts [common, military, police]. */
    private int[] spawnBatch(WorldServer world, int x, int z)
    {
        int y = world.getTopSolidOrLiquidBlock(x, z) + 1;
        int[] counts = new int[3];
        List<deci.ag.d> spawned = new ArrayList<deci.ag.d>();
        for (int i = 0; i < SPAWNS; i++)
        {
            deci.ag.d infected = new deci.ag.d(world);
            infected.setLocationAndAngles(x + 0.5, y, z + 0.5, 0, 0);
            if (world.spawnEntityInWorld(infected))
            {
                spawned.add(infected);
                int v = infected.eI(); // getVariant
                if (v >= 0 && v < 3)
                {
                    counts[v]++;
                }
            }
        }
        for (deci.ag.d infected : spawned)
        {
            infected.setDead();
        }
        return counts;
    }

    private static void deleteRecursive(File f)
    {
        File[] children = f.listFiles();
        if (children != null)
        {
            for (File c : children)
            {
                deleteRecursive(c);
            }
        }
        f.delete();
    }
}
