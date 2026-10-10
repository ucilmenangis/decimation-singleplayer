#!/usr/bin/env python3
"""Our own HESCO barrier textures (deciworldgen:hesco), drawn from scratch.

    python3 tools/props/hesco_textures.py

A real HESCO MIL cell: a tan geotextile bag inside a galvanised welded wire
mesh (squares about 7.5 cm), coil hinges at the cell joints, filled with
sand or gravel, the fabric dirty toward the ground. 16 x 16, one plain
full block instead of Decimation's BlockMilitaryBarrier prop (a tile entity
per cell, very costly in the thousands a base needs).
"""
import os
import random

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen", "textures", "blocks")

FABRIC = (158, 140, 104)
FILL = (150, 128, 92)
WIRE = (92, 94, 90)
WIRE_HI = (128, 130, 124)
HINGE = (70, 72, 68)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def side(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-9, 9)
            dirt = max(0, y - 9) * 3.2            # dirtier toward the ground
            streak = 6 if (x * 7 + 3) % 11 == 0 else 0   # faint vertical stains in the fabric
            px[x, y] = clamp((FABRIC[0] + n - dirt - streak, FABRIC[1] + n - dirt * 1.1 - streak,
                              FABRIC[2] + n - dirt * 1.2 - streak, 255))
    for y in range(16):
        for x in range(16):
            if x % 4 == 2 or y % 4 == 2:         # the welded mesh, off the block edge
                hi = (x % 4 == 2 and y % 4 == 1) or (y % 4 == 2 and x % 4 == 1)
                c = WIRE_HI if hi else WIRE
                px[x, y] = clamp((c[0] + rnd.uniform(-6, 6), c[1] + rnd.uniform(-6, 6), c[2] + rnd.uniform(-6, 6), 255))
    for y in range(16):                          # coil hinge on the cell joint
        if y % 2 == 0:
            px[0, y] = clamp(HINGE + (255,))
        px[15, y] = clamp((HINGE[0] + 18, HINGE[1] + 18, HINGE[2] + 18, 255)) if y % 2 else px[15, y]
    return img


def top(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            base = FABRIC if edge else FILL
            n = rnd.uniform(-12, 12)
            pebble = -22 if rnd.random() < 0.06 else 0
            px[x, y] = clamp((base[0] + n + pebble, base[1] + n + pebble, base[2] + n + pebble, 255))
    for y in range(16):
        for x in range(16):
            if x % 4 == 2 or y % 4 == 2:
                px[x, y] = clamp((WIRE[0] + rnd.uniform(-8, 8),) * 3 + (255,))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    rnd = random.Random(7)
    side(rnd).save(os.path.join(OUT, "hesco_side.png"))
    top(rnd).save(os.path.join(OUT, "hesco_top.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
