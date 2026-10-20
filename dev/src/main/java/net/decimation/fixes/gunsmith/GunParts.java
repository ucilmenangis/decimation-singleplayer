package net.decimation.fixes.gunsmith;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The parts a gun carries, kept in its NBT under the keys Decimation's own attachment screen uses (AttachmentInventory
 * deci/g/s): attachments by registry name, the paint's id and English name, the tracer's colour id. Decimation's
 * renderer and shots read them from there, so a part written here shows on the gun and works at once.
 */
public final class GunParts
{
    private GunParts()
    {
    }

    /**
     * Whether part may go into slot s of this gun: the right kind of item for the slot first (Decimation's FilteredSlot
     * maps indexes 5 and 6 to the grip rule, so it would take a grip as paint), then Decimation's own rules.
     */
    public static boolean fits(GunSlot s, ItemStack gun, ItemStack part)
    {
        if (gun == null || part == null || builtIn(s, gun))
        {
            return false;
        }
        Item i = part.getItem();
        boolean kind = s == GunSlot.PAINT ? Deci.isSkin(i) : s == GunSlot.TRACER ? Deci.isTracer(i) : Deci.isAttachment(i);
        return kind && Deci.fitsSlot(gun, s.deciIndex, part);
    }

    /** A slot the gun cannot use: an integrated scope, an integral suppressor, a forced tracer. */
    public static boolean builtIn(GunSlot s, ItemStack gun)
    {
        if (gun == null || !Deci.isGun(gun.getItem()))
        {
            return false;
        }
        Item g = gun.getItem();
        return s == GunSlot.SIGHT && Deci.hasIntegratedScope(g) || s == GunSlot.BARREL && Deci.isIntegrallySuppressed(g)
               || s == GunSlot.TRACER && Deci.hasForcedTracer(g);
    }

    /** The six parts on this gun, by GunSlot ordinal: a fresh one item stack each, or null. */
    public static ItemStack[] read(ItemStack gun)
    {
        ItemStack[] parts = new ItemStack[GunSlot.values().length];
        NBTTagCompound nbt = gun == null ? null : gun.getTagCompound();
        if (nbt == null)
        {
            return parts;
        }
        for (GunSlot s : GunSlot.values())
        {
            Item i;
            if (s == GunSlot.PAINT)
            {
                i = Deci.skinItem(gun);
            }
            else if (s == GunSlot.TRACER)
            {
                i = nbt.hasKey(s.nbtKey) ? Deci.tracerItem(nbt.getInteger(s.nbtKey)) : null;
            }
            else
            {
                String name = nbt.getString(s.nbtKey);
                i = name.isEmpty() ? null : GameRegistry.findItem("deci", name);
            }
            parts[s.ordinal()] = i == null ? null : new ItemStack(i);
        }
        return parts;
    }

    /** Writes slot s's part into the gun's NBT; null clears an attachment (paint and tracer are only ever replaced). */
    public static void write(ItemStack gun, GunSlot s, ItemStack part)
    {
        if (!gun.hasTagCompound())
        {
            gun.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = gun.getTagCompound();
        if (s == GunSlot.PAINT)
        {
            if (part != null)
            {
                nbt.setString("skin", Deci.skinId(part.getItem()));
                nbt.setString("skinEng", Deci.skinName(part.getItem()));
            }
        }
        else if (s == GunSlot.TRACER)
        {
            if (part != null)
            {
                nbt.setInteger(s.nbtKey, Deci.tracerColorId(part.getItem()));
            }
        }
        else
        {
            nbt.setString(s.nbtKey, part == null ? "" : GameRegistry.findUniqueIdentifierFor(part.getItem()).name);
        }
    }
}
