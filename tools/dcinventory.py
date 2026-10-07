#!/usr/bin/env python3
"""Catalogue of DeceasedCraft's world content (personal reference).

    python3 tools/dcinventory.py DATA_DIR MODS_DIR LCSTUDY_DIR OUT_DIR [NAMESPACE]

DATA_DIR: extracted `data/` of DCTweaks_*.jar (lostcities + structures);
MODS_DIR: the instance's mods/ (structure .nbt files are read straight
from the mod jars); LCSTUDY_DIR: output of tools/lcstudy.py (floors,
density per building). Writes:
  buildings.tsv   every Lost Cities building / multi building: category,
                  chunks, floors, districts that use it (city style weights),
                  furniture density
  infra.tsv       street / highway / bridge / park / front / stairs /
                  station / rail / fountain parts by kind
  structures.tsv  every standalone structure (.nbt): source, size, blocks,
                  furniture cells, category, main blocks
The result is summarised in docs/references/deceasedcraft_buildings.md.
"""
import collections
import gzip
import io
import json
import os
import re
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import worldcheck as wc  # noqa: E402
import lcstudy  # noqa: E402

CATEGORIES = [  # first match wins, on the name
    ("military", ("military", "army", "bunker", "barrack", "checkpoint", "camp_bostrox", "outpost", "base",
                  "ussnavy", "baselooters")),
    ("police", ("police", "prison", "prision", "jail", "cell")),
    ("medical", ("hospital", "clinic", "medical", "medic", "polyclinic", "asylum", "lab", "farmacia")),
    ("industrial", ("workshop", "factory", "derrick", "pumpjack", "carservice", "scrapyard", "storage", "warehouse", "hardware", "mines", "industrial",
                    "datacenter", "data_center")),
    ("retail / food", ("store", "shop", "market", "cafe", "caff", "cloth", "premium", "fastfood", "restaurant", "sushi", "gas",
                       "casino", "club", "sunkenclub", "flower", "gun", "convenient", "bakery", "foodtruck")),
    ("fire / public", ("firefighter", "verticalschool", "fire_station", "school", "community", "library", "church", "cathedral",
                       "post", "gallery", "bank", "film", "lighthouse", "station")),
    ("office / tower", ("office", "tower", "timbertower", "plaza", "courtyard", "thering", "terrace")),
    ("residential", ("house", "home", "apartment", "apartament", "flat", "residential", "condo", "cabin",
                     "farmhouse", "hotel", "lodge", "residence", "condo", "taiga", "casa", "cabine", "seahouse", "villager", "hut", "build1")),
    ("wasteland / ruin", ("ruin", "camp", "crash", "planecrash", "acampament", "acidente", "refug", "campode",
                          "construction", "cidade", "city", "canada", "desert", "farm", "hideout", "treehouse", "survivor", "mass", "grave",
                          "destroyed", "landslide", "path")),
    ("horror / boss", ("tomb", "boss", "ritual", "autel", "biomass", "labyrinth", "haunted", "sect", "entity",
                       "clogger", "pillar", "fabric", "farm_gone", "log_and_axe", "posess", "prisma", "flowertomb")),
]


def category(name):
    # names glue words together ("oasiscondo"); short keys would misfire inside
    # them ("smallabroken" is no lab), so those must start a word
    words = [w for w in re.split(r"[_\d./]+", name.lower().replace(".nbt", "")) if w]
    for cat, keys in CATEGORIES:
        # short keys (lab, gas, cell, hut...) only at a word start, longer ones anywhere
        if any((w.startswith(k) if len(k) <= 4 else k in w) for w in words for k in keys):
            return cat
    return "other"


def districts(root):
    """building name -> {city style: weight share}"""
    out = collections.defaultdict(dict)
    for f in sorted(os.listdir(os.path.join(root, "citystyles"))):
        cs = json.load(open(os.path.join(root, "citystyles", f)))
        sel = cs.get("selectors", {})
        for key in ("buildings", "multibuildings"):
            total = sum(e.get("factor", 0) for e in sel.get(key, [])) or 1
            for e in sel.get(key, []):
                b = e["value"].split(":")[-1].split("/")[0]
                out[b][f[:-5]] = out[b].get(f[:-5], 0) + e.get("factor", 0) / total
    return out


