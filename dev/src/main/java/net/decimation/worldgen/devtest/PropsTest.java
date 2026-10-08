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
 * Prop fps (-Ptest=props): a stone platform high above the city with a
 * 15 x 15 grid of mixed props: empty, props in view, props boxed in by
 * stone (hidden), the box alone; props_<phase>.png.
 */
public class PropsTest extends DevTestMode
{
    public String name() { return "props"; }

    public boolean client(Minecraft mc)
    {
        if (!propsDone)
        {
            propsClient(mc);
        }
        return !propsDone;
    }

    public void server()
    {
        if (!propsDone)
        {
            propsServer();
        }
    }

    // ---- prop fps test (-Pprops): on a stone platform high above the
    // city, looking at a 15 x 15 grid of mixed props: empty platform, props
    // in view, props boxed in by stone (all hidden), the box without props.
    private static final String[] PROP_BLOCKS = {"deci:BlockStreetBench", "deci:BlockStreetBin", "deci:BlockTrashBag1",
        "deci:BlockBarrier", "deci:BlockCone", "deci:BlockHazardbarrier", "deci:BlockWreckage1", "deci:BlockWoodCrate",
        "deci:BlockStreetLight"};
    private static final int PROP_PHASE = 400;
    private static final String[] PROP_PHASES = {"empty platform", "225 props in view", "225 props hidden in a stone box",
        "stone box, no props"};
    private volatile boolean propsDone;
    private volatile int propPhase = -1;
    private int propApplied = -1;
    private int propTicks;
    private final float[] propFps = new float[4];
    private final int[] propSamples = new int[4];

    private void propsServer()
    {
        int phase = propPhase;
        if (phase == propApplied || phase < 0)
        {
            return;
        }
        propApplied = phase;
        net.minecraft.entity.player.EntityPlayerMP sp = (net.minecraft.entity.player.EntityPlayerMP)
            MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
        net.minecraft.world.World w = sp.worldObj;
        int y = 150;
        if (phase == 0)
        {
            w.setWorldTime(6000);
            for (int x = -12; x <= 28; x++)
            {
                for (int z = 0; z <= 34; z++)
                {
                    w.setBlock(x, y, z, net.minecraft.init.Blocks.stone, 0, 2);
                }
            }
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(8.5, y + 4, 2.5);
        }
        if (phase == 1 || phase == 3)
        {
            for (int i = 0; i < 15; i++)
            {
                for (int k = 0; k < 15; k++)
                {
                    net.minecraft.block.Block b = phase == 1
                        ? net.minecraft.block.Block.getBlockFromName(PROP_BLOCKS[(i * 7 + k * 3) % PROP_BLOCKS.length])
                        : net.minecraft.init.Blocks.air;
                    w.setBlock(1 + i, y + 1, 14 + k, b != null ? b : net.minecraft.init.Blocks.air, (i + k) % 4 + 2, 3);
                }
            }
        }
        if (phase == 2)
        {
            for (int x = -1; x <= 17; x++)
            {
                for (int z = 12; z <= 30; z++)
                {
                    for (int yy = y + 1; yy <= y + 6; yy++)
                    {
                        boolean shell = x == -1 || x == 17 || z == 12 || z == 30 || yy == y + 6;
                        if (shell)
                        {
                            w.setBlock(x, yy, z, net.minecraft.init.Blocks.stone, 0, 2);
                        }
                    }
                }
            }
        }
        if (phase == 4)
        {
            for (int x = -12; x <= 28; x++)
            {
                for (int z = 0; z <= 34; z++)
                {
                    for (int yy = y; yy <= y + 6; yy++)
                    {
                        w.setBlockToAir(x, yy, z);
                    }
                }
            }
        }
    }

    private void propsClient(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return;
        }
        int phase = propTicks / PROP_PHASE, t = propTicks % PROP_PHASE;
        propTicks++;
        propPhase = Math.min(phase, 4);
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        mc.thePlayer.capabilities.isFlying = true;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;     // south, toward the grid
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 30;
        if (phase < 4)
        {
            if (t >= 120 && t % 20 == 0)
            {
                String dbg = mc.debug;
                propFps[phase] += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
                propSamples[phase]++;
            }
            if (t == PROP_PHASE - 1)
            {
                DevTestUtil.screenshot(mc, "props", "props_" + phase + ".png");
            }
            return;
        }
        if (t > 40)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++)
            {
                sb.append(i == 0 ? "" : ", ").append(PROP_PHASES[i]).append(' ')
                  .append(Math.round(propFps[i] / Math.max(1, propSamples[i])));
            }
            FMLLog.info("[%s] AUTOTEST props fps: %s", DecimationWorldGen.MODID, sb);
            for (int i = 0; i < 4; i++)
            {
                DevTestResults.value("props", "fps " + PROP_PHASES[i], Math.round(propFps[i] / Math.max(1, propSamples[i])));
            }
            propsDone = true;
        }
    }
}
