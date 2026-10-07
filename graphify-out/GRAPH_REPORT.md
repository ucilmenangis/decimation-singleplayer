# Graph Report - .  (2026-10-07)

## Corpus Check
- 8 files · ~75,163 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 917 nodes · 2065 edges · 62 communities (49 shown, 13 thin omitted)
- Extraction: 81% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 379 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Building: Building
- StructureGenerator: StructureGenerator
- CLAUDE: decimation-singleplayer README (public repo overview)
- worldgen: World generation doc (deciworldgen)
- DeciBiome: DeciBiome
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- DevAutoTest: DevAutoTest
- building_design: Procedural building design doc (city blocks, Building v2)
- Test Schematic Builder
- DecimationWorldGen: DecimationWorldGen
- new_feature: Feature tracker (new_feature.md)
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- ZoneStore: ZoneStore
- SchematicPlan: SchematicPlan
- architecture: ServerProxy (deci.a.e, dedicated only)
- architecture: Obfuscation map (package to meaning)
- architecture: LootInteractHandler (deci.aK.k)
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- Name Mapping Applier
- DeciGenLayer: DeciGenLayer
- SealedCaves: SealedCaves
- CLAUDE: tools/build.py real javac pipeline
- StructureData: StructureData
- interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)
- bug: Bug tracker (bug.md)
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- bug: Bug: armor buff ignores NPC gunfire (fixed)
- ArmorGunfireHandler: ArmorGunfireHandler
- SupplyDropScheduler: Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0)
- interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents
- new_feature: Current state and pending decisions
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- architecture: ServerTickHandler (deci.aK.q)
- new_feature: Feature tracker (new_feature.md)
- building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)
- terrain: Decimation world type doc (terrain, 0.14.0)
- Heuristic Auto Namer
- architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)
- architecture: Recurring root cause: integrated server reports side CLIENT
- Rotation: Rotation
- new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)
- DecimationWorldType: DecimationWorldType
- BottlecapHandler: BottlecapHandler.java
- Schematic: Schematic
- interior_spec: Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta)
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
1. `Building` - 84 edges
2. `StructureGenerator` - 44 edges
3. `DevAutoTest` - 33 edges
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
- `ArmorGunfireHandler` --implements--> `Proposed LivingHurtEvent gunshot damage unification`  [INFERRED]
  dev/src/main/java/net/decimation/fixes/ArmorGunfireHandler.java → bug.md
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md

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

## Communities (62 total, 13 thin omitted)

### Community 0 - "Building: Building"
Cohesion: 0.07
Nodes (20): Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, v0.17.0: step 4d.1, doors in every DOOR cell and low debris (docs/interior_spec.md section 8), v0.18.0 apartment rooms and propFacing fix, v0.19.0 storey height 5 with own ceilings, Building, Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Low debris rule: no full rubble cubes on walkways; stone / cobble / brick slabs, cobwebs, CardboardBoxes, TrashBags; full cubes only under a collapsed ceiling, propFacing was inverted; BlockProp front points 2 E, 3 S, 4 W, 5 N (+12 more)

### Community 1 - "StructureGenerator: StructureGenerator"
Cohesion: 0.07
Nodes (21): a, v0.12.1: whole footprint floor height sampling and dirt fill under schematics, cpw.mods.fml.common.IWorldGenerator, Schematic, LargeSites, Slices, Schematic, StructureGenerator (+13 more)

### Community 2 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.05
Nodes (54): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Case sensitive volume extraction (+46 more)

### Community 3 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.05
Nodes (35): Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), Graded, a, Plan, 1 block margin ring outside walls for exterior vines (SKIP elsewhere) (+27 more)

### Community 4 - "DeciBiome: DeciBiome"
Cohesion: 0.07
Nodes (15): cpw.mods.fml.relauncher.SideOnly, Sectors, BiomeMap, NoiseGeneratorSimplex, DeadTree, Override, DeciBiome, Override (+7 more)

