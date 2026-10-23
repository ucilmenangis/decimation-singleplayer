#!/usr/bin/env python3
"""What the Forge layer on Fabric must provide (docs/fabric_port.md): every Forge / FML class, method and field that
Decimation and our mod reference, and every member they use or override that Forge added to a Minecraft class (found
by comparing Forge's patched Minecraft with vanilla).

    python3 tools/fabric/forge_inventory.py [OUT.tsv]

Reads dev/libs/Decimation-base.jar (Decimation's own classes), the newest dev/build/libs/deciworldgen-*.jar (ours,
reobfuscated to SRG names like Decimation's: `./gradlew build` in dev/ first; dev/build/classes holds MCP names),
dev/build/rfg/recompiled_minecraft-1.7.10.jar (Forge patched, MCP names) and the vanilla client jar mapped to MCP names
through RFG's srg_merged_minecraft.jar (vanilla, SRG names) plus the MCP csvs.
Writes rows: kind, owner, name, descriptor, users (decimation / ours / both), count.
"""
import collections
import csv
import glob
import os
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DECI = os.path.join(ROOT, "dev", "libs", "Decimation-base.jar")
OURS = max((p for p in glob.glob(os.path.join(ROOT, "dev", "build", "libs", "deciworldgen-*.jar"))
            if not p.endswith(("-dev.jar", "-sources.jar"))), key=os.path.getmtime)
FORGE_MC = os.path.join(ROOT, "dev", "build", "rfg", "recompiled_minecraft-1.7.10.jar")
VANILLA_SRG = os.path.join(ROOT, "dev", "build", "rfg", "srg_merged_minecraft.jar")


def parse(data):
    """A class file: (name, super, interfaces, refs [(kind, owner, name, desc)], methods [(name, desc)], classes)."""
    def u2(i):
        return struct.unpack(">H", data[i:i + 2])[0]
    n = u2(8)
    cp = [None] * n
    i, k = 10, 1
    while k < n:
        tag = data[i]
        if tag == 1:
            ln = u2(i + 1)
            cp[k] = ("utf", data[i + 3:i + 3 + ln].decode("utf-8", "replace"))
            i += 3 + ln
        elif tag in (3, 4):
            i += 5
        elif tag in (5, 6):
            i += 9
            k += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[k] = ("ref1", tag, u2(i + 1))
            i += 3
        elif tag in (9, 10, 11, 12, 18, 17):
            cp[k] = ("ref2", tag, u2(i + 1), u2(i + 3))
            i += 5
        elif tag == 15:
            i += 4
        else:
            raise ValueError("bad tag %d" % tag)
        k += 1

    def utf(x):
        return cp[x][1]

    def cls(x):
        return utf(cp[x][2])

    refs, classes = [], set()
    for e in cp:
        if not e:
            continue
        if e[0] == "ref1" and e[1] == 7:
            classes.add(utf(e[2]).lstrip("[").lstrip("L").rstrip(";"))
        if e[0] == "ref2" and e[1] in (9, 10, 11):
            nt = cp[e[3]]
            refs.append(({9: "field", 10: "method", 11: "method"}[e[1]], cls(e[2]), utf(nt[2]), utf(nt[3])))
    i += 2
    name = cls(u2(i))
    sup = u2(i + 2)
    sup = cls(sup) if sup else None
    ni = u2(i + 4)
    itf = [cls(u2(i + 6 + 2 * j)) for j in range(ni)]
    i += 6 + 2 * ni
    for _ in range(2):                                   # fields, then methods
        count = u2(i)
        i += 2
        members = []
        for _ in range(count):
            members.append((utf(u2(i + 2)), utf(u2(i + 4))))
            ac = u2(i + 6)
            i += 8
            for _ in range(ac):
                i += 6 + struct.unpack(">I", data[i + 2:i + 6])[0]
        if _ == 1 or True:
            last = members
    return name, sup, itf, refs, last, classes


def classes_of_jar(path, prefixes=None):
    z = zipfile.ZipFile(path)
    for n in z.namelist():
        if n.endswith(".class") and (prefixes is None or n.startswith(prefixes)):
            yield parse(z.read(n))


def members(jar):
    """{class: {(name, desc)}} methods of a jar's classes, and its class names."""
    out = {}
    for name, sup, itf, refs, methods, _ in classes_of_jar(jar, ("net/minecraft/",)):
        out[name] = set(methods)
    return out


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "references", "forge_api_used.tsv")
    forge_mc = members(FORGE_MC)
    users = collections.defaultdict(set)
    counts = collections.Counter()
    supers = {}
    for who, it in (("decimation", classes_of_jar(DECI, ("deci/", "net/decimation/mod/"))), ("ours", classes_of_jar(OURS, ("net/decimation/",)))):
        for name, sup, itf, refs, methods, classes in it:
            supers[name] = (sup, itf, methods, who)
            for c in classes:
                if c.startswith(("cpw/mods/fml/", "net/minecraftforge/")):
                    key = ("class", c.split("$")[0], "", "")
                    users[key].add(who)
                    counts[key] += 1
            for kind, owner, mname, desc in refs:
                if owner.startswith(("cpw/mods/fml/", "net/minecraftforge/")):
                    key = (kind, owner, mname, desc)
                elif owner.startswith("net/minecraft/") and not mname.startswith(("func_", "field_", "<")):
                    key = ("forge-added " + kind, owner, mname, desc)   # vanilla members are SRG named in these jars
                else:
                    continue
                users[key].add(who)
                counts[key] += 1
    # overrides of Forge-added methods: a method of a class extending a Minecraft class whose name is not SRG and
    # which some Forge patched Minecraft class declares with that name and descriptor
    forge_added = collections.defaultdict(set)
    for c, ms in forge_mc.items():
        for m in ms:
            if not m[0].startswith(("func_", "field_", "<")) and not m[0].startswith("lambda$"):
                forge_added[m].add(c)
    for name, (sup, itf, methods, who) in supers.items():
        chain = []
        s = sup
        while s:
            chain.append(s)
            s = supers[s][0] if s in supers else None
        if not any(c.startswith("net/minecraft/") for c in chain + list(itf)):
            continue
        for m in methods:
            if m in forge_added:
                key = ("override forge-added", "/".join(sorted(forge_added[m]))[:120], m[0], m[1])
                users[key].add(who)
                counts[key] += 1
    with open(out, "w", newline="") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow(["kind", "owner", "name", "descriptor", "users", "count"])
        for key in sorted(users, key=lambda k: (k[0], k[1], k[2], k[3])):
            u = users[key]
            w.writerow(list(key) + ["both" if len(u) == 2 else next(iter(u)), counts[key]])
    by = collections.Counter(k[0] for k in users)
    print(out, dict(by), "decimation-only rows:", sum(1 for k in users if users[k] == {"decimation"}))


if __name__ == "__main__":
    main()
