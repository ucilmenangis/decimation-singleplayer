package net.decimation.worldgen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;

/**
 * The people of the safezone sites (docs/safezone.md): traders, guards, civilians and armed
 * survivors. A site's plan hands over its posts when its first slice is written (Slices, through
 * {@link Residents}); they are kept per world in {@code <world>/deciworldgen_safezones.json}.
 *
 * NPCs are not spawned during generation. Every 2 s, for each site with a player within 112 blocks
 * and all its chunks loaded, the keeper counts the living NPCs tagged with the site and post
 * (entity data deciwgSite / deciwgPost) and spawns the missing ones: all at the first visit, later
 * a dead one again after 5 minutes, only while no player is within 40 blocks of its post.
 */
public class SafezoneSites
{
    private static final String FILE = "deciworldgen_safezones.json";
    public static final String SITE_TAG = "deciwgSite", POST_TAG = "deciwgPost";
    static final int EVERY = 40, VISIT = 112, UNSEEN = 40, RESPAWN = 6000;

    /** One resident post: kind ("guard", "trader:TraderGuns", "civilian", "survivor"), position, yaw. */
    public static final class Post
    {
        public String kind;
        public double x, y, z;
        public float yaw;

        public Post(String kind, double x, double y, double z, float yaw)
        {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    public static final class Site
    {
        public String id;
        public int x0, y0, z0, x1, y1, z1;
        public boolean filled;
        /** Box edge to wall line (Residents.wallInset); 0 in sites saved before v0.48 = 8 (camps). */
        public int inset;
        public List<Post> posts = new ArrayList<Post>();
    }

    private static final class SiteList
    {
        List<Site> sites = new ArrayList<Site>();
    }

    /** Survivor camps in this world: worlds created since v0.47 (StructureData CAMPS_KEY). */
    private static volatile boolean camps;
    static final String CAMPS_KEY = "!safezoneCamps";
    private static File file;
    private static final List<Site> sites = new ArrayList<Site>();
    private static boolean dirty;
    private final Map<String, Long> missingSince = new HashMap<String, Long>();
    private int tick;

    /** Dev tests: true holds the keeper (nobody spawned or put back) while the land around a site is made. */
    public static volatile boolean hold;
    /** The last first visit fill (dev test refugevisit): residents spawned, time, world tick, and per kind {count, ns}. */
    public static volatile int lastFillSpawned;
    public static volatile long lastFillNanos, lastFillTick = -1;
    public static final Map<String, long[]> lastFillKinds = new java.util.LinkedHashMap<String, long[]>();

    /** Called by Slices when a resident plan's first slice is written. */
    public static synchronized void add(String id, int x0, int y0, int z0, int x1, int y1, int z1, List<Post> posts)
    {
        add(id, x0, y0, z0, x1, y1, z1, posts, 8);
    }

    /** As above, with the wall line inset cells inside the box (Residents.wallInset). */
    public static synchronized void add(String id, int x0, int y0, int z0, int x1, int y1, int z1, List<Post> posts,
                                        int inset)
    {
        if (file == null || find(id) != null)
        {
            return;
        }
        Site s = new Site();
        s.id = id;
        s.x0 = Math.min(x0, x1);
        s.y0 = Math.min(y0, y1);
        s.z0 = Math.min(z0, z1);
        s.x1 = Math.max(x0, x1);
        s.y1 = Math.max(y0, y1);
        s.z1 = Math.max(z0, z1);
        s.inset = inset;
        s.posts.addAll(posts);
        sites.add(s);
        dirty = true;
        FMLLog.info("[%s] safezone site %s at %d,%d,%d: %d residents", DecimationWorldGen.MODID, id, s.x0, s.y0, s.z0,
                    posts.size());
    }

    public static synchronized Site find(String id)
    {
        for (Site s : sites)
        {
            if (s.id.equals(id))
            {
                return s;
            }
        }
        return null;
    }

    /** All sites of this world (dev tests). */
    public static synchronized List<Site> all()
    {
        return new ArrayList<Site>(sites);
    }

    /** Removes the living residents of a site (dev tests, commands). */
    public static int clearResidents(World w, String id)
    {
        int n = 0;
        for (Object o : w.loadedEntityList)
        {
            Entity e = (Entity) o;
            if (!e.isDead && id.equals(e.getEntityData().getString(SITE_TAG)))
            {
                e.setDead();
                n++;
            }
        }
        return n;
    }

    /** Forgets a site (dev tests; later a fallen safezone). */
    public static synchronized boolean remove(String id)
    {
        Site s = find(id);
        if (s != null)
        {
            sites.remove(s);
            dirty = true;
        }
        return s != null;
    }

    /** True when the loaded world gets survivor camps (LargeSites). */
    public static boolean camps()
    {
        return camps;
    }

    /** Switches survivor camps for this session (dev tests); not stored. */
    public static void forceCamps(boolean on)
    {
        camps = on;
    }

    /** City refuges in this world (LcCity): worlds created since v0.48 (StructureData REFUGES_KEY). */
    private static volatile boolean refuges;
    static final String REFUGES_KEY = "!cityRefuges";

    public static boolean refuges()
    {
        return refuges;
    }

    /** Switches city refuges for this session (dev tests); not stored. Cached city plans are dropped. */
    public static void setRefuges(boolean on)
    {
        if (refuges != on)
        {
            refuges = on;
            net.decimation.worldgen.city.LcCity.layoutChanged();
        }
    }

    /** True when the world stored survivor camps on. */
    public static boolean stored(World world)
    {
        Integer v = StructureData.get(world).baseY(CAMPS_KEY);
        return v != null && v == 1;
    }

    // ------------------------------------------------------------ keeper

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || ++tick % EVERY != 0 || hold)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer w = server == null ? null : server.worldServerForDimension(0);
        if (w == null || w.playerEntities.isEmpty())
        {
            return;
        }
        for (Site s : all())
        {
            if (nearest(w, (s.x0 + s.x1) / 2.0, (s.z0 + s.z1) / 2.0) > VISIT || !loaded(w, s))
            {
                continue;
            }
            keep(w, s);
        }
    }

