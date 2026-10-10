# Military bases (US FOB style), design and code map

User request (10 Oktober 2026): "military zone with buildings ... 1 (our own generator), all three
sizes, US FOB style ... really really good output ... check small detail ... be detailed person and
architecture person ... work on loots especially props placement too". Replaces the test boxes
(`mil_outpost`, `mil_checkpoint`, `mil_barracks`, `mil_compound` from
tools/make_test_schematics.py) as the content of military sectors.

## 1. Reference (real US forward bases, Iraq / Afghanistan; researched 10 Oktober 2026)

Sources: Wikipedia "Forward operating base" and "B hut"; army.mil "All FOBs are not the same",
"The simple life: infantrymen maintain, improve austere Afghanistan FOB"; DVIDS / Marines on
COP Shukvani; Task & Purpose "how much sand to fill a HESCO tower"; HESCO product tables;
WBDG / Air Force entry control facility guidance; Smithsonian "HESCO barriers, new archaeology".

- **Sizes**: COP (combat outpost, 40 to 200 soldiers), FOB, large FOB / hub. Small ones are run
  by the troops themselves (own showers, latrines, generators).
- **Perimeter**: ring of concertina wire, then HESCO bastion walls (MIL7 cells 2.2 m high, 2.1 m
  deep; MIL3 1 x 1 m; height never more than twice the base), berms, T-walls (concrete blast
  walls, often painted by the troops), guard towers at corners and covering the gate, bunkers,
  sandbag fighting positions.
- **Guard tower**: HESCO filled base (about 30 MIL7 + 10 MIL1 in one example), wooden stair or
  ladder, plywood / sandbag shack on top.
- **Entry control point (ECP)**: approach lane with curves / serpentine barriers to slow
  vehicles, identification point (guard shack), search area (vehicles inspected away from the
  lanes), overwatch tower, final denial barrier, rejection lane / turnaround, standoff.
- **Inside**: TOC (tactical operations centre, interior, hardened with sandbags / HESCO /
  concrete), living area (LSA) of B-huts (plywood, 16 x 32 ft = about 5 x 10 m, up to 8 men,
  split into rooms or open) or tents (tan, Quonset like), DFAC (dining), MWR / gym, chapel, aid
  station / casualty collection point, latrines and showers, burn pit (open air), generators,
  motor pool, helipad (HLZ), fuel point, ammunition supply point (bermed), conex containers,
  duck and cover bunkers between the huts.

## 2. In Minecraft (1 block = 1 m, Decimation props, docs/prop_catalogue.md)

| Real thing | Built from |
|---|---|
| HESCO MIL7 wall | deciworldgen:hesco (our own plain block, tools/props/hesco_textures.py: tan fabric in a grey welded mesh), 2 thick x 3 high, sandbag cap in places. NOT deci:BlockMilitaryBarrier: that one is a prop, a tile entity per cell, thousands per base (user: "really hurt fps") |
| sandbags | deci:BlockSandbagStack (green grey) / BlockSandbagStackBeige: plain cubes, but BlockFalling (keep them supported) |
| concertina wire | deci:BlockConcertinaWire, ring outside the wall, double at the gate |
| T-wall | deci:BlockBarrierTall (tall concrete slab, multiblock) |
| jersey barrier | deci:BlockBarrier (long along x at meta 3) |
| hedgehog, sawhorse | deci:BlockHedgehog, deci:BlockHazardbarrier |
| plywood (B-hut) | birch planks walls, spruce frame / stairs / slabs, trapdoor shutters |
| tent (TEMPER) | smooth sandstone vault (hardened clay read orange), birch plank floor |
| footlocker | deci:BlockStorageCrate or (half) a vanilla chest: the chest has Decimation's loot, the storage crate is a player box (empty) |
| shelves, tool locker | deci:BlockMetalShelf / _Empty (multiblock, drawn 2 wide, 2 tall), deci:BlockElectricBoxBin |
| locked door | deci:Door_Emergency_1_Locked (the only locked door): opened by a military keycard on deci:BlockKeycardScreenMilitary within 3 blocks, a lockpick, or creative; military crates, wrecks and care packages can drop the military keycard |
| concrete | stone / deci:BlockStone_* |
| loot | deci:BlockMilitaryCrate, BlockAmmoCrate(Large), BlockWeaponCabinet, BlockMedicalCrate, BlockWoodCrate, BlockStorageCrate (footlocker), BlockCarePackage |
| equipment | BlockMilitaryRadio(Small), BlockRadioTower, BlockSpotlight, BlockPowerGenerator, BlockHazardLight, BlockFlagPollUAHD, BlockWallflag, BlockMonitor, BlockMetalTable, BlockOfficeChair, BlockKeycardScreen_Military |
| vehicles | BlockWreckageMilitary1 (jeep), 2 (APC, 3x3x7), 3 (helicopter, huge) |
| supplies | BlockWaterPallet(Tarp), BlockBarrel, BlockTire(Stack), BlockCardboardBoxes1, BlockTrashBag1/2 |
| bunks | vanilla bed + spruce slab (top half) above with a carpet mattress |

