#!/usr/bin/env python3
"""Measure the drawn footprint of every Decimation prop from the autotest
footprint shots (./gradlew runClient -Pautotest -Pfootprint in dev/).

    python3 tools/footprint.py [SHOTS_DIR] [LOG] > props.tsv

One cell on white wool with 4 black wool markers at +-4 blocks, camera
straight down from 40 blocks (DevAutoTest.serveFootprintView). View 0 =
empty, view n = one prop at meta 3 (front south). The markers give the
pixel -> world mapping; the difference to view 0 gives the model's
outline. Prints how far each model reaches past its own block on each
side, in blocks (0 = inside the block), and the cells it covers (an
overhang under 0.3 counts as inside: parallax and outline blur). Needs
numpy + pillow. Heights are not measured (top down view).
"""
import os
import re
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
X0, Z0 = 6000, 6000
NOISE = 0.3


def marker_centroids(img):
    dark = img.sum(axis=2) < 400  # black wool shows dark grey at full gamma
    h, w = dark.shape
    out = []
    for qy in (slice(0, h // 2), slice(h // 2, h)):
        for qx in (slice(0, w // 2), slice(w // 2, w)):
            ys, xs = np.nonzero(dark[qy, qx])
            if len(xs) < 10:
                return None
            out.append((xs.mean() + qx.start, ys.mean() + qy.start))
    return out


def main():
    shots = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "dev/run/client/screenshots")
    log = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "dev/run/client/logs/fml-client-latest.log")
    names = {}
    for line in open(log, errors="ignore"):
        m = re.search(r"AUTOTEST view (\d+) at footprint (\S+): Saved", line)
        if m:
            names[int(m.group(1))] = m.group(2)
    empty = np.asarray(Image.open(os.path.join(shots, "footprint_0.png")).convert("RGB")).astype(int)
    marks = marker_centroids(empty)
    if marks is None:
        sys.exit("markers not found in footprint_0.png")
    # markers: (x0-4, z0-4) (x0+4, z0-4) / (x0-4, z0+4) (x0+4, z0+4), block centres
    xs = sorted(m[0] for m in marks)
    zs = sorted(m[1] for m in marks)
    ax = 8.0 / ((xs[2] + xs[3]) / 2 - (xs[0] + xs[1]) / 2)
    az = 8.0 / ((zs[2] + zs[3]) / 2 - (zs[0] + zs[1]) / 2)
    bx = X0 - 4 + 0.5 - (xs[0] + xs[1]) / 2 * ax
    bz = Z0 - 4 + 0.5 - (zs[0] + zs[1]) / 2 * az
    print("prop\twest\teast\tnorth\tsouth\tcells_x\tcells_z")
    for v in sorted(names):
        if v == 0:
            continue
        name = names[v]
        f = os.path.join(shots, "footprint_%d.png" % v)
        if not os.path.exists(f):
            continue
        img = np.asarray(Image.open(f).convert("RGB")).astype(int)
        diff = np.abs(img - empty).sum(axis=2) > 45
        ys, xs_ = np.nonzero(diff)
        if len(xs_) < 5:
            print("%s\t-\t-\t-\t-\t0\t0" % name[5:] if name.startswith("deci:") else name)
            continue
        wx = xs_ * ax + bx
        wz = ys * az + bz
        # ignore stray pixels far away (a falling item, a particle)
        keep = (np.abs(wx - (X0 + 0.5)) < 4) & (np.abs(wz - (Z0 + 0.5)) < 4)
        wx, wz = wx[keep], wz[keep]
        if len(wx) < 5:
            print("%s\t-\t-\t-\t-\t0\t0" % name[5:])
            continue
        west = max(0.0, X0 - np.percentile(wx, 0.5))
        east = max(0.0, np.percentile(wx, 99.5) - (X0 + 1))
        north = max(0.0, Z0 - np.percentile(wz, 0.5))
        south = max(0.0, np.percentile(wz, 99.5) - (Z0 + 1))

        def cells(v):
            return int(np.ceil(v - NOISE)) if v > NOISE else 0

        print("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%d\t%d" % (name[5:] if name.startswith("deci:") else name, west, east,
                                                     north, south, 1 + cells(west) + cells(east),
                                                     1 + cells(north) + cells(south)))


if __name__ == "__main__":
    main()
