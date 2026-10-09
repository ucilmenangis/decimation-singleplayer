#!/usr/bin/env python3
"""Offline performance check of a world save and a game log (user report 10 Oktober 2026: 1 fps;
skill decimation-gun "Performance"; docs/performance.md).

    python3 tools/perfcheck.py world WORLD_DIR [--top N]   # entities by type, crowded chunks, broken chunks
    python3 tools/perfcheck.py log LOG_FILE [--top N]      # most repeated lines (exception spam)
    python3 tools/perfcheck.py fixchunk WORLD_DIR CX CZ    # drop one chunk from its region file (backup first!)

world: reads every region file (no game needed): entity count per id, the chunks with the most
entities, chunks stored in the wrong region slot (xPos / zPos not matching the slot: Forge logs
"Wrong location!" with a full stack trace for their entities, every tick), entities whose
position is outside the chunk that stores them, tile entities per id.
log: counts lines without their timestamps; a stack trace printed thousands of times is a
frame killer by itself.
fixchunk: clears the region slot of one chunk (the game regenerates it): the cure for a chunk
in the wrong slot. Copy the world first.
"""
import collections
import gzip
import os
import re
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from worldcheck import read_nbt  # noqa: E402


def chunks(world):
    for f in sorted(os.listdir(os.path.join(world, "region"))):
        if not f.endswith(".mca"):
            continue
        rx, rz = map(int, f.split(".")[1:3])
        data = open(os.path.join(world, "region", f), "rb").read()
        for i in range(1024):
            loc = struct.unpack(">I", data[i * 4:i * 4 + 4])[0]
            if not loc:
                continue
            off = (loc >> 8) * 4096
            ln, comp = struct.unpack(">IB", data[off:off + 5])
            raw = data[off + 5:off + 4 + ln]
            cx, cz = rx * 32 + (i & 31), rz * 32 + (i >> 5)
            try:
                yield cx, cz, read_nbt(zlib.decompress(raw) if comp == 2 else gzip.decompress(raw))["Level"]
            except Exception as e:  # unreadable chunk
                yield cx, cz, {"error": str(e)}


def world_report(world, top):
    kinds, per_chunk, tiles = collections.Counter(), collections.Counter(), collections.Counter()
    misplaced, outside, unreadable = [], collections.Counter(), []
    for cx, cz, lv in chunks(world):
        if "error" in lv:
            unreadable.append((cx, cz, lv["error"][:60]))
            continue
        if (lv.get("xPos"), lv.get("zPos")) != (cx, cz):
            misplaced.append(((cx, cz), (lv.get("xPos"), lv.get("zPos"))))
        for e in lv.get("Entities", []):
            kinds[e.get("id", "?")] += 1
            per_chunk[(cx, cz)] += 1
            p = e.get("Pos")
            if p and (int(p[0] // 16), int(p[2] // 16)) != (cx, cz):
                outside[(cx, cz)] += 1
        for t in lv.get("TileEntities", []):
            tiles[t.get("id", "?")] += 1
    total = sum(kinds.values())
    print("entities: %d" % total)
    for k, v in kinds.most_common(top):
        print("  %6d %s" % (v, k))
    print("most crowded chunks (a busy area: 20+ is a lot, hundreds is a problem):")
    for k, v in per_chunk.most_common(top):
        print("  %6d chunk %s (blocks x %d..%d, z %d..%d)" % (v, k, k[0] * 16, k[0] * 16 + 15, k[1] * 16, k[1] * 16 + 15))
    print("chunks in the wrong region slot: %d %s" % (len(misplaced), misplaced[:top]))
    print("entities stored outside their chunk: %d %s" % (sum(outside.values()), outside.most_common(top)))
    print("unreadable chunks: %d %s" % (len(unreadable), unreadable[:top]))
    print("tile entities: %d %s" % (sum(tiles.values()), tiles.most_common(top)))
    return total, misplaced


def log_report(path, top):
    c = collections.Counter()
    n = 0
    for line in open(path, errors="ignore"):
        n += 1
        m = re.search(r"\]: (?:\[[^\]]*\]: )?(.*)$", line)
        if m:
            c[re.sub(r"[-\d.]+", "#", m.group(1).strip())[:160]] += 1
    print("lines: %d" % n)
    for k, v in c.most_common(top):
        print("  %7d %s" % (v, k))


def fix_chunk(world, cx, cz):
    f = os.path.join(world, "region", "r.%d.%d.mca" % (cx >> 5, cz >> 5))
    data = bytearray(open(f, "rb").read())
    i = (cx & 31) + (cz & 31) * 32
    data[i * 4:i * 4 + 4] = b"\0\0\0\0"
    data[4096 + i * 4:4096 + i * 4 + 4] = b"\0\0\0\0"
    open(f, "wb").write(data)
    print("cleared chunk %d,%d in %s (regenerated on next load)" % (cx, cz, f))


def main():
    a = sys.argv[1:]
    if len(a) < 2:
        print(__doc__)
        return
    top = int(a[a.index("--top") + 1]) if "--top" in a else 8
    if a[0] == "world":
        world_report(a[1], top)
    elif a[0] == "log":
        log_report(a[1], top)
    elif a[0] == "fixchunk":
        fix_chunk(a[1], int(a[2]), int(a[3]))


if __name__ == "__main__":
    main()
