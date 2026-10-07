package net.decimation.worldgen.building;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Interior surfaces per building (docs/interior_spec.md section 2): one
 * wall panel colour set (Decimation BlockWallOffice_*: Bottom course with
 * a skirting stripe, Top above), floors per room type, the storey's own
 * ceiling with light panels and vents, and the door model of the rooms.
 */
final class Surfaces
{
    private final Building b;
    /** Decimation blocks; null = fall back to vanilla. */
    final Block wallBottom, wallTop;
    private final Block corridorFloor, meetingCarpet, lobbyFloor, shopFloor, stockFloor,
        kitchenFloor, bathFloor;
    /** Door used for this building's unit / room doors (null: none resolved). */
    private final Block roomDoor;

    Surfaces(Building b)
    {
        this.b = b;
        Building.Props props = b.props;
        roomDoor = b.kind == Building.OFFICE ? props.officeDoor
            : b.kind == Building.SHOP ? props.metalDoor
            : props.unitDoors.length > 0 ? props.unitDoors[(int) (b.unit(16, 16, 16) * props.unitDoors.length)]
            : props.officeDoor;
        String[][] sets = b.kind == Building.OFFICE
            ? new String[][] {{"BlockWallOffice_Bottom_3", "BlockWallOffice_Top"},
                              {"BlockWallOffice_4_Bottom_2", "BlockWallOffice_4_Top"},
                              {"BlockWallOffice_3_Bottom_2", "BlockWallOffice_3_Top"}}
            : b.kind == Building.SHOP
            ? new String[][] {{"BlockWallOffice_Bottom_3", "BlockWallOffice_Top"},
                              {"BlockWallOffice_2_Bottom_1", "BlockWallOffice_2_Top"}}
            : new String[][] {{"BlockWallOffice_2_Bottom_1", "BlockWallOffice_2_Top"},
                              {"BlockWallOffice_3_Bottom_2", "BlockWallOffice_3_Top"},
                              {"BlockWallOffice_4_Bottom_1", "BlockWallOffice_4_Top"},
                              {"BlockWallOffice_Bottom_1", "BlockWallOffice_Top"},
                              {"BlockWallOffice_Bottom_2", "BlockWallOffice_Top"}};
        String[] set = sets[(int) (b.unit(17, 17, 17) * sets.length)];
        wallBottom = b.deci(set[0]);
        wallTop = b.deci(set[1]);
        // light floors only: a floor is also the ceiling of the storey below
        // (black tiles made corridors read as a dark tunnel, audit 0.17)
        corridorFloor = b.deci(b.unit(18, 18, 18) < 0.5 ? "BlockStone_7" : "BlockStone_6");
        meetingCarpet = b.deci(b.unit(20, 20, 20) < 0.5 ? "BlockFloorCarpet_5" : "BlockFloorCarpet_6");
        lobbyFloor = b.deci("BlockStone_6");
        shopFloor = b.deci(b.unit(21, 21, 21) < 0.6 ? "BlockStone_6" : "BlockFloorTiles_2");
        stockFloor = b.deci("BlockStone_1");
        kitchenFloor = b.deci(b.unit(23, 23, 23) < 0.5 ? "BlockFloorTiles_1" : "BlockFloorTiles_2");
        bathFloor = b.deci(b.unit(24, 24, 24) < 0.5 ? "BlockFloorTiles_2" : "BlockFloorTiles_1");
    }

    /** Floor layer block of an inner cell, by room. */
    Block floor(int[] meta, int storey, int fx, int z)
    {
        byte room = b.plan(storey).rooms[fx][z];
        Block f = null;
        switch (room)
        {
            case StoreyPlan.R_CORRIDOR: f = corridorFloor; break;
            case StoreyPlan.R_BEDROOM: meta[0] = 1; return Blocks.planks; // warm spruce (grey carpet read as concrete)
            case StoreyPlan.R_LOBBY: f = lobbyFloor; break;
            case StoreyPlan.R_OFFICE: f = b.deci("BlockFloorCarpet_" + (1 + (int) (b.unit(storey, 22, 22) * 4))); break;
            case StoreyPlan.R_MEETING: f = meetingCarpet; break;
            case StoreyPlan.R_STORAGE:
            case StoreyPlan.R_STOCK: f = stockFloor; break;
            case StoreyPlan.R_SHOP: f = shopFloor; break;
            // checker tiles would show as a checkerboard ceiling in the flat
            // below (audit 0.18): tiles on the ground storey, light stone above
            case StoreyPlan.R_KITCHEN: f = storey == 0 ? kitchenFloor : lobbyFloor; break;
            case StoreyPlan.R_BATH: f = bathFloor; break;
            default: break; // living rooms keep planks
        }
        if (f != null)
        {
            meta[0] = 0;
            return f;
        }
        if (b.kind == Building.OFFICE || (b.kind == Building.SHOP && storey == 0))
        {
            meta[0] = 0;
            return Blocks.double_stone_slab;
        }
        meta[0] = (int) (b.unit(1, storey, 1) * 3); // oak / spruce / birch
        return Blocks.planks;
    }

