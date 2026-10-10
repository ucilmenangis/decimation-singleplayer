# Graph Report - .  (2026-10-10)

## Corpus Check
- 15 files · ~231,155 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2466 nodes · 5292 edges · 188 communities (120 shown, 68 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 559 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- MilitaryBase: MilitaryBase
- StructureGenerator: net.minecraft.world.World
- lctranslate: lctranslate.py
- ServerChecks: ServerChecks
- new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military
- Deci: Deci
- ScopeZoom: ScopeZoom
- gunmodel: gunmodel.py
- new_feature: Feature tracker (new_feature.md)
- DevTestUtil: net.minecraft.client.Minecraft
- Building: Building
- Highways: Highways
- make_test_schematics: make_test_schematics.py
- NpcEgg: cpw.mods.fml.relauncher.SideOnly
- architecture: Obfuscation map (package to meaning)
- new_feature: Current state and pending decisions
- IronSights: net.minecraft.item.ItemStack
- DecimationWorldGen: DecimationWorldGen
- bug: Bug tracker (bug.md)
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- create_weapons: Creating new weapons guide
- InfectedVariants: InfectedVariants
- Deci: net.minecraft.item.Item
- StreetPlan: StreetPlan
- NpcShots: NpcShots
- NpcTest: .batches()
- CLAUDE: decimation-singleplayer README (public repo overview)
- building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status
- NpcLoadouts: NpcLoadouts
- StoreyPlan: StoreyPlan
- Surfaces: net.minecraft.block.Block
- SchematicPlan: SchematicPlan
- Plan: Plan
- ZoneStore: ZoneStore
- worldgen: World generation doc (deciworldgen)
- bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- FurnitureSets: com.google.gson.JsonObject
- EdgePlan: EdgePlan
- CameraViews: CameraViews
- bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- DeciBiome: DeciBiome
- building_design: Procedural building design doc (city blocks, Building v2)
- ?: Sectors.java
- Capture: Capture
- performance: Performance doc: tools (gunperf, census, perfcheck, JFR via jcmd), findings, checklist
- gun_style_guide: Decimation gun style guide: study of all 98 shipped guns (look and construction rules for our own guns)
- sights: sights.py
- SupplyDropScheduler: .onServerTick()
- CLAUDE: Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags)
- CLAUDE: Javassist bytecode patcher
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- Furnisher: Furnisher
- BuildingPlan: BuildingPlan
- LcContent: LcContent
- floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- architecture: ServerProxy (deci.a.e, dedicated only)
- furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable
- LcCity: LcCity
- LotPlan: LotPlan
- new_feature: MAC-10 v2 (v0.37.0): 102 parts, 9.35 x 8.1 x 1.5, proportions from the user's side photo, sights at the aim centre, checked in game (gunview, gun)
- MilitarySpawner: net.minecraft.entity.player.EntityPlayer
- worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)
- lcstudy: lcstudy.py
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- Name Mapping Applier
- worldcheck: worldcheck.py
- edgescan: gradescan.py
- ?: net.minecraft.entity.EntityLiving
- Ruins: .unit()
- BiomeMap: BiomeMap
- ?: DeciGenLayer
- hwmap: hwmap.py
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- CLAUDE: tools/build.py real javac pipeline
- DevTestLive: DevTestLive
- hk416: hk416.py
- Shell: Shell
- anvil118: anvil118.py
- study: study.py
- CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)
- city_engine: .decor()
- NpcLoadouts: Tier
- AssetDir: AssetDir
- SealedCaves: SealedCaves
- WorldGenCommand: WorldGenCommand
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- DevAutoTest: DevAutoTest
- FireRateTest: FireRateTest
- bug: worlddiff.py
- interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)
- SKILL: decimation-gun skill revision casebook: 8 cases (boxy look, aim centre, suppressor floating, suppressor low / threads, gradation, stock in sight picture, icon halo, test traps) with symptom, cause, code location, fix, check, evidence
- mk18: mk18.py
- DecimationBiomes: DecimationBiomes
- Deci: .complete()
- Palettes: Palettes
- GunPerfTest: GunPerfTest
- StructureData: StructureData
- study: Part
- sightfit: sightfit.py
- VanillaMobs: VanillaMobs
- SKILL: decimation-gun skill revision casebook: 8 cases (boxy look, aim centre, suppressor floating, suppressor low / threads, gradation, stock in sight picture, icon halo, test traps) with symptom, cause, code location, fix, check, evidence
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- Heuristic Auto Namer
- TracerTest: TracerTest
- deceasedcraft_buildings: Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data)
- study: main()
- gun_style_guide: Gun aiming: aim mode draws every gun at one fixed place (x 0.5, translate -1 -0.35 0.923), sPos unused in first person; screen centre at the top of the iron sights, y -4.85 to -5.0, z -0.15
- CensusTest: net.minecraft.world.WorldServer
- GunTest: GunTest
- ShotTest: ShotTest
- ZombieTest: ZombieTest
- bbmcp: Server
- devtest: devtest.py
- study: .verts()
- GunViewTest: GunViewTest
- PropsTest: DevTestMode
- interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic
- NewGuns: NewGuns
- NpcKind: NpcKind
- FixedBase: FixedBase
- study: with_attachments()
- perfcheck: perfcheck.py
- PatchIronSights: Case 16: Mk18 Mod 1 and iron sights hidden under a sight (v0.40.0)
- PatchFactions: PatchFactions.java
- new_feature: NPC auto fire v0.32.0: bursts at the gun's rate, spread grows per shot (recoilSpread 0.35), pause after a burst
- CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side
- gun_style_guide: MAC-10 v2 revision v0.37.1: suppressor flush, gradation, aim checked steady like the Uzi
- bug: Bug: NPC machine guns fire at double rate (v0.39.2)
- asset_hashes: asset_hashes.py
- footprint: footprint.py
- PatchInfectedAI: PatchInfectedAI
- EdgePlan: .zone()
- ?: CityDistrict
- CLAUDE: Prism instance updated to 0.38.1 (2026-10-10) with the dev worldgen config; it has OptiFine HD U E7 and RTG, the dev client has neither
- Weather Type Id Bug
- ?: Entity
- ?: EntityLiving
- ?: EntityLivingBase
- ?: NpcKind
- ?: ObjectZone
- ?: ObjectZoneList
- ?: SuppressWarnings
- ?: TileEntity
- ?: World
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
- ?: EntityLiving
- ?: EntityPlayer
- ?: Entry
- ?: ItemStack
- ?: ServerTickEvent
- ?: ItemStack
- ?: Entity
- ?: EntityLivingBase
- ?: a
- ?: a
- ?: FurnitureSet
- ?: a
- ?: Override
- ?: World
- ?: a
- interior_spec: propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N
- CLAUDE: Live dev test trap: key=value persists across live runs, clear with key=; wait after --stop
- roadmap: Roadmap UI: main menu GUI fix later (user), details not decided
- shots_index: flats_v0.20 / v0.21 interiors and sets_v0.21 kitchens
- ?: LargeSites
- ?: Load
- ?: net.decimation.worldgen.StructureGenerator
- ?: net.decimation.worldgen.StructureGenerator.Sub
- ?: net.minecraftforge.client.event.FOVUpdateEvent
- ?: Schematic
- ?: Shape
- ?: Sub

