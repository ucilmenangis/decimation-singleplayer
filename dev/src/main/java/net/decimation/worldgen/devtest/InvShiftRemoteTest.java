package net.decimation.worldgen.devtest;

import cpw.mods.fml.client.FMLClientHandler;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * bug.md "Inventory: no shift click" off the integrated server: the client joins a dedicated server (server=HOST:PORT,
 * default localhost:25576; the player must be an operator there), gets a helmet, a vest, a gas mask, a backpack and 20
 * cobblestone by commands, opens Decimation's inventory the way the inventory key does (GuiInventory, which Decimation
 * swaps for its own screen through the server), shift clicks each one (PlayerControllerMP.windowClick, mode 1) and
 * reads where the server put it (the client's slots after the server's answer), then takes the helmet off again.
 * Every step also counts the items in all slots (24, nothing made or lost) and none on the ground. Client only.
 */
public class InvShiftRemoteTest extends DevTestMode
{
    public String name() { return "invshiftmp"; }

    private final String host = System.getProperty("deciworldgen.autotest.server", "localhost:25576");
    private static final String[][] MOVES = {   // item, where it must end: armor / vest / mask / backpack / pack / main
        {"deci:marineHelm", "armor"}, {"deci:militaryvest", "vest"}, {"deci:gasmask", "mask"},
        {"deci:ItemHikingPack_Green", "backpack"}, {"minecraft:cobblestone", "pack"}, {"deci:marineHelm", "main"}};
    private int step, ticks, move;

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        ticks++;
        switch (step)
        {
            case 0: // leave a singleplayer world for the menu
                if (mc.theWorld != null && mc.isSingleplayer())
                {
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    mc.displayGuiScreen(new GuiMainMenu());
                }
                if (mc.theWorld == null && ticks > 40)
                {
                    next();
                }
                break;
            case 1: // join
                if (ticks == 1)
                {
                    GuiMultiplayer parent = new GuiMultiplayer(new GuiMainMenu());
                    mc.displayGuiScreen(parent);
                    FMLClientHandler.instance().setupServerList();
                    FMLClientHandler.instance().connectToServer(parent, new ServerData("invshift test", host));
                }
                if (mc.theWorld != null && mc.thePlayer != null && !mc.isSingleplayer())
                {
                    DevTestResults.check(name(), "joined " + host, "in the world", true, "in the server's world");
                    next();
                }
                if (ticks > 1200)
                {
                    DevTestResults.check(name(), "joined " + host, "no world after 60 s", false, "in the server's world");
                    return end(mc);
                }
                break;
            case 2: // items by commands, given in creative (a dedicated server's Decimation lets only creative players pick up)
                String me = mc.thePlayer.getCommandSenderName();
                if (ticks == 100) mc.thePlayer.sendChatMessage("/gamemode 1");
                if (ticks == 110) mc.thePlayer.sendChatMessage("/clear");
                for (int i = 0; i < 5; i++)
                {
                    if (ticks == 120 + 10 * i)
                    {
                        mc.thePlayer.sendChatMessage("/give " + me + " " + MOVES[i][0] + " " + (i == 4 ? 20 : 1));
                    }
                }
                if (ticks == 190) mc.thePlayer.sendChatMessage("/gamemode 0");
                if (ticks == 210)
                {
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer)); // Decimation asks the server for its own screen
                }
                if (ticks > 210 && Deci.isInventoryScreen(mc.currentScreen) && mc.thePlayer.openContainer.windowId != 0)
                {
                    Container c = mc.thePlayer.openContainer;
                    DevTestResults.check(name(), "the inventory key opens Decimation's screen with shift click", Deci.hasShiftClick(c),
                                         Deci.hasShiftClick(c), "true");
                    DevTestResults.check(name(), "items before", count(c), count(c) == 24, "24");
                    next();
                }
                if (ticks > 600)
                {
                    DevTestResults.check(name(), "the inventory screen opened", "no", false, "yes");
                    return end(mc);
                }
                break;
            case 3: // one shift click a move
                Container c = mc.thePlayer.openContainer;
                String[] m = MOVES[move];
                if (ticks == 1)
                {
                    int from = find(c, m[0], "main".equals(m[1]));
                    if (from < 0)
                    {
                        DevTestResults.check(name(), m[0] + " found", "no", false, "in the inventory");
                        return end(mc);
                    }
                    mc.playerController.windowClick(c.windowId, from, 0, 1, mc.thePlayer);
                }
                if (ticks == 30)
                {
                    String at = where(c, m[0]);
                    DevTestResults.check(name(), "shift click " + m[0].substring(m[0].indexOf(':') + 1), at, at.equals(m[1]), m[1]);
                    DevTestResults.check(name(), "after " + m[0].substring(m[0].indexOf(':') + 1) + ": items in all slots", count(c),
                                         count(c) == 24, "24");
                    if (++move >= MOVES.length)
                    {
                        int loose = mc.theWorld.getEntitiesWithinAABB(EntityItem.class, mc.thePlayer.boundingBox.expand(4, 4, 4)).size();
                        DevTestResults.check(name(), "nothing on the ground", loose, loose == 0, "0");
                        mc.thePlayer.closeScreen();
                        mc.thePlayer.sendChatMessage("/clear");
                        next();
                        return true;
                    }
                    ticks = 0;
                }
                break;
            default:
                if (ticks > 20)
                {
                    return end(mc);
                }
        }
        return true;
    }

    /** Container slot of the item: in the worn places (armor 0..3, gear 40..42) when fromWorn, else in the inventory. */
    private static int find(Container c, String item, boolean fromWorn)
    {
        for (int i = 0; i < c.inventorySlots.size(); i++)
        {
            ItemStack s = ((Slot) c.inventorySlots.get(i)).getStack();
            boolean worn = i < 4 || (i >= 40 && i <= 42);
            boolean inv = i >= 4 && i <= 39;
            if (s != null && item.equals(Item.itemRegistry.getNameForObject(s.getItem())) && (fromWorn ? worn : inv))
            {
                return i;
            }
        }
        return -1;
    }

    private static String where(Container c, String item)
    {
        for (int i = 0; i < c.inventorySlots.size(); i++)
        {
            Slot slot = (Slot) c.inventorySlots.get(i);
            ItemStack s = slot.getStack();
            if (s == null || !item.equals(Item.itemRegistry.getNameForObject(s.getItem())))
            {
                continue;
            }
            if (i < 4) return "armor";
            if (i == 40) return "backpack";
            if (i == 41) return "mask";
            if (i == 42) return "vest";
            if (Deci.isBackpackInventory(slot.inventory)) return "pack";
            if (i <= 39) return "main";
            return "slot " + i;
        }
        return "nowhere";
    }

    private static int count(Container c)
    {
        int n = 0;
        for (Object o : c.inventorySlots)
        {
            ItemStack s = ((Slot) o).getStack();
            n += s == null ? 0 : s.stackSize;
        }
        return n;
    }

    private void next()
    {
        step++;
        ticks = 0;
    }

    private boolean end(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = false;
        if (mc.theWorld != null)
        {
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }
        mc.displayGuiScreen(new GuiMainMenu());
        return false;
    }
}
