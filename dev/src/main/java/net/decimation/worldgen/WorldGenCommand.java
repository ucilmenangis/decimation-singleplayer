package net.decimation.worldgen;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/**
 * /deciworldgen reload : re-reads the furniture sets from
 * config/decimation_worldgen/sets/ (newly generated chunks use them).
 */
public class WorldGenCommand extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "deciworldgen";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/deciworldgen reload | rebuild [radius] [x z]";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length >= 1 && "rebuild".equals(args[0]))
        {
            int radius = args.length > 1 ? parseIntBounded(sender, args[1], 1, 256) : 32;
            if (args.length >= 4)
            {
                // console / command block: explicit centre
                rebuild(sender, net.minecraft.server.MinecraftServer.getServer().worldServerForDimension(0),
                        parseInt(sender, args[2]), parseInt(sender, args[3]), radius);
            }
            else
            {
                net.minecraft.entity.player.EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                rebuild(sender, player.worldObj, (int) Math.floor(player.posX), (int) Math.floor(player.posZ), radius);
            }
            return;
        }
        if (args.length == 1 && "reload".equals(args[0]))
        {
            String report = net.decimation.worldgen.sets.FurnitureSets.reload();
            sender.addChatMessage(new ChatComponentText("[deciworldgen] " + report
                + " (newly generated chunks use them)"));
            return;
        }
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    /**
     * Rebuilds every city building whose lot lies within radius blocks of the
     * player, with the current code and sets, in place (no new world needed).
     */
    private void rebuild(ICommandSender sender, net.minecraft.world.World world, int px, int pz, int radius)
    {
        int done = 0, skipped = 0;
        for (int cx = Math.floorDiv(px - radius, 64); cx <= Math.floorDiv(px + radius, 64); cx++)
        {
            for (int cz = Math.floorDiv(pz - radius, 64); cz <= Math.floorDiv(pz + radius, 64); cz++)
            {
                if (Sectors.sector(world.getSeed(), cx * 4, cz * 4) != StructureGenerator.CITY)
                {
                    continue;
                }
                for (Building b : DecimationWorldGen.city.plan(world, cx, cz))
                {
                    if (b.lotMaxX() < px - radius || b.lotMinX() > px + radius
                        || b.lotMaxZ() < pz - radius || b.lotMinZ() > pz + radius)
                    {
                        continue;
                    }
                    if (Slices.rebuild(world, b))
                    {
                        done++;
                    }
                    else
                    {
                        skipped++;
                    }
                }
            }
        }
        sender.addChatMessage(new ChatComponentText("[deciworldgen] rebuilt " + done + " building(s) within "
            + radius + " blocks" + (skipped > 0 ? ", " + skipped + " not placed yet" : "")));
    }
}
