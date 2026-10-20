package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.worldgen.DecimationWorldGen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Screenshots for the project website (docs/website.md section 6): one shot per row of tools/site/siteshots.tsv
 * (name, x, y, z, yaw, pitch, time; y "~N" = N above the ground). The server holds the player there flying, no rain,
 * the row's time of day; rows with "~" wait 100 ticks high above first so the ground is generated. The client waits
 * until the chunks are drawn (no chunk updates for 3 s, at least 10 s, at most 60 s) and saves
 * screenshots/site_<name>.png with the GUI hidden. Row "gun" holds our MAC-10: the hand stays, the HUD is cancelled.
 * -Pshots=a,b shoots only those rows; -Pwindow=1920x1080 sets the window size.
 */
public class SiteShotsTest extends DevTestMode
{
    public String name() { return "siteshots"; }

    /** Shots need the current generation everywhere: a fresh seed 1 world (keepworld=true reuses the last one). */
    public boolean freshWorld() { return !Boolean.getBoolean("deciworldgen.autotest.keepworld"); }

    private static final int MIN_WAIT = 200, QUIET = 60, MAX_WAIT = 2400, GROUND_WAIT = 100, SETTLED = 20;

    /**
     * One camera row of the TSV. An 8th column, the cast, places characters for the shot (docs/main_menu.md 2h, the
     * Multiplayer popup: "show 2-3 mob in-game as preview"): "kind:ahead,right,turn;..." with kind a tier of
     * NpcLoadouts (a survivor, soldier or bandit by its tier) or "infected", ahead and right in blocks from the camera
     * on the ground, turn the degrees from facing the camera (180: looking where the camera looks). They are held in
     * place and kept at full health until the shot is taken.
     */
    static final class Row
    {
        final String name, y, cast;
        final double x, z;
        final float yaw, pitch;
        final long time;

        Row(String[] c)
        {
            name = c[0];
            x = Double.parseDouble(c[1]);
            y = c[2];
            z = Double.parseDouble(c[3]);
            yaw = Float.parseFloat(c[4]);
            pitch = Float.parseFloat(c[5]);
            time = Long.parseLong(c[6]);
            cast = c.length > 7 ? c[7].trim() : "";
        }

        boolean relative()
        {
            return y.startsWith("~");
        }
    }

