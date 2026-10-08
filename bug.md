# Bug Tracker - Decimation Mod

Status values: `Open` (found, not fixed) / `Fixed` / `Pending decision` (fix
known, waiting on user go-ahead).

For "known non-bugs" (behavior that looks broken but is original mod design,
not something we introduced or need to fix) see `documentation.md`.

---

## Fixed

### Upper storeys unreachable (ladder popped off, stair core collapsed)
- **Found**: 7 Oktober 2026 by the building audit (`tools/floorplan.py`
  reachability): shop b-4_1_0 storey 1 0% reachable, office b-3_2_2 top
  storey 0% reachable (seed 1, Decimation world type).
- **Root causes (ours)**: (1) buildings without a stair core use a ladder
  at (W-2, L-2) hanging on the back wall. The wall cell could be a window
  or a decay hole, and the wall column often belongs to the NEXT population
  window, so it does not exist yet when the ladder is written; any block
  update meanwhile (a metal shelf next to the ladder being completed with
  flag 3 since 0.16.0) makes the unsupported ladder pop off. (2) The
  collapsed corner cone also removed ladder and stair core cells on the
  top storeys.
- **Fix (v0.16.1)**: the wall cell behind the ladder is always solid wall;
  no furniture on the 4 cells around the ladder; ladder shaft and stair
  core (plus a 1 block ring) are exempt from the collapse. Rescan of 105
  buildings: no storey 0% reachable. Remaining low reachability comes from
  full rubble cubes in doorways (step 4d rubble redesign; jumpable in game).

### Supply drop crate vanished on a street prop
- **Found**: 7 Oktober 2026 by the dev autotest after street furniture
  (0.15.0): drop column 14/62 was a sidewalk with a trash bag on it.
- **Root cause**: same mechanism as the flower case below. Decimation props
  have collision boxes, the falling crate stops on one inside the prop's
  cell, cannot become a block there and dies.
- **Fix (v0.16.0)**: `SupplyDropScheduler.drop` skips a candidate column
  whose top block has a tile entity (any prop, chest, car) and tries the
  next of its 12 random positions. Autotest passes again.

### Supply drop crate vanished when it landed in a flower
- **Found**: 7 Oktober 2026 by the dev autotest (drop column 70/287: crate
  entity fell, no crate block anywhere in the column).
- **Root cause (vanilla behaviour, our column choice)**: the crate is an
  `EntityFallingBlock` (`EntityFallingSupplyDrop`); on landing it turns into
  a block only if the cell is replaceable. Flowers, saplings and the tall
  flowers are not (`BlockDoublePlant.isReplaceable` is true only for tall
  grass and fern), so the entity died and the drop was lost. The column had
  a 2 block tall flower.
- **Fix (v0.13.0)**: `SupplyDropScheduler.clearLanding` removes
  non-replaceable plants in the drop column down to the first solid block or
  liquid before deploying. Autotest passes again (crate block at y=71). The
  flower case itself was not reproduced (drop columns are random).

### Graded yard sand fell into caves (hole next to a building)
- **Found**: 7 Oktober 2026 by `tools/gradescan.py` on seed 7 while building
  terrain blending (a ring column 49 blocks below the floor).
- **Root cause (ours)**: the grader filled raised yard columns with the
  column's own surface block. Sand and gravel are `BlockFalling`;
  `setBlock` calls `onBlockAdded`, which schedules the fall even during
  generation, so a sand fill over a cave dropped into it.
- **Fix (v0.13.0)**: falling blocks are never used as fill (sand fills with
  sandstone, gravel with stone), and a sand or gravel top goes only on a
  solid block. Rescan of 3 seeds: ground at the walls within 1 block of the
  floor everywhere.

### Generated metal shelves invisible (multiblock master never set)
- **Reported**: 7 Oktober 2026 by the user; the culling patch below did not
  fix it (still invisible right next to the prop).
- **Root cause (ours)**: multiblock props (tile entities extending
  `deci.W.a` MultiblockPart, e.g. `deci:BlockMetalShelf`, MetalShelf_Empty)
  are drawn only by their master part: `MetalShelfRenderer` returns unless
  `isMaster()` (stored master position == own position). The master is set
  by the block's placement code; world generation places the block directly,
  so the master stayed 0,0,0 and the shelf never rendered. Found by reading
  the tile entity NBT (`multibl.mx/my/mz` = 0) in a generated world.
- **Fix (v0.12.3)**: `Slices` and the small-schematic placer call
  `setSelfMaster()` on every multiblock part they place;
  `MultiblockRepairHandler` makes orphaned parts (master never set) their own
  master on chunk load, which also repairs worlds generated before 0.12.3
  and any legacy Decimation prop broken the same way. Verified: new world
  407/407 shelves are masters; a copy of the user's pre-fix world: every
  shelf in the loaded area repaired, the rest repair when visited.

