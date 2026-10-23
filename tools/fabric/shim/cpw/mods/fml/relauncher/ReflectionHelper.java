package cpw.mods.fml.relauncher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge's ReflectionHelper on Legacy Fabric (docs/fabric_port.md): the same, except that a name may be an SRG name
 * that intermediary splits over several members (an override with a narrower type gets its own name: func_110813_b
 * is method_5781 in RendererLivingEntity and method_5791 in RenderLiving). Such a string is left as it is by
 * Forgify strings; here each of its intermediary names is tried on the class asked for (forgelayer/srg_ambiguous.txt,
 * written by tools/fabric/build_jars.py). Built over Forge's class in forge-runtime.jar.
 */
public class ReflectionHelper
{
    private static Map<String, String[]> ambiguous;

    /** The names to try: each name, then the intermediary names of an ambiguous SRG one. */
    private static synchronized String[] candidates(String[] names)
    {
        if (ambiguous == null)
        {
            ambiguous = new HashMap<String, String[]>();
            InputStream in = ReflectionHelper.class.getResourceAsStream("/forgelayer/srg_ambiguous.txt");
            if (in == null)
            {
                throw new IllegalStateException("forgelayer/srg_ambiguous.txt missing from forge-runtime.jar");
            }
            try
            {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                for (String line; (line = r.readLine()) != null;)
                {
                    String[] p = line.split("\t");
                    ambiguous.put(p[0], p[1].split(","));
                }
                r.close();
            }
            catch (IOException e)
            {
                throw new IllegalStateException("forgelayer/srg_ambiguous.txt unreadable", e);
            }
        }
        List<String> out = new ArrayList<String>(Arrays.asList(names));
        for (String n : names)
        {
            String[] more = ambiguous.get(n);
            if (more != null)
            {
                out.addAll(Arrays.asList(more));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public static Field findField(Class<?> clazz, String... fieldNames)
    {
        Exception failed = null;
        for (String fieldName : candidates(fieldNames))
        {
            try
            {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            }
            catch (Exception e)
            {
                failed = e;
            }
        }
        throw new UnableToFindFieldException(fieldNames, failed);
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, int fieldIndex)
    {
        try
        {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            return (T) f.get(instance);
        }
        catch (Exception e)
        {
            throw new UnableToAccessFieldException(new String[0], e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String... fieldNames)
    {
        try
        {
            return (T) findField(classToAccess, fieldNames).get(instance);
        }
        catch (Exception e)
        {
            throw new UnableToAccessFieldException(fieldNames, e);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, int fieldIndex)
    {
        try
        {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            f.set(instance, value);
        }
        catch (Exception e)
        {
            throw new UnableToAccessFieldException(new String[0], e);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String... fieldNames)
    {
        try
        {
            findField(classToAccess, fieldNames).set(instance, value);
        }
        catch (Exception e)
        {
            throw new UnableToAccessFieldException(fieldNames, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Class<? super Object> getClass(ClassLoader loader, String... classNames)
    {
        Exception err = null;
        for (String className : classNames)
        {
            try
            {
                return (Class<? super Object>) Class.forName(className, false, loader);
            }
            catch (Exception e)
            {
                err = e;
            }
        }
        throw new UnableToFindClassException(classNames, err);
    }

    public static <E> Method findMethod(Class<? super E> clazz, E instance, String[] methodNames, Class<?>... methodTypes)
    {
        Exception failed = null;
        for (String methodName : candidates(methodNames))
        {
            try
            {
                Method m = clazz.getDeclaredMethod(methodName, methodTypes);
                m.setAccessible(true);
                return m;
            }
            catch (Exception e)
            {
                failed = e;
            }
        }
        throw new UnableToFindMethodException(methodNames, failed);
    }

    public static class UnableToFindFieldException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private String[] fieldNameList;

        public UnableToFindFieldException(String[] fieldNameList, Exception e)
        {
            super(e);
            this.fieldNameList = fieldNameList;
        }
    }

    public static class UnableToAccessFieldException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private String[] fieldNameList;

        public UnableToAccessFieldException(String[] fieldNames, Exception e)
        {
            super(e);
            this.fieldNameList = fieldNames;
        }
    }

    public static class UnableToFindClassException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private String[] classNames;

        public UnableToFindClassException(String[] classNames, Exception err)
        {
            super(err);
            this.classNames = classNames;
        }
    }

    public static class UnableToFindMethodException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private String[] methodNames;

        public UnableToFindMethodException(String[] methodNames, Exception failed)
        {
            super(failed);
            this.methodNames = methodNames;
        }
    }
}
