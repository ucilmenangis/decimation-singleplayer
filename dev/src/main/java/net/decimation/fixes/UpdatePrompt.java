package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * Updates are the player's choice (user, 20 Oktober 2026: "make option when there is update so user can do update or not
 * at that time so they dont force user to update their version, if (worse case) the updater is doesnt support on some
 * client, there is link to download new jar"; docs/main_menu.md 2g). Once a newer release is known, Decimation's home
 * menu asks once a launch with its own question box (ConfirmScreen deci.d.s), on the title menu or, if the player is
 * already in a world, on the Esc menu (user, 21 Oktober 2026: "yes"): Update opens Decimation's progress screen
 * (UpdateProgress; its Quit game leaves a world safely, SafeQuit), Later leaves the menu as it is, Play or Resume Game
 * included. A game whose jar the updater cannot replace
 * (Updater.canSwap) is offered the release page instead. Afterwards the update stays one click away: Settings, View
 * Updates (Decimation's own button) and the Updates news panel (MenuNews). Client only (both buses).
 */
public class UpdatePrompt
{
    /** The box's kind (Decimation passes it only to a parent that is a panel callback; the home menu is not). */
    static final int KIND = 9400;
    /** ConfirmScreen's own button ids: left (red) and right. */
    static final int LEFT = 10, RIGHT = 11;

    /** Release pages opened (dev tests read them; -Ddeciworldgen.update.nobrowser records without opening). */
    public static final List<String> opened = new ArrayList<String>();
    private static String asked;
    private static GuiScreen box;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String latest = Updater.latest();
        if (Updater.state() != Updater.State.AVAILABLE || latest == null || latest.equals(asked)
            || !Deci.isHomeMenu(mc.currentScreen) || !Deci.menuShown())
        {
            return;
        }
        asked = latest;                                   // once a launch: Later means not now
        boolean swap = Updater.canSwap();
        String[] labels = labels(swap);
        box = Deci.confirm(KIND, mc.currentScreen, lines(latest, Updater.size(), swap), labels[0], labels[1]);
        mc.displayGuiScreen(box);
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.ActionPerformedEvent.Pre e)
    {
        if (box == null || e.gui != box)
        {
            return;
        }
        box = null;
        if (e.button.id != RIGHT)
        {
            return;                                       // Later: the box goes back to the menu itself
        }
        if (Updater.canSwap())
        {
            e.setCanceled(true);
            Minecraft.getMinecraft().displayGuiScreen(Deci.updatesPage()); // UpdateProgress starts the download there
        }
        else
        {
            open(MenuNews.updateUrl());                  // the box then goes back to the menu
        }
    }

    /** The box's lines: what is out and what the player can do (Decimation's box is 200 GUI pixels wide). */
    static String[] lines(String latest, long size, boolean swap)
    {
        String title = "§aDeciRemnant " + latest + " is out";
        if (!swap)
        {
            return new String[] {title, "§7Download it from GitHub and", "§7put it in your mods folder"};
        }
        String mb = size > 0 ? (size + (1 << 19)) / (1 << 20) + " MB. " : "";
        return new String[] {title, "§7" + mb + "Update now?", "§7Later: Settings, View Updates"};
    }

    /** Left (Decimation draws it red) and right button. */
    static String[] labels(boolean swap)
    {
        return new String[] {"Later", swap ? "Update" : "Download"};
    }

    /** UpdateScreen's buttons in each state, left to right (here: that screen is a GuiScreen, which tests cannot load). */
    static String[] screenLabels(Updater.State s)
    {
        switch (s)
        {
            case READY:
                return new String[] {"Quit game", "Later"};
            case FAILED:
                return new String[] {"Try again", "Download page", "Back"};
            case DOWNLOADING:
                return new String[0];
            default:
                return new String[] {"Back"};
        }
    }

    /** Opens a page in the browser as Decimation's own links do (DeciScreen.openUri: java.awt.Desktop). */
    public static void open(String url)
    {
        opened.add(url);
        if (Boolean.getBoolean("deciworldgen.update.nobrowser"))
        {
            return;
        }
        try
        {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        }
        catch (Exception ex)
        {
            FMLLog.warning("[deciworldgen] could not open %s: %s", url, ex);
        }
    }

    /** Forget the question and the opened pages (dev tests). */
    public static void reset()
    {
        asked = null;
        box = null;
        opened.clear();
    }
}
