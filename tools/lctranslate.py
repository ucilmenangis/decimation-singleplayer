"""Rule based translation of 1.20 block states (vanilla + furniture mods) to
1.7.10 blocks + metadata, with Decimation props for furniture.

translate("minecraft:oak_stairs[facing=north,half=bottom]") -> ("minecraft:oak_stairs", 3)
Returns ("skip", 0) for "leave the world as is" (structure void), ("minecraft:air", 0)
for air, None when nothing matches (reported by the caller).
Only one cell props are used for furniture (docs/prop_footprints.tsv), so a
translated model never draws over its neighbours.
"""
import re

COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
           "purple", "blue", "brown", "green", "red", "black"]
WOODS = {"oak": 0, "spruce": 1, "birch": 2, "jungle": 3, "acacia": 4, "dark_oak": 5,
         # newer woods -> closest look
         "mangrove": 5, "cherry": 2, "crimson": 5, "warped": 1, "bamboo": 2}
STAIR_FACING = {"east": 0, "west": 1, "south": 2, "north": 3}
# a seat (sofa, toilet) stair: the sitter faces this way -> stair meta
SEAT = {"west": 0, "east": 1, "north": 2, "south": 3}
VANILLA_FACE = {"north": 2, "south": 3, "west": 4, "east": 5}
PROP_FACE = {"east": 2, "south": 3, "west": 4, "north": 5}   # Decimation BlockProp front
BED_DIR = {"south": 0, "west": 1, "north": 2, "east": 3}
TRAPDOOR_SIDE = {"north": 0, "south": 1, "west": 2, "east": 3}
DOOR_FACE = {"east": 0, "south": 1, "west": 2, "north": 3}
TORCH = {"east": 1, "west": 2, "south": 3, "north": 4}


def parse(state):
    m = re.match(r"([^\[]+)(?:\[(.*)\])?$", state.strip())
    name, props = m.group(1), m.group(2) or ""
    p = dict(kv.split("=", 1) for kv in props.split(",") if "=" in kv)
    ns, _, path = name.partition(":")
    return ns, path, p


def colour_of(path):
    for c in sorted(COLOURS, key=len, reverse=True):
        if path.startswith(c + "_") or ("_" + c + "_") in path:
            return COLOURS.index(c)
    return None


def wood_of(path):
    for w in sorted(WOODS, key=len, reverse=True):
        if w in path:
            return w
    return None


# dark stones, matched before plain sandstone (also by slab_meta / stair_block)
DARK_STONES = ("deepslate", "blackstone", "basalt", "tuff", "obsidian", "gneiss", "slate", "black_sandstone", "scorchia")

# full block material by words in the name; first match wins
MATERIAL = [
    (("quartz", "calcite", "marble", "white_concrete", "snow"), ("minecraft:quartz_block", 0)),
    (("nether_brick",), ("minecraft:nether_brick", 0)),
    (("stone_brick", "stonebrick", "tile", "deepslate_brick", "concrete_brick", "concretbrick"),
     ("minecraft:stonebrick", 0)),
    (("brick",), ("minecraft:brick_block", 0)),
    (("mossy",), ("minecraft:mossy_cobblestone", 0)),
    (("cobble",), ("minecraft:cobblestone", 0)),
    # dark stones before plain sandstone: "black_sandstone" (DeceasedCraft's asphalt) once
    # matched "sandstone" and came out beige (audit 2026-10-09)
    (DARK_STONES, ("deci:BlockStone_4", 0)),
    (("sandstone",), ("minecraft:sandstone", 2)),
    (("limestone", "diorite", "smooth_stone", "concrete", "hempcrete", "cement", "plaster"),
     ("deci:BlockStone_7", 0)),
    (("andesite", "granite", "stone", "rock", "asphalt"), ("minecraft:stone", 0)),
    (("iron", "steel", "metal", "sheet", "corrugated", "aluminum", "plate", "copper", "casing"),
     ("deci:BlockMetal_2", 0)),
    (("dirt", "mud", "soil", "farmland", "path"), ("minecraft:dirt", 0)),
    (("grass_block",), ("minecraft:grass", 0)),
    (("moss",), ("minecraft:leaves", 4)),
    (("gravel",), ("minecraft:gravel", 0)),
    (("sand",), ("minecraft:sand", 0)),
    (("clay",), ("minecraft:hardened_clay", 0)),
]


