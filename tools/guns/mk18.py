#!/usr/bin/env python3
"""Mk18 Mod 1 (CQBR, black): Decimation's own M4A4 with our Daniel Defense RIS II rail, flip up
front sight, short barrel and flash hider.

User request (10 Oktober 2026): "mk18 mod 1, black only", 9 photos (side views of Mod 1 builds,
three quarter, tan / black rails, one with an EOTech); "if possible dont put iron and rear sight
so its ready to be place an attachment, and if possible make the front and rear sight disappear
when attaching attachment" (fixes/IronSights: parts of the defaultScope group are hidden while
a sight is attached; the M4A4's own folding rear sight is already in that group, our front sight
joins it, so with no sight the irons are there to aim with, with one they are gone).
Variant route (skill lesson 13, template tools/guns/hk416.py): the M4A4 model, texture and
animations are read from the user's Decimation.jar at build time, outputs git ignored.

Kept from the M4A4: receiver, trigger group, A2 grip (as in the photos), magazine, stock (the
M4A4's collapsible stock; the photos show SOPMOD / Crane stocks of a similar outline), rear sight
(defaultScope), hands.
Replaced: the quad rail, gas block and A-frame front sight, the barrel and birdcage.
Lengths: on user photo 2 (black, side) the rail is 1.25 x the upper receiver and only the flash
hider stands out past the rail (0.196 of the rail length). Decimation draws the M4A4's front
part shorter than real (its 7 inch handguard spans 7.5 .. 14.7 on a receiver drawn 7.45 long);
the same squeeze on the Mk18's 9.5 inch rail gives 7.5 .. 14.7 again, the flash hider to 16.1
(M4A4 muzzle 19.15): the Mk18 is visibly shorter, the rail runs almost to the muzzle as on the
real gun.
- DD RIS II rail x 7.5 to 14.7, top rail flush with the receiver's (y -3.6, like the HK416),
  side and bottom rails with teeth, rows of round holes in the angled panels between the rails,
  front ring, cross bolts;
- flip up front sight on the rail's front end (defaultScope group), post tip -4.7 like the M4A4;
- barrel x 14.7 .. 14.9 on the M4A4 bore (-2.39), flash hider 14.9 .. 16.1;
- flamePos x 16.1 (Decimation's M4A4 puts it at the muzzle tip, y -3.4).

    python3 tools/guns/mk18.py      # needs Decimation.jar in the project root
"""
import os
import re
import sys
import zipfile
from io import BytesIO

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import ZC, layout, octagon, paint, part, part_block, icon  # noqa: E402
import study  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deci")
NAME = "mk18"
G = "gunModel"
SIGHT = "defaultScopeModel"   # hidden while a sight is attached (fixes/IronSights)
RAIL = (31, 31, 33)     # anodised aluminium rail, as dark as the M4A4 receiver
DARK = (24, 24, 26)     # holes, slots, sights
METAL = (46, 46, 48)    # barrel
FLASH = (36, 36, 38)
BOLT = (52, 52, 54)
BORE = -2.39            # the M4A4's bore centre
MUZZLE = 16.1

P = []


def add(*parts, pair=False):
    for p in parts:
        P.append(p)
        if pair:
            P.append(p.mirror())


# ---------------------------------------------------------------- DD RIS II rail (x 7.5 .. 14.7)
X0, X1 = 7.5, 14.7
add(part("hgUpper", G, RAIL, (X0, -3.38, -0.85), (X1, -2.5, 0.55)).inset("y", 0, z=(0.15, 0.15)))
add(part("hgLower", G, RAIL, (X0, -2.5, -0.85), (X1, -1.55, 0.55)).inset("y", 1, z=(0.15, 0.15)))
add(part("hgRing", G, RAIL, (X1 - 0.15, -3.36, -0.82), (X1 + 0.05, -1.58, 0.52)).inset("x", 1, y=(0.1, 0.1), z=(0.1, 0.1))
    .inset("y", 0, z=(0.12, 0.12)))
add(part("railTopBase", G, RAIL, (X0, -3.5, -0.5), (X1, -3.38, 0.2)))
x = X0 + 0.2
while x + 0.2 <= X1:
    add(part("railTopTooth", G, RAIL, (x, -3.6, -0.5), (x + 0.2, -3.5, 0.2)).inset("y", 0, x=(0.02, 0.02)))
    x += 0.4
add(part("railBottomBase", G, RAIL, (X0 + 0.1, -1.55, -0.5), (X1 - 0.1, -1.42, 0.2)))
x = X0 + 0.3
while x + 0.2 <= X1 - 0.1:
    add(part("railBottomTooth", G, RAIL, (x, -1.42, -0.5), (x + 0.2, -1.32, 0.2)).inset("y", 1, x=(0.02, 0.02)))
    x += 0.4
