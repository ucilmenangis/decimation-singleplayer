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
import net.decimation.worldgen.building.Building;
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
     version = "0.43.0",
     useMetadata = true, // the real version comes from mcmod.info (Gradle fills it from the git tag)
     dependencies = "required-after:deci")
public class DecimationWorldGen
{
    public static final String MODID = "deciworldgen";
    /** The city planner, for the dev autotest's audit views. */
    static CityDistrict city;

    /** The procedural city planner (dev tests), or null. */
    public static CityDistrict city()
    {
        return city;
    }

    private final List<Schematic> schematics = new ArrayList<Schematic>();
    private final List<Schematic> largeSchematics = new ArrayList<Schematic>();

    private net.decimation.fixes.NpcLoadouts npcLoadouts;
    /** Zone rules (dev tests read the provocation state). */
    static net.decimation.fixes.ZoneRules zoneRules;

    public static net.decimation.fixes.ZoneRules zoneRules()
    {
        return zoneRules;
    }
    private net.decimation.fixes.MilitarySpawner militarySpawner;
    private net.decimation.fixes.VanillaMobs vanillaMobs;
    private net.decimation.fixes.InfectedVariants infectedVariants;

    /** config/ (preInit), for the files loaded in postInit. */
    private File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            net.decimation.fixes.GlInfo.log();             // which GPU and Java (bug.md black mobs)
        }
        // Decimation world type: biomes must exist before Decimation's init
        // strips vanilla monsters from every biome (see DecimationBiomes)
        net.decimation.worldgen.terrain.DecimationBiomes.create();
        net.decimation.worldgen.terrain.DecimationWorldType.INSTANCE =
            new net.decimation.worldgen.terrain.DecimationWorldType();
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(
            new net.decimation.worldgen.terrain.TerrainEvents());
        // cities of 4 to 64 districts (CityLayout): on for worlds created since 0.60 (stored per world);
        // registered first so the other world load handlers already see it
        CityLayoutEvents.configure(event.getModConfigurationDirectory());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new CityLayoutEvents());
        // rivers through cities: on for worlds created since 0.45 (stored per world)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.worldgen.city.CityRivers());
        // the wasteland district as a military area: its military building pick for worlds created since 0.54
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.worldgen.city.Wasteland());

        // cheap scope: view zoom instead of Decimation's picture in picture
        // scope (tools/patches/PatchScope.java gates the old one); client only
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            net.decimation.fixes.ScopeZoom zoom =
                new net.decimation.fixes.ScopeZoom(event.getModConfigurationDirectory());
            if (zoom.enabled())
            {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(zoom);
                cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(zoom);
            }
            net.decimation.fixes.SpecterKey.register(); // the SpecterDR's 1x / 4x switch (default G)
            // HUD style Decimation / Inline (J, Decimation's Settings page): Minecraft style bars and the player in the corner
            net.decimation.fixes.HudStyle.install(event.getModConfigurationDirectory());
            // prop render distance by size (tools/patches/PatchPropCulling.java step 4)
            net.minecraftforge.common.config.Configuration props = new net.minecraftforge.common.config.Configuration(
                new File(event.getModConfigurationDirectory(), "deciworldgen_props.cfg"));
            // 2026-10-10 (docs/performance.md section 5): looking over a city 48 / 32 / 24 took the
            // props' cost (about 18 % of the frame, 34 vs 41 fps with them hidden) to nothing, with
            // no visible loss at those distances; 64 = vanilla for all
            String[][] dist = {{"small", "24", "props smaller than 0.8 block (cans, bags, cones); 64 = vanilla"},
                               {"medium", "32", "props up to 1.6 blocks (crates, bins, benches); 64 = vanilla"},
                               {"large", "48", "bigger props (cars, shelves, lamps); 64 = vanilla"}};
            for (String[] d : dist)
            {
                System.setProperty("decimation.props." + d[0], String.valueOf(props.getInt(d[0], "distance",
                    Integer.parseInt(d[1]), 8, 128, "render distance in blocks for " + d[2])));
            }
            props.save();
        }

        // bandit / soldier / Soviet tiers (both sides: the client applies the synced gun)
        npcLoadouts = new net.decimation.fixes.NpcLoadouts(event.getModConfigurationDirectory());
        militarySpawner = new net.decimation.fixes.MilitarySpawner(event.getModConfigurationDirectory());
        vanillaMobs = new net.decimation.fixes.VanillaMobs(event.getModConfigurationDirectory());
        infectedVariants = new net.decimation.fixes.InfectedVariants(event.getModConfigurationDirectory());
        // 60 round STANAG / AK magazines: guns and loot of Decimation exist after its preInit
        net.decimation.fixes.Magazines.register();
        net.decimation.fixes.ArmorGunfireHandler.setMaxima(); // armor 256, vests 512 (feature 10: durability)
        net.decimation.fixes.RepairPart.register(); // armor kits, plates, gun parts; military and police loot
        // guns of our own (tools/guns, assets/deci/...): the MAC-10 pilot
        net.decimation.fixes.NewGuns.register();
        // guns from gun packages, data driven (fixes/gunpack, docs/gun_model_spec.md section 7): before the tables are read
        net.decimation.fixes.gunpack.GunPacks.register();
        net.decimation.fixes.NewSights.register(); // before SightPlacement: it moves these too
        net.decimation.fixes.NewGrips.register(); // the RK-1 Foregrip (before GripPlacement)
        net.decimation.fixes.NewMelee.register(); // the Karambit and the Katana (tools/guns/melee.py)
        net.decimation.fixes.GripRails.load(); // a Foregrip or RK-1 only on a gun with a rail (grip_offsets.txt)
        net.decimation.fixes.SightMounts.load(); // a sight only on its mount (sight_mounts.txt)
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.MountStrip());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.LootMagazines()); // loot mags half full
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.LootRefill()); // looted stays looted
        net.decimation.fixes.Net.register(); // our own channel (fixes/Net)
        net.decimation.fixes.MagUnload unload = new net.decimation.fixes.MagUnload(); // shift + right click empties a magazine
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(unload);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(unload);
        net.decimation.fixes.RadiationDose radiation = new net.decimation.fixes.RadiationDose(); // dose in percent (fixes/RadiationDose)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(radiation);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(radiation);
        net.decimation.fixes.MoonClock.install(); // moon phases from our day counter, not Decimation's reset world time
        net.decimation.fixes.MoonClock moon = new net.decimation.fixes.MoonClock();
        net.decimation.fixes.WorldClock.listen(moon);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(moon);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.GunWear()); // gun wear: racks
        // feature 7, radiation set: gas mask filters wear only in radiation and are refitted with Decimation's Gas Mask Filter
        net.decimation.fixes.GasMaskFilter filters = new net.decimation.fixes.GasMaskFilter();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(filters);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(filters);
        net.decimation.fixes.GasMaskFilter.addLoot(); // Gas Mask Filters in military and police loot
        net.decimation.fixes.RadiationPills.register(); // radiation pills: 40 points off the dose, medical loot
        net.decimation.fixes.LootBlocks.register(); // lootable copies of props, police and military shelves, cabinets (feature 12 B)
        net.decimation.fixes.LootSearch.init(); // items set apart by search time (feature 12 C)
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.LootSearch()); // loot searched slot by slot
        net.decimation.fixes.TraderMarket market = new net.decimation.fixes.TraderMarket(); // trader prices by day (feature 11)
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(market);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(market);
        // the wasteland city one radiation zone: worlds made before it get theirs at load (city/WastelandRadiation)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.worldgen.city.WastelandRadiation());
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            // right click fits a filter, "Filter: N%" in a gas mask's tooltip, Decimation's HUD unprotected when it is empty
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.GasMaskFilter.Client());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.LootBlocks.Tooltip()); // "Lootable: ..."
            net.decimation.fixes.LootSearch.Client search = new net.decimation.fixes.LootSearch.Client(); // "?", spinner, no tooltip
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(search);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(search);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.TraderMarket.Client()); // on sale, wanted
        }
        // feature 8, specific mobs at specific places: a table per place kind (config/deciworldgen_places.json, loaded in
        // postInit) turns Decimation's natural infected spawns into the place's mobs (fixes/PlaceSpawns, worldgen/Places)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.PlaceSpawns());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Places()); // its chunk memo cleared at world load
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            net.decimation.fixes.SightPlacement.apply(); // sights onto each gun's rail (sight_offsets.txt)
            net.decimation.fixes.GripPlacement.apply(); // grips under each gun's handguard (grip_offsets.txt)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.RadiationHud()); // the dose icon
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.PlayerSkins()); // no Steve
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.DarkNights()); // DayZ nights
            net.decimation.fixes.GunWearClient wearClient = new net.decimation.fixes.GunWearClient(); // jams, R to rack, tooltips
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(wearClient);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(wearClient);
        }
        // local kill / death counters for the menu and HUD; the menu's session check calls a
        // Mojang service that no longer answers, so the menu said "Play offline" with an invalid
        // session banner for every account: singleplayer does not need it (bug.md)
        net.decimation.fixes.LocalStats stats = new net.decimation.fixes.LocalStats(event.getModConfigurationDirectory());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(stats);
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(stats);
            net.decimation.fixes.Deci.setSessionValid(true);
            net.decimation.fixes.LocalStats.show();
        }
        // gunshot noise: infected and hostile NPCs walk to shots they hear (fixes/GunNoise)
        net.decimation.fixes.GunNoise noise = new net.decimation.fixes.GunNoise(event.getModConfigurationDirectory());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(noise);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(noise);
        // HESCO cells of our bases: a plain block, not Decimation's per cell prop (docs/military_base.md)
        net.decimation.worldgen.military.BlockHesco.instance = new net.decimation.worldgen.military.BlockHesco();
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(net.decimation.worldgen.military.BlockHesco.instance, "hesco");
        net.decimation.worldgen.military.BlockBoomArm.instance = new net.decimation.worldgen.military.BlockBoomArm();
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(net.decimation.worldgen.military.BlockBoomArm.instance, "boom_barrier");
        cpw.mods.fml.common.registry.GameRegistry.registerItem(
            new net.decimation.fixes.ZombieEgg(infectedVariants), "zombie_egg");
        cpw.mods.fml.common.registry.GameRegistry.registerItem(
            new net.decimation.fixes.NpcEgg(npcLoadouts), "npc_egg");
        // shared core (docs/systems_plan.md section 2): the world's day and its events, our areas
        configDir = event.getModConfigurationDirectory();
        // auto update from our GitHub releases (fixes/Updater): Decimation's update page opens ours
        net.decimation.fixes.Updater.configure(configDir);
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            // the update on Decimation's own progress screen: GUI events on Forge's bus, its tick on FML's
            net.decimation.fixes.UpdateProgress update = new net.decimation.fixes.UpdateProgress();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(update);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(update);
            // our news in the main menu's own news panels (GitHub, releases, website, coming soon)
            net.decimation.fixes.MenuNews news = new net.decimation.fixes.MenuNews();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(news);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(news);
            // "Continue" under Play: the last played world in one click
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.MenuContinue());
            // Server Status rows from real sources: Global Server, Community Server, Download (fixes/MenuStatus)
            net.decimation.fixes.MenuStatus status = new net.decimation.fixes.MenuStatus();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(status);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(status);
            // the corner art bottom right (our soldier, JOIN THE REMNANT) opens our Discord
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.MenuCorner());
            // Quit game on the update screens: leaves a world and lets it save before the game closes
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.SafeQuit());
            // a newer release: the menu asks Update or Later (never forced; the release page where it cannot swap)
            net.decimation.fixes.UpdatePrompt prompt = new net.decimation.fixes.UpdatePrompt();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(prompt);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(prompt);
            // Decimation's HUD needs the menu's player preview (bug.md): made if a world was reached without the menu
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.HudGuard());
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.GunUseKey()); // open corpses with a gun in hand
            // prop loops (radios, vending machines, electric boxes) stop out of hearing, 6 at most (bug.md "FPS falls")
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.PropSounds());
            // the hand holding a gun shows the player's own skin, not Steve (bug.md "Steve hand")
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.GunArmSkin());
            // a cape showed in front of the camera: Decimation's first person body drew it (bug.md "Cape in first person")
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.FirstPersonCape());
            // shift click in Decimation's inventory screen: its client container swapped for ours (bug.md)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.InventoryShift.Client());
            // the muzzle flash lit every mob drawn after a shooter: lighting put back around each living entity (bug.md)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.EntityLighting());
            // host your world online (docs/superpowers/specs/2026-10-18-player-hosting-part1-design.md): the pause menu's
            // Host Online page, the heartbeat on the client tick, hosting stops when the world closes
            net.decimation.fixes.hosting.HostMenu host = new net.decimation.fixes.hosting.HostMenu();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(host);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(host);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(net.decimation.fixes.hosting.HostService.INSTANCE);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.hosting.HostOps());
            // Decimation's Community page filled from our list, joins through the relay
            net.decimation.fixes.hosting.CommunityList community = new net.decimation.fixes.hosting.CommunityList();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(community);
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(community); // disconnects
            // Server Status: Community Server is up when our relay answers its list (fixes/MenuStatus; main only)
            net.decimation.fixes.MenuStatus.community = new java.util.concurrent.Callable<Boolean>()
            {
                public Boolean call() throws Exception
                {
                    new net.decimation.fixes.hosting.RelayClient(net.decimation.fixes.hosting.RelayClient.base()).list();
                    return true;
                }
            };
        }
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.WorldClock());
        // throwables left a stack of 0 in the hand that threw forever (bug.md "The last grenade never runs out")
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.EmptyStacks());
        // an entity joins a world once: Decimation's mob swaps during a chunk load added the new mob twice (bug.md)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.JoinOnce());
        // Decimation's mobs searched for targets every tick: paced like vanilla's (bench "crowd", bug.md "Safezones lag")
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.fixes.TargetPacing());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Areas());
        // loot of our own on Decimation's loot screen (docs/loot_levels.md): opener and the body entity
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new net.decimation.fixes.Loot());
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(net.decimation.fixes.LootBody.class, "loot_body", 0, this,
            64, 20, false);

        File dir = new File(event.getModConfigurationDirectory(), "decimation_worldgen");
        if (!dir.isDirectory() && !dir.mkdirs())
        {
            FMLLog.info("[%s] could not create %s", MODID, dir);
            return;
        }
        // the release jar carries the city pack and schematics: unpack them before anything reads the folder
        BundledContent.unpack(dir);
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

        // large schematics: any footprint up to LargeSites.MAX_SIZE
        File largeDir = new File(dir, "large");
        largeDir.mkdirs();
        File[] largeFiles = largeDir.listFiles();
        if (largeFiles != null)
        {
            for (File f : largeFiles)
            {
                if (!f.getName().toLowerCase().endsWith(".schematic"))
                {
                    continue;
                }
                try
                {
                    Schematic s = Schematic.load(f);
                    if (s.width > LargeSites.MAX_SIZE || s.length > LargeSites.MAX_SIZE)
                    {
                        FMLLog.info("[%s] skipping large '%s': %dx%d exceeds %d",
                                    MODID, s.name, s.width, s.length, LargeSites.MAX_SIZE);
                        continue;
                    }
                    largeSchematics.add(s);
                    FMLLog.info("[%s] loaded large schematic '%s' (%dx%dx%d)",
                                MODID, s.name, s.width, s.height, s.length);
                }
                catch (Exception e)
                {
                    FMLLog.info("[%s] failed to load large '%s': %s", MODID, f.getName(), e);
                }
            }
        }
    }

    @Mod.EventHandler
    public void serverStarting(cpw.mods.fml.common.event.FMLServerStartingEvent event)
    {
        event.registerServerCommand(new WorldGenCommand());
        // dev workspace only (deobfuscated run, never the shipped jar): every
        // singleplayer world allows commands, as if created with cheats on
        if (Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"))
            && !event.getServer().isDedicatedServer())
        {
            event.getServer().getConfigurationManager().setCommandsAllowedForAll(true);
            FMLLog.info("[%s] dev workspace: commands allowed in this world", MODID);
        }
    }

    @Mod.EventHandler
    public void postInit(cpw.mods.fml.common.event.FMLPostInitializationEvent event)
    {
        // loot lists and the protection table name items of every mod: all registered by now
        net.decimation.fixes.LootPools.load(configDir);
        net.decimation.fixes.gunpack.GunPacks.postInit(); // gun package guns join the lists of their like guns
        net.decimation.fixes.Protection.load(configDir);
        net.decimation.fixes.PlaceSpawns.load(configDir); // the place spawn table (feature 8): after InfectedVariants' looks
        net.decimation.fixes.hosting.OptionalMods.install(); // joiners without the voice chat jar (bug.md)
        net.decimation.fixes.gunsmith.GunsmithGuiHandler.install(); // the attach key opens the Gunsmith screen
        net.decimation.fixes.InventoryShift.install(); // shift click in the inventory screen (bug.md), after the Gunsmith's
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            net.decimation.fixes.Updater.check();     // the menu asks once a newer release is known (UpdatePrompt)
            net.decimation.fixes.GlStackGuard.install(); // Decimation's renderers left GL matrices pushed (bug.md)
            net.decimation.fixes.GunDraw.install(); // every gun drawn its own way, with its rack (fixes/GunDraw)
            net.decimation.fixes.HumanLayers.install(); // NPC looks drawn from one picture, not seven layers
            net.decimation.fixes.ModelWarmup.install(); // NPC textures and gear models loaded before their first draw
        }
        net.decimation.worldgen.terrain.DecimationBiomes.copySpawns();
        if (vanillaMobs != null && vanillaMobs.enabled())
        {
            vanillaMobs.removeFromBiomes(); // after copySpawns and Decimation's own spawn setup
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        net.decimation.fixes.WreckHardness.apply();   // military wrecks broke with one punch (bug.md)
        net.decimation.fixes.PropHardness.apply();    // every other prop with hardness 0 (bug.md)
        Map<Integer, Sub> subs = buildSubstitutions();
        StructureGenerator generator = new StructureGenerator(schematics, subs, roadBlocks(),
                                   Block.getBlockFromName("deci:BlockRoad"),
                                   city = new CityDistrict(cityProps()),
                                   new LargeSites(largeSchematics, subs));
        generator.setCars(resolve("deci:BlockWreckage1", "deci:BlockWreckage2", "deci:BlockWreckage3",
                                  "deci:BlockWreckage4", "deci:BlockWreckage5", "deci:BlockTruckWreckage1"));
        generator.setStreetProps(one("deci:BlockStreetLight"), one("deci:BlockStreetBench"),
                                 one("deci:BlockStreetBin"), resolve("deci:BlockTrashBag1", "deci:BlockTrashBag2"),
                                 one("deci:BlockRoad_CenterLine"));
        net.decimation.worldgen.city.LcContent.init(new File(cpw.mods.fml.common.Loader.instance().getConfigDir(),
                                                             "decimation_worldgen"));
        net.decimation.worldgen.city.LcCity lcCity = new net.decimation.worldgen.city.LcCity(
            Block.getBlockFromName("deci:BlockRoad"));
        net.decimation.worldgen.city.StreetProps sp = new net.decimation.worldgen.city.StreetProps();
        sp.lamp = one("deci:BlockStreetLight");
        sp.bench = one("deci:BlockStreetBench");
        sp.bin = one("deci:BlockStreetBin");
        sp.centreLine = one("deci:BlockRoad_CenterLine");
        sp.trashBags = resolve("deci:BlockTrashBag1", "deci:BlockTrashBag2");
        sp.cars = resolve("deci:BlockWreckage1", "deci:BlockWreckage2", "deci:BlockWreckage3",
                          "deci:BlockWreckage4", "deci:BlockWreckage5");
        lcCity.setStreetProps(sp);
        generator.setLcCity(lcCity);
        GameRegistry.registerWorldGenerator(generator, 100);
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
        // place names (city districts, military bases) through Decimation's turf banner (docs/zones.md)
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new PlaceNames());
        FMLLog.info("[%s] place names registered", MODID);
        // Decimation's zone rules in singleplayer, safezones protecting everyone friendly (docs/zones.md)
        zoneRules = new net.decimation.fixes.ZoneRules();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(zoneRules);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(zoneRules);
        // safezone sites: their residents, kept by a server tick (docs/safezone.md)
        SafezoneSites sites = new SafezoneSites();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(sites);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(sites);
        // garrisons of the hardest places (wasteland bunkers): NPCs on first visit, military infected spawners
        Garrisons garrisons = new Garrisons();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(garrisons);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(garrisons);
        FMLLog.info("[%s] zone store, zone spawn fix and zone rules registered", MODID);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.VehicleHitHandler());
        FMLLog.info("[%s] vehicle hit fix registered", MODID);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.ArmorGunfireHandler());
        FMLLog.info("[%s] armor vs NPC gunfire fix registered", MODID);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(npcLoadouts);
        // NPC shots fly as traced shots (tools/patches/PatchTracer.java v2 asks this hook)
        net.decimation.fixes.Deci.setNpcShotHook(new net.decimation.fixes.NpcShots(npcLoadouts));
        if (militarySpawner.enabled())
        {
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(militarySpawner);
        }
        FMLLog.info("[%s] NPC tiers and military spawner registered", MODID);
        if (vanillaMobs.enabled())
        {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(vanillaMobs);
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(infectedVariants);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.HumanityKillHandler());
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(
            new net.decimation.fixes.SupplyDropScheduler());
        FMLLog.info("[%s] humanity and supply drop fixes registered", MODID);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
            new net.decimation.fixes.MultiblockRepairHandler());
        FMLLog.info("[%s] multiblock prop repair registered", MODID);

        if (System.getProperty(DevPregen.PROPERTY) != null || System.getProperty("deciworldgen.gatescan") != null
            || System.getProperty("deciworldgen.pregenwalk") != null)
        {
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new DevPregen());
            FMLLog.info("[%s] PREGEN enabled", MODID);
        }
        // -Ptrackdebug: which entity joins a world twice (bug.md "Entity is already tracked"; devtest/TrackDebug)
        if (Boolean.getBoolean(net.decimation.worldgen.devtest.TrackDebug.PROPERTY))
        {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.decimation.worldgen.devtest.TrackDebug());
            FMLLog.info("[%s] TRACKDEBUG enabled", MODID);
        }

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

    /** Decimation crates and furniture for procedural buildings, by registry name. */
    private Building.Props cityProps()
    {
        Building.Props p = new Building.Props();
        p.crates = resolve("deci:BlockWoodCrate", "deci:BlockMedicalCrate", "deci:BlockPoliceCrate",
                           "deci:BlockAmmoCrate");
        p.furniture = resolve("deci:BlockWoodTable", "deci:BlockChair", "deci:BlockOfficeChair",
                              "deci:BlockMetalShelf");
        p.table = one("deci:BlockWoodTable");
        p.chair = one("deci:BlockChair");
        p.officeChair = one("deci:BlockOfficeChair");
        p.metalTable = one("deci:BlockMetalTable");
        p.shelf = one("deci:BlockMetalShelf");
        p.cabinet = one("deci:BlockWeaponCabinet");
        p.cooking = one("deci:BlockCookingStation");
        p.washer = one("deci:BlockWashingMachine");
        p.trashcan = one("deci:BlockTrashcan");
        p.vending = one("deci:BlockVendingMachine_1");
        p.mailbox = one("deci:BlockMailbox");
        p.cardboard = one("deci:BlockCardboardBoxes1");
        p.woodCrate = one("deci:BlockWoodCrate");
        p.medicalCrate = one("deci:BlockMedicalCrate");
        p.policeCrate = one("deci:BlockPoliceCrate");
        p.ammoCrate = one("deci:BlockAmmoCrate");
        p.road = one("deci:BlockRoad");
        p.unitDoors = resolve("deci:Door_Office_1", "deci:Door_Blue_3", "deci:Door_Green_3",
                              "deci:Door_Orange_3", "deci:Door_Blue_1");
        p.officeDoor = one("deci:Door_Office_1");
        p.metalDoor = one("deci:Door_Metal_3");
        p.trashBags = resolve("deci:BlockTrashBag1", "deci:BlockTrashBag2");
        // furniture sets: built-ins copied to config/decimation_worldgen/sets/ once, then read from there
        net.decimation.worldgen.sets.FurnitureSets.init(new File(cpw.mods.fml.common.Loader.instance().getConfigDir(),
                                                                  "decimation_worldgen"));
        // interior surfaces and fixtures (docs/interior_spec.md section 2)
        for (Object o : cpw.mods.fml.common.registry.GameData.getBlockRegistry().getKeys())
        {
            String n = (String) o;
            if (n.startsWith("deci:Block"))
            {
                p.named.put(n.substring(5), Block.getBlockFromName(n));
            }
        }
        p.cars = resolve("deci:BlockWreckage1", "deci:BlockWreckage2", "deci:BlockWreckage3",
                         "deci:BlockWreckage4", "deci:BlockWreckage5");
        return p;
    }

    private static Block one(String name)
    {
        Block b = Block.getBlockFromName(name);
        if (b == null)
        {
            FMLLog.info("[%s] city prop '%s' not found", MODID, name);
        }
        return b;
    }

    private static Block[] resolve(String... names)
    {
        List<Block> out = new ArrayList<Block>();
        for (String n : names)
        {
            Block b = Block.getBlockFromName(n);
            if (b != null)
            {
                out.add(b);
            }
        }
        return out.toArray(new Block[0]);
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
