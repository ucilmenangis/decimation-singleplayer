# Graph Report - .  (2026-10-07)

## Corpus Check
- 10 files · ~61,195 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 796 nodes · 1737 edges · 52 communities (41 shown, 11 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 274 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- StructureGenerator: StructureGenerator
- Building: Building
- CLAUDE: decimation-singleplayer README (public repo overview)
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- SchematicPlan: SchematicPlan
- DevAutoTest: DevAutoTest
- building_design: Procedural building design doc (city blocks, Building v2)
- worldgen: World generation doc (deciworldgen)
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- Test Schematic Builder
- new_feature: Feature tracker (new_feature.md)
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- architecture: ServerProxy (deci.a.e, dedicated only)
- bug: Bug tracker (bug.md)
- DeciBiome: DeciBiome
- new_feature: Current state and pending decisions
- Name Mapping Applier
- DeciGenLayer: DeciGenLayer
- architecture: Obfuscation map (package to meaning)
- CLAUDE: tools/build.py real javac pipeline
- BiomeMap: BiomeMap
- ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)
- StructureData: StructureData
- SealedCaves: SealedCaves
- architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)
- documentation: Feature tracker (new_feature.md)
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- terrain: Decimation world type doc (terrain, 0.14.0)
- graph_update: graph_update.py
- Heuristic Auto Namer
- architecture: LootInteractHandler (deci.aK.k)
- DecimationBiomes: DecimationBiomes
- Rotation: Rotation
- architecture: TurfManager (server.turf.a)
- documentation: Loot system: pool definition, cooldown registry, interact handler
- DeadTree: DeadTree
- DecimationWorldType: DecimationWorldType
- Weather Type Id Bug
- ?: ServerTickEvent
- ?: EntityPlayer
- ?: ServerTickEvent
- ?: Plan
- ?: a
- ?: Override
- ?: net.decimation.worldgen.StructureGenerator.Sub
- ?: Schematic
- ?: Sub

## God Nodes (most connected - your core abstractions)
1. `Building` - 58 edges
2. `StructureGenerator` - 44 edges
3. `World generation doc (deciworldgen)` - 27 edges
4. `DevAutoTest` - 26 edges
5. `Bug tracker (bug.md)` - 25 edges
6. `Current state and pending decisions` - 25 edges
7. `Procedural building design doc (city blocks, Building v2)` - 23 edges
8. `DeciBiome` - 21 edges
9. `SchematicPlan` - 20 edges
10. `Feature tracker (new_feature.md)` - 20 edges

## Surprising Connections (you probably didn't know these)
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` --semantically_similar_to--> `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS] [semantically similar]
  docs/terrain.md → CLAUDE.md
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `VehicleHitHandler (v0.8.1)` --references--> `VehicleHitHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/VehicleHitHandler.java
- `Bug: zones never active in singleplayer (partial fix)` --references--> `ZoneSpawnHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java
- `Small schematics (max 24x24, one pass, per cell sector chance)` --references--> `Rotation`  [INFERRED]
  docs/worldgen.md → dev/src/main/java/net/decimation/worldgen/Rotation.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_building_building, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_building_building_grade, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Whole multiblock generation (v0.16.0): slice writer and small placer call complete after writing, Decimation helper forms the box, multiscan checks** — dev_src_main_java_net_decimation_worldgen_slices_slices_write, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_place, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler_complete, docs_worldgen_form_multiblock_helper, docs_worldgen_complete_after_slice_rule, tools_multiscan [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_carmeta, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Decimation world type biome pipeline (TerrainEvents swaps GenLayers, DeciGenLayer reads BiomeMap, BiomeMap uses shared Sectors, DecimationBiomes defines the biomes)** — dev_src_main_java_net_decimation_worldgen_terrain_terrainevents_terrainevents, dev_src_main_java_net_decimation_worldgen_terrain_decigenlayer_decigenlayer, dev_src_main_java_net_decimation_worldgen_terrain_biomemap_biomemap, dev_src_main_java_net_decimation_worldgen_sectors_sectors, dev_src_main_java_net_decimation_worldgen_terrain_decimationbiomes_decimationbiomes, dev_src_main_java_net_decimation_worldgen_terrain_decimationworldtype_decimationworldtype [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]

## Communities (52 total, 11 thin omitted)

### Community 0 - "StructureGenerator: StructureGenerator"
Cohesion: 0.07
Nodes (24): a, Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), CityDistrict, v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), cpw.mods.fml.common.IWorldGenerator, Graded, Schematic, LargeSites (+16 more)

