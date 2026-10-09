package net.decimation.worldgen.devtest;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

/**
 * World load census (user report 10 Oktober 2026: 1 fps; docs/performance.md): entities by type,
 * tile entities, loaded chunks and fps, at the start and again after -Pspan seconds (default 60),
 * so growth shows (a spawn loop, a pile up). The player is left where they are: this measures the
 * open world as it is. Results "start <type>", "end <type>", "growth entities".
 * -Pkill=Infected,Hulk,Bloater first removes every loaded entity of those registry names (cleaning
 * a test world after a pile up).
 */
public class CensusTest extends DevTestMode
{
    public String name() { return "census"; }

    private final int span = Integer.parseInt(System.getProperty("deciworldgen.autotest.span", "60")) * 20;
    private volatile int ticks;
    private volatile boolean done;
    private int startTotal;
    private final String kill = System.getProperty("deciworldgen.autotest.kill", "");

    private static Map<String, Integer> count(WorldServer w)
    {
        Map<String, Integer> m = new TreeMap<String, Integer>();
        for (Object o : w.loadedEntityList.toArray())
        {
            String k = EntityList.getEntityString((Entity) o);
            if (k == null)
            {
                k = ((Entity) o).getClass().getSimpleName();
            }
            Integer v = m.get(k);
            m.put(k, v == null ? 1 : v + 1);
        }
        return m;
    }

    private void record(String label, WorldServer w)
    {
        Map<String, Integer> m = count(w);
        int total = 0;
        for (Map.Entry<String, Integer> e : m.entrySet())
        {
            total += e.getValue();
            if (e.getValue() >= 5)
            {
                DevTestResults.value(name(), label + " " + e.getKey(), e.getValue());
            }
        }
        DevTestResults.value(name(), label + " entities", total);
        DevTestResults.value(name(), label + " tile entities", w.loadedTileEntityList.size());
        DevTestResults.value(name(), label + " loaded chunks", w.theChunkProviderServer.getLoadedChunkCount());
        if (label.equals("start"))
        {
            startTotal = total;
        }
        else
        {
            DevTestResults.check(name(), "growth entities", String.valueOf(total - startTotal),
                                 total - startTotal < 100, "under 100 in " + span / 20 + " s");
        }
    }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer != null && ticks % 200 == 100)
        {
            String dbg = mc.debug;
            DevTestResults.value(name(), "fps at " + ticks / 20 + " s", dbg.substring(0, dbg.indexOf(' ')));
        }
        return !done;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        WorldServer w = (WorldServer) p.worldObj;
        if (t == 10 && !kill.isEmpty())
        {
            int n = 0;
            java.util.List<String> names = java.util.Arrays.asList(kill.split(","));
            for (Object o : w.loadedEntityList.toArray())
            {
                if (names.contains(EntityList.getEntityString((Entity) o)))
                {
                    ((Entity) o).setDead();
                    n++;
                }
            }
            DevTestResults.value(name(), "removed", n);
        }
        if (t == 20)
        {
            record("start", w);
        }
        if (t == 20 + span)
        {
            record("end", w);
            done = true;
        }
    }
}
