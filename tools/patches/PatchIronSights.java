import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Javassist patch: iron sights hidden while a sight is attached (user request 10 Oktober 2026,
 * Mk18; net.decimation.fixes.IronSights holds the logic).
 *
 * - GunItemRenderer (deci.K.b) a(ItemStack, Object[]), the method that draws a gun's model and its
 *   attachments in every view: IronSights.begin(gun.model, stack) first, IronSights.end() in a
 *   finally (GunItem.model = deci.ay.i.aeu).
 * - BModel (deci.n.f) renderParts (bm): each BModelPart.render(float, boolean) (deci.n.b.a(FZ)V)
 *   runs only when IronSights.skip(this, part) is false.
 * deci.n.f is ALREADY patched by PatchScope: take it from dist/Decimation.jar (Javassist reads a
 * path as a jar only when it ends in .jar, so not Decimation.jar.patched) (deci.K.b is identical in
 * both). IronSights is our class (deciworldgen jar), not on this class path: a stand in with the
 * same signatures is made here for the compile only.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_irons tools/patches/PatchIronSights.java
 *   java -cp tools/lib/javassist.jar:build/patch_irons PatchIronSights dist/Decimation.jar build/patch_irons/out
 *   (cd build/patch_irons/out && zip <jar> deci/K/b.class deci/n/f.class)   # for each of the three jars
 */
public class PatchIronSights {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass stand = pool.makeClass("net.decimation.fixes.IronSights");
        stand.addMethod(CtNewMethod.make("public static void begin(Object m, net.minecraft.item.ItemStack s) { }", stand));
        stand.addMethod(CtNewMethod.make("public static void end() { }", stand));
        stand.addMethod(CtNewMethod.make("public static boolean skip(Object m, Object p) { return false; }", stand));

        CtClass renderer = pool.get("deci.K.b");
        CtMethod draw = renderer.getMethod("a", "(Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V");
        draw.insertBefore("{ net.decimation.fixes.IronSights.begin(((deci.ay.i) $1.func_77973_b()).aeu, $1); }");
        draw.insertAfter("{ net.decimation.fixes.IronSights.end(); }", true);

        CtClass model = pool.get("deci.n.f");
        CtMethod parts = model.getMethod("bm", "()V");
        final int[] done = new int[1];
        parts.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws javassist.CannotCompileException {
                if (m.getClassName().equals("deci.n.b") && m.getMethodName().equals("a") && m.getSignature().equals("(FZ)V")) {
                    m.replace("{ if (!net.decimation.fixes.IronSights.skip(this, $0)) { $proceed($$); } }");
                    done[0]++;
                }
            }
        });
        if (done[0] != 1) {
            throw new IllegalStateException("expected one part render call in renderParts, got " + done[0]);
        }
        renderer.writeFile(a[1]);
        model.writeFile(a[1]);
        System.out.println("patched deci/K/b.a(ItemStack, Object[]) begin / end and deci/n/f.bm skip");
    }
}
