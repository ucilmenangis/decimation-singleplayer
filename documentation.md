# Decimation Mod (1.7.10) - Singleplayer Loot Fix Documentation

Abandonware mod, official server (mcdecimation.net) dead permanently. Personal-use
patch only, jar obtained legally via Technic modpack. Not for redistribution.

## What makes this mod different

Not a plain "zombie survival" mod - built around a persistent server-authoritative
game layer on top of vanilla Forge 1.7.10:

- **Clans/turf system**: players form clans, claim land ("turf"), get block
  break/place protection inside claimed territory (`net.decimation.mod.server.clans`,
  `net.decimation.mod.server.turf`).
- **Safezones/zones**: PvP-safe areas, radiation zones, military/police spawn zones,
  scripted via `net.decimation.mod.server.zones`.
- **Central network service**: the mod talked to `network.mcdecimation.net` over a
  custom TCP protocol (Kryonet) for stats tracking (kills, deaths, supporter status,
  clan bounties). That service is dead - the mod tries to reconnect forever and just
  fails silently now. Mostly harmless, noisy in logs; since v0.28.8 (PatchBackend) the
  launch no longer waits 5 s for it. Still open (bug.md): the main menu shows an offline
  banner and "Play offline", and the kill / death counters read the dead service (0).
- **Vehicles**: rideable car/vehicle entities (`deci.ad.c`) with horn, headlights,
  passenger seats. No lootable trunk feature exists in this build - vehicles are
  ride-only.
- **Custom loot system**: crates, ammo boxes, supply drops, wrecked-car props, etc.
  give randomized loot on right-click, server-authoritative (see below).

## How the loot table works

Three moving parts, all server-side logic (this is exactly why singleplayer never
worked before patching - see bug.md):

1. **Loot pool definition** - `deci.aD.l` (main class). Its `gh()` method builds a
   `Block -> ItemPool` map at startup: each entry is `(Block, weight, List<ItemStack>)`.
   This is **hardcoded Java, compiled into the jar** - not read from any external
   config file. There is no loot-table YAML/JSON shipped or loaded; the item pools
   themselves can only be changed by editing the compiled bytecode (like the ammo
   crate fix below), not by config.
2. **Per-position cooldown registry** - `deci.aB.e`, keyed by exact world dimension +
   block coordinates. Tracks "when was this specific crate/box/drop last looted" so
   the same container can respawn loot after a timer, independent of block type.
3. **Interact handler** - `deci.aK.k`, a `PlayerInteractEvent` (right-click-block)
   listener. On click: checks the position registry (2), looks up the loot pool for
   that block type (1), rolls items into a temp inventory, marks the position looted,
   plays a sound, sends the rolled inventory to the client over the network, and
   opens a GUI container after a short delay.

Vanilla chest/trapped-chest GUI is deliberately suppressed for any `BlockChest`-type
block so the mod's own loot GUI takes over instead - but only regular `Chest` has an
entry in the loot pool map. Trapped chest was never added by the original devs, so
right-clicking one does nothing (not a bug, just never wired in).

## Configuring loot

Only the **respawn cooldown** is externally configurable, via a plain Java
`.properties` file (not YAML, despite that being the original assumption) -
`decimation_server.properties`, created next to `mods/` (i.e. in the instance's
`minecraft/` root) on first launch if it doesn't exist yet.

```properties
lootRespawnTime=900
```

Value is in **seconds**. Default (compiled-in, used if the file/key is absent) is
`900` = 15 minutes. The old official server likely ran a custom value (~1800 = 30 min
based on memory) - not recoverable from the jar since that config lived on the dead
server, not in the client jar. Set whatever value you want, e.g. `1800` for 30 min.

Item pool **contents** (which items a crate can roll) are not config-editable at all
in this build - only fixable by patching the compiled classes directly.

## Known non-bugs (tested, working as originally designed)

- Trapped chest not lootable - never in the loot table to begin with.
- Truck wreckage props (`truckwreckage1`-`6`) not lootable - decorative only, no
  loot pool assigned, unlike the plain `wreckage1`-`5` / military / police wreckage
  variants which are.
