package net.decimation.worldgen.devtest;

import java.util.List;

import cpw.mods.fml.common.eventhandler.Event;
import net.decimation.fixes.Deci;
import net.decimation.fixes.InfectedVariants;
import net.decimation.fixes.NpcLoadouts;
import net.decimation.fixes.PlaceSpawns;
import net.decimation.worldgen.Places;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.city.Airport;
import net.decimation.worldgen.city.Highways;
import net.decimation.worldgen.city.LcCity;
import net.decimation.worldgen.city.Wasteland;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Specific mobs at specific places (feature 8, user 22 Oktober 2026, docs/ideas.md 35).
 * - Places: a position of each kind found from the seed's own sources (Wasteland, Airport, LcCity's buildings,
 *   Highways, the biome, a test base among StructureData's named places) where only that kind holds; Places.at must say
 *   so.
 * - The table: every place kind has entries, no problems, the file written.
 * - Rolls (dry run, 2000 spawn checks a place through Forge's own CheckSpawn event, at day and the wasteland at night):
 *   each entry's share against its chance (4 sigma).
 * - The real path on the arena (a place forced, the roll fixed): looks put on when the infected joins (medic, riot,
 *   irradiated, military), replacements spawned for an infected that is refused (bandit group, soldiers, dogs, a demon
 *   at night), a cap that keeps the infected.
 * - Natural spawning for 20 s with the forest forced: its checks went through the table, dogs among the spawns.
 * - Cost: Places.at a lookup (memoised) and its allocation, the spawn check's time.
 */
public class PlaceSpawnTest extends DevTestMode
{
    public String name() { return "placespawns"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    /** The test's spawn spot on the arena floor, 20 blocks from the player (a spot in the player fails getCanSpawnHere). */
    private static final int SX = X - 20;
    private static final String TEST_BASE = "placespawns_test_base";
    private volatile int ticks;
    private volatile boolean done;
    private String spawningBefore = "true";
    private EnumDifficulty difficultyBefore;
    private WorldSettings.GameType modeBefore;
    private long timeBefore;
    private final java.util.Map<Places.Kind, int[]> found = new java.util.EnumMap<Places.Kind, int[]>(Places.Kind.class);
    private EntityLiving pending;
    private String pendingLook;
    private Places.Kind pendingPlace;

    public boolean client(Minecraft mc)
    {
        return !done;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null || done)
        {
            return;
        }
        World w = p.worldObj;
        switch (t)
        {
            case 20: setup(p, w); break;
            case 30: places(w); break;
            case 40: table(); break;
            case 50: rolls(w); break;
            // looks: the infected is spawned as SpawnerAnimals does after the check, its look put on at join
            case 60: look(w, Places.Kind.HOSPITAL, 0, "medic"); break;
            case 64: lookCheck(w, "marineVestMedical", false); break;
            case 70: look(w, Places.Kind.POLICE_STATION, 0, "riot"); break;
            case 74: lookCheck(w, "nypdCoat", true); break;
            case 80: look(w, Places.Kind.WASTELAND, 0, "irradiated"); break;
            case 84: lookCheck(w, "hazmatVest", false); break;
            case 90: look(w, Places.Kind.WASTELAND, 30, "military"); break;
            case 94: lookCheck(w, null, false); break;
            // replacements
            case 100: replace(w, Places.Kind.HIGHWAY, 0); break;
            case 104: replaceCheck(w, Places.Kind.HIGHWAY, "bandit", 2, 4); break;
            case 110: capCheck(w); break;
            case 120: replace(w, Places.Kind.BASE, 0); break;
            case 124: replaceCheck(w, Places.Kind.BASE, "soldier", 2, 3); break;
            case 130: replace(w, Places.Kind.FOREST, 0); break;
            case 134: replaceCheck(w, Places.Kind.FOREST, "infected_dog", 1, 2); break;
            case 140: w.setWorldTime(18000); replace(w, Places.Kind.WASTELAND, 60); break;
            case 144: replaceCheck(w, Places.Kind.WASTELAND, "demon", 1, 1); w.setWorldTime(6000); break;
            // natural spawning, the forest forced on every spawn
            case 150: natural(w, true); break;
            case 550: natural(w, false); break;
            case 560: cost(w); break;
            case 570: finish(p, w); break;
            default: break;
        }
    }

    private void setup(EntityPlayerMP p, World w)
    {
        DevTestArena.build(w, p);
        timeBefore = w.getWorldTime();
        w.setWorldTime(6000);
        w.getWorldInfo().setRaining(false);
        w.getWorldInfo().setThundering(false);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(WorldSettings.GameType.CREATIVE);   // the soldiers, bandits and the demon leave a creative player be
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        difficultyBefore = w.difficultySetting;
        w.difficultySetting = EnumDifficulty.NORMAL;        // EntityMob.getCanSpawnHere refuses every spawn in peaceful
        w.setAllowedSpawnTypes(true, true);
        PlaceSpawns.forcePlace = null;
        PlaceSpawns.forceRoll = -1;
        PlaceSpawns.dryRun = false;
        Places.reset();
    }

    // ------------------------------------------------------------ places

    private void places(World w)
    {
        long seed = w.getSeed(), start = System.currentTimeMillis();
        int cell = LcCity.CELL, per = Sectors.REGION / cell;
        // wasteland and airport: the wasteland sectors near the origin
        for (int r = 0; r <= 12 && (!found.containsKey(Places.Kind.WASTELAND) || !found.containsKey(Places.Kind.AIRPORT)); r++)
        {
            for (int rx = -r; rx <= r; rx++)
            {
                for (int rz = -r; rz <= r; rz++)
                {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != r || !Wasteland.sector(seed, rx, rz))
                    {
                        continue;
                    }
                    int[] a = Airport.site(seed, rx, rz);
                    if (a != null && !found.containsKey(Places.Kind.AIRPORT))
                    {
                        found.put(Places.Kind.AIRPORT, new int[] {(a[0] * cell + a[2] * cell / 2) * 16, (a[1] * cell + a[3] * cell / 2) * 16});
                    }
                    for (int cx = rx * per; cx < rx * per + per && !found.containsKey(Places.Kind.WASTELAND); cx++)
                    {
                        for (int cz = rz * per; cz < rz * per + per && !found.containsKey(Places.Kind.WASTELAND); cz++)
                        {
                            boolean airport = a != null && cx >= a[0] && cz >= a[1] && cx < a[0] + a[2] && cz < a[1] + a[3];
                            int x = cx * cell * 16 + 8, z = cz * cell * 16 + 8;
                            if (!airport && Wasteland.chunk(seed, x >> 4, z >> 4) && LcCity.buildingKind(seed, x, z) == null)
                            {
                                found.put(Places.Kind.WASTELAND, new int[] {x, z});
                            }
                        }
                    }
                }
            }
        }
        // police stations and hospitals: city buildings, cells outward from the origin (10 s at most)
        for (int r = 0; r <= 40 && System.currentTimeMillis() - start < 10000
            && (!found.containsKey(Places.Kind.POLICE_STATION) || !found.containsKey(Places.Kind.HOSPITAL)); r++)
        {
            for (int cx = -r; cx <= r; cx++)
            {
                for (int cz = -r; cz <= r; cz++)
                {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) != r || !LcCity.isCity(seed, cx, cz) || Wasteland.chunk(seed, cx * cell, cz * cell))
                    {
                        continue;
                    }
                    for (int i = 0; i < cell * cell; i++)
                    {
                        int x = (cx * cell + i % cell) * 16 + 8, z = (cz * cell + i / cell) * 16 + 8;
                        String k = LcCity.buildingKind(seed, x, z);
                        Places.Kind kind = "police".equals(k) ? Places.Kind.POLICE_STATION : "medical".equals(k) ? Places.Kind.HOSPITAL : null;
                        if (kind != null && !found.containsKey(kind))
                        {
                            found.put(kind, new int[] {x, z});
                        }
                    }
                }
            }
        }
        // a highway chunk and a forest outside cities
        for (int r = 0; r <= 300 && (!found.containsKey(Places.Kind.HIGHWAY) || !found.containsKey(Places.Kind.FOREST)); r += 2)
        {
            for (int a = 0; a < 16; a++)
            {
                int chx = (int) Math.round(r * Math.cos(a * Math.PI / 8)), chz = (int) Math.round(r * Math.sin(a * Math.PI / 8));
                int x = chx * 16 + 8, z = chz * 16 + 8;
                boolean city = LcCity.isCity(seed, Math.floorDiv(chx, cell), Math.floorDiv(chz, cell));
                if (!found.containsKey(Places.Kind.HIGHWAY) && Highways.at(seed, chx, chz) && !city)
                {
                    found.put(Places.Kind.HIGHWAY, new int[] {x, z});
                }
                BiomeGenBase b = w.getWorldChunkManager().getBiomeGenAt(x, z);
                if (!found.containsKey(Places.Kind.FOREST) && !city && !Highways.at(seed, chx, chz)
                    && BiomeDictionary.isBiomeOfType(b, BiomeDictionary.Type.FOREST))
                {
                    found.put(Places.Kind.FOREST, new int[] {x, z});
                }
            }
        }
        // a base: a named place of the military as PlaceNames stores one, far from everything
        StructureData.get(w).setPlace(TEST_BASE, "-29000/0/-29000=-28900/255/-28900=Military=FOB_Test");
        found.put(Places.Kind.BASE, new int[] {-28950, -28950});
        DevTestResults.value(name(), "search", (System.currentTimeMillis() - start) + " ms");
        for (Places.Kind k : Places.Kind.values())
        {
            int[] at = found.get(k);
            Places.Kind said = at == null ? null : Places.at(w, at[0], at[1]);
            DevTestResults.check(name(), "place " + k.id, at == null ? "none found" : at[0] + "," + at[1] + " is " + said, said == k,
                k.id + " (only it holds there)");
        }
        DevTestResults.check(name(), "outside the test base", String.valueOf(Places.at(w, -29100, -28950)),
            Places.at(w, -29100, -28950) != Places.Kind.BASE, "not base");
    }

    // ------------------------------------------------------------ the table and its rolls

    private void table()
    {
        int with = 0;
        for (Places.Kind k : Places.Kind.values())
        {
            List<PlaceSpawns.Entry> l = PlaceSpawns.entries(k);
            with += l != null && !l.isEmpty() ? 1 : 0;
        }
        java.io.File f = new java.io.File(new java.io.File(Minecraft.getMinecraft().mcDataDir, "config"), "deciworldgen_places.json");
        DevTestResults.check(name(), "the table", with + " of 7 places with entries, problems " + PlaceSpawns.problems + ", file "
            + f.isFile(), with == 7 && PlaceSpawns.problems.isEmpty() && f.isFile(), "7 of 7, none, written");
    }

    private void rolls(World w)
    {
        PlaceSpawns.dryRun = true;
        PlaceSpawns.resetCounts();
        for (Places.Kind k : Places.Kind.values())
        {
            rollPlace(w, k, false);
        }
        w.setWorldTime(18000);
        PlaceSpawns.resetCounts();
        rollPlace(w, Places.Kind.WASTELAND, true);
        w.setWorldTime(6000);
        PlaceSpawns.dryRun = false;
    }

    private void rollPlace(World w, Places.Kind k, boolean night)
    {
        int n = 2000;
        PlaceSpawns.forcePlace = k;
        for (int i = 0; i < n; i++)
        {
            ForgeEventFactory.canEntitySpawn(infected(w), w, SX + 0.5f, Y, Z + 0.5f);
        }
        PlaceSpawns.forcePlace = null;
        List<PlaceSpawns.Entry> l = PlaceSpawns.entries(k);
        int[] got = PlaceSpawns.decisions(k);
        StringBuilder s = new StringBuilder();
        boolean ok = got.length == l.size() + 1;
        for (int i = 0; i < l.size() && ok; i++)
        {
            PlaceSpawns.Entry e = l.get(i);
            double chance = (night ? "day".equals(e.time) : "night".equals(e.time)) ? 0 : e.chance / 100.0;
            double expected = n * chance, sigma = Math.sqrt(n * chance * (1 - chance));
            ok &= Math.abs(got[i] - expected) <= 4 * sigma + 2;
            s.append(e.mob).append(e.look == null ? "" : ":" + e.look).append(" ").append(got[i]).append(" (").append((int) expected)
             .append(") ");
        }
        s.append("unchanged ").append(got.length == 0 ? 0 : got[got.length - 1]);
        DevTestResults.check(name(), "rolls " + k.id + (night ? " at night" : ""), s.toString().trim(), ok, "each within 4 sigma of its chance");
    }

    private static EntityLiving infected(World w)
    {
        EntityLiving z = Deci.newInfected(w);
        z.setLocationAndAngles(SX + 0.5, Y, Z + 0.5, 0, 0);
        return z;
    }

    // ------------------------------------------------------------ the real path

    private void look(World w, Places.Kind k, int roll, String look)
    {
        DevTestArena.clear(w, null);
        PlaceSpawns.forcePlace = k;
        PlaceSpawns.forceRoll = roll;
        for (int i = 0; i < 6; i++)                         // Decimation turns 1 in 10 infected into a hulk or bloater
        {
            EntityLiving z = infected(w);
            Event.Result r = ForgeEventFactory.canEntitySpawn(z, w, SX + 0.5f, Y, Z + 0.5f);
            if (r != Event.Result.DENY && w.spawnEntityInWorld(z))
            {
                pending = z;
                break;
            }
        }
        PlaceSpawns.forcePlace = null;
        PlaceSpawns.forceRoll = -1;
        pendingLook = look;
        pendingPlace = k;
    }

    private void lookCheck(World w, String chest, boolean helmet)
    {
        EntityLiving z = pending;
        ItemStack c = z == null ? null : z.getEquipmentInSlot(3), h = z == null ? null : z.getEquipmentInSlot(4);
        String chestName = c == null ? "none" : cpw.mods.fml.common.registry.GameRegistry.findUniqueIdentifierFor(c.getItem()).name;
        String variant = z == null ? "" : z.getEntityData().getString(InfectedVariants.TAG);
        String place = z == null ? "" : z.getEntityData().getString(PlaceSpawns.PLACE);
        boolean lookLeft = z != null && z.getEntityData().hasKey(PlaceSpawns.LOOK);
        boolean ok;
        String got;
        if (chest == null)                                  // Decimation's military look: its variant, no armour of ours
        {
            int v = z == null ? -1 : Deci.infectedVariant(z);
            got = "variant " + v + ", place " + place;
            ok = z != null && !z.isDead && v == 1 && pendingPlace.id.equals(place) && !lookLeft;
        }
        else
        {
            got = "chest " + chestName + ", helmet " + (h != null) + ", our look " + variant + ", place " + place;
            ok = z != null && !z.isDead && chest.equals(chestName) && (h != null) == helmet && pendingLook.equals(variant)
                && pendingPlace.id.equals(place) && !lookLeft && Deci.infectedVariant(z) == 0;
        }
        DevTestResults.check(name(), "look " + pendingLook + " at a " + pendingPlace.id, got, ok,
            chest == null ? "Decimation's military variant (1), place tag" : chest + (helmet ? " and a helmet" : ", no helmet")
                + ", look " + pendingLook + ", place tag");
        pending = null;
    }

    private void replace(World w, Places.Kind k, int roll)
    {
        sweep(w);                                           // mobs left from earlier runs would count against a cap
        PlaceSpawns.forcePlace = k;
        PlaceSpawns.forceRoll = roll;
        EntityLiving z = infected(w);
        Event.Result r = ForgeEventFactory.canEntitySpawn(z, w, SX + 0.5f, Y, Z + 0.5f);
        PlaceSpawns.forcePlace = null;
        PlaceSpawns.forceRoll = -1;
        pendingPlace = k;
        pendingLook = r.name();
    }

    private void replaceCheck(World w, Places.Kind k, String mob, int least, int most)
    {
        int n = 0, tiered = 0, swapped = 0;
        for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(SX - 6, Y - 2, Z - 6, SX + 7, Y + 6, Z + 7)))
        {
            EntityLiving m = (EntityLiving) o;
            if (m.isDead)
            {
                continue;
            }
            if (k.id.equals(m.getEntityData().getString(PlaceSpawns.PLACE)) && mob.equals(Deci.mobName(m)))
            {
                n++;
                tiered += m.getEntityData().hasKey(NpcLoadouts.TAG) ? 1 : 0;
            }
            else if (Deci.isMech(m) || "hulk".equals(Deci.mobName(m)) || "bloater".equals(Deci.mobName(m)))
            {
                swapped++;                                  // Decimation's own swaps at join (mechs for soldiers, hulks)
            }
        }
        boolean human = "soldier".equals(mob) || "bandit".equals(mob);
        DevTestResults.check(name(), mob + " for an infected at a " + k.id, "infected " + pendingLook + ", " + n + " " + mob
            + (human ? ", " + tiered + " with a tier" : "") + (swapped > 0 ? ", " + swapped + " swapped by Decimation" : ""),
            "DENY".equals(pendingLook) && n + swapped >= least && n + swapped <= most && n >= 1 && (!human || tiered == n),
            "infected refused, " + least + " to " + most + " " + mob + (human ? " with NpcLoadouts tiers" : ""));
        if (!"bandit".equals(mob))
        {
            DevTestArena.clear(w, null);
        }
    }

    /** With the highway's bandit cap (8) reached round the spot, a bandit roll keeps the infected. */
    private void capCheck(World w)
    {
        int have = 0;
        for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(SX - 64, Y - 32, Z - 64, SX + 64, Y + 32, Z + 64)))
        {
            have += "bandit".equals(Deci.mobName((Entity) o)) && !((Entity) o).isDead ? 1 : 0;
        }
        for (int i = have; i < 8; i++)
        {
            EntityLiving b = Deci.newBandit(w);
            b.setLocationAndAngles(SX + 0.5 - 10 + 2 * i, Y, Z + 0.5 - 12, 0, 0);
            w.spawnEntityInWorld(b);
        }
        PlaceSpawns.forcePlace = Places.Kind.HIGHWAY;
        PlaceSpawns.forceRoll = 0;
        EntityLiving z = infected(w);
        Event.Result r = ForgeEventFactory.canEntitySpawn(z, w, SX + 0.5f, Y, Z + 0.5f);
        PlaceSpawns.forcePlace = null;
        PlaceSpawns.forceRoll = -1;
        int after = 0;
        for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(SX - 64, Y - 32, Z - 64, SX + 64, Y + 32, Z + 64)))
        {
            after += "bandit".equals(Deci.mobName((Entity) o)) && !((Entity) o).isDead ? 1 : 0;
        }
        DevTestResults.check(name(), "bandit cap 8 reached", "infected " + r + ", bandits " + after, r != Event.Result.DENY && after >= 8
            && after <= Math.max(8, have), "the infected kept, no new bandits");
        DevTestArena.clear(w, null);
    }

    // ------------------------------------------------------------ natural spawning

    private int dogsBefore;

    private void natural(World w, boolean start)
    {
        if (start)
        {
            DevTestArena.clear(w, null);
            sweep(w);
            PlaceSpawns.resetCounts();
            PlaceSpawns.forcePlace = Places.Kind.FOREST;
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "true");
            dogsBefore = dogs(w);
            return;
        }
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        PlaceSpawns.forcePlace = null;
        int[] d = PlaceSpawns.decisions(Places.Kind.FOREST);
        int checks = 0;
        for (int c : d)
        {
            checks += c;
        }
        int dogs = dogs(w) - dogsBefore;
        DevTestResults.value(name(), "natural spawns in 20 s", PlaceSpawns.counts());
        DevTestResults.check(name(), "natural spawns go through the table", checks + " infected spawn checks, " + d[0]
            + " became dogs (" + dogs + " forest dogs in the world now)", checks >= 10 && d[0] >= 1 && dogs >= 1,
            "10 or more checks, some dogs");
        DevTestResults.check(name(), "spawn check cost", String.format("%.2f us a check, cap search %.1f us",
            PlaceSpawns.averageCheckMicros(), PlaceSpawns.averageSearchMicros()), PlaceSpawns.averageCheckMicros() < 10
            && PlaceSpawns.averageSearchMicros() < 500, "under 10 us a check (place, roll, tag), a cap search under 500 us");
        sweep(w);
    }

    private static int dogs(World w)
    {
        int n = 0;
        for (Object o : w.loadedEntityList)
        {
            Entity e = (Entity) o;
            n += !e.isDead && "infected_dog".equals(Deci.mobName(e)) && "forest".equals(e.getEntityData().getString(PlaceSpawns.PLACE)) ? 1 : 0;
        }
        return n;
    }

    /** Every mob within 160 blocks of the arena. */
    private static void sweep(World w)
    {
        for (Object o : w.loadedEntityList.toArray())
        {
            Entity e = (Entity) o;
            if (!e.isDead && e instanceof EntityLiving && Math.abs(e.posX - X) < 160 && Math.abs(e.posZ - Z) < 160)
            {
                e.setDead();
            }
        }
    }

    // ------------------------------------------------------------ cost and the end

    private void cost(World w)
    {
        int[][] at = found.values().toArray(new int[0][]);
        long[] best = {Long.MAX_VALUE, Long.MAX_VALUE};
        for (int round = 0; round < 3; round++)
        {
            long b0 = allocated(), t0 = System.nanoTime();
            for (int i = 0; i < 10000; i++)
            {
                int[] a = at[i % at.length];
                Places.at(w, a[0] + (i & 7), a[1]);
            }
            long nanos = System.nanoTime() - t0, bytes = allocated() - b0;
            best[0] = Math.min(best[0], nanos);
            best[1] = Math.min(best[1], bytes);
        }
        DevTestResults.check(name(), "place lookup cost", String.format("%.3f us a lookup, %d bytes in 10000 (steadiest of 3)",
            best[0] / 1000.0 / 10000, best[1]), best[0] / 1000.0 / 10000 < 5 && best[1] <= 64, "under 5 us, nothing allocated");
    }

    private static long allocated()
    {
        java.lang.management.ThreadMXBean b = java.lang.management.ManagementFactory.getThreadMXBean();
        return b instanceof com.sun.management.ThreadMXBean
            ? ((com.sun.management.ThreadMXBean) b).getThreadAllocatedBytes(Thread.currentThread().getId()) : 0;
    }

    private void finish(EntityPlayerMP p, World w)
    {
        StructureData.get(w).removePlace(TEST_BASE);
        Places.reset();
        PlaceSpawns.forcePlace = null;
        PlaceSpawns.forceRoll = -1;
        PlaceSpawns.dryRun = false;
        DevTestArena.clear(w, null);
        w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        w.difficultySetting = difficultyBefore;
        w.setAllowedSpawnTypes(difficultyBefore != EnumDifficulty.PEACEFUL, true);
        w.setWorldTime(timeBefore);
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);
        }
        done = true;
    }
}
