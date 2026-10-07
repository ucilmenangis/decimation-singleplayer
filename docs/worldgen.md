# World generation (deciworldgen)

Our own structure generation, shipped as `deciworldgen-<version>.jar` next to
Decimation. Source: `dev/src/main/java/net/decimation/worldgen/`. Written
2026-10-07 for v0.11.0. Terrain is not ours (RTG or vanilla); we only add
structures on top through one FML `IWorldGenerator` (`StructureGenerator`,
registered in `DecimationWorldGen.init`).

## Layout of the world

Terrain: any world type works; the "Decimation" world type (docs/terrain.md)
gives flat urban ground exactly on city sectors and dead / overgrown land
around them.

| Grid | Size | Decided by | Holds |
|---|---|---|---|
| Sector | 16x16 chunks (256 blocks) | seed + region coords | WILD 40%, CIV 25%, CITY 15%, MIL 20% |
| Cell | 4x4 chunks (64 blocks) | seed + cell coords | one small schematic (non city) or one city block (city) |
| Site | 8x8 chunks (128 blocks) | seed + site coords | at most one large schematic (not in CITY) |

Everything is a pure function of the world seed and coordinates, so the
same world always generates the same way and nothing needs cross-chunk
state, except the floor height of large structures (see Slices).

## What generates where

- **Small schematics** (`config/decimation_worldgen/*.schematic`, max 24x24):
  one per cell with a sector dependent chance (wild 20%, civ 55%, mil 45%),
  pool by filename prefix `civ_` / `city_` / `mil_` (untagged = every
  sector), random rotation, anchored in the first 3 chunks of the cell,
  written in ONE pass inside the safe 2x2 chunk population window. Skipped
  when the cell is covered by a large structure.
