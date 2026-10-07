# Study: how mature structure generators are organised (7 Oktober 2026)

Read once, from source (shallow clones in the session scratchpad, read
only). Purpose: design our worldgen framework (docs/worldgen_architecture.md)
on proven ideas instead of from scratch.

## The Lost Cities (McJty, MIT, github.com/McJtyMods/LostCities)

Checked: commit f18a094 (2026-10-04), 231 Java files, oldest version 1.10.2.
Everything a city is made of is a data asset (JSON under
`data/lostcities/lostcities/`):
- **parts** (207 files): one storey of a building as hand-drawn char grids,
  `xsize 16 / zsize 16`, `slices` = list of 16 rows per y level (6 slices =
  floor + walls up to the next floor). Example building1_1: slice 0 =
  solid floor with a ladder char `l` and a spawner `1`; slice 2 = outer
  wall of `a`/`@`, inner walls `#`, torches `T`, ladders `L`, chest `*`.
- **buildings** (37): a list of parts with conditions: `{"top": false,
  "part": "building1_3"}`; conditions (`ConditionContext`) can test `top`,
  `ground`, `cellar`, `floor` (exact), `range` (floor range), `inpart`,
  `inbuilding`, `inbiome`, `isbuilding`, `chunkx/z`. Per storey one part is
  picked at random among those whose condition passes. Also min / max
  floors and cellars, filler and rubble chars, `prefersLonely`, a local
  or referenced palette.
- **multibuildings**: 2x2 chunk buildings = a grid of building names
  (center00, center01, center10, center11).
- **palettes**: char -> block, or char -> weighted random list (`"blocks":
  [{"random": 32, "block": ...}]`), plus special entries (spawner with a
  mob condition, loot chest, torch). **styles**: lists of palettes picked
  at random, so one part looks different per city. **variants**: weighted
  block lists reused by palettes (e.g. cracked / mossy mixes).
- **citystyles**: weighted selectors for buildings, multibuildings,
  fronts, stairs, parks, fountains, bridges, rail dungeons; street block
  chars; inherit from another city style.
- **conditions**: weighted value lists with the same tests (loot table per
  floor range or part, mob types).
- Damage: explosions and damage areas applied after building (`DamageArea`,
  rubble char).
Takeaway: quality comes from HAND AUTHORED storey parts; variety comes
from combinatorics (part per storey under conditions x palette per city x
damage). Code is a generic engine; content is data. Fixed 16x16 grid.

## Recurrent Complex (Ivorforce, branches 1.7 .. 1.12, master)

Checked: master 8cd8d4b, branch `1.7` exists (git ls-remote).
- Structures are captured IN GAME with a selection tool and saved with
  metadata; generation is data (JSON + NBT), editable in a GUI.
- **Transformers** run while a structure is placed: `TransformerRuins`
  (decay as a smooth field: decay direction, min / max decay, chaos,
  density from blurred value fields, material stability, gravity so loose
  blocks fall, erosion, vine and cobweb growth), `TransformerNatural`
  (blends the bottom into terrain), `TransformerNegativeSpace` (marks
  cells that keep the world's blocks), `TransformerPillar` (foundations
  down to the ground), `TransformerReplace` (block swaps), `Ensure`, etc.
- **Generation types**: natural (worldgen), list, static, vanilla
  (villages), saplings, and **maze**: rooms are components with exits; a
  maze grid connects them (compose larger layouts from authored rooms).
Takeaways: (1) let the user author content in game and save it as data;
(2) decay as a coherent field with structural support (no floating
blocks), not random per-block holes; (3) composition of authored rooms
by their connections.

## What we take (input for docs/worldgen_architecture.md)

1. Generic engine + data assets (types, parts, palettes, styles, sets,
   conditions), user editable, reloadable in game (we already have
   reload / rebuild / hotswap).
2. Hybrid content: authored parts where quality matters most (storey
   layouts, special rooms) + our procedural planner and sets for variety
   and for footprints no part fits.
3. Conditions on parts / sets / loot (ground, top, floor range, biome,
   sector, building category).
4. Palettes and styles separate from geometry (one layout, many looks).
5. An in-game capture command: build a room or storey in creative, save
   it as a part or a furniture set JSON.
6. Ruins as a transformer: smooth decay field + support check + vines /
   cobwebs, replacing our per-cell random holes.
7. Room composition by connections (maze style) for houses and bunkers.
