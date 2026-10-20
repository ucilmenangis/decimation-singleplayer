package net.decimation.fixes;

/**
 * Our name and version where Decimation shows its own (user, 17 Oktober 2026: "for version replace version code with
 * our version code name, and for decimation replace with deciremnant"). PatchMenuTexts sends Decimation's version
 * constant "1.21.10f" in the main menu (under the logo, in the welcome box) here, and the window title; the title
 * line becomes "DeciRemnant: " and the welcome text names DeciRemnant (plain text swaps).
 */
public final class MenuTexts
{
    private MenuTexts()
    {
    }

    /**
     * Our version; a dev or test build shows its last tag and "dev" (0.64.0-main.3+abc1234 becomes "0.64.0 dev", user
     * 20 Oktober 2026: tell a build of main or prerelease from a release); Decimation's when unknown.
     */
    public static String version()
    {
        try
        {
            return shown(Updater.current());
        }
        catch (Throwable early)
        {
            return "1.21.10f"; // Forge's mod list not ready yet
        }
    }

    static String shown(String v)
    {
        if (v == null || v.isEmpty() || "0".equals(v))
        {
            return "1.21.10f";
        }
        return Updater.release(v) ? v : v.split("[-+]")[0] + " dev";
    }

    /** The game window's title (Decimation's was "Minecraft - Decimation v1.21.10f"; no dash in ours: user rule). */
    public static String windowTitle()
    {
        return "DeciRemnant v" + version() + " (Minecraft 1.7.10)";
    }
}
