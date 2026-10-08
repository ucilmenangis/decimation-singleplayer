package net.decimation.worldgen.devtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.worldgen.DecimationWorldGen;
import net.decimation.worldgen.DevAutoTest;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.building.Building;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.StructureGenerator;
import net.decimation.worldgen.ZoneStore;
import net.minecraft.world.WorldSettings;

/**
 * Scope (-Ptest=scope, fixes/ScopeZoom): fps with an empty hand, a 4x
 * gun held, aiming, empty again; then an aiming screenshot per sight
 * (reddot, 2x, 4x, 8x, integrated) in two window sizes.
 */
public class ScopeTest extends DevTestMode
{
    public String name() { return "scope"; }

    public boolean client(Minecraft mc)
    {
        if (!scopeDone)
        {
            scopeTest(mc);
        }
        return !scopeDone;
    }

    // ---- scope test (-Pscope): fps with an empty hand, a gun with a 4x
    // scope held, and aiming through it, plus the FOV zoom and a screenshot
    // (scope_<pip|zoom>.png). Run once per config/deciworldgen_scope.cfg
    // setting to compare Decimation's picture in picture scope with the zoom.
    private static final boolean SCOPE_ONLY = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".scopeonly"));
    /** -Pcityfps: fps looking down a city street (last autotest world): cityfps.png + log. */
    private static final boolean CITYFPS = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".cityfps"));
    /** -Pprops: fps with many props in view / hidden behind walls, in the last autotest world. */
    private static final boolean PROPS = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".props"));
    /** -Ptracer: NPC tracer direction test alone in the last autotest world (tools/patches/PatchTracer.java). */
    private static final boolean TRACER = "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".tracer"));
    private static final boolean SCOPE = SCOPE_ONLY || "true".equals(System.getProperty(DevAutoTest.PROPERTY + ".scope"));
    private static final int SCOPE_PHASE = 400;
    private int scopeTicks;
    private final float[] scopeFps = new float[4];
    private final int[] scopeSamples = new int[4];
    private boolean scopeDone;
    private float scopeFovMul;

    /** Client side: true while the scope test runs. */
    private boolean scopeTest(Minecraft mc)
    {
        int phase = scopeTicks / SCOPE_PHASE, t = scopeTicks % SCOPE_PHASE;
        scopeTicks++;
        net.minecraft.entity.player.EntityPlayerMP sp = MinecraftServer.getServer() == null ? null
            : (net.minecraft.entity.player.EntityPlayerMP) MinecraftServer.getServer().getConfigurationManager()
                .playerEntityList.get(0);
        if (mc.thePlayer == null || sp == null)
        {
            return false;
        }
        if (scopeTicks == 1)
        {
            mc.gameSettings.limitFramerate = 260; // unlimited
            mc.gameSettings.enableVsync = false;
            org.lwjgl.opengl.Display.setVSyncEnabled(false);
            mc.gameSettings.hideGUI = false; // hideGUI also hides the hand (gun and scope)
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(8.5, 72, 40.5);
            sp.worldObj.setWorldTime(6000); // noon: a reused world (-Pscopeonly) may be at night
        }
        mc.thePlayer.capabilities.isFlying = true;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 180;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 2;
        // phases: empty hand, 4x gun held, aiming, empty hand again (drift check)
        if (phase < 4 && t == 0)
        {
            net.minecraft.item.ItemStack gun = phase == 0 || phase == 3 ? null : scopedGun();
            for (net.minecraft.entity.player.EntityPlayer p : new net.minecraft.entity.player.EntityPlayer[] {mc.thePlayer, sp})
            {
                p.inventory.currentItem = 0;
                p.inventory.setInventorySlotContents(0, gun == null ? null : gun.copy());
                net.decimation.fixes.Deci.setAimMode(p, phase == 2 ? 1 : 0);
            }
        }
        if (phase >= 4 && scopeShots(mc, sp, scopeTicks - 4 * SCOPE_PHASE - 1))
        {
            return true;
        }
        if (phase < 4)
        {
            // keep it: the server may sync its own value back
            net.decimation.fixes.Deci.setAimMode(mc.thePlayer, phase == 2 ? 1 : 0);
            if (t >= 120 && t % 20 == 0)
            {
                String dbg = mc.debug; // "N fps, M chunk updates"
                scopeFps[phase] += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
                scopeSamples[phase]++;
            }
            if (phase == 2 && t == 300)
            {
                scopeFovMul = ((Double) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                    net.minecraft.client.renderer.EntityRenderer.class, mc.entityRenderer, "cameraZoom",
                    "field_78503_V")).floatValue();
                String mode = Boolean.getBoolean(net.decimation.fixes.ScopeZoom.PROPERTY) ? "pip" : "zoom";
                DevTestUtil.screenshot(mc, "scope", "scope_" + mode + ".png");
            }
            return true;
        }
        scopeDone = true;
        FMLLog.info("[%s] AUTOTEST scope %s: fps empty hand %.0f, gun with 4x held %.0f, aiming %.0f, empty hand "
                    + "again %.0f; camera zoom aiming %.2f", DecimationWorldGen.MODID,
                    Boolean.getBoolean(net.decimation.fixes.ScopeZoom.PROPERTY) ? "picture in picture" : "view zoom",
                    scopeFps[0] / Math.max(1, scopeSamples[0]), scopeFps[1] / Math.max(1, scopeSamples[1]),
                    scopeFps[2] / Math.max(1, scopeSamples[2]), scopeFps[3] / Math.max(1, scopeSamples[3]),
                    scopeFovMul);
        String m = Boolean.getBoolean(net.decimation.fixes.ScopeZoom.PROPERTY) ? "pip" : "zoom";
        DevTestResults.value("scope", m + " fps empty hand", Math.round(scopeFps[0] / Math.max(1, scopeSamples[0])));
        DevTestResults.value("scope", m + " fps 4x held", Math.round(scopeFps[1] / Math.max(1, scopeSamples[1])));
        DevTestResults.value("scope", m + " fps aiming", Math.round(scopeFps[2] / Math.max(1, scopeSamples[2])));
        DevTestResults.value("scope", m + " fps empty again", Math.round(scopeFps[3] / Math.max(1, scopeSamples[3])));
        if (m.equals("zoom"))
        {
            DevTestResults.check("scope", "camera zoom aiming 4x", String.format("%.2f", scopeFovMul),
                                 Math.abs(scopeFovMul - 4f) < 0.05f, "4.00");
        }
        for (net.minecraft.entity.player.EntityPlayer p : new net.minecraft.entity.player.EntityPlayer[] {mc.thePlayer, sp})
        {
            p.inventory.setInventorySlotContents(0, null);
            net.decimation.fixes.Deci.setAimMode(p, 0);
        }
        mc.gameSettings.hideGUI = false;
        return false;
    }

    /** Sights shot after the fps phases, each aimed for SHOT_TICKS, in two window sizes ("" = integrated, aug1). */
    private static final String[] SCOPE_SIGHTS = {"reddot", "2x", "4x", "8x", ""};
    private static final int SHOT_TICKS = 120;
    private static final int[][] SCOPE_WINDOWS = {{0, 0}, {900, 900}}; // 0 = the window as it is
    private float[] wander;

    /** Aiming screenshots per sight and window size: scope_<w>x<h>_<sight>.png. */
    private boolean scopeShots(Minecraft mc, net.minecraft.entity.player.EntityPlayerMP sp, int t)
    {
        int shot = t / SHOT_TICKS, tt = t % SHOT_TICKS;
        int window = shot / SCOPE_SIGHTS.length, sight = shot % SCOPE_SIGHTS.length;
        if (window >= SCOPE_WINDOWS.length)
        {
            return false;
        }
        if (tt == 0)
        {
            if (sight == 0 && SCOPE_WINDOWS[window][0] > 0)
            {
                try
                {
                    org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(SCOPE_WINDOWS[window][0],
                                                                                          SCOPE_WINDOWS[window][1]));
                    java.lang.reflect.Method resize = cpw.mods.fml.relauncher.ReflectionHelper.findMethod(
                        Minecraft.class, mc, new String[] {"resize", "func_71370_a"}, int.class, int.class);
                    resize.invoke(mc, SCOPE_WINDOWS[window][0], SCOPE_WINDOWS[window][1]);
                }
                catch (Exception e)
                {
                    FMLLog.info("[%s] AUTOTEST scope: resize failed: %s", DecimationWorldGen.MODID, e);
                }
            }
            net.minecraft.item.ItemStack gun = scopedGun(SCOPE_SIGHTS[sight]);
            for (net.minecraft.entity.player.EntityPlayer p : new net.minecraft.entity.player.EntityPlayer[] {mc.thePlayer, sp})
            {
                p.inventory.setInventorySlotContents(0, gun.copy());
            }
        }
        // from the hip first (as in play: the zoom starts at 1x), then aim
        net.decimation.fixes.Deci.setAimMode(mc.thePlayer, tt < 20 ? 0 : 1);
        net.decimation.fixes.Deci.setAimMode(sp, tt < 20 ? 0 : 1);
        // second half: turn the view like a mouse sweep (+-4 degrees) to see the gun lag under zoom
        if (tt > SHOT_TICKS / 2 && tt < SHOT_TICKS - 8)
        {
            float yaw = 180 + 4f * (float) Math.sin((tt - SHOT_TICKS / 2) / 6.0);
            mc.thePlayer.rotationYaw = yaw;
        }
        // sight wander over the second half of the aim (zoom settled): glass position spread
        float[] g = net.decimation.fixes.Deci.scopeGlassOnScreen();
        if (tt == SHOT_TICKS / 2)
        {
            wander = new float[] {1e9f, 1e9f, -1e9f, -1e9f};
        }
        if (tt > SHOT_TICKS / 2 && g != null && wander != null)
        {
            wander[0] = Math.min(wander[0], g[0]);
            wander[1] = Math.min(wander[1], g[1]);
            wander[2] = Math.max(wander[2], g[0]);
            wander[3] = Math.max(wander[3], g[1]);
        }
        if (tt == SHOT_TICKS - 1)
        {
            if (wander != null && wander[2] >= wander[0])
            {
                FMLLog.info("[%s] AUTOTEST scope sight %s: centre %.0f,%.0f of %dx%d, wander %.0f x %.0f px",
                            DecimationWorldGen.MODID, SCOPE_SIGHTS[sight].isEmpty() ? "integrated" : SCOPE_SIGHTS[sight],
                            (wander[0] + wander[2]) / 2, (wander[1] + wander[3]) / 2, mc.displayWidth,
                            mc.displayHeight, wander[2] - wander[0], wander[3] - wander[1]);
            }
            String name = "scope_" + mc.displayWidth + "x" + mc.displayHeight + "_"
                + (SCOPE_SIGHTS[sight].isEmpty() ? "integrated" : SCOPE_SIGHTS[sight]) + ".png";
            DevTestUtil.screenshot(mc, "scope", name);
            FMLLog.info("[%s] AUTOTEST scope shot %s", DecimationWorldGen.MODID, name);
        }
        return true;
    }

    /** A rifle without an integrated scope carrying the 4x sight attachment. */
    private static net.minecraft.item.ItemStack scopedGun()
    {
        return scopedGun("4x");
    }

    /** A rifle carrying this sight attachment, or for "" a gun with an integrated scope. */
    private static net.minecraft.item.ItemStack scopedGun(String sightName)
    {
        net.minecraft.item.Item gun = null, sight = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            net.minecraft.item.Item item = (net.minecraft.item.Item) o;
            if (gun == null && net.decimation.fixes.Deci.isGun(item)
                && net.decimation.fixes.Deci.hasIntegratedScope(item) == sightName.isEmpty())
            {
                gun = item;
            }
            if (net.decimation.fixes.Deci.isAttachment(item) && sightName.equals(net.decimation.fixes.Deci.attachmentName(item)))
            {
                sight = item;
            }
        }
        net.minecraft.item.ItemStack st = new net.minecraft.item.ItemStack(gun);
        st.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
        String name = sight == null ? "none" : net.minecraft.item.Item.itemRegistry.getNameForObject(sight);
        if (sight != null)
        {
            st.stackTagCompound.setString("sightAttach", name.substring(name.indexOf(':') + 1));
        }
        FMLLog.info("[%s] AUTOTEST scope: gun %s, sight %s", DecimationWorldGen.MODID,
                    net.minecraft.item.Item.itemRegistry.getNameForObject(gun), name);
        return st;
    }
}
