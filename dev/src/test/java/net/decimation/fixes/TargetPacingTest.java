package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import org.junit.jupiter.api.Test;

/** Target searches of Decimation's mobs paced: targetChance 0 becomes 5, nothing else changes (bench "crowd"). */
class TargetPacingTest
{
    static int chance(EntityAINearestAttackableTarget t) throws Exception
    {
        Field f = EntityAINearestAttackableTarget.class.getDeclaredField("targetChance");
        f.setAccessible(true);
        return f.getInt(t);
    }

    @Test
    void searchesEveryEvaluationArePaced() throws Exception
    {
        EntityAITasks tasks = new EntityAITasks(new Profiler());
        EntityAINearestAttackableTarget every = new EntityAINearestAttackableTarget(null, EntityPlayer.class, 0, true);
        EntityAINearestAttackableTarget own = new EntityAINearestAttackableTarget(null, EntityPlayer.class, 3, true);
        EntityAIBase other = new EntityAIBase()
        {
            public boolean shouldExecute()
            {
                return false;
            }
        };
        tasks.addTask(1, every);
        tasks.addTask(2, own);
        tasks.addTask(3, other);
        assertEquals(1, TargetPacing.pace(tasks));
        assertEquals(TargetPacing.CHANCE, chance(every));
        assertEquals(3, chance(own)); // a mob's own pacing stays
        assertEquals(0, TargetPacing.pace(tasks)); // a second join changes nothing more
    }
}
