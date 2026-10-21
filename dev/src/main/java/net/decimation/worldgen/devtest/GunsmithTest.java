package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.fixes.gunsmith.GunSlot;
import net.decimation.fixes.gunsmith.GunsmithAction;
import net.decimation.fixes.gunsmith.GunsmithContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * The Gunsmith screen (docs/superpowers/plans/2026-10-21-gunsmith-screen.md, Task 4: server side). Decimation's attach
 * key (the way its client tick reads it) opens our container for the held gun; list actions (container button ids) fit,
 * swap and take off parts by Decimation's own rules; a full inventory refuses; the gun never leaves its hotbar slot and
 * closing drops nothing; a replaced held stack closes the screen and keeps the parts; death drops exactly what was held.
 * Survival on the test arena, mob spawning off. Inventory: 0 M4A4, 1 free, 2 two 4x, 3 EOTech, 4 AR suppressor,
 * 5 foregrip, 6 M4A4 paint, 7 AK74 paint, 8 tracer powder, 9 AS Val (built in suppressor), 10 AUG (built in scope).
 */
public class GunsmithTest extends DevTestMode
{
    public String name() { return "gunsmith"; }

    private static final String SCREEN = "GunsmithScreen";
    private int step, ticks;
    private volatile Runnable onServer;
    private volatile boolean serverDone = true;
    private final List<String> chat = Collections.synchronizedList(new ArrayList<String>());
    private static Chat listener;
    /** Server readings, written on the server thread by read(). */
    private volatile String held = "", sight = "", barrel = "", skin = "", wantSkin = "";
    private volatile boolean tracerSet;
    private volatile int count4x, countEotech, countM4Paint, countAkPaint, countTracer, heldId, heldIndex, loose, dropped,
                         carried, found = -1;
    private int openHeldId, windowId;
    private static final String[] GUNS = {"m4a4", "glock17", "svd", "r870", "uzi", "minigun", "deagle", "rpg7"};
    private static final int[][] SIZES = {{854, 480}, {1440, 810}};
    private int gun, size, startW, startH;
    private double before;
    private final boolean PERF = Boolean.getBoolean("deciworldgen.autotest.perf"); // read per run: live settings stay set
    private double closedMs, fullMs, emptyMs;
    private int pressedAt;
    private volatile String swapClick = "";
    private volatile boolean swapClean;
    private volatile int countSupp;
    private volatile String logoutCursor, logoutFull;
    private long glass0;
    private Object textBuilds0;
    /** Turning (spec section 9): readings before a drag, the side on middle, the spin of the perf frames. */
    private Object measuresT, textBuildsT, viewChangesT;
    private int midX = -1, midY = -1;
    private float spinYaw;
    private static java.lang.reflect.Method spinView;
    private List<String> sweep;
    private final StringBuilder sweepBad = new StringBuilder();
    private int textures0;
    private volatile int updates0, updates1;
    private static final Clock clock = new Clock();
    private static final GlProbe gl = new GlProbe();
    private static boolean probesOn;

    /** Frame to frame times (RenderTickEvent START), the frame cap off: the frame's whole cost. */
    public static final class Clock
    {
        private final List<Long> frames = new ArrayList<Long>();
        private volatile boolean on;
        /** Run at each measured frame's start (the perf test turns the gun there). */
        volatile Runnable perFrame;
        private long last;
        private int skip;

        void start()
        {
            synchronized (frames)
            {
                frames.clear();
            }
            skip = 20;
            last = 0;
            on = true;
        }

        boolean idle()
        {
            return !on;
        }

        int frames()
        {
            synchronized (frames)
            {
                return on ? frames.size() : 0;
            }
        }

        /** Stops and returns the median frame in ms. */
        double stop()
        {
            on = false;
            synchronized (frames)
            {
                List<Long> f = new ArrayList<Long>(frames);
                Collections.sort(f);
                return f.isEmpty() ? -1 : f.get(f.size() / 2) / 1e6;
            }
        }

        @SubscribeEvent
        public void tick(cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent e)
        {
            if (e.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.START || !on)
            {
                return;
            }
            Runnable r = perFrame;
            if (r != null)
            {
                r.run();
            }
            long now = System.nanoTime();
            if (last != 0 && skip-- <= 0)
            {
                synchronized (frames)
                {
                    frames.add(now - last);
                }
            }
            last = now;
        }
    }

    /** The screen's own draw once more after each frame: GL errors and stack depths before and after it. */
    public static final class GlProbe
    {
        volatile GuiScreen screen;
        int draws, errors, depthChanged;
        String first = "";
        /** The screen's own draw, GPU included (glFinish before and after), per draw. */
        final long[] drawNs = new long[100];

        @SubscribeEvent
        public void tick(cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent e)
        {
            GuiScreen s = screen;
            if (e.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END || s == null || draws >= 100)
            {
                return;
            }
            while (org.lwjgl.opengl.GL11.glGetError() != 0)
            {
                // errors of the frame before: not the screen's
            }
            int[] d0 = depths();
            org.lwjgl.opengl.GL11.glFinish();
            long t0 = System.nanoTime();
            s.drawScreen(s.width / 2, s.height / 2, 0);
            org.lwjgl.opengl.GL11.glFinish();
            drawNs[draws] = System.nanoTime() - t0;
            int[] d1 = depths();
            int err = org.lwjgl.opengl.GL11.glGetError();
            draws++;
            if (err != 0)
            {
                errors++;
                first = first.isEmpty() ? "error " + err : first;
            }
            if (!java.util.Arrays.equals(d0, d1))
            {
                depthChanged++;
                first = first.isEmpty() ? java.util.Arrays.toString(d0) + " -> " + java.util.Arrays.toString(d1) : first;
            }
        }

