package net.decimation.fixes;

import java.io.File;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

/**
 * Two HUD styles (user 22 Oktober 2026: "an option to switch to inline mode, which is like minecraft default"):
 * DECIMATION is Decimation's own HUD unchanged (stats list top right with the player figure behind it, the column of big
 * food / water / blood / health icons); INLINE is ours, Minecraft's layout above the hotbar (InlineHud) and the player
 * alone in the top right corner as Minecraft's console editions show it (HudDoll).
 * Switched by the "DeciRemnant: HUD style" key (default J, free in vanilla, Decimation and the voice chat mod) or the
 * HUD button on Decimation's Settings page; kept in config/deciworldgen_hud.cfg. While INLINE, Decimation's own switches
 * are held (stats list hidden, its old GUI on so it stops hiding vanilla's bars); the player's own values come back on
 * the switch to DECIMATION. Client only.
 */
@SideOnly(Side.CLIENT)
public final class HudStyle
{
    /** Our button id on Decimation's Settings page (its own use 0 .. 107, 200, 1000 .. 1002). */
    static final int BUTTON = 951;
    private static boolean inline;
    private static Configuration cfg;
    private static KeyBinding key;
    /** Decimation's switches as the player had them before INLINE took them over (Deci.hudSwitches); null while not held. */
    private static boolean[] saved;
    /** Dev test hud: time between frames' HUDs, both styles (the frame time with and without the inline style). */
    public static boolean probe;
    public static long frames, frameNanos;
    private static long lastFrame;

    private HudStyle()
    {
    }

    public static void install(File configDir)
    {
        cfg = new Configuration(new File(configDir, "deciworldgen_hud.cfg"));
        inline = "inline".equals(cfg.getString("style", "hud", "decimation",
                                               "decimation: Decimation's own HUD, inline: Minecraft style bars over the hotbar and the player in the corner",
                                               new String[] {"decimation", "inline"}));
        cfg.save();
        key = new KeyBinding("key.deciworldgen.hudstyle", Keyboard.KEY_J, "key.decimation.category");
        ClientRegistry.registerKeyBinding(key);
        HudStyle h = new HudStyle();
        FMLCommonHandler.instance().bus().register(h);
        MinecraftForge.EVENT_BUS.register(h);
        MinecraftForge.EVENT_BUS.register(new InlineHud());
    }

    public static boolean inline()
    {
        return inline;
    }

    /** Switches the style and saves it (the key, the Settings button, dev tests). */
    public static void set(boolean on)
    {
        inline = on;
        cfg.get("hud", "style", "decimation").set(on ? "inline" : "decimation");
        cfg.save();
        if (!on && saved != null)
        {
            Deci.setHudSwitches(saved[0], saved[1]);
            saved = null;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || key == null)
        {
            return;
        }
        while (key.isPressed())
        {
            set(!inline);
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("random.click"), 1.0f));
        }
    }

    /**
     * First of the frame's HUD events: Decimation's switches held while INLINE (its reload config button and its O key
     * would otherwise bring the stats list back).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOverlay(RenderGameOverlayEvent.Pre e)
    {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL)
        {
            return;
        }
        long now = System.nanoTime();
        if (probe && lastFrame > 0)
        {
            frameNanos += now - lastFrame;
            frames++;
        }
        lastFrame = probe ? now : 0;
        if (!inline)
        {
            return;
        }
        if (saved == null)
        {
            saved = Deci.hudSwitches();
        }
        Deci.setHudSwitches(true, true);
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e)
    {
        if (!Deci.isOptionsMenu(e.gui))
        {
            return;
        }
        // the free place right of View Updates (GuiMenuOptions: width / 2 + 5, height / 6 + 42)
        e.buttonList.add(Deci.menuButton(BUTTON, e.gui.width / 2 + 5, e.gui.height / 6 + 42, 150, 20, label(),
                                         "Decimation: its own HUD. Inline: Minecraft style bars and your player in the corner. Key: "
                                         + Keyboard.getKeyName(key.getKeyCode())));
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.ActionPerformedEvent.Pre e)
    {
        if (e.button.id != BUTTON || !Deci.isOptionsMenu(e.gui))
        {
            return;
        }
        e.setCanceled(true);
        set(!inline);
        e.button.displayString = label(); // the button plays its own click
    }

    private static String label()
    {
        return "HUD: " + (inline ? "Inline" : "Decimation");
    }
}
