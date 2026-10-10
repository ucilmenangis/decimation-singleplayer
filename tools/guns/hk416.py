#!/usr/bin/env python3
"""HK416 (black) and HK416 Tan: Decimation's own M4A4 with our HK parts.

User request (10 Oktober 2026): "try hk416 ... make 2 version, black and tan", 5 photos (HK416
D / A5 side views, three quarter, a tan A5); "two separate guns". Variant route (skill lesson 13,
tools/guns/ump9.py): the M4A4 model, texture and animations are read from the user's own
Decimation.jar at build time and the outputs are git ignored; only the parts below are ours.

Kept from the M4A4: receiver, trigger group, magazine, the folding rear sight (so the aim and
the hands stay Decimation's own), header (flamePos at the same muzzle x 19.15).
Replaced (dropped by position, see DROP): the quad rail handguard and A-frame front sight, the
barrel and birdcage, the collapsible stock, the A2 grip.
Ours, measured on the user's side photo (photo x 250 = model x 2.7 at the rear sight, photo x
738 = 19.15 at the muzzle; the butt then lands at -5.12, the M4A4's at -5.15; heights follow
Decimation's receiver, which is drawn taller than the real gun):
- HK rail handguard x 7.5 to 14.4 (top, bottom and side rails with teeth, slots),
- HK folding front sight on the rail's front end (post tip -4.7 like the M4A4's post),
- thin barrel x 14.4 to 17.55 on the M4A4 bore (-2.39), HK flash hider to 19.15,
- HK slim line stock (ribbed butt, angled lower panel, buffer tube to the receiver),
- HK ergonomic grip (raked, flared base, textured panels, finger bumps).
Tan: the same model, the texture recoloured to flat dark earth except barrel, muzzle, sights,
trigger and bolt (user photo 4).

    python3 tools/guns/hk416.py     # needs Decimation.jar in the project root
"""
import os
import sys
import zipfile
from io import BytesIO

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import ZC, layout, octagon, paint, part, part_block, icon  # noqa: E402
import study  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deci")
G = "gunModel"
POLY = (34, 34, 36)     # polymer furniture
RAIL = (40, 40, 42)     # aluminium rails
DARK = (24, 24, 26)     # slots, sights
METAL = (46, 46, 48)    # barrel
FLASH = (36, 36, 38)
BORE = -2.39            # the M4A4's bore centre (barrel parts y -2.7 .. -2.07)
TAN = (128, 108, 80)    # flat dark earth (Decimation's tan guns: AR15 Beowulf 110,95,81)

P = []


def add(*parts, pair=False):
    for p in parts:
        P.append(p)
        if pair:
            P.append(p.mirror())


# ---------------------------------------------------------------- HK rail handguard (x 7.5 .. 14.4)
# its top rail flush with the receiver's (M4A4 receiver rail teeth y -3.6 .. -3.5, 0.4 pitch),
# as on the real gun (the M4A4's own quad rail stands higher, -4.2)
X0, X1 = 7.5, 14.4
add(part("hgUpper", G, RAIL, (X0, -3.38, -0.85), (X1, -2.5, 0.55)).inset("y", 0, z=(0.15, 0.15)))
add(part("hgLower", G, RAIL, (X0, -2.5, -0.85), (X1, -1.55, 0.55)).inset("y", 1, z=(0.15, 0.15)))
add(part("hgCap", G, RAIL, (X1 - 0.1, -3.33, -0.8), (X1 + 0.05, -1.6, 0.5)).inset("x", 1, y=(0.12, 0.12), z=(0.12, 0.12)))
add(part("railTopBase", G, RAIL, (X0, -3.5, -0.5), (X1, -3.38, 0.2)))
x = X0 + 0.2
while x + 0.2 <= X1:
    add(part("railTopTooth", G, RAIL, (x, -3.6, -0.5), (x + 0.2, -3.5, 0.2)).inset("y", 0, x=(0.02, 0.02)))
    x += 0.4
