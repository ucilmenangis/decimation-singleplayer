package net.decimation.fixes.gunsmith;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The Gunsmith screen (docs/superpowers/specs/2026-10-21-gunsmith-screen-design.md, mockup A "Tarkov lines"): the held
 * gun's own 3D model drawn large by Decimation's renderer, each of the six slots in a box near its part with a line to
 * it, a click on a box opens the list of matching parts the player carries. The gun's silhouette (the depth buffer of one
 * draw) is measured on opening and on each change only (updateScreen), never per frame; the lists are built on opening
 * and when the inventory changes. Fits, swaps and take offs go to the server as button ids (GunsmithAction).
 */
public class GunsmithScreen extends GuiContainer
{
    private static final String ATTACH_KEY = "key.decimation.gui.attach";
    private static final int RED = 0xFFE24848, GREY = 0xFF4A4A4A, LIGHT = 0xFF8A8A8A, BOX_BG = 0xE0101010, TEXT = 0xFFF0F0F0,
                             MUTED = 0xFF9A9A9A, DIM = 0xFF6A6A6A;
    /**
     * Turns that show the gun side on with the muzzle to the right (dev test gunsmith checks the side): Decimation's
     * dropped item draw lays the gun flat (90 degrees about x), TURN_X stands it up again (seen in the dev test's shots:
     * with a half turn about z as well it stood upside down with the muzzle to the left).
     */
    private static final float TURN_Z = 0, TURN_X = -90, TURN_Y = 0;
    final GunsmithContainer container;
    /** How often the silhouette was measured and a list built (dev test gunsmith: once per open and per change). */
    public int measures, listBuilds;
    private int attachKey = -1;
    private int[] area;
    private boolean remeasure = true;
    private float scale, gx, gy;
    private int[] bounds;
    private final int[][] points = new int[GunSlot.values().length][], boxes = new int[GunSlot.values().length][];
    private final String[] labels = new String[GunSlot.values().length];
    private final boolean[] builtIn = new boolean[GunSlot.values().length];
    private String partsSig = "", title = "";
    private int invSig;
    private GunSlot open;
    private final List<Integer> entries = new ArrayList<Integer>();
    private boolean takeOffRow;
    private int scroll;
    private FloatBuffer depth;
    private boolean[] mask;
    private EntityItem holder;
    private static final FloatBuffer AMBIENT = (FloatBuffer) BufferUtils.createFloatBuffer(4).put(new float[] {0.7f, 0.7f, 0.7f, 1})
                                                                    .flip();

    public GunsmithScreen(GunsmithContainer container)
    {
        super(container);
        this.container = container;
    }

    // ---- layout, read by the dev test gunsmith

    public int[] box(GunSlot s)
    {
        return boxes[s.ordinal()];
    }

    public int[] point(GunSlot s)
    {
        return points[s.ordinal()];
    }

    public int[] gunBounds()
    {
        return bounds;
    }

    public GunSlot openList()
    {
        return open;
    }

    public String boxLabel(GunSlot s)
    {
        return labels[s.ordinal()];
    }

    /** The open list's title, then its rows' texts as drawn. */
    public List<String> listLines()
    {
        if (open == null)
        {
            return Collections.emptyList();
        }
        List<String> l = new ArrayList<String>();
        l.add(open.listTitle());
        for (int i = 0; i < rows(); i++)
        {
            l.add(rowText(i));
        }
        return l;
    }

    /** Top left of row i of the open list (0 = the first row under the title), or null. */
    public int[] listRow(int i)
    {
        if (open == null || boxes[open.ordinal()] == null || i < 0 || i >= Math.min(rows(), GunsmithLayout.MAX_ROWS))
        {
            return null;
        }
        int[] l = GunsmithLayout.list(width, height, boxes[open.ordinal()], rows());
        return new int[] {l[0], l[1] + 14 + i * GunsmithLayout.ROW};
    }

    // ---- life cycle

