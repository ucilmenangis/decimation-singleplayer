package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

/**
 * The corner of the INLINE HUD style (user 22 Oktober 2026: "like xbox version in top corner, so only character 3rd
 * person view"): the player drawn small in the top right as Minecraft's console editions show it, turned three
 * quarters toward the screen's middle, live: the walk, the stance (crouch, prone), the head following the view, the gun
 * and gear in hand, armor and cape. Top right because Decimation's toasts and tips slide in at the top left. All on
 * Decimation's brush panel: "DAY N" under the figure, as Bedrock shows the days played under its figure; while the player
 * list key is held the vitals in numbers and the stats Decimation's own corner showed (kills, deaths, humanity, clan).
 * The range finder's distance and the target range run (Decimation's list showed them) appear there whenever they
 * apply. The conditions stand beside the panel as red edged plates, Decimation's notification look.
 */
@SideOnly(Side.CLIENT)
final class HudDoll
{
    /** Model scale (Minecraft's inventory figure is 30): about 43 GUI pixels from feet to head. */
    private static final float SCALE = 24.0f;
    /** The body turned three quarters toward the screen's middle (0 faces the viewer). */
    private static final float BODY_YAW = 50.0f;
    /** The player's turn put back after the draw (one array, not one a frame). */
    private static final float[] KEEP = new float[8];

    private HudDoll()
    {
    }

    static void draw(int w, float partial, Deci.Player p, boolean details)
    {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer f = mc.fontRenderer;
        build(mc, partial, p, details);
        int feet = 82, top = feet + 4;                  // the head clears vanilla's achievement / tutorial toasts (top 32)
        int boxW = Math.max(width, 52) + 14, boxH = feet - 30 + 4 + lines.size() * 10 + 6;
        InlineHud.panel(w - boxW - 6, 30, boxW + 6, boxH, 0.8f);
        figure(mc, w - 6 - boxW / 2, feet, partial);
        int y = top;
        for (String s : lines)
        {
            InlineHud.outlined(f, s, w - 10 - f.getStringWidth(s), y, 0xFFFFFF);
            y += 10;
        }
        y = 34;
        int edge = w - boxW - 6 - 3;                         // the conditions as Decimation's red edged plates, beside the panel
        for (int i = 0; i < chipCount; i++)
        {
            int cw = f.getStringWidth(chips[i]) + 9;
            Gui.drawRect(edge - cw, y, edge, y + 12, 0xD0141414);
            Gui.drawRect(edge - 2, y, edge, y + 12, InlineHud.RED);
            f.drawStringWithShadow(chips[i], edge - cw + 3, y + 2, 0xFFFFFF);
            y += 14;
        }
    }

