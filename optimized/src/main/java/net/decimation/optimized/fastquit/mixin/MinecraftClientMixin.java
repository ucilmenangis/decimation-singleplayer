package net.decimation.optimized.fastquit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.fastquit.FastQuit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

/** Leaving a world (loadWorld(null)) stops waiting for the save; opening a world and closing the game wait for it. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin
{
    /** Forge's loop "while (!theIntegratedServer.isServerStopped()) sleep(10)": hand the server over and go on. */
    @Redirect(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;isStopped()Z"))
    private boolean deciLeave(IntegratedServer server)
    {
        FastQuit.leave(server);
        return true;
    }

    /** The full collection loadWorld ends with: a pause the JVM would spread out itself (measured by dev test fastquit). */
    @Redirect(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",
              at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    private void deciNoFullGc()
    {
    }

    @Inject(method = "startIntegratedServer", at = @At("HEAD"))
    private void deciBeforeOpen(CallbackInfo ci)
    {
        FastQuit.await("opening a world");
    }

    /** shutdownMinecraftApplet: the window is gone, System.exit would cut a save short. */
    @Inject(method = "stop()V", at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
    private void deciBeforeExit(CallbackInfo ci)
    {
        FastQuit.await("closing the game");
    }
}
