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

    /** The damage to give a piece before vanilla adds its melee wear, so that it stops on the maximum (never destroyed). */
    public static int beforeVanillaWear(int damage, int max, int vanillaWear)
    {
        return damage + vanillaWear > max ? Math.max(0, max - vanillaWear) : damage;
    }
}
