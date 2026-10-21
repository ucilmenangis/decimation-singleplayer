package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Armor 20 % weaker against NPC fire (user, 22 Oktober 2026: "nerf all armor buff by -20% that affected to player when
 * they shooted by npc"; chosen: each piece's protection x 0.8, helmets too, player shots unchanged).
 */
class ArmorGunfireHandlerTest
{
    @Test
    void eachPieceBlocksAFifthLessAgainstNpcs()
    {
        assertEquals(0.6f, ArmorGunfireHandler.againstNpc(0.5f), 1e-6f);    // blocked 50 %, now 40 %
        assertEquals(0.44f, ArmorGunfireHandler.againstNpc(0.3f), 1e-6f);   // a helmet: 70 % then 56 %
        assertEquals(1f, ArmorGunfireHandler.againstNpc(1f), 1e-6f);        // no armor stays no armor
    }
}
