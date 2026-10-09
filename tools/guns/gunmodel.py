#!/usr/bin/env python3
"""Build a Decimation gun from a part spec, in Decimation's own style (our own art,
nothing copied). Rules and numbers: docs/gun_style_guide.md; formats:
docs/gun_model_spec.md; workflow: .claude/skills/decimation-gun.

A spec module (tools/guns/<gun>.py) makes parts with the helpers below, in
Decimation model units (1 unit = 1/16 block; x forward to the muzzle, y DOWN,
z sideways, guns centred on z -0.15). Every part is a hexahedron: 8 corners,
start from a box and bend it:

    p = part("upper", G, STEEL, (2, -4.45, -0.75), (9.4, -3.2, 0.45))
    p.inset("y", 0, z=(0.12, 0.12))          # top face narrower: bevelled long edges (a taper)
    p.inset("x", 1, y=(0.2, 0), z=(0.1, 0.1))  # front end smaller
    p.shift("y", 1, x=0.3)                   # bottom face moved forward (a skew)
    q = p.mirror()                           # the same part on the other side of z -0.15

build() writes, into dev/src/main/resources/assets/deci/:
- models/guns/<cat>/<gun>.bmodel: one part per shape, declared at its rounded
  size (at least 1, so long parts are 1x1xN like Decimation's) with the rest
  of the shape in the 8 corner offsets, textureWidth 512, UV islands shelf
  packed in steps of 8;
- textures/model/guns/<cat>/<gun>/<gun>.png: one tone per part with gradation
  (part shade, height gradient, face shifts, drift along long faces, texel
  noise), 2 pixels per unit (PNG 1024 wide);
- textures/items/gun/<cat>/<gun>.png: 32x32 icon from the model's side render
  (tools/guns/study.py), muzzle right, 1 px dark outline;
- animations/<gun>/<gun><Name>.anib from anib() keyframes.
Preview and compare with tools/guns/study.py (render / sheet / --split / attach).
"""
import os
import random
import struct
import zlib

ZC = -0.15          # Decimation's centre line in z
TEX_W = 512         # textureWidth (units)
STEP = 8            # UV island step (units)
PX = 2              # PNG pixels per unit
# BModelBox corner array index for each box corner (ix, iy, iz) (docs/gun_model_spec.md section 6)
INDEX = {(0, 0, 0): 7, (1, 0, 0): 6, (1, 1, 0): 4, (0, 1, 0): 5,
         (0, 0, 1): 3, (1, 0, 1): 2, (1, 1, 1): 0, (0, 1, 1): 1}
AXES = {"x": 0, "y": 1, "z": 2}


class Part:
    """A hexahedron: corner (ix, iy, iz) in {0, 1}^3 -> absolute model point."""

    def __init__(self, name, group, colour, a, b):
        lo = [min(p, q) for p, q in zip(a, b)]
        hi = [max(p, q) for p, q in zip(a, b)]
        self.name, self.group, self.colour = name, group, colour
        self.c = {k: [hi[i] if k[i] else lo[i] for i in range(3)] for k in INDEX}

    def inset(self, axis, side, **amounts):
        """Shrink the face at `side` (0 = low end, 1 = high end) of `axis`: amounts per other axis
        as (from the low side, from the high side). A taper (one end smaller) or, with a zero
        width left, a wedge."""
        a = AXES[axis]
        for k, p in self.c.items():
            if k[a] != side:
                continue
            for other, (lo, hi) in amounts.items():
                o = AXES[other]
                p[o] += lo if k[o] == 0 else -hi
        return self

    def shift(self, axis, side, **delta):
        """Move the face at `side` of `axis` (a skew / shear)."""
        a = AXES[axis]
        for k, p in self.c.items():
            if k[a] == side:
                for other, d in delta.items():
                    p[AXES[other]] += d
        return self

    def mirror(self, name=None):
        """The same part mirrored about z = ZC (left / right pairs)."""
        m = Part(name or self.name + "R", self.group, self.colour, (0, 0, 0), (1, 1, 1))
        for (ix, iy, iz), p in self.c.items():
            q = self.c[(ix, iy, 1 - iz)]
            m.c[(ix, iy, iz)] = [q[0], q[1], 2 * ZC - q[2]]
        return m

    def points(self):
        return list(self.c.values())


def part(name, group, colour, a, b):
    return Part(name, group, colour, a, b)


def octagon(name, group, colour, x0, x1, cy, cz, r, flat=0.42):
    """A round bar along x as Decimation builds it: a middle slab plus a trapezoid above and
    below (3 parts, an 8 sided section of half width r)."""
    k = r * flat
    mid = part(name + "Mid", group, colour, (x0, cy - k, cz - r), (x1, cy + k, cz + r))
    top = part(name + "Top", group, colour, (x0, cy - r, cz - r), (x1, cy - k, cz + r)).inset("y", 0, z=(r - k, r - k))
    bot = part(name + "Bot", group, colour, (x0, cy + k, cz - r), (x1, cy + r, cz + r)).inset("y", 1, z=(r - k, r - k))
    return [mid, top, bot]


