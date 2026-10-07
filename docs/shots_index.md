# Screenshot index (what every saved picture shows)

The pictures live in `docs/shots/` (git ignored: they show Decimation's
models and textures, the public repo holds only our own work). They stay
on this machine. This file is the committed memory of what they show, so a
later session reads THIS instead of opening or re-taking images. Open an
image only to check a detail not written here; re-shoot only after the
code that produced it changed.

How shots are made: `./gradlew runClient -Pautotest` (street views),
`-Paudit` (building audit), `-Pgallery` (prop gallery); output in
`dev/run/client/screenshots/`, then copied here into a versioned folder.

## worldmap_seed1_v0.14.png
Top down map (`tools/worldmap.py`), seed 1, Decimation world type, 800x800
around spawn. Grey squares = Decimated City sectors full of building
footprints; brown = military; yellow tan = dead plains / suburbs; green =
overgrown forest and plains (north west, east); blue winding rivers that
stop at city / military edges; small blue lakes in dry areas.

## street_v0.15/ (autotest street views, 0.15 / 0.16)
- autotest_0: from the east sidewalk of a north-south street looking north:
  flat dark asphalt, dashed white centre line running ALONG the street,
  light grey sidewalks, street lights on both sides with arms over the
  road, tall ruined apartment blocks (pink / brick / white palettes) with
  vines, a grass lot with a dead tree on the right.
- autotest_1: side-on view of a street light (meta 2): the arm bends to
  the RIGHT = east = over the road. Confirms the facing table in
  docs/building_design.md "Street life". A street bin on the inner
  sidewalk, a row of parked wrecks in the lane.
- autotest_2: from the west sidewalk looking east across the street:
  cross-section flat after levelling; the lot behind ramps up in steps of
  grass terraces to a white office tower.

## audit_v0.16/ (building audit, round 2 step 4a, see docs/building_audit.md)
- audit_apartment.png (b0_1_0, 3 floors, 17x14): facade = small grey stone
  brick box, gravel path, vines, no balconies or canopy. Ground storey: a
  1 wide corridor of birch plank walls, oak plank floor and ceiling, a full
  dark stone block in the way; claustrophobic, empty. Storey 1: same birch
  box, a full mossy cobblestone block in the doorway. Roof view: flat roof
  with scattered blocks.
- audit_office.png (b0_1_1, 11 floors, 24x26): facade = tall pink concrete
  grid, window rhythm good, vines. Ground AND storey 1 = identical open
  plan: rows of grey metal desks with black office chairs, a few wooden
  X-leg tables mixed in, grey stone floor, dark grey ceiling grid, full
  mossy cobble blocks standing between desks. Roof: parapet, snow / leaf
  dots, nothing else.
- audit_shop.png (b0_1_2, 2 floors, 12x15): facade = brick front with a
  small sign-like panel, car park with 3 wrecks in front, slab walkway.
  Ground storey: grey metal shelving with wooden goods on it, light grey
  floor, dark plank ceiling (good, reads as a store). Storey 1: an empty
  oak room with orange clay / mossy walls and a full mossy cobble block in
  the middle. Roof: sandstone top with a few blocks.

## plans_v0.16/ (tools/floorplan.py, seed 1)
Per storey plans, 14 px per block: dark grey = wall, light blue = glass,
white = air, letters = props (T table, c chair, S shelf, B bed, k cooking,
X crate, x boxes, V vending, t trashcan, m mailbox, r rubble, / stairs,
L ladder). Key observations are in docs/building_audit.md (empty ground
storey units in apartments, identical office storeys, b-4_1_0 shop upper
storey unreachable = the ladder bug fixed in 0.16.x).

## gallery_v0.16/ (prop gallery, round 2 step 4b)
83 views, 3 blocks each, `views.txt` maps view number -> block names
(left to right). Every block is described in docs/prop_catalogue.md (look,
size, facing at meta 3); read that, not the images. All views checked:
the first attempt (5 per view, far camera) was deleted; views 0, 23 and
70..76 were re-shot (platform not rendered yet, camera turned by a touched
mouse, doors placed without their upper half). Camera is now locked per
tick and `-Ponly=` re-shoots single views.

## audit_v0.17/ (after step 4d.1: doors + low debris)
Same 3 buildings and camera spots as audit_v0.16.
- apartment: dark wood office doors with a small window stand in the unit
  doorways along the corridor (both halves, flush in the wall); corridor
  debris is now a brick / stone slab instead of a mossy cube. Walls,
  floors and ceilings are still all birch / oak planks (step 4d.2).
