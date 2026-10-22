package net.decimation.worldgen.devtest;

import java.util.List;
import java.util.Map;

import net.decimation.fixes.Deci;
import net.decimation.fixes.TraderMarket;
import net.decimation.fixes.WorldClock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Trader market by day, feature 11 (fixes/TraderMarket): a food trader's prices are the day's (base x drift within 15 %,
 * three on sale at 70 %, three wanted at 150 %), the same on the client; ten buys of one item raise its price, five sales
 * of another lower its, a trade at an old price is refused; the next day the pressure eases by 30 % and the prices move.
 */
public class MarketTest extends DevTestMode
{
    public String name() { return "market"; }

    private int step, wait, traderId = -1, i;
    private volatile Runnable onServer;
    private volatile boolean served;
    private ItemStack up, down;
    private volatile int upBefore, downBefore, oldPrice;
    private volatile double upPressure;
    private volatile Double downPressure;
    private long capsBefore;

    public boolean client(Minecraft mc)
    {
        net.decimation.worldgen.DevAutoTest.keepScreen = true;
        wait++;
        Entity t = traderId < 0 ? null : mc.theWorld.getEntityByID(traderId);
        switch (step)
        {
            case 0:
                if (wait == 1)
                {
                    serve(this::setup);
                }
                if (wait > 40 && served && t != null)
                {
                    serve(this::dayPrices);
                    next(1);
                }
                break;
            case 1: // the client sees the server's prices; pick an item to push up and one to push down
                if (wait > 2 && served)
                {
                    List<Map.Entry<ItemStack, int[]>> client = Deci.traderOffers(t);
                    int[][] rows = TraderMarket.rowsOf(t.getClass());
                    int same = 0;
                    for (int k = 0; rows != null && k < client.size() && k < rows.length; k++)
                    {
                        same += client.get(k).getValue()[0] == rows[k][0] && client.get(k).getValue()[1] == rows[k][1] ? 1 : 0;
                    }
                    DevTestResults.check(name(), "the client's offers are the day's prices", same + " of " + client.size(),
                                         rows != null && same == client.size(), "all");
                    for (int k = 0; k < client.size(); k++)
                    {
                        int[] p = client.get(k).getValue();
                        if (p[2] == 0 && p[0] > 1 && p[1] > 0 && up == null && rows[k][2] == 0)
                        {
                            up = client.get(k).getKey();
                            upBefore = p[0];
                        }
                        else if (p[2] == 0 && p[1] > downBefore && rows[k][2] == 0 && up != null)
                        {
                            down = client.get(k).getKey();   // the best paid sale: rounding shows a change
                            downBefore = p[1];
                        }
                    }
                    final ItemStack give = down;
                    serve(() -> DevTestUtil.player().inventory.addItemStackToInventory(new ItemStack(give.getItem(), 5, give.getItemDamage())));
                    next(2);
                }
                break;
            case 2: // ten buys of one item, then five sales of another, each at the price of the moment
                if (wait % 12 == 0 && i < 15)
                {
                    ItemStack s = i < 10 ? up : down;
                    int[] p = price(t, s);
                    if (i == 0)
                    {
                        oldPrice = p[0];
                    }
                    Deci.sendTrade(i < 10 ? 0 : 1, s, i < 10 ? p[0] : p[1], traderId, 0);
                    i++;
                }
                if (i == 10 && wait % 12 == 6 && downPressure == null)
                {
                    serve(() -> downPressure = TraderMarket.pressureOf(down));
                }
                if (i >= 15 && wait % 12 == 6)
                {
                    serve(this::afterTrades);
                    next(3);
                }
                break;
            case 3: // an old price is refused now
                if (wait == 5 && served)
                {
                    serve(() -> capsBefore = Deci.player(DevTestUtil.player()).bottlecaps());
                }
                if (wait == 10)
                {
                    Deci.sendTrade(0, up, oldPrice, traderId, 0);
                }
                if (wait == 25)
                {
                    serve(() -> {
                        long now = Deci.player(DevTestUtil.player()).bottlecaps();
                        DevTestResults.check(name(), "a buy at the first price is refused once it rose", capsBefore + " -> " + now,
                                             now == capsBefore, "no caps taken");
                    });
                    next(4);
                }
                break;
            case 4: // the next day
                if (wait == 3 && served)
                {
                    serve(() -> {
                        upPressure = TraderMarket.pressureOf(up);
                        World w = DevTestUtil.player().worldObj;
                        WorldClock.setDay(w, WorldClock.day(w) + 1);
                    });
                }
                if (wait == 15)
                {
                    serve(() -> {
                        double p = TraderMarket.pressureOf(up);
                        DevTestResults.check(name(), "next day: the pressure eases 30 %", String.format("%.2f -> %.2f", upPressure, p),
                                             Math.abs(p - upPressure * 0.7) < 0.01, "x 0.7");
                    });
                    next(5);
                }
                break;
            default:
                if (wait == 3 && served)
                {
                    serve(this::cleanup);
                }
                if (wait > 3 && served && wait > 6)
                {
                    net.decimation.worldgen.DevAutoTest.keepScreen = false;
                    return false;
                }
        }
        return true;
    }