# side rail (photo 2): one tall rail in the middle of each side with cross teeth, screw heads
# on it, a round bolt head at its front end; its inner side inside the leaning panels
add(part("railSideBase", G, RAIL, (X0 + 0.1, -2.95, 0.4), (X1 - 0.12, -1.95, 0.6)), pair=True)
x = X0 + 0.22
while x + 0.24 <= X1 - 0.3:
    add(part("railSideTooth", G, RAIL, (x, -2.95, 0.6), (x + 0.24, -1.95, 0.7)).inset("z", 1, y=(0.03, 0.03)), pair=True)
    x += 0.4
for bx in (8.45, 11.25, 13.45):
    add(part("railScrew", G, BOLT, (bx, -2.57, 0.6), (bx + 0.22, -2.33, 0.76)).inset("z", 1, x=(0.05, 0.05), y=(0.05, 0.05)), pair=True)
add(part("frontBolt", G, BOLT, (X1 - 0.42, -2.62, 0.6), (X1 - 0.14, -2.28, 0.74)).inset("z", 1, x=(0.06, 0.06), y=(0.06, 0.06)), pair=True)
# round holes (photos 2, 7): two staggered rows in the upper angled panel, one row of small ones
# in the lower panel; dark plates set into the leaning panels (inner faces inside, lesson 15)
x = X0 + 0.35
i = 0
while x + 0.2 <= X1 - 0.3:
    add(part("holeTop", G, DARK, (x - 0.02, -3.34, 0.3), (x + 0.18, -3.19, 0.47)).inset("z", 1, x=(0.03, 0.03), y=(0.03, 0.03)), pair=True)
    xs = x + 0.2
    if xs + 0.2 <= X1 - 0.3:
        add(part("holeMid", G, DARK, (xs - 0.02, -3.16, 0.32), (xs + 0.24, -2.97, 0.52)).inset("z", 1, x=(0.04, 0.04), y=(0.04, 0.04)), pair=True)
    if i % 2 == 0:
        add(part("holeLow", G, DARK, (x + 0.08, -1.9, 0.32), (x + 0.26, -1.75, 0.5)).inset("z", 1, x=(0.03, 0.03), y=(0.03, 0.03)), pair=True)
    x += 0.4
    i += 1
# QD sling cup under the front of the rail (photo 2, small round block)
add(part("slingCup", G, DARK, (X1 - 0.9, -1.32, -0.3), (X1 - 0.5, -1.15, 0.0)).inset("y", 1, x=(0.05, 0.05), z=(0.05, 0.05)))

# ---------------------------------------------------------------- flip up front sight (defaultScope)
# on the rail's front end, like the HK's U (skill case 14): bridge, thick flared ears stopping just
# over the post, post tip at -4.7 like the M4A4's (same sight picture as its rear sight)
add(part("fsClamp", SIGHT, DARK, (13.95, -3.85, -0.48), (14.5, -3.6, 0.18)).inset("y", 0, x=(0.06, 0.06), z=(0.05, 0.05)))
add(part("fsTower", SIGHT, DARK, (14.05, -4.45, -0.42), (14.4, -3.85, 0.12)).inset("y", 0, x=(0.05, 0.05), z=(0.02, 0.02)))
add(part("fsBridge", SIGHT, DARK, (14.05, -4.58, ZC - 0.3), (14.4, -4.45, ZC + 0.3)).inset("y", 0, x=(0.02, 0.02)))
add(part("fsEar", SIGHT, DARK, (14.07, -4.74, ZC + 0.14), (14.37, -4.58, ZC + 0.3)).inset("y", 0, x=(0.05, 0.05), z=(0.0, 0.06)), pair=True)
add(part("fsPost", SIGHT, DARK, (14.17, -4.7, -0.21), (14.27, -4.58, -0.09)).inset("y", 0, x=(0.02, 0.02), z=(0.02, 0.02)))
add(part("fsPostBase", SIGHT, DARK, (14.13, -4.62, ZC - 0.09), (14.31, -4.58, ZC + 0.09)))
add(part("fsHinge", SIGHT, DARK, (13.97, -4.0, -0.4), (14.05, -3.85, 0.1)))

