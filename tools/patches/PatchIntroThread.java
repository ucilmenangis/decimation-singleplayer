import javassist.*;

/**
 * Javassist patch for deci.i.d (GuiIntroJumpscare, the boot intro; bug.md "FPS falls minute by minute", 19 Oktober 2026):
 * its static initializer starts a thread (body: private static aS) that counts the intro's frames at 30 Hz by busy waiting
 * (while (System.nanoTime() < next) {}) and never ends, so one CPU core is at full load for the whole session, also after
 * the intro (dev test `threads`: Thread-13 99% of a core in deci.i.d.aS). Laptops heat up and slow down after minutes.
 * Same frame counting (iI += 1 while bl, "started"), but it sleeps until the next frame and returns once iH ("finished")
 * is set: at once in our jars, where the intro is skipped (finished starts true).
 *
 * Patch on top of the CURRENT class (the intro skip already changed it): all three jars carry the same deci/i/d.class.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_intro tools/patches/PatchIntroThread.java
 *   java -cp tools/lib/javassist.jar:build/patch_intro PatchIntroThread dist/Decimation.jar build/patch_intro/out
 *   (cd build/patch_intro/out && zip <jar> deci/i/d.class)   # Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchIntroThread {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]); // Decimation jar
        CtClass c = pool.get("deci.i.d");
        CtMethod run = c.getDeclaredMethod("aS");
        run.setBody(
            "{"
          + "  long start = System.nanoTime();"
          + "  long frame = 0L;"
          + "  while (!iH) {"
          + "    if (bl) { iI = iI + 1.0; }"
          + "    long wait = start + frame * 1000000000L / 30L - System.nanoTime();"
          + "    if (wait > 0L) {"
          + "      try { Thread.sleep(wait / 1000000L, (int) (wait % 1000000L)); } catch (InterruptedException e) { return; }"
          + "    }"
          + "    frame++;"
          + "  }"
          + "}");
        c.writeFile(a[1]);
        System.out.println("patched deci.i.d.aS (intro frame thread sleeps, ends when the intro is finished)");
    }
}
