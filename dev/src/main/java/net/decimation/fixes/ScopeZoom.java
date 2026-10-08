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
import net.minecraft.client.renderer.EntityRenderer;
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
 * Scope glass: Decimation draws the scope texture on the glass part; behind
 * the glass the scope body is solid. Instead of a second world render, the
 * whole finished world frame is copied into that texture after the world and
 * before the hand (RenderWorldLastEvent), and PatchScope makes the glass map
 * it by its own screen position (projective texturing), so the glass is
 * see-through at any window size, aspect and zoom. (v0.28.1 copied a centre
 * square sized by a guess and was misaligned in a large window.)
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
    /** How much of the zoom the mouse follows: 1 = turning slows to 1 / zoom, 0 = unchanged. */
    private final float sensitivity;
    private float savedSensitivity = Float.NaN;

    public ScopeZoom(File configDir)
    {
        cfg = new Configuration(new File(configDir, "deciworldgen_scope.cfg"));
        pictureInPicture = cfg.getBoolean("pictureInPicture", "scope", false,
            "true = Decimation's original scope (the world rendered a second time into the scope glass; about "
            + "half the fps while a scoped gun is held). false = the view zooms while aiming through a scope.");
        sensitivity = cfg.getFloat("sensitivity", "scope", 1f, 0f, 1f,
            "zoom mode: mouse slowdown while zoomed (1 = turning slows to 1 / zoom, 0 = unchanged)");
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
        float def = "reddot".equals(name) ? 1.25f : "2x".equals(name) ? 2f : "4x".equals(name) ? 4f
            : "8x".equals(name) ? 8f : "dragunovScope".equals(name) ? 4f : 0f;
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
            return Deci.isScope(sight) ? magnification(Deci.attachmentName(sight), Deci.zoomFov(sight)) : 0f;
        }
        return Deci.hasIntegratedScope(held.getItem())
            ? magnification(Deci.integratedScopeName(held.getItem()), 46.5f) : 0f;
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
    /**
     * Sight position per gun + scope + window aspect ("item|sight|aspect"): the glass centre without
     * zoom and offset (normalised device coordinates), learned once the aim
     * pose has settled and then averaged (Decimation's weapon sway averages
     * out). Kept for the session, so later aims are centred at once.
     */
    private final java.util.Map<String, double[]> sights = new java.util.HashMap<String, double[]>();
    private long lastGlass;
    /** Last unshifted sample and how many samples in a row agreed with it. */
    private double prevX = Double.NaN, prevY = Double.NaN;
    private int stable;
    private long aimStart;
    private boolean wasAiming;
    private long lastFrame;
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
        // until its sight position is learned, at most 0.8 s
        if (!sights.containsKey(sightKey(held)) && System.nanoTime() - aimStart < 800_000_000L)
        {
            return Math.min(mag, 1.5f);
        }
        return mag;
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
        boolean aiming = mc.thePlayer != null && Deci.aimMode(mc.thePlayer) == 1;
        if (aiming && !wasAiming)
        {
            aimStart = now;
            stable = 0;
        }
        wasAiming = aiming;
        float target = mc.theWorld != null ? targetZoom(mc) : 1f;
        // ease about 0.15 s, in log space so 1x -> 8x feels as quick as 1x -> 2x
        double lz = Math.log(zoom), lt = Math.log(target);
        lz += (lt - lz) * (1 - Math.exp(-dt * 18));
        zoom = Math.abs(lz - lt) < 0.002 ? target : Math.exp(lz);
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

    /** Vanilla skips the hand while zoomed: draw it here, with the same zoom. */
    @SubscribeEvent
    public void onRenderHand(RenderHandEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (zoom == 1.0 || mc.entityRenderer == null)
        {
            return;
        }
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        centreSight(mc);
        try
        {
            CAMERA_YAW.setDouble(mc.entityRenderer, shiftX);
            CAMERA_PITCH.setDouble(mc.entityRenderer, -shiftY);
            RENDER_HAND.invoke(mc.entityRenderer, e.partialTicks, e.renderPass);
            learnSight(mc);
        }
        catch (Exception ex)
        {
            FMLLog.warning("[deciworldgen] scope: renderHand failed: %s", ex);
        }
        finally
        {
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
     * measures every 4th frame, during this very draw) was taken with this
     * frame's zoom and shift, so its unshifted position is exact. Samples
     * whose box touches the screen edge are skipped (clipped, e.g. an 8x
     * glass), and a position is only trusted after 3 samples in a row agree
     * (the gun swings in from the hip first: an early sample once pushed the
     * glass off screen for good). Then averaged slowly into the per gun +
     * scope entry.
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
            sights.put(key, new double[] {sx, sy});
        }
        else
        {
            sight[0] += (sx - sight[0]) * 0.05;
            sight[1] += (sy - sight[1]) * 0.05;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0 || !Deci.isScopedGun(mc.thePlayer.getHeldItem()))
        {
            return;
        }
        // whole frame: the patched glass maps it by screen position (projective texturing)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Deci.scopeTexture());
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0, mc.displayWidth, mc.displayHeight, 0);
    }
}
