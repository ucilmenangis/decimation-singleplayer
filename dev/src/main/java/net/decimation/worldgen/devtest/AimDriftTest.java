package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.decimation.fixes.ScopeZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Aim drift (user report 11 Oktober 2026: aiming an AK-74 with the ACOG, the gun crept down
 * second by second until it left the screen). Holds aim for 15 s with -Pgun (ak74) and -Psight
 * (ta11acog) and records ScopeZoom's gun shift at 3, 8 and 15 s; the shift must not move once the
 * sight is learned and stay small (gun on screen). A sniper NPC stands in view (-Pnpc): the cause
 * was ScopeZoom taking the glass of an NPC's scoped gun for the player's. Pictures aimdrift_3s /
 * aimdrift_15s. -Psize=WxH resizes the window, -Pturn turns the view.
 */
public class AimDriftTest extends DevTestMode
{
    public String name() { return "aimdrift"; }

    private final String gun = System.getProperty("deciworldgen.autotest.gun", "ak74");
    private final String sight = System.getProperty("deciworldgen.autotest.sight", "ta11acog");
    private volatile int ticks;
    private int frames;
    private double[] at3;
    private boolean done;
    /** -Psize=WxH resizes the window first (the user's was about 1160 x 650); -Pturn=deg per tick turns the view. */
    private final String size = System.getProperty("deciworldgen.autotest.size", "");
    private final float turn = Float.parseFloat(System.getProperty("deciworldgen.autotest.turn", "0"));
    private boolean resized;
    /**
     * -Pchanges=ak74:reddot,m4a4:2x,ak74:ta11acog: after the 15 s hold, the held gun / sight
     * changes WHILE aiming to each gun:sight in turn, 6 s each; the shift must settle and stay on
     * screen after every change (user, 11 Oktober 2026: "maybe the bug appear in change").
     */
    private final String[] changes = System.getProperty("deciworldgen.autotest.changes", "").isEmpty()
        ? new String[0] : System.getProperty("deciworldgen.autotest.changes").split(",");
    private static final int SEG = 120;
    private double[] segMid;
    /**
     * -Pnpc=TIER: an NPC of that tier in view, off centre (default elite_sniper: a scoped gun; the
     * user's report had sniper NPCs in view, their glass was taken for ours); -Pnpc=none for none.
     */
    private final String npc = System.getProperty("deciworldgen.autotest.npc", "elite_sniper");
    private int lastLogged = -1;

