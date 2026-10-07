# Furniture sets (data driven interiors, v0.20.0)

Rooms are furnished with SETS: small groups of blocks and props designed
together (a TV wall with sofa, rug and coffee table; a kitchen run with
wall cabinets; a bed with nightstands), placed as one unit against a room
wall. Sets are JSON files the user can edit without touching code, in the
spirit of Lost Cities' data driven parts.

## Where they live

- Built-in sets: `dev/src/main/resources/assets/deciworldgen/sets/*.json`
  (+ `index.txt`, regenerate with `ls *.json > index.txt`).
- They are copied to `config/decimation_worldgen/sets/`. A file there
  with the same name REPLACES the built-in; new files are added.
- Since 0.22.0 (`assets.AssetDir`, same for palettes, styles): at start a
  config copy that equals an OLD built-in version (sha1 listed in the
  built-in folder's `known.txt`) is replaced by the new built-in; a copy
  the user edited is kept (logged "differs from the built-in (edited):
  kept"). Missing built-ins are copied. After changing any built-in
  asset run `python3 tools/asset_hashes.py` (rebuilds known.txt from git
  history + working copy) BEFORE committing, else the old version counts
  as user edited. Verified 0.22.0: dev client and server logged
  "kitchen_run_4.json updated to the new built-in" for the 4 kitchens.
- `/deciworldgen reload` re-reads sets, palettes and styles in game.

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
  `face`), `bed` (+ `"part": "head"` or `"foot"`, `face` = foot to head),
  `trapdoor` (open trapdoor flat against the block on side `face`: cabinet
  doors), `hook` (tripwire hook on the wall on side `face`: a tap).
- Sets are sorted by name (stable preview numbering).
- `rooms` = the slot the set fills: living, kitchen, dining, bed, storage,
  desk, bath, lobby, closet. `weight` = how often it is picked.
- Prop looks and sizes: docs/prop_catalogue.md.
- `"when"` (0.22.0, `assets.Condition`): where the set may be used, every
  given test must pass: `{"kinds": ["apartment", "office", "shop"],
  "storey": "ground" | "upper", "floors": [min, max]}`.
- `"base": "<palette>"` and `"style": "<style>"` (0.22.0): chars missing
  from the inline palette come from the named palette, then from the
  palette the style picks (one pick per placement, so a whole set uses
  one look). The inline palette always wins: put the chars that should
  vary ONLY in the palettes. Example: the kitchen sets have no inline `u`
  (wall cabinets); style `kitchen_wood` picks oak 3 / spruce 3 / birch 2 /
  dark oak 2 (verified: birch planks, used by nothing else, appeared in a
  seed 1 city; set placement unchanged on all 453 rooms seen by both runs).

## Palettes and styles (0.22.0)

Built-ins in `assets/deciworldgen/palettes/` and `styles/` (with
index.txt and known.txt), copied to `config/decimation_worldgen/palettes/`
and `styles/` like sets.
```json
{"name": "kitchen_oak", "palette": {"u": {"block": "minecraft:planks", "meta": 0}}}
{"name": "kitchen_wood", "palettes": [{"palette": "kitchen_oak", "weight": 3},
                                      {"palette": "kitchen_spruce", "weight": 3}]}
```
Palette entries use the set palette format above. Later the part planner
and facades use the same palettes and styles (docs/worldgen_architecture.md).

## Capture (0.22.0): design sets in game

1. Build the furniture in creative against a wall, 3 blocks of air high
   like a storey. Sponge = "keep free" (`.`, walking space).
2. Stand in one corner of the group (on the floor) and type
   `/deciworldgen pos1`, stand in the opposite corner, `/deciworldgen pos2`
   (or give `x y z`). The box spans 3 layers from the lower corner.
3. `/deciworldgen capture set <name> <room> [north|south|west|east]
   [weight]`. Without a side, the back is the box side with the most solid
   blocks right outside it. Writes `config/decimation_worldgen/sets/
   <name>.json` and reloads; `/deciworldgen rebuild` shows it in the city.
4. `/deciworldgen capture part <name>`: raw box (absolute metadata) to
   `config/decimation_worldgen/parts/`, for the part planner (not used by
   the generator yet).

Facing is converted to `face` relative to the wall for stairs (seat), beds,
chests / furnaces (vanilla), open trapdoors, tripwire hooks and Decimation
tile entity props (meta 2..5, not doors); everything else keeps its
metadata. Non master parts of Decimation multiblocks are skipped (the
generator rebuilds them from the master). Verified 0.22.0 on a dev server
with console setblocks: furnace out, stair stool in, BlockChair out, bed
right (foot + head), hook in, open trapdoor in, sponge `.`; a lone chest
placed facing east read "out" because vanilla turns a single chest away
from the wall when placed `[inferred]`.

## How rooms use them (building/ApartmentPlanner.unit)

Flat: kitchen, then (studio) bed, then living, then dining in the living
part; bed, storage, desk in the bedroom; bath twice in the bathroom; lobby
twice; tiny units become a closet. The placer tries the sets in a seeded
weighted order, every wall of the room and every offset; a set fits when
every cell is free, off the kept walkway (entry door to bedroom door), its
back row stands against a wall, lining or glass partition and not in front
of a door; full height pieces (layer 2 content) never cover a window.
Measured on seed 1 (distinct rooms, v0.21): kitchen 100%, bath 100%,
lobby 100%, closet 100%, bed 92%, living 87%, dining 65%; storage and
desk are optional extras (~38%). `servertest.py ... debugsets` logs every
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
  (debug port 5005, bound to 127.0.0.1 only: on Java 8 a bare port listens
  on every interface and would let anyone on the network run code); after a code change `python3 tools/hotswap.py`
  compiles and pushes the changed classes into the running game (verified
  2026-10-07: a changed log text appeared without restart), then
  `/deciworldgen rebuild`. Only method body changes swap; new fields /
  methods / classes need a restart.