- No car/vehicle trunk loot - vehicles are ride-only entities, no inventory feature
  exists in this build's code.

## Intro screens (jumpscare + BoehMod logo)

On every launch, `deci.c.c`'s `GuiOpenEvent` handler replaces the vanilla main menu
with a chain of custom screens, gated by two static boolean flags that start `false`
and only flip to `true` once each screen finishes playing naturally:

- `deci.i.d.iH` - gates the 390-frame animated "jumpscare" intro
  (`textures/gui/intro/frame0..389.png`, sound `deci:gui.intro.decimation`).
- `deci.i.c.io` - gates the BoehMod studio logo intro (`boehmod_big.png`, mortar
  fire/explosion sound cues).

Patched both flags' default value to `true` directly in their class initializers -
classes, textures, and sounds are all still in the jar untouched, they're just
never instantiated since the check `if (!iH)` / `if (!io)` is now always false from
boot. Falls straight through to the mod's own (re-skinned) main menu after that.

## Balance changes

- **Ranged weapon damage**: flat 50% cut across every registered gun, including
  crossbow (no exceptions). Single chokepoint patch - `deci.ay.i.am(int)`, the
  damage-per-hit setter every one of the ~90 registered weapons (rifle/smg/mg/
  shotgun/pistol/revolver/rocket/crossbow) funnels through via its `.am(N)` builder
  call at registration. Now does `Math.round(n * 0.5f)` instead of storing `n`
  directly.
