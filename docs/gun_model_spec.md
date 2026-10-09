# Gun model and animation spec (Decimation 1.21.10f)

How a gun's 3D model, textures, animations and sounds are found, parsed and
drawn. Written 2026-10-07 from the decompiled code (`deobf/src`) and the 98
shipped guns. Use it to build a new gun that works on the first try.
Statements marked `[inferred]` were read from code but not seen in game.

Code map (readable names, obfuscated name in brackets):

| Class | Role |
|---|---|
| `GunItem` (`deci.ay.i`) | the gun item; builds every path below from `gunName` + `category` |
| `BModelLoader` (`deci.n.g`) | parses `.bmodel` text files |
| `BModel` (`deci.n.f`) | loaded model: parts, anchor points, scope glass, casings |
| `BModelPart` (`deci.n.b`) | one named part (a ModelRenderer) |
| `GunAnimation` (`deci.F.a`) | parses and plays `.anib` files |
| `GunItemRenderer` (`deci.K.b`) | first/third person drawing, sway, recoil, attachments |
| `AttachmentItem` | attachment items; their models are separate `.bmodel`s |

## 1. Files one gun needs

For a gun registered as `new GunItem("ak74", stats, WeaponCategory.rifle, ammo)`
(category folder = the enum name: `rifle`, `smg`, `mg`, `shotgun`, `pistol`,
`revolver`, `rocket`, `crossbow`):

| File (inside the jar, `assets/deci/...`) | Required |
|---|---|
| `models/guns/rifle/ak74.bmodel` | yes, loaded once at item construction (client) |
| `textures/model/guns/rifle/ak74/ak74.png` | yes, the model texture |
| `textures/model/guns/rifle/ak74/ak74_<skin>.png` | per weapon skin (spray can) |
| `textures/items/gun/rifle/ak74.png` | inventory icon (inventory does NOT draw the 3D model) |
| `animations/ak74/ak74Fire.anib` | yes, played on every shot |
| `animations/ak74/ak74Reload1.anib` | yes, played on reload key, drives the actual reload |
| `animations/ak74/ak74Rack.anib` | yes for bolt/charge guns (played when racking) |
| `animations/ak74/ak74SlideBack.anib` | optional (6 of 98 guns lack it; its absence is silently ignored) |
| `sounds/guns/rifle/ak74/fire.ogg`, `fire_distant.ogg`, `fire_suppressed.ogg` | always registered |
| `.../magIn.ogg`, `magOut.ogg`, `rack.ogg` | magazine guns and revolvers (`rocket`: magIn/magOut only) |
| `.../insert_shell.ogg` | guns with shell-by-shell ammo |
| `.../pump.ogg` | guns with the PUMP fire mode |

Note the animation folder has no category level: `animations/<gun>/`.

## 2. The `.bmodel` format

Plain text, read line by line by naive `contains`/`substring` matching (no real
parser, so keep to the exact shapes below; one statement per line).

Header lines (all optional, values are model units, 1 unit = 1/16 block):

| Line | Meaning | Default |
|---|---|---|
| `mOff: x, y, z;` | whole model offset | 0,0,0 |
| `sPos: x, y, z;` | sight position (used to centre the gun on screen when aiming) | 0 |
| `flamePos: x, y, z;` | muzzle: flash, smoke, barrel attachments are placed here | 20,-4,0 |
| `ejectPos: x, y, z;` | where spent casings spawn | 0 |
| `rhPos: x, y, z;` / `rhRot: x, y, z;` | right hand (trigger hand) pose, rot in degrees | -9,0.72,-2 / 0 |
| `lhPos: x, y, z;` / `lhRot: x, y, z;` | left hand (support hand) pose | 6.5,7.2,3.5 / 0 |
| `Scale: s;` | model scale | 1 |
| `textureWidth = 512;` / `textureHeight = 32;` | texture size for UV mapping | |

Parts (Techne / Toolbox style export):

```
gunModel0 = new ModelRenderer(this, 1, 1);       // also WW2ModelRenderer / BeardieModelRenderer
gunModel0.addShape(x, y, z, {{..},{..},{..},{..},{..},{..},{..},{..}}, w, h, d);  // 8 corner offsets
gunModel0.setRotationPoint(x, y, z);
gunModel0.setRotation(rx, ry, rz);               // radians; " / rotFix" suffix allowed
parent.addChild(child);                          // child is removed from the top level list
```

- The variable name becomes the part's name. It is what animations target.
- `new ...ModelRenderer(this, u, v)` gives the texture offset of the part.
- Numbers may carry an `F` suffix (`1.5F`); it is stripped.
- **Use `addShape`, never `addBox`.** `BModelLoader` passes the Y offset as
  the Z offset for `addBox` (bytecode confirmed: same local pushed twice), so
  any `addBox` part lands in the wrong place. All 98 shipped guns use
  `addShape` only (16757 lines, 0 `addBox`). A plain box is an `addShape`
  with all corner offsets 0.
- `addBox`/`addShape` lines containing a `"` are ignored.

### Part names that the code treats specially

