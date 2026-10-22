package net.decimation.worldgen.devtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.decimation.fixes.LootRefill;
import net.decimation.worldgen.DevAutoTest;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Loot rules, feature 12 A (fixes/LootRefill, docs/superpowers/specs/2026-10-22-loot-rules-design.md): real right clicks
 * on Decimation's military crate, a metal shelf (a multiblock, clicked on its upper part) and a supply drop crate, a real
 * save and reload of the test world in between (the client leaves, the integrated server stops and saves, the world
 * opens again), then refill on a 1 minute setting. Decimation's own "This ... is empty..." line is read from the chat.
 * Costs: our two handlers per click, a record of 10000 spots saved and read. Survival (Decimation's handler skips creative
 * players), mob spawning off, everything put back at the end.
 */
public class LootRulesTest extends DevTestMode
{
    public String name() { return "lootrules"; }

    private static final int TIMEOUT = 100;
    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    /** Crate, shelf (two high, origin below), supply drop: in front of the player. */
    private static final int CX = X, CZ = Z - 3, SX = X + 3, SZ = Z - 3, DX = X - 3, DZ = Z - 3;

    private int step, wait, emptiesBefore, clickedAt = -1;
    private volatile Runnable onServer;
    private volatile boolean served;
    private volatile long crateLooted = -1, refillAt = -1;
    private WorldSettings.GameType modeBefore;
    private String spawningBefore;
    private final Chat chat = new Chat();
    private volatile boolean fileDeleted;
    private LootRefill.Settings settingsAtStart;

    /** Decimation's "This <block> is empty..." lines the client received. */
    public static final class Chat
    {
        volatile int empties;
        volatile String last = "none";

        @SubscribeEvent
        public void onChat(net.minecraftforge.client.event.ClientChatReceivedEvent e)
        {
            String s = e.message == null ? "" : e.message.getUnformattedText();
            if (s.contains(" is empty..."))
            {
                empties++;
                last = s;
            }
        }
    }

    // ---- client: the player's own clicks, the leave and the reload

