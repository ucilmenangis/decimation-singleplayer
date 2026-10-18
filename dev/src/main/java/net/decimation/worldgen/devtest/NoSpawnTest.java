package net.decimation.worldgen.devtest;

import java.io.File;
import java.lang.reflect.Method;

import net.decimation.fixes.Deci;
import net.decimation.fixes.MilitarySpawner;
import net.decimation.fixes.NaturalSpawns;
import net.decimation.worldgen.Garrisons;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;

/**
 * Natural spawns off for tests (user, 18 Oktober 2026: "remove natural spawn on mobs, so to spawn it only from code or
 * egg"): with Minecraft's game rule doMobSpawning false our own automatic spawners stop too. Checks: the enemy military
 * spawner may run with the rule on and not with it off, a garrison spawner cage's infected is refused with it off,
 * a spawn from code still works with it off. The rule is put back as it was.
 */
public class NoSpawnTest extends DevTestMode
{
    public String name() { return "nospawn"; }

    private volatile boolean done;

    public boolean client(Minecraft mc)
    {
        return !done;
    }

    @Override
    public void server()
    {
        if (done)
        {
            return;
        }
        done = true;
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        String before = w.getGameRules().getGameRuleStringValue(NaturalSpawns.RULE);
        EnumDifficulty diff = w.difficultySetting;
        w.difficultySetting = EnumDifficulty.NORMAL;
        MilitarySpawner spawner = new MilitarySpawner(new File(Minecraft.getMinecraft().mcDataDir, "config"));
        int x = DevTestArena.X, y = DevTestArena.Y, z = DevTestArena.Z;
        try
        {
            w.getGameRules().setOrCreateGameRule(NaturalSpawns.RULE, "true");
            DevTestResults.check(name(), "military spawner may run with doMobSpawning true", spawner.allowed(w), spawner.allowed(w), "true");
            w.getGameRules().setOrCreateGameRule(NaturalSpawns.RULE, "false");
            DevTestResults.check(name(), "military spawner stops with doMobSpawning false", spawner.allowed(w), !spawner.allowed(w), "false");

            Method add = Garrisons.class.getDeclaredMethod("addSpawners", int[].class); // a spawner cage at the arena
            add.setAccessible(true);
            add.invoke(null, (Object) new int[] {0, 1, x, y - 1, z}); // {state, spawners, sx, sy, sz}
            boolean cage = spawnPlain(w, x + 1.5, y, z + 0.5);
            DevTestResults.check(name(), "spawner cage infected refused with doMobSpawning false", cage, !cage, "refused");
            boolean code = spawnPlain(w, x + 30.5, y, z + 0.5);
            DevTestResults.check(name(), "spawn from code still works with doMobSpawning false", code, code, "spawned");
        }
        catch (Exception e)
        {
            DevTestResults.check(name(), "test ran", e + " " + e.getCause(), false, "no exception");
        }
        finally
        {
            w.getGameRules().setOrCreateGameRule(NaturalSpawns.RULE, before);
            w.difficultySetting = diff;
            Garrisons.forgetSpawner(x, y - 1, z);
            DevTestResults.value(name(), "done", "rule back to " + before);
        }
    }

    /** A plain infected at (x, y, z), retried 10 times (Decimation refuses some spawns: hulks, bloaters); true once one joined. */
    private static boolean spawnPlain(WorldServer w, double x, double y, double z)
    {
        for (int i = 0; i < 10; i++)
        {
            EntityLiving e = Deci.newInfected(w);
            e.setLocationAndAngles(x, y, z, 0, 0);
            boolean joined = w.spawnEntityInWorld(e);
            e.setDead();
            if (joined && Deci.isPlainInfected(e))
            {
                return true;
            }
        }
        return false;
    }
}
