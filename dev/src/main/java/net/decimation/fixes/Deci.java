package net.decimation.fixes;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Readable access to Decimation's obfuscated classes. Decimation ships
 * obfuscated (deci.ay.i, adK...) and the game loads those exact names, so
 * our code has to call them; the readable names exist only in the reading
 * copy deobf/src (deobf/names.tsv, deobf/decimation.srg). Every obfuscated
 * name our fixes use lives here, so the rest of the code reads normally.
 */
public final class Deci
{
    private Deci()
    {
    }

    // ---- weapons: GunItem = deci.ay.i, AttachmentItem = deci.ay.h

    public static boolean isGun(Item item)
    {
        return item instanceof deci.ay.i;
    }

    private static java.lang.reflect.Method gunInitTag;

    /**
     * A looted gun's rounds (fixes/LootMagazines): GunItem.initTag (deci.ay.i.n, private: the tag Decimation gives a gun the
     * first time it is held), then "ammo". ammoType stays 0, the gun's first magazine (ammoTypes aep[0]), which a reload
     * then ejects with the rounds less the chambered one. False for launchers and crossbows (a loaded rocket for free)
     * and when the stack already has a tag.
     */
    public static boolean loadLootGun(net.minecraft.item.ItemStack stack, java.util.Random r)
    {
        if (stack == null || !(stack.getItem() instanceof deci.ay.i) || stack.hasTagCompound())
        {
            return false;
        }
        deci.ay.i g = (deci.ay.i) stack.getItem();
        if (g.aes == deci.ay.c.rocket || g.aes == deci.ay.c.crossbow || g.aep == null || g.aep.length == 0 || g.aep[0] == null)
        {
            return false;
        }
        deci.ay.f mag = g.aep[0];
        int max = mag.adE != deci.ay.a.bullet && mag.adE != deci.ay.a.sclip ? mag.adF : g.w(new net.minecraft.item.ItemStack(g));
        int rounds = LootMagazines.gunRounds(max, r);
        if (rounds <= 0)
        {
            return false;
        }
        try
        {
            if (gunInitTag == null)
            {
                java.lang.reflect.Method m = deci.ay.i.class.getDeclaredMethod("n", net.minecraft.item.ItemStack.class);
                m.setAccessible(true);
                gunInitTag = m;
            }
            gunInitTag.invoke(g, stack);
        }
        catch (ReflectiveOperationException ex)
        {
            return false;
        }
        stack.stackTagCompound.setInteger("ammo", rounds);
        return true;
    }

