package net.decimation.worldgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Garrisons of the hardest places (user, 13 Oktober 2026: the wasteland bunker "most harder place
 * building with full of high tier enemy npc in there. elite npc + juggernaut and full military outfit
 * zombie spawner"; docs/wasteland.md). A plan with a garrison hands Slices its posts and spawner cells
 * when its first slice is written; they are kept per world in StructureData lists "!gar_<id>":
 * {state, spawners, sx, sy, sz, ..., px, py, pz, ...}, state 0 waiting, 1 manned. The first time a
 * player comes within VISIT of a garrison's first post (its chunks loaded), its posts are manned:
 * juggernauts and elite military in turn, never despawning (spawned then and not at generation:
 * Decimation's NPCs would despawn before anyone came). A post whose chunk is not loaded yet waits for
 * it (an airport's posts lie up to 250 blocks apart, beyond the loaded area: until 17 Oktober 2026 they
 * were marked manned without their NPC); per post state in the list "!garm_<id>": 0 waiting, below 0 failed
 * tries, 1 spawned, 2 given up after TRIES. A post waits until the chunks around it exist (then its chunk is
 * populated and its building written: a tower guard spawned earlier stood on bare ground). Guards keep a home
 * area at their post and walk back to it after a chase (backToPosts). Its spawners (vanilla mob spawner cages) breed Decimation's
 * infected; every infected that appears within SPAWNER_REACH of one gets the military outfit.
 */
public class Garrisons
{
    /** A plan with a garrison: posts and spawners in world coordinates, the floor at baseY. */
    public interface Garrisoned extends Plan
    {
        /** {spawners, sx, sy, sz, ..., px, py, pz, ...}; null or empty for none. */
        int[] garrison(int baseY);
    }

    static final String PREFIX = "!gar_", POSTS = "!garm_";
    /** A garrison NPC's post (entity data, saved with it): its home area, so it guards the post instead of wandering off. */
    static final String POST_TAG = "deciworldgen_post";
    private static final int HOME = 4, TRIES = 5;
    private static final int EVERY = 40, VISIT = 40, SPAWNER_REACH = 6;
    /** Spawner cells of this world, x / y / z packed (filled at load and on register). */
    private static final Set<Long> spawners = new HashSet<Long>();
    private static volatile boolean loaded;
    private int tick;

    /** Slices: the plan's garrison, stored once. */
    public static void register(World world, Plan p, int baseY)
    {
        if (!(p instanceof Garrisoned))
        {
            return;
        }
        StructureData data = StructureData.get(world);
        if (data.list(PREFIX + p.id()) != null)
        {
            return;
        }
        int[] g = ((Garrisoned) p).garrison(baseY);
        if (g == null || g.length == 0)
        {
            return;
        }
        int[] v = new int[g.length + 1];
        System.arraycopy(g, 0, v, 1, g.length);
        data.setList(PREFIX + p.id(), v);
        addSpawners(v);
        FMLLog.info("[%s] garrison %s: %d posts, %d spawners", DecimationWorldGen.MODID, p.id(),
                    (g.length - 1 - 3 * g[0]) / 3, g[0]);
    }

    /** Forgets one spawner cell (dev test nospawn). */
    public static synchronized void forgetSpawner(int x, int y, int z)
    {
        spawners.remove(key(x, y, z));
    }

    private static synchronized void addSpawners(int[] v)
    {
        for (int i = 0; i < v[1]; i++)
        {
            spawners.add(key(v[2 + 3 * i], v[3 + 3 * i], v[4 + 3 * i]));
        }
    }

    private static long key(int x, int y, int z)
    {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** Slices: a mob spawner written by a plan breeds Decimation's infected (vanilla's default is a pig). */
    public static void setupSpawner(World world, int x, int y, int z)
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityMobSpawner)
        {
            String name = EntityList.getEntityString(Deci.newInfected(world));
            if (name != null)
            {
                ((TileEntityMobSpawner) te).func_145881_a().setEntityName(name);
            }
        }
    }

    private static synchronized void load(World w)
    {
        if (loaded)
        {
            return;
        }
        loaded = true;
        spawners.clear();
        StructureData data = StructureData.get(w);
        for (String id : data.lists(PREFIX))
        {
            addSpawners(data.list(id));
        }
    }

    /** A world loading (dev test worlds, a new world in one session) reads its own garrisons. */
    public static synchronized void reset()
    {
        loaded = false;
        spawners.clear();
    }

    @SubscribeEvent
    public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event)
    {
        if (!event.world.isRemote && event.world.provider != null && event.world.provider.dimensionId == 0)
        {
            reset();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || ++tick % EVERY != 0)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer w = server == null ? null : server.worldServerForDimension(0);
        if (w == null || w.playerEntities.isEmpty())
        {
            return;
        }
        load(w);
        if (!net.decimation.fixes.NaturalSpawns.on(w))
        {
            return;                                     // natural spawns off: posts wait (manned once it is on again)
        }
        StructureData data = StructureData.get(w);
        for (String id : data.lists(PREFIX))
        {
            int[] v = data.list(id);
            int first = 2 + 3 * v[1], posts = (v.length - first) / 3;
            if (v[0] != 0 || posts < 1)
            {
                continue;
            }
            String state = POSTS + id.substring(PREFIX.length());
            int[] done = data.list(state);
            boolean fresh = done == null;
            if (fresh)
            {
                int px = v[first], py = v[first + 1], pz = v[first + 2];
                EntityPlayer near = w.getClosestPlayer(px + 0.5, py, pz + 0.5, VISIT);
                if (near == null || !w.checkChunksExist(px - 16, py, pz - 16, px + 16, py, pz + 16))
                {
                    continue;                                   // nobody came yet
                }
                done = new int[posts];
            }
            int n = 0, waiting = 0, tried = 0;
            for (int k = 0; k < posts; k++)
            {
                int i = first + 3 * k;
                if (done[k] > 0)
                {
                    continue;                                   // 1 spawned, 2 given up
                }
                if (!w.checkChunksExist(v[i] - 16, v[i + 1], v[i + 2] - 16, v[i] + 16, v[i + 1], v[i + 2] + 16))
                {
                    waiting++;                                  // its chunks not all loaded: not populated, its building not written
                    continue;
                }
                if (spawn(w, v, i, k))
                {
                    n++;
                    done[k] = 1;
                }
                else
                {
                    done[k] = done[k] - 1 <= -TRIES ? 2 : done[k] - 1;   // a join handler said no: a few more tries
                    waiting += done[k] > 0 ? 0 : 1;
                }
                tried++;
            }
            if (fresh || tried > 0)
            {
                data.setList(state, done);
            }
            if (waiting == 0)
            {
                v[0] = 1;
                data.setList(id, v);
            }
            if (n > 0)
            {
                FMLLog.info("[%s] garrison %s manned: %d NPCs%s", DecimationWorldGen.MODID, id.substring(PREFIX.length()), n,
                            waiting > 0 ? ", " + waiting + " posts wait for their chunks" : "");
            }
        }
        backToPosts(w);
    }

    /**
     * Guards drawn off their posts (a chase, a push) walk back once nothing holds their attention: no revenge target
     * (Decimation never clears entityToAttack, so getAITarget is the "fighting" sign) and no path of their own. The
     * home area keeps idle wandering near the post but brings no one back from outside it. Legs of at most 12 blocks,
     * within the path finder's reach; a guard below a tower post stays at its foot (no ladders in vanilla paths).
     */
    private static void backToPosts(WorldServer w)
    {
        // a copy: a path search loads chunks around the guard, and a loaded chunk adds its entities to loadedEntityList
        for (Object o : new java.util.ArrayList<Object>(w.loadedEntityList))
        {
            if (!(o instanceof net.minecraft.entity.EntityCreature) || Deci.npcKind((Entity) o) != net.decimation.fixes.NpcKind.SOVIET)
            {
                continue;
            }
            net.minecraft.entity.EntityCreature c = (net.minecraft.entity.EntityCreature) o;
            int[] post = c.getEntityData().getIntArray(POST_TAG);
            if (post.length != 3 || !c.isEntityAlive() || c.getAITarget() != null || !c.getNavigator().noPath())
            {
                continue;
            }
            double dx = post[0] + 0.5 - c.posX, dz = post[2] + 0.5 - c.posZ, d = Math.sqrt(dx * dx + dz * dz);
            if (d <= HOME + 2)
            {
                continue;
            }
            if (d <= 12)
            {
                c.getNavigator().tryMoveToXYZ(post[0] + 0.5, post[1], post[2] + 0.5, 0.5);
            }
            else
            {
                c.getNavigator().tryMoveToXYZ(c.posX + dx * 12 / d, c.posY, c.posZ + dz * 12 / d, 0.5);
            }
        }
    }

    /** Spawns the garrison's NPCs on all their posts at once: juggernaut, elite, elite, juggernaut, ... (dev tests). */
    static int man(World w, String id, int[] v, int first)
    {
        int n = 0;
        for (int i = first, k = 0; i + 2 < v.length; i += 3, k++)
        {
            n += spawn(w, v, i, k) ? 1 : 0;
        }
        v[0] = 1;
        StructureData.get(w).setList(id, v);
        FMLLog.info("[%s] garrison %s manned: %d NPCs", DecimationWorldGen.MODID, id.substring(PREFIX.length()), n);
        return n;
    }

    /** One NPC on post k (its coordinates at v[i]): every third a juggernaut, the others elite military. */
    private static boolean spawn(World w, int[] v, int i, int k)
    {
        EntityLiving npc = Deci.newSoviet(w);
        if (NpcLoadouts.instance() != null)
        {
            NpcLoadouts.instance().equipTier(npc, k % 3 == 0 ? "juggernaut" : "elite_military");
        }
        npc.setLocationAndAngles(v[i] + 0.5, v[i + 1], v[i + 2] + 0.5, w.rand.nextFloat() * 360, 0);
        npc.func_110163_bv();                           // never despawns
        npc.getEntityData().setIntArray(POST_TAG, new int[] {v[i], v[i + 1], v[i + 2]});
        home(npc);
        return w.spawnEntityInWorld(npc);
    }

    /**
     * Keeps a garrison NPC near its post: vanilla's wander AI (Decimation's NPCs use EntityAIWander) picks targets only
     * within the home area; chasing a target still goes beyond it. Vanilla does not save a home: set again on load.
     */
    static void home(Entity e)
    {
        int[] post = e.getEntityData().getIntArray(POST_TAG);
        if (post.length == 3 && e instanceof net.minecraft.entity.EntityCreature)
        {
            ((net.minecraft.entity.EntityCreature) e).setHomeArea(post[0], post[1], post[2], HOME);
        }
    }

    /** A garrison NPC gets its post back as home; every infected that appears beside a garrison spawner wears the military outfit. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event)
    {
        Entity e = event.entity;
        if (!e.worldObj.isRemote && Deci.npcKind(e) == net.decimation.fixes.NpcKind.SOVIET)
        {
            home(e);                                    // a garrison NPC loaded with its chunk: back to its post
        }
        if (e.worldObj.isRemote || !Deci.isPlainInfected(e) || spawners.isEmpty())
        {
            return;
        }
        int x = (int) Math.floor(e.posX), y = (int) Math.floor(e.posY), z = (int) Math.floor(e.posZ);
        synchronized (Garrisons.class)
        {
            for (int dx = -SPAWNER_REACH; dx <= SPAWNER_REACH; dx++)
            {
                for (int dz = -SPAWNER_REACH; dz <= SPAWNER_REACH; dz++)
                {
                    for (int dy = -3; dy <= 3; dy++)
                    {
                        if (spawners.contains(key(x + dx, y + dy, z + dz)))
                        {
                            if (!net.decimation.fixes.NaturalSpawns.on(e.worldObj))
                            {
                                event.setCanceled(true); // the cage's spawn, natural spawns off
                                return;
                            }
                            Deci.setInfectedVariant(e, "MILITARY");
                            return;
                        }
                    }
                }
            }
        }
    }

    /** The garrison ids of this world (dev tests). */
    public static List<String> ids(World w)
    {
        return StructureData.get(w).lists(PREFIX);
    }

    /** The posts of a garrison, {x, y, z} each (dev tests). */
    public static int[][] posts(World w, String id)
    {
        int[] v = StructureData.get(w).list(id);
        if (v == null)
        {
            return new int[0][];
        }
        int first = 2 + 3 * v[1];
        int[][] out = new int[(v.length - first) / 3][];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = new int[] {v[first + 3 * i], v[first + 3 * i + 1], v[first + 3 * i + 2]};
        }
        return out;
    }

    /** True once every post of a garrison had its NPC spawned (dev tests). */
    public static boolean manned(World w, String id)
    {
        int[] v = StructureData.get(w).list(id);
        return v != null && v[0] != 0;
    }

    /** Posts of a garrison done so far: spawned, or given up after TRIES (dev tests). */
    public static int postsDone(World w, String id)
    {
        int[] v = StructureData.get(w).list(id);
        if (v != null && v[0] != 0)
        {
            return posts(w, id).length;
        }
        int[] done = StructureData.get(w).list(POSTS + id.substring(PREFIX.length()));
        int n = 0;
        for (int i = 0; done != null && i < done.length; i++)
        {
            n += done[i] > 0 ? 1 : 0;
        }
        return n;
    }

    /** Mans a garrison now (dev tests); the number of NPCs spawned. */
    public static int manNow(World w, String id)
    {
        int[] v = StructureData.get(w).list(id);
        return v == null || v[0] != 0 ? 0 : man(w, id, v, 2 + 3 * v[1]);
    }
}
