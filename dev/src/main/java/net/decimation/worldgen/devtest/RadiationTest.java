package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.RadiationDose;
import net.decimation.fixes.RadiationHud;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.ZoneStore;
import net.decimation.mod.server.zones.ObjectZone;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

/**
 * Radiation dose (user, 22 Oktober 2026: "radiation status below player status"; chosen: a dose in percent, always on
 * the HUD, Decimation's zone damage kept and the dose fast, a high dose hurts after leaving the zone until it falls back).
 * A radiation zone on the arena (ZoneStore, as the zones test); survival, natural regeneration off for the damage counts,
 * mob spawning off and every other mob near the arena removed each second (in the production client's world a survivor
 * shot the test player, a "Bandit" by humanity, in the middle of the mask check, 22 Oktober 2026).
 * Checks: 10 s in the zone without a mask is about half a dose; a gas mask (dose set to 30 % first, under the damage)
 * and creative stop the rise (the dose falls 1 a tick); out of the zone 90 % hurts every 4 s, 60 % every 8 s, 40 % not at all; the HUD's percent equals the server's;
 * the cost of our server tick and of the HUD draw. Shots: the HUD at about half a dose inside the zone, and with two
 * more stats lines (bleeding, infected), so the icon must move down with Decimation's column.
 */
public class RadiationTest extends DevTestMode
{
    public String name() { return "radiation"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z, RX = X - 40;
    private volatile int ticks;
    private volatile boolean done;
    private volatile int serverPct = -1;
    private volatile String shot;
    private volatile boolean shotTaken;
    private ObjectZone rad;
    private boolean regenBefore = true;
    private WorldSettings.GameType modeBefore;
    private String spawningBefore = "true";
    private int mark, waited;
    private float healthMark;

    public boolean client(Minecraft mc)
    {
        String s = shot;
        if (s != null && !shotTaken)
        {
            shotTaken = true;
            DevTestUtil.screenshot(mc, name(), s);
            DevTestResults.check(name(), "icon whole on screen (" + s + ")", RadiationHud.lastPlace(), RadiationHud.lastOnScreen(),
                "inside the screen");
            double off = RadiationHud.lastTextOffset();      // user, 23 Oktober 2026: "the text is not centered on radiaton hud"
            DevTestResults.check(name(), "percent centred on the icon (" + s + ")", String.format("%+.1f half pixels", off),
                Math.abs(off) <= 1, "within 1 half pixel");
        }
        int sp = serverPct;
        if (sp >= 0 && ++waited >= 10)                  // the server's packet takes a tick or two
        {
            serverPct = -1;
            waited = 0;
            int shown = RadiationDose.shown();
            DevTestResults.check(name(), "HUD percent equals the server's", shown + " on the HUD, " + sp + " on the server",
                Math.abs(shown - sp) <= 1, "within 1");
        }
        if (done)
        {
            DevTestResults.value(name(), "HUD draw", String.format("%.1f us a frame over %d frames", RadiationHud.averageMicros(),
                RadiationHud.framesDrawn()));
            DevTestResults.check(name(), "HUD draw cost", String.format("%.1f us", RadiationHud.averageMicros()),
                RadiationHud.framesDrawn() > 100 && RadiationHud.averageMicros() < 200, "under 200 us a frame, over 100 frames");
            DevTestResults.check(name(), "server tick cost", String.format("%.2f us", RadiationDose.averageTickMicros()),
                RadiationDose.averageTickMicros() < 50, "under 50 us a tick");
            return false;
        }
        return true;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null || done)
        {
            return;
        }
        World w = p.worldObj;
        Deci.Player d = Deci.player(p);
        if (t == 20)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(6000);
            modeBefore = p.theItemInWorldManager.getGameType();
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = false;
            regenBefore = w.getGameRules().getGameRuleBooleanValue("naturalRegeneration");
            w.getGameRules().setOrCreateGameRule("naturalRegeneration", "false");
            spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");   // no NPC may hurt the player meanwhile
            Deci.setPlayerMask(p, null);
            d.setSafezoneGrace(0);
            d.setWater(20);
            d.setBleeding(false);
            d.setInfected(false);
            p.setHealth(p.getMaxHealth());
            RadiationDose.setDose(p, 0);
            rad = ZoneStore.add(ZoneKind.RADIATION, RX - 6, Y - 3, Z - 6, RX + 6, Y + 8, Z + 6);
            p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
        }
        if (t == 30)
        {
            p.setPositionAndUpdate(RX + 0.5, Y, Z + 0.5);
        }
        if (t % 20 == 0)
        {
            sweep(w);
        }
        if (t == 230)
        {
            int pct = RadiationDose.doseOf(p) * 100 / RadiationDose.max();
            DevTestResults.check(name(), "10 s in the zone without a mask", pct + " %, irradiated " + d.irradiated(),
                pct >= 45 && pct <= 55 && d.irradiated(), "about 50 % (45 to 55), irradiated");
            DevTestResults.check(name(), "Decimation's zone damage kept", p.getHealth() + " of " + p.getMaxHealth(),
                p.getHealth() < p.getMaxHealth(), "below max");
            shot = "inside_half";
        }
        if (t == 235)
        {
            p.setHealth(p.getMaxHealth());
            Deci.setPlayerMask(p, new ItemStack(Deci.gasMask()));
            RadiationDose.setDose(p, RadiationDose.max() * 30 / 100);   // under 50 %: no damage of ours meanwhile
            mark = RadiationDose.doseOf(p);
        }
        if (t == 240)
        {
            serverPct = RadiationDose.doseOf(p) * 100 / RadiationDose.max();   // steady now: falls 1 a tick
        }
        if (t == 335)
        {
            int now = RadiationDose.doseOf(p);
            DevTestResults.check(name(), "a gas mask stops the rise", (now - mark) + " in 100 ticks, health " + p.getHealth(),
                now == mark - 100 && p.getHealth() == p.getMaxHealth(), "-100 (falls), no damage");
            Deci.setPlayerMask(p, null);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            mark = now;
        }
        if (t == 375)
        {
            int now = RadiationDose.doseOf(p);
            DevTestResults.check(name(), "creative stops the rise", (now - mark) + " in 40 ticks", now == mark - 40, "-40");
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = false;
            p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
        }
        if (t == 380)
        {
            hurtStart(p, 90);
        }
        if (t == 580)
        {
            hurtCheck(p, 90, 2, 3);
            hurtStart(p, 60);
        }
        if (t == 780)
        {
            hurtCheck(p, 60, 1, 2);
            hurtStart(p, 40);
            mark = RadiationDose.doseOf(p);
        }
        if (t == 980)
        {
            hurtCheck(p, 40, 0, 0);
            int now = RadiationDose.doseOf(p);
            DevTestResults.check(name(), "outside it falls 1 a tick", (now - mark) + " in 200 ticks", now == mark - 200, "-200");
            RadiationDose.setDose(p, RadiationDose.max() * 35 / 100);
            d.setBleeding(true);
            d.setInfected(true);
            serverPct = 35;
        }
        if (t == 990)
        {
            shot = "two_more_lines";
            shotTaken = false;
        }
        if (t == 1000)
        {
            d.setBleeding(false);
            d.setInfected(false);
            RadiationDose.setDose(p, 0);
            p.setHealth(p.getMaxHealth());
            ZoneStore.remove(rad);
            w.getGameRules().setOrCreateGameRule("naturalRegeneration", String.valueOf(regenBefore));
            if (modeBefore != null)
            {
                p.setGameType(modeBefore);                    // as the world had it (a survival player was later killed)
            }
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        }
        if (t == 1010)
        {
            done = true;
        }
    }

    /** Removes every mob within 120 blocks of the arena (the player is no EntityLiving subclass of that kind). */
    private static void sweep(World w)
    {
        for (Object o : w.loadedEntityList)
        {
            net.minecraft.entity.Entity e = (net.minecraft.entity.Entity) o;
            if (!e.isDead && e instanceof net.minecraft.entity.EntityLiving
                && Math.abs(e.posX - X) < 120 && Math.abs(e.posZ - Z) < 120)
            {
                e.setDead();
            }
        }
    }

    private void hurtStart(EntityPlayerMP p, int pct)
    {
        RadiationDose.setDose(p, RadiationDose.max() * pct / 100);
        p.setHealth(p.getMaxHealth());
        healthMark = p.getHealth();
    }

    private void hurtCheck(EntityPlayerMP p, int pct, int least, int most)
    {
        float lost = healthMark - p.getHealth();
        DevTestResults.check(name(), "out of the zone at " + pct + " %: damage in 10 s", lost + " (irradiated "
            + Deci.player(p).irradiated() + ")", lost >= least && lost <= most && !Deci.player(p).irradiated(),
            least == most ? least + ", not irradiated" : least + " to " + most + ", not irradiated");
    }
}
