package net.decimation.fixes;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Our loot lists (docs/systems_plan.md 2.2, docs/loot_levels.md section 3.2): named lists of items in
 * config/deciworldgen_loot.json, and a roller that fills an inventory from one. Used by crates of
 * every kind and level, NPC bodies, supply drops, stashes.
 *
 * A list: "rolls" [min, max] items per open (both inclusive), "refill" minutes, "first" lists rolled
 * once each before the rest (level 4 takes one item of every type), "items" entries with "item"
 * (registry name like deci:ak74 or deciworldgen:mac10, or "@list" for one item rolled from another
 * list), "meta", "weight" (common 10, uncommon 4, rare 1) and "count" [min, max]. Rarity is set per
 * list here, not by the item's own loot chance as Decimation does.
 *
 * The file is written with the built-in lists when missing or older than VERSION (the old one is kept
 * as .old); after that it is the user's: edit, then restart. Unknown item names are skipped and logged.
 */
public class LootPools
{
    /** Bump when the built-in lists change: an older file is replaced (kept as .old). */
    public static final int VERSION = 2;      // 2: the RK-1 and the SpecterDR (21 Oktober 2026)
    private static final String FILE = "deciworldgen_loot.json";

    public static class Entry
    {
        public String item;
        public Integer meta;
        public int weight = 10;
        public int[] count = {1, 1};
        transient Item resolved;
    }

    public static class Pool
    {
        public int[] rolls = {1, 1};
        public int refill = 15;
        public List<String> first;
        public List<Entry> items = new ArrayList<Entry>();
    }

    static class FileData
    {
        int version;
        Map<String, Pool> lists = new LinkedHashMap<String, Pool>();
    }

    private static final Map<String, Pool> pools = new LinkedHashMap<String, Pool>();
    /** What did not resolve at the last load ("list: name"). */
    public static final List<String> problems = new ArrayList<String>();

    public static synchronized void load(File configDir)
    {
        File f = new File(configDir, FILE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileData data = null;
        if (f.isFile())
        {
            try
            {
                Reader in = new FileReader(f);
                data = gson.fromJson(in, FileData.class);
                in.close();
            }
            catch (Exception e)
            {
                FMLLog.info("[deciworldgen] %s unreadable, built-in loot lists used: %s", f, e);
            }
        }
        if (data == null || data.version < VERSION || data.lists == null)
        {
            if (f.isFile())
            {
                File old = new File(configDir, FILE + ".old");
                old.delete();
                f.renameTo(old);
            }
            data = defaults();
            try
            {
                Writer out = new FileWriter(f);
                out.write(gson.toJson(data));
                out.close();
            }
            catch (Exception e)
            {
                FMLLog.info("[deciworldgen] could not write %s: %s", f, e);
            }
        }
        pools.clear();
        pools.putAll(data.lists);
        problems.clear();
        for (Map.Entry<String, Pool> e : pools.entrySet())
        {
            Pool p = e.getValue();
            for (Entry en : new ArrayList<Entry>(p.items))
            {
                if (en.item != null && en.item.startsWith("@"))
                {
                    if (!pools.containsKey(en.item.substring(1)))
                    {
                        problems.add(e.getKey() + ": " + en.item);
                        p.items.remove(en);
                    }
                    continue;
                }
                en.resolved = en.item == null ? null : (Item) Item.itemRegistry.getObject(en.item);
                if (en.resolved == null)
                {
                    problems.add(e.getKey() + ": " + en.item);
                    p.items.remove(en);
                }
            }
            if (p.first != null)
            {
                for (String s : new ArrayList<String>(p.first))
                {
                    if (!pools.containsKey(s))
                    {
                        problems.add(e.getKey() + ": first " + s);
                        p.first.remove(s);
                    }
                }
            }
        }
        for (String s : problems)
        {
            FMLLog.info("[deciworldgen] loot list entry skipped (unknown): %s", s);
        }
        FMLLog.info("[deciworldgen] loot lists loaded: %d lists, %d entries skipped", pools.size(), problems.size());
    }

    public static synchronized Set<String> names()
    {
        return new LinkedHashSet<String>(pools.keySet());
    }

    public static synchronized Pool pool(String name)
    {
        return pools.get(name);
    }

    /** Items of one open of the list (empty for an unknown list). */
    public static synchronized List<ItemStack> roll(String name, Random r)
    {
        List<ItemStack> out = new ArrayList<ItemStack>();
        Pool p = pools.get(name);
        if (p == null)
        {
            return out;
        }
        int n = between(p.rolls, r);
        if (p.first != null)
        {
            for (String s : p.first)
            {
                add(out, one(pools.get(s), r, 0));
            }
        }
        for (int guard = 0; out.size() < n && guard < n * 4; guard++)
        {
            add(out, one(p, r, 0));
        }
        return out;
    }

    /**
     * Rolls the list into random distinct slots of `inv` and empties the other slots (like Decimation's
     * LootPool.fillInventory); items beyond the inventory size are dropped from the roll.
     */
    public static void fill(String name, IInventory inv, Random r)
    {
        List<ItemStack> items = roll(name, r);
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            inv.setInventorySlotContents(i, null);
            slots.add(i);
        }
        java.util.Collections.shuffle(slots, r);
        for (int i = 0; i < items.size() && i < slots.size(); i++)
        {
            inv.setInventorySlotContents(slots.get(i), items.get(i));
        }
    }

