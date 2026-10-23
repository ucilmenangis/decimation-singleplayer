import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ListMultimap;

import cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer;
import cpw.mods.fml.common.patcher.ClassPatchManager;
import cpw.mods.fml.relauncher.Side;

/**
 * Forge 1.7.10 applied ahead of time for the Forge layer on Legacy Fabric (docs/fabric_port.md section 2), with Forge's
 * own classes doing the work (run on Java 8: the binpatches are Pack200).
 *
 *   game  VANILLA_CLIENT.jar MAPPINGS.tiny OUT.jar [OPTIFINE.jar]   vanilla + Forge's binpatches (OptiFine's patched
 *                                                       classes in place of Forge's, when given) + fml/forge access
 *                                                       transformers, official names (Legacy Fabric remaps it like vanilla)
 *   forge OUT.jar                                       the universal jar's classes (on the classpath), event classes
 *                                                       through EventSubscriptionTransformer, official names
 *   mod   IN.jar OUT.jar                                a Forge mod's event classes through EventSubscriptionTransformer
 *   strings IN.jar OUT.jar MAPPINGS.tiny                string constants naming Minecraft in SRG -> intermediary
 *
 * Classpath: this, the Forge universal jar, launchwrapper 1.12, asm-all 5.0.3, lzma 0.0.1, guava 17, log4j 2.0, and
 * for `mod` the mod jar itself (its event classes' parents are loaded).
 */
