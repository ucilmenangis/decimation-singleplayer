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


# ================================================================ output
def to_file(parts):
    """Gun space -> attachment file space (minus renderAttachments' translate)."""
    for p in parts:
        for k, q in p.c.items():
            p.c[k] = [q[0] - SHIFT[0], q[1] - SHIFT[1], q[2] - SHIFT[2]]
    return parts


def reticle_eotech(path, n=512):
    from PIL import Image, ImageDraw, ImageFilter
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, red = n / 2, (255, 46, 40, 255)
    r = n * 0.3
    d.ellipse((c - r, c - r, c + r, c + r), outline=red, width=int(n * 0.014))
    rd = n * 0.012
    d.ellipse((c - rd, c - rd, c + rd, c + rd), fill=red)
    for ang in range(0, 360, 90):        # the four ticks across the ring (photo 62)
        a = math.radians(ang)
        x0, y0 = c + math.cos(a) * r * 0.8, c + math.sin(a) * r * 0.8
        x1, y1 = c + math.cos(a) * r * 1.2, c + math.sin(a) * r * 1.2
        d.line((x0, y0, x1, y1), fill=red, width=int(n * 0.012))
    glow = img.filter(ImageFilter.GaussianBlur(n * 0.012))
    out = Image.alpha_composite(Image.eval(glow, lambda v: v), img)
    out.save(path)


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


def write(name, parts, reticle):
    from PIL import Image
    parts = to_file(parts)
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
    mp = os.path.join(OUT, "models", "attachments", "sight", name + ".bmodel")
    os.makedirs(os.path.dirname(mp), exist_ok=True)
    open(mp, "w").write(text)
    rows = paint(parts)
    k2 = 2
    img = Image.new("RGBA", (512 * k2, total * k2), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c[3] and y < img.height:
                img.putpixel((x, y), c)
    tp = os.path.join(OUT, "textures", "model", "attachments", "sight", name + ".png")
    os.makedirs(os.path.dirname(tp), exist_ok=True)
    img.save(tp)
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


if __name__ == "__main__":
    print(write("eotech558", eotech(), reticle_eotech))
    print(write("ta11acog", acog(), reticle_acog))
