package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * A player corpse opens on a right click with a gun in hand (bug.md "A joined player could not open a corpse": Decimation
 * unbinds the use key while a gun is held; fixes/GunUseKey). Singleplayer: a corpse with apples 2 blocks in front of the
 * player, an AKM in hand, aim at it, right click (the use key), the server's chest window (id above 0) must open.
 */
public class CorpseGunTest extends DevTestMode
{
    public String name() { return "corpsegun"; }

    private int ticks, waited;
    private volatile int corpseId = -1;
    private volatile boolean placed;

    public boolean client(Minecraft mc)
    {
        ticks++;
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        if (!placed)
        {
            ticks = 0; // the steps below count from the placement
            if (++waited >= 200)
            {
                DevTestResults.check(name(), "corpse placed", false, false, "true");
                net.decimation.worldgen.DevAutoTest.keepScreen = false;
                return false;
            }
            return true;
        }
        Entity c = corpseId < 0 ? null : mc.theWorld.getEntityByID(corpseId);
        if (ticks == 40 && c != null) // aim at its middle the way a player would
        {
            double dx = c.posX - mc.thePlayer.posX, dz = c.posZ - mc.thePlayer.posZ;
            double dy = c.posY + c.height / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            mc.thePlayer.rotationYaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
            mc.thePlayer.rotationPitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
        }
        if (ticks == 50)
        {
            ItemStack held = mc.thePlayer.getHeldItem();
            DevTestResults.value(name(), "holding", held == null ? "nothing" : held.getDisplayName());
            DevTestResults.check(name(), "crosshair on the corpse", mc.objectMouseOver == null ? "nothing" : String.valueOf(mc.objectMouseOver.entityHit),
                                 mc.objectMouseOver != null && Deci.isCorpse(mc.objectMouseOver.entityHit), "the corpse");
            net.minecraft.client.settings.KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode()); // a right click
        }
        GuiContainer gui = mc.currentScreen instanceof GuiContainer ? (GuiContainer) mc.currentScreen : null;
        if (ticks > 50 && gui != null && gui.inventorySlots.windowId != 0 || ticks > 200)
        {
            DevTestResults.check(name(), "right click with a gun opens the corpse", gui == null ? "no screen" : "window " + gui.inventorySlots.windowId,
                                 gui != null && gui.inventorySlots.windowId != 0, "the server's chest window");
            mc.thePlayer.closeScreen();
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            removeCorpse();
            return false;
        }
        return true;
    }

    @Override
    public void server()
    {
        if (placed)
        {
            return;
        }
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null)
        {
            return;
        }
        DevTestArena.build(w, p);
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.setPositionAndUpdate(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5);
        Item gun = (Item) Item.itemRegistry.getObject("deci:akm");
        p.inventory.setInventorySlotContents(p.inventory.currentItem, gun == null ? null : new ItemStack(gun));
        p.inventoryContainer.detectAndSendChanges();
        Entity c = Deci.newCorpse(w, DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 1.5, p.getUniqueID().toString(),
                                  new ItemStack(net.minecraft.init.Items.apple, 5));
        corpseId = w.spawnEntityInWorld(c) ? c.getEntityId() : -1;
        DevTestResults.value(name(), "corpse placed", corpseId >= 0);
        placed = true;
    }

    private void removeCorpse()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        Entity c = w.getEntityByID(corpseId);
        if (c != null)
        {
            c.setDead();
        }
    }
}
