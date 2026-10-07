package net.decimation.worldgen.building;

import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Lot grading around a building (Graded.grade): the ground ramps from the
 * floor level next to the building to the natural height at the lot edge
 * (smoothstep), so a building on a slope sits in a levelled yard instead
 * of on a plinth or in a pit. The column keeps its own surface block
 * (grass, sand, snow...), so the yard matches the biome. In front of
 * offices and shops with a setback: an asphalt parking strip (where the
 * ground is near floor level) with a slab walkway to the door and some
 * nose-in wrecks; apartments get a gravel path to the door.
 */
final class Yard
{
    private final Building b;

    Yard(Building b)
    {
        this.b = b;
    }

    void grade(World world, int x, int z, int baseY)
    {
        int minX = b.minX, minZ = b.minZ, lotX = b.lotX, lotZ = b.lotZ;
        int bx1 = minX + b.width - 1, bz1 = minZ + b.length - 1;
        if (x < lotX || z < lotZ || x > b.lotMaxX() || z > b.lotMaxZ()
            || x >= minX && x <= bx1 && z >= minZ && z <= bz1)
        {
            return;
        }
        int natural = StructureGenerator.soilTop(world, x, z);
        if (natural < 5 || StructureGenerator.waterAbove(world, x, natural, z))
        {
            return;
        }
        // 0 on the margin ring, 0 at the lot edge
        int d = Math.max(Math.max(minX - x, x - bx1), Math.max(minZ - z, z - bz1)) - 1;
        int e = Math.min(Math.min(x - lotX, b.lotMaxX() - x), Math.min(z - lotZ, b.lotMaxZ() - z));
        double t = d + e == 0 ? 0 : (double) d / (d + e);
        t = t * t * (3 - 2 * t);
        int target = baseY + (int) Math.round((natural - baseY) * t);

        Block surface = world.getBlock(x, natural, z);
        int surfaceMeta = world.getBlockMetadata(x, natural, z);
        Block cap = world.getBlock(x, natural + 1, z);
        if (surface == Blocks.air || surface.getMaterial().isLiquid())
        {
            return;
        }
        // sand and gravel fall into any cave under the yard (onBlockAdded
        // schedules the fall even during generation): never use them as fill
        Block filler = surface == Blocks.grass || surface == Blocks.mycelium ? Blocks.dirt
            : surface instanceof net.minecraft.block.BlockFalling
                ? (surface == Blocks.sand ? Blocks.sandstone : Blocks.stone)
            : surface;
        int fillerMeta = filler == surface ? surfaceMeta : 0;

        // front yard: between the front wall and the street edge of the lot
        boolean frontYard = b.front == Building.FRONT_WEST ? x < minX : x > bx1;
        int setback = b.front == Building.FRONT_WEST ? minX - lotX : b.lotMaxX() - bx1;
        boolean path = frontYard && Math.abs(z - (minZ + b.entranceZ)) <= 1;
        // asphalt only where the ground is near the floor level: on a slope
        // the strip stays a grassy embankment instead of a tilted car park
        boolean parking = frontYard && setback >= Building.MIN_YARD && b.kind != Building.APARTMENT
            && b.props.road != null && Math.abs(natural - baseY) <= 2;
        boolean paved = path || parking;
        if (path)
        {
            surface = b.kind == Building.APARTMENT ? Blocks.gravel : Blocks.double_stone_slab;
            surfaceMeta = 0;
        }
        else if (parking)
        {
            surface = b.props.road;
            surfaceMeta = 0;
        }

        for (int y = natural; y < target; y++)
        {
            world.setBlock(x, y, z, filler, fillerMeta, 2);
        }
        for (int y = target + 1; y <= natural; y++)
        {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
        }
        if (surface instanceof net.minecraft.block.BlockFalling
            && !world.getBlock(x, target - 1, z).getMaterial().isSolid())
        {
            surface = filler; // a gravel path or sand over a cut into a cave
            surfaceMeta = fillerMeta;
        }
        world.setBlock(x, target, z, surface, surfaceMeta, 2);
        // plants and trees left hanging over a cut, or on new paving
        if (target < natural || paved)
        {
            for (int y = Math.max(target, natural) + 1, n = 0; n < 12; y++, n++)
            {
                Block above = world.getBlock(x, y, z);
                if (above == Blocks.air || !StructureGenerator.clearable(above))
                {
                    break;
                }
                world.setBlock(x, y, z, Blocks.air, 0, 2);
            }
        }
        if (cap == Blocks.snow_layer && !paved)
        {
            world.setBlock(x, target + 1, z, Blocks.snow_layer, 0, 2);
        }

        // nose-in wrecks across the middle of the parking strip
        int stripMid = b.front == Building.FRONT_WEST ? lotX + (setback - 1) / 2 : b.lotMaxX() - (setback - 1) / 2;
        if (parking && !path && x == stripMid && b.props.cars.length > 0
            && (z - lotZ) % 4 == 2 && Math.abs(z - (minZ + b.entranceZ)) >= 3
            && Math.abs(target - baseY) <= 1 && b.unit(x, 77, z) < 0.45)
        {
            Block car = b.props.cars[(int) (b.unit(x, 78, z) * b.props.cars.length)];
            // long axis along x at rotation 0 / 180: metadata 4 or 2
            world.setBlock(x, target + 1, z, car, b.unit(x, 79, z) < 0.5 ? 4 : 2, 2);
            net.decimation.fixes.MultiblockRepairHandler.repair(world.getTileEntity(x, target + 1, z));
        }
    }
}
