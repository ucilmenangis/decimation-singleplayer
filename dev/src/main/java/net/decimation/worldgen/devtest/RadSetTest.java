package net.decimation.worldgen.devtest;

import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.decimation.fixes.GasMaskFilter;
import net.decimation.fixes.Net;
import net.decimation.fixes.RadiationDose;
import net.decimation.fixes.RadiationPills;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.ZoneStore;
import net.decimation.worldgen.city.LcCity;
import net.decimation.worldgen.city.Wasteland;
import net.decimation.worldgen.city.WastelandRadiation;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

/**
 * The radiation set (feature 7, user 22 Oktober 2026, docs/ideas.md 41 to 43): radiation pills, gas mask filters, the
 * wasteland city one radiation zone. On the arena with a radiation zone beside it (as the radiation test), survival,
 * natural regeneration and mob spawning off, every mob near the arena removed each second.
 * - Pills through the real right click (use key held): 90 % to 50 % after Decimation's 32 tick use, one pill gone, the HUD
 *   follows; 20 % to 0, the last pill leaves an empty slot; stack size, use time, pose and tab like Decimation's Pain Killers.
 * - Filter: no wear outside radiation, 1 a tick inside (200 ticks), the tag written at each percent and on leaving; an
 *   empty filter lets the dose rise like no mask and hurts like Decimation's zone damage, warnings at 10 % and empty,
 *   Decimation's HUD shows "Irradiated" (shot).
 * - Fitting through the real right click: a press looking at the floor (block and air events: one message) fills an empty
 *   filter and takes one filter; a full filter is refused; two forged requests in one tick use one filter; creative fills
 *   without using one; a stack of masks is refused; without a gas mask nothing is sent; a click on a loot crate opens the
 *   crate and fits nothing.
 * - Loot: one filter entry in the military and police lists, rarer than a weight 0.9 item, and the pills in the medical
 *   list; 10000 rolls of each crate match the weights.
 * - Wasteland: a district's zone call makes its MILITARY and RADIATION zones (the sector box in worlds with whole sector
 *   wasteland cities), Decimation's own check answers both kinds there, and the world load upgrade puts a removed
 *   radiation box back (worlds from before).
 * - Cost: our server tick, the bytes it allocates while a filter wears, the two HUD handlers a frame.
 */
public class RadSetTest extends DevTestMode
{
    public String name() { return "radset"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z, RX = X - 40;
    private static final int PILL = 1, FIT_DOWN = 2, FIT_AHEAD = 3, FORGE_TWO = 4, HUD = 5, SHOT = 6, HUD_PERCENT = 7,
        SHOT_PILLS = 8, CRATE = 9;

    private volatile int ask, ticks, expectedPercent;
    private volatile boolean done, serverDone;
    // client
    private int acting, actTicks, holdTicks;
    private boolean holding, sawUsing, finished;
    private long hiddenMark, framesMark;
    // server
    private ObjectZone rad;
    private boolean regenBefore = true;
    private String spawningBefore = "true";
    private WorldSettings.GameType modeBefore;
    private boolean watchPill, watchEmpty;
    private int prevDose, useStart, useCount, jumpAt, jumpDrop, jumpAfter, takenMark, emptyAt, doseAtEmpty;
    private float healthMark;
    private int[] counterMark;
    private long sentMark;
    private ItemStack mask;
    private final UseStart uses = new UseStart();
    private final double[] runMicros = new double[2];
    private long[] warm = new long[2];
    private int markTick;

    /** The use time an item use starts with (Forge's PlayerUseItemEvent.Start, server side), for the pills. */
    public static final class UseStart
    {
        volatile int duration = -1;

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onStart(net.minecraftforge.event.entity.player.PlayerUseItemEvent.Start e)
        {
            if (!e.entityPlayer.worldObj.isRemote && e.item != null && e.item.getItem() == RadiationPills.instance)
            {
                duration = e.duration;
            }
        }
    }

    // ------------------------------------------------------------ client

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        int a = ask;
        if (a != 0 && acting == 0)
        {
            ask = 0;
            acting = a;
            actTicks = 0;
        }
        if (acting != 0)
        {
            act(mc);
        }
        if (holding)
        {
            hold(mc);
        }
        if (serverDone && !finished)
        {
            finished = true;
            clientChecks(mc);
            done = true;
        }
        return !done;
    }

