# Decimation gun style guide (study of all 98 shipped guns)

Read this before modelling any gun of our own. It is the "training" for
gun design: what Decimation's guns are made of, measured over every model,
plus the recipes behind their shapes. Study date 10 Oktober 2026, after the
user judged our MAC-10 pilot (v0.36.0) "still looks bad". Formats and
paths are in docs/gun_model_spec.md; this file is about LOOK and
CONSTRUCTION.

## 1. Tools (rerun instead of re-deriving)

`tools/guns/study.py` reads Decimation's guns straight from Decimation.jar
(and ours from dev/src/main/resources) and draws them with their real
textures, without the game (pure Python + Pillow, about 0.1 s a view):

    python3 tools/guns/study.py render uzi ak74 ours:mac10 [--scale 40] [--split] [--out DIR]
    python3 tools/guns/study.py sheet uzi mp5a3 ... --cols 3 --scale 16 --out FILE.png
    python3 tools/guns/study.py stats [--tsv FILE]
    python3 tools/guns/study.py parts uzi

Views: side (muzzle right), other, top, three (three quarter, front right,
from above). `--split` paints every part a flat colour with outlines: shows
how a shape is cut into parts. Renderer: BModelBox corner order and
offsets, rotation Z, Y, X about the rotation point, `addChild` children
placed in their parent's space (1055 addChild lines across the guns,
mostly magazines), vanilla box UV. Checked against the look of the Uzi,
MP5, AK74, Glock (renders match their shapes; texture flips per face
[not verified], irrelevant at this pixel size).

Dataset: `docs/references/decimation_guns.tsv`, one line per gun (parts,
shaped %, rotated %, length / height / width in model units, median part
volume, texture size and pixel density, part groups). Measurements only,
nothing of Decimation's art. Renders live in
docs/shots/guns_study_v0.36/ (git ignored, described in
docs/shots_index.md).

## 2. The numbers

All 16757 parts of the 98 guns (BeardieModelRenderer, `addShape` only):

| Measure | Value |
|---|---|
| parts with corner offsets (shape boxes) | 95 to 100 % per gun (99 % overall) |
| declared size (w, h, d sorted) | 1x1x1 72 %, 1x1x2 12 %, 1x1x3 4 %, longer 1x1xN for the rest; 13 parts of 16757 have a fractional size |
| real (effective) part size, q10 / median / q90 | smallest side 0.06 / 0.2 / 0.6; middle 0.15 / 0.4 / 1.1; longest 0.3 / 0.9 / 3.7 units |
| corner offset size, q10 / median / q90 | 0.1 / 0.35 / 0.55 units |
| parts with a rotation | median 3 to 6 % (revolvers 27 %, curved stocks / AR15 up to 41 %) |
| texture | textureWidth 512, height 16 / 32 / 64, PNG at 2x (1024 px wide) |
| UV offset step between parts on a row | 8 (83 %), 16, 24 |
| texture brightness (0..255, used pixels) | median mean 49, median spread 17 |

Per category (median, min to max parts; size in units):

| Category | Guns | Parts | Length | Height | Width |
|---|---|---|---|---|---|
| pistol | 9 | 52 / 83 / 126 | 8.3 | 5.4 | 1.1 |
| revolver | 2 | 84 / 94 / 105 | 8.9 | 4.8 | 1.4 |
| smg | 13 | 58 / 105 / 391 | 19.9 | 7.3 | 1.6 |
| rifle | 53 | 67 / 174 / 520 | 28.0 | 7.6 | 2.0 |
| shotgun | 7 | 60 / 84 / 138 | 29.9 | 6.0 | 1.5 |
| rocket | 4 | 80 / 114 / 214 | 28.6 | 6.9 | 3.3 |
| mg | 9 | 157 / 289 / 487 | 33.3 | 8.8 | 8.3 (bipods, belts) |