### Props randomly not rendered (legacy Decimation bug)
- **Reported**: 7 Oktober 2026 by the user (screenshot: selection box of a
  placed prop, no model). Happens with player-placed props too, mostly
  props whose model is bigger than one block (tables etc).
- **Root cause**: `PropRenderer` (`deci.I.l`) first calls
  `LineOfSight.canSeeTileEntity` (`deci.a.c$a.a(EntityPlayer,TileEntity)`):
  8 rays from the eye to the 8 corners of `TileEntityProp.getRenderBoundingBox()`,
  drawn only if one ray is clear; Forge's frustum culling uses the same box.
  That box is the bare 1x1x1 cell for 36 of 72 props (no render size
  declared) although their models are drawn up to ~1.5 blocks wide, and it
  never rotates with the prop (the model turns by metadata % 4 * 90). So the
  corners land off screen or behind floors / walls while the model is in
  view, and the prop is skipped.
- **Fix (Javassist, `tools/patches/PatchPropCulling.java`)**:
  `getRenderBoundingBox` returns a box centred on the block, rotation proof,
  at least 2x2, covering the declared render size, half a block taller;
  `canSeeTileEntity` returns visible within 4 blocks and tries a ray to the
  box centre first. Applied to `Decimation.jar.patched`,
  `dist/Decimation.jar`, `dev/libs/Decimation-base.jar` (Prism needs the new
  `dist/Decimation.jar`). Verified: classes load, no render errors in the
  autotest, a table's box is now 99.5..101.5 instead of 100..101. Visual
  confirmation pending. NOTE: the user's invisible shelf was actually the
  multiblock bug above; this culling fix still stands for normal props.

