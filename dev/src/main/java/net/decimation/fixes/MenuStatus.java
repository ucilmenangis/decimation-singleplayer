package net.decimation.fixes;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiOpenEvent;

/**
 * The Server Status page's rows from real sources (user, 20 Oktober 2026: "download server its take from github release
 * link, and for community server it take status from cloudflare, for the offical it take from server list, official mean
 * not from my server, its combined with other community dedicated server"; docs/main_menu.md 2g). Decimation's page
 * (GuiMenuServerStatus deci.i.h) pings each row's host with a plain socket; its first three rows sit on hosts that never
 * resolve (PatchMenuTexts), and while the page is open this class sets their state every tick:
 * - Global Server from the official server list (MenuServers): Coming soon while the list holds only rows that never
 *   resolve, else Online when any of its servers answers;
 * - Community Server from `community`, the relay check main's hosting code sets; Coming soon where nobody set it (the
 *   pre-release has no multiplayer);
 * - Download from the GitHub release link (a HEAD request on the release page, not the API).
 * The fourth row, our Website, keeps Decimation's own ping. The checks run on a thread when the page opens, at most once
 * in 30 s. Client only.
 */
public class MenuStatus
{
    /** In the order of Decimation's ServerPinger states (deci.l.a$a); SOON is its UNKNOWN, drawn "Coming soon". */
    public enum Ping { PINGING, OFFLINE, ONLINE, SOON }

    /** Main's Community Server check (the relay answers; fixes/hosting sets it). Null: Coming soon. */
    public static volatile Callable<Boolean> community;
    static final String RELEASES = Updater.DEFAULT_WEB + "/latest";
    private static final int TIMEOUT = 5000;
    private static volatile Ping global = Ping.PINGING, relay = Ping.PINGING, download = Ping.PINGING;
    private static long checked;

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e)
    {
        if (Deci.isStatusPage(e.gui))
        {
            refresh();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.END && Deci.isStatusPage(mc.currentScreen))
        {
            Deci.setStatus(mc.currentScreen, 0, global.ordinal());
            Deci.setStatus(mc.currentScreen, 1, relay.ordinal());
            Deci.setStatus(mc.currentScreen, 2, download.ordinal());
        }
    }

    /** New answers on a thread, unless the last ones are less than 30 s old. */
    static synchronized void refresh()
    {
        long now = System.currentTimeMillis();
        if (checked != 0 && now - checked < 30000)
        {
            return;
        }
        checked = now;
        global = relay = download = Ping.PINGING;
        Thread t = new Thread("deciworldgen server status")
        {
            public void run()
            {
                List<String> hosts = MenuServers.hosts();
                List<Boolean> up = new ArrayList<Boolean>();
                for (String h : hosts)
                {
                    up.add(!placeholder(h) && reachable(h));
                }
                global = global(hosts, up);
                Callable<Boolean> c = community;
                relay = c == null ? Ping.SOON : answers(c) ? Ping.ONLINE : Ping.OFFLINE;
                download = http(head(RELEASES));
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /** Global Server: Coming soon while every row is a placeholder (or there is none), else Online when one answers. */
    static Ping global(List<String> hosts, List<Boolean> up)
    {
        boolean real = false;
        for (int i = 0; i < hosts.size(); i++)
        {
            if (!placeholder(hosts.get(i)))
            {
                real = true;
                if (up.get(i))
                {
                    return Ping.ONLINE;
                }
            }
        }
        return real ? Ping.OFFLINE : Ping.SOON;
    }

    /** A web page's answer: a page or a redirect (releases/latest goes to the newest release) means it is there. */
    static Ping http(int code)
    {
        return code >= 200 && code < 400 ? Ping.ONLINE : Ping.OFFLINE;
    }

    /** A host that never resolves (RFC 6761): the rows of servers that do not exist yet. */
    static boolean placeholder(String hostPort)
    {
        String host = hostPort.contains(":") ? hostPort.substring(0, hostPort.lastIndexOf(':')) : hostPort;
        return host.endsWith(".invalid");
    }

    private static boolean reachable(String hostPort)
    {
        int colon = hostPort.lastIndexOf(':');
        Socket s = new Socket();
        try
        {
            s.connect(new InetSocketAddress(hostPort.substring(0, colon), Integer.parseInt(hostPort.substring(colon + 1))), TIMEOUT);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                s.close();
            }
            catch (Exception ignored)
            {
                // closed
            }
        }
    }

    private static boolean answers(Callable<Boolean> c)
    {
        try
        {
            return Boolean.TRUE.equals(c.call());
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /** The HTTP status of a HEAD request (redirects not followed), -1 without an answer. */
    private static int head(String url)
    {
        try
        {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("HEAD");
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(TIMEOUT);
            c.setReadTimeout(TIMEOUT);
            c.setRequestProperty("User-Agent", "deciworldgen-status");
            int code = c.getResponseCode();
            c.disconnect();
            return code;
        }
        catch (Exception e)
        {
            return -1;
        }
    }
}