## God Nodes (most connected - your core abstractions)
1. `Deci` - 83 edges
2. `StructureGenerator` - 52 edges
3. `MilitaryBase` - 51 edges
4. `Building` - 50 edges
5. `NpcLoadouts` - 40 edges
6. `LcCity` - 34 edges
7. `Bug tracker (bug.md)` - 31 edges
8. `DecimationWorldGen` - 30 edges
9. `ScopeZoom` - 29 edges
10. `Current state and pending decisions` - 29 edges

## Surprising Connections (you probably didn't know these)
- `Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside)` --semantically_similar_to--> `survivor_camp()`  [INFERRED] [semantically similar]
  docs/interior_spec.md → tools/make_test_schematics.py
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` --semantically_similar_to--> `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS] [semantically similar]
  docs/terrain.md → CLAUDE.md
- `Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections)` --references--> `DevAutoTest`  [INFERRED]
  docs/building_design.md → dev/src/main/java/net/decimation/worldgen/DevAutoTest.java
- `run/client/devtest/results.txt: one line per value, PASS / FAIL with the expectation, exit code 1 on a FAIL` --references--> `DevTestResults`  [INFERRED]
  CLAUDE.md → dev/src/main/java/net/decimation/worldgen/devtest/DevTestResults.java
- `Tracer test (-Ptracer): original 52 tracers mean 133 degrees off the target, patched 42 tracers mean 1.0, max 2.0` --references--> `TracerTest`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/worldgen/devtest/TracerTest.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling, tools_patches_patchscope, tools_patches_patchtracer, tools_patches_patchbackend [INFERRED 0.85]
- **Visual evidence memory (autotest screenshot modes, docs/shots, written shots index, prop catalogue)** — claude_visual_evidence_rule, claude_autotest_screenshot_modes, docs_shots_index_shots_index, docs_prop_catalogue_prop_catalogue_doc [INFERRED 0.85]
- **Dev test modes run in one launch by tools/devtest.py** — claude_devtest_workflow, tools_devtest, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest, dev_src_main_java_net_decimation_worldgen_devtest_devtestmode_devtestmode, dev_src_main_java_net_decimation_worldgen_devtest_devtestresults_devtestresults, dev_src_main_java_net_decimation_worldgen_devtest_serverchecks_serverchecks, dev_src_main_java_net_decimation_worldgen_devtest_cameraviews_cameraviews, dev_src_main_java_net_decimation_worldgen_devtest_scopetest_scopetest, dev_src_main_java_net_decimation_worldgen_devtest_tracertest_tracertest, dev_src_main_java_net_decimation_worldgen_devtest_propstest_propstest, dev_src_main_java_net_decimation_worldgen_devtest_cityfpstest_cityfpstest [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Upper storey reachability fix v0.16.1 (floorplan scan, unsupported ladder, collapse exemption, Building.ladder / collapsed)** — bug_upper_storeys_unreachable, bug_ladder_unsupported_wall_cell, bug_ladder_core_collapse_exemption, tools_floorplan [EXTRACTED 1.00]
- **Prop render performance: measure, cache line of sight, render distance, bake (open)** — bug_prop_fps_drop, bug_prop_fps_measurement, bug_los_cache, bug_prop_render_distance_by_size, bug_cityfps_measurement, bug_bake_props_chunk_mesh, tools_patches_patchpropculling_patchpropculling, claude_jfr_profiling, docs_roadmap_prop_drawing_cost [EXTRACTED 1.00]
- **Cheap scope v0.28.0..0.28.4 (gate, view zoom, projective glass, sniper overlay)** — feature_cheap_scope, bug_scope_pip_gate, bug_scope_camera_zoom, bug_scope_projective_glass, bug_scope_sniper_overlay, dev_src_main_java_net_decimation_fixes_scopezoom_scopezoom, tools_patches_patchscope_patchscope, dev_src_main_java_net_decimation_worldgen_devtest_scopetest_scopetest [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
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
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]

## Communities (188 total, 68 thin omitted)

### Community 0 - "MilitaryBase: MilitaryBase"
Cohesion: 0.06
Nodes (5): MilBaseTest, Canvas, MilitaryBase, MilitaryBasePlan, net.decimation.worldgen.ZoneKind

### Community 1 - "StructureGenerator: net.minecraft.world.World"
Cohesion: 0.06
Nodes (26): Refactor check on both city paths: worlddiff 0, legacy path by moving the lc pack aside, git stash -u, Test arena (DevTestArena): flat stone floor at y 150 around (8, 8), every NPC test mode builds and cleans it first; retry Decimation-refused spawns, v0.12.1: whole footprint floor height sampling and dirt fill under schematics, cpw.mods.fml.common.IWorldGenerator, Props, Yard, CityDistrict, DevTestArena (+18 more)

### Community 2 - "lctranslate: lctranslate.py"
Cohesion: 0.06
Nodes (45): Converted buildings used LED lamp blocks as floor (FIXED 9 Oktober 2026: 'light' in colour names like light_gray matched the lamp rule), Knowledge index: translation audit tools/lcaudit.py, lcpack content: 290 converted buildings, stairs per district style (local only), Translation audit 9 Oktober 2026: dropped blocks and props by placement over 290 converted buildings; black sandstone is asphalt, laboratory panels, wallpaper, corundum, posts, shelves, seats mapped, Recently done: converted building quality pass (pack rebuilt, LED floor fixed, about 30000 dropped blocks mapped), lc_quality_v0.28.10 shots: 30 interior views after the translator fix, no lamp floors, ceiling redstone lamps; black sandstone stairs now cobblestone, building_columns(), main() (+37 more)

### Community 3 - "ServerChecks: ServerChecks"
Cohesion: 0.08
Nodes (10): b, BottlecapHandler (deciworldgen), BottlecapHandler, Player, HumanityKillHandler, EntityPlayer, EntityPlayerMP, ServerChecks (+2 more)

### Community 4 - "new_feature: NPC tiers design and result (v0.30.0): tier per armed NPC on first join (gear, gun, health, fire rate, share of player gun damage), stored in entity data, gun synced via data watcher slot 26; bandit light / medium / heavy, soldier camo sets, Soviets as enemy military"
Cohesion: 0.04
Nodes (48): CLAUDE.md v0.42.0 note: military bases, worldgen/military, milbase dev test, mil_ schematics no longer placed, Military base case 1: first FOB generator, four rounds (colours, test setup, overlaps, density), Military base lessons: research first, occupancy grid against overlaps, company streets, sky test without grading, orange dirt in Decimation's pack, avoid regular patterns, camera points in the generator, Project skill decimation-military-base: research, build (Canvas, occupancy grid, greedy fit, poi), test, small detail review checklist, CLAUDE.md v0.30.1 note: spawn egg per NPC tier, CLAUDE.md v0.30.4: cooldown ticks, rocket tiers, config version 2, new tiers last, CLAUDE.md v0.30.0 note: NPC tiers, gun sync via data watcher slot 26, test mode npc, CLAUDE.md v0.31.0: juggernaut tier, Barrett armor piercing (+40 more)

### Community 5 - "Deci: Deci"
Cohesion: 0.07
Nodes (12): a, DamageSource, Deci, Block, Entity, EntityLivingBase, f, NpcKind (+4 more)

### Community 6 - "ScopeZoom: ScopeZoom"
Cohesion: 0.09
Nodes (22): v0.28.1: EntityRenderer.cameraZoom zooms world and gun together; ScopeZoom draws the hand itself on RenderHandEvent (vanilla skips it while zoomed), Scope FPS drop: picture in picture second world render, fixed v0.28.0 with view zoom, Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4), PatchScope: renderScopeView gated behind system property decimation.scope.pip (old scope via pictureInPicture=true in config/deciworldgen_scope.cfg), v0.28.2 / 0.28.3: projective see-through glass (frame copied, mapped by screen position), sight learned per gun + scope + window aspect, copy limited to the glass box, v0.28.4: scopes from overlayFrom (default 4x) hide the gun and draw a black sniper overlay with the scope's reticle texture on the HUD, Javassist snippets must compile against Java 8 signatures (FloatBuffer.flip() through java.nio.Buffer, else NoSuchMethodError in game), v0.28.0..0.28.4 cheap scope versions (view zoom, cameraZoom world + gun, projective glass, glass box copy, sniper overlay from 4x) (+14 more)

