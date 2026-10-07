# Graph Report - .  (2026-10-07)

## Corpus Check
- 20 files · ~58,099 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 764 nodes · 1642 edges · 54 communities (42 shown, 12 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 245 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- architecture: Obfuscation map (package to meaning)
- StructureGenerator: StructureGenerator
- Building: Building
- worldgen: World generation doc (deciworldgen)
- SchematicPlan: SchematicPlan
- worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)
- Test Schematic Builder
- new_feature: Feature tracker (new_feature.md)
- StructureData: StructureData
- ZoneStore: ZoneStore
- ?: cpw.mods.fml.common.eventhandler.SubscribeEvent
- DevAutoTest: DevAutoTest
- DeciBiome: DeciBiome
- Name Mapping Applier
- CLAUDE: tools/build.py real javac pipeline
- BiomeMap: BiomeMap
- DeciGenLayer: DeciGenLayer
- SealedCaves: SealedCaves
- ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)
- bug: Bug tracker (bug.md)
- documentation: Feature tracker (new_feature.md)
- architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)
- bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)
- CLAUDE: deobf/ readable reference tree (decompiled with readable names, read only)
- SupplyDropScheduler: SupplyDropScheduler
- DecimationBiomes: DecimationBiomes
- new_feature: Current state and pending decisions
- Heuristic Auto Namer
- CLAUDE: decimation-singleplayer README (public repo overview)
- terrain: Decimation world type doc (terrain, 0.14.0)
- VehicleHitHandler: VehicleHitHandler
- DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer
- Rotation: Rotation
- DeadTree: DeadTree
- graph_update: graph_update.py
- CLAUDE: CLAUDE.md project guide
- CLAUDE: Javassist bytecode patcher
- DecimationWorldType: DecimationWorldType
- BottlecapHandler: BottlecapHandler.java
- Schematic: Schematic
- Weather Type Id Bug
- ?: ServerTickEvent
- ?: EntityPlayer
- ?: ServerTickEvent
- ?: a
- ?: net.decimation.worldgen.StructureGenerator.Sub
- ?: Override
- ?: Schematic
- ?: Sub

## God Nodes (most connected - your core abstractions)
1. `Building` - 58 edges
2. `StructureGenerator` - 39 edges
3. `World generation doc (deciworldgen)` - 26 edges
4. `Bug tracker (bug.md)` - 24 edges
5. `DevAutoTest` - 23 edges
6. `Current state and pending decisions` - 23 edges
7. `Procedural building design doc (city blocks, Building v2)` - 22 edges
8. `DeciBiome` - 21 edges
9. `SchematicPlan` - 20 edges
10. `Feature tracker (new_feature.md)` - 20 edges

## Surprising Connections (you probably didn't know these)
- `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit` --semantically_similar_to--> `Recurring root cause: integrated server reports side CLIENT`  [AMBIGUOUS] [semantically similar]
  docs/terrain.md → CLAUDE.md
- `Wall / trim / accent palettes from vanilla 1.7.10 blocks (brick, clays, sandstone, quartz, stone brick)` --references--> `Building`  [INFERRED]
  docs/building_design.md → dev/src/main/java/net/decimation/worldgen/Building.java
- `DevAutoTest` --implements--> `Autotest forces pauseOnLostFocus false`  [INFERRED]
  dev/src/main/java/net/decimation/worldgen/DevAutoTest.java → CLAUDE.md
- `Bug: zones never active in singleplayer (partial fix)` --references--> `ZoneSpawnHandler`  [INFERRED]
  bug.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java
- `ZoneSpawnHandler` --references--> `ZoneSpawnHandler`  [INFERRED]
  new_feature.md → dev/src/main/java/net/decimation/fixes/ZoneSpawnHandler.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.12.1 hillside foundation fix (whole footprint sampling, median floor, dirt foundation under schematics)** — claude_v0_12_1_footprint_floor_sampling, docs_worldgen_floor_height_sampling, docs_worldgen_stone_brick_foundation, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan_foundation [INFERRED 0.85]