    static List<Row> read(File f, String only) throws IOException
    {
        List<String> keep = only == null || only.isEmpty() ? null : Arrays.asList(only.split(","));
        List<Row> rows = new ArrayList<Row>();
        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8))
        {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
            {
                continue;
            }
            String[] c = line.split("\t");
            if (c.length < 7)
            {
                throw new IOException("siteshots.tsv needs 7 columns: " + line);
            }
            Row r = new Row(c);
            if (keep == null || keep.contains(r.name))
            {
                rows.add(r);
            }
        }
        return rows;
    }

    /** Hides the HUD (not the hand) while a "gun" row is shot. */
    public static final class HudHider
    {
        volatile boolean active;

        @SubscribeEvent
        public void overlay(RenderGameOverlayEvent.Pre e)
        {
            if (active && e.type == RenderGameOverlayEvent.ElementType.ALL)
            {
                e.setCanceled(true);
            }
        }
    }

    private static HudHider hud;
    private volatile List<Row> rows;
    private volatile int index;
    private volatile int placed = -1;
    private int serverRow = -1, serverT, clientRow = -1, waited, quiet;
    private double groundY;
    private float savedFov, savedGamma;
    private int savedDistance;
    private boolean savedClouds, saved;

    public boolean client(Minecraft mc)
    {
        if (rows == null)
        {
            String only = System.getProperty("deciworldgen.autotest.shots");
            // -Pshotsfile=mpshots: another table of tools/site (the main menu's Multiplayer pictures)
            String table = System.getProperty("deciworldgen.autotest.shotsfile", "siteshots");
            File tsv = new File(mc.mcDataDir, "../../../tools/site/" + table + ".tsv");
            try
            {
                rows = read(tsv.getCanonicalFile(), only);
            }
            catch (IOException e)
            {
                DevTestResults.check(name(), "siteshots.tsv", e.getMessage(), false, "a readable tools/site/siteshots.tsv");
                return false;
            }
            if (hud == null)
            {
                hud = new HudHider();
                MinecraftForge.EVENT_BUS.register(hud);
            }
        }
        if (!saved)
        {
            saved = true;
            savedFov = mc.gameSettings.fovSetting;
            savedGamma = mc.gameSettings.gammaSetting;
            savedDistance = mc.gameSettings.renderDistanceChunks;
            savedClouds = mc.gameSettings.clouds;
        }
        int i = index;
        if (i >= rows.size())
        {
            // the dev client is also used for play: leave its options as they were
            mc.gameSettings.fovSetting = savedFov;
            mc.gameSettings.gammaSetting = savedGamma;
            mc.gameSettings.renderDistanceChunks = savedDistance;
            mc.gameSettings.clouds = savedClouds;
            mc.gameSettings.hideGUI = false;
            hud.active = false;
            return false;
        }
        Row r = rows.get(i);
        if (clientRow != i)
        {
            clientRow = i; // a new row waits from zero, even when the server placed it at once
            waited = 0;
            quiet = 0;
        }
        boolean gun = "gun".equals(r.name);
        mc.gameSettings.fovSetting = 70.0F;
        mc.gameSettings.gammaSetting = 0.6F;
        mc.gameSettings.renderDistanceChunks = 16;
        mc.gameSettings.clouds = mc.thePlayer == null || mc.thePlayer.posY < 120; // above the clouds they hide the view
        mc.gameSettings.hideGUI = !gun;
        hud.active = gun;
        if (mc.thePlayer == null || placed < i)
        {
            waited = 0;
            quiet = 0;
            return true;
        }
        if (gun)
        {
            mc.thePlayer.inventory.currentItem = 0;
        }
        // hold the camera: a touched mouse or touchpad must not turn the shot
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = r.yaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = r.pitch;
        waited++;
        int updates = chunkUpdates(mc);
        quiet = updates <= SETTLED && chunksReady(mc, r) ? quiet + 1 : 0; // a fresh world never reaches 0 updates
        if (waited < MIN_WAIT || quiet < QUIET && waited < MAX_WAIT)
        {
            return true;
        }
        String file = "site_" + r.name + ".png";
        net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, file, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File out = new File(mc.mcDataDir, "screenshots/" + file);
        DevTestResults.shot(name(), file);
        DevTestResults.value(name(), "shot " + r.name, mc.displayWidth + " x " + mc.displayHeight + ", waited " + waited
                             + " ticks" + (quiet < QUIET ? " (chunks still drawing, " + updates + " updates/s)" : ""));
        DevTestResults.check(name(), "saved " + r.name, out.isFile(), out.isFile(), "a png in screenshots/");
        FMLLog.info("[%s] siteshots %s at %.1f %s %.1f yaw %.0f pitch %.0f", DecimationWorldGen.MODID, r.name, r.x, r.y, r.z, r.yaw, r.pitch);
        index = i + 1;
        return true;
    }

    /** True when the client has the chunks under the camera and 48 blocks ahead (a far teleport starts empty). */
    private static boolean chunksReady(Minecraft mc, Row r)
    {
        double ax = -Math.sin(Math.toRadians(r.yaw)) * 48, az = Math.cos(Math.toRadians(r.yaw)) * 48;
        return !mc.theWorld.getChunkFromBlockCoords((int) Math.floor(r.x), (int) Math.floor(r.z)).isEmpty()
            && !mc.theWorld.getChunkFromBlockCoords((int) Math.floor(r.x + ax), (int) Math.floor(r.z + az)).isEmpty();
    }

    /** "60 fps, 12 chunk updates": the chunk updates of the last second. */
    private static int chunkUpdates(Minecraft mc)
    {
        String d = mc.debug;
        int a = d.indexOf(", "), b = d.indexOf(" chunk");
        return a < 0 || b < a ? 0 : Integer.parseInt(d.substring(a + 2, b).trim());
    }

    public void server()
    {
        EntityPlayerMP p = DevTestUtil.player();
        List<Row> list = rows;
        int i = index;
        if (p == null || list == null || i >= list.size())
        {
            return;
        }
        Row r = list.get(i);
        if (serverRow != i)
        {
            serverRow = i;
            serverT = 0;
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
            p.worldObj.setWorldTime(r.time);
            p.worldObj.getWorldInfo().setRaining(false);
            p.worldObj.getWorldInfo().setThundering(false);
            if ("gun".equals(r.name))
            {
                giveMac10(p);
            }
        }
        serverT++;
        p.worldObj.setWorldTime(r.time); // hold the light while the client waits
        double y;
        if (r.relative())
        {
            if (serverT < GROUND_WAIT)
            {
                y = 200; // above everything while the chunks around generate
            }
            else
            {
                if (serverT == GROUND_WAIT)
                {
                    groundY = p.worldObj.getHeightValue((int) Math.floor(r.x), (int) Math.floor(r.z));
                }
                y = groundY + Double.parseDouble(r.y.substring(1));
            }
        }
        else
        {
            y = Double.parseDouble(r.y);
        }
        p.playerNetServerHandler.setPlayerLocation(r.x, y, r.z, r.yaw, r.pitch);
        if (!r.relative() || serverT > GROUND_WAIT)
        {
            cast(p, r, i);
            placed = i;
        }
    }

    private final List<net.minecraft.entity.EntityLiving> actors = new ArrayList<net.minecraft.entity.EntityLiving>();
    private final List<double[]> spots = new ArrayList<double[]>();
    private int castRow = -1;

    /** The row's cast: placed once on the ground, then held there at full health every tick; gone with the next row. */
    private void cast(EntityPlayerMP p, Row r, int row)
    {
        if (castRow != row)
        {
            for (net.minecraft.entity.EntityLiving e : actors)
            {
                e.setDead();
            }
            actors.clear();
            spots.clear();
            castRow = row;
            if (r.cast.isEmpty())
            {
                return;
            }
            p.worldObj.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            double fx = -Math.sin(Math.toRadians(r.yaw)), fz = Math.cos(Math.toRadians(r.yaw)); // the camera's ahead
            for (String part : r.cast.split(";"))
            {
                String[] kv = part.trim().split(":");
                String[] n = kv[1].split(",");
                double ahead = Double.parseDouble(n[0]), right = Double.parseDouble(n[1]);
                float turn = n.length > 2 ? Float.parseFloat(n[2]) : 0;
                double x = r.x + fx * ahead - fz * right, z = r.z + fz * ahead + fx * right;
                double ground = p.worldObj.getHeightValue((int) Math.floor(x), (int) Math.floor(z));
                float yaw = r.yaw + 180 + turn;
                net.minecraft.entity.EntityLiving e = null;
                for (int tries = 0; tries < 5; tries++)
                {
                    e = actor(p.worldObj, kv[0]);
                    if (e == null)
                    {
                        break;
                    }
                    e.setLocationAndAngles(x, ground, z, yaw, 0);
                    if (p.worldObj.spawnEntityInWorld(e))
                    {
                        break;                              // Decimation turns some spawns into other mobs: retried
                    }
                    e = null;
                }
                if (e == null)
                {
                    DevTestResults.value(name(), "cast " + r.name, kv[0] + " not spawned");
                    continue;
                }
                actors.add(e);
                spots.add(new double[] {x, ground, z, yaw});
            }
            DevTestResults.value(name(), "cast " + r.name, actors.size() + " of " + r.cast.split(";").length);
        }
        for (int k = 0; k < actors.size(); k++)
        {
            net.minecraft.entity.EntityLiving e = actors.get(k);
            double[] at = spots.get(k);
            e.setPositionAndRotation(at[0], at[1], at[2], (float) at[3], 0);
            e.rotationYawHead = e.renderYawOffset = (float) at[3];
            e.motionX = e.motionZ = 0;
            e.setHealth(e.getMaxHealth());
        }
    }

    private static net.minecraft.entity.EntityLiving actor(net.minecraft.world.World w, String kind)
    {
        if ("infected".equals(kind))
        {
            net.minecraft.entity.Entity e = net.minecraft.entity.EntityList.createEntityByName("Infected", w);
            return e instanceof net.minecraft.entity.EntityLiving ? (net.minecraft.entity.EntityLiving) e : null;
        }
        net.minecraft.entity.EntityLiving e = kind.startsWith("bandit") ? net.decimation.fixes.Deci.newBandit(w)
            : kind.startsWith("military") || kind.startsWith("elite") || kind.startsWith("juggernaut")
              ? net.decimation.fixes.Deci.newSoviet(w) : net.decimation.fixes.Deci.newSoldier(w);
        net.decimation.fixes.NpcLoadouts.instance().equipTier(e, kind);
        return e;
    }

    private static void giveMac10(EntityPlayerMP p)
    {
        Object o = Item.itemRegistry.getObject("deci:mac10");
        if (o == null)
        {
            o = Item.itemRegistry.getObject("deciworldgen:mac10");
        }
        if (o == null)
        {
            DevTestResults.check("siteshots", "MAC-10 item", "missing", false, "deci:mac10 or deciworldgen:mac10");
            return;
        }
        ItemStack s = new ItemStack((Item) o);
        s.stackTagCompound = new NBTTagCompound(); // gun data ready before the first frame
        s.stackTagCompound.setInteger("ammo", 0);
        p.inventory.currentItem = 0;
        p.inventory.mainInventory[0] = s;
        p.inventoryContainer.detectAndSendChanges();
    }
}
