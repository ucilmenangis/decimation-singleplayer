package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.decimation.fixes.Deci;
import net.decimation.fixes.HumanLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * NPC looks in one picture (fixes/HumanLayers, PatchHumanLayers; user 19 Oktober 2026 "from 7 to 1"). One NPC of each
 * kind at a time stands on the arena in front of the player (alone: no fight, no muzzle flash, no hurt flash) and is
 * drawn four times in one frame at the same place of the screen (the inventory's entity preview): with its picture, the
 * old way (seven layers on the model and its two copies), and both again. Pixels that differ between two drawings of the
 * same way (random effects) are left out; the rest may differ only where the old layers sat on copies 0.05 / 16 of a
 * block larger (texel edges shift by under a pixel: a pixel counts as the same when the other drawing has its colour
 * within one pixel): at most {@link #MAX_DIFF} of the NPC's pixels, checked bare (gear off the client copy for the
 * drawing); dressed only reported (a cap or a mask between the model and the old copies now covers hair and beard that
 * showed through it). Each pair is saved as npclook_<kind>[_dressed].png (picture | old way | red where they differ,
 * yellow where the same way differed from itself). Then a crowd of 20 civilians and 20 infected: how many pictures it
 * needs and how many draws are still the old way once they stand in view.
 */
public class NpcLookTest extends DevTestMode
{
    public String name() { return "npclook"; }

    /** -Pkinds=TraderFood,Human picks some. */
    static final String[] KINDS = kinds("Human", "TraderGuard", "TraderFood", "Soldier", "Bandit", "Soviet", "Hazmat", "Infected",
                                        "Frail", "Hulk", "Bloater");
    /** Kinds that must use their picture (ModelHuman mobs). */
    static final List<String> MUST = Arrays.asList("Human", "TraderGuard", "Soldier", "Bandit", "Infected", "Frail");
    static final double MAX_DIFF = 0.02;
    private static final int BACKGROUND = 0xFF203040;
    private static Draw draw;
    private static int runs;

    private static String[] kinds(String... all)
    {
        String only = System.getProperty("deciworldgen.autotest.kinds", "");
        return only.isEmpty() ? all : only.split(",");
    }

    private final List<Entity> spawned = new ArrayList<Entity>();
    private volatile int step;            // 0 arena (server), 1 kinds one by one, 2 crowd (server), 3 count, 4 clear, 5 done
    private volatile boolean ready;
    private volatile int want = -1, shown = -1, id = -1; // kind the client asks for, kind the server spawned, its entity
    private int ticks, kind, seen = -1;
    private long built0, drawn0, old0;
    private String spawning;

    /** One comparison per frame, after the frame is drawn (the game's framebuffer is still bound). */
    public static final class Draw
    {
        volatile NpcLookTest test;

        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            NpcLookTest t = test;
            if (e.phase == TickEvent.Phase.END && t != null && t.step == 1 && t.ready && t.kind < KINDS.length
                && t.shown == t.kind && t.ticks >= 10)
            {
                t.compareNext(Minecraft.getMinecraft());
            }
        }
    }

    public boolean client(Minecraft mc)
    {
        if (seen == -1 && step == 0 && !ready)
        {
            seen = -2;
            runs++;
        }
        if (draw == null)
        {
            draw = new Draw();
            FMLCommonHandler.instance().bus().register(draw);
        }
        draw.test = this;
        if (mc.thePlayer == null || !ready)
        {
            return step != 5;
        }
        if (seen != step)
        {
            seen = step; // the server set this step up: count its ticks from here
            ticks = 0;
        }
        ticks++;
        if (step == 1 && kind >= KINDS.length)
        {
            ready = false;
            step = 2;
        }
        else if (step == 1 && shown != kind)
        {
            want = kind; // the server spawns it (and removes the last one)
            ticks = 0;
        }
        else if (step == 1 && kind == 0 && ticks == 8)
        {
            DevTestUtil.screenshot(mc, name(), "npclook_world.png"); // the first one in the world, drawn with its picture
        }
        else if (step == 3 && ticks == 20)
        {
            built0 = HumanLayers.built;
            drawn0 = HumanLayers.drawn;
            old0 = HumanLayers.oldWay;
        }
        else if (step == 3 && ticks == 60)
        {
            long built = HumanLayers.built - built0, drawn = HumanLayers.drawn - drawn0, old = HumanLayers.oldWay - old0;
            DevTestUtil.screenshot(mc, name(), "npclook_crowd.png");
            DevTestResults.value(name(), "crowd of 40, 2 s", String.format("%d draws with a picture, %d the old way, %d pictures made"
                + " (%d made in all, %d draws old way in all)", drawn, old, built, HumanLayers.built, HumanLayers.oldWay));
            DevTestResults.check(name(), "crowd drawn with pictures", old + " old way", drawn > 0 && old * 50 <= drawn,
                                 "at most 2 % of the draws the old way once the crowd stands in view");
            ready = false;
            step = 4;
        }
        return step != 5;
    }

    /**
     * The next kind, bare (helmet, armour, mask, vest and backpack taken off the client copy for the drawing: the layers
     * alone, checked) and dressed (reported: gear that sits between the model and the old copies, a cap or a mask, now
     * covers hair and beard that used to show through it).
     */
    void compareNext(Minecraft mc)
    {
        String k = KINDS[kind];
        Entity e = id < 0 ? null : mc.theWorld.getEntityByID(id);
        kind++;
        if (!(e instanceof EntityLivingBase))
        {
            DevTestResults.value(name(), k, "not spawned (Decimation refused it)");
            return;
        }
        EntityLivingBase m = (EntityLivingBase) e;
        ItemStack[] armour = new ItemStack[4], gear = Deci.humanGear(m);
        for (int i = 0; i < 4; i++)
        {
            armour[i] = m.getEquipmentInSlot(i + 1);
            m.setCurrentItemOrArmor(i + 1, null);
        }
        if (gear != null)
        {
            Deci.setHumanGear(m, new ItemStack[3]);
        }
        String bare;
        try
        {
            bare = compare(mc, m, k, "");
        }
        finally
        {
            for (int i = 0; i < 4; i++)
            {
                m.setCurrentItemOrArmor(i + 1, armour[i]);
            }
            if (gear != null)
            {
                Deci.setHumanGear(m, gear);
            }
        }
        boolean dressed = gear != null && (gear[0] != null || gear[1] != null || gear[2] != null);
        for (ItemStack a : armour)
        {
            dressed |= a != null;
        }
        if (dressed)
        {
            DevTestResults.value(name(), k + " dressed", compare(mc, m, k, "_dressed").substring(1));
        }
        int[] look = new int[8];
        String folder = Deci.humanLook(m, look);
        if (folder != null)
        {
            bare += String.format("; look %s tone %d hair %d beard %d eyes %d shirt %d pants %d shoes %d flags %d", folder, look[0],
                                  look[1], look[2], look[3], look[4], look[5], look[6], look[7]);
        }
        boolean pass = bare.charAt(0) == '+';
        if (MUST.contains(k))
        {
            DevTestResults.check(name(), k, bare.substring(1), pass, "picture used, the layers drawn the same (at most 2 % of the pixels differ)");
        }
        else
        {
            DevTestResults.value(name(), k, bare.substring(1));
        }
    }

    /** Four drawings compared; the pair saved as npclook_<kind><suffix>.png; "+" or "-" (passes) and the numbers. */
    private String compare(Minecraft mc, EntityLivingBase m, String k, String suffix)
    {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int f = sr.getScaleFactor(), w = 100, h = 130;
        int gx = sr.getScaledWidth() / 2 - w / 2, gy = sr.getScaledHeight() / 2 - h / 2;
        drawOnce(mc, m, gx, gy, w, h, true);
        long d0 = HumanLayers.drawn;
        int[] a = drawOnce(mc, m, gx, gy, w, h, true);
        boolean usedA = HumanLayers.drawn > d0;
        long d1 = HumanLayers.drawn;
        int[] b = drawOnce(mc, m, gx, gy, w, h, false);
        boolean usedB = HumanLayers.drawn > d1;
        int[] a2 = drawOnce(mc, m, gx, gy, w, h, true), b2 = drawOnce(mc, m, gx, gy, w, h, false);
        int pw = w * f, ph = h * f, npc = 0, diff = 0, noise = 0;
        BufferedImage img = new BufferedImage(pw * 3, ph, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < ph; y++)
        {
            for (int x = 0; x < pw; x++)
            {
                int pa = a[y * pw + x], pb = b[y * pw + x];
                boolean inA = far(pa, BACKGROUND), inB = far(pb, BACKGROUND);
                boolean unstable = differs(a, a2, x, y, pw, ph) || differs(b, b2, x, y, pw, ph);
                boolean differs = !unstable && differs(a, b, x, y, pw, ph);
                if (inA || inB)
                {
                    npc++;
                }
                noise += unstable ? 1 : 0;
                diff += differs ? 1 : 0;
                img.setRGB(x, y, pa);
                img.setRGB(pw + x, y, pb);
                img.setRGB(2 * pw + x, y, differs ? 0xFF2020 : unstable ? 0xE0D020 : inA ? 0x404040 : 0);
            }
        }
        String file = "npclook_" + k.toLowerCase() + suffix + ".png";
        try
        {
            File dir = new File(mc.mcDataDir, "screenshots");
            dir.mkdirs();
            ImageIO.write(img, "png", new File(dir, file));
            DevTestResults.shot(name(), file);
        }
        catch (Exception ex)
        {
            DevTestResults.value(name(), k + " picture", "not saved: " + ex);
        }
        double share = npc == 0 ? 1 : diff / (double) npc;
        boolean pass = usedA && !usedB && npc > 2000 && share <= MAX_DIFF;
        if (share > MAX_DIFF)
        {
            try
            {
                ImageIO.write(img, "png", new File(new File(mc.mcDataDir, "screenshots"), "npclook_" + k.toLowerCase() + suffix
                                                                                        + "_over_" + runs + ".png"));
            }
            catch (Exception ex)
            {
                // the plain one is saved
            }
        }
        return (pass ? "+" : "-") + String.format("%d of %d pixels differ (%.2f %%; %d random), picture used %s, old way %s",
                                                  diff, npc, share * 100, noise, usedA ? "yes" : "no", usedB ? "no" : "yes");
    }

    /** Pixel x, y of p has no colour match within one pixel in q, or the other way round. */
    private static boolean differs(int[] p, int[] q, int x, int y, int w, int h)
    {
        return !near(p[y * w + x], q, x, y, w, h) || !near(q[y * w + x], p, x, y, w, h);
    }

    private static boolean near(int c, int[] q, int x, int y, int w, int h)
    {
        for (int dy = -1; dy <= 1; dy++)
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                int u = x + dx, v = y + dy;
                if (u >= 0 && v >= 0 && u < w && v < h && !far(c, q[v * w + u]))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** The NPC drawn on a plain background at the place the inventory draws the player; the pixels (top row first). */
    private static int[] drawOnce(Minecraft mc, EntityLivingBase m, int gx, int gy, int w, int h, boolean layers)
    {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int f = sr.getScaleFactor();
        GL11.glDisable(GL11.GL_BLEND);
        Gui.drawRect(gx, gy, gx + w, gy + h, BACKGROUND);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glColor4f(1, 1, 1, 1);
        boolean was = HumanLayers.enabled;
        HumanLayers.enabled = layers;
        try
        {
            GuiInventory.func_147046_a(gx + w / 2, gy + h - 12, 55, 0, 0, m);
        }
        finally
        {
            HumanLayers.enabled = was;
        }
        int pw = w * f, ph = h * f;
        ByteBuffer buf = BufferUtils.createByteBuffer(pw * ph * 4);
        GL11.glReadPixels(gx * f, mc.displayHeight - (gy + h) * f, pw, ph, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        int[] px = new int[pw * ph];
        for (int y = 0; y < ph; y++)
        {
            for (int x = 0; x < pw; x++)
            {
                int i = ((ph - 1 - y) * pw + x) * 4; // GL rows start at the bottom
                px[y * pw + x] = 0xFF000000 | (buf.get(i) & 255) << 16 | (buf.get(i + 1) & 255) << 8 | (buf.get(i + 2) & 255);
            }
        }
        return px;
    }

    private static boolean far(int p, int q)
    {
        return Math.abs((p >> 16 & 255) - (q >> 16 & 255)) > 24 || Math.abs((p >> 8 & 255) - (q >> 8 & 255)) > 24
            || Math.abs((p & 255) - (q & 255)) > 24;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null)
        {
            return;
        }
        if (step == 1 && want != shown && want >= 0)
        {
            for (Entity e : spawned)
            {
                e.setDead(); // one at a time: no fight, no muzzle flash, no hurt flash
            }
            spawned.clear();
            Entity e = spawn(w, KINDS[want], DevTestArena.X - 10 + 0.5, DevTestArena.Z + 0.5, 270);
            id = e == null ? -1 : e.getEntityId();
            shown = want;
            return;
        }
        if (ready || step == 1 || step == 3 || step == 5)
        {
            return;
        }
        if (step == 0)
        {
            spawning = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false"); // no strays
            DevTestArena.build(w, p);
            w.setWorldTime(18000); // night: infected do not burn
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            p.setGameType(WorldSettings.GameType.CREATIVE); // Decimation's NPCs leave a creative player alone
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 90, 10); // west
            step = 1;
            ready = true;
        }
        else if (step == 2)
        {
            for (Entity e : spawned)
            {
                e.setDead();
            }
            spawned.clear();
            for (int i = 0; i < 40; i++)
            {
                double x = DevTestArena.X - 20 - (i / 10) * 2.5, z = DevTestArena.Z + 0.5 + (i % 10 - 4.5) * 2.2;
                spawn(w, i < 20 ? "Human" : "Infected", x, z, 270);
            }
            step = 3;
            ready = true;
        }
        else if (step == 4)
        {
            for (Entity e : spawned)
            {
                e.setDead();
            }
            spawned.clear();
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning);
            step = 5;
        }
    }

    /** A mob of this kind facing the player (Decimation turns some spawns into other mobs: retried). */
    private Entity spawn(WorldServer w, String kind, double x, double z, float yaw)
    {
        for (int tries = 0; tries < 5; tries++)
        {
            Entity e = EntityList.createEntityByName(kind, w);
            if (!(e instanceof EntityLiving))
            {
                return null;
            }
            e.setLocationAndAngles(x, DevTestArena.Y, z, yaw, 0);
            if (w.spawnEntityInWorld(e))
            {
                spawned.add(e);
                return e;
            }
        }
        return null;
    }
}
