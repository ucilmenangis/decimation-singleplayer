# Graph Report - .  (2026-10-09)

## Corpus Check
- 1 files · ~165,704 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1995 nodes · 4305 edges · 147 communities (93 shown, 54 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 490 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- NpcTest: net.minecraft.world.World
- StructureGenerator: net.minecraft.world.World
- lctranslate: lctranslate.py
- anvil118: worldcheck.py
- new_feature: Decimation world type doc (terrain, 0.14.0)
- Highways: Highways
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- NpcEgg: NpcEgg
- Deci: Deci
- SchematicPlan: SchematicPlan
- NpcLoadouts: NpcLoadouts
- DevTestUtil: net.minecraft.client.Minecraft
- make_test_schematics: make_test_schematics.py
- new_feature: Feature tracker (new_feature.md)
- Building: Building
- architecture: Obfuscation map (package to meaning)
- architecture: ServerProxy (deci.a.e, dedicated only)
- ?: Sectors.java
- DecimationWorldGen: DecimationWorldGen
- ScopeZoom: ScopeZoom
- StreetPlan: StreetPlan
- create_weapons: Creating new weapons guide
- worldgen: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)
- Deci: Player
- FurnitureSets: com.google.gson.JsonObject
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- EdgePlan: EdgePlan
- Deci: net.minecraft.item.Item
- StoreyPlan: StoreyPlan
- CameraViews: CameraViews
- CLAUDE: decimation-singleplayer README (public repo overview)
- CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)
- ServerChecks: ServerChecks
- bug: Bug tracker (bug.md)
- new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military
- building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status
- Capture: Capture
- floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- NpcShots: net.minecraft.entity.Entity
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- BuildingPlan: BuildingPlan
- LcCity: LcCity
- LcContent: LcContent
- worldgen: World generation doc (deciworldgen)
- furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable
- Surfaces: net.minecraft.block.Block
- Ruins: .unit()
- LotPlan: LotPlan
- DeciBiome: DeciBiome
- ZoneStore: ZoneStore
- CLAUDE: decimation-singleplayer README (public repo overview)
- InfectedVariants: InfectedVariants
- lcstudy: dcinventory.py
- Graded: Graded
- Deci: .onServerTick()
- Name Mapping Applier
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- bug: Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)
- Shell: Shell
- BiomeMap: BiomeMap
- DeciGenLayer: DeciGenLayer
- worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)
- CLAUDE: tools/build.py real javac pipeline
- Facing: .setMeta()
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- SealedCaves: SealedCaves
- building_design: Procedural building design doc (city blocks, Building v2)
- CLAUDE: decimation-singleplayer README (public repo overview)
- city_engine: .decor()
- MilitarySpawner: net.minecraft.world.WorldServer
- AssetDir: AssetDir
- WorldGenCommand: WorldGenCommand
- devtest: Recently done: v0.28.0..0.28.4 cheap scope, v0.28.5 NPC tracers, v0.28.6 / 0.28.7 line of sight cache and prop render distance, v0.28.8 Deci + ZoneKind + launch wait removed, v0.28.9 dev test modes
- bug: Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots)
- bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)
- new_feature: Juggernaut v0.31.0: Soviet side, matching juggernaut set, MGs or armor piercing Barrett, 200 hp, takes 25%, speed 0.18, no knockback; only with military groups (juggernautChance 0.15) and its egg
- new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)
- interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)
- Palettes: Palettes
- VanillaMobs: VanillaMobs
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- DecimationBiomes: DecimationBiomes
- Heuristic Auto Namer
- Furnisher: Furnisher
- DevAutoTest: DevAutoTest
- VehicleHitHandler: SupplyDropScheduler.java
- Interior: .cell()
- bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)
- interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic
- NpcKind: NpcKind
- PatchSwing: PatchSwing.java
- PatchFactions: PatchFactions.java
- CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side
- asset_hashes: asset_hashes.py
- footprint: footprint.py
- EdgePlan: .zone()
- Weather Type Id Bug
- ?: Entity
- ?: EntityLivingBase
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
- ?: Entity
- ?: EntityLivingBase
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
- ?: EntityPlayerMP
- ?: Item
- ?: Load
- ?: net.decimation.worldgen.StructureGenerator
- ?: net.minecraftforge.client.event.FOVUpdateEvent
- ?: NpcKind
- ?: Shape

## God Nodes (most connected - your core abstractions)
1. `Deci` - 79 edges
2. `StructureGenerator` - 51 edges
3. `Building` - 50 edges
4. `NpcLoadouts` - 37 edges
5. `LcCity` - 36 edges
6. `Bug tracker (bug.md)` - 31 edges
7. `DecimationWorldGen` - 31 edges
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

