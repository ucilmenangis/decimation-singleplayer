package net.decimation.fixes;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * The SpecterDR's switch key: "DeciRemnant: SpecterDR 1x/4x" among Decimation's keys in Controls, default G (free in
 * vanilla, Decimation and Gliby's Voice Chat). A press flips the held gun's SpecterDR between 4x and 1x (SpecterMode)
 * with a soft click; with any other sight, or no gun, nothing happens and nothing plays. Client only.
 */
public final class SpecterKey
{
    private static KeyBinding key;
    /** Clicks played (dev test attachments: one a switch, none without a SpecterDR). */
    private static int clicks;

    private SpecterKey()
    {
    }

    public static void register()
    {
        key = new KeyBinding("key.deciworldgen.specterdr", Keyboard.KEY_G, "key.decimation.category");
        ClientRegistry.registerKeyBinding(key);
        FMLCommonHandler.instance().bus().register(new SpecterKey());
    }

    public static KeyBinding binding()
    {
        return key;
    }

    public static int clicks()
    {
        return clicks;
    }

    /** At the end of the client tick: the keyboard has been read by then (KeyBinding.onTick counts the presses). */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || key == null)
        {
            return;
        }
        while (key.isPressed())
        {
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
            Item sight = held != null && Deci.isGun(held.getItem()) ? Deci.sightAttachment(held) : null;
            if (sight == null || !"specterdr".equals(Deci.attachmentName(sight)))
            {
                continue;
            }
            SpecterMode.toggle(held, mc.thePlayer.inventory.currentItem);
            mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("random.click"), 1.8f));
            clicks++;
        }
    }
}
