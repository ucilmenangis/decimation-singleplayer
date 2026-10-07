# Compile-time shims

Forge 1.7.10 ships its Minecraft changes as `binpatches.pack.lzma` inside the
universal jar, applied at runtime. `tools/lib/minecraft-1.7.10-srg.jar` is the
*unpatched* vanilla jar, so members Forge *adds* to Minecraft classes are
missing from it - e.g. `protected WorldType(String)`, which vanilla only has as
`private WorldType(int, String)`.

A shim here replaces that one class on the **compile classpath only**. It is
never packaged into Decimation.jar. Forge-added members keep readable names
(they are not SRG-obfuscated), so a call compiled against the shim links
against the real Forge-patched class at runtime.

Rules:
- Copy signatures exactly. Confirm them empirically with `javap -p` on a mod
  that already calls the real thing (e.g. ezWastelands' `WastelandsWorldType`),
  the same way stubs were confirmed for the Javassist work.
- Only add members actually used. A shim hides the real class, so anything
  omitted becomes invisible to the compiler.
- Rerun `python3 tools/setup_toolchain.py` after editing.
