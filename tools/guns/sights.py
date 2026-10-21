#!/usr/bin/env python3
"""Our sight attachments (user request 10 Oktober 2026: "make new sight model, holographic sight
... we use 558 model holographic scope ... and acog 3.5x sight ... please be so detailed on the
model, i want you make its looks same like in real life"). Our own models, textures, icons and
reticles (nothing of Decimation's): EOTech 558 (user photos 60 to 67) and Trijicon ACOG TA11
3.5x35 with fibre optic (photos 52, 55, 58; photo 53 is a battery RCO, not used; reticle photo
59). Registered by fixes/NewSights as deci:eotech558 / deci:ta11acog.

Placement: Decimation draws a sight attachment at its file coordinates plus (0.8, 1.12, -0.128)
model units (GunItemRenderer.renderAttachments), on a gun whose receiver top is about y -4.45
(lower rails get fixes/SightPlacement). Parts are written here in GUN space (rail top -4.45,
centre z -0.15) and shifted into the file at the end.
Scale: Decimation draws guns slimmer than real; its 4x is 4.7 long, 1.35 wide, glass about 0.8
across. Real sizes x 0.52: EOTech 558 5.6 x 2.3 x 2.9 inch -> about 2.9 x 1.0 x 1.4 (window
1.2 x 0.85 inch -> 0.62 x 0.45); ACOG TA11 5.8 inch long, 35 mm objective -> 3.0 long, bell 1.05.
Glass parts are named scopeGlass* (see through: PatchScope / ScopeZoom), the reticle is drawn in
the glass by ScopeZoom from textures/model/guns/scopes/<name>.png.

    python3 tools/guns/sights.py        # writes models, textures, icons, reticles under assets/deci
"""
import math
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import ZC, layout, octagon, paint, part, part_block  # noqa: E402
import study  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deci")
SHIFT = (0.8, 1.12, -0.128)   # renderAttachments' sight translate in model units
RAIL = -4.45                  # receiver top the sights are placed for
HEADER = """
  flamePos: 13.89998, -4.200001, 0;
  ejectPos: 6.499998, -5.199995, -0.6999996;
  rhPos: -5.899982, 0.62, -2;
  rhRot: 0, 0, 0;
  lhPos: 6.499999, 6.719998, 4.22;
  lhRot: 0, 0, 0;"""        # the red dot's header (attachments carry the gun header fields, unused)
S = "sightModel"
GLASS = "scopeGlass"
RETICLE = {}                  # name -> (x, y, z, size) in gun space: the reticle plane in the glass


def oct_z(name, group, colour, cx, cy, z0, z1, r, flat=0.42):
    """A round bar along z (knobs, caps, the EOTech battery tube): 3 parts, 8 sided."""
    k = r * flat
    mid = part(name + "Mid", group, colour, (cx - r, cy - k, z0), (cx + r, cy + k, z1))
    top = part(name + "Top", group, colour, (cx - r, cy - r, z0), (cx + r, cy - k, z1)).inset("y", 0, x=(r - k, r - k))
    bot = part(name + "Bot", group, colour, (cx - r, cy + k, z0), (cx + r, cy + r, z1)).inset("y", 1, x=(r - k, r - k))
    return [mid, top, bot]


def oct_y(name, group, colour, cx, cz, y0, y1, r, flat=0.42):
    """A round bar along y (turrets): 3 parts, 8 sided."""
    k = r * flat
    mid = part(name + "Mid", group, colour, (cx - r, y0, cz - k), (cx + r, y1, cz + k))
    a = part(name + "A", group, colour, (cx - r, y0, cz + k), (cx + r, y1, cz + r)).inset("z", 1, x=(r - k, r - k))
    b = part(name + "B", group, colour, (cx - r, y0, cz - r), (cx + r, y1, cz - k)).inset("z", 0, x=(r - k, r - k))
    return [mid, a, b]


def ring_ribs(name, group, colour, cx, cy, z0, z1, r, n=12, size=0.045):
    """Knurling: small ridges around the rim of a disc along z."""
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        out.append(part(name, group, colour, (x - size / 2, y - size / 2, z0), (x + size / 2, y + size / 2, z1)))
    return out


def ring_ribs_y(name, group, colour, cx, cz, y0, y1, r, n=12, size=0.045):
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n
        x, z = cx + math.cos(a) * r, cz + math.sin(a) * r
        out.append(part(name, group, colour, (x - size / 2, y0, z - size / 2), (x + size / 2, y1, z + size / 2)))
    return out


