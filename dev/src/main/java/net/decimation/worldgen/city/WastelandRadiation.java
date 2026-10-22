package net.decimation.worldgen.city;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.worldgen.DecimationWorldGen;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.ZoneStore;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

/**
 * The wasteland city one radiation zone (user, 22 Oktober 2026, idea 43: "wasteland city become full radiation zone on all
 * place"; docs/wasteland.md). Decimation keeps every zone in one flat list and asks one kind at a time (ZoneManager: in
 * any box of that kind), so a RADIATION box on the ground of the wasteland's MILITARY boxes adds Decimation's radiation
 * rules there and changes none of the military ones (its spawn handler asks MILITARY, POLICE, SAFEZONE in turn).
 * - Worlds with whole sector wasteland cities (since v0.57.1): one box over the sector, 256 x 256, so the GPS map shows
 *   its radiation mark in the middle of the city and the four military marks of its districts beside it.
 * - Older worlds: one box over each wasteland district, 128 x 128 (the military box's ground).
 * Every height, like the military boxes. Added with the district's military zone the first time a chunk near it is
 * populated (Wasteland.zone; StructureData marker "!wr_..."), and in a world from before this feature at its load:
 * every wasteland military zone of ours (Wasteland.zone's box and marker) without a radiation zone over its middle gets
 * one (cheap: one pass over our zones at load; only adds).
 */
public final class WastelandRadiation
{
    /** A district's side in blocks (2 x 2 cells) and a sector's (a region, 4 x 4 cells). */
    static final int DISTRICT = 2 * LcCity.CELL * 16, SECTOR = net.decimation.worldgen.Sectors.REGION * 16;
    private static final int CELL_BLOCKS = LcCity.CELL * 16, SECTOR_CELLS = net.decimation.worldgen.Sectors.REGION / LcCity.CELL;

    /** The radiation box {x1, y1, z1, x2, y2, z2} over the wasteland city of district (gx, gz) (its first cell, even). */
    static int[] box(int gx, int gz, boolean sectors)
    {
        int side = sectors ? SECTOR : DISTRICT;
        int x0 = sectors ? Math.floorDiv(gx, SECTOR_CELLS) * SECTOR : gx * CELL_BLOCKS;
        int z0 = sectors ? Math.floorDiv(gz, SECTOR_CELLS) * SECTOR : gz * CELL_BLOCKS;
        return new int[] {x0, 0, z0, x0 + side - 1, 255, z0 + side - 1};
    }

    /** The StructureData marker of that box. */
    static String marker(int gx, int gz, boolean sectors)
    {
        return sectors ? "!wr_s" + Math.floorDiv(gx, SECTOR_CELLS) + "_" + Math.floorDiv(gz, SECTOR_CELLS) : "!wr_" + gx + "_" + gz;
    }

    /** The district (gx, gz) of a box shaped like a wasteland military zone (Wasteland.zone), else null. */
    static int[] district(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        if (y1 != 0 || y2 != 255 || x2 - x1 != DISTRICT - 1 || z2 - z1 != DISTRICT - 1
            || Math.floorMod(x1, DISTRICT) != 0 || Math.floorMod(z1, DISTRICT) != 0)
        {
            return null;
        }
        return new int[] {Math.floorDiv(x1, CELL_BLOCKS), Math.floorDiv(z1, CELL_BLOCKS)};
    }

    /** Adds the radiation box over the wasteland city of district (gx, gz) when this world has none yet (Wasteland.zone). */
    static void add(StructureData data, int gx, int gz)
    {
        boolean sectors = Wasteland.sectors();
        String id = marker(gx, gz, sectors);
        if (data.baseY(id) != null)
        {
            return;
        }
        int[] b = box(gx, gz, sectors);
        if (ZoneStore.add(ZoneKind.RADIATION, b[0], b[1], b[2], b[3], b[4], b[5]) != null)
        {
            data.setBaseY(id, 1);
            FMLLog.info("[%s] wasteland radiation zone %d,%d to %d,%d", DecimationWorldGen.MODID, b[0], b[2], b[3], b[5]);
        }
    }

    /**
     * Every wasteland military zone of this world without a radiation zone of ours over its middle gets its radiation box
     * (a world from before 22 Oktober 2026, or one whose box went); how many were added.
     */
    public static int upgrade(World world)
    {
        StructureData data = StructureData.get(world);
        boolean sectors = Wasteland.sectors();
        int added = 0;
        for (int[] z : ZoneStore.boxes(ZoneKind.MILITARY))
        {
            int[] g = district(z[0], z[1], z[2], z[3], z[4], z[5]);
            if (g == null || data.baseY("!wz_" + g[0] + "_" + g[1]) == null
                || ZoneStore.has(ZoneKind.RADIATION, z[0] + DISTRICT / 2, 64, z[2] + DISTRICT / 2))
            {
                continue;
            }
            int[] b = box(g[0], g[1], sectors);
            if (ZoneStore.add(ZoneKind.RADIATION, b[0], b[1], b[2], b[3], b[4], b[5]) != null)
            {
                data.setBaseY(marker(g[0], g[1], sectors), 1);
                added++;
                FMLLog.info("[%s] wasteland radiation zone %d,%d to %d,%d (a world from before)", DecimationWorldGen.MODID,
                            b[0], b[2], b[3], b[5]);
            }
        }
        return added;
    }

    /** World load, after ZoneStore read the world's zones (lowest priority): the radiation boxes an older world lacks. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.world;
        if (world.isRemote || world.provider == null || world.provider.dimensionId != 0)
        {
            return;
        }
        upgrade(world);
    }
}
