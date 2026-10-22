package net.decimation.fixes;

import java.lang.reflect.Field;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;

/**
 * DayZ dark nights (user, 22 Oktober 2026; NightLight has the rules). Vanilla rebuilds its light map once a client tick
 * (EntityRenderer.updateLightmap, on the frame after updateTorchFlicker set lightmapUpdateNeeded), at the start of the
 * frame; EntityViewRenderEvent.FogColors fires later in that frame, before the sky and the world are drawn
 * (EntityRenderer.renderWorld: updateLightmap, then updateFogColor, then renderSky and the terrain). There, once a tick,
 * the colours are rebuilt by NightLight and uploaded into the same texture. Overworld only, nothing by full daylight
 * (vanilla's own map stands), client only: every client works out the same night from the synced world time.
 * Dev switch: -Ddeciworldgen.darknights=false.
 */
public final class DarkNights
{
    private static final Field COLORS = ReflectionHelper.findField(EntityRenderer.class, "lightmapColors", "field_78504_Q");
    private static final Field TEXTURE = ReflectionHelper.findField(EntityRenderer.class, "lightmapTexture", "field_78513_d");
    private static final Field FLICKER = ReflectionHelper.findField(EntityRenderer.class, "torchFlickerX", "field_78514_e");
    private static final Field BOSS = ReflectionHelper.findField(EntityRenderer.class, "bossColorModifier", "field_82831_U");
    private static final Field BOSS_PREV = ReflectionHelper.findField(EntityRenderer.class, "bossColorModifierPrev", "field_82832_V");

    static volatile boolean enabled = !"false".equals(System.getProperty("deciworldgen.darknights"));
    private final Minecraft mc = Minecraft.getMinecraft();
    private final int[] map = new int[256];
    private long lastTick = Long.MIN_VALUE;
    private WorldClient lastWorld;
    /** For the dev test: rebuilds done and their time in nanoseconds. */
    static long rebuilds, nanos;

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors e)
    {
        WorldClient w = mc.theWorld;
        if (!enabled || w == null || mc.thePlayer == null || w.provider.dimensionId != 0)
        {
            return;
        }
        long tick = w.getTotalWorldTime();
        if (tick == lastTick && w == lastWorld)
        {
            return;                                       // this tick's map is ours already
        }
        lastTick = tick;
        lastWorld = w;
        float day = NightLight.daylight(w.getCelestialAngle(1.0F));
        if (day >= 1F)
        {
            return;
        }
        long start = System.nanoTime();
        try
        {
            EntityRenderer er = mc.entityRenderer;
            float boss = BOSS.getFloat(er), bossPrev = BOSS_PREV.getFloat(er);
            float bossNow = boss > 0F ? bossPrev + (boss - bossPrev) * (float) e.renderPartialTicks : 0F;
            NightLight.fill(map, w.provider.lightBrightnessTable, w.getSunBrightness(1.0F), day, w.getCurrentMoonPhaseFactor(),
                w.getRainStrength(1.0F), FLICKER.getFloat(er), bossNow, w.lastLightningBolt > 0, nightVision((float) e.renderPartialTicks),
                mc.gameSettings.gammaSetting);
            System.arraycopy(map, 0, (int[]) COLORS.get(er), 0, 256);
            ((DynamicTexture) TEXTURE.get(er)).updateDynamicTexture();
        }
        catch (IllegalAccessException ex)
        {
            enabled = false;                              // never again: vanilla's map stands
        }
        rebuilds++;
        nanos += System.nanoTime() - start;
    }

    /** EntityRenderer.getNightVisionBrightness: full above 10 s left, then flickering out. */
    private float nightVision(float partial)
    {
        PotionEffect nv = mc.thePlayer.getActivePotionEffect(Potion.nightVision);
        if (nv == null)
        {
            return 0F;
        }
        int left = nv.getDuration();
        return left > 200 ? 1.0F : 0.7F + MathHelper.sin((left - partial) * (float) Math.PI * 0.2F) * 0.3F;
    }

    /** For the dev test: the light map colour now (sky light 0..15, block light 0..15). */
    public static int colour(int sky, int block)
    {
        try
        {
            return ((int[]) COLORS.get(Minecraft.getMinecraft().entityRenderer))[sky * 16 + block];
        }
        catch (IllegalAccessException ex)
        {
            return 0;
        }
    }

    /** For the dev test: average rebuild time in microseconds, and the count. */
    public static double averageMicros()
    {
        return rebuilds == 0 ? 0 : nanos / 1000.0 / rebuilds;
    }

    public static long rebuildCount()
    {
        return rebuilds;
    }

    /** For the dev test: switch the dark nights off and on (vanilla's map comes back on its next rebuild). */
    public static void setEnabled(boolean on)
    {
        enabled = on;
    }
}
