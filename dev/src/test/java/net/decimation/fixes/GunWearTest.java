package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Gun wear (feature 10, user 22 Oktober 2026): 1 a shot, the odd jam below 25 % left, no firing at 0; racking clears a
 * jam. The gun's NBT holds the rounds fired and the jam.
 */
class GunWearTest
{
    @Test
    void eachShotWearsOneUpToTheMaximum()
    {
        NBTTagCompound tag = new NBTTagCompound();
        GunWear.shot(tag, 1500, 0.99);
        GunWear.shot(tag, 1500, 0.99);
        assertEquals(2, GunWear.wear(tag));
        tag.setInteger(GunWear.WEAR, 1500);
        GunWear.shot(tag, 1500, 0.99);
        assertEquals(1500, GunWear.wear(tag));                // never past the maximum
    }

    @Test
    void jamsOnlyBelowAQuarterAndOnlyOnAnUnluckyRoll()
    {
        NBTTagCompound fresh = new NBTTagCompound();
        assertFalse(GunWear.shot(fresh, 1500, 0.0));           // new gun: no jam even at the unluckiest roll
        assertFalse(GunWear.jammed(fresh));
        NBTTagCompound worn = new NBTTagCompound();
        worn.setInteger(GunWear.WEAR, 1200);
        assertFalse(GunWear.shot(worn, 1500, 0.5));            // a lucky roll
        assertTrue(GunWear.shot(worn, 1500, 0.01));            // under the 2 % chance
        assertTrue(GunWear.jammed(worn));
    }

    @Test
    void aJammedOrBrokenGunDoesNotFire()
    {
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(GunWear.fires(tag, 1500));
        tag.setBoolean(GunWear.JAM, true);
        assertFalse(GunWear.fires(tag, 1500));
        GunWear.clear(tag);
        assertTrue(GunWear.fires(tag, 1500));
        tag.setInteger(GunWear.WEAR, 1500);
        assertFalse(GunWear.fires(tag, 1500));
        assertTrue(GunWear.fires(tag, 0));                     // no wear for this kind of gun
    }

    @Test
    void gunPartsRepairAQuarter()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(GunWear.WEAR, 1500);
        GunWear.repair(tag, 1500);
        assertEquals(1125, GunWear.wear(tag));
    }
}
