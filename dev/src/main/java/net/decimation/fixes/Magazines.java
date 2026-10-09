package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 60 round magazines (user request 2026-10-09): a 60 round NATO STANAG
 * (5.56: every gun sharing Decimation's STANAG mag list (m16a2Mag: M16A1 /
 * A2, M231, AR-15, XM177, Honey Badger) plus the STANAG rifles Decimation
 * gives a mag of their own: M4A4, ACR, L85A1, SCAR-L; not FAMAS, G36C, AUG,
 * Galil, INSAS, whose real mags differ) and a 60 round AK (5.45: every gun
 * that takes the AK-74 mag ak74Mag, as Decimation shares it, AKM included). Same bullets as those mags, icons borrowed from Decimation's own
 * mags (STANAG: m16a2Mag, AK: the longer rpk74Mag), names in
 * assets/deciworldgen/lang/en_US.lang. One goes into every loot pool that
 * already holds the 30 round one. Runs in our preInit, right after
 * Decimation's (its guns and loot tables exist by then).
 */
public final class Magazines
{
    public static Item stanag60, ak60;

    private Magazines()
    {
    }

    public static void register()
    {
        stanag60 = Deci.newMagazine("stanag60Mag", 60, "m16a2Mag", "m16a2Mag");
        ak60 = Deci.newMagazine("ak60Mag", 60, "ak74Mag", "rpk74Mag");
        Item stanag30 = GameRegistry.findItem("deci", "m16a2Mag"), ak30 = GameRegistry.findItem("deci", "ak74Mag");
        java.util.Set<Item> stanagOwn = new java.util.HashSet<Item>();
        for (String g : new String[] {"m4a4", "acr", "l85a1", "fnscarl"})
        {
            stanagOwn.add(GameRegistry.findItem("deci", g));
        }
        int stanagGuns = 0, akGuns = 0;
        for (Object o : Item.itemRegistry)
        {
            Item gun = (Item) o;
            if (Deci.gunTakes(gun, stanag30) || stanagOwn.contains(gun))
            {
                Deci.addGunMagazine(gun, stanag60);
                stanagGuns++;
            }
            if (Deci.gunTakes(gun, ak30))
            {
                Deci.addGunMagazine(gun, ak60);
                akGuns++;
            }
        }
        int stanagPools = Deci.addLootLike(stanag30, new ItemStack(stanag60, 1));
        int akPools = Deci.addLootLike(ak30, new ItemStack(ak60, 1));
        FMLLog.info("[deciworldgen] 60 round magazines: STANAG in %d guns / %d loot pools, AK in %d guns / %d loot pools",
                    stanagGuns, stanagPools, akGuns, akPools);
    }
}