    public boolean client(Minecraft mc)
    {
        DevAutoTest.keepScreen = true; // the harness closes screens otherwise
        wait++;
        switch (step)
        {
            case 0:
                if (wait == 1)
                {
                    MinecraftForge.EVENT_BUS.register(chat);
                    serve(this::setup);
                }
                if (wait > 2 && served)
                {
                    next(1);
                }
                break;
            case 1: // first click: the crate opens, our record keeps the spot
                if (wait == 15)
                {
                    click(mc, CX, Y, CZ);
                }
                if (wait > 15 && screenOpen(mc) || wait > 15 + TIMEOUT)
                {
                    DevTestResults.check(name(), "crate: the first click opens it", screen(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    mc.thePlayer.closeScreen();
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        crateLooted = LootRefill.record(w).get(0, CX, Y, CZ);
                        long now = w.getTotalWorldTime();
                        DevTestResults.check(name(), "crate: the record keeps the spot", "tick " + crateLooted + ", now " + now,
                                             crateLooted >= 0 && now - crateLooted < 60, "the world tick of the click");
                    });
                    next(2);
                }
                break;
            case 2: // second click: Decimation's own words, nothing opens
                if (wait == 15)
                {
                    emptiesBefore = chat.empties;
                    click(mc, CX, Y, CZ);
                }
                if (wait == 45)
                {
                    DevTestResults.check(name(), "crate: a second click stays empty", screen(mc) + ", chat \"" + chat.last + "\"",
                                         !screenOpen(mc) && chat.empties > emptiesBefore, "no screen, Decimation's empty line");
                    closeIfOpen(mc);
                    next(3);
                }
                break;
            case 3: // save and reload the world
                if (wait == 1)
                {
                    serve(() -> fileDeleted = worldFile(DevTestUtil.player().worldObj).delete()); // the load must write it again
                }
                if (wait == 5)
                {
                    DevTestResults.value(name(), "reload: the world's file deleted before leaving", fileDeleted);
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null); // Forge waits here until the integrated server stopped (and saved)
                    mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu()); // without a screen the next tick crashes
                }
                if (wait == 25)
                {
                    DevTestResults.value(name(), "reload: integrated server after leaving", mc.getIntegratedServer() == null ? "stopped" : "running");
                    mc.launchIntegratedServer(DevAutoTest.SAVE, DevAutoTest.SAVE, null);
                }
                if (wait > 25 && mc.theWorld != null && mc.thePlayer != null && DevTestUtil.player() != null)
                {
                    next(4);
                }
                if (wait > 25 + 1200)
                {
                    DevTestResults.check(name(), "reload: the world opens again", "not in 60 s", false, "the player back");
                    return end(mc);
                }
                break;
            case 4: // after the reload: the record came back from disk, Decimation's own registry is empty
                if (wait == 80)
                {
                    serve(this::afterReload);
                }
                if (wait == 100)
                {
                    emptiesBefore = chat.empties;
                    click(mc, CX, Y, CZ);
                }
                if (wait == 125)
                {
                    DevTestUtil.screenshot(mc, name(), "lootrules_empty_after_reload");
                }
                if (wait == 130)
                {
                    DevTestResults.check(name(), "crate: still empty after a save, a reload and a new game's empty registry", screen(mc) + ", chat \"" + chat.last + "\"",
                                         !screenOpen(mc) && chat.empties > emptiesBefore, "no screen, Decimation's empty line");
                    closeIfOpen(mc);
                    next(5);
                }
                break;
            case 5: // the shelf: clicked on its upper part, kept as its origin
                if (wait == 15)
                {
                    click(mc, SX, Y + 1, SZ);
                }
                if (wait > 15 && screenOpen(mc) || wait > 15 + TIMEOUT)
                {
                    DevTestResults.check(name(), "shelf: a click on its upper part opens it", screen(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    mc.thePlayer.closeScreen();
                    serve(() -> {
                        LootRefill.Record r = LootRefill.record(DevTestUtil.player().worldObj);
                        long origin = r.get(0, SX, Y, SZ), upper = r.get(0, SX, Y + 1, SZ);
                        DevTestResults.check(name(), "shelf: the record keeps its origin", "origin " + origin + ", upper part " + upper,
                                             origin >= 0 && upper < 0, "origin kept, the upper part not");
                    });
                    next(6);
                }
                break;
            case 6:
                if (wait == 15)
                {
                    emptiesBefore = chat.empties;
                    click(mc, SX, Y, SZ);
                }
                if (wait == 45)
                {
                    DevTestResults.check(name(), "shelf: its lower part is the same spot", screen(mc) + ", chat \"" + chat.last + "\"",
                                         !screenOpen(mc) && chat.empties > emptiesBefore, "no screen, Decimation's empty line");
                    closeIfOpen(mc);
                    next(7);
                }
                break;
            case 7: // a supply drop keeps Decimation's own rule (it goes when its screen closes)
                if (wait == 15)
                {
                    click(mc, DX, Y, DZ);
                }
                if (wait > 15 && screenOpen(mc) || wait > 15 + TIMEOUT)
                {
                    DevTestResults.check(name(), "supply drop: it opens", screen(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    mc.thePlayer.closeScreen();
                    next(8);
                }
                break;
            case 8:
                if (wait == 40)
                {
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        long at = LootRefill.record(w).get(0, DX, Y, DZ);
                        boolean gone = w.getBlock(DX, Y, DZ) != Deci.supplyDropBlock();
                        DevTestResults.check(name(), "supply drop: not in the record, gone after closing", "record " + at + ", gone " + gone,
                                             at < 0 && gone, "no record, gone");
                    });
                }
                if (wait > 40 && served)
                {
                    next(9);
                }
                break;
            case 9: // refill on a 1 minute setting: 55 s after its loot the crate is still empty
                if (wait == 1)
                {
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        LootRefill.use(new LootRefill.Settings(true, 1));
                        long at = w.getTotalWorldTime() - 1100;
                        LootRefill.record(w).put(0, CX, Y, CZ, at);
                        refillAt = at + 1200;
                    });
                }
                if (wait == 15)
                {
                    click(mc, CX, Y, CZ);
                }
                if (wait == 40)
                {
                    DevTestResults.check(name(), "refill on (1 minute): not yet 55 s after its loot", screen(mc), !screenOpen(mc),
                                         "no screen");
                    closeIfOpen(mc);
                    next(10);
                }
                break;
            case 10: // and full again once its minute passed
                if (clickedAt < 0 && refillAt > 0 && mc.theWorld.getTotalWorldTime() >= refillAt + 20)
                {
                    click(mc, CX, Y, CZ);
                    clickedAt = wait;
                }
                if (clickedAt >= 0 && (screenOpen(mc) || wait > clickedAt + TIMEOUT) || clickedAt < 0 && wait > 2400)
                {
                    DevTestResults.check(name(), "refill on (1 minute): full again after it", screen(mc), screenOpen(mc), Deci.LOOT_SCREEN);
                    mc.thePlayer.closeScreen();
                    serve(() -> {
                        World w = DevTestUtil.player().worldObj;
                        long at = LootRefill.record(w).get(0, CX, Y, CZ);
                        DevTestResults.check(name(), "refill on: the record moves to the new loot", "tick " + at, at >= refillAt,
                                             "at or after " + refillAt);
                    });
                    next(11);
                }
                break;
            case 11:
                if (wait == 5)
                {
                    serve(this::perf);
                }
                if (wait > 5 && served)
                {
                    next(12);
                }
                break;
            default:
                if (wait == 1)
                {
                    serve(this::cleanup);
                }
                if (wait > 1 && served)
                {
                    return end(mc);
                }
        }
        return true;
    }

    private boolean end(Minecraft mc)
    {
        MinecraftForge.EVENT_BUS.unregister(chat);
        DevAutoTest.keepScreen = false;
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

    /** A right click on the top of a block, as the player's own click sends it. */
    private static void click(Minecraft mc, int x, int y, int z)
    {
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), x, y, z, 1,
                                               net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y + 1.0, z + 0.5));
    }

    /** A screen a failed check left open must not shift the steps after it. */
    private static void closeIfOpen(Minecraft mc)
    {
        if (screenOpen(mc))
        {
            mc.thePlayer.closeScreen();
        }
    }

    private static boolean screenOpen(Minecraft mc)
    {
        return mc.currentScreen != null && mc.currentScreen.getClass().getName().equals(Deci.LOOT_SCREEN);
    }

    private static String screen(Minecraft mc)
    {
        return mc.currentScreen == null ? "no screen" : mc.currentScreen.getClass().getSimpleName();
    }

    private static File worldFile(World w)
    {
        return new File(w.getSaveHandler().getWorldDirectory(), LootRefill.FILE);
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

    private void setup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.setWorldTime(6000);
        DevTestArena.build(w, p);
        p.playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 0.5, 180, 30);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(WorldSettings.GameType.SURVIVAL); // Decimation's handler skips creative players
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
        Block crate = GameRegistry.findBlock("deci", "BlockMilitaryCrate");
        Block shelf = GameRegistry.findBlock("deci", "BlockMetalShelf");
        Block drop = Deci.supplyDropBlock();
        DevTestResults.check(name(), "loot blocks: crate, shelf, supply drop have lists",
                             Deci.hasLootPool(crate) + ", " + Deci.hasLootPool(shelf) + ", " + Deci.hasLootPool(drop),
                             Deci.hasLootPool(crate) && Deci.hasLootPool(shelf) && Deci.hasLootPool(drop), "true, true, true");
        w.setBlock(CX, Y, CZ, crate, 0, 3);
        w.setBlock(SX, Y, SZ, shelf, 0, 3);
        Deci.buildMultiblock(w, SX, Y, SZ, shelf, 0);
        w.setBlock(DX, Y, DZ, drop, 0, 3);
        int[] spot = Deci.lootSpot(w, SX, Y + 1, SZ);
        DevTestResults.check(name(), "shelf: its upper part leads to its origin", spot == null ? "none" : spot[0] + "," + spot[1] + "," + spot[2],
                             spot != null && spot[0] == SX && spot[1] == Y && spot[2] == SZ, SX + "," + Y + "," + SZ);
        LootRefill.clearAll(); // reruns in the open live world start with every spot ready
        LootRefill.Settings s = LootRefill.settings();
        DevTestResults.check(name(), "this singleplayer world's switch", s.toString(), !s.refill && s.minutes == 15,
                             "no refill (15 minutes kept for when it is turned on)");
        settingsAtStart = s;
    }

    private void afterReload()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        LootRefill.Record r = LootRefill.record(w);
        long at = r.get(0, CX, Y, CZ);
        DevTestResults.check(name(), "reload: the record read back from the world", "crate tick " + at + ", " + r.size() + " spots",
                             at == crateLooted && at >= 0, "tick " + crateLooted);
        // Decimation's map is static and its clear at server start never runs (an @Mod.EventHandler method put on the event
        // buses): a world reloaded in the same game keeps it, a new game starts empty. Emptied here as a new game has it.
        DevTestResults.value(name(), "reload: Decimation's own registry says ready (kept in memory in one game)",
                             Deci.lootReady(w, CX, Y, CZ));
        Deci.clearLootCooldowns();
        File f = worldFile(w);
        String text;
        try
        {
            text = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        }
        catch (Exception e)
        {
            text = e.toString();
        }
        DevTestResults.check(name(), "reload: the world's file written again at load", f.isFile() ? f.length() + " bytes" : "missing",
                             text.contains("B:refill=false") && text.contains("I:refillMinutes=15"), "refill=false, refillMinutes=15");
        LootRefill.Settings s = LootRefill.settings();
        DevTestResults.check(name(), "reload: the switch read from the file again", s.toString(), !s.refill && s.minutes == 15, "no refill");
        DevTestResults.check(name(), "reload: still survival", p.theItemInWorldManager.getGameType().getName(),
                             p.theItemInWorldManager.getGameType() == WorldSettings.GameType.SURVIVAL, "survival");
    }

    /** Our two handlers for one click (with Decimation's handler between them a click costs this plus its own), and a big record. */
    private void perf()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        LootRefill probe = new LootRefill();
        PlayerInteractEvent ev = new PlayerInteractEvent(p, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, CX, Y, CZ, 1, w);
        for (int i = 0; i < 3000; i++)
        {
            probe.before(ev);
            probe.after(ev);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < 5000; i++)
        {
            probe.before(ev);
            probe.after(ev);
        }
        double us = (System.nanoTime() - t0) / 5000.0 / 1000.0;
        DevTestResults.check(name(), "cost a click (our two handlers)", String.format("%.2f us", us), us < 20, "under 20 us");
        LootRefill.Record big = new LootRefill.Record("deciworldgen_looted_probe");
        for (int i = 0; i < 10000; i++)
        {
            big.put(0, i % 400 - 200, 40 + i % 9, i / 400 - 12, 5000000000L + i);
        }
        NBTTagCompound tag = new NBTTagCompound();
        t0 = System.nanoTime();
        big.writeToNBT(tag);
        double writeMs = (System.nanoTime() - t0) / 1e6;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            CompressedStreamTools.writeCompressed(tag, out);
        }
        catch (java.io.IOException e)
        {
            throw new RuntimeException(e);
        }
        LootRefill.Record back = new LootRefill.Record("deciworldgen_looted_probe");
        t0 = System.nanoTime();
        back.readFromNBT(tag);
        double readMs = (System.nanoTime() - t0) / 1e6;
        DevTestResults.check(name(), "10000 looted spots: written, read, file", String.format("%.1f ms, %.1f ms, %d KB", writeMs, readMs,
                             out.size() / 1024), writeMs < 50 && readMs < 50 && out.size() < 150 * 1024 && back.size() == 10000,
                             "under 50 ms each, under 150 KB, all 10000 back");
    }

    private void cleanup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        if (settingsAtStart != null)
        {
            LootRefill.use(settingsAtStart); // never written to the file
        }
        w.setBlockToAir(CX, Y, CZ);
        w.setBlockToAir(SX, Y + 1, SZ);
        w.setBlockToAir(SX, Y, SZ);
        w.setBlockToAir(DX, Y, DZ);
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
