#!/usr/bin/env python3
"""Floor plans of generated city buildings, read from the world files.

    python3 tools/floorplan.py WORLD_DIR SERVER_LOG OUT_DIR [ID ...] [--sample N]

For each building (the ids given, or N per kind), one PNG with every
storey side by side (ground, then up to 3 upper storeys, then the roof),
sliced at furniture height (floor + 1) with the window band (floor + 2)
drawn as a thin inner frame. 14 px per block, a letter on every prop.
Also prints, per storey, the floor area, how much of it is walkable and
reachable from the stairs / entrance (flood fill), and prop counts, so
dead rooms, blocked doors and empty floors show up as numbers.
"""
import collections
import os
import re
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402

PX = 14
FLOOR = 5  # storey height when the log line does not say (before 0.23)

# (substring of the block name, colour, letter); first match wins
STYLE = [
    ("glass", (170, 210, 235), ""),
    ("door", (150, 95, 50), "D"),
    ("ladder", (230, 200, 60), "L"),
    ("quartz_stairs", (235, 235, 230), "o"),
    ("oak_stairs", (160, 110, 60), "h"),
    ("spruce_stairs", (110, 80, 50), "h"),
    ("stairs", (225, 150, 60), "/"),
    ("cauldron", (90, 90, 100), "u"),
    ("minecraft:carpet", (200, 120, 120), ""),
    ("stone_slab", (175, 160, 140), "s"),
    ("bed", (200, 60, 60), "B"),
    ("bookshelf", (120, 80, 40), "b"),
    ("chest", (160, 110, 40), "C"),
    ("furnace", (110, 110, 110), "F"),
    ("crafting", (170, 120, 60), "W"),
    ("carpet", (205, 185, 150), ""),
    ("leaves", (80, 150, 60), ""),
    ("vine", (60, 130, 50), ""),
    ("grass", (100, 160, 70), ""),
    ("snow", (240, 240, 250), ""),
    ("sand", (220, 205, 150), ""),
    ("web", (230, 230, 230), "w"),
    ("deci:BlockMetalShelf", (90, 120, 160), "S"),
    ("deci:BlockWoodTable", (190, 140, 80), "T"),
    ("deci:BlockMetalTable", (150, 150, 165), "T"),
    ("deci:BlockOfficeChair", (60, 60, 70), "c"),
    ("deci:BlockChair", (160, 110, 70), "c"),
    ("deci:BlockWeaponCabinet", (100, 70, 50), "K"),
    ("deci:BlockCookingStation", (200, 90, 40), "k"),
    ("deci:BlockWashingMachine", (230, 230, 235), "M"),
    ("deci:BlockVending", (200, 40, 40), "V"),
    ("deci:BlockTrashcan", (90, 110, 90), "t"),
    ("deci:BlockMailbox", (60, 80, 160), "m"),
    ("deci:BlockCardboard", (190, 160, 110), "x"),
    ("Crate", (170, 120, 50), "X"),
    ("WallOffice", (215, 200, 195), ""),
    ("FloorCarpet", (150, 150, 160), ""),
    ("FloorTiles", (120, 120, 120), ""),
    ("deci:BlockStone_", (190, 190, 190), ""),
    ("deci:BlockLight", (250, 240, 150), "l"),
    ("CeilingVent", (100, 100, 100), "v"),
    ("FlatscreenTV", (20, 20, 30), "V"),
    ("ElectricBoxBin", (210, 210, 220), "F"),
    ("Stereo", (40, 40, 40), "R"),
    ("Radio1", (40, 40, 40), "R"),
    ("CocaPlant", (60, 150, 60), "p"),
    ("Bicycle", (90, 90, 90), "y"),
    ("WoodTable2", (190, 140, 80), "T"),
    ("TrashBag", (30, 30, 30), "g"),
    ("deci:", (230, 80, 200), "?"),
]
WALL = (70, 70, 72)
AIR = (250, 250, 248)
RUBBLE = ("cobblestone", "gravel", "mossy")


def style(name):
    if name is None:
        return (0, 0, 0), ""
    if name == "minecraft:air":
        return AIR, ""
    for key, colour, letter in STYLE:
        if key.lower() in name.lower():
            return colour, letter
    if any(r in name for r in RUBBLE):
        return (175, 160, 140), "r"
    return WALL, ""


def walkable(names, x, z):
    """Feet cell and head cell passable."""
    a = names.get((x, 1, z))
    b = names.get((x, 2, z))

    def free(n):
        # passable: air, doors, things you walk through or step onto (half slabs)
        return n is not None and (n == "minecraft:air" or any(k in n.lower() for k in (
            "door", "carpet", "vine", "web", "snow_layer", "tallgrass", "torch", "trashbag", "stone_slab",
            "wooden_slab")) and "double" not in n)
    return free(a) and free(b)


