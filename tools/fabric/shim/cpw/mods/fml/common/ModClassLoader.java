package cpw.mods.fml.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.asm.transformers.ModAPITransformer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

/**
 * FML's mod class loader on Legacy Fabric (docs/fabric_port.md): Forge's own one casts its parent to LaunchWrapper's
 * LaunchClassLoader, which Knot is not. Same API; mod jars join Knot's class path, classes load through Knot.
 * Built by tools/fabric/build_jars.py over Forge's class in forge-runtime.jar.
 */
public class ModClassLoader extends URLClassLoader
{
    private static final List<String> STANDARD_LIBRARIES = ImmutableList.of("jinput.jar", "lwjgl.jar", "lwjgl_util.jar", "rt.jar");
    private final ClassLoader knot;

    public ModClassLoader(ClassLoader parent)
    {
        super(new URL[0], null);
        this.knot = parent;
    }

    public void addFile(File modFile) throws MalformedURLException
    {
        FabricLauncherBase.getLauncher().addToClassPath(modFile.toPath());
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return knot.loadClass(name);
    }

    /** Jars FML scans for mods on the class path: none (Forge mods come from forgemods/, the Fabric side is not FML's). */
    public File[] getParentSources()
    {
        return new File[0];
    }

    public List<String> getDefaultLibraries()
    {
        return STANDARD_LIBRARIES;
    }

    /** Knot keeps no negative cache to clear. */
    public void clearNegativeCacheFor(Set<String> classList)
    {
    }

    /**
     * FML's @Optional stripper, normally registered as a LaunchWrapper transformer. Knot takes no transformers, so it
     * is filled but never applied: a mod's @Optional.Interface on a missing mod would fail to load [not seen yet:
     * neither Decimation nor DeciRemnant uses @Optional].
     */
    public ModAPITransformer addModAPITransformer(ASMDataTable dataTable)
    {
        ModAPITransformer modAPI = new ModAPITransformer();
        modAPI.initTable(dataTable);
        return modAPI;
    }
}
