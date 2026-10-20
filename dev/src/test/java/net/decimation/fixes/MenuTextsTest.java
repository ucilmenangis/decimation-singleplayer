package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The version the menu and the window title show (docs/main_menu.md 2b): a dev or test build says so. */
class MenuTextsTest
{
    @Test
    void devAndTestBuildsSayDev()
    {
        assertEquals("0.65.0", MenuTexts.shown("0.65.0"));
        assertEquals("1.0.0-beta", MenuTexts.shown("1.0.0-beta"));
        assertEquals("0.64.8 dev", MenuTexts.shown("0.64.8-main.115+baf1c806dd"));
        assertEquals("0.65.0 dev", MenuTexts.shown("0.65.0-prerelease+14830b9e37-dirty"));
        assertEquals("1.21.10f", MenuTexts.shown("0"));                // Decimation's own when ours is unknown
        assertEquals("1.21.10f", MenuTexts.shown(null));
    }
}
