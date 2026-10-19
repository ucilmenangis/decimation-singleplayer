package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.server.MinecraftServer;

/**
 * bug.md "The last grenade never runs out" (user, 19 Oktober 2026): Decimation's throwables (GrenadeItem.throwGrenade
 * and the five that copy it, deci.aA.*) take one off the held stack on the server and never remove a stack of 0, which
 * looks like one left (counts under 2 are not drawn) and throws again and again. After every server tick, a stack of 0
 * or less in a player's inventory is removed, as vanilla does after placing the last block of a stack. The server tick,
 * not the player tick: a player's tick runs only when that client's movement packet arrives (dev test `throwables`
 * failed after a teleport that way).
 */
public final class EmptyStacks
{
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e)
    {
        MinecraftServer server = MinecraftServer.getServer();
        if (e.phase != TickEvent.Phase.END || server == null || server.getConfigurationManager() == null)
        {
            return;
        }
        for (Object o : server.getConfigurationManager().playerEntityList)
        {
            clear(((EntityPlayer) o).inventory);
        }
    }

    static void clear(InventoryPlayer inv)
    {
        for (int i = 0; i < inv.mainInventory.length; i++)
        {
            if (inv.mainInventory[i] != null && inv.mainInventory[i].stackSize <= 0)
            {
                inv.mainInventory[i] = null;
            }
        }
        for (int i = 0; i < inv.armorInventory.length; i++)
        {
            if (inv.armorInventory[i] != null && inv.armorInventory[i].stackSize <= 0)
            {
                inv.armorInventory[i] = null;
            }
        }
    }
}
