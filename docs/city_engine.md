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
    "legacy:deadzone"...): since v0.24.3 the wasteland district (legacy
    deadzone) within 2 cells of a military sector, elsewhere a weighted
    pick (current beta districts weight 2, legacy town styles 1). Seed 1
    spawn area: 98 current, 35 deadzone buildings.
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

## District street parts (v0.27.0)

User choice 8 Oktober 2026 ("really big gain own street pieces"). Each DC
city style lists street parts (citystyle "streetblocks" / "parts": all,
t, straight, bend, end, none, full; 61 parts, e.g. bus stop, newspaper
stand, adboard, police roadblock variants). Legacy styles have none: the
deadzone and the legacy towns keep our own dressed streets (v0.24.1).
- Placement as Lost Cities' generateNormalStreetSection: count the
  connections (neighbour street chunk at the same street level, or a
  highway chunk at the deck level); 0 none, 1 end, 2 straight or bend, 3
  t, 4 all. Unturned: straight runs along x, end opens west, bend west +
  north, t all but south. Its Transform ROTATE_90 maps part (x, z) to
  (15 - z, x), which is exactly one of our clockwise turns, so LC
  rotations map 1:1 to turns. Slice 0 at the street surface G. Stairs
  chunks keep our road + stairs part.
- Parts: 16 x 16, 5 wide sidewalks raised by half slabs, a 6 wide road
  (part z 5..10), lamps and benches of their own. Our furniture is not
  added; scenes and fronts still are; wrecks in lanes 6 / 9 (7%).
- Road paint: the refueled mod's decals (side, corner, side_corner, zebra,
  about 1800 blocks over the 61 parts) are converted by lcpack.py
  `paint()` into the layer below: lines -> deci:BlockRoad_CenterLine
  (meta 2 along x for paint facing east / west, 4 along z), zebra ->
  white quartz. Turning a part by an odd number of turns flips the line
  meta (XOR 2) in `LcCity.partAt` (Rotation leaves Decimation blocks
  unchanged). Translator additions: oxeye daisy, lily pads, ochrum,
  construction barricade (hazard barrier), command block / observer skip.
- Shots lc_streets_v0.27.0 (shots_index); autotest all pass, 0 generator
  errors.

## Parks, street scenes, fronts (v0.26.0)

User said "try next" on 8 Oktober 2026 after testing 0.25.0; this is the
next listed option. Rules read from Lost Cities (BuildingInfo,
LostCityTerrainFeature.generateStreet / generateFrontPart).
- Pack: `tools/lcpack.py` exports each city style's selectors "parks",
  "fountains", "fronts" into index.json "decor" (repeats = weight; road
  override as highways). 8 styles: dc 11 parks each, 1..2 fountains, 1..5
  fronts; legacy 12..14 parks, 9..10 fountains, 4 fronts.
- Parks: open lots now LOT_CHANCE = 10% of building chunks (was 6%) and
  carry a park part of the district (fallback legacy:standardcity, never
  for the deadzone) on their grass, layer 1 up (as Lost Cities: park part
  at street level + 1). Logged as "lc park". Seed 1 spawn: 16 parks,
  plazas with benches and a fountain pool, deadzone plazas with planters.
- Street scenes: DeceasedCraft's "fountains" are scenes in the road (bus,
  ambulance, roadblock, trash; legacy has real fountains too). 6% of
  straight street chunks without stairs get one, turned along the road;
  no extra wreck in that chunk. Logged as "lc street scene".
- Fronts: a straight street chunk looks up the chunk beside it on each
  side along the street, in that chunk's block plans (resolved lazily at
  write time, so block plans never recurse; the far side of the street
  belongs to another block and works too). A building chunk at the same
  street level gets one of its district's fronts with FRONT_CHANCE = 0.5,
  turned so the part's x 0 side faces the building (west 0, north 1, east
  2, south 3). Street chunks are now 13 high (fronts up to 12). Fronts are
  0..7 deep, so they cover the sidewalk and part of the outer lane; two
  hotel fronts facing each other read as a glass walkway over the road.
- Shots lc_decor_v0.26.0 (shots_index). Autotest all pass, 0 generator
  errors.

## Highways (v0.25.0, `city/Highways.java`)

User choice 8 Oktober 2026 (over fronts / parks, giant buildings,
interiors). Lost Cities' own bridges are road decks across non city
chunks between level 0 city streets (BuildingInfo.calculateXBridge); our
cities fill whole 256 block sectors, so those gaps barely exist. Highways
between cities do the job instead.
- Network (pure function of the seed, `tools/hwmap.py` is the Python
  copy): each region row has one highway chunk row (region start + 0 or 8
  chunks, by hash). That is always a city street row, superblocks
  included (they sit on even cells). Where the row leaves a city region and
  the next city region lies at most MAX_GAP = 3 non city regions away,
  every chunk between is an east west highway. Region columns the same
  way (north south). Both on one chunk: a crossing part. Seed 1: x row
  chunk 0 from x 256 to 511, z column chunk 0 from z -512 to -1.