## 3. Layout (gate on the south side in the plan, the whole base turned 0 / 90 / 180 / 270)

Sizes (footprint incl. standoff and gate lane): COP 50 x 58, FOB 78 x 84, large FOB 112 x 118.
From outside in:
- standoff ring (6), concertina at 2 (and 3 on the large base), a broken coil row 2 outside the
  HESCO;
- ECP on the south: gravel lane 5 wide between T-wall lines, serpentine jersey barriers every 3
  blocks from alternate sides, hedgehogs outside the T-walls, sawhorses at the mouth, drop arm
  barriers in the gate, sawhorses inside; guard shack (sandbag booth, plank roof, radio, chair);
  search area (FOB / large: gravel pad behind T-walls, light tower, drums);
- HESCO wall 2 thick, 3 high, concertina on top of the outer row in stretches, a sandbag cap in
  places, sandbag fighting positions (U shapes with ammo) on the inner face, a firing step on the
  gate side;
- towers 4 x 4 at the corners, beside the gate (1 on the COP, 2 on the others) and mid wall on the
  large base: HESCO base, plank deck at 4 with a ladder hatch, sandbag parapet with firing gaps,
  corner posts, slab roof at 8, spotlight and ammo on the deck;
- gravel ring road 3 wide inside the wall, main road 5 wide from the gate to the TOC front.
Inside (fixed first, then a greedy placer: each module scans its zone until its rectangle and a
1 block margin are free; roads may border modules, modules never touch each other):
- TOC (north, centre): concrete building, flat sandbag layer on the roof with a low sandbag
  parapet, LOCKED door with the military keycard screen beside it, map table, desk runs on both
  walls (a metal table is drawn 2 long: runs start 2 in from the wall and step 2) with monitors
  and big radios lying along the desk (dials to the room), commander's desk under the wall flag,
  weapon cabinet and ammo case by the door, ceiling lights; flag pole, antenna mast, generators,
  light tower; FOB / large: HESCO ring 2 high, 3 out (COP: sandbag wall instead).
- Living area (west): B-huts 5 x 10 in rows facing each other across company streets (9 wide)
  with duck and cover bunkers between the door columns, sandbag blast walls between huts. The hut:
  raised floor, a step at the door, a low gable roof (planks, a 3 wide slab ridge, slab eaves),
  windows on the long sides with some shutters (open trapdoors against the wall), a window and a
  power panel on the back end. Kinds: barracks (bunks with upper bunk and mattress,
  footlockers), arms room (a wall of 6 weapon cabinets, a wall of crates with a large ammo case,
  a cleaning bench at the far end, the row by the door free), office (desk run, monitors, radios,
  cabinet between the chairs, flag), chapel (benches, altar table, flag); latrine shed with a
  step, porta-john rows, shower conex.
- Logistics (east): ASP 12 x 12 (sandbag berm 2 thick: raw dirt read orange and grew grass;
  three rows of crates, a small ammo case on each large one, a care package, warning lights),
  fuel point (bladder in a sandbag berm, drums, pump generator), motor pool (gravel, oil stains,
  vehicles long along z, camo net on posts, T-wall screen, conexes with loot, maintenance
  shelter), dining tent, helipad (concrete, white H, landing lights, windsock; a 13 x 13 pad with
  a crashed helicopter on the large base), clamshell hangar and conex yards (large).
- Anywhere it fits: aid station tent (stretchers along both sides 3 apart, medical crates at the
  north end, the middle aisle free, body bags outside), dining tent (one long table down the
  middle, chairs both sides, a serving line with stoves on a metal table at the south end), gym
  (weights, chess tables; large), mortar pit, burn pit (large), generator farm, parked Humvees,
  bunkers; light towers along the main road.
- Guard towers: the searchlight (drawn 2.5 x 3.2) stands on the roof, the deck holds a crate and
  a radio. Guard booth (4 x 4 sandbags) north of the gate tower, window to the lane.
- Decay (overrun base): a wall breach (burst cells, spilled fill, wire gone), slumped cells, trash
  bags, open crates.