    @Override
    public void initGui()
    {
        xSize = width;
        ySize = height;
        super.initGui();
        area = GunsmithLayout.gunArea(width, height);
        remeasure = true;
        int left = width / 2 - 9 * 20 / 2;
        for (int i = 0; i < 36; i++)
        {
            Slot s = (Slot) container.inventorySlots.get(GunsmithContainer.HOTBAR + i);
            s.xDisplayPosition = i < 9 ? left + i * 20 + 2 : -2000;
            s.yDisplayPosition = i < 9 ? height - 24 : -2000;
        }
        refresh();
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        String sig = signature(container.gun()) + slotsSignature();
        if (!sig.equals(partsSig))
        {
            partsSig = sig;
            remeasure = true;
            refresh();
        }
        int inv = inventorySignature();
        if (inv != invSig)
        {
            invSig = inv;
            if (open != null)
            {
                fill(open);
            }
        }
    }

    /** Labels, built in slots and the title: on opening and on a change of the gun, never per frame. */
    private void refresh()
    {
        ItemStack gun = container.gun();
        title = gun == null ? "" : gun.getDisplayName();
        partsSig = signature(gun) + slotsSignature();
        for (GunSlot s : GunSlot.values())
        {
            builtIn[s.ordinal()] = GunParts.builtIn(s, gun);
            ItemStack part = ((Slot) container.inventorySlots.get(s.ordinal())).getStack();
            labels[s.ordinal()] = builtIn[s.ordinal()] ? "Built in" : part != null ? fit(part.getDisplayName(), 52) : s.label;
        }
    }

    private static String signature(ItemStack gun)
    {
        if (gun == null || !gun.hasTagCompound())
        {
            return gun == null ? "" : String.valueOf(System.identityHashCode(gun.getItem()));
        }
        StringBuilder b = new StringBuilder(String.valueOf(Item.getIdFromItem(gun.getItem())));
        for (GunSlot s : GunSlot.values())
        {
            b.append('|').append(gun.getTagCompound().getTag(s.nbtKey));
        }
        return b.toString();
    }

    /** What the part slots hold: they fill from the server a moment after opening. */
    private String slotsSignature()
    {
        StringBuilder b = new StringBuilder();
        for (GunSlot s : GunSlot.values())
        {
            ItemStack st = ((Slot) container.inventorySlots.get(s.ordinal())).getStack();
            b.append(',').append(st == null ? 0 : Item.getIdFromItem(st.getItem()));
        }
        return b.toString();
    }

    private int inventorySignature()
    {
        int h = 1;
        for (int i = 0; i < 36; i++)
        {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            h = 31 * h + (s == null ? 0 : Item.getIdFromItem(s.getItem()) * 97 + s.stackSize);
        }
        return h;
    }

