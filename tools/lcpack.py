#!/usr/bin/env python3
"""Build the content pack for the Lost Cities style city engine (LcCity):
every building, multi building and stairs part that the packs' district
styles (city styles) list, converted to 1.7.10 schematics, plus an index.

    python3 tools/lcpack.py OUT_DIR PACK=DATA_DIR:NAMESPACE[:STYLE,STYLE...] ...

e.g. dc=<v1>/../dc/data:deceasedcraft
     legacy=<legacy>/kubejs/data:c70cities
OUT_DIR is normally <instance>/config/decimation_worldgen/lc (local only:
DeceasedCraft's content, never commit it). Writes OUT_DIR/<pack>/<name>.
schematic and OUT_DIR/index.json:
  {"buildings": [{"file", "pack", "name", "styles": {style: factor},
                  "cx", "cz" (chunks), "groundY", "height"}],
   "stairs": {"<pack>:<style>": [file, ...]},
   "highways": {"open" | "open_bi" | "bridge" | "bridge_bi" | "tunnel" |
                "tunnel_bi": [file, ...]} (from the first pack whose world
                style lists highway parts; repeats = weight),
   "decor": {"<pack>:<style>": {"parks" | "fountains" | "fronts": [file, ...]}}
            (16 x 16 parts on a chunk's ground; fountains are street scenes,
            fronts building entrances on the street side, x 0 toward the
            building; repeats = weight),
   "streets": {"<pack>:<style>": {"straight" | "end" | "bend" | "t" | "all" |
              "none" | "full": [file, ...]}} (Lost Cities street parts,
              slice 0 = street surface; unturned straight runs along x, end
              opens west, bend west + north, t all but south; road paint
              (refueled) becomes painted road blocks in the layer below),
   "names": {"<id>": "<block name>"}}
Bedrock (id 7) marks "keep the world" (cellar padding of multi building
chunks with fewer cellars). Schematic ids are the ids of REGISTRY_WORLD below, "names" maps them back
to block names so LcContent can remap them in any world.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lc2schem as lc  # noqa: E402
import lctranslate as lt  # noqa: E402
import mapsurvey as ms  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# block ids read from a dev world's level.dat (the tests' own world since v0.42.5, else the old one)
REGISTRY_WORLD = next((p for p in (os.path.join(ROOT, "dev/run/client/saves/deciworldgen_devtest"),
                                   os.path.join(ROOT, "dev/run/client/saves/deciworldgen_autotest"))
                       if os.path.isdir(p)), os.path.join(ROOT, "dev/run/client/saves/deciworldgen_devtest"))
SKIP_STYLES = ("dummycity",)


def convert(pack, ns, ref, ids):
    """Building / multi building ref -> (W, H, L, blocks, add, meta, groundY, cx, cz)."""
    name = ref.split(":")[-1]
    multi = pack.load("multibuildings", ref) if "/" not in name else None
    if multi:
        grid = multi["buildings"]
    else:
        grid = [[ref if ":" in ref else ns + ":" + ref]]
    cols, grounds = {}, {}
    for gx, row in enumerate(grid):
        for gz, bref in enumerate(row):
            cols[(gx, gz)], grounds[(gx, gz)] = lc.building_columns(pack, bref)
            paint(cols[(gx, gz)], asphalt_only=True)  # parking lot lines (audit 2026-10-09)
    depth = max(grounds.values())
    # chunks with fewer cellars than the deepest one: padding below them
    # keeps the world (bedrock = skip marker, as in SchematicPlan)
    cols = {k: [[["SKIP"] * 16 for _ in range(16)]] * (depth - grounds[k]) + v for k, v in cols.items()}
    return rasterise(cols, len(grid), max(len(r) for r in grid), ids, road_override) + (
        depth, len(grid), max(len(r) for r in grid))


def road_override(state):
    """Highway decks: black sandstone is the asphalt (plain translation gives beige sandstone).
    "=name@meta" is an already translated 1.7.10 block (street paint)."""
    if state.startswith("="):
        name, _, meta = state[1:].partition("@")
        return (name, int(meta or 0))
    name = state.split("[")[0]
    if name == "biomesoplenty:black_sandstone":
        return ("deci:BlockRoad", 0)
    return lt.translate(state)


ASPHALT = ("black_sandstone", "smooth_black_sandstone", "basalt", "smooth_basalt", "polished_basalt",
           "blackstone", "black_concrete")


def paint(sl, asphalt_only=False):
    """Road paint decals (refueled mod) sit on top of the road; 1.7.10 has
    painted road blocks instead. Lines become deci:BlockRoad_CenterLine in
    the layer below (meta 2 = line along x for paint facing east / west, 4 =
    along z), zebra stripes white quartz; the decal itself becomes air."""
    for y in range(1, len(sl)):
        for z, row in enumerate(sl[y]):
            for x, st in enumerate(row):
                if not st or not (st.startswith("refueled:") or st.startswith("car:line")):
                    continue
                below = sl[y - 1][z][x] or ""
                if asphalt_only and below.split("[")[0].split(":")[-1] not in ASPHALT:
                    continue  # buildings: paint only on asphalt (parking lots), else leave the decal out
                kind = st.split(":")[1].split("[")[0]
                if kind == "post":
                    continue  # refueled:post is a bollard, not paint
                face = "north"
                if "facing=" in st:
                    face = st.split("facing=")[1].split(",")[0].rstrip("]")
                if kind == "zebra":
                    sl[y - 1][z][x] = "=minecraft:quartz_block@0"
                else:
                    sl[y - 1][z][x] = "=deci:BlockRoad_CenterLine@%d" % (2 if face in ("east", "west") else 4)
                row[x] = None
    return sl


def rasterise(cols, cx, cz, ids, translate=None):
    W, L = cx * 16, cz * 16
    H = max(len(c) for c in cols.values())
    blocks = bytearray(W * H * L)
    add = bytearray((W * H * L + 1) // 2)
    meta = bytearray(W * H * L)
    for (gx, gz), layers in cols.items():
        for y, s in enumerate(layers):
            for z, row in enumerate(s[:16]):
                for x, state in enumerate(row):
                    if state is None:
                        continue
                    res = ("minecraft:bedrock", 0) if state == "SKIP" else (translate or lt.translate)(state)
                    if res is None or res[0] == "skip":
                        continue
                    bid = ids.get(res[0])
                    if bid is None:
                        continue
                    i = (y * L + gz * 16 + z) * W + gx * 16 + x
                    blocks[i] = bid & 255
                    hi = (bid >> 8) & 15
                    add[i >> 1] = (add[i >> 1] & 0x0F) | (hi << 4) if i & 1 else (add[i >> 1] & 0xF0) | hi
                    meta[i] = res[1] & 15
    return W, H, L, blocks, add, meta


def main():
    out = sys.argv[1]
    reg = ms.registry(REGISTRY_WORLD)
    ids = {n: i for i, n in reg.items()}
    ids["minecraft:air"] = 0
    index = {"buildings": [], "stairs": {}, "decor": {}, "streets": {},
             "names": {str(i): n for i, n in reg.items()}}
    for spec in sys.argv[2:]:
        key, _, rest = spec.partition("=")
        parts = rest.split(":")
        data, ns = parts[0], parts[1]
        only = parts[2].split(",") if len(parts) > 2 else None
        pack = lc.Pack(data, ns)
        root = os.path.join(data, ns, "lostcities", "citystyles")
        wanted = {}   # ref -> {style: factor}
        os.makedirs(os.path.join(out, key), exist_ok=True)
        for f in sorted(os.listdir(root)):
            style = f[:-5]
            if style in SKIP_STYLES or (only and style not in only):
                continue
            cs = json.load(open(os.path.join(root, f)))
            sel = cs.get("selectors", {})
            for k in ("buildings", "multibuildings"):
                for e in sel.get(k, []):
                    wanted.setdefault(e["value"], {})[style] = wanted.get(e["value"], {}).get(style, 0) + e.get(
                        "factor", 1)
            stairs = []
            for e in sel.get("stairs", []):
                ref = e["value"]
                fname = ref.split(":")[-1].replace("/", "__")
                path = os.path.join(out, key, fname + ".schematic")
                if not os.path.exists(path):
                    sl = lc.part_slices(pack, ref, {}, {}, pack.palette(ns + ":common"))
                    if not sl:
                        continue
                    W, H, L, b, a, m = rasterise({(0, 0): sl}, 1, 1, ids)
                    lc.write_schematic(path, W, H, L, b, a, m)
                stairs.extend([key + "/" + fname + ".schematic"] * max(1, int(e.get("factor", 1))))
            if stairs:
                index["stairs"]["%s:%s" % (key, style)] = stairs
            decor = {}
            for kind in ("parks", "fountains", "fronts"):
                files = []
                for e in sel.get(kind, []):
                    ref = e["value"]
                    fname = ref.split(":")[-1].replace("/", "__")
                    path = os.path.join(out, key, fname + ".schematic")
                    if not os.path.exists(path):
                        sl = lc.part_slices(pack, ref, {}, {}, pack.palette(ns + ":common"))
                        if not sl:
                            continue
                        W, H, L, b, a, m = rasterise({(0, 0): sl}, 1, 1, ids, road_override)
                        lc.write_schematic(path, W, H, L, b, a, m)
                    files.extend([key + "/" + fname + ".schematic"] * max(1, int(e.get("factor", 1))))
                if files:
                    decor[kind] = files
            if decor:
                index["decor"]["%s:%s" % (key, style)] = decor
            streets = {}
            for kind, refs in cs.get("streetblocks", {}).get("parts", {}).items():
                files = []
                for ref in refs:
                    fname = ref.split(":")[-1].replace("/", "__")
                    path = os.path.join(out, key, fname + ".schematic")
                    if not os.path.exists(path):
                        sl = lc.part_slices(pack, ref, {}, {}, pack.palette(ns + ":common"))
                        if not sl:
                            continue
                        W, H, L, b, a, m = rasterise({(0, 0): paint(sl)}, 1, 1, ids, road_override)
                        lc.write_schematic(path, W, H, L, b, a, m)
                    files.append(key + "/" + fname + ".schematic")
                if files:
                    streets[kind] = files
            if streets:
                index["streets"]["%s:%s" % (key, style)] = streets
        # highway parts of the pack's world style (Lost Cities partselector "highways")
        wsdir = os.path.join(data, ns, "lostcities", "worldstyles")
        for f in sorted(os.listdir(wsdir)) if os.path.isdir(wsdir) and "highways" not in index else []:
            hw = json.load(open(os.path.join(wsdir, f))).get("parts", {}).get("highways")
            if not hw:
                continue
            index["highways"] = {}
            for kind, refs in hw.items():
                files = []
                for ref in refs:
                    fname = ref.split(":")[-1].replace("/", "__")
                    path = os.path.join(out, key, fname + ".schematic")
                    if not os.path.exists(path):
                        sl = lc.part_slices(pack, ref, {}, {}, pack.palette(ns + ":common"))
                        if not sl:
                            continue
                        W, H, L, b, a, m = rasterise({(0, 0): sl}, 1, 1, ids, road_override)
                        lc.write_schematic(path, W, H, L, b, a, m)
                    files.append(key + "/" + fname + ".schematic")
                index["highways"][kind] = files
            print("%s: highway parts %s" % (key, {k: len(v) for k, v in index["highways"].items()}))
            break
        done = 0
        for ref, styles in sorted(wanted.items()):
            fname = ref.split(":")[-1].replace("/", "__")
            try:
                W, H, L, b, a, m, g, cx, cz = convert(pack, ns, ref, ids)
            except SystemExit as e:
                print("  skip %s: %s" % (ref, e))
                continue
            lc.write_schematic(os.path.join(out, key, fname + ".schematic"), W, H, L, b, a, m)
            index["buildings"].append({"file": key + "/" + fname + ".schematic", "pack": key, "name": fname,
                                       "styles": styles, "cx": cx, "cz": cz, "groundY": g, "height": H})
            done += 1
        print("%s: %d buildings, stairs styles %s" % (key, done,
                                                     [s for s in index["stairs"] if s.startswith(key + ":")]))
    json.dump(index, open(os.path.join(out, "index.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
