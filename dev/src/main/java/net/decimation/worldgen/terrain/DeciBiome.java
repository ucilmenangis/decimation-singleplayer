package net.decimation.worldgen.terrain;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenTrees;

/**
 * One biome of the Decimation world type. Plain data: terrain height, colours,
 * decoration counts, the tree mix and optional ground patches (gravel and
 * coarse dirt for military ground). Built by {@link DecimationBiomes}.
 */
public class DeciBiome extends BiomeGenBase
{
    /** Share of trees that are dead (0..1); the rest are live oaks. */
    float deadTrees;
    /** Live trees carry vines (nature reclaiming the land). */
    boolean vines;
    /** Share of top blocks turned to gravel / coarse dirt, 0 for none. */
    float patches;
    int grass = -1, foliage = -1, sky = -1;

    private static final NoiseGeneratorSimplex PATCH_NOISE = new NoiseGeneratorSimplex(new Random(0xDEC1L));
    private final WorldGenAbstractTree deadTree = new DeadTree();
    private final WorldGenAbstractTree vineTree = new WorldGenTrees(false, 5, 0, 0, true);

    DeciBiome(int id, String name)
    {
        super(id);
        setBiomeName(name);
        setTemperatureRainfall(0.7F, 0.5F); // one temperate climate, never snow
    }

    DeciBiome height(float root, float variation)
    {
        setHeight(new Height(root, variation));
        return this;
    }

    DeciBiome colours(int grass, int foliage, int sky, int water)
    {
        this.grass = grass;
        this.foliage = foliage;
        this.sky = sky;
        this.waterColorMultiplier = water;
        return this;
    }

    DeciBiome decor(int trees, int tallGrass, int deadBushes, int flowers)
    {
        theBiomeDecorator.treesPerChunk = trees;
        theBiomeDecorator.grassPerChunk = tallGrass;
        theBiomeDecorator.deadBushPerChunk = deadBushes;
        theBiomeDecorator.flowersPerChunk = flowers;
        return this;
    }

    DeciBiome trees(float dead, boolean vines)
    {
        this.deadTrees = dead;
        this.vines = vines;
        return this;
    }

    DeciBiome patches(float share)
    {
        this.patches = share;
        return this;
    }

    @Override
    public WorldGenAbstractTree func_150567_a(Random rand) // tree for this biome
    {
        if (rand.nextFloat() < deadTrees)
        {
            return deadTree;
        }
        if (vines && rand.nextInt(3) != 0)
        {
            return vineTree;
        }
        return rand.nextInt(10) == 0 ? worldGeneratorBigTree : worldGeneratorTrees;
    }

    /**
     * Vanilla decoration with block updates run at once, as vanilla places its springs: every gravel and sand block
     * of the ore veins and shore discs queued a tick otherwise (about 800 a chunk), and each chunk save walks the whole
     * queue (188 000 entries after 225 chunks: 3.4 s of saving; docs/performance.md 9). A resting block's tick does
     * nothing; one over a cave falls straight away (population drops falling blocks instantly). Liquids and fire
     * still queue theirs.
     */
    @Override
    public void decorate(World world, Random rand, int x, int z)
    {
        boolean was = world.scheduledUpdatesAreImmediate;
        world.scheduledUpdatesAreImmediate = true;
        try
        {
            super.decorate(world, rand, x, z);
        }
        finally
        {
            world.scheduledUpdatesAreImmediate = was;
        }
    }

    @Override
    public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] metas, int x, int z, double noise)
    {
        genBiomeTerrain(world, rand, blocks, metas, x, z, noise);
        if (patches <= 0 || PATCH_NOISE.func_151605_a(x / 9.0, z / 9.0) < 1 - 2 * patches)
        {
            return;
        }
        // same column indexing as genBiomeTerrain: (z & 15) * 16 + (x & 15)
        int height = blocks.length / 256;
        int base = ((z & 15) * 16 + (x & 15)) * height;
        for (int y = height - 1; y > 0; y--)
        {
            Block b = blocks[base + y];
            if (b == null || b == Blocks.air)
            {
                continue;
            }
            if (b == Blocks.grass)
            {
                boolean gravel = ((x * 31 + z * 17) & 3) == 0;
                blocks[base + y] = gravel ? Blocks.gravel : Blocks.dirt;
                metas[base + y] = (byte) (gravel ? 0 : 1); // coarse dirt
            }
            return;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBiomeGrassColor(int x, int y, int z)
    {
        return grass >= 0 ? grass : super.getBiomeGrassColor(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBiomeFoliageColor(int x, int y, int z)
    {
        return foliage >= 0 ? foliage : super.getBiomeFoliageColor(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature)
    {
        return sky >= 0 ? sky : super.getSkyColorByTemp(temperature);
    }
}
