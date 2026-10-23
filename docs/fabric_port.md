# Decimation on Fabric (Legacy Fabric, Minecraft 1.7.10)

User 23 Oktober 2026: "lets do this porting to fabric", with Fabulously Optimized for FPS. Chosen after asking: Fabric on
1.7.10 (not a modern rewrite). Fabulously Optimized and Sodium exist for 1.16.5 and newer only (Modrinth, checked), so
on 1.7.10 the FPS side is the 1.7.10 Fabric set instead: Optifabric (OptiFine 1.7.10 HD U E7 on Fabric), Dynamic FPS,
FPS Plus (legacyfabric.net mod list). Analysis before: docs/porting.md section 8.

## 1. Facts

- Loader: Legacy Fabric (legacyfabric.net, 1.3 to 1.14), loader 0.20.1, yarn 1.7.10+build.603, Legacy Fabric API
  1.13.5+1.7.10, Loom 1.16 with legacy-looming. Plain Fabric 1.7.10 builds and runs on this Mac (Java 21, arm64): our
  `fabric/` project, `./gradlew runClient`.
- No Forge on Fabric layer exists for 1.7.10. Fabricated Legacy Forge (FabricCompatibilityLayers) does it for 1.3.2 to
  1.4.x on Ornithe with their Mod Remapping API; LegacyCompatibilityUtils is a Forge side mod (1.7.2 mods on 1.7.10).
- What Decimation and our mod use of Forge: docs/references/forge_api_used.tsv (tools/fabric/forge_inventory.py): 105
  Forge / FML classes, 175 methods, 196 fields, about 100 members Forge added to Minecraft classes, 18 such methods
  overridden. No coremod in either.

## 2. Design: Forge itself, applied ahead, on Fabric's loader

Forge 1.7.10 (10.13.4.1614) is LGPL 2.1, so its own code is used instead of being rebuilt. At run time Forge works by
LaunchWrapper transformers: binary patches to about 300 vanilla classes, notch to SRG renaming, access transformers,
side stripping, event class rewriting. Here they run ahead of time (tools/fabric):

1. Game jar: vanilla 1.7.10 client + Forge's binpatches (its own ClassPatchManager) + its access transformers
   (fml_at.cfg, forge_at.cfg), kept in official (notch) names. Legacy Fabric takes it as the game jar and remaps it to
   intermediary like vanilla; members Forge added are in no mapping, so they keep Forge's names, which is what Forge
   mods call.
2. Forge runtime jar: the universal jar's own classes (cpw.mods.fml, net.minecraftforge), remapped official to
   intermediary, its event classes run through Forge's EventSubscriptionTransformer.
3. Forge mods (Decimation, DeciRemnant): SRG names remapped to intermediary (SRG from Forge's
   deobfuscation_data-1.7.10.lzma joined with Legacy Fabric's intermediary through the notch names), event classes
   through EventSubscriptionTransformer.
4. The layer (fabric/, a Fabric mod, `ForgeLayer` pre launch): does what FML's tweakers and core plugins do under
   LaunchWrapper: FML's side (FMLRelaunchLog, FMLLaunchHandler), FMLInjectionData (version 7.99.40.1614, game dir,
   the FMLContainer / ForgeModContainer containers) and Loader.injectData, Launch.blackboard
   (fml.deobfuscatedEnvironment false; FML adds modList), FMLSanityChecker.fmlLocation / FMLForgePlugin.forgeLocation
   (the runtime jar: FML's splash reads its textures from it), the Forge mods of forgemods/ (ModListHelper.
   additionalMods). Forge's patched Minecraft then runs FML's own lifecycle (beginMinecraftLoading, loadMods, ...).
5. Shims (tools/fabric/shim, compiled against Fabric's loader, written over Forge's classes in forge-runtime.jar):
   ModClassLoader (Forge's casts its parent to LaunchClassLoader; ours adds mod jars to Knot's class path and loads
   through Knot; the @Optional stripper is filled but not applied: neither mod uses @Optional) and ReflectionHelper
   (also tries every intermediary name of an SRG name intermediary splits, see 6).
