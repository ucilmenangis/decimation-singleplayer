package net.decimation.worldgen.devtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.decimation.fixes.Deci;
import net.decimation.fixes.HumanLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * What drawing NPCs costs the client per frame (fixes/HumanLayers, PatchHumanLayers; the bench's frame times could not
 * tell: Mac load and the 60 fps cap hide it). Scenes in front of the player, in view: a camp (50 civilians, 30 guards
 * with gear standing still), a horde (80 infected) and a fight (`-Pscenes=fight`: 20 soldiers and 20 bandits shooting
 * at each other across the view). Per frame the time from RenderLivingEvent.Pre to Post of
 * every human NPC is summed (the whole NPC: model, gear, held gun, name), drawn the new way and the old way (Decimation's
 * seven layers, three line of sight checks and model parts in two lists of tiny draws: HumanLayers off, sightChecks on,
 * ModelBatch off) in turns of 30 frames, so a change in
 * the Mac's load hits both alike. Frame cap and vsync off.
 */
public class NpcDrawTest extends DevTestMode
{
    public String name() { return "npcdraw"; }

    /** -Pscenes=camp picks some. */
    private static final String[] SCENES = System.getProperty("deciworldgen.autotest.scenes", "camp,horde").split(",");
    private static final int TURN = 30, FRAMES = Integer.getInteger("deciworldgen.autotest.frames", 1200);
    private static Timer timer;

    private final List<Entity> spawned = new ArrayList<Entity>();
    private volatile int scene = -1, step; // step 0 the server sets the scene up, 1 measure, 2 cleared
    private volatile boolean ready;
    private String spawning;
    private int ticks;

    /** Sums NPC draw time per frame, by way. */
    public static final class Timer
    {
        boolean on;
        int frame;
        long start, sum;
        int drawnNow;
        final List<long[]> newWay = new ArrayList<long[]>(), oldWay = new ArrayList<long[]>(); // {ns, NPCs drawn, frame ns}
        long frameStart;
        boolean flashing;
        /** Draws of an NPC whose gun flashed / did not: {ns, count}. */
        final long[] flash = new long[2], calm = new long[2];

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void pre(RenderLivingEvent.Pre e)
        {
            start = on && Deci.humanLook(e.entity, LOOK) != null ? System.nanoTime() : 0; // a cancelled one never posts
            flashing = start != 0 && Deci.gunFlashTicks(e.entity.getHeldItem()) > 0;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void post(RenderLivingEvent.Post e)
        {
            if (on && start != 0)
            {
                long ns = System.nanoTime() - start;
                long[] c = flashing ? flash : calm;
                c[0] += ns;
                c[1]++;
                sum += ns;
                drawnNow++;
                start = 0;
            }
        }

        @SubscribeEvent
        public void onFrame(TickEvent.RenderTickEvent e)
        {
            if (e.phase != TickEvent.Phase.START || !on)
            {
                return;
            }
            long now = System.nanoTime();
            if (frame > 0)
            {
                (HumanLayers.enabled ? newWay : oldWay).add(new long[] {sum, drawnNow, now - frameStart});
            }
            frameStart = now;
            sum = 0;
            drawnNow = 0;
            frame++;
            boolean old = frame / TURN % 2 == 1;           // this frame's way, in turns of 30 frames
            HumanLayers.enabled = !old;
            HumanLayers.sightChecks = old;
            net.decimation.fixes.ModelBatch.enabled = !old;
        }
    }

    private static final int[] LOOK = new int[8];

