# Graph Report - .  (2026-10-09)

## Corpus Check
- 8 files · ~159,204 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1906 nodes · 4097 edges · 155 communities (99 shown, 56 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 496 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- NpcTest: net.minecraft.world.World
- lctranslate: lctranslate.py
- Highways: Highways
- SchematicPlan: SchematicPlan
- NpcLoadouts: NpcLoadouts
- Building: Building
- DevTestUtil: net.minecraft.client.Minecraft
- Test Schematic Builder
- Deci: Deci
- new_feature: Current state and pending decisions
- building_design: Procedural building design doc (city blocks, Building v2)
- new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military
- worldgen: World generation doc (deciworldgen)
- CLAUDE: decimation-singleplayer README (public repo overview)
- bug: Bug tracker (bug.md)
- roadmap: Plan: enemy military, juggernaut with machine guns / Barrett, stronger bandits, in military areas
- StreetPlan: StreetPlan
- ScopeZoom: ScopeZoom
- NpcTest: NpcTest
- architecture: Zones on generated structures (auto zone tagging)
- DecimationWorldGen: DecimationWorldGen
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- StoreyPlan: StoreyPlan
- EdgePlan: EdgePlan
- Palettes: com.google.gson.JsonObject
- CameraViews: CameraViews
- Deci: Player
- ?: Sectors.java
- create_weapons: Creating new weapons guide
- Deci: Entity
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- NpcEgg: NpcEgg
- Capture: Capture
- ServerChecks: ServerChecks
- ZoneStore: ZoneStore
- floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- NpcKind: ShotTest
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- architecture: Obfuscation map (package to meaning)
- WorldGenCommand: WorldGenCommand
- NpcShots: .apply()
- Surfaces: net.minecraft.block.Block
- BuildingPlan: BuildingPlan
- LcCity: LcCity
- LcContent: LcContent
- deceasedcraft_buildings: Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data)
- architecture: LootInteractHandler (deci.aK.k)
- Deci: .onServerTick()
- building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status
- LotPlan: LotPlan
- CLAUDE: Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags)
- lcstudy: dcinventory.py
- bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)
- CLAUDE: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- ScopeZoom: ScopeZoom
- Name Mapping Applier
- DeciBiome: DeciBiome
- worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)
- worldcheck: worldcheck.py
- furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable
- MilitarySpawner: MilitarySpawner
- Shell: Shell
- ?: DeciGenLayer
- hwmap: hwmap.py
- CLAUDE: tools/build.py real javac pipeline
- DevTestResults: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)
- Ruins: .unit()
- Facing: .setMeta()
- anvil118: anvil118.py
- Deci: .zoneType()
- city_engine: .decor()
- AssetDir: AssetDir
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- SealedCaves: SealedCaves
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- bug: Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots)
- Condition: com.google.gson.JsonObject
- Deci: .complete()
- BiomeMap: BiomeMap
- interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)
- DecimationBiomes: DecimationBiomes
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- Heuristic Auto Namer
- Furnisher: Furnisher
- TracerTest: TracerTest
- edgescan: edgescan.py
- bug: Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0)
- gradescan: gradescan.py
- DevAutoTest: DevAutoTest
- Interior: .cell()
- Graded: Graded
- Rotation: Rotation
- architecture: TurfManager (server.turf.a)
- gun_model_spec: Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)
- interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- DeadTree: DeadTree
- DecimationWorldType: DecimationWorldType
- multiscan: multiscan.py
- PatchSwing: PatchSwing.java
- PatchFactions: PatchFactions.java
- CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side
- new_feature: Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours)
- Deci: .gunDamage()
- asset_hashes: asset_hashes.py
- devtest: devtest.py
- footprint: footprint.py
- ScopeZoom: .onOverlay()
- EdgePlan: .zone()
- Weather Type Id Bug
- ?: ObjectZone
- ?: ObjectZoneList
- ?: SuppressWarnings
- ?: TileEntity
- ?: ServerTickEvent
- ?: Override
- ?: SuppressWarnings
- ?: Override
- ?: JsonObject
- ?: a
- ?: a
- ?: Entry
- ?: FurnitureSet
- ?: a
- ?: CityDistrict
- ?: Schematic
- ?: EntityLiving
- ?: EntityPlayer
- ?: Entry
- ?: ItemStack
- ?: ServerTickEvent
- ?: Plan
- ?: a
- ?: a
- ?: FurnitureSet
- ?: a
- ?: Override
- ?: World
- ?: a
- interior_spec: propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N
- shots_index: flats_v0.20 / v0.21 interiors and sets_v0.21 kitchens
- ?: Entity
- ?: EntityPlayerMP
- ?: Load
- ?: net.decimation.fixes.MilitarySpawner
- ?: net.decimation.worldgen.StructureGenerator
- ?: net.minecraftforge.client.event.FOVUpdateEvent
- ?: Shape

## God Nodes (most connected - your core abstractions)
1. `Deci` - 72 edges
2. `StructureGenerator` - 51 edges
3. `Building` - 50 edges
4. `NpcLoadouts` - 41 edges
5. `LcCity` - 36 edges
6. `Bug tracker (bug.md)` - 31 edges
7. `DecimationWorldGen` - 30 edges
8. `Current state and pending decisions` - 29 edges
9. `World generation doc (deciworldgen)` - 27 edges
10. `ScopeZoom` - 27 edges

