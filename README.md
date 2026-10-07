# decimation-singleplayer

Fixes and new content for **Decimation** (BoehMod, Minecraft Forge 1.7.10),
a zombie survival mod whose official servers are long gone. The goal is to
make it a complete singleplayer game.

> This repository contains **only our own work**. It does not contain
> Decimation itself, its code or its assets. You need your own copy of the
> mod (`Decimation.jar`, version 1.21.10f, e.g. from the Technic modpack).

## What is in here

- **`dev/`**: our companion mod `deciworldgen` (Java, RetroFuturaGradle):
  - world generation: procedural ruined city blocks (apartments, offices,
    shops up to 20 floors, real floor plans, biome overgrowth), street grid
    with sidewalks and parked wrecks, small and large `.schematic` ruins,
    Decimation zones on generated sites;
  - singleplayer fixes for logic that only ran on the official dedicated
    server: zones, bottlecap pickup, supply drops, humanity, armor against
    NPC gunfire (with headshot-only helmets), vehicles vanishing on a punch,
    invisible multiblock props.
- **`tools/patches/`**: Javassist patches applied to your own Decimation jar
  (CPU-burning busy loop, props culled while in view).
- **`tools/`**: test tools that verify world generation without playing
  (dedicated server runs, region file readers, wall scanner).
- **`docs/`**, `bug.md`, `new_feature.md`, `CLAUDE.md`: how everything works,
  every bug found with its root cause, and the project rules.
- **`deobf/`**: tooling to give Decimation's obfuscated classes readable
  names locally (the decompiled output itself is never committed).

## Building

```
cd dev
./gradlew setupDecompWorkspace     # first time
./gradlew build                    # -> dev/build/libs/deciworldgen-<version>.jar
```

`dev/libs/Decimation-base.jar` must be your own Decimation jar (with the
patches from `tools/patches/` applied) without our `net/decimation/worldgen`
and `net/decimation/fixes` classes; see `CLAUDE.md`.

## Installing

Put `deciworldgen-<version>.jar` next to Decimation in your instance's
`mods/` folder, copy the schematics from `structures/` into
`config/decimation_worldgen/` (large ones into `large/`), and create a new
world.

Personal, non-commercial fan project. Decimation belongs to its authors.