### Community 1 - "Building: Building"
Cohesion: 0.09
Nodes (9): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Building, a, Props, Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Office floor layout (core, open plan desk rows at windows, meeting rooms, reception, storage, break room), Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Foundation down to the ground (max 12, Plan.foundation): stone brick plinth for buildings, dirt for schematics (+1 more)

### Community 2 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.06
Nodes (45): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Case sensitive volume extraction (+37 more)

### Community 3 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.07
Nodes (26): BottlecapHandler (deciworldgen), cpw.mods.fml.common.eventhandler.SubscribeEvent, BottlecapHandler, HumanityKillHandler, VehicleHitHandler, ZoneSpawnHandler, DevPregen, ServerTickEvent (+18 more)

### Community 4 - "SchematicPlan: SchematicPlan"
Cohesion: 0.07
Nodes (17): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Schematic, a, Schematic (+9 more)

### Community 5 - "DevAutoTest: DevAutoTest"
Cohesion: 0.10
Nodes (17): EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, ClientTickEvent, SupplyDropScheduler (+9 more)

### Community 6 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.12
Nodes (36): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), CityDistrict, Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), BlockProp facing transform from deobf PropRenderer (rotate 180 about x, metadata % 4 * 90 about y, then extra rotation; toward the road: road west 4, east 2, north 5, south 3) (+28 more)

### Community 7 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.08
Nodes (21): Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), a, Plan, Schematic, Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), Rule: complete multiblocks AFTER the whole slice is written; column multiblocks (1x1 footprint) with air above get full height, others become their own master, MultiblockHelper.formMultiblock (deci.W.e.a(world, x, y, z, block, meta): same block in every cell of the size box, each part master set) (+13 more)

### Community 8 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.10
Nodes (26): Bug: structures built on ocean floor, Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Seed 1 biome share 800x800 around spawn (city 27%, dead wild 26%, suburbs 15%, military 9%, overgrown 21%, river 1%), Terrain testing (servertest type=decimation pregen, worldmap.py biome map, autotest on the Decimation world type, -Ddeciworldgen.autotest.type=default for vanilla) (+18 more)

### Community 9 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 10 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.17
Nodes (24): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+16 more)

### Community 11 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.17
Nodes (18): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), World, MultiblockRepairHandler (+10 more)

### Community 12 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.14
Nodes (20): Bug: zones never active in singleplayer (partial fix), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), ChatHandler (deci.aK.n, radio chat), DeathStatsHandler (deci.aK.h), DecimationMod (deci.a.b, @Mod entry), EntitySpawnZoneHandler (deci.aK.d), LootCooldownRegistry (deci.aB.e) (+12 more)

### Community 13 - "bug: Bug tracker (bug.md)"
Cohesion: 0.18
Nodes (16): Bug: large ammo crate NPE (dead field avk), Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: CustomSkinLoader coremod crash, Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: no supply drops in singleplayer (fixed v0.9.0), Prism launched from the shell, read fml-client-latest.log (+8 more)

### Community 14 - "DeciBiome: DeciBiome"
Cohesion: 0.19
Nodes (4): cpw.mods.fml.relauncher.SideOnly, DeciBiome, Override, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 15 - "new_feature: Current state and pending decisions"
Cohesion: 0.21
Nodes (15): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain stays vanilla ChunkProviderGenerate, only the biome map is replaced, Decimation world type feature (user chose A over reskinning vanilla / RTG, done v0.14.0, seen in game; open: fog colour, rubble and ash decoration), deciworldgen second @Mod (net.decimation.worldgen), dist/ separate jars (deciworldgen-0.7.0.jar + Decimation.jar), Dropped companion mods (Ruins, ezWastelands, GeneratorMods) (+7 more)

### Community 16 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 17 - "DeciGenLayer: DeciGenLayer"
Cohesion: 0.17
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 18 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.22
Nodes (14): Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, IntRange (deci.aB.a, off by one rolls), LootPool (deci.aB.c), LootTable (deci.aD.l, hardcoded pools l$1..l$12), LootTrackedItem (deci.ao.c) (+6 more)

### Community 19 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 20 - "BiomeMap: BiomeMap"
Cohesion: 0.26
Nodes (3): Sectors, BiomeMap, NoiseGeneratorSimplex

