package net.decimation.fixes;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Looted spots stay looted (feature 12 A, docs/superpowers/specs/2026-10-22-loot-rules-design.md; user, 22 Oktober 2026:
 * "loot by default is empty forever after player loot it, except multiplayer they can be disable or enable for minutes
 * refill loots (by default is enable for server)", "15 minute use old decimation setting, for singleplayer it has option
 * on cfg file world too so they can use refill method or not").
 *
 * Decimation's LootCooldownRegistry (deci.aB.e) keeps each looted spot for 15 minutes of real time in memory only, so a
 * restart filled every crate again. Our Record keeps the world tick of each spot's last loot with the world. A right
 * click on a loot block sets Decimation's own state of the spot before its handler reads it (HIGHEST): looted, and the
 * handler answers its own "This <block> is empty...", or ready, and it rolls as always. The spot is written after
 * Decimation looted it (LOWEST, also when cancelled, which it does): its loot inventory changed during the click, as
 * fixes/LootMagazines reads it. The switch lives in the world's folder (deciworldgen_loot.cfg), written on the first
 * load: refill off in singleplayer, on with Decimation's 15 minutes (its lootRespawnTime) on a dedicated server.
 * Supply drops keep Decimation's own rule (they go when looted); our loot bodies are entities and not touched.
 */
public final class LootRefill
{
    public static final String FILE = "deciworldgen_loot.cfg";
    public static final String CATEGORY = "loot";
    /** World ticks a minute. */
    static final long MINUTE = 1200;

    /** One world's switch. */
    public static final class Settings
    {
        /** Looted spots fill again after `minutes` of world time; false: a looted spot stays empty for good. */
        public final boolean refill;
        public final int minutes;

        public Settings(boolean refill, int minutes)
        {
            this.refill = refill;
            this.minutes = Math.max(0, minutes);
        }

        @Override
        public String toString()
        {
            return refill ? "refill after " + minutes + " minutes" : "no refill";
        }
    }

    /** A new world's switch: refill only on a dedicated server, after Decimation's own time (lootRespawnTime, seconds). */
    static Settings defaults(boolean dedicated, int decimationSeconds)
    {
        return new Settings(dedicated, (Math.max(0, decimationSeconds) + 30) / 60);
    }

    /** A spot gives loot: never looted (lootedAt below 0), or refill on and its minutes of world time passed. */
    public static boolean ready(long lootedAt, long now, Settings s)
    {
        return lootedAt < 0 || s.refill && now - lootedAt >= s.minutes * MINUTE;
    }

    /** A spot as one long, packed as Decimation's LootCooldownRegistry.packPos (j) does: x 26 bits, y 12, z 26. */
    static long key(int x, int y, int z)
    {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | (long) z & 0x3FFFFFFL;
    }

    /** Reads the world's switch from its folder, writing the defaults there first when the file is new. */
    public static Settings settings(File worldDir, boolean dedicated, int decimationSeconds)
    {
        Settings d = defaults(dedicated, decimationSeconds);
        Configuration cfg = new Configuration(new File(worldDir, FILE));
        cfg.setCategoryComment(CATEGORY, "Loot of this world (DeciRemnant). Crates, car wrecks and other loot blocks: "
            + "once looted, they stay empty unless refill is true.");
        boolean refill = cfg.get(CATEGORY, "refill", d.refill,
                                 "true: a looted spot fills again after refillMinutes of world time (Decimation's old way). "
                                 + "false: it stays empty for good. New worlds: false in singleplayer, true on a server.")
            .getBoolean(d.refill);
        int minutes = cfg.get(CATEGORY, "refillMinutes", d.minutes,
                              "Minutes of world time until a looted spot fills again (with refill true). 0: at once.", 0,
                              100000)
            .getInt(d.minutes);
        if (cfg.hasChanged())
        {
            cfg.save();
        }
        return new Settings(refill, minutes);
    }

    /** Every looted spot of a world (all dimensions share the overworld's data folder), with its last loot's world tick. */
    public static class Record extends WorldSavedData
    {
        public static final String NAME = "deciworldgen_looted";
        private final Map<Integer, Map<Long, Long>> byDimension = new HashMap<Integer, Map<Long, Long>>();

        public Record(String name)
        {
            super(name);
        }

        /** World tick of the spot's last loot, -1 when it was never looted. */
        public long get(int dim, int x, int y, int z)
        {
            Map<Long, Long> m = byDimension.get(dim);
            Long t = m == null ? null : m.get(key(x, y, z));
            return t == null ? -1 : t;
        }

        public void put(int dim, int x, int y, int z, long tick)
        {
            Map<Long, Long> m = byDimension.get(dim);
            if (m == null)
            {
                m = new HashMap<Long, Long>();
                byDimension.put(dim, m);
            }
            m.put(key(x, y, z), tick);
            markDirty();
        }

        /** Forgets one spot (tests); true when it was there. */
        public boolean forget(int dim, int x, int y, int z)
        {
            Map<Long, Long> m = byDimension.get(dim);
            boolean was = m != null && m.remove(key(x, y, z)) != null;
            if (was)
            {
                markDirty();
            }
            return was;
        }

        /** Forgets every spot (tests). */
        public void clear()
        {
            byDimension.clear();
            markDirty();
        }

        public int size()
        {
            int n = 0;
            for (Map<Long, Long> m : byDimension.values())
            {
                n += m.size();
            }
            return n;
        }

        /** Drops the spots that give loot again anyway (refill on and their time passed); returns how many. */
        public int prune(long now, Settings s)
        {
            int n = 0;
            for (Map<Long, Long> m : byDimension.values())
            {
                for (Iterator<Long> it = m.values().iterator(); it.hasNext(); )
                {
                    if (ready(it.next(), now, s))
                    {
                        it.remove();
                        n++;
                    }
                }
            }
            if (n > 0)
            {
                markDirty();
            }
            return n;
        }

        /** Per dimension an int array of x, y, z, tick high, tick low for each spot. */
        @Override
        public void readFromNBT(NBTTagCompound tag)
        {
            byDimension.clear();
            for (Object o : tag.func_150296_c())
            {
                String k = (String) o;
                if (!k.startsWith("d"))
                {
                    continue;
                }
                int dim;
                try
                {
                    dim = Integer.parseInt(k.substring(1));
                }
                catch (NumberFormatException e)
                {
                    continue;
                }
                int[] a = tag.getIntArray(k);
                Map<Long, Long> m = new HashMap<Long, Long>(a.length / 5 * 4 / 3 + 1);
                for (int i = 0; i + 4 < a.length; i += 5)
                {
                    m.put(key(a[i], a[i + 1], a[i + 2]), (long) a[i + 3] << 32 | a[i + 4] & 0xFFFFFFFFL);
                }
                byDimension.put(dim, m);
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound tag)
        {
            for (Map.Entry<Integer, Map<Long, Long>> d : byDimension.entrySet())
            {
                int[] a = new int[d.getValue().size() * 5];
                int i = 0;
                for (Map.Entry<Long, Long> e : d.getValue().entrySet())
                {
                    long k = e.getKey(), t = e.getValue();
                    a[i++] = (int) (k >> 38);
                    a[i++] = (int) (k >> 26 & 0xFFF);
                    a[i++] = (int) (k << 38 >> 38);
                    a[i++] = (int) (t >>> 32);
                    a[i++] = (int) t;
                }
                tag.setIntArray("d" + d.getKey(), a);
            }
        }
    }

    private static volatile Settings settings = defaults(false, 900);

    /** The running world's switch. */
    public static Settings settings()
    {
        return settings;
    }

    /** Changes the running world's switch until the next load (tests). */
    public static void use(Settings s)
    {
        settings = s;
    }

    /** Every spot of the running server gives loot again, in Decimation's registry and in our record (tests). */
    public static void clearAll()
    {
        Deci.clearLootCooldowns();
        MinecraftServer s = MinecraftServer.getServer();
        Record r = s == null ? null : record(s.worldServerForDimension(0));
        if (r != null)
        {
            r.clear();
        }
    }

    /** The record of a server world, null on a client world. */
    public static Record record(World world)
    {
        if (world == null || world.isRemote || world.mapStorage == null)
        {
            return null;
        }
        Record r = (Record) world.mapStorage.loadData(Record.class, Record.NAME);
        if (r == null)
        {
            r = new Record(Record.NAME);
            world.mapStorage.setData(Record.NAME, r);
        }
        return r;
    }

    // ---- the click

    private EntityPlayer clicker;
    private InventoryBasic before;
    private int spotX, spotY, spotZ;

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e)
    {
        World w = e.world;
        if (w.isRemote || w.provider.dimensionId != 0)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        boolean dedicated = server != null && server.isDedicatedServer();
        settings = settings(w.getSaveHandler().getWorldDirectory(), dedicated, Deci.lootRespawnSeconds());
        Record r = record(w);
        int pruned = r == null ? 0 : r.prune(w.getTotalWorldTime(), settings);
        FMLLog.info("[deciworldgen] loot: %s, %d looted spots kept, %d filled again", settings, r == null ? 0 : r.size(), pruned);
    }

    /** Before Decimation's LootInteractHandler: Decimation's own state of the spot follows ours. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void before(PlayerInteractEvent e)
    {
        clicker = null;
        if (e.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || e.world.isRemote
            || e.entityPlayer.capabilities.isCreativeMode)
        {
            return;
        }
        Block block = e.world.getBlock(e.x, e.y, e.z);
        if (!Deci.hasLootPool(block) || block == Deci.supplyDropBlock())
        {
            return;
        }
        int[] spot = Deci.lootSpot(e.world, e.x, e.y, e.z);
        Record r = spot == null ? null : record(e.world);
        if (r == null)
        {
            return;
        }
        long at = r.get(e.world.provider.dimensionId, spot[0], spot[1], spot[2]);
        Deci.setLootCooldown(e.world, spot[0], spot[1], spot[2], !ready(at, e.world.getTotalWorldTime(), settings));
        clicker = e.entityPlayer;
        before = Deci.lootInventory(e.entityPlayer);
        spotX = spot[0];
        spotY = spot[1];
        spotZ = spot[2];
    }

    /** After it: a new loot inventory means Decimation looted the spot. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void after(PlayerInteractEvent e)
    {
        if (e.world.isRemote || clicker == null || e.entityPlayer != clicker
            || e.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            return;
        }
        InventoryBasic now = Deci.lootInventory(e.entityPlayer);
        Record r = record(e.world);
        if (now != null && now != before && r != null)
        {
            r.put(e.world.provider.dimensionId, spotX, spotY, spotZ, e.world.getTotalWorldTime());
        }
        clicker = null;
        before = null;
    }
}
