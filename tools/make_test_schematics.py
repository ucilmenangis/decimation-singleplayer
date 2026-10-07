#!/usr/bin/env python3
"""Generate .schematic files (MCEdit format) for the deciworldgen structure
generator - post-apocalyptic ruins in the Decimation vibe.

Filename prefix picks the sector (see StructureGenerator): civ_ / city_ /
mil_, untagged = spawns in every sector.

Block id 7 (bedrock) = SKIP marker: the placer leaves the world untouched
there, so terrain/vegetation survive around and above the ruin. Grids start
fully SKIP; buildings carve explicit AIR for their rooms.

Loot/prop placeholders (swapped for real Decimation props by name-based
substitution at placement - see DecimationWorldGen.buildSubstitutions):
    sponge(19)   -> WoodCrate         iron(42)    -> Wreckage1-5 (random)
    lapis(22)    -> AmmoCrate         diamond(57) -> MedicalCrate
    gold(41)     -> MilitaryCrate     emerald(133)-> PoliceCrate
    coal block(173) -> BlockRoad (asphalt)
    wool(35)+colour  = street props   (P_* constants)
    clay(159)+colour = markings/furniture (F_* constants)
Vanilla chest (54) stays a chest - it's in Decimation's loot table already.

    python3 tools/make_test_schematics.py     writes into structures/
"""
import gzip
import io
import os
import random
import struct

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(PROJ, "structures")

AIR, STONE, DIRT, COBBLE, PLANKS = 0, 1, 3, 4, 5
SKIP = 7                               # bedrock = leave world untouched
LOG, GLASS = 17, 20
WOODCRATE, AMMOCRATE = 19, 22          # placeholders (sponge, lapis)
MILITARYCRATE, WRECKAGE = 41, 42       # placeholders (gold, iron)
MEDICALCRATE, POLICECRATE = 57, 133    # placeholders (diamond, emerald)
SLAB, DOUBLE_SLAB, BRICK = 44, 43, 45
MOSSY_COBBLE, OAK_STAIRS = 48, 53
CHEST, FURNACE = 54, 61
COBBLE_STAIRS, FENCE = 67, 85
STONEBRICK, IRON_BARS, GLASS_PANE = 98, 101, 102
COBBLE_WALL, COAL_BLOCK, GRAVEL = 139, 173, 13
# wool colour = street props
WOOL = 35
P_SANDBAG, P_CONE, P_VENDING, P_STREETLIGHT = 0, 1, 2, 3
P_BARRIER, P_BIN, P_BENCH, P_DUMPSTER = 4, 5, 6, 7
P_TIRE, P_BARREL, P_CARDBOARD, P_TRASHBAG = 8, 9, 10, 11
P_ELECTRICBOX, P_SHELF, P_CANFIRE, P_ROADSIGN = 12, 13, 14, 15
# stained clay colour = road markings + furniture
CLAY = 159
F_CENTERLINE, F_YELLOWLINE, F_WOODTABLE, F_CHAIR = 0, 1, 2, 3
F_OFFICECHAIR, F_METALTABLE, F_MAILBOX, F_PHONEBOOTH = 4, 5, 6, 7
F_NEWSSTAND, F_STRETCHER, F_TRASHCAN, F_WASHER = 8, 9, 10, 11
F_COOKING, F_WEAPONCABINET, F_WIRE, F_BATTLEFIELD = 12, 13, 14, 15

PLACEHOLDERS = (WOODCRATE, AMMOCRATE, MILITARYCRATE, WRECKAGE, MEDICALCRATE,
                POLICECRATE, CHEST, WOOL, CLAY)


class Grid:
    def __init__(self, w, h, l):
        self.w, self.h, self.l = w, h, l
        self.blocks = bytearray([SKIP]) * (w * h * l)
        self.data = bytearray(w * h * l)

    def i(self, x, y, z):
        return (y * self.l + z) * self.w + x

    def set(self, x, y, z, block, meta=0):
        self.blocks[self.i(x, y, z)] = block
        self.data[self.i(x, y, z)] = meta

    def get(self, x, y, z):
        return self.blocks[self.i(x, y, z)]

    def box(self, x1, y1, z1, x2, y2, z2, block, meta=0, hollow=False):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    if hollow and x not in (x1, x2) and z not in (z1, z2):
                        continue
                    self.set(x, y, z, block, meta)

    def room(self, x1, y1, z1, x2, y2, z2):
        """Carve explicit air (clears terrain inside the building)."""
        self.box(x1, y1, z1, x2, y2, z2, AIR)


