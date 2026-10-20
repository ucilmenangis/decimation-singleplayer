package net.decimation.fixes;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * A news panel's detail popup over Decimation's menu (what it shows: MenuDetails; user, 21 Oktober 2026; mockups in
 * docs/shots/main_menu_v0.65.0/mockups): the menu darkened behind, a black panel with the red top line of Decimation's
 * profile card, the title and its line, then a 16:9 picture carousel (torn arrow buttons, dots, the next slide every
 * 6 s) with each slide's heading and lines, or for Updates the release list. Decimation's own torn buttons: the red
 * action and Close. Esc, Close or a click outside the panel go back to the menu. Client only.
 */
public class MenuDetail extends GuiScreen
{
    static final int PREV = 9501, NEXT = 9502, ACTION = 9503, CLOSE = 9504;
    /** Decimation's red button (its question box's left button). */
    static final int RED = -44976;
    private static final long SLIDE_MS = 6000;
    /** Whether each picture is in the jars (asked once: a resource lookup opens a stream). */
    private static final java.util.Map<ResourceLocation, Boolean> HAVE = new java.util.HashMap<ResourceLocation, Boolean>();

    private final GuiScreen parent;
    private final MenuDetails.Detail detail;
    private int slide;
    private long since = System.currentTimeMillis();
    private int[] box;

    public MenuDetail(GuiScreen parent, MenuDetails.Detail detail)
    {
        this.parent = parent;
        this.detail = detail;
    }

    /**
     * Opens popup `id` over the open screen on the next tick; false for an id MenuDetails does not know. A news panel's
     * click arrives inside DeciScreen.mouseClicked's loop over the menu's panels: changing screens there let the menu
     * re-init and refill that list mid-loop (bug.md "Clicking a news popup crashed the game").
     */
    public static boolean open(String id)
    {
        final MenuDetails.Detail d = MenuDetails.get(id);
        if (d == null)
        {
            return false;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        final GuiScreen under = mc.currentScreen;
        mc.func_152344_a(new Runnable()
        {
            public void run()
            {
                if (mc.currentScreen == under)
                {
                    mc.displayGuiScreen(new MenuDetail(under, d));
                }
            }
        });
        return true;
    }

    public MenuDetails.Detail detail()
    {
        return detail;
    }

    public int slide()
    {
        return slide;
    }

    public void initGui()
    {
        if (parent != null && (parent.width != width || parent.height != height))
        {
            parent.setWorldAndResolution(mc, width, height); // a new window size: the menu behind follows it (never on open)
        }
        buttonList.clear();
        boolean pictures = detail.slides.length > 0;
        box = MenuDetails.layout(width, height, pictures);
        int x = box[0], y = box[1], w = box[2], h = box[3];
        String action = action();
        if (action != null)
        {
            buttonList.add(Deci.menuButton(ACTION, x + 10, y + h - 26, 120, 20, action, "", RED));
        }
        buttonList.add(Deci.menuButton(CLOSE, x + w - 90, y + h - 26, 80, 20, "Close", ""));
        if (pictures && detail.slides.length > 1)
        {
            int px = pictureX(), py = y + 26;
            buttonList.add(Deci.menuButton(PREV, px + 4, py + box[5] / 2 - 10, 18, 20, "<", ""));
            buttonList.add(Deci.menuButton(NEXT, px + box[4] - 22, py + box[5] / 2 - 10, 18, 20, ">", ""));
        }
    }

    /** The red button: the popup's own, or for Updates "Update to X" when one is known, else the release list. */
    private String action()
    {
        if (!MenuDetails.UPDATES.equals(detail.id))
        {
            return detail.action;
        }
        return Updater.state() == Updater.State.AVAILABLE ? "Update to " + Updater.latest() : "All releases";
    }

    private int pictureX()
    {
        return box[0] + (box[2] - box[4]) / 2;
    }

    protected void actionPerformed(GuiButton b)
    {
        switch (b.id)
        {
            case PREV:
            case NEXT:
                slide = MenuDetails.step(slide, detail.slides.length, b.id == NEXT ? 1 : -1);
                since = System.currentTimeMillis();
                break;
            case ACTION:
                act();
                break;
            default:
                mc.displayGuiScreen(parent);
        }
    }

    private void act()
    {
        if (MenuDetails.UPDATES.equals(detail.id))
        {
            if (Updater.state() == Updater.State.AVAILABLE)
            {
                mc.displayGuiScreen(Deci.updatesPage()); // UpdateProgress: the download on Decimation's progress screen
            }
            else
            {
                UpdatePrompt.open(Updater.DEFAULT_WEB);
            }
        }
    }

    protected void keyTyped(char c, int key)
    {
        if (key == 1)
        {
            mc.displayGuiScreen(parent);                  // Escape
        }
    }

    protected void mouseClicked(int mx, int my, int button)
    {
        super.mouseClicked(mx, my, button);
        if (box != null && mc.currentScreen == this && (mx < box[0] || mx > box[0] + box[2] || my < box[1] || my > box[1] + box[3]))
        {
            mc.displayGuiScreen(parent);                  // a click outside the panel, as Decimation's question box does
        }
    }

    public void updateScreen()
    {
        if (detail.slides.length > 1 && System.currentTimeMillis() - since > SLIDE_MS)
        {
            slide = MenuDetails.step(slide, detail.slides.length, 1);
            since = System.currentTimeMillis();
        }
    }

    public void drawScreen(int mx, int my, float partial)
    {
        if (parent != null)
        {
            parent.drawScreen(-1, -1, partial);           // the menu behind, without hover effects
        }
        drawRect(0, 0, width, height, 0x96000000);
        int x = box[0], y = box[1], w = box[2], h = box[3];
        drawRect(x, y, x + w, y + h, 0xEE080808);
        drawRect(x, y, x + w, y + 2, 0xFFE24848);
        fontRendererObj.drawStringWithShadow(detail.title, x + 10, y + 6, 0xFFFFFF);
        String line = MenuDetails.UPDATES.equals(detail.id) ? MenuNews.updateLine() : detail.line;
        small(line, x + 10, y + 17, 0xAAAAAA);
        if (detail.slides.length > 0)
        {
            slides(x, y, w);
        }
        else
        {
            releases(x, y);
        }
        super.drawScreen(mx, my, partial);
    }

    private void slides(int x, int y, int w)
    {
        MenuDetails.Slide s = detail.slides[slide];
        int px = pictureX(), py = y + 26, pw = box[4], ph = box[5];
        drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF303030);
        ResourceLocation picture = new ResourceLocation("deci", "textures/gui/menu/" + s.picture + ".png");
        if (exists(picture))
        {
            mc.getTextureManager().bindTexture(picture);
            GL11.glColor4f(1, 1, 1, 1);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(px, py + ph, zLevel, 0, 1);
            t.addVertexWithUV(px + pw, py + ph, zLevel, 1, 1);
            t.addVertexWithUV(px + pw, py, zLevel, 1, 0);
            t.addVertexWithUV(px, py, zLevel, 0, 0);
            t.draw();
        }
        else
        {
            drawRect(px, py, px + pw, py + ph, 0xFF151515); // tools/menu_assets.py has not written the pictures
            drawCenteredString(fontRendererObj, s.heading, px + pw / 2, py + ph / 2 - 4, 0x666666);
        }
        int n = detail.slides.length, dy = py + ph + 4;
        for (int i = 0; n > 1 && i < n; i++)
        {
            int dx = x + w / 2 - n * 5 + i * 10;
            drawRect(dx, dy, dx + 5, dy + 5, i == slide ? 0xFFE24848 : 0xFF5A5A5A);
        }
        fontRendererObj.drawStringWithShadow(s.heading, x + 10, dy + 10, 0xFFFFFF);
        for (int i = 0; i < s.lines.length; i++)
        {
            small(s.lines[i], x + 10, dy + 21 + i * 6, 0xBBBBBB);
        }
    }

