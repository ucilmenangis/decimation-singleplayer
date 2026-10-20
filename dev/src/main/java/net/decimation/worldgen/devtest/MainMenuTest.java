package net.decimation.worldgen.devtest;

import java.util.List;

import net.decimation.fixes.Deci;
import net.decimation.worldgen.DevAutoTest;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Decimation's main menu as a player sees it after the start (fixes/MenuNews, PatchMenuBackground, docs/main_menu.md):
 * leaves the world, opens the home menu, waits until the welcome box and the loading fade are gone, then checks the
 * four news panels (GitHub, Updates, Website, Coming soon, in that order, each with a line and an address), saves
 * mainmenu_home, then waits for the background slideshow (fixes/MenuBackground: a new picture every 7 s) to show the next
 * picture and saves mainmenu_next. Ends at the home menu.
 */
public class MainMenuTest extends DevTestMode
{
    public String name() { return "mainmenu"; }

    private static final String[] TITLES = {"GitHub", "Updates", "Website", "Coming soon"};
    private int step, ticks;
    private int[] first;
    private IntegratedServer stopping;
    /** The news panels with a popup (Updates, Coming soon) and what each must show. */
    private static final int[] POPUPS = {1, 3};
    private static final String[][] POPUP_WANT = {{"Updates", "0", "updates"}, {"Coming soon", "2", "coming"}};
    private net.minecraft.client.gui.GuiScreen home;
    private int popup;

