# Creating New Weapons - Decimation Mod

How the ~90 existing guns are built, and what it takes to add a new one. Two
paths depending on ambition: **reskin** (reuse an existing 3D model, change
stats/name/texture — easy, same-session doable) vs **new model** (genuinely new
shape — needs Techne or similar, see below).

Update (since v0.36.0, 10 Oktober 2026): new models are built in code by our own
pipeline, no Techne needed: tools/guns (gunmodel.py, one spec per gun), renders and
measurements with tools/guns/study.py, registration in fixes/NewGuns; the whole
workflow is the project skill decimation-gun (docs/gun_model_spec.md section 6,
docs/gun_style_guide.md). The Techne recipe below is the older manual route.

## The `.bmodel` format (not what it looks like)

Every gun's 3D model file lives at `assets/deci/models/guns/<category>/<name>.
bmodel`, loaded by `deci.n.g.a(ResourceLocation)`. Despite the name, **it is not
a binary/proprietary format** - it's a plain text file containing crude
Techne-style Java model-builder code, parsed line-by-line by naive string
splitting (see `deci.n.g` for the exact parser). This means:

- You do not need to reverse-engineer anything to make a new model.
- **Techne** (or any tool that exports the same style of code - a Minecraft
  1.7.10-era cuboid model editor) produces exactly this shape of output. Model
  visually, export, then reformat the generated Java into this loader's exact
  expected syntax.

### Recognized line patterns (what the parser actually looks for)

Each line is matched by substring, not a real parser, so formatting must be
close to these examples:

```
partName = new WW2ModelRenderer(this, u, v);   // or "new ModelRenderer(...)" or "new BeardieModelRenderer(...)"
partName.addBox(x, y, z, width, height, depth);   // BROKEN in the loader: Y is used as Z. Use addShape only (see docs/gun_model_spec.md)
partName.addShape(x, y, z, {{...8 float[3] corner offsets...}}, sx, sy, sz);  // alt to addBox, custom-shaped box
partName.setRotationPoint(x, y, z);
partName.setRotation(x, y, z);                  // radians; also accepts "... / rotFix" suffix, stripped before parsing
parentName.addChild(childName);                 // vanilla direction (BModelLoader: the name before the dot is the parent; checked 2026-10-10, an older note here said reversed)
textureWidth = N;
textureHeight = N;
```

Plus a handful of gun-specific header fields read before/around the part
definitions (position/rotation offsets used for view-model placement, muzzle
flash, shell eject, and held-item hand position):

```
mOff: x, y, z;       // model offset
sPos: x, y, z;       // sight/scope position
flamePos: x, y, z;   // muzzle flash position
lhPos: x, y, z;      // left hand position
lhRot: x, y, z;
rhPos: x, y, z;      // right hand position
rhRot: x, y, z;
Scale: n;
ejectPos: x, y, z;   // shell/casing eject position
```

Any line not matching one of these patterns is silently ignored - safe to leave
comments/whitespace/unrelated boilerplate from a raw Techne export in the file.

## Registering the weapon (`deci.aD.k`, item registration)

Every gun is one call, following this exact shape (real example, AK-74):

```java
aqY = new i("ak74", e2, deci.ay.c.rifle, itemArray).f(0.13).am(15);
```

- `"ak74"` - internal name. Used to build texture path (`deci:gun/<category>/
  <name>`), sound paths (`guns/<category>/<name>/fire`, `/magIn`, `/magOut`,
  `/rack`, etc — see `deci.ay.i` constructor), and by convention the `.bmodel`
  filename.
- `e2` - a `deci.ay.e` stats object, built once above the registration call:
  `new deci.ay.e(float[] recoil, e.a[] fireModes, int[] ?, float[][] perModeTable, float unknownF, float roundsPerMinute)`.
  Confirmed fields: `recoil[0]`/`recoil[1]` = base pitch/yaw kick (divided down
  further when aiming through sights), `roundsPerMinute` sets fire-rate delay.
  `fireModes` is an array of `deci.ay.e.a` enum values (`SINGLE`, `AUTO`,
  `BURST`, `PUMP`, `BOLT`) - order matters, first is default. The `int[]` and
  `float[][]` per-mode table's exact per-slot semantics were **not fully
  reverse-engineered this session** - safest approach for a new weapon is to
  copy the `e(...)` call from an existing gun in the same category and only
  adjust values you've confirmed (recoil numbers, RPM), rather than guessing at
  the table's structure.
- `deci.ay.c.rifle` - category enum. Also: `pistol`, `revolver`, `smg`, `lmg`,
  `shotgun`, `mg`, `rocket`, `flamethrower` (enum value exists, unused - no
  weapon registered under it), `crossbow`, `all`.
- `itemArray` - array of `Item` (ammo types this gun accepts - reuse an existing
  ammo `Item` field for a conventional caliber, or define a new one the same
  way ammo items are registered elsewhere in `deci.aD.k`).
- `.f(0.13)` - builder chain, not yet mapped to a confirmed meaning (varies
  0.02-0.4 across existing guns, roughly correlates with weapon class - pistols
  low, LMGs/rockets high; likely spread or sway).
- `.am(15)` - **damage per hit**. The chokepoint - every gun's damage funnels
  through `deci.ay.i.am(int)`. Currently patched (see `documentation.md`) to
  halve whatever value is passed here, as a global balance change.
- Optional chain calls seen on some weapons: `.fH()` (suppressed variant, skips
  mag-in/out sounds), `.fI()`, `.al(n)`, `.a(deci.ay.d.grenade)`, `.J("name")`
  (sound-set override), `.fP()` - not all individually mapped, safest to copy
  from whichever existing weapon is closest to what you're building and only
  change the values you understand.

## Recipe: reskin an existing weapon (fast path)

1. Pick an existing gun in `deci.aD.k` close to what you want stat-wise.
2. Copy its `e(...)` stats block and its `new i(...)` registration line, give
   it a new field name and new `"internal_name"` string.
3. Point it at a new texture (`deci:gun/<category>/<new_name>`) and/or new
   `.bmodel` if you also want a different shape - otherwise it'll just reuse
   the original model file path convention, so give it its own copy of the
   `.bmodel` (even if byte-identical) so later edits to one don't affect both.
4. Adjust `.am(damage)`, category, ammo `itemArray`, and any other numbers you
   want different.
5. Patch in via the same Javassist workflow as every other fix this session
   (see `CLAUDE.md`) - `deci.aD.k` has no lambdas in the registration section,
   so straightforward `CtMethod` edits/insertions should work directly.

## Recipe: genuinely new model (manual Techne route; our pipeline: skill decimation-gun)

1. Build the gun as boxes in Techne (or compatible 1.7.10-era cuboid editor),
   export as Java.
2. Reformat the exported code to match the exact patterns above (rename
   `ModelRenderer` calls if needed, add the `textureWidth`/`textureHeight`
   lines, add the gun-specific header fields - sight position, hand positions,
   muzzle flash, eject point - by eyeballing/measuring against the model).
3. Save as `assets/deci/models/guns/<category>/<name>.bmodel`, paint a
   matching texture at whatever UV layout Techne used.
4. Register the item same as the reskin recipe above, pointing at the new
   model/texture paths.

Claude cannot usefully author the model itself (no visual feedback loop to
check the result, and texture painting is out of scope) - can help with steps
2-4 (reformatting exported code into this loader's syntax, writing the item
registration/patch) once you have a model out of Techne.
