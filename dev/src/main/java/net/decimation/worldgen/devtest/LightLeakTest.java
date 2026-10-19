package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

/**
 * Lighting left off by one mob's drawing for every mob drawn after it (user 19 Oktober 2026: "when npc shot a gun, some
 * 'flash' rendered on their npc or mobs body ... the flash appear on all npc or mobs body"). Decimation's gun renderer
 * turns item lighting off while the gun flashes (GunItemRenderer.renderItem, MuzzleFlashModel) and never back on; so
 * does the player model of a firing player (ModelDeciPlayer); an NPC's night vision goggles turn both lights off and only
 * lighting back on (ModelHuman), so the NPC's own vest and backpack drawn after them are dark too. On the arena, drawn in
 * this order: the player in third person with a flashing AKM, a soldier whose gun keeps flashing (set on the client
 * copy every frame), a civilian with night vision goggles, then 6 infected and 2 civilians. Every mob must start its
 * drawing (RenderLivingEvent.Pre, after every other handler) with lighting, both lights and colour material on, and the
 * goggles civilian must end its own drawing (Post, before other handlers) with both lights on.
 */
public class LightLeakTest extends DevTestMode
{
    public String name() { return "lightleak"; }

    private static Probe probe;
    private volatile int step;            // 0 the server sets up, 1 measure, 2 the server clears, 3 done
    private volatile boolean ready;
    private volatile int shooter = -1, goggled = -1;
    private final java.util.List<Entity> spawned = new java.util.ArrayList<Entity>();
    private String spawning;
    private int ticks, view;

    /** Lighting state seen by each mob, counted while {@link #on}. */
    public static final class Probe
    {
        volatile boolean on;
        int shooter = -1, goggled = -1;
        int checked, leaked, shooterLeft, playerLeft, gogglesDark, gogglesSeen, playerSeen, shooterSeen;
        String firstLeak;

