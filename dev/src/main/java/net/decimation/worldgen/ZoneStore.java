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

    /** Called by the structure generator after a structure is placed. */
    public static synchronized void add(net.decimation.mod.server.zones.a type,
                                        int x1, int y1, int z1, int x2, int y2, int z2)
    {
        if (file == null)
        {
            return; // no overworld loaded (should not happen during generation)
        }
        ObjectZone zone = new ObjectZone();
        zone.zoneType = type;
        zone.zoneX1 = x1;
        zone.zoneY1 = y1;
        zone.zoneZ1 = z1;
        zone.zoneX2 = x2;
        zone.zoneY2 = y2;
        zone.zoneZ2 = z2;
        zones.add(zone);
        live().zoneList.add(zone);
        dirty = true;
    }

    public static synchronized int size()
    {
        return zones.size();
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
        if (deci.aJ.b.aAc == null)
        {
            ObjectZoneList list = new ObjectZoneList();
            list.zoneList = new ArrayList<ObjectZone>();
            deci.aJ.b.aAc = list;
        }
        else if (deci.aJ.b.aAc.zoneList == null)
        {
            deci.aJ.b.aAc.zoneList = new ArrayList<ObjectZone>();
        }
        return deci.aJ.b.aAc;
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
        if (deci.aJ.b.aAc != null && deci.aJ.b.aAc.zoneList != null)
        {
            deci.aJ.b.aAc.zoneList.removeAll(zones);
        }
        zones.clear();
        file = null;
        dirty = false;
    }
}