Loot (Decimation's crate blocks, its own loot tables): military crates and ammo crates in the ASP,
arms room, towers, fighting positions, conexes; weapon cabinets in the arms room and TOC; medical
crates in the aid station; footlockers in the barracks; wood crates and cardboard in conexes and
the DFAC; care packages at the ASP / helipad.

## 4. Code map

- `worldgen/military/Canvas.java`: the base built in memory (block, metadata, how to turn it).
- `worldgen/military/MilitaryBase.java`: the layout and every module (wall, tower, gate, B-hut,
  TOC, tents, motor pool, helipad, ...), deterministic from the site seed.
- `worldgen/military/MilitaryBasePlan.java`: the Plan (Graded: ramps the ground around it),
  rotation, cache.
- `worldgen/military/PropBoxes.java`: every prop's drawn box per metadata and its quarter turn
  metadata (assets/deciworldgen/prop_boxes.tsv from tools/props/propgeom.py).
- `Canvas.validateProps(log)`: removes and logs props drawn into blocks, doors or each other.
- `worldgen/military/BlockHesco.java`: the HESCO block (textures from tools/props/hesco_textures.py).
- `LargeSites`: military sector sites build a base instead of a `mil_` schematic.

## 5. Status

v0.42.2 (11 Oktober 2026), second review (docs/shots/milbase_v0.42.1_review/): TOC door wall with
two weapon cabinets facing the room, police and military crates and the large ammo case; half of
the barracks footlockers are vanilla chests (Decimation's chest loot; its storage crate is
empty); the DFAC counter has the small field radio instead of stoves; the motor pool has a parts
yard along its south edge (shelves, tyre stacks, drums, a covered pallet, open crates) and a
workshop (bench with a toolbox, tool locker, parts shelf). 0 clashes, checker 0 findings.

v0.42.1 (11 Oktober 2026), after the user's review (28 shots, docs/shots/milbase_v0.42_review/):
HESCO as our own plain block (tile entity props per base 1020 / 2007 / 2902 -> 129 / 381 / 571),
locked TOC door, ground mostly gravel, sandbag ASP, every prop placed from its drawn box
(docs/prop_placement.md) and checked by `Canvas.validateProps` (0 clashes on all three sizes)
and tools/props/propclash.py (0 findings).

v0.42.0 (10 Oktober 2026): all three sizes built and photographed (dev test `milbase`,
docs/shots/milbase_v0.42/, docs/shots_index.md), checked in real worldgen (seed 1, two COPs at
-216,63,435 rot 90 and -90,66,404 rot 180, no errors, terrain ramped). The old mil_ test boxes
are no longer placed. Waiting for the user's review.

Traps met (the skill decimation-military-base has them as lessons):
- Decimation's texture pack: dirt and especially coarse dirt are orange; ground is mostly
  gravel with dirt patches. Exposed dirt turns to grass over time (berm tops).
- Modules must not overlap: the occupancy grid (road class vs module class) plus the greedy
  placer; logging refused modules (test output "refused") found every layout bug at once.
- The dev test builds in the sky (y 200) without the ground ramp (grading from y 200 down to the
  real terrain grew 100 block pillars), clouds off, render distance 16, a 30 tick capture
  window (slow frames skipped the 5 tick one).
- Props are drawn bigger than their cell (computed, docs/prop_placement.md): APC 4.5 x 8.9,
  helicopter 9.4 x 14, jersey barriers and tables 2 long, care package 2.25 x 1.9, spotlight
  2.5 x 3.2. (The v0.42.0 notes said APC 5 x 8, helicopter 8 x 9, tables 3 long: eyeballed.)

## 6. How to test

- `python3 tools/devtest.py --live milbase [sizes=0,1,2] [turns=0..3] [seed=N] [points=a,b]`:
  builds the sizes side by side in the sky at -20000,230,-20000 (since 11 Oktober 2026; the old
  sites at 3000,110,3000 and -4000,200,-4000 cut into cities in the user's dev world and were
  cleared with `python3 tools/perfcheck.py clearblocks WORLD X0 Z0 X1 Z1`, game closed, region
  backup in dev/run/client/world_backups/), one shot per camera point
  (dev/run/client/screenshots/milbase_<size>_<point>), refused modules in the results.
- `points=none` only builds and reports what did not fit and the prop clashes (fast).
- Every run writes dev/run/client/devtest/milbase_<size>.tsv (all blocks); `python3
  tools/props/propclash.py dev/run/client/devtest/milbase_1.tsv` must report findings 0.
- `turns=1` / `turns=3` too: rotated bases use the per prop quarter turn table.
- Test trap: rebuilding a base IN PLACE over its previous copy makes the multiblock props
  (T-walls, street barriers, shelves) vanish on every second run (writing over the old parts
  breaks the new masters); real worldgen writes fresh chunks. Prop counts in the dumps then
  alternate (COP 128 / 68); judge multiblocks on a first build.
- Real worldgen: `python3 tools/servertest.py 1 type=decimation pregen=-64,448,9` places the
  two seed 1 COPs; the log lines "military base (...) ... at x,y,z".
