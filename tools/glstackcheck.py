#!/usr/bin/env python3
"""GL push / pop balance of every method in a jar, from its bytecode (bug.md "OpenGL Stack overflow", 19 Oktober 2026).

    python3 tools/glstackcheck.py JAR:PREFIX [JAR:PREFIX ...]
    python3 tools/glstackcheck.py dist/Decimation.jar:deci/ dist/Decimation.jar:net/decimation/mod/ \\
                                  dev/build/libs/deciworldgen-<version>-dev.jar:net/decimation/

Reads `javap -c -p` of every class under PREFIX and follows every path through each method (branches, switches, and an
exception at any instruction inside a try range), counting glPushMatrix / glPopMatrix and glPushAttrib / glPopAttrib.
A method whose return paths end with different counts can leave a GL stack deeper (or shallower) than it found it:
called every frame, that fills the stack (32 matrix levels, 16 attribute levels) and every push after that fails.
Methods that always end with the same net push or pop (helpers like Decimation's pushScissor) are counted as a push or
pop where they are called (fixed point over all classes given).

Limits: path blind (two branches on the same constant are taken as independent: PatchScope's glass shows up for that
reason), matrix modes are not told apart, calls through interfaces or to other jars count as nothing. Confirm a finding
in the game with the dev test `glstack`.
"""
import re
import subprocess
import sys
import zipfile

INS = re.compile(r'^\s+(\d+): (\w+)\s*(.*)$')
CALL = re.compile(r'// (?:Interface)?Method ([\w/$]+)\.([\w<>$]+):(\S+)')
OWN_CALL = re.compile(r'// (?:Interface)?Method ([\w<>$]+):(\S+)')
BASE = {
    'org/lwjgl/opengl/GL11.glPushMatrix:()V': (1, 0),
    'org/lwjgl/opengl/GL11.glPopMatrix:()V': (-1, 0),
    'org/lwjgl/opengl/GL11.glPushAttrib:(I)V': (0, 1),
    'org/lwjgl/opengl/GL11.glPopAttrib:()V': (0, -1),
}


def load(jar, prefix, methods):
    names = [n[:-6] for n in zipfile.ZipFile(jar).namelist() if n.endswith('.class') and n.startswith(prefix)]
    for i in range(0, len(names), 80):
        out = subprocess.run(['javap', '-c', '-p', '-s', '-cp', jar] + [n.replace('/', '.') for n in names[i:i + 80]],
                             capture_output=True, text=True).stdout
        cls = meth = None
        code = {}
        sw = None

        def flush():
            if cls and meth and code:
                methods[cls + '.' + meth] = dict(code)

        for line in out.split('\n'):
            m = re.match(r'^(?:[\w ]*?)(?:class|interface|enum) ([\w.$]+)', line)
            if m and not line.startswith(' '):
                flush()
                cls, meth, code = m.group(1).replace('.', '/'), None, {}
                continue
            if re.match(r'^  \S.*\(.*\).*;$', line) or line.strip() == 'static {};':
                flush()
                code, sw = {}, None
                mm = re.search(r'([\w$]+|<init>)\(', line)
                meth = '<clinit>' if line.strip() == 'static {};' else (mm.group(1) if mm else '?')
                continue
            if line.strip().startswith('descriptor:') and meth and ':' not in meth:
                meth = meth + ':' + line.strip().split(':', 1)[1].strip()
                continue
            if sw is not None:
                mm = re.match(r'^\s+(?:-?\d+|default): (\d+)$', line)
                if mm:
                    code[sw][2].append(int(mm.group(1)))
                    continue
                if line.strip() == '}':
                    sw = None
                    continue
            ex = re.match(r'^\s+(\d+)\s+(\d+)\s+(\d+)\s+(\S+)', line)
            if ex and meth and code and not INS.match(line):
                code.setdefault('__ex__', []).append((int(ex.group(1)), int(ex.group(2)), int(ex.group(3))))
                continue
            mm = INS.match(line)
            if mm:
                pc = int(mm.group(1))
                code[pc] = [mm.group(2), mm.group(3), []]
                if mm.group(2) in ('tableswitch', 'lookupswitch'):
                    sw = pc
        flush()


def callee(arg, owner):
    m = CALL.search(arg)
    if m:
        return m.group(1) + '.' + m.group(2) + ':' + m.group(3)
    m = OWN_CALL.search(arg)
    if m:
        return owner + '.' + m.group(1) + ':' + m.group(2)
    return None


def paths(key, code, eff):
    """Set of (matrix, attrib) counts at the method's returns."""
    owner = key.split('.')[0]
    exs = code.get('__ex__', [])
    code = {k: v for k, v in code.items() if k != '__ex__'}
    pcs = sorted(code)
    nxt = {pcs[i]: pcs[i + 1] for i in range(len(pcs) - 1)}
    res, seen, todo = set(), set(), [(pcs[0], 0, 0)]
    while todo:
        pc, m, a = todo.pop()
        if (pc, m, a) in seen or abs(m) > 40 or abs(a) > 40:
            continue
        seen.add((pc, m, a))
        op, arg, sw = code[pc]
        for f0, t0, h in exs:
            if f0 <= pc < t0:
                todo.append((h, m, a))  # an exception thrown here lands in the handler
        if op.startswith('invoke'):
            c = callee(arg, owner)
            e = BASE.get(c) or eff.get(c)
            if e:
                m, a = m + e[0], a + e[1]
        if op.endswith('return'):
            res.add((m, a))
            continue
        if op == 'athrow':
            continue
        if op in ('goto', 'goto_w'):
            nexts = [int(arg.split()[0])]
        elif op.startswith('if'):
            nexts = [int(arg.split()[0])] + ([nxt[pc]] if pc in nxt else [])
        elif op in ('tableswitch', 'lookupswitch'):
            nexts = sw
        else:
            nexts = [nxt[pc]] if pc in nxt else []
        for x in nexts:
            todo.append((x, m, a))
    return res


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    methods = {}
    for spec in sys.argv[1:]:
        jar, prefix = spec.rsplit(':', 1)
        load(jar, prefix, methods)
    eff = {}
    for _ in range(4):
        new = {}
        for k, code in methods.items():
            r = paths(k, code, eff)
            if len(r) == 1 and r != {(0, 0)}:
                new[k] = next(iter(r))
        if new == eff:
            break
        eff = new
    print('helpers that always leave a net push or pop (matrix, attrib):')
    for k, v in sorted(eff.items()):
        print('  ', k, v)
    print('methods whose return paths disagree (some path leaves a stack changed):')
    for k, code in sorted(methods.items()):
        r = paths(k, code, eff)
        if len(r) > 1:
            print('  ', k, sorted(r))


if __name__ == '__main__':
    main()
