package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcKind;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * Traced NPC shots (-Ptest=shots, net.decimation.fixes.NpcShots): a medium
 * tier bandit with an AKM, held in place high in the air, fires at a pig
 * (1000 health, so it never dies) 10 blocks away, then 20, then 10 with a
 * stone wall between. Only shots on the line hit, so: most hit at 10, fewer
 * at 20, none through the wall (the wall shows impact particles: shots_wall).
 */
public class ShotTest extends DevTestMode
{
    public String name() { return "shots"; }

    private static final int[] DIST = {10, 20, 10, 24, 20};
    private static final boolean[] WALL = {false, false, true, false, false};
    /** Phase with an AKM firing bursts (accuracy by place in the burst). */
    private static final int BURST_PHASE = 4;
    private final int[][] byIndex = new int[2][13]; // shots, hits by place in burst
    private final java.util.List<Integer> burstShotTicks = new java.util.ArrayList<Integer>();
    /** Phase with an RPG-7 bandit (real rockets, NpcShots.rocket). */
    private static final int ROCKET_PHASE = 3;
    private static final int LEN = 500, START = 20; // long enough for a reload (4 s) or two per phase
    private static final int Y = 140;

    private volatile int ticks;
    private volatile boolean done;
    private volatile boolean shoot;
    private EntityLiving bandit;
    private java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object> restore;
    private EntityPig pig;
    private final int[] hitCount = new int[DIST.length], shotCount = new int[DIST.length];
    private int shotTaken = -1, rocketShots, rocketShotFor;
    private volatile int rocketFiredAt;
    private float pigLast = -1;
    private double x0, z0;

    public boolean client(Minecraft mc)
    {
        int t = ticks;
        int phase = (t - START) / LEN;
        if (phase == 2 && (t - START) % LEN > 200 && shotTaken < 2 && mc.thePlayer != null)
        {
            shotTaken = 2;
            DevTestUtil.screenshot(mc, name(), "shots_wall");
        }
        if (phase == 2 && mc.thePlayer != null)
        {
            mc.gameSettings.hideGUI = true;
            mc.thePlayer.setPositionAndRotation(x0 + 4, Y + 1, z0 - 5, 90, 10); // beside the wall, looking west at it
        }
        if (phase == ROCKET_PHASE && mc.thePlayer != null)
        {
            // side view of the line: rockets and their smoke in flight
            mc.gameSettings.hideGUI = true;
            mc.thePlayer.setPositionAndRotation(x0 - 6, Y + 1, z0 - 5, 0, 5);
            int fired = rocketFiredAt;
            if (fired > 0 && fired != rocketShotFor && t - fired >= 3 && rocketShots < 4)
            {
                rocketShotFor = fired; // 3 ticks after a launch: about 5 blocks out, in front of the camera
                DevTestUtil.screenshot(mc, name(), "shots_rocket_" + rocketShots++);
            }
        }
        if (t > START + DIST.length * LEN + 10 && !done)
        {
            mc.gameSettings.hideGUI = false;
            for (int i = 0; i < DIST.length; i++)
            {
                DevTestResults.value(name(), "hits at " + DIST[i] + (WALL[i] ? " behind a wall" : ""),
                                     hitCount[i] + " / " + shotCount[i]);
            }
            DevTestResults.check(name(), "rockets fired and hits", shotCount[ROCKET_PHASE] + " fired, "
                                 + hitCount[ROCKET_PHASE] + " hurt the pig", shotCount[ROCKET_PHASE] >= 2
                                 && hitCount[ROCKET_PHASE] >= 1, ">= 2 rockets, >= 1 hit at 24 blocks");
            int first = byIndex[0][0], firstHit = byIndex[1][0], late = 0, lateHit = 0, bursts = first;
            for (int i = 3; i < 13; i++)
            {
                late += byIndex[0][i];
                lateHit += byIndex[1][i];
            }
            DevTestResults.value(name(), "burst shots by place", java.util.Arrays.toString(byIndex[0])
                                 + " hits " + java.util.Arrays.toString(byIndex[1]));
            DevTestResults.check(name(), "AKM fires bursts", bursts + " bursts, " + shotCount[BURST_PHASE] + " shots",
                                 bursts >= 3 && shotCount[BURST_PHASE] >= bursts * 3, ">= 3 bursts of >= 3 rounds");
            DevTestResults.check(name(), "recoil: first vs 4th+ shot hit rate at 20",
                                 String.format("%d/%d vs %d/%d", firstHit, first, lateHit, late),
                                 first > 0 && late > 0 && firstHit / (double) first > lateHit / (double) late,
                                 "first shots hit more often");
            // the first magazine: shots before the first gap of 3 s or more (the reload)
            int firstMag = 0, gap = 0;
            for (int i = 0; i < burstShotTicks.size(); i++)
            {
                firstMag++;
                if (i + 1 < burstShotTicks.size() && burstShotTicks.get(i + 1) - burstShotTicks.get(i) >= 60)
                {
                    gap = burstShotTicks.get(i + 1) - burstShotTicks.get(i);
                    break;
                }
            }
            int cap = net.decimation.fixes.Deci.gunMagazine(new ItemStack(
                cpw.mods.fml.common.registry.GameRegistry.findItem("deci", "akm")));
            DevTestResults.check(name(), "AKM magazine then reload", firstMag + " rounds, then " + gap + " ticks",
                                 firstMag == cap && gap >= 60, cap + " rounds, then >= 60 ticks (reload 80)");
            double r10 = rate(0), r20 = rate(1);
            DevTestResults.check(name(), "hit rate at 10 blocks", String.format("%.0f%%", 100 * r10),
                                 shotCount[0] > 20 && r10 > 0.6, "> 60% (spread 1.3 deg)");
            DevTestResults.check(name(), "hit rate at 20 blocks", String.format("%.0f%%", 100 * r20),
                                 shotCount[1] > 20 && r20 < r10, "below the 10 block rate");
            DevTestResults.check(name(), "hits through a wall", hitCount[2] + " / " + shotCount[2],
                                 shotCount[2] > 20 && hitCount[2] == 0, "0 (the shot stops at the wall)");
            done = true;
        }
        return !done;
    }