## Communities (147 total, 54 thin omitted)

### Community 0 - "NpcTest: net.minecraft.world.World"
Cohesion: 0.06
Nodes (21): a, Test arena (DevTestArena): flat stone floor at y 150 around (8, 8), every NPC test mode builds and cleans it first; retry Decimation-refused spawns, DamageSource, EntityLiving, NpcKind, World, DevTestArena, Item (+13 more)

### Community 1 - "StructureGenerator: net.minecraft.world.World"
Cohesion: 0.07
Nodes (19): Refactor check on both city paths: worlddiff 0, legacy path by moving the lc pack aside, git stash -u, cpw.mods.fml.common.IWorldGenerator, Yard, Schematic, LargeSites, LegacyStreets, Plan, Slices (+11 more)

### Community 2 - "lctranslate: lctranslate.py"
Cohesion: 0.05
Nodes (52): Bug: building base height depends on chunk generation order (Slices.decideBase), Converted buildings used LED lamp blocks as floor (FIXED 9 Oktober 2026: 'light' in colour names like light_gray matched the lamp rule), Knowledge index: translation audit tools/lcaudit.py, Rule: new code adds an accessor to Deci, never calls deci.* directly (all ~60 obfuscated uses migrated 2026-10-09, seed 1 world 0 blocks differ), lcpack content: 290 converted buildings, stairs per district style (local only), Translation audit 9 Oktober 2026: dropped blocks and props by placement over 290 converted buildings; black sandstone is asphalt, laboratory panels, wallpaper, corundum, posts, shelves, seats mapped, Open bug 4: arrows still pick up empty vehicles (punching fixed v0.8.1), Open bug 3: building base height depends on chunk generation order (low impact) (+44 more)

### Community 3 - "anvil118: worldcheck.py"
Cohesion: 0.06
Nodes (46): Bug: city edge ramp missed its outer columns (fixed v0.24.4, populate scans cells within EDGE), Bug: structures built on ocean floor, Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags) (+38 more)

### Community 4 - "new_feature: Decimation world type doc (terrain, 0.14.0)"
Cohesion: 0.06
Nodes (35): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type) (+27 more)

### Community 5 - "Highways: Highways"
Cohesion: 0.10
Nodes (23): CLAUDE.md v0.29.0 note: highway polish, pregen to check far away L links, Highways, Highway chunk kinds: TUNNEL (median 6+ above deck), BRIDGE (water or ground 2+ below, stone brick pillars), OPEN; stored as hw_X_Z, Highway L links (v0.29.0): isolated city region joined to its nearest diagonal city (at most 2 regions) by an L of the region lines; seed 1: 10 links, city groups 29 -> 19, Highway polish v0.29.0: hedges on crossing parts facing open land, no tunnels in city edge bands, side ramps beside bridges cut land above the deck only, Highway side ramps beside OPEN chunks (deck to natural height over 3..8 blocks, shared LcCity.reshape), City highways: seed based network between city regions, open / bridge / tunnel chunks, deck at 64, side ramps, Recently done v0.29.0: highway polish (L links, crossing hedges, no tunnels at city edges, bridge side ramps) (+15 more)

### Community 6 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.08
Nodes (27): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), Load, World (+19 more)

### Community 7 - "NpcEgg: NpcEgg"
Cohesion: 0.11
Nodes (13): cpw.mods.fml.relauncher.SideOnly, ItemStack, NpcEgg, SuppressWarnings, ZombieEgg, Override, EntityLiving, IIconRegister (+5 more)

### Community 8 - "Deci: Deci"
Cohesion: 0.09
Nodes (7): Deci, Entity, EntityLivingBase, net.minecraft.item.ItemStack, ObjectZoneList, Vector3f, ZoneKind

### Community 9 - "SchematicPlan: SchematicPlan"
Cohesion: 0.06
Nodes (11): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Plan, Schematic, SchematicPlan, ZoneKind, MILITARY, POLICE, SAFEZONE (+3 more)

### Community 10 - "NpcLoadouts: NpcLoadouts"
Cohesion: 0.13
Nodes (5): ItemStack, NpcKind, World, NpcLoadouts, Tier

### Community 11 - "DevTestUtil: net.minecraft.client.Minecraft"
Cohesion: 0.10
Nodes (9): City street after the cache (-Pcityfps): props 5.5% of the client thread, chunk drawing 15%, chunk rebuild 12.7%; props no longer the bottleneck, Scope fps measured (4x on ak74): picture in picture aiming 37..44 vs view zoom aiming 105..132, CityFpsTest, DevTestMode, DevTestUtil, PropsTest, EntityPlayerMP, ScopeTest (+1 more)

