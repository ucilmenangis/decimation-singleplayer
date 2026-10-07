# Critic pass 1: apartments v0.20 (7 Oktober 2026)

One critic agent, fresh context, judged docs/shots/flats_v0.20 and
sets_v0.20 against the checklist in docs/references/apartment.md.
Scores: living 3, kitchen 4, bedroom 4, bathroom 3, lobby 3, overall 3.

Verified by me before acting (a critic can misread images):
- WRONG: "dining_small / desk_chair chairs face away from the table".
  A facing test set (4 chairs out / in / left / right, set preview) showed
  `in` chairs face the wall / table, showing their backrest to the camera.
  The chairs were right. Lesson: a chair seen from behind looks "turned
  away" in a front-on shot; check with a facing test, not by eye.
- WRONG cause: "floating plaster lumps from decay": they are door lintels
  (within 3) over doorways whose door decayed away. Normal construction.
- RIGHT and the real root cause the critic did not name: narrow apartment
  blocks (12 wide) had NO flats at all, only stairs: the 7 long core took
  the width; their storeys were the empty "living halls" it complained
  about. Fixed (v0.21): apartments under 16 wide use the ladder shaft.
- RIGHT: kitchen upper cabinets read as a floating beam, the fridge's red
  electrical label, mixed counters -> v0.21: iron block fridge (2 high),
  trapdoor cabinet doors, tripwire hook tap, one counter material.
- RIGHT: grey bedroom carpet read as concrete -> spruce plank bedrooms.
- RIGHT: debris slabs in mid room -> debris only along walls, rarer.
- RIGHT: partition walls see-through -> decay removes whole columns,
  much rarer (decay * 0.18 per column per storey).
- RIGHT: lobby bare -> lobby_bench set (2 street benches + plant).
- OPEN (next): living rooms still sometimes empty when no set fits;
  wall detail layer (paintings as entities, curtains, lamps); sofa arms
  (trapdoors); seats touching the TV in the small living sets (needs 4
  deep layouts); bath sets minimal; bedroom minimum 3 wide; audit camera
  sometimes against furniture.
