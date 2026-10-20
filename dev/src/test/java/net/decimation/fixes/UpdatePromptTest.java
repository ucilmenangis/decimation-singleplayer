package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** The question a player gets when a newer release is out (docs/main_menu.md 2g): their choice, never forced. */
class UpdatePromptTest
{
    @Test
    void asksToUpdateWhereTheUpdaterCanSwapTheJar()
    {
        assertEquals(Arrays.asList("§aDeciRemnant 0.65.1 is out", "§7239 MB. Update now?", "§7Later: Settings, View Updates"),
                     Arrays.asList(UpdatePrompt.lines("0.65.1", 239L << 20, true)));
        assertEquals(Arrays.asList("Later", "Update"), Arrays.asList(UpdatePrompt.labels(true)));
        assertEquals("§7Update now?", UpdatePrompt.lines("0.65.1", 0, true)[1]);         // size not known
    }

    @Test
    void offersTheDownloadPageWhereItCannot()
    {
        assertEquals(Arrays.asList("§aDeciRemnant 0.65.1 is out", "§7Download it from GitHub and", "§7put it in your mods folder"),
                     Arrays.asList(UpdatePrompt.lines("0.65.1", 239L << 20, false)));
        assertEquals(Arrays.asList("Later", "Download"), Arrays.asList(UpdatePrompt.labels(false)));
    }

    @Test
    void failedUpdateOffersTheDownloadPage()
    {
        assertEquals(Arrays.asList("Try again", "Download page", "Back"), Arrays.asList(UpdatePrompt.screenLabels(Updater.State.FAILED)));
        assertEquals(Arrays.asList("Quit game", "Later"), Arrays.asList(UpdatePrompt.screenLabels(Updater.State.READY)));
        assertEquals(0, UpdatePrompt.screenLabels(Updater.State.DOWNLOADING).length);
        assertEquals(Arrays.asList("Back"), Arrays.asList(UpdatePrompt.screenLabels(Updater.State.NONE)));
    }

    @Test
    void linesFitTheBoxAndHaveNoDashes()
    {
        for (boolean swap : new boolean[] {true, false})
        {
            for (String l : UpdatePrompt.lines("10.10.10", 999L << 20, swap))
            {
                String plain = l.replaceAll("§.", "");
                assertTrue(plain.length() <= 33, plain);                   // Decimation's box is 200 GUI pixels wide
                assertFalse(plain.contains("-") || plain.contains("–") || plain.contains("—"), plain);
            }
        }
    }
}
