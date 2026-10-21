import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

/**
 * Javassist patch: recoil by grip (docs/superpowers/specs/2026-10-21-new-attachments-design.md; net.decimation.fixes.
 * GripRecoil holds the numbers).
 *
 * - GunItem (deci.ay.i) clientHeldTick d(ItemStack, EntityPlayer): Decimation turns the view by the player's recoil,
 *   divided by 3 when the grip IS its Foregrip (two reads of ItemRegistry.foregrip, deci.aD.k.awf). Each read becomes
 *   null (the divided branch never runs), and the two recoil reads that follow each one (PlayerData.getRecoilPitch
 *   deci.Q.b.cs()F, getRecoilYaw ct()F: the divided and the plain branch) become GripRecoil.pitch / yaw(gun, value).
 *   The decay reads before them (recoil x 0.04) and the camera kick read after stay. GripRecoil.current holds the gun
 *   while the method runs (an expression edit cannot see the method's own arguments).
 * - GunItemRenderer (deci.K.b) renderItem: the Object.equals right after its read of ItemRegistry.foregrip (the view's
 *   kick calmed for the Foregrip) becomes GripRecoil.calmsView(grip) || the old test.
 * deci.K.b is already patched by PatchScope / PatchIronSights: read dist/Decimation.jar (Javassist reads a path as a jar
 * only when it ends in .jar). GripRecoil is our class: a stand in with its signatures is made here for the compile.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_grip tools/patches/PatchGripRecoil.java
 *   java -cp tools/lib/javassist.jar:build/patch_grip PatchGripRecoil dist/Decimation.jar build/patch_grip/out
 *   (cd build/patch_grip/out && zip <jar> deci/ay/i.class deci/K/b.class)   # for each of the three jars
 */
public class PatchGripRecoil {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass stand = pool.makeClass("net.decimation.fixes.GripRecoil");
        stand.addField(CtField.make("public static net.minecraft.item.ItemStack current;", stand));
        stand.addMethod(CtNewMethod.make("public static float pitch(net.minecraft.item.ItemStack g, float r) { return r; }", stand));
        stand.addMethod(CtNewMethod.make("public static float yaw(net.minecraft.item.ItemStack g, float r) { return r; }", stand));
        stand.addMethod(CtNewMethod.make("public static boolean calmsView(Object g) { return false; }", stand));

        CtClass gun = pool.get("deci.ay.i");
        CtMethod tick = gun.getMethod("d", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V");
        // one pass in bytecode order (a replacement shifts later offsets, so none are recorded): each Foregrip read
        // arms "the next two recoil reads", which are the divided and the plain branch of that turn
        final int[] swapped = new int[3], armed = new int[1];
        tick.instrument(new ExprEditor() {
            public void edit(FieldAccess f) throws javassist.CannotCompileException {
                if (f.getClassName().equals("deci.aD.k") && f.getFieldName().equals("awf")) {
                    f.replace("{ $_ = null; }");
                    swapped[0]++;
                    armed[0] = 2;
                }
            }

            public void edit(MethodCall m) throws javassist.CannotCompileException {
                String n = m.getMethodName();
                if (armed[0] == 0 || !m.getClassName().equals("deci.Q.b") || !m.getSignature().equals("()F")
                    || !(n.equals("cs") || n.equals("ct"))) {
                    return;
                }
                armed[0]--;
                String hook = n.equals("cs") ? "pitch" : "yaw";
                m.replace("{ $_ = net.decimation.fixes.GripRecoil." + hook + "(net.decimation.fixes.GripRecoil.current, $proceed($$)); }");
                swapped[n.equals("cs") ? 1 : 2]++;
            }
        });
        if (swapped[0] != 2 || swapped[1] != 2 || swapped[2] != 2) {
            throw new IllegalStateException("expected 2 Foregrip reads, 2 pitch and 2 yaw reads swapped, got "
                                            + swapped[0] + ", " + swapped[1] + ", " + swapped[2]);
        }
        tick.insertBefore("{ net.decimation.fixes.GripRecoil.current = $1; }");
        tick.insertAfter("{ net.decimation.fixes.GripRecoil.current = null; }", true);

        CtClass renderer = pool.get("deci.K.b");
        CtMethod render = renderer.getMethod("renderItem",
            "(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V");
        final int[] calm = new int[2];     // reads seen, equals swapped
        render.instrument(new ExprEditor() {
            public void edit(FieldAccess f) {
                if (f.getClassName().equals("deci.aD.k") && f.getFieldName().equals("awf")) {
                    calm[0]++;
                }
            }

            public void edit(MethodCall m) throws javassist.CannotCompileException {
                if (calm[0] == 1 && calm[1] == 0 && m.getMethodName().equals("equals")
                    && m.getSignature().equals("(Ljava/lang/Object;)Z")) {
                    m.replace("{ $_ = net.decimation.fixes.GripRecoil.calmsView($0) || $proceed($$); }");
                    calm[1]++;
                }
            }
        });
        if (calm[0] != 1 || calm[1] != 1) {
            throw new IllegalStateException("expected one Foregrip read and the equals after it in renderItem, got "
                                            + calm[0] + ", " + calm[1]);
        }
        gun.writeFile(a[1]);
        renderer.writeFile(a[1]);
        System.out.println("patched deci/ay/i.d (2 Foregrip reads, 2 pitch and 2 yaw reads) and deci/K/b.renderItem (1 equals)");
    }
}
