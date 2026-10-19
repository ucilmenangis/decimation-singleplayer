package net.decimation.worldgen.devtest;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * bug.md "FPS falls minute by minute": a helper thread that never sleeps keeps a CPU core at full load for the whole
 * session (Decimation's boot intro thread, GuiIntroJumpscare deci.i.d, spun at 30 Hz forever; its SmoothSwingThread did
 * the same before PatchSwing). On a laptop that is heat, and the CPU slows down after a few minutes. Over 5 seconds each
 * thread's CPU time is read; no thread other than the game's client and server threads may use half a core or more.
 */
public class ThreadsTest extends DevTestMode
{
    public String name() { return "threads"; }

    private static final long SPAN_MS = 5000;
    private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    private Map<Long, Long> before;
    private long t0;

    public boolean client(Minecraft mc)
    {
        if (before == null)
        {
            before = cpu();
            t0 = System.nanoTime();
            return true;
        }
        long span = System.nanoTime() - t0;
        if (span < SPAN_MS * 1000000L)
        {
            return true;
        }
        Map<Long, Long> after = cpu();
        Map<Long, Thread> threads = new HashMap<Long, Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet())
        {
            threads.put(t.getId(), t);
        }
        List<String> busy = new ArrayList<String>();
        for (Map.Entry<Long, Long> e : after.entrySet())
        {
            Long b = before.get(e.getKey());
            if (b == null || b < 0 || e.getValue() < 0)
            {
                continue;
            }
            double share = (e.getValue() - b) / (double) span; // 1.0 = one whole core
            Thread t = threads.get(e.getKey());
            String n = t == null ? "id " + e.getKey() : t.getName();
            if (share >= 0.05)
            {
                DevTestResults.value(name(), "cpu " + n, String.format("%.0f%% of a core%s", share * 100, top(t)));
            }
            if (share >= 0.5 && !n.equals("Client thread") && !n.equals("Server thread"))
            {
                busy.add(n + " " + String.format("%.0f%%", share * 100) + top(t));
            }
        }
        DevTestResults.check(name(), "helper threads using half a core or more", busy.isEmpty() ? "none" : busy.toString(),
                             busy.isEmpty(), "none (only the client and server threads work that hard)");
        return false;
    }

    private Map<Long, Long> cpu()
    {
        Map<Long, Long> m = new HashMap<Long, Long>();
        for (long id : bean.getAllThreadIds())
        {
            m.put(id, bean.getThreadCpuTime(id));
        }
        return m;
    }

    private static String top(Thread t)
    {
        if (t == null)
        {
            return "";
        }
        StackTraceElement[] s = t.getStackTrace();
        return s.length == 0 ? "" : " at " + s[0].getClassName() + "." + s[0].getMethodName();
    }
}
