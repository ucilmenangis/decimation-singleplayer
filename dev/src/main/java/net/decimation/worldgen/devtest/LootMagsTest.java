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
 * Guns too (user, same day: "some guns loot have ammo inside it, so not all of them is empty"; chosen: 1 in 2, rounds
 * like a magazine's): the share loaded, rounds within the gun's magazine, Decimation's own tag on them; then one loaded
 * gun goes into the hand and fires a shot the way the mouse does (Deci.fireHeldGun): the server copy loses a round.
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
    private final List<int[]> guns = new ArrayList<int[]>();      // {rounds, magazine size, has Decimation's tag (1/0)}
    private ItemStack loadedGun;
    private int fireTicks, before = -1, after = -1;

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
                onServer = net.decimation.fixes.LootRefill::clearAll;
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
        if (loadedGun != null && fireTicks < 60)
        {
            fireTicks++;
            if (fireTicks == 1)
            {
                onServer = this::holdGun;
            }
            if (fireTicks == 25)
            {
                Deci.fireHeldGun(mc.thePlayer);
            }
            if (fireTicks == 50)
            {
                onServer = () -> after = Deci.gunAmmo(DevTestUtil.player().getHeldItem());
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
        net.decimation.fixes.LootRefill.clearAll();
        placed = true;
    }

    private void read()
    {
        InventoryBasic inv = Deci.lootInventory(DevTestUtil.player());
        for (int i = 0; inv != null && i < inv.getSizeInventory(); i++)
        {
            ItemStack s = inv.getStackInSlot(i);
            int cap = s == null ? 0 : Deci.magazineCapacity(s.getItem());
            if (s != null && Deci.isGun(s.getItem()))
            {
                int k = Deci.gunAmmo(s);
                guns.add(new int[] {k, Deci.gunMagazine(s), s.hasTagCompound() && s.getTagCompound().hasKey("bolted") ? 1 : 0});
                if (k > 1 && loadedGun == null && Deci.gunIsAutomatic(s))
                {
                    loadedGun = s.copy();
                }
            }
            if (cap > 1)
            {
                mags.add(new int[] {s.hasTagCompound() && s.getTagCompound().hasKey("ammo") ? s.getTagCompound().getInteger("ammo") : -1, cap});
            }
        }
    }

    private void holdGun()
    {
        EntityPlayerMP p = DevTestUtil.player();
        p.inventory.setInventorySlotContents(0, loadedGun.copy());
        p.inventory.currentItem = 0;
        before = Deci.gunAmmo(p.getHeldItem());
    }

    private void cleanup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        p.inventory.setInventorySlotContents(0, null);
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
        int g = guns.size(), loaded = 0, outside = 0, untagged = 0;
        for (int[] x : guns)
        {
            loaded += x[0] > 0 ? 1 : 0;
            outside += x[0] < 0 || x[0] > x[1] ? 1 : 0;
            untagged += x[0] > 0 && x[2] == 0 ? 1 : 0;
        }
        DevTestResults.value(name(), "guns rolled", g + " in " + CLICKS + " opens");
        // a coin per gun: within 3 standard deviations of half (a 30 to 70 % bound failed 15 of 21 by chance; the unit
        // test checks the split itself on 4000 rolls)
        DevTestResults.check(name(), "about half the guns loaded", loaded + " of " + g,
                             g >= 10 && Math.abs(2 * loaded - g) <= 3 * Math.sqrt(g), "half within 3 standard deviations, 10 or more guns");
        DevTestResults.check(name(), "gun rounds within its magazine, Decimation's tag", outside + " outside, " + untagged + " without the tag",
                             outside == 0 && untagged == 0, "0 and 0");
        DevTestResults.check(name(), "a looted loaded gun fires", loadedGun == null ? "no loaded automatic gun" : before + " then " + after,
                             loadedGun != null && before > 1 && after == before - 1, "one round less on the server");
    }
}
