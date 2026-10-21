package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.decimation.fixes.gunsmith.GunParts;
import net.decimation.fixes.gunsmith.GunSlot;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

/**
 * The new attachments (docs/superpowers/specs/2026-10-21-new-attachments-design.md): the RK-1 Foregrip and the ELCAN
 * SpecterDR 1x/4x are registered with their names, fit what the spec says (the Gunsmith's own rule, GunParts.fits) and
 * lie in Decimation's loot wherever the Foregrip / the 4x lie; recoil per grip; grips placed per gun; the SpecterDR's
 * 1x / 4x switch. perf=true measures the budgets instead (spec section 5): frames aimed through the SpecterDR at 4x and
 * 1x against the ACOG, the gun drawn with the RK-1 against none and with the SpecterDR against the ACOG (each draw
 * timed with the GPU), GL errors and stack depth around those draws, and SpecterMode's allocations.
 */
public class AttachmentsTest extends DevTestMode
{
    public String name() { return "attachments"; }

    private int ticks, step;
    private volatile Runnable onServer;
    /** Recoil per grip (none, Foregrip, RK-1): the view's turn up and sideways in one recoil tick. */
    private static final String[] GRIPS = {"", "foregrip", "rk1"};

    private final boolean perf = Boolean.getBoolean("deciworldgen.autotest.perf"); // per run: live settings stay set

