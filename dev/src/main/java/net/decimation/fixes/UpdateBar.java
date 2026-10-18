package net.decimation.fixes;

/**
 * bug.md "Update screen bar stops short of full": Decimation's GuiUpdateProgress draws the bar as (bar / 100) * field
 * with bar = width / 4 * 2 in integers, so at a GUI width of 500 a full download fills 200 of 250 pixels. The field
 * UpdateProgress writes is scaled so the drawn length matches the real share; the percent the player reads is its own
 * text and stays true.
 */
final class UpdateBar
{
    static int field(int percent, int guiWidth)
    {
        int bar = guiWidth / 4 * 2, step = bar / 100;
        if (step <= 0)
        {
            return percent; // a bar under 100 pixels: Decimation draws nothing either way
        }
        return Math.round(percent * bar / (100f * step));
    }
}
