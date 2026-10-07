#!/usr/bin/env python3
"""Find and measure the buildings of a hand-built (Decimation) world.

    python tools/mapbuildings.py WORLD_DIR OUT_DIR [x0 z0 x1 z1]

Needs numpy + pillow. A building = a connected group of columns (8
neighbours) that hold at least 3 non-natural blocks 2+ above the column's
ground (the block below its lowest air), so paving does not count.
For each building (area >= 24 columns) writes a row to OUT_DIR/buildings.tsv:
id, bbox, footprint area, height, estimated storeys (floor layers = y
levels covering >= 55% of the footprint), the main wall / floor blocks,
Decimation props by count and a category guess from the props. OUT_DIR/cams.tsv:
camera spots per storey (building, storey, x, y, z, yaw) for the
autotest study mode (-Pstudy). Plus, per building, OUT_DIR/b<id>.png: one top-down slice per storey (1 above each
floor layer) so layouts can be compared with ours (docs/references/
decimation_maps.md).
"""
import collections
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mapsurvey as ms  # noqa: E402

CATEGORY_HINTS = [
    ("military", ("Military", "Sandbag", "ConcertinaWire", "Hedgehog", "MissileLauncher", "WeaponCabinet", "AmmoCrate",
                  "Spotlight", "WreckageMilitary")),
    ("police", ("PoliceCrate", "WreckagePolice", "Door_security", "KeycardScreen", "CCTV")),
    ("medical", ("Medical", "Stretcher", "BodyBag", "HazardScreen")),
    ("store", ("MetalShelf", "ShopDisplay", "Vending", "Sign_", "NewsStand")),
    ("office", ("OfficeChair", "Monitor", "WallOffice", "Elevator", "MetalTable")),
    ("home", ("Chair", "WoodTable", "FlatscreenTV", "WashingMachine", "CookingStation", "Stereo")),
]


