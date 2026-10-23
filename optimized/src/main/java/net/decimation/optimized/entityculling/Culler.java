package net.decimation.optimized.entityculling;

import java.lang.reflect.Method;

import net.decimation.optimized.OptimizedConfig;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * EntityCulling (tr7zw's, in Fabulously Optimized) for 1.7.10: a thread checks, for every loaded entity and every block
 * entity near the camera, whether any of nine points of its box (centre and corners) can be seen from the camera along a
 * straight line through the block grid; one that cannot is not drawn (EntityRenderDispatcherMixin,
 * BlockEntityRenderDispatcherMixin). Only full opaque blocks (Block.isOpaqueCube, yarn hasTransparency) stop a line.
 * Decimation's cities hold thousands of prop block entities and NPCs mostly hidden by buildings.
 *
 * The render thread hands over a snapshot once per frame (when the last one was used); the thread reads the client
 * world's blocks while the game may change them, as the original does: a read that fails counts as visible.
 * Never culled: the camera entity and what it rides, anything within 2 blocks of the camera, block entities with an
 * infinite render box, and everything in third person (the camera is not at the eye the lines start from).
 */
public final class Culler implements Runnable
{
    private static final class Snapshot
    {
        World world;
        Entity camera;
        double x, y, z;
        Object[] entities, blockEntities;
        boolean thirdPerson;
    }

    private static volatile Snapshot next;
    private static volatile boolean taken = true;
    private static Thread thread;
    private static Method renderBox;
    private static final int BLOCK_ENTITY_DISTANCE = OptimizedConfig.number("entityculling.blockEntityDistance", 64);

    /** Lines checked and objects culled in the last pass (dev check). */
    public static volatile int checked, culled;

    private Culler()
    {
    }

    /** Render thread, once a frame (WorldRenderer.renderEntities): the next snapshot for the thread. */
    public static void frame(Entity camera)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (thread == null)
        {
            thread = new Thread(new Culler(), "DeciRemnant entity culling");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.start();
        }
        if (!taken || mc.world == null)
        {
            return;
        }
        Snapshot s = new Snapshot();
        s.world = mc.world;
        s.camera = camera;
        s.x = BlockEntityRenderDispatcher.CAMERA_X;
        s.y = BlockEntityRenderDispatcher.CAMERA_Y;
        s.z = BlockEntityRenderDispatcher.CAMERA_Z;
        s.entities = mc.world.loadedEntities.toArray();
        s.blockEntities = mc.world.blockEntities.toArray();
        s.thirdPerson = mc.options.perspective != 0;
        taken = false;
        next = s;
    }

    @Override
    public void run()
    {
        while (true)
        {
            Snapshot s = next;
            if (s != null && !taken)
            {
                taken = true;
                pass(s);
            }
            try
            {
                Thread.sleep(4);
            }
            catch (InterruptedException ex)
            {
                return;
            }
        }
    }

    private static void pass(Snapshot s)
    {
        int lines = 0, hidden = 0;
        for (Object o : s.entities)
        {
            Entity e = (Entity) o;
            boolean cull = !s.thirdPerson && e != s.camera && e != s.camera.vehicle && e.boundingBox != null
                && !visible(s, e.boundingBox);
            ((Cullable) e).deciSetCulled(cull);
            lines++;
            hidden += cull ? 1 : 0;
        }
        double far = (double) BLOCK_ENTITY_DISTANCE * BLOCK_ENTITY_DISTANCE;
        for (Object o : s.blockEntities)
        {
            BlockEntity be = (BlockEntity) o;
            double dx = be.x + 0.5 - s.x, dy = be.y + 0.5 - s.y, dz = be.z + 0.5 - s.z;
            boolean cull = false;
            if (!s.thirdPerson && dx * dx + dy * dy + dz * dz < far)
            {
                Box box = renderBox(be);
                cull = box != null && !Double.isInfinite(box.maxX) && !visible(s, box);
                lines++;
            }
            ((Cullable) be).deciSetCulled(cull);
            hidden += cull ? 1 : 0;
        }
        checked = lines;
        culled = hidden;
    }

    /** Forge's TileEntity.getRenderBoundingBox (added by Forge, so not in the 1.7.10 names this compiles against). */
    private static Box renderBox(BlockEntity be)
    {
        try
        {
            if (renderBox == null)
            {
                renderBox = BlockEntity.class.getMethod("getRenderBoundingBox");
            }
            return (Box) renderBox.invoke(be);
        }
        catch (ReflectiveOperationException | RuntimeException ex)
        {
            return null;   // no box: drawn as before
        }
    }

    /** Any of the box's centre and corners (pulled 0.05 inside) seen from the camera. */
    private static boolean visible(Snapshot s, Box b)
    {
        if (s.x > b.minX - 2 && s.x < b.maxX + 2 && s.y > b.minY - 2 && s.y < b.maxY + 2 && s.z > b.minZ - 2 && s.z < b.maxZ + 2)
        {
            return true;
        }
        double x0 = b.minX + 0.05, x1 = Math.max(x0, b.maxX - 0.05);
        double y0 = b.minY + 0.05, y1 = Math.max(y0, b.maxY - 0.05);
        double z0 = b.minZ + 0.05, z1 = Math.max(z0, b.maxZ - 0.05);
        if (line(s, (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2))
        {
            return true;
        }
        for (int i = 0; i < 8; i++)
        {
            if (line(s, (i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1))
            {
                return true;
            }
        }
        return false;
    }

    /** A clear line from the camera to (tx, ty, tz): no full opaque block between them (Amanatides and Woo). */
    private static boolean line(Snapshot s, double tx, double ty, double tz)
    {
        double dx = tx - s.x, dy = ty - s.y, dz = tz - s.z;
        int x = floor(s.x), y = floor(s.y), z = floor(s.z);
        int ex = floor(tx), ey = floor(ty), ez = floor(tz);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        double ddx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double ddy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double ddz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double mx = dx == 0 ? Double.MAX_VALUE : (sx > 0 ? x + 1 - s.x : s.x - x) * ddx;
        double my = dy == 0 ? Double.MAX_VALUE : (sy > 0 ? y + 1 - s.y : s.y - y) * ddy;
        double mz = dz == 0 ? Double.MAX_VALUE : (sz > 0 ? z + 1 - s.z : s.z - z) * ddz;
        for (int steps = 0; steps < 400; steps++)
        {
            if (mx < my && mx < mz)
            {
                x += sx;
                mx += ddx;
            }
            else if (my < mz)
            {
                y += sy;
                my += ddy;
            }
            else
            {
                z += sz;
                mz += ddz;
            }
            if (x == ex && y == ey && z == ez)
            {
                return true;
            }
            if (opaque(s.world, x, y, z))
            {
                return false;
            }
        }
        return true;   // farther than any render distance: leave it to the game
    }

    private static boolean opaque(World world, int x, int y, int z)
    {
        if (y < 0 || y > 255)
        {
            return false;
        }
        try
        {
            Block b = world.getBlock(x, y, z);
            return b != null && b.hasTransparency();   // yarn's name for Block.isOpaqueCube
        }
        catch (RuntimeException ex)
        {
            return false;   // a chunk changing under the thread: count it as see-through
        }
    }

    private static int floor(double v)
    {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
