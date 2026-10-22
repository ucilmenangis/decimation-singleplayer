package net.decimation.fixes;

import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent;

/**
 * Radiation dose (user, 22 Oktober 2026: "radiation status below player status"; chosen: a dose in percent, always on
 * the HUD, Decimation's zone damage kept and the dose fast, a high dose hurts after leaving the zone until it falls back).
 * Decimation only knows "irradiated" (PlayerData, set while the player stands in a RADIATION zone; without a gas mask it
 * hurts about once a second, docs/zones.md). Ours: the dose rises while Decimation's flag is on and no gas mask is worn
 * (MAX in 400 ticks, 20 s), falls otherwise (MAX in 24000 ticks, 20 minutes); from 50 % one damage every 8 s, from 80 %
 * every 4 s, as Decimation's radiation damage. Kept per player in an IExtendedEntityProperties: saved with the player,
 * a new life (respawn) starts at 0. Server work: one pass over the players a tick, a packet to a player only when the
 * percent shown changes (Net message 1); the HUD (fixes/RadiationHud) draws the last percent received.
 */
public final class RadiationDose
{
    /** A full dose; one percent is MAX / 100. */
    static final int MAX = 24000;
    static final int RISE = 60;
    static final int FALL = 1;
    static final String KEY = "deciworldgen_radiation";

    /** The local player's dose in percent, as the server last sent it (client). */
    private static volatile int shown;

    /** The dose a tick later. */
    static int next(int dose, boolean exposed)
    {
        return exposed ? Math.min(MAX, dose + RISE) : Math.max(0, dose - FALL);
    }

    /** The percent shown, rounded down. */
    static int percent(int dose)
    {
        return dose * 100 / MAX;
    }

    /** Ticks between two damage points at this percent, 0 for none. */
    static int hurtEvery(int percent)
    {
        return percent >= 80 ? 80 : percent >= 50 ? 160 : 0;
    }

    /** The local player's dose in percent (client, for the HUD). */
    public static int shown()
    {
        return shown;
    }

    /** A player's dose (server): what the save keeps. */
    static final class Dose implements IExtendedEntityProperties
    {
        int dose;
        int sent = -1;                                    // percent the client has; -1: nothing yet (join, respawn)

        public void saveNBTData(NBTTagCompound tag)
        {
            tag.setInteger(KEY, dose);
        }

        public void loadNBTData(NBTTagCompound tag)
        {
            dose = Math.max(0, Math.min(MAX, tag.getInteger(KEY)));
        }

        public void init(Entity entity, World world)
        {
        }
    }

    @SubscribeEvent
    public void onConstructing(EntityEvent.EntityConstructing e)
    {
        if (e.entity instanceof EntityPlayerMP)
        {
            e.entity.registerExtendedProperties(KEY, new Dose());
        }
    }

    /** Server: every player's dose a tick; damage at a high dose; the client told when its percent changes. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null)
        {
            return;
        }
        long start = System.nanoTime();
        List<?> players = server.getConfigurationManager().playerEntityList;
        for (int i = 0; i < players.size(); i++)
        {
            EntityPlayerMP p = (EntityPlayerMP) players.get(i);
            Dose d = (Dose) p.getExtendedProperties(KEY);
            if (d == null || p.isDead)
            {
                continue;
            }
            boolean creative = p.capabilities.isCreativeMode;
            d.dose = next(d.dose, !creative && Deci.irradiated(p) && !Deci.wearsGasMask(p));
            int pct = percent(d.dose);
            if (pct != d.sent)
            {
                d.sent = pct;
                Net.CHANNEL.sendTo(new Sync(pct), p);
            }
            int every = hurtEvery(pct);
            if (every > 0 && !creative && p.ticksExisted % every == 0)
            {
                p.attackEntityFrom(Deci.irradiatedDamage(), 1f);
            }
        }
        tickRuns++;
        tickNanos += System.nanoTime() - start;
    }

    /** For the dev test: server ticks run and their time in nanoseconds. */
    static long tickRuns, tickNanos;

    /** For the dev test: average time of our server tick in microseconds. */
    public static double averageTickMicros()
    {
        return tickRuns == 0 ? 0 : tickNanos / 1000.0 / tickRuns;
    }

    /** Client: a new world starts from what its server says, nothing from the last one. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e)
    {
        shown = 0;
    }

    /** Server to client: the local player's dose in percent. */
    public static final class Sync implements IMessage
    {
        int percent;

        public Sync()
        {
        }

        Sync(int percent)
        {
            this.percent = percent;
        }

        public void fromBytes(ByteBuf buf)
        {
            percent = buf.readUnsignedByte();
        }

        public void toBytes(ByteBuf buf)
        {
            buf.writeByte(percent);
        }
    }

    public static final class SyncHandler implements IMessageHandler<Sync, IMessage>
    {
        public IMessage onMessage(Sync m, MessageContext ctx)
        {
            shown = Math.max(0, Math.min(100, m.percent));
            return null;
        }
    }

    /** For tests: the server side dose of a player (MAX is 100 %), or -1 without one. */
    public static int doseOf(EntityPlayer p)
    {
        Dose d = (Dose) p.getExtendedProperties(KEY);
        return d == null ? -1 : d.dose;
    }

    /** For tests: set a player's dose. */
    public static void setDose(EntityPlayer p, int dose)
    {
        Dose d = (Dose) p.getExtendedProperties(KEY);
        if (d != null)
        {
            d.dose = Math.max(0, Math.min(MAX, dose));
        }
    }

    /** A full dose, for tests (MAX). */
    public static int max()
    {
        return MAX;
    }
}
