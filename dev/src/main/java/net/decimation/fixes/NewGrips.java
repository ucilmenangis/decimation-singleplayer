package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Our grip attachments (docs/superpowers/specs/2026-10-21-new-attachments-design.md): the RK-1 Foregrip (model, texture
 * and icon by tools/guns/grips.py), registered as deci:rk1 (Decimation looks attachments up in its own namespace), in
 * every loot list that holds Decimation's Foregrip. Its recoil: fixes/GripRecoil; its place on each gun: GripPlacement.
 */
public final class NewGrips
{
    public static Item rk1;

    private NewGrips()
    {
    }

    public static void register()
    {
        rk1 = Deci.newGripAttachment("rk1");
        Item foregrip = GameRegistry.findItem("deci", "foregrip");
        int pools = rk1 != null && foregrip != null ? Deci.addLootLike(foregrip, new ItemStack(rk1, 1)) : 0;
        FMLLog.info("[deciworldgen] grips registered: RK-1 %s, loot %d pools", rk1 != null, pools);
    }
}
