package net.decimation.worldgen.devtest;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Small helpers shared by the dev test modes. */
public final class DevTestUtil
{
    private DevTestUtil()
    {
    }

    /** The singleplayer server's player, or null. */
    public static EntityPlayerMP player()
    {
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.getConfigurationManager() == null || s.getConfigurationManager().playerEntityList.isEmpty())
        {
            return null;
        }
        return (EntityPlayerMP) s.getConfigurationManager().playerEntityList.get(0);
    }

    /** Current fps from the debug line ("N fps, M chunk updates"). */
    public static int fps(Minecraft mc)
    {
        String dbg = mc.debug;
        return Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
    }

    /** Unlimited frame rate, no vsync (fps measurements). */
    public static void unlimitedFps(Minecraft mc)
    {
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        org.lwjgl.opengl.Display.setVSyncEnabled(false);
    }

    /** Saves screenshots/<name>.png and records it in the results. */
    public static void screenshot(Minecraft mc, String mode, String name)
    {
        net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight,
                                                           mc.getFramebuffer());
        DevTestResults.shot(mode, name);
    }

    /**
     * The buttons of a screen (GuiScreen.buttonList is protected). Reflection names: MCP in the dev client, SRG in the
     * shipped game (the production client runs these tests too: docs/production_test.md).
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<net.minecraft.client.gui.GuiButton> buttons(net.minecraft.client.gui.GuiScreen screen)
    {
        try
        {
            java.lang.reflect.Field f;
            try
            {
                f = net.minecraft.client.gui.GuiScreen.class.getDeclaredField("buttonList");
            }
            catch (NoSuchFieldException shipped)
            {
                f = net.minecraft.client.gui.GuiScreen.class.getDeclaredField("field_146292_n");
            }
            f.setAccessible(true);
            return (java.util.List<net.minecraft.client.gui.GuiButton>) f.get(screen);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("buttonList", e);
        }
    }

    /** A button's label without colour codes. */
    public static String label(net.minecraft.client.gui.GuiButton b)
    {
        return b.displayString == null ? "" : net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(b.displayString).trim();
    }

    /** The button with this id on a screen, or null. */
    public static net.minecraft.client.gui.GuiButton byId(net.minecraft.client.gui.GuiScreen screen, int id)
    {
        for (net.minecraft.client.gui.GuiButton b : buttons(screen))
        {
            if (b.id == id)
            {
                return b;
            }
        }
        return null;
    }

    /**
     * What a click on the button does: the screen's actionPerformed (Decimation's menu buttons too); func_146284_a in
     * the shipped game.
     */
    public static void press(net.minecraft.client.gui.GuiScreen screen, net.minecraft.client.gui.GuiButton b)
    {
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass())
        {
            for (String name : new String[] {"actionPerformed", "func_146284_a"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(name, net.minecraft.client.gui.GuiButton.class);
                    m.setAccessible(true);
                    m.invoke(screen, b);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException("press " + b.displayString, e);
                }
            }
        }
        throw new IllegalStateException("no actionPerformed on " + screen.getClass());
    }

    /**
     * A real left click on the button's middle (GuiScreen.mouseClicked, func_73864_a in the shipped game): unlike press it
     * goes through Forge's ActionPerformedEvent, so handlers of other mods see it.
     */
    public static void click(net.minecraft.client.gui.GuiScreen screen, net.minecraft.client.gui.GuiButton b)
    {
        int x = b.xPosition + b.width / 2, y = b.yPosition + b.height / 2;
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass())
        {
            for (String name : new String[] {"mouseClicked", "func_73864_a"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(name, int.class, int.class, int.class);
                    m.setAccessible(true);
                    m.invoke(screen, x, y, 0);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException("click " + b.displayString, e);
                }
            }
        }
        throw new IllegalStateException("no mouseClicked on " + screen.getClass());
    }

    /**
     * A real left click at a point of the screen (GuiScreen.mouseClicked, func_73864_a in the shipped game), the way a
     * mouse reaches Decimation's panels: inside DeciScreen.mouseClicked's loop over them (bug.md "Clicking a news popup
     * crashed the game": a shortcut past that loop hid the crash).
     */
    public static void clickAt(net.minecraft.client.gui.GuiScreen screen, int x, int y)
    {
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass())
        {
            for (String name : new String[] {"mouseClicked", "func_73864_a"})
            {
                try
                {
                    java.lang.reflect.Method m = c.getDeclaredMethod(name, int.class, int.class, int.class);
                    m.setAccessible(true);
                    m.invoke(screen, x, y, 0);
                    return;
                }
                catch (NoSuchMethodException e)
                {
                    // the other name, then the parent class
                }
                catch (java.lang.reflect.InvocationTargetException e)
                {
                    throw new IllegalStateException("click at " + x + "," + y, e.getCause());
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException("click at " + x + "," + y, e);
                }
            }
        }
        throw new IllegalStateException("no mouseClicked on " + screen.getClass());
    }

    /** True when the class file of `cls` (e.g. "deci/d/p") as the game loads it contains `text` (UTF-8). */
    public static boolean classHas(String cls, String text)
    {
        try
        {
            java.io.InputStream in = DevTestUtil.class.getResourceAsStream("/" + cls + ".class");
            if (in == null)
            {
                return false;
            }
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; )
            {
                b.write(buf, 0, n);
            }
            in.close();
            String bytes = new String(b.toByteArray(), "ISO-8859-1");
            return bytes.contains(new String(text.getBytes("UTF-8"), "ISO-8859-1"));
        }
        catch (java.io.IOException e)
        {
            return false;
        }
    }

    public static void deleteRecursive(File f)
    {
        File[] kids = f.listFiles();
        if (kids != null)
        {
            for (File k : kids)
            {
                deleteRecursive(k);
            }
        }
        f.delete();
    }
}
