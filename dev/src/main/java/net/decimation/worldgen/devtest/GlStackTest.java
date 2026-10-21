package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.List;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * bug.md "OpenGL Stack overflow on the host": held items are drawn for a few frames each, and the GL stacks must end
 * every frame as deep as a clean frame does (modelview, attribute, projection, and the texture matrix stack of texture
 * units 0 and 1, which has only 2 levels at the GL minimum). Phase 1: every item with its own renderer, first and third
 * person. Phase 2: guns with each sight (scope glass drawing) and each grip, hip and aimed, first and third person. A
 * renderer that pushes without popping shows as a depth that grows by one a frame.
 */
public class GlStackTest extends DevTestMode
{
    public String name() { return "glstack"; }

    private static final int FRAMES = 6;
    private static final String[] GUNS = {"akm", "m4a4"};
    private static final String[] SIGHTS = {"reddot", "2x", "4x", "8x", "eotech558", "ta11acog", "specterdr"};
    /** Grip slot attachments, each placed per gun (GripPlacement): Decimation's three and our RK-1. */
    private static final String[] GRIPS = {"foregrip", "rk1", "laser", "flashlight"};
    private static volatile int frames;
    private static volatile int[] depth = new int[5]; // modelview, attrib, projection, texture unit 0, texture unit 1
    private static volatile int[] resetTo;
    private static boolean listening;

    /** One thing to hold: an item, optionally with a sight, aimed or not, in a view. */
    private static final class Case
    {
        final ItemStack stack;
        final String label;
        final boolean aim;
        final int view;
        int kind; // 0 hold the stack, 1 another player in view with fancy nameplates off, 2 a frame that leaks 3 on purpose

        Case(ItemStack stack, String label, boolean aim, int view)
        {
            this.stack = stack;
            this.label = label;
            this.aim = aim;
            this.view = view;
        }

        Case kind(int k)
        {
            kind = k;
            return this;
        }
    }

    private final List<Case> cases = new ArrayList<Case>();
    private final List<String> leaks = new ArrayList<String>();
    private int index = -1, start, ticks, items, guns;
    private int[] base;
    private int savedView;
    private boolean injected;

