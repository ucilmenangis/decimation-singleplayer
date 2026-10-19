package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** NPC looks in one picture: each layer only where ModelHuman draws it, in its order, at its alpha test (user "7 to 1"). */
class HumanLayersTest
{
    static final int W = 64, H = 32, SKIN = 0xFFC08060, CLEAR = 0x00000000;

    static int[] picture(int argb, int w, int h)
    {
        int[] p = new int[w * h];
        Arrays.fill(p, argb);
        return p;
    }

    static int[] picture(int argb)
    {
        return picture(argb, W, H);
    }

    static int at(int[] p, int x, int y)
    {
        return p[y * W + x];
    }

    @Test
    void layersShowOnlyWhereTheirPartsAreDrawn()
    {
        int beard = 0xFF402010;
        int[][] layers = new int[6][];
        layers[HumanLayers.BEARD] = picture(beard);
        int[] out = HumanLayers.composite(picture(SKIN), layers, W, H);
        assertEquals(beard, at(out, 10, 10)); // head
        assertEquals(SKIN, at(out, 20, 20));  // body: the beard is drawn on the head only
        assertEquals(SKIN, at(out, 5, 20));   // legs
        assertEquals(SKIN, at(out, 40, 5));   // headwear place: no box of the passes uses it
    }

    @Test
    void laterLayersCoverEarlierOnes()
    {
        int hair = 0xFF201008, shirt = 0xFF3050A0;
        int[][] layers = new int[6][];
        layers[HumanLayers.HAIR] = picture(hair);
        layers[HumanLayers.SHIRT] = picture(CLEAR);
        layers[HumanLayers.SHIRT][20 * W + 20] = shirt;
        int[] out = HumanLayers.composite(picture(SKIN), layers, W, H);
        assertEquals(shirt, at(out, 20, 20)); // shirt drawn after the hair
        assertEquals(hair, at(out, 30, 20));  // hair on the body where the shirt is clear
        assertEquals(hair, at(out, 10, 10));  // hair on the head
        assertEquals(SKIN, at(out, 5, 20));   // neither is drawn on the legs
    }

    @Test
    void alphaTestAtOneTenth()
    {
        int[][] layers = new int[6][];
        layers[HumanLayers.SHIRT] = picture(CLEAR);
        layers[HumanLayers.SHIRT][20 * W + 20] = 0x19FFFFFF; // alpha 25: 0.098, discarded
        layers[HumanLayers.SHIRT][20 * W + 21] = 0x1AFFFFFF; // alpha 26: 0.102, drawn
        int[] out = HumanLayers.composite(picture(SKIN), layers, W, H);
        assertEquals(SKIN, at(out, 20, 20));
        assertEquals(0x1AFFFFFF, at(out, 21, 20));
    }

    @Test
    void clearBodyStaysClearWhereNoLayerIsDrawn()
    {
        int pants = 0xFF101010;
        int[][] layers = new int[6][];
        layers[HumanLayers.PANTS] = picture(CLEAR);
        layers[HumanLayers.PANTS][20 * W + 5] = pants;
        int[] out = HumanLayers.composite(picture(CLEAR), layers, W, H);
        assertEquals(pants, at(out, 5, 20));
        assertEquals(CLEAR, at(out, 5, 25));
    }

    @Test
    void largerPicturesScaleThePlaces()
    {
        int beard = 0xFF402010, w = 128, h = 64;
        int[][] layers = new int[6][];
        layers[HumanLayers.BEARD] = picture(beard, w, h);
        int[] out = HumanLayers.composite(picture(SKIN, w, h), layers, w, h);
        assertEquals(beard, out[30 * w + 60]);  // head: x below 64, y below 32
        assertEquals(SKIN, out[40 * w + 40]);   // body
    }

    @Test
    void lookPacksIntoOneNumber()
    {
        int[] look = {3, 24, 8, 6, 24, 10, 4, HumanLayers.BLINKING | HumanLayers.INFECTED};
        long a = HumanLayers.pack(1, look);
        assertTrue(a >= 0);
        look[1] = 23;
        assertNotEquals(a, HumanLayers.pack(1, look));
        assertNotEquals(HumanLayers.pack(0, look), HumanLayers.pack(1, look));
        look[7] = 0;
        assertNotEquals(HumanLayers.pack(1, look), a);
        look[1] = 256;
        assertEquals(-1, HumanLayers.pack(1, look));
        look[1] = -1;
        assertEquals(-1, HumanLayers.pack(1, look));
        look[1] = 2;
        assertEquals(-1, HumanLayers.pack(16, look));
    }
}
