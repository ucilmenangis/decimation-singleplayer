# Bug Tracker - Decimation Mod

Status values: `Open` (found, not fixed) / `Fixed` / `Pending decision` (fix
known, waiting on user go-ahead).

For "known non-bugs" (behavior that looks broken but is original mod design,
not something we introduced or need to fix) see `documentation.md`.

---

## Open

### Looking down with a knife: the hand floats (reported 23 Oktober 2026 by the user, FIXED on branch feature/katana-fire-reach)
- User: "look down and you'll see its botched, the character hand is floating (left hand)", "try look-down while moving".
- Cause: our first person arm and knife (MeleeRenderer) stay in the view at any pitch while Decimation's first person
  body (arms hidden by us for knives, as Decimation does for guns) is drawn below: an arm without a shoulder over the
  body. Showing the body's arms instead brought another: walking swings its left arm forward into the view.
- Fix: MeleeMotion SLIDE / BODY: the view's arm and knife slide out from 35 to 55 degrees, past 55 the body's right arm
  holds the knife, its left arm stays hidden. Dev test `melee` look down shots at 30 / 50 / 70 / 88 while walking.
- Then (user: "in left hand you can see its gelang stayed"): Decimation's wrist display (ItemRegistry.wristTimer, on by
  default: enableWristDisplay) and its armor pieces draw their own arm parts, which skip only when the body's
  isHidden is set (Decimation resets that each draw; we hide by showModel). The floating "shoe" at 88 degrees was that
  band on the left wrist. Deci.dropArmorArms swaps the armor model's part list (BModel.kC) for a cached one without the
  arm parts for the first person body draw and puts it back after (a hidden BModelPart pushes a matrix it never pops,
  so not by its visible flag). 88 degrees while walking: no floating block left. A worn Decimation chest piece gets
  the same; not shot with one on [not verified].

### Gun durability gone from the tooltip; vests not worn by zombies, only one body piece by a shot (reported 23 Oktober 2026 by the user, FIXED on main)
- User: "for gun show durability on hover ... vest armor still doesnt affected when hit by zombie or shot. only armor
  affected, fix that."
- Tooltip cause: Decimation's ClientEventHandler.onItemTooltip (deci/c/c, NORMAL priority) clears the tooltip of guns,
  magazines and food and writes its own lines. At one priority Forge calls listeners in registration order: Decimation
  registers at init, our GunWearClient at preInit, so Decimation's clear ran after our durability line and wiped it (the
  RED run: 18 lines ending with Decimation's weight line "13%"). Fix: GunWearClient.onTooltip at EventPriority.LOW, the
  gun's durability laid out as Decimation lays out its entries (a blank line, "Durability", the value in red).
- Vest causes: a shot wore only the one of vest and chest piece that counted (ArmorGunfireHandler.wear); melee wear is
  Forge's ArmorProperties.ApplyArmor, which knows only the four armor slots, while the vest lives in Decimation's player
  data. Fix: a shot wears boots, legs, chest piece and vest by 1 (the helmet on a headshot); meleeLast wears the vest by
  Forge's wear of a piece of its points (Durability.meleeWear), stopping on its maximum. The client sees it through
  Decimation's player data sync (PacketSyncPlayerData every 6 ticks carries the vest stack).
- Checks: dev test durability RED (chest 0 after a shot, a zombie hit left the vest 3 to 3, no Durability line) then
  GREEN 27 PASS with "the client sees the vest's wear"; unit DurabilityTest (meleeWear). The same run also showed a test
  flaw: the test set the gun's hotbar slot on the server only, and the saved test world had another slot selected on
  the client, so its shots never fired; it now selects slot 0 on the client as pressing 1 does.

