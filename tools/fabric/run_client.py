#!/usr/bin/env python3
"""Starts Minecraft 1.7.10 on Legacy Fabric the way a launcher does (Knot, intermediary names), with the Forge
patched game jar and the Forge layer (docs/fabric_port.md): the test loop before Prism.

    python3 tools/fabric/run_client.py [--seconds N] [--window WxH] [--jfr] [--java8|--java21] [-Dkey=value ...]

Game dir build/fabric/run: mods/ gets the layer's Fabric jar (fabric/build/libs) and any other Fabric mods put in
build/fabric/fabricmods (FPS mods ...), forgemods/ the Forge mods from build/fabric/mods (tools/fabric/build_jars.py).
Libraries: Legacy Fabric's launcher profile (loader, mixin, ASM 9,
intermediary, its LWJGL 2.9.4 with arm64 natives) + Minecraft 1.7.10's own (Mojang's version file; its LWJGL left out)
+ the Forge runtime and the libraries Forge needs (guava 17 over vanilla's 15, lzma, launchwrapper for its API).
Downloads go to build/fabric/libs. Output: build/fabric/run/client.log; prints its last lines.
"""
import json
import os
import shutil
import subprocess
import sys
import time
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "build", "fabric")
LIBS = os.path.join(OUT, "libs")
RUN = os.path.join(OUT, "run")
PRISM = os.path.expanduser("~/Library/Application Support/PrismLauncher/")
LOADER = "0.20.1"


def fetch(url, path):
    if not os.path.isfile(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        subprocess.run(["curl", "-sfL", "-o", path + ".part", url], check=True)   # urllib gets 404 from some mavens
        os.replace(path + ".part", path)
    return path


def maven(coord, repo, classifier=None):
    g, a, v = coord.split(":")[:3]
    name = "%s-%s%s.jar" % (a, v, "-" + classifier if classifier else "")
    rel = "%s/%s/%s/%s" % (g.replace(".", "/"), a, v, name)
    cached = PRISM + "libraries/" + rel
    return cached if os.path.isfile(cached) else fetch(repo.rstrip("/") + "/" + rel, os.path.join(LIBS, rel))


def classpath():
    prof = json.load(urllib.request.urlopen(
        "https://meta.legacyfabric.net/v2/versions/loader/1.7.10/%s/profile/json" % LOADER))
    cp, natives = [], []
    for lib in prof["libraries"]:
        if lib["name"].startswith("org.lwjgl.lwjgl:lwjgl-platform"):
            natives.append(maven(lib["name"], lib["url"], "natives-osx"))   # natives only, no plain jar
        elif lib["name"].startswith("net.legacyfabric:intermediary"):
            cp.append(os.path.join(OUT, "intermediary-forge.jar"))         # + the nested classes Forge adds
        else:
            cp.append(maven(lib["name"], lib["url"]))
    manifest = json.load(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    ver = json.load(urllib.request.urlopen([v["url"] for v in manifest["versions"] if v["id"] == "1.7.10"][0]))
    for lib in ver["libraries"]:
        art = lib.get("downloads", {}).get("artifact")
        name = lib["name"]
        if not art or name.startswith(("org.lwjgl", "net.java.jinput:jinput-platform", "com.google.guava")):
            continue
        cp.append(fetch(art["url"], PRISM + "libraries/" + art["path"])
                  if not os.path.isfile(PRISM + "libraries/" + art["path"]) else PRISM + "libraries/" + art["path"])
    forge_repo = "https://maven.minecraftforge.net/"
    for coord in ("com.google.guava:guava:17.0", "lzma:lzma:0.0.1", "net.minecraft:launchwrapper:1.12",
                  "org.apache.commons:commons-lang3:3.3.2", "net.sf.jopt-simple:jopt-simple:4.5"):
        cp.append(maven(coord, "https://libraries.minecraft.net/" if "launchwrapper" in coord else forge_repo))
    cp.append(os.path.join(OUT, "forge-runtime.jar"))
    cp.append(os.path.join(OUT, "minecraft-forge.jar"))
    return prof["mainClass"], cp, natives, ver["assetIndex"]["id"]


def main():
    args = sys.argv[1:]
    seconds = 120
    if "--seconds" in args:
        seconds = int(args[args.index("--seconds") + 1])
    java = os.path.expanduser("~/.jdks/zulu-21.jdk/Contents/Home/bin/java" if "--java21" in args
                              else "~/.jdks/zulu-8.jdk/Contents/Home/bin/java")
    props = [a for a in args if a.startswith("-D")]
    if "--jfr" in args:   # Java Flight Recorder profile of the whole run: build/fabric/run/profile.jfr
        props.append("-XX:StartFlightRecording=filename=%s,settings=profile" % os.path.join(RUN, "profile.jfr"))
    main_class, cp, natives, assets = classpath()
    nat = os.path.join(RUN, "natives")
    shutil.rmtree(nat, ignore_errors=True)
    os.makedirs(nat)
    for n in natives:
        zipfile.ZipFile(n).extractall(nat)
    for d in ("mods", "forgemods"):
        shutil.rmtree(os.path.join(RUN, d), ignore_errors=True)
        os.makedirs(os.path.join(RUN, d))
    layer = [f for f in os.listdir(os.path.join(ROOT, "fabric", "build", "libs")) if f.endswith(".jar") and "-sources" not in f]
    for f in layer:
        shutil.copy(os.path.join(ROOT, "fabric", "build", "libs", f), os.path.join(RUN, "mods"))
    extra = os.path.join(OUT, "fabricmods")   # other Fabric mods to run with (FPS mods ...), when present
    for f in (os.listdir(extra) if os.path.isdir(extra) else []):
        if f.endswith(".jar"):
            shutil.copy(os.path.join(extra, f), os.path.join(RUN, "mods"))
    for f in os.listdir(os.path.join(OUT, "mods")):
        if f.endswith(".jar") and not f.endswith(".events.jar"):
            os.symlink(os.path.join(OUT, "mods", f), os.path.join(RUN, "forgemods", f))
    cmd = [java, "-Xmx3G", "-Djava.library.path=" + nat, "-Dfabric.gameJarPath=" + os.path.join(OUT, "minecraft-forge.jar")]
    cmd += props + ["-cp", ":".join(cp), main_class,
                    "--username", "Developer", "--version", "fabric-forge-1.7.10", "--gameDir", RUN,
                    "--assetsDir", PRISM + "assets", "--assetIndex", assets, "--accessToken", "0",
                    "--userProperties", "{}", "--userType", "legacy"]
    if "--window" in args:   # WxH, e.g. 1440x810 (FPS benches: the default 854 x 480 hits the display's refresh cap)
        w, h = args[args.index("--window") + 1].split("x")
        cmd += ["--width", w, "--height", h]
    log = os.path.join(RUN, "client.log")
    with open(log, "w") as f:
        p = subprocess.Popen(cmd, stdout=f, stderr=subprocess.STDOUT, cwd=RUN)
        try:
            p.wait(timeout=seconds)
        except subprocess.TimeoutExpired:
            p.terminate()
            time.sleep(3)
            p.kill()
    print("exit", p.returncode, "log", log)


if __name__ == "__main__":
    main()