### Community 21 - "ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.27
Nodes (8): Bug: armor buff ignores NPC gunfire (fixed), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, ArmorGunfireHandler, net.minecraft.entity.Entity, net.minecraft.entity.player.EntityPlayer, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 22 - "StructureData: StructureData"
Cohesion: 0.24
Nodes (5): Override, StructureData, NBTTagCompound, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 23 - "SealedCaves: SealedCaves"
Cohesion: 0.26
Nodes (7): Caves, Override, Ravines, SealedCaves, net.minecraft.world.biome.BiomeGenBase, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 24 - "architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)"
Cohesion: 0.18
Nodes (11): DamageSource split: gunDeci (player) vs human/turret (NPC), Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), TraderSpawnManager (server.traders.a), ItemArmorJuggernaut (juggernautHelm/Vest/Pants/Boots) (+3 more)

### Community 25 - "documentation: Feature tracker (new_feature.md)"
Cohesion: 0.24
Nodes (11): DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a), 35% armor damage reduction buff, Intro screen skip (deci.i.d.iH, deci.i.c.io flags), ItemArmorDeci.damageMultiplier, Loot fix documentation (documentation.md), 50% ranged weapon damage nerf, Buff armor damage reduction 35% (damageMultiplier x0.65) (+3 more)

### Community 26 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.22
Nodes (10): CFR decompiler, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief (+2 more)

### Community 27 - "terrain: Decimation world type doc (terrain, 0.14.0)"
Cohesion: 0.20
Nodes (10): BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Lake rules (none in city / military, 1 in 4 in other dead biomes, vanilla rate in overgrown, no surface lava pools), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Ravines cut 40 block trenches through flat cities; SealedCaves (InitMapGenEvent) digs nothing above y 50 under city and military biomes, Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors), Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit, Spawn search: suburb, wasteland, overgrown plains and forest added to WorldChunkManager.allowedBiomes, Decimation world type doc (terrain, 0.14.0) (+2 more)

### Community 28 - "graph_update: graph_update.py"
Cohesion: 0.36
Nodes (8): Graph update workflow (graph_update.py prepare/finish, one agent per chunk), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph), finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 29 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 30 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.32
Nodes (8): Bug: loot never worked in singleplayer, ClientProxy (deci.a.c), DeciConstants (deci.Q.c, GUI ids), LootCloseCallback (deci.aB.b), LootInteractHandler (deci.aK.k), PacketLootInventory (deci.aE.a$R), SupplyDropBlock (deci.V.a), TickScheduler (net.decimation.mod.common.utils.b)

### Community 31 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.32
Nodes (3): DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River)

### Community 33 - "architecture: TurfManager (server.turf.a)"
Cohesion: 0.40
Nodes (6): Bug: ClassCastException deci.a.c to deci.a.e, Block break/place protection handlers (deci.aK.a, b), ClanManagerV1 (server.clans.a), deci.a.b.d() hard cast to ServerProxy, TurfManager (server.turf.a), Clans and turf system

### Community 34 - "documentation: Loot system: pool definition, cooldown registry, interact handler"
Cohesion: 0.33
Nodes (6): Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), VehicleEntity (deci.ad.e) and parts, Known non-bugs (trapped chest, truck wreckage, no trunk), Loot system: pool definition, cooldown registry, interact handler, Vanilla chest GUI suppressed for loot GUI

### Community 35 - "DeadTree: DeadTree"
Cohesion: 0.40
Nodes (3): DeadTree, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree

### Community 36 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

## Ambiguous Edges - Review These
- `addBox passes Y as Z origin (use addShape)` → `Genuinely new model recipe (needs Techne)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` → `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `FML IWorldGenerator per chunk hook` → `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **35 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+30 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `addBox passes Y as Z origin (use addShape)` and `Genuinely new model recipe (needs Techne)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` and `Recurring root cause: integrated server reports side CLIENT`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `FML IWorldGenerator per chunk hook` and `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Current state and pending decisions` connect `new_feature: Current state and pending decisions` to `StructureGenerator: StructureGenerator`, `Building: Building`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `Rotation: Rotation`, `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`, `Test Schematic Builder`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `architecture: ServerProxy (deci.a.e, dedicated only)`, `ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)`?**
  _High betweenness centrality (0.151) - this node is a cross-community bridge._
- **Why does `World generation doc (deciworldgen)` connect `worldgen: World generation doc (deciworldgen)` to `StructureGenerator: StructureGenerator`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `SchematicPlan: SchematicPlan`, `building_design: Procedural building design doc (city blocks, Building v2)`, `worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `new_feature: Current state and pending decisions`, `terrain: Decimation world type doc (terrain, 0.14.0)`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._