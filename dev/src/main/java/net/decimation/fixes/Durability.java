package net.decimation.fixes;

/**
 * The numbers of feature 10 (user, 22 Oktober 2026: armor 256, vests 512, "1 shot = 1 durability", repaired with parts;
 * guns wear and are repaired with gun parts; choices in docs/ideas.md 47 and 48, design in
 * docs/superpowers/specs/2026-10-22-durability-design.md). Pure: ArmorWear, GunWear and the part items use it. Damage
 * here is what an item has used up (vanilla's item damage for armor and vests, our wear count for guns): 0 is new, the
 * maximum is broken (kept, working no more).
 */
public final class Durability
{
    public static final int ARMOR = 256;
    public static final int VEST = 512;
    /** Below this share left a gun may jam, JAM_CHANCE a shot. */
    static final float JAM_ZONE = 0.25f;
    public static final float JAM_CHANCE = 0.02f;

    private Durability()
    {
    }

    /** Rounds a gun of this WeaponCategory (Decimation's enum name) lasts; 0 for none (launchers, crossbows). */
    public static int gunMax(String category)
    {
        if (category == null)
        {
            return 0;
        }
        switch (category)
        {
            case "rifle": return 1500;
            case "smg": return 1800;
            case "pistol":
            case "revolver": return 2500;
            case "shotgun": return 1200;
            case "mg": return 1000;
            default: return 0;
        }
    }

    /** The damage after one part: a quarter of the maximum less, not below new. */
    public static int repaired(int damage, int max)
    {
        return Math.max(0, damage - max / 4);
    }

    /** Used up: kept, but protecting or firing no more. */
    public static boolean broken(int damage, int max)
    {
        return max > 0 && damage >= max;
    }

    /** Less than a quarter left: a gun may jam. */
    public static boolean jamZone(int damage, int max)
    {
        return max > 0 && max - damage < max * JAM_ZONE;
    }

    /** Against a body shot the stronger (lower) of the chest piece's and the vest's factors counts (1 for none). */
    public static float bodyFactor(float chest, float vest)
    {
        return Math.min(chest, vest);
    }

    /**
     * Forge's own wear of a vanilla armor piece in a hit (ArmorProperties.ApplyArmor): it absorbs health x 25 x points / 25
     * (scaled by 25), at most the durability left + 1, and wears (int) (absorbed / 25), at least 1.
     */
    public static int forgeWear(float health, int points, int max, int damage)
    {
        double absorb = Math.min(health * 25.0 * (points / 25.0), max + 1 - damage);
        return (int) (absorb / 25.0 < 1 ? 1 : absorb / 25.0);
    }

    /**
     * The damage to give a piece before Forge wears it in this hit, so that it ends on the maximum and is never destroyed
     * (Forge destroys a piece past it): a hit that would pass the maximum gets the piece set to the maximum less 1, where
     * Forge's wear is exactly 1 (it absorbs at most 2 / 25 health there). Otherwise untouched: never a free repair (an
     * automated review found the first version, a guess of Forge's wear plus 1, setting used up pieces back by several).
     */
    public static int beforeVanillaWear(int damage, int max, float health, int points)
    {
        return damage + forgeWear(health, points, max, damage) > max ? Math.max(0, max - 1) : damage;
    }

    /**
     * A vest's damage after a melee hit (user, 23 Oktober 2026: "vest armor still doesnt affected when hit by zombie"):
     * Forge wears only the four armor slots, so the vest gets the wear Forge gives a piece of its points, stopping on the
     * maximum (kept, never destroyed).
     */
    public static int meleeWear(int damage, int max, float health, int points)
    {
        return Math.min(max, damage + forgeWear(health, points, max, damage));
    }

    /** A durability bar on the item's icon, as vanilla draws one for a damaged tool: only once something is used up. */
    public static boolean barShown(int damage, int max)
    {
        return max > 0 && damage > 0;
    }

    /** The share used up, 0 .. 1, as Forge's Item.getDurabilityForDisplay answers it (1 draws an empty bar). */
    public static double barFraction(int damage, int max)
    {
        return max <= 0 ? 0 : Math.max(0, Math.min(1, damage / (double) max));
    }
}
