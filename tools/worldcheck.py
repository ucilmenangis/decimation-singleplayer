#!/usr/bin/env python3
"""Read blocks straight out of a 1.7.10 Anvil world, no game needed.

    python3 tools/worldcheck.py WORLD_DIR column X Z [YMIN YMAX]
        print the block ids of one column (top down)
    python3 tools/worldcheck.py WORLD_DIR box X1 Y1 Z1 X2 Y2 Z2
        count block ids inside a box

Lets the structure generator be verified from a dedicated server run:
generate, stop the server, then inspect what actually landed in the world.
"""
import collections
import gzip
import os
import struct
import sys
import zlib


def read_nbt(buf):
    pos = 0

    def u(fmt, n):
        nonlocal pos
        v = struct.unpack(">" + fmt, buf[pos:pos + n])[0]
        pos += n
        return v

    def name():
        nonlocal pos
        ln = u("H", 2)
        s = buf[pos:pos + ln].decode("utf-8", "replace")
        pos += ln
        return s

    def payload(t):
        nonlocal pos
        if t == 1: return u("b", 1)
        if t == 2: return u("h", 2)
        if t == 3: return u("i", 4)
        if t == 4: return u("q", 8)
        if t == 5: return u("f", 4)
        if t == 6: return u("d", 8)
        if t == 7:
            n = u("i", 4); v = buf[pos:pos + n]; pos += n; return v
        if t == 8: return name()
        if t == 9:
            et = u("b", 1); n = u("i", 4); return [payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                et = u("b", 1)
                if et == 0:
                    return out
                k = name()
                out[k] = payload(et)
        if t == 11:
            n = u("i", 4); v = struct.unpack(">%di" % n, buf[pos:pos + 4 * n]); pos += 4 * n; return v
        raise ValueError("tag %d" % t)

    t = u("b", 1)
    name()
    return payload(t)


class World:
    def __init__(self, path):
        self.path = path
        self.cache = {}

    def chunk(self, cx, cz):
        key = (cx, cz)
        if key in self.cache:
            return self.cache[key]
        rx, rz = cx >> 5, cz >> 5
        f = os.path.join(self.path, "region", "r.%d.%d.mca" % (rx, rz))
        sections = None
        if os.path.isfile(f):
            with open(f, "rb") as fh:
                idx = 4 * ((cx & 31) + (cz & 31) * 32)
                fh.seek(idx)
                loc = struct.unpack(">I", fh.read(4))[0]
                if loc:
                    fh.seek((loc >> 8) * 4096)
                    ln, comp = struct.unpack(">IB", fh.read(5))
                    data = fh.read(ln - 1)
                    raw = zlib.decompress(data) if comp == 2 else gzip.decompress(data)
                    level = read_nbt(raw)["Level"]
                    sections = {}
                    for s in level.get("Sections", []):
                        sections[s["Y"]] = s
        self.cache[key] = sections
        return sections

    def block(self, x, y, z):
        secs = self.chunk(x >> 4, z >> 4)
        if secs is None:
            return None  # chunk not generated
        s = secs.get(y >> 4)
        if s is None:
            return 0
        i = ((y & 15) * 16 + (z & 15)) * 16 + (x & 15)
        bid = s["Blocks"][i]
        if "Add" in s:
            add = s["Add"][i >> 1]
            bid |= ((add >> 4) if i & 1 else (add & 15)) << 8
        return bid


def _meta(self, x, y, z):
    """Block metadata (0..15) at a position, None if the chunk is missing."""
    secs = self.chunk(x >> 4, z >> 4)
    if secs is None:
        return None
    s = secs.get(y >> 4)
    if s is None:
        return 0
    i = ((y & 15) * 16 + (z & 15)) * 16 + (x & 15)
    d = s["Data"][i >> 1]
    return (d >> 4) & 15 if i & 1 else d & 15


World.meta = _meta


def main():
    w = World(sys.argv[1])
    mode = sys.argv[2]
    a = [int(v) for v in sys.argv[3:]]
    if mode == "column":
        x, z = a[0], a[1]
        lo, hi = (a[2], a[3]) if len(a) >= 4 else (0, 120)
        for y in range(hi, lo - 1, -1):
            print(y, w.block(x, y, z))
    elif mode == "box":
        x1, y1, z1, x2, y2, z2 = a
        c = collections.Counter()
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    c[w.block(x, y, z)] += 1
        for k, v in c.most_common():
            print(k, v)


if __name__ == "__main__":
    main()
