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
        packet.getDeclaredConstructor(new CtClass[] {CtClass.intType}).insertAfter("{ this.targetId = nextTarget; }");
        packet.getMethod("toBytes", "(Lio/netty/buffer/ByteBuf;)V").insertAfter("{ $1.writeInt(this.targetId); }");
        packet.getMethod("fromBytes", "(Lio/netty/buffer/ByteBuf;)V")
            .insertAfter("{ this.targetId = $1.readableBytes() >= 4 ? $1.readInt() : -1; }");
        packet.writeFile(a[1]);

        CtClass handler = pool.get("deci.aE.a$B$a");
        CtMethod handle = handler.getMethod("a",
            "(Ldeci/aE/a$B;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;");
        handle.insertBefore("{ deci.aE.a$B.handlingTarget = $1.targetId; }");
        handle.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("func_70676_i")) { // getLook(float) of the shooter
                    m.replace(
                        "{ int t = deci.aE.a$B.handlingTarget;"
                      + "  net.minecraft.entity.Entity e = t < 0 ? null"
                      + "    : net.minecraft.client.Minecraft.func_71410_x().field_71441_e.func_73045_a(t);"
                      + "  if (e == null) { $_ = $proceed($$); } else {"
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
            shoot.insertBefore("{ deci.aE.a$B.nextTarget = $1 == null ? -1 : $1.func_145782_y(); }");
            shoot.insertAfter("{ deci.aE.a$B.nextTarget = -1; }", true);
            c.writeFile(a[1]);
        }
        System.out.println("patched PacketGunFireEffects (+target id), its handler (tracer toward the target), "
                           + "BanditEntity.shootAt, ArmedTraderEntity.shootAt");
    }
}