- **Armor damage reduction**: scaled `ItemArmorDeci.damageMultiplier` by `x0.65` at
  construction (35% more reduction, proportional to each piece's original value) -
  patched both constructors' field-write in `net.decimation.mod.common.item.armor.
  ItemArmorDeci`.
  - **Important caveat found while patching**: `damageMultiplier` is only ever read
    in one place in the whole mod - the ranged-hit network handler
    (`deci.aE.a$z$a`). It does **not** affect melee damage at all (melee goes
    through vanilla's plain armor-point system, and every Decimation armor piece
    is registered under vanilla `ArmorMaterial.CHAIN` regardless of its custom
    multiplier). That same handler also skips armor slot index 3 (helmet) when
    applying the multiplier, so head armor currently does nothing for ranged
    damage reduction either. Scope of this buff: ranged only, chest/leg/boot only.
    Melee armor and helmet-affecting-ranged would both need separate new logic,
    not just a number tweak - left alone for now per user's call to keep this
    scoped.
  - Update (v0.9.0 / v0.9.1, our deciworldgen jar, `ArmorGunfireHandler`): armor now
    also reduces NPC gunfire, and helmets count, on headshots only (the aim line for
    player guns, 20 % of NPC shots). Melee is still vanilla armor points.

- **NPC tiers (v0.30.0, our deciworldgen jar)**: bandits, soldiers and
  Soviets get a tier when they spawn (gear, gun, health, fire rate, share
  of player gun damage they take); Soviets are the enemy military and also
  spawn in groups in military sectors. Values and how to change them:
  `config/deciworldgen_npc.cfg`, details in new_feature.md "Step 1 design:
  NPC tiers". NPC guns now look the same on the client as on the server.
- **NPC gunfire on the player (v0.30.2, hardcore)**: x5 after armor
  (`npcDamageToPlayer` in deciworldgen_npc.cfg, 1 = Decimation), and every
  NPC hit lands (vanilla's 0.5 s hit cooldown skipped for NPC shots). An
  NPC rifle hit: 10 hp bare, about 1.5 hp in a full marine body set.
  Player shots unchanged. v0.30.3: vanilla's hit cooldown is kept again
  (`npcHitsSkipCooldown` false), NPC shots are traced bullets (spread per
  tier, stopped by walls, impact particles, tracer = the real shot).
  v0.30.4: hit cooldown 0.25 s (`npcHitCooldownTicks` 5); RPG bandits
  (RPG-7) and RPG military (RPG-18) fire real rockets. v0.31.0: juggernaut
  (200 hp, takes 25%, machine guns or an armor piercing Barrett), only with
  military groups (`juggernautChance` 0.15) and its egg. v0.32.0: elite
  military (`eliteChance` 0.2, 150 hp, takes 16%, x2 damage); NPCs fire
  bursts with recoil, empty their real magazine, reload 4 s (category
  `npc_fire`). v0.32.1: sniper versions (juggernaut_sniper Barrett,
  elite_sniper), `sniperShare` 0.5. v0.32.2: snipers spot enemies up to
  `sniperRange` 96 blocks (others 32).
- **No vanilla mobs (v0.33.0)**: vanilla monsters, animals, squid, bats,
  villagers and golems are gone from the overworld (spawn lists and a join
  filter); Decimation's own mobs and the Nether untouched. Config
  `deciworldgen_mobs.cfg` (`removeVanillaMobs`, `keep`).
- **Zombie variants (v0.34.0)**: runner (12%, fast, 14 hp), riot (8%, 30
  hp, takes 50%), screamer (5%, its scream sends infected within 32 blocks
  after its target), night frenzy (all infected x1.2 speed, +2 attack at
  night). Config `deciworldgen_zombies.cfg`; eggs "Spawn Infected (...)".
- **60 round magazines (v0.35.0)**: 60rnd NATO STANAG (M16 family, M4A4,
  ACR, L85A1, SCAR-L) and 60rnd AK (AK-74 family), found in the same loot
  as the 30 round mags.
- **NPC burst rate (v0.39.2)**: NPC automatic fire is capped at 600 rounds a minute
  (`maxBurstRpm` in deciworldgen_npc.cfg, category npc_fire); a gun slower than that
  keeps its own rate. Before, machine guns (PKM, M240, MG3) fired every tick.
- **Guns of our own (v0.36 to v0.41, deciworldgen items)**: MAC-10 (smg, 1100 rpm,
  own 30 round mag), UMP9 (smg, 650 rpm, own curved 9 mm mag), HK416 and HK416 Tan
  (rifle, 800 rpm, STANAG and 60 round mags), Mk18 Mod 1 (rifle, 800 rpm, STANAG and
  60 round mags). Registered damage 12 / 11 / 16 / 15, halved like every gun by the
  global patch above (6 / 6 / 8 / 8 per hit). They drop wherever their model gun drops
  (MAC-10 with the Uzi, UMP9 with the UMP45, HK416 and Mk18 with the M4A4).
- **Sights of our own (v0.41)**: EOTech 558 (1.25x, red ring and dot) and ACOG TA11
  (3.5x, BDC reticle); the reticle is drawn in the sight's glass and sways with the gun.
  Iron sights fold away while any sight is attached (v0.40.0).

## Settings of our jar (config/)

- `deciworldgen_scope.cfg`: `pictureInPicture` (false = the cheap zoom scope; true =
  Decimation's own, about half the fps), `sensitivity` (mouse slowdown while zoomed),
  `overlayFrom` (scopes from this magnification show the black sniper overlay, 4).
- `deciworldgen_props.cfg`: prop render distance by size: small 24, medium 32, large 48
  blocks (64 = vanilla; city views lost about 18 % of the frame to props before).
- `deciworldgen_npc.cfg`, `deciworldgen_mobs.cfg`, `deciworldgen_zombies.cfg`: see the
  balance list above.
- `decimation_worldgen/`: our world generation data (city pack, schematics, furniture
  sets, palettes); docs/worldgen.md and docs/city_engine.md.

## Military bases (v0.42, our world generation)

Military sectors of the "Decimation" world type get US FOB style bases: a combat outpost,
a FOB or a large FOB (HESCO walls, towers, gate with a serpentine, TOC, B-huts, ammunition
point, motor pool, tents, helipad). Loot is Decimation's own crates (its loot tables):
military crates, ammo cases, weapon cabinets, medical crates, footlockers, care packages.
The TOC door is locked: a military keycard on the screen beside it opens it (or a
lockpick); military crates, military wrecks and care packages can drop that keycard.
Details: docs/military_base.md.

## Bug and feature tracking

Dated history moved out of this file - see `bug.md` (fixed/open/pending bugs)
and `new_feature.md` (added/planned features) going forward. This file stays
the technical reference (how the systems work, current config/balance values,
known non-bugs) rather than a changelog.
