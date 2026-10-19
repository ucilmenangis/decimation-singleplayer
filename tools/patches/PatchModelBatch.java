import javassist.*;
import javassist.expr.*;

/**
 * Javassist patch for deci.n.b (BModelPart) and deci.n.c (BModelQuad): .bmodel parts drawn in one draw each
 * (net.decimation.fixes.ModelBatch; bug.md "Drawing many human NPCs is heavy on the client", 19 Oktober 2026).
 *
 * BModelQuad (every quad of every gun, attachment, armour, mask, vest and backpack part): its Tessellator calls
 * (startDrawingQuads, setBrightness, setNormal, addVertexWithUV, draw) go to ModelBatch while ModelBatch.capturing.
 * BModelPart: new field deciBatch; the compile (f(F)V: Decimation's two display lists, own boxes jY and children jZ)
 * runs as before, then once more with capturing on (its genLists / glNewList / glEndList skipped, so the children's
 * matrices really apply and each quad is read back in the part's space): ModelBatch.end makes deciBatch, one list with
 * every quad. render (a(FZ)V) and its two helpers (b, c) call deciBatch in place of jY and skip jZ while
 * ModelBatch.enabled; off, they call Decimation's lists as before. renderWithRotation (own boxes only) is untouched.
 *
 * The classes are unchanged from the original jar (checked: same hash in the original and the three jars).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_batch tools/patches/PatchModelBatch.java
 *   java -cp tools/lib/javassist.jar:build/patch_batch PatchModelBatch dist/Decimation.jar build/patch_batch/out dev/build/classes/java/main LWJGL
 *   (cd build/patch_batch/out && zip <jar> deci/n/b.class deci/n/c.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *   (LWJGL: lwjgl-2.9.4-nightly-20150209.jar from ~/.gradle/caches/modules-2/files-2.1/org.lwjgl.lwjgl/lwjgl)
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchModelBatch {
    static final String B = "net.decimation.fixes.ModelBatch";

    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        pool.insertClassPath(a[2]);                                   // our classes: net.decimation.fixes.ModelBatch
        pool.insertClassPath(a[3]);                                   // lwjgl 2.9.4 (GL11) from the gradle cache

        CtClass quad = pool.get("deci.n.c");
        final int[] q = new int[5]; // start, brightness, normal, vertex, draw
        for (CtMethod m : quad.getDeclaredMethods()) {
            m.instrument(new ExprEditor() {
                public void edit(MethodCall e) throws CannotCompileException {
                    if (!e.getClassName().equals("net.minecraft.client.renderer.Tessellator")) {
                        return;
                    }
                    String n = e.getMethodName();
                    if (n.equals("func_78382_b")) {
                        e.replace("if (" + B + ".capturing) { " + B + ".quad(); } else { $proceed($$); }");
                        q[0]++;
                    } else if (n.equals("func_78380_c")) {
                        e.replace("if (" + B + ".capturing) { " + B + ".brightness($1); } else { $proceed($$); }");
                        q[1]++;
                    } else if (n.equals("func_78375_b")) {
                        e.replace("if (" + B + ".capturing) { " + B + ".normal($1, $2, $3); } else { $proceed($$); }");
                        q[2]++;
                    } else if (n.equals("func_78374_a")) {
                        e.replace("if (" + B + ".capturing) { " + B + ".vertex($1, $2, $3, $4, $5); } else { $proceed($$); }");
                        q[3]++;
                    } else if (n.equals("func_78381_a")) {
                        e.replace("if (" + B + ".capturing) { $_ = " + B + ".done(); } else { $_ = $proceed($$); }");
                        q[4]++;
                    } else {
                        throw new CannotCompileException("deci.n.c: unexpected Tessellator call " + n);
                    }
                }
            });
        }
        if (q[0] != 4 || q[1] != 3 || q[2] != 8 || q[3] != 10 || q[4] != 4) {
            throw new IllegalStateException("deci.n.c changed: Tessellator calls " + java.util.Arrays.toString(q));
        }

        CtClass part = pool.get("deci.n.b");
        part.addField(CtField.make("public int deciBatch;", part));
        CtMethod compile = part.getDeclaredMethod("f", new CtClass[] {CtClass.floatType});
        CtMethod orig = CtNewMethod.copy(compile, "deciCompile", part, null);
        orig.setModifiers(Modifier.PRIVATE);
        part.addMethod(orig);
        final int[] c = new int[3]; // genLists, newList / endList, glCallList
        orig.instrument(new ExprEditor() {
            public void edit(MethodCall e) throws CannotCompileException {
                String cl = e.getClassName(), n = e.getMethodName();
                if (cl.equals("net.minecraft.client.renderer.GLAllocation") && n.equals("func_74526_a")) {
                    e.replace("$_ = " + B + ".capturing ? 0 : $proceed($$);");
                    c[0]++;
                } else if (cl.equals("org.lwjgl.opengl.GL11") && (n.equals("glNewList") || n.equals("glEndList"))) {
                    e.replace("if (!" + B + ".capturing) { $proceed($$); }");
                    c[1]++;
                }
            }
        });
        compile.setBody("{"
            + "  this.deciCompile($1);"
            + "  if (" + B + ".enabled) {"
            + "    int y = this.jY; int z = this.jZ;"
            + "    " + B + ".begin();"
            + "    try { this.deciCompile($1); } catch (RuntimeException ex) { " + B + ".fail(); }"
            + "    int batch = " + B + ".end();"
            + "    this.jY = y; this.jZ = z; this.jX = true; this.deciBatch = batch;"
            + "  }"
            + "}");
        String[] paths = {"a", "b", "c"};
        for (String name : paths) {
            CtMethod m = part.getDeclaredMethod(name, new CtClass[] {CtClass.floatType, CtClass.booleanType});
            m.instrument(new ExprEditor() {
                public void edit(MethodCall e) throws CannotCompileException {
                    if (e.getClassName().equals("org.lwjgl.opengl.GL11") && e.getMethodName().equals("glCallList")) {
                        e.replace("if (" + B + ".enabled && this.deciBatch != 0) {"
                            + " if ($1 == this.jY) { org.lwjgl.opengl.GL11.glCallList(this.deciBatch); " + B + ".drawn++; } }"
                            + " else { $proceed($$); }");
                        c[2]++;
                    }
                }
            });
        }
        if (c[0] != 2 || c[1] != 4 || c[2] < 3) {
            throw new IllegalStateException("deci.n.b changed: calls " + java.util.Arrays.toString(c));
        }
        quad.writeFile(a[1]);
        part.writeFile(a[1]);
        System.out.println("patched deci.n.c (Tessellator calls " + java.util.Arrays.toString(q) + ") and deci.n.b (calls "
            + java.util.Arrays.toString(c) + "): parts drawn in one list each");
    }
}
