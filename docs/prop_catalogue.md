# Decimation prop catalogue (what every block looks like)

From the prop gallery (round 2 step 4b, 7 Oktober 2026): every `deci:`
block except roads, map barriers and sound blocks, placed on a sky platform
with metadata 3 and photographed 3 at a time
(`./gradlew runClient -Pautotest -Pgallery`, images in
`docs/shots/gallery_v0.16/gallery_<n>.png`, view -> names in `views.txt`
there). Read THIS instead of re-shooting. Size = rough model size in blocks
(w x h, long side first). "Faces camera" = its front points south at meta
3 (camera stood south of the row).

Facing rules (see docs/building_design.md "Street life"): BlockProp models
turn by metadata % 4 * 90 deg; for props with extra rotation 180 the front
points east 2, south 3, west 4, north 5. Doors use VANILLA door metadata
(they copy BlockDoor without extending it): lower half 0..3 (0 west edge,
1 north edge, 2 east edge, 3 south edge of the cell), upper half 8; place
both halves or the door removes itself. Decals and graffiti render as an
upright flat picture (a wall decal); which face they cling to by meta is
`[not verified]`, test against a wall before using them.

Full cube texture blocks (usable as building materials, not props):
- BlockBrick_1 red brick, BlockBrick_2 dark red worn brick, BlockBrick_3
  white / grey brick.
- BlockStone_1..8: grey stone variants (1 plain grey, 2 grey with seams,
  3 cracked, 4 dark grey, 5 small stone brick, 6 polished light grey,
  7 light grey, 8 lighter grey).
- BlockMetal_1 dark diamond plate, BlockMetal_2 dark plate, BlockMetal_3
  rusty brown metal; BlockMetalWall camouflage green (military).
- BlockCeiling_1 charcoal, _2 dark grey, _3 light grey stone, _4 light
  grey: ceiling tile textures.
- BlockFloorCarpet_1..6: carpet cubes (1 dark grey, 2 mid grey, 3 grey,
  4 light grey, 5 teal, 6 brown): use as the floor layer.
- BlockFloorTiles_1 black / white checker, _2 cream / black checker,
  _3 black tile: kitchens, bathrooms, diners.
- BlockWallOffice_*: interior wall panels in colour sets, `_Top` plain
  upper wall, `_Bottom_N` the same colour with a skirting / dado stripe
  for the lowest course: set 2 pink, set 3 light blue, set 4 white with
  `_4_Bottom_1` dark wood panelling, base set white with green (Bottom_1),
  dark red (Bottom_2), grey (Bottom_3) stripes. Build interior walls as
  Bottom on the first course, Top above.
- BlockSandbagStack green grey, BlockSandbagStackBeige beige: sandbag cubes.
- BlockPackagedCocaineStack: cube of wrapped white packages.
- BlockMilitaryBarrier: brown mesh cube (HESCO style).

Props (single block cell, model may be bigger):

