import javassist.*;

/**
 * Javassist patch: switch off Decimation's picture in picture scope (user
 * request 2026-10-08: scoped guns cost about 40 fps, 150 -> 110).
 *
 * The original scope (kept intact, only gated): ClientRenderHandler
 * (deci.c.b) renderScopeView a(Float) renders the whole world a second time
 * with a narrow FOV into a texture of up to 1024 x 1024 EVERY frame a gun
 * with a scope is held (aiming or not), and BModel (deci.n.f)
 * renderScopeGlass b(deci.n.b) draws that texture on the scope glass.
 *
 * Patch: renderScopeView returns at once unless the system property
 * decimation.scope.pip is "true". The rest of the method is untouched, so
 * the old scope comes back with that one switch (deciworldgen's
 * config/deciworldgen_scope.cfg "pictureInPicture" sets the property). With
 * the switch off, net.decimation.fixes.ScopeZoom zooms the view (FOV) while
 * aiming and copies the centre of each finished frame into the scope
 * texture, which renderScopeGlass (unpatched) still draws on the glass.
 * (A first version also gated renderScopeGlass: the glass then vanished and
 * the scope showed its solid black body.)
 *
 * Glass (zoom mode only): ScopeZoom copies the world frame under the glass
 * into the scope texture before the hand is drawn; renderScopeGlass maps it
 * by the glass's own screen position (projective texturing), so the glass is
 * see-through at any window size and zoom, and records where the glass is on
 * screen (static glassX / glassY / box / glassTime, GL feedback mode every
 * 8th frame) so ScopeZoom can centre the sight on the screen centre, where
 * shots go, and copy only that part of the frame. (v0.28.1 copied a guessed
 * centre square and was misaligned in a large window.) With the switch on,
 * the method runs exactly as before (the added fields stay unused).
 *
 * Self-contained: the patched class references no deciworldgen class, so
 * Decimation still loads without our mod (then the glass shows a still
 * texture and there is no zoom).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_scope tools/patches/PatchScope.java
 *   java -cp tools/lib/javassist.jar:build/patch_scope PatchScope <Decimation jar ending .jar> build/patch_scope/out <lwjgl 2.9 jar>
 *   (cd build/patch_scope/out && zip <jar> deci/c/b.class deci/n/f.class)
 * Patch from the ORIGINAL classes (check they are unchanged in the target jar).
 */
public class PatchScope {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        pool.insertClassPath(a[2]); // lwjgl 2.9 (Prism libraries) for the GL calls
        String gate = "{ if (!Boolean.getBoolean(\"decimation.scope.pip\")) return; }";

        CtClass handler = pool.get("deci.c.b");
        handler.getMethod("a", "(Ljava/lang/Float;)V").insertBefore(gate);
        handler.writeFile(a[1]);

        String gl = "org.lwjgl.opengl.GL11.";
        String zoomMode = "!Boolean.getBoolean(\"decimation.scope.pip\")";
        CtClass model = pool.get("deci.n.f");

        // renderScopeGlass (b), zoom mode: ScopeZoom copies the world frame (the part
        // under the glass) into the scope texture before the hand is drawn; the
        // glass maps it by its own screen position (projective texturing: eye
        // linear texgen, texture matrix = bias x current projection), so it is
        // see-through at any window size and zoom and hides what is behind it
        // (the gun's own front sight). Buffers are allocated once (static fields).
        // (A depth only glass drawn before the body, no copy at all, was tried in
        // v0.28.3 work: attachment glasses are drawn after the gun body, whose
        // front sight then showed in the glass, and the integrated scope stayed
        // grey; dropped.)
        model.addField(CtField.make("public static java.nio.FloatBuffer glassMatrix;", model));
        model.addField(CtField.make("public static java.nio.FloatBuffer glassPlanes;", model));
        StringBuilder on = new StringBuilder("{ if (" + zoomMode + ") {");
        on.append("if (glassMatrix == null) {")
          .append("  glassMatrix = org.lwjgl.BufferUtils.createFloatBuffer(16);")
          .append("  glassPlanes = org.lwjgl.BufferUtils.createFloatBuffer(16);")
          .append("  for (int i = 0; i < 16; i++) glassPlanes.put(i, i % 5 == 0 ? 1f : 0f);")  // 4 unit rows
          .append("}")
          .append("((java.nio.Buffer) glassMatrix).clear();")
          .append(gl).append("glGetFloat(2983, glassMatrix);")             // GL_PROJECTION_MATRIX
          .append(gl).append("glMatrixMode(5890);")                         // GL_TEXTURE
          .append(gl).append("glPushMatrix();")
          .append(gl).append("glLoadIdentity();")
          .append(gl).append("glTranslatef(0.5f, 0.5f, 0.5f);")
          .append(gl).append("glScalef(0.5f, 0.5f, 0.5f);")
          .append(gl).append("glMultMatrix(glassMatrix);")
          .append(gl).append("glMatrixMode(5888);")                         // GL_MODELVIEW
          .append(gl).append("glPushMatrix();")
          .append(gl).append("glLoadIdentity();");                           // eye planes = identity
        int[] coord = {8192, 8193, 8194, 8195};                               // GL_S, GL_T, GL_R, GL_Q
        for (int i = 0; i < 4; i++) {
            on.append("((java.nio.Buffer) glassPlanes).position(").append(i * 4).append(");")
              .append("((java.nio.Buffer) glassPlanes).limit(").append(i * 4 + 4).append(");")
              .append(gl).append("glTexGeni(").append(coord[i]).append(", 9472, 9216);")        // EYE_LINEAR
              .append(gl).append("glTexGen(").append(coord[i]).append(", 9474, glassPlanes);"); // EYE_PLANE
        }
        on.append("((java.nio.Buffer) glassPlanes).clear();")
          .append(gl).append("glPopMatrix();");
        for (int g : new int[] {3168, 3169, 3170, 3171}) {                   // GL_TEXTURE_GEN_S..Q
            on.append(gl).append("glEnable(").append(g).append(");");
        }
        on.append("} }");
        StringBuilder off = new StringBuilder("{ if (" + zoomMode + ") {");
        for (int g : new int[] {3168, 3169, 3170, 3171}) {
            off.append(gl).append("glDisable(").append(g).append(");");
        }
        off.append(gl).append("glMatrixMode(5890);")
           .append(gl).append("glPopMatrix();")
           .append(gl).append("glMatrixMode(5888);")
           .append("} }");

