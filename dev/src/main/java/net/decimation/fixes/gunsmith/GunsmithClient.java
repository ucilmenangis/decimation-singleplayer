package net.decimation.fixes.gunsmith;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * The client's way in: only the client calls it, so the dedicated server never loads GunsmithScreen (a GUI class). While
 * the Gunsmith screen is open the HUD (Decimation's stats and needs, chat, the hotbar) and the gun in the hand are not
 * drawn: the screen shows the gun alone, as the mockup did.
 */
public final class GunsmithClient
{
    private static boolean listening;

    private GunsmithClient()
    {
    }

    /** The Gunsmith screen over a container of the client player's own (FML gives it the server's window id). */
    public static Object screen(EntityPlayer player)
    {
        if (!listening)
        {
            listening = true;
            MinecraftForge.EVENT_BUS.register(new GunsmithClient());
        }
        return new GunsmithScreen(new GunsmithContainer(player));
    }

    /** The Gunsmith screen is up (ScopeZoom: no scope glass work while it hides the hand). */
    public static boolean isOpen()
    {
        return Minecraft.getMinecraft().currentScreen instanceof GunsmithScreen;
    }

    @SubscribeEvent
    public void overlay(RenderGameOverlayEvent.Pre e)
    {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL && isOpen())
        {
            // the HUD's draw is also where a world's GUI view gets set up (GuiIngameForge, before its other elements):
            // set it up here, then skip the HUD
            Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void hand(RenderHandEvent e)
    {
        if (isOpen())
        {
            e.setCanceled(true);
        }
    }

    /** The server's refusals reach the player in the screen: its chat is hidden with the HUD. */
    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e)
    {
        GuiScreen s = Minecraft.getMinecraft().currentScreen;
        if (s instanceof GunsmithScreen && e.message != null && GunsmithContainer.FULL.equals(e.message.getUnformattedText()))
        {
            ((GunsmithScreen) s).showNotice(GunsmithContainer.FULL);
        }
    }
}