### City buildings missing a whole wall at sector borders
- **Reported**: 7 Oktober 2026 by the user (0.11.0 in game: "one side has no
  wall, looks cut off like old village bugs").
- **Reproduced**: `tools/wallscan.py` on seed 1 + pregen: 2 of 33 buildings
  had a side 97% air. One was only the edge of the generated area (owner
  chunk never populated). The other (`b4_4_2`, west wall x=263) was real.
- **Root cause**: a chunk's population window is 16x16 at +8/+8, so it
  reaches 8 blocks into the neighbouring chunks and can cross a SECTOR
  border. `StructureGenerator.generateImpl` only ran the city writer when the
  populating chunk itself was in a CITY sector; a building in a city sector
  whose column fell in a window owned by a non-city chunk never got written.
  The large-schematic writer had the mirror bug (not run for city chunks).
- **Fix (v0.11.1)**: both slice writers run for every chunk (each checks the
  sector of the structure, not of the populating chunk); only small ruins
  and streets use the populating chunk's sector. Rescan: 37 buildings, 0 real
  missing walls, 4 gaps all at unpopulated edge chunks.
- **Note for players**: buildings straddling chunks generated by an older
  version (or never populated yet) still look cut off until those chunks
  populate; use a new world for a clean test.

### No supply drops in singleplayer
- **Found**: 7 Oktober 2026 (naming pass).
- **Root cause**: the 15 minute countdown lives in `ServerTickHandler`
  (`deci.aK.q`, ServerProxy only) and every `SupplyDropSpawner` (`deci.aM.c`)
  method checks `isServer()`. Drop points came from an admin file
  `decimation_supplydrop.properties`, which singleplayer never has.
- **Fix (v0.9.0, dist only)**: `SupplyDropScheduler` runs the same countdown
  (`ServerConfig.supplyDropCountdown/Interval`, 18000 ticks), the same 10 and
  5 minute chat warnings, then places the mod's own falling supply drop block
  at a random LOADED column 64 to 144 blocks from a random player and
  announces the coordinates. Honours `enableSupplyDrops`. The crate drifts
  down for roughly 45 s. Verified by the autotest (dropped, landed at y=69).

### Humanity never changed from ordinary kills in singleplayer
- **Found**: 7 Oktober 2026 (naming pass).
- **Root cause**: `DeathHandler` (`deci.an.f`) runs in singleplayer, but its
  "+1 per non player kill / -10 for killing an innocent player / +1 for any
  other player" block is behind `isServer()`. Only the victim's own
  `getHumanityOnKill()` value applied.
- **Fix (v0.9.0, dist only)**: `HumanityKillHandler` replicates that block
  without the clan branch (clans are dedicated only). Killing one infected now
  gives +2 (its own +1 plus the rule's +1), the same as on the official
  server. Verified by the autotest (50 -> 52).

### Armor damage-reduction buff doesn't affect NPC gunfire
- **Found**: 27 Juli 2026, while trying to explain how to test the armor buff
- **Symptom**: `ItemArmorDeci.damageMultiplier` (recently buffed 35%, see
  `new_feature.md`) has zero effect when bandits/soldiers shoot the player.
- **Root cause**: the multiplier is only ever consumed in one place in the
  whole mod - `deci.aE.a$z$a`, the network handler for a **player's own**
  gunshot hit report (client tells server "I hit X for Y damage", server
  applies the multiplier there). NPC ranged attacks
  (`deci.ag.a.e(EntityLivingBase)`) call vanilla `attackEntityFrom` directly,
  server-side, with a different `DamageSource` type (`"human"` vs the
  player-shot `"gunDeci"`) - completely bypassing the multiplier.
- **Proposed fix**: add a `LivingHurtEvent` listener (fires for all incoming
  damage regardless of source), filtered to the known gunshot-type
  `DamageSource`s (both `"gunDeci"` and `"human"`, possibly more NPC variants
  to confirm), apply the multiplier there instead - and remove the
  now-redundant multiplication inside `deci.aE.a$z$a` so player-vs-mob shots
  don't get it applied twice.
- **Status 7 Oktober 2026: FIXED** (user said go). Implemented slightly
  differently from the proposal: the player hit handler `deci.aE.a$z$a` is
  left untouched (it only applies the multiplier when the target is a player,
  i.e. PvP), and `ArmorGunfireHandler` (LivingHurtEvent) applies the same
  rule to the NPC sources `"human"` (bandits, armed traders) and `"turret"`.
  Same pieces, same helmet exclusion, no double application, no bytecode
  patch. Every Decimation damage source bypasses vanilla armor, so before
  this armor did literally nothing against NPC bullets. Verified by the
  singleplayer autotest: 10 human damage, bare 10.00 taken, with a
  spetsnazVest (x0.553) 5.52 taken.
- **Helmets (v0.9.1, user decision: headshots only)**: helmets carry a much
  stronger multiplier (0.3 raw, 0.195 after the 35% buff) than other pieces
  (0.8 to 0.85), which is probably why the original skipped them; on every
  hit a full set would block about 97%. Now: player guns (`gunDeci`) add the
  helmet only when the shooter's aim line crosses the victim's head band (top
  25% of the body); NPC/turret hits use it on a random 20% (no hit position,
  and NPCs always aim at eye level). Verified: 81/400 NPC hits used it; a
  player gun aimed at the head took 4.88 (helmet x0.487), at the chest 10.00.

### Background thread pinned one CPU core all session (SmoothSwingThread)
- **Found**: 7 Oktober 2026 by the naming pass, verified in code.
- **Root cause**: `deci.b.h.run()` (thread "SmoothSwingThread", started by
  ClientProxy) busy-waits with no sleep to advance the weapon sway clock
  `deci.b.i.bz` at 60 Hz.
- **Fix**: Javassist patch (`tools/patches/PatchSwing.java`), same 60 Hz
  accounting plus a 4 ms sleep, applied to `Decimation.jar.patched`,
  `dist/Decimation.jar`, `dev/libs/Decimation-base.jar` and Prism. Measured
  on Prism, idle main menu: 200% CPU -> 105%. Thread dump shows it sleeping.
  Weapon sway not yet looked at in game. May help the FPS reports below
  (less heat / throttling), not a proven fix for them.

### Prism instance crashed on every launch (CustomSkinLoader)
- **Found**: 7 Oktober 2026, while testing the separate deciworldgen jar.
- **Symptom**: crash during startup, before any mod loads, with
  `NoClassDefFoundError: net/minecraftforge/fml/relauncher/IFMLLoadingPlugin`.
- **Root cause**: `CustomSkinLoader_Universal-15.0.1.jar` (added to `mods/` on
  24 Agustus) ships a coremod built for Forge 1.8+ package names. Not related
  to our changes; the last successful launch log before it was 15 Agustus.
- **Fix**: renamed to `CustomSkinLoader_Universal-15.0.1.jar.disabled`
  (rename back to restore). A 1.7.10 build of CustomSkinLoader would be
  needed to keep custom skins.

### Generated structures built on the ocean floor
- **Found**: 7 Oktober 2026 (dedicated-server test, region file inspection).
- **Root cause**: `World.getTopSolidOrLiquidBlock` skips water in 1.7.10, so
  our "is this site water" check inspected the seabed instead of the water.
- **Fix**: v0.7.0 checks the block above each sampled ground point. Verified:
  same seed, all 4 underwater sites now rejected. Older worlds keep theirs.

### Zones never active in singleplayer
- **Found**: 7 Oktober 2026.
- **Root cause**: zone list loaded only by ServerProxy, zone handlers
  `deci.aK.d/g/i/m` are `@SideOnly(Side.SERVER)`. Same pattern as the loot bugs.
- **Fix (partial)**: v0.7.0 loads per-world generated zones and replicates the
  zone spawn handler (`deci.aK.d`) in singleplayer, verified by the
  unattended singleplayer test. Player effects of safezone/radiation/scary
  zones (`deci.aK.g/i/m`) are still not replicated.

### Loot never worked in singleplayer (crash on right-click)
- **Found**: 25 Juli 2026
- **Symptom**: crate/car/ammobox/supply drop right-click did nothing (or later,
  crashed) in singleplayer only. Worked fine in old multiplayer.
- **Root cause**: `ClientProxy` (`deci.a.c`) never registered the loot
  interact handler (`deci.aK.k`) - only `ServerProxy` (`deci.a.e`, dedicated
  server only) did. Singleplayer's integrated server reports physical side
  `CLIENT`, so ClientProxy is what loads even though it's hosting the "server."
  Handler also carried `@SideOnly(Side.SERVER)`, which gets ASM-stripped when
  loaded under `CLIENT` regardless.
- **Fix**: added `deci.aK.k` registration into `deci.a.c.preInit()`, stripped
  the `@SideOnly` annotation from the handler method.
- **Fixed**: 25 Juli 2026, 13.12

### `ClassCastException: deci.a.c cannot be cast to deci.a.e`
- **Found**: 25 Juli 2026, 13.09 (crash report), right after the above fix
- **Symptom**: crash on interacting with any lootable block, immediately after
  the previous fix went in.
- **Root cause**: `deci.aK.k`'s handler also calls `deci.a.b.d()` (which
  hard-casts the proxy to ServerProxy) to reach the clan-turf manager for an
  unrelated permission check. Fine on dedicated server, crashes on
  ClientProxy.
- **Fix**: `deci.a.b.d()` made null-safe (`instanceof` check, returns `null`
  instead of throwing). `deci.aK.k`'s two call sites that use the result made
  null-safe too (turf check treated as "no restriction" when null - correct,
  since clans/turf don't exist in singleplayer anyway).
- **Fixed**: 25 Juli 2026, ~13.15

### Loot GUI never opens (sound plays, no window)
- **Found**: 25 Juli 2026, 13.20
- **Symptom**: right-click plays the loot sound and marks the position looted,
  but no GUI window appears. Second click reports "this is empty."
- **Root cause**: the GUI-open call is scheduled on a delayed-task queue
  (`net.decimation.mod.common.utils.b`), drained only by
  `deci.aK.q` (`TickEvent.ServerTickEvent`) - which is *also*
  ServerProxy-only, so the queue is never processed in singleplayer.
- **Fix**: added a new `TickEvent.ClientTickEvent` listener directly on
  `deci.a.c` (already registered to the event bus) that drains the same
  static queue. Deliberately did *not* register the full `deci.aK.q` class -
  it pulls in unrelated dedicated-server-only logic (network reconnect,
  anti-dupe scan, clan supply-crate announcements) not wanted in singleplayer.
- **Fixed**: 25 Juli 2026, 13.20

### Crash opening ammo crate (large)
- **Found**: 25 Juli 2026, 13.40 (crash report)
- **Symptom**: `NullPointerException` in `RenderItem` while rendering the loot
  GUI, specifically for the large ammo crate.
- **Root cause**: `deci.aD.k.avk` (an item field) is declared but never
  assigned anywhere in the mod - dead/leftover reference, always `null`. It's
  the first entry in the large ammo crate's loot pool
  (`deci.aD.l$4`, 25-entry list). Pre-existing bug in the original abandoned
  mod, unrelated to any of our patches - only surfaced once loot started
  working at all.
- **Fix**: substituted the dead field read for its valid neighbor
  (`deci.aD.k.avl`) in `deci.aD.l$4`'s constructor.
- **Fixed**: 25 Juli 2026, ~13.55

---

## Pending decision

(none)

---

## Not investigated yet
(Some entries below were fixed later and say so in their heading; the open
ones on 9 Oktober 2026: base height order, prop dense FPS, LED lamp floor (fixed)
at the end of this file.)

### Building base height depends on chunk generation order (ours)
- **Found**: 8 Oktober 2026 (fresh seed 1 worlds, `tools/worlddiff.py`):
  apartments b-1_3_0 and b0_3_0 sat at y 64 in one run and y 63 in
  another, with the same seed.
- **Cause**: `Slices.decideBase` samples the footprint where
  `world.blockExists` is true when the building's FIRST slice is written,
  so the result depends on which neighbour chunks happen to exist then.
  The chosen height is stored (StructureData), so a building is always
  whole; only the height can vary between two new worlds of one seed,
  near the edge of what was generated. Spawn area: identical.
- **Impact**: low (no broken buildings); matters for "same seed, same
  city" expectations and for refactor checks (use `--within`). A fix
  would sample terrain height from the chunk generator (noise) instead of
  the world `[not verified]` how costly that is.

### FPS drop while aiming through scopes (red dot/2x/4x/6x) (FIXED v0.28.0..0.28.4)
- **Reported**: 26 Juli 2026; user 8 Oktober 2026: about 150 -> 110 fps
  with a scope, asked for the old cheap method (view zoom) instead of the
  two camera method, keeping the old code switchable.
- **Root cause (code, deobf ClientRenderHandler.renderScopeView)**: the
  world is rendered a second time (narrow FOV, up to 1024 x 1024) into a
  texture every frame a scoped gun is HELD, aiming or not; BModel
  renderScopeGlass draws it on the glass.
- **FIXED v0.28.0**: `tools/patches/PatchScope.java` gates renderScopeView
  behind system property `decimation.scope.pip` (method otherwise
  untouched: the old scope returns with `pictureInPicture=true` in
  `config/deciworldgen_scope.cfg`). `net.decimation.fixes.ScopeZoom`
  zooms the whole picture while aiming (reddot 1.25, 2x, 4x, 8x, dragunov
  4x, configurable) and copies the centre of each finished frame into the
  scope texture (RenderWorldLastEvent, before the hand) so the glass looks
  see-through.
  - v0.28.0 zoomed only the world (FOV; the hand renders at a fixed FOV
    70, so the gun stayed 1x). User then chose the "NORMAL" look of a
    NORMAL / PIP comparison picture: world AND gun zoom together.
  - v0.28.1: EntityRenderer.cameraZoom = magnification (eased, log
    space). Vanilla renderWorld skips renderHand while cameraZoom != 1,
    so ScopeZoom calls renderHand itself on RenderHandEvent (fired right
    before that check); renderHand applies the same cameraZoom scale, so
    the gun zooms with the world. Glass copy sized glassView x zoom. The
    eyepiece sits a little above the screen centre at 4x, but the glass
    shows the screen centre (the aim point), so the reticle aims true.
  - v0.28.2 (user: glass misaligned fullscreen / windowed; gun swinging
    wildly above 1x in a first centring try): the WHOLE world frame is
    copied and the patched glass maps it by screen position (projective
    texturing), so it is see-through at any window size and zoom. The
    glass reports its screen box (GL feedback, every 4th frame); the gun
    is moved (cameraYaw / cameraPitch, hand only) so the sight sits on the
    screen centre where shots go. The sight position is learned per gun +
    scope + window aspect, only from unclipped samples and after 3 samples
    in a row agree (the first try corrected every frame from old samples
    and chased the weapon sway; a sample taken while the gun swung in from
    the hip pushed the glass off screen for good); the first aim with a
    new combination holds 1.5x up to 0.8 s while it learns. Mouse turning
    slows to 1 / zoom while zoomed (config "sensitivity"). Checked: reddot,
    2x, 4x, 8x, integrated (aug1) in 854x480 and 900x895, reticle on the
    centre in all 10 (docs/shots/scope_v0.28.2); a +-4 degree sweep moves
    the sight 6..25 px (normal weapon lag). Java 8 trap: Javassist on a
    newer JDK compiled FloatBuffer.flip() with the Java 9 return type
    (NoSuchMethodError in game): call it through java.nio.Buffer.
  - v0.28.3 (user asked for a faster, more efficient way): the copy now
    covers only the glass's last measured box plus a margin (whole frame
    while the zoom changes), into a texture allocated once per window size
    (glCopyTexSubImage2D); GL buffers allocated once; the glass is measured
    every 8th frame (every 2nd while a new sight is learned: 10 settled
    samples averaged, zoom held at 1.5x up to 1.5 s, which also fixed the
    integrated aug scope learning a still moving pose). Tried and dropped:
    a depth only glass drawn before the body (no copy at all): attachment
    glasses are drawn AFTER the gun body, so its front sight showed in the
    glass, and the integrated scope stayed grey. Checked all 10 (5 sights x
    2 windows) centred and see-through, docs/shots/scope_v0.28.3.
  - v0.28.4 (user: "for 8x better use the fake it method with a black
    layout", then "include 4x"): scopes from `overlayFrom` (config,
    default 4) hide the gun once the zoom is 70% in and draw a classic
    sniper overlay on the HUD (RenderGameOverlayEvent HELMET): black
    outside a round view (radius 0.47 x the smaller screen side), a
    darkening edge of 12 thin rings, the scope's own reticle texture
    (textures/model/guns/scopes/<name>.png, black lines on transparent)
    full size, vanilla crosshair hidden. No gun, no glass copy, no sight
    learning for those scopes. GL traps met: the circle strips wind
    backwards in GUI coordinates (cull face off), the HUD shades flat
    (per vertex colour fades became spikes: solid rings instead). A
    "white haze" at the edge was the sky fading to black, checked by
    pixel values (190,213,250 -> 149 -> 97 -> 56 -> 0). Checked 10 shots
    (docs/shots/scope_v0.28.4): reddot / 2x gun with see-through glass,
    4x / 8x / integrated overlay, reticle on the centre.
- **Measured** (`./gradlew runClient -Pautotest -Pscope`, dev client,
  unlimited fps, 4x on an ak74): picture in picture empty hand 85..103,
  held 45..51, aiming 37..44; view zoom empty hand 70..92, held 64..76
  (the gun model only), aiming 105..132. Shots docs/shots/scope_v0.28.0.
  v0.28.1 on a busy machine: picture in picture empty 26, held 10,
  aiming 10; camera zoom empty 28, held 32, aiming 42.
- First try also gated renderScopeGlass: the glass vanished and the scope
  showed its solid black body (looked like a bigger gun); reverted.

### FPS drop in prop-dense areas (cars/crates/shelves etc.)
- **Reported**: 26 Juli 2026
- E.g. ~100fps baseline down to 20-30fps around 15+ props, worse with 50+.
- **Measured 9 Oktober 2026** (`./gradlew runClient -Pautotest -Pprops`,
  last autotest world: stone platform high up, 15 x 15 mixed props;
  `-Pjfr` adds a Java Flight Recorder CPU profile, run/client/profile.jfr,
  read with JDK 25 `jfr print --json`; the client thread is `main` in dev).
  Before: empty 28..29 fps, 225 props in view 12..14, hidden in a stone box
  23 (box alone 24..27). Profile: 76% of PropRenderer (`deci.I.l`) time in
  ClientProxy.LineOfSight.canSeeTileEntity (`deci.a.c$a.a`): up to 9 ray
  casts per prop per frame, a chunk lookup (LongHashMap) per block per ray;
  hidden props pay all 9 and are not drawn anyway. canSeeEntity does the
  same for every living entity and vehicle.
- **IMPROVED (v0.28.6, PatchPropCulling step 3)**: both answers cached until
  the player (or the entity) moves 0.3 blocks, at most 1.0..1.3 s for props
  and 0.15..0.18 s for entities, staggered per object. (A first try with a
  plain 80..111 ms lifetime rechecked nearly every frame at low fps; a null
  player on the main menu preview crashed the cache until guarded.) After:
  line of sight 25 of 251 prop renderer samples (was 804 of 1053); prop
  renderer share of the client thread 21% -> 9%. All autotest checks pass;
  props and NPCs still render (docs/shots/props_fps).
- **Left**: what remains is drawing the models (100% in glCallList, driver
  / GPU work): 225 visible props still about halve the fps on the test
  platform.
- **City street, after the cache** (`-Pcityfps`: x 8.5, y 68, z 120.5
  looking north, seed 1 autotest world, + `-Pjfr`): props 5.5% of the
  client thread, chunk drawing 15%, chunk rebuild 12.7%, entities and tile
  entities 11.8%. Props are no longer the bottleneck there.
- **Render distance by size** (PatchPropCulling step 4, config
  `deciworldgen_props.cfg`: small < 0.8 block, medium < 1.6, large): 32 / 48
  / 64 measured 35..40 fps vs 41 at 64 / 64 / 64 in that street (noise, no
  gain) and can pop in, so the default is 64 for all (vanilla); lower it
  for open prop heavy places.
- **Baking static props into chunk meshes** (the full fix for open views
  with hundreds of visible props): not started; worth it only if the user
  finds places that still drop (atlas for every prop texture, both model
  formats to quads).
- Side finding: Decimation's postInit blocked about 5 s at launch connecting
  to its dead server (kryonet `deci.aP.a.e`), on the main thread; startup
  only, not in game. FIXED 9 Oktober 2026 (`tools/patches/PatchBackend.java`):
  connect timeout 300 ms, and kryonet's own Client.connect (bundled in
  Decimation.jar) now passes it to the TCP connect, which it hardcoded to
  5000 ms (shortening only the first timeout still waited 5 s: measured).
  Launch: "Registering new client network" -> "Unable to connect" went from
  5 s to under 1 s; later reconnect attempts still fail quietly in their
  own threads as before.

