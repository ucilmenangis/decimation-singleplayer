package net.decimation.worldgen.devtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.worldgen.DecimationWorldGen;
import net.decimation.worldgen.DevAutoTest;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.building.Building;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.StructureGenerator;
import net.decimation.worldgen.ZoneStore;
import net.minecraft.world.WorldSettings;

/**
 * Camera views (-Ptest=views, and the -Paudit / -Pgallery / -Pfootprint /
 * -Pstudy / -Pflats / -Psets variants): the server puts the camera at each
 * spot, the client waits for chunks and saves a screenshot.
 */
public class CameraViews extends DevTestMode
{
    public String name() { return "views"; }

    /** The copied map a study run opens, or null. */
    public static String study() { return STUDY != null ? STUDY[0] : null; }

    private static net.minecraft.entity.player.EntityPlayerMP player() { return DevTestUtil.player(); }

    /** A study run opens a copied map instead of the fresh autotest world. */
    public boolean freshWorld() { return STUDY == null; }

    public boolean client(Minecraft mc)
    {
        return takeViews(mc);
    }

    public void server()
    {
        serveView();
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
        if (view >= views || "false".equals(System.getProperty(DevAutoTest.PROPERTY + ".views")))
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
        String shotName = (FOOTPRINT ? "footprint_" : STUDY != null ? "study_" : FLATS ? "flat_" : SETS ? "set_"
            : GALLERY ? "gallery_" : AUDIT ? "audit_" : "autotest_") + view + ".png";
        net.minecraft.util.IChatComponent msg = net.minecraft.util.ScreenShotHelper.saveScreenshot(
            mc.mcDataDir, shotName, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        DevTestResults.shot("views", shotName + "  " + viewSpot);
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
    private static final java.util.Set<Integer> ONLY = parseOnly(System.getProperty(DevAutoTest.PROPERTY + ".only"));

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
    private static final boolean GALLERY = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".gallery"));
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
    private static final boolean FOOTPRINT = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".footprint"));
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
    static final String[] STUDY = System.getProperty(DevAutoTest.PROPERTY + ".study") != null
        ? System.getProperty(DevAutoTest.PROPERTY + ".study").split("\\|") : null;
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
    private static final boolean FLATS = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".flats"));
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
                                for (Building b : DecimationWorldGen.city().plan(world, cx, cz))
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
    private static final boolean SETS = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".sets"));
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
    private static final boolean AUDIT = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".audit"));
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
                            for (Building b : DecimationWorldGen.city().plan(world, cx, cz))
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
}