    private double rate(int i)
    {
        return shotCount[i] == 0 ? 0 : hitCount[i] / (double) shotCount[i];
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World world = p.worldObj;
        if (t == START - 10)
        {
            // count every shot the hook fires (the bandit's own AI shoots too)
            final java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object>
                real = Deci.npcShotHook();
            Deci.setNpcShotHook((s, target) -> {
                int ph = (ticks - START) / LEN;
                boolean ours = s == bandit && target == pig && ph >= 0 && ph < DIST.length;
                float h = target == null ? 0 : target.getHealth();
                if (ours)
                {
                    pig.hurtResistantTime = 0;
                }
                Object r = real.apply(s, target);
                if (r != null && ours)
                {
                    if (ph == ROCKET_PHASE && pig.getDistanceToEntity(bandit) >= 8)
                    {
                        rocketFiredAt = ticks;
                    }
                    shotCount[ph]++;
                    if (ph == BURST_PHASE)
                    {
                        burstShotTicks.add(ticks);
                    }
                    if (ph == BURST_PHASE && net.decimation.fixes.NpcShots.lastBurstIndex >= 0)
                    {
                        int bi = Math.min(12, net.decimation.fixes.NpcShots.lastBurstIndex);
                        byIndex[0][bi]++;
                        byIndex[1][bi] += pig.getHealth() < h ? 1 : 0;
                    }
                    if (ph != ROCKET_PHASE && pig.getHealth() < h)
                    {
                        hitCount[ph]++;
                        pig.setHealth(pig.getMaxHealth());
                    }
                }
                return r;
            });
            restore = real;
            world.difficultySetting = EnumDifficulty.NORMAL;
            world.setWorldTime(6000);
            p.capabilities.isFlying = true;
            x0 = Math.floor(p.posX) + 0.5;
            z0 = Math.floor(p.posZ) + 0.5;
        }
        if (t < START || t >= START + DIST.length * LEN)
        {
            if (t == START + DIST.length * LEN)
            {
                cleanup(world);
                Deci.setNpcShotHook(restore);
            }
            return;
        }
        int phase = (t - START) / LEN, in = (t - START) % LEN;
        if (in == 0)
        {
            setup(world, p, phase);
        }
        // pig DIST west of the bandit, both held in place in the air
        bandit.setPosition(x0, Y, z0);
        bandit.motionX = bandit.motionY = bandit.motionZ = 0;
        pig.setPosition(x0 - DIST[phase], Y, z0);
        pig.motionX = pig.motionY = pig.motionZ = 0;
        pig.fallDistance = 0;
        bandit.setRevengeTarget(pig);
        if (phase == ROCKET_PHASE)
        {
            if (pigLast >= 0 && pig.getHealth() < pigLast)
            {
                hitCount[phase]++;
                pig.setHealth(pig.getMaxHealth());
            }
            pig.hurtResistantTime = 0;
            pigLast = pig.getHealth();
        }
        if (phase != BURST_PHASE)
        {
            Deci.banditShootAt(bandit, pig); // the AI shoots too: cooldowns run twice as fast here
        }
    }