    private void act(Minecraft mc)
    {
        actTicks++;
        mc.thePlayer.inventory.currentItem = 0;
        switch (acting)
        {
            case PILL:
                if (actTicks == 1)
                {
                    mc.thePlayer.rotationPitch = 0f;
                }
                if (actTicks == 3)
                {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);   // held, as a player does
                    holding = true;
                    sawUsing = false;
                    holdTicks = 0;
                    DevTestUtil.rightClick(mc);
                    acting = 0;
                }
                break;
            case FIT_DOWN:
            case FIT_AHEAD:
                if (actTicks == 1)
                {
                    mc.thePlayer.rotationPitch = acting == FIT_DOWN ? 90f : 0f;   // the floor (block, then air) or nothing (air)
                }
                if (actTicks == 3)
                {
                    DevTestUtil.rightClick(mc);
                    acting = 0;
                }
                break;
            case CRATE:
                if (actTicks == 1)
                {
                    mc.thePlayer.rotationYaw = 0f;          // south, at the crate 2 blocks ahead on the floor
                    mc.thePlayer.rotationPitch = 29f;
                }
                if (actTicks == 3)
                {
                    DevTestUtil.rightClick(mc);
                    acting = 0;
                }
                break;
            case FORGE_TWO:
                Net.CHANNEL.sendToServer(new GasMaskFilter.FitMessage());
                Net.CHANNEL.sendToServer(new GasMaskFilter.FitMessage());
                acting = 0;
                break;
            case HUD:
                if (actTicks == 1)
                {
                    hiddenMark = GasMaskFilter.Client.hudHidden();
                    framesMark = GasMaskFilter.Client.hudFrames();
                }
                if (actTicks == 20)
                {
                    ItemStack m = Deci.playerMask(mc.thePlayer);
                    long hidden = GasMaskFilter.Client.hudHidden() - hiddenMark, frames = GasMaskFilter.Client.hudFrames() - framesMark;
                    DevTestResults.check(name(), "empty filter: Decimation's HUD sees no gas mask while it draws",
                        hidden + " of " + frames + " frames in 19 ticks, mask back after the draw " + (m != null) + ", the client's tag "
                            + GasMaskFilter.tagOf(m) + ", irradiated " + Deci.irradiated(mc.thePlayer),
                        frames > 0 && hidden == frames && m != null && GasMaskFilter.tagOf(m) == 0 && Deci.irradiated(mc.thePlayer),
                        "every frame, mask kept, client tag 0, irradiated");
                    acting = 0;
                }
                break;
            case SHOT:
            case SHOT_PILLS:
                if (actTicks == 1)
                {
                    mc.thePlayer.rotationPitch = 10f;
                    mc.gameSettings.hideGUI = false;
                }
                if (actTicks == 6)
                {
                    DevTestUtil.screenshot(mc, name(), acting == SHOT ? "empty_filter_hud" : "pills_hotbar");
                    acting = 0;
                }
                break;
            case HUD_PERCENT:
                if (actTicks == 5)
                {
                    int shown = RadiationDose.shown(), want = expectedPercent;
                    DevTestResults.check(name(), "pill: the HUD follows", shown + " on the HUD, " + want + " on the server",
                        Math.abs(shown - want) <= 1, "within 1");
                    acting = 0;
                }
                break;
            default:
                acting = 0;
        }
    }

    /** The use key stays down until the pill is taken (a player holds it), then goes up before another use starts. */
    private void hold(Minecraft mc)
    {
        holdTicks++;
        if (mc.thePlayer.isUsingItem())
        {
            sawUsing = true;
        }
        else if (sawUsing || holdTicks > 80)
        {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            holding = false;
        }
    }

    private void clientChecks(Minecraft mc)
    {
        ItemStack pills = new ItemStack(RadiationPills.instance);
        boolean icon = true;
        try
        {
            mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("deciworldgen",
                "textures/items/radiation_pills.png")).getInputStream().close();
        }
        catch (Exception e)
        {
            icon = false;
        }
        DevTestResults.check(name(), "pills: name and icon", pills.getDisplayName() + ", icon " + (icon ? "found" : "missing"),
            "Radiation Pills".equals(pills.getDisplayName()) && icon, "Radiation Pills, icon found");
        DevTestResults.value(name(), "HUD handlers", String.format("%.2f us a frame over %d frames (%d with the empty mask hidden)",
            GasMaskFilter.Client.averageHudMicros(), GasMaskFilter.Client.hudFrames(), GasMaskFilter.Client.hudHidden()));
        DevTestResults.check(name(), "HUD handlers cost", String.format("%.2f us", GasMaskFilter.Client.averageHudMicros()),
            GasMaskFilter.Client.hudFrames() > 100 && GasMaskFilter.Client.averageHudMicros() < 20,
            "under 20 us a frame, over 100 frames");
    }

    // ------------------------------------------------------------ server

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null || serverDone)
        {
            return;
        }
        World w = p.worldObj;
        if (t % 20 == 0)
        {
            sweep(w);
        }
        if (watchPill)
        {
            int d = RadiationDose.doseOf(p);
            if (useStart == 0 && p.isUsingItem())
            {
                useStart = t;
                useCount = p.getItemInUseCount();       // the use the item set, less this tick's player updates
            }
            if (jumpAt == 0 && prevDose - d >= 9000)
            {
                jumpAt = t;
                jumpDrop = prevDose - d;
                jumpAfter = d;
            }
            prevDose = d;
        }
        if (watchEmpty && emptyAt == 0 && GasMaskFilter.filterOf(p) == 0)
        {
            emptyAt = t;
            doseAtEmpty = RadiationDose.doseOf(p);
        }
        switch (t)
        {
            case 20: setup(p, w); break;
            case 30: pill(p, 90); ask = SHOT_PILLS; break;
            case 45: ask = PILL; break;
            case 110: pillChecks(p, 90); break;
            case 120: pill(p, 20); ask = PILL; break;
            case 190: pillChecks(p, 20); break;

            case 200:   // a fresh gas mask (no tag: full), outside radiation
                mask = new ItemStack(Deci.gasMask());
                Deci.setPlayerMask(p, mask);
                break;
            case 205:
                GasMaskFilter.measureAllocation(true);
                break;
            case 300:
                GasMaskFilter.measureAllocation(false);
                DevTestResults.value(name(), "allocation of the whole handler, gas mask on out of radiation", GasMaskFilter.allocation());
                DevTestResults.check(name(), "filter: no wear outside radiation", GasMaskFilter.filterOf(p) + " after 100 ticks",
                    GasMaskFilter.filterOf(p) == GasMaskFilter.full(), String.valueOf(GasMaskFilter.full()));
                p.setPositionAndUpdate(RX + 0.5, Y, Z + 0.5);
                RadiationDose.setDose(p, 0);
                healthMark = p.getHealth();
                break;
            case 304:   // in radiation now: the wearing path run 3000 times first (the JIT's first look at it), then a full filter
                warm = GasMaskFilter.measureTicks(p, 3000);
                GasMaskFilter.setFilter(p, GasMaskFilter.full());
                markTick = t;
                GasMaskFilter.measureAllocation(true);
                break;
            case 500:
                GasMaskFilter.measureAllocation(false);
                wearChecks(p);
                measure(p, true);
                p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
                break;
            case 510:
                DevTestResults.check(name(), "filter: the tag catches up out of radiation", "tag " + GasMaskFilter.tagOf(mask)
                    + ", count " + GasMaskFilter.filterOf(p), GasMaskFilter.tagOf(mask) == GasMaskFilter.filterOf(p), "equal");
                measure(p, false);
                break;

            case 515:   // just over 10 % (the low warning in 20 ticks), into radiation
                GasMaskFilter.setFilter(p, GasMaskFilter.full() / 10 + 20);
                counterMark = GasMaskFilter.counters();
                RadiationDose.setDose(p, 0);
                p.setHealth(p.getMaxHealth());
                Deci.player(p).setSafezoneGrace(0);
                healthMark = p.getHealth();
                watchEmpty = true;
                emptyAt = 0;
                p.setPositionAndUpdate(RX + 0.5, Y, Z + 0.5);
                break;
            case 550:   // nearly empty: used up in 40 ticks
                GasMaskFilter.setFilter(p, 40);
                break;
            case 630: ask = HUD; break;
            case 655: ask = SHOT; break;
            case 680: emptyChecks(p); break;
            case 685:
                p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
                p.setHealth(p.getMaxHealth());
                RadiationDose.setDose(p, 0);
                break;

            case 700:   // fit: a press looking at the floor, the worn filter empty
                p.inventory.setInventorySlotContents(0, new ItemStack(Deci.gasMaskFilter(), 2));
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = FIT_DOWN;
                break;
            case 730:
                fitCheck(p, "fit: an empty filter filled, one filter used, one message for the press", GasMaskFilter.full(), 1,
                    1, 0, 1);
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = FIT_AHEAD;
                break;
            case 760:
                fitCheck(p, "fit: a full filter is refused, nothing used", GasMaskFilter.full(), 1, 0, 1, 1);
                p.inventory.setInventorySlotContents(0, new ItemStack(Deci.gasMaskFilter(), 2));
                GasMaskFilter.setFilter(p, GasMaskFilter.full() / 2);
                break;
            case 770:
                counterMark = GasMaskFilter.counters();
                ask = FORGE_TWO;
                break;
            case 800:
            {
                int[] c = GasMaskFilter.counters();
                int dropped = c[2] - counterMark[2], refused = c[1] - counterMark[1];
                DevTestResults.check(name(), "fit: two requests in one tick use one filter", count(p) + " filters left, filter "
                    + GasMaskFilter.filterOf(p) + ", second request dropped " + dropped + " / refused full " + refused,
                    count(p) == 1 && GasMaskFilter.filterOf(p) == GasMaskFilter.full() && c[0] - counterMark[0] == 1
                        && dropped + refused == 1, "1 left, full, 1 fitted, the other dropped or refused");
                p.setGameType(WorldSettings.GameType.CREATIVE);
                p.inventory.setInventorySlotContents(0, new ItemStack(Deci.gasMaskFilter(), 1));
                GasMaskFilter.setFilter(p, GasMaskFilter.full() / 2);
                break;
            }
            case 810:
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = FIT_AHEAD;
                break;
            case 840:
                fitCheck(p, "fit in creative: filled, no filter used", GasMaskFilter.full(), 1, 1, 0, 1);
                p.setGameType(WorldSettings.GameType.SURVIVAL);
                p.capabilities.disableDamage = false;
                Deci.setPlayerMask(p, new ItemStack(Deci.gasMask(), 2));
                GasMaskFilter.setFilter(p, GasMaskFilter.full() / 2);
                break;
            case 855:
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = FIT_AHEAD;
                break;
            case 885:
                fitCheck(p, "fit into a stack of 2 masks: refused", GasMaskFilter.full() / 2, 1, 0, 1, 1);
                Deci.setPlayerMask(p, null);
                break;
            case 900:
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = FIT_AHEAD;
                break;
            case 930:
                DevTestResults.check(name(), "no gas mask: the click stays Decimation's, nothing sent", count(p) + " filters, "
                    + (GasMaskFilter.Client.sent() - sentMark) + " sent", count(p) == 1 && GasMaskFilter.Client.sent() == sentMark,
                    "1 filter, 0 sent");
                break;
            case 935:   // a loot crate in front: its click stays the crate's
                Deci.setPlayerMask(p, new ItemStack(Deci.gasMask()));
                GasMaskFilter.setFilter(p, GasMaskFilter.full() / 2);
                Deci.clearLootCooldowns();
                w.setBlock(X, Y, Z + 2, GameRegistry.findBlock("deci", "BlockMilitaryCrate"), 0, 3);
                break;
            case 945:
                counterMark = GasMaskFilter.counters();
                sentMark = GasMaskFilter.Client.sent();
                ask = CRATE;
                break;
            case 975:
                DevTestResults.check(name(), "a click on a loot crate stays the crate's", "crate looted " + !Deci.lootReady(w, X, Y, Z + 2)
                    + ", " + count(p) + " filters, filter " + GasMaskFilter.filterOf(p) + ", sent " + (GasMaskFilter.Client.sent() - sentMark),
                    !Deci.lootReady(w, X, Y, Z + 2) && count(p) == 1 && GasMaskFilter.filterOf(p) == GasMaskFilter.full() / 2
                        && GasMaskFilter.Client.sent() == sentMark, "looted, 1 filter, filter 6000, 0 sent");
                w.setBlockToAir(X, Y, Z + 2);
                break;
            case 980: loot(); break;
            case 985: wasteland(p, w); break;
            case 995: finish(p, w); break;
            default: break;
        }
    }

    private void setup(EntityPlayerMP p, World w)
    {
        DevTestArena.build(w, p);
        w.setWorldTime(6000);
        w.getWorldInfo().setRaining(false);
        w.getWorldInfo().setThundering(false);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.capabilities.disableDamage = false;
        regenBefore = w.getGameRules().getGameRuleBooleanValue("naturalRegeneration");
        w.getGameRules().setOrCreateGameRule("naturalRegeneration", "false");
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");   // no NPC may hurt the player meanwhile
        p.inventory.clearInventory(null, -1);
        p.inventory.currentItem = 0;
        Deci.setPlayerMask(p, null);
        Deci.Player d = Deci.player(p);
        d.setSafezoneGrace(0);
        d.setWater(20);
        d.setBleeding(false);
        d.setInfected(false);
        p.setHealth(p.getMaxHealth());
        RadiationDose.setDose(p, 0);
        DevTestResults.check(name(), "the arena is out of every radiation zone", ZoneStore.has(ZoneKind.RADIATION, X, Y, Z),
            !ZoneStore.has(ZoneKind.RADIATION, X, Y, Z), "false");
        rad = ZoneStore.add(ZoneKind.RADIATION, RX - 6, Y - 3, Z - 6, RX + 6, Y + 8, Z + 6);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(uses);
        Item pk = GameRegistry.findItem("deci", "ItemPainKillers");
        ItemStack ours = new ItemStack(RadiationPills.instance), theirs = new ItemStack(pk);
        String got = "stack " + RadiationPills.instance.getItemStackLimit() + ", use " + RadiationPills.instance.getMaxItemUseDuration(ours)
            + " ticks, " + RadiationPills.instance.getItemUseAction(ours) + ", tab same " + (RadiationPills.instance.getCreativeTab() == pk.getCreativeTab())
            + ", " + Item.itemRegistry.getNameForObject(RadiationPills.instance);
        DevTestResults.check(name(), "pills: like Decimation's Pain Killers", got,
            RadiationPills.instance.getItemStackLimit() == pk.getItemStackLimit()
                && RadiationPills.instance.getMaxItemUseDuration(ours) == pk.getMaxItemUseDuration(theirs)
                && RadiationPills.instance.getItemUseAction(ours) == pk.getItemUseAction(theirs)
                && RadiationPills.instance.getCreativeTab() == pk.getCreativeTab()
                && "deciworldgen:radiation_pills".equals(Item.itemRegistry.getNameForObject(RadiationPills.instance)),
            "stack " + pk.getItemStackLimit() + ", use " + pk.getMaxItemUseDuration(theirs) + " ticks, "
                + pk.getItemUseAction(theirs) + ", same tab, deciworldgen:radiation_pills");
    }

    /**
     * A pill at this dose: one pill in the hand (slot 0) and a spare in slot 1, the dose watched each tick. One in the hand:
     * a held button takes the next pill of the stack right after the first (vanilla's repeat, Decimation's pills too).
     */
    private void pill(EntityPlayerMP p, int percent)
    {
        p.inventory.setInventorySlotContents(0, new ItemStack(RadiationPills.instance, 1));
        p.inventory.setInventorySlotContents(1, new ItemStack(RadiationPills.instance, 1));
        RadiationDose.setDose(p, RadiationDose.max() * percent / 100);
        prevDose = RadiationDose.doseOf(p);
        useStart = jumpAt = 0;
        takenMark = RadiationPills.taken();
        watchPill = true;
    }

    private void pillChecks(EntityPlayerMP p, int percent)
    {
        watchPill = false;
        int max = RadiationDose.max(), now = RadiationDose.doseOf(p);
        int pctAfter = jumpAfter * 100 / max;
        if (percent == 90)
        {
            DevTestResults.check(name(), "pill at 90 %", "fell " + jumpDrop + " in one tick to " + pctAfter + " %",
                jumpAt > 0 && jumpDrop >= max * 40 / 100 && jumpDrop <= max * 40 / 100 + 2 && pctAfter >= 49 && pctAfter <= 50,
                "40 points (" + max * 40 / 100 + " +1 for the tick's fall), 49 to 50 %");
            DevTestResults.check(name(), "pill: Decimation's use time", "use started with " + uses.duration + " ticks (count "
                + useCount + " when first seen), effect " + (jumpAt - useStart) + " server ticks later",
                uses.duration == 32 && useStart > 0 && jumpAt > useStart, "32 (Pain Killers' use), the effect after it ran out");
            expectedPercent = now * 100 / max;
            ask = HUD_PERCENT;
        }
        else
        {
            DevTestResults.check(name(), "pill at 20 %: never below nothing", "dose " + now + " after the pill",
                jumpAt == 0 && now == 0 && RadiationPills.taken() == takenMark + 1, "0 (a drop of 4800 is no 40 point jump)");
        }
        ItemStack s = p.inventory.getStackInSlot(0), spare = p.inventory.getStackInSlot(1);
        DevTestResults.check(name(), "pill used up (" + percent + " %)", "hand " + (s == null ? "empty" : s.stackSize + " left") + ", spare "
            + (spare == null ? 0 : spare.stackSize) + ", taken " + (RadiationPills.taken() - takenMark),
            s == null && spare != null && spare.stackSize == 1 && RadiationPills.taken() == takenMark + 1,
            "hand empty (no stack of 0), spare 1, taken 1");
    }

    private void wearChecks(EntityPlayerMP p)
    {
        int worn = GasMaskFilter.full() - GasMaskFilter.filterOf(p), tag = GasMaskFilter.tagOf(mask), span = 500 - markTick;
        DevTestResults.check(name(), "filter: 1 a tick in radiation (12000 ticks = 10 minutes)", worn + " in " + span + " ticks, irradiated "
            + Deci.irradiated(p), worn >= span - 3 && worn <= span && Deci.irradiated(p), (span - 3) + " to " + span + ", irradiated");
        DevTestResults.value(name(), "warm up of the wearing path", warm[0] + " bytes in 3000 runs (tag writes included)");
        DevTestResults.check(name(), "filter: the mask protects meanwhile", "dose " + RadiationDose.doseOf(p) + ", health "
            + p.getHealth() + " of " + healthMark, RadiationDose.doseOf(p) == 0 && p.getHealth() == healthMark, "dose 0, no damage");
        DevTestResults.check(name(), "filter: the tag at each whole percent", "tag " + tag + ", count " + GasMaskFilter.filterOf(p),
            tag % 120 == 0 && tag >= GasMaskFilter.filterOf(p) && tag - GasMaskFilter.filterOf(p) < 120, "a multiple of 120, under 120 ahead");
        DevTestResults.value(name(), "allocation of the whole handler, the filter wearing", GasMaskFilter.allocation());
    }

    /**
     * Our per player tick run on the server thread, 3 batches of 1000 runs. Bytes beyond the tag writes (16 each, one each
     * whole percent) and the 32 byte result: the steady state is the smallest batch, since the JIT compiling a path again
     * (a path its code had not seen, a move to the faster compiler) costs a few hundred bytes once in whichever batch it
     * happens.
     */
    private void measure(EntityPlayerMP p, boolean exposed)
    {
        long least = Long.MAX_VALUE;
        double micros = Double.MAX_VALUE;
        StringBuilder all = new StringBuilder();
        for (int b = 0; b < 3; b++)
        {
            int before = GasMaskFilter.filterOf(p);
            long[] m = GasMaskFilter.measureTicks(p, 1000);
            int writes = exposed ? (before - 1) / 120 - (GasMaskFilter.filterOf(p) - 1) / 120 : 0;
            long extra = m[0] - 16L * writes - 32;
            least = Math.min(least, extra);
            micros = Math.min(micros, m[1] / 1000.0 / 1000);
            all.append(b == 0 ? "" : ", ").append(extra);
        }
        runMicros[exposed ? 0 : 1] = micros;
        DevTestResults.check(name(), "our tick's allocation (" + (exposed ? "in radiation" : "out of it") + ", 3 x 1000 runs)",
            "bytes beyond the tag writes: " + all + String.format("; %.2f us a run", micros), least <= 0,
            "0 in the steady batch");
    }

    private void emptyChecks(EntityPlayerMP p)
    {
        int[] c = GasMaskFilter.counters();
        int now = RadiationDose.doseOf(p), expected = 60 * (680 - emptyAt);
        watchEmpty = false;
        DevTestResults.check(name(), "empty filter: no protection", "empty at tick " + emptyAt + ", wears a gas mask "
            + Deci.wearsGasMask(p), emptyAt > 0 && !Deci.wearsGasMask(p), "empty, false");
        DevTestResults.check(name(), "empty filter: the dose rises as without a mask", (now - doseAtEmpty) + " in " + (680 - emptyAt)
            + " ticks", emptyAt > 0 && Math.abs(now - doseAtEmpty - expected) <= 60, expected + " (60 a tick)");
        float lost = healthMark - p.getHealth();
        DevTestResults.check(name(), "empty filter: Decimation's zone damage applied", lost + " in " + (680 - emptyAt) + " ticks (dose "
            + now * 100 / RadiationDose.max() + " %: none of ours yet)", lost >= 1 && lost <= 12 && now * 100 / RadiationDose.max() < 50,
            "1 to 12 (10 % a tick, the hurt cooldown)");
        DevTestResults.check(name(), "warnings: 10 % and used up, once each", "low " + (c[3] - counterMark[3]) + ", empty "
            + (c[4] - counterMark[4]), c[3] - counterMark[3] == 1 && c[4] - counterMark[4] == 1, "1 and 1");
    }

    /** One fit step: the filter, filters left in slot 0, fits, refusals and messages sent since the marks. */
    private void fitCheck(EntityPlayerMP p, String what, int filter, int left, int fits, int refusals, int sent)
    {
        int[] c = GasMaskFilter.counters();
        int f = GasMaskFilter.filterOf(p), n = count(p), fi = c[0] - counterMark[0], re = c[1] - counterMark[1];
        long se = GasMaskFilter.Client.sent() - sentMark;
        DevTestResults.check(name(), what, "filter " + f + ", " + n + " filters left, fitted " + fi + ", refused " + re + ", sent " + se,
            f == filter && n == left && fi == fits && re == refusals && se == sent,
            "filter " + filter + ", " + left + " left, fitted " + fits + ", refused " + refusals + ", sent " + sent);
    }

    private static int count(EntityPlayerMP p)
    {
        ItemStack s = p.inventory.getStackInSlot(0);
        return s == null || s.getItem() != Deci.gasMaskFilter() ? 0 : s.stackSize;
    }

    // ------------------------------------------------------------ loot

    private void loot()
    {
        Map<String, Map.Entry<Integer, List<ItemStack>>> table = Deci.lootTable();
        lootList(table, "BlockMilitaryCrate", Deci.gasMaskFilter(), true);
        lootList(table, "BlockPoliceCrate", Deci.gasMaskFilter(), true);
        lootList(table, "BlockMedicalCrate", RadiationPills.instance, false);
    }

    private void lootList(Map<String, Map.Entry<Integer, List<ItemStack>>> table, String block, Item item, boolean rare)
    {
        String what = block + " " + (item == RadiationPills.instance ? "pills" : "filters");
        Map.Entry<Integer, List<ItemStack>> e = table.get("deci:" + block);
        Block b = GameRegistry.findBlock("deci", block);
        if (e == null || b == null)
        {
            DevTestResults.check(name(), what, "no list", false, "a list");
            return;
        }
        int entries = 0;
        double total = 0, mine = 0;
        for (ItemStack s : e.getValue())
        {
            double weight = Deci.lootWeight(s);
            total += weight;
            if (s != null && s.getItem() == item)
            {
                entries++;
                mine += weight;
            }
        }
        double share = mine / total, common = 0.9 / total;
        InventoryBasic inv = new InventoryBasic("radset", false, 18);
        int opens = 10000, found = 0;
        for (int i = 0; i < opens; i++)
        {
            Deci.rollDecimationLoot(b, inv);
            for (int k = 0; k < inv.getSizeInventory(); k++)
            {
                ItemStack s = inv.getStackInSlot(k);
                found += s != null && s.getItem() == item ? 1 : 0;
            }
        }
        double expected = opens * (e.getKey() + 1) / 2.0 * share;      // 1 .. n rolls an open (PatchLootRolls)
        DevTestResults.check(name(), what, String.format("%d entry, %.2f %% a roll (a weight 0.9 item %.2f %%), %d in %d opens",
            entries, 100 * share, 100 * common, found, opens),
            entries == 1 && Math.abs(found - expected) <= 4 * Math.sqrt(expected) + 3 && (!rare || share < common),
            String.format("1 entry, %.0f found (4 sigma)%s", expected, rare ? ", rarer than a weight 0.9 item" : ""));
    }

    // ------------------------------------------------------------ wasteland

    private void wasteland(EntityPlayerMP p, World w)
    {
        long seed = w.getSeed();
        int[] g = district(seed);
        if (g == null)
        {
            DevTestResults.check(name(), "wasteland district", "none within 80 cells", false, "one");
            return;
        }
        boolean sectors = Wasteland.sectors();
        int c = LcCity.CELL * 16, x0 = g[0] * c, z0 = g[1] * c, mx = x0 + c, mz = z0 + c;
        DevTestResults.value(name(), "wasteland district", g[0] + "," + g[1] + " (blocks " + x0 + "," + z0 + " to " + (x0 + 2 * c - 1)
            + "," + (z0 + 2 * c - 1) + "), whole sector cities " + sectors);
        Wasteland.zone(w, g[0], g[1]);                               // what a chunk population near it does
        int sx = Math.floorDiv(g[0], 4) * 256, sz = Math.floorDiv(g[1], 4) * 256;
        boolean all = ZoneStore.has(ZoneKind.RADIATION, mx, 70, mz);
        if (sectors)
        {
            for (int[] q : new int[][] {{sx, 0, sz}, {sx + 255, 255, sz + 255}, {sx + 64, 70, sz + 192}, {sx + 192, 70, sz + 64}})
            {
                all &= ZoneStore.has(ZoneKind.RADIATION, q[0], q[1], q[2]);
            }
        }
        int boxes = 0;
        for (int[] z : ZoneStore.boxes(ZoneKind.RADIATION))
        {
            boxes += z[0] <= mx && mx <= z[3] && z[2] <= mz && mz <= z[5] ? 1 : 0;
        }
        DevTestResults.check(name(), "wasteland: military and radiation zones", "military " + ZoneStore.has(ZoneKind.MILITARY, mx, 70, mz)
            + ", radiation over " + (sectors ? "the whole sector " : "the district ") + all + ", radiation boxes there " + boxes,
            ZoneStore.has(ZoneKind.MILITARY, mx, 70, mz) && all && boxes == 1, "both, one radiation box over the city");
        double px = p.posX, py = p.posY, pz = p.posZ;
        p.posX = mx + 0.5;
        p.posY = 80;
        p.posZ = mz + 0.5;
        boolean inRad = Deci.isPlayerInZone(p, ZoneKind.RADIATION), inMil = Deci.isPlayerInZone(p, ZoneKind.MILITARY);
        p.posX = px;
        p.posY = py;
        p.posZ = pz;
        DevTestResults.check(name(), "wasteland: Decimation's zone check answers both kinds", "radiation " + inRad + ", military " + inMil,
            inRad && inMil, "true, true");
        int[] rb = null;
        for (int[] z : ZoneStore.boxes(ZoneKind.RADIATION))
        {
            if (z[0] <= mx && mx <= z[3] && z[2] <= mz && mz <= z[5])
            {
                rb = z;
            }
        }
        boolean gone = rb != null && ZoneStore.removeBox(ZoneKind.RADIATION, rb[0], rb[1], rb[2], rb[3], rb[4], rb[5])
            && !ZoneStore.has(ZoneKind.RADIATION, mx, 70, mz);
        int added = WastelandRadiation.upgrade(w);
        DevTestResults.check(name(), "wasteland: an older world gets its radiation zone at load", "removed " + gone + ", upgrade added "
            + added + ", back " + ZoneStore.has(ZoneKind.RADIATION, mx, 70, mz), gone && added >= 1
            && ZoneStore.has(ZoneKind.RADIATION, mx, 70, mz), "removed, added, back");
    }

    /** The nearest wasteland district (its first cell, even) to the origin: all 4 cells city cells of the deadzone style. */
    private static int[] district(long seed)
    {
        for (int r = 0; r <= 40; r++)
        {
            for (int i = -r; i <= r; i++)
            {
                for (int j = -r; j <= r; j++)
                {
                    if (Math.max(Math.abs(i), Math.abs(j)) != r)
                    {
                        continue;
                    }
                    int gx = 2 * i, gz = 2 * j;
                    if (LcCity.isCity(seed, gx, gz) && LcCity.isCity(seed, gx + 1, gz + 1)
                        && LcCity.style(seed, gx, gz).endsWith(":deadzone"))
                    {
                        return new int[] {gx, gz};
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------ end

    private void finish(EntityPlayerMP p, World w)
    {
        DevTestResults.value(name(), "server tick, whole handler", String.format("%.2f us a tick average (wall time since start)",
            GasMaskFilter.averageTickMicros()));
        DevTestResults.check(name(), "server tick cost a player", String.format("%.2f us in radiation, %.2f us out of it",
            runMicros[0], runMicros[1]), runMicros[0] < 5 && runMicros[1] < 5, "under 5 us (budget 50 us a tick)");
        int[] c = GasMaskFilter.counters();
        DevTestResults.value(name(), "counters", "fitted " + c[0] + ", refused " + c[1] + ", dropped " + c[2] + ", low warnings "
            + c[3] + ", empty warnings " + c[4] + ", tag writes " + c[5]);
        p.inventory.clearInventory(null, -1);
        Deci.setPlayerMask(p, null);
        RadiationDose.setDose(p, 0);
        p.setHealth(p.getMaxHealth());
        ZoneStore.remove(rad);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(uses);
        w.getGameRules().setOrCreateGameRule("naturalRegeneration", String.valueOf(regenBefore));
        w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);
        }
        serverDone = true;
    }

    /** Removes every mob within 120 blocks of the arena (in the production world a survivor NPC shot the test player). */
    private static void sweep(World w)
    {
        for (Object o : w.loadedEntityList)
        {
            net.minecraft.entity.Entity e = (net.minecraft.entity.Entity) o;
            if (!e.isDead && e instanceof net.minecraft.entity.EntityLiving
                && Math.abs(e.posX - X) < 120 && Math.abs(e.posZ - Z) < 120)
            {
                e.setDead();
            }
        }
    }
}
