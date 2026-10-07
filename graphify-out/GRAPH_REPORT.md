# Graph Report - .  (2026-10-07)

## Corpus Check
- 18 files · ~84,094 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 988 nodes · 2185 edges · 74 communities (61 shown, 13 thin omitted)
- Extraction: 82% EXTRACTED · 17% INFERRED · 0% AMBIGUOUS · INFERRED: 382 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- StructureGenerator: StructureGenerator
- DevAutoTest: DevAutoTest
- building_design: Procedural building design doc (city blocks, Building v2)
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- worldgen: World generation doc (deciworldgen)
- Test Schematic Builder
- Building: Building
- architecture: LootInteractHandler (deci.aK.k)
- bug: Bug tracker (bug.md)
- DecimationWorldGen: DecimationWorldGen
- bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- new_feature: Feature tracker (new_feature.md)
- Building: net.minecraft.block.Block
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- Building: .apartmentUnit()
- ZoneStore: ZoneStore
- SchematicPlan: SchematicPlan
- CLAUDE: deobf/ readable reference tree (decompiled with readable names, read only)
- WorldGenCommand: WorldGenCommand
- CLAUDE: decimation-singleplayer README (public repo overview)
- DeciBiome: DeciBiome
- Name Mapping Applier
- new_feature: Feature tracker (new_feature.md)
- ?: DeciGenLayer
- architecture: ServerProxy (deci.a.e, dedicated only)
- CLAUDE: tools/build.py real javac pipeline
- FurnitureSets: FurnitureSets
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- BiomeMap: BiomeMap
- SealedCaves: net.minecraft.world.biome.BiomeGenBase
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- Building: Furniture sets doc: data driven JSON furniture groups, user editable
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)
- new_feature: Current state and pending decisions
- StructureData: StructureData
- interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- graph_update: graph_update.py
- Building: Step 1 DONE v0.17.0: Decimation door in every DOOR plan cell (meta 0/2 wall along z, 1/3 along x, +4 open 25%; 20% + decay * 50% missing; one door type per building), low debris only, never beside a door; seed 1: 373 doors in 105 buildings, storey reachability median 100%
- apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels
- Heuristic Auto Namer
- DecimationBiomes: DecimationBiomes
- FurnitureSet: FurnitureSet
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- Rotation: Rotation
- DeadTree: DeadTree
- new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)
- new_feature: deciworldgen second @Mod (net.decimation.worldgen)
- interior_spec: Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs
- DecimationWorldType: DecimationWorldType
- BottlecapHandler: BottlecapHandler.java
- architecture: BackendConnection (deci.aP.a, kryonet)
- Schematic: Schematic
- interior_spec: Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta)
- architecture: TurfManager (server.turf.a)
- ZoneSpawnHandler: ZoneSpawnHandler
- Weather Type Id Bug
- ?: ServerTickEvent
- ?: a
- ?: Schematic
- ?: EntityPlayer
- ?: ServerTickEvent
- ?: Plan
- ?: a
- ?: Override
- ?: Props

## God Nodes (most connected - your core abstractions)
1. `Building` - 93 edges
2. `StructureGenerator` - 44 edges
3. `DevAutoTest` - 37 edges
4. `World generation doc (deciworldgen)` - 27 edges
5. `Current state and pending decisions` - 27 edges
6. `Bug tracker (bug.md)` - 26 edges
7. `Procedural building design doc (city blocks, Building v2)` - 25 edges
8. `Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)` - 25 edges
9. `Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)` - 24 edges
10. `DeciBiome` - 21 edges

## Surprising Connections (you probably didn't know these)
- `Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside)` --semantically_similar_to--> `survivor_camp()`  [INFERRED] [semantically similar]
  docs/interior_spec.md → tools/make_test_schematics.py
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` --semantically_similar_to--> `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS] [semantically similar]
  docs/terrain.md → CLAUDE.md
