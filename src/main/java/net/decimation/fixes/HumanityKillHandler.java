package net.decimation.fixes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Singleplayer fix: humanity never changed from ordinary kills.
 *
 * Decimation's death handler (obfuscated {@code deci.an.f}, registered on
 * both sides) adds a human NPC's own humanity value on kill, which already
 * works. The rest of its humanity rules sit behind an isServer() gate and
 * therefore only ran on a dedicated server:
 * - killing anything that is not a player: +1 humanity;
 * - killing a player with no bounty and humanity >= 50 (an "innocent"):
 *   -10 humanity; any other player: +1.
 * On a dedicated server those go to the clan instead when the killer is in a
 * clan; clans only exist there, so this replicates the no clan branch and
 * steps aside on a dedicated server, where the original still runs.
 */
public class HumanityKillHandler
{
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event)
    {
        if (FMLCommonHandler.instance().getSide().isServer())
        {
            return; // dedicated server: original deci.an.f branch handles it
        }
        if (event.entityLiving.worldObj.isRemote
            || !(event.source.getEntity() instanceof EntityPlayer))
        {
            return;
        }
        EntityPlayer killer = (EntityPlayer) event.source.getEntity();
        deci.Q.b data = deci.Q.b.e(killer); // PlayerData.get
        if (data == null)
        {
            return;
        }
        if (!(event.entityLiving instanceof EntityPlayer))
        {
            data.I(1); // addHumanity
            return;
        }
        deci.Q.b victim = deci.Q.b.e((EntityPlayer) event.entityLiving);
        if (victim != null && victim.cc() <= 0L && victim.cd() >= 50) // getBounty, getHumanity
        {
            data.J(10); // removeHumanity
        }
        else
        {
            data.I(1);
        }
    }
}