    private void setup(World world, EntityPlayerMP p, int phase)
    {
        cleanup(world);
        boolean rocket = phase == ROCKET_PHASE;
        NpcLoadouts.Tier tier = NpcLoadouts.instance().byName(rocket ? "bandit_rpg" : "bandit_medium");
        bandit = Deci.newBandit(world);
        NpcLoadouts.instance().equip(bandit, tier, bandit.getEntityData());
        // single shot phases use a semi automatic rifle (bursts would mix recoil into the spread)
        String gun = rocket ? "rpg7" : phase == BURST_PHASE ? "akm" : "sks";
        bandit.getEntityData().setString(NpcLoadouts.GUN_TAG, gun);
        bandit.setPosition(x0, Y, z0);
        world.spawnEntityInWorld(bandit);
        Deci.setNpcGun(bandit, new ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("deci", gun)));
        if (!rocket && phase != BURST_PHASE)
        {
            Deci.setNpcShotDelay(bandit, 1, 1);
        }
        pigLast = -1;
        pig = new EntityPig(world);
        pig.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1000);
        pig.setHealth(1000);
        pig.setPosition(x0 - DIST[phase], Y, z0);
        world.spawnEntityInWorld(pig);
        for (int dy = -1; dy <= 3; dy++)
        {
            for (int dz = -2; dz <= 2; dz++)
            {
                world.setBlock((int) Math.floor(x0) - 5, Y + dy, (int) Math.floor(z0) + dz,
                               WALL[phase] ? Blocks.stone : Blocks.air);
            }
        }
        // blocks to stand on (held in the air, their client copies fall)
        world.setBlock((int) Math.floor(x0), Y - 1, (int) Math.floor(z0), Blocks.stone);
        world.setBlock((int) Math.floor(x0) - DIST[phase], Y - 1, (int) Math.floor(z0), Blocks.stone);
        p.setPositionAndUpdate(x0 + 4, Y + 1, z0 - 5);
    }

    private void cleanup(World world)
    {
        for (int d : DIST)
        {
            world.setBlockToAir((int) Math.floor(x0) - d, Y - 1, (int) Math.floor(z0));
        }
        world.setBlockToAir((int) Math.floor(x0), Y - 1, (int) Math.floor(z0));
        if (bandit != null)
        {
            bandit.setDead();
        }
        if (pig != null)
        {
            pig.setDead();
        }
        for (int dy = -1; dy <= 3; dy++)
        {
            for (int dz = -2; dz <= 2; dz++)
            {
                world.setBlock((int) Math.floor(x0) - 5, Y + dy, (int) Math.floor(z0) + dz, Blocks.air);
            }
        }
    }
}
