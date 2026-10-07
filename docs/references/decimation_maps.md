# Study: hand-built Decimation maps (8 Oktober 2026)

Purpose: learn how human builders made Decimation buildings, as data for
our generator (docs/worldgen_architecture.md). Read this instead of
re-surveying. Maps are the user's downloads (`~/Downloads/*.zip`,
`Decicraft Greenfield Project.rar`); copies only in the session
scratchpad, never committed.

## Maps

| map | origin | mods in level.dat | size |
|---|---|---|---|
| USA coast (`VillageSafeZone`) | epike, 2024/2025 | deci, gvc, customnpcs, gasstation, ... | ~47 buildings |
| world-e161 (`world`) | 2023/2024 | deci, gvc, worldedit, dsurround | prop heavy, mostly loot |
| Cloverfield | 2022 | deci, gvc | small town |
| Decicraft (`decicraft map solo`) | "Greenfield Project" | deci, gvc, customnpcs, FE | 68496 chunks, a whole city |

## Tools (all in `tools/`, need numpy + pillow; venv in the scratchpad)

- `mapsurvey.py WORLD OUT`: map.png, blocks.tsv, props.tsv, hotspots.tsv.
- `mapbuildings.py WORLD OUT [x0 z0 x1 z1]`: buildings.tsv (bbox, storeys,
  main blocks, props, category guess), cams.tsv, b<id>.png storey slices.
  Camera spots need a ceiling within 6 blocks (no roofs, since 8 Okt).
  Use the box on big maps (Decicraft: `-200 -200 400 400`, spawn town).
- Shots: copy the world (for Decicraft only the 4 regions around 0,0)
  to `dev/run/client/saves/<save>`, then
  `./gradlew runClient -Pautotest "-Pstudy=<save>|<cams.tsv>"`; the study
  mode closes any GUI and sets gamma 8 (full bright, unlit rooms) with
  particles off while shooting (night vision was tried: its swirls sat
  in front of the lens; the saved shots predate this fix). `tools/contactsheet.py study_ OUT_DIR`
  packs them into labelled sheets.
- Shots: `docs/shots/study_usa_coast/` (29), `docs/shots/study_decicraft/`
  (57), described in docs/shots_index.md.

## What the builders do (seen in shots + block counts)

Materials (USA coast block counts, top Decimation blocks): BlockStone_5
57k, BlockBrick_2 12k, BlockBrick_3 7.8k, BlockStone_7 7.2k, BlockStone_6,
BlockBrick_1, BlockFloorCarpet_4 2.1k, BlockCeiling_4 2.0k, BlockCeiling_1
1.8k, BlockLight 1.1k, BlockCeiling_3 1.1k, BlockWallOffice_4_Top 930.
Decicraft: BlockFloorTiles_3 9k, BlockCeiling_1 7k, BlockLightOff 1k.

1. **Ceilings are always a separate material.** Decimation ceiling tiles
   (BlockCeiling_1/3/4) with BlockLight panels in a regular grid (every
   3 to 4 blocks) and BlockCeilingVent strips. Decicraft uses redstone
   lamps the same way. Our v0.19 ceiling layer matches; our light spacing
   should be a grid, not scattered `[not verified]` how ours compares.
2. **Two tone walls.** Office and public interiors use BlockWallOffice
   panels: a `_Bottom` variant (coloured dado stripe, e.g. purple) below
   and `_Top` above. Brick or stone brick outside, white panels inside.
   We line with one plaster block; a dado row would read as finished.
3. **Floors by function.** Grey blue carpet (FloorCarpet_4/5) in offices
   and halls, light tiles (FloorTiles_3) in public / medical, planks in
   shops and homes, red carpet (wool) in a hotel corridor.
4. **Rooms are big and open; furniture lines walls or forms rows.**
   Waiting room: rows of chairs facing a long reception counter. Office:
   2 high stone partitions as cubicles, desk + computer per cubicle; desk
   islands of 2x2 with chairs both sides (Decicraft too). School / hall:
   tables in a long row, planters with roses along a wall. Store: metal
   shelf runs along walls (Decicraft supermarket: double shelf aisles).
   Military: hall with oak fence queue barriers, weapon cabinets,
   sandbags, crates, a vehicle (helicopter on a roof, truck wreck).
5. **Decay is done with ground blocks, not holes.** Patches of dirt /
   podzol and leaves on the floor, vines, cracked glass panes, standing
   water on a floor, blood decals. Walls stay whole. (Our decay removes
   wall blocks; the reference keeps the shell intact and dirties floors.)
6. **Corridors**: 2 to 3 wide, lit, doors every 4 to 6 blocks
   (Decicraft hotel / cell blocks: Door_security_1 rows, 78 to 142 per
   building).
7. **Prop density is moderate**: USA coast office buildings 300 to 1900
   Decimation blocks per building incl. structure; furniture is clustered
   by function, never sprinkled.
8. Decicraft is a converted vanilla city: excellent streets (lane lines,
   sidewalks, hedges, plazas, towers, cranes) but most interiors are
   empty corridors. Use it for streets and skyline, not interiors.

## Takeaways for our generator

- Keep the separate ceiling layer; add a light GRID (and vents) by room.
- Add a dado / two tone lining option per building style.
- Floor material by room function (already partly: bedroom spruce).
- Furniture as rows and islands for public rooms (waiting rows, desk
  islands, shelf aisles): a "row set" placer for offices, shops, police.
- Decay layer: dirt / leaves / water / cracked glass on intact shells
  before knocking out walls (fits the Ruins transformer plan).
