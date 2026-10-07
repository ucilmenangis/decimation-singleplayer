# Worldgen architecture v3 (APPROVED by the user 8 Oktober 2026)

Why: `Building.java` (~2000 lines) does shell, facade, floor plans, rooms,
furniture, decay and grading in one class; every new building type would
add special cases. Goal: a generic engine + data assets, so new building
types (houses, garage, vehicle / food stores, police station, military
base...) are mostly data, interiors improve version by version, and the
user can design content in game. Based on docs/references/worldgen_study.md
(Lost Cities, Recurrent Complex).

User decisions (7 Oktober 2026):
- Content: HYBRID. Hand-designed storey parts / special rooms where quality
  matters + procedural planners and furniture sets for variety and for
  footprints no part fits.
- In-game CAPTURE tool early: build in creative, save as a part or set.
- Footprints snap to SIZE CLASSES so designed parts fit exactly.

## Layers (each with its own package and data)

1. **City planner** (`city/`): sectors -> cells -> lots (exists), plus the
   choice of building TYPE per lot by weighted selectors per sector /
   district (like Lost Cities city styles), streets, yards.
2. **Building types** (`types/*.json`): category (civilian / police /
   military / medical / industrial), zone, sectors allowed, size classes,
   floors min / max, facade style(s), storey programme (which planner or
   parts per storey, with conditions: ground, top, floor range, cellar),
   loot profile, weight.
3. **Shell** (`building/shell/`): footprint, storeys (5 high), outer walls
   + lining, facade from a FACADE STYLE (palette + window pattern + trim +
   entrance + ground storey treatment), vertical circulation (stair core,
   ROTATED core for narrow blocks, ladder shaft, later lift shaft), roof.
4. **Storey plan** (`building/plan/`): a grid per storey: cell kind (open,
   wall, door, glass, lining, core, furniture) + ROOM id and room type,
   doors with connections. Produced by a planner:
   - `PartPlanner`: an authored storey part (char grids per layer, palette,
     ROOM SLOT chars that tell the furnisher which room type a region is,
     fixed furniture allowed);
   - procedural planners (current code, extracted): `CorridorFlats`
     (apartments), `OpenPlan` / `Cellular` (offices), `SalesFloor` (shops),
     later `HouseRooms` (rooms composed by connections, maze style).
5. **Furnisher** (`building/furnish/`): furniture SETS per room type (exists:
   docs/furniture_sets.md), room programmes (which slots in which order),
   walkway keeping, later wall decor (paintings as entities, curtains).
6. **Layers / transformers** (`building/layers/`), applied in order:
   `Ruins` (smooth decay field + structural support: nothing floats;
   breaches, collapsed corners, rubble only where something fell),
   `Story` (bodies, notes, blood, survivor camps, barricades, looting),
   `Overgrowth` (biome vines, moss, leaves, snow, sand), `Loot` (crates by
   loot profile and room type).
7. **Writer** (exists): `Plan` + `Slices` + `StructureData`, slice by slice.

## Assets (data, user editable, `/deciworldgen reload`)

`config/decimation_worldgen/` gets `types/`, `parts/`, `palettes/`,
`styles/`, `sets/` (exists), `conditions/`. Built-ins ship in the jar and
are copied on first start (as sets already are); a config file with the
same name replaces the built-in.
- palette: char -> block[:meta] | weighted list | prop with facing |
  special (loot crate by profile, set slot, door by role).
- style: weighted palette lists (one part, many looks).
- condition: ground / top / floor / range / cellar / biome / sector /
  category tests, used by parts, sets and loot.

## Size classes

Footprints snap to classes so parts fit: S 12x12, M 16x16, L 24x24,
LONG 12x24 (and 24x12), WIDE 16x24. A 26x26 lot holds any of them with a
yard. Types list the classes they allow; the procedural planners still
handle any size, so classes constrain only where parts are used.

## Capture tool (early)

`/deciworldgen capture set <name>` and `/deciworldgen capture part <name>`
with two corners marked by a wand item or `pos1` / `pos2` subcommands:
reads the blocks, builds the char grids and an automatic palette (one
char per block+meta, props with their facing converted to "face"
relative to the chosen back wall / front), writes JSON to the config
folder, reloads. Then `/deciworldgen rebuild` shows it in the city.

