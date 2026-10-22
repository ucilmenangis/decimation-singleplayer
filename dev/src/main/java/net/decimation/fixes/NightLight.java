package net.decimation.fixes;

/**
 * Minecraft's light map (a 16 x 16 texture: sky light by block light, the colour every lit face is multiplied by), as
 * vanilla builds it in EntityRenderer.updateLightmap, with DayZ nights (user, 22 Oktober 2026: "the night become so dark
 * so its like dayz they almost black 95% in night time"; chosen: the moon matters, nights ignore the brightness slider,
 * daytime keeps it). Three things change with the night share n = 1 - daylight: the sky term at night comes from the
 * moon (0.09 at a new moon to 0.36 at a full one, down to 0.4 of that in rain) instead of vanilla's 0.24; the two 0.03
 * ambient floors shrink to nothing; the brightness slider (gamma) is weighted by daylight. At full daylight the result is
 * vanilla's to the bit. Block light (torches, our street lights), lightning and night vision work as in vanilla.
 * Open sky at midnight, perceived brightness: new moon about 0.045 (95 % dark), full moon about 0.17; vanilla gave 0.16
 * at the darkest slider setting and 0.47 at the brightest. Pure: fixes/DarkNights feeds it and uploads the result.
 */
public final class NightLight
{
    private NightLight()
    {
    }

    /** How much of the day the sun gives, 0 at night to 1 by day, without weather (World.getSunBrightnessBody's curve). */
    public static float daylight(float celestialAngle)
    {
        float f = (float) Math.cos(celestialAngle * Math.PI * 2.0) * 2.0F + 0.2F;
        return f < 0F ? 0F : f > 1F ? 1F : f;
    }

    /**
     * Fills the 256 colours. sun: World.getSunBrightness (0.2 at night, weather included); day: daylight(); moon:
     * World.getCurrentMoonPhaseFactor (1 full, 0 new); rain: World.getRainStrength; torch: the torch flicker; boss: the
     * boss colour tint now; nightVision: 0 or the night vision strength (vanilla's getNightVisionBrightness).
     */
    public static void fill(int[] out, float[] table, float sun, float day, float moon, float rain, float torch, float boss,
                            boolean lightning, float nightVision, float gamma)
    {
        float night = 1F - day;
        float skyDay = sun * 0.95F + 0.05F;
        float skyNight = (0.09F + 0.27F * moon) * (1F - 0.6F * rain);
        float skyFactor = day >= 1F ? skyDay : skyDay * day + skyNight * night;
        float floor = 0.03F * day;
        float gammaNow = gamma * day;
        build(out, table, skyFactor, sun, torch, boss, lightning, nightVision, gammaNow, floor);
    }

    /** Vanilla's light map for the overworld (the reference: fill gives exactly this at full daylight). */
    public static void vanilla(int[] out, float[] table, float sun, float torch, float boss, boolean lightning, float nightVision,
                               float gamma)
    {
        build(out, table, sun * 0.95F + 0.05F, sun, torch, boss, lightning, nightVision, gamma, 0.03F);
    }

    /** EntityRenderer.updateLightmap's loop, with the sky factor, gamma and the ambient floor given. */
    private static void build(int[] out, float[] table, float skyFactor, float sun, float torch, float boss, boolean lightning,
                              float nightVision, float gamma, float floor)
    {
        for (int i = 0; i < 256; ++i)
        {
            float f2 = table[i / 16] * skyFactor;
            float f3 = table[i % 16] * (torch * 0.1F + 1.5F);
            if (lightning)
            {
                f2 = table[i / 16];
            }
            float f4 = f2 * (sun * 0.65F + 0.35F);
            float f5 = f2 * (sun * 0.65F + 0.35F);
            float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
            float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
            float r = (f4 + f3) * 0.96F + floor;
            float g = (f5 + f6) * 0.96F + floor;
            float b = (f2 + f7) * 0.96F + floor;
            if (boss > 0.0F)
            {
                r = r * (1.0F - boss) + r * 0.7F * boss;
                g = g * (1.0F - boss) + g * 0.6F * boss;
                b = b * (1.0F - boss) + b * 0.6F * boss;
            }
            if (nightVision > 0F)
            {
                float k = 1.0F / r;
                if (k > 1.0F / g)
                {
                    k = 1.0F / g;
                }
                if (k > 1.0F / b)
                {
                    k = 1.0F / b;
                }
                r = r * (1.0F - nightVision) + r * k * nightVision;
                g = g * (1.0F - nightVision) + g * k * nightVision;
                b = b * (1.0F - nightVision) + b * k * nightVision;
            }
            r = Math.min(r, 1.0F);
            g = Math.min(g, 1.0F);
            b = Math.min(b, 1.0F);
            float ir = 1.0F - r, ig = 1.0F - g, ib = 1.0F - b;
            ir = 1.0F - ir * ir * ir * ir;
            ig = 1.0F - ig * ig * ig * ig;
            ib = 1.0F - ib * ib * ib * ib;
            r = r * (1.0F - gamma) + ir * gamma;
            g = g * (1.0F - gamma) + ig * gamma;
            b = b * (1.0F - gamma) + ib * gamma;
            r = clamp(r * 0.96F + floor);
            g = clamp(g * 0.96F + floor);
            b = clamp(b * 0.96F + floor);
            out[i] = 255 << 24 | (int) (r * 255.0F) << 16 | (int) (g * 255.0F) << 8 | (int) (b * 255.0F);
        }
    }

    private static float clamp(float v)
    {
        return v < 0F ? 0F : v > 1F ? 1F : v;
    }
}