## Surprising Connections (you probably didn't know these)
- `Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside)` --semantically_similar_to--> `survivor_camp()`  [INFERRED] [semantically similar]
  docs/interior_spec.md → tools/make_test_schematics.py
- `Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections)` --references--> `DevAutoTest`  [INFERRED]
  docs/building_design.md → dev/src/main/java/net/decimation/worldgen/DevAutoTest.java
- `v0.28.1: EntityRenderer.cameraZoom zooms world and gun together; ScopeZoom draws the hand itself on RenderHandEvent (vanilla skips it while zoomed)` --implements--> `ScopeZoom`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ScopeZoom.java
- `v0.28.4: scopes from overlayFrom (default 4x) hide the gun and draw a black sniper overlay with the scope's reticle texture on the HUD` --implements--> `ScopeZoom`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ScopeZoom.java
- `config/deciworldgen_scope.cfg: zoom per sight (reddot 1.25, 2x, 4x, 8x, dragunov 4x), overlayFrom, sensitivity, pictureInPicture` --references--> `ScopeZoom`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/fixes/ScopeZoom.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling, tools_patches_patchscope, tools_patches_patchtracer, tools_patches_patchbackend [INFERRED 0.85]
- **Visual evidence memory (autotest screenshot modes, docs/shots, written shots index, prop catalogue)** — claude_visual_evidence_rule, claude_autotest_screenshot_modes, docs_shots_index_shots_index, docs_prop_catalogue_prop_catalogue_doc [INFERRED 0.85]
- **Dev test modes run in one launch by tools/devtest.py** — claude_devtest_workflow, tools_devtest, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest, dev_src_main_java_net_decimation_worldgen_devtest_devtestmode_devtestmode, dev_src_main_java_net_decimation_worldgen_devtest_devtestresults_devtestresults, dev_src_main_java_net_decimation_worldgen_devtest_serverchecks_serverchecks, dev_src_main_java_net_decimation_worldgen_devtest_cameraviews_cameraviews, dev_src_main_java_net_decimation_worldgen_devtest_scopetest_scopetest, dev_src_main_java_net_decimation_worldgen_devtest_tracertest_tracertest, dev_src_main_java_net_decimation_worldgen_devtest_propstest_propstest, dev_src_main_java_net_decimation_worldgen_devtest_cityfpstest_cityfpstest [EXTRACTED 1.00]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Upper storey reachability fix v0.16.1 (floorplan scan, unsupported ladder, collapse exemption, Building.ladder / collapsed)** — bug_upper_storeys_unreachable, bug_ladder_unsupported_wall_cell, bug_ladder_core_collapse_exemption, tools_floorplan [EXTRACTED 1.00]
- **Prop render performance: measure, cache line of sight, render distance, bake (open)** — bug_prop_fps_drop, bug_prop_fps_measurement, bug_los_cache, bug_prop_render_distance_by_size, bug_cityfps_measurement, bug_bake_props_chunk_mesh, tools_patches_patchpropculling_patchpropculling, claude_jfr_profiling, docs_roadmap_prop_drawing_cost [EXTRACTED 1.00]
- **Cheap scope v0.28.0..0.28.4 (gate, view zoom, projective glass, sniper overlay)** — feature_cheap_scope, bug_scope_pip_gate, bug_scope_camera_zoom, bug_scope_projective_glass, bug_scope_sniper_overlay, dev_src_main_java_net_decimation_fixes_scopezoom_scopezoom, tools_patches_patchscope_patchscope, dev_src_main_java_net_decimation_worldgen_devtest_scopetest_scopetest [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]
- **Interior spec layered build (structure, surfaces, room programs, story / decay, exterior, in work order)** — docs_interior_spec_principle_layer_order, docs_interior_spec_doors, docs_interior_spec_surfaces, docs_interior_spec_apartment_unit_program, docs_interior_spec_office_storey_programs, docs_interior_spec_shop_program, docs_interior_spec_story_decay_layer, docs_interior_spec_exterior, docs_interior_spec_order_of_work [EXTRACTED 1.00]
- **Step 4d.1 doors and low debris (spec, code, tracker, release note, audit shots)** — docs_interior_spec_step1_doors_low_debris, claude_v0_17_0_doors_low_debris, new_feature_step_4d1_doors_debris, docs_shots_index_audit_v0_17 [INFERRED 0.85]
- **Verification loop for every building change (floor plans, audit shots, scans, autotest, written findings)** — docs_interior_spec_verification, tools_floorplan, tools_wallscan, tools_gradescan, tools_multiscan, docs_building_audit_building_audit_doc, docs_shots_index_shots_index [EXTRACTED 1.00]
- **Building quality audit toolchain (floor plans with reachability, audit screenshots, view cells, indexed results)** — docs_building_audit_audit_method, tools_floorplan, docs_shots_index_audit_v0_16, docs_shots_index_plans_v0_16 [INFERRED 0.85]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Whole multiblock generation (v0.16.0): slice writer and small placer call complete after writing, Decimation helper forms the box, multiscan checks** — dev_src_main_java_net_decimation_worldgen_slices_slices_write, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_place, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler_complete, docs_worldgen_form_multiblock_helper, docs_worldgen_complete_after_slice_rule, tools_multiscan [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Decimation world type biome pipeline (TerrainEvents swaps GenLayers, DeciGenLayer reads BiomeMap, BiomeMap uses shared Sectors, DecimationBiomes defines the biomes)** — dev_src_main_java_net_decimation_worldgen_terrain_terrainevents_terrainevents, dev_src_main_java_net_decimation_worldgen_terrain_decigenlayer_decigenlayer, dev_src_main_java_net_decimation_worldgen_terrain_biomemap_biomemap, dev_src_main_java_net_decimation_worldgen_sectors_sectors, dev_src_main_java_net_decimation_worldgen_terrain_decimationbiomes_decimationbiomes, dev_src_main_java_net_decimation_worldgen_terrain_decimationworldtype_decimationworldtype [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]

## Communities (155 total, 56 thin omitted)

### Community 0 - "NpcTest: net.minecraft.world.World"
Cohesion: 0.08
Nodes (18): Refactor check on both city paths: worlddiff 0, legacy path by moving the lc pack aside, git stash -u, cpw.mods.fml.common.IWorldGenerator, Yard, Schematic, LargeSites, LegacyStreets, Plan, Slices (+10 more)

### Community 1 - "lctranslate: lctranslate.py"
Cohesion: 0.06
Nodes (50): Converted buildings used LED lamp blocks as floor (FIXED 9 Oktober 2026: 'light' in colour names like light_gray matched the lamp rule), Knowledge index: translation audit tools/lcaudit.py, Rule: new code adds an accessor to Deci, never calls deci.* directly (all ~60 obfuscated uses migrated 2026-10-09, seed 1 world 0 blocks differ), lcpack content: 290 converted buildings, stairs per district style (local only), Translation audit 9 Oktober 2026: dropped blocks and props by placement over 290 converted buildings; black sandstone is asphalt, laboratory panels, wallpaper, corundum, posts, shelves, seats mapped, Roadmap: code and tools, Recently done: converted building quality pass (pack rebuilt, LED floor fixed, about 30000 dropped blocks mapped), Split LcCity (1225 lines) and StructureGenerator (861 lines) into street, building, lot and edge plan files; old procedural city apart (+42 more)

### Community 2 - "Highways: Highways"
Cohesion: 0.10
Nodes (14): CLAUDE.md v0.29.0 note: highway polish, pregen to check far away L links, Highways, Override, StructureData, Highway chunk kinds: TUNNEL (median 6+ above deck), BRIDGE (water or ground 2+ below, stone brick pillars), OPEN; stored as hw_X_Z, Highway L links (v0.29.0): isolated city region joined to its nearest diagonal city (at most 2 regions) by an L of the region lines; seed 1: 10 links, city groups 29 -> 19, Highway polish v0.29.0: hedges on crossing parts facing open land, no tunnels in city edge bands, side ramps beside bridges cut land above the deck only, Highway side ramps beside OPEN chunks (deck to natural height over 3..8 blocks, shared LcCity.reshape) (+6 more)

### Community 3 - "SchematicPlan: SchematicPlan"
Cohesion: 0.06
Nodes (10): Plan, Schematic, SchematicPlan, ZoneKind, MILITARY, POLICE, SAFEZONE, City districts: wasteland next to military sectors, current beta weighted (+2 more)

### Community 4 - "NpcLoadouts: NpcLoadouts"
Cohesion: 0.12
Nodes (6): ItemStack, NpcKind, World, NpcLoadouts, Tier, LivingUpdateEvent

### Community 5 - "Building: Building"
Cohesion: 0.08
Nodes (11): Building, Props, CityDistrict, Furnisher, Interior, net.decimation.worldgen.Graded, Ruins, Shell (+3 more)

### Community 6 - "DevTestUtil: net.minecraft.client.Minecraft"
Cohesion: 0.10
Nodes (8): City street after the cache (-Pcityfps): props 5.5% of the client thread, chunk drawing 15%, chunk rebuild 12.7%; props no longer the bottleneck, CityFpsTest, DevTestMode, DevTestUtil, PropsTest, EntityPlayerMP, ScopeTest, net.minecraft.client.Minecraft

### Community 7 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 8 - "Deci: Deci"
Cohesion: 0.10
Nodes (5): Deci, net.minecraft.item.Item, net.minecraft.item.ItemStack, ObjectZoneList, Vector3f

### Community 9 - "new_feature: Current state and pending decisions"
Cohesion: 0.10
Nodes (28): dev/libs/Decimation-base.jar (patched jar minus our classes), dev/ RetroFuturaGradle workspace (GTNH ExampleMod1.7.10 template), MCP readable member names in dev workspace, Current state and pending decisions, v0.12.1: whole footprint floor height sampling and dirt fill under schematics, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), DecimationVoiceChat.jar required or menu shows Update instead of Play, Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate) (+20 more)

