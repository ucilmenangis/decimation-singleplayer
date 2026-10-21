package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.List;

import net.decimation.fixes.Deci;
import net.decimation.fixes.InventoryShift;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * bug.md "Inventory: no shift click" (user, 22 Oktober 2026): shift clicks in Decimation's inventory screen the way
 * GuiContainer sends them (PlayerControllerMP.windowClick, mode 1: the client's container predicts, the packet goes to
 * the integrated server, which answers). Each case closes the screen, sets the inventory up on the server, opens the
 * screen through openGui as Decimation does, clicks one slot and reads the server's inventory 12 ticks later; every
 * case also checks no item was made or lost (all slots and the cursor counted) and that the client's slots show what
 * the server holds. `fix=off` runs with InventoryShift.enabled false (Decimation's own containers: the RED run).
 *
 * Container slots of Decimation's PlayerGearContainer: 0..3 armor (head first), 4..30 main rows, 31..39 hotbar, 40
 * backpack, 41 mask, 42 vest, then the worn backpack's slots, then the loot area.
 */
public class InvShiftTest extends DevTestMode
{
    public String name() { return "invshift"; }

    private static final int CASES = 11;
    private volatile int kase, phase, wait;
    private volatile boolean done;
    private int before;
    private String predicted = "";

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        if (done)
        {
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            InventoryShift.enabled = true;
            return false;
        }
        if (phase == 1) // the screen is open on both sides: click
        {
            Container c = mc.thePlayer.openContainer;
            int slot = clickSlot();
            if (++wait > 200)
            {
                DevTestResults.check(name(), label() + ": the screen opened with slot " + slot, "no", false, "yes");
                predicted = "no click";
                wait = 0;
                phase = 3;
                return true;
            }
            if (!Deci.isInventoryScreen(mc.currentScreen) || c == null || c.windowId == 0 || wait < 6
                || c.inventorySlots.size() <= slot)
            {
                return true;
            }
            ItemStack had = c.inventorySlots.size() > slot ? ((Slot) c.inventorySlots.get(slot)).getStack() : null;
            mc.playerController.windowClick(c.windowId, slot, 0, 1, mc.thePlayer);
            ItemStack now = ((Slot) c.inventorySlots.get(slot)).getStack();
            predicted = had != null && (now == null || now.stackSize != had.stackSize) ? "moved" : "stayed";
            wait = 0;
            phase = 2;
        }
        else if (phase == 2 && ++wait >= 12) // the server's answer has arrived: compare the client's slots with it
        {
            EntityPlayerMP p = DevTestUtil.player();
            String diff = p == null ? "no player" : sameSlots(mc.thePlayer.openContainer, p.openContainer);
            DevTestResults.check(name(), label() + ": client shows what the server holds", diff.isEmpty() ? "same" : diff,
                                 diff.isEmpty(), "same");
            phase = 3;
        }
        return true;
    }

    @Override
    public void server()
    {
        if (done)
        {
            return;
        }
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null)
        {
            return;
        }
        if (phase == 0)
        {
            net.decimation.worldgen.DevAutoTest.keepScreen = true; // before the first open (it closes screens otherwise)
            if (kase == 0 && wait++ < 10)
            {
                return;
            }
            if (kase == 0)
            {
                InventoryShift.enabled = !"off".equals(System.getProperty("deciworldgen.autotest.fix"));
                DevTestResults.value(name(), "fix", InventoryShift.enabled ? "on" : "off (Decimation's containers)");
                DevTestArena.build(w, p);
                p.setGameType(WorldSettings.GameType.SURVIVAL);
                p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
            }
            p.closeScreen();
            setUp(p);
            if (kase == 8)
            {
                InventoryBasic loot = new InventoryBasic("invshift", false, 8);
                loot.setInventorySlotContents(0, stack("minecraft:cobblestone", 40));
                Deci.setLootInventory(p, loot);
                Deci.openLootGui(p);
            }
            else
            {
                p.openGui(Deci.decimationMod(), Deci.inventoryGuiId(), p.worldObj, -1, 0, 0);
            }
            before = count(p);
            if (kase == 0)
            {
                DevTestResults.check(name(), "the server's container has shift click", Deci.hasShiftClick(p.openContainer),
                                     Deci.hasShiftClick(p.openContainer), "true");
            }
            wait = 0;
            phase = 1;
        }
        else if (phase == 3)
        {
            check(p);
            int after = count(p);
            DevTestResults.check(name(), label() + ": items counted before and after", before + " / " + after,
                                 before == after, "equal");
            if (kase == 0)
            {
                Container cc = Minecraft.getMinecraft().thePlayer.openContainer;
                DevTestResults.check(name(), "the client's container has shift click", Deci.hasShiftClick(cc), Deci.hasShiftClick(cc), "true");
            }
            DevTestResults.check(name(), label() + ": the client moved it at once (prediction)", predicted,
                                 "moved".equals(predicted) == expectMove(), expectMove() ? "moved" : "stayed");
            if (++kase >= CASES)
            {
                p.closeScreen();
                Deci.setPlayerGear(p, null, null, null);
                clear(p);
                done = true;
            }
            phase = 0;
        }
    }

    private String label()
    {
        switch (kase)
        {
            case 0: return "helmet in a main row";
            case 1: return "helmet worn";
            case 2: return "helmet in the hotbar";
            case 3: return "vest in a main row";
            case 4: return "gas mask in a main row";
            case 5: return "backpack in a main row";
            case 6: return "stack with a backpack worn";
            case 7: return "stack in a main row, no backpack";
            case 8: return "stack in the loot area";
            case 9: return "helmet worn, inventory full";
            default: return "full backpack worn";
        }
    }

    private int clickSlot()
    {
        switch (kase)
        {
            case 1: case 9: return 0;       // the helmet slot
            case 10: return 40;             // the backpack slot
            case 2: return 31;              // hotbar 0
            case 8: return 43;              // loot slot 0 (no backpack worn)
            default: return 4;              // main row slot 9
        }
    }

    private boolean expectMove()
    {
        return kase != 9;
    }

    private void setUp(EntityPlayerMP p)
    {
        clear(p);
        Deci.setPlayerGear(p, null, null, null);
        switch (kase)
        {
            case 0: case 3: case 4: case 5:
                p.inventory.mainInventory[9] = stack(kase == 0 ? "deci:marineHelm" : kase == 3 ? "deci:militaryvest"
                                                     : kase == 4 ? "deci:gasmask" : "deci:ItemHikingPack_Green", 1);
                break;
            case 1:
                p.inventory.armorInventory[3] = stack("deci:marineHelm", 1);
                break;
            case 2:
                p.inventory.mainInventory[0] = stack("deci:marineHelm", 1);
                break;
            case 6:
                Deci.setPlayerGear(p, null, null, withTag(stack("deci:ItemHikingPack_Green", 1)));
                p.inventory.mainInventory[9] = stack("minecraft:cobblestone", 20);
                break;
            case 7:
                p.inventory.mainInventory[9] = stack("minecraft:cobblestone", 10);
                p.inventory.mainInventory[0] = stack("minecraft:cobblestone", 60);
                break;
            case 10:
                ItemStack pack = withTag(stack("deci:ItemHikingPack_Green", 1));
                net.minecraft.nbt.NBTTagCompound in = new net.minecraft.nbt.NBTTagCompound();
                stack("minecraft:cobblestone", 20).writeToNBT(in);
                pack.getTagCompound().setTag("stack_0", in);
                Deci.setPlayerGear(p, null, null, pack);
                break;
            case 9:
                p.inventory.armorInventory[3] = stack("deci:marineHelm", 1);
                for (int i = 0; i < 36; i++)
                {
                    p.inventory.mainInventory[i] = stack("minecraft:cobblestone", 64);
                }
                break;
            default:
                break;
        }
        p.inventory.markDirty();
    }

    private void check(EntityPlayerMP p)
    {
        ItemStack[] gear = Deci.playerGear(p); // vest, mask, backpack
        ItemStack[] main = p.inventory.mainInventory;
        ItemStack head = p.inventory.armorInventory[3];
        String got;
        boolean pass;
        switch (kase)
        {
            case 0: case 2:
                got = name(head) + " on the head, " + name(main[kase == 0 ? 9 : 0]) + " left";
                pass = "marineHelm".equals(name(head)) && main[kase == 0 ? 9 : 0] == null;
                break;
            case 1:
                got = name(head) + " on the head, main 9 " + name(main[9]);
                pass = head == null && "marineHelm".equals(name(main[9]));
                break;
            case 3: case 4: case 5:
                ItemStack on = gear.length < 3 ? null : gear[kase - 3 == 0 ? 0 : kase - 3 == 1 ? 1 : 2];
                got = name(on) + " worn, main 9 " + name(main[9]);
                pass = on != null && main[9] == null;
                break;
            case 6:
                int inPack = 0;
                for (Object o : p.openContainer.inventorySlots)
                {
                    Slot s = (Slot) o;
                    if (Deci.isBackpackInventory(s.inventory) && s.getHasStack())
                    {
                        inPack += s.getStack().stackSize;
                    }
                }
                got = inPack + " in the backpack, main 9 " + name(main[9]);
                pass = inPack == 20 && main[9] == null;
                break;
            case 7:
                got = "hotbar " + size(main[0]) + " + " + size(main[1]) + ", main 9 " + size(main[9]);
                pass = size(main[0]) == 64 && size(main[1]) == 6 && main[9] == null;
                break;
            case 8:
                int taken = 0;
                for (ItemStack s : main)
                {
                    taken += s != null && "cobblestone".equals(name(s)) ? s.stackSize : 0;
                }
                got = taken + " taken into the inventory";
                pass = taken == 40;
                break;
            case 9:
                got = name(head) + " on the head";
                pass = "marineHelm".equals(name(head));
                break;
            default:
                ItemStack held = main[9];
                int kept = stored(held);
                got = name(gear.length < 3 ? null : gear[2]) + " worn, main 9 " + name(held) + " holding " + kept;
                pass = gear.length == 3 && gear[2] == null && "ItemHikingPack_Green".equals(name(held)) && kept == 20;
                break;
        }
        DevTestResults.check(name(), label(), got, pass, expectation());
    }

    private String expectation()
    {
        switch (kase)
        {
            case 0: case 2: return "marineHelm on the head, slot empty";
            case 1: return "taken off into main 9";
            case 3: return "vest worn";
            case 4: return "mask worn";
            case 5: return "backpack worn";
            case 6: return "20 in the backpack";
            case 7: return "hotbar 64 + 6, main 9 empty";
            case 8: return "40 taken";
            case 9: return "stays on the head (nowhere to go)";
            default: return "taken off into main 9, still holding its 20";
        }
    }

    /** Every stack of the server's open container (inventory, armor, gear, backpack, loot) and the cursor. */
    private static int count(EntityPlayerMP p)
    {
        int n = p.inventory.getItemStack() == null ? 0 : p.inventory.getItemStack().stackSize;
        for (Object o : p.openContainer.inventorySlots)
        {
            if (Deci.isBackpackInventory(((Slot) o).inventory))
            {
                continue; // a view of the worn backpack's tag, counted there
            }
            ItemStack s = ((Slot) o).getStack();
            n += s == null ? 0 : s.stackSize + stored(s);
        }
        return n;
    }

    /** Items kept in a backpack's tag (ItemBackpack: "stack_N" compounds), 0 for anything else. */
    private static int stored(ItemStack s)
    {
        int n = 0;
        if (s != null && s.hasTagCompound())
        {
            for (Object k : s.getTagCompound().func_150296_c())
            {
                String key = (String) k;
                if (key.startsWith("stack_"))
                {
                    ItemStack in = ItemStack.loadItemStackFromNBT(s.getTagCompound().getCompoundTag(key));
                    n += in == null ? 0 : in.stackSize;
                }
            }
        }
        return n;
    }

    private static String sameSlots(Container client, Container server)
    {
        if (client == null || server == null || client.inventorySlots.size() != server.inventorySlots.size())
        {
            return "slot counts " + (client == null ? -1 : client.inventorySlots.size()) + " / "
                + (server == null ? -1 : server.inventorySlots.size());
        }
        List<String> d = new ArrayList<String>();
        for (int i = 0; i < client.inventorySlots.size(); i++)
        {
            ItemStack a = ((Slot) client.inventorySlots.get(i)).getStack(), b = ((Slot) server.inventorySlots.get(i)).getStack();
            if (!(name(a) + size(a)).equals(name(b) + size(b)))
            {
                d.add(i + ":" + name(a) + "x" + size(a) + "/" + name(b) + "x" + size(b));
            }
        }
        return String.join(" ", d);
    }

    private static void clear(EntityPlayerMP p)
    {
        for (int i = 0; i < p.inventory.mainInventory.length; i++)
        {
            p.inventory.mainInventory[i] = null;
        }
        for (int i = 0; i < p.inventory.armorInventory.length; i++)
        {
            p.inventory.armorInventory[i] = null;
        }
        p.inventory.setItemStack(null);
    }

    private static ItemStack stack(String name, int n)
    {
        Item item = (Item) Item.itemRegistry.getObject(name);
        return item == null ? null : new ItemStack(item, n);
    }

    private static ItemStack withTag(ItemStack s)
    {
        if (s != null && !s.hasTagCompound())
        {
            s.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        return s;
    }

    private static String name(ItemStack s)
    {
        if (s == null)
        {
            return "nothing";
        }
        String n = Item.itemRegistry.getNameForObject(s.getItem());
        return n == null ? "?" : n.substring(n.indexOf(':') + 1);
    }

    private static int size(ItemStack s)
    {
        return s == null ? 0 : s.stackSize;
    }
}
