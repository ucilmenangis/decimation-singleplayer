package net.decimation.fixes;

import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

/**
 * Singleplayer fix: supply drops never happened.
 *
 * On a dedicated server the server tick handler (obfuscated
 * {@code deci.aK.q}) counts {@code ServerConfig.supplyDropCountdown} down from
 * {@code supplyDropInterval} (18000 ticks, 15 minutes), warns in chat at 10
 * and 5 minutes, then drops the supply crate at a random point from the
 * admin's {@code decimation_supplydrop.properties} list. That handler is
 * ServerProxy-only and every SupplyDropSpawner method checks isServer(), so
 * none of it runs in singleplayer, where no such list exists either.
 *
 * This runs the same timer and messages in singleplayer and drops the crate
 * (the mod's own falling supply drop block, looted through the normal loot
 * system) at a random loaded spot 64 to 144 blocks from a random player,
 * announcing the coordinates like the original. Honours
 * {@code ServerConfig.enableSupplyDrops}.
 */
public class SupplyDropScheduler
{
    private static final int MIN_DISTANCE = 64;
    private static final int MAX_DISTANCE = 144;
    private final Random random = new Random();

    /** Last drop column, for the dev autotest. */
    public static int lastDropX = Integer.MIN_VALUE, lastDropZ = Integer.MIN_VALUE;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END
            || FMLCommonHandler.instance().getSide().isServer()
            || !deci.aJ.b.aAi) // enableSupplyDrops
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null)
        {
            return;
        }
        int countdown = deci.aJ.b.aAg; // supplyDropCountdown
        if (countdown == 12000)
        {
            announce(EnumChatFormatting.GRAY + "A supply crate will drop in "
                     + EnumChatFormatting.GREEN + "10" + EnumChatFormatting.GRAY + " minutes!");
        }
        if (countdown == 6000)
        {
            announce(EnumChatFormatting.GRAY + "A supply crate will drop in "
                     + EnumChatFormatting.GREEN + "5" + EnumChatFormatting.GRAY + " minutes!");
        }
        if (countdown > 0)
        {
            deci.aJ.b.aAg = countdown - 1;
            return;
        }
        deci.aJ.b.aAg = deci.aJ.b.aAf; // reset to supplyDropInterval
        try
        {
            drop(server);
        }
        catch (Throwable t)
        {
            FMLLog.info("[deciworldgen] supply drop failed: %s", t);
        }
    }

    private void drop(MinecraftServer server)
    {
        List<?> players = server.getConfigurationManager().playerEntityList;
        if (players.isEmpty())
        {
            return;
        }
        EntityPlayer player = (EntityPlayer) players.get(random.nextInt(players.size()));
        if (player.dimension != 0)
        {
            return; // overworld only, like the original
        }
        WorldServer world = server.worldServerForDimension(0);
        for (int attempt = 0; attempt < 12; attempt++)
        {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = (int) Math.floor(player.posX + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.posZ + Math.sin(angle) * dist);
            if (!world.blockExists(x, 64, z))
            {
                continue; // only loaded columns: never force chunk generation
            }
            world.setBlock(x, world.getHeight() - 1, z, deci.aD.c.afA); // BlockRegistry.supplyDrop
            world.playSoundEffect(player.posX, player.posY, player.posZ,
                                  "deci:item.supplydropradio.radio", 3.0F, 1.0F);
            announce(EnumChatFormatting.GRAY + "A supply drop has been deployed at "
                     + EnumChatFormatting.GREEN + x + EnumChatFormatting.GRAY + "/"
                     + EnumChatFormatting.GREEN + z + EnumChatFormatting.GRAY + "!");
            lastDropX = x;
            lastDropZ = z;
            return;
        }
    }

    private static void announce(String text)
    {
        MinecraftServer.getServer().getConfigurationManager()
            .sendChatMsg(new ChatComponentText(text));
    }
}
