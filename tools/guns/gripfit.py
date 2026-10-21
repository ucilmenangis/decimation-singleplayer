#!/usr/bin/env python3
"""Grip placement per gun (docs/superpowers/specs/2026-10-21-new-attachments-design.md, weapons.md section 8): Decimation
draws every grip slot attachment (Foregrip, Laser Marker, Flashlight, our RK-1) at one spot for all guns (GunItemRenderer.
renderAttachments, grip translate (-3.2, 0.8, 0) model units): on the M4A4 its Foregrip sat on the magazine and the
Laser inside the handguard. Here, for every gun and grip: a map of the gun's underside (gun_grid: vertical rays through
every part's real faces, a column each 0.05), the run ahead of the last thing hanging below the gun (front_run), and in
it the place nearest the target where the underside is flat over the grip's seat (spot, fit); lights with no such place
go on the gun's right side (side_fit, the P90). The seat (seat()) is the face that bears: the RK-1's clamp, not its rail
jaws. Decimation's foregrip.bmodel hangs its 8 parts from a parent "grip" whose own plate sits 7.5 behind them (hidden
in the game's copy by fixes/GripPlacement, left out here); the magazine in that file (ammoModel1..4, children of an
ammoModel0 it never declares) is never drawn: BModelLoader drops it, and study.py does the same. Every gun of the jar and
ours (sightfit.OURS: run the gun generators first).
Output (numbers only): dev/src/main/resources/assets/deciworldgen/grip_offsets.txt, "gun grip dx dy dz";
fixes/GripPlacement moves each grip on each gun at startup (Deci.offsetAttachment).
    python3 tools/guns/gripfit.py            # every gun and grip, writes the table (about 2 minutes)
    python3 tools/guns/gripfit.py --check    # every pair as placed, judged on the placed parts alone (placed_problem:
                                             # rays through them, not the fit's map); exit 1 listing the others
The first version placed by the lowest point of any part touching the footprint and its check reused that measure:
it passed while 37 grips floated (final review, 21 Oktober 2026). A check must never reuse the fit's own measure.
"""
import math
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
FRONT = {"pistol": 0.0, "revolver": 0.0}                      # share of the free front run behind the grip (pistols:
                                                              # right ahead of the trigger guard, as real pistol lights)
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


STEP = 0.05          # grid spacing (model units) of the gun's underside map
HANG = 2.3           # deeper than this below the bore (flamePos y + 0.85, Decimation's convention) is a pistol grip,
                     # a trigger guard, a handle or a gun's own grip; handguards, barrels and tubes rest 0.3 .. 2.2 below
NOSE = 0.6           # pistols: deeper than the frame's nose by this is the trigger guard (the Glock's is 1.8 below the bore)
_GRIDS = {}


def seat(parts):
    """The grip's seat, the face that bears on the gun (x0, x1, z0, z1, top): every part level (within 0.05) with the
    widest part at the grip's top. The RK-1's clamp (its rail jaws rise 0.16 above it beside a rail and do not bear:
    measured from them the clamp hung 0.11 low, and on the RPK their width reached the folded bipod legs); Decimation's
    Foregrip's two top blocks together."""
    top = min(q[1] for p in parts for q in p.verts())
    near = [p for p in parts if min(q[1] for q in p.verts()) <= top + 0.25]

    def area(p):
        v = p.verts()
        return (max(q[0] for q in v) - min(q[0] for q in v)) * (max(q[2] for q in v) - min(q[2] for q in v))
    level = min(q[1] for q in max(near, key=area).verts())
    flat = [q for p in near if abs(min(q[1] for q in p.verts()) - level) <= 0.05 for q in p.verts()]
    return min(q[0] for q in flat), max(q[0] for q in flat), min(q[2] for q in flat), max(q[2] for q in flat), level