### Community 5 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.08
Nodes (34): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Principle: circulation (free 1 block path door to door, no furniture on a doorway cell or the cell in front; reachability >= 95% per storey), Verification for every change: floorplan.py on 2+ buildings per kind (reachability >= 95%, required room items, no prop on doorway cells), -Paudit shots, wallscan / gradescan / multiscan / autotest green, findings into building_audit.md and shots_index.md, plans_v0.16 floor plans (tools/floorplan.py, 14 px per block, letter per prop) (+26 more)

### Community 6 - "DevAutoTest: DevAutoTest"
Cohesion: 0.11
Nodes (14): Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, ClientTickEvent, DevAutoTest, Audit method: tools/floorplan.py per storey plans with reachability flood fill, plus runClient -Pautotest -Paudit (facade, ground, storey 1, roof of a sample apartment, office, shop); seed 1, Decimation world type, Prop gallery method: every deci: block (minus roads, barriers, sound blocks) on a sky platform at metadata 3, 3 per shot, runClient -Pautotest -Pgallery, views.txt maps view to names, audit_v0.16 building audit shots (apartment, office, shop: facade, ground storey, storey 1, roof), audit_v0.17 building audit shots after step 4d.1 (office wood doors with window in apartment unit doorways, slab debris instead of mossy cubes, cobwebs between desks fixed to walls only, shop stockroom metal door; surfaces still planks / dark ceiling for 4d.2) (+6 more)

### Community 7 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.11
Nodes (37): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Prop TileEntitySpecialRenderers (deci.I.*), Props, CityDistrict, Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit) (+29 more)

### Community 8 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 9 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.14
Nodes (14): CityDistrict, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours), FMLPostInitializationEvent (+6 more)

### Community 10 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.18
Nodes (22): Bug: large ammo crate NPE (dead field avk), .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path) (+14 more)

### Community 11 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.19
Nodes (17): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), World, MultiblockRepairHandler (+9 more)

### Community 12 - "ZoneStore: ZoneStore"
Cohesion: 0.16
Nodes (9): a, ServerTickEvent, ZoneStore, net.decimation.mod.server.zones.ObjectZone, net.decimation.mod.server.zones.ObjectZoneList, ObjectZone, ObjectZoneList, Save (+1 more)

### Community 13 - "SchematicPlan: SchematicPlan"
Cohesion: 0.14
Nodes (4): a, Schematic, SchematicPlan, SchematicPlan prop facing FIXED / RANDOM only

### Community 14 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.19
Nodes (17): Bug: zones never active in singleplayer (partial fix), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), EntitySpawnZoneHandler (deci.aK.d), PlayerZoneTickHandler (deci.aK.m), SafezoneAttackHandler / FriendlyFireHandler (deci.aK.g, i), ServerCommandRegistrar (deci.aK.o) (+9 more)

### Community 15 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.20
Nodes (17): Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, GunItem (deci.ay.i), GunItem.setDamage am(int) chokepoint, IntRange (deci.aB.a, off by one rolls), LootPool (deci.aB.c) (+9 more)

### Community 16 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.15
Nodes (16): Bug: loot GUI never opens, ClientProxy (deci.a.c), DeciConstants (deci.Q.c, GUI ids), DecimationMod (deci.a.b, @Mod entry), LootCloseCallback (deci.aB.b), LootCooldownRegistry (deci.aB.e), LootCooldownResetHandler (deci.an.n), LootInteractHandler (deci.aK.k) (+8 more)

### Community 17 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.18
Nodes (9): Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), cpw.mods.fml.common.eventhandler.SubscribeEvent, HumanityKillHandler, VehicleHitHandler, DevPregen, ServerTickEvent, net.minecraftforge.event.entity.living.LivingDeathEvent (+1 more)