def nbt_schematic(g):
    out = io.BytesIO()

    def tag(tid, name):
        out.write(struct.pack(">bH", tid, len(name)) + name.encode())

    def short(name, v):
        tag(2, name); out.write(struct.pack(">h", v))

    def string(name, v):
        tag(8, name); out.write(struct.pack(">H", len(v)) + v.encode())

    def barray(name, v):
        tag(7, name); out.write(struct.pack(">i", len(v)) + bytes(v))

    tag(10, "Schematic")
    short("Width", g.w)
    short("Height", g.h)
    short("Length", g.l)
    string("Materials", "Alpha")
    barray("Blocks", g.blocks)
    barray("Data", g.data)
    out.write(b"\x00")  # TAG_End
    return gzip.compress(out.getvalue())


def decay(g, r, keep_by_y):
    """Knock random holes in solid blocks; higher = more ruined."""
    for y in range(g.h):
        keep = keep_by_y.get(y, 1.0)
        if keep >= 1.0:
            continue
        for x in range(g.w):
            for z in range(g.l):
                b = g.get(x, y, z)
                if b not in (AIR, SKIP) and b not in PLACEHOLDERS \
                        and r.random() > keep:
                    g.set(x, y, z, AIR)


def wall_material(r):
    roll = r.random()
    if roll < 0.15:
        return STONEBRICK, 1   # mossy
    if roll < 0.40:
        return STONEBRICK, 2   # cracked
    return STONEBRICK, 0


def shell(g, r, x1, z1, x2, z2, y1, y2, keep, mat=None):
    """Perimeter wall ring with decay."""
    for y in range(y1, y2 + 1):
        k = keep(y) if callable(keep) else keep
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if x not in (x1, x2) and z not in (z1, z2):
                    continue
                if r.random() > k:
                    continue
                if mat:
                    g.set(x, y, z, mat[0], mat[1] if len(mat) > 1 else 0)
                else:
                    b, m = wall_material(r)
                    g.set(x, y, z, b, m)


def civ_house_ruin():
    r = random.Random(41)
    g = Grid(11, 6, 9)
    for x in range(g.w):
        for z in range(g.l):
            edge = x in (0, g.w - 1) or z in (0, g.l - 1)
            g.set(x, 0, z, COBBLE if edge else PLANKS)
    g.room(1, 1, 1, 9, 4, 7)
    shell(g, r, 0, 0, 10, 8, 1, 3,
          lambda y: {1: 0.92, 2: 0.75, 3: 0.5}[y])
    g.set(5, 1, 0, AIR); g.set(5, 2, 0, AIR)          # doorway south
    for z in (3, 5):
        g.set(0, 2, z, AIR); g.set(g.w - 1, 2, z, AIR)  # windows
    shell(g, r, 0, 0, 10, 8, 4, 4, 0.18)
    for _ in range(5):
        g.set(r.randint(2, g.w - 3), 1, r.randint(2, g.l - 3),
              COBBLE if r.random() < 0.7 else MOSSY_COBBLE)
    # furnishing: table + chair + kitchen corner
    g.set(3, 1, 6, CLAY, F_WOODTABLE)
    g.set(4, 1, 6, CLAY, F_CHAIR)
    g.set(8, 1, 6, CLAY, F_COOKING)
    g.set(5, 1, 7, CHEST, 3)
    g.set(2, 1, 2, WOODCRATE)
    return g


