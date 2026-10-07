#!/usr/bin/env python3
"""Check the graded yards of procedural city lots (worldgen 0.13+).

    python3 tools/gradescan.py WORLD_DIR SERVER_LOG

For every city building in the log whose whole lot is populated, reads the
surface height of each lot column outside the walls and reports:
  ring   largest |surface - floor| on the 1 block ring around the walls
         (should be 0 or 1: the yard meets the floor at the door);
  steep  share of neighbouring yard columns more than 1 block apart;
  edge   largest |lot edge surface - sidewalk surface| just outside the lot;
  cars   wrecks parked in the yard, by metadata (4 / 2 = along x, nose-in).
Decimation blocks other than roads count as props, not ground (their ids,
read from level.dat, can be below 256: BlockWreckage1..5 are 176..180).
Lot origin comes from the id b<cellX>_<cellZ>_<lot>, as in CityDistrict.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402
from wallscan import populated  # noqa: E402

CELL, LOT, OFFS = 64, 26, (7, 36)
# air, plants, snow layer, leaves, logs, vines, cocoa, cobweb
SOFT = {0, 6, 30, 31, 32, 37, 38, 39, 40, 78, 18, 161, 17, 162, 106, 175, 83, 81, 86, 103, 111, 127}


def props(w):
    if not hasattr(w, "_props"):
        w._props = {v for k, v in w.registry().items()
                    if k.startswith("deci:") and not k.startswith("deci:BlockRoad")}
    return w._props


def surface(w, x, z, top):
    for y in range(top, 4, -1):
        b = w.block(x, y, z)
        if b is None:
            return None
        if b in (8, 9, 10, 11):
            return None  # water / lava: the grader leaves those columns alone
        if b in SOFT or b in props(w):
            continue  # vegetation or a Decimation prop (car, crate...)
        return y
    return None


def main():
    world_dir, log = sys.argv[1], sys.argv[2]
    w = wc.World(world_dir)
    rx = re.compile(r"city (\w+) (\d+)x(\d+), (\d+) floor\(s\), \w+, footprint (-?\d+),(-?\d+) "
                    r"b(-?\d+)_(-?\d+)_(\d) at -?\d+,(-?\d+),-?\d+")
    worst_ring = worst_edge = 0
    steep_all = pairs_all = checked = 0
    cars = {}
    wrecks = {v for k, v in w.registry().items() if k.startswith("deci:BlockWreckage")}
    for line in open(log, errors="ignore"):
        m = rx.search(line)
        if not m:
            continue
        kind, wd, ld, fl, fx, fz, cx, cz, lot, by = m.groups()
        wd, ld, fx, fz, cx, cz, lot, by = map(int, (wd, ld, fx, fz, cx, cz, lot, by))
        lx0, lz0 = cx * CELL + OFFS[lot & 1], cz * CELL + OFFS[lot >> 1]
        lx1, lz1 = lx0 + LOT - 1, lz0 + LOT - 1
        owners = {((x - 8) >> 4, (z - 8) >> 4) for x in range(lx0 - 1, lx1 + 2) for z in range(lz0 - 1, lz1 + 2)}
        if not all(populated(world_dir, a, b) for a, b in owners):
            continue
        checked += 1
        bx1, bz1 = fx + wd - 1, fz + ld - 1
        inside = lambda x, z: fx <= x <= bx1 and fz <= z <= bz1
        h = {}
        for x in range(lx0, lx1 + 1):
            for z in range(lz0, lz1 + 1):
                if not inside(x, z):
                    h[(x, z)] = surface(w, x, z, by + 40)
        for (x, z), y in h.items():
            if y is not None and w.block(x, y + 1, z) in wrecks:
                m = w.meta(x, y + 1, z)
                cars[m] = cars.get(m, 0) + 1
        ring = 0
        for (x, z), y in h.items():
            if y is not None and fx - 1 <= x <= bx1 + 1 and fz - 1 <= z <= bz1 + 1:
                ring = max(ring, abs(y - by))
        steep = pairs = 0
        for (x, z), y in h.items():
            for n in ((x + 1, z), (x, z + 1)):
                if n in h and y is not None and h[n] is not None:
                    pairs += 1
                    steep += abs(y - h[n]) > 1
        edge, edge_at = 0, None
        for x in range(lx0, lx1 + 1):
            for (z, zo) in ((lz0, lz0 - 1), (lz1, lz1 + 1)):
                if (x, z) in h and h[(x, z)] is not None:
                    o = surface(w, x, zo, by + 40)
                    if o is not None and abs(h[(x, z)] - o) > edge:
                        edge, edge_at = abs(h[(x, z)] - o), (x, zo)
        for z in range(lz0, lz1 + 1):
            for (x, xo) in ((lx0, lx0 - 1), (lx1, lx1 + 1)):
                if (x, z) in h and h[(x, z)] is not None:
                    o = surface(w, xo, z, by + 40)
                    if o is not None and abs(h[(x, z)] - o) > edge:
                        edge, edge_at = abs(h[(x, z)] - o), (xo, z)
        worst_ring, worst_edge = max(worst_ring, ring), max(worst_edge, edge)
        steep_all += steep
        pairs_all += pairs
        print("b%d_%d_%d %-9s floor %d  ring %d  steep %d/%d  edge %d%s"
              % (cx, cz, lot, kind, by, ring, steep, pairs, edge,
                 "  (outside column %d,%d)" % edge_at if edge > 6 else ""))
    print("lots checked %d, worst ring %d, steep pairs %d/%d, worst edge %d, yard cars by meta %s"
          % (checked, worst_ring, steep_all, pairs_all, worst_edge, cars))


if __name__ == "__main__":
    main()
