package net.decimation.worldgen.devtest;

import java.util.List;
import java.util.Map;

import net.decimation.fixes.Deci;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Trading with a Decimation trader (docs/decimation_code/traders.md): a food trader in the arena, the player with 1000
 * caps. The client sends the trades the way the trader screen does (Deci.sendTrade: PacketTraderTransaction); the
 * server's handler checks the price against the trader's offers. Checks: a buy takes the price and gives the item, a
 * wrong price is refused, a sale gives the sell price and takes the item.
 */
public class TradeTest extends DevTestMode
{
    public String name() { return "trade"; }

    private volatile int ticks, step, traderId = -1, buy, sell;
    private volatile ItemStack offer;
    private volatile boolean ready, sent;
    private int wait, sentAt;
    private Entity trader;
    private long caps;
    private int count;

    public boolean client(Minecraft mc)
    {
        if (mc.thePlayer == null || !ready || sent)
        {
            return step < 4;
        }
        if (++wait < 20)
        {
            return true;
        }
        wait = 0;
        // the trader market (fixes/TraderMarket) moves prices after each trade: take the offer's price as it is now
        for (java.util.Map.Entry<ItemStack, int[]> o : Deci.traderOffers(mc.theWorld.getEntityByID(traderId)))
        {
            if (o.getKey().getItem() == offer.getItem() && o.getKey().getItemDamage() == offer.getItemDamage())
            {
                buy = o.getValue()[0];
                sell = o.getValue()[1];
            }
        }
        switch (step)
        {
            case 0: Deci.sendTrade(0, offer, buy, traderId, 0); break;         // buy at the right price
            case 1: Deci.sendTrade(0, offer, buy - 1, traderId, 0); break;     // a price the trader does not ask
            case 2: Deci.sendTrade(1, offer, sell, traderId, 0); break;        // sell one back
            default: break;
        }
        sent = step < 3;
        return step < 4;
    }

    public void server()
    {
        int t = ++ticks;
        EntityPlayerMP p = DevTestUtil.player();
        if (p == null)
        {
            return;
        }
        World w = p.worldObj;
        if (t == 2)
        {
            DevTestArena.build(w, p);
            w.setWorldTime(6000);
            p.playerNetServerHandler.setPlayerLocation(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z + 0.5, 180, 0);
            for (int i = 0; i < 36; i++)
            {
                p.inventory.setInventorySlotContents(i, null);
            }
            Deci.player(p).addBottlecaps(1000);
            trader = EntityList.createEntityByName("TraderFood", w);
            if (trader == null)
            {
                DevTestResults.check(name(), "food trader", "none", false, "EntityList TraderFood");
                step = 4;
                return;
            }
            trader.setLocationAndAngles(DevTestArena.X + 0.5, DevTestArena.Y, DevTestArena.Z - 2.5, 0, 0);
            DevTestResults.check(name(), "food trader spawns", "", w.spawnEntityInWorld(trader), "spawnEntityInWorld true");
            List<Map.Entry<ItemStack, int[]>> offers = Deci.traderOffers(trader);
            for (Map.Entry<ItemStack, int[]> o : offers)
            {
                if (o.getValue()[0] > 1 && o.getValue()[1] > 0 && o.getValue()[2] == 0)
                {
                    offer = o.getKey();
                    buy = o.getValue()[0];
                    sell = o.getValue()[1];
                    break;
                }
            }
            DevTestResults.value(name(), "offers", offers.size() + ", picked " + (offer == null ? "none"
                : offer.getDisplayName() + " buy " + buy + " sell " + sell));
            if (offer == null)
            {
                DevTestResults.check(name(), "an offer to buy and sell for caps", "none", false, "buy > 1, sell > 0, caps");
                step = 4;
                return;
            }
            traderId = trader.getEntityId();
            caps = Deci.player(p).bottlecaps();
            count = count(p, offer.getItem());
            ready = true;
        }
        if (sent && sentAt == 0)
        {
            sentAt = t;
        }
        if (sent && t - sentAt >= 10)
        {
            long nowCaps = Deci.player(p).bottlecaps();
            int nowCount = count(p, offer.getItem());
            if (step == 0)
            {
                DevTestResults.check(name(), "buy takes the price", caps + " -> " + nowCaps, nowCaps == caps - buy, "-" + buy);
                DevTestResults.check(name(), "buy gives the item", count + " -> " + nowCount, nowCount == count + 1, "+1");
            }
            else if (step == 1)
            {
                DevTestResults.check(name(), "a wrong price is refused", caps + " -> " + nowCaps + ", items " + count + " -> " + nowCount,
                                     nowCaps == caps && nowCount == count, "nothing changes");
            }
            else if (step == 2)
            {
                DevTestResults.check(name(), "sale gives the sell price", caps + " -> " + nowCaps, nowCaps == caps + sell, "+" + sell);
                DevTestResults.check(name(), "sale takes the item", count + " -> " + nowCount, nowCount == count - 1, "-1");
                trader.setDead();
            }
            caps = nowCaps;
            count = nowCount;
            step++;
            sent = false;
            sentAt = 0;
            if (step == 3)
            {
                step = 4;
            }
        }
    }

    private static int count(EntityPlayerMP p, Item item)
    {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory)
        {
            n += s != null && s.getItem() == item ? s.stackSize : 0;
        }
        return n;
    }
}
