package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Sights of our own (user request 10 Oktober 2026: "make new sight model, holographic sight ... we
 * use 558 model ... and acog 3.5x sight", "please be so detailed"): EOTech 558 holographic (1x,
 * red ring and dot) and Trijicon ACOG TA11 3.5x35 (fibre optic, BDC reticle). Models, textures,
 * icons and reticles are ours (tools/guns/sights.py), registered as deci:eotech558 and
 * deci:ta11acog so Decimation's attachment lookups find them (Deci.newSightAttachment). Zoom and
 * reticle while aiming: ScopeZoom (magnification 1.25 / 3.5, reticle drawn in the glass).
 * Found where Decimation's red dot and 4x are found.
 */
public final class NewSights
{
    public static Item eotech558, ta11acog;

    private NewSights()
    {
    }

    public static void register()
    {
        eotech558 = Deci.newSightAttachment("eotech558", 42.5f, 0.4f);   // the red dot's zoom and sway
        ta11acog = Deci.newSightAttachment("ta11acog", 45f, 0.3f);       // the 4x's
        int pools = 0;
        Item reddot = GameRegistry.findItem("deci", "reddot"), x4 = GameRegistry.findItem("deci", "4x");
        if (eotech558 != null && reddot != null)
        {
            pools += Deci.addLootLike(reddot, new ItemStack(eotech558, 1));
        }
        if (ta11acog != null && x4 != null)
        {
            pools += Deci.addLootLike(x4, new ItemStack(ta11acog, 1));
        }
        FMLLog.info("[deciworldgen] sights registered: EOTech 558 %s, ACOG TA11 %s, loot %d pools",
                    eotech558 != null, ta11acog != null, pools);
    }
}
