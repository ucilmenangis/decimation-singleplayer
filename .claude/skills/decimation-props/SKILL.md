---
name: decimation-props
description: Place Decimation props (crates, tables, chairs, radios, lights, wrecks, barriers, flags, keycards, doors) from code in any generated structure (military bases, city buildings, furniture sets, street scenes) without them clipping walls or each other or facing the wrong way. Use before writing or reviewing any code that puts a deci prop into the world, and when the user reports props in walls, in each other, floating, or turned wrong.
---

# Placing Decimation props right (all structures)

A Decimation prop is a MODEL drawn wherever its renderer puts it: often bigger than its cell,
off centre, turned by its own table. Treating it as one block put crates in walls, chairs in
tables, a searchlight in a parapet and a pole through a light tower (user review of the military
bases, 11 Oktober 2026, 28 screenshots). Everything learned is in docs/prop_placement.md.

## 1. Before placing anything
1. Read docs/prop_placement.md (render chain, facing rule and its exceptions, the size table,
   the 11 rules).
2. Look up each prop you use: `python3 tools/props/propgeom.py show BlockX` (drawn box per
   metadata, renderer, model, extra rotation, collision box). Never guess sizes from the
   catalogue's eyeballed column.
3. Facing: front 2 E, 3 S, 4 W, 5 N (the placer rule) unless the prop is listed as an exception
   (big military radio: 90 degrees clockwise; military wrecks: long along z at 2 / 3; wall
   mounted keycard / wall flag / junction box hang on the wall behind their front). A prop whose
   front matters and is not on the checked list: check it in the gallery shots
   (docs/shots/gallery_v0.16, views.txt; camera south, metadata 3) or the model parts first.

## 2. Writing the placement
- Lay rows by drawn length (tables and large cases step 2, stretchers 3, military / medical
  crates and footlockers 1). Long props along walls with their long side along the wall.
- Every loot container needs a reachable floor cell beside it (walk test LOOT), every door a
  free cell on both sides (DOOR).
- Leave free: aisles, the cell in front of every door, the cells above tall props, the space
  of big props (spotlight 2.5 x 3.2, APC 4.5 x 8.9, helicopter 9.4 x 14).
- Things on tables stand on the table's top (same cell or the table's overhang half cell).
- A keycard opens a locked door (Door_Emergency_1_Locked) within 3 blocks; one wall prop per
  wall spot.
- If the structure is built on a Canvas (worldgen/military), `Canvas.validateProps(log)` removes
  and logs every clash; for other builders, dump the blocks and run tools/props/propclash.py.

## 3. Checking (before showing the user)
- Clash log empty (`milbase` "refused" lines, or propclash.py "findings 0").
- Pictures of every room / module with props (camera points), read them for facing: screens,
  chairs, cabinet doors, lenses toward where a person would use them.
- Save the shots and describe them in docs/shots_index.md.

## Lessons
1. Compute, do not eyeball: the drawn box comes from the renderer code
   (tools/props/propgeom.py), checked against top down photos (204 / 221 within 0.2 blocks).
2. Each custom renderer has its own metadata table: never assume metadata 3 and 5 are the same
   axis (military wrecks are not).
3. A model can turn or shift itself inside render() (jersey barrier 90 degrees, cooking station
   -0.5 offsets): the tool replays those too; a new prop with odd results: read its model.
4. The placer rule (front toward whoever placed it) holds for most props, but verify every
   prop whose front matters before relying on it (the big radio breaks it).
5. A plain looking prop can be very costly: every BlockProp is a tile entity with its own model
   render. For anything placed by the hundreds (walls, HESCO), use a plain block
   (deciworldgen:hesco replaced BlockMilitaryBarrier: tile entities per base 2007 -> 381).

## Casebook
### Case 1: military base props (v0.42.0 review -> v0.42.1, 11 Oktober 2026)
- User shots: docs/shots/milbase_v0.42_review/user_72..99 (shots_index.md).
- Found by the clash checker on the FOB: arms room crates and table piled (1.6 to 1.9 long
  cases 1 cell apart), fighting position case in the sandbags, TOC desks and radios in walls
  (radio 1.7 deep), DFAC chairs inside the table ends, stoves under the serving tables, tower
  spotlight in the parapet, light tower on a net post, half the vehicles sideways (meta 5 is
  along x for wrecks), conex crates in the walls, wall flag and keycard on one spot, closed
  trapdoor shutters reading as shelves.
- Fix: PropBoxes + Canvas.validateProps in the generator, each module re-laid from the drawn
  sizes (docs/military_base.md section 3); 0 clashes, 0 checker findings.
