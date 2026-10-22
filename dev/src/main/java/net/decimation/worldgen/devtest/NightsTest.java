package net.decimation.worldgen.devtest;

import net.decimation.fixes.DarkNights;
import net.decimation.fixes.MoonClock;
import net.decimation.fixes.WorldClock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/**
 * DayZ dark nights (user, 22 Oktober 2026; fixes/NightLight, fixes/DarkNights), with the brightness slider at its
 * brightest the whole time (nights must ignore it). On the arena under the open sky, a row of torches in front: noon is
 * bright and our hook leaves vanilla's light map alone; midnight at a new moon is near black (open sky light 0.03 to
 * 0.06), vanilla's own map (our hook off) far brighter; midnight at a full moon 0.12 to 0.22; torch light and night
 * vision still light the night. Then a seed 1 city street at both moons. Shots of each. The rebuild cost (once a tick).
 * The moon phase comes from our day counter (fixes/MoonClock: Decimation puts world time back to 0 every day, so vanilla's
 * phase was a full moon every night; the first run of this test read a full moon where it had set day 5 by world time):
 * day 5 is a new moon, day 1 a full one. The world's day is put back at the end.
 */
public class NightsTest extends DevTestMode
{
    public String name() { return "nights"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    /** A seed 1 city street (the site shots' street, looking east). */
    private static final int CX = 1460, CZ = 272;
    private int ticks;
    private float gammaBefore;
    private long rebuildsAtNoon;
    private volatile Runnable onServer;
    private String spawningBefore = "true";
    private int dayBefore = 1;
    private double floorNew = -1, floorVanilla = -1;

    static double lum(int argb)
    {
        return (0.30 * (argb >> 16 & 255) + 0.59 * (argb >> 8 & 255) + 0.11 * (argb & 255)) / 255.0;
    }

    /**
     * Mean brightness of the floor in a saved shot (x 10 to 55 %, y 55 to 80 %: left of the gun, above the hotbar): what
     * reached the screen, whoever (OptiFine too) uploaded the light map last.
     */
    static double floor(Minecraft mc, String shot)
    {
        try
        {
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(new java.io.File(mc.mcDataDir, "screenshots/" + shot));
            double sum = 0;
            int n = 0;
            for (int y = im.getHeight() * 55 / 100; y < im.getHeight() * 80 / 100; y += 2)
            {
                for (int x = im.getWidth() * 10 / 100; x < im.getWidth() * 55 / 100; x += 2)
                {
                    sum += lum(im.getRGB(x, y));
                    n++;
                }
            }
            return n == 0 ? -1 : sum / n;
        }
        catch (java.io.IOException ex)
        {
            return -1;
        }
    }

    public boolean client(Minecraft mc)
    {
        ticks++;
        if (ticks == 1)
        {
            gammaBefore = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 1.0F;          // brightest: nights must ignore it
            // dry on the client too: the server's change below happens outside its weather tick, so no packet tells the
            // client the rain stopped (the second run read 0.4 of every value: rain hides the moon)
            mc.theWorld.getWorldInfo().setRaining(false);
            mc.theWorld.getWorldInfo().setThundering(false);
            mc.theWorld.setRainStrength(0F);
            mc.theWorld.setThunderStrength(0F);
            onServer = this::arena;
        }
        if (ticks == 20)
        {
            look(mc, 0F, 20F);
        }
        if (ticks == 30)
        {
            rebuildsAtNoon = DarkNights.rebuildCount();    // the client has had the noon time packet (one a second)
        }
        if (ticks == 50)
        {
            double l = lum(DarkNights.colour(15, 0));
            DevTestResults.check(name(), "noon: a bright day, vanilla's map left alone", String.format("%.3f, rebuilds %d", l,
                DarkNights.rebuildCount() - rebuildsAtNoon), l > 0.9 && DarkNights.rebuildCount() == rebuildsAtNoon, "over 0.9, 0 rebuilds");
            DevTestUtil.screenshot(mc, name(), "arena_noon");
            onServer = () -> night(5);                       // day 5: a new moon (phase 4), midnight
        }
        if (ticks == 100)
        {
            double l = lum(DarkNights.colour(15, 0)), torch = lum(DarkNights.colour(15, 14));
            DevTestResults.check(name(), "new moon midnight, open sky, brightest slider", String.format("%.3f (moon %.2f)", l,
                mc.theWorld.getCurrentMoonPhaseFactor()), l >= 0.03 && l <= 0.06, "0.03 to 0.06 (about 95 % dark)");
            DevTestResults.check(name(), "torch light at night", String.format("%.3f", torch), torch > 0.5, "over 0.5");
            DevTestUtil.screenshot(mc, name(), "arena_new_moon");
            floorNew = floor(mc, "arena_new_moon");
            DarkNights.setEnabled(false);                  // vanilla's map comes back with its next rebuild
        }
        if (ticks == 120)
        {
            double l = lum(DarkNights.colour(15, 0));
            DevTestResults.check(name(), "the same night with our hook off (vanilla)", String.format("%.3f", l), l > 0.4, "over 0.4");
            DevTestUtil.screenshot(mc, name(), "arena_new_moon_vanilla");
            floorVanilla = floor(mc, "arena_new_moon_vanilla");
            DevTestResults.check(name(), "on screen: the new moon floor against vanilla's", String.format("%.3f against %.3f", floorNew,
                floorVanilla), floorNew >= 0 && floorVanilla > 0 && floorNew < 0.35 * floorVanilla, "under 0.35 of vanilla's");
            DarkNights.setEnabled(true);
            onServer = () -> night(1);                     // day 1: a full moon (phase 0), midnight
        }
        if (ticks == 170)
        {
            double l = lum(DarkNights.colour(15, 0));
            DevTestResults.check(name(), "full moon midnight, open sky, brightest slider", String.format("%.3f (moon %.2f)", l,
                mc.theWorld.getCurrentMoonPhaseFactor()), l >= 0.12 && l <= 0.22, "0.12 to 0.22 (shapes)");
            DevTestUtil.screenshot(mc, name(), "arena_full_moon");
            double full = floor(mc, "arena_full_moon");
            DevTestResults.check(name(), "on screen: the full moon floor between", String.format("%.3f (new %.3f, vanilla %.3f)", full,
                floorNew, floorVanilla), full > floorNew && full < floorVanilla, "brighter than the new moon, darker than vanilla");
            mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 400));
            onServer = () -> DevTestUtil.player().addPotionEffect(new PotionEffect(Potion.nightVision.id, 400));
        }
        if (ticks == 190)
        {
            double l = lum(DarkNights.colour(15, 0));
            DevTestResults.check(name(), "night vision lights the night", String.format("%.3f", l), l > 0.4, "over 0.4");
            mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
            onServer = () ->
            {
                DevTestUtil.player().removePotionEffect(Potion.nightVision.id);
                city(5);
            };
        }
        if (ticks == 330)
        {
            look(mc, 270F, 5F);                            // east along the street
        }
        if (ticks == 340)
        {
            DevTestUtil.screenshot(mc, name(), "city_new_moon");
            onServer = () -> night(1);
        }
        if (ticks == 390)
        {
            DevTestUtil.screenshot(mc, name(), "city_full_moon");
            onServer = () -> time(6000);
        }
        if (ticks == 430)
        {
            DevTestUtil.screenshot(mc, name(), "city_noon");
            DevTestResults.value(name(), "rebuilds", String.format("%d, %.1f us each", DarkNights.rebuildCount(), DarkNights.averageMicros()));
            DevTestResults.check(name(), "rebuild cost", String.format("%.1f us", DarkNights.averageMicros()),
                DarkNights.rebuildCount() > 100 && DarkNights.averageMicros() < 100, "under 100 us, once a tick");
            mc.gameSettings.gammaSetting = gammaBefore;
            onServer = this::restore;
        }
        return ticks < 440;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    private static void look(Minecraft mc, float yaw, float pitch)
    {
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.rotationPitch = pitch;
    }

    private void arena()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        w.getWorldInfo().setRaining(false);
        w.getWorldInfo().setThundering(false);
        w.getWorldInfo().setRainTime(100000);          // no rain starting during the test
        w.getWorldInfo().setThunderTime(100000);
        w.setRainStrength(0F);
        w.setThunderStrength(0F);
        for (int i = -3; i <= 3; i++)
        {
            w.setBlock(X + i * 4, Y, Z + 12, Blocks.torch);
        }
        w.setWorldTime(6000);
        dayBefore = WorldClock.day();
        p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
    }

    private static void time(long t)
    {
        DevTestUtil.player().worldObj.setWorldTime(t);
    }

    /** Midnight of this day (our day counter: its moon). */
    private static void night(int day)
    {
        MoonClock.setDay(day);
        time(18000);
    }

    private static void city(int day)
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        night(day);
        p.setPositionAndUpdate(CX + 0.5, w.getTopSolidOrLiquidBlock(CX, CZ) + 0.1, CZ + 0.5);
    }

    private void restore()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        for (int i = -3; i <= 3; i++)
        {
            w.setBlockToAir(X + i * 4, Y, Z + 12);
        }
        w.setWorldTime(6000);
        MoonClock.setDay(dayBefore);
        p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
    }
}
