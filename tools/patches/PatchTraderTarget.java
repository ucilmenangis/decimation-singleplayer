import javassist.*;

/**
 * Javassist patch for deci.ai.v (ArmedTraderEntity: every trader and the trader guards; bug.md "Safezones lag",
 * 19 Oktober 2026, the user's friend: "when in safezone its really laggy, i think its caused by traders or npc").
 *
 * Cause: onUpdate calls findTarget (eA) EVERY tick while the trader has no target, on the server AND on every client's
 * copy (no isRemote check), and findTarget lists every living entity in a 64 x 30 x 64 box and ray traces to each
 * (canEntityBeSeen, func_70685_l) BEFORE asking whether it is hostile at all (isHostileTarget, c). In a camp or refuge
 * every armed resident ray traced to every other resident and player 20 times a second: n x n ray traces per tick on the
 * host and on each joiner (JFR: deci/ai/v.eA and World.rayTraceBlocks on the client thread inside a refuge).
 *
 * Fix: the server only (the client copy never shoots: shootAt is server side, the server sends the shot effects), every
 * 10 ticks staggered by entity id, and the cheap checks first (not itself, hostile, within 32 blocks), the ray trace
 * last. Same targets, same range, at most half a second later.
 *
 * Patch on top of the CURRENT class (PatchTracer and PatchFactions changed its shootAt, not findTarget).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_trader tools/patches/PatchTraderTarget.java
 *   java -cp tools/lib/javassist.jar:build/patch_trader PatchTraderTarget dist/Decimation.jar build/patch_trader/out
 *   (cd build/patch_trader/out && zip <jar> deci/ai/v.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchTraderTarget {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        CtClass c = pool.get("deci.ai.v");
        CtMethod find = c.getDeclaredMethod("eA");
        find.setBody(
            "{"
          + "  if (this.field_70170_p.field_72995_K) { return; }"                            // client copy: never shoots
          + "  if ((this.field_70173_aa + this.func_145782_y()) % 10 != 0) { return; }"     // twice a second, staggered
          + "  java.util.List list = this.field_70170_p.func_72872_a(net.minecraft.entity.EntityLivingBase.class,"
          + "    net.minecraft.util.AxisAlignedBB.func_72330_a(this.field_70165_t - 32.0, this.field_70163_u - 15.0,"
          + "      this.field_70161_v - 32.0, this.field_70165_t + 32.0, this.field_70163_u + 15.0, this.field_70161_v + 32.0));"
          + "  for (int i = 0; i < list.size(); i++) {"
          + "    net.minecraft.entity.EntityLivingBase t = (net.minecraft.entity.EntityLivingBase) list.get(i);"
          + "    if (t == this || !this.c(t)) { continue; }"                                  // isHostileTarget: cheap first
          + "    if (this.func_70032_d(t) > 32.0f) { continue; }"                            // getDistanceToEntity
          + "    if (!this.func_70685_l(t)) { continue; }"                                   // canEntityBeSeen: ray trace last
          + "    this.field_70789_a = t;"                                                    // entityToAttack
          + "    this.func_70604_c(t);"                                                      // setRevengeTarget
          + "    return;"
          + "  }"
          + "}");
        c.writeFile(a[1]);
        System.out.println("patched deci.ai.v.eA (findTarget): server only, every 10 ticks, hostile check before the ray trace");
    }
}
