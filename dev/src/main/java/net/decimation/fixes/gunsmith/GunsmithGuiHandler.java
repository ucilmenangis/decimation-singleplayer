package net.decimation.fixes.gunsmith;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.decimation.fixes.Deci;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Decimation's GUI handler with one change: its attachments id (opened by its own attach key, I, through its ATTACH
 * action) opens the Gunsmith screen and container instead of AttachmentScreen. Every other id goes to Decimation. No
 * jar patch and no network channel of ours: FML keeps one handler per mod, so ours replaces Decimation's entry and keeps
 * the old one to hand on to. The client side is reached through GunsmithClient so a dedicated server never loads a
 * client class.
 */
public final class GunsmithGuiHandler implements IGuiHandler
{
    private final IGuiHandler decimation;

    private GunsmithGuiHandler(IGuiHandler decimation)
    {
        this.decimation = decimation;
    }

    /** Wraps Decimation's handler once (postInit, both sides). */
    public static void install()
    {
        IGuiHandler old = Deci.decimationGuiHandler();
        if (old != null && !(old instanceof GunsmithGuiHandler))
        {
            NetworkRegistry.INSTANCE.registerGuiHandler(Deci.decimationMod(), new GunsmithGuiHandler(old));
        }
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (id != Deci.attachmentsGuiId())
        {
            return decimation.getServerGuiElement(id, player, world, x, y, z);
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !Deci.isGun(held.getItem()))
        {
            GunsmithContainer.tell(player, "Hold a gun to modify it");
            return null;
        }
        return new GunsmithContainer(player);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        return id == Deci.attachmentsGuiId() ? GunsmithClient.screen(player)
                                             : decimation.getClientGuiElement(id, player, world, x, y, z);
    }
}
