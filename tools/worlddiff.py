#!/usr/bin/env python3
"""Block by block diff of two generated 1.7.10 worlds (refactor check).

    python3 tools/worlddiff.py WORLD_A WORLD_B

Compares block ids and metadata of every chunk present in both worlds
(chunk contents do not depend on generation order, so a refactor that
keeps behaviour must give 0 differences). Only chunks final in both
worlds count (it and its 3 lower neighbours populated); fluids and
sand / gravel are ignored (they flow / fall while the server ticks). Prints counts and the first
differing positions. Needs numpy.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mapsurvey as ms  # noqa: E402


FLUID = (8, 9, 10, 11, 12, 13)  # + sand / gravel: fall when neighbours update


def arrays(level):
    ids = ms.chunk_ids(level)
    meta = np.zeros((256, 16, 16), dtype=np.int32)
    for s in level.get("Sections", []):
        d = s["Data"]
        d = np.frombuffer(d if isinstance(d, (bytes, bytearray)) else bytes(x & 255 for x in d), dtype=np.uint8)
        m = np.empty(4096, dtype=np.int32)
        m[0::2] = d & 15
        m[1::2] = d >> 4
        meta[s["Y"] * 16:s["Y"] * 16 + 16] = m.reshape(16, 16, 16)
    return ids, meta


def main():
    a = {(l["xPos"], l["zPos"]): l for l in ms.chunks(sys.argv[1])}
    b = {(l["xPos"], l["zPos"]): l for l in ms.chunks(sys.argv[2])}
    # a chunk is final only when it and the 3 chunks whose population
    # window (16x16 at +8) covers it are populated in both worlds
    def done(w, k):
        return all(w.get((k[0] - dx, k[1] - dz), {}).get("TerrainPopulated") for dx in (0, 1) for dz in (0, 1))

    common = sorted(k for k in set(a) & set(b) if done(a, k) and done(b, k))
    diff_chunks, diff_blocks, shown = 0, 0, 0
    for key in common:
        ia, ma = arrays(a[key])
        ib, mb = arrays(b[key])
        d = (ia != ib) | (ma != mb)
        # flowing water / lava and falling sand / gravel depend on how long
        # the server ticked: ignore cells where either side holds one
        d &= ~(np.isin(ia, FLUID) | np.isin(ib, FLUID))
        n = int(d.sum())
        if n:
            diff_chunks += 1
            diff_blocks += n
            for y, z, x in zip(*np.nonzero(d)):
                if shown < 15:
                    print("  %d %d %d: %d:%d -> %d:%d" % (key[0] * 16 + x, y, key[1] * 16 + z,
                                                         ia[y, z, x], ma[y, z, x], ib[y, z, x], mb[y, z, x]))
                    shown += 1
    print("%d chunks in A, %d in B, %d final in both; %d chunks differ, %d blocks differ"
          % (len(a), len(b), len(common), diff_chunks, diff_blocks))


if __name__ == "__main__":
    main()
