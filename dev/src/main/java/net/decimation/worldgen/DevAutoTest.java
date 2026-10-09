package net.decimation.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.worldgen.devtest.CameraViews;
import net.decimation.worldgen.devtest.CityFpsTest;
import net.decimation.worldgen.devtest.DevTestMode;
import net.decimation.worldgen.devtest.DevTestResults;
import net.decimation.worldgen.devtest.DevTestUtil;
import net.decimation.worldgen.devtest.PropsTest;
import net.decimation.worldgen.devtest.ScopeTest;
import net.decimation.worldgen.devtest.ServerChecks;
import net.decimation.worldgen.devtest.TracerTest;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/**
 * Unattended dev tests, dev only: inert unless the JVM runs with
 * -Ddeciworldgen.autotest=true (`gradlew runClient -Pautotest`, or
 * `python3 tools/devtest.py ...`), so it never touches normal play.
 *
 * Runs a list of modes (package devtest) one after another in ONE game
 * session, then quits: -Ptest=checks,views,scope,tracer,props,cityfps. A
 * fresh "deciworldgen_autotest" world (seed 1, Decimation world type) is
 * made when a mode needs it (checks, views); otherwise the last one is
 * reused (faster). Results: run/client/devtest/results.txt (DevTestResults).
 *
 * Without -Ptest the old flags still pick the modes: -Pautotest alone =
 * checks + views; -Pscope adds scope; -Pscopeonly / -Ptracer / -Pprops /
 * -Pcityfps run that mode alone in the reused world; -Paudit / -Pgallery /
 * -Pfootprint / -Pstudy / -Pflats / -Psets choose the camera views.
 */
public class DevAutoTest
{
    public static final String PROPERTY = "deciworldgen.autotest";
    public static final String SAVE = "deciworldgen_autotest";
    /** Infected spawned per zone test batch (ServerChecks). */
    public static final int SPAWNS = 40;

    private volatile List<DevTestMode> modes = modesFromProperties();
    /** Live mode (devtest/DevTestLive): the run that is going, answered when it ends. */
    private net.decimation.worldgen.devtest.DevTestLive.Command liveRun;
    private volatile int current;
    private volatile boolean active;
    private int clientTicks;
    private int worldTicks;
    private boolean launched;

    /** The modes of this launch, in order. */
    private static List<DevTestMode> modesFromProperties()
    {
        String list = System.getProperty(PROPERTY + ".test");
        if (list == null && net.decimation.worldgen.devtest.DevTestLive.enabled())
        {
            list = ""; // live: nothing at launch, runs come over the port
        }
        return modesFromList(list);
    }

    private static List<DevTestMode> modesFromList(String list)
    {
        if (list == null)
        {
            if (flag("scopeonly"))
            {
                list = "scope";
            }
            else if (flag("tracer") || flag("props") || flag("cityfps"))
            {
                list = flag("tracer") ? "tracer" : flag("props") ? "props" : "cityfps";
            }
            else
            {
                list = "checks" + (flag("scope") ? ",scope" : "") + ",views";
            }
        }
        List<DevTestMode> out = new ArrayList<DevTestMode>();
        for (String name : list.split(","))
        {
            if (name.trim().isEmpty())
            {
                continue;
            }
            DevTestMode m = mode(name.trim());
            if (m != null)
            {
                out.add(m);
            }
            else
            {
                FMLLog.info("[%s] AUTOTEST unknown test mode %s", DecimationWorldGen.MODID, name);
            }
        }
        return out;
    }

    private static boolean flag(String name)
    {
        return "true".equals(System.getProperty(PROPERTY + "." + name));
    }