def material(path):
    for keys, res in MATERIAL:
        if any(k in path for k in keys):
            return res
    return None


def slab_meta(path):
    """1.7.10 stone_slab / wooden_slab for a material."""
    w = wood_of(path)
    if w and "stone" not in path:
        return "minecraft:wooden_slab", WOODS[w]
    if "quartz" in path or "calcite" in path or "marble" in path:
        return "minecraft:stone_slab", 7
    if "nether_brick" in path:
        return "minecraft:stone_slab", 6
    if "brick" in path and "stone" not in path:
        return "minecraft:stone_slab", 4
    if "stone_brick" in path or "tile" in path:
        return "minecraft:stone_slab", 5
    if "cobble" in path:
        return "minecraft:stone_slab", 3
    if any(d in path for d in DARK_STONES):
        return "minecraft:stone_slab", 3  # cobblestone: the darkest grey slab
    if "sandstone" in path:
        return "minecraft:stone_slab", 1
    return "minecraft:stone_slab", 0


def stair_block(path):
    w = wood_of(path)
    if w and "stone" not in path:
        return {"oak": "minecraft:oak_stairs", "spruce": "minecraft:spruce_stairs", "birch": "minecraft:birch_stairs",
                "jungle": "minecraft:jungle_stairs", "acacia": "minecraft:acacia_stairs",
                "dark_oak": "minecraft:dark_oak_stairs"}.get(
            {0: "oak", 1: "spruce", 2: "birch", 3: "jungle", 4: "acacia", 5: "dark_oak"}[WOODS[w]])
    if "quartz" in path or "calcite" in path or "marble" in path:
        return "minecraft:quartz_stairs"
    if "nether_brick" in path:
        return "minecraft:nether_brick_stairs"
    if "brick" in path and "stone" not in path:
        return "minecraft:brick_stairs"
    if any(d in path for d in DARK_STONES):
        return "minecraft:stone_stairs"
    if "sandstone" in path:
        return "minecraft:sandstone_stairs"
    if "cobble" in path or "mossy" in path:
        return "minecraft:stone_stairs"
    return "minecraft:stone_brick_stairs"


def furniture(ns, path, p):
    """Furniture of any mod -> one cell prop or vanilla stand-in; None if not furniture."""
    face = p.get("facing", "south")
    if path.endswith("_bed") or path == "bed" or "fancy_bed" in path:
        m = BED_DIR.get(face, 0)
        return "minecraft:bed", m | (8 if p.get("part") == "head" else 0)
    if "toilet" in path:
        return "minecraft:quartz_stairs", SEAT.get(face, 3)
    if any(k in path for k in ("basin", "kitchen_sink", "sink", "bathtub", "_bath")):
        return "minecraft:cauldron", 0
    if any(k in path for k in ("fridge", "freezer")):
        return "minecraft:iron_block", 0
    if any(k in path for k in ("stove", "oven", "cooker")):
        return "minecraft:furnace", VANILLA_FACE.get(face, 3)
    if "washing" in path:
        return "deci:BlockWashingMachine", PROP_FACE.get(face, 3)
    if any(k in path for k in ("kitchen_cabinetry", "kitchen_drawer", "counter", "kitchen_counter")):
        return "minecraft:double_stone_slab", 0
    if any(k in path for k in ("sofa", "couch", "armchair")):
        return "minecraft:spruce_stairs", SEAT.get(face, 3)
    if "office_chair" in path or "computer_chair" in path:
        return "deci:BlockOfficeChair", PROP_FACE.get(face, 3)
    if "chair" in path or "stool" in path:
        return "deci:BlockChair", PROP_FACE.get(face, 3)
    if "bench" in path:
        return "deci:BlockStreetBench", PROP_FACE.get(face, 3)
    if any(k in path for k in ("desk", "table")):
        w = wood_of(path)
        return "minecraft:wooden_slab", 8 | (WOODS[w] if w else 0)
    if "bookshelf" in path or "bookcase" in path:
        return "minecraft:bookshelf", 0
    if any(k in path for k in ("drawer", "cabinet", "wardrobe", "dresser", "cupboard", "locker", "storage")):
        w = wood_of(path)
        return "minecraft:planks", WOODS[w] if w else 0
    if "shelf" in path:
        return "deci:BlockCardboardBoxes3", PROP_FACE.get(face, 3)
    if "lootr" in ns or path.endswith("chest") or "crate" in path:
        return "deci:BlockWoodCrate", 2
    if any(k in path for k in ("monitor", "computer", "laptop")):
        return "deci:BlockMonitor", PROP_FACE.get(face, 3)
    if any(k in path for k in ("tv", "television")):
        return "deci:BlockMonitor", PROP_FACE.get(face, 3)
    if "radio" in path or "stereo" in path:
        return "deci:BlockRadio1", PROP_FACE.get(face, 3)
    if "trash" in path or "recycle" in path or "bin" == path.split("_")[-1]:
        return "deci:BlockTrashcan", 2
    if "vending" in path:
        return "deci:BlockVendingMachine_1", PROP_FACE.get(face, 3)
    lp = unlit(path)
    # full light blocks: an unlit lamp block (a hanging lamp prop sat in walls and floors)
    if "illuminant" in lp and (lp.endswith("_block") or lp.endswith("_block_on") or lp == "illuminant"):
        return "minecraft:redstone_lamp", 0
    if any(k in lp for k in ("illuminant", "ceiling_light", "ceiling_fan", "lightbulb", "edge_light",
                             "fluorescent")):
        return "deci:BlockLightOff", 2
    if any(k in lp for k in ("lamp", "lantern")) and "rodlamp" not in lp:
        return "deci:BlockLantern", 2
    if "potted" in path or "flower_pot" in path or "plant_pot" in path:
        return "minecraft:flower_pot", 0
    if "curtain" in path or "blind" in path:
        c = colour_of(path)
        return "minecraft:carpet" if False else "minecraft:wool", c if c is not None else 8
    if "towel" in path or "toalla" in path or "papel" in path or "regadera" in path or "llaves" in path:
        return "skip", 0
    if (path.endswith("_pan") or path == "pan") and "pane" not in path:
        return "skip", 0
    if any(k in path for k in ("plate", "toaster", "microwave", "range_hood", "jar", "tray", "bread",
                               "lightswitch", "switch", "doorbell", "painting", "picture", "clock", "mirror",
                               "book", "cup", "mug", "bottle", "phone", "keyboard")) and "pressure" not in path \
            and "metal_plate" not in path:
        return "skip", 0
    return None