    /** Updates: the newest releases, version and date, then the first line of their notes. */
    private void releases(int x, int y)
    {
        List<String[]> list = Updater.releases();
        int ly = y + 28;
        if (list.isEmpty())
        {
            small(Updater.offersUpdates() ? "No release list yet: see All releases" : "This build does not check for updates",
                  x + 10, ly, 0xBBBBBB);
            return;
        }
        for (int i = 0; i < list.size() && i < 5; i++)
        {
            String[] r = list.get(i);
            String date = MenuDetails.date(r[1]);
            fontRendererObj.drawStringWithShadow(r[0] + (date.isEmpty() ? "" : "  " + date), x + 10, ly, i == 0 ? 0xFFE24848 : 0xFFFFFF);
            small(MenuDetails.summary(r[2], MenuDetails.LINE), x + 10, ly + 10, 0xBBBBBB);
            ly += 21;
        }
    }

    /** Text at half size, as Decimation's menu draws its small lines. */
    private void small(String s, int x, int y, int colour)
    {
        GL11.glPushMatrix();
        GL11.glScalef(0.5f, 0.5f, 1);
        fontRendererObj.drawStringWithShadow(s, x * 2, y * 2, colour);
        GL11.glPopMatrix();
    }

    private boolean exists(ResourceLocation r)
    {
        Boolean have = HAVE.get(r);
        if (have == null)
        {
            try
            {
                mc.getResourceManager().getResource(r).getInputStream().close();
                have = true;
            }
            catch (Exception e)
            {
                have = false;
            }
            HAVE.put(r, have);
        }
        return have;
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
