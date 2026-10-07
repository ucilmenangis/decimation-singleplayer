#!/usr/bin/env python3
"""Heuristic namer: reads clues the obfuscator left in the code, costs no AI.

    python3 deobf/autoname.py RAW_SRC_DIR > deobf/maps/zz_auto.tsv

RAW_SRC_DIR is the CFR output of decimation-mcp.jar with obfuscated names
(on the case sensitive volume: /Volumes/DeciDeobf/src). Output uses the same
C/F/M format as the AI tables (see BRIEF.md), confidence L. The file is named
zz_ so apply_mappings.py reads it last; there the first name given wins, so
AI names always take precedence.

Rules:
  1. registry fields   x = new T(...).setUnlocalizedName("name")  -> x = name
                       (also setBlockName, else the first string argument)
  2. NBT fields        this.x = tag.getInteger("key") / tag.setInteger("key", this.x)
  3. event handlers    @SubscribeEvent void a(LivingHurtEvent e)    -> onLivingHurt
  4. registered types  registerModEntity(X.class, "Hummer")         -> X = HummerEntity
                       registerTileEntity(X.class, "name")          -> X = NameTileEntity
  5. accessors         T a() { return this.x; } / void b(T v) { this.x = v; }
                       -> getX / setX, using the field names known so far
"""
import os
import re
import sys
from collections import defaultdict

SRC = sys.argv[1]
MAPS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "maps")
OBF = re.compile(r"^[A-Za-z]{1,3}$")


def camel(s, upper=False):
    parts = [p for p in re.split(r"[^A-Za-z0-9]+", s) if p]
    if not parts:
        return None
    out = parts[0][0].lower() + parts[0][1:]
    for p in parts[1:]:
        out += ("_" + p) if p[0].isdigit() else (p[0].upper() + p[1:])
    if upper:
        out = out[0].upper() + out[1:]
    return out


def classes():
    """(binary name, source text) for every top-level file."""
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.endswith(".java"):
                p = os.path.join(root, f)
                yield os.path.relpath(p, SRC)[:-5], open(p, errors="ignore").read()


def known_fields():
    """Field names already chosen by the AI tables: (owner, obf) -> name."""
    names = {}
    for fn in sorted(os.listdir(MAPS)):
        if not fn.endswith(".tsv") or fn.startswith("zz_"):
            continue
        for line in open(os.path.join(MAPS, fn), errors="ignore"):
            p = line.rstrip("\n").split("\t")
            if len(p) >= 4 and p[0] == "F":
                names.setdefault((p[1].strip(), p[2].strip()), p[3].strip())
    return names


def top_level_fields(text):
    """Field names declared directly in the outer class (indent 4)."""
    return set(re.findall(r"^    (?:(?:public|private|protected|static|final|volatile|transient)\s+)*"
                          r"[\w.$<>\[\], ]+?\s+(\w+)\s*(?:=[^;]*)?;", text, re.M))


def main():
    out_c, out_f, out_m = {}, {}, {}
    fields = known_fields()
    sources = dict(classes())

    for b, text in sources.items():
        if not (b.startswith("deci/") or b.startswith("net/decimation/")):
            continue
        mine = top_level_fields(text)

        # 1. registry fields
        for m in re.finditer(r"^\s*(?:\w+\.)?(\w{1,3}) = new [\w.$]+\((.*)$", text, re.M):
            fld, rest = m.group(1), m.group(2)
            if fld not in mine or not OBF.match(fld):
                continue
            n = re.search(r'set(?:UnlocalizedName|BlockName)\("([^"]+)"\)', rest) \
                or re.search(r'"([A-Za-z][^"]{2,})"', rest)
            if n:
                name = camel(n.group(1))
                if name:
                    out_f.setdefault((b, fld), name)

        # 2. NBT fields
        for m in re.finditer(r'this\.(\w{1,3}) = (?:\(\w+\))?\w+\.get(?:Integer|Float|Boolean|String|Double|Long|Short|Byte|CompoundTag|TagList)\("([^"]+)"', text):
            if m.group(1) in mine:
                out_f.setdefault((b, m.group(1)), camel(m.group(2)))
        for m in re.finditer(r'\w+\.set(?:Integer|Float|Boolean|String|Double|Long|Short|Byte|Tag)\("([^"]+)", (?:\(\w+\))?this\.(\w{1,3})\)', text):
            if m.group(2) in mine:
                out_f.setdefault((b, m.group(2)), camel(m.group(1)))

        # 3. event handlers (outer class methods only: indent 4)
        for m in re.finditer(r"@SubscribeEvent\s*\n(?:\s*@[^\n]+\n)*    public void (\w{1,3})\(([\w.]+) \w+\)", text):
            ev = m.group(2).split(".")[-1]
            ev = ev[:-5] if ev.endswith("Event") and len(ev) > 5 else ev
            out_m.setdefault((b, m.group(1), "(%s)" % m.group(2)), "on" + ev)

        # 4. registered types
        for m in re.finditer(r'register(?:ModEntity|GlobalEntityID)\((?:\(Class\))?([\w.$]+)\.class, \(String\)"([^"]+)"', text):
            cls = m.group(1).replace(".", "/")
            nm = camel(m.group(2), upper=True)
            if nm and cls in sources and cls.startswith("deci/"):
                out_c.setdefault(cls, nm if nm.endswith("Entity") else nm + "Entity")
        for m in re.finditer(r'registerTileEntity\((?:\(Class\))?([\w.$]+)\.class, \(String\)"([^"]+)"', text):
            cls = m.group(1).replace(".", "/")
            nm = camel(m.group(2), upper=True)
            if nm and cls in sources and cls.startswith("deci/"):
                out_c.setdefault(cls, nm if nm.endswith("TileEntity") else nm + "TileEntity")

    # 5. accessors, using AI + rule 1/2 field names
    for (owner, obf), name in out_f.items():
        fields.setdefault((owner, obf), name)
    for b, text in sources.items():
        if not (b.startswith("deci/") or b.startswith("net/decimation/")):
            continue
        for m in re.finditer(r"^    public (?:final )?([\w.$<>\[\]]+) (\w{1,3})\(\) \{\s*return this\.(\w+);\s*\}", text, re.M):
            fname = fields.get((b, m.group(3)))
            if fname:
                pre = "is" if m.group(1) == "boolean" else "get"
                out_m.setdefault((b, m.group(2), "()"), pre + fname[0].upper() + fname[1:])
        for m in re.finditer(r"^    public void (\w{1,3})\(([\w.$<>\[\]]+) \w+\) \{\s*this\.(\w+) = \w+;\s*\}", text, re.M):
            fname = fields.get((b, m.group(3)))
            if fname:
                out_m.setdefault((b, m.group(1), "(%s)" % m.group(2)), "set" + fname[0].upper() + fname[1:])

    for cls, nm in sorted(out_c.items()):
        sub = "entity" if nm.endswith("Entity") and not nm.endswith("TileEntity") else "tileentity"
        print("C\t%s\t%s\t%s\tL\tauto: registry name" % (cls, nm, sub))
    for (owner, obf), nm in sorted(out_f.items()):
        if nm:
            print("F\t%s\t%s\t%s\tL" % (owner, obf, nm))
    for (owner, obf, params), nm in sorted(out_m.items()):
        print("M\t%s\t%s\t%s\t%s\tL" % (owner, obf, params, nm))
    print("auto: %d classes, %d fields, %d methods" % (len(out_c), len(out_f), len(out_m)), file=sys.stderr)


if __name__ == "__main__":
    main()
