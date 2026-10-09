package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Guns in first person and in an NPC's hands, one after another on the test arena
 * (docs/gun_style_guide.md sections 11 and 13). Per gun: gunview_<gun>.png (hip),
 * gunview_<gun>_aim.png (aim mode forced on, Deci.setAimMode, both sides), gunview_<gun>_npc.png
 * (a bandit holding the same gun, camera at its side, GUI hidden). GUI on for the first person
 * shots: F1 would hide the gun too.
 * -Pguns=uzi,mp5a3,mac10 picks the guns (default: a spread of Decimation guns plus ours);
 * -Pattach=reddot,smgSuppressor,laser puts attachments on every gun (Decimation's NBT keys
 * sightAttach / barrelAttach / gripAttach; the game shows a sight only when it is a scope item).
 * Names are tried as deci:<name> then deciworldgen:<name>.
 */
public class GunViewTest extends DevTestMode
{
    public String name() { return "gunview"; }

    private static final String DEFAULT = "uzi,mp5a3,ump45,vector,mp7,glock17,deagle,ak74,m4a4,r870,mac10";
    private static final int START = 30, EACH = 90;
    private static final int HIP = 20, AIM_ON = 24, AIM = 42, AIM_OFF = 46, NPC_ON = 50, NPC = 80, NPC_OFF = 86;

    private final String[] guns = System.getProperty("deciworldgen.autotest.guns", DEFAULT).split(",");
    private final String[] attach = System.getProperty("deciworldgen.autotest.attach", "").isEmpty() ? new String[0]
        : System.getProperty("deciworldgen.autotest.attach").split(",");
    private volatile int ticks;
    private EntityLiving bandit, target;
    private int shot;

    private static Item find(String name)
    {
        Object o = Item.itemRegistry.getObject("deci:" + name);
        if (o == null)
        {
            o = Item.itemRegistry.getObject("deciworldgen:" + name);
        }
        return (Item) o;
    }

    /** Decimation's attachment slot of an attachment item name. */
    private static String slot(String a)
    {
        if (a.endsWith("Suppressor") || a.equals("bayonet"))
        {
            return "barrel";
        }
        if (a.equals("foregrip") || a.equals("flashlight") || a.equals("laser"))
        {
            return "grip";
        }
        return "sight";
    }

    private String spec(String gun)
    {
        StringBuilder s = new StringBuilder(gun);
        for (String a : attach)
        {
            s.append(';').append(slot(a)).append('=').append(a);
        }
        return s.toString();
    }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        // the counter is the server's: a client tick can skip values, so phases are ranges and
        // every shot is taken on the first client tick at or after its time
        int t = ticks;
        if (t >= START)
        {
            int u = (t - START) % EACH;
            boolean npc = u >= NPC_ON && u < NPC_OFF;
            mc.gameSettings.hideGUI = npc; // F1 also hides the held item: on only for the NPC shot
            mc.gameSettings.thirdPersonView = 0;
            mc.gameSettings.fovSetting = 70;
            mc.thePlayer.inventory.currentItem = 0;
            if (!npc)
            {
                mc.thePlayer.rotationYaw = 180;
                mc.thePlayer.rotationPitch = 0;
            }
            Deci.setAimMode(mc.thePlayer, u >= AIM_ON && u < AIM_OFF ? 1 : 0);
            int[] at = {HIP, AIM, NPC};
            String[] suffix = {"", "_aim", "_npc"};
            if (shot < guns.length * 3)
            {
                int gun = shot / 3, k = shot % 3;
                if (t >= START + gun * EACH + at[k])
                {
                    DevTestUtil.screenshot(mc, name(), "gunview_" + guns[gun] + (attach.length > 0 ? "_att" : "") + suffix[k]);
                    shot++;
                }
            }
        }
        if (t >= START + guns.length * EACH && shot >= guns.length * 3)
        {
            mc.gameSettings.hideGUI = false;
            Deci.setAimMode(mc.thePlayer, 0);
            return false;
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
        World w = p.worldObj;
        double x = DevTestArena.X + 0.5, y = DevTestArena.Y, z = DevTestArena.Z + 0.5;
        if (t == 10)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(6000);
            p.capabilities.isFlying = true;
        }
        if (t < START)
        {
            if (t >= 10)
            {
                p.playerNetServerHandler.setPlayerLocation(x, y, z, 180, 0);
            }
            return;
        }
        int i = (t - START) / EACH, u = (t - START) % EACH;
        if (i >= guns.length)
        {
            return;
        }
        String g = guns[i];
        if (u == 0)
        {
            Item item = find(g);
            DevTestResults.check(name(), "gun " + g, String.valueOf(item), item != null, "registered");
            ItemStack s = item == null ? null : new ItemStack(item);
            if (s != null)
            {
                s.stackTagCompound = new net.minecraft.nbt.NBTTagCompound(); // gun data ready before the first frame
                s.stackTagCompound.setInteger("ammo", 0);
                for (String a : attach)
                {
                    s.stackTagCompound.setString(slot(a) + "Attach", a);
                }
            }
            p.inventory.currentItem = 0;
            p.inventory.mainInventory[0] = s;
            p.inventoryContainer.detectAndSendChanges();
        }
        Deci.setAimMode(p, u >= AIM_ON && u < AIM_OFF ? 1 : 0);
        double bx = x, bz = z - 6;
        if (u == NPC_ON)
        {
            bandit = Deci.newBandit(w);
            NpcLoadouts.instance().equip(bandit, NpcLoadouts.instance().byName("bandit_medium"), bandit.getEntityData());
            bandit.getEntityData().setString(NpcLoadouts.GUN_TAG, spec(g)); // synced to the client
            bandit.setPosition(bx, y, bz);
            w.spawnEntityInWorld(bandit);
            target = Deci.newSoviet(w); // the bandit turns to it: its right side faces the camera
            target.setPosition(bx - 14, y, bz);
            w.spawnEntityInWorld(target);
        }
        if (u >= NPC_ON && u < NPC_OFF)
        {
            for (Object o : w.loadedEntityList.toArray())
            {
                if (o instanceof EntityLiving && o != bandit && o != target) // strays shoot the bandit
                {
                    ((EntityLiving) o).setDead();
                }
            }
            for (EntityLiving e : new EntityLiving[] {bandit, target})
            {
                if (e != null)
                {
                    e.setHealth(e.getMaxHealth());
                    e.motionX = e.motionZ = 0;
                }
            }
            if (bandit != null)
            {
                bandit.setPosition(bx, y, bz);
                target.setPosition(bx - 14, y, bz);
            }
            // camera north of the bandit, looking south (yaw 0): the bandit faces west, its right side north
            p.playerNetServerHandler.setPlayerLocation(bx + 0.3, y, bz - 2.3, 0, 6);
        }
        if (u == NPC_OFF && bandit != null)
        {
            bandit.setDead();
            target.setDead();
            bandit = target = null;
        }
        if (u < NPC_ON || u >= NPC_OFF)
        {
            p.playerNetServerHandler.setPlayerLocation(x, y, z, 180, 0);
        }
    }
}
