package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * The hand holding a gun was Steve for everyone (user 19 Oktober 2026: "even its real minecraft account player"):
 * Decimation's gun renderer draws the first person arms with GunItemRenderer.TJ, "skins/<name>", which only vanilla's
 * old skins.minecraft.net download could fill (shut down, Steve as the fallback); a player's real skin comes from
 * Minecraft's skin manager under another name. Each client tick the arms get the player's own skin location
 * (Steve only when the account has no skin).
 */
@SideOnly(Side.CLIENT)
public final class GunArmSkin
{
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null)
        {
            return;
        }
        ResourceLocation own = mc.thePlayer.getLocationSkin();
        if (own != null && !own.equals(Deci.gunArmSkin()))
        {
            Deci.setGunArmSkin(own);
        }
    }
}