### Community 7 - "gunmodel: gunmodel.py"
Cohesion: 0.08
Nodes (31): anib(), bmodel(), build(), fnum(), icon(), layout(), numbered(), octagon() (+23 more)

### Community 8 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.08
Nodes (35): NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer), Cause: PacketGunFireEffects carried only the shooter id; the client drew the tracer along getLook() (a mob's body facing), Tracer test (-Ptracer): original 52 tracers mean 133 degrees off the target, patched 42 tracers mean 1.0, max 2.0, NPC ranged attacks call attackEntityFrom directly server side, CLAUDE.md v0.34.0: zombie variants, attribute modifiers trap, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected) (+27 more)

### Community 9 - "DevTestUtil: net.minecraft.client.Minecraft"
Cohesion: 0.10
Nodes (11): City street after the cache (-Pcityfps): props 5.5% of the client thread, chunk drawing 15%, chunk rebuild 12.7%; props no longer the bottleneck, Scope fps measured (4x on ak74): picture in picture aiming 37..44 vs view zoom aiming 105..132, Dev test modes: checks (fresh seed 1 world: zones, vehicle, humanity, prop box, bottlecaps, armor, helmet, supply drop), views (-Paudit / -Pgallery / -Pfootprint / -Pstudy / -Pflats / -Psets), scope, tracer, props, cityfps, New dev test: DevTestMode subclass, register its name in DevAutoTest.mode(), record with DevTestResults.value / check, screenshots with DevTestUtil.screenshot, CityFpsTest, DevTestMode, DevTestResults, DevTestUtil (+3 more)

### Community 10 - "Building: Building"
Cohesion: 0.08
Nodes (10): Building, ShopPlanner, Furnisher, Interior, net.decimation.worldgen.Graded, Ruins, Shell, StoreyPlan (+2 more)

### Community 11 - "Highways: Highways"
Cohesion: 0.14
Nodes (10): CLAUDE.md v0.29.0 note: highway polish, pregen to check far away L links, Highways, Highway chunk kinds: TUNNEL (median 6+ above deck), BRIDGE (water or ground 2+ below, stone brick pillars), OPEN; stored as hw_X_Z, Highway L links (v0.29.0): isolated city region joined to its nearest diagonal city (at most 2 regions) by an L of the region lines; seed 1: 10 links, city groups 29 -> 19, Highway polish v0.29.0: hedges on crossing parts facing open land, no tunnels in city edge bands, side ramps beside bridges cut land above the deck only, Highway side ramps beside OPEN chunks (deck to natural height over 3..8 blocks, shared LcCity.reshape), City highways: seed based network between city regions, open / bridge / tunnel chunks, deck at 64, side ramps, Recently done v0.29.0: highway polish (L links, crossing hedges, no tunnels at city edges, bridge side ramps) (+2 more)

### Community 12 - "make_test_schematics: make_test_schematics.py"
Cohesion: 0.16
Nodes (26): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+18 more)

### Community 13 - "NpcEgg: cpw.mods.fml.relauncher.SideOnly"
Cohesion: 0.15
Nodes (11): cpw.mods.fml.relauncher.SideOnly, Magazine, ItemStack, NpcEgg, SuppressWarnings, ZombieEgg, IIconRegister, net.minecraft.client.renderer.texture.IIconRegister (+3 more)

### Community 14 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.10
Nodes (31): Obfuscation map (package to meaning), Map filenames unique ignoring case, Deobfuscation naming brief, 8 agent deobfuscation naming pass, Naming map TSV format (C/F/M lines, H/M/L confidence), Never rename overrides, func_/field_ members, constructors, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes (+23 more)

### Community 15 - "new_feature: Current state and pending decisions"
Cohesion: 0.09
Nodes (26): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), Rotation, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Ravines cut 40 block trenches through flat cities; SealedCaves (InitMapGenEvent) digs nothing above y 50 under city and military biomes (+18 more)

### Community 16 - "IronSights: net.minecraft.item.ItemStack"
Cohesion: 0.09
Nodes (9): Bug: colourful M4A4 rail teeth under a sight (v0.41.0), Case 19: reticles fixed on screen, EOTech too big, HK416 front sight (v0.41.1), IronSights, SightPlacement, SightReticle, Item, NBTTagCompound, net.minecraft.item.ItemStack (+1 more)

### Community 17 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.11
Nodes (17): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, DecimationWorldGen, CityDistrict, Schematic, Sub, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours) (+9 more)

### Community 18 - "bug: Bug tracker (bug.md)"
Cohesion: 0.11
Nodes (28): Bug: large ammo crate NPE (dead field avk), Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0) (+20 more)

### Community 19 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.10
Nodes (28): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Exterior add-ons: entrance frame + slab canopy + steps, glass shopfronts with BlockSign_* logo sign, office lobby glazing, apartment balconies, fire escapes over 3 storeys, roof stair hut / water tank / vents / antenna / parapet, downpipes [not verified], Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling (+20 more)

### Community 20 - "create_weapons: Creating new weapons guide"
Cohesion: 0.15
Nodes (27): .bmodel is plain text Techne style code (earlier binary note was wrong), addChild is vanilla direction: parent.addChild(child) (old note said reversed; corrected 2026-10-10), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path) (+19 more)

### Community 21 - "InfectedVariants: InfectedVariants"
Cohesion: 0.12
Nodes (7): cpw.mods.fml.common.eventhandler.SubscribeEvent, InfectedVariants, Variant, ItemStack, DevPregen, ServerTickEvent, net.minecraftforge.event.entity.EntityJoinWorldEvent

