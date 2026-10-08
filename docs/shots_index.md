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

## flats_v0.20/ and flats_v0.21/ (-Pflats: 3 apartment blocks, living / bedroom / ground)
- v0.20: b0_1_0 (17x14): long oak floored hall (leftover space next to
  the stairs, labelled living by the fallback), door lintels over empty
  doorways, a debris slab mid floor; bedroom: bed along the window, chest,
  grey carpet floor; ground: stone lobby with bikes. b0_1_3 (12x22, 8
  storeys): every storey an empty hall with windows: NO flats (the stair
  core took the width). b0_3_1: living with a colour-bar TV close to the
  camera, bedroom with dark wardrobe, chest, bed.
- v0.21: same spots before the narrow-block fix; b0_1_3 still empty halls
  (fixed after these shots: ladder for blocks under 16 wide).
## sets_v0.21/
- sets_kitchen.png: kitchen runs with a 2 high silver iron fridge, one
  counter material, dark cauldron sink with a tripwire hook tap above,
  furnace oven, wall cabinets with trapdoor door fronts; kitchen_run_6
  ends with the washing machine. lobby_bench: 2 street benches + plant.


## study_usa_coast/ (-Pstudy on the USA coast map, 8 Okt, roof filter + night vision)
- Rerun after the camera fix; the first run had an Options menu in view
  1, black rooms and roof spots. views.txt has every spot. Night vision
  swirls (blue) sit in some frames; replaced by gamma 8 full bright
  afterwards (tested clean on 4 views).
- 4 (b4 s0): waiting room, rows of wooden chairs facing a long white
  counter with a purple stripe, ceiling light panel grid, white tiles.
- 5 (b4 s1): corridor, white office panels with a dark dado stripe,
  doors. 6 (b4 s2): empty hall, plank wall decor, blood decal.
- 7, 8 (b5): military hall, dark walls, oak fence queue rows, ladder,
  blue grey carpet, stone brick room boxes.
- 9 (b6 s0): office cubicles of 2 high cracked stone partitions, desk +
  computer, ceiling light strip + vent.
- 10 (b8): weapon room with metal shelves, a glass case, posters, a
  dead body prop. 12 (b9): brick corridor, red carpet, cracked glass.
- 14, 15, 16 (b12): store / office with metal shelves, office chairs,
  white walls, stone brick pillars, light panels, dirt and leaf patches
  on the floor (decay), desk + computer, camo vehicle and crates.
- 17 (b13): long hall, tables in a row, rose planters along the wall.
  18: bookshelves, red couch, cracked window. 21 (b14): water channel.
- 26 (b29): office with white chairs, flooded floor, exit sign, office
  chairs. 28: helicopter on the roof.

## study_decicraft/ (-Pstudy on Decicraft spawn town, 57 views)
- Mostly a converted vanilla city: 0..1, 7..10 street views from under
  overhangs (lane lines, sidewalks, hedges, towers, red cranes).
- 2..5 (b3): mall like hall, white pillars, wooden floor, cots (camp).
- 12..16 (b24): market street with awnings and cobwebs; supermarket with
  double metal shelf aisles, planks floor.
- 17..25 (b25): dark tower corridors, plank floor, nothing inside.
- 26..30 (b26): office floors with desk islands of computers, white
  floors, glass atrium with balconies.
- 34..47 (b30, b34): hotel / cell corridors, rows of security doors,
  redstone lamps in the ceiling, empty.
- 54..56 (b62, b63): empty office hall, stone corridor with lamps.

## audit_v0.23/ (-Paudit, 0.23.0: offices and shops 6 high)
- 0..4 apartment b0_1_0 (17x14, storey 5): unchanged look: stone brick
  facade with vines, corridor with bike and benches, plaster walls,
  kitchen with tiles, roof with leaves.
- 5 office b0_1_1 (24x26, 11 floors, storey 6) facade: camera ends up
  close above the window heads, mostly window recesses and vines.
- 6, 7 office storey 1 and 2: open plan desk rows with office chairs,
  3 high window bands, ceiling vents and light panels under the ceiling,
  stone brick stair core; reads roomier than the 5 high offices.
