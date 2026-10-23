package net.decimation.worldgen.devtest;

import net.decimation.fixes.GunDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Gun draws (fixes/GunDraw), one gun of each way of drawing in hotbar slots 0 .. 6 (pistol, revolver, SMG, rifle,
 * shotgun, machine gun, rocket launcher), slot 8 empty. Per gun: from the empty hand onto the gun, the draw must start,
 * and at its end the gun's own rack must play (all but the rocket launcher). Then shots at fixed moments of its draw
 * (GunDraw.testTime): gundraw_<gun>_<n>.
 */
public class GunDrawTest extends DevTestMode
{
    public String name() { return "gundraw"; }

    private static final String[] GUNS = {"glock17", "ragingbull", "uzi", "ak74", "r870", "pkm", "rpg7"};
    /** The shots, as parts of each gun's draw. */
    private static final float[] AT = {0f, 0.15f, 0.3f, 0.45f, 0.6f, 0.8f};
    private volatile int ticks;
    private int gun, step, wait, racks0;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ticks < 20)
        {
            return true;
        }
        mc.gameSettings.hideGUI = false;
        mc.gameSettings.fovSetting = 70f;
        mc.gameSettings.thirdPersonView = 0;
        look(mc, 90, 8);
        wait++;
        String n = GUNS[gun];
        Item item = find(n);
        if (step == 0)                                     // the empty hand first
        {
            GunDraw.testTime = -1;
            mc.thePlayer.inventory.currentItem = 8;
            if (wait > 8)
            {
                next();
            }
        }
        else if (step == 1)                                // onto the gun: its draw, then its rack
        {
            mc.thePlayer.inventory.currentItem = gun;
            if (wait == 1)
            {
                racks0 = GunDraw.racks;
            }
            if (wait == 3)
            {
                DevTestResults.check(name(), n + ": switching to it starts the draw", GunDraw.playing(), GunDraw.playing(), "true");
            }
            if (wait > 4 && !GunDraw.playing() || wait > 60)
            {
                boolean want = GunDraw.racksAtEnd(item);
                DevTestResults.check(name(), n + ": its rack at the end of the draw", GunDraw.racks - racks0,
                                     (GunDraw.racks - racks0 == 1) == want, want ? "1" : "0");
                next();
            }
        }
        else if (step < 2 + AT.length)                     // fixed moments of the draw
        {
            mc.thePlayer.inventory.currentItem = gun;
            GunDraw.testTime = AT[step - 2] * GunDraw.length(item);
            if (wait > 4)
            {
                DevTestUtil.screenshot(mc, name(), "gundraw_" + n + "_" + (step - 2));
                next();
            }
        }
        else
        {
            GunDraw.testTime = -1;
            gun++;
            step = 0;
            wait = 0;
            done = gun == GUNS.length;
        }
        return !done;
    }

    private void next()
    {
        step++;
        wait = 0;
    }

    private static Item find(String name)
    {
        Object o = Item.itemRegistry.getObject("deci:" + name);
        return (Item) (o != null ? o : Item.itemRegistry.getObject("deciworldgen:" + name));
    }

    private static void look(Minecraft mc, float yaw, float pitch)
    {
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        if (t == 10)
        {
            p.worldObj.setWorldTime(6000);
            DevTestArena.build(p.worldObj, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = true;
            for (int i = 0; i < 9; i++)
            {
                p.inventory.mainInventory[i] = i < GUNS.length ? new ItemStack(find(GUNS[i])) : null;
            }
            p.inventory.currentItem = 8;
        }
        if (t > 10)
        {
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5,
                                                       p.rotationYaw, p.rotationPitch);
        }
    }
}
