#!/usr/bin/env python3
"""Convert Lost Cities buildings (1.20 data packs, e.g. DeceasedCraft's) to
1.7.10 .schematic files with Decimation props, for personal use.

    python3 tools/lc2schem.py DATA_DIR NAMESPACE REGISTRY_WORLD OUT_DIR BUILDING...

DATA_DIR: the extracted `data/` folder (docs/references/deceasedcraft_buildings.md).
REGISTRY_WORLD: a 1.7.10 world of this instance (block name -> numeric id
from its level.dat; schematics store raw ids). BUILDING: a building group
name (e.g. building_apartmentsmalla) or multi building (multi_cafe).
Writes OUT_DIR/<building>.schematic (+ <building>.json with "groundY",
the layer of the ground floor: cellars lie below it) and prints
untranslated blocks. Floors are picked like Lost Cities does (part
conditions ground / top / floor / range / cellar), maxfloors above ground,
maxcellars below.
Never commit the output: it is DeceasedCraft's content.

Translation is rule based (tools/lctranslate.py): shape words (stairs,
slab, wall, pane, door...), material words (quartz, deepslate, oak...),
colours, and furniture categories mapped to one cell Decimation props
(docs/prop_footprints.tsv) or vanilla stand-ins.
"""
import gzip
import json
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mapsurvey as ms  # noqa: E402
import lctranslate as lt  # noqa: E402


PROFILE_FLOORS = (1, 5)  # buildingMinFloors / buildingMaxFloors of the DeceasedCraft profiles


class Pack:
    def __init__(self, data, ns):
        self.data, self.ns = data, ns
        self.cache = {}

    def load(self, kind, ref):
        ns, _, path = ref.partition(":")
        if not path:
            ns, path = self.ns, ref
        f = os.path.join(self.data, ns, "lostcities", kind, path + ".json")
        return json.load(open(f)) if os.path.exists(f) else None

    def palette(self, ref):
        if ref not in self.cache:
            p = self.load("palettes", ref) or {}
            m = {}
            for e in p.get("palette", []):
                b = e.get("block")
                if b is None and e.get("blocks"):
                    b = max(e["blocks"], key=lambda x: x.get("random", 1)).get("block")
                if b is None and "variant" in e:
                    b = None
                m[e["char"]] = b
            self.cache[ref] = m
        return self.cache[ref]


def part_slices(pack, ref, b, extra, common):
    part = pack.load("parts", ref)
    if part is None:
        return None
    pal = dict(common)
    for r in (b.get("refpalette"), part.get("refpalette")):
        if r:
            pal.update(pack.palette(r))
    if isinstance(part.get("palette"), list):
        for e in part["palette"]:
            pal[e["char"]] = e.get("block")
    slices = [[[pal.get(ch) if ch != " " else None for ch in row.ljust(16)[:16]] for row in s]
              for s in part["slices"]]
    for ref2 in extra.get(ref, []):  # parts2 overlays: non-space chars replace
        over = part_slices(pack, ref2, b, {}, common)
        for y, s in enumerate((over or [])[:len(slices)]):
            for z, row in enumerate(s):
                for x, blk in enumerate(row):
                    if blk is not None:
                        slices[y][z][x] = blk
    return slices


def matches(entry, floor, floors, cellars):
    """Lost Cities part conditions for one floor (cellars are negative)."""
    if bool(entry.get("cellar", False)) != (floor < 0):
        return False
    if "ground" in entry and entry["ground"] != (floor == 0):
        return False
    if "top" in entry and entry["top"] != (floor >= floors):  # LC: top = floor >= floors
        return False
    if "floor" in entry and entry["floor"] != floor:
        return False
    if "range" in entry:
        lo, hi = (int(v) for v in str(entry["range"]).split(","))
        if not lo <= floor <= hi:
            return False
    return True


def building_columns(pack, bref, floors_wanted=None):
    """(layers bottom up, ground layer index): every floor picked like Lost
    Cities picks it (first part whose conditions match; cellars included,
    deepest first). floors_wanted: storeys above ground (default: the
    building's maxfloors, or the number of non cellar parts)."""
    b = pack.load("buildings", bref)
    if b is None:
        raise SystemExit("no building " + bref)
    common = pack.palette(pack.ns + ":common")
    extra = {}
    for p in b.get("parts2", []):
        extra.setdefault(p.get("inpart"), []).append(p["part"])
    parts = b.get("parts", [])
    # Lost Cities (BuildingInfo.getMin/Maxfloors, generateBuilding): floors
    # 0..F are generated, floor F being the "top" (roof) part; F is clamped
    # to the building's min / max (exact when overrideFloors) and the
    # profile's (DeceasedCraft profiles: 1..5, min + 1 "because this doesn't
    # count the top"). We take the largest allowed F.
    pmin, pmax = PROFILE_FLOORS
    if b.get("overrideFloors") and b.get("maxfloors") is not None:
        floors = b["maxfloors"]
    else:
        floors = pmax if b.get("maxfloors") is None else min(pmax, b["maxfloors"])
        floors = max(floors, pmin + 1, b.get("minfloors") or 0)
    if floors_wanted:
        floors = floors_wanted
    cel_parts = [p for p in parts if p.get("cellar")]
    explicit = [p["floor"] for p in cel_parts if "floor" in p]
    if b.get("maxcellars") is not None:
        cellars = b["maxcellars"]
    else:
        cellars = len(cel_parts)
    if explicit:
        cellars = max(cellars, -min(explicit))
    if not cel_parts:
        cellars = 0
    layers, ground = [], 0
    for floor in range(-cellars, floors + 1):
        pick = next((p for p in parts if matches(p, floor, floors, cellars)), None)
        if pick is None:
            continue
        sl = part_slices(pack, pick["part"], b, extra, common)
        if sl:
            if floor == 0:
                ground = len(layers)
            layers.extend(sl)
    return layers, ground


