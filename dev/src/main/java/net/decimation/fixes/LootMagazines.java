package net.decimation.fixes;

import java.util.Random;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Looted magazines hold a random number of rounds (user, 22 Oktober 2026: "now all mag loot have variaty capacity ammo
 * inside it"; chosen: 1 round to full, weighted toward half full, rarely full, never empty). Decimation's loot lists
 * hold magazines without NBT, and a magazine without NBT fills itself to capacity on its first tick in an inventory
 * (AmmoItem.onUpdate): every looted magazine was full (dev test lootmags: 38 of 38).
 * Decimation's LootInteractHandler rolls a click's loot into a new inventory and hands it to PlayerData during the
 * click; this handler remembers the player's loot inventory before that (HIGHEST) and gives the magazines of a new one
 * their rounds after it (LOWEST, also when the click was cancelled, which the handler does). The screen opens two ticks
 * later and its container sends the server's stacks, so the client shows the same rounds. Server side, once a click.
 */
public final class LootMagazines
{
    private final Random random = new Random();
    private EntityPlayer clicker;
    private InventoryBasic before;

    /** Rounds in a looted magazine: two dice, so around half; 1 at least, full only now and then. */
    static int rounds(int capacity, Random r)
    {
        if (capacity <= 1)
        {
            return capacity;
        }
        double share = (r.nextDouble() + r.nextDouble()) / 2;
        return Math.max(1, Math.min(capacity, (int) Math.round(share * capacity)));
    }

    /** Gives every magazine of this inventory without rounds set its random rounds; returns how many. */
    public static int fill(IInventory inv, Random r)
    {
        int n = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack s = inv.getStackInSlot(i);
            int cap = s == null ? 0 : Deci.magazineCapacity(s.getItem());
            if (cap > 1 && !(s.hasTagCompound() && s.getTagCompound().hasKey("ammo")))
            {
                if (s.stackTagCompound == null)
                {
                    s.stackTagCompound = new NBTTagCompound();
                }
                s.stackTagCompound.setInteger("ammo", rounds(cap, r));
                n++;
            }
        }
        return n;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void before(PlayerInteractEvent e)
    {
        if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && !e.entityPlayer.worldObj.isRemote)
        {
            clicker = e.entityPlayer;
            before = Deci.lootInventory(e.entityPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void after(PlayerInteractEvent e)
    {
        if (e.entityPlayer != clicker || e.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            return;
        }
        InventoryBasic now = Deci.lootInventory(e.entityPlayer);
        if (now != null && now != before)
        {
            fill(now, random);
        }
        clicker = null;
        before = null;
    }
}