- office: desks unchanged; floor debris now brick and stone slabs;
  cobwebs were standing between desks (fixed right after: cobwebs only next
  to walls). Ceiling still dark grey, no lights (step 4d.2).
- shop: as v0.16 (shelves with goods), stockroom metal door.
- audit_*_in.png (after step 4d.2 surfaces, ground storey + storey 1):
  apartment corridor = white / pink plaster wall panels with a skirting
  course, black tile floor (looked like a dark tunnel because the floor
  above shows as ceiling -> corridor floors switched to light stone),
  office doors. Office = grey carpet floor, the carpet of the storey above
  as a grey ceiling, light panels / vents hanging as dark fixtures in a
  grid, pink plaster walls. Shop = shelving aisles under hanging lights,
  stone stockroom floor, white plaster walls upstairs.

## audit_v0.18/ and plans_v0.18/ (after step 4d.3 apartment rooms)
- audit_rooms.png: audit_1/2 = inside an apartment living room: kitchen
  run along the side wall (tall grey fridge, furnace oven, washing
  machine, cauldron sink) on black / white checker tiles beside an oak
  plank living floor, plaster walls, office door; the flat above showed
  its checker kitchen floor as a checker CEILING (fixed: upper kitchens use
  light stone). audit_5/6 = office open plan, chairs at desks, carpet,
  hanging fixtures. audit_9 = shop aisles.
- audit_apartment.png: facade unchanged; corridor with light stone floor
  and plaster walls.
- plans_v0.18 b-4_0_1 (24x25): every flat has F fridge, u sink, M washer,
  T + c dining, V TV + h sofa + rug, B bed + C chest, o toilet + u sink in
  bathrooms, D doors. b-4_0_2 (18x16): small flats (bed, fridge, sofa),
  ground storey = lobby (plants p, bicycles y) + a furnished flat.

## audit_v0.19/ (storey height 5, own ceilings)
- audit_s5.png: first try, ceilings of BlockCeiling_3/4 tiles: rendered as
  a dark grey lid (undersides are shaded); facades 25% taller, office
  tower window rhythm now 2 solid rows between window bands.
- audit_s5b.png: final: white plaster ceilings (shaded light grey) over
  the kitchen and living room, plaster lintels above doors, offices with
  plain ceilings and hanging fixtures. Remaining: outer walls still show
  stone brick facade on the inside (step 1b lining).

## sets_v0.20/ (furniture set preview, -Psets)
Each set alone in a plaster bay on a spruce floor, seen front-on;
set_<n>.png, sets_sheet_*.png (8 per sheet, names on top).
- dresser: double chest with a plant pot. dining_table: X leg wooden table,
  a chair each side facing it. bed_along_wall: chest + red bed lying along
  the wall, pillow at the chest. lobby_bin_plant: bin + leafy plant.
  lobby_bikes: 2 bicycles + plants (first version overlapped; now a gap).
  kitchen_run_4: tall grey fridge, cauldron sink, quartz counter, furnace,
  oak wall cabinets above. bath_tub: 2 cauldrons. wardrobe: first 3 high
  spruce block (read as a wall chunk) -> now 2 high dark oak cabinet.
  kitchen_run_6: fridge, counter, sink, counter, oven, washing machine,
  dark oak cabinets above. desk_chair: chair facing an oak block desk with
  a pot. closet_boxes: empty metal shelving + cardboard boxes.
  bed_nightstands: bed head to the wall between 2 chests, pot and radio on
  top. living_couch_wall: spruce stair couch facing the room, light grey
  rug. bath_toilet_sink: white quartz stair toilet + cauldron basin.
  living_tv_small: TV on a spruce cabinet, oak stair sofa facing it.
  living_tv_sofa: TV + stereo + pot on a dark oak cabinet, red rug (brown
  was invisible on the floor), dark oak slab coffee table, spruce sofa.
  dining_small: table + chair. kitchenette_3: fridge, sink, oven, cabinets.
  bed_single: bed + chest nightstand. living_tv_armchair: TV on cabinet,
  oak stair armchair. kitchen_run_5: fridge, counter, sink, counter, oven.
- sets_fix.png: the 4 sets after the fixes above.
- audit_v0.19/audit_sets_v0.20.png: in buildings: a kitchen run with wall
  cabinets and a dining table in a flat; offices unchanged.

