package net.decimation.worldgen.devtest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.decimation.fixes.Deci;
import net.decimation.fixes.LootBlocks;
import net.decimation.fixes.LootRefill;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

/**
 * Lootable copies and loot kinds, feature 12 B (fixes/LootBlocks, docs/superpowers/specs/2026-10-22-loot-rules-design.md):
 * every copy registered, of its original's class, look and name ("Cabinet" for the civilian cabinet), with its tooltip;
 * each pair standing side by side for a shot (original left, copy right); a real right click on every copy in survival:
 * the lootable ones open Decimation's loot screen with items of their list only, the plain wooden crate opens nothing; a
 * police shelf placed from its item as a player places it fills both cells and keeps one loot spot; the loot kinds our
 * world generation asks for. Costs: forPlace for any other block.
 */
public class LootBlocksLiveTest extends DevTestMode
{
    public String name() { return "lootblocks"; }

    private static final int TIMEOUT = 100;
    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;

    private int step, wait, index;
    private volatile Runnable onServer;
    private volatile boolean served;
    private WorldSettings.GameType modeBefore;
    private String spawningBefore;
    private final List<String[]> pairs = new java.util.ArrayList<String[]>();   // {copy name, original, like or null}


    private java.awt.image.BufferedImage shotA;
    private int alike;
    private final StringBuilder worst = new StringBuilder();

    private volatile double camX, camY, camZ;

    /** The client's camera stands where view() put it (a shot before that compares two places). */
    private boolean settled(Minecraft mc)
    {
        return mc.thePlayer != null && Math.abs(mc.thePlayer.posX - camX) < 0.05 && Math.abs(mc.thePlayer.posZ - camZ) < 0.05
            && Math.abs(mc.thePlayer.posY - camY) < 0.6 && Math.abs(mc.thePlayer.posX - mc.thePlayer.prevPosX) < 0.001
            && Math.abs(mc.thePlayer.posZ - mc.thePlayer.prevPosZ) < 0.001;
    }

    /** A screenshot saved for the sheet, and read back for the comparison. */
    private java.awt.image.BufferedImage shot(Minecraft mc, String file)
    {
        DevTestUtil.screenshot(mc, name(), file);
        try
        {
            return javax.imageio.ImageIO.read(new java.io.File(mc.mcDataDir, "screenshots/" + file)); // saved without an extension
        }
        catch (java.io.IOException e)
        {
            return null;
        }
    }

