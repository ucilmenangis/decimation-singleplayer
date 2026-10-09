#!/usr/bin/env python3
"""Call the headless Blockbench MCP server from the shell (no Claude MCP
connection needed: MCP servers load only when a Claude Code session starts).

    python3 tools/bbmcp.py list                      # tool names
    python3 tools/bbmcp.py schema TOOL               # one tool's input schema
    python3 tools/bbmcp.py call TOOL '{"arg": 1}'    # call, print the result
    python3 tools/bbmcp.py batch FILE.json           # [[tool, {args}], ...] in one session

Server: jasonjgardner/blockbench-mcp-plugin, headless stdio mode
(`npx -y github:jasonjgardner/blockbench-mcp-plugin --root ROOT`), GPL-3.0,
a tool only (nothing of it ships). ROOT = tools/guns/models (models and
renders live there; the server only reads / writes inside its root).
docs/roadmap.md "Pilot: a new gun made by Claude with tools".
"""
import json
import os
import subprocess
import sys
import threading

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "guns", "models")
CMD = ["npx", "-y", "github:jasonjgardner/blockbench-mcp-plugin", "--root", ROOT]


class Server:
    def __init__(self):
        os.makedirs(ROOT, exist_ok=True)
        self.p = subprocess.Popen(CMD, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                  text=True, bufsize=1)
        self.err = []
        threading.Thread(target=lambda: [self.err.append(l) for l in self.p.stderr], daemon=True).start()
        self.n = 0
        self.request("initialize", {"protocolVersion": "2024-11-05", "capabilities": {},
                                    "clientInfo": {"name": "bbmcp.py", "version": "1"}})
        self.notify("notifications/initialized")

    def notify(self, method, params=None):
        self.p.stdin.write(json.dumps({"jsonrpc": "2.0", "method": method, "params": params or {}}) + "\n")
        self.p.stdin.flush()

    def request(self, method, params):
        self.n += 1
        self.p.stdin.write(json.dumps({"jsonrpc": "2.0", "id": self.n, "method": method, "params": params}) + "\n")
        self.p.stdin.flush()
        while True:
            line = self.p.stdout.readline()
            if not line:
                raise SystemExit("server closed: " + "".join(self.err[-20:]))
            try:
                msg = json.loads(line)
            except ValueError:
                continue
            if msg.get("id") == self.n:
                if "error" in msg:
                    raise SystemExit("error: " + json.dumps(msg["error"]))
                return msg["result"]

    def call(self, tool, args):
        r = self.request("tools/call", {"name": tool, "arguments": args})
        out = []
        for c in r.get("content", []):
            if c.get("type") == "image":
                # rendered images come back as base64: saved as <ROOT>/render_<tool>_<n>.png
                import base64
                self.images = getattr(self, "images", 0) + 1
                path = os.path.join(ROOT, "render_%s_%d.png" % (tool, self.images))
                with open(path, "wb") as f:
                    f.write(base64.b64decode(c["data"]))
                out.append("[image saved: %s]" % path)
            else:
                out.append(c.get("text"))
        return ("ERROR: " if r.get("isError") else "") + "\n".join(o for o in out if o)

    def close(self):
        self.p.stdin.close()
        self.p.terminate()


def main():
    a = sys.argv[1:]
    s = Server()
    try:
        if a[0] == "list":
            for t in s.request("tools/list", {})["tools"]:
                print(t["name"], "-", (t.get("description") or "").split("\n")[0][:110])
        elif a[0] == "schema":
            for t in s.request("tools/list", {})["tools"]:
                if t["name"] == a[1]:
                    print(json.dumps(t.get("inputSchema"), indent=1))
        elif a[0] == "call":
            print(s.call(a[1], json.loads(a[2]) if len(a) > 2 else {}))
        elif a[0] == "batch":
            for tool, args in json.load(open(a[1])):
                print("==", tool)
                print(s.call(tool, args))
    finally:
        s.close()


if __name__ == "__main__":
    main()
