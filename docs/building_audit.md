# Building quality audit (round 2 step 4a, 7 Oktober 2026)

User request: buildings need polish in every aspect, interior and exterior,
to production quality; design interiors in deliberate steps. This is the
baseline audit of v0.16.0 city buildings.

How it was made (repeatable):
- `python3 tools/floorplan.py WORLD LOG OUT --sample 2`: per storey floor
  plans with a letter per prop, plus walkable / reachable area from the
  stairs (flood fill).
- `./gradlew runClient -Pautotest -Paudit` (in `dev/`): 12 screenshots,
  `dev/run/client/screenshots/audit_<n>.png`: for a sample apartment,
  office and shop: facade from the street, ground storey, storey 1, roof.
  Noon, full gamma.
- Seed 1, Decimation world type.

## Findings

Interior:
1. Rubble = full blocks of mossy cobblestone standing in corridors and
   rooms. Reads as noise, not debris. Real debris is low and small.
2. One interior wall material everywhere (birch planks), oak plank floors
   and ceilings in apartments: a wooden box, whatever the palette outside.
3. No doors anywhere, only gaps. Decimation has 14 door blocks
   (Door_Office_1, Door_Blue/Green/Orange_1..3, Door_Emergency_*, ...).
4. Rooms have no function: an apartment unit holds 1 bed, 1 to 3 chairs,
   a cooking station. No sofa, TV, wardrobe, bathroom, kitchen counter.
   Ground storey apartment units are EMPTY (only stairs, rubble, a mailbox).
5. Offices: the same desk grid on every storey including the ground
   storey (should be a lobby / reception); wood and metal tables mixed at
   random; no monitors (BlockMonitor exists), no partitions (BlockWallOffice
   cubicle walls exist), no restrooms, no plants.
6. No ceiling details or lighting: Decimation has BlockCeiling_1..4,
   BlockCeilingVent(+Corner), BlockLight / BlockLightOff, BlockExitLight;
   no carpets or tiles: BlockFloorCarpet_1..6, BlockFloorTiles_1..3.
7. No story details: barricades, skeletons (BlockSkeletonGround/Wall),
   body bags, blood decals, notes (decal.notegeneric), graffiti 1..12,
   survivor camps (BlockLantern, BlockCanFire), looted crates
   (BlockWoodCrateOpen) all exist and are unused.
8. BUG (fixed v0.16.1, bug.md): some upper storeys were unreachable
   (ladder popped off, stair core inside the collapse). Upper shop storey
   still has no defined use (storage? office? flat?). Full rubble cubes in
   doorways still cut reachability on the floor plans (jumpable in game).
9. Tall buildings repeat the same plan on every storey.

Exterior:
10. Flat facades: no balconies, no entrance canopy or steps, no ground
    storey shopfront glazing; shops have no sign (BlockSign_Minedonalds,
    _Mineway, _Minebay, _Gunsrus, _Decimunition, _Metro exist).
11. Roofs: random leaves / blocks, a parapet; no roof equipment (vents,
    water tank, antenna, stair hut, AC units).

Good already: window rhythm and vines on towers, desks with chairs in
offices, shop shelving with goods, car parks, street furniture.

## Prop inventory

All 275 `deci:` blocks are listed by `World.registry()`; the gallery
(step 4b) shows what each looks like. Furniture relevant to interiors,
by name: chairs (BlockChair, BlockOfficeChair), tables (BlockWoodTable,
BlockWoodTable2, BlockMetalTable, BlockChesstable), TVs
(BlockFlatscreenTV + _News/_Emergency/_Target/_Youtube), BlockMonitor,
BlockStereo, BlockRadio1, BlockMilitaryRadio(+Small/Off), BlockCookingStation,
BlockWashingMachine, BlockWaterfountain, BlockVendingMachine_1/2,
BlockShopDisplay_1, BlockNewsStand1/2, BlockStretcher, BlockStorageCrate,
cardboard boxes 1..3, crates (wood, open wood, medical, police, ammo,
military), BlockWeaponCabinet, BlockPowerGenerator, BlockElectricBox1/2,
BlockElevator(+Button), BlockCCTV, BlockAlarmBell, office wall partitions
(BlockWallOffice_*), ceilings, carpets, tiles, lights, doors, decals.
No toilet, sink, bath, sofa, bed or fridge props: those need vanilla
blocks (cauldron sink, quartz stairs toilet, stairs + carpet sofa, bed,
iron block or quartz pillar fridge, trapdoor cupboards).