# ================================================================ EOTech 558
def eotech():
    P = []

    def add(*parts, pair=False):
        for p in parts:
            P.append(p)
            if pair:
                P.append(p.mirror())

    BODY = (38, 38, 40)
    HOOD = (35, 35, 37)
    EDGE = (44, 44, 46)
    DARK = (20, 20, 22)
    SCREW = (60, 60, 62)
    LABEL = (214, 182, 42)
    LENS = (34, 46, 54)
    RUB = (26, 26, 28)
    # 558 proportions (photos 64, 66): a compact hood about as long as it is tall, its window
    # filling most of the rear face (photos 62, 67), the battery stub low in front
    X0, X1, XB = 2.45, 4.1, 4.58         # rear face, hood front, battery front
    ZL, ZR = ZC + 0.5, ZC - 0.5          # left (+z) and right (-z) sides
    WT, WB = -5.62, -5.08                # window top / bottom
    WZ0, WZ1 = ZC - 0.35, ZC + 0.35      # window sides: walls 0.15
    TOP = -5.84

    # --- QD mount on the rail: base plate, clamp jaws hanging beside the rail, throw lever (left)
    add(part("base", S, BODY, (X0 + 0.06, -4.58, ZR + 0.06), (XB - 0.1, RAIL, ZL - 0.06)).inset("y", 0, z=(0.04, 0.04)))
    add(part("jaw", S, BODY, (2.95, -4.56, ZL - 0.08), (3.95, RAIL + 0.12, ZL + 0.02)).inset("y", 1, x=(0.05, 0.05)), pair=True)
    add(part("lug", S, DARK, (3.35, RAIL - 0.02, ZC - 0.12), (3.53, RAIL + 0.08, ZC + 0.12)))
    add(part("leverPivot", S, BODY, (3.95, -4.66, ZL - 0.02), (4.25, RAIL, ZL + 0.1)).inset("z", 1, x=(0.04, 0.04), y=(0.03, 0.03)))
    lever = part("lever", S, BODY, (2.9, -4.62, ZL + 0.04), (4.1, -4.5, ZL + 0.1))
    add(lever.inset("x", 0, y=(0.02, 0.02)))
    add(part("leverTab", S, DARK, (2.8, -4.68, ZL + 0.02), (3.0, -4.47, ZL + 0.13)).inset("x", 0, y=(0.04, 0.04)))
    for bx in (3.05, 3.65):             # clamp screws, right side
        add(*oct_z("clampScrew", S, SCREW, bx, -4.52, ZR - 0.04, ZR + 0.02, 0.055))
        add(part("clampSlot", S, DARK, (bx - 0.05, -4.535, ZR - 0.05), (bx + 0.05, -4.505, ZR - 0.03)))

    # --- housing: lower body, hood (top and sides) around the window tunnel, rounded top corners
    add(part("lowerBody", S, BODY, (X0, WB, ZR), (X1, -4.58, ZL)).inset("x", 1, z=(0.03, 0.03)))
    add(part("hoodTop", S, HOOD, (X0, TOP, ZR), (X1, WT, ZL)).inset("y", 0, z=(0.08, 0.08))
        .inset("x", 0, y=(0.14, 0.0)).inset("x", 1, y=(0.06, 0.0)))
    add(part("hoodSide", S, HOOD, (X0, WT, WZ1), (X1, WB, ZL)), pair=True)
    # hood rims, a little proud, rear and front (photos 62, 66: the frame around the window)
    for x0, x1 in ((X0 - 0.06, X0), (X1, X1 + 0.06)):
        add(part("rimTop", S, EDGE, (x0, WT - 0.07, WZ0 - 0.07), (x1, WT, WZ1 + 0.07)))
        add(part("rimBottom", S, EDGE, (x0, WB, WZ0 - 0.07), (x1, WB + 0.06, WZ1 + 0.07)))
        add(part("rimSide", S, EDGE, (x0, WT, WZ1), (x1, WB, WZ1 + 0.07)), pair=True)
    # window: see through glass at the rear (the reticle sits in it), tinted lens at the front
    add(part("glass", GLASS, LENS, (X0 + 0.12, WT, WZ0), (X0 + 0.14, WB, WZ1)))
    # reticle on the glass's eye side; the 68 MOA ring about a fifth of the window height
    # (user v0.41.0: "too big ... like 20 now, make it 5"; a quarter came out a few pixels, a third reads)
    RETICLE["eotech558"] = (X0 + 0.115, (WT + WB) / 2, ZC, 0.18)
    add(part("frontLens", S, LENS, (X1 - 0.12, WT, WZ0), (X1 - 0.1, WB, WZ1)))
    add(part("lensEdge", S, DARK, (X1 - 0.1, WT, WZ0), (X1 - 0.02, WT + 0.04, WZ1)))

    # --- rear panel under the window: raised plate, logo plate, 4 screws (photos 62, 65, 67)
    add(part("rearPanel", S, EDGE, (X0 - 0.04, WB + 0.05, ZR + 0.06), (X0, -4.62, ZL - 0.06)).inset("x", 0, y=(0.03, 0.03), z=(0.03, 0.03)))
    add(part("logoPlate", S, DARK, (X0 - 0.07, -4.98, ZC - 0.17), (X0 - 0.04, -4.72, ZC + 0.17)).inset("x", 0, y=(0.02, 0.02), z=(0.02, 0.02)))
    for sy in (-4.98, -4.72):
        for sz in (ZC - 0.36, ZC + 0.36):
            add(part("rearScrew", S, SCREW, (X0 - 0.07, sy - 0.04, sz - 0.04), (X0 - 0.04, sy + 0.04, sz + 0.04)).inset("x", 0, y=(0.015, 0.015), z=(0.015, 0.015)))

    # --- left side (+z, photo 66): laser label, NV button, down / up buttons, two screws
    add(part("label", S, LABEL, (2.75, -5.5, ZL - 0.01), (3.35, -5.28, ZL + 0.015)))
    add(part("labelEdge", S, DARK, (2.75, -5.5, ZL + 0.01), (3.35, -5.48, ZL + 0.02)))
    add(*oct_z("nvButton", S, DARK, 3.72, -5.38, ZL - 0.01, ZL + 0.04, 0.085))
    for bx in (3.2, 3.48):
        add(part("button", S, DARK, (bx, -5.0, ZL - 0.01), (bx + 0.22, -4.8, ZL + 0.05)).inset("z", 1, x=(0.03, 0.03), y=(0.03, 0.03)))
        add(part("buttonArrow", S, EDGE, (bx + 0.08, -4.94, ZL + 0.05), (bx + 0.14, -4.86, ZL + 0.06)))
    for bx in (2.7, 3.0):
        add(*oct_z("sideScrew", S, SCREW, bx, -4.7, ZL - 0.01, ZL + 0.03, 0.05))

    # --- right side (-z, photo 65): elevation and windage adjusters (recessed dials, slot)
    for bx in (2.92, 3.38):
        add(*oct_z("dialRing", S, EDGE, bx, -4.92, ZR - 0.035, ZR + 0.02, 0.15))
        add(*oct_z("dial", S, DARK, bx, -4.92, ZR - 0.05, ZR - 0.02, 0.1))
        add(part("dialSlot", S, SCREW, (bx - 0.07, -4.935, ZR - 0.06), (bx + 0.07, -4.905, ZR - 0.045)))

    # --- battery compartment, front low: transverse tube, knurled cap on the right with "+", tether
    bx = XB - 0.25
    add(part("battBlock", S, BODY, (X1 - 0.02, WB + 0.02, ZR + 0.02), (bx, -4.58, ZL - 0.02)))
    add(*oct_z("battTube", S, BODY, bx, -4.84, ZR + 0.02, ZL - 0.02, 0.25))
    add(*oct_z("battCap", S, RUB, bx, -4.84, ZR - 0.15, ZR + 0.02, 0.27))
    add(*ring_ribs("capKnurl", S, DARK, bx, -4.84, ZR - 0.15, ZR - 0.02, 0.27, n=14, size=0.05))
    add(part("capPlusH", S, EDGE, (bx - 0.08, -4.855, ZR - 0.165), (bx + 0.08, -4.825, ZR - 0.145)))
    add(part("capPlusV", S, EDGE, (bx - 0.015, -4.92, ZR - 0.165), (bx + 0.015, -4.76, ZR - 0.145)))
    for (x0, y0), (x1, y1) in (((bx - 0.14, -5.12), (bx - 0.08, -5.0)), ((bx - 0.12, -5.22), (bx + 0.08, -5.16)),
                               ((bx + 0.04, -5.16), (bx + 0.1, -5.05))):
        add(part("tether", S, DARK, (x0, y0, ZR - 0.11), (x1, y1, ZR - 0.07)))   # the cap's wire loop
    add(part("tetherEnd", S, LABEL, (bx - 0.15, -5.02, ZR - 0.12), (bx - 0.07, -4.97, ZR - 0.06)))
    return P