### Community 22 - "Deci: net.minecraft.item.Item"
Cohesion: 0.11
Nodes (4): Magazines, NewSights, net.minecraft.item.Item, Our own sight attachments (deci:<name>, sights.py)

### Community 23 - "StreetPlan: StreetPlan"
Cohesion: 0.11
Nodes (5): Shape, ZoneKind, StreetPlan, StreetProps, City engine files after the 2026-10-09 split: LcCity layout + StreetPlan, BuildingPlan, LotPlan, EdgePlan, StreetProps

### Community 24 - "NpcShots: NpcShots"
Cohesion: 0.16
Nodes (8): NpcKind, NpcShots, net.minecraft.entity.ai.attributes.IAttributeInstance, net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, net.minecraft.util.MovingObjectPosition, net.minecraft.util.Vec3, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 25 - "NpcTest: .batches()"
Cohesion: 0.20
Nodes (4): Item, NpcLoadoutsAccess, NpcTest, net.minecraft.entity.player.EntityPlayerMP

### Community 26 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.12
Nodes (23): CLAUDE.md project guide, dev/libs/Decimation-base.jar (patched jar minus our classes), Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), dev/ RetroFuturaGradle workspace (GTNH ExampleMod1.7.10 template), Graph update workflow (graph_update.py prepare/finish, one agent per chunk), In place jar entry update (zip -u, never rebuild), Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers) (+15 more)

### Community 27 - "building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status"
Cohesion: 0.17
Nodes (23): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), BlockProp facing transform from deobf PropRenderer (rotate 180 about x, metadata % 4 * 90 about y, then extra rotation; toward the road: road west 4, east 2, north 5, south 3), Car wreck model axis: long axis along x at 0 degrees (confirmed in game 2026-10-07), Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections), Prop facing rule (PropRenderer rotation = metadata % 4 * 90; north south street 5/3, east west 4/2) (+15 more)

### Community 28 - "NpcLoadouts: NpcLoadouts"
Cohesion: 0.16
Nodes (6): ItemStack, World, NpcLoadouts, EntityConstructing, LivingAttackEvent, LivingUpdateEvent

### Community 29 - "StoreyPlan: StoreyPlan"
Cohesion: 0.18
Nodes (3): ApartmentPlanner, OfficePlanner, StoreyPlan

### Community 30 - "Surfaces: net.minecraft.block.Block"
Cohesion: 0.16
Nodes (4): Interior, Surfaces, B, net.minecraft.block.Block

### Community 31 - "SchematicPlan: SchematicPlan"
Cohesion: 0.10
Nodes (8): Schematic, SchematicPlan, ZoneKind, MILITARY, POLICE, SAFEZONE, City districts: wasteland next to military sectors, current beta weighted, SchematicPlan prop facing FIXED / RANDOM only

### Community 32 - "Plan: Plan"
Cohesion: 0.08
Nodes (5): Graded, Plan, Foundation down to the ground (max 12, Plan.foundation): stone brick plinth for buildings, dirt for schematics, Terrain blending (round 2 step 1, done v0.13.0, headless verified, not yet seen in game), Plan

### Community 33 - "ZoneStore: ZoneStore"
Cohesion: 0.11
Nodes (12): DecimationWorldType, Override, Load, ObjectZone, ObjectZoneList, ServerTickEvent, ZoneStore, net.decimation.mod.server.zones.ObjectZoneList (+4 more)

### Community 34 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.14
Nodes (19): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Schematic, Floor counts (shop 1 to 2, apartment 2 to 9, office 3 to 20, about 15% towers), Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), City lots (4 lots of 26x26 per cell: 10% empty, apartment 2..9, office 3..20, shop 1..2), Large schematics (up to 120x120, per site chance, placed inside the site) (+11 more)

### Community 35 - "bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.13
Nodes (19): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, PatchPropCulling step 3 (v0.28.6): line of sight answers cached until the player or entity moves 0.3 blocks (props 1.0..1.3 s, entities 0.15..0.18 s); prop renderer share 21% -> 9%, tools/patches/PatchSwing.java, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Hand written Forge/Minecraft stub classes, Javassist bytecode patcher (+11 more)

### Community 36 - "FurnitureSets: com.google.gson.JsonObject"
Cohesion: 0.15
Nodes (5): com.google.gson.JsonArray, com.google.gson.JsonObject, Condition, FurnitureSet, FurnitureSets

### Community 38 - "CameraViews: CameraViews"
Cohesion: 0.18
Nodes (3): CameraViews, EntityPlayerMP, Entry

### Community 39 - "bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)"
Cohesion: 0.12
Nodes (19): Baking static props into chunk meshes (prop textures into the block atlas, both model formats to quads): full fix for open views, not started, Launch waited 5 s for the dead Decimation backend (kryonet hardcoded 5000 ms); PatchBackend, Prop dense FPS drop: line of sight ray casts 76% of prop rendering, cached in v0.28.6; model drawing remains, Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains), Prop fps measured (-Pprops): 225 props in view 12..14 fps vs empty 28..29; 76% of PropRenderer time in canSeeTileEntity ray casts, PatchPropCulling step 4: render distance by prop size (deciworldgen_props.cfg, small / medium / large; default 64 = vanilla), Obfuscated Decimation names in our code go through fixes/Deci, -Pjfr Java Flight Recorder CPU profile (run/client/profile.jfr, read with jfr print --json; client thread is main in dev) (+11 more)

### Community 40 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.18
Nodes (19): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), Load, MultiblockRepairHandler (+11 more)

### Community 41 - "DeciBiome: DeciBiome"
Cohesion: 0.13
Nodes (6): DeadTree, Override, DeciBiome, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 42 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.17
Nodes (21): Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Procedural building design doc (city blocks, Building v2), Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Lots and yards (2 free on the sides, 3 behind, 6 to 9 front yard; offices and shops car park with nose-in wrecks, apartments gravel path and lawn), 1 block margin ring outside walls for exterior vines (SKIP elsewhere), Minecraft scale (storey 4 blocks, corridor 2 wide, doors 1x2, units 5 to 8 deep) (+13 more)

### Community 43 - "?: Sectors.java"
Cohesion: 0.32
Nodes (4): Building, net.decimation.mod.server.zones.ObjectZone, net.decimation.worldgen.building.Building, ObjectZone

### Community 44 - "Capture: Capture"
Cohesion: 0.21
Nodes (3): Capture, JsonArray, JsonObject

### Community 45 - "performance: Performance doc: tools (gunperf, census, perfcheck, JFR via jcmd), findings, checklist"
Cohesion: 0.13
Nodes (15): CityViewTest, Angelica in the dev client (-Pangelica, Decimation jar without its unused fastutil): starts but draws no far terrain on this Mac (GL 2.1), not usable, Lag checklist: perfcheck log, perfcheck world, census growth, JFR, gunperf vs control, fix and measure again, City view lag: first views were wrong (arena floor / clouds); valid view: props cost ~18% (34 vs 41 fps), the rest is city blocks and Mac heat, Dev world entity explosion: region slot (-1,-6) held chunk (-4,-1), entities doubled each save / load to 40 730 with a Forge stack trace per tick; fixed with fixchunk + census kill, Guns are not the cause: dropped / held fps of Uzi, UMP45, MAC-10, UMP9 equal to baseline, Decimation infected ran a full path search every tick on server and client plus a 40x40 horde scan: ~60% of both threads; PatchInfectedAI: fps 21 -> 35, Java 25 + lwjgl3ify still gets an OpenGL 2.1 context on this Mac: Angelica cannot help on macOS; Prism expected the same (+7 more)