### Community 10 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.14
Nodes (28): Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Procedural building design doc (city blocks, Building v2), Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Floor counts (shop 1 to 2, apartment 2 to 9, office 3 to 20, about 15% towers), Lots and yards (2 free on the sides, 3 behind, 6 to 9 front yard; offices and shops car park with nose-in wrecks, apartments gravel path and lawn), 1 block margin ring outside walls for exterior vines (SKIP elsewhere) (+20 more)

### Community 11 - "new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military"
Cohesion: 0.08
Nodes (28): CLAUDE.md v0.30.1 note: spawn egg per NPC tier, CLAUDE.md v0.30.4: cooldown ticks, rocket tiers, config version 2, new tiers last, CLAUDE.md v0.30.0 note: NPC tiers, gun sync via data watcher slot 26, test mode npc, CLAUDE.md v0.31.0: juggernaut tier, Barrett armor piercing, Balance v0.31.0: juggernaut, Balance v0.30.4: hit cooldown 0.25 s, RPG NPCs, Balance: NPC tiers (v0.30.0), config deciworldgen_npc.cfg, Recently done v0.32.0: elite military, bursts, magazines (+20 more)

### Community 12 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.12
Nodes (23): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Schematic, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors), Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), Large schematics (up to 120x120, per site chance, placed inside the site) (+15 more)