def watchtower():
    r = random.Random(7)
    g = Grid(7, 9, 7)
    g.box(0, 0, 0, 6, 0, 6, COBBLE)
    g.room(1, 1, 1, 5, 7, 5)
    shell(g, r, 0, 0, 6, 6, 1, 8,
          lambda y: max(0.25, 1.0 - y * 0.09), mat=None)
    for y in range(1, g.h):
        for x in range(g.w):
            for z in range(g.l):
                if (x in (0, 6) or z in (0, 6)) and g.get(x, y, z) == STONEBRICK:
                    if r.random() < 0.6:
                        g.set(x, y, z, MOSSY_COBBLE if r.random() < 0.25 else COBBLE)
    g.set(3, 1, 0, AIR); g.set(3, 2, 0, AIR)
    for y in (3, 5):
        g.set(0, y, 3, AIR); g.set(g.w - 1, y, 3, AIR)
    g.set(3, 1, 5, CHEST, 3)
    g.set(1, 1, 1, AMMOCRATE)
    g.set(5, 1, 1, CLAY, F_BATTLEFIELD)
    return g


def civ_gas_station():
    """Roadside stop: short asphalt patch, canopy, shop. No big apron -
    the surroundings stay natural terrain."""
    r = random.Random(88)
    g = Grid(17, 7, 13)
    # asphalt only where the forecourt actually is
    for x in range(1, 16):
        for z in range(1, 8):
            g.set(x, 0, z, COAL_BLOCK)
    for x in range(3, 14, 2):
        g.set(x, 0, 4, CLAY, F_YELLOWLINE)  # faded lane marking
    g.room(1, 1, 1, 15, 5, 7)
    # canopy columns + slab roof over the pumps
    for cx in (3, 13):
        for cz in (2, 6):
            g.box(cx, 1, cz, cx, 4, cz, COBBLE_WALL)
    for x in range(2, 15):
        for z in range(1, 8):
            if r.random() < 0.7:
                g.set(x, 5, z, SLAB, 0)
    for px in (6, 10):
        g.set(px, 1, 4, DOUBLE_SLAB, 0)
        g.set(px, 2, 4, SLAB, 0)
    g.set(4, 1, 3, WRECKAGE)
    g.set(12, 1, 5, WRECKAGE)
    g.set(2, 1, 6, WOOL, P_TIRE)
    # shop building at the back (z 9..12)
    g.box(1, 0, 9, 15, 0, 12, STONE)
    g.room(2, 1, 10, 14, 3, 11)
    for y in (1, 2, 3):
        for x in range(1, 16):
            for z in (9, 12):
                if r.random() < (0.9 if y < 3 else 0.55):
                    g.set(x, y, z, BRICK)
        for z in (10, 11):
            if r.random() < 0.9:
                g.set(1, y, z, BRICK)
                g.set(15, y, z, BRICK)
    for x in (4, 5, 11, 12):
        g.set(x, 2, 9, GLASS_PANE if r.random() < 0.5 else AIR)
    g.set(8, 1, 9, AIR); g.set(8, 2, 9, AIR)
    for x in range(1, 16):
        for z in range(9, 13):
            if r.random() < 0.65:
                g.set(x, 4, z, SLAB, 0)
    # shop interior: shelves, counter, vending, loot
    g.set(3, 1, 11, WOOL, P_SHELF)
    g.set(5, 1, 11, WOOL, P_SHELF)
    g.set(10, 1, 11, WOOL, P_VENDING)
    g.set(12, 1, 11, CLAY, F_METALTABLE)
    g.set(2, 1, 10, WOODCRATE)
    g.set(13, 1, 10, AMMOCRATE)
    g.set(7, 1, 11, CHEST, 3)
    g.set(14, 1, 11, MEDICALCRATE)
    # outside clutter hugging the building
    g.set(0, 1, 10, WOOL, P_TRASHBAG)
    g.set(16, 1, 10, WOOL, P_ELECTRICBOX)
    g.set(16, 1, 3, WOOL, P_BARREL)
    return g


def civ_shed():
    r = random.Random(12)
    g = Grid(7, 5, 7)
    g.box(0, 0, 0, 6, 0, 6, DIRT)
    g.box(1, 0, 1, 5, 0, 5, PLANKS)
    g.room(1, 1, 1, 5, 3, 5)
    shell(g, r, 0, 0, 6, 6, 1, 2, 0.85, mat=(PLANKS,))
    shell(g, r, 0, 0, 6, 6, 3, 3, 0.4, mat=(PLANKS,))
    g.set(3, 1, 0, AIR); g.set(3, 2, 0, AIR)
    for x in range(g.w):
        for z in range(g.l):
            if r.random() < 0.5:
                g.set(x, 4, z, SLAB, 0)
    g.set(2, 1, 4, CLAY, F_WOODTABLE)
    g.set(4, 1, 4, WOODCRATE)
    g.set(5, 1, 2, WOOL, P_CARDBOARD)
    return g


