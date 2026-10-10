#!/usr/bin/env python3
"""Where every Decimation prop is DRAWN, per metadata, computed from its code.

    python3 tools/props/propgeom.py table > docs/references/prop_geometry.tsv
    python3 tools/props/propgeom.py show BlockWallflag [BlockSpotlight ...]
    python3 tools/props/propgeom.py check        # compare with docs/prop_footprints.tsv
    python3 tools/props/propgeom.py resource     # the table the generator reads (prop_boxes.tsv)

Reads the readable decompile (deobf/src) and the prop .bmodel files in
Decimation.jar; nothing is copied anywhere. For each prop block it finds
the renderer (PropRenderer for every BlockProp, or the block's own
TileEntitySpecialRenderer), replays that renderer's GL transform for
metadata 2..5 (the translate, the 180 degree flip about x, the per meta
yaw from its switch, scales, extra rotations) and pushes every model box
through it. Output: the world box the model covers, relative to the
block's own corner (0..1 = inside its cell), per metadata.

Facing: every Decimation prop block sets its metadata from the placer's
yaw (5 when the player looks south, 2 west, 3 north, 4 east), so the side
the author built as the front faces the player: front 2 east, 3 south,
4 west, 5 north. The table also reports which way the model's own -z side
ends up ("mz"), which lets a check compare the two.
"""
import math
import os
import re
import sys
import zipfile

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SRC = os.path.join(ROOT, "deobf", "src")
JAR = os.path.join(ROOT, "Decimation.jar")
sys.path.insert(0, os.path.join(ROOT, "tools", "guns"))

NUM = r"-?\d+(?:\.\d+)?(?:[eE]-?\d+)?[fFdD]?"
FRONT = {2: "E", 3: "S", 4: "W", 5: "N"}


def read(rel):
    with open(os.path.join(SRC, rel), errors="ignore") as f:
        return f.read()


def num(s):
    return float(s.rstrip("fFdD"))


# ---------------------------------------------------------------- matrices
def mat_id():
    return [[1.0 if i == j else 0.0 for j in range(4)] for i in range(4)]


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(m, x, y, z):
    t = mat_id()
    t[0][3], t[1][3], t[2][3] = x, y, z
    return mat_mul(m, t)


def scale(m, x, y, z):
    t = mat_id()
    t[0][0], t[1][1], t[2][2] = x, y, z
    return mat_mul(m, t)


def rotate(m, deg, x, y, z):
    n = math.sqrt(x * x + y * y + z * z)
    if n == 0 or deg == 0:
        return m
    x, y, z = x / n, y / n, z / n
    a = math.radians(deg)
    c, s, t = math.cos(a), math.sin(a), 1 - math.cos(a)
    r = [[t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0],
         [t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0],
         [t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0],
         [0, 0, 0, 1]]
    return mat_mul(m, r)


def apply(m, p):
    return tuple(m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2] + m[i][3] for i in range(3))


