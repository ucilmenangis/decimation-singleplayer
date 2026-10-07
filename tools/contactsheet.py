#!/usr/bin/env python3
"""Pack autotest screenshots into labelled contact sheets (2 x 3 per sheet).

    python tools/contactsheet.py PREFIX OUT_DIR [LOG]

PREFIX: screenshot prefix in dev/run/client/screenshots (study_, flat_, ...).
Copies the shots to OUT_DIR and writes OUT_DIR/sheet_<n>.png plus
views.txt; labels come from the "AUTOTEST view N at ..." lines of LOG
(default dev/run/client/logs/fml-client-latest.log). Needs pillow.
"""
import os
import re
import shutil
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "dev/run/client/screenshots")


def main():
    prefix, out = sys.argv[1], sys.argv[2]
    log = sys.argv[3] if len(sys.argv) > 3 else os.path.join(ROOT, "dev/run/client/logs/fml-client-latest.log")
    os.makedirs(out, exist_ok=True)
    labels = {}
    for line in open(log, errors="replace"):
        m = re.search(r"AUTOTEST view (\d+) at (.*?): ", line)
        if m:
            labels[int(m.group(1))] = m.group(2)
    shots = sorted((int(f[len(prefix):-4]), f) for f in os.listdir(SHOTS)
                   if f.startswith(prefix) and f.endswith(".png"))
    with open(os.path.join(out, "views.txt"), "w") as f:
        for n, name in shots:
            shutil.copy(os.path.join(SHOTS, name), os.path.join(out, name))
            f.write("%d\t%s\n" % (n, labels.get(n, "")))
    w, h = 640, 232
    for s in range(0, len(shots), 6):
        sheet = Image.new("RGB", (2 * w + 1, 3 * (h + 18)), (255, 255, 255))
        d = ImageDraw.Draw(sheet)
        for i, (n, name) in enumerate(shots[s:s + 6]):
            img = Image.open(os.path.join(SHOTS, name)).convert("RGB")
            img = img.resize((w, int(img.height * w / img.width)))
            top = (img.height - h) // 2
            img = img.crop((0, top, w, top + h))
            x, y = (i % 2) * (w + 1), (i // 2) * (h + 18)
            d.text((x + 4, y + 3), "view %d at %s" % (n, labels.get(n, "?")), fill=(0, 0, 0))
            sheet.paste(img, (x, y + 18))
        sheet.save(os.path.join(out, "sheet_%d.png" % (s // 6)))
    print("%d shots, %d sheets in %s" % (len(shots), (len(shots) + 5) // 6, out))


if __name__ == "__main__":
    main()
