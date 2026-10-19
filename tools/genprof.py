#!/usr/bin/env python3
"""Where the server thread's chunk generation time goes, from a JFR recording (docs/performance.md 9).

    python3 tools/genprof.py RECORDING.jfr [FROM TO]

RECORDING: dev/run/server/profile.jfr of `python3 tools/servertest.py SEED type=decimation pregen=x,z,r jfr` (or a JSON
already printed by `jfr print --json --stack-depth 200 --events jdk.ExecutionSample`). FROM TO: local clock HH:MM:SS of
the PREGEN enabled / done lines, to leave out the server start. Every sample inside chunk loading or saving goes to the
innermost known part (light, saving, vanilla terrain, vanilla decoration, else our innermost class); light samples are
split by who set the block. `jfr print` keeps 5 frames a stack unless told otherwise: this asks for 200 (JFR records 64).
"""
import collections
import json
import os
import subprocess
import sys

JFR = os.path.expanduser("~/.jdks/zulu-25.jdk/Contents/Home/bin/jfr")
SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dev", "src", "main", "java")
LIGHT = {"World.updateLightByType", "World.func_147451_t", "Chunk.relightBlock", "Chunk.propagateSkylightOcclusion",
         "Chunk.recheckGaps", "Chunk.func_150811_f", "Chunk.updateSkylightNeighborHeight", "Chunk.generateSkylightMap",
         "World.markBlocksDirtyVertical", "World.computeLightValue", "World.getSavedLightValue"}
# frames between a light update and whoever set the block (passed over when naming the setter)
PASS = ("World.setBlock", "Chunk.func_150807_a", "World.setBlockToAir", "World.setBlockMetadataWithNotify",
        "Chunk.setBlockMetadata", "LongHashMap", "World.getBlock", "Chunk.getBlock", "ChunkProviderServer.provideChunk",
        "World.getChunkFromChunkCoords", "World.doChunksNearChunkExist", "World.checkChunksExist", "World.chunkExists",
        "ChunkProviderServer.chunkExists")
SAVE = ("AnvilChunkLoader.saveChunk", "AnvilChunkLoader.func_75816_a", "ChunkProviderServer.safeSaveChunk")
TERRAIN = ("ChunkProviderGenerate.provideChunk", "ChunkProviderGenerate.func_147424_a",
           "ChunkProviderGenerate.replaceBlocksForBiome", "ChunkProviderGenerate.func_147423_a", "NoiseGenerator", "MapGen",
           "GenLayer", "DeciGenLayer")
DECORATION = ("BiomeDecorator", "WorldGen", "DeciBiome.decorate", "BiomeGenBase.decorate")


def samples(path, a, b):
    if path.endswith(".jfr"):
        out = subprocess.run([JFR, "print", "--json", "--stack-depth", "200", "--events", "jdk.ExecutionSample", path],
                             check=True, capture_output=True).stdout
        events = json.loads(out)["recording"]["events"]
    else:
        events = json.load(open(path))["recording"]["events"]
    for e in events:
        v = e["values"]
        if not v.get("stackTrace") or v.get("sampledThread", {}).get("javaName") != "Server thread":
            continue
        if a <= v["startTime"].split("T")[1][:8] <= b:
            yield [f["method"]["type"]["name"].split("/")[-1] + "." + f["method"]["name"] for f in v["stackTrace"]["frames"]]


def own_classes():
    names = set()
    for d, _, files in os.walk(SRC):
        names.update(f[:-5] for f in files if f.endswith(".java"))
    return names


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    a, b = (sys.argv[2], sys.argv[3]) if len(sys.argv) > 3 else ("00:00:00", "99:99:99")
    own = own_classes()
    every = list(samples(sys.argv[1], a, b))
    gen = [s for s in every if any(f in ("ChunkProviderServer.loadChunk", "ChunkProviderServer.originalLoadChunk",
                                         "ChunkProviderServer.saveChunks") for f in s)]
    print("server thread samples %d, in chunk loading or saving %d, deepest stack %d"
          % (len(every), len(gen), max((len(s) for s in every), default=0)))
    part, setter = collections.Counter(), collections.Counter()
    for s in gen:
        label = None
        for i, f in enumerate(s):
            if f in LIGHT:
                j = i
                while j < len(s) and (s[j] in LIGHT or s[j].startswith(PASS)):
                    j += 1
                setter[s[j] if j < len(s) else "?"] += 1
                label = "light"
                break
            if any(g.startswith(SAVE) for g in s[i:]) and f.startswith(("AnvilChunkLoader", "RegionFile",
                                                                        "CompressedStreamTools", "NBTTag")):
                label = "saving"
                break
            if f.startswith(TERRAIN):
                label = "vanilla terrain"
                break
            if f.startswith(DECORATION):
                label = "vanilla decoration"
                break
        if label is None:
            label = next(("ours: " + f for f in s if f.split(".")[0] in own), "other: " + " < ".join(s[:2]))
        part[label] += 1
    n = max(len(gen), 1)
    print("--- each sample to the innermost part")
    for k, v in part.most_common(20):
        print("%6d %5.1f%%  %s" % (v, 100.0 * v / n, k))
    total = max(sum(setter.values()), 1)
    print("--- light samples by who set the block")
    for k, v in setter.most_common(12):
        print("%6d %5.1f%% of light  %s" % (v, 100.0 * v / total, k))


if __name__ == "__main__":
    main()
