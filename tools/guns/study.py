#!/usr/bin/env python3
"""Study gun models: render them with their textures and measure them
(docs/gun_style_guide.md). Reads Decimation's guns straight from
Decimation.jar and ours from dev/src/main/resources; nothing of Decimation
is copied into the repo (renders go to docs/shots/, git ignored).

    python3 tools/guns/study.py render NAME [NAME ...] [--out DIR] [--scale PX] [--split]
    python3 tools/guns/study.py sheet NAME [NAME ...] --out FILE.png [--cols N]  # side views
    python3 tools/guns/study.py stats [--tsv FILE]                       # every gun, one line each
    python3 tools/guns/study.py parts NAME                               # part list of one gun
    python3 tools/guns/study.py attach GUN ATTACHMENT [...]               # gun + attachments as placed in game
    python3 tools/guns/study.py gaps ours:NAME                           # ours vs its category, metric by metric
    python3 tools/guns/study.py vocab [NAME ...]                         # taper / cuboid / skew / wedge shares

NAME is a gun file name (uzi, m4a1, mac10); "ours:NAME" forces our own copy.
Views: side (muzzle right, the side facing +z), other (the other side), top,
three (three quarter from the front right, above). Renderer: every part's
8 corners (BModelBox order, offsets added, docs/gun_model_spec.md section 6
"Facts"), rotation Z then Y then X about the rotation point like
ModelRenderer (children of addChild inside their parent), faces textured by vanilla ModelBox's box UV, drawn back to
front (painter's order), simple light per face, 2x supersampled.
"""
import math
import os
import re
import sys
import zipfile
from io import BytesIO

from PIL import Image, ImageDraw

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
JAR = os.path.join(ROOT, "Decimation.jar")
OURS = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deci")

NUM = r"-?[\d.]+(?:[eE]-?\d+)?F?"


def num(s):
    return float(s.rstrip("Ff"))


class Part:
    def __init__(self, name, u, v):
        self.name, self.u, self.v = name, u, v
        self.off = (0.0, 0.0, 0.0)
        self.corners = [(0.0, 0.0, 0.0)] * 8
        self.size = (0, 0, 0)
        self.pivot = (0.0, 0.0, 0.0)
        self.rot = (0.0, 0.0, 0.0)
        self.parent = None
        self.tex = None  # (image, textureWidth, textureHeight) when not the gun's own

    @property
    def group(self):
        return re.sub(r"\d+$", "", self.name)

    def shaped(self):
        return any(abs(c) > 1e-6 for p in self.corners for c in p)

    def verts(self):
        """The 8 corners in model space, indexed like vanilla ModelBox: v7 (0,0,0), v0 (w,0,0),
        v1 (w,h,0), v2 (0,h,0), v3 (0,0,d), v4 (w,0,d), v5 (w,h,d), v6 (0,h,d)."""
        w, h, d = self.size
        ox, oy, oz = self.off
        # BModelBox corner array index -> box corner
        base = {7: (0, 0, 0), 6: (w, 0, 0), 4: (w, h, 0), 5: (0, h, 0),
                3: (0, 0, d), 2: (w, 0, d), 0: (w, h, d), 1: (0, h, d)}
        corner = {}
        for i, (bx, by, bz) in base.items():
            cx, cy, cz = self.corners[i]
            corner[(bx, by, bz)] = (ox + bx + cx, oy + by + cy, oz + bz + cz)
        named = [corner[(w, 0, 0)], corner[(w, h, 0)], corner[(0, h, 0)], corner[(0, 0, d)],
                 corner[(w, 0, d)], corner[(w, h, d)], corner[(0, h, d)], corner[(0, 0, 0)]]
        return [self.place(p) for p in named]

    def place(self, p):
        """A point in this part's space to model space (through the parents of addChild)."""
        rx, ry, rz = self.rot
        p = rot_z(rot_y(rot_x(p, rx), ry), rz)
        p = (p[0] + self.pivot[0], p[1] + self.pivot[1], p[2] + self.pivot[2])
        return self.parent.place(p) if self.parent else p

    def quads(self):
        """(4 vertices, (u1, v1, u2, v2)) per face, like vanilla ModelBox."""
        v = self.verts()
        w, h, d = self.size
        u, t = self.u, self.v
        return [
            ((v[4], v[0], v[1], v[5]), (u + d + w, t + d, u + d + w + d, t + d + h)),
            ((v[7], v[3], v[6], v[2]), (u, t + d, u + d, t + d + h)),
            ((v[4], v[3], v[7], v[0]), (u + d, t, u + d + w, t + d)),
            ((v[1], v[2], v[6], v[5]), (u + d + w, t + d, u + d + w + w, t)),
            ((v[0], v[7], v[2], v[1]), (u + d, t + d, u + d + w, t + d + h)),
            ((v[3], v[4], v[5], v[6]), (u + d + w + d, t + d, u + d + w + d + w, t + d + h)),
        ]


