package net.decimation.fixes;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * NPC shots as real shots (user request 2026-10-09: "player only takes
 * damage if the tracer hits"). Decimation's shootAt (bandits, soldiers,
 * Soviets) hit its target on 75% of shots, whatever stood between, and drew
 * a tracer that only looked like the shot. With PatchTracer v2 it asks this
 * hook instead: the shot leaves the shooter's eyes toward the target's chest
 * with a random spread (gaussian, sigma per tier, wider for machine guns,
 * narrow for sniper rifles), stops at the first solid block (glass, plants,
 * vines, iron bars, ladders let it through, as for player guns) or the first
 * living thing on the line, which takes Decimation's damage (gun damage / 8,
 * full against infected, "human" source). A block hit shows Decimation's
 * own impact particles; the tracer is drawn along the same line.
 */
public class NpcShots implements BiFunction<Entity, EntityLivingBase, Object>
{
    /** How far a shot flies (Decimation's NPC follow range is 64). */
    static final double RANGE = 96, SNIPER_RANGE = 160;
    private final NpcLoadouts loadouts;
    private final Random random = new Random();
    /**
     * Set while a .50 BMG hit is applied (server thread): ArmorGunfireHandler
     * then counts the victim's armor at half strength (square root of its
     * multiplier). User 2026-10-09: the juggernaut's Barrett, "ridiculous
     * damage since it's .50 BMG".
     */
    public static boolean armorPiercing;
    /** Set while a hit is applied: the shooter tier's damageDealt (NpcLoadouts.onHurt). */
    public static float damageScale = 1;
    /** Dev tests: the last shot's place in its burst (0 = first), -1 when it was a single shot. */
    public static volatile int lastBurstIndex = -1;
    /** Dev tests: rounds the last emptied magazine fired before its reload. */
    public static volatile int lastMagazineFired;
    /** Rounds left in each shooter's magazine. */
    private final java.util.Map<Entity, int[]> magazines = new java.util.WeakHashMap<Entity, int[]>();
    /** Bursts in progress: shooter -> {rounds left, rounds fired}. */
    private final java.util.Map<Entity, int[]> bursts = new java.util.WeakHashMap<Entity, int[]>();
    static final String[] PIERCING = {"barrett"};

    /** Dev tests: when set, every shot's direction {x, y, z, shooter entity id} is added here. */
    public static volatile java.util.Queue<double[]> record;

    public NpcShots(NpcLoadouts loadouts)
    {
        this.loadouts = loadouts;
    }