    /** Spawns the missing residents of a site that may be filled now. */
    void keep(World w, Site s)
    {
        long t0 = System.nanoTime();
        Map<String, long[]> kinds = new java.util.LinkedHashMap<String, long[]>();
        Set<Integer> present = new HashSet<Integer>();
        for (Object o : w.loadedEntityList)
        {
            Entity e = (Entity) o;
            if (e instanceof EntityLiving && !e.isDead && s.id.equals(e.getEntityData().getString(SITE_TAG)))
            {
                int i = e.getEntityData().getInteger(POST_TAG);
                present.add(i);
                if (i >= 0 && i < s.posts.size())
                {
                    reseat(s, s.posts.get(i), (EntityLiving) e);
                }
            }
        }
        long now = w.getTotalWorldTime();
        int spawned = 0;
        for (int i = 0; i < s.posts.size(); i++)
        {
            String key = s.id + "#" + i;
            if (present.contains(i))
            {
                missingSince.remove(key);
                continue;
            }
            Post p = s.posts.get(i);
            Long since = missingSince.get(key);
            if (since == null)
            {
                missingSince.put(key, since = now);
            }
            // a dedicated server's Decimation removes EVERY trader entity each 3000 ticks and respawns
            // only those of its decimation_traders.json (ServerTickHandler traderRestockCountdown,
            // TraderSpawnManager.respawnTraders; server side only, stripped in singleplayer): ours
            // come back at once there
            boolean wiped = cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isServer()
                && (p.kind.startsWith("trader:") || p.kind.equals("guard"));
            boolean due = !s.filled || wiped || now - since >= RESPAWN && nearest(w, p.x, p.z) > UNSEEN;
            if (!due)
            {
                continue;
            }
            long k0 = System.nanoTime();
            EntityLiving e = spawn(w, s, i);
            long[] k = kinds.get(p.kind);
            if (k == null)
            {
                kinds.put(p.kind, k = new long[2]);
            }
            k[0]++;
            k[1] += System.nanoTime() - k0;
            if (e != null)
            {
                missingSince.remove(key);
                spawned++;
            }
        }
        if (!s.filled)
        {
            synchronized (SafezoneSites.class)
            {
                s.filled = true;
                dirty = true;
            }
            long ns = System.nanoTime() - t0;
            synchronized (lastFillKinds)
            {
                lastFillKinds.clear();
                lastFillKinds.putAll(kinds);
            }
            lastFillSpawned = spawned;
            lastFillNanos = ns;
            lastFillTick = now;
            FMLLog.info("[%s] safezone site %s filled: %d residents spawned in %.1f ms", DecimationWorldGen.MODID, s.id, spawned,
                        ns / 1e6);
        }
    }