    // ---- drawing

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY)
    {
        ItemStack gun = container.gun();
        if (gun == null)
        {
            return;
        }
        drawRect(0, 0, width, height, 0x90000000); // the world dimmed further than a chest's screen: the gun stands out
        if (remeasure)
        {
            measure(gun);
        }
        if (bounds != null)
        {
            // a soft band of light behind the gun, fading out above and below it (the alpha test would cut the faint edges)
            int mid = bounds[1] + bounds[3] / 2, reach = bounds[3] / 2 + 50;
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            drawGradientRect(0, mid - reach, width, mid, 0x00FFFFFF, 0x34FFFFFF);
            drawGradientRect(0, mid, width, mid + reach, 0x34FFFFFF, 0x00FFFFFF);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            drawGun(gun, gx, gy, scale);
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawLines();
        for (GunSlot s : GunSlot.values())
        {
            drawBox(s);
        }
        for (int i = 0; i < 9; i++)
        {
            Slot s = (Slot) container.inventorySlots.get(GunsmithContainer.HOTBAR + i);
            int x = s.xDisplayPosition, y = s.yDisplayPosition;
            drawRect(x - 1, y - 1, x + 17, y + 17, i == container.heldSlot ? RED : GREY);
            drawRect(x, y, x + 16, y + 16, BOX_BG);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        drawCenteredString(fontRendererObj, title, width / 2, 10, TEXT);
        small("Esc  Close", width - 6 - fontRendererObj.getStringWidth("Esc  Close") / 2, 10, MUTED);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial)
    {
        super.drawScreen(mouseX, mouseY, partial);
        if (open != null)
        {
            drawList(mouseX, mouseY);
        }
    }

    /** The gun through Decimation's own renderer (its dropped item draw), side on, at s GUI units per model unit. */
    private void drawGun(ItemStack gun, float x, float y, float s)
    {
        IItemRenderer r = MinecraftForgeClient.getItemRenderer(gun, IItemRenderer.ItemRenderType.ENTITY);
        if (r == null || mc.theWorld == null)
        {
            return;
        }
        if (holder == null || holder.worldObj != mc.theWorld)
        {
            holder = new EntityItem(mc.theWorld, 0, 0, 0, gun);
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 150);
        GL11.glScalef(s, s, s);
        RenderHelper.enableGUIStandardItemLighting(); // light from the front, fixed to the screen, before the gun turns
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, AMBIENT); // Decimation's guns are near black: more light than an item
        GL11.glRotatef(TURN_Z, 0, 0, 1);
        GL11.glRotatef(TURN_Y, 0, 1, 0);
        GL11.glRotatef(TURN_X, 1, 0, 0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1, 1, 1, 1);
        r.renderItem(IItemRenderer.ItemRenderType.ENTITY, gun, RenderBlocks.getInstance(), holder);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    /**
     * The gun's silhouette: drawn alone into the depth buffer (colour writes off), read back once, measured by
     * Silhouette; the probe scale halves while the gun is cut by the window and doubles while it is small. Then the gun
     * is fitted into the area, the line ends and the boxes follow. On opening and on a change only.
     */
    private void measure(ItemStack gun)
    {
        remeasure = false;
        measures++;
        int f = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor(), w = mc.displayWidth,
            h = mc.displayHeight;
        float ox = area[0] + area[2] / 2f, oy = area[1] + area[3] / 2f, s0 = area[3] / 2f;
        Silhouette sil = null;
        for (int tries = 0; tries < 5; tries++)
        {
            sil = probe(gun, ox, oy, s0, w, h);
            if (sil == null)
            {
                break;
            }
            boolean cut = sil.minX <= 0 || sil.minY <= 0 || sil.maxX >= w - 1 || sil.maxY >= h - 1;
            if (cut)
            {
                s0 /= 2;
            }
            else if (sil.maxX - sil.minX < 200 && tries < 4)
            {
                s0 *= 2;
            }
            else
            {
                break;
            }
        }
        bounds = null;
        if (sil == null)
        {
            return;
        }
        float minX = sil.minX / (float) f, maxX = (sil.maxX + 1) / (float) f, minY = sil.minY / (float) f,
              maxY = (sil.maxY + 1) / (float) f;
        float k = Math.min(area[2] * 0.9f / (maxX - minX), area[3] * 0.8f / (maxY - minY));
        scale = s0 * k;
        gx = area[0] + area[2] / 2f - ((minX + maxX) / 2 - ox) * k;
        gy = area[1] + area[3] / 2f - ((minY + maxY) / 2 - oy) * k;
        bounds = new int[] {Math.round(gx + (minX - ox) * k), Math.round(gy + (minY - oy) * k), Math.round((maxX - minX) * k),
                            Math.round((maxY - minY) * k)};
        for (GunSlot s : GunSlot.values())
        {
            int[] p = sil.points[s.ordinal()];
            points[s.ordinal()] = new int[] {Math.round(gx + (p[0] / (float) f - ox) * k), Math.round(gy + (p[1] / (float) f - oy) * k)};
        }
        int[][] b = GunsmithLayout.boxes(width, height, points);
        for (GunSlot s : GunSlot.values())
        {
            boxes[s.ordinal()] = b[s.ordinal()];
            Slot slot = (Slot) container.inventorySlots.get(s.ordinal());
            slot.xDisplayPosition = b[s.ordinal()][0] + 4;
            slot.yDisplayPosition = b[s.ordinal()][1] + 4;
        }
    }

    /** One draw of the gun into a cleared depth buffer, read back as a mask (rows top down). */
    private Silhouette probe(ItemStack gun, float x, float y, float s, int w, int h)
    {
        GL11.glColorMask(false, false, false, false);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        drawGun(gun, x, y, s);
        GL11.glColorMask(true, true, true, true);
        if (depth == null || depth.capacity() < w * h)
        {
            depth = BufferUtils.createFloatBuffer(w * h);
            mask = new boolean[w * h];
        }
        depth.clear();
        GL11.glReadPixels(0, 0, w, h, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
        for (int row = 0; row < h; row++)
        {
            for (int col = 0; col < w; col++)
            {
                mask[(h - 1 - row) * w + col] = depth.get(row * w + col) < 0.9999f;
            }
        }
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        return Silhouette.measure(mask, w, h);
    }

    private void drawLines()
    {
        if (bounds == null)
        {
            return;
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA(255, 255, 255, 140);
        for (GunSlot s : GunSlot.values())
        {
            int[] p = points[s.ordinal()], b = boxes[s.ordinal()];
            if (p == null || b == null || builtIn[s.ordinal()])
            {
                continue;
            }
            int bx = Math.max(b[0], Math.min(b[0] + GunsmithLayout.BOX, p[0]));
            int by = Math.max(b[1], Math.min(b[1] + GunsmithLayout.BOX, p[1]));
            t.addVertex(bx, by, 0);
            t.addVertex(p[0], p[1], 0);
        }
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for (GunSlot s : GunSlot.values())
        {
            int[] p = points[s.ordinal()];
            if (p != null && !builtIn[s.ordinal()])
            {
                drawRect(p[0] - 1, p[1] - 1, p[0] + 2, p[1] + 2, RED);
            }
        }
    }

    private void drawBox(GunSlot s)
    {
        int[] b = boxes[s.ordinal()];
        if (b == null)
        {
            return;
        }
        boolean in = builtIn[s.ordinal()];
        boolean fitted = ((Slot) container.inventorySlots.get(s.ordinal())).getHasStack();
        int border = s == open ? RED : in ? GREY : fitted ? LIGHT : GREY;
        drawRect(b[0] - 1, b[1] - 1, b[0] + GunsmithLayout.BOX + 1, b[1] + GunsmithLayout.BOX + 1, border);
        drawRect(b[0], b[1], b[0] + GunsmithLayout.BOX, b[1] + GunsmithLayout.BOX, in ? 0x80101010 : BOX_BG);
        if (!fitted)
        {
            String t = in ? "" : s.label.toUpperCase();
            small(t, b[0] + GunsmithLayout.BOX / 2 - fontRendererObj.getStringWidth(t) / 4, b[1] + GunsmithLayout.BOX / 2 - 2, DIM);
        }
        String l = labels[s.ordinal()];
        small(l, b[0] + GunsmithLayout.BOX / 2 - fontRendererObj.getStringWidth(l) / 4, b[1] + GunsmithLayout.BOX + 3,
              in ? DIM : fitted ? TEXT : MUTED);
    }

    private void drawList(int mouseX, int mouseY)
    {
        int[] b = boxes[open.ordinal()];
        if (b == null)
        {
            return;
        }
        int n = rows(), shown = Math.min(n, GunsmithLayout.MAX_ROWS);
        int[] l = GunsmithLayout.list(width, height, b, n);
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 300);
        drawRect(l[0] - 1, l[1] - 1, l[0] + l[2] + 1, l[1] + l[3] + 1, RED);
        drawRect(l[0], l[1], l[0] + l[2], l[1] + l[3], 0xF0080808);
        drawRect(l[0], l[1], l[0] + l[2], l[1] + 2, RED);
        fontRendererObj.drawStringWithShadow(open.listTitle(), l[0] + 5, l[1] + 4, TEXT);
        for (int i = 0; i < shown; i++)
        {
            int row = i + scroll, y = l[1] + 14 + i * GunsmithLayout.ROW;
            if (mouseX >= l[0] && mouseX < l[0] + l[2] && mouseY >= y && mouseY < y + GunsmithLayout.ROW)
            {
                drawRect(l[0] + 1, y, l[0] + l[2] - 1, y + GunsmithLayout.ROW, 0x40FFFFFF);
            }
            ItemStack stack = rowStack(row);
            if (stack != null)
            {
                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, l[0] + 3, y + 1);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_LIGHTING);
            }
            String t = fitWidth(rowText(row), l[2] - 26);
            fontRendererObj.drawStringWithShadow(t, l[0] + (stack != null ? 23 : 6), y + 5, stack == null && row == 0 && takeOffRow ? RED : TEXT);
        }
        GL11.glPopMatrix();
    }

    // ---- the list

    private int rows()
    {
        return Math.max(1, entries.size() + (takeOffRow ? 1 : 0));
    }

    private String rowText(int row)
    {
        if (takeOffRow && row == 0)
        {
            return "Take off";
        }
        int i = row - (takeOffRow ? 1 : 0);
        if (i < 0 || i >= entries.size())
        {
            return entries.isEmpty() && !takeOffRow ? "None in your inventory" : "";
        }
        ItemStack s = mc.thePlayer.inventory.getStackInSlot(entries.get(i));
        return s == null ? "" : s.getDisplayName();
    }

    private ItemStack rowStack(int row)
    {
        int i = row - (takeOffRow ? 1 : 0);
        return i < 0 || i >= entries.size() ? null : mc.thePlayer.inventory.getStackInSlot(entries.get(i));
    }

    /** The parts in the whole inventory that fit slot s, by Decimation's rules (on opening and on a change). */
    private void fill(GunSlot s)
    {
        listBuilds++;
        entries.clear();
        ItemStack gun = container.gun();
        for (int i = 0; i < 36; i++)
        {
            ItemStack st = mc.thePlayer.inventory.getStackInSlot(i);
            if (i != container.heldSlot && st != null && GunParts.fits(s, gun, st))
            {
                entries.add(i);
            }
        }
        takeOffRow = !s.usedUp() && ((Slot) container.inventorySlots.get(s.ordinal())).getHasStack();
        scroll = Math.max(0, Math.min(scroll, rows() - GunsmithLayout.MAX_ROWS));
    }

    private void choose(int row)
    {
        GunSlot s = open;
        if (takeOffRow && row == 0)
        {
            mc.playerController.sendEnchantPacket(container.windowId, GunsmithAction.takeOff(s));
        }
        else
        {
            int i = row - (takeOffRow ? 1 : 0);
            if (i >= 0 && i < entries.size())
            {
                mc.playerController.sendEnchantPacket(container.windowId, GunsmithAction.fit(s, entries.get(i)));
            }
            else
            {
                return; // "None in your inventory"
            }
        }
        open = null;
    }

    // ---- input

    @Override
    protected void mouseClicked(int x, int y, int button)
    {
        if (open != null)
        {
            int[] l = GunsmithLayout.list(width, height, boxes[open.ordinal()], rows());
            if (x >= l[0] && x < l[0] + l[2] && y >= l[1] && y < l[1] + l[3])
            {
                int row = (y - l[1] - 14) / GunsmithLayout.ROW;
                if (y >= l[1] + 14 && row < GunsmithLayout.MAX_ROWS)
                {
                    choose(row + scroll);
                }
                return;
            }
            open = null;
        }
        if (mc.thePlayer.inventory.getItemStack() == null)
        {
            for (GunSlot s : GunSlot.values())
            {
                int[] b = boxes[s.ordinal()];
                if (b != null && x >= b[0] && x < b[0] + GunsmithLayout.BOX && y >= b[1] && y < b[1] + GunsmithLayout.BOX)
                {
                    if (!builtIn[s.ordinal()])
                    {
                        open = s;
                        scroll = 0;
                        fill(s);
                    }
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && open != null)
        {
            scroll = Math.max(0, Math.min(rows() - GunsmithLayout.MAX_ROWS, scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char c, int key)
    {
        if (key != 0 && key == attachKey())
        {
            mc.thePlayer.closeScreen();
            return;
        }
        if (key >= 2 && key <= 10 && key - 2 == container.heldSlot)
        {
            return; // the number key of the gun's own slot: the gun stays
        }
        super.keyTyped(c, key);
    }

    /** Decimation's attach key as the player bound it (found by its name once). */
    private int attachKey()
    {
        if (attachKey < 0)
        {
            attachKey = 0;
            for (KeyBinding k : mc.gameSettings.keyBindings)
            {
                if (ATTACH_KEY.equals(k.getKeyDescription()))
                {
                    attachKey = k.getKeyCode();
                }
            }
        }
        return attachKey;
    }

    // ---- text

    /** Text at half size, as Decimation's menus draw their small lines. */
    private void small(String s, int x, int y, int colour)
    {
        GL11.glPushMatrix();
        GL11.glScalef(0.5f, 0.5f, 1);
        fontRendererObj.drawStringWithShadow(s, x * 2, y * 2, colour);
        GL11.glPopMatrix();
    }

    /** s shortened to `max` GUI units at half size. */
    private String fit(String s, int max)
    {
        return fitWidth(s, max * 2);
    }

    private String fitWidth(String s, int max)
    {
        if (fontRendererObj.getStringWidth(s) <= max)
        {
            return s;
        }
        while (s.length() > 1 && fontRendererObj.getStringWidth(s + "..") > max)
        {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }
}