# ---------------------------------------------------------------- models
def rot_xyz(p, rx, ry, rz):
    # ModelRenderer: glRotate z, then y, then x (the point turns about x first)
    x, y, z = p
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def java_model(cls):
    """Corner points (model units) of every rendered box of a ModelBase in deobf/src."""
    path = None
    for d in ("decimation/model", "net/decimation/mod/client/models"):
        if os.path.exists(os.path.join(SRC, d, cls + ".java")):
            path = d + "/" + cls + ".java"
    if path is None:
        return None
    s = read(path)
    parts = {}
    for m in re.finditer(r"this\.(\w+(?:\[\d+\])?) = new ModelRenderer\(", s):
        parts[m.group(1)] = {"boxes": [], "rp": (0, 0, 0), "rot": [0, 0, 0], "off": (0, 0, 0), "parent": None}
    for m in re.finditer(r"this\.(\w+(?:\[\d+\])?)\.addBox\(([^;]*)\);", s):
        if m.group(1) in parts:
            v = [num(x) for x in re.findall(NUM, m.group(2))]
            parts[m.group(1)]["boxes"].append(v[:6])
    for m in re.finditer(r"this\.(\w+(?:\[\d+\])?)\.setRotationPoint\(([^;]*)\);", s):
        if m.group(1) in parts:
            parts[m.group(1)]["rp"] = tuple(num(x) for x in re.findall(NUM, m.group(2))[:3])
    for m in re.finditer(r"this\.(?:a|setRotation)\(this\.(\w+(?:\[\d+\])?), ([^;]*)\);", s):
        if m.group(1) in parts:
            r = [num(x) for x in re.findall(NUM, m.group(2))[:3]]
            parts[m.group(1)]["rot"] = (r + [0.0, 0.0, 0.0])[:3]
    for m in re.finditer(r"this\.(\w+)\.rotateAngle([XYZ]) = (" + NUM + r");", s):
        if m.group(1) in parts:
            parts[m.group(1)]["rot"]["XYZ".index(m.group(2))] = num(m.group(3))
    for m in re.finditer(r"this\.(\w+(?:\[\d+\])?)\.addChild\(this\.(\w+)\);", s):
        if m.group(2) in parts:
            parts[m.group(2)]["parent"] = m.group(1)
    body = re.search(r"public void render\(Entity[^{]*\{(.*?)\n    \}", s, re.S)
    rendered = set(re.findall(r"this\.(\w+)\.render\(", body.group(1))) if body else set(parts)
    if body and "for (" in body.group(1):
        rendered |= set(parts)  # parts drawn in a loop over an array
    # ModelRenderer.offsetX/Y/Z: a translate in block units before the part (CookingStation: -0.5)
    for m in re.finditer(r"this\.(\w+)(\[\w+\])?\.offset([XYZ]) = (" + NUM + r");", s):
        names = [k for k in parts if k == m.group(1) or k.startswith(m.group(1) + "[")] \
            if m.group(2) and not re.match(r"\[\d+\]", m.group(2)) else [m.group(1) + (m.group(2) or "")]
        for k in names:
            if k in parts:
                o = list(parts[k]["off"])
                o["XYZ".index(m.group(3))] = num(m.group(4)) * 16
                parts[k]["off"] = tuple(o)
    # GL calls the model itself makes before drawing its parts (ModelBarrier turns 90 degrees)
    MODEL_OPS[cls] = gl_calls(body.group(1), {}) if body else []
    hidden = set(re.findall(r"this\.(\w+)\.isHidden = true", s))

    def place(name, p):
        q = parts[name]
        p = rot_xyz(p, *q["rot"])
        p = (p[0] + q["rp"][0] + q["off"][0], p[1] + q["rp"][1] + q["off"][1], p[2] + q["rp"][2] + q["off"][2])
        return place(q["parent"], p) if q["parent"] in parts else p

    pts = []
    for name, q in parts.items():
        top = name
        while parts[top]["parent"] in parts:
            top = parts[top]["parent"]
        if top not in rendered or name in hidden:
            continue
        for b in q["boxes"]:
            x, y, z, w, h, d = b
            for cx in (x, x + w):
                for cy in (y, y + h):
                    for cz in (z, z + d):
                        pts.append(place(name, (cx, cy, cz)))
    return pts


_jar = None


def bmodel(name):
    global _jar
    import study
    if _jar is None:
        _jar = zipfile.ZipFile(JAR)
    path = "assets/deci/models/props/%s.bmodel" % name
    if path not in _jar.namelist():
        return None
    g = study.Gun(name, "prop", _jar.read(path).decode(errors="ignore"), None, False)
    return [q for p in g.parts for q in p.verts()]


# ---------------------------------------------------------------- registry
class Prop:
    def __init__(self, block):
        self.block = block          # registry name, e.g. BlockSpotlight
        self.renderer = None        # renderer class
        self.model = None           # ModelX or bmodel:name
        self.extra = (0.0, 0.0, 0.0)
        self.size = None            # collision box (setPropSize / setBlockBounds)
        self.note = ""