# ================================================================ ACOG TA11 3.5x35
def acog():
    P = []

    def add(*parts, pair=False):
        for p in parts:
            P.append(p)
            if pair:
                P.append(p.mirror())

    BODY = (40, 40, 42)
    DARK = (20, 20, 22)
    EDGE = (48, 48, 50)
    RUB = (24, 24, 26)
    SCREW = (62, 62, 64)
    FIBRE = (110, 205, 64)
    LENS = (58, 44, 36)        # objective coating, amber brown (photos 52, 56)
    AC = -5.22                 # optical axis
    XR = 1.6                   # eyepiece rear

    # --- eyepiece: rubber ring, tube, lens (see through glass), step to the body
    add(*octagon("eyeRubber", S, RUB, XR, XR + 0.14, AC, ZC, 0.33))
    add(*octagon("eyeTube", S, BODY, XR + 0.14, 2.35, AC, ZC, 0.29))
    add(*octagon("eyeRing", S, EDGE, 2.0, 2.06, AC, ZC, 0.31))
    add(part("glass", GLASS, (30, 36, 40), (XR - 0.02, AC - 0.24, ZC - 0.24), (XR, AC + 0.24, ZC + 0.24)))
    RETICLE["ta11acog"] = (XR - 0.025, AC, ZC, 0.48)    # the reticle fills the eyepiece
    # the eyepiece opening is round: wedges nearer the eye mask the square glass's corners
    for sy in (-1, 1):
        for sz in (-1, 1):
            w = part("eyeMask", S, RUB, (XR - 0.04, AC - 0.25, ZC - 0.25), (XR - 0.025, AC - 0.12, ZC - 0.12))
            # a triangle in the corner: collapse the inner corner onto the two outer edges
            w.c[(0, 1, 1)][1] = w.c[(1, 1, 1)][1] = AC - 0.25
            w = w if (sy, sz) == (-1, -1) else w
            if sy > 0:
                for k, q in w.c.items():
                    q[1] = 2 * AC - q[1]
            if sz > 0:
                for k, q in w.c.items():
                    q[2] = 2 * ZC - q[2]
            add(w)
    add(*octagon("eyeStep", S, BODY, 2.35, 2.6, AC, ZC, 0.38))
    add(part("lampHousing", S, BODY, (2.32, AC - 0.5, ZC - 0.13), (2.62, AC - 0.36, ZC + 0.13)).inset("y", 0, x=(0.03, 0.03), z=(0.03, 0.03)))
    add(part("lampCap", S, EDGE, (2.4, AC - 0.54, ZC - 0.07), (2.54, AC - 0.5, ZC + 0.07)))

    # --- body: forged housing, top rising toward the front (photo 52), side bosses, top shoulders
    body = octagon("body", S, BODY, 2.6, 4.05, AC, ZC, 0.44)
    top = body[1]
    for zi in (0, 1):
        top.c[(1, 0, zi)][1] -= 0.12      # the top climbs toward the objective
    add(*body)
    add(part("sideBoss", S, EDGE, (2.85, AC - 0.24, ZC + 0.42), (3.95, AC + 0.2, ZC + 0.48))
        .inset("z", 1, x=(0.08, 0.08), y=(0.06, 0.06)), pair=True)
    sh = part("shoulder", S, BODY, (3.3, AC - 0.6, ZC + 0.24), (4.6, AC - 0.44, ZC + 0.42))
    sh.c[(0, 0, 0)][1] = sh.c[(0, 0, 1)][1] = AC - 0.5   # low at the rear: the ridge rises forward
    add(sh.inset("z", 1, y=(0.04, 0.0)), pair=True)

    # --- objective bell and lens
    add(*octagon("bell", S, BODY, 4.05, 4.62, AC, ZC, 0.53))
    add(*octagon("bellLip", S, EDGE, 4.56, 4.66, AC, ZC, 0.55))
    add(*octagon("objRim", S, DARK, 4.66, 4.665, AC, ZC, 0.46))
    add(*octagon("objective", S, LENS, 4.665, 4.675, AC, ZC, 0.38))       # round, amber coated

    # --- fibre optic on top: dark channel, green fibre, clear front cap (photos 52, 55, 58)
    fy0, fy1 = AC - 0.47, AC - 0.6         # top surface at the rear / front of the channel
    ch = part("fibreChannel", S, DARK, (3.1, fy0 - 0.05, ZC - 0.08), (4.55, fy0 + 0.02, ZC + 0.08))
    fb = part("fibre", S, FIBRE, (3.1, fy0 - 0.08, ZC - 0.04), (4.6, fy0 - 0.03, ZC + 0.04))
    for p, d in ((ch, 0.0), (fb, 0.0)):
        for yi in (0, 1):
            for zi in (0, 1):
                p.c[(1, yi, zi)][1] += fy1 - fy0   # follow the rising top
    add(ch, fb)
    add(part("fibreCap", S, (150, 170, 150), (4.55, fy1 - 0.09, ZC - 0.06), (4.66, fy1 - 0.01, ZC + 0.06)))
    add(part("fibreClip", S, EDGE, (3.0, fy0 - 0.1, ZC - 0.1), (3.12, fy0 + 0.0, ZC + 0.1)))

    # --- turrets: elevation on top, windage on the right, knurled caps
    add(*oct_y("elevBase", S, BODY, 2.85, ZC, AC - 0.6, AC - 0.4, 0.17))
    add(*oct_y("elevCap", S, RUB, 2.85, ZC, AC - 0.74, AC - 0.6, 0.19))
    add(*ring_ribs_y("elevKnurl", S, DARK, 2.85, ZC, AC - 0.72, AC - 0.6, 0.19, n=12, size=0.045))
    add(part("elevTop", S, EDGE, (2.75, AC - 0.75, ZC - 0.1), (2.95, AC - 0.74, ZC + 0.1)))
    add(*oct_z("windBase", S, BODY, 2.85, AC, ZC - 0.6, ZC - 0.4, 0.17))
    add(*oct_z("windCap", S, RUB, 2.85, AC, ZC - 0.74, ZC - 0.6, 0.19))
    add(*ring_ribs("windKnurl", S, DARK, 2.85, AC, ZC - 0.72, ZC - 0.6, 0.19, n=12, size=0.045))
    add(part("windTop", S, EDGE, (2.75, AC - 0.1, ZC - 0.75), (2.95, AC + 0.1, ZC - 0.74)))

    # --- TA51 flat top mount: block under the body, rail jaws, cross bolts (left), thumb nuts (right)
    add(part("mountBlock", S, BODY, (2.9, AC + 0.38, ZC - 0.4), (4.4, RAIL, ZC + 0.4)).inset("y", 0, z=(0.1, 0.1))
        .inset("x", 0, y=(0.1, 0.0)).inset("x", 1, y=(0.1, 0.0)))
    add(part("mountJaw", S, BODY, (3.0, -4.55, ZC + 0.36), (4.3, RAIL + 0.12, ZC + 0.44)).inset("y", 1, x=(0.05, 0.05)), pair=True)
    for bx in (3.25, 4.05):
        add(*oct_z("crossBolt", S, SCREW, bx, -4.62, ZC + 0.38, ZC + 0.44, 0.06))
        add(*oct_z("thumbNut", S, DARK, bx, -4.58, ZC - 0.62, ZC - 0.4, 0.15))
        add(part("thumbSlot", S, EDGE, (bx - 0.11, -4.6, ZC - 0.63), (bx + 0.11, -4.56, ZC - 0.61)))
        add(*ring_ribs("thumbKnurl", S, RUB, bx, -4.58, ZC - 0.6, ZC - 0.42, 0.15, n=10, size=0.04))
    return P


