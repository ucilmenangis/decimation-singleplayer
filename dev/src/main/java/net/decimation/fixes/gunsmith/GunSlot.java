package net.decimation.fixes.gunsmith;

/**
 * The six slots of the Gunsmith screen (docs/superpowers/specs/2026-10-21-gunsmith-screen-design.md), in drawing order.
 * deciIndex is the slot's index in Decimation's WeaponModifyContainer (deci/f/a), which its FilteredSlot rules key on;
 * nbtKey is where the gun's NBT keeps the fitted part. Paint and tracer are used up when applied, as in Decimation's own
 * screen. Plain Java: unit tests load it without Minecraft.
 */
public enum GunSlot
{
    SIGHT("Sight", "Sights", 2, "sightAttach"),
    BARREL("Barrel", "Barrels", 1, "barrelAttach"),
    GRIP("Grip", "Grips", 4, "gripAttach"),
    STOCK("Stock", "Stocks", 3, "stockAttach"),
    PAINT("Paint", "Paints", 5, "skin"),
    TRACER("Tracer", "Tracers", 6, "tracerColorID");

    public final String label, plural, nbtKey;
    public final int deciIndex;

    GunSlot(String label, String plural, int deciIndex, String nbtKey)
    {
        this.label = label;
        this.plural = plural;
        this.deciIndex = deciIndex;
        this.nbtKey = nbtKey;
    }

    /** Paint and tracer are spent when applied: they can be replaced, never taken off. */
    public boolean usedUp()
    {
        return this == PAINT || this == TRACER;
    }

    /** The list panel's title: "Grips you carry". */
    public String listTitle()
    {
        return plural + " you carry";
    }
}
