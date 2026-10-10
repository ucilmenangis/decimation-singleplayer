---
name: decimation-military-base
description: Build, extend or review the procedural military bases (US FOB style, three sizes) of our worldgen for Decimation, or any similar procedural compound (police station, refugee camp, Soviet base). Use when the user asks for military places, bases, outposts, checkpoints or compound like structures with props and loot.
---

# Decimation military base (and compounds like it), end to end

Learned building the US FOB generator (10 Oktober 2026, v0.42.0). The facts live in
docs/military_base.md (reference, layout, code map, status, how to test); this skill is the order
of work, the checks and the lessons. User standard for this work: "really really good output ...
check small detail ... be detailed person and architecture person ... work on loots especially
props placement".

## 0. Read first
1. docs/military_base.md (all of it), docs/prop_placement.md and the skill decimation-props
   (how props are drawn, facing, drawn sizes: a jersey barrier / table 2 long, large ammo case
   1.9, care package 2.25 x 1.9, spotlight 2.5 x 3.2, APC 4.5 x 8.9), docs/prop_catalogue.md
   (what every Decimation block looks like).
2. Code: worldgen/military/ (Canvas, MilitaryBase, MilitaryBasePlan), LargeSites.militaryBase,
   Slices.placeAt, devtest/MilBaseTest.
3. The lessons and casebook at the end of this file.

## 1. Research before building (the user is away; do it yourself)
- Web search real references (doctrine pages, army.mil / DVIDS articles, product sheets) for
  the layout, the sizes and the small things that make the place real. Write the facts with
  their sources into the design doc section 1 BEFORE coding.
- Map every real element to a Decimation block (table in section 2 of the doc); the catalogue
  has HESCO (our block deciworldgen:hesco; Decimation's BlockMilitaryBarrier is a costly prop),
  sandbags, concertina, T-walls (BlockBarrierTall), jersey
  barriers, hedgehogs, crates, cabinets, radios, generators, light towers, flags, wrecks.

## 2. Build
- Canvas in plan coordinates (gate south), Decimation props by facing (front east 2, south 3,
  west 4, north 5), doors through Canvas.DOOR (vanilla door metadata), vanilla blocks through
  Canvas.VANILLA (Rotation turns them). MilitaryBasePlan turns the whole base.
- Fixed first (wall, gate, towers, roads, TOC, the living area rows), then the greedy placer
  `fit(name, zone, scan direction, module)` for the rest; every module calls `reserve()` for its
  rectangle (a 1 block margin may touch roads, never another module) and returns false when it
  does not fit.
- Record camera points (`poi`) inside every module worth a look; the dev test photographs them.
- Loot = Decimation's own crate blocks placed with intent (arms room, ASP, towers, fighting
  positions, conexes, aid station, footlockers); its loot registry fills them.
- Every prop from its DRAWN box (PropBoxes, docs/prop_placement.md): rows step by drawn length,
  long props along walls, aisles and door fronts free, tall props with free cells above.
  `c.validateProps(log)` at the end of build() removes and logs clashes; keep it at 0.
- Walls placed by the hundreds are plain blocks (deciworldgen:hesco), never props.

## 3. Test (live, minutes per round)
- `python3 tools/devtest.py --live milbase sizes=0,1,2 points=none`: builds and lists what did not
  fit ("refused"). Fix the layout until nothing important is refused.
- The same run writes run/client/devtest/milbase_<size>.tsv; `python3 tools/props/propclash.py
  dev/run/client/devtest/milbase_1.tsv` must report findings 0 (walls, props, doors, facing,
  wall mounts, floating).
- `python3 tools/devtest.py --live milbase sizes=...`: all shots; read the sheet and the single
  shots (aerials, top, gate, every inside). New classes / methods need `--stop` first.
- Real worldgen: `python3 tools/servertest.py 1 type=decimation pregen=-64,448,9` and a top down
  block map from the region files (tools/worldcheck.World) to see it on real terrain.

## 4. Review checklist (small details; the user looks at these)
- Nothing overlaps (refused list), nothing floats, no prop inside a wall, every door has a step
  and opens onto free ground, every ladder has a hatch, bunk beds have their upper bunk.
- Every area tells what it is from 10 blocks away (TOC antenna + flag, motor pool vehicles +
  net, ASP berm + crates, aid tent stretchers, helipad H + lights).
- Density: no big empty dirt patches; fill with conex yards, hangars, bunkers, generator pads.
- Colours in Decimation's pack: dirt and coarse dirt are orange: ground mostly gravel.
- Decay of an overrun base is light (a breach, slumped cells, trash), never on the roads.
- Shots saved to docs/shots/<topic>_v<version>/ and described in docs/shots_index.md.

## Lessons (read before starting)
0. A test site must stay out of the user's play area: the sky test now builds at x -20000,
   z -20000, y 230; the earlier sites (3000 / 110 and -4000 / 200) cut into cities and were
   cleared from the dev world with `tools/perfcheck.py clearblocks` (game closed, region backup).
