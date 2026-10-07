#!/usr/bin/env python3
"""Check Decimation multiblock props in a generated world (0.16+).

    python3 tools/multiscan.py WORLD_DIR

Every tile entity carrying multiblock master data (multibl.mx/my/mz) is
classified:
  whole   a master whose part box above it (column multiblocks, 2 high) is
          filled with the same block, each part pointing at the master;
  single  a master with no part above (no room, or a wide multiblock);
  orphan  master never set (0,0,0): invisible in game (bug.md);
  broken  a part whose master position holds no master.
Counts per block name.
"""
import collections
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402


def tile_entities(world_dir):
    rdir = os.path.join(world_dir, "region")
    for f in os.listdir(rdir):
        if not f.endswith(".mca"):
            continue
        data = open(os.path.join(rdir, f), "rb").read()
        for i in range(1024):
            loc = struct.unpack(">I", data[4 * i:4 * i + 4])[0]
            if not loc:
                continue
            off = (loc >> 8) * 4096
            ln, _ = struct.unpack(">IB", data[off:off + 5])
            level = wc.read_nbt(zlib.decompress(data[off + 5:off + 4 + ln]))["Level"]
            for te in level.get("TileEntities", []):
                if "multibl.mx" in te:
                    yield te


def main():
    world_dir = sys.argv[1]
    w = wc.World(world_dir)
    names = {v: k for k, v in w.registry().items()}
    tes = {(t["x"], t["y"], t["z"]): t for t in tile_entities(world_dir)}
    stats = collections.defaultdict(collections.Counter)
    for (x, y, z), t in tes.items():
        name = names.get(w.block(x, y, z), "?")
        m = (t["multibl.mx"], t["multibl.my"], t["multibl.mz"])
        if m == (0, 0, 0):
            stats[name]["orphan"] += 1
        elif m == (x, y, z):
            above = tes.get((x, y + 1, z))
            whole = above is not None and w.block(x, y + 1, z) == w.block(x, y, z) \
                and (above["multibl.mx"], above["multibl.my"], above["multibl.mz"]) == m
            stats[name]["whole" if whole else "single"] += 1
        else:
            master = tes.get(m)
            ok = master is not None and (master["multibl.mx"], master["multibl.my"], master["multibl.mz"]) == m
            stats[name]["part" if ok else "broken"] += 1
    for name, c in sorted(stats.items()):
        print("%-34s %s" % (name, dict(c)))


if __name__ == "__main__":
    main()
