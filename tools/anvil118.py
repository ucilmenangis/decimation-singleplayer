#!/usr/bin/env python3
"""Read 1.18+ Anvil worlds (palette sections, 4x4x4 biomes): used to study
modern reference worlds such as the DeceasedCraft server world
(docs/references/deceasedcraft_buildings.md). Needs numpy.

    python3 tools/anvil118.py WORLD_DIR OUT_DIR [--scale N]

Writes OUT_DIR/biomes.tsv (surface biome per column, counted), map.png
(top block colour, 1 px = N blocks) and biome_map.png (surface biome,
legend in biome_legend.tsv).
"""
import collections
import gzip
import os
import struct
import sys
import zlib

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402


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
                raw = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
                yield wc.read_nbt(raw)
            except Exception:
                continue


def unpack(longs, bits, count):
    """Entries packed without spanning longs (1.16+)."""
    per = 64 // bits
    mask = (1 << bits) - 1
    out = np.zeros(count, dtype=np.int32)
    i = 0
    for v in longs:
        v &= (1 << 64) - 1
        for k in range(per):
            if i >= count:
                return out
            out[i] = (v >> (k * bits)) & mask
            i += 1
    return out


class Palette:
    """Global name -> id for a whole survey."""

    def __init__(self):
        self.ids = {}
        self.names = []

    def id(self, name):
        if name not in self.ids:
            self.ids[name] = len(self.names)
            self.names.append(name)
        return self.ids[name]


def chunk_blocks(chunk, pal, props=False):
    """(min_y, ids[y, z, x]) with global palette ids (block name, with
    properties when props=True); None for chunks without sections."""
    secs = chunk.get("sections") or chunk.get("Level", {}).get("Sections")
    if not secs:
        return None
    ys = [s["Y"] for s in secs if "block_states" in s]
    if not ys:
        return None
    y0, y1 = min(ys), max(ys)
    ids = np.zeros(((y1 - y0 + 1) * 16, 16, 16), dtype=np.int32)
    air = pal.id("minecraft:air")
    ids[:] = air
    for s in secs:
        bs = s.get("block_states")
        if not bs:
            continue
        p = bs.get("palette", [])
        gl = []
        for e in p:
            n = e.get("Name", "minecraft:air")
            if props and e.get("Properties"):
                n += "[" + ",".join("%s=%s" % kv for kv in sorted(e["Properties"].items())) + "]"
            gl.append(pal.id(n))
        gl = np.array(gl, dtype=np.int32)
        if "data" not in bs or len(p) == 1:
            local = np.zeros(4096, dtype=np.int32)
        else:
            bits = max(4, (len(p) - 1).bit_length())
            local = unpack(bs["data"], bits, 4096)
        ids[(s["Y"] - y0) * 16:(s["Y"] - y0) * 16 + 16] = gl[local].reshape(16, 16, 16)
    return y0 * 16, ids


def chunk_biomes(chunk, pal):
    """{section Y: biomes[4,4,4] (y, z, x) global ids}."""
    out = {}
    for s in chunk.get("sections", []):
        b = s.get("biomes")
        if not b:
            continue
        p = [pal.id(n) for n in b.get("palette", [])]
        if len(p) == 1 or "data" not in b:
            local = np.zeros(64, dtype=np.int32)
        else:
            local = unpack(b["data"], (len(p) - 1).bit_length(), 64)
        out[s["Y"]] = np.array(p, dtype=np.int32)[local].reshape(4, 4, 4)
    return out


def main():
    world, out = sys.argv[1], sys.argv[2]
    scale = int(sys.argv[sys.argv.index("--scale") + 1]) if "--scale" in sys.argv else 4
    os.makedirs(out, exist_ok=True)
    from PIL import Image
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import mapsurvey as ms
    bpal, gpal = Palette(), Palette()
    tops, biomes = {}, {}
    count = collections.Counter()
    for c in chunks(world):
        cx, cz = c.get("xPos"), c.get("zPos")
        if cx is None:
            continue
        r = chunk_blocks(c, bpal)
        if r is None:
            continue
        y0, ids = r
        air = bpal.id("minecraft:air")
        nonair = ids != air
        top = ids.shape[0] - 1 - np.argmax(nonair[::-1], axis=0)
        topid = ids[top, np.arange(16)[:, None], np.arange(16)[None, :]]
        tops[(cx, cz)] = topid
        bio = chunk_biomes(c, gpal)
        # surface biome: biome cell at the top block height
        sb = np.zeros((16, 16), dtype=np.int32)
        for z in range(16):
            for x in range(16):
                wy = y0 + top[z, x]
                sec = bio.get(wy >> 4)
                sb[z, x] = sec[(wy & 15) >> 2, z >> 2, x >> 2] if sec is not None else 0
                count[gpal.names[sb[z, x]] if gpal.names else "?"] += 1
        biomes[(cx, cz)] = sb
    xs = [k[0] for k in tops]
    zs = [k[1] for k in tops]
    x0, z0 = min(xs) * 16, min(zs) * 16
    w, h = (max(xs) + 1) * 16 - x0, (max(zs) + 1) * 16 - z0
    img = Image.new("RGB", (w // scale + 1, h // scale + 1))
    bimg = Image.new("RGB", (w // scale + 1, h // scale + 1))
    px, bpx = img.load(), bimg.load()
    bcol = {}
    for (cx, cz), top in tops.items():
        sb = biomes[(cx, cz)]
        for z in range(0, 16, scale):
            for x in range(0, 16, scale):
                n = bpal.names[top[z, x]]
                p = ((cx * 16 + x - x0) // scale, (cz * 16 + z - z0) // scale)
                px[p] = ms.colour(n.replace("deci:", ""))
                b = gpal.names[sb[z, x]] if gpal.names else "?"
                if b not in bcol:
                    hsh = hash(b) & 0xFFFFFF
                    bcol[b] = (60 + (hsh & 191), 60 + ((hsh >> 8) & 191), 60 + ((hsh >> 16) & 191))
                bpx[p] = bcol[b]
    img.save(os.path.join(out, "map.png"))
    bimg.save(os.path.join(out, "biome_map.png"))
    with open(os.path.join(out, "biomes.tsv"), "w") as f:
        for k, v in count.most_common():
            f.write("%s\t%d\t%s\n" % (k, v, "#%02x%02x%02x" % bcol.get(k, (0, 0, 0))))
    print("%d chunks, area %dx%d from %d,%d" % (len(tops), w, h, x0, z0))


if __name__ == "__main__":
    main()
