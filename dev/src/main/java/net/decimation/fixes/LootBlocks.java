package net.decimation.fixes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/**
 * Lootable copies of Decimation's props and loot kinds of shelves and cabinets (feature 12 B,
 * docs/superpowers/specs/2026-10-22-loot-rules-design.md; user, 22 Oktober 2026: "props later can bee looted like truck,
 * wooden crate, watter pallet, military radio and small military radio (they have 2 version non loot and loot)", "lootable
 * is just copy of block the props i say then make lootable version ... so later when we make world city props, we can
 * placed 2 version props non loot and lootable"; "shelf can be civilian loot or military loot or police loot, weapon
 * cabinet now have 2 version its civilian loot with name cabinet (without weapon word) and police loot and military
 * loot").
 *
 * Each copy is a block of ours made from Decimation's own class (Deci.copyProp, newMetalShelf, newWeaponCabinet: the same
 * look, size, multiblock behaviour and display name) and bound to a Decimation list through a loot table entry of its own
 * made from the entry of a block that has that list (Deci.addLootBlock: its rolls, sound and items). Decimation's handler
 * and its loot hint then treat the copy as any loot block. Our world generation picks a shelf's and a cabinet's version by
 * the building (forPlace): police buildings police, military buildings, the wasteland and bases military, the rest
 * civilian. Worlds already generated keep what they have.
 */
public final class LootBlocks
{
    /** Kinds of places for forPlace. */
    public static final int CIVILIAN = 0, POLICE = 1, MILITARY = 2;

    static final String SHELF = "deci:BlockMetalShelf", CABINET = "deci:BlockWeaponCabinet";

    /** How a copy is made. */
    enum Make { PROP, SHELF, CABINET }

    /** One copy: our registry name, the Decimation block it copies, the block whose list it takes (null: none), its tooltip. */
    static final class Copy
    {
        final String name, original, like, tip;
        final Make make;

        Copy(String name, Make make, String original, String like, String tip)
        {
            this.name = name;
            this.make = make;
            this.original = original;
            this.like = like;
            this.tip = tip;
        }
    }

    private static final String WRECK = "deci:BlockWreckage1", DRINKS = "deci:BlockVendingMachine_1",
        MILITARY_CRATE = "deci:BlockMilitaryCrate", POLICE_CRATE = "deci:BlockPoliceCrate";

    static final List<Copy> COPIES;

    static
    {
        List<Copy> c = new ArrayList<Copy>();
        for (int i = 1; i <= 6; i++)
        {
            c.add(new Copy("truckwreckage" + i + "_loot", Make.PROP, "deci:BlockTruckWreckage" + i, WRECK, "car wreck loot"));
        }
        c.add(new Copy("waterpallet_loot", Make.PROP, "deci:BlockWaterPallet", DRINKS, "food and drink loot"));
        c.add(new Copy("waterpallettarp_loot", Make.PROP, "deci:BlockWaterPalletTarp", DRINKS, "food and drink loot"));
        c.add(new Copy("militaryradio_loot", Make.PROP, "deci:BlockMilitaryRadio", MILITARY_CRATE, "military loot"));
        c.add(new Copy("militaryradiosmall_loot", Make.PROP, "deci:BlockMilitaryRadioSmall", MILITARY_CRATE, "military loot"));
        c.add(new Copy("woodcrate_plain", Make.PROP, "deci:BlockWoodCrate", null, null));
        c.add(new Copy("shelf_police", Make.SHELF, SHELF, POLICE_CRATE, "police loot"));
        c.add(new Copy("shelf_military", Make.SHELF, SHELF, MILITARY_CRATE, "military loot"));
        c.add(new Copy("cabinet", Make.CABINET, CABINET, SHELF, "civilian loot"));
        c.add(new Copy("cabinet_police", Make.CABINET, CABINET, POLICE_CRATE, "police loot"));
        COPIES = Collections.unmodifiableList(c);
    }

    /** Tooltip lines of the originals whose kind now matters (the copies carry their own). */
    static final Map<String, String> ORIGINAL_TIPS = new HashMap<String, String>();

