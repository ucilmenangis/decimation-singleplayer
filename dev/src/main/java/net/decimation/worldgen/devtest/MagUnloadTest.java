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
 * Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"; chosen: about a
 * second like a reload, the rounds as Decimation's loose rounds of that calibre into the inventory, dropped if full, the
 * empty magazine stays). The player holds a magazine with 20 rounds and has 5 loose rounds of its calibre (so Decimation's
 * own right click, which loads one round, would show as 21); sneak held, a right click as the mouse makes it
 * (DevTestUtil.rightClick). Cases: unload (20 rounds out, magazine empty), switching away within the second (nothing
 * happens), a full inventory (the rounds on the ground).
 */
public class MagUnloadTest extends DevTestMode
{
    public String name() { return "magunload"; }

    private int phase, ticks;
    private volatile Runnable onServer;
    private Item mag, round;

    public boolean client(Minecraft mc)
    {
        ticks++;
        KeyBinding sneak = mc.gameSettings.keyBindSneak;
        switch (phase)
        {
            case 0: // unload
            case 1: // switch away within the second
            case 2: // full inventory
                if (ticks == 1)
                {
                    final int ph = phase;
                    onServer = () -> setup(ph);
                    mc.thePlayer.inventory.currentItem = 0;
                }
                if (ticks == 10)
                {
                    KeyBinding.setKeyBindState(sneak.getKeyCode(), true);
                }
                if (ticks == 16)
                {
                    DevTestUtil.rightClick(mc);
                }
                if (ticks == 20)
                {
                    onServer = this::early;
                }
                if (ticks == 22 && phase == 1)
                {
                    mc.thePlayer.inventory.currentItem = 1;           // away from the magazine within the second
                }
                if (ticks == 60)
                {
                    KeyBinding.setKeyBindState(sneak.getKeyCode(), false);
                    final int ph = phase;
                    onServer = () -> result(ph);
                }
                if (ticks == 70)
                {
                    phase++;
                    ticks = 0;
                }
                return true;
            default:
                onServer = () -> DevTestUtil.player().inventory.clearInventory(null, -1);
                return false;
        }
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

    private void setup(int ph)
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
        m.stackTagCompound.setInteger("ammo", 20);
        p.inventory.setInventorySlotContents(0, m);
        p.inventory.setInventorySlotContents(2, new ItemStack(round, 5));
        if (ph == 2)
        {
            for (int i = 3; i < p.inventory.mainInventory.length; i++)
            {
                p.inventory.setInventorySlotContents(i, new ItemStack(Blocks.stone, 64));
            }
            p.inventory.setInventorySlotContents(1, new ItemStack(Blocks.stone, 64));
            p.inventory.setInventorySlotContents(2, new ItemStack(Blocks.stone, 64));
        }
        p.inventory.currentItem = 0;
    }

    private void early()
    {
        DevTestResults.check(name(), "phase " + phase + ": the click did not load a round", ammo() + " in the magazine",
                             ammo() <= 20, "20 or less (Decimation's right click would make it 21)");
    }

    private void result(int ph)
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
        if (ph == 0)
        {
            DevTestResults.check(name(), "unload: rounds out, magazine empty", got, ammo() == 0 && loose == 25 && dropped == 0,
                                 "0 in the magazine, 25 loose (20 + 5), 0 on the ground");
        }
        else if (ph == 1)
        {
            DevTestResults.check(name(), "switching away stops it", got, ammo() == 20 && loose == 5 && dropped == 0,
                                 "20 in the magazine, 5 loose");
        }
        else
        {
            DevTestResults.check(name(), "full inventory: rounds on the ground", got, ammo() == 0 && dropped == 20,
                                 "0 in the magazine, 20 on the ground");
        }
    }

    private int ammo()
    {
        ItemStack m = DevTestUtil.player().inventory.getStackInSlot(0);
        return m == null || m.getItem() != mag || !m.hasTagCompound() ? -1 : m.getTagCompound().getInteger("ammo");
    }
}
