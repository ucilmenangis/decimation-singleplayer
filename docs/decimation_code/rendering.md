# Decimation code: rendering and models

Part of the feature study of Decimation's own code (16 Oktober 2026, index docs/decimation_code/README.md). The gun
model and animation formats and the gun renderer in detail: docs/gun_model_spec.md; prop drawing: docs/prop_placement.md;
measured costs and fixes: docs/performance.md.

## 1. What it is for

Everything Decimation draws beyond vanilla: guns in first and third person with animations and attachments, the
players' stances, gear and gestures, 96 prop renderers, mob and vehicle renderers, particles, and per frame world
layers (tracers, laser dots, loot hints, turf names, icons, debug boxes).

## 2. Code map

| Class (obf) | Job |
|---|---|
| BModelLoader (deci/n/g), BModel (deci/n/f), BModelPart (deci/n/b), BModelQuad (deci/n/c) | the plain text .bmodel format (docs/gun_model_spec.md) |
| GunItemRenderer (deci/K/b) | IItemRenderer of every gun: first person, third person (`renderEquippedThirdPerson`), attachments (`renderAttachments`), muzzle flash, smoke puffs; holds the playing GunAnimation |
| GunAnimation (deci/F/a) | .anib animations; frames can send reload steps |
| PlayerModelSwapper (deci/J/e) | swaps the player model each RenderPlayerEvent.Pre (`applyStancePose`): stances, gestures, extra armor |
| StanceEntityRenderer (deci/E/a) | the camera height while crouched or prone (swapped in by ClientRenderHandler) |
| ClientRenderHandler (deci/c/b) | render tick work (`onRenderTickStart`, `onRenderTick`), name tags; the old picture in picture scope (replaced by our ScopeZoom) |
| WorldRenderLayerDispatcher (deci/c/d) | runs the 13 world layers (deci/M): each layer's `render` once a frame and its `renderEntity` once per loaded entity per frame |
| layers (deci/M/c to o) | GrenadeTimerLayer, LaserDotLayer, BlockTipLayer, BulletTracerLayer, NpcIconLayer, ClanTurfLayer, WorldIconLayer, VehicleSpawnLayer, and the debug or empty ones (VehiclePartDebugLayer, VehicleDebugLayer, ZoneDebugLayer, EmptyRenderLayer, UnusedModelLayer) |
| prop renderers (deci/I), models (deci/p, q, r, s, t, u, w) | one TESR per prop tile entity with a Techne model; mobs, vehicles, items |
| DeciParticleSpawner (deci/B/d) | custom particles by name (blood, smoke, embers, flashes), also sent from the server |
| ModelHuman (deci/q/h), HumanRenderer (deci/J/k, a RenderBiped) | every human NPC and infected: `renderHuman` draws the body picture with `renderHead`, `renderBodyAndArms`, `renderLegs`, then eyes, hair, beard on copy Fa and shirt, pants, shoes on copy Fb (both 0.05 larger, posed again by their own `renderHuman`), then armour, mask, vest, backpack |

## 3. As it runs (dev test `gunview` with an AKM, call logger, 16 Oktober 2026)

Traced `deci.M,deci.c.d,deci.K.b,deci.F.a,deci.J.e,deci.c.b` over 88 rendered frames: every one of the 13 layers'
`renderEntity` 4447 times (88 frames x about 50 entities) and `render` 88 times; ClientRenderHandler.onRenderTickStart
264; PlayerModelSwapper.applyStancePose 132; GunItemRenderer.handleRenderType 128 and tickSmokePuffs 99;
GunAnimation.get 99 (a lookup each frame while the gun is held).

## 4. Known costs and faults

- The layers run per loaded entity per frame, the empty and debug ones too: in a city with hundreds of entities that
  is hundreds of thousands of mostly empty calls a second [cost not profiled]; skipping the empty layers would be a
  cheap patch.
- Fixed by us before: the picture in picture scope rendered the world twice (ScopeZoom, PatchScope), the sway
  thread spun a core (PatchSwing), props drew at any distance and without line of sight caching (PatchPropCulling),
  the scope glass measured NPC guns (ScopeZoom, v0.43.1). Details: docs/performance.md.
- Never run fps tests in a game started with the call logger (docs/call_trace.md).
- ModelHuman drew each NPC in seven texture layers (19 part draws, 7 binds) and called ClientProxy$LineOfSight
  `canSeeEntity` (up to nine ray traces) three times a frame for `markVisible`, whose timer nothing reads
  (`isRecentlyVisible` has no caller); RenderBiped poses only the main model for a held item, so the copies drew sleeves
  in the empty hand pose. Fixed by fixes/HumanLayers and PatchHumanLayers (bug.md "Drawing many human NPCs"). Still
  costly: an armed NPC's gun and gear, part by part (about 360 us of 423 an NPC, dev test npcdraw).
- Glows turn item lighting off and never back on (ClientEmptyB.disableItemLighting: a flashing gun, a firing player's
  body, mech lamps, the Gawker, an NPC's night vision goggles): every entity drawn after them lost its shading (the
  "flash on every body"). fixes/EntityLighting puts it back around each living entity, PatchLightLeaks fixes the
  goggles (bug.md "Muzzle flash lit every mob").