### Community 46 - "gun_style_guide: Decimation gun style guide: study of all 98 shipped guns (look and construction rules for our own guns)"
Cohesion: 0.12
Nodes (20): Gun animation templates: Fire 2 frames slide only, Reload1 57 frames keyframes every 5 (SWITCH 20, LOAD 40, TRYBOLT 50), Rack 19 frames Hand 1, SlideBack static in 92 of 98, Gun attachments: NBT sightAttach / barrelAttach / gripAttach, category match or all, fixed offsets in model units, barrel follows flamePos, foregrip misplaced even on M4A4 [inferred], Construction rules: small shape boxes, layered receiver panels, grooves as thin raised plates, octagons from 3 parts per slice, skewed segments for grips and curved mags (addChild), chamfered edges, thin features thin, docs/references/decimation_guns.tsv: per gun parts, shaped %, rotated %, length / height / width, texture size, Where gun detail goes: receiver 30 to 70% of length holds half the parts, 3 of 4 parts in the top two fifths; first person shows top, right side, rear, First person gun look (dev test gunview): seen from rear, top, right; short guns are their rear section; MAC-10 stock plate fills the view, Gun icon style: 32x32 side silhouette, muzzle right, dark tones, 1 px black outline; make from the model side render, Gun numbers: 99% shape boxes, 72% declared 1x1x1, real part median 0.2 x 0.4 x 0.9, offsets median 0.35, UV step 8, texture brightness median 49; smg ~105 parts, rifle ~174, pistol ~83; about 31 mm per unit [inferred] (+12 more)

### Community 47 - "sights: sights.py"
Cohesion: 0.15
Nodes (19): add(), acog(), eotech(), oct_y(), oct_z(), Gun space -> attachment file space (minus renderAttachments' translate)., 68 MOA ring, centre dot, four ticks across the ring (photo 62), red. Drawn…, TA11 BDC (photo 59): thin cross, thick side and bottom posts with // marks,… (+11 more)

### Community 48 - "SupplyDropScheduler: .onServerTick()"
Cohesion: 0.15
Nodes (6): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, ServerTickEvent, SupplyDropScheduler, EntityPlayer, net.minecraft.server.MinecraftServer, net.minecraftforge.event.entity.player.AttackEntityEvent

### Community 49 - "CLAUDE: Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags)"
Cohesion: 0.16
Nodes (16): Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (devtest.py first choice, servertest, worldcheck, worlddiff, autotest flags), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Missing furniture uses vanilla stand-ins (bed, stairs + carpet sofa, quartz stairs / cauldron toilet, cauldron sink and bath, BlockElectricBoxBin or iron block fridge, slab counters with trapdoor cupboards, bookshelf, flower pot) (+8 more)

### Community 50 - "CLAUDE: Javassist bytecode patcher"
Cohesion: 0.12
Nodes (18): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CFR decompiler, Decimation.jar (obfuscated Forge 1.7.10 mod jar), deobf/ readable reference tree (decompiled with readable names, read only), Map file case trap (p_deci_aK.tsv equals p_deci_ak.tsv on APFS), Naming source priority (g*, p_*, r_*, m_*, then zz_auto.tsv), rtk hook drops grep/find flags (use Python os.walk) (+10 more)

### Community 51 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.16
Nodes (18): BlockRegistry (deci.aD.c / g), Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Door decay: 25 to 50% missing, a few left open (meta bit 4), one barricaded in the most decayed buildings, Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta), Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only, Decals (blood splats, hazard signs, notes, framed picture, flags, warning signs; flat upright pictures, wall face by meta [not verified]), Doors use vanilla door metadata (copy BlockDoor without extending it): lower half 0..3 = west / north / east / south edge, upper half 8; place both halves or the door removes itself (+10 more)

### Community 52 - "Furnisher: Furnisher"
Cohesion: 0.22
Nodes (4): Facing, Furnisher, Entry, net.decimation.worldgen.sets.FurnitureSet

### Community 53 - "BuildingPlan: BuildingPlan"
Cohesion: 0.14
Nodes (4): BuildingPlan, Building, Shape, ZoneKind

### Community 54 - "LcContent: LcContent"
Cohesion: 0.14
Nodes (4): Building, LcContent, Shape, net.decimation.worldgen.Schematic

### Community 55 - "floorplan: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.21
Nodes (16): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type, Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Principle: circulation (free 1 block path door to door, no furniture on a doorway cell or the cell in front; reachability >= 95% per storey), Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md (+8 more)

### Community 56 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.20
Nodes (17): Bug: zones never active in singleplayer (partial fix), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), EntitySpawnZoneHandler (deci.aK.d), PlayerZoneTickHandler (deci.aK.m), SafezoneAttackHandler / FriendlyFireHandler (deci.aK.g, i), ServerCommandRegistrar (deci.aK.o) (+9 more)

### Community 57 - "furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.12
Nodes (17): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, DeceasedCraft interiors fully authored per storey, no procedural rooms, Furniture sets doc: data driven JSON furniture groups, user editable, In-game capture: pos1/pos2, capture set/part, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, known.txt: unedited old built-in copies are updated (tools/asset_hashes.py), Named palettes and weighted styles for sets (base / style keys), Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window (+9 more)

### Community 58 - "LcCity: LcCity"
Cohesion: 0.29
Nodes (3): Building, LcCity, Highways

### Community 59 - "LotPlan: LotPlan"
Cohesion: 0.12
Nodes (4): Shape, ZoneKind, LotPlan, FixedBase

### Community 60 - "new_feature: MAC-10 v2 (v0.37.0): 102 parts, 9.35 x 8.1 x 1.5, proportions from the user's side photo, sights at the aim centre, checked in game (gunview, gun)"
Cohesion: 0.17
Nodes (16): Skill casebook case 11 and lesson 14: verify performance for every gun; the 1 fps report was a corrupt chunk and infected pathing, Skill casebook case 9: closing the gaps after acceptance (method, numbers, checks), Project skill decimation-gun: end to end gun workflow (read docs, references, part plan, anchors, build, compare with study.py, register, gunview test, finish), CLAUDE.md v0.36.0: MAC-10 pipeline, our items register as deciworldgen:<name>, BModelBox facts: corner array order and added offsets, box UV face layout, Decimation textures 2 px per unit, 32x32 icons, study.py gaps: ours vs Decimation guns of the category, metric by metric (median, q10..q90, outside marks), incl. part sizes relative to length, Our own guns pipeline (gun_model_spec section 6): tools/guns spec to .bmodel / texture / icon / .anib, bbmcp previews, NewGuns registration (newGun, newMagazine, addLootLike, useGunSounds), dev test mode gun, gunmodel.py v2: hexahedron parts (part, inset, shift, mirror, octagon), 1x1x1 declared with corner offsets, flat tones, textureWidth 512 UV step 8, icon from the study.py render, anib with slide_names (+8 more)

