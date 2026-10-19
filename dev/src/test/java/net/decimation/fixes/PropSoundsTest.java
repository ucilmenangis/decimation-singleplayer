package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prop loops: stopped out of hearing, 6 at most, never two of one prop (bug.md "FPS falls minute by minute"). */
class PropSoundsTest
{
    /** What the game's sound handler would say: playing sounds, and the ones stopped. */
    static final class FakeSounds implements PropSounds.Sounds
    {
        final Set<ISound> playing = new HashSet<ISound>();
        final Set<ISound> stopped = new HashSet<ISound>();

        public boolean playing(ISound s)
        {
            return playing.contains(s);
        }

        public void stop(ISound s)
        {
            playing.remove(s);
            stopped.add(s);
        }
    }

    static ISound loop()
    {
        return new ISound()
        {
            public ResourceLocation getPositionedSoundLocation() { return new ResourceLocation("deci", "block.radio.idle"); }
            public boolean canRepeat() { return true; }
            public int getRepeatDelay() { return 0; }
            public float getVolume() { return 0.5f; }
            public float getPitch() { return 1f; }
            public float getXPosF() { return 0f; }
            public float getYPosF() { return 0f; }
            public float getZPosF() { return 0f; }
            public AttenuationType getAttenuationType() { return AttenuationType.LINEAR; }
        };
    }

    final Map<ChunkCoordinates, ISound> loops = new HashMap<ChunkCoordinates, ISound>();
    final FakeSounds sounds = new FakeSounds();

    @BeforeEach
    void clean()
    {
        PropSounds.MUTED.clear();
        PropSounds.SILENT.clear();
    }

    ISound playingAt(int x)
    {
        ISound s = loop();
        loops.put(new ChunkCoordinates(x, 0, 0), s);
        sounds.playing.add(s);
        return s;
    }

    @Test
    void aLoopOutOfHearingIsStoppedAndForgotten()
    {
        ISound far = playingAt(40), near = playingAt(5);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        assertTrue(sounds.stopped.contains(far));
        assertFalse(loops.containsValue(far)); // Decimation may start it again later
        assertTrue(sounds.playing.contains(near));
        assertTrue(loops.containsValue(near));
    }

    @Test
    void onlyTheNearestSixPlayAndTheOthersStayKnown()
    {
        ISound[] s = new ISound[10];
        for (int i = 0; i < 10; i++)
        {
            s[i] = playingAt(i + 1);
        }
        PropSounds.trim(loops, sounds, 0, 0, 0);
        for (int i = 0; i < 10; i++)
        {
            assertEquals(i < PropSounds.MAX, sounds.playing.contains(s[i]), "loop " + i);
        }
        assertEquals(10, loops.size()); // kept: Decimation does not start the muted ones again while near
        PropSounds.trim(loops, sounds, 0, 0, 0);
        assertEquals(10, loops.size());
        assertEquals(PropSounds.MAX, sounds.playing.size());
    }

    @Test
    void aMutedLoopThatBecomesOneOfTheNearestIsForgottenToStartAgain()
    {
        ISound[] s = new ISound[8];
        for (int i = 0; i < 8; i++)
        {
            s[i] = playingAt(i + 1);
        }
        PropSounds.trim(loops, sounds, 0, 0, 0); // s[6], s[7] muted
        assertFalse(sounds.playing.contains(s[7]));
        PropSounds.trim(loops, sounds, 9, 0, 0); // standing at 9: s[7] (at 8) is now one of the nearest
        assertFalse(loops.containsValue(s[7]));
    }

    @Test
    void aLoopThatOnlyJustStartedIsNeverForgottenEarly()
    {
        ISound s = loop();
        loops.put(new ChunkCoordinates(3, 0, 0), s); // started, the sound system has not reported it playing yet
        PropSounds.trim(loops, sounds, 0, 0, 0);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        assertTrue(loops.containsValue(s));
        assertFalse(sounds.stopped.contains(s));
        sounds.playing.add(s); // now it plays
        PropSounds.trim(loops, sounds, 0, 0, 0);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        assertTrue(loops.containsValue(s));
    }

    @Test
    void aLoopSilentForThreePassesIsStoppedThenForgotten()
    {
        ISound s = loop();
        loops.put(new ChunkCoordinates(3, 0, 0), s); // its channel was taken by another sound
        PropSounds.trim(loops, sounds, 0, 0, 0);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        PropSounds.trim(loops, sounds, 0, 0, 0);
        assertFalse(loops.containsValue(s));
        assertTrue(sounds.stopped.contains(s)); // stopped first: never two loops of one prop
    }
}
