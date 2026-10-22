package net.decimation.fixes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Trader market by day (feature 11, docs/superpowers/specs/2026-10-22-trader-market-design.md; user, 22 Oktober 2026:
 * "trader now have dynamic market supply and demand, its randomize by day in minecraft"; chosen: daily drift plus player
 * trades, both easing back over days, a few items on sale and in demand per trader and day).
 *
 * Decimation keeps one offer list per trader class (TraderEntity.OFFERS_BY_CLASS) and checks a trade's price against it;
 * a new trader object puts a fresh list of base prices in. The market keeps those base prices and writes the day's offers
 * into the lists: at dawn, after each trade, whenever a trader joins a world (its list was just reset), and on clients from
 * the price table the server sends (Net message 5). A day's price: base x drift (0.85 .. 1.15, by world seed, day and
 * item) x pressure (1 + 0.03 a unit, 0.6 .. 1.6; a buy +1, a sale -1, 30 % back toward 0 a day); each trader class has
 * up to 3 items on sale (buy 70 %) and 3 in demand (sell 150 %) a day. Trades are seen in Decimation's own channel in
 * front of its handlers and counted only when the player's caps changed (the trade went through). Saved per world.
 */
public final class TraderMarket
{
    // ---- the rules

    static double drift(long seed, int day, String key)
    {
        long h = seed * 0x9E3779B97F4A7C15L ^ (long) day * 0xC2B2AE3D27D4EB4FL ^ key.hashCode() * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        double u = (h >>> 11) / (double) (1L << 53);
        return 0.85 + 0.30 * u;
    }

    static double factor(double pressure)
    {
        return Math.max(0.6, Math.min(1.6, 1 + 0.03 * pressure));
    }

    static double ease(double pressure)
    {
        return pressure * 0.7;
    }

    /** The day's {buy, sell} of an offer: whole caps, buy at least 1, sell 0 stays 0 and stays under the buy price. */
    static int[] prices(int buy, int sell, double drift, double factor, boolean sale, boolean demand)
    {
        int b = Math.max(1, (int) Math.round(buy * drift * factor * (sale ? 0.7 : 1)));
        int s = sell <= 0 ? 0 : Math.max(1, (int) Math.round(sell * drift * factor * (demand ? 1.5 : 1)));
        if (s >= b)
        {
            s = Math.max(0, b - 1);
        }
        return new int[] {b, s};
    }