        static int[] depths()
        {
            return new int[] {org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MODELVIEW_STACK_DEPTH),
                              org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_PROJECTION_STACK_DEPTH),
                              org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_ATTRIB_STACK_DEPTH)};
        }

        String report()
        {
            return draws + " draws, " + errors + " with GL errors, " + depthChanged + " changed a stack" + (first.isEmpty() ? "" : " (" + first + ")");
        }

        /** Median of the screen's own draws in ms. */
        double drawMs()
        {
            long[] d = java.util.Arrays.copyOf(drawNs, Math.max(1, Math.min(draws, drawNs.length)));
            java.util.Arrays.sort(d);
            return d[d.length / 2] / 1e6;
        }
    }

    private static int textures()
    {
        java.util.Map<?, ?> m = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.client.renderer.texture.TextureManager.class, Minecraft.getMinecraft().getTextureManager(),
            "mapTextureObjects", "field_110585_a");
        return m.size();
    }

    private void next100()
    {
        step++;
        ticks = 0;
    }

    private int slotUpdates()
    {
        net.minecraft.inventory.Container c = player().openContainer;
        return c instanceof net.decimation.fixes.gunsmith.GunsmithContainer ? ((net.decimation.fixes.gunsmith.GunsmithContainer) c).slotUpdates : -1;
    }

    /** Catches the corpse a death makes (any inventory entity but a minecart joining on the server). */
    public static final class Joins
    {
        volatile GunsmithTest test;

        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST, receiveCanceled = true)
        public void join(net.minecraftforge.event.entity.EntityJoinWorldEvent e)
        {
            GunsmithTest t = test;
            if (t != null && !e.world.isRemote && e.entity instanceof IInventory && !(e.entity instanceof net.minecraft.entity.item.EntityMinecart))
            {
                t.watched = e.entity;
            }
        }
    }

    private static Joins joins;
    /** The corpse that joined last: Decimation moves a dead player's items into one (DeathHandler), and its chunk may
     *  unload (and save it) within a few ticks of the death, so it is counted the moment it joins. */
    volatile Entity watched;


    public static final class Chat
    {
        volatile GunsmithTest test;

        @SubscribeEvent
        public void chat(ClientChatReceivedEvent e)
        {
            GunsmithTest t = test;
            if (t != null && e.message != null)
            {
                t.chat.add(e.message.getUnformattedText());
            }
        }
    }

    public boolean client(Minecraft mc)
    {
        if (listener == null)
        {
            listener = new Chat();
            MinecraftForge.EVENT_BUS.register(listener);
        }
        listener.test = this;
        if (!probesOn)
        {
            probesOn = true;
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(clock);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(gl);
        }
        if (joins == null)
        {
            joins = new Joins();
            MinecraftForge.EVENT_BUS.register(joins);
        }
        joins.test = this;
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        if (mc.thePlayer == null || !serverDone)
        {
            return true;
        }
        ticks++;
        switch (step)
        {
            case 0: // the arena and the inventory
                startW = mc.displayWidth;
                startH = mc.displayHeight;
                if (PERF)
                {
                    server(() -> { setup(false, 0); fullBag(); });
                    step = 99;
                    ticks = 0;
                    break;
                }
                server(this::setup);
                next();
                break;
            case 99: // perf (Task 6): the same view closed, then open
                if (ticks == 1)
                {
                    mc.thePlayer.inventory.currentItem = 0;
                    DevTestUtil.unlimitedFps(mc);
                    mc.thePlayer.rotationYaw = 0;
                    mc.thePlayer.rotationPitch = 10;
                }
                if (ticks == 40) clock.start();
                if (clock.frames() >= 220)
                {
                    closedMs = clock.stop();
                    pressAttach(mc);
                    next100();
                }
                break;
            case 100:
                if (mc.currentScreen != null && SCREEN.equals(screenName(mc)) && Integer.valueOf(1).equals(hook(mc, "measures"))
                    && clock.idle())
                {
                    textures0 = textures();
                    clock.start();
                }
                if (clock.frames() >= 220)
                {
                    double openMs = clock.stop();
                    int tex = textures();
                    check("perf: frame cost open", String.format("closed %.2f ms, open %.2f ms (median of 200)", closedMs, openMs),
                          openMs - closedMs <= 1.0, "at most 1 ms more");
                    check("perf: no texture loaded while open", (tex - textures0) + " loaded", tex == textures0, "0");
                    check("perf: measured once while idle", hook(mc, "measures"), Integer.valueOf(1).equals(hook(mc, "measures")), "1");
                    // turning each frame (spec section 9): a third median, the gun turned 3 degrees a frame
                    spinView = method(mc.currentScreen, "setView", float[].class);
                    viewChangesT = hook(mc, "viewChanges");
                    spinYaw = 0;
                    clock.perFrame = () -> invoke(spinView, new float[] {spinYaw += 3, 15, 1.3f});
                    clock.start();
                    step = 102;
                    ticks = 0;
                }
                break;
            case 102: // turning each frame, then the GL probe at a turned view
                if (clock.frames() >= 220)
                {
                    double spinMs = clock.stop();
                    clock.perFrame = null;
                    Object vc = hook(mc, "viewChanges");
                    int turned = vc instanceof Integer && viewChangesT instanceof Integer ? (Integer) vc - (Integer) viewChangesT : 0;
                    check("perf: frame cost while turning", String.format("closed %.2f ms, turning %.2f ms (median of 200), %d views", closedMs,
                          spinMs, turned), turned >= 200 && spinMs - closedMs <= 1.0, "at most 1 ms more, the gun turned every frame");
                    hook(mc, "setView", new float[] {60, 20, 1.5f});
                    gl.draws = gl.errors = gl.depthChanged = 0;
                    gl.first = "";
                    gl.screen = mc.currentScreen;
                    server(() -> updates0 = slotUpdates());
                    step = 101;
                    ticks = 0;
                }
                break;
            case 101: // GL around the screen's own draw, and the server while idle
                if (gl.screen != null && gl.draws >= 100 && fullMs == 0)
                {
                    gl.screen = null;
                    check("perf: GL clean", gl.report(), gl.errors == 0 && gl.depthChanged == 0, "no errors, stacks as deep as before");
                    check("perf: GL clean while turned", gl.report() + ", yaw " + hook(mc, "yaw"), gl.errors == 0 && gl.depthChanged == 0
                          && Float.valueOf(60f).equals(hook(mc, "yaw")), "no errors at yaw 60, tilt 20, zoom 1.5");
                    fullMs = gl.drawMs();
                    server(() -> { updates1 = slotUpdates(); emptyBag(); });
                }
                if (fullMs > 0 && emptyMs == 0 && gl.screen == null && ticks > 40)
                {
                    gl.draws = gl.errors = gl.depthChanged = 0;
                    gl.screen = mc.currentScreen; // the same screen with the bag emptied
                    emptyMs = -1;
                }
                if (emptyMs < 0 && gl.screen != null && gl.draws >= 100)
                {
                    gl.screen = null;
                    emptyMs = gl.drawMs();
                    check("perf: a full bag costs the screen nothing", String.format("own draw %.2f ms with a full bag, %.2f ms empty (GPU included)",
                          fullMs, emptyMs), fullMs - emptyMs <= 0.1, "at most 0.1 ms more");
                }
                if (ticks == 120)
                {
                    check("perf: no slot updates while idle", (updates1 - updates0) + " in 100+ ticks", updates1 == updates0, "0");
                    server(() -> found = find(Item.itemRegistry.getObject("deci:4x")));
                }
                if (ticks == 122) act(mc, GunsmithAction.fit(GunSlot.SIGHT, found));
                if (ticks == 140)
                {
                    check("perf: measured again only on a change", hook(mc, "measures"), Integer.valueOf(2).equals(hook(mc, "measures")), "2");
                    clickBox(mc, GunSlot.SIGHT);
                }
                if (ticks == 240)
                {
                    check("perf: a list built once while open", hook(mc, "listBuilds"), Integer.valueOf(1).equals(hook(mc, "listBuilds")), "1");
                    mc.thePlayer.closeScreen();
                    net.decimation.worldgen.DevAutoTest.keepScreen = false;
                    listener.test = null;
                    joins.test = null;
                    return false;
                }
                break;
            case 1: // no gun in hand: nothing opens, a short message (pressed once the player stands: a fresh game's
                    // first run used to press while the player still fell after setup's teleport, docs/production_test.md)
                if (ticks == 1) mc.thePlayer.inventory.currentItem = 1;
                if (pressedAt == 0 && ticks >= 30 && mc.thePlayer.onGround)
                {
                    pressAttach(mc);
                    pressedAt = ticks;
                }
                if (pressedAt > 0 && (said("Hold a gun to modify it") > 0 && ticks - pressedAt >= 20 || ticks - pressedAt >= 60)
                    || ticks > 400)
                {
                    check("no gun: nothing opens", screenName(mc), mc.currentScreen == null, "no screen");
                    check("no gun: message", String.join(" | ", chat), said("Hold a gun to modify it") > 0, "Hold a gun to modify it");
                    mc.thePlayer.closeScreen();
                    next();
                }
                break;
            case 2: // gun in hand: the Gunsmith screen for it
                if (ticks == 1) mc.thePlayer.inventory.currentItem = 0;
                if (ticks == 6) pressAttach(mc);
                if (ticks == 50)
                {
                    check("gun in hand opens the Gunsmith screen", screenName(mc), SCREEN.equals(screenName(mc)), SCREEN);
                    Object slot = field(mc.thePlayer.openContainer, "heldSlot");
                    check("the container holds the selected slot", slot, Integer.valueOf(0).equals(slot), "0");
                    windowId = mc.thePlayer.openContainer.windowId;
                    server(this::read);
                    next();
                }
                break;
            case 3: // fit a 4x from a stack of two
                if (ticks == 1) openHeldId = heldId;
                if (ticks == 2) act(mc, GunsmithAction.fit(GunSlot.SIGHT, 2));
                if (ticks == 14) server(this::read);
                if (ticks == 16)
                {
                    check("fit: the gun has the 4x", sight, "4x".equals(sight), "sightAttach 4x");
                    check("fit: one 4x taken", count4x, count4x == 1, "1 left");
                    next();
                }
                break;
            case 4: // swap to the EOTech: the 4x comes back
                if (ticks == 1) server(() -> found = find(Item.itemRegistry.getObject("deci:eotech558")));
                if (ticks == 3) act(mc, GunsmithAction.fit(GunSlot.SIGHT, found));
                if (ticks == 15) server(this::read);
                if (ticks == 17)
                {
                    check("swap: the gun has the EOTech", sight, "eotech558".equals(sight), "sightAttach eotech558");
                    check("swap: the 4x came back", count4x, count4x == 2, "2");
                    check("swap: the EOTech left the inventory", countEotech, countEotech == 0, "0");
                    next();
                }
                break;
            case 5: // take off
                if (ticks == 1) act(mc, GunsmithAction.takeOff(GunSlot.SIGHT));
                if (ticks == 13) server(this::read);
                if (ticks == 15)
                {
                    check("take off: no sight", sight, "".equals(sight), "sightAttach empty");
                    check("take off: the EOTech is back", countEotech, countEotech == 1, "1");
                    next();
                }
                break;
            case 6: // a full inventory refuses what could not come back
                if (ticks == 1) server(() -> found = find(Item.itemRegistry.getObject("deci:eotech558")));
                if (ticks == 3) act(mc, GunsmithAction.fit(GunSlot.SIGHT, found));
                if (ticks == 15) server(() -> { twoFourX(); fillWithDirt(); });
                if (ticks == 17) act(mc, GunsmithAction.fit(GunSlot.SIGHT, 2)); // the stack of two 4x: the EOTech could not come back
                if (ticks == 27)
                {
                    check("full: the screen says so", hook(mc, "notice"), "Inventory full".equals(hook(mc, "notice")),
                          "Inventory full shown in the screen (the HUD and its chat are hidden)");
                }
                if (ticks == 29) act(mc, GunsmithAction.takeOff(GunSlot.SIGHT));
                if (ticks == 33) clickBox(mc, GunSlot.SIGHT);
                if (ticks == 37)
                {
                    List<String> l = lines(mc);
                    check("full: no Take off offered", String.join(" | ", l), l.size() > 1 && "Inventory full".equals(l.get(1)),
                          "Inventory full in Take off's place");
                    clickAt(mc, 2, 2);
                }
                if (ticks == 41) server(this::read);
                if (ticks == 43)
                {
                    check("full: nothing changed", sight, "eotech558".equals(sight), "sightAttach still eotech558");
                    check("full: told twice", said("Inventory full"), said("Inventory full") >= 2, "Inventory full x2");
                    check("full: the 4x stack kept", count4x, count4x == 2, "2");
                    server(this::clearDirt);
                    next();
                }
                break;
            case 7: // rules: built in suppressor and scope, paint of another gun
                if (ticks == 1) mc.thePlayer.closeScreen();
                if (ticks == 2) server(() -> put(1, "deci:asval"));
                if (ticks == 4) mc.thePlayer.inventory.currentItem = 1;
                if (ticks == 8) pressAttach(mc);
                if (ticks == 40) act(mc, GunsmithAction.fit(GunSlot.BARREL, 4));
                if (ticks == 52) server(this::read);
                if (ticks == 54)
                {
                    check("rules: no suppressor on the AS Val", held + " barrel '" + barrel + "'", "".equals(barrel), "refused");
                    mc.thePlayer.closeScreen();
                    server(() -> put(1, "deci:aug1"));
                }
                if (ticks == 60) pressAttach(mc);
                if (ticks == 92) act(mc, GunsmithAction.fit(GunSlot.SIGHT, 2));
                if (ticks == 104) server(this::read);
                if (ticks == 106)
                {
                    check("rules: no scope on the AUG", held + " sight '" + sight + "'", "".equals(sight), "refused");
                    mc.thePlayer.closeScreen();
                    mc.thePlayer.inventory.currentItem = 0;
                }
                if (ticks == 112) pressAttach(mc);
                if (ticks == 144) act(mc, GunsmithAction.fit(GunSlot.PAINT, 7));
                if (ticks == 156) server(this::read);
                if (ticks == 158)
                {
                    check("rules: no AK74 paint on the M4A4", "skin '" + skin + "', AK74 paint " + countAkPaint,
                          !skin.equals(wantSkin) && countAkPaint == 1, "refused, kept");
                    next();
                }
                break;
            case 8: // paint and tracer are used up
                if (ticks == 1) act(mc, GunsmithAction.fit(GunSlot.PAINT, 6));
                if (ticks == 2) act(mc, GunsmithAction.fit(GunSlot.TRACER, 8));
                if (ticks == 14) server(this::read);
                if (ticks == 16)
                {
                    check("paint: applied", skin + " (want " + wantSkin + ")", !wantSkin.isEmpty() && wantSkin.equals(skin), "the M4A4 paint's skin");
                    check("paint: used up", countM4Paint, countM4Paint == 0, "0");
                    check("tracer: applied", tracerSet, tracerSet, "tracerColorID set");
                    check("tracer: used up", countTracer, countTracer == 0, "0");
                    check("paint cannot be taken off", GunsmithAction.decode(GunsmithAction.takeOff(GunSlot.PAINT)),
                          GunsmithAction.decode(GunsmithAction.takeOff(GunSlot.PAINT)) == null, "no such action");
                    next();
                }
                break;
            case 9: // drag the suppressor from the hotbar onto the barrel slot
                if (ticks == 1) click(mc, 6 + 4, 0, 0);
                if (ticks == 3) click(mc, GunSlot.BARREL.ordinal(), 0, 0);
                if (ticks == 15) server(this::read);
                if (ticks == 17)
                {
                    check("drag: the gun has the suppressor", barrel, "arSuppressor".equals(barrel), "barrelAttach arSuppressor");
                    next();
                }
                break;
            case 10: // the held gun cannot be picked up or swapped away
                if (ticks == 1) click(mc, 6, 0, 0);
                if (ticks == 3) click(mc, GunSlot.SIGHT.ordinal(), 0, 2);
                if (ticks == 15) server(this::read);
                if (ticks == 17)
                {
                    check("locked: the same gun in hand", held + " id " + heldId, heldId == openHeldId && held.endsWith("m4a4"), "unchanged");
                    check("locked: nothing on the cursor", mc.thePlayer.inventory.getItemStack(), mc.thePlayer.inventory.getItemStack() == null, "empty");
                    next();
                }
                break;
            case 11: // I closes it, nothing dropped, the parts stay
                if (ticks == 1) typeAttach(mc);
                if (ticks == 20) server(this::read);
                if (ticks == 22)
                {
                    check("I closes the screen", screenName(mc), mc.currentScreen == null, "closed");
                    check("closing drops nothing", loose, loose == 0, "0 items on the ground");
                    check("the gun keeps its parts", barrel, "arSuppressor".equals(barrel) && heldIndex == 0, "barrel on, slot 0");
                    next();
                }
                break;
            case 12: // the held stack replaced while open: the screen closes, the copy keeps the parts
                if (ticks == 1) pressAttach(mc);
                if (ticks == 35) server(this::replaceAndClick);
                if (ticks == 45) server(this::read);
                if (ticks == 47)
                {
                    check("replaced: the screen closed", screenName(mc), mc.currentScreen == null, "no screen");
                    check("replaced: the copy keeps its parts", barrel, "arSuppressor".equals(barrel), "barrelAttach arSuppressor");
                    check("replaced: a part slot click in that tick takes nothing", swapClick, swapClean, "nothing taken, cursor empty");
                    mc.thePlayer.closeScreen();
                    next();
                }
                break;
            case 13: // death while open: the drops are what was held
                if (ticks == 1) pressAttach(mc);
                if (ticks == 35) server(this::die);
                if (ticks == 45)
                {
                    check("death: what was held, no more", dropped + " of " + carried, carried > 0 && dropped == carried, carried + " (dropped or kept)");
                    mc.thePlayer.respawnPlayer();
                    mc.displayGuiScreen(null);
                }
                if (ticks == 120)
                {
                    next();
                }
                break;
            case 14: // the screen (Task 5): a bare M4A4, no grip carried
                if (ticks == 1) server(() -> setup(false, 0));
                if (ticks == 3) mc.thePlayer.inventory.currentItem = 0;
                if (ticks == 8) pressAttach(mc);
                if (ticks == 50)
                {
                    check("screen: measured", hook(mc, "measures"), Integer.valueOf(1).equals(hook(mc, "measures")), "1");
                    Object det = hook(mc, "viewDeterminant");
                    check("screen: the gun not drawn inside out", det, det instanceof Float && (Float) det < 0,
                          "negative, as the world draws (the GUI view flips y: hidden parts showed through, e.g. the Uzi's magazine in its grip)");
                    before = relMag(mc);
                    server(() -> found = find(Item.itemRegistry.getObject("deci:arSuppressor")));
                }
                if (ticks == 52) act(mc, GunsmithAction.fit(GunSlot.BARREL, found));
                if (ticks == 75)
                {
                    double after = relMag(mc);
                    check("screen: muzzle to the right", String.format("magazine at %.2f then %.2f of the length", before, after),
                          after < before - 0.02, "the suppressor lengthens the right side");
                    check("screen: measured again on a change", hook(mc, "measures"), Integer.valueOf(2).equals(hook(mc, "measures")), "2");
                    shot(mc, "gunsmith_m4a4_suppressor");
                    next();
                }
                break;
            case 15: // a click like a mouse opens the Sight list; its 4x row fits the 4x
                if (ticks == 1) clickBox(mc, GunSlot.SIGHT);
                if (ticks == 5)
                {
                    List<String> lines = lines(mc);
                    String fourX = name("deci:4x"), red = name("deci:eotech558");
                    check("list: opens on its slot", hook(mc, "openList"), GunSlot.SIGHT.equals(hook(mc, "openList")), "SIGHT");
                    check("list: title", lines.isEmpty() ? "none" : lines.get(0), !lines.isEmpty() && "Sights you carry".equals(lines.get(0)), "Sights you carry");
                    check("list: what fits", String.join(" | ", lines), lines.contains(fourX) && lines.contains(red), fourX + " and " + red);
                    clickRow(mc, lines.indexOf(fourX) - 1);
                }
                if (ticks == 20) server(this::read);
                if (ticks == 22)
                {
                    check("list: the row fits it", sight, "4x".equals(sight), "sightAttach 4x");
                    check("list: closes after a pick", hook(mc, "openList"), hook(mc, "openList") == null, "closed");
                    next();
                }
                break;
            case 16: // Take off, none carried, built in
                if (ticks == 1) clickBox(mc, GunSlot.SIGHT);
                if (ticks == 5)
                {
                    List<String> lines = lines(mc);
                    check("list: Take off first", String.join(" | ", lines), lines.size() > 1 && "Take off".equals(lines.get(1)), "Take off");
                    clickRow(mc, 0);
                }
                if (ticks == 18) server(this::read);
                if (ticks == 20)
                {
                    check("list: Take off works", sight, "".equals(sight), "sightAttach empty");
                    clickBox(mc, GunSlot.GRIP);
                }
                if (ticks == 24)
                {
                    check("list: nothing carried", String.join(" | ", lines(mc)), lines(mc).contains("None in your inventory"), "None in your inventory");
                    clickAt(mc, 2, 2); // outside: closes the list
                }
                if (ticks == 28)
                {
                    check("list: a click outside closes it", hook(mc, "openList"), hook(mc, "openList") == null, "closed");
                    mc.thePlayer.closeScreen();
                    server(() -> put(1, "deci:aug1"));
                }
                if (ticks == 32) mc.thePlayer.inventory.currentItem = 1;
                if (ticks == 36) pressAttach(mc);
                if (ticks == 75)
                {
                    check("built in: the AUG's sight", hook(mc, "boxLabel", GunSlot.SIGHT), "Built in".equals(hook(mc, "boxLabel", GunSlot.SIGHT)), "Built in");
                    clickBox(mc, GunSlot.SIGHT);
                }
                if (ticks == 79)
                {
                    check("built in: no list", hook(mc, "openList"), hook(mc, "openList") == null, "no list");
                    mc.thePlayer.closeScreen();
                    mc.thePlayer.inventory.currentItem = 0;
                }
                if (ticks == 84) pressAttach(mc);
                if (ticks == 120) next();
                break;
            case 17: // drag from the hotbar row onto the Barrel box: take the suppressor off, then drag it back
                if (ticks == 1) act(mc, GunsmithAction.takeOff(GunSlot.BARREL));
                if (ticks == 12) server(() -> found = find(Item.itemRegistry.getObject("deci:arSuppressor")));
                if (ticks == 14)
                {
                    int[] slot = hotbarSlot(mc, found);
                    if (slot != null) clickAt(mc, slot[0] + 8, slot[1] + 8);
                }
                if (ticks == 17)
                {
                    int[] b = (int[]) hook(mc, "box", GunSlot.BARREL);
                    if (b != null) clickWithMove(mc, b[0] + 12, b[1] + 12); // a hand moves while it clicks
                }
                if (ticks == 30) server(this::read);
                if (ticks == 32)
                {
                    check("drag: hotbar row to the Barrel box", barrel, "arSuppressor".equals(barrel), "barrelAttach arSuppressor");
                    act(mc, GunsmithAction.takeOff(GunSlot.BARREL));
                }
                if (ticks == 44) server(() -> found = find(Item.itemRegistry.getObject("deci:arSuppressor")));
                if (ticks == 46)
                {
                    int[] slot = hotbarSlot(mc, found);
                    if (slot != null) clickAt(mc, slot[0] + 8, slot[1] + 8); // the part on the cursor
                }
                if (ticks == 49) typeAttach(mc);
                if (ticks == 62) server(this::read);
                if (ticks == 64)
                {
                    check("closing with a part on the cursor puts it back", "on the ground " + loose + ", in the bag " + countSupp,
                          loose == 0 && countSupp == 1, "0 on the ground, 1 in the bag");
                    mc.thePlayer.closeScreen();
                }
                if (ticks == 66) pressAttach(mc);
                if (ticks == 100) server(this::logoutWithPartOnCursor);
                if (ticks == 102)
                {
                    check("a disconnect with a part on the cursor keeps it", logoutCursor,
                          "picked up, then cursor empty, 1 in the bag".equals(logoutCursor), "picked up, then cursor empty, 1 in the bag (saved)");
                    check("a disconnect with a full bag drops the cursor's part", logoutFull,
                          "picked up, then cursor empty, 1 on the ground".equals(logoutFull), "picked up, then cursor empty, 1 on the ground");
                    mc.thePlayer.closeScreen();
                    gun = 0;
                    size = 0;
                    next();
                }
                break;
            case 18: // every kind of gun at two window sizes: on the gun, on screen, apart
                if (ticks == 1)
                {
                    if (gun == 0) resize(mc, SIZES[size]);
                    final String g = GUNS[gun];
                    server(() -> put(0, "deci:" + g));
                }
                if (ticks == 5) mc.thePlayer.inventory.currentItem = 0;
                if (ticks == 10) pressAttach(mc);
                if (ticks == 55)
                {
                    layoutChecks(mc, GUNS[gun] + " " + SIZES[size][0]);
                    shot(mc, "gunsmith_" + GUNS[gun] + "_" + SIZES[size][0]);
                    if ("rpg7".equals(GUNS[gun]))
                    {
                        clickBox(mc, GunSlot.BARREL); // sights of Decimation's category "all" fit a launcher too
                    }
                }
                if (ticks == 59 && "rpg7".equals(GUNS[gun]))
                {
                    check("rpg7 " + SIZES[size][0] + ": no barrel part fits", String.join(" | ", lines(mc)), lines(mc).contains("None in your inventory"), "None in your inventory");
                }
                if (ticks == 62)
                {
                    mc.thePlayer.closeScreen();
                    if (++gun == GUNS.length)
                    {
                        gun = 0;
                        size++;
                    }
                    ticks = 0;
                    if (size == SIZES.length)
                    {
                        resize(mc, new int[] {startW, startH});
                        next();
                    }
                }
                break;
            case 19: // opened while aiming (no scope glass work), measuring cost, nothing built per frame with a list open
                if (ticks == 1) server(() -> { put(0, "deci:m4a4"); put(2, "deci:4x"); });
                if (ticks == 4) mc.thePlayer.inventory.currentItem = 0;
                if (ticks == 8) pressAttach(mc);
                if (ticks == 40) act(mc, GunsmithAction.fit(GunSlot.SIGHT, 2));
                if (ticks == 52) mc.thePlayer.closeScreen();
                if (ticks == 54) net.decimation.fixes.Deci.setAimMode(mc.thePlayer, 1);
                if (ticks == 58) pressAttach(mc);
                if (ticks == 90)
                {
                    glass0 = net.decimation.fixes.Deci.scopeGlassTime();
                    Object ms = hook(mc, "measureMs"), px = hook(mc, "pixelsRead"), area = hook(mc, "areaPixels");
                    check("measuring: under a frame", ms, ms instanceof Float && (Float) ms <= 8f, "at most 8 ms");
                    check("measuring: reads the gun area only", px + " pixels of " + area, px instanceof Integer && area instanceof Integer
                          && (Integer) px <= 2.5 * (Integer) area, "at most 2.5 times the area (two side views, half the area from above)");
                    clickBox(mc, GunSlot.SIGHT);
                }
                if (ticks == 94) textBuilds0 = hook(mc, "textBuilds");
                if (ticks == 150)
                {
                    check("opened while aiming: no scope glass work", net.decimation.fixes.Deci.scopeGlassTime() == glass0 ? "none" : "measured",
                          net.decimation.fixes.Deci.scopeGlassTime() == glass0, "none (spec 5)");
                    Object tb = hook(mc, "textBuilds");
                    check("a list open: no text built per frame", textBuilds0 + " then " + tb, tb != null && tb.equals(textBuilds0),
                          "unchanged over 56 ticks");
                    net.decimation.fixes.Deci.setAimMode(mc.thePlayer, 0);
                    mc.thePlayer.closeScreen();
                    sweep = allGuns();
                    gun = 0;
                    next();
                }
                break;
            case 20: // turning the gun (spec section 9)
                turning(mc);
                break;
            case 21: // every registered gun at 854 x 480: lines on the gun, boxes on screen and apart
                if (ticks == 1)
                {
                    final String g = sweep.get(gun);
                    server(() -> put(0, g));
                }
                if (ticks == 4) mc.thePlayer.inventory.currentItem = 0;
                if (ticks == 6) pressAttach(mc);
                if (ticks >= 20 && mc.currentScreen != null && hook(mc, "gunBounds") != null || ticks > 120)
                {
                    String bad = layoutProblems(mc);
                    if (!bad.isEmpty()) sweepBad.append(sweep.get(gun)).append(": ").append(bad).append("; ");
                    mc.thePlayer.closeScreen();
                    ticks = 0;
                    if (++gun == sweep.size())
                    {
                        check("every gun lays out", sweep.size() + " guns" + (sweepBad.length() == 0 ? "" : ", " + sweepBad), sweepBad.length() == 0,
                              "every gun's lines on it, boxes on screen and apart");
                        net.decimation.worldgen.DevAutoTest.keepScreen = false;
                        listener.test = null;
                        joins.test = null;
                        return false;
                    }
                }
                break;
            default:
                return false;
        }
        return true;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            try
            {
                r.run();
            }
            catch (RuntimeException e)
            {
                DevTestResults.value(name(), "server error", e.toString());
            }
            serverDone = true;
        }
    }

    // ---- client side

    private void next()
    {
        step++;
        ticks = 0;
    }

    private void server(Runnable r)
    {
        serverDone = false;
        onServer = r;
    }

    private void check(String what, Object value, boolean pass, String expected)
    {
        DevTestResults.check(name(), what, value, pass, expected);
    }

    private int said(String line)
    {
        int n = 0;
        synchronized (chat)
        {
            for (String s : chat)
            {
                n += s.contains(line) ? 1 : 0;
            }
        }
        return n;
    }

    static String screenName(Minecraft mc)
    {
        return mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName();
    }

    static KeyBinding attachKey(Minecraft mc)
    {
        for (KeyBinding k : mc.gameSettings.keyBindings)
        {
            if ("key.decimation.gui.attach".equals(k.getKeyDescription()))
            {
                return k;
            }
        }
        throw new IllegalStateException("no key.decimation.gui.attach");
    }

    /** A press of Decimation's attach key the way its client tick reads it (KeyBinding.isPressed). */
    static void pressAttach(Minecraft mc)
    {
        KeyBinding.onTick(attachKey(mc).getKeyCode());
    }

    /** The attach key typed into the open screen, the way a key reaches a GUI (GuiScreen.keyTyped). */
    static void typeAttach(Minecraft mc)
    {
        GuiScreen s = mc.currentScreen;
        int code = attachKey(mc).getKeyCode();
        for (Class<?> c = s == null ? null : s.getClass(); c != null; c = c.getSuperclass())
        {
            for (String n : new String[] {"keyTyped", "func_73869_a"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(n, char.class, int.class);
                    m.setAccessible(true);
                    m.invoke(s, 'i', code);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /** A list click as the screen sends it: the container's button id. */
    private void act(Minecraft mc, int id)
    {
        mc.playerController.sendEnchantPacket(mc.thePlayer.openContainer.windowId, id);
    }

    /** A slot click as the screen sends it (mode 0 pick up / put, 2 hotbar swap with button as the hotbar index). */
    private void click(Minecraft mc, int slot, int button, int mode)
    {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, button, mode, mc.thePlayer);
    }

    private static Object field(Object o, String name)
    {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
        {
            try
            {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            }
            catch (NoSuchFieldException e)
            {
                // the parent class
            }
            catch (IllegalAccessException e)
            {
                return null;
            }
        }
        return null;
    }

    /** A layout hook of the open Gunsmith screen (reflection: the test also runs against older screens), or null. */
    static Object hook(Minecraft mc, String name, Object... args)
    {
        GuiScreen s = mc.currentScreen;
        if (s == null)
        {
            return null;
        }
        try
        {
            if (args.length == 0)
            {
                try
                {
                    return s.getClass().getField(name).get(s);
                }
                catch (NoSuchFieldException e)
                {
                    return s.getClass().getMethod(name).invoke(s);
                }
            }
            Class<?> type = args[0] instanceof Integer ? int.class : args[0].getClass();
            return s.getClass().getMethod(name, type).invoke(s, args[0]);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static List<String> lines(Minecraft mc)
    {
        Object o = hook(mc, "listLines");
        return o instanceof List ? (List<String>) o : Collections.<String>emptyList();
    }

    /** Where the magazine sits along the gun, 0 at the left end, 1 at the right. */
    private static double relMag(Minecraft mc)
    {
        int[] b = (int[]) hook(mc, "gunBounds"), p = (int[]) hook(mc, "point", GunSlot.TRACER);
        return b == null || p == null || b[2] == 0 ? -1 : (p[0] - b[0]) / (double) b[2];
    }

    /**
     * A click the way a mouse makes it: the screen drawn with the mouse over the spot, the press, then the release
     * (GuiContainer places an item from the cursor on the release: a press alone starts a drag).
     */
    static void clickAt(Minecraft mc, int x, int y)
    {
        GuiScreen s = mc.currentScreen;
        if (s != null)
        {
            s.drawScreen(x, y, 0);
            DevTestUtil.clickAt(s, x, y);
            release(s, x, y);
        }
    }

    /** A press, a one unit move with the button held, then the release: how a hand really clicks. */
    static void clickWithMove(Minecraft mc, int x, int y)
    {
        GuiScreen s = mc.currentScreen;
        if (s == null)
        {
            return;
        }
        s.drawScreen(x, y, 0);
        DevTestUtil.clickAt(s, x, y);
        move(s, x + 1, y, 80L);
        release(s, x + 1, y);
    }

    /** A mouse drag: the press at (x0, y0), the moves in steps with the button held, the release at (x1, y1). */
    static void drag(Minecraft mc, int x0, int y0, int x1, int y1, int steps)
    {
        GuiScreen s = mc.currentScreen;
        if (s == null)
        {
            return;
        }
        s.drawScreen(x0, y0, 0);
        DevTestUtil.clickAt(s, x0, y0);
        for (int i = 1; i <= steps; i++)
        {
            move(s, x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps, 50L * i);
        }
        release(s, x1, y1);
    }

    /** GuiScreen.mouseClickMove: the mouse at (x, y) with the left button held for `held` ms. */
    private static void move(GuiScreen s, int x, int y, long held)
    {
        for (Class<?> c = s.getClass(); c != null; c = c.getSuperclass())
        {
            for (String n : new String[] {"mouseClickMove", "func_146273_a"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(n, int.class, int.class, int.class, long.class);
                    m.setAccessible(true);
                    m.invoke(s, x, y, 0, held);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static void release(GuiScreen s, int x, int y)
    {
        for (Class<?> c = s.getClass(); c != null; c = c.getSuperclass())
        {
            for (String n : new String[] {"mouseMovedOrUp", "func_146286_b"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(n, int.class, int.class, int.class);
                    m.setAccessible(true);
                    m.invoke(s, x, y, 0);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    static void clickBox(Minecraft mc, GunSlot slot)
    {
        int[] b = (int[]) hook(mc, "box", slot);
        if (b != null)
        {
            clickAt(mc, b[0] + 12, b[1] + 12);
        }
    }

    static void clickRow(Minecraft mc, int row)
    {
        int[] r = (int[]) hook(mc, "listRow", row);
        if (r != null && row >= 0)
        {
            clickAt(mc, r[0] + 30, r[1] + 8);
        }
    }

    /** The screen position of the hotbar slot holding inventory index i (container slot 6 + i), or null. */
    private static int[] hotbarSlot(Minecraft mc, int i)
    {
        if (i < 0 || i > 8 || mc.thePlayer.openContainer.inventorySlots.size() <= 6 + i)
        {
            return null;
        }
        net.minecraft.inventory.Slot s = (net.minecraft.inventory.Slot) mc.thePlayer.openContainer.inventorySlots.get(6 + i);
        return new int[] {s.xDisplayPosition, s.yDisplayPosition};
    }

    static String name(String item)
    {
        Object i = Item.itemRegistry.getObject(item);
        return i == null ? "?" : new ItemStack((Item) i).getDisplayName();
    }

    private void shot(Minecraft mc, String file)
    {
        DevTestUtil.screenshot(mc, name(), file + ".png");
    }

    private void resize(Minecraft mc, int[] wh)
    {
        try
        {
            org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(wh[0], wh[1]));
            java.lang.reflect.Method r = cpw.mods.fml.relauncher.ReflectionHelper.findMethod(
                Minecraft.class, mc, new String[] {"resize", "func_71370_a"}, int.class, int.class);
            r.invoke(mc, wh[0], wh[1]);
        }
        catch (Exception e)
        {
            DevTestResults.value(name(), "resize failed", e.toString());
        }
    }

    /** Every point on the gun, every box on screen and apart (GunsmithLayout's rules). */
    private void layoutChecks(Minecraft mc, String what)
    {
        GuiScreen s = mc.currentScreen;
        int[] b = (int[]) hook(mc, "gunBounds");
        if (s == null || b == null)
        {
            check(what + ": screen", screenName(mc), false, "the Gunsmith screen");
            return;
        }
        StringBuilder off = new StringBuilder();
        for (GunSlot k : GunSlot.values())
        {
            int[] p = (int[]) hook(mc, "point", k);
            if (p == null || p[0] < b[0] - 1 || p[0] > b[0] + b[2] + 1 || p[1] < b[1] - 1 || p[1] > b[1] + b[3] + 1)
            {
                off.append(k).append(' ');
            }
        }
        check(what + ": every line ends on the gun", off.length() == 0 ? "all" : off.toString().trim(), off.length() == 0, "all six");
        int B = net.decimation.fixes.gunsmith.GunsmithLayout.BOX;
        StringBuilder bad = new StringBuilder();
        int[][] boxes = new int[GunSlot.values().length][];
        for (GunSlot k : GunSlot.values())
        {
            int[] x = (int[]) hook(mc, "box", k);
            boxes[k.ordinal()] = x;
            if (x == null || x[0] < 4 || x[0] + B > s.width - 4 || x[1] < net.decimation.fixes.gunsmith.GunsmithLayout.TOP
                || x[1] + B + 10 > s.height - net.decimation.fixes.gunsmith.GunsmithLayout.BOTTOM)
            {
                bad.append(k).append(" off screen; ");
            }
        }
        for (int i = 0; i < boxes.length; i++)
        {
            for (int j = i + 1; j < boxes.length; j++)
            {
                if (boxes[i] != null && boxes[j] != null && Math.abs(boxes[i][0] - boxes[j][0]) < B + 4
                    && Math.abs(boxes[i][1] - boxes[j][1]) < B + 14)
                {
                    bad.append(GunSlot.values()[i]).append('/').append(GunSlot.values()[j]).append(" overlap; ");
                }
            }
        }
        check(what + ": boxes on screen and apart", bad.length() == 0 ? "ok" : bad.toString(), bad.length() == 0, "ok");
    }

    // ---- server side (run on the server thread through server(Runnable))

    private static EntityPlayerMP player()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        return w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
    }

    private static Item byName(String prefix)
    {
        Item best = null;
        for (Object o : Item.itemRegistry)
        {
            Item i = (Item) o;
            String n = i.getUnlocalizedName();
            if (n != null && n.startsWith(prefix) && (best == null || n.contains("Orange")))
            {
                best = i;
            }
        }
        return best;
    }

    private static ItemStack stack(String name, int n)
    {
        Object i = Item.itemRegistry.getObject(name);
        return i == null ? null : new ItemStack((Item) i, n);
    }

    private void setup()
    {
        setup(true, 1);
    }

    /** The arena and the inventory; held is the hotbar index the client holds (it only reports a change). */
    private void setup(boolean grip, int held)
    {
        EntityPlayerMP p = player();
        WorldServer w = (WorldServer) p.worldObj;
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        DevTestArena.build(w, p);
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            p.inventory.mainInventory[i] = null;
        }
        Item m4Paint = byName("item.spraycan.m4a4."), akPaint = byName("item.spraycan.ak74."), tracer = byName("item.ItemPyroPowder");
        wantSkin = m4Paint == null ? "" : m4Paint.getUnlocalizedName().substring("item.spraycan.m4a4.".length());
        p.inventory.mainInventory[0] = stack("deci:m4a4", 1);
        p.inventory.mainInventory[2] = stack("deci:4x", 2);
        p.inventory.mainInventory[3] = stack("deci:eotech558", 1);
        p.inventory.mainInventory[4] = stack("deci:arSuppressor", 1);
        p.inventory.mainInventory[5] = grip ? stack("deci:foregrip", 1) : null;
        p.inventory.mainInventory[6] = m4Paint == null ? null : new ItemStack(m4Paint);
        p.inventory.mainInventory[7] = akPaint == null ? null : new ItemStack(akPaint);
        p.inventory.mainInventory[8] = tracer == null ? null : new ItemStack(tracer);
        p.inventory.mainInventory[9] = stack("deci:asval", 1);
        p.inventory.mainInventory[10] = stack("deci:aug1", 1);
        p.inventory.currentItem = held;
        p.inventoryContainer.detectAndSendChanges();
        DevTestResults.value(name(), "paints and tracer", m4Paint + ", " + akPaint + ", " + tracer + ", skin " + wantSkin);
    }

    private static String nbt(ItemStack s, String key)
    {
        return s != null && s.hasTagCompound() && s.getTagCompound().hasKey(key) ? s.getTagCompound().getString(key) : "";
    }

    private int countOf(EntityPlayerMP p, Item item)
    {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            n += s != null && item != null && s.getItem() == item ? s.stackSize : 0;
        }
        return n;
    }

    private int find(Object item)
    {
        EntityPlayerMP p = player();
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            ItemStack s = p.inventory.mainInventory[i];
            if (s != null && s.getItem() == item)
            {
                return i;
            }
        }
        return -1;
    }

    private void read()
    {
        EntityPlayerMP p = player();
        ItemStack h = p.inventory.getCurrentItem();
        held = h == null ? "nothing" : String.valueOf(Item.itemRegistry.getNameForObject(h.getItem()));
        heldId = System.identityHashCode(h);
        heldIndex = p.inventory.currentItem;
        sight = nbt(h, GunSlot.SIGHT.nbtKey);
        barrel = nbt(h, GunSlot.BARREL.nbtKey);
        skin = nbt(h, GunSlot.PAINT.nbtKey);
        tracerSet = h != null && h.hasTagCompound() && h.getTagCompound().hasKey(GunSlot.TRACER.nbtKey);
        count4x = countOf(p, (Item) Item.itemRegistry.getObject("deci:4x"));
        countEotech = countOf(p, (Item) Item.itemRegistry.getObject("deci:eotech558"));
        countM4Paint = countOf(p, byName("item.spraycan.m4a4."));
        countAkPaint = countOf(p, byName("item.spraycan.ak74."));
        countTracer = countOf(p, byName("item.ItemPyroPowder"));
        countSupp = countOf(p, (Item) Item.itemRegistry.getObject("deci:arSuppressor"));
        loose = p.worldObj.getEntitiesWithinAABB(EntityItem.class, p.boundingBox.expand(4, 4, 4)).size();
    }

    /** A full bag, as a player carries one: every free main inventory slot holds something (blocks and items). */
    private void fullBag()
    {
        EntityPlayerMP p = player();
        for (int i = 9; i < p.inventory.mainInventory.length; i++)
        {
            if (p.inventory.mainInventory[i] == null)
            {
                p.inventory.mainInventory[i] = i % 2 == 0 ? new ItemStack(Blocks.dirt, 16) : new ItemStack(net.minecraft.init.Items.apple, 8);
            }
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    private void emptyBag()
    {
        EntityPlayerMP p = player();
        for (int i = 11; i < p.inventory.mainInventory.length; i++)
        {
            p.inventory.mainInventory[i] = null;
        }
        p.openContainer.detectAndSendChanges();
    }

    /** Exactly one stack of two 4x, at index 2 (attachments do not stack on their own). */
    private void twoFourX()
    {
        EntityPlayerMP p = player();
        Object fourX = Item.itemRegistry.getObject("deci:4x");
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            ItemStack s = p.inventory.mainInventory[i];
            if (s != null && s.getItem() == fourX)
            {
                p.inventory.mainInventory[i] = null;
            }
        }
        p.inventory.mainInventory[2] = stack("deci:4x", 2);
    }

    private void fillWithDirt()
    {
        EntityPlayerMP p = player();
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            if (p.inventory.mainInventory[i] == null)
            {
                p.inventory.mainInventory[i] = new ItemStack(Blocks.dirt, 64);
            }
        }
        p.openContainer.detectAndSendChanges();
    }

    private void clearDirt()
    {
        EntityPlayerMP p = player();
        Item dirt = Item.getItemFromBlock(Blocks.dirt);
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            ItemStack s = p.inventory.mainInventory[i];
            if (s != null && s.getItem() == dirt)
            {
                p.inventory.mainInventory[i] = null;
            }
        }
        p.openContainer.detectAndSendChanges();
    }

    private void put(int index, String name)
    {
        EntityPlayerMP p = player();
        p.inventory.mainInventory[index] = stack(name, 1);
        p.openContainer.detectAndSendChanges();
    }

    /** The held stack swapped (as a right click does) and, in the same tick, a click on a part slot (a modified client). */
    /**
     * Turning the gun (spec section 9): a drag on empty space, staying turned, the line ends on the gun at several views
     * (against a real render of each view), the middle, plain clicks, the double click, the tilt limit, the zoom, no per
     * frame work, a fit keeping the view.
     */
    private void turning(Minecraft mc)
    {
        int w = mc.currentScreen == null ? 0 : mc.currentScreen.width, top = 20; // above every box (TOP 30): empty space
        if (ticks == 1) mc.thePlayer.inventory.currentItem = 0;
        if (ticks == 4) pressAttach(mc);
        if (ticks == 40)
        {
            int[] b = (int[]) hook(mc, "gunBounds");
            midX = b == null ? -1 : b[0] + b[2] / 2;
            midY = b == null ? -1 : b[1] + b[3] / 2;
            drag(mc, w / 2, top, w / 2 + 90, top, 9);
        }
        if (ticks == 42)
        {
            Object yaw = hook(mc, "yaw");
            int[] e = (int[]) hook(mc, "lineEnd", GunSlot.BARREL);
            check("turn: a drag on empty space turns the gun", yaw, Float.valueOf(90f).equals(yaw), "yaw 90 after 90 GUI pixels to the right");
            check("turn: the muzzle's line follows it", e == null ? "none" : e[0] + " (gun middle " + midX + ")", e != null && Math.abs(e[0] - midX) <= 12,
                  "the muzzle end near the gun's middle, seen end on");
        }
        if (ticks == 50)
        {
            check("turn: stays turned after letting go", hook(mc, "yaw"), Float.valueOf(90f).equals(hook(mc, "yaw")), "90, eight ticks later");
            hook(mc, "checkLinesNextFrame");
        }
        if (ticks == 53)
        {
            shot(mc, "gunsmith_turned_90");
            checkLines(mc, "turned 90");
            Object off = hook(mc, "pivotOff");
            check("turned 90: turns about its middle", off, off instanceof Float && (Float) off >= 0 && (Float) off <= 4,
                  "the drawn gun's middle within 4 GUI pixels of the side view's");
        }
        float[][] views = {{30, 0, 1}, {150, 20, 1.5f}, {-60, -30, 2.5f}};
        for (int i = 0; i < views.length; i++)
        {
            int t = 55 + i * 6;
            if (ticks == t) hook(mc, "setView", views[i]);
            if (ticks == t + 1) hook(mc, "checkLinesNextFrame");
            if (ticks == t + 4)
            {
                checkLines(mc, String.format("turned %.0f, tilted %.0f, zoom %.1f", views[i][0], views[i][1], views[i][2]));
                shot(mc, String.format("gunsmith_turned_%.0f_%.0f", views[i][0], views[i][1]));
            }
        }
        if (ticks == 75) clickAt(mc, w / 2, top);
        if (ticks == 76)
        {
            check("turn: a plain click on empty space does not turn", hook(mc, "yaw"), Float.valueOf(-60f).equals(hook(mc, "yaw")), "still -60");
        }
        if (ticks == 90)
        {
            clickAt(mc, w / 2, top);
            clickAt(mc, w / 2, top);
        }
        if (ticks == 91)
        {
            String v = hook(mc, "yaw") + ", " + hook(mc, "pitch") + ", " + hook(mc, "zoom");
            check("turn: a double click resets the view", v, "0.0, 0.0, 1.0".equals(v), "yaw 0, tilt 0, zoom 1");
        }
        if (ticks == 94) drag(mc, w / 2, top, w / 2, top + 300, 10);
        if (ticks == 96) check("turn: the tilt stops at 85", hook(mc, "pitch"), Float.valueOf(85f).equals(hook(mc, "pitch")), "85");
        if (ticks == 108)
        {
            clickAt(mc, w / 2, top);
            clickAt(mc, w / 2, top);
        }
        if (ticks == 110) clickBox(mc, GunSlot.SIGHT);
        if (ticks == 112)
        {
            check("turn: a click on a box opens its list and does not turn", hook(mc, "openList") + ", yaw " + hook(mc, "yaw"),
                  GunSlot.SIGHT == hook(mc, "openList") && Float.valueOf(0f).equals(hook(mc, "yaw")), "SIGHT list, yaw 0");
            hook(mc, "wheel", 120);
            hook(mc, "wheel", 120);
        }
        if (ticks == 113) check("zoom: an open list keeps the zoom", hook(mc, "zoom"), Float.valueOf(1f).equals(hook(mc, "zoom")), "1 (the wheel scrolls the list)");
        if (ticks == 114) clickAt(mc, w / 2, top); // outside the list: it closes
        if (ticks == 116)
        {
            for (int i = 0; i < 3; i++) hook(mc, "wheel", 120);
        }
        if (ticks == 117)
        {
            Object z = hook(mc, "zoom");
            check("zoom: the wheel zooms", z, z instanceof Float && Math.abs((Float) z - 1.331f) < 0.01f, "1.33 after three notches");
            for (int i = 0; i < 30; i++) hook(mc, "wheel", 120);
        }
        if (ticks == 118)
        {
            check("zoom: stops at 2.5", hook(mc, "zoom"), Float.valueOf(2.5f).equals(hook(mc, "zoom")), "2.5");
            for (int i = 0; i < 40; i++) hook(mc, "wheel", -120);
        }
        if (ticks == 119) check("zoom: stops at 1", hook(mc, "zoom"), Float.valueOf(1f).equals(hook(mc, "zoom")), "1");
        if (ticks == 120)
        {
            measuresT = hook(mc, "measures");
            textBuildsT = hook(mc, "textBuilds");
            viewChangesT = hook(mc, "viewChanges");
        }
        if (ticks > 120 && ticks <= 140) hook(mc, "setView", new float[] {(ticks - 120) * 9, 10, 1.2f});
        if (ticks == 142)
        {
            Object m = hook(mc, "measures"), tb = hook(mc, "textBuilds"), vc = hook(mc, "viewChanges");
            int turned = vc instanceof Integer && viewChangesT instanceof Integer ? (Integer) vc - (Integer) viewChangesT : 0;
            check("turning: no depth read, no text built", "measured " + measuresT + " then " + m + ", texts " + textBuildsT + " then " + tb + ", "
                  + turned + " views", turned == 20 && m != null && m.equals(measuresT) && tb != null && tb.equals(textBuildsT),
                  "20 views, measures and texts unchanged");
            hook(mc, "setView", new float[] {45, 10, 1.2f});
            server(() -> { put(3, "deci:eotech558"); found = 3; });
        }
        if (ticks == 146) act(mc, GunsmithAction.fit(GunSlot.SIGHT, found));
        if (ticks == 170)
        {
            String v = hook(mc, "yaw") + ", " + hook(mc, "pitch") + ", " + hook(mc, "zoom");
            Object m = hook(mc, "measures");
            check("turning: a fit keeps the view", v + ", measured " + measuresT + " then " + m, "45.0, 10.0, 1.2".equals(v) && m instanceof Integer
                  && measuresT instanceof Integer && (Integer) m == (Integer) measuresT + 1, "45, 10, 1.2; measured once more");
            mc.thePlayer.closeScreen();
            next();
        }
    }

    private void checkLines(Minecraft mc, String view)
    {
        Object on = hook(mc, "linesOnGun"), drawn = hook(mc, "linesDrawn");
        check(view + ": every line ends on the gun", on + " of " + drawn, on instanceof Integer && drawn instanceof Integer && (Integer) drawn > 0
              && on.equals(drawn), "all of them (a real render of that view)");
    }

    private static java.lang.reflect.Method method(Object o, String name, Class<?>... types)
    {
        try
        {
            return o == null ? null : o.getClass().getMethod(name, types);
        }
        catch (NoSuchMethodException e)
        {
            return null; // an older screen: the check then fails
        }
    }

    private static void invoke(java.lang.reflect.Method m, Object arg)
    {
        GuiScreen s = Minecraft.getMinecraft().currentScreen;
        if (m != null && s != null && m.getDeclaringClass().isInstance(s))
        {
            try
            {
                m.invoke(s, arg);
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * A part picked onto the cursor, then the player's logout as FML posts it before the save (Minecraft saves the bag,
     * never the cursor): the part must be back in the bag.
     */
    private void logoutWithPartOnCursor()
    {
        EntityPlayerMP p = player();
        int i = find(Item.itemRegistry.getObject("deci:arSuppressor"));
        if (i >= 0 && p.openContainer instanceof GunsmithContainer)
        {
            p.openContainer.slotClick(i < 9 ? GunsmithContainer.HOTBAR + i : GunsmithContainer.MAIN + i - 9, 0, 0, p);
        }
        boolean picked = p.inventory.getItemStack() != null;
        new GunsmithContainer.Logout().loggedOut(new cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent(p));
        ItemStack cursor = p.inventory.getItemStack();
        read();
        logoutCursor = (picked ? "picked up" : "not picked up") + ", then cursor " + (cursor == null ? "empty" : "held") + ", " + countSupp
                       + " in the bag";
        if (cursor != null)
        {
            p.inventory.setItemStack(null); // a test leftover: it must not drop when the screen closes
            p.inventory.addItemStackToInventory(cursor);
        }
        // a full bag: what cannot go back drops beside the player, as closing the screen drops it
        i = find(Item.itemRegistry.getObject("deci:arSuppressor"));
        if (i >= 0)
        {
            p.openContainer.slotClick(i < 9 ? GunsmithContainer.HOTBAR + i : GunsmithContainer.MAIN + i - 9, 0, 0, p);
        }
        picked = p.inventory.getItemStack() != null;
        fillWithDirt();
        new GunsmithContainer.Logout().loggedOut(new cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent(p));
        cursor = p.inventory.getItemStack();
        read();
        logoutFull = (picked ? "picked up" : "not picked up") + ", then cursor " + (cursor == null ? "empty" : "held") + ", " + loose
                     + " on the ground";
        clearDirt();
        if (cursor != null)
        {
            p.inventory.setItemStack(null);
            p.inventory.addItemStackToInventory(cursor);
        }
        for (Object o : p.worldObj.getEntitiesWithinAABB(EntityItem.class, p.boundingBox.expand(4, 4, 4)))
        {
            p.inventory.addItemStackToInventory(((EntityItem) o).getEntityItem()); // the dropped part back in the bag
            ((EntityItem) o).setDead();
        }
        p.openContainer.detectAndSendChanges();
    }

    private void replaceAndClick()
    {
        EntityPlayerMP p = player();
        ItemStack h = p.inventory.getCurrentItem();
        p.inventory.setInventorySlotContents(p.inventory.currentItem, h == null ? null : h.copy());
        ItemStack got = p.openContainer.slotClick(GunSlot.BARREL.ordinal(), 0, 0, p);
        ItemStack cursor = p.inventory.getItemStack();
        swapClick = (got == null ? "nothing" : got.getDisplayName()) + ", cursor " + (cursor == null ? "empty" : cursor.getDisplayName());
        swapClean = got == null && cursor == null;
        if (cursor != null)
        {
            p.inventory.setItemStack(null); // a test leftover: it must not drop when the screen closes
        }
    }

    /** Every registered gun, by registry name. */
    private static List<String> allGuns()
    {
        List<String> g = new ArrayList<String>();
        for (Object o : Item.itemRegistry)
        {
            if (net.decimation.fixes.Deci.isGun((Item) o))
            {
                g.add(String.valueOf(Item.itemRegistry.getNameForObject(o)));
            }
        }
        return g;
    }

    /** What layoutChecks would fail on, as text ("" when all is right). */
    private static String layoutProblems(Minecraft mc)
    {
        GuiScreen s = mc.currentScreen;
        int[] b = (int[]) hook(mc, "gunBounds");
        if (s == null || b == null)
        {
            return "no screen or no gun drawn";
        }
        StringBuilder bad = new StringBuilder();
        int B = net.decimation.fixes.gunsmith.GunsmithLayout.BOX;
        int[][] boxes = new int[GunSlot.values().length][];
        for (GunSlot k : GunSlot.values())
        {
            int[] p = (int[]) hook(mc, "point", k);
            if (p == null || p[0] < b[0] - 1 || p[0] > b[0] + b[2] + 1 || p[1] < b[1] - 1 || p[1] > b[1] + b[3] + 1)
            {
                bad.append(k).append(" line off the gun ");
            }
            int[] x = (int[]) hook(mc, "box", k);
            boxes[k.ordinal()] = x;
            if (x == null || x[0] < 4 || x[0] + B > s.width - 4 || x[1] < net.decimation.fixes.gunsmith.GunsmithLayout.TOP
                || x[1] + B + 10 > s.height - net.decimation.fixes.gunsmith.GunsmithLayout.BOTTOM)
            {
                bad.append(k).append(" box off screen ");
            }
        }
        for (int i = 0; i < boxes.length; i++)
        {
            for (int j = i + 1; j < boxes.length; j++)
            {
                if (boxes[i] != null && boxes[j] != null && Math.abs(boxes[i][0] - boxes[j][0]) < B + 4
                    && Math.abs(boxes[i][1] - boxes[j][1]) < B + 14)
                {
                    bad.append(GunSlot.values()[i]).append('/').append(GunSlot.values()[j]).append(" overlap ");
                }
            }
        }
        return bad.toString().trim();
    }

    private void replaceHeld()
    {
        EntityPlayerMP p = player();
        ItemStack h = p.inventory.getCurrentItem();
        p.inventory.setInventorySlotContents(p.inventory.currentItem, h == null ? null : h.copy());
    }

    private void die()
    {
        EntityPlayerMP p = player();
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            n += s == null ? 0 : 1;
        }
        for (ItemStack s : p.inventory.armorInventory)
        {
            n += s == null ? 0 : 1;
        }
        carried = n;
        watched = null;
        p.attackEntityFrom(DamageSource.outOfWorld, 1000);
        countDrops(); // at once: the corpse's chunk can unload (and save it) a few ticks later
    }


    /** Stacks lying near the body: loose items and whatever a corpse holds. */
    private void countDrops()
    {
        EntityPlayerMP p = player();
        int n = p.worldObj.getEntitiesWithinAABB(EntityItem.class, p.boundingBox.expand(16, 16, 16)).size();
        Entity corpse = watched;
        if (corpse instanceof IInventory)
        {
            IInventory inv = (IInventory) corpse;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                n += inv.getStackInSlot(i) == null ? 0 : 1;
            }
        }
        int kept = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            kept += s == null ? 0 : 1;
        }
        for (ItemStack s : p.inventory.armorInventory)
        {
            kept += s == null ? 0 : 1;
        }
        dropped = n + kept;
        DevTestResults.value(name(), "death", (corpse == null ? "no corpse" : "a corpse") + ", kept " + kept);
    }
}
