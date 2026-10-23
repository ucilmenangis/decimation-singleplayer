package net.decimation.optimized.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.decimation.optimized.entityculling.Cullable;
import net.minecraft.entity.Entity;

/** The culling thread's answer for an entity. */
@Mixin(Entity.class)
public abstract class EntityMixin implements Cullable
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
