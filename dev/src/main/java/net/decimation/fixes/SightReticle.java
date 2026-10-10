package net.decimation.fixes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Our sights' reticles drawn IN the glass (user review of v0.41.0: "the crosshair is intact now
 * following guns sway or scope sway ... learn how the scope working on 1x, 2x ... tarkov style where
 * scope follow sway"): a textured square on the eye side of the sight's glass, in the gun's own
 * model space, so it moves with the gun, its sway and its recoil like Decimation's red dot (whose
 * dot is part of its model). Position and size per sight: assets/deciworldgen/sight_reticles.txt
 * (tools/guns/sights.py, attachment file space); the per gun sight offset of SightPlacement is
 * added. Called from IronSights.end, still inside GunItemRenderer's gun draw method (the gun's
 * matrix, its aimShift included); first person only (a reticle seen from outside the sight would
 * not be real).
 */
public final class SightReticle
{
    /** name -> {x, y, z, size} (model units, attachment file space). */
    private static Map<String, float[]> specs;

    private SightReticle()
    {
    }

    private static Map<String, float[]> specs()
    {
        if (specs == null)
        {
            specs = new HashMap<String, float[]>();
            InputStream in = SightReticle.class.getResourceAsStream("/assets/deciworldgen/sight_reticles.txt");
            if (in != null)
            {
                try
                {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    for (String line; (line = r.readLine()) != null; )
                    {
                        String[] f = line.trim().split("\\s+");
                        if (f.length == 5 && !line.startsWith("#"))
                        {
                            specs.put(f[0], new float[] {Float.parseFloat(f[1]), Float.parseFloat(f[2]),
                                                         Float.parseFloat(f[3]), Float.parseFloat(f[4])});
                        }
                    }
                    r.close();
                }
                catch (Exception e)
                {
                    // no reticles then
                }
            }
        }
        return specs;
    }

    static void draw(ItemStack stack)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0 || stack != mc.thePlayer.getHeldItem())
        {
            return;
        }
        Item sight = Deci.sightAttachment(stack);
        if (sight == null)
        {
            return;
        }
        String name = Deci.attachmentName(sight);
        float[] s = specs().get(name);
        if (s == null)
        {
            return;
        }
        float dy = SightPlacement.offsetFor(GameRegistry.findUniqueIdentifierFor(stack.getItem()).name, name);
        float u = 0.0625f, x = s[0], y = s[1] + dy, z = s[2], h = s[3] / 2;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glTranslatef(0.05f, 0.07f, -0.008f);          // renderAttachments' sight translate
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f); // lit (illuminated)
        GL11.glColor4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(new ResourceLocation("deci", "textures/model/guns/scopes/" + name + ".png"));
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x * u, (y - h) * u, (z - h) * u, 1, 0);
        t.addVertexWithUV(x * u, (y - h) * u, (z + h) * u, 0, 0);
        t.addVertexWithUV(x * u, (y + h) * u, (z + h) * u, 0, 1);
        t.addVertexWithUV(x * u, (y + h) * u, (z - h) * u, 1, 1);
        t.draw();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
