import javassist.*;

/**
 * Javassist patch: Decimation props randomly not rendering (legacy bug,
 * reported by the user 2026-10-07, also with player-placed props).
 *
 * Root cause: props are drawn by PropRenderer (deci.I.l), which first asks
 * LineOfSight.canSeeTileEntity (deci.a.c$a.a(EntityPlayer, TileEntity)): 8 rays
 * from the eye to the 8 corners of TileEntityProp.getRenderBoundingBox(), drawn
 * only if one ray is clear. Forge's frustum culling uses the same box. But
 * that box is (a) the bare 1x1x1 block cell for half the props (no render size
 * declared) although their models are drawn up to ~1.5 blocks wide, and (b)
 * never rotated with the prop (the model turns by metadata % 4 * 90). Corners
 * end up off screen or behind floors / walls while the model is in plain view.
 *
 * Fix:
 * 1. getRenderBoundingBox: centred on the block, rotation proof (largest
 *    horizontal extent of the declared render size in every direction),
 *    at least 2x2 blocks, half a block taller; null render size = 0..1.
 * 2. canSeeTileEntity: always visible within 4 blocks, and one extra ray to
 *    the box centre before the original 8 corner rays.
 * 4. (2026-10-09) render distance by prop size (getMaxRenderDistanceSquared),
 *    configurable, default 64 for all (vanilla), see step 4.
 * 3. (2026-10-09) the answers of canSeeTileEntity and canSeeEntity are cached
 *    until the player (or the entity) moves 0.3 blocks, at most 1.0..1.3 s for
 *    props and 0.15..0.18 s for entities: see the comment at step 3.
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_props tools/patches/PatchPropCulling.java
 *   java -cp tools/lib/javassist.jar:build/patch_props PatchPropCulling <Decimation jar ending .jar> build/patch_props/out
 *   (cd build/patch_props/out && zip <jar> <the two class files>)
 *
 * Runtime names are SRG (xCoord = field_145851_c, AxisAlignedBB.getBoundingBox
 * = func_72330_a, ...); tools/lib/*-srg.jar provide them for compilation.
 */
public class PatchPropCulling {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath("tools/lib/minecraft-1.7.10-srg.jar");
        pool.insertClassPath("tools/lib/forge-1.7.10-srg.jar");

        CtClass te = pool.get("net.decimation.mod.common.entity.blockentities.props.TileEntityProp");
        CtMethod box = te.getDeclaredMethod("getRenderBoundingBox");
        box.setBody(
            "{"
          + "  if (this.renderBoundBox == null) {"
          + "    float x1 = 0f, y1 = 0f, z1 = 0f, x2 = 1f, y2 = 1f, z2 = 1f;"
          + "    if (this.renderPositions != null) {"
          + "      x1 = this.renderPositions.xpos1; y1 = this.renderPositions.ypos1; z1 = this.renderPositions.zpos1;"
          + "      x2 = this.renderPositions.xpos2; y2 = this.renderPositions.ypos2; z2 = this.renderPositions.zpos2;"
          + "    }"
          + "    double r = Math.max(Math.max(Math.abs(x1 - 0.5f), Math.abs(x2 - 0.5f)),"
          + "                        Math.max(Math.abs(z1 - 0.5f), Math.abs(z2 - 0.5f)));"
          + "    if (r < 1.0) r = 1.0;"
          + "    double cx = this.field_145851_c + 0.5, cz = this.field_145849_e + 0.5;"
          + "    this.renderBoundBox = net.minecraft.util.AxisAlignedBB.func_72330_a("
          + "        cx - r, this.field_145848_d + Math.min(y1, 0f) - 0.25, cz - r,"
          + "        cx + r, this.field_145848_d + Math.max(y2, 1f) + 0.5, cz + r);"
          + "  }"
          + "  return this.renderBoundBox;"
          + "}");
        // 4. (2026-10-09) render distance by size: vanilla draws every tile entity
        //    up to 64 blocks; a city holds ~21 props per chunk (up to 522 in a
        //    tower chunk). Largest render extent < 0.8 block (cans, bags, cones):
        //    decimation.props.small; < 1.6 (crates, bins, benches, props without a
        //    declared size): decimation.props.medium; else decimation.props.large.
        //    All default to 64 (vanilla): in a city street 32 / 48 measured no fps
        //    gain (props were 5.5% of the frame after step 3) and can pop in, so it
        //    stays a config option (deciworldgen_props.cfg) for open prop heavy areas.
        te.addField(CtField.make("public static double distSmall;", te), CtField.Initializer.constant(-1.0));
        te.addField(CtField.make("public static double distMedium;", te), CtField.Initializer.constant(-1.0));
        te.addField(CtField.make("public static double distLarge;", te), CtField.Initializer.constant(-1.0));
        te.addField(CtField.make("private double maxDistSq;", te), CtField.Initializer.constant(-1.0));
        te.addMethod(CtNewMethod.make(
            "public double func_145833_n() {"
          + "  if (this.maxDistSq < 0.0) {"
          + "    if (distSmall < 0.0) {"
          + "      distSmall = Double.parseDouble(System.getProperty(\"decimation.props.small\", \"64\"));"
          + "      distMedium = Double.parseDouble(System.getProperty(\"decimation.props.medium\", \"64\"));"
          + "      distLarge = Double.parseDouble(System.getProperty(\"decimation.props.large\", \"64\"));"
          + "    }"
          + "    float ext = 1f;"
          + "    if (this.renderPositions != null) {"
          + "      ext = Math.max(Math.abs(this.renderPositions.xpos2 - this.renderPositions.xpos1),"
          + "            Math.max(Math.abs(this.renderPositions.ypos2 - this.renderPositions.ypos1),"
          + "                     Math.abs(this.renderPositions.zpos2 - this.renderPositions.zpos1)));"
          + "    }"
          + "    double d = ext < 0.8f ? distSmall : ext < 1.6f ? distMedium : distLarge;"
          + "    this.maxDistSq = d * d;"
          + "  }"
          + "  return this.maxDistSq;"
          + "}", te));
        te.writeFile(a[1]);

