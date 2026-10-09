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
        {"military", "military", "military", "military"},
        {"juggernaut", "juggernaut", "juggernaut"},
        {"elite_military", "elite_military", "elite_military"},
        {"juggernaut_sniper", "elite_sniper", "elite_sniper", "elite_sniper"}};

    private volatile boolean done;
    private volatile int ticks;
    /** Lineup shown now (index into LINEUPS), -1 none; its entities and their guns. */
    private volatile int lineup = -1;
    private final List<EntityLiving> shown = new ArrayList<EntityLiving>();
    private volatile Map<Integer, String> shownGuns = new HashMap<Integer, String>();
    private volatile double camX, camY, camZ;
    private int clientMatch, clientTotal, checked = -1, shot = -1, eggTicks, eliteOk, eliteTotal;

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
                    if (g != null && g.getItem() == NpcLoadoutsAccess.item(NpcLoadouts.gunName(e.getValue())))
                    {
                        clientMatch++;
                        if (e.getValue().contains("mask="))
                        {
                            // elite: goggles on the face and the attachments on the client's gun
                            ItemStack m = Deci.npcMask(c);
                            boolean kit = g.stackTagCompound != null && !g.stackTagCompound.getString("sightAttach").isEmpty()
                                && !g.stackTagCompound.getString("barrelAttach").isEmpty();
                            eliteTotal++;
                            eliteOk += m != null && m.getItem() == NpcLoadoutsAccess.item("nvgoggles") && kit ? 1 : 0;
                        }
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
        if (t > 300 + LINEUPS.length * 200 + 20 && eggTicks < 40)
        {
            // the eggs in the hotbar, GUI on
            mc.gameSettings.hideGUI = false;
            net.minecraft.item.Item egg = cpw.mods.fml.common.registry.GameRegistry.findItem("deciworldgen", "npc_egg");
            if (eggTicks++ == 0 && egg != null)
            {
                for (int i = 0; i < 9; i++)
                {
                    mc.thePlayer.inventory.mainInventory[i] = NpcLoadouts.instance().byIndex(i) == null ? null
                        : new ItemStack(egg, 1, i);
                }
                mc.thePlayer.inventory.currentItem = 4;
            }
            if (eggTicks == 39)
            {
                DevTestUtil.screenshot(mc, name(), "npc_eggs");
            }
            return true;
        }
        if (t > 300 + LINEUPS.length * 200 + 20)
        {
            mc.gameSettings.hideGUI = false;
            DevTestResults.check(name(), "client shows the server's gun", clientMatch + " / " + clientTotal,
                                 clientTotal > 0 && clientMatch == clientTotal, "all lineup NPCs");
            DevTestResults.check(name(), "elite goggles and attachments on the client", eliteOk + " / " + eliteTotal,
                                 eliteTotal > 0 && eliteOk == eliteTotal, "all elite lineup NPCs");
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
        DevTestArena.build(world, p);
        int bx = DevTestArena.X, bz = DevTestArena.Z - 10;
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
        for (String tn : new String[] {"bandit_light", "bandit_heavy", "military", "juggernaut", "elite_military"})
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

        // factions (PatchFactions): Soviets leave Soviets alone, everyone else fights them
        EntityLiving s1 = Deci.newSoviet(world), s2 = Deci.newSoviet(world), b1 = Deci.newBandit(world),
            so1 = Deci.newSoldier(world);
        boolean[] got = {Deci.npcHostileTo(s1, s2), Deci.npcHostileTo(s1, b1), Deci.npcHostileTo(s1, so1),
                         Deci.npcHostileTo(b1, s1), Deci.npcHostileTo(so1, s1)};
        boolean[] want = {false, true, true, true, true};
        DevTestResults.check(name(), "factions soviet>soviet, soviet>bandit, soviet>soldier, bandit>soviet, soldier>soviet",
                             java.util.Arrays.toString(got), java.util.Arrays.equals(got, want), java.util.Arrays.toString(want));

        // juggernaut: slow, no knockback, one matching armor set
        EntityLiving jug = Deci.newSoviet(world);
        loadouts.equip(jug, loadouts.byName("juggernaut"), jug.getEntityData());
        String helm = net.minecraft.item.Item.itemRegistry.getNameForObject(jug.getEquipmentInSlot(4).getItem());
        boolean gray = helm.endsWith("Gray");
        boolean matched = true;
        for (int sl = 1; sl <= 4; sl++)
        {
            String n = net.minecraft.item.Item.itemRegistry.getNameForObject(jug.getEquipmentInSlot(sl).getItem());
            matched &= n.contains("juggernaut") && n.endsWith("Gray") == gray;
        }
        double speed = jug.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue();
        double kb = jug.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getBaseValue();
        DevTestResults.check(name(), "juggernaut set / speed / knockback", matched + " / " + speed + " / " + kb,
                             matched && Math.abs(speed - 0.18) < 1e-6 && kb == 1.0, "true / 0.18 / 1.0");

        // .50 BMG (NpcShots.armorPiercing): armor at half strength against a full marine body set
        String[] marine = {"marineBoots", "marinePants", "marineVest"};
        p.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
        for (int sl = 0; sl < 3; sl++)
        {
            p.setCurrentItemOrArmor(sl + 1, new ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("deci", marine[sl])));
        }
        float normal = hitPlayer(p, false), piercing = hitPlayer(p, true);
        net.decimation.fixes.NpcShots.damageScale = 2;
        float elite = hitPlayer(p, false);
        net.decimation.fixes.NpcShots.damageScale = 1;
        DevTestResults.check(name(), "elite hit 6 on a marine body set", String.format("%.2f (normal %.2f)", elite, normal),
                             Math.abs(elite - 2 * normal) < 0.05, "2 x normal (damageDealt 2)");
        for (int sl = 1; sl <= 3; sl++)
        {
            p.setCurrentItemOrArmor(sl, null);
        }
        p.setHealth(p.getMaxHealth());
        p.setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
        DevTestResults.check(name(), "barrett hit 6 on a marine body set", String.format("%.2f (normal %.2f)", piercing, normal),
                             piercing > normal * 2, "more than twice a normal hit (armor at half strength)");

        // sniper versions: juggernaut_sniper always a Barrett, elite_sniper only sniper rifles, base ones none
        int jugBarrett = 0, eliteSnipers = 0, baseSnipers = 0, n = 40;
        for (int k = 0; k < n; k++)
        {
            for (String tn : new String[] {"juggernaut_sniper", "elite_sniper", "juggernaut", "elite_military"})
            {
                EntityLiving e = Deci.newSoviet(world);
                loadouts.equip(e, loadouts.byName(tn), e.getEntityData());
                String g = NpcLoadouts.gunName(e.getEntityData().getString(NpcLoadouts.GUN_TAG));
                boolean sniper = java.util.Arrays.asList("barrett", "l115a3", "jng90", "sv98", "m110").contains(g);
                jugBarrett += tn.equals("juggernaut_sniper") && g.equals("barrett") ? 1 : 0;
                eliteSnipers += tn.equals("elite_sniper") && sniper ? 1 : 0;
                baseSnipers += !tn.endsWith("sniper") && sniper ? 1 : 0;
            }
        }
        DevTestResults.check(name(), "sniper tiers (jugg Barrett, elite snipers, base snipers)",
                             jugBarrett + " / " + eliteSnipers + " / " + baseSnipers,
                             jugBarrett == n && eliteSnipers == n && baseSnipers == 0, n + " / " + n + " / 0");

        // every tier's spawn egg spawns that tier
        net.minecraft.item.Item egg = cpw.mods.fml.common.registry.GameRegistry.findItem("deciworldgen", "npc_egg");
        int eggs = 0, eggOk = 0;
        String eggBad = "";
        for (int i = 0; egg != null && loadouts.byIndex(i) != null; i++)
        {
            eggs++;
            int y = ground(world, bx, bz) - 1;
            List<EntityLiving> before0 = humans(world, bx, y, bz);
            // a soldier egg can come out as a mech (Decimation's 5% swap): try again
            for (int attempt = 0; attempt < 5 && humans(world, bx, y, bz).size() == before0.size(); attempt++)
            {
                egg.onItemUse(new ItemStack(egg, 1, i), p, world, bx, y, bz, 1, 0.5f, 1, 0.5f);
            }
            for (EntityLiving e : humans(world, bx, y, bz))
            {
                if (!before0.contains(e))
                {
                    boolean ok = loadouts.byIndex(i).name.equals(e.getEntityData().getString(NpcLoadouts.TAG));
                    eggOk += ok ? 1 : 0;
                    eggBad += ok ? "" : " " + i;
                    e.setDead();
                }
            }
        }
        DevTestResults.check(name(), "spawn eggs", eggOk + " / " + eggs + eggBad, eggs > 0 && eggOk == eggs,
                             "one egg per tier spawns that tier");

        // the spawner places a tiered group near the player
        MilitarySpawner spawner = new MilitarySpawner(new File(Minecraft.getMinecraft().mcDataDir, "config"));
        List<EntityLiving> before = soviets(world, p, 64);
        boolean placed = spawner.spawnGroup((net.minecraft.world.WorldServer) world, p);
        List<EntityLiving> after = soviets(world, p, 64);
        int tagged = 0;
        for (EntityLiving e : after)
        {
            String tg = e.getEntityData().getString(NpcLoadouts.TAG);
            tagged += before.contains(e) || !(tg.startsWith("military") || tg.startsWith("juggernaut") || tg.startsWith("elite"))
                ? 0 : 1;
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
        int bad = 0, refused = 0;
        String firstBad = "";
        for (int i = 0; i < n; i++)
        {
            EntityLiving npc = kind == NpcKind.SOVIET ? Deci.newSoviet(world)
                : kind == NpcKind.SOLDIER ? newSoldier(world) : Deci.newBandit(world);
            npc.setPosition(x + 0.5 + i % 8, ground(world, x, z), z + 0.5 + i / 8);
            if (!world.spawnEntityInWorld(npc))
            {
                // Decimation turns 5% of soldier spawns into a mech (PlayerJoinSync): not ours to check
                refused++;
                continue;
            }
            String tier = npc.getEntityData().getString(NpcLoadouts.TAG);
            tiers.put(tier, count(tiers, tier) + 1);
            NpcLoadouts.Tier t = NpcLoadouts.instance().byName(tier);
            String problem = t == null ? "no tier" : t.kind != kind ? "kind " + t.kind : null;
            for (int s = 1; s <= 4 && problem == null; s++)
            {
                problem = npc.getEquipmentInSlot(s) == null ? "empty armor slot " + s : null;
            }
            ItemStack gun = Deci.npcGun(npc);
            String gunName = npc.getEntityData().getString(NpcLoadouts.GUN_TAG);
            if (problem == null && (gun == null || gun.getItem() != NpcLoadoutsAccess.item(NpcLoadouts.gunName(gunName))))
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
        DevTestResults.check(name(), kind + " gear checks", (n - refused - bad) + " / " + (n - refused)
                             + (refused > 0 ? " (" + refused + " refused, mech)" : "") + (bad > 0 ? " " + firstBad : ""),
                             bad == 0, "tier, 4 armor pieces, tier gun, tier health");
        return tiers;
    }

    /** One NPC gun hit of 6 (a Barrett, 50 / 8) on the player; armor piercing or not. */
    private static float hitPlayer(EntityPlayerMP p, boolean piercing)
    {
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        net.decimation.fixes.NpcShots.armorPiercing = piercing;
        try
        {
            p.attackEntityFrom(Deci.humanDamage(), 6.0f);
        }
        finally
        {
            net.decimation.fixes.NpcShots.armorPiercing = false;
        }
        return before - p.getHealth();
    }

    private static EntityLiving newSoldier(World world)
    {
        return Deci.newSoldier(world);
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

    private static List<EntityLiving> humans(World world, int x, int y, int z)
    {
        List<EntityLiving> out = new ArrayList<EntityLiving>();
        for (Object o : world.getEntitiesWithinAABB(EntityLiving.class,
            net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 2, y - 2, z - 2, x + 3, y + 4, z + 3)))
        {
            if (Deci.npcKind((Entity) o) != null)
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

    /** The arena floor inside it (DevTestArena), else the ground (the military sector batch). */
    private static int ground(World world, int x, int z)
    {
        if (x >= DevTestArena.X - DevTestArena.BACK && x <= DevTestArena.X + DevTestArena.FRONT
            && Math.abs(z - DevTestArena.Z) <= DevTestArena.SIDE)
        {
            return DevTestArena.Y;
        }
        return world.getTopSolidOrLiquidBlock(x, z);
    }

    // ---- lineups

    private void showLineup(World world, EntityPlayerMP p, int l)
    {
        clear();
        clearOthers(world, p);
        NpcLoadouts loadouts = NpcLoadouts.instance();
        int x0 = DevTestArena.X, z = DevTestArena.Z - 6;
        int y = DevTestArena.Y;
        Map<Integer, String> guns = new HashMap<Integer, String>();
        String[] names = LINEUPS[l];
        // plants between the camera and the lineup hide it (fresh worlds grow tall grass)
        for (int dx = -6; dx <= 6; dx++)
        {
            for (int dz = -1; dz <= 8; dz++)
            {
                for (int dy = 0; dy <= 3; dy++)
                {
                    net.minecraft.block.Block b = world.getBlock(x0 + dx, y + dy, z + dz);
                    if (b.getMaterial() == net.minecraft.block.material.Material.plants
                        || b.getMaterial() == net.minecraft.block.material.Material.vine)
                    {
                        world.setBlockToAir(x0 + dx, y + dy, z + dz);
                    }
                }
            }
        }
        for (int i = 0; i < names.length; i++)
        {
            NpcLoadouts.Tier tier = loadouts.byName(names[i]);
            EntityLiving npc = null;
            // Decimation swaps 5% of soldier spawns for a mech (PlayerJoinSync): try again
            for (int attempt = 0; attempt < 10; attempt++)
            {
                npc = tier.kind == NpcKind.SOVIET ? Deci.newSoviet(world)
                    : tier.kind == NpcKind.SOLDIER ? newSoldier(world) : Deci.newBandit(world);
                loadouts.equip(npc, tier, npc.getEntityData());
                double x = x0 + 0.5 + (i - names.length / 2.0) * 1.2;
                npc.setPosition(x, ground(world, (int) Math.floor(x), z), z + 0.5);
                npc.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0);
                if (world.spawnEntityInWorld(npc))
                {
                    break;
                }
            }
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
