package net.decimation.worldgen.devtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cpw.mods.fml.common.FMLLog;

/**
 * Live dev test mode (docs/roadmap.md "Live dev test mode"; `gradlew runClient
 * -Plive`, `tools/devtest.py --live`): the game stays open and takes test
 * runs over a local port, so a rerun needs no game launch (about 40 to 60 s
 * saved each time). Bound to 127.0.0.1 only (nobody else on the network can
 * reach it). One command per connection, one line:
 *
 *   ping                    -> "ready" once the world is loaded and no run is going, else "busy"
 *   run MODE [MODE ...] [key=value ...]
 *                           -> runs the modes in the open world (key=value sets
 *                              -Ddeciworldgen.autotest.key=value first), then sends
 *                              run/client/devtest/results.txt and a last line "END"
 *   quit                    -> closes the game
 *
 * The game thread takes commands from the queue (DevAutoTest.onClientTick);
 * this thread only waits for the run to finish.
 */
public final class DevTestLive
{
    public static final int PORT = 25599;
    public static final String PROPERTY = "deciworldgen.autotest.live";

    /** A run request: the modes, and the reply slot the game thread fills. */
    public static final class Command
    {
        public final String modes;
        public final boolean quit;
        final BlockingQueue<Boolean> done = new LinkedBlockingQueue<Boolean>();

        Command(String modes, boolean quit)
        {
            this.modes = modes;
            this.quit = quit;
        }

        /** Called by the game thread when the run is over. */
        public void finished()
        {
            done.offer(Boolean.TRUE);
        }
    }

    public static final BlockingQueue<Command> QUEUE = new LinkedBlockingQueue<Command>();
    /** Set by DevAutoTest: the world is loaded and no run is going. */
    public static volatile boolean idle;
    private static File results;

    private DevTestLive()
    {
    }

    public static boolean enabled()
    {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void start(File gameDir)
    {
        results = new File(gameDir, "devtest/results.txt");
        Thread t = new Thread(DevTestLive::serve, "deciworldgen live test port");
        t.setDaemon(true);
        t.start();
    }

    private static void serve()
    {
        try (ServerSocket server = new ServerSocket(PORT, 4, InetAddress.getByName("127.0.0.1")))
        {
            FMLLog.info("[deciworldgen] AUTOTEST live: listening on 127.0.0.1:%d", PORT);
            while (true)
            {
                try (Socket s = server.accept())
                {
                    handle(s);
                }
                catch (Exception e)
                {
                    FMLLog.info("[deciworldgen] AUTOTEST live: connection failed: %s", e);
                }
            }
        }
        catch (Exception e)
        {
            FMLLog.info("[deciworldgen] AUTOTEST live: cannot open port %d: %s", PORT, e);
        }
    }

    private static void handle(Socket s) throws Exception
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        String line = in.readLine();
        if (line == null)
        {
            return;
        }
        String[] words = line.trim().split("\\s+");
        if (words[0].equals("ping"))
        {
            out.println(idle ? "ready" : "busy");
            return;
        }
        if (words[0].equals("quit"))
        {
            QUEUE.offer(new Command("", true));
            out.println("bye");
            return;
        }
        if (!words[0].equals("run") || words.length < 2)
        {
            out.println("unknown command: " + line);
            out.println("END");
            return;
        }
        StringBuilder modes = new StringBuilder();
        for (int i = 1; i < words.length; i++)
        {
            int eq = words[i].indexOf('=');
            if (eq > 0)
            {
                System.setProperty("deciworldgen.autotest." + words[i].substring(0, eq), words[i].substring(eq + 1));
            }
            else
            {
                modes.append(modes.length() == 0 ? "" : ",").append(words[i]);
            }
        }
        Command c = new Command(modes.toString(), false);
        QUEUE.offer(c);
        if (c.done.poll(30, TimeUnit.MINUTES) == null)
        {
            out.println("TIMEOUT");
        }
        else if (results.isFile())
        {
            for (String r : Files.readAllLines(results.toPath(), StandardCharsets.UTF_8))
            {
                out.println(r);
            }
        }
        out.println("END");
    }
}
