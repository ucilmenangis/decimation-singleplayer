package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ChunkCoordinates;

/**
 * bug.md "FPS falls minute by minute": Decimation starts a looping sound for every radio, vending machine, electric box,
 * generator and hazard light the player comes within 16 blocks of (BlockProp.randomDisplayTick), and stops it only when
 * the prop's chunk unloads, so a walk through a city leaves dozens of loops running (the radio's is streamed: decoded in
 * Java, locked against the game's sound updates every tick). Once a second: a loop farther than {@link #HEAR} blocks is
 * stopped and forgotten (it starts again when the player comes back: before, a radio stayed silent after its chunk had
 * been reloaded), and at most {@link #MAX} loops play at once, the nearest; the others are stopped but kept, so Decimation
 * does not start them again while the player stays near.
 */
@SideOnly(Side.CLIENT)
public final class PropSounds
{
    /** Sounds of volume 1 or less fade out at 16 blocks (SoundManager): beyond this nobody hears them. */
    static final double HEAR = 20;
    static final int MAX = 6;
    /** Passes in a row a loop that should play was silent. */
    static final Map<ISound, Integer> SILENT = new java.util.IdentityHashMap<ISound, Integer>();
    /** Loops this class stopped while their prop is still near (identity: ISound has no equals). */
    static final java.util.Set<ISound> MUTED = Collections.newSetFromMap(new java.util.IdentityHashMap<ISound, Boolean>());
    private int ticks;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null || ++ticks % 20 != 0)
        {
            return;
        }
        final SoundHandler handler = mc.getSoundHandler();
        try
        {
            pass(handler, mc);
        }
        catch (java.util.ConcurrentModificationException ex)
        {
            // Decimation clears the map on a disconnect, from the network thread: the next pass sees the new map
        }
    }

    private static void pass(final SoundHandler handler, Minecraft mc)
    {
        trim(Deci.propSounds(), new Sounds()
        {
            public boolean playing(ISound s)
            {
                return handler.isSoundPlaying(s);
            }

            public void stop(ISound s)
            {
                handler.stopSound(s);
            }
        }, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    /** The two calls on Minecraft's sound handler this needs (unit tests give their own). */
    interface Sounds
    {
        boolean playing(ISound s);

        void stop(ISound s);
    }

    /**
     * One pass: loops beyond hearing are stopped and forgotten; of the rest the {@link #MAX} nearest may play (one this
     * class stopped is forgotten, so Decimation starts it again) and the others are stopped and kept (no restart).
     * Only loops stopped here are ever forgotten while near: Minecraft reports a loop started this tick as not playing yet,
     * and forgetting that one would leave it playing with nobody to stop it.
     */
    static void trim(Map<ChunkCoordinates, ISound> loops, Sounds sounds, final double px, final double py, final double pz)
    {
        List<Map.Entry<ChunkCoordinates, ISound>> near = new ArrayList<Map.Entry<ChunkCoordinates, ISound>>();
        for (Iterator<Map.Entry<ChunkCoordinates, ISound>> it = loops.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<ChunkCoordinates, ISound> en = it.next();
            if (en.getValue() == null)
            {
                it.remove();
            }
            else if (distanceSq(en.getKey(), px, py, pz) > HEAR * HEAR)
            {
                sounds.stop(en.getValue()); // works from the moment it was started (Minecraft knows its channel)
                MUTED.remove(en.getValue());
                it.remove(); // Decimation starts it again when the player is near
            }
            else
            {
                near.add(en);
            }
        }
        MUTED.retainAll(loops.values());
        SILENT.keySet().retainAll(loops.values());
        Collections.sort(near, new Comparator<Map.Entry<ChunkCoordinates, ISound>>()
        {
            public int compare(Map.Entry<ChunkCoordinates, ISound> a, Map.Entry<ChunkCoordinates, ISound> b)
            {
                return Double.compare(distanceSq(a.getKey(), px, py, pz), distanceSq(b.getKey(), px, py, pz));
            }
        });
        for (int i = 0; i < near.size(); i++)
        {
            Map.Entry<ChunkCoordinates, ISound> en = near.get(i);
            boolean muted = MUTED.contains(en.getValue());
            if (i < MAX && muted)
            {
                MUTED.remove(en.getValue());
                loops.remove(en.getKey()); // one of the nearest again: may start again
            }
            else if (i < MAX && sounds.playing(en.getValue()))
            {
                SILENT.remove(en.getValue());
            }
            else if (i < MAX)
            {
                Integer n = SILENT.get(en.getValue());
                SILENT.put(en.getValue(), n == null ? 1 : n + 1);
                if (n != null && n + 1 >= 3)
                {
                    SILENT.remove(en.getValue());
                    sounds.stop(en.getValue()); // in case it was only slow to start: never two loops of one prop
                    loops.remove(en.getKey()); // silent for 3 passes (its channel was taken): may start again
                }
            }
            else if (i >= MAX && !muted)
            {
                sounds.stop(en.getValue());
                MUTED.add(en.getValue()); // the entry stays: no restart while the player is near
            }
        }
    }

    static double distanceSq(ChunkCoordinates c, double px, double py, double pz)
    {
        double dx = c.posX + 0.5 - px, dy = c.posY + 0.5 - py, dz = c.posZ + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }
}
