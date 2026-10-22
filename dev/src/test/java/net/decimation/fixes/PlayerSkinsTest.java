package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Players without a skin of their own wear one of Decimation's 21 human skins instead of Steve (user, 22 Oktober 2026:
 * "each player has unique skin ... random ID ... from 1-10 skin as example"; chosen: by the player's id, own skins kept,
 * all 21). The pick comes from the name, which every client and the server agree on.
 */
class PlayerSkinsTest
{
    @Test
    void theSameNameAlwaysGetsTheSameSkin()
    {
        assertEquals(PlayerSkins.index("ProdTester"), PlayerSkins.index("ProdTester"));
        assertEquals(PlayerSkins.index("Developer"), PlayerSkins.index("developer"));   // names are unique ignoring case
    }

    @Test
    void everySkinIsUsedAndNoneOutsideTheTwentyOne()
    {
        int[] seen = new int[PlayerSkins.COUNT];
        for (int i = 0; i < 21000; i++)
        {
            int k = PlayerSkins.index("Player" + i);
            assertTrue(k >= 0 && k < PlayerSkins.COUNT, "" + k);
            seen[k]++;
        }
        for (int k = 0; k < PlayerSkins.COUNT; k++)
        {
            assertTrue(seen[k] > 800 && seen[k] < 1200, "skin " + k + " picked " + seen[k] + " times of 21000");
        }
    }

    @Test
    void similarNamesSpreadOut()
    {
        int same = 0;
        for (int i = 0; i < 100; i++)
        {
            same += PlayerSkins.index("Steve" + i) == PlayerSkins.index("Steve" + (i + 1)) ? 1 : 0;
        }
        assertTrue(same < 15, same + " of 100 neighbouring names share a skin");
    }

    @Test
    void theOfflineIdIsMinecraftsOwn()
    {
        UUID vanilla = UUID.nameUUIDFromBytes(("OfflinePlayer:ProdTester").getBytes(StandardCharsets.UTF_8));
        assertEquals(vanilla, PlayerSkins.offlineId("ProdTester"));
    }
}