def registry():
    props = {}
    reg = read("decimation/block/PropBlockRegistry.java")
    for m in re.finditer(r"= new (\w+)\(([^;]*?)\)((?:\.\w+\([^;]*?\))*)\.setCreativeTab", reg):
        cls, args, chain = m.group(1), m.group(2), m.group(3)
        t = re.search(r'"tile\.(\w+)"', args)
        if not t:
            continue
        p = Prop(t.group(1))
        mm = re.search(r"new (Model\w+)\(\)", args)
        mb = re.findall(r'"([^"]*)"', args)
        if mm:
            p.model = mm.group(1)
        elif len(mb) >= 3:
            p.model = "bmodel:" + mb[2]
        p.renderer = "PropRenderer"
        e = re.search(r"setExtraRotation\(([^)]*)\)", chain)
        if e:
            p.extra = tuple(num(x) for x in re.findall(NUM, e.group(1)))
        z = re.search(r"setPropSize\(([^)]*)\)", chain)
        if z:
            p.size = tuple(num(x) for x in re.findall(NUM, z.group(1)))
        props[p.block] = p
    # blocks with their own class: block -> tile entity -> renderer -> model
    bind = dict(re.findall(r"bindTileEntitySpecialRenderer\((\w+)\.class, \(TileEntitySpecialRenderer\)new (\w+)\(\)",
                           read("decimation/render/ClientRenderRegistry.java")))
    files = [os.path.join(SRC, d, f) for d in ("net/decimation/mod/common/block/props", "decimation/block")
             for f in sorted(os.listdir(os.path.join(SRC, d))) if f.endswith(".java")]
    for path in files:
        s = open(path, errors="ignore").read()
        n = re.search(r'setBlockName\("(\w+)"\)', s) or re.search(r'"tile\.(\w+)"', s)
        te = re.search(r"return new (\w+)\(\);", s)
        sup = re.search(r'super\((\w+|"\w+"), "tile\.(\w+)", Material\.\w+, new (Model\w+)\(\)\)', s)
        if sup and not te:
            # a BlockProp subclass: TileEntityProp, drawn by PropRenderer
            p = props.get(sup.group(2)) or Prop(sup.group(2))
            p.renderer, p.model = "PropRenderer", sup.group(3)
            e = re.search(r"setExtraRotation\(([^)]*)\)", s)
            if e:
                p.extra = tuple(num(x) for x in re.findall(NUM, e.group(1)))
            props[p.block] = p
            continue
        if not n or not te or te.group(1) not in bind:
            continue
        p = props.get(n.group(1)) or Prop(n.group(1))
        p.renderer = bind[te.group(1)]
        r = read("decimation/render/%s.java" % p.renderer)
        mm = re.search(r"new (Model\w+)\(\)", r)
        mb = re.search(r"models/props/(\w+)\.bmodel", r)
        p.model = mm.group(1) if mm else ("bmodel:" + mb.group(1) if mb else None)
        z = re.search(r"setBlockBounds\(([^)]*)\)", s)
        if z:
            p.size = tuple(num(x) for x in re.findall(NUM, z.group(1)))
        props[p.block] = p
    return props


# ---------------------------------------------------------------- renderers
def gl_ops(renderer, meta, extra):
    """The GL calls of renderTileEntityAt for one metadata, in order."""
    if renderer == "PropRenderer":
        return [("t", 0.5, 0, 0.5), ("r", 180, 0.1, 0, 0), ("r", meta % 4 * 90, 0, 1, 0),
                ("r", extra[0], 1, 0, 0), ("r", extra[1], 0, 1, 0), ("r", extra[2], 0, 0, 1)]
    s = read("decimation/render/%s.java" % renderer)
    body = re.search(r"public void renderTileEntityAt\(([^)]*)\)\s*\{(.*?)\n    \}", s, re.S)
    if not body:
        return None
    text = body.group(2)
    # pick the switch case for this metadata and inline it
    sw = re.search(r"switch \((.*?)\) \{(.*?)\n        \}", text, re.S)
    env = {"n": 0.0}
    if sw:
        key = meta % 4 if "% 4" in sw.group(1) else meta
        chosen = ""
        for c in re.finditer(r"case (\d+): \{(.*?)(?=case \d+:|\Z)", sw.group(2), re.S):
            if int(c.group(1)) == key:
                chosen = c.group(2)
        for a in re.finditer(r"(\w+) = (" + NUM + r");", chosen):
            env[a.group(1)] = num(a.group(2))
        text = text[:sw.start()] + chosen + text[sw.end():]
    return gl_calls(text, env)