def slide_names(parts):
    """Bmodel names of the slideModel parts, in order (animations target every one)."""
    return ["slideModel%d" % i for i in range(sum(1 for p in parts if p.group == "slideModel"))]


# ---------------------------------------------------------------- output

def fnum(v):
    v = round(v, 4)
    return ("%g" % (0 if v == 0 else v)) + "F"


def numbered(parts):
    counters, out = {}, []
    for p in parts:
        i = counters.get(p.group, 0)
        counters[p.group] = i + 1
        out.append(("%s%d" % (p.group, i), p))
    return out


def size(p):
    """Declared box size: each real extent rounded, at least 1 (Decimation declares long parts
    1x1xN, so their faces get N texels and gradation along them; the rest of the shape goes into
    the corner offsets, median 0.35 like Decimation's SMGs)."""
    pts = p.points()
    return [max(1, int(round(max(q[a] for q in pts) - min(q[a] for q in pts)))) for a in range(3)]


def layout(parts):
    """Box UV islands (2(d+w) x (d+h) units) shelf packed in a textureWidth 512 sheet, cells in
    steps of 8 units like Decimation's; returns [(u, v)] and the texture height (power of 2)."""
    out, x, y, row = [], 0, 0, 0
    for p in parts:
        w, h, d = size(p)
        cw = -(-(2 * (d + w) + 1) // STEP) * STEP
        ch = -(-(d + h + 1) // STEP) * STEP
        if x + cw > TEX_W:
            x, y, row = 0, y + row, 0
        out.append((x + 1, y + 1))
        x += cw
        row = max(row, ch)
    hgt = 16
    while hgt < y + row:
        hgt *= 2
    return out, hgt


def bmodel(spec, parts):
    lines = []
    for k in ("flamePos", "ejectPos", "rhPos", "rhRot", "lhPos", "lhRot"):
        if k in spec:
            lines.append("  %s: %s;" % (k, ", ".join("%g" % c for c in spec[k])))
    uvs, th = layout(parts)
    lines += ["  textureWidth = %d;" % TEX_W, "  textureHeight = %d;" % th]
    for i, (n, p) in enumerate(numbered(parts)):
        pts = p.points()
        piv = [min(q[a] for q in pts) for a in range(3)]
        s = size(p)
        offs = [None] * 8
        for k, q in p.c.items():
            offs[INDEX[k]] = [q[a] - piv[a] - k[a] * s[a] for a in range(3)]
        u, v = uvs[i]
        corners = ", ".join("{%s}" % ", ".join(fnum(c) for c in o) for o in offs)
        lines += ["",
                  "  // %s" % p.name,
                  "  %s = new BeardieModelRenderer(this, %d, %d);" % (n, u, v),
                  "  %s.addShape(0F,0F,0F, new float[][]{%s}, %d, %d, %d);" % (n, corners, s[0], s[1], s[2]),
                  "  %s.setRotationPoint(%s, %s, %s);" % (n, fnum(piv[0]), fnum(piv[1]), fnum(piv[2])),
                  "  %s.setRotation(0F, 0F, 0F);" % n]
    return "\n".join(lines) + "\n"


def paint(parts, seed=7):
    """RGBA rows: each part's box UV island, toned like Decimation's guns
    (docs/gun_style_guide.md section 4, measured on Uzi / AK74 / MP5A3: 15 to 31 distinct part
    tones a gun, faces of one part about 5 apart, texel noise about 2.5, top of the gun a bit
    lighter than the bottom):
    - every part its own shade of its material (+-6 %, seeded per part),
    - a gentle gradient over the gun's height (+5 at the top, -5 at the bottom),
    - per face: top +2, bottom -2, the four sides -1.5..+1.5,
    - along a long face a slow drift (+-2 over its length) and per texel noise -4..+4."""
    rnd = random.Random(seed)
    uvs, th = layout(parts)
    W, H = TEX_W * PX, th * PX
    px = [[(0, 0, 0, 0)] * W for _ in range(H)]
    ys = [q[1] for p in parts for q in p.points()]
    y0, y1 = min(ys), max(ys)
    for i, p in enumerate(parts):
        u, v = uvs[i]
        w, h, d = size(p)
        f = 1 + rnd.uniform(-0.06, 0.06)
        cy = sum(q[1] for q in p.points()) / 8
        grad = 5 - 10 * (cy - y0) / max(1e-6, y1 - y0)
        base = [c * f + grad for c in p.colour]
        # box UV (vanilla ModelBox): row v: top (u+d, w x d), bottom (u+d+w, w x d);
        # row v+d: x0 side (u, d x h), z0 side (u+d, w x h), x1 side (u+d+w, d x h), z1 side (u+2d+w, w x h)
        faces = [(u + d, v, w, d, 2), (u + d + w, v, w, d, -2)]
        for (fu, fw) in ((u, d), (u + d, w), (u + d + w, d), (u + 2 * d + w, w)):
            faces.append((fu, v + d, fw, h, rnd.uniform(-1.5, 1.5)))
        for fu, fv, fw, fh, shift in faces:
            drift = rnd.uniform(-2, 2)
            n_px = max(1, fw * PX)
            for yy in range(fv * PX, (fv + fh) * PX):
                for xx in range(fu * PX, (fu + fw) * PX):
                    along = drift * ((xx - fu * PX) / n_px * 2 - 1)
                    n = rnd.randint(-4, 4)
                    px[yy][xx] = tuple(max(0, min(255, int(round(c + shift + along + n)))) for c in base) + (255,)
    return px


def write_png(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in px)
    chunk = lambda t, data: struct.pack(">I", len(data)) + t + data + struct.pack(">I", zlib.crc32(t + data) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def icon(name, path, size=32, width=30):
    """The side render of the built model (study.py), muzzle right, scaled to `width` px,
    hard alpha, 1 px dark outline, lifted a little so it reads on the inventory slot."""
    import study
    from PIL import Image
    g = study.load("ours:" + name)
    img = study.render(g, "other", scale=24, pad=0, bg=(0, 0, 0, 0)).transpose(Image.FLIP_LEFT_RIGHT)
    # "other" sees the -z side (the right side, where the ejection port is); flipped: muzzle right
    k = width / img.width
    small = img.resize((width, max(1, round(img.height * k))), Image.BOX)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - small.width) // 2, (size - small.height) // 2
    for y in range(small.height):
        for x in range(small.width):
            r, gg, b, a = small.getpixel((x, y))
            if a >= 140:  # solid pixels only, colours un-premultiplied (no halo from the background)
                f = 255.0 / a * 1.3
                out.putpixel((ox + x, oy + y), (min(255, int(r * f)), min(255, int(gg * f)), min(255, int(b * f)), 255))
    edge = out.copy()
    for y in range(size):
        for x in range(size):
            if out.getpixel((x, y))[3] == 0 and any(0 <= x + dx < size and 0 <= y + dy < size and out.getpixel((x + dx, y + dy))[3]
                                                     for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.putpixel((x, y), (10, 10, 12, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    edge.save(path)


def anib(length, hand, keys, static=False):
    """An .anib animation (docs/gun_model_spec.md section 3) from keyframes:
    keys = {frame: {"kind": "" | "RAND" | "SWITCH" | "LOAD" | "TRYBOLT", "sound": "MagOut",
    "shake": 1.0, "parts": {"Model" | "OffHand" | part: ((px, py, pz), (rx, ry, rz))}}};
    the frames between keyframes are SKIP (interpolated)."""
    out = ["Length: %d" % length, "Hand: %d" % hand] + (["STATIC"] if static else []) + ["-" * 27, "START"]
    for i in range(length):
        k = keys.get(i)
        if k is None:
            out.append("Frame SKIP")
            continue
        out.append(((k.get("kind", "") + " ") if k.get("kind") else "") + "Frame {")
        if "shake" in k:
            out.append("Shake: %g;" % k["shake"])
        if "sound" in k:
            out.append("PlaySound: %s;" % k["sound"])
        for name, (pos, rot) in k.get("parts", {}).items():
            out += [" %s {" % name, "  Pos: %g, %g, %g;" % tuple(pos), "  Rot: %g, %g, %g;" % tuple(rot), " }"]
        out.append("}")
    out.append("END")
    return "\n".join(out) + "\n"


def build(spec, out_root):
    parts = spec["parts"]
    name, cat = spec["name"], spec["category"]
    model = os.path.join(out_root, "models", "guns", cat, name + ".bmodel")
    os.makedirs(os.path.dirname(model), exist_ok=True)
    with open(model, "w") as f:
        f.write(bmodel(spec, parts))
    tex = os.path.join(out_root, "textures", "model", "guns", cat, name, name + ".png")
    write_png(tex, paint(parts))
    for anim, text in spec.get("animations", {}).items():
        path = os.path.join(out_root, "animations", name, name + anim + ".anib")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            f.write(text)
    icon(name, os.path.join(out_root, "textures", "items", "gun", cat, name + ".png"))
    groups = {}
    for p in parts:
        groups[p.group] = groups.get(p.group, 0) + 1
    return {"parts": len(parts), "groups": groups, "texture": [TEX_W, layout(parts)[1]], "model": model}