add(part("railBottomBase", G, RAIL, (X0 + 0.3, -1.55, -0.5), (X1 - 0.1, -1.42, 0.2)))
x = X0 + 0.4
while x + 0.2 <= X1 - 0.1:
    add(part("railBottomTooth", G, RAIL, (x, -1.42, -0.5), (x + 0.2, -1.32, 0.2)).inset("y", 1, x=(0.02, 0.02)))
    x += 0.8
# side details start inside the handguard: its sides lean in (insets), so a plate at z 0.55 stood off
# them by up to 0.06 (study.py contact, case 14)
add(part("railSideBase", G, RAIL, (X0 + 0.2, -2.8, 0.45), (X1 - 0.1, -2.25, 0.65)), pair=True)
x = X0 + 0.3
while x + 0.2 <= X1 - 0.1:
    add(part("railSideTooth", G, RAIL, (x, -2.8, 0.65), (x + 0.2, -2.25, 0.75)).inset("z", 1, y=(0.03, 0.03)), pair=True)
    x += 0.8
for i, (a, b) in enumerate(((8.0, 9.3), (9.6, 10.9), (11.2, 12.5), (12.8, 13.9))):
    add(part("slot%d" % i, G, DARK, (a, -2.12, 0.4), (b, -1.9, 0.6)).inset("z", 1, x=(0.06, 0.06)), pair=True)
    add(part("slotHigh%d" % i, G, DARK, (a + 0.2, -3.15, 0.38), (b - 0.2, -2.95, 0.52)).inset("z", 1, x=(0.05, 0.05)), pair=True)

# ---------------------------------------------------------------- HK folding front sight on the rail front
# post tip at -4.7 like the M4A4's front post (same sight picture, skill lesson 7). The real sight
# (user photo 36) is a U: two thick ears flared at the base on one bridge. Ear tops stop just over
# the post tip like the M4A4's front ears (-4.7): higher ears show beside the rear sight's ears in
# aim. v0.39.0 had two thin separate ears up to -4.97: from behind "2 pillars" (case 14).
add(part("fsClamp", G, DARK, (13.9, -3.85, -0.48), (14.4, -3.6, 0.18)).inset("y", 0, x=(0.06, 0.06), z=(0.05, 0.05)))
add(part("fsTower", G, DARK, (13.98, -4.45, -0.42), (14.32, -3.85, 0.12)).inset("y", 0, x=(0.05, 0.05), z=(0.02, 0.02)))
add(part("fsBridge", G, DARK, (13.98, -4.58, ZC - 0.3), (14.32, -4.45, ZC + 0.3)).inset("y", 0, x=(0.02, 0.02)))
add(part("fsEar", G, DARK, (14.0, -4.74, ZC + 0.14), (14.3, -4.58, ZC + 0.3)).inset("y", 0, x=(0.05, 0.05), z=(0.0, 0.06)), pair=True)
add(part("fsPost", G, DARK, (14.1, -4.7, -0.21), (14.2, -4.58, -0.09)).inset("y", 0, x=(0.02, 0.02), z=(0.02, 0.02)))
add(part("fsPostBase", G, DARK, (14.06, -4.62, ZC - 0.09), (14.24, -4.58, ZC + 0.09)))
add(part("fsHinge", G, DARK, (13.92, -4.0, -0.4), (14.0, -3.85, 0.1)))

# ---------------------------------------------------------------- barrel and HK flash hider (bore -2.39)
add(*octagon("barrelNut", G, METAL, X1, X1 + 0.25, BORE, ZC, 0.32))
add(*octagon("barrel", G, METAL, X1 + 0.25, 16.6, BORE, ZC, 0.2))
add(*octagon("barrelStep", G, METAL, 16.6, 17.55, BORE, ZC, 0.18))
add(*octagon("flashBody", G, FLASH, 17.55, 18.45, BORE, ZC, 0.28))
add(*octagon("flashCrown", G, FLASH, 18.45, 19.15, BORE, ZC, 0.3))
for i, (dy, dz) in enumerate(((-0.31, 0.0), (0.31, 0.0))):
    add(part("flashSlot%d" % i, G, DARK, (18.5, BORE + dy - 0.03, ZC - 0.12), (19.1, BORE + dy + 0.03, ZC + 0.12)))
