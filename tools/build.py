#!/usr/bin/env python3
"""Compile src/ and inject the result into Decimation.jar.patched.

    python3 tools/build.py            compile + inject + verify
    python3 tools/build.py --compile  compile only (no jar touched)

Classpath is: shim, SRG Minecraft, SRG Forge, and Decimation.jar itself - the
last one so new code can call the mod's own (obfuscated) classes directly,
e.g. the zone system or the loot registry.

This UPDATES the existing Decimation.jar.patched in place rather than
rebuilding it from the backup, so the Javassist patches already applied to it
(loot handler, proxy cast, weapon nerf, armor buff, intro skip, ammo crate)
are preserved. The backup hash is checked before and after; it must not move.
"""
import hashlib
import os
import shutil
import subprocess
import sys
import zipfile

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIB = os.path.join(PROJ, "tools", "lib")
SRC = os.path.join(PROJ, "src")
OUT = os.path.join(PROJ, "build", "classes")
JAR = os.path.join(PROJ, "Decimation.jar.patched")
BACKUP = os.path.join(PROJ, "Decimation.jar.original.bak")
BACKUP_SHA = "57946011603eed3363ae99873a39eb20a9d83822c9abec5fe71194cca5a2d238"


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def classpath():
    return os.pathsep.join([
        os.path.join(LIB, "shim"),
        os.path.join(LIB, "minecraft-1.7.10-srg.jar"),
        os.path.join(LIB, "forge-1.7.10-srg.jar"),
        os.path.join(PROJ, "Decimation.jar"),
    ])


def compile_sources():
    sources = []
    for dp, _, fn in os.walk(SRC):
        sources += [os.path.join(dp, f) for f in fn if f.endswith(".java")]
    if not sources:
        sys.exit("no sources under src/")
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    r = subprocess.run(["javac", "--release", "8", "-nowarn", "-cp", classpath(), "-d", OUT] + sources,
                       capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("javac failed:\n" + r.stdout[-4000:] + r.stderr[-4000:])
    built = [os.path.join(dp, f) for dp, _, fn in os.walk(OUT) for f in fn if f.endswith(".class")]
    print("compiled %d source(s) -> %d class file(s)" % (len(sources), len(built)))
    return built


def inject(built):
    if sha256(BACKUP) != BACKUP_SHA:
        sys.exit("BACKUP HASH CHANGED - stop, something corrupted the original jar")
    if not os.path.exists(JAR):
        shutil.copy2(BACKUP, JAR)
        print("Decimation.jar.patched did not exist - seeded from backup")
    before = len(zipfile.ZipFile(JAR).namelist())
    entries = [os.path.relpath(p, OUT) for p in built]
    r = subprocess.run(["zip", "-q", os.path.abspath(JAR)] + entries, cwd=OUT,
                       capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("zip failed:\n" + r.stderr)
    after = len(zipfile.ZipFile(JAR).namelist())
    t = subprocess.run(["unzip", "-tq", JAR], capture_output=True, text=True)
    if t.returncode != 0:
        sys.exit("jar integrity check failed:\n" + t.stdout[-2000:])
    if sha256(BACKUP) != BACKUP_SHA:
        sys.exit("BACKUP HASH CHANGED AFTER WRITE - abort")
    print("entries %d -> %d (added %d), integrity OK, backup intact"
          % (before, after, after - before))
    for e in entries:
        print("   +", e)
    print("\njar:", JAR)
    print("copy into the Prism instance as mods/Decimation.jar to test")


if __name__ == "__main__":
    built = compile_sources()
    if "--compile" not in sys.argv:
        inject(built)
