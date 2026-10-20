package net.decimation.fixes.gunsmith;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * The Gunsmith screen's container (docs/superpowers/specs/2026-10-21-gunsmith-screen-design.md): opened in place of
 * Decimation's WeaponModifyContainer for its attachments GUI id (GunsmithGuiHandler). The gun stays in its hotbar slot;
 * slots 0 .. 5 are its parts (by GunSlot ordinal), a view of its NBT: every change writes the NBT at once (GunParts), so
 * nothing is ever kept in the container and closing drops nothing. Slots 6 .. 14 are the hotbar, 15 .. 41 the main
 * inventory (synced for the screen's lists, never drawn). The list's clicks arrive as button ids (enchantItem,
 * GunsmithAction); the server checks each one by Decimation's rules.
 */
public class GunsmithContainer extends Container
{
    public static final int PARTS = 6, HOTBAR = PARTS, MAIN = HOTBAR + 9;
    /** The hotbar slot of the gun, fixed while the screen is open. */
    public final int heldSlot;
    /** Slot updates sent to the client (Task 6: none while idle). */
    public int slotUpdates;
    private final EntityPlayer player;
    private final ItemStack gun;
    private final Parts parts = new Parts();
    private boolean ready;

    /** The gun's parts: any change is written to the gun's NBT on the server. */
    private final class Parts extends InventoryBasic
    {
        Parts()
        {
            super("Gunsmith", false, PARTS);
        }

        @Override
        public void markDirty()
        {
            super.markDirty();
            if (ready && !player.worldObj.isRemote)
            {
                for (GunSlot s : GunSlot.values())
                {
                    ItemStack part = getStackInSlot(s.ordinal());
                    if (!s.usedUp() || part != null)
                    {
                        GunParts.write(gun, s, part);
                    }
                }
            }
        }
    }

    private final class PartSlot extends Slot
    {
        final GunSlot kind;

        PartSlot(GunSlot kind)
        {
            super(parts, kind.ordinal(), -2000, -2000);
            this.kind = kind;
        }

        @Override
        public boolean isItemValid(ItemStack s)
        {
            return GunParts.fits(kind, gun(), s);
        }

        @Override
        public boolean canTakeStack(EntityPlayer p)
        {
            return !kind.usedUp();
        }

        @Override
        public int getSlotStackLimit()
        {
            return 1;
        }
    }

    /**
     * A main inventory slot: synced so the screen's lists see the bag, never drawn. GuiContainer draws every slot's item
     * each frame, wherever it sits: on the client it holds nothing (the lists read the player's inventory itself).
     * Measured: 25 full slots added 0.39 ms to the screen's draw in the production client with OptiFine.
     */
    private final class BagSlot extends Slot
    {
        BagSlot(int index)
        {
            super(player.inventory, index, -2000, -2000);
        }

        @Override
        public ItemStack getStack()
        {
            return player.worldObj.isRemote ? null : super.getStack();
        }
    }

    public GunsmithContainer(EntityPlayer player)
    {
        this.player = player;
        this.heldSlot = player.inventory.currentItem;
        this.gun = player.inventory.getStackInSlot(heldSlot);
        ItemStack[] fitted = GunParts.read(gun);
        for (GunSlot s : GunSlot.values())
        {
            parts.setInventorySlotContents(s.ordinal(), fitted[s.ordinal()]);
            addSlotToContainer(new PartSlot(s));
        }
        for (int i = 0; i < 36; i++)
        {
            addSlotToContainer(i < 9 ? new Slot(player.inventory, i, -2000, -2000) : new BagSlot(i));
        }
        ready = true;
    }

    /** The gun: on the server the very stack held at opening, on the client whatever its hotbar slot holds now. */
    public ItemStack gun()
    {
        return player.worldObj.isRemote ? player.inventory.getStackInSlot(heldSlot) : gun;
    }

    public GunSlot kind(int slot)
    {
        return slot >= 0 && slot < PARTS ? GunSlot.values()[slot] : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer p)
    {
        // the server closes the screen once the gun is no longer the one in hand (dropped, replaced, slot changed)
        return p.worldObj.isRemote || p.inventory.currentItem == heldSlot && p.inventory.getStackInSlot(heldSlot) == gun
                                      && gun != null && net.decimation.fixes.Deci.isGun(gun.getItem());
    }

    @Override
    public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer p)
    {
        if (slotId == HOTBAR + heldSlot || mode == 2 && (button == heldSlot || slotId < PARTS) || mode == 6
            || slotId >= 0 && slotId < PARTS && mode != 0)
        {
            return null; // the gun never moves; parts only by a plain click (a swap or a collect would bypass the rules)
        }
        return super.slotClick(slotId, button, mode, p);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int slot)
    {
        return null;
    }

    @Override
    public boolean enchantItem(EntityPlayer p, int id)
    {
        GunsmithAction a = GunsmithAction.decode(id);
        if (a == null || p.worldObj.isRemote || !canInteractWith(p))
        {
            return false;
        }
        return a.takeOff ? takeOff(p, a.slot) : fit(p, a.slot, a.invIndex);
    }

    private boolean fit(EntityPlayer p, GunSlot s, int index)
    {
        ItemStack src = p.inventory.getStackInSlot(index);
        if (index == heldSlot || src == null || !GunParts.fits(s, gun, src))
        {
            return false;
        }
        ItemStack old = parts.getStackInSlot(s.ordinal());
        boolean back = old != null && !s.usedUp();
        if (back && src.stackSize > 1 && p.inventory.getFirstEmptyStack() < 0)
        {
            tell(p, "Inventory full");
            return false;
        }
        ItemStack one = src.splitStack(1);
        if (src.stackSize <= 0)
        {
            p.inventory.setInventorySlotContents(index, null);
        }
        parts.setInventorySlotContents(s.ordinal(), one);
        if (back && !p.inventory.addItemStackToInventory(old))
        {
            p.dropPlayerItemWithRandomChoice(old, false); // not reached: room was checked above
        }
        detectAndSendChanges();
        return true;
    }

    private boolean takeOff(EntityPlayer p, GunSlot s)
    {
        ItemStack old = parts.getStackInSlot(s.ordinal());
        if (s.usedUp() || old == null)
        {
            return false;
        }
        if (p.inventory.getFirstEmptyStack() < 0)
        {
            tell(p, "Inventory full");
            return false;
        }
        parts.setInventorySlotContents(s.ordinal(), null);
        p.inventory.addItemStackToInventory(old);
        detectAndSendChanges();
        return true;
    }

    static void tell(EntityPlayer p, String line)
    {
        p.addChatMessage(new ChatComponentText(line).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY)));
    }

    @Override
    public void detectAndSendChanges()
    {
        for (int i = 0; i < inventorySlots.size(); i++)
        {
            ItemStack now = ((Slot) inventorySlots.get(i)).getStack();
            if (!ItemStack.areItemStacksEqual((ItemStack) inventoryItemStacks.get(i), now))
            {
                slotUpdates++;
            }
        }
        super.detectAndSendChanges();
    }
}
