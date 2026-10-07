# Decimation world type (terrain, 0.14.0)

Own world type "Decimation" (create world screen: More World Options, World
Type; `level-type=decimation` in server.properties). Decided with the user
on 7 Oktober 2026: mostly flat rolling land, rivers and lakes but no ocean,
dead near cities and reclaimed by nature far away, one temperate climate.
RTG and vanilla worlds keep working as before; this is an extra option.

Code: `dev/src/main/java/net/decimation/worldgen/terrain/`.

## How it works

- The terrain itself is vanilla's `ChunkProviderGenerate` (so caves, ores,
  dungeons, mineshafts, strongholds stay). We only replace the BIOME map:
  `TerrainEvents.onInitBiomeGens` (Forge `WorldTypeEvent.InitBiomeGens`,
  `TERRAIN_GEN_BUS`) swaps vanilla's whole GenLayer stack for two
  `DeciGenLayer`s (1:4 for the terrain generator, 1:1 per block) that read
  one `BiomeMap`. Terrain height follows each biome's `rootHeight` /
  `heightVariation`; vanilla blends heights across biome borders.
- `BiomeMap.biomeAt(x, z)` uses the seed only:
  - CITY sector: Decimated City; MIL sector: Irradiated Military Zone;
    exactly on the sector squares (`Sectors`, shared with the structure
    generator, so cities always sit on flat urban ground);
  - CIV sector: Decimated Suburbs, edge warped by noise up to 56 blocks;
  - rivers: a noise contour (`|simplex| < 0.022` at scale 520, domain
    warped), kept 32+ blocks from city and military sectors;
  - wilderness within about 100 blocks (+-48 noise) of a city or military
    sector: Decimated Plains or Burnt Forest; further out: Overgrown Plains,
    Overgrown Forest (vine covered trees), Overgrown Hills.
- No villages (our biomes are not in vanilla's village list).

## Biomes (ids are fixed, saved in every chunk)

| id | name | height (root, var) | look |
|----|------|--------------------|------|
| 110 | Decimated City | 0.1, 0.01 | grey olive grass, grey sky, rare dead trees |
| 111 | Decimated Suburbs | 0.125, 0.04 | faded grass, half dead / half live trees |
| 112 | Irradiated Military Zone | 0.1, 0.02 | brown grass, gravel and coarse dirt patches |
| 113 | Decimated Plains | 0.125, 0.06 | dry yellow brown grass, dead bushes, dead trees |
| 114 | Burnt Forest | 0.15, 0.12 | dark grass, many dead trees |
| 115 | Overgrown Plains | 0.125, 0.05 | lush green, tall grass, flowers |
| 116 | Overgrown Forest | 0.1, 0.2 | dense oaks with vines |
| 117 | Overgrown Hills | 0.45, 0.3 | rolling hills, trees |
| 118 | Murky River | -0.5, 0.0 | river, murky water colour |

Names carry the keywords Decimation's `AmbientMusicPlayer` looks for
("forest", "river", "plains", "hills", "decimated", "irrated"); a name
without one asks for a sound that does not exist.

`DeadTree`: 4 to 8 block leafless trunk (oak or spruce bark) with 1 to 3
short branches, sometimes a 2 to 3 block stump.

## Traps found while building it

- Spawns: Decimation adds its infected / NPC spawns to every biome existing
  in ITS preInit (before ours) and strips vanilla monsters in its init
  (after our preInit). So our biomes are created in our preInit and
  `DecimationBiomes.copySpawns()` copies plains' lists in our postInit
  (animals left out in the city and the military zone). Autotest checks it:
  "Decimation monster spawn entries 5".
- Ravines cut 40 block trenches through flat cities and the street painter
  laid sidewalks at their bottom. `SealedCaves` (via `InitMapGenEvent`,
  only replacing the exact vanilla classes) digs nothing above y 50 under
  city and military biomes. After: city ground 63+, worst lot edge 6.
- Lakes: none in city / military (buildings cancel on water), 1 in 4 in the
  other dead biomes, vanilla rate in overgrown ones; no surface lava pools.
- Spawn search: suburb, wasteland, overgrown plains and forest are added to
  `WorldChunkManager.allowedBiomes`.

## Not done yet

- Fog colour (the sky colour is per biome; fog in 1.7.10 comes from the
  world provider). `[not verified]` whether the haze needs it.
- Snow is never generated (temperature 0.7 everywhere), by design.
- Biome ids 110..118 are fixed; a mod using the same ids would clash (a
  warning is logged).

## Testing

- `python3 tools/servertest.py 1 pregen=0,0,24 type=decimation`
- `python3 tools/worldmap.py dev/run/server/world build/map.png`: top down
  biome map with hillshade and water, plus a biome share / height table.
  Seed 1, 800x800 around spawn: city 27%, dead wild 26%, suburbs 15%,
  military 9%, overgrown 21%, river 1%.
- `./gradlew runClient -Pautotest` now creates the autotest world with the
  Decimation world type (`-Ddeciworldgen.autotest.type=default` for vanilla).
- In game, user screenshot 7 Oktober 2026: olive dead grass, grey sky, flat
  city with streets and parked wrecks, greener trees on the horizon.
