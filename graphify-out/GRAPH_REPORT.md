# Graph Report - .  (2026-10-07)

## Corpus Check
- 11 files · ~68,155 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 848 nodes · 1865 edges · 53 communities (42 shown, 11 thin omitted)
- Extraction: 82% EXTRACTED · 17% INFERRED · 0% AMBIGUOUS · INFERRED: 323 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- worldgen: World generation doc (deciworldgen)
- CLAUDE: decimation-singleplayer README (public repo overview)
- DevAutoTest: DevAutoTest
- Building: Building
- building_design: Procedural building design doc (city blocks, Building v2)
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- Test Schematic Builder
- prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)
- worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole
- StructureGenerator: StructureGenerator
- DecimationWorldGen: DecimationWorldGen
- bug: Bug tracker (bug.md)
- architecture: ServerProxy (deci.a.e, dedicated only)
- new_feature: Feature tracker (new_feature.md)
- SchematicPlan: SchematicPlan
- ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)
- Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)
- LargeSites: net.minecraft.world.World
- DeciBiome: DeciBiome
- architecture: LootInteractHandler (deci.aK.k)
- Name Mapping Applier
- DeciGenLayer: DeciGenLayer
- architecture: Obfuscation map (package to meaning)
- CLAUDE: tools/build.py real javac pipeline
- Slices: Slices
- Plan: Plan
- BiomeMap: BiomeMap
- SealedCaves: SealedCaves
- documentation: Feature tracker (new_feature.md)
- StructureData: StructureData
- architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)
- DecimationBiomes: DecimationBiomes
- graph_update: graph_update.py
- Heuristic Auto Namer
- architecture: BackendConnection (deci.aP.a, kryonet)
- DeadTree: DeadTree
- ?: StructureGenerator.java
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
1. `Building` - 61 edges
2. `StructureGenerator` - 44 edges
3. `DevAutoTest` - 34 edges
4. `World generation doc (deciworldgen)` - 27 edges
5. `Bug tracker (bug.md)` - 26 edges
6. `Current state and pending decisions` - 25 edges
7. `Procedural building design doc (city blocks, Building v2)` - 24 edges
8. `Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)` - 22 edges
9. `DeciBiome` - 21 edges
10. `SchematicPlan` - 20 edges

## Surprising Connections (you probably didn't know these)
- `ZoneStore (per world deciworldgen_zones.json)` --references--> `ZoneStore`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/ZoneStore.java
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `VehicleHitHandler (v0.8.1)` --references--> `VehicleHitHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/VehicleHitHandler.java
- `Bug: zones never active in singleplayer (partial fix)` --references--> `ZoneSpawnHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java
- `ZoneSpawnHandler` --references--> `ZoneSpawnHandler`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Supply drop landing safety: falling crate dies on non replaceable cells (flowers, props), fixed by clearLanding and skipping prop topped columns** — bug_supply_drop_flower_vanish, bug_supply_drop_street_prop_vanish, bug_falling_block_replaceable_landing, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_clearlanding, dev_src_main_java_net_decimation_fixes_supplydropscheduler_supplydropscheduler_drop [EXTRACTED 1.00]
- **Building quality audit toolchain (floor plans with reachability, audit screenshots, view cells, indexed results)** — docs_building_audit_audit_method, tools_floorplan, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_serveauditview, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_pickauditbuildings, dev_src_main_java_net_decimation_worldgen_building_building_viewcell, docs_shots_index_audit_v0_16, docs_shots_index_plans_v0_16 [INFERRED 0.85]
- **Visual evidence memory (autotest screenshot modes, docs/shots, written shots index, prop catalogue)** — claude_visual_evidence_rule, claude_autotest_screenshot_modes, docs_shots_index_shots_index, docs_prop_catalogue_prop_catalogue_doc, dev_src_main_java_net_decimation_worldgen_devautotest_devautotest_servegalleryview [INFERRED 0.85]
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

## Communities (53 total, 11 thin omitted)

