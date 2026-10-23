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
 * fresh "deciworldgen_devtest" world (seed 1, Decimation world type) is
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
    // the tests' OWN world (user 2026-10-11): until v0.42.4 the tests used deciworldgen_autotest,
    // which the user also plays in; a fresh-world test deletes this folder, and the test game's
    // quit once broke that world's level.dat (docs/performance.md is unrelated; see bug.md)
    public static final String SAVE = "deciworldgen_devtest";
    /** A test mode showing a screen on purpose (stats: the home menu) sets this while it does. */
    public static volatile boolean keepScreen;
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
        if (name.equals("firerate")) return new net.decimation.worldgen.devtest.FireRateTest();
        if (name.equals("milbase")) return new net.decimation.worldgen.devtest.MilBaseTest();
        if (name.equals("stats")) return new net.decimation.worldgen.devtest.StatsTest();
        if (name.equals("loot")) return new net.decimation.worldgen.devtest.LootTest();
        if (name.equals("playerfire")) return new net.decimation.worldgen.devtest.PlayerFireTest();
        if (name.equals("drive")) return new net.decimation.worldgen.devtest.DriveTest();
        if (name.equals("trade")) return new net.decimation.worldgen.devtest.TradeTest();
        if (name.equals("useitems")) return new net.decimation.worldgen.devtest.UseItemsTest();
        if (name.equals("keycard")) return new net.decimation.worldgen.devtest.KeycardTest();
        if (name.equals("items")) return new net.decimation.worldgen.devtest.ItemsTest();
        if (name.equals("hardness")) return new net.decimation.worldgen.devtest.HardnessTest();
        if (name.equals("invshift")) return new net.decimation.worldgen.devtest.InvShiftTest();
        if (name.equals("invshiftmp")) return new net.decimation.worldgen.devtest.InvShiftRemoteTest();
        if (name.equals("gripsweep")) return new net.decimation.worldgen.devtest.GripSweepTest();
        if (name.equals("core")) return new net.decimation.worldgen.devtest.CoreTest();
        if (name.equals("menu")) return new net.decimation.worldgen.devtest.MenuTest();
        if (name.equals("click")) return new net.decimation.worldgen.devtest.ClickTest();
        if (name.equals("hitbox")) return new net.decimation.worldgen.devtest.HitboxTest();
        if (name.equals("update")) return new net.decimation.worldgen.devtest.UpdateTest();
        if (name.equals("npckill")) return new net.decimation.worldgen.devtest.NpcKillTest();
        if (name.equals("noise")) return new net.decimation.worldgen.devtest.NoiseTest();
        if (name.equals("aimdrift")) return new net.decimation.worldgen.devtest.AimDriftTest();
        if (name.equals("census")) return new net.decimation.worldgen.devtest.CensusTest();
        if (name.equals("cityview")) return new net.decimation.worldgen.devtest.CityViewTest();
        if (name.equals("siteshots")) return new net.decimation.worldgen.devtest.SiteShotsTest();
        if (name.equals("guideshots")) return new net.decimation.worldgen.devtest.GuideShotsTest();
        if (name.equals("mainmenu")) return new net.decimation.worldgen.devtest.MainMenuTest();
        if (name.equals("menupages")) return new net.decimation.worldgen.devtest.MenuPagesTest();
        if (name.equals("doublejoin")) return new net.decimation.worldgen.devtest.DoubleJoinTest();
        if (name.equals("nospawn")) return new net.decimation.worldgen.devtest.NoSpawnTest();
        if (name.equals("corpsegun")) return new net.decimation.worldgen.devtest.CorpseGunTest();
        if (name.equals("lanhost")) return new net.decimation.worldgen.devtest.LanHostTest();
        if (name.equals("lanjoin")) return new net.decimation.worldgen.devtest.LanJoinTest();
        if (name.equals("hostmenu")) return new net.decimation.worldgen.devtest.HostMenuTest();
        if (name.equals("hudguard")) return new net.decimation.worldgen.devtest.HudGuardTest();
        if (name.equals("glstack")) return new net.decimation.worldgen.devtest.GlStackTest();
        if (name.equals("throwables")) return new net.decimation.worldgen.devtest.ThrowablesTest();
        if (name.equals("soak")) return new net.decimation.worldgen.devtest.SoakTest();
        if (name.equals("radios")) return new net.decimation.worldgen.devtest.RadiosTest();
        if (name.equals("threads")) return new net.decimation.worldgen.devtest.ThreadsTest();
        if (name.equals("scopeperf")) return new net.decimation.worldgen.devtest.ScopePerfTest();
        if (name.equals("armskin")) return new net.decimation.worldgen.devtest.ArmSkinTest();
        if (name.equals("zoneperf")) return new net.decimation.worldgen.devtest.ZonePerfTest();
        if (name.equals("traderperf")) return new net.decimation.worldgen.devtest.TraderPerfTest();
        if (name.equals("scopediag")) return new net.decimation.worldgen.devtest.ScopeDiagTest();
        if (name.equals("aimdelay")) return new net.decimation.worldgen.devtest.AimDelayTest();
        if (name.equals("lootmags")) return new net.decimation.worldgen.devtest.LootMagsTest();
        if (name.equals("lootrules")) return new net.decimation.worldgen.devtest.LootRulesTest(); // feature 12 A: looted stays looted
        if (name.equals("lootblocks")) return new net.decimation.worldgen.devtest.LootBlocksLiveTest(); // feature 12 B: copies, kinds
        if (name.equals("lootsearch")) return new net.decimation.worldgen.devtest.LootSearchLiveTest(); // feature 12 C: search
        if (name.equals("market")) return new net.decimation.worldgen.devtest.MarketTest(); // feature 11: trader prices by day
        if (name.equals("magunload")) return new net.decimation.worldgen.devtest.MagUnloadTest();
        if (name.equals("radiation")) return new net.decimation.worldgen.devtest.RadiationTest();
        if (name.equals("channelmp")) return new net.decimation.worldgen.devtest.ChannelRemoteTest();
        if (name.equals("skins")) return new net.decimation.worldgen.devtest.SkinsTest();
        if (name.equals("nights")) return new net.decimation.worldgen.devtest.NightsTest();
        if (name.equals("durability")) return new net.decimation.worldgen.devtest.DurabilityLiveTest();
        if (name.equals("radset")) return new net.decimation.worldgen.devtest.RadSetTest(); // feature 7: pills, filters, wasteland
        if (name.equals("placespawns")) return new net.decimation.worldgen.devtest.PlaceSpawnTest(); // feature 8: mobs by place
        if (name.equals("bench")) return new net.decimation.worldgen.devtest.BenchTest();
        if (name.equals("targetpace")) return new net.decimation.worldgen.devtest.TargetPaceTest();
        if (name.equals("npclook")) return new net.decimation.worldgen.devtest.NpcLookTest();
        if (name.equals("npcdraw")) return new net.decimation.worldgen.devtest.NpcDrawTest();
        if (name.equals("lightleak")) return new net.decimation.worldgen.devtest.LightLeakTest();
        if (name.equals("genwalk")) return new net.decimation.worldgen.devtest.GenWalkTest();
        if (name.equals("refugevisit")) return new net.decimation.worldgen.devtest.RefugeVisitTest();
        if (name.equals("cornerart")) return new net.decimation.worldgen.devtest.CornerArtTest();
        if (name.equals("melee")) return new net.decimation.worldgen.devtest.MeleeTest();
        if (name.equals("fpsbench")) return new net.decimation.worldgen.devtest.FpsBenchTest();
        if (name.equals("fastquit")) return new net.decimation.worldgen.devtest.FastQuitTest();
        if (name.equals("culltest")) return new net.decimation.worldgen.devtest.CullTest();
        if (name.equals("gundraw")) return new net.decimation.worldgen.devtest.GunDrawTest();
        if (name.equals("hud")) return new net.decimation.worldgen.devtest.HudTest();
        if (name.equals("wounds")) return new net.decimation.worldgen.devtest.WoundsTest();
        if (name.equals("gunsmith")) return new net.decimation.worldgen.devtest.GunsmithTest();
        if (name.equals("attachments")) return new net.decimation.worldgen.devtest.AttachmentsTest();
        if (name.equals("gunsmithmp")) return new net.decimation.worldgen.devtest.GunsmithRemoteTest();
        if (name.equals("attachmentsmp")) return new net.decimation.worldgen.devtest.AttachmentsRemoteTest();
        if (name.equals("gunbatch")) return new net.decimation.worldgen.devtest.NpcLookTest(true);
        if (name.equals("hostops")) return new net.decimation.worldgen.devtest.HostOpsTest();
        if (name.equals("hostshots")) return new net.decimation.worldgen.devtest.HostShotsTest();
        if (name.equals("garrison")) return new net.decimation.worldgen.devtest.GarrisonTest();
        if (name.equals("rail")) return new net.decimation.worldgen.devtest.RailTest();
        if (name.equals("bridge")) return new net.decimation.worldgen.devtest.BridgeTest();
        if (name.equals("cityprops")) return new net.decimation.worldgen.devtest.CityPropsTest();
        if (name.equals("wasteland")) return new net.decimation.worldgen.devtest.WastelandTest();
        if (name.equals("cityedge")) return new net.decimation.worldgen.devtest.CityEdgeTest();
        if (name.equals("facing")) return new net.decimation.worldgen.devtest.FacingTest();
        if (name.equals("places")) return new net.decimation.worldgen.devtest.PlacesTest();
        if (name.equals("zones")) return new net.decimation.worldgen.devtest.ZonesTest();
        if (name.equals("camp")) return new net.decimation.worldgen.devtest.CampTest();
        if (name.equals("campcmd")) return new net.decimation.worldgen.devtest.CampCommandTest();
        if (name.equals("refuge")) return new net.decimation.worldgen.devtest.RefugeTest();
        if (name.equals("gates")) return new net.decimation.worldgen.devtest.GateTest();
        if (name.equals("gatewalk")) return new net.decimation.worldgen.devtest.GateWalkTest();
        if (name.equals("airport")) return new net.decimation.worldgen.devtest.AirportTest();
        // gun packages (fixes/gunpack): the loader, every package's gun against its like gun, rows, a reload and a shot
        if (name.equals("gunpack")) return new net.decimation.worldgen.devtest.GunPackLiveTest();
        if (name.equals("meshgun")) return new net.decimation.worldgen.devtest.MeshGunLiveTest();
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
        if (launched && mc.theWorld != null && mc.currentScreen != null && !keepScreen)
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
            screenCheck = modes.get(current).name();
            current++;
        }
        if (quitting)
        {
            quitSafely(mc);
            return;
        }
        if (net.decimation.worldgen.devtest.DevTestLive.enabled())
        {
            live(mc);
            return;
        }
        FMLLog.info("[%s] AUTOTEST done, shutting down", DecimationWorldGen.MODID);
        quitSafely(mc);
    }

    /** A live quit was asked for: quitSafely runs every tick until the game closes. */
    private boolean quitting;

    /** The integrated server being stopped by quitSafely, or null. */
    private net.minecraft.server.integrated.IntegratedServer stopping;
    private int stopTicks;

    /**
     * Leaves the world, waits until the integrated server has saved and stopped, then closes the
     * game. mc.shutdown() from inside a world let the server's save and the shutdown hook's save
     * write level.dat at the same moment: on 11 Oktober 2026 that left a level.dat whose item id
     * table missed 603 entries, and the next start failed ("Can't map item deci:ItemSelectionTool
     * to id -1"). Called every client tick until it shuts the game down.
     */
    private void quitSafely(Minecraft mc)
    {
        if (stopping == null && mc.theWorld != null)
        {
            stopping = mc.getIntegratedServer();
            FMLLog.info("[%s] AUTOTEST leaving the world before closing", DecimationWorldGen.MODID);
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld((net.minecraft.client.multiplayer.WorldClient) null);
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
            return;
        }
        if (stopping != null && !stopping.isServerStopped() && stopTicks++ < 20 * 60)
        {
            return;                                   // still saving
        }
        FMLLog.info("[%s] AUTOTEST world saved and closed, shutting down", DecimationWorldGen.MODID);
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
            quitting = true;
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

    /**
     * The mode that just ended, for a look at its player's server side screen (null when none is due). The harness closes
     * client screens with displayGuiScreen(null), which never tells the server: a container a mode left open stayed open
     * there, and every later mode that needs no screen open failed (radset, 23 Oktober 2026: filter wear and fitting).
     */
    private volatile String screenCheck;

    /**
     * A container still open on the server after a mode: named in the results, then closed as Esc would. Its fall
     * distance cleared too: genwalk flies in creative, and the next mode to switch to survival died on landing.
     */
    private void closeLeftoverScreen(String mode)
    {
        net.minecraft.entity.player.EntityPlayerMP p = net.decimation.worldgen.devtest.DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        p.fallDistance = 0;
        if (p.openContainer != null && p.openContainer != p.inventoryContainer)
        {
            DevTestResults.value(mode, "server screen left open", p.openContainer.getClass().getName() + " (closed)");
            p.closeScreen();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        String ended = screenCheck;
        if (event.phase == TickEvent.Phase.END && ended != null)
        {
            screenCheck = null;
            closeLeftoverScreen(ended);
        }
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
