package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** The Server Status page's rows from real sources (docs/main_menu.md 2g). */
class MenuStatusTest
{
    @Test
    void globalServerFollowsTheOfficialList()
    {
        // today's list: Bukovka and Chernarus on hosts that never resolve
        assertEquals(MenuStatus.Ping.SOON, MenuStatus.global(MenuServers.hosts(), Arrays.asList(false, false)));
        assertEquals(MenuStatus.Ping.SOON, MenuStatus.global(Collections.<String>emptyList(), Collections.<Boolean>emptyList()));
        assertEquals(MenuStatus.Ping.ONLINE, MenuStatus.global(Arrays.asList("a.example.org:25565", "b.example.org:25565"),
                                                               Arrays.asList(false, true)));
        assertEquals(MenuStatus.Ping.OFFLINE, MenuStatus.global(Arrays.asList("a.example.org:25565"), Arrays.asList(false)));
    }

    @Test
    void downloadAnswersLikeAWebPage()
    {
        assertEquals(MenuStatus.Ping.ONLINE, MenuStatus.http(302));      // releases/latest redirects to the release
        assertEquals(MenuStatus.Ping.ONLINE, MenuStatus.http(200));
        assertEquals(MenuStatus.Ping.OFFLINE, MenuStatus.http(404));
        assertEquals(MenuStatus.Ping.OFFLINE, MenuStatus.http(-1));       // no answer
    }
}
