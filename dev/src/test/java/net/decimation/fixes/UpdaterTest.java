package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Which builds look for updates (user, 20 Oktober 2026): release builds only, never a dev or test build. */
class UpdaterTest
{
    @Test
    void onlyReleaseBuildsLookForUpdates()
    {
        assertTrue(Updater.release("0.65.0"));
        assertTrue(Updater.release("1.0.0-beta"));                            // a clean tag with a name
        assertFalse(Updater.release("0.64.8-main.115+baf1c806dd"));           // main, commits after its last tag
        assertFalse(Updater.release("0.64.8-prerelease.3+0c149f892b"));       // prerelease between releases
        assertFalse(Updater.release("0.65.0-prerelease+14830b9e37-dirty"));   // a tag with uncommitted changes
        assertFalse(Updater.release("0"));                                    // version unknown
        assertFalse(Updater.release(""));
        assertFalse(Updater.release(null));
    }

    @Test
    void namesTheLineOfADevBuild()
    {
        assertEquals("main", Updater.line("0.64.8-main.115+baf1c806dd"));
        assertEquals("prerelease", Updater.line("0.65.0-prerelease+14830b9e37-dirty"));
        assertEquals("", Updater.line("0.65.0"));
        assertEquals("", Updater.line(null));
    }
}