    private static void add(List<ItemStack> out, ItemStack s)
    {
        if (s != null)
        {
            out.add(s);
        }
    }

    /** One weighted item of a list; "@list" entries roll one item of that list. */
    private static ItemStack one(Pool p, Random r, int depth)
    {
        if (p == null || p.items.isEmpty() || depth > 4)
        {
            return null;
        }
        int total = 0;
        for (Entry e : p.items)
        {
            total += Math.max(0, e.weight);
        }
        if (total <= 0)
        {
            return null;
        }
        int k = r.nextInt(total);
        for (Entry e : p.items)
        {
            k -= Math.max(0, e.weight);
            if (k < 0)
            {
                if (e.item.startsWith("@"))
                {
                    return one(pools.get(e.item.substring(1)), r, depth + 1);
                }
                int n = Math.min(between(e.count, r), e.resolved.getItemStackLimit());
                return new ItemStack(e.resolved, Math.max(1, n), e.meta == null ? 0 : e.meta);
            }
        }
        return null;
    }

    private static int between(int[] range, Random r)
    {
        if (range == null || range.length == 0)
        {
            return 1;
        }
        int lo = range[0], hi = range.length > 1 ? range[1] : range[0];
        return hi <= lo ? lo : lo + r.nextInt(hi - lo + 1);
    }

    // ---- the built-in lists (docs/loot_levels.md section 3.2, draft for the user's review)

    private static final int COMMON = 10, UNCOMMON = 4, RARE = 1;

