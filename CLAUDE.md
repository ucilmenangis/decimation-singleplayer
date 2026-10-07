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
   covers them (a pass over CLAUDE.md costs about 85k tokens).
3. Mark anything not verified in game or in code `[not verified]` /
   `[inferred]` in the doc itself.

Before starting work in an area, query the graph first
(`graphify query "<question>"`, or read `graphify-out/GRAPH_REPORT.md`) and
the matching `docs/` file, instead of re-deriving from source.

Knowledge index:
- `docs/gun_model_spec.md`: gun `.bmodel` / `.anib` formats, paths, renderer,
  new gun checklist.
- `docs/building_design.md`: researched floor plans (apartment / office /
  shop), palettes, decay, biome overgrowth, street and car facing rules.
- `docs/terrain.md`: the "Decimation" world type (biome map, biomes,
  spawn copy, sealed caves under cities, how to test).
- `docs/worldgen.md`: our structure generation (sectors, cells, sites, city
  blocks, large schematics, slice placement, placeholders, how to add
  community schematics, how to test).
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
- Confirmed 2026-10-07: dev client boots with deci + deciworldgen + gvc, Play
  works, user created a world and saw structures.
- Shipping: `./gradlew build` -> `build/libs/deciworldgen-<tag>.jar`
  (reobfuscated to SRG, version from the git tag). Deliverables in `dist/`:
  that jar + `Decimation.jar` (= patched jar minus our classes). Both are in
  Prism's `mods/` since 2026-10-07 (original in
  `minecraft/mods_backup_20261007/`). Spotless is disabled to keep our style.
- A Javassist patch to Decimation's own classes must go into THREE jars:
  `Decimation.jar.patched`, `dist/Decimation.jar`, `dev/libs/Decimation-base.jar`
  (then Prism's copy). Patch sources live in `tools/patches/` (so far:
  `PatchSwing.java`, SmoothSwingThread busy loop; `PatchPropCulling.java`,
  props not rendering, both 2026-10-07). Patch from the ORIGINAL classes
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

- `python3 tools/servertest.py SEED [keep]`: dev dedicated server, fresh
  world, waits for spawn generation, stops via console `stop`, prints our
  placement/zone log lines. Seed 1 places POLICE-zoned `city_street`s near
  spawn. Decimation's ServerProxy runs here, so original server handlers fire.
- `python3 tools/worldcheck.py WORLD column X Z [YMIN YMAX]` / `box ...`:
  reads block ids straight from Anvil region files. `World.registry()` maps
  block names to ids from level.dat (Decimation ids can be below 256:
  BlockWreckage1..5 = 176..180, so "id >= 256 means mod block" is wrong).
- `python3 tools/worldmap.py WORLD OUT.png`: top down biome / height map
  of a generated world (read the PNG to judge terrain). `servertest.py`
  takes `type=decimation` for the world type.
- `python3 tools/gradescan.py WORLD LOG`: graded city yards (floor vs ground
  at the walls, steep neighbour pairs, lot edge vs sidewalk, parked cars).
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
