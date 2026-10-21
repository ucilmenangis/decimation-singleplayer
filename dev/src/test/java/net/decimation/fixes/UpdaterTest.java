package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
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

    /**
     * GitHub answers 406 to Java's default Accept header ("text/html, image/gif, image/jpeg, *; q=.2" and any type at .2:
     * its "*; q=.2" is not a media range) on release downloads (found 22 Oktober 2026 with the published 0.66.0: every
     * updater from 0.64.7 on failed "Server returned HTTP response code: 406"). The updater names what it accepts.
     */
    @Test
    void downloadsSendAnAcceptHeaderGitHubTakes() throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", x -> {
            String accept = x.getRequestHeaders().getFirst("Accept");
            int code = accept == null || accept.contains("*; q=") ? 406 : 200;      // as github.com does
            x.sendResponseHeaders(code, -1);
            x.close();
        });
        server.start();
        try
        {
            HttpURLConnection c = Updater.open("http://127.0.0.1:" + server.getAddress().getPort() + "/Decimation.jar");
            assertEquals(200, c.getResponseCode());
        }
        finally
        {
            server.stop(0);
        }
    }
}