6. Names in strings: under Forge Minecraft runs with MCP class names and SRG member names, so code names them in
   strings (Class.forName("net.minecraft.init.Blocks"), reflection on "field_146297_k"). `Forgify strings` rewrites
   every string constant (code, field constants, annotation values) that is exactly an SRG class name (dotted or
   slashed) or an SRG member name with one intermediary name: 97 in Forge, 64 in the Decimation jar. 33 SRG names
   name several members in intermediary (covariant overrides: func_110813_b is method_5781 in RendererLivingEntity
   and method_5791 in RenderLiving); they stay, and the ReflectionHelper shim tries each on the class it is given
   (forgelayer/srg_ambiguous.txt in the runtime jar). Direct Class.getDeclaredMethod with one of those 33 would fail
   [not seen yet].
7. Mappings (tools/fabric/mappings.py): Forge's universal jar adds nested classes under notch names (aji$4 in Block,
   alb$FireInfo ...); they get their outer class's names in both the tiny file and intermediary-forge.jar (Legacy
   Fabric's intermediary + those, used instead of net.legacyfabric:intermediary when Fabric remaps the game jar).
   Nested classes whose intermediary name is in another package than their outer class (MinecraftServer, Main and
   the realms classes keep their names, their anonymous classes are net/minecraft/class_N) take the outer's name +
   suffix: 12 classes. Without it the integrated server fails (IllegalAccessError, MinecraftServer$1 / $2 / $3);
   moving only the class Forge creates (li) was tried and failed the same way on $1 and $3.
8. Legacy Fabric API is NOT in the runtime (the layer uses only the loader), and its networking module cannot be:
   its MinecraftServerMixin injects at `class_739.start()` in startServerThread (method_3048), the vanilla server
   thread MinecraftServer$3; Forge's patched MinecraftServer starts MinecraftServer$2 there, so the target is gone
   (and class_739 is one of the 12 renamed). Any Fabric mod that needs Legacy Fabric API must be checked first.
9. OptiFine (optional: the OptiFine 1.7.10 HD U E7 jar in build/fabric/optifine/, git ignored, never shipped:
   optifine.net's terms). Under Forge, OptiFine's class transformer hands out, for each of the 87 classes it changes,
   its xdelta applied to the VANILLA class (MD5 checked) in place of Forge's version (53 of them Forge also patches);
   OptiFine's versions reach Forge through its Reflector. `Forgify game ... OPTIFINE.jar` does exactly that with
   OptiFine's own Patcher, adds its 225 classes (136 in the default package, mapped into net/minecraft beside the notch
   classes they use package privately, in both tiny files so FML's Class.forName("Config") in detectOptifine /
   GuiModList is renamed by Forgify strings) and builds its default connected textures (840 xdeltas on vanilla
   textures, assets/minecraft/mcpatcher/ctm/default). Forge's access transformers apply on top as before.
