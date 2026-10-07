package net.decimation.worldgen;

/**
 * Clockwise (viewed from above) metadata rotation for 1.7.10 blocks.
 *
 * 1.7.10 has no BlockState - every directional block family encodes its
 * facing differently in the 4 metadata bits, so rotation needs a per-family
 * table. Covered: 6-direction facing (chest/furnace/ladder/piston/dispenser/
 * hopper...), stairs, log axes, torch/button/lever, doors, trapdoors, beds,
 * fence gates, pumpkins, repeaters/comparators, signs, rails, vines, anvils.
 * Anything unknown - including all of Decimation's own obfuscated blocks,
 * whose meta encoding we haven't reverse-engineered - passes through
 * unchanged, which is harmless for full-cube props and merely cosmetic for
 * anything else.
 */
public final class Rotation
{
    private Rotation() {}

    // index = old low bits, value = rotated
    private static final int[] STAIR = {2, 3, 1, 0};          // 0=E 1=W 2=S 3=N
    private static final int[] TORCH = {0, 3, 4, 2, 1, 5, 6, 7}; // 1=E 2=W 3=S 4=N 5=up
    private static final int[] TRAPDOOR = {3, 2, 0, 1};       // 0=S 1=N 2=E 3=W
    private static final int[] RAIL = {1, 0, 5, 4, 2, 3, 7, 8, 9, 6,
                                       10, 11, 12, 13, 14, 15};
    private static final int[] RAIL_POWERED = {1, 0, 5, 4, 2, 3};

    /** Rotate metadata by {@code turns} x 90 degrees clockwise. */
    public static int rotateMeta(int id, int meta, int turns)
    {
        for (int i = 0; i < (turns & 3); i++)
        {
            meta = cw(id, meta);
        }
        return meta;
    }

    private static int cw(int id, int meta)
    {
        switch (id)
        {
            // 6-direction facing (0=down 1=up 2=N 3=S 4=W 5=E), bit 3 kept
            // (piston extended / sticky, furnace unused, ...)
            case 23:  // dispenser
            case 29:  // sticky piston
            case 33:  // piston
            case 34:  // piston extension
            case 154: // hopper
            case 158: // dropper
            case 54:  // chest
            case 61: case 62: // furnace, lit furnace
            case 65:  // ladder
            case 68:  // wall sign
            case 130: // ender chest
            case 146: // trapped chest
                return face6(meta);

            // stairs: low 2 bits facing, bit 2 = upside down
            case 53: case 67: case 108: case 109: case 114: case 128:
            case 134: case 135: case 136: case 156: case 163: case 164:
                return (meta & ~3) | STAIR[meta & 3];

            // logs: bits 2-3 axis (0=Y 4=X 8=Z 12=bark), low bits species
            case 17: case 162:
            {
                int axis = meta & 12;
                if (axis == 4)
                {
                    return (meta & ~12) | 8;
                }
                if (axis == 8)
                {
                    return (meta & ~12) | 4;
                }
                return meta;
            }

            // torches: whole meta is the direction
            case 50: case 75: case 76:
                return meta < 8 ? TORCH[meta] : meta;

            // buttons / lever: low 3 bits direction (1-4 like torch), bit 3 powered
            case 77: case 143: case 69:
            {
                int d = meta & 7;
                return (d >= 1 && d <= 4 ? TORCH[d] : d) | (meta & 8);
            }

            // low 2 bits facing, +1 mod 4 == one CW step:
            // bed (0=S 1=W 2=N 3=E), pumpkins, fence gate, repeater/comparator
            case 26: case 86: case 91: case 93: case 94: case 107:
            case 149: case 150:
                return (meta & ~3) | ((meta + 1) & 3);

            // doors: lower half low 2 bits facing (0=W 1=N 2=E 3=S) + open
            // bit; upper half (bit 3 set) stores the hinge - untouched
            case 64: case 71:
                return (meta & 8) != 0 ? meta : (meta & ~3) | ((meta + 1) & 3);

            // trapdoors: low 2 bits wall, bit 2 open, bit 3 top half
            case 96: case 167:
                return (meta & ~3) | TRAPDOOR[meta & 3];

            // standing sign: 16-step rotation
            case 63:
                return (meta + 4) & 15;

            // plain rail: full 0-9 straight/slope/curve set
            case 66:
                return RAIL[meta & 15];

            // powered/detector/activator rail: 0-5 only, bit 3 powered
            case 27: case 28: case 157:
            {
                int d = meta & 7;
                return (d <= 5 ? RAIL_POWERED[d] : d) | (meta & 8);
            }

            // vines: bit flags 1=S 2=W 4=N 8=E
            case 106:
                return ((meta & 1) != 0 ? 2 : 0) | ((meta & 2) != 0 ? 4 : 0)
                     | ((meta & 4) != 0 ? 8 : 0) | ((meta & 8) != 0 ? 1 : 0);

            // anvil: bit 0 orientation, bits 2-3 damage
            case 145:
                return meta ^ 1;

            default:
                return meta;
        }
    }

    private static int face6(int meta)
    {
        int d = meta & 7;
        switch (d)
        {
            case 2: d = 5; break; // N -> E
            case 5: d = 3; break; // E -> S
            case 3: d = 4; break; // S -> W
            case 4: d = 2; break; // W -> N
            default: break;       // down/up/unused
        }
        return d | (meta & 8);
    }
}
