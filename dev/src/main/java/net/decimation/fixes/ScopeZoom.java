package net.decimation.fixes;

import java.io.File;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.decimation.fixes.gunsmith.GunsmithClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.Configuration;

/**
 * Cheap scope (user request 2026-10-08): instead of Decimation's picture in
 * picture scope (the world rendered a second time into the scope glass every
 * frame a scoped gun is held: measured 103 -> 51 fps holding, 44 aiming), the
 * whole picture zooms while aiming through a scope, world AND gun, like the
 * "normal" (non picture in picture) scopes of most shooters (the user chose
 * this look from a NORMAL / PIP comparison). Decimation's own scope code is
 * kept: tools/patches/PatchScope.java only gates its renderScopeView behind
 * the system property decimation.scope.pip, which this class sets from
 * config/deciworldgen_scope.cfg ("pictureInPicture", default false). With it
 * true the old scope runs and this class does nothing.
 *
 * Zoom: EntityRenderer.cameraZoom (vanilla's big screenshot zoom) scales the
 * projection by the scope's magnification (reddot 1.25, 2x, 4x, 8x, dragunov
 * 4x; integrated scopes and unknown sights from their zoomFov: the old scope
 * camera FOV 50 - zoomFov of 7.5 / 5 / 3.5 degrees maps to 2x / 4x / 8x),
 * eased in and out. Vanilla skips the hand while cameraZoom is not 1, so on
 * RenderHandEvent (fired right before that check) this class calls
 * renderHand itself; renderHand applies the same cameraZoom scale, so the gun
 * zooms with the world.
 *
 * Scope glass: behind the glass the scope body is solid. After the world
 * and before the hand (RenderWorldLastEvent) the part of the frame under the
 * glass (its last measured box plus a margin; the whole frame while the zoom
 * changes) is copied into the scope texture, allocated once per window size;
 * the patched glass maps it by its own screen position, so it is
 * see-through at any window size and zoom and hides the gun's own front
 * sight behind it. (A depth only glass without any copy failed: attachment
 * glasses are drawn after the gun body.)
 *
 * Sight on the screen centre: shots go to the screen centre, but Decimation's
 * aiming pose leaves the eyepiece a little off it, and zooming about the
 * centre multiplies that offset (4x: about 100 px in a 900 px window). The
 * patched glass reports its screen position (GL feedback), and while the
 * hand is drawn here EntityRenderer.cameraYaw / cameraPitch (a projection
 * offset applied with cameraZoom) move the gun so the eyepiece sits on the
 * centre (centreSight). The world is drawn without that offset, so the
 * see-through glass shows exactly the point shots go to.
 *
 * Scopes from overlayFrom (default 4x: 4x, dragunov, 8x, the integrated aug
 * scope, never our own sights: the SpecterDR at 4x keeps the gun and its glass;
 * user choice 2026-10-08, first 8x only, then 4x too: "4x is a bit
 * long in zoom mode") use the classic sniper overlay ("fake it" with a
 * black layout): once the zoom is most of the way in, the
 * gun is not drawn and the screen is black but for a round view with the
 * scope's own reticle texture (textures/model/guns/scopes/<name>.png) and a
 * soft dark edge. No gun, no glass copy: the cheapest mode.
 *
 * Mouse: while zoomed the sensitivity is scaled so the view turns about 1 /
 * zoom as fast (config "sensitivity": 1 = fully, 0 = off); set at render
 * tick start (before EntityRenderer turns the player) and restored at the
 * end, so options.txt never sees the changed value.
 */
public class ScopeZoom
{
    public static final String PROPERTY = "decimation.scope.pip";

    private final Configuration cfg;
    private final boolean pictureInPicture;
    /** Magnification from which the sniper overlay replaces the gun model. */
    private final float overlayFrom;
    /** The reticle texture of the overlay scope being aimed, or null when no overlay is shown. */
    private ResourceLocation overlay;
    /**
     * Our sights (NewSights): their reticle is drawn in the glass by SightReticle (it sways with the
     * gun); while aiming through one, the vanilla crosshair is hidden (the reticle is the crosshair).
     * They never get the black sniper overlay: the gun and the glass stay (the SpecterDR at 4x too).
     */
    private static final java.util.Set<String> OUR_SIGHTS = new java.util.HashSet<String>(
        java.util.Arrays.asList("eotech558", "ta11acog", "specterdr"));
    private boolean aimingOurSight;

