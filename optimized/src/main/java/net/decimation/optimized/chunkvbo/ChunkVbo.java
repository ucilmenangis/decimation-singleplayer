package net.decimation.optimized.chunkvbo;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

/**
 * Sodium's core idea for 1.7.10 (Fabulously Optimized's renderer, which does not exist below 1.16): chunk geometry
 * drawn from vertex buffers instead of display lists. Measured on this Mac (Apple's OpenGL on Metal), replaying the
 * chunk display lists (glCallLists) was about a third of each frame.
 *
 * Compile: while a chunk section's pass is built (BufferBuilderMixin: between preRenderBlocks and postRenderBlocks,
 * main thread, OptiFine's plain path), Tessellator.draw hands its vertices here instead of drawing them
 * (TessellatorMixin), so the display list stays empty; at the end they go into one buffer per display list id, the
 * section's own translate (posXClip ...) added to the positions. Draw: RenderList's glCallLists goes through draw():
 * each id with a buffer is drawn from it, any other id (geometry built some other way) by its display list as before.
 * Not used with OptiFine shaders, Fast Render or multi-texture (their own paths) or chunks built off the main thread.
 */
public final class ChunkVbo
{
    /** One draw of a compile: GL mode, first vertex, vertex count, which arrays the Tessellator had. */
    private static final class Segment
    {
        int mode, first, count;
        boolean texture, light, color, normal;
    }

    private static final class Entry
    {
        int vbo;
        Segment[] segments;
    }

    private static final int STRIDE = 32;   // Tessellator: x y z u v colour normal light, 4 bytes each
    private static final Map<Integer, Entry> ENTRIES = new HashMap<Integer, Entry>();
    private static final List<Segment> SEGMENTS = new ArrayList<Segment>();
    private static ByteBuffer staging = BufferUtils.createByteBuffer(1 << 20);
    private static final float SEAM = 1.000001f;
    private static int list = -1, vertices;
    private static float dx, dy, dz;
    private static Thread main;
    private static Method fastRender, shaders, multiTexture;
    private static boolean optifineChecked;

    /** Sections drawn from buffers / by their display list last frame (dev check). */
    public static volatile int fromBuffers, fromLists;

    private ChunkVbo()
    {
    }

    /** True while a compile's vertices are being taken (Tessellator.draw then hands them over). */
    public static boolean capturing()
    {
        return list >= 0;
    }

    /** preRenderBlocks: the display list of a section's pass is about to be built at (x, y, z) in its render list. */
    public static void begin(int glList, int x, int y, int z)
    {
        Entry old = ENTRIES.remove(glList);
        if (old != null)
        {
            GL15.glDeleteBuffers(old.vbo);
        }
        if (Thread.currentThread() != main || optifineOwnPath())
        {
            return;   // OptiFine's own path or another thread: the display list is used as before
        }
        list = glList;
        dx = x;
        dy = y;
        dz = z;
        vertices = 0;
        staging.clear();
        SEGMENTS.clear();
    }

    /** Tessellator.draw during a compile: its vertices (raw ints, 8 a vertex) appended instead of drawn. */
    public static void capture(int[] raw, int ints, int count, int mode, boolean texture, boolean light, boolean color,
                               boolean normal)
    {
        if (count <= 0)
        {
            return;
        }
        if (staging.remaining() < ints * 4)
        {
            ByteBuffer bigger = BufferUtils.createByteBuffer(Math.max(staging.capacity() * 2, staging.position() + ints * 4));
            staging.flip();
            bigger.put(staging);
            staging = bigger;
        }
        for (int i = 0; i < ints; i += 8)
        {
            // the display list's transform: translate to the section, scale 1.000001 about its centre (hides seams)
            staging.putFloat(dx + 8 + (Float.intBitsToFloat(raw[i]) - 8) * SEAM);
            staging.putFloat(dy + 8 + (Float.intBitsToFloat(raw[i + 1]) - 8) * SEAM);
            staging.putFloat(dz + 8 + (Float.intBitsToFloat(raw[i + 2]) - 8) * SEAM);
            for (int k = 3; k < 8; k++)
            {
                staging.putInt(raw[i + k]);
            }
        }
        Segment s = new Segment();
        s.mode = mode;
        s.first = vertices;
        s.count = count;
        s.texture = texture;
        s.light = light;
        s.color = color;
        s.normal = normal;
        SEGMENTS.add(s);
        vertices += count;
    }

