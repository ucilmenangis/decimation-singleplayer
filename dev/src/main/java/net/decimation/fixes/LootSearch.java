package net.decimation.fixes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/**
 * Tarkov style search of loot (feature 12 C, docs/superpowers/specs/2026-10-22-loot-rules-design.md; user, 22 Oktober
 * 2026: "loot system where it need to be analyzed one by one before taking it. so the item will show loading icon
 * animation then if it done it can be take it"; chosen: by value, 1 to 3 s an item).
 *
 * A loot inventory starts unsearched: the server reveals its filled slots in slot order, each after its time (guns,
 * keycards, gold bottlecaps 3 s; attachments, armor, vests, backpacks, masks, repair parts and filters 2 s; the rest 1 s),
 * only while its screen is open, and keeps the progress of that inventory when the screen closes (a crate rolls a new
 * inventory each open, our loot bodies keep theirs). Our GearContainer (Deci.GearContainer, the container of Decimation's
 * inventory and loot screen) refuses every click on an unsearched slot on both sides; the server sends the progress on our
 * channel when the screen opens and at each reveal; the client draws a dark "?" over unsearched slots and a spinner with
 * the progress on the one being searched, and gives an unsearched stack no tooltip.
 */
public final class LootSearch
{
    /** Search state of one loot inventory: its filled slots in slot order, each one's ticks, the one being searched. */
    static final class Search
    {
        private final int[] order, ticks;
        private int next, done;

        Search(int[] order, int[] ticks)
        {
            this.order = order;
            this.ticks = ticks;
        }

        /** One tick of searching; true when it revealed a slot. */
        boolean tick()
        {
            if (next >= order.length)
            {
                return false;
            }
            if (++done < ticks[next])
            {
                return false;
            }
            next++;
            done = 0;
            return true;
        }

        boolean hidden(int slot)
        {
            for (int i = next; i < order.length; i++)
            {
                if (order[i] == slot)
                {
                    return true;
                }
            }
            return false;
        }

        /** The slot being searched, -1 when all are. */
        int searching()
        {
            return next < order.length ? order[next] : -1;
        }

        int done()
        {
            return done;
        }

        int total()
        {
            return next < order.length ? ticks[next] : 0;
        }

        boolean finished()
        {
            return next >= order.length;
        }
    }

    /** Search on (default); -Ddeciworldgen.lootsearch=false turns it off, tests that move loot at once switch it for a while. */
    public static volatile boolean enabled = !"false".equals(System.getProperty("deciworldgen.lootsearch"));

    /** Items set apart by value at start (the gold bottlecap, the gas mask filter); classes are checked in seconds(). */
    private static final Set<Item> THREE = new HashSet<Item>(), TWO = new HashSet<Item>();

    /** The server tick handler (registered on FML's bus by our mod). */
    public LootSearch()
    {
    }

    /** Seconds to search a stack: guns, keycards, gold bottlecaps 3; attachments, armor, gear, repair parts, filters 2; else 1. */
    static int seconds(ItemStack s)
    {
        Item i = s == null ? null : s.getItem();
        if (i == null)
        {
            return 1;
        }
        if (Deci.isGun(i) || Deci.isKeycard(i) || THREE.contains(i))
        {
            return 3;
        }
        if (Deci.isAttachment(i) || i instanceof ItemArmor || Deci.isWornGear(i) || i instanceof RepairPart || TWO.contains(i))
        {
            return 2;
        }
        return 1;
    }

    /** Reads the items set apart by value (after every item is registered). */
    public static void init()
    {
        THREE.add(Deci.goldBottlecap());
        TWO.add(Deci.gasMaskFilter());
    }

