package net.decimation.fixes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.worldgen.Places;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

/**
 * Specific mobs at specific places (user, 22 Oktober 2026, docs/ideas.md 35: "a table per place kind, editable in a
 * config file: the wasteland irradiated and military infected, demons at night; bases and airports soldiers; hospitals
 * medic infected; police stations riot infected; forests infected dogs; highways bandit groups").
 * - The table: config/deciworldgen_places.json, one list per place kind (worldgen/Places: airport, base, police_station,
 *   hospital, highway, wasteland, forest). An entry: "mob" (infected, infected_dog, crawler, hulk, frail, bloater, demon,
 *   soldier, bandit, soviet), "look" for infected (military and police are Decimation's, runner, riot, screamer,
 *   irradiated and medic ours: fixes/InfectedVariants), "chance" in percent, "group" [least, most], "time" (any, day,
 *   night), "cap" (no more of that mob within 64 blocks across, 32 up or down; 0: none).
 * - How: when Decimation's natural spawning (vanilla's SpawnerAnimals, its biome lists) puts a plain infected at a
 *   place of the table, one roll of 100 walks that place's entries in order: an infected entry gives it that look (put
 *   on when it joins, after the zone handlers' looks), another mob replaces it (the infected is refused, the group
 *   spawns on its spot), the rest stays as Decimation spawns it. So the table changes WHAT spawns where, not how many:
 *   the game's mob cap, doMobSpawning, peaceful and the spawn rules stay in charge, and nothing runs per tick.
 * - Cost: one place lookup and one roll per natural spawn of an infected (Places memoises the seed's places per chunk);
 *   the cap count (a box search) only when an entry with a cap is rolled.
 * The file is written with the user's table when missing or older than VERSION (the old one kept as .old); after that
 * it is the user's: edit, then restart. Mistakes are logged and the entry skipped.
 */
public final class PlaceSpawns
{
    /** Bump when the built-in table changes: an older file is replaced (kept as .old). */
    public static final int VERSION = 1;
    static final String FILE = "deciworldgen_places.json";
    /** Entity data: the place a mob was spawned for, and a look still to put on. */
    public static final String PLACE = "deciworldgen_place", LOOK = "deciworldgen_place_look";
    static final List<String> MOBS = Arrays.asList("infected", "infected_dog", "crawler", "hulk", "frail", "bloater", "demon",
                                                   "soldier", "bandit", "soviet");
    /** Decimation's own infected looks (InfectedEntity's variant); the others are InfectedVariants'. */
    static final List<String> DECIMATION_LOOKS = Arrays.asList("military", "police");

    public static class Entry
    {
        public String mob = "infected";
        public String look;
        public int chance;
        public int[] group = {1, 1};
        public String time = "any";
        public int cap;
    }

    static class FileData
    {
        int version;
        String about;
        Map<String, List<Entry>> places = new LinkedHashMap<String, List<Entry>>();
    }

    private static final EnumMap<Places.Kind, List<Entry>> TABLE = new EnumMap<Places.Kind, List<Entry>>(Places.Kind.class);
    private static final EnumMap<Places.Kind, int[]> DECISIONS = new EnumMap<Places.Kind, int[]>(Places.Kind.class);
    /** What did not check out at the last load ("place: problem"). */
    public static final List<String> problems = new ArrayList<String>();
    private final Random random = new Random();
    private static boolean failed;

    // ------------------------------------------------------------ the table (unit tested)

    private static Entry entry(String mob, String look, int chance, int least, int most, String time, int cap)
    {
        Entry e = new Entry();
        e.mob = mob;
        e.look = look;
        e.chance = chance;
        e.group = new int[] {least, most};
        e.time = time;
        e.cap = cap;
        return e;
    }

