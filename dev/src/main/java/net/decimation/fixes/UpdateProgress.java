package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * The update runs on Decimation's OWN progress screen (user, 17 Oktober 2026: "why not using from decimation instead? as i
 * know decimation has it owns update loading screen"): GuiUpdateProgress deci.i.m (BoehMod banner, "DO NOT CLOSE YOUR
 * GAME!" / "SAFE TO CLOSE GAME!", file name and MB, green bar). Decimation's "Update" button opens its update page
 * (GuiMenuUpdates deci.i.l, a release list from BoehMod's dead server); this hook opens the progress screen instead and
 * starts our download (Updater). Each client tick copies our download into the fields that screen draws
 * (UpdateChecker, Deci.setUpdateProgress). When the jar is checked, "Quit game" (the swap runs as the game closes) and
 * "Later" (it runs whenever the game closes) appear under the bar; that screen draws its black layer over its own
 * buttons, so ours are drawn again after it (DrawScreenEvent.Post). A failed download, or a check that has not found a
 * release, shows our UpdateScreen (its message, Try again / Back). Client only.
 */
public class UpdateProgress
{
    private static final int QUIT = 9101, LATER = 9102;
    /** Our two buttons and the screen they were added to (a new screen or a resize makes new ones). */
    private static GuiButton quit, later;
    private static GuiScreen shownOn;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e)
    {
        if (e.gui == null || !"deci.i.l".equals(e.gui.getClass().getName()))
        {
            return;
        }
        Updater.State s = Updater.state();
        if (s == Updater.State.AVAILABLE)
        {
            Updater.download();
            s = Updater.state();
        }
        e.gui = s == Updater.State.DOWNLOADING || s == Updater.State.READY ? Deci.updateProgressScreen() : new UpdateScreen();
        mirror();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!Deci.isUpdateProgress(mc.currentScreen))
        {
            return;
        }
        mirror();
        Updater.State s = Updater.state();
        if (s == Updater.State.FAILED || s == Updater.State.NONE || s == Updater.State.AVAILABLE)
        {
            mc.displayGuiScreen(new UpdateScreen());           // the reason and Try again
            return;
        }
        if (s == Updater.State.READY && shownOn != mc.currentScreen)
        {
            // initGui again: Forge's InitGuiEvent.Post (onInit) hands over the button list
            mc.currentScreen.setWorldAndResolution(mc, mc.currentScreen.width, mc.currentScreen.height);
        }
    }

    /** Decimation's screen sizes itself on a resize (initGui clears the buttons): put ours back. */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e)
    {
        if (Deci.isUpdateProgress(e.gui) && Updater.state() == Updater.State.READY)
        {
            int y = e.gui.height / 2 + 92;
            quit = new GuiButton(QUIT, e.gui.width / 2 - 102, y, 100, 20, "Quit game");
            later = new GuiButton(LATER, e.gui.width / 2 + 2, y, 100, 20, "Later");
            e.buttonList.add(quit);
            e.buttonList.add(later);
            shownOn = e.gui;
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post e)
    {
        if (e.gui != shownOn || !Deci.isUpdateProgress(e.gui))
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        quit.drawButton(mc, e.mouseX, e.mouseY);
        later.drawButton(mc, e.mouseX, e.mouseY);
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre e)
    {
        if (!Deci.isUpdateProgress(e.gui))
        {
            return;
        }
        if (e.button.id == QUIT)
        {
            e.setCanceled(true);
            SafeQuit.quit();                              // from a world too (Esc menu): leave it and let it save first
        }
        else if (e.button.id == LATER)
        {
            e.setCanceled(true);
            Minecraft.getMinecraft().displayGuiScreen(Deci.homeMenu());
        }
    }

    /** Our download as Decimation's screen shows it: version and size, percent, done of total MB (one decimal). */
    static void mirror()
    {
        Updater.State s = Updater.state();
        long size = Updater.size(), done = s == Updater.State.READY ? size : Updater.downloaded();
        int percent = s == Updater.State.READY ? 100 : size <= 0 ? 0 : (int) Math.min(100, done * 100 / size);
        // as Decimation's own updater filled them: the status line "Downloading: 33%", then "Complete!"; the file line
        // names the version and its size; the size line reads done / total (the screen prints "total field / downloaded
        // field", so they go in swapped)
        String status = s == Updater.State.READY ? net.minecraft.client.resources.I18n.format("menu.updating.complete") : percent + "%";
        String file = "DeciRemnant" + (Updater.latest() == null ? "" : " " + Updater.latest());
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        int width = new net.minecraft.client.gui.ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
        Deci.setUpdateProgress(status, file, UpdateBar.field(percent, width), s == Updater.State.DOWNLOADING, mb(size), mb(done));
    }

    private static double mb(long bytes)
    {
        return Math.round(bytes / 104857.6) / 10.0;
    }
}
