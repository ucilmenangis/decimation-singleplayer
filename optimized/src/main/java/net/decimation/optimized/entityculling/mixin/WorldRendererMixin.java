package net.decimation.optimized.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.entityculling.Culler;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.LivingEntity;

/** RenderGlobal.renderEntities: once a frame, the culling thread gets the camera and what to check. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin
{
    @Inject(method = "method_6887", at = @At("HEAD"))   // no yarn name; one overload
    private void deciFrame(LivingEntity camera, CameraView view, float partial, CallbackInfo ci)
    {
        Culler.frame(camera);
    }
}