### Community 61 - "MilitarySpawner: net.minecraft.entity.player.EntityPlayer"
Cohesion: 0.17
Nodes (5): Configuration, MilitarySpawner, EntityLiving, net.minecraft.entity.player.EntityPlayer, ServerTickEvent

### Community 62 - "worldgen_architecture: Worldgen architecture v3 draft (layers, assets, size classes, capture tool)"
Cohesion: 0.13
Nodes (16): Builders: separate ceiling tiles with light panel grid and vents, Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161), Builders: decay as dirt/leaves/water/cracked glass on intact shells, Builders: furniture in rows and islands (waiting rows, cubicles, shelf aisles), Autotest -Pstudy camera mode for reference maps, Builders: two tone WallOffice walls (dado bottom + top), Audit shots v0.23: 6 high offices and shops, Worldgen code map: building package parts (Shell, StoreyPlan, planners, Furnisher, Surfaces, Interior, Ruins, Yard) (+8 more)

### Community 63 - "lcstudy: lcstudy.py"
Cohesion: 0.23
Nodes (11): category(), districts(), main(), building name -> {city style: weight share}, structure_summary(), category(), main(), Pack (+3 more)

### Community 64 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.19
Nodes (12): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a) (+4 more)

### Community 65 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 66 - "worldcheck: worldcheck.py"
Cohesion: 0.21
Nodes (10): Bug: structures built on ocean floor, Underwater placement fix (v0.7.0), tools/worldcheck.py region file inspection, main(), _meta(), Block metadata (0..15) at a position, None if the chunk is missing., Block name -> numeric id, from the FML id map in level.dat., read_nbt() (+2 more)

### Community 67 - "edgescan: gradescan.py"
Cohesion: 0.24
Nodes (10): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), is_city(), jrandom_float(), region_sector(), s64(), main(), props(), surface() (+2 more)

### Community 68 - "?: net.minecraft.entity.EntityLiving"
Cohesion: 0.27
Nodes (7): EntityPlayerMP, ItemStack, net.decimation.fixes.MilitarySpawner, net.decimation.fixes.NpcKind, net.decimation.fixes.NpcLoadouts, net.minecraft.entity.EntityLiving, net.minecraft.entity.passive.EntityPig

### Community 70 - "BiomeMap: BiomeMap"
Cohesion: 0.21
Nodes (6): Sectors, BiomeMap, NoiseGeneratorSimplex, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors), Sector grid (16x16 chunks: WILD 40%, CIV 25%, CITY 15%, MIL 20%)

### Community 71 - "?: DeciGenLayer"
Cohesion: 0.18
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 72 - "hwmap: hwmap.py"
Cohesion: 0.35
Nodes (13): at(), city(), clear(), hl(), line(), link(), onL(), onlink() (+5 more)

### Community 73 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.22
Nodes (12): Bug: city edge ramp missed its outer columns (fixed v0.24.4, populate scans cells within EDGE), City engine: Lost Cities style cities from converted DeceasedCraft content, City edge ramp: 24 wide, rounded corners, nearest cell owns a column, wobbled contours, Skipped giant buildings: casino 276 high, oasis condo top above 250, laboratory 90 deep cellars, City engine open items: Lost Cities bridges and rail unused, giant buildings too tall, rotation only data variants, chests became wood crates, City street dressing: sidewalks, centre lines, lamps, benches, wrecks, City superblocks: 2x2 cells, 7x7 building chunks, landmark towers, User review 0.24.4: love it for oneshot progress; highways chosen next (+4 more)

### Community 74 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 75 - "DevTestLive: DevTestLive"
Cohesion: 0.19
Nodes (5): Live dev test mode: tools/devtest.py --live [--swap] [--stop]; game stays open (-Plive, DevTestLive on 127.0.0.1:25599), ready in about 26 s, reruns only cost their own time; hotswap for method bodies, Command, DevTestLive, Recently done: live dev test mode, java.net.Socket

### Community 76 - "hk416: hk416.py"
Cohesion: 0.19
Nodes (11): Lesson 16: base parts reading past textureHeight wrap (M4A4 rear sight), Variant texture trap: M4A4 rear sight reads rows past textureHeight 32, black_part(), build(), dropped(), lower_bottom(), y of the lower panel's bottom edge at x (the wedge's front face is collapsed to…, The M4A4 parts our HK416 replaces (by position, measured on the M4A4:… (+3 more)

### Community 78 - "anvil118: anvil118.py"
Cohesion: 0.24
Nodes (10): chunk_biomes(), chunk_blocks(), chunks(), main(), Palette, {section Y: biomes[4,4,4] (y, z, x) global ids}., Entries packed without spanning longs (1.16+)., Global name -> id for a whole survey. (+2 more)

### Community 79 - "study: study.py"
Cohesion: 0.24
Nodes (11): perspective(), Gaussian elimination, a is n x n, b n., PIL PERSPECTIVE coefficients mapping output (screen) points to texture points., split: every part one flat colour (shows how the shapes are cut into parts)., A point in this part's space to model space (through the parents of addChild)., render(), rot_x(), rot_y() (+3 more)

### Community 80 - "CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)"
Cohesion: 0.20
Nodes (9): run/client/devtest/results.txt: one line per value, PASS / FAIL with the expectation, exit code 1 on a FAIL, tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09), Live loop: hotswap code, reload sets, rebuild in place, no restart per change, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), Roadmap: code and tools, Live dev test mode [idea]: keep the dev game open and start test modes over a localhost port, Split LcCity (1225 lines) and StructureGenerator (861 lines) into street, building, lot and edge plan files; old procedural city apart, jdb() (+1 more)

### Community 81 - "city_engine: .decor()"
Cohesion: 0.20
Nodes (8): v0.25.0 highways, v0.26.0 parks / street scenes / fronts, v0.27.0 district Lost Cities street parts, Shape, City decor: parks on open lots, street scenes (pack fountains), building fronts on the street side, Fronts: building chunk beside a straight street gets a district front part (FRONT_CHANCE 0.5), resolved lazily at write time, Parks: open lots LOT_CHANCE 10% of building chunks carry a district park part one layer up, Road paint: refueled mod decals converted by lcpack paint() into Road_CenterLine and quartz zebra; odd turns flip line meta, District street parts: LC street parts by connection count, road paint to painted road blocks, Street scenes: DeceasedCraft 'fountains' (bus, ambulance, roadblock, trash) in 6% of straight street chunks

### Community 84 - "SealedCaves: SealedCaves"
Cohesion: 0.26
Nodes (6): Caves, Override, Ravines, SealedCaves, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 85 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.38
Nodes (4): Override, WorldGenCommand, net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender

### Community 86 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.18
Nodes (11): Bug (fixed v0.30.2): full military armor made NPC gunfire almost harmless: armor multiplies per piece (x0.149 marine set) and vanilla hit cooldown dropped group hits; NPC shots are direct damage, not bullets, v0.30.4: NPC hit cooldown 0.25 s (npcHitCooldownTicks 5), v0.30.3: vanilla hit cooldown kept for NPC hits (npcHitsSkipCooldown false), x5 stays, Fix v0.30.2: NPC gun hits on players x npcDamageToPlayer (5) after armor, every NPC hit lands (LivingAttackEvent clears hurtResistantTime); bare 10 hp, marine set 1.49, 5 hits 7.47, Done v0.30.3: NPC shots traced with spread per tier, stopped by walls, impact particles, tracer always visible along the real line (PatchTracer v2 shotHook + NpcShots), CLAUDE.md v0.30.2 note: NPC hits x5, no hit cooldown, mech swap, CLAUDE.md v0.30.3: traced NPC shots, PatchTracer v2 then PatchFactions, test NPCs on a block, Balance: NPC gunfire on the player x5 after armor, every hit lands (v0.30.2) (+3 more)

### Community 87 - "DevAutoTest: DevAutoTest"
Cohesion: 0.35
Nodes (4): ClientTickEvent, Command, DevAutoTest, net.decimation.worldgen.devtest.DevTestMode

### Community 88 - "FireRateTest: FireRateTest"
Cohesion: 0.29
Nodes (4): FireRateTest, Entity, EntityLivingBase, EntityPig

### Community 89 - "bug: worlddiff.py"
Cohesion: 0.24
Nodes (9): Bug: building base height depends on chunk generation order (Slices.decideBase), Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), VehicleHitHandler, Open bug 4: arrows still pick up empty vehicles (punching fixed v0.8.1), Open bug 3: building base height depends on chunk generation order (low impact), Roadmap: open bugs, arrays() (+1 more)

### Community 90 - "interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 91 - "SKILL: decimation-gun skill revision casebook: 8 cases (boxy look, aim centre, suppressor floating, suppressor low / threads, gradation, stock in sight picture, icon halo, test traps) with symptom, cause, code location, fix, check, evidence"
Cohesion: 0.22
Nodes (10): Case 15: MAC-10 parts not seated, contact check cleanup (v0.39.3), Lesson 15: every part sits on its host, sights like the real gun from behind, Lesson 17: iron sights in the defaultScopeModel group, Lesson 18: shorter variant scaled from the base gun, Lesson 19: sights must sit on the rail (sightfit.py), Lesson 20: building a new sight attachment, Lesson 21: reticles live in the sight (sway), pixel art at drawn size, Lesson 22: keep accepted sight mechanics (+2 more)

### Community 92 - "mk18: mk18.py"
Cohesion: 0.24
Nodes (8): Case 18: Mk18 TS stock, EOTech 558, ACOG TA11 (v0.41.0), build(), dropped(), lower_edge(), px(), y of the stock's bottom edge (the lower strut) at model x: photo (480, 1470) to…, The M4A4 parts our Mk18 replaces (by position, measured on the M4A4:…, icon()

### Community 93 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.31
Nodes (3): DeciBiome, DecimationBiomes, net.minecraft.world.biome.BiomeGenBase

### Community 94 - "Deci: .complete()"
Cohesion: 0.27
Nodes (3): World, net.minecraft.tileentity.TileEntity, TileEntity

### Community 95 - "Palettes: Palettes"
Cohesion: 0.33
Nodes (3): Palettes, Style, Entry

### Community 96 - "GunPerfTest: GunPerfTest"
Cohesion: 0.36
Nodes (3): GunPerfTest, EntityItem, net.minecraft.entity.item.EntityItem

### Community 97 - "StructureData: StructureData"
Cohesion: 0.27
Nodes (4): Override, StructureData, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 98 - "study: Part"
Cohesion: 0.24
Nodes (6): all_names(), Gun, load(), num(), Part, Our gun if NAME starts with ours: or only exists there, else Decimation's.

### Community 99 - "sightfit: sightfit.py"
Cohesion: 0.31
Nodes (8): Bug: sights float above flat top rifles (v0.40.1), Case 17: sights floating above flat top rifles, bbox(), gap(), main(), rail_top(), Top of the receiver / rail in the sight zone: the highest long part there, iron…, dy > 0: the sights float that much above this gun's rail (they are placed for…

### Community 100 - "VanillaMobs: VanillaMobs"
Cohesion: 0.25
Nodes (4): Trap: vanilla class names are obfuscated in the shipped game, never test by package name (copySpawns fixed), VanillaMobs, No vanilla mobs (v0.33.0) in documentation, No vanilla mobs v0.33.0: vanilla monsters, animals, squid, bats, villagers, golems out of overworld spawn lists and refused on join (old chunks too); config deciworldgen_mobs.cfg; tests tag needed mobs with VanillaMobs.KEEP

### Community 101 - "SKILL: decimation-gun skill revision casebook: 8 cases (boxy look, aim centre, suppressor floating, suppressor low / threads, gradation, stock in sight picture, icon halo, test traps) with symptom, cause, code location, fix, check, evidence"
Cohesion: 0.28
Nodes (9): Skill casebook case 10: UMP9 as a variant of the UMP45 (how, magazine anchor, registration, checks), Skill case 12: UMP9 accepted without revision (variant route), Skill case 13: variants with many replaced parts (drop by position, fit to the kept receiver, colour twins by recolouring), Case 14: HK416 floating stock pieces and 2 pillar sight (v0.39.1), Case 20: verdict, sights accepted (v0.41.1), decimation-gun skill revision casebook: 8 cases (boxy look, aim centre, suppressor floating, suppressor low / threads, gradation, stock in sight picture, icon halo, test traps) with symptom, cause, code location, fix, check, evidence, Variants of Decimation guns: generate locally from the user's jar, their parts minus replaced ones plus ours, git ignored outputs, HK416 and HK416 Tan (v0.39.0): Decimation's M4A4 with our HK handguard, front sight, barrel, flash hider, stock, grip; tan by recolouring; two separate guns (+1 more)

### Community 102 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 103 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 105 - "deceasedcraft_buildings: Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data)"
Cohesion: 0.25
Nodes (9): DeceasedCraft content catalogue: 79 Lost Cities buildings, city parts, apocalypsenow structures, disabled vanilla structures, Lost Cities to 1.7.10 conversion (lc2schem, lctranslate, paste command), DeceasedCraft 79 building types and 5 district city styles, Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data), DeceasedCraft storey is 6 high (4 air) vs our 5, Measured prop footprints (footprint mode, tools/footprint.py), apocalypsenow .nbt structure converter for military and other sites outside cities [not decided], Reserve the real footprint of wide props (bicycles, cars) in placers so they do not overlap (+1 more)

