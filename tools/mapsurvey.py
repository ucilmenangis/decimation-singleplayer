#!/usr/bin/env python3
"""Survey a (Decimation) 1.7.10 world: what was built, where, with what.

    python tools/mapsurvey.py WORLD_DIR OUT_DIR [--scale N]

Needs numpy + pillow (a venv in the session scratchpad has them).
Writes to OUT_DIR:
  map.png       top-down map coloured by the top block (built blocks
                tinted, Decimation props marked in red), 1 px = N blocks
  blocks.tsv    every block id used, with its registry name and count
  props.tsv     Decimation (deci:) blocks only, by count
  hotspots.tsv  chunks ranked by built-up blocks (non natural) with their
                Decimation prop count and the most common props there
Used to study hand-built Decimation maps as reference for our generator
(docs/references/decimation_maps.md).
"""
import collections
import gzip
import os
import struct
import sys
import zlib

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402

NATURAL = {0, 1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 21, 24, 31, 32, 37, 38, 39, 40, 56, 73, 74,
           78, 79, 80, 81, 82, 83, 86, 99, 100, 106, 110, 111, 129, 161, 162, 174, 175, 49}


def registry(world):
    nbt = wc.read_nbt(gzip.decompress(open(os.path.join(world, "level.dat"), "rb").read()))
    items = nbt.get("FML", {}).get("ItemData", [])
    return {e["V"]: e["K"][1:] for e in items if e["K"].startswith("\x01")}


def chunks(world):
    rdir = os.path.join(world, "region")
    for f in sorted(os.listdir(rdir)):
        if not f.endswith(".mca"):
            continue
        data = open(os.path.join(rdir, f), "rb").read()
        if len(data) < 8192:
            continue
        for i in range(1024):
            loc = struct.unpack(">I", data[4 * i:4 * i + 4])[0]
            if not loc:
                continue
            off = (loc >> 8) * 4096
            try:
                ln, comp = struct.unpack(">IB", data[off:off + 5])
                raw = data[off + 5:off + 4 + ln]
                raw = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw)
                level = wc.read_nbt(raw)["Level"]
            except Exception:
                continue
            yield level


def chunk_ids(level):
    """ids[y, z, x] for the chunk (256 high)."""
    ids = np.zeros((256, 16, 16), dtype=np.int32)
    for s in level.get("Sections", []):
        y0 = s["Y"] * 16
        b = np.frombuffer(bytes(x & 255 for x in s["Blocks"]) if not isinstance(s["Blocks"], (bytes, bytearray))
                          else s["Blocks"], dtype=np.uint8).astype(np.int32)
        if "Add" in s:
            a = np.frombuffer(s["Add"] if isinstance(s["Add"], (bytes, bytearray)) else bytes(x & 255 for x in s["Add"]),
                              dtype=np.uint8).astype(np.int32)
            hi = np.empty(4096, dtype=np.int32)
            hi[0::2] = a & 15
            hi[1::2] = a >> 4
            b = b | (hi << 8)
        ids[y0:y0 + 16] = b.reshape(16, 16, 16)
    return ids


def colour(name):
    n = name or ""
    table = [("water", (50, 90, 200)), ("lava", (230, 90, 20)), ("grass", (95, 150, 60)), ("leaves", (50, 110, 40)),
             ("dirt", (120, 85, 55)), ("sand", (220, 205, 150)), ("snow", (240, 240, 245)), ("ice", (170, 200, 240)),
             ("gravel", (140, 130, 125)), ("log", (100, 75, 45)), ("stone", (125, 125, 125)), ("brick", (150, 80, 60)),
             ("planks", (170, 135, 85)), ("glass", (180, 220, 235)), ("quartz", (235, 230, 225)),
             ("wool", (210, 210, 210)), ("clay", (160, 110, 90)), ("iron", (200, 200, 205)), ("slab", (165, 165, 165)),
             ("Road", (45, 45, 48)), ("deci:", (210, 40, 40))]
    for k, c in table:
        if k in n:
            return c
    h = hash(n) & 0xFFFFFF
    return (90 + (h & 63), 90 + ((h >> 6) & 63), 90 + ((h >> 12) & 63))


