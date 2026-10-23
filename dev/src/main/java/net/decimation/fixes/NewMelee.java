package net.decimation.fixes;

import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;

/**
 * Our melee weapons (user 23 Oktober 2026): the Karambit and the Katana, Decimation melee weapons in its own sense
 * (MeleeWeaponItem: damage straight off the target's health, wear, its tooltip and tab), registered as
 * deciworldgen:karambit / deciworldgen:katana. Damage among Decimation's melee (Ice Pick 9, Machete / Combat Knife 7):
 * the karambit 7, the katana 17 (user 23 Oktober 2026, with a hit twice as wide: MeleeMotion.wideHit); 255 uses like most
 * of them. Loot: every list that holds Decimation's Machete. Fire Aspect goes onto the katana on an anvil (a book; it is
 * not a sword, so Minecraft alone refuses it) and sets it alight (MeleeFire). Models, textures, icons and the fire's heat
 * mask from tools/guns/melee.py; drawn by MeleeRenderer, moved by MeleeMotion.
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
        katana = item("katana", 17);
        Item machete = GameRegistry.findItem("deci", "ItemMachete");
        int pools = machete == null ? 0 : Deci.addLootLike(machete, new ItemStack(karambit, 1))
                                          + Deci.addLootLike(machete, new ItemStack(katana, 1));
        MinecraftForge.EVENT_BUS.register(new NewMelee());   // the anvil, on both sides
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

    /** The katana with a Fire Aspect book on an anvil: the katana with the book's Fire Aspect level, 5 levels. */
    @SubscribeEvent
    public void onAnvil(AnvilUpdateEvent e)
    {
        if (e.left == null || e.right == null || e.left.getItem() != katana || e.right.getItem() != Items.enchanted_book)
        {
            return;
        }
        NBTTagList stored = Items.enchanted_book.func_92110_g(e.right);
        for (int i = 0; i < stored.tagCount(); i++)
        {
            NBTTagCompound tag = stored.getCompoundTagAt(i);
            int level = tag.getShort("lvl");
            if (tag.getShort("id") == Enchantment.fireAspect.effectId
                && level > EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, e.left))
            {
                ItemStack out = e.left.copy();
                Map<Integer, Integer> enchants = EnchantmentHelper.getEnchantments(out);
                enchants.put(Enchantment.fireAspect.effectId, level);
                EnchantmentHelper.setEnchantments(enchants, out);
                e.output = out;
                e.cost = 5;
                return;
            }
        }
    }

    /** The Fire Aspect level of a stack (0 none). */
    public static int fireAspect(ItemStack stack)
    {
        return stack == null ? 0 : EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
    }

    /** 0 for the karambit, 1 for the katana, -1 for anything else. */
    public static int kind(Item item)
    {
        return item == null ? -1 : item == karambit ? 0 : item == katana ? 1 : -1;
    }
}
