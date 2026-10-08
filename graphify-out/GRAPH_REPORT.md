# Graph Report - .  (2026-10-09)

## Corpus Check
- 21 files · ~136,279 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1531 nodes · 3176 edges · 114 communities (67 shown, 47 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 433 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- StructureGenerator: net.minecraft.world.World
- DevAutoTest: DevAutoTest
- SchematicPlan: .init()
- lctranslate: lctranslate.py
- worldcheck: worldcheck.py
- DeciBiome: DeciBiome
- new_feature: Current state and pending decisions
- Test Schematic Builder
- Building: Building
- Ruins: net.minecraft.block.Block
- Deci: Deci
- building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status
- architecture: Obfuscation map (package to meaning)
- ScopeZoom: ScopeZoom
- architecture: ServerProxy (deci.a.e, dedicated only)
- create_weapons: Creating new weapons guide
- CLAUDE: decimation-singleplayer README (public repo overview)
- StoreyPlan: StoreyPlan
- decimation_maps: Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161)
- new_feature: Feature tracker (new_feature.md)
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- LcCity: EdgePlan
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- worldgen: World generation doc (deciworldgen)
- Plan: Plan
- LcCity: LcCity
- Deci: Entity
- bug: Bug tracker (bug.md)
- Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)
- Capture: Capture
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- LcCity: net.minecraft.block.Block
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- Deci: Player
- WorldGenCommand: WorldGenCommand
- Shell: .unit()
- BiomeMap: BiomeMap
- LcCity: LotPlan
- LcContent: LcContent
- lcstudy: dcinventory.py
- ?: BottlecapHandler.java
- bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- Name Mapping Applier
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- Condition: com.google.gson.JsonObject
- StructureData: StructureData
- Facing: .setMeta()
- SupplyDropScheduler: SupplyDropScheduler
- CLAUDE: tools/build.py real javac pipeline
- furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable
- LcCity: BuildingPlan
- anvil118: anvil118.py
- AssetDir: AssetDir
- Deci: .complete()
- FurnitureSets: FurnitureSet
- Palettes: com.google.gson.JsonObject
- building_design: Procedural building design doc (city blocks, Building v2)
- interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- Heuristic Auto Namer
- Furnisher: Furnisher
- hwmap: hwmap.py
- graph_update: graph_update.py
- interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic
- CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side
- bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)
- asset_hashes: asset_hashes.py
- footprint: footprint.py
- PatchBackend: PatchBackend
- PatchScope: PatchScope
- PatchTracer: PatchTracer
- bug: Launch waited 5 s for the dead Decimation backend (kryonet hardcoded 5000 ms); PatchBackend
- bug: Converted buildings use LED lamp blocks as floor (open)
- bug: Prop dense FPS drop: line of sight ray casts 76% of prop rendering, cached in v0.28.6; model drawing remains
- ?: CityDistrict
- Weather Type Id Bug
- ?: JsonObject
- ?: a
- ?: a
- ?: Entry
- ?: FurnitureSet
- ?: a
- ?: Entry
- ?: Plan
- ?: a
- ?: a
- ?: FurnitureSet
- ?: a
- ?: Override
- ?: World
- ?: a
- interior_spec: propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N
- ?: EntityLiving
- ?: LargeSites
- ?: Load
- ?: net.decimation.worldgen.building.Building
- ?: net.decimation.worldgen.Plan
- ?: net.decimation.worldgen.StructureGenerator
- ?: net.decimation.worldgen.StructureGenerator.Sub
- ?: net.minecraftforge.client.event.FOVUpdateEvent
- ?: ObjectZoneList
- ?: Props
- ?: Schematic
- ?: ServerTickEvent
- ?: Sub

