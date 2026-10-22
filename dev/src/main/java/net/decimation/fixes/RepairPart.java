package net.decimation.fixes;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.decimation.mod.common.item.armor.ItemMaskDeci;
import net.decimation.mod.common.item.armor.ItemVestDeci;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/**
 * The repair parts of feature 10 (user, 22 Oktober 2026: "armor now can be repaired with armor part and for vest
 * durability is 512 and need armor plate item that can be find it on military or police crate ... guns ... repaired by
 * guns part"; chosen: a quarter a part, military and police crates). A right click repairs, on the server, a quarter of
 * the maximum of: the most worn armor piece worn (Armor Repair Kit), the vest (Armor Plate), the most worn gun in the
 * hotbar (Gun Parts); one part is used and a line says what was repaired. Nothing to repair uses nothing. Icons:
 * tools/item_icons.py.
 */
public final class RepairPart extends Item
{
    public enum Kind
    {
        ARMOR("armor_part", "an Armor Repair Kit"), PLATE("armor_plate", "an Armor Plate"), GUN("gun_parts", "Gun Parts");

        public final String id, said;

        Kind(String id, String said)
        {
            this.id = id;
            this.said = said;
        }
    }

    public static RepairPart armorPart, armorPlate, gunParts;
    public final Kind kind;

    private RepairPart(Kind kind)
    {
        this.kind = kind;
        setMaxStackSize(16);
        setUnlocalizedName("deciworldgen." + kind.id);
        setTextureName("deciworldgen:" + kind.id);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    /** preInit, after Decimation's items and loot: the three items and their loot (military and police only). */
    public static void register()
    {
        armorPart = add(Kind.ARMOR);
        armorPlate = add(Kind.PLATE);
        gunParts = add(Kind.GUN);
        Item like = (Item) Item.itemRegistry.getObject("deci:pistolSuppressor");   // in military and police loot, not civilian
        int pools = like == null ? 0
            : Deci.addLootShare(like, new ItemStack(armorPart), 0.03) + Deci.addLootShare(like, new ItemStack(armorPlate), 0.02)
              + Deci.addLootShare(like, new ItemStack(gunParts), 0.03);
        cpw.mods.fml.common.FMLLog.info("[deciworldgen] repair parts registered, %d loot pools", pools);
    }

    private static RepairPart add(Kind kind)
    {
        RepairPart part = new RepairPart(kind);
        GameRegistry.registerItem(part, kind.id);
        return part;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (world.isRemote)
        {
            return stack;
        }
        String what = repair(player, kind);
        if (what == null)
        {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Nothing to repair with " + kind.said));
            return stack;
        }
        if (!player.capabilities.isCreativeMode)
        {
            stack.stackSize--;
        }
        world.playSoundAtEntity(player, "random.anvil_use", 0.4f, 1.4f);
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Repaired: " + what));
        player.inventoryContainer.detectAndSendChanges();
        return stack;
    }

    /** Repairs a quarter of the most worn thing of this kind; what it repaired ("name N / M"), or null for nothing. */
    public static String repair(EntityPlayer player, Kind kind)
    {
        if (kind == Kind.PLATE)
        {
            ItemStack vest = Deci.playerVest(player);
            if (vest != null && vest.getItem() instanceof ItemVestDeci && vest.getItemDamage() > 0 && vest.getMaxDamage() > 0)
            {
                vest.setItemDamage(Durability.repaired(vest.getItemDamage(), vest.getMaxDamage()));
                return said(vest, vest.getMaxDamage() - vest.getItemDamage(), vest.getMaxDamage());
            }
            return null;
        }
        if (kind == Kind.ARMOR)
        {
            ItemStack best = null;
            for (int slot = 0; slot < 4; slot++)
            {
                ItemStack s = player.getCurrentArmor(slot);
                if (s != null && s.getItem() instanceof ItemArmorDeci && !(s.getItem() instanceof ItemMaskDeci) && s.getMaxDamage() > 0
                    && s.getItemDamage() > 0 && (best == null || share(s.getItemDamage(), s.getMaxDamage()) > share(best.getItemDamage(), best.getMaxDamage())))
                {
                    best = s;
                }
            }
            if (best == null)
            {
                return null;
            }
            best.setItemDamage(Durability.repaired(best.getItemDamage(), best.getMaxDamage()));
            return said(best, best.getMaxDamage() - best.getItemDamage(), best.getMaxDamage());
        }
        ItemStack best = null;
        for (int slot = 0; slot < 9; slot++)
        {
            ItemStack s = player.inventory.getStackInSlot(slot);
            int max = GunWear.max(s);
            if (max > 0 && s.stackTagCompound != null && GunWear.wear(s.stackTagCompound) > 0
                && (best == null || share(GunWear.wear(s.stackTagCompound), max) > share(GunWear.wear(best.stackTagCompound), GunWear.max(best))))
            {
                best = s;
            }
        }
        if (best == null)
        {
            return null;
        }
        int max = GunWear.max(best);
        GunWear.repair(best.stackTagCompound, max);
        return said(best, max - GunWear.wear(best.stackTagCompound), max);
    }

    private static double share(int damage, int max)
    {
        return damage / (double) max;
    }

    private static String said(ItemStack s, int left, int max)
    {
        return s.getDisplayName() + " " + left + " / " + max;
    }
}