def gun_grid(gun):
    """The gun's underside map, cached per gun: {x0, xs, zs, under[i][j] (lowest body point, y grows downward, or None),
    mag[i][j] (a magazine part hangs below the body there)}; columns STEP apart, z within 0.8 of the gun's line."""
    if gun in _GRIDS:
        return _GRIDS[gun]
    g = study.load(gun)
    moving = ("ammoModel", "pumpModel")             # magazines, and pumps that slide when the gun is racked
    body = [planes(p) for p in g.parts if not p.name.startswith(moving)]
    mags = [planes(p) for p in g.parts if p.name.startswith(moving)]
    bx = [q[0] for p in g.parts if not p.name.startswith("ammoModel") for q in p.verts()]
    x0, x1 = min(bx), max(bx)
    xs = [x0 + STEP * i for i in range(int((x1 - x0) / STEP) + 1)]
    zs = [ZC + STEP * k for k in range(-16, 17)]
    under, mag = [], []
    for x in xs:
        bs = [sh for sh in body if sh[1][0] <= x <= sh[1][1]]
        ms = [sh for sh in mags if sh[1][0] <= x <= sh[1][1]]
        ur, mr = [], []
        for z in zs:
            low = None
            for sh in bs:
                h = ray(sh, x, z)
                if h and (low is None or h[1] > low):
                    low = h[1]
            mlow = None
            for sh in ms:
                h = ray(sh, x, z)
                if h and (mlow is None or h[1] > mlow):
                    mlow = h[1]
            ur.append(low)
            mr.append(mlow is not None and (low is None or mlow > low - 0.05))
        under.append(ur)
        mag.append(mr)
    fp = g.header.get("flamePos", (x1, 0, 0))
    out = {"x0": x0, "xs": xs, "zs": zs, "under": under, "mag": mag, "cat": g.cat, "muzzle": fp[0], "bore": fp[1] + 0.85}
    _GRIDS[gun] = out
    return out