def city_street():
    """Abandoned road segment with centerline, lights, bus stop, dead traffic."""
    r = random.Random(52)
    g = Grid(21, 7, 9)
    for x in range(g.w):
        for z in range(2, 7):
            g.set(x, 0, z, COAL_BLOCK)
        g.room(x, 1, 2, x, 4, 6)
    for x in range(0, 21, 2):
        g.set(x, 0, 4, CLAY, F_CENTERLINE)
    # sidewalk hint: double slab strip on the north side
    for x in range(g.w):
        if r.random() < 0.85:
            g.set(x, 0, 7, DOUBLE_SLAB, 0)
            g.set(x, 1, 7, AIR)
    # street lights both sides
    for x in (3, 10, 17):
        g.set(x, 1, 1, WOOL, P_STREETLIGHT)
        g.set(x + 1 if x < 17 else x - 1, 1, 7, WOOL, P_STREETLIGHT)
    # bus stop north side
    g.set(7, 1, 7, WOOL, P_BENCH)
    g.set(8, 1, 7, WOOL, P_BENCH)
    g.set(9, 1, 7, WOOL, P_BIN)
    g.set(5, 1, 7, WOOL, P_ROADSIGN)
    g.set(12, 1, 7, CLAY, F_MAILBOX)
    g.set(15, 1, 7, CLAY, F_PHONEBOOTH)
    g.set(18, 1, 7, CLAY, F_NEWSSTAND)
    # dead traffic + debris
    g.set(4, 1, 4, WRECKAGE)
    g.set(13, 1, 3, WRECKAGE)
    g.set(16, 1, 5, WRECKAGE)
    g.set(2, 1, 6, WOOL, P_TIRE)
    g.set(11, 1, 6, WOOL, P_TRASHBAG)
    g.set(19, 1, 2, WOOL, P_CARDBOARD)
    g.set(1, 1, 2, WOOL, P_DUMPSTER)
    g.set(20, 1, 7, WOODCRATE)
    return g


def city_office():
    """Three-storey office block, top floor torn open."""
    r = random.Random(77)
    g = Grid(15, 13, 13)
    g.box(0, 0, 0, 14, 0, 12, STONE)
    g.room(1, 1, 1, 13, 11, 11)
    # storeys: walls y1-3, floor y4, walls y5-7, floor y8, walls y9-11
    shell(g, r, 0, 0, 14, 12, 1, 3, 0.93, mat=(BRICK,))
    for x in range(g.w):
        for z in range(g.l):
            if r.random() < 0.9:
                g.set(x, 4, z, STONE)
    shell(g, r, 0, 0, 14, 12, 5, 7, 0.8, mat=(BRICK,))
    for x in range(g.w):
        for z in range(g.l):
            if x < 9 or r.random() < 0.3:   # east half of top floor gone
                g.set(x, 8, z, STONE)
    shell(g, r, 0, 0, 14, 12, 9, 11,
          lambda y: {9: 0.55, 10: 0.35, 11: 0.2}[y], mat=(BRICK,))
    # entrance + ground windows
    g.set(7, 1, 0, AIR); g.set(7, 2, 0, AIR); g.set(8, 1, 0, AIR); g.set(8, 2, 0, AIR)
    for x in (3, 11):
        for zz in (0, 12):
            g.set(x, 2, zz, GLASS_PANE)
    # stairwells (oak stairs, east wall)
    for i, sy in enumerate(range(1, 4)):
        g.set(12, sy, 2 + i, OAK_STAIRS, 2)
        g.set(12, sy - 1 if sy > 1 else 0, 2 + i, STONE)
    for i, sy in enumerate(range(5, 8)):
        g.set(12, sy, 5 + i, OAK_STAIRS, 2)
    # office furnishing, ground floor
    g.set(3, 1, 3, CLAY, F_METALTABLE); g.set(4, 1, 3, CLAY, F_OFFICECHAIR)
    g.set(3, 1, 6, CLAY, F_METALTABLE); g.set(4, 1, 6, CLAY, F_OFFICECHAIR)
    g.set(8, 1, 9, WOOL, P_SHELF); g.set(9, 1, 9, WOOL, P_SHELF)
    g.set(2, 1, 10, CLAY, F_TRASHCAN)
    g.set(6, 1, 5, WOOL, P_CARDBOARD)
    # floor 2
    g.set(4, 5, 4, CLAY, F_METALTABLE); g.set(5, 5, 4, CLAY, F_OFFICECHAIR)
    g.set(9, 5, 8, WOOL, P_CARDBOARD)
    g.set(2, 5, 9, CHEST, 3)
    # top floor loot (risky climb)
    g.set(3, 9, 5, MEDICALCRATE)
    g.set(6, 9, 8, WOODCRATE)
    return g