# ================================================================ ELCAN SpecterDR 1x/4x
def specterdr():
    """ELCAN SpecterDR 1x/4x on its quick detach mount, after the user's tarkovbot build (docs/shots/refcapture_v0.65.0/
    my_preset_close: side.png, top.png; REFERENCE ONLY). Real about 7.7 x 2.3 x 2.6 inch over the rail [not verified],
    x 0.52 -> 4.0 long, 1.15 wide; the reference's side gives the rest by ratio to that length: body 0.97 tall (axis
    0.96 over the rail), mount 0.47. Muzzle side (+x): a squared front housing round a big objective; middle: the body
    with the 1x / 4x throw lever on the left (+z, the side the reference shows) and the knurled illumination knob with
    its tethered cap; rear: a round eyepiece; two small guard ears on top; the mount below with two levers on the
    left, cross bolts on the right and the battery cap at its front left."""
    P = []

    def add(*parts, pair=False):
        for p in parts:
            P.append(p)
            if pair:
                P.append(p.mirror())

    BODY = (38, 38, 40)
    DARK = (20, 20, 22)
    EDGE = (50, 50, 52)
    RUB = (25, 25, 27)
    SCREW = (62, 62, 64)
    MARK = (150, 150, 146)          # the 4X / 1X marks: small light plates (no painted detail)
    LENS = (40, 52, 58)             # objective coating, blue grey
    AC = RAIL - 0.96                # optical axis
    XR, XF = 1.2, 5.2               # eyepiece rear, objective front
    H, W = 0.485, 0.575             # body half height / half width

    # --- eyepiece: rubber, tube, ring, the see through glass and its round opening (corner wedges, as the ACOG)
    add(*octagon("eyeRubber", S, RUB, XR, XR + 0.14, AC, ZC, 0.5))
    add(*octagon("eyeTube", S, BODY, XR + 0.14, 2.0, AC, ZC, 0.46))
    add(*octagon("eyeRing", S, EDGE, 1.78, 1.86, AC, ZC, 0.48))
    add(part("glass", GLASS, (30, 36, 40), (XR - 0.02, AC - 0.34, ZC - 0.34), (XR, AC + 0.34, ZC + 0.34)))
    RETICLE["specterdr"] = (XR - 0.025, AC, ZC, 0.68)    # the reticle fills the eyepiece
    for sy in (-1, 1):
        for sz in (-1, 1):
            w = part("eyeMask", S, RUB, (XR - 0.04, AC - 0.35, ZC - 0.35), (XR - 0.025, AC - 0.17, ZC - 0.17))
            w.c[(0, 1, 1)][1] = w.c[(1, 1, 1)][1] = AC - 0.35
            if sy > 0:
                for k, q in w.c.items():
                    q[1] = 2 * AC - q[1]
            if sz > 0:
                for k, q in w.c.items():
                    q[2] = 2 * ZC - q[2]
            add(w)

    # --- body: rear block, the main body, the squared front housing (a step taller and wider)
    add(part("rearBlock", S, BODY, (2.0, AC - H, ZC - W), (2.6, AC + H, ZC + W))
        .inset("x", 0, y=(0.1, 0.1), z=(0.1, 0.1)))
    add(part("body", S, BODY, (2.6, AC - H, ZC - W), (3.9, AC + H, ZC + W))
        .inset("y", 0, z=(0.08, 0.08)).inset("y", 1, z=(0.06, 0.06)))
    add(part("front", S, BODY, (3.9, AC - H - 0.04, ZC - W - 0.04), (XF - 0.06, AC + H + 0.02, ZC + W + 0.04))
        .inset("x", 1, y=(0.05, 0.05), z=(0.05, 0.05)))
    add(part("frontPlate", S, EDGE, (XF - 0.08, AC - H, ZC - W), (XF, AC + H - 0.02, ZC + W)))
    for sy in (-1, 1):                                   # the front housing's corner screws (top view)
        for sz in (-1, 1):
            add(part("frontScrew", S, SCREW, (XF - 0.02, AC + sy * 0.38 - 0.04, ZC + sz * 0.46 - 0.04),
                     (XF + 0.01, AC + sy * 0.38 + 0.04, ZC + sz * 0.46 + 0.04)))
    # the objective's round hood stands out of the squared housing (the reference's big front ring), the lens set back
    add(*octagon("objHood", S, BODY, XF - 0.04, XF + 0.2, AC, ZC, H + 0.04))    # a little taller than the body
    add(*octagon("objLip", S, EDGE, XF + 0.2, XF + 0.25, AC, ZC, H + 0.07))
    add(*octagon("objRim", S, DARK, XF + 0.25, XF + 0.255, AC, ZC, 0.42))   # discs stepping forward (as the ACOG's)
    add(*octagon("objective", S, LENS, XF + 0.255, XF + 0.265, AC, ZC, 0.36))
    add(part("seam", S, DARK, (3.86, AC - H - 0.05, ZC - W - 0.05), (3.94, AC + H + 0.03, ZC + W + 0.05)))

    # --- top: two guard ears (the reference's small fins over the front and the rear)
    for ex in (2.15, 4.35):
        ear = part("ear", S, BODY, (ex - 0.14, AC - H - 0.16, ZC - 0.14), (ex + 0.14, AC - H + 0.02, ZC + 0.14))
        add(ear.inset("y", 0, x=(0.1, 0.1), z=(0.06, 0.06)))

    # --- left side (+z): the throw lever between its 4X and 1X marks, the illumination knob with its tethered cap
    add(part("leverPlate", S, DARK, (2.75, AC + 0.12, ZC + W - 0.02), (3.85, AC + 0.4, ZC + W + 0.04)))
    lever = part("lever", S, EDGE, (3.15, AC + 0.02, ZC + W - 0.01), (3.4, AC + 0.44, ZC + W + 0.12))  # from the side
    add(lever.inset("z", 1, x=(0.04, 0.04), y=(0.04, 0.0)))
    add(part("mark4x", S, MARK, (2.86, AC + 0.2, ZC + W + 0.04), (2.98, AC + 0.26, ZC + W + 0.05)))
    add(part("mark1x", S, MARK, (3.62, AC + 0.2, ZC + W + 0.04), (3.74, AC + 0.26, ZC + W + 0.05)))
    add(*oct_z("knob", S, BODY, 2.3, AC - 0.14, ZC + W - 0.02, ZC + W + 0.16, 0.3))
    add(*ring_ribs("knobKnurl", S, DARK, 2.3, AC - 0.14, ZC + W + 0.0, ZC + W + 0.15, 0.3, n=14, size=0.06))
    add(*oct_z("knobCap", S, RUB, 2.3, AC - 0.14, ZC + W + 0.16, ZC + W + 0.22, 0.2))
    # the cap's tether: a wire from the knob down the body's side (inner face in the body, so it sits on it)
    add(part("tether", S, DARK, (2.45, AC + 0.12, ZC + W - 0.01), (2.85, AC + 0.17, ZC + W + 0.03)).shift("x", 1, y=0.12))

    # --- mount: a long base plate on the rail and two clamps holding the body (the rail shows between them, as in
    # the reference), rail jaws, two throw levers on the left, cross bolts on the right, the battery cap in front
    add(part("mountPlate", S, BODY, (1.55, RAIL - 0.16, ZC - 0.5), (4.45, RAIL + 0.02, ZC + 0.5))
        .inset("y", 0, x=(0.08, 0.08), z=(0.04, 0.04)))
    for cx0, cx1 in ((1.75, 2.45), (3.55, 4.25)):
        add(part("mountClamp", S, BODY, (cx0, AC + H - 0.06, ZC - 0.44), (cx1, RAIL - 0.14, ZC + 0.44))
            .inset("y", 0, x=(0.06, 0.06)))
    add(part("mountJaw", S, BODY, (1.65, RAIL - 0.12, ZC + 0.42), (4.35, RAIL + 0.1, ZC + 0.52))
        .inset("y", 1, x=(0.04, 0.04)), pair=True)
    for lx in (2.1, 3.9):                                # on the clamps' outer sides (left)
        arm = part("qdLever", S, EDGE, (lx - 0.3, RAIL - 0.36, ZC + 0.42), (lx + 0.3, RAIL - 0.22, ZC + 0.52))
        add(arm.shift("x", 0, y=0.08))
        add(*oct_z("qdPivot", S, SCREW, lx, RAIL - 0.29, ZC + 0.42, ZC + 0.56, 0.07))
        add(*oct_z("crossBolt", S, SCREW, lx, RAIL - 0.29, ZC - 0.52, ZC - 0.41, 0.07))
    add(*oct_z("battery", S, BODY, 4.65, AC + 0.42, ZC + 0.3, ZC + 0.56, 0.2))
    add(*ring_ribs("batteryKnurl", S, DARK, 4.65, AC + 0.42, ZC + 0.32, ZC + 0.55, 0.2, n=10, size=0.05))
    add(part("batteryArm", S, BODY, (4.4, AC + 0.3, ZC + 0.2), (4.7, AC + 0.55, ZC + 0.36)))
    return P