    /** The user's table (22 Oktober 2026); the numbers are mine [draft]. */
    static FileData defaults()
    {
        FileData d = new FileData();
        d.version = VERSION;
        d.about = "Specific mobs at specific places. When the game spawns a plain infected on its own at one of these places, "
            + "one roll of 100 walks the place's entries in order: chance is in percent of those spawns; an infected entry "
            + "gives it a look (military, police: Decimation's; runner, riot, screamer, irradiated, medic: ours), another mob "
            + "takes its place (group: least and most of them), time is any, day or night, cap: no more of that mob within "
            + "64 blocks across and 32 up or down (0: no cap). Mobs: infected, infected_dog, crawler, hulk, frail, bloater, "
            + "demon, soldier, bandit, soviet. "
            + "Places, the first that holds: airport, base, police_station, hospital, highway, wasteland, forest. "
            + "Edit and restart; a file older than the mod's table is replaced (kept as .old).";
        d.places.put("wasteland", Arrays.asList(entry("infected", "irradiated", 30, 1, 1, "any", 0),
                                                entry("infected", "military", 30, 1, 1, "any", 0),
                                                entry("demon", null, 2, 1, 1, "night", 2)));
        d.places.put("base", Arrays.asList(entry("soldier", null, 30, 2, 3, "any", 6)));
        d.places.put("airport", Arrays.asList(entry("soldier", null, 30, 2, 3, "any", 6)));
        d.places.put("hospital", Arrays.asList(entry("infected", "medic", 60, 1, 1, "any", 0)));
        d.places.put("police_station", Arrays.asList(entry("infected", "riot", 60, 1, 1, "any", 0)));
        d.places.put("forest", Arrays.asList(entry("infected_dog", null, 30, 1, 2, "any", 6)));
        d.places.put("highway", Arrays.asList(entry("bandit", null, 15, 2, 4, "any", 8)));
        return d;
    }

