package net.decimation.worldgen.devtest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.decimation.fixes.Deci;
import net.decimation.fixes.PlayerSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * No more Steve (user, 22 Oktober 2026; fixes/PlayerSkins): players without a skin of their own wear one of Decimation's
 * 21 human skins, picked from the name. On the arena: the local player (never Steve: the dev account has a skin of its
 * own, which must stay; the production test account has none, so it wears ours), two stand-in players added to the client
 * world with names that pick other skins, one of them then given a skin "of its own" (as the skin manager does when a
 * download arrives), which must win and stay; Decimation's menu preview player (HUD stats box, fancy graphics on for the
 * test) wears the local player's; corpses spawned on the server with an owner's id, as Decimation's death handler makes
 * them: each wears its player's skin (the stand-in's ours, the local player's their own), the same GL texture; before,
 * Decimation drew Steve for both (crafatar.com by id). Shots: the three players from the front, the corpses.
 */
public class SkinsTest extends DevTestMode
{
    public String name() { return "skins"; }

    private static final int X = DevTestArena.X, Y = DevTestArena.Y, Z = DevTestArena.Z;
    private static final ResourceLocation OWN = new ResourceLocation("deci", "textures/model/entities/mob/infected/infected_0.png");
    private int ticks, viewBefore;
    private boolean fancyBefore;
    private volatile Runnable onServer;
    private volatile String owner, ownerA;
    private volatile boolean localOurs;
    private EntityOtherPlayerMP a, b;
    private Entity corpse, corpseA;

    public boolean client(Minecraft mc)
    {
        ticks++;
        EntityPlayer me = mc.thePlayer;
        if (ticks == 1)
        {
            onServer = this::arena;
            viewBefore = mc.gameSettings.thirdPersonView;
            fancyBefore = mc.gameSettings.fancyGraphics;
            mc.gameSettings.fancyGraphics = true;           // the HUD's stats box draws the menu preview player only then
        }
        if (ticks == 20)
        {
            me.rotationYaw = 0f;
            me.rotationPitch = 0f;
            String mine = me.getCommandSenderName();
            ResourceLocation got = mc.thePlayer.getLocationSkin();
            localOurs = PlayerSkins.skinFor(mine).equals(got);
            DevTestResults.check(name(), "the local player is never Steve (ours, or a skin of their own kept)", mine + ": " + got
                + (localOurs ? " (ours)" : " (their own)"), !got.equals(AbstractClientPlayer.locationStevePng), "not Steve");
            String nameA = other(mine, null), nameB = other(mine, nameA);
            a = stand(mc, -71001, nameA, X + 1.6);
            b = stand(mc, -71002, nameB, X - 1.6);
        }
        if (ticks == 25)
        {
            DevTestResults.check(name(), "another player gets the skin of their name", a.getCommandSenderName() + ": " + a.getLocationSkin(),
                a.getLocationSkin() == PlayerSkins.skinFor(a.getCommandSenderName()), String.valueOf(PlayerSkins.skinFor(a.getCommandSenderName())));
            DevTestResults.check(name(), "a third player another one", b.getCommandSenderName() + ": " + b.getLocationSkin(),
                b.getLocationSkin() == PlayerSkins.skinFor(b.getCommandSenderName()) && b.getLocationSkin() != a.getLocationSkin(), "a different skin of ours");
            mc.gameSettings.thirdPersonView = 2;
        }
        if (ticks == 40)
        {
            DevTestUtil.screenshot(mc, name(), "players");
            b.func_152121_a(MinecraftProfileTexture.Type.SKIN, OWN);   // as the skin manager does when an own skin arrives
        }
        if (ticks == 55)
        {
            DevTestResults.check(name(), "a skin of their own arriving later wins and stays", b.getLocationSkin().toString(),
                b.getLocationSkin() == OWN, OWN.toString());
            EntityPlayer preview = Deci.menuPreviewPlayer();
            ResourceLocation p = preview instanceof AbstractClientPlayer ? ((AbstractClientPlayer) preview).getLocationSkin() : null;
            DevTestResults.check(name(), "Decimation's menu preview player wears the same", String.valueOf(p),
                p != null && p.equals(mc.thePlayer.getLocationSkin()), String.valueOf(mc.thePlayer.getLocationSkin()));
            mc.gameSettings.thirdPersonView = 0;
            me.rotationYaw = 0f;
            me.rotationPitch = 50f;
            onServer = this::spawnCorpse;
        }
        if (ticks == 90)
        {
            String o = owner, oa = ownerA;
            ResourceLocation sa = oa == null ? null : PlayerSkins.corpseSkin(oa), so = o == null ? null : PlayerSkins.corpseSkin(o);
            int[] ta = oa == null ? new int[] {-1, -2} : PlayerSkins.corpseTextures(oa), to = o == null ? new int[] {-1, -2} : PlayerSkins.corpseTextures(o);
            DevTestResults.check(name(), "a corpse of a player in our skin wears it", sa + ", GL " + ta[0] + " / " + ta[1],
                sa != null && sa.equals(a.getLocationSkin()) && ta[0] > 0 && ta[0] == ta[1], a.getLocationSkin() + ", the same GL texture");
            DevTestResults.check(name(), "the local player's corpse wears their skin, ours or their own", so + ", GL " + to[0] + " / " + to[1]
                + (localOurs ? " (ours)" : " (their own)"), so != null && so.equals(mc.thePlayer.getLocationSkin()) && to[0] > 0 && to[0] == to[1],
                mc.thePlayer.getLocationSkin() + ", the same GL texture (Decimation drew Steve from crafatar)");
            DevTestUtil.screenshot(mc, name(), "corpse");
        }
        if (ticks == 95)
        {
            DevTestResults.value(name(), "skin hooks", String.format("%.2f us a call over %d calls", PlayerSkins.averageMicros(),
                PlayerSkins.handlerCalls()));
            DevTestResults.check(name(), "skin hook cost", String.format("%.2f us", PlayerSkins.averageMicros()),
                PlayerSkins.handlerCalls() > 50 && PlayerSkins.averageMicros() < 20, "under 20 us a call");
            mc.theWorld.removeEntityFromWorld(-71001);
            mc.theWorld.removeEntityFromWorld(-71002);
            mc.gameSettings.thirdPersonView = viewBefore;
            mc.gameSettings.fancyGraphics = fancyBefore;
            onServer = () ->
            {
                if (corpse != null) corpse.setDead();
                if (corpseA != null) corpseA.setDead();
            };
        }
        return ticks < 100;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }

