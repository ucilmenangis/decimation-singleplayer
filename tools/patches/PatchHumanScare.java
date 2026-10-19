import javassist.*;

/**
 * Javassist patch for deci.ah.d (FactionHumanEntity: every civilian, bandit, soldier, trader and guard; bug.md "Every
 * human NPC walked all entities", 19 Oktober 2026).
 *
 * Cause: onUpdate (func_70071_h_) walked the world's WHOLE loaded entity list every tick, on the server and on every
 * client's copy, measuring the distance to each entity, to know whether someone within 32 blocks holds a gun (the NPC
 * then looks scared: setGesture, G); and it used that answer only when Math.random() < 0.1. Humans x all loaded
 * entities, twice (both sides), every tick; nine of ten scans thrown away.
 *
 * Fix: the scan runs only in that one in ten branch, over a 64 block box (func_72872_a) instead of the whole list, and
 * stops at the first gun; no fear (eU) skips it. Same answer, same Math.random() calls in the same order (blink first,
 * then the gesture), so the NPCs behave as before.
 *
 * The class is unchanged from the original jar.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_scare tools/patches/PatchHumanScare.java
 *   java -cp tools/lib/javassist.jar:build/patch_scare PatchHumanScare dist/Decimation.jar build/patch_scare/out
 *   (cd build/patch_scare/out && zip <jar> deci/ah/d.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchHumanScare {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        CtClass c = pool.get("deci.ah.d");
        CtMethod update = c.getDeclaredMethod("func_70071_h_", new CtClass[0]);
        update.setBody(
            "{"
          + "  super.func_70071_h_();"
          + "  if (this.ZX > 0) { this.ZX = this.ZX - 1; }"                                // blink ticks
          + "  if (Math.random() < 0.03) { this.ev(); }"                                   // startBlink
          + "  if (Math.random() < 0.1) {"
          + "    int n = 0;"
          + "    if (this.field_70170_p != null && !this.eU()) {"                         // isHasNoFear
          + "      java.util.List list = this.field_70170_p.func_72872_a(net.minecraft.entity.EntityLivingBase.class,"
          + "        net.minecraft.util.AxisAlignedBB.func_72330_a(this.field_70165_t - 32.0, this.field_70163_u - 32.0,"
          + "          this.field_70161_v - 32.0, this.field_70165_t + 32.0, this.field_70163_u + 32.0, this.field_70161_v + 32.0));"
          + "      for (int i = 0; i < list.size() && n == 0; i++) {"
          + "        net.minecraft.entity.EntityLivingBase e = (net.minecraft.entity.EntityLivingBase) list.get(i);"
          + "        if (this.func_70032_d(e) > 32.0f) { continue; }"
          + "        net.minecraft.item.ItemStack h = e.func_70694_bm();"
          + "        if (h == null || !(h.func_77973_b() instanceof deci.ay.i)) { continue; }"
          + "        if (e instanceof net.minecraft.entity.player.EntityPlayer"
          + "            && deci.Q.b.e((net.minecraft.entity.player.EntityPlayer) e).cd() >= 50) { continue; }"
          + "        n = 1;"
          + "      }"
          + "    }"
          + "    this.G(n);"                                                                // setGesture
          + "  }"
          + "}");
        c.writeFile(a[1]);
        System.out.println("patched deci.ah.d.onUpdate: the gun scare scan only when it is used, over a 64 block box");
    }
}
