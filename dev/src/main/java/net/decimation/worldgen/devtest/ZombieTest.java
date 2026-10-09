package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.decimation.fixes.Deci;
import net.decimation.fixes.InfectedVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * Zombie variants (-Ptest=zombies, net.decimation.fixes.InfectedVariants):
 * share of runners / riot / screamers among 300 plain infected, their speed
 * and health, riot damage share, a screamer sending infected 25 blocks away
 * after its target, night frenzy on at night and off by day, and a lineup
 * photo (zombies_lineup: common, runner, riot, screamer).
 */
public class ZombieTest extends DevTestMode
{
    public String name() { return "zombies"; }

    private static final String[] LINEUP = {null, "runner", "riot", "screamer"};
    private volatile int ticks;
    private volatile boolean done, shoot;
    private volatile double camX, camY, camZ;
    private final List<EntityLiving> held = new ArrayList<EntityLiving>();
    private final List<EntityLiving> alerted = new ArrayList<EntityLiving>();
    private EntityLiving screamer, bait, night;
    private double nightSpeed0, nightHit0;
    private boolean screamChecked;
    private int x0, z0, ground, lineGround;

    public boolean client(Minecraft mc)
    {
        if (shoot && mc.thePlayer != null)
        {
            mc.gameSettings.hideGUI = true;
            mc.thePlayer.setPositionAndRotation(camX, camY, camZ, 180, 10);
            if (ticks >= 330)
            {
                DevTestUtil.screenshot(mc, name(), "zombies_lineup");
                shoot = false;
                mc.gameSettings.hideGUI = false;
            }
        }
        if (ticks > 345)
        {
            done = true;
        }
        return !done;
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
        if (t == 20)
        {
            w.difficultySetting = EnumDifficulty.NORMAL;
            w.setWorldTime(6000);
            p.capabilities.isFlying = true;
            DevTestArena.build(w, p);
            x0 = DevTestArena.X;
            z0 = DevTestArena.Z;
            ground = DevTestArena.Y;
            batch(w);
            screamSetup(w);
        }
        if (t > 20 && t <= 80 && !screamChecked)
        {
            // the AI may drop a far target: set it again each tick, the scream reads it first
            screamer.setAttackTarget(bait);
            bait.setPosition(x0 + 0.5, ground, z0 - 10 + 0.5);
            bait.motionX = bait.motionY = bait.motionZ = 0;
            if (InfectedVariants.lastAlerted > 0 || t == 80)
            {
                screamChecked = true;
                int n = InfectedVariants.lastAlerted;
                DevTestResults.check(name(), "screamer reaches infected 25 blocks away", n + " / " + alerted.size(),
                                     n >= alerted.size() && alerted.size() > 0, ">= all " + alerted.size());
                for (EntityLiving z : alerted)
                {
                    z.setDead();
                }
                screamer.setDead();
                bait.setDead();
            }
        }
        if (t == 100)
        {
            night = Deci.newInfected(w);
            night.setPosition(x0 + 0.5, ground, z0 - 24 + 0.5);
            w.spawnEntityInWorld(night);
            InfectedVariants.instance().apply(night, InfectedVariants.instance().byName("riot"));
            w.setWorldTime(18000);
            nightSpeed0 = ratio(night); // Decimation resets the base every tick: compare value / base
            nightHit0 = night.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        }
        if (t == 150)
        {
            double s = ratio(night);
            double h = night.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
            DevTestResults.check(name(), "night frenzy speed / attack", String.format("x%.2f / %.1f -> %.1f",
                                 s / nightSpeed0, nightHit0, h), Math.abs(s / nightSpeed0 - 1.2) < 0.01
                                 && Math.abs(h - nightHit0 - 2) < 0.01, "x1.20 / +2");
            w.setWorldTime(6000);
        }
        if (t == 200)
        {
            double s = ratio(night);
            DevTestResults.check(name(), "night frenzy off by day", String.format("x%.2f", s / nightSpeed0),
                                 Math.abs(s / nightSpeed0 - 1) < 0.01, "x1.00");
            night.setDead();
        }
        if (t == 220)
        {
            lineup(w, p);
        }
        if (t > 220 && t < 340)
        {
            if (t % 10 == 0)
            {
                for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, p.boundingBox.expand(40, 30, 40)))
                {
                    if (!held.contains(o))
                    {
                        ((EntityLiving) o).setDead(); // natural spawns walk into the picture
                    }
                }
            }
            for (int i = 0; i < held.size(); i++)
            {
                EntityLiving z = held.get(i);
                z.setPosition(x0 + 0.5 + (i - 1.5) * 1.4, lineGround, z0 - 6 + 0.5);
                z.motionX = z.motionY = z.motionZ = 0;
                z.rotationYaw = z.rotationYawHead = z.renderYawOffset = 0;
                z.setAttackTarget(null);
            }
        }
        if (t == 340)
        {
            for (EntityLiving z : held)
            {
                z.setDead();
            }
        }
    }

    private static double ratio(EntityLiving z)
    {
        net.minecraft.entity.ai.attributes.IAttributeInstance a = z.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        return a.getAttributeValue() / a.getBaseValue();
    }

    /** 300 plain infected: variant shares, speed, health, riot damage share. */
    private void batch(World w)
    {
        InfectedVariants iv = InfectedVariants.instance();
        Map<String, Integer> count = new TreeMap<String, Integer>();
        double common = 0, runner = 0;
        int n = 0;
        boolean healthOk = true;
        for (int i = 0; i < 300; i++)
        {
            EntityLiving z = Deci.newInfected(w);
            z.setPosition(x0 + 0.5 + i % 10, ground, z0 - 24 + 0.5 + i / 10 % 10);
            if (!w.spawnEntityInWorld(z))
            {
                continue; // Decimation turns some into hulks / bloaters
            }
            n++;
            String v = z.getEntityData().getString(InfectedVariants.TAG);
            count.put(v, (count.containsKey(v) ? count.get(v) : 0) + 1);
            double speed = z.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
            double base = z.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue();
            if (v.equals("none"))
            {
                common = speed / base;
            }
            if (v.equals("runner"))
            {
                runner = speed / base;
            }
            InfectedVariants.Variant var = iv.byName(v);
            healthOk &= var == null || Math.abs(z.getMaxHealth() - var.health) < 0.01;
            z.setDead();
        }
        DevTestResults.value(name(), "variants among " + n + " infected", count);
        int runners = count.containsKey("runner") ? count.get("runner") : 0;
        int riot = count.containsKey("riot") ? count.get("riot") : 0;
        int screamers = count.containsKey("screamer") ? count.get("screamer") : 0;
        DevTestResults.check(name(), "variant shares runner / riot / screamer",
                             runners + " / " + riot + " / " + screamers,
                             runners > n * 0.06 && runners < n * 0.2 && riot > n * 0.03 && riot < n * 0.15
                             && screamers > 0 && screamers < n * 0.1, "about 12% / 8% / 5%");
        DevTestResults.check(name(), "runner speed vs common, health", String.format("x%.2f vs x%.2f, %s", runner,
                             common, healthOk), Math.abs(runner - 1.55) < 0.01 && Math.abs(common - 1) < 0.01
                             && healthOk, "x1.55 vs x1.00, true");
        EntityLiving riotZ = Deci.newInfected(w);
        riotZ.setPosition(x0 + 0.5, ground, z0 - 30 + 0.5);
        w.spawnEntityInWorld(riotZ);
        iv.apply(riotZ, iv.byName("riot"));
        float before = riotZ.getHealth();
        riotZ.attackEntityFrom(Deci.gunDamage(DevTestUtil.player()), 10);
        DevTestResults.check(name(), "gun hit 10 on riot", String.format("%.1f", before - riotZ.getHealth()),
                             Math.abs(before - riotZ.getHealth() - 5) < 0.01, "5.0");
        riotZ.setDead();
    }

    /**
     * A bait bandit on a block 20 up; the screamer 10 blocks off; 6 infected
     * 25 blocks from the screamer, too far to notice the bait themselves.
     */
    private void screamSetup(World w)
    {
        int y = ground;
        InfectedVariants.lastAlerted = 0;
        bait = Deci.newBandit(w);
        bait.setPosition(x0 + 0.5, y, z0 - 10 + 0.5);
        w.spawnEntityInWorld(bait);
        // Decimation turns 10% of infected spawns into hulks / bloaters: try until one is in
        for (int attempt = 0; attempt < 10; attempt++)
        {
            screamer = Deci.newInfected(w);
            screamer.setPosition(x0 - 4.5, ground, z0 - 10 + 0.5);
            if (w.spawnEntityInWorld(screamer))
            {
                break;
            }
        }
        InfectedVariants.instance().apply(screamer, InfectedVariants.instance().byName("screamer"));
        for (int i = 0; i < 6; i++)
        {
            EntityLiving z = Deci.newInfected(w);
            z.setPosition(x0 - 29.5, ground, z0 - 13 + i + 0.5);
            if (w.spawnEntityInWorld(z))
            {
                alerted.add(z);
            }
        }
        screamer.setAttackTarget(bait);
    }

    private void lineup(World w, EntityPlayerMP p)
    {
        for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, p.boundingBox.expand(40, 30, 40)))
        {
            ((EntityLiving) o).setDead();
        }
        lineGround = DevTestArena.Y;
        for (int dx = -4; dx <= 4; dx++)
        {
            for (int dz = -7; dz <= 1; dz++)
            {
                for (int dy = -1; dy <= 3; dy++)
                {
                    net.minecraft.block.material.Material m = w.getBlock(x0 + dx, lineGround + dy, z0 + dz).getMaterial();
                    if (m == net.minecraft.block.material.Material.plants || m == net.minecraft.block.material.Material.vine)
                    {
                        w.setBlockToAir(x0 + dx, lineGround + dy, z0 + dz);
                    }
                }
            }
        }
        for (String v : LINEUP)
        {
            EntityLiving z = null;
            for (int attempt = 0; attempt < 10 && z == null; attempt++)
            {
                EntityLiving c = Deci.newInfected(w);
                c.getEntityData().setString(InfectedVariants.TAG, "none"); // no random variant on join
                c.setPosition(x0 + 0.5, lineGround, z0 - 6 + 0.5);
                if (w.spawnEntityInWorld(c))
                {
                    z = c;
                }
            }
            if (v != null)
            {
                InfectedVariants.instance().apply(z, InfectedVariants.instance().byName(v));
            }
            held.add(z);
        }
        camX = x0 + 0.5;
        camY = lineGround + 1.2;
        camZ = z0 + 0.5;
        p.setPositionAndUpdate(camX, camY, camZ);
        shoot = true;
    }
}
