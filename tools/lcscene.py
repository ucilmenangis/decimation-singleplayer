#!/usr/bin/env python3
"""Alignment test scene for converted Lost Cities content: streets at two
city levels (6 apart, Lost Cities FLOORHEIGHT), buildings with their
ground floor at street level and cellars below, a stairs part between the
levels. Writes one raw schematic (+ groundY sidecar) for /deciworldgen paste.

    python3 tools/lcscene.py DATA_DIR NAMESPACE REGISTRY_WORLD OUT_DIR

Layout in chunks (x right, z down), street row z = 0:
  (0,0) street at level B = A + 6, (0,1) building_bunker1 on level B
  (1,0) street at level A with the stairs part (rises toward x 0)
  (2..7,0) street at level A; buildings on row z 1..2 at level A:
  (1,1) building_warehouse1, (2,1) building_lab1, (3,1) building_factory1,
  (4..6,1..2) multi_militarybase.
Rules from Lost Cities' source (LostCityTerrainFeature): the street
surface block is at the city ground level G, a building's ground floor
part starts at G (its floor layer at G), cellars below, stairs parts at
G + 1 turned toward the higher neighbour (XMIN = no rotation).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lc2schem as lc  # noqa: E402
import lctranslate as lt  # noqa: E402
import mapsurvey as ms  # noqa: E402

LEVEL = 6
BUILDINGS = [((0, 1), "building_bunker1", 1), ((1, 1), "building_warehouse1", 0),
             ((2, 1), "building_lab1", 0), ((3, 1), "building_factory1", 0),
             ((4, 1), "multi_militarybase", 0)]
STAIRS = "stair_deadzone_1"


def main():
    data, ns, regworld, out = sys.argv[1:5]
    os.makedirs(out, exist_ok=True)
    ids = {n: i for i, n in ms.registry(regworld).items()}
    ids["minecraft:air"] = 0
    pack = lc.Pack(data, ns)
    W, L = 8 * 16, 3 * 16
    # deepest cellar decides where street level A sits in the scene
    built = []
    depth = 0
    for (cx, cz), name, lvl in BUILDINGS:
        multi = pack.load("multibuildings", name)
        grid = multi["buildings"] if multi else [["%s:%s" % (ns, name)]]
        cols, grounds = {}, {}
        for gx, row in enumerate(grid):
            for gz, bref in enumerate(row):
                if not pack.load("buildings", bref):
                    bref = "%s:%s/%s" % (ns, name, name)
                cols[(gx, gz)], grounds[(gx, gz)] = lc.building_columns(pack, bref)
        g = max(grounds.values())
        cols = {k: [[[None] * 16 for _ in range(16)]] * (g - grounds[k]) + v for k, v in cols.items()}
        built.append(((cx, cz), name, lvl, cols, g))
        depth = max(depth, g - lvl * LEVEL)
    G = depth + 2          # street level A in the scene
    H = G + LEVEL + 40
    grid = [[[0, 0] for _ in range(W * L)] for _ in range(H)]

    def put(x, y, z, state):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < L) or state is None:
            return
        # "=name": already a 1.7.10 block name (scene ground, roads)
        r = (state[1:], 0) if state.startswith("=") else lt.translate(state)
        if r is None or r[0] == "skip":
            return
        bid = ids.get(r[0])
        if bid is not None:
            grid[y][z * W + x] = [bid, r[1]]

    stone, road = "=minecraft:stone", ("=deci:BlockRoad" if "deci:BlockRoad" in ids else "=minecraft:stone")
    # ground everywhere up to level A, street rows, level B chunk raised
    for x in range(W):
        for z in range(L):
            top = G + (LEVEL if x < 16 else 0)
            for y in range(top):
                put(x, y, z, stone)
            if z < 16:
                put(x, top, z, road)
            else:
                put(x, top, z, "=minecraft:grass")
    # buildings: ground floor layer at the street level of their row
    for (cx, cz), name, lvl, cols, g in built:
        base = G + lvl * LEVEL - g
        for (gx, gz), layers in cols.items():
            for y, s in enumerate(layers):
                for z, row in enumerate(s[:16]):
                    for x, state in enumerate(row):
                        wx, wz = (cx + gx) * 16 + x, (cz + gz) * 16 + z
                        if state is None:
                            continue
                        put(wx, base + y, wz, state)
    # stairs part in street chunk (1, 0), level A, at G + 1, rising toward x 0
    part = pack.load("parts", "%s:%s" % (ns, STAIRS))
    common = pack.palette(ns + ":common")
    sl = lc.part_slices(pack, "%s:%s" % (ns, STAIRS), {}, {}, common)
    for y, s in enumerate(sl or []):
        for z, row in enumerate(s[:16]):
            for x, state in enumerate(row):
                if state is not None:
                    put(16 + x, G + 1 + y, z, state)
    blocks = bytearray(W * H * L)
    add = bytearray((W * H * L + 1) // 2)
    meta = bytearray(W * H * L)
    for y in range(H):
        for i, (bid, m) in enumerate(grid[y]):
            j = y * W * L + i
            blocks[j] = bid & 255
            hi = (bid >> 8) & 15
            add[j >> 1] = (add[j >> 1] & 0x0F) | (hi << 4) if j & 1 else (add[j >> 1] & 0xF0) | hi
            meta[j] = m & 15
    lc.write_schematic(os.path.join(out, "lc_scene.schematic"), W, H, L, blocks, add, meta)
    json.dump({"groundY": G}, open(os.path.join(out, "lc_scene.json"), "w"))
    print("scene %dx%dx%d, street level A at layer %d, level B at %d; stairs part %s %s" %
          (W, H, L, G, G + LEVEL, STAIRS, "found" if part else "MISSING"))


if __name__ == "__main__":
    main()
