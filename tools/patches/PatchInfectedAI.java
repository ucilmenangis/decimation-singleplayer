import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Javassist patch: infected burn the frame time (user report 2026-10-10, 1 fps; docs/performance.md).
 *
 * Cause (profile of the live game, JFR: about half of the client and server thread samples in
 * pathfinding and the chunk lookups it does): InfectedEntity.onLivingUpdate (deci.ag.d
 * func_70636_d) calls EntityCreature.updateWanderPath (func_70779_j, a full path search to a
 * random spot) EVERY tick for every infected, on the server AND on the client copies (no
 * isRemote check; vanilla calls it only now and then from the server AI), and scans a 40 x 40
 * box for other infected (World.getEntitiesWithinAABB, func_72872_a) every tick for its horde
 * speed check: n x n work. Measured: removing the ~90 infected around the test arena took the
 * fps from about 21 to about 51.
 *
 * Fix: the wander search runs on the server only, once a second per infected (staggered by
 * entity id so they do not all search on one tick); the horde scan runs at most every 10 ticks
 * and its result is kept in between (a new field). Behaviour stays: infected still wander and
 * speed up in hordes, the server AI still paths to its targets.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_infected tools/patches/PatchInfectedAI.java
 *   java -cp tools/lib/javassist.jar:build/patch_infected PatchInfectedAI <ORIGINAL Decimation jar> build/patch_infected/out
 *   (cd build/patch_infected/out && zip <jar> deci/ag/d.class)   # for each of the three jars
 */
public class PatchInfectedAI {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass inf = pool.get("deci.ag.d");
        inf.addField(CtField.make("public java.util.List deciwgHorde;", inf));
        CtMethod update = inf.getDeclaredMethod("func_70636_d");
        final int[] done = new int[2];
        update.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws javassist.CannotCompileException {
                if (m.getMethodName().equals("func_70779_j")) { // updateWanderPath
                    m.replace("{ if (!this.field_70170_p.field_72995_K"
                            + " && (this.field_70173_aa + this.func_145782_y()) % 20 == 0) { $proceed(); } }");
                    done[0]++;
                } else if (m.getMethodName().equals("func_72872_a")) { // World.getEntitiesWithinAABB
                    m.replace("{ if (this.deciwgHorde == null || (this.field_70173_aa + this.func_145782_y()) % 10 == 0) {"
                            + "    this.deciwgHorde = $proceed($$); }"
                            + "  $_ = this.deciwgHorde; }");
                    done[1]++;
                }
            }
        });
        if (done[0] != 1 || done[1] != 1) {
            throw new IllegalStateException("expected one call each, got wander " + done[0] + ", aabb " + done[1]);
        }
        inf.writeFile(a[1]);
        System.out.println("deci/ag/d patched: wander search server side every 20 ticks, horde scan every 10");
    }
}
