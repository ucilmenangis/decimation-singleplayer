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
    static final double RANGE = 96;
    private final NpcLoadouts loadouts;
    private final Random random = new Random();
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
        double yaw = Math.atan2(dz, dx), pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double sigma = Math.toRadians(loadouts.spread(shooter, gun));
        yaw += random.nextGaussian() * sigma;
        pitch += random.nextGaussian() * sigma;
        double cx = Math.cos(pitch) * Math.cos(yaw), cy = Math.sin(pitch), cz = Math.cos(pitch) * Math.sin(yaw);
        Vec3 start = Vec3.createVectorHelper(sx, sy, sz);
        Vec3 stop = Vec3.createVectorHelper(sx + cx * RANGE, sy + cy * RANGE, sz + cz * RANGE);

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
            hit.attackEntityFrom(Deci.humanDamage(), (float) (damage / (Deci.isInfected(hit) ? 1 : 8)));
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
        java.util.Queue<double[]> r = record;
        if (r != null)
        {
            r.add(new double[] {cx, cy, cz, shooter.getEntityId()});
        }
        return Boolean.TRUE;
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
                                  (int) Math.floor(from.zCoord)) == b && from.distanceTo(start) < RANGE)
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