        // where the glass is on screen (window pixels, origin bottom left), for
        // ScopeZoom to centre the sight and to copy only the part of the frame
        // under the glass: every 8th frame the glass part is drawn once more in GL
        // feedback mode (a few quads) and its corners collected
        model.addField(CtField.make("public static float glassX;", model));
        model.addField(CtField.make("public static float glassY;", model));
        model.addField(CtField.make("public static long glassTime;", model));
        model.addField(CtField.make("public static int glassFrame;", model));
        // measure when (glassFrame & glassMask) == 0: ScopeZoom sets 1 (every 2nd
        // frame) while it learns a new sight, 7 (every 8th) otherwise
        model.addField(CtField.make("public static int glassMask = 7;", model));
        // the box itself: a glass bigger than the screen (8x) comes back clipped at
        // the screen edge, so its centre is only right when the box is inside
        model.addField(CtField.make("public static float glassMinX;", model));
        model.addField(CtField.make("public static float glassMinY;", model));
        model.addField(CtField.make("public static float glassMaxX;", model));
        model.addField(CtField.make("public static float glassMaxY;", model));
        model.addField(CtField.make("public static java.nio.FloatBuffer glassFeedback;", model));
        String measure =
            "{ if (" + zoomMode + " && (++glassFrame & glassMask) == 0) {"
          + "  if (glassFeedback == null) glassFeedback = org.lwjgl.BufferUtils.createFloatBuffer(4096);"
          + "  java.nio.FloatBuffer fb = glassFeedback;"
          + "  ((java.nio.Buffer) fb).clear();"
          + "  " + gl + "glFeedbackBuffer(1536, fb);"                   // GL_2D
          + "  " + gl + "glRenderMode(7169);"                           // GL_FEEDBACK
          + "  $1.a(0.0625f, true);"                                    // BModelPart.render(float, boolean)
          + "  int n = " + gl + "glRenderMode(7168);"                   // GL_RENDER
          + "  float x0 = 1.0E9f, y0 = 1.0E9f, x1 = -1.0E9f, y1 = -1.0E9f;"
          + "  int i = 0;"
          + "  while (i < n) {"
          + "    float tok = fb.get(i); i++;"
          + "    if (tok == 1795.0f) {"                                 // GL_POLYGON_TOKEN
          + "      int c = (int) fb.get(i); i++;"
          + "      for (int k = 0; k < c; k++) {"
          + "        float x = fb.get(i); float y = fb.get(i + 1); i += 2;"
          + "        if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;"
          + "      }"
          + "    } else if (tok == 1792.0f) { i += 1; }"                 // PASS_THROUGH
          + "    else if (tok == 1793.0f || tok == 1796.0f || tok == 1797.0f || tok == 1798.0f) { i += 2; }"
          + "    else if (tok == 1794.0f || tok == 1799.0f) { i += 4; }" // LINE, LINE_RESET
          + "    else { i = n; }"
          + "  }"
          + "  if (x1 >= x0) { glassX = (x0 + x1) / 2.0f; glassY = (y0 + y1) / 2.0f;"
          + "    glassMinX = x0; glassMinY = y0; glassMaxX = x1; glassMaxY = y1; glassTime = System.nanoTime(); }"
          + "} }";
        CtMethod glass = model.getMethod("b", "(Ldeci/n/b;)V");
        glass.insertBefore(on.toString());
        glass.insertAfter(off.toString());
        glass.insertAfter(measure);
        // Java 8 trap: Javassist on a newer JDK binds Buffer methods (flip, clear,
        // position, limit) of FloatBuffer to the Java 9 covariant returns
        // (NoSuchMethodError in game): always call them through java.nio.Buffer.
        model.writeFile(a[1]);
        // Java 8 trap: Javassist on a newer JDK binds FloatBuffer.flip()/clear() to
        // the Java 9 covariant return (NoSuchMethodError in game): go through Buffer.
        System.out.println("patched deci.c.b.a(Float) renderScopeView and deci.n.f.b(BModelPart) renderScopeGlass");
    }
}
