package net.decimation.worldgen.devtest;

import net.decimation.worldgen.DevAutoTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Blocks;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;

/**
 * Leaving a world and opening it again at once (DeciRemnant Optimized's FastQuit, Fabric only; on Forge the leave
 * waits for the save as always). A gold block is set beside the arena, the client leaves, the same world opens one tick
 * later, and the block must be there: the open waited for the save the leave left running. Values: how long the leave
 * held the client, whether the server was still saving when the menu came, how long the open waited.
 * Fabric run: -Ddeciremnant.optimized.fastquit=true (dev tests keep FastQuit off unless asked). Closing the game right
 * after leaving: -Ddeciworldgen.fastquit.phase=exit, then a second launch with phase=check reads the marker.
 */
public class FastQuitTest extends DevTestMode
{
    public String name() { return "fastquit"; }

    private static final int MX = DevTestArena.X + 3, MY = DevTestArena.Y, MZ = DevTestArena.Z + 3;
    private static final boolean FASTQUIT = Boolean.getBoolean("deciremnant.optimized.fastquit");
    /** "" leave and open again; "exit" leave, then close the game at once; "check" (next launch) the marker is there. */
    private static final String PHASE = System.getProperty("deciworldgen.fastquit.phase", "");

    private int step, wait;
    private volatile Runnable onServer;
    private volatile boolean served;
    private volatile String marker = "not read";
    private IntegratedServer left;

    public boolean client(Minecraft mc)
    {
        DevAutoTest.keepScreen = true;
        wait++;
        switch (step)
        {
            case 0: // the marker, set on the server (phase check: only read, it was set by the exit run)
                if (wait == 1)
                {
                    if (PHASE.equals("check"))
                    {
                        next(2);
                        wait = 39;
                        break;
                    }
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        DevTestArena.build(w, DevTestUtil.player());
                        w.setBlock(MX, MY, MZ, Blocks.gold_block);
                    });
                }
                if (wait > 2 && served)
                {
                    next(1);
                }
                break;
            case 1: // leave, then open the same world on the next tick
                if (wait == 20)
                {
                    left = mc.getIntegratedServer();
                    long t0 = System.nanoTime();
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    long leaveMs = (System.nanoTime() - t0) / 1_000_000L;
                    boolean saving = !left.isServerStopped();
                    mc.displayGuiScreen(new GuiMainMenu());
                    DevTestResults.value(name(), "leave held the client", leaveMs + " ms");
                    DevTestResults.value(name(), "server still saving when the menu came", saving);
                    if (FASTQUIT)
                    {
                        DevTestResults.check(name(), "the leave does not wait for the save", leaveMs + " ms", leaveMs < 100, "under 100 ms");
                    }
                }
                if (wait == 21 && PHASE.equals("exit")) // close the game right after leaving: the save must still finish
                {
                    DevTestResults.value(name(), "closing the game right after leaving", "asked");
                    mc.shutdown();
                    return true;
                }
                if (wait == 21)
                {
                    long t0 = System.nanoTime();
                    mc.launchIntegratedServer(DevAutoTest.SAVE, DevAutoTest.SAVE, null);
                    DevTestResults.value(name(), "opening again waited", (System.nanoTime() - t0) / 1_000_000L + " ms");
                    DevTestResults.check(name(), "the old server had stopped before the world opened", String.valueOf(left.isServerStopped()),
                                         left.isServerStopped(), "true");
                }
                if (wait > 21 && mc.theWorld != null && mc.thePlayer != null && DevTestUtil.player() != null)
                {
                    next(2);
                }
                if (wait > 21 + 1200)
                {
                    DevTestResults.check(name(), "the world opens again", "not in 60 s", false, "the player back");
                    return end();
                }
                break;
            case 2: // the marker came back from disk
                if (wait == 40)
                {
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        marker = w.getBlock(MX, MY, MZ) == Blocks.gold_block ? "gold block" : String.valueOf(w.getBlock(MX, MY, MZ));
                        w.setBlockToAir(MX, MY, MZ);
                    });
                }
                if (wait > 40 && served)
                {
                    DevTestResults.check(name(), "the block set before leaving is there after opening again", marker,
                                         marker.equals("gold block"), "gold block");
                    return end();
                }
                break;
        }
        return true;
    }

    private boolean end()
    {
        DevAutoTest.keepScreen = false;
        return false;
    }

    private void next(int s)
    {
        step = s;
        wait = 0;
    }

    private void serve(Runnable r)
    {
        served = false;
        onServer = r;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r == null || DevTestUtil.player() == null)
        {
            return;
        }
        onServer = null;
        try
        {
            r.run();
        }
        catch (RuntimeException e)
        {
            DevTestResults.check(name(), "server step", e.toString(), false, "no exception");
        }
        served = true;
    }
}
