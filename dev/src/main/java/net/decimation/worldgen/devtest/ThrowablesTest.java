package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * bug.md "The last grenade never runs out" (user, 19 Oktober 2026): a throw takes one off the held stack but leaves a
 * stack of 0 in the hand, which throws again (and again). Through the server's throw path (as the throw packet does):
 * three grenades leave two after one throw; the last one leaves an empty hand and a second throw spawns nothing.
 */
public class ThrowablesTest extends DevTestMode
{
    public String name() { return "throwables"; }

    private int ticks;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        return !done;
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
        ticks++;
        Item frag = (Item) Item.itemRegistry.getObject("deci:ItemFRAGGrenade");
        if (ticks == 1)
        {
            DevTestArena.build(w, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL); // creative throws take nothing
            p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
            hold(p, new ItemStack(frag, 3));
        }
        else if (ticks == 5)
        {
            int before = grenades(w);
            Deci.throwHeld(p, 1.5);
            DevTestResults.value(name(), "grenades spawned by a throw from 3", grenades(w) - before);
            clear(w);
        }
        else if (ticks == 8)
        {
            ItemStack h = p.getHeldItem();
            DevTestResults.check(name(), "three grenades, one thrown", h == null ? "empty" : String.valueOf(h.stackSize),
                                 h != null && h.stackSize == 2, "2 left");
            hold(p, new ItemStack(frag, 1));
        }
        else if (ticks == 11)
        {
            Deci.throwHeld(p, 1.5);
            clear(w);
        }
        else if (ticks == 14)
        {
            ItemStack h = p.getHeldItem();
            DevTestResults.check(name(), "the last grenade thrown leaves an empty hand", h == null ? "empty" : h.stackSize + " held",
                                 h == null, "empty");
            int before = grenades(w);
            Deci.throwHeld(p, 1.5);
            int spawned = grenades(w) - before;
            DevTestResults.check(name(), "a throw with the last one gone", spawned + " grenade(s)", spawned == 0, "0 grenades");
            clear(w);
            hold(p, new ItemStack((Item) Item.itemRegistry.getObject("deci:ItemMolotov"), 1));
        }
        else if (ticks == 17 || ticks == 23)
        {
            Deci.throwHeld(p, 1.5);
        }
        else if (ticks == 20)
        {
            ItemStack h = p.getHeldItem();
            DevTestResults.check(name(), "the last molotov thrown leaves an empty hand", h == null ? "empty" : h.stackSize + " held",
                                 h == null, "empty");
            hold(p, new ItemStack((Item) Item.itemRegistry.getObject("deci:ItemSmokeGrenade"), 1));
        }
        else if (ticks == 26)
        {
            ItemStack h = p.getHeldItem();
            DevTestResults.check(name(), "the last smoke grenade thrown leaves an empty hand", h == null ? "empty" : h.stackSize + " held",
                                 h == null, "empty");
            hold(p, null);
            done = true;
        }
    }

    private static void hold(EntityPlayerMP p, ItemStack s)
    {
        p.inventory.setInventorySlotContents(p.inventory.currentItem, s);
        p.inventoryContainer.detectAndSendChanges();
    }

    private static int grenades(WorldServer w)
    {
        int n = 0;
        for (Object o : w.loadedEntityList)
        {
            if (Deci.isGrenade((Entity) o) && !((Entity) o).isDead)
            {
                n++;
            }
        }
        return n;
    }

    /** No explosions in the arena. */
    private static void clear(WorldServer w)
    {
        for (Object o : new java.util.ArrayList<Object>(w.loadedEntityList))
        {
            if (Deci.isGrenade((Entity) o))
            {
                ((Entity) o).setDead();
            }
        }
    }
}
