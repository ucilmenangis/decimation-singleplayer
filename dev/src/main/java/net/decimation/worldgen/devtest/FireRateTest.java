package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcKind;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * NPC fire rate per tier and gun (user 10 Oktober 2026: "juggernaut who using pkm, the firerate is
 * ridiculous ... elite who using pkp is most balance"). One NPC at a time on the test arena, a pig
 * (1000 health) 12 blocks away; only the NPC's own AI shoots (no extra shootAt calls: those halve
 * the cooldowns). Every shot the NPC fires is timed (Deci.npcShotHook is asked once per shot, also
 * when Decimation then fires it itself). Per case: shots in 15 s, rounds per minute over the whole
 * time, the shortest gap and the median gap between shots, pauses (gaps of 5 ticks or more) and
 * their mean. -Pcases=tier:gun,tier:gun picks the cases.
 */
public class FireRateTest extends DevTestMode
{
    public String name() { return "firerate"; }

    private static final String DEFAULT = "juggernaut:pkm,juggernaut:pkp,elite_military:pkp,elite_military:pkm,"
        + "elite_military:mg3,military:pkp,military:ak74,bandit_heavy:pkm,bandit_medium:akm,bandit_light:uzi,"
        + "soldier_marine:m4a4,soldier_marine:m240";
    private final String[] cases = System.getProperty("deciworldgen.autotest.cases", DEFAULT).split(",");
    private static final int LEN = 330, WARM = 30, START = 20, DIST = 12;
    private static final int Y = DevTestArena.Y;

    private volatile int ticks;
    private volatile boolean done;
    private EntityLiving npc;
    private EntityPig pig;
    private final List<List<Integer>> shots = new ArrayList<List<Integer>>();
    private final int[] otherTarget;
    private java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object> restore;
    private double x0, z0;

    public FireRateTest()
    {
        otherTarget = new int[cases.length];
        for (int i = 0; i < cases.length; i++)
        {
            shots.add(Collections.synchronizedList(new ArrayList<Integer>()));
        }
    }

    public boolean client(Minecraft mc)
    {
        if (ticks > START + cases.length * LEN + 5 && !done)
        {
            for (int i = 0; i < cases.length; i++)
            {
                report(i);
            }
            done = true;
        }
        return !done;
    }

    private void report(int i)
    {
        List<Integer> s = new ArrayList<Integer>(shots.get(i));
        List<Integer> gaps = new ArrayList<Integer>();
        int pauses = 0, pauseSum = 0;
        for (int k = 1; k < s.size(); k++)
        {
            int g = s.get(k) - s.get(k - 1);
            gaps.add(g);
            if (g >= 5)
            {
                pauses++;
                pauseSum += g;
            }
        }
        Collections.sort(gaps);
        double seconds = (LEN - WARM) / 20.0;
        DevTestResults.value(name(), cases[i], String.format(
            "%d shots, %.0f rpm overall, gap min %s median %s ticks, %d pauses of %.1f ticks%s",
            s.size(), s.size() * 60 / seconds, gaps.isEmpty() ? "-" : gaps.get(0),
            gaps.isEmpty() ? "-" : gaps.get(gaps.size() / 2), pauses, pauses == 0 ? 0 : pauseSum / (double) pauses,
            otherTarget[i] > 0 ? ", " + otherTarget[i] + " at other targets" : ""));
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
            final java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object>
                real = Deci.npcShotHook();
            restore = real;
            Deci.setNpcShotHook((s, target) -> {
                int ph = (ticks - START) / LEN, in = (ticks - START) % LEN;
                if (s == npc && ph >= 0 && ph < cases.length && in >= WARM)
                {
                    if (target == pig)
                    {
                        shots.get(ph).add(ticks);
                        pig.hurtResistantTime = 0;
                    }
                    else
                    {
                        otherTarget[ph]++;
                    }
                }
                return real.apply(s, target);
            });
            world.difficultySetting = EnumDifficulty.NORMAL;
            world.setWorldTime(6000);
            p.capabilities.isFlying = true;
            DevTestArena.build(world, p);
            x0 = DevTestArena.X + 0.5;
            z0 = DevTestArena.Z + 0.5;
            p.setPositionAndUpdate(x0 - DIST / 2.0, Y + 4, z0 - 8);
        }
        if (t < START)
        {
            return;
        }
        if (t >= START + cases.length * LEN)
        {
            if (t == START + cases.length * LEN)
            {
                cleanup();
                Deci.setNpcShotHook(restore);
            }
            return;
        }
        int phase = (t - START) / LEN, in = (t - START) % LEN;
        if (in == 0)
        {
            setup(world, phase);
        }
        if (npc == null)
        {
            return;
        }
        npc.setPosition(x0, Y, z0);
        npc.motionX = npc.motionY = npc.motionZ = 0;
        pig.setPosition(x0 - DIST, Y, z0);
        pig.motionX = pig.motionY = pig.motionZ = 0;
        pig.setHealth(pig.getMaxHealth());
        npc.setRevengeTarget(pig);
        npc.setAttackTarget(pig);
    }

    private void setup(World world, int phase)
    {
        cleanup();
        String[] c = cases[phase].split(":");
        NpcLoadouts l = NpcLoadouts.instance();
        NpcLoadouts.Tier tier = l.byName(c[0]);
        if (tier == null)
        {
            DevTestResults.value(name(), cases[phase], "no such tier");
            npc = null;
            return;
        }
        for (int attempt = 0; attempt < 20; attempt++) // Decimation refuses some spawns (mechs, hulks)
        {
            npc = tier.kind == NpcKind.BANDIT ? Deci.newBandit(world)
                : tier.kind == NpcKind.SOLDIER ? Deci.newSoldier(world) : Deci.newSoviet(world);
            l.equip(npc, tier, npc.getEntityData());
            // the tier's kit stays, only the gun is the case's (the kit was chosen for the rolled gun's class)
            String spec = npc.getEntityData().getString(NpcLoadouts.GUN_TAG);
            int k = spec.indexOf(';');
            npc.getEntityData().setString(NpcLoadouts.GUN_TAG, c[1] + (k < 0 ? "" : spec.substring(k)));
            npc.setPosition(x0, Y, z0);
            if (world.spawnEntityInWorld(npc))
            {
                break;
            }
        }
        pig = new EntityPig(world);
        pig.getEntityData().setBoolean(net.decimation.fixes.VanillaMobs.KEEP, true);
        pig.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1000);
        pig.setHealth(1000);
        pig.setPosition(x0 - DIST, Y, z0);
        world.spawnEntityInWorld(pig);
    }

    private void cleanup()
    {
        if (npc != null)
        {
            npc.setDead();
        }
        if (pig != null)
        {
            pig.setDead();
        }
    }
}