    /** End of every frame: optionally pops back to the clean depths, then reads them. */
    public static final class Watch
    {
        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.END)
            {
                return;
            }
            int[] r = resetTo;
            if (r != null)
            {
                popTo(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH, r[0]);
                while (GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH) > r[1])
                {
                    GL11.glPopAttrib();
                }
                popTo(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH, r[2]);
                int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                popTo(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, r[3]);
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                popTo(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, r[4]);
                GL13.glActiveTexture(active);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                resetTo = null;
            }
            depth = read();
            GL11.glGetError(); // a leak sets the error flag; clear it so each case is read on its own
            frames++;
        }
    }

    /** Delegates to a renderer and records any call that leaves the modelview stack changed (which call, which type). */
    static final class Probe implements IItemRenderer
    {
        static final java.util.Map<String, Integer> seen = new java.util.TreeMap<String, Integer>();
        final IItemRenderer real;
        final String item;

        Probe(IItemRenderer real, String item)
        {
            this.real = real;
            this.item = item;
        }

        public boolean handleRenderType(ItemStack s, ItemRenderType t)
        {
            return real.handleRenderType(s, t);
        }

        public boolean shouldUseRenderHelper(ItemRenderType t, ItemStack s, ItemRendererHelper h)
        {
            return real.shouldUseRenderHelper(t, s, h);
        }

        public void renderItem(ItemRenderType t, ItemStack s, Object... data)
        {
            int before = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            real.renderItem(t, s, data);
            int after = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            if (after != before)
            {
                String k = item + " " + t + " " + (after - before);
                Integer n = seen.get(k);
                seen.put(k, n == null ? 1 : n + 1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<Item, IItemRenderer> renderers()
    {
        try
        {
            java.lang.reflect.Field f = MinecraftForgeClient.class.getDeclaredField("customItemRenderers");
            f.setAccessible(true);
            return (java.util.Map<Item, IItemRenderer>) f.get(null);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private final java.util.Map<Item, IItemRenderer> wrapped = new java.util.IdentityHashMap<Item, IItemRenderer>();

    private void probe(boolean on)
    {
        java.util.Map<Item, IItemRenderer> m = renderers();
        if (m == null)
        {
            DevTestResults.value(name(), "renderer probe", "customItemRenderers not found");
            return;
        }
        if (on)
        {
            for (java.util.Map.Entry<Item, IItemRenderer> e : m.entrySet())
            {
                wrapped.put(e.getKey(), e.getValue());
                e.setValue(new Probe(e.getValue(), Item.itemRegistry.getNameForObject(e.getKey())));
            }
        }
        else
        {
            m.putAll(wrapped);
            wrapped.clear();
        }
    }

    private static volatile int inject;

    /** Pushes `inject` matrices once, inside a frame (the leak the guard must set back). */
    public static final class Leak
    {
        @SubscribeEvent
        public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post e)
        {
            if (e.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL && inject > 0)
            {
                for (int i = 0; i < inject; i++)
                {
                    GL11.glPushMatrix();
                }
                inject = 0;
            }
        }
    }

    private static final int OTHER_ID = -424242;
    private boolean fancy;

    private void otherPlayer(Minecraft mc, boolean on)
    {
        if (on)
        {
            fancy = Deci.fancyNameplates();
            Deci.setFancyNameplates(false);
            net.minecraft.client.entity.EntityOtherPlayerMP o = new net.minecraft.client.entity.EntityOtherPlayerMP(mc.theWorld,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "GlStackOther"));
            net.minecraft.util.Vec3 look = mc.thePlayer.getLookVec();
            o.setPositionAndRotation(mc.thePlayer.posX + look.xCoord * 3, mc.thePlayer.posY - mc.thePlayer.yOffset,
                                     mc.thePlayer.posZ + look.zCoord * 3, mc.thePlayer.rotationYaw + 180, 0);
            mc.theWorld.addEntityToWorld(OTHER_ID, o);
        }
        else
        {
            mc.theWorld.removeEntityFromWorld(OTHER_ID);
            Deci.setFancyNameplates(fancy);
        }
    }

    private static void popTo(int mode, int query, int to)
    {
        GL11.glMatrixMode(mode);
        while (GL11.glGetInteger(query) > to)
        {
            GL11.glPopMatrix();
        }
    }

    private static int[] read()
    {
        int[] d = new int[5];
        d[0] = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        d[1] = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
        d[2] = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        d[3] = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        d[4] = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL13.glActiveTexture(active);
        return d;
    }

    private static String show(int[] d)
    {
        return "modelview " + d[0] + ", attrib " + d[1] + ", projection " + d[2] + ", texture0 " + d[3] + ", texture1 " + d[4];
    }

    public boolean client(Minecraft mc)
    {
        if (!listening)
        {
            FMLCommonHandler.instance().bus().register(new Watch());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Leak());
            listening = true;
            return true;
        }
        if (mc.thePlayer == null)
        {
            return true;
        }
        ticks++;
        if (ticks == 1)
        {
            build();
            if (Boolean.getBoolean("deciworldgen.autotest.glprobe"))
            {
                probe(true);
            }
            savedView = mc.gameSettings.thirdPersonView;
            hold(mc, null, false, 0);
            DevTestResults.value(name(), "items with their own renderer", items);
            DevTestResults.value(name(), "gun with sight or grip cases", guns);
            return true;
        }
        if (ticks < 4)
        {
            return true;
        }
        if (base == null)
        {
            base = depth.clone(); // empty hand: the depths a clean frame ends with
            DevTestResults.value(name(), "clean frame depths", show(base));
            next(mc);
            return true;
        }
        Case c = cases.get(index);
        if (c.aim)
        {
            Deci.setAimMode(mc.thePlayer, 1); // the server may send the mode back each tick
        }
        if (c.kind == 2 && !injected && frames - start >= 2)
        {
            inject = 3;
            injected = true;
        }
        if (frames - start < FRAMES + 1)
        {
            return true;
        }
        int[] d = depth;
        if (c.kind == 1)
        {
            otherPlayer(mc, false);
        }
        if (Boolean.getBoolean("deciworldgen.autotest.glall"))
        {
            DevTestResults.value(name(), "case", c.label + ": " + show(d));
        }
        if (!java.util.Arrays.equals(d, base))
        {
            leaks.add(c.label + ": " + show(d));
        }
        if (index + 1 >= cases.size())
        {
            hold(mc, null, false, savedView);
            resetTo = base;
            if (!wrapped.isEmpty())
            {
                probe(false);
                for (java.util.Map.Entry<String, Integer> e : Probe.seen.entrySet())
                {
                    DevTestResults.value(name(), "renderer call changed modelview", e.getKey() + " (" + e.getValue() + " calls)");
                }
            }
            for (String l : leaks)
            {
                DevTestResults.value(name(), "leak", l);
            }
            DevTestResults.check(name(), "no held item leaves a GL stack deeper", leaks.size() + " leaking", leaks.isEmpty(),
                                 "0 leaking (" + show(base) + " after each frame)");
            return false;
        }
        next(mc);
        return true;
    }

    private void build()
    {
        try
        {
            build0();
        }
        finally
        {
            cases.add(new Case(null, "another player in view, fancy nameplates off", false, 0).kind(1));
            cases.add(new Case(null, "the frame after a frame that leaked 3", false, 0).kind(2));
        }
    }

    private void build0()
    {
        String only = System.getProperty("deciworldgen.autotest.glonly", "");
        if (!only.isEmpty()) // a chosen order, first person only (glonly=ItemMachete,akm,ak74)
        {
            for (String n : only.split(","))
            {
                Item i = (Item) Item.itemRegistry.getObject("deci:" + n);
                if (i != null)
                {
                    cases.add(new Case(new ItemStack(i), n + " first person #" + cases.size(), false, 0));
                    items++;
                }
            }
            return;
        }
        for (Object o : Item.itemRegistry)
        {
            Item i = (Item) o;
            ItemStack s = new ItemStack(i);
            if (MinecraftForgeClient.getItemRenderer(s, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON) != null
                || MinecraftForgeClient.getItemRenderer(s, IItemRenderer.ItemRenderType.EQUIPPED) != null)
            {
                String n = Item.itemRegistry.getNameForObject(i);
                cases.add(new Case(s, n + " first person", false, 0));
                cases.add(new Case(s, n + " third person", false, 1));
                items++;
            }
        }
        for (String g : GUNS)
        {
            Item gun = (Item) Item.itemRegistry.getObject("deci:" + g);
            if (gun == null)
            {
                continue;
            }
            for (int a = 0; a < SIGHTS.length + GRIPS.length; a++)
            {
                boolean sight = a < SIGHTS.length;
                String part = sight ? SIGHTS[a] : GRIPS[a - SIGHTS.length];
                for (int k = 0; k < 4; k++)
                {
                    ItemStack s = new ItemStack(gun);
                    s.stackTagCompound = new NBTTagCompound();
                    s.stackTagCompound.setInteger("ammo", 0);
                    s.stackTagCompound.setString(sight ? "sightAttach" : "gripAttach", part);
                    boolean aim = k % 2 == 1;
                    int view = k / 2;
                    cases.add(new Case(s, g + " + " + part + (aim ? " aimed" : " hip") + (view == 0 ? " first" : " third") + " person", aim, view));
                    guns++;
                }
            }
        }
    }

    private void next(Minecraft mc)
    {
        index++;
        Case c = cases.get(index);
        resetTo = base;
        hold(mc, c.stack == null ? null : c.stack.copy(), c.aim, c.view);
        if (c.kind == 1)
        {
            otherPlayer(mc, true);
        }
        injected = false;
        start = frames;
    }

    private static void hold(Minecraft mc, ItemStack s, boolean aim, int view)
    {
        mc.gameSettings.thirdPersonView = view;
        mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, s);
        Deci.setAimMode(mc.thePlayer, aim ? 1 : 0);
    }
}
