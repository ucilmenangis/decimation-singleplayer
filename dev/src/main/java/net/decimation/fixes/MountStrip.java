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
 * A part on a gun that cannot mount it comes off into the player's inventory (user, 21 and 22 Oktober 2026: "Pops off to
 * inventory"): a Foregrip or RK-1 on a gun without a rail (fixes/GripRails), a sight on a gun without its mount
 * (fixes/SightMounts); guns fitted before the rules, from loot, from NPC bodies or another player. Server side, once a
 * second, every player's main inventory (36 slots, two string reads each: nothing else per tick). Never while the
 * Gunsmith is open: its part slots are a view of the held gun, so taking a part then would copy it when the screen
 * writes its slots back. The part goes to the inventory, else onto the ground at the player's feet: no item is lost.
 */
public final class MountStrip
{
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

    /** Takes every part this player's guns cannot mount off them; returns how many came off. */
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
            String name = GameRegistry.findUniqueIdentifierFor(gun.getItem()).name;
            String grip = gun.stackTagCompound.getString("gripAttach"), sight = gun.stackTagCompound.getString("sightAttach");
            if (GripRails.needsRail(grip) && !GripRails.fits(name, grip))
            {
                n += takeOff(p, gun, "gripAttach", grip);
            }
            if (!sight.isEmpty() && !SightMounts.fits(name, sight))
            {
                n += takeOff(p, gun, "sightAttach", sight);
            }
        }
        if (n > 0)
        {
            p.inventoryContainer.detectAndSendChanges();
        }
        return n;
    }

    private static int takeOff(EntityPlayerMP p, ItemStack gun, String key, String part)
    {
        gun.stackTagCompound.setString(key, "");
        Item item = GameRegistry.findItem("deci", part);
        if (item != null)
        {
            ItemStack back = new ItemStack(item);
            if (!p.inventory.addItemStackToInventory(back))
            {
                p.dropPlayerItemWithRandomChoice(back, false);
            }
        }
        return 1;
    }
}
