package net.decimation.fixes;

/**
 * What the news panels' detail popups show (user, 21 Oktober 2026: "add popup page to show detail of the content ... for
 * multiplayer, it show several image with carousel design and below that show title and description about the feat,
 * for updates same like multiplayer it show updates list, website stay intact, coming soon show information about what
 * feat we gonna add in future"; fewer promises: only what is being built; docs/main_menu.md 2h). MenuDetail draws them.
 * The pictures are Decimation menu textures written by tools/menu_assets.py (they show Decimation's art: never in the
 * repository). Plain Java: unit tests load it without Minecraft.
 */
public final class MenuDetails
{
    /** A news panel address that opens a popup instead of a web page: ADDRESS + id. */
    public static final String ADDRESS = "deciremnant:detail/";
    public static final String UPDATES = "updates", COMING = "coming";
    /** The longest line a popup draws at half size inside its panel. */
    static final int LINE = 46;

    private MenuDetails()
    {
    }

    /** One slide of a popup: its picture (textures/gui/menu/<picture>.png in Decimation's jar), heading and lines. */
    public static final class Slide
    {
        public final String picture, heading;
        public final String[] lines;

        Slide(String picture, String heading, String... lines)
        {
            this.picture = picture;
            this.heading = heading;
            this.lines = lines;
        }
    }

    /** A popup: title and line at the top, its slides (none for the release list), the red action button (or null). */
    public static final class Detail
    {
        public final String id, title, line, action;
        public final Slide[] slides;

        Detail(String id, String title, String line, String action, Slide... slides)
        {
            this.id = id;
            this.title = title;
            this.line = line;
            this.action = action;
            this.slides = slides;
        }
    }

    /** The popup behind an id, or null. The Updates popup's line and list come from the release check (MenuDetail). */
    public static Detail get(String id)
    {
        if (COMING.equals(id))
        {
            return new Detail(COMING, "Coming soon", "What we are building next", null,
                new Slide("detail_coming0", "Multiplayer in 1.0.0", "Host your world for your friends, listed on",
                          "the Community page."),
                new Slide("detail_coming1", "New cities in 1.0.0", "New city buildings, planned for the 1.0.0",
                          "release."));
        }
        if (UPDATES.equals(id))
        {
            return new Detail(UPDATES, "Updates", "", null);
        }
        return null;
    }

    /** The slide after `step` steps from `i` among `n`, wrapping both ways. */
    static int step(int i, int n, int step)
    {
        return n <= 0 ? 0 : ((i + step) % n + n) % n;
    }

    /** The first readable line of a release's notes (Markdown): no headings marks, list marks or links, at most max. */
    static String summary(String body, int max)
    {
        if (body == null)
        {
            return "";
        }
        for (String l : body.split("\r?\n"))
        {
            String s = l.replaceAll("^[#>*+\\s-]+", "").replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1").replaceAll("[*_`]", "").trim();
            if (!s.isEmpty())
            {
                return s.length() <= max ? s : s.substring(0, max - 3).trim() + "...";
            }
        }
        return "";
    }

    /** "2026-10-20T03:55:00Z" as "20 Oct 2026" (English, as the game reads). */
    static String date(String iso)
    {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        if (iso == null || !iso.matches("\\d{4}-\\d{2}-\\d{2}.*"))
        {
            return "";
        }
        int m = Integer.parseInt(iso.substring(5, 7));
        return Integer.parseInt(iso.substring(8, 10)) + " " + months[Math.max(1, Math.min(12, m)) - 1] + " " + iso.substring(0, 4);
    }

    /**
     * Where the popup goes on a GUI of width x height units: {panel x, y, w, h, picture w, picture h}. The picture is 16:9
     * and shrinks until the panel fits with 8 units around it; the panel is never narrower than 220 units.
     */
    static int[] layout(int width, int height, boolean pictures)
    {
        int other = 108;                                       // title, line, dots, heading, two lines, buttons, margins
        int pw = Math.max(220, Math.min(340, width - 60));
        int picW = pictures ? pw - 20 : 0, picH = picW * 9 / 16;
        if (pictures && other + picH > height - 16)
        {
            picH = Math.max(60, height - 16 - other);
            picW = picH * 16 / 9;
        }
        int ph = other + (pictures ? picH : 110);
        int x = (width - pw) / 2, y = Math.max(8, (height - ph) / 2);
        return new int[] {x, y, pw, ph, picW, picH};
    }
}
