import javassist.*;

/**
 * Javassist patch for deci.ag.k (MechEntity, the walking mech: about 5 % of soldier spawns; dev test bench, 19 Oktober
 * 2026: one mech took 13 % of the server thread with 40 NPCs standing by).
 *
 * Cause: onUpdate calls findTarget (eA) every tick while it has no target, on the server and on every client's copy,
 * and findTarget walks the WHOLE world's loaded entity list (every entity of every loaded chunk), ray tracing
 * (canEntityBeSeen, func_70685_l) to each one within 32 blocks before asking isHostileTo (c).
 *
 * Fix: every 10 ticks staggered by entity id, only the entities in a 64 x 30 x 64 box, the cheap checks first (not
 * itself, hostile, within 32 blocks) and the ray trace last. Same targets, at most half a second later. The client copy
 * keeps searching (throttled) as the bandit's does.
 *
 * The class is unchanged from the original jar.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_mech tools/patches/PatchMechTarget.java
 *   java -cp tools/lib/javassist.jar:build/patch_mech PatchMechTarget dist/Decimation.jar build/patch_mech/out
 *   (cd build/patch_mech/out && zip <jar> deci/ag/k.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchMechTarget {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        CtClass c = pool.get("deci.ag.k");
        CtMethod find = c.getDeclaredMethod("eA");
        find.setBody(
            "{"
          + "  if ((this.field_70173_aa + this.func_145782_y()) % 10 != 0) { return; }"     // twice a second, staggered
          + "  java.util.List list = this.field_70170_p.func_72872_a(net.minecraft.entity.EntityLivingBase.class,"
          + "    net.minecraft.util.AxisAlignedBB.func_72330_a(this.field_70165_t - 32.0, this.field_70163_u - 15.0,"
          + "      this.field_70161_v - 32.0, this.field_70165_t + 32.0, this.field_70163_u + 15.0, this.field_70161_v + 32.0));"
          + "  for (int i = 0; i < list.size(); i++) {"
          + "    net.minecraft.entity.EntityLivingBase t = (net.minecraft.entity.EntityLivingBase) list.get(i);"
          + "    if (t == this || !this.c(t)) { continue; }"                                  // isHostileTo: cheap first
          + "    if (this.func_70032_d(t) > 32.0f) { continue; }"                            // getDistanceToEntity
          + "    if (!this.func_70685_l(t)) { continue; }"                                   // canEntityBeSeen: ray trace last
          + "    this.field_70789_a = t;"                                                    // entityToAttack
          + "    this.func_70604_c(t);"                                                      // setRevengeTarget
          + "    return;"
          + "  }"
          + "}");
        c.writeFile(a[1]);
        System.out.println("patched deci.ag.k.eA (mech findTarget): a 32 block box every 10 ticks, hostile check before the ray trace");
    }
}
