package net.decimation.optimized.fastquit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.fastquit.FastQuit;
import net.minecraft.client.gui.screen.ConnectScreen;

/** Joining a server: FML's handshake sets the item ids the stopping server's FML would put back. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin
{
    @Inject(method = "connect(Ljava/lang/String;I)V", at = @At("HEAD"))
    private void deciBeforeJoin(CallbackInfo ci)
    {
        FastQuit.await("joining a server");
    }
}