### Community 13 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.10
Nodes (26): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CLAUDE.md project guide, Decimation.jar (obfuscated Forge 1.7.10 mod jar), Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), deobf/ readable reference tree (decompiled with readable names, read only), Graph update workflow (graph_update.py prepare/finish, one agent per chunk) (+18 more)

### Community 14 - "bug: Bug tracker (bug.md)"
Cohesion: 0.11
Nodes (26): Bug: building base height depends on chunk generation order (Slices.decideBase), Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: CustomSkinLoader coremod crash, Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: no supply drops in singleplayer (fixed v0.9.0), Bug: vehicles destroyed in one hit (fixed v0.8.1) (+18 more)

### Community 15 - "roadmap: Plan: enemy military, juggernaut with machine guns / Barrett, stronger bandits, in military areas"
Cohesion: 0.13
Nodes (25): NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer), Cause: PacketGunFireEffects carried only the shooter id; the client drew the tracer along getLook() (a mob's body facing), Tracer test (-Ptracer): original 52 tracers mean 133 degrees off the target, patched 42 tracers mean 1.0, max 2.0, NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), ItemRegistry (deci.aD.k) (+17 more)

### Community 16 - "StreetPlan: StreetPlan"
Cohesion: 0.11
Nodes (5): Shape, ZoneKind, StreetPlan, StreetProps, City engine files after the 2026-10-09 split: LcCity layout + StreetPlan, BuildingPlan, LotPlan, EdgePlan, StreetProps

### Community 17 - "ScopeZoom: ScopeZoom"
Cohesion: 0.15
Nodes (11): Configuration, ScopeZoom, Field, Method, net.minecraft.client.renderer.EntityRenderer, net.minecraft.util.ResourceLocation, net.minecraftforge.client.event.RenderHandEvent, net.minecraftforge.client.event.RenderWorldLastEvent (+3 more)

### Community 18 - "NpcTest: NpcTest"
Cohesion: 0.21
Nodes (6): Item, NpcLoadoutsAccess, NpcTest, net.decimation.fixes.NpcKind, net.minecraft.entity.EntityLiving, net.minecraft.entity.player.EntityPlayerMP

### Community 19 - "architecture: Zones on generated structures (auto zone tagging)"
Cohesion: 0.13
Nodes (24): Bug: zones never active in singleplayer (partial fix), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), ChatHandler (deci.aK.n, radio chat), DeathStatsHandler (deci.aK.h), DecimationMod (deci.a.b, @Mod entry), EntitySpawnZoneHandler (deci.aK.d), LootCooldownRegistry (deci.aB.e) (+16 more)

### Community 20 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.14
Nodes (13): CityDistrict, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, DecimationWorldGen, FMLPostInitializationEvent, FMLServerStartingEvent, MilitarySpawner (+5 more)

### Community 21 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.12
Nodes (11): cpw.mods.fml.common.eventhandler.SubscribeEvent, ArmorGunfireHandler, DevPregen, ServerTickEvent, EntityConstructing, EntityLivingBase, EntityPlayer, ItemStack (+3 more)

### Community 22 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.12
Nodes (24): Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), DeceasedCraft flats: 6 to 8 small rooms (3x4..5x5), density 0.35, Apartment unit rooms: living room (sofa of stairs facing FlatscreenTV), kitchen counter run (slabs, cauldron sink, ElectricBoxBin fridge, furnace oven, WashingMachine), bedroom (vanilla bed head to wall), tiled bathroom, studio flat, Door decay: 25 to 50% missing, a few left open (meta bit 4), one barricaded in the most decayed buildings, Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta) (+16 more)

### Community 23 - "StoreyPlan: StoreyPlan"
Cohesion: 0.19
Nodes (4): ApartmentPlanner, OfficePlanner, StoreyPlan, Worldgen code map: building package parts (Shell, StoreyPlan, planners, Furnisher, Surfaces, Interior, Ruins, Yard)

