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


# full block material by words in the name; first match wins
MATERIAL = [
    (("quartz", "calcite", "marble", "white_concrete", "snow"), ("minecraft:quartz_block", 0)),
    (("nether_brick",), ("minecraft:nether_brick", 0)),
    (("brick",), ("minecraft:brick_block", 0)),
    (("stone_brick", "stonebrick", "tile"), ("minecraft:stonebrick", 0)),
    (("mossy",), ("minecraft:mossy_cobblestone", 0)),
    (("cobble",), ("minecraft:cobblestone", 0)),
    (("sandstone",), ("minecraft:sandstone", 2)),
    (("deepslate", "blackstone", "basalt", "tuff", "obsidian", "gneiss", "slate", "black_sandstone"),
     ("deci:BlockStone_4", 0)),
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
    if any(k in path for k in ("illuminant", "ceiling_light", "ceiling_fan", "lightbulb", "edge_light",
                               "fluorescent")):
        return "deci:BlockLightOff", 2
    if any(k in path for k in ("lamp", "lantern")):
        return "deci:BlockLantern", 2
    if "potted" in path or "flower_pot" in path or "plant_pot" in path:
        return "minecraft:flower_pot", 0
    if "curtain" in path or "blind" in path:
        c = colour_of(path)
        return "minecraft:carpet" if False else "minecraft:wool", c if c is not None else 8
    if "towel" in path or "toalla" in path or "papel" in path or "regadera" in path or "llaves" in path:
        return "skip", 0
    if any(k in path for k in ("plate", "pan", "toaster", "microwave", "range_hood", "jar", "tray", "bread",
                               "lightswitch", "switch", "doorbell", "painting", "picture", "clock", "mirror",
                               "book", "cup", "mug", "bottle", "phone", "keyboard")) and "pressure" not in path \
            and "metal_plate" not in path:
        return "skip", 0
    return None


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
    if "air_duct" in path or "vent" in path:
        return "minecraft:iron_block" if "vent" not in path else "deci:BlockCeilingVent", 0 if "vent" not in path else 2
    if path in ("spawner", "gold_block") or "plushie" in ns or "decal" in path or "barbed" in path \
            or "documento" in path or "libro" in path or "display_board" in path or "fluid" in path:
        return "skip", 0
    if "lavabo" in path or "water_cauldron" in path:
        return "minecraft:cauldron", 0
    # ---- shapes
    if path.endswith("_stairs"):
        return stair_block(path), STAIR_FACING.get(face, 3) | (4 if half == "top" else 0)
    if path.endswith("vertical_slab"):
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
    if "button" in path or "lever" in path or "pressure_plate" in path or "sign" in path or "banner" in path \
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
    if "lamp" in path or "light" in path or "lantern" in path or "illuminant" in path:
        return "deci:BlockLightOff", 2
    m = material(path)
    if m:
        return m
    return None
