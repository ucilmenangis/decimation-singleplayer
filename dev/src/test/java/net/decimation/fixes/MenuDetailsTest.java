package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The news panels' detail popups (docs/main_menu.md 2h): content, slides, release lines, and a panel that fits. */
class MenuDetailsTest
{
    @Test
    void popupsHoldTheirSlides()
    {
        assertEquals("detail_coming1", MenuDetails.get(MenuDetails.COMING).slides[1].picture);
        assertNull(MenuDetails.get("multiplayer"));                                 // main only (1.0.0)
        assertEquals(2, MenuDetails.get(MenuDetails.COMING).slides.length);         // fewer promises: what is being built
        assertEquals(0, MenuDetails.get(MenuDetails.UPDATES).slides.length);        // the release list instead
        assertNull(MenuDetails.get("website"));                                     // the website panel opens the site
    }

    @Test
    void wordsFitAndHaveNoDashes()
    {
        for (String id : new String[] {MenuDetails.COMING})
        {
            MenuDetails.Detail d = MenuDetails.get(id);
            for (MenuDetails.Slide s : d.slides)
            {
                for (String l : s.lines)
                {
                    assertTrue(l.length() <= MenuDetails.LINE, l);
                    assertFalse(l.contains("-") || l.contains("–") || l.contains("—"), l);
                }
                assertFalse(s.heading.contains("-"), s.heading);
            }
        }
    }

    @Test
    void slidesWrapBothWays()
    {
        assertEquals(2, MenuDetails.step(0, 3, -1));
        assertEquals(0, MenuDetails.step(2, 3, 1));
        assertEquals(0, MenuDetails.step(0, 0, 1));
    }

    @Test
    void releaseLinesReadLikeText()
    {
        assertEquals("DeciRemnant 0.65.0", MenuDetails.summary("## DeciRemnant 0.65.0\n- fixes", 46));
        assertEquals("Faster refuges, see notes", MenuDetails.summary("\n* **Faster** refuges, see [notes](https://x.y)", 46));
        assertEquals("abcdefg...", MenuDetails.summary("abcdefghijklmnop", 10));
        assertEquals("", MenuDetails.summary(null, 10));
        assertEquals("20 Oct 2026", MenuDetails.date("2026-10-20T03:55:00Z"));
        assertEquals("", MenuDetails.date("soon"));
    }

    @Test
    void panelFitsEveryAutoGuiSize()
    {
        // auto GUI scale keeps at least 320 x 240 units: 854 x 480 at scale 2, 1440 x 810 at 3, 1920 x 1080 at 4
        int[][] sizes = {{427, 240}, {480, 270}, {640, 360}, {960, 540}, {320, 240}};
        for (int[] s : sizes)
        {
            for (boolean pictures : new boolean[] {true, false})
            {
                int[] l = MenuDetails.layout(s[0], s[1], pictures);
                String at = s[0] + " x " + s[1] + (pictures ? " pictures" : " list");
                assertTrue(l[0] >= 0 && l[0] + l[2] <= s[0], at);
                assertTrue(l[1] >= 8 && l[1] + l[3] <= s[1] - 8, at);
                assertTrue(l[4] <= l[2] - 20 && l[5] * 16 / 9 <= l[4] + 1, at);
            }
        }
    }
}
