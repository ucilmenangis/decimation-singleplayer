#!/usr/bin/env python3
"""Turn the agents' naming tables into a readable Decimation jar + source tree.

    python3 deobf/apply_mappings.py MAPS_DIR

Input:  deobf/decimation-mcp.jar  (Decimation's own classes, MC/Forge already MCP)
        MAPS_DIR/*.tsv            (C / F / M lines, see BRIEF.md)
Output: deobf/decimation.srg      (mapping, SRG format, obf -> readable)
        deobf/names.tsv           (every class: obf, readable, confidence, purpose)
        deobf/decimation-named.jar
        deobf/src/                (CFR output of the named jar)
        deobf/src_vineflower/     (Vineflower output for classes CFR failed on)
        deobf/apply_report.txt    (unmatched / conflicting entries)

Reference only: the named jar is for reading, it is never loaded by the game.
"""
import os
import re
import struct
import subprocess
import sys
import zipfile
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
PROJ = os.path.dirname(HERE)
IN_JAR = os.path.join(HERE, "decimation-mcp.jar")
OUT_JAR = os.path.join(HERE, "decimation-named.jar")
SRG = os.path.join(HERE, "decimation.srg")
NAMES = os.path.join(HERE, "names.tsv")
REPORT = os.path.join(HERE, "apply_report.txt")
SRC = os.path.join(HERE, "src")
SPECIAL = os.path.join(PROJ, "tools", "lib", "SpecialSource.jar")
CFR = os.path.join(HERE, "cfr.jar")
NEW_ROOT = "decimation"  # obf classes move to decimation/<subsystem>/<Name>

JAVA_KEYWORDS = set("""abstract assert boolean break byte case catch char class const continue
default do double else enum extends final finally float for goto if implements import
instanceof int interface long native new package private protected public return short
static strictfp super switch synchronized this throw throws transient try void volatile
while true false null""".split())


# ---------------------------------------------------------------- class files
def parse_class(data):
    """Minimal class file reader: name, super, interfaces, fields, methods."""
    pos = 10
    count = struct.unpack(">H", data[8:10])[0]
    cp = [None] * count
    i = 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack(">H", data[pos + 1:pos + 3])[0]
            cp[i] = ("utf8", data[pos + 3:pos + 3 + ln].decode("utf-8", "replace"))
            pos += 3 + ln
        elif tag in (3, 4):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag == 7:
            cp[i] = ("class", struct.unpack(">H", data[pos + 1:pos + 3])[0])
            pos += 3
        elif tag in (8, 16, 19, 20):
            pos += 3
        elif tag in (9, 10, 11, 12, 18, 17):
            pos += 5
        elif tag == 15:
            pos += 4
        else:
            raise ValueError("bad constant pool tag %d" % tag)
        i += 1

    def utf(idx):
        return cp[idx][1]

    def cls(idx):
        return utf(cp[idx][1]) if idx else None

    access, this_c, super_c = struct.unpack(">HHH", data[pos:pos + 6])
    pos += 6
    n_if = struct.unpack(">H", data[pos:pos + 2])[0]
    pos += 2
    ifaces = [cls(struct.unpack(">H", data[pos + 2 * k:pos + 2 * k + 2])[0]) for k in range(n_if)]
    pos += 2 * n_if

    def members():
        nonlocal pos
        n = struct.unpack(">H", data[pos:pos + 2])[0]
        pos += 2
        out = []
        for _ in range(n):
            acc, ni, di, na = struct.unpack(">HHHH", data[pos:pos + 8])
            pos += 8
            for _ in range(na):
                ln = struct.unpack(">I", data[pos + 2:pos + 6])[0]
                pos += 6 + ln
            out.append((utf(ni), utf(di), acc))
        return out

    fields = members()
    methods = members()
    return {"name": cls(this_c), "super": cls(super_c), "ifaces": ifaces,
            "fields": fields, "methods": methods}


def load_classes():
    classes = {}
    with zipfile.ZipFile(IN_JAR) as z:
        for n in z.namelist():
            if n.endswith(".class"):
                c = parse_class(z.read(n))
                classes[c["name"]] = c
    return classes


# ---------------------------------------------------------------- descriptors
def desc_params(desc):
    """'(ILjava/lang/String;[F)V' -> ['I', 'Ljava/lang/String;', '[F']"""
    out, i = [], 1
    while desc[i] != ")":
        j = i
        while desc[j] == "[":
            j += 1
        if desc[j] == "L":
            j = desc.index(";", j)
        out.append(desc[i:j + 1])
        i = j + 1
    return out


PRIM = {"I": "int", "J": "long", "Z": "boolean", "B": "byte", "C": "char",
        "S": "short", "F": "float", "D": "double", "V": "void"}


