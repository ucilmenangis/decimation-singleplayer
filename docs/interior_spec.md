# Building interior and exterior spec (round 2 step 4c, DRAFT for user review)

Target: production quality ruins. Every room reads as what it was, before
the reader notices it is a ruin. Built in layers so each can be tuned and
checked on its own. Based on docs/building_audit.md (what is wrong now) and
docs/prop_catalogue.md (what Decimation offers). Room layouts follow the
research in docs/building_design.md; item lists below are general interior
design practice scaled to Minecraft `[from design knowledge, not a cited
source]`.

## 1. Principles

1. Function first: each room has a purpose and ONE focal object (bed,
   sofa + TV, desk cluster, checkout) placed first, the rest arranged
   around it.
2. Layers, in this order: structure (walls, doors, stairs) -> surfaces
   (floor, wall panels, ceiling) -> furniture -> small detail -> story /
   decay. Decay may remove or move things, never invent new rooms.
3. Circulation: a free 1 block path from every door to every other door of
   the room; furniture never on a doorway cell or the cell in front of it.
   Check: floor plan reachability >= 95% of walkable cells per storey.
4. Against walls: furniture stands against a wall or a partner piece
   (chairs at tables, nightstand at bed), never floating in the middle,
   except island pieces by design (desk clusters, shop aisles, tables).
5. No two storeys identical: each storey picks its own variant (layout
   mirror, room program, furnishing set, decay level).
6. Restraint: 40 to 60% of a room's floor stays empty.

## 2. Surfaces

| space | floor | walls (lowest course / above) | ceiling |
|---|---|---|---|
| apartment living, bedroom | planks (oak / spruce / birch per unit), FloorCarpet rug in the middle | WallOffice set per unit (pink, light blue, white, dark wood panel bottom) | Ceiling_3/4 light |
| apartment kitchen, bathroom | FloorTiles_1 or _2 | white set, Bottom with stripe | Ceiling_4 |
| corridor, stair hall | FloorTiles_3 or stone | base white set, green or red stripe | Ceiling_2, BlockLight(Off) every 4 blocks |
| office open plan, meeting | FloorCarpet_1..4 (grey / teal) | white set | Ceiling_1/2 grid, Light panels, CeilingVent |
| office lobby | polished stone (Stone_6 / quartz) | white set, dark wood Bottom | Ceiling_3, Lights |
| shop sales floor | Stone_6 / light tiles | white or pink set | Ceiling_3, Lights in rows over aisles |
| stockroom | stone / plain concrete | Stone_1, Metal_2 | none (exposed slab) |

Facade materials stay as they are (palettes); interiors no longer copy the
facade.

## 3. Doors

- Apartment unit entrance: Door_Office_1 (wood) or a coloured door
  (Blue / Green / Orange _3 with window) per building.
- Inside a unit: Door_Office_1, bathrooms Door_Blue_1 or _Green_1.
- Stair core: Door_Emergency_3 or a coloured _2 (EXIT sign), exit light
  above on the corridor side.
- Office rooms: Door_Office_1; server / storage Door_Metal_3 or
  Door_security_1, keypad screen beside.
- Shop: entrance stays an open double gap or glass; stockroom Door_Metal_3.
- Decay: 25 to 50% of doors missing (open gap), a few left OPEN (meta bit
  4), one barricaded with planks or a WoodCrate stack in the most decayed
  buildings.
- Placement: both halves, vanilla door metadata (prop_catalogue.md).

## 4. Rooms

Apartment unit (5 to 8 deep, both sides of the corridor):
- Entry: door, 1 block hall, coat rack is not available -> skip.
- Living room (focal: sofa facing TV): sofa = 2 to 3 stair blocks (quartz
  or wood) facing the TV with carpet seats, FlatscreenTV (random channel
  variant) on a low stand against the opposite wall, rug (FloorCarpet),
  Stereo or Radio1, a plant (CocaPlant or flower pot), Chesstable in some.
- Kitchen (focal: counter run along a wall): counter = slabs (smooth stone
  or quartz) with trapdoor fronts below, sink = cauldron in the counter,
  fridge = ElectricBoxBin, oven = furnace, WashingMachine at the end;
  dining table (WoodTable / WoodTable2) with 2 to 4 Chairs.
- Bedroom (focal: bed against a wall, head to the wall): vanilla bed
  (coloured by unit), nightstand (wood slab / bookshelf) with a Lantern or
  Radio1, wardrobe (2 high planks with trapdoor doors; WeaponCabinet
  looks like a metal locker, keep it for offices / police), carpet beside
  the bed. Kids room variant: smaller bed, Presents.
- Bathroom (2x2 or 2x3, tiles): toilet = quartz stairs facing out, sink =
  cauldron, bath = 2 cauldrons or slabs with water `[not verified]`,
  mirror = glass pane on the wall.
- Studio flat (small units): bed + kitchen corner + 1 chair + TV.

Apartment ground storey (NOT empty): lobby at the entrance (the blue
BlockMailbox is a street mailbox, so it goes outside by the path), a
notice board (decal.notegeneric) by the stairs, stair
core, 1 or 2 ground units furnished like the others, a laundry room
(WashingMachines in a row) or a bike room (Bicycles) in some.