### Community 12 - "make_test_schematics: make_test_schematics.py"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 13 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.10
Nodes (29): CLAUDE.md v0.34.0: zombie variants, attribute modifiers trap, TraderSpawnManager (server.traders.a), Zombie variants (v0.34.0) in documentation, 60 round STANAG and 60 round 5.45 AK magazines, Civilian NPC, traders that spawn on their own and walk, more clothing variety on bandits and soldiers, MCP tools for the gun pilot: Blockbench MCP headless (create / edit / render bbmodel, first choice), Blender MCP (Sketchfab, Poly Haven, Rodin), ElevenLabs or Freesound MCP for sounds, Meshy / Tripo text to 3D (paid), Roadmap: pilot new gun, tools Claude runs: web search / agent-browser for licensed assets, .bbmodel / item JSON to .bmodel converter, trimesh voxelizing, PIL / ImageMagick, own preview renderer, arena dev test, ffmpeg for sounds; install trimesh and ffmpeg first, Blender optional, Roadmap: items and weapons (+21 more)

### Community 14 - "Building: Building"
Cohesion: 0.09
Nodes (9): Building, Furnisher, Interior, net.decimation.worldgen.Graded, Ruins, Shell, StoreyPlan, Surfaces (+1 more)

### Community 15 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.12
Nodes (27): Bug: ClassCastException deci.a.c to deci.a.e, Bug: loot never worked in singleplayer, Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, ClientProxy (deci.a.c), DeciConstants (deci.Q.c, GUI ids) (+19 more)

### Community 16 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.11
Nodes (27): Bug: zones never active in singleplayer (partial fix), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), DeathStatsHandler (deci.aK.h), DecimationMod (deci.a.b, @Mod entry) (+19 more)

### Community 17 - "?: Sectors.java"
Cohesion: 0.20
Nodes (9): Building, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.Mod, net.decimation.fixes.MilitarySpawner, net.decimation.fixes.NpcLoadouts, net.decimation.mod.server.zones.ObjectZone, net.decimation.worldgen.building.Building, net.decimation.worldgen.ZoneKind (+1 more)

### Community 18 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.13
Nodes (16): CityDistrict, cpw.mods.fml.common.event.FMLPreInitializationEvent, DecimationWorldGen, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours), FMLPostInitializationEvent, FMLServerStartingEvent, InfectedVariants, MilitarySpawner (+8 more)

### Community 19 - "ScopeZoom: ScopeZoom"
Cohesion: 0.14
Nodes (11): ScopeZoom, Field, Method, net.minecraft.client.renderer.EntityRenderer, net.minecraft.util.ResourceLocation, net.minecraftforge.client.event.RenderHandEvent, net.minecraftforge.client.event.RenderWorldLastEvent, net.minecraftforge.common.config.Configuration (+3 more)

### Community 20 - "StreetPlan: StreetPlan"
Cohesion: 0.11
Nodes (5): Shape, ZoneKind, StreetPlan, StreetProps, City engine files after the 2026-10-09 split: LcCity layout + StreetPlan, BuildingPlan, LotPlan, EdgePlan, StreetProps

### Community 21 - "create_weapons: Creating new weapons guide"
Cohesion: 0.17
Nodes (25): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+17 more)

### Community 22 - "worldgen: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)"
Cohesion: 0.12
Nodes (22): Props, CityDistrict, Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Floor counts (shop 1 to 2, apartment 2 to 9, office 3 to 20, about 15% towers), Lots and yards (2 free on the sides, 3 behind, 6 to 9 front yard; offices and shops car park with nose-in wrecks, apartments gravel path and lawn), 1 block margin ring outside walls for exterior vines (SKIP elsewhere), Parked and wrecked cars on road lanes (spacing at least 9, about 25% of slots filled, never in intersections, rotated along the street) (+14 more)

### Community 23 - "Deci: Player"
Cohesion: 0.13
Nodes (7): b, BottlecapHandler (deciworldgen), BottlecapHandler, Player, HumanityKillHandler, net.minecraftforge.event.entity.living.LivingDeathEvent, net.minecraftforge.event.entity.player.EntityItemPickupEvent

### Community 24 - "FurnitureSets: com.google.gson.JsonObject"
Cohesion: 0.15
Nodes (5): com.google.gson.JsonArray, com.google.gson.JsonObject, Condition, FurnitureSet, FurnitureSets