def main():
    args = sys.argv[1:]
    sample = 2
    if "--sample" in args:
        i = args.index("--sample")
        sample = int(args[i + 1])
        del args[i:i + 2]
    world_dir, log, out = args[0], args[1], args[2]
    wanted = set(args[3:])
    os.makedirs(out, exist_ok=True)
    w = wc.World(world_dir)
    names = {v: k for k, v in w.registry().items()}
    names[0] = "minecraft:air"
    # "storey N" (storey height) since 0.23; older logs are all 5 high
    rx = re.compile(r"city (\w+) (\d+)x(\d+), (\d+) floor\(s\), (\w+), (?:storey (\d+), )?footprint (-?\d+),(-?\d+) "
                    r"(b\S+) at -?\d+,(-?\d+),-?\d+")
    per_kind = collections.Counter()
    for line in open(log, errors="ignore"):
        m = rx.search(line)
        if not m:
            continue
        kind, wd, ld, fl, sty, sh, fx, fz, bid, by = m.groups()
        wd, ld, fl, fx, fz, by = map(int, (wd, ld, fl, fx, fz, by))
        floor_h = int(sh) if sh else FLOOR
        if wanted and bid not in wanted:
            continue
        if not wanted:
            if per_kind[kind] >= sample:
                continue
        if w.block(fx, by, fz) is None or w.block(fx + wd - 1, by, fz + ld - 1) is None:
            continue  # not generated
        per_kind[kind] += 1
        storeys = list(range(min(fl, 4)))
        if fl > 4:
            storeys[-1] = fl - 1
        panels = []
        print("%s %s %dx%d %d floor(s) %s at %d,%d,%d" % (bid, kind, wd, ld, fl, sty, fx, by, fz))
        for s in storeys + ["roof"]:
            y0 = by + (fl * floor_h if s == "roof" else s * floor_h)
            cells = {}
            for x in range(fx - 1, fx + wd + 1):
                for z in range(fz - 1, fz + ld + 1):
                    for dy in (0, 1, 2):
                        b = w.block(x, y0 + dy, z)
                        cells[(x - fx + 1, dy, z - fz + 1)] = names.get(b, "id%s" % b) if b is not None else None
            img = Image.new("RGB", ((wd + 2) * PX, (ld + 2) * PX), (30, 30, 30))
            d = ImageDraw.Draw(img)
            props = collections.Counter()
            for (x, dy, z), n in cells.items():
                if dy != 1:
                    continue
                colour, letter = style(n)
                band = cells.get((x, 2, z))
                bc, _ = style(band)
                x0, z0 = x * PX, z * PX
                d.rectangle([x0, z0, x0 + PX - 1, z0 + PX - 1], fill=bc)
                d.rectangle([x0 + 2, z0 + 2, x0 + PX - 3, z0 + PX - 3], fill=colour)
                if letter:
                    d.text((x0 + 4, z0 + 1), letter, fill=(0, 0, 0))
                    props[letter] += 1
            # reachability from stairs / ladders / doors on this storey
            if s != "roof":
                area = [(x, z) for x in range(1, wd + 1) for z in range(1, ld + 1)]
                walk = {c for c in area if walkable(cells, *c)}
                seeds = [c for c in area if cells.get((c[0], 1, c[1])) and any(
                    k in cells[(c[0], 1, c[1])].lower() for k in ("stairs", "ladder", "door"))]
                if s == 0:
                    seeds += [c for c in walk if c[0] in (1, wd) or c[1] in (1, ld)]
                seen, todo = set(), [c for c in seeds if c in walk or True]
                while todo:
                    c = todo.pop()
                    if c in seen:
                        continue
                    seen.add(c)
                    for n in ((c[0] + 1, c[1]), (c[0] - 1, c[1]), (c[0], c[1] + 1), (c[0], c[1] - 1)):
                        if n in walk and n not in seen:
                            todo.append(n)
                reach = len(seen & walk)
                print("  storey %-4s walkable %3d reachable %3d (%3.0f%%)  props %s"
                      % (s, len(walk), reach, 100.0 * reach / max(1, len(walk)), dict(props)))
            panels.append((str(s), img))
        W = sum(p[1].width for p in panels) + 10 * (len(panels) + 1)
        H = max(p[1].height for p in panels) + 30
        sheet = Image.new("RGB", (W, H), (255, 255, 255))
        dd = ImageDraw.Draw(sheet)
        xo = 10
        for label, im in panels:
            dd.text((xo, 5), "storey " + label if label != "roof" else "roof", fill=(0, 0, 0))
            sheet.paste(im, (xo, 20))
            xo += im.width + 10
        path = os.path.join(out, "%s_%s.png" % (bid, kind))
        sheet.save(path)
        print("  ->", path)


if __name__ == "__main__":
    main()