    private static DevTestMode mode(String name)
    {
        if (name.equals("checks")) return new ServerChecks();
        if (name.equals("views")) return new CameraViews();
        if (name.equals("scope")) return new ScopeTest();
        if (name.equals("tracer")) return new TracerTest();
        if (name.equals("props")) return new PropsTest();
        if (name.equals("cityfps")) return new CityFpsTest();
        if (name.equals("npc")) return new net.decimation.worldgen.devtest.NpcTest();
        if (name.equals("shots")) return new net.decimation.worldgen.devtest.ShotTest();
        if (name.equals("zombies")) return new net.decimation.worldgen.devtest.ZombieTest();
        if (name.equals("gun")) return new net.decimation.worldgen.devtest.GunTest();
        if (name.equals("gunview")) return new net.decimation.worldgen.devtest.GunViewTest();
        if (name.equals("gunperf")) return new net.decimation.worldgen.devtest.GunPerfTest();
        if (name.equals("census")) return new net.decimation.worldgen.devtest.CensusTest();
        if (name.equals("cityview")) return new net.decimation.worldgen.devtest.CityViewTest();
        return null;
    }

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
        if (launched && mc.theWorld != null && mc.currentScreen != null)
        {
            mc.displayGuiScreen(null);
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null && clientTicks > 100)
        {
            launched = true;
            launch(mc);
            return;
        }
        if (launched && mc.theWorld != null && !active && ++worldTicks > 100)
        {
            active = true; // chunks around the player are in; modes start
        }
        if (!active)
        {
            return;
        }
        while (current < modes.size())
        {
            if (modes.get(current).client(mc))
            {
                return;
            }
            DevTestResults.value(modes.get(current).name(), "done", "");
            current++;
        }
        if (net.decimation.worldgen.devtest.DevTestLive.enabled())
        {
            live(mc);
            return;
        }
        FMLLog.info("[%s] AUTOTEST done, shutting down", DecimationWorldGen.MODID);
        mc.shutdown();
    }

    /** Live mode: report the finished run, then take the next command (one per tick at most). */
    private void live(Minecraft mc)
    {
        if (liveRun != null)
        {
            liveRun.finished();
            liveRun = null;
        }
        net.decimation.worldgen.devtest.DevTestLive.idle = true;
        net.decimation.worldgen.devtest.DevTestLive.Command c = net.decimation.worldgen.devtest.DevTestLive.QUEUE.poll();
        if (c == null)
        {
            return;
        }
        if (c.quit)
        {
            FMLLog.info("[%s] AUTOTEST live: quit", DecimationWorldGen.MODID);
            mc.shutdown();
            return;
        }
        net.decimation.worldgen.devtest.DevTestLive.idle = false;
        DevTestResults.start(mc.mcDataDir);
        DevTestResults.value("run", "modes", c.modes + " (live)");
        List<DevTestMode> next = modesFromList(c.modes);
        current = 0;
        modes = next;
        liveRun = c;
        FMLLog.info("[%s] AUTOTEST live: running %s", DecimationWorldGen.MODID, c.modes);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        int i = current;
        if (event.phase == TickEvent.Phase.END && active && i < modes.size())
        {
            modes.get(i).server();
        }
    }

    /** Opens the world the modes need: a study map, the fresh test world or the last one. */
    private void launch(Minecraft mc)
    {
        if (net.decimation.worldgen.devtest.DevTestLive.enabled())
        {
            net.decimation.worldgen.devtest.DevTestLive.start(mc.mcDataDir);
        }
        DevTestResults.start(mc.mcDataDir);
        StringBuilder names = new StringBuilder();
        boolean fresh = false;
        for (DevTestMode m : modes)
        {
            names.append(names.length() == 0 ? "" : ",").append(m.name());
            fresh |= m.freshWorld();
        }
        DevTestResults.value("run", "modes", names);
        if (CameraViews.study() != null)
        {
            // open a copied reference map as it is (no new world)
            FMLLog.info("[%s] AUTOTEST study: opening %s", DecimationWorldGen.MODID, CameraViews.study());
            mc.launchIntegratedServer(CameraViews.study(), CameraViews.study(), null);
            return;
        }
        if (!fresh && new File(mc.mcDataDir, "saves/" + SAVE).isDirectory())
        {
            FMLLog.info("[%s] AUTOTEST reusing world %s", DecimationWorldGen.MODID, SAVE);
            mc.launchIntegratedServer(SAVE, SAVE, null);
            return;
        }
        DevTestUtil.deleteRecursive(new File(mc.mcDataDir, "saves/" + SAVE));
        FMLLog.info("[%s] AUTOTEST creating world %s", DecimationWorldGen.MODID, SAVE);
        // -Ddeciworldgen.autotest.type=default tests on vanilla terrain
        WorldType type = "default".equalsIgnoreCase(System.getProperty(PROPERTY + ".type"))
            ? WorldType.DEFAULT : net.decimation.worldgen.terrain.DecimationWorldType.INSTANCE;
        FMLLog.info("[%s] AUTOTEST world type %s", DecimationWorldGen.MODID, type.getWorldTypeName());
        mc.launchIntegratedServer(SAVE, SAVE, new WorldSettings(1L, WorldSettings.GameType.CREATIVE, true, false, type));
    }
}