| block | looks like | size | notes |
|---|---|---|---|
| BlockAcidPit | flat white tray / shallow pit | 1 x 0.1 | lab |
| BlockAlarmBell | tripod stand | 1 x 1.3 | |
| BlockAmmoCrate | long black case | 1.3 x 0.4 | loot |
| BlockAmmoCrateLarge | big black hard case | 1.5 x 0.8 | loot |
| BlockBarrel | dark oil drum | 0.6 x 1 | |
| BlockBarrier | grey concrete jersey barrier | 1.3 x 0.7 | long along x at meta 3 |
| BlockBarrierTall | tall grey concrete slab | 1 x 2.2 | multiblock |
| BlockBicycle | upright bicycle | 1.2 x 1 | long along z at meta 3 |
| BlockBodyBag{Black,Blue,Green,Orange} | body bag lying | 1 x 0.3 | green looks like an olive bag |
| BlockCCTV | small security camera | 0.3 | wall / ceiling mount |
| BlockCanFire | small tin can | 0.3 | |
| BlockCanTrap | thin stake with a wire | 0.2 x 1.5 | tripwire can trap |
| BlockCardboardBoxes1/2/3 | spread pile / tall stack / small stack of boxes | 1 x 0.5..1 | |
| BlockCarePackage | black military crate on a pallet, strapped | 1.5 x 0.9 | |
| BlockCeilingVent / Corner | flat dark grey vent box (L shape for corner) | 1 x 0.3 | mount on ceiling |
| BlockChair | wooden dining chair, tall slatted back | 0.6 x 1.1 | faces camera |
| BlockChesstable | small pedestal table with a chessboard top | 0.8 x 0.8 | |
| BlockCocaPlant | leafy green bush | 0.8 x 0.9 | usable as a potted plant |
| BlockConcertinaWire | coil of razor wire | 1 x 0.6 | |
| BlockCone | orange traffic cone | 0.4 x 0.6 | |
| BlockCookingStation | small camping stove with pots | 0.6 x 0.4 | NOT a kitchen oven |
| BlockDumpster | big dark green dumpster | 1.6 x 1 | |
| BlockElectricBox1 | utility pole with two junction boxes | 0.5 x 1.8 | |
| BlockElectricBox2 | thin pole with one junction box | 0.3 x 1.8 | |
| BlockElectricBoxBin | tall grey electrical cabinet, red label | 0.8 x 1.8 | could pass as a fridge / locker |
| BlockElevator | black boxy grille unit | 1 x 1 | elevator part |
| BlockElevatorButton | grey box with a keypad panel | 1 x 1 | |
| BlockExitLight | small green EXIT sign | 0.5 x 0.3 | mount over doors |
| BlockFlagPollUAHD / USSR | tall flag pole, blue / red flag | 0.3 x 5 | |
| BlockFlatscreenTV (+_Emergency colour bars, _News, _Target "personal best", _Youtube) | flat TV screen on a low stand | 1.2 x 0.6 | screen faces camera |
| BlockHangingPlayer | corpse hanging on a rope | 0.6 x 1.5 | hang from ceiling |
| BlockHazardLamp | small lamp with a yellow base | 0.5 | |
| BlockHazardLight | work light tower, two lamps on a pole over a crate | 1 x 2.5 | |
| BlockHazardScreen_1 | white plastic strip curtain on a rail | 1 x 2 | quarantine / cold room doorway |
| BlockHazardScreen_2 | short strip curtain | 1 x 0.6 | |
| BlockHazard_1 | brown block with hazard stripes | 1 x 1 | |
| BlockHazardbarrier | red / white striped sawhorse | 1 x 0.7 | |
| BlockHazardpole | red / white striped bollard | 0.2 x 1.3 | |
| BlockHedgehog | anti-tank hedgehog | 1.2 x 1.2 | |
| BlockKeycardScreen / _Laboratory / _Military | small keypad screen (blue / white / green) | 0.4 x 0.6 | wall mount by doors |
| BlockLantern | camping lantern, orange glow | 0.3 x 0.5 | survivor camps |
| BlockLight / BlockLightOff | flat rectangular ceiling light panel | 1 x 0.1 | mount on ceiling |
| BlockMailbox | blue US mailbox | 0.7 x 1 | |
| BlockMaintenanceScreen | trailer mounted LED road sign | 2 x 2.5 | road works |
| BlockMechWreckage | wrecked walking mech | 2 x 2.5 | |
| BlockMedicalCrate | white case with a red cross | 0.8 x 0.6 | loot |
| BlockMetalShelf | 2 tall metal shelving with cardboard boxes | 1 x 2 | multiblock 1x2x1 |
| BlockMetalShelf_Empty | empty metal shelving | 1 x 2 | multiblock 1x2x1 |
| BlockMetalTable | grey metal table, legs at the ends | 1.5 x 0.8 | long along x at meta 3 |
| BlockMetroRailing1 / 2 | teal railing post / 3 post railing | 1 x 1 | |
| BlockMilitaryCrate | olive crate | 1 x 0.8 | loot |
| BlockMilitaryRadio (+Off) | olive radio equipment stack | 1 x 0.6 | |
| BlockMilitaryRadioSmall (+Off) | small olive radio box | 0.6 x 0.4 | |
| BlockMissileLauncher | quad missile launcher on a tripod | 2 x 3 | |
| BlockMonitor | old CRT computer monitor | 0.7 x 0.7 | screen faces camera; put on desks |
| BlockNewsStand1 / 2 | blue / red newspaper box on a post | 0.5 x 1.1 | |
| BlockOfficeChair | black swivel office chair | 0.7 x 1.1 | faces camera |
| BlockPhonebooth | small payphone cabinet | 0.7 x 1.2 | |
| BlockPhonebox | tall phone booth | 1 x 2 | multiblock 1x2x1 |
| BlockPoliceCrate | dark blue crate | 1 x 0.8 | loot |
| BlockPowerGenerator | red generator, smoking | 1 x 0.8 | |
| BlockPowerPole | very tall wooden power pole | 0.3 x 6+ | |
| BlockPresent / 2 / 3 | gift box red / blue / green | 0.5 | |
| BlockPropellerTrap | can with a red warning diamond and blades | 0.6 | trap |
| BlockRadio1 | small portable radio | 0.5 x 0.3 | |
| BlockRadioTower | rusty lattice tower segment | 1 x 1 | |
| BlockRedlight | red beacon on a short post | 0.3 x 0.6 | |
| BlockRope / BlockRopeTop | vertical climbing rope / its top | 0.1 x 1 | |
| BlockRussianRoadSign / Arrow / Warning | sign board on two posts (info icon above) | 2 x 1.8 | |
| BlockSafezoneSignEntry / Exit | floating green / red "Entering / Leaving Safezone! PVP..." text | 3 x 1 | server UI, do not use in ruins |
| BlockShopDisplay_1 | white chest freezer / display counter | 1 x 1 | shops |
| BlockSign_Decimunition / Gunsrus / Metro / Minebay / Minedonalds / Mineway | flat shop logo sign (Minebay looked like the Mineway logo in the gallery [inferred]) | 1.5 x 0.5 | facade signs |
| BlockSkeletonGround / Wall | skeleton lying / sitting against a wall | 1 x 0.6 | |
| BlockSpotlight | big military searchlight on a tracked base | 1.5 x 1.5 | |
| BlockStereo | black stereo / boombox | 0.6 x 0.4 | |
| BlockStorageCrate | tan footlocker | 1 x 0.6 | |
| BlockStreetBarrier | concrete base with a tall checkered panel | 1 x 2 | multiblock |
| BlockStreetBench | 1 block bench with backrest | 1 x 0.9 | faces camera at 3 |
| BlockStreetBin | slatted wooden street bin | 0.8 x 0.9 | |
| BlockStreetLight / Middle | tall lamp post, one arm / two arms | 0.3 x 8 | |
| BlockStretcher | olive army stretcher on legs | 1.5 x 0.5 | long along x at meta 3; hospitals |
| BlockStudioCamera | TV camera on a tripod | 0.8 x 1.5 | |
| BlockSupplyDrop | green crate, smoking | 1.2 x 0.8 | the supply drop itself |
| BlockTarget | small bullseye target | 0.5 | |
| BlockTicketGate / _Open | metro turnstile closed / open | 1 x 1 | |
| BlockTire / BlockTireStack | tyre lying flat / tyres stacked on a pallet | 1 x 0.3 / 1 x 1 | |
| BlockTrafficLights / Middle | tall pole with lights at the top / with an arm | 0.3 x 6 | |
| BlockTrashBag1 / 2 | black bin bag | 0.6 x 0.5 | |
| BlockTrashcan | ribbed metal dustbin | 0.8 x 1 | |
| BlockTruckWreckage1..6 | pickup truck, black / blue / green / red / grey / white | 3 x 6 | long axis like cars |
| BlockVendingMachine_1 / 2 | rusty brown / green vending machine | 1 x 2 | single block prop, front faces camera |
| BlockWallflag | flat US-style flag banner | 1.5 x 0.8 | wall mount |
| BlockWashingMachine | front loading washer | 1 x 1 | door faces camera |
| BlockWaterPallet | pallet of water bottles | 1 x 0.8 | |
| BlockWaterPalletTarp | pallet under a blue tarp | 1 x 0.9 | |
| BlockWaterfountain | wall drinking fountain on a post | 0.6 x 1 | |
| BlockWaterpump | hand water pump on a stone base | 0.8 x 1.3 | |
| BlockWeaponCabinet | tall dark metal locker | 0.8 x 1.8 | loot |
| BlockWoodCrate / Open | wooden crate closed / open | 1 x 0.8 | loot |
| BlockWoodTable | wooden trestle table | 1.5 x 0.8 | long along x at meta 3 |
| BlockWoodTable2 | wooden table, straight legs | 1.5 x 0.8 | long along x at meta 3 |
| BlockWreckage1..5 | car, black / blue / green / red / grey | 2.5 x 5 | long along z at meta 3 |
| BlockWreckageMilitary1 | jeep | 2 x 3 | |
| BlockWreckageMilitary2 | APC / tank hull | 3 x 7 | multiblock 3x3x7 |
| BlockWreckageMilitary3 | crashed helicopter, smoking | huge | |
| BlockWreckagePolice1 / 3 | police car "303" with light bar | 2.5 x 5 | |
| BlockWreckagePolice2 | police car with the roof torn off | 2.5 x 5 | |