- With OptiFine, RenderHandEvent is posted inside renderHand (Forge posts it before): our ScopeZoom, which cancels it and
  draws the hand itself, recursed until the stack ran out (gun gone while aiming, "Stack overflow" every frame); it now
  ignores its own re-entry (bug.md "Aiming with OptiFine").
- Every quad of a .bmodel part was its own draw call inside the part's two display lists (BModelQuad: startDrawingQuads,
  four vertices, draw): a few hundred draws per gun. fixes/ModelBatch with PatchModelBatch draws each part in one draw
  (gunbatch: the same pixels; npcdraw: armed NPCs about half the time).
- Everything is made at its first draw (bug.md "First sight of armed NPCs stalls a frame", 20 Oktober 2026): a part's
  display lists (BModelPart.compileDisplayList, at the scale BModel passes, 1/16), every texture (TextureManager at its
  first bind), and gear models: ModelHuman.renderHuman loads a mask's, vest's or backpack's model into the item
  (BModelLoader.load of models/armor/mask|vest/<modelName>.bmodel, models/backpacks/<modelName>.bmodel) only when an NPC
  wearing it is first drawn, and a full armour piece's into a DeciPlayerModel (models/armor/full/<modelName>.bmodel), whose
  first draw on a human hangs the armour parts on its limb parts (addChildPart) and compiles those. A refuge's residents
  coming into view at once: 941 ms in the production client. fixes/ModelWarmup now loads the gear models and the model
  textures ahead (`Deci.loadGearModel`, `Deci.humanModel`, `Deci.decimationJar`).
- The display lists are heavy: about 9 KB of driver memory a quad on the M4 [inferred: compiling all 25,628 parts of all
  models raised the graphics memory from 1.2 to 2.6 GB]. ModelBatch keeps Decimation's two lists beside its own.

## 5. How our mod uses it

| Need | Use |
|---|---|
| our guns and sights drawn like Decimation's | tools/guns (gunmodel.py), fixes/NewGuns, fixes/NewSights, fixes/SightPlacement, fixes/SightReticle, `Deci.offsetAttachment` (skill decimation-gun) |
| every gun drawn into the view its own way, its rack at the end | fixes/GunDraw (wraps each gun's item renderer; dev test `gundraw`) |
| scopes | fixes/ScopeZoom |
| prop render distance and culling | tools/patches/PatchPropCulling.java, config deciworldgen_props.cfg |
| NPC tracers | fixes/NpcShots, PatchTracer |
| NPC looks in one picture | fixes/HumanLayers (`Deci.humanLook`, `Deci.humanGear`), tools/patches/PatchHumanLayers.java |
| .bmodel parts in one draw each | fixes/ModelBatch, tools/patches/PatchModelBatch.java |
| blood that stays on hit infected (splats painted into the NPC's own look picture) | fixes/Wounds (via fixes/HumanLayers.begin / bind); dev test `wounds` |
| textures and gear models before their first draw | fixes/ModelWarmup (`Deci.loadGearModel`) |

## 5b. Player skins and corpses (read 22 Oktober 2026 for fixes/PlayerSkins)

- Players are drawn by vanilla's RenderPlayer; Decimation only swaps its model (PlayerModelSwapper on
  RenderPlayerEvent.Pre), so the texture is AbstractClientPlayer.getLocationSkin(): the skin manager's download, or
  Steve while locationSkin is null (func_152123_o false). func_152121_a(SKIN, location) sets it (the skin manager's own
  callback, public).
- Decimation's menu preview player (MenuPreviewPlayer deci/e/a, DecimationMod.fakePlayerProfile.getPlayer: the HUD
  stats box with fancy graphics, the profile page) is an AbstractClientPlayer in a world of its own.
- Corpses (EntityCorpse deci/ae/a, owner id from DeathHandler: the dead player's server GameProfile id, sent in the spawn
  data; getOwnerUUID ef) are drawn by CorpseRenderer: it binds "skins/<owner id>", which RenderUtil.loadSkin fills from
  crafatar.com by id with Steve as the fallback, only when nothing is registered under that name yet. Before 5b every
  corpse of an offline id, and of the dev account, showed Steve.
- The gun arms (GunItemRenderer) use fixes/GunArmSkin, which follows getLocationSkin().
- Ours (feature 5b): fixes/PlayerSkins gives skinless players one of Decimation's 21 human skins (by name) and puts an
  alias texture under each seen player's corpse names; never deleteTexture such a name (it frees the player's skin).

## 6. Checks

- `gunview` (guns in hand, aimed, on an NPC), `scope`, `props` (prop fps), `cityfps` / `cityview` (fps: never with the
  call logger), `views` (street shots), `npclook` (NPC looks drawn both ways in one frame, pixels compared), `npcdraw`
  (time drawing NPCs a frame, both ways), `lightleak` (lighting as each mob starts drawing, after flashes and goggles), `gunbatch` (parts drawn both ways, pixels compared).
