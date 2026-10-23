package net.decimation.forgelayer;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * The Forge 1.7.10 layer on Legacy Fabric (docs/fabric_port.md). Before Minecraft starts it does what FML's tweakers
 * and coremod manager do under LaunchWrapper: the shared blackboard, FML's version data and the Forge mods to load
 * (forgemods/). Forge's patched Minecraft then runs FML's own lifecycle (beginMinecraftLoading -> Loader.loadMods).
 * Forge is reached by reflection: this project does not compile against it.
 */
public final class ForgeLayer implements PreLaunchEntrypoint
{
	/** FML's version data (fmlversion.properties of Forge 10.13.4.1614): major, minor, revision, build, Minecraft, MCP. */
	private static final String[] FML_VERSION = {"7", "99", "40", "1614", "1.7.10", "9.05"};
	/** What FML's two core plugins give as their mod containers (FMLCorePlugin, FMLForgePlugin). */
	private static final String[] CONTAINERS = {"cpw.mods.fml.common.FMLContainer", "net.minecraftforge.common.ForgeModContainer"};

	@Override
	public void onPreLaunch()
	{
		try
		{
			File gameDir = FabricLoader.getInstance().getGameDir().toFile();
			// FMLLaunchHandler.setupClient: the side FML logs and strips @SideOnly by, and the log's home
			Object client = Enum.valueOf(Class.forName("cpw.mods.fml.relauncher.Side").asSubclass(Enum.class), "CLIENT");
			set("cpw.mods.fml.relauncher.FMLRelaunchLog", "side", client);
			set("cpw.mods.fml.relauncher.FMLLaunchHandler", "side", client);
			set("cpw.mods.fml.relauncher.FMLRelaunchLog", "minecraftHome", gameDir);
			// the core plugins' "where is Forge" (FMLSanityChecker, FMLForgePlugin): the runtime jar; FML's loading
			// screen reads its textures from it, the two containers give it as their source
			File forge = new File(Class.forName("cpw.mods.fml.common.Loader").getProtectionDomain().getCodeSource().getLocation().toURI());
			set("cpw.mods.fml.common.asm.FMLSanityChecker", "fmlLocation", forge);
			set("net.minecraftforge.classloading.FMLForgePlugin", "forgeLocation", forge);
			// the blackboard LaunchWrapper's Launch.main fills; the mods read fml.deobfuscatedEnvironment, FML modList
			Map<String, Object> blackboard = new HashMap<String, Object>();
			blackboard.put("fml.deobfuscatedEnvironment", false);
			Class.forName("net.minecraft.launchwrapper.Launch").getField("blackboard").set(null, blackboard);

			// FMLInjectionData.build + the core plugins' containers, then Loader.injectData(FMLInjectionData.data()) as
			// FML's Loader does on its first use (Configuration reads the game dir from FMLInjectionData too)
			String data = "cpw.mods.fml.relauncher.FMLInjectionData";
			String[] keys = {"major", "minor", "rev", "build", "mccversion", "mcpversion"};
			for (int i = 0; i < keys.length; i++)
				set(data, keys[i], FML_VERSION[i]);
			set(data, "minecraftHome", gameDir);
			@SuppressWarnings("unchecked")
			java.util.List<String> containers = (java.util.List<String>) Class.forName(data).getField("containers").get(null);
			containers.addAll(Arrays.asList(CONTAINERS));
			Object injected = Class.forName(data).getMethod("data").invoke(null);
			Class.forName("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class).invoke(null, injected);

			// forgemods/: FML searches mods/ (the Fabric mods) and these extra files
			@SuppressWarnings("unchecked")
			Map<String, File> extra = (Map<String, File>) Class.forName("cpw.mods.fml.relauncher.ModListHelper")
				.getField("additionalMods").get(null);
			File[] jars = new File(gameDir, "forgemods").listFiles();
			if (jars == null)
				throw new IllegalStateException("no forgemods folder in " + gameDir);
			for (File jar : jars)
			{
				if (jar.getName().endsWith(".jar"))
					extra.put(jar.getName(), jar);
			}
			System.out.println("[forgelayer] FML " + String.join(".", FML_VERSION[0], FML_VERSION[1], FML_VERSION[2], FML_VERSION[3])
				+ " set up in " + gameDir + ", forge mods " + extra.keySet());
		}
		catch (ReflectiveOperationException | java.net.URISyntaxException ex)
		{
			throw new IllegalStateException("Forge layer: FML setup failed", ex);
		}
	}

	/** A package private static of FML's launcher, set as its own launch code would. */
	private static void set(String owner, String field, Object value) throws ReflectiveOperationException
	{
		java.lang.reflect.Field f = Class.forName(owner).getDeclaredField(field);
		f.setAccessible(true);
		f.set(null, value);
	}
}
