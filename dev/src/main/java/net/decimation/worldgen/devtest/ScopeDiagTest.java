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
 * aims the AKM with -Psight (default reddot) for 3 s and counts, per frame, the glass draws (PatchScope's glassFrame),
 * the measurements (glassTime), the hand events that reach ScopeZoom's priority and the ones cancelled before it.
 */
public class ScopeDiagTest extends DevTestMode
{
    public String name() { return "scopediag"; }

    private static volatile int handEvents, handCancelled, frames, glassDraws, glassMeasures, emptyBoxes;
    /** The box (Deci.scopeGlassBox) after the last measurement: ScopeZoom copies it each aimed frame. */
    private static volatile float[] lastBox;
    private static volatile int lastFrame;
    private static volatile long lastTime;
    private static volatile boolean on;
    private static boolean listening;
    private int ticks;

    public static final class Watch
    {
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
            }
        }

        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.END || !on)
            {
                return;
            }
            frames++;
            int f = field("glassFrame");
            if (f != lastFrame)
            {
                glassDraws++;
            }
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
            ItemStack s = new ItemStack((Item) Item.itemRegistry.getObject("deci:akm"));
            s.stackTagCompound = new NBTTagCompound();
            s.stackTagCompound.setInteger("ammo", 30);
            s.stackTagCompound.setString("sightAttach", System.getProperty("deciworldgen.autotest.sight", "reddot"));
            mc.gameSettings.thirdPersonView = 0;
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, s);
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
        DevTestResults.value(name(), "frames", frames);
        DevTestResults.value(name(), "hand events (cancelled before ScopeZoom's turn)", handEvents + " (" + handCancelled + ")");
        DevTestResults.value(name(), "glass draws", glassDraws);
        DevTestResults.value(name(), "glass measurements", glassMeasures);
        DevTestResults.value(name(), "glass mask now", field("glassMask"));
        float[] b = lastBox;
        DevTestResults.value(name(), "last glass box", b == null ? "none" : String.format("%.1f %.1f %.1f %.1f (window %d x %d), %d of %d empty",
                             b[0], b[1], b[2], b[3], mc.displayWidth, mc.displayHeight, emptyBoxes, glassMeasures));
        mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, null);
        return false;
    }
}
