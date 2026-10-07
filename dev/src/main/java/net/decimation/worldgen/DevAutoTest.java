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
        if (view >= VIEWS.length || "false".equals(System.getProperty(PROPERTY + ".views")))
        {
            return false;
        }
        mc.gameSettings.hideGUI = true;
        if (viewRequested < view)
        {
            viewRequested = view;
            viewWait = 0;
            return true;
        }
        if (viewReady < view || ++viewWait < 260)
        {
            return true; // teleport pending, then let the chunks render
        }
        net.minecraft.util.IChatComponent msg = net.minecraft.util.ScreenShotHelper.saveScreenshot(
            mc.mcDataDir, "autotest_" + view + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        FMLLog.info("[%s] AUTOTEST view %d at %s: %s", DecimationWorldGen.MODID, view, viewSpot,
                    msg == null ? "?" : msg.getUnformattedText());
        view++;
        return true;
    }

    private volatile String viewSpot = "";

    /** Server side: move the player to the requested view over a city street. */
    private void serveView()
    {
        int v = viewRequested;
        if (v < 0 || v <= viewReady)
        {
            return;
        }
        viewSpot = "";
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
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
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