### Community 0 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.05
Nodes (47): Ladder at (W-2, L-2) hangs on a back wall cell that can be a window, a decay hole or not yet written (next population window); a block update pops it off, Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Knowledge index (docs/, deobf notes, names.tsv, trackers), Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), Rotation, Schematic, DecimationWorldType (+39 more)

### Community 1 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.05
Nodes (55): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Case sensitive volume extraction (+47 more)

### Community 2 - "DevAutoTest: DevAutoTest"
Cohesion: 0.08
Nodes (24): EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), SupplyDropScheduler.drop skips a candidate column whose top block has a tile entity (prop, chest, car) and tries the next of its 12 random positions, Bug: supply drop crate vanished on a street prop (trash bag on a sidewalk, fixed v0.16.0), Autotest screenshot modes (default 3 street views, -Paudit building audit, -Pgallery every Decimation block 3 per shot, -Ponly= re-shoots single views; peaceful, mobs removed, camera locked per tick, fov / gamma restored), Autotest ends with 3 city street screenshots (dev/run/client/screenshots/autotest_<n>.png: along the street, street light side on, across); read them to check visuals instead of asking the user; -Ddeciworldgen.autotest.views=false skips them, Visual evidence rule: keep screenshots in docs/shots/<topic>_v<version>/ (git ignored) and describe them in docs/shots_index.md; read the index, re-shoot only when the code behind it changed (+16 more)

### Community 3 - "Building: Building"
Cohesion: 0.11
Nodes (6): a, Fix v0.16.1: solid wall cell behind the ladder, no furniture on the 4 cells around it, ladder shaft and stair core (plus 1 block ring) exempt from the collapse, Building, Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Graded, net.minecraft.block.Block

### Community 4 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.11
Nodes (37): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), v0.15.0: street life (levelled street cross sections, dashed centre lines, street lights, benches, bins, trash bags, facing derived from PropRenderer transform), Props, CityDistrict, Finding: rooms have no function (sparse apartment units, empty ground storey units, identical office desk grid on every storey incl. ground, repeated plans on tall buildings, undefined upper shop storey), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes) (+29 more)

### Community 5 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.08
Nodes (21): BottlecapHandler (deciworldgen), cpw.mods.fml.common.eventhandler.SubscribeEvent, BottlecapHandler, HumanityKillHandler, VehicleHitHandler, ZoneSpawnHandler, DevPregen, ServerTickEvent (+13 more)

### Community 6 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.09
Nodes (29): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Interior prop inventory (275 deci: blocks from World.registry(); no toilet, sink, bath, sofa, bed or fridge props), Missing furniture uses vanilla stand-ins (bed, stairs + carpet sofa, quartz stairs / cauldron toilet, cauldron sink and bath, BlockElectricBoxBin or iron block fridge, slab counters with trapdoor cupboards, bookshelf, flower pot), Seed 1 biome share 800x800 around spawn (city 27%, dead wild 26%, suburbs 15%, military 9%, overgrown 21%, river 1%) (+21 more)

### Community 7 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 8 - "prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)"
Cohesion: 0.09
Nodes (29): Bug: upper storeys unreachable (ladder popped off, stair core collapsed; fixed v0.16.1), BlockRegistry (deci.aD.c / g), Building quality audit (round 2 step 4a, baseline of v0.16.0 city buildings), Exterior findings: flat facades (no balconies, canopy, shopfront glazing, shop signs) and bare roofs (no vents, water tank, antenna, stair hut, AC units), Finding: rubble is full mossy cobblestone cubes in corridors, rooms and doorways (reads as noise, cuts reachability), Finding: no doors anywhere, only gaps (14 Decimation door blocks unused), Finding: no story details (barricades, skeletons, body bags, blood decals, notes, graffiti, survivor camps, looted crates all exist and are unused), Finding: one interior material everywhere (birch plank walls, oak plank floors and ceilings), no ceilings, lighting, carpets or tiles (+21 more)

### Community 9 - "worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole"
Cohesion: 0.15
Nodes (22): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL), tools/multiscan.py checks them, supply drops skip columns topped by a prop, Multiblock props (deci.W.*), World, MultiblockRepairHandler (+14 more)

