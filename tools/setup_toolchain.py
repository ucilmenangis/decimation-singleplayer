#!/usr/bin/env python3
"""Rebuild tools/lib/ from the local Prism Launcher install.

Only needs re-running if tools/lib/ is lost or Forge/Minecraft version changes.
Everything it needs is already on this machine except SpecialSource (one small
download from Maven Central).

What it produces, and why each piece is needed:

  joined.srg               notch -> SRG mapping, extracted from
                           deobfuscation_data-1.7.10.lzma inside the Forge
                           universal jar (LZMA-alone, python's lzma reads it).
                           This is the exact mapping FML applies at runtime.
  minecraft-1.7.10-srg.jar vanilla client jar remapped notch -> SRG.
  forge-1.7.10-srg.jar     Forge universal remapped the same way. Required:
                           the shipped universal jar references Minecraft by
                           *notch* names (runtime is notch; FML remaps mods
                           SRG -> notch at load), so it is unusable as a
                           compile classpath until remapped.
  shim/                    compiled tools/shim_src - see that dir's note.

Mods must be compiled against SRG names (that is why Decimation's own
decompiled code is full of func_XXXXX). Do not try to use MCP readable names:
there is no reobfuscation step in this workflow.
"""
import lzma
import os
import shutil
import subprocess
import sys
import urllib.request
import zipfile

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIB = os.path.join(PROJ, "tools", "lib")
PRISM = os.path.expanduser("~/Library/Application Support/PrismLauncher")
PRISM_LIB = os.path.join(PRISM, "libraries")

FORGE_UNIVERSAL = os.path.join(
    PRISM_LIB,
    "net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10",
    "forge-1.7.10-10.13.4.1614-1.7.10-universal.jar",
)
VANILLA = os.path.join(PRISM_LIB, "com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar")
SPECIALSOURCE_URL = (
    "https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.11.0/SpecialSource-1.11.0-shaded.jar"
)


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("FAILED: %s\n%s\n%s" % (" ".join(cmd[:3]), r.stdout[-2000:], r.stderr[-2000:]))
    return r


def main():
    for p in (FORGE_UNIVERSAL, VANILLA):
        if not os.path.exists(p):
            sys.exit("missing: %s" % p)
    os.makedirs(LIB, exist_ok=True)

    srg = os.path.join(LIB, "joined.srg")
    if not os.path.exists(srg):
        with zipfile.ZipFile(FORGE_UNIVERSAL) as z:
            raw = z.read("deobfuscation_data-1.7.10.lzma")
        with open(srg, "wb") as f:
            f.write(lzma.decompress(raw, format=lzma.FORMAT_ALONE))
        print("joined.srg written")

    ss = os.path.join(LIB, "SpecialSource.jar")
    if not os.path.exists(ss):
        urllib.request.urlretrieve(SPECIALSOURCE_URL, ss)
        print("SpecialSource downloaded")

    for src, out in ((VANILLA, "minecraft-1.7.10-srg.jar"), (FORGE_UNIVERSAL, "forge-1.7.10-srg.jar")):
        dst = os.path.join(LIB, out)
        if os.path.exists(dst):
            continue
        run(["java", "-jar", ss, "-i", src, "-o", dst, "-m", srg])
        print("remapped ->", out)

    shim_out = os.path.join(LIB, "shim")
    if os.path.isdir(shim_out):
        shutil.rmtree(shim_out)
    os.makedirs(shim_out)
    sources = []
    for dp, _, fn in os.walk(os.path.join(PROJ, "tools", "shim_src")):
        sources += [os.path.join(dp, f) for f in fn if f.endswith(".java")]
    run(["javac", "--release", "8", "-nowarn",
         "-cp", os.pathsep.join([os.path.join(LIB, "minecraft-1.7.10-srg.jar"),
                                 os.path.join(LIB, "forge-1.7.10-srg.jar")]),
         "-d", shim_out] + sources)
    print("shim compiled (%d sources)" % len(sources))
    print("toolchain ready:", LIB)


if __name__ == "__main__":
    main()
