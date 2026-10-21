package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Looted magazines: 1 round to full, weighted toward half full, rarely full, never empty (user, 22 Oktober 2026). */
class LootMagazinesTest
{
    @Test
    void mostlyPartialNeverEmpty()
    {
        Random r = new Random(7);
        for (int cap : new int[] {5, 10, 20, 30, 60, 100})
        {
            int full = 0, n = 4000;
            double sum = 0;
            for (int i = 0; i < n; i++)
            {
                int k = LootMagazines.rounds(cap, r);
                assertTrue(k >= 1 && k <= cap, cap + ": " + k);
                full += k == cap ? 1 : 0;
                sum += k / (double) cap;
            }
            assertTrue(full < n / 10, cap + ": " + full + " full of " + n);     // rarely full
            assertTrue(Math.abs(sum / n - 0.5) < 0.08, cap + ": mean " + sum / n); // around half
        }
    }

    @Test
    void aOneRoundMagazineStaysOne()
    {
        assertTrue(LootMagazines.rounds(1, new Random()) == 1);
    }
}