public final class Forgify
{
    public static void main(String[] args) throws Exception
    {
        if (args[0].equals("game"))
        {
            game(new File(args[1]), new File(args[2]), new File(args[3]), args.length > 4 ? new File(args[4]) : null);
        }
        else if (args[0].equals("forge"))
        {
            File universal = new File(ClassPatchManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            events(universal, new File(args[1]), true);
        }
        else if (args[0].equals("mod"))
        {
            events(new File(args[1]), new File(args[2]), false);
        }
        else if (args[0].equals("strings"))
        {
            strings(new File(args[1]), new File(args[2]), new File(args[3]));
        }
        else
        {
            throw new IllegalArgumentException(args[0]);
        }
    }

    // ---- game jar

    @SuppressWarnings("unchecked")
    private static void game(File vanilla, File tiny, File out, File optifine) throws Exception
    {
        Mappings map = new Mappings(tiny);
        Field side = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog").getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.CLIENT);                       // set by FML's launcher in a real start
        ClassPatchManager.INSTANCE.setup(Side.CLIENT);
        Field f = ClassPatchManager.class.getDeclaredField("patches");
        f.setAccessible(true);
        ListMultimap<String, ?> patches = (ListMultimap<String, ?>) f.get(ClassPatchManager.INSTANCE);
        Set<String> todo = new HashSet<String>(patches.keySet());
        Map<String, List<String[]>> ats = new HashMap<String, List<String[]>>();
        for (String at : new String[] {"fml_at.cfg", "forge_at.cfg"})
        {
            readAt(ClassPatchManager.class.getResourceAsStream("/" + at), map, ats);
        }
        int patched = 0, created = 0, widened = 0;
        ZipFile in = new ZipFile(vanilla);
        OptiFine of = optifine == null ? null : new OptiFine(optifine, in);
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(out));
        for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements();)
        {
            ZipEntry ze = e.nextElement();
            String n = ze.getName();
            if (ze.isDirectory() || n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA")))
            {
                continue;                                  // signatures: patched classes would break them
            }
            byte[] b = read(in.getInputStream(ze));
            if (n.endsWith(".class"))
            {
                String name = n.substring(0, n.length() - 6).replace('/', '.');
                byte[] ofb = of == null ? null : of.patched(n);
                if (ofb != null)
                {
                    b = ofb;                               // OptiFine's version wins over Forge's, as under Forge
                    todo.remove(name);
                }
                else if (todo.remove(name))
                {
                    b = ClassPatchManager.INSTANCE.applyPatch(name, map.srgClass(name.replace('.', '/')).replace('/', '.'), b);
                    patched++;
                }
                List<String[]> at = ats.get(name.replace('.', '/'));
                if (at != null)
                {
                    b = widen(b, at);
                    widened++;
                }
            }
            put(zo, n, b);
        }
        // classes Forge's universal jar adds inside vanilla classes, under notch names (aji$4 in Block ...)
        ZipFile universal = new ZipFile(new File(ClassPatchManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        for (Enumeration<? extends ZipEntry> e = universal.entries(); e.hasMoreElements();)
        {
            ZipEntry ze = e.nextElement();
            if (isVanillaNested(ze.getName()))
            {
                put(zo, ze.getName(), read(universal.getInputStream(ze)));
                created++;
            }
        }
        for (String name : todo)                           // classes Forge's patches create
        {
            byte[] b = ClassPatchManager.INSTANCE.applyPatch(name, map.srgClass(name.replace('.', '/')).replace('/', '.'), null);
            if (b != null && b.length > 0)
            {
                put(zo, name.replace('.', '/') + ".class", b);
                created++;
            }
        }
        if (of != null)                                    // OptiFine's own classes (Config, Reflector, shaders ...)
        {
            created += of.addClasses(zo);
        }
        zo.close();
        System.out.println("game: " + patched + " classes patched by Forge, " + (of == null ? 0 : of.patched)
                           + " by OptiFine, " + created + " created, " + widened + " widened by access transformers -> " + out);
    }

    /**
     * OptiFine 1.7.10 (optional, the user's jar): under Forge its class transformer hands out, for each class it
     * changes, its xdelta applied to the VANILLA class (MD5 checked), replacing Forge's version; those classes carry
     * Forge's hooks themselves (OptiFine's Reflector). Done here the same way with OptiFine's own Patcher, reached
     * by reflection so the build works without the jar.
     */
    private static final class OptiFine
    {
        final ZipFile zip;
        final Object patterns, cfg, provider;
        final java.lang.reflect.Method apply;
        int patched;

        OptiFine(File jar, final ZipFile vanilla) throws Exception
        {
            zip = new ZipFile(jar);
            ClassLoader cl = new java.net.URLClassLoader(new java.net.URL[] {jar.toURI().toURL()}, Forgify.class.getClassLoader());
            Class<?> patcher = Class.forName("optifine.Patcher", true, cl);
            cfg = patcher.getMethod("getConfigurationMap", ZipFile.class).invoke(null, zip);
            patterns = patcher.getMethod("getConfigurationPatterns", Map.class).invoke(null, cfg);
            Class<?> res = Class.forName("optifine.IResourceProvider", true, cl);
            provider = java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[] {res}, new java.lang.reflect.InvocationHandler()
            {
                public Object invoke(Object p, java.lang.reflect.Method m, Object[] a) throws Throwable
                {
                    ZipEntry ze = vanilla.getEntry(((String) a[0]).replaceFirst("^/", ""));   // the patch base: vanilla
                    return ze == null ? null : vanilla.getInputStream(ze);
                }
            });
            apply = patcher.getMethod("applyPatch", String.class, byte[].class, java.util.regex.Pattern[].class, Map.class, res);
        }

        /** OptiFine's version of a vanilla class entry (aho.class), or null when OptiFine leaves it. */
        byte[] patched(String entry) throws Exception
        {
            ZipEntry ze = zip.getEntry("patch/" + entry + ".xdelta");
            if (ze == null)
            {
                return null;
            }
            byte[] b = (byte[]) apply.invoke(null, entry, read(zip.getInputStream(ze)), patterns, cfg, provider);
            String want = new String(read(zip.getInputStream(zip.getEntry("patch/" + entry + ".md5"))), "ASCII").trim();
            String got = String.format("%032x", new java.math.BigInteger(1, java.security.MessageDigest.getInstance("MD5").digest(b)));
            if (!want.equals(got))
            {
                throw new IllegalStateException("OptiFine patch of " + entry + ": MD5 " + got + ", OptiFine says " + want);
            }
            patched += entry.endsWith(".class") ? 1 : 0;
            return b;
        }

        /**
         * OptiFine's own classes and assets (not its manifest), and its patched assets (the default connected glass
         * textures, xdeltas on vanilla textures, which its Forge transformer builds on demand) built here.
         */
        int addClasses(ZipOutputStream zo) throws Exception
        {
            int n = 0;
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();)
            {
                ZipEntry ze = e.nextElement();
                String name = ze.getName();
                if (name.startsWith("patch/") && name.endsWith(".xdelta") && !name.endsWith(".class.xdelta"))
                {
                    String asset = name.substring("patch/".length(), name.length() - ".xdelta".length());
                    put(zo, asset, patched(asset));
                }
                else if (!ze.isDirectory() && !name.startsWith("patch/") && !name.startsWith("META-INF/") && !name.equals("patch.cfg"))
                {
                    put(zo, name, read(zip.getInputStream(ze)));
                    n += name.endsWith(".class") ? 1 : 0;
                }
            }
            return n;
        }
    }