    /** GunItem.category (aes): Decimation's WeaponCategory name (rifle, smg, mg, shotgun, pistol, revolver, rocket, crossbow ...). */
    public static String gunCategory(Item gun)
    {
        return gun instanceof deci.ay.i && ((deci.ay.i) gun).aes != null ? ((deci.ay.i) gun).aes.name() : null;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static KeyBinding reloadKey;

    /** KeyBindingHandler.keyReload (deci.c.a field "do", a Java keyword, so read by reflection): Decimation's R. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static KeyBinding reloadKey()
    {
        if (reloadKey == null)
        {
            try
            {
                reloadKey = (KeyBinding) deci.c.a.class.getField("do").get(null);
            }
            catch (ReflectiveOperationException e)
            {
                return null;
            }
        }
        return reloadKey;
    }

    /**
     * Decimation's Rack animation of this gun on the gun renderer (GunAnimation.get b, "animations/<gunName aer>/<gunName>
     * Rack.anib", into GunItemRenderer.iNSTANCE TI .currentAnimation TM, active): what PlayerData does after a reload.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void playRack(ItemStack gun)
    {
        if (gun == null || !(gun.getItem() instanceof deci.ay.i) || deci.K.b.TI == null)
        {
            return;
        }
        String name = ((deci.ay.i) gun.getItem()).aer;
        deci.F.a rack = deci.F.a.b(new net.minecraft.util.ResourceLocation("deci", "animations/" + name + "/" + name + "Rack.anib"));
        if (rack != null)
        {
            deci.K.b.TI.TM = rack;
            rack.active = true;
        }
    }

    public static boolean isAttachment(Item item)
    {
        return item instanceof deci.ay.h;
    }

    /** GunItem.getSightAttachment(stack) (H): the fitted scope / sight, or null. */
    public static Item sightAttachment(ItemStack gun)
    {
        return ((deci.ay.i) gun.getItem()).H(gun);
    }

    /** GunItem.hasIntegratedScope() (fM). */
    public static boolean hasIntegratedScope(Item gun)
    {
        return ((deci.ay.i) gun).fM();
    }

    /** GunItem.getIntegratedScopeName() (fQ). */
    public static String integratedScopeName(Item gun)
    {
        return ((deci.ay.i) gun).fQ();
    }

    /** AttachmentItem.name (adK): "reddot", "2x", "4x", "8x", "dragunovScope"... */
    public static String attachmentName(Item attachment)
    {
        return ((deci.ay.h) attachment).adK;
    }

    /** AttachmentItem.isScope (adR). */
    public static boolean isScope(Item attachment)
    {
        return ((deci.ay.h) attachment).adR;
    }

    /** AttachmentItem.zoomFov (adS): the old scope camera used FOV 50 - zoomFov. */
    public static float zoomFov(Item attachment)
    {
        return ((deci.ay.h) attachment).adS;
    }

    /** True when a gun with a scope (fitted or integrated) is in this stack. */
    public static boolean isScopedGun(ItemStack stack)
    {
        if (stack == null || !isGun(stack.getItem()))
        {
            return false;
        }
        Item sight = sightAttachment(stack);
        return sight != null ? isScope(sight) : hasIntegratedScope(stack.getItem());
    }

    // ---- gunsmith (fixes/gunsmith): DeciConstants = deci.Q.c, DecimationMod = deci.a.b, FilteredSlot = deci.f.k,
    //      WeaponSkinItem = deci.ay.j, TracerItem = deci.ay.k, SkinRegistry = deci.aD.q

    /** DeciConstants.gUI_ATTACHMENTS (Xc): the GUI id Decimation's attach key (the ATTACH action) opens. */
    public static int attachmentsGuiId()
    {
        return deci.Q.c.Xc;
    }

    /** DecimationMod.instance (f): Decimation's @Mod object, the key of its GUI handler. */
    public static Object decimationMod()
    {
        return deci.a.b.f;
    }

    /** Decimation's own IGuiHandler as FML holds it (NetworkRegistry's private serverGuiHandlers map). */
    @SuppressWarnings("unchecked")
    public static cpw.mods.fml.common.network.IGuiHandler decimationGuiHandler()
    {
        java.util.Map<cpw.mods.fml.common.ModContainer, cpw.mods.fml.common.network.IGuiHandler> m =
            cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(cpw.mods.fml.common.network.NetworkRegistry.class,
                                                                    cpw.mods.fml.common.network.NetworkRegistry.INSTANCE,
                                                                    "serverGuiHandlers");
        return m.get(cpw.mods.fml.common.FMLCommonHandler.instance().findContainerFor(decimationMod()));
    }

    /**
     * FilteredSlot.isItemValid (deci.f.k): whether part may go into slot deciIndex (WeaponModifyContainer's index) of this
     * gun, by Decimation's own rules (weapon category, slot, integral suppressor, integrated scope, paint for this gun,
     * forced tracer). Builds the slot pair on a throwaway inventory: call it on a list build, never per frame.
     */
    public static boolean fitsSlot(ItemStack gun, int deciIndex, ItemStack part)
    {
        if (gun == null || part == null || !isGun(gun.getItem()))
        {
            return false;
        }
        net.minecraft.inventory.InventoryBasic inv = new net.minecraft.inventory.InventoryBasic("gunsmith", false, 7);
        inv.setInventorySlotContents(0, gun.copy());
        deci.f.k parent = new deci.f.k(inv, 0, 0, 0, null);
        return new deci.f.k(inv, deciIndex, 0, 0, parent).isItemValid(part);
    }

    /**
     * One run of GunItem.clientHeldTick (d, private) for a held gun: the client's per tick gun work, where Decimation
     * turns the view by the player's recoil (dev test attachments calls it right after setting a known recoil).
     */
    public static void runClientHeldTick(ItemStack gun, net.minecraft.entity.player.EntityPlayer player) throws Exception
    {
        java.lang.reflect.Method m = deci.ay.i.class.getDeclaredMethod("d", ItemStack.class, net.minecraft.entity.player.EntityPlayer.class);
        m.setAccessible(true);
        m.invoke(gun.getItem(), gun, player);
    }

    /** The gun's grip slot attachment (GunItem.getGripAttachment, J), or null. */
    public static Item gripAttachment(ItemStack gun)
    {
        return gun == null || !(gun.getItem() instanceof deci.ay.i) ? null : ((deci.ay.i) gun.getItem()).J(gun);
    }

    /** GunItem.isIntegrallySuppressed (fL). */
    public static boolean isIntegrallySuppressed(Item gun)
    {
        return ((deci.ay.i) gun).fL();
    }

    /** GunItem.stats.forcedTracer (aeq.adA): the gun's tracer colour is fixed. */
    public static boolean hasForcedTracer(Item gun)
    {
        return ((deci.ay.i) gun).aeq.adA;
    }

    /** True for a weapon paint (WeaponSkinItem, deci.ay.j). */
    public static boolean isSkin(Item item)
    {
        return item instanceof deci.ay.j;
    }

    /** WeaponSkinItem.getSkinId (fS): what the gun's "skin" NBT holds. */
    public static String skinId(Item skin)
    {
        return ((deci.ay.j) skin).fS();
    }

    /** WeaponSkinItem.getSkinName (fT): the paint's English name ("skinEng"). */
    public static String skinName(Item skin)
    {
        return ((deci.ay.j) skin).fT();
    }

    /** True for tracer powder (TracerItem, deci.ay.k). */
    public static boolean isTracer(Item item)
    {
        return item instanceof deci.ay.k;
    }

    /** TracerItem.getColorId (fW): what the gun's "tracerColorID" NBT holds. */
    public static int tracerColorId(Item tracer)
    {
        return ((deci.ay.k) tracer).fW();
    }

    /** The paint applied to this gun: the SkinRegistry.allSkins (deci.aD.q.awF) entry for its gunName and "skin", or null. */
    public static Item skinItem(ItemStack gun)
    {
        if (gun == null || !isGun(gun.getItem()))
        {
            return null;
        }
        deci.ay.i g = (deci.ay.i) gun.getItem();
        String skin = g.A(gun);
        if (skin == null || skin.isEmpty())
        {
            return null;
        }
        for (deci.ay.j s : deci.aD.q.awF)
        {
            if (g.aer.equals(s.fR()) && skin.equals(s.fS()))
            {
                return s;
            }
        }
        return null;
    }

    private static java.util.Map<Integer, Item> tracerItems;

    /** The tracer powder of this colour id (TracerItem.getColorId), or null; the registry is read once. */
    public static Item tracerItem(int colorId)
    {
        if (tracerItems == null)
        {
            java.util.Map<Integer, Item> m = new java.util.HashMap<Integer, Item>();
            for (Object o : Item.itemRegistry)
            {
                if (o instanceof deci.ay.k)
                {
                    m.put(((deci.ay.k) o).fW(), (Item) o);
                }
            }
            tracerItems = m;
        }
        return tracerItems.get(colorId);
    }

    // ---- player data: PlayerData = deci.Q.b

    /** PlayerData.get(player).getAimMode() (e, ci): 1 = aiming down the sights. */
    public static int aimMode(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? 0 : data.ci();
    }

    /** PlayerData.get(player).isHolstered() (e, cg): the gun lowered by Decimation's holster key. */
    public static boolean holstered(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data != null && data.cg();
    }

    /** PlayerData.get(player).setAimMode(mode) (e, M). */
    public static void setAimMode(EntityPlayer player, int mode)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.M(mode);
        }
    }

    // ---- rendering: ClientRenderHandler = deci.c.b

    /**
     * Where the scope glass was last drawn on screen, window pixels (origin
     * bottom left), or null when not within the last 0.25 s. Fields added to
     * BModel (deci.n.f) by tools/patches/PatchScope.java.
     */
    public static float[] scopeGlassOnScreen()
    {
        if (System.nanoTime() - deci.n.f.glassTime > 250_000_000L)
        {
            return null;
        }
        return new float[] {deci.n.f.glassX, deci.n.f.glassY};
    }

    /** The last scope glass box on screen: min x, min y, max x, max y (window pixels, clipped to the screen). */
    public static float[] scopeGlassBox()
    {
        return new float[] {deci.n.f.glassMinX, deci.n.f.glassMinY, deci.n.f.glassMaxX, deci.n.f.glassMaxY};
    }

    /**
     * PatchScope measures a scope glass draw when (++glassFrame & mask) == 0, glassFrame counting EVERY glass draw (NPC
     * guns and HUD figures too): 0 = every draw while set, -1 = never. ScopeZoom sets 0 only around its own hand draw on
     * the frames it measures (a mask of 1 or 7 never hit the hand's draw where the glass is drawn an even number of times
     * a frame: the production client with OptiFine, 6).
     */
    public static void setScopeGlassEvery(int mask)
    {
        deci.n.f.glassMask = mask;
    }

    /** System.nanoTime() of the last scope glass sample (changes with every new sample). */
    public static long scopeGlassTime()
    {
        return deci.n.f.glassTime;
    }

    /** ClientRenderHandler.scopeTextureId (dC): the texture BModel draws on scope glass. */
    public static int scopeTexture()
    {
        return deci.c.b.dC;
    }

    // ---- player data, readable: Deci.player(p).bottlecaps() ...

    /** PlayerData of a player (deci.Q.b), wrapped; null when it has none yet. */
    public static Player player(EntityPlayer player)
    {
        deci.Q.b data = player == null ? null : deci.Q.b.e(player);
        return data == null ? null : new Player(data);
    }

    /** Readable view of Decimation's PlayerData (deci.Q.b). */
    public static final class Player
    {
        private final deci.Q.b data;

        private Player(deci.Q.b data)
        {
            this.data = data;
        }

        /** getBottlecaps (cb). */
        public long bottlecaps() { return data.cb(); }
        /** addBottlecaps (k). */
        public void addBottlecaps(long n) { data.k(n); }
        /** getGoldBottlecaps (ca). */
        public long goldBottlecaps() { return data.ca(); }
        /** addGoldBottlecaps (f). */
        public void addGoldBottlecaps(long n) { data.f(n); }
        /** maxBottlecaps (field Vo): the wallet limit, both kinds. */
        public long maxBottlecaps() { return (long) data.Vo; }
        /** getHumanity (cd). */
        public int humanity() { return data.cd(); }
        /** getRecoilPitch (cs): the gun's recoil still to turn the view up, applied a part each tick. */
        public float recoilPitch() { return data.cs(); }
        /** getRecoilYaw (ct): the sideways part. */
        public float recoilYaw() { return data.ct(); }
        /** setRecoilPitch (j) / setRecoilYaw (k). */
        public void setRecoil(float pitch, float yaw) { data.j(pitch); data.k(yaw); }
        /** addHumanity (I). */
        public void addHumanity(int n) { data.I(n); }
        /** removeHumanity (J). */
        public void removeHumanity(int n) { data.J(n); }
        /** getBounty (cc). */
        public long bounty() { return data.cc(); }
        /** getWater (bN) / setWater (v). */
        public int water() { return data.bN(); }
        public void setWater(int n) { data.v(n); }
        /** isIrradiated (co) / setIrradiated (i). */
        public boolean irradiated() { return data.co(); }
        public void setIrradiated(boolean b) { data.i(b); }
        /** isInScaryZone (cQ) / setInScaryZone (n). */
        public boolean inScaryZone() { return data.cQ(); }
        public void setInScaryZone(boolean b) { data.n(b); }
        /** getCombatTimer (cf) / setCombatTimer (l): ticks; a running timer ends safezone protection. */
        public long combatTimer() { return data.cf(); }
        public void setCombatTimer(long t) { data.l(t); }
        /** getSafezoneGraceTicks (dn): protection left after leaving a safezone. */
        public long safezoneGrace() { return data.dn(); }
        /** setSafezoneGraceTicks (T). */
        public void setSafezoneGrace(int t) { data.T(t); }
        /** isInfected (cm) / setInfected (f), isBleeding (cn) / setBleeding (g), hasBrokenLeg (cl) / setBrokenLeg (e). */
        public boolean infected() { return data.cm(); }
        public void setInfected(boolean b) { data.f(b); }
        public boolean bleeding() { return data.cn(); }
        public void setBleeding(boolean b) { data.g(b); }
        public boolean brokenLeg() { return data.cl(); }
        public void setBrokenLeg(boolean b) { data.e(b); }
        /** getCreativeFlag (cP) / setCreativeFlag (m), healFully (bH). */
        public boolean creativeFlag() { return data.cP(); }
        public void setCreativeFlag(boolean b) { data.m(b); }
        public void healFully() { data.bH(); }
        /** getBlood (bO): 0 .. 20. */
        public int blood() { return data.bO(); }
        /** getDaysSurvived (dj). */
        public long daysSurvived() { return data.dj(); }
        /** getClanName (cY): ClanManagerV1.UNKNOWN_CLAN ("unknown") when in none. */
        public String clanName() { return data.cY(); }
        /** In a clan: the name is not ClanManagerV1.UNKNOWN_CLAN (net.decimation.mod.server.clans.a). */
        public boolean inClan() { return !net.decimation.mod.server.clans.a.UNKNOWN_CLAN.equals(data.cY()); }
        /** getHumanityRankText (O): "Survivor", "Hero II", ... with its colour code in front. */
        public String humanityRankText() { return data.O(data.cd()); }
        /** getMask (cx): the mask slot (gas mask, goggles), or null. */
        public net.minecraft.item.ItemStack mask() { return data.cx(); }
        /** getTargetTimer (bA) in ticks, getTargetScore (bB), getTargetBestScore (bz): the target range run. */
        public int targetTimer() { return data.bA(); }
        public int targetScore() { return data.bB(); }
        public int targetBestScore() { return data.bz(); }
        /**
         * getFlagN (cF) / setFlagN (l): ModelDeciPlayer draws the player whole (head, body in place) instead of as the
         * first person body; Decimation's inventory screen sets it around its own player picture.
         */
        public boolean guiPose() { return data.cF(); }
        public void setGuiPose(boolean b) { data.l(b); }
    }

    // ---- server config: ServerConfig = deci.aJ.b (static fields)

    /** ServerConfig.enableItemPickup (aAG): when true the original pickup handler does nothing, nor do we. */
    public static boolean itemPickupFlag()
    {
        return deci.aJ.b.aAG;
    }

    /** ServerConfig.enableSupplyDrops (aAi). */
    public static boolean supplyDropsEnabled()
    {
        return deci.aJ.b.aAi;
    }

    /** ServerConfig.supplyDropCountdown (aAg): ticks until the next drop. */
    public static int supplyDropCountdown()
    {
        return deci.aJ.b.aAg;
    }

    public static void setSupplyDropCountdown(int ticks)
    {
        deci.aJ.b.aAg = ticks;
    }

    /** ServerConfig.supplyDropInterval (aAf). */
    public static int supplyDropInterval()
    {
        return deci.aJ.b.aAf;
    }

    /** ServerConfig.zoneList (aAc): Decimation's live zone list, may be null. */
    public static net.decimation.mod.server.zones.ObjectZoneList zoneList()
    {
        return deci.aJ.b.aAc;
    }

    /** Boxes {x1, z1, x2, z2} (x1 <= x2, z1 <= z2) of every live zone of a kind (Decimation's and ours). */
    public static java.util.List<int[]> zoneBoxes(net.decimation.worldgen.ZoneKind kind)
    {
        java.util.List<int[]> out = new java.util.ArrayList<int[]>();
        net.decimation.mod.server.zones.ObjectZoneList list = deci.aJ.b.aAc;
        if (list == null || list.zoneList == null)
        {
            return out;
        }
        for (net.decimation.mod.server.zones.ObjectZone z : list.zoneList)
        {
            if (isZone(z, kind))
            {
                out.add(new int[] {Math.min(z.zoneX1, z.zoneX2), Math.min(z.zoneZ1, z.zoneZ2),
                                   Math.max(z.zoneX1, z.zoneX2), Math.max(z.zoneZ1, z.zoneZ2)});
            }
        }
        return out;
    }

    public static void setZoneList(net.decimation.mod.server.zones.ObjectZoneList list)
    {
        deci.aJ.b.aAc = list;
    }

    // ---- zones: EnumZoneType = net.decimation.mod.server.zones.a, ZoneManager = zones.b

    /** Decimation's zone type for one of ours. */
    public static net.decimation.mod.server.zones.a zoneType(net.decimation.worldgen.ZoneKind kind)
    {
        return kind == null ? null : net.decimation.mod.server.zones.a.valueOf(kind.name());
    }

    /** True when the zone object is of this kind. */
    public static boolean isZone(net.decimation.mod.server.zones.ObjectZone zone, net.decimation.worldgen.ZoneKind kind)
    {
        return zone != null && zone.zoneType == zoneType(kind);
    }

    /** ZoneManager.isInfectedInZone (b.a(InfectedEntity, EnumZoneType)). */
    public static boolean isInfectedInZone(net.minecraft.entity.Entity infected, net.decimation.worldgen.ZoneKind kind)
    {
        return net.decimation.mod.server.zones.b.a((deci.ag.d) infected, zoneType(kind));
    }

    /** ZoneManager.isPlayerInZone (b.a(EntityPlayer, a)): false for SAFEZONE while the player's combat timer runs. */
    public static boolean isPlayerInZone(EntityPlayer player, net.decimation.worldgen.ZoneKind kind)
    {
        return net.decimation.mod.server.zones.b.a(player, zoneType(kind));
    }

    /** ZoneManager.isPlayerNearZone (b.b(EntityPlayer, a)): the box grown by 10 blocks. */
    public static boolean isPlayerNearZone(EntityPlayer player, net.decimation.worldgen.ZoneKind kind)
    {
        return net.decimation.mod.server.zones.b.b(player, zoneType(kind));
    }

    /** ZoneManager.isEntityInZone (b.a(Entity, a)). */
    public static boolean isEntityInZone(net.minecraft.entity.Entity e, net.decimation.worldgen.ZoneKind kind)
    {
        return net.decimation.mod.server.zones.b.a(e, zoneType(kind));
    }

    /** ZoneManager.isEntityNearZone (b.b(Entity, a)): the box grown by 10 blocks. */
    public static boolean isEntityNearZone(net.minecraft.entity.Entity e, net.decimation.worldgen.ZoneKind kind)
    {
        return net.decimation.mod.server.zones.b.b(e, zoneType(kind));
    }

    /** ServerConfig.enableIrradiation (aAn). */
    public static boolean irradiationEnabled()
    {
        return deci.aJ.b.aAn;
    }

    /** ServerConfig.enableSafezoneGrace (aAJ). */
    public static boolean safezoneGraceEnabled()
    {
        return deci.aJ.b.aAJ;
    }

    /** ServerConfig.enableWorldBorder (aAL). */
    public static boolean worldBorderEnabled()
    {
        return deci.aJ.b.aAL;
    }

    /**
     * Sends PacketZoneLists (deci.aE.a$ax) to one player: zone boxes "x1/y1/z1=x2/y2/z2" per type
     * (safezone, military, radiation, scary, police) and the border box text. The client replaces
     * its lists (ClientState bS..bW, ca) with them.
     */
    public static void sendZoneLists(net.minecraft.entity.player.EntityPlayerMP player, java.util.ArrayList<String> safe,
                                     java.util.ArrayList<String> military, java.util.ArrayList<String> radiation,
                                     java.util.ArrayList<String> scary, java.util.ArrayList<String> police, String border)
    {
        deci.aF.a.a.a.gB().sendTo(new deci.aE.a.ax(safe, military, radiation, scary, police, border), player);
    }

    /**
     * Sends PacketSetClientStringList (deci.aE.a$w) to one player: the client's turf list
     * (ClientState.bY, deci.b.i) becomes these entries "x1/y1/z1=x2/y2/z2=owner=name". IngameHud
     * shows "ENTERING LOCATION" + the name, the chat line "Now entering NAME [controlled by OWNER]"
     * (no owner part for "unknown") and the newturf sound when the player walks into one, "Now
     * entering the Wilderness" when out of every turf and safezone. Spaces are stripped by the
     * client: write them as underscores (PatchTurfNames shows them as spaces).
     */
    public static void sendTurfList(net.minecraft.entity.player.EntityPlayerMP player, java.util.ArrayList<String> turfs)
    {
        deci.aF.a.a.a.gB().sendTo(new deci.aE.a.w(turfs), player);
    }

    /** Decimation's own clan turfs (ServerConfig.turfList, dedicated servers) in the turf list format. */
    public static java.util.List<String> decimationTurfs()
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        net.decimation.mod.server.turf.ObjectTurfList list = deci.aJ.b.aAd;
        if (list == null || list.turfList == null)
        {
            return out;
        }
        for (net.decimation.mod.server.turf.ObjectTurf t : list.turfList)
        {
            out.add(t.posX1 + "/" + t.posY1 + "/" + t.posZ1 + "=" + t.posX2 + "/" + t.posY2 + "/" + t.posZ2 + "="
                    + t.currentClanName + "=" + t.turfName);
        }
        return out;
    }

    /** The client's safezone list (ClientState.bS, deci.b.i), filled by PacketZoneLists. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> clientSafezones()
    {
        return deci.b.i.bS;
    }

    /** The client's turf list (ClientState.bY), filled by PacketSetClientStringList. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> clientTurfs()
    {
        return deci.b.i.bY;
    }

    // ---- zone related entity kinds

    /** A trader NPC (TraderEntity deci.ai.a, guards included): never attackable in Decimation. */
    public static boolean isTrader(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ai.a;
    }

    /** An armed trader (ArmedTraderEntity deci.ai.v): guards, guns / ammo / armor / medical traders. */
    public static boolean isArmedTrader(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ai.v;
    }

    /**
     * A trader's offers: TraderEntity.OFFERS_BY_CLASS (abd) for its class, each TraderOffer (deci.ai.y) as its item and
     * {buyPrice abi, sellPrice abj, currency hW: 0 bottlecaps, 1 gold caps}; empty for anything else.
     */
    public static java.util.List<java.util.Map.Entry<ItemStack, int[]>> traderOffers(net.minecraft.entity.Entity trader)
    {
        java.util.List<java.util.Map.Entry<ItemStack, int[]>> out = new java.util.ArrayList<java.util.Map.Entry<ItemStack, int[]>>();
        java.util.List<deci.ai.y> list = trader == null ? null : deci.ai.a.abd.get(trader.getClass());
        for (int i = 0; list != null && i < list.size(); i++)
        {
            deci.ai.y o = list.get(i);
            out.add(new java.util.AbstractMap.SimpleEntry<ItemStack, int[]>(o.itemStack, new int[] {o.abi, o.abj, o.hW}));
        }
        return out;
    }

    /**
     * Client: one trade as the trader screen sends it (PacketTraderTransaction deci.aE.a$M; action 0 buy, 1 sell). The
     * server checks the price against the trader class's offers and gives `new ItemStack(item)` on a buy (count 1,
     * metadata 0, no NBT).
     */
    public static void sendTrade(int action, ItemStack item, int price, int traderEntityId, int currency)
    {
        deci.aF.a.a.a.gB().sendToServer(new deci.aE.a.M(action, item, price, traderEntityId, currency));
    }

    // ---- trader market (fixes/TraderMarket): TraderEntity.OFFERS_BY_CLASS = deci.ai.a.abd, TraderOffer = deci.ai.y
    //      (itemStack, buyPrice abi, sellPrice abj, currency hW), PacketTraderTransaction = deci.aE.a$M

    /** Every trader class's live offer list (a new trader object of a class puts a fresh list of base prices in). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.Map<Class<?>, java.util.List<Object>> traderOfferLists()
    {
        return (java.util.Map) deci.ai.a.abd;
    }

    /** A TraderOffer with these prices. */
    public static Object newTraderOffer(ItemStack stack, int buy, int sell, int currency)
    {
        return new deci.ai.y(stack, buy, sell, currency);
    }

    /** An offer's {buy, sell, currency}. */
    public static int[] offerPrices(Object offer)
    {
        deci.ai.y o = (deci.ai.y) offer;
        return new int[] {o.abi, o.abj, o.hW};
    }

    public static ItemStack offerStack(Object offer)
    {
        return ((deci.ai.y) offer).itemStack;
    }

    /** True for a TraderOffer object. */
    public static boolean isTraderOffer(Object o)
    {
        return o instanceof deci.ai.y;
    }

    /** The trader shown by the trader screen (TraderScreen.currentTrader deci.g.q.hc), client only. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.entity.Entity traderOnScreen(Object gui)
    {
        return gui instanceof deci.g.q ? deci.g.q.hc : null;
    }

    private static java.lang.reflect.Field tradeAction, tradeStack, tradePrice, tradeTrader, tradeCurrency;

    /** A trade message (PacketTraderTransaction deci.aE.a$M: id, stack, caps, ayt, hW) as {action (0 buy, 1 sell), stack,
     *  price, trader entity id, currency (0 caps, 1 gold)}, null for any other message. */
    public static Object[] tradeMessage(Object msg)
    {
        if (!(msg instanceof deci.aE.a.M))
        {
            return null;
        }
        try
        {
            if (tradeAction == null)
            {
                tradeStack = deci.aE.a.M.class.getDeclaredField("stack");
                tradePrice = deci.aE.a.M.class.getDeclaredField("caps");
                tradeTrader = deci.aE.a.M.class.getDeclaredField("ayt");
                tradeCurrency = deci.aE.a.M.class.getDeclaredField("hW");
                tradeStack.setAccessible(true);
                tradePrice.setAccessible(true);
                tradeTrader.setAccessible(true);
                tradeCurrency.setAccessible(true);
                java.lang.reflect.Field a = deci.aE.a.M.class.getDeclaredField("id");
                a.setAccessible(true);
                tradeAction = a;
            }
            return new Object[] {tradeAction.getInt(msg), tradeStack.get(msg), tradePrice.getInt(msg), tradeTrader.getInt(msg),
                                 tradeCurrency.getInt(msg)};
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    /** Decimation's network channel of one side (its SimpleNetworkWrapper deci.aF.a$a$a.gB(), private channels map). */
    @SuppressWarnings("unchecked")
    public static io.netty.channel.Channel decimationChannel(cpw.mods.fml.relauncher.Side side)
    {
        try
        {
            java.lang.reflect.Field f = cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper.class.getDeclaredField("channels");
            f.setAccessible(true);
            return ((java.util.Map<cpw.mods.fml.relauncher.Side, io.netty.channel.Channel>) f.get(deci.aF.a.a.a.gB())).get(side);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    /** A civilian human: FactionHumanEntity (deci.ah.d) that is neither a bandit kind (deci.ag.a) nor a trader. */
    public static boolean isCivilian(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ah.d && !(e instanceof deci.ag.a) && !(e instanceof deci.ai.a);
    }

    /** A trap Decimation keeps away from safezones: landmine, campfire, punji sticks, barbed wire (deci.aj.c / b / e / a). */
    public static boolean isTrap(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.aj.c || e instanceof deci.aj.b || e instanceof deci.aj.e || e instanceof deci.aj.a;
    }

    /** A projectile Decimation keeps away from safezones: crossbow bolt, rocket, grenade, mortar round (deci.ak.b / f / c / e). */
    public static boolean isSafezoneProjectile(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ak.b || e instanceof deci.ak.f || e instanceof deci.ak.c || e instanceof deci.ak.e;
    }

    /** A frag grenade in flight (GrenadeEntity deci.ak.c). */
    public static boolean isGrenade(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ak.c;
    }

    /**
     * Throws the held throwable as Decimation's throw packet does on the server (PacketThrowItem's handler,
     * deci.aE.a$y$a: GrenadeItem.throwGrenade, deci.aA.a.a, on the held stack); false when nothing throwable is held.
     */
    public static boolean throwHeld(EntityPlayer player, double strength)
    {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof deci.aA.a))
        {
            return false;
        }
        ((deci.aA.a) held.getItem()).a(held, player.worldObj, player, Math.min(Math.max(strength, 0.0), 10.0));
        return true;
    }

    /** A mech (MechEntity deci.ag.k). */
    public static boolean isMech(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ag.k;
    }

    // ---- registry: ItemRegistry = deci.aD.k, BlockRegistry = deci.aD.c, DamageSources = deci.aD.h

    /** ItemRegistry.itemBottlecap (aln). */
    public static Item bottlecap()
    {
        return deci.aD.k.aln;
    }

    /** ItemRegistry.itemBottlecapGold (alo). */
    public static Item goldBottlecap()
    {
        return deci.aD.k.alo;
    }

    /** BlockRegistry.supplyDrop (afA): the falling supply crate block. */
    public static net.minecraft.block.Block supplyDropBlock()
    {
        return deci.aD.c.afA;
    }

    /** DamageSources.human (alh): NPC gunfire. */
    public static net.minecraft.util.DamageSource humanDamage()
    {
        return deci.aD.h.alh;
    }

    /** GunDamageSource.forShooter (deci.ab.a.c): a player's gunshot. */
    public static net.minecraft.util.DamageSource gunDamage(net.minecraft.entity.Entity shooter)
    {
        return deci.ab.a.c(shooter);
    }

    // ---- multiblocks: MultiblockPart = deci.W.a, IMultiblockD = deci.W.d, MultiblockHelper = deci.W.e

    /** A tile entity that is part of a multiblock (deci.W.a). */
    public static boolean isMultiblockPart(net.minecraft.tileentity.TileEntity te)
    {
        return te instanceof deci.W.a;
    }

    public static boolean isMultiblockMaster(net.minecraft.tileentity.TileEntity te)
    {
        return te instanceof deci.W.a && ((deci.W.a) te).isMaster();
    }

    public static void setMultiblockSelfMaster(net.minecraft.tileentity.TileEntity te)
    {
        ((deci.W.a) te).setSelfMaster();
    }

    /** Multiblock size {x, y, z} (IMultiblockD), or null for any other tile entity. */
    public static int[] multiblockSize(net.minecraft.tileentity.TileEntity te)
    {
        if (!(te instanceof deci.W.d))
        {
            return null;
        }
        deci.W.d d = (deci.W.d) te;
        return new int[] {d.getSizeX(), d.getSizeY(), d.getSizeZ()};
    }

    /** MultiblockHelper.a(World, x, y, z, Block, meta): builds every part around a master at x, y, z. */
    public static void buildMultiblock(net.minecraft.world.World world, int x, int y, int z,
                                       net.minecraft.block.Block block, int meta)
    {
        deci.W.e.a(world, x, y, z, block, meta);
    }

    // ---- entities

    /** A vehicle body (VehicleEntity deci.ad.e) or one of its seat / hitbox parts (deci.ad.b). */
    public static boolean isVehicle(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ad.e || e instanceof deci.ad.b;
    }

    /** A new HummerEntity (deci.ad.i). */
    public static net.minecraft.entity.Entity newHummer(net.minecraft.world.World world, double x, double y, double z)
    {
        return new deci.ad.i(world, x, y, z);
    }

    /** EntityCorpse (deci.ae.a) as DeathHandler makes it on a player's death: owner, death time, items (addItem g). */
    public static net.minecraft.entity.Entity newCorpse(net.minecraft.world.World world, double x, double y, double z,
                                                         String ownerUuid, net.minecraft.item.ItemStack... items)
    {
        deci.ae.a c = new deci.ae.a(world);
        c.setPositionAndRotation(x, y, z, 0, 0);
        c.r(world.getTotalWorldTime());
        c.E(ownerUuid);
        for (net.minecraft.item.ItemStack s : items)
        {
            c.g(s);
        }
        return c;
    }

    /** True for Decimation's player corpse (EntityCorpse, deci.ae.a). */
    public static boolean isCorpse(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ae.a;
    }

    /** VehicleEntity.driverSeat (YO): the seat whose rider drives; null until the vehicle's first server tick. */
    public static net.minecraft.entity.Entity vehicleDriverSeat(net.minecraft.entity.Entity vehicle)
    {
        return vehicle instanceof deci.ad.e ? ((deci.ad.e) vehicle).YO : null;
    }

    /** VehicleEntity.getSpeed (dZ): blocks per tick, negative in reverse. */
    public static float vehicleSpeed(net.minecraft.entity.Entity vehicle)
    {
        return vehicle instanceof deci.ad.e ? ((deci.ad.e) vehicle).dZ() : 0;
    }

    /** VehicleEntity.isEngineOn (ea): a player drives it with fuel (a jerry can holding fuel in the hotbar). */
    public static boolean vehicleEngineOn(net.minecraft.entity.Entity vehicle)
    {
        return vehicle instanceof deci.ad.e && ((deci.ad.e) vehicle).ea();
    }

    /** VehicleEntity.getDespawnSecondsLeft (dU): 900 minus the seconds since its last driver (it goes at 600). */
    public static long vehicleDespawnSecondsLeft(net.minecraft.entity.Entity vehicle)
    {
        return vehicle instanceof deci.ad.e ? ((deci.ad.e) vehicle).dU() : -1;
    }

    /** ItemRegistry.itemJerryCan (amy): vehicle and chainsaw fuel. */
    public static Item jerryCan()
    {
        return deci.aD.k.amy;
    }

    /** DescribedItem.getAmount (deci.ao.b p): the "amount" of a jerry can and the like; -1 for other items. */
    public static int itemAmount(net.minecraft.item.ItemStack stack)
    {
        return stack != null && stack.getItem() instanceof deci.ao.b ? ((deci.ao.b) stack.getItem()).p(stack) : -1;
    }

    /** InfectedEntity (deci.ag.d). */
    public static boolean isInfected(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ag.d;
    }

    /** Exactly an InfectedEntity (deci.ag.d), not one of its special subclasses (crawler, hulk, ...). */
    public static boolean isPlainInfected(net.minecraft.entity.Entity e)
    {
        return e != null && e.getClass() == deci.ag.d.class;
    }

    /** A landmine (LandmineEntity, deci.aj.c) lying at (x, y, z): goes off (power 4, no block damage) under a player. */
    public static net.minecraft.entity.Entity newLandmine(net.minecraft.world.World world, double x, double y, double z)
    {
        return new deci.aj.c(world, x, (float) y, z);
    }

    public static net.minecraft.entity.EntityLiving newInfected(net.minecraft.world.World world)
    {
        return new deci.ag.d(world);
    }

    /** InfectedEntity.setVariant (af) to InfectedVariant MILITARY / POLICE / ... (deci.am.b) by name. */
    public static void setInfectedVariant(net.minecraft.entity.Entity infected, String variant)
    {
        ((deci.ag.d) infected).af(deci.am.b.valueOf(variant).id);
    }

    /** InfectedEntity.getVariant (eI): 0 common, 1 military, 2 police. */
    public static int infectedVariant(net.minecraft.entity.Entity infected)
    {
        return ((deci.ag.d) infected).eI();
    }

    /** EntityFallingSupplyDrop (deci.ac.a): a supply crate still in the air. */
    public static boolean isFallingSupplyDrop(Object e)
    {
        return e instanceof deci.ac.a;
    }

    /** A new BanditEntity (deci.ag.a). */
    public static net.minecraft.entity.EntityLiving newBandit(net.minecraft.world.World world)
    {
        return new deci.ag.a(world);
    }

    /**
     * Which armed human an entity is (most specific class first): BanditEntity
     * deci.ag.a, its subclasses SoldierEntity deci.ag.l, HazmatSoldierEntity
     * deci.ag.c, SovietEntity deci.ag.m. Null for anything else.
     */
    public static NpcKind npcKind(net.minecraft.entity.Entity e)
    {
        if (e instanceof deci.ag.c) return NpcKind.HAZMAT;
        if (e instanceof deci.ag.l) return NpcKind.SOLDIER;
        if (e instanceof deci.ag.m) return NpcKind.SOVIET;
        if (e instanceof deci.ag.a) return NpcKind.BANDIT;
        return null;
    }

    /**
     * A human: a player, an armed human NPC (npcKind), a civilian (HumanEntity deci.af.d,
     * FactionHumanEntity deci.ah.d) or a trader (TraderEntity deci.ai.a and its kinds).
     */
    public static boolean isHuman(net.minecraft.entity.Entity e)
    {
        if (isInfectedKind(e))
        {
            return false;            // InfectedEntity extends Decimation's human entity class
        }
        return e instanceof net.minecraft.entity.player.EntityPlayer || npcKind(e) != null
            || e instanceof deci.af.d || e instanceof deci.ah.d || e instanceof deci.ai.a;
    }

    /** Any infected: InfectedEntity deci.ag.d and its special kinds, and the infected dog deci.ag.g. */
    public static boolean isInfectedKind(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ag.d || e instanceof deci.ag.g;
    }

    // ---- main menu / HUD state (client): ClientState deci.b.i, BackendProfileCache deci.aO.b,
    // DeciConstants deci.Q.c

    /** The session check result (ClientState.bv): false = "Play offline" and the invalid session banner. */
    /** UpdateChecker.isUpdateRequired (net.decimation.mod.common.utils.h.gI): false since PatchNoUpdate. */
    public static boolean updateRequired()
    {
        return net.decimation.mod.common.utils.h.gI();
    }

    /**
     * Decimation's question box (ConfirmScreen deci.d.s) over `parent`: the lines centred in a dark box, the left button
     * (red, id 10) and the right one (id 11). A click or a click outside the box returns to `parent`; catch the buttons
     * with Forge's ActionPerformedEvent.Pre. `kind` goes back only to a parent that is an IPanelCallback (deci.d.t).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen confirm(int kind, net.minecraft.client.gui.GuiScreen parent, String[] lines,
                                                             String left, String right)
    {
        deci.d.s box = new deci.d.s(deci.a.b.a(), kind, parent).b(lines);
        box.c(left, right);
        return box;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isConfirm(net.minecraft.client.gui.GuiScreen s)
    {
        return s instanceof deci.d.s;
    }

    /** The lines of a question box (tests). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> confirmLines(net.minecraft.client.gui.GuiScreen s)
    {
        return s instanceof deci.d.s && ((deci.d.s) s).go != null ? java.util.Arrays.asList(((deci.d.s) s).go)
                                                                  : java.util.Collections.<String>emptyList();
    }

    /** Decimation's update page (GuiMenuUpdates deci.i.l); UpdateProgress opens the progress screen in its place. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen updatesPage()
    {
        return new deci.i.l(deci.a.b.a());
    }

    /** Decimation's Settings page (GuiMenuOptions deci.i.f; its View Updates button is 1000). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen optionsMenu(net.minecraft.client.gui.GuiScreen parent)
    {
        return new deci.i.f(deci.a.b.a(), parent, net.minecraft.client.Minecraft.getMinecraft().gameSettings);
    }

    public static void setSessionValid(boolean valid)
    {
        deci.b.i.bv = valid;
    }

    /** The local player's UUID as the menu looks it up, or null without a session. */
    public static String sessionUuid()
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc == null || mc.getSession() == null || mc.getSession().func_148256_e() == null
            || mc.getSession().func_148256_e().getId() == null)
        {
            return null;
        }
        return mc.getSession().func_148256_e().getId().toString();
    }

    /** The profile the menu and HUD show (BackendProfileCache.profile, filled by the dead backend before). */
    public static void setLocalProfile(String uuid, long humanKills, long deaths, long infectedKills)
    {
        net.decimation.mod.utilities.net.client_network.api.objects.ObjectPlayerProfile p = deci.aO.b.aBV;
        if (p == null || !uuid.equals(p.playerUUID))
        {
            p = new net.decimation.mod.utilities.net.client_network.api.objects.ObjectPlayerProfile(uuid);
            deci.aO.b.aBV = p;
        }
        p.playerKills = humanKills;
        p.playerDeaths = deaths;
        p.zombieKills = infectedKills;
    }

    /** {session valid, profile kills, deaths, infected kills, total humans, total infected} (dev test). */
    public static long[] menuState()
    {
        net.decimation.mod.utilities.net.client_network.api.objects.ObjectPlayerProfile p = deci.aO.b.aBV;
        return new long[] {deci.b.i.bv ? 1 : 0, p == null ? -1 : p.playerKills, p == null ? -1 : p.playerDeaths,
                           p == null ? -1 : p.zombieKills, deci.Q.c.Xj, deci.Q.c.Xk};
    }

    // ---- player skins (fixes/PlayerSkins)

    /** EntityCorpse.getOwnerUUID (ef): the dead player's server id as text, sent with the spawn; null for none. */
    public static String corpseOwner(net.minecraft.entity.Entity corpse)
    {
        return ((deci.ae.a) corpse).ef();
    }

    /** DecimationMod.fakePlayerProfile (B) .getPlayer (an): the player drawn in the HUD stats box and the profile page. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static EntityPlayer menuPreviewPlayer()
    {
        deci.e.b preview = deci.a.b.a().B;
        return preview == null ? null : preview.an();
    }

    // ---- radiation (fixes/RadiationDose, fixes/RadiationHud)

    /** PlayerData.isIrradiated (co): Decimation's flag while the player stands in a RADIATION zone. */
    public static boolean irradiated(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data != null && data.co();
    }

    /**
     * PlayerData.getMask (cx) is a gas mask (ItemRegistry gasmask apo, gasmaskmilitary app): Decimation's protection; since
     * 22 Oktober 2026 only while its filter has life left (fixes/GasMaskFilter).
     */
    public static boolean wearsGasMask(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        ItemStack mask = data == null ? null : data.cx();
        return isGasMask(mask) && GasMaskFilter.left(player, mask) > 0;
    }

    /** PlayerData.getVest (cw): the vest slot's stack, or null (fixes/ArmorGunfireHandler, feature 10). */
    public static ItemStack playerVest(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? null : data.cw();
    }

    /** ItemRegistry.gasmask (apo): Decimation's gas mask. */
    public static Item gasMask()
    {
        return deci.aD.k.apo;
    }

    /** PlayerData.setMask (e): the player's mask slot (gas mask, goggles), null for none. */
    public static void setPlayerMask(EntityPlayer player, ItemStack mask)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.e(mask);
        }
    }

    /** DamageSources.irradiated (akW): "irradiatedDeci", bypasses armor. */
    public static net.minecraft.util.DamageSource irradiatedDamage()
    {
        return deci.aD.h.akW;
    }

    /** ClientState.cl: the HUD's status icons (food, water, blood, health) hidden. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean hudIconsHidden()
    {
        return deci.b.i.cl;
    }

    /**
     * How far IngameHud.render (deci.g.j.b) moves its status icons down: 0 with the stats list hidden (ClientState.cf),
     * else 10 a line plus 10. The lines are counted as it adds them: kills, zombie kills, deaths, days, humanity always;
     * clan (not ClanManagerV1.UNKNOWN_CLAN), bottlecaps, gold bottlecaps when above 0; high score, score and timer while
     * the target timer runs (bA); range and a blank line with a range finder (deci.av.t) in hand; irradiated (co),
     * bleeding (cn), infected (cm), broken leg (cl).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int hudIconsDown(EntityPlayer player)
    {
        if (deci.b.i.cf)
        {
            return 0;
        }
        deci.Q.b d = deci.Q.b.e(player);
        if (d == null)
        {
            return 0;
        }
        int lines = 5;
        String clan = d.cY();
        lines += clan == null || clan.equals(net.decimation.mod.server.clans.a.UNKNOWN_CLAN) ? 0 : 1;
        lines += d.cb() > 0L ? 1 : 0;
        lines += d.ca() > 0L ? 1 : 0;
        lines += d.bA() > 0 ? 3 : 0;
        ItemStack held = player.getHeldItem();
        lines += held != null && held.getItem() instanceof deci.av.t ? 2 : 0;
        lines += (d.co() ? 1 : 0) + (d.cn() ? 1 : 0) + (d.cm() ? 1 : 0) + (d.cl() ? 1 : 0);
        return 10 + lines * 10;
    }

    /** The screen is Decimation's inventory and loot screen (PlayerInventoryScreen deci.g.g): its HUD is not drawn. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isInventoryScreen(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.g.g;
    }

    /** RenderUtil.drawTextureOpaque (deci.j.a.c): a HUD picture the way Decimation draws its status icons. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void drawHudTexture(double x, double y, net.minecraft.util.ResourceLocation texture, double w, double h)
    {
        deci.j.a.c(x, y, texture, w, h);
    }

    /** RenderUtil.drawOutlined (deci.j.a.e): text with a black outline, as the status percentages. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void drawOutlined(String text, int x, int y, int colour)
    {
        deci.j.a.e(text, x, y, colour);
    }

    /** The in game HUD's stats list on (ClientState.cf false; Decimation's HUD toggle). */
    public static void showHudStats()
    {
        deci.b.i.cf = false;
    }

    /**
     * Decimation's HUD switches (ClientState): cf hides the stats list and the player figure behind it (its "cancel gui"
     * key, O), cl is its unused old GUI (config enableOldGui): vanilla hearts, armor, food and experience stay, and the
     * big food / water / blood / health icons go. {cf, cl}.
     */
    public static boolean[] hudSwitches()
    {
        return new boolean[] {deci.b.i.cf, deci.b.i.cl};
    }

    public static void setHudSwitches(boolean hideStats, boolean oldGui)
    {
        deci.b.i.cf = hideStats;
        deci.b.i.cl = oldGui;
    }

    /** Decimation's 9 x 9 water and blood icons (ClientResources ab, ac: 32 x 32 sheets; empty at u 0, full 9, half 18). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.util.ResourceLocation[] waterBloodIcons()
    {
        return new net.minecraft.util.ResourceLocation[] {deci.b.c.ab, deci.b.c.ac};
    }

    /** Decimation's range finder (ItemRangeFinder deci.av.t): its HUD shows the distance to the block looked at. */
    public static boolean isRangeFinder(net.minecraft.item.Item item)
    {
        return item instanceof deci.av.t;
    }

    /** Decimation's Settings page (GuiMenuOptions deci.i.f). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isOptionsMenu(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.i.f;
    }

    /**
     * Decimation's home menu screen (GuiMenuHome deci.i.e), e.g. for a dev test picture. Client
     * only: verifying it on a dedicated server loads GuiScreen, and Deci failed to load there.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen homeMenu()
    {
        return new deci.i.e(deci.a.b.a());
    }

    /** True for Decimation's home menu (GuiMenuHome deci.i.e): the main menu, and in a world the pause menu. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isHomeMenu(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.i.e;
    }

    /**
     * Adds one of Decimation's own news panels (ArticlePanel deci.d.c, 120 x 70: title, one small line, a picture
     * under them, red on hover, a click opens `url`) to a menu screen (DeciScreen.addPanel, deci.d.i.a). `picture`
     * is the key of its picture in ArticlePanel's picture map (newsPicture): a key missing there is downloaded as a
     * URL every frame, so always put it first. Returns the panel for newsLine.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static Object addNewsPanel(net.minecraft.client.gui.GuiScreen screen, int x, int y, String title, String line,
                                      String url, String picture)
    {
        deci.d.c panel = new deci.d.c(1, x, y, (deci.d.i) screen)
        {
            @Override
            public void o(String address) // ArticlePanel.openUrl: our in-game addresses open a screen, not the browser
            {
                if (!net.decimation.fixes.MenuNews.open(address))
                {
                    super.o(address);
                }
            }
        };
        panel.eu = title;
        panel.ev = line;
        panel.ew = url;
        panel.ex = picture;
        ((deci.d.i) screen).a(panel);
        return panel;
    }

    /**
     * Decimation's menu shows its buttons and panels: the welcome box is closed (ClientState.bO) and the loading fade
     * is over (ClientState.isMenuFadedOut, deci.b.i.F).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean menuShown()
    {
        return deci.b.i.bO && deci.b.i.F();
    }

    /** Which of Decimation's three menu backgrounds this launch shows (ClientState.bC, 0 to 2, random per launch). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int menuBackground()
    {
        return deci.b.i.bC;
    }

    /** RenderUtil.drawTextureCentered (deci.j.a.a): a picture `zoom` times its size, as the menu background draws it. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void drawCentered(double x, double y, net.minecraft.util.ResourceLocation texture, float w, float h, float zoom)
    {
        deci.j.a.a(x, y, texture, w, h, zoom);
    }

    /** RenderUtil.drawTextureCenteredAlpha (deci.j.a.a): the same with an alpha; leaves the GL colour at it. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void drawCentered(double x, double y, net.minecraft.util.ResourceLocation texture, float w, float h, float zoom,
                                    double alpha)
    {
        deci.j.a.a(x, y, texture, w, h, zoom, alpha);
    }

    /** One of Decimation's own menu buttons (DeciMenuButton deci.d.f): black, the label, `tooltip` on hover. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiButton menuButton(int id, int x, int y, int w, int h, String label, String tooltip)
    {
        return new deci.d.f(id, x, y, w, h, label, tooltip, java.awt.Color.black);
    }

    /** The same in a colour (DeciMenuButton.setColor, deci.d.f.d: -44976 is the red of Decimation's question box). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiButton menuButton(int id, int x, int y, int w, int h, String label, String tooltip,
                                                                int colour)
    {
        return new deci.d.f(id, x, y, w, h, label, tooltip, java.awt.Color.black).d(colour);
    }

    /** The addresses of the menu's two red text links top right (ClientResources deci.b.c.aZ and bb, once Discord and Twitter). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static String[] menuLinks()
    {
        return new String[] {String.valueOf(deci.b.c.aZ), String.valueOf(deci.b.c.bb), String.valueOf(deci.b.c.bc)};
    }

    /** What a click on the index-th news panel does (ArticlePanel.openUrl with its address; tests). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void openNewsPanel(net.minecraft.client.gui.GuiScreen screen, int index)
    {
        try
        {
            java.lang.reflect.Field f = deci.d.i.class.getDeclaredField("eX"); // protected panel list
            f.setAccessible(true);
            int n = 0;
            for (Object o : (java.util.List<?>) f.get(screen))
            {
                if (o instanceof deci.d.c && n++ == index)
                {
                    ((deci.d.c) o).o(((deci.d.c) o).ew);
                    return;
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            // no panels
        }
    }

    /** Changes the small line under a news panel's title (addNewsPanel). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void newsLine(Object panel, String line)
    {
        ((deci.d.c) panel).ev = line;
    }

    /** The picture news panels show for `key` (ArticlePanel's static picture map, deci.d.c.et). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void newsPicture(String key, net.minecraft.util.ResourceLocation texture)
    {
        deci.d.c.et.put(key, texture);
    }

    /** Title and line of every news panel on a menu screen (DeciScreen's panel list eX), for dev tests. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> newsPanels(net.minecraft.client.gui.GuiScreen screen)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try
        {
            java.lang.reflect.Field f = deci.d.i.class.getDeclaredField("eX"); // protected panel list
            f.setAccessible(true);
            for (Object o : (java.util.List<?>) f.get(screen))
            {
                if (o instanceof deci.d.c)
                {
                    out.add(((deci.d.c) o).eu + " | " + ((deci.d.c) o).ev + " | " + ((deci.d.c) o).ew);
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            out.add("unreadable: " + e);
        }
        return out;
    }

    /**
     * One row of Decimation's official server list (ServerListEntry deci.k.a): `name` on the row, `label` in brackets
     * on the line under it, `host`:`port` pinged for its state, `map` the key of its pictures
     * (textures/gui/menu/serverbackgrounds/<map>0..7.png, faded in behind the page while the row is hovered).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static Object officialServer(String id, String name, String host, int port, String label, String map)
    {
        return new deci.k.a(id, name, host, port, label, map).y(map);
    }

    /**
     * The rows of the official server list page (GuiMenuOfficialServers deci.i.i, its ScrollPanel iW, rows ListElement
     * deci.d.p) as "name | state | map", state pinging, offline (drawn "Coming soon") or online; for dev tests.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> officialRows(net.minecraft.client.gui.GuiScreen screen)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (deci.d.p row : officialRowList(screen))
        {
            try
            {
                java.lang.reflect.Field f = deci.d.p.class.getDeclaredField("fF"); // private final ServerListEntry
                java.lang.reflect.Method pinging = deci.d.p.class.getDeclaredMethod("af"); // private isPinging
                java.lang.reflect.Method offline = deci.d.p.class.getDeclaredMethod("ag"); // private isOffline
                f.setAccessible(true);
                pinging.setAccessible(true);
                offline.setAccessible(true);
                String state = (Boolean) pinging.invoke(row) ? "pinging" : (Boolean) offline.invoke(row) ? "offline" : "online";
                out.add(((deci.k.a) f.get(row)).getMotd() + " | " + state + " | " + (row.fI ? row.fJ : ""));
            }
            catch (ReflectiveOperationException e)
            {
                out.add("unreadable: " + e);
            }
        }
        return out;
    }

    /** Shows row `index`'s picture `picture` behind the official server list as a hover does (ListElement fH, fK). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void showRowPicture(net.minecraft.client.gui.GuiScreen screen, int index, int picture)
    {
        java.util.List<deci.d.p> rows = officialRowList(screen);
        if (index < rows.size())
        {
            rows.get(index).fH = 1.0f;
            rows.get(index).fK = picture;
        }
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static java.util.List<deci.d.p> officialRowList(net.minecraft.client.gui.GuiScreen screen)
    {
        java.util.List<deci.d.p> rows = new java.util.ArrayList<deci.d.p>();
        if (!(screen instanceof deci.i.i))
        {
            return rows;
        }
        try
        {
            java.lang.reflect.Field f = deci.i.i.class.getDeclaredField("iW"); // private final ScrollPanel serverListPanel
            f.setAccessible(true);
            for (Object o : ((deci.d.l) f.get(screen)).fl)
            {
                if (o instanceof deci.d.p)
                {
                    rows.add((deci.d.p) o);
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            // no rows
        }
        return rows;
    }

    /**
     * The rows of the server status page (GuiMenuServerStatus deci.i.h: iS, iT, iU, iV, each a ServerPinger deci.l.a)
     * as "name | description | status" without colour codes; for dev tests.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> statusRows(net.minecraft.client.gui.GuiScreen screen)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (screen instanceof deci.i.h)
        {
            deci.i.h page = (deci.i.h) screen;
            for (deci.l.a row : new deci.l.a[] {page.iS, page.iT, page.iU, page.iV})
            {
                out.add(row.name + " | " + row.description + " | "
                        + net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(row.getStatus()));
            }
        }
        return out;
    }

    /** True for any of Decimation's menu pages (GuiMenuBase deci.i.a): home and pause menu, server pages, settings. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isMenuPage(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.i.a;
    }

    /** True for Decimation's Server Status page (GuiMenuServerStatus deci.i.h). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isStatusPage(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.i.h;
    }

    /**
     * Sets row `row` (0 to 3) of the Server Status page to state `state` in the order of ServerPinger's PingStatus
     * (deci.l.a$a: 0 Pinging..., 1 Offline, 2 Online, 3 its Unknown, which PatchMenuTexts draws "Coming soon").
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void setStatus(net.minecraft.client.gui.GuiScreen screen, int row, int state)
    {
        if (screen instanceof deci.i.h)
        {
            deci.i.h page = (deci.i.h) screen;
            deci.l.a[] rows = {page.iS, page.iT, page.iU, page.iV};
            rows[row].jv = deci.l.a.a.values()[state];
        }
    }

    /** Decimation's Community page (GuiMenuCommunityServers deci.i.j) on these rows (ServerListEntry deci.k.a). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen communityScreen(java.util.List<Object> rows)
    {
        java.util.List<deci.k.a> l = new java.util.ArrayList<deci.k.a>();
        for (Object o : rows)
        {
            l.add((deci.k.a) o);
        }
        return new deci.i.j(deci.a.b.a(), l, false);
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isCommunityScreen(net.minecraft.client.gui.GuiScreen s)
    {
        return s instanceof deci.i.j;
    }

    /** True for Decimation's official server list page (GuiMenuOfficialServers deci.i.i). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isOfficialScreen(net.minecraft.client.gui.GuiScreen s)
    {
        return s instanceof deci.i.i;
    }

    /** One community row on a local port (pinged and joined like a server), the Chernarus pictures on hover. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static Object communityRow(String name, int port, String label)
    {
        return officialServer("community", name, "127.0.0.1", port, label, "chernarus");
    }

    /** The page's ScrollPanel (GuiMenuCommunityServers.serverListPanel iW, private final). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static deci.d.l communityPanel(net.minecraft.client.gui.GuiScreen s)
    {
        try
        {
            java.lang.reflect.Field f = deci.i.j.class.getDeclaredField("iW");
            f.setAccessible(true);
            return (deci.d.l) f.get(s);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    /** Replaces the page's rows (ScrollPanel.setElements) with ListElements titled by us (not "Community Server"). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void setCommunityRows(net.minecraft.client.gui.GuiScreen s, java.util.List<Object> rows, java.util.List<String> titles)
    {
        java.util.List<deci.d.p> el = new java.util.ArrayList<deci.d.p>();
        for (int i = 0; i < rows.size(); i++)
        {
            el.add(new deci.d.p(0.5f, titles.get(i), (deci.k.a) rows.get(i), false));
        }
        deci.d.l panel = communityPanel(s);
        if (panel != null)
        {
            panel.a(el.iterator());
        }
    }

    /** The rows as "name | caption" (ServerListEntry name, ListElement fX) in order; for dev tests. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> communityRowTexts(net.minecraft.client.gui.GuiScreen s)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        deci.d.l panel = s instanceof deci.i.j ? communityPanel(s) : null;
        if (panel == null)
        {
            return out;
        }
        for (Object o : panel.fl)
        {
            if (!(o instanceof deci.d.p))
            {
                continue;
            }
            try
            {
                java.lang.reflect.Field f = deci.d.p.class.getDeclaredField("fX");
                java.lang.reflect.Field e = deci.d.p.class.getDeclaredField("fF"); // its ServerListEntry
                f.setAccessible(true);
                e.setAccessible(true);
                out.add(((deci.k.a) e.get(o)).getMotd() + " | " + f.get(o)); // getMotd is the name shown, getName the id
            }
            catch (ReflectiveOperationException e)
            {
                out.add("unreadable");
            }
        }
        return out;
    }

    /** The row under the mouse: Decimation raises a hovered row's fade fH toward 1 (ListElement); -1 when none is over 0.5. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int hoveredCommunityRow(net.minecraft.client.gui.GuiScreen s)
    {
        deci.d.l panel = s instanceof deci.i.j ? communityPanel(s) : null;
        if (panel == null)
        {
            return -1;
        }
        int best = -1, i = 0;
        float top = 0.5f;
        for (Object o : panel.fl)
        {
            if (o instanceof deci.d.p)
            {
                if (((deci.d.p) o).fH > top)
                {
                    top = ((deci.d.p) o).fH;
                    best = i;
                }
                i++;
            }
        }
        return best;
    }

    /** True when DecimationMod's menu player preview (fakePlayerProfile B, MenuPreviewWorld deci.e.b) exists. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean hasMenuPreview()
    {
        return deci.a.b.a().B != null;
    }

    /**
     * Creates the menu player preview as Decimation's menu does (GuiMenuBase): its HUD reads it with fancy graphics and
     * crashed when a world was reached without the menu (bug.md "HUD crash after joining a world without Decimation's menu").
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void ensureMenuPreview()
    {
        deci.a.b mod = deci.a.b.a();
        if (mod.B == null)
        {
            mod.B = new deci.e.b(net.minecraft.client.Minecraft.getMinecraft().getSession().func_148256_e());
        }
    }

    /** Dev tests only: as if the menu was never opened. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void clearMenuPreview()
    {
        deci.a.b.a().B = null;
    }

    /** Shows one of Decimation's toasts (ToastNotification.show, deci.b.b.b): title "Decimation", one line. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void toast(String line)
    {
        deci.b.b.b(line);
    }

    /** Every toast notification on screen or waiting (ToastNotification deci.b.b: current S, queue R, lines W), oldest first. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> toasts()
    {
        java.util.List<deci.b.b> all = new java.util.ArrayList<deci.b.b>();
        if (deci.b.b.S != null)
        {
            all.add(deci.b.b.S);
        }
        all.addAll(deci.b.b.R);
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (deci.b.b t : all)
        {
            StringBuilder s = new StringBuilder();
            for (String line : t.W)
            {
                if (line != null && !line.isEmpty())
                {
                    s.append(s.length() == 0 ? "" : " ").append(line.trim());
                }
            }
            out.add(net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(s.toString()));
        }
        return out;
    }

    /** Decimation's update progress screen (GuiUpdateProgress deci.i.m: banner, texts, green bar), shown by our updater. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.client.gui.GuiScreen updateProgressScreen()
    {
        return new deci.i.m(deci.a.b.a());
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isUpdateProgress(net.minecraft.client.gui.GuiScreen screen)
    {
        return screen instanceof deci.i.m;
    }

    /**
     * What Decimation's progress screen shows (UpdateChecker net.decimation.mod.common.utils.h: downloadUrl azL, its
     * status line after "Downloading:"; percent azM, the bar; downloading azN, red DO NOT CLOSE or green SAFE TO CLOSE;
     * downloadedBytes azO, printed after the file name and LAST on the size line; totalBytes azP, FIRST on the size line;
     * fileName is not renamed), filled from our download (fixes/UpdateProgress).
     */
    public static void setUpdateProgress(String status, String file, int percent, boolean downloading, double downloadedField,
                                         double totalField)
    {
        net.decimation.mod.common.utils.h.azL = status;
        net.decimation.mod.common.utils.h.fileName = file;
        net.decimation.mod.common.utils.h.azM = percent;
        net.decimation.mod.common.utils.h.azN = downloading;
        net.decimation.mod.common.utils.h.azO = downloadedField;
        net.decimation.mod.common.utils.h.azP = totalField;
    }

    /** The progress screen's numbers as "percent | downloading | done MB of total MB" (dev tests). */
    public static String updateProgress()
    {
        return net.decimation.mod.common.utils.h.azM + " | " + net.decimation.mod.common.utils.h.azN + " | "
            + net.decimation.mod.common.utils.h.azP + " of " + net.decimation.mod.common.utils.h.azO + " MB";   // done of total (UpdateProgress swaps them)
    }

    /**
     * The three update lines Decimation's progress screen draws from UpdateChecker (GuiUpdateProgress.drawScreen): file
     * line "Downloading: fileName (downloadedBytes mb)", status line lang "menu.updating.downloading" + downloadUrl, size
     * line "totalBytes mb/downloadedBytes mb"; colour codes left out. Dev tests.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static String[] updateLines()
    {
        return new String[] {"Downloading: " + net.decimation.mod.common.utils.h.fileName + " (" + net.decimation.mod.common.utils.h.azO + "mb)",
            net.minecraft.client.resources.I18n.format("menu.updating.downloading") + " " + net.decimation.mod.common.utils.h.azL,
            net.decimation.mod.common.utils.h.azP + "mb/" + net.decimation.mod.common.utils.h.azO + "mb"};
    }

    /** The titles of every toast on screen or waiting (ToastNotification deci.b.b X), oldest first. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> toastTitles()
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (deci.b.b.S != null)
        {
            out.add(deci.b.b.S.X);
        }
        for (deci.b.b t : deci.b.b.R)
        {
            out.add(t.X);
        }
        return out;
    }

    /** Title and line of every entry of a Decimation popup menu (DeciSubScreen deci.d.j: fa, fb), e.g. My Community. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.List<String> popupLines(net.minecraft.client.gui.GuiScreen screen)
    {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (!(screen instanceof deci.d.j))
        {
            return out;
        }
        try
        {
            java.lang.reflect.Field titles = deci.d.j.class.getDeclaredField("fa");
            java.lang.reflect.Field lines = deci.d.j.class.getDeclaredField("fb");
            titles.setAccessible(true);
            lines.setAccessible(true);
            java.util.List<?> t = (java.util.List<?>) titles.get(screen);
            java.util.List<?> l = (java.util.List<?>) lines.get(screen);
            for (int i = 0; i < t.size(); i++)
            {
                out.add(net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(String.valueOf(t.get(i))) + " | "
                        + (i < l.size() ? l.get(i) : ""));
            }
        }
        catch (ReflectiveOperationException e)
        {
            out.add("unreadable: " + e);
        }
        return out;
    }

    /** Clicks entry `n` (1 = the first) of a Decimation popup menu (DeciSubScreen.onLineClicked, deci.d.j.g). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void clickPopupLine(net.minecraft.client.gui.GuiScreen screen, int n)
    {
        ((deci.d.j) screen).g(n);
    }

    /** The tooltip of one of Decimation's menu buttons (DeciMenuButton deci.d.f, private eN) without colour codes. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static String menuTooltip(net.minecraft.client.gui.GuiButton b)
    {
        if (!(b instanceof deci.d.f))
        {
            return "";
        }
        try
        {
            java.lang.reflect.Field f = deci.d.f.class.getDeclaredField("eN");
            f.setAccessible(true);
            Object tip = f.get(b);
            return tip == null ? "" : net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(tip.toString());
        }
        catch (ReflectiveOperationException e)
        {
            return "unreadable: " + e;
        }
    }

    /** The totals line at the bottom of the main menu (DeciConstants.globalHumanDeaths / globalZombieDeaths). */
    public static void setStatTotals(long humans, long infected)
    {
        deci.Q.c.Xj = humans;
        deci.Q.c.Xk = infected;
    }

    /** A new SoldierEntity (deci.ag.l). */
    public static net.minecraft.entity.EntityLiving newSoldier(net.minecraft.world.World world)
    {
        return new deci.ag.l(world);
    }

    /** A new SovietEntity (deci.ag.m). */
    public static net.minecraft.entity.EntityLiving newSoviet(net.minecraft.world.World world)
    {
        return new deci.ag.m(world);
    }

    /** BanditEntity.getGun (eD): the gun it holds and shoots with. */
    public static net.minecraft.item.ItemStack npcGun(net.minecraft.entity.Entity npc)
    {
        return ((deci.ag.a) npc).eD();
    }

    /** BanditEntity.setGun (k); its onUpdate puts it in the held slot every tick. */
    public static void setNpcGun(net.minecraft.entity.Entity npc, net.minecraft.item.ItemStack gun)
    {
        ((deci.ag.a) npc).k(gun);
    }

    /** BanditEntity.setShotDelayRange (s): ticks between shots, min .. max. */
    public static void setNpcShotDelay(net.minecraft.entity.Entity npc, int min, int max)
    {
        ((deci.ag.a) npc).s(min, max);
    }

    /**
     * BanditEntity.shotHook (added by tools/patches/PatchTracer.java v2): asked
     * by shootAt when its cooldown has run out; a non null answer means the
     * hook fired the shot itself. Null = Decimation's own random hit.
     */
    public static void setNpcShotHook(java.util.function.BiFunction<net.minecraft.entity.Entity,
                                      net.minecraft.entity.EntityLivingBase, Object> hook)
    {
        deci.ag.a.shotHook = hook;
    }

    @SuppressWarnings("unchecked")
    public static java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object> npcShotHook()
    {
        return (java.util.function.BiFunction<net.minecraft.entity.Entity, net.minecraft.entity.EntityLivingBase, Object>)
            deci.ag.a.shotHook;
    }

    /** Rounds in a gun's magazine (GunItem.getAmmo: its "ammo" NBT), -1 when the stack is not a gun. */
    public static int gunAmmo(net.minecraft.item.ItemStack gun)
    {
        return gun != null && gun.getItem() instanceof deci.ay.i ? ((deci.ay.i) gun.getItem()).t(gun) : -1;
    }

    /**
     * One shot of the gun the player holds, on the client, as the mouse path does it (GunItem.fire a(ItemStack,
     * EntityPlayer): fireRay per pellet, PacketGunHit / PacketGunFire to the server, recoil, the Fire animation).
     * Skips the mouse poll, fire rate and fire mode checks of GunItem.clientHeldTick. Client thread only.
     */
    public static boolean fireHeldGun(EntityPlayer player)
    {
        net.minecraft.item.ItemStack s = player.getHeldItem();
        if (s == null || !(s.getItem() instanceof deci.ay.i))
        {
            return false;
        }
        ((deci.ay.i) s.getItem()).a(s, player);
        return true;
    }

    /** A suppressor on this gun (its "barrelAttach" NBT names one; NPC guns carry it the same way). */
    public static boolean hasSuppressor(net.minecraft.item.ItemStack gun)
    {
        return gun != null && gun.stackTagCompound != null
            && gun.stackTagCompound.getString("barrelAttach").toLowerCase().contains("suppressor");
    }

    /** GunItem.damage (aew) of a gun stack, 0 when it is not a gun. */
    public static int gunDamageOf(net.minecraft.item.ItemStack gun)
    {
        return gun != null && gun.getItem() instanceof deci.ay.i ? ((deci.ay.i) gun.getItem()).aew : 0;
    }

    /** GunItem.setFlashTime (i): muzzle flash for one tick, as shootAt does. */
    public static void gunFlash(net.minecraft.item.ItemStack gun)
    {
        if (gun != null && gun.getItem() instanceof deci.ay.i)
        {
            ((deci.ay.i) gun.getItem()).i(gun, 1);
        }
    }

    /**
     * PacketGunFireEffects (deci.aE.a$B) to everyone: shot effects of an NPC,
     * tracer from its eyes to the aim point, ending there (PatchTracer v2, v3: the point where the shot ended).
     */
    public static void sendNpcShot(net.minecraft.entity.Entity shooter, net.minecraft.entity.Entity target,
                                   double x, double y, double z)
    {
        deci.aE.a.B.nextTarget = target == null ? -1 : target.getEntityId();
        deci.aE.a.B.nextHasAim = true;
        deci.aE.a.B.nextAimX = (float) x;
        deci.aE.a.B.nextAimY = (float) y;
        deci.aE.a.B.nextAimZ = (float) z;
        try
        {
            deci.aF.a.a.a.gB().sendToAll(new deci.aE.a.B(shooter.getEntityId()));
        }
        finally
        {
            deci.aE.a.B.nextHasAim = false;
            deci.aE.a.B.nextTarget = -1;
        }
    }

    /**
     * Bullet impact on a block, as Decimation's PacketBlockHitParticles handler
     * does for player shots: PacketSpawnVanillaParticle (deci.aE.a$V)
     * "explode" and "blockcrack_ID_META" at the hit point, to everyone.
     */
    public static void sendBlockImpact(double x, double y, double z, net.minecraft.block.Block block, int meta)
    {
        cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper ch = deci.aF.a.a.a.gB();
        ch.sendToAll(new deci.aE.a.V("explode", x, y, z));
        ch.sendToAll(new deci.aE.a.V("blockcrack_" + net.minecraft.block.Block.getIdFromBlock(block) + "_" + meta,
                                      x, y, z));
    }

    /** BanditEntity.isHostileTo (c) and its overrides (tools/patches/PatchFactions.java). */
    public static boolean npcHostileTo(net.minecraft.entity.Entity npc, net.minecraft.entity.EntityLivingBase other)
    {
        return ((deci.ag.a) npc).c(other);
    }

    /**
     * A RocketEntity (deci.ak.f, the player's RPG rocket: flies at the given
     * speed with a little gravity, smoke trail, explodes on impact) from the
     * shooter toward a direction. Its target constructor places it at the
     * shooter's eyes, a block toward the target; setShooter (f), then the
     * heading (setThrowableHeading) is ours.
     */
    public static void fireRocket(net.minecraft.entity.Entity shooter, net.minecraft.entity.EntityLivingBase target,
                                  double dx, double dy, double dz, float speed)
    {
        deci.ak.f rocket = new deci.ak.f(shooter.worldObj, (net.minecraft.entity.EntityLivingBase) shooter, target,
                                         speed, 0f);
        rocket.f(shooter);
        rocket.setThrowableHeading(dx, dy, dz, speed, 0f);
        shooter.worldObj.spawnEntityInWorld(rocket);
    }

    /** GunItem.stats (aeq).fireModes (adp) holds AUTO or BURST (GunStats$FireMode deci.ay.e$a). */
    public static boolean gunIsAutomatic(net.minecraft.item.ItemStack gun)
    {
        if (gun == null || !(gun.getItem() instanceof deci.ay.i))
        {
            return false;
        }
        for (deci.ay.e.a mode : ((deci.ay.i) gun.getItem()).aeq.adp)
        {
            if (mode == deci.ay.e.a.AUTO || mode == deci.ay.e.a.BURST)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Rounds in one magazine, as GunItem's max ammo rule: the first ammo type's
     * capacity (AmmoItem deci.ay.f.adF) unless it feeds loose rounds or
     * stripper clips (feedType adE bullet / sclip), then the gun's own
     * capacity (shotguns, bolt rifles, launchers).
     */
    public static int gunMagazine(net.minecraft.item.ItemStack gun)
    {
        if (gun == null || !(gun.getItem() instanceof deci.ay.i))
        {
            return 30;
        }
        deci.ay.i g = (deci.ay.i) gun.getItem();
        deci.ay.f first = g.aep != null && g.aep.length > 0 ? g.aep[0] : null;
        if (first != null && first.adE != deci.ay.a.bullet && first.adE != deci.ay.a.sclip)
        {
            return first.adF;
        }
        return g.getCapacity();
    }

    /**
     * A new magazine: an AmmoItem (deci.ay.f, feed type mag) that loads the
     * same bullet as an existing mag (its bulletItem adI) and shows another
     * mag's icon (no new art). Registered by AmmoItem's constructor under
     * the running mod (deciworldgen:NAME). Stacks of 3 as Decimation's mags.
     */
    public static Item newMagazine(String name, int capacity, String bulletOfMag, String iconOfMag)
    {
        deci.ay.f like = (deci.ay.f) cpw.mods.fml.common.registry.GameRegistry.findItem("deci", bulletOfMag);
        return new Magazine(name, capacity, like.adI, "deci:ammo/mag/" + iconOfMag);
    }

    private static final class Magazine extends deci.ay.f
    {
        private final String icon;

        Magazine(String name, int capacity, Item bullet, String icon)
        {
            super(name, capacity, 3, bullet);
            this.icon = icon;
        }

        @Override
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister reg)
        {
            itemIcon = reg.registerIcon(icon);
        }
    }

    /**
     * A new gun (GunItem deci.ay.i): GunStats (deci.ay.e: recoil pitch / yaw,
     * fire modes AUTO / SINGLE, mode params, aim table as the Uzi's, recoil
     * recovery, rounds per minute), category (WeaponCategory deci.ay.c by
     * name: smg, rifle, pistol ...), its magazines, setMovementSlowdown (f),
     * setDamage (am). Its constructor registers the item (under the running
     * mod), its sounds (guns/<category>/<name>/...) and, on the client, loads
     * models/guns/<category>/<name>.bmodel (docs/gun_model_spec.md).
     */
    public static Item newGun(String name, String category, float recoilPitch, float recoilYaw, boolean auto,
                              float recovery, float rpm, double slowdown, int damage, Item... mags)
    {
        deci.ay.e.a[] modes = auto ? new deci.ay.e.a[] {deci.ay.e.a.AUTO, deci.ay.e.a.SINGLE}
            : new deci.ay.e.a[] {deci.ay.e.a.SINGLE};
        deci.ay.e stats = new deci.ay.e(new float[] {recoilPitch, recoilYaw}, modes, new int[] {1, 1},
            new float[][] {{16.0f, 1.5f}, {14.0f, 2.0f, 50.0f}, {9.0f, 4.0f, 100.0f}}, recovery, rpm);
        return new deci.ay.i(name, stats, deci.ay.c.valueOf(category), mags).f(slowdown).am(damage);
    }

    /**
     * Points a gun's sounds (its SoundEntry paths guns/<cat>/<gun>/..., in
     * SoundRegistry deci.N.a.Ue, DecimationMod.getSoundRegistry p) at another
     * gun's files (no copied audio) and writes Decimation's generated
     * sounds.json again (writeSoundsJsonAndMount bw: Decimation wrote it
     * before our guns existed). Client only (the registry is client side).
     */
    public static int useGunSounds(String gun, String category, String fromGun, String fromCategory)
    {
        deci.N.a reg = deci.a.b.a().p();
        int n = 0;
        try
        {
            java.lang.reflect.Field name = deci.N.c.class.getDeclaredField("name");
            java.lang.reflect.Field path = deci.N.c.class.getDeclaredField("path");
            name.setAccessible(true);
            path.setAccessible(true);
            String from = "guns/" + category + "/" + gun + "/", to = "guns/" + fromCategory + "/" + fromGun + "/";
            for (Object o : reg.Ue)
            {
                String p = (String) path.get(o);
                if (((String) name.get(o)).startsWith(gun) && p != null && p.startsWith(from))
                {
                    path.set(o, to + p.substring(from.length()));
                    n++;
                }
            }
            reg.bw();
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] sound redirect for %s failed: %s", gun, e);
        }
        return n;
    }

    /**
     * Draws attachment `attachment` (deci:<name>) on `gun` moved by (dx, dy, dz) model units.
     * GunItemRenderer places a barrel attachment at a constant plus flamePos x 16 / 21, so on a
     * short gun it floats ahead of the muzzle (Decimation's own MP7 has a hardcoded fix). An
     * AttachmentItem (deci.ay.h) caches one model per gun in ST (getModelFor b): we put our own
     * copy there (BModelLoader deci.n.g.a), its offset (BModel lc / ld / le, package private,
     * applied by renderParts) set to the correction. Client only. docs/gun_style_guide.md section 14.
     */
    public static boolean offsetAttachment(Item gun, String attachment, float dx, float dy, float dz)
    {
        Item a = cpw.mods.fml.common.registry.GameRegistry.findItem("deci", attachment);
        if (!(a instanceof deci.ay.h) || !(gun instanceof deci.ay.i))
        {
            return false;
        }
        deci.ay.h att = (deci.ay.h) a;
        try
        {
            deci.n.f model = deci.n.g.a(new net.minecraft.util.ResourceLocation(
                "deci:models/attachments/" + att.adQ.toString() + "/" + att.adK + ".bmodel"));
            if (model == null)
            {
                return false;
            }
            String[] names = {"lc", "ld", "le"};
            float[] d = {dx, dy, dz};
            for (int i = 0; i < 3; i++)
            {
                java.lang.reflect.Field f = deci.n.f.class.getDeclaredField(names[i]);
                f.setAccessible(true);
                f.setFloat(model, f.getFloat(model) + d[i]);
            }
            att.ST.put((deci.ay.i) gun, model);
            return true;
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] attachment offset %s failed: %s", attachment, e);
            return false;
        }
    }

    /** The offset (BModel lc, ld, le) of an attachment's model copy for one gun (AttachmentItem.ST), or null. */
    public static float[] attachmentOffset(Item gun, String attachment)
    {
        deci.n.f model = attachmentModel(gun, attachment);
        if (model == null)
        {
            return null;
        }
        float[] o = new float[3];
        String[] names = {"lc", "ld", "le"};
        try
        {
            for (int i = 0; i < 3; i++)
            {
                java.lang.reflect.Field f = deci.n.f.class.getDeclaredField(names[i]);
                f.setAccessible(true);
                o[i] = f.getFloat(model);
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return o;
    }

    /**
     * The top level parts (BModel.parts, children hang from them) of an attachment's model copy for one gun: name
     * (ModelRenderer.boxName) to the number of the part's own boxes (BModelPart.jQ), or null when there is no copy.
     */
    public static java.util.Map<String, Integer> attachmentPartBoxes(Item gun, String attachment)
    {
        deci.n.f model = attachmentModel(gun, attachment);
        if (model == null)
        {
            return null;
        }
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<String, Integer>();
        try
        {
            java.lang.reflect.Field boxes = deci.n.b.class.getDeclaredField("jQ");
            boxes.setAccessible(true);
            for (Object p : topParts(model))
            {
                Object own = boxes.get(p);
                out.put(((net.minecraft.client.model.ModelRenderer) p).boxName, own instanceof java.util.List ? ((java.util.List<?>) own).size() : 0);
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return out;
    }

    /**
     * Draws a top level part of an attachment's model copy for one gun without its own boxes (its children stay): the
     * part gets an empty box list (BModelPart.jQ), so call it before the copy is first drawn (its display lists compile
     * then). True when the part was found.
     */
    public static boolean clearAttachmentPartBoxes(Item gun, String attachment, String part)
    {
        deci.n.f model = attachmentModel(gun, attachment);
        if (model == null)
        {
            return false;
        }
        try
        {
            java.lang.reflect.Field boxes = deci.n.b.class.getDeclaredField("jQ");
            boxes.setAccessible(true);
            for (Object p : model.kC)
            {
                if (part.equals(((net.minecraft.client.model.ModelRenderer) p).boxName))
                {
                    boxes.set(p, new java.util.ArrayList<Object>());   // a new list: never the loaded model's own
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] attachment part %s of %s not cleared: %s", part, attachment, e);
        }
        return false;
    }

    /** A model's top level parts, the ones inside a BatchedPart in its place included. */
    private static java.util.List<Object> topParts(deci.n.f model)
    {
        java.util.List<Object> out = new java.util.ArrayList<Object>();
        for (Object p : model.kC)
        {
            if (p instanceof BatchedPart)
            {
                out.addAll(((BatchedPart) p).parts);
            }
            else
            {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * The attachment's model copy for one gun draws its top level parts from one display list (BatchedPart): true when
     * done. Before the copy is first drawn.
     */
    public static boolean batchAttachmentParts(Item gun, String attachment)
    {
        deci.n.f model = attachmentModel(gun, attachment);
        if (model == null || model.kC.isEmpty() || model.kC.get(0) instanceof BatchedPart)
        {
            return false;
        }
        java.util.List<deci.n.b> parts = new java.util.ArrayList<deci.n.b>();
        for (Object p : model.kC)
        {
            parts.add((deci.n.b) p);
        }
        BatchedPart batch = new BatchedPart(model, parts);
        model.kC.clear();
        model.kC.add(batch);
        return true;
    }

    /** How many parts the attachment's copy for this gun draws from one list, or -1 when it draws them one by one. */
    public static int attachmentBatch(Item gun, String attachment)
    {
        deci.n.f model = attachmentModel(gun, attachment);
        return model != null && model.kC.size() == 1 && model.kC.get(0) instanceof BatchedPart
            ? ((BatchedPart) model.kC.get(0)).parts.size() : -1;
    }

    /**
     * Top level parts of a model copy drawn from one display list (GripPlacement's grip copies, docs/decimation_code/
     * weapons.md section 8). BModelPart (deci.n.b) draws each part with about seven GL calls (matrix push, move, scale,
     * its lists, pop): the RK-1's 56 parts cost 0.13 ms a draw in the production client, most of it handing calls to the
     * driver. The first draw lets every part compile its own lists (a list must never be compiled inside another), the
     * second compiles one list that calls them all, then a draw is one call. Parts draw one by one while {@link #enabled}
     * is off (dev test A / B), and for good when one of them was still not compiled after the first draw. Client only:
     * only GripPlacement makes them.
     */
    public static final class BatchedPart extends deci.n.b
    {
        public static boolean enabled = true;
        private static java.lang.reflect.Field compiled;
        final java.util.List<deci.n.b> parts;
        private int list;
        private boolean warm;

        BatchedPart(deci.n.f model, java.util.List<deci.n.b> parts)
        {
            super(model, "batch");
            this.parts = parts;
        }

        /** BModelPart.render(scale, useLists): BModel.renderParts calls it with lists on. */
        @Override
        public void a(float f, boolean lists)
        {
            if (!enabled || !lists || list < 0 || !warm)
            {
                for (deci.n.b p : parts)
                {
                    p.a(f, lists);
                }
                warm |= lists;
                return;
            }
            if (list == 0)
            {
                if (!allCompiled())
                {
                    list = -1;
                    a(f, lists);
                    return;
                }
                list = net.minecraft.client.renderer.GLAllocation.generateDisplayLists(1);
                org.lwjgl.opengl.GL11.glNewList(list, org.lwjgl.opengl.GL11.GL_COMPILE);
                for (deci.n.b p : parts)
                {
                    p.a(f, true);
                }
                org.lwjgl.opengl.GL11.glEndList();
            }
            org.lwjgl.opengl.GL11.glCallList(list);
        }

        /** Every part has compiled its own lists (BModelPart's private flag jX). */
        private boolean allCompiled()
        {
            try
            {
                if (compiled == null)
                {
                    compiled = deci.n.b.class.getDeclaredField("jX");
                    compiled.setAccessible(true);
                }
                for (deci.n.b p : parts)
                {
                    if (!compiled.getBoolean(p))
                    {
                        return false;
                    }
                }
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    private static deci.n.f attachmentModel(Item gun, String attachment)
    {
        Item a = cpw.mods.fml.common.registry.GameRegistry.findItem("deci", attachment);
        if (!(a instanceof deci.ay.h) || !(gun instanceof deci.ay.i))
        {
            return null;
        }
        Object m = ((deci.ay.h) a).ST.get(gun);
        return m instanceof deci.n.f ? (deci.n.f) m : null;
    }

    /**
     * A new sight attachment (AttachmentItem deci.ay.h, slot sight, all categories) registered as
     * deci:<name>. Decimation looks attachments up by GameRegistry.findItem("deci", name) (the gun's
     * NBT sightAttach), so ours must live in its namespace: FML names an item after the active
     * mod container, which is switched to Decimation's for the constructor (LoadController's
     * private activeContainer) and back. Model deci:models/attachments/sight/<name>.bmodel, texture
     * deci:textures/model/attachments/sight/<name>.png, icon deci:attach/<name>.
     */
    public static Item newSightAttachment(final String name, final float zoomFov, final float sway)
    {
        return inDecimation("sight " + name, () -> new deci.ay.h(deci.ay.b.sight, deci.ay.c.all, zoomFov, sway, name));
    }

    /**
     * A new grip attachment (AttachmentItem deci.ay.h, slot grip, category rifle as Decimation's Foregrip) registered as
     * deci:<name> like newSightAttachment. Model deci:models/attachments/grip/<name>.bmodel, texture
     * deci:textures/model/attachments/grip/<name>.png, icon deci:attach/<name>.
     */
    public static Item newGripAttachment(final String name)
    {
        return inDecimation("grip " + name, () -> new deci.ay.h(deci.ay.b.grip, deci.ay.c.rifle, name));
    }

    /**
     * A melee weapon of Decimation's own kind (MeleeWeaponItem deci.az.b, durability, damage): its damage taken off the
     * target's health by DamageModifierHandler (not reduced by armor), wear and impact sound, its "Melee Weapon / Damage"
     * tooltip; in Decimation's melee creative tab (CreativeTabRegistry axc). Not registered here.
     */
    public static Item newMeleeWeapon(int durability, int damage)
    {
        return new deci.az.b(durability, damage).setCreativeTab(deci.aD.r.axc);
    }

    /** A model from a .bmodel file (BModelLoader.load deci.n.g.a), as Object: draw it with renderModel. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static Object loadModel(String location)
    {
        return deci.n.g.a(new net.minecraft.util.ResourceLocation(location));
    }

    /** Draws every part of a model from loadModel at 1/16 block a unit (BModel.render), texture bound by the caller. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void renderModel(Object model)
    {
        ((deci.n.f) model).render(null, 0f, 0f, 0f, 0f, 0f, 0.0625f);
    }

    /** Runs an item constructor with Decimation as FML's active mod container (its namespace), then switches back. */
    private static Item inDecimation(String what, java.util.concurrent.Callable<Item> make)
    {
        cpw.mods.fml.common.ModContainer deciMod = cpw.mods.fml.common.Loader.instance().getIndexedModList().get("deci");
        java.lang.reflect.Field controller = null, active = null;
        Object lc = null, before = null;
        try
        {
            controller = cpw.mods.fml.common.Loader.class.getDeclaredField("modController");
            controller.setAccessible(true);
            lc = controller.get(cpw.mods.fml.common.Loader.instance());
            active = lc.getClass().getDeclaredField("activeContainer");
            active.setAccessible(true);
            before = active.get(lc);
            active.set(lc, deciMod);
            return make.call();
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.warning("[deciworldgen] %s not registered: %s", what, e);
            return null;
        }
        finally
        {
            if (active != null && lc != null)
            {
                try
                {
                    active.set(lc, before);
                }
                catch (IllegalAccessException e)
                {
                    // set accessible above
                }
            }
        }
    }

    /** True when the gun (GunItem deci.ay.i) loads this magazine (its ammoTypes aep). */
    public static boolean gunTakes(Item gun, Item mag)
    {
        if (!(gun instanceof deci.ay.i) || ((deci.ay.i) gun).aep == null)
        {
            return false;
        }
        for (deci.ay.f m : ((deci.ay.i) gun).aep)
        {
            if (m == mag)
            {
                return true;
            }
        }
        return false;
    }

    /** Lets a gun load one more magazine (appended to its ammoTypes aep). */
    public static void addGunMagazine(Item gun, Item mag)
    {
        deci.ay.i g = (deci.ay.i) gun;
        deci.ay.f[] now = g.aep == null ? new deci.ay.f[0] : g.aep;
        deci.ay.f[] more = java.util.Arrays.copyOf(now, now.length + 1);
        more[now.length] = (deci.ay.f) mag;
        g.aep = more;
    }

    /**
     * As addLootLike, with as many copies in each pool as give the stack about this share of the pool's entries (pools
     * differ: a military crate's holds about 191 entries, a police crate's about 14), at least one. Returns pools changed.
     */
    @SuppressWarnings("unchecked")
    public static int addLootShare(Item like, net.minecraft.item.ItemStack add, double share)
    {
        deci.aD.l table = deci.a.b.a().f();
        int n = 0;
        try
        {
            for (Object entry : table.awA)
            {
                java.lang.reflect.Field f = entry.getClass().getDeclaredField("awC");
                f.setAccessible(true);
                java.util.List<net.minecraft.item.ItemStack> items = (java.util.List<net.minecraft.item.ItemStack>) f.get(entry);
                boolean has = false;
                for (net.minecraft.item.ItemStack s : items)
                {
                    has |= s != null && s.getItem() == like;
                }
                if (has)
                {
                    int copies = Math.max(1, (int) Math.round(share * items.size() / (1 - share)));
                    for (int i = 0; i < copies; i++)
                    {
                        items.add(add.copy());
                    }
                    n++;
                }
            }
            table.init();
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] loot table change failed: %s", e);
        }
        return n;
    }

    /**
     * Adds a stack to every loot pool that already holds `like`
     * (LootTableRegistry deci.aD.l, DecimationMod.getLootTable deci.a.b.a().f():
     * its entries awA are a package private class, item lists in field awC),
     * then rebuilds the pools (init). Returns the number of pools changed.
     */
    @SuppressWarnings("unchecked")
    public static int addLootLike(Item like, net.minecraft.item.ItemStack add)
    {
        deci.aD.l table = deci.a.b.a().f();
        int n = 0;
        try
        {
            for (Object entry : table.awA)
            {
                java.lang.reflect.Field f = entry.getClass().getDeclaredField("awC");
                f.setAccessible(true);
                java.util.List<net.minecraft.item.ItemStack> items = (java.util.List<net.minecraft.item.ItemStack>) f.get(entry);
                boolean has = false;
                for (net.minecraft.item.ItemStack s : items)
                {
                    has |= s != null && s.getItem() == like;
                }
                if (has)
                {
                    items.add(add.copy());
                    n++;
                }
            }
            table.init();
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.info("[deciworldgen] loot table change failed: %s", e);
        }
        return n;
    }

    /**
     * Decimation's crate loot table as "block registry name" -> (items per open n, item list)
     * (LootTableRegistry deci.aD.l entries awA: block, afu = n, awC = items).
     */
    public static java.util.Map<String, java.util.Map.Entry<Integer, java.util.List<net.minecraft.item.ItemStack>>> lootTable()
    {
        java.util.Map<String, java.util.Map.Entry<Integer, java.util.List<net.minecraft.item.ItemStack>>> out =
            new java.util.LinkedHashMap<String, java.util.Map.Entry<Integer, java.util.List<net.minecraft.item.ItemStack>>>();
        for (Object o : deci.a.b.a().f().awA)
        {
            try
            {
                Class<?> c = o.getClass();   // a private class: its public fields need setAccessible
                java.lang.reflect.Field fb = c.getDeclaredField("block"), fn = c.getDeclaredField("afu"), fl = c.getDeclaredField("awC");
                fb.setAccessible(true);
                fn.setAccessible(true);
                fl.setAccessible(true);
                net.minecraft.block.Block block = (net.minecraft.block.Block) fb.get(o);
                int n = fn.getInt(o);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.item.ItemStack> items = (java.util.List<net.minecraft.item.ItemStack>) fl.get(o);
                out.put(net.minecraft.block.Block.blockRegistry.getNameForObject(block),
                        new java.util.AbstractMap.SimpleEntry<Integer, java.util.List<net.minecraft.item.ItemStack>>(n, items));
            }
            catch (Exception e)
            {
                cpw.mods.fml.common.FMLLog.info("[deciworldgen] loot table read failed: %s", e);
            }
        }
        return out;
    }

    /**
     * One open of Decimation's own crate loot for a block (LootTableRegistry.getPool c, then
     * LootPool.fillInventory a); false when the block has no pool.
     */
    public static boolean rollDecimationLoot(net.minecraft.block.Block block, net.minecraft.inventory.IInventory inv)
    {
        deci.aB.c pool = deci.a.b.a().f().c(block);
        if (pool == null)
        {
            return false;
        }
        pool.a(inv);
        return true;
    }

    /**
     * LootCooldownRegistry.isReady (deci.aB.e.d(world).k): the crate at this spot gives loot again
     * (lootRespawnTime, default 900 s, after its last open; kept in memory only).
     */
    public static boolean lootReady(net.minecraft.world.World world, int x, int y, int z)
    {
        return deci.aB.e.d(world).k(x, y, z);
    }

    /** LootCooldownRegistry.clear: every crate of every world gives loot again in Decimation's own map (its clear at server
     *  start never runs: an @Mod.EventHandler method put on the event buses; the map lives for the whole game). */
    public static void clearLootCooldowns()
    {
        deci.aB.e.clear();
    }

    /** ServerConfig.lootRespawnTime (deci.aJ.b.aAy): seconds until Decimation refills a looted spot (900; a dedicated
     *  server's decimation_server.properties may change it, read in preInit). */
    public static int lootRespawnSeconds()
    {
        return deci.aJ.b.aAy;
    }

    /**
     * The spot LootInteractHandler (deci.aK.k) keys a click on: the clicked block, or for a multiblock (IMultiblockB
     * deci.W.b) its origin (IMultiblockD deci.W.d); null where the handler stops (no tile entity there, another block at
     * the origin).
     */
    public static int[] lootSpot(net.minecraft.world.World world, int x, int y, int z)
    {
        net.minecraft.block.Block block = world.getBlock(x, y, z);
        if (!(block instanceof deci.W.b))
        {
            return new int[] {x, y, z};
        }
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof deci.W.d))
        {
            return null;
        }
        deci.W.d d = (deci.W.d) te;
        int ox = d.getOriginX(), oy = d.getOriginY(), oz = d.getOriginZ();
        return world.getBlock(ox, oy, oz) == block ? new int[] {ox, oy, oz} : null;
    }

    private static java.lang.reflect.Field lootTimes;

    /**
     * Sets LootCooldownRegistry's own state of a spot (deci.aB.e.d(world)): looted now (markLooted l: the handler then
     * answers "This <block> is empty...") or ready (the spot taken out of its private TLongLongMap afm, keyed as
     * packPos j: LootRefill.key).
     */
    public static void setLootCooldown(net.minecraft.world.World world, int x, int y, int z, boolean looted)
    {
        deci.aB.e reg = deci.aB.e.d(world);
        if (looted)
        {
            reg.l(x, y, z);
            return;
        }
        try
        {
            if (lootTimes == null)
            {
                for (java.lang.reflect.Field f : deci.aB.e.class.getDeclaredFields())
                {
                    if (f.getType() == gnu.trove.map.TLongLongMap.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                    {
                        f.setAccessible(true);
                        lootTimes = f;
                    }
                }
            }
            ((gnu.trove.map.TLongLongMap) lootTimes.get(reg)).remove(LootRefill.key(x, y, z));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("LootCooldownRegistry's map not found", e);
        }
    }

    // ---- copies of Decimation's props (fixes/LootBlocks, feature 12 B)

    /**
     * A copy of one of Decimation's props (BlockProp, net.decimation.mod.common.block.props): every field of BlockProp
     * (prop name, model or .bmodel, display name, size, light, flares, seat, sound, safezone flag) and the Block's look
     * and strength (copyBlockLook). A tile entity made by the copy is a TileEntityProp of the same prop name, so
     * PropRenderer draws it as the original. The caller registers it.
     */
    public static net.minecraft.block.Block copyProp(net.minecraft.block.Block original)
    {
        net.decimation.mod.common.block.props.BlockProp o = (net.decimation.mod.common.block.props.BlockProp) original;
        net.decimation.mod.common.block.props.BlockProp c =
            new net.decimation.mod.common.block.props.BlockProp("copy", "tile.copy", o.getMaterial(), "copy");
        try
        {
            for (java.lang.reflect.Field f : net.decimation.mod.common.block.props.BlockProp.class.getDeclaredFields())
            {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                {
                    f.setAccessible(true);
                    f.set(c, f.get(o));
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("BlockProp fields", e);
        }
        copyBlockLook(o, c);
        return c;
    }

    /** A new MetalShelf (net.decimation.mod.common.block.props.BlockMetalShelf, a 1 x 2 multiblock) as Decimation sets up its own. */
    public static net.minecraft.block.Block newMetalShelf(net.minecraft.block.Block original)
    {
        net.minecraft.block.Block c = new net.decimation.mod.common.block.props.BlockMetalShelf();
        copyBlockLook(original, c);
        return c;
    }

    /** A new WeaponCabinet (net.decimation.mod.common.block.props.BlockWeaponCabinet, EntityWeaponCabinet drawn) like the original. */
    public static net.minecraft.block.Block newWeaponCabinet(net.minecraft.block.Block original)
    {
        net.minecraft.block.Block c = new net.decimation.mod.common.block.props.BlockWeaponCabinet();
        copyBlockLook(original, c);
        return c;
    }

    /** The Block level settings PropBlockRegistry gives a prop: bounds, hardness, resistance, light, icon, tab, sound. */
    private static void copyBlockLook(net.minecraft.block.Block o, net.minecraft.block.Block c)
    {
        c.setBlockBounds((float) o.getBlockBoundsMinX(), (float) o.getBlockBoundsMinY(), (float) o.getBlockBoundsMinZ(),
                         (float) o.getBlockBoundsMaxX(), (float) o.getBlockBoundsMaxY(), (float) o.getBlockBoundsMaxZ());
        c.setHardness(o.getBlockHardness(null, 0, 0, 0));
        c.setResistance(o.getExplosionResistance(null) * 5f / 3f);   // setResistance stores 3x, the getter reads / 5
        c.setLightLevel(o.getLightValue() / 15f);
        c.setLightOpacity(o.getLightOpacity());
        c.setStepSound(o.stepSound);
        c.setTickRandomly(o.getTickRandomly());
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient())
        {
            c.setCreativeTab(o.getCreativeTabToDisplayOn());   // client only (a server strips the getter; tabs are a client thing)
        }
        String icon = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.block.Block.class, o,
                                                                               "textureName", "field_149768_d");
        c.setBlockTextureName(icon);
    }

    /**
     * A loot table entry of LootTableRegistry (deci.aD.l, entries awA of deci.aD.l$a: block, rolls afu, items awC, sound
     * afb) for a block of ours, made from the entry of `like`: its rolls, its sound and a copy of its items (the entry's
     * constructor copies the list, so later additions by addLootLike reach both entries once each); the pools are built
     * again (init). False when `like` has no entry (a dedicated server with disableLootSystem has none).
     */
    public static boolean addLootBlock(net.minecraft.block.Block block, net.minecraft.block.Block like)
    {
        deci.aD.l table = deci.a.b.a().f();
        try
        {
            for (Object entry : new java.util.ArrayList<Object>(table.awA))
            {
                Class<?> c = entry.getClass();
                java.lang.reflect.Field fb = c.getDeclaredField("block"), fn = c.getDeclaredField("afu"),
                    fl = c.getDeclaredField("awC"), fs = c.getDeclaredField("afb");
                fb.setAccessible(true);
                if (fb.get(entry) != like)
                {
                    continue;
                }
                fn.setAccessible(true);
                fl.setAccessible(true);
                fs.setAccessible(true);
                java.lang.reflect.Constructor<?> k =
                    c.getDeclaredConstructor(net.minecraft.block.Block.class, int.class, java.util.ArrayList.class);
                k.setAccessible(true);
                Object made = k.newInstance(block, fn.get(entry), fl.get(entry));
                fs.set(made, fs.get(entry));
                ((java.util.List<Object>) (java.util.List<?>) table.awA).add(made);
                table.init();
                return true;
            }
        }
        catch (Exception e)
        {
            cpw.mods.fml.common.FMLLog.warning("[deciworldgen] loot entry for %s failed: %s", block, e);
        }
        return false;
    }

    // ---- loot screen: the path of LootInteractHandler (deci.aK.k), docs/loot_levels.md

    /** Runtime class of the loot screen (PlayerInventoryScreen deci.g.g, client only): compare names. */
    public static final String LOOT_SCREEN = "deci.g.g";

    /**
     * The inventory a player's loot screen shows (PlayerData.setLootInventory, deci.Q.b.a) and,
     * server side, sends it to the client (PacketLootInventory deci.aE.a$R on the channel
     * deci.aF.a$a$a.gB). The screen's slots act on this same object server side, so a stack taken
     * leaves `inv` itself.
     */
    public static void setLootInventory(EntityPlayer player, net.minecraft.inventory.InventoryBasic inv)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data == null)
        {
            return;
        }
        data.a(inv);
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP)
        {
            deci.aF.a.a.a.gB().sendTo(new deci.aE.a.R(inv), (net.minecraft.entity.player.EntityPlayerMP) player);
        }
    }

    /** AmmoItem.capacity (deci.ay.f.adF) of a magazine, belt or clip (feedType adE not bullet); 0 for anything else. */
    public static int magazineCapacity(net.minecraft.item.Item item)
    {
        return item instanceof deci.ay.f && ((deci.ay.f) item).adE != deci.ay.a.bullet ? ((deci.ay.f) item).adF : 0;
    }

    /** AmmoItem.bulletItem (deci.ay.f.adI): the loose round a magazine is loaded with, or null. */
    public static net.minecraft.item.Item magazineRound(net.minecraft.item.Item item)
    {
        return item instanceof deci.ay.f ? ((deci.ay.f) item).adI : null;
    }

    /** PlayerData.getLootInventory (cN): what the loot screen shows on this side. */
    public static net.minecraft.inventory.InventoryBasic lootInventory(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? null : data.cN();
    }

    /**
     * Opens Decimation's inventory screen with the loot area (GUI_LOOT deci.Q.c.Xa, x -1, y 2: y is
     * the container type, 2 = with the loot slots of PlayerGearContainer deci.f.d).
     */
    public static void openLootGui(EntityPlayer player)
    {
        player.openGui(deci.a.b.a(), deci.Q.c.Xa, player.worldObj, -1, 2, 0);
    }

    // ---- inventory shift click (fixes/InventoryShift): PlayerGearContainer = deci.f.d (w, gy, type: its setupSlots
    //      arguments), PlayerExtraInventory = deci.f.i (mask, vest, backpack slots), BackpackInventory = deci.f.g,
    //      PlayerInventoryScreen = deci.g.g, DeciConstants.gUI_LOOT = deci.Q.c.Xa

    /** DeciConstants.gUI_LOOT (Xa): the GUI id of Decimation's inventory screen (with or without the loot area). */
    public static int inventoryGuiId()
    {
        return deci.Q.c.Xa;
    }

    /** PlayerExtraInventory (deci.f.i): the mask, vest and backpack slots of the inventory screen. */
    public static boolean isGearInventory(net.minecraft.inventory.IInventory inv)
    {
        return inv instanceof deci.f.i;
    }

    /** BackpackInventory (deci.f.g): the slots of the worn backpack. */
    public static boolean isBackpackInventory(net.minecraft.inventory.IInventory inv)
    {
        return inv instanceof deci.f.g;
    }

    /** PlayerInventoryScreen (deci.g.g), client only. */
    public static boolean isInventoryScreen(Object screen)
    {
        return screen instanceof deci.g.g;
    }

    /** The server's container of the inventory screen, as Decimation's GuiHandler builds it (x, y of openGui). */
    public static net.minecraft.inventory.Container gearContainer(EntityPlayer player, int x, int y)
    {
        return new GearContainer(player.inventory, player, x, y);
    }

    /**
     * The client's container of the inventory screen rebuilt as ours, with the same layout (screen size w, gy and type
     * of the one PlayerInventoryScreen.initGui made) and window id; null when it is ours already or not Decimation's.
     */
    public static net.minecraft.inventory.Container gearContainerLike(net.minecraft.inventory.Container old, EntityPlayer player)
    {
        if (!(old instanceof deci.f.d) || old instanceof GearContainer || ((deci.f.d) old).w == -1)
        {
            return null;
        }
        deci.f.d d = (deci.f.d) old;
        GearContainer c = new GearContainer(player.inventory, player, d.w, d.gy, 0, d.type);
        c.windowId = old.windowId;
        return c;
    }

    /** True for our container of the inventory screen. */
    public static boolean hasShiftClick(net.minecraft.inventory.Container c)
    {
        return c instanceof GearContainer;
    }

    /** PlayerGearContainer with shift click (InventoryShift.transfer); Decimation's answers null. */
    public static final class GearContainer extends deci.f.d
    {
        GearContainer(net.minecraft.entity.player.InventoryPlayer inv, EntityPlayer player, int x, int y)
        {
            super(inv, player, x, y);
        }

        GearContainer(net.minecraft.entity.player.InventoryPlayer inv, EntityPlayer player, int w, int h, int id, int type)
        {
            super(inv, player, w, h, id, type);
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer player, int index)
        {
            return InventoryShift.transfer(this, player, index);
        }

        /** No click of any kind on a loot slot not searched yet (fixes/LootSearch). */
        @Override
        public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer player)
        {
            return LootSearch.refuses(this, slotId) ? null : super.slotClick(slotId, button, mode, player);
        }

        /** A double click gathering stacks skips unsearched loot slots (fixes/LootSearch). */
        @Override
        public boolean func_94530_a(ItemStack stack, net.minecraft.inventory.Slot slot)
        {
            return !LootSearch.hidden(slot) && super.func_94530_a(stack, slot);
        }
    }

    /** ItemKeycard (deci.av.k): a keycard of any clearance. */
    public static boolean isKeycard(Item item)
    {
        return item instanceof deci.av.k;
    }

    /** Gear worn in Decimation's own slots: vests (ItemVestDeci), masks (ItemMaskDeci, ItemGasMaskDeci), backpacks (deci.av.a). */
    public static boolean isWornGear(Item item)
    {
        return item instanceof net.decimation.mod.common.item.armor.ItemVestDeci
            || item instanceof net.decimation.mod.common.item.armor.ItemMaskDeci
            || item instanceof net.decimation.mod.common.item.armor.ItemGasMaskDeci || item instanceof deci.av.a;
    }

    /** GunItem.stats (aeq).secondsPerShot (ads): the gun's own rate of fire. */
    public static double gunSecondsPerShot(net.minecraft.item.ItemStack gun)
    {
        return gun != null && gun.getItem() instanceof deci.ay.i ? ((deci.ay.i) gun.getItem()).aeq.ads : 0.1;
    }

    /** The magazines a gun loads (GunItem deci.ay.i ammoTypes aep); empty for anything else. */
    public static java.util.List<Item> gunMagazines(Item gun)
    {
        java.util.List<Item> out = new java.util.ArrayList<Item>();
        if (gun instanceof deci.ay.i && ((deci.ay.i) gun).aep != null)
        {
            for (deci.ay.f m : ((deci.ay.i) gun).aep)
            {
                if (m != null)
                {
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** PlayerData.getVest / getMask / getBackpack (cw, cx, cv): Decimation's extra gear slots, null if empty. */
    public static net.minecraft.item.ItemStack[] playerGear(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? new net.minecraft.item.ItemStack[0]
            : new net.minecraft.item.ItemStack[] {data.cw(), data.cx(), data.cv()};
    }

    /** PlayerData.setVest / setMask / setBackpack (d, e, c): puts Decimation's extra gear on a player (null takes it off). */
    public static void setPlayerGear(EntityPlayer player, ItemStack vest, ItemStack mask, ItemStack backpack)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.d(vest);
            data.e(mask);
            data.c(backpack);
        }
    }

    /** HumanEntity.getMask (deci.af.d.cx). */
    public static net.minecraft.item.ItemStack npcMask(net.minecraft.entity.Entity npc)
    {
        return ((deci.af.d) npc).cx();
    }

    /** HumanEntity.setMask (deci.af.d.j): the face item it wears (gas mask, night vision goggles). */
    public static void setNpcMask(net.minecraft.entity.Entity npc, net.minecraft.item.ItemStack mask)
    {
        ((deci.af.d) npc).j(mask);
    }

    /** BanditEntity.shootAt (e): fires when its cooldown has run out. */
    public static void banditShootAt(net.minecraft.entity.Entity bandit, net.minecraft.entity.EntityLivingBase target)
    {
        ((deci.ag.a) bandit).e(target);
    }

    // ---- rendering (client)

    /** BulletTracerRenderer.tracers (deci.n.d.kd): the live tracer list. */
    public static java.util.List<?> tracers()
    {
        return (java.util.List<?>) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(deci.n.d.class, null, "kd");
    }

    /** Start and end of a BulletTracer (deci.n.d$a, fields kf / kg). */
    /** BulletTracer.color (kj): GENERIC, INVIS or an attachment colour. */
    public static String tracerColor(Object tracer)
    {
        Object c = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(deci.n.d.a.class, (deci.n.d.a) tracer, "kj");
        return String.valueOf(c);
    }

    public static org.lwjgl.util.vector.Vector3f[] tracerLine(Object tracer)
    {
        deci.n.d.a t = (deci.n.d.a) tracer;
        return new org.lwjgl.util.vector.Vector3f[] {
            (org.lwjgl.util.vector.Vector3f) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(deci.n.d.a.class, t, "kf"),
            (org.lwjgl.util.vector.Vector3f) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(deci.n.d.a.class, t, "kg")};
    }

    /**
     * Decimation's ModelItemRenderer (deci.K.c) and BModelItemRenderer (deci.K.d): called in first person they push a
     * matrix and return without its pop (bug.md "GL stack overflow"). Melee weapons, grenades, the molotov, riot shield,
     * chainsaw, guitar and a few tools use them.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean leavesFirstPersonPush(net.minecraftforge.client.IItemRenderer r)
    {
        return r != null && (r.getClass() == deci.K.c.class || r.getClass() == deci.K.d.class);
    }

    /** ClientState.enableFancyNameplate (deci.b.i.bx, decimation_client.properties; on by default). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean fancyNameplates()
    {
        return deci.b.i.bx;
    }

    /** Tests only: the fancy nameplate setting for this session. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void setFancyNameplates(boolean on)
    {
        deci.b.i.bx = on;
    }

    /** Radio / ambient prop loops Decimation started (BlockProp.mapSoundPositions, one per prop position). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.Map<net.minecraft.util.ChunkCoordinates, net.minecraft.client.audio.ISound> propSounds()
    {
        return net.decimation.mod.common.block.props.BlockProp.mapSoundPositions;
    }

    /** Tests: entries in propSounds(). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int propSoundEntries()
    {
        return propSounds().size();
    }

    /** Tests: prop loops that are playing right now. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int playingPropSounds()
    {
        int n = 0;
        for (net.minecraft.client.audio.ISound s : propSounds().values())
        {
            if (s != null && net.minecraft.client.Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(s))
            {
                n++;
            }
        }
        return n;
    }

    /**
     * The texture GunItemRenderer (deci.K.b) binds for the first person arms: TJ, set once at start to "skins/<session
     * name>", which only vanilla's dead skins.minecraft.net download ever filled (Steve for everyone).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static net.minecraft.util.ResourceLocation gunArmSkin()
    {
        return deci.K.b.TJ;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void setGunArmSkin(net.minecraft.util.ResourceLocation skin)
    {
        deci.K.b.TJ = skin;
    }

    /**
     * True for the RenderPlayer that PlayerModelSwapper (deci.J.e) uses to draw your own body in first person
     * (deci.J.e$a, on RenderWorldLastEvent); the world's normal player renderer is a different instance.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean isFirstPersonBodyRenderer(Object renderer)
    {
        return renderer instanceof deci.J.e.a;
    }

    /** Arm-less part lists per armor model part list (dropArmorArms), [0] without the left arm, [1] without both. */
    private static final java.util.Map<Object, java.util.ArrayList<?>[]> ARMLESS = new java.util.IdentityHashMap<Object, java.util.ArrayList<?>[]>();

    /**
     * Takes the arm parts out of a Decimation armor piece's model for one draw (the left arm's, both when `right`):
     * ItemArmorDeci.armor (DeciPlayerModel deci.n.e) -> heldGunModel kw (BModel deci.n.f) -> parts kC, swapped for a
     * cached list without parts named leftArm (/ rightArm). Its own hide check reads the body's isHidden, which
     * Decimation sets on every draw, and a hidden BModelPart pushes a matrix it never pops, so the list is the clean way.
     * Returns what putArmorArms needs to put it back, or null (not Decimation armor, model not loaded yet).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings("unchecked")
    public static Object[] dropArmorArms(Item item, boolean right)
    {
        if (!(item instanceof net.decimation.mod.common.item.armor.ItemArmorDeci))
        {
            return null;
        }
        deci.n.e model = ((net.decimation.mod.common.item.armor.ItemArmorDeci) item).armor;
        if (model == null || model.kw == null)
        {
            return null;
        }
        java.util.ArrayList<deci.n.b> all = model.kw.kC;
        java.util.ArrayList<?>[] cut = ARMLESS.get(all);
        if (cut == null)
        {
            cut = new java.util.ArrayList<?>[2];
            for (int k = 0; k < 2; k++)
            {
                java.util.ArrayList<deci.n.b> kept = new java.util.ArrayList<deci.n.b>();
                for (deci.n.b p : all)
                {
                    if (!p.boxName.contains("leftArm") && !(k == 1 && p.boxName.contains("rightArm")))
                    {
                        kept.add(p);
                    }
                }
                cut[k] = kept;
            }
            ARMLESS.put(all, cut);
        }
        model.kw.kC = (java.util.ArrayList<deci.n.b>) cut[right ? 1 : 0];
        return new Object[] {model, all};
    }

    /** Puts back what dropArmorArms took out (its return value). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings("unchecked")
    public static void putArmorArms(Object[] dropped)
    {
        ((deci.n.e) dropped[0]).kw.kC = (java.util.ArrayList<deci.n.b>) dropped[1];
    }

    /** Decimation's wrist display (ItemRegistry.wristTimer deci.aD.k.anH), drawn on the left arm while ClientState.co. */
    public static Item wristDisplay()
    {
        return deci.aD.k.anH;
    }

    /**
     * A human NPC's look as ModelHuman (deci.q.h) draws it, from HumanEntity (deci.af.d): fills look with {skin tone (el),
     * hair (em), beard (ep), eyes (en), shirt (eq), pants (er), shoes (es), flags: 1 blinking (eu), 2 infected
     * (InfectedEntity deci.ag.d: no hair or beard), 4 frail (FrailEntity deci.ag.h: no clothes)} and returns the skin
     * folder (ew), or null for anything else.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static String humanLook(Object entity, int[] look)
    {
        if (!(entity instanceof deci.af.d))
        {
            return null;
        }
        deci.af.d h = (deci.af.d) entity;
        look[0] = h.el();
        look[1] = h.em();
        look[2] = h.ep();
        look[3] = h.en();
        look[4] = h.eq();
        look[5] = h.er();
        look[6] = h.es();
        look[7] = (h.eu() ? 1 : 0) | (entity instanceof deci.ag.d ? 2 : 0) | (entity instanceof deci.ag.h ? 4 : 0);
        return h.ew();
    }

    /** A human NPC's mask, vest and backpack (HumanEntity deci.af.d: cx, cw, cv), or null for anything else. */
    public static net.minecraft.item.ItemStack[] humanGear(Object entity)
    {
        if (!(entity instanceof deci.af.d))
        {
            return null;
        }
        deci.af.d h = (deci.af.d) entity;
        return new net.minecraft.item.ItemStack[] {h.cx(), h.cw(), h.cv()};
    }

    /** Puts a human NPC's mask, vest and backpack (setMask j, setVest i, setBackpack h), as humanGear gave them. */
    public static void setHumanGear(Object entity, net.minecraft.item.ItemStack[] gear)
    {
        deci.af.d h = (deci.af.d) entity;
        h.j(gear[0]);
        h.i(gear[1]);
        h.h(gear[2]);
    }

    /**
     * Sets a held gun's muzzle flash ticks (GunItem deci.ay.i setFlashTime i). A stack without its tag yet is left alone:
     * Decimation's client tick (tickMuzzleFlash) gives every held gun its tag.
     */
    public static void setGunFlash(net.minecraft.item.ItemStack stack, int ticks)
    {
        if (stack != null && stack.hasTagCompound() && stack.getItem() instanceof deci.ay.i)
        {
            ((deci.ay.i) stack.getItem()).i(stack, ticks);
        }
    }

    /** A held gun's muzzle flash ticks left (GunItem deci.ay.i getFlashTime M), 0 for anything else. */
    public static int gunFlashTicks(net.minecraft.item.ItemStack stack)
    {
        return stack != null && stack.getItem() instanceof deci.ay.i ? ((deci.ay.i) stack.getItem()).M(stack) : 0;
    }

    /**
     * Loads a gear model Decimation loads only at its first draw (ModelHuman.renderHuman, ModelDeciPlayer), set as those
     * draws set it: a mask's, vest's or backpack's model (item.model: models/armor/mask|vest/<name>.bmodel,
     * models/backpacks/<name>.bmodel), an armour piece's (item.armor: a DeciPlayerModel of the human model holding
     * models/armor/full/<name>.bmodel; every draw sets its self and type again). True when it loaded one. Client thread.
     */
    public static boolean loadGearModel(Object item, net.minecraft.client.model.ModelBiped human)
    {
        if (item instanceof net.decimation.mod.common.item.armor.ItemMaskDeci)
        {
            net.decimation.mod.common.item.armor.ItemMaskDeci m = (net.decimation.mod.common.item.armor.ItemMaskDeci) item;
            return m.model == null && (m.model = gearModel("models/armor/mask/" + m.modelName)) != null;
        }
        if (item instanceof net.decimation.mod.common.item.armor.ItemVestDeci)
        {
            net.decimation.mod.common.item.armor.ItemVestDeci v = (net.decimation.mod.common.item.armor.ItemVestDeci) item;
            return v.model == null && (v.model = gearModel("models/armor/vest/" + v.modelName)) != null;
        }
        if (item instanceof deci.av.a)
        {
            deci.av.a b = (deci.av.a) item;
            return b.model == null && (b.model = gearModel("models/backpacks/" + b.modelName)) != null;
        }
        if (item instanceof net.decimation.mod.common.item.armor.ItemArmorDeci && human != null)
        {
            net.decimation.mod.common.item.armor.ItemArmorDeci a = (net.decimation.mod.common.item.armor.ItemArmorDeci) item;
            if (a.modelName != null && (a.armor == null || a.armor.kw == null || !a.armor.kw.path.contains(a.modelName)))
            {
                deci.n.f model = gearModel("models/armor/full/" + a.modelName);
                if (model != null)
                {
                    a.armor = new deci.n.e(human);
                    a.armor.kw = model;
                    return true;
                }
            }
        }
        return false;
    }

    /** deci:<path>.bmodel loaded by BModelLoader when the jar has it (the loader prints a stack trace for a missing one). */
    private static deci.n.f gearModel(String path)
    {
        net.minecraft.util.ResourceLocation r = new net.minecraft.util.ResourceLocation("deci", path + ".bmodel");
        try
        {
            net.minecraft.client.Minecraft.getMinecraft().getResourceManager().getResource(r).getInputStream().close();
        }
        catch (Exception e)
        {
            return null;
        }
        return deci.n.g.a(r);
    }

    /** The model human NPCs are drawn with (ModelHuman deci.q.h, the "Human" renderer's main model), or null. */
    public static net.minecraft.client.model.ModelBiped humanModel()
    {
        try
        {
            @SuppressWarnings("unchecked")
            Class<? extends net.minecraft.entity.Entity> c = (Class<? extends net.minecraft.entity.Entity>)
                net.minecraft.entity.EntityList.stringToClassMapping.get("Human");
            Object r = net.minecraft.client.renderer.entity.RenderManager.instance.getEntityClassRenderObject(c);
            Object m = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                net.minecraft.client.renderer.entity.RendererLivingEntity.class,
                (net.minecraft.client.renderer.entity.RendererLivingEntity) r, "mainModel", "field_77045_g");
            return m instanceof deci.q.h ? (net.minecraft.client.model.ModelBiped) m : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** The jar Decimation's classes and assets are read from (the release's one jar, the dev deobf jar), or null. */
    public static java.io.File decimationJar()
    {
        try
        {
            java.net.URL u = deci.n.f.class.getProtectionDomain().getCodeSource().getLocation();
            String s = u.toString();
            if (s.startsWith("jar:"))
            {
                s = s.substring(4, s.indexOf("!/") > 0 ? s.indexOf("!/") : s.length());
            }
            java.io.File f = new java.io.File(new java.net.URI(s));
            return f.isFile() ? f : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // ---- feature agent (branch bugfix)

    /**
     * A Decimation gas mask (ItemRegistry gasmask apo, gasmaskmilitary app): the masks PlayerData.tick spares from the zone
     * damage and IngameHud calls "Protected".
     */
    public static boolean isGasMask(ItemStack stack)
    {
        return stack != null && (stack.getItem() == deci.aD.k.apo || stack.getItem() == deci.aD.k.app);
    }

    /** PlayerData.getMask (cx): the stack in Decimation's mask slot (the same object the gear screen's slot holds), or null. */
    public static ItemStack playerMask(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? null : data.cx();
    }

    /**
     * PlayerData.markDirty (df): on the server, the player's data (conditions, backpack, vest and mask with their tags) goes
     * to the client with PacketSyncPlayerData at its next tick; nothing on the client.
     */
    public static void markPlayerDataDirty(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.df();
        }
    }

    /** ItemRegistry.itemScrapMilitaryGasMaskFilter (amW): Decimation's "Gas Mask Filter" (deci:ItemScrapMilitaryGasMaskFilter). */
    public static Item gasMaskFilter()
    {
        return deci.aD.k.amW;
    }

    /** CreativeTabRegistry.axl ("tabDeciTools"): the creative tab Decimation's medical items (pills, syringes) are in. */
    public static net.minecraft.creativetab.CreativeTabs medicalTab()
    {
        return deci.aD.r.axl;
    }

    /**
     * PlayerData.isLongTimerElapsed (cj): 8 s since its last markActionTime; Decimation's medical items play their "open"
     * sound only then.
     */
    public static boolean actionTimerElapsed(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data != null && data.cj();
    }

    /** PlayerData.markActionTime (cq): starts that timer again. */
    public static void markActionTime(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        if (data != null)
        {
            data.cq();
        }
    }

    /** TearGasGrenadeEntity (deci.ak.p): Decimation's tear gas grenade. */
    public static Class<? extends net.minecraft.entity.Entity> tearGasClass()
    {
        return deci.ak.p.class;
    }

    /** TearGasGrenadeEntity.active (acz): settled and giving off its gas (both sides; 600 ticks, then gone). */
    public static boolean tearGasActive(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ak.p && ((deci.ak.p) e).acz;
    }

    /** A new TearGasGrenadeEntity (deci.ak.p) with no thrower, never exploding (dev tests). */
    public static net.minecraft.entity.Entity newTearGas(net.minecraft.world.World world)
    {
        return new deci.ak.p(world);
    }

    /** As addLootLike, this many copies in every pool holding `like`. Returns the pools changed. */
    public static int addLootCopies(Item like, net.minecraft.item.ItemStack add, int copies)
    {
        int n = 0;
        for (int i = 0; i < copies; i++)
        {
            n = addLootLike(like, add);
        }
        return n;
    }

    /**
     * A new Decimation mob by the place table's name (fixes/PlaceSpawns): infected (InfectedEntity deci.ag.d), infected_dog
     * (deci.ag.g), crawler (f), hulk (j), frail (h), bloater (e), demon (DemonEntity deci.ag.b), soldier (SoldierEntity
     * deci.ag.l), bandit (BanditEntity deci.ag.a), soviet (SovietEntity deci.ag.m); null for any other name.
     */
    public static net.minecraft.entity.EntityLiving newMob(String name, net.minecraft.world.World world)
    {
        if (name == null)
        {
            return null;
        }
        switch (name)
        {
            case "infected": return new deci.ag.d(world);
            case "infected_dog": return new deci.ag.g(world);
            case "crawler": return new deci.ag.f(world);
            case "hulk": return new deci.ag.j(world);
            case "frail": return new deci.ag.h(world);
            case "bloater": return new deci.ag.e(world);
            case "demon": return new deci.ag.b(world);
            case "soldier": return new deci.ag.l(world);
            case "bandit": return new deci.ag.a(world);
            case "soviet": return new deci.ag.m(world);
            default: return null;
        }
    }

    /** The place table's name of a Decimation mob (newMob's names; "hazmat" for a hazmat soldier), or null. */
    public static String mobName(net.minecraft.entity.Entity e)
    {
        if (e instanceof deci.ag.g) return "infected_dog";
        if (e instanceof deci.ag.f) return "crawler";
        if (e instanceof deci.ag.j) return "hulk";
        if (e instanceof deci.ag.h) return "frail";
        if (e instanceof deci.ag.e) return "bloater";
        if (e instanceof deci.ag.b) return "demon";
        if (isPlainInfected(e)) return "infected";
        NpcKind k = npcKind(e);
        return k == null ? null : k.name().toLowerCase();
    }

    /** LootTableRegistry.hasPool (deci.aD.l.d): a right click on this block rolls Decimation's loot (crates, wrecks, bins). */
    public static boolean hasLootPool(net.minecraft.block.Block block)
    {
        return block != null && deci.a.b.a().f().d(block);
    }

    /**
     * The weight LootPool.fillInventory (deci.aB.c) gives a loot entry: its item's getLootChance as a LootChanceItem
     * (deci.ao.d, Decimation's described items: 0.5), OwnedItem (deci.ao.c, guns) or ItemArmorDeci, else 0.9.
     */
    public static double lootWeight(ItemStack stack)
    {
        Item i = stack == null ? null : stack.getItem();
        if (i instanceof deci.ao.d)
        {
            return ((deci.ao.d) i).getLootChance();
        }
        if (i instanceof deci.ao.c)
        {
            return ((deci.ao.c) i).getLootChance();
        }
        if (i instanceof net.decimation.mod.common.item.armor.ItemArmorDeci)
        {
            return ((net.decimation.mod.common.item.armor.ItemArmorDeci) i).getLootChance();
        }
        return 0.9;
    }

    // ---- gun packages (fixes/gunpack, docs/superpowers/specs/2026-10-22-gun-converter-design.md): guns registered from
    //      gun.json with the stats and behaviour of a "like" gun. GunItem = deci.ay.i, GunStats = deci.ay.e (FireMode
    //      deci.ay.e$a), AmmoItem = deci.ay.f (AmmoFeedType deci.ay.a), BModel = deci.n.f, GunAnimation = deci.F.a

    /**
     * A gun like another: a copy of the like gun's GunStats (recoil pitch adl / yaw adm, recovery adn, fire modes adp and
     * their params adq, aim table adk, secondsPerShot ads, manufacturer adr, flags adt .. adw, particles adx .. adz,
     * forced tracer adA / adB) with the overrides set, then its damage (aew copied: setDamage halves, so an override goes
     * through setDamage am like every Decimation registration), setMovementSlowdown (f), setCapacity (al), setLootChance
     * (g, Decimation's crate weight), integral suppressor (fH), no fire sounds (fI), reduced bass (private aey, fP),
     * launcher ammo (aex). Not the integrated scope: that belongs to the like gun's model. Registers as deciworldgen:NAME
     * (the running mod), its sounds guns/CATEGORY/NAME/... and, on the client, loads models/guns/CATEGORY/NAME.bmodel.
     */
    public static Item newGunLike(String name, String category, Item like, net.decimation.fixes.gunpack.GunPack.Stats over,
                                  Item... mags)
    {
        deci.ay.i l = (deci.ay.i) like;
        deci.ay.e src = l.aeq;
        float pitch = over.pitch != null ? over.pitch : src.adl;
        float yaw = over.yaw != null ? over.yaw : src.adm;
        float recovery = over.recovery != null ? over.recovery : src.adn;
        deci.ay.e.a[] modes;
        int[] params;
        if (over.auto == null)
        {
            modes = src.adp.clone();
            params = src.adq.clone();
        }
        else
        {
            modes = over.auto ? new deci.ay.e.a[] {deci.ay.e.a.AUTO, deci.ay.e.a.SINGLE} : new deci.ay.e.a[] {deci.ay.e.a.SINGLE};
            params = src.adq.length == modes.length ? src.adq.clone() : new int[] {1, 1};   // as newGun
        }
        float[][] aim = new float[src.adk.length][];
        for (int i = 0; i < aim.length; i++)
        {
            aim[i] = src.adk[i].clone();
        }
        deci.ay.e s = new deci.ay.e(new float[] {pitch, yaw}, modes, params, aim, recovery, over.rpm != null ? over.rpm : 600f);
        if (over.rpm == null)
        {
            s.ads = src.ads;   // exact: not through rounds a minute and back
        }
        s.adr = src.adr;
        s.adt = src.adt;
        s.adu = src.adu;
        s.adv = src.adv;
        s.adw = src.adw;
        s.adx = src.adx;
        s.ady = src.ady;
        s.adz = src.adz;
        s.adA = src.adA;
        s.adB = src.adB;
        deci.ay.i g = new deci.ay.i(name, s, deci.ay.c.valueOf(category), mags);
        if (over.damage != null)
        {
            g.am(over.damage);
        }
        else
        {
            g.aew = l.aew;
        }
        g.f(over.slowdown != null ? over.slowdown : l.m((ItemStack) null));
        g.al(l.getCapacity());
        g.g(l.getLootChance());
        if (l.fL())
        {
            g.fH();
        }
        if (privateFlag(l, "aeA"))   // noFireSounds (its reader fK is private)
        {
            g.fI();
        }
        if (privateFlag(l, "aey"))   // reducedBass
        {
            g.fP();
        }
        g.aex = l.aex;
        return g;
    }

    /** A private boolean of GunItem: aey reducedBass, aeA noFireSounds. */
    private static boolean privateFlag(deci.ay.i g, String field)
    {
        try
        {
            java.lang.reflect.Field f = deci.ay.i.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.getBoolean(g);
        }
        catch (ReflectiveOperationException e)
        {
            return false;
        }
    }

    /** What a gun is made of, readable, for comparing a package's gun with its like gun (dev test gunpack). */
    public static java.util.Map<String, Object> gunFacts(Item gun)
    {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        if (!(gun instanceof deci.ay.i))
        {
            return m;
        }
        deci.ay.i g = (deci.ay.i) gun;
        deci.ay.e s = g.aeq;
        m.put("category", g.aes == null ? null : g.aes.name());
        m.put("pitch", s.adl);
        m.put("yaw", s.adm);
        m.put("recovery", s.adn);
        m.put("secondsPerShot", s.ads);
        StringBuilder modes = new StringBuilder();
        for (deci.ay.e.a mode : s.adp)
        {
            modes.append(modes.length() > 0 ? ", " : "").append(mode.name());   // its toString keeps the name too ("PUMP Action")
        }
        m.put("modes", "[" + modes + "]");
        m.put("params", java.util.Arrays.toString(s.adq));
        m.put("aim", java.util.Arrays.deepToString(s.adk));
        m.put("damage", g.aew);
        m.put("slowdown", g.m((ItemStack) null));
        m.put("capacity", g.getCapacity());
        m.put("lootChance", g.getLootChance());
        m.put("suppressed", g.fL());
        m.put("noFireSounds", privateFlag(g, "aeA"));
        m.put("reducedBass", privateFlag(g, "aey"));
        m.put("launcher", String.valueOf(g.aex));
        m.put("manufacturer", s.adr);
        m.put("particles", s.adx + "/" + s.ady + "/" + s.adz);
        m.put("flags", s.adt + "/" + s.adu + "/" + s.adv + "/" + s.adw);
        m.put("tracer", s.adA + "/" + s.adB);
        StringBuilder mags = new StringBuilder();
        for (deci.ay.f a : g.aep == null ? new deci.ay.f[0] : g.aep)
        {
            mags.append(mags.length() > 0 ? "," : "").append(net.minecraft.item.Item.itemRegistry.getNameForObject(a));
        }
        m.put("mags", mags.toString());
        return m;
    }

    /** True for a magazine (AmmoItem deci.ay.f fed as a magazine, AmmoFeedType mag). */
    public static boolean isMagazine(Item item)
    {
        return item instanceof deci.ay.f && ((deci.ay.f) item).adE == deci.ay.a.mag;
    }

    /** The round a magazine loads (AmmoItem.bulletItem adI), or null. */
    public static Item magazineBullet(Item mag)
    {
        return mag instanceof deci.ay.f ? ((deci.ay.f) mag).adI : null;
    }

    /**
     * The hand poses of a gun's model (BModel kK .. kV): left hand x, y, z, right hand x, y, z, right hand rotation x, y,
     * z, left hand rotation x, y, z; null before the model is loaded. Client only (GunItem.model is client side).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static float[] gunHands(Item gun)
    {
        deci.n.f m = gun instanceof deci.ay.i ? ((deci.ay.i) gun).aeu : null;
        return m == null ? null : new float[] {m.kK, m.kL, m.kM, m.kN, m.kO, m.kP, m.kQ, m.kR, m.kS, m.kT, m.kU, m.kV};
    }

    /** Sets the hand poses of a gun's model, in gunHands' order. Client only. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean setGunHands(Item gun, float[] h)
    {
        deci.n.f m = gun instanceof deci.ay.i ? ((deci.ay.i) gun).aeu : null;
        if (m == null || h == null || h.length != 12)
        {
            return false;
        }
        m.kK = h[0];
        m.kL = h[1];
        m.kM = h[2];
        m.kN = h[3];
        m.kO = h[4];
        m.kP = h[5];
        m.kQ = h[6];
        m.kR = h[7];
        m.kS = h[8];
        m.kT = h[9];
        m.kU = h[10];
        m.kV = h[11];
        return true;
    }

    /** Parts in a gun's loaded model (BModel.parts kC: every part the file declares), -1 when none loaded. Client only. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static int gunModelParts(Item gun)
    {
        deci.n.f m = gun instanceof deci.ay.i ? ((deci.ay.i) gun).aeu : null;
        return m == null ? -1 : m.kC.size();
    }

    /**
     * Ends the gun renderer's current animation (GunItemRenderer.iNSTANCE TI .currentAnimation TM): an animation still
     * active puts the parts it names back to their rest pose when it ends, on whatever gun is drawn. Client only (tests).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void stopGunAnimation()
    {
        if (deci.K.b.TI != null)
        {
            deci.K.b.TI.TM = null;
        }
    }

    /**
     * Moves the named part of a gun's loaded model (BModel.parts kC) by (dx, dy, dz) on its rotation point, as Decimation's
     * GunAnimation poses parts (rotationPointX += ..., matched by boxName ignoring case); false when there is no such part.
     * Client only: tests of what a pose moves (dev test meshgun).
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean nudgeGunPart(Item gun, String part, float dx, float dy, float dz)
    {
        deci.n.f m = gun instanceof deci.ay.i ? ((deci.ay.i) gun).aeu : null;
        if (m == null)
        {
            return false;
        }
        for (deci.n.b p : m.kC)
        {
            if (part.equalsIgnoreCase(p.boxName))
            {
                p.rotationPointX += dx;
                p.rotationPointY += dy;
                p.rotationPointZ += dz;
                return true;
            }
        }
        return false;
    }

    /** Decimation's sound entries of a gun (SoundRegistry Ue, SoundEntry name / path): name -> path. Client only. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static java.util.Map<String, String> gunSoundPaths(String gun)
    {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        try
        {
            java.lang.reflect.Field name = deci.N.c.class.getDeclaredField("name");
            java.lang.reflect.Field path = deci.N.c.class.getDeclaredField("path");
            name.setAccessible(true);
            path.setAccessible(true);
            for (Object o : deci.a.b.a().p().Ue)
            {
                String n = (String) name.get(o);
                if (n != null && n.startsWith(gun) && n.length() > gun.length() && Character.isUpperCase(n.charAt(gun.length())))
                {
                    out.put(n, (String) path.get(o));
                }
            }
        }
        catch (Exception e)
        {
            out.put("error", e.toString());
        }
        return out;
    }

    /** True when Decimation finds the gun's animation (GunAnimation.get b: animations/NAME/NAME<kind>.anib). Client only. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static boolean gunAnimationFound(String gun, String kind)
    {
        return deci.F.a.b(new net.minecraft.util.ResourceLocation("deci", "animations/" + gun + "/" + gun + kind + ".anib")) != null;
    }
}
