package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * The world's day and its daily events (docs/systems_plan.md 2.1), for every system that runs on
 * days: zombie growth and unlocks, horde nights, raids, claim decay and revenue, infection stages.
 * Systems subscribe instead of keeping their own timers.
 *
 * The day is a saved counter (Data, <world>/data/deciworldgen_clock.dat), not world time / 24000:
 * Decimation's DayLengthHandler (deci/an/p) puts the world time back to 0 each time it passes 24000
 * (bug.md "WorldClock day never advances"). Day 1 starts at world creation; a world made before the
 * counter starts at its total ticks / 24000 + 1. Events fire when the time crosses their point: DAY at
 * 0 (the morning a new day number starts), DUSK at 12000 (sunset), NIGHT at 13800 (dark enough for
 * monsters), DAWN at 22200 (sky lightens). A jump forward (sleeping, /time add) fires each point it
 * crosses once and counts every midnight it passes; a jump backward (Decimation's reset, /time set)
 * fires nothing and counts nothing.
 */
public class WorldClock
{
    public enum Event
    {
        DAY(0), DUSK(12000), NIGHT(13800), DAWN(22200);

        public final int time;

        Event(int time)
        {
            this.time = time;
        }
    }

    public interface Listener
    {
        /** On the server thread; `day` is the day the event belongs to. */
        void onClock(World world, Event event, int day);
    }

    /** The overworld's day counter, saved with the world. */
    public static class Data extends WorldSavedData
    {
        public static final String NAME = "deciworldgen_clock";

        /** Midnights passed: the day is days + 1. */
        int days;
        /** Raw world time our last tick saw, -1 before the first. */
        long last = -1;

        public Data(String name)
        {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag)
        {
            days = tag.getInteger("days");
            last = tag.getLong("last");
        }

        @Override
        public void writeToNBT(NBTTagCompound tag)
        {
            tag.setInteger("days", days);
            tag.setLong("last", last);
        }
    }

    private static final List<Listener> listeners = new ArrayList<Listener>();

    public static void listen(Listener l)
    {
        synchronized (listeners)
        {
            listeners.add(l);
        }
    }

    public static void unlisten(Listener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    /** The counter of a server world (all dimensions share the overworld's), null on a client world. */
    public static Data data(World world)
    {
        if (world == null || world.isRemote || world.mapStorage == null)
        {
            return null;
        }
        Data d = (Data) world.mapStorage.loadData(Data.class, Data.NAME);
        if (d == null)
        {
            d = new Data(Data.NAME);
            d.days = (int) (world.getTotalWorldTime() / 24000L);
            world.mapStorage.setData(Data.NAME, d);
            d.markDirty();
        }
        return d;
    }

    /** Day number of a world, 1 on the first day; 0 on a client world. */
    public static int day(World world)
    {
        Data d = data(world);
        return d == null ? 0 : d.days + 1;
    }

    /** Sets a server world's day number (tests, admins): the counter as if day - 1 midnights had passed. */
    public static void setDay(World world, int day)
    {
        Data d = data(world);
        if (d != null && day >= 1)
        {
            d.days = day - 1;
            d.markDirty();
        }
    }

    /** Day number of the overworld, 0 without a server. */
    public static int day()
    {
        MinecraftServer s = MinecraftServer.getServer();
        return s == null || s.worldServers == null || s.worldServers.length == 0 ? 0 : day(s.worldServers[0]);
    }

    /** True between DUSK and DAWN of the overworld time. */
    public static boolean isNight(World world)
    {
        long t = world.getWorldTime() % 24000L;
        return t >= Event.DUSK.time && t < Event.DAWN.time;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.worldServers == null || s.worldServers.length == 0)
        {
            return;
        }
        World w = s.worldServers[0];
        Data d = data(w);
        long now = w.getWorldTime();
        if (d.last >= 0 && now > d.last)
        {
            int base = d.days;
            d.days += (int) (now / 24000L - d.last / 24000L);
            fire(w, d.last, now, base - (int) (d.last / 24000L));
        }
        // first tick of a new counter, or time went back (Decimation's reset past 24000, /time set): no events
        d.last = now;
        d.markDirty();
    }

    /**
     * Fires every event point in (from, to] in time order, each event type at most once, with the day
     * numbered from the raw time (point / 24000 + 1): the jump checks of the core dev test.
     */
    public static void fire(World w, long from, long to)
    {
        fire(w, from, to, 0);
    }

    /** As fire(w, from, to), the day of a point being point / 24000 + 1 + offset. */
    private static void fire(World w, long from, long to, int offset)
    {
        List<long[]> due = new ArrayList<long[]>();
        for (Event ev : Event.values())
        {
            // the latest time <= to that is this event's point, if it lies after `from`
            long p = (to - ev.time) / 24000L * 24000L + ev.time;
            if (to >= ev.time && p > from)
            {
                due.add(new long[] {p, ev.ordinal()});
            }
        }
        java.util.Collections.sort(due, new java.util.Comparator<long[]>()
        {
            public int compare(long[] a, long[] b)
            {
                return Long.compare(a[0], b[0]);
            }
        });
        List<Listener> copy;
        synchronized (listeners)
        {
            copy = new ArrayList<Listener>(listeners);
        }
        for (long[] d : due)
        {
            for (Listener l : copy)
            {
                l.onClock(w, Event.values()[(int) d[1]], (int) (d[0] / 24000L) + 1 + offset);
            }
        }
    }
}
