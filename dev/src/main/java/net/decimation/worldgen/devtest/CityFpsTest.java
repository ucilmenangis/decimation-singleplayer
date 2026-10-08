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
 * City fps (-Ptest=cityfps): standing in a seed 1 city street at x 8.5,
 * z 120.5 looking north, fps averaged over 15 s after 5 s, cityfps.png.
 */
public class CityFpsTest extends DevTestMode
{
    public String name() { return "cityfps"; }

    public boolean client(Minecraft mc)
    {
        if (!cityDone)
        {
            cityClient(mc);
        }
        return !cityDone;
    }

    // ---- city fps (-Pcityfps): standing in a street at x 8, looking north
    // along it from z 120 (seed 1 city), fps averaged over 15 s after 5 s.
    private volatile boolean cityDone;
    private int cityTicks;
    private float cityFps;
    private int citySamples;

    private void cityClient(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return;
        }
        cityTicks++;
        if (cityTicks == 1)
        {
            net.minecraft.entity.player.EntityPlayerMP sp = (net.minecraft.entity.player.EntityPlayerMP)
                MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            sp.worldObj.setWorldTime(6000);
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(8.5, 68, 120.5);
            mc.gameSettings.limitFramerate = 260;
            mc.gameSettings.enableVsync = false;
        }
        mc.thePlayer.capabilities.isFlying = true;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 180;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 3;
        if (cityTicks > 100 && cityTicks % 20 == 0 && cityTicks <= 400)
        {
            String dbg = mc.debug;
            cityFps += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
            citySamples++;
        }
        if (cityTicks == 400)
        {
            DevTestUtil.screenshot(mc, "cityfps", "cityfps.png");
            FMLLog.info("[%s] AUTOTEST city fps %.0f (prop distances small %s, medium %s, large %s)",
                        DecimationWorldGen.MODID, cityFps / Math.max(1, citySamples),
                        System.getProperty("decimation.props.small"), System.getProperty("decimation.props.medium"),
                        System.getProperty("decimation.props.large"));
            DevTestResults.value("cityfps", "fps", Math.round(cityFps / Math.max(1, citySamples)));
            cityDone = true;
        }
    }
}
