# Graph Report - .  (2026-10-07)

## Corpus Check
- 16 files · ~53,629 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 668 nodes · 1446 edges · 40 communities (29 shown, 11 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 229 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- StructureGenerator: StructureGenerator
- CLAUDE: Bug tracker (bug.md)
- Building: Building
- new_feature: Current state and pending decisions
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- new_feature: Feature tracker (new_feature.md)
- building_design: Procedural building design doc (city blocks, Building v2)
- SchematicPlan: SchematicPlan
- Test Schematic Builder
- worldgen: World generation doc (deciworldgen)
- DevAutoTest: DevAutoTest
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- StructureData: StructureData
- architecture: ServerProxy (deci.a.e, dedicated only)
- Name Mapping Applier
- CLAUDE: tools/build.py real javac pipeline
- architecture: Obfuscation map (package to meaning)
- architecture: LootInteractHandler (deci.aK.k)
- architecture: ServerTickHandler (deci.aK.q)
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- graph_update: graph_update.py
- Heuristic Auto Namer
- architecture: Recurring root cause: integrated server reports side CLIENT
- architecture: TurfManager (server.turf.a)
- ?: a
- ?: CityDistrict
- Weather Type Id Bug
- ?: ServerTickEvent
- ?: EntityPlayer
- ?: ServerTickEvent
- ?: Override
- ?: net.decimation.worldgen.StructureGenerator.Sub
- ?: Props
- ?: Schematic
- ?: Sub

## God Nodes (most connected - your core abstractions)
1. `Building` - 58 edges
2. `StructureGenerator` - 37 edges
3. `World generation doc (deciworldgen)` - 25 edges
4. `Bug tracker (bug.md)` - 24 edges
5. `Procedural building design doc (city blocks, Building v2)` - 22 edges
6. `Current state and pending decisions` - 22 edges
7. `SchematicPlan` - 20 edges
8. `DevAutoTest` - 20 edges
9. `Grid` - 19 edges
10. `Feature tracker (new_feature.md)` - 19 edges

## Surprising Connections (you probably didn't know these)
- `ZoneStore (per world deciworldgen_zones.json)` --references--> `ZoneStore`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/worldgen/ZoneStore.java
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick)` --references--> `Building`  [INFERRED]
  docs/building_design.md → dev/src/main/java/net/decimation/worldgen/Building.java
- `Bug: zones never active in singleplayer (partial fix)` --references--> `ZoneSpawnHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java
- `ZoneSpawnHandler` --references--> `ZoneSpawnHandler`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_building_building, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_carmeta, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_building_building_grade, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]

## Communities (40 total, 11 thin omitted)

### Community 0 - "StructureGenerator: StructureGenerator"
Cohesion: 0.08
Nodes (16): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, cpw.mods.fml.common.IWorldGenerator, Graded, Schematic, LargeSites, Plan, Slices, a (+8 more)

### Community 1 - "CLAUDE: Bug tracker (bug.md)"
Cohesion: 0.05
Nodes (56): Bug tracker (bug.md), Bug: ClassCastException deci.a.c to deci.a.e, Bug: CustomSkinLoader coremod crash, LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, Bug: loot GUI never opens, Bug: loot never worked in singleplayer, tools/patches/PatchSwing.java, Report: FPS drop in prop dense areas (+48 more)

### Community 2 - "Building: Building"
Cohesion: 0.11
Nodes (5): Building, a, Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys), Foundation down to the ground (max 12, Plan.foundation): stone brick plinth for buildings, dirt for schematics, net.minecraft.block.Block

### Community 3 - "new_feature: Current state and pending decisions"
Cohesion: 0.06
Nodes (34): Bug: armor buff ignores NPC gunfire (fixed), DamageSource split: gunDeci (player) vs human/turret (NPC), EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation (+26 more)

### Community 4 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.07
Nodes (23): BottlecapHandler (deciworldgen), Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), cpw.mods.fml.common.eventhandler.SubscribeEvent, VehicleEntity (deci.ad.e) and parts, BottlecapHandler, HumanityKillHandler, VehicleHitHandler (+15 more)

### Community 5 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.08
Nodes (42): Bug: large ammo crate NPE (dead field avk), Bug: structures built on ocean floor, .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne) (+34 more)

### Community 6 - "building_design: Procedural building design doc (city blocks, Building v2)"
Cohesion: 0.11
Nodes (36): v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), Props, CityDistrict, Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Procedural building design doc (city blocks, Building v2), Car wreck model axis: long axis along x at 0 degrees (confirmed in game 2026-10-07) (+28 more)

### Community 7 - "SchematicPlan: SchematicPlan"
Cohesion: 0.08
Nodes (15): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Schematic, a, Schematic (+7 more)

### Community 8 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 9 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.10
Nodes (20): Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), a, Plan, Adding community schematics (prefix, folder, full restart, new chunks only), Cell grid (4x4 chunks, one small schematic or one city block), Large schematics (up to 120x120, per site chance, placed inside the site), Safe population window (chunk cx,cz writes only [cx*16+8, cx*16+23]), Filename prefix pools (civ_, city_, mil_, untagged = any sector) (+12 more)

### Community 10 - "DevAutoTest: DevAutoTest"
Cohesion: 0.14
Nodes (9): ClientTickEvent, SupplyDropScheduler, DevAutoTest, EntityPlayer, EntityPlayerMP, net.minecraft.server.MinecraftServer, net.minecraft.world.WorldServer, ObjectZone (+1 more)

