import javassist.*;
import javassist.expr.*;

/**
 * Javassist patch for deci.q.h (ModelHuman), on top of PatchHumanLayers (bug.md "Muzzle flash lit every mob drawn after
 * the shooter", 19 Oktober 2026).
 *
 * renderHuman draws an NPC's night vision goggles glowing: glEnable(RESCALE_NORMAL), glDisable(LIGHTING),
 * ClientEmptyB.disableItemLighting (lighting, both lights, colour material off), the mask, then glDisable(RESCALE_NORMAL)
 * and glEnable(LIGHTING) only: both lights and colour material stayed off, so the NPC's vest, backpack and gun drawn
 * after the goggles were lit by ambient light alone (dark), and RESCALE_NORMAL was off for them although vanilla had it
 * on. Now the switches are saved (net.decimation.fixes.EntityLighting.save) at the goggles' glEnable(RESCALE_NORMAL)
 * and put back exactly (EntityLighting.restore) at their glEnable(LIGHTING). The goggles still glow. Lighting left off
 * between entities (guns, players, mechs) is put back by fixes/EntityLighting at run time, no patch.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_light tools/patches/PatchLightLeaks.java
 *   java -cp tools/lib/javassist.jar:build/patch_light PatchLightLeaks dist/Decimation.jar build/patch_light/out dev/build/classes/java/main LWJGL
 *   (LWJGL: lwjgl-2.9.4-nightly-20150209.jar from ~/.gradle/caches/modules-2/files-2.1/org.lwjgl.lwjgl/lwjgl)
 *   (cd build/patch_light/out && zip <jar> deci/q/h.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar and hold deci.q.h as PatchHumanLayers left it (re-run this after PatchHumanLayers).
 */
public class PatchLightLeaks {
    static final String L = "net.decimation.fixes.EntityLighting";

    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        pool.insertClassPath(a[2]);                                   // our classes: net.decimation.fixes.EntityLighting
        pool.insertClassPath(a[3]);                                   // lwjgl 2.9.4 (GL11) from the gradle cache
        CtClass c = pool.get("deci.q.h");
        CtMethod render = c.getMethod("a", "(Ldeci/af/d;FFFFFF)V");
        if (!c.getRefClasses().contains("net.decimation.fixes.HumanLayers")) {
            throw new IllegalStateException("deci.q.h without PatchHumanLayers: patch that first");
        }
        final int[] n = new int[2]; // glEnable(RESCALE_NORMAL), glEnable(LIGHTING)
        render.instrument(new ExprEditor() {
            public void edit(MethodCall e) throws CannotCompileException {
                if (e.getClassName().equals("org.lwjgl.opengl.GL11") && e.getMethodName().equals("glEnable")) {
                    e.replace("{ if ($1 == 32826) { " + L + ".goggles = " + L + ".save(); } $proceed($$);"
                        + " if ($1 == 2896 && " + L + ".goggles >= 0) { " + L + ".restore(" + L + ".goggles); " + L + ".goggles = -1; } }");
                    n[0]++;
                }
            }
        });
        if (n[0] < 2) {
            throw new IllegalStateException("deci.q.h changed: " + n[0] + " glEnable calls in renderHuman");
        }
        c.writeFile(a[1]);
        System.out.println("patched deci.q.h.renderHuman: night vision goggles leave the lights as they found them ("
            + n[0] + " glEnable calls guarded)");
    }
}
