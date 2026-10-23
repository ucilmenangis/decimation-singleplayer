# Porting Decimation to modern Minecraft: three paths (13 Oktober 2026)

PARKED. The user asked for this plan to keep for later: "we use that only if i have really
thought time and have finding skills to handle this reverse engineering skills". Nothing here
was tried. Facts about our own repo were counted in this session; facts about modern Minecraft
and other mods come from general knowledge and carry `[not verified]`; effort figures are
guesses and carry `[inferred]`. Check every marked line before acting on it.

## 1. What we start from (counted 13 Oktober 2026)

- Decimation: one closed source Forge 1.7.10 jar, 238 MB, 21587 entries, name obfuscated. No
  source. Our readable copy (`deobf/src`) names every class (965 explicit) and every method except compiler made lambdas
  (since 16 Oktober 2026); it is a reading aid, never shippable.
- Our own code: 148 Java files, 36484 lines (worldgen 30972, fixes 5512) in
  `dev/src/main/java/net/decimation`, plus 11 Javassist / ASM patch sources in `tools/patches/`.
  Our code is ours: it can be ported and published. Everything it touches of Decimation goes
  through `net.decimation.fixes.Deci`, so that one class lists what a port must replace.
- Studied content with tools that already read it outside the game:
  - 98 guns: `.bmodel` (plain text, Techne style shape boxes) and `.anib` animations,
    docs/gun_model_spec.md; `tools/guns/study.py` renders and measures them from the jar.
  - 192 props with verified facing (docs/references/prop_facing.tsv); `tools/props/propgeom.py`
    computes each drawn box from the renderer code.
  - DeceasedCraft's Lost Cities data (1.20.1, Lost Cities 1.20-7.4.11,
    docs/references/deceasedcraft_buildings.md) was already translated DOWN to 1.7.10 by
    `tools/lctranslate.py` / `tools/lcpack.py`: the block mapping between the two versions exists
    in one direction.

## 2. Why any path is a rewrite, not a port

- No source: every Decimation system is rewritten from scratch against the decompile as a
  reference.
- Minecraft changed under every system since 1.7.10 `[not verified]`, roughly:
  - blocks: numeric ids and metadata gone, block states (1.13 "flattening"); every
    `meta % 4` facing rule in our code becomes a state property;
  - rendering: no direct OpenGL immediate mode; baked models, render types, a shader core
    (1.17); Decimation's per prop tile entity renderers and its gun renderer have no direct
    equivalent;
  - entities and AI: goal selectors and the brain system; Decimation's NPC and infected AI is
    rewritten;
  - items: NBT on stacks replaced by data components (1.20.5); every gun stat and attachment
    slot moves;
  - networking: typed payloads (1.20.5); Decimation's 4700 line message holder (`deci.aE.a`) is
    rewritten;
  - world: height -64 to 320 (1.18), data driven world generation (structures, biomes, noise
    settings as JSON), chunk generation in stages and threads; our slice writer
    (`worldgen/Slices`) and every plan's `blockAt` would sit on a structure / feature API;
  - loaders: Forge split into Forge and NeoForge (2023); new work usually targets NeoForge
    1.21.x.
- Legal: Decimation's code, models, textures and sounds belong to BoehMod / ScottehBoeh. A port
  for personal use is like today's setup; nothing of theirs can be published. A public release
  could only carry our own code and our own assets (our guns, our sights, our worldgen).

## 3. The three paths

### Path A: modpack instead of a port (smallest)

Play a modern zombie modpack and bring only what we built ourselves.
- Base: DeceasedCraft (1.20.1, already studied: its Lost Cities data is the source of our city
  engine) or another modern apocalypse pack `[not decided]`.
- What moves: ideas and our own code. The city work maps closely onto Lost Cities, which
  DeceasedCraft already runs (rivers, bridges, rail, wasteland, edge wear, dressing would become
  Lost Cities assets or a small companion mod). Military bases, camps, refuges as structure
  templates (`.nbt`) built from our generators.
- What is lost: Decimation itself (its guns, NPCs, factions, humanity, bottlecaps, vehicles,
  zones, HUD). Those come from whatever the pack has.
- Effort: weeks for a companion mod with our structures `[inferred]`.

### Path B: our guns as a content pack for a modern gun mod (middle)

Keep a modern gun engine and feed it Decimation style content.
- Engine: TACZ (Timeless and Classics Zero) or a similar mod; TACZ takes gun packs with
  Blockbench (Bedrock geometry) models, animations, sounds and gun data files `[not verified]`.
- What moves: our own guns (MAC-10, UMP9 parts, HK416 parts, Mk18, EOTech 558, ACOG TA11) as
  models we already generate in code (`tools/guns/gunmodel.py`); Decimation's own guns only as a
  private, never shared pack (their art).
- Model problem: most Decimation parts are tapered shape boxes (60 % of all parts,
  docs/gun_style_guide.md). Bedrock cubes cannot taper `[not verified]`; each taper becomes a
  mesh (Blockbench mesh elements or OBJ) or is approximated by cubes. Check what the gun mod
  accepts before converting anything.
- Effort: days per gun once the converter exists; the converter about one to two weeks
  `[inferred]`.

### Path C: full rewrite on NeoForge 1.21.x (largest)