    /** Interior wall course: the bottom panel at within 1, the top panel above. */
    Block wallPanel(int within, int[] meta)
    {
        Block panel = within == 1 ? wallBottom : wallTop;
        if (panel != null)
        {
            meta[0] = 0;
            return panel;
        }
        meta[0] = b.kind == Building.OFFICE ? 0 : 2;
        return b.kind == Building.OFFICE ? Blocks.stonebrick : Blocks.planks;
    }

    /** Plaster lining inside an outer wall. */
    Block lining(int within, int[] meta)
    {
        meta[0] = 0;
        return within == 1 ? (wallBottom != null ? wallBottom : Blocks.planks)
            : (wallTop != null ? wallTop : Blocks.planks);
    }

    /**
     * The storey's own ceiling (within ceil): walls continue as their top
     * panel, rooms get plaster, storage bare concrete. Decayed buildings
     * lose some ceiling cells, showing the slab above.
     */
    Block ceilingLayer(byte cell, byte room, int fx, int storey, int z, int[] meta)
    {
        meta[0] = 0;
        if (cell == StoreyPlan.WALL || cell == StoreyPlan.DOOR || cell == StoreyPlan.GLASS
            || cell == StoreyPlan.LINING)
        {
            return wallTop != null ? wallTop : Blocks.planks;
        }
        if (b.unit(fx, storey * 17 + 3, z) < b.ruins.decay * 0.12)
        {
            return null; // fallen ceiling panel
        }
        // white plaster everywhere: the underside of a block is shaded dark,
        // so ceiling tiles (BlockCeiling_3/4) read as a dark lid (audit 0.19)
        Block c = room == StoreyPlan.R_STORAGE || room == StoreyPlan.R_STOCK
            ? b.deci("BlockStone_1") : b.deci("BlockWallOffice_Top");
        return c != null ? c : Blocks.quartz_block;
    }

    /** Above doors and glass partitions: the wall's own top panel. */
    Block lintel(int[] meta)
    {
        meta[0] = 0;
        return wallTop != null ? wallTop : Blocks.stonebrick;
    }

    /**
     * Ceiling fixtures under the ceiling of an open cell: light panels
     * (Decimation BlockLight has its box at the top of the cell, so it
     * hangs from the ceiling; BlockLight glows, BlockLightOff is dead),
     * vents in offices and shops. Most lights are dead, some missing.
     */
    Block fixture(byte room, int fx, int z, int storey, int[] meta)
    {
        boolean grid;
        switch (room)
        {
            case StoreyPlan.R_CORRIDOR:
                grid = fx % 4 == 1;
                break;
            case StoreyPlan.R_OFFICE:
            case StoreyPlan.R_SHOP:
            case StoreyPlan.R_MEETING:
                if (fx % 6 == 5 && z % 6 == 5 && b.deci("BlockCeilingVent") != null)
                {
                    meta[0] = 2;
                    return b.deci("BlockCeilingVent");
                }
                grid = fx % 4 == 2 && z % 4 == 2;
                break;
            default:
                grid = fx % 5 == 2 && z % 5 == 2;
        }
        if (!grid || b.unit(fx, storey * 13 + 7, z) < 0.15 + b.ruins.decay * 0.3)
        {
            return null;
        }
        meta[0] = 2;
        return b.unit(fx, storey * 13 + 8, z) < 0.08 ? b.deci("BlockLight") : b.deci("BlockLightOff");
    }

    /**
     * A door in a DOOR cell of the plan (both halves: within 1 lower, 2
     * upper). Decimation doors copy vanilla door metadata: lower 0 / 2 for
     * a door in a wall running north-south (plan z axis), 1 / 3 for a wall
     * running along x, +4 = open; upper 8. Missing with a chance growing
     * with decay; about a quarter of the rest stand open.
     */
    Block door(StoreyPlan sp, int fx, int z, int storey, int within, int[] meta)
    {
        if (roomDoor == null || b.unit(fx, storey * 7 + 3, z) < 0.2 + b.ruins.decay * 0.5)
        {
            return null;
        }
        if (within == 2)
        {
            meta[0] = 8;
            return roomDoor;
        }
        boolean wallAlongZ = (z > 0 && sp.cells[fx][z - 1] == StoreyPlan.WALL)
            || (z < b.length - 1 && sp.cells[fx][z + 1] == StoreyPlan.WALL);
        int base = wallAlongZ ? 0 : 1;
        meta[0] = base | (b.unit(fx, storey * 7 + 5, z) < 0.25 ? 4 : 0);
        return roomDoor;
    }
}