    /** The real player through its own renderer (Decimation's player model and gear), posed in place, put back after. */
    private static void figure(Minecraft mc, int x, int feet, float partial)
    {
        EntityClientPlayerMP pl = mc.thePlayer;
        Deci.Player data = Deci.player(pl);
        if (data == null || pl.isInvisible())
        {
            return;
        }
        float body = pl.prevRenderYawOffset + (pl.renderYawOffset - pl.prevRenderYawOffset) * partial;
        float head = pl.prevRotationYawHead + (pl.rotationYawHead - pl.prevRotationYawHead) * partial;
        float pitch = pl.prevRotationPitch + (pl.rotationPitch - pl.prevRotationPitch) * partial;
        float turn = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(head - body), -60.0f, 60.0f);
        float[] keep = KEEP;
        keep[0] = pl.renderYawOffset;
        keep[1] = pl.prevRenderYawOffset;
        keep[2] = pl.rotationYaw;
        keep[3] = pl.prevRotationYaw;
        keep[4] = pl.rotationYawHead;
        keep[5] = pl.prevRotationYawHead;
        keep[6] = pl.rotationPitch;
        keep[7] = pl.prevRotationPitch;
        boolean pose = data.guiPose();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glTranslatef(x, feet, 50.0f);
        GL11.glScalef(-SCALE, SCALE, SCALE);
        GL11.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(135.0f, 0.0f, 1.0f, 0.0f);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135.0f, 0.0f, 1.0f, 0.0f);
        pl.renderYawOffset = pl.prevRenderYawOffset = BODY_YAW;
        pl.rotationYaw = pl.prevRotationYaw = BODY_YAW + turn;
        pl.rotationYawHead = pl.prevRotationYawHead = BODY_YAW + turn;
        pl.rotationPitch = pl.prevRotationPitch = pitch;
        data.setGuiPose(true);                        // head and body in place, not the first person body
        GL11.glTranslatef(0.0f, pl.yOffset, 0.0f);
        RenderManager.instance.playerViewY = 180.0f;
        try
        {
            RenderManager.instance.renderEntityWithPosYaw(pl, 0.0, 0.0, 0.0, 0.0f, partial);
        }
        finally
        {
            data.setGuiPose(pose);
            pl.renderYawOffset = keep[0];
            pl.prevRenderYawOffset = keep[1];
            pl.rotationYaw = keep[2];
            pl.prevRotationYaw = keep[3];
            pl.rotationYawHead = keep[4];
            pl.prevRotationYawHead = keep[5];
            pl.rotationPitch = keep[6];
            pl.prevRotationPitch = keep[7];
            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            if (!depth)
            {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    /** The box's lines and the condition plates, built once a client tick (or when the player list key changes). */
    private static final List<String> lines = new ArrayList<String>();
    private static final String[] chips = new String[4];
    private static int builtTick = -1, width, chipCount;
    private static boolean builtDetails;
    private static final String BLEEDING = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "BLEEDING",
        INFECTED = EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "INFECTED",
        BROKEN_LEG = EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "BROKEN LEG",
        IRRADIATED = EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + "\u2623 IRRADIATED",
        PROTECTED = EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "\u2623 PROTECTED";

    /**
     * The day under the figure; with `details` (player list key) the vitals in numbers and the stats Decimation's own list
     * showed; the range finder's distance and the target range run when they apply.
     */
    private static void build(Minecraft mc, float partial, Deci.Player p, boolean details)
    {
        int tick = mc.ingameGUI.getUpdateCounter();
        if (tick == builtTick && details == builtDetails)
        {
            return;
        }
        builtTick = tick;
        builtDetails = details;
        lines.clear();
        lines.add(EnumChatFormatting.BOLD + "DAY " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + Math.max(1, p.daysSurvived()));
        if (details)
        {
            EntityClientPlayerMP pl = mc.thePlayer;
            lines.add("Health: " + EnumChatFormatting.RED + Math.round(100 * pl.getHealth() / Math.max(1, pl.getMaxHealth())) + "%");
            lines.add("Food: " + EnumChatFormatting.GREEN + Math.max(0, pl.getFoodStats().getFoodLevel()) * 5 + "%");
            lines.add("Water: " + EnumChatFormatting.AQUA + Math.max(0, p.water()) * 5 + "%");
            lines.add("Blood: " + EnumChatFormatting.DARK_RED + Math.max(0, p.blood()) * 5 + "%");
            long[] stats = Deci.menuState();               // our local kills and deaths (fixes/LocalStats)
            lines.add("Human Kills: " + EnumChatFormatting.RED + Math.max(0, stats[1]));
            lines.add("Zombie Kills: " + EnumChatFormatting.DARK_GREEN + Math.max(0, stats[3]));
            lines.add("Deaths: " + EnumChatFormatting.DARK_RED + Math.max(0, stats[2]));
            lines.add("Humanity: " + p.humanityRankText());
            if (p.inClan())
            {
                lines.add("Clan: " + EnumChatFormatting.GRAY + p.clanName());
            }
        }
        if (mc.thePlayer.getHeldItem() != null && Deci.isRangeFinder(mc.thePlayer.getHeldItem().getItem()))
        {
            MovingObjectPosition hit = mc.thePlayer.rayTrace(1000.0, partial);
            double d = hit == null ? 1001 : Math.floor(mc.thePlayer.getDistance(hit.blockX, hit.blockY, hit.blockZ) * 100.0) / 100.0;
            lines.add("Range: " + (d > 1000.0 ? "1000+" : String.valueOf(d)));
        }
        if (p.targetTimer() > 0)
        {
            lines.add("High Score: " + p.targetBestScore());
            lines.add("Score: " + p.targetScore());
            lines.add("Timer: " + p.targetTimer() / 20);
        }
        width = 0;
        for (String s : lines)
        {
            width = Math.max(width, mc.fontRenderer.getStringWidth(s));
        }
        chipCount = 0;
        if (p.bleeding())
        {
            chips[chipCount++] = BLEEDING;
        }
        if (p.infected())
        {
            chips[chipCount++] = INFECTED;
        }
        if (p.brokenLeg())
        {
            chips[chipCount++] = BROKEN_LEG;
        }
        if (p.irradiated())
        {
            chips[chipCount++] = Deci.isGasMask(p.mask()) ? PROTECTED : IRRADIATED;
        }
    }
}
