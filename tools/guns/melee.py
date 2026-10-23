#!/usr/bin/env python3
"""Our melee weapons (user, 23 Oktober 2026: a Karambit and a long katana, "a vanilla version of how katana would look
like in a zombie apocalypse"): our own models, textures and icons in Decimation's style (gunmodel.py parts, painting and
part counts like its knives), registered by fixes/NewMelee, drawn by fixes/MeleeRenderer.
Space as Decimation's knives (assets/deci/models/items/combatknife.bmodel): units of 1/16 block, the blade up along -y,
the handle at the bottom (y 0 and down), x across the blade (edge +x), z the thickness, centred on z 0.
    python3 tools/guns/melee.py            # writes both under assets/deciworldgen
    python3 tools/guns/melee.py --sheet    # also a side / three quarter sheet to /tmp/melee_sheet.png
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import Part, layout, paint, part, part_block  # noqa: E402
from sights import oct_y  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen")
G = "meleeModel"
# worn steel, its darker spine and brighter edge (Decimation's knife greys), cloth, brass, tape
STEEL, SPINE, EDGE = (148, 150, 153), (96, 99, 103), (186, 188, 190)
IRON, DARK = (58, 56, 54), (34, 33, 32)
CORD, CORD_DARK = (74, 80, 54), (44, 48, 32)
WRAP, SKIN, TAPE, BRASS = (36, 34, 33), (168, 158, 136), (126, 128, 124), (160, 126, 62)


def prism(name, colour, quad, half):
    """A four sided slab: quad = four (x, y) corners in order (start inner, start outer, end outer, end inner), half =
    its half thickness in z at each corner (a number or four). Any quad Decimation's shaped boxes can hold."""
    if not isinstance(half, (list, tuple)):
        half = [half] * 4
    p = Part(name, G, colour, (0, 0, 0), (1, 1, 1))
    order = {(0, 0): 0, (0, 1): 1, (1, 1): 2, (1, 0): 3}       # (along, outward) -> quad index
    for (ia, io), i in order.items():
        x, y = quad[i]
        for iz in (0, 1):
            p.c[(ia, io, iz)] = [x, y, half[i] * (1 if iz else -1)]
    return p


def arc(cx, cy, r, a):
    return cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a))


