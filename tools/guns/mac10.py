#!/usr/bin/env python3
"""MAC-10 (Ingram M10, .45 ACP): box spec for tools/guns/gunmodel.py.

Our own layout from the real gun's proportions (boxy stamped receiver,
short threaded barrel, magazine inside the pistol grip, front strap lug,
retracted wire stock along the sides, cocking knob on top). Decimation
model units: x forward, y DOWN (receiver top at y -5), z sideways centred
near -0.15 like Decimation's Uzi. Groups: gunModel (body), ammoModel
(magazine, moved by the reload animation), slideModel (cocking knob, moved
by fire / rack).

    python3 tools/guns/mac10.py      # writes the model, texture, preview ops
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gunmodel  # noqa: E402

STEEL = (98, 101, 106)      # parkerized receiver
DARK = (72, 74, 78)       # lower parts, grip
BLACK = (50, 51, 54)      # sights, trigger, knob
BARREL = (92, 94, 98)     # bare threaded barrel
MAG = (78, 79, 82)
ROD = (110, 112, 116)        # wire stock

G, A, S = "gunModel", "ammoModel", "slideModel"

SPEC = {
    "name": "mac10",
    "category": "smg",
    "flamePos": (11.8, -4.15, -0.15),
    "ejectPos": (6.0, -4.4, 0.8),
    # hand poses start from Decimation's Uzi values (similar size and grip), tuned in game
    "rhPos": (-5.0, 1.52, -2.5),
    "rhRot": (0, 0, 0),
    "lhPos": (0.3, 10.07, 4.92),
    "lhRot": (0, 0, 0),
    "boxes": [
        # receiver
        ("receiverUpper", G, (1.0, -5.0, -1.0), (10.0, -3.2, 0.7), STEEL, {"side": "port", "side2": None}),
        ("receiverLower", G, (1.0, -3.2, -0.95), (8.6, -2.4, 0.65), DARK),
        # barrel and threads
        ("barrel", G, (10.0, -4.55, -0.55), (11.8, -3.75, 0.25), BARREL),
        ("thread1", G, (10.5, -4.65, -0.65), (10.8, -3.65, 0.35), BARREL, {"all": "rings"}),
        ("thread2", G, (11.1, -4.65, -0.65), (11.4, -3.65, 0.35), BARREL, {"all": "rings"}),
        # sights
        ("frontSight", G, (9.1, -5.6, -0.3), (9.5, -5.0, 0.0), BLACK),
        ("frontEarL", G, (9.0, -5.7, -0.75), (9.6, -5.0, -0.55), BLACK),
        ("frontEarR", G, (9.0, -5.7, 0.25), (9.6, -5.0, 0.45), BLACK),
        ("rearSightBase", G, (1.2, -5.25, -0.75), (2.0, -5.0, 0.45), BLACK),
        ("rearSightL", G, (1.2, -5.75, -0.75), (2.0, -5.25, -0.35), BLACK),
        ("rearSightR", G, (1.2, -5.75, 0.05), (2.0, -5.25, 0.45), BLACK),
        # grip / magazine well, trigger and guard
        ("grip", G, (4.0, -2.4, -0.9), (5.9, 0.9, 0.6), DARK, {"side": "grip", "side2": "grip"}),
        ("trigger", G, (6.4, -2.4, -0.3), (6.75, -1.4, 0.0), BLACK),
        ("guardFront", G, (7.6, -2.4, -0.4), (8.0, -0.3, 0.1), DARK),
        ("guardBottom", G, (5.9, -0.6, -0.4), (8.0, -0.3, 0.1), DARK),
        ("selector", G, (3.3, -3.0, 0.65), (3.8, -2.6, 0.8), BLACK),
        # front strap lug under the barrel shroud
        ("strapLug", G, (8.6, -3.2, -0.6), (9.6, -2.0, 0.3), DARK),
        # retracted wire stock
        ("stockRodL", G, (0.3, -3.55, -1.25), (9.0, -3.25, -1.0), ROD),
        ("stockRodR", G, (0.3, -3.55, 0.7), (9.0, -3.25, 0.95), ROD),
        ("stockPlate", G, (-0.15, -4.5, -1.25), (0.25, -2.3, 0.95), DARK),
        # cocking knob (moves back when firing / racking)
        ("cockingKnob", S, (6.6, -5.6, -0.45), (7.4, -5.0, 0.15), BLACK),
        # magazine, inside the grip, sticking out below it
        ("mag", A, (4.15, 0.9, -0.75), (5.75, 4.2, 0.45), MAG),
        ("magBase", A, (4.0, 4.2, -0.85), (5.9, 4.5, 0.55), BLACK),
    ],
}

Z = ((0, 0, 0), (0, 0, 0))  # rest pose
SPEC["animations"] = {
    # every shot: the cocking knob kicks back (it rides on the bolt), a little muzzle climb
    "Fire": gunmodel.anib(2, 0, {
        0: {"parts": {"Model": Z, "OffHand": Z}},
        1: {"kind": "RAND", "parts": {"slideModel0": ((-1.8, 0, 0), (0, 0, 0)), "Model": ((-0.2, 0, 0), (0, 0, 1.5))}},
    }),
    # magazine out (SWITCH unloads), a new one in (LOAD), then rack if the gun ran empty (TRYBOLT)
    "Reload1": gunmodel.anib(48, 0, {
        0: {"parts": {"Model": Z, "OffHand": Z, "ammoModel0": Z}},
        6: {"parts": {"Model": ((0, 0, 0), (12, 0, 4)), "OffHand": ((0, 0.3, 0), (-12, 0, 25))}},
        10: {"kind": "SWITCH", "sound": "MagOut", "shake": 0.8,
             "parts": {"ammoModel0": ((0, 3, 0), (0, 0, 0)), "OffHand": ((0, 0.6, 0), (-12, 0, 25))}},
        18: {"parts": {"ammoModel0": ((0, 14, 0), (0, 0, 10)), "OffHand": ((0, 1.2, 0), (-20, 0, 35))}},
        26: {"parts": {"ammoModel0": ((0, 7, 0), (0, 0, 4)), "OffHand": ((0, 0.8, 0), (-15, 0, 30))}},
        34: {"kind": "LOAD", "sound": "MagIn", "shake": 1.0,
             "parts": {"ammoModel0": Z, "OffHand": ((0, 0.4, 0), (-10, 0, 20))}},
        42: {"kind": "TRYBOLT", "parts": {"Model": ((0, 0, 0), (4, 0, 0)), "OffHand": Z}},
        47: {"parts": {"Model": Z, "OffHand": Z, "ammoModel0": Z}},
    }),
    # charging: the off hand pulls the knob back and lets it go
    "Rack": gunmodel.anib(16, 1, {
        0: {"parts": {"Model": Z, "OffHand": Z, "slideModel0": Z}},
        5: {"parts": {"Model": ((0, 0, 0), (0, 0, 8)), "OffHand": ((0, -0.4, 0), (0, 0, 10))}},
        8: {"sound": "Rack", "shake": 0.6, "parts": {"slideModel0": ((-2.2, 0, 0), (0, 0, 0))}},
        11: {"parts": {"slideModel0": Z}},
        15: {"parts": {"Model": Z, "OffHand": Z, "slideModel0": Z}},
    }),
}

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "..", "..", "dev", "src", "main", "resources", "assets", "deci")
    print(gunmodel.build(SPEC, os.path.normpath(out), os.path.join(here, "models")))
