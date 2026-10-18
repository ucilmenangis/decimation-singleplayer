package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;

/**
 * bug.md "HUD crash after joining a world without Decimation's menu": Decimation's HUD reads the menu's player preview
 * with fancy graphics; a world reached without the menu has none. With the preview gone (fancy graphics off meanwhile, so
 * the HUD cannot crash the test), the guard must create it again within half a second.
 */
public class HudGuardTest extends DevTestMode
{
    public String name() { return "hudguard"; }

    private int ticks;
    private boolean fancy;

    public boolean client(Minecraft mc)
    {
        ticks++;
        if (ticks == 1)
        {
            fancy = mc.gameSettings.fancyGraphics;
            mc.gameSettings.fancyGraphics = false;
            Deci.clearMenuPreview();
            return true;
        }
        if (ticks < 12)
        {
            return true;
        }
        DevTestResults.check(name(), "menu player preview there again", Deci.hasMenuPreview(), Deci.hasMenuPreview(),
                             "true within half a second");
        if (!Deci.hasMenuPreview())
        {
            Deci.ensureMenuPreview(); // leave the game as it was
        }
        mc.gameSettings.fancyGraphics = fancy;
        return false;
    }
}