### Military jeep/tank/helicopter destroyed in one hit (survival mode) (FIXED v0.8.1)
- **Reported**: 26 Juli 2026, clarified 27 Juli 2026
- **Symptom** (clarified): a single click/hit destroys the vehicle outright,
  like breaking a normal block - it should instead be tough/unbreakable this
  way, same as other static props (e.g. crate/prop blocks return hardness `-1`
  or similar unbreakable-by-hand values). Not about health-bar/HP tuning -
  about whatever block/entity property currently lets one click destroy it at
  all.
- **Root cause (7 Oktober 2026)**: vehicle body `deci.ad.e.attackEntityFrom`
  (subclasses `deci.ad.f/g/h/i/j`, `i` = hummer) treats any player hit on an
  EMPTY vehicle as "pick up": `setDead()` + drop its item, survival included.
  Only gun damage (`gundeci`) takes the health path. Seat/hitbox parts
  `deci.ad.b` / `deci.ad.a` forward every hit to the body. It is the
  original design (minecart style pickup), not a singleplayer bug.
- **FIXED in v0.8.1 (dist only, Prism still 0.7.0)**: `VehicleHitHandler`
  cancels Forge's `AttackEntityEvent` for survival players on vehicles and
  their parts unless sneaking. Sneak + punch still recovers an empty vehicle
  as an item (compromise so survival can still pick vehicles up; one line to
  remove). Creative unchanged; gunfire/explosions still use vehicle health.
  Not covered: projectiles (arrows) fired by a player still trigger pickup.
  Verified by the unattended singleplayer test: survival punch leaves a
  hummer alive, sneak punch picks it up.

