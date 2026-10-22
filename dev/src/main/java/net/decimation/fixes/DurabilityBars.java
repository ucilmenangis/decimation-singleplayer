package net.decimation.fixes;

import net.minecraft.item.ItemStack;

/**
 * Native durability bars on icons (user, 23 Oktober 2026: "the icon gun item has progress bar in there showing durability
 * like native progress bar"; "for gas mask, you better add durability progress on it, so user know remaning the
 * durability"). Forge draws a tool's bar from Item.showDurabilityBar and getDurabilityForDisplay
 * (RenderItem.renderItemOverlayIntoGUI: every slot, the hotbar); tools/patches/PatchDurabilityBars.java gives GunItem and
 * ItemGasMaskDeci both methods, answered here: a gun by its wear (GunWear), a gas mask by its filter (GasMaskFilter).
 * Armor and vests wear their item damage, so vanilla draws theirs already. Mirroring wear into item damage would draw the
 * same bar, but Decimation's traders refuse a damaged item. Asked for each such stack drawn, every frame: two tag reads, no
 * allocation.
 */
public final class DurabilityBars
{
    private DurabilityBars()
    {
    }

    /** A bar on this gun or gas mask: once a gun has worn, or a filter has been used. */
    public static boolean shown(ItemStack s)
    {
        if (s == null)
        {
            return false;
        }
        int gunMax = GunWear.max(s);
        if (gunMax > 0)
        {
            return Durability.barShown(GunWear.wear(s.stackTagCompound), gunMax);
        }
        return Deci.isGasMask(s) && Durability.barShown(GasMaskFilter.LIFE - GasMaskFilter.life(s), GasMaskFilter.LIFE);
    }

    /** The share used up, 0 .. 1 (1 draws an empty bar: a worn out gun, an empty filter). */
    public static double fraction(ItemStack s)
    {
        if (s == null)
        {
            return 0;
        }
        int gunMax = GunWear.max(s);
        if (gunMax > 0)
        {
            return Durability.barFraction(GunWear.wear(s.stackTagCompound), gunMax);
        }
        return Deci.isGasMask(s) ? Durability.barFraction(GasMaskFilter.LIFE - GasMaskFilter.life(s), GasMaskFilter.LIFE) : 0;
    }
}
