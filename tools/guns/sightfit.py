#!/usr/bin/env python3
"""Sight placement per gun: how far each sight attachment floats above (or sinks into) the gun.

User report (10 Oktober 2026, Honey Badger and Mk18 screenshots): with a red dot fitted, the
sight hangs in the air above the gun. Cause: GunItemRenderer.renderAttachments puts every sight
at one fixed spot, made for receivers whose top in the sight zone is about y -4.45 (Uzi, MP5).
Flat top rifles sit much lower (M4A4 rail y -3.6): their tall folding rear sight used to fill
the gap, and since v0.40.0 that rear sight folds away under a sight (fixes/IronSights), so the
gap shows. Measured here, for every gun and sight: the sight's lowest point against the highest
visible part under it (defaultScope parts excluded: they are hidden while a sight is on).

Output (numbers only, nothing of Decimation's art):
dev/src/main/resources/assets/deciworldgen/sight_offsets.txt, one line "gun sight dy" for every
pair whose gap is more than TOL; fixes/SightPlacement moves that sight down by dy (negative:
up) on that gun at startup (Deci.offsetAttachment). While aiming, ScopeZoom centres the sight
on the screen again (it learns where the glass is), so the aim stays right.

    python3 tools/guns/sightfit.py            # all Decimation guns and ours, writes the table
    python3 tools/guns/sightfit.py m4a4 uzi   # print only
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import study  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen", "sight_offsets.txt")
SIGHTS = ["reddot", "2x", "4x", "8x", "dragunovScope", "eotech558", "ta11acog"]
OURS = ["mac10", "ump9", "hk416", "hk416tan", "mk18"]
TOL = 0.06       # gaps this small are left alone (Decimation's own fits are within it)


def bbox(parts):
    v = [q for p in parts for q in p.verts()]
    return [(min(q[i] for q in v), max(q[i] for q in v)) for i in range(3)]


DESIGN_TOP = -4.45   # receiver top Decimation's sights are placed for (red dot bottom -4.43)
ZONE = (2.0, 3.5)    # where every sight sits on the gun (red dot 2.4 .. 2.8, scopes 0.8 .. 5.5)
LONG = 1.0           # a receiver panel or rail: at least this long (sight ears and pins are shorter)
# guns whose receiver ends before the sight zone: every sight moves by dx (model units, - = back).
# AS Val (user review 11 Oktober 2026, docs/shots/milbase_v0.42.1_review/user_100.png): its
# receiver runs x -6.7 .. 2.6 and the handguard starts there, so the sights (x 0.8 .. 5.5) sat
# on the handguard ahead of the bolt and ejection port; -4 puts them over the receiver.
SHIFT = {"asval": -4.0}


def rail_top(gun_name):
    """Top of the receiver / rail in the sight zone: the highest long part there, iron sights
    (defaultScope, hidden under a sight) and the magazine excluded."""
    g = study.load(gun_name)
    dx = SHIFT.get(gun_name.split(":")[-1], 0.0)
    zone = (ZONE[0] + dx, ZONE[1] + dx)
    top = None
    for p in g.parts:
        if p.name.startswith(("defaultScope", "ammo")):
            continue
        (a0, a1), (b0, b1), (c0, c1) = bbox([p])
        if a1 - a0 < LONG or a1 < zone[0] or a0 > zone[1] or c1 < -0.45 or c0 > 0.15:
            continue
        top = b0 if top is None else min(top, b0)
    if top is None:
        return None
    # rail teeth (short, a little above the rail base): the sight's clamp sits on them
    teeth = top
    for p in g.parts:
        if p.name.startswith(("defaultScope", "ammo")):
            continue
        (a0, a1), (b0, b1), (c0, c1) = bbox([p])
        if a1 < zone[0] or a0 > zone[1] or c1 < -0.45 or c0 > 0.15:
            continue
        if top - 0.15 <= b0 < top and b1 >= top - 0.02:
            teeth = min(teeth, b0)
    return teeth


def gap(gun_name, sight=None):
    """dy > 0: the sights float that much above this gun's rail (they are placed for -4.45)."""
    top = rail_top(gun_name)
    return None if top is None else (top - DESIGN_TOP, top, DESIGN_TOP)


def main():
    names = sys.argv[1:]
    write = not names
    if write:
        names = study.all_names() + ["ours:" + n for n in OURS]
    rows = []
    for n in names:
        try:
            gun = study.load(n)
        except SystemExit:
            continue
        if gun.cat in ("rocket", "crossbow"):
            continue
        for s in SIGHTS:
            r = gap(n, s)
            if r is None:
                continue
            dy, top, bottom = r
            if not write and s == SIGHTS[0]:
                print("%-14s rail top %.2f -> sights %+.2f" % (n, top, dy))
            dx = SHIFT.get(n.split(":")[-1], 0.0)
            if dy > TOL or dx:           # only down: a gun whose top is higher keeps Decimation's fit
                rows.append((n.split(":")[-1], s, round(max(dy, 0.0) if dy > TOL else 0.0, 3), dx))
    if write:
        with open(OUT, "w") as f:
            f.write("# gun sight dy [dx]: move that sight down by dy and forward by dx model units on that gun\n"
                    "# (tools/guns/sightfit.py)\n")
            for r in rows:
                f.write(("%s %s %s %s\n" % r) if r[3] else ("%s %s %s\n" % r[:3]))
        print("%d offsets -> %s" % (len(rows), OUT))


if __name__ == "__main__":
    main()
