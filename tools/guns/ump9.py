#!/usr/bin/env python3
"""UMP9 (HK UMP, 9 mm, curved 30 round magazine): Decimation's own UMP45 with our magazine.

User request (10 Oktober 2026): "mostly same like ump45, take from 45acp asset then replace
the mag with new model 9mm". The UMP9 and UMP45 share the receiver; the 9 mm magazine is
curved (like the MP5 / AK ones) where the .45 one is straight.

Nothing of Decimation goes into the repo (public): this script reads the UMP45 model,
texture and animations from the user's own Decimation.jar at build time and writes the UMP9
assets into dev/src/main/resources/assets/deci (those paths are git ignored, local only, like
the converted city pack). Only the magazine below is our own work.

    python3 tools/guns/ump9.py      # needs Decimation.jar in the project root

What it does:
- model: ump45.bmodel without its magazine (ammoModel0 / 1 and their addChild), textureHeight
  doubled, our magazine parts appended as ammoModel0..N with UV islands in the new lower half;
- texture: ump45.png with our magazine islands painted below (gunmodel.paint style, the UMP45
  magazine's tone 39);
- animations: ump45's .anib files under the UMP9 names (their magazine pose moves every
  part named ammoModel*, so ours follows);
- icon: rendered from the result (gunmodel.icon).
fixes/NewGuns registers the gun only when the model exists (a clone without the generated
assets still starts).

Magazine (anchored on the UMP45's: its top face (7.41, -1.18) rear to (8.58, -1.43) front, 1.17
deep, so it seats in the same well): 0.75 wide (9 mm), 6 skewed segments curving forward 1.92
over 4.43 down (user's side photo: about 0.45 forward per unit down, growing toward the base),
a base plate, smoked window strips on both sides of the middle segments (user photo 1).
"""
import os
import random
import sys
import zipfile
from io import BytesIO

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gunmodel import ZC, layout, paint, part, part_block, icon  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "dev", "src", "main", "resources", "assets", "deci")
A = "ammoModel"
MAG = (38, 38, 40)
BASE = (30, 30, 32)
WINDOW = (52, 52, 56)   # smoked round count window

# ---------------------------------------------------------------- our magazine
TOP_FRONT, TOP_Y, TOP_REAR_DROP, DEPTH = 8.58, -1.43, 0.25, 1.17
Z0, Z1 = ZC - 0.375, ZC + 0.375
BOTTOM = 3.0
FORWARD = [0, 0.22, 0.48, 0.78, 1.12, 1.5, 1.92]   # front edge shift at each segment boundary
SEG = (BOTTOM - TOP_Y) / 6

P = []
for i in range(6):
    y0, y1 = TOP_Y + i * SEG, TOP_Y + (i + 1) * SEG
    f0, f1 = TOP_FRONT + FORWARD[i], TOP_FRONT + FORWARD[i + 1]
    s = part("mag%d" % i, A, MAG, (f0 - DEPTH, y0, Z0), (f0, y1, Z1)).shift("y", 1, x=f1 - f0)
    if i == 0:
        s.inset("x", 0, y=(TOP_REAR_DROP, 0))   # the slanted top like the UMP45's
    P.append(s)
    if 1 <= i <= 4:
        w = part("window%d" % i, A, WINDOW, (f0 - DEPTH + 0.35, y0 + 0.05, Z1), (f0 - 0.3, y1 - 0.05, Z1 + 0.05))
        w.shift("y", 1, x=f1 - f0).inset("z", 1, x=(0.03, 0.03))
        P.append(w)
        P.append(w.mirror("window%dR" % i))
fb = TOP_FRONT + FORWARD[6]
base = part("magBase", A, BASE, (fb - DEPTH - 0.08, BOTTOM, Z0 - 0.06), (fb + 0.08, BOTTOM + 0.16, Z1 + 0.06))
P.append(base.shift("y", 1, x=0.06).inset("y", 1, x=(0.05, 0.05), z=(0.05, 0.05)))


def build():
    jar = os.path.join(ROOT, "Decimation.jar")
    z = zipfile.ZipFile(jar)
    text = z.read("assets/deci/models/guns/smg/ump45.bmodel").decode("latin1")
    from PIL import Image
    tex = Image.open(BytesIO(z.read("assets/deci/textures/model/guns/smg/ump45/ump45.png"))).convert("RGBA")
    # the UMP45 without its magazine
    keep, th = [], None
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("textureHeight"):
            th = int(s.split("=")[1].strip(" ;"))
            keep.append("  textureHeight = %d;" % (th * 2))
            continue
        if "ammoModel" in s:
            continue
        keep.append(line)
    # our magazine islands in the new lower half
    uvs, h = layout(P)
    assert h <= th, "magazine islands do not fit"
    for i, p in enumerate(P):
        u, v = uvs[i]
        keep += part_block("%s%d" % (A, i), p, u, v + th)
    model = os.path.join(OUT, "models", "guns", "smg", "ump9.bmodel")
    os.makedirs(os.path.dirname(model), exist_ok=True)
    with open(model, "w") as f:
        f.write("\n".join(keep) + "\n")
    # texture: UMP45's on top, ours below (2 px a unit like Decimation's)
    k = tex.width // 512
    mine = paint(P)
    out = Image.new("RGBA", (tex.width, tex.height * 2), (0, 0, 0, 0))
    out.paste(tex, (0, 0))
    for y, row in enumerate(mine):
        for x, c in enumerate(row):
            if c[3] and x < out.width and tex.height + y < out.height:
                out.putpixel((x, tex.height + y), c)
    tpath = os.path.join(OUT, "textures", "model", "guns", "smg", "ump9", "ump9.png")
    os.makedirs(os.path.dirname(tpath), exist_ok=True)
    out.save(tpath)
    # animations under our name
    adir = os.path.join(OUT, "animations", "ump9")
    os.makedirs(adir, exist_ok=True)
    for n in z.namelist():
        if n.startswith("assets/deci/animations/ump45/") and n.endswith(".anib"):
            with open(os.path.join(adir, os.path.basename(n).replace("ump45", "ump9")), "wb") as f:
                f.write(z.read(n))
    icon("ump9", os.path.join(OUT, "textures", "items", "gun", "smg", "ump9.png"))
    return {"magazine parts": len(P), "texture": [512, th * 2], "scale": k, "model": model}


if __name__ == "__main__":
    print(build())
