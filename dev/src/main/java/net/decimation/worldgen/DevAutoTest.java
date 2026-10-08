package net.decimation.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.worldgen.building.Building;
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
        if (launched && mc.theWorld != null && mc.currentScreen != null)
        {
            mc.displayGuiScreen(null);
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null && clientTicks > 100)
        {
            launched = true;
            if (STUDY != null)
            {
                // open a copied reference map as it is (no tests, no new world)
                FMLLog.info("[%s] AUTOTEST study: opening %s", DecimationWorldGen.MODID, STUDY[0]);
                mc.launchIntegratedServer(STUDY[0], STUDY[0], null);
                return;
            }
            deleteRecursive(new File(mc.mcDataDir, "saves/" + SAVE));
            FMLLog.info("[%s] AUTOTEST creating world %s", DecimationWorldGen.MODID, SAVE);
            // -Ddeciworldgen.autotest.type=default tests on vanilla terrain
            WorldType type = "default".equalsIgnoreCase(System.getProperty(PROPERTY + ".type"))
                ? WorldType.DEFAULT : net.decimation.worldgen.terrain.DecimationWorldType.INSTANCE;
            FMLLog.info("[%s] AUTOTEST world type %s", DecimationWorldGen.MODID, type.getWorldTypeName());
            mc.launchIntegratedServer(SAVE, SAVE, new WorldSettings(1L,
                WorldSettings.GameType.CREATIVE, true, false, type));
            return;
        }
        if (launched && mc.theWorld != null && !requested && ++worldTicks > 100)
        {
            requested = true; // the server tick picks this up
        }
        if (finished && SCOPE && !scopeDone && scopeTest(mc))
        {
            return;
        }
        if (finished)
        {
            if (takeViews(mc))
            {
                return;
            }
            FMLLog.info("[%s] AUTOTEST done, shutting down", DecimationWorldGen.MODID);
            mc.shutdown();
        }
    }

    // ---- scope test (-Pscope): fps with an empty hand, a gun with a 4x
    // scope held, and aiming through it, plus the FOV zoom and a screenshot
    // (scope_<pip|zoom>.png). Run once per config/deciworldgen_scope.cfg
    // setting to compare Decimation's picture in picture scope with the zoom.
    private static final boolean SCOPE = "true".equals(System.getProperty(PROPERTY + ".scope"));
    private static final int SCOPE_PHASE = 400;
    private int scopeTicks;
    private final float[] scopeFps = new float[4];
    private final int[] scopeSamples = new int[4];
    private boolean scopeDone;
    private float scopeFovMul;

    /** Client side: true while the scope test runs. */
    private boolean scopeTest(Minecraft mc)
    {
        int phase = scopeTicks / SCOPE_PHASE, t = scopeTicks % SCOPE_PHASE;
        scopeTicks++;
        net.minecraft.entity.player.EntityPlayerMP sp = MinecraftServer.getServer() == null ? null
            : (net.minecraft.entity.player.EntityPlayerMP) MinecraftServer.getServer().getConfigurationManager()
                .playerEntityList.get(0);
        if (mc.thePlayer == null || sp == null)
        {
            return false;
        }
        if (scopeTicks == 1)
        {
            mc.gameSettings.limitFramerate = 260; // unlimited
            mc.gameSettings.enableVsync = false;
            org.lwjgl.opengl.Display.setVSyncEnabled(false);
            mc.gameSettings.hideGUI = false; // hideGUI also hides the hand (gun and scope)
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(8.5, 72, 40.5);
        }
        mc.thePlayer.capabilities.isFlying = true;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 180;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 2;
        // phases: empty hand, 4x gun held, aiming, empty hand again (drift check)
        if (phase < 4 && t == 0)
        {
            net.minecraft.item.ItemStack gun = phase == 0 || phase == 3 ? null : scopedGun();
            for (net.minecraft.entity.player.EntityPlayer p : new net.minecraft.entity.player.EntityPlayer[] {mc.thePlayer, sp})
            {
                p.inventory.currentItem = 0;
                p.inventory.setInventorySlotContents(0, gun == null ? null : gun.copy());
                net.decimation.fixes.Deci.setAimMode(p, phase == 2 ? 1 : 0);
            }
        }
        if (phase < 4)
        {
            // keep it: the server may sync its own value back
            net.decimation.fixes.Deci.setAimMode(mc.thePlayer, phase == 2 ? 1 : 0);
            if (t >= 120 && t % 20 == 0)
            {
                String dbg = mc.debug; // "N fps, M chunk updates"
                scopeFps[phase] += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
                scopeSamples[phase]++;
            }
            if (phase == 2 && t == 300)
            {
                scopeFovMul = (Float) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                    net.minecraft.client.renderer.EntityRenderer.class, mc.entityRenderer, "fovModifierHand",
                    "field_78507_R");
                String mode = Boolean.getBoolean(net.decimation.fixes.ScopeZoom.PROPERTY) ? "pip" : "zoom";
                net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, "scope_" + mode + ".png",
                    mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            }
            return true;
        }
        scopeDone = true;
        FMLLog.info("[%s] AUTOTEST scope %s: fps empty hand %.0f, gun with 4x held %.0f, aiming %.0f, empty hand "
                    + "again %.0f; fov multiplier aiming %.3f", DecimationWorldGen.MODID,
                    Boolean.getBoolean(net.decimation.fixes.ScopeZoom.PROPERTY) ? "picture in picture" : "view zoom",
                    scopeFps[0] / Math.max(1, scopeSamples[0]), scopeFps[1] / Math.max(1, scopeSamples[1]),
                    scopeFps[2] / Math.max(1, scopeSamples[2]), scopeFps[3] / Math.max(1, scopeSamples[3]),
                    scopeFovMul);
        for (net.minecraft.entity.player.EntityPlayer p : new net.minecraft.entity.player.EntityPlayer[] {mc.thePlayer, sp})
        {
            p.inventory.setInventorySlotContents(0, null);
            net.decimation.fixes.Deci.setAimMode(p, 0);
        }
        mc.gameSettings.hideGUI = false;
        return false;
    }

    /** A rifle without an integrated scope, carrying the 4x sight attachment. */
    private static net.minecraft.item.ItemStack scopedGun()
    {
        net.minecraft.item.Item gun = null, sight = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            net.minecraft.item.Item item = (net.minecraft.item.Item) o;
            if (gun == null && net.decimation.fixes.Deci.isGun(item) && !net.decimation.fixes.Deci.hasIntegratedScope(item))
            {
                gun = item;
            }
            if (net.decimation.fixes.Deci.isAttachment(item) && "4x".equals(net.decimation.fixes.Deci.attachmentName(item)))
            {
                sight = item;
            }
        }
        net.minecraft.item.ItemStack st = new net.minecraft.item.ItemStack(gun);
        st.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
        String name = net.minecraft.item.Item.itemRegistry.getNameForObject(sight);
        st.stackTagCompound.setString("sightAttach", name.substring(name.indexOf(':') + 1));
        FMLLog.info("[%s] AUTOTEST scope: gun %s, sight %s", DecimationWorldGen.MODID,
                    net.minecraft.item.Item.itemRegistry.getNameForObject(gun), name);
        return st;
    }

    // ---- screenshots of a city street, so street furniture facing can be
    // checked without a person: <run dir>/screenshots/autotest_<view>.png

    /** View: offset from a north-south street centre (x, height above ground, z), yaw, pitch. */
    private static final float LAMP = 999;
    private static final float[][] VIEWS = {
        {4, 3, 0, 180, 12},   // from the east sidewalk, looking north along the street
        {LAMP, 0, 0, 0, 0},   // side-on view of the nearest street light (arm direction)
        {-3, 3, 0, 270, 10},  // on the west sidewalk, looking east across the street (levelling)
    };
    private volatile int viewRequested = -1;
    private volatile int viewReady = -1;
    private int view;
    private int viewWait;

    /** Client side: one view after another; false once all are saved. */
    private boolean takeViews(Minecraft mc)
    {
        int views = STUDY != null ? studyCams().size() : FLATS ? FLATS_VIEWS : SETS ? net.decimation.worldgen.sets.FurnitureSets.all().size()
            : FOOTPRINT ? galleryNames().size() + 1
            : GALLERY ? (galleryNames().size() + GALLERY_PER_VIEW - 1) / GALLERY_PER_VIEW
            : AUDIT ? AUDIT_VIEWS : VIEWS.length;
        if (view >= views || "false".equals(System.getProperty(PROPERTY + ".views")))
        {
            if (savedFov > 0)
            {
                // the dev client is also used for play: leave its options as they were
                mc.gameSettings.fovSetting = savedFov;
                mc.gameSettings.gammaSetting = savedGamma;
                mc.gameSettings.hideGUI = false;
                mc.gameSettings.particleSetting = savedParticles;
                mc.gameSettings.ambientOcclusion = savedAo;
                mc.gameSettings.clouds = savedClouds;
                savedFov = 0;
            }
            return false;
        }
        if (savedFov == 0)
        {
            savedFov = mc.gameSettings.fovSetting;
            savedGamma = mc.gameSettings.gammaSetting;
            savedParticles = mc.gameSettings.particleSetting;
            savedAo = mc.gameSettings.ambientOcclusion;
            savedClouds = mc.gameSettings.clouds;
        }
        mc.gameSettings.hideGUI = true;
        if (FOOTPRINT)
        {
            mc.gameSettings.fovSetting = 16.0F; // narrow: one cell, little parallax
            mc.gameSettings.ambientOcclusion = 0; // no dark halo on the floor around props
            mc.gameSettings.clouds = false;
        }
        if (AUDIT || GALLERY || SETS || FLATS || FOOTPRINT || STUDY != null)
        {
            mc.gameSettings.gammaSetting = 1.0F;
        }
        if (STUDY != null)
        {
            // unlit rooms of a reference map photograph black at slider
            // gamma; above 1 the lightmap is close to full bright (no
            // night vision potion: its swirls sat in front of the lens)
            mc.gameSettings.gammaSetting = 8.0F;
            mc.gameSettings.particleSetting = 2;
        }
        if (GALLERY || SETS)
        {
            mc.gameSettings.fovSetting = 45.0F;
        }
        if (ONLY != null && !ONLY.contains(view))
        {
            view++;
            return true;
        }
        if (viewRequested < view)
        {
            viewRequested = view;
            viewWait = 0;
            return true;
        }
        if (viewReady >= view && mc.thePlayer != null)
        {
            // hold the camera: a touched mouse or touchpad must not turn the shot
            mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = viewYaw;
            mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = viewPitch;
        }
        if (viewReady < view || ++viewWait < (FOOTPRINT ? (shotsTaken > 0 ? 40 : 500) : (GALLERY || SETS) && shotsTaken > 0 ? 80 : GALLERY || SETS ? 500 : 260))
        {
            return true; // teleport pending, then let the chunks render
        }
        if (viewSkip)
        {
            FMLLog.info("[%s] AUTOTEST view %d skipped: %s", DecimationWorldGen.MODID, view, viewSpot);
            view++;
            return true;
        }
        net.minecraft.util.IChatComponent msg = net.minecraft.util.ScreenShotHelper.saveScreenshot(
            mc.mcDataDir, (FOOTPRINT ? "footprint_" : STUDY != null ? "study_" : FLATS ? "flat_" : SETS ? "set_" : GALLERY ? "gallery_" : AUDIT ? "audit_" : "autotest_") + view + ".png",
            mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
        FMLLog.info("[%s] AUTOTEST view %d at %s: %s", DecimationWorldGen.MODID, view, viewSpot,
                    msg == null ? "?" : msg.getUnformattedText());
        view++;
        shotsTaken++;
        return true;
    }

    private int shotsTaken;
    private volatile String viewSpot = "";
    private float savedFov, savedGamma;
    private int savedParticles, savedAo;
    private boolean savedClouds;
    private volatile float viewYaw, viewPitch;
    /** -Ddeciworldgen.autotest.only=0,23,70-76: take only these views (re-shoots). */
    private static final java.util.Set<Integer> ONLY = parseOnly(System.getProperty(PROPERTY + ".only"));

    private static java.util.Set<Integer> parseOnly(String spec)
    {
        if (spec == null || spec.isEmpty())
        {
            return null;
        }
        java.util.Set<Integer> out = new java.util.HashSet<Integer>();
        for (String part : spec.split(","))
        {
            String[] r = part.trim().split("-");
            int a = Integer.parseInt(r[0]), b = r.length > 1 ? Integer.parseInt(r[1]) : a;
            for (int i = a; i <= b; i++)
            {
                out.add(i);
            }
        }
        return out;
    }
    private boolean peaceful;
    private volatile boolean viewSkip;

    // ---- gallery mode (-Ddeciworldgen.autotest.gallery=true): every
    // Decimation block on a sky platform, 5 per screenshot (gallery_<n>.png),
    // names logged per view, for the prop catalogue (docs/prop_catalogue.md)
    private static final boolean GALLERY = "true".equals(System.getProperty(PROPERTY + ".gallery"));
    private static final int GALLERY_PER_VIEW = 3, GALLERY_SPACING = 4, GALLERY_ROW = 15, GALLERY_ROW_GAP = 24;
    private static final int GX = 4000, GY = 150, GZ = 4000;
    private static List<String> galleryList;
    private boolean galleryBuilt;

    static synchronized List<String> galleryNames()
    {
        if (galleryList == null)
        {
            List<String> names = new ArrayList<String>();
            for (Object o : cpw.mods.fml.common.registry.GameData.getBlockRegistry().getKeys())
            {
                String n = (String) o;
                if (n.startsWith("deci:") && !n.startsWith("deci:BlockRoad") && !n.startsWith("deci:BlockMapBarrier")
                    && !n.startsWith("deci:BlockSoundBlock"))
                {
                    names.add(n);
                }
            }
            java.util.Collections.sort(names);
            galleryList = names;
        }
        return galleryList;
    }

    // ---- footprint mode (-Ddeciworldgen.autotest.footprint=true): measures
    // how far each prop's MODEL reaches beyond its 1 block (bicycles, shelves
    // and tables draw over neighbour cells). One cell on a white wool floor
    // with black wool markers at +-4 blocks; camera straight down from 40 up
    // (props are tile entity renderers, culled beyond 64 blocks). View 0 =
    // the empty cell, view n = prop n-1 at meta 3; tools/footprint.py diffs
    // every view against view 0.
    private static final boolean FOOTPRINT = "true".equals(System.getProperty(PROPERTY + ".footprint"));
    private static final int FX0 = 6000, FZ0 = 6000, FP_HEIGHT = 40;
    private boolean footprintBuilt;

    private void serveFootprintView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        List<String> names = galleryNames();
        if (!footprintBuilt)
        {
            footprintBuilt = true;
            for (int cx = (FX0 - 16 >> 4); cx <= (FX0 + 16 >> 4); cx++)
            {
                for (int cz = (FZ0 - 16 >> 4); cz <= (FZ0 + 16 >> 4); cz++)
                {
                    world.getChunkProvider().loadChunk(cx, cz);
                }
            }
            for (int x = FX0 - 12; x <= FX0 + 12; x++)
            {
                for (int z = FZ0 - 12; z <= FZ0 + 12; z++)
                {
                    boolean marker = Math.abs(x - FX0) == 4 && Math.abs(z - FZ0) == 4;
                    world.setBlock(x, GY - 1, z, net.minecraft.init.Blocks.wool, marker ? 15 : 0, 2);
                }
            }
        }
        // clear the previous prop (multiblocks remove all their parts when broken)
        for (int dx = -3; dx <= 3; dx++)
        {
            for (int dz = -3; dz <= 3; dz++)
            {
                for (int y = GY; y < GY + 8; y++)
                {
                    if (!world.isAirBlock(FX0 + dx, y, FZ0 + dz))
                    {
                        world.setBlock(FX0 + dx, y, FZ0 + dz, net.minecraft.init.Blocks.air, 0, 3);
                    }
                }
            }
        }
        String name = v == 0 ? "empty" : names.get(v - 1);
        if (v > 0)
        {
            net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromName(name);
            try
            {
                if (b instanceof net.minecraft.block.BlockDoor || name.startsWith("deci:Door_"))
                {
                    world.setBlock(FX0, GY, FZ0, b, 3, 2);
                    world.setBlock(FX0, GY + 1, FZ0, b, 8, 2);
                }
                else
                {
                    world.setBlock(FX0, GY, FZ0, b, 3, 2);
                    if (b.hasTileEntity(3))
                    {
                        net.decimation.fixes.MultiblockRepairHandler.complete(world, FX0, GY, FZ0);
                    }
                }
            }
            catch (Throwable t)
            {
                FMLLog.info("[%s] AUTOTEST footprint could not place %s: %s", DecimationWorldGen.MODID, name, t);
            }
        }
        world.setWorldTime(6000);
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = 180;
        viewPitch = 90;
        p.playerNetServerHandler.setPlayerLocation(FX0 + 0.5, GY + FP_HEIGHT, FZ0 + 0.5, 180, 90);
        viewSpot = "footprint " + name;
        viewReady = v;
    }

    private void serveGalleryView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        List<String> names = galleryNames();
        int rows = (names.size() + GALLERY_ROW - 1) / GALLERY_ROW;
        if (!galleryBuilt)
        {
            galleryBuilt = true;
            for (int cx = (GX >> 4) - 1; cx <= ((GX + GALLERY_ROW * GALLERY_SPACING) >> 4) + 1; cx++)
            {
                for (int cz = (GZ >> 4) - 1; cz <= ((GZ + rows * GALLERY_ROW_GAP + 16) >> 4) + 1; cz++)
                {
                    world.getChunkProvider().loadChunk(cx, cz);
                }
            }
            for (int x = GX - 4; x < GX + GALLERY_ROW * GALLERY_SPACING + 4; x++)
            {
                for (int z = GZ - 6; z < GZ + rows * GALLERY_ROW_GAP + 16; z++)
                {
                    world.setBlock(x, GY - 1, z, net.minecraft.init.Blocks.stone, 0, 2);
                }
            }
            for (int i = 0; i < names.size(); i++)
            {
                net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromName(names.get(i));
                int x = GX + (i % GALLERY_ROW) * GALLERY_SPACING, z = GZ + (i / GALLERY_ROW) * GALLERY_ROW_GAP;
                for (int dx = -2; dx < GALLERY_SPACING - 2; dx++)
                {
                    for (int y = GY; y < GY + 3; y++)
                    {
                        world.setBlock(x + dx, y, z - 4, net.minecraft.init.Blocks.quartz_block, 0, 2); // backdrop
                    }
                }
                try
                {
                    // Decimation doors (DeciDoorBlock) copy vanilla door logic
                    // without extending BlockDoor: place both halves
                    if (b instanceof net.minecraft.block.BlockDoor || names.get(i).startsWith("deci:Door_"))
                    {
                        world.setBlock(x, GY, z, b, 3, 2);
                        world.setBlock(x, GY + 1, z, b, 8, 2);
                    }
                    else
                    {
                        world.setBlock(x, GY, z, b, 3, 2);
                        if (b.hasTileEntity(3))
                        {
                            net.decimation.fixes.MultiblockRepairHandler.complete(world, x, GY, z);
                        }
                    }
                }
                catch (Throwable t)
                {
                    FMLLog.info("[%s] AUTOTEST gallery could not place %s: %s", DecimationWorldGen.MODID, names.get(i), t);
                }
            }
        }
        world.setWorldTime(6000);
        int first = v * GALLERY_PER_VIEW;
        int row = first / GALLERY_ROW, col = first % GALLERY_ROW;
        double x = GX + col * GALLERY_SPACING + (GALLERY_PER_VIEW - 1) * GALLERY_SPACING / 2.0 + 0.5;
        double z = GZ + row * GALLERY_ROW_GAP + 9.5;
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = 180;
        viewPitch = 12;
        p.playerNetServerHandler.setPlayerLocation(x, GY + 1.5, z, 180, 12);
        StringBuilder sb = new StringBuilder();
        for (int i = first; i < Math.min(names.size(), first + GALLERY_PER_VIEW); i++)
        {
            if (i / GALLERY_ROW != row)
            {
                break; // the rest of this view's slots are on the next row
            }
            sb.append(names.get(i).substring(5)).append(i + 1 < first + GALLERY_PER_VIEW ? " | " : "");
        }
        viewSpot = "gallery " + sb;
        viewReady = v;
    }

    // ---- study mode (-Ddeciworldgen.autotest.study=<save>|<cams.tsv>):
    // opens a copied reference map (dev/run/client/saves/<save>) and
    // photographs the camera spots from tools/mapbuildings.py (building,
    // storey, x, y, z, yaw[, pitch]): study_<n>.png (docs/references/decimation_maps.md)
    private static final String[] STUDY = System.getProperty(PROPERTY + ".study") != null
        ? System.getProperty(PROPERTY + ".study").split("\\|") : null;
    private static List<int[]> studyList;

    private static synchronized List<int[]> studyCams()
    {
        if (studyList == null)
        {
            studyList = new ArrayList<int[]>();
            try
            {
                for (String line : java.nio.file.Files.readAllLines(new File(STUDY[1]).toPath(),
                                                                       java.nio.charset.StandardCharsets.UTF_8))
                {
                    String[] f = line.trim().split("\t");
                    if (f.length >= 6)
                    {
                        int[] c = new int[7];
                        c[6] = 8; // pitch (optional 7th column)
                        for (int i = 0; i < Math.min(7, f.length); i++)
                        {
                            c[i] = Integer.parseInt(f[i]);
                        }
                        studyList.add(c);
                    }
                }
            }
            catch (Exception e)
            {
                FMLLog.info("[%s] AUTOTEST study: cannot read %s: %s", DecimationWorldGen.MODID, STUDY[1], e);
            }
        }
        return studyList;
    }

    private void serveStudyView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        int[] c = studyCams().get(v);
        world.getChunkProvider().loadChunk(c[2] >> 4, c[4] >> 4);
        world.setWorldTime(6000);
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = c[5];
        viewPitch = c[6];
        p.playerNetServerHandler.setPlayerLocation(c[2] + 0.5, c[3], c[4] + 0.5, c[5], c[6]);
        viewSpot = "building " + c[0] + " storey " + c[1] + " at " + c[2] + "," + c[3] + "," + c[4];
        viewSkip = false;
        viewReady = v;
    }

    // ---- flats audit (-Ddeciworldgen.autotest.flats=true): 3 apartment
    // blocks x (storey 1 living room, storey 1 bedroom, storey 2 living room,
    // ground storey lobby / flat); the camera stands at a room edge looking
    // along the longest open line (Building.lookCell)
    private static final boolean FLATS = "true".equals(System.getProperty(PROPERTY + ".flats"));
    private static final int FLATS_VIEWS = 12;
    private List<Building> flats;

    private void serveFlatView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        if (flats == null)
        {
            flats = new ArrayList<Building>();
            long seed = world.getSeed();
            for (int r = 0; r < 6 && flats.size() < 3; r++)
            {
                for (int rx = -r; rx <= r && flats.size() < 3; rx++)
                {
                    for (int rz = -r; rz <= r && flats.size() < 3; rz++)
                    {
                        if (Math.max(Math.abs(rx), Math.abs(rz)) != r
                            || Sectors.regionSector(seed, rx, rz) != StructureGenerator.CITY)
                        {
                            continue;
                        }
                        for (int cx = rx * 4; cx < rx * 4 + 4 && flats.size() < 3; cx++)
                        {
                            for (int cz = rz * 4; cz < rz * 4 + 4 && flats.size() < 3; cz++)
                            {
                                for (Building b : DecimationWorldGen.city.plan(world, cx, cz))
                                {
                                    if (b.kind == Building.APARTMENT && b.floors >= 3 && flats.size() < 3)
                                    {
                                        flats.add(b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        viewSkip = false;
        int i = v / 4, mode = v % 4;
        Building b = i < flats.size() ? flats.get(i) : null;
        if (b == null)
        {
            viewSkip = true;
            viewSpot = "no flat " + i;
            viewReady = v;
            return;
        }
        for (int cx = (b.minX >> 4) - 2; cx <= ((b.minX + b.width) >> 4) + 2; cx++)
        {
            for (int cz = (b.minZ >> 4) - 2; cz <= ((b.minZ + b.length) >> 4) + 2; cz++)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
        Integer baseY = StructureData.get(world).baseY(b.id);
        int storey = mode == 3 ? 0 : mode == 2 ? 2 : 1;
        byte room = mode == 1 ? net.decimation.worldgen.building.StoreyPlan.R_BEDROOM : mode == 3 ? net.decimation.worldgen.building.StoreyPlan.R_LOBBY : net.decimation.worldgen.building.StoreyPlan.R_LIVING;
        int[] c = b.lookCell(storey, room);
        if (c == null && mode == 3)
        {
            c = b.lookCell(0, net.decimation.worldgen.building.StoreyPlan.R_LIVING);
        }
        if (baseY == null || baseY == StructureData.CANCELLED || c == null)
        {
            viewSkip = true;
            viewSpot = b.id + " mode " + mode + " unavailable";
            viewReady = v;
            return;
        }
        world.setWorldTime(6000);
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = c[2];
        viewPitch = 12;
        p.playerNetServerHandler.setPlayerLocation(c[0] + 0.5, baseY + storey * b.storeyHeight + 1, c[1] + 0.5, c[2], 12);
        viewSpot = b.describe() + " " + b.id + " storey " + storey + " " + (mode == 1 ? "bedroom" : mode == 3 ? "ground" : "living")
            + " at " + c[0] + "," + (baseY + storey * b.storeyHeight + 1) + "," + c[1];
        viewReady = v;
    }

    // ---- furniture set preview (-Ddeciworldgen.autotest.sets=true): every
    // furniture set staged alone in a bay (plaster back wall, plank floor),
    // photographed front-on: set_<n>.png, set name logged per view
    private static final boolean SETS = "true".equals(System.getProperty(PROPERTY + ".sets"));
    private static final int SX = 4000, SY = 150, SZ = 5000, BAY = 10;
    private boolean setsBuilt;

    private void serveSetView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        java.util.List<net.decimation.worldgen.sets.FurnitureSet> sets = net.decimation.worldgen.sets.FurnitureSets.all();
        if (!setsBuilt)
        {
            setsBuilt = true;
            net.minecraft.block.Block wall = net.minecraft.block.Block.getBlockFromName("deci:BlockWallOffice_Top");
            net.minecraft.block.Block wallLow = net.minecraft.block.Block.getBlockFromName("deci:BlockWallOffice_Bottom_1");
            for (int i = 0; i < sets.size(); i++)
            {
                int bx = SX + i * BAY;
                for (int cx = (bx >> 4) - 1; cx <= ((bx + BAY) >> 4) + 1; cx++)
                {
                    for (int cz = (SZ >> 4) - 1; cz <= ((SZ + 16) >> 4) + 1; cz++)
                    {
                        world.getChunkProvider().loadChunk(cx, cz);
                    }
                }
                for (int x = bx; x < bx + BAY; x++)
                {
                    for (int z = SZ - 1; z < SZ + 12; z++)
                    {
                        world.setBlock(x, SY - 1, z, net.minecraft.init.Blocks.planks, 1, 2);
                        for (int y = SY; y < SY + 5; y++)
                        {
                            boolean back = z == SZ - 1, side = (x == bx || x == bx + BAY - 1) && z < SZ + 6;
                            world.setBlock(x, y, z, back || side ? (y == SY ? wallLow : wall) : net.minecraft.init.Blocks.air, 0, 2);
                        }
                    }
                }
                net.decimation.worldgen.sets.FurnitureSet set = sets.get(i);
                int x0 = bx + (BAY - set.width) / 2;
                for (int y = 0; y < set.layers.length && y < 3; y++)
                {
                    for (int r = 0; r < set.depth; r++)
                    {
                        for (int c = 0; c < set.width; c++)
                        {
                            char ch = set.at(y, r, c);
                            net.decimation.worldgen.sets.FurnitureSet.Entry e = set.entry(ch, 0);
                            if (ch == ' ' || ch == '.' || e == null || e.blocks.isEmpty())
                            {
                                continue;
                            }
                            world.setBlock(x0 + c, SY + y, SZ + r, e.blocks.get(0), previewMeta(e), 2);
                            net.decimation.fixes.MultiblockRepairHandler.complete(world, x0 + c, SY + y, SZ + r);
                        }
                    }
                }
            }
        }
        world.setWorldTime(6000);
        net.decimation.worldgen.sets.FurnitureSet set = sets.get(v);
        double x = SX + v * BAY + BAY / 2.0;
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = 180;
        viewPitch = 25;
        p.playerNetServerHandler.setPlayerLocation(x, SY + 2.5, SZ + 8.5, 180, 25);
        viewSpot = "set " + set.name;
        viewReady = v;
    }

    /**
     * Metadata for the preview bay: back wall to the north, so the set's
     * "out" is south (+z) and "right" is east (+x); same tables as Building.
     */
    private static int previewMeta(net.decimation.worldgen.sets.FurnitureSet.Entry e)
    {
        if (e.face == null)
        {
            return e.meta;
        }
        int dx = 0, dz = 0;
        if ("in".equals(e.face)) dz = -1;
        else if ("right".equals(e.face)) dx = 1;
        else if ("left".equals(e.face)) dx = -1;
        else dz = 1;
        String type = e.type != null ? e.type : "prop";
        if ("vanilla".equals(type)) return dx < 0 ? 4 : dx > 0 ? 5 : dz < 0 ? 2 : 3;
        if ("seat".equals(type)) return dx < 0 ? 0 : dx > 0 ? 1 : dz < 0 ? 2 : 3;
        if ("bed".equals(type)) return (dx < 0 ? 1 : dx > 0 ? 3 : dz < 0 ? 2 : 0) | ("head".equals(e.part) ? 8 : 0);
        if ("meta".equals(type)) return e.meta;
        if ("trapdoor".equals(type)) return (dz > 0 ? 0 : dz < 0 ? 1 : dx > 0 ? 2 : 3) | 4;
        if ("hook".equals(type)) return dz < 0 ? 0 : dx > 0 ? 1 : dz > 0 ? 2 : 3;
        return dx < 0 ? 4 : dx > 0 ? 2 : dz < 0 ? 5 : 3;
    }

    // ---- audit mode (-Ddeciworldgen.autotest.audit=true): instead of the
    // street views, 4 views each of a sample apartment, office and shop:
    // facade from the street, ground storey, first upper storey, roof
    private static final boolean AUDIT = "true".equals(System.getProperty(PROPERTY + ".audit"));
    private static final int AUDIT_VIEWS = 15;
    private List<Building> auditBuildings;

    private void serveAuditView(int v)
    {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        if (auditBuildings == null)
        {
            auditBuildings = pickAuditBuildings(world);
        }
        viewSkip = false;
        int i = v / 5, mode = v % 5; // facade, ground, storey 1, storey 1 second room, roof
        Building b = i < auditBuildings.size() ? auditBuildings.get(i) : null;
        if (b == null)
        {
            viewSkip = true;
            viewSpot = "no building " + i;
            viewReady = v;
            return;
        }
        for (int cx = (b.minX >> 4) - 2; cx <= ((b.minX + b.width) >> 4) + 2; cx++)
        {
            for (int cz = (b.minZ >> 4) - 2; cz <= ((b.minZ + b.length) >> 4) + 2; cz++)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
        Integer baseY = StructureData.get(world).baseY(b.id);
        if (baseY == null || baseY == StructureData.CANCELLED)
        {
            viewSkip = true;
            viewSpot = b.id + " not placed (" + baseY + ")";
            viewReady = v;
            return;
        }
        world.setWorldTime(6000);
        boolean west = b.front == Building.FRONT_WEST;
        double x, y, z;
        float yaw, pitch;
        if (mode == 0)
        {
            x = west ? b.lotX - 5 : b.lotMaxX() + 5;
            z = b.minZ + b.length / 2.0;
            y = baseY + 3;
            yaw = west ? 270 : 90;
            pitch = b.floors > 4 ? -30 : -12;
        }
        else if (mode == 4)
        {
            x = b.minX + b.width / 2.0;
            z = b.minZ + b.length + 6;
            y = baseY + b.floors * b.storeyHeight + 10;
            yaw = 180;
            pitch = 45;
        }
        else
        {
            int storey = mode == 3 ? 1 : mode - 1;
            byte second = b.kind == Building.APARTMENT ? net.decimation.worldgen.building.StoreyPlan.R_BEDROOM
                : b.kind == Building.OFFICE ? net.decimation.worldgen.building.StoreyPlan.R_MEETING : net.decimation.worldgen.building.StoreyPlan.R_STOCK;
            int[] c = storey < b.floors ? (mode == 3 ? b.viewCell(storey, second) : b.viewCell(storey)) : null;
            if (c == null)
            {
                viewSkip = true;
                viewSpot = b.id + " has no storey " + storey;
                viewReady = v;
                return;
            }
            x = c[0] + 0.5;
            z = c[1] + 0.5;
            y = baseY + storey * b.storeyHeight + 1;
            yaw = c[2];
            pitch = 8;
        }
        net.minecraft.entity.player.EntityPlayerMP p = player();
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = yaw;
        viewPitch = pitch;
        p.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
        viewSpot = b.describe() + " " + b.id + " mode " + mode + " at " + (int) x + "," + (int) y + "," + (int) z;
        viewReady = v;
    }

    /** First apartment (3+ floors), tallest office and a shop of the city region nearest spawn. */
    private List<Building> pickAuditBuildings(WorldServer world)
    {
        List<Building> out = new ArrayList<Building>();
        long seed = world.getSeed();
        Building apt = null, office = null, shop = null;
        for (int r = 0; r < 6 && (apt == null || office == null || shop == null); r++)
        {
            for (int rx = -r; rx <= r; rx++)
            {
                for (int rz = -r; rz <= r; rz++)
                {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != r
                        || Sectors.regionSector(seed, rx, rz) != StructureGenerator.CITY)
                    {
                        continue;
                    }
                    for (int cx = rx * 4; cx < rx * 4 + 4; cx++)
                    {
                        for (int cz = rz * 4; cz < rz * 4 + 4; cz++)
                        {
                            for (Building b : DecimationWorldGen.city.plan(world, cx, cz))
                            {
                                if (b.kind == Building.APARTMENT && b.floors >= 3 && apt == null)
                                {
                                    apt = b;
                                }
                                else if (b.kind == Building.OFFICE && (office == null || b.floors > office.floors)
                                         && b.floors <= 12)
                                {
                                    office = b;
                                }
                                else if (b.kind == Building.SHOP && shop == null)
                                {
                                    shop = b;
                                }
                            }
                        }
                    }
                }
            }
        }
        out.add(apt);
        out.add(office);
        out.add(shop);
        return out;
    }

    /** Server side: move the player to the requested view over a city street. */
    private void serveView()
    {
        int v = viewRequested;
        if (v < 0 || v <= viewReady)
        {
            return;
        }
        viewSpot = "";
        if (!peaceful)
        {
            // screenshots only after the tests (those need monsters): no mob
            // may walk into a shot or attack the camera
            peaceful = true;
            MinecraftServer.getServer().func_147139_a(net.minecraft.world.EnumDifficulty.PEACEFUL);
            WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
            for (Object o : new ArrayList<Object>(w.loadedEntityList))
            {
                if (o instanceof net.minecraft.entity.EntityLiving)
                {
                    ((net.minecraft.entity.Entity) o).setDead();
                }
            }
        }
        if (STUDY != null)
        {
            serveStudyView(v);
            return;
        }
        if (FLATS)
        {
            serveFlatView(v);
            return;
        }
        if (SETS)
        {
            serveSetView(v);
            return;
        }
        if (FOOTPRINT)
        {
            serveFootprintView(v);
            return;
        }
        if (GALLERY)
        {
            serveGalleryView(v);
            return;
        }
        if (AUDIT)
        {
            serveAuditView(v);
            return;
        }
        net.minecraft.entity.player.EntityPlayerMP p = player();
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        long seed = world.getSeed();
        int sx = 0, sz = 0;
        search:
        for (int r = 0; r < 6; r++)
        {
            for (int rx = -r; rx <= r; rx++)
            {
                for (int rz = -r; rz <= r; rz++)
                {
                    if (Sectors.regionSector(seed, rx, rz) == StructureGenerator.CITY)
                    {
                        sx = rx * Sectors.REGION_BLOCKS + 64 + 2; // centre of a north-south street
                        sz = rz * Sectors.REGION_BLOCKS + 64 + 34;
                        break search;
                    }
                }
            }
        }
        float[] f = VIEWS[v];
        int x = sx + (int) f[0], z = sz + (int) f[2];
        world.getChunkProvider().loadChunk(x >> 4, z >> 4);
        int ground = world.getTopSolidOrLiquidBlock(x, z);
        float yaw = f[3], pitch = f[4];
        double px = x + 0.5, py = ground + f[1], pz = z + 0.5;
        if (f[0] == LAMP)
        {
            // side-on view of the nearest street light: 7 blocks off it,
            // across its arm, so the arm points left or right on screen
            net.minecraft.block.Block lamp = net.minecraft.block.Block.getBlockFromName("deci:BlockStreetLight");
            search2:
            for (int dz = 0; dz < 64; dz++)
            {
                for (int dx = -40; dx < 40; dx++)
                {
                    for (int y = 60; y < 90; y++)
                    {
                        if (world.getBlock(sx + dx, y, sz + dz) == lamp)
                        {
                            int meta = world.getBlockMetadata(sx + dx, y, sz + dz);
                            // arm: 2 east, 3 south, 4 west, 5 north; look from 7 blocks to its right
                            int ax = meta == 2 ? 1 : meta == 4 ? -1 : 0, az = meta == 3 ? 1 : meta == 5 ? -1 : 0;
                            px = sx + dx + 0.5 - az * 7;
                            pz = sz + dz + 0.5 + ax * 7;
                            py = y + 4;
                            yaw = (float) Math.toDegrees(Math.atan2(-(sx + dx + 0.5 - px), sz + dz + 0.5 - pz));
                            pitch = 0;
                            viewSpot = "lamp meta " + meta + " at " + (sx + dx) + "," + y + "," + (sz + dz) + ", arm "
                                + (ax > 0 ? "east" : ax < 0 ? "west" : az > 0 ? "south" : "north") + ", camera";
                            break search2;
                        }
                    }
                }
            }
        }
        // creative: invulnerable, a camera spot inside a block must not
        // suffocate the player (1.7.10 has no spectator mode)
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        viewYaw = yaw;
        viewPitch = pitch;
        p.playerNetServerHandler.setPlayerLocation(px, py, pz, yaw, pitch);
        viewSpot += " " + (int) px + "," + (int) py + "," + (int) pz;
        viewReady = v;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && finished)
        {
            serveView();
            return;
        }
        if (event.phase != TickEvent.Phase.END || !requested || finished)
        {
            return;
        }
        if (waitTicks > 0)
        {
            waitTicks--;
            return;
        }
        try
        {
            switch (phase++)
            {
                case 0: // instant checks, then drop caps and the supply crate
                    if (STUDY != null)
                    {
                        break; // study mode: straight to the screenshots
                    }
                    run(MinecraftServer.getServer().worldServerForDimension(0));
                    checkVehicle();
                    checkHumanity();
                    checkPropBox();
                    dropBottlecaps();
                    deci.aJ.b.aAg = 0; // supplyDropCountdown: drop on the next tick
                    waitTicks = 100;   // pickup, and spawn invulnerability runs out
                    return;
                case 1:
                    checkBottlecaps();
                    checkArmor();
                    checkHelmet();
                    waitTicks = 100;
                    return;
                default: // poll until the crate has landed, at most ~60 s
                    if (!checkSupplyDrop() && phase < 14)
                    {
                        waitTicks = 100;
                        return;
                    }
                    break;
            }
        }
        catch (Throwable t)
        {
            FMLLog.info("[%s] AUTOTEST error: %s", DecimationWorldGen.MODID, t);
        }
        finished = true;
    }

    private int phase;
    private int waitTicks;

    /** Prop culling patch: a table's render box must be the rotation proof 2x2 box. */
    private void checkPropBox()
    {
        net.minecraft.block.Block table = net.minecraft.block.Block.getBlockFromName("deci:BlockWoodTable");
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (table == null || p == null)
        {
            return;
        }
        net.minecraft.tileentity.TileEntity te = table.createTileEntity(p.worldObj, 2);
        te.xCoord = 100;
        te.yCoord = 64;
        te.zCoord = 100;
        net.minecraft.util.AxisAlignedBB bb = te.getRenderBoundingBox();
        FMLLog.info("[%s] AUTOTEST prop box: table at 100,64,100 -> %.2f..%.2f x %.2f..%.2f y %.2f..%.2f z (old 1x1 cell = 100..101)",
                    DecimationWorldGen.MODID, bb.minX, bb.maxX, bb.minY, bb.maxY, bb.minZ, bb.maxZ);
    }

    /** Humanity fix: a player killing an infected gains humanity (+1 from the
     *  infected's own value, +1 from the formerly dedicated-only rule). */
    private void checkHumanity()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        deci.Q.b data = deci.Q.b.e(p);
        int before = data.cd(); // getHumanity
        deci.ag.d infected = new deci.ag.d(p.worldObj);
        infected.setLocationAndAngles(p.posX + 2, p.posY, p.posZ, 0, 0);
        p.worldObj.spawnEntityInWorld(infected);
        infected.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(p), 1000.0f);
        FMLLog.info("[%s] AUTOTEST humanity: killed infected, humanity %d -> %d, dead=%s",
                    DecimationWorldGen.MODID, before, data.cd(), infected.isDead || infected.getHealth() <= 0);
        infected.setDead();
    }

    /** Supply drop fix: the forced drop must leave a crate block in its column.
     *  Returns true once the crate has landed (or nothing was dropped). */
    private boolean checkSupplyDrop()
    {
        int x = net.decimation.fixes.SupplyDropScheduler.lastDropX;
        int z = net.decimation.fixes.SupplyDropScheduler.lastDropZ;
        if (x == Integer.MIN_VALUE)
        {
            FMLLog.info("[%s] AUTOTEST supply drop: nothing dropped", DecimationWorldGen.MODID);
            return true;
        }
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        int landedY = -1;
        for (int y = 255; y > 0; y--)
        {
            if (world.getBlock(x, y, z) == deci.aD.c.afA)
            {
                landedY = y;
                break;
            }
        }
        int falling = 0;
        for (Object o : world.loadedEntityList)
        {
            if (o instanceof deci.ac.a)
            {
                falling++;
            }
        }
        FMLLog.info("[%s] AUTOTEST supply drop: column %d/%d, crate block at y=%d, falling crates=%d",
                    DecimationWorldGen.MODID, x, z, landedY, falling);
        return landedY >= 0;
    }
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

    /**
     * Armor fix: a "human" (NPC gun) hit must be reduced by Decimation chest
     * armor exactly by its damage multiplier; without armor it must not be.
     */
    private void checkArmor()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        net.decimation.mod.common.item.armor.ItemArmorDeci chest = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            if (o instanceof net.decimation.mod.common.item.armor.ItemArmorDeci
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).armorType == 1
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).getDamageMultiplier() < 1.0f)
            {
                chest = (net.decimation.mod.common.item.armor.ItemArmorDeci) o;
                break;
            }
        }
        if (chest == null)
        {
            FMLLog.info("[%s] AUTOTEST armor: no Decimation chest armor found", DecimationWorldGen.MODID);
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        float bare = hit(p);
        p.setCurrentItemOrArmor(3, new net.minecraft.item.ItemStack(chest)); // slot 3 = chest
        float armored = hit(p);
        p.setCurrentItemOrArmor(3, null);
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.setHealth(p.getMaxHealth());
        FMLLog.info("[%s] AUTOTEST armor: 10 \"human\" damage, bare took %.2f, with %s (x%.3f) took %.2f",
                    DecimationWorldGen.MODID, bare, chest.getUnlocalizedName(), chest.getDamageMultiplier(), armored);
    }

    /**
     * Helmet fix: NPC hits use the helmet on ~20% (random headshots); a
     * player's gun uses it only when its aim line crosses the head.
     */
    private void checkHelmet()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        net.decimation.mod.common.item.armor.ItemArmorDeci helm = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            if (o instanceof net.decimation.mod.common.item.armor.ItemArmorDeci
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).armorType == 0
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).getDamageMultiplier() < 0.5f)
            {
                helm = (net.decimation.mod.common.item.armor.ItemArmorDeci) o;
                break;
            }
        }
        if (helm == null)
        {
            FMLLog.info("[%s] AUTOTEST helmet: no Decimation helmet found", DecimationWorldGen.MODID);
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.setCurrentItemOrArmor(4, new net.minecraft.item.ItemStack(helm)); // slot 4 = helmet
        int reduced = 0, total = 400;
        for (int i = 0; i < total; i++)
        {
            if (hit(p) < 9.99f)
            {
                reduced++;
            }
        }
        // player shooter 5 blocks north, facing south (+z), aiming at head then chest
        net.minecraftforge.common.util.FakePlayer shooter =
            net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((WorldServer) p.worldObj);
        float headTaken = shoot(p, shooter, p.boundingBox.minY + 1.6);
        float chestTaken = shoot(p, shooter, p.boundingBox.minY + 1.0);
        p.setCurrentItemOrArmor(4, null);
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.setHealth(p.getMaxHealth());
        FMLLog.info("[%s] AUTOTEST helmet %s (x%.3f): NPC hits reduced %d/%d (expect ~20%%);"
                    + " player gun 10 dmg: head took %.2f, chest took %.2f",
                    DecimationWorldGen.MODID, helm.getUnlocalizedName(), helm.getDamageMultiplier(),
                    reduced, total, headTaken, chestTaken);
    }

    /** A 10 point gunDeci hit from a shooter aiming at the given height. */
    private static float shoot(net.minecraft.entity.player.EntityPlayerMP p, net.minecraft.entity.player.EntityPlayer shooter, double aimY)
    {
        double sx = p.posX, sz = p.posZ - 5.0, sy = p.boundingBox.minY;
        double eyeY = sy + shooter.getEyeHeight();
        float pitch = (float) Math.toDegrees(-Math.atan2(aimY - eyeY, 5.0));
        shooter.setLocationAndAngles(sx, sy, sz, 0.0F, pitch); // yaw 0 = facing +z
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        p.attackEntityFrom(deci.ab.a.c(shooter), 10.0f); // GunDamageSource.forShooter
        return before - p.getHealth();
    }

    /** One 10 point "human" hit on a fully healed player, returns health lost. */
    private static float hit(net.minecraft.entity.player.EntityPlayerMP p)
    {
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        p.attackEntityFrom(deci.aD.h.alh, 10.0f); // DamageSources.human
        return before - p.getHealth();
    }

    /**
     * Vehicle fix: a survival punch must leave an empty vehicle alone, a
     * sneaking survival punch must still pick it up.
     */
    private void checkVehicle()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        deci.ad.e vehicle = new deci.ad.i(p.worldObj, p.posX + 3, p.posY, p.posZ); // hummer
        p.worldObj.spawnEntityInWorld(vehicle);
        p.setSneaking(false);
        p.attackTargetEntityWithCurrentItem(vehicle);
        boolean survivedPunch = !vehicle.isDead;
        p.setSneaking(true);
        p.attackTargetEntityWithCurrentItem(vehicle);
        boolean pickedUpSneaking = vehicle.isDead;
        p.setSneaking(false);
        vehicle.setDead();
        p.setGameType(WorldSettings.GameType.CREATIVE);
        FMLLog.info("[%s] AUTOTEST vehicle: survives survival punch=%s, sneak punch picks up=%s",
                    DecimationWorldGen.MODID, survivedPunch, pickedUpSneaking);
    }

    private void run(WorldServer world)
    {
        FMLLog.info("[%s] AUTOTEST generated zones in this world: %d",
                    DecimationWorldGen.MODID, ZoneStore.size());
        // Decimation's spawns must reach the biomes of the Decimation world type
        net.minecraft.entity.player.EntityPlayerMP sp = player();
        net.minecraft.world.biome.BiomeGenBase biome =
            world.getBiomeGenForCoords((int) sp.posX, (int) sp.posZ);
        int deciMonsters = 0;
        for (Object o : biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.monster))
        {
            if (((net.minecraft.world.biome.BiomeGenBase.SpawnListEntry) o).entityClass.getName().startsWith("deci."))
            {
                deciMonsters++;
            }
        }
        FMLLog.info("[%s] AUTOTEST biome at spawn: %s (id %d), Decimation monster spawn entries %d",
                    DecimationWorldGen.MODID, biome.biomeName, biome.biomeID, deciMonsters);
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
