package net.decimation.worldgen.devtest;

import java.lang.reflect.Method;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Entity culling check (-Ptest=culltest, Fabric only: DeciRemnant Optimized's EntityCulling, read by reflection since
 * DeciRemnant does not compile against it). In the arena, a stone wall 6 blocks west of the player (7 wide, 6 high)
 * with an infected and a chest behind it, and another infected and chest off to the side in plain view: after 3 s the
 * hidden two must be culled and the seen two not; then the wall goes and after 2 s nothing may be culled.
 */
public class CullTest extends DevTestMode
{
    private static final int WALL_X = DevTestArena.X - 6;
    /** x, z of the hidden and the seen infected and chests. */
    private static final double[][] MOBS = {{DevTestArena.X - 10.5, DevTestArena.Z - 1.5}, {DevTestArena.X - 10.5, DevTestArena.Z + 8.5}};
    private static final int[][] CHESTS = {{DevTestArena.X - 10, DevTestArena.Z - 4}, {DevTestArena.X - 10, DevTestArena.Z + 10}};

    private final EntityLiving[] held = new EntityLiving[2];
    private volatile int[] ids = {-1, -1};
    private volatile boolean wallGone;
    private int ticks, serverTicks;
    private boolean done;

    public String name() { return "culltest"; }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ids[1] < 0)
        {
            return true;
        }
        ticks++;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 90;   // facing west, at the wall
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
        if (ticks == 60)
        {
            check(mc, "wall", new boolean[] {true, false});
            DevTestUtil.screenshot(mc, name(), "culltest_wall");
            wallGone = true;
        }
        if (ticks == 100)
        {
            check(mc, "no wall", new boolean[] {false, false});
            DevTestUtil.screenshot(mc, name(), "culltest_open");
            done = true;
        }
        return !done;
    }

    /** The culled flags of the two infected and the two chests against what is expected (hidden first). */
    private void check(Minecraft mc, String when, boolean[] want)
    {
        String[] which = {"behind the wall", "in view"};
        for (int i = 0; i < 2; i++)
        {
            Entity e = mc.theWorld.getEntityByID(ids[i]);
            TileEntity te = mc.theWorld.getTileEntity(CHESTS[i][0], DevTestArena.Y, CHESTS[i][1]);
            Boolean ec = culled(e), tc = culled(te);
            DevTestResults.check(name(), when + ": infected " + which[i] + " culled", String.valueOf(ec),
                                 ec != null && ec == want[i], String.valueOf(want[i]));
            DevTestResults.check(name(), when + ": chest " + which[i] + " culled", String.valueOf(tc),
                                 tc != null && tc == want[i], String.valueOf(want[i]));
        }
    }

    /** EntityCulling's flag (Cullable.deciCulled), or null when the mod is not there or the object is missing. */
    private static Boolean culled(Object o)
    {
        if (o == null)
        {
            return null;
        }
        try
        {
            Method m = o.getClass().getMethod("deciCulled");
            return (Boolean) m.invoke(o);
        }
        catch (ReflectiveOperationException ex)
        {
            return null;
        }
    }

    @Override
    public void server()
    {
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World w = p.worldObj;
        int y = DevTestArena.Y;
        serverTicks++;
        if (serverTicks == 10)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(6000);
            for (int z = DevTestArena.Z - 5; z <= DevTestArena.Z + 1; z++)
            {
                for (int dy = 0; dy < 6; dy++)
                {
                    w.setBlock(WALL_X, y + dy, z, Blocks.stone);
                }
            }
            for (int[] c : CHESTS)
            {
                w.setBlock(c[0], y, c[1], Blocks.chest);
            }
        }
        if (serverTicks > 20)
        {
            for (int i = 0; i < 2; i++)
            {
                if (held[i] == null)
                {
                    EntityLiving e = Deci.newInfected(w);
                    e.setPosition(MOBS[i][0], y, MOBS[i][1]);
                    if (w.spawnEntityInWorld(e))   // Decimation refuses some infected spawns: retried
                    {
                        held[i] = e;
                    }
                }
                else
                {
                    held[i].setPositionAndUpdate(MOBS[i][0], y, MOBS[i][1]);
                    held[i].motionX = held[i].motionZ = 0;
                    held[i].setAttackTarget(null);
                }
            }
            if (held[0] != null && held[1] != null && ids[1] < 0)
            {
                ids = new int[] {held[0].getEntityId(), held[1].getEntityId()};
            }
        }
        if (wallGone && w.getBlock(WALL_X, y, DevTestArena.Z) != Blocks.air)
        {
            for (int z = DevTestArena.Z - 5; z <= DevTestArena.Z + 1; z++)
            {
                for (int dy = 0; dy < 6; dy++)
                {
                    w.setBlock(WALL_X, y + dy, z, Blocks.air);
                }
            }
        }
    }
}