def city_shop():
    r = random.Random(35)
    g = Grid(13, 6, 11)
    g.box(0, 0, 0, 12, 0, 10, STONE)
    g.room(1, 1, 1, 11, 3, 9)
    shell(g, r, 0, 0, 12, 10, 1, 3, 0.88, mat=(BRICK,))
    # glass shopfront south
    for x in range(2, 11):
        if x in (6,):
            continue
        g.set(x, 1, 0, GLASS_PANE if r.random() < 0.55 else AIR)
        g.set(x, 2, 0, GLASS_PANE if r.random() < 0.55 else AIR)
    g.set(6, 1, 0, AIR); g.set(6, 2, 0, AIR)   # entrance
    for x in range(g.w):
        for z in range(g.l):
            if r.random() < 0.7:
                g.set(x, 4, z, SLAB, 0)
    # aisles: shelf rows
    for z in (3, 6):
        for x in (3, 4, 5, 8, 9):
            g.set(x, 1, z, WOOL, P_SHELF)
    g.set(11, 1, 8, WOOL, P_VENDING)
    g.set(2, 1, 8, CLAY, F_METALTABLE)  # counter
    g.set(2, 1, 9, WOODCRATE)
    g.set(10, 1, 9, CHEST, 3)
    g.set(6, 1, 9, WOOL, P_CARDBOARD)
    # back alley clutter north
    g.set(3, 1, 10, WOOL, P_DUMPSTER)
    g.set(9, 1, 10, WOOL, P_TRASHBAG)
    return g


def mil_outpost():
    r = random.Random(66)
    g = Grid(15, 8, 15)
    g.box(2, 0, 2, 12, 0, 12, STONE)
    g.room(1, 1, 1, 13, 4, 13)
    # sandbag perimeter with a south gap
    for x in range(g.w):
        for z in range(g.l):
            if (x in (0, g.w - 1) or z in (0, g.l - 1)) and r.random() < 0.85:
                g.set(x, 0, z, DIRT)
                g.set(x, 1, z, WOOL, P_SANDBAG)
    g.set(7, 1, 0, AIR)
    # concertina wire on the corners
    for cx, cz in ((0, 0), (14, 0), (0, 14), (14, 14)):
        g.set(cx, 2, cz, CLAY, F_WIRE)
    # bunker
    g.box(4, 1, 6, 10, 3, 12, STONEBRICK, hollow=True)
    g.room(5, 1, 7, 9, 2, 11)
    for x in range(4, 11):
        for z in range(6, 13):
            if r.random() < 0.85:
                g.set(x, 4, z, SLAB, 0)
    g.set(7, 1, 6, AIR); g.set(7, 2, 6, AIR)
    g.set(4, 2, 9, AIR); g.set(10, 2, 9, AIR)
    # comms mast stub
    for y in range(1, 7):
        if y < 6 or r.random() < 0.5:
            g.set(12, y, 2, IRON_BARS)
    # gate defenses + battlefield leftovers
    g.set(4, 1, 2, WRECKAGE)
    g.set(6, 1, 3, CLAY, F_BATTLEFIELD)
    g.set(9, 1, 2, CLAY, F_BATTLEFIELD)
    g.set(11, 1, 4, WOOL, P_CANFIRE)
    # bunker interior
    g.set(5, 1, 11, MILITARYCRATE)
    g.set(9, 1, 11, AMMOCRATE)
    g.set(7, 1, 10, CHEST, 3)
    g.set(9, 1, 7, MEDICALCRATE)
    g.set(5, 1, 7, CLAY, F_WEAPONCABINET)
    return g


