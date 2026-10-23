package net.decimation.optimized.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.entityculling.Cullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;

/** TileEntityRendererDispatcher.renderTileEntity, the world's block entity draw: a culled one is not drawn. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin
{
    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;F)V", at = @At("HEAD"), cancellable = true)
    private void deciCull(BlockEntity blockEntity, float partial, CallbackInfo ci)
    {
        if (((Cullable) blockEntity).deciCulled())
        {
            ci.cancel();
        }
    }
}