- **Saved Javassist patch sources and the rules for applying them** — claude_three_jar_patch_rule, claude_patch_from_original_classes_rule, claude_javassist_patcher, tools_patches_patchswing, tools_patches_patchpropculling [INFERRED 0.85]
- **Slice placement flow for structures larger than the population window** — docs_worldgen_population_window, docs_worldgen_slice_placement, docs_worldgen_floor_height_sampling, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_plan_plan, dev_src_main_java_net_decimation_worldgen_structuredata_structuredata, dev_src_main_java_net_decimation_worldgen_building_building, dev_src_main_java_net_decimation_worldgen_schematicplan_schematicplan [EXTRACTED 1.00]
- **Worldgen test tooling without a player** — tools_servertest, tools_wallscan, tools_worldcheck, dev_src_main_java_net_decimation_worldgen_devpregen_devpregen, tools_make_test_schematics, docs_worldgen_seed1_test_world [EXTRACTED 1.00]
- **Terrain blending v0.13.0 (lot grading, Graded plans, Building.grade, CityDistrict lot placement, gradescan check)** — docs_worldgen_lot_grading, dev_src_main_java_net_decimation_worldgen_graded_graded, dev_src_main_java_net_decimation_worldgen_building_building_grade, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_worldgen_citydistrict_citydistrict_plan, tools_gradescan, bug_graded_sand_cave_fall [EXTRACTED 1.00]
- **Decimation world type biome pipeline (TerrainEvents swaps GenLayers, DeciGenLayer reads BiomeMap, BiomeMap uses shared Sectors, DecimationBiomes defines the biomes)** — dev_src_main_java_net_decimation_worldgen_terrain_terrainevents_terrainevents, dev_src_main_java_net_decimation_worldgen_terrain_decigenlayer_decigenlayer, dev_src_main_java_net_decimation_worldgen_terrain_biomemap_biomemap, dev_src_main_java_net_decimation_worldgen_sectors_sectors, dev_src_main_java_net_decimation_worldgen_terrain_decimationbiomes_decimationbiomes, dev_src_main_java_net_decimation_worldgen_terrain_decimationworldtype_decimationworldtype [EXTRACTED 1.00]
- **Singleplayer bugs from ServerProxy only registration or isServer gates** — claude_singleplayer_side_root_cause_pattern, bug_loot_singleplayer_crash, bug_loot_gui_never_opens, bug_no_supply_drops_singleplayer, bug_humanity_kill_singleplayer, bug_zones_inactive_singleplayer, bug_bottlecap_currency, deobf_notes_architecture_serverproxy [INFERRED 0.95]
- **Prop culling failure and fix (PropRenderer, LineOfSight corner rays, 1x1 unrotated render box, PatchPropCulling)** — bug_props_not_rendered, deobf_notes_architecture_prop_tesr_renderers, bug_lineofsight_corner_rays, bug_prop_render_bounding_box_1x1, tools_patches_patchpropculling_patchpropculling [INFERRED 0.95]
- **Multiblock master fix (v0.12.3): render gate, setSelfMaster in placers, repair on chunk load** — bug_generated_metal_shelves_invisible, bug_multiblock_master_render_gate, deobf_notes_architecture_multiblock_props, dev_src_main_java_net_decimation_worldgen_slices_slices, dev_src_main_java_net_decimation_fixes_multiblockrepairhandler_multiblockrepairhandler, claude_v0_12_3_multiblock_master_rule [EXTRACTED 1.00]
- **Street aligned car wreck facing (placement rule, metadata facing, renderer, axis assumption)** — docs_building_design_street_car_placement, docs_building_design_prop_facing_metadata, docs_building_design_car_model_axis_assumption, docs_worldgen_street_car_wrecks, dev_src_main_java_net_decimation_worldgen_structuregenerator_structuregenerator_carmeta, deobf_notes_architecture_prop_tesr_renderers [INFERRED 0.85]
- **Gun registration builder chain** — create_weapons_gun_registration_pattern, deobf_notes_architecture_gunitem, deobf_notes_architecture_gunstats, create_weapons_weapon_category_enum, create_weapons_fire_mode_enum, deobf_notes_architecture_gunitem_setdamage [EXTRACTED 1.00]
- **ServerProxy-only logic absent in singleplayer** — deobf_notes_architecture_serverproxy, deobf_notes_architecture_servertickhandler, deobf_notes_architecture_itempickuphandler, deobf_notes_architecture_entityspawnzonehandler, deobf_notes_architecture_playerzonetickhandler, deobf_notes_architecture_safezoneattackhandler, deobf_notes_architecture_servercommandregistrar, deobf_notes_architecture_zonemanager, deobf_notes_architecture_supplydropspawner, claude_singleplayer_side_root_cause_pattern [EXTRACTED 1.00]
- **Right click loot flow (interact, cooldown, pool, packet, delayed GUI)** — deobf_notes_architecture_lootinteracthandler, deobf_notes_architecture_lootcooldownregistry, deobf_notes_architecture_loottable, deobf_notes_architecture_lootpool, deobf_notes_architecture_packetlootinventory, deobf_notes_architecture_tickscheduler, deobf_notes_architecture_deciconstants [EXTRACTED 1.00]
- **Gun model, animation and registration pipeline** — deobf_notes_architecture_gunitem, deobf_notes_architecture_itemregistry, docs_gun_model_spec_bmodelloader, docs_gun_model_spec_bmodel_format, docs_gun_model_spec_gunanimation, docs_gun_model_spec_anib_format, docs_gun_model_spec_gunitemrenderer, create_weapons_gun_registration_pattern [EXTRACTED 1.00]

