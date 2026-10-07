package net.minecraft.world;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.biome.WorldChunkManager;
// COMPILE-TIME SHIM ONLY. Mirrors the Forge-binpatched WorldType surface.
// Forge-added members keep readable names, so these link at runtime.
public class WorldType {
    public static final WorldType[] worldTypes = new WorldType[16];
    protected WorldType(String name) {}
    public String getWorldTypeName() { return null; }
    public IChunkProvider getChunkGenerator(World world, String generatorOptions) { return null; }
    public WorldChunkManager getChunkManager(World world) { return null; }
    public float getCloudHeight() { return 0f; }
    public double getHorizon(World world) { return 0d; }
    public int getMinimumSpawnHeight(World world) { return 0; }
    public boolean hasVoidParticles(boolean flag) { return false; }
    public double voidFadeMagnitude() { return 0d; }
}
