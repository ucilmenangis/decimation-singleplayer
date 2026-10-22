package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;

/**
 * Loot refill (feature 12 A, user 22 Oktober 2026: "loot by default is empty forever after player loot it, except
 * multiplayer they can be disable or enable for minutes refill loots"; "15 minute use old decimation setting, for
 * singleplayer it has option on cfg file world too so they can use refill method or not").
 */
class LootRefillTest
{
    private static final LootRefill.Settings OFF = new LootRefill.Settings(false, 15);
    private static final LootRefill.Settings ON = new LootRefill.Settings(true, 15);

    @Test
    void neverLootedGivesLoot()
    {
        assertTrue(LootRefill.ready(-1, 0, OFF));
        assertTrue(LootRefill.ready(-1, 123456789L, ON));
    }

    @Test
    void refillOffStaysEmptyForever()
    {
        assertFalse(LootRefill.ready(100, 100, OFF));
        assertFalse(LootRefill.ready(100, 100 + 20L * 60 * 60 * 24 * 365 * 10, OFF), "ten years later");
    }

    @Test
    void refillOnAfterItsMinutesOfWorldTime()
    {
        long at = 5000000000L;
        assertFalse(LootRefill.ready(at, at, ON));
        assertFalse(LootRefill.ready(at, at + 15 * 1200 - 1, ON));
        assertTrue(LootRefill.ready(at, at + 15 * 1200, ON));
        LootRefill.Settings one = new LootRefill.Settings(true, 1);
        assertFalse(LootRefill.ready(at, at + 1199, one));
        assertTrue(LootRefill.ready(at, at + 1200, one));
        assertTrue(LootRefill.ready(at, at, new LootRefill.Settings(true, 0)), "0 minutes: at once");
    }

    @Test
    void defaultsSingleplayerKeepsLootedSpotsEmptyServersRefillOnDecimationsTime()
    {
        LootRefill.Settings sp = LootRefill.defaults(false, 900);
        assertFalse(sp.refill);
        assertEquals(15, sp.minutes);
        LootRefill.Settings server = LootRefill.defaults(true, 900);
        assertTrue(server.refill);
        assertEquals(15, server.minutes);
        assertEquals(10, LootRefill.defaults(true, 600).minutes, "an admin's own lootRespawnTime");
        assertEquals(1, LootRefill.defaults(true, 89).minutes, "rounded");
        assertEquals(0, LootRefill.defaults(true, 0).minutes);
    }

    @Test
    void keyIsDecimationsOwnPacking() throws Exception
    {
        Method pack = Class.forName("deci.aB.e").getDeclaredMethod("j", int.class, int.class, int.class);
        pack.setAccessible(true);
        int[][] spots = {{0, 0, 0}, {123, 64, -456}, {-30000000, 255, 29999999}, {-1, 1, -1}};
        for (int[] s : spots)
        {
            assertEquals(pack.invoke(null, s[0], s[1], s[2]), LootRefill.key(s[0], s[1], s[2]));
        }
    }

    @Test
    void recordKeepsSpotsPerDimensionThroughASave()
    {
        LootRefill.Record a = new LootRefill.Record(LootRefill.Record.NAME);
        a.put(0, -123, 64, 456, 5000000000L);
        a.put(0, 7, 255, -8, 20);
        a.put(-1, -123, 64, 456, 99);
        NBTTagCompound tag = new NBTTagCompound();
        a.writeToNBT(tag);
        LootRefill.Record b = new LootRefill.Record(LootRefill.Record.NAME);
        b.readFromNBT(tag);
        assertEquals(3, b.size());
        assertEquals(5000000000L, b.get(0, -123, 64, 456));
        assertEquals(20, b.get(0, 7, 255, -8));
        assertEquals(99, b.get(-1, -123, 64, 456));
        assertEquals(-1, b.get(1, -123, 64, 456), "another dimension");
        assertEquals(-1, b.get(0, -123, 65, 456), "another height");
        assertTrue(b.forget(0, 7, 255, -8));
        assertFalse(b.forget(0, 7, 255, -8));
        assertEquals(2, b.size());
    }

    @Test
    void pruneDropsOnlySpotsThatGiveLootAgain()
    {
        LootRefill.Record r = new LootRefill.Record(LootRefill.Record.NAME);
        r.put(0, 1, 1, 1, 0);
        r.put(0, 2, 2, 2, 18000);
        assertEquals(0, r.prune(18000, OFF), "refill off: every spot stays empty");
        assertEquals(1, r.prune(18000, ON));
        assertEquals(-1, r.get(0, 1, 1, 1));
        assertEquals(18000, r.get(0, 2, 2, 2));
    }

    @Test
    void settingsFileWrittenOnFirstLoadThenReadAsEdited(@TempDir File world) throws Exception
    {
        Field home = Class.forName("cpw.mods.fml.relauncher.FMLInjectionData").getDeclaredField("minecraftHome");
        home.setAccessible(true);
        if (home.get(null) == null)
        {
            home.set(null, world.getParentFile()); // Forge's Configuration names its file relative to the game folder
        }
        LootRefill.Settings first = LootRefill.settings(world, false, 900);
        assertFalse(first.refill);
        assertEquals(15, first.minutes);
        File f = new File(world, LootRefill.FILE);
        assertTrue(f.isFile(), "written on first load");
        Configuration cfg = new Configuration(f);
        cfg.get(LootRefill.CATEGORY, "refill", false).set(true);
        cfg.get(LootRefill.CATEGORY, "refillMinutes", 15).set(1);
        cfg.save();
        LootRefill.Settings edited = LootRefill.settings(world, false, 900);
        assertTrue(edited.refill, "the world's own file decides");
        assertEquals(1, edited.minutes);
        LootRefill.Settings server = LootRefill.settings(world, true, 600);
        assertTrue(server.refill);
        assertEquals(1, server.minutes, "an existing file is never reset by the defaults");
    }
}
