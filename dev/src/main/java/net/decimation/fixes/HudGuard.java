package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Decimation's HUD draws the menu's player preview with fancy graphics (IngameHud.render reads DecimationMod's
 * fakePlayerProfile without a null check) and crashed a game that reached a world without Decimation's menu (a launcher's
 * join on start; bug.md). In a world without the preview, make it as the menu does (each tick: one field read).
 */
@SideOnly(Side.CLIENT)
public final class HudGuard
{
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.START && mc.theWorld != null && !Deci.hasMenuPreview())
        {
            Deci.ensureMenuPreview(); // before this frame's HUD draws
        }
    }
}
