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
  fails silently now. Harmless, just noisy in logs.
- **Vehicles**: rideable car/vehicle entities (`deci.ad.c`) with horn, headlights,
  passenger seats. No lootable trunk feature exists in this build - vehicles are
  ride-only.
- **Custom loot system**: crates, ammo boxes, supply drops, wrecked-car props, etc.
  give randomized loot on right-click, server-authoritative (see below).

## How the loot table works

Three moving parts, all server-side logic (this is exactly why singleplayer never
worked before patching - see Session Log):

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

## Bug and feature tracking

Dated history moved out of this file - see `bug.md` (fixed/open/pending bugs)
and `new_feature.md` (added/planned features) going forward. This file stays
the technical reference (how the systems work, current config/balance values,
known non-bugs) rather than a changelog.
