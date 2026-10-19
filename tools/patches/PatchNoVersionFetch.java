import javassist.*;

/**
 * Javassist patch for net.decimation.mod.common.utils.h (UpdateChecker; bug.md "FPS falls minute by minute", 19 Oktober
 * 2026): tickVersionCheck (gH) starts a new thread every 60 s, on the client and on a dedicated server, that reads
 * BoehMod's dead raw.githubusercontent.com/boehmod/DecimationRelease/master/latestversion.txt (404) and prints a 20 line
 * stack trace. Its answer is no longer used: isUpdateRequired (gI) asks our Updater since PatchNoUpdate v2. Now it does
 * nothing.
 *
 * Patch on top of the CURRENT class (PatchNoUpdate changed it): all three jars carry the same h.class.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_nofetch tools/patches/PatchNoVersionFetch.java
 *   java -cp tools/lib/javassist.jar:build/patch_nofetch PatchNoVersionFetch dist/Decimation.jar build/patch_nofetch/out
 *   (cd build/patch_nofetch/out && zip <jar> net/decimation/mod/common/utils/h.class)   # the three jars
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchNoVersionFetch {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]); // Decimation jar
        CtClass c = pool.get("net.decimation.mod.common.utils.h");
        c.getDeclaredMethod("gH").setBody("{ }");
        c.writeFile(a[1]);
        System.out.println("patched UpdateChecker.tickVersionCheck (gH): no fetch from BoehMod's dead release file");
    }
}