Doors (2 high, 1 wide, place both halves): Door_Blue_1 plain blue,
Door_Blue_2 blue with an EXIT sign, Door_Blue_3 blue with a window;
Door_Emergency_1 (and _Locked) red brown with a window, _2 plain, _3 with
EXIT; Door_Green_1 window, _2 EXIT, _3 window; Door_Orange_1 window,
_2 EXIT, _3 window; Door_Office_1 dark wood; Door_Metal_3 grey metal with
vent slats; Door_InfektorSecurity_1 and Door_security_1 grey metal with a
mesh window.

Decals (flat upright pictures): bloodsplat1..5 red splatters of different
shapes; hazardbio / hazardnuclear yellow diamond signs; imagegeneric a dark
framed picture; notegeneric a paper note; sovietsymbol red hammer and
sickle; usflag a US flag (tall); warningcctv yellow, warningchemicalstorage
yellow "CAUTION", warninglights orange, warningslipperyfloor orange
"WARNING", warningtrespass white "WARNING", warningwatchstep orange,
warningxray white.

Graffiti (flat upright pictures): 1 "GET OUT HERE" red / black, 10 "WE'LL
BE TOGETHER SOON", 11 a small tag, 12 skull with crossed swords, 2 blue
tag, 3 multicolour tag, 4 grey blue tag, 5 colourful tag, 6 green tag,
7 white / green tag, 8 pink tag, 9 "WHERE IS YOUR GOD NOW?".

Missing from Decimation, use vanilla stand-ins: bed (vanilla bed), sofa
(stairs + carpet), toilet (quartz stairs / cauldron), sink and bath
(cauldron), fridge (BlockElectricBoxBin or iron block), kitchen counter
(quartz / stone slabs, trapdoor cupboards), bookcase (bookshelf), plant pot
(flower_pot, BlockCocaPlant).

## Measured footprints (8 Oktober 2026, `docs/prop_footprints.tsv`)

The "Size" column above is eyeballed and wrong for some props (bicycle
said 1.2 x 1; it draws about 2.4 long). Measured instead: `./gradlew
runClient -Pautotest -Pfootprint` (dev/) photographs every prop alone
from 40 blocks straight down (meta 3, front south) next to an empty
reference shot, `python3 tools/footprint.py > docs/prop_footprints.tsv`
turns the pixel difference into how far the model reaches past its own
block (west / east / north / south, blocks) and the cells it covers
(overhang under 0.3 counts as inside). A plain cube measures 0 on every
side. 26 props show nothing from above (flat decals, signs and wall
screens seen edge on, or no model) and are listed as "-". Footprints
turn with facing (meta % 4 * 90). Not used by the placer yet.
