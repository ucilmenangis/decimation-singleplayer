package net.decimation.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.worldgen.StructureGenerator.Sub;
import net.minecraft.block.Block;

/**
 * Second {@code @Mod} entry point shipped inside Decimation.jar.
 *
 * Forge scans every class in a jar for the {@code @Mod} annotation, so this
 * loads alongside the original "deci" mod without any bytecode edit to the
 * obfuscated mod class. Everything world-generation related is owned here, in
 * ordinary readable Java, and can be removed simply by deleting these entries
 * from the jar.
 *
 * World generation is deliberately implemented as an {@link
 * cpw.mods.fml.common.IWorldGenerator} rather than a custom {@code WorldType}.
 * IWorldGenerator runs per chunk on top of whichever terrain generator the
 * world was created with, so the realistic terrain can come from a dedicated
 * terrain mod (RTG) while structure placement stays ours.
 *
 * Structures are read from standard MCEdit/WorldEdit {@code .schematic} files
 * in {@code config/decimation_worldgen/} - drop files in, restart the game.
 * Filename prefix picks the sector: civ_ / city_ / mil_, untagged = anywhere.
 */
@Mod(modid = DecimationWorldGen.MODID,
     name = "Decimation World Generation",
     version = "0.7.0",
     dependencies = "required-after:deci")
public class DecimationWorldGen
{
    public static final String MODID = "deciworldgen";

    private final List<Schematic> schematics = new ArrayList<Schematic>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        File dir = new File(event.getModConfigurationDirectory(), "decimation_worldgen");
        if (!dir.isDirectory() && !dir.mkdirs())
        {
            FMLLog.info("[%s] could not create %s", MODID, dir);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null)
        {
            return;
        }
        for (File f : files)
        {
            if (!f.getName().toLowerCase().endsWith(".schematic"))
            {
                continue;
            }
            try
            {
                Schematic s = Schematic.load(f);
                if (s.width > StructureGenerator.MAX_FOOTPRINT
                    || s.length > StructureGenerator.MAX_FOOTPRINT)
                {
                    FMLLog.info("[%s] skipping '%s': footprint %dx%d exceeds max %d",
                                MODID, s.name, s.width, s.length,
                                StructureGenerator.MAX_FOOTPRINT);
                    continue;
                }
                schematics.add(s);
                FMLLog.info("[%s] loaded schematic '%s' (%dx%dx%d)",
                            MODID, s.name, s.width, s.height, s.length);
            }
            catch (Exception e)
            {
                FMLLog.info("[%s] failed to load '%s': %s", MODID, f.getName(), e);
            }
        }
        FMLLog.info("[%s] %d schematic(s) ready", MODID, schematics.size());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        GameRegistry.registerWorldGenerator(
            new StructureGenerator(schematics, buildSubstitutions(), roadBlocks()),
            100);
        FMLLog.info("[%s] structure generator registered", MODID);

