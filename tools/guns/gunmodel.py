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
- models/guns/<cat>/<gun>.bmodel: one part per shape, declared 1x1x1 with the
  real shape in the 8 corner offsets (as 99 % of Decimation's parts),
  textureWidth 512, UV islands stepping by 8;
- textures/model/guns/<cat>/<gun>/<gun>.png: one flat tone per part island with
  faint per texel noise, 2 pixels per unit (PNG 1024 wide);
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


def tex_height(n):
    rows = (n + TEX_W // STEP - 1) // (TEX_W // STEP)
    h = 16
    while h < rows * STEP:
        h *= 2
    return h


def uv(i):
    per_row = TEX_W // STEP
    return (i % per_row) * STEP + 1, (i // per_row) * STEP + 1


def bmodel(spec, parts):
    lines = []
    for k in ("flamePos", "ejectPos", "rhPos", "rhRot", "lhPos", "lhRot"):
        if k in spec:
            lines.append("  %s: %s;" % (k, ", ".join("%g" % c for c in spec[k])))
    lines += ["  textureWidth = %d;" % TEX_W, "  textureHeight = %d;" % tex_height(len(parts))]
    for i, (n, p) in enumerate(numbered(parts)):
        pts = p.points()
        piv = [min(q[a] for q in pts) for a in range(3)]
        offs = [None] * 8
        for k, q in p.c.items():
            offs[INDEX[k]] = [q[a] - piv[a] - k[a] for a in range(3)]
        u, v = uv(i)
        corners = ", ".join("{%s}" % ", ".join(fnum(c) for c in o) for o in offs)
        lines += ["",
                  "  // %s" % p.name,
                  "  %s = new BeardieModelRenderer(this, %d, %d);" % (n, u, v),
                  "  %s.addShape(0F,0F,0F, new float[][]{%s}, 1, 1, 1);" % (n, corners),
                  "  %s.setRotationPoint(%s, %s, %s);" % (n, fnum(piv[0]), fnum(piv[1]), fnum(piv[2])),
                  "  %s.setRotation(0F, 0F, 0F);" % n]
    return "\n".join(lines) + "\n"


def paint(parts, seed=7):
    """RGBA rows: each part's 1x1x1 island (4 x 2 units) one tone with faint per texel noise."""
    rnd = random.Random(seed)
    W, H = TEX_W * PX, tex_height(len(parts)) * PX
    px = [[(0, 0, 0, 0)] * W for _ in range(H)]
    for i, p in enumerate(parts):
        u, v = uv(i)
        for yy in range(v * PX, (v + 2) * PX):
            for xx in range(u * PX, (u + 4) * PX):
                n = rnd.randint(-3, 3)
                px[yy][xx] = tuple(max(0, min(255, c + n)) for c in p.colour) + (255,)
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
    return {"parts": len(parts), "groups": groups, "texture": [TEX_W, tex_height(len(parts))], "model": model}