    private void arena()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        w.setWorldTime(6000);
        p.setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
    }

    private void spawnCorpse()
    {
        EntityPlayerMP p = DevTestUtil.player();
        String id = p.getGameProfile().getId().toString(), idA = a.getGameProfile().getId().toString();
        corpse = Deci.newCorpse(p.worldObj, X - 1.0, Y, Z + 3.0, id);
        corpseA = Deci.newCorpse(p.worldObj, X + 2.0, Y, Z + 3.0, idA);
        boolean ok = p.worldObj.spawnEntityInWorld(corpse) & p.worldObj.spawnEntityInWorld(corpseA);
        DevTestResults.value(name(), "corpses spawned", ok + " for " + id + " and " + idA);
        owner = id;
        ownerA = idA;
    }

    /** A name whose skin differs from the local player's and from the one given. */
    private static String other(String mine, String not)
    {
        for (int i = 0; ; i++)
        {
            String n = "SkinTest" + i;
            ResourceLocation s = PlayerSkins.skinFor(n);
            if (s != PlayerSkins.skinFor(mine) && (not == null || s != PlayerSkins.skinFor(not)))
            {
                return n;
            }
        }
    }

    /** A stand-in player in the client world beside the local one, facing the same way. */
    private static EntityOtherPlayerMP stand(Minecraft mc, int id, String name, double x)
    {
        EntityOtherPlayerMP o = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        o.setPositionAndRotation(x, mc.thePlayer.posY - mc.thePlayer.yOffset, mc.thePlayer.posZ, 0f, 0f);
        o.rotationYawHead = 0f;
        mc.theWorld.addEntityToWorld(id, o);
        return o;
    }
}
