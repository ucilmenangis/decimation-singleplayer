# Feature Tracker - Decimation Mod

Status values: `Done` / `Decided` (direction agreed, not built yet) /
`Documented` (how-to written, not executed) / `Not started`.

---

## Done

### Gun draws: every gun comes into the view its own way (23 Oktober 2026, branch feature/gun-draw)
- User: "reimagine when switching to weapons (guns) they all need some kind of intro ... valorant smoothness or cs2 like
  ... arena breakout for intro animation for ALL of the guns". Decimation had none: a gun rose with Minecraft's flat
  equip dip.
- fixes/GunDraw wraps each gun's item renderer the first time the gun is in hand (Decimation's 103 guns, ours and gun
  pack guns alike): for the first person view it turns back to the view's axes (as MeleeRenderer), moves the gun along
  keys (Catmull-Rom on real time, MeleeMotion.curve) and lets Decimation draw it from there. A way per WeaponCategory:
  pistols snap up from the lower right with a flick (CS2), the revolver twirls once, SMGs swing in from the left
  (Valorant), rifles and shotguns rise low and canted and settle with their weight (Arena Breakout), machine guns the
  same slower and heavier, the rocket launcher onto the shoulder; 0.5 to 1.1 s. Each gun's own Rack animation
  (Deci.playRack: its Rack.anib and rack sounds, client only, nothing sent) works the action near the end (all but
  rockets, crossbow and flamethrower). On a switch to a gun and on leaving Decimation's holster (Deci.holstered).
  Minecraft's equip dip held off meanwhile (MeleeMotion.holdEquip, shared with the knives); aiming during the draw runs
  it three times as fast.
- Checked: dev test `gundraw` 14 PASS (Glock 17, Raging Bull, Uzi, AK-74, R870, PKM, RPG-7: switching starts the
  draw, the rack plays at its end, none for the RPG), shots at six moments of each draw. A gun below the view for the
  first tenth of a second read as a delay: every draw starts low but in view. Not seen by the user yet [not verified
  in game]; firing during the draw is not blocked (Decimation fires on its own mouse read).

### Katana on fire, wide hits and looking down (23 Oktober 2026, branch feature/katana-fire-reach)
- User: "fix the character view in FPS mode, look down and you'll see its botched, the character hand is floating (left
  hand)"; "increase the damage for katana and its attack range, 2x wide and damage 17"; "when katana is enchanted with fire
  aspect, it should add fire animation when user holding it ... when the intro animation is done and the user is almost
  holding it ... fire in the whole metal"; then "it should start burning when the hand catches it, fire slowly burns
  in the sword".
