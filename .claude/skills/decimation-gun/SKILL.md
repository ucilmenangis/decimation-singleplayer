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

0. "Lessons for every gun" and the "Revision casebook" at the end of
   this file: the user's reviews turned into rules, and every past issue
   with its cause, code location, fix and check. Apply the lessons; when
   a problem appears, find its case first.

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
  Views side, other, top, three, rear (from behind, slightly above: the
  sight picture) and low (three quarter from below: parts hanging off
  the bottom).
- `python3 tools/guns/study.py contact ours:<gun> ['gunModel1\d{3}']`:
  parts that do not sit on anything (a thin detail needs one whole face
  on or in another part; a thicker part only has to overlap one). Must
  list nothing but known collars and flanges (a ring around a barrel, a
  magazine base plate, a magazine well lip wider than its host). Decimation's
  own guns pass it almost clean (M4A4 2, Uzi 5).
- `python3 tools/guns/study.py gaps ours:<gun>`: every metric vs the
  Decimation guns of its category (median, q10 .. q90, "<- outside"):
  parts, size, part sizes (absolute and relative to the length: read the
  "/L%" rows for short or long guns), offsets, detail placement, shape
  kinds, tone. Close every gap that is not explained by the real gun
  (a short real gun stays short).
- `vocab ours:<gun>` vs 60 / 31 / 5 / 4 % (all guns; SMGs pooled 59 /
  33 / 6 / 3).
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

## 6b. Performance (user rule 10 Oktober 2026: "add verify performance")

- `python3 tools/devtest.py --live gunperf guns=<ref>,<gun> attach=` and
  again with `attach=reddot,<cat>Suppressor`: fps dropped and held next to
  the Decimation reference; ours must be within a few fps of it.
- If anything lags: docs/performance.md section 4 (perfcheck log / world,
  census growth, JFR on the live game). Lag is often not the gun: measure
  before blaming it.

## 7. Finish

- Shots worth keeping to docs/shots/<topic>_v<version>/ and described in
  docs/shots_index.md; findings into docs/gun_style_guide.md (new rule or
  number) the moment they are learned.
- docs/roadmap.md, new_feature.md, version bump, graph update
  (tools/graph_update.py prepare, graph_carry.py, finish), commit (never
  push, no attribution lines, no dash punctuation).
- Ask the user to try it in game (firing, aiming, feel) and record the
  verdict.
- After EVERY user revision (user rule 10 Oktober 2026): turn it into a
  general rule in "Lessons for every gun" below (what to check on ANY
  gun, with the numbers) AND write a full case in the "Revision
  casebook" (symptom, cause, where in the code, fix, check, evidence),
  put the rule into docs/gun_style_guide.md, all in the same commit.
  Never delete cases: they are the book for the next time the same issue
  shows up on another gun.

## Lessons for every gun (from user reviews; read before starting)

The user knows real guns far better than my training does (user,
10 Oktober 2026): treat their photos, screenshots and remarks as the
source of truth about the real gun and about how it must look in game.

1. Ask for the real gun first: photos (clean side view, three quarter,
   with each accessory), and ask how its parts and accessories really
   work when unsure (how the stock folds, where the charging handle is,
   how a suppressor mounts). Do not guess gun facts. (MAC-10: the user's
   photos fixed every proportion; my guesses were wrong.)
2. Measure the photos: pixels against one known length, write the
   numbers into the spec header. Decimation then draws guns a bit slimmer
   than real (SMG about 1.2 to 1.6 wide).
3. Look like Decimation, not like a toy: many small shape parts (tapers
   first), dark flat tones per part with gradation (15 to 31 shades,
   faces about 5 apart, texel noise about 2.5), no painted detail. (MAC-10
   v1: 23 light grey boxes, rejected; v0.37.0: too few shades.)
4. Accessories follow the real gun: check how each mounts on THIS gun in
   the photos. A suppressor on a threaded barrel screws over the threads
   up to the nut / receiver front, centred on the bore; it never floats
   ahead or hangs low. (MAC-10 v0.37.0 and v0.37.1.)
