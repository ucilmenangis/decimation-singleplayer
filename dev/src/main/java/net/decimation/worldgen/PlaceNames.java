package net.decimation.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.decimation.worldgen.city.CityNames;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * Place names through Decimation's own turf display (docs/zones.md "Place names", user choice 12
 * Oktober 2026): walking into a city district shows Decimation's "ENTERING LOCATION" banner with
 * "Ashford: Downtown - 7", the chat line "Now entering ..." and its sound; out of every place and
 * safezone Decimation says "Now entering the Wilderness". Military bases are named places too
 * ("FOB Viper", controlled by Military).
 *
 * Every 2 seconds per player: the districts around the player (CityNames, from the seed), the named
 * places stored in StructureData (bases), and Decimation's own clan turfs, sent as the client's turf
 * list (Deci.sendTurfList) when it changed. Safezone boxes are cut out of the district boxes: inside
 * both, Decimation's HUD would switch between the two messages every frame.
 */
public class PlaceNames
{
    private static final int INTERVAL = 40, RADIUS_DISTRICTS = 3, RADIUS_PLACES = 600;
    private static final Map<UUID, String> sent = new HashMap<UUID, String>(); // one handler per game (DecimationWorldGen)
    private int tick;

    /** Stores a named plan's box (Slices, first slice); y covers the whole column. */
    static void store(net.minecraft.world.World world, String id, NamedPlace p, int x0, int z0, int x1, int z1)
    {
        StructureData.get(world).setPlace(id, entry(x0, z0, x1, z1, p.placeOwner(), p.placeName()));
        cpw.mods.fml.common.FMLLog.info("[deciworldgen] place %s (%s) at %d,%d", p.placeName(), p.placeOwner(), x0, z0);
    }

    /** One turf list entry; spaces as underscores (the client strips spaces), no "=" or "/" in names. */
    static String entry(int x0, int z0, int x1, int z1, String owner, String name)
    {
        return x0 + "/0/" + z0 + "=" + x1 + "/255/" + z1 + "=" + clean(owner) + "=" + clean(name);
    }

    private static String clean(String s)
    {
        return s.replace("=", "").replace("/", "").replace(' ', '_');
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || ++tick % INTERVAL != 0)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer world = server == null ? null : server.worldServerForDimension(0);
        if (world == null)
        {
            return;
        }
        List<int[]> safe = Deci.zoneBoxes(ZoneKind.SAFEZONE);
        for (Object o : world.playerEntities)
        {
            EntityPlayerMP p = (EntityPlayerMP) o;
            ArrayList<String> list = list(world, (int) p.posX, (int) p.posZ, safe);
            String key = String.join("|", list);
            if (!key.equals(sent.get(p.getUniqueID())))
            {
                sent.put(p.getUniqueID(), key);
                Deci.sendTurfList(p, list);
            }
        }
    }

    /**
     * Decimation clears the client's turf list when it disconnects: forget what this player was sent, so the list goes
     * out again after a rejoin (found in the player hosting spike, bug.md "Place names gone after a rejoin").
     */
    @SubscribeEvent
    public void onLogout(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event)
    {
        sent.remove(event.player.getUniqueID());
    }

    /** Entries last sent to this player (dev test lanhost compares the joiner's client list with it); -1 never sent. */
    public static int sentCount(java.util.UUID player)
    {
        String key = sent.get(player);
        return key == null ? -1 : key.isEmpty() ? 0 : key.split("\\|").length;
    }

    static ArrayList<String> list(WorldServer world, int px, int pz, List<int[]> safe)
    {
        ArrayList<String> out = new ArrayList<String>(Deci.decimationTurfs());
        long seed = world.getSeed();
        int pdx = Math.floorDiv(px, 128), pdz = Math.floorDiv(pz, 128);
        for (int dx = pdx - RADIUS_DISTRICTS; dx <= pdx + RADIUS_DISTRICTS; dx++)
        {
            for (int dz = pdz - RADIUS_DISTRICTS; dz <= pdz + RADIUS_DISTRICTS; dz++)
            {
                String[] d = CityNames.district(seed, dx, dz);
                if (d == null)
                {
                    continue;
                }
                for (int[] r : cut(new int[] {dx * 128, dz * 128, dx * 128 + 127, dz * 128 + 127}, safe))
                {
                    out.add(entry(r[0], r[1], r[2], r[3], d[1], d[0]));
                }
            }
        }
        for (String place : StructureData.get(world).places())
        {
            String[] f = place.split("=")[0].split("/");
            int x = Integer.parseInt(f[0]), z = Integer.parseInt(f[2]);
            if (Math.abs(x - px) < RADIUS_PLACES && Math.abs(z - pz) < RADIUS_PLACES)
            {
                out.add(place);
            }
        }
        return out;
    }

    /** A box {x1, z1, x2, z2} minus every safezone box it overlaps: up to 4 pieces per cut. */
    static List<int[]> cut(int[] box, List<int[]> holes)
    {
        List<int[]> parts = new ArrayList<int[]>();
        parts.add(box);
        for (int[] h : holes)
        {
            List<int[]> next = new ArrayList<int[]>();
            for (int[] b : parts)
            {
                if (h[0] > b[2] || h[2] < b[0] || h[1] > b[3] || h[3] < b[1])
                {
                    next.add(b);
                    continue;
                }
                if (h[0] > b[0]) next.add(new int[] {b[0], b[1], h[0] - 1, b[3]});                  // west strip
                if (h[2] < b[2]) next.add(new int[] {h[2] + 1, b[1], b[2], b[3]});                  // east strip
                int x0 = Math.max(b[0], h[0]), x1 = Math.min(b[2], h[2]);
                if (h[1] > b[1]) next.add(new int[] {x0, b[1], x1, h[1] - 1});                      // north
                if (h[3] < b[3]) next.add(new int[] {x0, h[3] + 1, x1, b[3]});                      // south
            }
            parts = next;
        }
        return parts;
    }
}
