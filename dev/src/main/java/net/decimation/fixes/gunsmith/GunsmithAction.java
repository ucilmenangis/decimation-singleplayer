package net.decimation.fixes.gunsmith;

/**
 * A click in the Gunsmith screen's list, sent to the server as a container button id (Container.enchantItem, the way
 * vanilla's enchanting table sends its buttons), so no network channel of our own is needed. The id travels as ONE
 * signed byte (C11PacketEnchantItem writes it with writeByte): code = slot * 36 + inventory index for a fit (0 .. 215),
 * 216 + slot for a take off, sent as (byte) code. A singleplayer world hands the packet over without writing it, so only
 * a dedicated or hosted server showed the old ids (1000 and up) arriving cut to their last byte. The server decodes and
 * checks every id. Plain Java.
 */
public final class GunsmithAction
{
    private static final int INVENTORY = 36, TAKE_OFF = GunSlot.values().length * INVENTORY;

    public final boolean takeOff;
    public final GunSlot slot;
    public final int invIndex;

    private GunsmithAction(boolean takeOff, GunSlot slot, int invIndex)
    {
        this.takeOff = takeOff;
        this.slot = slot;
        this.invIndex = invIndex;
    }

    /** Fit one item from player inventory index invIndex (0 .. 35) into slot s. */
    public static int fit(GunSlot s, int invIndex)
    {
        return (byte) (s.ordinal() * INVENTORY + invIndex);
    }

    /** Take the part off slot s, back into the inventory. */
    public static int takeOff(GunSlot s)
    {
        return (byte) (TAKE_OFF + s.ordinal());
    }

    /** The action behind a button id as the server reads it (a signed byte), or null (no such action, or a used up part). */
    public static GunsmithAction decode(int id)
    {
        int code = id & 0xFF;
        GunSlot[] slots = GunSlot.values();
        if (code < TAKE_OFF)
        {
            return new GunsmithAction(false, slots[code / INVENTORY], code % INVENTORY);
        }
        if (code < TAKE_OFF + slots.length && !slots[code - TAKE_OFF].usedUp())
        {
            return new GunsmithAction(true, slots[code - TAKE_OFF], -1);
        }
        return null;
    }
}