    /** Access transformer lines, turned to official names: {class: [access, member, desc params or null]}. */
    private static void readAt(InputStream s, Mappings map, Map<String, List<String[]>> out) throws IOException
    {
        for (String line : new String(read(s), "UTF-8").split("\n"))
        {
            int hash = line.indexOf('#');
            line = (hash >= 0 ? line.substring(0, hash) : line).trim();
            if (line.isEmpty())
            {
                continue;
            }
            String[] p = line.split("\\s+");
            if (p.length < 2)
            {
                continue;
            }
            String srgClass = p[1].replace('.', '/');
            String cls = map.officialClass(srgClass);
            if (cls == null)
            {
                continue;
            }
            String member = p.length > 2 ? p[2] : null, desc = null;
            if (member != null && member.indexOf('(') >= 0)
            {
                desc = member.substring(member.indexOf('('), member.indexOf(')') + 1).replace('.', '/');
                member = member.substring(0, member.indexOf('('));
            }
            List<String[]> l = out.get(cls);
            if (l == null)
            {
                out.put(cls, l = new ArrayList<String[]>());
            }
            l.add(new String[] {p[0], member, desc == null ? null : map.officialDesc(desc), srgClass});
        }
    }

    private static byte[] widen(byte[] b, List<String[]> at)
    {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        Mappings map = Mappings.current;
        for (String[] a : at)
        {
            if (a[1] == null)
            {
                cn.access = access(cn.access, a[0]);
                continue;
            }
            if (a[2] == null)                              // a field (or * for all fields)
            {
                for (Object o : cn.fields)
                {
                    FieldNode f = (FieldNode) o;
                    if (a[1].equals("*") || a[1].equals(map.srgField(cn.name, f.name)))
                    {
                        f.access = access(f.access, a[0]);
                    }
                }
            }
            else
            {
                for (Object o : cn.methods)
                {
                    MethodNode m = (MethodNode) o;
                    String params = m.desc.substring(0, m.desc.indexOf(')') + 1);
                    boolean name = a[1].equals("*") || a[1].equals(m.name.equals("<init>") ? "<init>" : map.srgMethod(cn.name, m.name, m.desc));
                    if (name && (a[1].equals("*") || params.equals(a[2])))
                    {
                        m.access = access(m.access, a[0]);
                    }
                }
            }
        }
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /** Forge's access transformer levels: public / protected / private, with -f (no final) or +f. */
    private static int access(int acc, String level)
    {
        int vis = level.startsWith("public") ? Opcodes.ACC_PUBLIC : level.startsWith("protected") ? Opcodes.ACC_PROTECTED
                  : level.startsWith("private") ? Opcodes.ACC_PRIVATE : 0;
        int cur = acc & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
        if (vis != 0 && rank(vis) > rank(cur))
        {
            acc = acc & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE) | vis;
        }
        if (level.endsWith("-f"))
        {
            acc &= ~Opcodes.ACC_FINAL;
        }
        else if (level.endsWith("+f"))
        {
            acc |= Opcodes.ACC_FINAL;
        }
        return acc;
    }

    private static int rank(int vis)
    {
        return vis == Opcodes.ACC_PUBLIC ? 3 : vis == Opcodes.ACC_PROTECTED ? 2 : vis == 0 ? 1 : 0;
    }

    // ---- event classes