## God Nodes (most connected - your core abstractions)
1. `Building` - 57 edges
2. `Deci` - 52 edges
3. `StructureGenerator` - 50 edges
4. `DevAutoTest` - 49 edges
5. `LcCity` - 38 edges
6. `World generation doc (deciworldgen)` - 27 edges
7. `Current state and pending decisions` - 27 edges
8. `EdgePlan` - 26 edges
9. `Bug tracker (bug.md)` - 26 edges
10. `Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside)` --semantically_similar_to--> `survivor_camp()`  [INFERRED] [semantically similar]
  docs/interior_spec.md → tools/make_test_schematics.py
- `ZoneStore (per world deciworldgen_zones.json)` --references--> `ZoneStore`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/ZoneStore.java
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `Small schematics (max 24x24, one pass, per cell sector chance)` --references--> `Rotation`  [INFERRED]
  docs/worldgen.md → dev/src/main/java/net/decimation/worldgen/Rotation.java
- `Rotation.java metadata rotation table` --references--> `Rotation`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/Rotation.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Upper storey reachability fix v0.16.1 (floorplan scan, unsupported ladder, collapse exemption, Building.ladder / collapsed)** — bug_upper_storeys_unreachable, bug_ladder_unsupported_wall_cell, bug_ladder_core_collapse_exemption, tools_floorplan [EXTRACTED 1.00]
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Visual evidence memory (autotest screenshot modes, docs/shots, written shots index, prop catalogue)** — claude_visual_evidence_rule, claude_autotest_screenshot_modes, docs_shots_index_shots_index, docs_prop_catalogue_prop_catalogue_doc, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_servegalleryview [INFERRED 0.85]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]
- **Interior spec layered build (structure, surfaces, room programs, story / decay, exterior, in work order)** — docs_interior_spec_principle_layer_order, docs_interior_spec_doors, docs_interior_spec_surfaces, docs_interior_spec_apartment_unit_program, docs_interior_spec_office_storey_programs, docs_interior_spec_shop_program, docs_interior_spec_story_decay_layer, docs_interior_spec_exterior, docs_interior_spec_order_of_work [EXTRACTED 1.00]
- **Step 4d.1 doors and low debris (spec, code, tracker, release note, audit shots)** — docs_interior_spec_step1_doors_low_debris, claude_v0_17_0_doors_low_debris, new_feature_step_4d1_doors_debris, docs_shots_index_audit_v0_17 [INFERRED 0.85]
- **Verification loop for every building change (floor plans, audit shots, scans, autotest, written findings)** — docs_interior_spec_verification, tools_floorplan, tools_wallscan, tools_gradescan, tools_multiscan, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_serveauditview, docs_building_audit_building_audit_doc, docs_shots_index_shots_index [EXTRACTED 1.00]
- **Building quality audit toolchain (floor plans with reachability, audit screenshots, view cells, indexed results)** — docs_building_audit_audit_method, tools_floorplan, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_serveauditview, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_pickauditbuildings, docs_shots_index_audit_v0_16, docs_shots_index_plans_v0_16 [INFERRED 0.85]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Whole multiblock generation (v0.16.0): slice writer and small placer call complete after writing, Decimation helper forms the box, multiscan checks** — dev_src_main_java_net_decimation_worldgen_slices_slices_write, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_place, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler_complete, docs_worldgen_form_multiblock_helper, docs_worldgen_complete_after_slice_rule, tools_multiscan [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_carmeta, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Decimation world type biome pipeline (TerrainEvents swaps GenLayers, DeciGenLayer reads BiomeMap, BiomeMap uses shared Sectors, DecimationBiomes defines the biomes)** — dev_src_main_java_net_decimation_worldgen_terrain_terrainevents_terrainevents, dev_src_main_java_net_decimation_worldgen_terrain_decigenlayer_decigenlayer, dev_src_main_java_net_decimation_worldgen_terrain_biomemap_biomemap, dev_src_main_java_net_decimation_worldgen_sectors_sectors, dev_src_main_java_net_decimation_worldgen_terrain_decimationbiomes_decimationbiomes, dev_src_main_java_net_decimation_worldgen_terrain_decimationworldtype_decimationworldtype [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]

## Communities (114 total, 47 thin omitted)

### Community 0 - "StructureGenerator: net.minecraft.world.World"
Cohesion: 0.06
Nodes (19): Bug: building base height depends on chunk generation order (Slices.decideBase), cpw.mods.fml.common.IWorldGenerator, Yard, Highways, Schematic, LargeSites, Plan, Slices (+11 more)

### Community 1 - "DevAutoTest: DevAutoTest"
Cohesion: 0.08
Nodes (21): Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, ClientTickEvent, DevAutoTest, EntityLiving, EntityPlayer, ItemStack, ServerTickEvent (+13 more)

### Community 2 - "SchematicPlan: .init()"
Cohesion: 0.05
Nodes (22): Worldgen uses ZoneKind instead of Decimation's obfuscated zone enum; Deci.zoneType converts, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), ZoneSpawnHandler, DecimationWorldGen, CityDistrict (+14 more)

### Community 3 - "lctranslate: lctranslate.py"
Cohesion: 0.07
Nodes (36): building_columns(), main(), matches(), Pack, part_slices(), (layers bottom up, ground layer index): every floor picked like Lost Cities…, Lost Cities part conditions for one floor (cellars are negative)., tag() (+28 more)

### Community 4 - "worldcheck: worldcheck.py"
Cohesion: 0.08
Nodes (36): Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), City edge ramp: 24 wide, rounded corners, nearest cell owns a column, wobbled contours, Principle: circulation (free 1 block path door to door, no furniture on a doorway cell or the cell in front; reachability >= 95% per storey), Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md, plans_v0.16 floor plans (tools/floorplan.py, 14 px per block, letter per prop) (+28 more)

