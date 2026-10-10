#!/usr/bin/env python3
"""Walk test: can a player reach every ladder, door, room and loot container of a structure?

    python3 tools/props/walkcheck.py dev/run/client/devtest/milbase_1.tsv [--roofs]

Input: a block dump "x y z name meta" (the milbase dev test writes one per size; "# origin" and
"# poi" lines give the camera points; the start is the main road just inside the gate). Written 11 Oktober 2026
after the user could not climb a guard tower: 5 of 6 ladders had a wall or sandbags in front of
them, and checking each ladder's own cells had passed.

A player here: 2 cells tall, climbs a rise of 1 (a jump or a step), drops up to 3, climbs
ladders, walks through doors (the locked TOC door too, flagged) and open trapdoors, over carpet
and props whose collision box is low (<= 0.5) or that are walk through (setClippable). Fences,
walls, panes, concertina wire and every other prop block are in the way (a prop's collision box
is its block cell; the drawn overhang does not block).

Findings (exit 1 when any of the first five is found):
  LADDER  a ladder whose foot cannot be reached from the gate, or whose top / deck cannot
  DOOR    a door without a reachable cell on one of its two sides
  AREA    floor space (a room, a yard) the player cannot get to, 4 cells or more
  LOOT    a loot container with no reachable standing cell within 2 blocks (1 up / down)
  TRAP    places a player can get into but not back out of to the gate
  ROOF    (--roofs) roof and wall tops reachable by jumping, not tower decks: for review
"""
import os
import re
import sys
from collections import deque

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "props"))
import propgeom  # noqa: E402

PASS_VANILLA = ("air", "tallgrass", "double_plant", "red_flower", "yellow_flower", "deadbush",
                "carpet", "torch", "wall_sign", "standing_sign", "snow_layer", "ladder", "vine",
                "flower_pot", "lever", "rail", "redstone_wire", "web")
FENCE_LIKE = ("fence", "cobblestone_wall", "iron_bars", "glass_pane", "stained_glass_pane",
              "nether_brick_fence", "fence_gate")
LOOT = ("Crate", "Cabinet", "CarePackage", "chest", "WaterPallet")
NEVER_PASS = ("BlockConcertinaWire",)


def collision_table():
    """Prop block -> (collision height, walk through) from Decimation's registry code."""
    props = propgeom.registry()
    reg = propgeom.read("decimation/block/PropBlockRegistry.java")
    clip = set()
    for m in re.finditer(r"= new (\w+)\(([^;]*?)\)((?:\.\w+\([^;]*?\))*);", reg):
        if "setClippable" in m.group(3):
            t = re.search(r'"tile\.(\w+)"', m.group(2))
            clip.add(t.group(1) if t else m.group(1))
    out = {}
    for name, p in props.items():
        h = p.size[4] if p.size else 1.0
        out[name] = (h, name in clip)
    return out