### Bottlecaps only work as a plain item in singleplayer, not as currency
- **Reported**: 27 Juli 2026
- **Status 7 Oktober 2026: FIXED.** `BottlecapHandler` in deciworldgen
  replicates `deci.aK.e`. Verified by the unattended singleplayer test: 5
  bottlecaps dropped on the player, balance 0 -> 5, no caps left as items.
  The HUD display of the balance has not been looked at by the user yet.
- **Symptom**: picking up bottlecaps keeps them as a literal `ItemStack` in
  the inventory, instead of auto-converting into the currency
  counter/balance the way it did on the old official multiplayer server.
- **Likely root cause** (not yet confirmed, pattern strongly suggests it):
  earlier investigation of `deci.aL.a`/`deci.aK.*` found an
  `EntityItemPickupEvent` handler that detects bottlecap items specifically
  (`k.alK`/`k.alL`), deducts the picked-up stack, and credits a player-cap
  currency field instead (shows a "+N bottlecaps" message). That handler is
  very likely one of the same ServerProxy-only `deci.aK.*` classes as every
  other singleplayer bug found this session - never registered on
  `ClientProxy`, so it just never fires in singleplayer, leaving caps as
  plain items. Needs confirming the exact class and applying the same
  registration fix already used for the loot handler (`deci.aK.k`) and the
  tick-queue drain.