### Community 5 - "DeciBiome: DeciBiome"
Cohesion: 0.07
Nodes (19): cpw.mods.fml.relauncher.SideOnly, DeadTree, Override, DeciBiome, Override, DecimationBiomes, Caves, Override (+11 more)

### Community 6 - "new_feature: Current state and pending decisions"
Cohesion: 0.07
Nodes (27): Bug: armor buff ignores NPC gunfire (fixed), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), ArmorGunfireHandler, Rotation (+19 more)

### Community 7 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 8 - "Building: Building"
Cohesion: 0.09
Nodes (13): Building, Props, CityDistrict, CityDistrict.plan lot placement (2 block side yard, setback 6..9 on 75% of lots with 9 spare, 3 behind, random stream unchanged), Pure function of seed and coordinates (no cross chunk state), Furnisher, Interior, net.decimation.worldgen.Graded (+5 more)

### Community 9 - "Ruins: net.minecraft.block.Block"
Cohesion: 0.15
Nodes (4): Interior, Ruins, Surfaces, net.minecraft.block.Block

### Community 10 - "Deci: Deci"
Cohesion: 0.11
Nodes (7): Obfuscated Decimation names in our code go through fixes/Deci, Deci, ObjectZoneList, net.minecraft.entity.player.EntityPlayer, net.minecraft.item.Item, net.minecraft.item.ItemStack, Vector3f

### Community 11 - "building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status"
Cohesion: 0.14
Nodes (26): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*) (+18 more)

### Community 12 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.12
Nodes (27): Bug: large ammo crate NPE (dead field avk), Bug: ClassCastException deci.a.c to deci.a.e, Bug: loot never worked in singleplayer, Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, ClientProxy (deci.a.c) (+19 more)

### Community 13 - "ScopeZoom: ScopeZoom"
Cohesion: 0.13
Nodes (13): Scope FPS drop: picture in picture second world render, fixed v0.28.0 with view zoom, Configuration, ScopeZoom, Cheap scope done: zoom + see-through glass for reddot / 2x, black sniper overlay from 4x, Field, Method, net.minecraft.client.renderer.EntityRenderer, net.minecraft.util.ResourceLocation (+5 more)

