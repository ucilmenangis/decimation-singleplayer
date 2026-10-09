package net.decimation.worldgen.devtest;

import java.util.Map;

import net.decimation.mod.common.entity.blockentities.props.TileEntityProp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

/**
 * City view fps, props on / off (user report 10 Oktober 2026: 70 to 90 fps outside the city,
 * 40 to 55 looking at it from the test arena; docs/performance.md section 5). The camera stands
 * where the user stood (x 3, y 151, z -9, looking south over the seed 1 city, -Pview=x,y,z,yaw,pitch
 * to change it). Phases alternate: props drawn by Decimation's renderer, props not drawn (their
 * renderer swapped for one that draws nothing), three times each, so a slow drift cancels out.
 * Also counts the props in the render chunks. Results "fps props on N" / "off N", "props in render chunks".
 */
public class CityViewTest extends DevTestMode
{
    public String name() { return "cityview"; }

    private static final int START = 60, PHASE = 120, SETTLE = 50;
    // alternating, so the slow fps drift (the machine heating up) cancels out in the pairs
    private static final String[] PHASES = {"props on 1", "props off 1", "props on 2", "props off 2", "props on 3", "props off 3"};
    private final String[] view = System.getProperty("deciworldgen.autotest.view", "3,151,-9,0,5").split(",");
    private volatile int ticks;
    private final long[] sum = new long[PHASES.length];
    private final int[] n = new int[PHASES.length];
    private int last = -1;
    private TileEntitySpecialRenderer original;

    private static final TileEntitySpecialRenderer NOTHING = new TileEntitySpecialRenderer()
    {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float f)
        {
        }
    };

    private void propRenderer(boolean on)
    {
        Map<Class<? extends TileEntity>, TileEntitySpecialRenderer> map = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
        if (original == null)
        {
            original = map.get(TileEntityProp.class);
        }
        map.put(TileEntityProp.class, on ? original : NOTHING);
    }

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null)
        {
            return true;
        }
        int t = ticks;
        DevTestUtil.unlimitedFps(mc);
        mc.gameSettings.hideGUI = false;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = Float.parseFloat(view[3]);
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = Float.parseFloat(view[4]);
        if (t < START)
        {
            return true;
        }
        int ph = (t - START) / PHASE, u = (t - START) % PHASE;
        if (ph >= PHASES.length)
        {
            propRenderer(true);
            for (int i = 0; i < PHASES.length; i++)
            {
                DevTestResults.value(name(), "fps " + PHASES[i], Math.round(sum[i] / (double) Math.max(1, n[i])));
            }
            // props do not tick (not in loadedTileEntityList): count the renderer's tile entities
            int props = 0, all = 0;
            for (Object o : mc.renderGlobal.tileEntities)
            {
                all++;
                if (o instanceof TileEntityProp)
                {
                    props++;
                }
            }
            DevTestResults.value(name(), "props in render chunks", props);
            DevTestResults.value(name(), "tile entities in render chunks", all);
            return false;
        }
        propRenderer(ph % 2 == 0);
        if (u >= SETTLE && u / 10 != last)
        {
            last = u / 10;
            String dbg = mc.debug;
            sum[ph] += Integer.parseInt(dbg.substring(0, dbg.indexOf(' ')));
            n[ph]++;
        }
        if (u == PHASE - 5)
        {
            DevTestUtil.screenshot(mc, name(), "cityview_" + PHASES[ph].replace(' ', '_'));
        }
        return true;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        if (t == 5)
        {
            p.capabilities.isFlying = true;
            p.worldObj.setWorldTime(6000);
            p.worldObj.getWorldInfo().setRaining(false);
            p.worldObj.getWorldInfo().setThundering(false);
        }
        p.playerNetServerHandler.setPlayerLocation(Double.parseDouble(view[0]), Double.parseDouble(view[1]),
                                                   Double.parseDouble(view[2]), Float.parseFloat(view[3]),
                                                   Float.parseFloat(view[4]));
    }
}
