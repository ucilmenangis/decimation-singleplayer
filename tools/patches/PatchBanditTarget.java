import javassist.*;

/**
 * Javassist patch for deci.ag.a (BanditEntity, also every SoldierEntity: bandits, soldiers, our survivor and military
 * tiers, garrisons, base guards; bug.md "Safezones lag", 19 Oktober 2026).
 *
 * Cause: the same as PatchTraderTarget: onUpdate calls findTarget (eA) every tick while the NPC has no target, on the
 * server and on every client's copy, and findTarget ray traces (canEntityBeSeen, func_70685_l) to every living entity
 * within 32 blocks before asking isHostileTo (c, which PatchFactions changed for the Soviets). n x n ray traces a tick
 * wherever armed NPCs stand together.
 *
 * Fix: every 10 ticks staggered by entity id, and the cheap checks first (not itself, hostile, within 32 blocks), the
 * ray trace last. Unlike traders the client copy keeps searching (throttled): it switches its stance (crouch, prone) at
 * random while it has a target, and stance is not synced from the server.
 *
 * Patch on top of the CURRENT class (PatchTracer and PatchFactions changed shootAt and c, not findTarget).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_bandit tools/patches/PatchBanditTarget.java
 *   java -cp tools/lib/javassist.jar:build/patch_bandit PatchBanditTarget dist/Decimation.jar build/patch_bandit/out
 *   (cd build/patch_bandit/out && zip <jar> deci/ag/a.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchBanditTarget {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        CtClass c = pool.get("deci.ag.a");
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
        System.out.println("patched deci.ag.a.eA (findTarget): every 10 ticks, hostile check before the ray trace");
    }
}