    public boolean client(Minecraft mc)
    {
        if (timer == null)
        {
            timer = new Timer();
            MinecraftForge.EVENT_BUS.register(timer);
            FMLCommonHandler.instance().bus().register(timer);
        }
        if (mc.thePlayer == null)
        {
            return true;
        }
        if (scene < 0)
        {
            DevTestUtil.unlimitedFps(mc);
            scene = 0;
            step = 0;
            ready = false;
            return true;
        }
        if (scene >= SCENES.length)
        {
            HumanLayers.enabled = true;
            HumanLayers.sightChecks = false;
            net.decimation.fixes.ModelBatch.enabled = true;
            return false;
        }
        if (!ready)
        {
            return true;
        }
        ticks++;
        if (ticks == 60)                                       // 3 s: client copies there, pictures made
        {
            timer.newWay.clear();
            timer.oldWay.clear();
            timer.flash[0] = timer.flash[1] = timer.calm[0] = timer.calm[1] = 0;
            timer.frame = 0;
            timer.on = true;
            DevTestUtil.screenshot(mc, name(), "npcdraw_" + SCENES[scene] + ".png");
        }
        if (ticks > 60 && timer.newWay.size() + timer.oldWay.size() >= FRAMES)
        {
            timer.on = false;
            HumanLayers.enabled = true;
            HumanLayers.sightChecks = false;
            net.decimation.fixes.ModelBatch.enabled = true;
            double[] n = stats(timer.newWay), o = stats(timer.oldWay);
            String s = SCENES[scene];
            DevTestResults.value(name(), s + " new way", String.format("NPCs %.0f a frame: %.2f ms drawing them (%.1f us each),"
                + " frame median %.1f ms (%d frames)", n[1], n[0], n[2], n[3], timer.newWay.size()));
            DevTestResults.value(name(), s + " old way", String.format("NPCs %.0f a frame: %.2f ms drawing them (%.1f us each),"
                + " frame median %.1f ms (%d frames)", o[1], o[0], o[2], o[3], timer.oldWay.size()));
            if (timer.flash[1] > 0)
            {
                DevTestResults.value(name(), s + " muzzle flash", String.format("an NPC drawn while its gun flashes %.1f us (%d draws),"
                    + " not flashing %.1f us (%d draws)", timer.flash[0] / 1e3 / timer.flash[1], timer.flash[1],
                    timer.calm[1] == 0 ? 0 : timer.calm[0] / 1e3 / timer.calm[1], timer.calm[1]));
            }
            DevTestResults.check(name(), s + ": NPCs cheaper to draw", String.format("%.0f %% of the old time", 100 * n[0] / o[0]),
                                 n[1] > 20 && o[1] > 20 && n[0] < o[0], "the new way takes less time a frame, 20+ NPCs drawn");
            ticks = 0;
            ready = false;
            step = 2;
        }
        return true;
    }

    /** mean ms drawing NPCs a frame, mean NPCs drawn, mean us per NPC, median frame ms. */
    private static double[] stats(List<long[]> l)
    {
        if (l.isEmpty())
        {
            return new double[] {0, 0, 0, 0};
        }
        double ns = 0, npcs = 0;
        long[] frames = new long[l.size()];
        for (int i = 0; i < l.size(); i++)
        {
            ns += l.get(i)[0];
            npcs += l.get(i)[1];
            frames[i] = l.get(i)[2];
        }
        Arrays.sort(frames);
        return new double[] {ns / l.size() / 1e6, npcs / l.size(), npcs == 0 ? 0 : ns / npcs / 1e3, frames[frames.length / 2] / 1e6};
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null || scene < 0)
        {
            return;
        }
        if (ready)
        {
            if (scene < SCENES.length && SCENES[scene].equals("fight"))
            {
                for (Entity e : spawned)
                {
                    if (e instanceof EntityLiving && e.isEntityAlive())
                    {
                        ((EntityLiving) e).setHealth(((EntityLiving) e).getMaxHealth()); // they fight on, nobody dies
                    }
                }
            }
            return;
        }
        if (scene >= SCENES.length)
        {
            return;
        }
        if (step == 2)
        {
            for (Entity e : spawned)
            {
                e.setDead();
            }
            spawned.clear();
            if (scene + 1 >= SCENES.length && spawning != null)
            {
                w.getGameRules().setOrCreateGameRule("doMobSpawning", spawning); // the last scene: as the world had it
                spawning = null;
            }
            scene++;
            step = 0;
            return;
        }
        if (spawning == null)
        {
            spawning = w.getGameRules().getGameRuleStringValue("doMobSpawning");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        }
        DevTestArena.build(w, p);
        w.setWorldTime(18000);
        w.getWorldInfo().setRaining(false);
        w.getWorldInfo().setThundering(false);
        p.setGameType(WorldSettings.GameType.CREATIVE); // Decimation's NPCs leave a creative player alone
        p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 90, 12); // west
        boolean fight = SCENES[scene].equals("fight");
        for (int i = 0; i < (fight ? 40 : 80); i++)
        {
            String kind = SCENES[scene].equals("horde") ? "Infected" : fight ? (i < 20 ? "Soldier" : "Bandit")
                : i < 50 ? "Human" : "TraderGuard";
            double x = DevTestArena.X - 8 - (i / 10) * 2.0, z = DevTestArena.Z + 0.5 + (i % 10 - 4.5) * 1.6;
            if (fight) // soldiers on the left, bandits on the right of the view, 18 blocks apart: they shoot across it
            {
                x = DevTestArena.X - 12 - (i % 20) / 5 * 2.0;
                z = DevTestArena.Z + 0.5 + (i < 20 ? -9 : 9) + (i % 5 - 2) * 1.5;
            }
            for (int tries = 0; tries < 5; tries++) // Decimation turns some spawns into other mobs: retried
            {
                Entity e = EntityList.createEntityByName(kind, w);
                if (!(e instanceof EntityLiving))
                {
                    break;
                }
                e.setLocationAndAngles(x, DevTestArena.Y, z, 270, 0);
                if (w.spawnEntityInWorld(e))
                {
                    spawned.add(e);
                    break;
                }
            }
        }
        ticks = 0;
        step = 1;
        ready = true;
    }
}