| Name contains | Effect |
|---|---|
| `ammoModel` | every part whose name contains `ammoModel` follows any animation pose whose name contains `ammoModel`, so one pose moves the whole magazine |
| `scopeGlass` | rendered with the scope texture: originally a second world render (render-to-texture zoom); since v0.28.0 the centre of the zoomed frame (fixes/ScopeZoom), old way behind `pictureInPicture` |
| `scopeOverlay` | drawn semi transparent over the scope, texture `textures/model/guns/scopes/<scope>.png` |
| `leftArm` / `rightArm` | used in arm models, not gun models |

Every other name is free. The shipped guns use these conventions (only
meaningful because their `.anib` files target them by name):
`gunModel*` body, `ammoModel*` magazine, `slideModel*` slide/bolt carrier,
`pumpModel*`, `breakActionModel*`, `revolverBarrelModel*`,
`minigunBarrelModel*`, `bolt*`, `defaultBarrelModel*`, `defaultScopeModel*`,
`defaultGripModel*`, `defaultStockModel*`.

Important: **nothing in the code hides `default*Model` parts when an
attachment is fitted** (no reference to those names anywhere). They are
ordinary parts that always render. Attachments are simply drawn on top.

## 3. The `.anib` animation format

```
Length: 57          // number of frames (frame slots are created up front)
Hand: 0             // 0 or 1, see below
STATIC              // optional: never advances (SlideBack uses this to hold a pose)
---------------------------
START
Frame {             // a keyframe
 OffHand { Pos: x, y, z; Rot: x, y, z; }    // support hand
 Model   { Pos: ...; Rot: ...; }            // whole gun
 slideModel0 { Pos: -2.4, 0, 0; Rot: 0, 0, 0; }   // any part, by exact name (case insensitive)
}
Frame SKIP          // an in-between frame: interpolated from the keyframes around it
RAND Frame { ... }  // every value scaled by a random 0..1 (fire jitter)
SWITCH Frame { ... }    // on this frame: unload (sends PacketReload())
LOAD Frame { ... }      // on this frame: load (sends PacketReload(1))
TRYBOLT Frame { ... }   // on this frame: if the gun was empty, chain into the Rack animation
12 REPEAT Frame { ... } // jump back to frame 12 while ammo is missing (shell by shell reload)
PlaySound: MagOut;      // plays deci:<gunName>MagOut on the current frame
Shake: 1.2;             // camera shake on the current frame
END
```

- Poses are offsets added to the part's rest pose: `Pos` in model units,
  `Rot` in degrees. Parts not named in a frame inherit the pose they have in
  other frames (the loader fills every frame with every named part).
- Playback: one frame per client tick (20 per second), advanced in
  `PlayerData.clientTick`. A 57 frame reload lasts about 2.9 s.
- When the animation ends every animated part snaps back to its rest pose.
- `PlaySound` names are appended to the gun name, so they must match the
  registered sounds: `Fire`, `MagIn`, `MagOut`, `Rack`, `Pump`, `LoadShell`.

What the shipped guns do (useful as templates):

| Animation | Frames (min/avg/max) | Typical content |
|---|---|---|
| Fire | 1 / 3 / 19 | `RAND Frame` kicking `slideModel*` back; Hand 0 (90 guns) |
| Rack | 1 / 16 / 19 | slide/bolt travel, `PlaySound: Rack;`, `Shake`; Hand 1 for 70 guns |
| Reload1 | 46 / 56 / 57 | magazine out (`SWITCH`), `PlaySound: MagOut/MagIn`, magazine in (`LOAD`), `TRYBOLT`; 2 guns use `REPEAT` |
| SlideBack | 1 | `STATIC`, holds the slide back when the gun is emptied |

`Hand` [inferred]: the muzzle flash/smoke offset follows the animation's
`Model` rotation only when `Hand` is 0; it also selects which arm follows the
`OffHand` pose.

When each plays: Fire on every shot (`GunItem`), Reload1 on the reload key
(`KeyBindingHandler`), Rack when a `TRYBOLT` frame finds the gun empty
(`PlayerData`) or when racking, SlideBack after the last round if the gun was
bolted.

## 4. Drawing (GunItemRenderer)

- Inventory: not handled by the 3D renderer; the flat icon is used.
- First person: fixed base transform, then aim mode offset (`sPos` vs `rhPos`
  centre the sights), idle sway from `ClientState.swayClock` (sin waves; the
  clock is the SmoothSwingThread, see bug.md), recoil from
  `GunItem.cameraKick` (halved by a foregrip), head yaw sway, then the current
  animation's `Model` rotation, then the gun at 2x scale.
- Overlays (fancy graphics only): the model is drawn again with
  `textures/particle/mud/mudN.png` and `textures/particle/blood/bloodN.png`
  when the gun is dirty, so the model's UVs are reused for dirt.
