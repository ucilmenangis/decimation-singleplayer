package net.decimation.fixes;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * The katana alight with Fire Aspect (user 23 Oktober 2026: "when the intro is done and the user is almost holding it,
 * fire over the whole metal"). The blade's pixels come from a heat mask (tools/guns/melee.py --heat: the metal of the
 * texture, red = how far along the blade, guard 0 to tip 255). Each frame (at most 30 a second) a texture of the same
 * size is filled with fire: tongues that run up the blade and flicker, dark red at their roots to a white hot core, and
 * a bright front while the flame still climbs from the guard (spread below 1). Drawn over the model just drawn, full
 * bright and additive, then once more twice as thick and faint: the heat around the metal. Client only.
 */
@SideOnly(Side.CLIENT)
final class MeleeFire
{
    private static final ResourceLocation HEAT = new ResourceLocation("deciworldgen", "textures/model/melee/katana_heat.png");
    /** Where along the blade each pixel is (0 .. 1), below 0 for no metal; loaded once. */
    private static float[] along;
    private static int w, h;
    private static DynamicTexture texture;
    private static ResourceLocation location;
    private static long lastFill;
    private static float lastSpread = -2;

    private MeleeFire()
    {
    }

    /** The fire over the model just drawn (its matrix still current): `spread` 0 .. 1 how far it climbed. */
    static void draw(Object model, float spread)
    {
        fill(spread);
        Minecraft mc = Minecraft.getMinecraft();
        float bx = OpenGlHelper.lastBrightnessX, by = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        mc.getTextureManager().bindTexture(location);
        GL11.glColor4f(1, 1, 1, 1);
        Deci.renderModel(model);
        GL11.glPushMatrix();                            // the heat: the blade twice as thick, faint
        GL11.glScalef(2.0f, 1.0f, 2.0f);
        GL11.glColor4f(1, 1, 1, 0.3f);
        Deci.renderModel(model);
        GL11.glPopMatrix();
        tongues(spread >= 1 ? 9 : spread * 1.25f - 0.05f);
        GL11.glPopAttrib();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
    }

    /** Tongues: TONGUES little flames at a time. AHEAD: how far ahead of the climbing flame the steel glows red. */
    private static final int TONGUES = 16;
    private static final float AHEAD = 0.18f;

    /**
     * Flames licking off the burning part of the blade: little square flames that leave the edge or the spine (in
     * turns), drift out and up the blade, shrink and cool from yellow to red as they go; each one born again at a new
     * place when it dies. Model space (the blade along -y, 1/16 block a unit, guard at -1, tip at -18); two crossed
     * squares each, so they show from the side and from above. Only up to `frontAt` along the blade (0 guard .. 1 tip:
     * the climbing flame's front).
     */
    private static void tongues(float frontAt)
    {
        float t = (System.nanoTime() % 600_000_000_000L) / 1e9f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int i = 0; i < TONGUES; i++)
        {
            float life = t * 1.7f + i * 0.618f;
            float ph = life - (float) Math.floor(life);              // 0 born .. 1 gone
            float s = fract((float) Math.floor(life) * 0.381f + i * 0.137f);
            if (s > frontAt)
            {
                continue;
            }
            float side = i % 2 == 0 ? 1 : -1;
            float y = -(1 + 17 * s - ph * 1.5f) * 0.0625f;            // drifting toward the tip as it rises
            float x = side * (0.03f + ph * 0.14f) - 1.1f * s * s * 0.0625f;
            float r = 0.012f + 0.03f * (1 - ph);
            int c = colour(1 - ph * 0.9f, 1);
            tess.setColorRGBA_I(c & 0xFFFFFF, (int) (230 * (1 - ph)));
            tess.addVertex(x - r, y - r, 0);
            tess.addVertex(x + r, y - r, 0);
            tess.addVertex(x + r, y + r, 0);
            tess.addVertex(x - r, y + r, 0);
            tess.addVertex(x, y - r, -r);
            tess.addVertex(x, y - r, r);
            tess.addVertex(x, y + r, r);
            tess.addVertex(x, y + r, -r);
        }
        tess.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static float fract(float v)
    {
        return v - (float) Math.floor(v);
    }

    /** The fire texture for now (the heat mask loaded on the first call). */
    private static void fill(float spread)
    {
        if (along == null)
        {
            load();
        }
        long now = System.nanoTime();
        if (now - lastFill < 33_000_000L && Math.abs(spread - lastSpread) < 0.01f)
        {
            return;
        }
        lastFill = now;
        lastSpread = spread;
        float t = (now % 600_000_000_000L) / 1e9f;
        // the burn after the catch (spread below 1): a front climbs from the hand to the tip; ahead of it the steel heats
        // to a dull red, behind it the flames grow to full over a fifth of the blade, at the front itself a bright line
        float frontAt = spread >= 1 ? 9 : spread * 1.25f - 0.05f;
        int[] out = texture.getTextureData();
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                int i = y * w + x;
                float s = along[i];
                if (s < 0 || s > frontAt + AHEAD)
                {
                    out[i] = 0;
                    continue;
                }
                if (s > frontAt)                                   // heating ahead of the flame: glowing red steel
                {
                    float k = 1 - (s - frontAt) / AHEAD;
                    float glow = k * k * (0.75f + 0.25f * (float) Math.sin(t * 6 + s * 30));
                    out[i] = colour(0.12f * glow, 0.55f * glow);
                    continue;
                }
                // tongues: two waves running up the blade, broken up across it, and a fast flicker
                float run = (float) (Math.sin(s * 26 - t * 9 + x * 0.31) * 0.5 + 0.5);
                float lick = (float) (Math.sin(y * 1.7 + t * 14 + s * 9 + x * 0.07) * 0.5 + 0.5);
                float flicker = (float) (Math.sin(t * 31 + x * 1.3 + y * 2.1) * 0.5 + 0.5);
                float grown = Math.min(1, (frontAt - s) / 0.2f + 0.25f);
                float v = (0.25f + 0.5f * run * lick + 0.25f * flicker) * grown;
                float front = Math.max(0, 1 - (frontAt - s) / 0.05f);
                v = Math.min(1, v + 0.8f * front);
                out[i] = colour(v, (0.45f + 0.55f * v) * Math.max(grown, front));
            }
        }
        texture.updateDynamicTexture();
    }

    /** Fire colour for heat v (0 .. 1): dark red, orange, yellow, a white hot core; alpha a. */
    private static int colour(float v, float a)
    {
        float r, g, b;
        if (v < 0.4f)
        {
            float k = v / 0.4f;
            r = 0.55f + 0.45f * k; g = 0.08f + 0.27f * k; b = 0.02f;
        }
        else if (v < 0.8f)
        {
            float k = (v - 0.4f) / 0.4f;
            r = 1; g = 0.35f + 0.5f * k; b = 0.02f + 0.13f * k;
        }
        else
        {
            float k = (v - 0.8f) / 0.2f;
            r = 1; g = 0.85f + 0.15f * k; b = 0.15f + 0.6f * k;
        }
        return (int) (a * 255) << 24 | (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    private static void load()
    {
        try (InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(HEAT).getInputStream())
        {
            BufferedImage img = ImageIO.read(in);
            w = img.getWidth();
            h = img.getHeight();
            along = new float[w * h];
            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    int p = img.getRGB(x, y);
                    along[y * w + x] = (p >>> 24) == 0 ? -1 : ((p >> 16) & 255) / 255f;
                }
            }
            texture = new DynamicTexture(w, h);
            location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("deciworldgen_katana_fire", texture);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("katana heat mask " + HEAT + " not readable", ex);
        }
    }
}
