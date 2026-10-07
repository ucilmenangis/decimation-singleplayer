#!/usr/bin/env python3
"""Compact view of obfuscated classes for naming: declaration, fields, method
signatures, a few string constants and Forge/registry calls per method. Roughly
a tenth of the full source, which keeps naming passes cheap.

    python3 deobf/skeleton.py RAW_SRC_DIR deci/aD [deci/an/b ...]
    (a package prints every class in it; only classes still unnamed in
     deobf/names.tsv unless --all is given)
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = sys.argv[1]
ALL = "--all" in sys.argv
targets = [a for a in sys.argv[2:] if a != "--all"]

unnamed = set()
for line in list(open(os.path.join(HERE, "names.tsv")))[1:]:
    o, n = line.split("\t")[:2]
    if "/unnamed/" in n:
        unnamed.add(o)

CALL = re.compile(r"(register\w*|setUnlocalizedName|setBlockName|sendTo\w*|EVENT_BUS|bus\(\)|"
                  r"isServer|isClient|isRemote|Side\.\w+|new [A-Z]\w*Event|attackEntityFrom|"
                  r"spawnEntityInWorld|openGui|playSound\w*|addChatMessage|getSide)")


def show(path, binary):
    text = open(path, errors="ignore").read()
    print("=" * 8, binary)
    decl = re.search(r"^(public |final |abstract )*(class|interface|enum) [^{]+", text, re.M)
    print(decl.group(0).strip() if decl else "?")
    depth = 0
    method = None
    fold = []

    def flush(f):
        if f:
            names = f[1]
            print("  F", f[0] + (names[0] if len(names) == 1 else "%s..%s (%d fields)" % (names[0], names[-1], len(names))))
    strings, calls = [], []
    for raw in text.splitlines():
        line = raw.strip()
        if depth == 1 and re.match(r"(@\w+|(public|private|protected|static|final|abstract|synchronized|native|\w)[^;=]*\([^)]*\)\s*(throws [\w., ]+)?\s*\{?$)", line) and "(" in line and not line.startswith(("if", "for", "while", "switch", "return")):
            if method:
                extra = ""
                if strings:
                    extra += "  strs=" + ",".join(strings[:4])
                if calls:
                    extra += "  calls=" + ",".join(sorted(set(calls))[:5])
                print("   ", method, extra)
            method = line.rstrip("{").strip() if not line.startswith("@") else line
            strings, calls = [], []
            if line.startswith("@"):
                print("   ", line)
                method = None
        elif depth == 1 and line.endswith(";") and "(" not in line.split("=")[0]:
            m = re.match(r"(.*?)\b(\w+)\s*;$", line)
            key = m.group(1) if m and "=" not in line else None
            if key is not None and fold and fold[0] == key:
                fold[1].append(m.group(2))
            else:
                flush(fold)
                fold[:] = [key, [m.group(2)]] if key is not None else []
                if key is None:
                    print("  F", line[:120])
        else:
            if fold and depth == 1:
                flush(fold)
                fold = []
            strings += re.findall(r'"([^"]{3,40})"', line)
            calls += CALL.findall(line)
        depth += raw.count("{") - raw.count("}")
    if method:
        print("   ", method, ("  strs=" + ",".join(strings[:4])) if strings else "")


for t in targets:
    p = os.path.join(SRC, t)
    if os.path.isdir(p):
        for f in sorted(os.listdir(p)):
            if f.endswith(".java"):
                b = t + "/" + f[:-5]
                if ALL or b in unnamed:
                    show(os.path.join(p, f), b)
    elif os.path.isfile(p + ".java"):
        show(p + ".java", t)
