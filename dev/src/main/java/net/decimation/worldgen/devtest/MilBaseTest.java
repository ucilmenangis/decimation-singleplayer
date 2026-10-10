package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.decimation.worldgen.Slices;
import net.decimation.worldgen.military.MilitaryBase;
import net.decimation.worldgen.military.MilitaryBasePlan;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/**
 * Military bases (docs/military_base.md) on flat ground in the sky, every size side by side,
 * then one screenshot per camera point the generator recorded (aerial, gate, towers, insides).
 * Shots milbase_<size>_<point>.png. -Psizes=0,1,2 picks sizes, -Pturns=0..3 the rotation,
 * -Pseed=N the base seed, -Ppoints=a,b only those points.
 */
public class MilBaseTest extends DevTestMode
{
    public String name() { return "milbase"; }

    // far from where the user plays and above the tallest city buildings (about y 206): the old
    // sites (3000, 3000 at y 110; -4000, -4000 at y 200) cut into cities and were cleared from the
    // dev world on 11 Oktober 2026 (tools/perfcheck.py clearblocks)
    private static final int Y = 230, X0 = -20000, Z0 = -20000, STEP = 170;
    private static final int BUILD = 10, SETTLE = 40, SHOT = 70;

    private final String[] sizes = System.getProperty("deciworldgen.autotest.sizes", "0,1,2").split(",");
    private final int turns = Integer.parseInt(System.getProperty("deciworldgen.autotest.turns", "0"));
    private final long seed = Long.parseLong(System.getProperty("deciworldgen.autotest.seed", "42"));
    private final String only = System.getProperty("deciworldgen.autotest.points", "");

    /** {size, x, y, z, yaw, pitch} per shot, and its name. */
    private final List<float[]> shots = new ArrayList<float[]>();
    private final List<String> names = new ArrayList<String>();
    private volatile int ticks;
    private volatile boolean built;
    private int taken = -1;

    public boolean client(Minecraft mc)
    {
        int t = ticks;
        if (mc.thePlayer == null || !built)
        {
            return true;
        }
        int i = (t - BUILD - 20) / SHOT, u = (t - BUILD - 20) % SHOT;
        if (i >= shots.size())
        {
            DevTestResults.value(name(), "shots", shots.size());
            return false;
        }
        if (i < 0)
        {
            return true;
        }
        float[] s = shots.get(i);
        mc.gameSettings.hideGUI = true;
        mc.gameSettings.fovSetting = 80f;
        mc.gameSettings.clouds = false;
        mc.gameSettings.renderDistanceChunks = 16;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = s[4];
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = s[5];
        if (u >= SETTLE && taken != i)
        {
            taken = i;
            DevTestUtil.screenshot(mc, name(), "milbase_" + names.get(i));
        }
        return true;
    }

    /**
     * Every block of the built base to run/client/devtest/milbase_<size>.tsv ("x y z name meta",
     * relative to the plan corner and Y) for tools/props/propclash.py (props against walls, props,
     * doors; facing).
     */
    private static void dump(World w, MilitaryBasePlan plan, int size)
    {
        java.io.File f = new java.io.File(Minecraft.getMinecraft().mcDataDir, "devtest/milbase_" + size + ".tsv");
        try (java.io.PrintWriter out = new java.io.PrintWriter(f, "UTF-8"))
        {
            out.println("# origin " + plan.minX() + " " + Y + " " + plan.minZ() + " size " + size);
            for (Map.Entry<String, float[]> e : plan.pointsOfInterest().entrySet())
            {
                float[] q = e.getValue();
                out.println("# poi " + e.getKey() + " " + q[0] + " " + q[1] + " " + q[2]);
            }
            for (int y = Y - 1; y <= Y + plan.height(); y++)
            {
                for (int x = plan.minX(); x <= plan.maxX(); x++)
                {
                    for (int z = plan.minZ(); z <= plan.maxZ(); z++)
                    {
                        net.minecraft.block.Block b = w.getBlock(x, y, z);
                        if (b != net.minecraft.init.Blocks.air)
                        {
                            out.println((x - plan.minX()) + " " + (y - Y) + " " + (z - plan.minZ()) + " "
                                        + net.minecraft.block.Block.blockRegistry.getNameForObject(b) + " "
                                        + w.getBlockMetadata(x, y, z));
                        }
                    }
                }
            }
        }
        catch (java.io.IOException e)
        {
            DevTestResults.value("milbase", "dump failed", e.toString());
        }
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World w = p.worldObj;
        if (t == 2)
        {
            p.capabilities.isFlying = true;
            p.capabilities.disableDamage = true;
            w.setWorldTime(6000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            p.playerNetServerHandler.setPlayerLocation(X0, Y + 40, Z0, 0, 60);
        }
        if (t == BUILD)
        {
            for (int k = 0; k < sizes.length; k++)
            {
                int size = Integer.parseInt(sizes[k].trim());
                int bx = X0 + k * STEP, bz = Z0;
                MilitaryBasePlan plan = new MilitaryBasePlan("T" + size + "_" + seed + "_" + turns, size, seed + size,
                                                             bx, bz, turns);
                long t0 = System.nanoTime();
                Slices.placeAt(w, plan, Y, false);   // no ramp: the ground far below would grow pillars
                DevTestResults.value(name(), MilitaryBase.SIZE_NAME[size] + " built ms", (System.nanoTime() - t0) / 1000000);
                dump(w, plan, size);
                for (String line : plan.debugLog())
                {
                    if (!line.contains(" ok "))
                    {
                        DevTestResults.value(name(), size + " refused", line);
                    }
                }
                for (Map.Entry<String, float[]> e : plan.pointsOfInterest().entrySet())
                {
                    if (!only.isEmpty() && !("," + only + ",").contains("," + e.getKey() + ","))
                    {
                        continue;
                    }
                    float[] q = e.getValue();
                    shots.add(new float[] {size, q[0], Y + q[1], q[2], q[3], q[4]});
                    names.add(size + "_" + e.getKey());
                }
            }
            built = true;
        }
        if (built)
        {
            int i = (t - BUILD - 20) / SHOT;
            if (i >= 0 && i < shots.size())
            {
                float[] s = shots.get(i);
                p.playerNetServerHandler.setPlayerLocation(s[1], s[2] - 1.62, s[3], s[4], s[5]);
                p.motionX = p.motionY = p.motionZ = 0;
            }
        }
    }
}
