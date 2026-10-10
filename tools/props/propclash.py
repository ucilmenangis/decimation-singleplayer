#!/usr/bin/env python3
"""Props drawn into walls, into each other, into doorways, facing a wall, floating.

    python3 tools/props/propclash.py dev/run/client/devtest/milbase_1.tsv [--all]

Input: a block dump "x y z name meta" (the milbase dev test writes one per
size; lines starting with # are the origin and the camera points). Every
deci: prop is given its DRAWN box from docs/references/prop_geometry.tsv
(tools/props/propgeom.py, computed from Decimation's renderers), every other
block a simple shape (full cube, slab, carpet, fence post, thin door ...).

Reports, per finding, the prop, its cell, metadata and the nearest camera
point so it can be found in the base:
  WALL   the drawn box goes into a full block (not the floor under it)
  PROP   two drawn boxes overlap
  DOOR   a drawn box covers a door cell, or the cell in front of a door
  FACE   the front (2 E, 3 S, 4 W, 5 N) looks straight into a full block
  MOUNT  a wall mounted prop (keycard, wall flag) has no wall behind it
  FLOAT  nothing under a standing prop
Exits 1 when anything is found. --all also lists the small overlaps
(under 0.15 blocks) that are usually fine.
"""
import os
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
GEOM = os.path.join(ROOT, "docs", "references", "prop_geometry.tsv")
DIRS = {2: (1, 0), 3: (0, 1), 4: (-1, 0), 5: (0, -1)}   # front of a prop by metadata
BACK = {2: (-1, 0), 3: (0, -1), 4: (1, 0), 5: (0, 1)}
TOL = 0.08            # boxes are shrunk by this before testing (touching is fine)
SMALL = 0.15          # overlaps thinner than this are hidden without --all

# props whose front matters (a cabinet door, a screen, a seat)
FRONTED = {"BlockWeaponCabinet", "BlockMonitor", "BlockChair", "BlockOfficeChair", "BlockMilitaryRadio",
           "BlockMilitaryRadioSmall", "BlockVendingMachine_1", "BlockVendingMachine_2", "BlockPowerGenerator",
           "BlockHazardLight", "BlockSpotlight", "BlockStreetBench", "BlockMedicalCrate"}
# props hung on a wall behind them (their box pokes into that wall on purpose)
MOUNTED = {"BlockKeycardScreenMilitary", "BlockKeycardScreen", "BlockKeycardScreenLaboratory", "BlockWallflag",
           "BlockCCTV", "BlockExitLight", "BlockElectricBox1", "BlockWaterfountain", "BlockHazardScreen_1", "BlockHazardScreen_2"}
# props hung under a ceiling
CEILING = {"BlockLight", "BlockLightOff", "BlockCeilingVent", "BlockCeilingVentCorner"}
# props that draw only from a master part (the other parts are empty)
MULTI = {"BlockBarrierTall", "BlockStreetBarrier", "BlockMetalShelf", "BlockMetalShelf_Empty",
         "BlockWreckageMilitary2", "BlockPhonebox"}
NOT_SOLID = ("air", "tallgrass", "double_plant", "red_flower", "yellow_flower", "web", "torch", "ladder",
             "vine", "deadbush", "snow_layer", "BlockConcertinaWire", "rail", "lever", "redstone_wire",
             "flower_pot", "wall_sign", "standing_sign", "tripwire", "skull")


def load_geometry():
    g = {}
    with open(GEOM) as f:
        next(f)
        for line in f:
            c = line.rstrip("\n").split("\t")
            g[(c[0], int(c[3]))] = [float(v) for v in c[6:12]]   # x0 x1 y0 y1 z0 z1
    return g


def shape(name, meta):
    """Box of a non prop block inside its cell, or None when it does not block anything."""
    short = name.split(":")[-1]
    if any(short == n or short.startswith(n) for n in NOT_SOLID):
        return None
    if "door" in short.lower():
        return "door"
    if short.endswith("_slab") or short in ("stone_slab", "wooden_slab", "stone_slab2"):
        return [0, 1, 0.5, 1, 0, 1] if meta & 8 else [0, 1, 0, 0.5, 0, 1]
    if short == "carpet":
        return [0, 1, 0, 0.0625, 0, 1]
    if short == "trapdoor":
        return [0, 1, 0, 0.1875, 0, 1] if not meta & 4 else None
    if short in ("fence", "nether_brick_fence", "cobblestone_wall", "fence_gate"):
        return [0.375, 0.625, 0, 1, 0.375, 0.625]
    if short in ("iron_bars", "glass_pane", "stained_glass_pane"):
        return [0.4375, 0.5625, 0, 1, 0.4375, 0.5625]
    if short == "bed":
        return [0, 1, 0, 0.5625, 0, 1]
    if short in ("cauldron", "anvil", "enchanting_table", "brewing_stand", "cake", "hopper"):
        return [0, 1, 0, 0.8, 0, 1]
    return [0, 1, 0, 1, 0, 1]


def overlap(a, b):
    return min(a[1], b[1]) - max(a[0], b[0]), min(a[3], b[3]) - max(a[2], b[2]), min(a[5], b[5]) - max(a[4], b[4])


