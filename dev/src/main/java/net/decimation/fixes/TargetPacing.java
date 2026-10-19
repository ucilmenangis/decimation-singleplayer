package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Target searches of Decimation's mobs every tick (dev test bench, 19 Oktober 2026: ray traces from
 * EntityAINearestAttackableTarget were 8 of 14 ray trace samples of the server in the "crowd" scenario): the infected
 * carry four "nearest target" tasks (players, animals, human NPCs, mechs) and bandits one (infected), all with
 * targetChance 0, so an idle one lists every candidate in its follow range (and ray traces to players and infected)
 * at every evaluation of its target tasks (every 3rd tick), four times over for an infected. When a Decimation mob
 * joins a world on the server, those tasks get targetChance 5: a search every 15 ticks on average (0.75 s; 10, what
 * vanilla gives most mobs, made an infected take 1.5 s to notice someone). Vanilla mobs are left as they are.
 */
public final class TargetPacing
{
    static final int CHANCE = 5;
    private static Field chance;

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e)
    {
        if (e.world.isRemote || !(e.entity instanceof EntityLiving) || !e.entity.getClass().getName().startsWith("deci."))
        {
            return;
        }
        pace(((EntityLiving) e.entity).targetTasks);
    }

    /** targetChance 0 -> CHANCE on every "nearest attackable target" task of the list; returns how many changed. */
    static int pace(EntityAITasks tasks)
    {
        int n = 0;
        try
        {
            if (chance == null)
            {
                chance = ReflectionHelper.findField(EntityAINearestAttackableTarget.class, "targetChance", "field_75308_c");
            }
            for (Object o : tasks.taskEntries)
            {
                Object action = ((EntityAITasks.EntityAITaskEntry) o).action;
                if (action instanceof EntityAINearestAttackableTarget && chance.getInt(action) == 0)
                {
                    chance.setInt(action, CHANCE);
                    n++;
                }
            }
        }
        catch (Exception ex)
        {
            // a renamed field: the mob keeps searching every tick, as before
        }
        return n;
    }
}