### Community 10 - "StructureGenerator: StructureGenerator"
Cohesion: 0.16
Nodes (7): Schematic, StructureGenerator, Dashed centre line (3 on 3 off, deci:BlockRoad_CenterLine, DeciTexturedBlock top texture by metadata % 4; meta 4 north south, 2 east west; never in or next to intersections), Street furniture (BlockStreetLight on the road side sidewalk every 20 blocks, sides staggered by 10, 25% missing; BlockStreetBench 35%; BlockStreetBin 40%; BlockTrashBag1/2 4%; corners empty), Levelled streets (every street and sidewalk column takes its street centre column height at the same position; intersections use the intersection centre; cut / fill up to 6 blocks), Street furniture props (street lights, benches, bins, trash bags, dumpster, mailbox, news stands, phone booths, traffic lights, power and electric poles, cones, barriers, road signs, metro railings and gates), LargeSites

### Community 11 - "DecimationWorldGen: DecimationWorldGen"
Cohesion: 0.16
Nodes (12): CityDistrict, cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, DecimationWorldGen, Placeholder blocks to Decimation props (sponge, gold, lapis, diamond, emerald, iron, coal, wool and stained clay colours), FMLPostInitializationEvent, Mod.EventHandler (+4 more)

### Community 12 - "bug: Bug tracker (bug.md)"
Cohesion: 0.14
Nodes (21): Bug: bottlecaps not converted to currency (fixed), Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: loot never worked in singleplayer, Bug: no supply drops in singleplayer (fixed v0.9.0) (+13 more)

### Community 13 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.16
Nodes (20): Bug: zones never active in singleplayer (partial fix), Block break/place protection handlers (deci.aK.a, b), ChatHandler (deci.aK.n, radio chat), ClanManagerV1 (server.clans.a), EntitySpawnZoneHandler (deci.aK.d), LootCooldownRegistry (deci.aB.e), PlayerZoneTickHandler (deci.aK.m), SafezoneAttackHandler / FriendlyFireHandler (deci.aK.g, i) (+12 more)

### Community 14 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.21
Nodes (20): .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path), Techne cuboid model editor (+12 more)

### Community 15 - "SchematicPlan: SchematicPlan"
Cohesion: 0.14
Nodes (4): a, Schematic, SchematicPlan, SchematicPlan prop facing FIXED / RANDOM only

### Community 16 - "ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.18
Nodes (13): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, DeciDamageSources (deci.aD.h) (+5 more)

### Community 17 - "Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)"
Cohesion: 0.15
Nodes (7): Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), v0.13.0: terrain blending (graded city lots via Graded / Building.grade, front yard car parks, no falling block fill, supply drops clear flowers), Graded, Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing), Never fill with falling blocks (dirt under grass, sandstone under sand, stone under gravel), Foundation down to the ground (max 12, Plan.foundation): stone brick plinth for buildings, dirt for schematics, Terrain blending (round 2 step 1, done v0.13.0, headless verified, not yet seen in game)

### Community 18 - "LargeSites: net.minecraft.world.World"
Cohesion: 0.22
Nodes (4): Schematic, LargeSites, Sub, net.minecraft.world.World

### Community 19 - "DeciBiome: DeciBiome"
Cohesion: 0.19
Nodes (4): cpw.mods.fml.relauncher.SideOnly, DeciBiome, Override, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 20 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.15
Nodes (15): Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), DeciConstants (deci.Q.c, GUI ids), IntRange (deci.aB.a, off by one rolls), LootCloseCallback (deci.aB.b), LootInteractHandler (deci.aK.k), LootPool (deci.aB.c), LootTable (deci.aD.l, hardcoded pools l$1..l$12) (+7 more)

### Community 21 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 22 - "DeciGenLayer: DeciGenLayer"
Cohesion: 0.17
Nodes (8): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, net.minecraftforge.event.terraingen.InitMapGenEvent, Populate

### Community 23 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.24
Nodes (14): Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, GunItem (deci.ay.i), GunItem.setDamage am(int) chokepoint, LootTrackedItem (deci.ao.c), MenuFakeWorld (deci.e.d) (+6 more)