### Community 106 - "study: main()"
Cohesion: 0.28
Nodes (7): label(), main(), metrics(), wedge (two corners merged), cuboid (a plain smaller box), taper (one end face…, The numbers a gun is compared on (docs/gun_style_guide.md, `gaps` command)., shape_kind(), stats()

### Community 107 - "gun_style_guide: Gun aiming: aim mode draws every gun at one fixed place (x 0.5, translate -1 -0.35 0.923), sPos unused in first person; screen centre at the top of the iron sights, y -4.85 to -5.0, z -0.15"
Cohesion: 0.29
Nodes (8): decimation-gun skill revision log: lessons from every user review (v1, v2, v0.37.0, v0.37.1), Aim rule corrected v0.37.2: centre passes the Uzi's rear aperture hole (about -4.65); compare with a Decimation gun in the same shot, Aim sway: headYawSway / dP tilt the gun after mouse moves, so judge aim only steady, flamePos y sits about 0.85 above the bore (Uzi -4.5 over barrel -3.5); barrel attachments hang from it, Gun aiming: aim mode draws every gun at one fixed place (x 0.5, translate -1 -0.35 0.923), sPos unused in first person; screen centre at the top of the iron sights, y -4.85 to -5.0, z -0.15, Gun placement conventions: top at y -5, z centre -0.15, flamePos at muzzle minus 0.1, rhPos / lhPos medians per category, no sPos / mOff / Scale, MAC-10 v0.37.2: flamePos convention, suppressor over the threads on the bore, aperture matched to the Uzi, stock loop lowered, Attachment and aim shots: study.py attach renders, in game hip / aim / NPC with attachments, iron sight aim crops with centre lines

### Community 113 - "devtest: devtest.py"
Cohesion: 0.46
Nodes (7): live_ready(), live_send(), main(), True once the live game answers ping with ready (waits up to `wait` seconds)., One command to the live game; its reply lines (up to END), or None when nobody…, run_live(), sheets()

### Community 114 - "study: .verts()"
Cohesion: 0.36
Nodes (6): contact(), inside(), True when point q lies in the part's (convex) shape, grown by eps., Parts that do not sit on anything. A thin detail (0.15 or less in some axis):…, The 8 corners in model space, indexed like vanilla ModelBox: v7 (0,0,0), v0…, (4 vertices, (u1, v1, u2, v2)) per face, like vanilla ModelBox.

### Community 117 - "interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 119 - "NpcKind: NpcKind"
Cohesion: 0.33
Nodes (5): NpcKind, BANDIT, HAZMAT, SOLDIER, SOVIET

### Community 120 - "FixedBase: FixedBase"
Cohesion: 0.33
Nodes (4): FixedBase, Rules taken from Lost Cities source: street surface at G, ground floor at G, cellars below, stairs at G + 1 toward the higher neighbour, City levels per cell (6 blocks apart), streets at G, stairs parts between levels, Lost Cities: storey parts, buildings, palettes, city styles as data

### Community 121 - "study: with_attachments()"
Cohesion: 0.33
Nodes (6): attach_offset(), Where GunItemRenderer.renderAttachments draws an attachment, in model units (GL…, (gun, sight) -> dy from tools/guns/sightfit.py's table (fixes/SightPlacement…, The gun plus attachment models placed like the game does (plus ATTACH_FIX for…, sight_offsets(), with_attachments()

### Community 122 - "perfcheck: perfcheck.py"
Cohesion: 0.60
Nodes (5): chunks(), fix_chunk(), log_report(), main(), world_report()

### Community 123 - "PatchIronSights: Case 16: Mk18 Mod 1 and iron sights hidden under a sight (v0.40.0)"
Cohesion: 0.40
Nodes (3): Case 16: Mk18 Mod 1 and iron sights hidden under a sight (v0.40.0), defaultScopeModel parts hidden while a sight is attached (19 guns), PatchIronSights

### Community 125 - "new_feature: NPC auto fire v0.32.0: bursts at the gun's rate, spread grows per shot (recoilSpread 0.35), pause after a burst"
Cohesion: 0.50
Nodes (4): CLAUDE.md v0.32.0 note and the shootAt test trap, Balance v0.32.0: elite, bursts, reloads, NPC auto fire v0.32.0: bursts at the gun's rate, spread grows per shot (recoilSpread 0.35), pause after a burst, NPC magazines v0.32.0: fire the gun's magazine (M16 / AK 30, PKM 250), then reload 4 s (reloadTicks 80)

### Community 126 - "CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side"
Cohesion: 0.50
Nodes (4): v0.18.0 apartment rooms and propFacing fix, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

### Community 127 - "gun_style_guide: MAC-10 v2 revision v0.37.1: suppressor flush, gradation, aim checked steady like the Uzi"
Cohesion: 0.50
Nodes (4): Barrel attachment formula meets the muzzle only for flamePos x 12 to 15; Deci.offsetAttachment puts an offset model copy in AttachmentItem.ST per gun (MAC-10 -1.02, +0.14, 0), Gun texture gradation: Decimation 15 to 31 part tones, faces 4.7 to 5.7 apart, texel noise 2 to 2.7, top lighter; gunmodel.paint reproduces it, Texture rules: one flat dark tone per part island with faint noise, no painted detail; measured metal 24 to 64 grey, AK74 wood 41,25,22, MAC-10 v2 revision v0.37.1: suppressor flush, gradation, aim checked steady like the Uzi

### Community 128 - "bug: Bug: NPC machine guns fire at double rate (v0.39.2)"
Cohesion: 0.67
Nodes (3): Bug: NPC machine guns fire at double rate (v0.39.2), Decimation runs guns at 1.3x listed rpm (GunStats), maxBurstRpm 600 (deciworldgen_npc.cfg npc_fire)

## Ambiguous Edges - Review These
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` → `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Genuinely new model recipe (needs Techne)` → `addBox passes Y as Z origin (use addShape)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)` → `FML IWorldGenerator per chunk hook`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **169 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+164 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **68 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` and `Recurring root cause: integrated server reports side CLIENT`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Genuinely new model recipe (needs Techne)` and `addBox passes Y as Z origin (use addShape)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes (fixed v0.28.0..0.28.4)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)` and `FML IWorldGenerator per chunk hook`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `StructureGenerator` connect `StructureGenerator: net.minecraft.world.World` to `worldgen: World generation doc (deciworldgen)`, `BiomeMap: BiomeMap`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `?: Sectors.java`, `Highways: Highways`, `new_feature: Current state and pending decisions`, `CLAUDE: tools/devtest.py: dev test modes in ONE game launch, results file, contact sheet per mode (first choice since 2026-10-09)`, `DecimationWorldGen: DecimationWorldGen`, `LcCity: LcCity`, `building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status`, `Surfaces: net.minecraft.block.Block`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `Deci` connect `Deci: Deci` to `ServerChecks: ServerChecks`, `?: net.minecraft.entity.EntityLiving`, `bug: Report: FPS drop in prop dense areas (line of sight cached v0.28.6, model drawing remains)`, `new_feature: Feature tracker (new_feature.md)`, `NpcEgg: cpw.mods.fml.relauncher.SideOnly`, `IronSights: net.minecraft.item.ItemStack`, `SupplyDropScheduler: .onServerTick()`, `InfectedVariants: InfectedVariants`, `Deci: net.minecraft.item.Item`, `NpcTest: .batches()`, `MilitarySpawner: net.minecraft.entity.player.EntityPlayer`, `Deci: .complete()`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._