### Community 18 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 19 - "DeciGenLayer: DeciGenLayer"
Cohesion: 0.18
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 20 - "SealedCaves: SealedCaves"
Cohesion: 0.21
Nodes (8): Caves, Override, Ravines, SealedCaves, Ravines cut 40 block trenches through flat cities; SealedCaves (InitMapGenEvent) digs nothing above y 50 under city and military biomes, Limit: steep lots with a narrow yard still end in a step at the lot edge (worst 17 blocks); grader never touches sidewalks, streets or lot gaps, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 21 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 22 - "StructureData: StructureData"
Cohesion: 0.24
Nodes (5): Override, StructureData, NBTTagCompound, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 23 - "interior_spec: Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026)"
Cohesion: 0.20
Nodes (12): Apartment ground storey (not empty): lobby, notice board by the stairs, mailbox outside by the path, furnished ground units, laundry or bike room, Building interior and exterior spec (round 2 step 4c, approved 7 Oktober 2026), Office storey programs, one per storey: open plan desk clusters, cellular offices, meeting rooms, break room, restrooms, server / storage, vacant; ground storey lobby with reception, elevator doors, CCTV, Order of work step 4d / 4e: 1 structure + circulation, 2 surfaces, 3 apartment rooms + lobby, 4 office programs + lobby, 5 shop polish + upper storey, 6 story / decay, 7 exterior; each step implement, verify, commit, user look, Principle: furniture against a wall or partner piece, never floating (except islands: desk clusters, aisles, tables), Principle: function first, one focal object per room placed first (bed, sofa + TV, desk cluster, checkout), Principle: layer order structure -> surfaces -> furniture -> small detail -> story / decay (decay removes or moves, never invents rooms), Principle: no two storeys identical (mirror, room program, furnishing set, decay level per storey) (+4 more)

### Community 24 - "bug: Bug tracker (bug.md)"
Cohesion: 0.25
Nodes (11): Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: loot never worked in singleplayer, setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0) (+3 more)

### Community 25 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.20
Nodes (11): CFR decompiler, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief (+3 more)

### Community 26 - "bug: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.24
Nodes (10): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a), 35% armor damage reduction buff (+2 more)

### Community 27 - "ArmorGunfireHandler: ArmorGunfireHandler"
Cohesion: 0.31
Nodes (6): Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), ArmorGunfireHandler, net.minecraft.entity.Entity, net.minecraft.entity.player.EntityPlayer, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 28 - "SupplyDropScheduler: Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0)"
Cohesion: 0.33
Nodes (5): SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, SupplyDropScheduler, EntityPlayer, net.minecraft.server.MinecraftServer, ServerTickEvent

### Community 29 - "interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents"
Cohesion: 0.24
Nodes (10): A floor block is also the ceiling below: keep floors light, Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles, Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick), Room grid per storey plan (R_CORRIDOR..R_STOCK) decides floors and lights, Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents, Surfaces table per space (planks + FloorCarpet rugs, FloorTiles in kitchens / baths / corridors, WallOffice colour sets, Ceiling_1..4, lights, CeilingVent; interiors no longer copy the facade), Floor and ceiling blocks and fixtures (BlockCeiling_1..4, BlockFloorCarpet_1..6, BlockFloorTiles_1..3, ceiling vents, BlockLight / LightOff, BlockExitLight), Interior wall panel blocks (BlockWallOffice_* colour sets: _Bottom_N skirting course, _Top above) (+2 more)

### Community 30 - "new_feature: Current state and pending decisions"
Cohesion: 0.31
Nodes (10): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), deciworldgen second @Mod (net.decimation.worldgen), Dropped companion mods (Ruins, ezWastelands, GeneratorMods), Generator crash hardening (catch all, self disable after 3 errors), FML IWorldGenerator per chunk hook, tools/make_test_schematics.py (8 schematics), MarkerGenerator (milestone 2 glowstone pillars) (+2 more)

