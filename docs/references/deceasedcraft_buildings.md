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
