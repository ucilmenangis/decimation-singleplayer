package net.decimation.fixes;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.ForgeHooks;

/**
 * The INLINE HUD style (HudStyle) in Decimation's own look (user 22 Oktober 2026: "reimagine it ... look like the mod
 * theme ... NO VANILLA THEME", also in creative). Everything is drawn from Decimation's art: its brush stroke panel
 * (statsbg, the panel behind its stats list), its HUD icons (health, infected health, food, water, blood with their empty
 * forms, shield, humanity star, bottlecaps), its slot square, its red, outlined white text.
 * Over the hotbar, where Minecraft has its bars: a brush panel each side, health and blood left, food and water right
 * (oxygen over them under water), each its icon and a bar of ten segments in the icon's colour; a bar pulses below a
 * fifth, the blood icon shakes while bleeding, the health icon is Decimation's infected one while infected. Between them
 * and the hotbar humanity as a thin line (Decimation's location banner) red toward bandit, green toward hero, its star
 * where the player stands; the rank shows over the rows for 3 s after a change. The hotbar itself on a brush strip with
 * Decimation's slots, the selected one framed red. Armor (shield) left of the hotbar, bottlecaps right. The corner
 * (HudDoll) carries the player, the day, the conditions and the stats.
 * Drawn in the hotbar's place, in survival and creative alike; vanilla's health, armor, food, air and experience are
 * cancelled, and Forge's left / right heights are set over our rows so other mods' rows stack above them.
 */
@SideOnly(Side.CLIENT)
public final class InlineHud
{
    static final ResourceLocation PANEL = new ResourceLocation("deci", "textures/gui/ingame/statsbg.png");
    private static final ResourceLocation SLOT = new ResourceLocation("deci", "textures/gui/container/slot/slot_empty.png");
    private static final ResourceLocation HEALTH = icon("health"), HEALTH_INFECTED = icon("health_infected"),
        HEALTH_EMPTY = icon("health_empty"), FOOD = icon("food"), FOOD_EMPTY = icon("food_empty"), WATER = icon("water"),
        WATER_EMPTY = icon("water_empty"), BLOOD = icon("blood"), BLOOD_EMPTY = icon("blood_empty"), SHIELD = icon("shield"),
        STAR = icon("humanity"), CAP = icon("bottlecap"), GOLD_CAP = icon("bottlecapgold");
    /** Decimation's red (its menu buttons and warnings) and the bar colours, taken from its icons. */
    static final int RED = 0xFFD12A2A;
    private static final int C_HEALTH = 0xFFC9302C, C_INFECTED = 0xFF9C9A3C, C_RADIATION = 0xFF8FD13A, C_FOOD = 0xFF79AE36,
        C_WATER = 0xFF3C92D8, C_BLOOD = 0xFF9E1E22, C_AIR = 0xFFA6DDF2, C_HERO = 0xFF52C548, C_BANDIT = 0xFFD8392F;
    private static final RenderItem items = new RenderItem();
    private final Minecraft mc = Minecraft.getMinecraft();
    private int lastHumanity = -1, rankUntil;
    /** Texts built when their number changes, not every frame. */
    private long capsShown = -1, goldShown = -1;
    private int armorShown = -1;
    private String capsText, goldText, armorText;
    /** Dev test hud: our draw time, frames, GL errors seen after our drawing, modelview stack changed by the corner. */
    public static boolean probe;
    public static long drawNanos, frames, glErrors, stackChanged;

    private static ResourceLocation icon(String name)
    {
        return new ResourceLocation("deci", "textures/gui/ingame/icon_" + name + ".png");
    }

    @SubscribeEvent
    public void onPre(RenderGameOverlayEvent.Pre e)
    {
        if (!HudStyle.inline() || mc.thePlayer == null)
        {
            return;
        }
        RenderGameOverlayEvent.ElementType t = e.type;
        if (t == RenderGameOverlayEvent.ElementType.HEALTH || t == RenderGameOverlayEvent.ElementType.ARMOR
            || t == RenderGameOverlayEvent.ElementType.FOOD || t == RenderGameOverlayEvent.ElementType.AIR
            || t == RenderGameOverlayEvent.ElementType.EXPERIENCE)
        {
            e.setCanceled(true);
            return;
        }
        if (t != RenderGameOverlayEvent.ElementType.HOTBAR)
        {
            return;
        }
        e.setCanceled(true);
        Deci.Player p = Deci.player(mc.thePlayer);
        if (p == null)
        {
            return;
        }
        long t0 = System.nanoTime();
        int w = e.resolution.getScaledWidth(), h = e.resolution.getScaledHeight();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        hotbar(w, h);
        vitals(w, h, p);
        humanity(w, h, p);
        sides(w, h, p);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(Gui.icons);  // the elements after ours draw from vanilla's sheet
        GuiIngameForge.left_height = GuiIngameForge.right_height = 58;
        drawNanos += System.nanoTime() - t0;
    }

