---
name: decimation-gun
description: Make, rework or review a gun of our own for Decimation (Forge 1.7.10) in Decimation's own style, end to end (model, texture, icon, animations, aim, attachments, stats, sounds, loot, tests). Use when the user asks for a new gun, a gun model fix (the MAC-10 polish), or to compare a gun with Decimation's.
---

# Decimation gun, end to end

Everything here was learned from all 98 shipped guns (10 Oktober 2026).
The facts live in the docs; this skill is the order of work and the
checks. Never copy Decimation's models, textures, animations or sounds
into the repo: measure and learn from them, build our own.

## 0. Read first (cheap, do not re-derive)

1. `docs/gun_style_guide.md`: numbers, construction rules, shape kinds,
   detail placement, placement conventions, animations, first person,
   icons, aiming, attachments, the Uzi walkthrough, stats.
2. `docs/gun_model_spec.md`: file formats, paths, registration, section 6
   (our pipeline: tools/guns spec, gunmodel.py, bbmcp.py, NewGuns, Deci).
3. Datasets: `docs/references/decimation_guns.tsv` (shape per gun),
   `docs/references/decimation_gun_stats.tsv` (stats per gun).
4. `docs/shots_index.md` "guns_study_v0.36" for what the renders show.

## 1. Pick references (5 min)

- Photos first: ask the user for 2 to 4 pictures of the real gun (one
  clean side view, one three quarter, one with the accessory you will
  test, for example the suppressor), or use what they already sent. The
  MAC-10's side photo fixed every proportion in one pass: measure it in
  pixels against one known length (receiver = N units) and write the
  numbers into the spec header.
- Real gun: length, height, width in mm; divide by 31 for model units
  (Decimation guns are slimmer than real: SMG width about 1.2 to 1.6).
- Two or three Decimation guns of the same category and layout from the
  datasets (similar length, magazine in grip or in front, stock type).
- Render them: `python3 tools/guns/study.py sheet uzi mp5a3 --cols 2
  --scale 24 --out docs/shots/<topic>/ref.png` and `render NAME --split`
  for how their areas are cut. Read the images once, write what matters
  into the gun's spec file header comment.

## 2. Plan the parts (before writing boxes)

- Target count: style guide section 2 (SMG about 100, rifle 150 to 250,
  pistol 50 to 130). Spend parts where first person looks: rear sight,
  receiver top and right side, rear section (sections 8, 11, 15).