## Communities (54 total, 12 thin omitted)

### Community 0 - "architecture: Obfuscation map (package to meaning)"
Cohesion: 0.05
Nodes (69): Bug: bottlecaps not converted to currency (fixed), Bug: ClassCastException deci.a.c to deci.a.e, Bug: humanity never changed from ordinary kills in singleplayer (fixed v0.9.0), Bug: loot GUI never opens, Bug: loot never worked in singleplayer, Bug: no supply drops in singleplayer (fixed v0.9.0), Bug: zones never active in singleplayer (partial fix), Obfuscation map (package to meaning) (+61 more)

### Community 1 - "StructureGenerator: StructureGenerator"
Cohesion: 0.07
Nodes (18): a, CityDistrict, cpw.mods.fml.common.IWorldGenerator, CityDistrict, Graded, Schematic, LargeSites, Plan (+10 more)

### Community 2 - "Building: Building"
Cohesion: 0.07
Nodes (10): v0.12.1: whole footprint floor height sampling and dirt fill under schematics, Building, a, Props, a, Plan, Biome overgrowth (temperate vines and moss, jungle heavy vines, snowy snow layers, dry sand drifts and dead bushes), Decay model (level 0.15 to 0.55, wall holes, cracked and mossy blocks, broken windows, rubble, corner collapse over 1 to 3 storeys) (+2 more)

### Community 3 - "worldgen: World generation doc (deciworldgen)"
Cohesion: 0.07
Nodes (57): Bug: city buildings missing a whole wall at sector borders (fixed v0.11.1), Wall scan reproduction on seed 1 (2 of 33 buildings, one real: b4_4_2), v0.10.0 to v0.12.3 city blocks, large schematics, city v2 and fixes status, v0.12.2: car wreck long axis is x at 0 degrees (north south 5/3, east west 4/2), Prop TileEntitySpecialRenderers (deci.I.*), Apartment slab layout (double loaded corridor, stair core, living part and bedroom per unit), Procedural building design doc (city blocks, Building v2), Car wreck model axis: long axis along x at 0 degrees (confirmed in game 2026-10-07) (+49 more)

### Community 4 - "SchematicPlan: SchematicPlan"
Cohesion: 0.07
Nodes (17): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.Mod, BlockRegistry (deci.aD.c / g), DecimationWorldGen, Schematic, a, Schematic (+9 more)

