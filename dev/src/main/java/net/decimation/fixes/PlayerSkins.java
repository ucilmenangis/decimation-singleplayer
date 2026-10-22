package net.decimation.fixes;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * No more Steve (user, 22 Oktober 2026: "we dont use steve default skin, instead using decimation skin with randomize so
 * each player has unique skin ... random ID ... from 1-10 skin as example"; chosen: by the player's id, "as long not
 * distrub other feature especially multiplyaer", own skins kept, all 21 of Decimation's human skins). Minecraft shows
 * Steve while a player's locationSkin is null (AbstractClientPlayer.getLocationSkin); the skin manager fills it through
 * func_152121_a when the player's own skin arrives. A player who has none when joining a client world, or when first
 * drawn (Decimation's menu preview player too), gets human0 .. human20 through that same call, picked from the name:
 * every client and the server agree on it (on an offline server a client knows itself by its session id, the others by
 * the offline id made from the name). An own skin arriving later replaces ours. Nothing is sent: each client works it
 * out alone.
 * Corpses (Decimation's EntityCorpse) bind "skins/<owner id>", which Decimation fills from crafatar.com by id with Steve
 * as the fallback (Steve for every offline id, and for the dev account whose own skin the game shows). For every player
 * joining this client's world an alias texture goes under that name for both of the player's ids: it hands over the
 * player's current skin, so a corpse wears what its player wore, ours or their own. Never delete an alias's name from the
 * TextureManager: deleteTexture would free the player's own skin texture.
 */
public final class PlayerSkins
{
    static final int COUNT = 21;
    private static final ResourceLocation[] SKINS = new ResourceLocation[COUNT];
    static
    {
        for (int i = 0; i < COUNT; i++)
        {
            SKINS[i] = new ResourceLocation("deci", "textures/model/entities/mob/human/human" + i + ".png");
        }
    }

    /** For the dev test: handler calls and their time in nanoseconds. */
    static long calls, nanos;

    /** "skins/<id>" for a corpse: the texture of whatever its player wears now (the last seen once the player is gone). */
    static final class CorpseSkin implements ITextureObject
    {
        private WeakReference<AbstractClientPlayer> player;
        private ResourceLocation last;

        CorpseSkin(AbstractClientPlayer player)
        {
            follow(player);
        }

        void follow(AbstractClientPlayer p)
        {
            player = new WeakReference<AbstractClientPlayer>(p);
            last = p.getLocationSkin();
        }

        public void loadTexture(IResourceManager manager)
        {
        }

        public int getGlTextureId()
        {
            AbstractClientPlayer p = player.get();
            if (p != null)
            {
                last = p.getLocationSkin();
            }
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            ITextureObject t = tm.getTexture(last);
            if (t == null)
            {
                tm.bindTexture(last);                     // our human skins load on their first use
                t = tm.getTexture(last);
            }
            return t == null || t == this ? 0 : t.getGlTextureId();
        }

        ResourceLocation skin()
        {
            AbstractClientPlayer p = player.get();
            return p != null ? p.getLocationSkin() : last;
        }
    }

    /** Which of the skins a name gets: the name's hash, mixed (murmur3's finaliser) so close names spread out. */
    static int index(String name)
    {
        int h = name.toLowerCase(Locale.ROOT).hashCode();
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return Math.floorMod(h, COUNT);
    }

    /** The id an offline server gives this name (EntityPlayer's own rule). */
    static UUID offlineId(String name)
    {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    /** One of ours, by number (for the dev test). */
    public static ResourceLocation skin(int i)
    {
        return SKINS[i];
    }

    /** The skin a name gets (for the dev test). */
    public static ResourceLocation skinFor(String name)
    {
        return SKINS[index(name)];
    }

    /** Gives a player without a skin of their own one of ours; true when it did. */
    static boolean dress(AbstractClientPlayer p)
    {
        if (p.func_152123_o())
        {
            return false;                                 // a skin already: their own, or ours
        }
        String name = p.getCommandSenderName();
        if (name == null || name.isEmpty())
        {
            return false;
        }
        p.func_152121_a(MinecraftProfileTexture.Type.SKIN, SKINS[index(name)]);
        return true;
    }

    /** The corpse names of a player in this client's world follow that player's skin. */
    static void lendToCorpses(AbstractClientPlayer p)
    {
        String name = p.getCommandSenderName();
        if (name == null || name.isEmpty())
        {
            return;
        }
        String offline = offlineId(name).toString();
        String own = p.getGameProfile() == null || p.getGameProfile().getId() == null ? offline : p.getGameProfile().getId().toString();
        lend(p, offline);
        if (!own.equals(offline))
        {
            lend(p, own);
        }
    }

    private static void lend(AbstractClientPlayer p, String id)
    {
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        ResourceLocation key = new ResourceLocation("skins/" + id);
        ITextureObject t = tm.getTexture(key);
        if (t instanceof CorpseSkin)
        {
            ((CorpseSkin) t).follow(p);                   // the same player again (a respawn makes a new entity)
            return;
        }
        if (t != null)
        {
            tm.deleteTexture(key);                        // Decimation's crafatar download: its own texture, freed
        }
        tm.loadTexture(key, new CorpseSkin(p));
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e)
    {
        if (e.world.isRemote && e.entity instanceof AbstractClientPlayer && e.world == Minecraft.getMinecraft().theWorld)
        {
            AbstractClientPlayer p = (AbstractClientPlayer) e.entity;
            dress(p);
            lendToCorpses(p);
        }
    }

    /** Every drawn player (Decimation's menu preview player too, which joins no world): one without a skin gets ours. */
    @SubscribeEvent
    public void onPlayer(RenderPlayerEvent.Pre e)
    {
        if (e.entityPlayer instanceof AbstractClientPlayer)
        {
            long start = System.nanoTime();
            dress((AbstractClientPlayer) e.entityPlayer);
            calls++;
            nanos += System.nanoTime() - start;
        }
    }

    /** For the dev test: the skin a corpse of this owner draws, or null when Decimation's own way draws it. */
    public static ResourceLocation corpseSkin(String owner)
    {
        ITextureObject t = Minecraft.getMinecraft().getTextureManager().getTexture(new ResourceLocation("skins/" + owner));
        return t instanceof CorpseSkin ? ((CorpseSkin) t).skin() : null;
    }

    /** For the dev test: the GL texture a corpse of this owner binds, and the one its skin has. */
    public static int[] corpseTextures(String owner)
    {
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        ITextureObject t = tm.getTexture(new ResourceLocation("skins/" + owner));
        ResourceLocation skin = corpseSkin(owner);
        ITextureObject s = skin == null ? null : tm.getTexture(skin);
        return new int[] {t == null ? -1 : t.getGlTextureId(), s == null ? -1 : s.getGlTextureId()};
    }

    /** For the dev test: average handler time in microseconds. */
    public static double averageMicros()
    {
        return calls == 0 ? 0 : nanos / 1000.0 / calls;
    }

    public static long handlerCalls()
    {
        return calls;
    }
}
