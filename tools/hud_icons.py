#!/usr/bin/env python3
"""HUD icons of our own in the pixel style of Decimation's status icons (icon_food / water / blood / health).

    python3 tools/hud_icons.py            # writes the icons into dev/src/main/resources/assets/deciworldgen/textures/gui
    python3 tools/hud_icons.py --sheet F  # also a 4x sheet of them for a look

Decimation's icons (measured 22 Oktober 2026 from its jar, never copied): 40 x 40, hard pixel shapes, a flat fill whose
lower 40 % darkens to about 0.6 of it at the bottom row, a black outline ring right around the fill (alpha about 230,
pixels touching the fill only by a corner about 96) and a soft ring outside it (alpha about 50), nothing beyond.
Our art only: the shapes are drawn here from geometry.
"""
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dev/src/main/resources/assets/deciworldgen/textures/gui")
SIZE = 40


def trefoil(x, y):
    """The radiation sign: a centre dot and three blades (top right, top left, bottom), pixel centres tested."""
    dx, dy = x + 0.5 - SIZE / 2, SIZE / 2 - (y + 0.5)
    r = math.hypot(dx, dy)
    if r <= 3.6:
        return True
    if r < 6.4 or r > 17.6:
        return False
    a = math.degrees(math.atan2(dy, dx)) % 360
    return any(abs((a - c + 180) % 360 - 180) <= 30 for c in (30, 150, 270))


def icon(shape, colour):
    fill = {(x, y) for x in range(SIZE) for y in range(SIZE) if shape(x, y)}
    top = min(y for _, y in fill)
    bottom = max(y for _, y in fill)
    knee = top + 0.6 * (bottom - top)
    im = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = im.load()

    def near(p, diagonal):
        x, y = p
        steps = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (1, -1), (-1, 1), (-1, -1)] if diagonal else [])
        return [(x + a, y + b) for a, b in steps if 0 <= x + a < SIZE and 0 <= y + b < SIZE]

    side = {q for p in fill for q in near(p, False) if q not in fill}
    corner = {q for p in fill for q in near(p, True) if q not in fill and q not in side}
    soft = {q for p in side | corner for q in near(p, True) if q not in fill and q not in side and q not in corner}
    for (x, y) in fill:
        k = 1.0 if y <= knee else 1.0 - 0.4 * (y - knee) / max(1.0, bottom - knee)
        px[x, y] = tuple(int(round(c * k)) for c in colour) + (255,)
    for p in side:
        px[p] = (0, 0, 0, 230)
    for p in corner:
        px[p] = (0, 0, 0, 96)
    for p in soft:
        px[p] = (0, 0, 0, 50)
    return im


ICONS = {"hud_radiation.png": (trefoil, (190, 160, 42))}


def main():
    os.makedirs(OUT, exist_ok=True)
    made = []
    for name, (shape, colour) in ICONS.items():
        im = icon(shape, colour)
        im.save(os.path.join(OUT, name))
        made.append(im)
        print("wrote", os.path.relpath(os.path.join(OUT, name), ROOT))
    if "--sheet" in sys.argv:
        path = sys.argv[sys.argv.index("--sheet") + 1]
        sheet = Image.new("RGBA", (170 * len(made), 170), (128, 128, 128, 255))
        for i, im in enumerate(made):
            big = im.resize((160, 160), Image.NEAREST)
            sheet.paste(big, (170 * i, 5), big)
        sheet.save(path)
        print("sheet", path)


if __name__ == "__main__":
    main()
