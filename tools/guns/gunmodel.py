#!/usr/bin/env python3
"""Build a Decimation gun model from a box spec (our own art, nothing copied).

A spec module (tools/guns/<gun>.py) lists boxes in Decimation model units
(1 unit = 1/16 block; x forward to the muzzle, y DOWN, z sideways), each with
a part group (gunModel / ammoModel / slideModel, docs/gun_model_spec.md),
a colour and an optional face style, plus the header (flamePos, ejectPos,
hand poses). build() writes:

- the .bmodel (Techne / Toolbox style text read by BModelLoader): one part
  per box, `addShape(0F,0F,0F, <8 zero corners>, w, h, d)` at
  `setRotationPoint(x0, y0, z0)` (sizes may be decimals: the loader parses
  doubles; never addBox, docs/gun_model_spec.md);
- the model texture: box UV laid out as BModelBox maps it (top / bottom on
  the first row, then the 4 sides; float sizes), painted procedurally at
  SCALE pixels per unit (top faces lighter, bottom darker, edge highlight,
  noise); textureWidth / Height in units in the header;
- a .bbmodel for previews with the headless Blockbench MCP (tools/bbmcp.py,
  y flipped to Blockbench's y up) with the same texture.
"""
import json
import math
import os
import random
import struct
import zlib

SCALE = 4  # texture pixels per model unit (Decimation's own guns use 2)


class Box:
    def __init__(self, name, group, a, b, colour, style=None):
        self.name, self.group = name, group
        self.x0, self.y0, self.z0 = [min(p, q) for p, q in zip(a, b)]
        self.x1, self.y1, self.z1 = [max(p, q) for p, q in zip(a, b)]
        self.colour, self.style = colour, style or {}
        self.u = self.v = 0

    @property
    def w(self):
        return round(self.x1 - self.x0, 4)

    @property
    def h(self):
        return round(self.y1 - self.y0, 4)

    @property
    def d(self):
        return round(self.z1 - self.z0, 4)


def layout(boxes, width=128):
    """Shelf packs each box's UV footprint (2(d+w) x (d+h) units) into a texture `width` units wide."""
    x = y = row = 0
    for b in sorted(boxes, key=lambda b: -(b.d + b.h)):
        fw, fh = math.ceil(2 * (b.d + b.w)) + 1, math.ceil(b.d + b.h) + 1
        if x + fw > width:
            x, y, row = 0, y + row, 0
        b.u, b.v = x, y
        x += fw
        row = max(row, fh)
    height = 1
    while height < y + row:
        height *= 2
    return width, height


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb)


def paint(boxes, tw, th, seed=7):
    """RGBA pixels: every box's 6 faces at Decimation's box UV places."""
    rnd = random.Random(seed)
    W, H = tw * SCALE, th * SCALE
    px = [[(0, 0, 0, 0)] * W for _ in range(H)]

    def rect(u0, v0, u1, v1, rgb, face, b):
        a0, b0 = int(round(u0 * SCALE)), int(round(v0 * SCALE))
        a1, b1 = max(a0 + 1, int(round(u1 * SCALE))), max(b0 + 1, int(round(v1 * SCALE)))
        for yy in range(b0, min(b1, H)):
            for xx in range(a0, min(a1, W)):
                f = 1 + rnd.uniform(-0.05, 0.05)
                edge = xx in (a0, a1 - 1) or yy in (b0, b1 - 1)
                if edge and (a1 - a0) > 2 and (b1 - b0) > 2:
                    f *= 1.18  # worn edges catch light
                st = b.style.get(face) or b.style.get("all")
                if st == "grip" and (yy - b0) % 2 == 0:
                    f *= 0.78  # checkered grip
                if st == "rings" and (xx - a0) % 3 == 0:
                    f *= 1.35  # threads / cooling rings
                if st == "port" and (b1 - b0) > 4 and b0 + 1 < yy < b1 - 2 \
                        and a0 + (a1 - a0) * 0.45 < xx < a0 + (a1 - a0) * 0.68:
                    f *= 0.45  # ejection port: a window in the middle of the side
                c = shade(rgb, f)
                px[yy][xx] = (c[0], c[1], c[2], 255)

    for b in boxes:
        u, v, w, h, d = b.u, b.v, b.w, b.h, b.d
        c = b.colour
        rect(u + d, v, u + d + w, v + d, shade(c, 1.30), "top", b)            # y = 0 face (top: y is down)
        rect(u + d + w, v, u + d + w + w, v + d, shade(c, 0.70), "bottom", b)  # y = h face
        rect(u, v + d, u + d, v + d + h, shade(c, 0.95), "back", b)            # x = 0 end
        rect(u + d, v + d, u + d + w, v + d + h, shade(c, 1.0), "side", b)    # z = 0 side
        rect(u + d + w, v + d, u + d + w + d, v + d + h, shade(c, 1.05), "front", b)  # x = w end
        rect(u + d + w + d, v + d, u + d + w + d + w, v + d + h, shade(c, 0.92), "side2", b)  # z = d side
    return px