    static FileData defaults()
    {
        FileData d = new FileData();
        d.version = VERSION;
        String[] milCommon = {"deci:ak74", "deci:akm", "deci:m4a4", "deci:m16a2", "deci:sks", "deci:g36c", "deci:famas", "deci:aug1",
                              "deci:fal", "deci:mp5a3", "deci:ump45", "deci:uzi", "deciworldgen:mac10"};
        String[] milUncommon = {"deciworldgen:hk416", "deciworldgen:mk18", "deci:acr", "deci:fnscar", "deci:fnscarl", "deci:fnscarsc",
                                "deci:fnscarhamr", "deci:fnscarssr", "deci:ak12", "deci:asval", "deci:m110", "deci:svd"};
        String[] milRare = {"deci:barrett", "deci:pkm", "deci:m240", "deci:mk48", "deci:rpg7", "deci:rpg18", "deci:m79"};

        Pool m1 = pool(d, "military_1", 2, 3, 15);
        add(m1, COMMON, 1, 2, mags(milCommon));
        add(m1, UNCOMMON, 1, 1, mags(milUncommon));
        add(m1, RARE, 1, 1, mags(milRare));
        add(m1, RARE, 1, 1, "deciworldgen:stanag60Mag", "deciworldgen:ak60Mag");
        add(m1, COMMON, 1, 1, "deci:5_56mm_box", "deci:7_62x39mm_box", "deci:7_62x51mm_box", "deci:9x19mm_box", "deci:12gauge_box",
            "deci:5_45x39mm_box");
        Pool m2 = pool(d, "military_2", 1, 2, 30);
        add(m2, COMMON, 1, 1, "deci:reddot", "deci:arSuppressor", "deci:smgSuppressor", "deci:pistolSuppressor", "deci:foregrip",
            "deci:rk1", "deci:laser", "deci:flashlight");
        add(m2, UNCOMMON, 1, 1, "deci:eotech558", "deci:ta11acog", "deci:2x", "deci:mgSuppressor", "deci:shotgunSuppressor",
            "deci:bayonet");
        add(m2, RARE, 1, 1, "deci:4x", "deci:specterdr", "deci:8x");
        Pool m3 = pool(d, "military_3", 1, 1, 60);
        add(m3, COMMON, 1, 1, milCommon);
        add(m3, UNCOMMON, 1, 1, milUncommon);
        add(m3, RARE, 1, 1, milRare);
        add(m3, RARE, 1, 2, "deci:rpg7Rocket", "deci:rpg18Rocket", "deci:46mmGrenade");
        Pool m4 = pool(d, "military_4", 4, 6, 120);
        m4.first = new ArrayList<String>(Arrays.asList("military_1", "military_2", "military_3"));
        add(m4, COMMON, 1, 1, "@military_1", "@military_2", "@military_3");
        add(m4, UNCOMMON, 1, 1, "deci:ItemPlateCarrier_Beige", "deci:ItemPlateCarrier_Gray", "deci:ItemPlateCarrier_Green",
            "deci:militaryvest", "deci:ItemMilitaryKeycard");
        add(m4, RARE, 1, 1, "deci:spetsnazHelm", "deci:spetsnazVest", "deci:spetsnazPants", "deci:spetsnazBoots", "deci:sasHelm",
            "deci:sasVest", "deci:sasPants", "deci:sasBoots", "deci:juggernautHelm", "deci:juggernautVest", "deci:juggernautPants",
            "deci:juggernautBoots");

        String[] polCommon = {"deci:glock17", "deci:barettam9", "deci:r870"};
        String[] polUncommon = {"deci:spas12", "deci:benellim4", "deci:mp5a3", "deciworldgen:ump9"};
        Pool p1 = pool(d, "police_1", 2, 3, 15);
        add(p1, COMMON, 1, 2, mags(polCommon));
        add(p1, UNCOMMON, 1, 1, mags(polUncommon));
        add(p1, COMMON, 1, 1, "deci:9x19mm_box", "deci:12gauge_box");
        Pool p2 = pool(d, "police_2", 1, 2, 30);
        add(p2, COMMON, 1, 1, "deci:policevest", "deci:nypdHelm", "deci:nypdCoat", "deci:nypdShirt", "deci:nypdPants", "deci:nypdBoots",
            "deci:ItemPoliceBaton");
        add(p2, UNCOMMON, 1, 2, "deci:ItemStunGrenade", "deci:ItemTearGasGrenade", "deci:ItemSmokeGrenade");
        add(p2, RARE, 1, 1, "deci:ItemSecurityKeycard", "deci:bulletproofvest");
        Pool p3 = pool(d, "police_3", 1, 1, 60);
        add(p3, COMMON, 1, 1, polCommon);
        add(p3, UNCOMMON, 1, 1, polUncommon);
        Pool p4 = pool(d, "police_4", 4, 6, 120);
        p4.first = new ArrayList<String>(Arrays.asList("police_1", "police_2", "police_3"));
        add(p4, COMMON, 1, 1, "@police_1", "@police_2", "@police_3");
        add(p4, UNCOMMON, 1, 1, "deci:pistolSuppressor", "deci:shotgunSuppressor");

        Pool d1 = pool(d, "medical_1", 2, 3, 15);
        add(d1, COMMON, 1, 2, "deci:ItemBandage", "deci:ItemRag", "deci:ItemVitaminCapsules", "deci:ItemSyringeEmpty");
        add(d1, UNCOMMON, 1, 2, "deci:ItemBandageDirty", "deci:ItemRagDirty");
        Pool d2 = pool(d, "medical_2", 1, 2, 30);
        add(d2, COMMON, 1, 1, "deci:ItemPainKillers", "deci:ItemSplint");
        add(d2, UNCOMMON, 1, 1, "deci:ItemSyringeMorphine", "deci:ItemSyringeAdrenaline");
        Pool d3 = pool(d, "medical_3", 1, 1, 60);
        add(d3, COMMON, 1, 1, "deci:ItemFirstAidKit", "deci:ItemEmptyBloodBag");
        add(d3, UNCOMMON, 1, 1, "deci:ItemBloodBag", "deci:ItemSyringeBlood");
        add(d3, RARE, 1, 1, "deci:ItemSyringeCure", "deci:ItemLaboratoryKeycard");
        Pool d4 = pool(d, "medical_4", 4, 6, 120);
        d4.first = new ArrayList<String>(Arrays.asList("medical_1", "medical_2", "medical_3"));
        add(d4, COMMON, 1, 1, "@medical_1", "@medical_2", "@medical_3");
        add(d4, UNCOMMON, 1, 1, "deci:gasmask", "deci:gasmaskmilitary");
        add(d4, RARE, 1, 1, "deci:hazmatHelm", "deci:hazmatVest", "deci:hazmatPants", "deci:hazmatBoots");

        Pool c1 = pool(d, "civilian_1", 2, 3, 15);
        add(c1, COMMON, 1, 2, "deci:ItemCannedTuna", "deci:ItemCannedSweetcorn", "deci:ItemCannedPeach", "deci:ItemCannedPear",
            "deci:ItemCannedMacncheese", "deci:ItemCannedCarrot", "deci:ItemCannedMushroom", "deci:ItemCannedApplesauce",
            "deci:ItemCrispsBBQ", "deci:ItemCrispsCoolranch", "deci:ItemCrispsNachocheese", "deci:ItemBarChocolate",
            "deci:ItemBarEnergyApple", "deci:ItemBottledWater", "deci:ItemBottledCoke", "deci:ItemBottledLemonade",
            "deci:ItemDrinkCanWater", "deci:ItemDrinkCanEnergy");
        add(c1, UNCOMMON, 1, 1, "deci:ItemCannedCustard", "deci:ItemCannedGrape", "deci:ItemBottledWhiskey");
        add(c1, RARE, 1, 1, "deci:ItemMRE");
        Pool c2 = pool(d, "civilian_2", 1, 2, 30);
        add(c2, COMMON, 1, 1, "deci:ItemScrapDuctTape", "deci:ItemScrapScrewdriver", "deci:ItemScrapPliers", "deci:ItemScrapWD40",
            "deci:ItemScrapWrench", "deci:ItemScrapPropaneTank", "deci:ItemWire");
        add(c2, UNCOMMON, 1, 1, "deci:ItemLockpick", "deci:ItemJerryCan", "deci:ItemRadio", "deci:ItemMapDeci",
            "deci:ItemScrapWaterFilter");
        Pool c3 = pool(d, "civilian_3", 1, 1, 60);
        add(c3, COMMON, 1, 1, "deci:ItemFannyPack_Blue", "deci:ItemFannyPack_Green", "deci:ItemHikingPack_Gray",
            "deci:ItemHikingPack_Tan", "deci:casual1Vest", "deci:casual1Pants", "deci:casual2Vest", "deci:casual2Pants",
            "deci:casual3Boots", "deci:hoodie3", "deci:hoodie5", "deci:hoodie7", "deci:ItemCrowbar", "deci:ItemBaseballBat",
            "deci:ItemCricketbat");
        add(c3, UNCOMMON, 1, 1, "deci:ItemDuffleBag", "deci:ItemMachete", "deci:ItemFireaxe", "deci:ItemBaseballBatNails");
        add(c3, RARE, 1, 1, "deci:ItemChainsaw");
        Pool c4 = pool(d, "civilian_4", 4, 6, 120);
        c4.first = new ArrayList<String>(Arrays.asList("civilian_1", "civilian_2", "civilian_3"));
        add(c4, COMMON, 1, 1, "@civilian_1", "@civilian_2", "@civilian_3");
        add(c4, RARE, 1, 1, "deci:glock17", "deci:browninghp");
        add(c4, RARE, 1, 1, mags("deci:glock17", "deci:browninghp"));
        return d;
    }

    private static Pool pool(FileData d, String name, int min, int max, int refill)
    {
        Pool p = new Pool();
        p.rolls = new int[] {min, max};
        p.refill = refill;
        d.lists.put(name, p);
        return p;
    }

    private static void add(Pool p, int weight, int min, int max, String... names)
    {
        for (String n : names)
        {
            Entry e = new Entry();
            e.item = n;
            e.weight = weight;
            e.count = new int[] {min, max};
            p.items.add(e);
        }
    }

    /** Registry names of the magazines the guns load (their own lists, so a new gun brings its mags). */
    private static String[] mags(String... guns)
    {
        Set<String> out = new LinkedHashSet<String>();
        for (String g : guns)
        {
            Item gun = (Item) Item.itemRegistry.getObject(g);
            for (Item m : Deci.gunMagazines(gun))
            {
                String n = Item.itemRegistry.getNameForObject(m);
                // our 60 round mags go into the rare row on their own
                if (n != null && !n.endsWith("60Mag"))
                {
                    out.add(n);
                }
            }
        }
        return out.toArray(new String[0]);
    }
}
