# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project actually is

This is not a normal source-code repo. There is no source for the mod — it's a
closed-source, abandoned Minecraft Forge 1.7.10 mod ("Decimation", modid `deci`,
by ScottehBoeh/BoehMod, official server `mcdecimation.net` permanently dead). The
"codebase" is a single 238MB compiled, name-obfuscated jar. All work here is
**binary reverse engineering + bytecode patching**, not editing and recompiling
source. Purpose: singleplayer fixes and world generation for Decimation (the user's copy
came via the Technic modpack). Releases are PUBLIC (user decision 16 Oktober 2026): the one
Decimation.jar on GitHub releases and a download page (docs/roadmap.md UI 2); credit BoehMod,
say unofficial. The repo itself still never holds Decimation's jars, code or art.

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

Two branches (user, 18 Oktober 2026: pre-releases carry only low to medium fixes; multiplayer and the new cities ship
together in 1.0.0, so no jar a player downloads before 1.0.0 may hold multiplayer code):
- `main`: everything, all development, the docs, the graph. Never rewrite its history (the user's GitHub contributions
  count its commits). Multiplayer source on it is public since 18 Oktober 2026 (accepted by the user: only the jars
  must not carry it).
- `prerelease`: started at tag 0.64.8 (no multiplayer), checked out as a git worktree in
  `/Users/ucilmenangis/Projects/Decimation-prerelease` (dev/libs there is a link to main's). It gets ONLY plain fixes,
  carried from main by hand, one commit each, message naming the main commit ("from main abc1234"). Pre-release jars are
  built only from it. Never carry fixes/hosting, relay/, OptionalMods, the Lan* tests, the Community texts or the
  Multiplayer news panel. `git apply --3way` STAGES what it applies: stage and commit each fix by its file list and
  check `git show --stat` before the next one. Carried so far: UpdateBar, HudGuard, PlaceNames rejoin, GunUseKey (+
  corpsegun), NaturalSpawns (+ nospawn), GlStackGuard (+ glstack), EmptyStacks (+ throwables), PropSounds (+ radios,
  soak), the PatchIntroThread / PatchNoVersionFetch sources (+ threads) (19 Oktober 2026, prerelease df023cc), the NPC
  patch sources (Trader / Bandit / Mech target, BloodParticle, HumanScare, InfectedAI v2), TargetPacing (+ targetpace),
  GunArmSkin (+ armskin), GunNoise skip, ScopeZoom hip (+ scopeperf, scopediag), bench / traderperf / zoneperf
  (prerelease 0a2f601), HumanLayers + PatchHumanLayers source (+ npclook, npcdraw, bench layers=off; prerelease e53bad7), EntityLighting + PatchLightLeaks
  source (+ lightleak; 972c833), ScopeZoom OptiFine guard and glass wipe (+ scopeperf checks; 0af0eee), ModelBatch +
  PatchModelBatch source (+ gunbatch, npcdraw fight; e250f0e), city stall fixes: DeciBiome.decorate, BlockBatch, Keys,
  Highways lookups (+ genwalk, pregen walk and light check, tools/genprof.py; 9861d61), Continue tooltip (14830b9),
  first sight warm up: ModelWarmup, Deci gear model accessors, SafezoneSites fill timing (+ refugevisit; 91ef15a),
  release builds only look for updates and each line's live test ports (tools/lineports.py; 23605eb), Supporter texts
  (b90b4af), the update question UpdatePrompt (41a00d8), MenuStatus rows and DevTestUtil.click (193e50b; Community
  Server stays Coming soon there: the relay check is main's hosting code), the question on the Esc menu and SafeQuit
  (5e000e1), Discord links and our footer (bd9fab9), the Updates and Coming soon popups (3de0c39; no Multiplayer popup),
  the corner art and MenuCorner (5246e4b), the popup crash fix (e541d84), the corner redone to FURY's measured layout
  and the siteshots cast column (3aa1cdd; not the Multiplayer popup texts), the Gunsmith screen (fixes/gunsmith, dev tests gunsmith and gunsmithmp;
  0434450, the inside out fix b7c175a, the final review fixes 5cef9db, turning with the mouse 69c7157), the new
  attachments (RK-1, SpecterDR 1x/4x, recoil per grip, grips per gun from one list; dev tests attachments and
  attachmentsmp; 8a976c8: needed there because the shared base jar's PatchGripRecoil calls GripRecoil), the user's
  review of them (RK-1 v2, grips and sights only on their mounts, MountStrip, kept sight positions SightMemory and dev
  test aimdelay: 55b5fa8, be01288, beb4103, 99813b0), the updater's Accept header (e4fc4ca, released 0.66.1), and on
  22 Oktober 2026 the bugfix agent's fixes (prop hardness b4241f5, inventory shift click 16fdd6f, Foregrip seat
  7541706) and features 1 to 4 (armor 1db8fee, NPC tracer colours 31a8411, loot magazine rounds 3276b01, magazine
  unload and our channel e63e59b, 419ec46; unload v2, one round a sneaking click like Decimation's loading in reverse, ed9fe9c; looted guns hold rounds cb940c1; radiation dose on the HUD 62a059f, its test fixes 4e4b85c; no Steve (Decimation skins by name, corpses) c3cb6a1; DayZ dark nights and real moon phases 787a955; durability and repair parts acc19bf, the Deci.toast it needs ec2e091 (main has it with its hosting code), the melee wear fix 2cf49ad; the feature agent's radiation set (pills, gas mask filters, the wasteland radiation zone) 2923793; its filter follow up (military share, tear gas) 07a1f7f, mobs by place (feature 8) efd0e4e; loot rules A, looted stays looted 36327b7; loot rules B, lootable copies and kinds, with its test fix 611a767; loot rules C, the Tarkov style search, with its security fixes 4bed563; the production client classpath tool fix 176482c; raihanadf's PRs, melee 3c49f6d and the Inline HUD with blood on infected 021a159, for 0.70.0). Carries touching DecimationWorldGen / DevAutoTest: apply without those two
  files and add the registration lines by hand (prerelease lacks main's hosting and cape lines, 3-way merges conflict).
  A NEW CHECKOUT'S RUN FOLDER needs the content: `cp dist/config/decimation_worldgen/*.schematic
  dev/run/client/config/decimation_worldgen/` (lc and large too when missing). StructureGenerator.generate returns at
  once while no small schematic is loaded ("0 schematic(s) ready" in the log), and that also stops cities, refuges, camps
  and bases: the worktree's dev client generated no structures until 20 Oktober 2026 for this reason (11 copied, then
  refugevisit all PASS there). A one jar of the worktree's build in the production client still checks what ships
  (`python3 tools/onejar.py dev/build/libs/<exact jar> OUT` from the worktree; pick the jar by name, the folder holds
  older builds).
  RELEASING FROM IT (first done 20 Oktober 2026 for 0.65.0, docs/production_test.md):
  1. Base jar: worktree dist/Decimation.jar = main's dist/Decimation.jar minus
     assets/deci/textures/gui/menu/news_multiplayer.png (zip -d). Check first with `python3 tools/linecheck.py
     dist/Decimation.jar <worktree>/dev/build/libs/deciworldgen-VERSION.jar --last dist/previous/onejar-LAST/Decimation.jar`
     (from main): every call the base jar's patched classes make into net/decimation/fixes|worldgen exists in the
     prerelease build with the same descriptor (26 for 0.65.0; exit 1 and MISSING lines otherwise), the entries to strip
     (news_multiplayer.png and the Multiplayer popup's detail_multiplayer0..2.png),
     and every Decimation entry changed since the last release (read each: it must be a plain fix). Both lines share ONE
     base jar: main only behaviour lives in our classes, never in a jar patch (a patch both lines carry calls a hook both
     lines have). Also check that the prerelease's menu_assets.py pictures equal main's (run a copy of it
     inside main's tools/ with --only-dir: it reads git ignored sources under docs/shots; never run it in the worktree,
     whose dev/libs is main's).
  2. Git ignored generated files the worktree lacks: the 28 gun files of UMP9 / HK416 / HK416 Tan / Mk18 under
     dev/src/main/resources (generated from Decimation's art by tools/guns): copy them from main's tree (same
     generators; byte equal to what 0.64.8 shipped). Compare the two resources trees every release. And
     dist/config/decimation_worldgen (the content; same hash as 0.64.8: 3d2721f3ce41).
  3. `git tag VERSION` on prerelease HEAD (lightweight, local), `./gradlew build` in the worktree's dev/ (version from
     the tag), `python3 tools/onejar.py VERSION` in the worktree (dist/onejar/Decimation.jar + .sha256).
  4. Production server (`python3 tools/prodsrv_run.py SERVER_DIR JAR NAME -D...`): pregen light check equal to the dev
     numbers, walk; production
     client: genwalk, fix checks, menus, scopeperf with OptiFine, and the REAL update: the last release's jar in mods/,
     `tools/fake_release_server.py 8765 VERSION 40 50 FILE=<candidate>`, `devtest.py --live update jar=true
     expect=VERSION` (its "NEWJAR" head check FAILs by design with a real jar: compare the swapped jar's sha256).
  5. After the user pushed main and prerelease and said go: publish as a draft first, then make it public (RELEASE
     0.65.0 below); check the asset digest, the site's Download, and the real update from GitHub with the last release's
     jar: `devtest.py --live menu`, then `update real=true jar=true from=LAST expect=VERSION` (menu first: the welcome
     fade trap below), restart and read "up to date (VERSION)"; archive in dist/previous/onejar-VERSION.
  TWO LINES SIDE BY SIDE (20 Oktober 2026, user: "figure out how development goes on without interrupting others"):
  - Each checkout runs its own live test game: main 25599 with debug port 5005, the prerelease worktree 25597 with
    5007 (tools/lineports.py by branch; devtest.py starts its game with -Pliveport / -Pdebugport and hotswap.py attaches
    to its own port). Both ran at once on 20 Oktober 2026 (menu, update, mainmenu PASS in each). main's second game, the
    production client as a joiner, keeps 25598 / 5006. Releases up to 0.65.0 listen on 25599.
  - Gradle in one checkout while the OTHER checkout's live game runs is fine (worktree test and build with main's live
    game open: its runs still PASS). In the same checkout the old rule stands: no Gradle while its game runs.
  - Only release builds look for updates (fixes/Updater.offersUpdates: no "+" in the version, not the dev client):
    the dev client and every test jar of main or prerelease say "Updates off: dev build (<branch>)", show their version
    as "<tag> dev" and offer Play. Until then main builds (version 0.64.8-main.N, the newest tag main reaches) took the
    0.65.0 pre-release for an update. Test jars built before main 68ecf7a (Mac Prism and wyvern feeb69f0, Desktop
    d6aea397) still offer 0.65.0 and would swap themselves for it, without multiplayer: never click Update there,
    rebuild them from main.
  - `pgrep -fl "zulu-8.jdk.*GradleStart"` lists both lines' games; `lsof -a -p PID -d cwd` names the checkout.

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
   finish refuses a graph that got smaller; after removing code, check the
   lost nodes are the removed ones and run `finish --force`.
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
- `docs/tasks.md`: short checklist of what is open RIGHT NOW (`- [ ]`, `- [~]` in progress, `- [x]`),
  shown in Claude Code's usage band and pane (usage-pane mod, ~/.claude-mods); update it whenever an
  item opens, starts or ends (user request 16 Oktober 2026).
- `docs/gun_model_spec.md`: gun `.bmodel` / `.anib` formats, paths, renderer,
  new gun checklist; section 7: gun packages of the 3D gun converter (fixes/gunpack: where they live, how a release
  adds one, the rows from "points").
- `docs/production_test.md`: testing the RELEASE jar in a plain Forge server and client built in the
  scratchpad (never Prism): steps, traps (server jar via the Forge manifest, -XstartOnFirstThread hang),
  driving it with devtest.py --live.
- `docs/call_trace.md`: dev call logger (Java agent, `-Ptrace` / `devtest.py --live MODE -Ptrace=PREFIXES`,
  `/deciworldgen trace`): which Decimation / mod methods run, in order, per thread, named from decimation.srg.
- `docs/decimation_code/README.md`: DECIMATION'S OWN CODE feature by feature (loot, weapons, NPCs, network, ...):
  one doc and one project skill `decimation-code-<feature>` each, call order checked with the call logger; every
  such doc and skill must pass `python3 tools/skillcheck.py` (looks up each class, method, Deci accessor, path, test).
- `docs/performance.md`: how to check lag (dev test modes gunperf, census,
  tools/perfcheck.py world / log / fixchunk, JFR on the live game with
  jcmd) and what was found (corrupt chunk entity explosion, infected path
  search every tick, prop tile entities in the ticking list).
- `docs/gun_style_guide.md`: READ BEFORE MODELLING A GUN. Study of all 98
  Decimation guns (tools/guns/study.py renders them from the jar, no
  game; dataset docs/references/decimation_guns.tsv): shape box parts,
  part counts, sizes, octagon / curve / panel recipes, dark flat tones.
- `docs/gunconv.md`: the 3D gun converter (tools/gunconv: private local page on 127.0.0.1, converter, gun packages
  for the game side in build/gunconv/out; study.py reads a package through STUDY_OURS). Sources:
  docs/references/gunconv_sources.md (CC0 / CC BY only, credited).
- `docs/military_base.md`: US FOB style military bases (reference research,
  layout of the three sizes, prop mapping, code map, how to test).
- `docs/prop_placement.md`: READ BEFORE PLACING ANY DECIMATION PROP FROM CODE.
  How props are drawn (renderer chain, per renderer metadata tables, model
  offsets), facing rule and exceptions, drawn sizes, placement rules;
  tools/props/propgeom.py (drawn box per metadata from the code,
  docs/references/prop_geometry.tsv), tools/props/propclash.py (checks a
  block dump), skill decimation-props.
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
- `docs/zones.md`: Decimation's zone system end to end (safezone, military, police,
  radiation, scary, border; checks, handlers, client lists) and what fixes/ZoneRules makes
  work in singleplayer; safezone sites and the "destroyed by the player" plan.
- `docs/safezone.md`: safezone sites: survivor camps (layout, residents and their keeper,
  placement, test) and city refuges (our fortified building, one per city region, section 7).
- `docs/ideas.md`: the user's future feature ideas (12 Oktober 2026), not designed yet.
- `docs/player_hosting.md`: ANALYSIS of player hosted worlds bridged by our relay and listed on the Community page
  (ideas item 22, 18 Oktober 2026): code facts, ten ways in four layers, phases, the user's open decisions.
- `docs/city_replacement.md`: PLAN for legal city content (ChaosZPack checked: license blocks it; McJty Lost Cities MIT
  recommended); the release jar ships DeceasedCraft's buildings today (474 files).
- `docs/release_1.0.md`: PLAN of 1.0.0 beta (first non pre-release): scope, open bugs, candidates, gates, order.
- `docs/promotion.md`: promoting DeciRemnant (multiplayer teaser before its release): research with sources, the
  reupload risk to raise, plan, Reddit and Discord drafts (the user posts).
- `docs/website.md`: SPEC of DeciRemnant, the project website, LIVE at https://deciremnant.vercel.app/ (private repo
  github.com/ucilmenangis/deciremnant, local ~/Projects/deciremnant, Next.js on Vercel): page text, look (Minecraft Standard font at the game's sizes, item icons), release data,
  screenshots (dev test `siteshots`: camera rows tools/site/siteshots.tsv, `-Pwindow=1440x810` / `devtest.py --live
  --window=1440x810`, subjects from server logs with tools/site/subjects.py), art export tools/site/export_assets.py
  (content hash names), checks; skill decimation-website. Since 17 Oktober 2026 also the server guide page
  /docs/server (section 3b: every command from a real scratchpad Forge server with the release jar) and guide pictures
  of Minecraft's own screens (dev test `guideshots`, `export_assets.py --guide`).
- `docs/main_menu.md`: OUR main menu changes (v0.63.0 .. v0.64.1): Decimation's news panels with our four items, the
  real scene background slideshow, links / name / version / Continue, the server pages "coming soon", Early access,
  PatchMenuBackground / PatchMenuTexts / PatchOfficialServers, tools/menu_assets.py, dev tests mainmenu and menupages;
  how Decimation's menu works element by element: docs/decimation_code/menus.md section 7 (server pages 7b). Since
  20 Oktober 2026 section 8 maps every piece of the home and in game menu to its code, section 9 says how to change
  it; project skill decimation-main-menu (load it before any main menu work).
- `docs/systems_plan.md`: PLAN of every open feature together (loot, human NPCs, zombies, world and
  zones) with the shared core (world clock, pools, body entity, tier table, areas, protection
  table, group spawner) and the build order; user choices of 13 Oktober 2026.
- `docs/loot_levels.md`: DRAFT design of crate levels (kind x 4 levels by content type) and NPC loot bodies; how
  Decimation's loot works (33 blocks, 12 lists, 1 .. n items per open since the v0.57.0 fix, rarity per item).
- `docs/porting.md`: PARKED plan for a port to modern Minecraft (three paths: modpack, gun pack for a
  modern gun mod, full NeoForge rewrite; model conversion; first spike), for later only (user).
- `docs/fabric_port.md`: Decimation + DeciRemnant on Legacy Fabric 1.7.10 (user 23 Oktober 2026): Forge applied
  ahead of time (tools/fabric: mappings.py, Forgify game / forge / mod / strings, build_jars.py with shims for
  ModClassLoader and ReflectionHelper), the layer `fabric/` (FML set up in pre launch), run_client.py (launcher style
  run, dev tests by -Ddeciworldgen.autotest.test=MODE), prism_instance.py (Prism "Decimation Fabric"). Plays: checks,
  gun, zombies, loot, menu pass; FPS +70 % with OptiFine 1.7.10 built into the game jar (the user's OptiFine jar in
  build/fabric/optifine, never shipped; OptiFabric cannot: it wants vanilla classes); no 1.7.10 Fabric FPS mod helps.
  Our Fabulously Optimized style modules in optimized/ (ChunkVBO: chunk sections from vertex buffers, EntityCulling,
  Dynamic FPS; mixins, one config per module): +11 to +38 % over OptiFine alone at 1440 x 810 (at 854 x 480 the Mac
  holds windowed frames to its 120 Hz display: bench larger). Dev tests fpsbench, culltest.
  Traps: Legacy Fabric API's networking mixin cannot run on Forge's MinecraftServer; SRG names in strings; mixin
  targets without a yarn name need the bare intermediary name; a mixin package belongs to one config.
- `docs/city_dressing.md`: Decimation props through the whole city (streets, road scenes,
  interiors, story layer), rules in directions, verified facing table, checks, status / handoff.
- `docs/airport.md`: military airport in a wasteland city (placement as a 2 x 4 cell block, layout, block-built
  planes, garrison, loot, checks).
- `docs/wasteland.md`: the wasteland district as the city's military area (DeceasedCraft check,
  one MILITARY zone per district, military buildings, loot, dressing, enemy military, how to test).
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

## Every feature: optimized before done (user rule, 21 Oktober 2026)

User: "dont forget to optimize yet. you always forget this. noted that for future development". Performance and bugs are
part of every feature, never a later pass:
1. The spec has a performance section: what runs per frame, per tick and per packet, and a budget for each (frame time
   with the feature in use against without it, server tick cost, packets, memory).
2. The plan has an optimize task before its docs task: measure the budgets with a dev test probe (frame time, tick
   time), check nothing is searched, loaded, compiled or allocated in bulk per frame or tick (compute on open or on
   change and cache), GL stacks and errors clean (glstack), no growth over a soak, packets only on change (main).
3. Done means the numbers are measured and written into the feature's doc: dev client, and for anything drawn also the
   production client with OptiFine (docs/production_test.md). Profile before optimising (docs/performance.md).
4. Bugs: a review pass for item loss or duplication, edge cases (death, disconnect, full inventory) and both lines.
   A project skill that audits each feature for bugs and performance is planned (user, later; docs/tasks.md).

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
  Azul 21 too (RFG's applyJST asks for a Java 21 toolchain). Get them from
  api.azul.com/metadata/v1/zulu/packages (macos, aarch64, tar.gz) into `~/.jdks/zulu-N.jdk`.
  FRESH MACHINE (22 Oktober 2026, the user's Mac at /Users/raihan, no jars, no tags): base jar and
  content can come from the latest GitHub release: `dev/libs/Decimation-base.jar` = release
  Decimation.jar minus net/decimation/{fixes,worldgen}/ and assets/deciworldgen/, mcmod.info = the
  deci entry only; `dist/config/decimation_worldgen` = its assets/deciworldgen/content/ (minus
  manifest.txt, version.txt); for onejar, `dist/Decimation.jar` = that base minus every name the
  new deciworldgen jar holds (16 gun / sight files and LICENSE otherwise clash). The Scala libs'
  usual host gregtech.overminddl1.com is down: dev/repositories.gradle takes org.scala-lang from
  maven.minecraftforge.net (exclusiveContent, jar only). Without local tags the build version is
  the commit (deciworldgen-c82538a-main+...jar): give onejar a copy named deciworldgen-VERSION.jar.
  The user's Prism instance `Decimation` (Forge 10.13.4.1614) was made for testing on this Mac.
- `dev/libs/` (git-ignored): `Decimation-base.jar` = `Decimation.jar.patched`
  minus `net/decimation/{worldgen,fixes}` (else our mod loads twice), pulled in
  via `devOnlyNonPublishable(rfg.deobf(...))`; `DecimationVoiceChat.jar` (Gliby's
  Voice Chat, from Prism) deobfed and copied to `run/client/mods/` by the
  `installVoiceChat` task. Needed because Decimation's menu shows "Update"
  instead of "Play" when `mods/DecimationVoiceChat.jar` is absent
  (`net.decimation.mod.common.utils.h.gI()`). Never click that Update button. Since v0.57.2
  PatchNoUpdate makes gI always false, so the voice chat jar is no longer needed for "Play".
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
  0.7.0 jars and old config in `minecraft/backup_20261010/`; since
  2026-10-11 deciworldgen 0.42.1 (each replaced jar moved to
  `minecraft/backup_20261010/`, older dist jars in `dist/previous/`). The Prism
  instance also has OptiFine HD U E7 and RTG; the dev client has neither
  (the user tests in the dev client: its fps are without OptiFine). Spotless is disabled to keep our style.
- TEST WORLD (2026-10-11): dev tests use their OWN world `deciworldgen_devtest`
  (DevAutoTest.SAVE); the user plays in `deciworldgen_autotest` (same run
  folder, dev/run/client). Never point a test at the user's world: a fresh
  world test deletes its folder. The live test game quits safely (leaves the
  world, waits for the save, then exits; DevAutoTest.quitSafely): mc.shutdown()
  inside a world once wrote a broken level.dat id table into the user's world
  (bug.md). Before starting a live test, check no game of the user's runs
  (`pgrep -f "zulu-8.jdk.*GradleStart"`), and never run two games on one world.
- PRISM IS OFF LIMITS (user rule 2026-10-11): the user tests ONLY in the dev
  client (`./gradlew runClient` / IntelliJ), never in Prism. Do not launch
  Prism, do not kill its processes, do not copy jars into its `mods/`.
  Releases end at `dist/`. The Prism notes here are history only; touch Prism
  only when the user asks for it in that conversation.
- A Javassist patch to Decimation's own classes must go into THREE jars:
  `Decimation.jar.patched`, `dist/Decimation.jar`, `dev/libs/Decimation-base.jar`
  (not Prism: see the rule above). Patch sources live in `tools/patches/` (so far:
  `PatchSwing.java`, SmoothSwingThread busy loop; `PatchPropCulling.java`,
  props not rendering, both 2026-10-07; `PatchScope.java`, picture in
  picture scope gated behind `decimation.scope.pip`, 2026-10-08;
  `PatchTracer.java`, NPC tracers aimed at the target, 2026-10-09, needs
  netty-all 4.0.10 from Prism's libraries on the Javassist classpath;
  `PatchBackend.java` (2026-10-09) launch no longer waits 5 s for the
  dead Decimation server; v2 (v0.62.8) no BoehMod connection at all (each reconnect try leaked a thread); `PatchTracer.java` v2 (2026-10-09, aim point in
  the packet, `shotHook` in BanditEntity.shootAt, NPC tracers always
  visible; v3 2026-10-14: tracers end where the shot ended, drawn segment
  clamped to the tracer's length) then `PatchFactions.java` on top of its deci/ag/a output
  (Soviets vs everyone else);
  `PatchMenuStats.java` (2026-10-11, ASM not Javassist: rewrites string
  constants): menu / HUD stat labels (Human Kills, Deaths, Total Human Kills,
  Total Infected Kills) in deci/i/a, deci/i/e, deci/g/j;
  `PatchIronSights.java` (2026-10-10): defaultScope parts hidden while a
  sight is attached (deci/K/b, deci/n/f on top of PatchScope; Javassist
  reads a class path as a jar only if it ends in `.jar`, so feed it
  dist/Decimation.jar, not Decimation.jar.patched);
  `PatchTurfNames.java` (2026-10-12, docs/zones.md "Place names"): IngameHud chat lines turn "_"
  into spaces (turf names travel with "_"); argument `layer` (v0.52.1): the same for the floating
  turf name of ClanTurfLayer (deci/M/l);
  `PatchNoUpdate.java` (2026-10-13, bug.md): UpdateChecker.isUpdateRequired (net/decimation/mod/common/utils/h.gI)
  always false: no "Update" button without mods/DecimationVoiceChat.jar; dev test `menu`; v2 (v0.59.0)
  answers net.decimation.fixes.Updater.required() (our auto update), patched from the original class
  with our compiled classes on the Javassist path;
  `PatchLootRolls.java` (2026-10-13, bug.md): IntRange.pick (deci/aB/a) inclusive, loot counts 1 .. n;
  `PatchMenuTexts.java` (2026-10-17, docs/main_menu.md 2b, 2c and 4b, ASM on the ORIGINAL classes plus PatchMenuStats,
  kept as build/menutexts_rebuild/base.jar): menu links, title line, version and window title (net.decimation.fixes.
  MenuTexts); v0.64.1: status page rows, row state "Coming soon", Early access texts, Community toast (47 texts, 12
  classes); `PatchOfficialServers.java` (2026-10-17, Javassist): the official server list (deci/k/b.bg) comes from
  fixes/MenuServers (our classes on the Javassist path, third argument);
  `PatchMenuBackground.java` (2026-10-17, docs/main_menu.md): RenderUtil.drawMenuBackground (deci/j/a.b) without its
  black layer, cube planet and glow sprites, its picture drawn by fixes/MenuBackground (slideshow; our classes on the
  Javassist path, third argument);
  `PatchIntroThread.java` (19 Oktober 2026, bug.md "FPS falls minute by minute"): the boot intro's frame thread
  (deci/i/d.aS) busy waited at 99% of a core all session; it sleeps and ends when the intro is finished (on top of the
  intro skip); `PatchNoVersionFetch.java` (same day): UpdateChecker.tickVersionCheck (h.gH) no longer fetches BoehMod's
  dead latestversion.txt every 60 s (on top of PatchNoUpdate);
  `PatchTraderTarget.java`, `PatchBanditTarget.java`, `PatchMechTarget.java` (19 Oktober 2026, bug.md "Safezones lag"):
  findTarget (eA) of ArmedTraderEntity deci/ai/v (server only), BanditEntity deci/ag/a and MechEntity deci/ag/k every 10
  ticks, hostility before the ray trace (on top of PatchTracer / PatchFactions for v and a); `PatchBloodParticle.java`
  (same day): ground blood splats deci/B/k move only until they land; `PatchInfectedAI.java` v2: no unused wander search;
  `PatchHumanScare.java` (same day): FactionHumanEntity deci/ah/d no longer walks every loaded entity each tick;
  `PatchHumanLayers.java` (same day, bug.md "Drawing many human NPCs"): ModelHuman.renderHuman (deci/q/h) draws each look
  from one picture (fixes/HumanLayers; our classes on the Javassist path, third argument; patch from a copy of the
  ORIGINAL jar: build/patch_layers/original.jar) and answers its three dead line of sight checks true;
  `PatchLightLeaks.java` (same day, bug.md "Muzzle flash lit every mob"): on top of PatchHumanLayers (re-run it after
  that one), an NPC's night vision goggles leave the lights as they found them (fixes/EntityLighting.save / restore;
  fourth argument: lwjgl 2.9.4 from the gradle cache); fixes/EntityLighting puts lighting back around every entity;
  `PatchModelBatch.java` (same day): BModelPart deci/n/b and BModelQuad deci/n/c: each part's quads captured once at
  compile into one more display list (fixes/ModelBatch), drawn instead of Decimation's two lists of one draw per quad;
  ModelBatch.enabled switches back at run time (dev tests gunbatch, npcdraw);
  `PatchGripRecoil.java` (21 Oktober 2026, docs/decimation_code/weapons.md section 8): GunItem.clientHeldTick (deci/ay/i
  .d) turns the view by fixes/GripRecoil's factor for the fitted grip (Foregrip 1/3, RK-1 1/2 up and 1/4 sideways) instead
  of comparing with Decimation's Foregrip, and GunItemRenderer.renderItem (deci/K/b, on top of PatchIronSights: read
  dist/Decimation.jar) calms the view kick through GripRecoil.calmsView; it throws unless it finds
  exactly 2 Foregrip reads, 2 pitch and 2 yaw reads and 1 equals;
  `PatchDurabilityBars.java` (23 Oktober 2026, new_feature.md "Durability v2"): GunItem (deci/ay/i, on top of
  PatchGripRecoil and PatchGunWear: read dist/Decimation.jar) and ItemGasMaskDeci get Forge's showDurabilityBar /
  getDurabilityForDisplay, answered by fixes/DurabilityBars (gun wear, mask filter): vanilla's own bar on their icons;
  `PatchHudPercents.java` (23 Oktober 2026, bug.md "Radiation percent not centred"): IngameHud.render (deci/g/j b, on top
  of PatchMenuStats and PatchTurfNames: read dist/Decimation.jar) takes the x of its six outlined texts from
  fixes/HudPercents.x, which centres the food, water, blood and health percents on their icons; it throws unless it finds
  exactly six calls;
  `PatchVehicleArrows.java` (2026-10-11, bug.md vehicle entry): indirect player hits
  (arrows, thrown, TNT) on deci/ad/e take the health path, not pickup;
  `PatchInfectedAI.java` (2026-10-10, docs/performance.md): infected
  wander path search server side once a second instead of every tick on
  both sides, horde scan every 10 ticks (deci/ag/d);
  `PatchPropCulling.java` step 3 (2026-10-09) caches line of sight answers,
  step 4 render distance by prop size, config deciworldgen_props.cfg,
  default 64 = vanilla). Patch from the ORIGINAL classes
  only after checking the target class is identical in the patched jar. Earlier
  patches (loot handler, proxy cast, weapon nerf, armor buff, intro skip,
  ammo crate) were one-off and have no saved source.
- Prism (history, do not use without the user's request): `CustomSkinLoader_Universal-15.0.1.jar`
  is renamed `.disabled`; it is a Forge 1.8+ coremod and crashed every launch.
  A splash "Minecraft Crash Report" saying "THIS IS NOT A ERROR" is only a spec
  printout.

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
  `deciworldgen_devtest` (was deciworldgen_autotest until v0.42.5) from seed 1, spawns infected inside a generated
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
  mostly 3D model parts. 16 Oktober 2026: every non synthetic method named (1998; the
  597 left were model boilerplate, menus, render helpers, ~130 gameplay; table
  `maps/m_methods_rest.tsv`, H 446 / M 110 / L 41); only compiler made lambda bodies and
  accessors (203) keep obfuscated names. Methods ending in `Lost` are overrides of
  Forge added methods that the obfuscator renamed, so they never override [inferred]. Sources of names, in priority order (first name
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
  `deci.a.b.d()` (getServerProxy) hard-cast the proxy to ServerProxy and threw
  on the client; the early "proxy cast" patch makes it return null there. This proxy split is the root cause of nearly every singleplayer bug
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
  v0.40.0: Mk18 Mod 1 (tools/guns/mk18.py, run before building) and iron
  sights hidden under a sight (fixes/IronSights, PatchIronSights).
  v0.40.1: sights onto each gun's rail (tools/guns/sightfit.py ->
  sight_offsets.txt, fixes/SightPlacement; rerun sightfit after a new gun
  or sight).
  v0.41.0: our sights EOTech 558 and ACOG TA11 (tools/guns/sights.py,
  fixes/NewSights, registered as deci:<name>), Mk18 IMI TS stock.
  v0.41.1: reticles drawn in the sight's glass, swaying with the gun
  (fixes/SightReticle, sight_reticles.txt); HK416 front sight folds away.
  v0.42.0: US FOB style military bases in military sectors, three sizes
  (docs/military_base.md, worldgen/military, skill decimation-military-base,
  dev test `milbase`); the mil_ test schematics are no longer placed.
  v0.42.1: user review fixes: HESCO as our own plain block deciworldgen:hesco
  (Decimation's BlockMilitaryBarrier is a tile entity per cell), locked TOC
  door + keycard, gravel ground, sandbag ASP, every prop placed from its
  drawn box (worldgen/military/PropBoxes, Canvas.validateProps: 0 clashes);
  the milbase test writes run/client/devtest/milbase_<size>.tsv for
  tools/props/propclash.py.
  v0.42.2: second review: AS Val sights over the receiver (sightfit SHIFT,
  dx column in sight_offsets.txt, aim compensated), local stats
  (fixes/LocalStats, PatchMenuStats, dev test `stats`), menu session flag set
  valid (green Play), base loot / props (TOC door wall, chests, DFAC radio,
  motor pool parts yard); milbase test site moved to -20000,230,-20000 and the
  old sites cleared from the dev world (tools/perfcheck.py clearblocks).
  v0.42.3: third review: tower ladder mid inner side, hangar vault closed +
  loot, shelter roof; MilBaseTest pregenerates and populates the chunks under
  a base before building (worldgen below a sky base read its top as the
  ground and cut it) and places each size at X0 + size * STEP.
  v0.42.4: tower ladders on an open face (MilitaryBase.ladderCell), the
  outpost TOC back (missing since v0.42.0), walk test tools/props/walkcheck.py
  (must be 0 before handing a base over), milbase test checks "has its TOC".
  v0.42.5: wreck hardness 10 (fixes/WreckHardness), sniper NPC nerf (sniper
  shots x0.8, Barrett x0.6, sniperRange 70, Barrett tiers die to one 5.56
  magazine; deciworldgen_npc.cfg version 3), dev test `npckill`; tests in
  their own world deciworldgen_devtest, safe quit of the live test game.
  v0.43.0: gunshot noise (fixes/GunNoise, config deciworldgen_noise.cfg,
  new_feature.md "Gunshot noise"): infected and enemy NPCs walk to shots
  heard (64 blocks, 12 suppressed); dev test `noise` (6 checks).
  v0.43.1: aim drift fix (bug.md "Gun creeps down while aiming"): ScopeZoom
  measured the scope glass of NPC guns too; measuring now only during the
  player's hand draw. Dev test `aimdrift` (sniper NPC in view).
  v0.43.2: NPCs kept a dead old style target (entityToAttack) forever and
  ignored gunshots; GunNoise clears it. Decimation's NPCs never react to a
  creative player: test NPC reactions in survival. `aimdrift
  changes=gun:sight,...` swaps gun / sight while aiming. Test NPC modes must
  remove mobs they did not spawn (NoiseTest.sweep): arena strays distract NPCs.
  v0.43.3: Decimation never clears an NPC's entityToAttack (the last enemy
  seen); only getAITarget (revenge, expires) means "fighting". GunNoise
  reads that. noise test: -Ptier / -Pfar, stale target held, hit counter.
  v0.44.0: underground railway (city/Rails.java, docs/city_engine.md
  "Railway"): tracks on the highway lines at y 46, a station per city,
  pack rail parts via `lcpack.py OUT --rails dc=...`; springs / lakes
  denied near rails (a tunnel written by two windows got flooded); dev test
  `rail` (needs never generated chunks: each run moves on), hwmap --rails.
  Deci.homeMenu is @SideOnly(CLIENT): a client type in Deci broke the
  dedicated server (servertest.py) since v0.42.2.
  v0.45.0: rivers through cities (city/CityRivers, city/RiverPlan,
  docs/city_engine.md "Rivers and bridges"): canals, DeceasedCraft bridges,
  river cells level 0; ON ONLY FOR WORLDS CREATED SINCE 0.45 (StructureData
  "!cityRivers"; -Ddeciworldgen.cityrivers overrides). Dev test `bridge`
  (forces them on for the session). Check for the user's game as its own
  step before any live test (bridge run 1 ran beside it).
  v0.45.1: bridges are the street (StreetPlan onWater) carried over the canal by
  RiverPlan: same road / paint / sidewalks / lamps, deck and piers below, railings
  on non street edges, wasteland bridges broken (hole in the road); DC bridge parts
  dropped (user review). City work: skill decimation-city.
  v0.46.0: zones work in singleplayer (fixes/ZoneRules, docs/zones.md): copies of the
  server only zone handlers, safezones protect every friendly entity, provocation (guards fire
  on a player who attacks someone protected), client zone lists; dev test `zones` (19 checks).
  v0.47.0: survivor camps (docs/safezone.md): military/SurvivorCamp on the shared CanvasPlan
  (MilitaryBasePlan moved onto it), SAFEZONE + residents (SafezoneSites keeper spawns traders,
  guards, civilians, survivor tier) in wild / civilian sites, new worlds only
  ("!safezoneCamps"); dev test `camp`; walkcheck opens fence gates.
  v0.47.1: camp test keeps its camp populated, trader / guard respawn at once after a dedicated
  server's trader reset (Decimation wipes all traders every 3000 ticks there), awning fronts
  coloured, `/deciworldgen camp [turns]` (dev test `campcmd`).
  v0.47.2: no provocation (user: friendlies get hit by accident), keeper fills a camp only when
  its chunks are populated and puts residents back (a guard hung over the wall), entry sign one
  block out, cosmetic props (jeeps, car, care packages, crates, barrels, tyres, table).
  v0.48.0: city refuges (docs/safezone.md section 7): military/CityRefuge on the block nearest each
  city region's middle (LcCity.refuge), walled yard, chicane gate, three storey building with a
  market hall, clinic, canteen, dormitory, armory, roof guards; 22 residents; new worlds only
  ("!cityRefuges"); dev test `refuge`. Canvas metadata 0 now turns (gates / stairs in rotated
  compounds). One launch devtest runs ignore key=value: rotations through `--live MODE turns=N`.
  walkcheck climbs stairs; propclash skips multiblock upper parts.
  v0.48.1: arrows / thrown items / player TNT no longer pick up an empty vehicle
  (tools/patches/PatchVehicleArrows.java, three jars; checks "vehicle arrow"). Test trap: a live
  `checks` run right after camp / refuge leaves the player in a safezone (damage and spawn checks
  FAIL): run an arena mode first (`--live zombies checks`).
  Order trap (17 Oktober 2026, 19 mode run): `camp` after shooting modes FAILs "player inside counts as safezone"
  (Decimation's combat timer: isPlayerInZone SAFEZONE is false while it runs); `npc` elite goggles 5 / 6 and `noise`
  suppressed also failed only in that long run. All three PASS alone (`camp npc noise`). Second long run (camp and refuge
  right after checks): camp failed the same way and `npc` read 0 damage on the marine body set (the camp's safezone
  protects the arena). Run camp and refuge in a launch of their own.
  v0.49.0: converted city props fitted to their drawn size at load (city/PropFit: ceiling lights,
  shelf ledges, crates turned or chests, seats turned, floating props dropped), props of turned
  street parts turned (LcCity.partAt), one centred fit-tested wreck per DC street chunk; seed 1
  propclash findings 2901 -> 30 (docs/city_engine.md "Props fitted to their drawn size").
  Check a generated world: tools/props/worlddump.py + propclash.py; dev test `cityprops` (shots).
  v0.50.0: city dressing ON (docs/city_dressing.md; city/Dress, StreetDress, BuildingDress,
  StreetLayout, BuildingLayout, PropFacing, PropGeom): Decimation props through all streets and
  buildings by real world rules (docs/references/city_dressing_research.md) with the VERIFIED facing
  table of all 192 props (docs/references/prop_facing.tsv; corrections prop_facing_overrides.tsv; dev
  test `facing`). Off: -Ddeciworldgen.dress=false. servertest `dress` / devtest `--dress` flags.
  Skills: decimation-worldgen (index), decimation-city-street / -vehicles / -interior / -decay.
  v0.51.0: facades (shop signs, graffiti: BuildingDress.exterior, StreetDress.facades), parks and lots
  (LotDress), jams toward exits, crossing crashes, poles every chunk, DC gutters are road;
  tools/props/topdown.py draws a dump with each prop's front as an arrow (direction check, no game).
  v0.51.2: 35 % of open lots bare (LotDress scenes); base props fixed against the verified facing
  table (conex crates, booth crate, TOC ammo case). Milbase test cache: hotswap does not rebuild a
  cached base layout: --stop and start again after changing MilitaryBase.
  v0.52.0: place names (docs/zones.md "Place names"): worldgen/PlaceNames sends city districts
  (city/CityNames) and named bases (NamedPlace, StructureData) as Decimation's turf list; banner +
  chat + sound by Decimation's IngameHud; PatchTurfNames (three jars) shows "_" as spaces in chat;
  safezones cut out of district boxes. Dev test `places`.
  v0.52.1: the floating turf name over a district's middle (ClanTurfLayer) shows spaces too
  (PatchTurfNames layer, three jars).
  v0.53.0: building props varied and balanced (docs/city_dressing.md "variety and balance"):
  BuildingDress palettes per kind, a theme (kept entries, weights, variant, density) and a story per
  building, DESK / ROW groups; measure with servertest dress + tools/props/dressbalance.py
  (-Pdress logs one "dressed building" line each). Never place a BlockChest subclass where facing
  matters (it re-aims itself).
  v0.54.0: the wasteland district is the city's military area (docs/wasteland.md, city/Wasteland):
  one MILITARY zone per district, DeceasedCraft's military buildings x 3 in its pick (new worlds only,
  StructureData "!militaryWasteland"), the buildings' containers swapped to military loot, army
  supplies and wartime stories inside, zone entry checkpoints, military vehicles, corner fighting
  positions, military lot scenes; the enemy military spawner and bandit tiers count it as military
  (groups on its streets only). Dev test `wasteland`. A running dev game uses
  build/libs/deciworldgen-<tag>-dev.jar: with uncommitted changes Gradle names its build
  ...-dirty-dev.jar, so servertest runs beside it without touching that jar.
  v0.54.1: wasteland weak spots: base / camp x 3, bunker x 1 (10 military buildings in seed 1's three
  districts, not 23 alike), more open lots there (25 %, 75 % bare, new worlds), each building its own
  loot mix, concrete pads (never gravel: it falls) under motor pools and supply dumps, the helicopter
  tries every heading, enemy groups on any open spot at street level, each member on a good cell.
  The `wasteland` dev test also forces the four lot scenes in the sky (Wasteland.previewLot).
  v0.54.2: motor pools as the army lays them out (vehicle lines nose to the street, LotDress.streetSide;
  workshop, fuel point, work light, missile launcher), crash sites with burnt ground (stained clay, not
  podzol: it reads orange) and a cordon; a huge prop's drawn box (the helicopter's rotor disc) takes
  every cell under it, so put scene detail beside it, not under.
  v0.55.0 (user 13 Oktober 2026): juggernaut / elite rifles 6 in 10 (fixes/NpcLoadouts AUTO_RIFLE,
  MARKSMAN, rifleAttachments: none 1 in 2, else full or half a set); NEVER a vanilla door in anything
  we generate (worldgen/Doors swaps them in Slices and the small placer, style by building); city edge
  worn ground (EdgePlan.wear; dev test `cityedge`); wasteland buildings locked by loot
  (BuildingDress.lockDoors: Door_Emergency_1_Locked + military keycard screen; BuildingLayout.outdoors
  finds entrances by a flood fill from the plot edge). Crate levels idea: docs/ideas.md item 17.
  v0.56.0: wasteland city apart (new worlds, `!wastelandCity`: LcCity.isCity cuts a ring round wasteland
  districts) with WastelandFence (chain link, wire, landmines via Deci.newLandmine, gates); bunker
  blockhouse + worldgen/Garrisons (posts manned on first visit, spawner infected in military outfit;
  StructureData int lists); StreetPlan.rampAt replaces DC stairs parts; RiverPlan.damage; Rails.entrance
  (station stairs in the sidewalk).
  v0.57.0 (13 Oktober 2026): master plan steps 0 and 1 (docs/systems_plan.md): loot spikes (docs/loot_levels.md section 7): fixes/Loot.open
  shows an InventoryBasic of ours on Decimation's loot screen (Deci.setLootInventory, openLootGui),
  fixes/LootBody entity (deciworldgen:loot_body) opens it on a right click; dev test `loot`. Dev test
  trap: DevAutoTest closes every screen each tick unless `DevAutoTest.keepScreen` is set. Shared core:
  fixes/WorldClock, fixes/LootPools (config/deciworldgen_loot.json), fixes/Protection
  (config/deciworldgen_protection.json), worldgen/Areas (<world>/deciworldgen_areas.json); dev tests
  `core`, `items` (item dump -> docs/references/decimation_items.tsv). PatchLootRolls (three jars):
  Decimation's loot counts 1 .. n, not 1 .. n-1.
  v0.57.1 (user review of the v0.56 ring, docs/wasteland.md): a wasteland city is a whole city sector
  (all 4 districts) inside its city, streets joined, fence and gates on its edge (WastelandFence.inCity
  via StreetPlan, lineOnLand), no refuge in it; new worlds only (StructureData `!wastelandSector`).
  tools/topdown_blocks.py: top down block picture of a world from its region files.
  v0.57.2: PatchNoUpdate (three jars): no forced "Update" for players without DecimationVoiceChat.jar
  (bug.md); dev test `menu`.
  v0.58.0 (user: one jar with everything): the RELEASE is ONE Decimation.jar built by
  `python3 tools/onejar.py VERSION` (dist/Decimation.jar + dev/build/libs/deciworldgen-VERSION.jar +
  dist/config/decimation_worldgen as assets/deciworldgen/content/) -> dist/onejar/Decimation.jar;
  worldgen/BundledContent unpacks the content on start. Test a release jar on the real Forge server
  in the scratchpad style: Forge installer --installServer, only that jar in mods/ (never Prism).
  ScopeZoom copies only the glass box while aiming / zooming (weak laptop lag). Dev tests `click`
  (left button), `hitbox` (big infected boxes). The dev workspace is unchanged (two mods: dev/libs base
  jar + our classes).
  v0.59.0: auto update from our GitHub releases (new_feature.md; fixes/Updater, UpdateScreen, UpdateSwap,
  PatchNoUpdate v2). RELEASE RULES: the asset is named exactly Decimation.jar (the one jar), the tag a
  dotted version above the last; players see "Update" in Decimation's menu. Dev test `update` needs
  `python3 tools/fake_release_server.py` running.
  v0.59.1: update check survives GitHub's API rate limit (ETag cache, a 304 is free; fallback to the
  release feed releases.atom + plain download links). RELEASE RULE added: upload Decimation.jar.sha256
  (written by tools/onejar.py) beside Decimation.jar. Fake server flag LIMITED (API 403).
  v0.60.0: cities of 4 to 64 districts (worldgen/CityLayout, docs/city_engine.md "City sizes"): cells of
  5 x 5 sectors, one compact city each, 1 wasteland sector per 4 beside a military sector; new worlds only
  (`!citySizes`, ON by default: a new world's spawn search runs before WorldEvent.Load). Caches of sector
  answers must carry CityLayout.epoch(). tools/hwmap.py and edgescan.py follow the sized layout since 17 Oktober 2026
  (tools/citylayout.py, a bit exact port checked against Java; --old-layout for older worlds).
  v0.60.2: refuges in sized cities 1 per 4 city sectors (round, at least 1, spread apart, never wasteland;
  LcCity.refugeSectors; user 13 Oktober 2026). Releases are batched (user): no upload per small version.
  v0.61.0: wasteland gates in the inside fence (docs/wasteland.md "v0.61.0"): military/CityGate looks x states
  (8 in 10 shut: wire + landmines, steel, bus, tank ditch; 2 in 10 broken open), WastelandFence.gate owns the
  cells, StreetPlan sinks a ditch chunk's base, Slices spawns CityGate.MINE markers as landmines; our block
  deciworldgen:boom_barrier (skill decimation-block); the inside fence stands on full blocks (StreetPlan.floor)
  and runs over bridges. Dev test `gates`.
  v0.61.1 (user review): no tank ditch (HESCO wall instead), compact toll gate where the street before a connector
  cannot take a full one (gates on bridge decks too: no plain connector), T junctions at every city edge
  (StreetPlan.arms: our own streets close sides with no street or highway beyond), edge gates of the land fence
  closed with the same closures (WastelandFence.landGate, 1 in 20 a broken gap, no road).
  Gate checks: `servertest.py SEED type=decimation gatescan=x,z,r` logs every connector and edge gate around a point
  without generating (then pregen only around a hit); dev test `gatewalk` pushes infected down every lane of the toll
  and compact toll in each state (shut 0 of 10 through, open > 0; run at night, infected burn by day).
  v0.62.0: military airports in wasteland cities (docs/airport.md): city/Airport picks one half wasteland sector per
  city (1 in 3) as a block of 2 x 4 cells (LcCity.block kind 3), military/Airport builds it (runway, taxiway, apron,
  block-built planes, hangars, tower, locked HQ and armory, barracks, fuel farm, damage, garrison); new worlds
  (`!airports`). Dev test `airport`; gatescan also lists airport sites (and city refuges since 17 Oktober 2026). No lakes / springs over cities now.
  v0.62.1: the airport redone through the decimation-military-base skill after "to basic" (docs/airport.md section 9):
  module reserves, runway lighting / markings / PAPI / arresting gear, revetments, shelters, fire station, radar,
  igloos, FOB style entry; checked on the server (propclash 0 on 248 props, walkcheck 0).
  v0.62.2: sky shots of the rework (docs/airport.md section 10): UAHD flags (not USSR), green blast door leaves, chain
  link radar plot, road dashes turned with the plan (AirportPlan.blockAt: BlockRoad_CenterLine 4 along z, 2 along x),
  camera points that faced walls moved. Read every shot of a sky test: a blank wall or dark frame is a bad camera.
  v0.62.3: airport fighters F-15 size (19 x 13 x 6, Airport.fighter), revetments in one shared wall row 15 wide inside.
  v0.62.4: NPC tracers stop where the shot stopped (bug.md "NPC tracers fly through buildings"): PatchTracer v3 (three
  jars, deci/aE/a$B$a + deci/n/d$a; fourth argument lwjgl_util 2.9.4 from the gradle cache), NpcShots sends the shot's
  end point; dev test `tracer` checks tracer lengths.
  v0.62.5: misses' tracers fly on to 250 again (user), still stopped by buildings past the shot range (NpcShots.missEnd,
  loaded chunks only); no jar patch.
  v0.62.6: call logger (docs/call_trace.md, tools/trace): dev only Java agent marks every method of the chosen packages,
  /deciworldgen trace start PREFIXES [s] | stop, live runs take trace=PREFIXES; tools/trace/tracelog.py names the trace.
  Never run fps tests in a game started with the agent.
  v0.62.7: Decimation code study (docs/decimation_code/README.md; skills decimation-code and decimation-code-<feature>
  x 15): every non synthetic Decimation method named (deobf/maps/m_methods_rest.tsv); dev tests playerfire, drive,
  trade, useitems, keycard, and loot now clicks a real Decimation crate; tools/skillcheck.py (must pass for those docs
  and skills), tools/props/keycardcheck.py (keycard reach -3 .. +2). Found: backend reconnect leaks a thread every
  15 s, Decimation's daily world time reset stops our WorldClock day (bug.md, both fixed since).
  v0.62.8: PatchBackend v2 (three jars): Decimation never contacts BoehMod's network (connect and both reconnects
  do nothing); 0 kryonet threads after 2 minutes, menu / stats / zombies / checks and the dedicated server fine.
  Website plan on the roadmap (UI 2): download page and a community server list read over HTTPS, not kryonet.
  v0.62.9: WorldClock day fixed (bug.md): a saved counter (fixes/WorldClock.Data, <world>/data/deciworldgen_clock.dat;
  older worlds start at total ticks / 24000) counts each midnight the raw time passes; Decimation's reset to 0 counts
  nothing. Never take days from getWorldTime() / 24000: use WorldClock.day(). Dev test core checks two midnights.
  v0.63.0 (17 Oktober 2026, docs/main_menu.md): the main menu with Decimation's OWN parts (user: "just use component
  from decimation gui menu its self"): four of its news panels (ArticlePanel) with our GitHub, Updates (fixes/Updater
  state), Website and Coming soon (fixes/MenuNews, Forge InitGuiEvent, Deci.addNewsPanel); its background drawing
  shows six real scenes of our world, a new one every 7 s with a fade (fixes/MenuBackground), the black layer and the
  lava cube planet removed (tools/patches/PatchMenuBackground.java, three jars); the pictures go into Decimation's
  menu textures in the three jars by tools/menu_assets.py (never committed). Dev test `mainmenu`; run it LAST in a
  live game (it leaves the world: tests waiting for a player then hang). Dev test `guideshots`: Minecraft screens
  for the website's guides (docs/website.md 3b, 6).
  v0.64.0 (17 Oktober 2026, docs/main_menu.md 2b): menu links Website / GitHub, our name and version where Decimation
  showed its own (title line, under the logo, welcome box, window title; Decimation's mod version stays 1.21.10f),
  Continue under Play (fixes/MenuContinue), our corner instead of the FURY store advert; tools/patches/
  PatchMenuTexts.java (ASM, 20 texts in 10 classes, three jars), fixes/MenuTexts; news pictures are real topic shots.
  v0.64.1 (17 Oktober 2026, docs/main_menu.md 2c): the official list shows Bukovka and Chernarus "Coming soon" (fixes/
  MenuServers, PatchOfficialServers; rows on *.deciremnant.invalid hosts: pings fail at once, no network), the user's
  Chernarus pictures behind the page on hover (menu_assets.py, serverbackgrounds/chernarus0..7), Players Online and
  the Community tab "coming soon", status rows Global Server / Community Server / Download Offline, "My Supporter" is
  "Early access"; dev test `menupages` (run last). Check class texts by exact old strings: "Supporter" also matches
  the field name isSupporter.
  v0.64.2 (17 Oktober 2026, bug.md "Entity is already tracked"): Decimation's mob swaps (PlayerJoinSync: bloater /
  hulk / mech / frail) spawned during a chunk load were added twice and updated twice a tick (double speed); fixes/
  JoinOnce cancels a join of an entity the world already holds. Dev test `doublejoin`; dev flag -Ptrackdebug
  (devtest/TrackDebug) logs any entity that joins a world twice with both stacks (FML log, not latest.log).
  v0.64.3 (17 Oktober 2026, docs/airport.md section 12, bug.md): garrisons man each post once its own chunk is loaded
  (the airport's far posts stayed empty: their chunks were not loaded at the first visit), guards keep a 4 block home
  area at their post (entity data deciworldgen_post, set again on load). Dev test `garrison` (fresh seed 1 world, a
  real airport, creative player).
  v0.64.4 (17 Oktober 2026, bug.md, docs/wasteland.md): the checkpoint gate's sandbag position one row off the line
  (with HESCO it made a stair over the shut line: concertina wire on a fence top does not stop walking); dev test
  `gatewalk` covers every look and asks the path finder for a way through (open: yes, shut: no).
  v0.64.5 (17 Oktober 2026): garrison guards walk back to their posts after a chase (Garrisons.backToPosts; the home
  area alone bounds only idle wandering); dev test `garrison` moves a guard 12 blocks off and expects it back.
  v0.64.6 (17 Oktober 2026, code review by a second agent, bug.md): backToPosts walks a COPY of loadedEntityList (a
  path search loads chunks, which adds entities: crash risk); each garrison post waits until the chunks around it
  exist; refused spawns retried 5 times; the checkpoint's flag was outside its canvas (Canvas.set drops those
  silently): moved, and CityGate now logs props outside the canvas. Never iterate loadedEntityList while calling
  anything that can load chunks (path searches, getChunkFromChunkCoords).
  v0.64.7 (17 Oktober 2026, local): the in-game updater finds the running jar (bug.md: every shipped game since 0.59
  read itself as a dev folder, so players of 0.59 .. 0.64.6 must install the next release by hand once); toasts
  titled DeciRemnant, the status page's Website row pings deciremnant.vercel.app (PatchMenuTexts, 51 texts in 13
  classes, a text found at least once per class). Test `update jar=true` (production client) swaps the real jar.
  The update runs on Decimation's OWN progress screen (user; fixes/UpdateProgress fills UpdateChecker's fields, Quit game
  / Later drawn over its black layer; UpdateScreen only on failure); our DeciRemnant logo in banner.png and
  banner_intro.png (tools/menu_assets.py logos(), twice Decimation's size, three jars); docs/main_menu.md 2d.
  v0.64.8 (17 Oktober 2026): update screen lines as Decimation's updater wrote them (version and size, "Downloading:
  33%" then "Complete!", done/total MB; docs/main_menu.md 2d). Test trap: the open live world has weather; in rain or
  a storm World.isDaytime() is false at noon, so day / night checks must clear the weather (ZombieTest does now).
  Player hosting spike (18 Oktober 2026, docs/player_hosting.md section 6): dev test `lanhost` opens the live world to
  LAN (online=true|false) and checks a second player who joins through tools/hostspike (relay + host agent + the
  production client as joiner). A relayed world plays unchanged; same mods needed (gvc refused); HUD crash when joining
  without Decimation's menu (bug.md). Trap: the dev client's debug port is 5005, give a second game 5006.
  Joiner side: dev test `lanjoin` in the production client (one jar of the current code, -Ddeciworldgen.autotest.liveport=
  25598, `devtest.py --live --port=25598 --results=FILE lanjoin`) with the host's `lanhost scene=true`; both pass
  `bus=DIR` (devtest/LanBus files). Decimation vehicles are entered by right clicking a SEAT entity (deci.ad.c).
  Player hosting part 1 (18 Oktober 2026, spec docs/superpowers/specs/2026-10-18-player-hosting-part1-design.md, plan and
  ledger beside it): fixes/hosting (HostService, JoinService, RelaySocket on Minecraft's netty with TLS 1.2 ONLY: 1.3 never
  finished with Cloudflare, RelayClient, Pipe, LoginName gate, ModSet, HostScreen, HostMenu, CommunityList) and relay/
  (Cloudflare Worker + Durable Objects Lobby / Room, vitest; deployed on the user's account as
  deciremnant-relay.deciremnant-relay.workers.dev, `cd relay && npx wrangler deploy`). Dev tests hostmenu, lanhost
  real=true scene=true, lanjoin viaList=true (docs/player_hosting.md 9 to 11). Traps: never run Gradle (build OR test)
  while the live dev game runs (it recompiles the classes the game loads: NoClassDefFoundError); a commit by anyone
  changes the build version and host and joiner then refuse each other; Decimation's row object: getMotd() is the shown
  name, getName() the id; a row's Join needs FMLClientHandler.setupServerList() first (it crashed without).
  Commands while hosting: the world is shared with cheats off; HostOps ops the host only (ops.json, marker
  deciworldgen_hostop.txt), dev test `hostops`; the dev client gives everyone commands (DecimationWorldGen), so a test of
  "no commands" must turn that off (docs/player_hosting.md 12).
  Voice chat (gvc) is optional for joiners: fixes/hosting/OptionalMods wraps Forge's NetworkModHolder checker in postInit
  (Forge refuses a client without a mod the server has), ModSet.OPTIONAL in our own check (bug.md).
  Decimation unbinds the right mouse button while a gun is held (right click only aims): fixes/GunUseKey binds it again
  over a corpse or loot body (bug.md). Tests without stray mobs: `/gamerule doMobSpawning false` (also stops our
  spawners: fixes/NaturalSpawns; dev test `nospawn`).
  Direct joins over IPv6 (docs/player_hosting.md 15): fixes/hosting/Direct (host gate + joiner probe), relay route
  "direct" and POST /list/direct; Host Online "Direct + relay" (default) / "Relay only". Two game tests on one Mac need
  `directloopback=true` and a local relay `wrangler dev --var DIRECT_LOOPBACK:true`; the scratchpad twogame scripts stop
  both games at the end (a left open game plus a Gradle run broke one run).
  Stability (docs/player_hosting.md 16): relay sockets ping every 25 s, the room waits 20 s for a lost host control socket,
  the host reconnects to the same room, rooms placed near the host, far sockets retried (host overlap: the relay replaces a
  data socket and the far one's messages carry over). A local relay can fake far landings: `wrangler dev --var
  FAKE_COLOS:FRA,SIN --var FAKE_REGION:apac-se`. The dev host game rebuilds on a commit: build the joiner jar after it.
  Stability and FPS (19 Oktober 2026, bug.md, docs/performance.md 7; first brother laptop session): fixes/GlStackGuard
  (Decimation's melee / throwable item renderers and its name tag handler left GL matrices pushed: wrapped / popped, and
  each frame ends with the stacks as deep as it began), fixes/PropSounds (radio and other prop loops stop out of hearing,
  6 at most), fixes/EmptyStacks (throwables left a stack of 0 that threw forever), PatchIntroThread, PatchNoVersionFetch.
  Dev tests glstack, throwables, radios, threads, soak; tools/glstackcheck.py (GL push / pop paths from the bytecode).
  Trap: a server player's PlayerTickEvent runs only when that client's movement packet is processed: per player server
  work goes on the server tick.
  NPCs under load (19 Oktober 2026, docs/performance.md 8): dev test bench (standby, shooting, war, zombies, crowd; mob
  spawning off, two runs), traderperf, targetpace, zoneperf, scopeperf, scopediag, armskin; fixes/TargetPacing (vanilla
  target tasks of Decimation's mobs at targetChance 5), fixes/GunArmSkin (the gun hand shows the player's skin), GunNoise
  skips mobs already following a nearby shot, ScopeZoom measures the glass only while aiming. Traps: live key=value
  settings stay set for later modes (clear them with key=); vanilla never targets a player with disableDamage; the AI
  evaluates target tasks every 3rd tick (targetChance N: a search every 3N ticks); compare client fps only on a quiet Mac.
  NPC looks in one picture (19 Oktober 2026, bug.md "Drawing many human NPCs", docs/performance.md 8): fixes/HumanLayers
  + PatchHumanLayers; dev tests npclook (both ways in one frame, pixels compared; `kinds=`), npcdraw (NPC draw time a frame,
  both ways in turns); bench `layers=off` draws Decimation's way. Gear now covers hair and beard, sleeves stay on an arm
  holding an item (both were bugs of the old layer copies). Next: guns and gear, about 360 us of an armed NPC's 423.
  Muzzle flash lighting (19 Oktober 2026, bug.md): fixes/EntityLighting (lighting back on around each living entity),
  PatchLightLeaks (NPC goggles); dev test lightleak. Trap: Decimation's disableItemLighting is never paired with an
  enable; anything that draws after a Decimation glow must not trust the lighting state.
  OptiFine (19 Oktober 2026, bug.md "Aiming with OptiFine"): it posts RenderHandEvent INSIDE renderHand; code that cancels
  the hand event and calls renderHand itself must ignore its own re-entry (ScopeZoom.drawingHand). Test hand and scope
  changes in the production client with OptiFine too (docs/production_test.md); scopeperf checks GL errors per frame and
  the glass after aiming (no frozen picture: ScopeZoom wipes it to LENS).
  Model parts in one draw (19 Oktober 2026, bug.md "Drawing many human NPCs", docs/performance.md 8): fixes/ModelBatch +
  PatchModelBatch; dev tests gunbatch (npclook's comparison for parts), npcdraw `scenes=camp,horde,fight` (old way =
  Decimation's whole drawing). Trap: BModelPart.render's children list is compiled once (frozen poses); a capture runs at
  that same moment, so both ways hold the same geometry.
  HUD styles and infected wounds (22 Oktober 2026, new_feature.md "HUD styles", bug.md "Hits on infected leave nothing"):
  fixes/HudStyle (Decimation default / Inline; key J, Settings page button, config deciworldgen_hud.cfg; holds IngameHud's
  ClientState cf / cl while inline), fixes/InlineHud (Minecraft layout over the hotbar, humanity on the experience bar),
  fixes/HudDoll (the player top right: Decimation's toasts and tips own the top left, vanilla's tutorial / achievement
  toasts the top 32 px); fixes/Wounds (splats painted into each hit infected's own look picture via HumanLayers). Dev
  tests hud, wounds. Traps: the local player's posY is eye level (use getPosition(1.0f) for its view ray); a dev test
  that acts on `ticks == N` misses ticks (the client tick can skip server ticks): use steps with a wait counter.
  World generation stalls (20 Oktober 2026, bug.md "Server stalls while new city areas generate", docs/performance.md 9):
  terrain/DeciBiome.decorate runs vanilla decoration's block updates at once (its gravel and sand queued 188,721 ticks
  that every chunk save walked); worldgen/BlockBatch writes Slices' blocks without per block light and lights the
  written columns once (World.setBlock in a loop re-lit half built rooms with every column: 93 % of a structure write);
  worldgen/Keys.pair for HashMap keys of two ints; Highways.sides asks the highway flags once a chunk, not once a column.
  Tools: servertest `pregen=` PREGEN lines (population, writes wall and CPU, saving, queued ticks, slowest chunks, light
  check), `walk=x,z,steps,view` (stall per chunk crossed), `bulklight=false` (old way), `jfr`; tools/genprof.py; dev
  test genwalk (flight over new city land in a fresh seed 1 world, same path every run; `-Pbulklight=false` for the old
  way); worlddiff `--at X,Z`. Traps: bulk block writes go through BlockBatch (set, then light()), never World.setBlock in a
  loop; a Long key packed as (a << 32) ^ b hashes to a ^ b (bins turn into trees): Keys.pair; `jfr print` keeps 5 frames
  a stack unless --stack-depth; AnvilChunkLoader.chunkExists CREATES a missing region file (check the file first).
  First sight of NPCs (20 Oktober 2026, bug.md "First sight of armed NPCs stalls a frame", docs/performance.md 10): a
  city refuge's 22 residents spawn in 3 to 10 ms on the server, but the client's first frame with them took up to 941 ms
  in the production client (textures read at their first bind, gear models Decimation loads only at a first draw, part
  compiles). fixes/ModelWarmup loads the gear models (Deci.loadGearModel) and the model textures ahead, a slice per frame
  (8 ms in the menus, 2 ms in a world; done in about 1.6 s): 80 ms there now. Dev test refugevisit (one launch: fresh seed
  1 world, two refuges, SafezoneSites.hold while the land is made, per frame probe, check "no Decimation texture loaded at
  arrival"); dev switches -Pwarmup=false, -Pmodelbatch=false, -Phumanlayers=false. Traps: never compile all models ahead
  (Decimation's display lists cost about 9 KB of driver memory a quad: 1.2 to 2.6 GB of graphics memory, vmmap -summary
  shows it as IOAccelerator); JFR barely samples the client thread inside a GL heavy frame (6 samples in 200 ms), count
  what each frame loads instead; a model's display list bakes the scale and the scaleO / scaleN / jE flags of its first
  draw (sight glass parts get theirs just before drawing).
  Two lines apart (20 Oktober 2026, "TWO LINES SIDE BY SIDE" above): only release builds look for updates (fixes/Updater
  release / offersUpdates, State OFF; MenuNews "Updates off: dev build (main)"; MenuTexts "<tag> dev"; unit tests
  UpdaterTest, MenuTextsTest; checks menu "update check", mainmenu "updates panel line"); each line's live ports
  (tools/lineports.py); tools/linecheck.py for release step 1; the worktree's missing schematics. Traps: a live game that
  ran mainmenu has left its world, so a later mode waits for a player forever and --stop then times out with a
  traceback (end that game at the menu: its world is saved); macOS has no `timeout` command.
  Main menu (20 and 21 Oktober 2026, docs/main_menu.md 2g, project skill decimation-main-menu, map in section 8):
  updates are the player's choice (Updater.required false: Play and Resume Game are never replaced; fixes/UpdatePrompt
  asks with Decimation's ConfirmScreen on the title menu, or the Esc menu in a world, once a launch; Quit game leaves a
  world and lets it save first (fixes/SafeQuit); Download page where the jar cannot be swapped;
  UpdateScreen Download page on a failure), Supporter back (PatchMenuTexts), Server Status rows from real sources
  (fixes/MenuStatus: official list, relay on main only, GitHub release link). The dev test `update` leaves the world
  now: run it after modes that need a player. Trap: the user's own dev client may be open in main: check pgrep BEFORE
  any Gradle there; test meanwhile in another checkout (`git worktree add ../Decimation-scratch --detach HEAD`, dev/libs
  linked to main's, files copied over), which has its own build folder and run folder.
  Main menu 2h (21 Oktober 2026, docs/main_menu.md 2h): news panel popups (fixes/MenuDetail, MenuDetails), Discord in
  the links (the user's invite), our footer (BoehMod credited on the website only), corner art of our own from a real
  NPC render (dev test cornerart, menu_assets.py corner(), fixes/MenuCorner). menu_assets.py now also writes the footer
  logo, the popup pictures and the corner from docs/shots sources: run it after changing any of them. Trap (the user's
  crash, bug.md): a click on Decimation's panels runs inside DeciScreen.mouseClicked's loop over them: never change
  screens or re-init the menu there (next tick), and test clicks the way a mouse makes them (DevTestUtil.clickAt).
  Gunsmith screen (21 Oktober 2026, docs/decimation_code/weapons.md section 7, spec and plan in docs/superpowers): the I key
  (Decimation's ATTACH) opens fixes/gunsmith instead of its crafting style AttachmentScreen (GunsmithGuiHandler wraps
  Decimation's IGuiHandler; no jar patch): the held gun in 3D (its own renderer, outline from the depth buffer once per
  change), six slots joined to their parts, click to fit from the whole inventory by Decimation's FilteredSlot rules,
  the gun never leaves the hand; a left drag on empty space turns it, the wheel zooms (spec section 9). Dev tests
  gunsmith (110 checks), gunsmith perf=true, gunsmithmp (a joined client on the
  production server: /give in creative, a dedicated server's Decimation lets only creative players pick items up).
  Traps: a container button id (enchantItem) is ONE signed byte on the wire, singleplayer passes the int whole (bug.md);
  GuiContainer draws every slot's item each frame even off screen (hide unseen slots on the client); cancelling
  RenderGameOverlayEvent ALL also skips the world GUI view set up (call setupOverlayRendering first); live key=value
  settings such as perf=true stay set (clear with perf=); the Minecraft alpha test drops anything under 10 % opacity.
  A 3D model drawn in a GUI needs ONE mirror (the GUI's glOrtho flips y): without it Decimation's renderer draws it
  inside out, hidden parts showing (the user saw it; check the sign of det(projection x model view), negative is right).
  A container's canInteractWith runs once a tick in the player's update, AFTER that tick's packets: guard slotClick
  yourself when a slot is a view of the held stack (a swap and a click in one tick copied parts); vanilla's
  onContainerClosed drops the cursor and a logout saves the player with the screen open and the cursor unsaved
  (GunsmithContainer.Logout returns it on PlayerLoggedOutEvent, which FML posts before the save).
  New attachments (21 Oktober 2026, docs/decimation_code/weapons.md section 8, plan and ledger in docs/superpowers): the
  RK-1 Foregrip (deci:rk1, fixes/NewGrips, Deci.newGripAttachment) and the ELCAN SpecterDR 1x/4x (deci:specterdr; G flips
  it per gun: fixes/SpecterMode, fixes/SpecterKey; ScopeZoom: our sights never get the black overlay); recoil per grip
  (GripRecoil, PatchGripRecoil); every grip placed per gun (tools/guns/gripfit.py, rerun it after a new gun or grip like
  sightfit; fixes/GripPlacement) and drawn from one display list a copy (Deci.BatchedPart). Dev tests attachments (22),
  attachments perf=true (7), attachmentsmp (12, dedicated server), glstack now with every sight and grip. Traps:
  Decimation's BModelLoader drops children of a parent the file never declares (foregrip.bmodel's magazine is never
  drawn; study.py does the same now); a display list must never compile inside another (BatchedPart lets each part
  compile on a first draw, then compiles its own); BModel copies of attachments are per gun (AttachmentItem.ST) and
  must be changed before their first draw; compare aimed frame times only between sights of the same zoom (a 1.25x view
  draws about 4.5 ms more world than a 3.5x one) and look at the sky, in rounds with paired differences (single readings
  were noise: the same path read -0.61 ms); an aimed bench looking down from the arena (y 150) sees the land below; a
  geometry check must judge the placed result on its own and never reuse the fit's measure (the first gripfit check
  passed by construction while 37 grips floated: final review, bug.md).
  User review the same evening (docs/references/grip_rails.md): RK-1 v2 sized to Decimation's Foregrip (the baseline for
  every grip of ours) and rolled 45 degrees to the gun's left (+z), as the reference's end and bottom views show; a
  Foregrip or RK-1 only on the 19 rifles with a rail under the handguard (gripfit.py RAILED -> grip_offsets.txt ->
  fixes/GripRails: Gunsmith, NPC loadouts; fixes/MountStrip pops one off a rail-less gun, never while the Gunsmith is
  open). Trap: read every view of a reference (end and bottom too): a side view hid the RK-1's 45 degree roll.
  Our own network channel since 22 Oktober 2026: fixes/Net ("deciworldgen", SimpleNetworkWrapper); message ids fixed,
  new ones at the end (0: MagUnload, client to server; 1: RadiationDose.Sync, server to client; 32: GasMaskFilter.FitMessage,
  client to server: the feature agent's ids start at 32). Before, everything rode
  on Decimation's packets, data watchers and container buttons.
  Radiation dose on the HUD (22 Oktober 2026, new_feature.md): fixes/RadiationDose (server, per player an
  IExtendedEntityProperties, saved with the player) and fixes/RadiationHud (a fifth icon in Decimation's status column,
  our pixel icon from tools/hud_icons.py); dev tests radiation, channelmp (dedicated server: both channel messages).
  Trap: Decimation's status column (IngameHud) moves down 10 a stats line and runs off the bottom of a short GUI (1080p at
  GUI scale auto is 270 high): anything placed in it needs a fallback (docs/decimation_code/menus.md 3b).
  No Steve (22 Oktober 2026, new_feature.md feature 5b): fixes/PlayerSkins gives skinless players one of Decimation's 21
  human skins, picked by NAME (an offline server knows a client by another id than the client itself); corpses wear
  their player's skin through alias textures under "skins/<id>". Trap: never deleteTexture such a name (it frees the
  player's skin); dev test skins (the dev account has a skin of its own, the production test account none).
  DayZ dark nights (22 Oktober 2026, feature 6): fixes/NightLight + DarkNights rebuild the light map on Forge's FogColors
  event once a tick; fixes/MoonClock gives the overworld real moon phases (MoonProvider, our day counter, Net message 2):
  Decimation's DayLengthHandler resets world time every day, so vanilla's moon was full every night. Test trap: weather
  changed on the server outside its weather tick never reaches the client; set it on both sides (dev test nights).
  Durability (22 Oktober 2026, feature 10): fixes/Durability, ArmorGunfireHandler (wear, used up pieces, vests protect
  now), GunWear + GunWearClient (jams, R racks, Net message 3), RepairPart (three part items, Deci.addLootShare);
  tools/patches/PatchGunWear.java on GunItem (three jars, on top of PatchGripRecoil): its calls catch
  NoClassDefFoundError because the base jar is shared by every checkout. Trap: Decimation's reload key field is named
  "do", a Java keyword (Deci.reloadKey reads it by reflection). Dev test durability.
  Durability v2 (23 Oktober 2026, user): native bars on gun and gas mask icons (PatchDurabilityBars, fixes/DurabilityBars),
  the gun's durability line back in its tooltip, vests worn by every shot and every zombie hit, the radiation percent
  centred (RadiationHud.textX), NPC tracers Decimation's GENERIC near white on every shot (NpcLoadouts.withTracer).
  Traps: Decimation's ClientEventHandler.onItemTooltip clears the tooltip of guns, magazines and food at NORMAL priority
  and registers after us, so our lines for those items go in at EventPriority.LOW; the harness closes client screens
  with displayGuiScreen(null), which never tells the server: DevAutoTest now closes a container a mode left open on the
  server and records "server screen left open" (radset leaves Decimation's gear container), and clears the player's fall
  distance (genwalk flies in creative: the next mode that switched to survival died on landing; DevTestArena.build
  clears it too); a test that sets the held slot on the server must select it on the client too
  (mc.thePlayer.inventory.currentItem), the saved world may hold another. Decimation's HUD percents (food, water, blood,
  health) are centred by PatchHudPercents + fixes/HudPercents.
  Test trap (same day): a mode that switches the player to survival must put the game mode back (the world saves it);
  zombies, which assumed creative, had its player killed by a hulk and read nothing. Set weather strengths too
  (setRainStrength), not only the raining flag: isDaytime() counts the fading rain.
  Radiation set (22 Oktober 2026, feature 7, branch bugfix, new_feature.md): fixes/RadiationPills (deciworldgen:
  radiation_pills, used like Decimation's Pain Killers, 40 dose points off), fixes/GasMaskFilter (filter life in the worn
  mask's tag, wears only in radiation, a right click fits Decimation's Gas Mask Filter through message 32;
  Deci.wearsGasMask needs filter left; an empty filter also lets Decimation's tear gas poison through, whose own check
  runs on the client; loot rates per share of opens, copiesFor), city/WastelandRadiation (RADIATION over every wasteland city, older worlds at
  load; two zone kinds on one place both apply); dev test radset. Traps: Decimation's gear screen holds the worn mask
  stack itself, and a changed tag sent to that slot clears the client's cursor (write it only with no screen open); a
  held use key takes the next item of a stack right after a use ends (a test holds one in the hand); per thread
  allocation read right after a path first runs is JIT warm up (warm up first, take the steadiest batch).
  Loot rules A (22 Oktober 2026, feature 12, new_feature.md): fixes/LootRefill keeps looted spots with the world (record
  deciworldgen_looted, <world>/deciworldgen_loot.cfg: refill off in singleplayer, on with Decimation's 15 minutes on a
  dedicated server) and sets Decimation's own mark of a clicked spot before its handler (Deci.setLootCooldown, lootSpot);
  dev test lootrules (a real save and reload). Traps: Decimation's cooldown map is static and its clear at server start
  never runs (an @Mod.EventHandler method registered on the event buses), so it outlives a world reload in one game;
  tests reset loot with LootRefill.clearAll(); leaving a world in a test needs a screen right after mc.loadWorld(null)
  (GuiMainMenu), or the next client tick crashes.
  Mobs by place (22 Oktober 2026, feature 8, branch bugfix, new_feature.md): fixes/PlaceSpawns (table in
  config/deciworldgen_places.json) turns Decimation's natural plain infected spawns into the place's look or mobs;
  worldgen/Places names the place (airport, base, police station, hospital, highway, wasteland, forest) from the seed and
  our named places, memoised per chunk; dev test placespawns. Traps: Forge's CheckSpawn fires for natural spawns only
  (SpawnerAnimals; spawner blocks, eggs and code never); a synthetic spawn check on the player's own spot fails
  getCanSpawnHere (the player collides), and EntityMob.getCanSpawnHere refuses everything in peaceful.
  Loot rules B (22 Oktober 2026, new_feature.md): fixes/LootBlocks, 15 copies of Decimation props of their own class
  (Deci.copyProp copies every BlockProp field) with loot entries of their own (Deci.addLootBlock: Decimation's entries copy
  their item lists, so additions reach each once); shelves and cabinets by building (Dress.lootKinds, CanvasPlan.lootPlace);
  dev test lootblocks compares pixels of original and copy at one spot. Traps: Block.getCreativeTabToDisplayOn is client
  only (a dedicated server strips it); ScreenShotHelper saves without a file extension; a live game started without
  --gunpacks on a world holding package guns stops on Forge's missing items screen (pass the same --gunpacks every start).
  Loot rules C (22 Oktober 2026, new_feature.md): fixes/LootSearch, slots revealed one by one by value on the server (Net
  message 4), Deci.GearContainer refuses clicks on unsearched slots; dev test lootsearch (timing in server ticks, costs as
  medians). Traps: another mod's tooltip lines come after ours, clear tooltips at LOWEST; a test that moves loot at once
  turns LootSearch.enabled off; per player server state shared by several viewers must tick once a server tick.
  Trader market (feature 11, new_feature.md): fixes/TraderMarket rewrites Decimation's static offer lists per class (a new
  trader object resets its class's list: re-apply on EntityJoinWorld), Net message 5 carries the day's table; trades are seen
  by our handler in Decimation's server channel after its codec (Deci.decimationChannel). Tests that trade read the price of
  the moment (TradeTest); dev test market.
  Sights need a mount too (22 Oktober 2026): sight_mounts.txt ("gun rail|side|slide") -> fixes/SightMounts; the red dot
  only on pistols with an optic cut or rail, other sights on a rail, claw mount or AK side mount; fixes/MountStrip (was
  GripStrip) pops off grips and sights a gun cannot mount. A new gun needs its row there.
  Found and FIXED: the see-through glass was never updated in the production client with OptiFine (bug.md): that client
  draws a sight's glass 6 times a frame (its HUD figure holds the gun), and PatchScope counts EVERY glass draw, so a
  "measure every Nth draw" mask never hit the hand's draw; ScopeZoom counts frames itself and sets the mask to 0 only
  around its own hand draw. Trap: never key per frame work to a counter that other draws of the same model advance;
  dev test scopediag (`extra=N` adds draws a frame) checks the glass gets measured.
  Scope aim delay (user, same day: "sometime it has delay"): profiled with dev test `aimdelay` (time to 95 % zoom, worst
  frame; first, learned and relaunched aims): no stall, the first aim of each gun and sight in every launch waited for
  ScopeZoom to learn the sight (ACOG 431 ms against 187). fixes/SightMemory keeps learned positions between launches
  (config/deciworldgen_sights.txt, per build). scopediag's default sight is the ACOG (Decimation's red dot has no
  measured glass draws).
  Gun packages (22 Oktober 2026, 3D gun converter game side, docs/gun_model_spec.md section 7): fixes/gunpack registers
  guns from the converter's folders (gun.json + assets/deci/...) at preInit with no Java line per gun: the like gun's
  stats, magazines, loot, sounds; attachment rows from "points" (GripFit is gripfit.py in Java). Dev: build/gunpacks is
  read in the dev workspace (tools/guns/gunpack_standin.py), more folders with -Pgunpacks / devtest.py --gunpacks=;
  shipped ones in assets/deciworldgen/gunpacks/ + index.txt. Traps: GunItem's constructor loads its model through the
  resource manager at once, so package files are served by a resource pack installed first (GunPackResources); BModel.parts
  holds top level parts only; FireMode.toString keeps the enum name; packages are read at start (restart the live game
  after a change). Dev test gunpack (perf=true); gun, playerfire, gunview, gunperf, gripsweep take the package gun's name.
  Mesh guns (23 Oktober 2026, converter milestone 4, docs/gunconv.md 13): the converter's default now keeps the source's
  triangles 1:1 (cap 30000) with a pixel texture in the like gun's tones (tools/gunconv/meshpack.py); a package names
  "mesh": "mesh.dmesh", its .bmodel is a skeleton of marker cubes and ModelBatch compiles each group's triangles in place
  of its cube (fixes/gunpack/MeshGun; no jar patch). Dev test meshgun (each group follows its part, poses set with
  Deci.nudgeGunPart). Traps: Decimation's SlideBack never holds (GunAnimation.get resets its flags every held tick); an
  animation left running resets the parts it names on any gun drawn (Deci.stopGunAnimation before posing); pass the same
  devtest.py --gunpacks on every call (a new game started without it drops those guns from the world); never hand a
  stack of a null item to a player (the client's next tick crashes: likeMagazine is null for "magazine": "like");
  the gunconv-game worktree's test world now holds akmm, ak47m, m590m, p228m and akmc: start it with
  --gunpacks=<main>/build/gunconv/meshtest.
  RELEASE 0.66.1 published 22 Oktober 2026 02:46 WIB (hotfix, pre-release from prerelease e4fc4ca; sha256 1a827037...,
  digest equal; only Updater and a test changed against 0.66.0): the updater names its Accept header. Checked: 0.66.0
  updated itself to it through the fake server; the running 0.66.1 downloaded the real 0.66.0 file from GitHub with its
  own updater (fake_release_server REMOTE=url: 240 MB in 29 s, checksum, swapped). Players on 0.66.0 and older install
  it by hand once (0.66.0 notes and the site's Updating guide say so). Trap: a fake release offered during other tests
  puts the update question on the Esc menu, which pauses singleplayer (server ticks stop: MountStrip's check failed).
  RELEASE 0.66.0 published 22 Oktober 2026 02:03 WIB (pre-release from prerelease 99813b0, tag 0.66.0; sha256 e24df810...,
  GitHub's digest equal; notes RELEASE_NOTES_0.66.0.md; jar kept in dist/previous/onejar-0.66.0): Gunsmith, new
  attachments, mount rules, menu changes. Public download and the site's Download (0.66.0 at 02:27) checked. The real
  update from GitHub FAILED: GitHub now answers Java's default Accept header with 406 on release downloads (bug.md
  "Update downloads fail with 406"), so no shipped updater (0.64.7 to 0.66.0) can download; fixed in Updater on main and
  prerelease (e4fc4ca) and released as 0.66.1 (below). The mod repository is now named deciremnant on GitHub (the old
  name redirects).
  Trap: the production client's test world saved by a newer jar makes an older jar stop on Forge's "missing
  blocks/items" screen (deci:rk1, deci:specterdr): move saves/deciworldgen_devtest aside before an update test.
  RELEASE 0.65.0 published 20 Oktober 2026 10:55 (pre-release, the first from the prerelease branch: tag 0.65.0 on
  14830b9; sha256 1c604032..., GitHub's asset digest equal; notes RELEASE_NOTES_0.65.0.md; jar kept in
  dist/previous/onejar-0.65.0). Checked after: public download 251,272,285 bytes, site Download 0.65.0 at 11:11 (ISR
  600 s); the production client with the real 0.64.8 jar updated itself from GitHub (239 MB in 28 s, checked, Quit game
  / Later, jar swapped, sha256 1c604032) and restarted as 0.65.0 ("up to date (0.65.0)", menu and mainmenu all PASS).
  PUBLISH A BIG JAR AS A DRAFT FIRST: `gh release create VERSION --draft --target SHA --prerelease --title VERSION
  --notes-file NOTES` (no files), `gh release upload VERSION Decimation.jar.sha256`, `caffeinate -i -s gh release
  upload VERSION Decimation.jar --clobber` again until it lands, compare the asset digest with the local sha256, then
  `gh api -X PATCH repos/ucilmenangis/decimation-singleplayer/releases/ID -F draft=false`. A draft is invisible to
  players and the updater. 0.65.0's upload broke twice (broken pipe after 21 minutes at about 217 KB/s; then the Mac
  slept on battery); on another network it took 23 s. gh deletes its own draft when `gh release create` with files fails.
  Trap: the update test clicks Update 40 frames after opening Decimation's home menu, but that menu ignores clicks until
  its welcome fade ends (ClientState.cg starts at 1.5, minus 0.01 a frame, clicks pass at 0.1: about 140 frames of the
  menu per game session). A game that went straight into a world FAILs "Update opens Decimation's progress screen" and
  waits 20 minutes; run `menu` (or any mode showing the menu) before `update` in a fresh game.
  RELEASE 0.64.8 published 17 Oktober 2026 16:53 (pre-release, sha256 73049bc7...; notes dist/onejar/RELEASE_NOTES_0.64.8.md;
  released jars kept in dist/previous/onejar-<version>): the production client with the real 0.64.7 jar updated itself
  to it from GitHub (239 MB in 29 s, checksum, jar swapped); site Download 0.64.8 at 16:54.
  RELEASE 0.64.7 published 17 Oktober 2026 15:33 (pre-release; asset digest = local sha256 ad4197ab...; notes
  dist/onejar/RELEASE_NOTES_0.64.7.md with the Chernarus credit): checked before on the production server and client
  (26 checks), after it the production client running 0.64.7 as "0.64.6" updated itself from GitHub (239 MB in 31 s,
  checksum, the running jar swapped). Releases stay pre-release until city generation is done (user).
  RELEASE 0.64.6 published 17 Oktober 2026 (GitHub pre-release, Decimation.jar + .sha256; GitHub's asset digest equals
  the local sha256 d6b8dfe2...; notes dist/onejar/RELEASE_NOTES_0.64.6.md): everything since 0.59.0.
  Gun study (10 Oktober 2026): docs/gun_style_guide.md, tools/guns/study.py
  (renders / measures Decimation's guns from the jar, `attach` adds
  attachments as the game places them), dev test mode `gunview` (hip, aim
  and NPC shot per gun, -Pguns=uzi,mac10 -Pattach=reddot,smgSuppressor),
  stats dataset docs/references/decimation_gun_stats.tsv. tools/guns/refcapture.py (21 Oktober 2026): 8 cut out
  views of a build on tarkovbot.eu's weapon builder from the user's preset JSON, as a modelling reference only (skill
  decimation-gun step 1, docs/ideas.md 26). Project skill
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
  What comes next: docs/roadmap.md.
  Decimation's own loot registry (`deci.aB.e`) is block-type-agnostic and will
  pick up its own crate/ammobox blocks in any structure regardless of who
  placed them, so loot inside our structures needs no new Decimation code —
  just place the right blocks in the schematics. Confirmed Decimation itself
  never touches world/biome generation (only an unrelated internal rendering
  stub, `deci.e.d`, implements `IChunkProvider`).
- Performance: investigated and fixed by profiling (docs/performance.md): the
  scope FPS drop (v0.28, bug.md), prop heavy views (PatchPropCulling, render
  distance by size, v0.38.2), infected path search (v0.38.1), and props used
  as building material (Decimation's BlockMilitaryBarrier: a tile entity per
  cell, replaced by deciworldgen:hesco in v0.42.1). Profile before optimising.
- Custom weapon models: `.bmodel` is PLAIN TEXT (Techne style model code,
  parsed line by line), fully documented 2026-10-07 in
  `docs/gun_model_spec.md` together with the `.anib` animation format, every
  asset path, the renderer and a new gun checklist. New meshes are built in
  code since v0.36.0 (tools/guns, skill decimation-gun), no Techne needed.
  (An older note here called it a binary format; that was wrong.)
