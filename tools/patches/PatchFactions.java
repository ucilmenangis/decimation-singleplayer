import javassist.*;

/**
 * Javassist patch: Soviets kill each other (user report 2026-10-09, once our
 * MilitarySpawner put them in groups).
 *
 * Cause: SovietEntity.isHostileTo (deci.ag.m.c) returns, for another armed
 * human (HumanEntity2 deci.ah.d), faction == SOVIET: hostile ONLY to Soviets,
 * the opposite of what an enemy faction means (they ignored bandits and
 * soldiers and shot each other). Bandits (deci.ag.a.c) and soldiers
 * (deci.ag.l.c, also hazmat soldiers) never counted Soviets as enemies.
 *
 * Fix: Soviets are hostile to every other armed human except Soviets and
 * traders (TraderEntity deci.ai.a, ArmedTraderEntity deci.ai.v); bandits and
 * soldiers are hostile to Soviets, so fights go both ways. Players, infected
 * and everything else keep Decimation's rules.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_factions tools/patches/PatchFactions.java
 *   java -cp tools/lib/javassist.jar:build/patch_factions PatchFactions <PatchTracer out dir> <ORIGINAL Decimation jar> build/patch_factions/out
 *   (cd build/patch_factions/out && zip <jar> deci/ag/a.class deci/ag/l.class deci/ag/m.class)
 * deci/ag/a.class is also patched by PatchTracer: run that first and pass its
 * out dir, so this patch builds on it.
 */
public class PatchFactions {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[1]);
        pool.insertClassPath(a[0]); // first: PatchTracer's deci/ag/a
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        String soviet = "($1 instanceof deci.ah.d && ((deci.ah.d) $1).eV() == deci.ae.b.SOVIET)";
        CtClass sov = pool.get("deci.ag.m");
        sov.getMethod("c", "(Lnet/minecraft/entity/EntityLivingBase;)Z").insertBefore(
            "{ if ($1 instanceof deci.ah.d) {"
          + "    return ((deci.ah.d) $1).eV() != deci.ae.b.SOVIET && !($1 instanceof deci.ai.a) && !($1 instanceof deci.ai.v);"
          + "  } }");
        sov.writeFile(a[2]);
        for (String cls : new String[] {"deci.ag.a", "deci.ag.l"}) { // BanditEntity, SoldierEntity
            CtClass c = pool.get(cls);
            c.getMethod("c", "(Lnet/minecraft/entity/EntityLivingBase;)Z").insertBefore(
                "{ if " + soviet + " { return true; } }");
            c.writeFile(a[2]);
        }
        System.out.println("patched SovietEntity / BanditEntity / SoldierEntity.isHostileTo (Soviets vs everyone else)");
    }
}