    /** The share of pixels whose colour differs by more than 24 in any channel (sky and floor are the same in both). */
    private static double apart(java.awt.image.BufferedImage a, java.awt.image.BufferedImage b)
    {
        if (a == null || b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
        {
            return 1;
        }
        long off = 0;
        // the middle of the frame (the prop), clear of toasts and the hotbar
        int x0 = a.getWidth() / 4, x1 = a.getWidth() * 3 / 4, y0 = a.getHeight() / 4, y1 = a.getHeight() * 9 / 10;
        long all = 0;
        for (int y = y0; y < y1; y += 2)
        {
            for (int x = x0; x < x1; x += 2)
            {
                all++;
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                if (Math.abs((p >> 16 & 255) - (q >> 16 & 255)) > 24 || Math.abs((p >> 8 & 255) - (q >> 8 & 255)) > 24
                    || Math.abs((p & 255) - (q & 255)) > 24)
                {
                    off++;
                }
            }
        }
        return off / (double) Math.max(1, all);
    }

    // ---- client

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        wait++;
        switch (step)
        {
            case 0:
                if (wait == 1)
                {
                    // dry on the client too: a server change outside its weather tick never reaches the client
                    mc.theWorld.getWorldInfo().setRaining(false);
                    mc.theWorld.getWorldInfo().setThundering(false);
                    mc.theWorld.setRainStrength(0F);
                    mc.theWorld.setThunderStrength(0F);
                    serve(this::setup);
                }
                if (wait > 2 && served)
                {
                    looks(mc);
                    next(1);
                }
                break;
            case 1: // each pair at one spot, one camera: the original, then the copy in its place, pixels compared
                if (index >= pairs.size())
                {
                    mc.gameSettings.hideGUI = false;
                    DevTestResults.check(name(), "each copy draws as its original (same spot, same camera)", alike + " of "
                                         + pairs.size() + (worst.length() > 0 ? ": " + worst : ""), alike == pairs.size(),
                                         "all under 0.5 % of pixels apart");
                    index = 0;
                    next(2);
                    break;
                }
                if (wait == 1)
                {
                    mc.gameSettings.hideGUI = true;
                    final int i = index;
                    serve(() -> view(i, false));
                }
                if (wait >= 30 && wait < 1000 && (settled(mc) || wait > 120))
                {
                    wait = 1000;
                    shotA = shot(mc, "lootblocks_" + pairs.get(index)[0] + "_a");
                    final int i = index;
                    serve(() -> view(i, true));
                }
                if (wait >= 1030 && (settled(mc) || wait > 1120))
                {
                    java.awt.image.BufferedImage b = shot(mc, "lootblocks_" + pairs.get(index)[0] + "_b");
                    double d = apart(shotA, b);
                    if (d < 0.005)
                    {
                        alike++;
                    }
                    else
                    {
                        worst.append(pairs.get(index)[0]).append(String.format(" %.1f %%; ", 100 * d));
                    }
                    DevTestResults.value(name(), pairs.get(index)[0] + ": pixels apart from its original", String.format("%.3f %%", 100 * d));
                    index++;
                    wait = 0;
                }
                break;
            case 2: // a real click on every copy
                if (index >= pairs.size())
                {
                    next(3);
                    break;
                }
                if (wait == 1)
                {
                    final int i = index;
                    serve(() -> stand(i));
                }
                if (wait == 12)
                {
                    int[] c = copySpot(index);
                    mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, null, c[0], c[1], c[2], 1,
                        net.minecraft.util.Vec3.createVectorHelper(c[0] + 0.5, c[1] + 1.0, c[2] + 0.5));
                }
                if (wait == 40)
                {
                    boolean open = mc.currentScreen != null && mc.currentScreen.getClass().getName().equals(Deci.LOOT_SCREEN);
                    final String[] p = pairs.get(index);
                    final boolean opened = open;
                    serve(() -> clicked(p, opened));
                    if (open)
                    {
                        mc.thePlayer.closeScreen();
                    }
                }
                if (wait == 46)
                {
                    index++;
                    wait = 0;
                }
                break;
            case 3:
                if (wait == 1)
                {
                    serve(this::shelfFromItem);
                }
                if (wait == 5)
                {
                    serve(this::kindsAndCost);
                }
                if (wait == 9)
                {
                    serve(this::cleanup);
                }
                if (wait > 9 && served)
                {
                    net.decimation.worldgen.DevAutoTest.keepScreen = false;
                    return false;
                }
                break;
        }
        return true;
    }

    /** Names, tooltips and the client's loot table (the hint asks it). */
    private void looks(Minecraft mc)
    {
        int ok = 0;
        StringBuilder bad = new StringBuilder();
        for (String[] p : pairs)
        {
            Block copy = Block.getBlockFromName("deciworldgen:" + p[0]), orig = Block.getBlockFromName(p[1]);
            ItemStack cs = new ItemStack(copy), os = new ItemStack(orig);
            String want = p[0].equals("cabinet") ? "Cabinet" : os.getDisplayName();
            List<?> tip = cs.getTooltip(mc.thePlayer, false);
            String line = tip.size() > 1 ? String.valueOf(tip.get(tip.size() - 1)) : "";
            boolean good = cs.getDisplayName().equals(want) && (p[2] == null ? line.contains("Not lootable") : line.contains("Lootable: "))
                && Deci.hasLootPool(copy) == (p[2] != null);
            if (good)
            {
                ok++;
            }
            else
            {
                bad.append(p[0]).append(" '").append(cs.getDisplayName()).append("' ").append(line).append("; ");
            }
        }
        DevTestResults.check(name(), "names, tooltips, the client's loot table", ok + " of " + pairs.size() + " " + bad,
                             ok == pairs.size(), "all: the original's name (Cabinet for the civilian one), its loot line");
        List<?> t = new ItemStack(Block.getBlockFromName("deci:BlockMetalShelf")).getTooltip(mc.thePlayer, false);
        DevTestResults.value(name(), "the original shelf's tooltip", t.get(t.size() - 1));
    }

    private void next(int s)
    {
        step = s;
        wait = 0;
    }

    private void serve(Runnable r)
    {
        served = false;
        onServer = r;
    }

    // ---- server

    public void server()
    {
        Runnable r = onServer;
        if (r == null || DevTestUtil.player() == null)
        {
            return;
        }
        onServer = null;
        try
        {
            r.run();
        }
        catch (RuntimeException e)
        {
            DevTestResults.check(name(), "server step", e.toString(), false, "no exception");
        }
        served = true;
    }

    /** Pair i: the original at its spot (the six trucks in a row of their own, 15 apart; the rest 10 apart), the copy east of it. */
    private int[] origSpot(int i)
    {
        return i < 6 ? new int[] {X - 78 + i * 15, Y, Z - 2} : new int[] {X - 78 + (i - 6) * 10, Y, Z - 18};
    }

    private int[] copySpot(int i)
    {
        int[] o = origSpot(i);
        return new int[] {o[0] + (pairs.get(i)[1].contains("Truck") ? 6 : 3), o[1], o[2]};
    }

    private void setup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.setWorldTime(6000);
        w.getWorldInfo().setRaining(false);
        w.getWorldInfo().setThundering(false);
        w.getWorldInfo().setRainTime(100000);              // no rain starting during the test
        w.getWorldInfo().setThunderTime(100000);
        w.setRainStrength(0F);
        w.setThunderStrength(0F);
        DevTestArena.build(w, p);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(WorldSettings.GameType.SURVIVAL);   // Decimation's handler skips creative players
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
        LootRefill.clearAll();
        int same = 0;
        StringBuilder bad = new StringBuilder();
        for (Block copy : LootBlocks.made())
        {
            String name = String.valueOf(Block.blockRegistry.getNameForObject(copy)).replace("deciworldgen:", "");
            String[] row = row(name);
            if (row == null)
            {
                continue;
            }
            pairs.add(row);
            Block orig = Block.getBlockFromName(row[1]);
            boolean alike = copy.getClass() == orig.getClass() && copy.getLightValue() == orig.getLightValue()
                && copy.getBlockBoundsMaxY() == orig.getBlockBoundsMaxY() && copy.getCreativeTabToDisplayOn() == orig.getCreativeTabToDisplayOn()
                && copy.getBlockHardness(w, 0, 0, 0) == orig.getBlockHardness(w, 0, 0, 0);
            if (alike)
            {
                same++;
            }
            else
            {
                bad.append(name).append(' ');
            }
        }
        DevTestResults.check(name(), "copies registered", pairs.size(), pairs.size() == 15, "15");
        DevTestResults.check(name(), "each of its original's class, light, size, tab, hardness", same + " " + bad, same == pairs.size(),
                             "all");
        for (int i = 0; i < pairs.size(); i++)
        {
            int[] o = origSpot(i), c = copySpot(i);
            place(w, Block.getBlockFromName(pairs.get(i)[1]), o, p);
            place(w, Block.getBlockFromName("deciworldgen:" + pairs.get(i)[0]), c, p);
        }
    }

    /** {copy, original, like} of a copy's name (LootBlocks' table read through its loot lines). */
    private static String[] row(String name)
    {
        String orig = name.startsWith("truckwreckage") ? "deci:BlockTruckWreckage" + name.charAt(13)
            : name.startsWith("waterpallettarp") ? "deci:BlockWaterPalletTarp" : name.startsWith("waterpallet") ? "deci:BlockWaterPallet"
            : name.startsWith("militaryradiosmall") ? "deci:BlockMilitaryRadioSmall" : name.startsWith("militaryradio") ? "deci:BlockMilitaryRadio"
            : name.startsWith("woodcrate") ? "deci:BlockWoodCrate" : name.startsWith("shelf") ? "deci:BlockMetalShelf"
            : name.startsWith("cabinet") ? "deci:BlockWeaponCabinet" : null;
        String like = name.startsWith("truck") ? "deci:BlockWreckage1" : name.startsWith("waterpallet") ? "deci:BlockVendingMachine_1"
            : name.startsWith("militaryradio") || name.equals("shelf_military") ? "deci:BlockMilitaryCrate"
            : name.endsWith("_police") ? "deci:BlockPoliceCrate" : name.equals("cabinet") ? "deci:BlockMetalShelf" : null;
        return orig == null ? null : new String[] {name, orig, like};
    }

    /** A block facing south (toward the camera), a shelf built whole as Decimation's placement does. */
    private static void place(World w, Block b, int[] s, EntityPlayerMP p)
    {
        boolean shelf = b instanceof net.decimation.mod.common.block.props.BlockMetalShelf;
        int m = shelf ? 1 : 5;                           // fronts north, toward the camera of the look check
        w.setBlock(s[0], s[1], s[2], b, m, 3);
        if (shelf)
        {
            Deci.buildMultiblock(w, s[0], s[1], s[2], b, m);
        }
    }

    /** The spot every pair is shot at, clear of the rows. */
    private static final int SX = X - 30, SZ = Z + 16;

    /** Pair i's original (copy false) or copy at the shot spot, the camera in front of it. */
    private void view(int i, boolean copy)
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.setBlockToAir(SX, Y + 1, SZ);
        w.setBlockToAir(SX, Y, SZ);
        place(w, Block.getBlockFromName(copy ? "deciworldgen:" + pairs.get(i)[0] : pairs.get(i)[1]), new int[] {SX, Y, SZ}, p);
        boolean truck = pairs.get(i)[1].contains("Truck");
        p.setGameType(WorldSettings.GameType.CREATIVE);   // hovering: the camera stays where it is put (survival would drop)
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        camX = SX + 0.5;
        camY = Y + (truck ? 3 : 1);
        camZ = SZ - (truck ? 9 : 4);
        p.playerNetServerHandler.setPlayerLocation(camX, camY, camZ, 0, truck ? 20 : 15);
    }

    /** The player south of the copy of pair i, close enough to click it. */
    private void stand(int i)
    {
        EntityPlayerMP p = DevTestUtil.player();
        p.setGameType(WorldSettings.GameType.SURVIVAL);   // Decimation's handler skips creative players
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        int[] c = copySpot(i);
        p.playerNetServerHandler.setPlayerLocation(c[0] + 0.5, Y, c[2] + 2.5, 180, 40);
    }

    private void clicked(String[] pair, boolean opened)
    {
        EntityPlayerMP p = DevTestUtil.player();
        InventoryBasic inv = Deci.lootInventory(p);
        if (pair[2] == null)
        {
            DevTestResults.check(name(), pair[0] + ": a click", opened ? "a loot screen" : "nothing", !opened, "nothing (no list)");
            return;
        }
        Map.Entry<Integer, List<ItemStack>> list = Deci.lootTable().get(pair[2]);
        Set<Item> allowed = new HashSet<Item>();
        for (ItemStack s : list.getValue())
        {
            allowed.add(s.getItem());
        }
        int n = 0, foreign = 0;
        for (int k = 0; inv != null && k < inv.getSizeInventory(); k++)
        {
            ItemStack s = inv.getStackInSlot(k);
            if (s != null)
            {
                n++;
                foreign += allowed.contains(s.getItem()) ? 0 : 1;
            }
        }
        DevTestResults.check(name(), pair[0] + ": a click opens " + pair[2].replace("deci:", "") + "'s list",
                             (opened ? "screen" : "no screen") + ", " + n + " stacks, " + foreign + " from elsewhere",
                             opened && n > 0 && foreign == 0, "screen, stacks of that list only");
    }

    /** A police shelf from its item, as a player places it: both cells ours, masters set, one loot spot. */
    private void shelfFromItem()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        Block shelf = Block.getBlockFromName("deciworldgen:shelf_police");
        int x = X + 8, z = Z + 6;
        p.setLocationAndAngles(x + 0.5, Y, z + 3.5, 180, 0);
        ItemStack st = new ItemStack(shelf);
        boolean used = st.getItem().onItemUse(st, p, w, x, Y - 1, z, 1, 0.5f, 1f, 0.5f);
        int[] spot = Deci.lootSpot(w, x, Y + 1, z);
        DevTestResults.check(name(), "police shelf from its item: both cells, one spot",
                             used + ", " + w.getBlock(x, Y, z).getLocalizedName() + " / " + (w.getBlock(x, Y + 1, z) == shelf)
                                 + ", spot " + (spot == null ? "none" : spot[0] + "," + spot[1] + "," + spot[2]),
                             used && w.getBlock(x, Y, z) == shelf && w.getBlock(x, Y + 1, z) == shelf && spot != null && spot[1] == Y,
                             "placed, ours in both cells, the upper part leads to the lower");
        w.func_147480_a(x, Y, z, false);
        DevTestResults.check(name(), "police shelf broken: both cells go", w.getBlock(x, Y + 1, z).getLocalizedName(),
                             w.getBlock(x, Y + 1, z) == Blocks.air, "air");
    }

    /** What our world generation asks for, and the cost of asking for any other block. */
    private void kindsAndCost()
    {
        Block shelf = Block.getBlockFromName("deci:BlockMetalShelf"), cab = Block.getBlockFromName("deci:BlockWeaponCabinet");
        String got = name(LootBlocks.forPlace(shelf, LootBlocks.CIVILIAN)) + " " + name(LootBlocks.forPlace(shelf, LootBlocks.POLICE)) + " "
            + name(LootBlocks.forPlace(shelf, LootBlocks.MILITARY)) + " | " + name(LootBlocks.forPlace(cab, LootBlocks.CIVILIAN)) + " "
            + name(LootBlocks.forPlace(cab, LootBlocks.POLICE)) + " " + name(LootBlocks.forPlace(cab, LootBlocks.MILITARY));
        DevTestResults.check(name(), "kinds for world generation", got,
                             got.equals("BlockMetalShelf shelf_police shelf_military | cabinet cabinet_police BlockWeaponCabinet"),
                             "shelf: civilian, police, military | cabinet: Cabinet, police, the original");
        Block other = Blocks.stone;
        Block keep = null;
        for (int i = 0; i < 200000; i++)
        {
            keep = LootBlocks.forPlace(other, i % 3);
        }
        double ns = Double.MAX_VALUE;
        for (int round = 0; round < 3; round++)   // best of three: another busy process slows single rounds
        {
            long t0 = System.nanoTime();
            for (int i = 0; i < 1000000; i++)
            {
                keep = LootBlocks.forPlace(other, i % 3);
            }
            ns = Math.min(ns, (System.nanoTime() - t0) / 1e6);
        }
        DevTestResults.check(name(), "forPlace for any other block", String.format("%.2f ns", ns) + (keep == other ? "" : " wrong"),
                             ns < 20 && keep == other, "under 20 ns, the block as it is");
    }

    private static String name(Block b)
    {
        return String.valueOf(Block.blockRegistry.getNameForObject(b)).replace("deci:", "").replace("deciworldgen:", "");
    }

    private void cleanup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        for (int i = 0; i < pairs.size(); i++)
        {
            for (int[] s : new int[][] {origSpot(i), copySpot(i)})
            {
                w.setBlockToAir(s[0], s[1] + 1, s[2]);
                w.setBlockToAir(s[0], s[1], s[2]);
            }
        }
        w.setBlockToAir(SX, Y + 1, SZ);
        w.setBlockToAir(SX, Y, SZ);
        LootRefill.clearAll();
        if (spawningBefore != null)
        {
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        }
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);
        }
        p.playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 0.5, 180, 30);
    }
}