    /** Residents put back by reseat (dev test). */
    public static int reseated;

    /**
     * Puts a resident back where it belongs (user review 11 Oktober 2026: a guard "flying" on the
     * wall): traders and guards stand on their post; civilians and survivors walk about, but back
     * to their post when found higher than a step above it (a wall, a roof, a tower) or outside the
     * site's inner area (the site box less the zone margin and the wall: Site.inset).
     */
    static void reseat(Site s, Post p, EntityLiving e)
    {
        boolean fixed = p.kind.startsWith("trader:") || p.kind.equals("guard");
        double d = Math.hypot(e.posX - p.x, e.posZ - p.z);
        int in = s.inset > 0 ? s.inset : 8;
        boolean off = fixed ? d > 0.7 || Math.abs(e.posY - p.y) > 0.6
            : e.posY > p.y + 1.6 || e.posX < s.x0 + in + 1 || e.posX > s.x1 - in || e.posZ < s.z0 + in + 1
              || e.posZ > s.z1 - in;
        if (!off)
        {
            return;
        }
        FMLLog.info("[%s] safezone resident %s of %s put back from %.1f,%.1f,%.1f", DecimationWorldGen.MODID, p.kind, s.id,
                    e.posX, e.posY, e.posZ);
        reseated++;
        e.setPositionAndUpdate(p.x, p.y, p.z);
        e.rotationYaw = e.rotationYawHead = e.renderYawOffset = p.yaw;
        e.motionX = e.motionY = e.motionZ = 0;
        e.fallDistance = 0;
    }

    /** Spawns the resident of one post, tagged with its site and post; null when refused. */
    public static EntityLiving spawn(World w, Site s, int i)
    {
        Post p = s.posts.get(i);
        for (int attempt = 0; attempt < 10; attempt++)
        {
            EntityLiving e = create(w, p.kind);
            if (e == null)
            {
                return null;
            }
            e.setLocationAndAngles(p.x, p.y, p.z, p.yaw, 0);
            e.rotationYawHead = e.renderYawOffset = p.yaw;
            e.getEntityData().setString(SITE_TAG, s.id);
            e.getEntityData().setInteger(POST_TAG, i);
            e.func_110163_bv(); // never despawns
            if (w.spawnEntityInWorld(e))
            {
                return e;
            }
        }
        return null;
    }

    private static EntityLiving create(World w, String kind)
    {
        Entity e;
        if (kind.startsWith("trader:"))
        {
            e = EntityList.createEntityByName(kind.substring(7), w);
        }
        else if (kind.equals("guard"))
        {
            e = EntityList.createEntityByName("TraderGuard", w);
        }
        else if (kind.equals("civilian"))
        {
            e = EntityList.createEntityByName("Human", w);
        }
        else if (kind.equals("survivor"))
        {
            EntityLiving s = net.decimation.fixes.Deci.newSoldier(w);
            net.decimation.fixes.NpcLoadouts.instance().equipTier(s, "survivor");
            e = s;
        }
        else
        {
            e = null;
        }
        return e instanceof EntityLiving ? (EntityLiving) e : null;
    }

