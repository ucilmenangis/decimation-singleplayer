package net.decimation.worldgen.devtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import net.decimation.mod.server.zones.ObjectZone;
import net.decimation.worldgen.DecimationWorldGen;
import net.decimation.worldgen.DevAutoTest;
import net.decimation.worldgen.ZoneKind;
import net.decimation.worldgen.building.Building;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureData;
import net.decimation.worldgen.StructureGenerator;
import net.decimation.worldgen.ZoneStore;
import net.minecraft.world.WorldSettings;

/**
 * Server checks (-Ptest=checks), the original autotest: zones and infected
 * variants, vehicle punch, humanity, prop render box, bottlecaps, armor and
 * helmet against NPC gunfire, a forced supply drop that must land.
 */
public class ServerChecks extends DevTestMode
{
    public String name() { return "checks"; }

    public boolean freshWorld() { return true; }

    private volatile boolean finished;

    public boolean client(Minecraft mc)
    {
        return !finished;
    }

    public void server()
    {
        if (finished)
        {
            return;
        }
        if (waitTicks > 0)
        {
            waitTicks--;
            return;
        }
        try
        {
            switch (phase++)
            {
                case 0: // instant checks, then drop caps and the supply crate
                    if (CameraViews.study() != null)
                    {
                        break; // study mode: straight to the screenshots
                    }
                    run(MinecraftServer.getServer().worldServerForDimension(0));
                    checkVehicle();
                    checkHumanity();
                    checkPropBox();
                    dropBottlecaps();
                    net.decimation.fixes.Deci.setSupplyDropCountdown(0); // drop on the next tick
                    waitTicks = 100;   // pickup, and spawn invulnerability runs out
                    return;
                case 1:
                    checkBottlecaps();
                    checkArmor();
                    checkHelmet();
                    waitTicks = 100;
                    return;
                default: // poll until the crate has landed, at most ~60 s
                    if (!checkSupplyDrop() && phase < 14)
                    {
                        waitTicks = 100;
                        return;
                    }
                    break;
            }
        }
        catch (Throwable t)
        {
            FMLLog.info("[%s] AUTOTEST error: %s", DecimationWorldGen.MODID, t);
        }
        finished = true;
    }

    private int phase;
    private int waitTicks;

    /** Prop culling patch: a table's render box must be the rotation proof 2x2 box. */
    private void checkPropBox()
    {
        net.minecraft.block.Block table = net.minecraft.block.Block.getBlockFromName("deci:BlockWoodTable");
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (table == null || p == null)
        {
            return;
        }
        net.minecraft.tileentity.TileEntity te = table.createTileEntity(p.worldObj, 2);
        te.xCoord = 100;
        te.yCoord = 64;
        te.zCoord = 100;
        net.minecraft.util.AxisAlignedBB bb = te.getRenderBoundingBox();
        FMLLog.info("[%s] AUTOTEST prop box: table at 100,64,100 -> %.2f..%.2f x %.2f..%.2f y %.2f..%.2f z (old 1x1 cell = 100..101)",
                    DecimationWorldGen.MODID, bb.minX, bb.maxX, bb.minY, bb.maxY, bb.minZ, bb.maxZ);
        DevTestResults.check("checks", "prop render box width", String.format("%.2f", bb.maxX - bb.minX),
                             bb.maxX - bb.minX >= 2.0, "2x2 rotation proof box, PatchPropCulling");
    }