- 8 office ground: desk rows, stair core, broken glass bits in view.
- 9 office roof: stone with leaves, parapet.
- 10 shop b0_1_2 (12x15, 2 floors, storey 6) facade: orange clay, tall
  front, cars parked in front. 11: stockroom with shelves and boxes.
- 12, 13 shop storey 1: camera faces a plaster wall with a door (stock
  partition) from up close: poor camera spot, not a building defect.
- 14 shop roof with leaves.

## dc_proof_v0.23/ (DeceasedCraft buildings converted to 1.7.10, pasted at y 150)
- tools/lc2schem.py + lctranslate.py, `/deciworldgen paste`; buildings
  0 apartmentsmalla, 1 residentiala, 2 policeoffice1, 3 fastfood,
  4 office1a, pasted at x -140, -114, -88, -62, -36 (z 20). Cameras were
  naive (fixed spot per storey): many face a wall or the sky.
- 0 apartment ground: white walls, light tile floor, doorways, glass
  doors, wooden furniture: clean, detailed. 1..3: closet corner + door.
  4: facade, white quartz bands, dark stone, window strips.
- 5 house: brick, dark oak, chairs + table, birch floor.
- 10 police ground: glass front, stone counters, metal doors, tiles.
- 15 fast food: tables with chairs, glass + brick front, counter, door.
- 20, 21 office: stone counters, desk with monitor and chair, glass walls.
- 24 office tower exterior: glass curtain wall, stone core, floor bands.
- 11, 13, 22: the camera ended on the ground street (teleport issue),
  not the pasted buildings; 12, 23: inside a wall.

## wasteland_legacy_v0.23/ (all 14 legacy deadzone buildings converted, pasted at y 150, x -420..84, z 40)
- Order (building index): 0 warehouse1, 1 warehouse2, 2 factory1,
  3 factory2, 4 carfactory1, 5 lab1, 6 bunker1, 7 multi_warehouse,
  8 multi_factory, 9 multi_pumpjack, 10 multi_derrick, 11
  multi_militarybase, 12 multi_militarycamp1, 13 multi_laboratory1.
  "storey 99" = aerial view (pitch 40).
- Every building stands on its own pale ground plate (dried salt ->
  sandstone: reads too yellow, a grey / white stone would match salt
  flats better).
- 0, 1 warehouses: pitched grey roofs, plank walls, sandbags around.
  2, 3 factories: dark metal roofs with vents, machinery, chimneys, red
  stripe walls. 4 car factory: grey block with red band, wrecks outside.
  5, 6 lab and bunker: small open compounds with crates, vehicles, sandbags.
- 7, 8 multi warehouse / factory: long sheds with skylights, chimney and
  crane tower on the factory.
- 9, 10 pumpjack / derrick: oil rigs with derrick towers, tanks, pipes.
- 11 military base: walled compound, barbed wire fences, sandbag walls,
  watchtower, military truck in the court (19), parapets (18).
- 12 military camp: open camp with tents / crates on a plate.
- 13 laboratory complex: big walled block with the glass dome ring,
  corridors (22), a room with rows of chairs (23).
- Interior cameras (11) from tools/mapbuildings.py: some face walls.

## wasteland_cellars_v0.23/ (cellars of the legacy wasteland buildings, ground floors at y 200)
- Converter now generates floors like Lost Cities (cellars included);
  camera in creative (no suffocation inside blocks).
- 0 lab1 cellar: sandstone lined room, computer desks, bookshelf, crates.
  1 bunker1 cellar: brick room, computers, gun rack, desks, chairs.
- 2..5 military base, 4 levels (y 176 / 182 / 188 / 194): brick and
  stone brick corridors, rows of seats, cell bars, ladders between
  levels, crates, desks with chairs.
- 6..16 laboratory complex (15 cellars, y 110..200): bottom (-15) a big
  hall with sandstone blocks and machinery, glass partitions; -12, -9,
  -6, -3 show the same office / lab layout (one part used for a floor
  range in the data): rows of chairs and desks, glass walls. Several
  manual spots (x 28, z 60) face a wall.

## lc_alignment_v0.23/ (alignment test scene, tools/lcscene.py, pasted at -120 70 30)
- Low street (level A) road surface y 70, high street (level B) y 76,
  buildings' ground floors at y 70, military base cellars below.