# ================================================================ output
def to_file(parts, shift=SHIFT):
    """Gun space -> attachment file space (minus renderAttachments' translate for the slot)."""
    for p in parts:
        for k, q in p.c.items():
            p.c[k] = [q[0] - shift[0], q[1] - shift[1], q[2] - shift[2]]
    return parts


def reticle_eotech(path, n=32):
    """68 MOA ring, centre dot, four ticks across the ring (photo 62), red. Drawn about 15 to 30
    screen pixels wide in the glass (the reticle sways with the gun: SightReticle), and Minecraft
    samples textures without smoothing, so a big texture loses its thin lines (a 512 px first
    version came out as dashes): pixel art at 32 px with 2 px lines."""
    from PIL import Image
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    red, glow = (255, 40, 34, 255), (255, 60, 50, 110)
    c = (n - 1) / 2.0
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c)
            on = 9.6 <= d <= 11.6                                    # the ring
            on |= d <= 1.0                                           # the dot
            on |= (abs(x - c) <= 0.6 and 7.5 <= abs(y - c) <= 13.5)   # ticks top / bottom
            on |= (abs(y - c) <= 0.6 and 7.5 <= abs(x - c) <= 13.5)   # ticks left / right
            if on:
                img.putpixel((x, y), red)
            elif 9.0 <= d <= 12.3 or d <= 1.7:
                img.putpixel((x, y), glow)                           # a soft edge: the holographic glow
    img.save(path)