5. flamePos y goes about 0.85 ABOVE the bore (Decimation's convention,
   Uzi -4.5 over a barrel at -3.5); x at the muzzle tip minus 0.1. Barrel
   attachments hang from it. (MAC-10 v0.37.1: flamePos on the bore put
   the suppressor 0.9 low.)
6. Short guns (muzzle below x about 12): the game's barrel attachment
   formula misses the muzzle; fix with Deci.offsetAttachment in NewGuns
   plus study.py ATTACH_FIX, then check x and height against a
   Decimation gun with study.py attach. (MAC-10 v0.37.0.)
7. Aim: build the sight picture like a Decimation gun of the same kind
   (Uzi: the screen centre passes through the rear aperture hole, about
   y -4.65, front post tip at the same height) and check it SIDE BY SIDE
   with that gun in the same run, cropped with centre lines. Absolute
   numbers and my held camera test fooled me once. (MAC-10 v0.37.0 and
   v0.37.1.)
8. Keep the line of sight clear: nothing behind the rear sight (stock
   loops, charging handles, scope mounts) may rise above about -4.57.
   (MAC-10 v0.37.1: the stock loop crossed the aperture.)
9. Judge small offsets by numbers (study.py) or enlarged crops, not by an
   854 x 480 NPC shot; when the user's screenshot disagrees with my test,
   the user's screenshot wins: crop it and measure. (MAC-10 v0.37.1.)
10. Show the user side by side renders (ours next to the reference and
   their photo) before asking for an in game test; ask what still looks
   off rather than assuming it is done.
11. Keep what the user liked (MAC-10 v0.37.2 verdict, 10 Oktober 2026:
   "i like the style of the gun firing", icon "good art"): the Fire
   animation as Decimation does it (2 frames, RAND, only the
   slideModel parts kick back about 1.6, no whole gun kick), Rack and
   Reload1 on Decimation's timings, SlideBack held when empty, and the
   icon rendered from the model (transparent background, dark outline).
12. "Perfect is perfect" (user, v0.37.3): after acceptance, keep closing
   measurable gaps with `study.py gaps` until every metric not explained
   by the real gun sits inside the category's q10 .. q90: shape kinds
   (convert plain blocks to bevels / skews / wedges where they help the
   look; octagon middles, windows and rods stay plain), declared sizes
   rounded (long parts 1x1xN), raised details about 0.08 proud, tone near
   the median, middle detail from the photos.

13. A variant of a gun Decimation already has (user, UMP9: "mostly same
   like ump45, take from 45acp asset then replace the mag"): do not model
   it from scratch. Generate it locally from the user's Decimation.jar
   (tools/guns/ump9.py is the template): their model without the parts
   that differ, our own parts added (anchored on the replaced part's
   position, e.g. the old magazine's top face), their texture with our
   islands below, their animations under our name, the icon rendered.
   The generated files are git ignored (public repo: never Decimation
   art), NewGuns registers the gun only when its model exists. Aim,
   suppressor and hands are then Decimation's own.

14. Verify performance for every gun (user, v0.38.0: "1 fps after i drop
   it ... add verify performance skills on guns too"): run gunperf against
   the reference gun before handing a gun over; when the user reports lag,
   measure first (docs/performance.md checklist): in that report the gun
   was innocent (a corrupt dev world chunk doubling entities, and
   Decimation's infected path searching every tick).

15. Every part sits on its host, and every sight looks like the real one
   from behind (user, HK416 v0.39.0: "flying" stock pieces, front sight
   "like 2 pillar"). Before handing over: `study.py contact` lists no
   part (thin details on a slanted or angled face need their inner side
   pushed INTO the host, 0.05 to 0.15, because the face leans); look at
   the `low` and `rear` renders, not only side and three. Details on an
   angled edge are placed from the edge's formula at their own x (a
   function like lower_bottom(x)), never from one fixed y. Sight ears no
   higher than the base gun's (M4A4 front ears stop at the post tip,
   -4.7): higher ears show beside the rear sight in aim. Real gun
   sight shapes from the user's photo (HK: a U of thick flared ears on
   one bridge, not two thin posts).

16. A variant that makes the base texture taller: first check that no
   base part reads past the base's textureHeight (study.Gun parts with
   v + d + h > th). The game wraps those reads to the top (GL_REPEAT);
   a taller texture hands them OUR islands, transparent texels included,
   and the part looks cut or recoloured. Copy the wrapped rows into
   place and start our islands below them (tools/guns/hk416.py "spill").
   Of all 98 Decimation guns only the M4A4 does this (its rear sight,
   defaultScopeModel3..15, v 33 on a 32 high texture). When a part of
   the BASE gun looks wrong only in our variant, suspect the texture
   before the geometry: paint our islands one colour and the base's
   another in a debug texture and look again (case 14).