def main():
    path = sys.argv[1]
    show_all = "--all" in sys.argv
    geom = load_geometry()
    cells, pois, origin = {}, [], (0.0, 0.0, 0.0)
    for line in open(path):
        if line.startswith("# origin"):
            f = line.split()
            origin = (float(f[2]), float(f[3]), float(f[4]))
            continue
        if line.startswith("# poi"):
            _, _, n, x, y, z = line.split()
            pois.append((n, float(x) - origin[0], float(y), float(z) - origin[2]))
            continue
        if line.startswith("#"):
            continue
        x, y, z, name, meta = line.split()
        cells[(int(x), int(y), int(z))] = (name, int(meta))

    def near(x, z):
        best = min(pois, key=lambda p: (p[1] - x) ** 2 + (p[3] - z) ** 2) if pois else None
        return best[0] if best else "-"

    props, solids, doors = [], {}, set()
    for (x, y, z), (name, meta) in cells.items():
        short = name.split(":")[-1]
        if name.startswith("deci:") and (short, meta if meta in DIRS else 3) in geom:
            box = geom[(short, meta if meta in DIRS else 3)]
            props.append((short, meta, (x, y, z), [x + box[0], x + box[1], y + box[2], y + box[3],
                                                   z + box[4], z + box[5]]))
            continue
        s = shape(name, meta)
        if s == "door":
            doors.add((x, y, z))
        elif s is not None:
            solids[(x, y, z)] = ([x + s[0], x + s[1], y + s[2], y + s[3], z + s[4], z + s[5]], short)

    found = []

    def report(kind, prop, cell, meta, text, depth=1.0):
        if depth < SMALL and not show_all:
            return
        found.append("%-5s %-26s at %3d %3d %3d meta %d  %s  (near %s)"
                     % (kind, prop, cell[0], cell[1], cell[2], meta, text, near(cell[0], cell[2])))

    multi_seen = set()
    for short, meta, (x, y, z), box in props:
        if short in MULTI:
            # only the master draws: take the first part met in each connected run
            key = (short, x // 4, y // 4, z // 4)
            if key in multi_seen:
                continue
            multi_seen.add(key)
        b = [box[0] + TOL, box[1] - TOL, box[2] + TOL, box[3] - TOL, box[4] + TOL, box[5] - TOL]
        back = BACK.get(meta, (0, 0))
        for cx in range(int(b[0] // 1), int(b[1] // 1) + 1):
            for cy in range(int(b[2] // 1), int(b[3] // 1) + 1):
                for cz in range(int(b[4] // 1), int(b[5] // 1) + 1):
                    if (cx, cy, cz) == (x, y, z) or cy < y:
                        continue    # its own cell, or the ground it stands in (models sink a little)
                    if short in MOUNTED and (cx - x, cz - z) == back and cy == y:
                        continue
                    if (cx, cy, cz) in doors:
                        report("DOOR", short, (x, y, z), meta, "drawn into the door at %d %d %d" % (cx, cy, cz))
                        continue
                    hit = solids.get((cx, cy, cz))
                    if hit:
                        ov = overlap(b, hit[0])
                        if min(ov) > 0:
                            report("WALL", short, (x, y, z), meta, "into %s at %d %d %d (%.2f deep)"
                                   % (hit[1], cx, cy, cz, min(ov) + TOL), min(ov) + TOL)
        if short in FRONTED and meta in DIRS:
            fx, fz = DIRS[meta]
            hit = solids.get((x + fx, y, z + fz))
            above = solids.get((x + fx, y + 1, z + fz))
            if hit and above and hit[0][3] - hit[0][2] > 0.6:     # a wall, not a sill under a window
                report("FACE", short, (x, y, z), meta, "front looks into %s" % hit[1])
        if short in MOUNTED and meta in BACK:
            bx, bz = BACK[meta]
            if (x + bx, y, z + bz) not in solids:
                report("MOUNT", short, (x, y, z), meta, "no wall behind it (%s side)"
                       % {(-1, 0): "W", (1, 0): "E", (0, -1): "N", (0, 1): "S"}[(bx, bz)])
        if short not in MOUNTED and short not in CEILING and box[2] > -0.5:
            below = (x, y - 1, z)
            held = any(q[3][0] <= x + 0.5 <= q[3][1] and q[3][4] <= z + 0.5 <= q[3][5] and abs(q[3][3] - y) < 0.3
                       for q in props)               # standing on another prop's top (a table's overhang)
            if below not in solids and not held and y > 0:
                report("FLOAT", short, (x, y, z), meta, "nothing under it")
        for dx, dz in DIRS.values():
            if (x + dx, y, z + dz) in doors or (x + dx, y - 1, z + dz) in doors:
                if short not in MOUNTED:
                    report("DOOR", short, (x, y, z), meta, "stands right beside a doorway at %d %d %d"
                           % (x + dx, y, z + dz))
                break

    for i in range(len(props)):
        a = props[i]
        ab = [a[3][0] + TOL, a[3][1] - TOL, a[3][2] + TOL, a[3][3] - TOL, a[3][4] + TOL, a[3][5] - TOL]
        for j in range(i + 1, len(props)):
            c = props[j]
            if abs(c[2][0] - a[2][0]) > 12 or abs(c[2][2] - a[2][2]) > 12 or abs(c[2][1] - a[2][1]) > 6:
                continue
            if a[0] in MULTI and a[0] == c[0]:
                continue
            ov = overlap(ab, [c[3][0] + TOL, c[3][1] - TOL, c[3][2] + TOL, c[3][3] - TOL, c[3][4] + TOL,
                              c[3][5] - TOL])
            if min(ov) > 0:
                report("PROP", a[0], a[2], a[1], "overlaps %s at %d %d %d meta %d (%.2f deep)"
                       % (c[0], c[2][0], c[2][1], c[2][2], c[1], min(ov) + TOL), min(ov) + TOL)

    kinds = {}
    for f in found:
        kinds[f.split()[0]] = kinds.get(f.split()[0], 0) + 1
    for f in sorted(found, key=lambda s: (s.split()[0], s.split("(near ")[-1])):
        print(f)
    print("props", len(props), "findings", len(found), kinds)
    sys.exit(1 if found else 0)


if __name__ == "__main__":
    main()
