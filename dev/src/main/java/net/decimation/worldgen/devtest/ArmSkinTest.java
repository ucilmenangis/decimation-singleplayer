package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

/**
 * The hand holding a gun showed Steve for everyone (user 19 Oktober 2026, "even its real minecraft account player"):
 * Decimation's gun renderer draws the first person arms with GunItemRenderer.TJ, "skins/<name>", a texture only
 * vanilla's dead skins.minecraft.net download could fill. The arms must use the player's own skin (getLocationSkin:
 * the profile's texture from Minecraft's skin manager, Steve only when the account has none).
 */
public class ArmSkinTest extends DevTestMode
{
    public String name() { return "armskin"; }

    private int ticks;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ++ticks < 5)
        {
            return true;
        }
        ResourceLocation arms = Deci.gunArmSkin(), own = mc.thePlayer.getLocationSkin();
        ITextureObject t = arms == null ? null : mc.getTextureManager().getTexture(arms);
        DevTestResults.value(name(), "gun arm texture", arms + " (" + (t == null ? "not loaded" : t.getClass().getSimpleName()) + ")");
        DevTestResults.value(name(), "player skin", String.valueOf(own));
        DevTestResults.check(name(), "the gun's arms use the player's own skin", String.valueOf(arms), own != null && own.equals(arms),
                             String.valueOf(own));
        return false;
    }
}