## Migration (one step per version, no regressions)

Each step verified with wallscan, gradescan, multiscan, floorplan
reachability, audit / flats / sets screenshots and the autotest.
1. Asset core: palette / style / condition loaders; capture command.
   DONE 0.22.0 (package `assets`: AssetDir, Palettes, Condition,
   Capture; docs/furniture_sets.md). Conditions are inline `"when"`
   objects for now (no conditions/ folder yet); captured parts are
   written but not consumed until step 4.
2. Extract Shell, StoreyPlan, Furnisher, layers out of `Building` without
   behaviour change (compare floor plans before / after).
   DONE 0.22.1: package `worldgen/building/` (code map below); storey
   height is a per building field (5 everywhere). Verified with
   `tools/worlddiff.py`: seed 1 world before / after, 0 differing blocks
   over 589 final chunks, 282 of them with building interiors (173701
   interior blocks); autotest passes.
3. Express apartment / office / shop as type JSON on top of the extracted
   modules; rotated stair core.
4. PartPlanner: authored storey parts with room slots (first parts made
   with the capture tool).
5. Ruins transformer replaces the per-cell decay rules.
6. New types: house, garage, food store, vehicle store, police station,
   military base (each = type JSON + parts / sets + maybe one planner).

## Input from the map studies (8 Oktober 2026)

docs/references/decimation_maps.md and deceasedcraft_buildings.md:
- DeceasedCraft (79 building types) is 100% authored storey parts, no
  procedural rooms: supports the PartPlanner + capture tool priority.
- Room scale: 3x4..5x5 rooms, 6 to 8 per flat, a door each; furniture
  density 0.35 in flats, 0.25 offices, 0.12 shops / houses.
- Public rooms use rows and islands (waiting rows, desk islands, booths,
  shelf aisles): a row / island placer next to the wall sets.
- Style layer: two tone walls (WallOffice bottom + top), ceiling light
  grid + vents, floor by room function.
- Ruins layer: dirt / leaves / water / cracked glass on intact shells
  first, holes second.
- Building type list and district styles (suburb residential, retail,
  highrise residential / office / hotel) as the target catalogue.
- DECIDED (user, 8 Oktober 2026): storey height per building type.
  Public buildings (office, police, hospital, shop, military) get 6 high
  storeys (floor, 4 air, ceiling); houses and apartments stay 5 (floor,
  3 air, ceiling). Plan: step 2 turns Building.FLOOR into a per building
  storey height (still 5 everywhere, identical output), then a separate
  version switches offices and shops to 6 (stair runs 6 steps).

## Code map (0.22.1, `dev/src/main/java/net/decimation/worldgen/building/`)

- `Building`: identity, lot, kind, floors, storey height, stair core /
  corridor geometry, `blockAt` dispatcher (margin, collapse, roof, core,
  floor layer, outer wall, ladder, then Interior), dev camera helpers,
  the `unit` hash every part uses (pure: call order never matters).
- `Shell`: facade palette, outer walls with windows and the entrance,
  `liningOpen`, stair core runs, ladder, roof edge, margin vines.
- `StoreyPlan`: cell grid, room grid, 3 furniture layers, `put`,
  `wallLine`, `markRoom`, `lining`, `markCore`; cell and room constants.
- `ApartmentPlanner`, `OfficePlanner`, `ShopPlanner`: fill a StoreyPlan.
- `Furnisher`: furniture set placement (weighted order, fits, styles,
  conditions); `Facing`: plan direction to metadata per block type.
- `Surfaces`: wall panels, floors per room, ceiling layer, light grid and
  vents, lintels, room doors.
- `Interior`: plan cell to blocks above the floor layer.
- `Ruins`: decay value, collapsed corner, rubble, debris, overgrowth on
  surfaces, looted furniture.
- `Yard`: lot grading, parking, paths, nose-in wrecks.

## Open questions (for later)

- Interiors spanning several storeys (atriums, stair halls).
- Basements / cellars (Lost Cities style) and underground parking.
- How many footprint classes the city planner should mix per block.
