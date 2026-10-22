package net.decimation.worldgen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.mod.server.zones.ObjectZoneList;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Zones created by our structure generator, kept per world.
 *
 * Decimation's own zones live in one global {@code decimation_zones.json} in
 * the game directory and are only loaded by its ServerProxy, so in
 * singleplayer its zone list ({@code deci.aJ.b.aAc}) stays null and every
 * zone check answers "no". This store keeps the zones our generator places
 * in {@code <world>/deciworldgen_zones.json} instead (a zone belongs to the
 * world its structure is in) and merges them into Decimation's live list, so
 * the mod's own zone checks see them.
 *
 * Decimation replaces that list wholesale whenever it reloads its file (zone
 * commands, player login on a dedicated server), which would silently drop
 * ours, so a cheap server-tick check re-adds them.
 */
public class ZoneStore
{
    private static final String FILE = "deciworldgen_zones.json";
    /** Server ticks between "are our zones still merged in" checks. */
    private static final int CHECK_INTERVAL = 100;

    private static File file;
    private static final List<ObjectZone> zones = new ArrayList<ObjectZone>();
    private static boolean dirty;
    private static int tick;

    /** Called by the structure generator after a structure is placed; the zone, or null without a world. */
    public static synchronized ObjectZone add(ZoneKind type,
                                        int x1, int y1, int z1, int x2, int y2, int z2)
    {
        if (file == null)
        {
            return null; // no overworld loaded (should not happen during generation)
        }
        ObjectZone zone = new ObjectZone();
        zone.zoneType = net.decimation.fixes.Deci.zoneType(type);
        zone.zoneX1 = x1;
        zone.zoneY1 = y1;
        zone.zoneZ1 = z1;
        zone.zoneX2 = x2;
        zone.zoneY2 = y2;
        zone.zoneZ2 = z2;
        zones.add(zone);
        live().zoneList.add(zone);
        dirty = true;
        return zone;
    }

    /** True when a zone of ours of this kind contains the point (dev tests). */
    public static synchronized boolean has(ZoneKind kind, int x, int y, int z)
    {
        for (ObjectZone zone : zones)
        {
            if (net.decimation.fixes.Deci.isZone(zone, kind)
                && x >= Math.min(zone.zoneX1, zone.zoneX2) && x <= Math.max(zone.zoneX1, zone.zoneX2)
                && y >= Math.min(zone.zoneY1, zone.zoneY2) && y <= Math.max(zone.zoneY1, zone.zoneY2)
                && z >= Math.min(zone.zoneZ1, zone.zoneZ2) && z <= Math.max(zone.zoneZ1, zone.zoneZ2))
            {
                return true;
            }
        }
        return false;
    }

    /** Removes every zone of ours of this kind containing the point; how many (dev tests, commands). */
    public static synchronized int removeAt(ZoneKind kind, int x, int y, int z)
    {
        int n = 0;
        for (ObjectZone zone : new ArrayList<ObjectZone>(zones))
        {
            if (net.decimation.fixes.Deci.isZone(zone, kind)
                && x >= Math.min(zone.zoneX1, zone.zoneX2) && x <= Math.max(zone.zoneX1, zone.zoneX2)
                && y >= Math.min(zone.zoneY1, zone.zoneY2) && y <= Math.max(zone.zoneY1, zone.zoneY2)
                && z >= Math.min(zone.zoneZ1, zone.zoneZ2) && z <= Math.max(zone.zoneZ1, zone.zoneZ2))
            {
                remove(zone);
                n++;
            }
        }
        return n;
    }

    /** Removes the zone of ours of this kind with exactly this box (Areas); true when one went. */
    public static synchronized boolean removeBox(ZoneKind kind, int x1, int y1, int z1, int x2, int y2, int z2)
    {
        for (ObjectZone zone : new ArrayList<ObjectZone>(zones))
        {
            if (net.decimation.fixes.Deci.isZone(zone, kind) && zone.zoneX1 == x1 && zone.zoneY1 == y1 && zone.zoneZ1 == z1
                && zone.zoneX2 == x2 && zone.zoneY2 == y2 && zone.zoneZ2 == z2)
            {
                return remove(zone);
            }
        }
        return false;
    }

