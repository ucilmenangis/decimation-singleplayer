package net.decimation.worldgen.devtest;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Guns in first person, one after another on the test arena (docs/gun_style_guide.md
 * "First person"): screenshot gunview_<gun>.png per gun, same camera, noon (GUI on:
 * F1 would hide the gun too).
 * Pick guns with -Pguns=uzi,mp5a3,mac10 (default: a spread of Decimation guns plus ours).
 * Names are tried as deci:<name> then deciworldgen:<name>.
 */
public class GunViewTest extends DevTestMode
{
    public String name() { return "gunview"; }

    private static final String DEFAULT = "uzi,mp5a3,ump45,vector,mp7,glock17,deagle,ak74,m4a4,r870,mac10";
    private static final int START = 30, EACH = 40, SHOT = 30;

    private final String[] guns = System.getProperty("deciworldgen.autotest.guns", DEFAULT).split(",");
    private volatile int ticks;

    private static Item find(String name)
    {
        Object o = Item.itemRegistry.getObject("deci:" + name);
        if (o == null)
        {
            o = Item.itemRegistry.getObject("deciworldgen:" + name);
        }
        return (Item) o;
    }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        int t = ticks;
        if (t >= START)
        {
            mc.gameSettings.hideGUI = false; // F1 also hides the held item
            mc.gameSettings.thirdPersonView = 0;
            mc.gameSettings.fovSetting = 70;
            mc.thePlayer.inventory.currentItem = 0;
            mc.thePlayer.rotationYaw = 180;
            mc.thePlayer.rotationPitch = 0;
            int i = (t - START) / EACH;
            if (i < guns.length && (t - START) % EACH == SHOT)
            {
                DevTestUtil.screenshot(mc, name(), "gunview_" + guns[i]);
            }
        }
        if (t >= START + guns.length * EACH)
        {
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
        if (t == 10)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(6000);
            p.capabilities.isFlying = true;
        }
        if (t >= START && (t - START) % EACH == 0 && (t - START) / EACH < guns.length)
        {
            String g = guns[(t - START) / EACH];
            Item item = find(g);
            DevTestResults.check(name(), "gun " + g, String.valueOf(item), item != null, "registered");
            ItemStack s = item == null ? null : new ItemStack(item);
            if (s != null)
            {
                s.stackTagCompound = new net.minecraft.nbt.NBTTagCompound(); // gun data ready before the first frame
                s.stackTagCompound.setInteger("ammo", 0);
            }
            p.inventory.currentItem = 0;
            p.inventory.mainInventory[0] = s;
            p.inventoryContainer.detectAndSendChanges();
        }
        if (t >= 10)
        {
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 180, 0);
        }
    }
}