### Community 25 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.14
Nodes (10): Configuration, cpw.mods.fml.common.eventhandler.SubscribeEvent, ZoneSpawnHandler, DevPregen, ServerTickEvent, EntityConstructing, ItemStack, LivingAttackEvent (+2 more)

### Community 27 - "Deci: net.minecraft.item.Item"
Cohesion: 0.13
Nodes (5): Magazine, Magazines, f, net.minecraft.item.Item, SuppressWarnings

### Community 28 - "StoreyPlan: StoreyPlan"
Cohesion: 0.20
Nodes (3): ApartmentPlanner, OfficePlanner, StoreyPlan

### Community 29 - "CameraViews: CameraViews"
Cohesion: 0.19
Nodes (3): CameraViews, EntityPlayerMP, Entry

### Community 30 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.10
Nodes (21): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CFR decompiler, Decimation.jar (obfuscated Forge 1.7.10 mod jar), deobf/ readable reference tree (decompiled with readable names, read only), Map file case trap (p_deci_aK.tsv equals p_deci_ak.tsv on APFS), Naming source priority (g*, p_*, r_*, m_*, then zz_auto.tsv), rtk hook drops grep/find flags (use Python os.walk) (+13 more)

### Community 31 - "CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)"
Cohesion: 0.13
Nodes (12): Dev test modes: checks (fresh seed 1 world: zones, vehicle, humanity, prop box, bottlecaps, armor, helmet, supply drop), views (-Paudit / -Pgallery / -Pfootprint / -Pstudy / -Pflats / -Psets), scope, tracer, props, cityfps, run/client/devtest/results.txt: one line per value, PASS / FAIL with the expectation, exit code 1 on a FAIL, tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09), Live loop: hotswap code, reload sets, rebuild in place, no restart per change, New dev test: DevTestMode subclass, register its name in DevAutoTest.mode(), record with DevTestResults.value / check, screenshots with DevTestUtil.screenshot, DevTestResults, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), Roadmap: code and tools (+4 more)

### Community 32 - "ServerChecks: ServerChecks"
Cohesion: 0.22
Nodes (3): EntityPlayer, EntityPlayerMP, ServerChecks

### Community 33 - "bug: Bug tracker (bug.md)"
Cohesion: 0.14
Nodes (20): Bug: large ammo crate NPE (dead field avk), Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: CustomSkinLoader coremod crash, Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: no supply drops in singleplayer (fixed v0.9.0), Bug: vehicles destroyed in one hit (fixed v0.8.1) (+12 more)

### Community 34 - "new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military"
Cohesion: 0.11
Nodes (20): CLAUDE.md v0.30.1 note: spawn egg per NPC tier, CLAUDE.md v0.30.4: cooldown ticks, rocket tiers, config version 2, new tiers last, CLAUDE.md v0.30.0 note: NPC tiers, gun sync via data watcher slot 26, test mode npc, Balance v0.30.4: hit cooldown 0.25 s, RPG NPCs, Balance: NPC tiers (v0.30.0), config deciworldgen_npc.cfg, Recently done v0.30.4: hit cooldown 0.25 s, RPG NPCs, Recently done v0.30.0: NPC tiers, stronger bandits, enemy military spawner, Roadmap: zones, factions and world (user list 9 Oktober 2026): advanced safezone, player claimable zones, claims lost to NPCs, police NPC, armed survivor civilians, radiated areas, advanced military base, advanced bandits (+12 more)

### Community 35 - "building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status"
Cohesion: 0.18
Nodes (19): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), BlockProp facing transform from deobf PropRenderer (rotate 180 about x, metadata % 4 * 90 about y, then extra rotation; toward the road: road west 4, east 2, north 5, south 3), Car wreck model axis: long axis along x at 0 degrees (confirmed in game 2026-10-07), Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections), Prop facing rule (PropRenderer rotation = metadata % 4 * 90; north south street 5/3, east west 4/2) (+11 more)

### Community 36 - "Capture: Capture"
Cohesion: 0.21
Nodes (3): Capture, JsonArray, JsonObject

### Community 37 - "floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.18
Nodes (18): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), Knowledge index entry: docs/roadmap.md holds EVERYTHING planned in one list, v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type, Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability) (+10 more)

### Community 38 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.14
Nodes (19): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Exterior add-ons: entrance frame + slab canopy + steps, glass shopfronts with BlockSign_* logo sign, office lobby glazing, apartment balconies, fire escapes over 3 storeys, roof stair hut / water tank / vents / antenna / parapet, downpipes [not verified], Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look (+11 more)