def main():
    world, out = sys.argv[1], sys.argv[2]
    box = [int(v) for v in sys.argv[3:7]] if len(sys.argv) >= 7 else None
    os.makedirs(out, exist_ok=True)
    reg = ms.registry(world)
    reg[0] = "minecraft:air"
    natural = np.zeros(4096, dtype=bool)
    natural[list(ms.NATURAL | {78, 31, 175})] = True
    for i, n in reg.items():
        if "Road" in n or n.endswith(("tallgrass", "flower", "sapling")):
            natural[i] = True
    cols = {}      # (x, z) -> count of built blocks
    chunkdata = {}
    for level in ms.chunks(world):
        cx, cz = level["xPos"], level["zPos"]
        if box and not (box[0] - 16 <= cx * 16 <= box[2] and box[1] - 16 <= cz * 16 <= box[3]):
            continue
        ids = ms.chunk_ids(level)
        built = (~natural[ids]) & (ids != 0)
        # ground of a column = just below its lowest air block (y >= 1);
        # only built blocks 2+ above it count, so paving / roads / plazas
        # do not glue a whole town into one "building"
        air = ids == 0
        air[0] = False
        ground = np.argmax(air, axis=0) - 1
        yy = np.arange(256)[:, None, None]
        built &= yy >= (ground[None, :, :] + 2)
        cnt = built.sum(axis=0)
        if cnt.max() < 3:
            continue
        chunkdata[(cx, cz)] = ids
        for z in range(16):
            for x in range(16):
                if cnt[z, x] >= 3:
                    cols[(cx * 16 + x, cz * 16 + z)] = int(cnt[z, x])
    # connected components
    seen, comps = set(), []
    for c in cols:
        if c in seen:
            continue
        stack, comp = [c], []
        seen.add(c)
        while stack:
            p = stack.pop()
            comp.append(p)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    q = (p[0] + dx, p[1] + dz)
                    if q in cols and q not in seen:
                        seen.add(q)
                        stack.append(q)
        if len(comp) >= 24:
            comps.append(comp)
    comps.sort(key=len, reverse=True)

    def column(x, z):
        ids = chunkdata.get((x >> 4, z >> 4))
        return None if ids is None else ids[:, z & 15, x & 15]

    rows = []
    cams = []
    for bi, comp in enumerate(comps[:400]):
        xs = [p[0] for p in comp]
        zs = [p[1] for p in comp]
        x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
        layer = collections.Counter()   # y -> built columns
        blocks = collections.Counter()
        props = collections.Counter()
        ymin, ymax = 255, 0
        for (x, z) in comp:
            col = column(x, z)
            if col is None:
                continue
            for y in np.nonzero((~natural[col]) & (col != 0))[0]:
                b = int(col[y])
                name = reg.get(b, str(b))
                blocks[name] += 1
                if name.startswith("deci:"):
                    props[name[5:]] += 1
                layer[int(y)] += 1
                ymin, ymax = min(ymin, int(y)), max(ymax, int(y))
        floors = sorted(y for y, n in layer.items() if n >= 0.55 * len(comp))
        # a floor can be several layers thick (structure + carpet blocks):
        # group consecutive full layers, the storey's walking level is the
        # top layer of each group
        groups = []
        for y in floors:
            if groups and y <= groups[-1][-1] + 1:
                groups[-1].append(y)
            else:
                groups.append([y])
        levels = [g[-1] for g in groups if any(layer.get(g[-1] + k, 0) < 0.55 * len(comp) for k in (1, 2))]
        # camera spots: per storey the walkable cell with the longest straight
        # open line (feet and head air), looking along it
        for si, y in enumerate(levels):
            best = None
            for (x, z) in comp:
                col = column(x, z)
                if col is None or y + 8 > 255 or col[y + 1] != 0 or col[y + 2] != 0:
                    continue
                if not col[y + 3:y + 9].any():
                    continue  # no ceiling above: roof or yard, not a room
                for dx, dz, yaw in ((1, 0, 270), (-1, 0, 90), (0, 1, 0), (0, -1, 180)):
                    run, px, pz = 0, x, z
                    while True:
                        px += dx
                        pz += dz
                        c2 = column(px, pz)
                        if c2 is None or not (x0 <= px <= x1 and z0 <= pz <= z1) or c2[y + 1] != 0 or c2[y + 2] != 0:
                            break
                        run += 1
                    back = column(x - dx, z - dz)
                    if back is not None and back[y + 1] == 0:
                        run -= 2  # prefer standing with the back to a wall
                    if best is None or run > best[0]:
                        best = (run, x, z, yaw)
            if best and best[0] >= 3:
                cams.append((bi, si, best[1], y + 1, best[2], best[3]))
        score = collections.Counter()
        for cat, keys in CATEGORY_HINTS:
            for p, n in props.items():
                if any(k in p for k in keys):
                    score[cat] += n
        cat = score.most_common(1)[0][0] if score else "shell"
        main_blocks = ", ".join("%s %d" % (k.replace("minecraft:", ""), v) for k, v in blocks.most_common(5))
        top_props = ", ".join("%s %d" % (k, v) for k, v in props.most_common(8))
        rows.append((bi, x0, z0, x1, z1, len(comp), ymin, ymax, len(levels), cat, main_blocks, top_props,
                     sum(props.values())))
        # storey slices image
        if len(levels) >= 1 and len(comp) >= 40:
            px = 6
            W, H = (x1 - x0 + 3) * px, (z1 - z0 + 3) * px
            panels = []
            for y in levels[:6]:
                img = Image.new("RGB", (W, H), (25, 25, 25))
                d = ImageDraw.Draw(img)
                for x in range(x0 - 1, x1 + 2):
                    for z in range(z0 - 1, z1 + 2):
                        col = column(x, z)
                        if col is None:
                            continue
                        b1, b2 = int(col[min(255, y + 1)]), int(col[min(255, y + 2)])
                        n1 = reg.get(b1, "")
                        c = (245, 245, 240) if b1 == 0 else ms.colour(n1)
                        d.rectangle([(x - x0 + 1) * px, (z - z0 + 1) * px, (x - x0 + 2) * px - 1, (z - z0 + 2) * px - 1],
                                    fill=c)
                        if n1.startswith("deci:") or "door" in n1.lower() or "bed" in n1 or "chest" in n1:
                            d.rectangle([(x - x0 + 1) * px + 2, (z - z0 + 1) * px + 2, (x - x0 + 2) * px - 3,
                                         (z - z0 + 2) * px - 3], fill=(230, 40, 40))
                panels.append((y, img))
            sheet = Image.new("RGB", (sum(p[1].width + 8 for p in panels) + 8, H + 24), (255, 255, 255))
            d = ImageDraw.Draw(sheet)
            xo = 8
            for y, img in panels:
                d.text((xo, 4), "floor y=%d" % y, fill=(0, 0, 0))
                sheet.paste(img, (xo, 20))
                xo += img.width + 8
            sheet.save(os.path.join(out, "b%d.png" % bi))
    with open(os.path.join(out, "cams.tsv"), "w") as f:
        for c in cams:
            f.write("%d\t%d\t%d\t%d\t%d\t%d\n" % c)
    with open(os.path.join(out, "buildings.tsv"), "w") as f:
        f.write("id\tx0\tz0\tx1\tz1\tarea\tymin\tymax\tstoreys\tcategory\tdeci_blocks\tmain blocks\ttop props\n")
        for r in rows:
            f.write("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\t%d\t%s\t%s\n" % (r[0], r[1], r[2], r[3], r[4], r[5], r[6],
                                                                         r[7], r[8], r[9], r[12], r[10], r[11]))
    print("%d buildings (area >= 24); categories: %s" % (len(rows), dict(collections.Counter(r[9] for r in rows))))


if __name__ == "__main__":
    main()
