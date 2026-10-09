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
   gunview guns=uzi,<ours>` (section 11). Then the full test
   (`devtest.py --live gun`).

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

- The top of the gun is at y -5 (y grows downward), the gun is centred on
  z -0.15; flamePos x = the muzzle tip minus 0.1, flamePos y about 10 % of
  the height below the top, flamePos z -0.15. rhRot / lhRot are always 0.
- The grip sits at roughly x 3 to 6 on SMGs and pistols [inferred from
  the renders]; start rhPos / lhPos from the closest Decimation gun of the
  same layout (our MAC-10 took the Uzi's; in first person it sits where the
  Uzi sits, hands on it [only the hip view checked]), then tune in game.
- ejectPos is 0,0,0 on many guns (no casing spot set).

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