def mil_checkpoint():
    r = random.Random(19)
    g = Grid(15, 6, 9)
    for x in range(g.w):
        for z in range(2, 7):
            g.set(x, 0, z, COAL_BLOCK)
        g.room(x, 1, 2, x, 3, 6)
    for x in range(g.w):
        g.set(x, 0, 5, CLAY, F_CENTERLINE) if x % 2 == 0 else None
    # sandbag line with squeeze-through gap + tank traps
    for x in range(0, 15):
        if x == 7:
            continue
        if r.random() < 0.8:
            g.set(x, 1, 4, WOOL, P_SANDBAG)
    g.set(3, 1, 3, CLAY, F_BATTLEFIELD)
    g.set(11, 1, 5, CLAY, F_BATTLEFIELD)
    g.set(5, 1, 2, WOOL, P_CONE)
    g.set(9, 1, 2, WOOL, P_CONE)
    g.set(6, 1, 6, WOOL, P_CONE)
    g.set(0, 1, 7, WOOL, P_ROADSIGN)
    # booth
    g.box(10, 1, 0, 13, 3, 3, STONEBRICK, hollow=True)
    g.room(11, 1, 1, 12, 2, 2)
    g.set(11, 1, 3, AIR); g.set(11, 2, 3, AIR)
    g.set(10, 2, 1, GLASS_PANE)
    for x in range(10, 14):
        for z in range(0, 4):
            if r.random() < 0.7:
                g.set(x, 4, z, SLAB, 0)
    g.set(5, 1, 3, WRECKAGE)
    g.set(8, 1, 6, WRECKAGE)
    g.set(12, 1, 1, POLICECRATE)
    g.set(12, 1, 2, CLAY, F_CHAIR)
    g.set(10, 1, 2, CHEST, 3)
    g.set(2, 1, 5, AMMOCRATE)
    return g


def mil_barracks():
    """Field barracks: two bunk rooms, armory corner, wired perimeter."""
    r = random.Random(99)
    g = Grid(19, 7, 13)
    g.box(1, 0, 1, 17, 0, 11, STONE)
    g.room(2, 1, 2, 16, 3, 10)
    shell(g, r, 1, 1, 17, 11, 1, 3,
          lambda y: {1: 0.95, 2: 0.85, 3: 0.6}[y])
    # doorway south + windows
    g.set(9, 1, 1, AIR); g.set(9, 2, 1, AIR)
    for x in (4, 14):
        g.set(x, 2, 1, AIR); g.set(x, 2, 11, AIR)
    # partition wall between the two rooms
    for z in range(2, 11):
        if z != 6 and r.random() < 0.85:
            g.set(9, 1, z, STONEBRICK)
            g.set(9, 2, z, STONEBRICK)
    # slab roof, half gone
    for x in range(1, 18):
        for z in range(1, 12):
            if r.random() < 0.55:
                g.set(x, 4, z, SLAB, 0)
    # west room: bunks (double slabs) + stretcher + medical
    for z in (3, 5, 8):
        g.set(3, 1, z, DOUBLE_SLAB, 0)
        g.set(4, 1, z, DOUBLE_SLAB, 0)
    g.set(6, 1, 9, CLAY, F_STRETCHER)
    g.set(2, 1, 10, MEDICALCRATE)
    # east room: armory
    g.set(15, 1, 3, CLAY, F_WEAPONCABINET)
    g.set(15, 1, 5, CLAY, F_WEAPONCABINET)
    g.set(16, 1, 8, MILITARYCRATE)
    g.set(13, 1, 9, AMMOCRATE)
    g.set(11, 1, 3, CHEST, 3)
    g.set(12, 1, 6, CLAY, F_METALTABLE)
    # wire + sandbags out front
    for x in (3, 7, 11, 15):
        g.set(x, 1, 0, CLAY, F_WIRE)
    g.set(5, 1, 0, WOOL, P_SANDBAG)
    g.set(13, 1, 0, WOOL, P_SANDBAG)
    g.set(0, 1, 6, WOOL, P_CANFIRE)
    return g


