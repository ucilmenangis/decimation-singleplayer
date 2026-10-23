#!/usr/bin/env python3
"""One tiny v2 mapping file for the Forge layer (docs/fabric_port.md): namespaces official (notch), srg (Forge's names,
what Decimation and our release jar are compiled against) and intermediary (Legacy Fabric's run time names).

    python3 tools/fabric/mappings.py [OUT.tiny]

SRG: deobfuscation_data-1.7.10.lzma inside Forge's universal jar (notch -> srg: CL, FD without descriptors, MD).
Intermediary: Loom's legacy-intermediary-1-v2.tiny for 1.7.10 (official -> intermediary). Joined on the notch name
(fields on owner + name, methods on owner + name + descriptor). Writes build/fabric/srg_intermediary.tiny by default.
"""
import glob
import lzma
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
UNIVERSAL = os.path.expanduser("~/Library/Application Support/PrismLauncher/libraries/net/minecraftforge/forge/"
                               "1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar")
INTERMEDIARY = os.path.expanduser("~/.gradle/caches/fabric-loom/1.7.10/legacy-intermediary-1-v2.tiny")


def read_srg():
    data = zipfile.ZipFile(UNIVERSAL).read("deobfuscation_data-1.7.10.lzma")
    text = lzma.LZMADecompressor(format=lzma.FORMAT_ALONE).decompress(data).decode()
    cls, fld, mth = {}, {}, {}
    for line in text.splitlines():
        p = line.split()
        if not p:
            continue
        if p[0] == "CL:":
            cls[p[1]] = p[2]
        elif p[0] == "FD:":
            o, n = p[1].rsplit("/", 1)
            fld[(o, n)] = p[2].rsplit("/", 1)[1]
        elif p[0] == "MD:":
            o, n = p[1].rsplit("/", 1)
            mth[(o, n, p[2])] = p[3].rsplit("/", 1)[1]
    return cls, fld, mth


def read_intermediary():
    """{class: intermediary}, {(owner, name, desc): intermediary} for fields and for methods (official keys)."""
    cls, fld, mth = {}, {}, {}
    owner = None
    with open(INTERMEDIARY) as f:
        header = f.readline().split("\t")
        assert header[3:5] == ["official", "intermediary\n"] or header[3].strip() == "official", header
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "c":
                owner = p[1]
                cls[owner] = p[2]
            elif len(p) >= 5 and p[1] == "f":
                fld[(owner, p[3], p[2])] = p[4]
            elif len(p) >= 5 and p[1] == "m":
                mth[(owner, p[3], p[2])] = p[4]
    return cls, fld, mth


