#!/usr/bin/env python3
"""Rebuild known.txt for every built-in asset folder of the mod.

    python3 tools/asset_hashes.py

known.txt (in dev/src/main/resources/assets/deciworldgen/<kind>/) lists the
sha1 of every version of every built-in asset that was ever committed, plus
the working copy. AssetDir.init replaces a config copy matching one of them
(never edited by the user) with the current built-in, so improved sets reach
worlds that already have copies. Run it after changing any built-in asset,
before committing (a version missing here is treated as user edited).
"""
import hashlib
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "dev/src/main/resources/assets/deciworldgen"


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, check=True).stdout


def main():
    for kind in sorted(os.listdir(os.path.join(ROOT, BASE))):
        folder = os.path.join(ROOT, BASE, kind)
        if not os.path.isfile(os.path.join(folder, "index.txt")):
            continue
        hashes = set()
        # every committed path this folder ever had, renamed files included
        paths = set()
        for line in git("log", "--all", "--name-only", "--pretty=format:", "--", BASE + "/" + kind).decode().split():
            if line.endswith(".json"):
                paths.add(line)
        for path in paths:
            for rev in git("rev-list", "--all", "--", path).decode().split():
                try:
                    hashes.add(hashlib.sha1(git("show", "%s:%s" % (rev, path))).hexdigest())
                except subprocess.CalledProcessError:
                    pass  # deleted in that revision
        for f in os.listdir(folder):
            if f.endswith(".json"):
                hashes.add(hashlib.sha1(open(os.path.join(folder, f), "rb").read()).hexdigest())
        with open(os.path.join(folder, "known.txt"), "w") as out:
            out.write("\n".join(sorted(hashes)) + "\n")
        print("%s: %d known versions" % (kind, len(hashes)))


if __name__ == "__main__":
    main()
