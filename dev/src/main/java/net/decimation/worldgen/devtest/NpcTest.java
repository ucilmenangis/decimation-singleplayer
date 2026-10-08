package net.decimation.worldgen.devtest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.decimation.fixes.Deci;
import net.decimation.fixes.MilitarySpawner;
import net.decimation.fixes.NpcKind;
import net.decimation.fixes.NpcLoadouts;
import net.decimation.worldgen.Sectors;
import net.decimation.worldgen.StructureGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/**
 * NPC tiers (-Ptest=npc, net.decimation.fixes.NpcLoadouts): spawns batches
 * of bandits (outside and inside a military sector), soldiers and Soviets
 * and checks every one got a tier, 4 armor pieces, a gun matching the tier
 * data and the tier's health; a player's gun hit takes the tier's share; the
 * client shows the server's gun (data watcher sync); the military spawner
 * places a group. Then a lineup per faction is photographed (npc_*.png).
 */
public class NpcTest extends DevTestMode
{
    public String name() { return "npc"; }

    private static final String[][] LINEUPS = {
        {"bandit_light", "bandit_light", "bandit_medium", "bandit_medium", "bandit_heavy", "bandit_heavy"},
        {"soldier_marine", "soldier_marineforest", "soldier_marineurban", "soldier_marineblack"},
        {"military", "military", "military", "military"}};

    private volatile boolean done;
    private volatile int ticks;
    /** Lineup shown now (index into LINEUPS), -1 none; its entities and their guns. */
    private volatile int lineup = -1;
    private final List<EntityLiving> shown = new ArrayList<EntityLiving>();
    private volatile Map<Integer, String> shownGuns = new HashMap<Integer, String>();
    private volatile double camX, camY, camZ;
    private int clientMatch, clientTotal, checked = -1, shot = -1;

    public boolean client(Minecraft mc)
    {
        int t = ticks;
        int l = lineup;
        if (l >= 0 && mc.thePlayer != null)
        {
            mc.gameSettings.hideGUI = true;
            mc.thePlayer.setPositionAndRotation(camX, camY, camZ, 180, 10);
            mc.thePlayer.rotationYawHead = 180;
            int phase = (t - 300) % 200;
            if (phase >= 120 && checked < l)
            {
                checked = l;
                for (Map.Entry<Integer, String> e : shownGuns.entrySet())
                {
                    Entity c = mc.theWorld.getEntityByID(e.getKey());
                    ItemStack g = c == null ? null : Deci.npcGun(c);
                    clientTotal++;
                    if (g != null && g.getItem() == NpcLoadoutsAccess.item(e.getValue()))
                    {
                        clientMatch++;
                    }
                    else
                    {
                        DevTestResults.value(name(), "client gun mismatch lineup " + l, e.getValue() + " vs "
                            + (c == null ? "no entity" : g == null ? "no gun" : g.getItem().getUnlocalizedName()
                            + " watcher '" + c.getDataWatcher().getWatchableObjectString(NpcLoadouts.GUN_SLOT) + "'"));
                    }
                }
            }
            if (phase >= 150 && shot < l)
            {
                shot = l;
                DevTestUtil.screenshot(mc, name(), "npc_" + l);
            }
        }
        if (t > 300 + LINEUPS.length * 200 + 20)
        {
            mc.gameSettings.hideGUI = false;
            DevTestResults.check(name(), "client shows the server's gun", clientMatch + " / " + clientTotal,
                                 clientTotal > 0 && clientMatch == clientTotal, "all lineup NPCs");
            done = true;
        }
        return !done;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World world = p.worldObj;
        if (t == 20)
        {
            world.difficultySetting = EnumDifficulty.NORMAL; // humans are mobs: peaceful removes them
            world.setWorldTime(6000);
            p.capabilities.isFlying = true;
            batches(world, p);
        }
        if (t >= 300 && t < 300 + LINEUPS.length * 200)
        {
            int l = (t - 300) / 200;
            if ((t - 300) % 200 == 0)
            {
                showLineup(world, p, l);
            }
            hold();
            if (t % 20 == 0)
            {
                clearOthers(world, p);
            }
        }
        if (t == 300 + LINEUPS.length * 200)
        {
            clear();
            lineup = -1;
        }
    }

    // ---- batches: tiers, gear, health, damage taken, spawner