    private static double nearest(World w, double x, double z)
    {
        double best = Double.MAX_VALUE;
        for (Object o : w.playerEntities)
        {
            EntityPlayer p = (EntityPlayer) o;
            best = Math.min(best, Math.hypot(p.posX - x, p.posZ - z));
        }
        return best;
    }

    /**
     * Every chunk under the site and one around it exists and is populated: the structure is
     * written during population (Slices), so a resident spawned earlier could stand in a tower or on
     * a deck that is not there yet, and a static guard would hang in the air.
     */
    public static boolean ready(World w, Site s)
    {
        return loaded(w, s);
    }

    private static boolean loaded(World w, Site s)
    {
        for (int cx = (s.x0 >> 4) - 1; cx <= (s.x1 >> 4) + 1; cx++)
        {
            for (int cz = (s.z0 >> 4) - 1; cz <= (s.z1 >> 4) + 1; cz++)
            {
                if (!w.getChunkProvider().chunkExists(cx, cz) || !w.getChunkFromChunkCoords(cx, cz).isTerrainPopulated)
                {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------ storage

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.world;
        if (world.isRemote || world.provider == null || world.provider.dimensionId != 0)
        {
            return;
        }
        // a world that never ticked is new and gets survivor camps; older worlds would get camps
        // cut off by chunks generated before (as city rivers); -Ddeciworldgen.camps overrides
        StructureData data = StructureData.get(world);
        Integer v = data.baseY(CAMPS_KEY);
        if (v == null)
        {
            v = world.getTotalWorldTime() == 0 ? 1 : 0;
            data.setBaseY(CAMPS_KEY, v);
        }
        String force = System.getProperty("deciworldgen.camps");
        camps = force != null ? "true".equals(force) : v == 1;
        FMLLog.info("[%s] survivor camps %s", DecimationWorldGen.MODID, camps ? "on" : "off");
        // city refuges (one fortified building per city region): worlds created since v0.48 only,
        // an older world would get a refuge cut into a block whose other chunks hold buildings
        Integer rv = data.baseY(REFUGES_KEY);
        if (rv == null)
        {
            rv = world.getTotalWorldTime() == 0 ? 1 : 0;
            data.setBaseY(REFUGES_KEY, rv);
        }
        String rforce = System.getProperty("deciworldgen.refuges");
        setRefuges(rforce != null ? "true".equals(rforce) : rv == 1);
        FMLLog.info("[%s] city refuges %s", DecimationWorldGen.MODID, refuges ? "on" : "off");
        synchronized (SafezoneSites.class)
        {
            sites.clear();
            file = new File(world.getSaveHandler().getWorldDirectory(), FILE);
            if (file.isFile())
            {
                try
                {
                    Reader in = new FileReader(file);
                    SiteList saved = new Gson().fromJson(in, SiteList.class);
                    in.close();
                    if (saved != null && saved.sites != null)
                    {
                        sites.addAll(saved.sites);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.info("[%s] could not read %s: %s", DecimationWorldGen.MODID, file, e);
                }
            }
            dirty = false;
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        if (!event.world.isRemote && event.world.provider != null && event.world.provider.dimensionId == 0)
        {
            save();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (!event.world.isRemote && event.world.provider != null && event.world.provider.dimensionId == 0)
        {
            synchronized (SafezoneSites.class)
            {
                save();
                sites.clear();
                file = null;
            }
        }
    }

    private static synchronized void save()
    {
        if (!dirty || file == null)
        {
            return;
        }
        try
        {
            SiteList out = new SiteList();
            out.sites = new ArrayList<Site>(sites);
            Writer wr = new FileWriter(file);
            wr.write(new GsonBuilder().setPrettyPrinting().create().toJson(out));
            wr.close();
            dirty = false;
        }
        catch (Exception e)
        {
            FMLLog.info("[%s] could not write %s: %s", DecimationWorldGen.MODID, file, e);
        }
    }
}