add(part("flashSlotSide", G, DARK, (18.5, BORE - 0.12, ZC + 0.28), (19.1, BORE + 0.12, ZC + 0.32)), pair=True)
add(part("flashRing", G, FLASH, (17.95, BORE - 0.32, ZC - 0.32), (18.05, BORE + 0.32, ZC + 0.32)).inset("x", 0, y=(0.06, 0.06), z=(0.06, 0.06)))

# ---------------------------------------------------------------- HK slim line stock (x -5.15 .. 0.4) + buffer tube
add(*octagon("tube", G, POLY, 0.3, 1.3, -2.45, ZC, 0.32))
add(part("stockUpper", G, POLY, (-4.75, -3.2, -0.5), (0.35, -1.65, 0.2)).inset("x", 1, y=(0.2, 0.25), z=(0.06, 0.06)).inset("y", 0, z=(0.08, 0.08)))
# lower panel: square at the butt, its front edge angled up to the tube (user photo 36); every
# detail on it stays inside the outline (v0.39.0 fins hung 0.77 below it, case 14)
LX, LF, LB = -3.4, -1.4, 0.35        # wedge start, wedge front, panel bottom


def lower_bottom(x):
    """y of the lower panel's bottom edge at x (the wedge's front face is collapsed to y -1.6)."""
    return LB if x <= LX else LB - 1.95 * (x - LX) / (LF - LX)


add(part("stockLowerRear", G, POLY, (-4.75, -1.65, -0.45), (LX, LB, 0.15)).inset("y", 1, z=(0.06, 0.06)))
lower = part("stockLower", G, POLY, (LX, -1.65, -0.45), (LF, LB, 0.15))
lower.inset("x", 1, y=(0.0, 1.95))   # the bottom rises toward the front: the angled front edge
add(lower.inset("y", 1, z=(0.06, 0.06)))
add(part("buttPlate", G, POLY, (-5.05, -3.15, -0.55), (-4.75, 0.3, 0.25)).inset("x", 0, y=(0.08, 0.08), z=(0.05, 0.05)))
for i in range(7):
    y0 = -3.0 + i * 0.47
    add(part("buttRib", G, DARK, (-5.15, y0, -0.5), (-5.05, y0 + 0.24, 0.2)).inset("x", 0, y=(0.04, 0.04)))
# the ribbed band along the angled edge: short ribs, each 0.22 inside the bottom edge at its front
# end, so the whole rib lies on the panel (the edge falls toward the rear)
for i in range(7):
    xr = -3.0 + i * 0.25
    yr = lower_bottom(xr) - 0.22
    if yr - 0.07 < -1.55:
        break
    add(part("stockFin%d" % i, G, DARK, (xr - 0.42, yr - 0.07, 0.08), (xr, yr, 0.19)).inset("z", 1, x=(0.03, 0.03)), pair=True)
add(part("stockSlot", G, DARK, (-4.3, -2.05, 0.14), (-3.2, -1.91, 0.24)).inset("z", 1, x=(0.05, 0.05)), pair=True)
add(part("stockLatch", G, DARK, (-0.3, -2.18, -0.3), (0.2, -1.95, 0.0)).inset("y", 1, x=(0.05, 0.05)))  # under the tube

# ---------------------------------------------------------------- HK ergonomic grip (top y -0.8 .. base 2.2)


def grip_edge(y, top, bottom):
    return top + (bottom - top) * (y + 0.8) / 3.0


for i, (y0, y1) in enumerate(((-0.8, 0.2), (0.2, 1.2), (1.2, 2.05))):
    r0, r1 = grip_edge(y0, 2.05, 0.8), grip_edge(y1, 2.05, 0.8)
    f0, f1 = grip_edge(y0, 3.05, 2.45), grip_edge(y1, 3.05, 2.45)
    g = part("grip%d" % i, G, POLY, (r0, y0, -0.6), (f0, y1, 0.3))
    g.c[(0, 1, 0)][0] = g.c[(0, 1, 1)][0] = r1      # bottom rear edge further back
    g.c[(1, 1, 0)][0] = g.c[(1, 1, 1)][0] = f1      # bottom front edge
    add(g.inset("x", 0, z=(0.12, 0.12)))