def unlit(path):
    """The name without words that only look like "light": light_gray / light_blue
    (colours), lightning (rods), daylight (detector). The lamp rules once turned
    5429 light_gray corrugated metal plates into lamps, many of them floors."""
    for w in ("light_gray", "light_grey", "light_blue", "lightning", "daylight"):
        path = path.replace(w, "")
    return path


def translate(state):
    ns, path, p = parse(state)
    if path in ("air", "cave_air", "void_air"):
        return "minecraft:air", 0
    if path in ("structure_void", "barrier", "light", "jigsaw", "structure_block"):
        return "skip", 0
    if ns != "minecraft" or path in ("player_head",):
        f = furniture(ns, path, p)
        if f:
            return f
    else:
        f = furniture(ns, path, p) if any(k in path for k in ("bed", "chest", "lantern")) else None
        if f and not path.endswith(("_stairs", "_slab")):
            return f
    face = p.get("facing", "north")
    half = p.get("half", "bottom")
    c = colour_of(path)
    path = re.sub(r"_\d+$", "", path)  # buildersdelight variants: oak_stairs_1, jungle_planks_6
    if "window" in path:
        return "minecraft:glass_pane", 0
    # audit 2026-10-09: blocks 1.7.10 has, and the most common dropped ones
    if path in ("redstone_lamp", "daylight_detector"):
        return "minecraft:" + path, 0
    if path in ("black_sandstone", "smooth_black_sandstone"):
        return "deci:BlockRoad", 0  # DeceasedCraft's asphalt
    if "laboratory" in path:
        return "deci:BlockStone_7", 0  # buildersdelight lab panels, white [not verified look]
    if path == "charcoal_block":
        return "minecraft:coal_block", 0
    if path == "magma_block":
        return "minecraft:netherrack", 0
    if "wallpaper" in path:
        return ("minecraft:sandstone", 2) if "beige" in path else ("minecraft:stained_hardened_clay", c if c is not None else 0)
    if "corundum" in path:
        if "cluster" in path or path.endswith("_bud"):
            return "skip", 0  # crystal growths: decoration only
        return "minecraft:stained_glass", c if c is not None else 0
    if (ns == "create" and path.endswith("_seat")) or (ns == "redeco" and path.endswith("_cushion")):
        return "minecraft:carpet", c if c is not None else 0  # low seat pad
    if path in ("end_rod", "lightning_rod") or path.endswith("_rod"):
        return "minecraft:iron_bars", 0
    if path == "bamboo_mat":
        return "minecraft:carpet", 4
    if "barricade" in path and "construction" not in path:
        return "minecraft:fence", 0  # boarded up opening: blocks the way, stays see-through
    if path.endswith("_post") and ns == "quark":
        return "minecraft:fence", 0
    if ns == "refueled" and path == "post":
        return "minecraft:cobblestone_wall", 0
    if "shelves" in path:
        return "deci:BlockCardboardBoxes3", PROP_FACE.get(face, 3)
    if "air_duct" in path or "vent" in path:
        return "minecraft:iron_block" if "vent" not in path else "deci:BlockCeilingVent", 0 if "vent" not in path else 2
    if path in ("spawner", "gold_block") or "plushie" in ns or "decal" in path \
            or "documento" in path or "libro" in path or "display_board" in path or "fluid_pipe" in path:
        return "skip", 0
    if "lavabo" in path or "water_cauldron" in path:
        return "minecraft:cauldron", 0
    # wasteland / industrial (legacy deadzone district)
    if path in ("dried_salt", "salt_block", "salt"):  # not basalt
        return "deci:BlockStone_8", 0  # pale grey salt flat
    if "paving" in path:
        return ("deci:BlockStone_5" if "moist" in path else "deci:BlockStone_6"), 0
    if "scoria" in path or "coral" in path:
        return "deci:BlockStone_4", 0
    if path == "oxeye_daisy":
        return "minecraft:red_flower", 8
    if path in ("lily_pad", "huge_lily_pad"):
        return "minecraft:waterlily", 0
    if "ochrum" in path:
        return "minecraft:sandstone", 2
    if path == "construction_barricade":
        return "deci:BlockHazardbarrier", 2
    if path in ("command_block", "observer"):
        return "skip", 0
    if path == "dead_grass" or path == "dead_bush":
        return "minecraft:deadbush", 0
    if "razor_wire" in path or "barbed" in path:
        return "deci:BlockConcertinaWire", 2
    if "barrel" in path:
        return "deci:BlockBarrel", 2
    if path == "pallet":
        return "minecraft:wooden_slab", 0
    if any(k in path for k in ("cogwheel", "gearbox", "shaft", "blaze_burner", "engineering", "generator",
                                "workshop", "industrial", "machine", "pipe", "tank")):
        return "deci:BlockMetal_1", 0
    if "shingles" in path:
        c = colour_of(path)
        return "minecraft:stained_hardened_clay", c if c is not None else 7
    if ns == "car" or "crudeoil" in path or "cans" in path or "stick" in path:
        return "skip", 0
    if path == "coal_block":
        return "minecraft:coal_block", 0
    if path == "hopper":
        return "minecraft:hopper", 0
    # ---- shapes
    if path.endswith("_stairs"):
        return stair_block(path), STAIR_FACING.get(face, 3) | (4 if half == "top" else 0)
    if path.endswith("vertical_slab"):
        w = wood_of(path)
        if w and "stone" not in path:
            return "minecraft:planks", WOODS[w]
        return material(path) or ("minecraft:stone", 0)
    if path.endswith("_slab"):
        name, m = slab_meta(path)
        t = p.get("type", "bottom")
        if t == "double":
            return ("minecraft:double_wooden_slab" if name == "minecraft:wooden_slab" else "minecraft:double_stone_slab"), m
        return name, m | (8 if t == "top" else 0)
    if path.endswith("_wall") and "sign" not in path and "torch" not in path and "banner" not in path:
        return "minecraft:cobblestone_wall", 1 if "mossy" in path else 0
    if "fence_gate" in path:
        return "minecraft:fence_gate", 0
    if path.endswith("fence") or "railing" in path or "picket" in path:
        if any(k in path for k in ("iron", "metal", "steel")):
            return "minecraft:iron_bars", 0
        return "minecraft:fence", 0
    if "bars" in path or "chain" in path or "grate" in path or "mesh" in path:
        return "minecraft:iron_bars", 0
    if "trapdoor" in path or "shutter" in path:
        m = TRAPDOOR_SIDE.get(face, 0) | (4 if p.get("open") == "true" else 0) | (8 if half == "top" else 0)
        return ("minecraft:iron_trapdoor" if False else "minecraft:trapdoor"), m
    if path.endswith("door") or "_door" in path:
        iron = any(k in path for k in ("iron", "metal", "steel", "andesite", "security"))
        name = "minecraft:iron_door" if iron else "minecraft:wooden_door"
        if p.get("half") == "upper":
            return name, 8 | (1 if p.get("hinge") == "right" else 0)
        return name, DOOR_FACE.get(face, 0) | (4 if p.get("open") == "true" else 0)
    if "pane" in path or ("glass" in path and "framed" in path):
        if c is not None:
            return "minecraft:stained_glass_pane", c
        return "minecraft:glass_pane", 0
    if "glass" in path:
        if c is not None:
            return "minecraft:stained_glass", c
        return "minecraft:glass", 0
    if "carpet" in path and "leaf" not in path:
        return "minecraft:carpet", c if c is not None else 8
    if "leaf" in path or "leaves" in path or "hedge" in path or "azalea" in path:
        return "minecraft:leaves", 4
    if "wool" in path:
        return "minecraft:wool", c if c is not None else 0
    if "terracotta" in path:
        if c is None:
            return "minecraft:hardened_clay", 0
        return "minecraft:stained_hardened_clay", c
    if "concrete" in path and c is not None:
        return "minecraft:stained_hardened_clay", c
    if "ladder" in path:
        return "minecraft:ladder", VANILLA_FACE.get(face, 2)
    if "torch" in path or "candle" in path:
        return "minecraft:torch", TORCH.get(face, 5) if "wall" in path else 5
    if path.endswith("_button"):
        # 1.7 buttons only hang on walls: 1 east, 2 west, 3 south, 4 north
        if p.get("face", "wall") != "wall":
            return "skip", 0
        wood = wood_of(path) is not None and "stone" not in path
        return ("minecraft:wooden_button" if wood else "minecraft:stone_button"), {"east": 1, "west": 2, "south": 3,
                                                                                   "north": 4}.get(face, 1)
    if path.endswith("pressure_plate"):
        wood = wood_of(path) is not None and "stone" not in path
        return ("minecraft:wooden_pressure_plate" if wood else "minecraft:stone_pressure_plate"), 0
    if "fluid_tank" in path or path.endswith("_tank"):
        return "deci:BlockMetal_1", 0
    if "lever" in path or "sign" in path or "banner" in path \
            or "item_frame" in path or "rail" == path or path.endswith("_rail"):
        return "skip", 0
    if path.endswith("_planks") or path == "planks" or re.search(r"planks_\d+$", path):
        w = wood_of(path)
        return "minecraft:planks", WOODS[w] if w else 0
    if path.endswith(("_log", "_wood", "_stem")) or "stripped" in path:
        w = wood_of(path) or "oak"
        m = WOODS[w]
        axis = p.get("axis", "y")
        block = "minecraft:log2" if m >= 4 else "minecraft:log"
        return block, (m & 3) | {"y": 0, "x": 4, "z": 8}.get(axis, 0)
    if path in ("water", "lava", "bedrock", "grass", "tall_grass", "fern", "vine", "cobweb", "glowstone",
                "bookshelf", "crafting_table", "furnace", "cauldron", "hay_block", "pumpkin", "melon",
                "snow", "ice", "packed_ice", "sponge", "tnt", "obsidian", "netherrack", "soul_sand"):
        return {"tall_grass": ("minecraft:tallgrass", 1), "grass": ("minecraft:tallgrass", 1),
                "fern": ("minecraft:tallgrass", 2), "vine": ("minecraft:vine", 0), "cobweb": ("minecraft:web", 0),
                "snow": ("minecraft:snow_layer", 0)}.get(path, ("minecraft:" + path, 0))
    if any(k in path for k in ("flower", "rose", "tulip", "poppy", "dandelion", "bush", "sapling", "dead_bush",
                               "mushroom", "roots", "sprouts", "weed", "crop", "wheat")):
        return "minecraft:tallgrass", 1
    lp = unlit(path)
    if "lamp" in lp or "light" in lp or "lantern" in lp or "illuminant" in lp:
        return "deci:BlockLightOff", 2
    m = material(path)
    if m:
        return m
    return None