Office storey (pick ONE program per storey):
- Open plan: desk clusters of 2x2 MetalTables, Monitor on each desk,
  OfficeChair at each, low partitions between clusters (oak fence /
  iron bars / glass pane), plants at the ends.
- Cellular: corridor with small offices (desk + monitor + chair + shelf
  + plant), a manager office with WoodTable2, bookshelf, Wallflag.
- Meeting rooms: long table (2 to 3 MetalTables in a row) with chairs on
  both sides, FlatscreenTV on the end wall, glass partition to corridor.
- Break room: VendingMachine_1/2, Waterfountain, table + chairs, sink
  counter, fridge (ElectricBoxBin), trash can.
- Restrooms: tiled, 2 to 3 toilets (quartz stairs) behind spruce / iron
  partitions, sinks (cauldrons), mirrors.
- Server / storage: ElectricBox, ElectricBoxBin, MetalShelf, CCTV,
  keypad door.
- Vacant / under renovation: bare floor, cardboard, ladder, paint
  (graffiti) - one in a while for variety.
Ground storey of an office = lobby: reception desk (counter of slabs +
Monitor + OfficeChair behind), waiting chairs, plants, elevator doors
(BlockElevator + ElevatorButton beside the stair core), NewsStand, exit
lights, CCTV in a corner.

Shop:
- Sales floor (keep): aisles of MetalShelf with goods, ShopDisplay_1
  freezers along one wall, checkout counter front left with a Monitor as
  register and a Trashcan, NewsStand by the entrance, sign over the door.
- Stockroom (keep, enrich): MetalShelf_Empty and full, CardboardBoxes,
  WaterPallet(Tarp), TireStack (tyre shop variant), crates.
- Upper storey (new, was undefined): owner's flat (a small apartment unit)
  or the shop office (desk, monitor, shelves, safe = MilitaryCrate).

Stair core: landing lights, EXIT light over the door, a window on the
facade side, rubble in collapsed buildings only on landings.

## 5. Story and decay layer (per building, from its decay level)

- Rubble: NO full cubes on walkways any more. Low debris: stone / cobble /
  brick slabs (half blocks), cobwebs in corners, scattered CardboardBoxes,
  TrashBags. Full rubble cubes only under a collapsed ceiling.
- Looted: crates swapped to WoodCrateOpen, shelves to MetalShelf_Empty,
  boxes scattered, doors missing.
- Bodies: SkeletonGround in beds / on floors, SkeletonWall against walls,
  blood decals on walls near them, BodyBags in lobbies of decayed buildings.
- Survivor camp (about 1 in 12 buildings, one room): Lantern, CanFire,
  carpet bedroll, crates, Radio1 or MilitaryRadioSmall, WaterPallet,
  barricaded door (planks / SandbagStack), a note decal, graffiti outside
  ("GET OUT HERE", "WHERE IS YOUR GOD NOW?").
- Nature: vines and leaves near broken windows (exists), grass on floors
  under roof holes, moss on lower courses in lush biomes.
- Graffiti: on ground storey outer walls and in stairwells, never on
  upper interiors.

## 6. Exterior

- Entrance: stone brick or quartz door frame, a slab canopy over the door
  (1 deep, entrance width + 2), 1 or 2 steps up when the floor is above the
  path, a door (glass or office door), EXIT light inside.
- Ground storey: shops get full glass shopfronts (panes from 1 to 2
  above floor) and a logo sign (BlockSign_* matching the shop type: Mineway
  sandwich, MineDonalds burger, Minebay general, Guns R Us / Decimunition
  gun shop) centred above the entrance; offices get a taller lobby glazing.
- Apartments: balconies on every second or third bay of the upper
  storeys (slab floor, iron bar or glass pane railing, the window behind
  turned into a door gap), some collapsed (missing slab).
- Fire escape on a side or back wall of apartments over 3 storeys: iron
  bars landings + ladders.
- Roof: stair hut (3x3 with a door) over the stair core, water tank
  (cauldrons / iron block stack on legs) on taller buildings, CeilingVent
  units, an antenna (iron bars, RadioTower segment), parapet; debris only
  near the collapse.
- Downpipes (iron bars or cobblestone walls) at corners `[not verified]`.

## 7. Verification for every change

1. `tools/floorplan.py` on 2+ buildings per kind: reachability >= 95%,
   required room items present (counts per storey), no prop on doorway
   cells.
2. `-Paudit` screenshots: facade, lobby, a typical upper storey, roof.
3. wallscan, gradescan, multiscan, autotest stay green.
4. Write findings into docs/building_audit.md and the shot descriptions
   into docs/shots_index.md.

## 8. Order of work (step 4d / 4e)

1. Structure + circulation: doors, rubble redesign, stair core extras.
2. Surfaces: floors, wall panels, ceilings, lights.
3. Apartment rooms (living, kitchen, bedroom, bathroom), lobby.
4. Office storey programs + lobby.
5. Shop floor polish + upper storey use.
6. Story / decay layer.
7. Exterior: entrances, shopfronts + signs, balconies, fire escapes, roofs.
Each step: implement, verify (section 7), commit, user look in game when
convenient.