def survivor_camp():
    r = random.Random(33)
    g = Grid(13, 6, 13)
    for x in range(g.w):
        for z in range(g.l):
            if r.random() < 0.3:
                g.set(x, 0, z, GRAVEL if r.random() < 0.5 else DIRT)
    g.set(6, 1, 6, WOOL, P_CANFIRE)
    g.set(7, 1, 6, CLAY, F_COOKING)
    # lean-to shelter 1
    g.box(1, 1, 1, 4, 2, 1, PLANKS)
    g.room(1, 1, 2, 4, 1, 3)
    for x in range(1, 5):
        for z in range(2, 4):
            if r.random() < 0.8:
                g.set(x, 2, z, SLAB, 0)
    g.set(2, 1, 2, CHEST, 3)
    # lean-to shelter 2
    g.box(11, 1, 8, 11, 2, 11, PLANKS)
    g.room(9, 1, 8, 10, 1, 11)
    for x in range(9, 12):
        for z in range(8, 12):
            if r.random() < 0.8:
                g.set(x, 2, z, SLAB, 0)
    g.set(10, 1, 10, WOODCRATE)
    # camp clutter
    g.set(9, 1, 3, WOOL, P_CARDBOARD)
    g.set(4, 1, 9, WOOL, P_BARREL)
    g.set(8, 1, 6, WOOL, P_BENCH)
    g.set(3, 1, 6, WOOL, P_TRASHBAG)
    g.set(6, 1, 10, WOOL, P_TIRE)
    g.set(11, 1, 2, MEDICALCRATE)
    g.set(2, 1, 11, CLAY, F_BATTLEFIELD)
    for x in range(g.w):
        for z in range(g.l):
            if (x in (0, g.w - 1) or z in (0, g.l - 1)) and r.random() < 0.25:
                g.set(x, 1, z, FENCE)
    return g


BUILDINGS = (
    ("civ_house_ruin", civ_house_ruin),
    ("civ_gas_station", civ_gas_station),
    ("civ_shed", civ_shed),
    ("city_street", city_street),
    ("city_office", city_office),
    ("city_shop", city_shop),
    ("mil_outpost", mil_outpost),
    ("mil_checkpoint", mil_checkpoint),
    ("mil_barracks", mil_barracks),
    ("watchtower", watchtower),
    ("survivor_camp", survivor_camp),
)