def karambit():
    P = []
    # finger ring under the handle: 8 segments round z, centre (0, 2.1)
    for i in range(8):
        a0, a1 = i * 45 - 22.5, i * 45 + 22.5
        q = [arc(0, 2.1, 0.85, a0), arc(0, 2.1, 1.55, a0), arc(0, 2.1, 1.55, a1), arc(0, 2.1, 0.85, a1)]
        P.append(prism("ring%d" % i, STEEL if i % 2 else SPINE, q, 0.32))
    # handle: a tang plate wrapped in paracord, bands alternating dark and light, gently curved toward the edge
    P.append(part("tang", G, IRON, (-0.55, -5.2, -0.18), (0.55, 0.8, 0.18)))
    for i in range(7):
        y0 = 0.55 - i * 0.82
        P.append(part("cord%d" % i, G, CORD if i % 2 else CORD_DARK, (-0.95 + i * 0.06, y0 - 0.8, -0.5),
                      (0.95 + i * 0.06, y0, 0.5)).inset("y", 0, z=(0.06, 0.06)))
    # the bolster / finger guard: a small hook forward at the top of the handle
    P.append(prism("guard", SPINE, [(0.5, -5.1), (0.5, -5.9), (1.9, -5.3), (1.7, -4.9)], 0.42))
    # the claw: an arc rising from the handle and hooking forward (edge on the inside of the curve), 7 segments,
    # wide at the base, a point at the tip; spine thicker than the edge strip on its inner side
    cx, cy, r = 4.4, -5.6, 4.4
    seg = 7
    for i in range(seg):
        a0, a1 = 180 + i * 102.0 / seg, 180 + (i + 1) * 102.0 / seg
        w0 = 1.55 * (1 - i / float(seg)) + 0.05
        w1 = 1.55 * (1 - (i + 1) / float(seg)) + 0.05
        t0, t1 = 0.34 - 0.2 * i / seg, 0.34 - 0.2 * (i + 1) / seg
        inner0, inner1 = r - w0 * 0.35, r - w1 * 0.35
        outer0, outer1 = r + w0 * 0.65, r + w1 * 0.65
        q = [arc(cx, cy, inner0, a0), arc(cx, cy, outer0, a0), arc(cx, cy, outer1, a1), arc(cx, cy, inner1, a1)]
        P.append(prism("blade%d" % i, STEEL, q, [t0, t0, t1, t1]))
        e = [arc(cx, cy, inner0 - 0.32 * (1 - i / float(seg)) - 0.05, a0), arc(cx, cy, inner0, a0),
             arc(cx, cy, inner1, a1), arc(cx, cy, inner1 - 0.32 * (1 - (i + 1) / float(seg)) - 0.05, a1)]
        P.append(prism("edge%d" % i, EDGE, e, [0.08, t0 * 0.6, t1 * 0.6, 0.06]))
    # spine notches (jimping) on the outer curve near the handle, seated on the spine where it narrows
    for i in range(3):
        a = 186 + i * 7
        w = 1.55 * (1 - (a - 180) / 102.0) + 0.05
        o = r + w * 0.65
        q = [arc(cx, cy, o - 0.1, a), arc(cx, cy, o + 0.18, a), arc(cx, cy, o + 0.18, a + 3), arc(cx, cy, o - 0.1, a + 3)]
        P.append(prism("jimp%d" % i, SPINE, q, 0.3))
    return P


def katana():
    P = []
    # handle (tsuka): y 0.4 .. 7.4, black cloth wrap over the pale ray skin, a grey duct tape repair round the middle
    P.append(part("core", G, SKIN, (-0.5, 0.4, -0.38), (0.5, 7.4, 0.38)))
    for i in range(8):
        y0 = 0.5 + i * 0.86
        if 3.1 < y0 < 4.6:
            continue
        P.append(part("wrap%d" % i, G, WRAP, (-0.58, y0, -0.46), (0.58, y0 + 0.55, 0.46))
                 .inset("y", 0, x=(0.12, 0.12)).inset("y", 1, x=(0.12, 0.12)))
    P.append(part("tape", G, TAPE, (-0.62, 3.2, -0.5), (0.62, 4.6, 0.5)))
    P.append(part("tapeEnd", G, TAPE, (0.55, 3.3, -0.2), (0.75, 3.9, 0.15)).shift("y", 1, x=0.08))
    P += oct_y("kashira", G, IRON, 0, 0, 7.4, 7.85, 0.6)
    # guard (tsuba): a worn iron disc, the collar (habaki) in brass above it
    P += oct_y("tsuba", G, IRON, 0, 0, 0.05, 0.4, 1.75)
    P.append(part("habaki", G, BRASS, (-0.62, -1.0, -0.3), (0.62, 0.05, 0.3)).inset("y", 0, z=(0.05, 0.05)))
    # blade: 17 long, a gentle curve back toward the spine (-x) at the tip, the edge strip lighter (the hamon side),
    # the point (kissaki) cut at an angle
    L, seg = 17.0, 8
    def xc(t):
        return -1.1 * t * t                     # the curve: the tip sits 1.1 behind the line
    for i in range(seg):
        t0, t1 = i / float(seg), (i + 1) / float(seg)
        y0, y1 = -1.0 - L * t0, -1.0 - L * t1
        if i < seg - 1:
            q = [(xc(t0) - 0.5, y0), (xc(t0) + 0.28, y0), (xc(t1) + 0.28, y1), (xc(t1) - 0.5, y1)]
            P.append(prism("blade%d" % i, STEEL, q, [0.17, 0.12, 0.12, 0.17]))
            e = [(xc(t0) + 0.28, y0), (xc(t0) + 0.5, y0), (xc(t1) + 0.5, y1), (xc(t1) + 0.28, y1)]
            P.append(prism("edge%d" % i, EDGE, e, [0.12, 0.05, 0.05, 0.12]))
            s = [(xc(t0) - 0.58, y0), (xc(t0) - 0.5, y0), (xc(t1) - 0.5, y1), (xc(t1) - 0.58, y1)]
            P.append(prism("spine%d" % i, SPINE, s, 0.15))
        else:   # the point: the edge sweeps up into the spine
            q = [(xc(t0) - 0.58, y0), (xc(t0) + 0.5, y0), (xc(t1) - 0.3, y1), (xc(t1) - 0.58, y1 + 0.2)]
            P.append(prism("kissaki", EDGE, q, [0.15, 0.06, 0.03, 0.08]))
    # a bo-hi (groove) along the lower blade, darker
    for i in range(4):
        t0, t1 = i / float(seg), (i + 1) / float(seg)
        y0, y1 = -1.4 - L * t0, -1.0 - L * t1
        P.append(prism("hi%d" % i, SPINE, [(xc(t0) - 0.38, y0), (xc(t0) - 0.2, y0), (xc(t1) - 0.2, y1),
                                           (xc(t1) - 0.38, y1)], 0.19))
    return P


