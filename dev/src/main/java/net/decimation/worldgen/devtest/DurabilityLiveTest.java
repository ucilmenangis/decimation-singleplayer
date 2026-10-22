package net.decimation.worldgen.devtest;

import net.decimation.fixes.ArmorGunfireHandler;
import net.decimation.fixes.Deci;
import net.decimation.fixes.Durability;
import net.decimation.fixes.GunWear;
import net.decimation.fixes.GunWearClient;
import net.decimation.fixes.RepairPart;
import net.decimation.mod.common.item.armor.ItemArmorDeci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Durability (feature 10, user 22 Oktober 2026; docs/superpowers/specs/2026-10-22-durability-design.md). On the arena,
 * survival, natural regeneration and mob spawning off, mobs swept: a full Marine set and a Military Vest. An NPC shot
 * ("human") takes the stronger of vest and chest piece and wears each counted piece by 1; a used up piece protects
 * nothing and stays on its maximum; a player's shot as Decimation's hit handler hands it over (boots, legs, chest already
 * multiplied) gets the used up boots taken back out and the vest in; a melee hit on a piece with 1 left ends on the
 * maximum, never destroying it. Then an M4A4: three real shots (Deci.fireHeldGun, the mouse path) wear 3; a jammed gun
 * does not fire and R racks it (the press taken before Decimation's reload); a worn out gun does not fire; each part
 * repairs a quarter and is used up; nothing to repair uses nothing; parts in military and police loot, none in civilian.
 * The hooks' costs.
 */
public class DurabilityLiveTest extends DevTestMode
{
    public String name() { return "durability"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    private static final int BOOTS = 0, LEGS = 1, CHEST = 2, HELMET = 3;
    private int ticks, ammoBefore, racksBefore;
    /** Our NPC damage to players (NpcLoadouts: npcDamageToPlayer x NpcShots.damageScale), measured on a bare hit. */
    private float npcScale = 1f;
    private static final float NPC_HIT = 0.6f;
    private volatile Runnable onServer;
    private String spawningBefore = "true";
    private boolean regenBefore = true;
    private net.minecraft.world.WorldSettings.GameType modeBefore;

    public boolean client(Minecraft mc)
    {
        ticks++;
        if (ticks == 1) onServer = this::setup;
        if (ticks == 10) onServer = this::npcShot;
        if (ticks == 15) onServer = this::brokenBoots;
        if (ticks == 20) onServer = this::playerShot;
        if (ticks == 25) onServer = this::melee;
        if (ticks == 30) onServer = this::gunSetup;
        if (ticks == 40 || ticks == 46 || ticks == 52)
        {
            Deci.fireHeldGun(mc.thePlayer);
        }
        if (ticks == 70) onServer = this::threeShots;
        if (ticks == 80)
        {
            Deci.fireHeldGun(mc.thePlayer);                 // jammed: must not fire
        }
        if (ticks == 90) onServer = () -> unchanged("a jammed gun does not fire", 1400);
        if (ticks == 92)
        {
            racksBefore = GunWearClient.rackCount();
            KeyBinding r = Deci.reloadKey();
            KeyBinding.setKeyBindState(r.getKeyCode(), true);
            KeyBinding.onTick(r.getKeyCode());              // one press, as the keyboard makes it
        }
        if (ticks == 95)
        {
            KeyBinding.setKeyBindState(Deci.reloadKey().getKeyCode(), false);
        }
        if (ticks == 110)
        {
            final int racked = GunWearClient.rackCount() - racksBefore;
            onServer = () -> racked(racked);
        }
        if (ticks == 125)
        {
            Deci.fireHeldGun(mc.thePlayer);                 // worn out: must not fire
        }
        if (ticks == 135) onServer = () -> unchanged("a worn out gun does not fire", 1500);
        if (ticks == 140) onServer = this::repairs;
        if (ticks == 145) onServer = this::loot;
        if (ticks == 147) onServer = this::hurtCost;
        if (ticks == 150)
        {
            DevTestResults.check(name(), "shot hook cost", String.format("%.2f us over %d shots", GunWear.averageMicros(), GunWear.shotCount()),
                GunWear.shotCount() >= 3 && GunWear.averageMicros() < 50, "under 50 us a shot");
            onServer = this::restore;
        }
        return ticks < 160;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    private static Item item(String name)
    {
        return (Item) Item.itemRegistry.getObject(name);
    }

    private static float mult(ItemStack s)
    {
        return s == null ? 1f : ((ItemArmorDeci) s.getItem()).getDamageMultiplier();
    }

    private static float npc(float m)
    {
        return 1f - 0.8f * (1f - m);                        // ArmorGunfireHandler.againstNpc
    }

    private void setup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        w.setWorldTime(6000);
        modeBefore = p.theItemInWorldManager.getGameType();
        p.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
        p.capabilities.disableDamage = false;
        regenBefore = w.getGameRules().getGameRuleBooleanValue("naturalRegeneration");
        w.getGameRules().setOrCreateGameRule("naturalRegeneration", "false");
        spawningBefore = w.getGameRules().getGameRuleStringValue("doMobSpawning");
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        sweep(w);
        Deci.player(p).setSafezoneGrace(0);
        p.inventory.clearInventory(null, -1);
        for (int i = 0; i < 4; i++)
        {
            p.inventory.armorInventory[i] = null;
        }
        Deci.setPlayerGear(p, null, null, null);
        npcScale = hit(p, Deci.humanDamage(), NPC_HIT) / NPC_HIT;   // bare: only our NPC damage factor
        DevTestResults.value(name(), "NPC damage factor on players", String.format("%.2f", npcScale));
        p.inventory.armorInventory[BOOTS] = new ItemStack(item("deci:marineBoots"));
        p.inventory.armorInventory[LEGS] = new ItemStack(item("deci:marinePants"));
        p.inventory.armorInventory[CHEST] = new ItemStack(item("deci:marineVest"));
        p.inventory.armorInventory[HELMET] = new ItemStack(item("deci:marineHelm"));
        Deci.setPlayerGear(p, new ItemStack(item("deci:militaryvest")), null, null);
        p.setHealth(p.getMaxHealth());
        p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
        DevTestResults.value(name(), "maxima", "boots " + p.inventory.armorInventory[BOOTS].getMaxDamage() + ", vest "
            + Deci.playerVest(p).getMaxDamage());
    }

    private void sweep(World w)
    {
        for (Object o : w.loadedEntityList)
        {
            Entity e = (Entity) o;
            if (!e.isDead && e instanceof net.minecraft.entity.EntityLiving && Math.abs(e.posX - X) < 120 && Math.abs(e.posZ - Z) < 120)
            {
                e.setDead();
            }
        }
    }

    /** Health lost to one hit of this source and amount, from full health, no hurt cooldown. */
    private static float hit(EntityPlayerMP p, DamageSource source, float amount)
    {
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        float before = p.getHealth();
        p.attackEntityFrom(source, amount);
        return before - p.getHealth();
    }

    private void npcShot()
    {
        EntityPlayerMP p = DevTestUtil.player();
        ItemStack[] a = p.inventory.armorInventory;
        ItemStack vest = Deci.playerVest(p);
        float lost = hit(p, Deci.humanDamage(), NPC_HIT);
        boolean head = a[HELMET].getItemDamage() == 1;
        float expect = NPC_HIT * npcScale * npc(mult(a[BOOTS])) * npc(mult(a[LEGS])) * npc(Durability.bodyFactor(mult(a[CHEST]), mult(vest)))
                       * (head ? npc(mult(a[HELMET])) : 1f);
        DevTestResults.check(name(), "NPC shot: the stronger of vest and chest counts", String.format("lost %.3f, expected %.3f%s", lost,
            expect, head ? " (headshot)" : ""), Math.abs(lost - expect) < 0.02, "equal");
        String wear = "boots " + a[BOOTS].getItemDamage() + ", legs " + a[LEGS].getItemDamage() + ", chest " + a[CHEST].getItemDamage()
                      + ", vest " + vest.getItemDamage() + ", helmet " + a[HELMET].getItemDamage();
        DevTestResults.check(name(), "NPC shot wears each counted piece by 1", wear, a[BOOTS].getItemDamage() == 1
            && a[LEGS].getItemDamage() == 1 && a[CHEST].getItemDamage() == 0 && vest.getItemDamage() == 1, "boots, legs, vest 1; chest 0");
    }

    private void brokenBoots()
    {
        EntityPlayerMP p = DevTestUtil.player();
        ItemStack[] a = p.inventory.armorInventory;
        ItemStack vest = Deci.playerVest(p);
        a[BOOTS].setItemDamage(a[BOOTS].getMaxDamage());
        int helmetBefore = a[HELMET].getItemDamage();
        float lost = hit(p, Deci.humanDamage(), NPC_HIT);
        boolean head = a[HELMET].getItemDamage() > helmetBefore;
        float expect = NPC_HIT * npcScale * npc(mult(a[LEGS])) * npc(Durability.bodyFactor(mult(a[CHEST]), mult(vest))) * (head ? npc(mult(a[HELMET])) : 1f);
        DevTestResults.check(name(), "a used up piece protects nothing and stays on its maximum", String.format("lost %.3f, expected %.3f, "
            + "boots %d of %d", lost, expect, a[BOOTS].getItemDamage(), a[BOOTS].getMaxDamage()), Math.abs(lost - expect) < 0.02
            && a[BOOTS].getItemDamage() == a[BOOTS].getMaxDamage(), "equal, boots on the maximum");
    }

    private void playerShot()
    {
        EntityPlayerMP p = DevTestUtil.player();
        ItemStack[] a = p.inventory.armorInventory;
        ItemStack vest = Deci.playerVest(p);
        // a shooter behind its own aim (facing away): never a headshot
        Entity shooter = Deci.newInfected(p.worldObj);
        shooter.setLocationAndAngles(X + 3.5, Y, Z + 0.5, -90f, 0f);
        float handedOver = 4f * mult(a[BOOTS]) * mult(a[LEGS]) * mult(a[CHEST]);   // as Decimation's hit handler multiplies
        float lost = hit(p, Deci.gunDamage(shooter), handedOver);
        float expect = 4f * mult(a[LEGS]) * Durability.bodyFactor(mult(a[CHEST]), mult(vest));
        DevTestResults.check(name(), "player shot: used up boots out, the vest in", String.format("lost %.3f, expected %.3f (handed over %.3f)",
            lost, expect, handedOver), Math.abs(lost - expect) < 0.02, "equal");
    }

    private void melee()
    {
        EntityPlayerMP p = DevTestUtil.player();
        ItemStack[] a = p.inventory.armorInventory;
        a[LEGS].setItemDamage(a[LEGS].getMaxDamage() - 1);
        Entity zombie = Deci.newInfected(p.worldObj);
        zombie.setLocationAndAngles(X + 1.5, Y, Z + 0.5, 90f, 0f);
        hit(p, DamageSource.causeMobDamage((net.minecraft.entity.EntityLivingBase) zombie), 6f);
        ItemStack legs = p.inventory.armorInventory[LEGS];
        DevTestResults.check(name(), "a melee hit ends on the maximum, never destroys", legs == null ? "destroyed"
            : legs.getItemDamage() + " of " + legs.getMaxDamage(), legs != null && legs.getItemDamage() == legs.getMaxDamage(), "kept, on the maximum");
        // a big hit on a used up piece: kept on its maximum, never set back (an automated review found the first clamp
        // repairing used up pieces by several points a hit)
        a[CHEST].setItemDamage(a[CHEST].getMaxDamage());
        hit(p, DamageSource.causeMobDamage((net.minecraft.entity.EntityLivingBase) zombie), 18f);
        ItemStack chest = p.inventory.armorInventory[CHEST];
        DevTestResults.check(name(), "a big melee hit on a used up piece: no free repair", chest == null ? "destroyed"
            : chest.getItemDamage() + " of " + chest.getMaxDamage(), chest != null && chest.getItemDamage() == chest.getMaxDamage(), "kept, on the maximum");
    }

    private void gunSetup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        ItemStack gun = new ItemStack(item("deci:m4a4"));
        gun.stackTagCompound = new NBTTagCompound();
        gun.stackTagCompound.setInteger("ammo", 30);
        p.inventory.setInventorySlotContents(0, gun);
        p.inventory.setInventorySlotContents(1, new ItemStack(RepairPart.gunParts, 2));
        p.inventory.setInventorySlotContents(2, new ItemStack(RepairPart.armorPart, 2));
        p.inventory.setInventorySlotContents(3, new ItemStack(RepairPart.armorPlate, 2));
        p.inventory.currentItem = 0;
        DevTestResults.value(name(), "rifle", Deci.gunCategory(gun.getItem()) + ", lasts " + GunWear.max(gun) + " rounds");
    }

    private static NBTTagCompound gunTag()
    {
        return DevTestUtil.player().inventory.getStackInSlot(0).stackTagCompound;
    }

    private void threeShots()
    {
        NBTTagCompound t = gunTag();
        ammoBefore = t.getInteger("ammo");
        DevTestResults.check(name(), "three real shots wear 3", "wear " + t.getInteger(GunWear.WEAR) + ", ammo " + ammoBefore,
            t.getInteger(GunWear.WEAR) == 3 && ammoBefore == 27, "wear 3, ammo 27");
        t.setInteger(GunWear.WEAR, 1400);
        t.setBoolean(GunWear.JAM, true);
        DevTestUtil.player().inventoryContainer.detectAndSendChanges();
    }

    private void unchanged(String what, int wear)
    {
        NBTTagCompound t = gunTag();
        DevTestResults.check(name(), what, "ammo " + t.getInteger("ammo") + ", wear " + t.getInteger(GunWear.WEAR), t.getInteger("ammo") == ammoBefore
            && t.getInteger(GunWear.WEAR) == wear, "ammo " + ammoBefore + ", wear " + wear);
    }

    private void racked(int racked)
    {
        NBTTagCompound t = gunTag();
        DevTestResults.check(name(), "R racks a jammed gun (no reload)", "jammed " + t.getBoolean(GunWear.JAM) + ", racks " + racked + ", ammo "
            + t.getInteger("ammo"), !t.getBoolean(GunWear.JAM) && racked == 1 && t.getInteger("ammo") == ammoBefore, "not jammed, 1 rack, ammo kept");
        t.setInteger(GunWear.WEAR, 1500);
        DevTestUtil.player().inventoryContainer.detectAndSendChanges();
    }

    private void repairs()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        ItemStack parts = p.inventory.getStackInSlot(1), kit = p.inventory.getStackInSlot(2), plate = p.inventory.getStackInSlot(3);
        parts.useItemRightClick(w, p);
        DevTestResults.check(name(), "Gun Parts repair a quarter", "wear " + gunTag().getInteger(GunWear.WEAR) + ", parts left " + parts.stackSize,
            gunTag().getInteger(GunWear.WEAR) == 1125 && parts.stackSize == 1, "wear 1125, 1 left");
        ItemStack[] a = p.inventory.armorInventory;
        for (ItemStack s : a)
        {
            s.setItemDamage(0);
        }
        a[HELMET].setItemDamage(200);
        kit.useItemRightClick(w, p);
        DevTestResults.check(name(), "an Armor Repair Kit repairs a quarter of the most worn piece", "helmet " + a[HELMET].getItemDamage()
            + ", kits left " + kit.stackSize, a[HELMET].getItemDamage() == 136 && kit.stackSize == 1, "helmet 136, 1 left");
        ItemStack vest = Deci.playerVest(p);
        vest.setItemDamage(vest.getMaxDamage());
        plate.useItemRightClick(w, p);
        DevTestResults.check(name(), "an Armor Plate repairs a quarter of the vest", "vest " + vest.getItemDamage() + ", plates left "
            + plate.stackSize, vest.getItemDamage() == 384 && plate.stackSize == 1, "vest 384, 1 left");
        a[HELMET].setItemDamage(0);
        kit.useItemRightClick(w, p);
        DevTestResults.check(name(), "nothing to repair uses nothing", "kits left " + kit.stackSize, kit.stackSize == 1, "1 left");
    }

    private void loot()
    {
        int mil = 0, police = 0, civ = 0;
        for (java.util.Map.Entry<String, java.util.Map.Entry<Integer, java.util.List<ItemStack>>> e : Deci.lootTable().entrySet())
        {
            int n = 0;
            for (ItemStack s : e.getValue().getValue())
            {
                n += s != null && s.getItem() instanceof RepairPart ? 1 : 0;
            }
            String k = e.getKey();
            if (k.contains("MilitaryCrate")) mil += n;
            if (k.contains("PoliceCrate")) police += n;
            if (k.contains("minecraft:chest") || k.contains("MetalShelf")) civ += n;
        }
        DevTestResults.check(name(), "parts in military and police loot only", "military " + mil + ", police " + police + ", civilian " + civ,
            mil > 0 && police > 0 && civ == 0, "military and police above 0, civilian 0");
    }

    /** The hurt hook's cost after warm up: 200 NPC hits, timed by its own counters. */
    private void hurtCost()
    {
        EntityPlayerMP p = DevTestUtil.player();
        long[] before = ArmorGunfireHandler.totals();
        for (int i = 0; i < 200; i++)
        {
            hit(p, Deci.humanDamage(), 0.01f);
        }
        long[] after = ArmorGunfireHandler.totals();
        double us = (after[1] - before[1]) / 1000.0 / Math.max(1, after[0] - before[0]);
        DevTestResults.check(name(), "hurt hook cost", String.format("%.2f us over %d hits (first hits, cold: %.1f us)", us, after[0] - before[0],
            before[0] == 0 ? 0 : before[1] / 1000.0 / before[0]), us < 50, "under 50 us a hurt event");
    }

    private void restore()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        w.getGameRules().setOrCreateGameRule("naturalRegeneration", String.valueOf(regenBefore));
        w.getGameRules().setOrCreateGameRule("doMobSpawning", spawningBefore);
        p.inventory.clearInventory(null, -1);
        for (int i = 0; i < 4; i++)
        {
            p.inventory.armorInventory[i] = null;
        }
        Deci.setPlayerGear(p, null, null, null);
        p.setHealth(p.getMaxHealth());
        if (modeBefore != null)
        {
            p.setGameType(modeBefore);                        // as the world had it (a survival player was later killed)
        }
    }
}
