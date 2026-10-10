#!/usr/bin/env python3
"""Run dev test modes in ONE game launch and print a compact result.

    python3 tools/devtest.py MODE [MODE ...] [-P<gradle property> ...] [--no-sheets]

MODE: checks (server checks: zones, vehicle, humanity, bottlecaps, armor,
helmet, supply drop; fresh seed 1 world), views (camera street views; or
with -Paudit / -Pgallery / ...), scope, tracer, props, cityfps (these reuse
the last autotest world). Example:

    python3 tools/devtest.py checks scope tracer

LIVE (no game launch per run, docs/roadmap.md "Live dev test mode"):

    python3 tools/devtest.py --live MODE [MODE ...]   # starts the game once if needed
    python3 tools/devtest.py --live --swap MODE ...   # first push changed code (tools/hotswap.py)
    python3 tools/devtest.py --stop                   # close the live game
    python3 tools/devtest.py --live --angelica MODE   # start the live game with Angelica (dev only)
    python3 tools/devtest.py --live --java25 MODE     # start it on Java 25 with lwjgl3ify (runClient25)

--live sends the modes to a game started with `gradlew runClient -Plive`
(devtest/DevTestLive, 127.0.0.1:25599); if none answers it starts one in the
background (log build/live.log) and waits until it is ready. Runs happen
in the world that is open; -Pkey=value becomes a system property for that
run. Method body changes reach the open game with --swap; new classes,
fields or methods need --stop and a new start.

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


PORT = 25599
LIVE_LOG = os.path.join(ROOT, "build", "live.log")


def live_send(line, timeout=1800):
    """One command to the live game; its reply lines (up to END), or None when nobody listens."""
    import socket
    try:
        s = socket.create_connection(("127.0.0.1", PORT), timeout=3)
    except OSError:
        return None
    s.settimeout(timeout)
    with s:
        s.sendall((line + "\n").encode())
        data = b""
        while True:
            chunk = s.recv(65536)
            if not chunk:
                break
            data += chunk
            if data.endswith(b"END\n"):
                break
    return data.decode(errors="replace").splitlines()


def live_ready(wait):
    """True once the live game answers ping with ready (waits up to `wait` seconds)."""
    end = time.time() + wait
    while True:
        r = live_send("ping", 5)
        if r and r[0] == "ready":
            return True
        if time.time() > end:
            return False
        time.sleep(2)


def run_live(modes, extra, swap, angelica=False, task="runClient"):
    if live_send("ping", 5) is None:
        print("no live game: starting one (log %s)..." % os.path.relpath(LIVE_LOG, ROOT))
        os.makedirs(os.path.dirname(LIVE_LOG), exist_ok=True)
        start = ["./gradlew", task, "-q", "-Plive"]
        if angelica:  # Angelica rendering overhaul in the dev client (docs/performance.md)
            start += ["-Pangelica", "-PforceEnableMixins=true"]
        subprocess.Popen(start, cwd=DEV, stdout=open(LIVE_LOG, "w"),
                         stderr=subprocess.STDOUT, start_new_session=True)
        t0 = time.time()
        if not live_ready(600):
            print("the live game did not come up (see the log)")
            return False
        print("live game ready after %.0f s" % (time.time() - t0))
    if swap:
        r = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "hotswap.py")], capture_output=True, text=True)
        print("hotswap:", (r.stdout + r.stderr).strip().splitlines()[-1:] or ["?"])
    if not live_ready(1800):
        print("the live game stays busy")
        return False
    props = []
    for e in extra:  # -Pkey=value / -Pflag -> key=value / flag=true for this run
        kv = e[2:]
        props.append(kv if "=" in kv else kv + "=true")
    if os.path.isfile(RESULTS):
        os.remove(RESULTS)
    t0 = time.time()
    reply = live_send("run " + " ".join(modes + props))
    print("ran %s live in %.0f s" % (" ".join(modes), time.time() - t0))
    return reply is not None


def main():
    args = sys.argv[1:]
    modes = [a for a in args if not a.startswith("-")]
    extra = [a for a in args if a.startswith("-P")]
    if "--stop" in args:
        print(live_send("quit", 10) or "no live game")
        return 0
    if not modes:
        print(__doc__)
        return 2
    if "--live" in args:
        task = "runClient25" if "--java25" in args else "runClient"  # Java 25 + lwjgl3ify (docs/performance.md)
        if not run_live(modes, extra, "--swap" in args, "--angelica" in args, task):
            return 1
    else:
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
