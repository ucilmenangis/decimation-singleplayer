package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** bug.md "Update screen bar stops short of full": Decimation draws (bar / 100, an integer) * percent. */
class UpdateBarTest
{
    /** What GuiUpdateProgress.drawScreen draws for a GUI width and the field's value. */
    private static int drawn(int guiWidth, int field)
    {
        int bar = guiWidth / 4 * 2;
        return bar / 100 * field;
    }

    @Test
    void fullDownloadFillsTheWholeBar()
    {
        for (int w : new int[] {427, 480, 500, 640, 683, 960})
        {
            int bar = w / 4 * 2;
            assertEquals(bar, drawn(w, UpdateBar.field(100, w)), 1.0, "GUI width " + w);
        }
    }

    @Test
    void halfwayIsHalfTheBar()
    {
        int w = 500, bar = w / 4 * 2;
        assertEquals(bar / 2.0, drawn(w, UpdateBar.field(50, w)), bar / 100);
    }

    @Test
    void narrowScreensKeepThePercent()
    {
        assertEquals(40, UpdateBar.field(40, 150)); // bar under 100 px: Decimation draws nothing either way
    }
}
