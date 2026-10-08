package net.decimation.worldgen.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.decimation.worldgen.Plan;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.Slices;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import static net.decimation.worldgen.city.LcCity.*;

/** Street furniture (same blocks and facing rules as StructureGenerator's streets). */
public final class StreetProps
{
    public Block lamp, bench, bin, centreLine, sidewalk = Blocks.double_stone_slab;
    public Block[] trashBags = new Block[0], cars = new Block[0];
}
