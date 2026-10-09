package net.decimation.fixes;

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

    // ---- player data: PlayerData = deci.Q.b

    /** PlayerData.get(player).getAimMode() (e, ci): 1 = aiming down the sights. */
    public static int aimMode(EntityPlayer player)
    {
        deci.Q.b data = deci.Q.b.e(player);
        return data == null ? 0 : data.ci();
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

    /** Measure the scope glass every (mask + 1)th frame (mask 1 = every 2nd, 7 = every 8th). */
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
        /** addHumanity (I). */
        public void addHumanity(int n) { data.I(n); }
        /** removeHumanity (J). */
        public void removeHumanity(int n) { data.J(n); }
        /** getBounty (cc). */
        public long bounty() { return data.cc(); }
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

    /** InfectedEntity (deci.ag.d). */
    public static boolean isInfected(net.minecraft.entity.Entity e)
    {
        return e instanceof deci.ag.d;
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
     * tracer along the line from its eyes to the aim point (PatchTracer v2).
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

    /** GunItem.stats (aeq).secondsPerShot (ads): the gun's own rate of fire. */
    public static double gunSecondsPerShot(net.minecraft.item.ItemStack gun)
    {
        return gun != null && gun.getItem() instanceof deci.ay.i ? ((deci.ay.i) gun.getItem()).aeq.ads : 0.1;
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
}