    private void batches(World world, EntityPlayerMP p)
    {
        NpcLoadouts loadouts = NpcLoadouts.instance();
        int bx = (int) p.posX, bz = (int) p.posZ - 24;
        Map<String, Integer> plain = spawnBatch(world, NpcKind.BANDIT, 60, bx, bz);
        DevTestResults.value(name(), "bandit tiers outside military", plain);
        int[] mil = militaryChunk(world, p);
        if (mil != null)
        {
            world.getChunkProvider().loadChunk(mil[0], mil[1]);
            Map<String, Integer> m = spawnBatch(world, NpcKind.BANDIT, 60, (mil[0] << 4) + 8, (mil[1] << 4) + 8);
            DevTestResults.value(name(), "bandit tiers in military sector", m);
            int heavyOut = count(plain, "bandit_heavy") + count(plain, "bandit_medium");
            int heavyIn = count(m, "bandit_heavy") + count(m, "bandit_medium");
            DevTestResults.check(name(), "military sector shifts bandits heavier", heavyOut + " -> " + heavyIn,
                                 heavyIn > heavyOut, "more medium + heavy inside");
        }
        DevTestResults.value(name(), "soldier tiers", spawnBatch(world, NpcKind.SOLDIER, 12, bx, bz));
        DevTestResults.value(name(), "soviet tiers", spawnBatch(world, NpcKind.SOVIET, 12, bx, bz));

        // a player's gun hit: the tier's share
        for (String tn : new String[] {"bandit_light", "bandit_heavy", "military"})
        {
            NpcLoadouts.Tier tier = loadouts.byName(tn);
            EntityLiving npc = tier.kind == NpcKind.SOVIET ? Deci.newSoviet(world) : Deci.newBandit(world);
            loadouts.equip(npc, tier, npc.getEntityData());
            npc.setPosition(bx + 0.5, ground(world, bx, bz), bz + 0.5);
            world.spawnEntityInWorld(npc);
            float before = npc.getHealth();
            npc.attackEntityFrom(Deci.gunDamage(p), 10);
            float lost = before - npc.getHealth();
            DevTestResults.check(name(), "gun hit 10 on " + tn, String.format("%.1f", lost),
                                 Math.abs(lost - 10 * tier.taken) < 0.01, String.format("%.1f", 10 * tier.taken));
            npc.setDead();
        }

        // the spawner places a tiered group near the player
        MilitarySpawner spawner = new MilitarySpawner(new File(Minecraft.getMinecraft().mcDataDir, "config"));
        List<EntityLiving> before = soviets(world, p, 64);
        boolean placed = spawner.spawnGroup((net.minecraft.world.WorldServer) world, p);
        List<EntityLiving> after = soviets(world, p, 64);
        int tagged = 0;
        for (EntityLiving e : after)
        {
            tagged += before.contains(e) || !"military".equals(e.getEntityData().getString(NpcLoadouts.TAG)) ? 0 : 1;
        }
        DevTestResults.check(name(), "military spawner group", placed + ", " + tagged + " new",
                             placed && tagged >= 2, "2 or 3 new tier military NPCs");
        for (EntityLiving e : after)
        {
            e.setDead();
        }
    }

    /** Spawns n NPCs of a kind, checks each one, removes them; tier counts. */
    private Map<String, Integer> spawnBatch(World world, NpcKind kind, int n, int x, int z)
    {
        Map<String, Integer> tiers = new java.util.TreeMap<String, Integer>();
        int bad = 0;
        String firstBad = "";
        for (int i = 0; i < n; i++)
        {
            EntityLiving npc = kind == NpcKind.SOVIET ? Deci.newSoviet(world)
                : kind == NpcKind.SOLDIER ? newSoldier(world) : Deci.newBandit(world);
            npc.setPosition(x + 0.5 + i % 8, ground(world, x, z), z + 0.5 + i / 8);
            boolean added = world.spawnEntityInWorld(npc);
            String tier = npc.getEntityData().getString(NpcLoadouts.TAG);
            if (tier.isEmpty())
            {
                DevTestResults.value(name(), "untiered " + kind, npc.getClass().getName() + " added " + added
                    + " dead " + npc.isDead + " at " + (int) npc.posX + "," + (int) npc.posY + "," + (int) npc.posZ);
            }
            tiers.put(tier, count(tiers, tier) + 1);
            NpcLoadouts.Tier t = NpcLoadouts.instance().byName(tier);
            String problem = t == null ? "no tier" : t.kind != kind ? "kind " + t.kind : null;
            for (int s = 1; s <= 4 && problem == null; s++)
            {
                problem = npc.getEquipmentInSlot(s) == null ? "empty armor slot " + s : null;
            }
            ItemStack gun = Deci.npcGun(npc);
            String gunName = npc.getEntityData().getString(NpcLoadouts.GUN_TAG);
            if (problem == null && (gun == null || gun.getItem() != NpcLoadoutsAccess.item(gunName)))
            {
                problem = "gun " + gun + " vs " + gunName;
            }
            if (problem == null
                && npc.getEntityAttribute(SharedMonsterAttributes.maxHealth).getBaseValue() != t.health)
            {
                problem = "health " + npc.getMaxHealth();
            }
            if (problem != null)
            {
                bad++;
                firstBad = firstBad.isEmpty() ? problem : firstBad;
            }
            npc.setDead();
        }
        DevTestResults.check(name(), kind + " gear checks", (n - bad) + " / " + n + (bad > 0 ? " " + firstBad : ""),
                             bad == 0, "tier, 4 armor pieces, tier gun, tier health");
        return tiers;
    }

