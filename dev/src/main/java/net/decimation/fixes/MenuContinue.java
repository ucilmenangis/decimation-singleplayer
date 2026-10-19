package net.decimation.fixes;

import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * "Continue" on Decimation's main menu (user, 17 Oktober 2026: picked "Continue last world"): one of Decimation's own
 * menu buttons, right under Play, opening the world played last in one click; its tooltip names the world ("Last played:
 * <name>" since 0.65.0). The buttons under it move down one row. Only on the main menu and only when a world exists.
 * Client only (Forge bus).
 */
@SideOnly(Side.CLIENT)
public class MenuContinue
{
    /** Our button id: Decimation's menu buttons use 0 to 33. */
    public static final int ID = 950;
    /** Decimation's own buttons under Play: My Community, Server Status, Settings, Exit Game. */
    private static final int[] BELOW = {27, 11, 5, 0};

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Deci.isHomeMenu(e.gui) || mc.theWorld != null)
        {
            return;
        }
        SaveFormatComparator last = lastWorld(mc);
        if (last == null)
        {
            return;
        }
        List<?> buttons = e.buttonList;
        for (Object o : buttons)
        {
            GuiButton b = (GuiButton) o;
            for (int id : BELOW)
            {
                if (b.id == id)
                {
                    b.yPosition += 21;
                }
            }
        }
        // GuiMenuBase.addMenuButtons: the column starts at width / 2 - 173 + 1, Play's row at 86, rows 21 apart
        e.buttonList.add(Deci.menuButton(ID, e.gui.width / 2 - 173 + 1, 86 + 21, 100, 20, "Continue",
                                         "Last played: " + last.getDisplayName())); // "Open world" read like an order
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.ActionPerformedEvent.Pre e)
    {
        if (e.button.id != ID || !Deci.isHomeMenu(e.gui))
        {
            return;
        }
        e.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        SaveFormatComparator last = lastWorld(mc);
        if (last != null && mc.getSaveLoader().canLoadWorld(last.getFileName()))
        {
            mc.launchIntegratedServer(last.getFileName(), last.getDisplayName(), null);
        }
    }

    /** The world played last (the world list's own order: newest first), or null. */
    @SuppressWarnings("unchecked")
    static SaveFormatComparator lastWorld(Minecraft mc)
    {
        try
        {
            List<SaveFormatComparator> saves = mc.getSaveLoader().getSaveList();
            if (saves == null || saves.isEmpty())
            {
                return null;
            }
            Collections.sort(saves); // SaveFormatComparator sorts by last time played, newest first
            return saves.get(0);
        }
        catch (Exception unreadable)
        {
            return null;
        }
    }
}