def nest(scls, icls):
    """Nested classes whose intermediary name left their outer class's package: MinecraftServer, Main and the realms
    classes keep their names, their anonymous classes are net/minecraft/class_N, and package private access between
    them fails (IllegalAccessError, MinecraftServer$1 / $2 / $3 when the integrated server starts). They take the
    outer's name + their suffix: 12 classes, all anonymous, none a Legacy Fabric API mixin target [not checked
    against every API module]. Returns the moved ones."""
    official = {s: o for o, s in scls.items()}
    moved = set()
    for o in sorted(icls, key=lambda c: scls.get(c, c).count("$")):   # outer classes first
        s = scls.get(o, o)
        if "$" not in s:
            continue
        outer_srg, rest = s.rsplit("$", 1)
        outer = official.get(outer_srg)
        if outer in icls and icls[o].rsplit("/", 1)[0] != icls[outer].rsplit("/", 1)[0]:
            icls[o] = icls[outer] + "$" + rest
            moved.add(o)
    return moved


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "build", "fabric", "srg_intermediary.tiny")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    scls, sfld, smth = read_srg()
    icls, ifld, imth = read_intermediary()
    moved = nest(scls, icls)
    by_owner = {}
    for (o, n, d), i in ifld.items():
        by_owner.setdefault(o, {"f": [], "m": []})["f"].append((n, d, i))
    for (o, n, d), i in imth.items():
        by_owner.setdefault(o, {"f": [], "m": []})["m"].append((n, d, i))
    missing = {"class": 0, "field": 0, "method": 0}
    lines = ["tiny\t2\t0\tofficial\tsrg\tintermediary"]
    # classes Forge's universal jar adds inside vanilla classes under notch names (aji$4 in Block, alb$FireInfo ...):
    # they go into the game jar (Forgify) and follow their outer class's names, else the outer class moves to
    # net/minecraft and the nested one stays in the default package (IllegalAccessError)
    for n in zipfile.ZipFile(UNIVERSAL).namelist():
        if "/" not in n and "$" in n and n.endswith(".class"):
            outer, rest = n[:-6].split("$", 1)
            if outer in icls:
                lines.append("c\t%s\t%s\t%s" % (n[:-6], scls.get(outer, outer) + "$" + rest, icls[outer] + "$" + rest))
    for o in sorted(set(icls) | set(scls)):
        s, i = scls.get(o, o), icls.get(o)
        if i is None:
            missing["class"] += 1
            i = s
        lines.append("c\t%s\t%s\t%s" % (o, s, i))
        m = by_owner.get(o, {"f": [], "m": []})
        for n, d, im in sorted(m["f"]):
            sn = sfld.get((o, n))
            if sn is None:
                missing["field"] += 1
                sn = n
            lines.append("\tf\t%s\t%s\t%s\t%s" % (d, n, sn, im))
        for n, d, im in sorted(m["m"]):
            sn = smth.get((o, n, d))
            if sn is None:
                missing["method"] += 1
                sn = n
            lines.append("\tm\t%s\t%s\t%s\t%s" % (d, n, sn, im))
    # srg members intermediary does not know (none expected): reported, not written
    known = {(o, n) for (o, n, d) in ifld}
    extra_f = [k for k in sfld if k not in known]
    knownm = set(imth)
    extra_m = [k for k in smth if k not in knownm]
    # OptiFine's own classes in the default package (Config, Reflector, ChunkCacheOF ...: build/fabric/optifine, when
    # there) sit beside the notch classes they use package privately: they move into net/minecraft with them. In our
    # tiny too, so Forgify strings turns FML's Class.forName("Config") (detectOptifine, GuiModList) into the new name
    optifine = [n[:-6] for j in glob.glob(os.path.join(ROOT, "build", "fabric", "optifine", "*.jar"))
                for n in zipfile.ZipFile(j).namelist() if n.endswith(".class") and "/" not in n]
    lines += ["c\t%s\t%s\tnet/minecraft/%s" % (n, n, n) for n in optifine]
    with open(out, "w") as f:
        f.write("\n".join(lines) + "\n")
    # Fabric renames the game jar with the intermediary jar on its classpath (mappings/mappings.tiny): the same jar as
    # Legacy Fabric's net.legacyfabric:intermediary:1.7.10, plus the nested classes Forge adds and OptiFine's classes
    extra = [l for l in lines if l.startswith("c\t") and "$" in l.split("\t")[1] and l.split("\t")[1].split("$")[0] in icls
             and l.split("\t")[1] not in icls]
    with open(INTERMEDIARY) as f:
        base = f.read().rstrip("\n").split("\n")
    for k, line in enumerate(base):
        p = line.split("\t")
        if p[0] == "c" and p[1] in moved:
            base[k] = "c\t%s\t%s" % (p[1], icls[p[1]])
    tiny = "\n".join(base) + "\n" + "\n".join("c\t%s\t%s" % (l.split("\t")[1], l.split("\t")[3]) for l in extra) + "\n"
    tiny += "".join("c\t%s\tnet/minecraft/%s\n" % (n, n) for n in optifine)
    jar = os.path.join(os.path.dirname(out), "intermediary-forge.jar")
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("mappings/mappings.tiny", tiny)
    print(jar, "with", len(extra), "nested classes Forge adds,", len(moved), "nested classes moved to their outer's package,",
          len(optifine), "OptiFine classes into net/minecraft")
    print(out, "classes", len(icls), "srg classes", len(scls), "fields", len(ifld), "methods", len(imth),
          "no srg name:", missing, "srg only: fields", len(extra_f), "methods", len(extra_m))


if __name__ == "__main__":
    main()
