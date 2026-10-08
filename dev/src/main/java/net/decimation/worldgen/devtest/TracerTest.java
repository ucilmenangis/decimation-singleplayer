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
 * NPC tracer direction (-Ptest=tracer, tools/patches/PatchTracer.java): a
 * bandit held facing away from the player shoots the player. Since v0.30.3
 * shots have a spread (NpcShots) and the tracer must show the shot itself:
 * every tracer is compared with the directions the server shot along
 * (NpcShots.record), PASS: mean under 3 deg (the muzzle sits a little off
 * the eyes). The angle to the player's chest is logged too (spread).
 */
public class TracerTest extends DevTestMode
{
    public String name() { return "tracer"; }

    public boolean client(Minecraft mc)
    {
        if (!tracerDone)
        {
            tracerClient(mc);
        }
        return !tracerDone;
    }

    public void server()
    {
        if (!tracerDone)
        {
            tracerServer();
        }
    }

    // ---- NPC tracer test (-Ptracer): a bandit 8 blocks north of the player,
    // held facing AWAY (north), shoots the player; every tracer the client
    // draws is compared with the line from its start to the player's eyes.
    // Before PatchTracer the tracer followed the body facing (about 180 deg off).
    private volatile boolean tracerDone;
    private int tracerTicks;
    private net.minecraft.entity.EntityLiving tracerBandit;
    private final java.util.Set<Object> tracersSeen =
        java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
    private int tracerCount;
    private double tracerMax, tracerSum, shotSum;
    private int allSeen, rejected;
    private final java.util.Map<String, Integer> colours = new java.util.TreeMap<String, Integer>();
    private final java.util.Queue<double[]> shotDirs = new java.util.concurrent.ConcurrentLinkedQueue<double[]>();

    private void tracerServer()
    {
        net.minecraft.entity.player.EntityPlayerMP sp = (net.minecraft.entity.player.EntityPlayerMP)
            MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
        tracerTicks++;
        if (tracerTicks == 20)
        {
            sp.worldObj.setWorldTime(6000);
            // the camera views switch to peaceful, which removes a hostile bandit at once
            sp.worldObj.difficultySetting = net.minecraft.world.EnumDifficulty.NORMAL;
            sp.capabilities.isFlying = true;
            sp.setPositionAndUpdate(8.5, 80, 40.5);
            net.decimation.fixes.NpcShots.record = shotDirs;
            // a block to stand on: held in the air, the client's copy falls and its tracers start lower
            sp.worldObj.setBlock(8, 79, 32, net.minecraft.init.Blocks.stone);
            tracerBandit = net.decimation.fixes.Deci.newBandit(sp.worldObj);
            tracerBandit.setPosition(8.5, 80, 32.5);
            // a fixed gun and rate (a random tier could roll a slow sniper rifle)
            net.decimation.fixes.NpcLoadouts.instance().equip(tracerBandit,
                net.decimation.fixes.NpcLoadouts.instance().byName("bandit_medium"), tracerBandit.getEntityData());
            tracerBandit.getEntityData().setString(net.decimation.fixes.NpcLoadouts.GUN_TAG, "akm");
            sp.worldObj.spawnEntityInWorld(tracerBandit);
            net.decimation.fixes.Deci.setNpcShotDelay(tracerBandit, 3, 3);
        }
        if (tracerBandit != null && tracerTicks > 40 && tracerTicks < 400)
        {
            tracerBandit.setPosition(8.5, 80, 32.5);
            tracerBandit.motionX = tracerBandit.motionY = tracerBandit.motionZ = 0;
            tracerBandit.rotationYaw = tracerBandit.rotationYawHead = tracerBandit.renderYawOffset = 180; // away
            tracerBandit.rotationPitch = 0;
            tracerBandit.setRevengeTarget(sp); // a miss plays its sound at getAITarget() (null: crash)
            net.decimation.fixes.Deci.banditShootAt(tracerBandit, sp); // fires when its cooldown runs out
        }
        if (tracerTicks == 400 && tracerBandit != null)
        {
            tracerBandit.setDead();
            net.decimation.fixes.NpcShots.record = null;
            tracerBandit.worldObj.setBlockToAir(8, 79, 32);
        }
    }

