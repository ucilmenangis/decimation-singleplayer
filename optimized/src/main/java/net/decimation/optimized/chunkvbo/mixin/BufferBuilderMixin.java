package net.decimation.optimized.chunkvbo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.chunkvbo.ChunkVbo;
import net.minecraft.client.render.BufferBuilder;

/**
 * WorldRenderer (yarn BufferBuilder): a section pass's compile (preRenderBlocks .. postRenderBlocks) is taken by
 * ChunkVbo, with the display list id (glRenderList + pass) and the section's place in its render list (posXClip ...).
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin
{
    @Shadow
    public int field_1803;   // glRenderList
    @Shadow
    public int field_1787;   // posXClip
    @Shadow
    public int field_1788;   // posYClip
    @Shadow
    public int field_1789;   // posZClip

    @Inject(method = "method_6875(I)V", at = @At("TAIL"))
    private void deciBegin(int pass, CallbackInfo ci)
    {
        ChunkVbo.begin(field_1803 + pass, field_1787, field_1788, field_1789);
    }

    @Inject(method = "method_6873", at = @At("TAIL"))
    private void deciEnd(CallbackInfo ci)
    {
        ChunkVbo.end();
    }
}