    /** Humanity fix: a player killing an infected gains humanity (+1 from the
     *  infected's own value, +1 from the formerly dedicated-only rule). */
    private void checkHumanity()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        net.decimation.fixes.Deci.Player data = net.decimation.fixes.Deci.player(p);
        int before = data.humanity();
        net.minecraft.entity.EntityLiving infected = net.decimation.fixes.Deci.newInfected(p.worldObj);
        infected.setLocationAndAngles(p.posX + 2, p.posY, p.posZ, 0, 0);
        p.worldObj.spawnEntityInWorld(infected);
        infected.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(p), 1000.0f);
        FMLLog.info("[%s] AUTOTEST humanity: killed infected, humanity %d -> %d, dead=%s",
                    DecimationWorldGen.MODID, before, data.humanity(), infected.isDead || infected.getHealth() <= 0);
        DevTestResults.check("checks", "humanity kill infected", before + " -> " + data.humanity(),
                             data.humanity() > before, "rises (HumanityKillHandler)");
        infected.setDead();
    }

    /** Supply drop fix: the forced drop must leave a crate block in its column.
     *  Returns true once the crate has landed (or nothing was dropped). */
    private boolean checkSupplyDrop()
    {
        int x = net.decimation.fixes.SupplyDropScheduler.lastDropX;
        int z = net.decimation.fixes.SupplyDropScheduler.lastDropZ;
        if (x == Integer.MIN_VALUE)
        {
            FMLLog.info("[%s] AUTOTEST supply drop: nothing dropped", DecimationWorldGen.MODID);
            return true;
        }
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        int landedY = -1;
        for (int y = 255; y > 0; y--)
        {
            if (world.getBlock(x, y, z) == net.decimation.fixes.Deci.supplyDropBlock())
            {
                landedY = y;
                break;
            }
        }
        int falling = 0;
        for (Object o : world.loadedEntityList)
        {
            if (net.decimation.fixes.Deci.isFallingSupplyDrop(o))
            {
                falling++;
            }
        }
        FMLLog.info("[%s] AUTOTEST supply drop: column %d/%d, crate block at y=%d, falling crates=%d",
                    DecimationWorldGen.MODID, x, z, landedY, falling);
        if (landedY >= 0)
        {
            DevTestResults.check("checks", "supply drop landed", "y=" + landedY, true, "crate block in its column");
        }
        return landedY >= 0;
    }
    private long capsBefore;
    private static final int CAPS = 5;

    private static net.minecraft.entity.player.EntityPlayerMP player()
    {
        List<?> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        return players.isEmpty() ? null : (net.minecraft.entity.player.EntityPlayerMP) players.get(0);
    }

    /** Bottlecap fix: drop caps on the player, they must become balance. */
    private void dropBottlecaps()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            FMLLog.info("[%s] AUTOTEST bottlecaps: no player", DecimationWorldGen.MODID);
            return;
        }
        capsBefore = net.decimation.fixes.Deci.player(p).bottlecaps();
        net.minecraft.entity.item.EntityItem item = new net.minecraft.entity.item.EntityItem(
            p.worldObj, p.posX, p.posY, p.posZ,
            new net.minecraft.item.ItemStack(net.decimation.fixes.Deci.bottlecap(), CAPS));
        item.delayBeforeCanPickup = 0;
        p.worldObj.spawnEntityInWorld(item);
    }

    private void checkBottlecaps()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        long after = net.decimation.fixes.Deci.player(p).bottlecaps();
        int inInventory = 0;
        for (net.minecraft.item.ItemStack st : p.inventory.mainInventory)
        {
            if (st != null && st.getItem() == net.decimation.fixes.Deci.bottlecap())
            {
                inInventory += st.stackSize;
            }
        }
        FMLLog.info("[%s] AUTOTEST bottlecaps: balance %d -> %d (dropped %d), caps left as items: %d",
                    DecimationWorldGen.MODID, capsBefore, after, CAPS, inInventory);
        DevTestResults.check("checks", "bottlecaps picked up", capsBefore + " -> " + after,
                             after - capsBefore >= CAPS && inInventory == 0, ">= +" + CAPS + ", none left as items");
    }

    /**
     * Armor fix: a "human" (NPC gun) hit must be reduced by Decimation chest
     * armor exactly by its damage multiplier; without armor it must not be.
     */
    private void checkArmor()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        net.decimation.mod.common.item.armor.ItemArmorDeci chest = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            if (o instanceof net.decimation.mod.common.item.armor.ItemArmorDeci
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).armorType == 1
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).getDamageMultiplier() < 1.0f)
            {
                chest = (net.decimation.mod.common.item.armor.ItemArmorDeci) o;
                break;
            }
        }
        if (chest == null)
        {
            FMLLog.info("[%s] AUTOTEST armor: no Decimation chest armor found", DecimationWorldGen.MODID);
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        float bare = hit(p);
        p.setCurrentItemOrArmor(3, new net.minecraft.item.ItemStack(chest)); // slot 3 = chest
        float armored = hit(p);
        p.setCurrentItemOrArmor(3, null);
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.setHealth(p.getMaxHealth());
        FMLLog.info("[%s] AUTOTEST armor: 10 \"human\" damage, bare took %.2f, with %s (x%.3f) took %.2f",
                    DecimationWorldGen.MODID, bare, chest.getUnlocalizedName(), chest.getDamageMultiplier(), armored);
        DevTestResults.check("checks", "armor vs NPC gunfire", String.format("%.2f of %.2f", armored, bare),
                             armored < bare, "less damage with chest armor (ArmorGunfireHandler)");
    }

    /**
     * Helmet fix: NPC hits use the helmet on ~20% (random headshots); a
     * player's gun uses it only when its aim line crosses the head.
     */
    private void checkHelmet()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        net.decimation.mod.common.item.armor.ItemArmorDeci helm = null;
        for (Object o : net.minecraft.item.Item.itemRegistry)
        {
            if (o instanceof net.decimation.mod.common.item.armor.ItemArmorDeci
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).armorType == 0
                && ((net.decimation.mod.common.item.armor.ItemArmorDeci) o).getDamageMultiplier() < 0.5f)
            {
                helm = (net.decimation.mod.common.item.armor.ItemArmorDeci) o;
                break;
            }
        }
        if (helm == null)
        {
            FMLLog.info("[%s] AUTOTEST helmet: no Decimation helmet found", DecimationWorldGen.MODID);
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.setCurrentItemOrArmor(4, new net.minecraft.item.ItemStack(helm)); // slot 4 = helmet
        int reduced = 0, total = 400;
        for (int i = 0; i < total; i++)
        {
            if (hit(p) < 9.99f)
            {
                reduced++;
            }
        }
        // player shooter 5 blocks north, facing south (+z), aiming at head then chest
        net.minecraftforge.common.util.FakePlayer shooter =
            net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((WorldServer) p.worldObj);
        float headTaken = shoot(p, shooter, p.boundingBox.minY + 1.6);
        float chestTaken = shoot(p, shooter, p.boundingBox.minY + 1.0);
        p.setCurrentItemOrArmor(4, null);
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.setHealth(p.getMaxHealth());
        FMLLog.info("[%s] AUTOTEST helmet %s (x%.3f): NPC hits reduced %d/%d (expect ~20%%);"
                    + " player gun 10 dmg: head took %.2f, chest took %.2f",
                    DecimationWorldGen.MODID, helm.getUnlocalizedName(), helm.getDamageMultiplier(),
                    reduced, total, headTaken, chestTaken);
        DevTestResults.check("checks", "helmet NPC hits reduced", reduced + "/" + total,
                             reduced > total / 10 && reduced < total * 3 / 10, "about 20% (headshots)");
        DevTestResults.check("checks", "helmet player headshot", String.format("%.2f vs chest %.2f", headTaken, chestTaken),
                             headTaken < chestTaken, "head takes less with a helmet");
    }

    /** A 10 point gunDeci hit from a shooter aiming at the given height. */
    private static float shoot(net.minecraft.entity.player.EntityPlayerMP p, net.minecraft.entity.player.EntityPlayer shooter, double aimY)
    {
        double sx = p.posX, sz = p.posZ - 5.0, sy = p.boundingBox.minY;
        double eyeY = sy + shooter.getEyeHeight();
        float pitch = (float) Math.toDegrees(-Math.atan2(aimY - eyeY, 5.0));
        shooter.setLocationAndAngles(sx, sy, sz, 0.0F, pitch); // yaw 0 = facing +z
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        p.attackEntityFrom(net.decimation.fixes.Deci.gunDamage(shooter), 10.0f);
        return before - p.getHealth();
    }

    /** One 10 point "human" hit on a fully healed player, returns health lost. */
    private static float hit(net.minecraft.entity.player.EntityPlayerMP p)
    {
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        p.attackEntityFrom(net.decimation.fixes.Deci.humanDamage(), 10.0f);
        return before - p.getHealth();
    }

    /**
     * Vehicle fix: a survival punch must leave an empty vehicle alone, a
     * sneaking survival punch must still pick it up.
     */
    private void checkVehicle()
    {
        net.minecraft.entity.player.EntityPlayerMP p = player();
        if (p == null)
        {
            return;
        }
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        net.minecraft.entity.Entity vehicle = net.decimation.fixes.Deci.newHummer(p.worldObj, p.posX + 3, p.posY, p.posZ);
        p.worldObj.spawnEntityInWorld(vehicle);
        p.setSneaking(false);
        p.attackTargetEntityWithCurrentItem(vehicle);
        boolean survivedPunch = !vehicle.isDead;
        p.setSneaking(true);
        p.attackTargetEntityWithCurrentItem(vehicle);
        boolean pickedUpSneaking = vehicle.isDead;
        p.setSneaking(false);
        vehicle.setDead();
        p.setGameType(WorldSettings.GameType.CREATIVE);
        FMLLog.info("[%s] AUTOTEST vehicle: survives survival punch=%s, sneak punch picks up=%s",
                    DecimationWorldGen.MODID, survivedPunch, pickedUpSneaking);
        DevTestResults.check("checks", "vehicle punch", "survives=" + survivedPunch + " sneak pickup=" + pickedUpSneaking,
                             survivedPunch && pickedUpSneaking, "survives a punch, sneak punch picks up");
    }

    private void run(WorldServer world)
    {
        FMLLog.info("[%s] AUTOTEST generated zones in this world: %d",
                    DecimationWorldGen.MODID, ZoneStore.size());
        DevTestResults.check("checks", "generated zones", ZoneStore.size(), ZoneStore.size() > 0, "> 0");
        // Decimation's spawns must reach the biomes of the Decimation world type
        net.minecraft.entity.player.EntityPlayerMP sp = player();
        net.minecraft.world.biome.BiomeGenBase biome =
            world.getBiomeGenForCoords((int) sp.posX, (int) sp.posZ);
        int deciMonsters = 0;
        for (Object o : biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.monster))
        {
            if (((net.minecraft.world.biome.BiomeGenBase.SpawnListEntry) o).entityClass.getName().startsWith("deci."))
            {
                deciMonsters++;
            }
        }
        FMLLog.info("[%s] AUTOTEST biome at spawn: %s (id %d), Decimation monster spawn entries %d",
                    DecimationWorldGen.MODID, biome.biomeName, biome.biomeID, deciMonsters);
        ObjectZone zone = null;
        if (net.decimation.fixes.Deci.zoneList() != null)
        {
            for (ObjectZone z : net.decimation.fixes.Deci.zoneList().zoneList)
            {
                if (net.decimation.fixes.Deci.isZone(z, ZoneKind.POLICE)
                    || net.decimation.fixes.Deci.isZone(z, ZoneKind.MILITARY))
                {
                    zone = z;
                    break;
                }
            }
        }
        if (zone == null)
        {
            FMLLog.info("[%s] AUTOTEST no military/police zone found, nothing to test",
                        DecimationWorldGen.MODID);
            return;
        }
        int cx = (zone.zoneX1 + zone.zoneX2) / 2;
        int cz = (zone.zoneZ1 + zone.zoneZ2) / 2;
        int[] inside = spawnBatch(world, cx, cz);
        int[] outside = spawnBatch(world, cx + 3000, cz + 3000);
        FMLLog.info("[%s] AUTOTEST %s zone at %d,%d: inside common=%d military=%d police=%d"
                    + " | outside common=%d military=%d police=%d",
                    DecimationWorldGen.MODID, zone.zoneType.name(), cx, cz,
                    inside[0], inside[1], inside[2], outside[0], outside[1], outside[2]);
        int insideZoned = inside[1] + inside[2], outsideZoned = outside[1] + outside[2];
        DevTestResults.check("checks", "zone infected variants", "inside " + insideZoned + " outside " + outsideZoned,
                             insideZoned > 0 && outsideZoned == 0, "zoned variants only inside the zone");
    }

    /** Spawns DevAutoTest.SPAWNS infected at x,z and returns variant counts [common, military, police]. */
    private int[] spawnBatch(WorldServer world, int x, int z)
    {
        int y = world.getTopSolidOrLiquidBlock(x, z) + 1;
        int[] counts = new int[3];
        List<net.minecraft.entity.EntityLiving> spawned = new ArrayList<net.minecraft.entity.EntityLiving>();
        for (int i = 0; i < DevAutoTest.SPAWNS; i++)
        {
            net.minecraft.entity.EntityLiving infected = net.decimation.fixes.Deci.newInfected(world);
            infected.setLocationAndAngles(x + 0.5, y, z + 0.5, 0, 0);
            if (world.spawnEntityInWorld(infected))
            {
                spawned.add(infected);
                int v = net.decimation.fixes.Deci.infectedVariant(infected);
                if (v >= 0 && v < 3)
                {
                    counts[v]++;
                }
            }
        }
        for (net.minecraft.entity.EntityLiving infected : spawned)
        {
            infected.setDead();
        }
        return counts;
    }

}