10. Our Fabulously Optimized modules (optimized/, Fabric mod "DeciRemnant Optimized", user 23 Oktober 2026: "if some of
   them not exist then just import them ... write it ourselves"). None of FO's 46 mods (v15.0.0-beta.2, MC 26.3) has
   a Legacy Fabric 1.7.10 build (Modrinth, checked per mod); only EntityCulling has a 1.7.10 version, for Forge.
   Sorted: OptiFine (item 9) already does what Iris, Continuity, Animatica, ETF / EMF, Skyboxify, BetterGrassify,
   Polytone, LambDynamicLights and Zoomify do; FerriteCore (block states), Ixeris (LWJGL 3), the config / library mods,
   Mod Menu, e4mc, Controlify, capes, chat and credits have nothing to port to 1.7.10. Written, one mixin config and
   package per module, each switched in config/deciremnant_optimized.properties (system property
   deciremnant.optimized.<module> overrides; MixinPlugin leaves a module that is off unpatched):
   - chunkvbo (Sodium's core): chunk sections drawn from vertex buffers instead of display lists. A JFR profile of
     steady frames (fpsbench, this Mac) had two thirds of the client thread in native GL and 56 % of that in
     glCallLists (replaying chunk display lists: Apple's OpenGL runs on Metal). TessellatorMixin hands a section's
     vertices to ChunkVbo while it compiles (preRenderBlocks .. postRenderBlocks, BufferBuilderMixin), with the
     list's translate and 1.000001 seam scale baked into the positions; ListedChunkRenderManagerMixin redirects
     RenderList's glCallLists: sections with a buffer draw from it (client arrays switched once a batch), others from
     their list. Off with OptiFine Fast Render, shaders, multi-texture or a chunk built off the main thread.
   - entityculling (tr7zw's EntityCulling): a thread ray marches from the camera to the centre and corners of every
     entity and every block entity within 64 blocks through full opaque blocks; hidden ones are not drawn
     (EntityRenderDispatcher.renderEntity, BlockEntityRenderDispatcher.render). Never the camera entity, its vehicle,
     anything within 2 blocks, infinite render boxes, nor anything in third person. Dev test `culltest` 8/8.
   - dynamicfps (Dynamic FPS): at most 15 fps unfocused, 1 fps hidden (Display.isActive / isVisible, end of
     runGameLoop). Off in dev tests (their windows are often unfocused) unless forced by the system property.
   - fastquit (FastQuit): leaving a world (Minecraft.loadWorld(null)) no longer waits for the integrated server's
     save (Forge's isServerStopped loop is redirected; loadWorld's System.gc() skipped too); the server saves on its
     own thread. Opening a world, joining a server, deleting a world and closing the game (before System.exit, after
     the window is gone) wait for that save first. Off in dev tests unless forced: many leave and expect the save done.
   RRLS (Remove Reloading Screen) has nothing to port: 1.7.10 reloads resources (refreshResources) with no screen at
   all, only a frozen frame, and its first load is Forge's splash.
   Measured, not kept: OptiFine settings. Multi-Core chunk loading, Fast Render, more chunk updates a frame were all
   slower on this Mac; Fast Math within noise. OptiFine's defaults stay.

Build and run: `python3 tools/fabric/build_jars.py` (Java 8 + 21, about 30 s), `cd fabric && ./gradlew build`,
`cd optimized && ./gradlew build` (copy its jar to build/fabric/fabricmods),
`python3 tools/fabric/run_client.py [--seconds N] [--window WxH] [--jfr] [-Dkey=value ...]` (launches like a launcher: Knot, Legacy Fabric's
profile libraries, game dir build/fabric/run, Forge mods in run/forgemods). DeciRemnant's dev tests run there:
`-Ddeciworldgen.autotest=true -Ddeciworldgen.autotest.test=MODE`, results in build/fabric/run/devtest/results.txt.

## 3. Status

- [x] Legacy Fabric 1.7.10 project builds and runs (fabric/).
- [x] Forge API inventory (docs/references/forge_api_used.tsv).
- [x] mappings, game jar, Forge runtime jar, mod jars (tools/fabric)
- [x] FML on Knot: Forge 10.13.4.1614 initialises, Decimation and DeciRemnant construct, pre-init, init, post-init
- [x] Decimation reaches its menu and a world loads (23 Oktober 2026): dev test `menu` 4 PASS on Fabric (Play without
  the voice chat jar, update check off for a dev build, our logo), shot: a fresh Decimation world type world with
  Decimation's HUD and tip panel and a generated city on the horizon. Missing textures: the same 7 Decimation ones as
  under Forge.
- [x] a world plays (23 Oktober 2026, dev tests on Fabric): `checks` 16/16 (zones, vehicle, humanity, prop box,
  bottlecaps, armor and NPC hits, helmet, vanilla mobs, 60 round mags, supply drop), `zombies` 6/6, `gun` 2/2 (the
  MAC-10 reload missed once in a long checks + gun + zombies run that fell 15 s behind; alone it passes, Decimation's
  Uzi reloaded in both), `loot` 17/17 twice (once the body's take found no loot slot: the screen was built before
  Decimation's loot inventory packet; LootTest now resets `taken`, which had let "in the player's inventory" pass on
  the crate's item). Shots: MAC-10 in first person with arms, Decimation HUD, soldier NPC.
- [x] FPS, `cityfps` (seed 1 city street, this Mac, 854 x 480). Fabulously Optimized / Sodium do not exist for 1.7.10;
  the 1.7.10 Fabric mods FPS Plus Ver-D + MenuFPSUnlocker 1.1.0 gain nothing (FPS Plus only swaps MathHelper's sin /
  cos, MenuFPSUnlocker only the menu's frame cap). OptiFine 1.7.10 HD U E7 does (23 Oktober 2026), built into the game
  jar: same Fabric world and view, without 62 / 71, with OptiFine 114 / 114 (+70 %). (An earlier "Forge dev client 70
  / 61" was another world with no city in view: not comparable.)
- [x] OptiFine (section 9): Legacy OptiFabric 2.3.6 fails ("MD5 not matching, aho.class": its patcher wants vanilla
  classes, ours carry Forge's). OptiFine's own Forge path is used instead, ahead of time. Plays with OptiFine: menu
  4/4, loot 17/17, gun 2/2 and checks 16/16 twice each (a first gun run missed the MAC-10 reload and a checks +
  zombies run missed "infected spawns" once: the same intermittent pattern as without OptiFine), zombies 6/6.
- [x] Our FO modules (section 2, item 10; 23 Oktober 2026). Bench `fpsbench` (dev test: last autotest world, seed 1
  city around x 8 z 120, noon; three still views settled 8 s then 10 s of frame times, and a 15 s walk north at 5
  blocks a second; average fps and 1 % low; `cityfps` read 117 and 138 for one build, too noisy). At 854 x 480 the
  game pins at about 115 fps (this Mac's 120 Hz display: Apple's OpenGL holds windowed swaps to it even with vsync
  off), so the comparison runs at 1440 x 810, render distance 16. All modules against none (both with OptiFine),
  medians of three interleaved runs: street north 108 -> 107, south 98 -> 109 (+11 %), roofs 79 -> 109 (+38 %),
  walk 80 -> 109 (+36 %). ChunkVBO alone, same setup: south +13 %, roofs +29 %, walk +31 %, north 0; every section of
  a region drawn from buffers (0 display lists). EntityCulling alone at 854 x 480: within noise (hides about half of
  the 33 to 60 entities and props in range in these views; built for crowded hidden scenes, checked by `culltest`).
  Dynamic FPS: window unfocused mid bench -> 14 fps (cap 15), focused unchanged. Pictures with and without ChunkVBO
  match (fpsbench shots). What is left in steady frames: the buffer swap (GPU), OptiFine's per frame walk of its
  dirty section list and frustum checks over every section (the CPU side Sodium's visibility graph and threaded
  meshing would remove: not written yet).
- [x] FastQuit (23 Oktober 2026, dev test `fastquit` on Fabric): leaving a world held the client 647 ms, now 3 to 13
  ms (3 runs; 248 ms with the server wait alone removed: the rest was loadWorld's System.gc()); the server still saving
  when the menu came; opening the same world one tick later waited 0.4 to 1 s and found the block set before leaving.
  Closing the game right after leaving (phase exit) waited 107 ms for the save before exiting; the next launch
  (phase check) found the block. With FastQuit forced on: checks + zombies 22/22 three times (after ServerChecks
  retries its infected spawn: a single try had failed "infected spawns" in 4 of about 12 Fabric runs, with or without
  our modules), gun 2/2, loot 17/17, menu 4/4, culltest 8/8, lootrules 21/21 (it leaves and reopens its world).
- [x] Prism instance "Decimation Fabric" (tools/fabric/prism_instance.py, 23 Oktober 2026): one custom component
  (patches/net.decimation.forgelayer.json: Knot main class, Legacy Fabric's loader libraries, guava 17, lzma,
  launchwrapper, commons-lang3, local libraries forge-runtime and intermediary-forge, and `mainJar` = the local
  minecraft-forge.jar in place of vanilla's), Prism's own LWJGL 2.9.4-nightly; mods/ the layer, forgemods/ the
  Decimation jar. Launched from Prism (offline): Fabric Loader 0.20.1, FML loaded 5 mods, DeciRemnant's warm up,
  Decimation's EULA screen, on Prism's jre-legacy Java 8 (Azul 1.8.0_504 arm64, as in the measurements); with the
  OptiFine build also "[OptiFine] OptiFine_1.7.10_HD_U_E7". Trap: a Prism jar mod written as `+jarMods` is looked up by
  its maven name, not MMC-filename (deprecated form); `mainJar` replaces the game jar without a merge. A new instance
  folder is seen by Prism only after a rescan: the first `--launch` of it fails, the second works.