    /** How much of the zoom the mouse follows: 1 = turning slows to 1 / zoom, 0 = unchanged. */
    private final float sensitivity;
    private float savedSensitivity = Float.NaN;

    public ScopeZoom(File configDir)
    {
        INSTANCE = this;
        sightsFile = new File(configDir, "deciworldgen_sights.txt");
        cfg = new Configuration(new File(configDir, "deciworldgen_scope.cfg"));
        pictureInPicture = cfg.getBoolean("pictureInPicture", "scope", false,
            "true = Decimation's original scope (the world rendered a second time into the scope glass; about "
            + "half the fps while a scoped gun is held). false = the view zooms while aiming through a scope.");
        sensitivity = cfg.getFloat("sensitivity", "scope", 1f, 0f, 1f,
            "zoom mode: mouse slowdown while zoomed (1 = turning slows to 1 / zoom, 0 = unchanged)");
        overlayFrom = cfg.getFloat("overlayFrom", "scope", 4f, 1f, 100f,
            "zoom mode: scopes with at least this magnification hide the gun and show a black sniper "
            + "overlay with the reticle while aiming (100 = never)");
        for (String k : new String[] {"reddot", "2x", "4x", "8x", "dragunovScope"})
        {
            magnification(k, 0f);
        }
        cfg.save();
        System.setProperty(PROPERTY, String.valueOf(pictureInPicture));
        FMLLog.info("[deciworldgen] scope: %s", pictureInPicture ? "picture in picture (original)" : "view zoom");
    }

    public boolean enabled()
    {
        return !pictureInPicture;
    }

    /** Configured magnification of a scope by name, else from its old zoomFov. */
    private float magnification(String name, float zoomFov)
    {
        float def = "reddot".equals(name) || "eotech558".equals(name) || "specterdr1x".equals(name) ? 1.25f
            : "2x".equals(name) ? 2f : "4x".equals(name) || "specterdr".equals(name) ? 4f
            : "ta11acog".equals(name) ? 3.5f : "8x".equals(name) ? 8f : "dragunovScope".equals(name) ? 4f : 0f;
        if (def > 0)
        {
            return (float) cfg.get("magnification", name, def, "zoom while aiming through this scope").getDouble(def);
        }
        float scopeFov = 50f - zoomFov;
        return scopeFov >= 7f ? 2f : scopeFov >= 4.5f ? 4f : 8f;
    }

    /** Magnification of the scope on this held gun, or 0 when it has none. */
    private float magnificationOf(ItemStack held)
    {
        Item sight = Deci.sightAttachment(held);
        if (sight != null)
        {
            String name = Deci.attachmentName(sight);
            if ("specterdr".equals(name) && !SpecterMode.isFour(held, slotOf(held)))
            {
                name = "specterdr1x";              // flipped to 1x (SpecterKey): as close as the red dots
            }
            return Deci.isScope(sight) ? magnification(name, Deci.zoomFov(sight)) : 0f;
        }
        return Deci.hasIntegratedScope(held.getItem())
            ? magnification(Deci.integratedScopeName(held.getItem()), 46.5f) : 0f;
    }

