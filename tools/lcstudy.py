#!/usr/bin/env python3
"""Study a Lost Cities data pack (e.g. DeceasedCraft's): building types,
storey plans and furniture use, as reference for our generator.

    python tools/lcstudy.py DATA_DIR NAMESPACE OUT_DIR

DATA_DIR: the extracted `data/` folder (e.g. from DCTweaks_*.jar, see
docs/references/deceasedcraft_buildings.md). Needs pillow. Writes:
  buildings.tsv  one row per building (multi buildings as one): chunks,
                 floors, storey height, interior cells, furniture cells
                 by category, density, doors, windows, top furniture
  furniture.tsv  every furniture block id with its category and count
  plans/<name>.png  per storey the furniture layer (slice 1 + 2 of the
                 part) drawn top down, coloured by category
Only the unrotated variant of each building is studied (rotations are
copies).
"""
import collections
import json
import os
import re
import sys

from PIL import Image, ImageDraw

CATS = [  # first match wins; keys are matched against the block id
    ("door", ("door",)),
    ("glass", ("glass", "pane", "window")),
    ("bed", ("bed",)),
    ("bath", ("toilet", "basin", "bath", "shower", "regadera", "toalla", "papelhigienico", "llaves", "sink_bath")),
    ("kitchen", ("stove", "fridge", "freezer", "cabinetry", "kitchen", "oven", "range_hood", "toaster", "microwave",
                 "frying_pan", "cutting_board", "counter", "sink", "dishwasher", "bread", "tray", "jar")),
    ("seat", ("chair", "sofa", "stool", "bench", "couch", "seat", "armchair")),
    ("table", ("desk", "table")),
    ("storage", ("drawer", "shelf", "shelves", "cabinet", "wardrobe", "locker", "crate", "barrel", "storage",
                 "bookshelf", "cupboard")),
    ("loot", ("chest", "lootr")),
    ("light", ("lamp", "light", "lantern", "illuminant", "ceiling_fan", "torch", "glowstone", "chandelier")),
    ("decor", ("curtain", "potted", "plant", "painting", "rug", "carpet", "radio", "tv", "computer", "monitor",
               "clock", "vase", "flower", "picture", "frame", "mirror", "book", "trash", "bin", "sign", "poster",
               "doorbell", "switch", "fan", "speaker", "phone", "laptop", "printer", "register", "machine")),
    ("shape", ("stairs", "slab", "vertical_slab", "fence", "wall", "trapdoor", "shutter", "bars", "pressure_plate")),
]
COLOURS = {
    "air": (245, 244, 238), "solid": (105, 100, 92), "glass": (150, 200, 225), "door": (0, 0, 0),
    "bed": (200, 60, 60), "bath": (60, 160, 200), "kitchen": (230, 160, 40), "seat": (120, 170, 60),
    "table": (150, 100, 60), "storage": (130, 90, 160), "loot": (240, 220, 40), "light": (255, 240, 150),
    "decor": (220, 120, 170), "shape": (150, 145, 135),
}
CELL = 12  # pixels per block in the plans
FURNITURE = ("bed", "bath", "kitchen", "seat", "table", "storage", "loot", "decor", "light")


def category(block):
    if block is None:
        return "air"
    b = block.split("[")[0]
    if b in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
        return "air"
    # colour words and block shapes must not decide the category
    name = re.sub(r"light_gray|light_blue", "", b.split(":")[-1])
    if name == "plate":
        return "kitchen"
    if "metal_plate" in name or "pressure_plate" in name:
        return "shape"
    for cat, keys in CATS:
        if any(k in name for k in keys):
            return cat
    return "solid"


class Pack:
    def __init__(self, data, ns):
        self.root = os.path.join(data, ns, "lostcities")
        self.ns = ns
        self.palettes = {}

    def load(self, kind, ref):
        ns, _, path = ref.partition(":")
        if not path:
            ns, path = self.ns, ref
        f = os.path.join(os.path.dirname(self.root), "..", ns, "lostcities", kind, path + ".json")
        f = os.path.normpath(f)
        return json.load(open(f)) if os.path.exists(f) else None

    def palette(self, ref):
        if ref not in self.palettes:
            p = self.load("palettes", ref) or {}
            m = {}
            for e in p.get("palette", []):
                b = e.get("block")
                if b is None and e.get("blocks"):
                    b = e["blocks"][0].get("block")
                if b is None:
                    b = e.get("frompalette") or "?"
                m[e["char"]] = b
            self.palettes[ref] = m
        return self.palettes[ref]


def part_blocks(pack, ref, building_palette):
    """slices (list of 16 rows each) and a char -> block function."""
    p = pack.load("parts", ref)
    if p is None:
        return None, None
    pal = {}
    for r in (building_palette, p.get("refpalette")):
        if r:
            pal.update(pack.palette(r))
    if isinstance(p.get("palette"), dict):
        pal.update({k: v.get("block") if isinstance(v, dict) else v for k, v in p["palette"].items()})
    return p["slices"], lambda ch: None if ch == " " else pal.get(ch, "?")