    private int ownShots()
    {
        int n = 0;
        for (double[] d : shotDirs)
        {
            n += tracerBandit != null && (int) d[3] == tracerBandit.getEntityId() ? 1 : 0;
        }
        return n;
    }

    private void tracerClient(Minecraft mc)
    {
        if (tracerTicks >= 420)
        {
            FMLLog.info("[%s] AUTOTEST tracer: %d NPC tracers, angle to the target mean %.1f, max %.1f degrees "
                        + "(bandit facing away; before the fix about 180)", DecimationWorldGen.MODID, tracerCount,
                        tracerCount > 0 ? tracerSum / tracerCount : 0, tracerMax);
            DevTestResults.value("tracer", "tracer colours (test bandit)", colours + ", all tracers seen " + allSeen);
            DevTestResults.value("tracer", "mean angle to target (spread)", String.format("%.1f deg (%d)",
                                 tracerCount > 0 ? tracerSum / tracerCount : 0, tracerCount));
            DevTestResults.check("tracer", "tracer vs server shot line", String.format("%.1f deg (%d tracers, %d shots)",
                                 tracerCount > 0 ? shotSum / tracerCount : 0, tracerCount, ownShots()),
                                 tracerCount > 10 && shotSum / tracerCount < 3,
                                 "< 3 deg over > 10 tracers (before v0.28.5 ~133 to the target)");
            tracerDone = true;
            return;
        }
        if (mc.thePlayer == null)
        {
            return;
        }
        try
        {
            java.util.List<?> list = net.decimation.fixes.Deci.tracers();
            for (Object t : list)
            {
                if (!tracersSeen.add(t))
                {
                    continue;
                }
                org.lwjgl.util.vector.Vector3f[] line = net.decimation.fixes.Deci.tracerLine(t);
                org.lwjgl.util.vector.Vector3f a = line[0], b = line[1];
                allSeen++;
                // only the test bandit's tracers (8.5, 80, 32.5): other NPCs nearby fight each other
                if (Math.abs(a.x - 8.5) > 4 || Math.abs(a.z - 32.5) > 4 || Math.abs(a.y - 81.5) > 4)
                {
                    if (rejected++ < 6)
                    {
                        DevTestResults.value("tracer", "other tracer start", String.format("%.1f %.1f %.1f %s",
                            a.x, a.y, a.z, net.decimation.fixes.Deci.tracerColor(t)));
                    }
                    continue;
                }
                double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
                double px = mc.thePlayer.posX - a.x, pz = mc.thePlayer.posZ - a.z;
                double py = mc.thePlayer.boundingBox.minY + mc.thePlayer.height * 0.6 - a.y;
                double cos = (dx * px + dy * py + dz * pz)
                    / Math.sqrt((dx * dx + dy * dy + dz * dz) * (px * px + py * py + pz * pz));
                double ang = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
                String col = net.decimation.fixes.Deci.tracerColor(t);
                colours.put(col, (colours.containsKey(col) ? colours.get(col) : 0) + 1);
                // closest direction the server shot along
                double bestShot = 180, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                for (double[] d : shotDirs)
                {
                    if (tracerBandit == null || (int) d[3] != tracerBandit.getEntityId())
                    {
                        continue; // another NPC's shot
                    }
                    double c = (dx * d[0] + dy * d[1] + dz * d[2]) / len;
                    bestShot = Math.min(bestShot, Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, c)))));
                }
                shotSum += bestShot;
                tracerCount++;
                tracerSum += ang;
                tracerMax = Math.max(tracerMax, ang);
            }
        }
        catch (Exception e)
        {
            FMLLog.info("[%s] AUTOTEST tracer: cannot read tracers: %s", DecimationWorldGen.MODID, e);
            tracerDone = true;
        }
    }
}
