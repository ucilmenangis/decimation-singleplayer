package net.decimation.worldgen.devtest;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Gun performance (user report 10 Oktober 2026: 1 fps after dropping a gun; skill
 * decimation-gun "performance"): on the test arena, per gun, the fps with nothing in view, with
 * the gun dropped 2.5 blocks in front of the camera (EntityItem, IItemRenderer ENTITY), and held
 * (first person). Decimation's guns are the control. Uncapped frame rate (DevTestUtil.unlimitedFps).
 * -Pguns=uzi,ump45,mac10,ump9 (default), -Pattach=reddot,smgSuppressor puts attachments on them.
 * Results: "fps <gun> dropped" / "held" / "baseline", gunperf_<gun>_dropped.png.
 */
public class GunPerfTest extends DevTestMode
{
    public String name() { return "gunperf"; }

    private static final int START = 40, PHASE = 140, SETTLE = 50;
    private static final String[] KINDS = {"baseline", "dropped", "held"};

    private final String[] guns = System.getProperty("deciworldgen.autotest.guns", "uzi,ump45,mac10,ump9").split(",");
    private final String[] attach = System.getProperty("deciworldgen.autotest.attach", "").isEmpty() ? new String[0]
        : System.getProperty("deciworldgen.autotest.attach").split(",");
    private volatile int ticks;
    private final long[] sum = new long[64];
    private final int[] samples = new int[64];
    private int lastSample = -1, shot = -1;
    private EntityItem dropped;

    private static Item find(String name)
    {
        Object o = Item.itemRegistry.getObject("deci:" + name);
        return (Item) (o != null ? o : Item.itemRegistry.getObject("deciworldgen:" + name));
    }

    private ItemStack stack(String gun)
    {
        Item item = find(gun);
        if (item == null)
        {
            return null;
        }
        ItemStack s = new ItemStack(item);
        s.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
        s.stackTagCompound.setInteger("ammo", 0);
        for (String a : attach)
        {
            String slot = a.endsWith("Suppressor") || a.equals("bayonet") ? "barrel"
                : a.equals("foregrip") || a.equals("flashlight") || a.equals("laser") ? "grip" : "sight";
            s.stackTagCompound.setString(slot + "Attach", a);
        }
        return s;
    }

    /** Phase index: 0 = baseline, then per gun 2 phases (dropped, held). */
    private int phases()
    {
        return 1 + guns.length * 2;
    }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        int t = ticks;
        DevTestUtil.unlimitedFps(mc);
        mc.gameSettings.hideGUI = false;
        mc.gameSettings.thirdPersonView = 0;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 30;
        if (t < START)
        {
            return true;
        }
        int ph = (t - START) / PHASE, u = (t - START) % PHASE;
        if (ph >= phases())
        {
            for (int i = 0; i < phases(); i++)
            {
                String label = i == 0 ? "baseline" : guns[(i - 1) / 2] + " " + KINDS[1 + (i - 1) % 2];
                DevTestResults.value(name(), "fps " + label, Math.round(sum[i] / (double) Math.max(1, samples[i])));
            }
            return false;
        }
        if (u >= SETTLE && u / 10 != lastSample)
        {
            lastSample = u / 10;
            String dbg = mc.debug;
            sum[ph] += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
            samples[ph]++;
        }
        if (u == PHASE - 10 && shot != ph && ph > 0)
        {
            shot = ph;
            DevTestUtil.screenshot(mc, name(), "gunperf_" + guns[(ph - 1) / 2] + "_" + KINDS[1 + (ph - 1) % 2]);
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
        if (t >= 10)
        {
            p.playerNetServerHandler.setPlayerLocation(x, y, z, 0, 30);
        }
        if (t < START)
        {
            return;
        }
        int ph = (t - START) / PHASE, u = (t - START) % PHASE;
        if (u != 0 || ph >= phases())
        {
            if (dropped != null)
            {
                dropped.motionX = dropped.motionZ = 0;
                dropped.delayBeforeCanPickup = 32767;
                dropped.age = 0;
            }
            return;
        }
        // a new phase: clear the last one
        if (dropped != null)
        {
            dropped.setDead();
            dropped = null;
        }
        p.inventory.mainInventory[0] = null;
        p.inventory.currentItem = 0;
        if (ph > 0)
        {
            String gun = guns[(ph - 1) / 2];
            ItemStack s = stack(gun);
            DevTestResults.check(name(), "gun " + gun, String.valueOf(s), s != null, "registered");
            if (s != null && (ph - 1) % 2 == 0)
            {
                dropped = new EntityItem(w, x, y + 0.1, z + 2.5, s);
                dropped.motionX = dropped.motionY = dropped.motionZ = 0;
                dropped.delayBeforeCanPickup = 32767;
                w.spawnEntityInWorld(dropped);
            }
            else if (s != null)
            {
                p.inventory.mainInventory[0] = s;
            }
        }
        p.inventoryContainer.detectAndSendChanges();
    }
}
