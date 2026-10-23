import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * Javassist patch: Decimation's food, water, blood and health percents centred on their icons (user, 23 Oktober 2026:
 * "centre decimation hud percents too").
 *
 * IngameHud.render (deci.g.j b(int, int, float)) outlines six texts with RenderUtil.drawOutlined (deci.j.a
 * e(String, int, int, int)): two player name lines, already centred, and the four percents, each started at 2w - 63 at half
 * scale (y 60, 129, 199, 266), which centres only a text as wide as "100%". Every one of the six calls gets its x from
 * net.decimation.fixes.HudPercents.x(text, x, y), which moves only the percents; each call sits in a catch of
 * NoClassDefFoundError (dev/libs/Decimation-base.jar is shared by every checkout; one without HudPercents draws as
 * before). deci.g.j is already patched by PatchMenuStats and PatchTurfNames: read dist/Decimation.jar (Javassist reads a
 * path as a jar only when it ends in .jar). HudPercents is ours: a stand in with its signature is made here. It throws
 * unless it finds exactly six such calls.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_hudpct tools/patches/PatchHudPercents.java
 *   java -cp tools/lib/javassist.jar:build/patch_hudpct PatchHudPercents dist/Decimation.jar build/patch_hudpct/out
 *   (cd build/patch_hudpct/out && zip <jar> deci/g/j.class)   # for each of the three jars
 */
public class PatchHudPercents {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass stand = pool.makeClass("net.decimation.fixes.HudPercents");
        stand.addMethod(CtNewMethod.make("public static int x(String t, int x, int y) { return x; }", stand));

        CtClass hud = pool.get("deci.g.j");
        CtMethod render = hud.getMethod("b", "(IIF)V");
        int[] edited = new int[1];
        render.instrument(new javassist.expr.ExprEditor() {
            public void edit(javassist.expr.MethodCall c) throws javassist.CannotCompileException {
                if (c.getClassName().equals("deci.j.a") && c.getMethodName().equals("e")
                    && c.getSignature().equals("(Ljava/lang/String;III)V")) {
                    c.replace("{ int nx = $2; try { nx = net.decimation.fixes.HudPercents.x($1, $2, $3); }"
                              + " catch (java.lang.NoClassDefFoundError e) { } $proceed($1, nx, $3, $4); }");
                    edited[0]++;
                }
            }
        });
        if (edited[0] != 6) {
            throw new IllegalStateException("expected six drawOutlined calls in IngameHud.render, found " + edited[0]);
        }
        hud.writeFile(a[1]);
        System.out.println("patched deci.g.j: IngameHud.render's six outlined texts take their x from HudPercents.x");
    }
}