# ---------------------------------------------------------------- barrel and flash hider (bore -2.39)
add(*octagon("barrel", G, METAL, X1 - 0.4, 14.9, BORE, ZC, 0.2))
add(*octagon("flashBody", G, FLASH, 14.9, 15.45, BORE, ZC, 0.27))
add(*octagon("flashCrown", G, FLASH, 15.45, MUZZLE, BORE, ZC, 0.3))
add(part("flashRing", G, FLASH, (15.4, BORE - 0.31, ZC - 0.31), (15.5, BORE + 0.31, ZC + 0.31)).inset("x", 0, y=(0.06, 0.06), z=(0.06, 0.06)))
for i, dy in enumerate((-0.3, 0.3)):
    add(part("flashSlot%d" % i, G, DARK, (15.55, BORE + dy - 0.03, ZC - 0.12), (16.05, BORE + dy + 0.03, ZC + 0.12)))
add(part("flashSlotSide", G, DARK, (15.55, BORE - 0.12, ZC + 0.27), (16.05, BORE + 0.12, ZC + 0.31)), pair=True)


def dropped(p):
    """The M4A4 parts our Mk18 replaces (by position, measured on the M4A4: docs/shots/hk416_v0.39)."""
    if p.group.startswith(("ammo", "slide", "default")):
        return False
    v = p.verts()
    x0, x1 = min(q[0] for q in v), max(q[0] for q in v)
    y0, y1 = min(q[1] for q in v), max(q[1] for q in v)
    if x0 >= 7.4 and y0 >= -4.25 and y1 <= -1.25 and x1 <= 16.7:   # quad rail, gas block, A-frame
        return True
    if x0 >= 14.6 or x1 > 16.7:          # front sight, barrel (12.15 .. 18.15), birdcage
        return True
    return False


def build():
    z = zipfile.ZipFile(os.path.join(ROOT, "Decimation.jar"))
    from PIL import Image
    text = z.read("assets/deci/models/guns/rifle/m4a4.bmodel").decode("latin1")
    tex = Image.open(BytesIO(z.read("assets/deci/textures/model/guns/rifle/m4a4/m4a4.png"))).convert("RGBA")
    m4 = study.Gun("m4a4", "rifle", text, tex, False)
    drop = {p.name for p in m4.parts if dropped(p)}
    pat = re.compile(r"\b(%s)\b" % "|".join(sorted(drop, key=len, reverse=True)))
    keep, th = [], m4.th
    for line in text.splitlines():
        s = line.strip()
        if s.startswith(("textureHeight", "textureWidth")):
            continue
        if s.startswith("flamePos:"):
            line = "  flamePos: %s, -3.400001, -0.15;" % MUZZLE   # the M4A4's: at the muzzle tip
        if drop and pat.search(s):
            continue
        keep.append(line)
    # the M4A4's rear sight reads texture rows past its textureHeight (skill lesson 16): copy the
    # wrapped rows into place, our islands start below them
    spill = max(p.v + int(round(p.size[2])) + int(round(p.size[1])) for p in m4.parts) - th
    spill = (spill + 7) // 8 * 8 if spill > 0 else 0
    v0 = th + spill
    uvs, h = layout(P)
    total = th
    while total < v0 + h:
        total *= 2
    header = ["  textureWidth = 512;", "  textureHeight = %d;" % total]
    first = next(i for i, l in enumerate(keep) if "= new " in l)
    keep = keep[:first] + header + keep[first:]
    for i, p in enumerate(P):
        u, v = uvs[i]
        keep += part_block("%s%d" % (p.group, 1000 + i), p, u, v + v0)
    model = "\n".join(keep) + "\n"
    k = tex.width // 512
    mine = paint(P)
    img = Image.new("RGBA", (tex.width, total * k), (0, 0, 0, 0))
    img.paste(tex, (0, 0))
    if spill:
        img.paste(tex.crop((0, 0, tex.width, spill * k)), (0, th * k))
    for y, row in enumerate(mine):
        for x, c in enumerate(row):
            if c[3] and v0 * k + y < img.height:
                img.putpixel((x, v0 * k + y), c)
    mp = os.path.join(OUT, "models", "guns", "rifle", NAME + ".bmodel")
    os.makedirs(os.path.dirname(mp), exist_ok=True)
    open(mp, "w").write(model)
    tp = os.path.join(OUT, "textures", "model", "guns", "rifle", NAME, NAME + ".png")
    os.makedirs(os.path.dirname(tp), exist_ok=True)
    img.save(tp)
    adir = os.path.join(OUT, "animations", NAME)
    os.makedirs(adir, exist_ok=True)
    for n in z.namelist():
        if n.startswith("assets/deci/animations/m4a4/") and n.endswith(".anib"):
            open(os.path.join(adir, os.path.basename(n).replace("m4a4", NAME)), "wb").write(z.read(n))
    icon(NAME, os.path.join(OUT, "textures", "items", "gun", "rifle", NAME + ".png"))
    return {"dropped": len(drop), "ours": len(P), "texture": [512, total], "spill rows": spill, "model": mp}


if __name__ == "__main__":
    print(build())
