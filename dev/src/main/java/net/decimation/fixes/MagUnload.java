package net.decimation.fixes;

import java.util.ArrayList;
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
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Unloading a magazine (user, 22 Oktober 2026: "mags now can be unload by click shift + right click"; then "make it same
 * like load ammo process code ... its reverse"): Decimation's loading (AmmoItem.onItemRightClick) backwards. Loading is
 * one round a right click (held, the click repeats every 4 ticks), the "deci:bullet.in" sound and one loose round taken
 * from the inventory, nothing of it in creative but the round; unloading is one round a sneaking right click, the same
 * sound and one loose round given back (on the ground when the inventory is full), only the round in creative. A stack
 * of magazines is refused with the same kind of message, an empty one says so (Decimation: "This Magazine is full!").
 * The client cancels a sneaking right click with a magazine in hand (Decimation would load a round) and asks the server
 * through our channel (fixes/Net); the server checks again and does one step, at most one a player a tick.
 */
public final class MagUnload
{
    private static final ConcurrentLinkedQueue<EntityPlayerMP> REQUESTS = new ConcurrentLinkedQueue<EntityPlayerMP>();
    private final List<EntityPlayerMP> done = new ArrayList<EntityPlayerMP>();

    /** What one sneaking right click does, in the order Decimation's loading checks its cases. */
    enum Step { UNLOAD, MANY, EMPTY }

    static Step step(int rounds, int stackSize)
    {
        if (rounds > 0 && stackSize == 1)
        {
            return Step.UNLOAD;
        }
        return stackSize > 1 ? Step.MANY : Step.EMPTY;
    }

    /** The chat line of a step, worded like Decimation's loading lines; null when the round comes out. */
    static String message(Step s, int stackSize)
    {
        switch (s)
        {
            case MANY: return "You can only unload 1 magazine at a time! Not " + stackSize + "!";
            case EMPTY: return "This Magazine is empty!";
            default: return null;
        }
    }

    /** A magazine Decimation loads by hand: its loose round is known and it carries its rounds tag. */
    static boolean magazine(ItemStack s)
    {
        return s != null && Deci.magazineCapacity(s.getItem()) > 0 && Deci.magazineRound(s.getItem()) != null && s.hasTagCompound();
    }

    /** Client: a sneaking right click with a magazine in hand unloads instead of loading. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent e)
    {
        if (!e.entityPlayer.worldObj.isRemote || e.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK || !e.entityPlayer.isSneaking())
        {
            return;
        }
        if (magazine(e.entityPlayer.getHeldItem()))
        {
            e.setCanceled(true);
            Net.CHANNEL.sendToServer(new Message());
        }
    }

    /** Server: one step for each player who asked this tick. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || REQUESTS.isEmpty())
        {
            return;
        }
        for (EntityPlayerMP p; (p = REQUESTS.poll()) != null; )
        {
            if (p.isDead || done.contains(p))
            {
                continue;                                   // a request is only a wish: one step a tick, like a held click
            }
            done.add(p);
            try
            {
                unloadOne(p);
            }
            catch (RuntimeException ex)
            {
                cpw.mods.fml.common.FMLLog.warning("[deciworldgen] magazine unload failed: %s", ex);   // never out of the tick
            }
        }
        done.clear();
    }

    private static void unloadOne(EntityPlayerMP p)
    {
        ItemStack mag = p.getHeldItem();
        if (!magazine(mag))
        {
            return;                                         // the client's check again: never trusted
        }
        Step s = step(mag.getTagCompound().getInteger("ammo"), mag.stackSize);
        if (s != Step.UNLOAD)
        {
            p.addChatMessage(new ChatComponentText(message(s, mag.stackSize)));
            return;
        }
        Item round = Deci.magazineRound(mag.getItem());
        mag.getTagCompound().setInteger("ammo", mag.getTagCompound().getInteger("ammo") - 1);
        if (!p.capabilities.isCreativeMode)
        {
            p.worldObj.playSoundAtEntity(p, "deci:bullet.in", 1f, 1f);
            ItemStack r = new ItemStack(round, 1);
            if (!p.inventory.addItemStackToInventory(r) || r.stackSize > 0)
            {
                p.dropPlayerItemWithRandomChoice(r, false);
            }
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    /** "Take one round out of the magazine in my hand" (no fields: the server reads the held item). */
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
