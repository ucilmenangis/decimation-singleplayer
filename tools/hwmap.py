#!/usr/bin/env python3
"""Highways between cities (city/Highways.java) recomputed from the seed.

    python3 tools/hwmap.py SEED RADIUS_REGIONS

Prints the region map (. wild, c civilian, C city, M military) and every
highway line near the origin: ('x', chunk row) or ('z', chunk column) with
its chunk range. Same rule as Highways.java: one line per region row /
column (offset 0 or 8 chunks), joining two city regions at most 3 non city
regions apart, plus an L link from every isolated city to its nearest
diagonal city (link(), printed as ('L', A, B, x first)). edgescan.py uses
at() to skip highway chunks.
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
_links={}
def link(seed,ax,az):
    """Highways.link: (bx, bz, x_first) for an isolated city region, else None."""
    k=(seed,ax,az)
    if k in _links: return _links[k]
    r=None
    if city(seed,ax,az) and not any(city(seed,ax+d,az) or city(seed,ax-d,az) or city(seed,ax,az+d) or city(seed,ax,az-d) for d in range(1,5)):
        best=None
        for dx in (-2,-1,1,2):
            for dz in (-2,-1,1,2):
                bx,bz=ax+dx,az+dz
                if best is not None and abs(dx)+abs(dz)>=best: continue
                if not city(seed,bx,bz): continue
                first=1 if clear(seed,ax,az,bx,bz,True) else 0 if clear(seed,ax,az,bx,bz,False) else -1
                if first>=0: r=(bx,bz,first==1); best=abs(dx)+abs(dz)
    _links[k]=r
    return r
def clear(seed,ax,az,bx,bz,xf):
    sx=1 if bx>ax else -1; sz=1 if bz>az else -1
    ps=[(x,az if xf else bz) for x in range(ax,bx+sx,sx)]+[(bx if xf else ax,z) for z in range(az,bz+sz,sz)]
    return all(not city(seed,*p) for p in ps if p!=(ax,az) and p!=(bx,bz))
def onL(seed,ax,az,bx,bz,xf,cx,cz,alongx):
    R=16
    row=line(seed,az if xf else bz,0x48575958); col=line(seed,bx if xf else ax,0x4857595A)
    if alongx:
        if cz!=row: return False
        lo=((ax+1)*R if bx>ax else col) if xf else (col if bx>ax else (bx+1)*R)
        hi=(col if bx>ax else ax*R-1) if xf else (bx*R-1 if bx>ax else col)
        return lo<=cx<=hi
    if cx!=col: return False
    lo=(row if bz>az else (bz+1)*R) if xf else ((az+1)*R if bz>az else row)
    hi=(bz*R-1 if bz>az else row) if xf else (row if bz>az else az*R-1)
    return lo<=cz<=hi
def onlink(seed,cx,cz,alongx):
    rx,rz=cx//16,cz//16
    for ax in range(rx-2,rx+3):
        for az in range(rz-2,rz+3):
            l=link(seed,ax,az)
            if l and onL(seed,ax,az,l[0],l[1],l[2],cx,cz,alongx): return True
    return False
def xh(seed,cx,cz):
    rz,rx=cz//16,cx//16
    return cz==line(seed,rz,0x48575958) and not city(seed,rx,rz) and spans(seed,rx,rz,True) or onlink(seed,cx,cz,True)
def zh(seed,cx,cz):
    rz,rx=cz//16,cx//16
    return cx==line(seed,rx,0x4857595A) and not city(seed,rx,rz) and spans(seed,rz,rx,False) or onlink(seed,cx,cz,False)
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
    for rx in range(-R,R+1):
        for rz in range(-R,R+1):
            l=link(seed,rx,rz)
            if l: print(('L',(rx,rz),l[:2],'x first' if l[2] else 'z first'))
    for s in sorted(segs):
        cs=[c for c in range(-R*16,R*16) if (xh(seed,c,s[1]) if s[0]=='x' else zh(seed,s[1],c))]
        print(s, 'chunks',min(cs),'..',max(cs),len(cs))
