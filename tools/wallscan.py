#!/usr/bin/env python3
"""Check every procedural city building in a world for missing walls.

    python3 tools/wallscan.py WORLD_DIR SERVER_LOG

Reads the `city <kind> WxL, N floor(s) <id> at x,y,z` lines from the log,
measures the share of air in each of the 4 outer walls (normal: 0.05..0.35
from windows and decay), and flags sides above 0.6. A flagged side whose
slice-owning chunk is not populated yet is only the edge of the generated
area (fills in when a player gets close); a flagged side with every owner
chunk populated is a real bug. The owner of column (x, z) is the chunk whose
population window covers it: ((x - 8) >> 4, (z - 8) >> 4).
"""
import os
import re
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402


def populated(world_dir, cx, cz):
    f = os.path.join(world_dir, "region", "r.%d.%d.mca" % (cx >> 5, cz >> 5))
    if not os.path.isfile(f):
        return False
    with open(f, "rb") as fh:
        fh.seek(4 * ((cx & 31) + (cz & 31) * 32))
        loc = struct.unpack(">I", fh.read(4))[0]
        if not loc:
            return False
        fh.seek((loc >> 8) * 4096)
        ln, _ = struct.unpack(">IB", fh.read(5))
        return bool(wc.read_nbt(zlib.decompress(fh.read(ln - 1)))["Level"].get("TerrainPopulated"))


def main():
    world_dir, log = sys.argv[1], sys.argv[2]
    w = wc.World(world_dir)
    # v1: "city K WxL, N floor(s) ID at X,Y,Z"; v2 adds ", STYLE, footprint FX,FZ";
    # 0.23 adds ", storey H" before footprint
    # and X,Z are then the plan bounds (1 block vine margin), so use FX,FZ
    rx = re.compile(r"city (\w+) (\d+)x(\d+), (\d+) floor\(s\)(?:, \w+(?:, storey (\d+))?, footprint (-?\d+),(-?\d+))? (b\S+) at (-?\d+),(-?\d+),(-?\d+)")
    bugs = edges = total = 0
    for line in open(log, errors="ignore"):
        m = rx.search(line)
        if not m:
            continue
        kind, wd, ld, fl, sh, fx, fz, bid, x0, by, z0 = m.groups()
        # whole wall height: storey height from the log (0.23+), else 5 (0.19+)
        wall_h = int(fl) * (int(sh) if sh else 5)
        if fx is not None:
            x0, z0 = fx, fz
        wd, ld, fl, x0, by, z0 = int(wd), int(ld), int(fl), int(x0), int(by), int(z0)
        x1, z1 = x0 + wd - 1, z0 + ld - 1
        total += 1
        sides = {"W": [(x0, z) for z in range(z0 + 1, z1)], "E": [(x1, z) for z in range(z0 + 1, z1)],
                 "N": [(x, z0) for x in range(x0 + 1, x1)], "S": [(x, z1) for x in range(x0 + 1, x1)]}
        for name, cols in sides.items():
            air = tot = 0
            owners = set()
            for (x, z) in cols:
                owners.add(((x - 8) >> 4, (z - 8) >> 4))
                for y in range(by + 1, by + wall_h):
                    b = w.block(x, y, z)
                    if b is None:
                        continue
                    tot += 1
                    air += b == 0
            frac = air / tot if tot else 1.0
            if frac <= 0.6:
                continue
            if all(populated(world_dir, cx, cz) for cx, cz in owners):
                bugs += 1
                print("BUG  %s %s side %s %.2f air, owner chunks all populated" % (bid, kind, name, frac))
            else:
                edges += 1
                print("edge %s %s side %s %.2f air (owner chunk not populated yet)" % (bid, kind, name, frac))
    print("buildings %d, real missing walls %d, edge-of-world gaps %d" % (total, bugs, edges))
    sys.exit(1 if bugs else 0)


if __name__ == "__main__":
    main()
