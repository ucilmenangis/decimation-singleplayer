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
        int[] p = TraderMarket.prices(10, 2, 1.0, 1.0, 1.0, false, false);
        assertEquals(10, p[0]);
        assertEquals(2, p[1]);
        p = TraderMarket.prices(1, 1, 0.85, 0.6, 0.6, false, false);
        assertEquals(1, p[0], "never below 1");
        p = TraderMarket.prices(10, 0, 1.0, 1.0, 1.0, false, false);
        assertEquals(0, p[1], "an offer the trader does not buy stays so");
        p = TraderMarket.prices(100, 25, 1.0, 1.0, 1.0, true, false);
        assertEquals(70, p[0], "on sale: buy at 70 %");
        p = TraderMarket.prices(100, 25, 1.0, 1.0, 1.0, false, true);
        assertEquals(38, p[1], "in demand: sells to it for 150 %");
        p = TraderMarket.prices(10, 9, 1.0, 1.0, 1.0, false, true);
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

    @Test
    void aTradeCountsOnlyWhenTheCapsMovedByExactlyItsPrice()
    {
        assertTrue(TraderMarket.traded(0, 100, 91, 5, 5, 9, 0), "a buy at 9 caps");
        assertTrue(TraderMarket.traded(1, 100, 102, 5, 5, 2, 0), "a sale at 2 caps");
        assertTrue(TraderMarket.traded(0, 100, 100, 5, 3, 2, 1), "a buy for 2 gold caps");
        assertTrue(!TraderMarket.traded(0, 100, 100, 5, 5, 9, 0), "a refused buy changed nothing");
        assertTrue(!TraderMarket.traded(0, 100, 95, 5, 5, 9, 0), "caps moved by something else");
        assertTrue(!TraderMarket.traded(1, 100, 91, 5, 5, 9, 0), "a sale never takes caps");
        assertTrue(!TraderMarket.traded(2, 100, 91, 5, 5, 9, 0), "other actions are not trades");
    }

    @Test
    void noTraderBuysAnItemForMoreThanAnotherSellsItThatDay()
    {
        java.util.Map<String, int[][]> rows = new java.util.HashMap<String, int[][]>();
        java.util.Map<String, String[]> keys = new java.util.HashMap<String, String[]>();
        rows.put("A", new int[][] {{7, 2, 1}});          // water on sale at A: buy 7
        keys.put("A", new String[] {"water"});
        rows.put("B", new int[][] {{12, 13, 2}, {5, 1, 0}});   // water wanted at B: would pay 13
        keys.put("B", new String[] {"water", "bread"});
        TraderMarket.noArbitrage(rows, keys);
        assertEquals(6, rows.get("B")[0][1], "B pays under A's lowest price");
        assertEquals(2, rows.get("A")[0][1], "A's own sale price was already under");
        assertEquals(1, rows.get("B")[1][1], "another item untouched");
    }

    @Test
    void buyingRaisesOnlyTheBuyPriceAndSellingLowersOnlyTheSellPrice()
    {
        int[] calm = TraderMarket.prices(100, 90, 1.0, 1.0, 1.0, false, false);
        int[] bought = TraderMarket.prices(100, 90, 1.0, TraderMarket.factor(20), 1.0, false, false);
        assertEquals(160, bought[0], "twenty buys: the buy price up 60 %");
        assertEquals(calm[1], bought[1], "the sell price does not follow: buying then selling back never pays");
        int[] sold = TraderMarket.prices(100, 90, 1.0, 1.0, TraderMarket.factor(-10), false, false);
        assertEquals(calm[0], sold[0], "ten sales leave the buy price");
        assertEquals(63, sold[1], "and lower the sell price 30 %");
    }

    @Test
    void aTradeAtAnythingButTodaysPriceIsDroppedBeforeDecimationSeesIt()
    {
        java.util.Map<String, int[][]> rows = new java.util.HashMap<String, int[][]>();
        java.util.Map<String, String[]> keys = new java.util.HashMap<String, String[]>();
        rows.put("A", new int[][] {{12, 3, 0}});
        keys.put("A", new String[] {"0/water"});
        java.util.Map<String, Set<String>> valid = TraderMarket.validPrices(rows, keys);
        assertTrue(TraderMarket.allowed(valid, "water", 0, 12, 0), "today's buy price");
        assertTrue(TraderMarket.allowed(valid, "water", 1, 3, 0), "today's sell price");
        assertTrue(!TraderMarket.allowed(valid, "water", 0, 10, 0), "the base price of a list reset a moment ago");
        assertTrue(!TraderMarket.allowed(valid, "water", 0, 12, 1), "gold for a caps offer");
        assertTrue(TraderMarket.allowed(valid, "bread", 0, 5, 0), "an item not in today's table: Decimation's own check");
    }
}