    /** postRenderBlocks: what was taken goes into the section pass's buffer. */
    public static void end()
    {
        if (list < 0)
        {
            return;
        }
        if (vertices > 0)
        {
            staging.flip();
            Entry e = new Entry();
            e.vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, e.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, staging, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            e.segments = SEGMENTS.toArray(new Segment[SEGMENTS.size()]);
            ENTRIES.put(list, e);
        }
        list = -1;
    }

    /** RenderList.callLists: the region's lists, each from its buffer when it has one. */
    public static void draw(IntBuffer lists)
    {
        main = Thread.currentThread();
        int buffers = 0, displayLists = 0;
        for (int i = lists.position(); i < lists.limit(); i++)
        {
            int id = lists.get(i);
            Entry e = ENTRIES.get(id);
            if (e == null)
            {
                arrays(0);   // a display list sets its own arrays
                GL11.glCallList(id);
                displayLists++;
                continue;
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, e.vbo);
            for (Segment s : e.segments)
            {
                drawSegment(s);
            }
            buffers++;
        }
        arrays(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        fromBuffers = buffers;
        fromLists = displayLists;
    }

    private static final int TEXTURE = 1, LIGHT = 2, COLOR = 4, NORMAL = 8, VERTEX = 16;
    /** The client arrays switched on now (bits above): kept across sections, off again after the batch. */
    private static int enabled;

    /** Tessellator.draw's arrays, from the bound buffer. */
    private static void drawSegment(Segment s)
    {
        arrays(VERTEX | (s.texture ? TEXTURE : 0) | (s.light ? LIGHT : 0) | (s.color ? COLOR : 0) | (s.normal ? NORMAL : 0));
        long base = (long) s.first * STRIDE;
        if (s.texture)
        {
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, base + 12);
        }
        if (s.light)
        {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
            GL11.glTexCoordPointer(2, GL11.GL_SHORT, STRIDE, base + 28);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }
        if (s.color)
        {
            GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, base + 20);
        }
        if (s.normal)
        {
            GL11.glNormalPointer(GL11.GL_BYTE, STRIDE, base + 24);
        }
        GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, base);
        GL11.glDrawArrays(s.mode, 0, s.count);
    }

    /** Switches the client arrays to `want`, touching only those that change. */
    private static void arrays(int want)
    {
        int change = want ^ enabled;
        if (change == 0)
        {
            return;
        }
        array(change, want, TEXTURE, GL11.GL_TEXTURE_COORD_ARRAY);
        if ((change & LIGHT) != 0)
        {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
            array(change, want, LIGHT, GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }
        array(change, want, COLOR, GL11.GL_COLOR_ARRAY);
        array(change, want, NORMAL, GL11.GL_NORMAL_ARRAY);
        array(change, want, VERTEX, GL11.GL_VERTEX_ARRAY);
        enabled = want;
    }

    private static void array(int change, int want, int bit, int glArray)
    {
        if ((change & bit) != 0)
        {
            if ((want & bit) != 0)
            {
                GL11.glEnableClientState(glArray);
            }
            else
            {
                GL11.glDisableClientState(glArray);
            }
        }
    }

    /** OptiFine's Fast Render, shaders or multi-texture on (they build chunks their own way); false without OptiFine. */
    private static boolean optifineOwnPath()
    {
        try
        {
            if (!optifineChecked)
            {
                optifineChecked = true;
                Class<?> config = Class.forName("net.minecraft.Config");
                fastRender = config.getMethod("isFastRender");
                shaders = config.getMethod("isShaders");
                multiTexture = config.getMethod("isMultiTexture");
            }
            return fastRender != null && ((Boolean) fastRender.invoke(null) || (Boolean) shaders.invoke(null)
                || (Boolean) multiTexture.invoke(null));
        }
        catch (ClassNotFoundException ex)
        {
            return false;   // no OptiFine: vanilla's plain path
        }
        catch (ReflectiveOperationException ex)
        {
            throw new IllegalStateException("DeciRemnant Optimized: OptiFine's Config not readable", ex);
        }
    }
}