    private static Search start(IInventory inv)
    {
        int n = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            n += inv.getStackInSlot(i) != null ? 1 : 0;
        }
        int[] order = new int[n], ticks = new int[n];
        n = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null)
            {
                order[n] = i;
                ticks[n++] = seconds(s) * 20;
            }
        }
        return new Search(order, ticks);
    }

    // ---- server

    private static final Map<IInventory, Search> SEARCHES = new WeakHashMap<IInventory, Search>();
    /** Per container whether it shows the player's loot inventory (the loot screen), asked once a container. */
    private static final Map<Container, Boolean> LOOT_SCREEN = new WeakHashMap<Container, Boolean>();
    private final Map<EntityPlayer, Container> told = new WeakHashMap<EntityPlayer, Container>();
    /** Nanoseconds spent in onServerTick and its ticks (the dev test reads them). */
    static long nanos, calls;
    private static final long[] TICK_SAMPLES = new long[256], DRAW_SAMPLES = new long[256];

    private static long median(long[] ring, long count)
    {
        int n = (int) Math.min(count, ring.length);
        if (n == 0)
        {
            return 0;
        }
        long[] c = java.util.Arrays.copyOf(ring, n);
        java.util.Arrays.sort(c);
        return c[n / 2];
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e)
    {
        MinecraftServer server = MinecraftServer.getServer();
        if (e.phase != TickEvent.Phase.END || server == null)
        {
            return;
        }
        long t0 = System.nanoTime();
        for (Object o : server.getConfigurationManager().playerEntityList)
        {
            EntityPlayerMP p = (EntityPlayerMP) o;
            Container c = p.openContainer;
            IInventory inv = enabled && Deci.hasShiftClick(c) ? Deci.lootInventory(p) : null;
            if (inv == null || !shows(c, inv))
            {
                told.remove(p);
                continue;
            }
            Search s = SEARCHES.get(inv);
            if (s == null)
            {
                s = start(inv);
                SEARCHES.put(inv, s);
            }
            boolean fresh = told.get(p) != c;
            if (fresh)
            {
                told.put(p, c);
            }
            if (s.tick() || fresh)
            {
                Net.CHANNEL.sendTo(new State(c.windowId, s), p);
            }
        }
        long took = System.nanoTime() - t0;
        nanos += took;
        TICK_SAMPLES[(int) (calls++ & 255)] = took;
    }

    private static boolean shows(Container c, IInventory inv)
    {
        Boolean b = LOOT_SCREEN.get(c);
        if (b == null)
        {
            b = Boolean.FALSE;
            for (Object o : c.inventorySlots)
            {
                if (((Slot) o).inventory == inv)
                {
                    b = Boolean.TRUE;
                    break;
                }
            }
            LOOT_SCREEN.put(c, b);
        }
        return b;
    }

    /** A loot slot not searched yet: on the server by its inventory's search, on the client by the last progress sent. */
    public static boolean hidden(Slot slot)
    {
        if (!enabled || slot == null || InventoryShift.region(slot) != InventoryShift.Region.LOOT)
        {
            return false;
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isServer())
        {
            Search s = SEARCHES.get(slot.inventory);
            return s != null && s.hidden(slot.getSlotIndex());
        }
        return Client.hidden(slot.getSlotIndex());
    }

    /** Container.slotClick of our container: a click on an unsearched slot does nothing (both sides, any kind of click). */
    public static boolean refuses(Container c, int slotId)
    {
        return slotId >= 0 && slotId < c.inventorySlots.size() && hidden(c.getSlot(slotId));
    }

    /** A loot inventory's search on the server as {slots revealed, ticks into the next, its ticks, slots}, null before it opened. */
    public static int[] progress(IInventory inv)
    {
        Search s = SEARCHES.get(inv);
        return s == null ? null : new int[] {s.next, s.done, s.total(), s.order.length};
    }

    /** Costs: {median server tick ns, server ticks, median draw ns a frame, frames drawn} (medians: a busy machine's
     *  descheduling makes averages swing). */
    public static long[] costs()
    {
        return new long[] {median(TICK_SAMPLES, calls), calls, median(DRAW_SAMPLES, Client.frames), Client.frames};
    }

    /** Seconds a stack takes (the dev test). */
    public static int secondsOf(ItemStack s)
    {
        return seconds(s);
    }

    // ---- the message: progress of the open loot screen, server to client

    public static final class State implements IMessage
    {
        int window, next, done, total;
        int[] order;

        public State()
        {
        }

        State(int window, Search s)
        {
            this.window = window;
            this.order = s.order;
            this.next = s.next;
            this.done = s.done;
            this.total = s.total();
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            window = buf.readInt();
            int n = buf.readUnsignedByte();
            order = new int[n];
            for (int i = 0; i < n; i++)
            {
                order[i] = buf.readUnsignedByte();
            }
            next = buf.readUnsignedByte();
            done = buf.readUnsignedShort();
            total = buf.readUnsignedShort();
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(window);
            buf.writeByte(order.length);
            for (int o : order)
            {
                buf.writeByte(o);
            }
            buf.writeByte(next);
            buf.writeShort(done);
            buf.writeShort(total);
        }
    }

    public static final class StateHandler implements IMessageHandler<State, IMessage>
    {
        @Override
        public IMessage onMessage(State m, MessageContext ctx)
        {
            Client.receive(m);
            return null;
        }
    }

    // ---- client

    /** The client's view of the open loot screen's search, and its drawing. */
    public static final class Client
    {
        static volatile State state;
        static volatile long at;
        static long ticks;

        static void receive(State m)
        {
            state = m;
            at = ticks;
        }

        static boolean hidden(int slot)
        {
            State s = state;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (s == null || mc.thePlayer == null || mc.thePlayer.openContainer == null
                || mc.thePlayer.openContainer.windowId != s.window)
            {
                return false;
            }
            for (int i = s.next; i < s.order.length; i++)
            {
                if (s.order[i] == slot)
                {
                    return true;
                }
            }
            return false;
        }

        /** Progress 0 .. 1 of the slot being searched, counted on from the last message. */
        static float progress(float partial)
        {
            State s = state;
            return s == null || s.total <= 0 ? 0 : Math.min(1f, (s.done + ticks - at + partial) / s.total);
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent e)
        {
            if (e.phase == TickEvent.Phase.END)
            {
                ticks++;
            }
        }

        /** No tooltip for a stack in an unsearched slot: its name would give it away (last, after every mod added its lines). */
        @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
        public void onTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e)
        {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (state == null || mc.thePlayer == null || !Deci.hasShiftClick(mc.thePlayer.openContainer))
            {
                return;
            }
            for (Object o : mc.thePlayer.openContainer.inventorySlots)
            {
                Slot s = (Slot) o;
                if (s.getStack() == e.itemStack && LootSearch.hidden(s))
                {
                    e.toolTip.clear();
                    return;
                }
            }
        }

        /** Nanoseconds spent drawing and frames drawn (the dev test reads them). */
        static long drawNanos, frames, warm;

        /** A dark "?" over every unsearched slot, a turning dot and the progress on the one being searched. */
        @SubscribeEvent
        public void onDraw(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post e)
        {
            State st = state;
            if (st == null || !Deci.isInventoryScreen(e.gui) || st.next >= st.order.length)
            {
                return;
            }
            net.minecraft.client.gui.inventory.GuiContainer g = (net.minecraft.client.gui.inventory.GuiContainer) e.gui;
            if (g.inventorySlots == null || g.inventorySlots.windowId != st.window)
            {
                return;
            }
            long t0 = System.nanoTime();
            int left = guiInt(g, "guiLeft", "field_147003_i"), top = guiInt(g, "guiTop", "field_147009_r");
            net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            org.lwjgl.opengl.GL11.glPushMatrix();
            org.lwjgl.opengl.GL11.glTranslatef(0, 0, 300);   // over the items GuiContainer drew
            for (Object o : g.inventorySlots.inventorySlots)
            {
                Slot s = (Slot) o;
                if (InventoryShift.region(s) != InventoryShift.Region.LOOT || !hidden(s.getSlotIndex()))
                {
                    continue;
                }
                int x = left + s.xDisplayPosition, y = top + s.yDisplayPosition;
                net.minecraft.client.gui.Gui.drawRect(x, y, x + 16, y + 16, 0xF0141414);
                if (s.getSlotIndex() == st.order[st.next])
                {
                    float p = progress(e.renderPartialTicks);
                    net.minecraft.client.gui.Gui.drawRect(x, y + 15, x + Math.round(16 * p), y + 16, 0xFFB0B0B0);
                    int k = (int) ((ticks % 8 + 8) % 8);
                    int[][] ring = {{7, 1}, {11, 3}, {13, 7}, {11, 11}, {7, 13}, {3, 11}, {1, 7}, {3, 3}};
                    for (int i = 0; i < 8; i++)
                    {
                        int c = i == k ? 0xFFE0E0E0 : i == (k + 7) % 8 ? 0xFF808080 : 0xFF3A3A3A;
                        net.minecraft.client.gui.Gui.drawRect(x + ring[i][0], y + ring[i][1], x + ring[i][0] + 2, y + ring[i][1] + 2, c);
                    }
                }
                else
                {
                    font.drawStringWithShadow("?", x + 6, y + 4, 0x9A9A9A);
                }
            }
            org.lwjgl.opengl.GL11.glPopMatrix();
            if (++warm > 30)                                 // the first frames load the font and compile: left out
            {
                long took = System.nanoTime() - t0;
                drawNanos += took;
                DRAW_SAMPLES[(int) (frames++ & 255)] = took;
            }
        }

        private static java.lang.reflect.Field left, top;

        private static int guiInt(net.minecraft.client.gui.inventory.GuiContainer g, String mcp, String srg)
        {
            try
            {
                java.lang.reflect.Field f = mcp.equals("guiLeft") ? left : top;
                if (f == null)
                {
                    f = cpw.mods.fml.relauncher.ReflectionHelper.findField(net.minecraft.client.gui.inventory.GuiContainer.class, mcp, srg);
                    if (mcp.equals("guiLeft"))
                    {
                        left = f;
                    }
                    else
                    {
                        top = f;
                    }
                }
                return f.getInt(g);
            }
            catch (IllegalAccessException ex)
            {
                return 0;
            }
        }
    }
}
