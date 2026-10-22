package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.decimation.fixes.Loot;
import net.decimation.fixes.LootBody;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * The two loot spikes (docs/loot_levels.md section 3.4 and 4, docs/systems_plan.md section 7):
 * 1. our code opens Decimation's loot screen on an inventory of ours (Loot.open), and a stack taken
 *    there through the real window clicks leaves that inventory and lands in the player's;
 * 2. a body entity (LootBody) opens it on a right click (the client's real interact packet), keeps
 *    what was not taken (reopened: one stack left), goes when emptied, and saves its items to NBT.
 * 3. Decimation's own crate (a real right click through LootInteractHandler deci.aK.k): the screen opens with rolled
 *    stacks, the spot goes on cooldown, a second click within it opens nothing ("This ... is empty...").
 * Shots: loot_crate (screen on our inventory), loot_body_world (the body), loot_body (its screen).
 */
public class LootTest extends DevTestMode
{
    public String name() { return "loot"; }

    private static final int SETUP = 20, TIMEOUT = 100;
    private volatile int ticks;
    /** Client step; the server reacts to the request flags. */
    private int step, wait;
    private volatile boolean openCrate, checkCrate, crateDone, checkTake, takeDone, checkEmpty, emptyDone, done;
    private volatile int bodyId = -1, secondId = -1;
    private volatile boolean spawnSecond, kill, killed;
    private LootBody second;
    private volatile Item taken;
    private InventoryBasic crate;
    private LootBody body;
    private int emptyAt;
    /** Step 3: Decimation's crate, placed by the server, clicked by the client. */
    private volatile boolean placeDeci, deciPlaced, checkDeciCooldown, finishDeci, deciCleaned;
    private volatile int crateX, crateY, crateZ;
    private volatile String deciName = "none";
    private net.minecraft.world.WorldSettings.GameType modeBefore;

    // ---- client: the player's side, through the real GUI paths

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ticks < SETUP + 5)
        {
            return true;
        }
        net.decimation.worldgen.DevAutoTest.keepScreen = true; // the harness closes screens otherwise
        wait++;
        switch (step)
        {
            case 0: // spike 1: our inventory on Decimation's loot screen
                if (screenOpen(mc) || wait > TIMEOUT)
                {
                    DevTestResults.check(name(), "crate: loot screen opens", screenName(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    DevTestResults.check(name(), "crate: stacks shown", lootStacks(mc), lootStacks(mc) == 2, "2");
                    next(1);
                }
                break;
            case 1:
                if (wait == 15)
                {
                    DevTestUtil.screenshot(mc, name(), "loot_crate");
                }
                if (wait == 25)
                {
                    take(mc);
                }
                if (wait == 26)
                {
                    put(mc);
                }
                if (wait == 35)
                {
                    checkCrate = true;
                }
                if (crateDone)
                {
                    mc.thePlayer.closeScreen();
                    next(2);
                }
                break;
            case 2: // spike 2: the body
                Entity e = bodyId < 0 ? null : mc.theWorld.getEntityByID(bodyId);
                if (e != null && wait == 20)
                {
                    mc.gameSettings.hideGUI = true;
                }
                if (e != null && wait == 30)
                {
                    DevTestUtil.screenshot(mc, name(), "loot_body_world");
                }
                if (e != null && wait == 40)
                {
                    mc.gameSettings.hideGUI = false;
                    mc.playerController.interactWithEntitySendPacket(mc.thePlayer, e);
                    next(3);
                }
                if (wait > TIMEOUT * 2)
                {
                    DevTestResults.check(name(), "body: seen by the client", "no", false, "the body entity on the client");
                    done = true;
                }
                break;
            case 3:
                if (screenOpen(mc) || wait > TIMEOUT)
                {
                    DevTestResults.check(name(), "body: right click opens the loot screen", screenName(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    DevTestResults.check(name(), "body: stacks shown", lootStacks(mc), lootStacks(mc) == 2, "2");
                    next(4);
                }
                break;
            case 4:
                if (wait == 15)
                {
                    DevTestUtil.screenshot(mc, name(), "loot_body");
                }
                if (wait == 25)
                {
                    take(mc);
                }
                if (wait == 26)
                {
                    put(mc);
                }
                if (wait == 35)
                {
                    checkTake = true;
                }
                if (takeDone)
                {
                    mc.thePlayer.closeScreen();
                    next(5);
                }
                break;
            case 5: // reopen: what was not taken is still there
                if (wait == 10)
                {
                    Entity b = mc.theWorld.getEntityByID(bodyId);
                    if (b != null)
                    {
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, b);
                    }
                }
                if (wait > 10 && screenOpen(mc) || wait > TIMEOUT)
                {
                    DevTestResults.check(name(), "body: reopened shows what is left", lootStacks(mc), lootStacks(mc) == 1, "1");
                    next(6);
                }
                break;
            case 6:
                if (wait == 10)
                {
                    take(mc);
                }
                if (wait == 11)
                {
                    put(mc);
                }
                if (wait == 15)
                {
                    mc.thePlayer.closeScreen();
                    checkEmpty = true;
                }
                if (emptyDone)
                {
                    spawnSecond = true;
                    next(7);
                }
                break;
            case 7: // a screen left open on a body that goes must close (no taking from it after)
                Entity b2 = secondId < 0 ? null : mc.theWorld.getEntityByID(secondId);
                if (b2 != null && wait > 10 && !kill)
                {
                    if (!screenOpen(mc) && wait % 20 == 11)
                    {
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, b2);
                    }
                    if (screenOpen(mc))
                    {
                        kill = true;
                        wait = 0;
                    }
                }
                if (kill && (!screenOpen(mc) && serverClosed() || wait > 40))
                {
                    DevTestResults.check(name(), "body: open screen closes when the body goes", wait + " ticks",
                                         !screenOpen(mc) && serverClosed(), "closed on both sides");
                    next(8);
                }
                if (!kill && wait > TIMEOUT * 2)
                {
                    DevTestResults.check(name(), "body: open screen closes when the body goes", "never opened", false,
                                         "closed on both sides");
                    next(8);
                }
                break;
            case 8: // spike 3: Decimation's own crate, a real right click (LootInteractHandler deci.aK.k)
                if (wait == 1)
                {
                    placeDeci = true;
                }
                if (deciPlaced && wait == 20)
                {
                    rightClick(mc);
                }
                if (deciPlaced && wait > 20 && screenOpen(mc) || wait > 20 + TIMEOUT)
                {
                    DevTestResults.check(name(), "decimation crate: right click opens the loot screen", screenName(mc) + " (" + deciName + ")",
                                         screenOpen(mc), Deci.LOOT_SCREEN);
                    DevTestResults.check(name(), "decimation crate: rolled stacks shown", lootStacks(mc), lootStacks(mc) > 0, "1 or more");
                    checkDeciCooldown = true;
                    mc.thePlayer.closeScreen();
                    next(9);
                }
                break;
            case 9: // a second click within the cooldown opens nothing
                if (wait == 15)
                {
                    rightClick(mc);
                }
                if (wait == 40)
                {
                    DevTestResults.check(name(), "decimation crate: second click within the cooldown stays shut", screenName(mc),
                                         !screenOpen(mc), "no loot screen");
                    mc.thePlayer.closeScreen();
                    finishDeci = true;
                }
                if (deciCleaned || wait > 40 + TIMEOUT)
                {
                    done = true;
                }
                break;
        }
        if (done)
        {
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
        }
        return !done;
    }

    private void next(int s)
    {
        step = s;
        wait = 0;
    }

    /** The server player's container is back to its own inventory. */
    private boolean serverClosed()
    {
        EntityPlayerMP p = DevTestUtil.player();
        return killed && p != null && p.openContainer == p.inventoryContainer;
    }

    private static String screenName(Minecraft mc)
    {
        return mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName();
    }

    private static boolean screenOpen(Minecraft mc)
    {
        return mc.currentScreen != null && mc.currentScreen.getClass().getName().equals(Deci.LOOT_SCREEN);
    }

    private static int lootStacks(Minecraft mc)
    {
        InventoryBasic inv = Deci.lootInventory(mc.thePlayer);
        int n = 0;
        for (int i = 0; inv != null && i < inv.getSizeInventory(); i++)
        {
            n += inv.getStackInSlot(i) != null ? 1 : 0;
        }
        return n;
    }

    /** A right click on the top of Decimation's crate, as the player's own click sends it. */
    private void rightClick(Minecraft mc)
    {
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), crateX, crateY, crateZ, 1,
                                               net.minecraft.util.Vec3.createVectorHelper(crateX + 0.5, crateY + 1.0, crateZ + 0.5));
    }

    /** Left click on the first loot slot holding a stack: it goes onto the cursor. */
    private void take(Minecraft mc)
    {
        Container c = mc.thePlayer.openContainer;
        InventoryBasic inv = Deci.lootInventory(mc.thePlayer);
        for (Object o : c.inventorySlots)
        {
            Slot s = (Slot) o;
            if (s.inventory == inv && s.getHasStack())
            {
                taken = s.getStack().getItem();
                mc.playerController.windowClick(c.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                return;
            }
        }
        DevTestResults.value(name(), "take", "no loot slot with a stack");
    }

    /** Left click on an empty main inventory slot: the cursor stack goes there. */
    private void put(Minecraft mc)
    {
        Container c = mc.thePlayer.openContainer;
        for (Object o : c.inventorySlots)
        {
            Slot s = (Slot) o;
            if (s.inventory == mc.thePlayer.inventory && s.getSlotIndex() >= 9 && s.getSlotIndex() < 36 && !s.getHasStack())
            {
                mc.playerController.windowClick(c.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                return;
            }
        }
        DevTestResults.value(name(), "put", "no empty inventory slot");
    }

    // ---- server

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World w = p.worldObj;
        if (t == SETUP)
        {
            w.setWorldTime(6000);
            p.capabilities.disableDamage = true;
            p.capabilities.isFlying = false;
            DevTestArena.build(w, p);
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 180, 30);
            for (int i = 9; i < 36; i++)
            {
                p.inventory.setInventorySlotContents(i, null); // room for what is taken
            }
            nbt(w);
            openCrate = true;
        }
        if (openCrate && t == SETUP + 5)
        {
            crate = new InventoryBasic("Loot", true, 18);
            crate.setInventorySlotContents(0, new ItemStack(Items.apple, 3));
            crate.setInventorySlotContents(5, new ItemStack(Items.iron_ingot, 2));
            Loot.open(p, crate);
        }
        if (checkCrate && !crateDone)
        {
            DevTestResults.check(name(), "crate: taken stack left our inventory", stacks(crate), stacks(crate) == 1, "1 of 2 left");
            DevTestResults.check(name(), "crate: taken stack in the player's inventory", has(p, taken), has(p, taken),
                                 "the item in the main inventory");
            body = new LootBody(w);
            body.inventory.setInventorySlotContents(0, new ItemStack(Items.gold_ingot, 4));
            body.inventory.setInventorySlotContents(3, new ItemStack(Items.bread, 2));
            body.setPosition(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 1.5);
            DevTestResults.check(name(), "body: spawns", "", w.spawnEntityInWorld(body), "spawnEntityInWorld true");
            bodyId = body.getEntityId();
            crateDone = true;
        }
        if (checkTake && !takeDone)
        {
            DevTestResults.check(name(), "body: taken stack left the body", body.stacks(), body.stacks() == 1, "1 of 2 left");
            DevTestResults.check(name(), "body: taken stack in the player's inventory", has(p, taken), has(p, taken),
                                 "the item in the main inventory");
            takeDone = true;
        }
        second(w);
        deciCrate(p, w);
        if (checkEmpty && !emptyDone)
        {
            if (emptyAt == 0)
            {
                emptyAt = t;
            }
            if (body.isDead || t - emptyAt > 45)
            {
                DevTestResults.check(name(), "body: goes when emptied", (t - emptyAt) + " ticks", body.isDead, "dead within 45 ticks");
                emptyDone = true;
            }
        }
    }

    private void second(World w)
    {
        if (spawnSecond && second == null)
        {
            second = new LootBody(w);
            second.inventory.setInventorySlotContents(0, new ItemStack(Items.emerald, 1));
            second.setPosition(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 1.5);
            w.spawnEntityInWorld(second);
            secondId = second.getEntityId();
        }
        if (kill && !killed && second != null)
        {
            second.setDead();
            killed = true;
        }
    }

    /**
     * Decimation's crate for step 3: the first loot table block named like a crate (survival for the click: the handler
     * skips creative players), cooldowns cleared so reruns in the open live world start ready; removed at the end.
     */
    private void deciCrate(EntityPlayerMP p, World w)
    {
        if (placeDeci && !deciPlaced)
        {
            String pick = null;
            for (String n : Deci.lootTable().keySet())
            {
                if (n.startsWith("deci:") && (pick == null || n.toLowerCase().contains("crate") && !pick.toLowerCase().contains("crate")))
                {
                    pick = n;
                }
            }
            net.minecraft.block.Block b = pick == null ? null : net.minecraft.block.Block.getBlockFromName(pick);
            if (b == null)
            {
                DevTestResults.check(name(), "decimation crate: a loot table block", "none", false, "a deci: block with a pool");
                placeDeci = false;
                return;
            }
            modeBefore = p.theItemInWorldManager.getGameType();
            p.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            crateX = DevTestArena.X;
            crateY = DevTestArena.Y;
            crateZ = DevTestArena.Z - 2;
            w.setBlock(crateX, crateY, crateZ, b, 0, 3);
            net.decimation.fixes.LootRefill.clearAll();
            deciName = pick;
            deciPlaced = true;
        }
        if (checkDeciCooldown)
        {
            checkDeciCooldown = false;
            DevTestResults.check(name(), "decimation crate: spot on cooldown after the open", "ready=" + Deci.lootReady(w, crateX, crateY, crateZ),
                                 !Deci.lootReady(w, crateX, crateY, crateZ), "not ready");
            InventoryBasic inv = Deci.lootInventory(p);
            DevTestResults.value(name(), "decimation crate: stacks rolled (server)", inv == null ? "none" : String.valueOf(stacks(inv)));
        }
        if (finishDeci && !deciCleaned)
        {
            if (deciPlaced)
            {
                w.setBlockToAir(crateX, crateY, crateZ);
            }
            if (modeBefore != null)
            {
                p.setGameType(modeBefore);
            }
            deciCleaned = true;
        }
    }

    /** Items survive a save and load of the body. */
    private void nbt(World w)
    {
        LootBody a = new LootBody(w);
        a.inventory.setInventorySlotContents(2, new ItemStack(Items.arrow, 7));
        a.inventory.setInventorySlotContents(17, new ItemStack(Items.diamond, 1));
        NBTTagCompound tag = new NBTTagCompound();
        a.writeToNBT(tag);
        LootBody b = new LootBody(w);
        b.readFromNBT(tag);
        ItemStack s2 = b.inventory.getStackInSlot(2), s17 = b.inventory.getStackInSlot(17);
        boolean ok = b.stacks() == 2 && s2 != null && s2.getItem() == Items.arrow && s2.stackSize == 7 && s17 != null
            && s17.getItem() == Items.diamond;
        DevTestResults.check(name(), "body: items saved and loaded", b.stacks() + " stacks", ok, "arrow x7 in slot 2, diamond in 17");
    }

    private static int stacks(InventoryBasic inv)
    {
        int n = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            n += inv.getStackInSlot(i) != null ? 1 : 0;
        }
        return n;
    }

    private static boolean has(EntityPlayerMP p, Item item)
    {
        for (int i = 9; i < 36 && item != null; i++)
        {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == item)
            {
                return true;
            }
        }
        return false;
    }
}