    @Override
    public Object apply(Entity shooter, EntityLivingBase target)
    {
        ItemStack gun = Deci.npcGun(shooter);
        int damage = Deci.gunDamageOf(gun);
        if (target == null || damage <= 0)
        {
            return null; // Decimation's own shot
        }
        World world = shooter.worldObj;
        double sx = shooter.posX, sy = shooter.posY + shooter.getEyeHeight(), sz = shooter.posZ;
        double dx = target.posX - sx, dy = target.boundingBox.minY + target.height * 0.6 - sy, dz = target.posZ - sz;
        String gunName = cpw.mods.fml.common.registry.GameRegistry.findUniqueIdentifierFor(gun.getItem()).name;
        if (NpcLoadouts.contains(NpcLoadouts.ROCKET, gunName))
        {
            return rocket(shooter, target, gun, gunName, dx, dy, dz);
        }
        double yaw = Math.atan2(dz, dx), pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double sigma = Math.toRadians(loadouts.spread(shooter, gun));
        // automatic guns: bursts, each shot spreading more (recoil), then a pause
        NpcLoadouts.Tier tier = loadouts.byName(shooter.getEntityData().getString(NpcLoadouts.TAG));
        int[] burst = null;
        if (loadouts.autoFire() && Deci.gunIsAutomatic(gun) && !NpcLoadouts.contains(NpcLoadouts.SNIPER, gunName))
        {
            burst = bursts.get(shooter);
            if (burst == null || burst[0] <= 0)
            {
                boolean mg = NpcLoadouts.contains(NpcLoadouts.MG, gunName);
                burst = new int[] {mg ? 6 + random.nextInt(7) : 3 + random.nextInt(4), 0};
                bursts.put(shooter, burst);
            }
            sigma *= 1 + loadouts.recoilSpread() * burst[1];
        }
        lastBurstIndex = burst == null ? -1 : burst[1];
        yaw += random.nextGaussian() * sigma;
        pitch += random.nextGaussian() * sigma;
        double cx = Math.cos(pitch) * Math.cos(yaw), cy = Math.sin(pitch), cz = Math.cos(pitch) * Math.sin(yaw);
        Vec3 start = Vec3.createVectorHelper(sx, sy, sz);
        double range = NpcLoadouts.contains(NpcLoadouts.SNIPER, gunName) ? SNIPER_RANGE : RANGE;
        Vec3 stop = Vec3.createVectorHelper(sx + cx * range, sy + cy * range, sz + cz * range);

        MovingObjectPosition block = traceBlocks(world, start, stop, cx, cy, cz);
        if (block != null)
        {
            stop = block.hitVec;
        }
        Entity hit = null;
        Vec3 hitPoint = null;
        double best = start.distanceTo(stop);
        AxisAlignedBB span = AxisAlignedBB.getBoundingBox(Math.min(sx, stop.xCoord), Math.min(sy, stop.yCoord),
            Math.min(sz, stop.zCoord), Math.max(sx, stop.xCoord), Math.max(sy, stop.yCoord),
            Math.max(sz, stop.zCoord)).expand(1, 1, 1);
        for (Object o : (List<?>) world.getEntitiesWithinAABBExcludingEntity(shooter, span))
        {
            Entity e = (Entity) o;
            if (!(e instanceof EntityLivingBase) || !e.canBeCollidedWith() || e.isDead || sameSide(shooter, e))
            {
                continue; // allies in the line are passed (no friendly fire: Soviets killed each other)
            }
            MovingObjectPosition m = e.boundingBox.expand(0.1, 0.1, 0.1).calculateIntercept(start, stop);
            if (m != null)
            {
                double d = start.distanceTo(m.hitVec);
                if (d < best)
                {
                    best = d;
                    hit = e;
                    hitPoint = m.hitVec;
                }
            }
        }

        Deci.gunFlash(gun);
        if (hit != null)
        {
            armorPiercing = NpcLoadouts.contains(PIERCING, gunName);
            damageScale = tier == null ? 1 : tier.damageDealt;
            try
            {
                hit.attackEntityFrom(Deci.humanDamage(), (float) (damage / (Deci.isInfected(hit) ? 1 : 8)));
            }
            finally
            {
                armorPiercing = false;
                damageScale = 1;
            }
        }
        else if (block != null)
        {
            Deci.sendBlockImpact(block.hitVec.xCoord, block.hitVec.yCoord, block.hitVec.zCoord,
                world.getBlock(block.blockX, block.blockY, block.blockZ),
                world.getBlockMetadata(block.blockX, block.blockY, block.blockZ));
        }
        // the client only needs the direction: a point far along the line keeps it exact even
        // when the shot stopped close to the shooter (client and server positions differ a little)
        Deci.sendNpcShot(shooter, target, sx + cx * 64, sy + cy * 64, sz + cz * 64);
        // the magazine: empty means a reload (the burst ends with it)
        int capacity = Math.max(1, Deci.gunMagazine(gun));
        int[] mag = magazines.get(shooter);
        if (mag == null || mag[1] != capacity)
        {
            mag = new int[] {capacity, capacity};
            magazines.put(shooter, mag);
        }
        if (--mag[0] <= 0)
        {
            lastMagazineFired = capacity;
            mag[0] = capacity;
            if (burst != null)
            {
                burst[0] = 0;
            }
            int r = loadouts.reloadTicks();
            Deci.setNpcShotDelay(shooter, Math.max(0, r - 10), r + 10);
            shooter.worldObj.playSoundAtEntity(shooter, "deci:" + gunName + "MagOut", 1.0f, 1.0f);
            return Boolean.TRUE;
        }
        if (burst == null)
        {
            int[] d = NpcLoadouts.shotDelay(tier, gunName); // back from a reload
            Deci.setNpcShotDelay(shooter, d[0], d[1]);
        }
        if (burst != null)
        {
            burst[0]--;
            burst[1]++;
            if (burst[0] > 0)
            {
                // next round at the gun's rate: shootAt waits cooldown + 1 ticks
                int every = Math.max(1, (int) Math.round(Deci.gunSecondsPerShot(gun) * 20));
                Deci.setNpcShotDelay(shooter, every - 1, every - 1);
            }
            else
            {
                int[] pause = NpcLoadouts.burstPause(tier);
                Deci.setNpcShotDelay(shooter, pause[0], pause[1]);
            }
        }
        java.util.Queue<double[]> r = record;
        if (r != null)
        {
            r.add(new double[] {cx, cy, cz, shooter.getEntityId()});
        }
        return Boolean.TRUE;
    }

    /** Rocket speed (blocks per tick, as the player's RPG) and its gravity (RocketEntity). */
    static final float ROCKET_SPEED = 1.5f;
    static final double ROCKET_GRAVITY = 0.002, ROCKET_MIN = 8, ROCKET_BLAST = 5;