def rot_x(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def rot_y(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def rot_z(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2])


class Gun:
    def __init__(self, name, cat, text, texture, ours):
        self.name, self.cat, self.ours = name, cat, ours
        self.header = {}
        self.tw, self.th = 64, 32
        self.parts = []
        self.texture = texture
        self.parse(text)

    def parse(self, text):
        by = {}
        for line in text.splitlines():
            s = line.strip()
            m = re.match(r"(\w+):\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*;", s)
            if m:
                self.header[m.group(1)] = tuple(num(m.group(i)) for i in (2, 3, 4))
                continue
            m = re.match(r"texture(Width|Height)\s*=\s*(\d+)", s)
            if m:
                if m.group(1) == "Width":
                    self.tw = int(m.group(2))
                else:
                    self.th = int(m.group(2))
                continue
            m = re.match(r"(\w+)\s*=\s*new\s+\w+\(this,\s*(\d+),\s*(\d+)\)", s)
            if m:
                p = Part(m.group(1), int(m.group(2)), int(m.group(3)))
                by[p.name] = p
                self.parts.append(p)
                continue
            m = re.match(r"(\w+)\.addChild\((\w+)\);", s)
            if m and m.group(1) in by and m.group(2) in by:
                by[m.group(2)].parent = by[m.group(1)]
                continue
            m = re.match(r"(\w+)\.(addShape|setRotationPoint|setRotation)\((.*)\);", s)
            if not m or m.group(1) not in by:
                continue
            p, args = by[m.group(1)], m.group(3)
            nums = [num(x) for x in re.findall(NUM, args.replace("new float[][]", ""))]
            if m.group(2) == "addShape":
                p.off = tuple(nums[0:3])
                p.corners = [tuple(nums[3 + 3 * i:6 + 3 * i]) for i in range(8)]
                p.size = tuple(int(round(x)) if abs(x - round(x)) < 1e-6 else x for x in nums[27:30])
            elif m.group(2) == "setRotationPoint":
                p.pivot = tuple(nums[0:3])
            else:
                p.rot = tuple(nums[0:3])

    def bounds(self, groups=None):
        pts = [q for p in self.parts if groups is None or p.group in groups for q in p.verts()]
        return [(min(a[i] for a in pts), max(a[i] for a in pts)) for i in range(3)]


def load(name):
    """Our gun if NAME starts with ours: or only exists there, else Decimation's."""
    ours = name.startswith("ours:")
    name = name.split(":")[-1]
    if not ours:
        z = zipfile.ZipFile(JAR)
        hit = [i for i in z.namelist() if re.match(r"assets/deci/models/guns/\w+/%s\.bmodel$" % re.escape(name), i)]
        if hit:
            cat = hit[0].split("/")[4]
            tex = "assets/deci/textures/model/guns/%s/%s/%s.png" % (cat, name, name)
            img = Image.open(BytesIO(z.read(tex))).convert("RGBA") if tex in z.namelist() else None
            return Gun(name, cat, z.read(hit[0]).decode("latin1"), img, False)
    for cat in os.listdir(os.path.join(OURS, "models", "guns")):
        path = os.path.join(OURS, "models", "guns", cat, name + ".bmodel")
        if os.path.isfile(path):
            tex = os.path.join(OURS, "textures", "model", "guns", cat, name, name + ".png")
            img = Image.open(tex).convert("RGBA") if os.path.isfile(tex) else None
            return Gun(name, cat, open(path).read(), img, True)
    raise SystemExit("no gun " + name)


ATTACH_SLOT = {"reddot": "sight", "2x": "sight", "4x": "sight", "8x": "sight", "dragunovScope": "sight",
               "foregrip": "grip", "flashlight": "grip", "laser": "grip", "bayonet": "barrel",
               "pistolSuppressor": "barrel", "smgSuppressor": "barrel", "arSuppressor": "barrel",
               "shotgunSuppressor": "barrel", "mgSuppressor": "barrel"}


def attach_offset(gun, name):
    """Where GunItemRenderer.renderAttachments draws an attachment, in model units (GL / 0.0625),
    in the gun's own model space (same matrix as the gun's parts)."""
    slot = ATTACH_SLOT[name]
    if slot == "sight":
        o = [0.05, 0.07, -0.008]
        if name == "dragunovScope":
            o = [o[0] - 0.1, o[1] - 0.05, o[2] - 0.004]
        elif name == "reddot":
            o = [o[0] - 0.362, o[1] + 0.0065, o[2] - 0.002]
    elif slot == "grip":
        o = [-0.2, 0.05, 0.0]
    else:
        f = gun.header.get("flamePos", (20, -4, 0))
        o = [-1.55 + f[0] / 21, 0.27 + f[1] / 21, -0.003 + f[2] / 21]
        if gun.name == "mp7":
            o = [o[0] - 0.1, o[1] + 0.03, o[2]]
        if name == "bayonet":
            o = [o[0] + 1.8, o[1] - 0.23, o[2] - 0.06]
    return tuple(c / 0.0625 for c in o)


# our per gun attachment corrections (fixes/NewGuns: Deci.offsetAttachment), model units
ATTACH_FIX = {"mac10": {"smgSuppressor": (-2.37, -0.16, 0)}}


def with_attachments(gun, names):
    """The gun plus attachment models placed like the game does (plus ATTACH_FIX for ours)."""
    z = zipfile.ZipFile(JAR)
    for n in names:
        slot = ATTACH_SLOT[n]
        text = z.read("assets/deci/models/attachments/%s/%s.bmodel" % (slot, n)).decode("latin1")
        tp = "assets/deci/textures/model/attachments/%s/%s.png" % (slot, n)
        img = Image.open(BytesIO(z.read(tp))).convert("RGBA") if tp in z.namelist() else None
        a = Gun(n, slot, text, img, False)
        ox, oy, oz = attach_offset(gun, n)
        fix = ATTACH_FIX.get(gun.name, {}).get(n, (0, 0, 0)) if gun.ours else (0, 0, 0)
        ox, oy, oz = ox + fix[0], oy + fix[1], oz + fix[2]
        for p in a.parts:
            p.name = n + "_" + p.name
            p.tex = (img, a.tw, a.th)
            if p.parent is None:
                p.pivot = (p.pivot[0] + ox, p.pivot[1] + oy, p.pivot[2] + oz)
        gun.parts += a.parts
    return gun


def all_names():
    z = zipfile.ZipFile(JAR)
    out = []
    for i in sorted(z.namelist()):
        m = re.match(r"assets/deci/models/guns/(\w+)/(\w+)\.bmodel$", i)
        if m:
            out.append(m.group(2))
    return out


VIEWS = {
    # name: (yaw degrees about y, pitch degrees about x); screen x = right, y = down, nearer = +z
    "side": (0, 0),
    "other": (180, 0),
    "top": (0, -90),
    "three": (-35, -25),
}


def view_point(p, yaw, pitch):
    q = rot_y(p, math.radians(yaw))
    q = rot_x(q, math.radians(pitch))
    return q


def solve(a, b):
    """Gaussian elimination, a is n x n, b n."""
    n = len(b)
    m = [list(a[i]) + [b[i]] for i in range(n)]
    for c in range(n):
        piv = max(range(c, n), key=lambda r: abs(m[r][c]))
        if abs(m[piv][c]) < 1e-12:
            return None
        m[c], m[piv] = m[piv], m[c]
        for r in range(n):
            if r != c:
                f = m[r][c] / m[c][c]
                for k in range(c, n + 1):
                    m[r][k] -= f * m[c][k]
    return [m[i][n] / m[i][i] for i in range(n)]


def perspective(screen, tex):
    """PIL PERSPECTIVE coefficients mapping output (screen) points to texture points."""
    a, b = [], []
    for (x, y), (u, v) in zip(screen, tex):
        a.append([x, y, 1, 0, 0, 0, -u * x, -u * y])
        b.append(u)
        a.append([0, 0, 0, x, y, 1, -v * x, -v * y])
        b.append(v)
    return solve(a, b)


def render(gun, view, scale=24, ss=2, pad=12, bg=(236, 233, 224, 255), only=None, split=False):
    """split: every part one flat colour (shows how the shapes are cut into parts)."""
    yaw, pitch = VIEWS[view]
    k = scale * ss
    faces = []
    tex = gun.texture
    if tex is None:
        tex = Image.new("RGBA", (gun.tw, gun.th), (120, 120, 120, 255))
    light = (0.35, -0.75, 0.55)
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)
    for k, part in enumerate(gun.parts):
        if only and part.group not in only:
            continue
        colour = ((k * 97) % 200 + 40, (k * 57 + 80) % 200 + 40, (k * 151 + 30) % 200 + 40)
        ptex, fx, fy = tex, tex.width / gun.tw, tex.height / gun.th
        if part.tex:
            ptex = part.tex[0] or Image.new("RGBA", (part.tex[1], part.tex[2]), (150, 120, 60, 255))
            fx, fy = ptex.width / part.tex[1], ptex.height / part.tex[2]
        for verts, (u1, v1, u2, v2) in part.quads():
            q = [view_point(p, yaw, pitch) for p in verts]
            depth = sum(p[2] for p in q) / 4
            e1 = [q[1][i] - q[0][i] for i in range(3)]
            e2 = [q[3][i] - q[0][i] for i in range(3)]
            n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
            nl = math.sqrt(sum(c * c for c in n)) or 1
            shade = 0.62 + 0.38 * abs(sum(n[i] * light[i] for i in range(3)) / nl)
            uv = [(u2 * fx, v1 * fy), (u1 * fx, v1 * fy), (u1 * fx, v2 * fy), (u2 * fx, v2 * fy)]
            faces.append((depth, q, uv, shade, colour, ptex))
    if not faces:
        return Image.new("RGBA", (8, 8), bg)
    xs = [p[0] for f in faces for p in f[1]]
    ys = [p[1] for f in faces for p in f[1]]
    x0, y0 = min(xs), min(ys)
    W = int((max(xs) - x0) * k) + 2 * pad * ss
    H = int((max(ys) - y0) * k) + 2 * pad * ss
    img = Image.new("RGBA", (W, H), bg)
    faces.sort(key=lambda f: f[0])
    for depth, q, uv, shade, colour, ptex in faces:
        scr = [((p[0] - x0) * k + pad * ss, (p[1] - y0) * k + pad * ss) for p in q]
        bx0, by0 = int(min(s[0] for s in scr)), int(min(s[1] for s in scr))
        bx1, by1 = int(math.ceil(max(s[0] for s in scr))) + 1, int(math.ceil(max(s[1] for s in scr))) + 1
        if bx1 - bx0 < 1 or by1 - by0 < 1:
            continue
        area = 0.5 * abs(sum(scr[i][0] * scr[(i + 1) % 4][1] - scr[(i + 1) % 4][0] * scr[i][1] for i in range(4)))
        if area < 0.5:
            continue
        local = [(s[0] - bx0, s[1] - by0) for s in scr]
        if split:
            ImageDraw.Draw(img).polygon(scr, fill=tuple(int(c * shade) for c in colour) + (255,),
                                        outline=(20, 20, 20, 255))
            continue
        coeffs = perspective(local, uv)
        if coeffs is None:
            continue
        patch = ptex.transform((bx1 - bx0, by1 - by0), Image.PERSPECTIVE, coeffs, Image.NEAREST)
        r, g, b, a = patch.split()
        r, g, b = (ch.point(lambda c, f=shade: int(c * f)) for ch in (r, g, b))
        mask = Image.new("L", patch.size, 0)
        ImageDraw.Draw(mask).polygon(local, fill=255)
        a = Image.composite(a, Image.new("L", patch.size, 0), mask)
        img.alpha_composite(Image.merge("RGBA", (r, g, b, a)), (bx0, by0))
    return img.resize((W // ss, H // ss), Image.LANCZOS)


def label(img, text):
    out = Image.new("RGBA", (img.width, img.height + 14), (236, 233, 224, 255))
    out.alpha_composite(img, (0, 14))
    ImageDraw.Draw(out).text((4, 1), text, fill=(30, 30, 28, 255))
    return out


BASE = {7: (0, 0, 0), 6: (1, 0, 0), 4: (1, 1, 0), 5: (0, 1, 0), 3: (0, 0, 1), 2: (1, 0, 1), 0: (1, 1, 1), 1: (0, 1, 1)}


def shape_kind(p):
    """wedge (two corners merged), cuboid (a plain smaller box), taper (one end face smaller), skew (sheared)."""
    pts = {k: tuple(b[i] * p.size[i] + p.corners[k][i] for i in range(3)) for k, b in BASE.items()}
    pl = list(pts.values())
    for i in range(8):
        for j in range(i + 1, 8):
            if sum(abs(pl[i][a] - pl[j][a]) for a in range(3)) < 0.02:
                return "wedge"
    lo = [min(q[a] for q in pl) for a in range(3)]
    hi = [max(q[a] for q in pl) for a in range(3)]
    if all(abs(q[a] - lo[a]) < 1e-3 or abs(q[a] - hi[a]) < 1e-3 for q in pl for a in range(3)):
        return "cuboid"
    for ax in range(3):
        f0 = [pts[k] for k, b in BASE.items() if b[ax] == 0]
        f1 = [pts[k] for k, b in BASE.items() if b[ax] == 1]
        for o in range(3):
            if o != ax:
                e0 = max(q[o] for q in f0) - min(q[o] for q in f0)
                e1 = max(q[o] for q in f1) - min(q[o] for q in f1)
                if abs(e0 - e1) > 0.02:
                    return "taper"
    return "skew"


def metrics(gun):
    """The numbers a gun is compared on (docs/gun_style_guide.md, `gaps` command)."""
    import statistics as st
    (xa, xb), (ya, yb), (za, zb) = gun.bounds()
    eff, xs, ys, offs, kinds = [], [0] * 10, [0] * 5, [], {}
    for p in gun.parts:
        v = p.verts()
        eff.append(sorted(max(q[i] for q in v) - min(q[i] for q in v) for i in range(3)))
        cx, cy = sum(q[0] for q in v) / 8, sum(q[1] for q in v) / 8
        xs[min(9, int((cx - xa) / (xb - xa) * 10))] += 1
        ys[min(4, int((cy - ya) / (yb - ya) * 5))] += 1
        offs += [abs(c) for cc in p.corners for c in cc if abs(c) > 1e-6]
        k = shape_kind(p)
        kinds[k] = kinds.get(k, 0) + 1
    n = len(gun.parts)
    m = {"parts": n, "length": xb - xa, "height": yb - ya, "width": zb - za,
         "minDim": st.median(e[0] for e in eff), "midDim": st.median(e[1] for e in eff),
         "maxDim": st.median(e[2] for e in eff), "offset": st.median(offs) if offs else 0,
         "top2fifths%": 100.0 * (ys[0] + ys[1]) / n, "middle%": 100.0 * sum(xs[3:7]) / n}
    # part sizes relative to the gun's length: a short gun has smaller parts, compare these
    for k in ("minDim", "midDim", "maxDim"):
        m[k + "/L%"] = 100.0 * m[k] / m["length"]
    for k in ("taper", "cuboid", "skew", "wedge"):
        m[k + "%"] = 100.0 * kinds.get(k, 0) / n
    if gun.texture:
        px = [c for c in gun.texture.get_flattened_data() if c[3] > 0]
        lum = [0.3 * r + 0.59 * g + 0.11 * b for r, g, b, a in px]
        m["tone"], m["toneSd"] = st.mean(lum), st.pstdev(lum)
    return m


def stats(gun):
    n = len(gun.parts)
    groups = {}
    for p in gun.parts:
        groups[p.group] = groups.get(p.group, 0) + 1
    shaped = sum(1 for p in gun.parts if p.shaped())
    rotated = sum(1 for p in gun.parts if any(abs(r) > 1e-6 for r in p.rot))
    (xa, xb), (ya, yb), (za, zb) = gun.bounds()
    vols = sorted(p.size[0] * p.size[1] * p.size[2] for p in gun.parts)
    thin = sum(1 for p in gun.parts if min(p.size) <= 1)
    return {
        "gun": gun.name, "cat": gun.cat, "parts": n, "shaped%": round(100 * shaped / max(n, 1)),
        "rotated%": round(100 * rotated / max(n, 1)), "length": round(xb - xa, 1), "height": round(yb - ya, 1),
        "width": round(zb - za, 1), "median_vol": vols[len(vols) // 2] if vols else 0,
        "thin%": round(100 * thin / max(n, 1)),
        "tex": "%dx%d" % (gun.tw, gun.th) + ("@%dx" % (gun.texture.width // gun.tw) if gun.texture else ""),
        "groups": " ".join("%s=%d" % kv for kv in sorted(groups.items(), key=lambda kv: -kv[1])),
    }


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return
    cmd, rest = args[0], args[1:]
    out = None
    scale = 24
    if "--out" in rest:
        i = rest.index("--out")
        out = rest[i + 1]
        del rest[i:i + 2]
    cols = 1
    if "--cols" in rest:
        i = rest.index("--cols")
        cols = int(rest[i + 1])
        del rest[i:i + 2]
    if "--scale" in rest:
        i = rest.index("--scale")
        scale = int(rest[i + 1])
        del rest[i:i + 2]
    split = "--split" in rest
    if split:
        rest.remove("--split")
    if cmd == "stats":
        tsv = None
        if "--tsv" in rest:
            tsv = rest[rest.index("--tsv") + 1]
        rows = [stats(load(n)) for n in all_names()]
        try:
            rows.append(stats(load("ours:mac10")))
        except SystemExit:
            pass
        keys = list(rows[0].keys())
        lines = ["\t".join(keys)] + ["\t".join(str(r[k]) for k in keys) for r in rows]
        if tsv:
            open(tsv, "w").write("\n".join(lines) + "\n")
        print("\n".join(lines))
    elif cmd == "attach":
        out = out or os.path.join(ROOT, "docs", "shots", "guns_study")
        os.makedirs(out, exist_ok=True)
        g = with_attachments(load(rest[0]), rest[1:])
        for v in ("side", "three"):
            path = os.path.join(out, "attach_%s%s_%s_%s.png" % ("ours_" if g.ours else "", g.name, "_".join(rest[1:]), v))
            render(g, v, scale).save(path)
            print(path)
    elif cmd == "gaps":
        # ours vs Decimation's guns of its category: median, q10, q90 per metric
        import statistics as st
        g = load(rest[0])
        cat = g.cat
        refs = [metrics(load(n)) for n in all_names() if load(n).cat == cat]
        mine = metrics(g)
        print("%s vs %d Decimation %s guns" % (g.name, len(refs), cat))
        print("%-12s %8s %8s %16s" % ("metric", "ours", "median", "q10 .. q90"))
        for k, v in mine.items():
            vals = sorted(r[k] for r in refs if k in r)
            lo, hi = vals[len(vals) // 10], vals[min(len(vals) - 1, len(vals) * 9 // 10)]
            flag = "" if lo <= v <= hi else "  <- outside"
            print("%-12s %8.2f %8.2f %7.2f .. %-7.2f%s" % (k, v, st.median(vals), lo, hi, flag))
    elif cmd == "vocab":
        counts = {}
        for n in (rest or all_names()):
            for p in load(n).parts:
                k = shape_kind(p)
                counts[k] = counts.get(k, 0) + 1
        total = sum(counts.values())
        for k, c in sorted(counts.items(), key=lambda kv: -kv[1]):
            print("%-7s %6d %3d %%" % (k, c, round(100 * c / total)))
    elif cmd == "parts":
        g = load(rest[0])
        for p in g.parts:
            print("%-22s size %-14s pivot %-28s rot %-22s %s" % (
                p.name, p.size, tuple(round(c, 2) for c in p.pivot), tuple(round(c, 2) for c in p.rot),
                shape_kind(p)))
    elif cmd == "render":
        out = out or os.path.join(ROOT, "docs", "shots", "guns_study")
        os.makedirs(out, exist_ok=True)
        for n in rest:
            g = load(n)
            for v in VIEWS:
                path = os.path.join(out, "%s%s_%s%s.png" % ("ours_" if g.ours else "", g.name, v,
                                                             "_split" if split else ""))
                render(g, v, scale, split=split).save(path)
                print(path)
    elif cmd == "sheet":
        imgs = []
        for n in rest:
            g = load(n)
            s = stats(g)
            imgs.append(label(render(g, "side", scale), "%s%s  %s  parts %d  shaped %d%%  %sx%s" % (
                "OURS " if g.ours else "", g.name, g.cat, s["parts"], s["shaped%"], s["length"], s["height"])))
        cw, ch = max(i.width for i in imgs), max(i.height for i in imgs)
        rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cw * min(cols, len(imgs)), ch * rows), (236, 233, 224, 255))
        for k, i in enumerate(imgs):
            sheet.alpha_composite(i, ((k % cols) * cw, (k // cols) * ch))
        os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
        sheet.save(out)
        print(out)


if __name__ == "__main__":
    main()
