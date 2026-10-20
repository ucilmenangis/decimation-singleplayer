package net.decimation.fixes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * The news on Decimation's main menu (user, 17 Oktober 2026: "lets make the news panel now. 1. github repo, 2. update
 * release, 3. our website release, 4. Coming soon feature"; "just use component from decimation gui menu its self, no
 * need to add something new on it"). Decimation's home page keeps a 2 x 2 grid for its own news panels (ArticlePanel),
 * filled only from BoehMod's dead backend; this puts four of those same panels there: our GitHub repository, the release
 * check (fixes/Updater, the line follows its state), our website and the website's coming soon list. Hover, sound and
 * the click that opens the page are the panel's own. Pictures: textures/gui/menu/news_<id>.png in Decimation's assets,
 * written into the jars by tools/menu_assets.py (real shots of our world, never committed); without one a panel shows
 * Decimation's own placeholder. Main menu only (in a world the same screen is the pause menu). Client only: register
 * on both buses (screen event and client tick).
 */
@SideOnly(Side.CLIENT)
public class MenuNews
{
    public static final String REPO = "https://github.com/ucilmenangis/decimation-singleplayer";
    public static final String SITE = "https://deciremnant.vercel.app/";

    /** id, title, line (null: the release check's), address. */
    private static final String[][] PANELS = {
        {"github", "GitHub", "Source code, issues, all releases", REPO},
        {"update", "Updates", null, Updater.DEFAULT_WEB},
        {"website", "Website", "Download, install and server guides", SITE},
        {"coming", "Coming soon", "What we are building next", SITE + "#coming"},
    };
    private static final ResourceLocation PLACEHOLDER = new ResourceLocation("deci", "textures/gui/menu/loadingarticle.png");
    private static final Map<String, ResourceLocation> PICTURES = new HashMap<String, ResourceLocation>();

    /** The update panel of the open main menu: its line follows the release check. */
    private Object update;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e)
    {
        update = null;
        if (!Deci.isHomeMenu(e.gui) || Minecraft.getMinecraft().theWorld != null)
        {
            return;
        }
        // where GuiMenuHome.initGui puts its own panels: 104 right of the button column, two rows
        int x = e.gui.width / 2 - 173 + 104;
        for (int i = 0; i < PANELS.length; i++)
        {
            String[] p = PANELS[i];
            String key = "deciworldgen:news/" + p[0];
            Deci.newsPicture(key, picture(p[0])); // before the panel: a missing key would be downloaded as a URL
            boolean releases = "update".equals(p[0]);
            Object panel = Deci.addNewsPanel(e.gui, x + i % 2 * 123, i < 2 ? 47 : 120, p[1],
                                             releases ? updateLine() : p[2], releases ? updateUrl() : p[3], key);
            if (releases)
            {
                update = panel;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        Object panel = update;
        if (e.phase == TickEvent.Phase.END && panel != null)
        {
            Deci.newsLine(panel, updateLine());
        }
    }

    /** The release check in one short line (the panel draws it small, about 36 characters fit). */
    static String updateLine()
    {
        String line;
        switch (Updater.state())
        {
            case AVAILABLE:
            case DOWNLOADING:
            case READY:
                line = "New version " + Updater.latest() + " is out";
                break;
            case NONE:
                line = "Up to date: " + Updater.current().split("[-+]")[0]; // a dev build's 0.62.9-main.23+01b1201
                break;
            case FAILED:
                line = "Release check failed, see GitHub";
                break;
            case OFF:
                line = "Updates off: " + Updater.message();  // a dev or test build: "dev build (main)"
                break;
            default:
                line = "Checking GitHub releases";
        }
        return line.length() > 36 ? line.substring(0, 36) : line;
    }

    /** The newest release's page when the check knows it, else the release list. */
    static String updateUrl()
    {
        String v = Updater.latest();
        return v == null ? Updater.DEFAULT_WEB : Updater.DEFAULT_WEB + "/tag/" + v;
    }

    /** news_<id>.png from Decimation's menu textures when the jar has it, else Decimation's placeholder. */
    private static ResourceLocation picture(String id)
    {
        ResourceLocation found = PICTURES.get(id);
        if (found == null)
        {
            found = new ResourceLocation("deci", "textures/gui/menu/news_" + id + ".png");
            try
            {
                Minecraft.getMinecraft().getResourceManager().getResource(found);
            }
            catch (IOException missing)
            {
                found = PLACEHOLDER;
            }
            PICTURES.put(id, found);
        }
        return found;
    }
}
