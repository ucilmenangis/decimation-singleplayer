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
# The M4A4's handguard (study.py): bottom rail at y -1.30 for x 8.5 .. 14.7, 2.2 tall (rail top -3.5).
# Size (user, 21 Oktober 2026: "its to big for decimation guns ... same as foregrip, use that as baseline size later if
# we want to add new grip in future"): Decimation's Foregrip is the baseline for every grip of ours: 1.0 long, 2.7
# from the rail down, 0.8 thick. Decimation draws grips far slimmer than real (its Foregrip body is 0.36 of the M4A4
# handguard's height, a real one about 0.7), so never size a grip from a photo's proportions.
# Angle (user: "its actually not vertical grip, it bit sway to left ... the grip use align 45%"; measured in the
# reference's end and bottom views, docs/references/grip_rails.md section 2): the clamp sits flat on the bottom rail and
# the grip hangs from an angled mount ROLLED 45 degrees about the bore toward the gun's left. The gun's left is +z (the
# M4A4's selector marks, the "1" on its lower, face +z).
CX, TOP = 10.7, -1.35         # grip centre along the gun; clamp top 0.05 inside the rail (it hugs it)
ROLL = 45.0                   # degrees from straight down toward +z (the gun's left)


def roll(parts, y0, z0, deg=ROLL):
    """Turn parts built straight down about the x axis through (y0, z0): down (+y) leans toward +z by deg."""
    import math
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    for p in parts:
        for q in p.c.values():
            dy, dz = q[1] - y0, q[2] - z0
            q[1], q[2] = y0 + dy * c - dz * s, z0 + dy * s + dz * c
    return parts


def rk1():
    """Zenit RK-1 on an angled mount, sized to Decimation's Foregrip: a flat rail clamp with jaws and a cross bolt, an
    angled mount block under it, then the grip rolled 45 degrees to the left: a short neck, the round body with
    checkered panels between smooth bands, a knurled ring and a flat end cap."""
    P = []

    def add(*parts, pair=False, into=None):
        out = P if into is None else into
        for p in parts:
            out.append(p)
            if pair:
                out.append(p.mirror())

    METAL = (44, 44, 47)       # the clamp and mount: black anodised aluminium, a shade lighter than the polymer
    BODY = (36, 36, 38)
    DARK = (21, 21, 23)
    EDGE = (52, 52, 55)
    RUB = (25, 25, 27)         # the checkered panels: a step darker than the body, so they read
    y = TOP

    # --- clamp: a flat block under the rail (the Foregrip's 1.0 long footprint plus its rounded ends), jaws hugging
    # the rail's sides, one cross bolt (nut on the right, -z)
    add(part("clamp", G, METAL, (CX - 0.6, y, ZC - 0.4), (CX + 0.6, y + 0.22, ZC + 0.4))
        .inset("y", 1, x=(0.06, 0.06), z=(0.05, 0.05)))
    add(part("jaw", G, METAL, (CX - 0.6, y - 0.12, ZC + 0.33), (CX + 0.6, y + 0.04, ZC + 0.41))
        .inset("y", 0, z=(0.0, 0.04)), pair=True)
    add(*oct_z("bolt", G, EDGE, CX, y + 0.11, ZC + 0.38, ZC + 0.47, 0.07))
    add(*oct_z("nut", G, DARK, CX, y + 0.11, ZC - 0.47, ZC - 0.38, 0.08))
    y += 0.22

    # --- angled mount: a block under the clamp whose lower face slopes 45 degrees up toward the left (+z), the face
    # the rolled grip seats on [the B-25U's real shape not verified]
    W = 0.38
    py = y + 0.5               # the grip's roll point, on the slope's centre line
    add(part("mount", G, METAL, (CX - 0.5, y - 0.02, ZC - W), (CX + 0.5, py + W, ZC + W)).inset("z", 1, y=(0.0, 2 * W)))

    # --- the grip, built straight down from the roll point, then rolled
    R = 0.42                   # 0.84 thick: the Foregrip's 0.8
    G0 = []
    g = py
    add(part("neck", G, BODY, (CX - R, g, ZC - R), (CX + R, g + 0.3, ZC + R)).inset("y", 0, x=(0.1, 0.1), z=(0.1, 0.1)),
        into=G0)
    g += 0.3
    L = 1.45
    add(*oct_y("body", G, BODY, CX, ZC, g, g + L, R), into=G0)
    for name, a, b in (("checkF", (CX + R - 0.03, g + 0.25, ZC - 0.18), (CX + R + 0.03, g + L - 0.25, ZC + 0.18)),
                       ("checkB", (CX - R - 0.03, g + 0.25, ZC - 0.18), (CX - R + 0.03, g + L - 0.25, ZC + 0.18))):
        add(part(name, G, RUB, a, b).inset("x", 1 if name == "checkF" else 0, y=(0.03, 0.03), z=(0.03, 0.03)), into=G0)
    add(part("checkS", G, RUB, (CX - 0.18, g + 0.25, ZC + R - 0.03), (CX + 0.18, g + L - 0.25, ZC + R + 0.03))
        .inset("z", 1, x=(0.03, 0.03), y=(0.03, 0.03)), pair=True, into=G0)
    for by in (g + 0.16, g + L - 0.21):                # the smooth bands' grooves above and below the checkering
        add(part("bandF", G, DARK, (CX + R - 0.03, by, ZC - 0.2), (CX + R + 0.02, by + 0.05, ZC + 0.2)), into=G0)
        add(part("bandB", G, DARK, (CX - R - 0.02, by, ZC - 0.2), (CX - R + 0.03, by + 0.05, ZC + 0.2)), into=G0)
        add(part("bandS", G, DARK, (CX - 0.2, by, ZC + R - 0.03), (CX + 0.2, by + 0.05, ZC + R + 0.02)), pair=True,
            into=G0)
    g += L
    add(*oct_y("ring", G, EDGE, CX, ZC, g, g + 0.24, R + 0.03), into=G0)
    add(*ring_ribs_y("ringRib", G, DARK, CX, ZC, g + 0.02, g + 0.22, R + 0.03, n=16, size=0.05), into=G0)
    g += 0.24
    add(*oct_y("cap", G, BODY, CX, ZC, g, g + 0.2, R - 0.02), into=G0)
    add(*oct_y("capFace", G, DARK, CX, ZC, g + 0.17, g + 0.23, R - 0.12), into=G0)
    P += roll(G0, py, ZC)
    return P


if __name__ == "__main__":
    if sys.argv[1:2] == ["--check"]:
        sys.exit(max(sights.check(n, slot="grip", lo=15, hi=90) for n in sys.argv[2:] or ["rk1"]))
    print(sights.write("rk1", rk1(), None, slot="grip", shift=SHIFT))
