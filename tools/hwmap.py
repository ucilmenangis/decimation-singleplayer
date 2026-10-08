#!/usr/bin/env python3
"""Highways between cities (city/Highways.java) recomputed from the seed.

    python3 tools/hwmap.py SEED RADIUS_REGIONS

Prints the region map (. wild, c civilian, C city, M military) and every
highway line near the origin: ('x', chunk row) or ('z', chunk column) with
its chunk range. Same rule as Highways.java: one line per region row /
column (offset 0 or 8 chunks), joining two city regions at most 3 non city
regions apart. edgescan.py uses at() to skip highway chunks.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import edgescan as es  # noqa: E402

M=(1<<64)-1
def s64(v):
    v&=M; return v-(1<<64) if v>>63 else v
def hl(seed,x,z):
    v=(seed ^ s64(x*0x9E3779B97F4A7C15) ^ s64(z*0xC2B2AE3D27D4EB4F))&M
    v^=v>>31; v=(v*0xBF58476D1CE4E5B9)&M; v^=v>>29; v=(v*0x94D049BB133111EB)&M; v^=v>>32
    return v & ((1<<63)-1)
def city(seed,rx,rz): return es.region_sector(seed,rx,rz)==2
def line(seed,r,salt): return r*16+(hl(seed^salt,r,0)&1)*8
def spans(seed,r,o,ax):
    c=(lambda a:city(seed,a,o)) if ax else (lambda a:city(seed,o,a))
    lo,hi=r-1,r+1
    while lo>=r-3 and not c(lo): lo-=1
    while hi<=r+3 and not c(hi): hi+=1
    return c(lo) and c(hi) and hi-lo-1<=3
def xh(seed,cx,cz):
    rz,rx=cz//16,cx//16
    return cz==line(seed,rz,0x48575958) and not city(seed,rx,rz) and spans(seed,rx,rz,True)
def zh(seed,cx,cz):
    rz,rx=cz//16,cx//16
    return cx==line(seed,rx,0x4857595A) and not city(seed,rx,rz) and spans(seed,rz,rx,False)
def at(seed, cx, cz):
    return xh(seed, cx, cz) or zh(seed, cx, cz)


if __name__=='__main__':
    seed=int(sys.argv[1]); R=int(sys.argv[2])
    for rz in range(-R,R+1):
        print("%3d "%rz+"".join({0:'.',1:'c',2:'C',3:'M'}[es.region_sector(seed,rx,rz)] for rx in range(-R,R+1)))
    segs=set()
    for cx in range(-R*16,R*16):
        for cz in range(-R*16,R*16):
            if xh(seed,cx,cz): segs.add(('x',cz))
            if zh(seed,cx,cz): segs.add(('z',cx))
    for s in sorted(segs):
        cs=[c for c in range(-R*16,R*16) if (xh(seed,c,s[1]) if s[0]=='x' else zh(seed,s[1],c))]
        print(s, 'chunks',min(cs),'..',max(cs),len(cs))
