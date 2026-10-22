import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * Javassist patch: gun durability (feature 10, docs/superpowers/specs/2026-10-22-durability-design.md; the rules are in
 * net.decimation.fixes.GunWear).
 *
 * - GunItem (deci.ay.i) fire a(ItemStack, EntityPlayer), the client's shot (fireRay, PacketGunHit, PacketGunFire, the
 *   Fire animation, recoil), starts with `if (GunWear.blocked($1, $2)) return;`: a jammed or worn out gun clicks instead.
 * - GunItem onServerFire b(ItemStack, EntityPlayer), the server's once a shot (PacketGunFire's handler), starts with
 *   `GunWear.onShot($1, $2);`: the wear and the jam roll.
 * Both calls sit in a catch of NoClassDefFoundError: dev/libs/Decimation-base.jar is shared by every checkout, and one
 * without GunWear (a branch not yet merged) keeps firing as before. deci.ay.i is already patched by PatchGripRecoil
 * (clientHeldTick): read dist/Decimation.jar (Javassist reads a path as a jar only when it ends in .jar). GunWear is ours:
 * a stand in with its signatures is made here for the compile.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_gunwear tools/patches/PatchGunWear.java
 *   java -cp tools/lib/javassist.jar:build/patch_gunwear PatchGunWear dist/Decimation.jar build/patch_gunwear/out
 *   (cd build/patch_gunwear/out && zip <jar> deci/ay/i.class)   # for each of the three jars
 */
public class PatchGunWear {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass stand = pool.makeClass("net.decimation.fixes.GunWear");
        stand.addMethod(CtNewMethod.make("public static boolean blocked(net.minecraft.item.ItemStack g, net.minecraft.entity.player.EntityPlayer p) { return false; }", stand));
        stand.addMethod(CtNewMethod.make("public static void onShot(net.minecraft.item.ItemStack g, net.minecraft.entity.player.EntityPlayer p) { }", stand));

        CtClass gun = pool.get("deci.ay.i");
        String desc = "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V";
        CtMethod fire = gun.getMethod("a", desc);
        CtMethod serverFire = gun.getMethod("b", desc);
        fire.insertBefore("{ try { if (net.decimation.fixes.GunWear.blocked($1, $2)) { return; } } catch (java.lang.NoClassDefFoundError e) { } }");
        serverFire.insertBefore("{ try { net.decimation.fixes.GunWear.onShot($1, $2); } catch (java.lang.NoClassDefFoundError e) { } }");

        // both calls must be in, once each
        int[] calls = new int[2];
        for (CtMethod m : new CtMethod[] {fire, serverFire}) {
            m.instrument(new javassist.expr.ExprEditor() {
                public void edit(javassist.expr.MethodCall c) {
                    if (c.getClassName().equals("net.decimation.fixes.GunWear")) {
                        calls[c.getMethodName().equals("blocked") ? 0 : 1]++;
                    }
                }
            });
        }
        if (calls[0] != 1 || calls[1] != 1) {
            throw new IllegalStateException("expected one blocked and one onShot call, found " + calls[0] + " and " + calls[1]);
        }
        gun.writeFile(a[1]);
        System.out.println("patched deci.ay.i: fire asks GunWear.blocked, onServerFire calls GunWear.onShot");
    }
}
