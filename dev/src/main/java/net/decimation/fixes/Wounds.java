package net.decimation.fixes;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

/**
 * Blood that stays on a hit infected (user 22 Oktober 2026: "when they got hit it looks like yellowish tint splatter, but
 * nothing gets stick onto their character"). Decimation only throws its green-yellow infected blood particles off a hit
 * (ClientEventHandler.onLivingHurt); the body never changes. Every hit now paints a splat into that infected's own copy of
 * its look picture (HumanLayers makes the picture; renderHuman binds ours instead), so the blood sits on the model and
 * follows every swing of an arm or leg, at no extra draw.
 * A hit is seen on the client as the hurt flash starting (hurtTime rising: the server's entity status 2), so it works in
 * singleplayer and on a server, for any attacker. Where: when the local player's view ray crosses the infected's box, the
 * spot that ray hits (head, body, an arm or a leg, front, back or side, the place on that face); otherwise a random spot
 * (body most often). Each splat stays inside its face of the picture (a face is one island of the 64 x 32 layout, so
 * blood never shows up on another part), only over pixels the model shows, dark red, a few drips running down.
 * At most {@link #MAX} splats an infected (the oldest goes), {@link #SLOTS} wounded infected pictures at once (the one
 * longest not drawn is reused), {@link #PER_FRAME} pictures made a frame. Infected without a picture of their look
 * (HumanLayers' old way) show no splats.
 */
@SideOnly(Side.CLIENT)
public final class Wounds
{
    static final int MAX = 10;
    static final int SLOTS = 64;
    static final int PER_FRAME = 4;
    /** Off: no splats (-Ddeciworldgen.wounds=false). */
    public static boolean enabled = !"false".equals(System.getProperty("deciworldgen.wounds"));
    /** For tests: splats painted (hits seen), pictures made, time spent here (watching hits and making pictures). */
    public static long hits, built, nanos;

    /**
     * Faces of the 64 x 32 layout (x0, y0, x1, y1) by part and side {front, back, right, left}: ModelBox lays a box at
     * (u, v) of size w, h, d out as right side (u, v + d, d x h), front (u + d, v + d, w x h), left (u + d + w, ...),
     * back (u + d + w + d, ...). Head 8x8x8 at 0,0; body 8x12x4 at 16,16; arms 4x12x4 at 40,16 (the left one mirrored);
     * legs ModelHuman's 4x6x4 at 0,16 with a 4x6x4 at 0,22 below, taken as one 12 high face.
     */
    private static final int HEAD = 0, BODY = 1, ARM = 2, LEG = 3;
    private static final int FRONT = 0, BACK = 1, RIGHT = 2, LEFT = 3;
    private static final int[][][] FACES = {
        {{8, 8, 16, 16}, {24, 8, 32, 16}, {0, 8, 8, 16}, {16, 8, 24, 16}},
        {{20, 20, 28, 32}, {32, 20, 40, 32}, {16, 20, 20, 32}, {28, 20, 32, 32}},
        {{44, 20, 48, 32}, {52, 20, 56, 32}, {40, 20, 44, 32}, {48, 20, 52, 32}},
        {{4, 20, 8, 32}, {12, 20, 16, 32}, {0, 20, 4, 32}, {8, 20, 12, 32}},
    };

    /** By the entity itself: a lookup allocates nothing, and an unloaded infected leaves with its entity. */
    private static final Map<EntityLivingBase, Body> bodies = new WeakHashMap<EntityLivingBase, Body>();
    private static final Pic[] slots = new Pic[SLOTS];
    private static final Random random = new Random();
    private static long frame;
    private static int made;

    private Wounds()
    {
    }

    /**
     * HumanLayers.begin, for an NPC drawn from the picture `look` (w x h, ARGB) of look number `key`: the picture to bind
     * instead when this infected carries splats, else null. Notes a new hit first.
     */
    static ResourceLocation picture(Object entity, boolean infected, long key, int[] look, int w, int h)
    {
        if (!enabled || !infected || !(entity instanceof EntityLivingBase))
        {
            return null;
        }
        long t0 = System.nanoTime();
        EntityLivingBase e = (EntityLivingBase) entity;
        Body b = bodies.get(e);
        if (b == null)
        {
            b = new Body();
            bodies.put(e, b);
        }
        if (e.hurtTime > b.lastHurt)
        {
            b.add(hitSpot(e, b.count));
            hits++;
        }
        b.lastHurt = e.hurtTime;
        ResourceLocation out = null;
        if (b.count > 0)
        {
            if (b.pic != null && b.pic.owner == b && b.pic.key == key && b.pic.count == b.count)
            {
                out = b.pic.location;
            }
            else if (made >= PER_FRAME)
            {
                out = b.pic != null && b.pic.owner == b ? b.pic.location : null; // the older splats until next frame
            }
            else
            {
                Pic p = b.pic != null && b.pic.owner == b ? b.pic : free();
                if (p != null)
                {
                    made++;
                    p.show(b, key, paint(look, w, h, b), w, h);
                    built++;
                    out = p.location;
                }
            }
            if (out != null)
            {
                b.pic.used = frame;
            }
        }
        nanos += System.nanoTime() - t0;
        return out;
    }

