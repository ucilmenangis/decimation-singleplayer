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
