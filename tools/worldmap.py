#!/usr/bin/env python3
"""Top down map of a generated world, from the region files.

    python3 tools/worldmap.py WORLD_DIR OUT.png [scale]

Colour = biome (Decimation world type biomes 110..118 have their own
colours, others grey), shaded by terrain height (hillshade from the
HeightMap), water drawn blue. Prints a biome histogram and the height
range per biome. Lets terrain work be judged without playing.
"""
import collections
import os
import struct
import sys
import zlib

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402

BIOMES = {
    110: ("Decimated City", (150, 146, 128)),
    111: ("Decimated Suburbs", (170, 165, 110)),
    112: ("Irradiated Military Zone", (140, 120, 90)),
    113: ("Decimated Plains", (190, 175, 105)),
    114: ("Burnt Forest", (105, 92, 70)),
    115: ("Overgrown Plains", (120, 185, 80)),
    116: ("Overgrown Forest", (60, 130, 50)),
    117: ("Overgrown Hills", (95, 150, 75)),
    118: ("Murky River", (70, 110, 150)),
}


def chunks(world_dir):
    rdir = os.path.join(world_dir, "region")
    for f in os.listdir(rdir):
        if not f.endswith(".mca"):
            continue
        _, rx, rz, _ = f.split(".")
        rx, rz = int(rx), int(rz)
        data = open(os.path.join(rdir, f), "rb").read()
        for i in range(1024):
            loc = struct.unpack(">I", data[4 * i:4 * i + 4])[0]
            if not loc:
                continue
            off = (loc >> 8) * 4096
            ln, comp = struct.unpack(">IB", data[off:off + 5])
            raw = zlib.decompress(data[off + 5:off + 4 + ln])
            level = wc.read_nbt(raw)["Level"]
            yield rx * 32 + (i & 31), rz * 32 + (i >> 5), level


def main():
    world_dir, out = sys.argv[1], sys.argv[2]
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    cols = {}
    for cx, cz, level in chunks(world_dir):
        if not level.get("TerrainPopulated"):
            continue
        biomes = level.get("Biomes")
        hm = level.get("HeightMap")
        secs = {s["Y"]: s for s in level.get("Sections", [])}
        for z in range(16):
            for x in range(16):
                i = z * 16 + x
                b = biomes[i] & 255 if biomes else 255
                h = hm[i] if hm else 64
                y = h - 1
                s = secs.get(y >> 4)
                bid = s["Blocks"][((y & 15) * 16 + z) * 16 + x] if s else 0
                cols[(cx * 16 + x, cz * 16 + z)] = (b, h, bid in (8, 9))
    xs = [k[0] for k in cols]
    zs = [k[1] for k in cols]
    x0, z0 = min(xs), min(zs)
    w, hgt = max(xs) - x0 + 1, max(zs) - z0 + 1
    img = Image.new("RGB", (w, hgt), (0, 0, 0))
    px = img.load()
    hist = collections.Counter()
    hrange = {}
    for (x, z), (b, h, water) in cols.items():
        hist[b] += 1
        lo, hi = hrange.get(b, (999, -1))
        hrange[b] = (min(lo, h), max(hi, h))
        base = BIOMES.get(b, ("?", (128, 128, 128)))[1]
        west = cols.get((x - 1, z), (b, h, water))[1]
        north = cols.get((x, z - 1), (b, h, water))[1]
        shade = 1.0 + 0.06 * ((h - west) + (h - north)) + 0.004 * (h - 64)
        c = tuple(max(0, min(255, int(v * shade))) for v in base)
        if water:
            c = (40, 70, 140) if b != 118 else (55, 90, 150)
        px[x - x0, z - z0] = c
    if scale > 1:
        img = img.resize((w * scale, hgt * scale), Image.NEAREST)
    img.save(out)
    print("map %dx%d blocks from x=%d z=%d -> %s" % (w, hgt, x0, z0, out))
    total = sum(hist.values())
    for b, n in hist.most_common():
        name = BIOMES.get(b, ("vanilla/other %d" % b, None))[0]
        print("  %-26s %5.1f%%  height %d..%d" % (name, 100.0 * n / total, *hrange[b]))


if __name__ == "__main__":
    main()
