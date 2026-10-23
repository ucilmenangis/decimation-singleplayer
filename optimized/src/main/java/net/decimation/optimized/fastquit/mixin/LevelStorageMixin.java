package net.decimation.optimized.fastquit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.decimation.optimized.fastquit.FastQuit;
import net.minecraft.world.level.storage.LevelStorage;

/** Deleting a world from the list (SaveFormatOld.deleteWorldDirectory) waits for a save still writing into it. */
@Mixin(LevelStorage.class)
public abstract class LevelStorageMixin
{
    @Inject(method = "method_255", at = @At("HEAD")) // no yarn name (deleteLevel lives on the interface)
    private void deciBeforeDelete(String name, CallbackInfoReturnable<Boolean> cir)
    {
        FastQuit.await("deleting a world");
    }
}
