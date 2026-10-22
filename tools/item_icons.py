#!/usr/bin/env python3
"""Item icons of our own in the style of Decimation's item icons (firstaidkit, kit_metalwall, scraps/*).

    python3 tools/item_icons.py              # writes dev/src/main/resources/assets/deciworldgen/textures/items/<name>.png
    python3 tools/item_icons.py --sheet F    # also an 8x sheet for a look

Decimation's item icons (looked at 22 Oktober 2026 from its jar, never copied): 32 x 32, an object in a slight three
quarter view, a 1 px near black outline, muted fills lighter at the top left and darker at the bottom right, a little
texture noise. Our shapes are drawn here from geometry.

The radiation pills (the feature agent, 22 Oktober 2026) follow Decimation's medical icons instead (painkillers,
vitamincapsules): every pixel fully opaque or empty, the shape's border in one dark slate (64, 66, 72), fills shaded
like a lit cylinder; radiation_pills.png is an amber pill bottle with a white child safe cap and a yellow label carrying
a black radiation sign.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dev/src/main/resources/assets/deciworldgen/textures/items")
S = 32
OUTLINE = (22, 22, 20, 255)
PILL_OUTLINE = (64, 66, 72)


def shade(base, x, y, noise, rnd):
    """Lighter toward the top left, darker toward the bottom right, with a little noise."""
    k = 1.18 - 0.36 * (x + y) / (2 * S)
    n = (rnd.random() - 0.5) * noise
    return tuple(max(0, min(255, int(c * k + n))) for c in base) + (255,)


def layer(polys, base, noise=14, seed=1):
    """A filled shape (polygons, one colour, shaded) on its own layer."""
    mask = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(mask)
    for p in polys:
        d.polygon(p, fill=255)
    rnd = random.Random(seed)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px, m = im.load(), mask.load()
    for y in range(S):
        for x in range(S):
            if m[x, y]:
                px[x, y] = shade(base, x, y, noise, rnd)
    return im


def outline(im):
    """A 1 px outline round everything drawn (8 neighbours), as Decimation's icons have."""
    px = im.load()
    solid = {(x, y) for x in range(S) for y in range(S) if px[x, y][3] > 0}
    out = im.copy()
    o = out.load()
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if 0 <= q[0] < S and 0 <= q[1] < S and q not in solid:
                    o[q] = OUTLINE
    return out


def stack(*layers):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for l in layers:
        im = Image.alpha_composite(im, l)
    return im


def armor_part():
    """An olive kit case in three quarter view with a grey plate emblem and a strap."""
    top = [(5, 11), (19, 5), (28, 9), (14, 15)]
    front = [(5, 11), (14, 15), (14, 27), (5, 23)]
    side = [(14, 15), (28, 9), (28, 21), (14, 27)]
    case = stack(layer([front], (88, 92, 52), seed=2), layer([side], (64, 68, 38), seed=3), layer([top], (110, 114, 66), seed=4))
    emblem = layer([[(17, 15), (25, 11), (25, 18), (21, 22), (17, 20)]], (150, 152, 150), noise=6, seed=5)
    strap = layer([[(8, 12), (10, 13), (10, 25), (8, 24)]], (40, 40, 34), noise=4, seed=6)
    return outline(stack(case, strap, emblem))


def armor_plate():
    """A ballistic plate with cut top corners, curved a little, in three quarter view."""
    face = [(8, 6), (12, 3), (22, 6), (26, 10), (24, 28), (8, 24)]
    edge = [(24, 28), (26, 10), (28, 11), (26, 29)]
    plate = stack(layer([face], (66, 70, 64), noise=10, seed=7), layer([edge], (40, 42, 38), noise=4, seed=8))
    band = layer([[(9, 14), (25, 18), (25, 20), (9, 16)]], (96, 100, 92), noise=4, seed=9)
    return outline(stack(plate, band))


def gun_parts():
    """A bolt carrier and a recoil spring."""
    bolt = [(4, 16), (19, 9), (22, 13), (7, 20)]
    lug = [(17, 10), (21, 8), (23, 11), (20, 13)]
    carrier = stack(layer([bolt], (92, 94, 98), noise=8, seed=10), layer([lug], (120, 122, 126), noise=6, seed=11))
    spring = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(spring)
    for i in range(7):
        x = 9 + i * 3
        d.line([(x, 28), (x + 2, 21)], fill=(150, 150, 146, 255), width=1)
        d.line([(x + 2, 21), (x + 3, 28)], fill=(112, 112, 108, 255), width=1)
    return outline(stack(carrier, spring))