    private static int[] price(Entity t, ItemStack s)
    {
        for (Map.Entry<ItemStack, int[]> o : Deci.traderOffers(t))
        {
            if (o.getKey().getItem() == s.getItem() && o.getKey().getItemDamage() == s.getItemDamage())
            {
                return o.getValue();
            }
        }
        return new int[] {0, 0, 0};
    }

    private void next(int s)
    {
        step = s;
        wait = 0;
    }

    private void serve(Runnable r)
    {
        served = false;
        onServer = r;
    }

    public void server()
    {
        Runnable r = onServer;
        if (r == null || DevTestUtil.player() == null)
        {
            return;
        }
        onServer = null;
        try
        {
            r.run();
        }
        catch (RuntimeException e)
        {
            DevTestResults.check(name(), "server step", e.toString(), false, "no exception");
        }
        served = true;
    }

    private void setup()
    {
        EntityPlayerMP p = DevTestUtil.player();
        World w = p.worldObj;
        DevTestArena.build(w, p);
        w.setWorldTime(6000);
        p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 180, 0);
        for (int k = 0; k < 36; k++)
        {
            p.inventory.setInventorySlotContents(k, null);
        }
        Deci.player(p).addBottlecaps(5000);
        Entity t = EntityList.createEntityByName("TraderFood", w);
        t.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 2.5, 0, 0);
        DevTestResults.check(name(), "food trader spawns", "", w.spawnEntityInWorld(t), "spawned");
        traderId = t.getEntityId();
    }

    /** The server's live offers against the base: drift within 15 % (on sale and wanted aside), 3 on sale, 3 wanted. */
    private void dayPrices()
    {
        Entity t = DevTestUtil.player().worldObj.getEntityByID(traderId);
        int[][] rows = TraderMarket.rowsOf(t.getClass()), base = TraderMarket.baseOf(t.getClass());
        List<Map.Entry<ItemStack, int[]>> live = Deci.traderOffers(t);
        int ok = 0, sale = 0, wanted = 0, n = rows == null ? 0 : rows.length;
        for (int k = 0; k < n; k++)
        {
            sale += rows[k][2] == 1 ? 1 : 0;
            wanted += rows[k][2] == 2 ? 1 : 0;
            double f = rows[k][2] == 1 ? 0.7 : 1;
            boolean inRange = rows[k][0] >= Math.max(1, Math.floor(base[k][0] * 0.85 * f * 0.6) - 1)   // pressure may sit from earlier runs
                && rows[k][0] <= Math.ceil(base[k][0] * 1.15 * f * 1.6) + 1;
            ok += inRange && live.get(k).getValue()[0] == rows[k][0] ? 1 : 0;
        }
        DevTestResults.check(name(), "today's prices in the trader's offers", ok + " of " + n, n > 0 && ok == n, "all, within the day's range");
        DevTestResults.check(name(), "on sale and wanted today", sale + " and " + wanted, sale == 3 && wanted == 3, "3 and 3");
    }

    private void afterTrades()
    {
        Entity t = DevTestUtil.player().worldObj.getEntityByID(traderId);
        int[] u = price(t, up), d = price(t, down), c = TraderMarket.counts();
        DevTestResults.check(name(), "ten buys raise that item's price", upBefore + " -> " + u[0] + " (pressure " + TraderMarket.pressureOf(up) + ")",
                             u[0] > upBefore, "higher");
        double dp = TraderMarket.pressureOf(down);
        DevTestResults.check(name(), "five sales push the other's pressure down and its price with it",
                             downBefore + " -> " + d[1] + ", pressure " + downPressure + " -> " + dp,
                             downPressure != null && Math.abs(dp - (downPressure - 5)) < 1e-6 && d[1] <= downBefore, "pressure -5, price not higher");
        DevTestResults.value(name(), "trades seen, counted, tables sent", c[0] + ", " + c[1] + ", " + c[2]);
        long[] cost = TraderMarket.cost(51);
        DevTestResults.check(name(), "a refresh (every trader kind) and the table sent", String.format("%.2f ms, %d bytes", cost[0] / 1e6, cost[1]),
                             cost[0] < 5_000_000 && cost[1] < 30000, "under 5 ms (at dawn and per trade only), under 30 KB");
    }

    private void cleanup()
    {
        Entity t = DevTestUtil.player().worldObj.getEntityByID(traderId);
        if (t != null)
        {
            t.setDead();
        }
    }
}
