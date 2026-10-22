package net.decimation.fixes;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Gun durability (feature 10, user 22 Oktober 2026: "guns now has durabitlity and they need to be repaired by guns part";
 * chosen: 1 a shot, the odd jam below 25 % left cleared by racking, no firing at 0, a part repairs a quarter). The gun's
 * NBT holds the rounds fired (WEAR) and a jam (JAM); the maximum comes from its kind (Durability.gunMax: launchers and
 * crossbows never wear). Decimation's GunItem calls in through tools/patches/PatchGunWear.java (three jars): fire() asks
 * blocked() first (client: a jammed or used up gun clicks and does not fire), onServerFire() calls onShot() (server,
 * once a shot: the wear and the jam roll). Racking: R with a jammed gun in hand (GunWearClient) plays Decimation's Rack
 * animation and sends Rack (Net message 3); the server clears the jam of the gun in hand, checking it again.
 */
public final class GunWear
{
    public static final String WEAR = "deciworldgen_wear";
    public static final String JAM = "deciworldgen_jammed";
    private static final Random RANDOM = new Random();
    private static final ConcurrentLinkedQueue<EntityPlayerMP> RACKS = new ConcurrentLinkedQueue<EntityPlayerMP>();

    /** Rounds this gun has fired (its wear). */
    static int wear(NBTTagCompound tag)
    {
        return tag == null ? 0 : tag.getInteger(WEAR);
    }

    static boolean jammed(NBTTagCompound tag)
    {
        return tag != null && tag.getBoolean(JAM);
    }

    /** Whether a gun with this NBT may fire (max 0: a gun that does not wear). */
    static boolean fires(NBTTagCompound tag, int max)
    {
        return max <= 0 || !jammed(tag) && !Durability.broken(wear(tag), max);
    }

    /** One shot: wear 1 (up to the maximum) and roll for a jam; true when it jammed now. */
    static boolean shot(NBTTagCompound tag, int max, double roll)
    {
        int w = Math.min(max, wear(tag) + 1);
        tag.setInteger(WEAR, w);
        if (Durability.jamZone(w, max) && roll < Durability.JAM_CHANCE)
        {
            tag.setBoolean(JAM, true);
            return true;
        }
        return false;
    }

    static void clear(NBTTagCompound tag)
    {
        tag.removeTag(JAM);
    }

    /** Gun parts: a quarter of the maximum off the wear. */
    static void repair(NBTTagCompound tag, int max)
    {
        tag.setInteger(WEAR, Durability.repaired(wear(tag), max));
    }

    /** The maximum of this gun (0: none). */
    public static int max(ItemStack gun)
    {
        return gun == null || !Deci.isGun(gun.getItem()) ? 0 : Durability.gunMax(Deci.gunCategory(gun.getItem()));
    }

    /** Called by GunItem.fire (client) before anything: true = this shot does not happen. */
    public static boolean blocked(ItemStack gun, EntityPlayer player)
    {
        int max = max(gun);
        if (max <= 0)
        {
            return false;
        }
        boolean jam = jammed(gun.stackTagCompound), used = Durability.broken(wear(gun.stackTagCompound), max);
        if (!jam && !used && !GunWearClient.racking())
        {
            return false;
        }
        GunWearClient.click(player, jam, used);
        return true;
    }

    /** For the dev test: shots seen on the server and their time in nanoseconds. */
    static long shots, shotNanos;

    /** For the dev test: average time of onShot in microseconds, and the count. */
    public static double averageMicros()
    {
        return shots == 0 ? 0 : shotNanos / 1000.0 / shots;
    }

    public static long shotCount()
    {
        return shots;
    }

    /** Called by GunItem.onServerFire (server) once a shot. */
    public static void onShot(ItemStack gun, EntityPlayer player)
    {
        long start = System.nanoTime();
        shot(gun, player);
        shots++;
        shotNanos += System.nanoTime() - start;
    }

    private static void shot(ItemStack gun, EntityPlayer player)
    {
        int max = max(gun);
        if (max <= 0 || gun.stackTagCompound == null)
        {
            return;
        }
        if (shot(gun.stackTagCompound, max, RANDOM.nextDouble()))
        {
            player.worldObj.playSoundAtEntity(player, "deci:item.gun.empty", 0.6f, 0.7f);
        }
    }

    /** Server: clear the jams asked for, on the server tick. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        for (EntityPlayerMP p; (p = RACKS.poll()) != null; )
        {
            ItemStack gun = p.getHeldItem();
            if (!p.isDead && max(gun) > 0 && jammed(gun.stackTagCompound))
            {
                clear(gun.stackTagCompound);
                p.inventoryContainer.detectAndSendChanges();
            }
        }
    }

    /** "Rack the gun in my hand" (no fields: the server reads the held gun). */
    public static final class Rack implements IMessage
    {
        public void fromBytes(ByteBuf buf)
        {
        }

        public void toBytes(ByteBuf buf)
        {
        }
    }

    public static final class RackHandler implements IMessageHandler<Rack, IMessage>
    {
        public IMessage onMessage(Rack m, MessageContext ctx)
        {
            RACKS.add(ctx.getServerHandler().playerEntity);
            return null;
        }
    }
}