add(part("gripBase", G, POLY, (0.65, 2.05, -0.66), (2.55, 2.25, 0.36)).inset("y", 1, x=(0.08, 0.08), z=(0.06, 0.06)))
for i, y in enumerate((-0.35, 0.5, 1.3)):
    f = grip_edge(y + 0.25, 3.05, 2.45)
    add(part("fingerBump%d" % i, G, POLY, (f - 0.1, y, -0.5), (f + 0.12, y + 0.5, 0.2)).inset("x", 1, y=(0.15, 0.15)))
gp = part("gripPanel", G, DARK, (grip_edge(-0.4, 2.05, 0.8) + 0.2, -0.4, 0.16), (grip_edge(-0.4, 3.05, 2.45) - 0.2, 1.8, 0.35))
gp.c[(0, 1, 0)][0] = gp.c[(0, 1, 1)][0] = grip_edge(1.8, 2.05, 0.8) + 0.2
gp.c[(1, 1, 0)][0] = gp.c[(1, 1, 1)][0] = grip_edge(1.8, 3.05, 2.45) - 0.2
add(gp.inset("z", 1, x=(0.06, 0.06), y=(0.06, 0.06)), pair=True)


def dropped(p):
    """The M4A4 parts our HK416 replaces (by position, measured on the M4A4: docs/shots/hk416_v0.39)."""
    if p.group.startswith(("ammo", "slide", "default")):
        return False
    v = p.verts()
    x0, x1 = min(q[0] for q in v), max(q[0] for q in v)
    y0, y1 = min(q[1] for q in v), max(q[1] for q in v)
    if x1 <= 0.05:                       # collapsible stock
        return True
    if p.name in ("gunModel7", "gunModel8", "gunModel9", "gunModel10", "gunModel33"):  # A2 grip
        return True
    if x0 >= 7.4 and y0 >= -4.25 and y1 <= -1.25 and x1 <= 16.7:   # quad rail, gas block, A-frame
        return True
    if x0 >= 14.6 or x1 > 16.7:          # front sight, barrel (12.15 .. 18.15), birdcage
        return True
    return False


def recolour_tan(img, islands):
    """The texture with every island in `islands` ((u, v, w, h) in texture units) turned flat dark
    earth. The M4A4's islands are darker than ours, so each island is first brought to one tone
    (its own mean) and only its texel gradation kept, then a small per island shade (+-6 %)."""
    import random
    rnd = random.Random(416)
    out = img.copy()
    k = img.width // 512
    for (u, v, w, h) in islands:
        px = [(xx, yy) for yy in range(v * k, min(img.height, (v + h) * k))
              for xx in range(u * k, min(img.width, (u + w) * k)) if img.getpixel((xx, yy))[3]]
        if not px:
            continue
        lums = [0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2] for c in (img.getpixel(q) for q in px)]
        mean = max(1.0, sum(lums) / len(lums))
        shade = 1 + rnd.uniform(-0.06, 0.06)
        for (xx, yy), lum in zip(px, lums):
            f = shade * max(0.85, min(1.15, lum / mean))
            out.putpixel((xx, yy), tuple(min(255, int(c * f)) for c in TAN) + (img.getpixel((xx, yy))[3],))
    return out


def black_part(name_or_part):
    """Parts that stay black on the tan gun: sights, barrel, muzzle, trigger, bolt (user photo 4)."""
    n = name_or_part
    return n.startswith(("defaultScope", "slideModel", "fs", "barrel", "flash", "trigger", "buttRib", "stockFin"))