### Community 25 - "Palettes: com.google.gson.JsonObject"
Cohesion: 0.16
Nodes (5): Palettes, Style, Entry, FurnitureSet, FurnitureSets

### Community 26 - "CameraViews: CameraViews"
Cohesion: 0.19
Nodes (3): CameraViews, EntityPlayerMP, Entry

### Community 27 - "Deci: Player"
Cohesion: 0.15
Nodes (6): b, BottlecapHandler (deciworldgen), BottlecapHandler, Player, net.minecraftforge.event.entity.living.LivingDeathEvent, net.minecraftforge.event.entity.player.EntityItemPickupEvent

### Community 28 - "?: Sectors.java"
Cohesion: 0.30
Nodes (5): Building, net.decimation.mod.server.zones.ObjectZone, net.decimation.worldgen.building.Building, net.decimation.worldgen.ZoneKind, ObjectZone

### Community 29 - "create_weapons: Creating new weapons guide"
Cohesion: 0.20
Nodes (21): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+13 more)

### Community 30 - "Deci: Entity"
Cohesion: 0.12
Nodes (5): Entity, EntityLivingBase, NpcKind, net.minecraftforge.event.entity.player.AttackEntityEvent, SuppressWarnings

### Community 31 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.15
Nodes (20): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look, Principle: furniture against a wall or partner piece, never floating (except islands: desk clusters, aisles, tables) (+12 more)

### Community 32 - "NpcEgg: NpcEgg"
Cohesion: 0.20
Nodes (8): cpw.mods.fml.relauncher.SideOnly, ItemStack, NpcEgg, EntityLiving, net.minecraft.client.renderer.texture.IIconRegister, net.minecraft.creativetab.CreativeTabs, net.minecraft.util.IIcon, Override

### Community 33 - "Capture: Capture"
Cohesion: 0.21
Nodes (3): Capture, JsonArray, JsonObject

### Community 34 - "ServerChecks: ServerChecks"
Cohesion: 0.23
Nodes (3): EntityPlayer, EntityPlayerMP, ServerChecks

### Community 35 - "ZoneStore: ZoneStore"
Cohesion: 0.14
Nodes (11): Load, ObjectZone, ObjectZoneList, ServerTickEvent, ZoneStore, Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, Zone tagging (mil_ MILITARY, city_ and buildings POLICE, deciworldgen_zones.json), net.decimation.mod.server.zones.ObjectZoneList (+3 more)

### Community 36 - "floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.18
Nodes (18): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), Knowledge index entry: docs/roadmap.md holds EVERYTHING planned in one list, v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type, Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability) (+10 more)

### Community 37 - "NpcKind: ShotTest"
Cohesion: 0.15
Nodes (10): NpcKind, BANDIT, HAZMAT, SOLDIER, SOVIET, Entity, EntityLivingBase, ShotTest (+2 more)

### Community 38 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.22
Nodes (17): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), Load, MultiblockRepairHandler (+9 more)

### Community 39 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.18
Nodes (18): CFR decompiler, Obfuscation map (package to meaning), rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), CFR decompiled source on /Volumes/DeciDeobf/src, 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes (+10 more)

### Community 40 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.21
Nodes (9): Live loop: hotswap code, reload sets, rebuild in place, no restart per change, Override, WorldGenCommand, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), Live dev test mode [idea]: keep the dev game open and start test modes over a localhost port, net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender, jdb() (+1 more)

### Community 41 - "NpcShots: .apply()"
Cohesion: 0.20
Nodes (10): CLAUDE.md v0.32.0 note and the shootAt test trap, NpcKind, NpcShots, Balance v0.32.0: elite, bursts, reloads, NPC auto fire v0.32.0: bursts at the gun's rate, spread grows per shot (recoilSpread 0.35), pause after a burst, NPC magazines v0.32.0: fire the gun's magazine (M16 / AK 30, PKM 250), then reload 4 s (reloadTicks 80), net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase (+2 more)

### Community 43 - "BuildingPlan: BuildingPlan"
Cohesion: 0.14
Nodes (4): BuildingPlan, Building, Shape, ZoneKind

### Community 44 - "LcCity: LcCity"
Cohesion: 0.27
Nodes (3): Building, LcCity, Highways

### Community 45 - "LcContent: LcContent"
Cohesion: 0.14
Nodes (4): Building, LcContent, Shape, net.decimation.worldgen.Schematic

### Community 46 - "deceasedcraft_buildings: Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data)"
Cohesion: 0.14
Nodes (18): Skipped giant buildings: casino 276 high, oasis condo top above 250, laboratory 90 deep cellars, City engine open items: Lost Cities bridges and rail unused, giant buildings too tall, rotation only data variants, chests became wood crates, City superblocks: 2x2 cells, 7x7 building chunks, landmark towers, DeceasedCraft content catalogue: 79 Lost Cities buildings, city parts, apocalypsenow structures, disabled vanilla structures, Lost Cities to 1.7.10 conversion (lc2schem, lctranslate, paste command), DeceasedCraft interiors fully authored per storey, no procedural rooms, DeceasedCraft 79 building types and 5 district city styles, Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data) (+10 more)

