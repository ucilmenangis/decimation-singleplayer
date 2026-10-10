# Placing Decimation props: how they are drawn, facing, sizes, rules

Read before placing any Decimation prop from code (bases, cities, furniture sets). Written
11 Oktober 2026 after the user's review of the first military bases (28 screenshots,
docs/shots/milbase_v0.42_review/, described in docs/shots_index.md): crates in walls, chairs in
tables, a spotlight in the parapet, a pole through a light tower, a keycard on a flag. Every one
of those came from treating a prop as "one block". It is not: a prop is a model drawn wherever
its renderer puts it, often bigger than its cell and off centre.

## 1. How a prop is drawn (read in deobf/src)

- `BlockProp` (net/decimation/mod/common/block/props) gives every prop a `TileEntityProp`, drawn
  by `PropRenderer`: translate to the cell centre (+0.5, 0, +0.5), turn 180 degrees about x (the
  Techne model is upside down), turn `metadata % 4 * 90` degrees about y, then the prop's
  "extra rotation" (`setExtraRotation`, e.g. chairs 180, spotlight 270), then the model.
- About 75 props have their own renderer instead (`ClientRenderRegistry`: crates, cabinet,
  wall flag, office chair, wood table, wrecks, T-wall, street barrier, ...). Each has its OWN
  metadata to angle table and sometimes its own offsets and scales (the wall flag shifts 0.88
  blocks to its wall and is scaled 1.04; the generator is scaled 1.1).
- Models can turn themselves inside `render()` (ModelBarrier, the jersey barrier, turns 90
  degrees) and parts can carry `offsetX/Y/Z` in blocks (ModelCookingStation -0.5, -0.5).
- `setPropSize` is only the COLLISION box, `setPropRenderSize` only the culling box. Neither
  says where the model is drawn.
- Multiblocks (T-wall, street barrier, APC, shelves, phone box) draw only from the master part.
- The client recreates a prop's tile entity from its block (model, extra rotation); only the
  rotation tool angles are saved in NBT. So the block and its metadata decide the look.

## 2. The numbers: tools/props/propgeom.py

`python3 tools/props/propgeom.py` replays each renderer's GL calls and the model's own boxes
(Java models from deobf/src, .bmodel files from the jar) and gives the drawn box per metadata,
relative to the block corner (0..1 = inside the cell):

- `table` -> docs/references/prop_geometry.tsv (every prop, metadata 2..5, box, cells);
- `show BlockX ...` -> one prop with its renderer, model, extra rotation, collision box;
- `check` -> against docs/prop_footprints.tsv (top down photos at metadata 3): 204 of 221 props
  within 0.2 blocks; the rest are thin parts the photos miss (car mirrors, a flag pole's cloth,
  helicopter rotor), so the computed box is the safer one;
- `resource` -> dev/src/main/resources/assets/deciworldgen/prop_boxes.tsv, read by the
  generator (`worldgen/military/PropBoxes`).

Known adjustment: the wall flag's cloth quad is 6 blocks tall but drawn transparent below about
2.5 blocks; its box is cut to 2.5 [inferred from the user's screenshot user_72].

## 3. Facing

Every prop block sets its metadata from the placer's yaw (`onBlockPlacedBy`: looking south 5,
west 2, north 3, east 4), and the authors tuned the renderers so the model faces the person who
placed it. So by default the FRONT points: **2 east, 3 south, 4 west, 5 north**.

Checked (gallery shots at metadata 3, camera south, docs/shots/gallery_v0.16; user shots):
chair, office chair, monitor (screen), weapon cabinet (doors), spotlight (lens), work light
tower (lamps), small military radio (panel), vending machines, washing machine, street bench.

Exceptions (from the model geometry and the gallery):
- BlockMilitaryRadio (the big double radio): its dial side is the model's +z, which ends up
  90 degrees clockwise of the rule: front 2 south, 3 west, 4 north, 5 east. It is 1.7 long along
  z at metadata 3 / 5, along x at 2 / 4.
- Military wrecks (jeep BlockWreckageMilitary1, APC 2, helicopter 3) use their own table: long
  along z at metadata 2 / 3, along x at 4 / 5 (not 3 / 5).
- Wall mounted props hang on the wall BEHIND their front: keycard screens, the wall flag, the
  junction box BlockElectricBox1 (metadata 3: on the north face of their cell). Their box enters
  that wall on purpose.
- The keycard screen opens a locked door (only Door_Emergency_1_Locked exists) within 3 blocks
  (`KeycardReaderBlock`: x, y, z each -3 .. +2 of the screen).

## 4. Sizes that matter (drawn, metadata 3; long axis turns with the metadata)

