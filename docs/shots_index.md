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