def norm_desc_type(t):
    """descriptor type -> comparable key (simple name, arrays as [])."""
    dims = len(t) - len(t.lstrip("["))
    t = t[dims:]
    if t in PRIM:
        base = PRIM[t]
    else:
        name = t[1:-1]
        if name.startswith("deci/"):
            base = name.replace("/", ".").replace("$", ".")
        else:
            base = name.split("/")[-1].replace("$", ".")  # Outer.Inner
    return base + "[]" * dims


def norm_src_type(t):
    t = t.strip().replace("...", "[]")
    t = re.sub(r"<.*>", "", t)  # drop generics
    dims = t.count("[]")
    base = t.replace("[]", "").strip()
    if base.startswith("deci."):
        base = base.replace("$", ".")
    else:
        # drop package segments, keep Outer.Inner for nested types
        segs = [x for x in base.replace("$", ".").split(".") if x]
        keep = [x for x in segs if x[:1].isupper()]
        base = ".".join(keep) if keep else segs[-1]
    return base + "[]" * dims


def parse_src_params(s):
    s = s.strip()
    if s.startswith("(") and s.endswith(")"):
        s = s[1:-1]
    if not s.strip():
        return []
    parts, depth, cur = [], 0, ""
    for ch in s:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    parts.append(cur)
    return [norm_src_type(p) for p in parts]


def params_match(have, want):
    """Descriptor keys vs agent keys. Agents often wrote obfuscated types short
    ('a.Y' for deci.aE.a.Y), so a deci type also matches on a dotted suffix."""
    if len(have) != len(want):
        return False
    for h, w in zip(have, want):
        if h == w:
            continue
        if h.endswith("." + w):
            continue  # short form: 'a.Y' for deci.aE.a.Y, 'Pre' for X.Pre
        return False
    return True


# ---------------------------------------------------------------- names
def ident(s, upper):
    s = re.sub(r"[^A-Za-z0-9_]", "", s or "")
    if not s:
        return None
    if s[0].isdigit():
        s = "_" + s
    s = (s[0].upper() if upper else s[0].lower()) + s[1:]
    if s in JAVA_KEYWORDS:
        s += "_"
    return s


def is_obf_member(name):
    return len(name) <= 3 and name not in ("<init>", "<clinit>") and not name.startswith(("func_", "field_"))


