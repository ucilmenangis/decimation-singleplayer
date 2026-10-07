#!/usr/bin/env python3
"""Push changed classes into a RUNNING dev client, no restart.

    (game started with)  cd dev && ./gradlew runClient -Photswap
    python3 tools/hotswap.py            # compile, then redefine every class changed since the last swap
    python3 tools/hotswap.py --all      # redefine every class of our mod

Then in game: /deciworldgen reload (furniture sets) and
/deciworldgen rebuild [radius] (rebuild the buildings around you).

Works for changes INSIDE method bodies (the JVM's HotSwap). Adding or
removing fields, methods or classes, or changing signatures, is refused by
the JVM: then restart the game. Uses jdb from the Java 8 JDK in ~/.jdks,
attached to the debug port 5005 opened by -Photswap (bound to 127.0.0.1 only:
Java 8 would otherwise listen on every interface, letting anyone on the
network run code in the game).
"""
import glob
import os
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEV = os.path.join(ROOT, "dev")
CLASSES = os.path.join(DEV, "build", "classes", "java", "main")
STAMP = os.path.join(ROOT, "build", ".hotswap_stamp")


def jdb():
    for p in sorted(glob.glob(os.path.expanduser("~/.jdks/*8*/Contents/Home/bin/jdb"))) + \
            sorted(glob.glob(os.path.expanduser("~/.jdks/*/Contents/Home/bin/jdb"))):
        return p
    return "jdb"


def main():
    print("compiling...")
    r = subprocess.run(["./gradlew", "-q", "compileJava"], cwd=DEV, capture_output=True, text=True)
    errors = [l for l in (r.stdout + r.stderr).splitlines() if "error" in l.lower()]
    if r.returncode != 0:
        print("\n".join(errors[:20]) or r.stderr[-2000:])
        sys.exit(1)
    since = 0 if "--all" in sys.argv or not os.path.isfile(STAMP) else os.path.getmtime(STAMP)
    changed = []
    for root, _, files in os.walk(os.path.join(CLASSES, "net", "decimation")):
        for f in files:
            p = os.path.join(root, f)
            if f.endswith(".class") and os.path.getmtime(p) > since:
                changed.append(p)
    if not changed:
        print("nothing changed since the last swap")
        return
    cmds = []
    for p in changed:
        name = os.path.relpath(p, CLASSES)[:-6].replace(os.sep, ".")
        cmds.append("redefine %s %s" % (name, p))
    cmds.append("exit")
    print("redefining %d class(es)..." % len(changed))
    proc = subprocess.Popen([jdb(), "-attach", "127.0.0.1:5005"], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    time.sleep(2)
    out, _ = proc.communicate("\n".join(cmds) + "\n", timeout=60)
    bad = [l for l in out.splitlines() if "not" in l.lower() or "error" in l.lower() or "exception" in l.lower()
           or "unsupported" in l.lower() or "fail" in l.lower()]
    if "Unable to attach" in out or "Connection refused" in out:
        print("no game listening on port 5005: start it with  cd dev && ./gradlew runClient -Photswap")
        sys.exit(1)
    if bad:
        print("some classes were refused (structural change or not loaded yet):")
        print("\n".join(bad[:20]))
        print("not loaded yet = fine (the new file is used when it loads); structural = restart the game")
    else:
        print("done: changed method code is live; in game run /deciworldgen rebuild")
    os.makedirs(os.path.dirname(STAMP), exist_ok=True)
    open(STAMP, "w").write(str(time.time()))


if __name__ == "__main__":
    main()