### Radiation percent not centred on its icon (reported 23 Oktober 2026 by the user with a screenshot, FIXED on main)
- Cause: RadiationHud drew the percent from 2x + 17 at half scale, Decimation's own start for its percents, which
  centres only a text as wide as "100%"; shorter texts sat left (-2.5 half pixels for "49%"). Fix: RadiationHud.textX
  (2x + 30 less half the text's width). Decimation's food, water, blood and health percents had the same fault ("40%" on
  the health cross sat a little left); the user asked for those too ("centre decimation hud percents too"):
  tools/patches/PatchHudPercents.java sends the x of IngameHud.render's six outlined texts through fixes/HudPercents.x,
  which centres the four percents (y 60, 129, 199, 266, a text ending in "%") on their icon, the start plus 13.
  Checks: unit HudPercentsTest; dev test radiation "Decimation's <food|water|blood|health> percent centred" RED (never
  drawn through HudPercents, 8 FAIL) then GREEN (+0.0 / +0.5 half pixels); shot hud_percents_centred.png. One run of
  that GREEN read the radiation icon's own draw cost 230.8 us (budget 200): a fresh game with the base jar just
  deobfuscated, the counter averaging from its first frame; the same game warm read 116.6 us over 21,511 frames (about
  102 us without the cold run), as before the patch.
- Checks: unit RadiationHudTest; dev test radiation "percent centred on the icon" RED -2.5 then GREEN; shot
  docs/shots/durability_v0.70.0/radiation_half.png.

### Dev tests left a server screen open (found 23 Oktober 2026 in the 0.70.0 checks, FIXED in the harness)
- Seen: radset failed 6 checks after a long batch in the production client, 8 when run again alone in the same game, and
  passed 40 of 40 in a fresh game. Its filter wear and fitting run only while the server sees no screen open.
- Cause: DevAutoTest closes client screens with displayGuiScreen(null), which never tells the server (Esc sends
  C0DPacketCloseWindow), so a container a mode opened stayed open there; radset itself leaves Decimation's gear
  container (Deci$GearContainer) open, so a second radset in one game failed.
- Fix: after each mode the harness looks at the player's container on the next server tick, records "server screen left
  open" with its class and closes it as Esc would (EntityPlayerMP.closeScreen). Not a player bug: Esc and the inventory
  key send the close packet [inferred from vanilla's closeScreen].
- Same day, the second harness leftover: throwables failed "three grenades, one thrown" in batch A of two candidates
  (an empty hand) and passed alone. The client log had "ProdTester fell from a high place" in that second: genwalk flies
  in creative and leaves a large fall distance, throwables switches to survival, and the first landing on the arena
  killed the player (the dead drop everything). Reproduced as `genwalk throwables`. Fix: DevTestArena.build clears the
  fall distance when it teleports, and the harness clears it after each mode. (The later "fell out of the world" in the
  same batch is gunsmith's own death check.)

### Inventory: no shift click (reported 22 Oktober 2026 by the user, FIXED on branch bugfix)
- User: "fix where inventory cannot shift + click for fast item organize since decimation use custom design instead using
  vanilla, shift click to armor is same so user need to take one by one on inv slot not like vanilla where you can fast
  put armor with shortcut shift click".
- Cause (decompiled PlayerGearContainer deci/f/d): transferStackInSlot returns null, so vanilla's shift click moves
  nothing, in every part of the screen (armor, mask / vest / backpack, worn backpack, loot area).
- Fix, no jar patch: Deci.GearContainer (PlayerGearContainer with transferStackInSlot sent to fixes/InventoryShift).
  The server gets it from InventoryShift.Handler, a wrapper of Decimation's GUI handler installed after the Gunsmith's
  (GUI_LOOT only); the client screen builds its own container in initGui, InventoryShift.Client swaps it on
  InitGuiEvent.Post (same slots from Decimation's setupSlots, same window id), so the client predicts the move too.
  Rules: from the inventory first onto the body (armor slot, then Decimation's mask, vest, backpack slots; only empty
  slots, each slot's own isItemValid: Decimation's armor slots refuse vests and masks), then into a worn backpack, then
  the other row (main to hotbar, hotbar to main); from armor, gear, backpack or loot into the inventory, main rows first.
  Equal stacks merge first, slot limits kept, nothing moves when nothing fits. Off switch InventoryShift.enabled
  (-Ddeciworldgen.invshift=false).
- Checks: dev test `invshift` (11 cases through PlayerControllerMP.windowClick mode 1, items counted before and after,
  client slots equal to the server's, client prediction): `fix=off` (Decimation's containers) 21 FAIL, then 46 PASS (helmet
  on and off, hotbar helmet, vest, gas mask, backpack, 20 into a worn backpack, 10 onto a hotbar 60 = 64 + 6, 40 out of
  the loot area, a full inventory leaves the helmet on, a full backpack taken off keeps its 20). Dedicated dev server
  (`invshiftmp`, client joins localhost:25576): 17 PASS (the inventory key opens the screen with shift click, helmet,
  vest, mask, backpack, stack into the backpack, helmet back off; 24 items in every step, none on the ground). Unit test
  InventoryShiftTest.
- Not verified: the production client with OptiFine and a release jar on the production server (only the dev
  dedicated server); a window resize while the screen is open (Decimation's initGui then makes a container with window
  id 0, an old Decimation behaviour we copy, not fixed) [not verified]. Open question for the user: with a backpack worn,
  a shift click from the inventory goes into the backpack before the other row (like a chest); say if the row swap
  should come first.

### Foregrip looked like it floated under the gun (reported 22 Oktober 2026 by the user, FIXED on branch bugfix)
- User: "foregrip on some gun is invicble so its looks flying below". Swept in game (dev test `gripsweep`: every gun and
  grip pair of grip_offsets.txt in the Gunsmith screen): every one of the 19 Foregrip pairs, not some; the RK-1 sat
  right on all 19. Shots docs/shots/grip_float_v0.65.0/.
- Cause: Decimation's foregrip.bmodel has two clamp tabs (grip_1, grip_2: 0.2 long, 0.1 tall) 0.3 above the grip's
  body. gripfit.py's seat() took the widest part at the grip's top, the tabs, so the tabs touched the rail and the body
  hung 0.35 to 0.4 below it; the tabs are too small to see, so the grip looked like it floated. study.py draws the same
  (study_m110_foregrip_before.png): not a drawing fault. Ruled out in game: BatchedPart off and ModelBatch off draw the
  same; Decimation's own copy at its one spot (AKM, no row) has the same gap.
- Fix: gripfit.py CLAMP / bearing(): the Foregrip is seated by its body (the tabs sink into the rail); fit and check both
  use it; grip_offsets.txt rewritten (19 Foregrip rows 0.4 higher, nothing else changed).
- Checks: `gripfit.py --check` with the new seat on the old table: 19 pairs not right ("floats: 0 of 7 places touch,
  gaps 0.35 ..") (RED), on the new table 0 (GREEN); gripsweep after: each Foregrip touches its handguard
  (foregrip_after.png, m110_before_after.png); `attachments` 30 PASS; unit suite 155 PASS.
- Not verified: lights (laser, flashlight) on all 103 guns were not shot in game (their models have no such tabs and the
  check passes them); the production client [not verified]; NPC held views [not verified].

### Some props still broke with one punch (reported 22 Oktober 2026 by the user, FIXED on branch bugfix)
- User: "fix some props that still can be easly destroyed by one click hand".
- Found by dev test `hardness` (every block of deci and deciworldgen set in the arena, a survival empty hand asked
  getPlayerRelativeBlockHardness as onBlockClicked does; table run/client/devtest/hardness.tsv): 8 of 277 break with one
  click, all hardness 0: BlockStreetBarrier, BlockBarrierTall, BlockMetalShelf, BlockMetalShelf_Empty (multiblock
  BlockContainers, like the wrecks before WreckHardness), BlockElevator and BlockElevatorButton (elevator), BlockRope
  and BlockRopeTop (climbing rope). Everything else takes at least 0.4 hardness (several clicks held).
- Fix: fixes/PropHardness (init, after WreckHardness) gives every deci / deciworldgen block of hardness 0 BlockProp's
  10, except MEANT_TO_BREAK: the two ropes (a player's own climbing tool, taken down with a click like vanilla string).
- Checks: hardness RED "8: BlockElevatorButton BlockStreetBarrier BlockMetalShelf BlockElevator BlockRope BlockRopeTop
  BlockMetalShelf_Empty BlockBarrierTall", GREEN 0; the dev dedicated server logs the same 6 set to 10; unit test
  PropHardnessTest. Open question for the user: should ropes stay one click (now) or get a ladder's 0.4?

### Hits on infected leave nothing on the body (reported 22 Oktober 2026 by the user, FIXED in code)
- User: "when they got hit it looks like yellowish tint splatter, but nothing gets stick onto their character so the
  feedback of its getting hit is none".
- Cause: Decimation's only hit feedback on infected is its green-yellow BloodInfected particles and ground splats
  (ClientEventHandler.onLivingHurt, damage over 3); the model never changes.
- Fix: fixes/Wounds paints blood splats into each hit infected's own copy of its look picture (fixes/HumanLayers binds
  it instead), so the blood follows every limb. A hit is the hurt flash starting on the client (works on servers and for
  any attacker); the spot is where the local player's view ray crosses the box (head / body / arm / leg, front / back /
  side, place on that face), else random. Up to 10 a body, 64 wounded pictures at once, 4 made a frame. Infected, Frail,
  Hulk and Bloater (ModelHuman); crawlers and infected dogs have their own models: no splats.
- Trap found: the local player's posY is already eye level (EntityClientPlayerMP); a ray from posY + getEyeHeight() -
  yOffset started 1.5 blocks low (head shots read as legs). Use getPosition(1.0f).
- Checked: dev test `wounds` 4 PASS (A: head, body, legs front, both arms as aimed; B 4 random; 9 hits seen, 9 pictures
  made), shots in docs/shots/wounds_v0.65.0. Cost: 0.0017 ms a frame with 3 infected in view, 2 wounded (dev client,
  unlimited fps, about 600 frames); no allocation per lookup (WeakHashMap by entity); a picture is painted only when a
  splat is added (64 x 32: a few thousand pixel steps and one 8 KB upload). Known: a wounded infected that blinks changes
  its look number, so its picture is made again on each blink [inferred from HumanLayers' key, not measured]. Not seen
  by the user yet [not verified in a real fight].

### Cape in first person: drawn in front of the camera (reported 22 Oktober 2026 by the user, FIXED, user checked in game)
- A player with a cape (Mojang account cape; OptiFine capes go the same way) saw it in front of himself in first
  person, in Prism with the 0.65.0 release jar.
- Cause (read from the bytecode of the 0.65.0 jar): PlayerModelSwapper (deci/J/e) draws your own body in first person
  on RenderWorldLastEvent with its own RenderPlayer, deci/J/e$a. Its renderEquippedItems (func_77029_c) calls vanilla's
  whenever no gun is held, and vanilla's draws the cape. The body there is turned by the head yaw while the cape swing
  is worked out from the body yaw and the cape chase position, so the cape lands in view [inferred: not traced frame
  by frame]. Decimation's own supporter
  cape (net/decimation/mod/client/cape/b) already skips first person and needs the dead backend anyway.
- Fix: fixes/FirstPersonCape sets RenderPlayerEvent.Specials.Pre.renderCape false when the renderer is deci/J/e$a
  (Deci.isFirstPersonBodyRenderer). Third person, the inventory preview and other players keep their capes.

### Gunsmith screen drew the gun inside out (seen by the user 21 Oktober 2026 05:45, FIXED before shipping)
- User: "i see the guns part model well not aligned ... example magazine it show up full, actually when insert in gun,
  some of them is not visible". Beside study.py's side render the Uzi's whole magazine showed through its grip.
- Cause: Minecraft's GUI view (glOrtho with top 0) flips y, a mirror: Decimation's renderer culls faces by winding, so
  every face turned inside out, the faces toward the camera were dropped and the far ones drawn, hidden parts showing.
  det(projection x model view, 3 x 3) was +0.0098 where the world view's is negative.
- Fix: GunsmithScreen.drawGun mirrors depth (glScalef 1, 1, -1) after the lights are set; nothing moves on screen (the
  view is orthographic). Vanilla's player preview in the inventory mirrors x for the same reason. Check "screen: the gun
  not drawn inside out" (GunsmithScreen.viewDeterminant read on a measuring draw): RED +0.0098, GREEN -0.0098; shots
  docs/shots/gunsmith_v0.65.0/uzi_inside_out_before.png and _after.png.

### Gunsmith list clicks lost on a server (found by the dev test gunsmithmp 21 Oktober 2026, FIXED before shipping)
- The Gunsmith screen's list sends each click as a container button id (Container.enchantItem). On a dedicated server
  the 4x never fitted, while every singleplayer check passed.
- Cause: C11PacketEnchantItem writes the id with writeByte and reads it as a signed byte. The ids were 1000 and up, so a
  server got only their last byte; decode found no action and the click did nothing. A singleplayer world hands the
  packet to its integrated server as an object, never written, so the int arrived whole.
- Fix: GunsmithAction packs slot and inventory index into one byte (0 .. 221, sent as (byte) code, read with & 0xFF).
  Unit test everyIdSurvivesTheNetwork (RED with the old ids, then GREEN); gunsmithmp on the production server then
  fitted and took off the 4x as the server sent it back.
- Lesson (CLAUDE.md traps): anything sent through a vanilla packet is checked off the integrated server once.

### Gunsmith: a part copied by a swap in the same tick (final review C1, 21 Oktober 2026, FIXED before shipping)
- A client that puts a copy of its gun in the hand (a use packet can replace the held stack) and clicks a part slot in
  the same tick got the part onto its cursor while both guns kept it in their tag: one copied part per click on any
  server, for a modified client.
- Cause: a container's canInteractWith runs once a tick (the player's update), slotClick runs before it, and the part
  slots are a view of the tag of the stack the screen opened on, so taking the part wrote nothing to the gun in hand.
- Fix: GunsmithContainer.slotClick refuses unless the hand still holds that very stack, and the part slots write the
  tag only then. Check "replaced: a part slot click in that tick takes nothing": RED took "Rifle Suppressor" onto the
  cursor, GREEN nothing.

### Gunsmith: a part on the cursor dropped when the screen closed (final review I3, 21 Oktober 2026, FIXED before shipping)
- Closing the screen with a part picked up (Esc, I, the gun leaving the hand) threw it on the ground, vanilla's
  Container.onContainerClosed; leaving the game with it lost it (the cursor is not saved).
- Fix: GunsmithContainer.onContainerClosed puts the cursor's part back in the bag first (dropped only when the bag is
  full), and GunsmithContainer.Logout does the same on PlayerLoggedOutEvent, which FML fires before the player is saved.
  Check "closing with a part on the cursor puts it back": RED 1 on the ground and 0 in the bag.
- Same pass (final review I1, I2): a part put down with the mouse moving while pressed failed (GuiContainer turned it
  into drag painting, which a part slot refuses; GunsmithContainer.canDragIntoSlot leaves part slots and the gun's own
  slot out of a drag, so the release there is a plain click), and "Inventory full" went to the hidden chat (now said
  in the screen, and the list shows "Inventory full" in Take off's place).

### Clicking a news popup crashed the game (found by the user 21 Oktober 2026 01:40, FIXED main 089edfd, prerelease)
- User: "when i click the pop up, its crashed". Crash reports 01:40:26 and 01:40:52: ConcurrentModificationException in
  DeciScreen.mouseClicked (deci.d.i), screen MenuDetail.
- Cause: a news panel's click arrives inside DeciScreen.mouseClicked's loop over the menu's panels (eX); ArticlePanel
  calls openUrl, our MenuNews.open opened MenuDetail at once, and MenuDetail.initGui re-initialised the menu behind it
  (setWorldAndResolution: DeciScreen.initGui clears eX, MenuNews adds the four panels again) while that loop was still
  running. The last panel (Coming soon) happened not to throw: the loop ended.
- Why the tests missed it: mainmenu opened the popups with Deci.openNewsPanel, which calls openUrl directly, outside
  that loop. ArticlePanel takes a click only when it was hovered in the last drawn frame (field es).
- Fix: MenuDetail.open switches screens on the next tick (Minecraft.func_152344_a), after Decimation's loop; initGui
  re-inits the menu behind only when the window size changed. mainmenu now draws the menu with the mouse over the panel
  and clicks it through GuiScreen.mouseClicked (DevTestUtil.clickAt): RED first ("Multiplayer panel click:
  java.util.ConcurrentModificationException", the same for Updates), then all PASS on main and prerelease.
- Lesson (skill decimation-main-menu): test a click the way a mouse makes it; shortcuts past Decimation's loops hide
  crashes.

### Dev client and main's test jars offered the 0.65.0 pre-release as an update (found 20 Oktober 2026, FIXED main 68ecf7a, prerelease 23605eb)
- User (dev client after 0.65.0 was published, screenshot): the home menu showed Update instead of Play, the Updates
  panel "Release check failed, see GitHub" and Decimation's corner "New update available! Update from the Options menu!".
- Cause: our version comes from the newest git tag a build reaches. 0.65.0 is tagged on the prerelease branch, so main
  builds still read 0.64.8 (log: "update 0.65.0 available (239 MB) from the api (not modified), running
  0.64.8-main.115+baf1c806dd") and every main build took the pre-release for an update. The dev client cannot replace
  its build output (Updater.download sets FAILED, "this game does not run from a jar"); FAILED with a known release
  keeps required() true, hence Update with "Release check failed". Worse in a test jar built from main (multiplayer
  tests: Mac Prism and wyvern feeb69f0, Desktop d6aea397): Update works there and would swap the test jar for 0.65.0,
  which has no multiplayer.
- Fix: only release builds look for updates. Updater.release(version): a clean tag, no "+" (Gradle writes
  "+<commit>" into every other build: 0.64.8-main.115+baf1c806dd, 0.65.0-prerelease+14830b9e37-dirty);
  offersUpdates(): a release build and not the dev client unless a dev test stands a jar in; check() otherwise sets the
  new State OFF with "dev build (<branch>)" (or "set in config" when turned off there, which used to leave the panel on
  "Checking GitHub releases"). MenuNews shows "Updates off: dev build (main)"; MenuTexts shows a dev build's version as
  "0.64.8 dev" (title, under the logo, welcome box). The dev test `update` pretends a release version (`from`, default
  0.58.0) in both its ways now and puts the updater back to OFF at its end.
- Checked: UpdaterTest and MenuTextsTest failed first (methods missing), then pass; suites main 103 / 0, prerelease
  24 / 0. Live in both lines' dev clients (main 25599, prerelease 25597 at the same time): menu "update check OFF (dev
  build (main))" / "(prerelease)", update (fake release server) all PASS, mainmenu "updates panel line: Updates off: dev
  build (main)", window title "DeciRemnant v0.64.8 dev" / "v0.65.0 dev"; the home menu shot shows Play and no corner
  text. Players' release jars are unchanged by this (their versions have no "+") [not verified in a release jar: none
  was built with this code yet]. Test jars built before 68ecf7a still offer the update: do not click it there.

### WorldClock day never advances (found 16 Oktober 2026, FIXED v0.62.9)
- Found in the Decimation code study (docs/decimation_code/world.md). Decimation's DayLengthHandler (deci/an/p,
  common, every world tick) sets world time back to 0 once it passes 24000. Our fixes/WorldClock (v0.57.0) takes
  day = world time / 24000 + 1, so day() is always 1; and its tick sees the time go from about 24000 back to 0 and
  treats that as "time set back". CONFIRMED by the failing dev test (core, "clock: two midnights", 16 Oktober 2026):
  day() stays 1, and DAY does fire at each midnight (the tick sees 23999 -> 24000 before Decimation's reset at the next
  tick start) but always as DAY@2: got "1 [DAY@2], 1 [DAY@2]", want "2 [DAY@2], 3 [DAY@3]".
- Impact today: Areas.since is always day 1. Before the systems plan's day based features (zombie growth, horde
  nights, raids, claim decay) it must be fixed.
- Fix (in progress, user "start fix worldclock"): a saved day counter (WorldSavedData "deciworldgen_clock": days,
  last raw time; existing worlds start at totalWorldTime / 24000). Forward raw time: fire events in (last, now] with
  day = days + 1 + (p / 24000 - last / 24000), days += now / 24000 - last / 24000. Backward: last >= 24000 means
  Decimation's reset (nothing to do), else a /time set back (no events). Sleeping jumps to 24000: forward, counted
  once. fire(w, from, to) keeps its old day numbers for the jump checks. Test first: DONE (fails as above).
- FIX v0.62.9: fixes/WorldClock.Data (WorldSavedData "deciworldgen_clock" in the overworld's mapStorage: days, last)
  as planned; the tick handler counts each multiple of 24000 the raw time passes forward and fires events with
  day = days before + 1 + (point / 24000 - last / 24000); any backward step only sets last. day(world) = days + 1
  (0 on a client world). The Forge bus registration and the unload reset went (nothing static left to reset).
- Checked: core dev test all PASS in a fresh live game ("two midnights (day 36 before)": 37 [DAY@37], 38 [DAY@38];
  the dev world started at its total ticks / 24000 = 35 days); <world>/data/deciworldgen_clock.dat then held
  days 37, last 6324; after a restart the same test began at day 38 (39 [DAY@39], 40 [DAY@40]), so the count is
  saved. Dedicated server (servertest seed 1): starts, writes deciworldgen_clock.dat, stops cleanly, no exception.
  Not seen: a real night passing by itself or sleeping in a bed [not verified; both are forward steps the test
  covers with setWorldTime].

### Backend reconnect leaks a thread every 15 seconds (found 16 Oktober 2026, FIXED v0.62.8)
- Found in the Decimation code study (docs/decimation_code/backend.md). Every 300 client ticks Decimation tries to
  reconnect to its dead network (ClientEventHandler, client tick): `new Thread(backendClient.aBX).start();
  aBX.reconnect()`. Each new thread runs kryonet's `Client.run` loop, which never ends; the connect fails, so the
  next try 15 s later starts another one.
- Measured on the live test game: `jcmd <pid> Thread.print`: 27 threads in `kryonet.Client.run` at 03:56:48, 28 at
  03:57:09, 7 min 10 s after launch; the log shows "Attempting reconnection to Network..." / "Failed to Connect to
  Network!" every 15 s from a new Thread-N. About 240 blocked threads an hour of play (memory per thread stack;
  no CPU, they wait on one selector) [cost per hour inferred].
- Also: network.mcdecimation.net resolved to 37.48.77.81 in an earlier run and to 212.92.104.119 now. If anybody
  answered there, the game would talk to them (kryonet, Decimation's registered classes) [risk inferred].
- PatchBackend (2026-10-09) only shortened the connect timeout to 300 ms; the reconnect loop is untouched.
- FIX v0.62.8 (user: "do both", the website plan stays separate: docs/roadmap.md UI 2): tools/patches/PatchBackend.java
  v2, three jars: BackendClient.connect (deci/aP/a.e) does nothing, the client reconnect (ClientEventHandler deci/c/c,
  static a(deci.aP.a)) and the server reconnect (ServerTickHandler$1.run, deci/aK/q$1) do nothing; v1's kryonet
  change kept. Only those methods changed (javap compare: every other method of deci/c/c the same instructions and
  constants; offsets moved, one nop for switch alignment).
- Checked: live game log "Decimation's BoehMod network is off" once, 0 "Attempting reconnection", 0 connects; thread
  dumps at 1 min 6 s and 2 min 12 s uptime: 0 kryonet Client.run threads (45 to 48 threads in all, was 72 with 27
  leaked); dev tests menu, stats, zombies, checks all PASS; dedicated server (servertest seed 1) starts, logs the
  same line, no reconnects, stops cleanly. Backup hash unchanged.

### Every Decimation mob drawn solid black on a fresh Windows install (reported 13 Oktober 2026, Open)
- User: on the brother's Windows laptop (fresh Prism, Java provisioned by Prism, first Minecraft ever),
  "all mobs decimation from zombie, animals, etc is rendered black". The user's Mac renders them fine.
- What the code says: every mob texture comes from the jar (ModelHuman binds body / eyes / hair / shirt
  / pants / shoes PNGs per variant; animals use plain RenderLiving), so a missing texture would show
  Minecraft's purple / black checkerboard, not solid black. Solid black on every mob points at entity
  LIGHTING: blocks are lit by the lightmap only, mobs and held / dropped items also by OpenGL's fixed
  function lights (RenderHelper.enableStandardItemLighting); a driver that gets those wrong draws them
  black while the world looks right [inferred: not reproduced, GPU and log not seen].
- Our own code: only SightReticle and ScopeZoom touch lighting / texture state, both inside
  glPushAttrib / glPopAttrib and only while a gun or scope is drawn; nothing of ours runs per mob
  [checked in code]. Decimation has no shaders (no GL20 / ARB program calls).
- Needed: logs/fml-client-latest.log and latest.log from that instance, the GPU (Task Manager,
  Performance), a screenshot; are the player's hand, held items, dropped items and the player in F5 also
  black; day and night alike. Things to try there: Windows Graphics settings, javaw.exe on the
  high performance GPU (if the laptop has two), a GPU driver update [not verified as a fix].
- Possible fix on our side once confirmed: an option to draw mobs without the fixed function lights.
- Update (user, 13 Oktober 2026): every Decimation mob (NPCs, animals, zombies) is black, AND props do not
  load, or appear black and vanish / come back with every move of the player or the camera. The laptop: NVIDIA
  RTX 2060 + Intel i7-9750H (its Intel UHD 630 is the second GPU: an Optimus laptop). User: strong confidence
  it is Java or GPU rendering.
- Read in code: Decimation draws mobs (ModelHuman on ModelBiped) and props (BModel parts) through vanilla
  ModelRenderer display lists with the normal fixed function lighting; GlBufferPool is only a scissor stack.
  No shaders, no vertex buffers of its own: nothing GPU specific that a Mac would forgive and a PC not. So a
  driver / GPU choice problem is the likely cause [inferred]; the classic one on Optimus laptops: Java runs
  on the Intel GPU unless Windows or the NVIDIA panel says otherwise [not verified on that laptop].
- v0.60.1: fixes/GlInfo logs the GPU, its driver vendor and OpenGL version, and the Java at start
  ("[deciworldgen] graphics: ..." and "java: ..." in logs/fml-client-latest.log; checked in the dev client:
  "Apple M4 | Apple | OpenGL 2.1 Metal"; the dedicated server logs nothing and starts).
- Steps for that laptop: (1) NVIDIA Control Panel, Manage 3D settings, Program settings, add the javaw.exe
  Prism uses (its path: Prism Settings, Java), "High performance NVIDIA processor"; or Windows Settings,
  Display, Graphics, javaw.exe, High performance; (2) update the NVIDIA and Intel drivers; (3) in game Video
  Settings: Advanced OpenGL off, Mipmap levels 0; (4) send the "graphics:" line of the log.

### Water spread over a wasteland crossing once in the bridge test (seen 13 Oktober 2026, Open, not reproduced)
- Live `bridge` run (district at 31664,1536): 100 water blocks at street level on a crossing beside
  the test bridge (flowing on the road from its west part, no source found in the chunk layers read).
  The next run passed (0). Cause unknown [not verified]: a spring decorated into a neighbouring
  chunk (as in Rails lesson 2) or the new bridge damage of v0.56.0.

### Dev test game crashed once in vanilla grass ticking (seen 13 Oktober 2026, Open, not reproduced)
- Live `wasteland` run 1 after v0.54.2's lot changes: "Exception getting block type in world"
  (NullPointerException in World.getBlock from BlockGrass.updateTick) at 1256,68,-465, outside the
  tested district and away from any lot; crash report dev/run/client/crash-reports/
  crash-2026-10-13_01.35.07-server.txt. The block there on disk is grass under air. The next three
  runs did not crash. Cause unknown [not verified]; the lot code only plans blocks, it touches no
  world. Watch for it again (an "Entity is already tracked" error came just before).

### Community row Join crashed the game (found 18 Oktober 2026, player hosting, FIXED in code)
- Seen: a Join from Decimation's Community page (ListElement button 123, FMLClientHandler.connectToServer) threw a
  NullPointerException and crashed the game when Minecraft's own Multiplayer screen had never been opened: FML's server
  list cache (serverDataTag) is created only by setupServerList, which GuiMultiplayer calls.
- Fix: CommunityList calls setupServerList before showing rows. RED (the crash in the two game test) then GREEN.

### The relay connection never finished TLS with Cloudflare (found 18 Oktober 2026, player hosting, FIXED in code)
- Seen: against the deployed relay the host's control socket failed with "SSLException: handshake timed out" (Java
  8u504, Minecraft's netty 4.0.10); joiners got "Connection reset by peer". A TLS test echo server (TLS 1.2) had worked.
- Fix: RelaySocket offers TLS 1.2 only; probe RED then GREEN, the two game test through the deployed relay all PASS.

### Place names gone after a rejoin (found 18 Oktober 2026, player hosting spike, FIXED, released in 0.65.0)
- Seen: a second player who left and joined the host's world again had an EMPTY turf list (0 entries) while the server
  had sent 5 to that player before (dev test `lanjoin`, docs/player_hosting.md section 9). The same happens in
  singleplayer after leaving a world and entering it again in one game session [inferred: same handler, not tried].
- Cause: worldgen/PlaceNames sends a player's list only when it differs from the last one sent to that player's UUID;
  after a reconnect the client's list is empty, the server's memory says "already sent".
- Fix: PlaceNames.onLogout forgets the player (PlayerLoggedOutEvent). RED then GREEN in the rejoin run; `places` and
  `zones` all PASS.

### HUD crash after joining a world without Decimation's menu (found 18 Oktober 2026, player hosting spike, FIXED, released in 0.65.0)
- Seen: the production client (release 0.64.8) started with `--server HOST --port P` joined a LAN world and crashed 4 to
  5 s later, 3 of 3 times with fancy graphics on, 0 of 2 with it off: NullPointerException in IngameHud.render
  (deci/g/j.b) from ClientEventHandler.onRenderOverlayPost (deci/c/c.a).
- Cause (proven, docs/player_hosting.md section 6): with fancy graphics the HUD calls
  `DecimationMod.fakePlayerProfile.getPlayer()` (field B, MenuPreviewWorld) without a null check; only Decimation's menu
  (GuiMenuBase.initGui) creates it. A line tagged copy of the class put the crash on that call (instruction 1064).
- Who hits it: anyone who reaches a world without opening Decimation's menu first (a launcher's join on start)
  [inferred]; players through the menu do not (seen 18 Oktober 2026: `lanjoin` goes through the menu, fancy graphics
  on, no crash in 3 joins).
- Fix (18 Oktober 2026): fixes/HudGuard creates the preview (Deci.ensureMenuPreview, as GuiMenuBase does) on the client
  tick when a world has none. Dev test `hudguard`: preview removed, back within half a second (RED with the guard
  stubbed, then GREEN). Not fixed yet: a guard (create the profile on world join, or a
  null check by patch) belongs to the player hosting work.

### Update screen bar stops short of full (found 17 Oktober 2026 in the user's Prism, FIXED 18 Oktober 2026, released in 0.65.0)
- The user's real auto update 0.64.7 to 0.64.8 (16:56): "SAFE TO CLOSE GAME!", 239.5mb/239.5mb, Quit game and Later,
  but the green bar about 80 % long. After Quit game and a restart the title read DeciRemnant v0.64.8: the update itself
  worked (first real auto update by a player's game).
- Cause [inferred from GuiUpdateProgress.drawScreen]: bar length = (width / 2) / 100 * percent in integers; with a GUI
  width of 500 that is 2 * 100 = 200 of 250 pixels. Decimation's own bug; the dev shots happened to use widths where
  it rounds less.
- Fix idea: UpdateProgress puts percent * (width / 2) / (100 * ((width / 2) / 100)) into downloadPercent, so the
  drawn bar matches the real share; test with the screen width of the user's window.
- FIXED (18 Oktober 2026, next release): fixes/UpdateBar.field(percent, GUI width) scales the field UpdateProgress
  writes (bar = width / 4 * 2, step = bar / 100; under 100 px nothing to fix); the percent text stays the real one.
  JUnit UpdateBarTest RED (2 of 3) then GREEN for GUI widths 427 to 960; dev test `update` new check "ready bar full"
  at --window=1000x600 (GUI width 500, the user's case): 250 of 250 px, field 125; shot
  docs/shots/main_menu_v0.64.9/update_ready.png (green bar to the end, flare at the right).

### In-game update never worked in a shipped game: "does not run from a jar (dev folder)" (found 17 Oktober 2026, FIXED v0.64.7)
- Seen by the user in Prism on 0.59: Update, then "this game does not run from a jar (dev folder): nothing to replace".
- Cause: Updater.target read our class's code source, which under Forge's class loader is a "jar:file:...!/..."
  address; new File(URI) refused it and the updater took it for a dev folder. Every version from 0.59.0 to 0.64.6 has
  this: their players must install the next release by hand once. The update tests used a stand in target and the
  production tests never pressed Update, so nothing caught it.
- Fix: the mod's source file from Forge (ModContainer.getSource), a jar: address read as a fallback, nothing in a
  deobfuscated (dev) game. Checks: dev test `menu` "update target" (RED none in the production client, GREEN
  mods/Decimation.jar; none in the dev client); `update real=true from=0.59.0 expect=0.64.6 jar=true` in the
  production client: GitHub check, Update button, our screen, 238 MB in 29 s, checksum, the running jar swapped
  (its sha256 then equals the published 0.64.6). UpdateTest used MCP only reflection: now DevTestUtil.
- Seen again by the user 17 Oktober 2026 15:11 in Prism ("Decimation, but better.", its jar 240980301 bytes = the 0.59.0
  release): expected, the screen comes from the 0.59 code in that jar; 0.64.7 is not published and cannot change it.
- Released in 0.64.7 (17 Oktober 2026 15:33); after it the production client running 0.64.7 as "0.64.6" updated
  itself from GitHub (dev test `update real=true jar=true from=0.64.6 expect=0.64.7`, all PASS).
- Way out for an old game: install the release by hand (replace mods/Decimation.jar), or start it with the JVM argument
  -Ddeciworldgen.update.target=<full path of mods/Decimation.jar>: every version 0.59.0 .. 0.64.6 reads that property
  first (checked in the tags 0.59.0, 0.60.1, 0.62.8, 0.64.6), then the old updater downloads and swaps as normal. How
  Prism passes an argument with spaces in the path [not verified].

### Checkpoint gate with HESCO: a stair over the shut line (found 17 Oktober 2026, FIXED v0.64.4)
- Found by `gatewalk` extended to every gate look with a path finder check (docs/wasteland.md): the checkpoint
  look in the HESCO state, meant to be shut, had a way through: the HESCO closure's 1 high sandbag at lane 3, the
  checkpoint's 2 high sandbag fighting position beside it at lanes 0..2 touching the line, then the line's 3 high
  sidewalk fence, whose concertina wire on top does not stop walking (the path went through the wire cell), and down
  past the line. One pushed body got through in one run. The steel state's random sandbags could make the same stair.
- Fix (military/CityGate.checkpoint): the fighting position stands one row off the line, so its wall never touches
  the fence; the spotlight and the flag moved back with it (0 prop clashes).
- Checks: RED path found (route logged: lane 3 at y 2, lane 2 at y 3, lane 1 at the line at y 4, down past it);
  GREEN 54 of 54 (30 path checks, 24 shut lane counts).
- v0.64.6 (found by the code review): the flag moved with the position to d 10, outside the look canvas (d 0..8);
  Canvas.set drops such cells without a word, so the checkpoint had no flag (and "0 clashes" only because it was
  missing). Flag now on the east sidewalk before the tower; CityGate's prop helper logs a prop outside the canvas
  (the gates test reports it as a clash); the gates test counts the checkpoint's flag (RED 0, GREEN 1 in all five
  states; all 30 gates 0 clashes; gatewalk still 54 of 54).

### Airport garrison: far posts never manned, guards wandered off (found 17 Oktober 2026, FIXED v0.64.3)
- Found by the new dev test `garrison` (fresh seed 1 world, the airport at 272, -2032; docs/airport.md section 12):
  the first visit manned 14 of 16 posts. Garrisons.man spawned every post when a player came within 40 blocks of the
  FIRST post and then marked the garrison manned; posts about 240 blocks away lay in chunks not loaded yet, so their
  spawns failed and they stayed empty for good. The guards also had no home, and Decimation's wander AI
  (EntityAIWander) walked them off their posts.
- Fix (worldgen/Garrisons): the first visit starts the garrison, each post spawns once its own chunk is loaded (per
  post state "!garm_<id>"; garrisons that older versions marked manned stay as they are); each guard keeps its post in
  its entity data (deciworldgen_post) and a home area of 4 blocks there, set again on every load.
- Checks: RED 14 of 16 posts held, 0 of 16 guards with a home; GREEN 16 of 16 posts (14, then 1 and 1 as their chunks
  loaded), 16 of 16 homes, 0 inside a block. Also for the wasteland bunker garrisons (same code).
- v0.64.5: the home area only bounds idle wandering; a chase (the airport's military infected spawner is close) drew a
  guard away and nothing brought it back (production client: 15 of 16 posts held). Garrisons.backToPosts walks a guard
  that is not fighting (no revenge target) and has no path back in legs of at most 12 blocks. Check: a guard moved 12
  blocks off its post: RED still 9.9 away after 300 ticks; GREEN back within 6 after 174 ticks.
- v0.64.6 (code review of the night's changes): (1) backToPosts walked the world's entity list while asking for
  paths; a path search loads chunks around the guard (about 72 blocks) and a loaded chunk adds its entities to that
  list, so a ConcurrentModificationException could crash the server: it walks a copy now. Not reproduced in a test
  (two tries: a lower view distance and an entity in a chunk to reload; the guard's search did not reach it), fixed
  by reasoning from the vanilla code. (2) A post far from the first spawned once its chunk was loaded, maybe before
  the chunk was populated and its tower written: each post now waits until the chunks around it exist (as the first
  post did). (3) A spawn a join handler refused marked the post done for good: up to 5 tries now. garrison test
  PASS again (13 posts at the first visit, the rest as their chunks filled in, 16 homes, a guard walks back).

### "Entity is already tracked" at every world load: swapped mobs added twice, double speed (seen 16 Oktober 2026, FIXED v0.64.2)
- Seen: vanilla's EntityTracker logged `"Silently" catching entity tracking error ... IllegalStateException: Entity is
  already tracked!` 12 to 32 times at each load of a world, in 39 of 40 dev client logs; also once beside the
  grass ticking crash above.
- Found 17 Oktober 2026 with the dev diagnostic -Ptrackdebug (devtest/TrackDebug: remembers where each entity joined,
  logs both stacks when an id joins twice): the entity was a BloaterEntity (deci.ag.e), first joined inside
  Decimation's PlayerJoinSync (deci.an.e) handler, then again in World.addLoadedEntities of the same chunk load.
- Cause: PlayerJoinSync swaps joining mobs by spawning the new one at once (1 in 10 infected, not bloaters: a bloater
  3 in 10, else a hulk; 5 in 100 soldiers: a mech; an infected in the rain: a frail, and both rolls can fire for one
  infected). During a chunk load that runs inside World.addLoadedEntities, which walks the chunk's LIVE entity list by
  index: spawnEntityInWorld appends the new mob to that list and adds it to the world, then the walk reaches it and
  adds it again. The world listed it twice and updated it twice a tick: double speed movement, AI and attacks until it
  died or its chunk unloaded, for every swapped mob that came with a loaded chunk (in singleplayer: all the time).
- Fix (no jar patch): fixes/JoinOnce cancels the join of an entity the world already holds (WorldServer's id map has
  this very object), at HIGHEST priority so Decimation never rolls a swap for it again; one map lookup per join.
- Checks: dev test `doublejoin` puts 80 plain infected into an arena chunk's list the way a disk load does and runs
  addLoadedEntities on it. RED before: 8 swapped, 8 listed twice, 40 updates in 20 ticks; after: 6 swapped, 0 listed
  twice, 20 updates in 20 ticks. A world load with -Ptrackdebug: 0 "already tracked" errors (was 12), 0 duplicate
  joins; `zombies` and `menu` still PASS.

### Wasteland fence floating over half slab sidewalks (reported 14 Oktober 2026 by the user, FIXED v0.61.0)
- User shot docs/shots/wasteland_gate_v0.60.2_review/user_fence_float.png: the inside fence posts and bars
  started above the sidewalk's half slabs and hedges ("looks flying ... replace the slab").
- Cause: StreetPlan passed the top of whatever the street part had in the column (a half slab, a hedge, a
  railing) as the fence's base.
- Fix: StreetPlan.floor = the last full opaque block (leaves excluded: they are opaque on fast graphics); the
  fence starts on it and replaces anything above. The fence also runs over bridges now (it skipped
  bridge chunks: a gap with only the bridge railing).
- Check: seed 1 server, x 1792 line: 205 of 220 fence columns on full blocks; the 15 others were bridge
  railings over a canal (the gap the second change closes). New chunks only.

### No city on a friend's install (found 13 Oktober 2026, FIXED v0.58.0)
- User: "most critical bug is the city is not generated. why you using splited mod? why not combine it
  with one".
- Cause: the converted city pack (config/decimation_worldgen/lc, 2.4 MB) and the schematics lived only in
  our own config folders; no release ever carried them, so a fresh install fell back to the old
  procedural city.
- Fix (user choice: one jar with everything): tools/onejar.py builds ONE Decimation.jar (Decimation with
  our bytecode fixes, our mod, the content under assets/deciworldgen/content/); worldgen/BundledContent
  unpacks the content into config/decimation_worldgen/ on start when its stamp differs.
- Checked on a real Forge 1.7.10 server outside the dev setup (installer 10.13.4.1614, only the one jar
  in mods/, seed 1, type decimation): "bundled content ... unpacked: 486 files, 0 failed", 231 city
  buildings (34 DeceasedCraft, 64 legacy wasteland in the first log lines), the refuge in the spawn
  city, no errors of ours.

### Scope zoom lags on a weak laptop (found 13 Oktober 2026 by a friend of the user, FIX v0.58.0, effect not measured there)
- User: "scope when do zoom in/out animation, its really laggy and do framedrop rell high. this happen
  on my potato laptop".
- Cause [inferred from the code]: fixes/ScopeZoom copied the WHOLE screen into the scope texture every
  frame of the zoom animation, and also every frame while a scoped gun was only held (no glass box known
  there); only a settled aim copied just the glass. A full screen copy each frame is heavy on an
  integrated GPU.
- Fix: copy only while aiming or zooming, always just the glass box (a wider margin while the gun still
  moves), nothing before a glass was measured. Dev test `scope` PASS, the 2x / red dot glass still shows
  the world (docs/shots/bugs_v0.58/sheet_scope.png). Not measured on a weak GPU.

### Left click does nothing with fists or melee (reported 13 Oktober 2026 by a friend, CLOSED: not our bug)
- User: "left mouse click is not usable, except gun it can shot, but cant click left mouse button with
  melee and hit block too".
- What Decimation does: while a gun is held it unbinds the attack key (keyBindAttack code 0; the gun
  reads the mouse itself) and binds it back to the left button (-100) when no gun is held
  (ClientEventHandler tick).
- Dev test `click` (physical left button pressed the way Minecraft does, survival): bare hand breaks
  dirt, also after holding a gun (attack key 0 with the gun, -100 after), a fire axe hits a pig: all
  PASS. Test traps found: the client owns the selected slot; Minecraft digs only with window focus
  (mc.inGameHasFocus); a teleport every tick keeps the player off the ground (digging 5 times slower).
- Needed from the friend: survival or adventure, inside a safezone (camp, refuge) or not, mouse or
  touchpad, Windows or Mac, which item and what target.
- Friend (13 Oktober 2026): creative and survival both; guns fire. User asked whether dev and
  production differ: a production client was built (docs/production_test.md) and `click` run in it with
  the release jar: all PASS there too (simulated key presses). Our shipped code has no name based
  lookups touching input; Decimation's dev only branch (StanceEntityRenderer) only covers fog colours.
  A real mouse click is still untested (`click manual=true` waits for one).
- Real click in the production client (user, 13 Oktober 2026, on their Mac): held left button on the
  dirt, the hand swung, the block broke (`click manual=true` PASS). So on a standard install the whole
  path works.
- How the path goes (read in code): the mouse button becomes key code -100; vanilla attacks / digs
  through the "Attack" key binding with that code. Decimation (ClientEventHandler client tick, no early
  exit or swallowed error before it) unbinds Attack (code 0) while a gun is held and binds it back to
  -100 only when its code is 0. Guns never use the Attack binding: they read Mouse.isButtonDown(0)
  directly. So "guns fire, fists / melee / blocks do nothing" is exactly what happens when the Attack
  binding is not the left button on that install: rebound to something else (never restored: only 0 is),
  or another control holding the left button (a key conflict: only one binding per code gets it), or
  another mod taking the mouse. [inferred: their options.txt / Controls screen not seen]
- Closed (user, 13 Oktober 2026): the friend had changed the button binding before ("caused by button
  type he changed before"); the same release on the user's brother's Windows laptop clicks fine.
- To check on the friend's game: Options > Controls, "Attack/Destroy" must read "Button 1" with no red
  conflict; or options.txt line `key_key.attack:-100`; and their mods folder.
- Confirmed by the user (14 Oktober 2026): the friend's Attack was bound to a key, not the mouse button.
  Done, nothing to check again.

### Shots at a big zombie's head miss (reported 13 Oktober 2026 by a friend, Open, not reproduced)
- User: "big zombie model hit box not hit when its hit head, i think its caused by hitbox issue".
- Dev test `hitbox`: Hulk and Bloater boxes are 1.2 wide, 2.85 high (eyes 2.42); side view with boxes
  drawn: both models stand inside their boxes, the box top at the head (docs/shots/bugs_v0.58/
  hitbox_side.png). Not checked: how Decimation's player bullet tests a hit (client ray or server), our
  riot variant (InfectedVariants, takes less damage), which zombie the friend meant.

### New players are forced to update: "Update" instead of "Play", then an empty server list (found 13 Oktober 2026 by a friend of the user, FIXED v0.57.2)
- User: "when user first time opening the mods, it force player to update and after it click update,
  it show server list without any of them. and there is no create world or button".
- Cause: UpdateChecker.isUpdateRequired (net.decimation.mod.common.utils.h.gI) answers true when
  mods/DecimationVoiceChat.jar is missing (our releases ship only Decimation.jar and the deciworldgen
  jar) or when BoehMod's latestversion.txt on GitHub names another version (the file is gone today:
  404, so the built-in 1.21.10f stays). The menu then shows "Update" (GuiMenuBase and the profile /
  supporter pages), which leads to BoehMod's dead server list.
- Fix: tools/patches/PatchNoUpdate.java, gI always false (three jars). Dev test `menu` records the
  answer and photographs the home menu. Run live with dev/run/client/mods/DecimationVoiceChat.jar
  moved aside (put back after): jar present false, update required false (PASS); the home menu shows
  "Resume Game" where it showed "Update" (GuiMenuBase builds that button update first, then resume in
  a world, else Play: the title screen gets "Play" by the same check). Shot
  docs/shots/menu_v0.57.2/menu_home_no_voicechat.png. Not seen on the friend's install itself.

### Decimation's crates gave one item fewer than listed; stacks one short (found 13 Oktober 2026, FIXED v0.57.0)
- Found reading LootPool / IntRange (docs/loot_levels.md section 2); user: "fix the off by one too".
- IntRange (deci.aB.a) is half open and every loot range is built as upTo(n) = [1, n), so a crate
  listed with n items per open gave 1 .. n-1 (military, police, medical, ammo crates and the weapon
  cabinet: n = 2, always exactly 1) and an item listed as a stack of n gave 1 .. n-1.
- Fix: tools/patches/PatchLootRolls.java makes IntRange.pick inclusive (three jars). Checked by the
  dev test `core` over 2000 opens each: military crate 1 or 2, chest 1 .. 3, supply drop 1 .. 8,
  every multi stack item seen reaches its listed size and none passes it.

### Enemy military group members spawned beside their spot, into walls; none in superblock districts (found 12 Oktober 2026, FIXED v0.54.1)
- Found by the dev test `wasteland` (docs/wasteland.md section 5).
- MilitarySpawner checked one spot, then put each member up to one block off it: at a street chunk's
  edge that cell belongs to the building chunk beside it (its wall, or a closed room). Now each member
  takes a free cell with air above, ground below and the same street level rule, else the spot itself.
- Wasteland.streetSpot asked for a street chunk; a superblock district has streets only on its edge,
  out of the 24 to 48 ring around a player in its middle, so no group came there. Now any top block
  at street level counts (street, yard, lot, park; a roof is higher, a ground floor is never the top).

### Small lab read as an office; barricades at every inner door (found 12 Oktober 2026, FIXED v0.54.0)
- Found while building the wasteland military area (docs/wasteland.md).
- BuildingDress.kind matched "building_la" (meant for office names) before the lab rule, so
  DeceasedCraft's building_lab1 got office furniture. Now building_lab* is LAB.
- BuildingDress.barricade piled crates (v0.53.0) at every door of the building, inner ones too; in a
  bunker the sandbag version made a third of its props. Now only entrances (one side of the door
  outdoors, the other inside), as the doc always said ("ground floor doors").

### Thin props passed every overlap check (found 12 Oktober 2026, FIXED v0.50.0)
- Found while checking the city dressing: wall TVs (drawn 0.125 deep) hung into door cells and
  nothing refused them.
- Cause: drawn boxes were shrunk by 0.08 per side before testing (the propclash tolerance); a box
  thinner than 0.16 turned inside out and overlapped nothing. Affected PropFit and the dressing.
- Fix: PropGeom.shrink keeps at least 0.04 per axis. Checked: seed 1 dressed world, 0 findings
  from the dressing.

### Converted city props drawn into walls, floating, facing walls; cars in the sidewalks (found 11 Oktober 2026, FIXED v0.49.0)
- Found measuring a seed 1 world (tools/props/worlddump.py + propclash.py): 3609 findings over
  11009 props, 93 % inside converted DeceasedCraft buildings.
- Causes: tools/lctranslate.py maps 1.20 furniture to a Decimation prop per cell (wood crates
  are drawn 1.38 long; hung lamps and wall shelves became floor props in the air; facing kept
  from the 1.20 state); props of turned street parts / scenes / fronts / parks were not turned
  (Rotation leaves Decimation blocks); our wrecks in DC street parts sat on lanes 6 and 9 of a
  6 wide road although a car is drawn 3.85 wide.
- Fix: city/PropFit at load (docs/city_engine.md "Props fitted to their drawn size"), props
  turned in LcCity.partAt, one centred and fit-tested wreck per chunk (StreetPlan.carFits).
  Same area after: 30 findings. Checker fixes on the way (trapdoors counted as doors, doors
  flagged for props beside them along the wall, pressure plates as full blocks).
- New chunks only. Left: about 14 crates in crate runs with a 0.19 overhang (loot kept rather
  than a third chest in a row), lamp arm false positives.

### Railway tunnels flooded with standing water and lava (11 Oktober 2026, found in testing, FIXED v0.44.0)
- Dev test `rail` (first runs): diamonds of standing water and lava on the track, no source
  block anywhere, rails washed out (43 of 48 through a station).
- Cause: a tunnel chunk is written by two population windows (Slices.window, offset 8). Between
  them a neighbour chunk's decoration found the open stone face of the first half: vanilla
  springs (BiomeDecorator, 50 water and 20 lava per chunk, WorldGenLiquids) go into stone with
  exactly one open side, and they flow at once (immediate updates during worldgen). The second
  half then wrote its tunnel over the spring block and left the flow standing.
- Fix (terrain/TerrainEvents): Decorate LAKE (the springs) and Populate LAKE / LAVA denied where
  their area touches a rail chunk (city/Rails.near). Checked: liquid in 11 tunnel chunks 0 (fresh
  region), seed 1 gap line 0. A second, unrelated flood: the station staircase's water wall
  (DeceasedCraft part) does not hold its water in 1.7.10; it became light blue glass.
- City cellars could meet the same springs `[not checked]`.

### Dedicated server crashed at start since v0.42.2 (found 11 Oktober 2026, FIXED v0.44.0)
- tools/servertest.py: "NoClassDefFoundError: net/minecraft/client/gui/GuiScreen" from
  Magazines.register (the first call into fixes/Deci).
- Cause: Deci.homeMenu() (v0.42.2, for the stats test picture) returns a GuiScreen; verifying Deci
  on a server loads that client class. Singleplayer was never affected.
- Fix: @SideOnly(CLIENT) on homeMenu (stripped on the server). servertest runs again.

### Enemy NPCs ignored gunshots, juggernaut too (11 Oktober 2026, FIXED v0.43.3)
- User on 0.43.2: "even on survival mode the juggernaut dont react when i shooting when there is
  no zombies". v0.43.2 (below) fixed only dead targets.
- Cause: Decimation's NPCs fight their revenge target (getAITarget): findTarget sets it together
  with the old style entityToAttack, vanilla drops the revenge target about 5 s later or when it
  dies, then findTarget looks again. entityToAttack is only ever overwritten by the next enemy
  found, never cleared, so it keeps the last enemy seen even while that one lives far away or out
  of sight. GunNoise's busy check read entityToAttack: an NPC that had ever seen any enemy counted
  as fighting forever, and with no zombies near nothing replaced it.
- Reproduced (dev test noise -Ptier=juggernaut -Pfar=45, stale living target held, a pig behind
  it): old code heard the shot and moved -0.1 blocks; fixed: came 13 to 15 and engaged.
- Fix (fixes/GunNoise Investigate.busy): busy only while the revenge target or the new AI target
  is alive; entityToAttack is ignored.
- Seen on the way (dev test, juggernaut 20 blocks away in sight, survival): Decimation's own
  targeting works, it engaged and hit the player on 8 ticks; in creative 0.
- Test hardening: noise test removes stray mobs, respawns infected Decimation swapped for a
  hulk / bloater, checks "took the noise" where wandering made distances flaky, counts hits on
  a survival player (damage on, health 100000); a sniper rifle Soviet engages from up to 70
  (sniperSearch) without walking, which counts. 4 runs in a row all PASS (10 checks).
- Confirmed by the user in game on 0.43.3: "nice it work now".

### NPCs that once killed something ignored gunshots (11 Oktober 2026, FIXED v0.43.2)
- User (0.43.0): "enemy npc do nothing when me shooting near them"; zombies did react. They were
  in Creative, where Decimation's NPCs never react to a player at all (isHostileTo is false for
  creative players in every NPC class: kept, new_feature.md "Gunshot noise").
- Found while testing the survival case: every NPC on the test arena showed a DEAD infected as
  its old style target (EntityCreature.entityToAttack). Decimation's NPCs run the new AI, so
  vanilla's updateEntityActionState, which clears a dead entityToAttack, never runs; Decimation's
  findTarget sets it and nothing clears it. GunNoise's Investigate counted that as "busy
  fighting", so an NPC that had killed anything never investigated a shot again.
- Fix (fixes/GunNoise Investigate.busy): a dead entityToAttack or attack target is cleared.
  Not enough: see the v0.43.3 entry above (a living stale entityToAttack blocked too).
- Check: dev test noise, 8 checks PASS: Soviet walks 19.8 toward a bandit's shot; survival player
  shot: Soviet heard, came 12, engaged the player; creative player shot: not heard. Not yet tried
  by the user in survival [not verified].

### Gun creeps down while aiming through a scope until it leaves the screen (11 Oktober 2026, FIXED v0.43.1)
- User (AK-74 with the ACOG, screenshots docs/shots/aimdrift_v0.43.1/user_116.png, user_117.png):
  "the guns go to below ... second by second until the gun dont show up"; they suspected the
  window size. Both shots have sniper NPCs in view; the scope sits down right of the centre, then
  the gun is nearly gone.
- Cause: ScopeZoom centres the sight by measuring where the scope glass is drawn (PatchScope adds
  a GL feedback measurement to Decimation's renderScopeGlass, every 8th draw). That method draws
  EVERY scope glass, also the scoped guns NPCs hold in the world. A measurement of an NPC's glass
  was taken for the player's sight: ScopeZoom learned a wrong position and pushed the gun away
  (reproduced: elite sniper NPC in view, gun shift x -3.89, gun off screen; without the NPC 0.005
  / -0.22). The window size was not the cause (1160 x 650 and 854 x 480 gave the same shift).
- Fix (fixes/ScopeZoom): measuring is off (mask -1) except during the player's own hand draw:
  onRenderTick switches it off at the start of every frame, onRenderHand switches it on just
  before the hand (ours while zoomed, vanilla's at hip) and off right after; a sample is learned
  only when it was taken inside that draw.
- Check: dev test `aimdrift` (AimDriftTest: AK-74 + ACOG aimed 15 s with an elite sniper NPC in
  view, -Psize / -Pturn / -Pnpc): old code shift -3.8931, 0.1726 (gun gone); fixed 0.0049,
  -0.2189 at 3, 8 and 15 s, same as with no NPC; sheet docs/shots/aimdrift_v0.43.1/
  sheet_fixed_npc_in_view.png. Not yet confirmed by the user in their own game [not verified].

### The user's dev world would not load: broken level.dat id table (11 Oktober 2026, FIXED v0.42.5)
- User: "world got corrupted or idk". The live test game (npckill) ran in deciworldgen_autotest,
  the world the user plays in. A cancelled command still sent "quit": DevAutoTest called
  mc.shutdown() inside the world, and the server's save and the client shutdown hook's save both
  wrote level.dat at 03:59:29; the file kept had 603 item ids missing (592 deci, 11 ours) and 15
  at -1. The next start failed: "Can't map item deci:ItemSelectionTool to id -1 (seen at: 4096)".
- Fixed: level.dat restored from level.dat_old (1656 entries, none at -1; the broken file kept
  in dev/run/client/world_backups/broken_leveldat_20261011_0359/). DevAutoTest.quitSafely leaves
  the world, waits until the integrated server stopped, then exits (checked: "leaving the world",
  "Stopping server", "world saved and closed", id table complete). Tests now use their own world
  deciworldgen_devtest; a fresh world test deleted the test world's folder, which was the user's
  world. Checked: the user's level.dat untouched by the next test run.

### Military jeep and tank wrecks break with one punch (reported 10 Oktober 2026, FIXED v0.42.5)
- User: "tank, jeep military can be destroyed with one click hand".
- Cause (read in deobf/src, net/decimation/mod/common/block/props/
  BlockWreckageMilitary1 and 2): the constructors call only
  `super(Material.iron)`, never `setHardness`, so hardness stays 0 and the
  block breaks instantly by hand, also in survival. The car wrecks
  (BlockProp) get hardness 10 from BlockProp. BlockWreckageMilitary3 not
  checked yet [not verified].
- Not the same as the vehicle entity bug fixed in v0.8.1 ("Military
  jeep/tank/helicopter destroyed in one hit" below): these are the WRECK
  props our military bases place. The user's shots were taken in creative,
  where any block breaks with one click; with hardness 0 survival breaks
  them in one click too [inferred from code, not tested in survival].
- Fix idea: set hardness / resistance on those blocks from our side at
  init (Deci accessor, `Block.setHardness`), like a BlockProp (10).
- FIXED v0.42.5: fixes/WreckHardness sets hardness 10 on the three (like a BlockProp) at init;
  dev test npckill: 50 s to break by hand in survival (log "wreck hardness 10: ...").


### Kill and death counters read the dead backend, "Player Kills" should be "Human Kills" (reported 11 Oktober 2026, FIXED v0.42.2)
- User: "players kill change to humans kill instead and the total can be view ingame or in main
  menu at bottom". Their HUD shows Player Kills 0, Zombie Kills 0, Deaths 0 (shots
  docs/shots/milbase_v0.42_review/).
- Code: the in game HUD (deobf/src/decimation/hud/IngameHud.java line 128) and the main menu
  list (GuiMenuBase line 180, GuiMenuHome line 111) read `ObjectPlayerProfile` from
  `BackendProfileCache` (the dead backend), not the local PlayerData. PlayerData does count:
  `DeathHandler` line 57 adds a player kill only when the dead entity is a PLAYER killed by a
  player; zombie kills are counted there too [line not read yet].
- Wanted: "Human Kills" = every human killed (bandits, soldiers, Soviets, juggernauts, elites,
  survivors and players), shown in game and at the bottom of the main menu, from local data.
- Fix idea: count human NPC kills in our own handler (like HumanityKillHandler), store in the
  player's data, rename the label, feed the HUD / menu from local data (Javassist on the label
  and the profile source, or draw our own line).

### Main menu shows "Play offline" and an offline banner for a real account (reported 11 Oktober 2026, FIXED v0.42.2)
- User: "main menu have issue with non crack user where main menu show up offline banner and
  play button say play offline instead play only text with green color background of the
  button". Wanted: the button says "Play" on Decimation's green background, no offline banner.
- Code (deobf/src/decimation/gui/GuiMenuBase.java around line 371): the green "Play" button
  (colour -13395661, tooltip "Play now!") only when `ClientState.bv` is true, otherwise the grey
  "Play offline" (-8355712); the same choice in GuiMenuProfile line 63 and
  GuiMenuSupporterLounge line 78. The banner: GuiMenuHome around lines 66 to 76 (checks the
  backend client's connection). `bv` is most likely set by a successful login to the dead
  Decimation backend, so it never turns on now [inferred, not traced yet].
- Fix idea: a Javassist patch (or Deci accessor at start) that treats a valid Minecraft session
  as "online" for the menu, without touching the dead backend calls (PatchBackend already
  removed the 5 s wait).
- FIXED v0.42.2 (both): fixes/LocalStats counts every kill and death in singleplayer per player
  UUID across worlds (config/deciworldgen_stats.cfg): human kills (players and every human NPC;
  Deci.isHuman, which excludes the infected: InfectedEntity extends Decimation's human entity
  class), infected kills, deaths; the client puts them into BackendProfileCache.profile and the
  totals line (DeciConstants) once a second. Deci.setSessionValid(true) at startup (green "Play",
  no banner). tools/patches/PatchMenuStats.java (ASM, three jars) renames the labels: Human Kills,
  Deaths, Total Human Kills, Total Infected Kills. Checked with dev test `stats` (kills +1 each,
  menu profile = local counts, totals, session flag; HUD and home menu pictures
  docs/shots/milbase_v0.42.2/stats_*.png); the test puts the counters back afterwards.
  Not changed: "Not Supporter :(" in the profile box (backend supporter flag).


### Colourful rail teeth on the M4A4 under a sight (v0.41.0, 10 Oktober 2026)
- Seen in our own shots: a rainbow strip on the receiver top of the M4A4 (and the HK416 / Mk18
  built from it) once a sight is fitted. The M4A4's rail teeth gunModel205..208 (x 2.1 .. 3.5)
  use a placeholder texture island with saturated colours; its tall rear sight always covered
  them, and since v0.40.0 that sight folds away under a sight (IronSights).
- Fix: our variants repaint the four islands with the clean tooth gunModel204's (tools/guns/mk18.py,
  hk416.py); on Decimation's own M4A4 (its art, unchanged) IronSights hides those four teeth while a
  sight is attached (the sight covers that spot). Check: gunview Mk18 / M4A4 / HK416 with the
  EOTech (docs/shots/sights_v0.41/final_eotech_three_guns.png).

### Sights float above flat top rifles (v0.40.1, 10 Oktober 2026)
- User (Honey Badger and Mk18 screenshots, red dot): the sight hangs in the air above the gun.
- Cause: GunItemRenderer.renderAttachments draws every sight at one spot, made for receivers
  whose top in the sight zone is about y -4.45 (Uzi, MP5). Flat top rifles sit lower (M4A4 rail
  teeth -3.6, Honey Badger -3.85, ACR -3.87): 0.6 to 1.6 of air. Their tall folding rear sight
  used to fill it (the v0.36 M4A4 shot has the red dot frame at the same screen spot, resting on
  the rear sight); since v0.40.0 that sight folds away under a sight (IronSights) and the gap
  shows. Not a render bug: the NPC side view matched study.py.
- Fix: tools/guns/sightfit.py measures each gun's rail top in the sight zone (x 2 to 3.5, long
  parts plus rail teeth, iron sights excluded) and writes
  assets/deciworldgen/sight_offsets.txt (dy = rail top + 4.45, only downwards, 20 guns x 5
  sights); fixes/SightPlacement moves those sights down per gun at startup
  (Deci.offsetAttachment) and, while aiming in first person, raises the whole gun by the same
  dy (from IronSights.begin inside the gun draw method), so the sight picture stays where it was.
- Check: gunview reddot on Mk18 / M4A4 / ACR / Uzi: hip on the rail, aim dot on the centre lines,
  Uzi unchanged; 2x on Mk18 / ACR the same. docs/shots/sights_v0.40.1/.
- Open: the muzzle flash is drawn outside the gun draw method, so while aiming with a lowered
  sight it shows dy below the muzzle `[inferred, not checked]`.

### Some NPC machine guns fire at double rate (v0.39.2, 10 Oktober 2026)
- Verdict (user, in game, 10 Oktober 2026): "feels right now, wew. better than before".
- User: "juggernaut who using pkm, the firerate is ridiculous, like using mg guns ... the elite
  mobs who using pkp is most balance ... this happen not in juggernaut but on other npc too".
- Measured (new dev test `firerate`: one NPC per tier and gun on the arena, its own AI only,
  every shot timed): inside a burst the PKM, M240, MG3 and M4A4 fired every tick (1200 rpm),
  the PKP, AK74, AKM every 2nd tick (600). Juggernaut PKM: 1 tick gaps, 46 shots in 15 s.
- Cause: Decimation runs a gun at 1.3 x its listed rpm (GunStats: seconds per shot =
  1 / (rpm x 1.3 / 60)); our burst code (NpcShots) rounded that to whole ticks: PKP 600 ->
  780 -> 1.54 ticks -> 2; PKM 650 -> 845 -> 1.42 ticks -> 1. A 50 rpm gap on paper became
  double the rate.
- Fix: NPC bursts never faster than `maxBurstRpm` (config deciworldgen_npc.cfg, npc_fire,
  default 600 = the elite PKP), slower guns at their own rate with the tick fraction carried
  from round to round instead of rounded away (NpcLoadouts.burstTicksPerShot, NpcShots).
- Check: firerate after the fix: every case 2 tick gaps; juggernaut PKM 232 rpm overall,
  elite PKP 340, elite PKM 268, elite MG3 324, soldier M240 304, bandit heavy PKM 244, AKM
  124, Uzi 84 (15 s each, bursts plus pauses); `shots` all PASS. Players keep Decimation's
  real rates (a player's gun times shots in milliseconds, not ticks).

### 1 fps in the dev world: entity explosion through a corrupt chunk (v0.38.1, 10 Oktober 2026)
User: "really big performance issue now ... 1 fps after i drop it to the ground". Not the gun
(gunperf: our guns cost what Decimation's cost). The dev world's region slot (-1,-6) held chunk
(-4,-1)'s data, so entities were saved twice and doubled on every load (9 800 -> 40 730 in one
session, 31 010 Hulks), and Forge printed a stack trace for each every tick. Fixed in the save
(tools/perfcheck.py fixchunk, census kill=); origin of the broken slot unknown `[not verified]`
(it already crashed the server on 10-09 04:12). docs/performance.md section 2.

### Infected path search every tick (Decimation, v0.38.1, 10 Oktober 2026)
InfectedEntity.onLivingUpdate (deci.ag.d) ran updateWanderPath every tick on server and client
plus a 40 x 40 horde scan every tick: about 60 % of both threads in pathfinding. Patch
tools/patches/PatchInfectedAI.java (three jars): wander server side once a second, horde scan
every 10 ticks. Same 53 infected: fps 21 -> 35, client pathfinding 60 % -> 0 %. zombies test
passes. docs/performance.md section 2.

### Upper storeys unreachable (ladder popped off, stair core collapsed)
- **Found**: 7 Oktober 2026 by the building audit (`tools/floorplan.py`
  reachability): shop b-4_1_0 storey 1 0% reachable, office b-3_2_2 top
  storey 0% reachable (seed 1, Decimation world type).
- **Root causes (ours)**: (1) buildings without a stair core use a ladder
  at (W-2, L-2) hanging on the back wall. The wall cell could be a window
  or a decay hole, and the wall column often belongs to the NEXT population
  window, so it does not exist yet when the ladder is written; any block
  update meanwhile (a metal shelf next to the ladder being completed with
  flag 3 since 0.16.0) makes the unsupported ladder pop off. (2) The
  collapsed corner cone also removed ladder and stair core cells on the
  top storeys.
- **Fix (v0.16.1)**: the wall cell behind the ladder is always solid wall;
  no furniture on the 4 cells around the ladder; ladder shaft and stair
  core (plus a 1 block ring) are exempt from the collapse. Rescan of 105
  buildings: no storey 0% reachable. Remaining low reachability comes from
  full rubble cubes in doorways (step 4d rubble redesign; jumpable in game).

### Supply drop crate vanished on a street prop
- **Found**: 7 Oktober 2026 by the dev autotest after street furniture
  (0.15.0): drop column 14/62 was a sidewalk with a trash bag on it.
- **Root cause**: same mechanism as the flower case below. Decimation props
  have collision boxes, the falling crate stops on one inside the prop's
  cell, cannot become a block there and dies.
- **Fix (v0.16.0)**: `SupplyDropScheduler.drop` skips a candidate column
  whose top block has a tile entity (any prop, chest, car) and tries the
  next of its 12 random positions. Autotest passes again.

### Supply drop crate vanished when it landed in a flower
- **Found**: 7 Oktober 2026 by the dev autotest (drop column 70/287: crate
  entity fell, no crate block anywhere in the column).
- **Root cause (vanilla behaviour, our column choice)**: the crate is an
  `EntityFallingBlock` (`EntityFallingSupplyDrop`); on landing it turns into
  a block only if the cell is replaceable. Flowers, saplings and the tall
  flowers are not (`BlockDoublePlant.isReplaceable` is true only for tall
  grass and fern), so the entity died and the drop was lost. The column had
  a 2 block tall flower.
- **Fix (v0.13.0)**: `SupplyDropScheduler.clearLanding` removes
  non-replaceable plants in the drop column down to the first solid block or
  liquid before deploying. Autotest passes again (crate block at y=71). The
  flower case itself was not reproduced (drop columns are random).

### Graded yard sand fell into caves (hole next to a building)
- **Found**: 7 Oktober 2026 by `tools/gradescan.py` on seed 7 while building
  terrain blending (a ring column 49 blocks below the floor).
- **Root cause (ours)**: the grader filled raised yard columns with the
  column's own surface block. Sand and gravel are `BlockFalling`;
  `setBlock` calls `onBlockAdded`, which schedules the fall even during
  generation, so a sand fill over a cave dropped into it.
- **Fix (v0.13.0)**: falling blocks are never used as fill (sand fills with
  sandstone, gravel with stone), and a sand or gravel top goes only on a
  solid block. Rescan of 3 seeds: ground at the walls within 1 block of the
  floor everywhere.

### Generated metal shelves invisible (multiblock master never set)
- **Reported**: 7 Oktober 2026 by the user; the culling patch below did not
  fix it (still invisible right next to the prop).
- **Root cause (ours)**: multiblock props (tile entities extending
  `deci.W.a` MultiblockPart, e.g. `deci:BlockMetalShelf`, MetalShelf_Empty)
  are drawn only by their master part: `MetalShelfRenderer` returns unless
  `isMaster()` (stored master position == own position). The master is set
  by the block's placement code; world generation places the block directly,
  so the master stayed 0,0,0 and the shelf never rendered. Found by reading
  the tile entity NBT (`multibl.mx/my/mz` = 0) in a generated world.
- **Fix (v0.12.3)**: `Slices` and the small-schematic placer call
  `setSelfMaster()` on every multiblock part they place;
  `MultiblockRepairHandler` makes orphaned parts (master never set) their own
  master on chunk load, which also repairs worlds generated before 0.12.3
  and any legacy Decimation prop broken the same way. Verified: new world
  407/407 shelves are masters; a copy of the user's pre-fix world: every
  shelf in the loaded area repaired, the rest repair when visited.

### Props randomly not rendered (legacy Decimation bug)
- **Reported**: 7 Oktober 2026 by the user (screenshot: selection box of a
  placed prop, no model). Happens with player-placed props too, mostly
  props whose model is bigger than one block (tables etc).
- **Root cause**: `PropRenderer` (`deci.I.l`) first calls
  `LineOfSight.canSeeTileEntity` (`deci.a.c$a.a(EntityPlayer,TileEntity)`):
  8 rays from the eye to the 8 corners of `TileEntityProp.getRenderBoundingBox()`,
  drawn only if one ray is clear; Forge's frustum culling uses the same box.
  That box is the bare 1x1x1 cell for 36 of 72 props (no render size
  declared) although their models are drawn up to ~1.5 blocks wide, and it
  never rotates with the prop (the model turns by metadata % 4 * 90). So the
  corners land off screen or behind floors / walls while the model is in
  view, and the prop is skipped.
- **Fix (Javassist, `tools/patches/PatchPropCulling.java`)**:
  `getRenderBoundingBox` returns a box centred on the block, rotation proof,
  at least 2x2, covering the declared render size, half a block taller;
  `canSeeTileEntity` returns visible within 4 blocks and tries a ray to the
  box centre first. Applied to `Decimation.jar.patched`,
  `dist/Decimation.jar`, `dev/libs/Decimation-base.jar` (Prism needs the new
  `dist/Decimation.jar`). Verified: classes load, no render errors in the
  autotest, a table's box is now 99.5..101.5 instead of 100..101. Visual
  confirmation pending. NOTE: the user's invisible shelf was actually the
  multiblock bug above; this culling fix still stands for normal props.

### City buildings missing a whole wall at sector borders
- **Reported**: 7 Oktober 2026 by the user (0.11.0 in game: "one side has no
  wall, looks cut off like old village bugs").
- **Reproduced**: `tools/wallscan.py` on seed 1 + pregen: 2 of 33 buildings
  had a side 97% air. One was only the edge of the generated area (owner
  chunk never populated). The other (`b4_4_2`, west wall x=263) was real.
- **Root cause**: a chunk's population window is 16x16 at +8/+8, so it
  reaches 8 blocks into the neighbouring chunks and can cross a SECTOR
  border. `StructureGenerator.generateImpl` only ran the city writer when the
  populating chunk itself was in a CITY sector; a building in a city sector
  whose column fell in a window owned by a non-city chunk never got written.
  The large-schematic writer had the mirror bug (not run for city chunks).
- **Fix (v0.11.1)**: both slice writers run for every chunk (each checks the
  sector of the structure, not of the populating chunk); only small ruins
  and streets use the populating chunk's sector. Rescan: 37 buildings, 0 real
  missing walls, 4 gaps all at unpopulated edge chunks.
- **Note for players**: buildings straddling chunks generated by an older
  version (or never populated yet) still look cut off until those chunks
  populate; use a new world for a clean test.

### No supply drops in singleplayer
- **Found**: 7 Oktober 2026 (naming pass).
- **Root cause**: the 15 minute countdown lives in `ServerTickHandler`
  (`deci.aK.q`, ServerProxy only) and every `SupplyDropSpawner` (`deci.aM.c`)
  method checks `isServer()`. Drop points came from an admin file
  `decimation_supplydrop.properties`, which singleplayer never has.
- **Fix (v0.9.0, dist only)**: `SupplyDropScheduler` runs the same countdown
  (`ServerConfig.supplyDropCountdown/Interval`, 18000 ticks), the same 10 and
  5 minute chat warnings, then places the mod's own falling supply drop block
  at a random LOADED column 64 to 144 blocks from a random player and
  announces the coordinates. Honours `enableSupplyDrops`. The crate drifts
  down for roughly 45 s. Verified by the autotest (dropped, landed at y=69).

### Humanity never changed from ordinary kills in singleplayer
- **Found**: 7 Oktober 2026 (naming pass).
- **Root cause**: `DeathHandler` (`deci.an.f`) runs in singleplayer, but its
  "+1 per non player kill / -10 for killing an innocent player / +1 for any
  other player" block is behind `isServer()`. Only the victim's own
  `getHumanityOnKill()` value applied.
- **Fix (v0.9.0, dist only)**: `HumanityKillHandler` replicates that block
  without the clan branch (clans are dedicated only). Killing one infected now
  gives +2 (its own +1 plus the rule's +1), the same as on the official
  server. Verified by the autotest (50 -> 52).

### Armor damage-reduction buff doesn't affect NPC gunfire
- **Found**: 27 Juli 2026, while trying to explain how to test the armor buff
- **Symptom**: `ItemArmorDeci.damageMultiplier` (recently buffed 35%, see
  `new_feature.md`) has zero effect when bandits/soldiers shoot the player.
- **Root cause**: the multiplier is only ever consumed in one place in the
  whole mod - `deci.aE.a$z$a`, the network handler for a **player's own**
  gunshot hit report (client tells server "I hit X for Y damage", server
  applies the multiplier there). NPC ranged attacks
  (`deci.ag.a.e(EntityLivingBase)`) call vanilla `attackEntityFrom` directly,
  server-side, with a different `DamageSource` type (`"human"` vs the
  player-shot `"gunDeci"`) - completely bypassing the multiplier.
- **Proposed fix**: add a `LivingHurtEvent` listener (fires for all incoming
  damage regardless of source), filtered to the known gunshot-type
  `DamageSource`s (both `"gunDeci"` and `"human"`, possibly more NPC variants
  to confirm), apply the multiplier there instead - and remove the
  now-redundant multiplication inside `deci.aE.a$z$a` so player-vs-mob shots
  don't get it applied twice.
- **Status 7 Oktober 2026: FIXED** (user said go). Implemented slightly
  differently from the proposal: the player hit handler `deci.aE.a$z$a` is
  left untouched (it only applies the multiplier when the target is a player,
  i.e. PvP), and `ArmorGunfireHandler` (LivingHurtEvent) applies the same
  rule to the NPC sources `"human"` (bandits, armed traders) and `"turret"`.
  Same pieces, same helmet exclusion, no double application, no bytecode
  patch. Every Decimation damage source bypasses vanilla armor, so before
  this armor did literally nothing against NPC bullets. Verified by the
  singleplayer autotest: 10 human damage, bare 10.00 taken, with a
  spetsnazVest (x0.553) 5.52 taken.
- **Helmets (v0.9.1, user decision: headshots only)**: helmets carry a much
  stronger multiplier (0.3 raw, 0.195 after the 35% buff) than other pieces
  (0.8 to 0.85), which is probably why the original skipped them; on every
  hit a full set would block about 97%. Now: player guns (`gunDeci`) add the
  helmet only when the shooter's aim line crosses the victim's head band (top
  25% of the body); NPC/turret hits use it on a random 20% (no hit position,
  and NPCs always aim at eye level). Verified: 81/400 NPC hits used it; a
  player gun aimed at the head took 4.88 (helmet x0.487), at the chest 10.00.

### Background thread pinned one CPU core all session (SmoothSwingThread)
- **Found**: 7 Oktober 2026 by the naming pass, verified in code.
- **Root cause**: `deci.b.h.run()` (thread "SmoothSwingThread", started by
  ClientProxy) busy-waits with no sleep to advance the weapon sway clock
  `deci.b.i.bz` at 60 Hz.
- **Fix**: Javassist patch (`tools/patches/PatchSwing.java`), same 60 Hz
  accounting plus a 4 ms sleep, applied to `Decimation.jar.patched`,
  `dist/Decimation.jar`, `dev/libs/Decimation-base.jar` and Prism. Measured
  on Prism, idle main menu: 200% CPU -> 105%. Thread dump shows it sleeping.
  Weapon sway not yet looked at in game. May help the FPS reports below
  (less heat / throttling), not a proven fix for them.

### Prism instance crashed on every launch (CustomSkinLoader)
- **Found**: 7 Oktober 2026, while testing the separate deciworldgen jar.
- **Symptom**: crash during startup, before any mod loads, with
  `NoClassDefFoundError: net/minecraftforge/fml/relauncher/IFMLLoadingPlugin`.
- **Root cause**: `CustomSkinLoader_Universal-15.0.1.jar` (added to `mods/` on
  24 Agustus) ships a coremod built for Forge 1.8+ package names. Not related
  to our changes; the last successful launch log before it was 15 Agustus.
- **Fix**: renamed to `CustomSkinLoader_Universal-15.0.1.jar.disabled`
  (rename back to restore). A 1.7.10 build of CustomSkinLoader would be
  needed to keep custom skins.

### Generated structures built on the ocean floor
- **Found**: 7 Oktober 2026 (dedicated-server test, region file inspection).
- **Root cause**: `World.getTopSolidOrLiquidBlock` skips water in 1.7.10, so
  our "is this site water" check inspected the seabed instead of the water.
- **Fix**: v0.7.0 checks the block above each sampled ground point. Verified:
  same seed, all 4 underwater sites now rejected. Older worlds keep theirs.

### Zones never active in singleplayer
- **Found**: 7 Oktober 2026.
- **Root cause**: zone list loaded only by ServerProxy, zone handlers
  `deci.aK.d/g/i/m` are `@SideOnly(Side.SERVER)`. Same pattern as the loot bugs.
- **Fix (partial)**: v0.7.0 loads per-world generated zones and replicates the
  zone spawn handler (`deci.aK.d`) in singleplayer, verified by the
  unattended singleplayer test. Player effects of safezone/radiation/scary
  zones (`deci.aK.g/i/m`) are still not replicated.

### Loot never worked in singleplayer (crash on right-click)
- **Found**: 25 Juli 2026
- **Symptom**: crate/car/ammobox/supply drop right-click did nothing (or later,
  crashed) in singleplayer only. Worked fine in old multiplayer.
- **Root cause**: `ClientProxy` (`deci.a.c`) never registered the loot
  interact handler (`deci.aK.k`) - only `ServerProxy` (`deci.a.e`, dedicated
  server only) did. Singleplayer's integrated server reports physical side
  `CLIENT`, so ClientProxy is what loads even though it's hosting the "server."
  Handler also carried `@SideOnly(Side.SERVER)`, which gets ASM-stripped when
  loaded under `CLIENT` regardless.
- **Fix**: added `deci.aK.k` registration into `deci.a.c.preInit()`, stripped
  the `@SideOnly` annotation from the handler method.
- **Fixed**: 25 Juli 2026, 13.12

### `ClassCastException: deci.a.c cannot be cast to deci.a.e`
- **Found**: 25 Juli 2026, 13.09 (crash report), right after the above fix
- **Symptom**: crash on interacting with any lootable block, immediately after
  the previous fix went in.
- **Root cause**: `deci.aK.k`'s handler also calls `deci.a.b.d()` (which
  hard-casts the proxy to ServerProxy) to reach the clan-turf manager for an
  unrelated permission check. Fine on dedicated server, crashes on
  ClientProxy.
- **Fix**: `deci.a.b.d()` made null-safe (`instanceof` check, returns `null`
  instead of throwing). `deci.aK.k`'s two call sites that use the result made
  null-safe too (turf check treated as "no restriction" when null - correct,
  since clans/turf don't exist in singleplayer anyway).
- **Fixed**: 25 Juli 2026, ~13.15

### Loot GUI never opens (sound plays, no window)
- **Found**: 25 Juli 2026, 13.20
- **Symptom**: right-click plays the loot sound and marks the position looted,
  but no GUI window appears. Second click reports "this is empty."
- **Root cause**: the GUI-open call is scheduled on a delayed-task queue
  (`net.decimation.mod.common.utils.b`), drained only by
  `deci.aK.q` (`TickEvent.ServerTickEvent`) - which is *also*
  ServerProxy-only, so the queue is never processed in singleplayer.
- **Fix**: added a new `TickEvent.ClientTickEvent` listener directly on
  `deci.a.c` (already registered to the event bus) that drains the same
  static queue. Deliberately did *not* register the full `deci.aK.q` class -
  it pulls in unrelated dedicated-server-only logic (network reconnect,
  anti-dupe scan, clan supply-crate announcements) not wanted in singleplayer.
- **Fixed**: 25 Juli 2026, 13.20

### Crash opening ammo crate (large)
- **Found**: 25 Juli 2026, 13.40 (crash report)
- **Symptom**: `NullPointerException` in `RenderItem` while rendering the loot
  GUI, specifically for the large ammo crate.
- **Root cause**: `deci.aD.k.avk` (an item field) is declared but never
  assigned anywhere in the mod - dead/leftover reference, always `null`. It's
  the first entry in the large ammo crate's loot pool
  (`deci.aD.l$4`, 25-entry list). Pre-existing bug in the original abandoned
  mod, unrelated to any of our patches - only surfaced once loot started
  working at all.
- **Fix**: substituted the dead field read for its valid neighbor
  (`deci.aD.k.avl`) in `deci.aD.l$4`'s constructor.
- **Fixed**: 25 Juli 2026, ~13.55

---

## Pending decision

(none)

---

## Not investigated yet
(Some entries below were fixed later and say so in their heading; the open
ones on 9 Oktober 2026: base height order, prop dense FPS, LED lamp floor (fixed)
at the end of this file.)

### Building base height depends on chunk generation order (ours)
- **Found**: 8 Oktober 2026 (fresh seed 1 worlds, `tools/worlddiff.py`):
  apartments b-1_3_0 and b0_3_0 sat at y 64 in one run and y 63 in
  another, with the same seed.
- **Cause**: `Slices.decideBase` samples the footprint where
  `world.blockExists` is true when the building's FIRST slice is written,
  so the result depends on which neighbour chunks happen to exist then.
  The chosen height is stored (StructureData), so a building is always
  whole; only the height can vary between two new worlds of one seed,
  near the edge of what was generated. Spawn area: identical.
- **Impact**: low (no broken buildings); matters for "same seed, same
  city" expectations and for refactor checks (use `--within`). A fix
  would sample terrain height from the chunk generator (noise) instead of
  the world `[not verified]` how costly that is.

### FPS drop while aiming through scopes (red dot/2x/4x/6x) (FIXED v0.28.0..0.28.4)
- **Reported**: 26 Juli 2026; user 8 Oktober 2026: about 150 -> 110 fps
  with a scope, asked for the old cheap method (view zoom) instead of the
  two camera method, keeping the old code switchable.
- **Root cause (code, deobf ClientRenderHandler.renderScopeView)**: the
  world is rendered a second time (narrow FOV, up to 1024 x 1024) into a
  texture every frame a scoped gun is HELD, aiming or not; BModel
  renderScopeGlass draws it on the glass.
- **FIXED v0.28.0**: `tools/patches/PatchScope.java` gates renderScopeView
  behind system property `decimation.scope.pip` (method otherwise
  untouched: the old scope returns with `pictureInPicture=true` in
  `config/deciworldgen_scope.cfg`). `net.decimation.fixes.ScopeZoom`
  zooms the whole picture while aiming (reddot 1.25, 2x, 4x, 8x, dragunov
  4x, configurable) and copies the centre of each finished frame into the
  scope texture (RenderWorldLastEvent, before the hand) so the glass looks
  see-through.
  - v0.28.0 zoomed only the world (FOV; the hand renders at a fixed FOV
    70, so the gun stayed 1x). User then chose the "NORMAL" look of a
    NORMAL / PIP comparison picture: world AND gun zoom together.
  - v0.28.1: EntityRenderer.cameraZoom = magnification (eased, log
    space). Vanilla renderWorld skips renderHand while cameraZoom != 1,
    so ScopeZoom calls renderHand itself on RenderHandEvent (fired right
    before that check); renderHand applies the same cameraZoom scale, so
    the gun zooms with the world. Glass copy sized glassView x zoom. The
    eyepiece sits a little above the screen centre at 4x, but the glass
    shows the screen centre (the aim point), so the reticle aims true.
  - v0.28.2 (user: glass misaligned fullscreen / windowed; gun swinging
    wildly above 1x in a first centring try): the WHOLE world frame is
    copied and the patched glass maps it by screen position (projective
    texturing), so it is see-through at any window size and zoom. The
    glass reports its screen box (GL feedback, every 4th frame); the gun
    is moved (cameraYaw / cameraPitch, hand only) so the sight sits on the
    screen centre where shots go. The sight position is learned per gun +
    scope + window aspect, only from unclipped samples and after 3 samples
    in a row agree (the first try corrected every frame from old samples
    and chased the weapon sway; a sample taken while the gun swung in from
    the hip pushed the glass off screen for good); the first aim with a
    new combination holds 1.5x up to 0.8 s while it learns. Mouse turning
    slows to 1 / zoom while zoomed (config "sensitivity"). Checked: reddot,
    2x, 4x, 8x, integrated (aug1) in 854x480 and 900x895, reticle on the
    centre in all 10 (docs/shots/scope_v0.28.2); a +-4 degree sweep moves
    the sight 6..25 px (normal weapon lag). Java 8 trap: Javassist on a
    newer JDK compiled FloatBuffer.flip() with the Java 9 return type
    (NoSuchMethodError in game): call it through java.nio.Buffer.
  - v0.28.3 (user asked for a faster, more efficient way): the copy now
    covers only the glass's last measured box plus a margin (whole frame
    while the zoom changes), into a texture allocated once per window size
    (glCopyTexSubImage2D); GL buffers allocated once; the glass is measured
    every 8th frame (every 2nd while a new sight is learned: 10 settled
    samples averaged, zoom held at 1.5x up to 1.5 s, which also fixed the
    integrated aug scope learning a still moving pose). Tried and dropped:
    a depth only glass drawn before the body (no copy at all): attachment
    glasses are drawn AFTER the gun body, so its front sight showed in the
    glass, and the integrated scope stayed grey. Checked all 10 (5 sights x
    2 windows) centred and see-through, docs/shots/scope_v0.28.3.
  - v0.43.1: measured only during the player's own hand draw (NPC guns' scope glass was taken
    for the player's: "Gun creeps down while aiming" above).
  - v0.28.4 (user: "for 8x better use the fake it method with a black
    layout", then "include 4x"): scopes from `overlayFrom` (config,
    default 4) hide the gun once the zoom is 70% in and draw a classic
    sniper overlay on the HUD (RenderGameOverlayEvent HELMET): black
    outside a round view (radius 0.47 x the smaller screen side), a
    darkening edge of 12 thin rings, the scope's own reticle texture
    (textures/model/guns/scopes/<name>.png, black lines on transparent)
    full size, vanilla crosshair hidden. No gun, no glass copy, no sight
    learning for those scopes. GL traps met: the circle strips wind
    backwards in GUI coordinates (cull face off), the HUD shades flat
    (per vertex colour fades became spikes: solid rings instead). A
    "white haze" at the edge was the sky fading to black, checked by
    pixel values (190,213,250 -> 149 -> 97 -> 56 -> 0). Checked 10 shots
    (docs/shots/scope_v0.28.4): reddot / 2x gun with see-through glass,
    4x / 8x / integrated overlay, reticle on the centre.
- **Measured** (`./gradlew runClient -Pautotest -Pscope`, dev client,
  unlimited fps, 4x on an ak74): picture in picture empty hand 85..103,
  held 45..51, aiming 37..44; view zoom empty hand 70..92, held 64..76
  (the gun model only), aiming 105..132. Shots docs/shots/scope_v0.28.0.
  v0.28.1 on a busy machine: picture in picture empty 26, held 10,
  aiming 10; camera zoom empty 28, held 32, aiming 42.
- First try also gated renderScopeGlass: the glass vanished and the scope
  showed its solid black body (looked like a bigger gun); reverted.

### FPS drop in prop-dense areas (cars/crates/shelves etc.)
- **Reported**: 26 Juli 2026
- E.g. ~100fps baseline down to 20-30fps around 15+ props, worse with 50+.
- **Measured 9 Oktober 2026** (`./gradlew runClient -Pautotest -Pprops`,
  last autotest world: stone platform high up, 15 x 15 mixed props;
  `-Pjfr` adds a Java Flight Recorder CPU profile, run/client/profile.jfr,
  read with JDK 25 `jfr print --json`; the client thread is `main` in dev).
  Before: empty 28..29 fps, 225 props in view 12..14, hidden in a stone box
  23 (box alone 24..27). Profile: 76% of PropRenderer (`deci.I.l`) time in
  ClientProxy.LineOfSight.canSeeTileEntity (`deci.a.c$a.a`): up to 9 ray
  casts per prop per frame, a chunk lookup (LongHashMap) per block per ray;
  hidden props pay all 9 and are not drawn anyway. canSeeEntity does the
  same for every living entity and vehicle.
- **IMPROVED (v0.28.6, PatchPropCulling step 3)**: both answers cached until
  the player (or the entity) moves 0.3 blocks, at most 1.0..1.3 s for props
  and 0.15..0.18 s for entities, staggered per object. (A first try with a
  plain 80..111 ms lifetime rechecked nearly every frame at low fps; a null
  player on the main menu preview crashed the cache until guarded.) After:
  line of sight 25 of 251 prop renderer samples (was 804 of 1053); prop
  renderer share of the client thread 21% -> 9%. All autotest checks pass;
  props and NPCs still render (docs/shots/props_fps).
- **Left**: what remains is drawing the models (100% in glCallList, driver
  / GPU work): 225 visible props still about halve the fps on the test
  platform.
- **City street, after the cache** (`-Pcityfps`: x 8.5, y 68, z 120.5
  looking north, seed 1 autotest world, + `-Pjfr`): props 5.5% of the
  client thread, chunk drawing 15%, chunk rebuild 12.7%, entities and tile
  entities 11.8%. Props are no longer the bottleneck there.
- **Render distance by size** (PatchPropCulling step 4, config
  `deciworldgen_props.cfg`: small < 0.8 block, medium < 1.6, large): 32 / 48
  / 64 measured 35..40 fps vs 41 at 64 / 64 / 64 in that street (noise, no
  gain) and can pop in, so the default is 64 for all (vanilla); lower it
  for open prop heavy places.
- **Baking static props into chunk meshes** (the full fix for open views
  with hundreds of visible props): not started; worth it only if the user
  finds places that still drop (atlas for every prop texture, both model
  formats to quads).
- Side finding: Decimation's postInit blocked about 5 s at launch connecting
  to its dead server (kryonet `deci.aP.a.e`), on the main thread; startup
  only, not in game. FIXED 9 Oktober 2026 (`tools/patches/PatchBackend.java`):
  connect timeout 300 ms, and kryonet's own Client.connect (bundled in
  Decimation.jar) now passes it to the TCP connect, which it hardcoded to
  5000 ms (shortening only the first timeout still waited 5 s: measured).
  Launch: "Registering new client network" -> "Unable to connect" went from
  5 s to under 1 s; later reconnect attempts still fail quietly in their
  own threads as before.

### Military jeep/tank/helicopter destroyed in one hit (survival mode) (FIXED v0.8.1)
- **Reported**: 26 Juli 2026, clarified 27 Juli 2026
- **Symptom** (clarified): a single click/hit destroys the vehicle outright,
  like breaking a normal block - it should instead be tough/unbreakable this
  way, same as other static props (e.g. crate/prop blocks return hardness `-1`
  or similar unbreakable-by-hand values). Not about health-bar/HP tuning -
  about whatever block/entity property currently lets one click destroy it at
  all.
- **Root cause (7 Oktober 2026)**: vehicle body `deci.ad.e.attackEntityFrom`
  (subclasses `deci.ad.f/g/h/i/j`, `i` = hummer) treats any player hit on an
  EMPTY vehicle as "pick up": `setDead()` + drop its item, survival included.
  Only gun damage (`gundeci`) takes the health path. Seat/hitbox parts
  `deci.ad.b` / `deci.ad.a` forward every hit to the body. It is the
  original design (minecart style pickup), not a singleplayer bug.
- **FIXED in v0.8.1 (dist only, Prism still 0.7.0)**: `VehicleHitHandler`
  cancels Forge's `AttackEntityEvent` for survival players on vehicles and
  their parts unless sneaking. Sneak + punch still recovers an empty vehicle
  as an item (compromise so survival can still pick vehicles up; one line to
  remove). Creative unchanged; gunfire/explosions still use vehicle health.
  Not covered: projectiles (arrows) fired by a player still trigger pickup.
  Verified by the unattended singleplayer test: survival punch leaves a
  hummer alive, sneak punch picks it up.
- **Arrows FIXED in v0.48.1 (11 Oktober 2026)**: the body's pickup test is
  `getEntity() instanceof EntityPlayer`, and an arrow's source entity is the
  shooter (indirect source), so arrows, thrown items and the player's own TNT
  picked an empty vehicle up (TNT `[inferred]` from vanilla Explosion: its source is
  indirect with the placer as the entity; not tested). No Forge event covers damage to a non living
  entity, so `tools/patches/PatchVehicleArrows.java` (Javassist, three jars,
  only vanilla names) swaps an indirect player source for
  `DamageSource.generic` at the start of `deci.ad.e.func_70097_a`: those hits
  now take the health path. Melee, sneak pickup, creative punch and gun
  bullets unchanged. Dev test `checks` "vehicle arrow" (arrow and snowball
  damage with the player as shooter): FAIL before the patch (picked up), PASS
  after. A real bow shot in game not tried `[not verified]` (the test calls
  attackEntityFrom with the same source EntityArrow builds).

### Bottlecaps only work as a plain item in singleplayer, not as currency
- **Reported**: 27 Juli 2026
- **Status 7 Oktober 2026: FIXED.** `BottlecapHandler` in deciworldgen
  replicates `deci.aK.e`. Verified by the unattended singleplayer test: 5
  bottlecaps dropped on the player, balance 0 -> 5, no caps left as items.
  The HUD display of the balance has not been looked at by the user yet.
- **Symptom**: picking up bottlecaps keeps them as a literal `ItemStack` in
  the inventory, instead of auto-converting into the currency
  counter/balance the way it did on the old official multiplayer server.
- **Likely root cause** (not yet confirmed, pattern strongly suggests it):
  earlier investigation of `deci.aL.a`/`deci.aK.*` found an
  `EntityItemPickupEvent` handler that detects bottlecap items specifically
  (`k.alK`/`k.alL`), deducts the picked-up stack, and credits a player-cap
  currency field instead (shows a "+N bottlecaps" message). That handler is
  very likely one of the same ServerProxy-only `deci.aK.*` classes as every
  other singleplayer bug found this session - never registered on
  `ClientProxy`, so it just never fires in singleplayer, leaving caps as
  plain items. Needs confirming the exact class and applying the same
  registration fix already used for the loot handler (`deci.aK.k`) and the
  tick-queue drain.

## City edge ramp missed its outer columns (fixed v0.24.4, 8 Oktober 2026)
`LcCity.populate` only planned the cells under the population window, so a
window lying wholly outside the city never wrote the outer 1..2 columns of
the 10 wide edge band (a small step at the band's end). Also small and
large sites next to a city could sit inside the band and get cut under.
Fix: populate scans cells within EDGE of the window; sites within EDGE of a
city are dropped. Checked with tools/edgescan.py (seed 1).

## Converted buildings: LED lamp blocks as floor (reported 8 Oktober 2026, FIXED 9 Oktober 2026)
User saw some converted (DeceasedCraft) buildings with LED lamp blocks
used as floor.
- **Found with** `tools/lcaudit.py` (counts every source block of every
  converted building by placement: floor / ceiling / wall / free).
- **Cause**: not the simplylight lamps. tools/lctranslate.py's lamp rule
  matched the word "light" inside COLOUR names:
  `embellishcraft:light_gray_corrugated_metal_plate` (5429 uses, 1350 of
  them as floor), light_gray / light_blue sheet metal, tiles, seats and
  cushions all became deci:BlockLightOff. `minecraft:daylight_detector`
  (935, mostly floor trim) and `lightning_rod` matched too. Full
  simplylight illuminant blocks (about 5000) also became the small lamp
  prop, sitting in walls and ceilings as a floating fixture.
- **Fix** (lctranslate.py, pack rebuilt): `unlit()` removes light_gray /
  light_grey / light_blue / lightning / daylight before the lamp rules;
  full illuminant blocks -> minecraft:redstone_lamp (a solid lamp block);
  daylight_detector, redstone_lamp kept as themselves; rods -> iron_bars.
- **Verified**: worlddiff of seed 1 old pack vs new pack (`--top`):
  2115 BlockLightOff -> BlockMetal_2, 542 -> redstone_lamp, 111 ->
  daylight_detector. 30 interior photos (shots_index "lc_quality") show
  no lamp floors; ceilings show redstone lamps where the source had them.
  Not yet seen by the user in game `[not verified]`.

## NPC tracers fly sideways or backwards (old bug, also in the original; FIXED 9 Oktober 2026)
- **Reported**: 9 Oktober 2026: an NPC shooting forward draws its tracer to
  the left, right or behind.
- **Cause (code)**: BanditEntity.shootAt (`deci.ag.a.e`, also soldiers and
  hazmat soldiers) and ArmedTraderEntity.shootAt (`deci.ai.v.e`) damage the
  target directly on the server and send PacketGunFireEffects
  (`deci.aE.a$B`) with only the shooter's id; the client handler
  (`deci.aE.a$B$a`) draws the tracer along the shooter's getLook(), which
  for a mob is its BODY facing, not its aim.
- **Fix** (`tools/patches/PatchTracer.java`, in Decimation.jar.patched,
  dist/Decimation.jar, dev/libs/Decimation-base.jar): the packet also carries
  the target's id (read only when present; -1 = none), shootAt records its
  target, and the handler aims the tracer from the shooter's eyes at the
  target's chest (bounding box bottom + 0.6 x height). Players unchanged.
- **Measured** (`./gradlew runClient -Pautotest -Ptracer`: a bandit 8
  blocks from the player, held facing away, shoots the player): original
  classes 52 tracers, mean 133 degrees off the line to the target, max 179;
  patched 42 tracers, mean 1.0, max 2.0. (The test also showed the original
  shootAt crashes on a miss when the bandit has no AI target: it plays the
  miss sound at getAITarget(); never happens in play, the AI sets it.)

## NPC tracers fly through buildings (reported 14 Oktober 2026, FIXED v0.62.4)
- **Reported**: 14 Oktober 2026 (user): "the tracer ammo pass through any building ... normally when it hit block,
  it will disappear since its the end of the tracer ... even apartment can be pass it".
- **Cause (code)**: a tracer (BulletTracerRenderer$BulletTracer, deci.n.d$a) flies from start to end and is removed
  when its head reaches its length (tick c(): ticks x 10 >= length - 10); nothing checks blocks. A player's own
  shot sends PacketBulletTracer with the ray trace's hit as the end, so it stops at the wall. An NPC shot sends
  PacketGunFireEffects; its handler (deci.aE.a$B$a) ends the tracer 250 blocks along the aim (shotguns 50) with a
  random +-1 (+-3) end. Our v2 aim point (NpcShots, v0.30.3) only set the direction ("a point far along the
  line"), so the server stopped the shot at the wall while the client drew it 250 blocks on. Since v2 every NPC
  tracer is visible (Decimation hid 60 %), which made it obvious.
- **Fix** (`tools/patches/PatchTracer.java` v3, three jars: deci/aE/a$B$a and deci/n/d$a; the packet and shooter
  classes are unchanged from v2, checked byte for byte): NpcShots sends the point where the shot ENDED (the body it
  hit, the block's hit point, else its range) and the handler ends a traced shot's tracer there (Vec3.addVector
  answered with the aim point, no end jitter). The renderer clamps the drawn 10 block segment to the tracer's
  length, so a shot into a near wall does not poke through it during its first frames (player tracers too).
- **Checked**: patch built from the original classes, classes load under -Xverify:all, bytecode shows both
  segment points clamped and the handler's end replaced; mod compiles; backup hash unchanged, entry counts and zip
  tests clean. Dev test `tracer` got a check "tracers end where the shot ended" (hits on the player 8 away end
  within 12 blocks, none longer than the shot range). Run 14 Oktober 2026 (`--live tracer shots`, fresh live
  game on the patched base jar): 62 of 75 tracers end within 12 blocks, longest 96 (was 250 each), tracer vs
  server shot line 0.8 deg, hits through a wall 0 / 30. Tracers into walls stop at the wall: user confirmed
  in game 15 Oktober 2026 ("its blocked now"). In v0.62.4 a miss's tracer ended at the shot's range
  (NpcShots.RANGE 96, SNIPER_RANGE 160) and so showed for about 0.45 s instead of 1.2 s (the head moves 10
  blocks a tick); a side effect, not a performance choice. The shots mode's "spotting at 70" FAIL is older: the
  test still expects sniperRange 96 (70 since v0.42.5) with its sniper exactly 70 away.
- **v0.62.5 (user 15 Oktober 2026: "put misses back to 250")**: NpcShots.missEnd carries a miss's tracer on to
  TRACER_MISS 250 (drawing only, damage still ends at the range) and traces blocks past the range, so a long miss
  still stops at a building; only loaded chunks are traced (blockExists in 16 block steps), so a shot never loads
  terrain. Dev test `tracer` (live, 15 Oktober 2026): 70 of 84 within 12 blocks, longest 224 (beyond the loaded
  chunks it stops early), tracer vs shot line 0.5 deg; check now wants the longest over 161 and at most 251. A
  miss stopping at a wall beyond 96 is not measured by a test `[not verified]`. shots mode: "AKM magazine then
  reload" read 17 rounds once and 30 on the rerun (flaky: the count can start mid magazine).

## Full military armor makes NPC gunfire almost harmless (reported 9 Oktober 2026, FIXED v0.30.2)
User: a full set of normal military armor takes about 1 hp per NPC shot,
even with a group of NPCs shooting at once.
- **NPC shots are not bullets** (BanditEntity.shootAt, read in deobf
  source): when the NPC can see its target, 75% of shots call
  `attackEntityFrom("human", gun damage / 8)` on the target directly, 25%
  only play a sound. The tracer (PacketGunFireEffects) is a visual only.
  Player shots are client ray traces sent as PacketGunHit; the server
  applies body armor and hits with "gunDeci".
- **Cause 1, armor multiplies piece by piece**: ArmorGunfireHandler
  (v0.9.0) uses Decimation's PvP formula, damage x every worn body piece's
  multiplier, and the multipliers are the originals x 0.65 (our armor
  buff). Marine set: vest 0.85, pants 0.8, boots 0.8 -> 0.5525 x 0.52 x
  0.52 = x 0.149 (checks mode: chest alone 5.52 of 10). An M4A4 NPC hit
  (16 / 8 = 2) does 0.30 hp; the helmet (0.195) on the 20% headshots
  makes that 0.06.
- **Cause 2, vanilla hit cooldown**: EntityLivingBase.attackEntityFrom
  (lines 854..870 of the MCP source) ignores a hit while hurtResistantTime
  is above half of maxHurtResistantTime (20 ticks for players) unless it
  is bigger than the last one. NPC shots never reset it (Decimation's
  player gun handler sets it to half after every hit, so player shots all
  count). So for 0.5 s after a hit every other NPC hit of the same size is
  dropped: ten NPCs do about as much as one, at most ~2 hits per second.
- **Fix (v0.30.2, user decision)**: player shots unchanged; NPC gun hits
  ("human") on a player x `npcDamageToPlayer` (default 5, user: "hardcore
  singleplayer"), after armor; and NPC hits clear the victim's hit
  cooldown first (LivingAttackEvent), so every hit of a group lands
  (`everyNpcHitCounts`). Both in config/deciworldgen_npc.cfg, category
  "player"; code NpcLoadouts.onAttack / onHurt.
- **Numbers (checks mode)**: an NPC M4A4 hit (2) on a bare player 10.00 hp
  (half the bar), full marine body set 1.49 (was 0.30), 5 hits in one
  tick 7.47 (all land; before only the first). Helmet headshots (20%)
  cut a hit further. Armed traders also use "human" at full gun damage
  but only ever target infected and bandits, never players.
- Not seen in game by the user yet `[not verified]`.
- **v0.30.3 (user: "too hard")**: the hit cooldown is back (config
  `npcHitsSkipCooldown`, default false; the v0.30.2 key
  `everyNpcHitCounts` is removed from the file on load); x5 stays. Full
  marine body set: 5 hits in one tick now 1.49 (the first lands).

## Soviets kill each other (reported 9 Oktober 2026, FIXED v0.30.3)
- **Cause** (bytecode of deci.ag.m.c, original Decimation):
  SovietEntity.isHostileTo returns, for another armed human, faction ==
  SOVIET: Soviets were hostile ONLY to Soviets (and ignored bandits and
  soldiers). Shown once MilitarySpawner put them in groups. (The deobf
  naming note "hostile to everything except Soviets" described the
  intent, not the code.)
- **Fix** `tools/patches/PatchFactions.java` (built on PatchTracer's
  deci/ag/a): Soviets hostile to every other armed human except Soviets
  and traders; bandits and soldiers (and hazmat soldiers) hostile to
  Soviets. Our traced NPC shots (NpcShots) pass through allies of the
  shooter's side, so no friendly fire. Checked: npc mode "factions"
  [soviet>soviet false, soviet>bandit / soldier, bandit / soldier>soviet
  true].

## NPC shots were not bullets (user request 9 Oktober 2026, DONE v0.30.3)
User: a player should only take damage when the tracer hits; plus NPC
bullet impacts on blocks (until now only player shots made them).
- **Before**: shootAt hit its target on 75% of shots through anything once
  it could see it; the tracer was a picture only, and Decimation's client
  makes 60% of tracers invisible (INVIS colour) unless the gun has a
  tracer attachment.
- **Now** (`tools/patches/PatchTracer.java` v2 + `fixes/NpcShots`):
  BanditEntity.shootAt asks our hook (`shotHook`), which traces a shot
  from the shooter's eyes toward the target's chest with a gaussian
  spread per tier (degrees: bandit light 1.6, medium 1.3, heavy 1.1,
  soldier 1.0, military 0.9; machine guns x1.4, sniper rifles 0.25;
  config `spread`), stops at the first solid block (glass, plants, vines,
  iron bars, ladders pass, as for player guns) or the first living thing
  that is not an ally, and only that takes Decimation's damage (gun / 8,
  full on infected). A block hit sends Decimation's own impact particles
  ("explode" + "blockcrack", as the player block hit handler does). The
  packet carries a point far along the shot line, the client draws the
  tracer along it and always visible. (Since v0.62.4 the point is where the
  shot ended and the tracer stops there: "NPC tracers fly through buildings".)
- **Checked** (dev test modes, fresh world, all PASS): shots: AKM medium
  bandit at a pig, 97% hits at 10 blocks, 57..66% at 20, 0 / 143 through
  a stone wall, impact particles on the wall (shots_wall); tracer: 163
  shots, 163 visible tracers, 0.2 deg mean from the server's shot line,
  2.1 deg mean from the target (the spread). Not seen in game by the user
  yet `[not verified]`.
- Test traps found: an NPC held in mid air falls on the client (tracers
  then start lower): test NPCs stand on a block now; the tracer test only
  counts its own bandit's shots (nearby NPCs fight each other since the
  faction fix).
- **v0.30.4 (user: shorter, 0.25 s)**: `npcHitCooldownTicks` (default 5;
  10 = vanilla, 0 = every hit lands) replaces `npcHitsSkipCooldown`. An
  NPC hit 4 ticks after the last is dropped, 5 ticks after lands
  (checks mode); a group lands at most about 4 hits a second.

## Zone rules dead in singleplayer: safezone PvP / water / traps, radiation, scary, client zone lists (found 11 Oktober 2026, FIXED v0.46.0)

Found while studying the zone system for the safezone feature (docs/zones.md). Decimation's zone
handlers that are not about plain damage live in the server proxy with @SideOnly(SERVER) methods
(PlayerZoneTickHandler deci.aK.m, SafezoneAttackHandler deci.aK.g, FriendlyFireHandler
deci.aK.i, most of EntitySpawnZoneHandler deci.aK.d), so in singleplayer: radiation zones never
irradiated anyone, safezones never refilled water, traps / rockets / grenades were not kept away,
the grace timer protected nobody, conditions could start inside a safezone (PlayerData setters
check the zone only on a dedicated server), and the client never got any zone list (only
ZoneManager.refreshZones sends one, with the json file zones only), so no "Now entering a
Safezone" line and no map markers, on a server too for our generated zones.
Fix: fixes/ZoneRules (copies of the server handlers, stepping aside on a dedicated server, plus
the client zone list sync on both) and ZoneSpawnHandler (traps, projectiles; ours: no bandits,
Soviets, mechs spawning inside a safezone). Checked: dev test `zones`, 19 checks PASS.

## Survivor camp NPCs missing (reported 11 Oktober 2026 on 0.47.0, FIXED v0.47.1)

User shot docs/shots/safezone_camp_v0.47.0_review/user_119.png: the market of the camp with no
trader, guard or civilian. The user suspected Decimation's trader "reset" countdown. Read from
the code: that reset is real but server only (ServerTickHandler traderRestockCountdown 3000
ticks -> TraderSpawnManager.respawnTraders removes every TraderEntity and spawns those of
decimation_traders.json), stripped in singleplayer. The camp shown was the dev test's sky camp
in deciworldgen_devtest (the user's game had that world open, log "survivor camps off"), whose
residents and site the camp test removed at its end. Fixes: the test keeps its camp populated;
on a dedicated server the keeper respawns traders and guards at once after a reset; awning
front edges coloured; `/deciworldgen camp` to place a populated camp anywhere. Checked: camp
test all PASS, all 20 residents saved in the world after the test, campcmd 20 residents. Peaceful
difficulty removes Decimation humans (EntityMob) `[inferred]`, documented in docs/safezone.md.

## Camp guard "flying" over the wall (reported 11 Oktober 2026 on 0.47.1, FIXED v0.47.2)

User shots docs/shots/safezone_camp_v0.47.1_review/user_120.png and user_121.png: a guard
standing in the air over the scrap wall next to a tower. Guards (Decimation's GuardTrader) are
static: TraderEntity resets its position every tick, so one spawned before the blocks under it
exist hangs in the air. The keeper only checked that the chunks existed, while the camp is
written during chunk population (and a camp built over an older one keeps the older residents
in place) `[inferred: not reproduced; the 30 s test after the fix had nobody off post]`. Fix:
the keeper waits until every chunk under the site and one around is populated, and puts any
trader / guard off its post, or any civilian / survivor on a wall, roof or outside the walls,
back to its post every 2 s (logged). Same review: provocation removed, entry sign one block out,
more cosmetic props.

## Canvas metadata 0 never turned with a rotated compound (found 11 Oktober 2026, FIXED v0.48.0)

Canvas.set(x, y, z, block, meta) marked a cell VANILLA (turned by Rotation) only when meta was not
0, so blocks whose metadata 0 has a direction stayed unturned in a plan rotated by 90 / 270:
stairs ascending east, fence gates spanning x (the survivor camp gate and garden gate in camps at
turns 1 / 3 spanned across their wall line) `[inferred from the code; not looked at in game]`.
Found building the city refuge (its stair runs ascend east). Fix: every set with metadata is
VANILLA (Rotation leaves blocks without a table unchanged). Checked: refuge turns 1 dump (stairs
meta 2, gate meta 1), camp turns 1 and bases turns 1 propclash / walkcheck 0.

## Dev tools: walkcheck stopped at y 4, propclash counted shelf tops as floating (found 11 Oktober 2026, FIXED v0.48.0)

tools/props/walkcheck.py allowed a climb of one only below y 4 (towers and decks by ladder), so
the refuge's upper floors read as unreachable; stairs now climb at any height. propclash.py took
the first multiblock part per 4 block bucket as the master, so a shelf standing at y 11 had its
top part (y 12, next bucket) reported "nothing under it"; an upper part (same block below) is now
skipped. Also: `devtest.py MODE key=value` in a one launch run does not pass the option (only
-P flags the Gradle script knows); use `--live MODE key=value`.

## District name floating over the city showed "_" for spaces (user report 12 Oktober 2026, FIXED v0.52.1)

User: "underline _ show up on text heading city (not screen text) but actually on the zone it is".
Besides the banner and the chat line, Decimation's ClanTurfLayer (deci.M.l, a world render layer)
draws the name of the turf the player stands in as floating text over the turf's middle (x / z
middle, y 127 + 3 for our 0..255 boxes). Our names travel with "_" (the client strips spaces), and
that layer printed them raw. Fix: tools/patches/PatchTurfNames.java argument `layer` turns "_"
into spaces in its 7 text calls (three jars). Check: dev test `places` last shot looks at the
floating name over the second district.

## Joining a world with a mod you do not have: "Connecting" for two minutes (18 Oktober 2026, OPEN)

Seen in the promo run: the dev host had Gliby's Voice Chat (gvc 0.6.1), the production joiner did not. JoinService
should refuse at once with the toast "You need gvc 0.6.1 to join ..." (it closes the local socket), but the joiner sat on
Minecraft's "Connecting" screen for 120 s (first try) and ended on the disconnect screen after 120 s (second try); the
toast was not seen (screens only) [not verified]. Release players have no voice chat jar while Technic players do, so
this will happen. To check: whether the toast shows, why the closed socket is not noticed at once, and whether
voice chat should be left out of the mod check.
Seen again 18 Oktober 2026 13:38 in a real game: the user hosted from the dev client (runClient always installs gvc into
run/client/mods, dependencies.gradle installVoiceChat), the brother joined with only Decimation.jar and saw the need
gvc 0.6.1 message, so our refusal does reach the player. Workaround that day: the original voice chat jar served beside
Decimation.jar.
FIXED for voice chat (18 Oktober 2026, commit 179e211): Forge's default checker (NetworkModHolder) lets a client in
without a mod only when the remote is the server (side SERVER), so a host WITH gvc refused every joiner without it;
the reverse already passed. fixes/hosting/OptionalMods wraps gvc's checker in postInit (a remote without gvc passes,
one with gvc still needs the same version), ModSet.OPTIONAL skips gvc in our own check. Tests OptionalModsTest (Forge
refuses, relaxed lets in, version still checked; RED then GREEN), ModSetTest; suite 56 PASS. Real app: dev client host
WITH gvc ("gvc is optional for players who join" in its log), production client joiner with only the one jar (built
from 179e211): lanhost real=true scene=true all PASS, lanjoin viaList=true joined through the deployed relay in 4 s,
all PASS. Still open: the two minute "Connecting" wait for any OTHER missing mod (the toast shows, the screen stays).

## A joined player could not open a corpse (user report 18 Oktober 2026, FIXED, released in 0.65.0)

User: "the corpse cannot be clicked by joined player". Decimation's player corpse (EntityCorpse, deci.ae.a) opens a
chest screen on a right click (interact: displayGUIChest), nothing server only. Two game test (lanhost scene=true +
lanjoin, new scenes corpse, corpsedeath, corpsefind): a corpse opened fine for a joiner with empty hands, also the
joiner's own corpse after a real death. With an AKM in hand the right click did nothing: no screen, and the host's
server saw no click (EntityInteractEvent counter 0; the host making the same click packet itself opened the corpse).
Cause: Decimation's ClientEventHandler.onClientTick unbinds the use key while a gun is held (key code 0, right click
only aims) and binds it again only over an item frame. It hits every player with a gun, host included; joined players
noticed it because they come back armed. Fix: fixes/GunUseKey (client tick START, after Decimation's) binds the use key
again while the crosshair is on a corpse or our loot body. RED: "right click opens the corpse after a real death"
holding AKM FAIL (no window); GREEN: window 1 after 5 ticks, the 8 items shown, ContainerChest on the host; all lanjoin
and lanhost checks PASS. Test trap found on the way: the joiner's own game opens an EMPTY chest at once (window 0); the
real one (the host's window, with the items) comes after the click crosses the relay, so a check must wait for a window
above 0. Traders and vehicle seats with a gun in hand behave the same in Decimation [not checked]; left as Decimation made
them.

## Natural spawns off for tests (user request 18 Oktober 2026, DONE, released in 0.65.0)

User: "add some code to remove natural spawn on mobs, so to spawn it only from code or egg side so if in middle test
there is npc or zombie attacking the user, it doesnt happen anymore". Minecraft's `/gamerule doMobSpawning false`
already stops the biome spawn lists, which bring all of Decimation's own NPCs and infected. Our automatic spawners did
not follow it: fixes/NaturalSpawns (the rule) now gates fixes/MilitarySpawner (allowed), garrison posts (Garrisons
waits until the rule is on again) and garrison spawner cages (their infected are refused). Code, eggs and /summon still
spawn. Dev test `nospawn` RED 2 FAIL, GREEN 4 PASS (three runs). The two game test (lanhost scene=true) turns the rule
off for its scenes and back at the end. Safezone residents are friendly and still fill their camps.

## Dev test zombies: "night frenzy off by day" FAILs (18 Oktober 2026, OPEN)

x1.20 at world time 6000 instead of x1.00, three runs on the prerelease branch and one on main (so not from the fixes
carried there). The test clears rain and thunder at the night step; World.isDaytime() reads the sky darkness, which
rain strength keeps low for about 100 ticks after the rain stops, and Decimation's own storm weather may set rain again
[inferred, not checked]. Look at rain strength and Decimation's weather at the check.

## JoinServiceTest "a join is carried to the room" timed out once (18 Oktober 2026, OPEN, flaky)

SocketTimeoutException at JoinServiceTest.java:79 (3 s read of the echo) in one full `./gradlew build`; passed alone 3
of 3 right after and in the next full build (56 PASS). A build loaded machine [inferred]; raise the timeout if it comes
back.

## Two game test: "bandit fire hurts the joiner" sometimes 20 -> 20 (18 Oktober 2026, OPEN, flaky)

Failed twice today (CorpseCheck9, RelayCheck3), passed in the other runs of the same code. The bandit scene spawns its
bandit from code next to the joiner; why it sometimes does not shoot is not looked at [not checked].

## A joined player was kicked again and again while the ping looked stable (user report 18 Oktober 2026, FIXED)

The host's control socket to the relay had no keep-alive and was cut when idle (measured 391 s, code 1006); the relay then
closed every joiner and the host registered a new room. Fixed with keep-alive pings, a 20 s grace in the relay room,
reconnecting to the same room, and direct players kept (docs/player_hosting.md 16). Checked with a forced control drop in
the two game test on the deployed relay. A real long session with the friend is still to come.


## OpenGL "Stack overflow" 1964 times on the host, from Market District (user test 19 Oktober 2026, FIXED, released in 0.65.0)

Brother's laptop (wyvern: RTX 2060, NVIDIA 610.57, Linux, OptiFine E7) hosting, the user joining from the Mac. Its
latest.log: "GL ERROR @ Post render 1283: Stack overflow" 1959 times and "1284: Stack underflow" 5 times, from 02:38:53
to 02:51, up to 398 a minute; OptiFine also put "OpenGL Error" in chat. The Mac log has none (Apple's GL on Metal may not
report it) [not checked].

Cause (found 19 Oktober 2026, dev test `glstack` plus a bytecode path check of every Decimation method):
1. Decimation's ModelItemRenderer (deci.K.c) and BModelItemRenderer (deci.K.d) push a matrix and return without its pop
   when called in first person. Holding the item does not call them that way (Decimation draws it itself), but after a
   switch AWAY from one Minecraft keeps drawing the old item in first person for a few frames: 2 to 5 matrix levels lost
   per switch. 24 items use them: every melee weapon, every grenade, the molotov, riot shield, chainsaw, guitar, range
   finder, suicide vest, party horn. A few switches fill the 32 level stack; from then on every push fails and models
   are drawn with the wrong matrix. Measured: machete then a gun, +2 to +6 levels; a renderer probe counted the leaking
   calls (ItemMachete / ItemPartyHorn EQUIPPED_FIRST_PERSON, one level each).
2. Decimation's name tag handler (ClientRenderHandler.onRenderNameTag, deci.c.b) returns without its pop for another
   PLAYER while "enableFancyNameplate" is off (decimation_client.properties; on by default, so not this session): one
   level per player in view each frame, multiplayer only. Measured +12 in about 7 frames.
3. Checked clean: every other Decimation method on normal and exception paths (tools: scratchpad glinter.py, javap based;
   one latent case left: BModelPart.render returns without its pop for a hidden part, and nothing hides BModelParts
   today [checked by search]).

Fix (fixes/GlStackGuard, client): the 24 renderers are wrapped and pop after a first person call; a name tag handler
after Decimation's pops on that one path; and a guard for anything else: each frame ends with the modelview, attribute and
projection stacks as deep as it began (anything left is set back, logged at most once a minute as "[deciworldgen] GL
stack guard: ..."). Plain GL 1.1, the same on every OS. Dev test `glstack`: 131 items in first and third person, 48 gun +
sight cases (hip, aimed), the other player case and a frame that leaks 3 on purpose: FAIL before (4 leaking), PASS after.
Not yet seen on the NVIDIA laptop with the new jar [not verified].

## FPS falls minute by minute (120 to 60 and lower), the Mac froze after about 20 minutes (user 19 Oktober 2026, FIXED, released in 0.65.0: causes found)

Same session (brother's laptop hosting, the user's Mac joining, Prism, release jar 5077445e on both). Checked first: no
entity pile up (the host world saved at quit: 191 entities, 7119 tile entities of which 5404 PropTile; tools/perfcheck.py
world on a copy), the Mac log ends at 02:52:45 with nothing unusual and the process was ended at 02:54:38 (Prism: "Game
crashed", no hs_err file, no crash report, no macOS hang report: most likely force quit while frozen [inferred]); both
games had 4 GB.

Measured in the dev game (19 Oktober 2026; docs/performance.md section 7):
1. **A thread that never sleeps** (the big one for laptops): Decimation's boot intro (GuiIntroJumpscare, deci.i.d) starts
   a thread in its static initializer that counts intro frames at 30 Hz by busy waiting and never ends. Our jars skip the
   intro, so it does nothing, at 99% of a CPU core for the whole session (dev test `threads`; JFR: 100% of Thread-13 in
   deci.i.d.aS). Laptops heat up and throttle after minutes: that fits "fresh 120, minute by minute 60 and worse" on both
   machines [throttling itself not measured]. PatchSwing (v0.7) fixed a twin of it: "idle main menu 200% CPU -> 105%";
   the 105% left was this thread. Fix: tools/patches/PatchIntroThread.java (three jars): it sleeps between frames and
   ends when the intro is finished (at once in our jars). `threads`: FAIL (99%) before, PASS after.
2. **Prop sound loops that never stop**: every radio, vending machine, electric box, generator, hazard light and military
   radio starts a looping sound when the player comes within 16 blocks (BlockProp.randomDisplayTick) and Decimation stops
   it only when the prop's chunk unloads, so a walk through a city leaves dozens running; the radio's is streamed
   (decoded in Java on paulscode's stream thread, which holds the sound lock the game needs 4 times per loop and tick).
   The brother's small world has 56 radios. Dev test `radios` (30 radios): 26 loops played on 80 blocks away, frame rate
   74 without radios, 57 with them, 45 away from them (still playing). Also a bug: a radio stayed silent for good after
   its chunk reloaded (its map entry was never removed). Fix: fixes/PropSounds (client, once a second): loops beyond 20
   blocks stopped and forgotten (they start again when near), at most 6 play (the nearest), never two of one prop.
   `radios`: FAIL before, PASS after (6 near, 0 away, playing again on return); unit tests PropSoundsTest (5).
3. **The GL stack overflow** (entry above, fixed by fixes/GlStackGuard): broken matrices after the stack fills.
4. **A dead update check every minute**: UpdateChecker.tickVersionCheck (gH) fetched BoehMod's dead latestversion.txt
   (404 and a 20 line stack trace) every 60 s on clients and servers; its answer is unused since PatchNoUpdate v2. Fix:
   tools/patches/PatchNoVersionFetch.java (three jars): it does nothing.
5. Checked, not a leak: a 10 minute soak (dev test `soak`, flying into new land at 6 blocks a second): heap after
   collection 230 -> 256 MB, threads 27 to 29, entities and chunks steady; heap class histogram start vs end grew 16 MB,
   all vanilla structure data (mineshaft maps) and items. The frame rate there falls by place (generating and meshing
   new city chunks: JFR client thread 70% vanilla chunk mesh building, server thread world generation), not by time.

Still to see: the same session again with the new jar on both machines (FPS after 20 minutes, no GL errors in the host
log, no freeze) [not verified]. The user after their next session (20 Oktober 2026): "fps felt stable"; what they still
felt was a short lag while new land generated ("Server stalls while new city areas generate", released in 0.65.0); the
host log and the second machine were not reported [not verified].

## Direct join never used on the brother's NixOS laptop: its firewall blocks the gate port (19 Oktober 2026, OPEN)

Joiner went through the relay (Singapore) although both were on the same wifi. wyvern's NixOS firewall is active and lets
in only listed ports; Direct.Gate listens on a random free port (ServerSocket(0)), so no rule can name it. Windows asks
"allow Java" once instead [inferred]. Options: a fixed default gate port the host can open, shown on the host page.
Also seen: two "registration failed: 429 too often" at 02:34:15 after a quick restart of hosting; the next try worked.

## The last grenade never runs out (user 19 Oktober 2026, FIXED, released in 0.65.0)

"if its more than one its will defrade 1 by 1 normally but when it hit quantity 1 it will be act like unlimited
throwable". Decimation's throw (GrenadeItem.throwGrenade, deci.aA.a, and its five copies for the molotov, smoke, signal,
stun and tear gas grenades) takes one off the held stack on the server and never removes a stack of 0. A stack of 0 looks
like one (counts under 2 are not drawn) and its next throw only checks the item type: another grenade, and so on. Fix:
fixes/EmptyStacks removes stacks of 0 or less from every player's inventory after each server tick (vanilla does the
same after placing the last block). Not on the player tick: a server player's tick runs only when that client's movement
packet arrives (NetHandlerPlayServer.processPlayer -> onUpdateEntity), and the first version failed the test after a
teleport that way. Dev test `throwables`: FAIL before ("0 held", a second throw spawned a grenade), PASS after (alone,
after `glstack`, and twice more), also for the molotov and the smoke grenade. Left open: two throw packets inside one 50 ms tick could
still both throw from the last one (needs a patch of the six throw methods in the three jars) [not done].

## Unit test ColosTest "with overlap the next socket opens before the far one closes" failed once (19 Oktober 2026, OPEN, flaky)

Saw "close 2" from the previous test at the start of its event list (expected [open 0, open 1, close 0]); passed in the
next two full runs (87 of 87). The shared EchoServer's hop events are not cleared between tests, and a socket closed by
the previous test reports late [inferred]. Clear the events and wait for the previous test's closes before the overlap
test if it comes back.


## Dev test playerfire: "the shot hits the infected" fails every other run (19 Oktober 2026, OPEN, flaky, older than today)

In one live session the runs go FAIL, PASS, FAIL, PASS; every failing run's target starts at 12 health (plain Infected,
deci.ag.d, same spot, height 1.95, did not move), the passing ones at 20 or 30. Same on the code before today (46db3dc,
4 runs), so not from the GL / sound / stack fixes. Why a 12 health infected takes no hit is not found (Decimation's
spawn handling of some infected [not checked]); the test now writes a "target" line (class, name, place, height) with
each run.

## Safezones lag: traders, guards, bandits and mechs searched for targets every tick (user's friend 19 Oktober 2026, FIXED, released in 0.65.0)

"when in safezone its really laggy, i think its caused by traders or npc". Measured (dev tests traderperf, zoneperf,
bench; docs/performance.md section 8): every armed trader and trader guard (ArmedTraderEntity deci.ai.v), every
bandit and soldier (BanditEntity deci.ag.a, also our survivor / military tiers, garrisons, base guards) and the mech
(MechEntity deci.ag.k) called findTarget EVERY tick while it had no target, on the server and on every client's copy;
findTarget ray traced (canEntityBeSeen) to every living entity within 32 blocks before asking whether it is hostile,
and the mech's walked the whole world's loaded entity list. On top, Decimation's mobs carried vanilla "nearest target"
tasks with targetChance 0 (the infected four: players, animals, human NPCs, mechs), searching at every evaluation.
- 20 trader guards around the player: server +0.87 ms a tick, client +4.25 ms a frame (82 -> 61 fps) before; -0.18 ms
  and +0.47 ms after. 20 bandits: +0.58 ms / +5.27 ms before, +0.31 ms / +0.52 ms after. Both still shoot an infected
  that comes near (6 and 16 ticks). One mech took 10 to 13 % of the server with 40 NPCs standing by.
- Fix: tools/patches/PatchTraderTarget.java (server only), PatchBanditTarget.java and PatchMechTarget.java (client copy
  keeps searching, throttled: bandits switch stance only while they have a target, and stance is not synced): every 10
  ticks staggered by entity id, the cheap checks first, the ray trace last; three jars. fixes/TargetPacing: the
  "nearest target" tasks of Decimation's mobs get targetChance 5 (a search every 15 ticks on average, 0.75 s; 10 made
  infected take 1.5 s to notice someone). Dev test targetpace: [0, 0, 0, 0] / [0] before, [5, 5, 5, 5] / [5] after, an
  infected still takes a civilian 6 blocks away as its target (2 to 11 ticks).
Not yet seen in the friend's game [not verified].

## Infected searched a wander path no one walked (19 Oktober 2026, FIXED, released in 0.65.0)

InfectedEntity.onLivingUpdate (deci.ag.d) called EntityCreature.updateWanderPath, a full path search (PatchInfectedAI v1
cut it to once a second per infected); its result fills pathToEntity, which only the old AI branch follows, and infected
run the new AI (isAIEnabled true: EntityAIWander makes them wander, getNavigator() leads the hordes). bench "zombies"
(80 infected): 37 % of the server thread in that search. PatchInfectedAI v2 removes the call (built from the original
class, as v1); bench zombies 37 % -> 0 %, shooting 23 % -> 0 %. Dev tests zombies and noise pass.

## Blood splats on the ground moved for 40 seconds (19 Oktober 2026, FIXED, released in 0.65.0)

GroundBloodParticle (deci.B.k) lives 800 ticks with fancy graphics and called moveEntity every tick, also lying on the
ground: block collision plus a scan of the entities around (splats x entities in a fight; bench "crowd": 14 % of the
client thread). tools/patches/PatchBloodParticle.java: it moves only until it lands (onGround). 14 % -> 4 % (what is left
is splats still falling). Each splat still draws on its own (its own texture bind and draw, a new ResourceLocation each
frame) [not changed].

## The hand holding a gun showed Steve (user 19 Oktober 2026, FIXED, released in 0.65.0)

"when holding a guns, the hand still using steve skins instead of custom skins player (even its real minecraft account
player)". Decimation's gun renderer draws the first person arms with GunItemRenderer.TJ (deci.K.b), "skins/<session
name>", which only vanilla's old skins.minecraft.net download filled (shut down; Steve as its fallback); a real skin
comes from Minecraft's skin manager under "skins/<hash>". fixes/GunArmSkin sets TJ to the player's getLocationSkin()
each client tick. Dev test armskin: arms skins/Developer before, the player's skin location after. Seen with a real
account [not verified]: the dev account has no skin.

## Scope: the glass was measured at the hip for nothing; no stutter found on the M4 (user 19 Oktober 2026, partly FIXED)

"our new scope methods realy buggy and often drop fps". Measured (dev tests scopeperf, scopediag): with a 4x / 2x / EOTech
sight held at the hip PatchScope's GL feedback measurement ran every 8th frame, although the glass copy runs only while
aiming (v0.58). ScopeZoom now measures only while aiming: 0 at the hip (30 before). On the M4 the frame times while
aiming through the scope matched aiming over iron sights (slowest 1 % 27 to 35 ms both), and spikes did not fall on
measuring frames. On NVIDIA / Intel laptops the feedback pass forces the CPU to wait for the GPU, a possible stutter every
8th frame while aiming [inferred, not measured there]; replacing it with CPU math from the matrices is the next step if
the drops stay. What "buggy" looks like for the user is not known yet. Trap found: live key=value settings stay set for
later modes (an aimdrift run used a red dot from an earlier scopeperf run).

## GunNoise wrote a noise to every mob for every burst in a fight (19 Oktober 2026, FIXED, released in 0.65.0)

Each burst (once per 10 ticks per shooter) searched a 128 x 64 x 128 box and wrote a new noise compound to every infected
or hostile NPC in range: 4.5 to 6 % of the server in the bench fights. A mob still walking to a shot within 8 blocks of
the new one, with more than half of its search time left, is now skipped. Dev test noise passes.

## Drawing many human NPCs is heavy on the client (19 Oktober 2026, looks and model parts FIXED, released in 0.65.0)

bench "standby" (40 idle NPCs near the player): client about 25 to 30 ms a frame on the M4. Each human NPC is drawn in
layers (ModelHuman deci.q.h: body, eyes, hair, beard, shirt, pants, shoes, each a full body pass with its own texture
and a new ResourceLocation), then up to four armour models, mask, vest, backpack and the gun part by part (one display
list call per BModelPart: 25 to 30 % of the client's native time).

Step 1, the looks (user: "from 7 to 1"): fixes/HumanLayers and tools/patches/PatchHumanLayers.java (three jars). Each
look (skin folder, tone, hair, beard, eyes, shirt, pants, shoes; blinking, infected, frail) is one 64 x 32 picture made
from the same files in the same order, each layer only on the boxes its pass draws (the files hold pixels elsewhere too:
hair 663, shirt 105, beard 72), a layer pixel where its alpha is above 0.1 (all 91 files are 0 or 255); at most 256
pictures on the card and 4 new ones a frame, anything odd drawn the old way. renderHuman binds the picture once and skips
the six layer binds, their passes and the posing of its two layer copies. Its three line of sight checks a frame (the
model and both copies, up to nine ray traces each) fed only HumanEntity.visibleTicks, which nothing reads
(isRecentlyVisible has no caller in the jar): they answer true now. Hidden NPCs are still not drawn (ClientEventHandler
cancels RenderLivingEvent.Pre with the same check).
Checked: dev test npclook (each kind alone, drawn both ways in one frame, pixels compared without gear: at most 0.6 %
differ, texel edges; dev and production client), npcdraw (time drawing NPCs a frame, both ways in turns of 30 frames):
80 infected 6.55 to 4.72 ms (28 % less, 82 to 59 us each); 70 armed civilians and guards 31.2 to 29.6 ms (5 % less, 423
us each). The bench's frame times could not show it (Mac load 4 to 14, the 60 fps cap).
Visible changes, both from bugs of the old drawing: the layer copies sat 0.05 / 16 of a block out, so hair and beard
showed through a cap or a mask (a mustache line on a balaclava, hair along a cap's edge); and the copies were posed
without the held item (RenderBiped sets heldItemRight on the main model only), so an NPC holding a frying pan had a bare
arm with its sleeve hanging beside it. Now gear covers hair and beard, and sleeves stay on the arm.
Step 2, guns and gear (user: "the gun model and gun fighting when npc shoots is the most hurt of the fps"): every .bmodel
part (guns, attachments, armour, masks, vests, backpacks: BModelPart deci.n.b) compiled two display lists in which every
quad was a draw call of its own (BModelQuad: startDrawingQuads, four vertices, draw): a few hundred tiny draws per armed
NPC and frame. fixes/ModelBatch and tools/patches/PatchModelBatch.java (three jars): when a part compiles its lists it runs
once more with BModelQuad's Tessellator calls captured, each quad moved into the part's space by the GL matrix its children
set (read back), all in one more list with one draw per brightness run; the part draws that list instead of its two (its
own transform still applies every frame). Decimation's lists stay: ModelBatch.enabled switches back at once. Dev test
gunbatch (armed kinds drawn both ways in one frame, bare and dressed): 0 pixels differ. npcdraw (old way = Decimation's
whole drawing: seven layers, sight checks, tiny draws; turns of 30 frames): camp 46 armed NPCs 12.3 to 6.0 ms a frame
(frame median 28.3 to 16.9), horde 82 infected 8.4 to 4.7 ms (16.5 to 12.9), fight 40 soldiers and bandits shooting 35.9
to 19.3 ms (50.0 to 32.4).
The muzzle flash itself is not the cost (user's guess): in the fight an NPC drawn while its gun flashes took 939 us, not
flashing 840 us (old way), 9 % of the draws flash: about 1 % of the NPC drawing. A military NPC with full kit simply costs
about 840 us old, 480 us now, fighting or not; 40 of them in view are most of a fight's frame.

## Aiming with OptiFine made the gun vanish, "Stack overflow" every frame, FPS sinking (19 Oktober 2026, FIXED, released in 0.65.0)

User (brother's laptop, NVIDIA RTX 2060, OptiFine HD U E7, jar 9e89d0d3): with the EOTech 558 "the aiming model is doesnt
show up, so the guns actually invicble ... minute by minute the perfomance getting wrose". Its log: from 22:50:56 to
23:06:47 "@ Post render 1283: Stack overflow" 4747 times and "[deciworldgen] scope: renderHand failed:
InvocationTargetException" 4779 times, one per frame while aiming; GlStackGuard never had to repair a frame.
Cause: Forge posts RenderHandEvent before EntityRenderer.renderHand; OptiFine posts it inside renderHand. While a sight
zooms, fixes/ScopeZoom cancels the event and calls renderHand itself, so under OptiFine its own call came back into its
handler, again and again until Java's stack ran out: every level pushed a GL matrix (over 32: overflow), the gun was never
drawn, and the warning was logged each frame. Without OptiFine (every dev and Mac test) the event comes before
renderHand, so it never showed.
Fix: ScopeZoom ignores the event while it draws the hand itself (OptiFine's inner event then draws normally); the warning
at most once a minute. Checked in the production client with OptiFine (wyvern's own jar in the scratchpad mods): dev test
scopeperf with sight=eotech558 now checks GL errors at five points of every frame: before the fix aiming 117719 errors in 5
s (modelview at 32 before the hand, gun gone on the shot), after 0 in every phase, gun and see-through glass drawn.

## A sight's glass showed a frozen picture at the hip after aiming (19 Oktober 2026, FIXED, released in 0.65.0)

Same report: "on idle aiming, the 558 scope render a 'new camera' in there". Since v0.58.0 ScopeZoom copies the screen
under the glass into the scope texture only while aiming or zooming; back at the hip the glass kept the last aimed view,
a frozen picture of another place. Now the texture is filled with a plain dark lens colour (ScopeZoom.LENS) once when the
copying stops (in strips of 32 rows). Dev test scopeperf phase "scoped gun at the hip after aiming" reads the texture back:
before 0 % lens, after 100 % (dev client and production client with OptiFine).

## Muzzle flash lit every mob drawn after the shooter (19 Oktober 2026, FIXED, released in 0.65.0)

User: "when npc shot a gun, some 'flash' rendered on their npc or mobs body ... the flash appear on all npc or mobs
body". Cause: Decimation turns item lighting off (ClientEmptyB.disableItemLighting = RenderHelper.
disableStandardItemLighting: lighting, both lights, colour material) to draw something glowing and never turns it back
on: the gun while its flash time is above 0 (GunItemRenderer.renderItem, renderEquippedThirdPerson, MuzzleFlashModel),
the whole body of a firing player (ModelDeciPlayer.renderPlayer glDisable(LIGHTING)), a mech's lamps (ModelMech), the
Gawker (GawkerRenderer.getEntityTexture). Entities are drawn in list order, so every one after a shooter lost its
shading for that frame (flat, bright). An NPC's night vision goggles turned both lights off and only lighting back on:
its own vest, backpack and gun after the goggles were lit by ambient light alone.
Fix: fixes/EntityLighting puts the standard entity lighting back (the four switches only, light directions untouched)
before and after each living entity is drawn, so glows inside one entity stay as Decimation drew them;
tools/patches/PatchLightLeaks.java (three jars, on top of PatchHumanLayers) saves the switches before an NPC's goggles
and puts them back exactly after. Dev test lightleak (player in third person firing, a soldier whose gun keeps flashing,
a civilian with goggles, infected after them): before 1108 of 1203 mob drawings started unlit and the goggles NPC ended
dark 95 of 95; after 0 of 1309 and 0 of 119.
Left as Decimation drew them [not fixed]: a player wearing night vision goggles draws the rest of its own body unlit and
without alpha test (ModelDeciPlayer), a mech's parts after its lamps get ambient light only (ModelMech), a turret's
muzzle flash (TurretRenderer, MuzzleFlashModel.renderAt) leaves lighting off for tile entities drawn after it.
The parked "Every Decimation mob drawn solid black" (brother's laptop) may be this leak on another driver: after the
goggles, lighting was on with no light, which is near black where a driver resets the material [not verified].

## Server stalls while new city areas generate (20 Oktober 2026, FIXED, released in 0.65.0)

First three player session (docs/player_hosting.md 17): the host logged "Can't keep up" three times, 2.5 to 2.8 s behind
(01:24:19, 01:30:53, 01:31:08), each while our worldgen built new city areas (dress totals, a watchtower, a rail station
in the same seconds) as players explored in different directions of a new world. Players feel it as rubber banding.
The user after the next session: "when new generated building or chunk its bit laggy, idk its from the city it self or
from safezone (i encounter 1 safezone city) or from world generated it self".

Measured on the dev server (`tools/servertest.py 1 type=decimation pregen=512,-1024,6`: 225 chunks of a seed 1 city,
fresh world each run; JFR of the server thread, split by tools/genprof.py; docs/performance.md 9):
- Light was 39 % of generation, 82 % of it from our structure writes (Slices.write): 93 % of a structure write was light.
  World.setBlock lights every block it sets, a light check at the block (60 % of that) and a relight of the column and
  its four neighbours when the column's height changes (31 %). Written column by column, every new column moved the open
  side of the half built rooms one column on, and their light was taken away and put back again.
- Saving chunks 24 %: 188,721 queued block ticks after 225 chunks (gravel 173,480 and sand 14,747 from vanilla
  decoration: gravel veins in stone, sand and gravel discs at water), and vanilla walks the whole queue for every chunk
  it saves.
- Highways' link cache 8 %: its keys (rx << 32 ^ rz) hash to rx ^ rz, so the HashMap put many regions in one bucket and
  searched trees.

Fixed:
1. terrain/DeciBiome.decorate: vanilla decoration with block updates run at once (World.scheduledUpdatesAreImmediate,
   as vanilla places its springs). A resting gravel or sand block's tick does nothing; one over a cave falls straight
   away (population drops falling blocks instantly). Queued ticks 188,721 to 3,556 (left: our own falling blocks,
   liquids); saving 1.22 s to 0.05 s. Decimation world type only (our biomes).
2. worldgen/BlockBatch: Slices writes a window's blocks without per block light (the same hooks as World.setBlock with
   flag 2: onBlockPreDestroy, breakBlock, tile entities, onBlockAdded, height map, client update), then lights the
   written columns once on the final blocks with vanilla's own light update (column sky light as Chunk.relightBlock
   sets it, then World.updateLightByType over the ring around and the written columns, block light at every written
   cell whose opacity or light changed). Off switch: -Ddeciworldgen.bulklight=false (servertest `bulklight=false`).
3. worldgen/Keys.pair (spread keys, unit test KeysTest RED with the old key shape: 16 of 256 buckets); Highways' cache
   uses it, and Highways.sides asks for the highway flags once a chunk instead of once a block column (25 cached links
   per ask; 5 to 8 % of generation in the live flights).
4. BlockBatch refined after a live profile: ring columns take the heights of the written columns beside them (not the
   whole window's), and block light is checked only where block light already was or a block's own light changed.

Results (back to back runs; pregen of 225 chunks, server thread):
| | original | now |
|---|---|---|
| loading the 225 chunks | 3.9 s | 2.0 s |
| structure writes (CPU) | 2.47 s | 0.83 s |
| saving | 1.64 s | 0.11 s |
| chunk median / slowest 1 % / slowest | 6.7 / 114 / 300 ms | 5.5 / 40 / 107 ms |

Walk (servertest `walk=256,-1024,40,10`: chunks loaded as a player flying east loads them, view 10, a new row of 21
chunks per chunk crossed, 37 of the 40 rows in the city): original code per step median 222 ms, 90 % 435 ms, worst
466 ms, 15 steps over 250 ms; now 99 to 107 / 160 to 168 / 209 ms, 0 over 250 ms (two runs). Batch off alone (the other
fixes in): 184 to 188 / 404 to 407 / 438 to 541 ms, 11 to 12 over 250 ms.
Light (DevPregen's check: cells y 30 to 160 whose stored light breaks vanilla's own rule, 5.67 M cells): sky 6,714 to
2,197, block 15,423 to 8,620.
Dev client, dev test genwalk (the same 30 chunk flight over new city land in a fresh seed 1 world, view 12; only the
batch off / on): slowest 1 % of server ticks 304 to 188 ms, worst 690 to 403 ms, ticks over 250 ms 14 to 4; client frames
unchanged; noon and midnight shots of the same spot look the same both ways (docs/shots/worldgen_stalls_v0.64.8). Blocks: 0 differences over the area (worlddiff), except 4 daylight sensors whose power is
read from the light (a sensor recomputes it every second).
Safezone city: the refuge area (pregen 656,-880,5) is not slower to generate than other city land (its chunks 12 to 27 ms;
structure writes 0.46 to 0.27 s CPU there). Its 22 residents spawning is NPC work, not generation ("Safezones lag").

Left (each sample of the new profile): vanilla terrain 36 %, light 29 % (our batch relight about 13 %, vanilla's own
light of a new chunk about 10 %), vanilla decoration 14 %. Highways.place, Rails.write and other writers outside Slices
still light block by block (under 2 % of light). In the dev client (dev test genwalk, view 12): a slow tick at each chunk
crossed (vanilla loads the whole new row of 25 chunks in that tick), and vanilla retries the light of edge chunks every
tick until their neighbours exist (about 20 % of the server's time while flying); both are vanilla's (docs/performance.md
9).

## The user died again and again to NPC gunfire at Market District (19 Oktober 2026, OPEN, not looked at)

The Mac's log of the session: "was shot by a survivor" 15 times (Decimation's message for ANY NPC gunfire, damage
source "human": bandits, soldiers, guards), often 5 to 7 s apart, entering and leaving Market District. Who shot and
why the respawn was so close is not examined [not checked].

## Every human NPC walked all loaded entities every tick (19 Oktober 2026, FIXED, released in 0.65.0)

FactionHumanEntity.onUpdate (deci.ah.d: every civilian, bandit, soldier, trader, guard) walked the world's whole loaded
entity list every tick on both sides to know whether someone within 32 blocks holds a gun (then the NPC looks scared,
setGesture), and used the answer only when Math.random() < 0.1. tools/patches/PatchHumanScare.java: the scan only in that
branch, over a 64 block box, first gun ends it; same answer and same random calls. bench "loaded" (30 civilians, about
1250 entities): server 11.1 / 8.5 ms a tick with the original class, 7.4 / 8.0 with the patch (the rest is vanilla item
and chunk work); the scan's share 1 to 3 % before, 0 to 1 % after.

## First sight of armed NPCs stalls a frame: a city refuge's residents (20 Oktober 2026, FIXED in dev)

The user's lag near a safezone city (with the city stalls, released in 0.65.0): its 22 residents had never been measured.
Dev test `refugevisit` (devtest/RefugeVisitTest, docs/performance.md 10): a fresh seed 1 world, a refuge on land never
generated, the keeper held (SafezoneSites.hold) while the land around a player 40 blocks south of it is made and drawn,
5 s of baseline, then the keeper fills it; two refuges in one game (a cold and a warm visit); a per frame probe counts
what each frame after the arrival loaded for the first time.
- Server: the fill of 22 residents takes 3 to 10 ms (guard 0.2 to 0.9 ms each, traders 0.1 to 0.9, civilians about 0,
  survivors 0.1 to 0.2); its tick 54 to 73 ms against 50. Not a stall. The 150 ms server ticks around it are vanilla's
  chunk light population (Chunk.func_150809_p), present with and without residents (JFR).
- Client, the cause: the first frame with the residents took 150 to 630 ms in the dev client and 941 ms in the
  production client (one jar), the second refuge of the same game 120 to 635 ms (new traders, guns and gear). That frame
  loaded 38 to 49 textures for the first time (armour skins, masks, backpacks, guns, bullets, the human layer files of
  NPCs drawn the old way), and Decimation compiled 750 to 1200 model parts and loaded the gear models it loads only at a
  first draw (ModelHuman: masks, vests, backpacks, full armour).
- Fixed: fixes/ModelWarmup loads those ahead, a slice at the end of each frame (8 ms in the menus, 2 ms in a world):
  every gear model as Decimation's first draw sets it (Deci.loadGearModel), the textures under textures/model/{armor,
  backpacks,bullets,guns,masks,entities} of Decimation's jar and HumanLayers' layer files. All done in 1.5 to 1.8 s of
  frame time, mostly at the main menu (140 gear models, 444 textures; 451 in the one jar). The first frame with the
  residents: dev 36 to 114 ms (was 190 to 635), production 80 / 48 ms (was 941 / 122). Check "no Decimation texture
  loaded at arrival": 40 and 28 without the warm up (FAIL), 0 with it (PASS). Process footprint and graphics memory the
  same within the noise (4.1 to 4.6 GB, 1.1 to 1.5 GB). `-Pwarmup=false` (dev) / `-Ddeciworldgen.warmup=false` turns it off.
  Carried to prerelease 91ef15a; its one jar in the production client: 140 gear models and 451 textures in 1.1 s, the
  first frames 81 / 42 ms, all checks PASS.
- Not done: compiling all model parts ahead. Tried: 25,628 parts in about 1.5 s and the arrival at 38 to 46 ms, but the
  graphics memory went from 1.2 to 2.6 GB (Decimation's display lists, one draw per quad, cost about 9 KB of driver memory
  a quad on this Mac [inferred: 1.4 GB / 160k quads]). Left: about 1,100 part compiles at the first sight of a whole
  refuge, the rest of an 80 to 114 ms frame in the worst case of everyone appearing at once. A per frame budget for first
  compiles would need a patch of BModelPart (PatchModelBatch) [idea, not built].
- In real play residents come into the client's view one by one as the player walks in (vanilla sends global entities
  within 80 blocks), not all in one frame as in the test [inferred].

## See-through glass not updated with OptiFine (21 Oktober 2026, FIXED on main and prerelease)

Found by the attachments perf test in the production client with OptiFine HD U E7 (one jar of main, sha256 f0afd86c, this
Mac): Deci.scopeGlassBox() read 0 x 0 for every see-through sight aimed (ACOG, EOTech, SpecterDR at 4x and 1x), so the
box PatchScope's GL feedback pass measures (renderScopeGlass, deci.n.f.b) was never set in that session and
ScopeZoom.onRenderWorldLast never copied the world under the glass ("no glass measured yet"). The dev client on the same
Mac reads 133 x 133 (ACOG), 225 x 225 (SpecterDR 4x), 62 x 48 (EOTech), 70 x 70 (SpecterDR 1x). Seen: the production 4x
shot (docs/shots/attachments_v0.65.0/prod_specter_4x.png) shows sky and a horizon in the glass while the zoomed view
around it is stone floor; the dev shot shows the floor through the glass as around it. Likely since the glass is
measured only while aiming (19 Oktober 2026) or longer [not verified]; also no centring of the sight there
(centreSight learns from the same measurement) [inferred]. Players with OptiFine (the brother's laptop) would see a
picture of the first aimed frame, later the dark lens [inferred].
Dev test scopediag (now reports the box after each measurement) in the same OptiFine client: the glass is drawn every
aimed frame (glassFrame moves) but measured only for some sights: AKM + ACOG 63 measurements in 475 frames (box 133 x
133, as in dev), AKM + SpecterDR 0 in 225 (dev: 27 of 27, box 225 x 225); on the M4A4 (attachments perf) every sight
read 0. Two RenderHandEvents a frame there (OptiFine posts one inside renderHand, ScopeZoom cancels the outer one after
drawing the hand itself); the three glass parts are alike (0.02 thick boxes). Not found yet: whether the measuring pass
runs with the mask off there, or GL feedback returns no polygon (counters inside PatchScope's pass needed: a jar patch).
Next: those counters, then either the fix of the pass or the box from the matrices in Java (no GL feedback; bug.md
"Scope: the glass was measured at the hip" already named that as the next step).
Hypothesis (21 Oktober 2026, to check): the production client's HUD draws a player figure holding the gun in the stats box
(seen in prod_specter_1x.png / _4x.png, absent from the dev shots), so the sight's glass is drawn twice a frame there.
PatchScope measures when (++glassFrame & glassMask) == 0, counting every glass draw: with two draws a frame the hand's
draw can land on odd counts only and is never measured (the HUD draw comes with the mask off), depending on the parity the
session starts with; that fits AKM + ACOG measured, AKM + SpecterDR and every M4A4 case not. Check: glass draws per frame
in scopediag (dev 1, production 2 expected). Fix then in ScopeZoom alone: its own frame counter decides, and the mask is
0 (every draw) around the hand draw on the frames it measures, -1 otherwise.
Confirmed (scopediag, now with per frame counts): the production client with OptiFine draws the glass 6 times every frame
(2 before the hand events, 1 in ScopeZoom's own hand draw, 3 after them: the stats box of the HUD shows the player's
figure holding the gun), the dev client once. PatchScope's (++glassFrame & mask) == 0 then put the hand's draw at count
6k + 3, odd, never hit by mask 1 or 7: 0 measurements in 354 frames. A dev reproduction: scopediag extra=1 draws the
held gun once more after the hand (two draws a frame, the hand's odd): 1 measurement in 222 frames, FAIL.
Fixed in fixes/ScopeZoom alone (no jar patch): its own frame counter picks the measuring frames (every 2nd while
learning a sight, every 8th after), mask 0 (every draw) only around its own hand draw on those frames, -1 otherwise; no
measuring at zoom 1.0. Dev: 30 in 235 frames with the extra draw (PASS), 32 in 244 without; aimdrift with the sniper NPC
in view: shift 0.0049, -0.2189 steady 1 .. 15 s (the recorded value; the test player is now unhurt during the run: a
"survivor" had shot him dead mid run and zeroed the shift, old code too). Production client with OptiFine: 45
measurements in 347 frames (still 6 draws a frame), the four glass boxes as in dev, attachments 22 and perf 7 PASS,
the 4x glass showing what is around it (docs/shots/attachments_v0.65.0/prod_specter_4x_fixed.png).

## Grips drawn at one spot for every gun (21 Oktober 2026, FIXED on main)

User (21 Oktober 2026, ideas item 26: "in future we may fix grip and laser model since they dont have model on it"):
Decimation draws every grip slot attachment at one fixed offset for all guns (GunItemRenderer.renderAttachments' grip
translate, (-3.2, 0.8, 0) model units): on the M4A4 its Foregrip sat on the magazine and the Laser and Flashlight inside
the handguard (study.py renders). Fixed: tools/guns/gripfit.py measures each gun's underside 40 % of the way from its
magazine to its muzzle (pistols 62 %) and writes grip_offsets.txt (318 rows: every gun of the jar and our five, each grip
it takes; `--check` 0 pairs not right); fixes/GripPlacement gives each gun its own moved copy at startup and draws the
Foregrip's parent without its stray plate (its 8 parts hang from a parent whose own plate sits 7.5 behind them). The
magazine in foregrip.bmodel (ammoModel1..4) was never drawn: children of an ammoModel0 the file never declares, which
Decimation's BModelLoader drops; study.py showed it until it did the same. Dev test attachments "placement" checks and
four Gunsmith shots (docs/shots/attachments_v0.65.0/grip_*.png).
Final review (21 Oktober 2026, one reviewer agent): that first gripfit still left 37 of 318 grips floating 0.15 to 1.6
under the gun (bullpups AUG A1 / A3, the Model 642, our MAC-10, RPK, FAMAS-I, L85A1, M1 Garand, MGL, G36C): it rested a
grip on the lowest point of any part touching its footprint (a sliver of a trigger guard), read a slanted part's lowest
corner past the part's end, and its check reused that measure, so it agreed by construction. Rewritten (weapons.md
section 8): an underside map from rays through the real faces, the run ahead of everything hanging below the gun, a flat
seat; the check judges the placed parts alone. RED 42 pairs not right, GREEN 0; the reviewer's march check leaves only
the P90's side mounted lights and the RPK / RPK-74 Foregrip resting on the folded bipod's legs. Lesson: a check must not
reuse the code it checks.

## Our EOTech and ACOG showed raw item names (21 Oktober 2026, FIXED on main)

The EOTech 558 and the ACOG TA11 (fixes/NewSights, v0.41.0) had no lang lines: the game showed "item.eotech558.name"
and "item.ta11acog.name" (seen in the attachments test's RED run). Named in assets/deciworldgen/lang/en_US.lang with the
RK-1 and the SpecterDR ("EOTech 558", "ACOG TA11 3.5x", "RK-1 Foregrip", "ELCAN SpecterDR 1x/4x"); dev test attachments
"items" checks every name.

## aimdrift never learns the sight when it runs right after scopediag in one game (21 Oktober 2026, OPEN, low)

`devtest.py --live scopediag sight=specterdr extra=1` then `aimdrift` in the same game: the AK-74 + ACOG's shift stays
0.0000, 0.0000 for all 15 s (the sight never learned or never zoomed), with the ScopeZoom of ffdbd0a and of 37ddb28
alike, on main and on prerelease; aimdrift alone in a fresh game gives 0.0048 .. 0.0050, -0.2190 steady. scopediag ends
by emptying the hand on the client only and switching the aim off [the cause not investigated: a screen left open, or
the client's empty slot]. Run aimdrift in a game of its own (or before scopediag) until this is understood.

## RK-1 too big and not canted; Foregrips on rifles without a rail (user review 21 Oktober 2026, FIXED on main)
- User: "rk grip ... its actually not vertical grip, it bit sway to left ... the grip use align 45%", "for RPK and
  someguns that doesnt have picatiny rail, they dont support any grip on it", "its to big for decimation guns".
- Cause: the RK-1 was sized from the reference's ratio to the handguard (1.9 x 3.96 x 1.58 against Decimation's Foregrip
  1.0 x 2.7 x 0.8, down to the M4A4's magazine bottom) and built straight down from the side view only; the reference's
  end and bottom views show it rolled 45 degrees to the gun's left. Decimation fits its Foregrip by category, so every
  rifle took one, wooden AKs and the RPK included (docs/references/grip_rails.md).
- Fix: tools/guns/grips.py rk1 v2 (Foregrip size, angled mount, rolled 45 degrees to +z); gripfit.py RAILED (19 rifles,
  244 rows) and a clip check; fixes/GripRails (Gunsmith fit, NPC loadouts), fixes/MountStrip (a vertical grip on a
  rail-less gun goes back to the inventory or the ground, once a second, never while the Gunsmith is open).
- Checks: unit GripRailsTest, dev attachments "rails:" checks (RED 4, GREEN 5; the Gunsmith guard proven by a mutation
  run), gunsmith, glstack; attachmentsmp's pop-off on the dedicated server.

## Scope aim sometimes slow to zoom in (user 21 Oktober 2026, FIXED on main)
- User: "the scope aiming still need more optimizing cause sometime it has delay on it. due to loading the scope render".
- Profiled first (new dev test `aimdelay`: time from the aim to 95 % of the zoom, worst frame, frames over 50 ms; first
  aim with ScopeZoom's learned sights forgotten, then a second aim). Dev: ACOG 3.5x 431 ms first against 187 ms
  learned, SpecterDR 4x 382 / 200, Decimation's 2x 339 / 183, 1.25x sights 166 both; production client with OptiFine
  375 / 193, 392 / 193, 351 / 178. No stall anywhere (worst frame 37 ms, none over 50 ms): nothing is loaded late.
- Cause: ScopeZoom holds the zoom at 1.5x on the first aim of each gun, sight and window shape until it has measured
  where the sight sits (10 settled samples, measured every 2nd frame), at most 1.5 s; the learned positions lived for
  the session only, so every launch waited again on every gun and sight, longer at lower frame rates.
- Fix: fixes/SightMemory keeps learned positions in config/deciworldgen_sights.txt (lines "gun|sight|aspect x y", a file
  of another build ignored), read on ScopeZoom's first frame, written when a sight becomes learned. Only the very first
  aim of a gun and sight ever (per window shape and build) still waits.
- Checks: unit SightMemoryTest (3); aimdelay "after a relaunch zooms like a learned sight" RED (366 ms against at most
  266) then GREEN on all 5 pairs (166 to 193 ms); aimdrift, scopeperf, scopediag PASS; production client with OptiFine:
  relaunch aims 167 to 194 ms, all 5 PASS, scopediag 38 measurements in 300 frames. scopediag's default sight is now
  the ACOG: with Decimation's red dot it counted 0 glass draws (that sight needs no measuring: 1.25x is under the hold).

## Sights fitted on guns without a mount; the red dot on rifles (user 22 Oktober 2026, FIXED on main)
- User: "same rule applied on sight too. for red dot i think its better to put only on pistol ... cause some guns like
  ak or some non-AR platform doesnt fit well". Decimation fits every sight on every gun (category all).
- Fix: sight_mounts.txt + fixes/SightMounts (red dot: pistols with a mount; other sights: rail or side mount),
  GunParts.fits, NpcLoadouts.fitKit, fixes/MountStrip (sights pop off too). docs/references/grip_rails.md section 6.
- Checks: unit SightMountsTest (3), dev attachments "mounts:" RED 4 then GREEN (fit table of 10 pairs, NPC kits, pop off
  with the right ones kept), gunsmith (now the EOTech), aimdelay (the Glock with the red dot), glstack; production client
  with OptiFine 145 PASS (attachments, gunsmith, aimdelay), glstack; dedicated server attachmentsmp + gunsmithmp 21 PASS.

## Release checks of 0.66.0: two test FAILs that were the tests (22 Oktober 2026)
- attachments "full inventory drops it" FAILed in the production client right after genwalk: genwalk leaves the player
  creative and high over a city. In creative, vanilla's addItemStackToInventory takes anything into a full inventory
  and deletes it (creative items are free), so nothing dropped; in survival up there the player fell to death. The rails
  steps now stand on the test arena in survival with the fall reset (reproduced in dev as `genwalk attachments`, then
  PASS). Product note: a creative player with a full inventory loses a popped grip or sight (vanilla's creative rule).
- scopeperf "glass after aiming holds no picture" read 77 % plain lens once in the long production run (after npclook,
  gunbatch, threads, radios, lightleak); the same order twice more and four other runs read 100 %. Flaky once, not
  reproduced [cause not found].

## Update downloads fail with 406 (22 Oktober 2026, FIXED on main, OPEN for shipped versions)
- Found by the real update test after publishing 0.66.0: the 0.65.0 jar found 0.66.0 through the API, then "download
  failed: java.io.IOException: Server returned HTTP response code: 406 for URL:
  https://github.com/ucilmenangis/deciremnant/releases/download/0.66.0/Decimation.jar".
- Cause: GitHub answers 406 to Java's default Accept header ("text/html, image/gif, image/jpeg, *; q=.2, ..."; its
  "*; q=.2" is not a media range) on release downloads of most repositories (ours, leibnitz27/cfr; cli/cli still 302).
  Not the repository rename (decimation-singleplayer is now deciremnant: the old name redirects, then the same 406).
  The 0.64.8 to 0.65.0 update from GitHub worked on 20 Oktober 2026: GitHub changed since. curl and browsers are fine.
- Fix: fixes/Updater.open names "Accept: application/octet-stream, */*" (API calls set their own). Unit
  UpdaterTest.downloadsSendAnAcceptHeaderGitHubTakes (a local server answering 406 like GitHub) RED then GREEN; a Java 8
  client against the real 0.66.0 asset: 200 with the header, 406 without.
- It was a TEMPORARY GitHub incident, not the rename: at 03:01 WIB the same Java request got 302 / 200 from our repository
  (both names), cfr and cli/cli, and the real 0.65.0 jar updated itself from GitHub to 0.66.1 (240 MB in 31 s,
  checksum, swapped, "Up to date: 0.66.1"). 406 seen from about 02:25 (first real update test) to 02:51 (the user's
  Prism game); cfr (never renamed) answered 406 at about 02:30 too. The repository rename happened before 02:03 (gh
  printed the new name when 0.66.0 was created) while the 0.65.0 fake server update worked, so the rename was not it.
  0.66.1's named Accept header stays: the updater no longer depends on how GitHub treats Java's default.
- Released as 0.66.1 (22 Oktober 2026 02:46 WIB): the running 0.66.1 downloaded from GitHub with its own updater.
  Every older updater (0.64.7 to 0.66.0) still fails: their players install 0.66.1 by hand once (0.66.0 shows a
  Download page button on a failed download, 0.65.0 only Try again / Back). The 0.66.0 release notes and the site's
  Updating guide and questions say so (site commit 29bf8a3, pushed by the user).

## study.py renders scaled by part count (22 Oktober 2026, FIXED on branch gunconv-tool)
- Found while comparing the gun converter's AKM (88 parts) with Decimation's akm (194) in one `study.py sheet`: same
  length (28.5 units), ours drawn at less than half the size.
- Cause: tools/guns/study.py render() set k = scale x supersampling, then looped `for k, part in enumerate(gun.parts)`,
  so every face was drawn at (number of parts minus 1) pixels a unit, whatever --scale said.
- Effect: every sheet, render, attach view and icon source since study.py was written. Sheets of two guns with about
  the same part count (Uzi 105, MAC-10 102) looked right, which is why it went unnoticed; sizes in older sheets of guns
  with different part counts are not comparable. Icons are scaled down to 30 pixels anyway (shape unchanged).
- Fix: the loop variable renamed (pi). Check: akm and the converted gun now 707 pixels wide each at --scale 24, the Uzi
  350 (13.6 units). docs/gun_style_guide.md section 1 and the decimation-gun skill say so.
