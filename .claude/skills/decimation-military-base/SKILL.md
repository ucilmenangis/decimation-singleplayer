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
1. docs/military_base.md (all of it), docs/prop_catalogue.md (what every Decimation block looks
   like), docs/prop_footprints.tsv (how far big props reach: APC 5 x 8, helicopter 8 x 9,
   jersey barrier / metal table 3 long, care package 3 x 3, spotlight 3 x 3).
2. Code: worldgen/military/ (Canvas, MilitaryBase, MilitaryBasePlan), LargeSites.militaryBase,
   Slices.placeAt, devtest/MilBaseTest.
3. The lessons and casebook at the end of this file.

## 1. Research before building (the user is away; do it yourself)
- Web search real references (doctrine pages, army.mil / DVIDS articles, product sheets) for
  the layout, the sizes and the small things that make the place real. Write the facts with
  their sources into the design doc section 1 BEFORE coding.
- Map every real element to a Decimation block (table in section 2 of the doc); the catalogue
  has HESCO (BlockMilitaryBarrier), sandbags, concertina, T-walls (BlockBarrierTall), jersey
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

## 3. Test (live, minutes per round)
- `python3 tools/devtest.py --live milbase sizes=0,1,2 points=none`: builds and lists what did not
  fit ("refused"). Fix the layout until nothing important is refused.
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
