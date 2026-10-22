package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.decimation.fixes.LootBody;
import net.decimation.fixes.LootRefill;
import net.decimation.fixes.LootSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

/**
 * Tarkov style search, feature 12 C (fixes/LootSearch): a real click on Decimation's military crate opens its loot screen
 * with every filled slot unsearched; a click on an unsearched slot takes nothing; its stack shows no tooltip; the slots are
 * revealed in slot order, each after its time; a searched slot can be taken. A loot body keeps its progress when its screen
 * closes (no time passes while closed) and goes on when it opens again. Costs: the server tick and the drawing.
 */
public class LootSearchLiveTest extends DevTestMode
{
    public String name() { return "lootsearch"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    private static final int CX = X, CZ = Z - 3;

    private int step, wait, opened, firstAt = -1, doneAt = -1;
    private volatile Runnable onServer;
    private volatile boolean served;
    private volatile int bodyId = -1;
    private volatile int[] prog;
    private volatile String note = "";
    private WorldSettings.GameType modeBefore;
    private String spawningBefore;
    private int[] firstTicks;
    private int bodyDoneAtClose;

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        wait++;
        switch (step)
        {
            case 0:
                if (wait == 1)
                {
                    mc.theWorld.getWorldInfo().setRaining(false);
                    mc.theWorld.setRainStrength(0F);
                    serve(this::setup);
                }
                if (wait > 2 && served)
                {
                    next(1);
                }
                break;
            case 1: // open the crate as the mouse does
                if (wait == 10)
                {
                    watching = true;
                    mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, null, CX, Y, CZ, 1,
                        net.minecraft.util.Vec3.createVectorHelper(CX + 0.5, Y + 1.0, CZ + 0.5));
                }
                if (wait > 10 && screenOpen(mc))
                {
                    opened = wait;
                    next(2);
                }
                if (wait > 120)
                {
                    DevTestResults.check(name(), "crate: the loot screen opens", "no screen", false, Deci.LOOT_SCREEN);
                    return end(mc);
                }
                break;
            case 2: // unsearched at first: nothing can be taken, no tooltip, the drawing
                if (wait == 8)
                {
                    serve(() -> prog = LootSearch.progress(Deci.lootInventory(DevTestUtil.player())));
                }
                if (wait >= 10 && wait < 1000 && served)
                {
                    wait = 1000;
                    DevTestResults.value(name(), "client container of the loot screen", mc.thePlayer.openContainer.getClass().getName()
                                         + " ours " + Deci.hasShiftClick(mc.thePlayer.openContainer));
                    int hidden = 0, filled = 0;
                    Slot first = null;
                    for (Object o : mc.thePlayer.openContainer.inventorySlots)
                    {
                        Slot s = (Slot) o;
                        if (s.inventory == Deci.lootInventory(mc.thePlayer) && s.getHasStack())
                        {
                            filled++;
                            if (LootSearch.hidden(s))
                            {
                                hidden++;
                                first = first == null ? s : first;
                            }
                        }
                    }
                    int[] p = prog;
                    DevTestResults.check(name(), "crate: every filled slot unsearched at first",
                                         hidden + " hidden of " + filled + ", server " + (p == null ? "none" : p[0] + " revealed of " + p[3]),
                                         filled > 0 && hidden == filled && p != null && p[0] == 0 && p[3] == filled, "all hidden, none revealed");
                    if (first != null)
                    {
                        java.util.List<?> tip = first.getStack().getTooltip(mc.thePlayer, false);
                        DevTestResults.check(name(), "an unsearched stack shows no tooltip", tip.size() + " lines", tip.isEmpty(), "none");
                        firstSlot = first.slotNumber;
                        firstIndex = first.getSlotIndex();
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, first.slotNumber, 0, 0, mc.thePlayer);
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, first.slotNumber, 0, 1, mc.thePlayer);
                    }
                }
                if (wait == 1006)
                {
                    DevTestUtil.screenshot(mc, name(), "lootsearch_crate");
                    serve(this::stillThere);
                }
                if (wait > 1006 && served)
                {
                    next(3);
                }
                break;
            case 3: // revealed in order and time
                if (wait % 2 == 0)
                {
                    serve(() -> prog = LootSearch.progress(Deci.lootInventory(DevTestUtil.player())));
                }
                int[] p = prog;
                if (p != null && p[0] >= 1 && firstAt < 0)
                {
                    firstAt = wait;
                }
                if (p != null && p[0] >= p[3] && doneAt < 0)
                {
                    doneAt = wait;
                }
                if (doneAt > 0 || wait > 1200)
                {
                    serve(this::timing);
                    next(4);
                }
                break;
            case 4: // a searched slot is taken as always
                if (wait >= 4 && wait < 500 && served)
                {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, firstSlot, 0, 0, mc.thePlayer);
                    wait = 500;
                }
                if (wait == 506)
                {
                    ItemStack cursor = mc.thePlayer.inventory.getItemStack();
                    DevTestResults.check(name(), "a searched slot is taken", cursor == null ? "nothing" : cursor.getDisplayName(),
                                         cursor != null, "the item on the cursor");
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, firstSlot, 0, 0, mc.thePlayer);   // back
                }
                if (wait == 512)
                {
                    mc.thePlayer.closeScreen();
                    next(5);
                }
                break;
            case 5: // the body: open 25 ticks, closed 40, open again
                Entity b = bodyId < 0 ? null : mc.theWorld.getEntityByID(bodyId);
                if (wait == 10 && b != null)
                {
                    mc.playerController.interactWithEntitySendPacket(mc.thePlayer, b);
                }
                if (wait > 10 && screenOpen(mc) && opened > 0)
                {
                    opened = -wait;                       // opened at -opened
                }
                if (opened < 0 && wait == -opened + 25)
                {
                    serve(() -> {
                        prog = LootSearch.progress(Deci.lootInventory(DevTestUtil.player()));
                        bodyDoneAtClose = prog == null ? -1 : prog[1];
                    });
                }
                if (opened < 0 && wait == -opened + 28)
                {
                    mc.thePlayer.closeScreen();
                }
                if (opened < 0 && wait == -opened + 68 && b != null)
                {
                    mc.playerController.interactWithEntitySendPacket(mc.thePlayer, b);
                }
                if (opened < 0 && wait == -opened + 80)
                {
                    serve(this::bodyKept);
                }
                if (opened < 0 && wait > -opened + 80 && served)
                {
                    next(6);
                }
                if (wait > 400)
                {
                    DevTestResults.check(name(), "body: its screen opens", "no screen", false, Deci.LOOT_SCREEN);
                    next(6);
                }
                break;
            default:
                if (wait == 1)
                {
                    long[] c = LootSearch.costs();
                    double drawUs = c[2] / 1000.0;
                    DevTestResults.check(name(), "drawing the search", String.format("%.1f us a frame (median of %d frames)", drawUs, c[3]),
                                         c[3] > 0 && drawUs < 100, "under 100 us a frame");
                    mc.thePlayer.closeScreen();
                    serve(this::cleanup);
                }
                if (wait > 1 && served)
                {
                    return end(mc);
                }
        }
        return true;
    }

    private int firstSlot = -1, firstIndex = -1;

    private boolean end(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = false;
        return false;
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

    private static boolean screenOpen(Minecraft mc)
    {
        return mc.currentScreen != null && mc.currentScreen.getClass().getName().equals(Deci.LOOT_SCREEN);
    }

    public void server()
    {
        if (watching && DevTestUtil.player() != null)
        {
            watch();
        }
        Runnable r = onServer;
        if (r == null || DevTestUtil.player() == null)
        {
            return;
        }
        onServer = null;
        runServerStep(r);
    }

    /** Server ticks since the crate's search started, its first reveal and its end (read every tick while watching). */
    private volatile int seenTick = -1, revealTick = -1, finishTick = -1, serverTicks;
    private volatile boolean watching;

    private void watch()
    {
        serverTicks++;
        int[] p = LootSearch.progress(Deci.lootInventory(DevTestUtil.player()));
        if (p == null)
        {
            return;
        }
        if (seenTick < 0)
        {
            seenTick = serverTicks;
        }
        if (p[0] >= 1 && revealTick < 0)
        {
            revealTick = serverTicks;
        }
        if (p[0] >= p[3] && finishTick < 0)
        {
            finishTick = serverTicks;
            watching = false;
        }
    }

    private void runServerStep(Runnable r)
    {
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

    private void setup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.setWorldTime(6000);
        w.getWorldInfo().setRaining(false);
        w.setRainStrength(0F);
        DevTestArena.build(w, p);
        p.playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 0.5, 180, 40);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
        p.inventory.setItemStack(null);
        LootRefill.clearAll();
        w.setBlock(CX, Y, CZ, GameRegistry.findBlock("deci", "BlockMilitaryCrate"), 0, 3);
        LootBody body = new LootBody(w);
        body.inventory.setInventorySlotContents(1, new ItemStack(GameRegistry.findItem("deci", "akm")));   // a gun: 3 s
        body.inventory.setInventorySlotContents(4, new ItemStack(Items.bread, 2));                        // 1 s
        body.setPosition(X + 2.5, Y, Z - 1.5);
        DevTestResults.check(name(), "body: spawns", "", w.spawnEntityInWorld(body), "spawned");
        bodyId = body.getEntityId();
    }

    /** The click on the unsearched slot took nothing: the stack is still in the crate and the cursor empty. */
    private void stillThere()
    {
        EntityPlayerMP p = DevTestUtil.player();
        IInventory inv = Deci.lootInventory(p);
        ItemStack in = inv == null || firstIndex < 0 ? null : inv.getStackInSlot(firstIndex);
        ItemStack cursor = p.inventory.getItemStack();
        DevTestResults.check(name(), "an unsearched slot cannot be taken (plain and shift click)",
                             (in == null ? "slot empty" : "still in the crate") + ", cursor " + (cursor == null ? "empty" : cursor.getDisplayName()),
                             in != null && cursor == null, "still in the crate, cursor empty");
        // the expected time of each slot, in slot order
        java.util.List<Integer> t = new java.util.ArrayList<Integer>();
        for (int i = 0; inv != null && i < inv.getSizeInventory(); i++)
        {
            if (inv.getStackInSlot(i) != null)
            {
                t.add(LootSearch.secondsOf(inv.getStackInSlot(i)) * 20);
            }
        }
        firstTicks = new int[t.size()];
        for (int i = 0; i < t.size(); i++)
        {
            firstTicks[i] = t.get(i);
        }
    }

    /** First reveal and the whole search against the items' times (client ticks; the screen opened at "opened"). */
    private void timing()
    {
        int first = firstTicks == null || firstTicks.length == 0 ? 0 : firstTicks[0], all = 0;
        for (int i = 0; firstTicks != null && i < firstTicks.length; i++)
        {
            all += firstTicks[i];
        }
        // the search ticks once on the server tick that starts it, so a slot of N ticks shows at start + N - 1
        int tFirst = revealTick - seenTick + 1, tAll = finishTick - seenTick + 1;
        DevTestResults.check(name(), "the first slot revealed after its time", tFirst + " server ticks, expected " + first,
                             revealTick > 0 && Math.abs(tFirst - first) <= 1, "within 1 tick");
        DevTestResults.check(name(), "all slots revealed, in order, after their times", tAll + " server ticks, expected " + all,
                             finishTick > 0 && Math.abs(tAll - all) <= 1, "within 1 tick");
        long[] c = LootSearch.costs();
        DevTestResults.check(name(), "server tick cost", String.format("%.2f us a tick (median of %d ticks)", c[0] / 1000.0, c[1]),
                             c[0] / 1000.0 < 20, "under 20 us a tick");
    }

    /** The body's search kept what it had when its screen closed and went on once it opened again. */
    private void bodyKept()
    {
        int[] p = LootSearch.progress(Deci.lootInventory(DevTestUtil.player()));
        DevTestResults.check(name(), "body: progress kept while closed",
                             "at close " + bodyDoneAtClose + " ticks into the gun, 12 ticks after reopening " + (p == null ? "none" : p[1]),
                             p != null && bodyDoneAtClose >= 20 && p[0] == 0 && p[1] >= bodyDoneAtClose && p[1] <= bodyDoneAtClose + 20,
                             "the gun's ticks went on from where they stopped, none counted while closed");
    }

    private void cleanup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.setBlockToAir(CX, Y, CZ);
        Entity b = bodyId < 0 ? null : w.getEntityByID(bodyId);
        if (b != null)
        {
            b.setDead();
        }
        LootRefill.clearAll();
        if (spawningBefore != null)
        {
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        }
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);
        }
    }
}