## Revision casebook (never delete a case; look here first)

Every user revision and every problem found on the way, with how it was
found, why it happened, where the code is, the fix and how it was
checked. When something similar shows up on another gun, start from the
matching case instead of researching again. Add a new case after every
revision (same commit), never remove old ones.

### Case 1: the gun looks bad, boxy, toy like (MAC-10 v1, v0.36.0)
- Symptom (user): "not good, needs polish"; first person a light grey
  block seen from behind, bigger than the Uzi.
- Cause: 23 plain boxes, painted texture detail (ports, rings,
  checkering), light grey, 12 x 10.2 x 2.2 units (real gun about 8.7 x 1.4).
- How Decimation does it: docs/gun_style_guide.md (all 98 guns studied):
  99 % shape parts, 1x1x1 declared, tapers first, many small parts, flat
  dark tones, slim.
- Fix: gunmodel.py v2 (part / inset / shift / mirror / octagon), MAC-10
  rebuilt to 102 parts from the user's photo (v0.37.0).
- Check: study.py sheet uzi ours:mac10, vocab, stats; gunview hip.
- Evidence: docs/shots/guns_study_v0.36/cmp_uzi_mac10.png (before),
  docs/shots/mac10_v0.37/cmp_side.png (after).

### Case 2: aim not centred on the crosshair (v0.37.0 to v0.37.2)
- Symptom (user): aiming, the sights are not on the 0 crosshair.
- How aim works: GunItemRenderer (deobf/src/decimation/render/
  GunItemRenderer.java) aim branch draws EVERY gun at one fixed place
  (x scaled 0.5, translate (-1, -0.35, 0.923)); sPos is not used in
  first person. So the sight picture is a fixed MODEL height.
- First wrong turn: my gunview test (camera held still) said the sight
  tops were on the centre, and I blamed Decimation's aim sway
  (ClientEventHandler headYawSway / dP). The user then sent MAC-10 and
  Uzi aim screenshots from the same game: the centre goes through the
  Uzi's rear APERTURE HOLE (about y -4.65), ours sat 0.25 higher.
- Fix (v0.37.2, tools/guns/mac10.py "front sight" / "rear sight"
  blocks): aperture hole y -4.8 to -4.55 (centre -4.675), front post tip
  -4.67, ears to -4.97, centred z -0.15.
- Check: `devtest.py --live gunview guns=uzi,mac10 attach=`, crop both
  aim shots around the centre with centre lines side by side
  (docs/shots/mac10_v0.37/v0372_aim_iron_cmp.png): both holes on the
  line. Measure user screenshots the same way (game area starts below
  the 83 px title bar on their Mac window).
- Uzi reference numbers: rear aperture hole about -4.75 to -4.55 [inferred from its parts], rear
  ears top -4.97, front post tip -4.52, front ears top -4.83.

### Case 3: suppressor floating ahead of the muzzle (v0.37.0)
- Symptom (user): suppressor "flying"; a gap of about 1.1 units in their
  hip screenshot.
- Cause: GunItemRenderer.renderAttachments places a barrel attachment at
  translate(-1.55, 0.27, -0.003) + flamePos / 21 (GL units, 0.0625 per
  model unit), so x = constant + 0.762 flamePos x: it meets the muzzle
  only for muzzles around x 12 to 15. Gaps (study.py): Uzi 0.12, UMP45 0,
  MP5A3 / Vector 0.44, MP7 -0.56 (Decimation hardcodes an MP7 shift),
  MAC-10 1.12.