    @SubscribeEvent
    public void onPost(RenderGameOverlayEvent.Post e)
    {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL || !HudStyle.inline() || mc.thePlayer == null)
        {
            return;
        }
        Deci.Player p = Deci.player(mc.thePlayer);
        if (p == null)
        {
            return;
        }
        long t0 = System.nanoTime();
        int depth = probe ? GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) : 0;
        HudDoll.draw(e.resolution.getScaledWidth(), e.partialTicks, p, mc.gameSettings.keyBindPlayerList.getIsKeyPressed());
        drawNanos += System.nanoTime() - t0;
        frames++;
        if (probe)
        {
            stackChanged += GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) != depth ? 1 : 0;
            glErrors += GL11.glGetError() != GL11.GL_NO_ERROR ? 1 : 0;
        }
    }

    /** The hotbar on a brush strip with Decimation's slots; the selected slot framed in its red. */
    private void hotbar(int w, int h)
    {
        int x0 = w / 2 - 91, y0 = h - 22;
        panel(x0 - 8, y0 - 3, 198, 27, 0.85f);
        for (int i = 0; i < 9; i++)
        {
            image(SLOT, x0 + 2 + i * 20, y0 + 2, 18, 18, 0.9f);
        }
        int sel = mc.thePlayer.inventory.currentItem;
        int sx = x0 + 1 + sel * 20, sy = y0 + 1;
        Gui.drawRect(sx, sy, sx + 20, sy + 20, 0x30D12A2A);
        frame(sx, sy, 20, 20, RED);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        for (int i = 0; i < 9; i++)
        {
            ItemStack s = mc.thePlayer.inventory.mainInventory[i];
            if (s != null)
            {
                items.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), s, x0 + 3 + i * 20, y0 + 3);
                items.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), s, x0 + 3 + i * 20, y0 + 3);
            }
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /** Health and blood left, food and water right (oxygen over them under water), on a brush panel each side. */
    private void vitals(int w, int h, Deci.Player p)
    {
        EntityClientPlayerMP pl = mc.thePlayer;
        int left = w / 2 - 91, right = w / 2 + 3, row1 = h - 40, row2 = h - 52;
        boolean underwater = pl.getAir() < 300;
        panel(left - 6, row2 - 3, 98, 28, 0.75f);
        panel(right - 6, (underwater ? row2 - 12 : row2) - 3, 98, underwater ? 40 : 28, 0.75f);
        float health = pl.getMaxHealth() <= 0 ? 0 : pl.getHealth() / pl.getMaxHealth();
        boolean rads = p.irradiated() && !Deci.isGasMask(p.mask());
        vital(left, row1, health <= 0 ? HEALTH_EMPTY : p.infected() ? HEALTH_INFECTED : HEALTH, health,
              p.infected() ? C_INFECTED : rads ? C_RADIATION : C_HEALTH, false);
        vital(left, row2, p.blood() <= 0 ? BLOOD_EMPTY : BLOOD, p.blood() / 20.0f, C_BLOOD, p.bleeding());
        int food = pl.getFoodStats().getFoodLevel();
        vital(right, row1, food <= 0 ? FOOD_EMPTY : FOOD, food / 20.0f, C_FOOD, false);
        vital(right, row2, p.water() <= 0 ? WATER_EMPTY : WATER, p.water() / 20.0f, C_WATER, false);
        if (underwater)
        {
            vital(right, row2 - 12, WATER_EMPTY, Math.max(0, pl.getAir()) / 300.0f, C_AIR, false);
        }
    }

    /** One row: the icon (shaking when `shake`) and ten segments filled to `value` (0 .. 1), pulsing below a fifth. */
    private void vital(int x, int y, ResourceLocation icon, float value, int colour, boolean shake)
    {
        float clock = mc.ingameGUI.getUpdateCounter() + 0.0f;
        int dx = shake ? (int) Math.round(Math.sin(clock * 1.7)) : 0, dy = shake ? (int) Math.round(Math.sin(clock * 2.3)) : 0;
        image(icon, x + dx, y + dy, 11, 11, 1.0f);
        value = Math.max(0, Math.min(1, value));
        float alpha = value < 0.2f ? 0.55f + 0.45f * (float) Math.abs(Math.sin(clock * 0.35)) : 1.0f;
        int a = (int) (alpha * 255) << 24, fill = colour & 0xFFFFFF | a, dim = 0xA0101010;
        float segments = value * 10;
        for (int i = 0; i < 10; i++)
        {
            int sx = x + 15 + i * 7, sy = y + 3;
            Gui.drawRect(sx, sy, sx + 6, sy + 5, dim);
            float part = Math.min(1, segments - i);
            if (part > 0)
            {
                Gui.drawRect(sx, sy, sx + Math.max(1, Math.round(6 * part)), sy + 5, fill);
                Gui.drawRect(sx, sy, sx + Math.max(1, Math.round(6 * part)), sy + 1, 0x40FFFFFF); // a lit top edge
            }
        }
    }

    /** Humanity as a thin banner line over the hotbar: red half, green half, Decimation's star where the player stands. */
    private void humanity(int w, int h, Deci.Player p)
    {
        int value = Math.max(0, Math.min(100, p.humanity()));
        int x0 = w / 2 - 91, mid = w / 2, y = h - 26;
        Gui.drawRect(x0, y, mid, y + 1, 0x90D8392F);
        Gui.drawRect(mid, y, x0 + 182, y + 1, 0x9052C548);
        Gui.drawRect(mid, y - 1, mid + 1, y + 2, 0xC0FFFFFF);
        int at = x0 + value * 182 / 100;
        image(STAR, at - 5, y - 5, 11, 11, 1.0f);
        int now = mc.ingameGUI.getUpdateCounter();
        if (value != lastHumanity)
        {
            lastHumanity = value;
            rankUntil = now + 60;
        }
        if (now < rankUntil)
        {
            String rank = bold(p.humanityRankText());
            int code = rank.lastIndexOf('\u00a7') + 2;        // the name after its colour and bold codes, in capitals
            rank = rank.substring(0, code) + rank.substring(code).toUpperCase();
            outlined(mc.fontRenderer, rank, w / 2 - mc.fontRenderer.getStringWidth(rank) / 2, h - 72, 0xFFFFFF); // over the item name
        }
    }

    /** Armor (Decimation's shield) left of the hotbar, bottlecaps (its cap icons) right, each on a small brush panel. */
    private void sides(int w, int h, Deci.Player p)
    {
        FontRenderer f = mc.fontRenderer;
        int armor = ForgeHooks.getTotalArmorValue(mc.thePlayer);
        if (armor > 0)
        {
            if (armor != armorShown)
            {
                armorShown = armor;
                armorText = EnumChatFormatting.BOLD + String.valueOf(armor);
            }
            int tw = f.getStringWidth(armorText), x = w / 2 - 91 - 12 - tw - 13;
            panel(x - 5, h - 21, tw + 26, 20, 0.8f);
            image(SHIELD, x, h - 16, 10, 10, 1.0f);
            outlined(f, armorText, x + 13, h - 15, 0xFFFFFF);
        }
        long caps = p.bottlecaps(), gold = p.goldBottlecaps();
        if (caps != capsShown)
        {
            capsShown = caps;
            capsText = EnumChatFormatting.BOLD + String.format("%,d", caps);
        }
        int x = w / 2 + 91 + 12;
        int width = 13 + f.getStringWidth(capsText);
        if (gold > 0)
        {
            if (gold != goldShown)
            {
                goldShown = gold;
                goldText = EnumChatFormatting.BOLD + String.format("%,d", gold);
            }
            width += 6 + 13 + f.getStringWidth(goldText);
        }
        panel(x - 5, h - 21, width + 10, 20, 0.8f);
        image(CAP, x, h - 16, 11, 11, 1.0f);
        outlined(f, capsText, x + 13, h - 15, 0xFFE36B);
        if (gold > 0)
        {
            int gx = x + 13 + f.getStringWidth(capsText) + 6;
            image(GOLD_CAP, gx, h - 16, 11, 11, 1.0f);
            outlined(f, goldText, gx + 13, h - 15, 0xFFAA00);
        }
    }

    /** Decimation's brush stroke panel (statsbg) stretched over a box, as its stats list draws it. */
    static void panel(int x, int y, int w, int h, float alpha)
    {
        image(PANEL, x, y, w, h, alpha);
    }

    /** A whole texture drawn into a box. */
    static void image(ResourceLocation texture, int x, int y, int w, int h, float alpha)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1, 1, 1, alpha);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + h, 0, 0, 1);
        t.addVertexWithUV(x + w, y + h, 0, 1, 1);
        t.addVertexWithUV(x + w, y, 0, 1, 0);
        t.addVertexWithUV(x, y, 0, 0, 0);
        t.draw();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** A one pixel frame. */
    static void frame(int x, int y, int w, int h, int colour)
    {
        Gui.drawRect(x, y, x + w, y + 1, colour);
        Gui.drawRect(x, y + h - 1, x + w, y + h, colour);
        Gui.drawRect(x, y, x + 1, y + h, colour);
        Gui.drawRect(x + w - 1, y, x + w, y + h, colour);
    }

    /** Text with a black outline, as Decimation's HUD writes (RenderUtil.drawOutlined). */
    static void outlined(FontRenderer f, String text, int x, int y, int colour)
    {
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        String bold = text.contains(EnumChatFormatting.BOLD.toString()) ? EnumChatFormatting.BOLD + plain : plain;
        f.drawString(bold, x + 1, y, 0);
        f.drawString(bold, x - 1, y, 0);
        f.drawString(bold, x, y + 1, 0);
        f.drawString(bold, x, y - 1, 0);
        f.drawString(text, x, y, colour);
    }

    /** A rank text "(colour)Hero II" as "(colour)(bold)Hero II": a colour code after bold would end the bold. */
    static String bold(String coloured)
    {
        boolean code = coloured.length() > 1 && coloured.charAt(0) == '\u00a7';
        String colour = code ? coloured.substring(0, 2) : "";
        return colour + EnumChatFormatting.BOLD + (code ? coloured.substring(2) : coloured);
    }
}
