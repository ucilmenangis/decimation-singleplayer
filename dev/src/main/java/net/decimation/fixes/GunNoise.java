package net.decimation.fixes;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Gunshot noise (user request 10 / 11 Oktober 2026): a shot is heard `range` blocks away (64), a
 * suppressed one `suppressedRange` (12). Infected that hear it walk to where it was fired and
 * search around it for a while; armed NPCs that hear it do the same, unless the shooter is on
 * their own side (Decimation's own isHostileTo decides: "if the shot from their team or group,
 * they dont need to go"). Player shots are seen as the held gun's ammo dropping (no packet hook);
 * NPC shots come from NpcShots. Neither reaction overrides a target the mob already has: an
 * infected or NPC that sees an enemy fights it.
 *
 * The walk is an AI task added to every infected (priority 3, before their wander at 7) and armed
 * NPC (priority 2, before their wander at 3): a straight path search is limited to the mob's
 * follow range, so it walks in legs of 12 blocks toward the spot, then to random points within 6
 * blocks of it until its search time is over. Config: config/deciworldgen_noise.cfg.
 */
public final class GunNoise
{
    private static GunNoise instance;
    private static final String TAG = "deciwgNoise";

    private boolean enabled = true;
    private double range = 64, suppressedRange = 12;
    private int zombieSearch = 30 * 20, npcSearch = 40 * 20;

    /** Per player: held slot, item and ammo seen last tick (a drop in ammo = a shot). */
    private final Map<EntityPlayer, int[]> lastAmmo = new WeakHashMap<EntityPlayer, int[]>();
    /** Per shooter: the last noise (tick, x, z), so a machine gun is one noise per half second. */
    private final Map<Entity, double[]> lastNoise = new WeakHashMap<Entity, double[]>();

    public GunNoise(File configDir)
    {
        instance = this;
        Configuration cfg = new Configuration(new File(configDir, "deciworldgen_noise.cfg"));
        enabled = cfg.getBoolean("enabled", "noise", enabled, "gunshots draw infected and hostile NPCs");
        range = cfg.getFloat("range", "noise", (float) range, 0, 256, "blocks an unsuppressed shot is heard");
        suppressedRange = cfg.getFloat("suppressedRange", "noise", (float) suppressedRange, 0, 256,
            "blocks a shot through a suppressor is heard (0 = silent)");
        zombieSearch = 20 * cfg.getInt("zombieSearchSeconds", "noise", zombieSearch / 20, 1, 600,
            "how long infected keep walking to / searching around a shot they heard");
        npcSearch = 20 * cfg.getInt("npcSearchSeconds", "noise", npcSearch / 20, 1, 600,
            "how long NPCs keep walking to / searching around a shot they heard");
        cfg.save();
    }

    public static GunNoise instance()
    {
        return instance;
    }

    // ------------------------------------------------------------------ sources

    /** A shot by a player or an NPC with this gun (server side). */
    public static void shot(Entity shooter, ItemStack gun)
    {
        if (instance == null || !instance.enabled || shooter == null || shooter.worldObj.isRemote)
        {
            return;
        }
        double[] last = instance.lastNoise.get(shooter);
        long now = shooter.worldObj.getTotalWorldTime();
        if (last != null && now - last[0] < 10 && Math.abs(last[1] - shooter.posX) + Math.abs(last[2] - shooter.posZ) < 8)
        {
            return;                                   // a burst is one noise
        }
        instance.lastNoise.put(shooter, new double[] {now, shooter.posX, shooter.posZ});
        instance.heard(shooter, Deci.hasSuppressor(gun) ? instance.suppressedRange : instance.range);
    }

    /** Players: a drop in the held gun's ammo is a shot (the server applies every shot so). */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        EntityPlayer p = event.player;
        if (event.phase != TickEvent.Phase.END || p.worldObj.isRemote)
        {
            return;
        }
        ItemStack held = p.getHeldItem();
        int ammo = Deci.gunAmmo(held);
        if (ammo < 0)
        {
            lastAmmo.remove(p);
            return;
        }
        int slot = p.inventory.currentItem, item = net.minecraft.item.Item.getIdFromItem(held.getItem());
        int[] last = lastAmmo.get(p);
        if (last != null && last[0] == slot && last[1] == item && ammo < last[2])
        {
            shot(p, held);
        }
        lastAmmo.put(p, new int[] {slot, item, ammo});
    }

    /** Every mob within `r` that cares turns its attention to the shot. */
    private void heard(Entity shooter, double r)
    {
        if (r <= 0)
        {
            return;
        }
        World w = shooter.worldObj;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(shooter.posX - r, shooter.posY - 32, shooter.posZ - r,
                                                         shooter.posX + r, shooter.posY + 32, shooter.posZ + r);
        List<?> mobs = w.getEntitiesWithinAABB(EntityCreature.class, box);
        long now = w.getTotalWorldTime();
        for (Object o : mobs)
        {
            EntityCreature m = (EntityCreature) o;
            if (m == shooter || m.isDead || m.getDistanceSqToEntity(shooter) > r * r)
            {
                continue;
            }
            boolean infected = Deci.isInfectedKind(m), npc = Deci.npcKind(m) != null;
            if (!infected && !npc)
            {
                continue;
            }
            if (npc && !(shooter instanceof EntityLivingBase && Deci.npcHostileTo(m, (EntityLivingBase) shooter)))
            {
                continue;                             // a friend's shot (or a player it does not fight)
            }
            NBTTagCompound n = new NBTTagCompound();
            n.setDouble("x", shooter.posX);
            n.setDouble("y", shooter.posY);
            n.setDouble("z", shooter.posZ);
            n.setLong("until", now + (infected ? zombieSearch : npcSearch));
            m.getEntityData().setTag(TAG, n);
        }
    }

    // ------------------------------------------------------------------ reaction

    /** The walk-to-the-shot task on every infected and armed NPC as they join a world (server). */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event)
    {
        if (event.world.isRemote || !(event.entity instanceof EntityCreature))
        {
            return;
        }
        EntityCreature m = (EntityCreature) event.entity;
        if (Deci.isInfectedKind(m))
        {
            m.tasks.addTask(3, new Investigate(m, 1.0));
        }
        else if (Deci.npcKind(m) != null)
        {
            m.tasks.addTask(2, new Investigate(m, 1.0));
        }
    }

    /** The noise a mob heard and still follows, or null. */
    static NBTTagCompound noise(EntityCreature m)
    {
        NBTTagCompound d = m.getEntityData();
        if (!d.hasKey(TAG))
        {
            return null;
        }
        NBTTagCompound n = d.getCompoundTag(TAG);
        if (n.getLong("until") < m.worldObj.getTotalWorldTime())
        {
            d.removeTag(TAG);
            return null;
        }
        return n;
    }

    static final class Investigate extends EntityAIBase
    {
        private final EntityCreature m;
        private final double speed;
        private int cooldown;

        Investigate(EntityCreature m, double speed)
        {
            this.m = m;
            this.speed = speed;
            setMutexBits(1);
        }

        private boolean busy()
        {
            return m.getAttackTarget() != null || m.getEntityToAttack() != null;
        }

        @Override
        public boolean shouldExecute()
        {
            return !busy() && noise(m) != null;
        }

        @Override
        public boolean continueExecuting()
        {
            return shouldExecute();
        }

        @Override
        public void startExecuting()
        {
            cooldown = 0;
        }

        @Override
        public void updateTask()
        {
            NBTTagCompound n = noise(m);
            if (n == null || --cooldown > 0 && !m.getNavigator().noPath())
            {
                return;
            }
            cooldown = 20;
            double x = n.getDouble("x"), y = n.getDouble("y"), z = n.getDouble("z");
            double dx = x - m.posX, dz = z - m.posZ, d = Math.sqrt(dx * dx + dz * dz);
            if (d > 4)
            {
                // a leg of at most 12 blocks toward the spot (path search is limited to the follow range)
                double k = Math.min(1, 12 / d);
                m.getNavigator().tryMoveToXYZ(m.posX + dx * k, y, m.posZ + dz * k, speed);
            }
            else if (m.getNavigator().noPath())
            {
                // arrived: look around the spot
                m.getNavigator().tryMoveToXYZ(x + (m.getRNG().nextDouble() - 0.5) * 12, y,
                                              z + (m.getRNG().nextDouble() - 0.5) * 12, speed * 0.7);
            }
        }

        @Override
        public void resetTask()
        {
            m.getNavigator().clearPathEntity();
        }
    }
}