- Fix: `Deci.offsetAttachment(gun, "smgSuppressor", dx, dy, dz)`
  (fixes/Deci.java): loads the attachment's .bmodel (BModelLoader
  deci.n.g.a), adds to its offset (BModel deci.n.f fields lc / ld / le,
  package private, reflection; renderParts applies them) and puts it in
  the attachment's per gun model cache (AttachmentItem deci.ay.h, field
  ST, read by getModelFor b). Called client side in fixes/NewGuns; the
  same numbers in tools/guns/study.py ATTACH_FIX for renders.
- Check: study.py attach ours:<gun> smgSuppressor (x gap like the Uzi's),
  then in game (first person hip crop, NPC side view); log line
  "suppressor moved".
- Other barrel attachments per category: pistolSuppressor,
  arSuppressor, shotgunSuppressor, mgSuppressor, bayonet (rifle): same
  formula, same fix.

### Case 4: suppressor low and not over the threads (v0.37.1)
- Symptom (user): suppressor "doesn't match the MAC-10 flash hider";
  their hip screenshot: about 0.9 units below the bore, starting after
  the thread tip (real one screws over the threads, user photo 2).
- Cause: flamePos y was on the bore. Decimation's convention: flamePos y
  about 0.85 ABOVE the bore (Uzi flamePos -4.5, barrel centre -3.5; UMP45
  -3.75 over about -2.95); barrel attachments hang from flamePos, so they
  land on the bore only with that convention. I first misread study.py's
  low suppressors as a render bug: the renders were right.
- Fix (v0.37.2): flamePos (10.4, -4.75, -0.15); offset (-2.37, -0.16, 0)
  so the suppressor covers the threads up to the receiver front.
- Check: study.py attach (suppressor x 9.25 to 14.25, centre on the bore
  -3.9), first person crop docs/shots/mac10_v0.37/v0372_hip_crop.png.

### Case 5: no gradation, few static colours (v0.37.0)
- Symptom (user): Decimation guns have gradation "if you look closely";
  ours only a few static colours.
- Measured (1x1x1 islands of Uzi / AK74 / MP5A3; script idea: read the 6
  face texels of each island at u+1,v / u+2,v / u..u+3,v+1): 15 to 31
  distinct part tones per gun, faces of one part 4.7 to 5.7 apart, texel
  noise 2.0 to 2.7, sometimes top lighter (Glock tone vs height -0.55).
  Ours had 9 tones, faces 2.6, noise 1.6.
- Fix: gunmodel.paint: part shade +-6 %, +5 top to -5 bottom, faces top
  +2 / bottom -2 / sides +-1.5, texel noise +-4 (MAC-10: 37 tones, 5.2,
  2.1).
- Check: re-run the island measurement on ours vs a Decimation gun.

### Case 6: the stock crosses the sight picture (v0.37.2, found while fixing case 2)
- Symptom: aiming, the folded wire stock loop's rear bar showed just under
  the aperture hole.
- Fix: loop top lowered from -4.72 to -4.57 (tools/guns/mac10.py "folded
  wire shoulder loop"). Rule: nothing behind the rear sight above about
  -4.57.

### Case 7: inventory icon with a light halo (v0.37.0, found by me)
- Cause: icon downscaled from a render on the light study background.
- Fix: gunmodel.icon renders with bg (0, 0, 0, 0), keeps pixels with
  alpha >= 140, un-premultiplies, dark outline. Check: enlarge next to
  Decimation's icon (docs/shots/mac10_v0.37/icon_vs_uzi.png).

### Case 0: verdict, MAC-10 accepted (v0.37.2, 10 Oktober 2026)
- User: aim "done, its fixed", firing "its good actually, i like the
  style of the gun firing", icon "its fixed and good art". Their final
  screenshot (docs/shots/mac10_v0.37/user_v0372_final.png): hip view
  with suppressor and red dot, suppressor on the bore over the threads.
- Path to acceptance: v1 rejected (case 1), v2 from photos, three
  revisions (cases 2 to 7). Reuse this path: photos, study, anchors,
  side by side checks, user screenshots.

### Case 9: closing the gaps after acceptance (v0.37.3, 10 Oktober 2026)
- Request (user): "perfect is perfect, we close the gaps until the mac10
  looks really like decimation guns" (after asking what tapers are).
- Measured with the new `study.py gaps` (13 Decimation SMGs): shape kinds
  58 / 32 / 5 / 5 -> 60 / 31 / 5 / 5 % (13 plain blocks turned into
  bevelled seams and ribs, a drafted magazine, a curved trigger, leaning
  butt struts, wedge bends on the loop and the strap lug); declared sizes
  were all 1x1x1, now rounded (78 % 1x1x1, 11 % 1x1x2, long panels 1x1x5 to
  7) so long faces get texels with gradation along them (gunmodel.size,
  layout, paint); tone 42 -> 47 (median 49); middle detail 44 -> 48 %
  (rivets, housing side plates, SAFE / FIRE lever, ejection deflector,
  slot notch from photos 1 and 3; the real MAC-10 has a short receiver,
  so it stays under the median 60); raised details 0.03 to 0.05 -> 0.08
  proud (PROUD set in mac10.py). Remaining "outside": length and the
  absolute part sizes / offsets that follow from the short real gun;
  every length relative size is inside.
- Check: geometry identical after the size change (all 8 corners of all
  parts compared), gaps report, study.py sheet with the Uzi, gunview aim
  (holes still on the line, v0373_aim_cmp.png), hip with suppressor
  (v0373_hip_supp_crop.png), gun test (reload 30), suppressor numbers
  unchanged (x 9.25 to 14.25, centre -3.90).

### Case 10: UMP9, a variant of Decimation's UMP45 (v0.38.0, 10 Oktober 2026)
- Request (user): UMP9, "basically 9mm version of the ump45 ... ump9 has
  rounded mag like rpk, akm mag"; photos 4 (side views, three quarter,
  suppressor); then "mostly same like ump45, you can take from 45acp
  asset then replace the mag with new model 9mm".
- First plan was a full model from the photos (photo 2 measured: scaled
  to the UMP45's 19.7 length, bore -2.93 vs the UMP45's about -2.9,
  flamePos (14.25, -3.78) vs (13.9, -3.75), so the photo and Decimation's
  model agree); the user's shortcut is better: exact Decimation look.
- How: tools/guns/ump9.py reads ump45.bmodel / ump45.png / ump45*.anib
  from Decimation.jar, drops every line with ammoModel (their straight
  2 part magazine), doubles textureHeight, appends our magazine
  (gunmodel.part_block, islands in the new lower half, gunmodel.paint),
  writes the assets (git ignored), copies the animations as ump9*.anib
  (their magazine pose moves every ammoModel* part), renders the icon.
- Our magazine: anchored on the UMP45 magazine's top face (rear
  (7.41, -1.18), front (8.58, -1.43), 1.17 deep); 0.75 wide; 6 skewed
  segments curving forward 1.92 over 4.43 (photo: about 0.45 forward per
  unit down, growing toward the base); smoked window strips both sides;
  base plate; 15 parts.