## City edge ramp missed its outer columns (fixed v0.24.4, 8 Oktober 2026)
`LcCity.populate` only planned the cells under the population window, so a
window lying wholly outside the city never wrote the outer 1..2 columns of
the 10 wide edge band (a small step at the band's end). Also small and
large sites next to a city could sit inside the band and get cut under.
Fix: populate scans cells within EDGE of the window; sites within EDGE of a
city are dropped. Checked with tools/edgescan.py (seed 1).

## Converted buildings: LED lamp blocks as floor (reported 8 Oktober 2026, FIXED 9 Oktober 2026)
User saw some converted (DeceasedCraft) buildings with LED lamp blocks
used as floor.
- **Found with** `tools/lcaudit.py` (counts every source block of every
  converted building by placement: floor / ceiling / wall / free).
- **Cause**: not the simplylight lamps. tools/lctranslate.py's lamp rule
  matched the word "light" inside COLOUR names:
  `embellishcraft:light_gray_corrugated_metal_plate` (5429 uses, 1350 of
  them as floor), light_gray / light_blue sheet metal, tiles, seats and
  cushions all became deci:BlockLightOff. `minecraft:daylight_detector`
  (935, mostly floor trim) and `lightning_rod` matched too. Full
  simplylight illuminant blocks (about 5000) also became the small lamp
  prop, sitting in walls and ceilings as a floating fixture.
- **Fix** (lctranslate.py, pack rebuilt): `unlit()` removes light_gray /
  light_grey / light_blue / lightning / daylight before the lamp rules;
  full illuminant blocks -> minecraft:redstone_lamp (a solid lamp block);
  daylight_detector, redstone_lamp kept as themselves; rods -> iron_bars.
- **Verified**: worlddiff of seed 1 old pack vs new pack (`--top`):
  2115 BlockLightOff -> BlockMetal_2, 542 -> redstone_lamp, 111 ->
  daylight_detector. 30 interior photos (shots_index "lc_quality") show
  no lamp floors; ceilings show redstone lamps where the source had them.
  Not yet seen by the user in game `[not verified]`.

## NPC tracers fly sideways or backwards (old bug, also in the original; FIXED 9 Oktober 2026)
- **Reported**: 9 Oktober 2026: an NPC shooting forward draws its tracer to
  the left, right or behind.
- **Cause (code)**: BanditEntity.shootAt (`deci.ag.a.e`, also soldiers and
  hazmat soldiers) and ArmedTraderEntity.shootAt (`deci.ai.v.e`) damage the
  target directly on the server and send PacketGunFireEffects
  (`deci.aE.a$B`) with only the shooter's id; the client handler
  (`deci.aE.a$B$a`) draws the tracer along the shooter's getLook(), which
  for a mob is its BODY facing, not its aim.
- **Fix** (`tools/patches/PatchTracer.java`, in Decimation.jar.patched,
  dist/Decimation.jar, dev/libs/Decimation-base.jar): the packet also carries
  the target's id (read only when present; -1 = none), shootAt records its
  target, and the handler aims the tracer from the shooter's eyes at the
  target's chest (bounding box bottom + 0.6 x height). Players unchanged.
- **Measured** (`./gradlew runClient -Pautotest -Ptracer`: a bandit 8
  blocks from the player, held facing away, shoots the player): original
  classes 52 tracers, mean 133 degrees off the line to the target, max 179;
  patched 42 tracers, mean 1.0, max 2.0. (The test also showed the original
  shootAt crashes on a miss when the bandit has no AI target: it plays the
  miss sound at getAITarget(); never happens in play, the AI sets it.)

## Full military armor makes NPC gunfire almost harmless (reported 9 Oktober 2026, FIXED v0.30.2)
User: a full set of normal military armor takes about 1 hp per NPC shot,
even with a group of NPCs shooting at once.
- **NPC shots are not bullets** (BanditEntity.shootAt, read in deobf
  source): when the NPC can see its target, 75% of shots call
  `attackEntityFrom("human", gun damage / 8)` on the target directly, 25%
  only play a sound. The tracer (PacketGunFireEffects) is a visual only.
  Player shots are client ray traces sent as PacketGunHit; the server
  applies body armor and hits with "gunDeci".
- **Cause 1, armor multiplies piece by piece**: ArmorGunfireHandler
  (v0.9.0) uses Decimation's PvP formula, damage x every worn body piece's
  multiplier, and the multipliers are the originals x 0.65 (our armor
  buff). Marine set: vest 0.85, pants 0.8, boots 0.8 -> 0.5525 x 0.52 x
  0.52 = x 0.149 (checks mode: chest alone 5.52 of 10). An M4A4 NPC hit
  (16 / 8 = 2) does 0.30 hp; the helmet (0.195) on the 20% headshots
  makes that 0.06.
- **Cause 2, vanilla hit cooldown**: EntityLivingBase.attackEntityFrom
  (lines 854..870 of the MCP source) ignores a hit while hurtResistantTime
  is above half of maxHurtResistantTime (20 ticks for players) unless it
  is bigger than the last one. NPC shots never reset it (Decimation's
  player gun handler sets it to half after every hit, so player shots all
  count). So for 0.5 s after a hit every other NPC hit of the same size is
  dropped: ten NPCs do about as much as one, at most ~2 hits per second.
- **Fix (v0.30.2, user decision)**: player shots unchanged; NPC gun hits
  ("human") on a player x `npcDamageToPlayer` (default 5, user: "hardcore
  singleplayer"), after armor; and NPC hits clear the victim's hit
  cooldown first (LivingAttackEvent), so every hit of a group lands
  (`everyNpcHitCounts`). Both in config/deciworldgen_npc.cfg, category
  "player"; code NpcLoadouts.onAttack / onHurt.
- **Numbers (checks mode)**: an NPC M4A4 hit (2) on a bare player 10.00 hp
  (half the bar), full marine body set 1.49 (was 0.30), 5 hits in one
  tick 7.47 (all land; before only the first). Helmet headshots (20%)
  cut a hit further. Armed traders also use "human" at full gun damage
  but only ever target infected and bandits, never players.
- Not seen in game by the user yet `[not verified]`.