### Community 39 - "NpcShots: net.minecraft.entity.Entity"
Cohesion: 0.20
Nodes (10): CLAUDE.md v0.32.0 note and the shootAt test trap, NpcKind, NpcShots, Balance v0.32.0: elite, bursts, reloads, NPC auto fire v0.32.0: bursts at the gun's rate, spread grows per shot (recoilSpread 0.35), pause after a burst, NPC magazines v0.32.0: fire the gun's magazine (M16 / AK 30, PKM 250), then reload 4 s (reloadTicks 80), net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase (+2 more)

### Community 40 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.16
Nodes (18): BlockRegistry (deci.aD.c / g), Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Door decay: 25 to 50% missing, a few left open (meta bit 4), one barricaded in the most decayed buildings, Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta), Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only, Decals (blood splats, hazard signs, notes, framed picture, flags, warning signs; flat upright pictures, wall face by meta [not verified]), Doors use vanilla door metadata (copy BlockDoor without extending it): lower half 0..3 = west / north / east / south edge, upper half 8; place both halves or the door removes itself (+10 more)

### Community 41 - "BuildingPlan: BuildingPlan"
Cohesion: 0.14
Nodes (4): BuildingPlan, Building, Shape, ZoneKind

### Community 42 - "LcCity: LcCity"
Cohesion: 0.27
Nodes (3): Building, LcCity, Highways

### Community 43 - "LcContent: LcContent"
Cohesion: 0.14
Nodes (4): Building, LcContent, Shape, net.decimation.worldgen.Schematic

### Community 44 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.22
Nodes (17): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), Large schematics (up to 120x120, per site chance, placed inside the site), Safe population window (chunk cx,cz writes only [cx*16+8, cx*16+23]), Filename prefix pools (civ_, city_, mil_, untagged = any sector), MCEdit/WorldEdit .schematic format (no Sponge .schem or .litematic, TileEntities ignored) (+9 more)

### Community 45 - "furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.12
Nodes (17): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, DeceasedCraft interiors fully authored per storey, no procedural rooms, Furniture sets doc: data driven JSON furniture groups, user editable, In-game capture: pos1/pos2, capture set/part, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, known.txt: unedited old built-in copies are updated (tools/asset_hashes.py), Named palettes and weighted styles for sets (base / style keys), Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window (+9 more)

### Community 46 - "Surfaces: net.minecraft.block.Block"
Cohesion: 0.21
Nodes (3): Surfaces, Sub, net.minecraft.block.Block

### Community 48 - "LotPlan: LotPlan"
Cohesion: 0.12
Nodes (4): Shape, ZoneKind, LotPlan, FixedBase

### Community 49 - "DeciBiome: DeciBiome"
Cohesion: 0.15
Nodes (5): DeadTree, Override, DeciBiome, net.minecraft.world.gen.feature.WorldGenAbstractTree, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 50 - "ZoneStore: ZoneStore"
Cohesion: 0.18
Nodes (8): Load, ObjectZone, ObjectZoneList, ServerTickEvent, ZoneStore, net.decimation.mod.server.zones.ObjectZoneList, Save, Unload

### Community 51 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.19
Nodes (15): CLAUDE.md project guide, Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), Graph update workflow (graph_update.py prepare/finish, one agent per chunk), In place jar entry update (zip -u, never rebuild), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph), Public repo rule: never commit Decimation jars, decompiled code or tools/lib (check git ls-files, nothing over 5 MB), Visual evidence rule: keep screenshots in docs/shots/<topic>_v<version>/ (git ignored) and describe them in docs/shots_index.md; read the index, re-shoot only when the code behind it changed (+7 more)

### Community 52 - "InfectedVariants: InfectedVariants"
Cohesion: 0.19
Nodes (5): InfectedVariants, Variant, ItemStack, LivingUpdateEvent, net.minecraft.entity.ai.attributes.IAttributeInstance

### Community 53 - "lcstudy: dcinventory.py"
Cohesion: 0.23
Nodes (11): category(), districts(), main(), building name -> {city style: weight share}, structure_summary(), category(), main(), Pack (+3 more)

### Community 54 - "Graded: Graded"
Cohesion: 0.16
Nodes (9): EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0), v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), Graded, Never fill with falling blocks (dirt under grass, sandstone under sand, stone under gravel) (+1 more)

### Community 55 - "Deci: .onServerTick()"
Cohesion: 0.18
Nodes (4): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Block, ServerTickEvent, SupplyDropScheduler

