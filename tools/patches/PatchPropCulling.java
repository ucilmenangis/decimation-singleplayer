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
        los.writeFile(a[1]);
        System.out.println("patched TileEntityProp.getRenderBoundingBox and deci.a.c$a.a(EntityPlayer,TileEntity)");
    }
}