def mil_compound():
    """Large walled military base, 48x48: LARGE structure (structures/large/)."""
    r = random.Random(77)
    g = Grid(48, 14, 48)
    W = 48
    # perimeter wall ring (3 high) with decay, gate gap on the south side
    for x in range(2, W - 2):
        for z in range(2, W - 2):
            if x not in (2, W - 3) and z not in (2, W - 3):
                continue
            g.set(x, 0, z, STONEBRICK)
            for y in range(1, 4):
                if r.random() < (0.92 if y < 3 else 0.7):
                    b, m = wall_material(r)
                    g.set(x, y, z, b, m)
    for x in range(21, 27):          # gate opening
        for y in range(1, 4):
            g.set(x, y, W - 3, AIR)
    # corner watchtowers 5x5, 10 high, open top platform
    for cx, cz in ((0, 0), (W - 5, 0), (0, W - 5), (W - 5, W - 5)):
        g.box(cx, 0, cz, cx + 4, 0, cz + 4, STONEBRICK)
        g.box(cx, 1, cz, cx + 4, 8, cz + 4, STONEBRICK, hollow=True)
        g.room(cx + 1, 1, cz + 1, cx + 3, 8, cz + 3)
        g.box(cx, 9, cz, cx + 4, 9, cz + 4, SLAB)
        for y in range(1, 9):
            g.set(cx + 2, y, cz + 1, 65, 3)  # ladder against the north inner wall
        g.set(cx + 2, 9, cz + 1, AIR)
        for x in range(cx, cx + 5):          # sandbag parapet
            for z in range(cz, cz + 5):
                if x in (cx, cx + 4) or z in (cz, cz + 4):
                    if r.random() < 0.8:
                        g.set(x, 10, z, WOOL, P_SANDBAG)
        g.set(cx + 2, 1, cz + 4 if cz == 0 else cz, AIR)   # door toward the yard
        g.set(cx + 2, 2, cz + 4 if cz == 0 else cz, AIR)
        g.set(cx + 1, 10, cz + 1, MILITARYCRATE)
    # gate defences outside
    for x in (18, 19, 28, 29):
        g.set(x, 1, W - 2, WOOL, P_SANDBAG)
        g.set(x, 2, W - 2, WOOL, P_SANDBAG)
    for x in range(16, 32, 3):
        g.set(x, 1, W - 1, CLAY, F_WIRE)
    g.set(23, 1, W - 1, WOOL, P_BARRIER)
    # road from the gate to the plaza, plaza with a helipad
    for z in range(20, W - 2):
        for x in range(21, 27):
            g.set(x, 0, z, COAL_BLOCK)
    for x in range(14, 34):
        for z in range(8, 20):
            g.set(x, 0, z, STONE)
            g.room(x, 1, z, x, 3, z)
    for x in range(19, 29):
        for z in range(10, 18):
            g.set(x, 0, z, CLAY, F_YELLOWLINE if (x in (19, 28) or z in (10, 17)) else F_CENTERLINE)
    g.set(16, 1, 9, WRECKAGE); g.set(31, 1, 18, WRECKAGE)
    # two barracks, 12x8, flat roofs, windows, loot
    for bx in (5, 31):
        bz = 24
        g.box(bx, 0, bz, bx + 11, 0, bz + 7, STONEBRICK)
        g.box(bx, 1, bz, bx + 11, 4, bz + 7, STONEBRICK, hollow=True)
        g.room(bx + 1, 1, bz + 1, bx + 10, 4, bz + 6)
        for x in range(bx, bx + 12):
            for z in range(bz, bz + 8):
                if r.random() < 0.88:
                    g.set(x, 5, z, SLAB)
        for x in range(bx + 2, bx + 11, 3):
            g.set(x, 2, bz, GLASS_PANE if r.random() < 0.5 else AIR)
            g.set(x, 2, bz + 7, GLASS_PANE if r.random() < 0.5 else AIR)
        door_x = bx + 11 if bx < 24 else bx
        g.set(door_x, 1, bz + 4, AIR); g.set(door_x, 2, bz + 4, AIR)
        g.set(bx + 1, 1, bz + 1, MILITARYCRATE)
        g.set(bx + 10, 1, bz + 6, AMMOCRATE)
        g.set(bx + 5, 1, bz + 1, CLAY, F_WEAPONCABINET)
        g.set(bx + 7, 1, bz + 6, MEDICALCRATE)
        for x in range(bx + 2, bx + 10, 2):
            g.set(x, 1, bz + 3, CLAY, F_METALTABLE if r.random() < 0.5 else F_CHAIR)
    # yard clutter
    for _ in range(14):
        x, z = r.randint(5, W - 6), r.randint(34, W - 5)
        if g.get(x, 1, z) == SKIP:
            g.set(x, 1, z, WOOL, r.choice((P_BARREL, P_TIRE, P_CARDBOARD, P_SANDBAG)))
    return g


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in BUILDINGS:
        g = fn()
        assert g.w <= 24 and g.l <= 24, name + " footprint too big"
        path = os.path.join(OUT, name + ".schematic")
        with open(path, "wb") as f:
            f.write(nbt_schematic(g))
        print("wrote %s (%dx%dx%d, %d bytes)"
              % (path, g.w, g.h, g.l, os.path.getsize(path)))
    large_out = os.path.join(OUT, "large")
    os.makedirs(large_out, exist_ok=True)
    for name, fn in LARGE:
        g = fn()
        path = os.path.join(large_out, name + ".schematic")
        with open(path, "wb") as f:
            f.write(nbt_schematic(g))
        print("wrote %s (%dx%dx%d, %d bytes)"
              % (path, g.w, g.h, g.l, os.path.getsize(path)))


LARGE = [("mil_compound", mil_compound)]


if __name__ == "__main__":
    main()
