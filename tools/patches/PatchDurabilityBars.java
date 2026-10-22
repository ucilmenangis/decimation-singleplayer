import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

/**
 * Javassist patch: native durability bars on gun and gas mask icons (user, 23 Oktober 2026: "the icon gun item has progress
 * bar in there showing durability like native progress bar"; "for gas mask, you better add durability progress on it").
 *
 * Forge's RenderItem.renderItemOverlayIntoGUI draws a tool's bar when Item.showDurabilityBar(ItemStack) says so, filled by
 * Item.getDurabilityForDisplay(ItemStack) (the share used up). Both are Forge's own methods (readable names everywhere, no
 * SRG names). This adds both to GunItem (deci.ay.i) and ItemGasMaskDeci, answered by net.decimation.fixes.DurabilityBars
 * (a gun's wear, a mask's filter). Each call sits in a catch of NoClassDefFoundError: dev/libs/Decimation-base.jar is shared
 * by every checkout, and one without DurabilityBars draws no bar, as before. Neither class nor its supers declare them
 * (checked here). deci.ay.i is already patched by PatchGripRecoil and PatchGunWear: read dist/Decimation.jar (Javassist
 * reads a path as a jar only when it ends in .jar). DurabilityBars is ours: a stand in with its signatures is made here.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_bars tools/patches/PatchDurabilityBars.java
 *   java -cp tools/lib/javassist.jar:build/patch_bars PatchDurabilityBars dist/Decimation.jar build/patch_bars/out
 *   (cd build/patch_bars/out && zip <jar> deci/ay/i.class net/decimation/mod/common/item/armor/ItemGasMaskDeci.class)
 *   # for each of the three jars
 */
public class PatchDurabilityBars {
    static final String STACK = "net.minecraft.item.ItemStack";

    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass stand = pool.makeClass("net.decimation.fixes.DurabilityBars");
        stand.addMethod(CtNewMethod.make("public static boolean shown(" + STACK + " s) { return false; }", stand));
        stand.addMethod(CtNewMethod.make("public static double fraction(" + STACK + " s) { return 0.0; }", stand));

        for (String name : new String[] {"deci.ay.i", "net.decimation.mod.common.item.armor.ItemGasMaskDeci"}) {
            CtClass c = pool.get(name);
            for (CtClass k = c; k != null && !k.getName().equals("net.minecraft.item.Item"); k = superOf(k)) {
                for (CtMethod m : k.getDeclaredMethods()) {
                    if (m.getName().equals("showDurabilityBar") || m.getName().equals("getDurabilityForDisplay")) {
                        throw new IllegalStateException(k.getName() + " already declares " + m.getName());
                    }
                }
            }
            c.addMethod(CtNewMethod.make("public boolean showDurabilityBar(" + STACK + " s) { try { return "
                + "net.decimation.fixes.DurabilityBars.shown(s); } catch (java.lang.NoClassDefFoundError e) { return false; } }", c));
            c.addMethod(CtNewMethod.make("public double getDurabilityForDisplay(" + STACK + " s) { try { return "
                + "net.decimation.fixes.DurabilityBars.fraction(s); } catch (java.lang.NoClassDefFoundError e) { return 0.0; } }", c));
            c.getDeclaredMethod("showDurabilityBar");      // both are in, by name and descriptor
            c.getMethod("getDurabilityForDisplay", "(Lnet/minecraft/item/ItemStack;)D");
            c.getMethod("showDurabilityBar", "(Lnet/minecraft/item/ItemStack;)Z");
            c.writeFile(a[1]);
            System.out.println("patched " + name + ": showDurabilityBar / getDurabilityForDisplay ask DurabilityBars");
        }
    }

    static CtClass superOf(CtClass k) {
        try {
            return k.getSuperclass();
        } catch (NotFoundException e) {
            return null;
        }
    }
}