A new mod with Decimation's gameplay, written by us.
- Stages, each playable on its own:
  1. Assets: converter for `.bmodel` to OBJ or Blockbench, `.anib` to keyframes, props to block
     models (`propgeom.py` already knows every drawn box), textures and sounds copied privately.
  2. Guns: firing, recoil, magazines, attachments, scopes (our ScopeZoom work and the sight
     placement tables carry over as ideas).
  3. Player systems: humanity, bottlecaps, infection, armor.
  4. Infected and NPCs: variants, tiers (NpcLoadouts), factions, gunshot noise, traders.
  5. Vehicles (their own physics: the hardest single system `[inferred]`).
  6. World: zones, supply drops, our worldgen (cities, bases, camps, refuges, wasteland, rails,
     rivers) on the modern structure API.
  7. HUD and menus.
- Effort: months to years for one person, even with help `[inferred]`. Only worth it if the
  user wants to publish their own mod (with only their own assets) or plans to keep playing
  for years.

## 4. Moving the models (all paths that keep Decimation art)

| asset | today | modern target | our tool that helps |
|---|---|---|---|
| gun model | `.bmodel` text, shape boxes with corner offsets | OBJ via NeoForge's OBJ model loader, or Blockbench meshes `[not verified]` | study.py parses and renders it; gunmodel.py writes it |
| gun animation | `.anib` frames (Fire, Reload1, Rack, SlideBack) | own animation player, or the gun mod's keyframe format | docs/gun_model_spec.md documents the format |
| prop model | Java model classes, drawn by per prop renderers | JSON block models (boxes), OBJ for odd turns | propgeom.py: drawn box per metadata |
| facing | metadata 2 / 3 / 4 / 5, per renderer exceptions | block state `facing` | prop_facing.tsv (verified table) |
| textures, sounds | in the jar | resource pack | none needed (copy, privately) |

Rule for any conversion: render the converted model next to the original
(`study.py sheet`) and keep both images side by side before trusting it, the same check the
gun skill uses.

## 5. Skills to have before starting (the user's condition)

- Modern modding: NeoForge docs, Java 21, data generation, mixins (the modern way to change a
  game class; replaces our Javassist patches).
- Blockbench (models, animations) and the target gun mod's pack format (path B).
- Data packs: structures, loot tables, world generation JSON (paths A and C).
- Reverse engineering at the level this repo already uses: reading `deobf/src`, the notes in
  `deobf/notes/architecture.md`, the names table `deobf/names.tsv`.

## 6. First step when the user is ready (a spike, 1 to 2 days `[inferred]`)

Convert ONE gun (the MAC-10, fully ours, publishable) to the chosen format, load it in a bare
1.21 test mod (path C) or as a one gun pack (path B), and compare it with our own render. That
spike answers the open questions (tapers, texture layout, animation) before any path is chosen.

## 7. Recommendation

Path B first, as the spike above: smallest, keeps the part of this project that is most our
own (the guns), and tests the model pipeline every other path needs. Path A for the world side
if the user wants modern terrain and performance. Path C only with a long time budget.

## 8. Fabric on 1.7.10 instead of Forge (asked 23 Oktober 2026, analysis only)

User: "make a fabric mod to support this version of minecraft, then make DeciRemnant a fabric mod".

- The loader part exists: Legacy Fabric (legacyfabric.net) runs Fabric on 1.3 to 1.14, 1.7.10 included, with a
  Legacy Fabric API. Nothing to build there.
- The blocker is Decimation, not us: it is a closed Forge mod and our mod calls into it and patches it. Counted from
  the bytecode (dev/libs/Decimation-base.jar, our build): 590 of Decimation's 1323 own classes use Forge / FML, 81
  distinct Forge / FML types (SideOnly, SimpleNetworkWrapper and 207 IMessage classes, FMLCommonHandler, the event bus
  with RenderWorldLastEvent / RenderGameOverlayEvent / LivingHurtEvent and more, IItemRenderer, GameRegistry,
  ReflectionHelper, Configuration); no coremod. Ours: 255 of 583 classes, 79 types, nearly the same set.
- So Decimation on Fabric needs a Forge 1.7.10 layer on Fabric (what Sinytra Connector is for modern versions; none
  exists for 1.7.10 [not verified beyond a search]): those 81 types, the FML mod lifecycle and @Mod loading, and every
  vanilla hook Forge adds by binpatch (the events must fire from mixins at Forge's exact places, the IItemRenderer path in
  ItemRenderer, Forge's extra methods on vanilla classes), Forge's registry ids saved in level.dat (existing worlds),
  and the jar remapped from SRG to Legacy Fabric's intermediary. If that layer works for Decimation, our mod runs on it
  as it is (same types): porting ours to Fabric API on its own would be wasted, since it cannot load without Decimation.
- Rough share of the work [inferred, not measured]: loader 0 %, the Forge layer 75 to 85 %, remapping and world / id
  compatibility 10 to 15 %, our own code 0 to 10 %. Payoff on 1.7.10 is small: the 1.7.10 mods worth having (Angelica,
  lwjgl3ify, the GTNH set this workspace already builds with) are Forge mods, and gvc (voice chat) too.
- Advice given: do not; stay on Forge 1.7.10 (GTNH tooling), and if a move is wanted, it is to modern Minecraft
  (sections 3 and 7), where a Fabric / NeoForge choice is real.