| prop | drawn size (x by z, height) | note |
|---|---|---|
| BlockAmmoCrate | 1.6 x 0.9, 0.25 | long along x at 3 / 5 |
| BlockAmmoCrateLarge | 1.9 x 1.1, 1.0 | long along x at 3 / 5 |
| BlockMilitaryCrate, BlockMedicalCrate | 1 x 1, 1.15 | exactly one cell: use where space is tight |
| BlockStorageCrate (footlocker) | 1.05 x 1.05, 0.9 | |
| BlockWoodCrate | 1.4 x 0.85 | |
| BlockCardboardBoxes1 | 1.45 x 1.2 | |
| BlockCarePackage | 2.25 x 1.9, 1.6 | |
| BlockWeaponCabinet | 1 x 1.3, 2.05 | the doors stand 0.3 out of the cell toward the front |
| BlockMetalTable, BlockWoodTable | 2 x 1, 1.0 | centre cell +- half a cell: a run steps 2 |
| BlockStretcher | 2.1 x 1.1, 0.55 | |
| BlockBarrier (jersey) | 2 x 0.55, 1.0 | long along x at 3 |
| BlockBarrierTall (T-wall) | 1 x 0.6, 3.0 | multiblock |
| BlockChesstable | 1.45 x 1.45 | |
| BlockSpotlight | 2.5 x 3.2, 1.75 | does not fit a 2 x 2 tower deck |
| BlockHazardLight (work light) | 1 x 1, 2.45 | needs 2 free cells above |
| BlockMilitaryRadio | 1.05 x 1.7, 0.8 | deeper than a desk the wrong way round |
| BlockWreckageMilitary1 (jeep) | 2.2 x 4.0, 1.7 | |
| BlockWreckageMilitary2 (APC) | 4.5 x 8.9, 3.7 | |
| BlockWreckageMilitary3 (helicopter) | 9.4 x 14, 3.8 | with the rotor |
| BlockHedgehog | 1.7 x 2.0, 1.4 | sinks 1 block into the ground |
| BlockFlagPollUAHD | pole + 1.7 cloth, 8 tall | cloth to the east at 3 |
| BlockMetalShelf (+_Empty) | 2 x 1, 2.05 | long along x at 2 / 4, along z at 3 / 5; multiblock |
| BlockPoliceCrate | 1 x 1, 1.15 | one cell |
| BlockMilitaryRadioSmall | 0.9 x 0.8, 0.6 | the 1 block field radio |
| BlockElectricBoxBin | 1 x 1, 2.0 | tall grey cabinet (tool locker) |

## 5. Rules (each one from a user review case)

1. Never place a prop as a 1 x 1 block: look up its drawn box (prop_boxes.tsv) and keep every
   neighbour (walls, posts, other props) out of it.
2. A row of props steps by their drawn length: 2 for tables and large ammo cases, 3 for
   stretchers; 1 only for military / medical crates and footlockers.
3. Long props along a wall lie with their long side along the wall (a 1.9 case across a 1 wide
   hold goes into both walls).
4. A desk is a table 2 long: start the run 2 cells in from the end wall (it pokes half a cell
   back), put the screen / radio on top in the same cell, the chair in front facing it.
5. Chairs go along the long sides of a table facing it, never at the ends (they sit inside it).
6. Tall props (cabinet 2, work light 2.5, T-wall 3, radio tower) need their cells above free:
   a fence post or net above a work light reads as a pole through it.
7. Big props only where they fit: the spotlight on a tower ROOF, not on the 2 x 2 deck.
8. Small things on a table use the table's top (a prop standing on another prop's drawn top is
   supported, also over the table's half cell overhang).
9. Wall mounted props: one per wall spot (a keycard and a flag on the same cell overlap); the
   keycard beside the door it opens.
10. Keep aisles and the cell in front of every door free.
11. A trapdoor shutter is an OPEN trapdoor against the wall (meta 4 | side); a closed one lies
    flat and reads as a shelf.

## 6. Checks

- In code: `Canvas.validateProps(log)` (worldgen/military) runs after a base is built: every
  prop whose drawn box enters a full block, a door, a fence post, a slab, or another prop is
  removed and logged as `clash ...`; the dev test `milbase` lists them as "refused". Fix the
  design until none are left (v0.42.1: 0 on all three sizes).
- Offline: the `milbase` test writes run/client/devtest/milbase_<size>.tsv (every block) and
  `python3 tools/props/propclash.py FILE` reports WALL / PROP / DOOR / FACE (front into a full
  height wall) / MOUNT (wall prop without a wall) / FLOAT (nothing under a standing prop).
- Pictures: the milbase camera points (tower_roof, toc_door, guard_booth, fighting_position,
  conex_inside, hut_back, ...) for the final look.
