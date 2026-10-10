# Roadmap: planned work (9 Oktober 2026)

One list of everything still to build or fix, so nothing gets lost between
sessions. Each item points at the doc that holds its details. Keep it
current: add new requests here as they come in, move finished items to
"Recently done" with their version, and keep the detail in the linked doc.
Priority order inside each section is the one last agreed with the user,
or `[not decided]`.

## Open bugs

1. **Drawing cost of props in open prop-heavy views** (measured again
   10 Oktober 2026, docs/performance.md section 5: about 18 % of a city
   view; render distance by size 48 / 32 / 24 removes it, new defaults in
   v0.38.2; batching per type was slower; Angelica draws no far terrain on
   this Mac): 225 props in plain
   view still about halve the fps (driver / GPU work in glCallList). Fix if
   the user finds places that still drop: bake static props into chunk
   meshes (prop textures into the block atlas, both model formats to
   quads). bug.md "FPS drop in prop-dense areas".
2. **Building base height depends on chunk generation order** (low
   impact; height can differ between two new worlds of one seed near the
   edge of the generated area). bug.md "Building base height depends on
   chunk generation order".
3. **Military jeep / tank wrecks break with one punch** (bug.md "Open",
   hardness 0 in Decimation, fix later).
4. **Arrows still pick up empty vehicles** (punching was fixed in v0.8.1).
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
   they do not overlap. Done for the military bases (v0.42.1: PropBoxes +
   Canvas.validateProps, docs/prop_placement.md); city placers (Slices,
   furniture sets, street dressing) still to do with the same table.

## NPCs and combat (user plan, "later")

1. Traders that spawn on their own and walk.
   All in new_feature.md ("New hostile NPCs and stronger bandits", "More
   zombie variants...", "3 new mobs", "More clothing variety").
2. **Gunshot noise** (user, 10 Oktober 2026, "later but not now"): a shot
   from a gun without a suppressor draws attention; zombies walk toward
   where the shot came from, and NPCs react the same way. Suppressed shots
   stay quiet.
3. **Sniper NPC nerf** (user, 10 Oktober 2026, "fix later"): sniper damage
   about 20 % lower, sniper range 70 blocks instead of the current one
   (config sniperRange, now 96 [not verified against the user's "90"]);
   the Barrett NPC tiers (juggernaut_sniper, elite_sniper with the Barrett)
   about 40 % lower and their health cut so one 5.56 magazine kills them.
4. **NPC loot system** (user, 10 Oktober 2026, new feature): what NPCs drop
   when killed, how it works, the rewards per tier; design first (ask).
5. **Smart NPCs** (user, 10 Oktober 2026, "later, idk yet like what").
   Ideas to be collected with the user before any design.

## Zones, factions and world (user list 9 Oktober 2026, "later", no order yet)

Details and starting points: new_feature.md "Zones, factions and world".
1. **Advanced safezone.**
2. **Zone areas claimable by the player.**
3. **Advanced zone claims**: a claimed zone can be lost now and then when
   NPCs claim it.
4. **Police NPC.**
5. **Survivor civilians** carrying a minimal weapon.
6. **Radiated areas.**
7. **Advanced military base.** IN PROGRESS: v0.42.0 US FOB style bases of our own in three
   sizes (docs/military_base.md, skill decimation-military-base); v0.42.1 after the user's
   review (prop placement study docs/prop_placement.md, HESCO block, locked TOC door), waiting
   for the next review.
8. **Advanced bandits.**

## Items and weapons

0b. HK416 and HK416 Tan: v0.39.1 fixes the user's review of v0.39.0 (floating stock
   pieces, "2 pillar" front sight: really the rear sight reading our texture; skill
   casebook case 14); waiting for the user's verdict.
0e. Sights onto the rail (v0.40.1, bug.md); Mk18 IMI Defense TS stock, EOTech 558 and ACOG TA11
   (v0.41.0), reticles in the glass, HK416 front sight folds away (v0.41.1, skill case 19):
   ACCEPTED by the user 10 Oktober 2026 ("really good result"; skill case 20) ("small detail will be i analyze"). Open: the
   Mk18 held fps was 14 vs the M4A4's 22 in one gunperf run and equal in the next (318 parts, 82
   of them rail holes) `[not verified]`.
0d. Mk18 Mod 1 (v0.40.0) and iron sights hidden under a sight (all guns with defaultScope
   parts): waiting for the user's verdict. Possible polish: study.py gaps lists 7 metrics
   outside the rifle range (mostly the M4A4 base and the many rail teeth).
0c. MAC-10 parts seated (v0.39.3, skill casebook case 15): 17 flagged -> 2 flanges
   kept on purpose; aim unchanged. Waiting for the user's look in game.
0. UMP9 (v0.38.0): accepted by the user ("work really well and no
   problem"); more variants of Decimation guns can be made the same way
   (skill lesson 13).
1. **More scope models**: EOTech 558 and ACOG TA11 3.5x DONE (v0.41.0, tools/guns/sights.py,
   skill casebook case 18), waiting for the user's look. More later the same way (PSO-1 for the
   AK family, 6x / 10x sniper scopes) `[not decided]`: ask the user and for photos.

1. Custom weapon creation: how-to written, a new model needs Techne.
   create_weapons.md, docs/gun_model_spec.md.

## UI

1. **Main menu GUI fix** (user, 10 Oktober 2026: "later in future, we fix
   the GUI on main menu, not our priority"). Known so far (11 Oktober 2026):
   with a real account the menu shows an offline banner and "Play offline"
   instead of "Play" on green (bug.md "Main menu shows Play offline").
2. **Human Kills counter** (user, 11 Oktober 2026, later): "Player Kills"
   becomes "Human Kills" (NPC humans and players), in game and at the bottom
   of the main menu, from local data (bug.md "Kill and death counters").

## Code and tools

(nothing open)

## Last: only when nothing else is left (user 9 Oktober 2026)

1. **MAC-10 model polish** (DONE v0.37.2, 10 Oktober 2026, accepted by
   the user: aim, firing style and icon good; details new_feature.md,
   skill casebook) (user: "not good, needs polish, okay for a
   first shot"): the pilot model is too boxy and chunky (first person it
   reads as a grey block seen from behind, bigger than Decimation's Uzi).
   Slimmer receiver, smaller stock plate, more shape detail (shapebox
   corners, rounded edges), better texture; check aiming and the fire /
   rack animations in first person. Study done 10 Oktober 2026
   (docs/gun_style_guide.md, tools/guns/study.py): next step is
   gunmodel.py support for shape boxes (corner offsets, 1x1x1 declared
   sizes, addChild, flat tones, UV step 8, icon from the render,
   Decimation's animation timings with SlideBack), then rebuild the
   MAC-10 to about 90 parts, 8.7 x 1.4 units, sight top at y -4.85 to
   -5.0 (style guide section 13), compared with study.py sheets and
   `gunview`. Workflow: project skill .claude/skills/decimation-gun.
2. **More guns of our own** with the same pipeline (docs/gun_model_spec.md
   section 6). MCP tools noted 9 Oktober 2026: Blockbench MCP headless (in
   use), Blender MCP (Sketchfab / Poly Haven / Rodin, needs Blender),
   ElevenLabs or Freesound MCP for sounds (API keys).

## Recently done (details in bug.md / new_feature.md)

- v0.39.2 NPC machine guns no longer fire at double rate (burst rate
  capped at 600 rpm, config maxBurstRpm), dev test firerate.
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
