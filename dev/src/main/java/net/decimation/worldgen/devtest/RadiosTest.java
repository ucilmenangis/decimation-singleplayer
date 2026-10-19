package net.decimation.worldgen.devtest;

import net.decimation.fixes.Deci;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/**
 * bug.md "FPS falls minute by minute": every radio prop (BlockRadio1, 56 in the brother's small world, placed by our city
 * dressing) starts its own looping streaming sound when the player comes within 16 blocks, and Decimation stops it only
 * when the radio's chunk unloads, so radios passed in a city keep streaming (decoded in Java, locked against the game's
 * sound updates every tick). 30 radios around the player: the frame rate without them, with them playing, and after
 * walking 80 blocks away (chunks still loaded: they must stop), then back (they must play again).
 */
public class RadiosTest extends DevTestMode
{
    public String name() { return "radios"; }

    private static final int COUNT = 30, AWAY = 80;
    private volatile int phase; // 0 baseline, 1 radios placed, 2 moved away, 3 moved back, 4 cleared
    private int ticks, fpsSum, fpsN;
    private boolean placed;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || !placed)
        {
            return true;
        }
        ticks++;
        int fps = SoakTest.fpsNow();
        if (ticks % 20 == 0 && ticks > 0)
        {
            fpsSum += fps;
            fpsN++;
        }
        if (phase == 0 && ticks == 120)
        {
            DevTestResults.value(name(), "fps without radios", avg());
            phase = 1; // the server places them
        }
        else if (phase == 1 && ticks == 120 + 600)
        {
            int playing = Deci.playingPropSounds();
            DevTestResults.value(name(), "radio loops playing near 30 radios", playing + " of " + Deci.propSoundEntries() + " entries");
            DevTestResults.value(name(), "fps with the radios near", avg());
            phase = 2; // the server moves the player away
        }
        else if (phase == 2 && ticks == 120 + 600 + 200)
        {
            int playing = Deci.playingPropSounds();
            DevTestResults.check(name(), "radio loops playing " + AWAY + " blocks away", String.valueOf(playing), playing <= 2,
                                 "2 or fewer (out of hearing)");
            DevTestResults.value(name(), "fps away from the radios", avg());
            phase = 3; // the server moves the player back
        }
        else if (phase == 3 && ticks == 120 + 600 + 200 + 600)
        {
            int playing = Deci.playingPropSounds();
            DevTestResults.check(name(), "radio loops playing again after coming back", String.valueOf(playing), playing > 0,
                                 "more than 0");
            phase = 4; // the server removes the radios
        }
        else if (phase == 5)
        {
            return false;
        }
        return true;
    }

    private int avg()
    {
        int a = fpsN == 0 ? -1 : fpsSum / fpsN;
        fpsSum = 0;
        fpsN = 0;
        return a;
    }

    @Override
    public void server()
    {
        WorldServer w = MinecraftServer.getServer().worldServerForDimension(0);
        EntityPlayerMP p = w.playerEntities.isEmpty() ? null : (EntityPlayerMP) w.playerEntities.get(0);
        if (p == null)
        {
            return;
        }
        int x0 = DevTestArena.X, z0 = DevTestArena.Z, y = (int) DevTestArena.Y;
        if (!placed)
        {
            DevTestArena.build(w, p);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.setPositionAndUpdate(x0 + 0.5, y, z0 + 0.5);
            placed = true;
        }
        Block radio = Block.getBlockFromName("deci:BlockRadio1");
        if (phase == 1 && radio != null && w.getBlock(x0 - 7, y, z0 - 6) != radio)
        {
            for (int i = 0; i < COUNT; i++)
            {
                int gx = x0 - 7 + (i % 6) * 3, gz = z0 - 6 + (i / 6) * 3;
                w.setBlock(gx, y, gz, radio, 2, 3);
            }
        }
        else if (phase == 2 && p.posX < x0 + AWAY - 1)
        {
            p.setPositionAndUpdate(x0 + AWAY + 0.5, y, z0 + 0.5);
        }
        else if (phase == 3 && p.posX > x0 + 1)
        {
            p.setPositionAndUpdate(x0 + 0.5, y, z0 + 0.5);
        }
        else if (phase == 4)
        {
            for (int i = 0; i < COUNT; i++)
            {
                w.setBlockToAir(x0 - 7 + (i % 6) * 3, y, z0 - 6 + (i / 6) * 3);
            }
            phase = 5;
        }
    }
}
