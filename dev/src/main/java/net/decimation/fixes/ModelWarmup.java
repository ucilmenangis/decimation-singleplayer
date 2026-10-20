package net.decimation.fixes;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * NPC textures and gear models ready before their first draw (docs/performance.md 10, bug.md "First sight of armed
 * NPCs stalls a frame"). Minecraft reads and uploads a texture the first time it is bound, and Decimation loads a mask's,
 * vest's, backpack's or armour piece's model only when an NPC wearing it is first drawn. A city refuge's 22 residents
 * coming into view at once loaded about 45 textures in one frame (dev test refugevisit: 220 to 635 ms; with this 36 to
 * 86 ms). This does that work ahead, a slice at the end of each frame: the gear models (Deci.loadGearModel), the textures
 * NPCs are drawn with (TEXTURES) and HumanLayers' layer files. Model parts are not compiled ahead: Decimation's display
 * lists cost about 9 KB of driver memory a quad, all models 1.4 GB on the M4, and their first compile is the smaller part.
 */
@SideOnly(Side.CLIENT)
public final class ModelWarmup
{
    /** Off: -Ddeciworldgen.warmup=false (first draws load their own textures and models, as before). */
    public static boolean enabled = !"false".equals(System.getProperty("deciworldgen.warmup"));
    /** Time a frame gives the warm up: more in the menus (no world to draw), little in a world. */
    static final long MENU_NS = 8000000L, WORLD_NS = 2000000L;
    /** Texture folders of the models and mobs NPCs are drawn with (paths in Decimation's jar). */
    static final String[] TEXTURES = {"assets/deci/textures/model/armor/", "assets/deci/textures/model/backpacks/",
                                      "assets/deci/textures/model/bullets/", "assets/deci/textures/model/guns/",
                                      "assets/deci/textures/model/masks/", "assets/deci/textures/model/entities/"};
    private static final String DETAILS = "/details/";

    /** For tests and the log: gear models loaded, textures loaded, time spent (ns), all done. */
    public static long gear, textures, nanos;
    public static volatile boolean finished;

    private static List<Object> items;
    private static net.minecraft.client.model.ModelBiped human;
    private static List<String> pictures;
    private static int item, picture;

    private ModelWarmup()
    {
    }

    public static void install()
    {
        FMLCommonHandler.instance().bus().register(new ModelWarmup());
    }

    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e)
    {
        if (e.phase == TickEvent.Phase.END && enabled && !finished)
        {
            run(Minecraft.getMinecraft().theWorld == null ? MENU_NS : WORLD_NS);
        }
    }

    /** Works through what is left until ns are spent (client thread, GL context); true once everything is done. */
    public static boolean run(long ns)
    {
        long start = System.nanoTime(), end = start + ns;
        int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try
        {
            if (items == null)
            {
                items = new ArrayList<Object>();
                for (Object o : net.minecraft.item.Item.itemRegistry)
                {
                    items.add(o);
                }
                human = Deci.humanModel();
                pictures = new ArrayList<String>();
                File jar = Deci.decimationJar();
                if (jar != null)
                {
                    ZipFile z = new ZipFile(jar);
                    try
                    {
                        pictures = texturePaths(z);
                    }
                    finally
                    {
                        z.close();
                    }
                }
            }
            while (System.nanoTime() < end && item < items.size())
            {
                try
                {
                    gear += Deci.loadGearModel(items.get(item++), human) ? 1 : 0;
                }
                catch (RuntimeException ex)
                {
                    // that item's model loads at its first draw, as before
                }
            }
            Minecraft mc = Minecraft.getMinecraft();
            while (System.nanoTime() < end && picture < pictures.size())
            {
                String path = pictures.get(picture++);
                ResourceLocation r = new ResourceLocation("deci", path);
                if (mc.getTextureManager().getTexture(r) == null)
                {
                    mc.getTextureManager().bindTexture(r);
                    textures++;
                }
                if (path.contains(DETAILS))
                {
                    HumanLayers.warm(r.toString());          // the layers it composes NPC looks from
                }
            }
            if (item >= items.size() && picture >= pictures.size())
            {
                finished = true;
                FMLLog.info("[deciworldgen] warm up done: %d gear models, %d textures in %.0f ms", gear, textures,
                            (nanos + System.nanoTime() - start) / 1e6);
            }
        }
        catch (Exception ex)
        {
            finished = true;                          // whatever is left loads at its first draw, as before
            FMLLog.warning("[deciworldgen] warm up stopped: %s", ex);
        }
        finally
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
            nanos += System.nanoTime() - start;
        }
        return finished;
    }

    /** The pictures under TEXTURES in Decimation's jar, as paths in the deci domain ("textures/model/..."). */
    static List<String> texturePaths(ZipFile jar)
    {
        List<String> out = new ArrayList<String>();
        for (Enumeration<? extends ZipEntry> en = jar.entries(); en.hasMoreElements();)
        {
            String n = en.nextElement().getName();
            for (String t : TEXTURES)
            {
                if (n.startsWith(t) && n.endsWith(".png"))
                {
                    out.add(n.substring("assets/deci/".length()));
                    break;
                }
            }
        }
        return out;
    }
}
