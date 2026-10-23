package net.decimation.optimized.fastquit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;

/**
 * FastQuit (in Fabulously Optimized) for 1.7.10. Leaving a singleplayer world no longer holds the game until the
 * integrated server has saved: the client goes on to the menu and the server saves on its own thread. Everything that
 * could touch that save or FML's state waits for it first: opening a world, joining a server, deleting a world, and
 * closing the game (after the window is gone, before the JVM exits), so a quit never cuts a save short (bug.md:
 * a cut save once left a level.dat without its item ids).
 */
public final class FastQuit
{
    /** Integrated servers left behind that may still be saving. */
    private static final List<MinecraftServer> SAVING = new ArrayList<MinecraftServer>();

    private FastQuit()
    {
    }

    /** The client leaves this server; it saves and stops on its own thread. */
    public static synchronized void leave(MinecraftServer server)
    {
        SAVING.add(server);
    }

    /** True while a server left behind is still saving. */
    public static synchronized boolean saving()
    {
        for (MinecraftServer s : SAVING)
        {
            if (!s.isStopped())
            {
                return true;
            }
        }
        return false;
    }

    /** Waits until every server left behind has saved and stopped (what vanilla did on the way out). */
    public static void await(String why)
    {
        long start = System.nanoTime();
        boolean waited = false;
        while (saving())
        {
            waited = true;
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
        synchronized (FastQuit.class)
        {
            SAVING.clear();
        }
        if (waited)
        {
            System.out.println("[deciremnant_optimized] fastquit: " + why + " waited " + (System.nanoTime() - start) / 1_000_000L + " ms for the last world's save");
        }
    }
}