### Community 24 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 25 - "Slices: Slices"
Cohesion: 0.27
Nodes (5): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Slices, Lake rules (none in city / military, 1 in 4 in other dead biomes, vanilla rate in overgrown, no surface lava pools), Floor height sampling (5x5 grid over the whole footprint where chunks exist plus 9 soilTop points in the window, median, maxSpread buildings 12 / schematics 7, stored in StructureData), Plan

### Community 27 - "BiomeMap: BiomeMap"
Cohesion: 0.26
Nodes (3): Sectors, BiomeMap, NoiseGeneratorSimplex

### Community 28 - "SealedCaves: SealedCaves"
Cohesion: 0.22
Nodes (8): Caves, Override, Ravines, SealedCaves, Ravines cut 40 block trenches through flat cities; SealedCaves (InitMapGenEvent) digs nothing above y 50 under city and military biomes, Limit: steep lots with a narrow yard still end in a step at the lot edge (worst 17 blocks); grader never touches sidewalks, streets or lot gaps, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine

### Community 29 - "documentation: Feature tracker (new_feature.md)"
Cohesion: 0.21
Nodes (12): Bug: structures built on ocean floor, 35% armor damage reduction buff, Intro screen skip (deci.i.d.iH, deci.i.c.io flags), ItemArmorDeci.damageMultiplier, Loot fix documentation (documentation.md), 50% ranged weapon damage nerf, Buff armor damage reduction 35% (damageMultiplier x0.65), Feature tracker (new_feature.md) (+4 more)

### Community 30 - "StructureData: StructureData"
Cohesion: 0.27
Nodes (4): Override, StructureData, net.minecraft.nbt.NBTTagCompound, net.minecraft.world.WorldSavedData

### Community 31 - "architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)"
Cohesion: 0.20
Nodes (10): Bug: large ammo crate NPE (dead field avk), BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), ItemRegistry (deci.aD.k), TraderSpawnManager (server.traders.a), ItemArmorJuggernaut (juggernautHelm/Vest/Pants/Boots), Planned: more clothing variety on NPCs (+2 more)

### Community 32 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.27
Nodes (4): DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River), net.minecraft.world.biome.BiomeGenBase

### Community 33 - "graph_update: graph_update.py"
Cohesion: 0.36
Nodes (8): Graph update workflow (graph_update.py prepare/finish, one agent per chunk), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph), finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 34 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 35 - "architecture: BackendConnection (deci.aP.a, kryonet)"
Cohesion: 0.25
Nodes (8): AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), DeathStatsHandler (deci.aK.h), DecimationMod (deci.a.b, @Mod entry), LootCooldownResetHandler (deci.an.n), PlayerLoginHandler (deci.aK.l), Shared event handlers (deci.an.*), Central network service network.mcdecimation.net (dead)

### Community 36 - "DeadTree: DeadTree"
Cohesion: 0.40
Nodes (3): DeadTree, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree

### Community 37 - "?: StructureGenerator.java"
Cohesion: 0.40
Nodes (3): cpw.mods.fml.common.IWorldGenerator, net.minecraft.world.chunk.IChunkProvider, Override

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
- **38 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+33 more)
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
- **Why does `Current state and pending decisions` connect `worldgen: World generation doc (deciworldgen)` to `CLAUDE: decimation-singleplayer README (public repo overview)`, `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `Test Schematic Builder`, `worldgen: Multiblock props (0.16.0): generation completes Decimation multiblocks (tile entity extends deci.W.a MultiblockPart) whole`, `architecture: ServerProxy (deci.a.e, dedicated only)`, `ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)`, `Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)`, `Slices: Slices`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Why does `Building` connect `Building: Building` to `prop_catalogue: Decimation prop catalogue (look, size and facing of every deci: block, from the prop gallery)`, `Graded: Lot grading (city yards, 0.13.0: Graded plans own the 26x26 lot, Slices grades every lot column before writing)`, `DevAutoTest: DevAutoTest`, `building_design: Procedural building design doc (city blocks, Building v2)`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._