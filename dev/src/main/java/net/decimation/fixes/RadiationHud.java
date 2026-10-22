package net.decimation.fixes;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * The radiation dose on the HUD (user, 22 Oktober 2026: "radiation status below player status"; always shown; "you
 * better follow pixelated design on hud like health, water, food, blood"): a fifth status icon under Decimation's food,
 * water, blood and health (IngameHud.render, deci.g.j.b), at their x, 35 below health, its percent where theirs sit,
 * moved down with them by the stats list (Deci.hudIconsDown) and hidden with them (Deci.hudIconsHidden, the inventory
 * screen), the percent centred on the icon (textX). Where the column runs off the bottom of the screen (a long stats list
 * on a short GUI: 1080p at GUI scale auto is 270 high, and Decimation's own health icon gets cut off there too), the icon
 * sits left of health on its row. The
 * icon is ours (tools/hud_icons.py, Decimation's icon style). A frame: one picture, one outlined text; the text is
 * rebuilt only when the percent changes.
 */
public final class RadiationHud
{
    private static final ResourceLocation ICON = new ResourceLocation("deciworldgen", "textures/gui/hud_radiation.png");
    private final Minecraft mc = Minecraft.getMinecraft();
    private int textFor = -1;
    private String text = "";
    /** For the dev test: frames drawn, and their draw time in nanoseconds; where the icon went last (scaled pixels). */
    static long drawn, drawNanos;
    static volatile int lastX, lastY, lastHeight, lastTextX, lastTextW;

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e)
    {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL || mc.thePlayer == null || Deci.hudIconsHidden()
            || Deci.isInventoryScreen(mc.currentScreen))
        {
            return;
        }
        long start = System.nanoTime();
        int pct = RadiationDose.shown();
        if (pct != textFor)
        {
            textFor = pct;
            text = EnumChatFormatting.BOLD + "" + pct + "%";
        }
        int w = e.resolution.getScaledWidth(), h = e.resolution.getScaledHeight();
        int down = Deci.hudIconsDown(mc.thePlayer);
        boolean below = down + 155 + 30 <= h;
        int x = below ? w - 40 : w - 75, y = below ? 155 : 120;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, down, 0f);
        Deci.drawHudTexture(x, y, ICON, 30.0, 30.0);
        GL11.glPushMatrix();
        GL11.glScalef(0.5f, 0.5f, 0.5f);
        lastTextW = mc.fontRenderer.getStringWidth(text);
        lastTextX = textX(x, lastTextW);
        Deci.drawOutlined(text, lastTextX, below ? 336 : 266, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        lastX = x;
        lastY = y + down;
        lastHeight = h;
        drawn++;
        drawNanos += System.nanoTime() - start;
    }

    /**
     * Where the percent starts at half scale, centred on the icon at x (user, 23 Oktober 2026: "the text is not centered
     * on radiaton hud"): the 30 pixel icon spans 60 there, its middle at 2x + 30. Decimation starts its own percents at
     * 2x + 17, which centres only a text as wide as "100%".
     */
    static int textX(int x, int width)
    {
        return x * 2 + 30 - width / 2;
    }

    /** For the dev test: frames drawn so far and their average draw time in microseconds. */
    public static double averageMicros()
    {
        return drawn == 0 ? 0 : drawNanos / 1000.0 / drawn;
    }

    public static long framesDrawn()
    {
        return drawn;
    }

    /** For the dev test: "x,y of h" of the icon's last draw, scaled pixels. */
    public static String lastPlace()
    {
        return lastX + "," + lastY + " of " + lastHeight;
    }

    /** For the dev test: the percent text's middle less the icon's, half scale pixels (0 is centred). */
    public static double lastTextOffset()
    {
        return lastTextX + lastTextW / 2.0 - (lastX * 2 + 30);
    }

    /** For the dev test: the icon was drawn whole on the screen. */
    public static boolean lastOnScreen()
    {
        return lastY >= 0 && lastY + 30 <= lastHeight;
    }
}