### Community 31 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.29
Nodes (10): Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Story and decay layer per building decay level: looted state (open crates, empty shelves), bodies (skeletons, blood decals, body bags), nature under roof holes and windows, graffiti on ground storey and stairwells only, Decals (blood splats, hazard signs, notes, framed picture, flags, warning signs; flat upright pictures, wall face by meta [not verified]), Graffiti 1..12 (tags and slogans, flat upright pictures), Home, kitchen and shop items (cooking station is a camping stove not an oven, washing machine, stereo, radio, chess table, coca plant, shop display freezer, vending machines single block), Crates and loot containers (ammo crates, medical, military, police, wood crate / open, weapon cabinet, storage crate, care package, supply drop), Full cube material blocks (BlockBrick_1..3, BlockStone_1..8, BlockMetal_1..3, BlockMetalWall, sandbag stacks, military barrier, packaged cocaine stack), Military and hazard items (military radios, hedgehog, concertina wire, missile launcher, spotlight, hazard lights and barriers, flag poles, radio tower) (+2 more)

### Community 32 - "architecture: ServerTickHandler (deci.aK.q)"
Cohesion: 0.25
Nodes (9): Bug: no supply drops in singleplayer (fixed v0.9.0), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), DeathStatsHandler (deci.aK.h), PlayerLoginHandler (deci.aK.l), ServerTickHandler (deci.aK.q), SupplyDropSpawner (deci.aM.c), UpdateChecker (utils.h) (+1 more)

### Community 33 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.28
Nodes (9): Bug: structures built on ocean floor, Intro screen skip (deci.i.d.iH, deci.i.c.io flags), Loot fix documentation (documentation.md), 50% ranged weapon damage nerf, Feature tracker (new_feature.md), Skip intro screens (jumpscare and BoehMod logo gate flags), Underwater placement fix (v0.7.0), Nerf ranged weapon damage 50% (am(int) chokepoint) (+1 more)

### Community 34 - "building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)"
Cohesion: 0.31
Nodes (9): Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), v0.16.1: every storey reachable (ladder support, collapse spares the stairs); building audit, prop gallery, catalogue and spec (round 2 step 4a..c), Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Exterior add-ons: entrance frame + slab canopy + steps, glass shopfronts with BlockSign_* logo sign, office lobby glazing, apartment balconies, fire escapes over 3 storeys, roof stair hut / water tank / vents / antenna / parapet, downpipes [not verified], Shop facade signs (BlockSign_Decimunition / Gunsrus / Metro / Minebay / Minedonalds / Mineway) (+1 more)

### Community 35 - "terrain: Decimation world type doc (terrain, 0.14.0)"
Cohesion: 0.28
Nodes (9): Knowledge index (docs/, interior spec, deobf notes, names.tsv, trackers), worldmap_seed1_v0.14.png: top down map of seed 1, Decimation world type (city sectors, military, suburbs, overgrown forest, rivers stopping at city edges), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Spawn search: suburb, wasteland, overgrown plains and forest added to WorldChunkManager.allowedBiomes, Decimation world type doc (terrain, 0.14.0), Terrain stays vanilla ChunkProviderGenerate, only the biome map is replaced, Decimation world type feature (user chose A over reskinning vanilla / RTG, done v0.14.0, seen in game; open: fog colour, rubble and ash decoration) (+1 more)

### Community 36 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 37 - "architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)"
Cohesion: 0.25
Nodes (8): BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), TraderSpawnManager (server.traders.a), ItemArmorJuggernaut (juggernautHelm/Vest/Pants/Boots), Planned: more clothing variety on NPCs, Planned: civilian, trader fix, spec-ops military mobs, Trader entity family (deci.ai.a base, deci.ai.v armed, ~20 types)