def reticle_acog(path, n=512):
    """TA11 BDC (photo 59): thin cross, thick side and bottom posts with // marks, green centre,
    stadia for 4 to 10 (hundred metres)."""
    from PIL import Image, ImageDraw
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, k = n // 2, (8, 8, 8, 255)
    thin, thick = max(4, n // 128), max(14, n // 34)
    d.line((c, int(n * 0.08), c, c), fill=k, width=thin)                     # top
    d.line((int(n * 0.33), c, int(n * 0.67), c), fill=k, width=thin)         # centre line
    d.rectangle((int(n * 0.06), c - thick // 2, int(n * 0.33), c + thick // 2), fill=k)   # left post
    d.rectangle((int(n * 0.67), c - thick // 2, int(n * 0.94), c + thick // 2), fill=k)   # right post
    d.line((c, c, c, int(n * 0.82)), fill=k, width=thin)
    d.rectangle((c - thick // 2, int(n * 0.82), c + thick // 2, int(n * 0.95)), fill=k)   # bottom post
    for x, y in ((c, int(n * 0.14)), (int(n * 0.12), c), (int(n * 0.88), c), (c, int(n * 0.9))):
        for dx in (-5, 3):                                                   # the // marks
            d.line((x + dx - 4, y + 7, x + dx + 4, y - 7), fill=(255, 255, 255, 255), width=3)
    for i, (dy, w, label) in enumerate(((0.05, 0.035, "4"), (0.09, 0.045, "6"), (0.14, 0.06, "8"), (0.2, 0.075, "10"))):
        y = c + int(n * dy)
        d.line((c - int(n * w), y, c + int(n * w), y), fill=k, width=thin)
        d.text((c + int(n * w) + 8, y - 6), label, fill=k)
    for dy in (0.25, 0.3):                                                   # the lower stadia, no numbers
        y = c + int(n * dy)
        d.line((c - int(n * 0.09), y, c + int(n * 0.09), y), fill=k, width=thin)
    g = (90, 255, 90, 255)                                                   # lit green centre
    d.line((c - 18, c, c + 18, c), fill=g, width=6)
    d.line((c, c - 18, c, c + 18), fill=g, width=6)
    img.save(path)


def reticle_specterdr(path, n=512):
    """SpecterDR: a crosshair with heavy outer posts and thin inner lines meeting at a lit red centre dot (one
    reticle for 1x and 4x, as the real optic). Lines as thick as the ACOG's (they stayed visible at 3.5x)."""
    from PIL import Image, ImageDraw
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, k = n // 2, (8, 8, 8, 255)
    thin, thick = max(4, n // 128), max(16, n // 30)
    d.line((c, int(n * 0.3), c, int(n * 0.7)), fill=k, width=thin)
    d.line((int(n * 0.3), c, int(n * 0.7), c), fill=k, width=thin)
    for a, b in (((int(n * 0.05), c - thick // 2), (int(n * 0.3), c + thick // 2)),
                 ((int(n * 0.7), c - thick // 2), (int(n * 0.95), c + thick // 2)),
                 ((c - thick // 2, int(n * 0.7)), (c + thick // 2, int(n * 0.95))),
                 ((c - thick // 2, int(n * 0.05)), (c + thick // 2, int(n * 0.3)))):
        d.rectangle((a, b), fill=k)
    r = max(6, n // 60)
    d.ellipse((c - r, c - r, c + r, c + r), fill=(235, 40, 30, 255))
    img.save(path)


def write(name, parts, reticle, slot="sight", shift=SHIFT):
    """The model, its texture and icon (and the reticle for a sight) under assets/deci, in the slot's folders."""
    from PIL import Image
    parts = to_file(parts, shift)
    uvs, h = layout(parts)
    total = 32
    while total < h:
        total *= 2
    names, counts = [], {}
    lines = [HEADER, "  textureWidth = 512;", "  textureHeight = %d;" % total]
    for i, p in enumerate(parts):
        k = counts.get(p.group, 0)
        counts[p.group] = k + 1
        lines += part_block("%s%d" % (p.group, k), p, uvs[i][0], uvs[i][1])
    text = "\n".join(lines) + "\n"
    mp = os.path.join(OUT, "models", "attachments", slot, name + ".bmodel")
    os.makedirs(os.path.dirname(mp), exist_ok=True)
    open(mp, "w").write(text)
    rows = paint(parts)
    k2 = 2
    img = Image.new("RGBA", (512 * k2, total * k2), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c[3] and y < img.height:
                img.putpixel((x, y), c)
    tp = os.path.join(OUT, "textures", "model", "attachments", slot, name + ".png")
    os.makedirs(os.path.dirname(tp), exist_ok=True)
    img.save(tp)
    if reticle:
        rp = os.path.join(OUT, "textures", "model", "guns", "scopes", name + ".png")
        os.makedirs(os.path.dirname(rp), exist_ok=True)
        reticle(rp)
    icon(name, text, img, os.path.join(OUT, "textures", "items", "attach", name + ".png"))
    return {"name": name, "parts": len(parts), "texture": [512, total]}


def icon(name, text, img, path, size=32, width=30):
    from PIL import Image
    g = study.Gun(name, "sight", text, img, True)
    r = study.render(g, "three", scale=24, pad=0, bg=(0, 0, 0, 0))
    k = width / max(r.width, r.height)
    small = r.resize((max(1, round(r.width * k)), max(1, round(r.height * k))), Image.BOX)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - small.width) // 2, (size - small.height) // 2
    for y in range(small.height):
        for x in range(small.width):
            p = small.getpixel((x, y))
            if p[3] >= 140:
                a = p[3] / 255.0
                out.putpixel((ox + x, oy + y), tuple(min(255, int(c / a)) for c in p[:3]) + (255,))
    edge = out.copy()
    for y in range(size):
        for x in range(size):
            if out.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < size and 0 <= y + dy < size and out.getpixel((x + dx, y + dy))[3]
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.putpixel((x, y), (8, 8, 8, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    edge.save(path)


def write_reticles():
    """assets/deciworldgen/sight_reticles.txt for fixes/SightReticle: name x y z size, in the
    attachment FILE space (gun space minus SHIFT), model units."""
    path = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen", "sight_reticles.txt")
    with open(path, "w") as f:
        f.write("# name x y z size: reticle square on the sight's glass, attachment file space (tools/guns/sights.py)\n")
        for n, (x, y, z, s) in sorted(RETICLE.items()):
            f.write("%s %.4f %.4f %.4f %.4f\n" % (n, x - SHIFT[0], y - SHIFT[1], z - SHIFT[2], s))
    return path


def check(name, slot="sight", lo=30, hi=150, on="m4a4", allowed=3):
    """The written model exists, its part count is in range (Decimation's sights 7 to 68, ours 92 and 117: lo .. hi)
    and, placed on a gun as the game places it (study.with_attachments), at most `allowed` parts sit on nothing
    (study.contact): our accepted EOTech and ACOG flag 3 and 2 there (thin details on leaning faces), Decimation's 2x
    and 4x none. Prints the problems; returns 1 on any."""
    path = os.path.join(OUT, "models", "attachments", slot, name + ".bmodel")
    if not os.path.exists(path):
        print("%s: no model at %s" % (name, path))
        return 1
    g = study.with_attachments(study.load(on), [name])
    mine = [p for p in g.parts if p.name.startswith(name + "_")]
    bad = [n for n, k in study.contact(g, only=re.escape(name) + "_")]
    problems = []
    if not lo <= len(mine) <= hi:
        problems.append("%d parts (want %d .. %d)" % (len(mine), lo, hi))
    if len(bad) > allowed:
        problems.append("%d parts sit on nothing on the %s (at most %d): %s" % (len(bad), on, allowed, ", ".join(bad[:12])))
    print("%s: %d parts, %d unseated on the %s, %s" % (name, len(mine), len(bad), on, "; ".join(problems) or "ok"))
    return 1 if problems else 0


if __name__ == "__main__":
    if sys.argv[1:2] == ["--check"]:
        sys.exit(max(check(n) for n in sys.argv[2:] or ["eotech558", "ta11acog"]))
    print(write("eotech558", eotech(), reticle_eotech))
    print(write("ta11acog", acog(), reticle_acog))
    print(write("specterdr", specterdr(), reticle_specterdr))
    print(write_reticles())
