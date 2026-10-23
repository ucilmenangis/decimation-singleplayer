#!/usr/bin/env python3
"""Makes (or updates) the Prism instance "Decimation Fabric": Minecraft 1.7.10 on Legacy Fabric with the Forge layer
(docs/fabric_port.md). Prism has no Legacy Fabric loader, so it is one custom component of the instance:

  patches/net.decimation.forgelayer.json   main class Knot, Legacy Fabric's loader libraries, guava 17 over vanilla's
                                           15, lzma / launchwrapper / commons-lang3 (Forge needs them), and two local
                                           libraries (instance libraries/): the Forge runtime and our intermediary
                                           and the game jar: mainJar = the Forge patched minecraft-forge.jar
                                           (local), in place of vanilla's
  minecraft/mods/                          the layer's Fabric jar (+ build/fabric/fabricmods when present)
  minecraft/forgemods/                     the Forge mods of build/fabric/mods (Decimation with DeciRemnant)

    python3 tools/fabric/build_jars.py && (cd fabric && ./gradlew build) && python3 tools/fabric/prism_instance.py

Prism's own LWJGL stays (it runs 1.7.10 on this Mac). Only touches its own instance folder.
"""
import json
import os
import shutil
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "build", "fabric")
INSTANCE = os.path.expanduser("~/Library/Application Support/PrismLauncher/instances/Decimation Fabric")
LOADER = "0.20.1"
UID = "net.decimation.forgelayer"
GAME = "net.decimation:minecraft-forge:1.7.10"
LOCAL = (("net.decimation:forge-runtime:1.7.10", "forge-runtime.jar"),
         ("net.decimation:intermediary-forge:1.7.10", "intermediary-forge.jar"),
         (GAME, "minecraft-forge.jar"))


def libraries():
    prof = json.load(urllib.request.urlopen(
        "https://meta.legacyfabric.net/v2/versions/loader/1.7.10/%s/profile/json" % LOADER))
    libs = [{"name": l["name"], "url": l["url"]} for l in prof["libraries"]
            if not l["name"].startswith(("org.lwjgl", "net.legacyfabric:intermediary"))]
    libs += [{"name": "com.google.guava:guava:17.0", "url": "https://maven.minecraftforge.net/"},
             {"name": "lzma:lzma:0.0.1", "url": "https://maven.minecraftforge.net/"},
             {"name": "net.minecraft:launchwrapper:1.12", "url": "https://libraries.minecraft.net/"},
             {"name": "org.apache.commons:commons-lang3:3.3.2", "url": "https://maven.minecraftforge.net/"}]
    libs += [{"name": name, "MMC-hint": "local"} for name, _ in LOCAL if name != GAME]
    return prof["mainClass"], libs


def main():
    main_class, libs = libraries()
    for d in ("patches", "libraries", "minecraft/mods", "minecraft/forgemods"):
        os.makedirs(os.path.join(INSTANCE, d), exist_ok=True)
    for name, f in LOCAL:   # a local library is found by its maven file name in the instance's libraries/
        a, v = name.split(":")[1:]
        shutil.copy(os.path.join(OUT, f), os.path.join(INSTANCE, "libraries", "%s-%s.jar" % (a, v)))
    for d, src in (("mods", [os.path.join(ROOT, "fabric", "build", "libs"), os.path.join(OUT, "fabricmods")]),
                   ("forgemods", [os.path.join(OUT, "mods")])):
        dst = os.path.join(INSTANCE, "minecraft", d)
        for f in os.listdir(dst):
            os.remove(os.path.join(dst, f))
        for s in src:
            for f in (os.listdir(s) if os.path.isdir(s) else []):
                if f.endswith(".jar") and not f.endswith((".events.jar", ".remapped.jar", "-sources.jar")):
                    shutil.copy(os.path.join(s, f), dst)
    with open(os.path.join(INSTANCE, "patches", UID + ".json"), "w") as f:
        json.dump({"formatVersion": 1, "uid": UID, "name": "Forge layer on Legacy Fabric", "version": LOADER,
                   "mainClass": main_class, "libraries": libs, "mainJar": {"name": GAME, "MMC-hint": "local"},
                   "requires": [{"uid": "net.minecraft", "equals": "1.7.10"}]}, f, indent=2)
    with open(os.path.join(INSTANCE, "mmc-pack.json"), "w") as f:
        json.dump({"formatVersion": 1, "components": [
            {"uid": "org.lwjgl", "version": "2.9.4-nightly-20150209", "dependencyOnly": True},
            {"uid": "net.minecraft", "version": "1.7.10", "important": True},
            {"uid": UID, "version": LOADER, "cachedName": "Forge layer on Legacy Fabric"}]}, f, indent=2)
    cfg = os.path.join(INSTANCE, "instance.cfg")
    if not os.path.isfile(cfg):
        with open(cfg, "w") as f:
            f.write("[General]\nConfigVersion=1.3\nInstanceType=OneSix\nname=Decimation Fabric\niconKey=default\n"
                    "AutomaticJava=true\nOverrideMemory=true\nMinMemAlloc=1024\nMaxMemAlloc=4096\n")
    print("instance", INSTANCE, "libraries", len(libs), "mods", os.listdir(os.path.join(INSTANCE, "minecraft", "mods")),
          "forgemods", os.listdir(os.path.join(INSTANCE, "minecraft", "forgemods")))


if __name__ == "__main__":
    main()
