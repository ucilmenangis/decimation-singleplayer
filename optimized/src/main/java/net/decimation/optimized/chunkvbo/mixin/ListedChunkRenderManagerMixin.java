package net.decimation.optimized.chunkvbo.mixin;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.decimation.optimized.chunkvbo.ChunkVbo;
import net.minecraft.client.render.world.ListedChunkRenderManager;

/** RenderList.callLists (yarn ListedChunkRenderManager): the region's display lists drawn through ChunkVbo. */
@Mixin(ListedChunkRenderManager.class)
public abstract class ListedChunkRenderManagerMixin
{
    @Redirect(method = "method_1391()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallLists(Ljava/nio/IntBuffer;)V", remap = false))
    private void deciDraw(IntBuffer lists)
    {
        ChunkVbo.draw(lists);
    }
}