- Attachments: barrel attachment at `flamePos / 21` plus fixed offsets, grip
  and sight at fixed offsets (with special cases for mp7, bayonet, dragunov
  scope, red dot). Attachment models: `models/attachments/<slot>/<name>.bmodel`,
  textures `textures/model/attachments/<slot>/<name>.png`. Fixed offsets mean a
  new gun's rails must sit where the existing guns' rails sit, or attachments
  will float [inferred].
- Muzzle flash and smoke puffs at `flamePos`; casings spawn at `ejectPos`
  while the client cooldown runs.

## 5. Checklist for a new gun

1. Model in Techne/Toolbox, every box as a shape box (`addShape`). Name the
   body `gunModel*`, the magazine `ammoModel*`, the moving slide/bolt
   `slideModel*`. Export, add the header lines (`flamePos`, `ejectPos`,
   `sPos`, `rhPos`, `lhPos`, texture size).
2. Texture at the model's UV layout, plus a 16x16 style inventory icon.
3. Copy the four `.anib` files of a similar gun, rename them, and keep the
   part names they target identical to your model's names.
4. Add the sounds (or copy a similar gun's).
5. Register: clone an existing `new GunItem(...)` line in `ItemRegistry`
   (`deci.aD.k`) with the new name and stats (`create_weapons.md`), as a
   Javassist patch or from our own mod.
6. Test in the dev workspace (`dev/`, `runClient`).

Fastest path: start from an existing gun of the same action type (copy its
`.bmodel` and `.anib` files) and change the shape, so the animation part
names already match.

## 6. Our own guns: the tools/guns pipeline (v0.36.0, MAC-10 pilot)

Every asset is our own work (public repo: never copy Decimation art).
1. Spec: `tools/guns/<gun>.py` lists boxes in model units (x forward, y
   down, z sideways), part group (gunModel / ammoModel / slideModel),
   colour, face style (port, grip, rings), the header (flamePos, ejectPos,
   rhPos / lhPos) and the animations (`gunmodel.anib` keyframes).
2. Build: `python3 tools/guns/<gun>.py` (`tools/guns/gunmodel.py`) writes
   into dev/src/main/resources/assets/deci/: `models/guns/<cat>/<gun>.bmodel`
   (one part per box, addShape with 8 zero corners, decimal sizes are
   fine: BModelLoader parses doubles), the texture (box UV exactly as
   BModelBox maps it, painted procedurally at 4 px per unit), the 32x32
   icon (side silhouette with outline), `animations/<gun>/*.anib`; plus a
   .bbmodel ops file for previews.
3. Preview without the game: `python3 tools/bbmcp.py batch FILE.json`
   drives the headless Blockbench MCP (bbmodel_create / edit /
   add_texture / validate / contact_sheet); renders land in
   tools/guns/models/render_*.png (git ignored). Views front / back are the
   gun's sides, three-quarter, top.
4. Register in Java (fixes/NewGuns): `Deci.newMagazine` (bullet of an
   existing mag, an existing mag's icon), `Deci.newGun` (GunStats, category,
   magazines, slowdown, damage; registers as deciworldgen:<gun>),
   `Deci.addLootLike` (loot where a similar gun is), and on the client
   `Deci.useGunSounds` (points the gun's SoundEntry paths at an existing
   gun's files and rewrites Decimation's sounds.json; no copied audio).
   Names in assets/deciworldgen/lang/en_US.lang (item.<gun>.name).
5. Test: `python3 tools/devtest.py gun` (devtest/GunTest, test arena):
   first person still, reload key pressed (Decimation's key 19, isPressed
   on its client tick; items must be given on the SERVER, mags need NBT
   "ammo"), the gun's rounds after the reload, a bandit holding and firing
   it. Player firing cannot be automated: GunItem reads the fire button
   from the mouse itself (Mouse.isButtonDown(0)).
Facts learned: BModelBox corner array order (index 7 = (0,0,0) corner, 6
= (w,0,0), 4 = (w,h,0), 5 = (0,h,0), 3 = (0,0,d), 2 = (w,0,d), 0 =
(w,h,d), 1 = (0,h,d)), offsets are added; face UVs: top (y = 0) at
(u+d, v), bottom at (u+d+w, v), then x = 0, z = 0, x = w, z = d sides on
the row v+d. Decimation's guns use 2 px per unit textures (Uzi 1024x32
for a 512x16 layout), 32x32 icons.

Outside 3D generators (Claude Design "3D object", 2026-10-10, judged from
a screenshot only, no file tried): they export OBJ meshes (triangles, own
UV atlas). Decimation draws only `addShape` parts (8 corners each), so an
OBJ cannot be loaded as is. Usable path [not verified]: ask for boxes only
(every part one cuboid or 8 vertex block, no cylinders), one OBJ object per
part, named by group (gunModel / ammoModel / slideModel), colours per
material (MTL), no texture; a converter (to write: obj to the tools/guns
spec) turns each 8 vertex object into a box with corner offsets, and
gunmodel.py paints the box UV texture as usual. The generator's own atlas
and UVs are useless here (box UV is fixed by BModelBox).
Decision (user, 2026-10-10): keep the tools/guns pipeline (spec boxes +
Blockbench MCP previews); no OBJ converter, no outside generator.