def main():
    maps_dir = sys.argv[1]
    classes = load_classes()
    report = []

    cls_rows, fld_rows, mth_rows = [], [], []
    for fn in sorted(os.listdir(maps_dir)):
        if not fn.endswith(".tsv"):
            continue
        for ln, line in enumerate(open(os.path.join(maps_dir, fn), encoding="utf-8", errors="replace"), 1):
            p = line.rstrip("\n").split("\t")
            if not p or not p[0].strip():
                continue
            k = p[0].strip()
            src = "%s:%d" % (fn, ln)
            if k == "C" and len(p) >= 5:
                cls_rows.append((p[1].strip(), p[2].strip(), p[3].strip().lower(), p[4].strip(),
                                 p[5].strip() if len(p) > 5 else "", src))
            elif k == "F" and len(p) >= 5:
                fld_rows.append((p[1].strip(), p[2].strip(), p[3].strip(), p[4].strip(), src))
            elif k == "M" and len(p) >= 6:
                mth_rows.append((p[1].strip(), p[2].strip(), p[3].strip(), p[4].strip(), p[5].strip(), src))
            else:
                report.append("BAD LINE %s: %r" % (src, line.strip()[:160]))

    # ---- classes: top-level/nested explicit names
    explicit = {}
    meta = {}
    for obf, new, sub, conf, purpose, src in cls_rows:
        obf = obf.replace(".", "/")
        if obf not in classes:
            report.append("UNKNOWN CLASS %s (%s)" % (obf, src))
            continue
        name = ident(new.split(".")[-1].split("$")[-1], True)
        if not name:
            report.append("BAD CLASS NAME %r (%s)" % (new, src))
            continue
        if obf in explicit:
            report.append("DUPLICATE CLASS %s, kept first (%s)" % (obf, src))
            continue
        explicit[obf] = (name, ident(sub, False) or "misc")
        meta[obf] = (conf, purpose)

    def outer_of(b):
        return b.rsplit("$", 1)[0] if "$" in b else None

    cmap = {}
    used = set()  # case-insensitive, the readable tree lands on macOS APFS

    def claim(path):
        base, n = path, 2
        while path.lower() in used:
            path = "%s%d" % (base, n)
            n += 1
        used.add(path.lower())
        return path

    # readable classes keep their path unless renamed; reserve them first
    for b in sorted(classes):
        if not b.startswith("deci/") and "$" not in b and b not in explicit:
            used.add(b.lower())

    def resolve(b):
        if b in cmap:
            return cmap[b]
        out = outer_of(b)
        tail = b.rsplit("$", 1)[1] if out else None
        if out:
            parent = resolve(out) if out in classes else out
            if b in explicit:
                new = parent + "$" + explicit[b][0]
            else:
                new = parent + "$" + tail
            new = claim(new)
        elif b in explicit:
            name, sub = explicit[b]
            if b.startswith("deci/"):
                new = claim("%s/%s/%s" % (NEW_ROOT, sub, name))
            else:
                new = claim(b.rsplit("/", 1)[0] + "/" + name)
        elif b.startswith("deci/"):
            # unnamed obfuscated class: keep it findable, but collision free
            pkg, simple = b[len("deci/"):].rsplit("/", 1)
            new = claim("%s/unnamed/%s_%s" % (NEW_ROOT, pkg, simple))
        else:
            new = b
        cmap[b] = new
        return new

    for b in sorted(classes, key=lambda x: x.count("$")):
        resolve(b)

    # ---- hierarchy (within the jar) for override-consistent method names
    def ancestors(b):
        seen, stack = [], [b]
        while stack:
            c = stack.pop()
            info = classes.get(c)
            if not info:
                continue
            for s in [info["super"]] + info["ifaces"]:
                if s and s in classes and s not in seen:
                    seen.append(s)
                    stack.append(s)
        return seen

    # ---- fields
    fmap = {}
    for owner, obf, new, conf, src in fld_rows:
        owner = owner.replace(".", "/")
        info = classes.get(owner)
        if not info:
            report.append("FIELD UNKNOWN OWNER %s (%s)" % (owner, src))
            continue
        hits = [f for f in info["fields"] if f[0] == obf]
        if not hits:
            report.append("FIELD NOT FOUND %s.%s (%s)" % (owner, obf, src))
            continue
        name = ident(new, False)
        if not name or not is_obf_member(obf):
            report.append("FIELD SKIPPED %s.%s -> %r (%s)" % (owner, obf, new, src))
            continue
        fmap.setdefault((owner, obf), name)  # first table wins (AI before zz_auto)
    # de-duplicate names per class (also against names not being renamed)
    for owner, info in classes.items():
        taken = {f[0] for f in info["fields"] if (owner, f[0]) not in fmap}
        for f in info["fields"]:
            k = (owner, f[0])
            if k in fmap:
                base, n, nm = fmap[k], 2, fmap[k]
                while nm in taken:
                    nm = "%s%d" % (base, n)
                    n += 1
                fmap[k] = nm
                taken.add(nm)

    # ---- methods
    mmap = {}
    for owner, obf, params, new, conf, src in mth_rows:
        owner = owner.replace(".", "/")
        info = classes.get(owner)
        if not info:
            report.append("METHOD UNKNOWN OWNER %s (%s)" % (owner, src))
            continue
        cands = [m for m in info["methods"] if m[0] == obf]
        if not cands:
            report.append("METHOD NOT FOUND %s.%s%s (%s)" % (owner, obf, params, src))
            continue
        want = parse_src_params(params)
        exact = [m for m in cands if params_match([norm_desc_type(t) for t in desc_params(m[1])], want)]
        if len(exact) == 1:
            pick = exact[0]
        elif len(cands) == 1:
            pick = cands[0]
            if not exact:
                report.append("METHOD PARAMS MISMATCH, used only overload %s.%s%s vs %s (%s)"
                              % (owner, obf, params, pick[1], src))
        else:
            report.append("METHOD AMBIGUOUS %s.%s%s candidates %s (%s)"
                          % (owner, obf, params, [m[1] for m in cands], src))
            continue
        name = ident(new, False)
        if not name or not is_obf_member(obf):
            report.append("METHOD SKIPPED %s.%s -> %r (%s)" % (owner, obf, new, src))
            continue
        mmap.setdefault((owner, obf, pick[1]), name)

    # an override must carry its ancestor's name; ancestor wins
    for (owner, obf, desc), name in list(mmap.items()):
        for anc in ancestors(owner):
            if (anc, obf, desc) in mmap and mmap[(anc, obf, desc)] != name:
                report.append("OVERRIDE NAME UNIFIED %s.%s%s: %s -> %s"
                              % (owner, obf, desc, name, mmap[(anc, obf, desc)]))
                mmap[(owner, obf, desc)] = mmap[(anc, obf, desc)]
                break
    # propagate to overriding subclasses that have no name of their own
    for owner, info in classes.items():
        for (mname, mdesc, acc) in info["methods"]:
            if (owner, mname, mdesc) in mmap or not is_obf_member(mname):
                continue
            for anc in ancestors(owner):
                if (anc, mname, mdesc) in mmap:
                    mmap[(owner, mname, mdesc)] = mmap[(anc, mname, mdesc)]
                    break
    # same name + same descriptor twice in one class would not verify
    for owner, info in classes.items():
        taken = {(m[0], m[1]) for m in info["methods"] if (owner, m[0], m[1]) not in mmap}
        for (mname, mdesc, acc) in info["methods"]:
            k = (owner, mname, mdesc)
            if k in mmap:
                base, n, nm = mmap[k], 2, mmap[k]
                while (nm, mdesc) in taken:
                    nm = "%s%d" % (base, n)
                    n += 1
                if nm != mmap[k]:
                    report.append("METHOD NAME CLASH %s.%s%s -> %s" % (owner, mname, mdesc, nm))
                mmap[k] = nm
                taken.add((nm, mdesc))

    # ---- write SRG
    def remap_desc(d):
        return re.sub(r"L([^;]+);", lambda m: "L%s;" % cmap.get(m.group(1), m.group(1)), d)

    with open(SRG, "w") as f:
        for b in sorted(cmap):
            if cmap[b] != b:
                f.write("CL: %s %s\n" % (b, cmap[b]))
        for (owner, obf), name in sorted(fmap.items()):
            f.write("FD: %s/%s %s/%s\n" % (owner, obf, cmap.get(owner, owner), name))
        for (owner, obf, desc), name in sorted(mmap.items()):
            f.write("MD: %s/%s %s %s/%s %s\n" % (owner, obf, desc, cmap.get(owner, owner), name, remap_desc(desc)))

    with open(NAMES, "w") as f:
        f.write("obfuscated\treadable\tconfidence\tpurpose\n")
        for b in sorted(classes):
            conf, purpose = meta.get(b, ("", ""))
            f.write("%s\t%s\t%s\t%s\n" % (b, cmap[b], conf, purpose))

    unnamed = sum(1 for b in cmap if "/unnamed/" in cmap[b])
    obf_f = sum(1 for c in classes.values() for x in c["fields"] if c["name"].startswith("deci/") and is_obf_member(x[0]))
    obf_m = sum(1 for c in classes.values() for x in c["methods"] if c["name"].startswith("deci/") and is_obf_member(x[0]))
    summary = ("classes %d, renamed %d (explicit %d, unnamed fallback %d); "
               "fields renamed %d of ~%d obf; methods renamed %d of ~%d obf"
               % (len(classes), sum(1 for b in cmap if cmap[b] != b), len(explicit), unnamed,
                  len(fmap), obf_f, len(mmap), obf_m))
    with open(REPORT, "w") as f:
        f.write(summary + "\n\n" + "\n".join(report) + "\n")
    print(summary)
    print("report lines:", len(report))

    # ---- remap + decompile
    r = subprocess.run(["java", "-jar", SPECIAL, "--in-jar", IN_JAR, "--out-jar", OUT_JAR,
                        "--srg-in", SRG, "--quiet"], capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("SpecialSource failed:\n" + r.stdout[-2000:] + r.stderr[-2000:])
    if os.path.isdir(SRC):
        import shutil
        shutil.rmtree(SRC)
    r = subprocess.run(["java", "-jar", CFR, OUT_JAR, "--outputdir", SRC, "--silent", "true"],
                       capture_output=True, text=True)
    n = sum(len([x for x in fs if x.endswith(".java")]) for _, _, fs in os.walk(SRC))
    print("decompiled", n, "files into", SRC)

    # Vineflower second opinion for the few classes CFR could not structure
    vf = os.path.join(HERE, "vineflower.jar")
    vf_out = os.path.join(HERE, "src_vineflower")
    failed = []
    for root, _, fs in os.walk(SRC):
        for f in fs:
            if f.endswith(".java"):
                t = open(os.path.join(root, f), errors="ignore").read()
                if "failed to decompile" in t or "Unable to fully structure" in t:
                    failed.append(os.path.relpath(os.path.join(root, f), SRC)[:-5])
    if failed and os.path.isfile(vf):
        import shutil, tempfile
        shutil.rmtree(vf_out, ignore_errors=True)
        tmp = tempfile.mkdtemp()
        with zipfile.ZipFile(OUT_JAR) as z:
            for n_ in z.namelist():
                if any(n_ == c + ".class" or n_.startswith(c + "$") for c in failed):
                    z.extract(n_, tmp)
        classes_ = [os.path.join(tmp, c + ".class") for c in failed]
        subprocess.run(["java", "-jar", vf, "-dgs=1"] + classes_ + [vf_out], capture_output=True)
        shutil.rmtree(tmp, ignore_errors=True)
        print("vineflower second opinion for", len(failed), "classes in", vf_out)


if __name__ == "__main__":
    main()