def main():
    path = sys.argv[1]
    roofs = "--roofs" in sys.argv
    table = collision_table()
    cells, pois, origin = {}, {}, (0.0, 0.0, 0.0)
    for line in open(path):
        if line.startswith("# origin"):
            f = line.split()
            origin = (float(f[2]), float(f[3]), float(f[4]))
        elif line.startswith("# poi"):
            f = line.split()
            pois[f[2]] = (float(f[3]) - origin[0], float(f[4]), float(f[5]) - origin[2])
        elif not line.startswith("#"):
            x, y, z, name, meta = line.split()
            cells[(int(x), int(y), int(z))] = (name.split(":")[-1], int(meta))

    def kind(x, y, z):
        """'free', 'solid', 'fence', 'door', 'ladder', 'wire' for a cell."""
        c = cells.get((x, y, z))
        if c is None:
            return "free"
        n, m = c
        if n == "ladder":
            return "ladder"
        if "door" in n.lower() and "trap" not in n.lower():
            return "door"
        if n == "trapdoor":
            return "free" if m & 4 else "slab"
        if any(n == k or n.startswith(k) for k in NEVER_PASS):
            return "wire"
        if any(n == k or n.startswith(k) for k in PASS_VANILLA):
            return "free"
        if any(n == k for k in FENCE_LIKE):
            return "fence"
        if n in table:
            h, clip = table[n]
            if clip:
                return "free"
            return "slab" if h <= 0.5 else "solid"
        if n.endswith("_slab") and not m & 8:
            return "slab"
        return "solid"

    def passable(x, y, z):
        return kind(x, y, z) in ("free", "door", "ladder")

    def support(x, y, z):
        return kind(x, y, z) in ("solid", "slab")

    def standable(x, y, z):
        if not (passable(x, y, z) and passable(x, y + 1, z)):
            return False
        if kind(x, y, z) == "ladder":
            return True
        return support(x, y - 1, z) or kind(x, y - 1, z) == "ladder"

    def moves(p):
        x, y, z = p
        out = []
        if kind(x, y, z) == "ladder" or kind(x, y - 1, z) == "ladder":
            for dy in (1, -1):
                q = (x, y + dy, z)
                if passable(*q) and passable(q[0], q[1] + 1, q[2]) and (
                        kind(*q) == "ladder" or kind(x, y, z) == "ladder" or standable(*q)):
                    out.append(q)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            # up 1: the block jumped onto must be a full support, room for the head on the way
            # (never up to y 4 or higher: walls, roofs and decks are reached by ladders only; a
            # route over a slumped HESCO cell onto the wall top is real, but not the way in)
            if y + 1 < 4 and standable(nx, y + 1, nz) and passable(x, y + 2, z) and kind(nx, y, nz) != "fence":
                out.append((nx, y + 1, nz))
            if standable(nx, y, nz):
                out.append((nx, y, nz))
            elif passable(nx, y, nz) and passable(nx, y + 1, nz):
                for ny in range(y - 1, y - 4, -1):
                    if not passable(nx, ny, nz):
                        break
                    if standable(nx, ny, nz):
                        out.append((nx, ny, nz))
                        break
        return out

    # start on the main road just inside the gate (the gate camera stands outside the dump)
    gx, _, gz = pois.get("main_road", pois.get("gate_approach", (0, 0, 0)))
    gx, gz = int(gx), int(gz)
    start = next(((gx + a, y, gz + c) for r in range(0, 6) for a in range(-r, r + 1)
                  for c in range(-r, r + 1) for y in range(1, 4) if standable(gx + a, y, gz + c)), None)
    if start is None:
        print("no standing cell at the gate approach", gx, gz)
        sys.exit(2)
    seen, edges = {start}, {}
    dq = deque([start])
    while dq:
        p = dq.popleft()
        for q in moves(p):
            edges.setdefault(q, []).append(p)
            if q not in seen:
                seen.add(q)
                dq.append(q)
    back = {start}
    dq = deque([start])
    while dq:
        p = dq.popleft()
        for q in edges.get(p, []):
            if q not in back:
                back.add(q)
                dq.append(q)

    def near(x, z):
        if not pois:
            return "-"
        return min(pois, key=lambda k: (pois[k][0] - x) ** 2 + (pois[k][2] - z) ** 2)

    found = []

    def report(k, what, p, text):
        found.append("%-6s %-26s at %3d %3d %3d  %s  (near %s)" % (k, what, p[0], p[1], p[2], text, near(p[0], p[2])))

    # ladders: foot and top
    cols = {}
    for (x, y, z), (n, m) in cells.items():
        if n == "ladder":
            cols.setdefault((x, z), []).append(y)
    for (x, z), ys in sorted(cols.items()):
        lo, hi = min(ys), max(ys)
        if (x, lo, z) not in seen:
            report("LADDER", "ladder", (x, lo, z), "its foot cannot be reached from the gate")
        elif not any(q in seen for q in [(x, hi + 1, z)] + [(x + a, hi + 1, z + b) for a, b in
                                                             ((1, 0), (-1, 0), (0, 1), (0, -1))]):
            report("LADDER", "ladder", (x, hi, z), "its top / deck cannot be reached")
    # doors: both sides
    for (x, y, z), (n, m) in sorted(cells.items()):
        if kind(x, y, z) != "door" or m & 8:
            continue
        sides = [(x + 1, y, z), (x - 1, y, z)] if (m & 3) in (0, 2) else [(x, y, z + 1), (x, y, z - 1)]
        for s in sides:
            if not any((s[0], s[1] + dy, s[2]) in seen for dy in (0, -1, 1)):
                report("DOOR", n, (x, y, z), "no reachable cell at %d %d %d" % s)
        if "Locked" in n:
            found.append("NOTE   %-26s at %3d %3d %3d  locked: needs a military keycard" % (n, x, y, z))
    # loot
    for (x, y, z), (n, m) in sorted(cells.items()):
        if any(k in n for k in LOOT):
            ok = any((x + a, y + b, z + c) in seen for a in range(-2, 3) for c in range(-2, 3)
                     for b in (-1, 0, 1))
            if not ok:
                report("LOOT", n, (x, y, z), "no reachable standing cell within 2")
    # unreachable floor areas and traps, as clusters
    xs = [k[0] for k in cells]
    zs = [k[2] for k in cells]
    floor = set()
    for x in range(min(xs), max(xs) + 1):
        for z in range(min(zs), max(zs) + 1):
            for y in range(1, 12):
                if standable(x, y, z) and kind(x, y, z) != "ladder" and (x, y, z) not in seen \
                        and cells.get((x, y - 1, z)) is not None:
                    floor.add((x, y, z))

    def clusters(points):
        points, out = set(points), []
        while points:
            p = points.pop()
            group, dq2 = [p], deque([p])
            while dq2:
                a = dq2.popleft()
                for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
                    b = (a[0] + d[0], a[1] + d[1], a[2] + d[2])
                    if b in points:
                        points.remove(b)
                        group.append(b)
                        dq2.append(b)
            out.append(group)
        return out

    for g in clusters(floor):
        ground = [p for p in g if p[1] <= 2]
        if len(ground) >= 4:                      # roofs and wall tops are not "areas"
            p = min(ground)
            report("AREA", "%d cells" % len(ground), p, "floor space that cannot be reached")
    for g in clusters(p for p in seen if p not in back):
        report("TRAP", "%d cells" % len(g), min(g), "can be entered but not left")
    if roofs:
        ladder_near = [(x, z) for (x, z) in cols]
        tops = [p for p in seen if p[1] >= 4 and not any(abs(p[0] - a) <= 3 and abs(p[2] - b) <= 3
                                                          for a, b in ladder_near)]
        for g in clusters(tops):
            found.append("ROOF   %-26s at %3d %3d %3d  reachable by jumping (review)" % (
                "%d cells" % len(g), min(g)[0], min(g)[1], min(g)[2]))
    kinds = {}
    for f in found:
        kinds[f.split()[0]] = kinds.get(f.split()[0], 0) + 1
    for f in sorted(found):
        print(f)
    print("reachable cells", len(seen), "findings", {k: v for k, v in kinds.items()})
    bad = sum(v for k, v in kinds.items() if k in ("LADDER", "DOOR", "AREA", "LOOT", "TRAP"))
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