### Community 14 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.11
Nodes (27): Bug: zones never active in singleplayer (partial fix), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), DeathStatsHandler (deci.aK.h), DecimationMod (deci.a.b, @Mod entry) (+19 more)

### Community 15 - "create_weapons: Creating new weapons guide"
Cohesion: 0.16
Nodes (26): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+18 more)

### Community 16 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.10
Nodes (26): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CLAUDE.md project guide, dev/libs/Decimation-base.jar (patched jar minus our classes), Decimation.jar (obfuscated Forge 1.7.10 mod jar), Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), deobf/ readable reference tree (decompiled with readable names, read only) (+18 more)

### Community 17 - "StoreyPlan: StoreyPlan"
Cohesion: 0.16
Nodes (4): ApartmentPlanner, OfficePlanner, StoreyPlan, Worldgen code map: building package parts (Shell, StoreyPlan, planners, Furnisher, Surfaces, Interior, Ruins, Yard)

### Community 18 - "decimation_maps: Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161)"
Cohesion: 0.09
Nodes (26): DeceasedCraft content catalogue: 79 Lost Cities buildings, city parts, apocalypsenow structures, disabled vanilla structures, Lost Cities to 1.7.10 conversion (lc2schem, lctranslate, paste command), DeceasedCraft interiors fully authored per storey, no procedural rooms, DeceasedCraft 79 building types and 5 district city styles, Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data), DeceasedCraft storey is 6 high (4 air) vs our 5, Builders: separate ceiling tiles with light panel grid and vents, Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161) (+18 more)

### Community 19 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.10
Nodes (25): DamageSource split: gunDeci (player) vs human/turret (NPC), Bug: structures built on ocean floor, Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), DeciDamageSources (deci.aD.h), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected) (+17 more)

### Community 20 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.13
Nodes (25): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props) (+17 more)

### Community 22 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.16
Nodes (22): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), Load, MultiblockRepairHandler (+14 more)

### Community 23 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.13
Nodes (24): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors) (+16 more)

### Community 24 - "Plan: Plan"
Cohesion: 0.10
Nodes (10): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Plan, Lake rules (none in city / military, 1 in 4 in other dead biomes, vanilla rate in overgrown, no surface lava pools), Floor height sampling (5x5 grid over the whole footprint where chunks exist plus 9 soilTop points in the window, median, maxSpread buildings 12 / schematics 7, stored in StructureData), MCEdit/WorldEdit .schematic format (no Sponge .schem or .litematic, TileEntities ignored), SKIP meta (bedrock leaves world untouched, null means air), Slice placement (Slices.place writes each Plan column inside the window), Rule: slice writers run for every chunk, filter by structure sector (+2 more)

### Community 26 - "Deci: Entity"
Cohesion: 0.11
Nodes (8): a, DamageSource, EntityLiving, ObjectZone, World, Entity, EntityLivingBase, net.minecraftforge.event.entity.EntityJoinWorldEvent

### Community 27 - "bug: Bug tracker (bug.md)"
Cohesion: 0.15
Nodes (21): Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: CustomSkinLoader coremod crash, EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: no supply drops in singleplayer (fixed v0.9.0), Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0) (+13 more)

### Community 28 - "Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)"
Cohesion: 0.13
Nodes (13): Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), Graded, Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Lots and yards (2 free on the sides, 3 behind, 6 to 9 front yard; offices and shops car park with nose-in wrecks, apartments gravel path and lawn), 1 block margin ring outside walls for exterior vines (SKIP elsewhere) (+5 more)

### Community 29 - "Capture: Capture"
Cohesion: 0.21
Nodes (3): Capture, JsonArray, JsonObject

### Community 30 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.15
Nodes (19): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look, Principle: furniture against a wall or partner piece, never floating (except islands: desk clusters, aisles, tables) (+11 more)

### Community 32 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.16
Nodes (10): Load, ObjectZone, ObjectZoneList, ServerTickEvent, ZoneStore, net.decimation.mod.server.zones.ObjectZone, net.decimation.mod.server.zones.ObjectZoneList, Save (+2 more)