def structure_summary(raw):
    nbt = wc.read_nbt(gzip.decompress(raw) if raw[:2] == b"\x1f\x8b" else raw)
    size = nbt.get("size", [0, 0, 0])
    palette = nbt.get("palette") or (nbt.get("palettes") or [[]])[0]
    names = [p.get("Name", "?") for p in palette]
    blocks = collections.Counter()
    furn = 0
    for b in nbt.get("blocks", []):
        n = names[b["state"]] if b["state"] < len(names) else "?"
        if n.endswith(("air", "structure_void")):
            continue
        blocks[n] += 1
        if lcstudy.category(n) in lcstudy.FURNITURE:
            furn += 1
    return size, blocks, furn


def main():
    data, mods, lcs, out = sys.argv[1:5]
    ns = sys.argv[5] if len(sys.argv) > 5 else "deceasedcraft"  # legacy pack: c70cities
    os.makedirs(out, exist_ok=True)
    root = os.path.join(data, ns, "lostcities")
    dist = districts(root)
    study = {}
    for line in open(os.path.join(lcs, "buildings.tsv")):
        f = line.rstrip("\n").split("\t")
        if f[0] != "building":
            study[f[0]] = f
    with open(os.path.join(out, "buildings.tsv"), "w") as o:
        o.write("building\tcategory\tchunks\tfloors\tdensity\tdistricts\n")
        for name, f in sorted(study.items(), key=lambda kv: (category(kv[0]), kv[0])):
            d = ", ".join("%s %.0f%%" % (k, v * 100) for k, v in sorted(dist.get(name, {}).items(),
                                                                       key=lambda kv: -kv[1]))
            o.write("%s\t%s\t%s\t%s\t%s\t%s\n" % (name, category(name.replace("building_", "").replace("multi_", "")),
                                                  f[1], f[2], f[6], d or "scattered / unused"))
    kinds = collections.Counter()
    for r, ds, fs in os.walk(os.path.join(root, "parts")):
        for f in fs:
            n = os.path.relpath(os.path.join(r, f), os.path.join(root, "parts"))
            if n.startswith(("building_", "multi_", "scattered_", "villager_")):
                continue
            kinds[re.split(r"[/_]", n)[0]] += 1
    with open(os.path.join(out, "infra.tsv"), "w") as o:
        o.write("kind\tparts\n")
        for k, v in kinds.most_common():
            o.write("%s\t%d\n" % (k, v))
    rows = []
    for r, ds, fs in os.walk(data):
        if "/structures" not in r:
            continue
        for f in fs:
            if f.endswith(".nbt"):
                p = os.path.join(r, f)
                rows.append(("DCTweaks", os.path.relpath(p, data), open(p, "rb").read()))
    for jar in sorted(os.listdir(mods)):
        if not jar.endswith(".jar") or jar.startswith("DCTweaks"):
            continue
        try:
            z = zipfile.ZipFile(os.path.join(mods, jar))
        except Exception:
            continue
        for n in z.namelist():
            if "/structures/" in n and n.endswith(".nbt"):
                rows.append((jar, n, z.read(n)))
    with open(os.path.join(out, "structures.tsv"), "w") as o:
        o.write("source\tpath\tcategory\tsize\tblocks\tfurniture\tmain blocks\n")
        for src, path, raw in sorted(rows, key=lambda r: (r[0], r[1])):
            try:
                size, blocks, furn = structure_summary(raw)
            except Exception as e:
                o.write("%s\t%s\terror\t\t\t\t%s\n" % (src, path, e))
                continue
            main = ", ".join("%s %d" % (k.split(":")[-1], v) for k, v in blocks.most_common(5))
            o.write("%s\t%s\t%s\t%dx%dx%d\t%d\t%d\t%s\n" % (src, path, category(os.path.basename(path)),
                                                          size[0], size[1], size[2], sum(blocks.values()),
                                                          furn, main))
    print("buildings %d, infra kinds %d, structures %d" % (len(study), len(kinds), len(rows)))


if __name__ == "__main__":
    main()