        CtClass los = pool.get("deci.a.c$a");
        CtMethod see = null;
        for (CtMethod m : los.getDeclaredMethods("a")) {
            if (m.getSignature().equals("(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/tileentity/TileEntity;)Z")) {
                see = m;
            }
        }
        see.insertBefore(
            "{"
          + "  double cx = $2.field_145851_c + 0.5, cy = $2.field_145848_d + 0.5, cz = $2.field_145849_e + 0.5;"
          + "  double ey = $1.field_70163_u + $1.func_70047_e();"
          + "  double dx = $1.field_70165_t - cx, dy = ey - cy, dz = $1.field_70161_v - cz;"
          + "  if (dx * dx + dy * dy + dz * dz < 16.0) return true;"
          + "  if (a($1.field_70170_p,"
          + "        net.minecraft.util.Vec3.func_72443_a($1.field_70165_t, ey, $1.field_70161_v),"
          + "        net.minecraft.util.Vec3.func_72443_a(cx, cy, cz), false, false, false) == null) return true;"
          + "}");

        // 3. (2026-10-09, user: fps drop near many props) cache the answers. A
        //    Flight Recorder profile of 225 props in view showed 76% of the prop
        //    renderer's time in these ray casts (a chunk lookup per block per
        //    ray, every frame, 9 rays for every hidden prop). Answers are reused
        //    (below); canSeeEntity is used for every living entity and vehicle.
        //    Lifetimes are staggered by identity hash so not all recheck on one
        //    frame. The original methods stay as *Raw copies.
        los.addField(CtField.make("public static java.util.WeakHashMap seeCache;", los),
                     CtField.Initializer.byNew(pool.get("java.util.WeakHashMap")));
        CtMethod seeEntity = null;
        for (CtMethod m : los.getDeclaredMethods("a")) {
            if (m.getSignature().equals("(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)Z")) {
                seeEntity = m;
            }
        }
        los.addMethod(CtNewMethod.copy(see, "seeTileEntityRaw", los, null));
        los.addMethod(CtNewMethod.copy(seeEntity, "seeEntityRaw", los, null));
        // entry: {expiry nanos, result, player x, y, z, target x, y, z}; reused while
        // neither the player nor (for entities) the target moved 0.3 blocks and it
        // is younger than the lifetime (a frame time based lifetime alone rechecked
        // nearly every frame at low fps, 2026-10-09 profile)
        String cached =
            "{ if ($1 == null || $2 == null) return %4$s($1, $2);"   // menu preview player: no world, no player
          + "  long now = System.nanoTime();"
          + "  double px = $1.field_70165_t, py = $1.field_70163_u, pz = $1.field_70161_v;"
          + "  double tx = %1$s, ty = %2$s, tz = %3$s;"
          + "  double[] c = (double[]) seeCache.get($2);"
          + "  if (c != null && now < c[0]"
          + "      && (px - c[2]) * (px - c[2]) + (py - c[3]) * (py - c[3]) + (pz - c[4]) * (pz - c[4]) < 0.09"
          + "      && (tx - c[5]) * (tx - c[5]) + (ty - c[6]) * (ty - c[6]) + (tz - c[7]) * (tz - c[7]) < 0.09)"
          + "    return c[1] != 0.0;"
          + "  boolean r = %4$s($1, $2);"
          + "  if (c == null) { c = new double[8]; seeCache.put($2, c); }"
          + "  c[0] = (double) (now + %5$dL + (long) (System.identityHashCode($2) & 31) * %6$dL);"
          + "  c[1] = r ? 1.0 : 0.0; c[2] = px; c[3] = py; c[4] = pz; c[5] = tx; c[6] = ty; c[7] = tz;"
          + "  return r; }";
        // props: 1.0..1.3 s while the player stands still; entities: 0.15..0.18 s
        see.setBody(String.format(cached, "0.0", "0.0", "0.0", "seeTileEntityRaw", 1000000000L, 10000000L));
        seeEntity.setBody(String.format(cached, "$2.field_70165_t", "$2.field_70163_u", "$2.field_70161_v",
                                        "seeEntityRaw", 150000000L, 1000000L));
        los.writeFile(a[1]);
        System.out.println("patched TileEntityProp.getRenderBoundingBox and deci.a.c$a.a(EntityPlayer,TileEntity) "
                           + "+ a(EntityPlayer,Entity): wider check, cached results");
    }
}
