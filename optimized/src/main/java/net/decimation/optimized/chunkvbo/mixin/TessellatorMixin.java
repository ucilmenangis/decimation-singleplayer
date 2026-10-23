package net.decimation.optimized.chunkvbo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.decimation.optimized.chunkvbo.ChunkVbo;
import net.minecraft.client.render.Tessellator;

/**
 * Tessellator.draw while a chunk section is compiled: its vertices go to ChunkVbo instead of the GL; the vertex count
 * set to 0 skips only the draw (the buffer is still reset by draw itself).
 */
@Mixin(Tessellator.class)
public abstract class TessellatorMixin
{
    @Shadow
    private int[] array;
    @Shadow
    private int arrayIdx;
    @Shadow
    private int count;
    @Shadow
    private int format;
    @Shadow
    private boolean hasTexture;
    @Shadow
    private boolean hasLight;
    @Shadow
    private boolean hasColor;
    @Shadow
    private boolean hasNormal;

    @Inject(method = "end()I", at = @At("HEAD"))
    private void deciCapture(CallbackInfoReturnable<Integer> cir)
    {
        if (ChunkVbo.capturing() && (Object) this == Tessellator.INSTANCE)
        {
            ChunkVbo.capture(array, arrayIdx, count, format, hasTexture, hasLight, hasColor, hasNormal);
            count = 0;
        }
    }
}