- 0 along street A: road level with the building fronts on the left
  (sheds with dead bushes on their salt plates), our generated city on
  the right (the scene was pasted inside it).
- 1 lab / factory fronts: plates and doorways flush with the road.
- 2 military base: outer wall with razor wire right at the street edge.
- 3 the deadzone stairs part from street A: a wide stone staircase with
  brick sides and railings climbing to street B: lines up.
- 4 from street B looking back east; 5 overview from above (mostly our
  city's tower in view, poor spot).
- 6 base courtyard passage at street level. 7, 8 camera inside cellar
  walls (see-through), bunker levels with cell bars visible.

## lc_city_v0.24/ (first Lost Cities style city, seed 1, 20 views, all valid)
- 0..11 street views (pairs: along a north-south street, then facing the
  block fronts): asphalt chunk wide streets flush with the buildings;
  legacy deadzone sheds / factories / military base wall with razor wire
  (0..3), white modern blocks (4, 5), timber framed / glass fronted
  current beta buildings (6, 7), dark offices (8, 9), brick and white
  facades (10, 11). City edge: the land beyond rises as a grass topped
  dirt cliff next to the street (0, 2, 10).
- 12..19 stairs between street levels (64 -> 70): wide stone stairs with
  brick sides and railings (12, 13), sandstone terraced steps with stone
  side blocks (14, 16, 19), a brick terrace with stone steps and a blue
  door wall (15, 17), an underpass like part (18).

## lc_city_v0.24.1/ (street dressing + city edge ramp, seed 1, 19 views, all valid)
- 0..11 streets: grey stone sidewalks 3 wide on both sides flush with the
  asphalt, dashed white centre line, street lights on the road edge with
  arms over the road (staggered sides), benches facing the road, bins,
  scattered trash bags; a wreck now and then (an earlier run of this
  version had a hash bug: wrecks and bags everywhere, no lamps; fixed).
- 12..15 stairs between levels as before.
- 16..18 city edge (north side, z 6): the land outside is ramped down to
  the street over 10 blocks; it reads as grass terraces 1 block high up
  to natural ground about 12 higher. Works, could be smoother / wider.

## lc_interiors_v0.24.1/ and lc_interiors_v0.24.2/ (interiors of converted city buildings, 24 views each, same spots)
- v0.24.1: offices with desk rows, monitors and chairs, restaurant tables,
  kitchens; BUT rows of sandstone cubes on floors (7, 13, 15: basalt
  translated as salt flat) and many open window holes (glass panes were
  skipped: the frying pan rule matched "pane").
- v0.24.2 (translator fixed): glass walls and windows back (4, 6, 8, 14,
  23), stone floors instead of sandstone cubes, stone bricks grey (were red
  brick). Several camera spots face a wall or stand in a corridor (3, 5,
  10, 16, 17).

## lc_superblock_v0.24.3/ (superblock with the courtyard office tower, 0.24.2 code)
- 0 from the street next to a legacy military base wall: the 192 high
  courtyard office tower (4 x 4 chunks), lamps, other towers behind.
- 1 from above (y 140): a real skyline: white and glass towers, the
  chunk street grid with sidewalks and stairs, lots; reads like the
  DeceasedCraft screenshots the user sent.
- 2 street level between the tower's columns and a pink glass fronted
  block; 3 a rooftop terrace of a neighbouring tower.

## lc_edge_v0.24.4/ and lc_edge_v0.24.4b/ (city edge ramp, seed 1, 7 views each, all valid)
Same spots both: 0..2 from the north sidewalk (z 6) looking north at
x -150, -60, 20; 3 aerial over the city's north west corner; 4 and 5 from
the ramp looking south at the city; 6 aerial over the north band.
- v0.24.4: 0, 1 grass terraces 1 block high in long straight rows parallel
  to the street, up to the natural ground; 2 a natural hill with trees east;
  3 the corner ramp rounded, no cliff, dirt shows on cut faces; 4, 5 the
  city seen from the ramp, land meets the sidewalk; 6 smooth contours.
- v0.24.4b (contour wobble): 0 the first terrace now steps in and out;
  rows wander instead of running straight; rest as v0.24.4.

## lc_highway_v0.25.0/ and lc_highway_v0.25.0b/ (highways, seed 1, 8 views, all valid; b = with side ramps, same spots)
- 0 from the city street (8,67,24) north: the street runs on straight into
  the highway, hedge median in the distance.
- 1, 2 on the deck at z -150 north / south: dark asphalt (BlockRoad), leaf
  hedge median and side hedges, stone wall lamp posts with coal block heads
  and slab arms; 2 an adboard tower with ladder in the median, low grass
  slopes beside the road.
- 3 aerial side view of the 2 bridge chunks (z -112..-97) over a ravine:
  rails, a crash variant with white blocks.
- 4 under the bridge looking north west: stone brick pillar, natural stone
  slope (bridge chunks get no side ramp).
- 5 deck at z -300 north: bridge over a lake with iron bar rails, a wreck
  in the lane.
- 6 the east west highway east of the city (250,68,8) looking east.
- 7 v0.25.0: aerial, straight highway across the land; v0.25.0b: aerial
  from the east (36,80,-170): road with lamp arms, graded grass beside it.

## lc_tunnel_v0.25.0/ (highway tunnels, seed 1, 4 views, all valid)
- 0 x highway at 400,67,8 looking east: road in a cut between graded grass
  slopes, a lit tunnel portal ahead.
- 1 aerial over that tunnel: the road disappears under the hill.
- 2 z highway at z -440 looking north: adboard tower, a short tunnel just
  before the northern city, towers behind.
- 3 aerial near the northern city: the highway reaching the city edge.

## lc_decor_v0.26.0/ (parks, street scenes, fronts; seed 1, 12 views, 10 valid)
- 0 (-200,66,8) west: a street scene with a blue tarp shape on the road
  edge, bench, terraces of the city edge on the right.
- 1 (170,66,8) west: concrete roadblock across the road ahead; leaf hedge
  front on the left sidewalk.
- 2 (-136,66,136) west, deadzone: debris scene on the road, wide stone
  sidewalk in front.
- 3 (-248,66,172) north: legacy roadblock (stone blocks) in the road,
  hedges, the deadzone wall with razor wire on the right.
- 4 INVALID for parks: looks down between towers (camera placement).
- 5 aerial over the parks at 16..47,16: a plaza with seating groups and a
  white fountain pool with water.
- 6 aerial over the deadzone park at -176,144: stone plaza with leaf
  planters, benches, scattered dead bushes.
- 7 (-248,67,60) north: a cream awning with pillars on the right building,
  roadblock ahead.
- 8 (8,67,60) north: two hotel fronts facing each other form a glass
  walkway over the road on stone pillars; leaf hedges cover both
  sidewalks.
- 9 INVALID: black, camera inside a building (z 64 is no street there:
  superblock).
- 10 (-60,67,8) west: plain street, a wreck in the lane.
- 11 (-248,67,100) south: plain street with lamps, bin, bench.

## lc_streets_v0.27.0/ (district street parts, seed 1, 10 views, 9 valid)
- 0 (-200,67,8) west, office district: asphalt with white edge lines and a
  double dashed centre line, raised sidewalks with curbs and a paving strip,
  lamp posts; the blue tarp scene ahead.
- 1 (170,67,8) west, suburb: hedge rows along the sidewalks, a roadblock
  scene ahead.
- 2 (-248,67,60) north: north south street (turned part): centre dashes run
  along the street (line meta flip works), wooden benches, a white zebra
  patch in the foreground.
- 3 (8,67,60) north: hotel fronts' glass walkway over the part street.
- 4 (-60,67,8) west: residential hedges, plain road.
- 5 (-248,67,100) south: like 2.
- 6 aerial over a crossing near (-30,-20): white crossing marks (X pattern
  of quartz), wrecks, houses with brown roofs.
- 7 aerial at (8,90,240) north: a wide stairs part between levels, the
  deadzone district (sand bags, crates) on the left.
- 8 INVALID / unclear: (-136,67,136) west shows a railed platform and a
  blue water like area with mirrored buildings.
- 9 (72,67,200) north: legacy street (our own lamps, centre line), stairs
  railing in the foreground.

## scope_v0.28.0/ (scope test, ak74 with 4x, aiming, dev client)
- scope_pip.png: original picture in picture scope: normal FOV, the glass
  shows the second camera view.
- scope_zoom.png: view zoom (FOV x 0.284 = 4x): world magnified, gun and
  scope the same size as in scope_pip, glass shows the centre of the
  zoomed frame with the reticle. An F3 debug overlay is open in this one
  (key pressed during the run, not the test).
- scope_mag1.png: control, zoom off (4x set to 1.0): gun the same size
  again, so the zoom does not scale the gun.
- sheet.png: the three side by side. (An earlier zoom shot with the glass
  not drawn showed a solid black eyepiece: replaced.)
- scope_normal.png (v0.28.1, camera zoom 4.00): world and gun zoom
  together, the scope ring fills the middle of the screen, the glass shows
  the zoomed road with the reticle (see-through look); eyepiece slightly
  above the screen centre.

## scope_v0.28.2/ (every sight, two windows, v0.28.2)
- scope_<854x480|900x895>_<reddot|2x|4x|8x|integrated>.png, sheet.png
  (top row 854x480, bottom 900x895, red cross = screen centre): the
  reticle sits on the screen centre in all 10; the glass is see-through
  (road and trees continue across its edge); 8x and the integrated aug
  scope fill the screen with the glass, ring edges at the sides.

## scope_v0.28.3/ (every sight, two windows, region copy, v0.28.3)
- same layout as scope_v0.28.2 (sheet.png, red cross = screen centre):
  reticle on the centre in all 10, glass see-through, the gun's front sight
  no longer visible inside 2x / 4x / 8x glasses. (A depth only glass try
  showed the front sight post inside the glass and a grey integrated scope:
  not kept.)

## scope_v0.28.4/ (sniper overlay from 4x, v0.28.4)
- sheet.png (top 854x480, bottom 900x895, red cross = screen centre):
  reddot and 2x as in v0.28.3 (gun, see-through glass); 4x, 8x and the
  integrated aug scope: black screen with a round view, a soft dark edge,
  the scope's reticle (4x / 8x posts and bars, aug circle) on the centre,
  no gun, HUD still on top. One earlier run's integrated 900x895 shot
  showed the player off position (test glitch, rerun fine).

## props_fps/ (prop fps test, 9 Oktober 2026)
- props_0..3.png, sheet.png: empty stone platform; the 15 x 15 grid in
  view (car wrecks and street lights dominate the skyline); the grid boxed
  in stone; the box alone. sheet_cached.png: empty / in view after the line
  of sight cache: props and a few NPCs still render. cityfps.png: the
  -Pcityfps view (city street looking north from z 120).

## lc_quality_v0.28.10 (converted building quality pass, 9 Oktober 2026)

Seed 1, Decimation world type, pack rebuilt with the fixed translator;
camera spots from tools/mapbuildings.py (2 per building, mid storeys).
- sheet_interiors_before_stairs.png: 30 views. Many land inside walls or
  look out of a facade (mapbuildings spots fit hand-built maps better
  than Lost Cities floors); the rest show offices (desks, computers,
  chairs, bookshelves), lobbies with light stone brick floors. No lamp
  blocks on floors. Ceilings have recessed redstone lamps (dark brown
  with orange) in a grid. Views 4 / 11 / 15: stairs of beige sandstone
  next to black asphalt blocks (black sandstone stairs still beige).
- sheet_stairs_after.png: the same stair spots after the stairs / slab
  fix: cobblestone stairs and slabs, no beige left. Doorway header of
  oak planks over red brick comes from the source building.

## lc_highway_v0.29.0/ (L link, seed 1, pregen 1100,1460)
- sheet_l_link.png: 0 aerial from the north west: the highway runs east
  then turns south at a corner, graded land and a pond beside it. 1 on
  the x run looking east and 2 on the z run looking north: hedge median,
  adboard tower ahead, lamp arms with redstone lamp heads. 3 side view of
  the z run's bridge chunk: iron bar rails over a small gully. 4 the z run
  entering city B: road continues into the city street, buildings ahead.
  5 blocked (camera against a median tower).
- sheet_l_link_b.png: 0 the x run entering city A (1066,67,1411 west):
  road narrows into the city street between graded grass terraces. 1 top
  down on the corner (1160,100,1416, top of image = south): the two runs
  meet in the crossing square, outer sides face open land, side cuts
  graded. 2 the bridge chunk from the east: rails, graded slopes.
