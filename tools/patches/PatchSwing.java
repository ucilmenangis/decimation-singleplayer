import javassist.*;

/**
 * Javassist patch for deci.b.h (SmoothSwingThread, started by ClientProxy):
 * the original run() busy-waits with no sleep and pins one CPU core for the
 * whole session (measured: idle main menu 200% CPU -> 105% after patch).
 * Same 60 Hz accounting of deci.b.i.bz (weapon sway clock), plus a 4 ms sleep.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_swing tools/patches/PatchSwing.java
 *   java -cp tools/lib/javassist.jar:build/patch_swing PatchSwing Decimation.jar build/patch_swing/out
 *   (cd build/patch_swing/out && zip <jar> deci/b/h.class)
 *
 * The jar argument must end in .jar (Javassist treats other paths as dirs).
 */
public class PatchSwing {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]); // Decimation jar
        CtClass c = pool.get("deci.b.h");
        CtMethod run = c.getDeclaredMethod("run");
        run.setBody(
            "{"
          + "  double rate = 60.0;"
          + "  while (true) {"
          + "    long now = System.nanoTime();"
          + "    this.bo += (double)(now - this.bn) / (1.0E9 / rate);"
          + "    this.bn = now;"
          + "    while (this.bo >= 1.0) { deci.b.i.bz += 1.0f; this.bo -= 1.0; }"
          + "    try { Thread.sleep(4L); } catch (InterruptedException e) { return; }"
          + "  }"
          + "}");
        c.writeFile(a[1]);
        System.out.println("patched deci.b.h.run");
    }
}