### Community 34 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.21
Nodes (9): Building, Live loop: hotswap code, reload sets, rebuild in place, no restart per change, Override, WorldGenCommand, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender, jdb() (+1 more)

### Community 35 - "Shell: .unit()"
Cohesion: 0.14
Nodes (3): Shell, FixedBase, Plan

### Community 36 - "BiomeMap: BiomeMap"
Cohesion: 0.17
Nodes (7): Sectors, BiomeMap, NoiseGeneratorSimplex, DeciGenLayer, Override, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, net.minecraft.world.gen.layer.GenLayer

### Community 37 - "LcCity: LotPlan"
Cohesion: 0.14
Nodes (3): LotPlan, FixedBase, Shape

### Community 38 - "LcContent: LcContent"
Cohesion: 0.17
Nodes (4): Building, LcContent, Shape, lcpack content: 290 converted buildings, stairs per district style (local only)

### Community 39 - "lcstudy: dcinventory.py"
Cohesion: 0.23
Nodes (11): category(), districts(), main(), building name -> {city style: weight share}, structure_summary(), category(), main(), Pack (+3 more)

### Community 40 - "?: BottlecapHandler.java"
Cohesion: 0.14
Nodes (10): BottlecapHandler (deciworldgen), Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), BottlecapHandler, VehicleHitHandler, EntityPlayer, ItemStack, net.minecraftforge.event.entity.living.LivingDeathEvent (+2 more)

### Community 41 - "bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.17
Nodes (13): tools/patches/PatchSwing.java, Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Performance complaints need real profiling, Javassist patches must go into three jars (patched, dist, dev base) plus Prism; sources in tools/patches (PatchSwing, PatchPropCulling), ClientRenderHandler.renderScopeView (deci.c.b), ClientState (deci.b.i), SmoothSwingThread (deci.b.h) (+5 more)

### Community 42 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.18
Nodes (8): cpw.mods.fml.common.eventhandler.SubscribeEvent, DevPregen, ServerTickEvent, TerrainEvents, InitBiomeGens, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate, Pre

### Community 43 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 44 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.15
Nodes (14): CFR decompiler, Hand written Forge/Minecraft stub classes, Javassist bytecode patcher, Javassist cannot compile Java 8 lambdas, Patch from ORIGINAL classes only after checking the target class is identical in the patched jar, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event (+6 more)

### Community 45 - "Condition: com.google.gson.JsonObject"
Cohesion: 0.19
Nodes (5): com.google.gson.JsonArray, com.google.gson.JsonObject, Condition, Set conditions: when kinds / storey / floors, net.decimation.worldgen.Schematic

### Community 46 - "StructureData: StructureData"
Cohesion: 0.20
Nodes (6): Override, StructureData, NBTTagCompound, net.minecraft.nbt.NBTTagCompound, net.minecraft.tileentity.TileEntity, net.minecraft.world.WorldSavedData

### Community 47 - "Facing: .setMeta()"
Cohesion: 0.23
Nodes (3): Facing, Entry, ShopPlanner

### Community 48 - "SupplyDropScheduler: SupplyDropScheduler"
Cohesion: 0.23
Nodes (4): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, ServerTickEvent, SupplyDropScheduler, net.minecraft.server.MinecraftServer

### Community 49 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 50 - "furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.15
Nodes (13): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, Furniture sets doc: data driven JSON furniture groups, user editable, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, known.txt: unedited old built-in copies are updated (tools/asset_hashes.py), Named palettes and weighted styles for sets (base / style keys), Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window, Set preview mode -Psets: each set in a plaster bay, photographed, Seed 1 placement: kitchen/bath/lobby/closet 100%, bed 84%, living 80%, dining 68% (+5 more)

