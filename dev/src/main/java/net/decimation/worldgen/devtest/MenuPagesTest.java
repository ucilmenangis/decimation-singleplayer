package net.decimation.worldgen.devtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.decimation.fixes.Deci;
import net.decimation.worldgen.DevAutoTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;

/**
 * Decimation's menu pages next to the home menu, as a player meets them (user, 17 Oktober 2026; docs/main_menu.md):
 * the My Community popup (My Supporter, Website, Forums, GitHub; My Supporter tells a player without it that it is for
 * supporters: user, 20 Oktober 2026, "rewind it to supporter", it is for people who donate), the official
 * server list (Bukovka and Chernarus, both coming soon, players online coming soon, eight pictures per map), the
 * Community tab (a toast: community server coming soon) and the server status page (Global Server, Community Server and
 * Download offline). Texts no check can click (the profile card, the lounge title) are checked in the loaded classes.
 * Leaves the world like mainmenu: run it last. Shots menupages_community, menupages_official, menupages_status.
 */
public class MenuPagesTest extends DevTestMode
{
    public String name() { return "menupages"; }

    private static final List<String> POPUP = Arrays.asList(
            "★ My Supporter | Personalize your Supporter settings!",
            "Website | Download, install and server guides",
            "Forums | Our Official BoehMod Forums!",
            "GitHub | Source code, issues, all releases");
    private static final List<String> ROWS = Arrays.asList("Bukovka | offline | bukovka", "Chernarus | offline | chernarus");
    private static final String[] STATUS = {
            "Global Server | Official DeciRemnant server | Offline",
            "Community Server | Servers run by players | Offline",
            "Download | DeciRemnant download server | Offline"};
    private static final int MY_COMMUNITY = 27, PLAY = 9, COMMUNITY_TAB = 13, SERVER_STATUS = 11;