- **City sectors** (v0.12.0 "city v2"): a street grid (`paintStreets`,
  `deci:BlockRoad`, 5 wide, along every cell's west and north edge) with a
  2 block smooth stone sidewalk ring around every block and car wrecks
  (`deci:BlockWreckage1..5`, `BlockTruckWreckage1`) on lanes 1 and 3, one
  slot per 9 blocks, ~25% filled, never in intersections, facing along the
  street (metadata 5/3 north-south, 4/2 east-west; model long axis is x). Procedural city blocks
  (`CityDistrict` + `Building` v2): 4 lots of 26x26 per cell; per lot 10%
  empty, else apartment (2..9 floors), office (3..20, towers rare) or shop
  (1..2), flush to its street, entrance facing it, overgrowth style from the
  biome at the cell centre (temperate / lush / cold / dry). Floor plans,
  palettes and decay rules: `docs/building_design.md`. Small `city_`
  schematics are NOT used in city sectors any more.
- **Large schematics** (`config/decimation_worldgen/large/*.schematic`, any
  size up to 120x120): per site chance wild 15%, civ 30%, mil 50%, city 0,
  same prefix pools, random rotation, placed fully inside the site.
- **Zones**: `mil_` structures get a MILITARY zone, `city_` structures and
  every procedural building a POLICE zone (footprint + margin), stored per
  world in `<world>/deciworldgen_zones.json` (`ZoneStore`).

## Slice placement (structures bigger than the safe window)

Population of chunk (cx, cz) may only write inside its window
`[cx*16+8, cx*16+23] x [cz*16+8, cz*16+23]`; these windows tile the world.
`Slices.place` writes, for each `Plan` crossing the window, exactly the
columns inside it. The first slice of a plan samples a 5x5 grid over the
WHOLE footprint wherever chunks already exist (never forcing generation)
plus 9 points of its own window (`soilTop`, which ignores trunks and
leaves), rejects the site on water or a spread above `maxSpread` (buildings
12, schematics 7), else takes the median as floor height, and stores it (or
CANCELLED) in `StructureData` (`data/deciworldgen_structures.dat`). Later
slices reuse it. Foundation down to the ground (max 12) in `Plan.foundation()`:
stone brick for buildings (a plinth), dirt for schematics (reads as ground;
the user saw brick cliffs under a compound on a hillside in 0.12.0).
`Plan.blockAt` returns null for air, null with meta SKIP to leave the world
untouched (schematic bedrock).

Implementations: `Building` (procedural), `SchematicPlan` (large schematic,
inverse rotation, placeholder substitution, prop facing FIXED/RANDOM only).

RULE: slice writers must run for EVERY chunk, never gated on the populating
chunk's own sector. A window crosses into neighbouring chunks and therefore
sometimes into another sector; each writer filters by the sector of the
structure it writes. Gating on the chunk's sector left wall strips missing at
sector borders (fixed in 0.11.1, see bug.md).

A column (x, z) is written by chunk `((x - 8) >> 4, (z - 8) >> 4)` when that
chunk populates, which Minecraft does only once its +x, +z and +x+z
neighbours are loaded. So the outermost ring of generated terrain is never
populated and structures there look cut off until a player comes closer.

## Schematic format and placeholders

MCEdit/WorldEdit `.schematic` (gzip NBT, `Blocks`/`Data`/`AddBlocks`,
Minecraft 1.12 or older). NOT supported: Sponge `.schem` (1.13+) and
`.litematic`; those need converting first. TileEntities are ignored
(Decimation loot is by block position, not chest contents).

Bedrock (7) = SKIP (keep terrain). Placeholders turned into Decimation props
by registry name (`DecimationWorldGen.buildSubstitutions`):

| Placeholder | Becomes |
|---|---|
| sponge 19 | WoodCrate |
| gold block 41 | MilitaryCrate |
| lapis block 22 | AmmoCrate |
| diamond block 57 | MedicalCrate |
| emerald block 133 | PoliceCrate |
| iron block 42 | Wreckage1..5 |
| coal block 173 | BlockRoad |
| wool 35, colour 0..15 | sandbag, cone, vending, streetlight, barrier, bin, bench, dumpster, tire, barrel, cardboard, trashbag, electric box, shelf, can fire, road sign |
| stained clay 159, colour 0..15 | road centre line, yellow line, wood table, chair, office chair, metal table, mailbox, phone booth, news stand, stretcher, trash can, washer, cooking station, weapon cabinet, concertina wire, hedgehog/skeleton |

A community schematic built with plain blocks generates as is; to get
Decimation loot inside it, put the placeholder blocks where crates should be
(or a vanilla chest, which is in Decimation's loot table already).

## Adding community schematics

1. Get `.schematic` files (MCEdit format) of ruins / buildings.
2. Name them with the sector prefix: `mil_...`, `civ_...`, `city_...` (city
   only applies to small ones), or no prefix for anywhere.
3. Up to 24x24: `config/decimation_worldgen/`. Bigger: `config/decimation_worldgen/large/`.
4. Restart the game fully (configs are read once per launch). The log lists
   `loaded large schematic '<name>' (WxHxL)` or why it was skipped.
5. New chunks only; existing terrain is never regenerated.

## Lot grading (city yards, 0.13.0)

A `Plan` that also implements `Graded` (so far only `Building`) owns its
whole city lot (26x26). `Slices.place` then works on the plan bounds widened
to the lot, and for every lot column of the window calls
`grade(world, x, z, baseY)` before writing the plan's own blocks there. If
the first slice of a building is yard only, the floor height is still
decided from the building's 5x5 footprint grid.

`Building.grade` (columns outside the walls only):
- natural height = `soilTop`; columns with water above are left alone;
- `t` = d / (d + e), d = distance from the margin ring, e = distance from the
  lot edge, then smoothstep; target = floor + (natural - floor) * t. So the
  ring around the walls sits at floor level (doors are reachable) and the lot
  edge stays at natural height (meets the sidewalk);
- fill up (dirt under grass, sandstone under sand, stone under gravel, else
  the surface block) or cut down, clearing plants and trees left hanging;
  the column keeps its own surface block and snow cap, so yards match the
  biome. NEVER fill with falling blocks: sand over a cave falls in (bug.md);
- front yard (between the front wall and the street edge): a 3 wide path to
  the door (gravel for apartments, slab for others); for offices and shops
  with a setback of at least `Building.MIN_YARD` (6), asphalt (`deci:BlockRoad`)
  where the ground is within 2 of the floor, and nose-in wrecks in the
  middle of the strip every 4 blocks (45%, only on flat spots), metadata 4/2
  (long axis along x, perpendicular to the north-south street).

`CityDistrict.plan` placement inside the lot: a 2 block side yard where the
length allows, setback 6..9 from the street (75% of lots with at least 9
spare blocks, always leaving 3 behind), else min(spare / 2, 3). Both come
from the building seed or the same single `nextInt`, so the random stream
of the cell is unchanged.

Limit: lots on a steep slope with a narrow yard still end in a step at the
lot edge (worst seen: 17 blocks, natural cliffs and a ravine). The grader
never touches sidewalks, streets or the 3 block gaps between lots.

## Testing without a player

- `python3 tools/servertest.py SEED [keep] [pregen=x,z,r]`: dev dedicated
  server; `pregen` also generates r chunks around x,z (`DevPregen`), needed
  to see a large structure whole. Log lines: `city <kind> ...`,
  `large '<name>' ... at x,y,z`, `placed '<name>' ...`.
- `python3 tools/wallscan.py WORLD LOG`: checks every procedural building's
  4 walls, separates real missing walls (exit code 1) from unpopulated edge
  chunks. Run after any change to city or slice code.
- `python3 tools/gradescan.py WORLD LOG`: graded yards per fully populated
  lot: ring (ground vs floor next to the walls, expect 0..1), steep
  neighbour pairs, lot edge vs outside, yard cars by metadata.
- `tools/worldcheck.py` as a module: `World(path).block(x,y,z)` and
  `.meta(x,y,z)` (facing checks, e.g. every car's metadata vs its street).
- `python3 tools/worldcheck.py WORLD column|box ...` to read blocks; map ids to
  names through `level.dat` `FML.ItemData` (Decimation blocks can have ids
  below 256, so never assume >255 means modded).
- Seed 1: city blocks near spawn, a `mil_compound` at -62,65,434 (rot 180).
- `tools/make_test_schematics.py` regenerates the 11 small test schematics
  (byte identical) and `structures/large/mil_compound.schematic` (48x14x48).