def front_run(m):
    """(start, end) along x of the gun's front part: from ahead of the last thing hanging below it to the muzzle.
    Hanging (along the gun's line, z within 0.3): a magazine or a pump part below the body, the body more than HANG
    below the bore (pistol grip, trigger guard, handle, the AUG's own grip), on pistols also more than NOSE deeper than
    the frame's nose (the median of the front 40 %). Ammunition ahead of the muzzle (RPG-7 rocket, crossbow bolt) and
    anything within 1.0 of the muzzle do not count."""
    xs, under, mag, bore, muzzle = m["xs"], m["under"], m["mag"], m["bore"], m["muzzle"]
    mid = [k for k, z in enumerate(m["zs"]) if abs(z - ZC) <= 0.3 + 1e-6]
    span = [i for i, x in enumerate(xs) if x <= muzzle - 1.0]
    deep = bore + HANG
    if m["cat"] in ("pistol", "revolver"):
        nose = [under[i][k] for i, x in enumerate(xs) if muzzle - 0.4 * (muzzle - xs[0]) <= x <= muzzle for k in mid
                if under[i][k] is not None]
        if nose:
            deep = min(deep, sorted(nose)[len(nose) // 2] + NOSE)
    last = None
    for i in span:
        lows = [under[i][k] for k in mid if under[i][k] is not None]
        if any(mag[i][k] for k in mid) or (lows and max(lows) > deep):
            last = xs[i]
    start = (last + STEP) if last is not None else xs[0] + 0.45 * (muzzle - xs[0])
    return start, muzzle - 0.2


def spot(m, sx0, sx1, hz):
    """How a seat from sx0 to sx1, half width hz about the gun's line, would sit: (places along it that touch, places
    covered, lowest gun point over it, in the way). The seat's inner part (10 .. 90 % each way, every grid column there)
    sets the lowest point, a slanted stock or a step under it read where it is deepest; 7 places along it must touch
    (the gun at most CONTACT above the seat placed at that point less OVERLAP, somewhere across it): a seat touching only
    at its side edges (the FAMAS-I's handguard skirts) is not resting. In the way: a magazine or pump part over the whole
    seat, or anything at the seat's edges reaching more than REACH below that point."""
    xs, zs, under, mag = m["xs"], m["zs"], m["under"], m["mag"]
    allz = [k for k, z in enumerate(zs) if abs(z - ZC) <= hz + 1e-6]
    inz = [k for k, z in enumerate(zs) if abs(z - ZC) <= 0.8 * hz + 1e-6]
    def idx(x, up):
        return int(math.floor((x - m["x0"]) / STEP + 1e-6)) if up else int(math.ceil((x - m["x0"]) / STEP - 1e-6))
    a0, a1 = idx(sx0, False), idx(sx1, True)
    n0, n1 = idx(sx0 + 0.1 * (sx1 - sx0), False), idx(sx1 - 0.1 * (sx1 - sx0), True)
    if a0 < 0 or a1 >= len(xs) or n1 < n0 or not inz:
        return 0, 0, None, False
    inner = [under[i][k] for i in range(n0, n1 + 1) for k in inz if under[i][k] is not None]
    if not inner:
        return 0, 0, None, any(mag[i][k] for i in range(a0, a1 + 1) for k in allz)
    lowest = max(inner)
    edge = max([under[i][k] for i in range(a0, a1 + 1) for k in allz if under[i][k] is not None] or [lowest])
    blocked = any(mag[i][k] for i in range(a0, a1 + 1) for k in allz) or edge > lowest + REACH
    top = lowest - OVERLAP
    touch = covered = 0
    for n in range(NX):
        i = int(round((sx0 + (sx1 - sx0) * (0.1 + 0.8 * n / (NX - 1)) - m["x0"]) / STEP))
        row = [under[i][k] for k in inz if under[i][k] is not None]
        covered += 1 if row else 0
        touch += 1 if row and min(top - u for u in row) <= CONTACT else 0
    return touch, covered, lowest, blocked


def fit(gun, grip):
    """(dx, dy, dz) that hangs the grip under the gun's front part, its top on the gun's lowest point there, or None.
    Searched along the front run (front_run) for the place nearest the target (FRONT of the free run behind the grip;
    pistols right ahead of the trigger guard) where the gun's underside is flat over the grip's whole length: every one
    of the 7 places covered, at least 6 touching, no magazine in the way."""
    parts = raw_grip(gun, grip)
    if not parts:
        return None
    m = gun_grid(gun)
    gx0, gx1 = min(q[0] for p in parts for q in p.verts()), max(q[0] for p in parts for q in p.verts())
    sx0, sx1, sz0, sz1, top = seat(parts)
    gz = (sz0 + sz1) / 2
    w, hz = gx1 - gx0, (sz1 - sz0) / 2
    mid = (gx0 + gx1) / 2
    start, end = front_run(m)
    target = start + w / 2 + FRONT.get(m["cat"], 0.4) * max(0.0, end - start - w)
    best = None
    tx = start + w / 2
    while tx <= end - w / 2 + 1e-6:
        touch, covered, lowest, blocked = spot(m, sx0 - mid + tx, sx1 - mid + tx, hz)
        good = (lowest is not None and not blocked and covered == NX and touch >= NX - 1
                and lowest <= m["bore"] + HANG)        # never hung from a trigger guard or a handle
        key = (good, -abs(tx - target)) if good else (False, touch, covered, -abs(tx - target))
        if lowest is not None and (best is None or key > best[0]):
            best = (key, tx, lowest)
        tx += STEP
    if (best is None or not best[0][0]) and grip in LIGHTS:
        side = side_fit(gun, parts, start, end, target)
        if side:
            return side
    if best is None:
        return None
    _, tx, lowest = best
    return tx - mid, lowest - OVERLAP - top, ZC - gz


LIGHTS = ("laser", "flashlight")    # may sit on the gun's right side where no underside fits (the P90's hand loop)


def side_fit(gun, parts, start, end, target):
    """A light beside the gun's front on its right (+z), its middle as near the bore's height as fits (within 1.5),
    touching the gun's side, clear of it:
    (dx, dy, dz), or None. For guns with no flat underside ahead of their grip (the P90's front is its hand loop), as
    real P90 lights sit on the side rails. The spot along x nearest the target where the light's inner face, against
    the gun's outermost side there, touches it at 5 of 7 places along its length."""
    g = study.load(gun)
    body = [planes(p) for p in g.parts if not p.name.startswith(("ammoModel", "pumpModel"))]
    bore = gun_grid(gun)["bore"]
    gx0, gx1 = min(q[0] for p in parts for q in p.verts()), max(q[0] for p in parts for q in p.verts())
    gy0, gy1 = min(q[1] for p in parts for q in p.verts()), max(q[1] for p in parts for q in p.verts())
    gz0, gz1 = min(q[2] for p in parts for q in p.verts()), max(q[2] for p in parts for q in p.verts())
    w, h, d = gx1 - gx0, gy1 - gy0, gz1 - gz0
    best = None
    for k in range(31):                                 # heights about the bore, nearest first: 0, +0.1, -0.1, ...
        cy = bore + (0.1 * ((k + 1) // 2) * (1 if k % 2 else -1))
        tx = start + w / 2
        while tx <= end - w / 2 + 1e-6:
            sides = []                                  # the gun's outermost side at each place along the light
            for i in range(NX):
                x = tx - w / 2 + w * (0.1 + 0.8 * i / (NX - 1))
                hits = [r for j in range(NZ) for r in (ray_z(sh, x, cy - h / 2 + h * (0.1 + 0.8 * j / (NZ - 1)))
                                                             for sh in body) if r]
                sides.append(max(r[1] for r in hits) if hits else None)
            real = [v for v in sides if v is not None]
            if len(real) >= NX - 1:
                side = max(real)
                key = (-abs(cy - bore), -abs(tx - target))
                if (sum(1 for v in real if side - v <= CONTACT) >= NX - 2 and (best is None or key > best[0])
                        and clear(body, tx - w / 2, tx + w / 2, cy - h / 2, cy + h / 2, side + 0.02, side - OVERLAP + d)):
                    best = (key, tx, side, cy)
            tx += STEP
        if best is not None:
            break
    if best is None:
        return None
    _, tx, side, cy = best
    return tx - (gx0 + gx1) / 2, cy - (gy0 + gy1) / 2, side - OVERLAP - gz0



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


def planes(part):
    """The part's faces as (outward normal, offset) planes: a shape box is convex, so a point is inside when it is
    behind every plane (study.inside does the same test with a margin)."""
    v = part.verts()
    c = [sum(p[i] for p in v) / 8 for i in range(3)]
    out = []
    for quad, _ in part.quads():
        a, b, d = quad[0], quad[1], quad[3]
        e1 = [quad[2][i] - a[i] for i in range(3)]
        e2 = [d[i] - b[i] for i in range(3)]
        n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
        nl = math.sqrt(sum(x * x for x in n))
        if nl < 1e-9:
            continue                                   # a collapsed face (wedge edge)
        n = [x / nl for x in n]
        m = [sum(p[i] for p in quad) / 4 for i in range(3)]
        if sum(n[i] * (c[i] - m[i]) for i in range(3)) > 0:
            n = [-x for x in n]
        out.append((n, sum(n[i] * m[i] for i in range(3))))
    xs, ys, zs = [q[0] for q in v], [q[1] for q in v], [q[2] for q in v]
    return out, (min(xs), max(xs), min(zs), max(zs), min(ys), max(ys))


def ray(shape, x, z):
    """Where the vertical line at x, z runs inside one part: (top, bottom) y (y grows downward), or None."""
    pl, bb = shape
    if not (bb[0] <= x <= bb[1] and bb[2] <= z <= bb[3]):
        return None
    lo, hi = -1e9, 1e9
    for n, d in pl:
        r = d - n[0] * x - n[2] * z                    # n.y * y <= r
        if abs(n[1]) < 1e-9:
            if r < -1e-6:
                return None
            continue
        t = r / n[1]
        if n[1] > 0:
            hi = min(hi, t)
        else:
            lo = max(lo, t)
        if lo > hi + 1e-6:
            return None
    return lo, hi


def ray_z(shape, x, y):
    """Where the line across the gun (along z) at x, y runs inside one part: (z0, z1), or None."""
    pl, bb = shape
    if not (bb[0] <= x <= bb[1] and bb[4] <= y <= bb[5]):
        return None
    lo, hi = -1e9, 1e9
    for n, d in pl:
        r = d - n[0] * x - n[1] * y                    # n.z * z <= r
        if abs(n[2]) < 1e-9:
            if r < -1e-6:
                return None
            continue
        t = r / n[2]
        if n[2] > 0:
            hi = min(hi, t)
        else:
            lo = max(lo, t)
        if lo > hi + 1e-6:
            return None
    return lo, hi


def columns(x0, x1, z0, z1, nx, nz):
    """Sample columns over a footprint, kept 10 % in from its edges."""
    return [(x0 + (x1 - x0) * (0.1 + 0.8 * i / (nx - 1)), z0 + (z1 - z0) * (0.1 + 0.8 * j / (nz - 1)))
            for i in range(nx) for j in range(nz)]


CONTACT = 0.25        # a column touches when the gun is at most this far above the grip's top (rail slots: 0.2)
REACH = 0.15          # nothing of the gun may reach further than this below the grip's top (into the grip)
NX, NZ = 7, 3         # columns along the grip's top and across it


def placed_problem(gun, grip):
    """Why the grip is not right on the gun as the game places it (study.with_attachments applies the table), or None.
    Judged on the placed parts alone, column by column over the grip's top (vertical rays through the gun's real part
    shapes) over its seat (seat(): the face that bears): along its length at least 4 of 7 places touch the gun (some
    column across the seat there has the gun at most CONTACT above it: a round handguard narrower than the grip touches
    along the middle only), no column has the gun reaching more than REACH below the seat (a trigger guard, a handle, a
    pump inside the grip), no magazine or pump part in the grip."""
    g = study.with_attachments(study.load(gun), [grip])
    mine = [p for p in g.parts if p.name.startswith(grip + "_")]
    if not mine:
        return "no parts"
    moving = ("ammoModel", "pumpModel")
    body = [planes(p) for p in g.parts if not p.name.startswith(grip + "_") and not p.name.startswith(moving)]
    mags = [p for p in g.parts if p.name.startswith(moving)]
    V = [q for p in mine for q in p.verts()]
    bottom = max(q[1] for q in V)
    sx0, sx1, sz0, sz1, top = seat(mine)              # the face that bears (the RK-1's clamp, not its rail jaws)
    cols = columns(sx0, sx1, sz0, sz1, NX, NZ)
    touching, deepest, gaps = set(), None, []
    for k, (x, z) in enumerate(cols):
        hits = [h for h in (ray(s, x, z) for s in body) if h and h[0] < bottom]   # gun material above the grip's bottom
        if not hits:
            continue
        low = max(h[1] for h in hits)                  # the gun's lowest point in this column
        gaps.append(top - low)
        if -REACH <= top - low <= CONTACT:
            touching.add(k // NZ)                      # this place along the grip touches
        deepest = low - top if deepest is None else max(deepest, low - top)
    problem = None
    if deepest is not None and deepest > REACH:
        problem = "the gun reaches %.2f into it" % deepest
    elif len(touching) < 4:
        problem = "floats: %d of %d places along it touch (gaps %s)" % (len(touching), NX, "none above" if not gaps else
                                                                       "%.2f .. %.2f" % (min(gaps), max(gaps)))
    if problem and not (grip in LIGHTS and on_side(mine, body)):   # a light may sit against the gun's side instead
        return problem
    if any(study.inside(m, q) for m in mags for q in V) or any(study.inside(p, q) for p in mine for m in mags for q in m.verts()):
        return "in the magazine"
    return None


def clear(body, x0, x1, y0, y1, z0, z1):
    """Nothing of the gun inside the box (vertical rays on a 7 x 3 grid over it, kept 0.05 in from its faces): a side
    mounted light must not sink into a hand loop or a rail beside the barrel."""
    for x, z in columns(x0, x1, z0, z1, NX, NZ):
        for sh in body:
            r = ray(sh, x, z)
            if r and r[0] < y1 - 0.05 and r[1] > y0 + 0.05:
                return False
    return True


def on_side(mine, body):
    """A light resting against the gun's side: along its length at least 4 of 7 places where the gun's side (the part
    surface across z at the light's height) is at most CONTACT from the light's inner face, nothing of the gun reaching
    more than REACH into it."""
    V = [q for p in mine for q in p.verts()]
    x0, x1, y0, y1 = min(q[0] for q in V), max(q[0] for q in V), min(q[1] for q in V), max(q[1] for q in V)
    z0 = min(q[2] for q in V)
    places = 0
    for i in range(NX):
        x = x0 + (x1 - x0) * (0.1 + 0.8 * i / (NX - 1))
        touch = False
        for j in range(NZ):
            y = y0 + (y1 - y0) * (0.1 + 0.8 * j / (NZ - 1))
            hits = [r for r in (ray_z(sh, x, y) for sh in body) if r and r[0] < z0 + 1.0]
            if not hits:
                continue
            side = max(r[1] for r in hits)
            if side - z0 > REACH:
                return False
            touch |= z0 - side <= CONTACT
        places += 1 if touch else 0
    return places >= 4


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
