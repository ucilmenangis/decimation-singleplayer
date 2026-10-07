# Deobfuscation naming brief (shared by all 8 agents)

Context: Decimation is a closed-source, abandoned Minecraft Forge 1.7.10 mod
(modid `deci`). Its own classes were obfuscated by the author (packages like
`deci/aK`, classes `a`,`b`, members `aew`, `am(int)`). Minecraft/Forge references
in the decompiled code below are ALREADY readable (MCP names, e.g. `setBlock`,
`EntityPlayer`). Goal: give every obfuscated Decimation class, field and method
a readable name, so the user can browse and later edit the mod.

Decompiled source (CFR, read-only): /Volumes/DeciDeobf/src/
Your file list: /Volumes/DeciDeobf/groups/gN.txt (N = your group number).
You may READ any other file under /Volumes/DeciDeobf/src/ for context (callers,
superclasses), but you only produce names for classes in YOUR list.
Already known facts: /Users/ucilmenangis/Projects/Decimation/CLAUDE.md, section
"Obfuscation map" and "The recurring root-cause pattern". Read it first.
Do not modify anything outside /Volumes/DeciDeobf/maps/ and /Volumes/DeciDeobf/notes/.
Do not touch the project folder. Use Python (os.walk + re) for searching, not
grep/find: a shell hook in this environment silently drops grep/find flags.

## Output 1: /Volumes/DeciDeobf/maps/gN.tsv  (tab separated, no header)

C <TAB> obfBinaryName <TAB> NewSimpleName <TAB> subsystem <TAB> H|M|L <TAB> one-line purpose
F <TAB> ownerBinaryName <TAB> obfFieldName <TAB> newFieldName <TAB> H|M|L
M <TAB> ownerBinaryName <TAB> obfMethodName <TAB> (paramTypes) <TAB> newMethodName <TAB> H|M|L

- Binary names use slashes and `$` for nested classes: `deci/aE/a$z$a`.
  CFR prints nested classes inline inside the outer file; work out the chain.
  Anonymous classes (`$1`) need no C line.
- (paramTypes): simple Java types exactly as the decompiled source declares
  them, comma separated, no spaces, no names. e.g. `(int,String,EntityPlayer)`,
  `()`, `(deci.ay.i,float[])`. Obfuscated Decimation types keep their dotted
  obfuscated name. This is how overloads are told apart, so be exact.
- Classes already readable (net/decimation/...) need a C line only if the class
  name itself is obfuscated (e.g. `utils/h` -> UpdateChecker). Still map their
  obfuscated members.
- NEVER rename: members that override/implement Minecraft, Forge or JDK
  methods (anything already named readably or `func_*`/`field_*`), constructors,
  `<clinit>`, enum constants that already have readable names, or anything
  already readable.
- Skip a member only if there is genuinely no clue. Otherwise guess and mark L.
  H = the code makes it obvious. M = strong inference. L = educated guess.
- Class names: UpperCamelCase, unique within the subsystem, descriptive
  (`LootInteractHandler`, `GunItem`, `PacketHitEntity`). Fields/methods
  lowerCamelCase. Names must be valid Java identifiers.
- subsystem: pick from: core, proxy, network, gui, hud, render, model, item,
  weapon, armor, block, tileentity, entity, ai, loot, zone, clan, trader,
  player, chat, command, config, event, util, sound, vehicle, world, management
  (connection to BoehMod's backend servers), cosmetic, misc. Add a new one
  only if none fit.

## Output 2: /Volumes/DeciDeobf/notes/gN.md

Short architecture notes for your group, in English, plain prose and lists:
- Per subsystem you touched: the key classes (obf -> new name), how they fit
  together, the main flows (e.g. "right click crate -> X -> packet Y -> Z").
- Anything only active on a dedicated server (`@SideOnly(Side.SERVER)`,
  ServerProxy-only registration, `isServer()` checks): these are the classic
  singleplayer bugs, list each one you see.
- Network calls to BoehMod's dead backend servers that could hang or fail.
- Anything that looks like a bug.
Keep it factual; mark guesses with [guess]. No em dashes or en dashes.

Finish by replying with: counts (C/F/M lines, H/M/L split) and the 5 most
important things you learned. Keep the reply under 250 words.

## File names (IMPORTANT)

The disk ignores letter case: maps/x_deci_aK.tsv and maps/x_deci_ak.tsv are the same
file. Every map file name must stay unique ignoring case, e.g. add a suffix per
package letter (_lu for aK, _ll for ak).
