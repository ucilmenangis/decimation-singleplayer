package net.decimation.optimized.dynamicfps.mixin;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.decimation.optimized.OptimizedConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Dynamic FPS (in Fabulously Optimized) for 1.7.10: when the window is not focused the game draws at most
 * dynamicfps.unfocusedFps frames a second (15), when it is minimised or hidden dynamicfps.hiddenFps (1), so a game in
 * the background stops heating the machine. The integrated server keeps its own thread and pace. Off for dev tests.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin
{
    @Unique
    private static final int UNFOCUSED = OptimizedConfig.number("dynamicfps.unfocusedFps", 15);
    @Unique
    private static final int HIDDEN = OptimizedConfig.number("dynamicfps.hiddenFps", 1);
    @Unique
    private long deciFrameEnd;

    @Inject(method = "runGameLoop()V", at = @At("TAIL"))
    private void deciThrottle(CallbackInfo ci)
    {
        int fps = !Display.isVisible() ? HIDDEN : !Display.isActive() ? UNFOCUSED : 0;
        long now = System.nanoTime();
        if (fps > 0)
        {
            long wait = deciFrameEnd + 1_000_000_000L / fps - now;
            if (wait > 0)
            {
                try
                {
                    Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
                now = System.nanoTime();
            }
        }
        deciFrameEnd = now;
    }
}
