# Graph Report - .  (2026-10-09)

## Corpus Check
- 7 files · ~134,174 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1457 nodes · 3043 edges · 111 communities (73 shown, 38 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 418 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- StructureGenerator: net.minecraft.world.World
- DevAutoTest: DevAutoTest
- building_design: Procedural building design doc (city blocks, Building v2)
- DeciBiome: DeciBiome
- worldcheck: worldcheck.py
- lctranslate: lctranslate.py
- CLAUDE: decimation-singleplayer README (public repo overview)
- LcCity: EdgePlan
- Test Schematic Builder
- ScopeZoom: ScopeZoom
- ?: DecimationWorldGen
- Building: Building
- create_weapons: Creating new weapons guide
- LcCity: net.minecraft.block.Block
- decimation_maps: Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161)
- architecture: ServerProxy (deci.a.e, dedicated only)
- Deci: Deci
- architecture: Obfuscation map (package to meaning)
- Surfaces: net.minecraft.block.Block
- LcCity: LcCity
- StoreyPlan: StoreyPlan
- LcCity: BuildingPlan
- Capture: Capture
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- bug: Bug tracker (bug.md)
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- WorldGenCommand: WorldGenCommand
- Shell: .unit()
- SchematicPlan: SchematicPlan
- worldgen: World generation doc (deciworldgen)
- FurnitureSets: FurnitureSet
- lcstudy: dcinventory.py
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- Name Mapping Applier
- new_feature: Feature tracker (new_feature.md)
- LcContent: LcContent
- Ruins: Ruins
- architecture: Recurring root cause: integrated server reports side CLIENT
- furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable
- Plan: Plan
- anvil118: anvil118.py
- Slices: Slices
- Graded: Graded
- StructureData: StructureData
- architecture: Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)
- AssetDir: AssetDir
- Palettes: com.google.gson.JsonObject
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)
- new_feature: Current state and pending decisions
- ?: com.google.gson.JsonObject
- Facing: .setMeta()
- city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content
- ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)
- terrain: Decimation world type doc (terrain, 0.14.0)
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- Heuristic Auto Namer
- Furnisher: Furnisher
- interior_spec: Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only
- Plan: Floor height sampling (5x5 grid over the whole footprint where chunks exist plus 9 soilTop points in the window, median, maxSpread buildings 12 / schematics 7, stored in StructureData)
- MultiblockRepairHandler: MultiblockRepairHandler
- floorplan: Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md
- Rotation: Rotation
- interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic
- DecimationWorldType: DecimationWorldType
- BottlecapHandler: BottlecapHandler.java
- Schematic: Schematic
- terrain: Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone
- CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side
- VehicleHitHandler: VehicleHitHandler
- ZoneSpawnHandler: ZoneSpawnHandler
- DevPregen: DevPregen
- architecture: Bug: bottlecaps not converted to currency (fixed)
- bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)
- asset_hashes: asset_hashes.py
- footprint: footprint.py
- PatchScope: PatchScope
- PatchTracer: PatchTracer
- bug: Converted buildings use LED lamp blocks as floor (open)
- bug: Prop dense FPS drop: line of sight ray casts 76% of prop rendering, cached in v0.28.6; model drawing remains
- Weather Type Id Bug
- ?: ServerTickEvent
- ?: a
- ?: a
- ?: Entry
- ?: FurnitureSet
- ?: a
- ?: CityDistrict
- ?: Schematic
- ?: EntityPlayer
- ?: Entry
- ?: ItemStack
- ?: ServerTickEvent
- ?: FurnitureSet
- ?: Override
- ?: World
- interior_spec: propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N
- ?: LargeSites
- ?: net.minecraftforge.client.event.FOVUpdateEvent
- ?: Shape
- ?: World

## God Nodes (most connected - your core abstractions)
1. `Building` - 50 edges
2. `StructureGenerator` - 48 edges
3. `DevAutoTest` - 47 edges
4. `LcCity` - 38 edges
5. `World generation doc (deciworldgen)` - 27 edges
6. `StoreyPlan` - 27 edges
7. `Current state and pending decisions` - 27 edges
8. `EdgePlan` - 26 edges
9. `Bug tracker (bug.md)` - 26 edges
10. `Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside)` --semantically_similar_to--> `survivor_camp()`  [INFERRED] [semantically similar]
  docs/interior_spec.md → tools/make_test_schematics.py
