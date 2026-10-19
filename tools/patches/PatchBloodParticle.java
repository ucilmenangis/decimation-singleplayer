import javassist.*;
import javassist.expr.*;

/**
 * Javassist patch for deci.B.k (GroundBloodParticle, the blood splat on the ground; dev test bench, 19 Oktober 2026).
 *
 * Cause: a splat lives 800 ticks (40 s, fancy graphics) and calls Entity.moveEntity every tick, also long after it lies
 * on the ground: block collision plus a scan of the entities around for their collision boxes (World
 * .getEntitiesWithinAABBExcludingEntity), so splats x entities work each tick in a fight (JFR, bench "crowd": 13% of
 * the client thread in GroundBloodParticle.onUpdate -> moveEntity, 5% in Chunk.getEntitiesWithinAABBForEntity).
 *
 * Fix: it moves only until it lands (onGround, field_70122_E); same fall, same place, same 40 s.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_blood tools/patches/PatchBloodParticle.java
 *   java -cp tools/lib/javassist.jar:build/patch_blood PatchBloodParticle dist/Decimation.jar build/patch_blood/out
 *   (cd build/patch_blood/out && zip <jar> deci/B/k.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchBloodParticle {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        CtClass c = pool.get("deci.B.k");
        CtMethod update = c.getDeclaredMethod("func_70071_h_");
        final int[] done = new int[1];
        update.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("func_70091_d")) { // Entity.moveEntity
                    m.replace("{ if (!this.field_70122_E) { $proceed($$); } }");
                    done[0]++;
                }
            }
        });
        if (done[0] != 1) {
            throw new IllegalStateException("expected one moveEntity call, got " + done[0]);
        }
        c.writeFile(a[1]);
        System.out.println("patched deci.B.k.onUpdate: the splat moves only until it lands");
    }
}