        @SubscribeEvent
        public void frame(TickEvent.RenderTickEvent e)
        {
            Minecraft mc = Minecraft.getMinecraft();
            if (e.phase != TickEvent.Phase.START || !on || mc.theWorld == null)
            {
                return;
            }
            Entity s = mc.theWorld.getEntityByID(shooter);
            if (s instanceof EntityLiving)
            {
                Deci.setGunFlash(((EntityLiving) s).getHeldItem(), 5); // the soldier's gun flashes the whole time
            }
            if (mc.thePlayer != null)
            {
                Deci.setGunFlash(mc.thePlayer.getHeldItem(), 5);
            }
            Entity g = mc.theWorld.getEntityByID(goggled);
            ItemStack[] gear = Deci.humanGear(g);
            Item nv = cpw.mods.fml.common.registry.GameRegistry.findItem("deci", "nvgoggles");
            if (gear != null && nv != null && (gear[0] == null || gear[0].getItem() != nv))
            {
                gear[0] = new ItemStack(nv);
                Deci.setHumanGear(g, gear);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void pre(RenderLivingEvent.Pre e)
        {
            if (!on)
            {
                return;
            }
            checked++;
            if (!lit())
            {
                leaked++;
                if (firstLeak == null)
                {
                    firstLeak = e.entity.getClass().getSimpleName() + " " + state();
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void post(RenderLivingEvent.Post e)
        {
            if (!on)
            {
                return;
            }
            int id = e.entity.getEntityId();
            if (id == shooter)
            {
                shooterSeen++;
                shooterLeft += GL11.glIsEnabled(GL11.GL_LIGHTING) ? 0 : 1; // the leak's source ran
            }
            else if (e.entity == Minecraft.getMinecraft().thePlayer)
            {
                playerSeen++;
                playerLeft += GL11.glIsEnabled(GL11.GL_LIGHTING) ? 0 : 1;
            }
            else if (id == goggled)
            {
                gogglesSeen++;
                gogglesDark += GL11.glIsEnabled(GL11.GL_LIGHT0) && GL11.glIsEnabled(GL11.GL_LIGHT1)
                               && GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL) ? 0 : 1;
            }
        }

        static boolean lit()
        {
            return GL11.glIsEnabled(GL11.GL_LIGHTING) && GL11.glIsEnabled(GL11.GL_LIGHT0) && GL11.glIsEnabled(GL11.GL_LIGHT1)
                && GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        }

        static String state()
        {
            return String.format("lighting %b light0 %b light1 %b colour %b", GL11.glIsEnabled(GL11.GL_LIGHTING),
                                 GL11.glIsEnabled(GL11.GL_LIGHT0), GL11.glIsEnabled(GL11.GL_LIGHT1),
                                 GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL));
        }
    }

    public boolean client(Minecraft mc)
    {
        if (probe == null)
        {
            probe = new Probe();
            MinecraftForge.EVENT_BUS.register(probe);
            FMLCommonHandler.instance().bus().register(probe);
        }
        if (mc.thePlayer == null || !ready)
        {
            return step != 3;
        }
        ticks++;
        if (ticks == 20)                                       // client copies there, the player holds the AKM
        {
            view = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;               // the player drawn among the mobs, first in the list
            probe.shooter = shooter;
            probe.goggled = goggled;
            probe.checked = probe.leaked = probe.shooterLeft = probe.playerLeft = probe.gogglesDark = 0;
            probe.gogglesSeen = probe.playerSeen = probe.shooterSeen = 0;
            probe.firstLeak = null;
            probe.on = true;
        }
        if (ticks == 50)
        {
            DevTestUtil.screenshot(mc, name(), "lightleak.png");
        }
        if (ticks == 80)
        {
            probe.on = false;
            mc.gameSettings.thirdPersonView = view;
            DevTestResults.value(name(), "leak sources", String.format("soldier's flashing gun left lighting off in %d of %d"
                + " drawings, the firing player in %d of %d; goggles civilian drawn %d times", probe.shooterLeft,
                probe.shooterSeen, probe.playerLeft, probe.playerSeen, probe.gogglesSeen));
            DevTestResults.check(name(), "mobs start drawing lit", probe.leaked + " of " + probe.checked,
                                 probe.checked > 100 && probe.leaked == 0 && probe.shooterSeen > 0,
                                 "every mob starts with lighting, both lights and colour material on"
                                 + (probe.firstLeak == null ? "" : "; first leak: " + probe.firstLeak));
            DevTestResults.check(name(), "goggles NPC's gear lit", probe.gogglesDark + " of " + probe.gogglesSeen,
                                 probe.gogglesSeen > 10 && probe.gogglesDark == 0,
                                 "both lights on again after the night vision goggles");
            ready = false;
            step = 2;
        }
        return true;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || ready || step == 3)
        {
            return;
        }
        if (step == 0)
        {
            spawning = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            DevTestArena.build(w, p);
            w.setWorldTime(18000);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            p.setGameType(WorldSettings.GameType.CREATIVE); // Decimation's NPCs leave a creative player alone
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 90, 10);
            Item akm = cpw.mods.fml.common.registry.GameRegistry.findItem("deci", "akm");
            if (akm != null)
            {
                p.inventory.setInventorySlotContents(p.inventory.currentItem, new ItemStack(akm));
            }
            Entity s = spawn(w, "Soldier", DevTestArena.X - 7, DevTestArena.Z - 2);
            Entity g = spawn(w, "Human", DevTestArena.X - 7, DevTestArena.Z + 2);
            for (int i = 0; i < 8; i++)
            {
                spawn(w, i < 6 ? "Infected" : "Human", DevTestArena.X - 10 - (i % 2) * 2, DevTestArena.Z - 4 + i);
            }
            shooter = s == null ? -1 : s.getEntityId();
            goggled = g == null ? -1 : g.getEntityId();
            ticks = 0;
            step = 1;
            ready = true;
        }
        else if (step == 2)
        {
            for (Entity e : spawned)
            {
                e.setDead();
            }
            spawned.clear();
            w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning);
            step = 3;
        }
    }

    /** A mob facing the player (Decimation turns some spawns into other mobs: retried). */
    private Entity spawn(WorldServer w, String kind, double x, double z)
    {
        for (int tries = 0; tries < 5; tries++)
        {
            Entity e = EntityList.createEntityByName(kind, w);
            if (!(e instanceof EntityLiving))
            {
                return null;
            }
            e.setLocationAndAngles(x + 0.5, DevTestArena.Y, z + 0.5, 270, 0);
            if (w.spawnEntityInWorld(e))
            {
                spawned.add(e);
                return e;
            }
        }
        return null;
    }
}
