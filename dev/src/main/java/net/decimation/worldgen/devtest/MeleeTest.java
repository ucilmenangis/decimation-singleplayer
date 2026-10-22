package net.decimation.worldgen.devtest;

import org.lwjgl.input.Keyboard;

import net.decimation.fixes.MeleeMotion;
import net.decimation.fixes.NewMelee;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Our melee weapons (fixes/NewMelee, MeleeRenderer, MeleeMotion). The karambit in slot 1, the katana in slot 2. Checks:
 * a slot change starts the draw, the inspect key starts the inspect. Shots per weapon (melee_<name>_<what>): idle, the
 * draw and the inspect at fixed times (MeleeMotion.testAnim / testTime: the same frame every run), third person from the
 * front, the knife dropped on the floor. -Pgif: the draw and inspect every 1/20 s with the HUD hidden (GIF frames).
 */
public class MeleeTest extends DevTestMode
{
    public String name() { return "melee"; }

    private static final String[] NAMES = {"karambit", "katana"};
    private static final boolean GIF = Boolean.getBoolean("deciworldgen.autotest.gif");
    private static final float[][] DRAW = GIF ? new float[][] {times(0, MeleeMotion.DRAW), times(1, MeleeMotion.DRAW)}
        : new float[][] {{0.0f, 0.15f, 0.3f, 0.5f}, {0.0f, 0.12f, 0.24f, 0.36f, 0.46f, 0.56f, 0.66f, 0.78f}};
    private static final float[][] INSPECT = GIF ? new float[][] {times(0, MeleeMotion.INSPECT), times(1, MeleeMotion.INSPECT)}
        : new float[][] {{0.45f, 1.05f, 1.7f, 2.3f}, {0.4f, 0.85f, 1.3f, 1.75f, 2.0f, 2.3f, 2.95f, 3.2f, 3.45f, 4.15f, 4.45f, 4.7f}};
    /** Slash A, B, C per kind: wind up, strike, follow through (GIF: every 1/20 s). */
    private static final float[][][] SLASH = GIF
        ? new float[][][] {{times(0, MeleeMotion.SLASH_A), times(0, MeleeMotion.SLASH_B), times(0, MeleeMotion.SLASH_C)},
                           {times(1, MeleeMotion.SLASH_A), times(1, MeleeMotion.SLASH_B), times(1, MeleeMotion.SLASH_C)}}
        : new float[][][] {{{0.06f, 0.12f, 0.18f}, {0.06f, 0.12f, 0.18f}, {0.10f, 0.18f, 0.30f}},
                           {{0.07f, 0.13f, 0.19f}, {0.07f, 0.13f, 0.19f}, {0.15f, 0.27f, 0.36f}}};
    private volatile int ticks;
    /** The server drops the weapon of this kind in front of the player (-1 none). */
    private volatile int drop = -1;
    private EntityItem dropped;
    private int step, wait, kind;
    private boolean done;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || ticks < 20)
        {
            return true;
        }
        mc.gameSettings.hideGUI = false;           // hideGUI would hide the hand too: GIF frames drop the HUD instead
        if (GIF)
        {
            if (noHud == null)
            {
                noHud = new NoHud();
                MinecraftForge.EVENT_BUS.register(noHud);
            }
            mc.guiAchievement.func_146257_b();     // the "Press E" tutorial toast
        }
        mc.gameSettings.fovSetting = 70f;
        wait++;
        String n = NAMES[kind];
        int slashes = 0;
        for (float[] s : SLASH[kind])
        {
            slashes += s.length;
        }
        int frames = 1 + DRAW[kind].length + INSPECT[kind].length + slashes;
        if (step == 0)                                     // select the weapon: the draw must start
        {
            hold(mc);
            mc.gameSettings.thirdPersonView = 0;
            look(mc, 90, 8);
            if (wait == 3)
            {
                DevTestResults.check(name(), n + ": slot change starts the draw", MeleeMotion.playing(),
                                     MeleeMotion.playing() == MeleeMotion.DRAW, String.valueOf(MeleeMotion.DRAW));
            }
            if (wait > 40)                                  // the draw over: the inspect key
            {
                KeyBinding.onTick(Keyboard.KEY_Y);
                next();
            }
        }
        else if (step == 1)
        {
            hold(mc);
            look(mc, 90, 8);
            if (wait == 3)
            {
                DevTestResults.check(name(), n + ": inspect key starts the inspect", MeleeMotion.playing(),
                                     MeleeMotion.playing() == MeleeMotion.INSPECT, String.valueOf(MeleeMotion.INSPECT));
            }
            if (wait == 60)
            {
                mc.thePlayer.swingItem();                   // a click: the inspect ends, the first slash plays
            }
            if (wait == 63)
            {
                DevTestResults.check(name(), n + ": a swing starts slash A", MeleeMotion.playing(),
                                     MeleeMotion.playing() == MeleeMotion.SLASH_A, String.valueOf(MeleeMotion.SLASH_A));
            }
            if (wait > 80)
            {
                next();
            }
        }
        else if (step < 2 + frames)                        // the fixed frames
        {
            hold(mc);
            look(mc, 90, 8);
            int f = step - 2;
            if (f == 0)
            {
                MeleeMotion.testAnim = MeleeMotion.NONE;    // the rest pose (an inspect may still be playing)
            }
            else if (f <= DRAW[kind].length)
            {
                MeleeMotion.testAnim = MeleeMotion.DRAW;
                MeleeMotion.testTime = DRAW[kind][f - 1];
            }
            else if (f <= DRAW[kind].length + INSPECT[kind].length)
            {
                MeleeMotion.testAnim = MeleeMotion.INSPECT;
                MeleeMotion.testTime = INSPECT[kind][f - 1 - DRAW[kind].length];
            }
            else
            {
                int s = f - 1 - DRAW[kind].length - INSPECT[kind].length, a = 0;
                while (s >= SLASH[kind][a].length)
                {
                    s -= SLASH[kind][a].length;
                    a++;
                }
                MeleeMotion.testAnim = MeleeMotion.SLASH_A + a;
                MeleeMotion.testTime = SLASH[kind][a][s];
            }
            if (wait > 4)
            {
                String what = f == 0 ? "idle" : f <= DRAW[kind].length ? "draw" + (f - 1)
                    : f <= DRAW[kind].length + INSPECT[kind].length ? "inspect" + (f - 1 - DRAW[kind].length)
                    : "slash" + (f - 1 - DRAW[kind].length - INSPECT[kind].length);
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_" + what);
                next();
            }
        }
        else if (step == 2 + frames)                       // third person, from the front
        {
            hold(mc);
            MeleeMotion.testAnim = -1;
            mc.gameSettings.thirdPersonView = 2;
            look(mc, 90, 15);
            if (wait > 10)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_third");
                mc.gameSettings.thirdPersonView = 0;
                drop = kind;
                next();
            }
        }
        else if (step == 3 + frames)                       // looking down (Decimation's body: no arms of its own), then dropped on the floor
        {
            if (wait <= 8)
            {
                mc.thePlayer.inventory.currentItem = 2;      // empty hand first: what the body alone shows below the view
            }
            else
            {
                hold(mc);
            }
            look(mc, 90, wait <= 16 ? 75 : 40);
            if (wait == 8)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_down_empty");
            }
            if (wait == 16)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_down");
            }
            if (wait > 30)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_dropped");
                drop = -2;                                  // the server removes it
                next();
            }
        }
        else if (kind == 0)
        {
            kind = 1;
            step = 0;
            wait = 0;
        }
        else
        {
            done = wait > 5;
        }
        return !done;
    }

    private void next()
    {
        step++;
        wait = 0;
    }

    /** The slot that holds this pass's knife (looked up by item: the slots are not fixed once in the client's hands). */
    private void hold(Minecraft mc)
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack s = mc.thePlayer.inventory.mainInventory[i];
            if (s != null && NewMelee.kind(s.getItem()) == kind)
            {
                mc.thePlayer.inventory.currentItem = i;
            }
        }
    }

    private static void look(Minecraft mc, float yaw, float pitch)
    {
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        int x = DevTestArena.X, y = DevTestArena.Y, z = DevTestArena.Z;
        if (t == 10)
        {
            p.worldObj.setWorldTime(6000);
            DevTestArena.build(p.worldObj, p);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.disableDamage = true;
            p.inventory.mainInventory[0] = new ItemStack(NewMelee.karambit);
            p.inventory.mainInventory[1] = new ItemStack(NewMelee.katana);
            p.inventory.currentItem = 2;                   // neither in hand yet: selecting one must draw it
        }
        if (t > 10)
        {
            p.playerNetServerHandler.setPlayerLocation(x + 0.5, y, z + 0.5, p.rotationYaw, p.rotationPitch);
        }
        int d = drop;
        if (d >= 0 && dropped == null)
        {
            dropped = new EntityItem(p.worldObj, x - 1.5, y + 0.2, z + 0.5, new ItemStack(d == 0 ? NewMelee.karambit : NewMelee.katana));
            dropped.motionX = dropped.motionY = dropped.motionZ = 0;
            dropped.delayBeforeCanPickup = 32767;
            p.worldObj.spawnEntityInWorld(dropped);
        }
        else if (d == -2 && dropped != null)
        {
            dropped.setDead();
            dropped = null;
            drop = -1;
        }
    }

    private static NoHud noHud;

    /** GIF frames: no HUD at all (nothing of Decimation's art in a picture we commit), the hand and the world stay. */
    public static final class NoHud
    {
        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Pre e)
        {
            e.setCanceled(true);
        }
    }

    /** Every 1/20 s of one animation of a weapon kind, from 0 to its end (GIF frames). */
    private static float[] times(int kind, int anim)
    {
        int n = (int) Math.ceil(MeleeMotion.length(kind, anim) * 20) + 1;
        float[] t = new float[n];
        for (int i = 0; i < n; i++)
        {
            t[i] = i / 20f;
        }
        return t;
    }
}
