#!/usr/bin/env python3
"""Convert Lost Cities buildings (1.20 data packs, e.g. DeceasedCraft's) to
1.7.10 .schematic files with Decimation props, for personal use.

    python3 tools/lc2schem.py DATA_DIR NAMESPACE REGISTRY_WORLD OUT_DIR BUILDING...

DATA_DIR: the extracted `data/` folder (docs/references/deceasedcraft_buildings.md).
REGISTRY_WORLD: a 1.7.10 world of this instance (block name -> numeric id
from its level.dat; schematics store raw ids). BUILDING: a building group
name (e.g. building_apartmentsmalla) or multi building (multi_cafe).
Writes OUT_DIR/<building>.schematic and prints untranslated blocks.
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


def building_columns(pack, bref):
    """Stacked slices of a single building: list of 16-row slices, bottom up."""
    b = pack.load("buildings", bref)
    if b is None:
        raise SystemExit("no building " + bref)
    common = pack.palette(pack.ns + ":common")
    extra = {}
    for p in b.get("parts2", []):
        extra.setdefault(p.get("inpart"), []).append(p["part"])
    layers = []
    for entry in b.get("parts", []):
        if entry.get("cellar"):
            continue
        refs = [entry["part"]] + extra.get(entry["part"], [])
        storey = None
        for ref in refs:
            part = pack.load("parts", ref)
            if part is None:
                continue
            pal = dict(common)
            for r in (b.get("refpalette"), part.get("refpalette")):
                if r:
                    pal.update(pack.palette(r))
            if isinstance(part.get("palette"), list):
                for e in part["palette"]:
                    pal[e["char"]] = e.get("block")
            slices = [[[pal.get(ch) if ch != " " else None for ch in row.ljust(16)[:16]] for row in s]
                      for s in part["slices"]]
            if storey is None:
                storey = slices
            else:  # overlay: non-space chars replace
                for y, s in enumerate(slices[:len(storey)]):
                    for z, row in enumerate(s):
                        for x, blk in enumerate(row):
                            if blk is not None:
                                storey[y][z][x] = blk
        if storey:
            layers.extend(storey)
    return layers


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
        grid = multi["buildings"] if multi else [["%s:%s/%s" % (ns, name, name)]]
        cols = {}
        for gx, row in enumerate(grid):
            for gz, bref in enumerate(row):
                cols[(gx, gz)] = building_columns(pack, bref)
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
        print("%s: %dx%dx%d" % (name, W, H, L))
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
