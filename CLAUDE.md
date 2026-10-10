# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project actually is

This is not a normal source-code repo. There is no source for the mod — it's a
closed-source, abandoned Minecraft Forge 1.7.10 mod ("Decimation", modid `deci`,
by ScottehBoeh/BoehMod, official server `mcdecimation.net` permanently dead). The
"codebase" is a single 238MB compiled, name-obfuscated jar. All work here is
**binary reverse engineering + bytecode patching**, not editing and recompiling
source. Purpose: personal singleplayer fixes for the user's own legally-obtained
copy (via Technic modpack). Not for redistribution.

Files in this directory:
- `Decimation.jar` — original jar, currently untouched, do not modify directly.
- `Decimation.jar.original.bak` — verified-untouched backup, hash-checked after
  every patch. If this hash ever changes, something went wrong.
- `Decimation.jar.patched` — the actual deliverable. Rebuilt from
  `Decimation.jar.original.bak` plus a `zip -u`-style update of specific patched
  `.class` entries. The user manually drags this into their Prism Launcher
  instance's `mods/` folder (renamed to `Decimation.jar`) to test.
- `documentation.md` — user-facing changelog/feature doc (mod overview, loot
  system explanation, balance changes, dated session log). Keep this updated
  after every fix, same style as existing entries (Indonesian/English mixed is
  fine, matches the user's own voice).
- `DECIMATION_MOD_TASK.md` — the original task brief that kicked this off.

## Repository (public)

The project root is a git repo (`main`), public on GitHub as
`decimation-singleplayer`. NEVER commit anything of Decimation: no jars, no
decompiled code (`deobf/src`), no `tools/lib`; `.gitignore` enforces it and
every commit should be checked (`git ls-files` for *.jar other than the
Gradle wrapper, nothing over 5 MB). Commit at the root; the user pushes.

## Knowledge maintenance (mandatory, user rule 2026-10-07)

This session can vanish; the files are the memory. After EVERY analysis or
finding (not only at the end of a task), before moving on:

1. Write it down where the next agent will look:
   - subsystem knowledge (how something works, formats, file paths, code map)
     -> `docs/<topic>.md` (e.g. `docs/gun_model_spec.md`); create one per
     subsystem, link it from the index below;
   - bug found/fixed -> `bug.md`; feature progress -> `new_feature.md`;
   - workflow, tools, traps, rules -> this file;
   - new names for obfuscated code -> `deobf/maps/m_<topic>.tsv` (case safe
     file name) + rerun `deobf/apply_mappings.py deobf/maps`.
2. Update the knowledge graph (`graphify-out/`, scope set by `.graphifyignore`:
   docs + our own code, never the decompiled tree) with
   `python3 tools/graph_update.py prepare`. Code-only changes: run `finish`
   right away (free, no LLM). Changed docs: prepare prints the chunk file(s);
   run ONE extraction agent per chunk with the graphify extraction prompt
   (`~/.claude/skills/graphify/references/extraction-spec.md`) plus the note
   "reuse existing node ids from graphify-out/graph.json; classes added
   since the last update are not in graph.json yet but get AST ids
   `dev_src_main_java_net_decimation_<pkg>_<file>_<class>` at finish, use
   those", then
   `python3 tools/graph_update.py finish`. Batch doc edits so one agent pass
   covers them (a pass over CLAUDE.md costs about 85k tokens). For SMALL
   doc edits (a version line, a DONE marker) skip the agent: write the few
   new nodes / edges to a JSON and run `tools/graph_carry.py EXTRA.json
   DOC...` between prepare and finish (carries over what graph.json has
   from those docs; see its docstring). Agent only for new docs or rewrites.
3. Mark anything not verified in game or in code `[not verified]` /
   `[inferred]` in the doc itself.
4. Visual evidence (user rule 2026-10-07: never re-shoot or re-analyse
   what was already seen): every screenshot / render worth keeping goes to
   `docs/shots/<topic>_v<version>/` (git ignored, local only: shows
   Decimation art) AND gets a written description in
   `docs/shots_index.md` (committed). Later sessions read the index, not
   the images; open an image only for a detail not written down; re-shoot
   only when the code behind it changed. Same for analysis results: write
   the conclusion into the matching doc the moment it is reached.

Before starting work in an area, query the graph first
(`graphify query "<question>"`, or read `graphify-out/GRAPH_REPORT.md`) and
the matching `docs/` file, instead of re-deriving from source.

Knowledge index:
- `docs/roadmap.md`: EVERYTHING planned (open bugs, worldgen, NPCs, items,
  tools) in one list with links; add new requests there, move done items.
- `docs/gun_model_spec.md`: gun `.bmodel` / `.anib` formats, paths, renderer,
  new gun checklist.
- `docs/performance.md`: how to check lag (dev test modes gunperf, census,
  tools/perfcheck.py world / log / fixchunk, JFR on the live game with
  jcmd) and what was found (corrupt chunk entity explosion, infected path
  search every tick, prop tile entities in the ticking list).
- `docs/gun_style_guide.md`: READ BEFORE MODELLING A GUN. Study of all 98
  Decimation guns (tools/guns/study.py renders them from the jar, no
  game; dataset docs/references/decimation_guns.tsv): shape box parts,
  part counts, sizes, octagon / curve / panel recipes, dark flat tones.
- `docs/building_design.md`: researched floor plans (apartment / office /
  shop), palettes, decay, biome overgrowth, street and car facing rules.
- `docs/terrain.md`: the "Decimation" world type (biome map, biomes,
  spawn copy, sealed caves under cities, how to test).
- `docs/worldgen.md`: our structure generation (sectors, cells, sites, city
  blocks, large schematics, slice placement, placeholders, how to add
  community schematics, how to test).
- `docs/building_audit.md`: quality audit of city buildings (round 2
  step 4a), findings with evidence, prop inventory.
- `docs/interior_spec.md`: design spec for interiors and exteriors
  (surfaces, doors, room programs, story / decay layer, exterior, how to
  verify, building categories); round 2 step 4c, approved 2026-10-07.
- `docs/city_engine.md`: the Lost Cities style city from converted
  DeceasedCraft content (tools/lcpack.py, city/LcContent, city/LcCity).
- `docs/worldgen_architecture.md`: worldgen v3 design (engine + data
  assets, shell / storey plan / furnisher / layers, size classes, capture
  tool, migration order); `docs/references/worldgen_study.md`: what Lost
  Cities and Recurrent Complex do (read from source, 2026-10-07).
- `docs/references/decimation_maps.md`: hand-built Decimation maps
  (USA coast, Decicraft, Cloverfield, world-e161) surveyed + photographed
  (tools mapsurvey / mapbuildings / contactsheet, autotest `-Pstudy`).
- `docs/references/deceasedcraft_buildings.md`: DeceasedCraft's 79 Lost
  Cities building types (data in DCTweaks jar, tool `tools/lcstudy.py`),
  room sizes, densities, storey 6 high; translation audit
  `tools/lcaudit.py` (dropped blocks, props by placement).
- `docs/references/apartment.md`: real-world clearances, 1.7.10 furniture
  techniques, the apartment review checklist; critic reports in
  `docs/references/critic_*.md`.
- `docs/furniture_sets.md`: data driven furniture sets (JSON format,
  placement rules, preview mode, live editing: /deciworldgen reload /
  rebuild, tools/hotswap.py).
- `docs/prop_catalogue.md`: what every Decimation block looks like, its
  size and facing (from the prop gallery); doors use vanilla door meta.
- `docs/shots_index.md`: what every saved screenshot in `docs/shots/`
  shows (read this instead of opening images).
- `deobf/notes/architecture.md`: package by package architecture of the mod,
  singleplayer gaps, dead backend calls, suspected bugs.
- `deobf/names.tsv`: every class, obfuscated -> readable name + purpose.
- `bug.md`, `new_feature.md`, `documentation.md`, `create_weapons.md`.

## Critical gotcha: case-sensitive extraction required

The jar's obfuscator produced packages that differ only by case
(`deci/ad` vs `deci/aD`, `deci/a` vs `deci/A`, etc — **270+ colliding paths**).
macOS's default APFS volume is case-insensitive, so a naive
`unzip Decimation.jar -d somedir` silently clobbers half of these classes with
whichever one the zip driver happens to extract last, corrupting the extraction
without any error. **Always extract onto a case-sensitive volume**:

```bash
hdiutil create -size 2g -fs "Case-sensitive APFS" -volname DecimationCS /path/to/scratch/decimation_cs.dmg
hdiutil attach /path/to/scratch/decimation_cs.dmg
unzip -q -o Decimation.jar -d /Volumes/DecimationCS
```

Verify with `unzip -l Decimation.jar | wc -l` vs a `find`/`os.walk` count on the
extracted volume — they must match (jar has 21587 total entries).

The `rtk` shell hook in this environment rewrites bare `grep`/`find` invocations
and silently drops flags it doesn't recognize (e.g. `grep -a`, `find -name`),
which produces false "no matches" results with no error. Do bytecode/text
searches with a small Python script (`os.walk` + regex over raw bytes) instead
of relying on `grep`/`find` directly.

## Toolchain (nothing is pre-installed here — set up each session)

- **CFR** (decompiler) — single jar, no deps:
  `curl -sL -o cfr.jar https://github.com/leibnitz27/cfr/releases/download/0.152/cfr-0.152.jar`
  `java -jar cfr.jar path/to/Some.class --outputdir out/`
  Decompile targeted individual `.class` files, not the whole jar (21587 entries,
  most are bundled libraries like okhttp/kryonet/trove/fastutil — irrelevant).
- **Javassist** (bytecode patcher) — single jar:
  `curl -sL -o javassist.jar https://repo1.maven.org/maven2/org/javassist/javassist/3.29.2-GA/javassist-3.29.2-GA.jar`
  There is no real Forge/Minecraft 1.7.10 jar available locally, so
  `ClassPool.appendClassPath` only sees the mod's own extracted classes. Any
  Javassist source-snippet compile (`insertAfter`, `CtNewMethod.make`, method-call
  `$proceed` replacement, etc.) that references a Forge/Minecraft type needs a
  **hand-written stub class** with matching package/name/method descriptors
  compiled and added to a separate stub classpath dir first (get the exact real
  descriptor via `javap -c -p` on a class that already calls the real thing, then
  match it byte-for-byte — the actual game will link against the real classes at
  runtime, the stub only exists to satisfy Javassist's compile-time type check).
  Stubs accumulated so far: `MinecraftForge`, `EventBus`, `FMLCommonHandler`,
  `FMLPreInitializationEvent`, `EntityPlayer`, `TickEvent`(+`ClientTickEvent`/
  `Phase`), `Item`.
- Low-level field/method rewrites (`ExprEditor.edit(FieldAccess/MethodCall)`,
  `CtMethod.setBody`) are preferred over full source reconstruction — many
  methods in this jar use Java 8 lambdas, which Javassist's mini-compiler cannot
  parse, so `CtMethod.setBody(String)`/`CtNewMethod.make` only work on
  lambda-free methods. For anything with lambdas nearby, target the specific
  expression via `ExprEditor` instead of rewriting the whole method.
- **Real compile pipeline (preferred for anything NEW)** — `tools/` now holds a
  working javac setup, so new functionality should be written as ordinary Java
  classes instead of Javassist source snippets. Javassist stays only for
  *editing existing obfuscated methods*.
  ```bash
  python3 tools/setup_toolchain.py   # once; rebuilds tools/lib/ from Prism
  python3 tools/build.py             # compile src/ -> inject into Decimation.jar.patched
  ```
  How it works: the notch→SRG mapping is `deobfuscation_data-1.7.10.lzma`
  inside Forge's universal jar (LZMA-alone, python `lzma` reads it directly);
  SpecialSource remaps both the vanilla client jar *and* the Forge universal jar
  with it. Remapping Forge is not optional — the shipped universal jar
  references Minecraft by **notch** names (runtime is notch; FML remaps mods
  SRG→notch at load), so it cannot be a compile classpath until remapped.
  ForgeAutoRenamingTool (present in Prism's libraries) fails at this: its
  bundled ASM rejects Java 21 library classes, and it aborts on Minecraft
  classes absent from the client jar. SpecialSource tolerates both.
  Code must be written against **SRG names** (`func_147465_d`, not `setBlock`) —
  there is no reobfuscation step. Forge-*added* members keep readable names.
  Members Forge adds to Minecraft classes via binpatches are missing from the
  unpatched vanilla jar; cover those with a compile-only shim in
  `tools/shim_src/` (see its README). `Decimation.jar` is on the compile
  classpath, so new code can call the mod's own obfuscated classes directly.
- Repackaging: never rebuild the whole jar. Update specific entries in place:
  ```bash
  cp Decimation.jar.original.bak Decimation.jar.patched
  (cd patched_out_dir && zip /path/Decimation.jar.patched deci/some/Class.class)
  ```
  Verify after every rebuild: entry count unchanged (`unzip -l | tail -1`),
  `unzip -tq` reports no errors, backup hash unchanged.

## Dev workspace (`dev/`, set up 2026-10-07): live play and debugging

RetroFuturaGradle workspace from the GTNH ExampleMod1.7.10 template. Since
2026-10-07 part of the ONE repo at the project root (public on GitHub as
`decimation-singleplayer`, branch `main`; dev history and version tags were
imported, old `dev/.git` kept only as a scratch backup). Minecraft/Forge decompiled with **MCP names** (`setBlock`, not
`func_147465_d`). `dev/src/main/java` holds our code (worldgen + fixes) converted
SRG→MCP from the old root `src/` via `mcp_stable/12` CSVs; the root `src/` +
`tools/build.py` copy is now stale unless the user decides otherwise.
- JDKs: Azul 25 (runs Gradle 9.7.1) + Azul 8 arm64 (runs the game) in `~/.jdks`,
  registered in `~/.gradle/gradle.properties`. foojay auto-download returns 400.
- `dev/libs/` (git-ignored): `Decimation-base.jar` = `Decimation.jar.patched`
  minus `net/decimation/{worldgen,fixes}` (else our mod loads twice), pulled in
  via `devOnlyNonPublishable(rfg.deobf(...))`; `DecimationVoiceChat.jar` (Gliby's
  Voice Chat, from Prism) deobfed and copied to `run/client/mods/` by the
  `installVoiceChat` task. Needed because Decimation's menu shows "Update"
  instead of "Play" when `mods/DecimationVoiceChat.jar` is absent
  (`net.decimation.mod.common.utils.h.gI()`). Never click that Update button.
- Run: IntelliJ Gradle panel → Tasks → modded minecraft → `runClient` → Debug.
  Plain `runClient` only; `runClient17/21/25` swap in lwjgl3ify, untested with
  Decimation. Schematics for dev live in `run/client/config/decimation_worldgen/`.
- "Your session is invalid!" on the menu is harmless (offline dev user).
- Dev client: every singleplayer world allows commands (cheats) since
  2026-10-08 (`DecimationWorldGen.serverStarting`, deobfuscated runs only;
  the shipped jar is unaffected).
- Confirmed 2026-10-07: dev client boots with deci + deciworldgen + gvc, Play
  works, user created a world and saw structures.
- Shipping: `./gradlew build` -> `build/libs/deciworldgen-<tag>.jar`
  (reobfuscated to SRG, version from the git tag). Deliverables in `dist/`:
  that jar + `Decimation.jar` (= patched jar minus our classes). Both are in
  Prism's `mods/` since 2026-10-07 (original in
  `minecraft/mods_backup_20261007/`); updated 2026-10-10 to 0.38.1 with the
  dev `config/decimation_worldgen` (lc city pack, large schematics), the
  0.7.0 jars and old config in `minecraft/backup_20261010/`. The Prism
  instance also has OptiFine HD U E7 and RTG; the dev client has neither
  (the user tests in the dev client: its fps are without OptiFine). Spotless is disabled to keep our style.
- A Javassist patch to Decimation's own classes must go into THREE jars:
  `Decimation.jar.patched`, `dist/Decimation.jar`, `dev/libs/Decimation-base.jar`
  (then Prism's copy). Patch sources live in `tools/patches/` (so far:
  `PatchSwing.java`, SmoothSwingThread busy loop; `PatchPropCulling.java`,
  props not rendering, both 2026-10-07; `PatchScope.java`, picture in
  picture scope gated behind `decimation.scope.pip`, 2026-10-08;
  `PatchTracer.java`, NPC tracers aimed at the target, 2026-10-09, needs
  netty-all 4.0.10 from Prism's libraries on the Javassist classpath;
  `PatchBackend.java` (2026-10-09) launch no longer waits 5 s for the
  dead Decimation server; `PatchTracer.java` v2 (2026-10-09, aim point in
  the packet, `shotHook` in BanditEntity.shootAt, NPC tracers always
  visible) then `PatchFactions.java` on top of its deci/ag/a output
  (Soviets vs everyone else);
  `PatchInfectedAI.java` (2026-10-10, docs/performance.md): infected
  wander path search server side once a second instead of every tick on
  both sides, horde scan every 10 ticks (deci/ag/d);
  `PatchPropCulling.java` step 3 (2026-10-09) caches line of sight answers,
  step 4 render distance by prop size, config deciworldgen_props.cfg,
  default 64 = vanilla). Patch from the ORIGINAL classes
  only after checking the target class is identical in the patched jar. Earlier
  patches (loot handler, proxy cast, weapon nerf, armor buff, intro skip,
  ammo crate) were one-off and have no saved source.
- Prism: `CustomSkinLoader_Universal-15.0.1.jar` is renamed `.disabled`; it is
  a Forge 1.8+ coremod and crashed every launch. Prism can be driven from the
  shell: `"/Applications/Prism Launcher.app/Contents/MacOS/prismlauncher"
  --launch "Decimation, but better."`, then read
  `<instance>/minecraft/logs/fml-client-latest.log`. A splash "Minecraft
  Crash Report" saying "THIS IS NOT A ERROR" is only a spec printout.

## Testing without the user (no screen capture permission here)

- LIVE FIRST (2026-10-09): `python3 tools/devtest.py --live MODE ...`
  sends the modes to a game that stays open (`gradlew runClient -Plive`,
  devtest/DevTestLive on 127.0.0.1:25599; started in the background on
  the first call, ready after about 26 s, log build/live.log). A run then
  costs only its own time (gun 18 to 20 s, zombies 15 to 17 s, checks +
  npc + shots + tracer + zombies 305 s). `--swap` first pushes changed
  method bodies (tools/hotswap.py; the live game has the debug port);
  new classes / fields / methods need `--stop` and a new start. A
  `key=value` stays set for later live runs (clear it with `key=`); right
  after `--stop` wait a few seconds before the next `--live` run. Runs happen
  in the open world (no fresh world per run); -Pkey=value become system
  properties for the run. `--stop` closes it. Flaky test spawns: always
  retry Decimation-refused spawns (see the arena note).
- ONE LAUNCH (2026-10-09): `python3 tools/devtest.py MODE [MODE ...]
  [-P<flag>]` (venv python for the contact sheets) runs dev test modes in
  ONE game launch (no boot per test) and prints run/client/devtest/
  results.txt: one line per value, PASS / FAIL with the expectation, exit
  code 1 on a FAIL; one contact sheet per mode (dev/run/client/devtest/
  sheet_<mode>.png): read that, not every screenshot. Modes (package
  `worldgen/devtest`, one class each, core `DevAutoTest`): checks (fresh
  seed 1 world: zones, vehicle, humanity, prop box, bottlecaps, armor,
  helmet, supply drop), views (camera; -Paudit / -Pgallery / -Pfootprint /
  -Pstudy / -Pflats / -Psets pick the variant), scope, tracer, props,
  cityfps (reuse the last autotest world). Measured: checks + views +
  tracer 126 s; views + tracer + scope + props + cityfps 332 s in one go.
  Gradle form: `./gradlew runClient -Pdevtest=checks,scope`. Each mode sets
  up its own state (the camera views switch to peaceful, so the tracer
  test sets normal difficulty itself). New test: a DevTestMode subclass,
  register its name in DevAutoTest.mode(), record values with
  DevTestResults.value / check, screenshots with DevTestUtil.screenshot.
  The old flags below still work (-Pautotest = checks + views).
  TEST ARENA (user request 2026-10-09): modes that place NPCs (npc, shots,
  tracer, zombies) use `devtest/DevTestArena`: a flat stone floor at
  y 150 around (8, 8), 97 x 49 blocks, stand at DevTestArena.Y. Each mode
  calls DevTestArena.build(world, player) first (clean floor, no leftover
  entities). Never film on terrain or hold NPCs in the air again (grass
  hid lineups, client copies of held mobs fall). Decimation refuses some
  spawns (10% of infected become hulks / bloaters, 5% of soldiers mechs):
  retry until spawnEntityInWorld returns true.

- `python3 tools/servertest.py SEED [keep]`: dev dedicated server, fresh
  world, waits for spawn generation, stops via console `stop`, prints our
  placement/zone log lines. Seed 1 places POLICE-zoned `city_street`s near
  spawn. Decimation's ServerProxy runs here, so original server handlers fire.
- `python3 tools/worldcheck.py WORLD column X Z [YMIN YMAX]` / `box ...`:
  reads block ids straight from Anvil region files. `World.registry()` maps
  block names to ids from level.dat (Decimation ids can be below 256:
  BlockWreckage1..5 = 176..180, so "id >= 256 means mod block" is wrong).
- `python3 tools/worlddiff.py WORLD_A WORLD_B [--within R]`: block +
  metadata diff of two generated worlds (chunks final in both and within
  R = 10 chunks of spawn; fluids, sand, gravel ignored). The refactor
  check: `servertest.py 1 type=decimation` (NO `keep`: keep reuses the
  old chunks, so nothing is regenerated) before and after, copy
  `dev/run/server/world` aside in between. Same code twice gives 0
  (checked 2026-10-08). Outside the spawn area runs differ even with the
  same code: chunks loaded later vary per run, and a building's base
  height is sampled from whatever chunks exist when its first slice is
  written.
- `python3 tools/worldmap.py WORLD OUT.png`: top down biome / height map
  of a generated world (read the PNG to judge terrain). `servertest.py`
  takes `type=decimation` for the world type.
- `python3 tools/gradescan.py WORLD LOG`: graded city yards (floor vs ground
  at the walls, steep neighbour pairs, lot edge vs sidewalk, parked cars).
- Live loop (user request 2026-10-07: no restart per change): game via
  `./gradlew runClient -Photswap`, code via `python3 tools/hotswap.py`
  (method bodies only), sets via `/deciworldgen reload`, then
  `/deciworldgen rebuild [radius]` regenerates nearby buildings in place.
- Screenshot modes of the autotest (all switch to peaceful and remove
  mobs first, lock the camera each tick, restore fov / gamma at the end):
  default = 3 street views; `-Paudit` = facade / ground / storey 1 / roof
  of a sample apartment, office, shop; `-Pgallery` = every Decimation
  block, 3 per shot; `-Ponly=0,23,70-76` re-shoots single views. Output
  `dev/run/client/screenshots/`; copy keepers to `docs/shots/` and describe
  them in `docs/shots_index.md`.
- `./gradlew runClient -Pautotest` (in `dev/`): unattended singleplayer run
  (`DevAutoTest`, inert without the flag): makes world
  `deciworldgen_autotest` from seed 1, spawns infected inside a generated
  zone and outside, drops 5 bottlecaps on the player and checks they become
  balance, punches a spawned hummer as survival (plain and sneaking), hits
  the player with 10 "human" damage bare and in Decimation armor, kills an
  infected for humanity, forces a supply drop and polls until it lands, logs
  `AUTOTEST` lines, quits (about 90 s). Damage checks must run more than 60
  server ticks after joining: spawn invulnerability blocks all damage before. It forces
  `pauseOnLostFocus = false`; an unfocused window otherwise pauses the
  integrated server and server ticks stop.

## Readable reference tree (`deobf/`, started 2026-10-07)

`deobf/src/` = Decimation decompiled with readable names, for reading only (the
game never loads it). Pipeline: `deobf/decimation-mcp.jar` (Decimation's own
classes from the RFG deobf jar, so Minecraft calls are MCP named) + naming tables
`deobf/maps/*.tsv` (format in `deobf/BRIEF.md`) -> `python3
deobf/apply_mappings.py deobf/maps` -> `decimation.srg`, `names.tsv` (obf,
readable, confidence, purpose), `decimation-named.jar`, `src/`. Named classes go
to `decimation/<subsystem>/<Name>`; unnamed ones to
`decimation/unnamed/<pkg>_<cls>` so the tree is case collision free on APFS.
- Status 2026-10-07: EVERY class named (965 explicit, inner classes follow
  their outer), 2890 fields, 1400 of ~2038 methods (69%). Unnamed fields are
  mostly 3D model parts. Sources of names, in priority order (first name
  wins in `apply_mappings.py`): AI tables `g*.tsv`, `p_*.tsv`, `r_*.tsv`,
  `m_*.tsv`, then `zz_auto.tsv` from `deobf/autoname.py` (heuristics: registry
  names, NBT keys, event types, entity registrations, accessors; runs in 1 s
  over the raw source, regenerate it after AI tables change).
- `deobf/skeleton.py RAWSRC deci/xx`: compact class view (declaration, folded
  field runs, method signatures, strings) for cheap naming passes; by default
  only classes still unnamed.
- CASE TRAP for map files: `p_deci_aK.tsv` and `p_deci_ak.tsv` are the SAME
  file on APFS. A naming pass lost 10 package tables this way (recovered or
  redone). Map file names must carry a case suffix (`_lu`, `_ll`, `_u`, `_l`
  per letter) or otherwise be unique ignoring case.
- Decompilers: CFR for `deobf/src` (typed local names); Vineflower
  (`deobf/vineflower.jar`) is run automatically for the few classes CFR
  cannot structure (3), output in `deobf/src_vineflower/`.
- Raw obfuscated decompile needs a case sensitive volume:
  `hdiutil attach deobf/cs.sparseimage` (mounts `/Volumes/DeciDeobf`), then
  `java -jar deobf/cfr.jar deobf/decimation-mcp.jar --outputdir
  /Volumes/DeciDeobf/src --caseinsensitivefs false`. CFR's default
  `--caseinsensitivefs true` silently drops colliding classes (29 packages).

## Obfuscation map (package → meaning, built up empirically)

Single/double-lowercase-letter packages under `deci.*` are the obfuscated core;
`net.decimation.mod.*` and a few `deci.*` subsystems kept readable names.

- `deci.a` — main mod class (`b`, `@Mod` entry point) + the two proxies:
  `deci.a.c` = **ClientProxy**, `deci.a.e` = **ServerProxy** (dedicated-only).
  `deci.a.b.d()` returns the proxy hard-cast to ServerProxy type — throws on
  client. This proxy split is the root cause of nearly every singleplayer bug
  found so far (see below).
- `deci.aD.g` — block registration (`init()`, hundreds of `GameRegistry.
  registerBlock` calls). `deci.aD.c` — a few more blocks incl. `afA` =
  `BlockSupplyDrop`. `deci.aD.k` — huge (1800+ lines) item registration,
  includes all gun instantiation (`new deci.ay.i(...)`).
- `deci.aD.l` — the loot table. `gh()` builds `Block → ItemPool` map (hardcoded
  Java, not config/YAML). `c(Block)` looks up a pool. Inner classes `l$1`..
  `l$12` are the individual per-block loot pool `ArrayList<ItemStack>` literals
  (numbering does **not** match declaration order in `gh()` — always verify
  which `l$N` a fix target is via bytecode string search, not by guessing).
- `deci.aB.e` — per-position loot cooldown registry, keyed by dimension+coords,
  block-type-agnostic (works for any mod's structures, not just Decimation's own).
- `deci.aK.*` (a–q, 17 classes) — the **ServerProxy-only** event handlers:
  block break/place protection, tick events, chat, entity join, player login,
  and critically `deci.aK.k` = the crate/car loot right-click handler
  (`PlayerInteractEvent`). `deci.aK.q` = `TickEvent.ServerTickEvent` (drains the
  scheduled-task queue `net.decimation.mod.common.utils.b`, needed for the
  delayed loot-GUI open). `deci.aL.a` is an old near-duplicate of these same
  handlers that is **never registered anywhere** — dead code from a refactor,
  don't confuse it with the live `deci.aK.*` versions.
- `deci.an.*` (a–p, 16 classes) — the sibling handler package that IS registered
  unconditionally on both sides (in the main mod class, not a proxy) — safe
  reference point for "this fires in singleplayer" behavior.
- `deci.aE.a` — giant (4700+ lines) holder of every `SimpleNetworkWrapper`
  message + handler pair as nested classes (`R`, `S`, `T`... `aa`, `ab`, single
  letters get reused/nested oddly due to obfuscation — CFR sometimes renders a
  nested class name identical to an unrelated top-level one; always confirm via
  `javap` bytecode, not decompiled source text, when two things could plausibly
  share a display name). Message registration (`registerMessage` + `Side`) lives
  in `deci.aD.n.init()` (called unconditionally, both sides — safe).
- `deci.aF.a$a$a` — the actual `SimpleNetworkWrapper` singleton (`.gB()`).
- `deci.ay.*` — weapons. `deci.ay.i` = base gun `Item` class (huge, 1000+
  lines), `.aew` field = damage-per-hit, `.am(int)` = the single setter every
  registered gun's `.am(N)` builder call goes through — the chokepoint for any
  global weapon-damage change. `deci.ay.c` = weapon category enum (rifle/smg/
  mg/shotgun/pistol/revolver/rocket/**crossbow**/flamethrower[unused]/all).
  `deci.ay.e` = per-weapon stat block (recoil/firerate, NOT damage).
- `deci.ao.c` — shared base `Item` class for both guns (`deci.ay.i`) and ammo
  boxes (`deci.ay.g`) — just NBT owner/uniqueID tagging, no damage/gameplay
  logic.
- `deci.ag.*` — hostile mob **entity** classes (bandit `deci.ag.a`, soldier
  `deci.ag.l`, infected/zombie `deci.ag.d`, etc). NPC ranged attacks call
  vanilla `attackEntityFrom` **directly** server-side
  (`entityLivingBase.func_70097_a(h.alh, i2.aew / ...)`) — this bypasses the
  player-fired-shot network message entirely, which matters for anything that
  only hooks that message (see armor caveat below).
- `deci.ai.*` — NPC AI / non-hostile NPC entities (trader `deci.ai.e`, etc).
- `net.decimation.mod.common.item.armor.ItemArmorDeci` — armor items.
  `damageMultiplier` field is only ever read in **one place in the whole mod**:
  `deci.aE.a$z$a` (the player-fired-shot hit handler), which also skips armor
  slot index 3 (helmet) when applying it. Damage source type strings differ by
  attacker: player-fired shots use `"gunDeci"` (`deci.ab.a`), NPC-fired shots
  use `"human"` (`deci.aD.h.alh`) — two different `DamageSource` identities for
  conceptually the same "shot" event, neither currently unified.
- `deci.i.*` — the boot-time intro screens (`d` = jumpscare frame animation,
  `c` = BoehMod studio logo), gated by static booleans `iH`/`io` that flip
  `true` once each screen finishes playing naturally.

## The recurring root-cause pattern

Every singleplayer-specific bug found so far traces back to the same thing:
**this mod was built assuming "not a dedicated server" means "just a client with
no gameplay authority."** But in singleplayer, the integrated server shares the
same JVM and reports `FMLCommonHandler.getSide() == CLIENT` — so anything gated
on `isServer()` or only registered inside `deci.a.e` (ServerProxy) silently never
runs, even though singleplayer absolutely needs that logic to be authoritative.
When investigating a "works in multiplayer, broken in singleplayer" report,
check event-bus registration site and any `@SideOnly(Side.SERVER)` annotation
first — that's been the answer close to every time.

## Current state / pending decisions

See `documentation.md`'s Session Log for the dated blow-by-blow. Open items as
of the last session:
- Armor vs NPC gunfire: FIXED 2026-10-07 in v0.9.0 (`ArmorGunfireHandler`,
  LivingHurtEvent on the "human" and "turret" sources, same helmet exclusion
  as the PvP handler `deci.aE.a$z$a`, which was left untouched). See bug.md.
  v0.9.1: helmets count on headshots only (aim line for player guns, 20%
  random for NPC shots).
- v0.10.0: procedural city blocks (`CityDistrict`, `Building`), v0.11.0:
  large schematics of any size (`LargeSites`, `SchematicPlan`), both written
  slice by slice (`Slices`, `StructureData`). Details in `docs/worldgen.md`.
  v0.11.1: slice writers run for every chunk (sector border wall bug).
  User reviewed city blocks in game on 0.11.0 ("good for oneshot, needs
  polish") and reported the missing walls. `tools/wallscan.py` checks walls.
  v0.12.0: city v2 (see docs/building_design.md), sidewalks, street-aligned
  car wrecks. Prop facing: PropRenderer turns props by metadata % 4 * 90.
  v0.12.1: whole-footprint floor height sampling, dirt fill under
  schematics (user saw stone brick cliffs under a hillside compound).
  v0.12.2: car wreck model's long axis is x at 0 degrees (seen in game), so
  north-south streets use metadata 5/3, east-west 4/2.
  v0.12.3: multiblock props (`deci.W.a`, metal shelves) render only from a
  master part; anything placing them outside player placement must call
  `setSelfMaster()` (done in Slices / small placer, plus
  `MultiblockRepairHandler` on chunk load).
  v0.13.0: terrain blending. City lots are graded (`Graded`, Building.grade):
  smoothstep ramp from the floor at the walls to natural height at the lot
  edge, column keeps its own surface block; front yards (setback 6..9) get
  asphalt parking where the ground is within 2 of the floor, a path to the
  door, nose-in wrecks (metadata 4/2). Never fill with falling blocks.
  `tools/gradescan.py` checks it. Supply drops clear flowers in their
  column first (crate entities vanish in non-replaceable plants).
  v0.14.0: "Decimation" world type (docs/terrain.md): own biome map on
  vanilla's terrain generator, flat cities on the exact city sectors
  (`Sectors` is now shared), dead land near cities, overgrown far away,
  rivers, no ocean. Autotest now runs on this world type.
  v0.15.0: street life (docs/building_design.md "Street life"): levelled
  street cross-sections, dashed centre lines, street lights, benches, bins,
  trash bags, facing derived from PropRenderer's transform. Autotest ends
  with 3 screenshots of a city street in `dev/run/client/screenshots/
  autotest_<n>.png` (along the street, a street light side-on, across the
  street); READ THEM to check anything visual (facing, levelling) instead
  of asking the user. `-Ddeciworldgen.autotest.views=false` skips them.
  v0.16.0: multiblock props generated whole (shelves are 1x1x2 TALL; see
  docs/worldgen.md "Multiblock props"), `tools/multiscan.py` checks them.
  Supply drops skip columns topped by a prop (crate vanished on a trash bag).
  v0.16.1: every storey reachable (ladder support, collapse spares the
  stairs); building audit, prop gallery, catalogue, spec (step 4a..c).
  v0.17.0: step 4d.1 doors in every DOOR cell and low debris; step 4d.2
  surfaces: room grid beside each storey plan (corridor, living, bedroom,
  lobby, office, meeting, storage, shop, stock), floors per room, wall
  panels per building, ceiling lights / vents (docs/interior_spec.md
  section 8). A floor block is also the ceiling below: keep floors light.
  v0.18.0: step 4d.3 apartment rooms (spec section 8 item 3); propFacing
  fixed: a BlockProp's FRONT points 2 E, 3 S, 4 W, 5 N.
  v0.19.0: storeys 5 high with their own white plaster ceiling layer
  (Building.FLOOR = 5, CEIL = 4), 5 step stair runs; after the user's
  0.18 review ("not ready to be called interior"), see
  docs/interior_spec.md section 8b for the revised order.
  v0.20.0: furniture sets (docs/furniture_sets.md), plaster lining inside
  apartment outer walls (LINING cells, window recesses), corner entry
  doors, one-sided corridor for apartment blocks under 18 deep, closets,
  /deciworldgen reload | rebuild, -Photswap + tools/hotswap.py.
  v0.21.0: critic pass 1 on apartments (docs/references/
  critic_apartment_v0.20.md, verify every critic claim: it was wrong on
  chair facing and lintels), reference + checklist
  (docs/references/apartment.md), kitchen sets with iron fridge, trapdoor
  cabinet doors and tap, wall breaches by column, narrow apartment blocks
  use the ladder, `-Pflats` audit (Building.lookCell camera).
  v0.22.0: worldgen v3 step 1 (architecture approved 2026-10-08): asset
  core `worldgen/assets/` (AssetDir loader shared by sets / palettes /
  styles, named palettes, weighted styles, `"when"` conditions on sets,
  in-game capture `/deciworldgen pos1|pos2|capture set|part`). Built-in
  assets update unedited config copies via `known.txt`: after changing
  ANY built-in asset run `python3 tools/asset_hashes.py` before
  committing. Map studies: docs/references/decimation_maps.md,
  deceasedcraft_buildings.md.
  v0.22.1: step 2, `Building` split into `worldgen/building/` (Shell,
  StoreyPlan, Apartment / Office / ShopPlanner, Furnisher, Facing,
  Surfaces, Interior, Ruins, Yard; code map in
  docs/worldgen_architecture.md); storey height per building (5); 0
  blocks differ from 0.22.0 in the seed 1 spawn area (fresh worlds).
  v0.23.0: offices and shops 6 high storeys (user decision), stair core
  H + 2 long; building log lines carry "storey H" (floorplan, wallscan,
  gradescan read it; wallscan now scans the full wall height). Seed 1
  spawn area vs 0.22.1: changes only inside office / shop footprints;
  0 real missing walls, upper storeys 92 to 99% reachable.
  v0.24.0: city engine (docs/city_engine.md): converted Lost Cities
  buildings (DeceasedCraft current + legacy, tools/lcpack.py into
  config/decimation_worldgen/lc, local only) replace the procedural city
  when installed; chunk streets, city levels 6 apart, stairs parts.
  v0.24.1: street dressing, city edge ramp; v0.24.2: superblocks (towers),
  translator fixes (windows, stone bricks, basalt); v0.24.3: wasteland
  district next to military sectors. dist/ holds the 0.24.3 jar and the
  pack (dist/config/decimation_worldgen/lc, local only).
  v0.24.4: city edge ramp up to 24 wide, rounded corners, no site inside
  it; `tools/edgescan.py WORLD` checks it. Study mode needs BOTH
  `-Pautotest -Pstudy=...` (study alone sits at the menu).
  v0.25.0: highways between cities (docs/city_engine.md "Highways",
  `city/Highways.java`, `tools/hwmap.py SEED R` prints the network).
  v0.26.0: parks on open lots (10%), street scenes, building fronts
  (docs/city_engine.md "Parks, street scenes, fronts").
  v0.27.0: DC districts use their own Lost Cities street parts (road
  paint converted to painted road blocks); legacy districts keep ours.
  v0.28.0: cheap scope (bug.md "FPS drop while aiming through scopes"):
  view zoom + frame copy on the glass, old picture in picture scope kept
  behind config `pictureInPicture`; `-Pscope` autotest measures fps.
  v0.28.1: world and gun zoom together (EntityRenderer.cameraZoom; the
  hand is drawn by ScopeZoom because vanilla skips it while zoomed).
  v0.28.2: projective see-through glass, sight centred on the screen
  centre (learned per gun + scope), mouse slowdown; `-Pscope` also shoots
  every sight in two window sizes. Javassist snippets: compile against
  Java 8 signatures (cast to java.nio.Buffer before flip()).
  v0.28.3: glass copy limited to the glass box, buffers once.
  `./gradlew runClient -Pautotest -Pscopeonly` (in dev/): the scope test
  alone in the last autotest world (no new world, no server checks, noon
  forced), about 3.5 min; `-Pscope` = full autotest + scope test.
  v0.28.4: 4x and up use a black sniper overlay with the reticle (no gun);
  reddot / 2x keep the gun with see-through glass (config `overlayFrom`).
  `-Ptracer` (with -Pautotest, last autotest world): NPC tracer direction
  test, logs the mean / max angle between tracers and the target.
  `-Pprops`: prop fps test (platform, 225 props in view / hidden);
  `-Pjfr`: Java Flight Recorder profile to run/client/profile.jfr (read with
  ~/.jdks/zulu-25.jdk/Contents/Home/bin/jfr print --json; client thread is
  `main` in dev). Profile before optimising: guesses were wrong twice.
- Refactor check (2026-10-09, both city paths): `servertest.py 1
  type=decimation` before / after, `worlddiff --within 10` must be 0; the
  old procedural city path runs when `config/decimation_worldgen/lc` is
  absent (move it aside on the server run dir, put it back after). Stash
  with `git stash -u`: new untracked files otherwise stay and break the
  old build.
  `-Pcityfps`: fps looking down a seed 1 city street (cityfps.png).
  v0.29.0: highway polish (L links for diagonal only cities, hedges on
  crossings, no tunnels in city edge bands, side ramps beside bridges);
  docs/city_engine.md "Highways". `servertest.py ... pregen=x,z,r` makes
  a far away spot (an L link from `tools/hwmap.py`) checkable.
  v0.30.0: NPC tiers (`fixes/NpcLoadouts`, `MilitarySpawner`, config
  deciworldgen_npc.cfg; new_feature.md "Step 1 design: NPC tiers"):
  Decimation rolls NPC guns per side and never syncs them, so our gun goes
  to the client via data watcher slot 26. Dev test mode `npc`.
  v0.30.1: spawn egg per tier (`deciworldgen:npc_egg`, `fixes/NpcEgg`).
  v0.30.2: NPC gun hits on players x5 after armor and no vanilla hit
  cooldown for them (bug.md "Full military armor makes NPC gunfire almost
  harmless"); NPC shots are direct damage, not bullets. Decimation turns
  5% of soldier spawns into mechs (PlayerJoinSync).
  v0.30.3: NPC shots traced (`fixes/NpcShots`, spread per tier, walls
  stop them, impact particles), hit cooldown back, Soviets fixed (bug.md
  "Soviets kill each other", "NPC shots were not bullets"); dev test
  modes `shots`, `tracer` (now tracer vs server shot line). Test NPCs
  must stand on a block (the client copy of a mob held in the air falls).
  v0.30.4: NPC hit cooldown 5 ticks (config npcHitCooldownTicks), rocket
  tiers bandit_rpg / military_rpg fire RocketEntity (Deci.fireRocket);
  deciworldgen_npc.cfg has config version "2" (weights reset once). New
  tiers go LAST in NpcLoadouts (egg metadata = index).
  v0.31.0: juggernaut tier (weights 0: MilitarySpawner juggernautChance
  and its egg only), Barrett hits armor piercing (NpcShots.armorPiercing).
  v0.32.0: elite military, bursts, magazines and reloads (new_feature.md
  "v0.32.0"). Test trap: a test calling shootAt while the AI also does
  halves every cooldown. v0.32.1: juggernaut_sniper / elite_sniper tiers
  (sniperShare). v0.32.2: sniperSearch (sniperRange 96).
  v0.33.0: `fixes/VanillaMobs` removes vanilla mobs from the overworld;
  a dev test that needs a vanilla mob (pig target) must set
  getEntityData().setBoolean(VanillaMobs.KEEP, true) before spawning it.
  Runtime class names are obfuscated in the shipped game: never test
  vanilla classes by package name.
  v0.34.0: zombie variants (`fixes/InfectedVariants`, dev test mode
  `zombies`). Decimation resets infected walk speed BASE every tick: use
  attribute modifiers (operation 2 to multiply on top of others).
  v0.35.0: 60 round mags (`fixes/Magazines`); our own item names live in
  dev/src/main/resources/assets/deciworldgen/lang/en_US.lang.
  v0.37.0: MAC-10 v2 in Decimation's style (gunmodel.py v2: shape parts,
  flat tones, icon from the render, Decimation's animation timings; 102
  parts, sights at the aim centre), new_feature.md. v0.37.1: suppressor
  flush on short guns (Deci.offsetAttachment), texture gradation.
  v0.37.2: flamePos y 0.85 above the bore (Decimation convention), aim
  sight picture matched to the Uzi in the same shot; every user revision
  updates the skill's revision log (user rule). v0.37.3: MAC-10 gaps
  closed (`study.py gaps`, declared sizes, more bevels and details).
  v0.38.0: UMP9 = Decimation's UMP45 generated locally from the jar
  (tools/guns/ump9.py, outputs git ignored, run it before building) with
  our curved 9 mm magazine; DevTest gun takes -Pgun=NAME.
  v0.38.1: performance (docs/performance.md): PatchInfectedAI, dev test
  modes gunperf and census, tools/perfcheck.py; the dev world was repaired
  (a corrupt chunk had doubled entities to 40 000). v0.38.2: prop render
  distance defaults 48 / 32 / 24 (large / medium / small; city view props
  cost about 18 % -> 0), dev test cityview; Angelica tried in the dev
  client (-Pangelica): draws no far terrain on this Mac, not usable.
  v0.39.0: HK416 and HK416 Tan from Decimation's M4A4 (tools/guns/hk416.py,
  run it before building, outputs git ignored). v0.39.1: HK416 review
  fixes (seated stock details, U front sight, M4A4 rear sight texture
  wrap; skill case 14); `study.py contact` finds floating parts.
  v0.39.2: NPC burst rate capped (maxBurstRpm 600, bug.md "Some NPC
  machine guns fire at double rate"); dev test `firerate` times every
  NPC shot per tier and gun (-Pcases=tier:gun,...).
  v0.39.3: MAC-10 parts seated (skill case 15).
  Gun study (10 Oktober 2026): docs/gun_style_guide.md, tools/guns/study.py
  (renders / measures Decimation's guns from the jar, `attach` adds
  attachments as the game places them), dev test mode `gunview` (hip, aim
  and NPC shot per gun, -Pguns=uzi,mac10 -Pattach=reddot,smgSuppressor),
  stats dataset docs/references/decimation_gun_stats.tsv. Project skill
  `.claude/skills/decimation-gun` (committed; .gitignore keeps only
  .claude/skills) runs the whole gun workflow.
  v0.36.0: MAC-10, first gun of our own: pipeline tools/guns (spec ->
  .bmodel / texture / icon / .anib), tools/bbmcp.py drives the headless
  Blockbench MCP (renders without the game), fixes/NewGuns, dev test mode
  `gun`; docs/gun_model_spec.md section 6. Our items register as
  deciworldgen:<name>: lookups by name must try "deci" then "deciworldgen".
- Obfuscated Decimation names in OUR code go through
  `net.decimation.fixes.Deci` (readable accessors: player data as
  `Deci.player(p).bottlecaps()`, server config, registry items / blocks,
  multiblocks, vehicles, NPCs, damage sources, zones, tracers). The game
  loads Decimation's obfuscated classes, so the names cannot be renamed;
  since 2026-10-09 NO obfuscated name is used outside Deci (migrated all
  ~60; checked: seed 1 world 0 blocks differ, autotest all pass). Our own
  worldgen uses `net.decimation.worldgen.ZoneKind` (MILITARY / POLICE /
  SAFEZONE) instead of Decimation's obfuscated zone enum
  (`net.decimation.mod.server.zones.a`); `Deci.zoneType(kind)` converts.
  New code: add an accessor to Deci, never call deci.* directly.
- World generation: direction reversed after companion mods failed (Ruins /
  ezWastelands / GeneratorMods all dropped — see `new_feature.md`). Now built
  as our own code: second `@Mod` (`deciworldgen`, `required-after:deci`) in
  `src/net/decimation/worldgen/`, compiled via `tools/build.py` into the same
  jar — Forge scans all jar classes for `@Mod`, so no bytecode edit to the
  obfuscated mod class needed. Structures use `IWorldGenerator` (ride on any
  terrain gen). Terrain: since v0.14.0 our own "Decimation" world type
  (docs/terrain.md); RTG 1.7.10-1.1.1.7 still works as an alternative. Milestones 2 (marker) and 3 (real
  `.schematic` structure placer: cell-based deterministic placement, dirt
  foundation, slope/water site checks, 24x24 footprint cap, schematics read
  from `config/decimation_worldgen/`) both user-confirmed in-game 2026-08-15.
  Test schematics generated by `tools/make_test_schematics.py` into
  `structures/`. Milestone 4 (rotation: `Rotation.java` per-family metadata
  table, deterministic 0/90/180/270 per cell) confirmed 2026-08-15. RTG
  installed 2026-08-15. Source now lives in `dev/src/main/java` (the root
  `src/` + `tools/build.py` injection path is superseded by the separate
  jar). v0.7.0 (2026-10-07): zone auto tagging (`ZoneStore`, `mil_` ->
  MILITARY, `city_` -> POLICE, per-world `deciworldgen_zones.json`),
  singleplayer zone spawn fix (`ZoneSpawnHandler`), underwater placement fix.
  v0.8.0 (2026-10-07, dist only, Prism still 0.7.0): city street grid in CITY
  sectors (`paintStreets`), structure anchors limited to the first 3 chunks
  of a cell. v0.8.1: vehicle one punch pickup fix (`VehicleHitHandler`).
  v0.9.0 (dist only): armor vs NPC gunfire (`ArmorGunfireHandler`),
  singleplayer supply drops (`SupplyDropScheduler`) and humanity
  (`HumanityKillHandler`).
  Next: user review of streets, lane markings, street furniture, driveways,
  bridges.
  Decimation's own loot registry (`deci.aB.e`) is block-type-agnostic and will
  pick up its own crate/ammobox blocks in any structure regardless of who
  placed them, so loot inside our structures needs no new Decimation code —
  just place the right blocks in the schematics. Confirmed Decimation itself
  never touches world/biome generation (only an unrelated internal rendering
  stub, `deci.e.d`, implements `IChunkProvider`).
- Performance complaints (scope-aiming FPS drop, prop-dense-area FPS drop) not
  yet investigated — flagged as needing actual profiling, not guesswork, since
  static bytecode reading alone won't reliably find a render bottleneck.
- Custom weapon models: `.bmodel` is PLAIN TEXT (Techne style model code,
  parsed line by line), fully documented 2026-10-07 in
  `docs/gun_model_spec.md` together with the `.anib` animation format, every
  asset path, the renderer and a new gun checklist. Reskinning an existing
  weapon is low risk; a new mesh needs Techne/Toolbox (visual work) plus the
  checklist. (An older note here called it a binary format; that was wrong.)
