package net.decimation.worldgen;

import net.decimation.worldgen.building.Building;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/**
 * /deciworldgen reload : re-reads furniture sets, palettes and styles from
 * config/decimation_worldgen/ (newly generated chunks and rebuilds use them).
 * rebuild [radius] [x z] : regenerates nearby city buildings in place.
 * pos1 | pos2 [x y z] : capture corners (default: the block at your feet).
 * capture set NAME ROOM [north|south|west|east] [weight] : saves the box as
 * a furniture set (docs/furniture_sets.md "Capture") and reloads.
 * capture part NAME : saves the box as a raw part (for the part planner).
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
        return "/deciworldgen reload | rebuild [radius] [x z] | pos1|pos2 [x y z]"
               + " | capture set <name> <room> [north|south|west|east] [weight] | capture part <name>";
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
        if (args.length >= 1 && ("pos1".equals(args[0]) || "pos2".equals(args[0])))
        {
            int x, y, z;
            if (args.length >= 4)
            {
                x = parseInt(sender, args[1]);
                y = parseInt(sender, args[2]);
                z = parseInt(sender, args[3]);
            }
            else
            {
                net.minecraft.entity.player.EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                x = (int) Math.floor(player.posX);
                y = (int) Math.floor(player.boundingBox.minY + 0.01);
                z = (int) Math.floor(player.posZ);
            }
            net.decimation.worldgen.assets.Capture.setPos(sender.getCommandSenderName(), "pos1".equals(args[0]) ? 1 : 2,
                                                          x, y, z);
            sender.addChatMessage(new ChatComponentText("[deciworldgen] " + args[0] + " = " + x + " " + y + " " + z));
            return;
        }
        if (args.length >= 3 && "capture".equals(args[0]))
        {
            capture(sender, args);
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

    private void capture(ICommandSender sender, String[] args)
    {
        int[] box = net.decimation.worldgen.assets.Capture.box(sender.getCommandSenderName());
        String name = args[2];
        if (box == null)
        {
            sender.addChatMessage(new ChatComponentText("[deciworldgen] mark both corners first: /deciworldgen pos1, pos2"));
            return;
        }
        if (!name.matches("[a-z0-9_]+"))
        {
            sender.addChatMessage(new ChatComponentText("[deciworldgen] name: lower case letters, digits, _ only"));
            return;
        }
        java.io.File sets = net.decimation.worldgen.sets.FurnitureSets.SETS.dir();
        net.minecraft.world.World world = sender.getEntityWorld();
        try
        {
            String report;
            if ("set".equals(args[1]) && args.length >= 4)
            {
                String side = args.length >= 5 && !args[4].matches("\\d+") ? args[4] : null;
                int weight = 10;
                String last = args[args.length - 1];
                if (args.length >= 5 && last.matches("\\d+"))
                {
                    weight = Integer.parseInt(last);
                }
                report = net.decimation.worldgen.assets.Capture.captureSet(world, box, name, args[3], side, weight, sets);
                report += "; " + net.decimation.worldgen.sets.FurnitureSets.reload()
                          + ". /deciworldgen rebuild to see it in the city";
            }
            else if ("part".equals(args[1]))
            {
                report = net.decimation.worldgen.assets.Capture.capturePart(world, box, name,
                    new java.io.File(sets.getParentFile(), "parts"));
            }
            else
            {
                report = getCommandUsage(sender);
            }
            sender.addChatMessage(new ChatComponentText("[deciworldgen] " + report));
        }
        catch (IllegalArgumentException e)
        {
            sender.addChatMessage(new ChatComponentText("[deciworldgen] " + e.getMessage()));
        }
        catch (Exception e)
        {
            sender.addChatMessage(new ChatComponentText("[deciworldgen] capture failed: " + e));
        }
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
