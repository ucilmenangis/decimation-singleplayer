#!/usr/bin/env python3
"""Incremental update of the project knowledge graph (graphify-out/).

The graphify skill's --update flow, scripted so it is the same every time:

    python3 tools/graph_update.py prepare
        -> lists changed docs that need semantic extraction, and the chunk
           file each extraction agent must write (graphify extraction prompt:
           ~/.claude/skills/graphify/references/extraction-spec.md)
        -> code changes are extracted here (AST, free)
    (run one extraction agent per printed chunk, if any)
    python3 tools/graph_update.py finish
        -> merge, recluster (keeps old community names where they still fit),
           GRAPH_REPORT.md, graph.json, graph.html, manifest, cleanup

Code-only changes need no agent: prepare then finish straight away.
Must run with graphify's own interpreter; this script re-execs itself with
the interpreter recorded in graphify-out/.graphify_python.
"""
import json
import os
import subprocess
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "graphify-out"
SPEC = str(Path.home() / ".claude/skills/graphify/references/extraction-spec.md")
CHUNK_SIZE = 20

os.chdir(ROOT)
py = (OUT / ".graphify_python").read_text().strip()
if os.path.realpath(sys.executable) != os.path.realpath(py) and not os.environ.get("GRAPH_UPDATE_REEXEC"):
    os.environ["GRAPH_UPDATE_REEXEC"] = "1"
    os.execv(py, [py, __file__] + sys.argv[1:])

from graphify.detect import detect_incremental, save_manifest  # noqa: E402


def wj(name, data):
    (OUT / name).write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")


def rj(name, default=None):
    p = OUT / name
    return json.loads(p.read_text(encoding="utf-8")) if p.exists() else default


def prepare():
    from graphify.cache import check_semantic_cache
    from graphify.extract import collect_files, extract

    r = detect_incremental(Path("."))
    wj(".graphify_incremental.json", r)
    deleted = list(r.get("deleted_files", []))
    new = r.get("new_files", {})
    print("changed: %d file(s), deleted: %d" % (r.get("new_total", 0), len(deleted)))
    if r.get("new_total", 0) == 0 and not deleted:
        print("nothing to update")
        return
    wj(".graphify_detect.json", {"files": new, "all_files": r.get("files", {}),
                                 "total_files": r.get("new_total", 0),
                                 "total_words": r.get("total_words", 0)})
    if (OUT / "graph.json").exists():
        (OUT / ".graphify_old.json").write_text((OUT / "graph.json").read_text(encoding="utf-8"), encoding="utf-8")

    code = []
    for f in new.get("code", []):
        code.extend(collect_files(Path(f)) if Path(f).is_dir() else [Path(f)])
    ast = extract(code, cache_root=Path(".")) if code else {"nodes": [], "edges": []}
    wj(".graphify_ast.json", ast)
    print("AST: %d code file(s), %d nodes" % (len(code), len(ast["nodes"])))

    docs = [f for c in ("document", "paper", "image") for f in new.get(c, [])]
    cn, ce, ch, unc = check_semantic_cache(docs, root=".", prompt_file=SPEC)
    wj(".graphify_cached.json", {"nodes": cn, "edges": ce, "hyperedges": ch})
    (OUT / ".graphify_uncached.txt").write_text("\n".join(unc), encoding="utf-8")
    if not unc:
        print("no docs need extraction: run finish now")
        return
    print("docs needing extraction: %d" % len(unc))
    for i in range(0, len(unc), CHUNK_SIZE):
        n = i // CHUNK_SIZE + 1
        print("CHUNK %d -> %s" % (n, OUT / (".graphify_chunk_%02d.json" % n)))
        for f in unc[i:i + CHUNK_SIZE]:
            print("   ", f)


def relabel(G, communities):
    """Keep an old community name when its members mostly carried over."""
    old = rj(".graphify_labels.json", {}) or {}
    old_graph = rj(".graphify_old.json")
    old_members = {}
    if old_graph:
        for n in old_graph.get("nodes", []):
            c = n.get("community")
            if c is not None:
                old_members.setdefault(str(c), set()).add(n["id"])
    labels = {}
    for cid, members in communities.items():
        ms = set(members)
        best, score = None, 0.0
        for oc, om in old_members.items():
            j = len(ms & om) / float(len(ms | om) or 1)
            if j > score:
                best, score = oc, j
        if best is not None and score >= 0.5 and best in old:
            labels[cid] = old[best]
        else:
            files = Counter(Path(G.nodes[m].get("source_file") or "?").stem for m in members)
            top = max(members, key=lambda m: G.degree(m))
            labels[cid] = "%s: %s" % (files.most_common(1)[0][0], G.nodes[top].get("label", top))[:48]
    return labels


