package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * The corner art bottom right of Decimation's menu pages (donor_rank_0 / 1, written by tools/menu_assets.py: our soldier,
 * "JOIN THE REMNANT", our Discord) opens our Discord when clicked (user, 21 Oktober 2026; docs/main_menu.md 2h): a
 * button that draws nothing over the art's words (Decimation draws the art at the bottom right, 150 x 150).
 * Client only (Forge bus).
 */
public class MenuCorner
{
    static final int ID = 9600;

    /** A button Decimation's art shows through: it only takes the click. */
    static final class Hidden extends GuiButton
    {
        Hidden(int x, int y, int w, int h)
        {
            super(ID, x, y, w, h, "");
        }

        public void drawButton(Minecraft mc, int mx, int my)
        {
            field_146123_n = visible && mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        }
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e)
    {
        if (Deci.isMenuPage(e.gui))
        {
            // the art's words (JOIN THE REMNANT, the address): from 380 / 800 and 550 / 800 of the 150 x 150 square;
            // not the soldier, which small windows draw near the news panels
            e.buttonList.add(new Hidden(e.gui.width - 79, e.gui.height - 47, 79, 47));
        }
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.ActionPerformedEvent.Pre e)
    {
        if (e.button.id == ID && Deci.isMenuPage(e.gui))
        {
            e.setCanceled(true);
            UpdatePrompt.open(MenuNews.DISCORD);
        }
    }
}