    private int step, ticks;
    private IntegratedServer stopping;

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
                next();
                return true;
            case 1:
                boolean saved = stopping == null || stopping.isServerStopped();
                if ((!saved || !Deci.menuShown()) && ticks < 20 * 60)
                {
                    return true; // the test world is saved, then the welcome box and the fade go away
                }
                DevTestUtil.press(mc.currentScreen, DevTestUtil.byId(mc.currentScreen, MY_COMMUNITY));
                next();
                return true;
            case 2:
                if (ticks < 10)
                {
                    return true;
                }
                List<String> lines = Deci.popupLines(mc.currentScreen);
                DevTestResults.check(name(), "my community popup", lines, lines.equals(POPUP),
                                     "My Supporter, Website, Forums, GitHub with our lines");
                Deci.clickPopupLine(mc.currentScreen, 1);
                next();
                return true;
            case 3:
                if (ticks < 20)
                {
                    return true; // the toast slides in
                }
                List<String> toasts = Deci.toasts();
                DevTestResults.check(name(), "supporter click", toasts, toasts.contains("You must be a ★ Supporter to access this!"),
                                     "a toast: You must be a ★ Supporter to access this!");
                List<String> titles = Deci.toastTitles();
                DevTestResults.check(name(), "toast title", titles, !titles.isEmpty() && titles.get(titles.size() - 1).equals("DeciRemnant"),
                                     "DeciRemnant (was Decimation)");
                DevTestUtil.screenshot(mc, name(), "menupages_community");
                mc.displayGuiScreen(Deci.homeMenu());
                next();
                return true;
            case 4:
                if (DevTestUtil.byId(mc.currentScreen, PLAY) == null && ticks < 20 * 10)
                {
                    return true;
                }
                DevTestUtil.press(mc.currentScreen, DevTestUtil.byId(mc.currentScreen, PLAY));
                next();
                return true;
            case 5:
                List<String> rows = Deci.officialRows(mc.currentScreen);
                boolean pinging = rows.isEmpty();
                for (String r : rows)
                {
                    pinging |= r.contains("| pinging |");
                }
                if (pinging && ticks < 20 * 15)
                {
                    return true; // Play opens the list once the rows are made, then each row pings its host
                }
                DevTestResults.check(name(), "official rows", rows, rows.equals(ROWS), "Bukovka and Chernarus, both not reachable (drawn: Coming soon)");
                String players = I18n.format("menu.stats.playercount.unknown");
                DevTestResults.check(name(), "players online", players, "Coming soon".equals(players), "Players Online: Coming soon");
                List<String> missing = new ArrayList<String>();
                for (String map : new String[] {"bukovka", "chernarus"})
                {
                    for (int i = 0; i < 8; i++)
                    {
                        ResourceLocation picture = new ResourceLocation("deci", "textures/gui/menu/serverbackgrounds/" + map + i + ".png");
                        mc.getTextureManager().bindTexture(picture);
                        if (mc.getTextureManager().getTexture(picture) == TextureUtil.missingTexture)
                        {
                            missing.add(map + i);
                        }
                    }
                }
                DevTestResults.check(name(), "map pictures", missing.isEmpty() ? "16 of 16 load" : "missing " + missing, missing.isEmpty(),
                                     "bukovka0..7 and chernarus0..7 load as textures");
                String tip = Deci.menuTooltip(DevTestUtil.byId(mc.currentScreen, COMMUNITY_TAB));
                DevTestResults.check(name(), "community tab tooltip", tip, "Coming soon".equals(tip), "Coming soon");
                DevTestUtil.press(mc.currentScreen, DevTestUtil.byId(mc.currentScreen, COMMUNITY_TAB));
                next();
                return true;
            case 6:
                Deci.showRowPicture(mc.currentScreen, 1, 0); // Chernarus' first picture behind the page, as a hover shows it
                if (ticks < 30)
                {
                    return true;
                }
                List<String> shown = Deci.toasts();
                DevTestResults.check(name(), "community tab click", shown, shown.contains("Community server coming soon"),
                                     "a toast: Community server coming soon");
                DevTestUtil.screenshot(mc, name(), "menupages_official");
                DevTestUtil.press(mc.currentScreen, DevTestUtil.byId(mc.currentScreen, SERVER_STATUS));
                next();
                return true;
            case 7:
                List<String> status = Deci.statusRows(mc.currentScreen);
                boolean done = status.size() == 4;
                for (int i = 0; done && i < 4; i++)   // the website row answers over the network
                {
                    done = !status.get(i).endsWith("| Pinging...");
                }
                if (!done && ticks < 20 * 20)
                {
                    return true; // each row pings its host on its own thread (a host that answers nothing: about 75 s)
                }
                boolean rowsOk = status.size() == 4;
                for (int i = 0; rowsOk && i < 3; i++)
                {
                    rowsOk = status.get(i).equals(STATUS[i]);
                }
                DevTestResults.check(name(), "status rows", status, rowsOk, "Global Server, Community Server, Download: Offline");
                String site = status.size() == 4 ? status.get(3) : "";
                DevTestResults.check(name(), "website row", site, site.equals("Website | Official DeciRemnant Website | Online"),
                                     "our website, Online");
                next();
                return true;
            case 8:
                if (ticks < 40)
                {
                    return true; // the rows settle on screen
                }
                DevTestUtil.screenshot(mc, name(), "menupages_status");
                List<String> wrong = new ArrayList<String>();
                has(wrong, "deci/d/p", "Coming soon", "Not Reachable");      // ListElement: a row's state
                has(wrong, "deci/i/i", "Official DeciRemnant Server", "Official Decimation Server");
                has(wrong, "deci/i/a", "Not Supporter :(", "No early access"); // GuiMenuBase: the profile card
                has(wrong, "deci/i/e", "Is not a Supporter :(", "No early access"); // GuiMenuHome: a viewed profile
                has(wrong, "deci/i/k", "Supporters Lounge", "Early access lounge");
                has(wrong, "deci/d/k", "My Supporter", "Early access");
                has(wrong, "deci/i/h", "Global Server", "mcdecimation");
                DevTestResults.check(name(), "texts in the loaded classes", wrong.isEmpty() ? "all ours" : wrong, wrong.isEmpty(),
                                     "our texts in, Decimation's old ones out");
                DevAutoTest.keepScreen = false;
                step = 9;
                return false;
            default:
                return false;
        }
    }

    private void next()
    {
        step++;
        ticks = 0;
    }

    private static void has(List<String> wrong, String cls, String in, String out)
    {
        if (!DevTestUtil.classHas(cls, in))
        {
            wrong.add(cls + " lacks \"" + in + "\"");
        }
        if (DevTestUtil.classHas(cls, out))
        {
            wrong.add(cls + " still has \"" + out + "\"");
        }
    }
}