### Community 38 - "architecture: Recurring root cause: integrated server reports side CLIENT"
Cohesion: 0.38
Nodes (7): Bug: bottlecaps not converted to currency (fixed), Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Recurring root cause: integrated server reports side CLIENT, DeathHandler (deci.an.f, humanity isServer gate), Inner isServer() gates in shared code, ItemPickupHandler (deci.aK.e, bottlecaps), LegacyServerEventHandler (deci.aL.a, dead code)

### Community 40 - "new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)"
Cohesion: 0.38
Nodes (7): Extensibility: polish built as reusable parts (shell, room programs, surface sets, door rules, story / decay layer, exterior add-ons); a new type = footprint rule + room programs + facade + loot profile, Survivor camp (about 1 in 12 buildings, one room: lantern, CanFire, bedroll, crates, radio, WaterPallet, barricaded door, note decal, graffiti outside), Spec user decisions: survivor camps (about 1 in 12 buildings), shop signs use BlockSign_* assets, reusable parts for later categories, start by fixing the audited apartment / office / shop, Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces), Round 2 step 5 (was step 4): new procedural building types (police station, hospital, gas station, warehouse), then step 6 loot balance per building, User decisions on the interior spec (7 Oktober 2026): survivor camps yes, shop signs from Decimation assets, start with audited apartment / office / shop, later many types in categories, polish built as reusable parts, Worldgen round 2 plan (option A: terrain blending, street life, full multiblocks, new building types, loot balance)

### Community 41 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

### Community 42 - "BottlecapHandler: BottlecapHandler.java"
Cohesion: 0.50
Nodes (3): BottlecapHandler (deciworldgen), BottlecapHandler, net.minecraftforge.event.entity.player.EntityItemPickupEvent

### Community 44 - "interior_spec: Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta)"
Cohesion: 0.60
Nodes (5): Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Door decay: 25 to 50% missing, a few left open (meta bit 4), one barricaded in the most decayed buildings, Door rules per space (unit entrance Door_Office_1 or coloured _3, bathrooms Door_Blue_1 / Green_1, stair core Door_Emergency_3 with EXIT light, server rooms Door_Metal_3 / security + keypad, shop stockroom metal door; both halves, vanilla meta), Doors use vanilla door metadata (copy BlockDoor without extending it): lower half 0..3 = west / north / east / south edge, upper half 8; place both halves or the door removes itself, Door blocks (Door_Blue / Emergency / Green / Orange 1..3, Door_Office_1, Door_Metal_3, security doors; 2 high, 1 wide)

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
- **48 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+43 more)
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
- **Why does `Building` connect `Building: Building` to `StructureGenerator: StructureGenerator`, `worldgen: World generation doc (deciworldgen)`, `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`, `new_feature: Round 2 step 4: building quality pass before new types (a audit, b prop catalogue, c design spec, d interiors room by room, e exterior polish, f user review in game; 4d.1 doors + low debris DONE v0.17.0, next 4d.2 surfaces)`, `interior_spec: Step 2 surfaces done v0.17.0: floors per room, wall panel set per building, ceiling light panels and vents`?**
  _High betweenness centrality (0.157) - this node is a cross-community bridge._
- **Why does `Current state and pending decisions` connect `new_feature: Current state and pending decisions` to `Building: Building`, `StructureGenerator: StructureGenerator`, `CLAUDE: decimation-singleplayer README (public repo overview)`, `worldgen: World generation doc (deciworldgen)`, `building_audit: Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings)`, `terrain: Decimation world type doc (terrain, 0.14.0)`, `building_design: Procedural building design doc (city blocks, Building v2)`, `Rotation: Rotation`, `Test Schematic Builder`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `ZoneStore: ZoneStore`, `ZoneSpawnHandler: ZoneSpawnHandler`, `architecture: LootInteractHandler (deci.aK.k)`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `bug: Bug: armor buff ignores NPC gunfire (fixed)`, `ArmorGunfireHandler: ArmorGunfireHandler`, `SupplyDropScheduler: Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0)`?**
  _High betweenness centrality (0.138) - this node is a cross-community bridge._