- Registration (fixes/NewGuns.registerUmp9): only if
  /assets/deci/models/guns/smg/ump9.bmodel exists; ump9Mag 30 rounds
  with the MP5A3 magazine's 9 mm bullet and curved icon; damage 11, 650
  rpm, recoil 5.5 / 0.4, recovery 5, slowdown 0.14; loot where the
  UMP45 and its magazine are; the UMP45's sounds.
- Check: study.py sheet ump45 ours:ump9 (docs/shots/ump9_v0.38/
  cmp_side.png), render three; gunview guns=ump45,ump9: hip and aim
  identical to the UMP45; gun test gun=ump9: registered, reload 30.
  DevTestGun now takes -Pgun=NAME.

### Case 11: "1 fps after dropping the gun" (v0.38.0 -> v0.38.1, 10 Oktober 2026)
- Symptom (user): huge lag, 1 fps after dropping a gun in the dev world.
- First check: gunperf (new dev test) dropped / held vs baseline for
  Uzi, UMP45, MAC-10, UMP9: all within 2 fps of each other and of the
  baseline. The guns were not the cause.
- Real causes (docs/performance.md): the dev world's region slot (-1,-6)
  held chunk (-4,-1), entities doubled on every save / load (40 730, 31 010
  Hulks) with a Forge stack trace per entity per tick; and Decimation's
  infected running a full path search every tick on both sides.