### Community 5 - "worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)"
Cohesion: 0.08
Nodes (32): EntityFallingSupplyDrop turns into a block only on a replaceable cell (flowers, saplings, tall flowers are not), Bug: graded yard sand fell into caves, hole next to a building (fixed v0.13.0), Bug: structures built on ocean floor, setBlock calls onBlockAdded, so BlockFalling (sand, gravel) falls even during generation, Bug: supply drop crate vanished when it landed in a flower (fixed v0.13.0), Autotest forces pauseOnLostFocus false, Damage checks must run after 60 server ticks (spawn invulnerability), Testing without the user (servertest, worldcheck, autotest) (+24 more)

### Community 6 - "Test Schematic Builder"
Cohesion: 0.15
Nodes (27): mil_compound large test schematic (48x14x48), city_office(), city_shop(), city_street(), civ_gas_station(), civ_house_ruin(), civ_shed(), decay() (+19 more)

### Community 7 - "new_feature: Feature tracker (new_feature.md)"
Cohesion: 0.17
Nodes (23): Bug: large ammo crate NPE (dead field avk), .bmodel is plain text Techne style code (earlier binary note was wrong), Gun specific .bmodel header fields (mOff, sPos, flamePos, lhPos, rhPos, ejectPos, Scale), Unmapped .f(n) builder call (likely spread or sway), Fire mode enum deci.ay.e.a (SINGLE, AUTO, BURST, PUMP, BOLT), Gun registration call new i(...).f().am(), Genuinely new model recipe (needs Techne), Reskin an existing weapon recipe (fast path) (+15 more)

### Community 8 - "StructureData: StructureData"
Cohesion: 0.15
Nodes (12): Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3), MetalShelfRenderer draws only the master part (returns unless isMaster: stored master position equals own position), setSelfMaster() on every placed multiblock part plus repair on chunk load, v0.12.3 rule: multiblock props (deci.W.a, metal shelves) render only from a master part; anything placing them outside player placement must call setSelfMaster(), Multiblock props (deci.W.*), MultiblockRepairHandler, Override, StructureData (+4 more)

### Community 9 - "ZoneStore: ZoneStore"
Cohesion: 0.14
Nodes (11): a, ServerTickEvent, ZoneStore, Zone tagging (mil_ MILITARY, city_ and buildings POLICE, deciworldgen_zones.json), net.decimation.mod.server.zones.ObjectZone, net.decimation.mod.server.zones.ObjectZoneList, ZoneStore (per world deciworldgen_zones.json), ObjectZone (+3 more)

### Community 10 - "?: cpw.mods.fml.common.eventhandler.SubscribeEvent"
Cohesion: 0.14
Nodes (9): ClientTickEvent, cpw.mods.fml.common.eventhandler.SubscribeEvent, HumanityKillHandler, ZoneSpawnHandler, DevPregen, ServerTickEvent, Load, net.minecraftforge.event.entity.EntityJoinWorldEvent (+1 more)

### Community 12 - "DeciBiome: DeciBiome"
Cohesion: 0.20
Nodes (4): cpw.mods.fml.relauncher.SideOnly, DeciBiome, Override, net.minecraft.world.gen.NoiseGeneratorSimplex

### Community 13 - "Name Mapping Applier"
Cohesion: 0.22
Nodes (14): desc_params(), ident(), is_obf_member(), load_classes(), main(), norm_desc_type(), norm_src_type(), params_match() (+6 more)

### Community 14 - "CLAUDE: tools/build.py real javac pipeline"
Cohesion: 0.19
Nodes (11): deobfuscation_data-1.7.10.lzma notch to SRG mapping, Compile only shim for Forge binpatch members (tools/shim_src), tools/build.py real javac pipeline, SpecialSource notch to SRG remapping, SRG member names (no reobfuscation step), classpath(), compile_sources(), inject() (+3 more)

### Community 15 - "BiomeMap: BiomeMap"
Cohesion: 0.26
Nodes (3): Sectors, BiomeMap, NoiseGeneratorSimplex

### Community 16 - "DeciGenLayer: DeciGenLayer"
Cohesion: 0.19
Nodes (7): DeciGenLayer, Override, TerrainEvents, GenLayer swap on WorldTypeEvent.InitBiomeGens (TERRAIN_GEN_BUS): two DeciGenLayers (1:4 and 1:1) reading one BiomeMap, InitBiomeGens, net.minecraft.world.gen.layer.GenLayer, Populate

