package net.decimation.worldgen.devtest;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * The test arena (user request 2026-10-09: tests kept fighting the terrain:
 * tall grass in front of lineups, slopes, NPCs held in mid air falling on
 * the client): one flat stone floor high in the air near the autotest spawn,
 * nothing around it. Every mode stands its NPCs on it and films from it.
 *
 * Floor at Y - 1 (stand at Y) from X - BACK .. X + FRONT and Z - SIDE ..
 * Z + SIDE: long enough west for the 70 block sniper test. Every mode calls
 * build() at its start: a clean floor and no leftovers of earlier modes.
 */
public final class DevTestArena
{
    /** Centre of the arena (blocks) and the height things stand at. */
    public static final int X = 8, Z = 8, Y = 151;
    public static final int BACK = 84, FRONT = 12, SIDE = 24;
    private static boolean built;

    private DevTestArena()
    {
    }

    /**
     * Gets the arena ready for a mode: the floor (and the 4 layers above it
     * cleared of blood marks and leftovers), no entities but the player.
     * The player is moved onto it first so its chunks are loaded.
     */
    public static void build(World w, EntityPlayer p)
    {
        p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
        for (int x = X - BACK; x <= X + FRONT; x++)
        {
            for (int z = Z - SIDE; z <= Z + SIDE; z++)
            {
                if (!built)
                {
                    w.getChunkProvider().loadChunk(x >> 4, z >> 4);
                }
                w.setBlock(x, Y - 1, z, Blocks.stone, 0, 2);
                for (int y = Y; y < Y + 4; y++)
                {
                    if (!w.isAirBlock(x, y, z))
                    {
                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                }
            }
        }
        built = true;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(X - BACK - 8, Y - 20, Z - SIDE - 8,
                                                         X + FRONT + 8, Y + 40, Z + SIDE + 8);
        for (Object o : w.getEntitiesWithinAABB(Entity.class, box))
        {
            if (!(o instanceof EntityPlayer))
            {
                ((Entity) o).setDead(); // mobs, dropped items, corpses of earlier modes
            }
        }
    }

    /** Removes every mob on and around the arena except those kept. */
    public static void clear(World w, java.util.Collection<?> keep)
    {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(X - BACK - 8, Y - 20, Z - SIDE - 8,
                                                         X + FRONT + 8, Y + 40, Z + SIDE + 8);
        for (Object o : w.getEntitiesWithinAABB(EntityLiving.class, box))
        {
            if (keep == null || !keep.contains(o))
            {
                ((Entity) o).setDead();
            }
        }
    }
}
