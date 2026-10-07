package net.decimation.fixes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.mod.server.zones.a;
import net.decimation.mod.server.zones.b;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Singleplayer fix: zones never change what spawns.
 *
 * Decimation's spawn handler (obfuscated {@code deci.aK.d}) turns a quarter
 * of the infected spawning inside a MILITARY or POLICE zone into the
 * military / police variant, and cancels infected spawns inside a SAFEZONE.
 * Its event method is {@code @SideOnly(Side.SERVER)}, so FML strips it in
 * singleplayer, the same ServerProxy disease as the bottlecap fix. This
 * replicates the infected part 1:1 using the mod's own zone checks
 * ({@code net.decimation.mod.server.zones.b}).
 *
 * On a dedicated server the original handler IS registered, so this one
 * steps aside.
 */
public class ZoneSpawnHandler
{
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (FMLCommonHandler.instance().getSide().isServer())
        {
            return; // dedicated server: original deci.aK.d handles it
        }
        if (event.world.isRemote || !(event.entity instanceof deci.ag.d))
        {
            return;
        }
        deci.ag.d infected = (deci.ag.d) event.entity;
        double roll = Math.random();
        if (b.a(infected, a.MILITARY))
        {
            if (roll < 0.25)
            {
                infected.af(deci.am.b.MILITARY.id); // setVariant
            }
        }
        else if (b.a(infected, a.POLICE))
        {
            if (roll < 0.25)
            {
                infected.af(deci.am.b.POLICE.id); // setVariant
            }
        }
        else if (b.a(infected, a.SAFEZONE))
        {
            event.setCanceled(true);
        }
    }
}