def finish():
    import glob
    from graphify.build import build_merge, build_from_json
    from graphify.cache import save_semantic_cache
    from graphify.cluster import cluster, score_all
    from graphify.analyze import god_nodes, surprising_connections, suggest_questions, graph_diff
    from graphify.report import generate
    from graphify.export import to_json
    from graphify.cli import _stamped_manifest_files

    inc = rj(".graphify_incremental.json")
    if inc is None:
        sys.exit("run prepare first")
    N, E, H = [], [], []
    for c in sorted(glob.glob(str(OUT / ".graphify_chunk_*.json"))):
        d = json.loads(Path(c).read_text(encoding="utf-8"))
        N += d.get("nodes", []); E += d.get("edges", []); H += d.get("hyperedges", [])
    unc = [l for l in (OUT / ".graphify_uncached.txt").read_text(encoding="utf-8").splitlines() if l] \
        if (OUT / ".graphify_uncached.txt").exists() else []
    if N or E:
        print("cached %d doc(s)" % save_semantic_cache(N, E, H, root=".", allowed_source_files=unc, prompt_file=SPEC))
    cached = rj(".graphify_cached.json", {"nodes": [], "edges": [], "hyperedges": []})
    ast = rj(".graphify_ast.json", {"nodes": [], "edges": []})
    seen, nodes = set(), []
    for n in ast["nodes"] + cached["nodes"] + N:
        if n["id"] not in seen:
            seen.add(n["id"]); nodes.append(n)
    new_ex = {"nodes": nodes, "edges": ast["edges"] + cached["edges"] + E,
              "hyperedges": cached.get("hyperedges", []) + H, "input_tokens": 0, "output_tokens": 0}

    deleted = list(inc.get("deleted_files", []))
    G = build_merge([new_ex], graph_path=str(OUT / "graph.json"), prune_sources=deleted or None,
                    root=".", directed=False)
    merged = {"nodes": [{"id": n, **d} for n, d in G.nodes(data=True)],
              "edges": [{**{k: v for k, v in d.items() if k not in ("_src", "_tgt", "source", "target")},
                         "source": d.get("_src", u), "target": d.get("_tgt", v)} for u, v, d in G.edges(data=True)],
              "hyperedges": list(G.graph.get("hyperedges", [])), "input_tokens": 0, "output_tokens": 0}

    mf = _stamped_manifest_files(inc["files"], new_ex, Path("."))
    sem = ("document", "paper", "image")
    disp = {f for t, fl in inc.get("new_files", {}).items() if t in sem for f in fl}
    stamped = {f for fl in mf.values() for f in fl}
    save_manifest(mf, root=".", scan_corpus={f for fl in inc["files"].values() for f in fl},
                  clear_semantic=(disp - stamped) or None)
    if disp - stamped:
        print("WARNING: not stamped (no extraction output), will be re-queued:", sorted(disp - stamped))

    G = build_from_json(merged, root=".", directed=False)
    com = cluster(G)
    coh = score_all(G, com)
    labels = relabel(G, com)
    det = rj(".graphify_detect.json", {})
    q = suggest_questions(G, com, labels)
    if not to_json(G, com, str(OUT / "graph.json"), community_labels=labels):
        sys.exit("graphify refused to shrink graph.json; rebuild with --force if intended")
    (OUT / "GRAPH_REPORT.md").write_text(
        generate(G, com, coh, labels, god_nodes(G), surprising_connections(G, com), det,
                 {"input": 0, "output": 0}, ".", suggested_questions=q), encoding="utf-8")
    wj(".graphify_labels.json", {str(k): v for k, v in labels.items()})
    old = rj(".graphify_old.json")
    if old:
        from networkx.readwrite import json_graph
        print(graph_diff(json_graph.node_link_graph(old, edges="links"), G)["summary"])
    subprocess.run(["graphify", "export", "html"], cwd=ROOT, capture_output=True)
    print("graph: %d nodes, %d edges, %d communities" % (G.number_of_nodes(), G.number_of_edges(), len(com)))
    for f in OUT.glob(".graphify_*"):
        if f.name not in (".graphify_python", ".graphify_root", ".graphify_labels.json"):
            f.unlink()


if __name__ == "__main__":
    {"prepare": prepare, "finish": finish}[sys.argv[1] if len(sys.argv) > 1 else "prepare"]()
