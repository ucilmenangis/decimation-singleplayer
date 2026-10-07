# City engine: Lost Cities style cities from converted content (v0.24, 8 Oktober 2026)

User direction (8 Oktober 2026): use DeceasedCraft's buildings as the
city content instead of procedural buildings, with the Lost Cities layout:
road, sidewalk and building doors at the same height, the city divided by
chunk and height level, a structure (stairs / bridge) between street
levels. Content study: docs/references/deceasedcraft_buildings.md.

## Content (offline, local only)

`python3 tools/lcpack.py OUT_DIR dc=<data>:deceasedcraft legacy=<data>:c70cities`
(venv python) converts every building / multi building the packs' city
styles list (rotation variants are separate entries there), plus each
style's stairs parts, into `OUT_DIR/<pack>/*.schematic` and
`OUT_DIR/index.json` (per building: file, pack, styles with weights,
size in chunks, groundY = layer of the ground floor, height; stairs per
style; "names" = id -> block name of the registry world). OUT_DIR is
`<instance>/config/decimation_worldgen/lc/` (dev: run/client and
run/server; Prism: the instance's config). DeceasedCraft content: never
committed (dev/run is git ignored). 2026-10-08: 290 buildings (170
current beta, 120 legacy), 8 stairs styles, 1.7 MB.
Cellar padding under multi building chunks with fewer cellars is bedrock
= "keep the world".

## Engine (`dev/src/main/java/net/decimation/worldgen/city/`)

- `LcContent`: loads index.json at init, remaps schematic ids to this
  world's blocks by name, caches schematics (`shape`).
- `LcCity`: replaces CityDistrict + paintStreets in CITY sectors when
  content is installed (`LcCity.enabled()`; `-Ddeciworldgen.lccity=false`
  switches back to the procedural city).
  - Cell = 4 x 4 chunks (the existing city cell). Chunk column 0 and row
    0 of each cell are STREET chunks, the 3 x 3 rest holds buildings.
  - Level per cell 0..2: smooth value noise on a 3 cell lattice; at most
    the distance in cells to a non city cell, so the city meets the land
    at level 0. Ground G = 64 + 6 * level (Lost Cities FLOORHEIGHT).
  - District style per 2 x 2 cells ("dc:suburb_residential",
    "legacy:deadzone"...), uniform over the styles in the content.
  - Buildings: weighted pick (style factors) of buildings that fit the
    free part of the 3 x 3 block (multi buildings up to 3 x 3 chunks),
    cellars must stay above y 4 and the top below 250 (laboratory with 90
    deep cellars, casino 276 high are skipped). 6% of chunks stay empty
    lots (grass, dead bushes). Placed with their ground floor layer at G
    (`FixedBase` plans: Slices skips terrain sampling).
  - Streets: road surface at G; a street chunk whose street continues
    into a cell one level higher gets a stairs part of the district,
    turned so its high side (west in the data) faces that cell.
  - Street dressing (v0.24.1): 3 wide sidewalks (double stone slab) flush
    with the road on both sides of straight street chunks, corner squares
    at crossings, dashed centre line (Road_CenterLine, meta 4 north-south /
    2 east-west), street lights on the sidewalk's road edge (one per side
    per chunk, sides staggered, 25% missing), benches and bins on the inner
    column facing the road, trash bags (6 / 256 of sidewalk cells), wrecks
    in lanes 5 / 10 (7% per lane per chunk). Facing rules are the old
    street's (prop front east 2, south 3, west 4, north 5). Stairs chunks
    get no furniture or cars. Chances use a full 64 bit integer hash (a
    double scaled to a long had zero low bits: every chance passed).
  - Zones: POLICE per building, MILITARY in the deadzone district.
- Rules taken from Lost Cities' source: street surface at G, ground floor
  floor layer at G, cellars below, stairs at G + 1 toward the higher
  neighbour (LostCityTerrainFeature.generateStreet / generateBuilding /
  generateStreetDecorations); floors -cellars..F with the top part at
  floor >= F (BuildingInfo).

## Verified (seed 1, Decimation world type, fresh server world)

119 converted buildings placed in the spawn area (102 current, 17
legacy), levels 64 and 70, no generator errors. Shots
`docs/shots/lc_city_v0.24/` (docs/shots_index.md): streets level with
building fronts, several stairs designs joining levels.

## Open

- City edge: since v0.24.1 a 10 block band outside the city is ramped
  (smoothstep) from street level to the natural height (`EdgePlan`, a
  Graded plan that writes no blocks of its own); on higher land it reads
  as 1 block grass terraces: smoother / wider ramp or raising edge cells
  later.
- Districts are uniform random; deadzone should follow dead land.
- Bridges, building fronts, parks, highways of the packs are not used.
- Multi buildings bigger than 3 x 3 chunks (towers, casino, school,
  laboratory) need bigger blocks (merge cells).
- Building rotation: only the rotation variants the data lists.
- Loot: chests became Decimation wood crates (loot works by block).
