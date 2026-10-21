#!/usr/bin/env python3
"""Our grip attachments (docs/superpowers/specs/2026-10-21-new-attachments-design.md): the RK-1 Foregrip, after the
Zenit RK-1 on a B-25U rail mount of the user's tarkovbot build (docs/shots/refcapture_v0.65.0/my_preset_close,
REFERENCE ONLY: Battlestate Games' models, never copied). Our own model, texture and icon in Decimation's style,
registered by fixes/NewGrips as deci:rk1.
Placement: Decimation draws a grip slot attachment at its file coordinates plus (-3.2, 0.8, 0) model units
(GunItemRenderer.renderAttachments' grip translate), the same spot on every gun; fixes/GripPlacement then moves each
grip under each gun's own handguard (tools/guns/gripfit.py). Parts are written here in GUN space for the M4A4 (the
grip hangs from its handguard's bottom rail, centre z -0.15) and shifted into the file at the end.
    python3 tools/guns/grips.py            # writes the model, texture and icon under assets/deci
    python3 tools/guns/grips.py --check    # the model exists, its part count, seated on the M4A4
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sights  # noqa: E402

from gunmodel import ZC, part  # noqa: E402
from sights import oct_y, oct_z, ring_ribs_y  # noqa: E402

SHIFT = (-3.2, 0.8, 0.0)      # renderAttachments' grip translate in model units
G = "gripModel"
# The M4A4's handguard (study.py): bottom rail at y -1.30 for x 8.5 .. 14.7, 2.2 tall (rail top -3.5). The reference
# (my_preset_close side.png) puts the RK-1 at 36 % of the handguard from the receiver, its whole height (clamp to cap)
# 1.81 times the handguard's and its body 0.71 times: on the M4A4 3.8 tall, 1.45 thick (a bit slimmer than the
# reference, as Decimation draws). Real RK-1: about 100 mm with the clamp, a 35 to 39 mm body [real sizes not verified].
CX, TOP = 10.7, -1.35         # grip centre along the gun; clamp top 0.05 inside the rail (it hugs it)


def rk1():
    """Zenit RK-1 on a B-25U clamp: flat rail clamp with jaws and two cross bolts, a squared neck rounding into the
    body, the round body with checkered panels between smooth bands, a knurled ring and a flat end cap."""
    P = []

    def add(*parts, pair=False):
        for p in parts:
            P.append(p)
            if pair:
                P.append(p.mirror())

    METAL = (44, 44, 47)       # the clamp: black anodised aluminium, a shade lighter than the polymer
    BODY = (36, 36, 38)
    DARK = (21, 21, 23)
    EDGE = (52, 52, 55)
    RUB = (25, 25, 27)         # the checkered panels: a step darker than the body, so they read
    y = TOP

    # --- B-25U clamp: a flat block under the rail, jaws hugging the rail's sides, two cross bolts (nuts on the right)
    add(part("clamp", G, METAL, (CX - 0.95, y, ZC - 0.5), (CX + 0.95, y + 0.32, ZC + 0.5))
        .inset("y", 1, x=(0.08, 0.08), z=(0.06, 0.06)))
    add(part("jaw", G, METAL, (CX - 0.95, y - 0.16, ZC + 0.42), (CX + 0.95, y + 0.06, ZC + 0.52))
        .inset("y", 0, z=(0.0, 0.05)), pair=True)
    for bx in (CX - 0.55, CX + 0.55):
        add(*oct_z("bolt", G, EDGE, bx, y + 0.17, ZC + 0.47, ZC + 0.58, 0.09))
        add(*oct_z("nut", G, DARK, bx, y + 0.17, ZC - 0.58, ZC - 0.47, 0.1))
    y += 0.32

    # --- neck: no waist (the reference runs the body straight up into the clamp): as wide as the body, its top
    # corners rounded off under the clamp
    R = 0.72
    neck = part("neck", G, BODY, (CX - R, y - 0.03, ZC - R), (CX + R, y + 0.42, ZC + R))
    add(neck.inset("y", 0, x=(0.16, 0.16), z=(0.16, 0.16)))
    y += 0.42

    # --- body: round, checkered panels front and back and on both sides between two smooth bands
    add(*oct_y("body", G, BODY, CX, ZC, y, y + 2.3, R))
    for name, a, b in (("checkF", (CX + R - 0.03, y + 0.42, ZC - 0.3), (CX + R + 0.04, y + 1.9, ZC + 0.3)),
                       ("checkB", (CX - R - 0.04, y + 0.42, ZC - 0.3), (CX - R + 0.03, y + 1.9, ZC + 0.3))):
        add(part(name, G, RUB, a, b).inset("x", 1 if name == "checkF" else 0, y=(0.04, 0.04), z=(0.04, 0.04)))
    add(part("checkS", G, RUB, (CX - 0.3, y + 0.42, ZC + R - 0.03), (CX + 0.3, y + 1.9, ZC + R + 0.04))
        .inset("z", 1, x=(0.04, 0.04), y=(0.04, 0.04)), pair=True)
    for by in (y + 0.3, y + 2.0):                      # the smooth bands' grooves above and below the checkering
        add(part("bandF", G, DARK, (CX + R - 0.03, by, ZC - 0.32), (CX + R + 0.02, by + 0.07, ZC + 0.32)))
        add(part("bandB", G, DARK, (CX - R - 0.02, by, ZC - 0.32), (CX - R + 0.03, by + 0.07, ZC + 0.32)))
        add(part("bandS", G, DARK, (CX - 0.32, by, ZC + R - 0.03), (CX + 0.32, by + 0.07, ZC + R + 0.02)), pair=True)
    y += 2.3

    # --- knurled ring and the flat end cap
    add(*oct_y("ring", G, EDGE, CX, ZC, y, y + 0.38, R + 0.04))
    add(*ring_ribs_y("ringRib", G, DARK, CX, ZC, y + 0.03, y + 0.35, R + 0.04, n=16, size=0.07))
    y += 0.38
    add(*oct_y("cap", G, BODY, CX, ZC, y, y + 0.34, R - 0.02))
    add(*oct_y("capFace", G, DARK, CX, ZC, y + 0.3, y + 0.38, R - 0.16))
    return P


if __name__ == "__main__":
    if sys.argv[1:2] == ["--check"]:
        sys.exit(max(sights.check(n, slot="grip", lo=15, hi=90) for n in sys.argv[2:] or ["rk1"]))
    print(sights.write("rk1", rk1(), None, slot="grip", shift=SHIFT))
