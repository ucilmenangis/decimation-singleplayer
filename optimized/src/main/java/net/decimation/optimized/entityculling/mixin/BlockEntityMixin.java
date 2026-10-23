package net.decimation.optimized.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.decimation.optimized.entityculling.Cullable;
import net.minecraft.block.entity.BlockEntity;

/** The culling thread's answer for a block entity. */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements Cullable
{
    @Unique
    private volatile boolean deciCulled;

    @Override
    public boolean deciCulled()
    {
        return deciCulled;
    }

    @Override
    public void deciSetCulled(boolean culled)
    {
        deciCulled = culled;
    }
}