def icon(boxes, size=32, pad=1):
    """Inventory icon: the side view (z = 0 faces) of every box, scaled to fit, outlined."""
    x0 = min(b.x0 for b in boxes); x1 = max(b.x1 for b in boxes)
    y0 = min(b.y0 for b in boxes); y1 = max(b.y1 for b in boxes)
    k = (size - 2 * pad) / max(x1 - x0, y1 - y0)
    ox = pad + ((size - 2 * pad) - (x1 - x0) * k) / 2
    oy = pad + ((size - 2 * pad) - (y1 - y0) * k) / 2
    px = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for b in sorted(boxes, key=lambda b: b.z1):  # nearer boxes (bigger z) drawn last
        c = shade(b.colour, 1.25)
        for yy in range(int(oy + (b.y0 - y0) * k), int(math.ceil(oy + (b.y1 - y0) * k))):
            for xx in range(int(ox + (b.x0 - x0) * k), int(math.ceil(ox + (b.x1 - x0) * k))):
                if 0 <= xx < size and 0 <= yy < size:
                    top = yy == int(oy + (b.y0 - y0) * k)
                    px[yy][xx] = shade(c, 1.25 if top else 1.0) + (255,)
    out = [row[:] for row in px]
    for yy in range(size):  # dark outline around the shape, as Minecraft item icons have
        for xx in range(size):
            if px[yy][xx][3] == 0 and any(0 <= yy + dy < size and 0 <= xx + dx < size and px[yy + dy][xx + dx][3]
                                           for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[yy][xx] = (12, 12, 14, 255)
    return out


def write_png(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in px)
    chunk = lambda t, data: struct.pack(">I", len(data)) + t + data + struct.pack(">I", zlib.crc32(t + data) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def fnum(v):
    return ("%g" % v) + "F"


def bmodel(spec, boxes, tw, th):
    lines = []
    for k in ("flamePos", "ejectPos", "rhPos", "rhRot", "lhPos", "lhRot", "sPos"):
        if k in spec:
            lines.append("  %s: %s;" % (k, ", ".join("%g" % c for c in spec[k])))
    lines += ["  textureWidth = %d;" % tw, "  textureHeight = %d;" % th]
    counters = {}
    for b in boxes:
        i = counters.get(b.group, 0)
        counters[b.group] = i + 1
        n = "%s%d" % (b.group, i)
        zero = "new float[][]{" + ", ".join(["{0F, 0F, 0F}"] * 8) + "}"
        lines.append("  %s = new ModelRenderer(this, %d, %d);" % (n, b.u, b.v))
        lines.append("  %s.addShape(0F,0F,0F, %s, %s, %s, %s);" % (n, zero, fnum(b.w), fnum(b.h), fnum(b.d)))
        lines.append("  %s.setRotationPoint(%s, %s, %s);" % (n, fnum(b.x0), fnum(b.y0), fnum(b.z0)))
    return "\n".join(lines) + "\n"


def bb_ops(boxes):
    """bbmodel_edit operations: one cube per box, Blockbench y up (y -> -y), box UV at our layout."""
    ops = []
    for g in sorted({b.group for b in boxes}):
        ops.append({"op": "add_group", "name": g})
    for b in boxes:
        ops.append({"op": "add_cube", "name": b.name, "parent": b.group,
                    "from": [b.x0, -b.y1, b.z0], "to": [b.x1, -b.y0, b.z1],
                    "box_uv": True, "uv_offset": [b.u, b.v]})
    return ops


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
        for part, (pos, rot) in k.get("parts", {}).items():
            out += [" %s {" % part, "  Pos: %g, %g, %g;" % tuple(pos), "  Rot: %g, %g, %g;" % tuple(rot), " }"]
        out.append("}")
    out.append("END")
    return "\n".join(out) + "\n"


def build(spec, out_root, bb_root):
    boxes = [Box(*x) for x in spec["boxes"]]
    tw, th = layout(boxes, spec.get("textureWidth", 128))
    name, cat = spec["name"], spec["category"]
    model = os.path.join(out_root, "models", "guns", cat, name + ".bmodel")
    os.makedirs(os.path.dirname(model), exist_ok=True)
    with open(model, "w") as f:
        f.write(bmodel(spec, boxes, tw, th))
    tex = os.path.join(out_root, "textures", "model", "guns", cat, name, name + ".png")
    write_png(tex, paint(boxes, tw, th))
    write_png(os.path.join(out_root, "textures", "items", "gun", cat, name + ".png"), icon(boxes))
    for anim, text in spec.get("animations", {}).items():
        path = os.path.join(out_root, "animations", name, name + anim + ".anib")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            f.write(text)
    os.makedirs(bb_root, exist_ok=True)
    write_png(os.path.join(bb_root, name + "_tex.png"), paint(boxes, tw, th))
    with open(os.path.join(bb_root, name + "_ops.json"), "w") as f:
        json.dump(bb_ops(boxes), f)
    return {"boxes": len(boxes), "texture": [tw, th], "model": model, "tex": tex}
