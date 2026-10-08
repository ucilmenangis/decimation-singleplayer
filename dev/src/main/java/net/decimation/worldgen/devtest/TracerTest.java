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
 * bandit held facing away from the player shoots the player; every tracer
 * is compared with the line to the player's chest. PASS: mean under 5 deg.
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
    private double tracerMax, tracerSum;

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
            tracerBandit = net.decimation.fixes.Deci.newBandit(sp.worldObj);
            tracerBandit.setPosition(8.5, 80, 32.5);
            sp.worldObj.spawnEntityInWorld(tracerBandit);
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
        }
    }

    private void tracerClient(Minecraft mc)
    {
        if (tracerTicks >= 420)
        {
            FMLLog.info("[%s] AUTOTEST tracer: %d NPC tracers, angle to the target mean %.1f, max %.1f degrees "
                        + "(bandit facing away; before the fix about 180)", DecimationWorldGen.MODID, tracerCount,
                        tracerCount > 0 ? tracerSum / tracerCount : 0, tracerMax);
            DevTestResults.check("tracer", "mean angle to target", String.format("%.1f deg (%d)", tracerCount > 0
                                 ? tracerSum / tracerCount : 0, tracerCount), tracerCount > 10
                                 && tracerSum / tracerCount < 5, "< 5 deg over > 10 tracers (original ~133)");
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
                double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
                double px = mc.thePlayer.posX - a.x, pz = mc.thePlayer.posZ - a.z;
                double py = mc.thePlayer.boundingBox.minY + mc.thePlayer.height * 0.6 - a.y;
                double cos = (dx * px + dy * py + dz * pz)
                    / Math.sqrt((dx * dx + dy * dy + dz * dz) * (px * px + py * py + pz * pz));
                double ang = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
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