    /** The mistakes of a table: unknown places, mobs, looks or times, bad groups, chances above 100 at one time of day. */
    static List<String> check(Map<String, List<Entry>> places, Collection<String> ourLooks)
    {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, List<Entry>> p : places.entrySet())
        {
            if (Places.Kind.byId(p.getKey()) == null)
            {
                out.add(p.getKey() + ": no such place");
                continue;
            }
            int day = 0, night = 0;
            for (Entry e : p.getValue() == null ? new ArrayList<Entry>() : p.getValue())
            {
                String bad = problem(e, ourLooks);
                if (bad != null)
                {
                    out.add(p.getKey() + ": " + bad);
                    continue;
                }
                day += "night".equals(e.time) ? 0 : e.chance;
                night += "day".equals(e.time) ? 0 : e.chance;
            }
            if (day > 100 || night > 100)
            {
                out.add(p.getKey() + ": chances add up to more than 100");
            }
        }
        return out;
    }

    private static String problem(Entry e, Collection<String> ourLooks)
    {
        if (e == null || !MOBS.contains(e.mob))
        {
            return "no such mob " + (e == null ? null : e.mob);
        }
        if (e.look != null && !("infected".equals(e.mob) && (DECIMATION_LOOKS.contains(e.look) || ourLooks.contains(e.look))))
        {
            return "no such look " + e.look + " for " + e.mob;
        }
        if (!"any".equals(e.time) && !"day".equals(e.time) && !"night".equals(e.time))
        {
            return "time " + e.time + " (any, day or night)";
        }
        if (e.chance < 0 || e.group == null || e.group.length != 2 || e.group[0] < 1 || e.group[1] < e.group[0] || e.group[1] > 8
            || e.cap < 0)
        {
            return "chance, group or cap out of range for " + e.mob;
        }
        return null;
    }

    /** The entry a roll of 0 .. 99 lands on (entries for the other time of day skipped), or null: unchanged. */
    static Entry pick(List<Entry> entries, int roll, boolean night)
    {
        for (int i = 0; i < entries.size(); i++)
        {
            Entry e = entries.get(i);
            if (night ? "day".equals(e.time) : "night".equals(e.time))
            {
                continue;
            }
            if (roll < e.chance)
            {
                return e;
            }
            roll -= e.chance;
        }
        return null;
    }

    /** Night by the time of day: dusk (13000) to dawn (23000). */
    static boolean night(long worldTime)
    {
        long t = worldTime % 24000;
        return t >= 13000 && t < 23000;
    }

    /** The file, written with the built-in table when missing or older than VERSION. */
    static FileData read(File dir)
    {
        File f = new File(dir, FILE);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();   // readable quotes for people
        FileData data = null;
        if (f.isFile())
        {
            try (Reader in = new InputStreamReader(new FileInputStream(f), "UTF-8"))
            {
                data = gson.fromJson(in, FileData.class);
            }
            catch (Exception e)
            {
                FMLLog.warning("[deciworldgen] %s unreadable, the built-in place table used: %s", f, e);
            }
        }
        if (data == null || data.version < VERSION || data.places == null)
        {
            if (f.isFile())
            {
                File old = new File(dir, FILE + ".old");
                old.delete();
                f.renameTo(old);
            }
            data = defaults();
            try (Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))
            {
                out.write(gson.toJson(data));
            }
            catch (Exception e)
            {
                FMLLog.warning("[deciworldgen] could not write %s: %s", f, e);
            }
        }
        return data;
    }

    /** postInit: the table from the config folder (InfectedVariants' looks are known by then). */
    public static synchronized void load(File configDir)
    {
        FileData data = read(configDir);
        List<String> looks = new ArrayList<String>();
        InfectedVariants v = InfectedVariants.instance();
        for (int i = 0; v != null && v.byIndex(i) != null; i++)
        {
            looks.add(v.byIndex(i).name);
        }
        problems.clear();
        problems.addAll(check(data.places, looks));
        TABLE.clear();
        DECISIONS.clear();
        for (Map.Entry<String, List<Entry>> p : data.places.entrySet())
        {
            Places.Kind k = Places.Kind.byId(p.getKey());
            if (k == null || p.getValue() == null)
            {
                continue;
            }
            List<Entry> ok = new ArrayList<Entry>();
            for (Entry e : p.getValue())
            {
                if (problem(e, looks) == null)
                {
                    ok.add(e);
                }
            }
            TABLE.put(k, ok);
            DECISIONS.put(k, new int[ok.size() + 1]);   // the last counts rolls that changed nothing
        }
        for (String s : problems)
        {
            FMLLog.warning("[deciworldgen] place table: %s", s);
        }
        FMLLog.info("[deciworldgen] place table: %d places, %d problems", TABLE.size(), problems.size());
    }

    // ------------------------------------------------------------ natural spawns

    /** Natural spawns only (SpawnerAnimals fires this; spawner blocks, eggs, commands and our code do not). */
    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e)
    {
        if (e.world.isRemote || !Deci.isPlainInfected(e.entityLiving) || TABLE.isEmpty())
        {
            return;
        }
        long start = System.nanoTime();
        try
        {
            decide(e);
        }
        catch (RuntimeException ex)
        {
            if (!failed)
            {
                failed = true;                              // once: never out of the spawn cycle
                FMLLog.warning("[deciworldgen] place spawn failed: %s", ex);
            }
        }
        checks++;
        checkNanos += System.nanoTime() - start;
    }

    private void decide(LivingSpawnEvent.CheckSpawn e)
    {
        Places.Kind place = forcePlace != null ? forcePlace
            : Places.at(e.world, MathHelper.floor_float(e.x), MathHelper.floor_float(e.z));
        List<Entry> entries = place == null ? null : TABLE.get(place);
        if (entries == null || entries.isEmpty())
        {
            return;
        }
        int roll = forceRoll >= 0 ? forceRoll : random.nextInt(100);
        Entry en = pick(entries, roll, night(e.world.getWorldTime()));
        int[] counts = DECISIONS.get(place);
        counts[en == null ? entries.size() : entries.indexOf(en)]++;
        if (en == null || dryRun)
        {
            return;
        }
        int n = en.group[0] + random.nextInt(en.group[1] - en.group[0] + 1);
        if (en.cap > 0 && near(e.world, en.mob, e.x, e.y, e.z) + ("infected".equals(en.mob) ? 0 : n) > en.cap)
        {
            capped++;
            return;                                         // stays as Decimation spawns it
        }
        if ("infected".equals(en.mob))
        {
            tag(e.entityLiving, place, en.look);            // its look is put on when it joins (onJoin)
            if (n > 1)
            {
                spawn(e.world, place, en, e.x, e.y, e.z, n - 1, true);
            }
            return;
        }
        if (!((EntityLiving) e.entityLiving).getCanSpawnHere())
        {
            return;                                         // no spot for a mob: the game refuses it as it is
        }
        e.setResult(Event.Result.DENY);
        spawn(e.world, place, en, e.x, e.y, e.z, n, false);
    }

    private static void tag(Entity m, Places.Kind place, String look)
    {
        NBTTagCompound d = m.getEntityData();
        d.setString(PLACE, place.id);
        if (look != null)
        {
            d.setString(LOOK, look);
        }
    }

    /** n of the entry's mob on and round the spot (each on a free cell beside it, else on it), as SpawnerAnimals spawns. */
    private int spawn(World w, Places.Kind place, Entry en, float x, float y, float z, int n, boolean beside)
    {
        long start = System.nanoTime();
        int made = 0;
        for (int i = 0; i < n; i++)
        {
            EntityLiving m = Deci.newMob(en.mob, w);
            if (m == null)
            {
                break;
            }
            float mx = x, mz = z;
            if (i > 0 || beside)
            {
                int ox = random.nextInt(3) - 1, oz = random.nextInt(3) - 1;
                int bx = MathHelper.floor_float(x) + ox, by = MathHelper.floor_float(y), bz = MathHelper.floor_float(z) + oz;
                if (w.isAirBlock(bx, by, bz) && w.isAirBlock(bx, by + 1, bz) && !w.isAirBlock(bx, by - 1, bz))
                {
                    mx += ox;
                    mz += oz;
                }
            }
            m.setLocationAndAngles(mx, y, mz, random.nextFloat() * 360f, 0f);
            tag(m, place, "infected".equals(en.mob) ? en.look : null);
            if (w.spawnEntityInWorld(m))
            {
                made++;
                if (!ForgeEventFactory.doSpecialSpawn(m, w, mx, y, mz))
                {
                    m.onSpawnWithEgg(null);
                }
            }
        }
        spawned += made;
        spawnNanos += System.nanoTime() - start;
        return made;
    }

    /**
     * That mob within 64 blocks across and 32 up or down (only for an entry with a cap): a box search of the chunks'
     * entity lists, the few height sections near the spot only.
     */
    private static int near(World w, String mob, float x, float y, float z)
    {
        long start = System.nanoTime();
        int n = 0;
        List<?> l = w.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(x - 64, y - 32, z - 64, x + 64, y + 32, z + 64));
        for (int i = 0; i < l.size(); i++)
        {
            n += mob.equals(Deci.mobName((Entity) l.get(i))) ? 1 : 0;
        }
        searches++;
        searchNanos += System.nanoTime() - start;
        return n;
    }

    /** A look still to put on: after the zone handlers (their military / police looks) and before InfectedVariants' roll. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onJoin(EntityJoinWorldEvent e)
    {
        if (e.world.isRemote || e.isCanceled() || !Deci.isPlainInfected(e.entity))
        {
            return;
        }
        NBTTagCompound d = e.entity.getEntityData();
        if (!d.hasKey(LOOK))
        {
            return;
        }
        String look = d.getString(LOOK);
        d.removeTag(LOOK);
        if (DECIMATION_LOOKS.contains(look))
        {
            Deci.setInfectedVariant(e.entity, look.toUpperCase());
            return;
        }
        InfectedVariants v = InfectedVariants.instance();
        InfectedVariants.Variant ours = v == null ? null : v.byName(look);
        if (ours != null)
        {
            Deci.setInfectedVariant(e.entity, "COMMON");   // a zone's military / police look gives way to the place's
            v.apply((EntityLiving) e.entity, ours);
            looked++;
        }
    }

    // ------------------------------------------------------------ for the dev test

    /** Test switches: a place for every spawn, a fixed roll (-1: random), decisions counted but nothing changed. */
    public static volatile Places.Kind forcePlace;
    public static volatile int forceRoll = -1;
    public static volatile boolean dryRun;
    static long checks, checkNanos, spawnNanos, searches, searchNanos;
    static int capped, spawned, looked;

    /** For the dev test: the place's entries (null without a table entry for it). */
    public static List<Entry> entries(Places.Kind k)
    {
        return TABLE.get(k);
    }

    /** For the dev test: decisions per entry of a place since the last reset; the last number counts unchanged spawns. */
    public static int[] decisions(Places.Kind k)
    {
        int[] c = DECISIONS.get(k);
        return c == null ? new int[0] : c.clone();
    }

    public static void resetCounts()
    {
        for (int[] c : DECISIONS.values())
        {
            Arrays.fill(c, 0);
        }
        checks = checkNanos = spawnNanos = searches = searchNanos = 0;
        capped = spawned = looked = 0;
    }

    /** For the dev test: checks and their own time, the cap searches, capped, spawned, looks put on. */
    public static String counts()
    {
        return String.format("%d checks, %.2f us a check (cap searches and spawning apart), %d cap searches of %.1f us,"
            + " %d capped, %d spawned (%.1f us a mob with its join handlers), %d looks put on", checks, averageCheckMicros(),
            searches, searches == 0 ? 0.0 : searchNanos / 1000.0 / searches, capped, spawned,
            spawned == 0 ? 0.0 : spawnNanos / 1000.0 / spawned, looked);
    }

    /** For the dev test: our own time a check (place, roll, tag), without the cap searches and the mobs' spawning. */
    public static double averageCheckMicros()
    {
        return checks == 0 ? 0 : (checkNanos - spawnNanos - searchNanos) / 1000.0 / checks;
    }

    /** For the dev test: average time of a cap search in microseconds. */
    public static double averageSearchMicros()
    {
        return searches == 0 ? 0 : searchNanos / 1000.0 / searches;
    }
}
