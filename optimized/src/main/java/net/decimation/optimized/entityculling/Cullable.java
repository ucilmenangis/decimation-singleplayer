package net.decimation.optimized.entityculling;

/** Added to Entity and BlockEntity by the mixins: whether the culling thread found it hidden behind solid blocks. */
public interface Cullable
{
    boolean deciCulled();

    void deciSetCulled(boolean culled);
}
