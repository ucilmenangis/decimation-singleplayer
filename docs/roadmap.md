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

1. Traders that spawn on their own and walk.
   All in new_feature.md ("New hostile NPCs and stronger bandits", "More
   zombie variants...", "3 new mobs", "More clothing variety").

## Zones, factions and world (user list 9 Oktober 2026, "later", no order yet)

Details and starting points: new_feature.md "Zones, factions and world".
1. **Advanced safezone.**
2. **Zone areas claimable by the player.**
3. **Advanced zone claims**: a claimed zone can be lost now and then when
   NPCs claim it.
4. **Police NPC.**
5. **Survivor civilians** carrying a minimal weapon.
6. **Radiated areas.**
7. **Advanced military base.**
8. **Advanced bandits.**

## Items and weapons

1. Custom weapon creation: how-to written, a new model needs Techne.
   create_weapons.md, docs/gun_model_spec.md.

## Code and tools

(nothing open)

## Last: only when nothing else is left (user 9 Oktober 2026)

1. **MAC-10 model polish** (user: "not good, needs polish, okay for a
   first shot"): the pilot model is too boxy and chunky (first person it
   reads as a grey block seen from behind, bigger than Decimation's Uzi).
   Slimmer receiver, smaller stock plate, more shape detail (shapebox
   corners, rounded edges), better texture; check aiming and the fire /
   rack animations in first person. Study done 10 Oktober 2026
   (docs/gun_style_guide.md, tools/guns/study.py): next step is
   gunmodel.py support for shape boxes (corner offsets, 1x1x1 declared
   sizes, addChild, flat tones, UV step 8), then rebuild the MAC-10 to
   about 90 parts, 8.7 x 1.4 units, compared with study.py sheets.
2. **More guns of our own** with the same pipeline (docs/gun_model_spec.md
   section 6). MCP tools noted 9 Oktober 2026: Blockbench MCP headless (in
   use), Blender MCP (Sketchfab / Poly Haven / Rodin, needs Blender),
   ElevenLabs or Freesound MCP for sounds (API keys).

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
- v0.30.0 NPC tiers: stronger bandits (3 tiers, heavier in military
  sectors, more clothing mixes), soldier camo sets, Soviets as the enemy
  military with their own group spawner in military sectors, NPC armor
  now counts against player shots (tier share), client shows the
  server's NPC gun (new_feature.md "Step 1 design: NPC tiers").
- v0.30.1 spawn egg per NPC tier; v0.30.2 / 0.30.3 NPC gunfire x5 on the
  player, hit cooldown kept; NPC shots are traced bullets with spread,
  impact particles on blocks, tracers always shown along the real line;
  Soviets no longer kill each other (bug.md).
- v0.30.4 NPC hit cooldown 0.25 s; RPG-7 bandits and RPG-18 military fire
  real rockets (new_feature.md).
- v0.31.0 juggernaut (Soviet side, juggernaut set, machine guns or an
  armor piercing Barrett, 200 hp, takes 25%, slow; only with military
  groups in military sectors, and its egg).
- v0.32.0 elite military (marine black, night vision, MGs / snipers with
  all attachments, 2 magazines to kill, x2 damage); NPC auto fire in
  bursts with recoil spread, real magazines and 4 s reloads.
- v0.32.1 sniper versions of the juggernaut (Barrett) and elite, half of
  the spawner's juggernauts / elites, own eggs.
- v0.32.2 snipers spot enemies up to 96 blocks (others 32).
- v0.33.0 vanilla mobs removed from the overworld (config
  deciworldgen_mobs.cfg).
- v0.34.0 zombie variants: runner, riot, screamer, night frenzy, eggs.
- v0.35.0 60 round STANAG and AK magazines (guns, loot).
- v0.36.0 MAC-10, the first gun of our own (model, texture, icon,
  animations by script; Blockbench MCP previews; Uzi sounds by reference).
- Live dev test mode (9 Oktober 2026): `tools/devtest.py --live [--swap]`,
  the game stays open (`-Plive`, port 127.0.0.1:25599), a rerun costs only
  its own time (CLAUDE.md "Testing").