    /** Once a frame (HumanLayers.onFrame). */
    static void frame()
    {
        frame++;
        made = 0;
    }

    /** A resource pack change: every picture made again. */
    static void reload()
    {
        for (Pic p : slots)
        {
            if (p != null)
            {
                p.key = -1;
            }
        }
    }

    /** The splats of one infected as {part, side} pairs, oldest first (dev test wounds: 0 head 1 body 2 arm 3 leg; 0 front 1 back 2 right 3 left). */
    public static int[][] spots(int entityId)
    {
        Body b = null;
        for (Map.Entry<EntityLivingBase, Body> en : bodies.entrySet())
        {
            if (en.getKey().getEntityId() == entityId)
            {
                b = en.getValue();
            }
        }
        int n = b == null ? 0 : b.count;
        int[][] out = new int[n][];
        for (int i = 0; i < n; i++)
        {
            float[] s = b.spots[(b.next - n + i + MAX) % MAX];
            out[i] = new int[] {(int) s[0], (int) s[1]};
        }
        return out;
    }

    /**
     * The splat of a new hit: {part, side, across 0..1 (left to right as the face is seen), down 0..1, seed}. From the
     * local player's view ray when it crosses the box, else random.
     */
    private static float[] hitSpot(EntityLivingBase e, int n)
    {
        EntityPlayer pl = Minecraft.getMinecraft().thePlayer;
        float seed = random.nextFloat();
        if (pl != null && pl.getDistanceSqToEntity(e) < 128 * 128)
        {
            Vec3 eye = pl.getPosition(1.0f);              // the eyes, as Minecraft's own view ray (posY is eye level here)
            Vec3 look = pl.getLook(1.0f);
            Vec3 far = eye.addVector(look.xCoord * 128, look.yCoord * 128, look.zCoord * 128);
            AxisAlignedBB box = e.boundingBox.expand(0.15, 0.05, 0.15);
            MovingObjectPosition m = box.calculateIntercept(eye, far);
            if (m != null && m.hitVec != null)
            {
                return spotAt(e, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord, seed);
            }
        }
        float r = random.nextFloat();
        int part = r < 0.5f ? BODY : r < 0.7f ? HEAD : r < 0.85f ? ARM : LEG;
        float s = random.nextFloat();
        int side = s < 0.6f ? FRONT : s < 0.85f ? BACK : s < 0.93f ? RIGHT : LEFT;
        return new float[] {part, side, 0.2f + random.nextFloat() * 0.6f, 0.15f + random.nextFloat() * 0.6f, seed};
    }

    /** A world point on (or near) the infected's box as {part, side, across, down, seed} in its model's terms. */
    static float[] spotAt(EntityLivingBase e, double x, double y, double z, float seed)
    {
        double yaw = Math.toRadians(e.renderYawOffset);
        double dx = x - e.posX, dz = z - e.posZ;
        double forward = -dx * Math.sin(yaw) + dz * Math.cos(yaw); // + in front of the body
        double side = dx * Math.cos(yaw) + dz * Math.sin(yaw);     // + on the body's left
        double up = (y - e.boundingBox.minY) / Math.max(0.1, e.boundingBox.maxY - e.boundingBox.minY);
        double fromTop = Math.max(0, Math.min(0.999, 1 - up)) * 32; // model pixels from the top of the head
        boolean frontBack = Math.abs(forward) >= Math.abs(side);
        int part;
        double down;
        if (fromTop < 8)
        {
            part = HEAD;
            down = fromTop / 8;
        }
        else if (fromTop < 20)
        {
            part = Math.abs(side) > 0.22 ? ARM : BODY;
            down = (fromTop - 8) / 12;
        }
        else
        {
            part = LEG;
            down = (fromTop - 20) / 12;
        }
        int face = frontBack ? (forward >= 0 ? FRONT : BACK) : (side >= 0 ? LEFT : RIGHT);
        double half = part == HEAD ? 0.25 : part == BODY ? 0.25 : 0.12;
        double centre = part == ARM ? (side >= 0 ? 0.37 : -0.37) : part == LEG ? (side >= 0 ? 0.12 : -0.12) : 0;
        double across;
        if (frontBack)
        {
            // seen from the front the body's right is on the viewer's left; from the back its left is
            double a = (side - centre) / (2 * half) + 0.5;
            across = face == FRONT ? 1 - a : a;
        }
        else
        {
            double a = forward / 0.5 + 0.5;
            across = face == LEFT ? 1 - a : a;
        }
        return new float[] {part, face, (float) Math.max(0.1, Math.min(0.9, across)), (float) Math.max(0.08, Math.min(0.92, down)), seed};
    }

