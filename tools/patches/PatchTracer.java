import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Javassist patch: NPC tracers fly in the wrong direction (old bug, also in
 * the original game; reported by the user 2026-10-09: an NPC shooting forward
 * draws its tracer to the left, right or behind).
 *
 * Cause: an NPC shot (BanditEntity.shootAt deci.ag.a.e, inherited by the
 * soldiers; ArmedTraderEntity.shootAt deci.ai.v.e) damages its target
 * directly on the server and sends PacketGunFireEffects (deci.aE.a$B) with
 * the shooter's entity id only. The client handler (deci.aE.a$B$a.a) draws
 * the tracer along the shooter's getLook() (func_70676_i), which for a mob is
 * its BODY facing, not where it aims: a strafing / backing / turning NPC
 * shoots its target while the tracer goes sideways or behind.
 *
 * Fix: the packet also carries the target's entity id (written after the
 * shooter id, read only when present, so an unpatched sender still works;
 * -1 = none). The two shootAt methods record their target in a static
 * nextTarget that the packet constructor picks up (the server thread builds
 * the packet inside shootAt). The handler's getLook() for a packet with a
 * target becomes the direction from the shooter's eyes to the target's chest;
 * players (no target sent) keep their own look.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_tracer tools/patches/PatchTracer.java
 *   java -cp tools/lib/javassist.jar:build/patch_tracer PatchTracer <Decimation jar ending .jar> build/patch_tracer/out <netty-all-4.0.10.Final.jar>
 *   (cd build/patch_tracer/out && zip <jar> 'deci/aE/a$B.class' 'deci/aE/a$B$a.class' deci/ag/a.class deci/ai/v.class)
 * Patch from the ORIGINAL classes (check they are unchanged in the target jar).
 * Runtime names are SRG (getEntityId = func_145782_y, posX = field_70165_t...).
 *
 * v2 (2026-10-09, user: NPC shots should hit only when the tracer does):
 * - the packet can also carry an AIM POINT (hasAim, aimX/Y/Z; written after
 *   the target id, read only when present). The handler's direction is then
 *   the line from the shooter's eyes to that point, before the target rule.
 *   A sender sets the static nextHasAim / nextAimX.. before building it.
 * - BanditEntity gets `public static Object shotHook` (a
 *   java.util.function.BiFunction(shooter, target)). When set, shootAt asks it
 *   once its cooldown has run out; a non null answer means the hook fired the
 *   shot itself (our mod: a traced shot with spread, damage only on a hit,
 *   impact particles, the packet with its aim point), and shootAt only resets
 *   its cooldown. Unset (no deciworldgen), shootAt is Decimation's own.
 * - the handler shows a traced shot's tracer always (Decimation draws 40% of
 *   tracers and makes 60% invisible).
 * Rebuild from the ORIGINAL classes (v1 is included here), then PatchFactions.
 */
public class PatchTracer {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        pool.insertClassPath(a[2]); // netty-all 4.0.10 (Prism libraries): ByteBuf of toBytes / fromBytes

        CtClass packet = pool.get("deci.aE.a$B");
        packet.addField(CtField.make("public int targetId;", packet), CtField.Initializer.constant(-1));
        packet.addField(CtField.make("public static int nextTarget;", packet), CtField.Initializer.constant(-1));
        // set by the server side constructor (shootAt fills nextTarget first)
        packet.addField(CtField.make("public static int handlingTarget;", packet), CtField.Initializer.constant(-1));
        for (String f : new String[] {"public boolean hasAim;", "public float aimX;", "public float aimY;",
                                      "public float aimZ;", "public static boolean nextHasAim;",
                                      "public static float nextAimX;", "public static float nextAimY;",
                                      "public static float nextAimZ;", "public static boolean handlingHasAim;",
                                      "public static float handlingAimX;", "public static float handlingAimY;",
                                      "public static float handlingAimZ;"}) {
            packet.addField(CtField.make(f, packet));
        }
        packet.getDeclaredConstructor(new CtClass[] {CtClass.intType}).insertAfter(
            "{ this.targetId = nextTarget; this.hasAim = nextHasAim;"
          + "  this.aimX = nextAimX; this.aimY = nextAimY; this.aimZ = nextAimZ; }");
        packet.getMethod("toBytes", "(Lio/netty/buffer/ByteBuf;)V").insertAfter(
            "{ $1.writeInt(this.targetId); $1.writeBoolean(this.hasAim);"
          + "  if (this.hasAim) { $1.writeFloat(this.aimX); $1.writeFloat(this.aimY); $1.writeFloat(this.aimZ); } }");
        packet.getMethod("fromBytes", "(Lio/netty/buffer/ByteBuf;)V").insertAfter(
            "{ this.targetId = $1.readableBytes() >= 4 ? $1.readInt() : -1;"
          + "  this.hasAim = $1.readableBytes() >= 13 && $1.readBoolean();"
          + "  if (this.hasAim) { this.aimX = $1.readFloat(); this.aimY = $1.readFloat(); this.aimZ = $1.readFloat(); } }");
        packet.writeFile(a[1]);

