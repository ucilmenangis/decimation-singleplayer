# Decimation architecture notes (package by package)

## deci/aK: server event handlers (event)

All classes except LootInteractHandler (k) are registered only in ServerProxy `deci/a/e`, and every handler method carries `@SideOnly(Side.SERVER)`, so none of them run in singleplayer. k is also registered in DecimationMod `deci/a/b` and ClientProxy `deci/a/c` in the decompiled copy (this is the user's earlier patch) and its method has no @SideOnly.

- a BlockBreakProtectionHandler, b BlockPlaceProtectionHandler: config enableBlockBreaking/Placing plus clan turf rules (sandbags and metal walls only inside your own turf). b also forbids placing within 3 blocks of an elevator (deci.U.b).
- d EntitySpawnZoneHandler: infected spawning in MILITARY/POLICE zones gets 25% chance of that variant (InfectedEntity.af), hostiles in SAFEZONE are cancelled, demons (deci.ag.b) capped at maxDemonCount.
- e ItemPickupHandler: bottlecap items become currency on pickup. Also cancels all non creative pickups when side isServer.
- g SafezoneAttackHandler, i FriendlyFireHandler: safezone and clan/ally damage protection, safezone grace timer (enableSafezoneGrace, player data dn()).
- h DeathStatsHandler: sends Request_FromClient_AddKill/AddDeath/AddZombieKill over the kryonet client `b.a().e().aBX` (dead BoehMod backend; sendTCP on a disconnected client fails silently). Pays player and clan bounties.
- k LootInteractHandler: right click block, not creative, server world. Flow: LootCooldownRegistry `deci.aB.e.d(world)` k(x,y,z) ready check, pool lookup `b.f().c(block)` (deci.aD.l loot table), l(x,y,z) mark looted, fill InventoryBasic(18), play pool sound, store in player data, send `deci.aE.a$R` to client, schedule openGui after 2 ticks through `net.decimation.mod.common.utils.b`. Multiblock props (deci.W.b) redirect to origin tile. Blocks of type deci.aB.b get a callback when the player closes the GUI. Also blocks vanilla workstations unless enableVanillaBlocks, and blocks vanilla chests unless disableLootSystem.
- l PlayerLoginHandler: spawns a thread that sends Request_FromClient_Supporter_Check to the backend and reloads clans/zones/turfs from disk; combat log kill on login if player data cf() > 0.
- m PlayerZoneTickHandler: per player zone flags (safezone, BORDER damage when enableWorldBorder, SCARY, RADIATION).
- n ChatHandler: radio chat (ItemRadio frequency NBT) and `#` clan chat. [guess] bug: the radio loop returns after the first matching receiver, so only one other radio holder hears each message.
- o ServerCommandRegistrar: Handler_Commands is only registered here, so /deci commands do not exist in singleplayer.
- q ServerTickHandler: drains the scheduled task queue (needed by k's delayed GUI open; dead in singleplayer, which was the loot GUI bug), dynamic weather, anti dupe sweep (enableAntiDupe), backend reconnect thread every Xm ticks, trader restock every 3000 ticks (`net.decimation.mod.server.traders.a.f`), supply drop countdown (deci.aM.c.ha), first tick downloads server-icon.png from boehmod.net.

Singleplayer gaps: everything above. Notable consequences: no supply drops, no trader restock, no dynamic weather, no scheduler tick, no safezone/radiation/border logic, no /deci commands, no bottlecap pickup conversion.

## deci/aJ: server config (config)

- a ServerConfigLoader: init() writes `decimation_server.properties` with defaults if missing, loadProperties() (C) copies every key into ServerConfig statics. Note the defaults in code and the written file disagree for some keys (allowRandomSpawns default false in file vs true when key missing; enableCommunityServerList similar; forcedAddress falls back to "false").
- b ServerConfig: all toggles (enableBlockBreaking, disableLootSystem, lootRespawnTime 900, enableSupplyDrops, enableDynamicWeather, maxDemonCount ...), zoneList (aAc, ObjectZoneList), turfList (aAd), supply drop countdown (aAg, interval aAf = 18000 ticks), trader restock countdown (aAS, reset to 3000), currentWeather (bZ, a deci.aI.a weather type), networkConnected (bp).
- Loader is constructed in ServerProxy (deci/a/e field N, getter y()), so in singleplayer the static defaults in ServerConfig apply, which happen to be fairly permissive (enableBlockBreaking true, enableBlockPlacing true).

## deci/Q: player data and constants (player, config, core)

- b PlayerData (IExtendedEntityProperties "decimation.player_data"), get via `PlayerData.get(player)` (obf `e`). Holds conditions (brokenLeg, infected, bleeding, irradiated, scary), water 0..20 (thirst), blood 0..20, humanity 0..100 (HumanityRank tiers hero V to bandit V), bottlecaps and gold bottlecaps (max 1,500,000), bounty, stance (0 stand, 1 crouch, 2 prone, with movement speed modifier "decistance"), recoil (recoilPitch/Yaw/Recovery applied to player rotation each tick), extra armor slots backpack/vest/mask, banker items (24 stacks), loot inventory, target range scores, clan name, kills/deaths/zombie kills/days survived, typing indicator, supporter cape.
- Sync: every setter calls markDirty (df), the server tick then sends `deci.aE.a$Y` to the owner and `deci.aE.a$Z` to trackers. inventorySyncTimer (640 ticks) calls syncHotbarGunsToTrackers with `deci.aE.a$aa` when enableInventorySync.
- Server tick (tick, non remote branch): thirst every 1600 ticks (damage source deci.aD.h.akT when water is 0), infection damage akU, bleeding drains blood every 1200 ticks then damage akV, radiation damage akW unless wearing a gas mask (k.apo/k.app). This part runs in singleplayer too (it is in the shared tick).
- isServer() gates (singleplayer gaps): tickMovementAndRecoil (bx) only syncs clan humanity on a dedicated server; setBrokenLeg/setInfected/setBleeding only refuse conditions inside a SAFEZONE on a dedicated server.
- Easter egg: player "LeScooter" sneaking in creative broadcasts a fart sound.
- a ClientConfig: decimation.properties allowVanillaMonsters.
- c DeciConstants: GUI ids (GUI_LOOT = Xa used by the loot handler, GUI_KEYPAD_LOCK, GUI_ATTACHMENTS, GUI_CRAFTING, GUI_INVSEE ...), boehmod.net URLs, global death stats fed by the backend, backend reconnect timer (every 300 ticks).
- The IGuiHandler is `net/decimation/mod/common/handlers/a`.

## deci/aE: network messages (network)

`deci/aE/a` (NetworkMessages) holds every message as a nested class `X` with its handler `X$a` (handler method `a` renamed `handle`). Registered in `deci/aD/n.init()` on both sides; the Side argument there is the receiving side. Lockpick messages H/I/J have their handlers in `net/decimation/mod/common/handlers/b,c,d`.

Main flows:
- Shooting: client fires, sends PacketGunFire (A) -> server consumes ammo, broadcasts PacketGunFireEffects (B) to all for sound and tracers. Client hit detection sends PacketGunHit (z) with target id and damage: server trusts the client damage value, checks the shot is under 400 ms old, rolls leg break (10%), multiplies by ItemArmorDeci.damageMultiplier for armor slots 0..2 (helmet skipped), then attacks with the "gunDeci" damage source. PacketBlockHitParticles (C) and PacketBulletRicochet (D) for block hits. Rockets/crossbows use PacketFireProjectileWeapon (Q). Reload steps: PacketReload (E).
- Player state: PlayerData.sync sends PacketSyncPlayerData (Y, owner) and PacketSyncPlayerDataPublic (Z, trackers); PacketSyncExtraArmor (a) for backpack/vest/mask. Key actions use PacketPlayerAction (P) with enum PlayerAction (whistle, stances, firemode, gestures, attach/craft GUI, holster, vehicle lights, unharness).
- Loot: LootInteractHandler sends PacketLootInventory (R), then the GUI opens; PacketReopenLootGui (o) reopens it.
- Traders: PacketTraderTransaction (M) buy/sell for caps or gold caps.
- Anticheat: on login (dedicated only, PlayerLoginHandler) server sends PacketRequestModHashes (h); client replies PacketModHashes (g) with mod file hashes; unknown hashes are kicked with Base64 hidden strings "BoehMod Anticheat" / "You've been kicked for having modified files!".
- Admin screenshot: PacketRequestScreenshot (ae) -> client sends PacketScreenshotChunkToServer (af) chunks -> server can forward via PacketScreenshotChunkToClient (ad) to the requesting admin.
- Zones: `net/decimation/mod/server/zones/b.refreshZones` sends PacketZoneLists (ax) with zone boxes per type.

Suspected bugs / oddities:
- PacketBulletRicochet (D) constructor only stores the entity id; position and surface type are always 0, so the bounce sound plays at 0,0,0 with the water sound.
- PacketGunHitHandler: the helmet branch for infected (`random.nextFloat() > 1.0f`) can never be true, so helmets on zombies never reduce damage.
- PacketApplyPoison (ak) and PacketBlockHitParticles (C) accept arbitrary entity ids/positions from any client.
- PacketThrowItem (y) only blocks throwing near safezones when isServer() is true (dedicated); fine for singleplayer, but note the isServer pattern.
- PacketClimbState, PacketSetCosmetics and others write PlayerData from any client without validation.
- Several S2C messages (c, d, e, f, x, v, S, T, U) have no sender found in the decompiled source [guess: leftovers or sent from code paths not yet searched].

## deci/aF: network channel (network)

- `deci/aF/a$a$a` NetworkChannel: the single SimpleNetworkWrapper, channel name "ctx", accessed everywhere as `a.a.a.gB()` (getChannel). The outer classes are empty IASMHook implementations that do nothing (decoys).

## deci/aD: registries (core, item, block, entity, loot)

- Every registry implements Registrable (`deci/aD/a`, init()). BlockRegistry (c) calls PropBlockRegistry (g), BuildingBlockRegistry (d), SoundBlockRegistry (f), DecalRegistry (e). ItemRegistry (k) registers ~585 items: ammo calibres (registerAmmoCalibres, damage per calibre set with `.y(float)`), magazines, ammo boxes, guns in registerGuns (each gun gets a `deci.ay.e` stat block: recoil pair, fire modes, ... , fire rate; the trailing `.I(String)` is the manufacturer text), armor (ItemArmorDeci etc.), food, drinks, medical, tools. Field names in the map are derived automatically from each item's unlocalized name (confidence M).
- SkinRegistry (q): weapon skin items per gun; CreativeTabRegistry (r): all tabs; EntityRegistryDeci (i): entity names (Infected, Bandit, Soldier, every Trader type, vehicles ATV/Hummer/Buggy/M35/BTR70, grenades, rockets, traps, Turret, PlayerSeat); TileEntityRegistry (s); MessageRegistry (n); DeciDamageSources (h); ArmorMaterials (b); PriceTable (p, tier 0..7 -> 10..200 caps); RecipeRegistry (o).
- Entity class names from the registry (useful for other packages): ag.d Infected, ag.f InfectedCrawler, ag.j Hulk, ag.g InfectedDog, ag.h Frail, ag.e Bloater, ag.i Gawker, ag.b Demon, ag.k Mech, ag.a Bandit, ag.l Soldier, ag.m Soviet, ag.c Hazmat, ah.c Doggo, ah.a Boar, ah.b Buck, ah.d Human, ai.* traders (ai.e is TraderBanker, not the generic trader), ae.a Corpse, ac.a FallingSupplyDrop, ad.e Vehicle, ad.c VehicleSeat, aj.* traps/campfire/mortar, ak.* grenades and rockets, al.b Turret, al.a PlayerSeat.
- LootTable (l): hardcoded. buildEntries (gh) creates 12 anonymous ItemStack lists (`l$1`..`l$12`) and binds them to blocks with a roll count and a sound key ("loot.metal", "loot.wood", "loot.garbage"); vanilla chests use pool 1 with 3 rolls. init() turns entries into `deci.aB.c` pools keyed by Block; getPool (c) is what LootInteractHandler calls. The supply drop block gets 8 rolls from pool 11.

## deci/aB: loot runtime (loot)

- IntRange (a): `upTo(n)` builds [1, n) and pick returns lower + nextInt(upper - lower). Consequence: a pool with N rolls actually rolls 1..N-1 items, and an entry with stack size S gives 1..S-1 items (S = 1 gives 1). [guess: off by one, probably unintended]
- LootPool (c).fillInventory: per roll, weighted pick of an entry (weight = item getLootChance for deci.ao.d / deci.ao.c / ItemArmorDeci, else 0.9), placed into a random still free slot via a bitmask; leftover slots set to null. If rolls ever exceeded the slot count (18) the free slot loop would spin forever.
- LootCooldownRegistry (e): static per dimension, positions packed into a long (26 bit x, 12 bit y, 26 bit z), value = last looted ms; isReady when now - last >= ServerConfig.lootRespawnTime (default 900 s). Block type agnostic, in memory only (cleared on restart, not saved).
- LootCloseCallback (b): blocks implementing it get a callback once the player closes the loot GUI.

## deci/ay: weapons (weapon)

- GunItem (i) extends LootTrackedItem (deci/ao/c) and implements HeavyItem (deci/ao/a). Constructor registers the item, the IItemRenderer `deci.K.b` on the client, all per gun sounds (Fire, FireDistant, FireSuppressed, MagIn/MagOut/Rack, LoadShell, Pump) and loads `deci:models/guns/<category>/<name>.bmodel` plus the casing model.
- Firing is client authoritative: clientHeldTick (d) polls the mouse every tick (static leftMouseDown/rightMouseDown, right click toggles aim mode PlayerData VI), checks secondsPerShot and calls fire (a(ItemStack,EntityPlayer)) -> fireRay per pellet: builds a ray from getShotOrigin (eye height lowered by stance) along getShotDirection (spread), tests every loaded entity's bounding box and rayTraceBlocks (with glass/vine/flowers etc. pass through), then sends PacketGunHit (z) with the target id and `damage` (field aew), PacketBlockHitParticles (C) for block hits, and PacketGunFire (A). Rockets and crossbows send PacketFireProjectileWeapon (Q) instead.
- setDamage (am) is the single chokepoint for gun damage; in this copy it stores round(n * 0.5) (user's balance patch). Ammo calibre damage (AmmoItem.setDamage `y`) exists separately but hit damage uses GunItem.damage.
- onServerFire (b) alerts infected within 64 blocks (10 when suppressed) via alertNearbyInfected.
- applyRecoil (c) adds recoilPitch/Yaw from GunStats into PlayerData, reduced when crouching (/1.5) or prone (/2.4).
- Ammo NBT keys: ammo, ammoType, mode, bolted, prevAmmo, firing, mgItem, gripAttach, sightAttach, stockAttach, barrelAttach, triggerAttach, skin, skinEng, mud, blood, flashTime, tracerColorID.
- Bug: setTracerColor (a(ItemStack, TracerColor)) writes "tracerColor" but getTracerColor reads "tracerColorID" (the attachment GUI `deci/g/s` writes the right key, so only this helper is broken).
- Performance note [guess]: fireRay walks the full loaded entity list per pellet (shotguns up to 6+ pellets) on the client, and rayTraceBlocks is a custom voxel walk; heavy entity counts make every shot O(entities).
- GunStats (e): constructed in ItemRegistry.registerGuns as (float[]{recoilPitch, recoilYaw}, FireMode[], int[] per mode params, float[][] aim table, recoilRecovery, roundsPerMinute); secondsPerShot = 1 / (rpm * 1.3 / 60).
- AmmoItem (f) covers single bullets and magazines (magazines hold "ammo" NBT and are refilled by right click with loose bullets). AmmoBoxItem (g) hands out bullets. AttachmentItem (h): scopes (zoomFov, sway), suppressors (isSuppressor), flashlight. WeaponSkinItem (j) and TracerItem (k) are cosmetic accessories.

## deci/ao: item base classes (item)

- HeavyItem (a, interface): getMovementSlowdown(ItemStack) and shouldSlowPlayer(EntityPlayer, ItemStack); the client movement code in deci/c/c multiplies player motion by (1 - slowdown).
- AmountItem (b): item with an "amount" NBT value up to a max (jerry can, vehicle related items); refill adds 20, consume subtracts.
- LootTrackedItem (c): lootChance plus, on first update in a player inventory, NBT "originOwner" and "uniqueID" (the anti dupe key, see ServerTickHandler).
- LootChanceItem (d): plain Item with lootChance only.

## deci/ak: projectiles and explosives (entity)

- GrenadeEntity (c) is the thrown grenade base (fuse ticks, bounce, explodeOnImpact for launcher grenades); subclasses MolotovEntity (d), SignalGrenadeEntity (k, with green l / red m), SmokeGrenadeEntity (n), TearGasGrenadeEntity (p). StunGrenadeEntity (o) is a separate flashbang that triggers PacketFlashbangDetonate (C2S, so the detonation is reported by the client) [guess from the message handler]. GrenadeEntity.explode broadcasts PacketExplosionSound within 128 blocks.
- Rockets: RocketEntity (f) base, HeatSeekingRocketEntity (g, steers toward nearest entity within 80), NuclearRocketEntity (h, rpg18RocketNWH), TargetRocketEntity (i, flies to a set position). MortarRoundEntity (e), TankShellEntity (j), CrossbowBoltEntity (b, EntityArrow copy).
- DeciExplosion (a): copy of vanilla Explosion with plant/ladder pass through.
- Spawned server side from PacketFireProjectileWeapon (rockets, bolts, launcher grenades) and PacketThrowItem (hand thrown grenades).

## deci/aj: placeable trap entities (entity)

- BarbedWireEntity (a), CampFireEntity (b), LandmineEntity (c, detonate fe = createExplosion 4.0), MortarGunEntity (d, rideable), PunjiSticksEntity (e). EntitySpawnZoneHandler cancels them (with spawn of hostiles) inside safezones, server only. A gun hit on a landmine detonates it (PacketGunHitHandler calls fe()).

## deci/ah: animals and the human NPC base (entity)

- BoarEntity (a), BuckEntity (b) animals; DoggoEntity (c): EntityWolf subclass with names from a fixed list, NBT dogName/dogSkin, harness item slot, owner alerts every 800 ticks (wounded warning, "can smell an unknown human"). [guess bug] the sniff check skips when the owner IS in a clan (`!owner.getClanName().equals(UNKNOWN_CLAN)`), so clan members never get the alert.
- FactionHumanEntity (d): extends HumanEntity (deci/af/d); registered as the civilian "Human" and also the base of BanditEntity, SoldierEntity, HazmatSoldierEntity and every trader (deci/ai/a). Adds faction (NpcFaction deci/ae/b, CIVILIAN default) and a hostile flag (setHostile(true) in bandits, soldiers, traders). Every tick it scans the whole loadedEntityList for armed entities within 32 blocks [performance: O(entities) per human per tick].

## deci/am: small enums (item, entity, trader)

- HydrationRating (a) and NutritionRating (c) are tooltip ratings on food (FoodItem deci/ar/a). InfectedVariant (b): COMMON/MILITARY/POLICE with ids used by InfectedEntity.af(int). TraderClientele (d): COMMON/BANDIT/HERO, which humanity group a trader serves.

## deci/an: shared event handlers (event)

All registered unconditionally in DecimationMod (deci/a/b), so they run in singleplayer. Still, several contain inner `isServer()` gates:
- DeathHandler (f): humanity gain/loss on kills and clan score changes only run when isServer() (dedicated). In singleplayer killing infected/bandits never changes humanity. Corpse spawning is skipped on a dedicated server when enableCorpses is false; in singleplayer it always spawns. The corpse takes 1..80 percent of the player's caps.
- CombatHurtHandler (h): combat log timer (setCombatTimer 300) is set on client side or on dedicated server with enableCombatLogging; bleeding chance from PvP gunshots on a dedicated server respects clanmates and enableBleeding, otherwise a flat 20 percent. Bow/crossbow friendly fire check and melee clan check only on dedicated server. Melee weapons (deci.az.b) subtract their damage directly with setHealth (bypasses armor). Shooting within 2 blocks adds blood dirt to the gun.
- RespawnHandler (l): clears the inventory on respawn (items go to the corpse) and sends Request_FromClient_Supporter_Check to the dead backend; random spawn points only on dedicated servers with allowRandomSpawns.
- PlayerTickHandler (m) drives PlayerData.tick and tickMovementAndRecoil; it saves and restores the naturalRegeneration gamerule around the tick.
- EntityJoinHandler (e): 10 percent of infected spawns become a Bloater (30 percent) or Hulk, 5 percent of soldiers become a Mech, infected spawning where lightning can strike may become a Frail.
- WorldTimeWrapHandler (p) resets world time to 0 after 24000, so getWorldTime never passes one day.
- LootCooldownResetHandler (n) clears the loot cooldowns on server start (so cooldowns never persist across restarts).
- ItemEntityInteractHandler (d): right click an item entity to pick it up (works even when ItemPickupHandler blocks normal pickup on servers).

## deci/aL: legacy handler (event)

- LegacyServerEventHandler (a): a single class that contains the same handlers later split into deci/aK (method names mirror the aK ones). Never registered, dead code. Its kickForCheating helper ("Decimation Anticheat") is the only extra logic.

## deci/aN: anticheat and helpers (misc, util)

- AntiCheatScanner (a): collectModHashes hashes every file in mods/ (or mods/1.7.10/) into modHashes/modFileNames (sent to the server in PacketModHashes); detectCheats runs `tasklist.exe` or `ps` and looks for cheat engine / process hacker, checks ~/xenobyte, config/ehacks/cheat.json and "Cheating Essentials" folders, and treats a LiteLoader mod name as cheating. Kicks only happen on a dedicated server (PlayerLoginHandler is server only), so singleplayer is unaffected.
- WeightedRandomCollection (b) duplicates deci/aB/f.
- UuidFetcher (c): name to UUID via https://mcapi.ca (dead). Lookups queue names and return null until a background batch resolves them, so anything relying on it silently gets null now.

## deci/b: client state and helpers (core, hud, render, config)

- ClientState (i): huge static holder. Options from decimation_client.properties (ClientConfigLoader f): isModpack, disableCustomMainMenu, disableCustomPlayerList, disableUpdates, enableFancyNameplate, enableOldGui, enableDiscordRP, enableServerList, enableWristDisplay. Server pushed data: zone boxes per type (PacketZoneLists), turf list, clan lists, weather (currentWeather, fog density), flash intensity (flashbang/nuke), camera shake. Profile data saved to profile_data_<username>.json.
- PERFORMANCE BUG (verified in code): SmoothSwingThread (h) is started from ClientProxy (deci/a/c, thread name "SmoothSwingThread") and runs `while(true)` accumulating time without any sleep or yield, so it pins one CPU core at 100 percent for the whole session. It only increments ClientState.animTicks (used for weapon sway). A fix is to add Thread.sleep(1..16) in the loop or drive animTicks from the render tick.
- ToastNotification (b) and TipPopup (d) are the sliding HUD popups (tips remembered in a local file). UpdaterLauncher (e) launches deci-updater.jar (dead BoehMod updater). ClientResources (c) holds texture/sound locations and BoehMod URLs.

## deci/c: client handlers (event, render, player, sound)

- KeyBindingHandler (a): all key bindings; each maps to a PacketPlayerAction. Middle mouse = gun bash (melee with the held gun, bayonet sound if the bayonet barrel is attached).
- ClientRenderHandler (b): render tick work.
  - SCOPE FPS DROP (verified in code): renderScopeView (`a(Float)`) is called every render frame whenever the player holds a GunItem that has a sight attachment or an integrated scope and no GUI is open, aiming or not. It calls `mc.entityRenderer.renderWorld(...)` a second time at up to 1024x1024 with a narrowed FOV and copies the result into a texture (glCopyTexImage2D). That doubles the world render cost while such a gun is held. Possible fixes: only render when PlayerData aim mode (VI) is on, skip frames, or lower the resolution.
  - Admin screenshot capture (glReadPixels) only when PacketRequestScreenshot is received.
  - Swaps in a custom EntityRenderer (deci.E.a) while crouched/prone for camera height.
- ClientEventHandler (c): the main client handler. updateDynamicLights (N) does block light updates around players holding a flashlight gun or in a vehicle with lights every other frame (world.updateLightByType), another potential FPS cost in busy areas. HUD overlays for water/blood/conditions, fog density from weather, item tooltips, client tick (537 lines) for menus, pickup ray (getEntitiesWithinAABB along the look vector), vehicle input.
- WorldRenderLayerDispatcher (d) runs the deci/M render layers. AmbientMusicPlayer (e).

## deci/d: menu GUI toolkit (gui)

- DeciGuiScreen (i) base screen with GuiPanels (b). Widgets: DeciButton (d), variants (e, f), OptionButton (g), SliderButton (h), ScrollListPanel (l) with ListEntry (m) subclasses ImageListEntry (n, downloads images from URLs), TextListEntry (o), ServerListEntry (p, pings servers from the BoehMod server list). Dialogs: PopupMenuScreen (j), SupporterMenuScreen (k), TextInputDialog (q), StatusEditDialog (r), OptionToggleDialog (s). ArticleTile (c) shows backend news (empty now that the backend is gone).

## deci/e: main menu player preview (gui)

- MenuFakeWorld (d) with EmptyChunkProvider/NullSaveHandler/MenuWorldProvider nested stubs, MenuPreviewPlayer (a), MenuPreviewPlayerHolder (b). Purely cosmetic, for drawing the player on the custom main menu (deci/i). Confirms that Decimation never generates terrain.

## deci/f: containers and inventories (gui)

- GUI ids (DeciConstants) to containers: GUI_LOOT -> PlayerLootContainer (d) + LootInventory (i) (player inventory, armor, backpack/vest/mask, loot); GUI_BANKER -> BankerContainer (b) + BankerInventory (h, 24 stacks in PlayerData.bankerItems); GUI_ATTACHMENTS -> AttachmentContainer (a); GUI_CRAFTING -> CraftingContainer (c, 2x2); GUI_INSPECT_BANKER -> InspectBankerContainer (e) reads playerdata/<uuid>.dat for offline players (admin command).
- Slots: AttachmentSlot (k) validates attachments against the gun, EquipmentSlot (l) by EquipmentSlotType (f).

## deci/g: in game screens and HUD (gui, hud)

- PlayerInventoryScreen (g) is the GUI_LOOT client screen (player model, extra armor, loot slots). BankerScreen (c), InspectBankerScreen (n), TraderScreen (q) with TraderOfferEntry (r, sends PacketTraderTransaction), AttachmentScreen (a) with AttachmentInventory (s, writes attachment, skin and tracerColorID NBT onto the gun), CraftingScreen (p), LockpickingScreen (k), bounty screens (d, e, f), creative editors for decals/notes/road signs (h, l, o), ReadNoteScreen (m), RenameBackpackScreen (b).
- IngameHud (j): singleton drawing the HUD (gas mask and viewport blur overlays, conditions, zone info). BlockTip (t): floating hints.

## deci/h, deci/j, deci/k: screenshot viewer, render utils, server list (gui, render, network)

- ScreenshotViewerScreen (h/a). RenderUtils (j/a, ~70 static draw helpers), ScissorStack (j/b), TextWrapUtil (j/c).
- ServerInfo (k/a) and ServerPinger (k/b): the menu server browser lists four hardcoded official servers on eu.mcdecimation.net and us.mcdecimation.net (dead) and pings them asynchronously on the mod executor.

## net/decimation/mod/server/zones and turf (zone)

- ZoneManager (zones/b) reads decimation_zones.json into ServerConfig.zoneList. reloadZones (hk) is only called from ServerProxy init (deci/a/e) and PlayerLoginHandler/ZoneCommands, all dedicated server paths. SINGLEPLAYER GAP: zoneList stays null in singleplayer, so every isPlayerInZone/isEntityInZone check returns false. No safezones, radiation, scary, military or police zones exist in singleplayer even if the json file is present. EnumZoneType (zones/a).
- TurfManager (turf/a): clan turfs in decimation_turfs.json. readTurfsFile (hg) re-reads and parses the JSON from disk on every call, and it is called from block break/place handlers and turf checks per event [performance: disk IO per interaction on servers]. isCapturable: no owner, or owner clan has nobody inside and last capture >= 900 s ago.
- Bugs: getTurfById compares UUIDs with != (reference) so it never matches; canPlayerUseInTurf ignores the block coordinates it is given and uses the player position, and returns false for clanless players, so on a dedicated server clanless players cannot open storage crates outside safezones ("Can't open that in another clans turf!").

## net/decimation/mod/server: traders, screenshot, commands, clans (trader, player, command, clan)

- TraderSpawnManager (traders/a): every 3000 server ticks (ServerTickHandler) removes every trader entity and respawns them from decimation_traders.json. Server only, so in singleplayer traders only exist if placed by hand/spawn egg.
- ScreenshotTracker (screenshot/a): IExtendedEntityProperties per EntityPlayerMP (registered by PlayerDataRegistrar); /screenshot requests a client screenshot with a 10 s timeout that kicks the target if unanswered.
- commands: Command_Server (/deciserver: weather, supply drop spawns, player spawns, viewbanker, wipebanker, reload), Command_RSpawn, Command_Supporter; already readable, no map file. All are registered through Handler_Commands by ServerCommandRegistrar, i.e. dedicated server only.
- ClanManagerV1 (clans/a): the clan system actually used (clans/v2 ClanManager/Clan is a readable rewrite that is not wired in [guess]). Clan files clans/<name>.json plus userData.json. tick pays turf income every 18000 ticks (20 caps per owned turf). Clan lookups from PlayerData.clanName; isPlayerInClan etc. throw IOException and are wrapped everywhere.
- Bug: isMemberOnline (j) compares memberUUID with != (reference compare), so it is effectively always false.

## net/decimation/mod/common/utils and handlers (util, management, gui)

- TickScheduler (utils/b): two instances, server() (gD) and client() (gE). The server one is ticked by ServerTickHandler (dedicated only) and, in this patched copy, also by ClientProxy (deci/a/c line ~287), which is how the delayed loot GUI open works in singleplayer now. client() is ticked by ClientEventHandler.
- UpdateChecker (utils/h): every 1200 server ticks fetches https://raw.githubusercontent.com/boehmod/DecimationRelease/master/latestversion.txt on a thread. isUpdateRequired returns true when the version differs OR when mods/DecimationVoiceChat.jar is missing; ServerTickHandler then stops trying to reconnect to the BoehMod network. requestLatestVersionFromNetwork sends Request_FromClient_LatestVersion to the dead backend.
- MathUtils.newUniqueId (utils/e.gF) generates the per item "uniqueID" NBT used by the anti dupe sweep.
- FortificationBlocks.isFortification (utils/g) is the sandbag/metal wall whitelist used by the block protection handlers.
- DeciGuiHandler (handlers/a): see deci/f notes for the id to container mapping.

## deci/A, deci/B, deci/D: misc model, particles, Discord (model, render, misc)

- deci/B: DeciParticleSpawner (d) creates the custom EntityFX particles by name; the classes b..r are the individual particle types (blood, embers, smoke, explosion flash, jet flyover...). Server side code triggers them through PacketSpawnDeciParticle. Particle internal fields were not named (low value).
- DiscordPresence (D/a): Discord RPC with a 2 second polling thread "RPC-Callback-Handler" that also queries the player profile (backend data, now always null).
- ModelOpenBox (A/a) is only referenced from the unused render layer deci/M/h.

## deci/E, deci/F, deci/H: stance camera, gun animations, decal images (render)

- StanceEntityRenderer (E/a) replaces mc.entityRenderer while crouched/prone (ClientRenderHandler swaps it).
- GunAnimation (F/a): parses `.anib` text files (Length, Hand, Frame SKIP, PlaySound, Shake, Pos, STATIC). AnimationFrame flags drive gameplay: triggerUnload/triggerLoad send PacketReload (E) steps from PlayerData.clientTick, loopUntilFull jumps back while ammo remains to load (shell by shell reloads). A missing "SlideBack" file is expected and silent.
- DecalRenderer (H/a) is the TESR for TileEntityDecal (deci/S/d) and caches decal textures.

## deci/I: prop tile entity renderers (render)

- 96 TileEntitySpecialRenderers, one per prop tile entity (names derived from the bound tile entity in the client proxy registration, deci/m/c). Each holds a texture (fP) and a model instance (deci/p model classes). I.b and I.h are not bound anywhere [guess: unused].
- Performance note [guess, not profiled]: every prop is a TESR drawing a multi part ModelBase each frame (no display lists or chunk baking), so prop dense areas cost one immediate mode model render per visible prop per frame. That is the most likely cause of the prop dense FPS drop; worth profiling.

## deci/J: entity renderers (render)

- One renderer per entity type (mapping from the client proxy registerEntityRenderingHandler calls). PlayerModelSwapper (e) replaces the vanilla player model through reflection on RenderPlayer each RenderPlayerEvent.Pre to draw stances, gestures and the extra armor.

## deci/K: item renderers (render)

- GunItemRenderer (b): registered for every gun in the GunItem constructor; its static INSTANCE (TI) also holds the currently playing GunAnimation (TM) that PlayerData.clientTick drives. Smoke puffs are kept in a list and moved every tick.

## deci/L, O, P, R (core, sound, management, block)

- ClientContext (L/a), DeciPositionedSound (O/a), ShutdownHook (P/a, disposes the management server connection), ImageDownloader (P/b).
- deci/R: building blocks. Map barriers (c, d infected only, e player only), roads and road slabs, shop sign props (h..m). Bug: SignMinebayBlock (k) creates EntitySign_Mineway, so the Minebay sign renders as Mineway.

## deci/T, V, W, X: fortifications, supply drop, multiblocks (block, loot)

- Fortifications (T): concertina wire, metal wall, sandbags, packaged cocaine stack, StorageCrateBlock (T/f). These are the blocks FortificationBlocks.isFortification allows in turfs.
- SupplyDropBlock (V/a) implements LootCloseCallback (deci/aB/b): the crate destroys itself after the looter closes the GUI.
- Multiblock props (W): MultiblockTileEntity (a), MultiblockHelper (e) place/remove all parts; LootInteractHandler resolves clicks on any part to the master position before looking up the loot pool.

## deci/aG, aH, aI, aM: reflection, vectors, weather, server world features (util, world, loot, management)

- WeatherType (aI/a) with DefaultWeather (b) and StormWeather (c), chosen by weight. Bug: both are constructed with id 0, so byId(0) returns DefaultWeather and clients that receive the storm id still render the default weather.
- WeatherController (aM/d), SupplyDropSpawner (aM/c), PlayerSpawnPoints (aM/b), OfficialServerListRegistrar (aM/a, dead backend). SupplyDropSpawner methods all check isServer(), so supply drops (including the /deciserver supply drop command) never spawn in singleplayer.
- ReflectionUtils (aG/a), Vector3i and LWJGL style vector copies (aH).

## deci/aO, aP: BoehMod management backend client (management)

- BackendConnection (aP/a) holds a kryonet Client (field aBX, renamed `client`) used across the mod as `DecimationMod.getBackend().client` (`b.a().e().aBX`). DecimationMod.postInit connects to network.mcdecimation.net with a 5000 ms timeout on the main thread (dead host: adds up to 5 s to startup on DNS/timeout, then everything checks isConnected()).
- Everything that talks to it: kill/death/zombie kill stats (DeathStatsHandler), supporter checks (login, respawn), articles, server list, player count, profile polling every 300 ticks (BackendProfileCache aO/b), community server registration (aM/a, aO/d), latest version request. With the backend dead these calls are skipped or silently dropped; sendTCP on a disconnected kryonet client does nothing.
- ServerTickHandler on a dedicated server spawns a reconnect thread every 300 ticks while disconnected (unless an update is "required", which is true whenever mods/DecimationVoiceChat.jar is missing).

## deci/ad: vehicles (vehicle)

- VehicleEntity (e) base with subclasses ATV (f), BTR70 (g, turret seat), Buggy (h), Hummer (i), M35 (j). Each vehicle spawns child VehiclePartEntity (b) instances: VehicleSeatEntity (c, flags isDriver/protectPlayer/hidePlayer/showOverlay/forcePerspective), TurretSeatEntity (d), VehicleEmitterEntity (a, exhaust particles). Engine loop sounds (k, l, m).
- Vehicles despawn 900 s after creation (getDespawnSecondsLeft) [guess: unless placed by a player]. Lights toggled by PacketPlayerAction VEHICLE_LIGHTS; driver yaw synced with PacketVehicleInput/PacketVehicleYawClient.
- Protected seats cancel all damage to the rider (VehicleSeatProtectionHandler, shared) and block leg breaking.

## deci/ai: traders and quests (trader)

- TraderEntity (a) extends FactionHumanEntity; offers are registered per trader class into a static map (OFFERS_BY_CLASS) as TraderOffer (y) entries; PacketTraderTransaction validates against it. ArmedTraderEntity (v) shoots infected and bandits within 32 blocks. One subclass per trader type (registry names TraderFood, TraderGuns ...). BankerTrader (e) opens GUI_BANKER. Note: CLAUDE.md's "deci.ai.e = trader" is specifically the banker.
- Traders are spawned only by TraderSpawnManager on a dedicated server; in singleplayer use spawn eggs.
- Quest (z) with rewards and tasks appears unused [guess].

## deci/al, ap, aq, ar, as, at, au, ax, az: misc entities and items (entity, item, hud, vehicle, weapon)

- TurretEntity (al/b) shoots hostile mobs within 32 blocks; PlayerSeatEntity (al/a) for chairs.
- Door items (ap): DoorItem (a) and LockedDoorItem (f) are used; ap/b..e are legacy hardwired door items not used by ItemRegistry.
- Drinks (aq), food (ar: FoodItem base with ratings, RawMeatFoodItem and InfectedFleshItem can infect), fortification kits (at), medical items (au: bandage, blood bag, splint, syringes), vehicle placer items (ax), melee (az: chainsaw with fuel, melee weapon, shield that halves gunshot damage).
- MapRenderHelper (as/a): GPS/map overlay drawing zone and turf markers (relies on the client zone lists, which are empty in singleplayer).

## deci/n: the .bmodel format and loader (model)

KEY FINDING for custom weapon models: `.bmodel` is a plain TEXT format, not binary. BModelLoader (n/g) reads it line by line:
- Header lines `key: x, y, z;` : `mOff:` model offset (in 1/16 block), `sPos:` sight/scope position, `flamePos:` muzzle flash position, `ejectPos:` casing eject position, `lhPos:`/`lhRot:` and `rhPos:`/`rhRot:` left/right hand pose, `Scale:` overall scale, `textureWidth = N;`, `textureHeight = N;`.
- Then Java source style lines as exported by Techne / Flan's style model tools: `partName = new ModelRenderer(this, u, v);` (also accepts WW2ModelRenderer / BeardieModelRenderer), `partName.addBox(x, y, z, w, h, d);`, `partName.addShape(x, y, z, new float[][]{{...}x8}, w, h, d ...)` for 8 corner shapes, `setRotationPoint(...)`, `setRotation(part, rx, ry, rz)` (optionally `/ rotFix`), `addChild`.
- So a new gun mesh can be authored in a tool that exports a 1.7.10 Java ModelBase (Techne, Blockbench "Modded Entity" Java export) and pasted into a .bmodel with the header lines. Texture is loaded separately from deci:textures/model/guns/... [guess path; check deci/K/b].
- Verified with javap on the original class (deci/n/g, bytecode offset ~2243): the addBox branch passes the parsed y origin twice and drops the z origin, so every `addBox` box gets z origin = y origin. Existing .bmodel files were presumably authored around this quirk (or use addShape). Anyone writing new .bmodel files must account for it (or patch BModelLoader).
- BModelPart (n/b) compiles a GL display list per part, so gun rendering is reasonably cheap. BulletTracerRenderer (n/d) draws tracer lines with pass/snap sounds. DeciPlayerModel (n/e) is the player/human biped with extra parts.

## deci/p: prop and entity models (model)

- ModelBase classes with hundreds of box parts (part fields like nx, ny ... left unnamed as instructed). Names derived from the renderer or prop registry entry that instantiates them. 26 model classes are not referenced anywhere (named ModelUnused*), dead assets that still cost jar size only.

## deci/q, r, s, t, u, w: entity, placeable, projectile, vehicle and item models (model)

- ModelDeciPlayer (q/c, used by PlayerModelSwapper), ModelHuman (q/h), mob models (boar, buck, demon, doggo, infected crawler/dog, mech, corpse). Placeables (r), throwables (s), ModelATV (t/a, hard coded BModel subclass; t/b is an unused 4000 line vehicle model), propeller trap (u), handheld item models (w). Unreferenced ones are marked L.

## deci/x, y, z: melee/tool item models and muzzle flash (model, render)

- Melee item models (x), detonator and unused tool models (y), MuzzleFlashModel (z/a) used by GunItemRenderer.
