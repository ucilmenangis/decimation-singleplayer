package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.LocalStats;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Local stats (net.decimation.fixes.LocalStats, user request 11 Oktober 2026): the player kills
 * an infected and a soldier in the arena; the human and infected kill counters must each go up
 * by one, the menu / HUD profile and the totals line must carry them, the session flag must say
 * valid (green "Play", no invalid session banner). Pictures: the HUD with its stats list
 * (stats_hud) and Decimation's home menu opened in the world (stats_menu).
 */
public class StatsTest extends DevTestMode
{
    public String name() { return "stats"; }

    private volatile int ticks;
    private volatile boolean killed, done;
    private int stage, menuFrames;
    /** The counters before this test (the user's own numbers, put back at the end). */
    private long[] before;
    private String uuid;

    public boolean client(Minecraft mc)
    {
        int t = ticks;
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (killed && t >= 120 && stage == 0)       // the client sees server ticks between frames: >=
        {
            stage = 1;
            long[] s = Deci.menuState(), c = LocalStats.counts(uuid);
            DevTestResults.check(name(), "session valid (green Play)", s[0], s[0] == 1, "1");
            DevTestResults.check(name(), "menu profile human kills", s[1], s[1] == c[0], "= local count " + c[0]);
            DevTestResults.check(name(), "menu profile infected kills", s[3], s[3] == c[2], "= local count " + c[2]);
            DevTestResults.check(name(), "totals line", s[4] + " / " + s[5], s[4] == c[0] && s[5] == c[2], c[0] + " / " + c[2]);
            mc.gameSettings.hideGUI = false;
            Deci.showHudStats();
        }
        if (t >= 150 && stage == 1)
        {
            stage = 2;
            DevTestUtil.screenshot(mc, name(), "stats_hud");
            net.decimation.worldgen.DevAutoTest.keepScreen = true;
            mc.displayGuiScreen(Deci.homeMenu());
        }
        if (stage == 2 && ++menuFrames >= 60)          // the menu pauses the server: count frames
        {
            DevTestUtil.screenshot(mc, name(), "stats_menu");
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            mc.displayGuiScreen(null);
            LocalStats.restore(uuid, before);         // the test's kills out of the user's stats again
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
        if (t == 2)
        {
            DevTestArena.build(w, p);
            uuid = p.getUniqueID().toString();
            before = LocalStats.counts(uuid);
        }
        if (t == 40)
        {
            EntityLiving infected = spawn(w, Deci.newInfected(w), 4);
            EntityLiving soldier = spawn(w, Deci.newSoldier(w), 6);
            if (infected != null)
            {
                infected.attackEntityFrom(DamageSource.causePlayerDamage(p), 10000f);
            }
            if (soldier != null)
            {
                soldier.attackEntityFrom(DamageSource.causePlayerDamage(p), 10000f);
            }
        }
        if (t == 60)
        {
            long[] after = LocalStats.counts(uuid);
            DevTestResults.check(name(), "human kills +1", before[0] + " -> " + after[0], after[0] == before[0] + 1, "+1 (a soldier)");
            DevTestResults.check(name(), "infected kills +1", before[2] + " -> " + after[2], after[2] == before[2] + 1, "+1 (an infected)");
            killed = true;
        }
    }

    /** Spawned on the arena floor in front of the player (Decimation refuses some spawns: retried). */
    private static EntityLiving spawn(World w, EntityLiving e, int dz)
    {
        for (int i = 0; i < 20 && e != null; i++)
        {
            e.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + dz + 0.5, 0, 0);
            if (w.spawnEntityInWorld(e))
            {
                return e;
            }
        }
        return null;
    }
}