### Community 52 - "anvil118: anvil118.py"
Cohesion: 0.24
Nodes (10): chunk_biomes(), chunk_blocks(), chunks(), main(), Palette, {section Y: biomes[4,4,4] (y, z, x) global ids}., Entries packed without spanning longs (1.16+)., Global name -> id for a whole survey. (+2 more)

### Community 54 - "Deci: .complete()"
Cohesion: 0.24
Nodes (3): Block, TileEntity, World

### Community 56 - "Palettes: com.google.gson.JsonObject"
Cohesion: 0.29
Nodes (3): Palettes, Style, Entry

### Community 57 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.35
Nodes (11): Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Procedural building design doc (city blocks, Building v2), Minecraft scale (storey 4 blocks, corridor 2 wide, doors 1x2, units 5 to 8 deep), Office floor layout (core, open plan desk rows at windows, meeting rooms, reception, storage, break room), Layout research sources (Auckland Design Manual, archgyan, Pult, Shopify, Small Business Trends), Shop grid layout (parallel aisles, decompression zone, checkout front left, stockroom behind), Switchback stair core 4x7 to a roof hatch (replaces the old ladder shaft) (+3 more)

### Community 58 - "interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 59 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.20
Nodes (10): City engine: Lost Cities style cities from converted DeceasedCraft content, City decor: parks on open lots, street scenes (pack fountains), building fronts on the street side, City districts: wasteland next to military sectors, current beta weighted, City highways: seed based network between city regions, open / bridge / tunnel chunks, deck at 64, side ramps, City levels per cell (6 blocks apart), streets at G, stairs parts between levels, City street dressing: sidewalks, centre lines, lamps, benches, wrecks, District street parts: LC street parts by connection count, road paint to painted road blocks, City superblocks: 2x2 cells, 7x7 building chunks, landmark towers (+2 more)

### Community 60 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 61 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 63 - "hwmap: hwmap.py"
Cohesion: 0.53
Nodes (8): at(), city(), hl(), line(), s64(), spans(), xh(), zh()

### Community 64 - "graph_update: graph_update.py"
Cohesion: 0.52
Nodes (6): finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 65 - "interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 66 - "CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side"
Cohesion: 0.50
Nodes (4): v0.18.0 apartment rooms and propFacing fix, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

### Community 67 - "bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)"
Cohesion: 0.67
Nodes (3): NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer), Plan: enemy military, juggernaut with machine guns / Barrett, stronger bandits, in military areas, Plan: more zombie variants, 60 round STANAG / 5.45 AK mags, NPC bullet impact particles

## Ambiguous Edges - Review These
- `Genuinely new model recipe (needs Techne)` → `addBox passes Y as Z origin (use addShape)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` → `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `FML IWorldGenerator per chunk hook` → `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to

## Knowledge Gaps
- **96 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+91 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **47 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Genuinely new model recipe (needs Techne)` and `addBox passes Y as Z origin (use addShape)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` and `Recurring root cause: integrated server reports side CLIENT`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `FML IWorldGenerator per chunk hook` and `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `StructureGenerator` connect `StructureGenerator: net.minecraft.world.World` to `SchematicPlan: .init()`, `BiomeMap: BiomeMap`, `new_feature: Current state and pending decisions`, `Ruins: net.minecraft.block.Block`, `building_design: v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `worldgen: World generation doc (deciworldgen)`, `Plan: Plan`, `LcCity: LcCity`?**
  _High betweenness centrality (0.133) - this node is a cross-community bridge._
- **Why does `Building` connect `Building: Building` to `StructureGenerator: net.minecraft.world.World`, `DevAutoTest: DevAutoTest`, `SchematicPlan: .init()`, `Shell: .unit()`, `LcCity: LotPlan`, `Ruins: net.minecraft.block.Block`, `Facing: .setMeta()`, `StoreyPlan: StoreyPlan`, `decimation_maps: Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161)`, `LcCity: BuildingPlan`, `LcCity: LcCity`, `Furnisher: Furnisher`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._