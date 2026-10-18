package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

/**
 * With a gun in hand Decimation unbinds the right mouse button every client tick (ClientEventHandler.onClientTick, START:
 * keyBindUseItem key code 0, so right click only aims) and binds it again only while the crosshair is on an item frame.
 * So nobody holding a gun could open a corpse (bug.md, found by a joined player who came back armed). After Decimation's
 * tick (LOWEST) the button is bound again while the crosshair is on a body that opens on a right click: Decimation's
 * player corpse and our loot body. Decimation's own tick unbinds it once the crosshair leaves.
 */
@SideOnly(Side.CLIENT)
public final class GunUseKey
{
    /** Minecraft's code for the right mouse button (-100 + button 1). */
    static final int RIGHT_BUTTON = -99;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.START)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.keyBindUseItem.getKeyCode() != 0)
        {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        Entity aimed = mc.objectMouseOver == null ? null : mc.objectMouseOver.entityHit;
        if (held != null && Deci.isGun(held.getItem()) && opensOnClick(aimed))
        {
            mc.gameSettings.keyBindUseItem.setKeyCode(RIGHT_BUTTON); // what Decimation does for item frames
            KeyBinding.resetKeyBindingArrayAndHash();
        }
    }

    /** Bodies that open on a right click: Decimation's corpse, our loot body. */
    static boolean opensOnClick(Entity e)
    {
        return e != null && (Deci.isCorpse(e) || e instanceof LootBody);
    }
}
