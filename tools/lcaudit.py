#!/usr/bin/env python3
"""Audit the 1.20 -> 1.7.10 block translation over every converted building.

    python3 tools/lcaudit.py OUT_TSV pack=DATA:NAMESPACE [pack=DATA:NAMESPACE ...]

Walks the same buildings / multi buildings tools/lcpack.py converts (every
building the packs' city styles list) and, for every source block state:
  - counts how often each source block name is DROPPED (translate() gives
    None) or SKIPPED on purpose ("skip"), with its example state;
  - for every source block that becomes a prop (deci:Block* other than
    stone / metal / road surfaces), counts where it sits: FLOOR (solid
    below, air above: used as a floor tile), CEILING (air below, solid
    above), WALL (solid on a side, air below and above) or FREE.
Prints the top of both lists and writes everything to OUT_TSV
(kind, source, count, target, floor, ceiling, wall, free, example).
Used 2026-10-09 to find the "LED lamp as floor" bug (bug.md).
"""
import collections
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lc2schem as lc  # noqa: E402
import lctranslate as lt  # noqa: E402

SKIP_STYLES = ("dummycity",)
SURFACES = ("deci:BlockStone", "deci:BlockMetal", "deci:BlockRoad", "deci:BlockBrick", "deci:BlockConcrete")


def buildings(data, ns):
    pack = lc.Pack(data, ns)
    root = os.path.join(data, ns, "lostcities", "citystyles")
    refs = set()
    for f in sorted(os.listdir(root)):
        if f[:-5] in SKIP_STYLES:
            continue
        sel = json.load(open(os.path.join(root, f))).get("selectors", {})
        for k in ("buildings", "multibuildings"):
            for e in sel.get(k, []):
                refs.add(e["value"])
    for ref in sorted(refs):
        name = ref.split(":")[-1]
        multi = pack.load("multibuildings", ref) if "/" not in name else None
        grid = multi["buildings"] if multi else [[ref if ":" in ref else ns + ":" + ref]]
        for row in grid:
            for bref in row:
                try:
                    cols, _ = lc.building_columns(pack, bref)
                except SystemExit:
                    continue
                yield bref, cols


def solid(state):
    if not state:
        return False
    r = lt.translate(state)
    return bool(r) and r[0] not in ("skip", "minecraft:air") and not r[0].startswith("deci:Block") \
        or bool(r) and r[0].startswith(SURFACES)


def main():
    out = sys.argv[1]
    dropped = collections.Counter()
    examples = {}
    props = collections.defaultdict(lambda: [0, 0, 0, 0])
    targets = {}
    seen = set()
    for spec in sys.argv[2:]:
        data, ns = spec.split("=", 1)[1].split(":")
        for bref, layers in buildings(data, ns):
            if bref in seen:
                continue
            seen.add(bref)
            H = len(layers)
            for y in range(H):
                for z in range(16):
                    for x in range(16):
                        st = layers[y][z][x] if z < len(layers[y]) and x < len(layers[y][z]) else None
                        if not st:
                            continue
                        name = st.split("[")[0]
                        r = lt.translate(st)
                        if r is None or r[0] == "skip":
                            key = ("DROPPED" if r is None else "SKIPPED", name)
                            dropped[key] += 1
                            examples.setdefault(key, st)
                            continue
                        if not r[0].startswith("deci:Block") or r[0].startswith(SURFACES):
                            continue
                        below = layers[y - 1][z][x] if y > 0 else None
                        above = layers[y + 1][z][x] if y + 1 < H else None
                        sides = [layers[y][zz][xx] for xx, zz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1))
                                 if 0 <= xx < 16 and 0 <= zz < 16]
                        c = props[name]
                        if solid(below) and not solid(above):
                            c[0] += 1
                        elif not solid(below) and solid(above):
                            c[1] += 1
                        elif any(solid(s) for s in sides):
                            c[2] += 1
                        else:
                            c[3] += 1
                        targets[name] = r[0]
                        examples.setdefault(("PROP", name), st)
    with open(out, "w") as f:
        f.write("kind\tsource\tcount\ttarget\tfloor\tceiling\twall\tfree\texample\n")
        for (kind, name), n in dropped.most_common():
            f.write("%s\t%s\t%d\t\t\t\t\t\t%s\n" % (kind, name, n, examples[(kind, name)]))
        for name, c in sorted(props.items(), key=lambda kv: -sum(kv[1])):
            f.write("PROP\t%s\t%d\t%s\t%d\t%d\t%d\t%d\t%s\n" % (name, sum(c), targets[name], c[0], c[1], c[2], c[3],
                                                             examples[("PROP", name)]))
    print("%d building chunks audited" % len(seen))
    print("--- dropped / skipped, top 25")
    for (kind, name), n in dropped.most_common(25):
        print("%7d %-8s %s" % (n, kind, name))
    print("--- props by placement (floor ceiling wall free), top 25")
    for name, c in sorted(props.items(), key=lambda kv: -sum(kv[1]))[:25]:
        print("%7d %-45s -> %-28s %6d %6d %6d %6d" % (sum(c), name, targets[name], c[0], c[1], c[2], c[3]))


if __name__ == "__main__":
    main()
