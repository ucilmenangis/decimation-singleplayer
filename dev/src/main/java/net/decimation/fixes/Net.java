package net.decimation.fixes;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Our own network channel (22 Oktober 2026; before, our features rode on Decimation's packets, data watcher slots and
 * vanilla container buttons). Message ids are fixed: never reuse or renumber one, add new ones at the end.
 */
public final class Net
{
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("deciworldgen");

    private Net()
    {
    }

    /** Both sides, once at preInit. */
    public static void register()
    {
        CHANNEL.registerMessage(MagUnload.Handler.class, MagUnload.Message.class, 0, Side.SERVER);   // unload the held magazine
    }
}