### Community 47 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.15
Nodes (17): Bug: large ammo crate NPE (dead field avk), Bug: loot never worked in singleplayer, ClientProxy (deci.a.c), DeciConstants (deci.Q.c, GUI ids), IntRange (deci.aB.a, off by one rolls), LootCloseCallback (deci.aB.b), LootInteractHandler (deci.aK.k), LootPool (deci.aB.c) (+9 more)

### Community 48 - "Deci: .onServerTick()"
Cohesion: 0.16
Nodes (5): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Block, ServerTickEvent, SupplyDropScheduler, net.minecraft.server.MinecraftServer

### Community 49 - "building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status"
Cohesion: 0.21
Nodes (16): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), BlockProp facing transform from deobf PropRenderer (rotate 180 about x, metadata % 4 * 90 about y, then extra rotation; toward the road: road west 4, east 2, north 5, south 3), Car wreck model axis: long axis along x at 0 degrees (confirmed in game 2026-10-07), Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections), Prop facing rule (PropRenderer rotation = metadata % 4 * 90; north south street 5/3, east west 4/2) (+8 more)

### Community 50 - "LotPlan: LotPlan"
Cohesion: 0.12
Nodes (4): Shape, ZoneKind, LotPlan, FixedBase

### Community 51 - "CLAUDE: Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags)"
Cohesion: 0.19
Nodes (14): Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), street_v0.15 autotest street views (centre line along the street, street light arm over the road confirms the facing table, levelled cross-section, terraced lot), worldmap_seed1_v0.14.png: top down map of seed 1, Decimation world type (city sectors, military, suburbs, overgrown forest, rivers stopping at city edges) (+6 more)

### Community 52 - "lcstudy: dcinventory.py"
Cohesion: 0.23
Nodes (11): category(), districts(), main(), building name -> {city style: weight share}, structure_summary(), category(), main(), Pack (+3 more)

### Community 53 - "bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)"
Cohesion: 0.18
Nodes (13): Baking static props into chunk meshes (prop textures into the block atlas, both model formats to quads): full fix for open views, not started, Launch waited 5 s for the dead Decimation backend (kryonet hardcoded 5000 ms); PatchBackend, Prop dense FPS drop: line of sight ray casts 76% of prop rendering, cached in v0.28.6; model drawing remains, Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains), Prop fps measured (-Pprops): 225 props in view 12..14 fps vs empty 28..29; 76% of PropRenderer time in canSeeTileEntity ray casts, PatchPropCulling step 4: render distance by prop size (deciworldgen_props.cfg, small / medium / large; default 64 = vanilla), Obfuscated Decimation names in our code go through fixes/Deci, -Pjfr Java Flight Recorder CPU profile (run/client/profile.jfr, read with jfr print --json; client thread is main in dev) (+5 more)

### Community 54 - "CLAUDE: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.19
Nodes (11): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, PatchPropCulling step 3 (v0.28.6): line of sight answers cached until the player or entity moves 0.3 blocks (props 1.0..1.3 s, entities 0.15..0.18 s); prop renderer share 21% -> 9%, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Hand written Forge/Minecraft stub classes, Javassist bytecode patcher, Javassist cannot compile Java 8 lambdas, Patch from ORIGINAL classes only after checking the target class is identical in the patched jar (+3 more)

### Community 55 - "ScopeZoom: ScopeZoom"
Cohesion: 0.17
Nodes (13): v0.28.1: EntityRenderer.cameraZoom zooms world and gun together; ScopeZoom draws the hand itself on RenderHandEvent (vanilla skips it while zoomed), Scope FPS drop: picture in picture second world render, fixed v0.28.0 with view zoom, Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4), Scope fps measured (4x on ak74): picture in picture aiming 37..44 vs view zoom aiming 105..132, PatchScope: renderScopeView gated behind system property decimation.scope.pip (old scope via pictureInPicture=true in config/deciworldgen_scope.cfg), v0.28.2 / 0.28.3: projective see-through glass (frame copied, mapped by screen position), sight learned per gun + scope + window aspect, copy limited to the glass box, v0.28.4: scopes from overlayFrom (default 4x) hide the gun and draw a black sniper overlay with the scope's reticle texture on the HUD, Javassist snippets must compile against Java 8 signatures (FloatBuffer.flip() through java.nio.Buffer, else NoSuchMethodError in game) (+5 more)

### Community 56 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 57 - "DeciBiome: DeciBiome"
Cohesion: 0.18
Nodes (3): DeciBiome, Override, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 58 - "worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)"
Cohesion: 0.14
Nodes (15): Builders: separate ceiling tiles with light panel grid and vents, Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161), Builders: decay as dirt/leaves/water/cracked glass on intact shells, Builders: furniture in rows and islands (waiting rows, cubicles, shelf aisles), Autotest -Pstudy camera mode for reference maps, Builders: two tone WallOffice walls (dado bottom + top), Audit shots v0.23: 6 high offices and shops, Worldgen architecture v3 draft (layers, assets, size classes, capture tool) (+7 more)

