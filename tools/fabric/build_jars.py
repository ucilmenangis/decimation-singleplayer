#!/usr/bin/env python3
"""Builds the jars of the Forge layer on Legacy Fabric (docs/fabric_port.md section 2) into build/fabric/:

    python3 tools/fabric/build_jars.py [MOD.jar ...]

  srg_intermediary.tiny         tools/fabric/mappings.py
  minecraft-forge.jar           vanilla 1.7.10 client + Forge's binpatches + access transformers, official names: the
                                game jar for Legacy Fabric
  forge-runtime.jar             Forge's own classes, event classes rewritten, intermediary names
  mods/<name>.jar               each Forge mod given (default: the newest dist/onejar/Decimation.jar or
                                /tmp/decirel/onejar/Decimation.jar), event classes rewritten, SRG -> intermediary

Needs Java 8 (~/.jdks/zulu-8.jdk: Pack200) for Forgify and Java 21 for tiny-remapper; libraries from Prism's cache.
"""
import glob
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "build", "fabric")
J8 = os.path.expanduser("~/.jdks/zulu-8.jdk/Contents/Home/bin/")
J21 = os.path.expanduser("~/.jdks/zulu-21.jdk/Contents/Home/bin/")
LIB = os.path.expanduser("~/Library/Application Support/PrismLauncher/libraries/")
UNIVERSAL = LIB + "net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar"
VANILLA = os.path.expanduser("~/.gradle/caches/retro_futura_gradle/mc-vanilla/1.7.10/client.jar")
DEPS = [LIB + p for p in (
    "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
    "org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
    "lzma/lzma/0.0.1/lzma-0.0.1.jar",
    "com/google/guava/guava/17.0/guava-17.0.jar",
    "org/apache/logging/log4j/log4j-api/2.0-beta9-fixed/log4j-api-2.0-beta9-fixed.jar",
    "org/apache/logging/log4j/log4j-core/2.0-beta9-fixed/log4j-core-2.0-beta9-fixed.jar",
    "org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar")]
REMAPPER = os.path.join(HERE, "lib", "tiny-remapper-0.15.0-fat.jar")