    /**
     * A launcher: a real RocketEntity (flies, drops a little, smoke trail,
     * explodes on what it touches) aimed at the target's chest with the
     * gravity drop added and a spread 1.5 x the tier's. Held (no shot, the
     * long reload starts again) when the target is closer than ROCKET_MIN or
     * an ally stands near the line or within ROCKET_BLAST of the target: a
     * rocket explodes on anything, allies included.
     */
    private Object rocket(Entity shooter, EntityLivingBase target, ItemStack gun, String gunName,
                          double dx, double dy, double dz)
    {
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < ROCKET_MIN || allyInWay(shooter, target))
        {
            return Boolean.TRUE;
        }
        double t = dist / ROCKET_SPEED;
        dy += 0.5 * ROCKET_GRAVITY * t * t; // aim above by the drop over the flight
        double yaw = Math.atan2(dz, dx), pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double sigma = Math.toRadians(loadouts.spread(shooter, gun) * 1.5);
        yaw += random.nextGaussian() * sigma;
        pitch += random.nextGaussian() * sigma;
        Deci.gunFlash(gun);
        Deci.fireRocket(shooter, target, Math.cos(pitch) * Math.cos(yaw), Math.sin(pitch),
                        Math.cos(pitch) * Math.sin(yaw), ROCKET_SPEED);
        shooter.worldObj.playSoundAtEntity(shooter, "deci:" + gunName + "Fire", 4.0f, 1.0f);
        return Boolean.TRUE;
    }

    /** An ally of the shooter within 1.5 blocks of the line to the target, or within ROCKET_BLAST of it. */
    static boolean allyInWay(Entity shooter, EntityLivingBase target)
    {
        Vec3 a = Vec3.createVectorHelper(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);
        Vec3 b = Vec3.createVectorHelper(target.posX, target.boundingBox.minY + target.height * 0.6, target.posZ);
        AxisAlignedBB span = AxisAlignedBB.getBoundingBox(Math.min(a.xCoord, b.xCoord), Math.min(a.yCoord, b.yCoord),
            Math.min(a.zCoord, b.zCoord), Math.max(a.xCoord, b.xCoord), Math.max(a.yCoord, b.yCoord),
            Math.max(a.zCoord, b.zCoord)).expand(ROCKET_BLAST, ROCKET_BLAST, ROCKET_BLAST);
        for (Object o : shooter.worldObj.getEntitiesWithinAABBExcludingEntity(shooter, span))
        {
            Entity e = (Entity) o;
            if (e == target || e.isDead || !sameSide(shooter, e))
            {
                continue;
            }
            if (e.getDistanceToEntity(target) < ROCKET_BLAST
                || e.boundingBox.expand(1.5, 1.5, 1.5).calculateIntercept(a, b) != null)
            {
                return true;
            }
        }
        return false;
    }

    /** Both armed humans of one side: Soviets, bandits, or soldiers (hazmat soldiers included). */
    static boolean sameSide(Entity a, Entity b)
    {
        NpcKind x = Deci.npcKind(a), y = Deci.npcKind(b);
        if (x == null || y == null)
        {
            return false;
        }
        return side(x) == side(y);
    }

    private static int side(NpcKind k)
    {
        return k == NpcKind.HAZMAT ? NpcKind.SOLDIER.ordinal() : k.ordinal();
    }

    /** First block on the line that stops a bullet, or null; see-through blocks are passed. */
    static MovingObjectPosition traceBlocks(World world, Vec3 start, Vec3 stop, double cx, double cy, double cz)
    {
        Vec3 from = Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord);
        for (int n = 0; n < 16; n++)
        {
            // func_147447_a moves its first argument along the line: pass copies
            MovingObjectPosition m = world.func_147447_a(Vec3.createVectorHelper(from.xCoord, from.yCoord, from.zCoord),
                Vec3.createVectorHelper(stop.xCoord, stop.yCoord, stop.zCoord), false, true, false);
            if (m == null || m.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
            {
                return null;
            }
            Block b = world.getBlock(m.blockX, m.blockY, m.blockZ);
            if (!passes(b))
            {
                return m;
            }
            // just past this block, then on
            from = Vec3.createVectorHelper(m.hitVec.xCoord + cx * 0.05, m.hitVec.yCoord + cy * 0.05,
                                           m.hitVec.zCoord + cz * 0.05);
            while (world.getBlock((int) Math.floor(from.xCoord), (int) Math.floor(from.yCoord),
                                  (int) Math.floor(from.zCoord)) == b && from.distanceTo(start) < SNIPER_RANGE)
            {
                from = Vec3.createVectorHelper(from.xCoord + cx * 0.1, from.yCoord + cy * 0.1, from.zCoord + cz * 0.1);
            }
        }
        return null;
    }

    /** Blocks a player's gun shoots through (Decimation GunItem.passThrough*), the vanilla ones. */
    static boolean passes(Block b)
    {
        Material m = b.getMaterial();
        return m == Material.glass || m == Material.vine || m == Material.plants || b == Blocks.iron_bars
            || b == Blocks.ladder || b == Blocks.tallgrass || b == Blocks.double_plant || b == Blocks.deadbush;
    }
}
