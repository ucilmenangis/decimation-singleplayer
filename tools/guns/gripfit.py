#!/usr/bin/env python3
"""Grip placement per gun (docs/superpowers/specs/2026-10-21-new-attachments-design.md): Decimation draws every grip
slot attachment (Foregrip, Laser Marker, Flashlight, our RK-1) at one spot for all guns (GunItemRenderer.
renderAttachments, grip translate (-3.2, 0.8, 0) model units): on the M4A4 its Foregrip lands behind and below the
stock and the Laser inside the handguard. Measured here for every gun and grip: the gun's underside in its front part
(between the magazine and the muzzle; pistols further forward, ahead of the trigger guard), and the move that puts the
grip's top there, centred on the gun's line (z -0.15). Decimation's foregrip.bmodel hangs its 8 parts from a parent
"grip" whose own plate sits 7.5 behind them (hidden in the game's copy by fixes/GripPlacement, left out here); the
magazine in that file (ammoModel1..4, children of an ammoModel0 it never declares) is never drawn: BModelLoader drops
it, and study.py does the same. Every gun of the jar and ours (sightfit.OURS: run the gun generators first).
Output (numbers only): dev/src/main/resources/assets/deciworldgen/grip_offsets.txt, "gun grip dx dy dz";
fixes/GripPlacement moves each grip on each gun at startup (Deci.offsetAttachment).
    python3 tools/guns/gripfit.py            # every gun and grip, writes the table
    python3 tools/guns/gripfit.py --check    # every pair placed by the table: touching the underside, under the gun,
                                             # clear of the magazine and the trigger guard; exit 1 listing the others
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import study  # noqa: E402
from sightfit import OURS  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen", "grip_offsets.txt")
GRIPS = {"rk1": "rifle", "foregrip": "rifle", "laser": "all", "flashlight": "all"}
ZC = -0.15


STRAY = {"foregrip": re.compile(r"^grip$")}                   # the foregrip's stray plate (also GripPlacement)
FRONT = {"pistol": 0.62, "revolver": 0.62}                    # how far from the magazine to the muzzle the grip goes
OVERLAP = 0.05                                                # the grip's top sits this far into the underside


def raw_grip(gun, grip):
    """The grip's parts where Decimation alone draws them on this gun (the fixed spot), stray parts left out."""
    os.environ["STUDY_NO_GRIP_OFFSETS"] = "1"
    try:
        g = study.with_attachments(study.load(gun), [grip])
    finally:
        del os.environ["STUDY_NO_GRIP_OFFSETS"]
    drop = STRAY.get(grip)
    return [p for p in g.parts if p.name.startswith(grip + "_") and not (drop and drop.match(p.name[len(grip) + 1:]))]


def bottom_at(part, x):
    """The part's underside (largest y) at x: its bottom face's corners interpolated along x (slanted undersides)."""
    v = sorted(part.verts(), key=lambda q: -q[1])[:4]
    lo, hi = min(v, key=lambda q: q[0]), max(v, key=lambda q: q[0])
    if hi[0] - lo[0] < 1e-6:
        return max(q[1] for q in v)
    t = min(1.0, max(0.0, (x - lo[0]) / (hi[0] - lo[0])))
    return lo[1] + t * (hi[1] - lo[1])


def fit(gun, grip):
    """(dx, dy, dz) that puts the grip's top centre under the gun's front part, or None (no underside found)."""
    g = study.load(gun)
    parts = raw_grip(gun, grip)
    if not parts:
        return None
    gx0, gx1 = min(q[0] for p in parts for q in p.verts()), max(q[0] for p in parts for q in p.verts())
    top = min(q[1] for p in parts for q in p.verts())
    gz = (min(q[2] for p in parts for q in p.verts()) + max(q[2] for p in parts for q in p.verts())) / 2
    w = gx1 - gx0
    body = [p for p in g.parts if not p.name.startswith("ammoModel")]
    mags = [p for p in g.parts if p.name.startswith("ammoModel")]
    xs = [q[0] for p in body for q in p.verts()]
    muzzle = g.header.get("flamePos", (max(xs), 0, 0))[0]
    x0 = max(q[0] for p in mags for q in p.verts()) if mags else None
    if x0 is None or x0 > muzzle - 1.0 - w:   # no magazine, or ammunition ahead of the muzzle (RPG-7 rocket, crossbow
        x0 = min(xs) + 0.45 * (max(xs) - min(xs))  # bolt): the front part starts 45 % along the gun
    tx = x0 + FRONT.get(g.cat, 0.4) * (muzzle - x0)
    tx = max(x0 + 0.3 + w / 2, min(muzzle - 0.3 - w / 2, tx))
    hz = (max(q[2] for p in parts for q in p.verts()) - min(q[2] for p in parts for q in p.verts())) / 2
    h = max(q[1] for p in parts for q in p.verts()) - top
    mv = [q for p in mags for q in p.verts()]
    best = None
    x = tx
    while x <= muzzle - 0.3 - w / 2 + 1e-6 or best is None:
        under = underside(body, x - w / 2, x + w / 2, ZC - hz, ZC + hz)
        if under is not None:
            box = (x - w / 2 - 0.05, x + w / 2 + 0.05, under - 0.1, under + h, ZC - hz, ZC + hz)
            corners = [(cx, cy, cz) for cx in box[0:2] for cy in box[2:4] for cz in box[4:6]]
            clear = not any(box[0] < q[0] < box[1] and box[2] < q[1] < box[3] and box[4] < q[2] < box[5] for q in mv) and \
                not any(study.inside(m, c) for m in mags for c in corners)
            if best is None:
                best = (x, under)          # the first spot found, kept when nothing ahead is clear of the magazine
            if clear:
                best = (x, under)
                break
        if x > muzzle - 0.3 - w / 2:
            break
        x += 0.1                           # a compact gun (G36C): step forward until clear of the magazine
    if best is None:
        return None
    x, under = best
    return x - (gx0 + gx1) / 2, under - OVERLAP - top, ZC - gz