    /** The inventory slot of this stack in the player's inventory (the held slot first), or -1. */
    private static int slotOf(ItemStack stack)
    {
        EntityPlayer p = Minecraft.getMinecraft().thePlayer;
        if (p == null)
        {
            return -1;
        }
        if (stack == p.getHeldItem())
        {
            return p.inventory.currentItem;
        }
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            if (p.inventory.mainInventory[i] == stack)
            {
                return i;
            }
        }
        return -1;
    }

    /** True when aiming through this gun's scope shows the black sniper overlay instead of the gun (never ours). */
    private boolean overlaid(ItemStack held, float mag)
    {
        Item sight = Deci.sightAttachment(held);
        return mag >= overlayFrom && (sight == null || !OUR_SIGHTS.contains(Deci.attachmentName(sight)));
    }

    /** The magnification aiming through this gun's scope gives (0 without one; the SpecterDR by its mode). */
    public static float magnificationFor(ItemStack held)
    {
        return INSTANCE == null || held == null || !Deci.isGun(held.getItem()) ? 0f : INSTANCE.magnificationOf(held);
    }

    /** True when aiming through this gun's scope shows the black sniper overlay instead of the gun. */
    public static boolean overlays(ItemStack held)
    {
        return INSTANCE != null && held != null && Deci.isGun(held.getItem()) && INSTANCE.overlaid(held, INSTANCE.magnificationOf(held));
    }

    /** Dev test: the zoom applied now (eased; 1 when not aiming through a scope). */
    public static double currentZoom()
    {
        return INSTANCE == null ? 1 : INSTANCE.zoom;
    }

    /** Dev test aimdelay: forget the learned sight positions, so the next aim is a first aim as after a launch. */
    public static void forgetSights()
    {
        if (INSTANCE != null)
        {
            INSTANCE.sights.clear();
        }
    }

    /** Dev test aimdelay: as after a new launch: the session's sight positions dropped, the kept ones read back. */
    public static void relaunchSights()
    {
        if (INSTANCE != null)
        {
            INSTANCE.sights.clear();
            INSTANCE.sightsRead = false;
            INSTANCE.readSights();
        }
    }

    private void readSights()
    {
        if (!sightsRead && sightsFile != null)
        {
            sightsRead = true;
            sights.putAll(SightMemory.load(sightsFile, Updater.current(), LEARN));
        }
    }

    /** Dev test: the sniper overlay is being drawn. */
    public static boolean overlayShown()
    {
        return INSTANCE != null && INSTANCE.overlay != null;
    }

    private static final java.lang.reflect.Field CAMERA_ZOOM = ReflectionHelper.findField(EntityRenderer.class,
        "cameraZoom", "field_78503_V");
    private static final java.lang.reflect.Field CAMERA_YAW = ReflectionHelper.findField(EntityRenderer.class,
        "cameraYaw", "field_78502_W");
    private static final java.lang.reflect.Field CAMERA_PITCH = ReflectionHelper.findField(EntityRenderer.class,
        "cameraPitch", "field_78509_X");
    private static final java.lang.reflect.Method RENDER_HAND = ReflectionHelper.findMethod(EntityRenderer.class,
        null, new String[] {"renderHand", "func_78476_b"}, float.class, int.class);

    /** Current (eased) zoom and the renderer it was set on. */
    private double zoom = 1;
    /** Gun offset applied while drawing the hand (projection units). */
    private double shiftX, shiftY;
    private static ScopeZoom INSTANCE;
    /**
     * Sight position per gun + scope + window aspect ("item|sight|aspect"): the glass centre without
     * zoom and offset (normalised device coordinates) and the number of
     * samples behind it, learned once the aim pose has settled and averaged
     * (Decimation's weapon sway averages out). Kept for the session, so later
     * aims are centred at once.
     */
    private final java.util.Map<String, double[]> sights = new java.util.HashMap<String, double[]>();
    /**
     * Learned positions kept between launches (fixes/SightMemory, config/deciworldgen_sights.txt): without them every
     * launch held the zoom at 1.5x on each gun and sight's first aim until learned again (user: "sometime it has delay";
     * dev test aimdelay: ACOG 415 ms to 95 % zoom against 188 ms once learned). Read on the first frame (our version is
     * known by then), written when a sight becomes learned.
     */
    private File sightsFile;
    private boolean sightsRead;
    private long lastGlass;
    /** Last unshifted sample and how many samples in a row agreed with it. */
    private double prevX = Double.NaN, prevY = Double.NaN;
    private int stable;
    private long aimStart;
    private boolean wasAiming;
    private long lastFrame;
    /** Frames counted by this class (onRenderTick): which of them measure the glass (centreSight). */
    private int measureFrame;
    private EntityRenderer zoomed;

    /** Target zoom: the scope's magnification while aiming through it, else 1. */
    private float targetZoom(Minecraft mc)
    {
        EntityPlayer player = mc.thePlayer;
        ItemStack held = player != null ? player.getHeldItem() : null;
        if (held == null || !Deci.isGun(held.getItem()) || Deci.aimMode(player) != 1
            || mc.gameSettings.thirdPersonView != 0 || mc.currentScreen != null)
        {
            return 1f;
        }
        float mag = magnificationOf(held);
        if (mag <= 1.001f)
        {
            return 1f;
        }
        // first aim with this gun + scope: stay low (the glass fits the screen)
        // until its sight position is learned, at most 1.5 s (not for the
        // sniper overlay: no gun is drawn there)
        if (!overlaid(held, mag) && !learned(sightKey(held)) && System.nanoTime() - aimStart < 1_500_000_000L)
        {
            return Math.min(mag, 1.5f);
        }
        return mag;
    }

    /** Dev test: the gun shift applied while aiming {x, y} (normalised device coordinates). */
    public static double[] debugShift()
    {
        return INSTANCE == null ? null : new double[] {INSTANCE.shiftX, INSTANCE.shiftY};
    }

    /** Settled samples needed before a sight position is trusted for zooming in. */
    private static final int LEARN = 10;

    private boolean learned(String key)
    {
        double[] s = sights.get(key);
        return s != null && s[2] >= LEARN;
    }

    private String sightKey(ItemStack held)
    {
        Item sight = Deci.sightAttachment(held);
        Minecraft mc = Minecraft.getMinecraft();
        // the hand projection depends on the window's aspect: a resized window learns anew
        return Item.itemRegistry.getNameForObject(held.getItem()) + "|"
            + (sight != null ? Deci.attachmentName(sight) : "integrated") + "|"
            + (mc.displayHeight > 0 ? mc.displayWidth * 100 / mc.displayHeight : 0);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        readSights();
        if (e.phase == TickEvent.Phase.END)
        {
            if (!Float.isNaN(savedSensitivity))
            {
                mc.gameSettings.mouseSensitivity = savedSensitivity;
                savedSensitivity = Float.NaN;
            }
            return;
        }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        boolean aiming = mc.thePlayer != null && Deci.aimMode(mc.thePlayer) == 1 && !GunsmithClient.isOpen();
        if (aiming && !wasAiming)
        {
            aimStart = now;
            stable = 0;
        }
        wasAiming = aiming;
        float target = mc.theWorld != null ? targetZoom(mc) : 1f;
        // the glass is measured only while our own gun is drawn (onRenderHand): scoped guns of NPCs
        // in the world drew their glass too and were taken for ours (user report 11 Oktober 2026:
        // the gun crept down while aiming until it left the screen, a sniper NPC in view)
        Deci.setScopeGlassEvery(-1);
        measureFrame++;
        // ease about 0.15 s, in log space so 1x -> 8x feels as quick as 1x -> 2x
        double lz = Math.log(zoom), lt = Math.log(target);
        lz += (lt - lz) * (1 - Math.exp(-dt * 18));
        zoom = Math.abs(lz - lt) < 0.002 ? target : Math.exp(lz);
        overlay = overlayFor(mc, target);
        glassReticleFor(mc, target);
        if (zoom > 1.001 && sensitivity > 0 && mc.currentScreen == null)
        {
            // EntityRenderer turns by (s * 0.6 + 0.2)^3: scale that by zoom^-sensitivity
            savedSensitivity = mc.gameSettings.mouseSensitivity;
            double f = savedSensitivity * 0.6 + 0.2;
            f /= Math.pow(zoom, sensitivity / 3.0);
            mc.gameSettings.mouseSensitivity = (float) ((f - 0.2) / 0.6);
        }
        try
        {
            if (zoomed != null && zoomed != mc.entityRenderer)
            {
                CAMERA_ZOOM.setDouble(zoomed, 1.0); // renderer swapped (Decimation's stance camera)
            }
            if (mc.entityRenderer != null)
            {
                CAMERA_ZOOM.setDouble(mc.entityRenderer, zoom);
                zoomed = mc.entityRenderer;
            }
        }
        catch (IllegalAccessException ex)
        {
            // field found by ReflectionHelper is accessible
        }
    }

    /**
     * True while this class draws the hand itself. OptiFine posts RenderHandEvent inside renderHand (Forge posts it
     * before): our own call came back here, cancelled and called again until Java's stack ran out, pushing a matrix
     * each time: the gun vanished while aiming and every frame logged "Stack overflow" (the brother's laptop, NVIDIA and
     * OptiFine HD U E7, 19 Oktober 2026; dev test scopeperf in the production client with OptiFine).
     */
    private boolean drawingHand;
    /** The glass at the hip: a plain dark lens (0xRRGGBB), not the last aimed view. */
    public static final int LENS = 0x14181C;
    private long lastHandWarning;

    private boolean copied;
    private java.nio.ByteBuffer lensRows;

    /** The whole scope texture the plain {@link #LENS} colour, in strips of 32 rows (once, when the copying stops). */
    private void clearGlass()
    {
        if (texW <= 0 || texH <= 0)
        {
            return;
        }
        int rows = 32;
        if (lensRows == null || lensRows.capacity() < texW * rows * 3)
        {
            lensRows = org.lwjgl.BufferUtils.createByteBuffer(texW * rows * 3);
            for (int i = 0; i < texW * rows; i++)
            {
                lensRows.put((byte) (LENS >> 16)).put((byte) (LENS >> 8)).put((byte) LENS);
            }
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Deci.scopeTexture());
        int align = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);            // rows of width x 3 bytes
        for (int y = 0; y < texH; y += rows)
        {
            ((java.nio.Buffer) lensRows).position(0);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, y, texW, Math.min(rows, texH - y), GL11.GL_RGB,
                                 GL11.GL_UNSIGNED_BYTE, lensRows);
        }
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, align);
    }

    /** Vanilla skips the hand while zoomed: draw it here, with the same zoom. */
    @SubscribeEvent
    public void onRenderHand(RenderHandEvent e)
    {
        if (drawingHand)
        {
            return;                     // our own renderHand call below, posted again by OptiFine: let it draw
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (overlay != null)
        {
            e.setCanceled(true); // sniper overlay: no gun
            return;
        }
        if (zoom == 1.0 || mc.entityRenderer == null)
        {
            // vanilla draws the hand next, and whatever else draws the glass in this frame after it: not measured here
            // (every glass sight zooms past 1 from its first aimed frame on, where the measuring below runs)
            Deci.setScopeGlassEvery(-1);
            return;
        }
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        centreSight(mc);
        try
        {
            CAMERA_YAW.setDouble(mc.entityRenderer, shiftX);
            CAMERA_PITCH.setDouble(mc.entityRenderer, -shiftY);
            long before = Deci.scopeGlassTime();
            drawingHand = true;
            RENDER_HAND.invoke(mc.entityRenderer, e.partialTicks, e.renderPass);
            Deci.setScopeGlassEvery(-1);
            if (Deci.scopeGlassTime() != before)
            {
                learnSight(mc);         // a sample of our own gun's glass, taken in this draw
            }
        }
        catch (Exception ex)
        {
            long now = System.currentTimeMillis();
            if (now - lastHandWarning > 60_000)  // once a minute, not every frame
            {
                lastHandWarning = now;
                FMLLog.warning("[deciworldgen] scope: renderHand failed: %s", ex.getCause() != null ? ex.getCause() : ex);
            }
        }
        finally
        {
            drawingHand = false;
            Deci.setScopeGlassEvery(-1);
            try
            {
                CAMERA_YAW.setDouble(mc.entityRenderer, 0.0);
                CAMERA_PITCH.setDouble(mc.entityRenderer, 0.0);
            }
            catch (IllegalAccessException ex)
            {
                // accessible
            }
        }
        e.setCanceled(true);
    }

    /**
     * Gun offset that puts the sight on the screen centre: renderHand maps a
     * point p (normalised device coordinates) to zoom * p + shift, so the
     * sight (unzoomed, unshifted position s) lands on 0 with shift =
     * -zoom * s. Faded in over the first quarter of the zoom. Reset when
     * aiming stops or the gun changes (the next aim learns its own sight).
     */
    private void centreSight(Minecraft mc)
    {
        ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
        double[] sight = held != null && Deci.isGun(held.getItem()) ? sights.get(sightKey(held)) : null;
        // measure often while a sight is being learned (every 2nd frame), rarely once it is known (every 8th), only
        // during the hand draw below (onRenderHand switches measuring off right after it). The frames are counted here:
        // PatchScope counts every glass draw, and the production client with OptiFine drew the glass 6 times a frame (2
        // before the hand, 3 after it), so its count put the hand's draw on an odd number every frame and it was never
        // measured (bug.md "See-through glass not updated with OptiFine"); mask 0 = measure every draw while it is set
        boolean learning = held != null && Deci.isGun(held.getItem()) && targetZoom(mc) > 1f && (sight == null || sight[2] < LEARN);
        Deci.setScopeGlassEvery((measureFrame & (learning ? 1 : 7)) == 0 ? 0 : -1);
        if (sight == null || zoom <= 1.0)
        {
            shiftX = shiftY = 0;
            return;
        }
        double fade = Math.min(1.0, (zoom - 1.0) * 4.0);
        shiftX = -zoom * sight[0] * fade;
        shiftY = -zoom * sight[1] * fade;
    }

    /**
     * After the hand was drawn: a new glass sample (the patched glass
     * measures every 8th frame, during this very draw) was taken with this
     * frame's zoom and shift, so its unshifted position is exact. Samples
     * whose box touches the screen edge are skipped (clipped, e.g. an 8x
     * glass), and only samples after 3 in a row agree count (the gun swings
     * in from the hip first: an early sample once pushed the glass off screen
     * for good). The first LEARN settled samples are averaged (measured every
     * 2nd frame, zoom held at 1.5x meanwhile), later ones slowly.
     */
    private void learnSight(Minecraft mc)
    {
        float[] g = Deci.scopeGlassOnScreen();
        long t = Deci.scopeGlassTime();
        ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
        if (g == null || t == lastGlass || held == null || mc.displayWidth <= 0 || mc.displayHeight <= 0
            || targetZoom(mc) <= 1f)
        {
            return;
        }
        lastGlass = t;
        float[] box = Deci.scopeGlassBox();
        if (box[0] < 2 || box[1] < 2 || box[2] > mc.displayWidth - 2 || box[3] > mc.displayHeight - 2)
        {
            stable = 0;
            return;
        }
        double sx = ((2.0 * g[0] / mc.displayWidth - 1) - shiftX) / zoom;
        double sy = ((2.0 * g[1] / mc.displayHeight - 1) - shiftY) / zoom;
        stable = Math.abs(sx - prevX) < 0.01 && Math.abs(sy - prevY) < 0.01 ? stable + 1 : 0;
        prevX = sx;
        prevY = sy;
        if (stable < 3)
        {
            return;
        }
        String key = sightKey(held);
        double[] sight = sights.get(key);
        if (sight == null)
        {
            sights.put(key, new double[] {sx, sy, 1});
            return;
        }
        // running mean over the first LEARN settled samples, then a slow average
        double a = sight[2] < LEARN ? 1.0 / (sight[2] + 1) : 0.05;
        sight[0] += (sx - sight[0]) * a;
        sight[1] += (sy - sight[1]) * a;
        sight[2]++;
        if (sight[2] == LEARN && sightsFile != null)
        {
            SightMemory.save(sightsFile, sights, Updater.current(), LEARN);   // once per sight: the next launch has it
        }
    }


    /** Size the scope texture was allocated at (reallocated when the window changes). */
    private int texW, texH;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (overlay != null || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0
            || !Deci.isScopedGun(mc.thePlayer.getHeldItem()))
        {
            return;
        }
        // the glass shows the world only while aiming (or zooming in / out): holding a scoped gun at the
        // hip copied the whole screen every frame (the glass box is unknown there), and so did every
        // frame of the zoom animation: a large frame drop on a weak laptop (bug report 13 Oktober 2026)
        boolean aiming = Deci.aimMode(mc.thePlayer) == 1 && !GunsmithClient.isOpen(); // the Gunsmith hides the hand: no glass
        if (!aiming && zoom <= 1.001)
        {
            if (copied)
            {
                clearGlass(); // else the glass keeps the last aimed view at the hip (a frozen "camera": brother's laptop)
                copied = false;
            }
            return;
        }
        int w = mc.displayWidth, h = mc.displayHeight;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Deci.scopeTexture());
        copied = true;
        if (w != texW || h != texH)
        {
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0, w, h, 0);
            texW = w;
            texH = h;
            return;
        }
        float[] box = Deci.scopeGlassBox();
        if (!(box[2] > box[0] && box[3] > box[1]))
        {
            return;                                       // no glass measured yet: nothing to fill
        }
        // the box is up to 8 frames old: a margin for sway and turning, wider while the gun still moves
        boolean settled = Deci.scopeGlassOnScreen() != null && Math.abs(zoom - targetZoom(mc)) < 0.01;
        int m = settled ? 16 + h / 20 : 16 + h / 6;
        int x0 = Math.max(0, (int) box[0] - m), y0 = Math.max(0, (int) box[1] - m);
        int x1 = Math.min(w, (int) box[2] + m), y1 = Math.min(h, (int) box[3] + m);
        if (x1 > x0 && y1 > y0)
        {
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, x0, y0, x1 - x0, y1 - y0);
        }
    }

    /**
     * The reticle texture when the sniper overlay should show: a scope of at
     * least overlayFrom that is not one of ours, aimed, and the zoom at least
     * 70% of the way in (the gun raises normally first).
     */
    private ResourceLocation overlayFor(Minecraft mc, float target)
    {
        ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
        if (held == null || !Deci.isGun(held.getItem()) || target <= 1f || !overlaid(held, target) || zoom < 1 + (target - 1) * 0.7)
        {
            return null;
        }
        Item sight = Deci.sightAttachment(held);
        String name = sight != null ? Deci.attachmentName(sight) : Deci.integratedScopeName(held.getItem());
        return new ResourceLocation("deci", "textures/model/guns/scopes/" + name + ".png");
    }

    /** True while aiming through one of our sights (zoom most of the way in). */
    private void glassReticleFor(Minecraft mc, float target)
    {
        ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
        Item sight = held != null && Deci.isGun(held.getItem()) ? Deci.sightAttachment(held) : null;
        aimingOurSight = sight != null && OUR_SIGHTS.contains(Deci.attachmentName(sight)) && overlay == null
            && target > 1f && zoom >= 1 + (target - 1) * 0.7;
    }

    /**
     * While aiming through one of our sights its reticle (drawn in the glass by SightReticle, moving
     * with the gun's sway since v0.41.1; a screen fixed reticle before) is the crosshair.
     */
    private void hideCrosshair(RenderGameOverlayEvent.Pre e)
    {
        if (e.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS)
        {
            e.setCanceled(true);
        }
    }

    /** Sniper overlay: black screen but for a round view, the reticle, a soft edge (before the HUD). */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre e)
    {
        if (overlay == null && aimingOurSight)
        {
            hideCrosshair(e);
            return;
        }
        if (overlay == null || e.type != RenderGameOverlayEvent.ElementType.HELMET)
        {
            if (overlay != null && e.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS)
            {
                e.setCanceled(true); // the reticle is the crosshair
            }
            return;
        }
        double w = e.resolution.getScaledWidth_double(), h = e.resolution.getScaledHeight_double();
        double cx = w / 2, cy = h / 2, r = Math.min(w, h) * 0.47, far = Math.hypot(w, h);
        Tessellator t = Tessellator.instance;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE); // GUI y points down: the strips wind backwards
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int n = 96;
        // solid black outside the circle
        t.startDrawing(GL11.GL_TRIANGLE_STRIP);
        t.setColorRGBA_F(0f, 0f, 0f, 1f);
        for (int i = 0; i <= n; i++)
        {
            double a = 2 * Math.PI * i / n;
            t.addVertex(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0);
            t.addVertex(cx + Math.cos(a) * far, cy + Math.sin(a) * far, 0);
        }
        t.draw();
        // soft dark edge inside the circle (lens vignette): thin solid rings
        // darkening outward (per vertex colours came out white in the HUD state)
        int rings = 12;
        for (int k = 0; k < rings; k++)
        {
            double r0 = r * (0.88 + 0.12 * k / rings), r1 = r * (0.88 + 0.12 * (k + 1) / rings);
            t.startDrawing(GL11.GL_TRIANGLE_STRIP);
            t.setColorRGBA_F(0f, 0f, 0f, (float) Math.pow((k + 1) / (double) rings, 0.8) * 0.95f);
            for (int i = 0; i <= n; i++)
            {
                double a = 2 * Math.PI * i / n;
                t.addVertex(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, 0);
                t.addVertex(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1, 0);
            }
            t.draw();
        }
        // the scope's reticle, centred, as big as the view
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(overlay);
        t.startDrawingQuads();
        t.addVertexWithUV(cx - r, cy + r, 0, 0, 1);
        t.addVertexWithUV(cx + r, cy + r, 0, 1, 1);
        t.addVertexWithUV(cx + r, cy - r, 0, 1, 0);
        t.addVertexWithUV(cx - r, cy - r, 0, 0, 0);
        t.draw();
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}