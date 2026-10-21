package net.decimation.fixes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Recoil by grip (docs/superpowers/specs/2026-10-21-new-attachments-design.md section 2). Decimation turned the view
 * by the player's recoil in GunItem.clientHeldTick and divided it by 3 only for its own Foregrip (an identity check);
 * tools/patches/PatchGripRecoil.java makes that code ask here instead (the Foregrip keeps its third), and makes
 * GunItemRenderer calm the view's kick for every grip that calmsView. Client thread only.
 */
public final class GripRecoil
{
    /** The gun whose clientHeldTick runs now (set and cleared by the patched method). */
    public static ItemStack current;

    private GripRecoil()
    {
    }

    /** {up, sideways} factors: Decimation's Foregrip 1/3 both (unchanged), the RK-1 1/2 up and 1/4 sideways, else 1. */
    public static float[] factors(String grip)
    {
        if ("foregrip".equals(grip))
        {
            return new float[] {1 / 3f, 1 / 3f};
        }
        if ("rk1".equals(grip))
        {
            return new float[] {0.5f, 0.25f};
        }
        return new float[] {1, 1};
    }

    /** True for the grips that calm the view's kick as Decimation's Foregrip does. */
    public static boolean calmsViewName(String grip)
    {
        return "foregrip".equals(grip) || "rk1".equals(grip);
    }

    /** The upward turn this tick for `recoil` with the gun's grip. */
    public static float pitch(ItemStack gun, float recoil)
    {
        return recoil * factors(gripName(gun))[0];
    }

    /** The sideways turn this tick. */
    public static float yaw(ItemStack gun, float recoil)
    {
        return recoil * factors(gripName(gun))[1];
    }

    /** GunItemRenderer's "is it the Foregrip" test, for any grip item. */
    public static boolean calmsView(Object grip)
    {
        return grip instanceof Item && calmsViewName(Deci.attachmentName((Item) grip));
    }

    private static String gripName(ItemStack gun)
    {
        Item g = Deci.gripAttachment(gun);
        return g == null ? null : Deci.attachmentName(g);
    }
}