MODEL_OPS = {}


def gl_calls(text, env):
    ops = []
    for g in re.finditer(r"GL11\.gl(Translate|Rotate|Scale)[fd]\((.*?)\);", text):
        vals = [evaluate(a.strip(), env) for a in split_args(g.group(2))]
        if g.group(1) == "Translate":
            ops.append(("t", vals[0], vals[1], vals[2]))
        elif g.group(1) == "Rotate":
            ops.append(("r",) + tuple(vals))
        else:
            ops.append(("s", vals[0], vals[1], vals[2]))
    return ops


def split_args(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return out


def evaluate(expr, env):
    e = re.sub(r"\((?:float|double|int)\)", "", expr)
    e = re.sub(r"(\d)[fFdD]\b", r"\1", e)
    e = re.sub(r"\bd[23]?\b", "0", e)
    for k, v in env.items():
        e = re.sub(r"\b%s\b" % k, repr(v), e)
    # only arithmetic left from our own local decompile, no names, no builtins
    try:
        return float(eval(e, {"__builtins__": {}}, {}))
    except Exception:
        return 0.0


def world_box(prop, meta):
    pts = model_points(prop)
    if not pts:
        return None
    m = mat_id()
    for op in (gl_ops(prop.renderer, meta, prop.extra) or []) + MODEL_OPS.get(prop.model, []):
        if op[0] == "t":
            m = translate(m, *op[1:4])
        elif op[0] == "r":
            m = rotate(m, *op[1:5])
        else:
            m = scale(m, *op[1:4])
    w = [apply(m, (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0)) for p in pts]
    box = [min(q[0] for q in w), max(q[0] for q in w), min(q[1] for q in w), max(q[1] for q in w),
           min(q[2] for q in w), max(q[2] for q in w)]
    # which way the model's -z side points in the world
    o = apply(m, (0, 0, 0))
    v = apply(m, (0, 0, -1))
    dx, dz = v[0] - o[0], v[2] - o[2]
    mz = ("E" if dx > 0 else "W") if abs(dx) > abs(dz) else ("S" if dz > 0 else "N")
    if prop.block == "BlockWallflag":
        # the cloth quad is 6 blocks tall but its texture is transparent below about 2.5 of them
        # (user screenshot 10 Oktober 2026, docs/shots/milbase_v0.42_review/user_72.png) [inferred]
        box[2] = max(box[2], box[3] - 2.5)
    return [round(b, 3) for b in box], mz


def turn_meta(prop, meta):
    """The metadata drawing this prop turned a quarter clockwise (seen from above: north -> east):
    a point (x, z) of the cell goes to (1 - z, x), as MilitaryBasePlan turns its canvas. Plain
    BlockProps give meta + 1; renderers with their own tables do not (military wrecks: 2 -> 5)."""
    box = world_box(prop, meta)[0]
    want = (round(1 - box[5], 2), round(1 - box[4], 2), round(box[0], 2), round(box[1], 2))
    o = 2 + ((meta - 2 + 1) & 3)
    best, err = o, None
    for m in (o,) + tuple(x for x in (2, 3, 4, 5) if x != o):
        b = world_box(prop, m)[0]
        e = max(abs(b[0] - want[0]), abs(b[1] - want[1]), abs(b[4] - want[2]), abs(b[5] - want[3]))
        if err is None or e < err - 0.02:
            best, err = m, e
    # the model's -z side must turn the same way (tells symmetric boxes apart)
    order = "NESW"
    mz = world_box(prop, meta)[1]
    if world_box(prop, best)[1] != order[(order.index(mz) + 1) % 4]:
        for m in (2, 3, 4, 5):
            b = world_box(prop, m)
            e = max(abs(b[0][0] - want[0]), abs(b[0][1] - want[1]), abs(b[0][4] - want[2]), abs(b[0][5] - want[3]))
            if b[1] == order[(order.index(mz) + 1) % 4] and e <= err + 0.05:
                best = m
    return best


_cache = {}


def model_points(prop):
    if prop.model not in _cache:
        if prop.model is None:
            _cache[prop.model] = None
        elif prop.model.startswith("bmodel:"):
            _cache[prop.model] = bmodel(prop.model[7:])
        else:
            _cache[prop.model] = java_model(prop.model)
    return _cache[prop.model]


def cells(box):
    """Cells the drawn box covers past its own (overhang under 0.3 ignored)."""
    x0, x1, _, _, z0, z1 = box
    return (int(math.floor(x0 + 0.3)), int(math.ceil(x1 - 0.3)) - 1,
            int(math.floor(z0 + 0.3)), int(math.ceil(z1 - 0.3)) - 1)


def main():
    props = registry()
    cmd = sys.argv[1] if len(sys.argv) > 1 else "table"
    if cmd == "table":
        print("prop\trenderer\tmodel\tmeta\tfront\tmodel_minus_z\tx0\tx1\ty0\ty1\tz0\tz1\tcells_x\tcells_z")
        for name in sorted(props):
            p = props[name]
            for meta in (2, 3, 4, 5):
                r = world_box(p, meta)
                if not r:
                    continue
                box, mz = r
                c = cells(box)
                print("\t".join([name, p.renderer, p.model or "-", str(meta), FRONT[meta], mz]
                                + ["%.2f" % b for b in box] + ["%d..%d" % (c[0], c[1]), "%d..%d" % (c[2], c[3])]))
    elif cmd == "resource":
        # the drawn boxes for the generator (net.decimation.worldgen.military.PropBoxes), plus per
        # metadata the metadata that draws the same prop turned 90 degrees clockwise
        out = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deciworldgen", "prop_boxes.tsv")
        with open(out, "w") as f:
            f.write("# prop meta x0 x1 y0 y1 z0 z1 turn: drawn box relative to the block corner and the\n"
                    "# metadata after one clockwise quarter turn, from tools/props/propgeom.py\n"
                    "# (Decimation's renderers and models), docs/prop_placement.md\n")
            for name in sorted(props):
                boxes = {m: world_box(props[name], m) for m in (2, 3, 4, 5)}
                for meta in (2, 3, 4, 5):
                    if boxes[meta]:
                        f.write("%s %d %s %d\n" % (name, meta, " ".join("%.3f" % v for v in boxes[meta][0]),
                                                   turn_meta(props[name], meta)))
        print("wrote", out)
    elif cmd == "show":
        for name in sys.argv[2:]:
            p = props.get(name)
            if not p:
                print(name, "not a prop")
                continue
            print(name, p.renderer, p.model, "extra", p.extra, "collision", p.size)
            for meta in (2, 3, 4, 5):
                r = world_box(p, meta)
                print("  meta", meta, "front", FRONT[meta], r)
    elif cmd == "check":
        meas = {}
        for line in open(os.path.join(ROOT, "docs", "prop_footprints.tsv")):
            f = line.rstrip("\n").split("\t")
            if f[0] == "prop" or f[1] == "-":
                continue
            meas[f[0]] = [float(x) for x in f[1:5]]
        bad = 0
        for name, (w, e, n, s) in sorted(meas.items()):
            p = props.get(name)
            r = world_box(p, 3) if p else None
            if not r:
                print("%-28s no geometry" % name)
                continue
            box = r[0]
            got = (max(0, -box[0]), max(0, box[1] - 1), max(0, -box[4]), max(0, box[5] - 1))
            diff = max(abs(a - b) for a, b in zip(got, (w, e, n, s)))
            flag = "" if diff < 0.2 else "  <-- differs"
            bad += bool(flag)
            print("%-28s measured W%.1f E%.1f N%.1f S%.1f  computed W%.1f E%.1f N%.1f S%.1f%s"
                  % ((name, w, e, n, s) + got + (flag,)))
        print("differ:", bad, "of", len(meas))


if __name__ == "__main__":
    main()