    public boolean client(Minecraft mc)
    {
        ticks++;
        if (perf && step < 10)
        {
            step = 10;
            ticks = 1;
        }
        switch (step)
        {
            case 0:
                if (ticks == 5)
                {
                    items();
                    step++;
                    ticks = 0;
                }
                return true;
            case 1:
                if (!recoil(mc))
                {
                    step++;
                    ticks = 0;
                }
                return true;
            case 2:
                if (!placement(mc))
                {
                    step++;
                    ticks = 0;
                }
                return true;
            case 3:
                if (!specter(mc))
                {
                    step++;
                    ticks = 0;
                }
                return true;
            case 4:
                return rails(mc);
            case 10:
                return perf(mc);
            default:
                return false;
        }
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    /**
     * Each grip in turn on an M4A4 in the hand: a known recoil (1000 up, 1000 sideways) set in PlayerData, then one run
     * of Decimation's own GunItem.clientHeldTick, the code that turns the view (Deci.runClientHeldTick: no sway or
     * server sync in between). It keeps 4 % of the recoil and turns the view by it, divided by the grip's factor: 40 x
     * the factor up and sideways. The ratios against no grip are the factors (spec section 2). A real shot cannot be
     * measured: its turn is that small 4 % and Decimation's view sway moves the view more per tick (seen).
     */
    private boolean recoil(Minecraft mc)
    {
        if (ticks == 1)
        {
            onServer = () -> {
                EntityPlayerMP p = player();
                ItemStack gun = new ItemStack((Item) Item.itemRegistry.getObject("deci:m4a4"));
                gun.stackTagCompound = new NBTTagCompound();
                gun.stackTagCompound.setInteger("ammo", 30);
                p.inventory.setInventorySlotContents(0, gun);
                p.inventory.currentItem = 0;
            };
            mc.thePlayer.inventory.currentItem = 0;
        }
        if (ticks == 20)
        {
            recoilChecks(name(), mc, mc.thePlayer.getHeldItem());
            return false;
        }
        return true;
    }

    /** Recoil with no grip, the Foregrip and the RK-1 on the held gun (turn): the ratios against no grip. */
    static void recoilChecks(String mode, Minecraft mc, ItemStack held)
    {
        double[][] sums = new double[GRIPS.length][];
        for (int g = 0; g < GRIPS.length; g++)
        {
            sums[g] = turn(mode, mc, held, GRIPS[g]);
        }
        double up0 = sums[0][0], side0 = sums[0][2];
        DevTestResults.value(mode, "recoil: no grip", String.format("up %.3f, sideways %.3f degrees", up0, side0));
        ratio(mode, "recoil: Foregrip", sums[1], up0, side0, 1 / 3.0, 1 / 3.0);
        ratio(mode, "recoil: RK-1", sums[2], up0, side0, 0.5, 0.25);
    }

    /**
     * One recoil tick on this held gun with this grip (its own grip put back after): a known recoil (1000 up, 1000
     * sideways) in PlayerData, then Decimation's own GunItem.clientHeldTick once; {up, 1, sideways, 1} in degrees.
     */
    static double[] turn(String mode, Minecraft mc, ItemStack held, String grip)
    {
        Deci.Player d = Deci.player(mc.thePlayer);
        if (held == null || held.stackTagCompound == null || d == null)
        {
            return new double[] {0, 1, 0, 1};
        }
        String was = held.stackTagCompound.getString("gripAttach");
        held.stackTagCompound.setString("gripAttach", grip);
        mc.thePlayer.rotationPitch = 0;
        d.setRecoil(1000, 1000);
        float p0 = mc.thePlayer.rotationPitch, y0 = mc.thePlayer.rotationYawHead;
        try
        {
            Deci.runClientHeldTick(held, mc.thePlayer);
        }
        catch (Exception e)
        {
            DevTestResults.value(mode, "recoil tick failed", String.valueOf(e));
        }
        double[] r = {p0 - mc.thePlayer.rotationPitch, 1, y0 - mc.thePlayer.rotationYawHead, 1};
        d.setRecoil(0, 0);
        mc.thePlayer.rotationPitch = 0;
        held.stackTagCompound.setString("gripAttach", was);
        return r;
    }

    /**
     * Grips placed per gun (GripPlacement at startup): on the M4A4 Decimation's Foregrip, the Laser and our RK-1 have
     * their own moved copy (not the one spot for all guns), the Foregrip's parent part without its stray plate; shots of
     * four grips on their guns in the Gunsmith screen (Decimation's renderer draws them as in the world).
     */
    private boolean placement(Minecraft mc)
    {
        if (ticks == 1)
        {
            Item m4 = (Item) Item.itemRegistry.getObject("deci:m4a4");
            for (String g : new String[] {"foregrip", "rk1", "laser"})
            {
                float[] o = Deci.attachmentOffset(m4, g);
                DevTestResults.check(name(), "placement: " + g + " moved on the m4a4", o == null ? "the one spot" : String.format("%.2f %.2f %.2f",
                    o[0], o[1], o[2]), o != null && (o[0] != 0 || o[1] != 0 || o[2] != 0), "its own offset from grip_offsets.txt");
            }
            int batch = Deci.attachmentBatch(m4, "rk1");
            DevTestResults.check(name(), "placement: the RK-1's copy drawn from one list", batch < 0 ? "part by part" : batch + " parts in one list",
                                 batch == 51, "51 parts in one list (Deci.BatchedPart)");
            Map<String, Integer> parts = Deci.attachmentPartBoxes(m4, "foregrip");
            boolean plain = parts != null && Integer.valueOf(0).equals(parts.get("grip"))
                            && parts.keySet().stream().noneMatch(n -> n.startsWith("ammoModel"));
            DevTestResults.check(name(), "placement: Foregrip without its stray plate", parts == null ? "no copy" : "own boxes " + parts,
                                 plain, "{grip=0}: the parent of its 8 parts drawn without its plate, no magazine");
        }
        // a grip on two rifles, an SMG and a pistol (the RK-1 and the Foregrip fit railed rifles only)
        String[][] shots = {{"deci:m4a4", "rk1"}, {"deci:fnscar", "foregrip"}, {"deci:ump45", "flashlight"}, {"deci:glock17", "laser"}};
        int k = (ticks - 5) / 40, t = (ticks - 5) % 40;
        if (ticks >= 5 && k < shots.length)
        {
            final String[] sh = shots[k];
            if (t == 0)
            {
                onServer = () -> {
                    EntityPlayerMP p = player();
                    ItemStack gun = new ItemStack((Item) Item.itemRegistry.getObject(sh[0]));
                    gun.stackTagCompound = new NBTTagCompound();
                    gun.stackTagCompound.setInteger("ammo", 10);
                    gun.stackTagCompound.setString("gripAttach", sh[1]);
                    p.inventory.setInventorySlotContents(0, gun);
                    p.inventory.currentItem = 0;
                };
                mc.thePlayer.inventory.currentItem = 0;
                net.decimation.worldgen.DevAutoTest.keepScreen = true;
            }
            if (t == 8) GunsmithTest.pressAttach(mc);
            if (t == 34) DevTestUtil.screenshot(mc, name(), "grip_" + sh[1] + "_" + sh[0].substring(5) + ".png");
            if (t == 36) mc.thePlayer.closeScreen();
            return true;
        }
        if (ticks >= 5)
        {
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            return false;
        }
        return true;
    }

    /**
     * Foregrips need a rail (user, 21 Oktober 2026; docs/references/grip_rails.md): the Gunsmith refuses a Foregrip or
     * RK-1 on a rail-less rifle (lights still fit), NPC rifles without a rail never carry one, and a vertical grip on a
     * rail-less gun pops off into the inventory (fixes/GripStrip), never while the Gunsmith is open on it (its slots are
     * a view of the gun: taking it then would copy it), and onto the ground when the inventory is full.
     */
    private boolean rails(Minecraft mc)
    {
        if (ticks == 1)
        {
            ItemStack rpk = stack("deci:rpk"), fnscar = stack("deci:fnscar"), barrett = stack("deci:barrett");
            boolean a = GunParts.fits(GunSlot.GRIP, rpk, stack("deci:foregrip")), b = GunParts.fits(GunSlot.GRIP, rpk, stack("deci:rk1"));
            boolean c = GunParts.fits(GunSlot.GRIP, fnscar, stack("deci:rk1")), d = GunParts.fits(GunSlot.GRIP, rpk, stack("deci:laser"));
            boolean e = GunParts.fits(GunSlot.GRIP, barrett, stack("deci:foregrip"));
            DevTestResults.check(name(), "rails: Gunsmith fits a foregrip only on a rail", "rpk foregrip " + a + ", rpk rk1 " + b
                                 + ", fnscar rk1 " + c + ", rpk laser " + d + ", barrett foregrip " + e, !a && !b && c && d && !e,
                                 "rpk false, false; fnscar true; rpk laser true; barrett false");
            int rpkGrips = 0, m4Grips = 0;
            for (int i = 0; i < 400; i++)
            {
                for (String k : net.decimation.fixes.NpcLoadouts.instance().rifleAttachments("rpk"))
                {
                    rpkGrips += k.equals("grip=foregrip") ? 1 : 0;
                }
                for (String k : net.decimation.fixes.NpcLoadouts.instance().rifleAttachments("m4a4"))
                {
                    m4Grips += k.equals("grip=foregrip") ? 1 : 0;
                }
            }
            DevTestResults.check(name(), "rails: NPC rifles carry a Foregrip only on a rail", "rpk " + rpkGrips + ", m4a4 " + m4Grips + " of 400",
                                 rpkGrips == 0 && m4Grips > 0, "rpk 0, m4a4 some");
        }
        if (ticks == 2)
        {
            onServer = () -> {
                EntityPlayerMP p = player();
                p.inventory.clearInventory(null, -1);
                p.inventory.setInventorySlotContents(0, gunWith("deci:rpk", "foregrip"));
                p.inventory.setInventorySlotContents(1, gunWith("deci:rpk", "laser"));
                p.inventory.setInventorySlotContents(2, gunWith("deci:fnscar", "rk1"));
                p.inventory.currentItem = 0;
            };
            mc.thePlayer.inventory.currentItem = 0;
            net.decimation.worldgen.DevAutoTest.keepScreen = true;
        }
        if (ticks == 8)
        {
            GunsmithTest.pressAttach(mc);
        }
        if (ticks == 60)
        {
            onServer = () -> {
                EntityPlayerMP p = player();
                DevTestResults.check(name(), "rails: kept while the Gunsmith is open", "gun " + grip(p, 0) + ", loose " + count(p, "foregrip")
                                     + ", screen " + p.openContainer.getClass().getSimpleName(),
                                     "foregrip".equals(grip(p, 0)) && count(p, "foregrip") == 0, "gun foregrip, loose 0");
            };
        }
        if (ticks == 62)
        {
            mc.thePlayer.closeScreen();
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
        }
        if (ticks == 120)
        {
            onServer = () -> {
                EntityPlayerMP p = player();
                DevTestResults.check(name(), "rails: a foregrip on a rail-less gun pops off", "rpk " + grip(p, 0) + ", loose foregrips "
                                     + count(p, "foregrip") + ", rpk laser " + grip(p, 1) + ", fnscar " + grip(p, 2),
                                     "".equals(grip(p, 0)) && count(p, "foregrip") == 1 && "laser".equals(grip(p, 1)) && "rk1".equals(grip(p, 2)),
                                     "rpk none, 1 loose, laser and fnscar's rk1 kept");
                p.inventory.clearInventory(null, -1);
                for (int i = 1; i < p.inventory.mainInventory.length; i++)
                {
                    p.inventory.setInventorySlotContents(i, new ItemStack(net.minecraft.init.Blocks.stone, 64));
                }
                p.inventory.setInventorySlotContents(0, gunWith("deci:rpk", "rk1"));
            };
        }
        if (ticks == 180)
        {
            onServer = () -> {
                EntityPlayerMP p = player();
                int dropped = 0;
                for (Object o : p.worldObj.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, p.boundingBox.expand(4, 4, 4)))
                {
                    net.minecraft.entity.item.EntityItem it = (net.minecraft.entity.item.EntityItem) o;
                    if (it.getEntityItem().getItem() == Item.itemRegistry.getObject("deci:rk1"))
                    {
                        dropped += it.getEntityItem().stackSize;
                    }
                    it.setDead();
                }
                DevTestResults.check(name(), "rails: full inventory drops it", "rpk " + grip(p, 0) + ", dropped " + dropped,
                                     "".equals(grip(p, 0)) && dropped == 1, "rpk none, 1 RK-1 on the ground");
                p.inventory.clearInventory(null, -1);
            };
        }
        return ticks < 185;
    }

