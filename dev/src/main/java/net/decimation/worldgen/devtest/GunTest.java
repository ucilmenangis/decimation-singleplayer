package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.NewGuns;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * A gun of our own on the test arena (-Ptest=gun, fixes/NewGuns, the MAC-10):
 * first person still (gun_fp), the reload animation (Decimation's reload key
 * pressed: gun_reload_<tick>), firing with the attack key held (gun_fire_<tick>),
 * Decimation's Uzi for comparison (gun_fp_uzi), then a bandit holding it from
 * the side and the front and firing it at a Soviet (gun_npc_*).
 */
public class GunTest extends DevTestMode
{
    public String name() { return "gun"; }

    private static final int RELOAD_KEY = 19; // Decimation's key.decimation.gui.reload (R)
    private static final int NPC = 260;       // the NPC part starts here
    private static final int[] RELOAD_SHOTS = {58, 64, 72, 86, 98};
    private static final int[] FIRE_SHOTS = {112, 114, 118};

    private volatile int ticks;
    private volatile boolean done;
    private volatile double camX, camY, camZ;
    private volatile float camYaw, camPitch = 5;
    private volatile String shot;
    private EntityLiving bandit, target;
    private int ammoAfterReload;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        int t = ticks;
        int attack = mc.gameSettings.keyBindAttack.getKeyCode();
        if (t >= 30 && t < 250)
        {
            mc.gameSettings.hideGUI = false;
            mc.gameSettings.thirdPersonView = 0;
            mc.thePlayer.inventory.currentItem = 0;
            mc.thePlayer.rotationYaw = 180;
            mc.thePlayer.rotationPitch = 0;
            if (t == 43)
            {
                DevTestUtil.screenshot(mc, name(), "gun_fp");
            }
            if (t == 50) // 20 ticks after the gun was handed over (server, synced)
            {
                KeyBinding.setKeyBindState(RELOAD_KEY, true);
                KeyBinding.onTick(RELOAD_KEY); // isPressed() on Decimation's next client tick
            }
            if (t == 52)
            {
                KeyBinding.setKeyBindState(RELOAD_KEY, false);
            }
            for (int s : RELOAD_SHOTS)
            {
                if (t == s)
                {
                    DevTestUtil.screenshot(mc, name(), "gun_reload_" + (t - 50));
                }
            }
            if (t >= 108 && t < 125)
            {
                KeyBinding.setKeyBindState(attack, true);
            }
            if (t == 125)
            {
                KeyBinding.setKeyBindState(attack, false);
            }
            for (int s : FIRE_SHOTS)
            {
                if (t == s)
                {
                    DevTestUtil.screenshot(mc, name(), "gun_fire_" + (t - 108));
                }
            }
            if (t == 147)
            {
                DevTestUtil.screenshot(mc, name(), "gun_fp_uzi");
            }
            if (t == 150)
            {
                KeyBinding.setKeyBindState(RELOAD_KEY, true); // control: Decimation's own Uzi reloaded the same way
                KeyBinding.onTick(RELOAD_KEY);
            }
            if (t == 152)
            {
                KeyBinding.setKeyBindState(RELOAD_KEY, false);
            }
        }
        if (t >= NPC && shot != null)
        {
            mc.gameSettings.hideGUI = true;
            mc.thePlayer.rotationYaw = camYaw;
            mc.thePlayer.rotationPitch = camPitch;
            int u = t - NPC;
            String s = shot;
            if ((u == 35 && s.equals("side")) || (u == 75 && s.equals("front")) || (u >= 110 && u <= 130 && u % 10 == 0
                && s.equals("fire")))
            {
                DevTestUtil.screenshot(mc, name(), "gun_npc_" + s + (s.equals("fire") ? "_" + u : ""));
            }
        }
        if (t > NPC + 140)
        {
            mc.gameSettings.hideGUI = false;
            done = true;
        }
        return !done;
    }

    /** A full magazine: Decimation keeps a mag's rounds in its NBT "ammo" (none = empty). */
    private static ItemStack fullMag()
    {
        ItemStack m = new ItemStack(NewGuns.mac10Mag);
        m.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
        m.stackTagCompound.setInteger("ammo", 30);
        return m;
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
        double x = DevTestArena.X + 0.5, y = DevTestArena.Y, z = DevTestArena.Z - 6 + 0.5;
        if (t == 20)
        {
            DevTestArena.build(w, p);
            w.difficultySetting = EnumDifficulty.NORMAL;
            w.setWorldTime(6000);
            p.capabilities.isFlying = true;
            DevTestResults.check(name(), "MAC-10 registered", NewGuns.mac10 + " / " + NewGuns.mac10Mag,
                                 NewGuns.mac10 != null && NewGuns.mac10Mag != null && Deci.gunTakes(NewGuns.mac10,
                                 NewGuns.mac10Mag), "gun and magazine, the gun takes it");
        }
        // the items on the SERVER (reload and firing are checked there), synced to the client
        if (t == 30 || t == 135)
        {
            p.inventory.currentItem = 0;
            ItemStack gun = new ItemStack(t == 30 ? NewGuns.mac10 : (Item) Item.itemRegistry.getObject("deci:uzi"));
            gun.stackTagCompound = new net.minecraft.nbt.NBTTagCompound(); // set up already: a fresh gun's data
            gun.stackTagCompound.setInteger("ammo", 0);                    // appears a tick later, a reload before that is lost
            p.inventory.mainInventory[0] = gun;
            if (t == 30)
            {
                p.inventory.mainInventory[1] = fullMag();
                p.inventory.mainInventory[2] = fullMag();
            }
            else
            {
                ItemStack um = new ItemStack((Item) Item.itemRegistry.getObject("deci:uziMag"));
                um.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
                um.stackTagCompound.setInteger("ammo", 32);
                p.inventory.mainInventory[3] = um;
            }
            p.inventoryContainer.detectAndSendChanges();
        }
        if (t == 225)
        {
            ItemStack g = p.inventory.mainInventory[0];
            DevTestResults.value(name(), "control: Uzi rounds after the same reload",
                                 g == null || g.stackTagCompound == null ? "-1" : String.valueOf(g.stackTagCompound.getInteger("ammo")));
        }
        if (t == 105 || t == 132)
        {
            // rounds in the gun (Decimation keeps them in the gun's NBT "ammo" too) after the reload / firing
            ItemStack g = p.inventory.mainInventory[0];
            int ammo = g == null || g.stackTagCompound == null ? -1 : g.stackTagCompound.getInteger("ammo");
            if (t == 105)
            {
                ammoAfterReload = ammo;
                DevTestResults.check(name(), "MAC-10 loaded by the reload", String.valueOf(ammo), ammo == 30, "30");
            }
            else
            {
                // Decimation reads the fire button from the mouse itself (GunItem: Mouse.isButtonDown(0)),
                // so a test cannot pull the trigger: player firing stays a manual check
                DevTestResults.value(name(), "MAC-10 rounds after the (simulated) trigger, not testable",
                                     ammoAfterReload + " -> " + ammo);
            }
        }
        int u = t - NPC;
        if (u == 0)
        {
            bandit = Deci.newBandit(w);
            NpcLoadouts.instance().equip(bandit, NpcLoadouts.instance().byName("bandit_medium"), bandit.getEntityData());
            bandit.getEntityData().setString(NpcLoadouts.GUN_TAG, "mac10"); // synced to the client
            bandit.setPosition(x, y, z);
            w.spawnEntityInWorld(bandit);
            target = Deci.newSoviet(w);
            target.setPosition(x - 12, y, z);
            shot = "side"; // camera south of the bandit, looking north: the gun from its side
            camX = x; camY = y; camZ = z + 3.6; camYaw = 180; camPitch = 4;
        }
        if (bandit != null && u > 0)
        {
            bandit.setPosition(x, y, z);
            bandit.motionX = bandit.motionY = bandit.motionZ = 0;
            bandit.rotationYaw = bandit.rotationYawHead = bandit.renderYawOffset = u < 40 ? 90 : u < 80 ? 0 : 90;
        }
        if (u == 40)
        {
            shot = "front"; // the bandit faces south (yaw 0) toward the camera
            camX = x; camY = y; camZ = z + 3.2; camYaw = 180; camPitch = 4;
        }
        if (u == 80)
        {
            shot = "fire"; // behind the bandit's right shoulder, looking west along the shots
            camX = x + 2.5; camY = y + 0.3; camZ = z + 1.6; camYaw = 110; camPitch = 8;
            w.spawnEntityInWorld(target);
        }
        if (u >= 0)
        {
            p.playerNetServerHandler.setPlayerLocation(camX, camY, camZ, camYaw, camPitch);
        }
        if (u > 90 && u < 135 && target != null)
        {
            target.setPosition(x - 12, y, z);
            target.setHealth(target.getMaxHealth());
            Deci.banditShootAt(bandit, target);
        }
        if (u == 138)
        {
            bandit.setDead();
            target.setDead();
        }
    }
}