### Community 56 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 57 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.18
Nodes (15): Skipped giant buildings: casino 276 high, oasis condo top above 250, laboratory 90 deep cellars, City engine open items: Lost Cities bridges and rail unused, giant buildings too tall, rotation only data variants, chests became wood crates, City superblocks: 2x2 cells, 7x7 building chunks, landmark towers, DeceasedCraft content catalogue: 79 Lost Cities buildings, city parts, apocalypsenow structures, disabled vanilla structures, Lost Cities to 1.7.10 conversion (lc2schem, lctranslate, paste command), DeceasedCraft 79 building types and 5 district city styles, Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data), DeceasedCraft storey is 6 high (4 air) vs our 5 (+7 more)

### Community 58 - "bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.21
Nodes (12): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, PatchPropCulling step 3 (v0.28.6): line of sight answers cached until the player or entity moves 0.3 blocks (props 1.0..1.3 s, entities 0.15..0.18 s); prop renderer share 21% -> 9%, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, PatchPropCulling step 4: render distance by prop size (deciworldgen_props.cfg, small / medium / large; default 64 = vanilla), Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Hand written Forge/Minecraft stub classes, Javassist bytecode patcher, Javassist cannot compile Java 8 lambdas (+4 more)

### Community 59 - "bug: Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)"
Cohesion: 0.19
Nodes (12): v0.28.1: EntityRenderer.cameraZoom zooms world and gun together; ScopeZoom draws the hand itself on RenderHandEvent (vanilla skips it while zoomed), Scope FPS drop: picture in picture second world render, fixed v0.28.0 with view zoom, Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4), PatchScope: renderScopeView gated behind system property decimation.scope.pip (old scope via pictureInPicture=true in config/deciworldgen_scope.cfg), v0.28.2 / 0.28.3: projective see-through glass (frame copied, mapped by screen position), sight learned per gun + scope + window aspect, copy limited to the glass box, v0.28.4: scopes from overlayFrom (default 4x) hide the gun and draw a black sniper overlay with the scope's reticle texture on the HUD, Javassist snippets must compile against Java 8 signatures (FloatBuffer.flip() through java.nio.Buffer, else NoSuchMethodError in game), v0.28.0..0.28.4 cheap scope versions (view zoom, cameraZoom world + gun, projective glass, glass box copy, sniper overlay from 4x) (+4 more)

### Community 61 - "BiomeMap: BiomeMap"
Cohesion: 0.21
Nodes (6): Sectors, BiomeMap, NoiseGeneratorSimplex, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors), Sector grid (16x16 chunks: WILD 40%, CIV 25%, CITY 15%, MIL 20%)

### Community 62 - "DeciGenLayer: DeciGenLayer"
Cohesion: 0.18
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 63 - "worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)"
Cohesion: 0.15
Nodes (14): Builders: separate ceiling tiles with light panel grid and vents, Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161), Builders: decay as dirt/leaves/water/cracked glass on intact shells, Builders: furniture in rows and islands (waiting rows, cubicles, shelf aisles), Autotest -Pstudy camera mode for reference maps, Builders: two tone WallOffice walls (dado bottom + top), Audit shots v0.23: 6 high offices and shops, Worldgen code map: building package parts (Shell, StoreyPlan, planners, Furnisher, Surfaces, Interior, Ruins, Yard) (+6 more)

### Community 64 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 65 - "Facing: .setMeta()"
Cohesion: 0.26
Nodes (3): Facing, Entry, ShopPlanner

### Community 66 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.17
Nodes (10): FixedBase, City engine: Lost Cities style cities from converted DeceasedCraft content, Rules taken from Lost Cities source: street surface at G, ground floor at G, cellars below, stairs at G + 1 toward the higher neighbour, City levels per cell (6 blocks apart), streets at G, stairs parts between levels, City street dressing: sidewalks, centre lines, lamps, benches, wrecks, User review 0.24.4: love it for oneshot progress; highways chosen next, Shots of the first Lost Cities style city, Study: Lost Cities and Recurrent Complex organisation (+2 more)

### Community 67 - "SealedCaves: SealedCaves"
Cohesion: 0.24
Nodes (7): Caves, Override, Ravines, SealedCaves, net.minecraft.world.biome.BiomeGenBase, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 68 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.27
Nodes (13): Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Procedural building design doc (city blocks, Building v2), Minecraft scale (storey 4 blocks, corridor 2 wide, doors 1x2, units 5 to 8 deep), Office floor layout (core, open plan desk rows at windows, meeting rooms, reception, storage, break room), Layout research sources (Auckland Design Manual, archgyan, Pult, Shopify, Small Business Trends), Shop grid layout (parallel aisles, decompression zone, checkout front left, stockroom behind) (+5 more)