- `ZoneStore (per world deciworldgen_zones.json)` --references--> `ZoneStore`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/ZoneStore.java
- `ArmorGunfireHandler` --implements--> `Proposed LivingHurtEvent gunshot damage unification`  [INFERRED]
  dev/src/main/java/net/decimation/fixes/ArmorGunfireHandler.java → bug.md
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `VehicleHitHandler (v0.8.1)` --references--> `VehicleHitHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/VehicleHitHandler.java

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

## Communities (111 total, 38 thin omitted)

### Community 0 - "StructureGenerator: net.minecraft.world.World"
Cohesion: 0.07
Nodes (19): cpw.mods.fml.common.IWorldGenerator, Yard, Highways, Plan, Schematic, LargeSites, a, CityDistrict (+11 more)

### Community 1 - "DevAutoTest: DevAutoTest"
Cohesion: 0.06
Nodes (33): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, ClientTickEvent, SupplyDropScheduler, DevAutoTest, Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type, Finding: no doors anywhere, only gaps (14 Decimation door blocks unused) (+25 more)

### Community 2 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.06
Nodes (59): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Hand written Forge/Minecraft stub classes, Javassist bytecode patcher, Javassist cannot compile Java 8 lambdas (+51 more)

### Community 3 - "DeciBiome: DeciBiome"
Cohesion: 0.05
Nodes (24): cpw.mods.fml.relauncher.SideOnly, Sectors, BiomeMap, NoiseGeneratorSimplex, DeadTree, Override, DeciBiome, Override (+16 more)

### Community 4 - "worldcheck: worldcheck.py"
Cohesion: 0.07
Nodes (43): Bug: structures built on ocean floor, Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), City edge ramp: 24 wide, rounded corners, nearest cell owns a column, wobbled contours, Seed 1 biome share 800x800 around spawn (city 27%, dead wild 26%, suburbs 15%, military 9%, overgrown 21%, river 1%) (+35 more)

### Community 5 - "lctranslate: lctranslate.py"
Cohesion: 0.07
Nodes (36): building_columns(), main(), matches(), Pack, part_slices(), (layers bottom up, ground layer index): every floor picked like Lost Cities…, Lost Cities part conditions for one floor (cellars are negative)., tag() (+28 more)

### Community 6 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.06
Nodes (44): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CLAUDE.md project guide, dev/libs/Decimation-base.jar (patched jar minus our classes), Decimation.jar (obfuscated Forge 1.7.10 mod jar), Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), deobf/ readable reference tree (decompiled with readable names, read only) (+36 more)

### Community 7 - "LcCity: EdgePlan"
Cohesion: 0.06
Nodes (4): EdgePlan, LotPlan, FixedBase, Graded

### Community 8 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 9 - "ScopeZoom: ScopeZoom"
Cohesion: 0.13
Nodes (14): Scope FPS drop: picture in picture second world render, fixed v0.28.0 with view zoom, Configuration, ScopeZoom, Cheap scope done: zoom + see-through glass for reddot / 2x, black sniper overlay from 4x, Field, Method, net.minecraft.client.Minecraft, net.minecraft.client.renderer.EntityRenderer (+6 more)

### Community 10 - "?: DecimationWorldGen"
Cohesion: 0.12
Nodes (17): Building, CityDistrict, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours) (+9 more)

### Community 11 - "Building: Building"
Cohesion: 0.09
Nodes (7): Building, ShopPlanner, Furnisher, net.decimation.worldgen.Graded, Ruins, Surfaces, Yard

### Community 12 - "create_weapons: Creating new weapons guide"
Cohesion: 0.17
Nodes (24): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+16 more)

### Community 14 - "decimation_maps: Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161)"
Cohesion: 0.09
Nodes (24): DeceasedCraft content catalogue: 79 Lost Cities buildings, city parts, apocalypsenow structures, disabled vanilla structures, Lost Cities to 1.7.10 conversion (lc2schem, lctranslate, paste command), DeceasedCraft interiors fully authored per storey, no procedural rooms, DeceasedCraft 79 building types and 5 district city styles, Study: DeceasedCraft city buildings (DCTweaks jar Lost Cities data), DeceasedCraft storey is 6 high (4 air) vs our 5, Builders: separate ceiling tiles with light panel grid and vents, Study: hand-built Decimation maps (USA coast, Decicraft, Cloverfield, world-e161) (+16 more)

### Community 15 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.13
Nodes (23): Bug: zones never active in singleplayer (partial fix), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), DeathStatsHandler (deci.aK.h), EntitySpawnZoneHandler (deci.aK.d) (+15 more)

### Community 16 - "Deci: Deci"
Cohesion: 0.15
Nodes (5): Obfuscated Decimation names in our code go through fixes/Deci, Deci, net.minecraft.entity.player.EntityPlayer, net.minecraft.item.Item, net.minecraft.item.ItemStack

### Community 17 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.15
Nodes (23): Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, ClientProxy (deci.a.c), DeciConstants (deci.Q.c, GUI ids), DecimationMod (deci.a.b, @Mod entry), LootCloseCallback (deci.aB.b) (+15 more)

### Community 18 - "Surfaces: net.minecraft.block.Block"
Cohesion: 0.19
Nodes (3): Interior, Surfaces, net.minecraft.block.Block

### Community 19 - "LcCity: LcCity"
Cohesion: 0.21
Nodes (5): LcCity, StreetProps, Highways, net.decimation.worldgen.Plan, net.decimation.worldgen.StructureGenerator

### Community 20 - "StoreyPlan: StoreyPlan"
Cohesion: 0.22
Nodes (3): ApartmentPlanner, OfficePlanner, StoreyPlan

### Community 21 - "LcCity: BuildingPlan"
Cohesion: 0.12
Nodes (3): a, BuildingPlan, Building

### Community 22 - "Capture: Capture"
Cohesion: 0.21
Nodes (3): Capture, JsonObject, JsonArray

### Community 23 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.16
Nodes (9): a, ServerTickEvent, ZoneStore, net.decimation.mod.server.zones.ObjectZone, net.decimation.mod.server.zones.ObjectZoneList, ObjectZone, ObjectZoneList, Save (+1 more)

### Community 24 - "bug: Bug tracker (bug.md)"
Cohesion: 0.15
Nodes (18): Bug: large ammo crate NPE (dead field avk), Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Bug: loot never worked in singleplayer, setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation (+10 more)

### Community 25 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.23
Nodes (17): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), Office and table furniture (BlockChair, BlockOfficeChair, wood / metal tables long along x at meta 3, BlockMonitor, flatscreen TVs, 2 tall metal shelves), Chunk load repair only sets the master (writing blocks during ChunkEvent.Load is unsafe), so shelves in pre 0.16 worlds stay one block tall (+9 more)

### Community 26 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.23
Nodes (8): Live loop: hotswap code, reload sets, rebuild in place, no restart per change, Override, WorldGenCommand, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender, jdb(), main()

### Community 27 - "Shell: .unit()"
Cohesion: 0.18
Nodes (4): Shell, Audit shots v0.23: 6 high offices and shops, Offices and shops 6 high storeys DONE v0.23.0, Storey height per building: public 6, homes 5 (user decision)

### Community 28 - "SchematicPlan: SchematicPlan"
Cohesion: 0.15
Nodes (3): a, Schematic, SchematicPlan

### Community 29 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.23
Nodes (16): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), Large schematics (up to 120x120, per site chance, placed inside the site), Safe population window (chunk cx,cz writes only [cx*16+8, cx*16+23]), Filename prefix pools (civ_, city_, mil_, untagged = any sector), MCEdit/WorldEdit .schematic format (no Sponge .schem or .litematic, TileEntities ignored) (+8 more)

### Community 30 - "FurnitureSets: FurnitureSet"
Cohesion: 0.19
Nodes (4): Condition, FurnitureSet, FurnitureSets, Set conditions: when kinds / storey / floors

### Community 31 - "lcstudy: dcinventory.py"
Cohesion: 0.23
Nodes (11): category(), districts(), main(), building name -> {city style: weight share}, structure_summary(), category(), main(), Pack (+3 more)

### Community 32 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.18
Nodes (15): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look, Principle: furniture against a wall or partner piece, never floating (except islands: desk clusters, aisles, tables), Principle: function first, one focal object per room placed first (bed, sofa + TV, desk cluster, checkout) (+7 more)

### Community 33 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.19
Nodes (8): cpw.mods.fml.common.eventhandler.SubscribeEvent, HumanityKillHandler, TerrainEvents, InitBiomeGens, net.minecraftforge.event.entity.living.LivingDeathEvent, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate, Pre

### Community 34 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 35 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.16
Nodes (15): BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), TraderSpawnManager (server.traders.a), Intro screen skip (deci.i.d.iH, deci.i.c.io flags), Loot fix documentation (documentation.md), 50% ranged weapon damage nerf, dist/ separate jars (deciworldgen-0.7.0.jar + Decimation.jar) (+7 more)

### Community 36 - "LcContent: LcContent"
Cohesion: 0.18
Nodes (3): LcContent, Shape, lcpack content: 290 converted buildings, stairs per district style (local only)

### Community 38 - "architecture: Recurring root cause: integrated server reports side CLIENT"
Cohesion: 0.19
Nodes (13): Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: no supply drops in singleplayer (fixed v0.9.0), Recurring root cause: integrated server reports side CLIENT, DeathHandler (deci.an.f, humanity isServer gate), IntRange (deci.aB.a, off by one rolls), Inner isServer() gates in shared code, LootPool (deci.aB.c) (+5 more)

### Community 39 - "furniture_sets: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.15
Nodes (13): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, Furniture sets doc: data driven JSON furniture groups, user editable, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, known.txt: unedited old built-in copies are updated (tools/asset_hashes.py), Named palettes and weighted styles for sets (base / style keys), Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window, Set preview mode -Psets: each set in a plaster bay, photographed, Seed 1 placement: kitchen/bath/lobby/closet 100%, bed 84%, living 80%, dining 68% (+5 more)

### Community 41 - "anvil118: anvil118.py"
Cohesion: 0.24
Nodes (10): chunk_biomes(), chunk_blocks(), chunks(), main(), Palette, {section Y: biomes[4,4,4] (y, z, x) global ids}., Entries packed without spanning longs (1.16+)., Global name -> id for a whole survey. (+2 more)

### Community 42 - "Slices: Slices"
Cohesion: 0.33
Nodes (3): Bug: building base height depends on chunk generation order (Slices.decideBase), Plan, Slices

### Community 43 - "Graded: Graded"
Cohesion: 0.17
Nodes (3): FixedBase, Graded, Plan

### Community 44 - "StructureData: StructureData"
Cohesion: 0.24
Nodes (5): Override, StructureData, NBTTagCompound, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 45 - "architecture: Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)"
Cohesion: 0.24
Nodes (11): Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Performance complaints need real profiling, ClientRenderHandler.renderScopeView (deci.c.b), ClientState (deci.b.i), PlayerData (deci.Q.b), SmoothSwingThread (deci.b.h), Attachment fixed offsets on rails (+3 more)

### Community 47 - "Palettes: com.google.gson.JsonObject"
Cohesion: 0.29
Nodes (3): Palettes, Style, Entry

### Community 48 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.24
Nodes (10): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a), 35% armor damage reduction buff (+2 more)

### Community 49 - "building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.29
Nodes (10): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Exterior add-ons: entrance frame + slab canopy + steps, glass shopfronts with BlockSign_* logo sign, office lobby glazing, apartment balconies, fire escapes over 3 storeys, roof stair hut / water tank / vents / antenna / parapet, downpipes [not verified] (+2 more)

### Community 50 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.22
Nodes (10): CFR decompiler, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief (+2 more)

### Community 51 - "interior_spec: Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade)"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 52 - "new_feature: Current state and pending decisions"
Cohesion: 0.31
Nodes (10): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), deciworldgen second @Mod (net.decimation.worldgen), Dropped companion mods (Ruins, ezWastelands, GeneratorMods), Generator crash hardening (catch all, self disable after 3 errors), FML IWorldGenerator per chunk hook, tools/make_test_schematics.py (8 schematics), MarkerGenerator (milestone 2 glowstone pillars) (+2 more)

### Community 53 - "?: com.google.gson.JsonObject"
Cohesion: 0.27
Nodes (3): com.google.gson.JsonArray, com.google.gson.JsonObject, net.decimation.worldgen.Schematic

### Community 55 - "city_engine: City engine: Lost Cities style cities from converted DeceasedCraft content"
Cohesion: 0.20
Nodes (10): City engine: Lost Cities style cities from converted DeceasedCraft content, City decor: parks on open lots, street scenes (pack fountains), building fronts on the street side, City districts: wasteland next to military sectors, current beta weighted, City highways: seed based network between city regions, open / bridge / tunnel chunks, deck at 64, side ramps, City levels per cell (6 blocks apart), streets at G, stairs parts between levels, City street dressing: sidewalks, centre lines, lamps, benches, wrecks, District street parts: LC street parts by connection count, road paint to painted road blocks, City superblocks: 2x2 cells, 7x7 building chunks, landmark towers (+2 more)

### Community 56 - "ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.33
Nodes (5): Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), ArmorGunfireHandler, net.minecraft.entity.Entity, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 57 - "terrain: Decimation world type doc (terrain, 0.14.0)"
Cohesion: 0.28
Nodes (9): Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), worldmap_seed1_v0.14.png: top down map of seed 1, Decimation world type (city sectors, military, suburbs, overgrown forest, rivers stopping at city edges), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Spawn search: suburb, wasteland, overgrown plains and forest added to WorldChunkManager.allowedBiomes, Decimation world type doc (terrain, 0.14.0), Terrain stays vanilla ChunkProviderGenerate, only the biome map is replaced, Decimation world type feature (user chose A over reskinning vanilla / RTG, done v0.14.0, seen in game; open: fog colour, rubble and ash decoration) (+1 more)

### Community 58 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 59 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 61 - "interior_spec: Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only"
Cohesion: 0.31
Nodes (9): Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only, Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside), Spec user decisions: survivor camps (about 1 in 12 buildings), shop signs use BlockSign_* assets, reusable parts for later categories, start by fixing the audited apartment / office / shop, Decals (blood splats, hazard signs, notes, framed picture, flags, warning signs; flat upright pictures, wall face by meta [not verified]), Graffiti 1..12 (tags and slogans, flat upright pictures), Survivor and death props (skeletons, body bags, hanging player, lantern, can fire, can trap, propeller trap) (+1 more)

### Community 62 - "Plan: Floor height sampling (5x5 grid over the whole footprint where chunks exist plus 9 soilTop points in the window, median, maxSpread buildings 12 / schematics 7, stored in StructureData)"
Cohesion: 0.25
Nodes (5): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Lake rules (none in city / military, 1 in 4 in other dead biomes, vanilla rate in overgrown, no surface lava pools), Floor height sampling (5x5 grid over the whole footprint where chunks exist plus 9 soilTop points in the window, median, maxSpread buildings 12 / schematics 7, stored in StructureData), Foundation down to the ground (max 12, Plan.foundation): stone brick plinth for buildings, dirt for schematics, Terrain blending (round 2 step 1, done v0.13.0, headless verified, not yet seen in game)

### Community 63 - "MultiblockRepairHandler: MultiblockRepairHandler"
Cohesion: 0.36
Nodes (4): World, MultiblockRepairHandler, Load, net.minecraft.tileentity.TileEntity

### Community 64 - "floorplan: Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md"
Cohesion: 0.36
Nodes (7): Principle: circulation (free 1 block path door to door, no furniture on a doorway cell or the cell in front; reachability >= 95% per storey), Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md, plans_v0.16 floor plans (tools/floorplan.py, 14 px per block, letter per prop), main(), Feet cell and head cell passable., style(), walkable()

### Community 66 - "interior_spec: Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 67 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

### Community 68 - "BottlecapHandler: BottlecapHandler.java"
Cohesion: 0.50
Nodes (3): BottlecapHandler (deciworldgen), BottlecapHandler, net.minecraftforge.event.entity.player.EntityItemPickupEvent

### Community 70 - "terrain: Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone"
Cohesion: 0.40
Nodes (5): Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Rivers as a noise contour (|simplex| < 0.022 at scale 520, domain warped, 32+ blocks from city and military sectors), Sector grid (16x16 chunks: WILD 40%, CIV 25%, CITY 15%, MIL 20%), Zone tagging (mil_ MILITARY, city_ and buildings POLICE, deciworldgen_zones.json)

### Community 71 - "CLAUDE: Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side"
Cohesion: 0.50
Nodes (4): v0.18.0 apartment rooms and propFacing fix, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

### Community 75 - "architecture: Bug: bottlecaps not converted to currency (fixed)"
Cohesion: 1.00
Nodes (3): Bug: bottlecaps not converted to currency (fixed), ItemPickupHandler (deci.aK.e, bottlecaps), LegacyServerEventHandler (deci.aL.a, dead code)

### Community 76 - "bug: NPC tracers flew along the body facing; fixed by sending the target id (PatchTracer)"
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
- **91 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+86 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

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
- **Why does `StructureGenerator` connect `StructureGenerator: net.minecraft.world.World` to `building_design: Procedural building design doc (city blocks, Building v2)`, `DeciBiome: DeciBiome`, `?: DecimationWorldGen`, `Surfaces: net.minecraft.block.Block`, `LcCity: LcCity`, `new_feature: Current state and pending decisions`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `worldgen: World generation doc (deciworldgen)`?**
  _High betweenness centrality (0.184) - this node is a cross-community bridge._
- **Why does `World generation doc (deciworldgen)` connect `worldgen: World generation doc (deciworldgen)` to `StructureGenerator: net.minecraft.world.World`, `building_design: Procedural building design doc (city blocks, Building v2)`, `worldcheck: worldcheck.py`, `terrain: Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone`, `?: DecimationWorldGen`, `new_feature: Current state and pending decisions`, `terrain: Decimation world type doc (terrain, 0.14.0)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._