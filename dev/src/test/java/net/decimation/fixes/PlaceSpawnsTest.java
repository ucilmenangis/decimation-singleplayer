package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Specific mobs at specific places (user, 22 Oktober 2026, idea 35): a table per place kind, editable in a config file:
 * the wasteland irradiated and military infected, demons at night; bases and airports soldiers; hospitals medic infected;
 * police stations riot infected; forests infected dogs; highways bandit groups.
 */
class PlaceSpawnsTest
{
    private static PlaceSpawns.Entry e(String mob, String look, int chance, String time)
    {
        PlaceSpawns.Entry x = new PlaceSpawns.Entry();
        x.mob = mob;
        x.look = look;
        x.chance = chance;
        x.time = time;
        return x;
    }

    @Test
    void theRollWalksTheEntriesInOrder()
    {
        List<PlaceSpawns.Entry> l = Arrays.asList(e("infected", "irradiated", 30, "any"), e("infected", "military", 30, "any"),
                                                  e("demon", null, 2, "night"));
        assertSame(l.get(0), PlaceSpawns.pick(l, 0, false));
        assertSame(l.get(0), PlaceSpawns.pick(l, 29, false));
        assertSame(l.get(1), PlaceSpawns.pick(l, 30, false));
        assertSame(l.get(1), PlaceSpawns.pick(l, 59, true));
        assertNull(PlaceSpawns.pick(l, 60, false));                      // the demon only at night
        assertSame(l.get(2), PlaceSpawns.pick(l, 60, true));
        assertSame(l.get(2), PlaceSpawns.pick(l, 61, true));
        assertNull(PlaceSpawns.pick(l, 62, true));                       // the rest stays as Decimation spawns it
    }

    @Test
    void dayEntriesOnlyByDay()
    {
        List<PlaceSpawns.Entry> l = Arrays.asList(e("bandit", null, 10, "day"), e("infected_dog", null, 10, "any"));
        assertSame(l.get(0), PlaceSpawns.pick(l, 5, false));
        assertSame(l.get(1), PlaceSpawns.pick(l, 5, true));               // at night the day entry is skipped
        assertNull(PlaceSpawns.pick(l, 15, true));
    }

    @Test
    void nightIsFromDuskToDawn()
    {
        assertFalse(PlaceSpawns.night(6000));
        assertFalse(PlaceSpawns.night(12999));
        assertTrue(PlaceSpawns.night(13000));
        assertTrue(PlaceSpawns.night(22999));
        assertFalse(PlaceSpawns.night(23000));
        assertTrue(PlaceSpawns.night(24000 + 18000));                    // world time past a day counts by the time of day
    }

    @Test
    void theUsersTableIsTheDefault()
    {
        Map<String, List<PlaceSpawns.Entry>> d = PlaceSpawns.defaults().places;
        assertEquals(new HashSet<String>(Arrays.asList("wasteland", "base", "airport", "hospital", "police_station", "forest",
                                                       "highway")), d.keySet());
        assertTrue(has(d.get("wasteland"), "infected", "irradiated", "any"));
        assertTrue(has(d.get("wasteland"), "infected", "military", "any"));
        assertTrue(has(d.get("wasteland"), "demon", null, "night"));
        assertTrue(has(d.get("base"), "soldier", null, "any"));
        assertTrue(has(d.get("airport"), "soldier", null, "any"));
        assertTrue(has(d.get("hospital"), "infected", "medic", "any"));
        assertTrue(has(d.get("police_station"), "infected", "riot", "any"));
        assertTrue(has(d.get("forest"), "infected_dog", null, "any"));
        assertTrue(has(d.get("highway"), "bandit", null, "any"));
        assertTrue(d.get("highway").get(0).group[1] >= 2);                   // bandit GROUPS
        assertEquals(0, PlaceSpawns.check(d, Arrays.asList("runner", "riot", "screamer", "irradiated", "medic")).size());
    }

    private static boolean has(List<PlaceSpawns.Entry> l, String mob, String look, String time)
    {
        for (PlaceSpawns.Entry x : l)
        {
            if (x.mob.equals(mob) && (look == null ? x.look == null : look.equals(x.look)) && x.time.equals(time) && x.chance > 0)
            {
                return true;
            }
        }
        return false;
    }

    @Test
    void mistakesInTheFileAreReported()
    {
        Map<String, List<PlaceSpawns.Entry>> m = new LinkedHashMap<String, List<PlaceSpawns.Entry>>();
        m.put("forest", Arrays.asList(e("infected_dog", null, 30, "any"), e("dragon", null, 5, "any"), e("infected", "pirate", 5, "any"),
                                      e("bandit", null, 5, "noon")));
        m.put("moon", Arrays.asList(e("infected", "riot", 5, "any")));
        m.put("hospital", Arrays.asList(e("infected", "medic", 70, "any"), e("infected", "riot", 40, "any")));
        List<String> p = PlaceSpawns.check(m, Arrays.asList("riot", "medic"));
        assertEquals(5, p.size(), p.toString());   // dragon, pirate, noon, moon, hospital over 100 %
    }

    @Test
    void theFileIsWrittenOnceThenTheUsers(@TempDir File dir) throws Exception
    {
        PlaceSpawns.FileData first = PlaceSpawns.read(dir);
        assertTrue(new File(dir, PlaceSpawns.FILE).isFile());
        assertEquals(PlaceSpawns.VERSION, first.version);
        File f = new File(dir, PlaceSpawns.FILE);
        String edited = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8").replace("\"infected_dog\"", "\"crawler\"");
        java.nio.file.Files.write(f.toPath(), edited.getBytes("UTF-8"));
        PlaceSpawns.FileData again = PlaceSpawns.read(dir);
        assertEquals("crawler", again.places.get("forest").get(0).mob);     // the user's edit stays
    }
}