def blood(px, parts, uvs, seed, names, reach):
    """Dried blood in the texture of the parts named: splotches, more toward the tip (the part's place in `names`)."""
    rnd = random.Random(seed)
    from gunmodel import size, PX
    for i, p in enumerate(parts):
        if not any(p.name.startswith(n) for n in names):
            continue
        u, v = uvs[i]
        w, h, d = size(p)
        x0, y0, x1, y1 = u * PX, v * PX, (u + 2 * (d + w)) * PX, (v + d + h) * PX
        k = int(''.join(c for c in p.name if c.isdigit()) or 9)
        amount = min(1.0, reach * (k + 1))
        for _ in range(int(amount * (x1 - x0) * (y1 - y0) * 0.5)):
            cx, cy = rnd.randrange(x0, max(x0 + 1, x1)), rnd.randrange(y0, max(y0 + 1, y1))
            for dy in range(-1, 2):
                for dx in range(-1, 2):
                    yy, xx = cy + dy, cx + dx
                    if 0 <= yy < len(px) and 0 <= xx < len(px[0]) and px[yy][xx][3] and rnd.random() < 0.7:
                        r = rnd.choice(((92, 14, 14), (118, 20, 18), (70, 10, 10)))
                        px[yy][xx] = r + (255,)


def write(name, parts, blood_names, reach):
    from PIL import Image
    uvs, th = layout(parts)
    lines = ["  textureWidth = 512;", "  textureHeight = %d;" % th]
    counts = {}
    for i, p in enumerate(parts):
        k = counts.get(p.group, 0)
        counts[p.group] = k + 1
        lines += part_block("%s%d" % (p.group, k), p, uvs[i][0], uvs[i][1])
    text = "\n".join(lines) + "\n"
    mp = os.path.join(OUT, "models", "melee", name + ".bmodel")
    os.makedirs(os.path.dirname(mp), exist_ok=True)
    open(mp, "w").write(text)
    px = paint(parts, seed=hash(name) & 0xffff)
    blood(px, parts, uvs, 11, blood_names, reach)
    img = Image.new("RGBA", (len(px[0]), len(px)), (0, 0, 0, 0))
    for y, row in enumerate(px):
        for x, c in enumerate(row):
            if c[3]:
                img.putpixel((x, y), c)
    tp = os.path.join(OUT, "textures", "model", "melee", name + ".png")
    os.makedirs(os.path.dirname(tp), exist_ok=True)
    img.save(tp)
    icon(name, text, img, os.path.join(OUT, "textures", "items", name + ".png"))
    return {"name": name, "parts": len(parts), "texture": [512, th]}


