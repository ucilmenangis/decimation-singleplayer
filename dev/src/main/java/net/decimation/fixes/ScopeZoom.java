package net.decimation.fixes;

import java.io.File;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.Configuration;

/**
 * Cheap scope (user request 2026-10-08): instead of Decimation's picture in
 * picture scope (the world rendered a second time into the scope glass every
 * frame a scoped gun is held: measured 103 -> 51 fps holding, 44 aiming), the
 * view zooms while aiming through a scope, like old shooters. Decimation's
 * own scope code is kept: tools/patches/PatchScope.java only gates its
 * renderScopeView behind the system property decimation.scope.pip, which
 * this class sets from config/deciworldgen_scope.cfg ("pictureInPicture",
 * default false). With it true the old scope runs and this class does
 * nothing.
 *
 * Zoom: the world FOV whose half angle tangent is the normal one's divided
 * by the scope's magnification (reddot 1.25, 2x, 4x, 8x, dragunov 4x;
 * integrated scopes and unknown sights from their zoomFov: the old scope
 * camera FOV 50 - zoomFov of 7.5 / 5 / 3.5 degrees maps to 2x / 4x / 8x).
 * The gun is drawn by EntityRenderer.renderHand at a fixed FOV of 70, so it
 * keeps its size.
 *
 * Scope glass: Decimation draws the scope texture on the glass part; behind
 * the glass the scope body is solid. Instead of a second world render, the
 * centre of the frame already drawn (zoomed) is copied into that texture
 * after the world and before the hand (RenderWorldLastEvent): one texture
 * copy per frame while a scoped gun is held.
 */
public class ScopeZoom
{
    public static final String PROPERTY = "decimation.scope.pip";

    private final Configuration cfg;
    private final boolean pictureInPicture;
    /** Side of the frame centre copied onto the scope glass, as a share of the screen height. */
    private final float glassView;

    public ScopeZoom(File configDir)
    {
        cfg = new Configuration(new File(configDir, "deciworldgen_scope.cfg"));
        pictureInPicture = cfg.getBoolean("pictureInPicture", "scope", false,
            "true = Decimation's original scope (the world rendered a second time into the scope glass; about "
            + "half the fps while a scoped gun is held). false = the view zooms while aiming through a scope.");
        glassView = cfg.getFloat("glassView", "scope", 0.2f, 0.05f, 1f,
            "zoom mode: side of the screen centre shown on the scope glass, as a share of the screen height");
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

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e)
    {
        EntityPlayer player = e.entity;
        ItemStack held = player != null ? player.getHeldItem() : null;
        if (held == null || !Deci.isGun(held.getItem()) || Deci.aimMode(player) != 1)
        {
            return;
        }
        float mag = magnificationOf(held);
        if (mag <= 1.001f)
        {
            return;
        }
        float base = Minecraft.getMinecraft().gameSettings.fovSetting;
        double target = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(base) / 2) / mag));
        e.newfov = (float) (target / base);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0 || !Deci.isScopedGun(mc.thePlayer.getHeldItem()))
        {
            return;
        }
        int side = Math.max(16, (int) (mc.displayHeight * glassView));
        int x = (mc.displayWidth - side) / 2, y = (mc.displayHeight - side) / 2;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Deci.scopeTexture());
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, x, y, side, side, 0);
    }
}
