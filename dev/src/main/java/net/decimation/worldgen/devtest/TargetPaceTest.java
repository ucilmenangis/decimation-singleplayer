package net.decimation.worldgen.devtest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * Target searches of Decimation's mobs paced (fixes/TargetPacing, bench "crowd"): an infected and a bandit spawned on
 * the arena carry their "nearest target" tasks with targetChance 5, not 0; and an infected 6 blocks from a civilian NPC at night
 * still takes it as its target within 4 s (a search every 15 ticks on average).
 */
public class TargetPaceTest extends DevTestMode
{
    public String name() { return "targetpace"; }

    private volatile boolean done;
    private int ticks;
    private EntityLiving infected, bandit, civilian;

    public boolean client(Minecraft mc)
    {
        return !done;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || done)
        {
            return;
        }
        ticks++;
        if (ticks == 1)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(18000);
            p.setGameType(WorldSettings.GameType.CREATIVE); // vanilla never targets a player who takes no damage
            p.sendPlayerAbilities();
            for (int i = 0; i < 5 && civilian == null; i++)
            {
                Entity e = EntityList.createEntityByName("Human", w); // a civilian NPC: infected hunt them
                e.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 12.5, 0, 0);
                civilian = e instanceof EntityLiving && w.spawnEntityInWorld(e) ? (EntityLiving) e : null;
            }
            for (int i = 0; i < 5 && infected == null; i++)
            {
                EntityLiving e = Deci.newInfected(w);
                e.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 6.5, 180, 0);
                infected = w.spawnEntityInWorld(e) ? e : null;
            }
            for (int i = 0; i < 5 && bandit == null; i++)
            {
                Entity e = EntityList.createEntityByName("Bandit", w);
                e.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 20.5, 0, 0);
                bandit = e instanceof EntityLiving && w.spawnEntityInWorld(e) ? (EntityLiving) e : null;
            }
            return;
        }
        if (ticks == 3)
        {
            DevTestResults.check(name(), "infected target searches paced", chances(infected), all10(infected), "every one 5");
            DevTestResults.check(name(), "bandit target searches paced", chances(bandit), all10(bandit), "every one 5");
            return;
        }
        if (ticks > 3 && (infected == null || infected.getAttackTarget() == civilian || ticks >= 83))
        {
            boolean ok = infected != null && civilian != null && infected.getAttackTarget() == civilian;
            DevTestResults.check(name(), "an infected 6 blocks from a civilian takes it as target", ok ? "after " + (ticks - 3) + " ticks"
                                 : "not in 4 s", ok, "within 4 s");
            for (EntityLiving e : new EntityLiving[] {infected, bandit, civilian})
            {
                if (e != null)
                {
                    e.setDead();
                }
            }
            done = true;
        }
    }

    private static List<Integer> list(EntityLiving e)
    {
        List<Integer> out = new ArrayList<Integer>();
        if (e == null)
        {
            return out;
        }
        try
        {
            Field f = cpw.mods.fml.relauncher.ReflectionHelper.findField(EntityAINearestAttackableTarget.class, "targetChance", "field_75308_c");
            for (Object o : e.targetTasks.taskEntries)
            {
                Object a = ((EntityAITasks.EntityAITaskEntry) o).action;
                if (a instanceof EntityAINearestAttackableTarget)
                {
                    out.add(f.getInt(a));
                }
            }
        }
        catch (Exception ex)
        {
            out.add(-1);
        }
        return out;
    }

    private static String chances(EntityLiving e)
    {
        return e == null ? "not spawned" : list(e).toString();
    }

    private static boolean all10(EntityLiving e)
    {
        List<Integer> l = list(e);
        if (l.isEmpty())
        {
            return false;
        }
        for (int c : l)
        {
            if (c != 5)
            {
                return false;
            }
        }
        return true;
    }
}