- Looking down (fixes/MeleeMotion SLIDE 35 / BODY 55 degrees, MeleeRenderer): our first person arm and knife hung over
  Decimation's armless first person body. They slide out of the view from 35 to 55 degrees; past 55 the body's own right
  arm holds the knife (Decimation's way for any item but a gun) and Minecraft's swing moves it; its left arm stays hidden
  (Decimation moves the body back and the arms forward: walking swung the left arm in as a hand without a shoulder).
  Walking while looking straight down still shows the body's left leg and brown shoe swing forward under the camera
  (Decimation's own body, the same with any item) [not verified with the user that this is not what they meant].
- Katana: 17 damage (NewMelee), hits twice as wide (MeleeMotion.wideHit: a swing at nothing looks again with every
  living box twice its width, then attacks through Minecraft's own attack: the server checks reach as for any hit).
- Slashes redone (user: "slashing animation looks weird still, doesnt come natural, not like the video"): the hand slid
  in straight lines while the blade spun about the grip. Now the arm turns about the shoulder (MeleeRenderer.SHOULDER,
  three more motion columns: shoulder yaw / pitch / roll), so the hand runs on an arc, and the blade rolls in the view
  so its tip leads the cut, side on as in the video; a slow wind up (0.11 s), a strike over in 0.06 s, a follow through
  that eases out and keeps the blade in view (forehand ends low left tip up, backhand right tip up, heavy low left).
  Karambit: the same, smaller, its thrust kept. A first try pointing the blade forward showed it end on: dropped.
- Fire Aspect: the katana is no sword to Minecraft, so a Fire Aspect book on an anvil goes on through NewMelee.onAnvil
  (AnvilUpdateEvent, 5 levels). fixes/MeleeFire draws the fire: a heat mask (tools/guns/melee.py --heat:
  katana_heat.png, the metal's pixels, red = guard 0 .. tip 255) fills a live texture at 30 fps with tongues of fire
  (dark red, orange, yellow, white core), drawn full bright and additive over the blade, then again twice as thick and
  faint (heat haze), plus little square flames peeling off the edge and spine. The burn after the catch
  (MeleeMotion.IGNITE 0.78 s into the draw): over SPREAD 0.8 s (user: 2 s was too slow, "2.5x" faster) a front climbs
  from the hand to the tip, the steel glowing red
  just ahead of it, the flames growing behind it. Held in third person and dropped it burns whole; others holding one
  shed flame and smoke particles.
- Checked: dev test `melee` 9 PASS (the 6 before, Fire Aspect II book on the anvil, an infected 0.62 off the view's line
  is off the crosshair and one swing takes 17 + 1 burn), look down shots at 30 / 50 / 70 / 88 degrees while walking, the
  burn every 0.12 to 0.3 s from the catch to the full blade; 0 OpenGL errors. Not seen by the user yet [not verified in
  game].

### Karambit and Katana: our own melee weapons with first person draw and inspect (23 Oktober 2026, branch feature/karambit-katana)
- User: "add 2 more knife: Karambit (held like CS) and a long Katana (like Valorant), vanilla version of how a katana would
  look like in a zombie apocalypse"; draw animation on pulling it out and an inspect (CS / Valorant smooth).
- Models, textures and icons: tools/guns/melee.py (Decimation style parts, worn and bloodied); registered by fixes/NewMelee
  as deci:karambit (7 damage) and deci:katana (9), Decimation's MeleeWeaponItem, loot wherever the machete is.
- fixes/MeleeRenderer: first person forearm in the player's skin holding the knife (Decimation cancels the hand for
  everything but its guns: MeleeMotion lifts that for our knives), third person, dropped, flat icon.
- fixes/MeleeMotion: keyframed draw and inspect (Catmull-Rom on real time, every frame), key "DeciRemnant: Inspect" (Y),
  a swing ends the inspect; Minecraft's equip dip held off. Katana draw after the user's Valorant Sovereign frames: tossed
  out of the hand spinning, a fake catch, the catch into the low hold (the knife has its own place off the hand); inspect:
  upright, tilted, hung point down, flat across. Decimation's first person body hides its arms only for guns: showModel
  off for our knives (three hands when looking down, user).
- Slashes (user: "the slashing animation is legit boring, refer to the video"): after the Sovereign blade's attacks, clicks
  closer than 0.9 s chain A (forehand, right to left), B (backhand, left to right), C (katana: heavy overhead diagonal;
  karambit: a thrust); a new swing is Minecraft's swingProgressInt back at 0. Minecraft's own swing is held at 0 for the
  first person frame (onRenderTick START / END), so third person still swings. Follow throughs keep the blade in view.
- Checked: dev test `melee` 6 PASS (slot change starts the draw, Y starts the inspect, a swing starts slash A, each
  knife), frames compared with the reference videos; `-Pgif` samples draw, inspect and slashes every 1/20 s without the
  HUD. User checked earlier builds in Prism; toss draw and slashes not seen by the user yet [not verified in game]. Arm is
  one skin block (no fingers). Looking down: see the next entry (Katana on fire, wide hits and looking down).

### HUD styles: Decimation or Inline (22 Oktober 2026, main)
- User: "an option to switch to inline mode, which is like minecraft default one ... reimagine it ... for the corner
  view of character make it like xbox version in top corner, so only character 3rd person view overall".
- fixes/HudStyle: style DECIMATION (default, Decimation's HUD unchanged) or INLINE; key "DeciRemnant: HUD style" (J)
  and a "HUD: Decimation / Inline" button on Decimation's Settings page (right of View Updates); kept in
  config/deciworldgen_hud.cfg. While inline it holds Decimation's own switches (ClientState cf: stats list hidden, cl:
  its old GUI, so vanilla's bars are not hidden) and gives the player's values back on the switch.
- fixes/InlineHud, redesigned after the user's first look (22 Oktober 2026: "looking vanilla ... reimagine it ... look
  like the mod theme, also make it available too to see in creative mode ... NO VANILLA THEME"): only Decimation's art.
  Its brush stroke panel (statsbg) behind every group; health and blood left, food and water right (oxygen over them
  under water), each its own HUD icon (the infected cross while infected) and a bar of ten segments in the icon's
  colour, pulsing below a fifth, the blood icon shaking while bleeding; humanity as a thin banner line (its location
  banner's look) red toward bandit, green toward hero, its humanity star as the marker, the rank in its bold outlined
  type for 3 s after a change; the hotbar on a brush strip with its slot squares and the selected slot framed in its
  red; armor with its shield left of the hotbar, bottlecaps with its cap icons right. Drawn in the hotbar's place, so
  survival and creative alike; vanilla's health, armor, food, air and experience are cancelled.
- fixes/HudDoll: the player in the top right (Decimation's toasts and tips own the top left) on a brush panel, three
  quarter view toward the screen's middle, live (walk, crouch, gear, gun, cape), its head below vanilla's 32 px toast
  strip; "DAY N" under it; the conditions as red edged plates (its notification look) beside the panel; with the player
  list key the vitals in numbers and the stats Decimation's own list showed; the range finder distance / target range
  run when they apply.
- Checked: dev test `hud` 7 PASS (switches held and given back, the Settings button and its label, 0 GL errors after
  our drawing, 0 frames where the corner changed the matrix stack, creative), shots in docs/shots/hud_v0.65.0
  (docs/shots_index.md). Cost (dev client, unlimited fps, all conditions on, 100 ticks each way, 22 Oktober 2026):
  frame 8.541 ms with Decimation's HUD, 8.399 ms inline (its stats panel, menu figure and big icons are not drawn);
  our inline drawing 0.664 ms a frame (hotbar, rows, line, sides and the corner figure); texts built when their number
  changes, the corner box once a tick (the range finder's 1000 block ray once a tick, Decimation's ran every frame).
  The user's first look was the vanilla styled version; this one not seen yet; not tried with OptiFine (no production
  client on this Mac) or on a server [not verified].

### New attachments: RK-1 Foregrip, ELCAN SpecterDR 1x/4x, grips on every gun (21 Oktober 2026, main)
- User: "we have 2 new attachment: new grip, and 4x scope"; chosen: the SpecterDR with a 1x/4x switch on a key (G),
  the grip with its own recoil (1/2 up, 1/4 sideways), every grip placed per gun, loot like the 4x and the Foregrip.
- Spec and plan: docs/superpowers/specs/2026-10-21-new-attachments-design.md, docs/superpowers/plans/
  2026-10-21-new-attachments.md. Code map, numbers, checks and what is open: docs/decimation_code/weapons.md section 8.
- Checked: dev client (attachments 22, perf 7), production client with OptiFine (22 + 7, glstack), dedicated server
  (attachmentsmp 12). Not seen by the user in game yet; carried to prerelease with the docs.

### Wasteland gates: 5 looks, 8 in 10 shut (14 Oktober 2026, v0.61.0)
- User on 0.60.2: gates between a wasteland city and its city in about 6 styles, the first a toll gate;
  then 80 % shut so players and mobs cannot pass, 20 % broken open; closures barbed wire with landmines
  plus traps. Chosen (asked): looks toll, checkpoint, containers, quarantine, police; closures wire +
  mines, welded steel, burnt bus, tank ditch; open gates smashed.
- military/CityGate, CityGatePlan, city/WastelandFence.gate, our block deciworldgen:boom_barrier. Details,
  checks and what is not verified: docs/wasteland.md "v0.61.0". New worlds' chunks only (wasteland sector
  worlds since v0.57.1).
- Refuges in big cities 1 per 4 city sectors (v0.60.2, docs/city_engine.md "City sizes").

### Cities of 4 to 64 districts (13 Oktober 2026, v0.60.0)
- User idea (docs/ideas.md item 19) and choices: sizes 4 / 8 / 16 / 24 / 48 / 64 districts with
  weights 30 35 40 35 28 22, compact blobs, more city land (cities and roads felt too rare), one
  wasteland sector per 4 city sectors on the side toward the military.
- worldgen/CityLayout (+ CityLayoutEvents, config/deciworldgen_cities.cfg); new worlds only. Design,
  numbers and checks: docs/city_engine.md "City sizes". Seed 1: city land 24.3 % (was 15 %).

### Auto update from our GitHub releases (13 Oktober 2026, v0.59.0)
- User: "user launch decimation then it trigger check updates on github via api, if there is update,
  the button on hud show "update" and it will begin install on minecraft with install progress
  screen. if not it show "play" button"; finish chosen: "Quit game" / "Later".
- fixes/Updater: at start (client) a background check of the release list
  (api.github.com/repos/ucilmenangis/decimation-singleplayer/releases, pre-releases included), the
  newest release with a "Decimation.jar" asset above our mcmod.info version. Decimation's own
  UpdateChecker.isUpdateRequired (tools/patches/PatchNoUpdate.java v2) answers Updater.required(), so its
  menu button says "Update" only then, "Play" otherwise. The button's page (GuiMenuUpdates) is swapped
  for fixes/UpdateScreen (GuiOpenEvent; since v0.64.7 Decimation's own progress screen GuiUpdateProgress through
  fixes/UpdateProgress, UpdateScreen only for a failure: docs/main_menu.md 2d): download at once into "<jar>.download" beside the running jar
  with a progress bar, checked against GitHub's sha256 digest, then "Quit game" / "Later"; the swap runs
  when the game closes (fixes/UpdateSwap as its own Java process from a temp copy: Windows locks a
  loaded jar). config/deciworldgen_update.cfg: enabled, api.
- Checked: dev test `update` against tools/fake_release_server.py: release found, Decimation's check
  true, menu Update button, our screen, progress (33 %), ready, the helper replaced a stand in jar
  (all PASS); a wrong digest: refused, download deleted, old jar kept. The real GitHub check from the dev
  game: "up to date (0.58.0...)". Shots docs/shots/update_v0.59/sheet_update.png.
- Real GitHub (after 0.59.0 was published, dev test `update real=true`: the dev game pretends to run
  0.58.0): found 0.59.0, Decimation's check true, Update button, our screen, the real 229 MB jar
  downloaded in 177 s and matched GitHub's sha256 (9e20395d...), the helper swapped it into the stand in
  (starts PK, 240980301 bytes): all PASS. Shots docs/shots/update_v0.59/sheet_update_real.png.
- Not verified: old Java 8 builds bundled by launchers (HTTPS to GitHub), a launcher that kills the game's
  child processes on exit, the update running inside a real installed game (the dev game runs from a
  folder, so a stand in jar was replaced; the first real one is 0.59.0 -> the next release).
- Security (commit review, same day): the swap helper ran from a fixed folder in the shared temp
  directory, where another local user could plant a class; now a fresh folder from
  Files.createTempDirectory (owner only: rwx------ checked on Java 8), deleted by the helper when done.
- Release rules for it: the asset must be named exactly Decimation.jar, the tag a dotted version above
  the last one (0.59.0, 0.60.0 ...). Since v0.59.1 also upload Decimation.jar.sha256 (tools/onejar.py
  writes it beside the jar): the fallback below checks the download against it.
- v0.59.1, rate limit fallback (13 Oktober 2026): GitHub allows 60 API calls an hour per address without
  a login; our own production client got 403 after an afternoon of checks, and players behind one shared
  address (mobile networks, CGNAT) would too: the menu then showed Play and never offered the update.
  Now (fixes/Updater): (1) the API is asked with the ETag of its last answer
  (config/deciworldgen_update_cache.json); GitHub does not count a 304; (2) when the API fails anyway
  (403 / 429, timeout, bad answer) the check reads the release feed <web>.atom (not the API, pre-releases
  included, newest first), takes the first newer tag whose <web>/download/<tag>/Decimation.jar answers a
  HEAD, and its sha256 from <web>/download/<tag>/Decimation.jar.sha256 (older releases have none: size
  check only). Config key `web`. Updater.source() says where the answer came from.
- Checked: dev test `update` with tools/fake_release_server.py: API answer, then a second check "api (not
  modified)" (304); with LIMITED (API 403) the answer comes from the feed and the jar downloads, is checked
  against the .sha256 and swapped; LIMITED BADDIGEST: refused, old jar kept. Real GitHub with the API
  pointed at a dead address (`real=true feedonly=true`): 0.59.0 found from github.com's feed, 229 MB in
  13 s, size matched (0.59.0 has no .sha256), swapped: all PASS.

### Shared core, step 1 of the master plan (13 Oktober 2026, v0.57.0)
- docs/systems_plan.md section 2: `fixes/WorldClock` (day number, DAY / DUSK / NIGHT / DAWN events
  for every later system), `fixes/LootPools` (16 loot lists, 4 kinds x 4 levels, in
  config/deciworldgen_loot.json; magazines taken from each gun's own list; roller with "first"
  lists so level 4 holds one item of every type), `fixes/Protection` (armor table in
  config/deciworldgen_protection.json, 124 wearables by family; not used by damage yet),
  `worldgen/Areas` (our areas with kind, owner, state, per world; radiation / safezone / military
  areas put a Decimation zone in place). Loot spikes before it (v0.57.0 too): fixes/Loot, LootBody.
- Decimation's crate off by one fixed (bug.md).
- Checked: dev test `core` 36 checks PASS; dedicated server boots with it; dev test `items` dumps
  every item (docs/references/decimation_items.tsv).

### Zones on generated structures (auto zone tagging)
- **Requested**: 27 Juli 2026
- Zones (safezone/military/police/radiation/scary/border,
  `net.decimation.mod.server.zones`) normally need manual wand placement.
- **Root cause found on the way**: zones never worked in singleplayer at all.
  The zone list (`deci.aJ.b.aAc`) is only loaded by ServerProxy, and every
  zone effect handler (`deci.aK.d/g/i/m`) is `@SideOnly(Side.SERVER)`.
- **Implementation (v0.7.0)**: `ZoneStore` keeps zones per world in
  `<world>/deciworldgen_zones.json` (Decimation's own JSON format) and merges
  them into Decimation's live list, re-adding them if Decimation reloads its
  global file. The generator tags `mil_*` structures MILITARY and `city_*`
  POLICE, footprint plus 8 blocks. `ZoneSpawnHandler` replicates
  `deci.aK.d` in singleplayer: 25% of infected spawning in a MILITARY/POLICE
  zone become that uniformed variant, infected spawns in a SAFEZONE are
  cancelled.
- **Verified 7 Oktober 2026** without the user: dedicated server run (zones
  created, saved, reloaded after restart without duplicates) and an
  unattended singleplayer run (`gradlew runClient -Pautotest`): inside a
  POLICE zone 10 of 33 infected became police, outside 0 of 34.
- Not covered: radiation/scary/safezone player effects (`deci.aK.m`, `g`,
  `i`) in singleplayer; our generator does not create those zone types.

### Skip intro screens (jumpscare + BoehMod studio logo)
- **Requested**: 25 Juli 2026
- Every launch replaced the vanilla main menu with a chained animated intro
  (390-frame "jumpscare" clip, then BoehMod logo zoom/mortar-explosion
  sequence) before reaching the mod's own main menu.
- **Implementation**: flipped the default value of two static gate flags
  (`deci.i.d.iH`, `deci.i.c.io`) to `true` in their class initializers - both
  screens' own logic already treats "flag true" as "already played." Assets
  and classes untouched, just never instantiated.
- **Done**: 25 Juli 2026, 14.30. User-confirmed working 14.41.

### Nerf ranged weapon damage (50%, all weapons including crossbow)
- **Requested**: 27 Juli 2026
- **Implementation**: single chokepoint patch on `deci.ay.i.am(int)` - the
  damage-per-hit setter every one of the ~90 registered guns/rockets funnels
  through via its `.am(N)` builder call. Now stores `Math.round(n * 0.5f)`
  instead of `n` directly. Applies uniformly, no per-weapon exceptions.
- Side effect (welcome one): NPC bandit/soldier gunfire uses the same `.aew`
  field for their own damage, so this also halved NPC-inflicted bullet damage.
- **Done**: 27 Juli 2026. User-confirmed via mob testing (2->3 bullets to kill
  unarmored zombie/skeleton, consistent with the cut).

### Buff armor damage reduction (35%)
- **Requested**: 27 Juli 2026
- **Implementation**: scaled `ItemArmorDeci.damageMultiplier` by `x0.65` at
  construction (both constructors), proportional to each armor piece's
  original value.
- **Known gap**: only affects damage from a *player's own gun* - does nothing
  against NPC (bandit/soldier) gunfire. See `bug.md` -> "Armor damage-reduction
  buff doesn't affect NPC gunfire" for the root cause and proposed fix
  (pending user go-ahead, not yet implemented).
- **Done (partially)**: 27 Juli 2026, 00.08.

---

### Cheap scope (view zoom instead of picture in picture), DONE v0.28.0..0.28.4
- **Requested**: 8 Oktober 2026 (scope cost about 150 -> 110 fps); user
  approved the result 9 Oktober 2026 ("the scope do really well").
- reddot / 2x: world and gun zoom together (EntityRenderer.cameraZoom),
  see-through glass (frame under the glass copied before the hand, mapped
  by screen position), sight centred on the aim point.
- 4x and up (4x, dragunov, 8x, integrated aug): black sniper overlay with
  the scope's reticle, no gun (config `overlayFrom`).
- Mouse slows to 1 / zoom while zoomed; old picture in picture scope kept
  behind `pictureInPicture=true` (config/deciworldgen_scope.cfg).
- Code: `net.decimation.fixes.ScopeZoom`, `tools/patches/PatchScope.java`;
  history and measurements in bug.md "FPS drop while aiming through
  scopes"; test `./gradlew runClient -Pautotest -Pscopeonly`.

## In progress

### Decimation world generation (own code inside the jar)
- **Requested**: 26 Juli 2026 (original "Decimation world type" idea);
  direction changed 13-15 Agustus 2026.
- **History of direction**: first decision was "don't build it, use companion
  mods" - tried Ruins, then ezWastelands + GeneratorMods (CARuins/GreatWall/
  WalledCity). All dropped: CARuins generated wrong-aesthetic rubble blobs,
  GreatWall/WalledCity ship zero usable templates + Windows-backslash path bug,
  ezWastelands terrain made stepped-pyramid artifacts. New decision: build
  world-gen ourselves as ordinary Java compiled into `Decimation.jar`.
- **Architecture** (settled 15 Agustus 2026):
  - **Terrain is not our job**: user wants realistic real-life-looking
    terrain, handled by companion mod **RTG (Realistic Terrain Generation)
    1.7.10-1.1.1.7** (CurseForge, confirmed exists; not yet installed).
  - **Structures are our job**: an FML `IWorldGenerator` (runs per chunk on
    top of ANY terrain generator, no custom WorldType needed) placing
    buildings/cities read from standard **`.schematic` files** (MCEdit/
    WorldEdit format - user picked this over a custom save command).
  - **Second `@Mod` in the same jar**: new code lives under
    `net.decimation.worldgen` as mod id `deciworldgen`
    (`required-after:deci`). Forge scans all classes in a jar for `@Mod`, so
    this loads alongside the original mod with zero edits to obfuscated
    bytecode - fully removable by deleting its entries from the jar.
- **Milestone 2 DONE, user-confirmed in-game 15 Agustus 2026, 11.52**:
  proof-of-life generator (`MarkerGenerator`) places a 4-tall glowstone
  pillar at the centre of every overworld chunk. Screenshot confirmed pillars
  + FML log confirmed full mod lifecycle. Crash-hardened: catch-all around
  generation (logs + self-disables after 3 errors, never crashes the game),
  null-guards on block lookup and world provider, +8 chunk offset so no
  cascading chunk generation. (Marker later removed again - replaced by the
  milestone-3 structure generator.)
- **Milestone 3 DONE, user-confirmed in-game 15 Agustus 2026, 12.08**:
  real structure placement. `Schematic.java` loads standard MCEdit/WorldEdit
  `.schematic` files (gzip NBT, AddBlocks nibbles for mod-block ids > 255,
  TileEntities ignored - Decimation loot is position-based) from
  `config/decimation_worldgen/`. `StructureGenerator.java` places them:
  deterministic 4x4-chunk cells (world seed + cell coords -> same layout
  every time, no overlap possible), 50% chance per cell, site rejected if
  ground-height spread > 6 or on water, dirt foundation auto-filled up to 8
  deep, footprint capped 24x24 to stay inside the safe population window
  (zero cascading chunk generation). Placement coords logged. Same
  crash-hardening pattern as milestone 2. Two generated test schematics
  (`tools/make_test_schematics.py` -> `structures/`): `ruined_house` 11x6x9,
  `watchtower` 7x9x7, vanilla blocks + vanilla chest only (chest is in
  Decimation's loot table -> loot GUI works, and vanilla ids are immune to
  FML's per-save mod-id assignment). Screenshots confirmed: placement +
  foundation + ruin decay + Decimation loot GUI on the chest all working.
- **Milestone 4 DONE, user-confirmed in-game 15 Agustus 2026, 12.27**:
  rotation. `Rotation.java` = clockwise metadata rotation table per 1.7.10
  block family (6-direction facing, stairs, log axes, torch/button/lever,
  doors incl. untouched hinge half, trapdoors, beds, fence gates, pumpkins,
  repeaters/comparators, 16-step signs, rails incl. slopes/curves, vines,
  anvils; unknown ids - including Decimation's own blocks - pass through
  unchanged). Structure direction (0/90/180/270) picked deterministically
  per cell from the world seed; coordinates transformed, footprint extents
  swapped on 90/270. Unit-tested before deploy: 4x90 = identity for all
  256 ids x 16 metas + 15 spot checks. Old chunks keep their baked north-
  facing builds; new chunks rotate.
- ezWastelands + GeneratorMods renamed to `.jar.disabled` in the instance's
  `mods/` folder (15 Agustus 2026) - restore by renaming back if ever needed.
- **RTG installed 15 Agustus 2026** (`RTG-1.7.10-1.1.1.7.jar` in mods/,
  archive copy in `WorldGenerator/`), user-confirmed loading; world type
  "Realistic" at world creation.
- **Prop substitution DONE, crates user-confirmed in-game 15 Agustus 2026,
  12.46**: schematics use vanilla placeholder blocks, swapped for real
  Decimation props at placement, resolved BY REGISTRY NAME (immune to FML
  per-save numeric ids). Registry names are the CamelCase class names
  (`deci:BlockWoodCrate`) - found via FML registry snapshot in a world's
  `level.dat`; the lowercase `prop_*` strings in the jar are texture paths
  (first attempt used those, failed cleanly thanks to the null-guard).
  Mapping: sponge->WoodCrate, gold->MilitaryCrate, lapis->AmmoCrate,
  diamond->MedicalCrate, emerald->PoliceCrate, iron->Wreckage1-5 (random per
  position), coal block->BlockRoad, wool colour meta = 16 street-prop slots
  (sandbag/cone/vending/streetlight/barrier/bin/bench/dumpster/tire/barrel/
  cardboard/trashbag/electricbox/shelf/canfire/roadsign).
- **Full prop catalog discovered** in level.dat: Decimation ships road blocks
  with lane markings (`BlockRoad_*`, `BlockRoadSlab_*`), street lights,
  traffic lights, road signs, sandbags, barriers, dumpsters, vending
  machines, office wall sets, graffiti, metro props - everything needed for
  proper city generation later.
- **8 schematics** now (`tools/make_test_schematics.py`): ruined_house,
  watchtower, gas_station, house_2story, military_outpost,
  police_checkpoint, street_scene, survivor_camp - the last six use real
  Decimation props (asphalt roads, sandbag barricades, street lights, fire
  barrels, wrecked cars).
- **Underwater placement fixed, 7 Oktober 2026 (v0.7.0)**: the water check
  never worked. `getTopSolidOrLiquidBlock` skips water in 1.7.10, so the check
  looked at the lake/sea bed and structures were built on the ocean floor
  (found in a dedicated-server test: a shed at y=31 under 27 blocks of
  water, read straight from the region file with `tools/worldcheck.py`).
  Now the block above every sampled ground point is checked; the same seed
  rejects all 4 underwater sites. Terrain grading had the same blind spot,
  also fixed. Worlds generated before 0.7.0 keep their underwater ruins.
- **Ships as its own jar since 7 Oktober 2026**: `dist/deciworldgen-0.7.0.jar`
  next to `dist/Decimation.jar` (= patched Decimation without our classes).
  Built in the `dev/` workspace (`./gradlew build`). Prism confirmed loading
  it (7 mods, all our handlers registered).
- **City street grid, 7 Oktober 2026 (v0.8.0, in `dist/`, NOT yet in
  Prism; Prism runs 0.7.0)**: CITY sectors get a 5 block wide street
  (`deci:BlockRoad`) along every cell's west and north edge, i.e. a grid
  with 64 block spacing. Each chunk paints only its own columns (no
  cascading generation), the surface follows the terrain, skips water (streets
  stop at lakes, no bridges yet) and clears plants/tree parts above.
  Structure anchors are now limited to the first 3 chunks of a cell so a
  footprint never reaches the next cell's street. Side effect: in worlds
  started before 0.8.0, newly generated chunks use the new anchor rule, so a
  structure can rarely be duplicated or missing right at the old/new border.
  Verified on seed 1 from the region files: continuous streets at x=128 and
  z=192 (y 62 to 72), gap of at least 3 blocks to every structure; the
  singleplayer autotest still passes. Needs the user's eyes on how it looks.
- **Procedural city blocks, 7 Oktober 2026 (v0.10.0)**: user found the
  structures too basic and chose procedural buildings plus community
  schematics. City sector cells are now filled by `CityDistrict`: 4 lots per
  street block with ruined apartments, offices and shops (1 to 6 floors,
  rooms, ladder shafts, broken windows, decay, collapsed corners, Decimation
  crates and furniture), POLICE zone per building. Verified from the world
  files on seed 1 (34 buildings near spawn; a 6 floor office checked column
  by column) and in the singleplayer autotest (40 buildings).
- **Large schematics, 7 Oktober 2026 (v0.11.0)**: any size up to 120x120 from
  `config/decimation_worldgen/large/`, one per 128 block site, written slice
  by slice like the city buildings. Test content: `mil_compound` (48x48
  walled base, 4 towers, 2 barracks, helipad, gate). Verified whole with the
  new pregen tool: all 16 chunks, towers, roads, 6 military / 2 ammo / 2
  medical crates, cabinets, wreckage. See `docs/worldgen.md`.
- **City v2, 7 Oktober 2026 (v0.12.0)**: user review of 0.11.0 ("good for a
  oneshot, needs polish"): more size variety (1 to 20 floors), less brick,
  heavier decay matched to the biome, realistic interiors (asked for
  research), sidewalks, parked cars facing along the road (an old session
  had all cars facing north). Done: researched floor plans
  (`docs/building_design.md`): apartments with a double-loaded corridor and
  units (kitchen, dining, bedroom or studio bed), offices with reception,
  meeting room, storage and desk rows, shops with checkout front left,
  aisles and a stockroom; switchback stair cores to a roof hatch; 11
  palettes; collapse cones over 1 to 3 storeys; biome overgrowth (vines,
  leaves, snow, sand, dead bushes). Verified on seed 1: 33 buildings, 0
  missing walls, 8 stairs per storey in every cored building, beds in all 14
  apartments, 72/72 cars aligned with their street, 3000 sidewalk columns.
  User checked in game: cars sat across the road (model long axis is x, not
  z), swapped in v0.12.2 (north-south 5/3, east-west 4/2), fewer cars.
- **Decimation world type (user chose A over re-skinning vanilla / RTG,
  7 Oktober 2026)**: flat rolling land, rivers and lakes no ocean, dead near
  cities and overgrown far away, one temperate climate. DONE in v0.14.0
  (docs/terrain.md); seen in game by the user (screenshot). Inserted before
  round 2 step 2. Open: fog colour, more dead decoration (rubble, ash).
- **Worldgen round 2 (user chose option A, 7 Oktober 2026), plan in order**:
  1. terrain blending: Building plan covers its whole 26x26 lot; yard
     columns graded from the building floor (at the walls) to the natural
     height (at the sidewalk), so no plinth cliffs; front yard as gravel /
     asphalt parking with a few parked wrecks;
  2. street life: street lamps on sidewalks facing the road, benches,
     bins, dashed centre lines (road marking orientation must be checked
     in game like the cars were);
  3. full multiblocks: place the second block of 2 block props (shelves);
  4. new procedural building types: police station, hospital, gas station,
     warehouse, with themed loot;
  5. loot balance per building.
  Status: step 1 DONE in v0.13.0 (headless verified, in game look not yet
  seen by the user). Implementation differs from the first sketch: the
  Building plan keeps its own bounds; it implements `Graded` (lot bounds +
  `grade(world, x, z, baseY)`) and `Slices` grades every lot column of a
  window before writing the plan there. Buildings now keep a 2 block side
  yard and a 3 block back yard where the lot allows, and a setback of 6..9
  (75% of lots with room) for a front yard.
  Step 2 (street life) DONE in v0.15.0: levelled streets, centre lines,
  lamps, benches, bins, trash bags, checked in autotest screenshots.
  Step 3 (full multiblocks) DONE in v0.16.0: generated shelves get their
  upper part (they are 2 tall, not 2 wide); vending machines turned out to
  be single block props.
  NEW step 4 (user, 7 Oktober 2026): building quality pass BEFORE new
  types. User: buildings still need improvement and polish in every
  aspect, interior and exterior; people love small details; design the
  interiors in many deliberate steps so future buildings are production
  quality. Sub-steps: a. audit (floor plan renders per storey + in game
  screenshots per kind, findings with evidence); b. prop catalogue
  (screenshot gallery of every Decimation prop); c. design spec per room
  type and for exteriors in docs/building_design.md; d. interiors room by
  room, each verified; e. exterior polish; f. user review in game.
  Then step 5: police station / hospital / gas station / warehouse, and
  step 6: loot balance per building.
  User decisions on the spec (7 Oktober 2026): survivor camps yes, shop
  signs from Decimation's assets, start with the audited apartment /
  office / shop; later many more types in categories (civilian: houses,
  garage, food store, vehicle store...; police; military base; more
  categories to come). Build the polish as reusable parts (see
  docs/interior_spec.md section 9) so new types are cheap.
  4d.1 DONE v0.17.0: doors + low debris. 4d.2 DONE v0.17.0: surfaces
  (room grid, floors, wall panels, ceiling lights / vents). 4d.3 DONE
  v0.18.0: apartment rooms. Next 4d.4: office storey programs + lobby.
  REVISED after the user's in-game review of 0.18 (7 Oktober 2026, "not
  ready to be called interior"): items stood on the floor along the walls
  (TV and radio on the floor, sofa of bare stairs, kitchen = a row of loose
  blocks, lime rug, empty room middle), ceilings were the floor of the
  storey above, outer walls showed the facade inside. New order, approved:
  1. structure: storeys 5 high (floor, 3 air, own ceiling layer), plaster
     lining on the inner face of outer walls;
  2. furniture SETS designed as groups (living, kitchen, bedroom,
     bathroom first), DATA DRIVEN and user editable like Lost Cities parts
     (JSON grid + palette in config/decimation_worldgen/sets/, in-game
     reload command), each checked with close-up screenshots;
  3. reference library per building category (real plans + good Minecraft
     interiors) saved once in docs/, a checklist per category, and ONE
     critic agent pass per milestone on the audit screenshots;
  4. then offices / shops with sets + critic.
  Lost Cities (McJty, MIT, data-driven JSON parts / palettes, 1.10.2+):
  do not port the mod (replaces terrain, fights our world type, its
  interiors are simple too); take the data-driven idea, maybe import its
  parts later through a block translation tool with a license notice.
  Steps 1a (v0.19.0) and 1b + 2 for apartments (v0.20.0) DONE; live loop
  added on user request (no restart per change): /deciworldgen reload /
  rebuild, -Photswap + tools/hotswap.py. Step 3 first pass DONE v0.21.0:
  reference + checklist, critic pass 1 (overall 3/10 on v0.20), fixes for
  the verified findings. Open items listed in
  docs/references/critic_apartment_v0.20.md; then offices / shops on sets.
  DIRECTION CHANGE (user, 7 Oktober 2026): no fixed score target, steady
  improvement per version; first restructure the worldgen code so new
  building types are easy and interiors manageable. Study of Lost Cities
  + Recurrent Complex done (docs/references/worldgen_study.md); user
  decisions: hybrid content (authored parts + procedural), in-game
  capture tool early, footprint size classes. Architecture draft:
  docs/worldgen_architecture.md, migration in 6 steps. Map studies (8
  Oktober): hand-built Decimation maps + DeceasedCraft's 79 building
  types (docs/references/decimation_maps.md, deceasedcraft_buildings.md).
  Architecture APPROVED 8 Oktober 2026. Step 1 (asset core + capture) DONE
  v0.22.0. Next: step 2, extract Shell / StoreyPlan / Furnisher / layers
  out of Building without behaviour change. User decision 8 Oktober:
  public buildings 6 high storeys, homes / apartments 5 (after step 2).
  DIRECTION CHANGE (user, 8 Oktober 2026): DeceasedCraft's buildings as
  the city content (current beta + legacy 5.5.5 with the wasteland
  district), Lost Cities layout (chunk streets, levels 6 apart, stairs).
  v0.24.0..0.24.3 (docs/city_engine.md): content pack tool, city engine,
  street dressing, city edge ramp, superblocks for towers, wasteland
  district next to military sectors. Procedural buildings stay as the
  fallback when no pack is installed.
  Step 2 DONE v0.22.1 (Building split into worldgen/building/, identical
  output). Offices and shops 6 high DONE v0.23.0. Next: step 3 (types
  as JSON, rotated stair core).
  Lost Cities leftovers (user, 11 Oktober 2026: "start 6, except giant
  building"; choices: rail first, underground subway with tunnels between
  cities, then bridges over rivers through cities): railway DONE v0.44.0,
  waiting for the user's look in game (docs/city_engine.md "Railway",
  shots lc_rail_v0.44.0). Bridges DONE v0.45.0 (rivers through cities in
  new worlds, canals, DeceasedCraft's bridges; docs/city_engine.md "Rivers
  and bridges"). User look at 0.45.0: "the bridge show up just need change
  the design so the bridge match with other road"; v0.45.1: the bridge is
  the street part itself over the water (same blocks, same height, railings,
  broken hole in the wasteland), waiting for the user's look.

---

## Not started

### 3 new mobs
- **Requested**: 27 Juli 2026
- **Civilian**: new peaceful/passive NPC mob. Scope (combat behavior, spawn
  conditions, etc.) not yet defined - flesh out when picked up.
- **Trader fix**: trader NPCs already exist in the codebase. Since the
  7 Oktober 2026 naming pass the family is known: `deci.ai.a` TraderEntity
  (base), `deci.ai.v` ArmedTraderEntity, and about 20 types in `deci.ai.*`
  (ammo, armor, guns, food, medical, black market, car dealer, banker =
  `deci.ai.e`, ...); see `deobf/names.tsv`. Which one(s) the user means still
  needs confirming. They currently can
  only be spawned via a server command and is static - can't move. Goal: make
  it spawn normally (not command-only) and able to walk around like other
  mobs.
- **Spec-ops military**: new elite mob variant. Full Juggernaut armor
  (`ItemArmorJuggernaut` items already exist -
  `juggernautHelm/Vest/Pants/Boots`, see `deci.aD.k` around the `.setLootChance`
  calls) *or* a full black armor set + night vision goggles - two possible
  looks, decide which (or both as variants) when implementing. Heavy loadout,
  all weapons already exist in `deci.aD.k`, no new guns needed:
  - Rifles: SCAR-H (`fnscarhamr`/`arI` or `fnscar`/`arH` - confirm exact
    variant), SCAR SSR (`fnscarssr`/`arL`), M110 (`m110`/`arf`), FAL (`fal`/
    `ark`).
  - Snipers: SV98 (`sv98`/`asl`), JNG-90 (`jng90`/`asm`), Barrett (`barrett`/
    `asn` - user said "M82," confirm this existing entry is that model), AWM
    (not seen in the registered weapon list yet - confirm it exists or needs
    adding), SVD (`svd`/`asj`).

### New hostile NPCs and stronger bandits (plan, 9 Oktober 2026, "later")
- **Enemy military**: medium to high armor, rifles or heavy weapons.
- **Enemy juggernaut**: full juggernaut armor (`juggernautHelm/Vest/Pants/
  Boots` exist), heavy machine guns (PKM, M240, any machine gun in the
  registry) or a Barrett sniper with ridiculous damage (.50 BMG).
- Both spawn in military buildings / military areas (our MIL sectors,
  military zones, the wasteland district next to them).
- **Bandits**: heavier arsenal (PKM, SV98, militia guns of the medium to
  heavy class) and medium to heavy armor.
- Relates to "3 new mobs" (spec-ops military, weapon list there) and
  "More clothing variety" below; NPC shots now draw correct tracers
  (bug.md, fixed 9 Oktober 2026), and NPC gunfire respects armor (v0.9.0).
- Registry names checked 9 Oktober 2026 (level.dat ItemData): every gun
  and armor piece registers under its plain name (`deci:pkm`, `deci:sv98`,
  `deci:m240`, `deci:rpk74`, `deci:akm`, `deci:marineHelm`,
  `deci:spetsnazVest`, `deci:juggernautVest`...), so our code finds them
  with `GameRegistry.findItem("deci", name)`, no obfuscated names.

#### Step 1 design: NPC tiers (started 9 Oktober 2026, user: "Bandits +
military first, hard but fair")
How Decimation's armed NPCs work today (read from deobf source):
- BanditEntity (deci.ag.a, base of Soldier deci.ag.l, Hazmat deci.ag.c,
  Soviet deci.ag.m) rolls its gun and armor in the CONSTRUCTOR, on server
  and client separately. The gun is a plain field, put in the held slot
  every tick on both sides and never saved: the client shows its own
  random gun, and a reloaded NPC rolls a new one. Armor goes through
  vanilla equipment (saved, sent to the client on tracking start).
  Backpack / mask / vest are plain unsynced fields (client random).
- Health 20 for all of them. Shots: 75% hit, gun damage / 8 against
  non infected, delay 5..20 ticks (soldier 2..5), "human" source without
  the shooter.
- Player shots hit NPCs for the full gun damage: Decimation applies armor
  multipliers only when the victim is a player, so NPC armor is
  cosmetic. Runtime multipliers are the originals x 0.65 (our armor buff),
  a full bandit set would be x 0.17: far too strong to reuse for NPCs.
- Soldiers attack players with humanity under 50 only. Soviets (spetsnaz
  set, AK74 / AKS74U / AK12) are hostile to every player and every non
  Soviet human: Decimation's own enemy military. Bandit, Soldier, Soviet,
  Hazmat each spawn naturally at weight 1 in every biome (MobSpawnRegistry).

Design (our code, `net.decimation.fixes.NpcLoadouts` + `MilitarySpawner`):
- Every armed NPC gets a tier when it first joins a world (server), stored
  in its entity data (ForgeData "deciworldgen_tier", saved with it): gear
  (all 4 armor slots, so the client's own roll never shows), gun, max
  health, shot delay, and the share of a player's gun damage it takes
  (LivingHurtEvent, source "gunDeci"). Reloaded NPCs get gun and delay
  back from the stored data.
- The chosen gun goes to the client through data watcher slot 26 (gun
  registry name, added in EntityConstructing), applied on the client each
  tick, so the gun seen, its flash and sound match what shoots.
- Tiers (defaults, config `deciworldgen_npc.cfg`):
  bandit light (more clothing mixes, pistols / SMGs / old rifles), bandit
  medium (militia set, AKM / SKS / RPK / FAL / G3 class), bandit heavy
  (militia + steel helmet, PKM / RPK74 / RPD / SV98 / SVD); soldiers keep
  their side but get camo sets and NATO rifles; Soviets are the enemy
  military tier (spetsnaz set, AK74 family, PKP / RPK74 / SVD, more
  health, takes less damage). Machine guns fire faster, sniper rifles
  slower. Military areas (our MIL sectors and MILITARY zones) shift
  bandits to the heavier tiers.
- MilitarySpawner: near a player standing in a military area, groups of
  2 to 3 enemy military spawn 24 to 48 blocks away now and then, up to a
  cap nearby (not in peaceful).
- Juggernaut: step 2, same tier system (juggernaut set, PKM / M240 /
  Barrett, much more health).

Step 1 DONE in v0.30.0 (9 Oktober 2026), `fixes/NpcLoadouts`,
`fixes/MilitarySpawner`, `fixes/NpcKind`, Deci npcKind / npcGun /
setNpcGun / setNpcShotDelay / newSoviet; dev test mode `npc`
(`python3 tools/devtest.py npc`, about 70 s), all PASS:
- 60 bandits outside / inside a military sector: every one tiered with 4
  armor pieces, the tier's gun and health; medium + heavy 22..29 of 60
  outside, 38..47 inside. 12 soldiers, 12 Soviets: same checks.
- A player's 10 damage gun hit takes 10.0 (bandit light), 7.0 (bandit
  heavy), 6.0 (military).
- Client shows the server's gun for all 14 lineup NPCs (before: each side
  rolled its own). Military spawner places 2 to 3 tier military NPCs.
- checks + tracer modes still PASS (tracer 3.6 deg mean).
- Untiered soldiers in some runs: explained 9 Oktober 2026, Decimation's
  PlayerJoinSync cancels 5% of soldier spawns and spawns a Mech instead
  (also why mechs gather in the reused test world); the test now skips
  refused spawns.
Defaults (config/deciworldgen_npc.cfg): bandit light 20 hp, takes 100%,
weight 55 (military 25), Decimation's 5..20 tick delay; bandit medium 26
hp, 80%, weight 35 (45); bandit heavy 32 hp, 70%, weight 10 (30);
soldiers 24 hp, 80%, delay 2..6, camo marine / forest / urban / black;
military (Soviets) 40 hp, 60%, delay 3..9. Machine guns 3..8 ticks,
sniper rifles 25..45. Spawner: every 400 ticks per player in a military
sector, chance 0.5, group 2..3 at 24..48 blocks, cap 4 within 64.
Not seen by the user in game yet `[not verified]`; balance numbers are
first guesses for "hard but fair".
v0.33.0 (user: "remove vanilla mobs since this is a zombie mod";
`fixes/VanillaMobs`, config deciworldgen_mobs.cfg): vanilla zombies,
skeletons, creepers, spiders, cave spiders, endermen, slimes, witches,
silverfish, zombie pigmen, pigs, cows, sheep, chickens, horses, wolves,
ocelots, mooshrooms, squid, bats, villagers, iron golems and snow golems
leave every biome's spawn lists but Hell and Sky (postInit, after
DecimationBiomes.copySpawns) and are refused (and marked dead, so old
chunks drop them) when they join the overworld (spawners, villages, eggs,
breeding, old saves). Exact class match: Decimation's mobs that extend
vanilla classes stay. Config: removeVanillaMobs, keep (entity names).
Dev tests tag mobs they need with VanillaMobs.KEEP (the shots test pig).
Checked: checks mode 5 / 5 vanilla mobs refused, infected still spawn, 0
vanilla spawn entries; fresh seed 1 world: saved entities only Decimation's
(Bandit 43, Human 17, Boar 17, Hazmat 9, Buck 6, Doggo 2, Soviet, Mech).
Also: DecimationBiomes.copySpawns told vanilla animals by package name,
which only works in dev (the shipped game is obfuscated); it now also
checks the class. Juggernaut armor (user question): ArmorGunfireHandler
covers every ItemArmorDeci, juggernaut pieces included (body x 0.022 after
the 35% buff: an NPC rifle hit about 0.22 hp, a Barrett about 4.4)
`[not verified]` in game, computed from the formula the marine numbers
matched.

v0.32.2 (user: snipers should spot players further than infantry):
Decimation's findTarget looks 32 blocks around (15 up / down) for every
armed NPC. NpcLoadouts.sniperSearch: once a second, an NPC holding a sniper
rifle without a target looks sniperRange (96, config npc_fire) around (40
up / down) for the nearest enemy it can see (isHostileTo) and takes it as
its target (setRevengeTarget; vanilla drops it after 100 ticks, the search
finds it again while in sight). Sniper shots fly 160 blocks (others 96).
Checked (shots mode, range phase): enemy bandit 70 blocks away, elite
sniper spots it, elite machine gunner does not.

v0.32.1 (user: Barrett for the elite, split sniper eggs, "don't make it
rare"): juggernaut and elite_military are machine gun only now; new tiers
juggernaut_sniper (juggernaut set, Barrett with 8x, always) and
elite_sniper (elite gear, Barrett 4 in 8 / L115A3 / JNG90 / SV98 / M110,
8x, suppressor, laser, x2 damage). MilitarySpawner: sniperShare 0.5 of its
juggernauts / elites are the sniper version: a Barrett in about 12% of
groups (was 3%). Elite Barrett hit: 6 x 5 x 2 = 60 hp bare, about 23
through a marine body set (armor piercing). 14 eggs. Checked (npc mode):
40 / 40 juggernaut_sniper Barrett, 40 / 40 elite_sniper sniper rifles, 0
snipers in the base tiers, 14 / 14 eggs, client guns 24 / 24, goggles and
attachments 6 / 6; photo npc_snipers_v0.32.1.

v0.32.0 (user requests 9 Oktober 2026):
- ELITE MILITARY, tier "elite_military" (Soviet side): marine black set,
  night vision goggles (mask "nvgoggles"), heavy machine guns (PKP, M240,
  MK48, MG3, PKM) with 4x sight, MG suppressor and laser, or sniper rifles
  (L115A3, JNG90, SV98, M110) with 8x sight, rifle suppressor and laser;
  150 hp, takes 16% (about 59 rifle hits, 2 magazines), its hits x2 on a
  player (tier damageDealt, on top of x5), speed 0.27, knockback
  resistance 0.5. Only MilitarySpawner (eliteChance 0.2 per group) and its
  egg. The gun spec "name;sight=4x;barrel=..;grip=..;mask=.." is stored in
  ForgeData and synced through watcher slot 26, so the client shows the
  attachments (Decimation's NBT keys sightAttach / barrelAttach / ...) and
  the goggles (HumanEntity.setMask).
- AUTO FIRE: guns with an AUTO or BURST fire mode fire bursts (3..6 rounds,
  machine guns 6..12) at the gun's own rate (GunStats.secondsPerShot), each
  shot's spread x (1 + 0.35 x its place in the burst), then a pause (3 x
  the tier's delay). Sniper rifles stay single shot.
- MAGAZINES (user: "M16 30 rounds, then reload around 4 seconds"): an NPC
  fires its gun's magazine (Deci.gunMagazine: the first ammo item's
  capacity, M16 / AK 30, PKM belt 250; loose rounds or clips: the gun's
  capacity), then reloads reloadTicks (80, +-10) with the MagOut sound.
- Config category npc_fire: autoFire, recoilSpread, reloadTicks.
- Checked (dev test modes, fresh world, 39 PASS): elite 1.6 of a 10 hit,
  elite hit = 2 x normal, goggles and attachments on all 3 client lineup
  elites, 12 eggs; shots: AKM 12 bursts / 53 shots, first shots hit 7/12
  at 20 blocks vs 3/18 from the 4th on, first magazine 30 rounds then 82
  ticks; tracers 0.2 deg from the shot line. Photo npc_elite.png.
  Test trap: calling shootAt from a test while the AI also calls it
  halves every cooldown (reloads looked 2 s); the burst phase lets the AI
  shoot alone.

v0.31.0 (user choice, step 2 of the NPC plan): the JUGGERNAUT, tier
"juggernaut" (Soviet side, so hostile to every player): one matching
juggernaut set (normal or gray), PKM / PKP / M240 / MK48 or a Barrett,
200 hp, takes 25% of a player's gun damage (a 16 damage rifle hit does 4:
about 50 hits, near two magazines), walk speed 0.18 (others 0.25),
knockback resistance 1. Never rolled at random (weights 0): only
MilitarySpawner brings one (juggernautChance 0.15 per group, so only in
military sectors) and its egg. Barrett hits are armor piercing
(NpcShots.armorPiercing): ArmorGunfireHandler applies the square root of
the armor multiplier. Barrett hit (50 / 8 = 6, x5): 30 hp bare (one shot
kills), 11.6 through a full marine body set (normal 6 hit: 4.5). Checked
(npc mode, all PASS): 2.5 of a 10 hit, set / speed / knockback, piercing,
11 eggs, spawner group; lineup photo npc_3.

v0.30.4 (user request): rocket launchers. Tiers bandit_rpg (RPG-7,
weight 3, 8 in military sectors, 30 hp, takes 75%) and military_rpg
(RPG-18 standard rockets, about 1 in 10 Soviets: military weight 9 vs 1,
40 hp, takes 60%); a config written before (version != "2") gets the new
weights once. NpcShots fires a real RocketEntity (the player's rocket:
speed 1.5, gravity 0.002 per tick, smoke trail, explosion 6 without block
damage), aimed at the chest plus the gravity drop over the flight, spread
1.5 x the tier's; 60..100 ticks between rockets; no rocket when the target
is closer than 8 blocks or an ally stands within 1.5 of the line or 5 of
the target (a rocket explodes on anything). Fire sound deci:<gun>Fire.
Checked (shots mode): 7 to 8 rockets in 15 s at 24 blocks, 3 to 5 hurt the
pig; close shots show the launcher in hand and the rocket with its smoke.
New tiers sit at the end of the list (egg metadata = index), 10 eggs now.

v0.30.1 (user request): one spawn egg per tier, `deciworldgen:npc_egg`
(`fixes/NpcEgg`), metadata = index in NpcLoadouts' tier list, so new
tiers (juggernaut) get an egg on their own. Creative tab Misc, names
"Spawn Bandit (heavy)", "Spawn Soldier (marineforest)", "Spawn Military";
vanilla egg look, base colour per faction (Decimation's own egg colours),
spots per tier. Test mode npc: all 8 eggs spawn their tier.

### Military bases, US FOB style (v0.42.0, 10 Oktober 2026, waiting for the user's review)
User: "military zone with buildings ... our own generator, all three sizes, US FOB style ... really
really good output ... work on loots especially props placement". worldgen/military: combat
outpost (50 x 58), FOB (78 x 84), large FOB (112 x 118) in military sector sites (45 / 35 / 20 %),
turned 0..270, ground ramped around them, MILITARY zone. Perimeter (wire, HESCO, towers, ECP with
serpentine), TOC, B-hut living area with bunkers, logistics (ASP, fuel, motor pool, conexes,
hangar), services (DFAC, aid station, gym, helipads), loot in Decimation's crates. Details:
docs/military_base.md; skill decimation-military-base.

### Requests for later (10 and 11 Oktober 2026, the user: "save this feat/fix later")
Not started unless marked DONE; each one also has a line in docs/roadmap.md.
- **Gunshot noise** (DONE v0.43.0, section "Gunshot noise"): a shot from a gun without a suppressor draws attention; zombies walk to
  where the shot came from, NPCs react too; suppressed shots stay quiet.
- **Sniper NPC nerf** (DONE v0.42.5): sniper damage about 20 % lower, sniper range 70 blocks (config
  `sniperRange`, 96 now; the user said 90); Barrett NPCs (juggernaut_sniper, elite_sniper)
  about 40 % lower and health cut so one 5.56 magazine kills them.
- **NPC loot system**: what NPCs drop on death, per tier, how it works, rewards; design first
  with the user.
- **Smart NPCs**: open, ideas to be collected with the user.
- **Human Kills** and the **main menu Play button**: DONE in v0.42.2 (bug.md, Fixed).
- Military jeep / tank wrecks breaking with one punch: DONE v0.42.5 (bug.md).

### Gunshot noise (v0.43.0, 11 Oktober 2026)
User request (10 Oktober: "if player shot with non suppressor gun, it will draw attention to player
and zombie will be go to direction where the shot from. this applied to npc too"); choices
(11 Oktober): one range for every gun; zombies go to the spot and search; hostile NPCs
investigate; NPC shots draw zombies too; "if the shot from their team or group, they dont need
to go to direction ... except if its from the enemy or rival".
- fixes/GunNoise: a shot is heard 64 blocks away, 12 with a suppressor (barrelAttach on the
  gun, NPC guns too). Player shots: the held gun's ammo NBT drops (no packet hook); NPC shots:
  NpcShots. A burst is one noise (10 ticks, 8 blocks).
- Who reacts: every infected (InfectedEntity and the infected dog) in range; armed NPCs in range
  only when Decimation's isHostileTo says the shooter is their enemy (so their own side, and a
  player they do not fight, e.g. a bandit vs a player under humanity 50, are ignored).
- How: an AI task (Investigate; infected priority 3, NPCs 2, before their wander) walks to the
  spot in 12 block legs, then to random points within 6 blocks of it; search time 30 s
  (infected) / 40 s (NPCs). A mob with a target fights it instead.
- Config: config/deciworldgen_noise.cfg (enabled, range, suppressedRange, zombieSearchSeconds,
  npcSearchSeconds).
- Checked (dev test noise, 6 checks PASS): an infected 50 away comes 32 closer, one 76 away does
  not; a suppressed shot does not move one 30 away; a Soviet 43 away walks 32 toward a bandit's
  shot, a bandit does not; a real player shot (ammo 30 -> 29) brings an infected from 45 away.
  Not measured: frame time with many infected walking to a firefight [not verified].
- User test of 0.43.0 (11 Oktober): "enemy npc do nothing when me shooting near them", zombies
  react. Two causes (v0.43.2):
  - The user was in Creative. Decimation's isHostileTo is false for every creative player
    (bandit, soldier, Soviet, mech), so no NPC fights or investigates a creative player's shots;
    kept as Decimation's rule. In survival: Soviets react to everyone, bandits to humanity 50
    or more, soldiers and mechs to humanity under 50.
  - Bug: an NPC that once killed something kept the corpse as its old style target
    (entityToAttack is never cleared under the new AI) and never investigated again; Investigate
    now clears dead targets (bug.md "NPCs that once killed something ignored gunshots").
  - v0.43.3 (user: the juggernaut still did not react in survival): entityToAttack keeps the
    last enemy seen even while it lives on far away; busy now reads only the revenge target and
    the new AI target (bug.md "Enemy NPCs ignored gunshots, juggernaut too").
  - Accepted by the user on 0.43.3 (11 Oktober 2026): "nice it work now".
  - The dev test noise (v0.43.2) had 8 checks: a survival player's shot brings a Soviet from 40 away
    until it sees the player within 32 and fights (heard, came 12, engaged); a creative player's
    shot does not; the friendly bandit check reads whether it took the noise. The test removes
    mobs it did not spawn (stray infected on the arena made every NPC fight them). All PASS.

### v0.42.4: fourth review (11 Oktober 2026)
Ladders walkable from the base on every tower (5 of 6 were walled in), the outpost's TOC back,
a walk test for every base (ladders, doors, rooms, loot, traps; tools/props/walkcheck.py).

### v0.42.3: third review (11 Oktober 2026)
Tower ladders reachable (middle of the inner side), hangar roof closed with loot inside, motor
pool shelter with a full wooden roof; the "render issue" was worldgen below the sky test cutting
it (fixed in the test, docs/military_base.md section 6).

### v0.42.2: second review (11 Oktober 2026)
User list: AS Val sight position, TOC door side loot, barracks chests, DFAC radio, a busier
garage, the main menu (no session warning, green Play, local stats). Done: sights shifted onto the
AS Val's receiver with the aim compensated; Human Kills / Zombie Kills / Deaths kept locally
(fixes/LocalStats) on the HUD and menu, labels renamed (tools/patches/PatchMenuStats.java);
the base items (docs/military_base.md section 5). Checked: dev tests stats, gunview, milbase
(0 clashes).

### Military bases v0.42.1: user review fixes (11 Oktober 2026)
User review of v0.42.0 (28 shots, docs/shots/milbase_v0.42_review/): fix the orange dirt, the
raw dirt ASP berm, replace BlockMilitaryBarrier (FPS), make the TOC door a locked keycard door,
and every prop position. Done: deciworldgen:hesco (own texture), Door_Emergency_1_Locked + the
military keycard screen, gravel ground, sandbag ASP, the prop placement study
(docs/prop_placement.md, tools/props/propgeom.py + propclash.py, skill decimation-props),
Canvas.validateProps in the generator, every module re-laid (0 clashes). Tile entity props per
base: 1020 / 2007 / 2902 -> 129 / 381 / 571.

### EOTech 558, ACOG TA11 3.5x, Mk18 TS stock (DONE v0.41.0, 10 Oktober 2026, waiting for the user's verdict)
User: holographic 558 and ACOG 3.5x "so detailed ... same like in real life", the Mk18 with an IMI
Defense TS stock. tools/guns/sights.py (our art, committed): EOTech 558 (92 parts) and ACOG TA11
(117 parts) as deci:eotech558 / deci:ta11acog (NewSights, loot like the red dot / 4x), reticles
drawn in the glass while aiming (ScopeZoom: 1.25x red ring and dot, 3.5x BDC with green centre),
on every gun's rail (sightfit). Mk18: TS stock from the user's photo. Details: skill case 18.
v0.41.1 (user review): reticles drawn in the glass and swaying with the gun (SightReticle), EOTech
ring smaller (32 px pixel art), HK416 front sight folds away under a sight (skill case 19).
Verdict (user, 10 Oktober 2026): "its really good result"; front sights fold away only with a
sight attached (kept without one).

### Mk18 Mod 1 and iron sights that fold away (DONE v0.40.0, 10 Oktober 2026, waiting for the user's verdict)
User: "mk18 mod 1, black only", 9 photos; irons ready for attachments and hidden when one is
fitted. Mk18 from Decimation's M4A4 (tools/guns/mk18.py, generated locally, git ignored): DD RIS
II rail, flip up front sight, short barrel and flash hider (muzzle 16.1 vs 19.15), flamePos
moved, suppressor offset; registered as deciworldgen:mk18 (STANAG, damage 15, 800 rpm, recoil 7.5
/ 0.3, slowdown 0.12, M4A4 sounds and loot). Mechanic: every `defaultScopeModel*` part of a gun is
hidden while a sight is attached (fixes/IronSights, tools/patches/PatchIronSights.java): our Mk18
and 19 Decimation guns. Details: skill casebook case 16.

### HK416 and HK416 Tan (DONE v0.39.0, 10 Oktober 2026, waiting for the user's verdict)
User: "try hk416 ... make 2 version, black and tan", 5 photos; "two separate guns" (asked:
skin spray can vs two guns vs both). Variant route like the UMP9: tools/guns/hk416.py builds
both from Decimation's M4A4 (generated locally, git ignored). Kept: receiver, trigger group,
magazine, folding rear sight (aim and hands stay the M4A4's). Dropped by position (155 parts):
quad rail, gas block and A-frame, barrel and birdcage, collapsible stock, A2 grip. Ours (121
parts): HK rail handguard flush with the receiver rail (teeth y -3.6, 0.4 pitch; side and
bottom rails, slots), tall HK folding front sight (post tip -4.7 like the M4A4's), thin barrel
on the M4A4 bore and HK flash hider to the same muzzle x 19.15, HK slim line stock (ribbed
butt, angled panel with fins, buffer tube), raked HK grip with finger bumps and panels.
Lengths from the user's side photo (rear sight and muzzle as anchors: the butt then lands
within 0.03 of the M4A4's). Tan: the same model, the texture's islands recoloured flat dark
earth (each island brought to one tone, its gradation kept) except sights, barrel, muzzle,
trigger, bolt. Registered as deciworldgen:hk416 / hk416tan (STANAG: m4a4Mag and our 60 round),
damage 16, 800 rpm, recoil 6.5 / 0.2, the M4A4's sounds, loot where the M4A4 is.
Checked: renders next to the M4A4 (docs/shots/hk416_v0.39), aim crops with the M4A4 (rear
aperture and post tip on the same line), arSuppressor at the M4A4's place, red dot, reload 30,
gunperf within noise of the M4A4.
v0.39.1 (user review: "flying" pieces under the stock, front sight "like 2 pillar"): stock
lower panel rebuilt (square rear part plus angled wedge, ribbed band placed from the edge's
formula), every detail seated (new `study.py contact`: 46 parts not seated before, 1 after,
the flash hider collar); the "pillars" were the M4A4's rear sight reading texture rows past
its 32 high texture, which our taller texture filled with our islands (fixed: wrapped rows
copied, ours start below); front sight now a U of thick flared ears on a bridge, tops at
-4.74. Aim crops match the M4A4 again (docs/shots/hk416_v0.39.1). Skill case 14, lessons 15
and 16.

### UMP9 (DONE v0.38.0, 10 Oktober 2026; user: "the ump9 work really well and no problem")
Built as the user suggested: Decimation's UMP45 generated locally from
their Decimation.jar (tools/guns/ump9.py, outputs git ignored) with our
own curved 9 mm magazine (15 parts); registered only when the generated
model exists; ump9Mag 30 rounds (MP5A3 bullet and icon); damage 11, 650
rpm. Checked: hip / aim identical to the UMP45, reload 30. Details:
skill casebook case 10. The notes below were written before the
shortcut.
User request: the 9 mm UMP, Decimation has only the UMP45 (straight .45
magazine); the UMP9's magazine is curved like the RPK / AKM ones. Built
with the decimation-gun skill (second gun).
Reference measurements (Decimation's ump45, numbers only, nothing copied):
19.7 long, 9.9 high, 2.0 wide, 142 parts; x -5.35 to 14.3 (skeleton stock
extended, receiver about x 2 to 12.5, top rail, front sight hood at the
front, rear sight at the rear top, railed handguard with vents, short
barrel with a muzzle cap); magazine in front of the trigger guard at x
7.4 to 9.9, straight, leaning forward, 2 parts; flamePos (13.9, -3.75,
-0.15), rhPos (-5.6, 2.62, -2.0), lhPos (6.5, 8.12, 4.62); stats damage
13, 600 rpm, recoil 6 / 0.4, recovery 5, slowdown 0.14, AUTO / SINGLE.
Curved magazine recipes: AK74 11 parts, MP5A3 4 segments shifted forward
each (children of ammoModel0). Renders docs/shots/ump9_v0.38/
(ref_side.png, ump45_*.png). Waiting for the user's photos (lesson 1).

### MAC-10 gaps closed (v0.37.3, 10 Oktober 2026)
User: "perfect is perfect, we close the gaps until the mac10 looks really
like decimation guns". New `study.py gaps` (ours vs the 13 Decimation
SMGs). Shape kinds 60 / 31 / 5 / 5 % (Decimation 60 / 31 / 5 / 4), 110
parts, declared sizes like Decimation's (gunmodel.size / layout: long
parts 1x1xN with gradation along them), tone 47, middle details from the
photos (rivets, housing plates, SAFE / FIRE lever, deflector, slot
notch), raised details 0.08 proud. Only the short real length and what
follows from it stay outside. Checked: geometry unchanged by the size
change, aim holes on the line with the Uzi, suppressor unchanged, reload
30. Details: skill casebook case 9.

### MAC-10 accepted (v0.37.2, 10 Oktober 2026)
User verdict in game: aim fixed, "i like the style of the gun firing",
icon "good art". Screenshot docs/shots/mac10_v0.37/user_v0372_final.png.
Player firing, fire / rack animations in first person and the icon are
now verified by the user.

### MAC-10 v2 revision 2 (DONE v0.37.2, 10 Oktober 2026)
User review of 0.37.1 with screenshots (MAC-10 and Uzi aiming, MAC-10
hip view with the suppressor): suppressor not matching the muzzle and
still low; aim compared with the Uzi; asked that every revision updates
the skill.
- Suppressor: flamePos y must sit 0.85 above the bore (Decimation's
  convention, Uzi / UMP45); now -4.75 over the bore at -3.9. Offset
  (-2.37, -0.16, 0): it screws over the threads up to the receiver front
  like the real one (user photo 2), centred on the bore.
- Aim: the user's Uzi shot shows the centre in the Uzi's rear aperture
  hole; ours sat 0.25 higher. Rear aperture hole now -4.8 to -4.55, front
  post tip -4.67, stock loop top lowered to -4.57. Checked in the same
  gunview run as the Uzi: both holes on the centre line.
- Skill .claude/skills/decimation-gun: photos first, corrected anchors,
  side by side aim check, attachment check, revision log; style guide
  sections 9, 13, 14 corrected.

### MAC-10 v2 revision (DONE v0.37.1, 10 Oktober 2026)
User review of 0.37.0 ("huge upgrade"): 1. aim not centred on the
crosshair, 2. suppressor flying, 3. no gradation like Decimation's guns.
- 2: the barrel attachment formula meets the muzzle only on guns with
  flamePos x 12 to 15; the MAC-10 gap was 1.12 units (also in the user's
  shot). New `Deci.offsetAttachment` (our copy of the attachment model in
  AttachmentItem.ST with an offset), NewGuns moves smgSuppressor by
  (-1.02, +0.14, 0). Checked in game: flush on the muzzle (bandit side
  view, first person).
- 3: gunmodel.paint gradation from measured Decimation textures (style
  guide section 4): 37 part tones, face spread 5.2, noise 2.1.
- 1: steady aim (gunview, camera held) puts the sight tops on the screen
  centre exactly like the Uzi, and the rear sight matches the Uzi's
  build; the user's shot shows them 0.2 units high, which matches
  Decimation's aim sway after a mouse move `[inferred]`. Not changed;
  asked the user to compare with the Uzi in the same moment.

### MAC-10 v2 in Decimation's style (DONE v0.37.0, 10 Oktober 2026)
After the user's "not good, needs polish" and the gun study
(docs/gun_style_guide.md, skill .claude/skills/decimation-gun).
- tools/guns/gunmodel.py v2: every part a hexahedron (box bent with inset
  / shift / mirror / octagon), written as Decimation does (1x1x1 declared,
  shape in the corner offsets), one flat tone per part (textureWidth 512,
  UV step 8, 2 px a unit), icon from the model's side render, animations
  with Decimation's timings (Fire 2 frames slide only, Reload1 57,
  Rack 19 Hand 1, new SlideBack static), our own keyframe values.
- tools/guns/mac10.py v2: 102 parts (93 body, 4 slide: knob halves with
  the sight notch, stem, bolt in the ejection port; 5 magazine), 9.35 x
  8.1 x 1.5 units (v1: 23 plain boxes, 12 x 10.2 x 2.2). Proportions
  measured on the user's side photo (receiver = 7.0 units): raked grip
  behind the mag housing, threads right at the receiver, thin barrel,
  knob forward, front sight, strap loop, butt pad, the folded wire loop
  over the rear top, left side stamping and pins, ejection port with the
  bolt on the right (-z). Sight tops y -4.95 to -5.0, receiver top -4.45.
- Checked: study.py renders next to the Uzi; in game (gunview, gun):
  dark slim gun where the Uzi sits, the rear seen inside the wire loop
  frame (no grey block), aim centre exactly at the sight tops (v1: 0.75
  above), red dot on the receiver and its ring at the aim centre,
  suppressor on the muzzle, flashlight; reload loads 30 (animation:
  tilt, magazine out and in), a bandit holds and fires it. Not checked:
  the player firing it (mouse) and the feel `[not verified]`; icon only
  seen as a file.
- Shape kinds 52 % taper / 44 % cuboid / 2 / 2 (Decimation 60 / 31 / 5 /
  4): more tapered chamfers would move it closer `[idea]`.

### MAC-10, the first gun of our own (DONE v0.36.0, 9 Oktober 2026)
User: a gun made by Claude with tools (docs/roadmap.md pilot); user picked
the MAC-10. Pipeline in docs/gun_model_spec.md section 6. Model: 23 boxes
of our own (boxy receiver with ejection port, threaded barrel, sights,
checkered grip with the magazine inside, trigger guard, strap lug,
retracted wire stock, cocking knob as slideModel0), texture and icon
painted by script, animations Fire / Reload1 / Rack written by us. Gun:
smg, .45 ACP (the UMP45 / Uzi rounds), 30 round mac10Mag (Uzi mag icon),
1100 rpm, damage 12, slowdown 0.08, the Uzi's sounds; in the loot pools
of the Uzi (8) and its magazine (8), and in the light bandits' guns.
Checked (dev test mode gun): registered, takes its magazine, the reload
key loads 30 rounds (control: the Uzi loads its 32 the same way), a
bandit holds it (third person) and fires it (tracers); first person it
renders where the Uzi does, lighter texture after a first run that was
almost black. Not testable here: the player firing it (mouse read
directly) `[not verified]`; how it looks while aiming and the fire / rack
animations in first person `[not verified]`. Tools: headless Blockbench
MCP (registered for later sessions, `claude mcp add blockbench`), our
client tools/bbmcp.py for this session.

### 60 round magazines (DONE v0.35.0, 9 Oktober 2026)
`fixes/Magazines`, Deci newMagazine / gunTakes / addGunMagazine /
addLootLike. Read from deobf: a magazine is an AmmoItem (deci.ay.f: name,
feed type mag, capacity, stack 3, bullet item adI) registered by its own
constructor; a gun keeps the mags it loads in ammoTypes (deci.ay.i.aep,
copied at construction: the AK family shares ak74 / aks74u / rpk74 / ak12
mags, the M16 family the STANAG list); its max ammo is the loaded mag's
capacity. Loot: LootTableRegistry (DecimationMod.getLootTable) builds item
lists per container block in Decimation's preInit (entries awA, package
private class, lists awC), then pools (init).
- 60rnd NATO STANAG Magazine (deciworldgen:stanag60Mag, 5.56): M16A1 /
  A2, M231, AR-15, XM177, Honey Badger (Decimation's STANAG list) plus M4A4,
  ACR, L85A1, SCAR-L (own mag in Decimation, STANAG in reality). Not FAMAS,
  G36C, AUG, Galil, INSAS. Icon of m16a2Mag.
- 60rnd AK Magazine (deciworldgen:ak60Mag, 5.45): every gun taking ak74Mag
  (AK-74, AKS-74, AKS-74U, AK-12, RPK-74, and AKM / AKMS as Decimation
  shares that list). Icon of the longer rpk74Mag.
- Names in assets/deciworldgen/lang/en_US.lang (also names the egg items).
- Loot: one in every pool already holding the 30 round mag (8 STANAG pools,
  7 AK pools). Black market trader offers not added `[not decided]`.
- Checked (checks mode): right guns take them, wrong ones not, capacity
  60; hotbar photo npc_mags (names, icons). Reloading a gun with one in
  game `[not verified]`.

### Wasteland district = military area (DONE v0.54.0, 12 Oktober 2026)
User: "go with 1, wasteland military area. do full system now" (docs/ideas.md item 14). Full
design, checks and code map: docs/wasteland.md. DeceasedCraft checked first (legacy 5.5.5 data):
its wasteland holds the military base, camp and bunker with military loot, the rest industry with
industrial loot. Ours: the whole district one MILITARY zone, military buildings more often (new
worlds), military loot in its containers, army supplies, wartime stories, zone entry checkpoints,
military vehicles, fighting positions, military lot scenes, enemy military groups on its streets.
Dev test `wasteland`. v0.54.1: the weak spots (alike military buildings, few lots, unseen lot
scenes, patrol spots) fixed, docs/wasteland.md "v0.54.1". v0.54.2: motor pool and crash site
scenes reworked (docs/wasteland.md "v0.54.2"). Waiting for the user's look.

### Rifles for the elite and the juggernaut, Decimation doors, city edge, locked wasteland (DONE v0.55.0, 13 Oktober 2026)
User: "elite npc and juggernaut have rifle now, from marksman and normal automatic rifle, combined with
attachement (50% change) full or half attachment. and add gradient texture block on border of
wildneres cause its looks not natural when see from my view. door never use vanilla ones, use
decimation door instead on all world in decimation. for wasteland, 80% most building its locked door,
so if the building has more loots + high value in there, you put locked door decimation in there".
Choices asked: the border is the city edge (not dead land vs overgrown), locked doors open with a
keycard screen or a lockpick, all four in one release.
- Rifles (fixes/NpcLoadouts): juggernaut and elite_military carry a rifle 6 in 10 (config
  `rifleShare` per tier): automatic (ak74, ak12, akm, m4a4, hk416, mk18, fnscarl, g36c, acr, aug3) 6 in
  10, marksman (m110, svd, m14, galilsar, fnscarssr) the rest; their machine guns otherwise. A rifle's
  attachments (rifleAttachments): none 1 in 2, else a full set (sight, arSuppressor, grip) or half of
  one (1 or 2 slots); marksman sights 4x / 8x / TA11 ACOG (the SVD its Dragunov scope), automatic red
  dot / 2x / EOTech / ACOG / 4x; grips foregrip / laser / flashlight; no sight on a gun with a built in
  scope. Dev test `npc`: 121 of 200 with a rifle (none 62, half 35, full 24), the client gets the
  server's attachments; `gunview guns=m110,ak12 attach=4x,arSuppressor,foregrip`: attachments drawn on
  the NPC's gun. Snipers tiers unchanged (the Barrett, elite_sniper).
- Decimation doors everywhere we generate (worldgen/Doors): every vanilla wooden / iron door written
  by Slices or the small schematic placer becomes a Decimation door with the same metadata, styled
  per structure (city buildings by kind: homes dark wood / blue / orange / green, offices dark wood /
  blue, shops coloured with windows, clinics green / emergency, police blue / security, industry and
  military metal / security / emergency, labs security; bases metal, camps dark wood, the refuge
  emergency / metal; iron doors metal). Seed 1 area: 0 vanilla doors. Chunks generated before keep
  theirs.
- City edge (city/EdgePlan.wear): the ramp's grass within 10 of the city turns into broken pavement
  in patches, denser at the city: the sidewalk's slab and stone next to it, then cobblestone, then
  mossy cobblestone. Dev test `cityedge`: worn tops by distance 62 53 41 26 14 7 1 0 of 64.
- Locked wasteland (BuildingDress.lockDoors, docs/wasteland.md "v0.55.0").

### The wasteland city apart, its fence, the bunker; ramps, bridges, stations (DONE v0.56.0, 13 Oktober 2026)
User: bunker "photo the bunker and make it lockable ... highest loot ... most harder place building with
full of high tier enemy npc in there. elite npc + juggernaut and full military outfit zombie spawner";
"make full covered fence using decimation block? with barbed wire and full of landmine on the edge side
of city. this is only applicable on wasteland city"; "cars portal in there like toll gate and military
post gate"; answers: the fence on the wasteland district square, "split out the wasteland city with other
city", chain link + wire, asphalt ramp for the street stairs, bridge damage "gradient, dont put retacle
hole"; and the subway station "on side building instead on road". Details: docs/wasteland.md "v0.56.0",
docs/city_engine.md "Open" (v0.56.0). The city edge gradient of v0.55.0 accepted ("i like it").

### Main menu: server pages coming soon, Early access, Chernarus (DONE v0.64.1, 17 Oktober 2026)
User: "for server status, changed it like this: Global Server, Community Server, Download, with that name 3 of them
for the offline"; "for my supporter, change the name to Early access"; "for server that is down, replace it with
Bukovka and Chernarus only and its say coming soon, same with player online its say coming soon instead of unknown,
with same community to when notification appear, its say community server coming soon instead of failed to load";
five Chernarus pictures. Official list rows from fixes/MenuServers (PatchOfficialServers), texts by PatchMenuTexts (47
in 12 classes), Chernarus pictures and the player count line by tools/menu_assets.py. Dev test menupages RED (every
check) then PASS 9 of 9; menu, stats, mainmenu PASS. Later: a forum on our website (roadmap UI 3), real servers (UI 4).
Details: docs/main_menu.md section 2c.

### Main menu: links, our name and version, Continue, our corner (DONE v0.64.0, 17 Oktober 2026)
User picked from suggestions: "Links to our site", "Replace FURY advert", "Our version shown" ("for version replace
version code with our version code name, and for decimation replace with deciremnant"), "Continue last world"; and
"replace the picture of news panel to real topic image". Website / GitHub links, DeciRemnant and our version in the
title line, under the logo, the welcome box and the window title, Continue under Play, our corner art, topic pictures.
Dev test mainmenu RED then PASS; menu, stats PASS. Details: docs/main_menu.md section 2b.

### Main menu: our news in Decimation's news panels, real scene slideshow (DONE v0.63.0, 17 Oktober 2026)
User: "lets make the news panel now. 1. github repo, 2. update release, 3. our website release, 4. Coming soon
feature"; "replace with real scene with your suggestion animation"; "just use component from decimation gui menu its
self, no need to add something new on it"; "make the background image, changed every 7 second". Built with Decimation's
own parts: four of its news panels (ArticlePanel) in its own news grid, its own background drawing with six real scenes
of our world, 7 s each with a fade, the black layer and lava cube planet removed (PatchMenuBackground, three jars).
Checked in the dev client (dev test mainmenu, RED then PASS twice; menu and stats still PASS). Waiting for the user's
look in game (needs a release: their Prism has the old jars). Details: docs/main_menu.md.

### Zones, factions and world (user list 9 Oktober 2026, "later")
Only the list and starting points; scope of each is `[not decided]`, ask
the user before building.
1. Advanced safezone. Exists: Decimation zone type SAFEZONE (ZoneManager,
   infected spawns cancelled inside; explosions skip it, RocketEntity
   checks it); our ZoneStore tags generated structures, no safezones yet.
   User choices (11 Oktober 2026, after the bridges): "create safezone maybe
   that have trader + guard inside"; where: BOTH one per city and own sites
   outside cities (roadside / countryside camps); look: BOTH a walled
   survivor camp built by our own generator (FOB style: scrap and sandbag
   walls, guarded gate, trader stalls, tents, campfire, medical tent) and a
   fortified city building (barricaded, traders on the ground floor);
   guards friendly unless provoked (they shoot infected, bandits and
   Soviets near the zone, turn on a player who shoots inside or hits
   someone); people inside: traders, guards, unarmed civilians and armed
   survivors (item 5 joins this). Next: research the existing pieces
   (Decimation trader entity, civilian FactionHumanEntity, faction rules, zone
   registration) and a plan for the user.
   What Decimation already has (deobf/names.tsv, 11 Oktober 2026, read from
   the names table only; behaviour `[not verified]` until the classes are
   read):
   - Traders: TraderEntity deci.ai.a (FactionHumanEntity, static offer list
     per class, opens TraderScreen; PacketTraderTransaction buys / sells for
     caps or gold caps) and 19 subclasses: ammunition, armor, backpack,
     banker, black market, bounty, car dealer, convenience, drug dealer,
     food, fortification, gold cap, GuardTrader deci.ai.n ("armed guard NPC
     protecting a safezone"), guitar player, guns, heavy guns, medical,
     scrap, skins (+ Santa / witch). ArmedTraderEntity deci.ai.v shoots
     infected and bandits within 32 blocks with its held gun.
   - Placement on the official server: TraderSpawnManager
     (net.decimation.mod.server.traders, decimation_traders.json, removes
     all trader entities and respawns them), server proxy only: in
     singleplayer we place and keep them ourselves.
   - Safezone rules: SafezoneAttackHandler deci.aK.g (cancels attacks in
     safezones and in the leave grace timer), FriendlyFireHandler deci.aK.i,
     EntitySpawnZoneHandler deci.aK.d (blocks hostiles in safezones),
     PlayerZoneTickHandler deci.aK.m (grace timer): all ServerProxy, so dead
     in singleplayer like the others `[inferred]` (our ZoneSpawnHandler
     covers the spawn side since v0.7.0). TraderAndSafezoneAttackGuard
     deci.an.a (both sides): traders cannot be attacked.
   - Props: BlockSafezoneSignEntry / Exit (signs at a safezone gate).
   - Civilians: FactionHumanEntity deci.ah.d (faction CIVILIAN, unarmed);
     NpcFaction deci.ae.b: Bandit, Military, Civilian, Soviet.
   Open design point for the plan: Decimation's safezone cancels every
   attack inside, while the user wants guards to turn on a player who
   shoots or hits someone inside; the plan must say what "provoked" means
   (a shot fired inside, an attack attempt on a trader / civilian).
   User answer (11 Oktober 2026): "since there is safezone zones in decimation, you can use that so
   if player shot, the trader or any npc or any player can be safe. learn how zone working ...
   end by end"; destroying a safezone later, plan in docs (docs/zones.md section 8).
   Step 1 DONE v0.46.0 (fixes/ZoneRules, docs/zones.md): Decimation's zone rules working in
   singleplayer plus protection of every friendly entity in a safezone; provoked = a player
   attacks someone protected inside: no damage, guards and soldiers within 64 blocks fire on them
   for 60 s after the last offence, their combat timer runs (no safezone protection). Dev test
   `zones` 19 PASS. Provocation REMOVED in v0.47.2 (user: "its easily accidently hit friendly
   npc so i guess in safezone dont need that feature"): a hit on a protected one just does
   nothing.
   Step 2 DONE v0.47.0 (docs/safezone.md): survivor camps in wild / civilian sectors (5 % of
   sites, new worlds only): scrap wall, 4 watchtowers, guarded gate with safezone signs, market
   street of 6 traders, plaza with campfire and water, medical tent, family tents, latrines,
   generator, garden; residents 6 guards, 6 traders, 5 civilians, 3 armed survivors (tier
   "survivor"), kept by SafezoneSites. Dev test `camp` all PASS, propclash and walkcheck 0.
   Next: fortified city building.
2. Player claimable zone areas. Exists: Decimation's TURF system
   (net.decimation.mod.server.turf: TurfManager add / remove / owner /
   isCapturable, turfs json, TurfCommands), server proxy only, so likely
   dead in singleplayer like the other ServerProxy handlers
   `[not verified]`; check before writing a new one.
3. Advanced claims: a claimed zone can be taken back now and then by NPCs
   (raids by bandits / enemy military). Builds on 2 and the NPC tiers.
4. Police NPC. Exists: police infected look (NYPD set), NYPD armor items;
   no living police NPC. Would be a new tier on an existing human class
   (as the elites) or a faction `[not decided]`.
5. Survivor civilians with a minimal weapon. Exists: civilian humans
   (FactionHumanEntity, faction CIVILIAN, entity "Human", unarmed); bandits hunt
   them, Soviets too since the faction fix.
6. Radiated areas. Exists: our "Irradiated Military Zone" biome (no
   effect yet), hazmat suits and gas masks as items.
7. Advanced military base. Exists: military sectors with mil_ schematics,
   MILITARY zones, MilitarySpawner groups (juggernaut / elite / snipers).
   Update: since v0.42.0 / v0.42.1 our own US FOB style bases replace the
   mil_ schematics (docs/military_base.md).
8. Advanced bandits. Exists: bandit tiers (light / medium / heavy / rpg).
   Step 3 DONE v0.48.0 (docs/safezone.md section 7): city refuges. User choice (11 Oktober
   2026): our own building on a whole city block (not a fortified DeceasedCraft building), on
   the block nearest each city region's middle. 48 x 48: barriers and wire at the street, a
   concrete wall, gate with a sandbag chicane and two guards, a three storey concrete building
   (lobby with registration desk, market hall of 6 stalls, clinic, canteen; dormitory; armory,
   guard bunks, command; roof with 4 guard nests, spotlight, flag), yard with tents, aid
   distribution, APC, generator, water, garden; 22 residents. New worlds only ("!cityRefuges").

### More zombie variants, 60 round magazines, NPC bullet impacts (9 Oktober 2026, "later")
#### Zombie variants design (started 9 Oktober 2026, user choice)
Read from deobf source: InfectedEntity (deci.ag.d) is a HumanEntity with
skins from the "infected" folder, 20 hp, attack 3, speed set EVERY TICK in
onLivingUpdate (0.25, 0.3 in a horde of 10+), 8% bite infection. Variant
(watcher 21): COMMON, MILITARY (marine set), POLICE (NYPD set), the armor
re-applied every tick while it holds nothing; zones pick military / police
for 25%. Specials exist as own classes: crawler, dog, frail (screams),
gawker, hulk, bloater (10% of infected spawns become hulk / bloater).
Plan (`fixes/InfectedVariants`, tag "deciworldgen_zvariant", eggs):
- runner (12%): civilian clothes (hoodie, casual), 14 hp, speed x1.55;
- riot (8%): combat helmet + police clothes, 30 hp, takes 50% of player
  gun damage, speed x0.85;
- screamer (5%): hazmat suit, 16 hp; when it gets a target it screams
  (deci:mob.frail.scream) and every infected within 32 blocks takes the
  same target (once per 10 s);
- night frenzy: at night every infected walks x1.2 and hits +2.
Speed through attribute MODIFIERS (the base value is reset every tick).
Only plain infected with the common variant get one (zone looks stay).
DONE v0.34.0 (`fixes/InfectedVariants`, `fixes/ZombieEgg`
deciworldgen:zombie_egg, config deciworldgen_zombies.cfg). Night frenzy
speed uses modifier operation 2 (multiplies on top of a variant's speed;
operation 1 amounts add up: riot 0.85 + night 0.2 gave 1.05). The join
handler runs at LOWEST priority, after the zone looks. Checked (dev test
mode zombies, about 30 s, all PASS): 265 infected (the rest became hulks /
bloaters): runner 31, riot 21, screamer 14; runner speed x1.55, health per
variant; riot takes 5.0 of a 10 gun hit; a scream reached all 6 infected
25 blocks away (49 in range); night x1.20 speed and +2 attack, off by day;
lineup photo zombies_v0.34.0. Not seen by the user in game yet
`[not verified]`.

- More infected / zombie variants.
- New magazines: 60 round STANAG and 60 round 5.45 AK.
- Bullet impact particles on blocks hit by NPC shots (today only player
  shots make them; NPC shots damage the target directly, see bug.md
  "NPC tracers", the tracer patch now knows the target).

### More clothing variety on military/bandit NPCs
- **Requested**: 27 Juli 2026
- Bandits already have *some* randomization - `deci.ag.a` picks from small
  item pools (`aam`/`aan`/`aao`/`aap`, 2 options each) per armor slot on
  spawn. User wants more variety (bigger pools, more distinct looks) so NPCs
  don't all read as "one type" - and the same treatment extended to
  soldier-type mobs, which currently use fixed (non-randomized) gear.

---

## Documented, not started

### Custom weapon creation
- **Requested**: 27 Juli 2026
- Full how-to written: `create_weapons.md` - covers the `.bmodel` model format
  (turns out to be plain text Techne-style code, not a proprietary binary
  format), the `deci.aD.k` item-registration pattern, and two recipes: reskin
  an existing weapon (fast, no new 3D model needed) vs. a genuinely new model
  (needs Techne - Claude can't usefully author the 3D shape itself, no visual
  feedback loop and texture painting is out of scope).
- **Next step** (not started, user said "later if I have long time"): pick a
  base weapon to clone for a first reskin test, or start modeling in Techne
  for a new shape.

## Armor 20 % weaker against NPC fire (22 Oktober 2026, feature 1 of docs/roadmap.md "Feature order")
- User: "nerf all armor buff by -20% that affected to player when they shooted by npc. now the damage dealt more since the
  armor got nerf"; chosen: each piece's protection x 0.8, helmets too, player shots unchanged.
- fixes/ArmorGunfireHandler.againstNpc (NPC_PROTECTION 0.8) on the body pieces and the headshot helmet of NPC hits.
- Checks: unit ArmorGunfireHandlerTest RED then GREEN, suite 150; dev test checks "armor vs NPC gunfire" now checks the
  exact ratio (x0.642 for the chest tested), helmet and cooldown checks PASS. Server side only (LivingHurtEvent), no
  per frame cost. Dedicated server not run [the same handler; not verified there].

## NPC tracers in colours, every shot (22 Oktober 2026, feature 2 of docs/roadmap.md "Feature order")
- User: "npc who shot gun can randomly use other tracer instead white only, all guns now shot full tracer instead divided
  by 3-6 bullet"; chosen: each armed NPC one random colour for life, every NPC shot a tracer, player guns unchanged.
- Found: Decimation's client draws an NPC shot GENERIC (near white) on 40 % of shots and INVIS on the rest; PatchTracer v2
  already made NPC tracers always visible, so players saw white on every shot (dev test tracer: GENERIC 79 of 79). A gun
  carrying a tracer colour (NBT tracerColorID, GunItem.getTracerColor) draws it on every shot.
- fixes/NpcLoadouts: the NPC's gun spec gets ";tracer=N" (Decimation's WHITE 1 .. YELLOW 7) when armed or loaded
  (withTracer; NPCs saved before get one on load, saved back to GUN_TAG); gunStack writes it as tracerColorID; the client
  re-applies the gun when only the tracer differs (tracerOf: no allocation, asked every tick per NPC).
- Checks: unit NpcTracerTest RED then GREEN (suite 152); dev test tracer "every shot a tracer in the NPC's colour" RED
  (GENERIC 79) then GREEN (GREEN 89 of 89); npc PASS after its client check learned the int tracer. shots "spotting at
  70" FAILs with or without this change (old, docs/tasks.md). Cost: none per frame (the same tracers, another colour).
  Production client not run [colour drawing is Decimation's own; not verified with OptiFine]. Decimation's own armed
  traders and guards that our loadouts do not arm keep its white [not changed].

## Looted magazines hold random rounds (22 Oktober 2026, feature 3 of docs/roadmap.md "Feature order")
- User: "now all mag loot have variaty capacity ammo inside it"; chosen: 1 round to full, weighted toward half full,
  rarely full, never empty.
- Found: Decimation's loot lists hold magazines without NBT, and AmmoItem.onUpdate fills such a magazine to capacity
  on its first tick in an inventory: every looted magazine was full (dev test lootmags: 38 of 38).
- fixes/LootMagazines: remembers the player's loot inventory before Decimation's LootInteractHandler runs (HIGHEST) and
  gives the magazines (belts, clips: Deci.magazineCapacity, AmmoItem feedType not bullet) of a new one their rounds
  after it (LOWEST, receiveCanceled): two dice around half, rounded, at least 1. The screen opens 2 ticks later and its
  container sends the server's stacks. Covers crates, wrecks, cabinets, supply drops (all through that handler).
- Checks: unit LootMagazinesTest RED then GREEN (it caught a bias of small magazines: 5 rounders averaged 60 %); dev test
  lootmags (40 opens of Decimation's weapon cabinet through real clicks) RED (38 of 38 full) then GREEN (0 full of 39,
  mean 47 %), loot PASS; suite 154. Server side, once per loot click. Client display after the container sync [not
  looked at on screen].

## Unload a magazine with shift + right click (22 Oktober 2026, feature 4 of docs/roadmap.md "Feature order")
- User: "mags now can be unload by click shift + right click"; chosen: about a second like a reload, the rounds as
  Decimation's loose rounds of that calibre into the inventory (dropped if full), the empty magazine stays.
- Decimation's own right click on a magazine loads one round (AmmoItem.onItemRightClick, sneaking or not): the client
  cancels a sneaking right click with a loaded magazine (no item use sent) and asks the server through our own channel
  (fixes/Net "deciworldgen", message 0; the first channel of ours); fixes/MagUnload waits 20 ticks with the same
  magazine in hand, then empties it (Deci.magazineRound: AmmoItem.bulletItem; full stacks first); switching away,
  dropping it or dying stops it. Sounds: Decimation's bullet.in, low at the start, high at the end.
- Checks: unit MagUnloadTest RED then GREEN (suite 155); dev test magunload (sneak held, a right click as the mouse makes
  it: DevTestUtil.rightClick) RED (Decimation loaded a round: 21) then GREEN 6 of 6: unload 20 out (25 loose with the 5
  held), switching away keeps 20, a full inventory drops 20. Server work only while a request runs. The server repeats
  the client's check (a loaded magazine with a known round) and keeps any failure inside the tick (automated security
  review: a modified client could otherwise throw a null pointer in the server tick). A dedicated server
  and a joined client [not run: the channel is new, check it with the next production run].
- v2, same day (user: "just make it same like load ammo process code, just wanted reverse version or unload version"):
  unloading is Decimation's loading backwards (AmmoItem.onItemRightClick: one round in a right click, the button held
  repeats it every 4 ticks, bullet.in at pitch 1, one loose round taken, in creative only the round). Now one round out a
  sneaking right click (held: one every 4 ticks), bullet.in at pitch 1, one loose round given (on the ground when full),
  in creative only the round; a stack of magazines gets "You can only unload 1 magazine at a time! Not N!", an empty one
  "This Magazine is empty!" (Decimation: "This Magazine is full!"), and an empty magazine no longer falls through to
  Decimation's loading on a sneaking click. No timer, no running list: the server does at most one step a player a tick.
  Checks: unit MagUnloadTest (step order and texts) RED (no Step / step / message) then GREEN, suite 163; dev test
  magunload 5 of 5 (one click 20 to 19 with 6 loose, five clicks 15 with 10, empty stays 0 with 5, full inventory 19 with
  1 on the ground, a plain click still loads 21). Dedicated server and joined client [not run, as above].

## Looted guns hold rounds (22 Oktober 2026, idea 53)
- User: "some guns loot have ammo inside it, so not all of them is empty"; chosen: about 1 in 2 loaded, the rounds like a
  looted magazine's (1 round to a full magazine, around half, rarely full), Decimation's crates and car loot only (NPC
  guns and corpse drops unchanged).
- How Decimation keeps rounds in a gun: NBT "ammo" and "ammoType" (index into GunItem.ammoTypes, the magazine it holds;
  0 = the first); a reload (PacketReload type 0) ejects that magazine with the rounds less the chambered one. A looted
  gun has no tag until first held (GunItem.initTag).
- fixes/LootMagazines.fill (the same loot hook as the magazines) calls Deci.loadLootGun for each gun without a tag:
  LootMagazines.gunRounds (1 in 2 none, else rounds(magazine size)); the magazine size is the first magazine's capacity
  (the gun's own capacity for bullet and stripper clip feeds); Decimation's initTag through reflection, then "ammo".
  My call: launchers and crossbows never come loaded (a free rocket), guns already tagged are left alone.
- Checks: unit LootMagazinesTest (about half loaded, mean around half, none over the magazine; a size 0 gun stays empty)
  RED (no gunRounds) then GREEN, suite 165; dev test lootmags (Decimation's weapon cabinet opened 40 times by real right
  clicks) all PASS: 12 of 24 guns loaded, 0 outside their magazine, all with Decimation's tag, a loaded looted gun fired
  in hand by the mouse path (Deci.fireHeldGun) went 10 to 9 on the server. Before the change the live gun checks would
  read 0 loaded [inferred: looted guns had no tag, so no rounds; not run against the old code]. Cost: one roll a gun at
  a loot click, server side; no per tick work.

## Radiation dose on the HUD (22 Oktober 2026, feature 5 of docs/roadmap.md "Feature order", idea 30)
- User: "radiation status below player status"; chosen (questions the same day): a dose in percent, always on screen,
  a high dose already hurts; then, told that Decimation's own zone damage (about 1 a second without a mask) kills before
  a 2 minute dose could matter: keep Decimation's damage and make the dose fast. Look: "you better follow pixelated
  design on hud like health, water, food, blood".
- Rules (fixes/RadiationDose, server): while Decimation's irradiated flag is on (PlayerData, RADIATION zone, not creative)
  and no gas mask is worn (gasmask, gasmaskmilitary: Decimation's own protection test) the dose rises 60 a tick of
  24000 (100 % in 20 s); otherwise it falls 1 a tick (100 % in 20 minutes); from 50 % 1 damage every 8 s, from 80 %
  every 4 s, with DamageSources.irradiated (Decimation's death message), inside the zone and after leaving it. My calls:
  the damage numbers, the dose stopping in creative, a new life at 0. Stored in an IExtendedEntityProperties of the
  server player (saved in the player file; a respawn makes a new player entity: 0 [not run in game, only the unit test of a new Dose]; [not run] the return from the End
  also makes a new entity and starts at 0). Server work: one pass over the players a tick, a message to that player only
  when the shown percent changes (Net message 1, RadiationDose.Sync: one byte), and once after joining or respawning.
- HUD (fixes/RadiationHud, client): a fifth icon under Decimation's food, water, blood and health, same x, 35 below
  health, its bold percent where theirs sit, moved down with their column by the stats list (Deci.hudIconsDown counts the
  lines as IngameHud adds them) and hidden with it (ClientState.cl, the inventory screen); where the column would run
  off a short GUI the icon sits left of health on its row (docs/decimation_code/menus.md 3b). The icon is ours,
  tools/hud_icons.py: 40 x 40 pixel art in the style measured from Decimation's four icons (flat fill, lower 40 %
  darkened to 0.6, black outline ring alpha 230, soft ring 50), a yellow radiation sign.
- Checks: unit RadiationDoseTest (rise, fall, percent, damage steps, half a dose after 10 s) RED then GREEN; the save
  and clamp tests came after the code; suite 172. Dev test `radiation` 12 checks + 2 screenshot checks, all PASS at
  1440 x 810 and at 854 x 480: 10 s unmasked = 50 % with Decimation's damage kept (20 to 10 health), mask and creative
  stop the rise (-100 in 100 ticks, -40 in 40), outside 90 % = 2 damage in 10 s, 60 % = 1, 40 % = 0, falls 200 in
  200 ticks, HUD percent = server percent, icon whole on screen (below health at 1440 x 810, beside it at 854 x 480;
  shots docs/shots/hud_radiation_v0.66.1). glstack, zones, magunload PASS in the same game.
- Performance (budgets: HUD under 200 us a frame, server under 50 us a tick): dev client HUD 48.1 to 48.7 us a frame
  (6300 frames; nearly all of it the outlined percent, 9 string draws like each of Decimation's four), server 4.5 to
  4.8 us a tick with one player; no allocation a frame (the text is rebuilt only when the percent changes), a packet
  only on change.
- Production (one jar of main's tree with the production client and OptiFine, docs/production_test.md; then the
  production dedicated server joined from that client): radiation 15 of 15 (HUD 45.8 and 53.3 us a frame over 6600
  frames, server 2.7 and 5.0 us a tick; icon beside health in its 240 high GUI), glstack PASS (88 cases, 0 leaking);
  dev test `channelmp` on the dedicated server 7 of 7: the unload over our channel (20 and 5 became 19 and 6, the first
  run of message 0 off the integrated server), Decimation's irradiated flag on from its own server zone tick in a zone
  made with its commands (/deci pos1, pos2, /zone create radiation; removed after), the HUD's dose 52 % after 10 s
  (message 1), 52 then 51 % 10 s after leaving.
- Test fixes on the way (both in my tests, not the feature): in the production world a survivor NPC shot the test
  player (humanity Bandit) during the mask check ("ProdTester was shot by a survivor": health 4, then 0 damage at 90 %
  around the death), so `radiation` turns mob spawning off and removes every mob near the arena each second; in the
  first `channelmp` run the player fell between the corner teleports and stood under the new zone (flag off, 0 %), so
  it flies while setting the corners and the box reaches 4 below to 6 above the ground. The `lootmags` gun share bound
  failed once by chance (15 of 21 loaded against 30 to 70 %): 3 standard deviations of half now.

## Random Decimation skins instead of Steve (22 Oktober 2026, feature 5b, idea 54)
- User: "later we dont use steve default skin, instead using decimation skin with randomize so each player has unique
  skin they use, maybe like random ID we got from 1-10 skin as example"; chosen: by the player's id ("as long not
  distrub other feature especially multiplyaer"), players with a skin of their own keep it, all 21 skins.
- fixes/PlayerSkins (client only, nothing sent): a player with no skin when joining a client world, or when first drawn
  (Decimation's menu preview player), gets one of Decimation's human skins (human0 .. human20, 64 x 32 player layout,
  textures/model/entities/mob/human) through AbstractClientPlayer.func_152121_a, the skin manager's own setter; an own
  skin arriving later replaces it. My call: the pick comes from the NAME (murmur3 mix of the lower case name, mod 21),
  not the UUID: on an offline server a client knows itself by its session id and the others by the offline id made
  from the name, so a UUID pick would show a player one skin and their friends another; every client agrees on names.
- Corpses: Decimation's corpse binds "skins/<owner id>" and fills it from crafatar.com (Steve for offline ids; Steve for
  the dev account too, though the game shows its own skin). For every player joining this client's world an alias
  texture (PlayerSkins.CorpseSkin) goes under both of the player's ids (server id, offline id): it hands over the
  player's current skin, so a corpse wears what its player wore, ours or their own; no crafatar download for them. Trap:
  TextureManager.deleteTexture on such a name would free the player's skin texture (1.7.10's deleteTexture keeps the
  map entry and frees the GL texture of whatever is there).
- Checks: unit PlayerSkinsTest (same name same skin, ignoring case; all 21 used about evenly over 21000 names; close
  names spread; the offline id is Minecraft's) RED then GREEN, suite 176. Dev test `skins` all PASS at 1440 x 810: the
  dev account keeps its own skin (never Steve), stand-in players SkinTest0 / SkinTest1 wear human0 / human2, an own
  skin arriving later wins and stays, Decimation's menu preview player wears the local player's, a corpse of a stand-in
  wears its skin of ours and the local player's corpse its own (same GL texture; the first run found the corpse of the
  own skinned dev account in ours, and before that Decimation drew Steve for both); armskin PASS. Hook cost 1.2 to
  2.8 us a drawn player (RenderPlayerEvent.Pre: one check), nothing per frame otherwise. Shots
  docs/shots/skins_v0.66.1.
- Production client with OptiFine (one jar of main's tree): skins, armskin, glstack 11 of 11: the production test
  account (no skin of its own) wears human0, so do the menu preview player, the gun arms and its corpse (same GL
  texture); hook 0.23 us a call. Dedicated server [not run: client only code; a corpse there carries the server id,
  which the alias covers with the offline id].

## DayZ dark nights (22 Oktober 2026, feature 6, idea 52)
- User: "the night become so dark so its like dayz they almost black 95% in night time"; chosen: DayZ style, the moon
  matters (near black at a new moon, shapes at a full one), nights ignore the brightness slider, daytime keeps it.
- fixes/NightLight (pure): Minecraft's 16 x 16 light map built as EntityRenderer.updateLightmap does, with three night
  changes weighted by the night share (1 minus daylight from the sun's height): the sky term from the moon (0.09 new to
  0.36 full, down to 0.4 of that in rain) instead of vanilla's 0.24, the two 0.03 ambient floors gone, the slider
  (gamma) times daylight. At full daylight it is vanilla's map to the bit (unit test). Block light (torches, our street
  lights), lightning and night vision (Decimation's goggles give the potion) as in vanilla.
- fixes/DarkNights (client): vanilla rebuilds its light map once a tick at the start of a frame; Forge's
  EntityViewRenderEvent.FogColors fires later in that frame, before the sky and the world are drawn (renderWorld:
  updateLightmap, updateFogColor, renderSky, terrain), and Decimation does not use it. There, once a tick, NightLight's
  colours go into EntityRenderer.lightmapColors (field_78504_Q) and its texture (field_78513_d) is uploaded. Overworld
  only, nothing at full daylight, -Ddeciworldgen.darknights=false switches it off. No jar patch, no core mod.
- fixes/MoonClock: the first live run read a full moon where it set day 5 by world time. Decimation's DayLengthHandler
  puts world time back to 0 after 24000, so vanilla's phase (time / 24000 % 8) was a full moon EVERY night, in the sky
  too. The reset stays (Decimation's ambient music and the stereo's clock read fixed hours). The overworld's provider
  becomes MoonProvider (a WorldProviderSurface, only while vanilla's is the registered one) whose phase is
  (WorldClock day - 1) % 8: day 1 full, day 5 new, changing where vanilla's would (world time 0, when WorldClock counts
  the day). The client gets the day by message (Net 2) when joining and at each new day. Sky moon, light map and
  vanilla's moon rules all follow. MoonClock.setDay (and WorldClock.setDay) for tests and admins.
- Checks: unit NightLightTest (vanilla at full daylight for three slider settings and rain; new moon 0.03 to 0.06;
  full moon 0.12 to 0.22 and 2.5 times the new; the slider does nothing at night; vanilla nights were over 0.4 at the
  brightest slider; torches as vanilla's darkest slider; night vision close to vanilla's; smooth dusk; rain hides the
  moon) and MoonClockTest RED then GREEN, suite 188 (one expectation of mine was wrong: vanilla's night vision reaches
  about 0.6, not 0.85). Dev test `nights` 10 of 10 at 1440 x 810 with the slider at its brightest: noon bright and
  untouched (0 rebuilds), new moon midnight open sky 0.044 (vanilla 0.489), full moon 0.176, torch light 0.957, night
  vision 0.514; on screen (the saved shot's floor, whatever uploaded the map last) new moon 0.055 against vanilla's
  0.299, full moon 0.116. A test trap found on the way: weather changed on the server outside its weather tick never
  reaches the client (no packet), the client kept its rain (every value 0.4 of the dry one); the test dries both sides.
- Performance: a rebuild 53 to 74 us, once a client tick (about 1 ms a second), none by day; nothing per frame.
  Shots docs/shots/nights_v0.66.1.
- Production client with OptiFine (one jar of main's tree): nights 10 of 10 with the same numbers (on screen 0.054
  against vanilla's 0.297: OptiFine does not override the map), glstack PASS, rebuild 55 us. Dedicated server [not run:
  the light map is client only; the moon day message rides our channel like message 1, checked there by channelmp].

## Durability of armor, vests and guns, with repair parts (22 Oktober 2026, feature 10, ideas 47, 48)
- User: "armor durability highly nerf to 256 and they will be reduce by hit or shot, 1 shot = 1 durability. armor now
  can be repaired with armor part and for vest durability is 512 and need armor plate item that can be find it on
  military or police crate. guns now has durabitlity and they need to be repaired by guns part"; chosen: kept at 0 but
  working no more, a quarter a part, guns jam below 25 % and stop at 0, parts in military and police crates only; and,
  told that vests protected NOTHING (their multiplier was never applied, by Decimation or by us), the stronger of vest
  and chest piece counts against body shots. Spec docs/superpowers/specs/2026-10-22-durability-design.md.
- Armor and vests (fixes/ArmorGunfireHandler, fixes/Durability): every Decimation armor piece 256, every vest 512, masks
  untouched (ArmorGunfireHandler.setMaxima at preInit); the vanilla durability bar shows. A gunshot wears each piece
  that counted by 1 (body pieces on every shot, the helmet on a headshot, the vest or the chest piece, whichever
  counted); a used up piece protects nothing (its factor is taken back out of Decimation's player hit, and Forge's
  ArmorProperties.ApplyArmor already lets a piece absorb no more than its durability left against melee); melee wear
  stays vanilla's (Forge's, at least 1 a piece) but ends on the maximum, never destroying a piece (meleeLast sets it
  back first). Vests: Durability.bodyFactor, applied to NPC shots directly and to a player's shot by swapping the chest
  piece's factor (Decimation's handler applied it) for the vest's when the vest is stronger.
- Guns (fixes/GunWear, GunWearClient, tools/patches/PatchGunWear.java on the three jars): NBT deciworldgen_wear and
  deciworldgen_jammed; rifle 1500 rounds, SMG 1800, pistol and revolver 2500, shotgun 1200, machine gun 1000, launchers
  and crossbows none. GunItem.fire (client) asks GunWear.blocked first: a jammed or worn out gun gives Decimation's
  empty click and, now and then, a toast "Jammed: press R to rack" / "Worn out: repair with Gun Parts";
  GunItem.onServerFire calls GunWear.onShot once a shot (wear, and below 25 % left a 2 % jam roll). Both calls sit in a
  catch of NoClassDefFoundError: the base jar is shared by every checkout. R with a jammed gun in hand plays
  Decimation's Rack animation (Deci.playRack) and sends Net message 3; the server clears the jam; the press is taken in
  the client tick's START phase, before Decimation reads R in END, so no reload starts. Decimation's reload key field is
  named "do" (a Java keyword): Deci.reloadKey reads it by reflection.
- Parts (fixes/RepairPart, icons tools/item_icons.py in the style of Decimation's 32 x 32 item icons): Armor Repair Kit
  (the most worn armor piece worn), Armor Plate (the vest), Gun Parts (the most worn gun in the hotbar); a right click
  repairs a quarter of the maximum and uses one, nothing to repair uses nothing. Loot: Deci.addLootShare, next to
  Decimation's Pistol Suppressor (military and police pools, never civilian), copies sized per pool for about 3 % of
  its entries (plates 2 %): 23 copies in military pools, 3 in police ones. Tooltips: "Durability N / M", the reason in
  red when jammed or worn out.
- Checks: unit DurabilityTest and GunWearTest RED then GREEN, suite 198. Dev test `durability` 16 of 16: an NPC shot
  0.642 lost against 0.642 expected (vest counted over the Marine top) and boots, legs, vest worn 1, chest 0; used up
  boots protect nothing and stay at 256 of 256; a player's shot with used up boots and the vest 0.946 against 0.946; a
  melee hit on a piece with 1 left ends on 256 of 256, kept; three real shots (the mouse path) wear 3; a jammed gun does
  not fire; R racks it with no reload (ammo kept); a worn out gun does not fire; each part repairs a quarter and is used
  up; nothing to repair uses nothing; parts in military and police loot, none civilian. Costs: hurt hook 28.7 us a hit
  after warm up (285 us the first, class loading), shot hook under 36 us; nothing a tick.
- Regression on the way: checks, playerfire, gun, attachments, glstack PASS (61); zombies failed twice and the cause
  was in the tests: radiation, durability (and magunload) left the player in survival, the world saved it, and in the
  next zombies run a hulk killed the test player, which took the watched infected out of the world (ticksExisted stuck
  at 2, not in loadedEntityList), while leftover rain made isDaytime false at noon. Now durability and radiation put
  the game mode back, zombies makes its player creative and zeroes rain and thunder strength: zombies, durability,
  radiation, zombies in one game 42 of 42.
- Automated security review of the commit, two findings. (1) Free repair, real: the melee clamp guessed Forge's wear
  as health x points / 25 plus 1, but Forge caps a nearly used up piece's wear at 1, so a big hit set a used up piece
  back several points (protecting again). Now Durability.forgeWear is Forge's formula and a piece is set to the maximum
  less 1 only when the hit would pass the maximum (unit tests; live check "a big melee hit on a used up piece: no free
  repair", 17 of 17). (2) Jams enforced on the client only: true, and kept: Decimation's shots are client authoritative
  (the client picks target and damage, the server applies what PacketGunHit says), so a modified client fires anyway;
  a server check would need a patch of Decimation's hit handler and stop nothing of that. Noted, not changed.
- Production client with OptiFine (one jar of main's tree, with PatchGunWear): durability 16 of 16 with the same
  numbers, playerfire and glstack PASS (24 in all); hooks 3.4 us a hit warm and 16.4 us a shot. Dedicated server [not
  run: the hit and shot hooks are the same server code as in singleplayer; the R rack rides Net message 3 like
  message 0, which channelmp checked there].

## Radiation set (22 Oktober 2026, feature 7 of docs/roadmap.md "Feature order", ideas 41 to 43; branch bugfix)
- User (decisions of 22 Oktober 2026, docs/ideas.md 41 to 43): a radiation pill takes 40 percentage points off the dose
  at once and is found in medical loot; a gas mask's filter wears only while its wearer is in radiation, about 10
  minutes a filter, and an empty filter protects no more until a new Gas Mask Filter is fitted (right click it); "wasteland
  city become full radiation zone on all place".
- Pills (fixes/RadiationPills, deciworldgen:radiation_pills, "Radiation Pills"): used the way Decimation's own pills are
  (Pain Killers HealingSyringeItem, Vitamin Capsules DrugSyringeItem): 4 to a stack, a held right click in the block
  pose for 32 ticks, Decimation's syringe "open" sound at most every 8 s (PlayerData's long action timer), then on the
  server RadiationDose.lower(player, 40) (40 x 240 = 9600 of 24000, never below 0), one pill gone (an empty slot, never
  a stack of 0) and the syringe "use" sound, on the client the green chat line "You've used the Radiation Pills to lower
  your radiation!". Tooltip "Medical Item (For Radiation -40%)" in Decimation's red, Decimation's medical creative tab.
  A held button takes the next pill of a stack right after the first (vanilla's repeat; Decimation's pills do the
  same). Loot: one entry wherever Vitamin Capsules are (the medical list: medical crates), weight 0.9 like theirs, 7.3 %
  a roll. Icon ours (tools/item_icons.py, 32 x 32 in the style measured from Decimation's pill icons: only opaque
  pixels, the shape's border in one dark slate (64, 66, 72), fills shaded like a lit cylinder): an amber bottle, a white
  cap, a yellow label with a black radiation sign.
- Filters (fixes/GasMaskFilter): the life is ticks in the worn mask's own tag (deciworldgen_filter, 12000 = 10 minutes;
  a mask without the tag has a full filter). Server, one pass over the players a tick: while Decimation's irradiated
  flag is on (never in creative) and a gas mask is worn (deci:gasmask, deci:gasmaskmilitary) the life falls 1 a tick in
  our per player memory; it goes into the tag at each whole percent (every 120 ticks), when leaving radiation, when the
  mask comes off, at death (before Decimation's DeathHandler gives the gear to the corpse) and at logout (FML posts it
  before the player save), never while a screen is open: Decimation's gear screen holds the worn stack itself, and a
  changed tag sent to its mask slot makes the client clear its cursor and rebuild the slots (PlayerExtraInventory.
  markDirty). Empty: Deci.wearsGasMask answers no, so our dose rises as without a mask, and Decimation's own zone damage,
  which spares any gas mask item (PlayerData.tick: 10 % a tick, 1 damage, DamageSources.irradiated), is applied by us.
  Chat once each: yellow "Your gas mask filter is almost used up!" at 10 %, red "Your gas mask filter is used up! Fit a
  new Gas Mask Filter.".
- Fitting: a right click with Decimation's Gas Mask Filter (deci:ItemScrapMilitaryGasMaskFilter) while wearing a gas
  mask. The client cancels the click and sends message 32 (our channel fixes/Net; the feature agent's ids start at 32),
  once a press (a press on a block fires a block and an air event); a click on a block with a use of its own (a
  Decimation loot block, anything with a tile entity: props, chests, Decimation's doors) stays the block's. The server
  checks again: the filter in hand, one gas mask worn, no screen open, the filter not at 100 % ("Your gas mask filter is
  still full!": a full filter is never thrown away), at most one fit a player a tick; then the filter is full, one Gas
  Mask Filter is taken (none in creative), Decimation's gear sound (deci:misc.armor.equip) and the green line "You've
  fitted a new Gas Mask Filter!". A stack of masks (commands only) is refused.
- Loot: one Gas Mask Filter entry wherever Decimation's military gas mask is (military crates and wrecks, care packages,
  mech wrecks) and one wherever the police vest is (police crates and cars). Its weight is its own 0.5 (a LootChanceItem
  of Decimation's, the same in every list), so it is as rare as a gas mask and rarer than any weight 0.9 item: military
  0.27 % a roll (about 0.4 % a crate, 1 or 2 rolls), police 4.7 % a roll (about 7 % a crate); a second military entry
  would already beat the common items there (0.48 %). Supply drops keep Decimation's entry.
- Client view: "Filter: N%" in every gas mask's tooltip (rounded up, so any life left shows at least 1 %; green over 50,
  yellow over 10, red), "Right click to fit it into your gas mask" in the filter's. Decimation's HUD line "☣ Protected"
  (IngameHud) and its missing green tint (ClientEventHandler) follow the mask item only; while the worn filter is empty
  and the player irradiated, a HIGHEST priority overlay handler hides the client's own mask for Decimation's draw
  (PlayerData.setMask on the client only sets a field) and a LOWEST one puts it back: "☣ Irradiated" and the tint, as
  without a mask. The client learns the empty filter from Decimation's own PacketSyncPlayerData (the mask with its tag),
  sent after we mark its PlayerData dirty when a filter empties or is fitted.
- Wasteland (city/WastelandRadiation): a RADIATION zone over every wasteland city. Two kinds on one place, analysed:
  Decimation keeps every zone in one flat list and each check asks one kind (ZoneManager.isPlayerInZone and the others:
  inside any box of that kind); its spawn handler asks MILITARY, POLICE, SAFEZONE in turn; nothing picks one kind per
  place. So a RADIATION box on the ground of the MILITARY boxes adds the radiation rules and changes nothing military.
  Worlds with whole sector wasteland cities (since v0.57.1): one box over the sector, 256 x 256, every height, so the
  GPS map shows the radiation mark at the city's middle beside the four districts' military marks; older worlds: one
  over each district, 128 x 128 (there both marks fall on one spot and the military one is drawn on top). Added with
  the district's military zone when a chunk near it is populated (Wasteland.zone, StructureData markers
  "!wr_s<rx>_<rz>" or "!wr_<gx>_<gz>").
- Existing worlds too (my call): at world load (lowest priority, after ZoneStore read the world's zones) every wasteland
  military zone of ours (Wasteland.zone's box with its "!wz_" marker) without a radiation zone of ours over its middle
  gets its box. Cheap (one pass over our zones at load, only adds, idempotent) and safe for the save; for players it
  means whoever logged out inside a wasteland city of an older world is irradiated at the next login (the user's wish;
  Decimation's damage about 1 a second without a working mask, our dose on top). Decimation's radiation hurts players
  only: garrisons and NPCs there are unaffected.
- Checks: unit GasMaskFilterTest 8 (10 minutes a filter, wear only in radiation, percent rounded up, no tag a full filter,
  clamped tags, tag steps, fit decisions, warnings), RadiationDoseTest one more (40 points, never below 0),
  WastelandRadiationTest 3 (district and sector boxes, markers, military box recognition): RED (94 compile errors) then
  GREEN; suite 188, 0 failures. Dev test `radset` 36 checks, all PASS (last run 22 Oktober 2026 16:00, 1440 x 810):
  pills like Pain Killers (stack 4, use 32 ticks from Forge's item use start, block pose, same tab), 90 % fell 9601 in
  one tick (9600 and the tick's fall) to 49 %, the HUD 49 like the server, 20 % to 0, one pill used each time and the
  hand slot empty; no wear in 100 ticks outside radiation, 196 in 196 ticks inside with no dose and no damage, the tag
  at 11880 with the count at 11804, the tag equal to the count after leaving; an empty filter: no protection, the dose
  rose 5400 in 90 ticks (60 a tick), 3 to 6 damage in 90 ticks (Decimation's damage done by us), one low and one empty
  warning, Decimation's HUD saw no gas mask in 47 of 47 frames with the mask back after each draw (shot: "☣
  Irradiated"); a press on the floor filled an empty filter with one filter and one message, a full filter refused, two
  forged requests in one tick used one filter (the second dropped), creative filled it with none used, a stack of 2
  masks refused, no message without a gas mask, a click on a loot crate opened the crate and fitted nothing; loot
  entries 1, 1 and 1, 10000 opens of each crate: 38 filters (40 expected), 717 (708), 1108 pills (1098); the wasteland
  district at 0,512 got its military zone and the sector's radiation box (0,512 to 255,767), Decimation's own check
  answers both kinds there, and the load upgrade put a removed radiation box back. Shots docs/shots/radset_v0.66.1.
- Performance (budgets: server under 50 us a tick, HUD under 20 us a frame, no allocation a tick, packets only on a
  change): our per player work 0.10 us in radiation and 0.27 us out of it (3 x 1000 runs on the server thread); the
  whole handler 8.5 to 14.6 us a tick on average (wall time since the start, a load average of 10 to 38 with a second
  dev game running beside it). Allocation (HotSpot's per thread allocated bytes): in steady state nothing but the tag
  written each whole percent (16 bytes per 120 ticks, 0.08 bytes a tick on real ticks); the first ticks of a path the
  JIT had not compiled yet read 16 bytes a tick, and one batch of 1000 runs 912 bytes once, both gone once warm
  [inferred: JIT warm up]. Packets: message 32 only on a fit click, Decimation's PlayerData sync only when a filter
  empties or is fitted. HUD: the two overlay handlers 1.2 to 5.6 us a frame (two checks, nothing allocated). The pills:
  nothing per tick.
- Production (main session, 22 Oktober 2026, after the merge 12afb2b): radset 36 of 36 and glstack in the production
  client with OptiFine (38 PASS, one jar of main's tree, sha256 a9e39441); a player's server cost 0.07 us in radiation
  and 0.16 us out of it, the HUD handlers 2.32 us a frame, nothing allocated out of radiation.
- Bug review: a filter is taken only after every check, in one server step; two requests in one tick use one (tested);
  a full inventory changes nothing (nothing goes into the inventory); creative wears nothing (not irradiated) and fits
  without using one (tested); a stack of masks is refused (tested) and would wear as one; a mask taken off keeps its
  wear (the old stack's tag is written when the slot changes); a crash between autosaves loses at most 119 ticks of
  wear (the player's favour); a dying player's mask (now the corpse's) no longer wears.
- Regressions run in the same game: zones, loot, useitems, magunload PASS; wasteland PASS (a new district in generation
  got its sector's radiation zone: log "wasteland radiation zone 1536,256 to 1791,511"); radiation: every functional
  check PASS, its own cost checks FAILED under the load (HUD draw 223 and 261 us against 200, server tick 112 us once,
  then 38.7 us): RadiationHud and RadiationDose are unchanged but for the filter check, a second game (Decimation-scratch)
  ran beside it; to rerun on a quiet Mac [not verified].
- Not verified: the dedicated server (message 32; the main session's run); the death and
  logout writes of the tag (no death or logout in the test); tear gas: Decimation's tear gas still spares any gas mask
  item, even with an empty filter (TearGasGrenadeEntity checks the item; the filter wears in radiation only, by the
  user's decision; open question).
- My calls: pill tooltip and chat text, no refusal at 0 % dose (Decimation's pills have none), pills only where Vitamin
  Capsules are, one filter entry per list, the "still full" refusal, warnings at 10 % and empty, Decimation's HUD saying
  "Irradiated" with an empty filter, the fit sound, the block click rule, the sector box, existing worlds upgraded at
  load, "Filter: N%" written like Decimation's "Damage Reduction 5%".
- Follow up (user, 22 Oktober 2026, docs/ideas.md 42: "Gas Mask Filters about 3 % of military crate opens (police 7 % as
  built); an empty filter protects against nothing, Decimation's tear gas included"):
  - Loot: GasMaskFilter.addLoot now puts as many filters in the pools as their weights ask for: copiesFor finds the whole
    number of entries whose share of opens (1 or 2 rolls an open, PatchLootRolls) is nearest the target. Military: 9
    entries in each of the 6 pools holding Decimation's military gas mask, 3.2 % of military crate opens by the weights
    (8 would give 2.8 %, 10 give 3.5 %; 10000 opens rolled in three runs: 308, 322 and 280). Police: still 1 entry, now 5.6 % of police crate
    opens, not 7 %: feature 10's repair parts went into the same pools since, and 2 entries would give 10.6 %, further
    from 7 % (575 and 567 of 10000 rolled). Logged at start: "gas mask filters: 9 in each of 6 loot pools like gasmaskmilitary".
  - Tear gas: Decimation's check is on the CLIENT (TearGasGrenadeEntity.onUpdate, once settled, every tick for the 600
    ticks it lasts): every player whose box meets x and z within 6, 3 below to 5 above the gas and under 5 blocks away,
    not in creative, not already poisoned and without a gas mask ITEM gets PacketApplyPoison, whose server handler gives
    Poison V for 40 ticks. Ours, on the server: a player whose worn gas mask filter is empty gets the same poison from
    any settled tear gas in that reach (a box search round such players only, every 4 ticks); a player without a mask is
    left to Decimation's path, a filter with life left still protects. With radiation (above) an empty filter now
    protects against nothing Decimation checks a gas mask for (also its HUD and its tint; the breathing sound stays).
  - Checks: unit GasMaskFilterTest 4 more (share of opens, copies for 3 % and 7 %, the tear gas reach, our share of
    poisonings), RED then GREEN, suite 215. Dev test radset 39 checks: tear gas without a mask poisoned by Decimation's
    own path (the gas active), with an empty filter poisoned by ours (1 poisoning), with filter left not poisoned; the
    loot checks count opens holding a filter (rolled within 4 sigma of the weights) and that one entry more or less
    would land further from the target.
  - Cost: nothing more for a player with filter left; with an empty filter one small box search every 4 ticks.

## Specific mobs at specific places (22 Oktober 2026, feature 8 of docs/roadmap.md "Feature order", idea 35; branch bugfix)
- User (docs/ideas.md 35): "a table per place kind, editable in a config file: the wasteland irradiated and military
  infected, demons at night; bases and airports soldiers; hospitals medic infected; police stations riot infected;
  forests infected dogs; highways bandit groups".
- The table: config/deciworldgen_places.json (fixes/PlaceSpawns), one list per place kind. An entry: "mob" (infected,
  infected_dog, crawler, hulk, frail, bloater, demon, soldier, bandit, soviet), "look" for infected (military and police
  are Decimation's own looks; runner, riot, screamer and the new irradiated and medic are ours, fixes/InfectedVariants),
  "chance" in percent, "group" [least, most], "time" (any, day, night), "cap" (no more of that mob within 64 blocks
  across and 32 up or down; 0 none). Written with the user's table when missing or older than its version (the old file kept as .old), then the
  user's: edit and restart; mistakes (an unknown place, mob, look or time, chances above 100 at one time of day) are
  logged and the entry skipped. My numbers [draft]: wasteland irradiated 30, military 30, a demon 2 at night (cap 2);
  base and airport soldiers 30 (2 to 3, cap 6); hospital medic 60; police station riot 60; forest infected dogs 30 (1
  to 2, cap 6); highway bandits 15 (2 to 4, cap 8).
- How (my design): the table changes WHAT spawns where, not how many. When the game's own natural spawning (vanilla's
  SpawnerAnimals over Decimation's biome lists) puts a plain infected at a place of the table, Forge's CheckSpawn event
  (fired by SpawnerAnimals only: spawner blocks, eggs, commands and our code never pass it) rolls 100 through that
  place's entries in order: an infected entry tags it with a look, put on when it joins at LOW priority (after the zone
  handlers' 25 % military and police looks, before InfectedVariants' random roll); another mob refuses the infected
  (only on a spot it could spawn on) and spawns the group on its spot as SpawnerAnimals spawns (special spawn event,
  onSpawnWithEgg), each member on a free cell beside it, every one tagged with its place (entity data
  deciworldgen_place). The rest stays as Decimation spawns it. So the game's mob cap, doMobSpawning, peaceful and the
  spawn rules stay in charge, NpcLoadouts still gives soldiers and bandits their tiers, Decimation still swaps some
  (hulks, bloaters, mechs), ZoneSpawnHandler still refuses bandits in safezones, and nothing runs per tick.
- Places (worldgen/Places), the first that holds wins: airport (Airport.site of a wasteland sector), base (the
  military's named places in StructureData: PlaceNames stores a base's box with its first slice), police station and
  hospital (the city building planned on that chunk, LcCity.buildingKind: BuildingDress's kinds by the building's name,
  "police", "hospital", "polyclinic"), highway (Highways.at), wasteland (Wasteland.chunk), forest (a biome of Forge's
  FOREST type: vanilla forests and taigas, our Overgrown Forest, Overgrown Hills and Burnt Forest). The seed's places
  of a chunk are memoised in a direct mapped table of 8192 chunks (no allocation on a hit), cleared at every world load.
  Could NOT be known: buildings in worlds without the converted city content (the procedural city has no police or
  hospital buildings), buildings of hand made maps or other mods, a base before its first slice (no spawns there yet
  anyway), Decimation's own wasteland military buildings as "bases" (they count as wasteland), a forest's trees
  outside a forest biome, the inside of a building against its roof or yard on the same chunk.
- New looks (InfectedVariants, last in the list, weight 0 so never rolled at random): irradiated (Decimation's hazmat
  suit, bare headed: the screamer wears the hood; health 24) and medic (Decimation's medic vest, the one its soldier
  medics wear, gray trousers, white shoes). Their spawn eggs come with them (ZombieEgg metadata 3 and 4).
- Checks: unit PlaceSpawnsTest 6 (the roll walks the entries in order, day and night entries, night by the time of
  day, the user's table is the default and checks clean, mistakes reported, the file written once then the user's)
  and PlacesTest 3 (the order, base boxes from place entries, the chunk memo): RED (55 compile errors) then GREEN;
  suite 224. Dev test `placespawns` 29 checks, all PASS: a position of each kind from the seed's own sources where
  only it holds, and Places agrees (airport -640,-1600, police station 344,552, hospital 536,-552, highway 8,8,
  wasteland 8,520, forest -40,-40, a test base among the named places; search 162 ms); the table 7 of 7, no problems,
  the file written; 2000 spawn checks a place through Forge's own event, each entry within 4 sigma of its chance (for
  example forest dogs 652 of 2000 against 600, the night demon 40 against 40, none by day); looks put on at join (medic
  vest, riot coat and helmet, hazmat vest bare headed, Decimation's military variant 1); replacements (an infected
  refused for 2 bandits with tiers, 2 soldiers with tiers, 2 dogs, 1 demon at night); the highway's bandit cap kept
  the infected; natural spawning for 20 s with the forest forced: 98 checks through the table, 31 rolled dogs, 22
  forest dogs in the world (13 capped); last run 97 checks, 36 dogs rolled, 23 in the world, 16 capped. Same game:
  zombies, npc, nospawn PASS.
- Performance (budgets: nothing per tick, a place lookup under 5 us without allocation, our part of a spawn check
  under 10 us, a cap search under 500 us): Places.at 0.11 to 0.27 us a lookup, 0 bytes in 10000 (memo hits, steadiest
  of 3 runs); over the natural window, two runs (load average about 8 with another dev game running, then lower): 5.9
  and 2.7 us a check of our own (place, roll, tag), a cap search 143 and 55 us (a box of the chunks' entity lists 128
  across, 32 up and down; only when an entry with a cap is rolled: 36 of 97 and 36 of 124 checks with the forest's
  dogs), a replacement mob 258 and 102 us with every join handler (the game's own cost of a spawn); nothing per tick. A first try searched the full height for the cap (30 us
  a check on average): the box is now 64 high round the spot.
- Bug review: a replacement only where the infected could spawn (getCanSpawnHere: in peaceful never, with an entity in
  the spot never); caps count living mobs of that kind within 64 blocks before a group; a look tag left on an infected
  Decimation swapped away is dropped with it; the place memo resets at every world load, so a world with other layout
  switches never reads the last one's places; dedicated server: the same event and join order [not verified: no
  production runs on this branch].

## Loot rules A: looted stays looted (22 Oktober 2026, feature 12 A, idea 36)
- User: "loot by default is empty forever after player loot it, except multiplayer they can be disable or enable for
  minutes refill loots (by default is enable for server)"; "15 minute use old decimation setting, for singleplayer it has
  option on cfg file world too so they can use refill method or not". Spec docs/superpowers/specs/2026-10-22-loot-rules-design.md.
- Before: Decimation keeps each looted spot for 15 minutes of real time in one static map in memory (LootCooldownRegistry,
  keyed by dimension and position). A new game refilled every crate; a world reloaded in the same game, or ANOTHER world
  with a crate at the same coordinates, kept the marks. Its clear at server start never runs (ServerAboutToStartHandler
  is an @Mod.EventHandler method put on the event buses: FML never posts lifecycle events there).
- Now (fixes/LootRefill): a record saved with the world (data/deciworldgen_looted.dat, per dimension the world tick of
  each spot's last loot, 5 ints a spot) is the one rule. A right click on a loot block (one with a list; not a supply
  drop, not in creative) sets Decimation's own mark of that spot before its handler reads it (HIGHEST): looted, and the
  handler answers its own "This <block> is empty..."; ready, and its mark is taken out of the map (the private
  TLongLongMap, found by type) so it rolls as always. After the handler (LOWEST, also when cancelled) a new loot inventory
  means Decimation looted it, and the spot is written with the world's tick. A multiblock is kept by its origin, as the
  handler resolves it (Deci.lootSpot). Our record also ends the cross world leftovers.
- The switch: <world>/deciworldgen_loot.cfg, written at the world's load when missing: `refill` (false in singleplayer,
  true on a dedicated server) and `refillMinutes` (Decimation's lootRespawnTime / 60: 15, or a server admin's own value).
  Minutes are world time (1200 ticks a minute), so a paused or closed world does not refill. At each load spots that
  would refill anyway are dropped from the record (refill on only). A LAN or hosted world is a singleplayer world:
  refill off unless its file says so [my call].
- Checks: unit LootRefillTest 8 (never looted, refill off forever, minutes of world time, defaults from Decimation's
  seconds, the key equals Decimation's packPos, the record through NBT with two dimensions and negative coordinates, prune,
  the file written then read as edited): RED (compile) then GREEN; suite 219, 0 failures. Dev test `lootrules` (real
  clicks on Decimation's military crate, a metal shelf on its upper part and a supply drop; a real save and reload: the
  client leaves, the integrated server stops and saves, the world opens again, then Decimation's map emptied as a new game
  has it): RED without LootRefill registered, 7 FAIL (no record, no file at load, the crate opened after the reload, the
  shelf not kept, no refill after its minute); GREEN all PASS: record tick 939718 read back after the reload, the file
  written again at load (808 bytes), the crate empty after the reload with Decimation's own line, the shelf kept by its
  origin, its lower part empty, the supply drop untouched (gone after closing, no record), refill on 1 minute: shut 55 s
  after its loot, open after it, the record moved to the new loot. Regressions in the same game: loot, lootmags, radset
  58 PASS (their crate resets call LootRefill.clearAll now).
- Performance (budget: under 20 us a click, nothing per tick): our two handlers 0.48 us a click (5000 warm clicks); a
  record of 10000 spots written in 1.8 ms, read in 1.4 ms, 55 KB on disk; nothing runs per tick or per frame, no packets.
  Memory about 80 bytes a spot in the HashMap [inferred].
- Bug review: nothing is given or taken (only whether Decimation rolls); a crash between autosaves loses the marks since
  the last save (those spots refill: the player's favour); the loot hint still shows on a looted spot (Decimation asks the
  client's map, which nothing marks; not in scope, spec A); a broken and replaced loot block keeps its spot's mark [my
  call: rare, and a looted spot should not refill by breaking it]. Not verified: a dedicated server's first load
  (refill true written) and a real game restart (the test empties Decimation's map instead).

## 3D gun converter, milestone 1: the converter (22 Oktober 2026, idea 27, agent 1, branch gunconv-tool)
- User, 21 and 22 Oktober 2026: a private page that converts a 3D gun model into a Decimation styled gun; two agents
  (converter, game side), first milestone one rifle end to end. Spec docs/superpowers/specs/2026-10-22-gun-converter-
  design.md; how it works, results, limits: docs/gunconv.md.
- Built: tools/gunconv (meshio.py readers for GLB, glTF, OBJ and Blockbench .bbmodel; fit.py axes, pieces, cross
  sections, Decimation shape kinds; match.py Decimation's guns as references: like gun, alignment, palette, anatomy;
  convert.py the pipeline and command line; package.py the gun package and study.py's gaps; edit.py the reshapes;
  server.py and web/ the private page on 127.0.0.1 with three.js 0.160.0). Sources: docs/references/gunconv_sources.md.
- Packages (build/gunconv/out, git ignored): ak47 from Lamoot's CC0 "High poly AK-47" (the milestone rifle, 205
  parts, like akm, 2 of 19 study.py metrics outside Decimation's rifle range, 2 parts not seated) and akmc from
  J-Toastie's CC BY 3.0 "AKM" (153 parts, 2 of 19 outside, 7 not seated).
- Checks: 73 unit tests (RED first for every step); the page driven with agent-browser (sample loaded, converted, a part
  selected, moved by one step, reshaped, dropped, undone, exported, the 8 files on disk; a GLB uploaded and converted;
  both colour themes; no page errors).
- Found on the way: study.py's renders were scaled by the gun's part count (bug.md, fixed).
- Not done here: the game side (agent 2: register, aim, fire, reload, grips, NPCs, gunperf). Not verified in game.

## 3D gun converter, game side, milestone 1 (22 Oktober 2026, docs/ideas.md 27; agent 2 on branch gunconv-game)
- User: "a model file in, a game gun out", two agents (the converter tools/gunconv and the game side), milestone 1 one
  rifle end to end. Spec docs/superpowers/specs/2026-10-22-gun-converter-design.md; the loader, field by field:
  docs/gun_model_spec.md section 7.
- Built (fixes/gunpack): GunPack (gun.json read and checked, every problem at once, unknown fields only warnings),
  GunPackSource (package folders; the jar's assets/deciworldgen/gunpacks/index.txt), GunPacks (found in order:
  -Ddeciworldgen.gunpacks folders, the dev checkout's build/gunpacks, the jar; checked before any item exists;
  registered at preInit right after NewGuns; loot; our lists in postInit), GunPacksClient and GunPackResources (one
  resource pack of the deci domain serving only each package's own paths and a generated lang file, installed before the
  first gun loads its model; the like gun's sounds; its hands when the model has none; barrel shifts), GunPackRows (the
  sight, grip and mount table lines; GripRails, SightMounts, SightPlacement and GripPlacement read their table through
  GunPackRows.with, one line each), GripFit (tools/guns/gripfit.py in Java), PackModel (a .bmodel as geometry, as
  study.py). Deci: newGunLike (a copy of the like gun's GunStats and gun fields with the overrides), gunFacts,
  isMagazine, magazineBullet, gunHands / setGunHands, gunModelParts, gunSoundPaths, gunAnimationFound. LootPools.addLike.
  Dev: tools/guns/gunpack_standin.py (gcmac10 from our MAC-10, gcm4a4 from Decimation's M4A4 local only, gcbroken),
  `-Pgunpacks=DIR`, `devtest.py --live --gunpacks=DIR`, dev test `gunpack` (`perf=true`), playerfire takes `gun=`,
  GunTest reads `gun=` per run (its static field kept the first value for a whole live session).
- Decided on the way (agent 2, reasons): packages live as self contained folders (the contract's layout) in the jar
  under assets/deciworldgen/gunpacks/NAME/ with an index (a jar cannot be listed reliably, and the index fixes the
  order); dev folders come first so a converter's newer output wins over a shipped copy; "like" must be a Decimation gun
  (our guns' sounds are themselves redirected); grip rows come from the model's geometry with gripfit's own algorithm
  (the contract gives the rail's x span, not its height; same numbers as every other gun's rows), points.gripRail
  decides whether a foregrip fits and bounds where its seat goes; lights use the same fit on every gun; the sight shift
  from "rail" puts a long scope (x 0.8 .. 5.5) on the top rail; license "test" (packages made from Decimation's art)
  only in the dev workspace; added optional fields: magazine.icon, magazine.display, points.sightShift,
  points.barrelShift.
- Checks, unit: 229 PASS (30 new). RED first: with GunPack.parse stubbed GunPackTest failed 9 of 9 and GunPackRowsTest
  4 of 7 (one waiting for the readers to take package lines), then GREEN. GripFitTest reads the shipped
  grip_offsets.txt: 230 rows reproduced within 0.0015 (14 skipped: UMP9, HK416, HK416 Tan, Mk18 exist only where their
  generators ran), 666 ms for about 100 guns.
- Checks, live dev client (port 25594): `gunpack` 46 of 46 with the stand ins, then all PASS with agent 1's first
  rifle too (akmc, "AKM Converted", J-Toastie's CC BY 3.0 AKM, their output of 17:30): each gun registered as
  deciworldgen:NAME of its category; stats 19 of 19 facts equal to the like gun's or its override (gcmac10's seven
  overrides of the Uzi); magazines the like gun's (akmc: ak74Mag, aks74uMag, rpk74Mag, ak12Mag, ak60Mag) or its own
  (gcmac10Mag, 30 rounds of the UMP45 magazine's round); crate pools as the like gun (7 for the AKM, 8 for the M4A4 and
  the Uzi), our lists as the like gun (1); wear as its category (rifle 1500, SMG 1800); foregrip and RK-1 only with a
  gripRail (Gunsmith rules and MountStrip: kept on gcm4a4, taken off akmc and gcmac10); sight mounts (rail, side: the
  EOTech fits, the red dot not); model parts as declared top level (akmc 153, gcm4a4 228 of 229: one child, gcmac10
  110); files served, animations found, name, icon deci:gun/CAT/NAME, 6 sounds at the like gun's files, hands from the
  model; every sight and grip row placed on the gun's attachment copies (gcm4a4 12, gcmac10 3, akmc 2) and drawn from
  one list; gcm4a4 and gcmac10 got exactly the rows of the M4A4 and the MAC-10 (12 and 3 attachments); gcbroken refused
  ("missing ... gcbroken.png"), nothing registered. Loader time at startup 135 to 180 ms for three packages (akmc 82 ms,
  gcm4a4 53 to 113 ms: the first fit pays the JIT); nothing runs after startup.
- `gun gun=gcm4a4 mag=deci:m4a4Mag`, `gun gun=gcmac10`, `gun gun=akmc mag=deci:ak74Mag`: reload loads 30, the reload
  pose plays, a bandit carries each and fires (gun_npc_fire_110: tracer and flash). `playerfire gun=...` (the mouse
  path): gcm4a4 hits for 8 (the M4A4's), gcmac10 for 6 (its override 12, halved), akmc for 8 (the AKM's 15 halved and
  rounded), one round each on server and client, a miss uses a round and hurts nothing.
- `gunview` next to the like guns: hip and iron sight aim of gcm4a4 / gcmac10 identical to the M4A4 / MAC-10; the
  EOTech and ACOG on gcm4a4 placed and aimed as on the M4A4 (reticle centred, magnified glass learned on the first aim);
  the converted AKM holds like the AKM, the ACOG on its side mount aims identically, its iron sight post sits on the
  centre (the sight picture differs: below). `gripsweep`: Foregrip and RK-1 on gcm4a4 where the M4A4 has them, the
  Laser on gcmac10 where the MAC-10 has it, the lights under akmc's handguard as under the AKM's.
- Cost, `gunpack perf=true` (each gun drawn once more a frame as Decimation draws it, glFinish around, medians of 200,
  order alternating): akmc 0.786 ms (CPU 0.317) against the AKM's 0.879 (0.375): cheaper, 153 parts against 194;
  gcm4a4 against the M4A4 +0.022, -0.018, +0.028, +0.009 ms in four runs; gcmac10 against the MAC-10 +0.015, +0.009,
  +0.005, -0.017 ms (noise of the order of 0.02 ms, budget 0.05); 0 GL errors, no stack change. `gunperf` on a loaded Mac
  (load 6 to 10, other agents' games): readings swing 4 to 9 fps for the same gun; held, the AKM 39 and 39 against akmc
  37 and 40; M4A4 and gcm4a4 averaged over two passes 55.5 and 58.5 held.
- Dedicated dev server (servertest, port 25593): the same packages registered with the same rows (gcmac10's barrel
  shift and every sound are client only), loot and lists as in the client, no client class loaded, Done in 4.9 s.
- Found: Decimation's gun icons (M4A4, AK-74, MP5A3, Uzi, Glock 17, SVD) point the muzzle LEFT with a dark outline; our
  MAC-10's icon and akmc's point it right (the decimation-gun skill says "muzzle right"): the skill and Decimation
  disagree, not changed. akmc's icon is 9 rows high against the AKM's 21. akmc's aimed iron sights show a single post on
  the centre where the AKM shows its rear notch ears with the post between them. The stand in gcm4a4 shows the M4A4's
  rainbow rail teeth under a sight (bug.md "Colourful rail teeth": IronSights hides them only on the gun named m4a4);
  a real package has its own texture. Minecraft's TEXTURE ERRORS report names our pack's class among the deci packs
  (FML knows only its own pack types); the 7 missing textures are Decimation's own, the same in main.
- Not verified: shipped packages in game (the jar path is only unit tested: GunPackSourceTest); the production client
  with OptiFine (the main session's ports); a client joined to a dedicated server with packages; client and server
  with different package sets (both need the same packages, as with any mod's items) [not handled]; NPC loadouts do not
  pick package guns (an NPC carries one only when given, as in the gun test) [open: an optional "npcs" field].
- Open: the main session merges both branches, then the user's look in game; agent 1's sight picture and icon points
  above; for a release, the package folder and its index line (docs/gun_model_spec.md section 7).

## Loot rules B: lootable copies and loot kinds (22 Oktober 2026, feature 12 B, ideas 49, 50)
- fixes/LootBlocks: 15 blocks of ours made from Decimation's own classes (Deci.copyProp copies every BlockProp field and the
  Block look; Deci.newMetalShelf, newWeaponCabinet), each bound to a Decimation list through a loot entry of its own made
  from a block that has that list (Deci.addLootBlock): six pickup trucks (car wreck list), water pallet and tarp (food and
  drink), big and small military radio (military), a plain wooden crate (no list), police and military metal shelves,
  "Cabinet" (civilian, general list) and a police weapon cabinet. Tooltip "Lootable: <kind> loot" (originals of the shelf
  and cabinet too). World generation: Dress.lootKinds swaps shelves and cabinets by building (police, military and the
  wasteland military, the rest civilian; the original cabinet stays the military one), CanvasPlan swaps them in bases and
  airports. Worlds already generated keep what they have.
- Checks: unit LootBlocksTest 5 (RED with the mapping broken, then GREEN); dev test lootblocks 23 PASS (15 copies, each of its
  original's class, light, size, tab, hardness; names and tooltips; each copy draws as its original at one spot with one
  camera, 0.000 % of pixels apart for 13, trucks 0.01 to 0.26 %; a real click opens only its list's items, the plain crate
  nothing; a police shelf placed from its item fills both cells with one loot spot; forPlace 5.88 ns for any other block).
  Dev dedicated server seed 1: 15 copies, 14 with a list; around 1536,512 109 wasteland buildings as military (9 shelves
  swapped), the police office at 336,544 got 3 police cabinets (counted in the saved world). The pass costs 73 us a
  building (median, p90 207 us). Regressions loot, lootrules, lootmags, milbase 46 PASS.
- Trap: Block.getCreativeTabToDisplayOn is client only (a dedicated server strips it: NoSuchMethodError at start).
- Carried to prerelease (611a767 with its test fix; there lootblocks all PASS, 158 unit tests). Production client with
  OptiFine 61 PASS (lootblocks, lootrules, loot, glstack); production dedicated server: 15 copies, the loot file written with
  refill=true, 15 minutes (docs/production_test.md).
## 3D gun converter, milestone 2: dataset and exemplar areas (22 Oktober 2026, agent 1, branch gunconv-tool)
- User: "make dataset full of decimation guns and used as data so the converter can be good enough"; chosen: exemplar
  areas. Spec section "Milestone 2"; results, numbers and limits: docs/gunconv.md section 10.
- Built: tools/gunconv/dataset.py (anatomy labeller, 83.1 % agreement with Decimation's own stock, grip and barrel
  names; dataset in build/, aggregates docs/references/decimation_gun_areas.tsv), tools/gunconv/exemplar.py (closest
  exemplar per area, the like gun first, refit, overlap gate, tones), the converter step, the report in gun.json and on
  the page, icons in the like gun's proportions. Tests: 85, RED first for the labeller, exemplars and icon.
- ak47: 74 of 204 parts from exemplars; akmc: 120 of 162 (receiver, stock, grip, magazine of the AKM fitted to the
  source). Page smoke test passed (agent-browser, port 8791). Not verified in game.

## Loot rules C: Tarkov style search (22 Oktober 2026, feature 12 C, idea 29)
- User: "loot system where it need to be analyzed one by one before taking it. so the item will show loading icon animation
  then if it done it can be take it"; chosen: by value, 1 to 3 s an item.
- fixes/LootSearch: a loot inventory starts unsearched; the server reveals its filled slots in slot order, each after its
  time (guns, keycards, gold bottlecaps 3 s; attachments, armor, vests, masks, backpacks, repair parts, filters 2 s; the rest
  1 s), only while its screen is open; the progress of an inventory is kept when the screen closes (a crate rolls a new
  inventory each open, our loot bodies keep theirs). Our GearContainer refuses every click on an unsearched slot (and a
  double click gathering skips them) on both sides; Net message 4 sends the progress when the screen opens and at each
  reveal; the client draws a dark "?" over unsearched slots, a turning dot ring and a progress bar on the one being
  searched, and an unsearched stack has no tooltip (cleared at the lowest priority: Decimation adds its lines after ours).
  Switch: -Ddeciworldgen.lootsearch=false (LootSearch.enabled; dev test invshift turns it off for its loot step).
- Checks: unit LootSearchTest 4 (RED: no class, then GREEN); suite 271. Dev test lootsearch all PASS: every filled slot hidden
  at first on both sides, a click and a shift click on one take nothing, no tooltip, the first slot revealed after 60
  server ticks (expected 60), all after 100 (expected 100), a searched slot taken, a loot body kept 27 ticks of its gun's
  search while closed and went on from there. Shot docs/shots/lootsearch_v0.66.1. Regressions invshift, loot, lootrules,
  lootblocks PASS; lootmags's "a looted loaded gun fires" failed in 3 of 5 runs, with the search on and off alike (passed
  with it off once and on once): the shot depends on the looted gun picked [not verified which guns]; its check now names
  the gun.
- Performance (budget: server under 20 us a tick, drawing under 100 us a frame, a packet only at a reveal): server 4.42 us a
  tick (median of 451, with a loot screen open), drawing 45.2 us a frame (median of 154); one message per reveal.
- Trap: a busy Mac (an emulator at 228 % and a forgotten process of mine at 90 %) tripled averages: costs are medians now.
- Security review (automated, same evening): fixed, test first (LootSearchTest 6): the server failed open between the screen
  opening and its first tick (no search yet: a quick click could take an item), now hidden() starts the search there and
  fails closed; a body two players look at was searched twice as fast, now a search ticks once a server tick. Known limit,
  left as is: the client still receives the hidden stacks (Decimation's PacketLootInventory and the container sync send the
  whole inventory), so a modified client could read what is inside before the search ends; taking is refused on the server
  either way. Hiding them needs placeholder stacks in that sync [open; matters for public servers, docs/release_1.0.md].
- Carried to prerelease (from main 0233182 and 757fa57; lootsearch and invshift PASS there). Production client with OptiFine:
  lootsearch, invshift, loot, glstack 75 PASS; the production dedicated server starts clean with it.

## Trader market by day (22 Oktober 2026 night, feature 11, idea 39)
- User: "trader now have dynamic market supply and demand, its randomize by day in minecraft"; chosen: daily drift plus
  player trades, both easing back over days, a few items on sale and in demand per trader and day. Plan
  docs/superpowers/specs/2026-10-22-trader-market-design.md.
- fixes/TraderMarket: Decimation keeps one offer list per trader class and checks a trade's price against it; a new trader
  object puts a fresh list of base prices in. The market keeps those bases and writes the day's offers into the lists at
  dawn (a new day by WorldClock), after each trade, when a trader joins a world, and on clients from the price table it
  sends (Net message 5, at login, each new day and after trades). Price = base x drift (0.85 .. 1.15 by world seed, day
  and item) x pressure (1 + 0.03 a unit, 0.6 .. 1.6; a buy +1, a sale -1; 30 % back toward 0 each day), whole caps,
  buy at least 1, sell under buy; up to 3 offers on sale (buy at 70 %) and 3 wanted (sell at 150 %) per trader kind and
  day, shown in the trader screen's tooltips ("On sale today", "Wanted today"). Trades are seen by a handler of ours in
  Decimation's own server channel right after its codec (no jar patch) and counted on the next server tick only when the
  player's caps changed. Pressures saved per world (data/deciworldgen_market.dat).
- Checks: unit TraderMarketTest 4 (RED: no class, then GREEN); suite 277. Dev test market all PASS: the food trader's 29
  offers at the day's prices on server and client alike, 3 on sale and 3 wanted, ten buys raised that item 11 -> 13
  (pressure +10), five sales took another's pressure 5 down, a buy at the morning's price refused once it rose, the next day
  eased the pressure 10 -> 7; trade (Decimation's own trade test, now reading the price of the moment) PASS; camp PASS.
- Performance: a refresh of every trader kind 0.06 ms (only at dawn, per trade and for a trader kind's first spawn), the
  table 276 bytes; nothing per tick but a queue poll.
- Security review (automated, same night), fixed test first (TraderMarketTest 6): a trade now counts only when its own
  message moved the player's caps by exactly its price, read right before and after Decimation handles it on the same
  network thread, and only for an item the trader's class offers (a forged or refused message, or caps moving for another
  reason, pushes nothing; no client chosen keys in the saved map); prices go back in whenever a class's list holds offers we
  did not write (each server tick, before each trade, and on cancelled joins, whose constructor reset the list); no trader
  buys an item for as much as any trader sells it that day (no buying here and selling there for profit; per currency).
  The saved pressures are written under the same lock as a trade's update. Rerun: market (15 trades seen, 15 counted) and
  trade PASS, suite 279.
- Second security round (23 Oktober 2026), fixed test first (TraderMarketTest 7): buying pushes only buy prices ("b:" pressure)
  and selling only sell prices ("s:"), so buying a stack up never raises what selling it back pays; the network thread only
  reads the caps before and after a trade message and queues it, the server thread checks the item against the trader's
  offers and moves the pressure (no world data or offer lists touched off the server thread). Rerun: market (15 of 15
  counted) and trade PASS, suite 280.
- Third round (same night): with the trader check off the network thread, a class list reset by a new trader object held
  base prices until the next server tick; now the watcher drops any trade message whose price is not one of today's for
  that item (a read of the day's table, swapped whole at each refresh) before Decimation sees it (TraderMarketTest 8).
- Not verified: the tooltips in the trader screen (no shot: the screen opens through Decimation's dialogue); a dedicated
  server with a joined client (the table and the trade watcher there); the production client.

## Gun converter milestone 4: converted guns as real meshes, 1:1 (23 Oktober 2026, main)

User: "use convert 1:1 instead generating decimation style guns"; "pixelated like decimation, cap 30000 triangles".
Spec docs/superpowers/specs/2026-10-23-gun-mesh-design.md; everything in docs/gunconv.md section 13.
- Converter: mesh mode is the page's one Convert and the command line's default (tools/gunconv/meshpack.py, convert_mesh,
  export_mesh): the source's triangles (simplified only over 30000), smooth normals at 40 degrees, a pixel texture of our
  own charts (a texel a quarter unit) in the like gun's tones, the skeleton .bmodel, the icon from the mesh, Decimation's
  animation timings. 180 converter tests.
- Game: fixes/gunpack/MeshGun + ModelBatch draw each group's triangles in place of its placeholder part; GunPack "mesh".
  288 Java unit tests (plus a hosting test isolation fix, ColosTest).
- Checked in game: dev tests gunpack (mesh checks) and meshgun (each group follows its part), the dev client and the
  production client with OptiFine; cost: 5,240 triangles 0.87 x the akm's draw, 30,000 1.50 x (dev), 0.77 x and 1.33 x
  (production); graphics memory no step at the first draw.
- Not done: the user's look in game, a release with a package; prerelease gets nothing (the gun package loader is main
  only).

## Durability v2: icon bars, the gun tooltip, vests (23 Oktober 2026, user)
- User: "for gun show durability on hover, and the icon gun item has progress bar in there showing durability like native
  progress bar. vest armor still doesnt affected when hit by zombie or shot"; "for gas mask, you better add durability
  progress on it, so user know remaning the durability".
- Bars: Forge's own bar (RenderItem.renderItemOverlayIntoGUI asks Item.showDurabilityBar / getDurabilityForDisplay) on
  guns by wear and gas masks by filter left. tools/patches/PatchDurabilityBars.java adds both methods to GunItem (deci/ay/i,
  on top of PatchGripRecoil and PatchGunWear) and ItemGasMaskDeci (three jars), answered by fixes/DurabilityBars
  (Durability.barShown / barFraction). As on a vanilla tool the bar shows once something is used up; a worn out gun or an
  empty filter shows the empty track. Armor and vests had vanilla's bar already (item damage). Guns do not mirror their
  wear into item damage: Decimation's traders refuse a damaged item.
- Tooltip and vests: bug.md (Decimation wiped our gun line; vests now wear on every shot and on zombie hits).
- Checks (dev client): durability 27 PASS (bars: worn gun 0.5, half filter 0.5, empty filter 1.0, full filter none, new
  gun none, vest 0.5; tooltip line; vest wear and its client copy), radiation 16, radset 39, tracer 3, glstack, gun: 88
  PASS.
  Hotbar picture docs/shots/durability_v0.70.0/bars_hotbar_4x.png. Cost measured (durability "bar hooks cost", dev
  client, median of 5 batches of 200,000 after warm up): 55 ns a stack for both calls, so a full inventory of 45 guns and
  masks costs about 2.5 us a frame; no allocation.

## NPC tracers white again (23 Oktober 2026, user)
- User: "oh yeah pls make the npc shot white tracer again, just changing the color only, the tracer each shot still intact".
- NpcLoadouts.withTracer gives every NPC Decimation's GENERIC (8, #FFF6E8, the near white Decimation drew NPC shots in)
  and replaces the random colours saved since 22 Oktober 2026 when the NPC loads; a gun with a colour id draws a tracer on
  every shot, so that stays.
- Checks: unit NpcTracerTest; dev test tracer "every shot a white tracer" RED ({GREEN=81}) then GREEN ({GENERIC=79}).