### Community 59 - "worldcheck: worldcheck.py"
Cohesion: 0.21
Nodes (10): Bug: structures built on ocean floor, Underwater placement fix (v0.7.0), tools/worldcheck.py region file inspection, main(), _meta(), Block metadata (0..15) at a position, None if the chunk is missing., Block name -> numeric id, from the FML id map in level.dat., read_nbt() (+2 more)

### Community 60 - "furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.14
Nodes (14): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, Furniture sets doc: data driven JSON furniture groups, user editable, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, known.txt: unedited old built-in copies are updated (tools/asset_hashes.py), Named palettes and weighted styles for sets (base / style keys), Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window, Set preview mode -Psets: each set in a plaster bay, photographed, Seed 1 placement: kitchen/bath/lobby/closet 100%, bed 84%, living 80%, dining 68% (+6 more)

### Community 61 - "MilitarySpawner: MilitarySpawner"
Cohesion: 0.21
Nodes (4): MilitarySpawner, net.minecraft.entity.player.EntityPlayer, net.minecraft.world.WorldServer, ServerTickEvent

### Community 63 - "?: DeciGenLayer"
Cohesion: 0.18
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 64 - "hwmap: hwmap.py"
Cohesion: 0.35
Nodes (13): at(), city(), clear(), hl(), line(), link(), onL(), onlink() (+5 more)

### Community 65 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 66 - "DevTestResults: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)"
Cohesion: 0.23
Nodes (5): Dev test modes: checks (fresh seed 1 world: zones, vehicle, humanity, prop box, bottlecaps, armor, helmet, supply drop), views (-Paudit / -Pgallery / -Pfootprint / -Pstudy / -Pflats / -Psets), scope, tracer, props, cityfps, run/client/devtest/results.txt: one line per value, PASS / FAIL with the expectation, exit code 1 on a FAIL, tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09), New dev test: DevTestMode subclass, register its name in DevAutoTest.mode(), record with DevTestResults.value / check, screenshots with DevTestUtil.screenshot, DevTestResults

### Community 68 - "Facing: .setMeta()"
Cohesion: 0.26
Nodes (3): Facing, Entry, ShopPlanner

### Community 69 - "anvil118: anvil118.py"
Cohesion: 0.24
Nodes (10): chunk_biomes(), chunk_blocks(), chunks(), main(), Palette, {section Y: biomes[4,4,4] (y, z, x) global ids}., Entries packed without spanning longs (1.16+)., Global name -> id for a whole survey. (+2 more)

### Community 70 - "Deci: .zoneType()"
Cohesion: 0.24
Nodes (4): a, EntityLiving, World, ZoneKind

### Community 71 - "city_engine: .decor()"
Cohesion: 0.20
Nodes (8): v0.25.0 highways, v0.26.0 parks / street scenes / fronts, v0.27.0 district Lost Cities street parts, Shape, City decor: parks on open lots, street scenes (pack fountains), building fronts on the street side, Fronts: building chunk beside a straight street gets a district front part (FRONT_CHANCE 0.5), resolved lazily at write time, Parks: open lots LOT_CHANCE 10% of building chunks carry a district park part one layer up, Road paint: refueled mod decals converted by lcpack paint() into Road_CenterLine and quartz zebra; odd turns flip line meta, District street parts: LC street parts by connection count, road paint to painted road blocks, Street scenes: DeceasedCraft 'fountains' (bus, ambulance, roadblock, trash) in 6% of straight street chunks

### Community 73 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.18
Nodes (9): FixedBase, City engine: Lost Cities style cities from converted DeceasedCraft content, Rules taken from Lost Cities source: street surface at G, ground floor at G, cellars below, stairs at G + 1 toward the higher neighbour, City levels per cell (6 blocks apart), streets at G, stairs parts between levels, City street dressing: sidewalks, centre lines, lamps, benches, wrecks, User review 0.24.4: love it for oneshot progress; highways chosen next, Shots of the first Lost Cities style city, Lost Cities: storey parts, buildings, palettes, city styles as data (+1 more)

### Community 74 - "SealedCaves: SealedCaves"
Cohesion: 0.26
Nodes (6): Caves, Override, Ravines, SealedCaves, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 75 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.22
Nodes (11): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a) (+3 more)

### Community 76 - "bug: Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots)"
Cohesion: 0.18
Nodes (11): Bug (fixed v0.30.2): full military armor made NPC gunfire almost harmless: armor multiplies per piece (x0.149 marine set) and vanilla hit cooldown dropped group hits; NPC shots are direct damage, not bullets, v0.30.4: NPC hit cooldown 0.25 s (npcHitCooldownTicks 5), v0.30.3: vanilla hit cooldown kept for NPC hits (npcHitsSkipCooldown false), x5 stays, Fix v0.30.2: NPC gun hits on players x npcDamageToPlayer (5) after armor, every NPC hit lands (LivingAttackEvent clears hurtResistantTime); bare 10 hp, marine set 1.49, 5 hits 7.47, Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots), CLAUDE.md v0.30.2 note: NPC hits x5, no hit cooldown, mech swap, CLAUDE.md v0.30.3: traced NPC shots, PatchTracer v2 then PatchFactions, test NPCs on a block, Balance: NPC gunfire on the player x5 after armor, every hit lands (v0.30.2) (+3 more)

