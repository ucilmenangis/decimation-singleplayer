package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Shift click in Decimation's inventory screen (user report 22 Oktober 2026, bug.md "Inventory: no shift click"):
 * its PlayerGearContainer answers transferStackInSlot with null, so a shift click moved nothing, not even armor onto
 * the body. Deci.GearContainer is that container with transferStackInSlot sending here; no jar patch. The server
 * gets it from our wrapper of Decimation's GUI handler (install, after the Gunsmith's wrapper); the client screen
 * builds its own container in initGui, and Client swaps it for ours on InitGuiEvent.Post (the same slots, built by
 * Decimation's own setupSlots).
 *
 * Where a stack goes (targets): from the inventory first onto the body (armor slots, then Decimation's mask, vest and
 * backpack slots, each only when empty and the slot takes it by Decimation's own rules), then into a worn backpack,
 * then to the other row (main to hotbar, hotbar to main). From armor, gear, a backpack or the loot area: into the
 * inventory, main rows first. Stacks merge with equal ones first, every slot's own limit and isItemValid are kept,
 * and nothing moves when nothing fits.
 */
public final class InventoryShift
{
    public enum Region
    {
        ARMOR, MAIN, HOTBAR, GEAR, BACKPACK, LOOT
    }

    /** Off: Decimation's own containers, no shift click (-Ddeciworldgen.invshift=false; dev test invshift switches it). */
    public static boolean enabled = !"false".equals(System.getProperty("deciworldgen.invshift"));

    private InventoryShift()
    {
    }

    /** The regions a stack shift clicked in from tries, in order. */
    public static Region[] targets(Region from, boolean backpackWorn)
    {
        switch (from)
        {
            case MAIN:
                return backpackWorn ? new Region[] {Region.ARMOR, Region.GEAR, Region.BACKPACK, Region.HOTBAR}
                                    : new Region[] {Region.ARMOR, Region.GEAR, Region.HOTBAR};
            case HOTBAR:
                return backpackWorn ? new Region[] {Region.ARMOR, Region.GEAR, Region.BACKPACK, Region.MAIN}
                                    : new Region[] {Region.ARMOR, Region.GEAR, Region.MAIN};
            default:
                return new Region[] {Region.MAIN, Region.HOTBAR};
        }
    }

    /** Which part of Decimation's inventory screen a slot belongs to. */
    public static Region region(Slot s)
    {
        if (s.inventory instanceof InventoryPlayer)
        {
            int i = s.getSlotIndex();
            return i >= 36 ? Region.ARMOR : i < 9 ? Region.HOTBAR : Region.MAIN;
        }
        if (Deci.isGearInventory(s.inventory))
        {
            return Region.GEAR;
        }
        return Deci.isBackpackInventory(s.inventory) ? Region.BACKPACK : Region.LOOT;
    }

    /**
     * Container.transferStackInSlot of Deci.GearContainer: moves the stack of slot index as far as it fits; a copy
     * of the stack as it was when anything moved, else null (vanilla's slotClick repeats the call while it gets a
     * stack back and the slot still holds that item, so null ends the loop).
     */
    public static ItemStack transfer(Container c, EntityPlayer player, int index)
    {
        if (!enabled || index < 0 || index >= c.inventorySlots.size())
        {
            return null;
        }
        Slot from = (Slot) c.inventorySlots.get(index);
        if (from == null || !from.getHasStack() || !from.canTakeStack(player))
        {
            return null;
        }
        ItemStack stack = from.getStack();
        ItemStack before = stack.copy();
        Region source = region(from);
        boolean backpack = false;
        List<Slot> slots = new ArrayList<Slot>(c.inventorySlots.size());
        List<Region> regions = new ArrayList<Region>(c.inventorySlots.size());
        for (Object o : c.inventorySlots)
        {
            Slot s = (Slot) o;
            Region r = region(s);
            backpack |= r == Region.BACKPACK;
            slots.add(s);
            regions.add(r);
        }
        boolean worn = false; // a gear slot took the stack: Decimation rebuilt the slot list (stop here)
        for (Region target : targets(source, backpack))
        {
            if (stack.stackSize <= 0 || worn)
            {
                break;
            }
            if (target != Region.GEAR && target != Region.ARMOR)
            {
                merge(stack, from, slots, regions, target);
            }
            worn = fill(stack, from, slots, regions, target);
        }
        if (stack.stackSize == before.stackSize)
        {
            return null;
        }
        if (stack.stackSize <= 0)
        {
            from.putStack(null);
        }
        else
        {
            from.onSlotChanged();
        }
        from.onPickupFromSlot(player, stack.stackSize <= 0 ? before : stack);
        return before;
    }

    /** Adds to equal stacks of the region (same item, damage and tag), within each slot's limit. */
    private static void merge(ItemStack stack, Slot from, List<Slot> slots, List<Region> regions, Region target)
    {
        for (int i = 0; i < slots.size() && stack.stackSize > 0; i++)
        {
            Slot s = slots.get(i);
            ItemStack there = s.getStack();
            if (regions.get(i) != target || s == from || there == null || !s.isItemValid(stack) || !stack.isStackable()
                || there.getItem() != stack.getItem() || (stack.getHasSubtypes() && stack.getItemDamage() != there.getItemDamage())
                || !ItemStack.areItemStackTagsEqual(stack, there))
            {
                continue;
            }
            int room = Math.min(s.getSlotStackLimit(), stack.getMaxStackSize()) - there.stackSize;
            if (room > 0)
            {
                int n = Math.min(room, stack.stackSize);
                there.stackSize += n;
                stack.stackSize -= n;
                s.onSlotChanged();
            }
        }
    }

    /** Puts the stack (or as much as a slot holds) into empty slots of the region that take it; true when a gear slot did. */
    private static boolean fill(ItemStack stack, Slot from, List<Slot> slots, List<Region> regions, Region target)
    {
        for (int i = 0; i < slots.size() && stack.stackSize > 0; i++)
        {
            Slot s = slots.get(i);
            if (regions.get(i) != target || s == from || s.getHasStack() || !s.isItemValid(stack))
            {
                continue;
            }
            int n = Math.min(stack.stackSize, Math.min(s.getSlotStackLimit(), stack.getMaxStackSize()));
            if (n <= 0)
            {
                continue;
            }
            s.putStack(stack.splitStack(n));
            if (target == Region.GEAR)
            {
                return true;
            }
        }
        return false;
    }

    // ---- wiring

    /** Wraps the GUI handler FML holds for Decimation (after GunsmithGuiHandler.install, postInit, both sides). */
    public static void install()
    {
        IGuiHandler old = Deci.decimationGuiHandler();
        if (old != null && !(old instanceof Handler))
        {
            NetworkRegistry.INSTANCE.registerGuiHandler(Deci.decimationMod(), new Handler(old));
        }
    }

    /** The server's container of the inventory screen is ours; everything else goes on to the handler before. */
    public static final class Handler implements IGuiHandler
    {
        private final IGuiHandler before;

        private Handler(IGuiHandler before)
        {
            this.before = before;
        }

        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
        {
            return enabled && id == Deci.inventoryGuiId() ? Deci.gearContainer(player, x, y)
                                                : before.getServerGuiElement(id, player, world, x, y, z);
        }

        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
        {
            return before.getClientGuiElement(id, player, world, x, y, z);
        }
    }

    /** Client: Decimation's inventory screen gets our container in place of the one its initGui built. */
    public static final class Client
    {
        @SubscribeEvent
        public void initGui(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post e)
        {
            if (!enabled || !Deci.isInventoryScreen(e.gui))
            {
                return;
            }
            net.minecraft.client.gui.inventory.GuiContainer screen = (net.minecraft.client.gui.inventory.GuiContainer) e.gui;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            Container ours = Deci.gearContainerLike(screen.inventorySlots, mc.thePlayer);
            if (ours != null)
            {
                screen.inventorySlots = ours;
                mc.thePlayer.openContainer = ours;
            }
        }
    }
}