- Deck at DECK = 64 (city level 0 street surface): parts are placed with
  slice 0 (deck underside) at 63, so the road (slice 1) is flush with the
  city street it continues.
- Kind per chunk, decided by the first slice from the terrain it can see
  (5 x 5 soil samples) and stored in StructureData as "hw_X_Z": TUNNEL when
  the median is 6+ above the deck, BRIDGE over water or ground more than 2
  below, else OPEN. Parts from the pack's world style "highways" selector
  (dc modern: 13 open, 13 bridge with adboard / sideblock / crash
  variants, tunnel, the 3 _bi crossings). Open and bridge chunks clear 20
  blocks above the deck; open chunks fill dirt down to the ground; bridges
  get 2 x 2 stone brick pillars at both rails of every chunk down to solid
  ground (through water). Wrecks in lanes 4 / 11, 8% each per chunk.
- Side ramps: land beside OPEN chunks ramps from the deck to the natural
  height over 3..8 blocks (2 per block of difference, smoothstep, shared
  `LcCity.reshape`); columns within the city edge band are the city's.
  The city edge ramp skips highway chunks.
- Small and large sites within 4 blocks of a highway chunk are dropped.
- Pack: `tools/lcpack.py` exports the highway parts into index.json
  "highways"; the deck asphalt (Biomes O' Plenty black sandstone) becomes
  deci:BlockRoad there (plain translation gave beige sandstone).
- Checked (seed 1): 13 highway chunks in the server spawn area (11 open,
  2 bridges over a ravine), more in the client run (7 bridges, 5
  tunnels); side profile at z -128 climbs 4,5,5,6,6,7,7 above 60 from the
  road; edge band still 0.3% steps over 1 block; autotest all pass. Shots
  lc_highway_v0.25.0(b), lc_tunnel_v0.25.0.
- Open: a tunnel right at a city's edge ends in the graded edge band
  (reads as a short underpass); bridge chunks get no side ramp (a hill
  next to the deck stays a wall); highways only join cities in the same
  region row / column (no diagonal links); lamp heads translate to coal
  blocks (faithful to the data `[not verified]` against 1.20 look).

## Verified (seed 1, Decimation world type, fresh server world)

119 converted buildings placed in the spawn area (102 current, 17
legacy), levels 64 and 70, no generator errors. Shots
`docs/shots/lc_city_v0.24/` (docs/shots_index.md): streets level with
building fronts, several stairs designs joining levels.

## User review

- 8 Oktober 2026, 0.24.4 tested in Prism: "i love it for oneshot
  progress". Next chosen: highways between cities (over the bridge,
  fronts / parks, giant buildings and interior options).

## Open

- City edge (v0.24.4, replaces the v0.24.1 band of 10): every city cell
  with open land among its 8 neighbours has an `EdgePlan` that ramps the
  land up to EDGE = 24 blocks out (smoothstep, Euclidean distance so outer
  corners are rounded, width 2 blocks per block of height difference,
  6..24, contours wobbled +-4 blocks by value noise so the 1 block steps
  do not run parallel to the street). A column belongs to the nearest city
  cell only (ties to the lower cell): no column is graded twice.
  `populate` now looks at cells within EDGE of the window (before, the
  outermost 2 band columns were never written). Small sites and large
  sites within EDGE of a city are dropped (the ramp would cut under them).
  `tools/edgescan.py WORLD [R]` measures it (sector map recomputed in
  Python): seed 1, 9024 band columns, neighbour steps over 1 block 1.2%
  (v0.24.3) -> 0.3% (v0.24.4); the 3 block steps left are natural hills
  and tree tops. It still reads as 1 block grass terraces from the street
  (shots lc_edge_v0.24.4b), which is plain Minecraft terrain.
- Fronts, parks and street scenes since v0.26.0, district street parts
  since v0.27.0, highways since v0.25.0. Not used yet: Lost Cities
  bridges (our cities have no gaps for them), rail parts.
- Superblocks (v0.24.2): 30% of aligned 2 x 2 city cell groups (all 4
  city) become one block: streets only on its first chunk row / column,
  7 x 7 building chunks, one level (the lowest of the 4), a landmark
  (multi building over 3 x 3 chunks) placed first at its corner. Seed 1:
  the courtyard office tower (4 x 4, 192 high). Still too big / tall:
  casino (276 high), oasis condo (top above 250 with its cellars),
  laboratory (90 deep cellars), terrace plaza 5 x 7 fits.
- Building rotation: only the rotation variants the data lists.
- Loot: chests became Decimation wood crates (loot works by block).