    private static ItemStack gunWith(String gun, String grip)
    {
        ItemStack s = new ItemStack((Item) Item.itemRegistry.getObject(gun));
        s.stackTagCompound = new NBTTagCompound();
        s.stackTagCompound.setInteger("ammo", 10);
        s.stackTagCompound.setString("gripAttach", grip);
        return s;
    }

    private static String grip(EntityPlayerMP p, int slot)
    {
        ItemStack s = p.inventory.getStackInSlot(slot);
        return s == null || s.stackTagCompound == null ? "no gun" : s.stackTagCompound.getString("gripAttach");
    }

    private static int count(EntityPlayerMP p, String item)
    {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            n += s != null && s.getItem() == Item.itemRegistry.getObject("deci:" + item) ? s.stackSize : 0;
        }
        return n;
    }

    /** The three guns of the SpecterDR step (slots 0 .. 2): two with the SpecterDR, one with the ACOG. */
    private static final String[] SPECTER_SIGHTS = {"specterdr", "specterdr", "ta11acog"};
    private String runId;
    private int clicks0;

    /**
     * The SpecterDR's switch (spec section 2): 4x by default (magnification 4, the gun and its glass kept, no black
     * overlay); the key (SpecterKey, default G) flips the held gun's SpecterDR to 1x (1.25) and back; another gun keeps
     * its own mode; with the ACOG the key changes nothing and plays no click; aimed shots at 1x and 4x. Each run gives
     * its guns new ids: the modes live for the session (a live game keeps them between runs).
     */
    private boolean specter(Minecraft mc)
    {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (ticks == 1)
        {
            runId = Long.toString(System.nanoTime(), 36);
            onServer = () -> {
                EntityPlayerMP p = player();
                for (int i = 0; i < SPECTER_SIGHTS.length; i++)
                {
                    ItemStack gun = new ItemStack((Item) Item.itemRegistry.getObject("deci:m4a4"));
                    gun.stackTagCompound = new NBTTagCompound();
                    gun.stackTagCompound.setInteger("ammo", 30);
                    gun.stackTagCompound.setString("sightAttach", SPECTER_SIGHTS[i]);
                    gun.stackTagCompound.setString("uniqueID", "specter" + i + "_" + runId);
                    p.inventory.setInventorySlotContents(i, gun);
                }
                p.inventory.currentItem = 0;
            };
            mc.thePlayer.inventory.currentItem = 0;
        }
        if (ticks == 20)
        {
            check("specter: 4x by default", held, 4.0f, false);
            clicks0 = net.decimation.fixes.SpecterKey.clicks();
            press();
        }
        if (ticks == 24)
        {
            check("specter: the key switches to 1x", held, 1.25f, net.decimation.fixes.SpecterKey.clicks() != clicks0 + 1);
            press();
        }
        if (ticks == 28)
        {
            check("specter: the key again: 4x", held, 4.0f, net.decimation.fixes.SpecterKey.clicks() != clicks0 + 2);
            press();                                   // the first gun to 1x, then the second gun in hand
        }
        if (ticks == 32)
        {
            mc.thePlayer.inventory.currentItem = 1;
        }
        if (ticks == 36)
        {
            ItemStack first = mc.thePlayer.inventory.getStackInSlot(0);
            float other = net.decimation.fixes.ScopeZoom.magnificationFor(held);
            boolean firstFour = net.decimation.fixes.SpecterMode.isFour(first, 0);
            DevTestResults.check(name(), "specter: another gun keeps its own mode", String.format("second %.2f, first %s", other,
                                 firstFour ? "4x" : "1x"), Math.abs(other - 4f) < 0.01f && !firstFour, "second 4.00, first 1x");
            mc.thePlayer.inventory.currentItem = 2;
        }
        if (ticks == 41)
        {
            clicks0 = net.decimation.fixes.SpecterKey.clicks();
            press();
        }
        if (ticks == 45)
        {
            float acog = net.decimation.fixes.ScopeZoom.magnificationFor(held);
            int clicks = net.decimation.fixes.SpecterKey.clicks() - clicks0;
            DevTestResults.check(name(), "specter: the key with the ACOG", String.format("%.2f, %d clicks", acog, clicks),
                                 Math.abs(acog - 3.5f) < 0.01f && clicks == 0, "3.50, 0 clicks (nothing changes)");
            mc.thePlayer.inventory.currentItem = 0;    // the first gun, at 1x: aim through it
            aim(mc, 1);
        }
        if (ticks == 46)
        {
            onServer = () -> {                          // shots at noon, clear, full health (no damage tint)
                EntityPlayerMP p = player();
                if (p != null)
                {
                    p.worldObj.setWorldTime(6000);
                    p.worldObj.getWorldInfo().setRaining(false);
                    p.worldObj.getWorldInfo().setThundering(false);
                    p.setHealth(p.getMaxHealth());
                }
            };
        }
        if (ticks == 85)
        {
            double z = net.decimation.fixes.ScopeZoom.currentZoom();
            DevTestResults.check(name(), "specter: aimed at 1x", String.format("zoom %.2f", z), Math.abs(z - 1.25) < 0.05, "zoom 1.25");
            DevTestUtil.screenshot(mc, name(), "specter_1x.png");
            press();
        }
        if (ticks == 115)
        {
            double z = net.decimation.fixes.ScopeZoom.currentZoom();
            boolean overlay = net.decimation.fixes.ScopeZoom.overlayShown();
            DevTestResults.check(name(), "specter: aimed at 4x, gun and glass kept", String.format("zoom %.2f, overlay %s", z, overlay),
                                 Math.abs(z - 4.0) < 0.1 && !overlay, "zoom 4.00, overlay false");
            DevTestUtil.screenshot(mc, name(), "specter_4x.png");
        }
        if (ticks == 116)
        {
            aim(mc, 0);
        }
        return ticks < 120;                            // the server takes the aim off before the mode ends
    }

    /**
     * Aimed through these (slots 0 .. 3, all M4A4s) in pairs of the same zoom: each SpecterDR mode against our sight of
     * that magnification (the ACOG 3.5x, the EOTech 1.25x: the same glass and reticle path). A wider view draws more of
     * the world: the SpecterDR at 1x cost 3.2 ms more than the ACOG at 3.5x in the first run, as the EOTech does; the
     * frames are taken looking up at the sky.
     */
    private static final String[] AIMED = {"ACOG", "SpecterDR 4x", "EOTech", "SpecterDR 1x"};
    private static final String[] AIMED_SIGHTS = {"ta11acog", "specterdr", "eotech558", "specterdr"};
    private static final float[] AIMED_ZOOM = {3.5f, 4f, 1.25f, 1.25f};
    /** The held M4A4 drawn once more each frame as one of these {sightAttach, gripAttach}, in turn (Decimation's own
     *  Foregrip and 4x as references). */
    private static final String[][] DRAWN = {{"", ""}, {"", "foregrip"}, {"", "rk1"}, {"4x", ""}, {"ta11acog", ""}, {"specterdr", ""},
                                             {"eotech558", ""}, {"", "rk1", "one by one"}};
    private static final String[] DRAWN_NAMES = {"none", "Foregrip", "RK-1", "4x", "ACOG", "SpecterDR", "EOTech", "RK-1 one by one"};
    private static final int AIM_FRAMES = 100, ROUNDS = 6, DRAWS = 200;
    private final List<List<Long>> aimed = new ArrayList<List<Long>>();
    /** Per round and config: the median frame (ms), for differences of neighbours in time. */
    private final double[][] roundMs = new double[ROUNDS][4];
    private final double[] zoomSeen = new double[AIMED.length];
    /** Per config, the last glass box on screen (window pixels): ScopeZoom copies it plus a margin every aimed frame. */
    private final float[][] glassSeen = new float[AIMED.length][];
    private int config = -1, round, settle = -1, perfEnd = -1;
    private static boolean perfProbes;

    /** Frame to frame times (RenderTickEvent START, frame cap off) into `into` while it is set. */
    public static final class FrameClock
    {
        static volatile List<Long> into;
        static volatile int count;
        private long last;

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void tick(cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent e)
        {
            if (e.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.START)
            {
                return;
            }
            long now = System.nanoTime();
            List<Long> l = into;
            if (l != null && last != 0)
            {
                l.add(now - last);
                count++;
            }
            last = now;
        }
    }

    /**
     * After the world each frame while variants are set: the held gun drawn once more as Decimation draws it
     * (GunItemRenderer, EQUIPPED, two blocks ahead), as the next variant, glFinish before and after (its own cost, GPU
     * included); GL errors and the modelview depth around it. The first two rounds compile display lists: not timed.
     */
    public static final class DrawProbe
    {
        static volatile String[][] variants;
        /** Frames left that draw the RK-1 both ways into a cleared middle of the screen and compare the pixels. */
        static volatile int compare;
        /** {pixels the gun covered, pixels that differ, frames compared} once compare reached 0. */
        static volatile int[] result;
        private static java.nio.ByteBuffer pa, pb;
        private static int covered, differ, frames;
        static final List<List<Long>> times = new ArrayList<List<Long>>(), cpu = new ArrayList<List<Long>>();
        static int n, errors, depthChanged;
        private static net.minecraft.client.renderer.RenderBlocks blocks;

        static void start(String[][] v)
        {
            times.clear();
            cpu.clear();
            for (int i = 0; i < v.length; i++)
            {
                times.add(new ArrayList<Long>());
                cpu.add(new ArrayList<Long>());
            }
            n = errors = depthChanged = 0;
            variants = v;
        }

        static int samples()
        {
            int k = Integer.MAX_VALUE;
            for (List<Long> t : times)
            {
                k = Math.min(k, t.size());
            }
            return times.isEmpty() ? 0 : k;
        }

        /**
         * The held gun with the RK-1 drawn into the middle half of the screen (scissored, cleared to black first) from its
         * one list, read back, then part by part, read back: pixels covered and pixels that differ.
         */
        private static void compareOnce(Minecraft mc, IItemRenderer r, ItemStack held)
        {
            int w = mc.displayWidth, h = mc.displayHeight, x0 = w / 4, y0 = h / 4, cw = w / 2, ch = h / 2;
            if (pa == null || pa.capacity() < cw * ch * 4)
            {
                pa = org.lwjgl.BufferUtils.createByteBuffer(cw * ch * 4);
                pb = org.lwjgl.BufferUtils.createByteBuffer(cw * ch * 4);
            }
            ItemStack s = held.copy();
            s.stackTagCompound.setString("sightAttach", "");
            s.stackTagCompound.setString("gripAttach", "rk1");
            net.minecraft.util.Vec3 look = mc.thePlayer.getLookVec();
            boolean batched = net.decimation.fixes.Deci.BatchedPart.enabled;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x0, y0, cw, ch);
            for (int pass = 0; pass < 2; pass++)
            {
                net.decimation.fixes.Deci.BatchedPart.enabled = pass == 0;
                GL11.glClearColor(0, 0, 0, 1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glPushMatrix();
                GL11.glTranslated(look.xCoord * 1.2, look.yCoord * 1.2, look.zCoord * 1.2);
                r.renderItem(IItemRenderer.ItemRenderType.EQUIPPED, s, blocks, mc.thePlayer);
                GL11.glPopMatrix();
                java.nio.ByteBuffer b = pass == 0 ? pa : pb;
                b.clear();
                GL11.glReadPixels(x0, y0, cw, ch, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, b);
            }
            net.decimation.fixes.Deci.BatchedPart.enabled = batched;
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            for (int i = 0; i < cw * ch * 4; i += 4)
            {
                boolean ina = (pa.get(i) | pa.get(i + 1) | pa.get(i + 2)) != 0, inb = (pb.get(i) | pb.get(i + 1) | pb.get(i + 2)) != 0;
                covered += ina || inb ? 1 : 0;
                differ += pa.get(i) != pb.get(i) || pa.get(i + 1) != pb.get(i + 1) || pa.get(i + 2) != pb.get(i + 2) ? 1 : 0;
            }
            frames++;
        }

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent e)
        {
            Minecraft mc = Minecraft.getMinecraft();
            if (compare > 0 && mc.thePlayer != null && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().stackTagCompound != null)
            {
                IItemRenderer cr = MinecraftForgeClient.getItemRenderer(mc.thePlayer.getHeldItem(), IItemRenderer.ItemRenderType.EQUIPPED);
                if (cr != null)
                {
                    if (blocks == null)
                    {
                        blocks = new net.minecraft.client.renderer.RenderBlocks();
                    }
                    if (frames == 0)
                    {
                        covered = differ = 0;
                    }
                    compareOnce(mc, cr, mc.thePlayer.getHeldItem());
                    if (--compare == 0)
                    {
                        result = new int[] {covered, differ, frames};
                        frames = 0;
                    }
                }
                return;
            }
            String[][] v = variants;
            ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
            if (v == null || held == null || held.stackTagCompound == null)
            {
                return;
            }
            IItemRenderer r = MinecraftForgeClient.getItemRenderer(held, IItemRenderer.ItemRenderType.EQUIPPED);
            if (r == null)
            {
                return;
            }
            if (blocks == null)
            {
                blocks = new net.minecraft.client.renderer.RenderBlocks();
            }
            int k = n % v.length;
            ItemStack s = held.copy();
            s.stackTagCompound.setString("sightAttach", v[k][0]);
            s.stackTagCompound.setString("gripAttach", v[k][1]);
            while (GL11.glGetError() != 0)
            {
                // errors of the frame so far: not the draw's
            }
            int d0 = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            net.minecraft.util.Vec3 look = mc.thePlayer.getLookVec();
            GL11.glPushMatrix();
            GL11.glTranslated(look.xCoord * 2, look.yCoord * 2, look.zCoord * 2);
            boolean batched = net.decimation.fixes.Deci.BatchedPart.enabled;
            net.decimation.fixes.Deci.BatchedPart.enabled = v[k].length < 3;   // "one by one": the copy's parts drawn as before
            GL11.glFinish();
            long t0 = System.nanoTime();
            r.renderItem(IItemRenderer.ItemRenderType.EQUIPPED, s, blocks, mc.thePlayer);
            long submitted = System.nanoTime() - t0;          // the CPU's part: the calls handed to the driver
            GL11.glFinish();
            long dt = System.nanoTime() - t0;
            net.decimation.fixes.Deci.BatchedPart.enabled = batched;
            GL11.glPopMatrix();
            if (GL11.glGetError() != 0)
            {
                errors++;
            }
            if (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) != d0)
            {
                depthChanged++;
            }
            if (n >= v.length * 2)
            {
                times.get(k).add(dt);
                cpu.get(k).add(submitted);
            }
            n++;
        }
    }

    private static double median(List<Long> l)
    {
        List<Long> c = new ArrayList<Long>(l);
        Collections.sort(c);
        return c.isEmpty() ? -1 : c.get(c.size() / 2) / 1e6;
    }

    /**
     * perf=true: in the arena at noon, three M4A4s (ACOG, SpecterDR, SpecterDR flipped to 1x). First SpecterMode's
     * allocation, then the draw probe at the hip (each variant timed), then aimed frames: the three guns in turn,
     * AIM_FRAMES each after the zoom settled, ROUNDS rounds (the turns share the world's own noise).
     */
    private boolean perf(Minecraft mc)
    {
        if (!perfProbes)
        {
            perfProbes = true;
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new FrameClock());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new DrawProbe());
        }
        if (ticks == 1)
        {
            runId = Long.toString(System.nanoTime(), 36);
            onServer = () -> {
                EntityPlayerMP p = player();
                DevTestArena.build(p.worldObj, p);
                p.worldObj.setWorldTime(6000);
                p.worldObj.getWorldInfo().setRaining(false);
                p.worldObj.getWorldInfo().setThundering(false);
                p.setHealth(p.getMaxHealth());
                for (int i = 0; i < AIMED_SIGHTS.length; i++)
                {
                    ItemStack gun = new ItemStack((Item) Item.itemRegistry.getObject("deci:m4a4"));
                    gun.stackTagCompound = new NBTTagCompound();
                    gun.stackTagCompound.setInteger("ammo", 30);
                    gun.stackTagCompound.setString("sightAttach", AIMED_SIGHTS[i]);
                    gun.stackTagCompound.setString("uniqueID", "perf" + i + "_" + runId);
                    p.inventory.setInventorySlotContents(i, gun);
                }
                p.inventory.currentItem = 0;
            };
            mc.thePlayer.inventory.currentItem = 0;
            DevTestUtil.unlimitedFps(mc);
            aimed.clear();
            for (int i = 0; i < AIMED.length; i++)
            {
                aimed.add(new ArrayList<Long>());
            }
            config = -1;
            round = 0;
            settle = perfEnd = -1;
        }
        if (ticks == 30)
        {
            mc.thePlayer.rotationYaw = 90;   // down the arena's long side
            mc.thePlayer.rotationPitch = 0;
            ItemStack last = mc.thePlayer.inventory.getStackInSlot(3);
            if (last != null && net.decimation.fixes.SpecterMode.isFour(last, 3))
            {
                net.decimation.fixes.SpecterMode.toggle(last, 3);       // the fourth gun's SpecterDR at 1x
            }
            allocation(mc);
            DrawProbe.start(DRAWN);
        }
        if (DrawProbe.compare == 0 && DrawProbe.result != null)
        {
            int[] c = DrawProbe.result;
            DrawProbe.result = null;
            DevTestResults.check(name(), "perf: the RK-1 from one list draws the same", c[1] + " of " + c[0] + " gun pixels differ ("
                                 + c[2] + " frames)", c[0] > 500 && c[1] <= c[0] / 200, "at most 0.5 % of the pixels (the gun covering 500 or more)");
        }
        if (ticks > 30 && config < 0 && DrawProbe.variants != null && DrawProbe.samples() >= DRAWS)
        {
            DrawProbe.variants = null;
            double[] m = new double[DRAWN.length], c = new double[DRAWN.length];
            StringBuilder all = new StringBuilder(), sub = new StringBuilder();
            for (int i = 0; i < m.length; i++)
            {
                m[i] = median(DrawProbe.times.get(i));
                c[i] = median(DrawProbe.cpu.get(i));
                all.append(i == 0 ? "" : ", ").append(String.format("%s %.3f", DRAWN_NAMES[i], m[i]));
                sub.append(i == 0 ? "" : ", ").append(String.format("%s %.3f", DRAWN_NAMES[i], c[i]));
            }
            DevTestResults.value(name(), "perf: gun draw, ms (median of " + DRAWS + ", GPU included)", all.toString());
            DevTestResults.value(name(), "perf: gun draw, ms handed to the driver (CPU)", sub.toString());
            DevTestResults.check(name(), "perf: the RK-1's draw", String.format("%+.3f ms (one by one %+.3f, Foregrip %+.3f)", m[2] - m[0],
                                 m[7] - m[0], m[1] - m[0]), m[2] - m[0] <= 0.1, "at most +0.100 ms over no grip");
            DrawProbe.compare = 3;
            DevTestResults.check(name(), "perf: the SpecterDR's draw against the ACOG's and the EOTech's", String.format(
                                 "%+.3f ms, %+.3f ms", m[5] - m[4], m[5] - m[6]), m[5] - m[4] <= 0.5 && m[5] - m[6] <= 0.5,
                                 "at most +0.500 ms each");
            DevTestResults.check(name(), "perf: GL clean around the timed draws", DrawProbe.errors + " errors, depth changed "
                                 + DrawProbe.depthChanged + " times in " + DrawProbe.n + " draws",
                                 DrawProbe.errors == 0 && DrawProbe.depthChanged == 0 && DrawProbe.n > 0, "0 errors, depth unchanged");
            config = 0;
            mc.thePlayer.inventory.currentItem = 0;
            // aimed at the sky: no chunks in view, so the frame is mostly the gun, its sight, the glass copy and the HUD
            // (at the horizon a 1.25x view draws about 4.5 ms more world than a 4x one, and that world's own noise was
            // larger than the budget; looking down from the arena, 150 high, a 1.25x view took in the land below: 24 ms)
            mc.thePlayer.rotationPitch = -60;
            aim(mc, 1);
            settle = ticks + 40;            // the first aim learns the glass (at most 1.5 s), the zoom eases in
        }
        if (config >= 0 && perfEnd < 0)
        {
            if (ticks == settle)
            {
                FrameClock.count = 0;
                FrameClock.into = aimed.get(config);
            }
            else if (ticks > settle && FrameClock.count >= AIM_FRAMES)
            {
                FrameClock.into = null;
                List<Long> all = aimed.get(config);
                roundMs[round][config] = median(all.subList(all.size() - FrameClock.count, all.size()));
                zoomSeen[config] = net.decimation.fixes.ScopeZoom.currentZoom();
                glassSeen[config] = Deci.scopeGlassBox();
                config++;
                if (config == AIMED.length)
                {
                    config = 0;
                    round++;
                }
                if (round == ROUNDS)
                {
                    report();
                    aim(mc, 0);
                    perfEnd = ticks + 5;
                }
                else
                {
                    mc.thePlayer.inventory.currentItem = config;
                    mc.thePlayer.rotationPitch = -60;
                    aim(mc, 1);
                    settle = ticks + 30;
                }
            }
        }
        return perfEnd < 0 || ticks < perfEnd;
    }

    private void report()
    {
        double[] m = new double[AIMED.length];
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < m.length; i++)
        {
            m[i] = median(aimed.get(i));
            all.append(i == 0 ? "" : ", ").append(String.format("%s %.2f ms (zoom %.2f, %d frames)", AIMED[i], m[i], zoomSeen[i],
                                                                aimed.get(i).size()));
            float[] b = glassSeen[i];
            if (b != null)
            {
                DevTestResults.value(name(), "perf: glass box, " + AIMED[i], String.format("%.0f x %.0f px at %.0f,%.0f (window %d x %d)",
                                     b[2] - b[0], b[3] - b[1], b[0], b[1], Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight));
            }
        }
        DevTestResults.value(name(), "perf: aimed frame (median, " + ROUNDS + " rounds in turn)", all.toString());
        for (int i = 1; i < m.length; i += 2)
        {
            // each round measures the pair one after the other: their difference there cancels the drift between rounds
            List<Long> d = new ArrayList<Long>();
            StringBuilder each = new StringBuilder();
            for (int r = 0; r < ROUNDS; r++)
            {
                double diff = roundMs[r][i] - roundMs[r][i - 1];
                d.add(Math.round(diff * 1e6));
                each.append(r == 0 ? "" : " ").append(String.format("%+.2f", diff));
            }
            double paired = median(d);
            boolean zoomed = Math.abs(zoomSeen[i] - AIMED_ZOOM[i]) < 0.1 && Math.abs(zoomSeen[i - 1] - AIMED_ZOOM[i - 1]) < 0.1;
            DevTestResults.check(name(), "perf: aimed frame, " + AIMED[i] + " against the " + AIMED[i - 1], String.format(
                                 "%+.2f ms (median of the rounds' differences: %s)%s", paired, each, zoomed ? ""
                                 : String.format(", zoom %.2f, %.2f", zoomSeen[i], zoomSeen[i - 1])), paired <= 0.5 && zoomed,
                                 String.format("at most +0.50 ms, zoom %.2f", AIMED_ZOOM[i]));
        }
    }

    /** SpecterMode.isFour is asked every frame while a gun is held (ScopeZoom): it must not allocate. */
    private void allocation(Minecraft mc)
    {
        ItemStack gun = mc.thePlayer.inventory.getStackInSlot(1);
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        if (gun == null || !(mx instanceof com.sun.management.ThreadMXBean))
        {
            DevTestResults.value(name(), "perf: SpecterMode allocation", gun == null ? "no gun" : "not measurable on this JVM");
            return;
        }
        com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) mx;
        long id = Thread.currentThread().getId();
        int four = 0;
        for (int i = 0; i < 20000; i++)
        {
            four += net.decimation.fixes.SpecterMode.isFour(gun, 1) ? 1 : 0;   // warm up
        }
        long a0 = sun.getThreadAllocatedBytes(id);
        for (int i = 0; i < 100000; i++)
        {
            four += net.decimation.fixes.SpecterMode.isFour(gun, 1) ? 1 : 0;
        }
        long a1 = sun.getThreadAllocatedBytes(id);
        // a one time cost (the JIT's switch to compiled code) showed as 3,224 bytes once: the check is per call
        DevTestResults.check(name(), "perf: SpecterMode.isFour allocates nothing", (a1 - a0) + " bytes in 100000 calls (" + four + " at 4x)",
                             a1 - a0 < 10000, "under 0.1 byte a call (10,000 bytes; a string key cost 80 a call)");
    }

    /** The held gun's magnification and overlay; `clickWrong` adds a failure when the click count is not as expected. */
    private void check(String key, ItemStack held, float mag, boolean clickWrong)
    {
        float m = net.decimation.fixes.ScopeZoom.magnificationFor(held);
        boolean overlay = net.decimation.fixes.ScopeZoom.overlays(held);
        DevTestResults.check(name(), key, String.format("%.2f, overlay %s%s", m, overlay, clickWrong ? ", click count wrong" : ""),
                             Math.abs(m - mag) < 0.01f && !overlay && !clickWrong, String.format("%.2f, overlay false, one click a press", mag));
    }

    /** A press of the SpecterDR key as the keyboard makes it (KeyBinding state down, a tick, up). */
    static void press()
    {
        net.minecraft.client.settings.KeyBinding k = net.decimation.fixes.SpecterKey.binding();
        if (k != null)
        {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(k.getKeyCode(), true);
            net.minecraft.client.settings.KeyBinding.onTick(k.getKeyCode());
            net.minecraft.client.settings.KeyBinding.setKeyBindState(k.getKeyCode(), false);
        }
    }

    /** Aim mode on both sides (Decimation's player data). */
    private void aim(Minecraft mc, int mode)
    {
        Deci.setAimMode(mc.thePlayer, mode);
        onServer = () -> {
            EntityPlayerMP p = player();
            if (p != null)
            {
                Deci.setAimMode(p, mode);
            }
        };
    }

    private static void ratio(String mode, String key, double[] s, double up0, double side0, double up, double side)
    {
        double u = s[0] / Math.max(1e-9, s[1]) / up0, w = s[2] / Math.max(1e-9, s[3]) / side0;
        DevTestResults.check(mode, key, String.format("up %.3f, sideways %.3f of no grip (no grip: %.3f, %.3f)", u, w, up0, side0),
                             Math.abs(u - up) <= 0.02 * Math.max(1, up / 0.5) && Math.abs(w - side) <= 0.02 * Math.max(1, side / 0.5),
                             String.format("up %.3f, sideways %.3f (within 2 %%)", up, side));
    }

    private static EntityPlayerMP player()
    {
        MinecraftServer s = MinecraftServer.getServer();
        return s == null || s.getConfigurationManager().playerEntityList.isEmpty() ? null
            : (EntityPlayerMP) s.getConfigurationManager().playerEntityList.get(0);
    }

    private void items()
    {
        name("RK-1 registered", "deci:rk1", "RK-1 Foregrip");
        name("SpecterDR registered", "deci:specterdr", "ELCAN SpecterDR 1x/4x");
        name("EOTech named", "deci:eotech558", "EOTech 558");
        name("ACOG named", "deci:ta11acog", "ACOG TA11 3.5x");
        ItemStack m4 = stack("deci:m4a4"), glock = stack("deci:glock17"), rk1 = stack("deci:rk1"), specter = stack("deci:specterdr");
        boolean rifle = rk1 != null && GunParts.fits(GunSlot.GRIP, m4, rk1), pistol = rk1 != null && GunParts.fits(GunSlot.GRIP, glock, rk1);
        DevTestResults.check(name(), "items: RK-1 fits a rifle, not a pistol", "m4a4 " + rifle + ", glock17 " + pistol, rifle && !pistol,
                             "m4a4 true, glock17 false (category rifle, as the Foregrip)");
        boolean sm4 = specter != null && GunParts.fits(GunSlot.SIGHT, m4, specter);
        boolean sglock = specter != null && GunParts.fits(GunSlot.SIGHT, glock, specter);
        DevTestResults.check(name(), "items: SpecterDR fits a rifle and a pistol", "m4a4 " + sm4 + ", glock17 " + sglock, sm4 && sglock,
                             "both true (category all, as the ACOG)");
        loot("loot: the SpecterDR wherever the 4x is", "deci:4x", "deci:specterdr");
        loot("loot: the RK-1 wherever the Foregrip is", "deci:foregrip", "deci:rk1");
    }

    private void name(String key, String item, String want)
    {
        ItemStack s = stack(item);
        String got = s == null ? "not registered" : s.getDisplayName();
        DevTestResults.check(name(), "items: " + key, got, want.equals(got), want);
    }

    /** Decimation's crate lists holding `like` against those also holding `added` (Deci.lootTable). */
    private void loot(String key, String like, String added)
    {
        Item a = (Item) Item.itemRegistry.getObject(like), b = (Item) Item.itemRegistry.getObject(added);
        int withA = 0, withBoth = 0;
        for (Map.Entry<Integer, List<ItemStack>> e : net.decimation.fixes.Deci.lootTable().values())
        {
            boolean hasA = false, hasB = false;
            for (ItemStack s : e.getValue())
            {
                hasA |= s != null && s.getItem() == a;
                hasB |= s != null && b != null && s.getItem() == b;
            }
            withA += hasA ? 1 : 0;
            withBoth += hasA && hasB ? 1 : 0;
        }
        DevTestResults.check(name(), key, withBoth + " of " + withA + " lists", withA > 0 && withBoth == withA, "every one of them");
    }

    private static ItemStack stack(String name)
    {
        Object i = Item.itemRegistry.getObject(name);
        return i == null ? null : new ItemStack((Item) i);
    }
}