def main():
    world, out = sys.argv[1], sys.argv[2]
    scale = int(sys.argv[sys.argv.index("--scale") + 1]) if "--scale" in sys.argv else 1
    os.makedirs(out, exist_ok=True)
    reg = registry(world)
    reg[0] = "minecraft:air"
    counts = np.zeros(4096, dtype=np.int64)
    tops = {}
    hot = []
    natural = np.zeros(4096, dtype=bool)
    natural[list(NATURAL)] = True
    deci = np.zeros(4096, dtype=bool)
    for i, n in reg.items():
        if n.startswith("deci:") and not n.startswith("deci:BlockRoad"):
            deci[i] = True
    n = 0
    for level in chunks(world):
        n += 1
        cx, cz = level["xPos"], level["zPos"]
        ids = chunk_ids(level)
        counts += np.bincount(ids.ravel(), minlength=4096)[:4096]
        nonair = ids != 0
        # top block per column
        ys = 255 - np.argmax(nonair[::-1], axis=0)
        has = nonair.any(axis=0)
        top = np.where(has, ids[ys, np.arange(16)[:, None], np.arange(16)[None, :]], 0)
        tops[(cx, cz)] = (top, ys)
        built = int((~natural[ids] & nonair).sum())
        dp = ids[deci[ids]]
        if built:
            pc = collections.Counter(dp.tolist()).most_common(4)
            hot.append((built, int(dp.size), cx, cz, ", ".join("%s x%d" % (reg.get(k, k)[5:], v) for k, v in pc)))
    xs = [c[0] for c in tops]
    zs = [c[1] for c in tops]
    x0, z0 = min(xs) * 16, min(zs) * 16
    w, h = (max(xs) + 1) * 16 - x0, (max(zs) + 1) * 16 - z0
    img = Image.new("RGB", (w // scale + 1, h // scale + 1), (0, 0, 0))
    px = img.load()
    cache = {}
    for (cx, cz), (top, ys) in tops.items():
        for z in range(0, 16, scale):
            for x in range(0, 16, scale):
                b = int(top[z, x])
                c = cache.get(b)
                if c is None:
                    c = cache[b] = colour(reg.get(b, str(b)))
                y = int(ys[z, x])
                f = 0.75 + 0.5 * (y - 40) / 120.0
                px[(cx * 16 + x - x0) // scale, (cz * 16 + z - z0) // scale] = tuple(
                    max(0, min(255, int(v * f))) for v in c)
    img.save(os.path.join(out, "map.png"))
    with open(os.path.join(out, "blocks.tsv"), "w") as f:
        for i in np.argsort(-counts):
            if counts[i] == 0:
                break
            f.write("%d\t%s\t%d\n" % (i, reg.get(int(i), "?"), counts[i]))
    with open(os.path.join(out, "props.tsv"), "w") as f:
        for i in np.argsort(-counts):
            if counts[i] and deci[i]:
                f.write("%s\t%d\n" % (reg[int(i)], counts[i]))
    hot.sort(reverse=True)
    with open(os.path.join(out, "hotspots.tsv"), "w") as f:
        f.write("built\tdeci\tchunkX\tchunkZ\tblockX\tblockZ\ttop props\n")
        for b, d, cx, cz, pc in hot[:300]:
            f.write("%d\t%d\t%d\t%d\t%d\t%d\t%s\n" % (b, d, cx, cz, cx * 16, cz * 16, pc))
    print("%d chunks, map %dx%d blocks from %d,%d (1px=%d), %d deci prop blocks total"
          % (n, w, h, x0, z0, scale, int(counts[deci].sum())))


if __name__ == "__main__":
    main()
