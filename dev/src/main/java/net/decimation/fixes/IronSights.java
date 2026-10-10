package net.decimation.fixes;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Iron sights fold away while a sight is attached (user request 10 Oktober 2026, Mk18: "make the
 * front and rear sight disappear when attaching attachment in guns").
 *
 * Decimation's gun models already name their iron sights "defaultScopeModel*" (19 guns: M4A4, ACR,
 * G36C, AUG, JNG90, AR15, ...; our Mk18's front sight too), but nothing ever read that name: the
 * irons stayed up behind a red dot. tools/patches/PatchIronSights.java hooks two places:
 * GunItemRenderer's gun draw method (deci.K.b.a(ItemStack, Object[])) calls begin / end around it,
 * and BModel.renderParts (deci.n.f.bm) asks skip for every part. Only the gun's own model is
 * affected: attachments are drawn inside the same method, and the Dragunov scope attachment has
 * defaultScope parts of its own.
 * Client render thread only (one gun drawn at a time), so plain static fields.
 */
public final class IronSights
{
    private static Object hideIn;

    private IronSights()
    {
    }

    /** Before a gun is drawn: its model, and whether the stack carries a sight attachment. */
    public static void begin(Object gunModel, ItemStack stack)
    {
        hideIn = hasSight(stack) ? gunModel : null;
        if (hideIn != null)
        {
            SightPlacement.aimShift(stack); // aiming: the gun up by its lowered sight's dy
        }
    }

    public static void end()
    {
        hideIn = null;
    }

    /** True for a defaultScope part of the gun being drawn while it has a sight attached. */
    public static boolean skip(Object model, Object part)
    {
        if (model == null || model != hideIn || !(part instanceof ModelRenderer))
        {
            return false;
        }
        String name = ((ModelRenderer) part).boxName;
        return name != null && name.startsWith("defaultScope");
    }

    /** Decimation keeps the attached sight's registry name in the gun's NBT ("sightAttach"). */
    public static boolean hasSight(ItemStack stack)
    {
        if (stack == null || !stack.hasTagCompound())
        {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey("sightAttach") && !tag.getString("sightAttach").isEmpty();
    }
}
