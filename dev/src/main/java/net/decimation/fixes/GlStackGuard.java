package net.decimation.fixes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.lwjgl.opengl.GL11;

/**
 * OpenGL "Stack overflow" on a host (bug.md, 19 Oktober 2026): Decimation leaves matrices pushed in two places, the
 * stack fills (32 levels) and every push after that fails, so later models are drawn with the wrong matrix.
 * <ul>
 * <li>its melee / throwable item renderers return without their pop in first person: Minecraft draws the old item that
 * way for a few frames after every switch away from it (2 to 5 levels per switch). Wrapped: the pop follows.</li>
 * <li>its name tag handler returns without its pop for another player while fancy nameplates are off (one level per
 * player a frame, multiplayer only). The pop follows in a handler after it.</li>
 * <li>a guard for anything else: each frame ends with the matrix and attribute stacks as deep as it began; what a
 * renderer left behind is set back at once and counted in the log (once a minute at most).</li>
 * </ul>
 * Plain GL 1.1 calls through LWJGL: the same on macOS, Linux and Windows drivers.
 */
@SideOnly(Side.CLIENT)
public final class GlStackGuard
{
    private static final long LOG_MS = 60_000;
    private static int modelview, attrib, projection;
    private static boolean started;
    private static int fixedFrames, worst, sinceLog;
    private static long lastLog;

    private GlStackGuard()
    {
    }

    /** Client postInit: after Decimation registered its item renderers. */
    public static void install()
    {
        int wrapped = 0;
        for (Object o : Item.itemRegistry)
        {
            Item item = (Item) o;
            try
            {
                IItemRenderer r = MinecraftForgeClient.getItemRenderer(new ItemStack(item), IItemRenderer.ItemRenderType.EQUIPPED);
                if (Deci.leavesFirstPersonPush(r))
                {
                    MinecraftForgeClient.registerItemRenderer(item, new FirstPersonPop(r));
                    wrapped++;
                }
            }
            catch (RuntimeException ex)
            {
                // another mod's renderer that cannot answer for a plain stack: not one of Decimation's two
            }
        }
        GlStackGuard guard = new GlStackGuard();
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(guard);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        FMLLog.info("[deciworldgen] GL stack guard on; %d item renderers get their missing first person pop", wrapped);
    }

    /** Frames whose stacks were set back since the game started (tests). */
    public static int fixedFrames()
    {
        return fixedFrames;
    }

    /** Decimation's ModelItemRenderer / BModelItemRenderer with the pop their first person branch lacks. */
    static final class FirstPersonPop implements IItemRenderer
    {
        private final IItemRenderer real;

        FirstPersonPop(IItemRenderer real)
        {
            this.real = real;
        }

        public boolean handleRenderType(ItemStack stack, ItemRenderType type)
        {
            return real.handleRenderType(stack, type);
        }

        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper)
        {
            return real.shouldUseRenderHelper(type, stack, helper);
        }

        public void renderItem(ItemRenderType type, ItemStack stack, Object... data)
        {
            real.renderItem(type, stack, data);
            if (type == ItemRenderType.EQUIPPED_FIRST_PERSON && stack != null)
            {
                GL11.glPopMatrix(); // both return right after their push here (deobf/src: renderItem of each)
            }
        }
    }

    /**
     * ClientRenderHandler.onRenderNameTag (deci.c.b) pushes first and returns without the pop for an EntityPlayer when
     * fancy nameplates are off (and the GUI is shown), without cancelling: this runs right after it on that path only.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNameTag(RenderLivingEvent.Specials.Pre e)
    {
        if (e.entity instanceof EntityPlayer && !Minecraft.getMinecraft().gameSettings.hideGUI && !Deci.fancyNameplates())
        {
            GL11.glPopMatrix();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFrameStart(TickEvent.RenderTickEvent e)
    {
        if (e.phase != TickEvent.Phase.START)
        {
            return;
        }
        modelview = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        attrib = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
        projection = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        started = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFrameEnd(TickEvent.RenderTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || !started)
        {
            return;
        }
        started = false;
        int extra = 0;
        int m = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int a = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
        int p = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        if (m > modelview || p > projection)
        {
            int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            for (int i = p; i > projection; i--)
            {
                GL11.glPopMatrix();
            }
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            for (int i = m; i > modelview; i--)
            {
                GL11.glPopMatrix();
            }
            GL11.glMatrixMode(mode);
            extra += Math.max(0, m - modelview) + Math.max(0, p - projection);
        }
        for (int i = a; i > attrib; i--)
        {
            GL11.glPopAttrib();
            extra++;
        }
        if (extra == 0)
        {
            return;
        }
        fixedFrames++;
        sinceLog++;
        worst = Math.max(worst, extra);
        long now = System.currentTimeMillis();
        if (now - lastLog >= LOG_MS)
        {
            FMLLog.info("[deciworldgen] GL stack guard: %d frame(s) left the GL stacks deeper (worst +%d: modelview %d of %d, "
                        + "attrib %d of %d, projection %d of %d), set back", sinceLog, worst, m, modelview, a, attrib, p, projection);
            lastLog = now;
            sinceLog = 0;
            worst = 0;
        }
    }
}
