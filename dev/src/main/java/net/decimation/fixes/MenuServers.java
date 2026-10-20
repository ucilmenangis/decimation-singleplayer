package net.decimation.fixes;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The rows of Decimation's official server list (user, 17 Oktober 2026: "replace it with Bukovka and Chernarus only and
 * its say coming soon"). PatchOfficialServers makes ServerListPinger.officialServers (deci.k.b.bg) return this list
 * instead of BoehMod's four dead servers. Each host ends in .invalid, a name that never resolves (RFC 6761), so a row's
 * ping fails at once without touching the network and the row shows its "Coming soon" (PatchMenuTexts, ListElement).
 * Hovering a row fades its map's pictures in behind the page (textures/gui/menu/serverbackgrounds/<map>0..7.png):
 * Decimation's own for Bukovka, the user's Chernarus pictures for Chernarus (tools/menu_assets.py). Client only.
 */
@SideOnly(Side.CLIENT)
public final class MenuServers
{
    /** Name on the row and the key of its map's pictures, in list order. */
    static final String[][] ROWS = {{"Bukovka", "bukovka"}, {"Chernarus", "chernarus"}};
    private static final int PORT = 25565;

    private MenuServers()
    {
    }

    /** The official list's rows (ServerListEntry objects); called by Decimation in place of its own list. */
    public static List<Object> official()
    {
        List<Object> rows = new ArrayList<Object>();
        for (String[] row : ROWS)
        {
            rows.add(Deci.officialServer(row[1].toUpperCase(), row[0], host(row), PORT, "§aOfficial", row[1]));
        }
        return rows;
    }

    /** Each row's "host:port" (MenuStatus: the Server Status page's Global Server row follows this list). */
    public static List<String> hosts()
    {
        List<String> out = new ArrayList<String>();
        for (String[] row : ROWS)
        {
            out.add(host(row) + ":" + PORT);
        }
        return out;
    }

    private static String host(String[] row)
    {
        return row[1] + ".deciremnant.invalid";
    }
}
