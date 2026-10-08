import javassist.*;

/**
 * Javassist patch: switch off Decimation's picture in picture scope (user
 * request 2026-10-08: scoped guns cost about 40 fps, 150 -> 110).
 *
 * The original scope (kept intact, only gated): ClientRenderHandler
 * (deci.c.b) renderScopeView a(Float) renders the whole world a second time
 * with a narrow FOV into a texture of up to 1024 x 1024 EVERY frame a gun
 * with a scope is held (aiming or not), and BModel (deci.n.f)
 * renderScopeGlass b(deci.n.b) draws that texture on the scope glass.
 *
 * Patch: renderScopeView returns at once unless the system property
 * decimation.scope.pip is "true". The rest of the method is untouched, so
 * the old scope comes back with that one switch (deciworldgen's
 * config/deciworldgen_scope.cfg "pictureInPicture" sets the property). With
 * the switch off, net.decimation.fixes.ScopeZoom zooms the view (FOV) while
 * aiming and copies the centre of each finished frame into the scope
 * texture, which renderScopeGlass (unpatched) still draws on the glass.
 * (A first version also gated renderScopeGlass: the glass then vanished and
 * the scope showed its solid black body.)
 *
 * Self-contained: the patched class references no deciworldgen class, so
 * Decimation still loads without our mod (then the glass shows a still
 * texture and there is no zoom).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_scope tools/patches/PatchScope.java
 *   java -cp tools/lib/javassist.jar:build/patch_scope PatchScope <Decimation jar ending .jar> build/patch_scope/out
 *   (cd build/patch_scope/out && zip <jar> deci/c/b.class)
 * Patch from the ORIGINAL classes (check they are unchanged in the target jar).
 */
public class PatchScope {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");
        String gate = "{ if (!Boolean.getBoolean(\"decimation.scope.pip\")) return; }";

        CtClass handler = pool.get("deci.c.b");
        handler.getMethod("a", "(Ljava/lang/Float;)V").insertBefore(gate);
        handler.writeFile(a[1]);
        System.out.println("patched deci.c.b.a(Float) renderScopeView");
    }
}