### Community 69 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.21
Nodes (12): Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), dev/libs/Decimation-base.jar (patched jar minus our classes), dev/ RetroFuturaGradle workspace (GTNH ExampleMod1.7.10 template), MCP readable member names in dev workspace, DecimationVoiceChat.jar required or menu shows Update instead of Play, ClientState (deci.b.i), SmoothSwingThread (deci.b.h), Attachment fixed offsets on rails (+4 more)

### Community 70 - "city_engine: .decor()"
Cohesion: 0.20
Nodes (8): v0.25.0 highways, v0.26.0 parks / street scenes / fronts, v0.27.0 district Lost Cities street parts, Shape, City decor: parks on open lots, street scenes (pack fountains), building fronts on the street side, Fronts: building chunk beside a straight street gets a district front part (FRONT_CHANCE 0.5), resolved lazily at write time, Parks: open lots LOT_CHANCE 10% of building chunks carry a district park part one layer up, Road paint: refueled mod decals converted by lcpack paint() into Road_CenterLine and quartz zebra; odd turns flip line meta, District street parts: LC street parts by connection count, road paint to painted road blocks, Street scenes: DeceasedCraft 'fountains' (bus, ambulance, roadblock, trash) in 6% of straight street chunks

### Community 71 - "MilitarySpawner: net.minecraft.world.WorldServer"
Cohesion: 0.23
Nodes (3): MilitarySpawner, net.minecraft.world.WorldServer, ServerTickEvent

### Community 73 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.38
Nodes (4): Override, WorldGenCommand, net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender

### Community 74 - "devtest: Recently done: v0.28.0..0.28.4 cheap scope, v0.28.5 NPC tracers, v0.28.6 / 0.28.7 line of sight cache and prop render distance, v0.28.8 Deci + ZoneKind + launch wait removed, v0.28.9 dev test modes"
Cohesion: 0.22
Nodes (8): Launch waited 5 s for the dead Decimation backend (kryonet hardcoded 5000 ms); PatchBackend, Prop dense FPS drop: line of sight ray casts 76% of prop rendering, cached in v0.28.6; model drawing remains, Obfuscated Decimation names in our code go through fixes/Deci, Worldgen uses ZoneKind instead of Decimation's obfuscated zone enum; Deci.zoneType converts, Recently done: v0.28.0..0.28.4 cheap scope, v0.28.5 NPC tracers, v0.28.6 / 0.28.7 line of sight cache and prop render distance, v0.28.8 Deci + ZoneKind + launch wait removed, v0.28.9 dev test modes, main(), sheets(), PatchBackend

### Community 75 - "bug: Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots)"
Cohesion: 0.18
Nodes (11): Bug (fixed v0.30.2): full military armor made NPC gunfire almost harmless: armor multiplies per piece (x0.149 marine set) and vanilla hit cooldown dropped group hits; NPC shots are direct damage, not bullets, v0.30.4: NPC hit cooldown 0.25 s (npcHitCooldownTicks 5), v0.30.3: vanilla hit cooldown kept for NPC hits (npcHitsSkipCooldown false), x5 stays, Fix v0.30.2: NPC gun hits on players x npcDamageToPlayer (5) after armor, every NPC hit lands (LivingAttackEvent clears hurtResistantTime); bare 10 hp, marine set 1.49, 5 hits 7.47, Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots), CLAUDE.md v0.30.2 note: NPC hits x5, no hit cooldown, mech swap, CLAUDE.md v0.30.3: traced NPC shots, PatchTracer v2 then PatchFactions, test NPCs on a block, Balance: NPC gunfire on the player x5 after armor, every hit lands (v0.30.2) (+3 more)

### Community 76 - "bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)"
Cohesion: 0.20
Nodes (8): NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer), Cause: PacketGunFireEffects carried only the shooter id; the client drew the tracer along getLook() (a mob's body facing), Tracer test (-Ptracer): original 52 tracers mean 133 degrees off the target, patched 42 tracers mean 1.0, max 2.0, NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), PatchTracer

### Community 77 - "new_feature: Juggernaut v0.31.0: Soviet side, matching juggernaut set, MGs or armor piercing Barrett, 200 hp, takes 25%, speed 0.18, no knockback; only with military groups (juggernautChance 0.15) and its egg"
Cohesion: 0.18
Nodes (11): CLAUDE.md v0.31.0: juggernaut tier, Barrett armor piercing, Balance v0.31.0: juggernaut, Recently done v0.32.0: elite military, bursts, magazines, Recently done v0.31.0: juggernaut, npc_elite_v0.32.0: elites with night vision goggles, npc_juggernaut_v0.31.0: juggernaut lineup and eggs, npc_snipers_v0.32.1: juggernaut with Barrett and elite snipers, Elite military v0.32.0: marine black, night vision goggles, MGs or snipers with all attachments, 150 hp, takes 16%, x2 damage; only with military groups (eliteChance 0.2) and its egg (+3 more)

### Community 78 - "new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)"
Cohesion: 0.24
Nodes (11): Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside), Spec user decisions: survivor camps (about 1 in 12 buildings), shop signs use BlockSign_* assets, reusable parts for later categories, start by fixing the audited apartment / office / shop, Zone tagging (mil_ MILITARY, city_ and buildings POLICE, deciworldgen_zones.json), Direction change (8 Oktober 2026): DeceasedCraft buildings as city content with Lost Cities layout; procedural buildings stay as fallback, Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces), Round 2 step 5 (was step 4): new procedural building types (police station, hospital, gas station, warehouse), then step 6 loot balance per building (+3 more)

