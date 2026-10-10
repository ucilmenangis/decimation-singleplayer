package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.registry.GameRegistry;
import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcKind;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Hits to kill a tiered NPC with a player's M4A4 (dev test npckill; user request 11 Oktober 2026:
 * "health point for this is take only 1 mag of 5.56"). Each hit is applied the way Decimation's
 * player gun hit handler does it (NetworkMessages PacketGunHit: attackEntityFrom(GunDamageSource
 * of the player, the gun's damage), then hurtResistantTime = max / 2, so a rapid next shot lands).
 * Logs per tier the damage of the first hit and the hits until it dies. -Ptiers=a,b picks tiers.
 */
public class NpcKillTest extends DevTestMode
{
    public String name() { return "npckill"; }

    private final String[] tiers = System.getProperty("deciworldgen.autotest.tiers",
        "juggernaut_sniper,elite_sniper,juggernaut,elite_military,military").split(",");
    private volatile int ticks;
    private volatile boolean done;

    public boolean client(net.minecraft.client.Minecraft mc)
    {
        return !done;
    }

    public void server()
    {
        if (++ticks != 20)
        {
            return;
        }
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        Item m4 = GameRegistry.findItem("deci", "m4a4");
        float dmg = Deci.gunDamageOf(new ItemStack(m4));
        DevTestResults.value(name(), "M4A4 damage per hit", dmg);
        NpcLoadouts l = NpcLoadouts.instance();
        for (String name : tiers)
        {
            NpcLoadouts.Tier tier = l.byName(name.trim());
            if (tier == null)
            {
                DevTestResults.value(name(), name, "no such tier");
                continue;
            }
            EntityLiving npc = null;
            for (int a = 0; a < 20; a++)
            {
                npc = tier.kind == NpcKind.BANDIT ? Deci.newBandit(w)
                    : tier.kind == NpcKind.SOLDIER ? Deci.newSoldier(w) : Deci.newSoviet(w);
                l.equip(npc, tier, npc.getEntityData());
                npc.setPosition(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 6.5);
                if (w.spawnEntityInWorld(npc))
                {
                    break;
                }
                npc = null;
            }
            if (npc == null)
            {
                DevTestResults.value(name(), name, "spawn refused");
                continue;
            }
            float h0 = npc.getHealth(), first = -1;
            int hits = 0;
            while (!npc.isDead && npc.getHealth() > 0 && hits < 1000)
            {
                float before = npc.getHealth();
                npc.attackEntityFrom(Deci.gunDamage(p), dmg);
                npc.hurtResistantTime = npc.maxHurtResistantTime / 2;
                hits++;
                if (first < 0)
                {
                    first = before - npc.getHealth();
                }
            }
            DevTestResults.value(name(), name + " health / first hit / hits", h0 + " / " + first + " / " + hits);
            npc.setDead();
        }
        // the sniper nerf numbers and the military wrecks' break time by hand in survival
        DevTestResults.check(name(), "NPC Barrett damage share", l.gunDamageShare("barrett"),
            Math.abs(l.gunDamageShare("barrett") - 0.6f) < 1e-4, "0.6");
        DevTestResults.check(name(), "NPC SVD damage share", l.gunDamageShare("svd"),
            Math.abs(l.gunDamageShare("svd") - 0.8f) < 1e-4, "0.8");
        DevTestResults.check(name(), "NPC AK-74 damage share", l.gunDamageShare("ak74"),
            l.gunDamageShare("ak74") == 1f, "1");
        boolean creative = p.capabilities.isCreativeMode;
        p.capabilities.isCreativeMode = false;
        int x = DevTestArena.X + 3, y = DevTestArena.Y, z = DevTestArena.Z + 3;
        for (String n : new String[] {"BlockWreckageMilitary1", "BlockWreckageMilitary2", "BlockWreckageMilitary3"})
        {
            net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromName("deci:" + n);
            w.setBlock(x, y, z, b, 3, 2);
            float perTick = b.getPlayerRelativeBlockHardness(p, w, x, y, z);
            float secs = perTick <= 0 ? Float.POSITIVE_INFINITY : 1f / perTick / 20f;
            DevTestResults.check(name(), n + " seconds by hand", secs, secs > 5f, "more than 5 (was one punch)");
            w.setBlockToAir(x, y, z);
        }
        p.capabilities.isCreativeMode = creative;
        done = true;
    }
}
