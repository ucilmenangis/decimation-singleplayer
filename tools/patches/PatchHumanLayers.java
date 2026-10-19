import javassist.*;
import javassist.expr.*;

/**
 * Javassist patch for deci.q.h (ModelHuman: every human NPC and infected; user 19 Oktober 2026 "from 7 to 1", bug.md
 * "NPC drawing heavy").
 *
 * renderHuman (a(Ldeci/af/d;FFFFFF)V) drew each NPC in seven layers (body on the model; eyes, hair, beard on copy Fa;
 * shirt, pants, shoes on copy Fb, both 0.05 larger and posed again every frame) and asked
 * ClientProxy.LineOfSight.canSeeEntity (up to nine ray traces) three times a frame (the model and both copies) only to
 * set HumanEntity.visibleTicks, which nothing reads (isRecentlyVisible ei()Z has no caller in the jar); hidden NPCs are
 * not drawn at all already (ClientEventHandler cancels RenderLivingEvent.Pre with the same check).
 *
 * After the patch, with net.decimation.fixes.HumanLayers (our classes):
 *   - first thing (model only, not the copies): HumanLayers.begin(entity) picks the look's one picture (active);
 *   - the copies are posed only when not active (the two calls a(Ldeci/af/d;FFFFFF)V on Fa / Fb);
 *   - bind 1 (body) binds the picture when active; binds 2 .. 7 (eyes, hair, beard, shirt, pants, shoes) and the
 *     passes on the copies (renderHead 2, renderHeadLayer 1 and 2, renderBodyAndArms 2 and 3, renderLegs 2 and 3) run
 *     only when not active; the model's own passes (renderHead 1, renderBodyAndArms 1, renderLegs 1) always run;
 *   - canSeeEntity in renderHuman answers true without ray tracing (the NPC is drawn, so it passed the same check);
 *     HumanLayers.sightChecks brings the old checks back (the dev test bench compares with it).
 * Armour, masks, vests and backpacks after the layers are untouched (binds 8 and later).
 *
 * The class is unchanged from the original jar (checked: same hash in the original and the three jars).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_layers tools/patches/PatchHumanLayers.java
 *   cp Decimation.jar.original.bak build/patch_layers/original.jar   # the class as Decimation shipped it
 *   java -cp tools/lib/javassist.jar:build/patch_layers PatchHumanLayers build/patch_layers/original.jar build/patch_layers/out dev/build/classes/java/main
 *   (cd build/patch_layers/out && zip <jar> deci/q/h.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchHumanLayers {
    static final String L = "net.decimation.fixes.HumanLayers";
    static final String SKIP = "if (!" + L + ".active) { $proceed($$); }";

    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        pool.insertClassPath(a[2]);                                   // our classes: net.decimation.fixes.HumanLayers
        CtClass c = pool.get("deci.q.h");
        CtMethod render = c.getMethod("a", "(Ldeci/af/d;FFFFFF)V");
        final int[] n = new int[7]; // binds, renderHead, renderHeadLayer, renderBodyAndArms, renderLegs, copies posed, sight
        render.instrument(new ExprEditor() {
            public void edit(MethodCall e) throws CannotCompileException {
                String name = e.getMethodName(), sig = e.getSignature(), owner = e.getClassName();
                if (name.equals("func_110577_a") && sig.equals("(Lnet/minecraft/util/ResourceLocation;)V")) {
                    int i = ++n[0];
                    if (i == 1) {
                        e.replace("if (" + L + ".active) { " + L + ".bind(); } else { $proceed($$); }");
                    } else if (i <= 7) {
                        e.replace(SKIP);
                    }
                } else if (owner.equals("deci.q.h") && sig.equals("(FLdeci/q/h;)V")) {
                    int pass = name.length() == 1 ? "abcd".indexOf(name.charAt(0)) + 1 : 0;
                    if (pass == 0) {
                        return;
                    }
                    int i = ++n[pass];
                    if (pass == 2 || i >= 2) {                        // every renderHeadLayer; the others after the first
                        e.replace(SKIP);
                    }
                } else if (owner.equals("deci.q.h") && name.equals("a") && sig.equals("(Ldeci/af/d;FFFFFF)V")) {
                    n[5]++;
                    e.replace(SKIP);
                } else if (owner.equals("deci.a.c$a") && name.equals("a")
                        && sig.equals("(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)Z")) {
                    n[6]++;
                    e.replace("$_ = " + L + ".sightChecks ? $proceed($$) : true;");
                }
            }
        });
        int[] want = {-1, 2, 2, 3, 3, 2, 1};
        for (int i = 1; i < want.length; i++) {
            if (n[i] != want[i]) {
                throw new IllegalStateException("deci.q.h changed: call counts " + java.util.Arrays.toString(n));
            }
        }
        if (n[0] < 7) {
            throw new IllegalStateException("deci.q.h changed: " + n[0] + " texture binds");
        }
        render.insertBefore("if (!this.EO) { " + L + ".begin($1); }");
        c.writeFile(a[1]);
        System.out.println("patched deci.q.h.renderHuman: one picture per look (binds " + n[0] + ", calls "
            + java.util.Arrays.toString(n) + ")");
    }
}