### Community 11 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.13
Nodes (20): Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest), worldcheck World.registry() maps block names to ids from level.dat (BlockWreckage1..5 = 176..180, id >= 256 is not a mod block test), Map block ids via level.dat FML.ItemData (Decimation ids can be below 256), Seed 1 test world (city blocks near spawn, mil_compound at -62,65,434), Worldgen testing without a player (servertest pregen, wallscan, worldcheck) (+12 more)

### Community 12 - "StructureData: StructureData"
Cohesion: 0.13
Nodes (13): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), Multiblock props (deci.W.*), MultiblockRepairHandler, Override, StructureData (+5 more)

### Community 13 - "architecture: ServerProxy (deci.a.e, dedicated only)"
Cohesion: 0.21
Nodes (15): Bug: zones never active in singleplayer (partial fix), ChatHandler (deci.aK.n, radio chat), EntitySpawnZoneHandler (deci.aK.d), PlayerZoneTickHandler (deci.aK.m), SafezoneAttackHandler / FriendlyFireHandler (deci.aK.g, i), ServerCommandRegistrar (deci.aK.o), ServerConfig (deci.aJ.b), ServerConfigLoader (deci.aJ.a) (+7 more)

### Community 14 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 15 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 16 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.26
Nodes (13): Obfuscation map (package to meaning), 8 agent deobfuscation naming pass, Subsystem taxonomy (core, proxy, network, loot, zone, ...), Decimation architecture notes, GunItem (deci.ay.i), GunItem.setDamage am(int) chokepoint, MenuFakeWorld (deci.e.d), MessageRegistry (deci.aD.n) (+5 more)

### Community 17 - "architecture: LootInteractHandler (deci.aK.k)"
Cohesion: 0.21
Nodes (13): DeciConstants (deci.Q.c, GUI ids), DecimationMod (deci.a.b, @Mod entry), LootCloseCallback (deci.aB.b), LootCooldownRegistry (deci.aB.e), LootCooldownResetHandler (deci.an.n), LootInteractHandler (deci.aK.k), LootTable (deci.aD.l, hardcoded pools l$1..l$12), PacketLootInventory (deci.aE.a$R) (+5 more)

### Community 18 - "architecture: ServerTickHandler (deci.aK.q)"
Cohesion: 0.18
Nodes (12): Bug: no supply drops in singleplayer (fixed v0.9.0), AntiCheatScanner (deci.aN.a), BackendConnection (deci.aP.a, kryonet), DeathStatsHandler (deci.aK.h), IntRange (deci.aB.a, off by one rolls), LootPool (deci.aB.c), LootTrackedItem (deci.ao.c), PlayerLoginHandler (deci.aK.l) (+4 more)

### Community 19 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.22
Nodes (10): CFR decompiler, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled), Hypotheses: dedicated gate, YAML config path, dedicated lifecycle event, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief (+2 more)

### Community 20 - "graph_update: graph_update.py"
Cohesion: 0.36
Nodes (8): Graph update workflow (graph_update.py prepare/finish, one agent per chunk), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph), finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 21 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 22 - "architecture: Recurring root cause: integrated server reports side CLIENT"
Cohesion: 0.38
Nodes (7): Bug: bottlecaps not converted to currency (fixed), Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Recurring root cause: integrated server reports side CLIENT, DeathHandler (deci.an.f, humanity isServer gate), Inner isServer() gates in shared code, ItemPickupHandler (deci.aK.e, bottlecaps), LegacyServerEventHandler (deci.aL.a, dead code)

### Community 23 - "architecture: TurfManager (server.turf.a)"
Cohesion: 0.67
Nodes (4): Block break/place protection handlers (deci.aK.a, b), ClanManagerV1 (server.clans.a), TurfManager (server.turf.a), Clans and turf system

## Ambiguous Edges - Review These
- `addBox passes Y as Z origin (use addShape)` → `Genuinely new model recipe (needs Techne)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to

## Knowledge Gaps
- **30 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+25 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `addBox passes Y as Z origin (use addShape)` and `Genuinely new model recipe (needs Techne)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Current state and pending decisions` connect `new_feature: Current state and pending decisions` to `StructureGenerator: StructureGenerator`, `CLAUDE: Bug tracker (bug.md)`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `building_design: Procedural building design doc (city blocks, Building v2)`, `Test Schematic Builder`, `DevAutoTest: DevAutoTest`, `StructureData: StructureData`, `architecture: LootInteractHandler (deci.aK.k)`?**
  _High betweenness centrality (0.174) - this node is a cross-community bridge._
- **Why does `v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status` connect `building_design: Procedural building design doc (city blocks, Building v2)` to `StructureGenerator: StructureGenerator`, `CLAUDE: Bug tracker (bug.md)`, `Building: Building`, `new_feature: Current state and pending decisions`, `SchematicPlan: SchematicPlan`, `worldgen: World generation doc (deciworldgen)`, `worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)`, `StructureData: StructureData`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Why does `Bug tracker (bug.md)` connect `CLAUDE: Bug tracker (bug.md)` to `new_feature: Current state and pending decisions`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `new_feature: Feature tracker (new_feature.md)`, `worldgen: World generation doc (deciworldgen)`, `StructureData: StructureData`, `architecture: ServerProxy (deci.a.e, dedicated only)`, `architecture: ServerTickHandler (deci.aK.q)`, `graph_update: graph_update.py`, `architecture: Recurring root cause: integrated server reports side CLIENT`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Building` (e.g. with `Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick)` and `City v2 (v0.12.0): size variety, researched interiors, biome decay, sidewalks, street aligned cars (axis fixed in v0.12.2, fewer cars)`) actually correct?**
  _`Building` has 3 INFERRED edges - model-reasoned connections that need verification._