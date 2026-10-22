package net.decimation.fixes;

import java.lang.reflect.Field;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * The first person motion of our melee weapons (user 23 Oktober 2026: "an animation of it when it is pulled, do also an
 * inspect animation ... karambit smooth like cs2 or valorant hybrid ... katana the same like in valorant, extra smooth").
 * DRAW plays when a knife comes into the hand (slot change, pick up, joining with it held); INSPECT on the "DeciRemnant:
 * Inspect" key (default Y, Valorant's), cancelled by a swing. Keyframes {seconds, hand x y z, hand pitch yaw roll, knife
 * pitch yaw roll} (hand in blocks of the first person view, x right, y up, z toward the eye; knife turns about its
 * pivot: the karambit's finger ring, the katana's grip), joined by a Catmull-Rom curve (no corner at any key: the speed
 * runs on through it), timed by the system clock and read every frame, so the motion is as smooth as the frame rate
 * whatever the tick rate. A slow breathing sway lies under everything. Minecraft's own equip dip is held at rest while a
 * knife of ours is in hand (the draw replaces it), and Decimation's first person body does not draw it (it would show
 * a second knife below the view). Client only.
 */
@SideOnly(Side.CLIENT)
public final class MeleeMotion
{
    public static final int NONE = 0, DRAW = 1, INSPECT = 2, SLASH_A = 3, SLASH_B = 4, SLASH_C = 5;
    /** Dev test melee: an animation and its time in seconds instead of the clock (testAnim -1: the clock). */
    public static int testAnim = -1;
    public static float testTime;

