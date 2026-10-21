package net.decimation.fixes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

/**
 * The ELCAN SpecterDR's two magnifications (docs/superpowers/specs/2026-10-21-new-attachments-design.md; user: "1x/4x
 * switch"): 4x until the player flips it (SpecterKey, default G), then 1x, per gun, for the session. A gun is known by
 * the "uniqueID" Decimation's OwnedItem tags it with in a player's inventory, else by its inventory slot. Plain Java;
 * ScopeZoom asks it on the client.
 */
public final class SpecterMode
{
    /**
     * Guns switched to 1x, by id or (without one) by slot; every other gun's SpecterDR is at 4x. Asked every frame a gun
     * is held (ScopeZoom): no key is built per call (a string key cost 80 bytes a call, dev test attachments perf=true),
     * and a slot's Integer comes from Integer's own cache.
     */
    private static final Set<String> ONE_IDS = new HashSet<String>();
    private static final Set<Integer> ONE_SLOTS = new HashSet<Integer>();

    private SpecterMode()
    {
    }

    /** True while this gun's SpecterDR is at 4x (the default). */
    public static boolean isFour(ItemStack gun, int slot)
    {
        return isFourById(id(gun), slot);
    }

    /** Flips this gun's SpecterDR between 4x and 1x; the new state (true = 4x). */
    public static boolean toggle(ItemStack gun, int slot)
    {
        return toggleById(id(gun), slot);
    }

    static boolean isFourById(String id, int slot)
    {
        return id != null && !id.isEmpty() ? !ONE_IDS.contains(id) : !ONE_SLOTS.contains(slot);
    }

    static boolean toggleById(String id, int slot)
    {
        boolean byId = id != null && !id.isEmpty();
        if (byId ? ONE_IDS.remove(id) : ONE_SLOTS.remove(slot))
        {
            return true;
        }
        if (byId)
        {
            ONE_IDS.add(id);
        }
        else
        {
            ONE_SLOTS.add(slot);
        }
        return false;
    }

    static void reset()
    {
        ONE_IDS.clear();
        ONE_SLOTS.clear();
    }

    private static String id(ItemStack gun)
    {
        return gun != null && gun.stackTagCompound != null ? gun.stackTagCompound.getString("uniqueID") : null;
    }
}