    public boolean client(Minecraft mc)
    {
        ticks++;
        switch (step)
        {
            case 0:
                DevAutoTest.keepScreen = true;
                if (mc.theWorld != null)
                {
                    stopping = mc.getIntegratedServer();
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld((net.minecraft.client.multiplayer.WorldClient) null);
                }
                mc.displayGuiScreen(Deci.homeMenu());
                step = 1;
                ticks = 0;
                return true;
            case 1:
                boolean saved = stopping == null || stopping.isServerStopped();
                if ((!saved || !Deci.menuShown()) && ticks < 20 * 60)
                {
                    return true; // the test world is saved, then the welcome box and the fade go away
                }
                DevTestResults.check(name(), "menu shown", Deci.menuShown(), Deci.menuShown(), "welcome box closed, fade over");
                DevTestResults.value(name(), "background picked this launch", Deci.menuBackground());
                List<String> panels = Deci.newsPanels(mc.currentScreen);
                for (String p : panels)
                {
                    DevTestResults.value(name(), "news panel", p);
                }
                StringBuilder titles = new StringBuilder();
                boolean filled = panels.size() == TITLES.length;
                for (int i = 0; i < panels.size(); i++)
                {
                    String[] parts = panels.get(i).split(" \\| ", -1);
                    titles.append(i == 0 ? "" : ", ").append(parts[0]);
                    // Updates and Coming soon open their detail popup (user, 21 Oktober 2026), GitHub and Website their page
                    filled &= parts.length == 3 && !parts[1].isEmpty()
                              && parts[2].startsWith(i == 0 || i == 2 ? "https://" : net.decimation.fixes.MenuDetails.ADDRESS);
                }
                boolean order = filled;
                for (int i = 0; order && i < TITLES.length; i++)
                {
                    order = panels.get(i).startsWith(TITLES[i] + " | ");
                }
                DevTestResults.check(name(), "news panels", titles, order, "GitHub, Updates, Website, Coming soon, each with a line and an address (popups for Updates and Coming soon)");
                // the Updates panel says what the release check did; a dev or test build never looks (fixes/Updater.offersUpdates)
                String updates = "";
                for (String p : panels)
                {
                    updates = p.startsWith("Updates | ") ? p.split(" \\| ", -1)[1] : updates;
                }
                boolean devBuild = !net.decimation.fixes.Updater.offersUpdates();
                DevTestResults.check(name(), "updates panel line", updates,
                                     devBuild ? updates.startsWith("Updates off: dev build") : !updates.startsWith("Updates off"),
                                     devBuild ? "Updates off: dev build (<branch>)" : "the release check's answer");
                // user, 17 Oktober 2026: links to our site, our version and name, Continue last world
                String website = "", github = "", cont = "", contTip = "";
                for (net.minecraft.client.gui.GuiButton b : DevTestUtil.buttons(mc.currentScreen))
                {
                    String l = DevTestUtil.label(b);
                    website = b.id == 12 ? l : website;
                    github = b.id == 15 ? l : github;
                    if (l.startsWith("Continue"))
                    {
                        cont = l;
                        contTip = String.valueOf(Deci.menuTooltip(b));
                    }
                }
                String[] links = Deci.menuLinks();
                DevTestResults.check(name(), "top right links", website + " " + links[0] + ", " + github + " " + links[1],
                                     "Website".equals(website) && "Discord".equals(github)
                                     && links[0].equals(net.decimation.fixes.MenuNews.SITE) && links[1].equals(net.decimation.fixes.MenuNews.DISCORD),
                                     "Website to the site, Discord to our invite (user, 21 Oktober 2026)");
                DevTestResults.check(name(), "popup GitHub line address", links[2], links[2].equals(net.decimation.fixes.MenuNews.REPO),
                                     "the repository (was BoehMod's forums)");
                String title = org.lwjgl.opengl.Display.getTitle();
                DevTestResults.check(name(), "window title", title, title.startsWith("DeciRemnant v") && title.endsWith("(Minecraft 1.7.10)"),
                                     "DeciRemnant v<our version> (Minecraft 1.7.10)");
                DevTestResults.check(name(), "continue button", cont, !cont.isEmpty(), "a Continue button (saves exist in the dev run folder)");
                // "Open world" read like an instruction (a world named "world"); tasks.md, 0.65.0
                DevTestResults.check(name(), "continue tooltip", contTip, contTip.startsWith("Last played: ")
                                     && contTip.length() > "Last played: ".length(), "Last played: <world name>");
                step = 2;
                ticks = 0;
                return true;
            case 2:
                if (ticks < 40)
                {
                    return true; // hover sounds and panel fade in
                }
                DevTestUtil.screenshot(mc, name(), "mainmenu_home");
                first = net.decimation.fixes.MenuBackground.shown();
                DevTestResults.value(name(), "pictures in the background show", first[0]);
                step = 3;
                ticks = 0;
                return true;
            case 3:
                int[] now = net.decimation.fixes.MenuBackground.shown();
                if (now[1] == first[1] && ticks < 20 * 10)
                {
                    return true; // the next picture comes after at most 7 s
                }
                boolean changed = now[1] != first[1] && first[1] >= 0;
                DevTestResults.check(name(), "background picture changes", "picture " + first[1] + " -> " + now[1] + " of "
                                     + now[0] + " after " + ticks + " ticks", changed && now[0] >= 2,
                                     "a new picture within 7 s, at least 2 pictures");
                step = 4;
                ticks = 0;
                return true;
            case 4:
                if (ticks < 40)
                {
                    return true; // the fade is over
                }
                DevTestUtil.screenshot(mc, name(), "mainmenu_next");
                // the corner art opens our Discord (fixes/MenuCorner): recorded, not opened in a browser
                System.setProperty("deciworldgen.update.nobrowser", "true");
                net.decimation.fixes.UpdatePrompt.reset();
                net.minecraft.client.gui.GuiButton corner = DevTestUtil.byId(mc.currentScreen, 9600);
                if (corner != null)
                {
                    DevTestUtil.click(mc.currentScreen, corner);
                }
                String page = net.decimation.fixes.UpdatePrompt.opened.isEmpty() ? "none" : net.decimation.fixes.UpdatePrompt.opened.get(0);
                DevTestResults.check(name(), "corner opens our Discord", (corner == null ? "no corner button, " : "") + page,
                                     corner != null && net.decimation.fixes.MenuNews.DISCORD.equals(page), net.decimation.fixes.MenuNews.DISCORD);
                System.clearProperty("deciworldgen.update.nobrowser");
                home = mc.currentScreen;
                popup = 0;
                step = 7;
                ticks = 0;
                return true;
            case 7:
                // each detail popup (docs/main_menu.md 2h): what a click on its panel does, its slides, arrows and Close
                if (ticks == 1)
                {
                    Deci.openNewsPanel(home, POPUPS[popup]);
                    return true;
                }
                if (ticks < 30)
                {
                    return true;                           // the picture loads, the popup settles
                }
                String[] want = POPUP_WANT[popup];
                net.minecraft.client.gui.GuiScreen open = mc.currentScreen;
                boolean isPopup = open instanceof net.decimation.fixes.MenuDetail;
                net.decimation.fixes.MenuDetails.Detail d = isPopup ? ((net.decimation.fixes.MenuDetail) open).detail() : null;
                String seen = d == null ? (open == null ? "none" : open.getClass().getName()) : d.title + ", " + d.slides.length + " slides";
                DevTestResults.check(name(), want[0] + " panel opens its popup", seen, (want[0] + ", " + want[1] + " slides").equals(seen),
                                     want[0] + ", " + want[1] + " slides");
                DevTestUtil.screenshot(mc, name(), "mainmenu_popup_" + want[2]);
                if (isPopup && d.slides.length > 1)
                {
                    net.decimation.fixes.MenuDetail m = (net.decimation.fixes.MenuDetail) open;
                    DevTestUtil.click(open, DevTestUtil.byId(open, 9502));             // the right arrow
                    DevTestResults.check(name(), want[0] + " arrow", "slide " + m.slide(), m.slide() == 1, "slide 1");
                }
                if (isPopup)
                {
                    DevTestUtil.click(open, DevTestUtil.byId(open, 9504));             // Close
                    DevTestResults.check(name(), want[0] + " Close", mc.currentScreen == home ? "the menu" : String.valueOf(mc.currentScreen),
                                         mc.currentScreen == home, "back on the menu");
                }
                next();
                return true;
            default:
                return false;
        }
    }

    /** The next popup, or the end: back on a fresh menu. */
    private void next()
    {
        ticks = 0;
        if (++popup < POPUPS.length)
        {
            step = 7;
            return;
        }
        mc().displayGuiScreen(Deci.homeMenu());
        DevAutoTest.keepScreen = false;
        step = 9;
    }

    private static Minecraft mc()
    {
        return Minecraft.getMinecraft();
    }
}
