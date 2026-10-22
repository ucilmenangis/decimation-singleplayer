package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Trader market by day (feature 11, user 22 Oktober 2026: "trader now have dynamic market supply and demand, its randomize
 * by day in minecraft"; chosen: daily drift plus player trades, both easing back over days, a few items on sale and in
 * demand per trader and day).
 */
class TraderMarketTest
{
    @Test
    void driftStaysWithinFifteenPercentAndIsTheSameForADay()
    {
        for (int day = 1; day < 200; day++)
        {
            double d = TraderMarket.drift(42L, day, "deci:akm");
            assertTrue(d >= 0.85 && d <= 1.15, "day " + day + ": " + d);
            assertEquals(d, TraderMarket.drift(42L, day, "deci:akm"), "same seed, day and item: same drift");
        }
        assertNotEquals(TraderMarket.drift(42L, 1, "deci:akm"), TraderMarket.drift(42L, 2, "deci:akm"), "a new day moves it");
    }

    @Test
    void tradesPushPricesAndTheyEaseBack()
    {
        assertEquals(1.0, TraderMarket.factor(0), 1e-9);
        assertEquals(1.3, TraderMarket.factor(10), 1e-9, "ten buys: +30 %");
        assertEquals(0.7, TraderMarket.factor(-10), 1e-9, "ten sales: -30 %");
        assertEquals(1.6, TraderMarket.factor(100), 1e-9, "clamped");
        assertEquals(0.6, TraderMarket.factor(-100), 1e-9, "clamped");
        assertEquals(7.0, TraderMarket.ease(10), 1e-9, "30 % back toward nothing a day");
    }

    @Test
    void pricesAreWholeCapsNeverBelowOneAndSellNeverAboveBuy()
    {
        int[] p = TraderMarket.prices(10, 2, 1.0, 1.0, false, false);
        assertEquals(10, p[0]);
        assertEquals(2, p[1]);
        p = TraderMarket.prices(1, 1, 0.85, 0.6, false, false);
        assertEquals(1, p[0], "never below 1");
        p = TraderMarket.prices(10, 0, 1.0, 1.0, false, false);
        assertEquals(0, p[1], "an offer the trader does not buy stays so");
        p = TraderMarket.prices(100, 25, 1.0, 1.0, true, false);
        assertEquals(70, p[0], "on sale: buy at 70 %");
        p = TraderMarket.prices(100, 25, 1.0, 1.0, false, true);
        assertEquals(38, p[1], "in demand: sells to it for 150 %");
        p = TraderMarket.prices(10, 9, 1.0, 1.0, false, true);
        assertTrue(p[1] < p[0], "a sale price stays under the buy price: " + p[1] + " / " + p[0]);
    }

    @Test
    void threeOnSaleAndThreeInDemandEachDayDifferentItems()
    {
        Set<Integer> sale = TraderMarket.picks(42L, 5, "TraderFood", 29, 0), demand = TraderMarket.picks(42L, 5, "TraderFood", 29, 1);
        assertEquals(3, sale.size());
        assertEquals(3, demand.size());
        Set<Integer> both = new HashSet<Integer>(sale);
        both.retainAll(demand);
        assertTrue(both.isEmpty(), "an item is not on sale and in demand at once");
        assertEquals(sale, TraderMarket.picks(42L, 5, "TraderFood", 29, 0), "the same for the whole day");
        assertNotEquals(sale, TraderMarket.picks(42L, 6, "TraderFood", 29, 0), "another day, other items");
        assertEquals(2, TraderMarket.picks(42L, 5, "TraderTiny", 4, 0).size(), "a small list: half on sale at most");
    }
}
