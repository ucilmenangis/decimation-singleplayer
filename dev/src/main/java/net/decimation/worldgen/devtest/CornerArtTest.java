package net.decimation.worldgen.devtest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.decimation.fixes.NpcLoadouts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * The soldier of the main menu's corner art (docs/main_menu.md 2h; user, 21 Oktober 2026: "learn that design (FURY) and
 * after that we make our version"): one NPC of a tier (default elite_military: night vision goggles, a rifle) stands on
 * the arena; the client draws it large with the inventory's entity preview on a magenta background, reads the pixels and
 * saves the NPC alone with a clear background as run/client/devtest/corner_<tier>_<left|right>.png, turned both ways.
 * tools/menu_assets.py builds the corner from such a picture (copied to docs/shots/main_menu_v0.65.0/corner_src).
 * -Ptier=NAME picks another tier.
 */
public class CornerArtTest extends DevTestMode
{
    public String name() { return "cornerart"; }

    private static final int KEY = 0xFFFF00FF;
    private final String tier = System.getProperty("deciworldgen.autotest.tier", "elite_military");
    private volatile int step, id = -1;           // 0 arena (server), 1 wait, 2 draw left, 3 draw right, 4 done
    private int ticks;
    private static Draw draw;

    /** Draws after the frame (the game's framebuffer is still bound), one side per frame. */
    public static final class Draw
    {
        volatile CornerArtTest test;

        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e)
        {
            CornerArtTest t = test;
            if (e.phase == TickEvent.Phase.END && t != null && (t.step == 2 || t.step == 3))
            {
                t.drawNext(Minecraft.getMinecraft());
            }
        }
    }

    public boolean client(Minecraft mc)
    {
        if (draw == null)
        {
            draw = new Draw();
            FMLCommonHandler.instance().bus().register(draw);
        }
        draw.test = this;
        if (mc.thePlayer == null || step == 0)
        {
            return true;
        }
        if (step == 1 && ++ticks > 60 && mc.theWorld.getEntityByID(id) instanceof EntityLivingBase)
        {
            step = 2;                                  // gear and gun reached the client copy
        }
        if (step == 1 && ticks > 600)
        {
            DevTestResults.check(name(), "soldier on the client", "none after 30 s", false, "an NPC of tier " + tier);
            step = 4;
        }
        return step != 4;
    }

    private void drawNext(Minecraft mc)
    {
        Entity e = mc.theWorld == null ? null : mc.theWorld.getEntityByID(id);
        if (!(e instanceof EntityLivingBase))
        {
            return;
        }
        boolean left = step == 2;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int f = sr.getScaleFactor(), w = sr.getScaledWidth(), h = sr.getScaledHeight();
        int size = h * 2 / 5;                          // the NPC about 0.85 of the screen's height
        GL11.glDisable(GL11.GL_BLEND);
        Gui.drawRect(0, 0, w, h, KEY);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glColor4f(1, 1, 1, 1);
        GuiInventory.func_147046_a(w / 2, h - h / 14, size, left ? 40 : -40, -10, (EntityLivingBase) e);
        int pw = mc.displayWidth, ph = mc.displayHeight;
        ByteBuffer buf = BufferUtils.createByteBuffer(pw * ph * 4);
        GL11.glReadPixels(0, 0, pw, ph, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        BufferedImage im = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
        int drawn = 0;
        for (int y = 0; y < ph; y++)
        {
            for (int x = 0; x < pw; x++)
            {
                int i = ((ph - 1 - y) * pw + x) * 4;
                int r = buf.get(i) & 255, g = buf.get(i + 1) & 255, b = buf.get(i + 2) & 255;
                boolean key = r > 240 && g < 16 && b > 240;
                im.setRGB(x, y, key ? 0 : 0xFF000000 | r << 16 | g << 8 | b);
                drawn += key ? 0 : 1;
            }
        }
        File out = new File(mc.mcDataDir, "devtest/corner_" + tier + "_" + (left ? "left" : "right") + ".png");
        try
        {
            out.getParentFile().mkdirs();
            ImageIO.write(im, "png", out);
        }
        catch (Exception ex)
        {
            DevTestResults.value(name(), "save", ex.toString());
        }
        DevTestResults.check(name(), "soldier drawn " + (left ? "left" : "right"), drawn + " pixels, " + out.getName(), drawn > pw * ph / 50,
                             "the NPC on a clear background");
        step = left ? 3 : 4;
    }

    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || step != 0)
        {
            return;
        }
        w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        DevTestArena.build(w, p);
        p.setGameType(WorldSettings.GameType.CREATIVE);   // Decimation's NPCs leave a creative player alone
        p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 90, 10);
        for (int tries = 0; tries < 5 && id < 0; tries++)
        {
            EntityLiving npc = Deci.newSoviet(w);
            NpcLoadouts.instance().equipTier(npc, tier);
            npc.setLocationAndAngles(DevTestArena.X - 8 + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 270, 0);
            if (w.spawnEntityInWorld(npc))
            {
                id = npc.getEntityId();
            }
        }
        DevTestResults.value(name(), "tier", tier + (id < 0 ? ": not spawned" : ""));
        step = id < 0 ? 4 : 1;
    }
}