### Community 17 - "SealedCaves: SealedCaves"
Cohesion: 0.23
Nodes (7): Caves, Override, Ravines, SealedCaves, net.minecraft.world.gen.MapGenCaves, net.minecraft.world.gen.MapGenRavine, net.minecraftforge.event.terraingen.InitMapGenEvent

### Community 18 - "ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)"
Cohesion: 0.27
Nodes (8): Bug: armor buff ignores NPC gunfire (fixed), Helmets give no gun protection (slot 3 excluded), Helmet counts on headshots only (v0.9.1: aim line for player guns, 20% random for NPC), Proposed LivingHurtEvent gunshot damage unification, ArmorGunfireHandler, net.minecraft.entity.Entity, net.minecraft.entity.player.EntityPlayer, net.minecraftforge.event.entity.living.LivingHurtEvent

### Community 19 - "bug: Bug tracker (bug.md)"
Cohesion: 0.24
Nodes (12): Bug tracker (bug.md), Bug: CustomSkinLoader coremod crash, Report: FPS drop in prop dense areas, Report: FPS drop while aiming scopes, Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep), Performance complaints need real profiling, Prism launched from the shell, read fml-client-latest.log, ClientRenderHandler.renderScopeView (deci.c.b) (+4 more)

### Community 20 - "documentation: Feature tracker (new_feature.md)"
Cohesion: 0.21
Nodes (12): DeciDamageSources (deci.aD.h), PacketGunHit handler (deci.aE.a$z$a), 35% armor damage reduction buff, Intro screen skip (deci.i.d.iH, deci.i.c.io flags), ItemArmorDeci.damageMultiplier, Loot fix documentation (documentation.md), 50% ranged weapon damage nerf, Buff armor damage reduction 35% (damageMultiplier x0.65) (+4 more)

### Community 21 - "architecture: Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected)"
Cohesion: 0.18
Nodes (11): DamageSource split: gunDeci (player) vs human/turret (NPC), Two gunshot DamageSource identities (gunDeci player, human NPC), NPC ranged attacks call attackEntityFrom directly server side, BankerTrader (deci.ai.e), FactionHumanEntity (deci.ah.d), Hostile mob entities (deci.ag.*: Bandit, Soldier, Infected), TraderSpawnManager (server.traders.a), ItemArmorJuggernaut (juggernautHelm/Vest/Pants/Boots) (+3 more)

### Community 22 - "bug: Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling)"
Cohesion: 0.25
Nodes (7): LineOfSight.canSeeTileEntity (deci.a.c$a.a): 8 rays from the eye to the render box corners, tools/patches/PatchSwing.java, TileEntityProp.getRenderBoundingBox: bare 1x1x1 cell for 36 of 72 props, never rotated, Bug: props randomly not rendered (legacy Decimation bug, fixed by PatchPropCulling), Javassist patches must go into three jars (patched, dist, dev base) plus Prism; sources in tools/patches (PatchSwing, PatchPropCulling), PatchPropCulling, PatchSwing

### Community 23 - "CLAUDE: deobf/ readable reference tree (decompiled with readable names, read only)"
Cohesion: 0.20
Nodes (11): Case sensitive volume extraction, CFR --caseinsensitivefs true silently drops colliding classes, Decimation.jar (obfuscated Forge 1.7.10 mod jar), deobf/ readable reference tree (decompiled with readable names, read only), Map file case trap (p_deci_aK.tsv equals p_deci_ak.tsv on APFS), Naming source priority (g*, p_*, r_*, m_*, then zz_auto.tsv), Vineflower fallback for classes CFR cannot structure, Map filenames unique ignoring case (+3 more)

### Community 24 - "SupplyDropScheduler: SupplyDropScheduler"
Cohesion: 0.29
Nodes (5): SupplyDropScheduler, EntityPlayer, net.minecraft.server.MinecraftServer, net.minecraft.world.WorldServer, ServerTickEvent