### Community 77 - "Condition: com.google.gson.JsonObject"
Cohesion: 0.27
Nodes (3): com.google.gson.JsonArray, com.google.gson.JsonObject, Condition

### Community 78 - "Deci: .complete()"
Cohesion: 0.24
Nodes (4): World, NBTTagCompound, net.minecraft.tileentity.TileEntity, TileEntity

### Community 79 - "BiomeMap: BiomeMap"
Cohesion: 0.29
Nodes (3): Sectors, BiomeMap, NoiseGeneratorSimplex

### Community 80 - "interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 81 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.27
Nodes (4): DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River), net.minecraft.world.biome.BiomeGenBase

### Community 82 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 83 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 85 - "TracerTest: TracerTest"
Cohesion: 0.31
Nodes (3): EntityLiving, TracerTest, DevTestMode

### Community 86 - "edgescan: edgescan.py"
Cohesion: 0.39
Nodes (6): Bug: city edge ramp missed its outer columns (fixed v0.24.4, populate scans cells within EDGE), City edge ramp: 24 wide, rounded corners, nearest cell owns a column, wobbled contours, is_city(), jrandom_float(), region_sector(), s64()

### Community 87 - "bug: Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0)"
Cohesion: 0.36
Nodes (8): EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0), v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), Never fill with falling blocks (dirt under grass, sandstone under sand, stone under gravel), Unattended singleplayer autotest (runClient -Pautotest)

### Community 88 - "gradescan: gradescan.py"
Cohesion: 0.43
Nodes (6): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), main(), props(), surface(), main(), populated()

### Community 89 - "DevAutoTest: DevAutoTest"
Cohesion: 0.43
Nodes (3): ClientTickEvent, DevAutoTest, net.decimation.worldgen.devtest.DevTestMode

### Community 93 - "architecture: TurfManager (server.turf.a)"
Cohesion: 0.40
Nodes (6): Bug: ClassCastException deci.a.c to deci.a.e, Block break/place protection handlers (deci.aK.a, b), ClanManagerV1 (server.clans.a), deci.a.b.d() hard cast to ServerProxy, TurfManager (server.turf.a), Clans and turf system

### Community 94 - "gun_model_spec: Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)"
Cohesion: 0.47
Nodes (6): Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), ClientState (deci.b.i), SmoothSwingThread (deci.b.h), Attachment fixed offsets on rails, BModel / BModelPart (deci.n.f, deci.n.b), GunItemRenderer (deci.K.b)

### Community 95 - "interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 96 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.40
Nodes (6): Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief, Technic modpack Decimation 1.7.10 (linusrhone)

### Community 97 - "DeadTree: DeadTree"
Cohesion: 0.40
Nodes (3): DeadTree, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree

### Community 98 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

### Community 99 - "multiscan: multiscan.py"
Cohesion: 0.50
Nodes (4): multiscan seed 1: 1499 of 1499 shelves whole, no orphans, no broken parts, Seed 1 test world (city blocks near spawn, mil_compound at -62,65,434), main(), tile_entities()

### Community 102 - "CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side"
Cohesion: 0.50
Nodes (4): v0.18.0 apartment rooms and propFacing fix, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

### Community 103 - "new_feature: Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours)"
Cohesion: 0.50
Nodes (4): BlockRegistry (deci.aD.c / g), Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours), Full Decimation prop catalog from level.dat registry snapshot, Prop substitution by registry name

## Ambiguous Edges - Review These
- `Genuinely new model recipe (needs Techne)` → `addBox passes Y as Z origin (use addShape)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` → `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `FML IWorldGenerator per chunk hook` → `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **117 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **56 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Genuinely new model recipe (needs Techne)` and `addBox passes Y as Z origin (use addShape)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` and `Recurring root cause: integrated server reports side CLIENT`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `FML IWorldGenerator per chunk hook` and `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Bug tracker (bug.md)` connect `bug: Bug tracker (bug.md)` to `lctranslate: lctranslate.py`, `new_feature: Current state and pending decisions`, `worldgen: World generation doc (deciworldgen)`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `roadmap: Plan: enemy military, juggernaut with machine guns / Barrett, stronger bandits, in military areas`, `architecture: Zones on generated structures (auto zone tagging)`, `create_weapons: Creating new weapons guide`, `floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `architecture: LootInteractHandler (deci.aK.k)`, `bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)`, `CLAUDE: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)`, `ScopeZoom: ScopeZoom`, `worldcheck: worldcheck.py`, `bug: Bug: armor buff ignores NPC gunfire (fixed)`, `edgescan: edgescan.py`, `bug: Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0)`, `architecture: TurfManager (server.turf.a)`, `gun_model_spec: Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Why does `StructureGenerator` connect `NpcTest: net.minecraft.world.World` to `lctranslate: lctranslate.py`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `new_feature: Current state and pending decisions`, `Surfaces: net.minecraft.block.Block`, `worldgen: World generation doc (deciworldgen)`, `LcCity: LcCity`, `BiomeMap: BiomeMap`, `building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status`, `DecimationWorldGen: DecimationWorldGen`, `?: Sectors.java`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._