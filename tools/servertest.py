#!/usr/bin/env python3
"""Fresh dev dedicated server run: new world with SEED, wait for spawn
generation, stop (saves), print our placement log lines.
usage: python3 tools/servertest.py SEED [keep] [pregen=x,z,r]
  keep = reuse the existing world; pregen = also generate chunks around x,z"""
import os, re, shutil, signal, subprocess, sys, time

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEV = os.path.join(PROJ, "dev")
RUN = DEV + "/run/server"
LOG = os.path.join(PROJ, "build", "server.log")
os.makedirs(os.path.dirname(LOG), exist_ok=True)
seed = sys.argv[1]

if "keep" not in sys.argv:
    shutil.rmtree(RUN + "/world", ignore_errors=True)
props = open(RUN + "/server.properties").read()
props = re.sub(r"level-seed=.*", "level-seed=" + seed, props)
open(RUN + "/server.properties", "w").write(props)

log = open(LOG, "w")
args = ["./gradlew", "runServer", "--no-configuration-cache", "-q"]
for a in sys.argv[2:]:
    if a.startswith("pregen="):
        args.append("-Ppregen=" + a.split("=", 1)[1])
p = subprocess.Popen(args,
                     cwd=DEV, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.PIPE,
                     start_new_session=True)
deadline = time.time() + 300
ok = False
while time.time() < deadline:
    time.sleep(2)
    s = open(LOG, errors="ignore").read()
    if "Done (" in s and ("pregen=" not in " ".join(sys.argv) or "PREGEN done" in s):
        ok = True
        break
    if p.poll() is not None:
        break
time.sleep(2)
if ok:
    try:
        p.stdin.write(b"stop\n"); p.stdin.flush()
    except Exception:
        pass
    try:
        p.wait(60)
    except subprocess.TimeoutExpired:
        os.killpg(p.pid, signal.SIGTERM)
        p.wait(30)
else:
    os.killpg(p.pid, signal.SIGTERM)
s = open(LOG, errors="ignore").read()
print("started" if ok else "DID NOT START")
for l in s.splitlines():
    if re.search(r"deciworldgen\] (placed|structure generator|\d+ generated zone|large |PREGEN)|Exception in server tick|Encountered an unexpected|Stopping server|Saving worlds", l):
        print(l[l.find("]") + 1:][:170])
zf = RUN + "/world/deciworldgen_zones.json"
print("zones file:", open(zf).read()[:600] if os.path.exists(zf) else "none")
