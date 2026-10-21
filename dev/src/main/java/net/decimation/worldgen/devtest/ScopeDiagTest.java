package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Why no scope glass is measured (19 Oktober 2026: aimdrift's shift stayed 0.0000, v0.43.1 measured 0.0049 / -0.2189):
 * aims the AKM with -Psight (default ta11acog; Decimation's red dot drew no measured glass, 0 draws a frame, 21 Oktober
 * 2026) for 3 s and counts, per frame, the glass draws (PatchScope's glassFrame),
 * the measurements (glassTime), the hand events that reach ScopeZoom's priority and the ones cancelled before it, and
 * where in the frame the glass is drawn. The production client with OptiFine drew it 6 times a frame (2 before the hand,
 * 1 in it, 3 after: bug.md "See-through glass not updated with OptiFine"); -Pextra=N draws the held gun N more times
 * after the hand each frame (as that client's HUD does) to see the same in the dev client. Check: the glass is measured
 * while aiming.
 */
public class ScopeDiagTest extends DevTestMode
{
    public String name() { return "scopediag"; }

    private static volatile int handEvents, handCancelled, frames, glassDraws, glassMeasures, emptyBoxes;
    /** Frames by how many glass draws they had (0 .. 6, 7 or more): PatchScope counts every draw (glassFrame). */
    private static final int[] perFrame = new int[8];
    /** The box (Deci.scopeGlassBox) after the last measurement: ScopeZoom copies it each aimed frame. */
    private static volatile float[] lastBox;
    private static volatile int lastFrame;
    private static volatile long lastTime;
    private static volatile boolean on;
    private static boolean listening;
    private int ticks;
    private volatile Runnable onServer;

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    /** Glass draws per frame before the first hand event, between it and the last, and after it (sums over the run). */
    private static final long[] where = new long[3];
    private static int frameStart = -1, firstHand = -1, lastHand = -1;

    /** -Pextra: draws of the held gun after the hand each frame (the HUD of the production client draws it again). */
    private static volatile int extra;
    private static net.minecraft.client.renderer.RenderBlocks blocks;

    public static final class Watch
    {
        @SubscribeEvent
        public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post e)
        {
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
            if (!on || extra <= 0 || held == null || e.type != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL)
            {
                return;
            }
            net.minecraftforge.client.IItemRenderer r = net.minecraftforge.client.MinecraftForgeClient.getItemRenderer(held,
                net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED);
            if (r == null)
            {
                return;
            }
            if (blocks == null)
            {
                blocks = new net.minecraft.client.renderer.RenderBlocks();
            }
            for (int i = 0; i < extra; i++)
            {
                org.lwjgl.opengl.GL11.glPushMatrix();
                org.lwjgl.opengl.GL11.glTranslatef(40, 40, -100);
                org.lwjgl.opengl.GL11.glScalef(20, 20, 20);
                r.renderItem(net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED, held, blocks, mc.thePlayer);
                org.lwjgl.opengl.GL11.glPopMatrix();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void onHand(RenderHandEvent e)
        {
            if (on)
            {
                handEvents++;
                if (e.isCanceled())
                {
                    handCancelled++;
                }
                lastHand = field("glassFrame");
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public void onHandFirst(RenderHandEvent e)
        {
            if (on && firstHand < 0)
            {
                firstHand = field("glassFrame");
            }
        }

        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            if (e.phase == TickEvent.Phase.START)
            {
                frameStart = field("glassFrame");
                firstHand = lastHand = -1;
                return;
            }
            if (e.phase != TickEvent.Phase.END || !on)
            {
                return;
            }
            int end = field("glassFrame");
            if (frameStart >= 0 && firstHand >= 0 && lastHand >= 0)
            {
                where[0] += firstHand - frameStart;
                where[1] += lastHand - firstHand;
                where[2] += end - lastHand;
            }
            frames++;
            int f = field("glassFrame");
            if (f != lastFrame)
            {
                glassDraws++;
            }
            perFrame[Math.max(0, Math.min(7, f - lastFrame))]++;
            lastFrame = f;
            long t = Deci.scopeGlassTime();
            if (t != lastTime)
            {
                glassMeasures++;
                float[] b = Deci.scopeGlassBox();
                lastBox = b;
                emptyBoxes += b[2] > b[0] && b[3] > b[1] ? 0 : 1;
            }
            lastTime = t;
        }
    }

    static int field(String n)
    {
        try
        {
            return deci.n.f.class.getField(n).getInt(null);
        }
        catch (Exception e)
        {
            return -9;
        }
    }

    public boolean client(Minecraft mc)
    {
        if (!listening)
        {
            Watch w = new Watch();
            FMLCommonHandler.instance().bus().register(w);
            MinecraftForge.EVENT_BUS.register(w);
            listening = true;
        }
        if (mc.thePlayer == null)
        {
            return true;
        }
        ticks++;
        if (ticks == 1)
        {
            handEvents = handCancelled = frames = glassDraws = glassMeasures = emptyBoxes = 0;
            java.util.Arrays.fill(perFrame, 0);
            java.util.Arrays.fill(where, 0);
            lastBox = null;
            extra = Integer.getInteger("deciworldgen.autotest.extra", 0);
            ItemStack s = new ItemStack((Item) Item.itemRegistry.getObject("deci:akm"));
            s.stackTagCompound = new NBTTagCompound();
            s.stackTagCompound.setInteger("ammo", 30);
            s.stackTagCompound.setString("sightAttach", System.getProperty("deciworldgen.autotest.sight", "ta11acog"));
            mc.gameSettings.thirdPersonView = 0;
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, s);
            // the server's copy too: else its next slot sync puts back what it holds (an empty slot after attachments)
            final int slot = mc.thePlayer.inventory.currentItem;
            final ItemStack copy = s.copy();
            onServer = () -> {
                net.minecraft.entity.player.EntityPlayerMP p = DevTestUtil.player();
                if (p != null)
                {
                    p.inventory.setInventorySlotContents(slot, copy);
                }
            };
            DevTestResults.value(name(), "scoped gun (Deci.isScopedGun)", Deci.isScopedGun(s));
            DevTestResults.value(name(), "pip property (zoom mode off when true)", Boolean.getBoolean("decimation.scope.pip"));
            return true;
        }
        Deci.setAimMode(mc.thePlayer, 1);
        on = ticks > 20;
        if (ticks < 80)
        {
            return true;
        }
        on = false;
        Deci.setAimMode(mc.thePlayer, 0);
        ItemStack held = mc.thePlayer.getHeldItem();
        DevTestResults.value(name(), "held at the end", held == null ? "nothing" : Item.itemRegistry.getNameForObject(held.getItem()));
        DevTestResults.value(name(), "frames", frames);
        DevTestResults.value(name(), "hand events (cancelled before ScopeZoom's turn)", handEvents + " (" + handCancelled + ")");
        DevTestResults.value(name(), "glass draws", glassDraws);
        DevTestResults.value(name(), "glass measurements", glassMeasures);
        DevTestResults.value(name(), "glass mask now", field("glassMask"));
        StringBuilder pf = new StringBuilder();
        for (int i = 0; i < perFrame.length; i++)
        {
            pf.append(i == 0 ? "" : " / ").append(perFrame[i]);
        }
        DevTestResults.value(name(), "frames with 0 / 1 / .. / 6 / 7+ glass draws", pf.toString());
        DevTestResults.value(name(), "glass draws before / inside / after the hand events", where[0] + " / " + where[1] + " / " + where[2]
                             + " (sums over " + frames + " frames)");
        float[] b = lastBox;
        DevTestResults.value(name(), "last glass box", b == null ? "none" : String.format("%.1f %.1f %.1f %.1f (window %d x %d), %d of %d empty",
                             b[0], b[1], b[2], b[3], mc.displayWidth, mc.displayHeight, emptyBoxes, glassMeasures));
        DevTestResults.check(name(), "glass measured while aiming" + (extra > 0 ? " (" + extra + " more draws a frame)" : ""),
                             glassMeasures + " measurements in " + frames + " frames, " + (b == null ? "no box"
                             : String.format("box %.0f x %.0f", b[2] - b[0], b[3] - b[1])),
                             glassMeasures * 10 >= frames && b != null && b[2] > b[0] && b[3] > b[1],
                             "one every 10 frames or more (ScopeZoom: every 8th, every 2nd while learning), a box on the screen");
        mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, null);
        return false;
    }
}