    /** Takes a zone of ours out of this world and the live list (dev tests; later a fallen safezone). */
    public static synchronized boolean remove(ObjectZone zone)
    {
        ObjectZoneList live = net.decimation.fixes.Deci.zoneList();
        if (live != null && live.zoneList != null)
        {
            live.zoneList.remove(zone);
        }
        boolean had = zones.remove(zone);
        dirty |= had;
        return had;
    }

    public static synchronized int size()
    {
        return zones.size();
    }

    /** Every zone of ours of this kind as {x1, y1, z1, x2, y2, z2} (city/WastelandRadiation at world load, dev tests). */
    public static synchronized List<int[]> boxes(ZoneKind kind)
    {
        List<int[]> out = new ArrayList<int[]>();
        for (ObjectZone zone : zones)
        {
            if (net.decimation.fixes.Deci.isZone(zone, kind))
            {
                out.add(new int[] {zone.zoneX1, zone.zoneY1, zone.zoneZ1, zone.zoneX2, zone.zoneY2, zone.zoneZ2});
            }
        }
        return out;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.world;
        if (world.isRemote || world.provider == null || world.provider.dimensionId != 0)
        {
            return;
        }
        synchronized (ZoneStore.class)
        {
            detach();
            file = new File(world.getSaveHandler().getWorldDirectory(), FILE);
            if (file.isFile())
            {
                try
                {
                    Reader in = new FileReader(file);
                    ObjectZoneList saved = new Gson().fromJson(in, ObjectZoneList.class);
                    in.close();
                    if (saved != null && saved.zoneList != null)
                    {
                        zones.addAll(saved.zoneList);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.info("[%s] could not read %s: %s", DecimationWorldGen.MODID, file, e);
                }
            }
            live().zoneList.addAll(zones);
            dirty = false;
            FMLLog.info("[%s] %d generated zone(s) loaded for this world",
                        DecimationWorldGen.MODID, zones.size());
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        if (!event.world.isRemote && event.world.provider != null
            && event.world.provider.dimensionId == 0)
        {
            save();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (!event.world.isRemote && event.world.provider != null
            && event.world.provider.dimensionId == 0)
        {
            synchronized (ZoneStore.class)
            {
                save();
                detach();
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || ++tick < CHECK_INTERVAL)
        {
            return;
        }
        tick = 0;
        synchronized (ZoneStore.class)
        {
            if (zones.isEmpty())
            {
                return;
            }
            ObjectZoneList list = live();
            if (!list.zoneList.contains(zones.get(0)))
            {
                list.zoneList.addAll(zones); // Decimation reloaded its file
            }
        }
    }

    /** Decimation's live zone list, created if it never got loaded. */
    private static ObjectZoneList live()
    {
        ObjectZoneList live = net.decimation.fixes.Deci.zoneList();
        if (live == null)
        {
            live = new ObjectZoneList();
            live.zoneList = new ArrayList<ObjectZone>();
            net.decimation.fixes.Deci.setZoneList(live);
        }
        else if (live.zoneList == null)
        {
            live.zoneList = new ArrayList<ObjectZone>();
        }
        return live;
    }

    private static synchronized void save()
    {
        if (!dirty || file == null)
        {
            return;
        }
        try
        {
            ObjectZoneList out = new ObjectZoneList();
            out.zoneList = new ArrayList<ObjectZone>(zones);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer w = new FileWriter(file);
            w.write(gson.toJson(out));
            w.close();
            dirty = false;
        }
        catch (Exception e)
        {
            FMLLog.info("[%s] could not write %s: %s", DecimationWorldGen.MODID, file, e);
        }
    }

    /** Forget the current world's zones (and pull them out of the live list). */
    private static void detach()
    {
        ObjectZoneList live = net.decimation.fixes.Deci.zoneList();
        if (live != null && live.zoneList != null)
        {
            live.zoneList.removeAll(zones);
        }
        zones.clear();
        file = null;
        dirty = false;
    }
}