    /** The look with each of the body's splats painted on, clipped to its face and to pixels the model shows. */
    static int[] paint(int[] look, int w, int h, Body b)
    {
        int[] out = look.clone();
        int k = w / 64;
        for (int i = 0; i < b.count; i++)
        {
            float[] s = b.spots[i];
            int[] f = FACES[(int) s[0]][(int) s[1]];
            int x0 = f[0] * k, y0 = f[1] * k, x1 = f[2] * k, y1 = f[3] * k;
            Random r = new Random((long) (s[4] * Integer.MAX_VALUE));
            double cx = x0 + s[2] * (x1 - x0), cy = y0 + s[3] * (y1 - y0);
            double radius = (1.3 + r.nextFloat() * 1.1) * k;
            for (int y = y0; y < y1; y++)
            {
                for (int x = x0; x < x1; x++)
                {
                    double d = Math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.15) / radius;
                    double edge = 0.75 + r.nextFloat() * 0.5;     // ragged rim
                    if (d < edge)
                    {
                        // a dark wound in a wet red rim: reads on pale skin and on black uniforms alike
                        stain(out, y * w + x, d < 0.4 ? 0x5C0707 : 0xA81414, 0.95f);
                    }
                }
            }
            int specks = 2 + r.nextInt(4);                        // spray around the wound
            for (int j = 0; j < specks; j++)
            {
                double a = r.nextDouble() * Math.PI * 2, d = radius * (1.3 + r.nextDouble());
                int x = (int) Math.floor(cx + Math.cos(a) * d), y = (int) Math.floor(cy + Math.sin(a) * d);
                if (x >= x0 && x < x1 && y >= y0 && y < y1)
                {
                    stain(out, y * w + x, 0xB51818, 0.9f);
                }
            }
            int drips = 1 + r.nextInt(3);
            for (int j = 0; j < drips; j++)
            {
                int x = (int) Math.floor(cx + (r.nextFloat() - 0.5) * radius * 1.4);
                int length = (1 + r.nextInt(4)) * k;
                for (int y = (int) Math.floor(cy); y < Math.min(y1, cy + radius * 0.5 + length); y++)
                {
                    if (x >= x0 && x < x1 && y >= y0)
                    {
                        stain(out, y * w + x, 0x8E0E0E, 0.9f);
                    }
                }
            }
        }
        return out;
    }

    /** Lays dark red over one pixel by `alpha`, only where the model shows it (alpha test 0.1, as HumanLayers). */
    private static void stain(int[] px, int i, int rgb, float alpha)
    {
        int p = px[i];
        if (p >>> 24 <= 25)
        {
            return;
        }
        int r = (int) (((p >> 16) & 255) * (1 - alpha) + ((rgb >> 16) & 255) * alpha);
        int g = (int) (((p >> 8) & 255) * (1 - alpha) + ((rgb >> 8) & 255) * alpha);
        int bl = (int) ((p & 255) * (1 - alpha) + (rgb & 255) * alpha);
        px[i] = (p & 0xFF000000) | r << 16 | g << 8 | bl;
    }

    /** A free picture slot, or the one longest not drawn (never one drawn this frame or the last). */
    private static Pic free()
    {
        Pic pick = null;
        for (int i = 0; i < SLOTS; i++)
        {
            if (slots[i] == null)
            {
                slots[i] = new Pic(i);
            }
            Pic p = slots[i];
            if (p.owner == null)
            {
                return p;
            }
            if (p.used < frame - 1 && (pick == null || p.used < pick.used))
            {
                pick = p;
            }
        }
        return pick;
    }

    /** One infected's splats and hit watch. */
    static final class Body
    {
        final float[][] spots = new float[MAX][];
        int count, next, lastHurt;
        Pic pic;

        void add(float[] spot)
        {
            spots[next] = spot;
            next = (next + 1) % MAX;
            count = Math.min(MAX, count + 1);
            if (pic != null)
            {
                pic.count = -1;                        // made again
            }
        }
    }

    /** One wounded picture on the graphics card under a fixed name (TextureManager keeps names: names are reused). */
    private static final class Pic
    {
        final ResourceLocation location;
        DynamicTexture texture;
        Body owner;
        long key = -1, used = Long.MIN_VALUE;
        int count, w, h;

        Pic(int i)
        {
            location = new ResourceLocation("deciworldgen", "wounds/" + i);
        }

        void show(Body b, long key, int[] argb, int pw, int ph)
        {
            if (owner != null && owner != b)
            {
                owner.pic = null;
            }
            owner = b;
            b.pic = this;
            this.key = key;
            count = b.count;
            if (texture == null || w != pw || h != ph)
            {
                if (texture != null)
                {
                    TextureUtil.deleteTexture(texture.getGlTextureId());
                }
                texture = new DynamicTexture(pw, ph);
                w = pw;
                h = ph;
                Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
            }
            System.arraycopy(argb, 0, texture.getTextureData(), 0, argb.length);
            texture.updateDynamicTexture();
        }
    }
}