- Fix: tools/perfcheck.py fixchunk + census kill= (dev world only);
  tools/patches/PatchInfectedAI.java (three jars) for the real game: fps
  21 -> 35 with the same 53 infected, client pathfinding 60 % -> 0 %;
  zombies test still passes.
- Lesson 14; tools: gunperf, census, perfcheck, JFR via jcmd.

### Case 12: verdict, UMP9 accepted (v0.38.1, 10 Oktober 2026)
- User: "the ump9 work really well and no problem". The variant route
  (lesson 13: Decimation's model plus our part, generated locally) needed
  no revision: aim, attachments and hands were Decimation's own.

### Case 13: HK416 black and tan, a variant with many replaced parts (v0.39.0, 10 Oktober 2026)
- Request (user): HK416, 5 photos, "make 2 version black and tan", then
  "two separate guns" (choices offered: Decimation's skin spray can,
  two guns, both).
- How: tools/guns/hk416.py (template for variants that replace several
  areas): parse the base model with study.Gun, choose the parts to drop
  by POSITION (dropped(): x / y ranges per area, plus named grip parts;
  beware long parts that start inside a dropped area and run past it,
  like the M4A4 barrel 12.15 .. 18.15: drop anything reaching past the
  area's end), filter their bmodel lines with a word boundary regex on
  the names, append ours as gunModel1000+.
- Fitting ours to theirs: read their receiver's numbers, not the photo's
  heights (the M4A4 receiver rail teeth are at y -3.6 on a 0.4 pitch; the
  first handguard stood 0.6 higher than the receiver, the real gun is
  flush); lengths from the photo calibrated on two kept parts (rear
  sight, muzzle) and checked on a third (butt within 0.03).
- Colour variants: same model, recoloured texture; bring each island to
  one tone first (their islands are darker than ours: the first tan
  receiver came out dark brown), keep the texel gradation, a +-6 % shade
  per island; parts that stay black chosen by name / position.
- Checks: aim crops next to the base gun, suppressor numbers equal to
  the base gun's, gun test with -Pmag=deci:m4a4Mag, gunperf.

### Case 14: HK416 floating stock pieces and "2 pillar" front sight (v0.39.0 -> v0.39.1, 10 Oktober 2026)
- Symptom (user, screenshots): dropped gun, side: "flying" pieces under
  the stock; first person from behind: "front sight looks like 2 pillar".
- Stock cause: three diagonal fins (tools/guns/hk416.py, stock block)
  were placed at one fixed y range (-1.2 .. -0.1) while the lower panel's
  bottom edge rises toward the front (0.35 at the butt to -1.6 at x
  -1.4): the front fin hung 0.77 below the panel. Their inner face also
  sat at z 0.15 on a side that leans in toward the bottom (inset 0.06).
- Stock fix: lower panel in two parts like the real slim line stock
  (photo 36): square rear part, wedge in front with the angled edge;
  lower_bottom(x) gives the edge's y at any x; seven short ribs (the
  real stock's ribbed band) each 0.22 inside the edge at its own front
  end, inner face at z 0.08 (inside the panel).
- Found on the way by the new `study.py contact`: handguard side rails
  and slots, the stock window slot (it ran past the bottom of the
  stock's front taper), the grip panels and finger bumps all stood off
  their leaning host faces by 0.02 to 0.06; their inner sides now start
  inside the host. HK416 parts not seated: 46 before, 1 after (the
  flash hider ring, a collar: correct).
- Front sight, part 1 (the real cause of the "pillars"): the M4A4's rear
  sight parts (defaultScopeModel3..15) read texture rows 33+ on a 32
  high texture and wrap to the top rows in the game. Our variant made
  the texture 64 high and put our islands at row 32+, so the rear sight
  sampled our islands, transparent texels included: in aim the left ear
  looked like a wide block, the right one a thin tall pillar (the user
  took them for the front sight). Found with a debug texture: our
  islands red, the M4A4's green: the rear sight came out red
  (docs/shots/hk416_v0.39.1/debug_colours_aim.png). Fix in build():
  spill = rows read past th (8), the M4A4's top rows copied there, our
  islands start below (texture now 512 x 128). Scan of all 98 guns: only
  the M4A4 does this, the UMP9 is safe.
- Front sight, part 2: our front sight was two thin separate ears (0.1
  wide, tops -4.97, 0.27 over the post): from behind two posts. Now a U
  like the real HK sight (photo 36): bridge (fsBridge) across, thick
  ears (0.16) flared at the base, tops at -4.74 (the M4A4's front ears
  stop at the post tip, -4.7; ears at -4.84 still peeked out beside the
  rear ears in aim), post base, post tip still -4.7.
- Check: study.py contact ours:hk416 'gunModel1\d{3}' (1, the collar);
  renders low / rear; gunview aim crops m4a4 / hk416 / hk416tan with
  centre lines: same symmetric rear sight, post on the line
  (docs/shots/hk416_v0.39.1/aim_cmp.png, before:
  aim_cmp_before_texfix.png); gunperf m4a4 dropped / held 28 / 30,
  hk416 25 / 29, baseline 29 (noise).
- Also found: our MAC-10 has 17 parts flagged by contact (accepted gun,
  not changed yet; roadmap).

### Case 15: MAC-10 parts not seated, found by the contact check (v0.39.3, 10 Oktober 2026)
- Request (user): "2" (clean up the 17 MAC-10 parts `study.py contact`
  flagged after case 14).
- Causes (tools/guns/mac10.py): raised side details (seam, housingPlate,
  gripPanel, housingRib, housingRib2) started at the host's NOMINAL side
  (z 0.45 / 0.4 / 0.3 / 0.32) while the host leans in (insets 0.06 to
  0.12): gaps of 0.05 to 0.14; the seam also ran 0.1 past the lower
  receiver's front end; the rear sight ears overhung their base by 0.04
  (base inset under them); triggerTip started 0.09 behind the trigger's
  lower end (a step); the stock hinge hovered above the rod end.
- Fix (v0.39.3): inner faces pushed into the host (outer faces kept, so
  the 0.08 proud look of case 9 stays), seam to x 8.8, rsBase widened
  0.05 under the ears, triggerTip from the trigger's lower end (x 5.84
  .. 5.99, raked back 0.14), hinge a knuckle around the rod end. Kept on
  purpose: gripLip and magBase, flanges wider than their host (like a
  collar). 17 flagged -> 2 (the flanges). Only those 14 parts moved
  (corner diff against the old spec), geometry elsewhere identical.
- Check: contact; before / after renders three and low
  (docs/shots/mac10_v0.39.3/); gunview aim next to the Uzi: aperture
  holes on the centre line as before (aim_cmp_uzi.png).

### Case 8: test traps met on the way (dev test gunview)
- No gun in the shots: F1 (hideGUI) hides the held item too; first
  person shots need the GUI on.
- Missing shots / aim stuck on: the client reads the server's tick
  counter and can skip values; phases are ranges, shots taken on the
  first tick at or after their time, aim set every tick.
- Live runs: a key=value stays set for later runs (clear with key=); new
  classes or fields need --stop and a restart; .bmodel / texture changes
  need a restart (models load at item construction); wait a few seconds
  after --stop.
- A stray NPC killed the test bandit: kill other living entities during
  the NPC phase; a Soviet target makes the bandit turn its side.