    /** Offer indices of a trader kind on sale (which 0) or in demand (which 1) on a day: up to 3 each, half the list at most. */
    static Set<Integer> picks(long seed, int day, String kind, int offers, int which)
    {
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < offers; i++)
        {
            order.add(i);
        }
        Collections.shuffle(order, new Random(seed * 31 + day * 0x632BE5ABL + kind.hashCode()));
        int n = Math.min(3, offers / 2);
        return new HashSet<Integer>(order.subList(which * n, which * n + n));
    }

    /** A trade went through: the caps of its currency moved by exactly its price, the right way (buy down, sell up). */
    static boolean traded(int action, long capsBefore, long capsAfter, long goldBefore, long goldAfter, int price, int currency)
    {
        long before = currency == 1 ? goldBefore : capsBefore, after = currency == 1 ? goldAfter : capsAfter;
        long other = currency == 1 ? capsAfter - capsBefore : goldAfter - goldBefore;
        return price > 0 && other == 0 && (action == 0 && before - after == price || action == 1 && after - before == price);
    }

    /** No trader buys an item for as much as any trader sells it that day (no buying here and selling there for profit). */
    static void noArbitrage(Map<String, int[][]> rows, Map<String, String[]> keys)
    {
        Map<String, Integer> lowest = new HashMap<String, Integer>();
        for (Map.Entry<String, int[][]> e : rows.entrySet())
        {
            String[] k = keys.get(e.getKey());
            for (int i = 0; k != null && i < e.getValue().length; i++)
            {
                Integer l = lowest.get(k[i]);
                lowest.put(k[i], l == null ? e.getValue()[i][0] : Math.min(l, e.getValue()[i][0]));
            }
        }
        for (Map.Entry<String, int[][]> e : rows.entrySet())
        {
            String[] k = keys.get(e.getKey());
            for (int i = 0; k != null && i < e.getValue().length; i++)
            {
                int cap = Math.max(0, lowest.get(k[i]) - 1);
                if (e.getValue()[i][1] > cap)
                {
                    e.getValue()[i][1] = cap;
                }
            }
        }
    }

    static String key(ItemStack s)
    {
        Object name = s == null || s.getItem() == null ? null : Item.itemRegistry.getNameForObject(s.getItem());
        return name + ":" + (s == null ? 0 : s.getItemDamage());
    }

    // ---- saved pressures

    public static class Data extends WorldSavedData
    {
        public static final String NAME = "deciworldgen_market";
        final Map<String, Double> pressure = new HashMap<String, Double>();
        int day;

        public Data(String name)
        {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag)
        {
            synchronized (LOCK)
            {
                pressure.clear();
                day = tag.getInteger("day");
                NBTTagCompound p = tag.getCompoundTag("pressure");
                for (Object o : p.func_150296_c())
                {
                    pressure.put((String) o, p.getDouble((String) o));
                }
            }
        }

        /** Under the same lock as a trade's update (trades are counted on the network thread). */
        @Override
        public void writeToNBT(NBTTagCompound tag)
        {
            synchronized (LOCK)
            {
                tag.setInteger("day", day);
                NBTTagCompound p = new NBTTagCompound();
                for (Map.Entry<String, Double> e : pressure.entrySet())
                {
                    p.setDouble(e.getKey(), e.getValue());
                }
                tag.setTag("pressure", p);
            }
        }
    }

    private static Data data(World w)
    {
        Data d = (Data) w.mapStorage.loadData(Data.class, Data.NAME);
        if (d == null)
        {
            d = new Data(Data.NAME);
            w.mapStorage.setData(Data.NAME, d);
        }
        return d;
    }

    // ---- the lists

    /** Base offers per trader class (captured from a fresh list), and the offer objects we wrote (to tell them apart). */
    private static final Map<Class<?>, List<Object>> BASE = new HashMap<Class<?>, List<Object>>();
    private static final Set<Object> OURS = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    /** The day's prices per trader class name: {buy, sell, tag (0, 1 on sale, 2 in demand)} per base offer, in order. */
    private static volatile Map<String, int[][]> table = new HashMap<String, int[][]>();

    /** Makes sure a class's base is known: a list holding an offer we did not write is a fresh base list. */
    private static synchronized List<Object> base(Class<?> c, List<Object> live)
    {
        for (Object o : live)
        {
            if (!OURS.contains(o))
            {
                BASE.put(c, new ArrayList<Object>(live));
                return BASE.get(c);
            }
        }
        return BASE.get(c);
    }

    /** The server's prices of every known trader class for the day (a new table). */
    private static Map<String, int[][]> compute(long seed, int day, Data d)
    {
        Map<String, int[][]> t = new HashMap<String, int[][]>();
        Map<String, String[]> keys = new HashMap<String, String[]>();
        for (Map.Entry<Class<?>, List<Object>> e : Deci.traderOfferLists().entrySet())
        {
            List<Object> b = base(e.getKey(), e.getValue());
            if (b == null)
            {
                continue;
            }
            String kind = e.getKey().getName();
            Set<Integer> sale = picks(seed, day, kind, b.size(), 0), demand = picks(seed, day, kind, b.size(), 1);
            int[][] rows = new int[b.size()][];
            String[] ks = new String[b.size()];
            for (int i = 0; i < b.size(); i++)
            {
                int[] p = Deci.offerPrices(b.get(i));
                String k = key(Deci.offerStack(b.get(i)));
                ks[i] = p[2] + "/" + k;                    // per currency: caps and gold prices never compare
                Double pr;
                synchronized (LOCK)
                {
                    pr = d.pressure.get(k);
                }
                int[] np = prices(p[0], p[1], drift(seed, day, k), factor(pr == null ? 0 : pr), sale.contains(i), demand.contains(i));
                rows[i] = new int[] {np[0], np[1], sale.contains(i) ? 1 : demand.contains(i) ? 2 : 0};
            }
            t.put(kind, rows);
            keys.put(kind, ks);
        }
        noArbitrage(t, keys);
        return t;
    }

    /** Writes the table's prices into the live list of a class (both sides; a fresh list's base is taken first). */
    private static synchronized void apply(Class<?> c, Map<String, int[][]> t)
    {
        List<Object> live = Deci.traderOfferLists().get(c);
        int[][] rows = t.get(c.getName());
        List<Object> b = live == null ? null : base(c, live);
        if (b == null || rows == null || rows.length != b.size())
        {
            return;
        }
        live.clear();
        for (int i = 0; i < b.size(); i++)
        {
            int[] p = Deci.offerPrices(b.get(i));
            Object o = Deci.newTraderOffer(Deci.offerStack(b.get(i)), rows[i][0], rows[i][1], p[2]);
            OURS.add(o);
            live.add(o);
        }
    }

    private static void applyAll(Map<String, int[][]> t)
    {
        for (Class<?> c : new ArrayList<Class<?>>(Deci.traderOfferLists().keySet()))
        {
            apply(c, t);
        }
    }

    /** Today's tag of a stack offered by a trader of this class: 1 on sale, 2 in demand, else 0 (the client tooltip). */
    static int tag(Class<?> c, ItemStack s)
    {
        int[][] rows = table.get(c.getName());
        List<Object> b = BASE.get(c);
        for (int i = 0; rows != null && b != null && i < b.size() && i < rows.length; i++)
        {
            ItemStack o = Deci.offerStack(b.get(i));
            if (o != null && s != null && o.getItem() == s.getItem() && o.getItemDamage() == s.getItemDamage())
            {
                return rows[i][2];
            }
        }
        return 0;
    }

    /** For the dev test: the day's {buy, sell, tag} rows of a trader class, its base {buy, sell} rows, a stack's pressure. */
    public static int[][] rowsOf(Class<?> c)
    {
        return table.get(c.getName());
    }

    public static int[][] baseOf(Class<?> c)
    {
        List<Object> b = BASE.get(c);
        if (b == null)
        {
            return null;
        }
        int[][] r = new int[b.size()][];
        for (int i = 0; i < r.length; i++)
        {
            r[i] = Deci.offerPrices(b.get(i));
        }
        return r;
    }

    public static double pressureOf(ItemStack s)
    {
        World w = overworld();
        Double p;
        synchronized (LOCK)
        {
            p = w == null ? null : data(w).pressure.get(key(s));
        }
        return p == null ? 0 : p;
    }

    /** For the dev test: {nanoseconds of one refresh without sending (median of n), the table's bytes}. */
    public static long[] cost(int n)
    {
        long[] t = new long[n];
        for (int i = 0; i < n; i++)
        {
            long t0 = System.nanoTime();
            refresh(false);
            t[i] = System.nanoTime() - t0;
        }
        java.util.Arrays.sort(t);
        io.netty.buffer.ByteBuf b = io.netty.buffer.Unpooled.buffer();
        new Table(table).toBytes(b);
        return new long[] {t[n / 2], b.readableBytes()};
    }

    /** For the dev test: {trades seen, trades counted, tables sent}. */
    public static int[] counts()
    {
        return new int[] {seen, counted, sent};
    }

    // ---- the server

    private int lastDay = -1;
    private static boolean listening;
    /** Counts for the dev test: trades seen, trades counted, tables sent. */
    static volatile int seen, counted, sent;

    private static World overworld()
    {
        MinecraftServer s = MinecraftServer.getServer();
        return s == null ? null : s.worldServerForDimension(0);
    }

    /** Recomputes the day's table from the saved pressures, writes it into the lists and sends it to every player. */
    public static void refresh(boolean send)
    {
        World w = overworld();
        if (w == null)
        {
            return;
        }
        Map<String, int[][]> t = compute(w.getSeed(), WorldClock.day(w), data(w));
        table = t;
        applyAll(t);
        if (send)
        {
            Net.CHANNEL.sendToAll(new Table(t));
            sent++;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e)
    {
        World w = overworld();
        if (e.phase != TickEvent.Phase.END || w == null)
        {
            return;
        }
        if (!listening)
        {
            listen();
        }
        int day = WorldClock.day(w);
        if (day != lastDay)
        {
            Data d = data(w);
            synchronized (LOCK)
            {
                for (int k = d.day; k > 0 && k < day; k++)            // each day passed eases the pressures
                {
                    for (Map.Entry<String, Double> p : d.pressure.entrySet())
                    {
                        p.setValue(ease(p.getValue()));
                    }
                }
            }
            d.day = day;
            d.markDirty();
            lastDay = day;
            refresh(true);
        }
        if (tradesChanged)
        {
            tradesChanged = false;
            refresh(true);
        }
        for (Class<?> c : new ArrayList<Class<?>>(Deci.traderOfferLists().keySet()))
        {
            ensure(c);                                       // a list reset without a join we saw (a cancelled one) goes back
        }
    }

    /** A trader joining a world put a fresh list of base prices in: the day's prices go back in (both sides). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJoin(EntityJoinWorldEvent e)
    {
        if (!Deci.isTrader(e.entity))
        {
            return;                                          // a cancelled join too: its constructor reset the list
        }
        if (!e.world.isRemote && !table.containsKey(e.entity.getClass().getName()) && lastDay >= 0)
        {
            refresh(true);                                   // the first trader of its kind this game: its prices join the table
            return;
        }
        apply(e.entity.getClass(), table);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e)
    {
        if (e.player instanceof EntityPlayerMP && !table.isEmpty())
        {
            Net.CHANNEL.sendTo(new Table(table), (EntityPlayerMP) e.player);
        }
    }

    /** Our handler in Decimation's server channel, right after its codec: every trade message passes it first. */
    private static synchronized void listen()
    {
        if (listening)
        {
            return;
        }
        listening = true;
        Channel ch = Deci.decimationChannel(Side.SERVER);
        if (!(ch instanceof cpw.mods.fml.common.network.FMLEmbeddedChannel))
        {
            FMLLog.warning("[deciworldgen] market: Decimation's channel not found, trades do not move prices");
            return;
        }
        cpw.mods.fml.common.network.FMLEmbeddedChannel fc = (cpw.mods.fml.common.network.FMLEmbeddedChannel) ch;
        String codec = fc.findChannelHandlerNameForType(cpw.mods.fml.common.network.simpleimpl.SimpleIndexedCodec.class);
        if (fc.pipeline().get("deciworldgen:market") == null)
        {
            fc.pipeline().addAfter(codec, "deciworldgen:market", new Watcher());
        }
        FMLLog.info("[deciworldgen] market: watching trades on Decimation's channel");
    }

    /** Keys a trader class offers (its base list): only these can be pushed by trades. */
    private static synchronized boolean offers(Class<?> c, String k)
    {
        List<Object> b = BASE.get(c);
        for (int i = 0; b != null && i < b.size(); i++)
        {
            if (k.equals(key(Deci.offerStack(b.get(i)))))
            {
                return true;
            }
        }
        return false;
    }

    private static final Object LOCK = new Object();
    private static volatile boolean tradesChanged;

    /**
     * In Decimation's server channel right after its codec. Decimation handles a trade message at once on this thread, so
     * the player's caps are read just before and just after: a trade counts only when its own message moved the caps by
     * exactly its price, for an item the trader's class offers (a refused or forged message moves nothing).
     */
    @ChannelHandler.Sharable
    static final class Watcher extends ChannelInboundHandlerAdapter
    {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            Object[] t = Deci.tradeMessage(msg);
            Object h = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
            if (t == null || !(t[1] instanceof ItemStack) || !(h instanceof NetHandlerPlayServer))
            {
                ctx.fireChannelRead(msg);
                return;
            }
            EntityPlayerMP p = ((NetHandlerPlayServer) h).playerEntity;
            net.minecraft.entity.Entity trader = p.worldObj.getEntityByID((Integer) t[3]);
            if (trader != null && Deci.isTrader(trader))
            {
                ensure(trader.getClass());                  // the day's prices are what the handler checks against
            }
            long caps = Deci.player(p).bottlecaps(), gold = Deci.player(p).goldBottlecaps();
            ctx.fireChannelRead(msg);
            seen++;
            String k = key((ItemStack) t[1]);
            if (trader != null && traded((Integer) t[0], caps, Deci.player(p).bottlecaps(), gold, Deci.player(p).goldBottlecaps(),
                                         (Integer) t[2], (Integer) t[4]) && offers(trader.getClass(), k))
            {
                World w = overworld();
                if (w != null)
                {
                    synchronized (LOCK)
                    {
                        Data d = data(w);
                        Double old = d.pressure.get(k);
                        d.pressure.put(k, (old == null ? 0 : old) + ((Integer) t[0] == 0 ? 1 : -1));
                        d.markDirty();
                    }
                    counted++;
                    tradesChanged = true;
                }
            }
        }
    }

    /** A class's live list holds offers we did not write (it was reset): the day's prices go back in. */
    private static void ensure(Class<?> c)
    {
        List<Object> live = Deci.traderOfferLists().get(c);
        if (live != null && !live.isEmpty() && !OURS.contains(live.get(0)) && table.containsKey(c.getName()))
        {
            apply(c, table);
        }
    }

    // ---- the price table, server to client

    public static final class Table implements IMessage
    {
        Map<String, int[][]> rows;

        public Table()
        {
        }

        Table(Map<String, int[][]> rows)
        {
            this.rows = rows;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            rows = new HashMap<String, int[][]>();
            int n = buf.readShort();
            for (int i = 0; i < n; i++)
            {
                byte[] name = new byte[buf.readShort()];
                buf.readBytes(name);
                int[][] r = new int[buf.readShort()][];
                for (int j = 0; j < r.length; j++)
                {
                    r[j] = new int[] {buf.readInt(), buf.readInt(), buf.readByte()};
                }
                rows.put(new String(name, java.nio.charset.StandardCharsets.UTF_8), r);
            }
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeShort(rows.size());
            for (Map.Entry<String, int[][]> e : rows.entrySet())
            {
                byte[] name = e.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                buf.writeShort(name.length);
                buf.writeBytes(name);
                buf.writeShort(e.getValue().length);
                for (int[] r : e.getValue())
                {
                    buf.writeInt(r[0]);
                    buf.writeInt(r[1]);
                    buf.writeByte(r[2]);
                }
            }
        }
    }

    public static final class TableHandler implements IMessageHandler<Table, IMessage>
    {
        @Override
        public IMessage onMessage(Table m, MessageContext ctx)
        {
            table = m.rows;
            applyAll(m.rows);
            return null;
        }
    }

    // ---- the client: today's tag in the trader screen's tooltips

    public static final class Client
    {
        @SubscribeEvent
        public void onTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e)
        {
            net.minecraft.entity.Entity trader = Deci.traderOnScreen(net.minecraft.client.Minecraft.getMinecraft().currentScreen);
            int t = trader == null ? 0 : tag(trader.getClass(), e.itemStack);
            if (t == 1)
            {
                e.toolTip.add(net.minecraft.util.EnumChatFormatting.GREEN + "On sale today: 30% off");
            }
            else if (t == 2)
            {
                e.toolTip.add(net.minecraft.util.EnumChatFormatting.GOLD + "Wanted today: sells for 50% more");
            }
        }
    }
}
