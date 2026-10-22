package net.decimation.fixes;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraftforge.common.DimensionManager;

/**
 * The moon phase from our day counter (DayZ dark nights, user 22 Oktober 2026: "the moon matters"). Vanilla takes the
 * phase from world time (WorldProvider.getMoonPhase: time / 24000 % 8), but Decimation's DayLengthHandler puts world time
 * back to 0 each time it passes 24000 (it stays: Decimation's ambient music and the stereo's clock read fixed hours), so
 * every night had a full moon. The overworld's provider becomes MoonProvider (a WorldProviderSurface, only when vanilla's
 * is still the one registered), whose phase is phaseOf(WorldClock day): day 1 full, day 5 new, eight days round, changing
 * where vanilla's did (world time 0). The client gets the day by message (Net 2) on joining and at each new day. The sky's
 * moon, the light map (fixes/DarkNights through World.getCurrentMoonPhaseFactor) and vanilla's moon rules all follow.
 */
public final class MoonClock implements WorldClock.Listener
{
    /** The day the server last sent (client), 0 before the first. */
    private static volatile int clientDay;

    /** Vanilla's cycle on our days: day 1 phase 0 (full), day 5 phase 4 (new). */
    static int phaseOf(int day)
    {
        return day <= 0 ? 0 : (day - 1) % 8;
    }

    /** The phase of a world now: its saved day on the server, the sent one on a client. */
    static int phase(World world)
    {
        return phaseOf(world == null || world.isRemote ? clientDay : WorldClock.day(world));
    }

    /** The overworld's provider with our moon. */
    public static final class MoonProvider extends WorldProviderSurface
    {
        @Override
        public int getMoonPhase(long worldTime)
        {
            return phase(worldObj);
        }
    }

    /** Both sides, once at preInit: dimension 0 takes MoonProvider unless another mod already replaced vanilla's. */
    @SuppressWarnings("unchecked")
    public static void install()
    {
        try
        {
            Field f = DimensionManager.class.getDeclaredField("providers");
            f.setAccessible(true);
            Class<?> now = ((Hashtable<Integer, Class<?>>) f.get(null)).get(0);
            if (now != WorldProviderSurface.class)
            {
                FMLLog.warning("[deciworldgen] overworld provider is %s, not vanilla's: moon phases stay with it", now);
                return;
            }
        }
        catch (ReflectiveOperationException ex)
        {
            FMLLog.warning("[deciworldgen] moon: cannot read the provider table (%s), left as it is", ex);
            return;
        }
        DimensionManager.unregisterProviderType(0);
        DimensionManager.registerProviderType(0, MoonProvider.class, true);
    }

    /** Server: a new day for everyone online. */
    public void onClock(World world, WorldClock.Event event, int day)
    {
        if (event == WorldClock.Event.DAY)
        {
            sendAll(day);
        }
    }

    /** Server: the day for a player who just joined. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e)
    {
        if (e.player instanceof EntityPlayerMP)
        {
            Net.CHANNEL.sendTo(new Day(WorldClock.day()), (EntityPlayerMP) e.player);
        }
    }

    /** Client: a new server starts from what it says. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e)
    {
        clientDay = 0;
    }

    static void sendAll(int day)
    {
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.getConfigurationManager() == null)
        {
            return;
        }
        List<?> players = s.getConfigurationManager().playerEntityList;
        for (int i = 0; i < players.size(); i++)
        {
            Net.CHANNEL.sendTo(new Day(day), (EntityPlayerMP) players.get(i));
        }
    }

    /** For tests and admins: the overworld's day becomes this one (its moon too), told to everyone online. */
    public static void setDay(int day)
    {
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.worldServers == null || s.worldServers.length == 0 || day < 1)
        {
            return;
        }
        WorldClock.setDay(s.worldServers[0], day);
        sendAll(day);
    }

    /** Server to client: the overworld's day number. */
    public static final class Day implements IMessage
    {
        int day;

        public Day()
        {
        }

        Day(int day)
        {
            this.day = day;
        }

        public void fromBytes(ByteBuf buf)
        {
            day = buf.readInt();
        }

        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(day);
        }
    }

    public static final class DayHandler implements IMessageHandler<Day, IMessage>
    {
        public IMessage onMessage(Day m, MessageContext ctx)
        {
            clientDay = Math.max(0, m.day);
            return null;
        }
    }
}
