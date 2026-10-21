package net.decimation.fixes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"; chosen: about a
 * second like a reload, the rounds as Decimation's loose rounds of that calibre into the inventory, dropped if full, the
 * empty magazine stays). Decimation's own right click on a magazine loads one round (AmmoItem.onItemRightClick, sneaking
 * or not), so the client cancels a sneaking right click with a loaded magazine in hand (no item use is sent) and asks the
 * server through our channel (fixes/Net); the server waits UNLOAD_TICKS with the same magazine in hand, then empties it.
 * Switching away stops it. Server work only on a request and while one runs (a list, empty almost always).
 */
public final class MagUnload
{
    /** About a second, like a reload. */
    static final int UNLOAD_TICKS = 20;
    private static final ConcurrentLinkedQueue<EntityPlayerMP> REQUESTS = new ConcurrentLinkedQueue<EntityPlayerMP>();
    private final List<Running> running = new ArrayList<Running>();

    private static final class Running
    {
        final EntityPlayerMP player;
        final int slot;
        final ItemStack mag;
        int left = UNLOAD_TICKS;

        Running(EntityPlayerMP player, int slot, ItemStack mag)
        {
            this.player = player;
            this.slot = slot;
            this.mag = mag;
        }
    }

    /** Loose round stacks for this many rounds, full stacks first. */
    static int[] stacks(int rounds, int perStack)
    {
        if (rounds <= 0 || perStack <= 0)
        {
            return new int[0];
        }
        int[] out = new int[(rounds + perStack - 1) / perStack];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = Math.min(perStack, rounds - i * perStack);
        }
        return out;
    }

    /** The rounds in a magazine stack, 0 for anything else. */
    static int rounds(ItemStack s)
    {
        return s != null && Deci.magazineCapacity(s.getItem()) > 1 && s.hasTagCompound() ? s.getTagCompound().getInteger("ammo") : 0;
    }

    /** Client: a sneaking right click with a loaded magazine in hand asks the server to unload it. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent e)
    {
        if (!e.entityPlayer.worldObj.isRemote || e.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK || !e.entityPlayer.isSneaking())
        {
            return;
        }
        ItemStack held = e.entityPlayer.getHeldItem();
        if (unloadable(held))
        {
            e.setCanceled(true);
            Net.CHANNEL.sendToServer(new Message());
        }
    }

    /** Server: start requested unloads, finish the ones whose second is up. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        for (EntityPlayerMP p; (p = REQUESTS.poll()) != null; )
        {
            ItemStack held = p.getHeldItem();
            // the client's check again: a request is only a wish, never trusted (a magazine with a round item, loaded)
            if (unloadable(held) && !busy(p))
            {
                running.add(new Running(p, p.inventory.currentItem, held));
                p.worldObj.playSoundAtEntity(p, "deci:bullet.in", 1f, 0.7f);
            }
        }
        for (Iterator<Running> it = running.iterator(); it.hasNext(); )
        {
            Running r = it.next();
            boolean same = !r.player.isDead && r.player.inventory.currentItem == r.slot
                           && r.player.inventory.getStackInSlot(r.slot) == r.mag && unloadable(r.mag);
            if (!same)
            {
                it.remove();                              // switched away, dropped or died: nothing happens
            }
            else if (--r.left <= 0)
            {
                it.remove();
                try
                {
                    unload(r.player, r.mag);
                }
                catch (RuntimeException ex)
                {
                    cpw.mods.fml.common.FMLLog.warning("[deciworldgen] magazine unload failed: %s", ex);   // never out of the tick
                }
            }
        }
    }

    private boolean busy(EntityPlayerMP p)
    {
        for (Running r : running)
        {
            if (r.player == p)
            {
                return true;
            }
        }
        return false;
    }

    /** A loaded magazine whose loose round is known: the only thing either side unloads. */
    static boolean unloadable(ItemStack s)
    {
        return rounds(s) > 0 && Deci.magazineRound(s.getItem()) != null;
    }

    private static void unload(EntityPlayerMP p, ItemStack mag)
    {
        Item round = Deci.magazineRound(mag.getItem());
        if (round == null)
        {
            return;
        }
        int n = rounds(mag);
        mag.getTagCompound().setInteger("ammo", 0);
        for (int k : stacks(n, round.getItemStackLimit()))
        {
            ItemStack s = new ItemStack(round, k);
            if (!p.inventory.addItemStackToInventory(s) || s.stackSize > 0)
            {
                p.dropPlayerItemWithRandomChoice(s, false);   // what did not fit (addItemStackToInventory takes part of it)
            }
        }
        p.worldObj.playSoundAtEntity(p, "deci:bullet.in", 1f, 1.2f);
        p.inventoryContainer.detectAndSendChanges();
    }

    /** "Unload the magazine in my hand" (no fields: the server reads the held item). */
    public static final class Message implements IMessage
    {
        public void fromBytes(ByteBuf buf)
        {
        }

        public void toBytes(ByteBuf buf)
        {
        }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage>
    {
        public IMessage onMessage(Message m, MessageContext ctx)
        {
            REQUESTS.add(ctx.getServerHandler().playerEntity);   // handled on the server tick
            return null;
        }
    }
}