Scale [inferred: real lengths from memory, not checked]: about 31 mm per
unit (AKM 880 mm = 28.5, Barrett 1448 mm = 46.2, MP5A3 490 mm = 16.5,
Glock 17 186 mm = 6.5, Uzi folded 470 mm = 13.6). Widths are SLIMMER than
real (Uzi 1.2, a rifle receiver about 1.2 to 2.0, a pistol 0.9 to 1.4):
in first person the gun is seen from behind, so a wide gun looks like a
block.

## 3. Construction rules (Decimation style)

1. **Every part is a small shape box.** Declare 1x1x1 (or 1x1xN for long
   runs) and get the real size from corner offsets: a 1x1x1 with x
   offsets -0.35 on all corners is 0.3 long. Never a plain box. The
   integer size keeps the box UV island tiny (one texel a face).
2. **Many small parts, not few big ones.** An SMG is about 100 parts, a
   rifle 150 to 250, a pistol 50 to 130. Most parts are 0.2 x 0.4 x 0.9.
3. **Receivers are layered panels.** A core block, then separate long
   panels for the top, upper side and lower side, each a little inset or
   proud of the next, with tapered ends (trapezoids from offsets) where the
   receiver meets the barrel or the stock (Uzi side view, split).
4. **Grooves and lines are geometry, not paint:** thin plates about 0.1
   thick sitting 0.05 proud of the surface (Uzi receiver strips), or a
   gap between two panels.
5. **Round things are octagons from 3 parts per slice:** a middle
   rectangle plus a trapezoid above and one below (top face inset 0.2,
   bottom face inset 0.4 in z, or the mirror), together an 8 sided
   section. Barrels, gas tubes, muzzle devices, magazine tubes (AK74
   muzzle: parts 189 to 195). Thin barrels on small guns stay a single
   square bar of about 0.5 x 0.5.
6. **Slopes and curves come from skewed offsets, not rotation.** A grip
   is 2 to 4 stacked segments, each skewed so the front edge leans back;
   a curved magazine is 1 unit tall segments, each a child of the first
   (`addChild`) and shifted forward 0.3 to 0.5 more than the one above
   (MP5 ammoModel0..3; AK74 magazine looks the same [inferred from the render]). Rotation is kept for a few parts
   (folding stocks, revolver cylinders).
7. **Chamfer every visible edge.** Corners are cut with 0.05 to 0.2
   offsets: receiver tops, sight ears (front sight hood as a chamfered
   hexagon), butt plates, magazine base plates.
8. **Thin features stay thin:** trigger guard bars 0.15 to 0.2, trigger
   0.15, sights 0.2 to 0.3 wide, sling loops and pins 0.1.
9. **Parts are named by group then index** (`gunModel0..N`,
   `ammoModel0..N`, `slideModel0..N`); the readable name lives only in our
   spec file.

## 4. Texture rules

- Each part's box UV island gets ONE flat tone with a slight per texel
  noise (2 to 6 brightness steps); no painted panel lines, rings or
  checkering (our MAC-10 painted these on big boxes: the wrong way round).
- Dark, neutral grey metal (median texel, measured): AK74 24,24,24;
  Glock 28,28,28; AR15 Beowulf 32,32,32; Uzi and MP5 41,41,41; Thompson
  64,64,64. Wood: AK74 41,25,22 (dark red brown), Thompson 67,46,26.
  Tan / FDE (AR15 Beowulf 110,95,81) and OD green only on guns that are
  that colour. Tiny bright accents (brass, a few texels) are allowed.
  The magazine is often a slightly different tone from the body (MP5 mag
  lighter, AK74 mag brown polymer).
- Light comes from the renderer, not the texture: neighbouring panels
  read through their slightly different tones and the face shading.
