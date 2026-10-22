package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"; then "make it same
 * like load ammo process code ... its reverse"): one round out a sneaking right click, like Decimation's loading puts one
 * in a plain right click. The player holds a magazine and has 5 loose rounds of its calibre; sneak held, right clicks as
 * the mouse makes them (DevTestUtil.rightClick), 4 ticks apart like a held button. Cases: one click (one round out), five
 * clicks (five out), an empty magazine (nothing, and no round loaded), a full inventory (the round on the ground), a
 * plain click (Decimation still loads one).
 */
public class MagUnloadTest extends DevTestMode
{
    public String name() { return "magunload"; }

    /** Per case: rounds in the magazine at the start, clicks, sneaking, inventory full. */
    private static final int[][] CASES = {{20, 1, 1, 0}, {20, 5, 1, 0}, {0, 1, 1, 0}, {20, 1, 1, 1}, {20, 1, 0, 0}};
    private int phase, ticks;
    private volatile Runnable onServer;
    private Item mag, round;

    public boolean client(Minecraft mc)
    {
        ticks++;
        KeyBinding sneak = mc.gameSettings.keyBindSneak;
        if (phase >= CASES.length)
        {
            onServer = () -> DevTestUtil.player().inventory.clearInventory(null, -1);
            return false;
        }
        final int[] c = CASES[phase];
        final int ph = phase;
        if (ticks == 1)
        {
            onServer = () -> setup(ph, c);
            mc.thePlayer.inventory.currentItem = 0;
        }
        if (ticks == 10 && c[2] == 1)
        {
            KeyBinding.setKeyBindState(sneak.getKeyCode(), true);
        }
        if (ticks >= 16 && ticks < 16 + 4 * c[1] && (ticks - 16) % 4 == 0)
        {
            DevTestUtil.rightClick(mc);
        }
        if (ticks == 60)
        {
            KeyBinding.setKeyBindState(sneak.getKeyCode(), false);
            onServer = () -> result(ph, c);
        }
        if (ticks == 70)
        {
            phase++;
            ticks = 0;
        }
        return true;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    private void setup(int ph, int[] c)
    {
        EntityPlayerMP p = DevTestUtil.player();
        if (ph == 0)
        {
            DevTestArena.build(p.worldObj, p);
            p.fallDistance = 0;
            p.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            for (Object o : Item.itemRegistry)
            {
                Item i = (Item) o;
                if (mag == null && Deci.magazineCapacity(i) >= 30 && Deci.magazineRound(i) != null)
                {
                    mag = i;
                    round = Deci.magazineRound(i);
                }
            }
            DevTestResults.value(name(), "magazine", mag == null ? "none" : Item.itemRegistry.getNameForObject(mag) + " of "
                                 + Deci.magazineCapacity(mag) + ", round " + Item.itemRegistry.getNameForObject(round));
        }
        for (Object o : p.worldObj.getEntitiesWithinAABB(EntityItem.class, p.boundingBox.expand(8, 8, 8)))
        {
            ((EntityItem) o).setDead();
        }
        p.inventory.clearInventory(null, -1);
        ItemStack m = new ItemStack(mag);
        m.stackTagCompound = new NBTTagCompound();
        m.stackTagCompound.setInteger("ammo", c[0]);
        p.inventory.setInventorySlotContents(0, m);
        p.inventory.setInventorySlotContents(2, new ItemStack(round, 5));
        if (c[3] == 1)
        {
            for (int i = 1; i < p.inventory.mainInventory.length; i++)
            {
                p.inventory.setInventorySlotContents(i, new ItemStack(Blocks.stone, 64));
            }
        }
        p.inventory.currentItem = 0;
    }

    private void result(int ph, int[] c)
    {
        EntityPlayerMP p = DevTestUtil.player();
        int loose = 0, dropped = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            loose += s != null && s.getItem() == round ? s.stackSize : 0;
        }
        for (Object o : p.worldObj.getEntitiesWithinAABB(EntityItem.class, p.boundingBox.expand(8, 8, 8)))
        {
            ItemStack s = ((EntityItem) o).getEntityItem();
            dropped += s.getItem() == round ? s.stackSize : 0;
        }
        String got = ammo() + " in the magazine, " + loose + " loose, " + dropped + " on the ground";
        int start = c[3] == 1 ? 0 : 5;
        int moved = c[2] == 0 ? -1 : Math.min(c[1], c[0]);        // a plain click loads one, a sneaking one unloads
        int ammo = c[0] - moved, inv = c[3] == 1 ? start : start + moved, ground = c[3] == 1 ? moved : 0;
        String[] what = {"one sneaking click: one round out", "five clicks: five rounds out",
                         "empty magazine: nothing, no round loaded", "full inventory: the round on the ground",
                         "plain click: Decimation still loads one"};
        DevTestResults.check(name(), what[ph], got, ammo() == ammo && loose == inv && dropped == ground,
                             ammo + " in the magazine, " + inv + " loose, " + ground + " on the ground");
    }

    private int ammo()
    {
        ItemStack m = DevTestUtil.player().inventory.getStackInSlot(0);
        return m == null || m.getItem() != mag || !m.hasTagCompound() ? -1 : m.getTagCompound().getInteger("ammo");
    }
}