def vanilla_libraries():
    """Minecraft 1.7.10's own libraries (Mojang's version file), from Prism's cache: classes Forge and the mods extend
    or use (netty, authlib, gson ...) must load when their event classes are rewritten."""
    import json
    import urllib.request
    manifest = json.load(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    url = [v["url"] for v in manifest["versions"] if v["id"] == "1.7.10"][0]
    out = []
    for lib in json.load(urllib.request.urlopen(url))["libraries"]:
        art = lib.get("downloads", {}).get("artifact")
        if art and os.path.isfile(LIB + art["path"]):
            out.append(LIB + art["path"])
    return out


def run(cmd):
    print("+", " ".join(os.path.basename(c) if c.endswith(".jar") else c for c in cmd[:6]), "...")
    subprocess.run(cmd, check=True, cwd=OUT)   # FML's logging writes logs/ into the working directory


def remap(src, dst, frm, to, classpath):
    run([J21 + "java", "-jar", REMAPPER, src, dst, os.path.join(OUT, "srg_intermediary.tiny"), frm, to] + classpath)


def shim(jar, cp):
    """Our replacements of Forge classes that assume LaunchWrapper or Forge's names (tools/fabric/shim, compiled
    against Fabric's loader), written over Forge's in the runtime jar, with the table they read."""
    import glob
    import shutil
    import zipfile
    src = os.path.join(HERE, "shim")
    classes = os.path.join(OUT, "shim")
    shutil.rmtree(classes, ignore_errors=True)
    os.makedirs(classes)
    loader = os.path.join(OUT, "libs", "net", "fabricmc", "fabric-loader", "0.20.1", "fabric-loader-0.20.1.jar")
    run([J8 + "javac", "-nowarn", "-d", classes, "-cp", cp + ":" + loader]
        + glob.glob(os.path.join(src, "**", "*.java"), recursive=True))
    ours = {}
    for path in glob.glob(os.path.join(classes, "**", "*.class"), recursive=True):
        with open(path, "rb") as f:
            ours[os.path.relpath(path, classes).replace(os.sep, "/")] = f.read()
    ours["forgelayer/srg_ambiguous.txt"] = ambiguous_srg().encode("utf-8")
    with zipfile.ZipFile(jar) as zin, zipfile.ZipFile(jar + ".part", "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            if info.filename not in ours:
                zout.writestr(info, zin.read(info.filename))
        for name, data in ours.items():
            zout.writestr(name, data)
    os.replace(jar + ".part", jar)
    print("shim:", ", ".join(sorted(ours)))


def ambiguous_srg():
    """SRG member names intermediary splits over several members (covariant overrides): one line each, srg name tab
    its intermediary names. The ReflectionHelper shim tries them all on the class it is given."""
    names = {}
    with open(os.path.join(OUT, "srg_intermediary.tiny")) as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) > 5 and p[1] in ("f", "m") and p[4].startswith(("func_", "field_")):
                names.setdefault(p[4], set()).add(p[5])
    return "".join("%s\t%s\n" % (k, ",".join(sorted(v))) for k, v in sorted(names.items()) if len(v) > 1)


def strings(tool, src, dst):
    """String constants naming Minecraft in SRG -> intermediary (Forgify strings), then the input is dropped."""
    run(tool[:1] + ["-Xmx3G"] + tool[1:] + ["strings", src, dst, os.path.join(OUT, "srg_intermediary.tiny")])
    os.remove(src)


def main():
    os.makedirs(os.path.join(OUT, "mods"), exist_ok=True)
    run([sys.executable, os.path.join(HERE, "mappings.py"), os.path.join(OUT, "srg_intermediary.tiny")])
    classes = os.path.join(OUT, "forgify")
    os.makedirs(classes, exist_ok=True)
    cp = ":".join([UNIVERSAL] + DEPS + vanilla_libraries())
    run([J8 + "javac", "-nowarn", "-d", classes, "-cp", cp, os.path.join(HERE, "forgify", "Forgify.java")])
    tool = [J8 + "java", "-Dfml.ignorePatchDiscrepancies=false", "-cp", classes + ":" + cp, "Forgify"]
    # OptiFine (optional, the user's jar in build/fabric/optifine): its patched classes replace Forge's, as under Forge
    optifine = sorted(glob.glob(os.path.join(OUT, "optifine", "*.jar")))[:1]
    run(tool + ["game", VANILLA, os.path.join(OUT, "srg_intermediary.tiny"), os.path.join(OUT, "minecraft-forge.jar")]
        + optifine)
    game = os.path.join(OUT, "minecraft-forge.jar")     # event classes' parents can be Minecraft classes
    tool = [J8 + "java", "-cp", classes + ":" + cp + ":" + game, "Forgify"]
    run(tool + ["forge", os.path.join(OUT, "forge-runtime-official.jar")])
    shim(os.path.join(OUT, "forge-runtime-official.jar"), cp)
    remap(os.path.join(OUT, "forge-runtime-official.jar"), os.path.join(OUT, "forge-runtime-names.jar"), "official",
          "intermediary", [os.path.join(OUT, "minecraft-forge.jar")])
    strings(tool, os.path.join(OUT, "forge-runtime-names.jar"), os.path.join(OUT, "forge-runtime.jar"))
    # Forge in SRG names: the class tree (with RFG's SRG named vanilla) the mods are remapped against, so a mod's
    # class under a Forge class under a Minecraft class gets its inherited names mapped
    srg_mc = os.path.join(ROOT, "dev", "build", "rfg", "srg_merged_minecraft.jar")
    remap(os.path.join(OUT, "forge-runtime-official.jar"), os.path.join(OUT, "forge-runtime-srg.jar"), "official", "srg",
          [os.path.join(OUT, "minecraft-forge.jar")])
    mods = sys.argv[1:] or [p for p in (os.path.join(ROOT, "dist", "onejar", "Decimation.jar"), "/tmp/decirel/onejar/Decimation.jar")
                            if os.path.isfile(p)][:1]
    for mod in mods:
        name = os.path.basename(mod)
        ev = os.path.join(OUT, "mods", name[:-4] + ".events.jar")   # tiny-remapper reads a zip only by a .jar name
        # the mod is in SRG names: its parents load from RFG's SRG named vanilla jar
        run([J8 + "java", "-Xmx3G", "-cp", ":".join([classes, cp, srg_mc, mod]), "Forgify", "mod", mod, ev])
        # the mod's references to Forge's classes keep their names; to Minecraft's: srg -> intermediary
        remap(ev, os.path.join(OUT, "mods", name[:-4] + ".remapped.jar"), "srg", "intermediary",
              [srg_mc, os.path.join(OUT, "forge-runtime-srg.jar")])
        os.remove(ev)
        strings(tool, os.path.join(OUT, "mods", name[:-4] + ".remapped.jar"), os.path.join(OUT, "mods", name))


if __name__ == "__main__":
    main()