- Gradation (measured 10 Oktober 2026 after the user saw it "if you look
  closely"; Uzi / AK74 / MP5A3 1x1x1 islands): 15 to 31 distinct part
  tones per gun (every part its own shade, Uzi parts 33 to 47), faces of
  one part 4.7 to 5.7 apart, texel noise 2.0 to 2.7 inside a face, on
  some guns the top lighter than the bottom (Glock tone vs height -0.55).
  gunmodel.paint does: part shade +-6 %, +5 top to -5 bottom, faces top
  +2 / bottom -2 / sides +-1.5, texel noise +-4 (MAC-10: 37 tones, faces
  5.2, noise 2.1).
- Layout: textureWidth 512, UV offsets stepping by 8 on a row (a 1x1x1
  island is 4 x 2 texels), new row every 8; the PNG is 2x (1024 wide).

## 5. What was wrong with our MAC-10 (v0.36.0), measured

| | MAC-10 v0.36 | Decimation style |
|---|---|---|
| parts | 23 | an SMG of this size: about 80 to 110 |
| shaped parts | 0 % | 95 to 100 % |
| median part volume | 0.3 (large plain boxes) | 1x1x1 declared, 0.2 x 0.4 x 0.9 real |
| size | 12.0 long, 10.2 high, 2.2 wide | real MAC-10 about 270 mm [not verified] = 8.7 long, width about 1.4 |
| texture | light grey, painted port / grip / rings | dark flat tones per part |
| receiver | one box with a painted port | layered panels, chamfered top, raised strips |
| grip, magazine | straight boxes, mag as wide as the grip | skewed segments, slimmer mag inside the grip |
| first person | a big light grey square (the 2.2 x 2.2 stock plate) fills the view | Uzi: slim dark rear, stock hinge bars, sights |
| animations | Fire kicks the whole Model; no SlideBack | Fire moves only slideModel*; SlideBack in 92 of 98 |
| icon | light grey block filling the frame | dark slim side silhouette, 1 px black outline |

## 6. Workflow for a new gun

1. Pick the real gun; note its real length, height, width; divide by 31
   mm for units. Render 2 or 3 Decimation guns of the same category and
   size (`study.py sheet`) as the target look.
2. Plan the parts per area (receiver panels, barrel slices, sights, grip
   segments, trigger group, magazine segments, stock) on the side view;
   aim for the part count of section 2.
3. Write the spec with shape boxes (gunmodel.py must support corner
   offsets, 1x1x1 declared sizes, children, flat tones: not done yet,
   see docs/roadmap.md).
4. Render ours next to the Decimation reference with `study.py sheet`
   (same scale) and `--split`; compare silhouette, width, part density,
   tone. Iterate before any in game test.
5. First person next to the reference: `python3 tools/devtest.py --live
   gunview guns=uzi,<ours> [attach=reddot,smgSuppressor]` (sections 11,
   13, 14: hip, aim, NPC). Then the full test (`devtest.py --live gun`).
6. Stats from section 16; sounds, loot, magazines, registration:
   docs/gun_model_spec.md section 6.

## 6b. First rebuild with this guide: MAC-10 v2 (v0.37.0)

What worked, keep doing it:
- A real side photo of the gun, measured in pixels against one known
  length (receiver = 7.0 units), fixed every proportion in one pass
  (grip rake, knob and sight positions, magazine length, barrel threads).
- Anchors first (receiver top -4.45, sight tops -4.95 to -5.0, z -0.15):
  the aim view matched the Uzi on the first in game run.
- A one piece rake: `inset("y", 1, x=(-d, 0))` grows the bottom rear
  corners backward (a negative inset), per segment along one line.
- A short gun's rear: frame it (wire loop, butt pad frame) instead of a
  plate; in aim it then reads like Decimation's guns.
- Icons: render on a transparent background (bg alpha 0) and keep only
  solid pixels; a light background leaves a halo.
Numbers: 102 parts (SMG median 105), shape kinds 52 / 44 / 2 / 2 % (taper
/ cuboid / skew / wedge; Decimation 60 / 31 / 5 / 4).

## 6c. Closing the gaps: `study.py gaps` (v0.37.3)

`python3 tools/guns/study.py gaps ours:<gun>` lists every metric of ours
next to the Decimation guns of its category (median, q10 .. q90, marks
what is outside): parts, length / height / width, part sizes (also as %
of the length, for guns shorter or longer than the category), corner
offsets, top two fifths and middle detail shares, shape kinds, tone. The
MAC-10 after v0.37.3 is inside on all of them except length and what
follows from it (absolute part sizes, offsets): a short real gun.
Builder rules learned on the way: declare parts at their rounded size
(long parts 1x1xN; gunmodel.size), raised details about 0.08 proud,
convert plain blocks to bevels / skews / wedges only where the real
part has that shape.

## 7. Shape vocabulary (second pass, all 16757 parts classified)

| Kind | Share | What it is | Used for |
|---|---|---|---|
| taper | 60 % | one end face smaller than the other | almost everything: receiver ends, sight bases, barrel octagon halves, stock panels, grips |
| cuboid | 31 % | a 1x1x1 shrunk to a plain smaller box by offsets | plates, strips, pins, rails |
| skew | 5 % | opposite faces same size but shifted | leaning grips, magazine segments, trigger guard bars |
| wedge | 4 % | two corners merged (a triangle side) | points, ramps, stock toes, sight blades |

So the default part is a TAPER, not a box. Rerun:
`python3 tools/guns/study.py vocab [guns]`; `parts NAME` prints each
part's kind.

## 8. Where the detail goes

Share of parts by position (median over the category):
- along the length (stock to muzzle, tenths): SMG 4 4 7 14 15 20 13 8 9 6;
  rifle 5 3 10 17 15 16 12 10 6 6; pistol 10 21 18 15 9 8 4 3 4 8 %.
  The receiver (30 to 70 % of the length) holds about half of all parts.
- along the height (top to bottom, fifths): SMG 39 35 15 8 2; rifle 35 38
  17 7 2; pistol 46 29 14 5 6 %. About 3 of 4 parts sit in the top two
  fifths (receiver, sights, rails); grips and magazines are few, simple
  parts.
- first person shows the TOP, the RIGHT side and the REAR (section 11):
  spend parts there, the left side and the underside are rarely seen.

## 9. Placement conventions (header and model space)

Every gun has flamePos, ejectPos, rhPos, rhRot, lhPos, lhRot (no sPos, no
mOff, no Scale in any shipped gun). Medians:

| | pistol | smg | rifle |
|---|---|---|---|
| model x range (rear, muzzle) | 1.2, 9.0 | -5.4, 14.7 | -7.5, 19.4 |
| model y range (top, bottom) | -4.8, 0.6 | -5.0, 2.4 | -4.9, 2.5 |
| model z centre | -0.15 | -0.12 | -0.25 |
| rhPos | -6.35, 1.07, -2.0 | -6.55, 1.92, -2.0 | -7.2, 2.07, -2.0 |
| lhPos | -1.55, 9.12, 5.32 | 4.2, 8.02, 4.52 | 3.7, 8.22, 4.52 |

- The top of the gun is at y -5 (y grows downward; sight line rules in
  section 13), the gun is centred on
  z -0.15; flamePos x = the muzzle tip minus 0.1, flamePos y about 10 % of
  the height below the top, flamePos z -0.15. rhRot / lhRot are always 0.
- The grip sits at roughly x 3 to 6 on SMGs and pistols [inferred from
  the renders]; start rhPos / lhPos from the closest Decimation gun of the
  same layout (our MAC-10 took the Uzi's; in first person it sits where the
  Uzi sits, hands on it [only the hip view checked]), then tune in game.
- ejectPos is 0,0,0 on many guns (no casing spot set).
- flamePos y is NOT the bore: it sits about 0.85 above it (Uzi flamePos
  -4.5 over a barrel centred at -3.5; UMP45 -3.75 over about -2.95). Barrel
  attachments are hung from flamePos, so they land on the bore only with
  this convention (MAC-10 v0.37.1 had flamePos on the bore and its
  suppressor 0.9 too low in the user's game).

## 10. Animations

All 98 guns have Fire, Rack and Reload1; 92 also SlideBack. Timings are
shared templates (the Uzi and MP5 Rack are identical):

| Animation | Frames | Keyframes | Hand | Content |
|---|---|---|---|---|
| Fire | 2 (shotgun 11) | 2 | 0 | frame 1 RAND: only slideModel* back about 2.4; the whole Model does NOT kick (recoil is the game's) |
| Reload1 | 57 (pistol 46) | about 12, every 5 frames | 0 | mag out with MagOut sound and shake 1.2 at frame 5, SWITCH at 20 (mag far down, y +17), new mag up, LOAD with MagIn at 40, TRYBOLT at 50, rest at 55; Model tilts up to about 25 to 30 degrees, OffHand follows the mag |
| Rack | 19 (pistol 11) | 7, every 3 frames | 1 (rifles, SMGs), 0 (pistols) | starts with the slide back, sound Rack at frame 6 with the slide home, OffHand pulls and returns |
| SlideBack | 1, STATIC | 1 | 0 | the slide held back (empty gun) |

Our guns: use the same timing and structure with our own values; add
SlideBack; no Model kick in Fire.

## 11. First person (in game, dev test mode `gunview`)

Live mode trap: a `key=value` given to `devtest.py --live` stays set for
later runs in the same game (System properties); clear it with `key=`
(for example `attach=`).

`python3 tools/devtest.py --live gunview [guns=uzi,mac10,...]`
(devtest/GunViewTest, test arena, noon, same camera; one screenshot per
gun, gunview_<gun>.png). Shots of uzi, mp5a3, ump45, vector, mp7,
glock17, deagle, ak74, m4a4, r870 and our mac10: docs/shots/
guns_study_v0.36/fp/ (shots_index.md). What it shows:
- The gun comes in from the lower right toward the screen centre; the
  player sees the REAR face, the TOP and the RIGHT side, at a steep angle.
  The sights sit just below the crosshair.
- Long guns show a long diagonal of the right side; short guns (Uzi,
  pistols) are seen almost straight from behind: their rear cross section
  IS the gun. The Uzi's rear is slim (1.2 wide), dark, broken up by the
  folded stock bars, hinge and rear sight.
- Our MAC-10's rear is a 2.2 x 2.2 light grey plate: one flat square
  filling the view. Rule: a short gun's rear must be slim, dark and
  broken into parts (chamfers, stock rods, rear sight, cocking handle).

## 12. Icons (items/gun/<cat>/<gun>.png, all 32 x 32)

Side view, muzzle right, horizontal, the gun's own dark tones, a 1 px
black outline, slim (rifles are a thin line with a few pixels of
receiver). Ours was a light grey block filling the frame. Make icons from
the model: study.py side render scaled to 30 px wide plus a black outline
[to build into gunmodel.py].

## 13. Aiming (first person, aim mode)

Read from GunItemRenderer (deobf/src/decimation/render/GunItemRenderer.java)
and checked in game (`gunview`, shot gunview_<gun>_aim.png):
- Aim mode (PlayerData aimMode 1, toggled by the right mouse button in
  GunItem's client update; `Deci.setAimMode` sets it) draws EVERY gun at
  one fixed place: x scaled 0.5, translate (-1, -0.35, 0.923); scopes
  (not the red dot) add 0.03 up, dragunov scope (0.2, 0.05), guns with an
  integrated scope -1.3 in x. No per gun value: `sPos` is used only for
  the third person (EQUIPPED) offset, and no shipped gun sets it.
- So the sight line is a fixed MODEL height: the screen centre in aim is
  about y -4.85 (red dot glass centre, y -5.1 to -4.55) to -5.2 (2x / 4x
  glass), z -0.15. Decimation's guns keep the receiver top at y -4.92
  (median over rifles / SMGs / MGs / shotguns, range -5.05 to -4.8) over
  x 1.5 to 5, and iron sights peak at about y -5.0.
- Seen in game: M4A4 and Uzi with a red dot have the ring at the screen
  centre. Iron sights alone (fp_aim_iron.png, red lines = screen
  centre): the centre sits exactly at the TOP of the sights: Uzi (top
  -5.0), MP5A3 (-4.95), AK74 (-4.85) touch the line, Glock (-4.75) a hair
  below. Our MAC-10's sight ears (top -5.75) stick out about 0.75 units
  (40 px at 854 x 480, so about 53 px a unit) above it and its 2.2 x 2.2
  rear plate fills the lower half: in aim a short gun is only its rear
  section.
- Aim sway: ClientEventHandler tilts the gun while the mouse moves
  (headYawSway from yaw, dP from pitch changes; applied in aim too), so
  a screenshot right after looking up or down shows the sights off the
  centre for a moment (the user's 0.37.0 shot: about 0.2 units) for any
  gun [inferred]; judge the aim only steady (gunview holds the camera).
- CORRECTED v0.37.2 (user's screenshots, Uzi and MAC-10 aiming in the
  same game): the aim centre passes through the Uzi's rear aperture hole
  (about y -4.65; its post tip -4.52, rear ears -4.97), not the sight
  tops. The held camera gunview shot first suggested the tops; judge
  only by comparing with a Decimation gun in the same shot.
- Rules: rear aperture hole centre about y -4.65 to -4.68 and front post
  tip at the same height on z -0.15, ears up to about -4.97; receiver top
  in the sight zone about -4.45; nothing behind the rear sight above
  about -4.57 (it crosses the sight picture); the rear section below the
  sights at most about 1.2 wide and dark.

## 14. Attachments

Code: AttachmentItem, FilteredSlot (attachment screen), GunItemRenderer
.renderAttachments; stored in the gun's NBT as `sightAttach`,
`barrelAttach`, `gripAttach` (also `stockAttach`, `skin`,
`tracerColorID`), looked up as deci:<name>.

| Attachment | Slot | Category | Notes |
|---|---|---|---|
| reddot | sight | all | zoom 42.5, sway 0.4 |
| 2x, 4x, 8x | sight | all | zoom 42.5 / 45 / 46.5 |
| dragunovScope | sight | all | zoom 45, own offset |
| foregrip | grip | rifle | first person kick rotation x0.5 instead of x2, shift /10 instead of /8 |
| flashlight, laser | grip | all | |
| bayonet | barrel | rifle | own offset |
| pistolSuppressor, smgSuppressor, arSuppressor, shotgunSuppressor, mgSuppressor | barrel | pistol / smg / rifle / shotgun / mg | |

- A gun accepts an attachment when the categories match or the
  attachment is `all`; no suppressor on an integrally suppressed gun, no
  scope on one with an integrated scope; a skin only for its own gun.
  A new SMG therefore takes smgSuppressor, reddot, 2x, 4x, 8x, dragunov
  scope, flashlight and laser with no extra code.
- Placement (model units = GL / 0.0625, in the gun's own model space):
  sight (0.8, 1.12, -0.13) plus red dot (-5.79, 0.10, -0.03) or dragunov
  (-1.6, -0.8, -0.06); grip (-3.2, 0.8, 0); barrel (-24.8, 4.32, -0.05) +
  flamePos x 16 / 21 (mp7 and bayonet have extra offsets). The attachment
  models are built around these spots, so the only per gun input is
  flamePos, but only at 16 / 21 of it: barrel attachment x = constant +
  0.762 flamePos x, so it meets the muzzle only around x 12 to 15
  (study.py gaps: Uzi 0.12, UMP45 0, MP5A3 / Vector 0.44, MP7 -0.56 with
  its hardcoded fix, our MAC-10 v2 1.12: "flying" in the user's
  screenshot). Fix for a short gun: `Deci.offsetAttachment(gun,
  "smgSuppressor", dx, dy, dz)` puts our own copy of the attachment model
  in AttachmentItem.ST for that gun with a BModel offset (MAC-10: -1.02,
  -0.16, 0 after v0.37.2: over the threads up to the receiver front).
  Mirror it in study.py ATTACH_FIX. study.py's attachment heights are
  right (an earlier note here called them 0.85 too low: that was the
  flamePos convention above, wrongly read as a render error). Sights land at x 0.8 to 5.5 (scope) or 2.4 to 2.8 (red
  dot) over y -5.9 to -3.1: a new gun needs its receiver top there (section
  13). The foregrip renders behind and below even Decimation's M4A4
  (study render and the NPC shot) [inferred: misplaced in the game too].
- Render: `python3 tools/guns/study.py attach GUN reddot smgSuppressor ...`
  draws a gun with attachments as the game places them; in game:
  `devtest.py --live gunview guns=m4a4,mac10 attach=reddot,smgSuppressor`
  (hip, aim and an NPC holding it).

## 15. Walkthrough: the Uzi, 105 parts by area

Positions are part centres in model units (x forward, y down); `study.py
parts uzi` prints the full list with kinds.

| Area | x | Parts | How |
|---|---|---|---|
| folded stock | 1.3 to 2.4 | 12 | a butt bar 0.4 x 2.55 x 0.6, tapered end caps, thin side struts 0.1 thick |
| rear sight | 2.5 to 2.7 | about 25 | an aperture ring from 0.1 bits, two protective ears 1.0 x 0.35 x 0.1 as a mirrored pair, a base plate 1.15 x 0.15 x 0.8 |
| receiver | 2 to 10 | about 20 | core 7.8 x 1.0 x 0.8; top cover 2.65 x 0.2 x 1.0 in two pieces; side lips 0.25 thick (tapers) left and right; lower side plates 3.3 x 0.65 x 0.2 |
| grip | 5.4 to 6.3 | 9 | 3 rows (y -2.4, -1.6, -0.5) of a front and a back taper 0.9 wide, an inner core |
| trigger group | 6.8 to 8.3 | 8 | guard bars 0.1 thick, trigger 0.2 x 0.6 tapers |
| front sight | 9.5 to 9.9 | 8 | post 0.3 x 1.05 x 0.3, two hood ears (mirrored tapers) |
| barrel nut, front end | 9.9 to 11.8 | 10 | stepped blocks 1.1 wide tapering forward |
| barrel | 12.7 to 14.7 | 1 | 2.0 x 0.4 x 0.4 bar |
| magazine | 6.2 | 2 | ammoModel0 1.0 x 5.1 x 0.5 inside the grip, base plate 1.1 x 0.1 x 0.6 |

Lessons: side details come in mirrored pairs about z -0.15 (z +0.2 and
-0.5); the most parts go where the eye goes in first person (rear sight
25, receiver 20); the barrel can be a single bar; the magazine is simple.

## 16. Stats and balance

All 98 registrations (deobf ItemRegistry) in
`docs/references/decimation_gun_stats.tsv`: damage, rpm, recoil pitch /
yaw, recoil recovery, movement slowdown, fire modes, mode parameters,
aim table, magazines, other builder calls. GunStats(recoil {pitch, yaw},
fire modes, mode params, aim table, recovery, rpm); secondsPerShot =
60 / (rpm x 1.3). Medians per category:

| Category | Damage (min / median / max) | rpm | Recoil pitch / yaw | Recovery | Slowdown |
|---|---|---|---|---|---|
| pistol | 8 / 9 / 14 | 500 | 13 / 1.0 | 5 | 0.03 |
| revolver | 11 / 11.5 / 12 | 325 | 9 / 1.5 | 8 | 0.045 |
| smg | 9 / 11 / 15 | 700 | 6 / 0.4 | 8 | 0.12 |
| rifle | 10 / 16 / 50 | 625 | 6 / 0.4 | 3 | 0.13 |
| shotgun | 6 / 10 / 11 | 300 | 13 / 1.0 | 15 | 0.14 |
| mg | 10 / 19 / 21 | 650 | 21 / 0.6 | 5 | 0.29 |
| rocket | 75 / 87.5 / 100 | 60 | 40 / 0.2 | 8 | 0.17 |

Fire modes: AUTO/SINGLE 54, SINGLE 37, PUMP 3, AUTO 3. The aim table is
the same {{16, 1.5}, {14, 2, 50}, {9, 4, 100}} on most guns (fast guns
like vector / mp7 use {12, 7, 100} last) [meaning not decoded].
Examples: Uzi 11 dmg 600 rpm 5 / 0.35; MP5A3 12 / 700; UMP45 13 / 600;
Vector 10 / 1200; MP7 10 / 1000; our MAC-10 12 / 1100 / 5.5 / 0.45:
inside the SMG band. A new gun: copy the closest real equivalent's
numbers, then move one or two values with a reason.
