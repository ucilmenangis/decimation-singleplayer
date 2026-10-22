package net.decimation.fixes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Our melee weapons (user 23 Oktober 2026): the Karambit and the Katana, Decimation melee weapons in its own sense
 * (MeleeWeaponItem: damage straight off the target's health, wear, its tooltip and tab), registered as
 * deciworldgen:karambit / deciworldgen:katana. Damage among Decimation's melee (Ice Pick 9, Machete / Combat Knife 7):
 * the karambit 7, the katana 9; 255 uses like most of them. Loot: every list that holds Decimation's Machete. Models,
 * textures and icons from tools/guns/melee.py; drawn by MeleeRenderer, moved by MeleeMotion.
 */
public final class NewMelee
{
    public static Item karambit, katana;

    private NewMelee()
    {
    }

    public static void register()
    {
        karambit = item("karambit", 7);
        katana = item("katana", 9);
        Item machete = GameRegistry.findItem("deci", "ItemMachete");
        int pools = machete == null ? 0 : Deci.addLootLike(machete, new ItemStack(karambit, 1))
                                          + Deci.addLootLike(machete, new ItemStack(katana, 1));
        if (FMLCommonHandler.instance().getSide().isClient())
        {
            MeleeRenderer.install();                    // client classes stay out of this class (dedicated server)
        }
        FMLLog.info("[deciworldgen] melee registered: karambit, katana, loot %d pools", pools);
    }

    private static Item item(String name, int damage)
    {
        Item i = Deci.newMeleeWeapon(255, damage).setUnlocalizedName(name).setTextureName("deciworldgen:" + name);
        GameRegistry.registerItem(i, name);
        return i;
    }

    /** 0 for the karambit, 1 for the katana, -1 for anything else. */
    public static int kind(Item item)
    {
        return item == null ? -1 : item == karambit ? 0 : item == katana ? 1 : -1;
    }
}
