package net.decimation.fixes;

import net.minecraft.client.Minecraft;

/**
 * Decimation's food, water, blood and health percents centred on their icons (user, 23 Oktober 2026: "centre decimation
 * hud percents too", after the radiation percent got centred). IngameHud.render (deci.g.j b) starts each at 2w - 63 at
 * half scale, 17 half pixels into its 60 half pixel icon, which centres only a text as wide as "100%" ("40%" sat left);
 * tools/patches/PatchHudPercents.java hands the x of its six outlined texts to x(), which moves only the four percents to
 * their icon's middle, the start plus 13. Client only (IngameHud is). A frame: one string width for each of the four.
 */
public final class HudPercents
{
    /** For the dev test: per row (food, water, blood, health) the x a percent was drawn at and its width, half scale. */
    private static final int[] LAST_X = new int[4], LAST_W = new int[4];
    private static volatile long calls;

    private HudPercents()
    {
    }

    /** The x IngameHud draws an outlined text at: a percent's centred, anything else as Decimation had it. */
    public static int x(String text, int x, int y)
    {
        int row = row(y);
        if (row < 0 || text == null || !text.endsWith("%"))
        {
            return x;
        }
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
        int centred = centred(x, width);
        LAST_X[row] = centred;
        LAST_W[row] = width;
        calls++;
        return centred;
    }

    /** IngameHud's percent rows by their y: 0 food, 1 water, 2 blood, 3 health, -1 any other outlined text. */
    static int row(int y)
    {
        switch (y)
        {
            case 60: return 0;
            case 129: return 1;
            case 199: return 2;
            case 266: return 3;
            default: return -1;
        }
    }

    /** Where a text of this width starts to sit in the middle of the icon Decimation starts its percent at start in. */
    static int centred(int start, int width)
    {
        return start + 13 - width / 2;
    }

    /** For the dev test: a row's text middle less its icon's middle on a screen sw scaled pixels wide, half pixels. */
    public static double offset(int row, int sw)
    {
        return LAST_X[row] + LAST_W[row] / 2.0 - (2 * (sw - 40) + 30);
    }

    /** For the dev test: percents drawn through x() so far. */
    public static long calls()
    {
        return calls;
    }
}
