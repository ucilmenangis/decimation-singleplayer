# Furniture sets (data driven interiors, v0.20.0)

Rooms are furnished with SETS: small groups of blocks and props designed
together (a TV wall with sofa, rug and coffee table; a kitchen run with
wall cabinets; a bed with nightstands), placed as one unit against a room
wall. Sets are JSON files the user can edit without touching code, in the
spirit of Lost Cities' data driven parts.

## Where they live

- Built-in sets: `dev/src/main/resources/assets/deciworldgen/sets/*.json`
  (+ `index.txt`, regenerate with `ls *.json > index.txt`).
- On first start they are copied to `config/decimation_worldgen/sets/`.
  From then on a file there with the same name REPLACES the built-in; new
  files are added. Delete the config folder to get the built-ins again.
- `/deciworldgen reload` re-reads them in game.

## Format

```json
{
  "name": "living_tv_sofa",
  "rooms": ["living"],
  "weight": 10,
  "comment": "what it is",
  "layers": [
    ["cdc",
     "rrr",
     "rtr",
     "sss"],
    ["pVo"]
  ],
  "palette": {
    "V": {"pick": ["deci:BlockFlatscreenTV", "deci:BlockFlatscreenTV_News"], "type": "prop", "face": "out"},
    "s": {"block": "minecraft:spruce_stairs", "type": "seat", "face": "in"},
    "r": {"block": "minecraft:carpet", "meta": 14}
  }
}
```

- `layers[0]` stands on the floor, `layers[1]` one block up (on top of a
  cabinet), `layers[2]` just under the ceiling (wall cabinets). Storeys
  have 3 blocks of air.
- In each layer, row 0 touches the wall the set stands against, the next
  rows go into the room; columns run along the wall (left to right seen
  from the room).
- `' '` = nothing; `'.'` = must stay free (walking space in front);
  any other char = a palette letter.
- Palette: `block` (or `pick`: a list, one chosen per placement), `meta`
  for fixed metadata (carpet colour, plank type, slab type), `face` =
  `out` (into the room), `in` (toward the wall), `left`, `right`, and
  `type` for how the facing becomes metadata:
  `prop` (Decimation BlockProp: TVs, chairs, fridges...), `vanilla`
  (chest, furnace), `seat` (stairs as a sofa / toilet: the sitter faces
  `face`), `bed` (+ `"part": "head"` or `"foot"`, `face` = foot to head).
- `rooms` = the slot the set fills: living, kitchen, dining, bed, storage,
  desk, bath, lobby, closet. `weight` = how often it is picked.
- Prop looks and sizes: docs/prop_catalogue.md.

## How rooms use them (Building.apartmentUnit)

Flat: kitchen, then (studio) bed, then living, then dining in the living
part; bed, storage, desk in the bedroom; bath twice in the bathroom; lobby
twice; tiny units become a closet. The placer tries the sets in a seeded
weighted order, every wall of the room and every offset; a set fits when
every cell is free, off the kept walkway (entry door to bedroom door), its
back row stands against a wall, lining or glass partition and not in front
of a door; full height pieces (layer 2 content) never cover a window.
Measured on seed 1 (distinct rooms): kitchen 100%, bath 100%, lobby
100%, closet 100%, bed 84%, living 80%, dining 68%; storage and desk are
optional extras (~40%). `servertest.py ... debugsets` logs every
placement (`sets: ok` / `sets: no ... room WxL`).

## Checking a set

`./gradlew runClient -Pautotest -Psets` (in `dev/`) builds every set alone
in a plaster bay and photographs it front-on: `set_<n>.png`
(`-Ponly=4,7` re-shoots single sets). The preview uses the same facing
tables as the generator. Descriptions of the current shots:
docs/shots_index.md `sets_v0.20`.

## Live editing without restarting

- Sets: edit the JSON in `config/decimation_worldgen/sets/`, then in game
  `/deciworldgen reload` and `/deciworldgen rebuild [radius]` (rebuilds the
  city buildings around you with the current code and sets; console:
  `/deciworldgen rebuild <radius> <x> <z>`).
- Code: start the game with `cd dev && ./gradlew runClient -Photswap`
  (debug port 5005); after a code change `python3 tools/hotswap.py`
  compiles and pushes the changed classes into the running game (verified
  2026-10-07: a changed log text appeared without restart), then
  `/deciworldgen rebuild`. Only method body changes swap; new fields /
  methods / classes need a restart.