        // singleplayer fixes that don't belong to world generation but need
        // a registration home - this is the only @Mod class we own
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.BottlecapHandler());
        FMLLog.info("[%s] bottlecap pickup fix registered", MODID);

        // generated structures carry Decimation zones (military / police)
        ZoneStore store = new ZoneStore();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(store);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(store);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.ZoneSpawnHandler());
        FMLLog.info("[%s] zone store and zone spawn fix registered", MODID);

        // PROPERTY is a compile-time constant, so a dedicated server never
        // loads DevAutoTest (it references client-only classes)
        if (Boolean.getBoolean(DevAutoTest.PROPERTY)
            && cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            DevAutoTest test = new DevAutoTest();
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(test);
            FMLLog.info("[%s] AUTOTEST enabled", MODID);
        }
    }

    /**
     * Placeholder -> Decimation-prop substitution. Schematics use vanilla
     * marker blocks; at placement they become the mod's own props, resolved
     * BY REGISTRY NAME here - immune to FML's per-save numeric id
     * assignment. Registry names are the CamelCase class names (verified
     * against the FML registry snapshot in a world's level.dat) - the
     * lowercase prop_* strings in the jar are texture paths, not registry
     * names. Runs in init, after every mod's block registration.
     *
     * Key = (placeholderId << 4) | metadata. Wool (35) and stained clay
     * (159) carry 16 prop slots each in their colour metadata; the other
     * placeholders match on every metadata value. The Sub mode controls
     * facing: RANDOM for clutter, FIXED (rotates with the structure) for
     * furniture, FACE_ROAD for street furniture, PLAIN for surfaces.
     */
    private Map<Integer, Sub> buildSubstitutions()
    {
        Map<Integer, Sub> subs = new HashMap<Integer, Sub>();
        // lootable crates - RANDOM facing for subtle variety
        add(subs, 19, -1, Sub.RANDOM, "deci:BlockWoodCrate");         // sponge
        add(subs, 41, -1, Sub.RANDOM, "deci:BlockMilitaryCrate");     // gold block
        add(subs, 22, -1, Sub.RANDOM, "deci:BlockAmmoCrate");         // lapis block
        add(subs, 57, -1, Sub.RANDOM, "deci:BlockMedicalCrate");      // diamond block
        add(subs, 133, -1, Sub.RANDOM, "deci:BlockPoliceCrate");      // emerald block
        add(subs, 42, -1, Sub.RANDOM, "deci:BlockWreckage1",
            "deci:BlockWreckage2", "deci:BlockWreckage3",
            "deci:BlockWreckage4", "deci:BlockWreckage5");            // iron block
        // asphalt: coal block -> the mod's own road surface
        add(subs, 173, -1, Sub.PLAIN, "deci:BlockRoad");
        // wool colour -> street/scene props
        add(subs, 35, 0, Sub.FIXED, "deci:BlockSandbagStack");        // white
        add(subs, 35, 1, Sub.RANDOM, "deci:BlockCone");               // orange
        add(subs, 35, 2, Sub.FIXED, "deci:BlockVendingMachine_1",
            "deci:BlockVendingMachine_2");                            // magenta
        add(subs, 35, 3, Sub.FACE_ROAD_AWAY, "deci:BlockStreetLight"); // light blue
        add(subs, 35, 4, Sub.FIXED, "deci:BlockHazardbarrier",
            "deci:BlockStreetBarrier");                               // yellow
        add(subs, 35, 5, Sub.RANDOM, "deci:BlockStreetBin");          // lime
        add(subs, 35, 6, Sub.FACE_ROAD, "deci:BlockStreetBench");     // pink
        add(subs, 35, 7, Sub.FIXED, "deci:BlockDumpster");            // gray
        add(subs, 35, 8, Sub.RANDOM, "deci:BlockTire",
            "deci:BlockTireStack");                                   // light gray
        add(subs, 35, 9, Sub.RANDOM, "deci:BlockBarrel");             // cyan
        add(subs, 35, 10, Sub.RANDOM, "deci:BlockCardboardBoxes1",
            "deci:BlockCardboardBoxes2", "deci:BlockCardboardBoxes3"); // purple
        add(subs, 35, 11, Sub.RANDOM, "deci:BlockTrashBag1",
            "deci:BlockTrashBag2");                                   // blue
        add(subs, 35, 12, Sub.FIXED, "deci:BlockElectricBox1",
            "deci:BlockElectricBox2");                                // brown
        add(subs, 35, 13, Sub.FIXED, "deci:BlockMetalShelf",
            "deci:BlockMetalShelf_Empty");                            // green
        add(subs, 35, 14, Sub.RANDOM, "deci:BlockCanFire");           // red
        add(subs, 35, 15, Sub.FACE_ROAD, "deci:BlockRoadsignStop",
            "deci:BlockRoadsign50", "deci:BlockRoadsignOneway");      // black
        // stained clay colour -> road markings + furniture
        add(subs, 159, 0, Sub.FIXED, "deci:BlockRoad_CenterLine");    // white
        add(subs, 159, 1, Sub.FIXED, "deci:BlockRoad_YellowLine");    // orange
        add(subs, 159, 2, Sub.FIXED, "deci:BlockWoodTable",
            "deci:BlockWoodTable2");                                  // magenta
        add(subs, 159, 3, Sub.FIXED, "deci:BlockChair");              // light blue
        add(subs, 159, 4, Sub.RANDOM, "deci:BlockOfficeChair");       // yellow
        add(subs, 159, 5, Sub.FIXED, "deci:BlockMetalTable");         // lime
        add(subs, 159, 6, Sub.FACE_ROAD, "deci:BlockMailbox");        // pink
        add(subs, 159, 7, Sub.FACE_ROAD, "deci:BlockPhonebooth");     // gray
        add(subs, 159, 8, Sub.FACE_ROAD, "deci:BlockNewsStand1",
            "deci:BlockNewsStand2");                                  // light gray
        add(subs, 159, 9, Sub.RANDOM, "deci:BlockStretcher");         // cyan
        add(subs, 159, 10, Sub.RANDOM, "deci:BlockTrashcan");         // purple
        add(subs, 159, 11, Sub.FIXED, "deci:BlockWashingMachine");    // blue
        add(subs, 159, 12, Sub.FIXED, "deci:BlockCookingStation");    // brown
        add(subs, 159, 13, Sub.FIXED, "deci:BlockWeaponCabinet");     // green
        add(subs, 159, 14, Sub.FIXED, "deci:BlockConcertinaWire");    // red
        add(subs, 159, 15, Sub.RANDOM, "deci:BlockHedgehog",
            "deci:BlockSkeletonGround");                              // black
        return subs;
    }

    /** Road-surface blocks, used by FACE_ROAD prop orientation. */
    private Set<Block> roadBlocks()
    {
        Set<Block> roads = new HashSet<Block>();
        for (String name : new String[] {
            "deci:BlockRoad", "deci:BlockRoad_CenterLine",
            "deci:BlockRoad_YellowLine", "deci:BlockRoad_DoubleYellowLine",
            "deci:BlockRoad_RedLine", "deci:BlockRoad_StopLine"})
        {
            Block b = Block.getBlockFromName(name); // getBlockFromName
            if (b != null)
            {
                roads.add(b);
            }
        }
        return roads;
    }

    /** meta -1 = match every metadata value of the placeholder id. */
    private void add(Map<Integer, Sub> subs, int placeholderId, int meta,
                     int mode, String... names)
    {
        List<Block> targets = new ArrayList<Block>();
        for (String name : names)
        {
            Block b = Block.getBlockFromName(name); // getBlockFromName
            if (b != null)
            {
                targets.add(b);
            }
            else
            {
                FMLLog.info("[%s] substitution target '%s' not found", MODID, name);
            }
        }
        if (targets.isEmpty())
        {
            return;
        }
        Sub sub = new Sub(mode, targets.toArray(new Block[0]));
        if (meta < 0)
        {
            for (int m = 0; m < 16; m++)
            {
                subs.put((placeholderId << 4) | m, sub);
            }
        }
        else
        {
            subs.put((placeholderId << 4) | meta, sub);
        }
    }
}
