package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Gas mask filters (user, 22 Oktober 2026, idea 42): a gas mask's filter wears only while its wearer is in radiation
 * (Decimation's irradiated flag, PlayerData), 12000 ticks (10 minutes) a filter; an empty filter protects no more until a
 * new "Gas Mask Filter" (Decimation's deci:ItemScrapMilitaryGasMaskFilter, before only scrap) is fitted with a right click.
 * - The life is in the worn mask's own tag (KEY, ticks); a mask without it has a full filter (loot, masks worn before).
 * - Server, one pass over the players a tick: the life counts down in our per player memory and goes into the tag at each
 *   whole percent, out of radiation, when the mask comes off, at death and at logout; never while a screen is open, since
 *   Decimation's gear screen holds the worn stack itself and a changed tag sent to its mask slot makes the client clear
 *   its cursor and rebuild the slots (PlayerExtraInventory.markDirty). Nothing is allocated between those writes.
 * - Empty: Deci.wearsGasMask (our dose, fixes/RadiationDose) answers no, and Decimation's own zone damage, which spares
 *   any gas mask (PlayerData.tick: 10 % a tick, 1 damage, DamageSources.irradiated), is done here. Chat warnings at 10 %
 *   and when used up, once each.
 * - Fitting: the client cancels a right click with the filter in hand while wearing a gas mask (a block with a use of its
 *   own keeps its click: loot blocks, props, chests, doors) and asks the server through our channel (message 32), once a
 *   press; the server checks again (filter in hand, one gas mask worn, no screen open, the
 *   filter not already full), at most once a player a tick, then fills the filter and takes one Gas Mask Filter (none in
 *   creative), with Decimation's gear sound.
 * - Client: "Filter: N%" in a gas mask's tooltip, a hint in the filter's; Decimation's HUD line "Protected" (IngameHud)
 *   and its missing screen tint (ClientEventHandler) follow the mask item only, so while the worn filter is empty the
 *   mask is hidden from them for that draw: "Irradiated" and the tint, as without a mask.
 * Loot: one Gas Mask Filter entry in the military list (where Decimation's military gas mask is) and one in the police
 * list; its own weight 0.5 makes it as rare as a gas mask there, rarer than every item of weight 0.9.
 */
public final class GasMaskFilter
{
    /** A full filter: ticks of radiation it lasts (10 minutes). */
    static final int LIFE = 12000;
    /** One percent of a filter: the tag is written at each step. */
    static final int STEP = LIFE / 100;
    /** The "almost used up" warning: 10 % left. */
    static final int LOW = LIFE / 10;
    /** The filter's life in the mask's tag, in ticks; none is a full filter. */
    static final String KEY = "deciworldgen_filter";
    private static final String PROPS = "deciworldgen_filter_wear";

    enum Fit { FIT, FULL, MANY, NO_MASK }

    enum Warn { LOW, EMPTY }

    private static final ConcurrentLinkedQueue<EntityPlayerMP> REQUESTS = new ConcurrentLinkedQueue<EntityPlayerMP>();
    private final List<EntityPlayerMP> done = new ArrayList<EntityPlayerMP>();
    private final Random random = new Random();
    private static boolean failed;

    // ------------------------------------------------------------ the rules (unit tested)

    /** The life a tick later: one less while worn in radiation, never below nothing. */
    static int wear(int life, boolean exposed)
    {
        return exposed ? Math.max(0, life - 1) : life;
    }

    static boolean protects(int life)
    {
        return life > 0;
    }

    /** The percent shown, rounded up: any life left shows at least 1 %. */
    static int percent(int life)
    {
        return (Math.max(0, life) * 100 + LIFE - 1) / LIFE;
    }

    /** A whole percent: the tag is written here. */
    static boolean step(int life)
    {
        return life % STEP == 0;
    }

    /** A mask's filter from its tag: a mask without one has a full filter. */
    static int life(NBTTagCompound tag)
    {
        return tag == null || !tag.hasKey(KEY) ? LIFE : Math.max(0, Math.min(LIFE, tag.getInteger(KEY)));
    }

    /** No mask, no filter. */
    static int life(ItemStack mask)
    {
        return mask == null ? 0 : life(mask.getTagCompound());
    }

    static void setLife(ItemStack mask, int life)
    {
        if (mask.getTagCompound() == null)
        {
            mask.setTagCompound(new NBTTagCompound());
        }
        mask.getTagCompound().setInteger(KEY, Math.max(0, Math.min(LIFE, life)));
    }

    /** What a right click with a filter does: a gas mask, one of them, its filter not full. */
    static Fit fit(boolean gasMask, int masks, int percent)
    {
        if (!gasMask)
        {
            return Fit.NO_MASK;
        }
        if (masks > 1)
        {
            return Fit.MANY;
        }
        return percent >= 100 ? Fit.FULL : Fit.FIT;
    }

    /** The warning due when a filter wore from before to now: at 10 % and when used up. */
    static Warn warning(int before, int now)
    {
        if (now == 0 && before > 0)
        {
            return Warn.EMPTY;
        }
        return before > LOW && now <= LOW && now > 0 ? Warn.LOW : null;
    }

    static String message(Fit f)
    {
        switch (f)
        {
            case FULL: return "Your gas mask filter is still full!";
            case MANY: return "You can only fit a filter into 1 gas mask at a time!";
            case NO_MASK: return "You need to wear a gas mask to fit a filter!";
            default: return EnumChatFormatting.GREEN + "You've fitted a new Gas Mask Filter!";
        }
    }

    // ------------------------------------------------------------ server

    /** A player's worn filter while the server counts it down (the life itself is the mask's: nothing to save here). */
    static final class Filter implements IExtendedEntityProperties
    {
        ItemStack mask;                                   // the stack in the mask slot when last looked, a gas mask or null
        int life;
        boolean pending;                                  // life is ahead of the mask's tag
        Warn given;                                       // the last warning for this mask

        public void saveNBTData(NBTTagCompound tag)
        {
        }

        public void loadNBTData(NBTTagCompound tag)
        {
        }

        public void init(Entity entity, World world)
        {
        }
    }

    private static Filter props(EntityPlayer p)
    {
        return (Filter) p.getExtendedProperties(PROPS);
    }

    @SubscribeEvent
    public void onConstructing(EntityEvent.EntityConstructing e)
    {
        if (e.entity instanceof EntityPlayerMP)
        {
            e.entity.registerExtendedProperties(PROPS, new Filter());
        }
    }

    /**
     * The filter left in a gas mask this player wears: our running count on the server, the mask's tag elsewhere (the
     * client's copy, a mask not looked at yet).
     */
    public static int left(EntityPlayer p, ItemStack mask)
    {
        Filter f = p == null ? null : props(p);
        return f != null && mask != null && f.mask == mask ? f.life : life(mask);
    }

    /** Server: the filters of every player a tick, fitting requests first. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
        {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null)
        {
            return;
        }
        long start = System.nanoTime();
        long bytes = measureAllocation ? allocated() : 0;
        if (!REQUESTS.isEmpty())
        {
            requests();
        }
        List<?> players = server.getConfigurationManager().playerEntityList;
        for (int i = 0; i < players.size(); i++)
        {
            EntityPlayerMP p = (EntityPlayerMP) players.get(i);
            Filter f = props(p);
            if (f == null || p.isDead || p.getHealth() <= 0)
            {
                continue;                                   // dead or dying: the mask is the corpse's now
            }
            try
            {
                tick(p, f);
            }
            catch (RuntimeException ex)
            {
                if (!failed)
                {
                    failed = true;                          // once: never out of the server tick
                    FMLLog.warning("[deciworldgen] gas mask filter tick failed: %s", ex);
                }
            }
        }
        if (measureAllocation)
        {
            allocatedBytes += allocated() - bytes - allocationOverhead;
            allocationTicks++;
            return;                                         // the counter readings would count as our time
        }
        tickRuns++;
        tickNanos += System.nanoTime() - start;
    }

    /**
     * For the dev test: one player's part of our server tick run n times on this thread (the server's): the bytes it
     * allocated and its time in nanoseconds.
     */
    public static long[] measureTicks(EntityPlayerMP p, int n)
    {
        Filter f = props(p);
        GasMaskFilter run = new GasMaskFilter();
        long bytes = allocated(), start = System.nanoTime();
        for (int i = 0; i < n && f != null; i++)
        {
            run.tick(p, f);
        }
        long nanos = System.nanoTime() - start;
        return new long[] {allocated() - bytes, nanos};
    }

    private void tick(EntityPlayerMP p, Filter f)
    {
        ItemStack mask = Deci.playerMask(p);
        if (mask != f.mask)
        {
            flush(f);                                       // the mask taken off keeps its wear wherever it went
            f.mask = Deci.isGasMask(mask) ? mask : null;
            f.life = life(f.mask);
            f.pending = false;
            f.given = null;
        }
        if (f.mask == null)
        {
            return;
        }
        boolean exposed = !p.capabilities.isCreativeMode && Deci.irradiated(p);
        if (exposed)
        {
            if (f.life > 0)
            {
                int before = f.life;
                f.life = wear(before, true);
                f.pending = true;
                wornTicks++;
                Warn w = warning(before, f.life);
                if (w != null)
                {
                    tell(p, f, w);
                }
            }
            else
            {
                if (f.given != Warn.EMPTY)
                {
                    tell(p, f, Warn.EMPTY);                 // a mask already empty when its wearer walks into radiation
                }
                // Decimation's zone damage spares any gas mask (PlayerData.tick); an empty one is none
                if (random.nextFloat() < 0.1f)
                {
                    p.attackEntityFrom(Deci.irradiatedDamage(), 1f);
                }
            }
        }
        if (f.pending && p.openContainer == p.inventoryContainer && (!exposed || step(f.life)))
        {
            flush(f);
            if (f.life == 0)
            {
                Deci.markPlayerDataDirty(p);                // the client's copy of the mask: its HUD says Irradiated now
            }
        }
    }

    private static void flush(Filter f)
    {
        if (f.pending && f.mask != null)
        {
            setLife(f.mask, f.life);
            flushes++;
        }
        f.pending = false;
    }

    private static void tell(EntityPlayerMP p, Filter f, Warn w)
    {
        f.given = w;
        if (w == Warn.EMPTY)
        {
            emptyWarnings++;
        }
        else
        {
            lowWarnings++;
        }
        p.addChatMessage(new ChatComponentText(w == Warn.EMPTY
            ? EnumChatFormatting.RED + "Your gas mask filter is used up! Fit a new Gas Mask Filter."
            : EnumChatFormatting.YELLOW + "Your gas mask filter is almost used up!"));
    }

    /** Sets the filter of a mask this player wears (a fit, the dev test): the tag, our count, the client told. */
    static void setLeft(EntityPlayer p, ItemStack mask, int life)
    {
        setLife(mask, life);
        Filter f = props(p);
        if (f != null && f.mask == mask)
        {
            f.life = Math.max(0, Math.min(LIFE, life));
            f.pending = false;
            f.given = null;
        }
        Deci.markPlayerDataDirty(p);
    }

    /** The mask keeps its last wear when its wearer dies (before Decimation's DeathHandler moves the gear to the corpse). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent e)
    {
        if (e.entity instanceof EntityPlayerMP)
        {
            Filter f = props((EntityPlayer) e.entity);
            if (f != null)
            {
                flush(f);
            }
        }
    }

    /** And when its wearer leaves (FML posts this before the player is saved). */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e)
    {
        Filter f = props(e.player);
        if (f != null)
        {
            flush(f);
        }
    }

    private void requests()
    {
        for (EntityPlayerMP p; (p = REQUESTS.poll()) != null; )
        {
            if (p.isDead || done.contains(p))
            {
                dropped++;                                  // one fit a player a tick
                continue;
            }
            done.add(p);
            try
            {
                fitOne(p);
            }
            catch (RuntimeException ex)
            {
                FMLLog.warning("[deciworldgen] gas mask filter fit failed: %s", ex);   // never out of the tick
            }
        }
        done.clear();
    }

    private static void fitOne(EntityPlayerMP p)
    {
        ItemStack held = p.getHeldItem();
        if (held == null || held.getItem() != Deci.gasMaskFilter() || held.stackSize <= 0 || p.openContainer != p.inventoryContainer)
        {
            return;                                         // the client's checks again: never trusted
        }
        ItemStack mask = Deci.playerMask(p);
        boolean gas = Deci.isGasMask(mask);
        Fit fit = fit(gas, gas ? mask.stackSize : 0, gas ? percent(left(p, mask)) : 0);
        if (fit != Fit.FIT)
        {
            refused++;
            p.addChatMessage(new ChatComponentText(message(fit)));
            return;
        }
        setLeft(p, mask, LIFE);
        if (!p.capabilities.isCreativeMode && --held.stackSize <= 0)
        {
            p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
        }
        fitted++;
        p.worldObj.playSoundAtEntity(p, "deci:misc.armor.equip", 1f, 1f);
        p.addChatMessage(new ChatComponentText(message(fit)));
        p.inventoryContainer.detectAndSendChanges();
    }

    /** "Fit the Gas Mask Filter in my hand into the gas mask I wear" (no fields: the server reads both). */
    public static final class FitMessage implements IMessage
    {
        public void fromBytes(ByteBuf buf)
        {
        }

        public void toBytes(ByteBuf buf)
        {
        }
    }

    public static final class FitHandler implements IMessageHandler<FitMessage, IMessage>
    {
        public IMessage onMessage(FitMessage m, MessageContext ctx)
        {
            REQUESTS.add(ctx.getServerHandler().playerEntity);   // handled on the server tick
            return null;
        }
    }

    /**
     * One Gas Mask Filter entry in the military loot list (every container holding Decimation's military gas mask:
     * military crates and wrecks, care packages, mech wrecks) and one in the police list (police crates and cars).
     */
    public static void addLoot()
    {
        ItemStack filter = new ItemStack(Deci.gasMaskFilter());
        Item military = GameRegistry.findItem("deci", "gasmaskmilitary"), police = GameRegistry.findItem("deci", "policevest");
        int m = military == null ? 0 : Deci.addLootLike(military, filter);
        int p = police == null ? 0 : Deci.addLootLike(police, filter);
        FMLLog.info("[deciworldgen] gas mask filters: %d military and %d police loot pools", m, p);
    }

    // ------------------------------------------------------------ for the dev test

    static long tickRuns, tickNanos, wornTicks, flushes;
    static int fitted, refused, dropped, lowWarnings, emptyWarnings;
    /** Allocation measuring (dev test only): the server thread's allocated bytes around our tick. */
    static volatile boolean measureAllocation;
    static long allocatedBytes, allocationTicks, allocationOverhead;

    private static long allocated()
    {
        java.lang.management.ThreadMXBean b = java.lang.management.ManagementFactory.getThreadMXBean();
        return b instanceof com.sun.management.ThreadMXBean
            ? ((com.sun.management.ThreadMXBean) b).getThreadAllocatedBytes(Thread.currentThread().getId()) : 0;
    }

    /** For the dev test: measure the bytes our server tick allocates (the reading's own cost taken off). */
    public static void measureAllocation(boolean on)
    {
        if (on)
        {
            long a = allocated(), b = allocated();
            allocationOverhead = b - a;
            allocatedBytes = 0;
            allocationTicks = 0;
        }
        measureAllocation = on;
    }

    /** For the dev test: average bytes our server tick allocated while measuring, and the ticks measured. */
    public static String allocation()
    {
        return String.format("%.2f bytes a tick over %d ticks (reading cost %d taken off)",
            allocationTicks == 0 ? 0.0 : (double) allocatedBytes / allocationTicks, allocationTicks, allocationOverhead);
    }

    /** For the dev test: average time of our server tick in microseconds. */
    public static double averageTickMicros()
    {
        return tickRuns == 0 ? 0 : tickNanos / 1000.0 / tickRuns;
    }

    /** For the dev test: counters "fitted, refused, dropped, low warnings, empty warnings, tag writes". */
    public static int[] counters()
    {
        return new int[] {fitted, refused, dropped, lowWarnings, emptyWarnings, (int) flushes};
    }

    /** For tests: the filter left in the gas mask this player wears (ticks), -1 without one. */
    public static int filterOf(EntityPlayer p)
    {
        ItemStack m = Deci.playerMask(p);
        return Deci.isGasMask(m) ? left(p, m) : -1;
    }

    /** For tests: the filter written in a mask's tag (a full filter without one), -1 for no mask. */
    public static int tagOf(ItemStack mask)
    {
        return mask == null ? -1 : life(mask.getTagCompound());
    }

    /** For tests: sets the filter of the gas mask this player wears. */
    public static void setFilter(EntityPlayer p, int life)
    {
        ItemStack m = Deci.playerMask(p);
        if (Deci.isGasMask(m))
        {
            setLeft(p, m, life);
        }
    }

    /** A full filter in ticks (LIFE), for tests. */
    public static int full()
    {
        return LIFE;
    }

    /** The percent a filter shows (rounded up), for tests. */
    public static int percentOf(int life)
    {
        return percent(life);
    }

    // ------------------------------------------------------------ client

    /** Client: the right click that fits a filter, the tooltips, Decimation's HUD with an empty filter. */
    public static final class Client
    {
        private long lastSent = -1, blockPress = -1;
        private ItemStack hidden;
        private EntityPlayer hiddenFrom;
        /** For the dev test: HUD frames looked at, their time in nanoseconds, frames with the empty mask hidden. */
        static long hudFrames, hudNanos, hudHidden;
        static volatile long sent;

        /**
         * A right click with the filter in hand while wearing a gas mask fits it (Decimation does nothing with a filter),
         * except on a block with a use of its own: a loot crate, wreck or bin, a prop, a chest or door keep their click, as a
         * held item never takes a chest's click in vanilla (else a crate click would swap a filter and loot nothing).
         */
        @SubscribeEvent
        public void onInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent e)
        {
            EntityPlayer p = e.entityPlayer;
            if (!p.worldObj.isRemote || e.action == net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.LEFT_CLICK_BLOCK)
            {
                return;
            }
            ItemStack held = p.getHeldItem();
            if (held == null || held.getItem() != Deci.gasMaskFilter() || !Deci.isGasMask(Deci.playerMask(p)))
            {
                return;
            }
            long now = p.worldObj.getTotalWorldTime();
            if (e.action == net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                && usable(p.worldObj, e.x, e.y, e.z))
            {
                blockPress = now;                           // the block's click; the air event of this press is skipped too
                return;
            }
            if (now == blockPress)
            {
                return;
            }
            e.setCanceled(true);                            // no block used, no item use sent
            if (now != lastSent)                            // one press fires a block and an air event
            {
                lastSent = now;
                sent++;
                Net.CHANNEL.sendToServer(new FitMessage());
            }
        }

        /** A block a right click uses: Decimation's loot blocks, anything with a tile entity (props, chests, Decimation's doors). */
        private static boolean usable(World w, int x, int y, int z)
        {
            net.minecraft.block.Block b = w.getBlock(x, y, z);
            return b.hasTileEntity(w.getBlockMetadata(x, y, z)) || Deci.hasLootPool(b);
        }

        @SubscribeEvent
        public void onTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e)
        {
            ItemStack s = e.itemStack;
            if (Deci.isGasMask(s))
            {
                int pct = percent(life(s));
                e.toolTip.add(EnumChatFormatting.GRAY + "Filter: " + (pct > 50 ? EnumChatFormatting.GREEN
                    : pct > 10 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED) + pct + "%");
            }
            else if (s != null && s.getItem() == Deci.gasMaskFilter())
            {
                e.toolTip.add(EnumChatFormatting.GRAY + "Right click to fit it into your gas mask");
            }
        }

        /** Before Decimation's HUD (IngameHud, its tint): an empty filter's mask is no gas mask to them. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void beforeHud(net.minecraftforge.client.event.RenderGameOverlayEvent.Post e)
        {
            if (e.type != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL)
            {
                return;
            }
            long start = System.nanoTime();
            EntityPlayer p = net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            restore(p);                                     // a frame that failed in between
            if (p != null && Deci.irradiated(p))
            {
                ItemStack m = Deci.playerMask(p);
                if (Deci.isGasMask(m) && life(m) == 0)
                {
                    hidden = m;
                    hiddenFrom = p;
                    Deci.setPlayerMask(p, null);            // on the client only a field: no sound, no sync
                    hudHidden++;
                }
            }
            hudFrames++;
            hudNanos += System.nanoTime() - start;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void afterHud(net.minecraftforge.client.event.RenderGameOverlayEvent.Post e)
        {
            if (e.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL)
            {
                long start = System.nanoTime();
                restore(net.minecraft.client.Minecraft.getMinecraft().thePlayer);
                hudNanos += System.nanoTime() - start;
            }
        }

        private void restore(EntityPlayer p)
        {
            if (hidden != null)
            {
                if (p != null && p == hiddenFrom && Deci.playerMask(p) == null)
                {
                    Deci.setPlayerMask(p, hidden);
                }
                hidden = null;
                hiddenFrom = null;
            }
        }

        /** For the dev test: average time of the two HUD handlers a frame in microseconds. */
        public static double averageHudMicros()
        {
            return hudFrames == 0 ? 0 : hudNanos / 1000.0 / hudFrames;
        }

        public static long hudFrames()
        {
            return hudFrames;
        }

        public static long hudHidden()
        {
            return hudHidden;
        }

        public static long sent()
        {
            return sent;
        }
    }
}