    private static void events(File jar, File out, boolean forge) throws Exception
    {
        EventSubscriptionTransformer t = new EventSubscriptionTransformer();
        int changed = 0;
        ZipFile in = new ZipFile(jar);
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(out));
        for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements();)
        {
            ZipEntry ze = e.nextElement();
            String n = ze.getName();
            if (ze.isDirectory() || forge && (n.equals("binpatches.pack.lzma") || isVanillaNested(n)
                                               || n.startsWith("META-INF/") && !n.equals("META-INF/MANIFEST.MF")))
            {
                continue;
            }
            byte[] b = read(in.getInputStream(ze));
            if (n.endsWith(".class"))
            {
                String name = n.substring(0, n.length() - 6).replace('/', '.');
                byte[] t2 = t.transform(name, name, b);
                if (t2 != b)
                {
                    changed++;
                    b = t2;
                }
            }
            put(zo, n, b);
        }
        zo.close();
        System.out.println((forge ? "forge" : "mod") + ": " + changed + " event classes rewritten -> " + out);
    }

    // ---- strings naming Minecraft

    /**
     * Under Forge, Minecraft runs with MCP class names and SRG member names, so Forge and the mods name them in strings
     * (Class.forName("net.minecraft.init.Blocks"), reflection on "field_146297_k"); tiny-remapper leaves strings alone.
     * Every string constant (code, field constant, annotation value) that is exactly an SRG class name, dotted or
     * slashed, or an SRG member name with one intermediary name, becomes the intermediary name. Other text stays.
     */
    private static void strings(File jar, File out, File tiny) throws Exception
    {
        final Map<String, String> names = new HashMap<String, String>();
        Set<String> ambiguous = new HashSet<String>();
        for (String line : new String(Files.readAllBytes(tiny.toPath()), "UTF-8").split("\n"))
        {
            String[] p = line.split("\t");
            if (p[0].equals("c") && p.length > 3)
            {
                names.put(p[2], p[3]);
                names.put(p[2].replace('/', '.'), p[3].replace('/', '.'));
            }
            else if (p.length > 5 && (p[1].equals("f") || p[1].equals("m")) && p[4].matches("(field|func)_\\d+_\\w+"))
            {
                String had = names.put(p[4], p[5]);
                if (had != null && !had.equals(p[5]))
                {
                    ambiguous.add(p[4]);   // an srg name given to members with different intermediary names
                }
            }
        }
        names.keySet().removeAll(ambiguous);
        final int[] changed = {0, 0};
        ZipFile in = new ZipFile(jar);
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(out));
        for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements();)
        {
            ZipEntry ze = e.nextElement();
            if (ze.isDirectory())
            {
                continue;
            }
            byte[] b = read(in.getInputStream(ze));
            if (ze.getName().endsWith(".class"))
            {
                final int before = changed[0];
                ClassNode cn = new ClassNode();
                new ClassReader(b).accept(cn, 0);
                for (Object o : cn.fields)
                {
                    FieldNode f = (FieldNode) o;
                    if (f.value instanceof String && names.containsKey(f.value))
                    {
                        f.value = names.get(f.value);
                        changed[0]++;
                    }
                    annotations(f.visibleAnnotations, names, changed);
                }
                for (Object o : cn.methods)
                {
                    MethodNode m = (MethodNode) o;
                    for (java.util.Iterator<?> it = m.instructions.iterator(); it.hasNext();)
                    {
                        Object insn = it.next();
                        if (insn instanceof org.objectweb.asm.tree.LdcInsnNode)
                        {
                            org.objectweb.asm.tree.LdcInsnNode ldc = (org.objectweb.asm.tree.LdcInsnNode) insn;
                            if (ldc.cst instanceof String && names.containsKey(ldc.cst))
                            {
                                ldc.cst = names.get(ldc.cst);
                                changed[0]++;
                            }
                        }
                    }
                    annotations(m.visibleAnnotations, names, changed);
                }
                annotations(cn.visibleAnnotations, names, changed);
                if (changed[0] != before)
                {
                    ClassWriter cw = new ClassWriter(0);   // only constants change: sizes and frames stay
                    cn.accept(cw);
                    b = cw.toByteArray();
                    changed[1]++;
                }
            }
            put(zo, ze.getName(), b);
        }
        zo.close();
        System.out.println("strings: " + changed[0] + " in " + changed[1] + " classes, " + ambiguous.size()
                           + " ambiguous srg names left -> " + out);
    }

    /** String values of annotations (and of their nested annotations and arrays). */
    private static void annotations(List<?> list, Map<String, String> names, int[] changed)
    {
        if (list == null)
        {
            return;
        }
        for (Object o : list)
        {
            List<Object> values = ((org.objectweb.asm.tree.AnnotationNode) o).values;
            for (int i = 1; values != null && i < values.size(); i += 2)
            {
                values.set(i, annotationValue(values.get(i), names, changed));
            }
        }
    }

    private static Object annotationValue(Object v, Map<String, String> names, int[] changed)
    {
        if (v instanceof String && names.containsKey(v))
        {
            changed[0]++;
            return names.get(v);
        }
        if (v instanceof List)
        {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) v;
            for (int i = 0; i < l.size(); i++)
            {
                l.set(i, annotationValue(l.get(i), names, changed));
            }
        }
        else if (v instanceof org.objectweb.asm.tree.AnnotationNode)
        {
            annotations(java.util.Collections.singletonList(v), names, changed);
        }
        return v;
    }

    // ---- helpers

    /** A class of Forge's universal jar nested in a vanilla class (default package, notch outer name: aji$4). */
    private static boolean isVanillaNested(String entry)
    {
        return entry.endsWith(".class") && entry.indexOf('/') < 0 && entry.indexOf('$') > 0;
    }

    private static void put(ZipOutputStream zo, String name, byte[] b) throws IOException
    {
        zo.putNextEntry(new ZipEntry(name));
        zo.write(b);
        zo.closeEntry();
    }

    private static byte[] read(InputStream s) throws IOException
    {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int r;
        while ((r = s.read(buf)) > 0)
        {
            o.write(buf, 0, r);
        }
        s.close();
        return o.toByteArray();
    }

    /** The official / srg names of tools/fabric/mappings.py's tiny file. */
    static final class Mappings
    {
        static Mappings current;
        final Map<String, String> clsO2S = new HashMap<String, String>(), clsS2O = new HashMap<String, String>();
        final Map<String, String> fld = new HashMap<String, String>(), mth = new HashMap<String, String>();

        Mappings(File tiny) throws IOException
        {
            current = this;
            String owner = null;
            for (String line : new String(Files.readAllBytes(tiny.toPath()), "UTF-8").split("\n"))
            {
                String[] p = line.split("\t");
                if (p[0].equals("c"))
                {
                    owner = p[1];
                    clsO2S.put(p[1], p[2]);
                    clsS2O.put(p[2], p[1]);
                }
                else if (p.length > 4 && p[1].equals("f"))
                {
                    fld.put(owner + "/" + p[3], p[4]);
                }
                else if (p.length > 4 && p[1].equals("m"))
                {
                    mth.put(owner + "/" + p[3] + p[2], p[4]);
                }
            }
        }

        String srgClass(String official)
        {
            String s = clsO2S.get(official);
            return s == null ? official : s;
        }

        String officialClass(String srg)
        {
            return clsS2O.get(srg);
        }

        String srgField(String owner, String name)
        {
            String s = fld.get(owner + "/" + name);
            return s == null ? name : s;
        }

        /** The srg name of a method, looking up the class tree is not needed: AT entries name the declaring class. */
        String srgMethod(String owner, String name, String desc)
        {
            String s = mth.get(owner + "/" + name + desc);
            return s == null ? name : s;
        }

        /** A descriptor in srg class names, in official ones. */
        String officialDesc(String desc)
        {
            StringBuilder o = new StringBuilder();
            for (int i = 0; i < desc.length(); i++)
            {
                char c = desc.charAt(i);
                if (c == 'L')
                {
                    int end = desc.indexOf(';', i);
                    String cls = desc.substring(i + 1, end);
                    String off = clsS2O.get(cls);
                    o.append('L').append(off == null ? cls : off).append(';');
                    i = end;
                }
                else
                {
                    o.append(c);
                }
            }
            return o.toString();
        }
    }
}
