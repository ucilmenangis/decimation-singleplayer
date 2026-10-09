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
    public static Item mac10, mac10Mag, ump9, ump9Mag;

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
            // the SMG suppressor would start 1.12 units ahead of the short MAC-10's muzzle
            // (study.py attach); like the real one it screws over the threads up to the receiver front
            suppressor = Deci.offsetAttachment(mac10, "smgSuppressor", -2.37f, -0.16f, 0);
        }
        FMLLog.info("[deciworldgen] MAC-10 registered: loot %d gun / %d mag pools, %d sounds from the Uzi, suppressor %s",
                    gunPools, magPools, sounds, suppressor ? "moved" : "not moved");
        registerUmp9();
    }

    /**
     * UMP9 (user request 10 Oktober 2026): Decimation's UMP45 with our curved 9 mm magazine. Its
     * model, texture, icon and animations are generated locally from the user's Decimation.jar by
     * tools/guns/ump9.py (git ignored: nothing of Decimation in the repo), so the gun is registered
     * only when that model is there. 9 mm like the MP5A3 (its rounds, its curved magazine icon),
     * 30 rounds; damage 11 (Uzi 11, MP5A3 12, UMP45 13), 650 rpm, a bit less kick than the UMP45,
     * its weight; the UMP45's sounds; found where the UMP45 and its magazine are found.
     */
    private static void registerUmp9()
    {
        if (NewGuns.class.getResource("/assets/deci/models/guns/smg/ump9.bmodel") == null)
        {
            FMLLog.info("[deciworldgen] UMP9 not registered: run tools/guns/ump9.py (needs Decimation.jar)");
            return;
        }
        ump9Mag = Deci.newMagazine("ump9Mag", 30, "mp5a3Mag", "mp5a3Mag");
        ump9 = Deci.newGun("ump9", "smg", 5.5f, 0.4f, true, 5.0f, 650f, 0.14, 11, ump9Mag);
        Item ump45 = GameRegistry.findItem("deci", "ump45"), ump45Mag = GameRegistry.findItem("deci", "ump45Mag");
        int gunPools = Deci.addLootLike(ump45, new ItemStack(ump9, 1));
        int magPools = Deci.addLootLike(ump45Mag, new ItemStack(ump9Mag, 2));
        int sounds = FMLCommonHandler.instance().getSide().isClient() ? Deci.useGunSounds("ump9", "smg", "ump45", "smg") : 0;
        FMLLog.info("[deciworldgen] UMP9 registered: loot %d gun / %d mag pools, %d sounds from the UMP45",
                    gunPools, magPools, sounds);
    }
}