- List areas with a part budget, like the Uzi table (section 15).
- Fix the anchors first (sections 9, 13, 14):
  - top of receiver in the sight zone (x 1.5 to 5) at about y -4.45 (the
    red dot's bottom is -4.43);
  - iron sight picture like the Uzi's: rear aperture hole centre about
    y -4.65 to -4.68, front post tip at the same height, ears up to
    -4.97, centred z -0.15 (measured in the user's game, v0.37.2; the
    older "sight tops at -4.85 to -5.0" rule was wrong);
  - nothing else in the line of sight above about -4.57 behind the rear
    sight (a stock loop there crosses the aperture in aim);
  - gun centred on z -0.15, side details as mirrored pairs about it;
  - flamePos x at the muzzle tip minus 0.1, y about 0.85 ABOVE the bore
    (Decimation's convention: Uzi -4.5 over a barrel at -3.5; barrel
    attachments hang from it), z -0.15;
  - rhPos / lhPos from the closest Decimation gun of the same layout.

## 3. Build the model (tools/guns/<gun>.py + gunmodel.py)

- Parts are shape boxes: declared 1x1x1 (or 1x1xN), real size from corner
  offsets; default kind is a TAPER (60 % of Decimation's parts), then
  shrunk cuboids, skews, wedges (section 7).
- Recipes (section 3): layered receiver panels; grooves as thin plates
  0.1 thick; round parts as 3 part octagon slices; grips and curved
  magazines as skewed segments (magazine segments as addChild children of
  ammoModel0); chamfer every visible edge; thin features thin.
- Groups: gunModel*, ammoModel* (magazine), slideModel* (bolt / slide,
  moved by Fire, Rack, SlideBack).
- Texture: one flat dark tone per part island with faint noise, metal
  grey 24 to 64, wood like 41,25,22; textureWidth 512, UV step 8, PNG 2x
  (section 4). No painted detail.
- Icon: from the model's side render, about 30 px wide, own dark tones,
  1 px black outline, muzzle right (section 12).
- Animations: same timing as Decimation's (section 10): Fire 2 frames
  (slide only, no Model kick), Reload1 57 frames (MagOut at 5, SWITCH 20,
  LOAD 40 with MagIn, TRYBOLT 50), Rack 19 frames Hand 1, SlideBack 1
  frame STATIC. Our own keyframe values.
- If gunmodel.py lacks a feature the recipe needs (corner offsets,
  children, flat tones, icon from render), add it to gunmodel.py first.

## 4. Compare without the game (loop until it matches)

- `python3 tools/guns/study.py sheet <ref> ours:<gun> --cols 2 --scale 24
  --out ...`: silhouette, width, density, tone next to the reference.
- `python3 tools/guns/study.py render ours:<gun> --split`: part cuts.
- `python3 tools/guns/study.py stats` line for ours vs the category
  (parts, shaped %, size); `vocab ours:<gun>` vs 60 / 31 / 5 / 4 %.
- `python3 tools/guns/study.py attach ours:<gun> reddot smgSuppressor`:
  sight sits on the receiver, suppressor on the bore line. Compare
  suppressor x and y centre with a Decimation gun (Uzi: starts 0.12
  after the muzzle, centre 0.05 above its barrel centre). A short gun
  (muzzle below x about 12) needs `Deci.offsetAttachment(gun, name, dx,
  dy, dz)` in NewGuns plus the same numbers in study.py ATTACH_FIX; a
  threaded barrel: let the suppressor cover the threads up to the
  receiver front (the real gun does).

## 5. Register (Java, dev/src/main/java/net/decimation/fixes/NewGuns)

- Stats from the closest real equivalent in decimation_gun_stats.tsv
  (section 16), changed only with a reason; `Deci.newGun`,
  `Deci.newMagazine` (bullet and icon of an existing mag),
  `Deci.addLootLike`, `Deci.useGunSounds` (client only); lang line in
  assets/deciworldgen/lang/en_US.lang. Our items are deciworldgen:<name>.
- Attachments need no code: the category decides (section 14).
- New code reaches Decimation only through `net.decimation.fixes.Deci`.

## 6. Test in game (live, about 30 s a run)

- `python3 tools/devtest.py --live gunview guns=<ref>,<gun> attach=` then
  `attach=reddot,<cat>Suppressor`: hip, aim, NPC side view. Read
  dev/run/client/devtest/sheet_gunview.png.
- Aim: never judge by absolute numbers; crop the reference gun's and
  ours' aim shots around the screen centre with centre lines, side by
  side (docs/shots/mac10_v0.37/v0372_aim_iron_cmp.png): our aperture
  hole must sit on the line where the reference's sits. The user's own
  screenshots are the ground truth: crop them the same way.
- Small features (suppressor height) are too small to judge in the
  854 x 480 NPC shot: use study.py numbers or crop and enlarge first
  person shots.
- `python3 tools/devtest.py --live gun` (reload loads the magazine, NPC
  fires it). Player firing is manual (Decimation reads the mouse).
- Live traps: new classes / fields need `--stop`; `key=value` persists,
  clear with `key=`.

## 7. Finish

- Shots worth keeping to docs/shots/<topic>_v<version>/ and described in
  docs/shots_index.md; findings into docs/gun_style_guide.md (new rule or
  number) the moment they are learned.
- docs/roadmap.md, new_feature.md, version bump, graph update
  (tools/graph_update.py prepare, graph_carry.py, finish), commit (never
  push, no attribution lines, no dash punctuation).
- Ask the user to try it in game (firing, aiming, feel) and record the
  verdict.
- After EVERY user revision: add the lesson to the revision log below
  and the rule to docs/gun_style_guide.md, in the same commit (user rule
  10 Oktober 2026).

## Revision log (lessons from user reviews; read before starting)

- MAC-10 v1 (v0.36.0, "not good, needs polish"): 23 plain light grey
  boxes, too big, painted detail. Led to the whole study and gunmodel v2.
- MAC-10 v2 (v0.37.0, "huge upgrade"): the user's photos gave the
  proportions; shape parts, flat dark tones and Decimation timings made
  it read as a Decimation gun.
- v0.37.0 review: (1) aim not centred, (2) suppressor flying, (3) no
  gradation. (2): the barrel attachment formula only fits muzzles at x
  12 to 15: Deci.offsetAttachment. (3): Decimation textures have 15 to
  31 part shades, faces about 5 apart, noise about 2.5: gunmodel.paint
  now does that. (1): my held camera test said fine; it was not.
- v0.37.1 review (user screenshots of MAC-10 and Uzi aiming, suppressor
  hip view): the suppressor sat 0.9 below the bore because flamePos y
  must be 0.85 ABOVE the bore (Decimation convention), and it should
  cover the threads like the real one (user photo 2); the aim centre is
  the Uzi's aperture hole (about -4.65), not the sight tops. Fixed in
  v0.37.2: flamePos y -4.75, suppressor (-2.37, -0.16, 0), aperture hole
  -4.8 to -4.55, front post tip -4.67, stock loop lowered to -4.57.