### Community 79 - "interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 80 - "Palettes: Palettes"
Cohesion: 0.33
Nodes (3): Palettes, Style, Entry

### Community 81 - "VanillaMobs: VanillaMobs"
Cohesion: 0.25
Nodes (4): Trap: vanilla class names are obfuscated in the shipped game, never test by package name (copySpawns fixed), VanillaMobs, No vanilla mobs (v0.33.0) in documentation, No vanilla mobs v0.33.0: vanilla monsters, animals, squid, bats, villagers, golems out of overworld spawn lists and refused on join (old chunks too); config deciworldgen_mobs.cfg; tests tag needed mobs with VanillaMobs.KEEP

### Community 82 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 83 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.33
Nodes (4): DeciBiome, DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River)

### Community 84 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 86 - "DevAutoTest: DevAutoTest"
Cohesion: 0.43
Nodes (3): ClientTickEvent, DevAutoTest, net.decimation.worldgen.devtest.DevTestMode

### Community 87 - "VehicleHitHandler: SupplyDropScheduler.java"
Cohesion: 0.29
Nodes (4): VehicleHitHandler, EntityPlayer, net.minecraft.server.MinecraftServer, net.minecraftforge.event.entity.player.AttackEntityEvent

### Community 89 - "bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)"
Cohesion: 0.47
Nodes (6): Baking static props into chunk meshes (prop textures into the block atlas, both model formats to quads): full fix for open views, not started, Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains), Prop fps measured (-Pprops): 225 props in view 12..14 fps vs empty 28..29; 76% of PropRenderer time in canSeeTileEntity ray casts, -Pjfr Java Flight Recorder CPU profile (run/client/profile.jfr, read with jfr print --json; client thread is main in dev), Performance: profile before optimising (scope fixed v0.28, props improved v0.28.6; guesses were wrong twice), Open bug 2: drawing cost of props in open prop heavy views (225 props halve the fps; fix by baking static props into chunk meshes)

### Community 90 - "interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 91 - "NpcKind: NpcKind"
Cohesion: 0.33
Nodes (5): NpcKind, BANDIT, HAZMAT, SOLDIER, SOVIET

### Community 94 - "CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side"
Cohesion: 0.50
Nodes (4): v0.18.0 apartment rooms and propFacing fix, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

## Ambiguous Edges - Review These
- `Genuinely new model recipe (needs Techne)` → `addBox passes Y as Z origin (use addShape)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` → `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)` → `FML IWorldGenerator per chunk hook`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **129 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+124 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **54 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

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
- **What is the exact relationship between `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)` and `FML IWorldGenerator per chunk hook`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `StructureGenerator` connect `StructureGenerator: net.minecraft.world.World` to `building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status`, `new_feature: Decimation world type doc (terrain, 0.14.0)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `LcCity: LcCity`, `worldgen: World generation doc (deciworldgen)`, `Surfaces: net.minecraft.block.Block`, `?: Sectors.java`, `DecimationWorldGen: DecimationWorldGen`, `BiomeMap: BiomeMap`, `CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Why does `Bug tracker (bug.md)` connect `bug: Bug tracker (bug.md)` to `lctranslate: lctranslate.py`, `anvil118: worldcheck.py`, `new_feature: Decimation world type doc (terrain, 0.14.0)`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)`, `bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)`, `worldgen: World generation doc (deciworldgen)`, `new_feature: Feature tracker (new_feature.md)`, `architecture: Obfuscation map (package to meaning)`, `architecture: ServerProxy (deci.a.e, dedicated only)`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `create_weapons: Creating new weapons guide`, `Graded: Graded`, `bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)`, `bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)`, `bug: Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._