    private static EntityLiving newSoldier(World world)
    {
        // Deci has no soldier constructor accessor: the entity list knows it by name
        return (EntityLiving) net.minecraft.entity.EntityList.createEntityByName("Soldier", world);
    }

    private static int count(Map<String, Integer> m, String k)
    {
        Integer v = m.get(k);
        return v == null ? 0 : v;
    }

    private static List<EntityLiving> soviets(World world, EntityPlayerMP p, double r)
    {
        List<EntityLiving> out = new ArrayList<EntityLiving>();
        for (Object o : world.getEntitiesWithinAABB(EntityLiving.class, p.boundingBox.expand(r, 128, r)))
        {
            if (Deci.npcKind((Entity) o) == NpcKind.SOVIET)
            {
                out.add((EntityLiving) o);
            }
        }
        return out;
    }

    /** Nearest chunk of a military sector (spiral over 40 chunks), or null. */
    private static int[] militaryChunk(World world, EntityPlayerMP p)
    {
        int cx = (int) p.posX >> 4, cz = (int) p.posZ >> 4;
        for (int r = 0; r < 40; r++)
        {
            for (int dx = -r; dx <= r; dx++)
            {
                for (int dz = -r; dz <= r; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r
                        && Sectors.sector(world.getSeed(), cx + dx, cz + dz) == StructureGenerator.MIL)
                    {
                        return new int[] {cx + dx, cz + dz};
                    }
                }
            }
        }
        return null;
    }

    private static int ground(World world, int x, int z)
    {
        return world.getTopSolidOrLiquidBlock(x, z);
    }

    // ---- lineups

    private void showLineup(World world, EntityPlayerMP p, int l)
    {
        clear();
        clearOthers(world, p);
        NpcLoadouts loadouts = NpcLoadouts.instance();
        int x0 = (int) Math.floor(p.posX), z = (int) Math.floor(p.posZ) - 6;
        int y = ground(world, x0, z);
        Map<Integer, String> guns = new HashMap<Integer, String>();
        String[] names = LINEUPS[l];
        for (int i = 0; i < names.length; i++)
        {
            NpcLoadouts.Tier tier = loadouts.byName(names[i]);
            EntityLiving npc = tier.kind == NpcKind.SOVIET ? Deci.newSoviet(world)
                : tier.kind == NpcKind.SOLDIER ? newSoldier(world) : Deci.newBandit(world);
            loadouts.equip(npc, tier, npc.getEntityData());
            double x = x0 + 0.5 + (i - names.length / 2.0) * 1.2;
            npc.setPosition(x, ground(world, (int) Math.floor(x), z), z + 0.5);
            npc.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0);
            world.spawnEntityInWorld(npc);
            shown.add(npc);
            guns.put(npc.getEntityId(), npc.getEntityData().getString(NpcLoadouts.GUN_TAG));
        }
        shownGuns = guns;
        camX = x0 + 0.5;
        camY = y + 1.2;
        camZ = z + 6.5;
        p.setPositionAndUpdate(camX, camY, camZ);
        lineup = l;
    }

    /** Removes every other mob near the lineup (the reused world gathers infected, mechs, animals). */
    private void clearOthers(World world, EntityPlayerMP p)
    {
        for (Object o : world.getEntitiesWithinAABB(EntityLiving.class, p.boundingBox.expand(48, 32, 48)))
        {
            if (!shown.contains(o))
            {
                ((Entity) o).setDead();
            }
        }
    }

    /** Keeps the lineup in place, facing the camera (south). */
    private void hold()
    {
        for (EntityLiving npc : shown)
        {
            npc.motionX = npc.motionY = npc.motionZ = 0;
            npc.rotationYaw = npc.rotationYawHead = npc.renderYawOffset = 0;
            npc.setRevengeTarget(null);
            npc.setAttackTarget(null);
        }
    }

    private void clear()
    {
        for (EntityLiving npc : shown)
        {
            npc.setDead();
        }
        shown.clear();
    }

    /** Item lookup by registry name, for both threads. */
    static final class NpcLoadoutsAccess
    {
        static net.minecraft.item.Item item(String name)
        {
            return cpw.mods.fml.common.registry.GameRegistry.findItem("deci", name);
        }
    }
}
