package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.gunsmith.GunsmithContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/**
 * A Foregrip or RK-1 on a gun without a rail comes off into the player's inventory (user, 21 Oktober 2026: "Pops off to
 * inventory"; fixes/GripRails): guns fitted before the rule, from loot, from NPC bodies or another player. Server side,
 * once a second, every player's main inventory (36 slots, a string read each: nothing else per tick). Never while the
 * Gunsmith is open: its part slots are a view of the held gun, so taking the grip then would copy it when the screen
 * writes its slots back. The grip goes to the inventory, else onto the ground at the player's feet: no item is lost.
 */
public final class GripStrip
{
    private static final String KEY = "gripAttach";
    private int ticks;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || ++ticks % 20 != 0)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null)
        {
            return;
        }
        for (Object o : server.getConfigurationManager().playerEntityList)
        {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (!(p.openContainer instanceof GunsmithContainer))
            {
                strip(p);
            }
        }
    }

    /** Takes every vertical grip off this player's rail-less guns; returns how many came off. */
    public static int strip(EntityPlayerMP p)
    {
        int n = 0;
        ItemStack[] inv = p.inventory.mainInventory;
        for (int i = 0; i < inv.length; i++)
        {
            ItemStack gun = inv[i];
            if (gun == null || gun.stackTagCompound == null || !Deci.isGun(gun.getItem()))
            {
                continue;
            }
            String grip = gun.stackTagCompound.getString(KEY);
            if (!GripRails.needsRail(grip) || GripRails.fits(GameRegistry.findUniqueIdentifierFor(gun.getItem()).name, grip))
            {
                continue;
            }
            gun.stackTagCompound.setString(KEY, "");
            n++;
            Item item = GameRegistry.findItem("deci", grip);
            if (item != null)
            {
                ItemStack back = new ItemStack(item);
                if (!p.inventory.addItemStackToInventory(back))
                {
                    p.dropPlayerItemWithRandomChoice(back, false);
                }
            }
        }
        if (n > 0)
        {
            p.inventoryContainer.detectAndSendChanges();
        }
        return n;
    }
}
