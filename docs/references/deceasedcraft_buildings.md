# Study: DeceasedCraft city buildings (8 Oktober 2026)

The user pointed at their DeceasedCraft instance ("so much building and
type building in there"). Read this instead of re-extracting.

## Where the data is

- Prism instances `v1` and `v2` = DeceasedCraft_Beta 5.10.15, Minecraft
  1.20.1, 309 mods, Lost Cities 1.20-7.4.11. Both `saves/` folders are
  EMPTY; the generated world the user played lives on the server
  `unless-yearling.gl.joinmc.link` (only a Xaero map cache is local, no
  blocks). So no world to survey.
- The buildings themselves are data inside
  `mods/DCTweaks_5.10.14.jar`: `data/deceasedcraft/lostcities/` (Lost
  Cities format, see docs/references/worldgen_study.md): 79 building
  groups (35 single chunk, 37 multi chunk, 7 scattered), 456 building
  files (4 rotations each), 7945 storey parts, 389 palettes, 224
  conditions, 6 city styles, world style `modern`. Profile
  `config/lostcities/profiles/deceasedcraft.json` (cities rare but big:
  chance 0.003, radius 150..200, no explosions, no ruins, 1..5 floors).
- Content is DeceasedCraft's (C70 / TqLxQuanZ): study only, never copy
  into our public repo.
- `tools/lcstudy.py DATA_DIR deceasedcraft OUT` (needs pillow): per
  building buildings.tsv (chunks, floors, interior cells, furniture by
  category, density, doors, glass, top items), furniture.tsv, and
  plans/<name>_s<storey>.png (furniture layer top down, coloured by
  category). Extract with
  `unzip DCTweaks_*.jar 'data/deceasedcraft/lostcities/*' -d <dir>`.
  Multi chunk plans may have chunks transposed `[not verified]`.

## Building types (the catalogue we can aim for)

Civilian homes: residential a..d (1 chunk houses, 1 storey + roof),
residentiale/f, residentialsurvivor (survivor modified house),
apartmentsmall a..d (+ broken variants), apartmentmedium, flatapartment,
flatlarge, flatmedium, oasiscondo (34 floors), taigaresidence, timbertower,
thering, cabins 1..3, farmhouse, treehouse, hideout, campsite.
Retail / food: clothstore, premiumclothstore, fastfood, restaurantsushi,
flowershop, hardwarestore (+ damaged), convenientstore, gasstation (single
and multi), gunstore, cafe, casino, gallery, foodtruck.
Work / public: office1..5 (a/b/c variants), officebank,
courtyardofficetower, officethefirst, filmworkstower, communitycenter,
verticalschool, workshop, terraceplaza.
Police / medical: policeoffice1, policestation, policeapartment,
hospital (+ damaged), polyclinic.
Other: planecrash, sunkenclub, sunkenmonumentpark, hotel a..d.

City styles pick buildings per district: `suburb_residential`,
`retail_district`, `highrise_residential`, `highrise_office`,
`highrise_hotel` (each a weighted list incl. the 4 rotations, its own
street parts). Matches our planned "types per sector / district".

## Measured patterns (lcstudy, plans viewed)

- **Storey = 6 slices**: floor, 4 blocks of room, ceiling layer (we use
  5: floor, 3 air, ceiling). Their extra block of height leaves room for
  ceiling lamps, fans and tall furniture.
- **Fully authored per storey**: every building lists ground, floor1..n
  and top parts explicitly (plus `parts2` overlays); variety comes from
  many buildings and 4 rotations, not from procedural rooms.
- **Furniture density** (furniture cells / interior cells over the two
  lowest room layers): small apartments 0.35..0.37, hotels 0.33..0.53,
  medium flats 0.24..0.35, offices 0.24..0.29, shops 0.10..0.18, houses
  0.11..0.14, cabins near 0. `[not verified]` what ours measures; add the
  same metric to tools/floorplan.py to compare.
- **Small apartment floor** (1 chunk, about 13x13 inside, one unit per
  floor): 6 to 8 rooms of 3x4 to 5x5: 2 bedrooms (double bed 2x2 on a
  wall, nightstand / storage each side), a 2 to 3 cell bathroom (toilet,
  basin, shower or tub), kitchen as an L or a run of 3 to 4, living with
  sofa + TV, a small hall. Interior walls 1 thick, a door per room.
  Curtains beside every window, light switches next to doors, ceiling
  fans, small clutter (plates, toaster, bread crate, radio, potted
  flowers).
- **Flat apartment** (3 chunks): 4 units mirrored along a corridor, bath
  in the outer corners, kitchen runs on the shared walls `[inferred from the plan colours]`.
- **Houses**: open plan, a straight kitchen run of 4 to 6, one bathroom
  of 1 to 2 cells, a sofa pair, double front door, few items.
- **Offices**: glass curtain walls on 2 to 3 sides, open floor with desk
  ISLANDS (2x2 desks, chairs both sides, a shelf at the end), a core
  strip with WC and kitchenette.
- **Police office**: ground floor reception counter run of 6 with chairs
  both sides, back offices; upper floor desks + storage; cells via rows
  of doors (policeapartment: 389 doors).
- **Fast food / cafe**: booths of table + 2 chairs along the glass front,
  a long counter, a kitchen behind with parallel runs.
- **Hardware / clothing store**: perimeter shelving plus aisle runs.
- **Most used items** (furniture.tsv): ceiling light slabs, desks,
  drawers, lootr chests (loot in every building), bookshelves, shower
  taps, shelves, cabinets, beds, baths, toilets, carpets, chairs,
  fridges + freezers (paired), stoves, sinks, sofas, monitors, curtains,
  hedges / leaf carpets (overgrowth), broken TVs and radios, trash bags.

## Takeaways for our generator

1. Confirms the hybrid plan: authored storey parts give the quality;
   DeceasedCraft has no procedural interiors at all.
2. Rooms should be SMALLER and MORE: 3x4..5x5 rooms, 6 to 8 per flat,
   each with a door, instead of our large living spaces.
3. Every room gets wall decor equivalents: curtains (wool) by windows,
   a switch / panel by doors, a ceiling light per room.
4. Public buildings use rows and islands (booths, desk islands, counters
   with chairs both sides, shelf aisles).
5. Consider a 6 high storey (4 air) for public / office types
   `[not verified]` cost: needs Slices / StructureData and stair core
   changes.
6. A building type catalogue and district styles to copy as our own
   types (names only, our own layouts with Decimation props).

## Conversion to 1.7.10 (proof, 8 Oktober 2026)

`tools/lc2schem.py DATA_DIR deceasedcraft REGISTRY_WORLD OUT_DIR BUILDING...`
(venv python: needs numpy for mapsurvey) turns a Lost Cities building
(single or multi) into a raw .schematic with this instance's block ids;
`tools/lctranslate.py` translates 1.20 states by rules (shape words:
stairs / slab / wall / pane / door / trapdoor; materials; colours;
furniture categories to ONE CELL Decimation props or vanilla stand-ins, so
no model draws over its neighbours, see docs/prop_footprints.tsv).
5 buildings: about 98% of blocks translated; leftovers are small decor
(soap dish, plush toys, food). `/deciworldgen paste NAME X Y Z` writes
`config/decimation_worldgen/paste/NAME.schematic` raw. Result in
docs/shots_index.md "dc_proof_v0.23": interiors read clean and detailed.
Converted files are DeceasedCraft content: never commit them.
Open: stair / door metadata mapping checked only by eye `[not verified]`;
rotations (the _90/_180/_270 variants exist in the data).

## Full catalogue (step 1 of the DeceasedCraft plan, 8 Oktober 2026)

`tools/dcinventory.py DATA_DIR MODS_DIR LCSTUDY_DIR OUT_DIR` (venv python)
reads the DCTweaks data, the structure files inside the mod jars and the
lcstudy output. Tables (names, sizes, counts only, no content) in
`docs/references/dc_catalogue/`: `buildings.tsv`, `infra.tsv`,
`structures.tsv`.

What a DeceasedCraft world is made of:
1. **Lost Cities city** (the main content): 79 building groups. Counts by
   category: residential 30 (small apartments a..d plus broken versions,
   hotels a..d, houses residential a..f, survivor house, flats small /
   medium / large, condo tower 34 floors, taiga residence, cabins,
   farmhouse, treehouse), office / tower 15 (office 1..5, bank, courtyard
   tower 24 floors, ring tower 27 floors, timber tower, terrace plaza),
   retail / food 13 (clothes, premium clothes, fast food, sushi, flower
   shop, cafe, convenience store, gun store, casino 33 floors, club, gas
   stations, food truck), public 5 (community centre, film studio
   tower, gallery, vertical school 33 floors), industrial 4 (hardware
   stores, workshop), police 3 (police office, police station, police
   apartment), medical 3 (hospital, damaged hospital, polyclinic),
   wasteland 3 (campsite, plane crash, hideout), other 1 (sunken
   monument park).
   Districts (city styles, share of each style's building picks):
   suburb_residential (houses, survivor house, community centre, police
   station, polyclinic), retail_district (shops, cafe, convenience,
   gun store, hospital, gas station, terrace plaza), highrise_residential
   (small apartments, flats, condo, school, taiga residence, police
   apartment), highrise_office (offices, bank, workshop, towers, film
   studio), highrise_hotel (hotels, casino, ring tower, gallery, club,
   monument park). Scattered outside cities (worldstyle "modern"): cabins
   1..3, food truck, treehouse, hideout, campsite, plane crash, farmhouse.
   Infrastructure parts: 68 streets, 25 parks, 20 fronts (building
   entrances to the street), 14 fountains, 12 highway, 12 rail, 7 stairs
   (between city levels), 2 bridges.
   The base Lost Cities jar adds its own 35 buildings / 14 multi
   buildings / 187 parts (vanilla style ruins) `[not catalogued]`.
2. **apocalypsenow structures** (mod jar, NOT disabled by DeceasedCraft):
   military base (26x8x37), police station (33x16x45), fire station
   (42x20x42), clinic (35x12x45), market (23x7x40), apartment block
   (45x35x30), house, mansion, scrapyard (45x12x45), storage (20x20x20),
   medical post, post, ruins 1..2, destroyed survivor camp, plus 3 small
   unnamed (sandbag posts). This is the "military base" content.
3. **Disabled by DeceasedCraft**: DCTweaks ships empty 1x1x1 copies of
   every vanilla structure (villages, mansions, shipwrecks, ancient city,
   ruined portals, igloo, outpost) and of the spore, horror_element,
   undead_revamp2 and zombie_extreme structures. Those mods' real files
   are still in their jars (spore: hospital 48^3, prison, military camp,
   lab, asylum, cathedral; zombie_extreme: gas station, cafe, modern house,
   lighthouse, ruins; horror_element: laboratory, small horror sites)
   and could be used anyway.
4. DeceasedCraft's own worldgen features are only ores.
There is no separate military "wasteland" base in the Lost Cities data;
military content = apocalypsenow military.nbt (+ spore military_camp
if wanted) `[inferred]`.

## Versions and the wasteland (8 Oktober 2026)

- Prism instances `v1` / `v2` and the user's server repo
  `github.com/ucilmenangis/DeceasedCraft-Server-5.10` (public; server
  files, mods, the played world) all run DeceasedCraft_Beta 5.10.15:
  `DCTweaks_5.10.14.jar` and Lost Cities 7.4.11 are byte identical (git
  blob hashes match). The user calls the server world the LEGACY one: it
  was generated earlier, so its buildings can differ from what the current
  data generates; treat the world and the data as two separate sources
  (user warning: "some or most of the building is little bit different").
- Wasteland in the current data is a BIOME, not structures: badlands are
  replaced by `biomesoplenty:wasteland` (config/biome_replacer.properties),
  kubejs groups them as `deceasedcraft:wasteland` for In Control spawns,
  scattered buildings are blacklisted there. The user says the wasteland
  feature is work in progress in the beta. The legacy version (with
  wasteland and military buildings) was deleted; the user may reinstall
  it as a Prism instance if CurseForge still lists it.
- Server world: 131 overworld region files (regions -19..4 on both axes,
  about 1.1 GB), plus dimensions deceasedcraft:abyss, lostcities:lostcity,
  lostworlds:abyss (data only). Sparse clone in the session scratchpad;
  never commit it. Reader for 1.18+ chunks: `tools/anvil118.py`.
- Survey of the server world (`tools/anvil118.py`, 7 minutes): 95071
  chunks in two areas: a pregenerated square of about 4000 x 4000 blocks
  (snowy coniferous / taiga / plains, cities, straight highways) and a
  travelled strip near -9700,-9700. 45 surface biomes, NO wasteland or
  badlands: this world is the current beta, not legacy. Still useful as a
  real generated Lost Cities layout (levels, stairs, bridges, highways)
  for the city engine step.

## Legacy DeceasedCraft 5.5.5 (Prism instance `legacy`, 8 Oktober 2026)

Installed by the user from CurseForge (pack 490660, file 5525524):
Minecraft 1.18.2, Forge 40.2.4, Lost Cities 1.18-5.3.29, 224 mods.
DIFFERENT content from the current beta (user warning: same names can
hold different buildings, keep the two apart). Catalogue tables in
`docs/references/dc_catalogue/legacy/`.
- City data lives in `kubejs/data/c70cities/lostcities/` (namespace
  `c70cities`, flat files: building_x.json + _90/_180/_270): 58 building
  types, 1149 parts, 1174 palettes. Profile `deceasedcraft`
  (defaultconfigs/lostcities-server.toml): cities rare but large (chance
  0.003, radius 150..200), city levels at y 75 / 83 / 91 / 99 (8 apart),
  1..5 floors, no ruins or explosions.
- City styles: standardcity (offices 1..5, apartment, police office,
  hotels, bank, flat apartment, gas station, car service, gallery, cafe,
  convenience, hospital, clubhouse, condo, mega tower 16 floors),
  residential (houses 1..4, restaurants, gas stations, community centre),
  dummycity, and **deadzone** = the WASTELAND district: the worldstyle
  multiplies city chance by 10 in biomesoplenty:wasteland /
  wooded_wasteland and those cities use deadzone: warehouses 1..2,
  factories 1..2, car factory, lab, bunker, multi warehouse / factory, oil
  pumpjack and derrick, **military base** (3x2 chunks: walled compound,
  barracks, round pad, corner towers), **military camp**, **laboratory
  complex** (3x5 chunks, glass dome, symmetric wings).
- Legacy-only types (not in the beta): all deadzone buildings above,
  bank, apartment1, restaurants 1..2, car service, clubhouse, condo1, mega
  tower, sea house, cabins 1..6. Shared names (office1..5, police office,
  hotels, residential, cafe, gallery, flat apartment, hospital, community
  centre, convenience store, gas station) may differ in content `[not
  verified]` per building.
- City parts: 26 parks, 19 fountains, 9 fronts, 7 stairs, 2 bridges; no
  street parts of its own (the base Lost Cities streets are used
  `[inferred]`).
- apocalypsenow 1.18 v2.0.7: 69 structures (current beta: 18): military
  base (48^3), military airport, military camps 1..2, looters' base,
  bases (48^3), prison (prision), police stations 1..2, fire station,
  hospital (40x32x39), pharmacy (farmacia), gun store, stores 1..5, gas
  station, factory, lighthouse, US navy ship, refugee camps, plane crash
  (acidentedeaviao), construction site, towns (cidade1, city1, canada1,
  desert1, farm), houses casa1..10, about 20 ruins (ruina*), camps.
  Names are mostly Portuguese.
- Vanilla structures are blanked here too (villages, mansions...).
- All 14 deadzone (wasteland) buildings converted with
  `tools/lc2schem.py <legacy>/kubejs/data c70cities ...` (flat layout
  supported) plus wasteland rules in `tools/lctranslate.py` (salt flats,
  paving, scoria, dead grass, razor wire, barrels, machinery). Sizes:
  singles 16x18x16; multi warehouse / factory 16x32, pumpjack 32x32,
  derrick 32x16, military base 48x32, military camp 32x16 (24 high),
  laboratory 48x80. Leftovers: single digit decor. Shots and notes in
  docs/shots_index.md "wasteland_legacy_v0.23". Schematics are local only
  (dev/run/client/config/decimation_worldgen/paste/, git ignored):
  `/deciworldgen paste multi_militarybase X Y Z` in the dev client.
- CELLARS (user reminder: "most building has really deep bunker"): 86
  legacy building files have cellars: laboratory complex 15, condo 5,
  military base and mega tower 4, hospital and clubhouse 2, many 1.
  tools/lc2schem.py now picks floors exactly like Lost Cities
  (BuildingInfo / LostCityTerrainFeature.generateBuilding, read from
  source): floors -cellars .. F, the part with "top" at floor >= F (the
  roof), F clamped to the building's min / max (exact when
  overrideFloors) and the profile's 1..5 (+1 for the top); cellars =
  maxcellars, at least the deepest explicit "floor". First matching part
  per floor (LC picks among matches at random `[not verified]` for
  buildings with several candidates). The schematic's y 0 is the bottom
  of the deepest cellar; NAME.json {"groundY": n} tells /deciworldgen
  paste where the ground floor goes. Military base: 24 blocks of cellars,
  laboratory: 90. A city engine must dig these below street level.
- ALIGNMENT TEST (8 Oktober 2026, `tools/lcscene.py`): Lost Cities
  rules read from source: street surface block at the city ground level
  G = groundLevel + cityLevel * 6 (FLOORHEIGHT 6), a building's ground
  floor part starts at G (floor layer at G, walls from G + 1), cellars
  stacked below, a stairs part goes into the lower street chunk at G + 1
  rotated toward the higher neighbour (XMIN = no rotation). Scene with
  two street levels 6 apart, 5 converted buildings and stair_deadzone_1:
  world scan confirmed road at y 70 / 76, ground floor layers at 70,
  stairs rising from 71 to 76 (the high street's surface); shots in
  docs/shots_index.md "lc_alignment_v0.23" show fronts flush with the
  road and the staircase meeting the upper street. Verified for one
  stairs part without rotation; rotated stairs, bridges and fronts not
  tested yet `[not verified]`.
- Translator audit (8 Oktober 2026, top 300 block types by use): fixed
  stone_bricks -> red brick (material order), every "...pane" skipped by
  the frying pan rule (windows missing), basalt -> sandstone (the "salt"
  rule), wooden vertical slabs -> stone. Buttons (wall only) and pressure
  plates map to 1.7 ones, tanks to dark metal, barbed wire to Decimation
  razor wire, salt flats to pale grey stone. Still skipped on purpose:
  structure void, papers / books / towels / paintings / light switches /
  fluid pipes / small food items (decor without a 1.7 block).

## Translation audit (9 Oktober 2026)

`python3 tools/lcaudit.py OUT.tsv pack=DATA:NS ...` walks every building
both packs convert and lists (a) source blocks the translator DROPS or
SKIPS, with counts and an example state, (b) every source block that
becomes a deci prop, counted by placement (floor, ceiling, wall, free).
Compare two translator versions over the same buildings with a small
script that imports both (old one via `git show HEAD:tools/lctranslate.py`);
compare two generated worlds with `tools/worlddiff.py A B --top 40`.

Found and fixed (290 buildings, both packs):
- "light" inside colour names made lamps (LED floor bug, see bug.md).
- `biomesoplenty:black_sandstone` (36451 uses, plus 804 smooth) is
  DeceasedCraft's asphalt; it matched "sandstone" and came out beige.
  Now deci:BlockRoad; its stairs / slabs -> cobblestone stairs / slabs,
  other dark stones (deepslate, basalt, scorchia...) -> deci:BlockStone_4.
- Dropped before, mapped now: buildersdelight `laboratory_*` (19491) ->
  deci:BlockStone_7; quark charcoal_block -> coal_block; magma_block ->
  netherrack; embellishcraft wallpaper -> sandstone (beige) or stained
  clay by colour; quark corundum -> stained glass by colour (clusters
  skipped); bamboo_mat -> carpet; zombie_extreme barricades and quark
  posts -> fence; refueled post -> cobblestone_wall; apocalypsenow
  shelves -> deci:BlockCardboardBoxes3 (prop facing); create seats and
  redeco cushions -> carpet by colour.
- Road paint (refueled lines, car:line) inside building parts is now
  painted on asphalt the same way as street parts (lcpack.py,
  `paint(cols, asphalt_only=True)`).
Look of laboratory panels as BlockStone_7 and of cobblestone stairs for
black sandstone: seen in photos only (shots_index "lc_quality").
