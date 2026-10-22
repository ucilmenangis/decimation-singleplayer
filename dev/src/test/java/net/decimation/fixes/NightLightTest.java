package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * DayZ dark nights (user, 22 Oktober 2026: "the night become so dark so its like dayz they almost black 95% in night
 * time"; chosen: the moon matters, nights ignore the brightness slider, daytime keeps it). NightLight builds Minecraft's
 * 16 x 16 light map as vanilla does (EntityRenderer.updateLightmap) with the night changed.
 */
class NightLightTest
{
    /** The overworld's light table (WorldProvider.generateLightBrightnessTable with no ambient light). */
    static float[] table()
    {
        float[] t = new float[16];
        for (int i = 0; i < 16; i++)
        {
            float f = 1.0F - i / 15.0F;
            t[i] = (1.0F - f) / (f * 3.0F + 1.0F);
        }
        return t;
    }

    static int sky(int sky, int block)
    {
        return sky * 16 + block;
    }

    /** Perceived brightness of a light map colour (0 black .. 1 white). */
    static double lum(int argb)
    {
        return (0.30 * (argb >> 16 & 255) + 0.59 * (argb >> 8 & 255) + 0.11 * (argb & 255)) / 255.0;
    }

    static int[] night(float moon, float gamma)
    {
        int[] out = new int[256];
        NightLight.fill(out, table(), 0.2F, 0F, moon, 0F, 0.1F, 0F, false, 0F, gamma);
        return out;
    }

    @Test
    void fullDaylightIsVanillaWhateverTheSlider()
    {
        for (float gamma : new float[] {0F, 0.5F, 1F})
        {
            for (float sun : new float[] {1.0F, 0.7F})               // clear and rainy noon
            {
                int[] ours = new int[256], vanilla = new int[256];
                NightLight.fill(ours, table(), sun, 1F, 0.5F, 0.3F, 0.2F, 0F, false, 0F, gamma);
                NightLight.vanilla(vanilla, table(), sun, 0.2F, 0F, false, 0F, gamma);
                assertArrayEquals(vanilla, ours, "gamma " + gamma + ", sun " + sun);
            }
        }
    }

    @Test
    void newMoonOpenSkyIsNearBlack()
    {
        double l = lum(night(0F, 0F)[sky(15, 0)]);
        assertTrue(l >= 0.03 && l <= 0.06, "new moon, open sky: " + l);
    }

    @Test
    void fullMoonLetsYouMakeOutShapes()
    {
        double l = lum(night(1F, 0F)[sky(15, 0)]);
        assertTrue(l >= 0.12 && l <= 0.22, "full moon, open sky: " + l);
        assertTrue(l > 2.5 * lum(night(0F, 0F)[sky(15, 0)]), "full moon clearly brighter than new");
    }

    @Test
    void theBrightnessSliderDoesNothingAtNight()
    {
        assertArrayEquals(night(0F, 0F), night(0F, 1F));
        assertArrayEquals(night(1F, 0F), night(1F, 1F));
    }

    @Test
    void vanillaNightsWereFarBrighter()
    {
        int[] vanilla = new int[256];
        NightLight.vanilla(vanilla, table(), 0.2F, 0.1F, 0F, false, 0F, 1F);
        assertTrue(lum(vanilla[sky(15, 0)]) > 0.4, "vanilla, brightest slider: " + lum(vanilla[sky(15, 0)]));
    }

    @Test
    void torchesLightTheNightAsTheDarkestVanillaSliderDoes()
    {
        int[] vanilla = new int[256];
        NightLight.vanilla(vanilla, table(), 0.2F, 0.1F, 0F, false, 0F, 0F);
        double ours = lum(night(0F, 1F)[sky(0, 14)]), theirs = lum(vanilla[sky(0, 14)]);
        assertTrue(ours >= 0.9 * theirs, "torch light " + ours + " against vanilla " + theirs);
        assertTrue(lum(night(0F, 1F)[sky(0, 0)]) < 0.01, "a dark cave at night is black: " + lum(night(0F, 1F)[sky(0, 0)]));
    }

    @Test
    void nightVisionStillLightsTheNight()
    {
        // vanilla's night vision scales the light map until its brightest channel (blue at night) is full: about 0.6
        int[] out = new int[256], vanilla = new int[256];
        NightLight.fill(out, table(), 0.2F, 0F, 0F, 0F, 0.1F, 0F, false, 1F, 0F);
        NightLight.vanilla(vanilla, table(), 0.2F, 0.1F, 0F, false, 1F, 0F);
        double ours = lum(out[sky(15, 0)]), theirs = lum(vanilla[sky(15, 0)]);
        assertTrue(ours >= 0.8 * theirs && ours > 10 * lum(night(0F, 0F)[sky(15, 0)]),
            "night vision, new moon: " + ours + " against vanilla's " + theirs);
    }

    @Test
    void duskBrightensSmoothly()
    {
        double last = -1;
        for (int k = 0; k <= 10; k++)
        {
            int[] out = new int[256];
            float day = k / 10F;
            NightLight.fill(out, table(), 0.2F + 0.8F * day, day, 0F, 0F, 0.1F, 0F, false, 0F, 0.5F);
            double l = lum(out[sky(15, 0)]);
            assertTrue(l >= last, "day " + day + ": " + l + " after " + last);
            last = l;
        }
    }

    @Test
    void daylightFromTheSunsHeight()
    {
        assertEquals(1F, NightLight.daylight(0F), 1e-6);          // noon
        assertEquals(0F, NightLight.daylight(0.5F), 1e-6);        // midnight
        float dusk = NightLight.daylight(0.24F);
        assertTrue(dusk > 0F && dusk < 1F, "dusk " + dusk);
    }

    @Test
    void rainHidesTheMoon()
    {
        int[] wet = new int[256];
        NightLight.fill(wet, table(), 0.2F, 0F, 1F, 1F, 0.1F, 0F, false, 0F, 0F);
        assertTrue(lum(wet[sky(15, 0)]) < 0.7 * lum(night(1F, 0F)[sky(15, 0)]), "rain at full moon darker");
    }
}
