package net.decimation.fixes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Guns of our own (docs/roadmap.md "Pilot: a new gun made by Claude with
 * tools"). Model, texture, icon and animations are our own work, built by
 * tools/guns/<gun>.py (tools/guns/gunmodel.py) into
 * dev/src/main/resources/assets/deci/...; Decimation finds them by the gun's
 * name and category. Sounds point at an existing Decimation gun's files
 * (Deci.useGunSounds): nothing of Decimation is copied.
 *
 * MAC-10 (Ingram M10, .45 ACP): 30 round magazine (the .45 ACP rounds of
 * the UMP45 / Uzi, icon of the Uzi mag), 1100 rounds a minute, damage 12
 * (Uzi 11, UMP45 13), light; the Uzi's sounds; found where the Uzi and its
 * magazine are found.
 */
public final class NewGuns
{
    public static Item mac10, mac10Mag;

    private NewGuns()
    {
    }

    public static void register()
    {
        mac10Mag = Deci.newMagazine("mac10Mag", 30, "ump45Mag", "uziMag");
        mac10 = Deci.newGun("mac10", "smg", 5.5f, 0.45f, true, 7.0f, 1100f, 0.08, 12, mac10Mag);
        Item uzi = GameRegistry.findItem("deci", "uzi"), uziMag = GameRegistry.findItem("deci", "uziMag");
        int gunPools = Deci.addLootLike(uzi, new ItemStack(mac10, 1));
        int magPools = Deci.addLootLike(uziMag, new ItemStack(mac10Mag, 2));
        int sounds = 0;
        boolean suppressor = false;
        if (FMLCommonHandler.instance().getSide().isClient())
        {
            sounds = Deci.useGunSounds("mac10", "smg", "uzi", "smg");
            // the SMG suppressor sits 1.12 units ahead of the short MAC-10's muzzle (study.py attach);
            // pull it back to about the Uzi's 0.1 gap and 0.14 down to the Uzi's height under the bore
            suppressor = Deci.offsetAttachment(mac10, "smgSuppressor", -1.02f, 0.14f, 0);
        }
        FMLLog.info("[deciworldgen] MAC-10 registered: loot %d gun / %d mag pools, %d sounds from the Uzi, suppressor %s",
                    gunPools, magPools, sounds, suppressor ? "moved" : "not moved");
    }
}
