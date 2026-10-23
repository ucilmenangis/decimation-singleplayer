package net.decimation.optimized.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.decimation.optimized.entityculling.Cullable;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;

/** RenderManager.renderEntitySimple, the world's entity draw: a culled entity is not drawn (false: nothing drawn). */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
    @Inject(method = "renderEntity(Lnet/minecraft/entity/Entity;F)Z", at = @At("HEAD"), cancellable = true)
    private void deciCull(Entity entity, float partial, CallbackInfoReturnable<Boolean> cir)
    {
        if (((Cullable) entity).deciCulled())
        {
            cir.setReturnValue(false);
        }
    }
}