    public boolean client(Minecraft mc)
    {
        int t = ticks;
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (!resized && size.contains("x"))
        {
            resized = true;
            try
            {
                String[] wh = size.split("x");
                int w = Integer.parseInt(wh[0]), h = Integer.parseInt(wh[1]);
                org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(w, h));
                java.lang.reflect.Method r = cpw.mods.fml.relauncher.ReflectionHelper.findMethod(
                    Minecraft.class, mc, new String[] {"resize", "func_71370_a"}, int.class, int.class);
                r.invoke(mc, w, h);
            }
            catch (Exception ex)
            {
                DevTestResults.value(name(), "resize failed", ex.toString());
            }
        }
        if (t < 40)
        {
            return true;
        }
        if (turn != 0)
        {
            mc.thePlayer.rotationYaw += turn / 3f;
            mc.thePlayer.rotationPitch = (float) (10 * Math.sin(t / 40.0));
        }
        if ((t - 40) / 20 != lastLogged)
        {
            lastLogged = (t - 40) / 20;
            DevTestResults.value(name(), "shift at " + lastLogged + " s (" + mc.displayWidth + "x" + mc.displayHeight + ")",
                                 fmt(ScopeZoom.debugShift()));
        }
        mc.gameSettings.hideGUI = false;
        Deci.setAimMode(mc.thePlayer, 1);
        frames++;
        if (t >= 40 + 60 && at3 == null)
        {
            at3 = ScopeZoom.debugShift();
            DevTestResults.value(name(), "shift at 3 s", fmt(at3));
            DevTestUtil.screenshot(mc, name(), "aimdrift_3s");
        }
        if (t >= 40 + 160 && t < 40 + 162)
        {
            DevTestResults.value(name(), "shift at 8 s", fmt(ScopeZoom.debugShift()));
        }
        if (t >= 40 + 300 && !done)
        {
            double[] at15 = ScopeZoom.debugShift();
            double dy = at3 == null || at15 == null ? 99 : Math.abs(at15[1] - at3[1]);
            double dx = at3 == null || at15 == null ? 99 : Math.abs(at15[0] - at3[0]);
            DevTestResults.check(name(), "shift change 3 s -> 15 s", String.format("%.4f / %.4f", dx, dy),
                                 dx < 0.01 && dy < 0.01, "both < 0.01 (no creep)");
            DevTestResults.check(name(), "shift at 15 s", fmt(at15),
                                 at15 != null && Math.abs(at15[0]) < 0.5 && Math.abs(at15[1]) < 0.5,
                                 "both within 0.5 (gun on screen; an NPC's glass gave -3.9)");
            DevTestUtil.screenshot(mc, name(), "aimdrift_15s");
            done = true;
            if (changes.length == 0)
            {
                Deci.setAimMode(mc.thePlayer, 0);
                return false;
            }
        }
        int seg = t < 40 + 300 + 2 ? -1 : (t - 342) / SEG, at = (t - 342) % SEG;
        if (seg >= changes.length)
        {
            Deci.setAimMode(mc.thePlayer, 0);
            return false;
        }
        if (seg >= 0 && at == 60)
        {
            segMid = ScopeZoom.debugShift();
        }
        if (seg >= 0 && at == SEG - 4)
        {
            double[] end = ScopeZoom.debugShift();
            double d = segMid == null || end == null ? 99
                : Math.max(Math.abs(end[0] - segMid[0]), Math.abs(end[1] - segMid[1]));
            DevTestResults.check(name(), "after change to " + changes[seg] + ": shift 3 s / 6 s",
                                 fmt(segMid) + " / " + fmt(end),
                                 d < 0.01 && end != null && Math.abs(end[0]) < 0.5 && Math.abs(end[1]) < 0.5,
                                 "steady (< 0.01) and within 0.5");
            DevTestUtil.screenshot(mc, name(), "aimdrift_change" + seg);
        }
        return true;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        if (t == 2)
        {
            // the player is not hurt for the run: the sniper in view shot the test player dead mid run (21 Oktober 2026,
            // "Developer was shot by a survivor"), the gun went with the death and the shift fell to 0 (old code too)
            unhurt = p.capabilities.disableDamage;
            p.capabilities.disableDamage = true;
            p.sendPlayerAbilities();
            p.setHealth(p.getMaxHealth());
            DevTestArena.build(p.worldObj, p);
            p.worldObj.setWorldTime(6000);
            ItemStack g = new ItemStack(GameRegistry.findItem("deci", gun));
            g.stackTagCompound = new NBTTagCompound();
            g.stackTagCompound.setInteger("ammo", 30);
            g.stackTagCompound.setString("sightAttach", sight);
            p.inventory.setInventorySlotContents(p.inventory.currentItem, g);
        }
        if (t == 4)
        {
            p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
            p.rotationYaw = 0;
            p.rotationPitch = 0;
            p.playerNetServerHandler.setPlayerLocation(p.posX, p.posY, p.posZ, 0, 0);
        }
        if (t == 30 && !npc.isEmpty() && !npc.equals("none"))
        {
            net.decimation.fixes.NpcLoadouts l = net.decimation.fixes.NpcLoadouts.instance();
            net.decimation.fixes.NpcLoadouts.Tier tier = l.byName(npc);
            for (int a = 0; tier != null && a < 20; a++)
            {
                net.minecraft.entity.EntityLiving e = tier.kind == net.decimation.fixes.NpcKind.BANDIT
                    ? Deci.newBandit(p.worldObj) : tier.kind == net.decimation.fixes.NpcKind.SOLDIER
                    ? Deci.newSoldier(p.worldObj) : Deci.newSoviet(p.worldObj);
                l.equip(e, tier, e.getEntityData());
                e.setLocationAndAngles(DevTestArena.X - 4.5, DevTestArena.Y, DevTestArena.Z + 10.5, 90, 0);
                if (p.worldObj.spawnEntityInWorld(e))
                {
                    DevTestResults.value(name(), "npc in view", npc);
                    break;
                }
            }
        }
        if (t >= 342 && (t - 342) % SEG == 0 && (t - 342) / SEG < changes.length)
        {
            String[] gs = changes[(t - 342) / SEG].split(":");
            ItemStack g = new ItemStack(GameRegistry.findItem("deci", gs[0]));
            g.stackTagCompound = new NBTTagCompound();
            g.stackTagCompound.setInteger("ammo", 30);
            if (gs.length > 1 && !gs[1].equals("none"))
            {
                g.stackTagCompound.setString("sightAttach", gs[1]);
            }
            p.inventory.setInventorySlotContents(p.inventory.currentItem, g);
        }
        if (t >= 40)
        {
            Deci.setAimMode(p, 1);
        }
        if (t == (changes.length == 0 ? 40 + 300 : 342 + changes.length * SEG))
        {
            p.capabilities.disableDamage = unhurt;   // as it was, before the client ends the mode in this tick
            p.sendPlayerAbilities();
        }
    }

    private boolean unhurt;

    private static String fmt(double[] s)
    {
        return s == null ? "none" : String.format("%.4f, %.4f", s[0], s[1]);
    }
}
