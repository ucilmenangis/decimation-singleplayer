# Roadmap: planned work (9 Oktober 2026)

One list of everything still to build or fix, so nothing gets lost between
sessions. Each item points at the doc that holds its details. Keep it
current: add new requests here as they come in, move finished items to
"Recently done" with their version, and keep the detail in the linked doc.
Priority order inside each section is the one last agreed with the user,
or `[not decided]`.

## Open bugs

1. **Drawing cost of props in open prop-heavy views**: 225 props in plain
   view still about halve the fps (driver / GPU work in glCallList). Fix if
   the user finds places that still drop: bake static props into chunk
   meshes (prop textures into the block atlas, both model formats to
   quads). bug.md "FPS drop in prop-dense areas".
2. **Building base height depends on chunk generation order** (low
   impact; height can differ between two new worlds of one seed near the
   edge of the generated area). bug.md "Building base height depends on
   chunk generation order".
3. **Arrows still pick up empty vehicles** (punching was fixed in v0.8.1).
   bug.md "Military jeep/tank/helicopter destroyed in one hit".

## Worldgen and cities

1. **Giant buildings that are skipped**: casino (276 high), oasis condo
   (top above 250 with its cellars), laboratory (90 deep cellars); fit them
   by trimming cellars or capping height. docs/city_engine.md "Open".
2. Lost Cities parts not used yet: bridges (no gaps in our cities), rail.
   Building rotation only uses the variants the data lists.
3. apocalypsenow structures (.nbt) converter for military and other sites
   outside cities `[not decided]`. docs/references/deceasedcraft_buildings.md.
4. Reserve the real footprint of wide props (bicycles, cars) in placers so
   they do not overlap. docs/prop_footprints.tsv, docs/prop_catalogue.md.

## NPCs and combat (user plan, "later")

1. **Enemy military**: medium to high armor, rifles or heavy weapons,
   spawning in military buildings and areas.
2. **Enemy juggernaut**: full juggernaut armor, machine guns (PKM, M240, any
   in the registry) or a Barrett with very high (.50 BMG) damage.
3. **Stronger bandits**: PKM, SV98, militia guns of the medium to heavy
   class, medium to heavy armor.
4. **NPC bullet impacts** on blocks (today only player shots make them).
5. **More zombie variants.**
6. Civilian NPC; traders that spawn on their own and walk; more clothing
   variety on bandits and soldiers.
   All in new_feature.md ("New hostile NPCs and stronger bandits", "More
   zombie variants...", "3 new mobs", "More clothing variety").

## Items and weapons

1. **60 round STANAG and 60 round 5.45 AK magazines.** new_feature.md.
2. Custom weapon creation: how-to written, a new model needs Techne.
   create_weapons.md, docs/gun_model_spec.md.

## Code and tools

1. **Live dev test mode** `[idea]`: keep the dev game open and start test
   modes over a localhost port, so most reruns need no restart.

## Recently done (details in bug.md / new_feature.md)

- v0.28.0..0.28.4 cheap scope (zoom, see-through glass, sniper overlay
  from 4x); v0.28.5 NPC tracers aimed at the target; v0.28.6 / 0.28.7 prop
  and entity line of sight cache, prop render distance option; v0.28.8
  all obfuscated names behind Deci, ZoneKind, 5 s launch wait removed;
  v0.28.9 dev tests split into modes, one launch for many, results file,
  tools/devtest.py; v0.28.10 LcCity split (1225 -> 662 lines, plan
  classes in their own files) and the procedural street painter out of
  StructureGenerator (861 -> 596, LegacyStreets); seed 1 identical on
  both city paths (worlddiff 0 blocks), devtest checks all PASS.
- 9 Oktober 2026 converted building quality pass (no code change, pack
  rebuilt): tools/lcaudit.py, LED lamp floor fixed, black sandstone as
  asphalt, about 30000 dropped blocks mapped (bug.md, docs/references/
  deceasedcraft_buildings.md "Translation audit"); worlddiff `--top`.
- v0.29.0 highway polish: L links for cities only reachable diagonally,
  hedges on crossing parts, no tunnels in city edge bands, side ramps
  beside bridges (docs/city_engine.md "Highways").
