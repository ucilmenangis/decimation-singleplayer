package net.decimation.fixes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Model parts drawn in one go (bug.md "Drawing many human NPCs is heavy on the client", step 2; user 19 Oktober 2026:
 * "the gun model and gun fighting when npc shoots is the most hurt of the fps"). Decimation's .bmodel parts (guns,
 * attachments, armour, masks, vests, backpacks: BModelPart deci.n.b) each compile two display lists, their own boxes
 * and their children, and every quad in them is a draw call of its own (BModelQuad: startDrawingQuads, four vertices,
 * draw). A rifle with its sight is a few hundred tiny draws per NPC and frame; 40 soldiers drew in 33 ms a frame on the
 * M4 (dev test npcdraw fight). PatchModelBatch: when a part compiles its lists, it runs the same drawing once more with
 * {@link #capturing} on, BModelQuad hands its quads here instead of to the Tessellator, and they are moved by the GL
 * matrix the part's children set (read back per quad) into the part's own space; {@link #end} compiles them into one
 * more list, consecutive quads of one brightness in one draw. The part draws that list in place of its two
 * ({@link #enabled}); its own transform still applies each frame, so animated parts move as before. Decimation's lists
 * stay, so {@link #enabled} switches back at once (dev tests gunbatch and npcdraw compare both ways in one frame).
 */
@SideOnly(Side.CLIENT)
public final class ModelBatch
{
    /** Parts draw their one list (off: Decimation's two; -Ddeciworldgen.modelbatch=false, dev tests switch it). */
    public static boolean enabled = !"false".equals(System.getProperty("deciworldgen.modelbatch"));
    /** True while a part's drawing is captured (read by the patched BModelQuad and BModelPart). */
    public static boolean capturing;
    /** For tests: lists made, quads in them, parts that drew one. */
    public static long built, quads, drawn;

    private static FloatBuffer gl;                       // made on first use (unit tests have no LWJGL)
    private static final float[] base = new float[16], inv = new float[16], cur = new float[16], loc = new float[16];
    private static final float[] tmp = new float[3];
    private static float[] data = new float[32 * 23];   // per quad: 4 x (x, y, z, u, v), normal x, y, z
    private static int[] bright = new int[32];
    private static int n, vi, brightNow, depth, texture;
    private static float nx, ny, nz;
    private static boolean broken;

    private ModelBatch()
    {
    }

    /** Start capturing (PatchModelBatch: before Decimation's compile runs a second time). */
    public static void begin()
    {
        capturing = true;
        broken = false;
        n = 0;
        vi = 0;
        read(base);
        broken = !invert(base, inv);
        depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    /** The capture threw: the part keeps Decimation's lists. */
    public static void fail()
    {
        broken = true;
    }

    /** BModelQuad's startDrawingQuads: a new quad, drawn under the current GL matrix. */
    public static void quad()
    {
        if (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) != texture)
        {
            broken = true;                              // a part bound its own texture: one list cannot hold that
        }
        read(cur);
        mul(inv, cur, loc);
        brightNow = -1;
        vi = 0;
        nx = 0;
        ny = 0;
        nz = 1;
    }

    /** BModelQuad's setBrightness (224 for glowing parts). */
    public static void brightness(int b)
    {
        brightNow = b;
    }

    /** BModelQuad's setNormal. */
    public static void normal(float x, float y, float z)
    {
        normal(loc, x, y, z, tmp);
        nx = tmp[0];
        ny = tmp[1];
        nz = tmp[2];
    }

    /** BModelQuad's addVertexWithUV. */
    public static void vertex(double x, double y, double z, double u, double v)
    {
        if (vi >= 4)
        {
            broken = true;                              // never more than a quad between start and draw
            return;
        }
        if ((n + 1) * 23 > data.length)
        {
            data = java.util.Arrays.copyOf(data, data.length * 2);
            bright = java.util.Arrays.copyOf(bright, bright.length * 2);
        }
        point(loc, (float) x, (float) y, (float) z, tmp);
        int i = n * 23 + vi * 5;
        data[i] = tmp[0];
        data[i + 1] = tmp[1];
        data[i + 2] = tmp[2];
        data[i + 3] = (float) u;
        data[i + 4] = (float) v;
        vi++;
    }

    /** BModelQuad's draw: the quad is complete (returns the Tessellator's vertex count, unused). */
    public static int done()
    {
        if (vi == 4)
        {
            int i = n * 23 + 20;
            data[i] = nx;
            data[i + 1] = ny;
            data[i + 2] = nz;
            bright[n] = brightNow;
            n++;
        }
        else if (vi != 0)
        {
            broken = true;
        }
        vi = 0;
        return 0;
    }

    /**
     * Stop capturing: the quads in one display list (0 when nothing was drawn or something looked odd: the part then
     * keeps Decimation's lists). Matrices a hidden part pushed without popping (BModelPart.render) are popped.
     */
    public static int end()
    {
        capturing = false;
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth)
        {
            GL11.glPopMatrix();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        if (broken || n == 0)
        {
            return 0;
        }
        int list = net.minecraft.client.renderer.GLAllocation.generateDisplayLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        try
        {
            net.minecraft.client.renderer.Tessellator t = net.minecraft.client.renderer.Tessellator.instance;
            int q = 0;
            while (q < n)
            {
                int b = bright[q];
                t.startDrawingQuads();
                if (b >= 0)
                {
                    t.setBrightness(b);
                }
                for (; q < n && bright[q] == b; q++)
                {
                    int i = q * 23;
                    t.setNormal(data[i + 20], data[i + 21], data[i + 22]);
                    for (int k = 0; k < 4; k++, i += 5)
                    {
                        t.addVertexWithUV(data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4]);
                    }
                }
                t.draw();
            }
        }
        catch (RuntimeException e)
        {
            GL11.glEndList();
            net.minecraft.client.renderer.GLAllocation.deleteDisplayLists(list);
            return 0;
        }
        GL11.glEndList();
        built++;
        quads += n;
        return list;
    }

    // ------------------------------------------------------------------ matrices (column major, as GL keeps them)

    private static void read(float[] m)
    {
        if (gl == null)
        {
            gl = BufferUtils.createFloatBuffer(16);
        }
        gl.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, gl);
        gl.get(m, 0, 16);
    }

    /** out = a x b (b applied first). */
    static void mul(float[] a, float[] b, float[] out)
    {
        for (int c = 0; c < 4; c++)
        {
            for (int r = 0; r < 4; r++)
            {
                out[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
            }
        }
    }

    /** A point moved by m. */
    static void point(float[] m, float x, float y, float z, float[] out)
    {
        out[0] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out[1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out[2] = m[2] * x + m[6] * y + m[10] * z + m[14];
    }

    /** A direction turned by m's rotation, unit length (part scales are uniform: GL rescales normals the same way). */
    static void normal(float[] m, float x, float y, float z, float[] out)
    {
        float a = m[0] * x + m[4] * y + m[8] * z, b = m[1] * x + m[5] * y + m[9] * z, c = m[2] * x + m[6] * y + m[10] * z;
        float l = (float) Math.sqrt(a * a + b * b + c * c);
        if (l > 1e-12f)
        {
            a /= l;
            b /= l;
            c /= l;
        }
        out[0] = a;
        out[1] = b;
        out[2] = c;
    }

    /** General 4 x 4 inverse; false when m has none. */
    static boolean invert(float[] m, float[] out)
    {
        float[] v = new float[16];
        v[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        v[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        v[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        v[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        v[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        v[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        v[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        v[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        v[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        v[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        v[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        v[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        v[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        v[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        v[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        v[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        float det = m[0] * v[0] + m[1] * v[4] + m[2] * v[8] + m[3] * v[12];
        if (Math.abs(det) < 1e-20f)
        {
            return false;
        }
        for (int i = 0; i < 16; i++)
        {
            out[i] = v[i] / det;
        }
        return true;
    }
}
