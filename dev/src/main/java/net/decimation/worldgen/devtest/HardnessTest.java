package net.decimation.worldgen.devtest;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.decimation.fixes.PropHardness;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * bug.md "Some props still break with one punch" (user, 22 Oktober 2026): every block of Decimation and ours is set
 * in the arena one at a time and asked what a survival player with an empty hand gets from one click, the same call
 * ItemInWorldManager.onBlockClicked makes (getPlayerRelativeBlockHardness: 1 or more breaks the block on that click).
 * The table goes to run/client/devtest/hardness.tsv (name, class, material, hardness, one click share, broken by one
 * click). Check: no block breaks with one click unless PropHardness.MEANT_TO_BREAK lists it (glass, plants and
 * the like).
 */
public class HardnessTest extends DevTestMode
{
    public String name() { return "hardness"; }

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
        if (ticks == 1)
        {
            DevTestArena.build(w, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
            p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
            return;
        }
        if (ticks < 20) // standing on the floor: a hand in the air breaks 5 times slower
        {
            return;
        }
        int x = DevTestArena.X + 6, y = DevTestArena.Y + 2, z = DevTestArena.Z + 6;
        Map<String, String> rows = new TreeMap<String, String>();
        List<String> oneClick = new ArrayList<String>();
        int count = 0;
        for (Object o : Block.blockRegistry.getKeys())
        {
            String key = (String) o;
            if (!key.startsWith("deci:") && !key.startsWith("deciworldgen:"))
            {
                continue;
            }
            Block b = (Block) Block.blockRegistry.getObject(key);
            if (b == null || b == Blocks.air)
            {
                continue;
            }
            count++;
            String hard, share, cls = b.getClass().getName(), mat = material(b);
            boolean instant;
            try
            {
                w.setBlock(x, y, z, b, 0, 2);
                float h = b.getBlockHardness(w, x, y, z);
                float s = b.getPlayerRelativeBlockHardness(p, w, x, y, z);
                hard = String.valueOf(h);
                share = String.valueOf(s);
                instant = s >= 1f;
            }
            catch (Throwable t)
            {
                hard = "?";
                share = t.getClass().getSimpleName();
                instant = false;
            }
            finally
            {
                w.removeTileEntity(x, y, z);
                w.setBlock(x, y, z, Blocks.air, 0, 2);
            }
            boolean meant = PropHardness.meantToBreak(key);
            if (instant && !meant)
            {
                oneClick.add(key.replace("deci:", ""));
            }
            rows.put(key, key + "\t" + cls + "\t" + mat + "\t" + hard + "\t" + share + "\t" + (instant ? meant ? "yes, meant" : "YES" : "no"));
        }
        File out = new File(Minecraft.getMinecraft().mcDataDir, "devtest/hardness.tsv");
        try
        {
            PrintWriter pw = new PrintWriter(out, "UTF-8");
            pw.println("name\tclass\tmaterial\thardness\tone_click_share\tbroken_by_one_click");
            for (String r : rows.values())
            {
                pw.println(r);
            }
            pw.close();
        }
        catch (Exception e)
        {
            DevTestResults.value(name(), "write", e.toString());
        }
        DevTestResults.value(name(), "blocks", count + " in " + out);
        DevTestResults.check(name(), "blocks broken by one click of a survival hand (not meant to)",
                             oneClick.size() + (oneClick.isEmpty() ? "" : ": " + String.join(" ", oneClick)),
                             oneClick.isEmpty(), "0");
        done = true;
    }

    private static String material(Block b)
    {
        net.minecraft.block.material.Material m = b.getMaterial();
        if (m == net.minecraft.block.material.Material.glass) return "glass";
        if (m == net.minecraft.block.material.Material.plants) return "plants";
        if (m == net.minecraft.block.material.Material.vine) return "vine";
        if (m == net.minecraft.block.material.Material.circuits) return "circuits";
        if (m == net.minecraft.block.material.Material.iron) return "iron";
        if (m == net.minecraft.block.material.Material.rock) return "rock";
        if (m == net.minecraft.block.material.Material.wood) return "wood";
        if (m == net.minecraft.block.material.Material.cloth) return "cloth";
        if (m == net.minecraft.block.material.Material.carpet) return "carpet";
        if (m == net.minecraft.block.material.Material.web) return "web";
        if (m == net.minecraft.block.material.Material.anvil) return "anvil";
        if (m == net.minecraft.block.material.Material.ground) return "ground";
        if (m == net.minecraft.block.material.Material.sand) return "sand";
        if (m == net.minecraft.block.material.Material.water) return "water";
        if (m == net.minecraft.block.material.Material.air) return "air";
        return m.toString();
    }
}
