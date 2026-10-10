import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM patch: the stat labels of the main menu and the in game HUD (user request 11 Oktober 2026:
 * "players kill change to humans kill", "stats on hud replace with local kills").
 * net.decimation.fixes.LocalStats fills the numbers (the profile and totals the dead backend
 * used to send); here only the words change, in the string constants:
 *
 * - GuiMenuBase deci.i.a: "Player Kills: " -> "Human Kills: ", "Player Deaths: " -> "Deaths: ",
 *   "Total Player Casualties: " -> "Total Human Kills: ",
 *   "Total Infected Casualties: " -> "Total Infected Kills: ";
 * - GuiMenuHome deci.i.e and IngameHud deci.g.j: "Player Kills: " -> "Human Kills: ".
 *
 * None of the three classes is patched by anything else. Run on each of the three jars
 * (Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar):
 *
 *   ASM=~/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-debug-all/5.0.3/f9e3*.../asm-debug-all-5.0.3.jar
 *   javac -cp $ASM --release 8 -d build/patch_menu tools/patches/PatchMenuStats.java
 *   java -cp $ASM:build/patch_menu PatchMenuStats dist/Decimation.jar build/patch_menu/out
 *   (cd build/patch_menu/out && zip <jar> deci/i/a.class deci/i/e.class deci/g/j.class)
 */
public class PatchMenuStats
{
    public static void main(String[] a) throws Exception
    {
        final Map<String, String> words = new HashMap<String, String>();
        words.put("Player Kills: ", "Human Kills: ");
        words.put("Player Deaths: ", "Deaths: ");
        words.put("Total Player Casualties: ", "Total Human Kills: ");
        words.put("Total Infected Casualties: ", "Total Infected Kills: ");
        ZipFile jar = new ZipFile(a[0]);
        for (String cls : new String[] {"deci/i/a", "deci/i/e", "deci/g/j"})
        {
            ZipEntry e = jar.getEntry(cls + ".class");
            byte[] in = read(jar.getInputStream(e));
            ClassReader r = new ClassReader(in);
            ClassWriter w = new ClassWriter(0);
            final int[] changed = {0};
            r.accept(new ClassVisitor(Opcodes.ASM5, w)
            {
                @Override
                public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex)
                {
                    return new MethodVisitor(Opcodes.ASM5, super.visitMethod(acc, name, desc, sig, ex))
                    {
                        @Override
                        public void visitLdcInsn(Object cst)
                        {
                            if (cst instanceof String && words.containsKey(cst))
                            {
                                changed[0]++;
                                cst = words.get(cst);
                            }
                            super.visitLdcInsn(cst);
                        }
                    };
                }
            }, 0);
            File out = new File(a[1], cls + ".class");
            out.getParentFile().mkdirs();
            FileOutputStream o = new FileOutputStream(out);
            o.write(w.toByteArray());
            o.close();
            System.out.println(cls + ": " + changed[0] + " labels");
        }
        jar.close();
    }

    private static byte[] read(InputStream in) throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; )
        {
            b.write(buf, 0, n);
        }
        in.close();
        return b.toByteArray();
    }
}
