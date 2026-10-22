package net.decimation.worldgen.devtest;

import java.util.Arrays;

import net.decimation.fixes.Deci;
import net.decimation.fixes.Wounds;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * Blood splats that stay on hit infected (fixes/Wounds). Three plain infected stand in the arena facing the player 4 blocks
 * away: A is hit while the player looks at its head, chest, legs, left arm and right arm (in that order), B four times
 * while the player looks just over its head (random spots), C never. Hits are server damage from the player, so the
 * client sees them as the hurt flash, as in play. The client aims first, the server hits once the client says it aims,
 * the client waits for the splat before the next. Checks: the parts and sides of A's splats, B's count, pictures made.
 * Shots: A before (wounds_a_before) and after (wounds_a), B after (wounds_b), at a narrow field of view.
 */
public class WoundsTest extends DevTestMode
{
    public String name() { return "wounds"; }

    /**
     * What the player looks at for each hit. A's five: a point on A {to its left (blocks), above its feet (blocks)}: head,
     * chest, legs, left arm, right arm, aimed from where the player and A really are (a fixed angle missed the box when
     * the player stood a little off centre, and a miss is a random spot). The arms reach past the box (0.6 wide): their
     * points sit 0.28 aside, inside the box but past the body's width. B's four: a view {yaw, pitch} just over its head.
     */
    private static final float B_YAW = 53.13f;
    private static final float[][] VIEW = {{0, 1.70f}, {0, 1.15f}, {0, 0.45f}, {0.28f, 1.15f}, {-0.28f, 1.15f},
                                           {B_YAW, -25}, {B_YAW, -25}, {B_YAW, -25}, {B_YAW, -25}};
    private static final int A_HITS = 5;
    private volatile int ticks;
    private volatile int idA = -1, idB = -1;
    /** Hit asked by the client (index into VIEW), -1 none; the server answers in hitDone. */
    private volatile int hitAsk = -1, hitDone = -1;
    private volatile boolean applied;
    private final EntityLiving[] held = new EntityLiving[3];
    private long hits0, built0, nanos0, fpsSum;
    private int step, wait, seen;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ticks < 20 || idA < 0)
        {
            return true;
        }
        mc.gameSettings.hideGUI = true;
        mc.gameSettings.thirdPersonView = 0;
        wait++;
        if (step == 0)
        {
            hits0 = Wounds.hits;
            built0 = Wounds.built;
            look(mc, 90, 8, 30f);
            if (wait > 15)
            {
                DevTestUtil.screenshot(mc, name(), "wounds_a_before");
                next();
            }
        }
        else if (step <= VIEW.length)
        {
            int i = step - 1;
            if (i < A_HITS)
            {
                aimAt(mc, VIEW[i][0], VIEW[i][1]);
            }
            else
            {
                look(mc, VIEW[i][0], VIEW[i][1], 70f);
            }
            if (wait == 6)
            {
                seen = Wounds.spots(i < A_HITS ? idA : idB).length;
                hitAsk = i;                            // aimed for a few ticks: the server hits now
            }
            if (wait > 6 && hitDone == i && Wounds.spots(i < A_HITS ? idA : idB).length > seen || wait > 40)
            {
                DevTestResults.value(name(), "hit " + i, "server applied " + applied + ", splats "
                    + Wounds.spots(i < A_HITS ? idA : idB).length + " after " + wait + " ticks");
                next();
            }
        }
        else if (step == VIEW.length + 1)
        {
            look(mc, 90, 8, 30f);
            if (wait > 15)
            {
                DevTestUtil.screenshot(mc, name(), "wounds_a");
                int[][] a = Wounds.spots(idA);
                StringBuilder s = new StringBuilder();
                for (int[] p : a)
                {
                    s.append(Arrays.toString(p));
                }
                boolean ok = a.length == 5 && a[0][0] == 0 && a[0][1] == 0 && a[1][0] == 1 && a[1][1] == 0
                             && a[2][0] == 3 && a[2][1] == 0 && a[3][0] == 2 && a[4][0] == 2;
                DevTestResults.check(name(), "A's splats: head, body, legs front, then both arms", s.toString(), ok,
                                     "[0, 0][1, 0][3, 0][2, *][2, *]");
                next();
            }
        }
        else if (step == VIEW.length + 2)
        {
            look(mc, B_YAW, 8, 30f);
            if (wait > 15)
            {
                DevTestUtil.screenshot(mc, name(), "wounds_b");
                int b = Wounds.spots(idB).length, all = VIEW.length;
                DevTestResults.check(name(), "B's splats (random spots)", b, b == all - A_HITS, String.valueOf(all - A_HITS));
                DevTestResults.check(name(), "hits seen", Wounds.hits - hits0, Wounds.hits - hits0 == all, String.valueOf(all));
                DevTestResults.check(name(), "pictures made", Wounds.built - built0, Wounds.built - built0 >= 2, ">= 2");
                DevTestUtil.unlimitedFps(mc);
                nanos0 = Wounds.nanos;
                fpsSum = 0;
                next();
            }
        }
        else if (step == VIEW.length + 3)
        {
            look(mc, 72, 8, 70f);                      // all three in view, A and B wounded
            fpsSum += DevTestUtil.fps(mc);
            if (wait > 100)
            {
                double frames = fpsSum / 20.0;           // the fps read each tick summed over the 100 ticks / 20 ticks a second
                DevTestResults.value(name(), "Wounds a frame (3 infected in view, 2 wounded)",
                                     String.format("%.4f ms over about %.0f frames", (Wounds.nanos - nanos0) / 1e6 / Math.max(1, frames), frames));
                mc.gameSettings.fovSetting = 70f;
                mc.gameSettings.hideGUI = false;
                hitAsk = -2;                           // the server removes the three
                next();
            }
        }
        else if (wait > 5)
        {
            done = true;
        }
        return !done;
    }

    private void next()
    {
        step++;
        wait = 0;
    }

    private static void look(Minecraft mc, float yaw, float pitch, float fov)
    {
        mc.gameSettings.fovSetting = fov;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
    }

    /** Looks at a point on A (it faces +x, so its left is -z): `left` blocks to its left, `up` blocks over its feet. */
    private void aimAt(Minecraft mc, float left, float up)
    {
        net.minecraft.entity.Entity a = mc.theWorld.getEntityByID(idA);
        if (a == null)
        {
            return;
        }
        double dx = a.posX - mc.thePlayer.posX, dz = a.posZ - left - mc.thePlayer.posZ;
        double dy = a.boundingBox.minY + up - mc.thePlayer.posY;   // the local player's posY is eye level
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        look(mc, yaw, pitch, 70f);
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
        int x = DevTestArena.X, y = DevTestArena.Y, z = DevTestArena.Z;
        double[][] at = {{x - 3.5, z + 0.5}, {x - 3.5, z + 3.5}, {x - 3.5, z - 2.5}};
        if (t == 10)
        {
            w.difficultySetting = EnumDifficulty.NORMAL;
            w.setWorldTime(6000);
            DevTestArena.build(w, p);
            p.capabilities.disableDamage = true;
            p.playerNetServerHandler.setPlayerLocation(x + 0.5, y, z + 0.5, 90, 8);
            for (int i = 0; i < held.length; i++)
            {
                for (int tries = 0; tries < 20 && held[i] == null; tries++)
                {
                    EntityLiving e = Deci.newInfected(w);
                    e.setPosition(at[i][0], y, at[i][1]);
                    if (w.spawnEntityInWorld(e) && Deci.isPlainInfected(e))
                    {
                        held[i] = e;
                    }
                }
            }
            idA = held[0] == null ? -1 : held[0].getEntityId();
            idB = held[1] == null ? -1 : held[1].getEntityId();
        }
        if (t <= 10)
        {
            return;
        }
        if (hitAsk == -2)
        {
            for (EntityLiving e : held)
            {
                if (e != null)
                {
                    e.setDead();
                }
            }
            hitAsk = -3;
            return;
        }
        for (int i = 0; i < held.length; i++)
        {
            EntityLiving e = held[i];
            if (e != null && !e.isDead)
            {
                e.setPosition(at[i][0], y, at[i][1]);
                e.motionX = e.motionZ = 0;
                e.rotationYaw = e.renderYawOffset = e.rotationYawHead = -90; // facing the player
                e.setAttackTarget(null);
                e.setHealth(e.getMaxHealth());
            }
        }
        int ask = hitAsk;
        if (ask >= 0 && ask != hitDone)
        {
            EntityLiving e = held[ask < A_HITS ? 0 : 1];
            if (e != null)
            {
                e.hurtResistantTime = 0;
                applied = e.attackEntityFrom(DamageSource.causePlayerDamage(p), 1.0f);
            }
            hitDone = ask;
        }
    }
}