### Community 25 - "DecimationBiomes: DecimationBiomes"
Cohesion: 0.22
Nodes (6): DecimationBiomes, Biome names carry AmbientMusicPlayer keywords (forest, river, plains, hills, decimated, irrated), Fixed biome ids 110..118 (Decimated City, Suburbs, Irradiated Military Zone, Decimated Plains, Burnt Forest, Overgrown Plains / Forest / Hills, Murky River), Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit, Overgrowth style from the biome at the cell centre (temperate, lush, cold, dry), net.minecraft.world.biome.BiomeGenBase

### Community 26 - "new_feature: Current state and pending decisions"
Cohesion: 0.31
Nodes (10): Current state and pending decisions, v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type), deciworldgen second @Mod (net.decimation.worldgen), Dropped companion mods (Ruins, ezWastelands, GeneratorMods), Generator crash hardening (catch all, self disable after 3 errors), FML IWorldGenerator per chunk hook, tools/make_test_schematics.py (8 schematics), MarkerGenerator (milestone 2 glowstone pillars) (+2 more)

### Community 27 - "Heuristic Auto Namer"
Cohesion: 0.33
Nodes (8): camel(), classes(), known_fields(), main(), (binary name, source text) for every top-level file., Field names already chosen by the AI tables: (owner, obf) -> name., Field names declared directly in the outer class (indent 4)., top_level_fields()

### Community 28 - "CLAUDE: decimation-singleplayer README (public repo overview)"
Cohesion: 0.29
Nodes (8): dev/libs/Decimation-base.jar (patched jar minus our classes), dev/ RetroFuturaGradle workspace (GTNH ExampleMod1.7.10 template), MCP readable member names in dev workspace, Public repo rule: never commit Decimation jars, decompiled code or tools/lib (check git ls-files, nothing over 5 MB), DecimationVoiceChat.jar required or menu shows Update instead of Play, Building deciworldgen (gradlew setupDecompWorkspace, gradlew build, own Decimation-base.jar with patches), Repository holds only our own work (no Decimation code, assets or jar; bring your own 1.21.10f copy), decimation-singleplayer README (public repo overview)

### Community 29 - "terrain: Decimation world type doc (terrain, 0.14.0)"
Cohesion: 0.32
Nodes (8): Knowledge index (docs/, deobf notes, names.tsv, trackers), Decimation world type (level-type=decimation: flat rolling land, rivers and lakes, no ocean, one temperate climate), Terrain not done yet: fog colour comes from the world provider [not verified whether needed], no snow by design (temperature 0.7), fixed ids may clash, Spawn search: suburb, wasteland, overgrown plains and forest added to WorldChunkManager.allowedBiomes, Decimation world type doc (terrain, 0.14.0), Terrain stays vanilla ChunkProviderGenerate, only the biome map is replaced, Decimation world type feature (user chose A over reskinning vanilla / RTG, done v0.14.0, seen in game; open: fog colour, rubble and ash decoration), RTG Realistic Terrain Generation 1.7.10-1.1.1.7

### Community 30 - "VehicleHitHandler: VehicleHitHandler"
Cohesion: 0.33
Nodes (5): Bug: vehicles destroyed in one hit (fixed v0.8.1), VehicleHitHandler (v0.8.1), VehicleEntity (deci.ad.e) and parts, VehicleHitHandler, net.minecraftforge.event.entity.player.AttackEntityEvent

### Community 31 - "DECIMATION_MOD_TASK: Goal: loot crates and cars in singleplayer"
Cohesion: 0.29
Nodes (7): CFR decompiler, Personal use only, no redistribution, backup first, Prism Launcher instance mods folder, Goal: loot crates and cars in singleplayer, Singleplayer loot fix task brief, Technic modpack Decimation 1.7.10 (linusrhone), CFR decompiled source on /Volumes/DeciDeobf/src

### Community 33 - "DeadTree: DeadTree"
Cohesion: 0.33
Nodes (3): DeadTree, Override, net.minecraft.world.gen.feature.WorldGenAbstractTree

### Community 34 - "graph_update: graph_update.py"
Cohesion: 0.52
Nodes (6): finish(), prepare(), Keep an old community name when its members mostly carried over., relabel(), rj(), wj()

