package net.decimation.fixes;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * A draw for every gun (user 23 Oktober 2026: "when switching to weapons (guns) they all need some kind of intro ...
 * valorant smoothness or cs2 like ... arena breakout"). Decimation has none: a gun just rose with Minecraft's flat equip
 * dip. Now each gun comes into the view its own way by its WeaponCategory, and its own Rack animation (Decimation's
 * Rack.anib with its rack sounds, Deci.playRack) works the action at the end: pistols snap up from the lower right with
 * a flick and pull the slide (CS2), the revolver twirls once, SMGs swing in from the left (Valorant), rifles and shotguns
 * rise low and canted, settle with their weight and rack (Arena Breakout), machine guns the same slower and heavier, the
 * rocket launcher comes up onto the shoulder. On a switch to a gun and on leaving Decimation's holster.
 * Drawn by wrapping each gun's item renderer the first time the gun is in hand (gun packs too): for the first person
 * view it turns back to the view's axes (as MeleeRenderer does), moves the gun, and lets Decimation draw it from there.
 * Minecraft's equip dip is held off meanwhile (MeleeMotion.holdEquip); aiming during the draw runs it three times as
 * fast so the sights come up. Client only.
 */
@SideOnly(Side.CLIENT)
public final class GunDraw
{
    /** Dev test gundraw: a fixed time in seconds into the held gun's draw instead of the clock (-1: the clock). */
    public static float testTime = -1;
    /** Dev test gundraw: the racks played at the end of draws. */
    public static int racks;

    /** One way of drawing: keys {t, x, y, z, pitch, yaw, roll} and when the gun's own rack starts (-1: none). */
    private static final class Style
    {
        final float rackAt;
        final float[][] keys;

        Style(float rackAt, float[][] keys)
        {
            this.rackAt = rackAt;
            this.keys = keys;
        }

        float length()
        {
            return keys[keys.length - 1][0];
        }
    }

    // keys in the view's axes about the gun's grip: x right, y up, z toward the eye (blocks); pitch tips the muzzle
    // up, yaw turns it left, roll turns the gun counter clockwise as seen (degrees); every draw starts low but already in
    // view (a gun below the view for the first tenth of a second read as a delay) and ends at rest
    private static final Style PISTOL = new Style(0.20f, new float[][] {
        {0.00f, 0.12f, -0.32f, 0.10f, -45, 15, 65},
        {0.16f, 0.04f, 0.03f, 0.00f, 6, -3, -8},
        {0.28f, -0.01f, -0.01f, 0.00f, -2, 1, 3},
        {0.50f, 0, 0, 0, 0, 0, 0}});
    private static final Style REVOLVER = new Style(0.38f, new float[][] {
        {0.00f, 0.25f, -0.35f, 0.10f, -330, 0, 15},
        {0.22f, 0.02f, 0.03f, 0.00f, 12, 0, -4},
        {0.34f, 0.00f, -0.01f, 0.00f, -3, 0, 1},
        {0.62f, 0, 0, 0, 0, 0, 0}});
    private static final Style SMG = new Style(0.30f, new float[][] {
        {0.00f, -0.25f, -0.42f, 0.10f, -30, -25, -50},
        {0.20f, 0.03f, 0.04f, 0.00f, 5, 3, 7},
        {0.34f, -0.01f, 0.00f, 0.00f, -2, -1, -2},
        {0.62f, 0, 0, 0, 0, 0, 0}});
    private static final float[][] RIFLE_KEYS = {
        {0.00f, 0.15f, -0.50f, 0.20f, -30, 30, 40},
        {0.24f, 0.03f, -0.08f, 0.06f, 3, -5, -12},
        {0.38f, 0.00f, 0.02f, 0.00f, 3, 0, -4},
        {0.52f, 0.00f, -0.01f, 0.03f, -3, 0, 1},
        {0.80f, 0, 0, 0, 0, 0, 0}};
    private static final Style RIFLE = new Style(0.42f, RIFLE_KEYS);
    private static final Style RIFLE_NO_RACK = new Style(-1, RIFLE_KEYS);
    private static final Style SHOTGUN = new Style(0.45f, new float[][] {
        {0.00f, 0.20f, -0.50f, 0.20f, -25, 25, 35},
        {0.28f, 0.02f, 0.03f, 0.00f, 5, -3, -7},
        {0.42f, 0.00f, 0.00f, 0.00f, -1, 0, 0},
        {0.55f, 0.00f, -0.01f, 0.06f, -3, 0, 0},
        {0.85f, 0, 0, 0, 0, 0, 0}});
    private static final Style MG = new Style(0.60f, new float[][] {
        {0.00f, 0.15f, -0.45f, 0.20f, -35, 35, 50},
        {0.38f, 0.04f, -0.06f, 0.05f, 6, -6, -14},
        {0.58f, 0.00f, 0.02f, 0.00f, -2, 2, 3},
        {0.72f, 0.00f, -0.01f, 0.02f, -2, 0, 0},
        {1.05f, 0, 0, 0, 0, 0, 0}});
    private static final Style ROCKET = new Style(-1, new float[][] {
        {0.00f, 0.30f, -0.62f, 0.30f, -20, 40, 30},
        {0.50f, 0.02f, 0.05f, 0.00f, 4, -4, -5},
        {0.75f, 0.00f, -0.01f, 0.00f, -1, 1, 1},
        {1.10f, 0, 0, 0, 0, 0, 0}});
    private static final Map<String, Style> STYLES = new HashMap<String, Style>();

    static
    {
        STYLES.put("pistol", PISTOL);
        STYLES.put("revolver", REVOLVER);
        STYLES.put("smg", SMG);
        STYLES.put("rifle", RIFLE);
        STYLES.put("shotgun", SHOTGUN);
        STYLES.put("lmg", MG);
        STYLES.put("mg", MG);
        STYLES.put("rocket", ROCKET);
        STYLES.put("crossbow", RIFLE_NO_RACK);
        STYLES.put("flamethrower", RIFLE_NO_RACK);
    }

    /** The draw playing (null: none), when it started (nanoTime, moved back while aiming), its rack done. */
    private static Style style;
    private static long start;
    private static boolean racked, lastHolstered;
    private static Item lastItem;
    private static int lastSlot = -1;
    private static final float[] pose = new float[6];

    private GunDraw()
    {
    }

    public static void install()
    {
        FMLCommonHandler.instance().bus().register(new GunDraw());
    }

    /** The way a gun is drawn (rifles' for an unknown category). */
    private static Style styleOf(Item gun)
    {
        Style s = STYLES.get(Deci.gunCategory(gun));
        return s == null ? RIFLE : s;
    }

    /** Seconds into the draw playing. */
    private static float elapsed()
    {
        return (System.nanoTime() - start) / 1e9f;
    }

    /** True while a draw plays (dev test). */
    public static boolean playing()
    {
        return style != null;
    }

    /** How long a gun's draw runs (dev test), and whether it ends with the gun's rack. */
    public static float length(Item gun)
    {
        return styleOf(gun).length();
    }

    public static boolean racksAtEnd(Item gun)
    {
        return styleOf(gun).rackAt >= 0;
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
        boolean holstered = Deci.holstered(mc.thePlayer);
        if (!Deci.isGun(item))
        {
            style = null;
        }
        else
        {
            wrap(held);
            if (item != lastItem || slot != lastSlot || lastHolstered && !holstered)
            {
                style = styleOf(item);
                start = System.nanoTime();
                racked = false;
            }
        }
        lastItem = item;
        lastSlot = slot;
        lastHolstered = holstered;
        if (style == null)
        {
            return;
        }
        if (Deci.aimMode(mc.thePlayer) != 0)
        {
            start -= 100_000_000L;                       // aiming: two more ticks a tick, three times as fast
        }
        float t = elapsed();
        if (!racked && style.rackAt >= 0 && t >= style.rackAt)
        {
            racked = true;
            racks++;
            Deci.playRack(held);
        }
        if (t >= style.length())
        {
            style = null;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.START && mc.thePlayer != null && (style != null || testTime >= 0)
            && Deci.isGun(mc.thePlayer.getHeldItem() == null ? null : mc.thePlayer.getHeldItem().getItem()))
        {
            MeleeMotion.holdEquip(mc);
        }
    }

    /** The held gun's renderer wrapped once (Decimation's, a gun pack's, re-registered or not). */
    private static void wrap(ItemStack gun)
    {
        IItemRenderer r = MinecraftForgeClient.getItemRenderer(gun, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (r != null && !(r instanceof Wrap))
        {
            MinecraftForgeClient.registerItemRenderer(gun.getItem(), new Wrap(r));
        }
    }

    /** The draw's pose now for this gun {x, y, z, pitch, yaw, roll}, or null when no draw plays. */
    private static float[] pose(ItemStack gun)
    {
        Style s;
        float t;
        if (testTime >= 0)
        {
            s = styleOf(gun.getItem());
            t = testTime;
        }
        else if (style != null)
        {
            s = style;
            t = elapsed();
        }
        else
        {
            return null;
        }
        MeleeMotion.curve(s.keys, Math.min(t, s.length()), pose);
        return pose;
    }

    /** A gun's renderer with the draw put in front of Decimation's first person drawing. */
    private static final class Wrap implements IItemRenderer
    {
        private final IItemRenderer real;

        Wrap(IItemRenderer real)
        {
            this.real = real;
        }

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type)
        {
            return real.handleRenderType(item, type);
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
        {
            return real.shouldUseRenderHelper(type, item, helper);
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data)
        {
            float[] p = type == ItemRenderType.EQUIPPED_FIRST_PERSON && item != null ? pose(item) : null;
            if (p == null)
            {
                real.renderItem(type, item, data);
                return;
            }
            GL11.glPushMatrix();
            // to the view's axes: Forge's sword transform and vanilla's 0.4 and 45 degrees undone (as MeleeRenderer)
            GL11.glTranslatef(0.9375f, 0.0625f, 0.0f);
            GL11.glRotatef(25.0f, 0, 0, 1);
            GL11.glRotatef(-50.0f, 0, 1, 0);
            GL11.glScalef(1 / 1.5f, 1 / 1.5f, 1 / 1.5f);
            GL11.glTranslatef(0.0f, 0.3f, 0.0f);
            GL11.glScalef(2.5f, 2.5f, 2.5f);
            GL11.glRotatef(-45f, 0, 1, 0);
            // the draw
            GL11.glTranslatef(p[0], p[1], p[2]);
            GL11.glRotatef(p[4], 0, 1, 0);
            GL11.glRotatef(p[3], 1, 0, 0);
            GL11.glRotatef(p[5], 0, 0, 1);
            // and back, for Decimation's own drawing
            GL11.glRotatef(45f, 0, 1, 0);
            GL11.glScalef(0.4f, 0.4f, 0.4f);
            GL11.glTranslatef(0.0f, -0.3f, 0.0f);
            GL11.glScalef(1.5f, 1.5f, 1.5f);
            GL11.glRotatef(50.0f, 0, 1, 0);
            GL11.glRotatef(-25.0f, 0, 0, 1);
            GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
            real.renderItem(type, item, data);
            GL11.glPopMatrix();
        }
    }
}
