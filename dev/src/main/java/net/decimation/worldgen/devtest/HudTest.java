package net.decimation.worldgen.devtest;

import java.util.Arrays;

import net.decimation.fixes.Deci;
import net.decimation.fixes.HudStyle;
import net.decimation.fixes.InlineHud;
import net.decimation.worldgen.DevAutoTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * The two HUD styles (fixes/HudStyle): a survival player in the arena with every condition, some armor, water, health,
 * humanity 72 and 1,250 bottlecaps. Shots: Decimation's HUD (hud_decimation), the inline style (hud_inline: hearts, armor,
 * food / water / blood, humanity bar, caps, condition tags, the player in the corner), the same with the player list key
 * held (hud_inline_tab: the stats box), the Settings page with our button (hud_options), Decimation's HUD again
 * (hud_decimation_after); the inline corner while crouching in third person for comparison (hud_inline_third).
 * Checks: Decimation's switches held while inline and given back after, the Settings button switches the style and its label.
 */
public class HudTest extends DevTestMode
{
    public String name() { return "hud"; }

    private volatile int ticks;
    /** Set by the client when the server may take the conditions off. */
    private volatile boolean finished, creative;
    private boolean[] before;
    private long frameDecimation;
    private int step, wait;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ticks < 20)
        {
            return true;
        }
        mc.gameSettings.hideGUI = false;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 90;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 10;
        wait++;
        int list = mc.gameSettings.keyBindPlayerList.getKeyCode();
        // the corner shows the crouch (inline shots and third person), released before the Settings page
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), step >= 2 && step <= 4);
        switch (step)
        {
            case 0:
                mc.gameSettings.thirdPersonView = 0;
                HudStyle.set(false);
                Deci.showHudStats();
                before = Deci.hudSwitches();
                next();
                break;
            case 1:
                if (wait > 15)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_decimation");
                    HudStyle.set(true);
                    next();
                }
                break;
            case 2:
                if (wait > 15)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_inline");
                    boolean[] held = Deci.hudSwitches();
                    DevTestResults.check(name(), "inline holds Decimation's stats list and old GUI", Arrays.toString(held),
                                         held[0] && held[1], "[true, true]");
                    next();
                }
                break;
            case 3:
                KeyBinding.setKeyBindState(list, true);
                if (wait > 5)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_inline_tab");
                    KeyBinding.setKeyBindState(list, false);
                    mc.gameSettings.thirdPersonView = 1;
                    next();
                }
                break;
            case 4:
                if (wait > 10)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_inline_third");
                    mc.gameSettings.thirdPersonView = 0;
                    DevAutoTest.keepScreen = true;
                    mc.displayGuiScreen(Deci.optionsMenu(null));
                    next();
                }
                break;
            case 5:
                if (wait > 5)
                {
                    GuiButton b = button(mc);
                    DevTestResults.check(name(), "Settings button while inline", b == null ? "missing" : b.displayString,
                                         b != null && "HUD: Inline".equals(b.displayString), "HUD: Inline");
                    DevTestUtil.screenshot(mc, name(), "hud_options");
                    if (b != null)
                    {
                        DevTestUtil.clickAt(mc.currentScreen, b.xPosition + 5, b.yPosition + 5);
                    }
                    next();
                }
                break;
            case 6:
                if (wait > 2)
                {
                    GuiButton b = button(mc);
                    DevTestResults.check(name(), "Settings button click switches to Decimation",
                                         (b == null ? "missing" : b.displayString) + ", inline " + HudStyle.inline(),
                                         b != null && "HUD: Decimation".equals(b.displayString) && !HudStyle.inline(),
                                         "HUD: Decimation, inline false");
                    mc.displayGuiScreen(null);
                    DevAutoTest.keepScreen = false;
                    next();
                }
                break;
            case 7:
                if (wait > 15)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_decimation_after");
                    boolean[] now = Deci.hudSwitches();
                    DevTestResults.check(name(), "Decimation's switches given back", Arrays.toString(now),
                                         Arrays.equals(now, before), Arrays.toString(before));
                    DevTestUtil.unlimitedFps(mc);
                    HudStyle.probe = true;
                    HudStyle.frames = HudStyle.frameNanos = 0;
                    next();
                }
                break;
            case 8:                                       // 100 ticks in Decimation's style
                if (wait > 100)
                {
                    frameDecimation = HudStyle.frameNanos / Math.max(1, HudStyle.frames);
                    HudStyle.set(true);
                    HudStyle.frames = HudStyle.frameNanos = 0;
                    InlineHud.probe = true;
                    InlineHud.drawNanos = InlineHud.frames = InlineHud.glErrors = InlineHud.stackChanged = 0;
                    next();
                }
                break;
            case 9:                                       // 100 ticks in ours
                if (wait > 100)
                {
                    long frameInline = HudStyle.frameNanos / Math.max(1, HudStyle.frames);
                    long ours = InlineHud.drawNanos / Math.max(1, InlineHud.frames);
                    DevTestResults.value(name(), "frame (Decimation's HUD)", String.format("%.3f ms", frameDecimation / 1e6));
                    DevTestResults.value(name(), "frame (inline)", String.format("%.3f ms", frameInline / 1e6));
                    DevTestResults.value(name(), "inline drawing a frame (bars, tags, caps, corner)",
                                         String.format("%.3f ms over %d frames", ours / 1e6, InlineHud.frames));
                    DevTestResults.check(name(), "GL errors after our drawing", InlineHud.glErrors, InlineHud.glErrors == 0, "0");
                    DevTestResults.check(name(), "frames the corner changed the matrix stack", InlineHud.stackChanged,
                                         InlineHud.stackChanged == 0, "0");
                    HudStyle.probe = InlineHud.probe = false;
                    creative = true;                          // the server switches the game mode
                    next();
                }
                break;
            case 10:                                      // the inline style in creative too (user)
                if (wait > 20)
                {
                    DevTestUtil.screenshot(mc, name(), "hud_inline_creative");
                    DevTestResults.check(name(), "creative while inline", mc.playerController.isInCreativeMode(),
                                         mc.playerController.isInCreativeMode(), "true");
                    creative = false;
                    HudStyle.set(false);
                    finished = true;
                    next();
                }
                break;
            default:
                done = wait > 5;
        }
        return !done;
    }

    private void next()
    {
        step++;
        wait = 0;
    }

    private static GuiButton button(Minecraft mc)
    {
        if (mc.currentScreen == null)
        {
            return null;
        }
        for (GuiButton b : DevTestUtil.buttons(mc.currentScreen))
        {
            if (b.displayString.startsWith("HUD: "))
            {
                return b;
            }
        }
        return null;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        if (t == 10)
        {
            p.worldObj.setWorldTime(6000);
            DevTestArena.build(p.worldObj, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = true;
            p.setHealth(13);
            p.getFoodStats().setFoodLevel(15);
            p.inventory.armorInventory[2] = new ItemStack(Items.iron_chestplate);
            p.inventory.armorInventory[0] = new ItemStack(Items.leather_boots);
            p.inventory.mainInventory[0] = new ItemStack(Items.iron_sword);
            p.inventory.mainInventory[1] = new ItemStack(Items.bread, 12);
            p.inventory.mainInventory[2] = new ItemStack(Items.potionitem);
            p.inventory.currentItem = 1;
            Deci.Player d = Deci.player(p);
            d.setBleeding(true);
            d.setInfected(true);
            d.setBrokenLeg(true);
            d.setWater(13);
            int h = d.humanity();
            if (h < 72)
            {
                d.addHumanity(72 - h);
            }
            else if (h > 72)
            {
                d.removeHumanity(h - 72);
            }
            d.addBottlecaps(1250 - d.bottlecaps());
        }
        if (t > 10 && !finished)
        {
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 90, 10);
            WorldSettings.GameType want = creative ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL;
            if (p.theItemInWorldManager.getGameType() != want)
            {
                p.setGameType(want);
            }
        }
        if (t > 10 && finished && Deci.player(p).bleeding())
        {
            Deci.Player d = Deci.player(p);
            d.setBleeding(false);
            d.setInfected(false);
            d.setBrokenLeg(false);
            p.inventory.armorInventory[2] = null;
            p.inventory.armorInventory[0] = null;
            p.inventory.mainInventory[0] = p.inventory.mainInventory[1] = p.inventory.mainInventory[2] = null;
            p.setHealth(p.getMaxHealth());
        }
    }
}