1. Research real references first and write them down with sources; the layout follows real
   doctrine (perimeter rings, ECP, TOC in the interior, living area, logistics, services).
2. Modules overlap unless something stops them: the occupancy grid with a road class and a module
   class, plus a greedy placer that logs what did not fit. Fixed coordinates for three sizes
   failed (first layout: the TOC on the main road, tents in the living area, ASP on the fuel
   point).
3. Company streets: rows of B-huts face each other across a 9 wide street, bunkers between the
   door columns (in front of a door they block it), blast walls between huts.
4. Test in the sky with no ground ramp (grading from y 200 to the real ground grew 100 block
   pillars), clouds off, render distance 16, a long capture window (slow frames skip short ones).
5. Decimation's texture pack: dirt and coarse dirt are orange; exposed dirt grows grass (a berm
   top turns green); hardened clay reads orange brown: tents use smooth sandstone.
6. Regular patterns look artificial: a sandbag course on every third block read as castle
   battlements; use noise for piles and rough edges.
7. Name camera points in the generator itself (poi), so every module can be photographed after
   every change without hand placed cameras.
8. Props are models, not blocks (user review of v0.42.0, 28 shots): compute the drawn box of
   every prop before placing it and validate the whole base (skill decimation-props). The first
   bases had about 40 clashes per FOB that pictures from a distance did not show.
9. Count tile entities: a prop used as a building material (BlockMilitaryBarrier as HESCO) makes
   thousands of model renders. Use a plain block of our own.
10. Locked buildings: the TOC uses Door_Emergency_1_Locked + a military keycard screen; the key
    drops from military crates, wrecks and care packages, so the room stays reachable.
11. Ground in Decimation's pack: gravel with about 10 % dirt patches; coarse dirt never.

## Casebook (never delete a case)

### Case 1: first FOB generator (v0.42.0, 10 Oktober 2026)
- Request: "military zone with buildings ... our own generator, all three sizes, US FOB style ...
  really really good output", user away for an hour.
- Round 1 (sky test inside a city): base seen, many details right (TOC inside, towers, gate
  lane); problems: orange ground, ASP berm like a planter, TOC roof crenellated, shelter roof an
  orange table, city towers in the aerials.
- Round 2: grading pillars in the sky test, fog, skipped shots: test fixed (no ramp, y 200,
  clouds off, render distance, 30 tick window).
- Round 3: modules overlapping, bunkers blocking doors: occupancy grid + greedy placer +
  refused log; the COP got its own layout; sizes COP 50 x 58.
- Round 4: large FOB too empty in the south-east: hangar and conex yards. Real worldgen on seed
  1: two COPs placed, no errors.
- Evidence: docs/shots/milbase_v0.42/ (shots_index.md).

### Case 2: user review of v0.42.0 (11 Oktober 2026) -> v0.42.1
- User: "fix 1 and 2 now, replace military barrier ... its really hurt fps ... door type is using
  non locked type ... dataset issue about props position ... read fully codebase about props
  placement direction". 28 shots: docs/shots/milbase_v0.42_review/ (shots_index.md).
- Found: HESCO prop per cell (FPS), unlocked TOC door, keycard on the outside wall flag, raw dirt
  ASP berm and orange ground patches, about 40 prop clashes per FOB (arms room, TOC desks,
  DFAC, kitchen, tower spotlight, fighting positions, conexes, vehicles sideways), stair roofs
  reading as a saw tooth, closed trapdoor shutters as shelves, latrine without a step, guard
  booth built into the gate tower.
- Fix: deciworldgen:hesco, Door_Emergency_1_Locked, gravel ground, sandbag ASP, the prop
  placement study (docs/prop_placement.md, tools/props, skill decimation-props), validateProps,
  every module re-laid; 0 clashes on all sizes. Shots: docs/shots/milbase_v0.42.1/.

### Case 3: second review (v0.42.1 -> v0.42.2, 11 Oktober 2026)
- User shots docs/shots/milbase_v0.42.1_review/user_100..106: TOC door side held only an ammo
  case and a cabinet turned toward the door; barracks footlockers should partly be real chests
  (loot); the DFAC counter stoves should be the 1 block radio; the motor pool "boring" (the user
  placed shelves to show what they want); our test structures cut into a city in their world.
- Fix: door wall loot (cabinets facing the room, police / military crates, the large case with a
  small one on top), footlocker() half vanilla chests, radioSmall on the counter, parts yard +
  workshop in the motor pool (shelves drawn 2 wide step 3), test site moved and old sites cleared.
- Test traps found: rebuilding a base in place drops its multiblocks every second run; the first
  build into brand new sky chunks can leave a tower knocked down (rebuild was clean) [inferred:
  generation of the chunks being written].