def build():
    z = zipfile.ZipFile(os.path.join(ROOT, "Decimation.jar"))
    from PIL import Image
    text = z.read("assets/deci/models/guns/rifle/m4a4.bmodel").decode("latin1")
    tex = Image.open(BytesIO(z.read("assets/deci/textures/model/guns/rifle/m4a4/m4a4.png"))).convert("RGBA")
    m4 = study.Gun("m4a4", "rifle", text, tex, False)
    drop = {p.name for p in m4.parts if dropped(p)}
    import re
    pat = re.compile(r"\b(%s)\b" % "|".join(sorted(drop, key=len, reverse=True)))
    keep, th = [], m4.th
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("textureHeight"):
            continue
        if drop and pat.search(s):
            continue
        keep.append(line)
    # some M4A4 parts (the rear sight, defaultScopeModel3..15) read rows past its textureHeight 32
    # and wrap to the top in the game (GL_REPEAT); a taller texture would hand them our islands
    # (v0.39.0: rear sight ears cut into a block and a pillar, casebook case 14). Copy the wrapped
    # rows into place and start our islands below them.
    spill = max(p.v + int(round(p.size[2])) + int(round(p.size[1])) for p in m4.parts) - th
    spill = (spill + 7) // 8 * 8 if spill > 0 else 0
    v0 = th + spill
    uvs, h = layout(P)
    total = th
    while total < v0 + h:
        total *= 2
    keep = [l for l in keep if not l.strip().startswith("textureWidth")]
    header = ["  textureWidth = 512;", "  textureHeight = %d;" % total]
    first = next(i for i, l in enumerate(keep) if "= new " in l)
    keep = keep[:first] + header + keep[first:]
    for i, p in enumerate(P):
        u, v = uvs[i]
        keep += part_block("%s%d" % (G, 1000 + i), p, u, v + v0)
    model = "\n".join(keep) + "\n"
    # texture: the M4A4's on top, ours below
    k = tex.width // 512
    mine = paint(P)
    black = Image.new("RGBA", (tex.width, total * k), (0, 0, 0, 0))
    black.paste(tex, (0, 0))

    # the M4A4's rail teeth gunModel205..208 (x 2.1 .. 3.5, under its rear sight) carry a colourful
    # placeholder texture nobody saw while the sight covered them; it folds away under a sight now
    # (IronSights): give them the island of the clean tooth gunModel204 (u 425, v 25, 4 x 2 units)
    island = black.crop((425 * k, 25 * k, 429 * k, 27 * k))
    for u in (433, 441, 449, 457):
        black.paste(island, (u * k, 25 * k))
    if spill:
        black.paste(tex.crop((0, 0, tex.width, spill * k)), (0, th * k))
    for y, row in enumerate(mine):
        for x, c in enumerate(row):
            if c[3] and v0 * k + y < black.height:
                black.putpixel((x, v0 * k + y), c)
    # tan islands: kept M4A4 parts except sights / bolt / trigger, our parts except metal ones
    islands = []
    for p in m4.parts:
        if p.name in drop or black_part(p.name):
            continue
        v = p.verts()
        if 3.6 <= min(q[0] for q in v) and max(q[0] for q in v) <= 4.2 and min(q[1] for q in v) >= -0.85:
            continue  # the trigger
        w, hh, d = [int(round(s)) for s in p.size]
        islands.append((p.u, p.v, 2 * (d + w), d + hh))
    from gunmodel import size
    for i, p in enumerate(P):
        if black_part(p.name):
            continue
        u, v = uvs[i]
        w, hh, d = size(p)
        islands.append((u, v + v0, 2 * (d + w), d + hh))
    tan = recolour_tan(black, islands)
    out = {}
    for name, img in (("hk416", black), ("hk416tan", tan)):
        mp = os.path.join(OUT, "models", "guns", "rifle", name + ".bmodel")
        os.makedirs(os.path.dirname(mp), exist_ok=True)
        open(mp, "w").write(model)
        tp = os.path.join(OUT, "textures", "model", "guns", "rifle", name, name + ".png")
        os.makedirs(os.path.dirname(tp), exist_ok=True)
        img.save(tp)
        adir = os.path.join(OUT, "animations", name)
        os.makedirs(adir, exist_ok=True)
        for n in z.namelist():
            if n.startswith("assets/deci/animations/m4a4/") and n.endswith(".anib"):
                open(os.path.join(adir, os.path.basename(n).replace("m4a4", name)), "wb").write(z.read(n))
        icon(name, os.path.join(OUT, "textures", "items", "gun", "rifle", name + ".png"))
        out[name] = mp
    return {"dropped": len(drop), "ours": len(P), "texture": [512, total], "spill rows": spill, "files": out}


if __name__ == "__main__":
    print(build())
