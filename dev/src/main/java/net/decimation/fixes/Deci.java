package net.decimation.fixes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Readable access to Decimation's obfuscated classes. Decimation ships
 * obfuscated (deci.ay.i, adK...) and the game loads those exact names, so
 * our code has to call them; the readable names exist only in the reading
 * copy deobf/src (deobf/names.tsv, deobf/decimation.srg). Every obfuscated
 * name our fixes use lives here, so the rest of the code reads normally.
 */
public final class Deci
{
    private Deci()
    {
    }

    // ---- weapons: GunItem = deci.ay.i, AttachmentItem = deci.ay.h

    public static boolean isGun(Item item)
    {
        return item instanceof deci.ay.i;
    }

    public static boolean isAttachment(Item item)
    {
        return item instanceof deci.ay.h;
    }

    /** GunItem.getSightAttachment(stack) (H): the fitted scope / sight, or null. */
    public static Item sightAttachment(ItemStack gun)
    {
        return ((deci.ay.i) gun.getItem()).H(gun);
    }

    /** GunItem.hasIntegratedScope() (fM). */
    public static boolean hasIntegratedScope(Item gun)
    {
        return ((deci.ay.i) gun).fM();
    }

    /** GunItem.getIntegratedScopeName() (fQ). */
    public static String integratedScopeName(Item gun)
    {
        return ((deci.ay.i) gun).fQ();
    }

    /** AttachmentItem.name (adK): "reddot", "2x", "4x", "8x", "dragunovScope"... */
    public static String attachmentName(Item attachment)
    {
        return ((deci.ay.h) attachment).adK;
    }

    /** AttachmentItem.isScope (adR). */
    public static boolean isScope(Item attachment)
    {
        return ((deci.ay.h) attachment).adR;
    }

    /** AttachmentItem.zoomFov (adS): the old scope camera used FOV 50 - zoomFov. */
    public static float zoomFov(Item attachment)
    {
        return ((deci.ay.h) attachment).adS;
    }

    /** True when a gun with a scope (fitted or integrated) is in this stack. */
    public static boolean isScopedGun(ItemStack stack)
    {
        if (stack == null || !isGun(stack.getItem()))
        {
            return false;
        }
        Item sight = sightAttachment(stack);
        return sight != null ? isScope(sight) : hasIntegratedScope(stack.getItem());
    }

    // ---- player data: PlayerData = deci.Q.b

    /** PlayerData.get(player).getAimMode() (e, ci): 1 = aiming down the sights. */
    public static int aimMode(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? 0 : data.ci();
    }

    /** PlayerData.get(player).setAimMode(mode) (e, M). */
    public static void setAimMode(EntityPlayer player, int mode)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.M(mode);
        }
    }

    // ---- rendering: ClientRenderHandler = deci.c.b

    /**
     * Where the scope glass was last drawn on screen, window pixels (origin
     * bottom left), or null when not within the last 0.25 s. Fields added to
     * BModel (deci.n.f) by tools/patches/PatchScope.java.
     */
    public static float[] scopeGlassOnScreen()
    {
        if (System.nanoTime() - deci.n.f.glassTime > 250_000_000L)
        {
            return null;
        }
        return new float[] {deci.n.f.glassX, deci.n.f.glassY};
    }

    /** The last scope glass box on screen: min x, min y, max x, max y (window pixels, clipped to the screen). */
    public static float[] scopeGlassBox()
    {
        return new float[] {deci.n.f.glassMinX, deci.n.f.glassMinY, deci.n.f.glassMaxX, deci.n.f.glassMaxY};
    }

    /** System.nanoTime() of the last scope glass sample (changes with every new sample). */
    public static long scopeGlassTime()
    {
        return deci.n.f.glassTime;
    }

    /** ClientRenderHandler.scopeTextureId (dC): the texture BModel draws on scope glass. */
    public static int scopeTexture()
    {
        return deci.c.b.dC;
    }
}
