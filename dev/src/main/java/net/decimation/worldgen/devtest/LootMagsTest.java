package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.List;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Looted magazines hold a random number of rounds (user, 22 Oktober 2026: "now all mag loot have variaty capacity ammo
 * inside it"; chosen: 1 round to full, weighted toward half full, rarely full, never empty). Decimation's weapon cabinet
 * (its guns and magazines list) on the arena, opened by real right clicks through its own LootInteractHandler, the
 * cooldown cleared between clicks; every magazine the server rolled is read: its rounds (NBT ammo; a magazine without it
 * fills itself to capacity on its first tick in an inventory, AmmoItem.onUpdate) against its capacity.
 */
public class LootMagsTest extends DevTestMode
{
    public String name() { return "lootmags"; }

    private static final int CLICKS = 40;
    private int ticks, clicks;
    private volatile boolean placed;
    private volatile Runnable onServer;
    private int crateX, crateY, crateZ;
    private final List<int[]> mags = new ArrayList<int[]>();      // {rounds or -1 for no NBT, capacity}
    private net.minecraft.world.WorldSettings.GameType modeBefore;

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        ticks++;
        if (ticks == 2)
        {
            onServer = this::place;
        }
        if (!placed)
        {
            return ticks < 200;
        }
        int t = ticks % 20;
        if (clicks < CLICKS)
        {
            if (t == 0)
            {
                onServer = Deci::clearLootCooldowns;
            }
            if (t == 3)
            {
                mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), crateX, crateY, crateZ, 1,
                    net.minecraft.util.Vec3.createVectorHelper(crateX + 0.5, crateY + 1.0, crateZ + 0.5));
            }
            if (t == 12)
            {
                onServer = this::read;
            }
            if (t == 16)
            {
                mc.thePlayer.closeScreen();
                clicks++;
            }
            return true;
        }
        if (t == 0)
        {
            onServer = this::cleanup;
            report();
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            return false;
        }
        return true;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    private void place()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        String pick = null;
        for (String n : Deci.lootTable().keySet())
        {
            if (n.startsWith("deci:") && n.toLowerCase().contains("cabinet"))
            {
                pick = n;
            }
        }
        net.minecraft.block.Block b = pick == null ? null : net.minecraft.block.Block.getBlockFromName(pick);
        DevTestResults.value(name(), "loot block", pick == null ? "none" : pick);
        if (b == null)
        {
            return;
        }
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);   // the handler skips creative players
        crateX = DevTestArena.X;
        crateY = DevTestArena.Y;
        crateZ = DevTestArena.Z - 2;
        w.setBlock(crateX, crateY, crateZ, b, 0, 3);
        Deci.clearLootCooldowns();
        placed = true;
    }

    private void read()
    {
        InventoryBasic inv = Deci.lootInventory(DevTestUtil.player());
        for (int i = 0; inv != null && i < inv.getSizeInventory(); i++)
        {
            ItemStack s = inv.getStackInSlot(i);
            int cap = s == null ? 0 : Deci.magazineCapacity(s.getItem());
            if (cap > 1)
            {
                mags.add(new int[] {s.hasTagCompound() && s.getTagCompound().hasKey("ammo") ? s.getTagCompound().getInteger("ammo") : -1, cap});
            }
        }
    }

    private void cleanup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        p.worldObj.setBlockToAir(crateX, crateY, crateZ);
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);
        }
    }

    private void report()
    {
        int bad = 0, full = 0;
        double share = 0;
        for (int[] m : mags)
        {
            int rounds = m[0] < 0 ? m[1] : m[0];                  // no NBT: it fills itself to capacity
            bad += rounds < 1 || rounds > m[1] ? 1 : 0;
            full += rounds == m[1] ? 1 : 0;
            share += rounds / (double) m[1];
        }
        int n = mags.size();
        DevTestResults.value(name(), "magazines rolled", n + " in " + CLICKS + " opens");
        DevTestResults.check(name(), "every magazine 1 round to full", bad + " outside of " + n, n >= 5 && bad == 0, "0 outside, 5 or more magazines");
        DevTestResults.check(name(), "magazines mostly partial", String.format("%d full of %d, mean %.0f %% full", full, n, n == 0 ? 0 : 100 * share / n),
                             n >= 5 && full * 5 <= n && share / n > 0.3 && share / n < 0.7, "at most 1 in 5 full, mean 30 to 70 %");
    }
}