    // {t, hx, hy, hz, hrx, hry, hrz, kx, ky, kz, ox, oy, oz}: hand place and turn, knife turn about the view's axes at the
    // grip, knife place off the hand (thrown: the katana's draw); columns left out are 0
    private static final float[][] KARAMBIT_DRAW = {
        {0.00f, 0.12f, -0.62f, 0.15f, -52, 12, -28, 0, 0, -540},
        {0.30f, 0.02f, 0.06f, 0.00f, 8, -5, 6, 0, 0, -60},
        {0.55f, 0.00f, -0.02f, 0.00f, -3, 2, -2, 0, 0, 8},
        {0.80f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][] KARAMBIT_INSPECT = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.45f, -0.12f, 0.12f, 0.06f, 12, -35, 35, 0, 0, 0},
        {1.05f, -0.12f, 0.12f, 0.06f, 12, -60, -30, 0, 0, 0},
        {1.40f, -0.04f, 0.06f, 0.02f, 4, -15, 0, 0, 0, 0},
        {2.05f, -0.04f, 0.08f, 0.02f, 4, -15, 0, 0, 0, 720},
        {2.45f, 0.00f, 0.02f, 0.00f, 0, 0, 0, 0, 0, 720},
        {2.80f, 0, 0, 0, 0, 0, 0, 0, 0, 720}};
    // the katana's draw after Valorant's Sovereign blade (user's reference frames, 23 Oktober 2026): pulled up from the
    // lower right, flicked up out of the hand spinning, the open hand waiting under it, a fake catch that lets it slip and
    // turn once more, then the real catch as the hand sweeps left into the low hold, blade out to the right
    private static final float[][] KATANA_DRAW = {
        {0.00f, 0.55f, -0.35f, 0.00f, 0, 0, 0, 0, 0, 75, 0, 0, 0},
        {0.12f, 0.50f, 0.10f, -0.15f, 0, 0, -15, 0, 0, 140, -0.02f, 0.10f, 0},
        {0.24f, 0.48f, 0.05f, -0.15f, 0, 0, -8, -10, 0, 250, -0.05f, 0.32f, -0.05f},
        {0.36f, 0.46f, 0.02f, -0.15f, 0, 0, -4, -10, 0, 435, -0.06f, 0.38f, -0.05f},
        {0.46f, 0.44f, 0.06f, -0.15f, 0, 0, 0, -5, 0, 470, -0.10f, 0.22f, -0.05f},
        {0.56f, 0.42f, 0.03f, -0.12f, 0, 0, 6, 0, 0, 520, -0.12f, 0.05f, 0},
        {0.66f, 0.22f, 0.08f, -0.08f, 0, 0, 4, 0, 0, 615, -0.18f, 0.15f, 0},
        {0.78f, 0.05f, 0.00f, 0.00f, 0, 0, 2, 0, 0, 700, -0.10f, 0.00f, 0},
        {0.88f, 0.00f, -0.03f, 0.00f, 0, 0, 0, 0, 0, 725, 0, 0, 0},
        {1.00f, -0.01f, 0.01f, 0.00f, 0, 0, 0, 0, 0, 718, 0, 0, 0},
        {1.12f, 0, 0, 0, 0, 0, 0, 0, 0, 720, 0, 0, 0}};
    // inspect: a dip, the blade raised upright in the middle and tilted to show its flat, hung point down high on the
    // right, swung flat across the view to admire its length, then spun down and round into the rest; the blade stays
    // in the view's plane (a turn round the view's up axis would swing its tip through the camera)
    private static final float[][] KATANA_INSPECT = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.40f, 0.03f, -0.10f, 0.00f, 0, 0, 0, 0, 0, -20},
        {0.85f, 0.25f, 0.02f, -0.05f, 0, 0, 0, 0, 0, 75},
        {1.30f, 0.26f, 0.04f, -0.05f, 0, 0, 0, -15, 0, 82},
        {1.75f, 0.26f, 0.04f, -0.05f, 0, 0, 0, 10, 25, 68},
        {2.30f, 0.35f, 0.42f, -0.50f, 0, 0, 0, 0, 0, 255},
        {2.95f, 0.37f, 0.44f, -0.50f, 0, 0, 0, 0, 0, 250},
        {3.45f, 0.50f, 0.15f, -0.30f, 0, 0, 0, 0, 35, 172},
        {4.15f, 0.48f, 0.17f, -0.30f, 0, 0, 0, 0, 35, 168},
        {4.85f, 0, 0, 0, 0, 0, 0, 0, 0, 360}};
    // the slashes after the Sovereign blade's attacks (user's reference video, 23 Oktober 2026), chained by quick clicks:
    // A a forehand (hand raised high right, the blade sweeps right to left leading, ends low left), B a backhand (blade
    // pulled across to the left, sweeps back to the right), C the heavy one (arm up overhead, blade cocked back, a big
    // diagonal down to the lower left); each starts and ends at rest so any can follow any
    private static final float[][] KATANA_SLASH_A = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.07f, 0.75f, 0.30f, -0.15f, 0, 0, -20, 0, -20, 80},
        {0.13f, 0.35f, 0.12f, -0.25f, 0, 0, 10, 0, 0, 160},
        {0.19f, -0.15f, -0.05f, -0.10f, 0, 0, 15, 0, 0, 115},
        {0.30f, 0.00f, -0.05f, 0.00f, 0, 0, 4, 0, 0, 30},
        {0.42f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][] KATANA_SLASH_B = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.07f, -0.05f, 0.05f, -0.10f, 0, 0, 15, 0, 20, 150},
        {0.13f, 0.40f, 0.15f, -0.25f, 0, 0, -5, 0, 0, 90},
        {0.19f, 0.62f, 0.05f, -0.12f, 0, 0, -15, 0, 0, 25},
        {0.30f, 0.20f, 0.00f, 0.00f, 0, 0, -4, 0, 0, -5},
        {0.42f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][] KATANA_SLASH_C = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.15f, 0.55f, 0.40f, -0.50f, -20, 0, -10, 30, 0, 100},
        {0.27f, 0.15f, 0.08f, -0.35f, 10, 0, 10, -20, 0, 190},
        {0.36f, -0.20f, -0.05f, -0.15f, 5, 0, 15, -10, 0, 170},
        {0.50f, 0.00f, -0.05f, 0.00f, 0, 0, 3, 0, 0, 20},
        {0.62f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    // the karambit's: short wrist cuts (the claw stays in the fist), a forehand, a backhand and a thrust
    private static final float[][] KARAMBIT_SLASH_A = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.06f, 0.30f, 0.15f, -0.05f, -10, 15, -30, 0, 0, 0},
        {0.12f, -0.05f, 0.02f, -0.15f, 5, -10, 25, 0, 0, 0},
        {0.18f, -0.25f, -0.12f, 0.00f, 8, -15, 35, 0, 0, 0},
        {0.32f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][] KARAMBIT_SLASH_B = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.06f, -0.20f, 0.05f, -0.05f, -5, -15, 30, 0, 0, 0},
        {0.12f, 0.15f, 0.08f, -0.15f, 0, 10, -20, 0, 0, 0},
        {0.18f, 0.30f, -0.08f, 0.00f, 5, 15, -30, 0, 0, 0},
        {0.32f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][] KARAMBIT_SLASH_C = {
        {0.00f, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0.10f, 0.06f, 0.08f, 0.05f, 15, 0, -10, 0, 0, 0},
        {0.18f, -0.06f, 0.06f, -0.35f, -15, 0, 5, 0, 0, 0},
        {0.30f, -0.04f, 0.04f, -0.25f, -10, 0, 0, 0, 0, 0},
        {0.45f, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    private static final float[][][][] ANIMS = {
        {KARAMBIT_DRAW, KARAMBIT_INSPECT, KARAMBIT_SLASH_A, KARAMBIT_SLASH_B, KARAMBIT_SLASH_C},
        {KATANA_DRAW, KATANA_INSPECT, KATANA_SLASH_A, KATANA_SLASH_B, KATANA_SLASH_C}};
    /** Clicks closer than this (seconds) chain the slashes A, B, C; a longer pause starts again at A. */
    private static final float COMBO = 0.9f;

    private static KeyBinding key;
    private static Field equipped, prevEquipped, drawn, drawnSlot, mainModel;
    /** The models whose arms are out of Decimation's first person body draw now (onBodyPre / onBodyPost). */
    private static final ModelBiped[] hidden = new ModelBiped[3];
    private static int anim, lastSlot = -1, combo;
    private static long start, lastSlash;
    /** Minecraft's own arm swing, held at 0 for the frame's first person draw and put back after (onRenderTick). */
    private static float swing, prevSwing;
    private static boolean swingHeld;
    private static Item lastItem;
    private static final float[] pose = new float[12];

    private MeleeMotion()
    {
    }

    public static void install()
    {
        key = new KeyBinding("key.deciworldgen.inspect", Keyboard.KEY_Y, "key.decimation.category");
        ClientRegistry.registerKeyBinding(key);
        MeleeMotion m = new MeleeMotion();
        FMLCommonHandler.instance().bus().register(m);
        MinecraftForge.EVENT_BUS.register(m);
    }

    /**
     * The motion now for a weapon kind (0 karambit, 1 katana): {hand x y z, hand pitch yaw roll, knife pitch yaw roll,
     * knife x y z off the hand}, added to the weapon's rest pose by MeleeRenderer. One array, filled again each call.
     */
    static float[] pose(int kind)
    {
        java.util.Arrays.fill(pose, 0);
        int a = testAnim >= 0 ? testAnim : anim;
        float t = testAnim >= 0 ? testTime : (System.nanoTime() - start) / 1e9f;
        if (a != NONE)
        {
            float[][] keys = ANIMS[kind][a - 1];
            if (t >= keys[keys.length - 1][0] && testAnim < 0)
            {
                anim = NONE;
            }
            else
            {
                curve(keys, Math.min(t, keys[keys.length - 1][0]), pose);
            }
        }
        double s = System.nanoTime() / 1e9;            // breathing: never still, never noticeable
        pose[1] += (float) (0.008 * Math.sin(s * 1.7));
        pose[3] += (float) (0.5 * Math.sin(s * 1.3));
        pose[5] += (float) (0.8 * Math.sin(s * 1.1));
        return pose;
    }

    /** How long an animation of a weapon kind runs, in seconds (dev test melee: GIF frames). */
    public static float length(int kind, int anim)
    {
        float[][] keys = ANIMS[kind][anim - 1];
        return keys[keys.length - 1][0];
    }

    /** Catmull-Rom through the keys at time t (end keys doubled: the motion starts and settles at rest). */
    static void curve(float[][] keys, float t, float[] out)
    {
        int i = 0;
        while (i < keys.length - 2 && t > keys[i + 1][0])
        {
            i++;
        }
        float[] p0 = keys[Math.max(0, i - 1)], p1 = keys[i], p2 = keys[i + 1], p3 = keys[Math.min(keys.length - 1, i + 2)];
        float u = Math.max(0, Math.min(1, (t - p1[0]) / Math.max(1e-4f, p2[0] - p1[0])));
        float u2 = u * u, u3 = u2 * u;
        for (int k = 0; k < out.length; k++)
        {
            float a = at(p0, k), b = at(p1, k), c = at(p2, k), d = at(p3, k);
            out[k] = 0.5f * (2 * b + (c - a) * u + (2 * a - 5 * b + 4 * c - d) * u2 + (3 * b - a - 3 * c + d) * u3);
        }
    }

    /** Channel k of a key (after its time), 0 where the key leaves it out. */
    private static float at(float[] key, int k)
    {
        return k + 1 < key.length ? key[k + 1] : 0;
    }

    /** True while a draw or inspect plays (dev test). */
    public static int playing()
    {
        return anim;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null)
        {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        Item item = held == null ? null : held.getItem();
        int slot = mc.thePlayer.inventory.currentItem;
        int kind = NewMelee.kind(item);
        boolean ours = kind >= 0;
        if (anim != NONE && (!ours || (System.nanoTime() - start) / 1e9f > ANIMS[kind][anim - 1][ANIMS[kind][anim - 1].length - 1][0]))
        {
            anim = NONE;                               // over (also when the view never drew it)
        }
        if (ours && (item != lastItem || slot != lastSlot))
        {
            anim = DRAW;
            start = System.nanoTime();
        }
        lastItem = item;
        lastSlot = slot;
        while (key.isPressed())
        {
            if (ours && anim != DRAW)
            {
                anim = INSPECT;
                start = System.nanoTime();
            }
        }
        if (ours && mc.thePlayer.isSwingInProgress && mc.thePlayer.swingProgressInt == 0 && anim != DRAW)
        {
            long now = System.nanoTime();                // a new swing (Minecraft restarts it at 0): the next slash
            combo = (now - lastSlash) / 1e9f < COMBO ? (combo + 1) % 3 : 0;
            lastSlash = now;
            anim = SLASH_A + combo;                      // also ends an inspect, as in CS and Valorant
            start = now;
        }
    }

    /**
     * Minecraft's equip dip left out while our knife is in hand (the draw is its own way in): the item the first person
     * view draws and its slot set to the knife in hand, the dip held at rest. Minecraft only swaps the drawn item while
     * its dip is down, so holding the dip alone kept the old item on screen. Its arm swing is held at 0 for the frame
     * too (our slashes are the swing) and put back at the frame's end, so the third person body still swings.
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.END)
        {
            if (swingHeld && mc.thePlayer != null)
            {
                mc.thePlayer.swingProgress = swing;
                mc.thePlayer.prevSwingProgress = prevSwing;
            }
            swingHeld = false;
            return;
        }
        if (mc.thePlayer == null || NewMelee.kind(itemInHand(mc)) < 0)
        {
            return;
        }
        if (mc.gameSettings.thirdPersonView == 0)
        {
            swing = mc.thePlayer.swingProgress;
            prevSwing = mc.thePlayer.prevSwingProgress;
            mc.thePlayer.swingProgress = 0;
            mc.thePlayer.prevSwingProgress = 0;
            swingHeld = true;
        }
        try
        {
            if (equipped == null)
            {
                equipped = ReflectionHelper.findField(ItemRenderer.class, "equippedProgress", "field_78454_c");
                prevEquipped = ReflectionHelper.findField(ItemRenderer.class, "prevEquippedProgress", "field_78451_d");
                drawn = ReflectionHelper.findField(ItemRenderer.class, "itemToRender", "field_78453_b");
                drawnSlot = ReflectionHelper.findField(ItemRenderer.class, "equippedItemSlot", "field_78450_g");
            }
            ItemRenderer r = mc.entityRenderer.itemRenderer;
            drawn.set(r, mc.thePlayer.getHeldItem());
            drawnSlot.setInt(r, mc.thePlayer.inventory.currentItem);
            equipped.setFloat(r, 1.0f);
            prevEquipped.setFloat(r, 1.0f);
        }
        catch (Exception ex)
        {
            equipped = null;                           // left to Minecraft: its dip on top of the draw
        }
    }

    /**
     * Decimation cancels the first person hand for anything but its guns (ClientEventHandler.onRenderHand: its own body
     * stands in); for our knives the hand is drawn again, so MeleeRenderer's first person view shows.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderHand(RenderHandEvent e)
    {
        if (e.isCanceled() && NewMelee.kind(itemInHand(Minecraft.getMinecraft())) >= 0
            && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)
        {
            e.setCanceled(false);
        }
    }

    /** Decimation's first person body would hold a second knife below the view. */
    @SubscribeEvent
    public void onSpecials(RenderPlayerEvent.Specials.Pre e)
    {
        if (Deci.isFirstPersonBodyRenderer(e.renderer) && NewMelee.kind(itemInHand(Minecraft.getMinecraft())) >= 0)
        {
            e.renderItem = false;
        }
    }

    /**
     * Decimation's first person body shows its arms unless a gun is in hand (ModelDeciPlayer.render sets their isHidden
     * on every draw), so with our knife looking down showed three hands. showModel, which it never sets, takes them out
     * of that one draw (its model, the renderer's biped and armor), put back right after.
     */
    @SubscribeEvent
    public void onBodyPre(RenderPlayerEvent.Pre e)
    {
        if (!Deci.isFirstPersonBodyRenderer(e.renderer) || NewMelee.kind(itemInHand(Minecraft.getMinecraft())) < 0)
        {
            return;
        }
        if (mainModel == null)
        {
            mainModel = ReflectionHelper.findField(RendererLivingEntity.class, "mainModel", "field_77045_g");
        }
        Object main;
        try
        {
            main = mainModel.get(e.renderer);
        }
        catch (IllegalAccessException ex)
        {
            throw new IllegalStateException("RendererLivingEntity.mainModel not readable", ex);
        }
        hidden[0] = main instanceof ModelBiped ? (ModelBiped) main : null;
        hidden[1] = e.renderer.modelBipedMain;
        hidden[2] = e.renderer.modelArmorChestplate;
        arms(false);
    }

    @SubscribeEvent
    public void onBodyPost(RenderPlayerEvent.Post e)
    {
        if (hidden[1] != null)
        {
            arms(true);
            java.util.Arrays.fill(hidden, null);
        }
    }

    private static void arms(boolean show)
    {
        for (ModelBiped m : hidden)
        {
            if (m != null)
            {
                m.bipedRightArm.showModel = show;
                m.bipedLeftArm.showModel = show;
            }
        }
    }

    private static Item itemInHand(Minecraft mc)
    {
        ItemStack s = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        return s == null ? null : s.getItem();
    }
}