        CtClass handler = pool.get("deci.aE.a$B$a");
        CtMethod handle = handler.getMethod("a",
            "(Ldeci/aE/a$B;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;");
        handle.insertBefore("{ deci.aE.a$B.handlingTarget = $1.targetId; deci.aE.a$B.handlingHasAim = $1.hasAim;"
          + "  deci.aE.a$B.handlingAimX = $1.aimX; deci.aE.a$B.handlingAimY = $1.aimY;"
          + "  deci.aE.a$B.handlingAimZ = $1.aimZ; }");
        handle.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("java.lang.Math") && m.getMethodName().equals("random")) {
                    // its only random call: 40% GENERIC tracer, else INVIS. A traced NPC shot (aim point)
                    // always shows its tracer, so what you see is every bullet (v2)
                    m.replace("{ $_ = deci.aE.a$B.handlingHasAim ? 0.0 : $proceed($$); }");
                    return;
                }
                if (m.getMethodName().equals("func_70676_i")) { // getLook(float) of the shooter
                    m.replace(
                        "{ double ax = deci.aE.a$B.handlingAimX - $0.field_70165_t;"
                      + "  double ay = deci.aE.a$B.handlingAimY - ($0.field_70163_u + $0.func_70047_e());"
                      + "  double az = deci.aE.a$B.handlingAimZ - $0.field_70161_v;"
                      + "  double al = Math.sqrt(ax * ax + ay * ay + az * az);"
                      + "  int t = deci.aE.a$B.handlingHasAim && al > 1.0E-4 ? -2 : deci.aE.a$B.handlingTarget;"
                      + "  net.minecraft.entity.Entity e = t < 0 ? null"
                      + "    : net.minecraft.client.Minecraft.func_71410_x().field_71441_e.func_73045_a(t);"
                      + "  if (t == -2) { $_ = net.minecraft.util.Vec3.func_72443_a(ax / al, ay / al, az / al); }"
                      + "  else if (e == null) { $_ = $proceed($$); } else {"
                      + "    double dx = e.field_70165_t - $0.field_70165_t;"
                      // chest: bounding box bottom + 0.6 x height (posY of the
                      // client's own player is at eye height in 1.7.10)
                      + "    double dy = e.field_70121_D.field_72338_b + e.field_70131_O * 0.6 - ($0.field_70163_u + $0.func_70047_e());"
                      + "    double dz = e.field_70161_v - $0.field_70161_v;"
                      + "    double l = Math.sqrt(dx * dx + dy * dy + dz * dz);"
                      + "    if (l < 1.0E-4) { $_ = $proceed($$); }"
                      + "    else { $_ = net.minecraft.util.Vec3.func_72443_a(dx / l, dy / l, dz / l); }"
                      + "  } }");
                }
            }
        });
        handler.writeFile(a[1]);

        String[][] shooters = {{"deci.ag.a", "e"}, {"deci.ai.v", "e"}}; // BanditEntity / ArmedTraderEntity.shootAt
        for (String[] s : shooters) {
            CtClass c = pool.get(s[0]);
            CtMethod shoot = c.getMethod(s[1], "(Lnet/minecraft/entity/EntityLivingBase;)V");
            if (s[0].equals("deci.ag.a")) {
                // v2 shot hook; inserted first, so the nextTarget line below runs before it
                // and the finally reset also covers its return. aau shotCooldown,
                // aaw minShotDelay, aav maxShotDelay; field_70170_p worldObj, field_72995_K isRemote
                c.addField(CtField.make("public static Object shotHook;", c));
                shoot.insertBefore(
                    "{ if (shotHook != null && !this.field_70170_p.field_72995_K && this.aau <= 0) {"
                  + "    Object r = ((java.util.function.BiFunction) shotHook).apply(this, $1);"
                  + "    if (r != null) { this.aau = this.aaw + new java.util.Random().nextInt(this.aav - this.aaw + 1); return; }"
                  + "  } }");
            }
            shoot.insertBefore("{ deci.aE.a$B.nextTarget = $1 == null ? -1 : $1.func_145782_y(); }");
            shoot.insertAfter("{ deci.aE.a$B.nextTarget = -1; }", true);
            c.writeFile(a[1]);
        }
        System.out.println("patched PacketGunFireEffects (+target id, +aim point), its handler (tracer toward the "
                           + "aim point / target), BanditEntity.shootAt (+shotHook), ArmedTraderEntity.shootAt");
    }
}