def underside(body, x0, x1, z0, z1):
    """The lowest point (largest y) of the gun over a footprint: each part crossing it, its bottom face at the
    footprint's ends and middle. The same rule places a grip (fit) and judges it (check)."""
    under = None
    for p in body:
        v = p.verts()
        if max(q[0] for q in v) < x0 or min(q[0] for q in v) > x1 or max(q[2] for q in v) < z0 or min(q[2] for q in v) > z1:
            continue
        lo, hi = min(q[0] for q in v), max(q[0] for q in v)
        y = max(bottom_at(p, x) for x in (max(lo, x0), (max(lo, x0) + min(hi, x1)) / 2, min(hi, x1)))
        under = y if under is None else max(under, y)
    return under


def guns():
    """Every gun name to place grips on: the jar's, then ours ("ours:NAME") that exist here."""
    out = []
    for name in study.all_names() + ["ours:" + n for n in OURS]:
        try:
            out.append((name, study.load(name)))
        except SystemExit:          # a generated gun of ours not generated in this checkout
            continue
    return out


def write():
    rows = []
    for name, g in guns():
        for grip, cat in sorted(GRIPS.items()):
            if cat != "all" and g.cat != cat:
                continue
            d = fit(name, grip)
            if d:
                rows.append("%s %s %.3f %.3f %.3f" % (name.split(":")[-1], grip, d[0], d[1], d[2]))
    with open(OUT, "w") as f:
        f.write("# gun grip dx dy dz: tools/guns/gripfit.py, model units in the gun's space (fixes/GripPlacement)\n")
        f.write("\n".join(rows) + "\n")
    print("%d grip offsets -> %s" % (len(rows), OUT))


def check():
    """Every gun and grip it takes, as the game would place them; problems per pair. Returns the bad pairs."""
    bad = []
    for name, g in guns():
        for grip, cat in sorted(GRIPS.items()):
            if cat != "all" and g.cat != cat:
                continue
            problem = placed_problem(name, grip)
            if problem:
                bad.append((name, grip, problem))
    return bad


def placed_problem(gun, grip):
    """Why the grip is not right on the gun as placed (study.with_attachments applies the table), or None: its top on
    the gun's underside over its footprint (within 0.12), nothing of the gun lower than its top by more than 0.15
    there, no magazine part in it."""
    g = study.with_attachments(study.load(gun), [grip])
    mine = [p for p in g.parts if p.name.startswith(grip + "_")]
    body = [p for p in g.parts if not p.name.startswith(grip + "_") and not p.name.startswith("ammoModel")]
    mags = [p for p in g.parts if p.name.startswith("ammoModel")]
    if not mine:
        return "no parts"
    V = [q for p in mine for q in p.verts()]
    top = min(q[1] for q in V)
    under = underside(body, min(q[0] for q in V), max(q[0] for q in V), min(q[2] for q in V), max(q[2] for q in V))
    if under is None:
        return "nothing of the gun above it"
    if under < top - 0.02:
        return "floats %.2f below the gun" % (top - under)
    if under > top + 0.15:
        return "inside the gun (gun parts reach %.2f below its top)" % (under - top)
    if any(study.inside(m, q) for m in mags for q in V) or any(study.inside(p, q) for p in mine for m in mags for q in m.verts()):
        return "in the magazine"
    return None


def boxes_meet(a, b):
    va, vb = a.verts(), b.verts()
    return all(min(q[i] for q in va) < max(q[i] for q in vb) and min(q[i] for q in vb) < max(q[i] for q in va) for i in range(3))


if __name__ == "__main__":
    if not sys.argv[1:]:
        write()
        sys.exit(0)
    if sys.argv[1:2] == ["--check"]:
        bad = check()
        for gun, grip, why in bad:
            print("%-14s %-10s %s" % (gun, grip, why))
        print("%d pairs not right" % len(bad))
        sys.exit(1 if bad else 0)