- `ZoneStore (per world deciworldgen_zones.json)` --references--> `ZoneStore`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/ZoneStore.java
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `VehicleHitHandler (v0.8.1)` --references--> `VehicleHitHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/VehicleHitHandler.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Visual evidence memory (autotest screenshot modes, docs/shots, written shots index, prop catalogue)** — claude_visual_evidence_rule, claude_autotest_screenshot_modes, docs_shots_index_shots_index, docs_prop_catalogue_prop_catalogue_doc, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_servegalleryview [INFERRED 0.85]
- **Interior spec layered build (structure, surfaces, room programs, story / decay, exterior, in work order)** — docs_interior_spec_principle_layer_order, docs_interior_spec_doors, docs_interior_spec_surfaces, docs_interior_spec_apartment_unit_program, docs_interior_spec_office_storey_programs, docs_interior_spec_shop_program, docs_interior_spec_story_decay_layer, docs_interior_spec_exterior, docs_interior_spec_order_of_work [EXTRACTED 1.00]
- **Step 4d.1 doors and low debris (spec, code, tracker, release note, audit shots)** — docs_interior_spec_step1_doors_low_debris, dev_src_main_java_net_decimation_worldgen_building_building_door, dev_src_main_java_net_decimation_worldgen_building_building_lowdebris, dev_src_main_java_net_decimation_worldgen_building_building_bydoor, dev_src_main_java_net_decimation_worldgen_building_building_nearwall, claude_v0_17_0_doors_low_debris, new_feature_step_4d1_doors_debris, docs_shots_index_audit_v0_17 [INFERRED 0.85]
- **Verification loop for every building change (floor plans, audit shots, scans, autotest, written findings)** — docs_interior_spec_verification, tools_floorplan, tools_wallscan, tools_gradescan, tools_multiscan, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_serveauditview, docs_building_audit_building_audit_doc, docs_shots_index_shots_index [EXTRACTED 1.00]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Building quality audit toolchain (floor plans with reachability, audit screenshots, view cells, indexed results)** — docs_building_audit_audit_method, tools_floorplan, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_serveauditview, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_pickauditbuildings, dev_src_main_java_net_decimation_worldgen_building_building_viewcell, docs_shots_index_audit_v0_16, docs_shots_index_plans_v0_16 [INFERRED 0.85]
- **Upper storey reachability fix v0.16.1 (floorplan scan, unsupported ladder, collapse exemption, Building.ladder / collapsed)** — bug_upper_storeys_unreachable, bug_ladder_unsupported_wall_cell, bug_ladder_core_collapse_exemption, tools_floorplan, dev_src_main_java_net_decimation_worldgen_building_building_ladder, dev_src_main_java_net_decimation_worldgen_building_building_collapsed [EXTRACTED 1.00]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_building_building, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_building_building_grade, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Whole multiblock generation (v0.16.0): slice writer and small placer call complete after writing, Decimation helper forms the box, multiscan checks** — dev_src_main_java_net_decimation_worldgen_slices_slices_write, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_place, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler_complete, docs_worldgen_form_multiblock_helper, docs_worldgen_complete_after_slice_rule, tools_multiscan [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_carmeta, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Decimation world type biome pipeline (TerrainEvents swaps GenLayers, DeciGenLayer reads BiomeMap, BiomeMap uses shared Sectors, DecimationBiomes defines the biomes)** — dev_src_main_java_net_decimation_worldgen_terrain_terrainevents_terrainevents, dev_src_main_java_net_decimation_worldgen_terrain_decigenlayer_decigenlayer, dev_src_main_java_net_decimation_worldgen_terrain_biomemap_biomemap, dev_src_main_java_net_decimation_worldgen_sectors_sectors, dev_src_main_java_net_decimation_worldgen_terrain_decimationbiomes_decimationbiomes, dev_src_main_java_net_decimation_worldgen_terrain_decimationworldtype_decimationworldtype [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]

## Communities (74 total, 13 thin omitted)

### Community 0 - "StructureGenerator: StructureGenerator"
Cohesion: 0.07
Nodes (17): a, cpw.mods.fml.common.IWorldGenerator, Graded, Schematic, LargeSites, Slices, Schematic, StructureGenerator (+9 more)

### Community 1 - "DevAutoTest: DevAutoTest"
Cohesion: 0.07
Nodes (24): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, ClientTickEvent, SupplyDropScheduler, DevAutoTest, Entry, Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type (+16 more)

### Community 2 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.09
Nodes (44): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), Props, CityDistrict, Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit) (+36 more)

### Community 3 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.08
Nodes (34): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Principle: circulation (free 1 block path door to door, no furniture on a doorway cell or the cell in front; reachability >= 95% per storey), Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md, plans_v0.16 floor plans (tools/floorplan.py, 14 px per block, letter per prop) (+26 more)

### Community 4 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.07
Nodes (29): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), v0.12.1: whole footprint floor height sampling and dirt fill under schematics, a, Plan, Building categories: civilian (apartment, office, shops, houses, garage), police (police station), military (base, checkpoint), later medical / industrial; category decides sector and Decimation zone, BiomeMap.biomeAt rules (seed only: city and military biomes exactly on sector squares, suburbs warped up to 56, dead wilderness within about 100 blocks, overgrown further out, no villages), Lake rules (none in city / military, 1 in 4 in other dead biomes, vanilla rate in overgrown, no surface lava pools) (+21 more)

### Community 5 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 6 - "Building: Building"
Cohesion: 0.09
Nodes (3): Building, Entry, Graded

### Community 7 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.10
Nodes (28): Bug: large ammo crate NPE (dead field avk), Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), Obfuscation map (package to meaning), DeciConstants (deci.Q.c, GUI ids), DecimationMod (deci.a.b, @Mod entry), IntRange (deci.aB.a, off by one rolls), LootCloseCallback (deci.aB.b) (+20 more)

### Community 8 - "bug: Bug tracker (bug.md)"
Cohesion: 0.11
Nodes (28): Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens (+20 more)

### Community 9 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.13
Nodes (15): CityDistrict, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours), FMLPostInitializationEvent (+7 more)

### Community 10 - "bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.12
Nodes (21): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Hand written Forge/Minecraft stub classes (+13 more)

### Community 11 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.17
Nodes (24): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+16 more)

### Community 12 - "Building: net.minecraft.block.Block"
Cohesion: 0.21
Nodes (3): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), net.minecraft.block.Block

### Community 13 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.19
Nodes (17): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), World, MultiblockRepairHandler (+9 more)

### Community 14 - "Building: .apartmentUnit()"
Cohesion: 0.18
Nodes (5): v0.18.0 apartment rooms and propFacing fix, propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N, Step 3 apartment rooms done v0.18.0: kitchen run, dining, TV + sofa, bedroom, bathroom, lobby on one side, audit_v0.18 / plans_v0.18: kitchen run, checker ceiling issue, furnished flats, 4d.3 apartment rooms done v0.18.0, next 4d.4 office programs

### Community 15 - "ZoneStore: ZoneStore"
Cohesion: 0.15
Nodes (10): a, ServerTickEvent, ZoneStore, Load, net.decimation.mod.server.zones.ObjectZone, net.decimation.mod.server.zones.ObjectZoneList, ObjectZone, ObjectZoneList (+2 more)

### Community 16 - "SchematicPlan: SchematicPlan"
Cohesion: 0.14
Nodes (4): a, Schematic, SchematicPlan, SchematicPlan prop facing FIXED / RANDOM only

### Community 17 - "CLAUDE: deobf/ readable reference tree (decompiled with readable names, read only)"
Cohesion: 0.15
Nodes (17): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, CFR decompiler, Decimation.jar (obfuscated Forge 1.7.10 mod jar), deobf/ readable reference tree (decompiled with readable names, read only), Map file case trap (p_deci_aK.tsv equals p_deci_ak.tsv on APFS), Naming source priority (g*, p_*, r_*, m_*, then zz_auto.tsv), rtk hook drops grep/find flags (use Python os.walk) (+9 more)

### Community 18 - "WorldGenCommand: WorldGenCommand"
Cohesion: 0.21
Nodes (9): Live loop: hotswap code, reload sets, rebuild in place, no restart per change, World, WorldGenCommand, Live editing: /deciworldgen reload + rebuild, -Photswap + tools/hotswap.py (method bodies only), net.minecraft.command.CommandBase, net.minecraft.command.ICommandSender, Override, jdb() (+1 more)

### Community 19 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.19
Nodes (15): CLAUDE.md project guide, dev/libs/Decimation-base.jar (patched jar minus our classes), Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), dev/ RetroFuturaGradle workspace (GTNH ExampleMod1.7.10 template), In place jar entry update (zip -u, never rebuild), Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), MCP readable member names in dev workspace (+7 more)

### Community 20 - "DeciBiome: DeciBiome"
Cohesion: 0.20
Nodes (4): cpw.mods.fml.relauncher.SideOnly, DeciBiome, Override, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 21 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 22 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.18
Nodes (14): Bug: structures built on ocean floor, DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a), 35% armor damage reduction buff, Intro screen skip (deci.i.d.iH, deci.i.c.io flags), ItemArmorDeci.damageMultiplier, Loot fix documentation (documentation.md), 50% ranged weapon damage nerf (+6 more)

### Community 23 - "?: DeciGenLayer"
Cohesion: 0.18
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 24 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.26
Nodes (13): Bug: zones never active in singleplayer (partial fix), ChatHandler (deci.aK.n, radio chat), EntitySpawnZoneHandler (deci.aK.d), PlayerZoneTickHandler (deci.aK.m), SafezoneAttackHandler / FriendlyFireHandler (deci.aK.g, i), ServerCommandRegistrar (deci.aK.o), ServerConfig (deci.aJ.b), ServerConfigLoader (deci.aJ.a) (+5 more)

### Community 25 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 26 - "FurnitureSets: FurnitureSets"
Cohesion: 0.29
Nodes (3): com.google.gson.JsonObject, FurnitureSets, FurnitureSet

### Community 27 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.22
Nodes (7): cpw.mods.fml.common.eventhandler.SubscribeEvent, HumanityKillHandler, VehicleHitHandler, DevPregen, ServerTickEvent, net.minecraftforge.event.entity.living.LivingDeathEvent, net.minecraftforge.event.entity.player.AttackEntityEvent

### Community 28 - "BiomeMap: BiomeMap"
Cohesion: 0.26
Nodes (3): Sectors, BiomeMap, NoiseGeneratorSimplex

### Community 29 - "SealedCaves: net.minecraft.world.biome.BiomeGenBase"
Cohesion: 0.24
Nodes (7): Caves, Override, Ravines, SealedCaves, net.minecraft.world.biome.BiomeGenBase, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 30 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.27
Nodes (8): Bug: armor buff ignores NPC gunfire (fixed), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, ArmorGunfireHandler, net.minecraft.entity.Entity, net.minecraft.entity.player.EntityPlayer, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 31 - "Building: Furniture sets doc: data driven JSON furniture groups, user editable"
Cohesion: 0.20
Nodes (7): v0.20.0 furniture sets, wall lining, corner doors, one sided corridors, live loop, FurnitureSet, Furniture sets doc: data driven JSON furniture groups, user editable, Placement: seeded weighted order, every wall and offset, free cells off walkway, back against wall, no full height piece over a window, Seed 1 placement: kitchen/bath/lobby/closet 100%, bed 84%, living 80%, dining 68%, Steps 1b (lining) and 2 (furniture sets) done for apartments v0.20.0, Steps 1a/1b/2 done for apartments; live loop added; next reference library + critic

### Community 32 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.20
Nodes (12): Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look, Principle: furniture against a wall or partner piece, never floating (except islands: desk clusters, aisles, tables), Principle: function first, one focal object per room placed first (bed, sofa + TV, desk cluster, checkout), Principle: layer order structure -> surfaces -> furniture -> small detail -> story / decay (decay removes or moves, never invents rooms), Principle: no two storeys identical (mirror, room program, furnishing set, decay level per storey) (+4 more)

### Community 33 - "architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)"
Cohesion: 0.18
Nodes (11): DamageSource split: gunDeci (player) vs human/turret (NPC), Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), TraderSpawnManager (server.traders.a), ItemArmorJuggernaut (juggernautHelm/Vest/Pants/Boots) (+3 more)

### Community 34 - "new_feature: Current state and pending decisions"
Cohesion: 0.27
Nodes (11): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Spawn search: suburb, wasteland, overgrown plains and forest added to WorldChunkManager.allowedBiomes, Decimation world type doc (terrain, 0.14.0), Terrain stays vanilla ChunkProviderGenerate, only the biome map is replaced, Decimation world type feature (user chose A over reskinning vanilla / RTG, done v0.14.0, seen in game; open: fog colour, rubble and ash decoration) (+3 more)

### Community 35 - "StructureData: StructureData"
Cohesion: 0.27
Nodes (4): Override, StructureData, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 36 - "interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 37 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.29
Nodes (10): Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only, Decals (blood splats, hazard signs, notes, framed picture, flags, warning signs; flat upright pictures, wall face by meta [not verified]), Graffiti 1..12 (tags and slogans, flat upright pictures), Home, kitchen and shop items (cooking station is a camping stove not an oven, washing machine, stereo, radio, chess table, coca plant, shop display freezer, vending machines single block), Full cube material blocks (BlockBrick_1..3, BlockStone_1..8, BlockMetal_1..3, BlockMetalWall, sandbag stacks, military barrier, packaged cocaine stack), Military and hazard items (military radios, hedgehog, concertina wire, missile launcher, spotlight, hazard lights and barriers, flag poles, radio tower), Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery) (+2 more)

### Community 38 - "building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.31
Nodes (9): Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Exterior add-ons: entrance frame + slab canopy + steps, glass shopfronts with BlockSign_* logo sign, office lobby glazing, apartment balconies, fire escapes over 3 storeys, roof stair hut / water tank / vents / antenna / parapet, downpipes [not verified], Shop facade signs (BlockSign_Decimunition / Gunsrus / Metro / Minebay / Minedonalds / Mineway) (+1 more)

### Community 39 - "graph_update: graph_update.py"
Cohesion: 0.36
Nodes (8): Graph update workflow (graph_update.py prepare/finish, one agent per chunk), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph), finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 40 - "Building: Step 1 DONE v0.17.0: Decimation door in every DOOR plan cell (meta 0/2 wall along z, 1/3 along x, +4 open 25%; 20% + decay * 50% missing; one door type per building), low debris only, never beside a door; seed 1: 373 doors in 105 buildings, storey reachability median 100%"
Cohesion: 0.42
Nodes (4): v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, Step 1 DONE v0.17.0: Decimation door in every DOOR plan cell (meta 0/2 wall along z, 1/3 along x, +4 open 25%; 20% + decay * 50% missing; one door type per building), low debris only, never beside a door; seed 1: 373 doors in 105 buildings, storey reachability median 100%, Step 4d.1 DONE v0.17.0: doors + low debris

### Community 41 - "apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels"
Cohesion: 0.22
Nodes (9): v0.21.0 critic pass 1, kitchen rework, wall breaches by column, narrow blocks ladder, -Pflats audit, Apartment references: real-world clearances, 1.7.10 furniture techniques, review checklist, Apartment review checklist: walkway, function readable, 40-60% free, palette, plausible decay, per room rules, Clearances: 1 block walkway, sofa-table 0-1 block, one free bed side, kitchen work triangle in a 4-6 block run, 1.7.10 techniques: stairs sofas with trapdoor arms, slab coffee tables, cauldron sink + tripwire tap, quartz stair toilet, paintings, wool curtains, Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels, Root cause found: narrow apartment blocks had no flats (core took the width); ladder under 16 wide, Open after pass 1: empty living fallback, wall detail layer, sofa arms, deeper living sets, bath sets, bedroom min width, camera (+1 more)

### Community 42 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 43 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.28
Nodes (4): DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River), Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit

### Community 44 - "FurnitureSet: FurnitureSet"
Cohesion: 0.29
Nodes (4): Entry, FurnitureSet, Set format: layers (floor, +1, under ceiling), row 0 against the wall, palette with face/type, rooms slot, weight, Set palette types trapdoor (cabinet doors) and hook (tap)

### Community 45 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.33
Nodes (7): Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief, Technic modpack Decimation 1.7.10 (linusrhone), decimation_server.properties lootRespawnTime

### Community 47 - "DeadTree: DeadTree"
Cohesion: 0.33
Nodes (3): DeadTree, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree

### Community 48 - "new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)"
Cohesion: 0.38
Nodes (7): Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside), Spec user decisions: survivor camps (about 1 in 12 buildings), shop signs use BlockSign_* assets, reusable parts for later categories, start by fixing the audited apartment / office / shop, Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces), Round 2 step 5 (was step 4): new procedural building types (police station, hospital, gas station, warehouse), then step 6 loot balance per building, User decisions on the interior spec (7 Oktober 2026): survivor camps yes, shop signs from Decimation assets, start with audited apartment / office / shop, later many types in categories, polish built as reusable parts, Worldgen round 2 plan (option A: terrain blending, street life, full multiblocks, new building types, loot balance)

### Community 49 - "new_feature: deciworldgen second @Mod (net.decimation.worldgen)"
Cohesion: 0.38
Nodes (7): deciworldgen second @Mod (net.decimation.worldgen), dist/ separate jars (deciworldgen-0.7.0.jar + Decimation.jar), Generator crash hardening (catch all, self disable after 3 errors), tools/make_test_schematics.py (8 schematics), MarkerGenerator (milestone 2 glowstone pillars), Schematic.java (.schematic loader), StructureGenerator.java (cell based placement)

### Community 50 - "interior_spec: Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs"
Cohesion: 0.33
Nodes (6): v0.19.0 storey height 5 with own ceilings, Revised order after user 0.18 review: structure first, then furniture sets, then reference library + critic, Step 1a done v0.19.0: storeys 5 high, own white plaster ceiling layer, 5 step stair runs, Step 1b next: plaster lining inside outer walls, with furniture sets, audit_v0.19: dark tile ceilings first, then white plaster ceilings, Revised interior plan after user review: structure, data driven furniture sets, references + critic; Lost Cities idea not port

### Community 51 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

### Community 52 - "BottlecapHandler: BottlecapHandler.java"
Cohesion: 0.50
Nodes (3): BottlecapHandler (deciworldgen), BottlecapHandler, net.minecraftforge.event.entity.player.EntityItemPickupEvent

### Community 53 - "architecture: BackendConnection (deci.aP.a, kryonet)"
Cohesion: 0.40
Nodes (5): AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), DeathStatsHandler (deci.aK.h), PlayerLoginHandler (deci.aK.l), Central network service network.mcdecimation.net (dead)

### Community 55 - "interior_spec: Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta)"
Cohesion: 0.60
Nodes (5): Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Door decay: 25 to 50% missing, a few left open (meta bit 4), one barricaded in the most decayed buildings, Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta), Doors use vanilla door metadata (copy BlockDoor without extending it): lower half 0..3 = west / north / east / south edge, upper half 8; place both halves or the door removes itself, Door blocks (Door_Blue / Emergency / Green / Orange 1..3, Door_Office_1, Door_Metal_3, security doors; 2 high, 1 wide)

### Community 56 - "architecture: TurfManager (server.turf.a)"
Cohesion: 0.67
Nodes (4): Block break/place protection handlers (deci.aK.a, b), ClanManagerV1 (server.clans.a), TurfManager (server.turf.a), Clans and turf system

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
- **59 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+54 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

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
- **Why does `Building` connect `Building: Building` to `StructureGenerator: StructureGenerator`, `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`, `interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents`, `Building: Step 1 DONE v0.17.0: Decimation door in every DOOR plan cell (meta 0/2 wall along z, 1/3 along x, +4 open 25%; 20% + decay * 50% missing; one door type per building), low debris only, never beside a door; seed 1: 373 doors in 105 buildings, storey reachability median 100%`, `apartment: Critic pass 1 on v0.20 flats: overall 3/10, verified findings and fixes, critic wrong on chair facing and lintels`, `Building: net.minecraft.block.Block`, `Building: .apartmentUnit()`, `new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)`, `Building: Furniture sets doc: data driven JSON furniture groups, user editable`?**
  _High betweenness centrality (0.194) - this node is a cross-community bridge._
- **Why does `Current state and pending decisions` connect `new_feature: Current state and pending decisions` to `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`, `worldgen: World generation doc (deciworldgen)`, `Test Schematic Builder`, `building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)`, `architecture: LootInteractHandler (deci.aK.k)`, `bug: Bug tracker (bug.md)`, `Building: Step 1 DONE v0.17.0: Decimation door in every DOOR plan cell (meta 0/2 wall along z, 1/3 along x, +4 open 25%; 20% + decay * 50% missing; one door type per building), low debris only, never beside a door; seed 1: 373 doors in 105 buildings, storey reachability median 100%`, `bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `Rotation: Rotation`, `ZoneStore: ZoneStore`, `new_feature: deciworldgen second @Mod (net.decimation.worldgen)`, `ZoneSpawnHandler: ZoneSpawnHandler`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `bug: Bug: armor buff ignores NPC gunfire (fixed)`?**
  _High betweenness centrality (0.128) - this node is a cross-community bridge._