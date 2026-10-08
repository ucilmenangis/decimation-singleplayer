#!/usr/bin/env python3
"""Run dev test modes in ONE game launch and print a compact result.

    python3 tools/devtest.py MODE [MODE ...] [-P<gradle property> ...] [--no-sheets]

MODE: checks (server checks: zones, vehicle, humanity, bottlecaps, armor,
helmet, supply drop; fresh seed 1 world), views (camera street views; or
with -Paudit / -Pgallery / ...), scope, tracer, props, cityfps (these reuse
the last autotest world). Example:

    python3 tools/devtest.py checks scope tracer

Runs `./gradlew runClient -Pdevtest=checks,scope,tracer` in dev/, then prints
run/client/devtest/results.txt (one line per value, PASS / FAIL with the
expectation) and builds one contact sheet per mode from the screenshots the
run recorded: dev/run/client/devtest/sheet_<mode>.png (read one image per
mode instead of every screenshot). Exit code 1 when any check FAILs or the
run wrote no results (crash: see the newest dev/run/client/crash-reports).
"""
import os
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEV = os.path.join(ROOT, "dev")
RUN = os.path.join(DEV, "run", "client")
RESULTS = os.path.join(RUN, "devtest", "results.txt")


def sheets(shots):
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("(no PIL: contact sheets skipped; run with a python that has pillow)")
        return
    for mode, files in shots.items():
        ims = []
        for f in files:
            p = os.path.join(RUN, "screenshots", f)
            if os.path.isfile(p):
                ims.append((f, Image.open(p).convert("RGB")))
        if not ims:
            continue
        cols = 3 if len(ims) > 4 else 2 if len(ims) > 1 else 1
        w, h = 640, 360
        rows = (len(ims) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * w, rows * (h + 18)), "white")
        d = ImageDraw.Draw(sheet)
        for i, (name, im) in enumerate(ims):
            x, y = (i % cols) * w, (i // cols) * (h + 18)
            im.thumbnail((w, h))
            sheet.paste(im, (x, y + 18))
            d.text((x + 4, y + 3), name, fill="black")
        out = os.path.join(RUN, "devtest", "sheet_%s.png" % mode)
        sheet.save(out)
        print("sheet %-8s %s (%d shots)" % (mode, os.path.relpath(out, ROOT), len(ims)))


def main():
    args = sys.argv[1:]
    modes = [a for a in args if not a.startswith("-")]
    extra = [a for a in args if a.startswith("-P")]
    if not modes:
        print(__doc__)
        return 2
    if os.path.isfile(RESULTS):
        os.remove(RESULTS)
    cmd = ["./gradlew", "runClient", "-q", "-Pdevtest=" + ",".join(modes)] + extra
    t0 = time.time()
    proc = subprocess.run(cmd, cwd=DEV, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    print("ran %s in %.0f s" % (" ".join(modes), time.time() - t0))
    errors = [l for l in proc.stdout.splitlines() if "error:" in l or "FAILED" in l]
    if proc.returncode != 0 and errors:
        print("BUILD FAILED:")
        for l in errors[:10]:
            print("  " + l.strip())
        return 1
    if not os.path.isfile(RESULTS):
        print("NO RESULTS (crash?): newest crash report:")
        crash = os.path.join(RUN, "crash-reports")
        if os.path.isdir(crash) and os.listdir(crash):
            print("  " + max((os.path.join(crash, f) for f in os.listdir(crash)), key=os.path.getmtime))
        return 1
    shots, fails = {}, 0
    for line in open(RESULTS):
        line = line.rstrip("\n")
        parts = line.split()
        if len(parts) >= 3 and parts[1] == "shot":
            shots.setdefault(parts[0], []).append(parts[2])
            continue
        if " FAIL " in line:
            fails += 1
        print(line)
    if "--no-sheets" not in args:
        sheets(shots)
    print("%d FAIL" % fails if fails else "all checks PASS")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
