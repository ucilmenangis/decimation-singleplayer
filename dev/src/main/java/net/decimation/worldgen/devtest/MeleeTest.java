package net.decimation.worldgen.devtest;

import org.lwjgl.input.Keyboard;

import net.decimation.fixes.MeleeMotion;
import net.decimation.fixes.HudStyle;
import net.decimation.fixes.NewMelee;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.decimation.fixes.Deci;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.util.MovingObjectPosition;

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
        : new float[][] {{0.0f, 0.15f, 0.3f, 0.5f}, {0.0f, 0.24f, 0.46f, 0.66f, 0.78f, 0.9f, 1.05f, 1.2f, 1.4f, 1.7f}};
    private static final float[][] INSPECT = GIF ? new float[][] {times(0, MeleeMotion.INSPECT), times(1, MeleeMotion.INSPECT)}
        : new float[][] {{0.45f, 1.05f, 1.7f, 2.3f}, {0.4f, 0.85f, 1.3f, 1.75f, 2.0f, 2.3f, 2.95f, 3.2f, 3.45f, 4.15f, 4.45f, 4.7f}};
    /** Slash A, B, C per kind: wind up, strike, follow through (GIF: every 1/20 s). */
    private static final float[][][] SLASH = GIF
        ? new float[][][] {{times(0, MeleeMotion.SLASH_A), times(0, MeleeMotion.SLASH_B), times(0, MeleeMotion.SLASH_C)},
                           {times(1, MeleeMotion.SLASH_A), times(1, MeleeMotion.SLASH_B), times(1, MeleeMotion.SLASH_C)}}
        : new float[][][] {{{0.10f, 0.15f, 0.21f}, {0.10f, 0.15f, 0.21f}, {0.10f, 0.18f, 0.30f}},
                           {{0.11f, 0.17f, 0.23f}, {0.11f, 0.17f, 0.23f}, {0.18f, 0.26f, 0.33f}}};
    private volatile int ticks;
    /** The server drops the weapon of this kind in front of the player (-1 none). */
    private volatile int drop = -1;
    /** The client walks forward (look-down shots): the server leaves the player free meanwhile. */
    private volatile boolean walking;
    /** Wide hit check: 1 the client asks for the infected, 2 it stands, 3 the client swung (the server checks), 4 done. */
    private volatile int wide;
    private EntityLiving target;
    private float targetHealth;
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
        else if (step == 3 + frames)                       // looking down while walking (Decimation's body below the view), then dropped
        {
            if (wait <= 8)
            {
                mc.thePlayer.inventory.currentItem = 2;      // empty hand first: what the body alone shows below the view
            }
            else
            {
                hold(mc);
                MeleeMotion.testAnim = MeleeMotion.NONE;   // at rest (the slot change would start the draw)
            }
            walking = wait <= 34;                          // the user: "try look down while moving"
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), walking);
            look(mc, 90, wait <= 8 ? 75 : wait <= 14 ? 30 : wait <= 20 ? 50 : wait <= 27 ? 70 : wait <= 34 ? 88 : 40);
            if (wait == 8)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_down_empty");
            }
            if (wait == 14 || wait == 20 || wait == 27 || wait == 34)
            {
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_down" + (wait == 14 ? 30 : wait == 20 ? 50 : wait == 27 ? 70 : 88));
            }
            if (wait > 46)
            {
                MeleeMotion.testAnim = -1;
                DevTestUtil.screenshot(mc, name(), "melee_" + n + "_dropped");
                drop = -2;                                  // the server removes it
                next();
            }
        }
        else if (step == 4 + frames && kind == 1 && wait <= 40)   // the burning katana with the inline HUD: first person, F5 behind, F5 front
        {
            hold(mc);
            MeleeMotion.testAnim = -1;
            HudStyle.set(true);
            look(mc, 90, 8);
            mc.gameSettings.thirdPersonView = wait <= 14 ? 0 : wait <= 27 ? 1 : 2;
            if (wait == 14 || wait == 27 || wait == 40)
            {
                DevTestUtil.screenshot(mc, name(), "melee_katana_inline" + (wait == 14 ? "" : wait == 27 ? "_behind" : "_front"));
            }
            if (wait == 40)
            {
                HudStyle.set(false);
                mc.gameSettings.thirdPersonView = 0;
            }
        }
        else if (step == 4 + frames && kind == 1)          // the katana hits twice as wide: an infected just off the crosshair
        {
            hold(mc);
            look(mc, 90, 0);
            if (wait == 42)
            {
                wide = 1;                                   // the server stands one there
            }
            if (wait == 70)
            {
                MovingObjectPosition over = mc.objectMouseOver;
                boolean miss = over == null || over.typeOfHit == MovingObjectPosition.MovingObjectType.MISS;
                DevTestResults.check(name(), "katana: the infected is off the crosshair", over == null ? "none" : over.typeOfHit,
                                     miss, "MISS");
                mc.thePlayer.swingItem();
            }
            if (wait == 80)
            {
                wide = 3;                                   // the server checks the hit
            }
            if (wait > 86)
            {
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

    /**
     * The wide hit's target: a plain infected 2.5 blocks ahead, its middle 0.62 to the side of the view's line, so the
     * crosshair misses its box (0.3 + 0.1 border) and only the katana's doubled width (0.6 + 0.1) reaches it; held in
     * place each tick. After the swing: the katana's 17 off its health (Fire Aspect may add a burn).
     */
    private void wideHit(EntityPlayerMP p, int x, int y, int z)
    {
        double tx = x + 0.5 - 2.5, tz = z + 0.5 + 0.62;
        if (wide == 1 && (target == null || target.isDead))
        {
            EntityLiving e = Deci.newInfected(p.worldObj);
            e.setPosition(tx, y, tz);
            if (p.worldObj.spawnEntityInWorld(e) && Deci.isPlainInfected(e))
            {
                target = e;
                targetHealth = e.getHealth();
                wide = 2;
            }
            else
            {
                e.setDead();                                // a hulk or bloater (wider), or refused: again next tick
            }
        }
        if (target != null && !target.isDead && wide >= 2)
        {
            target.setPositionAndUpdate(tx, y, tz);
            target.motionX = target.motionZ = 0;
            target.setAttackTarget(null);
            target.rotationYaw = target.rotationYawHead = target.renderYawOffset = -90;
        }
        if (wide == 3)
        {
            float now = target == null || target.isDead ? 0 : target.getHealth();
            DevTestResults.check(name(), "katana: a swing just beside the infected hits it for 17", targetHealth - now,
                                 targetHealth - now >= 17 || now <= 0, ">= 17");
            if (target != null)
            {
                target.setDead();
            }
            wide = 4;
        }
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
            // the katana gets Fire Aspect II from a book on an anvil (NewMelee.onAnvil): the fire is drawn in every shot
            AnvilUpdateEvent anvil = new AnvilUpdateEvent(new ItemStack(NewMelee.katana),
                Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(Enchantment.fireAspect, 2)), "", 0);
            MinecraftForge.EVENT_BUS.post(anvil);
            int level = anvil.output == null ? 0 : NewMelee.fireAspect(anvil.output);
            DevTestResults.check(name(), "katana + Fire Aspect II book on an anvil", level, level == 2, "2");
            p.inventory.mainInventory[1] = anvil.output != null ? anvil.output : new ItemStack(NewMelee.katana);
            p.inventory.currentItem = 2;                   // neither in hand yet: selecting one must draw it
        }
        wideHit(p, x, y, z);
        if (t > 10 && !walking)
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