def main():
    data, ns, regworld, out = sys.argv[1:5]
    names = sys.argv[5:]
    os.makedirs(out, exist_ok=True)
    ids = {n: i for i, n in ms.registry(regworld).items()}
    ids["minecraft:air"] = 0
    pack = Pack(data, ns)
    missing = {}
    for name in names:
        multi = pack.load("multibuildings", name)
        if multi:
            grid = multi["buildings"]
        elif pack.load("buildings", "%s:%s/%s" % (ns, name, name)):
            grid = [["%s:%s/%s" % (ns, name, name)]]
        else:
            grid = [["%s:%s" % (ns, name)]]  # flat layout (legacy c70cities)
        cols = {}
        grounds = {}
        for gx, row in enumerate(grid):
            for gz, bref in enumerate(row):
                cols[(gx, gz)], grounds[(gx, gz)] = building_columns(pack, bref)
        # align every chunk of a multi building on the ground floor: the
        # schematic's y 0 is the bottom of the deepest cellar
        depth = max(grounds.values())
        cols = {k: [[[None] * 16 for _ in range(16)]] * (depth - grounds[k]) + v for k, v in cols.items()}
        W = len(grid) * 16
        L = max(len(r) for r in grid) * 16
        H = max(len(c) for c in cols.values())
        blocks = bytearray(W * H * L)
        add = bytearray((W * H * L + 1) // 2)
        meta = bytearray(W * H * L)
        for (gx, gz), layers in cols.items():
            for y, s in enumerate(layers):
                for z, row in enumerate(s[:16]):
                    for x, state in enumerate(row):
                        if state is None:
                            continue
                        res = lt.translate(state)
                        if res is None:
                            missing[state.split("[")[0]] = missing.get(state.split("[")[0], 0) + 1
                            continue
                        bname, m = res
                        if bname == "skip":
                            continue
                        bid = ids.get(bname)
                        if bid is None:
                            missing["(no id) " + bname] = missing.get("(no id) " + bname, 0) + 1
                            continue
                        wx, wz = gx * 16 + x, gz * 16 + z
                        i = (y * L + wz) * W + wx
                        blocks[i] = bid & 255
                        hi = (bid >> 8) & 15
                        if i & 1:
                            add[i >> 1] = (add[i >> 1] & 0x0F) | (hi << 4)
                        else:
                            add[i >> 1] = (add[i >> 1] & 0xF0) | hi
                        meta[i] = m & 15
        write_schematic(os.path.join(out, name + ".schematic"), W, H, L, blocks, add, meta)
        # sidecar: the ground floor's layer, so /deciworldgen paste puts the
        # ground floor (street level) at the given y and cellars below it
        with open(os.path.join(out, name + ".json"), "w") as f:
            json.dump({"groundY": depth}, f)
        print("%s: %dx%dx%d, ground floor at layer %d" % (name, W, H, L, depth))
    for k, v in sorted(missing.items(), key=lambda kv: -kv[1])[:60]:
        print("  untranslated %6d %s" % (v, k))


def tag(t, name, payload):
    n = name.encode()
    return bytes([t]) + struct.pack(">H", len(n)) + n + payload


def write_schematic(path, W, H, L, blocks, add, meta):
    body = b"".join([
        tag(2, "Width", struct.pack(">h", W)), tag(2, "Height", struct.pack(">h", H)),
        tag(2, "Length", struct.pack(">h", L)), tag(8, "Materials", struct.pack(">H", 5) + b"Alpha"),
        tag(7, "Blocks", struct.pack(">i", len(blocks)) + bytes(blocks)),
        tag(7, "AddBlocks", struct.pack(">i", len(add)) + bytes(add)),
        tag(7, "Data", struct.pack(">i", len(meta)) + bytes(meta)),
        tag(9, "Entities", bytes([10]) + struct.pack(">i", 0)),
        tag(9, "TileEntities", bytes([10]) + struct.pack(">i", 0)),
    ])
    nbt = tag(10, "Schematic", body + b"\x00")
    with gzip.open(path, "wb") as f:
        f.write(nbt)


if __name__ == "__main__":
    main()
