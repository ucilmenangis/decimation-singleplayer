#!/usr/bin/env python3
"""Fresh dev dedicated server run: new world with SEED, wait for spawn
generation, stop (saves), print our placement log lines.
usage: python3 tools/servertest.py SEED [keep] [pregen=x,z,r] [walk=x,z,steps,view] [gatescan=x,z,r] [type=decimation] [jfr]
       [bulklight=false]
       [cmd="console command;;another"]
  keep = reuse the existing world; pregen = also generate chunks around x,z;
  type = level-type (default DEFAULT)"""
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
# type=decimation selects our world type; anything else is the default
wtype = next((a.split("=", 1)[1] for a in sys.argv[2:] if a.startswith("type=")), "DEFAULT")
props = re.sub(r"level-type=.*", "level-type=" + wtype, props)
open(RUN + "/server.properties", "w").write(props)

log = open(LOG, "w")
args = ["./gradlew", "runServer", "--no-configuration-cache", "-q"]
for a in sys.argv[2:]:
    if a.startswith("pregen="):
        args.append("-Ppregen=" + a.split("=", 1)[1])
    if a.startswith("walk="):
        args.append("-Pwalk=" + a.split("=", 1)[1])     # x,z,steps,view: load chunks as a player walking east (DevPregen.walk)
    if a.startswith("gatescan="):
        args.append("-Pgatescan=" + a.split("=", 1)[1])   # x,z,r: log the wasteland gates there (WastelandFence.scan)
    if a == "debugsets":
        args.append("-Pdebugsets")
    if a == "dress":
        args.append("-Pdress")      # city dressing on (docs/city_dressing.md)
    if a == "jfr":
        args.append("-Pjfr")        # JFR of the whole run: dev/run/server/profile.jfr (docs/performance.md)
    if a.startswith("bulklight="):
        args.append("-Pbulklight=" + a.split("=", 1)[1])   # false: structure writes lit block by block (BlockBatch off)
p = subprocess.Popen(args,
                     cwd=DEV, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.PIPE,
                     start_new_session=True)
deadline = time.time() + (1200 if "pregen=" in " ".join(sys.argv) or "walk=" in " ".join(sys.argv) else 300)
ok = False
while time.time() < deadline:
    time.sleep(2)
    s = open(LOG, errors="ignore").read()
    if "Done (" in s and ("pregen=" not in " ".join(sys.argv) or "PREGEN done" in s) \
            and ("walk=" not in " ".join(sys.argv) or "WALK done" in s) \
            and ("gatescan=" not in " ".join(sys.argv) or "GATESCAN done" in s):
        ok = True
        break
    if p.poll() is not None:
        break
time.sleep(2)
if ok:
    # cmd="..." : a console command run before stopping (e.g. cmd="deciworldgen rebuild 64 0 100")
    for a in sys.argv[2:]:
        if a.startswith("cmd="):
            # several commands: separated by ";;", 1 s apart, 15 s after the last
            for c in a.split("=", 1)[1].split(";;"):
                p.stdin.write((c.strip() + "\n").encode()); p.stdin.flush()
                time.sleep(1)
            time.sleep(14)
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
    if re.search(r"deciworldgen\] (placed|structure generator|\d+ generated zone|large |PREGEN|WALK)|Exception in server tick|Encountered an unexpected|Stopping server|Saving worlds", l):
        print(l[l.find("]") + 1:][:170])
zf = RUN + "/world/deciworldgen_zones.json"
print("zones file:", open(zf).read()[:600] if os.path.exists(zf) else "none")