def storeys(pack, bref):
    b = pack.load("buildings", bref)
    if b is None:
        return [], 0
    parts = [p["part"] for p in b.get("parts", []) if not p.get("cellar")]
    # parts2 overlays the same storeys (inpart -> extra part); merge them
    extra = collections.defaultdict(list)
    for p in b.get("parts2", []):
        extra[p.get("inpart")].append(p["part"])
    out = []
    for ref in parts:
        out.append((ref, extra.get(ref, []), b.get("refpalette")))
    return out, b.get("maxfloors", len(parts))


def main():
    data, ns, out = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(os.path.join(out, "plans"), exist_ok=True)
    pack = Pack(data, ns)
    root = pack.root
    groups = {}  # name -> list of rows of building refs (chunk grid)
    for f in sorted(os.listdir(os.path.join(root, "multibuildings"))):
        m = json.load(open(os.path.join(root, "multibuildings", f)))
        groups[f[:-5]] = m["buildings"]
    for d in sorted(os.listdir(os.path.join(root, "buildings"))):
        if d.startswith("multi_") or not os.path.isdir(os.path.join(root, "buildings", d)):
            continue
        groups[d] = [["%s:%s/%s" % (ns, d, d)]]
    furniture = collections.Counter()
    rows = []
    for name, grid in groups.items():
        dimx = len(grid)
        dimz = max(len(r) for r in grid)
        cats = collections.Counter()
        items = collections.Counter()
        floors = 0
        heights = set()
        interior = 0
        plans = {}  # storey -> image
        for gx, col in enumerate(grid):
            for gz, bref in enumerate(col):
                st, _ = storeys(pack, bref)
                floors = max(floors, len(st))
                for si, (ref, extras, bpal) in enumerate(st):
                    for pref in [ref] + extras:
                        slices, blk = part_blocks(pack, pref, bpal)
                        if not slices:
                            continue
                        heights.add(len(slices))
                        img = plans.setdefault(si, Image.new("RGB", (dimx * 16 * CELL, dimz * 16 * CELL), (25, 25, 25)))
                        d = ImageDraw.Draw(img)
                        floor_s = slices[0]
                        for z in range(16):
                            for x in range(16):
                                fl = blk(floor_s[z][x]) if z < len(floor_s) and x < len(floor_s[z]) else None
                                found = []
                                for s in slices[1:3]:
                                    ch = s[z][x] if z < len(s) and x < len(s[z]) else " "
                                    found.append(blk(ch))
                                c1 = category(found[0]) if found else "air"
                                c2 = category(found[1]) if len(found) > 1 else "air"
                                if fl is not None and c1 not in ("solid", "glass") and c2 not in ("solid", "glass"):
                                    interior += 1
                                for b in found:
                                    c = category(b)
                                    cats[c] += 1
                                    if c in FURNITURE:
                                        k = b.split("[")[0]
                                        items[k] += 1
                                        furniture[(c, k)] += 1
                                if fl is None and c1 == "air":
                                    continue
                                c = c1 if c1 not in ("air",) else (c2 if c2 in FURNITURE else "air")
                                # in Lost Cities parts x runs along the row, z down the rows
                                px, pz = (gx * 16 + x) * CELL, (gz * 16 + z) * CELL
                                d.rectangle([px, pz, px + CELL - 2, pz + CELL - 2], fill=COLOURS.get(c, (255, 0, 255)))
        if not plans:
            continue
        fcells = sum(cats[c] for c in FURNITURE)
        rows.append((name, "%dx%d" % (dimx, dimz), floors, "/".join(str(h) for h in sorted(heights)), interior,
                     fcells, round(fcells / max(1, interior), 2), cats["door"], cats["glass"],
                     " ".join("%s:%d" % (c, cats[c]) for c in FURNITURE if cats[c]),
                     ", ".join("%s %d" % (k.split(":")[-1], v) for k, v in items.most_common(12))))
        for k in sorted(plans):
            plans[k].save(os.path.join(out, "plans", "%s_s%d.png" % (name, k)))
        panels = [plans[k] for k in sorted(plans)][:6]
        W = sum(p.width + 10 for p in panels) + 10
        H = max(p.height for p in panels) + 26
        sheet = Image.new("RGB", (W, H), (255, 255, 255))
        d = ImageDraw.Draw(sheet)
        xo = 10
        for i, p in enumerate(panels):
            d.text((xo, 6), "storey %d" % i, fill=(0, 0, 0))
            sheet.paste(p, (xo, 22))
            xo += p.width + 10
        sheet.save(os.path.join(out, "plans", name + ".png"))
    with open(os.path.join(out, "buildings.tsv"), "w") as f:
        f.write("building\tchunks\tfloors\tslices\tinterior\tfurniture\tdensity\tdoors\tglass\tby category\ttop items\n")
        for r in rows:
            f.write("\t".join(str(v) for v in r) + "\n")
    with open(os.path.join(out, "furniture.tsv"), "w") as f:
        for (c, k), v in furniture.most_common():
            f.write("%s\t%s\t%d\n" % (c, k, v))
    print("%d buildings studied" % len(rows))


if __name__ == "__main__":
    main()
