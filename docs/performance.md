# Performance: how to check it, what we found (10 Oktober 2026)

User report: "really big performance issue now ... 1 fps after i drop it [a gun] to the
ground ... check other variables that maybe can make the performance degrade". Measured, not
guessed; every number below comes from the tools in section 1.

## 1. Tools

| Tool | What it tells |
|---|---|
| `python3 tools/devtest.py --live gunperf [guns=uzi,ump45,mac10,ump9] [attach=...]` | fps per gun: nothing in view (baseline), the gun dropped 2.5 blocks in front of the camera (EntityItem, IItemRenderer ENTITY), held; Decimation guns are the control; uncapped frame rate |
| `python3 tools/devtest.py --live census [span=60] [kill=Infected,Hulk,...]` | loaded entities by registry name, tile entities, loaded chunks, fps every 10 s, start vs end (growth check: PASS under 100 in the span); `kill=` removes those entities first (cleaning a test world) |
| `python3 tools/perfcheck.py world WORLD` | offline scan of the save: entities by id, most crowded chunks, chunks in the wrong region slot, entities stored outside their chunk, tile entities by id |
| `python3 tools/perfcheck.py log LOG` | most repeated log lines (a stack trace printed every tick is a frame killer) |
| `python3 tools/perfcheck.py fixchunk WORLD CX CZ` | clears one chunk's region slot (regenerated from the seed); copy the world first |
| JFR on the live game | `~/.jdks/zulu-8.jdk/Contents/Home/bin/jcmd <pid> JFR.start name=x settings=profile duration=30s filename=F.jfr`, then `~/.jdks/zulu-25.jdk/Contents/Home/bin/jfr print --events jdk.ExecutionSample --json F.jfr` and count top frames / callers per thread ("Client thread", "Server thread"). Default stack depth 64: deep recursion (pathfinding) cuts the bottom of the stacks |

## 2. What it was (dev world, deciworldgen_autotest)

1. **Entity explosion through a corrupt chunk** (the 1 fps). Region slot (-1,-6) held the data
   of chunk (-4,-1) (xPos / zPos of the other chunk). The game then had two chunk objects
   claiming (-4,-1); entities of both were saved into (-4,-1) and came back as new entities on
   every load, while Forge printed "Wrong location!" plus a full stack trace for each of them
   every tick (43 000 traces in one session). The save grew from about 9 800 entities to
   40 730 (31 010 Hulks, 8 263 Infected, 1 252 Bloaters) in one 15 minute live session; 32 965
   of them stored in (-4,-1) with positions outside it. Five server crashes on 10-09 04:12
   (AnvilChunkLoader.loadEntities -> addTileEntity, index -20: a tile entity outside its chunk)
   show the slot was already broken then; what broke it is not known `[not verified]` (a kill
   during a save is the usual cause).
   Fix: `perfcheck fixchunk` on (-1,-6) and (-4,-1) (backup first), `census kill=...` removed
   the remaining 8 121 piled up infected; afterwards about 200 entities, no growth in 60 s, no
   "Wrong location" lines.
2. **The ground under the test arena is a mob farm**: the 97 x 49 stone floor at y 150 keeps the
   ground below permanently dark, next to spawn (always loaded); infected spawn there and walk
   around under the player (about 50 infected and 15 infected dogs come back within a minute).
   Harmless now that they are cheap (item 3), but they are why a dev world always has infected
   near the player.
3. **Decimation's infected path search every tick** (real game too): InfectedEntity
   .onLivingUpdate (deci.ag.d) called updateWanderPath (a full path search) every tick for every
   infected on the server AND on the client copies, and scanned a 40 x 40 box for other infected
   every tick (n x n). JFR: about 60 % of the client thread and of the server thread in
   pathfinding; the client even searched the server's chunks. Removing the ~90 infected near the
   arena took the fps from 21 to 51. Fixed by tools/patches/PatchInfectedAI.java (v0.38.1):
   wander search server side once a second per infected (staggered by id), horde scan every 10
   ticks with the result kept. With the same 53 infected alive: baseline fps 21 -> 35, client
   pathfinding 60 % -> 0 %, server 60 % -> 20 %; the zombies dev test still passes (variants,
   runner speed, riot armour, screamer, night frenzy).
4. **Guns are not the cause**: gunperf in the clean world, fps dropped / held vs baseline: Uzi
   22 / 19, UMP45 23 / 18, MAC-10 22 / 20, UMP9 22 / 20 (baseline 21; after the patch baseline
   35, Uzi 34 / 34, MAC-10 35 / 35). Our guns cost what Decimation's cost.

## 3. Other things that cost frame time (checked, not changed)

- 10 694 PropTile tile entities in the dev world: they do NOT tick (TileEntityProp extends
  StaticTileEntity deci.Z.a, canUpdate() false; an earlier note here guessed otherwise, checked
  10 Oktober 2026). Their cost is drawing only (section 5).
- 784 loaded chunks in the dev world (spawn chunks + view distance): vanilla random block ticks
  (WorldServer.func_147456_g) are now the top server item.
- Prop drawing in prop heavy views: bug.md "FPS drop in prop-dense areas" (open, item 1 of the
  roadmap's open bugs).

## 4. Checklist when the user reports lag

1. `perfcheck log` on the session log: anything printed thousands of times?
2. `perfcheck world` on the save: entity totals, a chunk with hundreds of entities, chunks in
   the wrong slot, entities outside their chunk.
3. `census` in the live game: growth over 60 s.
4. JFR 30 s on the live game: top frames and callers per thread.
5. `gunperf` (or a mode for the suspected item) against Decimation's own as the control.
6. Fix, then measure the same way again; write the numbers here.

## 5. Props (user, 10 Oktober 2026: "the performance really fuck up when props so many")

Measured with dev test props (225 props packed in front of the camera; now with clear weather
every run: rain particles cost frame time and made runs differ) and JFR:
- Java side is small: PropRenderer (deci.I.l) 1.4 % of client samples. The client thread
  waits in the driver: native samples 70 % in glCallLists (terrain lists, where the driver
  stalls on a busy GPU) and 12 % in the props' own part display lists (BModelPart deci.n.b).
- Isolation in one session (fps empty platform -> 225 props in view): Decimation's renderer
  48 -> 29 and 31 -> 24; transforms and texture bind without the model 33 -> 33. The cost is
  drawing the models (geometry through the props' display lists), not the per prop setup,
  not the line of sight checks, not ticking.
- Tried and dropped: one flat display list per prop type (BModel parts recorded in immediate
  mode): slower, 26 / 23 -> 16 / 17 fps in A/B runs (two each). Per draw call overhead is not
  the bottleneck on this Mac; removed again (not shipped).
- Earlier (10-09): prop render distance 32 / 48 in a seed 1 city street gave no gain, props were
  5.5 % of that frame. So props hurt in dense spots (packed props, tower chunks with 500+),
  not in an ordinary street.
- The fps of the empty platform falls from run to run in one live session (48 -> about 33)
  with steady entity counts: probably the machine heating up after GPU heavy runs
  `[inferred]`; compare only within one run.
Open: which places the user sees lag in (screenshot plus F3 fps), then measure there.
Options: render distance by size there (deciworldgen_props.cfg, already in place), a
lower detail model for far props, or baking props into chunk meshes (big).