    static
    {
        ORIGINAL_TIPS.put(SHELF, "civilian loot");
        ORIGINAL_TIPS.put(CABINET, "military loot");
    }

    private LootBlocks()
    {
    }

    /** The registry name our world generation places for a shelf or a cabinet in this kind of place; any other name as is. */
    static String forPlace(String name, int place)
    {
        if (SHELF.equals(name))
        {
            return place == POLICE ? "deciworldgen:shelf_police" : place == MILITARY ? "deciworldgen:shelf_military" : name;
        }
        if (CABINET.equals(name))
        {
            return place == POLICE ? "deciworldgen:cabinet_police" : place == CIVILIAN ? "deciworldgen:cabinet" : name;
        }
        return name;
    }

    private static Block shelf, cabinet;
    private static Block[] shelves = new Block[3], cabinets = new Block[3];
    private static final Map<Block, String> TIPS = new HashMap<Block, String>();
    private static final List<Block> MADE = new ArrayList<Block>();

    /** The block to place for a shelf or a cabinet in this kind of place (two reference compares for anything else). */
    public static Block forPlace(Block b, int place)
    {
        if (b == null || b != shelf && b != cabinet || place < 0 || place > 2)
        {
            return b;
        }
        Block r = b == shelf ? shelves[place] : cabinets[place];
        return r == null ? b : r;
    }

    /** The copies made (tests). */
    public static List<Block> made()
    {
        return Collections.unmodifiableList(MADE);
    }

    /** Copies and their loot entries; preInit, after Decimation's blocks and its loot table exist. */
    public static void register()
    {
        int bound = 0;
        for (Copy c : COPIES)
        {
            Block original = Block.getBlockFromName(c.original);
            if (original == null || original == Blocks.air)
            {
                FMLLog.warning("[deciworldgen] loot copy %s: no %s", c.name, c.original);
                continue;
            }
            Block b = c.make == Make.PROP ? Deci.copyProp(original)
                : c.make == Make.SHELF ? Deci.newMetalShelf(original) : Deci.newWeaponCabinet(original);
            if ("cabinet".equals(c.name))
            {
                b.setBlockName("deciworldgen.cabinet"); // "Cabinet", the user's name for the civilian one
            }
            GameRegistry.registerBlock(b, c.name);
            MADE.add(b);
            TIPS.put(b, c.tip);
            Block like = c.like == null ? null : Block.getBlockFromName(c.like);
            if (like != null && like != Blocks.air && Deci.addLootBlock(b, like))
            {
                bound++;
            }
        }
        shelf = Block.getBlockFromName(SHELF);
        cabinet = Block.getBlockFromName(CABINET);
        for (int p = 0; p < 3; p++)
        {
            shelves[p] = Block.getBlockFromName(forPlace(SHELF, p));
            cabinets[p] = Block.getBlockFromName(forPlace(CABINET, p));
        }
        for (Map.Entry<String, String> e : ORIGINAL_TIPS.entrySet())
        {
            Block o = Block.getBlockFromName(e.getKey());
            if (o != null && o != Blocks.air)
            {
                TIPS.put(o, e.getValue());
            }
        }
        FMLLog.info("[deciworldgen] loot copies: %d made, %d with a loot list", MADE.size(), bound);
    }

    /** "Lootable: police loot" under the name of a copy or of an original shelf or cabinet (client). */
    public static final class Tooltip
    {
        @SubscribeEvent
        public void onTooltip(ItemTooltipEvent e)
        {
            if (e.itemStack == null || !(e.itemStack.getItem() instanceof ItemBlock))
            {
                return;
            }
            Block b = Block.getBlockFromItem(e.itemStack.getItem());
            if (!TIPS.containsKey(b))
            {
                return;
            }
            String tip = TIPS.get(b);
            e.toolTip.add(EnumChatFormatting.GRAY + (tip == null ? "Not lootable" : "Lootable: " + tip));
        }
    }

    /** Every name this class knows (tests). */
    static List<String> names()
    {
        List<String> n = new ArrayList<String>();
        for (Copy c : COPIES)
        {
            n.add(c.name);
        }
        n.addAll(Arrays.asList(SHELF, CABINET));
        return n;
    }
}
