#!/usr/bin/env python3
"""MAC-10 (Ingram M10, .45 ACP), v2 in Decimation's style (docs/gun_style_guide.md,
skill .claude/skills/decimation-gun). Our own layout from the real gun's look:
boxy stamped receiver, short threaded barrel, magazine inside the pistol grip,
front strap loop, collapsed wire stock along the lower sides, cocking knob on
top with a sight notch, peep rear sight with protective ears.

References: Decimation's Uzi (same layout: magazine in the grip, short
receiver, folded stock; 105 parts, 13.6 x 7.5 x 1.2) and MP5A3 (receiver
panels). Real M10 about 270 mm long with the stock collapsed [not verified]
= about 8.7 units at 31 mm a unit; ours 9.35. Proportions measured on the
user's side photo (10 Oktober 2026, receiver = 7.0 units, 42 px a unit):
receiver 1.86 high, grip + housing 2.8 below it, magazine 2.7 out, grip
x 3.6 to 5.55 raked back, knob x 7.2 to 7.55, front sight x 8.55 to 8.9,
threads right at the receiver then a thin barrel to 10.5, butt pad behind
the lower rear, the folded wire loop over the rear top.

Anchors (style guide sections 9, 13, 14):
- receiver top y -4.45 (red dot bottom lands at -4.43), sight tops -4.95 to
  -5.0 (the aim centre), everything centred on z -0.15, receiver 1.2 wide;
- grip at x 3.6 to 5.55 (photo), hands as the v1 MAC-10
  (Uzi based, looked right in first person);
- muzzle tip x 10.5, flamePos x 10.4 on the bore (y -3.9);
- right side (where casings go, ejectPos z negative like Decimation's guns)
  is -z: the ejection port is there.
Part budget (Uzi table style): receiver 20, barrel 15, front sight 6, rear
sight 11, cocking knob and bolt 4 (slideModel), grip 14, trigger group 6,
magazine 5 (ammoModel), stock 12.

    python3 tools/guns/mac10.py     # writes model, texture, icon, animations
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import ZC, anib, build, octagon, part, slide_names  # noqa: E402

STEEL = (38, 38, 40)    # parkerized receiver
STEEL2 = (44, 44, 46)   # raised panels, caps
DARK = (24, 24, 26)     # sights, port inside, slot
BARREL = (50, 50, 52)
THREAD = (58, 58, 60)
GRIP = (30, 30, 32)
MAG = (34, 34, 36)
ROD = (56, 56, 58)      # bright wire stock
BOLT = (70, 70, 72)     # bolt face seen in the port

G, A, S = "gunModel", "ammoModel", "slideModel"
P = []


def add(*parts, pair=False):
    for p in parts:
        P.append(p)
        if pair:
            P.append(p.mirror())


# ---------------------------------------------------------------- receiver (20)
add(part("upperCore", G, STEEL, (2.0, -4.3, -0.75), (9.0, -3.2, 0.45)))
add(part("topCover", G, STEEL, (2.0, -4.45, -0.75), (9.0, -4.3, 0.45)).inset("y", 0, z=(0.1, 0.1)))
add(part("rearCap", G, STEEL2, (1.85, -4.35, -0.72), (2.0, -2.7, 0.42)).inset("x", 0, y=(0.1, 0.08), z=(0.1, 0.1)))
add(part("frontCap", G, STEEL2, (9.0, -4.35, -0.72), (9.15, -2.75, 0.42)).inset("x", 1, y=(0.12, 0.12), z=(0.1, 0.1)))
add(part("lowerCore", G, STEEL, (2.0, -3.2, -0.7), (8.8, -2.6, 0.4)).inset("y", 1, z=(0.06, 0.06)))
add(part("seam", G, STEEL2, (2.1, -3.26, 0.45), (8.9, -3.14, 0.5)), pair=True)
add(part("stampRect", G, STEEL2, (4.4, -4.25, 0.45), (5.7, -3.55, 0.49)).inset("z", 1, x=(0.06, 0.06), y=(0.06, 0.06)))
add(part("stampLine", G, STEEL2, (2.55, -4.25, 0.45), (2.7, -3.45, 0.48)).shift("y", 1, x=0.9))
add(part("pin", G, STEEL2, (8.4, -3.05, 0.45), (8.62, -2.83, 0.5)).inset("z", 1, x=(0.05, 0.05), y=(0.05, 0.05)), pair=True)
# ejection port on the right (-z) side: a frame around a dark window, the bolt inside (slideModel)
add(part("portTop", G, STEEL2, (5.5, -4.12, -0.8), (7.0, -4.02, -0.75)))
add(part("portBottom", G, STEEL2, (5.5, -3.62, -0.8), (7.0, -3.52, -0.75)))
add(part("portRear", G, STEEL2, (5.5, -4.02, -0.8), (5.6, -3.62, -0.75)))
add(part("portFront", G, STEEL2, (6.9, -4.02, -0.8), (7.0, -3.62, -0.75)))
add(part("portInside", G, DARK, (5.6, -4.02, -0.77), (6.9, -3.62, -0.75)))
# cocking slot on top: two lips and a dark insert
add(part("slotLip", G, STEEL2, (3.2, -4.52, -0.34), (8.3, -4.45, -0.24)).inset("y", 0, x=(0.05, 0.05)), pair=True)
add(part("slotInsert", G, DARK, (3.2, -4.47, -0.24), (8.3, -4.45, -0.06)))
# front strap loop under the front of the receiver
add(part("lugFront", G, STEEL, (8.85, -2.6, -0.32), (9.0, -2.05, 0.02)))
add(part("lugBottom", G, STEEL, (8.25, -2.17, -0.32), (9.0, -2.05, 0.02)).inset("y", 1, x=(0.05, 0.05)))
add(part("lugRear", G, STEEL, (8.25, -2.6, -0.32), (8.4, -2.15, 0.02)))

# ---------------------------------------------------------------- barrel (15)
add(*octagon("collar", G, STEEL2, 9.15, 9.3, -3.9, ZC, 0.36))
add(*octagon("thread", G, THREAD, 9.3, 9.95, -3.9, ZC, 0.28))
add(*octagon("ring1", G, THREAD, 9.45, 9.55, -3.9, ZC, 0.32))
add(*octagon("ring2", G, THREAD, 9.7, 9.8, -3.9, ZC, 0.32))
add(*octagon("barrel", G, BARREL, 9.95, 10.5, -3.9, ZC, 0.21))

# ---------------------------------------------------------------- front sight (6)
add(part("fsBase", G, DARK, (8.3, -4.6, -0.45), (8.85, -4.45, 0.15)).inset("y", 0, x=(0.1, 0.1)))
add(part("fsPost", G, DARK, (8.52, -4.95, -0.22), (8.64, -4.6, -0.08)).inset("y", 0, x=(0.02, 0.02), z=(0.02, 0.02)))
add(part("fsEar", G, DARK, (8.38, -4.97, 0.04), (8.78, -4.6, 0.14)).inset("y", 0, x=(0.12, 0.12)), pair=True)
add(part("fsBrace", G, DARK, (8.42, -4.72, 0.14), (8.74, -4.6, 0.26)).inset("y", 0, z=(0, 0.1)), pair=True)

# ---------------------------------------------------------------- rear sight (11)
add(part("rsBase", G, DARK, (2.1, -4.6, -0.55), (2.85, -4.45, 0.25)).inset("y", 0, x=(0.08, 0.08), z=(0.05, 0.05)))
add(part("rsRingTop", G, DARK, (2.4, -4.97, -0.27), (2.52, -4.88, -0.03)).inset("y", 0, z=(0.05, 0.05)))
add(part("rsRingBottom", G, DARK, (2.4, -4.68, -0.27), (2.52, -4.6, -0.03)).inset("y", 1, z=(0.05, 0.05)))
add(part("rsRingSide", G, DARK, (2.4, -4.88, -0.07), (2.52, -4.68, 0.01)), pair=True)
add(part("rsEar", G, DARK, (2.15, -5.0, 0.12), (2.75, -4.6, 0.24)).inset("y", 0, x=(0.14, 0.14)), pair=True)
add(part("rsBrace", G, DARK, (2.2, -4.75, 0.24), (2.7, -4.6, 0.36)).inset("y", 0, z=(0, 0.1)), pair=True)
add(part("rsScrew", G, STEEL2, (2.42, -4.5, 0.25), (2.52, -4.4, 0.29)), pair=True)

# ---------------------------------------------------------------- cocking knob + bolt (slideModel, 4)
add(part("knob", S, DARK, (7.2, -4.92, -0.34), (7.55, -4.47, -0.21)).inset("y", 0, x=(0.06, 0.06), z=(0.03, 0.0)))
add(P[-1].mirror("knobR"))
add(part("knobStem", S, STEEL2, (7.25, -4.52, -0.21), (7.5, -4.44, -0.09)))
add(part("bolt", S, BOLT, (6.0, -3.98, -0.79), (6.5, -3.66, -0.76)))

# ---------------------------------------------------------------- grip (14)
# mag housing (front, vertical, steel) and the raked hand grip behind it (photo 1: the rear
# edge leans back toward the bottom)
def grip_rear(y):
    """The raked rear edge of the hand grip: x 4.0 at the receiver, 3.6 at the bottom (photo)."""
    return 4.0 - 0.4 * (y + 2.6) / 2.8


for i, (y0, y1) in enumerate(((-2.6, -1.55), (-1.55, -0.5), (-0.5, 0.2))):
    add(part("housing%d" % i, G, STEEL, (4.35, y0, -0.62), (5.55, y1, 0.32)).inset("x", 1, z=(0.08, 0.08)))
    g = part("grip%d" % i, G, GRIP, (grip_rear(y0), y0, -0.6), (4.36, y1, 0.3))
    g.inset("y", 1, x=(grip_rear(y1) - grip_rear(y0), 0))  # bottom rear corners further back: one straight rake
    add(g.inset("x", 0, z=(0.12, 0.12)))
gp = part("gripPanel", G, GRIP, (grip_rear(-2.3) + 0.08, -2.3, 0.3), (4.3, 0.0, 0.34))
gp.inset("y", 1, x=(grip_rear(0.0) - grip_rear(-2.3), 0))
add(gp.inset("z", 1, x=(0.05, 0.05), y=(0.08, 0.08)), pair=True)
add(part("housingRib", G, STEEL2, (4.5, -2.4, 0.32), (5.4, -2.3, 0.36)), pair=True)
add(part("housingRib2", G, STEEL2, (4.5, 0.0, 0.32), (5.4, 0.1, 0.36)), pair=True)
add(part("magRelease", G, DARK, (4.25, 0.1, -0.35), (4.37, 0.38, 0.05)).inset("x", 0, y=(0.05, 0.05)))
add(part("gripLip", G, STEEL2, (4.3, 0.15, -0.66), (5.6, 0.27, 0.36)).inset("y", 1, x=(0.05, 0.05), z=(0.05, 0.05)))

# ---------------------------------------------------------------- trigger group (6)
add(part("trigger", G, DARK, (5.8, -2.6, -0.22), (5.95, -2.2, -0.08)))
add(part("triggerTip", G, DARK, (5.75, -2.2, -0.22), (5.9, -1.9, -0.08)).shift("y", 1, x=-0.05).inset("y", 1, x=(0.03, 0)))
add(part("guardFront", G, STEEL, (6.9, -2.6, -0.26), (7.05, -1.62, -0.04)).shift("y", 1, x=-0.12))
add(part("guardBottom", G, STEEL, (5.55, -1.62, -0.26), (6.93, -1.5, -0.04)))
add(part("guardCorner", G, STEEL, (6.7, -1.75, -0.26), (6.93, -1.62, -0.04)).inset("y", 0, x=(0.2, 0)))
add(part("safety", G, DARK, (6.45, -2.6, -0.3), (6.65, -2.47, 0.0)).inset("y", 1, x=(0.04, 0.04)))

# ---------------------------------------------------------------- magazine (ammoModel, 5)
add(part("magBody", A, MAG, (4.45, 0.27, -0.5), (5.45, 2.9, 0.2)))
add(part("magBase", A, DARK, (4.35, 2.9, -0.56), (5.55, 3.05, 0.26)).inset("y", 1, x=(0.05, 0.05), z=(0.05, 0.05)))
add(part("magRib", A, MAG, (4.65, 0.5, 0.2), (5.25, 2.7, 0.24)), pair=True)
add(part("magFront", A, MAG, (5.45, 0.45, -0.42), (5.49, 2.8, 0.12)))

# ---------------------------------------------------------------- collapsed wire stock (12)
add(part("rod", G, ROD, (1.6, -2.95, 0.42), (8.5, -2.77, 0.56)), pair=True)
add(part("rodCap", G, STEEL2, (8.5, -3.0, 0.39), (8.7, -2.72, 0.6)).inset("x", 1, y=(0.05, 0.05)), pair=True)
add(part("rodGuide", G, STEEL2, (2.0, -3.05, 0.4), (2.4, -2.67, 0.6)).inset("x", 0, y=(0.06, 0.06)), pair=True)
add(part("buttTop", G, DARK, (1.2, -3.3, -0.85), (1.75, -3.1, 0.55)).inset("x", 0, y=(0.05, 0)))
add(part("buttUp", G, DARK, (1.2, -3.1, 0.37), (1.45, -2.05, 0.55)), pair=True)
add(part("buttPad", G, DARK, (1.15, -2.05, -0.85), (1.75, -1.85, 0.55)).inset("x", 0, y=(0, 0.06), z=(0.08, 0.08)))
add(part("hinge", G, STEEL2, (1.75, -3.3, 0.5), (1.95, -3.05, 0.6)), pair=True)
# folded wire shoulder loop lying over the rear top, its sides outside the rear sight
add(part("loopSide", G, ROD, (1.25, -4.72, 0.45), (3.6, -4.6, 0.57)), pair=True)
add(part("loopRear", G, ROD, (1.25, -4.72, -0.87), (1.37, -4.6, 0.57)))
add(part("loopFront", G, ROD, (3.48, -4.6, 0.45), (3.6, -4.35, 0.57)), pair=True)
add(part("loopDown", G, ROD, (1.25, -4.6, 0.45), (1.37, -3.3, 0.57)), pair=True)

SPEC = {
    "name": "mac10",
    "category": "smg",
    "flamePos": (10.4, -3.9, ZC),
    "ejectPos": (6.25, -3.82, -0.8),
    "rhPos": (-5.0, 1.52, -2.5),
    "rhRot": (0, 0, 0),
    "lhPos": (0.3, 10.07, 4.92),
    "lhRot": (0, 0, 0),
    "parts": P,
}

# ---------------------------------------------------------------- animations (Decimation's timings, our values)
Z = ((0, 0, 0), (0, 0, 0))
SL = slide_names(P)
back = {n: ((-1.6, 0, 0), (0, 0, 0)) for n in SL}
home = {n: Z for n in SL}


def keys(**kw):
    return kw


SPEC["animations"] = {
    # every shot: the bolt and knob run back (RAND frame); the game does the recoil
    "Fire": anib(2, 0, {
        0: {"parts": dict(Model=Z, OffHand=Z)},
        1: {"kind": "RAND", "parts": dict(back, Model=Z, OffHand=Z)},
    }),
    # empty: bolt and knob held back
    "SlideBack": anib(1, 0, {0: {"parts": dict(back, Model=Z, OffHand=Z)}}, static=True),
    # charging: start held back, the off hand rides the knob home (sound at 6)
    "Rack": anib(19, 1, {
        0: {"parts": dict(back, OffHand=((0, 0, 0), (-6, 0, 0)), Model=((0, 0, 0), (8, 0, 8)))},
        3: {"parts": dict(back, OffHand=((2.6, 0, -0.4), (-12, 0, 8)), Model=((0, 0, 0), (4, 0, 4)))},
        6: {"sound": "Rack", "shake": 1.1, "parts": dict(home, OffHand=((2.6, 0, -2.2), (-8, 0, 8)), Model=Z)},
        9: {"parts": dict(OffHand=((2.6, 0, -2.2), (0, 0, 8)), Model=Z)},
        12: {"parts": dict(OffHand=((3.8, 0, -2.2), (0, 0, 8)), Model=Z)},
        15: {"parts": dict(OffHand=((2.5, 0, -0.7), (0, 0, 0)), Model=Z)},
        18: {"parts": dict(OffHand=Z, Model=Z)},
    }),
    # magazine out down the grip, a new one in, then the bolt if it ran dry
    "Reload1": anib(57, 0, {
        0: {"parts": dict(Model=Z, OffHand=Z, ammoModel0=Z)},
        5: {"sound": "MagOut", "shake": 1.0,
            "parts": dict(OffHand=((0, 0.4, 0), (-12, 4, 30)), Model=((0, 0, 0), (8, 0, 6)))},
        10: {"parts": dict(ammoModel0=((0, 2.5, -1.5), (0, 0, 0)), OffHand=((-0.8, 2.2, 1.2), (-30, 4, 32)),
                           Model=((0, 0, 0), (8, 0, 12)))},
        15: {"parts": dict(ammoModel0=((-0.5, 7, -3.5), (0, 0, 0)), OffHand=((0.5, 5, 1), (-35, 4, 45)),
                           Model=((0, 0, 0), (2, -4, 18)))},
        20: {"kind": "SWITCH", "parts": dict(ammoModel0=((-3, 16, -6), (0, 0, 0)), OffHand=((3, 12.5, -1), (-48, 4, 65)),
                                             Model=((0, 0, 0), (0, -5, 20)))},
        25: {"parts": dict(ammoModel0=((-3.5, 15.5, -5.5), (0, 0, 0)), OffHand=((2.4, 12, 0.4), (-48, 4, 65)),
                           Model=((0, 0, 0), (0, -3, 20)))},
        30: {"parts": dict(ammoModel0=((-1, 9, -6), (0, 0, 0)), OffHand=((3, 6.4, -1), (-34, 4, 55)),
                           Model=((0, 0, 0), (4, 0, 22)))},
        35: {"parts": dict(ammoModel0=((-1, 6.8, -4.5), (0, 0, 0)), OffHand=((1.2, 5.4, 1.4), (-34, 4, 46)),
                           Model=((0, 0, 0), (12, 4, 22)))},
        40: {"kind": "LOAD", "sound": "MagIn", "shake": 1.1,
             "parts": dict(ammoModel0=((0, 2.2, -2), (0, 0, 0)), OffHand=((-1.8, -0.2, 2.8), (-32, 4, 14)),
                           Model=((0, 0, 0), (22, 12, 22)))},
        45: {"parts": dict(ammoModel0=Z, OffHand=((-3.6, -3.2, 3.8), (-32, 4, 14)), Model=((0, 0, 0), (26, 12, 22)))},
        50: {"kind": "TRYBOLT", "parts": dict(OffHand=((0, 0, 0), (-18, 0, 4)), Model=((0, 0, 0), (12, 4, 12)))},
        55: {"parts": dict(Model=Z, OffHand=Z, ammoModel0=Z)},
    }),
}

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "..", "..", "dev", "src", "main", "resources", "assets", "deci")
    print(build(SPEC, os.path.normpath(out)))
