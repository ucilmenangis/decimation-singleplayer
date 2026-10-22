package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * NPC looks drawn in one go (user 19 Oktober 2026: "from 7 to 1"; bug.md "NPC drawing heavy"). Decimation's ModelHuman
 * (deci.q.h) draws every human NPC and infected in seven layers: the body picture on the model, then eyes, hair, beard,
 * shirt, pants and shoes, each bound and drawn again on one of two copies of the model 0.05 larger (7 texture binds and
 * 19 part draws per NPC and frame, both copies posed again every frame). Every layer uses the same boxes at the same
 * places of a 64 x 32 picture, alpha tested at 0.1 without blending, so one picture of the layers drawn once on the model
 * shows the same pixels. PatchHumanLayers: renderHuman calls {@link #begin} first; when the picture of this look is
 * ready, the copies are not posed, {@link #bind} binds it instead of the body, and the six layer binds and their draws
 * are skipped. Anything unusual (a missing or odd sized layer file, more looks on screen than {@link #SLOTS}, an
 * invisible NPC, more than {@link #PER_FRAME} new looks in one frame) is drawn the old way.
 *
 * Kept as ModelHuman draws: the file names and the order (body, eyes unless blinking, hair and beard unless infected,
 * shirt, pants and shoes unless frail), each layer only on the parts its passes draw ({@link #PLACES}: layer files hold
 * pixels elsewhere too, hair 663, shirt 105, beard 72), a layer pixel shows when its alpha is above 0.1 (every pixel of
 * the 91 files is 0 or 255). What changes: the copies were 0.05 / 16 of a block larger, so at an edge a layer could show
 * one screen pixel wider; and the hair copy never swung (its swing progress was never set), so in a melee swing hair on
 * the back no longer stays behind.
 */
@SideOnly(Side.CLIENT)
public final class HumanLayers implements IResourceManagerReloadListener
{
    /** Layers in ModelHuman's drawing order over the body. */
    static final int EYES = 0, HAIR = 1, BEARD = 2, SHIRT = 3, PANTS = 4, SHOES = 5;
    /** look[7] (Deci.humanLook). */
    static final int BLINKING = 1, INFECTED = 2, FRAIL = 4;
    /**
     * Places in a 64 x 32 picture (x0, y0, x1, y1) of the boxes each layer's passes draw: the head (renderHead and
     * renderHeadLayer: bipedHead 8x8x8 at 0,0), body and arms (renderBodyAndArms: bipedBody 8x12x4 at 16,16, arms 4x12x4
     * at 40,16), legs (renderLegs: 4x6x4 at 0,16 with a 4x6x4 at 0,22 below).
     */
    private static final int[] HEAD = {0, 0, 32, 16}, BODY_ARMS = {16, 16, 56, 32}, LEGS = {0, 16, 16, 32};
    static final int[][][] PLACES = {{HEAD}, {HEAD, BODY_ARMS}, {HEAD}, {BODY_ARMS}, {LEGS}, {LEGS}};
    /** Pictures kept on the graphics card (8 KB each): more looks than this on screen at once are drawn the old way. */
    static final int SLOTS = 256;
    /** New pictures per frame at most: a horde coming into view does not stall one frame. */
    static final int PER_FRAME = 4;
    private static final String MOB = "deci:textures/model/entities/mob/";

    /** True while renderHuman draws the NPC begin() was called for with its picture (read by the patched code). */
    public static boolean active;
    /** Off: every NPC drawn the old way (-Ddeciworldgen.humanlayers=false; the dev test switches it). */
    public static boolean enabled = !"false".equals(System.getProperty("deciworldgen.humanlayers"));
    /**
     * renderHuman's three line of sight checks a frame (the model and both copies: up to nine ray traces each) feed only
     * HumanEntity.visibleTicks, which nothing reads; PatchHumanLayers answers them true. On: they run as before (the dev
     * test bench turns it on with layers=off, to measure the old drawing).
     */
    public static boolean sightChecks;
    /** For tests: pictures made, NPC draws with a picture, NPC draws the old way, layer files read, time making pictures. */
    public static long built, drawn, oldWay, filesRead, makeNanos;

    private static final int[] look = new int[8];
    private static final Map<String, Integer> folders = new HashMap<String, Integer>();
    private static final Map<Long, Slot> byLook = new HashMap<Long, Slot>();
    /** Looks that cannot be made (a layer file missing or of another size): drawn the old way, never tried again. */
    private static final Slot BROKEN = new Slot(-1);
    private static final Map<String, Picture> files = new HashMap<String, Picture>();
    private static final Picture MISSING = new Picture(0, 0, null);
    private static final Slot[] slots = new Slot[SLOTS];
    private static Slot current;
    /** This NPC's own picture with its blood splats (Wounds), bound instead of the look's; null for none. */
    private static ResourceLocation wounded;
    private static long frame;
    private static int made;

    public static void install()
    {
        HumanLayers h = new HumanLayers();
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(h);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(h);
    }

    /** ModelHuman.renderHuman, before anything else (not for its two copies). */
    public static void begin(Object entity)
    {
        active = false;
        current = null;
        wounded = null;
        if (!enabled)
        {
            return;
        }
        String folder = Deci.humanLook(entity, look);
        if (folder == null || ((Entity) entity).isInvisible())
        {
            oldWay++;                                 // invisible NPCs are drawn see-through, layer on layer
            return;
        }
        Integer f = folders.get(folder);
        if (f == null)
        {
            f = folders.size();
            folders.put(folder, f);
        }
        long key = pack(f, drawn(look));
        Slot s = key < 0 ? BROKEN : byLook.get(key);
        if (s == null)
        {
            s = make(key, folder);
        }
        if (s == null || s == BROKEN)
        {
            oldWay++;
            return;
        }
        s.used = frame;
        current = s;
        wounded = Wounds.picture(entity, (look[7] & INFECTED) != 0, key, s.argb, s.w, s.h);
        active = true;
        drawn++;
    }

    /** ModelHuman.renderHuman, in place of binding the body picture while {@link #active}. */
    public static void bind()
    {
        if (current != null)
        {
            Minecraft.getMinecraft().getTextureManager().bindTexture(wounded != null ? wounded : current.location);
        }
    }

    /** The look as one number: folder (0 .. 15), seven values (0 .. 255), flags; -1 when something does not fit. */
    static long pack(int folder, int[] look)
    {
        if (folder < 0 || folder > 15 || look[7] < 0 || look[7] > 7)
        {
            return -1;
        }
        long k = folder;
        for (int i = 0; i < 7; i++)
        {
            if (look[i] < 0 || look[i] > 255)
            {
                return -1;
            }
            k = k << 8 | look[i];
        }
        return k << 3 | look[7];
    }

    /** Values of layers that are not drawn set to 0, so looks that draw the same share one picture. */
    private static int[] drawn(int[] look)
    {
        if ((look[7] & BLINKING) != 0)
        {
            look[3] = 0;
        }
        if ((look[7] & INFECTED) != 0)
        {
            look[1] = look[2] = 0;
        }
        if ((look[7] & FRAIL) != 0)
        {
            look[4] = look[5] = look[6] = 0;
        }
        return look;
    }

    /** The body with each layer (null: not drawn) laid over it where its passes draw it and its alpha passes 0.1. */
    static int[] composite(int[] body, int[][] layers, int w, int h)
    {
        int[] out = body.clone();
        for (int k = 0; k < PLACES.length; k++)
        {
            int[] layer = layers[k];
            if (layer == null)
            {
                continue;
            }
            for (int[] r : PLACES[k])
            {
                int x0 = r[0] * w / 64, y0 = r[1] * h / 32, x1 = r[2] * w / 64, y1 = r[3] * h / 32;
                for (int y = y0; y < y1; y++)
                {
                    for (int x = x0; x < x1; x++)
                    {
                        int p = layer[y * w + x];
                        if (p >>> 24 > 25) // alpha / 255 > 0.1: GL_GREATER 0.1, Minecraft's entity alpha test
                        {
                            out[y * w + x] = p;
                        }
                    }
                }
            }
        }
        return out;
    }

    /** The picture of this look in a free slot (or the one longest not drawn), or null to draw the old way now. */
    private static Slot make(long key, String folder)
    {
        if (made >= PER_FRAME)
        {
            return null;
        }
        Slot pick = null;
        for (int i = 0; i < SLOTS && (pick == null || pick.key >= 0); i++)
        {
            Slot s = slots[i];
            if (s == null)
            {
                s = slots[i] = new Slot(i);
            }
            if (s.used < frame - 1 && (pick == null || s.key < 0 || s.used < pick.used))
            {
                pick = s;                             // never a picture drawn this frame or the last
            }
        }
        if (pick == null)
        {
            return null;                              // more looks on screen than slots
        }
        made++;
        long t0 = System.nanoTime();
        Picture p;
        try
        {
            p = picture(folder);
        }
        catch (RuntimeException e)
        {
            p = null;
        }
        makeNanos += System.nanoTime() - t0;
        if (p == null)
        {
            byLook.put(key, BROKEN);
            return BROKEN;
        }
        if (pick.key >= 0)
        {
            byLook.remove(pick.key);
        }
        long t1 = System.nanoTime();
        pick.show(p);
        makeNanos += System.nanoTime() - t1;
        pick.key = key;
        byLook.put(key, pick);
        built++;
        return pick;
    }

    /** The layers of the look in {@link #look} from the files ModelHuman binds, or null if one is missing or odd. */
    private static Picture picture(String folder)
    {
        Picture body = file(MOB + folder + "/details/body_" + look[0] + ".png");
        if (body == null)
        {
            return null;
        }
        String[] names = new String[PLACES.length];
        if ((look[7] & BLINKING) == 0)
        {
            names[EYES] = MOB + folder + "/details/eyes_" + look[3] + ".png";
        }
        if ((look[7] & INFECTED) == 0)
        {
            names[HAIR] = MOB + "human/details/hair_" + look[1] + ".png";
            names[BEARD] = MOB + folder + "/details/beard_" + look[2] + ".png";
        }
        if ((look[7] & FRAIL) == 0)
        {
            names[SHIRT] = MOB + "human/details/shirt_" + look[4] + ".png";
            names[PANTS] = MOB + "human/details/pants_" + look[5] + ".png";
            names[SHOES] = MOB + "human/details/shoes_" + look[6] + ".png";
        }
        int[][] layers = new int[PLACES.length][];
        for (int k = 0; k < names.length; k++)
        {
            if (names[k] != null)
            {
                Picture l = file(names[k]);
                if (l == null || l.w != body.w || l.h != body.h)
                {
                    return null;
                }
                layers[k] = l.argb;
            }
        }
        return new Picture(body.w, body.h, composite(body.argb, layers, body.w, body.h));
    }

    /** Decodes a layer file ahead of its first look (fixes/ModelWarmup; name as "deci:textures/model/..."). */
    public static void warm(String name)
    {
        file(name);
    }

    /** A layer file decoded once (91 in the jar, 8 KB each), null if missing (the old way shows the missing texture). */
    private static Picture file(String name)
    {
        Picture p = files.get(name);
        if (p == null)
        {
            p = MISSING;
            filesRead++;
            InputStream in = null;
            try
            {
                in = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(name)).getInputStream();
                BufferedImage img = ImageIO.read(in);   // as SimpleTexture reads them
                int w = img.getWidth(), h = img.getHeight();
                if (w % 64 == 0 && h % 32 == 0 && w * 32 == h * 64)
                {
                    p = new Picture(w, h, img.getRGB(0, 0, w, h, null, 0, w));
                }
            }
            catch (Exception e)
            {
                // missing or unreadable: this look is drawn the old way
            }
            finally
            {
                org.apache.commons.io.IOUtils.closeQuietly(in);
            }
            files.put(name, p);
        }
        return p == MISSING ? null : p;
    }

    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e)
    {
        if (e.phase == TickEvent.Phase.START)
        {
            frame++;
            made = 0;
            Wounds.frame();
        }
    }

    /** A resource pack change: layer files read again, every picture made again (the slots stay). */
    @Override
    public void onResourceManagerReload(IResourceManager manager)
    {
        files.clear();
        byLook.clear();
        for (Slot s : slots)
        {
            if (s != null)
            {
                s.key = -1;
            }
        }
        Wounds.reload();
    }

    private static final class Picture
    {
        final int w, h;
        final int[] argb;

        Picture(int w, int h, int[] argb)
        {
            this.w = w;
            this.h = h;
            this.argb = argb;
        }
    }

    /** One picture on the graphics card under a fixed name (TextureManager never forgets a name: names are reused). */
    private static final class Slot
    {
        final ResourceLocation location;
        DynamicTexture texture;
        int w, h;
        long key = -1, used = Long.MIN_VALUE;
        /** The picture shown (Wounds paints its splats on a copy). */
        int[] argb;

        Slot(int i)
        {
            location = new ResourceLocation("deciworldgen", "human_layers/" + i);
        }

        void show(Picture p)
        {
            if (texture == null || w != p.w || h != p.h)
            {
                if (texture != null)
                {
                    TextureUtil.deleteTexture(texture.getGlTextureId());
                }
                texture = new DynamicTexture(p.w, p.h);
                w = p.w;
                h = p.h;
                Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
            }
            System.arraycopy(p.argb, 0, texture.getTextureData(), 0, p.argb.length);
            texture.updateDynamicTexture();
            argb = p.argb;
        }
    }
}