def icon(name, text, img, path, size=32):
    """The side render, turned so the blade points to the top right like Minecraft's swords, 1 px dark outline."""
    import study
    from PIL import Image
    g = study.Gun(name, "melee", text, img, True)
    big = study.render(g, "side", scale=24, pad=0, bg=(0, 0, 0, 0)).rotate(-45, expand=True, resample=Image.BICUBIC)
    box = big.getbbox()
    big = big.crop(box)
    k = (size - 2) / float(max(big.width, big.height))
    small = big.resize((max(1, round(big.width * k)), max(1, round(big.height * k))), Image.BOX)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - small.width) // 2, (size - small.height) // 2
    for y in range(small.height):
        for x in range(small.width):
            r, gg, b, a = small.getpixel((x, y))
            if a >= 120:
                f = 255.0 / a * 1.2
                out.putpixel((ox + x, oy + y), (min(255, int(r * f)), min(255, int(gg * f)), min(255, int(b * f)), 255))
    edge = out.copy()
    for y in range(size):
        for x in range(size):
            if out.getpixel((x, y))[3] == 0 and any(0 <= x + dx < size and 0 <= y + dy < size and out.getpixel((x + dx, y + dy))[3]
                                                     for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.putpixel((x, y), (10, 10, 12, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    edge.save(path)


def sheet(path):
    import study
    from PIL import Image
    shots = []
    for n in ("karambit", "katana"):
        text = open(os.path.join(OUT, "models", "melee", n + ".bmodel")).read()
        img = Image.open(os.path.join(OUT, "textures", "model", "melee", n + ".png")).convert("RGBA")
        g = study.Gun(n, "melee", text, img, True)
        for v in ("side", "three", "top"):
            shots.append(study.render(g, v, scale=16 if n == "katana" else 30))
    w, h = sum(s.width for s in shots) + 10 * len(shots), max(s.height for s in shots)
    out = Image.new("RGBA", (w, h), (236, 233, 224, 255))
    x = 0
    for s in shots:
        out.paste(s, (x, 0))
        x += s.width + 10
    out.save(path)
    return out.size


def heat(name, parts, names, guard, length):
    """The fire's heat mask (MeleeFire, Fire Aspect): the texture's metal pixels (the parts named) with how far along the
    blade their part sits, guard 0 to tip 255, in red, 255 alpha; everything else clear. Only this file is written, the
    model and texture stay as they are."""
    from PIL import Image
    from gunmodel import size, PX
    uvs, th = layout(parts)
    tex = Image.open(os.path.join(OUT, "textures", "model", "melee", name + ".png")).convert("RGBA")
    img = Image.new("RGBA", tex.size, (0, 0, 0, 0))
    for i, p in enumerate(parts):
        if not any(p.name.startswith(n) for n in names):
            continue
        ys = [c[1] for c in p.points()]
        s = max(0.0, min(1.0, (guard - (min(ys) + max(ys)) / 2) / length))
        u, v = uvs[i]
        w, h, d = size(p)
        for y in range(int(v * PX), int((v + d + h) * PX)):
            for x in range(int(u * PX), int((u + 2 * (d + w)) * PX)):
                if x < img.width and y < img.height and tex.getpixel((x, y))[3]:
                    img.putpixel((x, y), (int(s * 255), 255, 255, 255))
    path = os.path.join(OUT, "textures", "model", "melee", name + "_heat.png")
    img.save(path)
    return path


if __name__ == "__main__":
    if "--heat" in sys.argv:
        print(heat("katana", katana(), ("blade", "edge", "spine", "kissaki", "hi"), -1.0, 17.0))
        sys.exit(0)
    print(write("karambit", karambit(), ("blade", "edge"), 0.05))
    print(write("katana", katana(), ("blade", "edge", "kissaki"), 0.025))
    if "--sheet" in sys.argv:
        print(sheet("/tmp/melee_sheet.png"))