# ---- the radiation pills (Decimation's medical icon style)

def pill_shade(colour, x, x0, x1):
    """A lit cylinder across x0 .. x1: brightest a third in, darkest at the right edge."""
    t = (x + 0.5 - x0) / (x1 + 1 - x0)
    k = 1.25 - 0.9 * abs(t - 0.32) if t < 0.32 else 1.25 - 0.75 * (t - 0.32) / 0.68 - 0.1
    k = max(0.55, min(1.3, k))
    return tuple(max(0, min(255, int(round(c * k)))) for c in colour)


def pill_trefoil(x, y, cx, cy, r):
    """The radiation sign around (cx, cy), radius r: a centre dot and three blades (top left, top right, bottom)."""
    dx, dy = x + 0.5 - cx, cy - (y + 0.5)
    d = math.hypot(dx, dy)
    if d <= r * 0.16:
        return True
    if d < r * 0.36 or d > r:
        return False
    a = math.degrees(math.atan2(dy, dx)) % 360
    return any(abs((a - c + 180) % 360 - 180) <= 30 for c in (30, 150, 270))


def radiation_pills():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()
    x0, x1 = 9, 23                                               # 15 wide: the sign sits on a pixel column
    amber, cap, label = (176, 98, 30), (226, 226, 222), (232, 196, 48)
    cx, cy = (x0 + x1 + 1) / 2, 18.5
    for y in range(5, 27):
        for x in range(x0, x1 + 1):
            if y <= 9:                                           # cap with grip ridges
                if y == 5 and x in (x0, x1):
                    continue
                c = pill_shade(cap, x, x0, x1)
                if y >= 6 and (x - x0) % 2 == 1:
                    c = tuple(int(v * 0.86) for v in c)
            elif y == 10:                                        # neck under the cap
                if x in (x0, x1):
                    continue
                c = pill_shade((120, 64, 22), x, x0, x1)
            elif 12 <= y <= 24:                                  # label round the bottle
                c = pill_shade(label, x, x0, x1)
                if y in (12, 24):
                    c = tuple(int(v * 0.72) for v in c)
                elif pill_trefoil(x, y, cx, cy, 6.0):
                    c = (24, 22, 20)
            else:                                                # amber glass
                if y == 26 and x in (x0, x1):
                    continue
                c = pill_shade(amber, x, x0, x1)
                if x == x0 + 3:
                    c = tuple(min(255, int(v * 1.35)) for v in c)   # the glass shine
            px[x, y] = c + (255,)
    edge = [(x, y) for y in range(S) for x in range(S) if px[x, y][3]
            and any(not (0 <= x + a < S and 0 <= y + b < S) or px[x + a, y + b][3] == 0
                    for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for p in edge:
        px[p] = PILL_OUTLINE + (255,)
    for x in range(x0 + 1, x1):                                  # the cap's rim
        px[x, 9] = PILL_OUTLINE + (255,)
    return im


ICONS = {"armor_part.png": armor_part, "armor_plate.png": armor_plate, "gun_parts.png": gun_parts,
         "radiation_pills.png": radiation_pills}


def main():
    os.makedirs(OUT, exist_ok=True)
    made = []
    for name, draw in ICONS.items():
        im = draw()
        im.save(os.path.join(OUT, name))
        made.append(im)
        print("wrote", os.path.relpath(os.path.join(OUT, name), ROOT))
    if "--sheet" in sys.argv:
        path = sys.argv[sys.argv.index("--sheet") + 1]
        sheet = Image.new("RGBA", (S * 8 * len(made) + 10 * len(made), S * 8), (128, 128, 128, 255))
        for i, im in enumerate(made):
            big = im.resize((S * 8, S * 8), Image.NEAREST)
            sheet.paste(big, (i * (S * 8 + 10), 0), big)
        sheet.save(path)
        print("sheet", path)


if __name__ == "__main__":
    main()