### Community 35 - "CLAUDE: CLAUDE.md project guide"
Cohesion: 0.47
Nodes (6): CLAUDE.md project guide, Decimation.jar.original.bak (hash checked backup), Decimation.jar.patched (deliverable), Graph update workflow (graph_update.py prepare/finish, one agent per chunk), In place jar entry update (zip -u, never rebuild), Knowledge maintenance rule (write findings to docs, bug.md, new_feature.md, maps, graph)

### Community 36 - "CLAUDE: Javassist bytecode patcher"
Cohesion: 0.33
Nodes (6): Hand written Forge/Minecraft stub classes, Javassist bytecode patcher, Javassist cannot compile Java 8 lambdas, Patch from ORIGINAL classes only after checking the target class is identical in the patched jar, rtk hook drops grep/find flags (use Python os.walk), Toolchain set up each session (nothing preinstalled)

### Community 37 - "DecimationWorldType: DecimationWorldType"
Cohesion: 0.40
Nodes (3): DecimationWorldType, Override, net.minecraft.world.WorldType

### Community 38 - "BottlecapHandler: BottlecapHandler.java"
Cohesion: 0.50
Nodes (3): BottlecapHandler (deciworldgen), BottlecapHandler, net.minecraftforge.event.entity.player.EntityItemPickupEvent

## Ambiguous Edges - Review These
- `addBox passes Y as Z origin (use addShape)` → `Genuinely new model recipe (needs Techne)`  [AMBIGUOUS]
  docs/gun_model_spec.md · relation: conceptually_related_to
- `worldcheck.py` → `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` → `Report: FPS drop while aiming scopes`  [AMBIGUOUS]
  bug.md · relation: conceptually_related_to
- `FML IWorldGenerator per chunk hook` → `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to
- `Recurring root cause: integrated server reports side CLIENT` → `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit`  [AMBIGUOUS]
  docs/terrain.md · relation: semantically_similar_to

## Knowledge Gaps
- **32 isolated node(s):** `Technic modpack Decimation 1.7.10 (linusrhone)`, `Prism Launcher instance mods folder`, `Subsystem taxonomy (core, proxy, network, loot, zone, ...)`, `ServerCommandRegistrar (deci.aK.o)`, `ChatHandler (deci.aK.n, radio chat)` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `addBox passes Y as Z origin (use addShape)` and `Genuinely new model recipe (needs Techne)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `worldcheck.py` and `Bug: generated metal shelves invisible (multiblock master never set, fixed v0.12.3)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Fix: SmoothSwingThread busy wait (PatchSwing 4 ms sleep)` and `Report: FPS drop while aiming scopes`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `FML IWorldGenerator per chunk hook` and `v0.14.0: Decimation world type (own biome map on vanilla terrain generator, flat cities on the exact city sectors, Sectors now shared, autotest runs on this type)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Recurring root cause: integrated server reports side CLIENT` and `Spawn trap: Decimation adds its spawns to biomes existing in its preInit and strips vanilla monsters in its init; our biomes are made in our preInit and copySpawns copies plains lists in postInit`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **Why does `Current state and pending decisions` connect `new_feature: Current state and pending decisions` to `architecture: Obfuscation map (package to meaning)`, `Rotation: Rotation`, `Building: Building`, `worldgen: World generation doc (deciworldgen)`, `worldcheck: Worldgen testing without a player (servertest pregen, wallscan, worldcheck)`, `Test Schematic Builder`, `StructureData: StructureData`, `ZoneStore: ZoneStore`, `?: cpw.mods.fml.common.eventhandler.SubscribeEvent`, `ArmorGunfireHandler: Bug: armor buff ignores NPC gunfire (fixed)`, `bug: Bug tracker (bug.md)`, `SupplyDropScheduler: SupplyDropScheduler`, `terrain: Decimation world type doc (terrain, 0.14.0)`, `VehicleHitHandler: VehicleHitHandler`?**
  _High betweenness centrality (0.149) - this node is a cross-community bridge._
- **Why does `Building` connect `Building: Building` to `StructureGenerator: StructureGenerator`, `worldgen: World generation doc (deciworldgen)`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._