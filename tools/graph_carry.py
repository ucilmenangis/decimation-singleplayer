#!/usr/bin/env python3
"""Cheap knowledge graph update for SMALL doc edits (no extraction agent).

    python3 tools/graph_update.py prepare          # lists the changed docs
    python3 tools/graph_carry.py EXTRA.json DOC [DOC ...]
    python3 tools/graph_update.py finish

Writes graphify-out/.graphify_chunk_01.json = every node / edge / hyperedge
graph.json already has from the given docs (a re-extraction replaces what a
doc contributed, so they must be carried over) plus the new nodes and edges
in EXTRA.json:

    {"nodes": [["id", "label", "docs/x.md"], ...],
     "edges": [["source_id", "target_id", "relation", "docs/x.md"], ...],
     "relabel": {"existing_id": "new label"}}

Use it when a doc gained a few lines (a version note, a DONE marker); use
one extraction agent (CLAUDE.md "Knowledge maintenance") for new docs or
big rewrites. Saves about 100k tokens per small update.
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + "/"
OUT = ROOT + "graphify-out/"
KEYS = ("id", "label", "file_type", "source_file", "source_location", "source_url", "captured_at",
        "author", "contributor")


def main():
    extra = json.load(open(sys.argv[1]))
    docs = sys.argv[2:]
    srcs = {ROOT + d for d in docs} | set(docs)
    g = json.load(open(OUT + "graph.json"))
    links = g.get("links") or g.get("edges") or []

    def absf(f):
        return f if not f or f.startswith("/") else ROOT + f

    nodes = []
    for n in g["nodes"]:
        if n.get("source_file") in srcs:
            c = {k: n.get(k) for k in KEYS}
            c["source_file"] = absf(c["source_file"])
            if n["id"] in extra.get("relabel", {}):
                c["label"] = extra["relabel"][n["id"]]
            nodes.append(c)
    edges = [{"source": e["source"], "target": e["target"], "relation": e.get("relation"),
              "confidence": e.get("confidence", "EXTRACTED"), "confidence_score": e.get("confidence_score", 1.0),
              "source_file": absf(e.get("source_file")), "source_location": e.get("source_location"),
              "weight": e.get("weight", 1.0)} for e in links if e.get("source_file") in srcs]
    hyper = [h for h in g.get("hyperedges", []) if h.get("source_file") in srcs]
    known = {n["id"] for n in g["nodes"]}
    ast = OUT + ".graphify_ast.json"
    if os.path.isfile(ast):
        known |= {n["id"] for n in json.load(open(ast))["nodes"]}
    for i, label, f in extra.get("nodes", []):
        nodes.append({"id": i, "label": label, "file_type": "document", "source_file": ROOT + f,
                      "source_location": None, "source_url": None, "captured_at": None, "author": None,
                      "contributor": None})
        known.add(i)
    for a, b, rel, f in extra.get("edges", []):
        for x in (a, b):
            if x not in known:
                print("warning: unknown id", x)
        edges.append({"source": a, "target": b, "relation": rel, "confidence": "EXTRACTED",
                      "confidence_score": 1.0, "source_file": ROOT + f, "source_location": None, "weight": 1.0})
    json.dump({"nodes": nodes, "edges": edges, "hyperedges": hyper, "input_tokens": 0, "output_tokens": 0},
              open(OUT + ".graphify_chunk_01.json", "w"), indent=1)
    print("chunk: %d nodes, %d edges, %d hyperedges" % (len(nodes), len(edges), len(hyper)))


if __name__ == "__main__":
    main()
