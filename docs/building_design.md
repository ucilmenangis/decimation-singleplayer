# Procedural building design (city blocks)

Design rules for `Building` (v2, 2026-10-07), from real building layouts
researched for this project, scaled to Minecraft. Read this before changing
the generator; `docs/worldgen.md` covers how buildings are placed.

## Real world layouts (research summary)

- **Apartment slab**: a double-loaded corridor (units on both sides of a
  central hallway) is the most efficient layout (80 to 85% net to gross).
  Stairs, lifts and shafts sit together in a core, usually central or at the
  end of the corridor. Inside a unit, living and sleeping zones are separate
  and both reached from the entrance hall; the kitchen sits next to the
  living / dining area (Auckland Design Manual; archgyan guide).
- **Office floor**: a core (main stair, lifts, restrooms) serves every
  floor; the rest is mostly open plan with desk rows along the exterior
  windows, meeting rooms of several sizes, a reception at the entrance as
  first contact point, plus storage / IT rooms and a break room or
  kitchenette (Pult office concepts; office floor plan guides).
- **Shop**: almost every convenience store and pharmacy uses the grid
  layout, long parallel aisles with shelving on both sides; a free
  "decompression zone" just inside the entrance; the checkout near the
  entrance, ideally front left; staples at the back; a stockroom behind the
  sales floor (Shopify / Small Business Trends store layout guides).

Sources:
- https://www.aucklanddesignmanual.co.nz/sites-and-buildings/apartments/guidance/the-building/Apartment-building-types/building-access-arrangements
- https://archgyan.com/how-to-design-an-apartment/
- https://pult.com/blog/modern-office-concepts-floor-plan
- https://www.shopify.com.au/retail/the-ultimate-guide-to-retail-store-layouts
- https://smallbiztrends.com/2024/01/store-layout.html

## Minecraft scale

- Storey height 4 blocks: 1 floor slab + 3 air.
- Corridor 2 wide, interior doors 1 wide x 2 high, entrance 1 to 3 wide.
- Apartment unit 5 to 8 deep on each side of the corridor, split by a wall
  into a living part (corridor side: table, chairs, kitchen corner) and a
  bedroom (window side: bed, wardrobe).
- Stair core 4 x 7: two 2-wide straight runs of 4 steps, switching sides
  every storey (switchback), landings at both ends, continuing to a roof
  hatch. Replaces the old ladder shaft.

## Heights, sizes, palettes

- Floors: shop 1 to 2; apartment 2 to 9 (mostly 3 to 5); office 3 to 20 with
  a long tail (about 15% are 12 to 20 floor towers).
- Palettes (wall / trim / accent), vanilla 1.7.10 blocks: brick, white
  concrete look (white hardened clay), light gray and gray clay, orange and
  brown clay, cyan clay with tinted glass, sandstone (smooth + chiseled),
  quartz (block + pillar), plain stone brick, plain hardened clay. The
  ground storey often uses the trim material (shop fronts, lobbies).

## Decay and biome overgrowth

Per building: a decay level 0.15 to 0.55 and a collapse chance. Decay =
holes in walls (more on upper floors), cracked / mossy variants, broken
windows, rubble on floors, partial collapse of the top floors (a corner
falls in over 1 to 3 storeys).

Overgrowth follows the biome at the building's centre (asked from the world's
biome generator, so it is deterministic even before chunks exist):
- forest / plains / temperate: vines on outer walls, moss, grass and leaves
  on roofs and in holes;
- jungle / swamp: heavy vines and moss, jungle leaves;
- snowy: snow layers on roofs, floors near openings, no vines;
- desert / savanna / mesa: sand drifts and dead bushes, sun bleached (less
  moss), sandstone favoured.
Exterior vines need a 1 block ring outside the walls, so plans include a
margin ring (written as SKIP except where overgrowth goes).

## Streets

- Sidewalk: 2 block ring (cell offsets 5..6 and 62..63) of smooth stone
  (double stone slab) at terrain height, plants cleared.
- Parked / wrecked cars on the road lanes, spaced at least 9 blocks, about
  25% of slots filled, never in intersections, rotated ALONG the street.
  Decimation props render with rotation (metadata % 4) * 90 degrees
  (`PropRenderer`). Car wreck model (deci:BlockWreckage*): long axis along x at 0 degrees,
confirmed in game 2026-10-07 (the first guess, along z, put every car across
the road). North-south streets use metadata 5/3, east-west streets 4/2.
