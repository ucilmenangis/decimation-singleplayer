package net.decimation.fixes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraftforge.common.config.Configuration;

/**
 * Auto update from our GitHub releases (docs/ideas.md item 20; user, 13 Oktober 2026: launch, check
 * GitHub, the menu button says "Update" when there is one, it installs with a progress screen, else
 * "Play"). Client only:
 * - check(): on a background thread, GitHub's release list (pre-releases too: ours are), the newest
 *   release with a "Decimation.jar" asset whose version is above ours (mcmod.info); only release builds ask
 *   (offersUpdates: dev clients and test jars built from main or prerelease stay OFF, user 20 Oktober 2026);
 * - required(): what Decimation's UpdateChecker.isUpdateRequired answers (tools/patches/PatchNoUpdate,
 *   v2): false, the update is never forced (UpdatePrompt asks); Decimation's update page is replaced by its
 *   own progress screen (UpdateProgress; UpdateScreen for a failure, with the download page);
 * - download(): the asset into "<jar>.download" beside the running jar with progress, checked against
 *   GitHub's sha256 (or the size), then staged; the swap (UpdateSwap, its own process) runs when the
 *   game closes ("Quit game" or later), because a loaded jar cannot be replaced on Windows.
 * GitHub allows 60 API calls an hour per address without a login, and players behind one shared address
 * (mobile, campus, CGNAT) run out: the API is asked with the ETag of its last answer (a 304 does not
 * count), and when it fails anyway (403 / 429 limit, timeout) the check falls back to the release feed
 * (releases.atom, newest first, pre-releases too) and the plain download links, which are not the API;
 * the sha256 then comes from the release's Decimation.jar.sha256 asset (tools/onejar.py writes it), else
 * only the size is checked.
 * config/deciworldgen_update.cfg: enabled, api, web. Dev tests: -Ddeciworldgen.update.api / .web / .target.
 */
public final class Updater
{
    /** OFF: this build does not look for updates (a dev or test build, or turned off in the config; message() says which). */
    public enum State { IDLE, OFF, CHECKING, NONE, AVAILABLE, DOWNLOADING, READY, FAILED }

    public static final String ASSET = "Decimation.jar";
    public static final String DEFAULT_API = "https://api.github.com/repos/ucilmenangis/decimation-singleplayer/releases?per_page=10";
    public static final String DEFAULT_WEB = "https://github.com/ucilmenangis/decimation-singleplayer/releases";

    private static volatile State state = State.IDLE;
    private static volatile String latest, url, digest, message = "", source = "";
    private static volatile long size, done;
    private static volatile boolean enabled = true, hooked;
    private static String api = DEFAULT_API, web = DEFAULT_WEB;
    private static File staged, target, cache;

    private Updater()
    {
    }

    public static void configure(File configDir)
    {
        Configuration cfg = new Configuration(new File(configDir, "deciworldgen_update.cfg"));
        enabled = cfg.getBoolean("enabled", "update", true, "check GitHub for a newer release at start and offer it in the menu");
        api = cfg.getString("api", "update", DEFAULT_API, "release list (GitHub API)");
        web = cfg.getString("web", "update", DEFAULT_WEB, "release pages, used when the API refuses (rate limit): <web>.atom, <web>/download/<tag>/Decimation.jar");
        cfg.save();
        api = System.getProperty("deciworldgen.update.api", api);
        web = System.getProperty("deciworldgen.update.web", web);
        cache = new File(configDir, "deciworldgen_update_cache.json");
    }

    /**
     * What Decimation's UpdateChecker.isUpdateRequired answers (tools/patches/PatchNoUpdate): false since 20 Oktober 2026
     * (user: "dont force user to update their version"). While it was true, Decimation's menu put Update in Play's and in
     * Resume Game's place and Play opened the update. The player decides now: UpdatePrompt asks once a launch, and
     * Settings / View Updates and the Updates news panel keep the update one click away.
     */
    public static boolean required()
    {
        return false;
    }

    /**
     * Whether this game can put a new jar in place itself (its jar was found). Where it cannot, the player gets the
     * release page to download the jar by hand (UpdatePrompt, UpdateScreen). Dev tests: -Ddeciworldgen.update.noswap.
     */
    public static boolean canSwap()
    {
        return target() != null && !Boolean.getBoolean("deciworldgen.update.noswap");
    }

    public static State state() { return state; }
    public static String latest() { return latest; }
    public static String message() { return message; }
    /** Where the last check got its answer: "api", "api (not modified)" or "feed". */
    public static String source() { return source; }
    public static long size() { return size; }
    public static long downloaded() { return done; }

    /** Our own version from mcmod.info (FML metadata). */
    public static String current()
    {
        ModContainer c = Loader.instance().getIndexedModList().get("deciworldgen");
        return c == null || c.getMetadata() == null ? "0" : c.getMetadata().version;
    }

    /**
     * A release build: a clean tag ("0.65.0", "1.0.0-beta"). Every other build carries "+<commit>" in the version Gradle
     * takes from git (0.64.8-main.115+baf1c806dd after a tag, 0.65.0-prerelease+14830b9e37-dirty with changes); "0" when
     * unknown.
     */
    static boolean release(String version)
    {
        return version != null && !version.isEmpty() && !"0".equals(version) && version.indexOf('+') < 0;
    }

    /** The branch a dev build came from ("main" for 0.64.8-main.115+baf1c806dd); "" for a release. */
    static String line(String version)
    {
        if (version == null || version.indexOf('+') < 0)
        {
            return "";
        }
        String head = version.substring(0, version.indexOf('+'));          // 0.64.8-main.115
        String b = head.substring(head.lastIndexOf('-') + 1);              // main.115
        int dot = b.lastIndexOf('.');
        return dot > 0 && b.substring(dot + 1).matches("[0-9]+") ? b.substring(0, dot) : b;
    }

    /**
     * Whether this game looks for updates (user, 20 Oktober 2026): a release build only, and not the dev client unless a
     * dev test stands a jar in. A test jar built from main (multiplayer) would otherwise take the newest pre-release,
     * which has no multiplayer, for an update and replace itself with it; the dev client cannot replace its build output.
     */
    public static boolean offersUpdates()
    {
        return release(current()) && (!devClient() || System.getProperty("deciworldgen.update.target") != null);
    }

    private static boolean devClient()
    {
        return Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"));
    }

    /** The jar this game runs from (the one to replace), or the dev test's stand in; null in a dev folder. */
    public static File target()
    {
        String t = System.getProperty("deciworldgen.update.target");
        if (t != null)
        {
            return new File(t);
        }
        if (devClient())
        {
            return null;                                  // the dev client: its mod jar is Gradle's build output, never replaced
        }
        // Forge knows the file our mod came from; a class's code source is a "jar:file:...!/..." address under
        // Forge's class loader, which is no file (until 0.64.6 every shipped game read it as a dev folder: bug.md)
        ModContainer c = Loader.instance().getIndexedModList().get("deciworldgen");
        File f = c == null ? null : c.getSource();
        if (f == null)
        {
            try
            {
                java.net.URL u = Updater.class.getProtectionDomain().getCodeSource().getLocation();
                String s = u.toString();
                if (s.startsWith("jar:"))
                {
                    s = s.substring(4, s.indexOf("!/") > 0 ? s.indexOf("!/") : s.length());
                }
                f = new File(new java.net.URI(s));
            }
            catch (Exception e)
            {
                return null;
            }
        }
        return f.isFile() ? f : null;
    }

    /** Starts the release check on a background thread (once at a time). */
    public static synchronized void check()
    {
        if (state == State.CHECKING || state == State.DOWNLOADING || state == State.READY)
        {
            return;
        }
        if (!enabled || !offersUpdates())
        {
            String from = line(current());
            message = !enabled ? "set in config" : "dev build" + (from.isEmpty() ? "" : " (" + from + ")");
            state = State.OFF;
            FMLLog.info("[deciworldgen] update check off: %s, running %s", message, current());
            return;
        }
        state = State.CHECKING;
        Thread t = new Thread("deciworldgen update check")
        {
            public void run()
            {
                try
                {
                    find();
                }
                catch (Throwable e)
                {
                    message = "update check failed: " + e;
                    state = Updater.State.NONE;
                    FMLLog.info("[deciworldgen] %s", message);
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private static void find() throws Exception
    {
        String mine = current();
        try
        {
            fromApi(mine);
        }
        catch (Exception e)
        {
            FMLLog.info("[deciworldgen] release API failed (%s), trying the release feed", e);
            fromFeed(mine);
        }
        if (state != State.AVAILABLE)
        {
            message = "up to date (" + mine + ")";
            state = State.NONE;
            FMLLog.info("[deciworldgen] update check (%s): %s", source, message);
        }
    }

    @SuppressWarnings("unchecked")
    private static void fromApi(String mine) throws Exception
    {
        String[] cached = readCache();
        HttpURLConnection c = open(api);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        if (cached != null)
        {
            c.setRequestProperty("If-None-Match", cached[0]);
        }
        int code = c.getResponseCode();
        String body;
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED && cached != null)
        {
            body = cached[1];
            source = "api (not modified)";
        }
        else if (code == HttpURLConnection.HTTP_OK)
        {
            body = text(c);
            writeCache(c.getHeaderField("ETag"), body);
            source = "api";
        }
        else
        {
            throw new java.io.IOException("HTTP " + code + ", rate limit left " + c.getHeaderField("X-RateLimit-Remaining"));
        }
        List<Map<String, Object>> releases = new Gson().fromJson(body, List.class);
        for (Map<String, Object> r : releases)
        {
            if (Boolean.TRUE.equals(r.get("draft")))
            {
                continue;
            }
            String tag = String.valueOf(r.get("tag_name"));
            List<Map<String, Object>> assets = (List<Map<String, Object>>) r.get("assets");
            for (Map<String, Object> a : assets == null ? java.util.Collections.<Map<String, Object>>emptyList() : assets)
            {
                if (ASSET.equals(a.get("name")) && newer(tag, mine))
                {
                    found(tag, String.valueOf(a.get("browser_download_url")), a.get("digest") == null ? null : String.valueOf(a.get("digest")),
                          ((Number) a.get("size")).longValue(), mine);
                    return;
                }
            }
            if (assets != null && !assets.isEmpty())
            {
                return;                                    // the newest release with files is not newer: none
            }
        }
    }

    /** The release feed (newest first): the first newer tag whose Decimation.jar exists. Not the API. */
    private static void fromFeed(String mine) throws Exception
    {
        source = "feed";
        String xml = text(open(web + ".atom"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<id>tag:github\\.com,2008:Repository/\\d+/([A-Za-z0-9._-]+)</id>").matcher(xml);
        while (m.find())
        {
            String tag = m.group(1);
            if (!newer(tag, mine))
            {
                return;                                    // newest first: nothing newer below
            }
            String jar = web + "/download/" + tag + "/" + ASSET;
            HttpURLConnection h = open(jar);
            h.setRequestMethod("HEAD");
            if (h.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                continue;                                  // a release without the jar
            }
            long len = h.getContentLengthLong();
            String sum = null;
            try
            {
                String[] t = text(open(jar + ".sha256")).trim().split("\\s+");
                if (t[0].matches("[0-9a-fA-F]{64}"))
                {
                    sum = "sha256:" + t[0];
                }
            }
            catch (Exception e)
            {
                // older releases have no .sha256 asset: the size is checked instead
            }
            found(tag, jar, sum, len, mine);
            return;
        }
    }

    private static void found(String tag, String u, String sum, long len, String mine)
    {
        latest = tag;
        url = u;
        digest = sum;
        size = len;
        message = "update " + tag + " available (" + (size >> 20) + " MB)";
        state = State.AVAILABLE;
        FMLLog.info("[deciworldgen] %s from the %s, running %s, %s", message, source, mine, sum == null ? "size check only" : "sha256 known");
    }

    private static String text(HttpURLConnection c) throws Exception
    {
        int code = c.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK)
        {
            throw new java.io.IOException("HTTP " + code + " for " + c.getURL());
        }
        InputStream in = c.getInputStream();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int r; (r = in.read(buf)) > 0;)
        {
            out.write(buf, 0, r);
        }
        in.close();
        return out.toString("UTF-8");
    }

    /** {etag, body} of the last API answer for the same API address, or null. */
    @SuppressWarnings("unchecked")
    private static String[] readCache()
    {
        try
        {
            if (cache == null || !cache.isFile())
            {
                return null;
            }
            InputStreamReader r = new InputStreamReader(new java.io.FileInputStream(cache), "UTF-8");
            Map<String, String> m = new Gson().fromJson(r, Map.class);
            r.close();
            return m != null && api.equals(m.get("api")) && m.get("etag") != null && m.get("body") != null
                ? new String[] {m.get("etag"), m.get("body")} : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static void writeCache(String etag, String body)
    {
        if (cache == null || etag == null)
        {
            return;
        }
        try
        {
            Map<String, String> m = new java.util.HashMap<String, String>();
            m.put("api", api);
            m.put("etag", etag);
            m.put("body", body);
            java.io.Writer w = new java.io.OutputStreamWriter(new FileOutputStream(cache), "UTF-8");
            w.write(new Gson().toJson(m));
            w.close();
        }
        catch (Exception e)
        {
            FMLLog.info("[deciworldgen] update cache not written: %s", e);
        }
    }

    /** a > b by their leading dotted numbers ("0.58.0", "0.58.0-main+abc-dirty"). */
    static boolean newer(String a, String b)
    {
        int[] x = numbers(a), y = numbers(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++)
        {
            int p = i < x.length ? x[i] : 0, q = i < y.length ? y[i] : 0;
            if (p != q)
            {
                return p > q;
            }
        }
        return false;
    }

    private static int[] numbers(String v)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^v?(\\d+(?:\\.\\d+)*)").matcher(v == null ? "" : v.trim());
        if (!m.find())
        {
            return new int[0];
        }
        String[] parts = m.group(1).split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

    /** Downloads the known update on a background thread; READY when checked, FAILED with a message. */
    public static synchronized void download()
    {
        if (state != State.AVAILABLE && state != State.FAILED || url == null)
        {
            return;
        }
        target = target();
        if (target == null)
        {
            message = "this game does not run from a jar (dev folder): nothing to replace";
            state = State.FAILED;
            return;
        }
        staged = new File(target.getPath() + ".download");
        state = State.DOWNLOADING;
        done = 0;
        Thread t = new Thread("deciworldgen update download")
        {
            public void run()
            {
                try
                {
                    fetch();
                }
                catch (Throwable e)
                {
                    message = "download failed: " + e;
                    state = Updater.State.FAILED;
                    staged.delete();
                    FMLLog.info("[deciworldgen] %s", message);
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private static void fetch() throws Exception
    {
        HttpURLConnection c = open(url);
        long total = c.getContentLengthLong();
        if (total > 0)
        {
            size = total;
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        InputStream in = c.getInputStream();
        OutputStream out = new FileOutputStream(staged);
        byte[] buf = new byte[1 << 16];
        for (int r; (r = in.read(buf)) > 0;)
        {
            out.write(buf, 0, r);
            sha.update(buf, 0, r);
            done += r;
        }
        out.close();
        in.close();
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest())
        {
            hex.append(String.format("%02x", b));
        }
        boolean ok = digest != null && digest.startsWith("sha256:") ? digest.substring(7).equalsIgnoreCase(hex.toString())
            : size <= 0 || done == size;
        if (!ok)
        {
            staged.delete();
            message = "the download did not match the release (" + (digest != null ? "sha256" : "size") + "): try again";
            state = State.FAILED;
            FMLLog.info("[deciworldgen] update %s: %s", latest, message);
            return;
        }
        message = "update " + latest + " ready: restart the game to play it";
        state = State.READY;
        FMLLog.info("[deciworldgen] update %s downloaded and checked (sha256 %s)", latest, hex);
        hookSwap();
    }

    /** When the game closes, the staged jar replaces the running one (UpdateSwap in its own process). */
    private static synchronized void hookSwap()
    {
        if (hooked)
        {
            return;
        }
        hooked = true;
        Runtime.getRuntime().addShutdownHook(new Thread("deciworldgen update swap")
        {
            public void run()
            {
                swap(false);
            }
        });
    }

    /**
     * Starts UpdateSwap from a copy of its class in a temporary folder (it must not run from the jar it
     * replaces); waits for it when `wait` (dev tests). Returns the helper's exit code when waited, else 0.
     */
    public static int swap(boolean wait)
    {
        if (staged == null || !staged.isFile() || target == null)
        {
            return -1;
        }
        try
        {
            // a fresh private folder each time (owner only on POSIX): a fixed name in the shared temp folder
            // could be planted by another local user and its class run as the player (security review)
            File dir = java.nio.file.Files.createTempDirectory("deciworldgen-update").toFile();
            File cls = new File(dir, "net/decimation/fixes/UpdateSwap.class");
            cls.getParentFile().mkdirs();
            InputStream in = Updater.class.getResourceAsStream("/net/decimation/fixes/UpdateSwap.class");
            OutputStream out = new FileOutputStream(cls);
            byte[] buf = new byte[8192];
            for (int r; (r = in.read(buf)) > 0;)
            {
                out.write(buf, 0, r);
            }
            out.close();
            in.close();
            String java = new File(new File(System.getProperty("java.home"), "bin"),
                                   System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java").getPath();
            Process p = new ProcessBuilder(java, "-cp", dir.getPath(), "net.decimation.fixes.UpdateSwap",
                                           target.getAbsolutePath(), staged.getAbsolutePath(), dir.getAbsolutePath()).start();
            return wait ? p.waitFor() : 0;
        }
        catch (Exception e)
        {
            System.err.println("[deciworldgen] update swap could not start: " + e);
            return -2;
        }
    }

    private static HttpURLConnection open(String u) throws Exception
    {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setRequestProperty("User-Agent", "deciworldgen-updater");
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        return c;
    }

    /** Release list and release pages to ask (dev tests: the fake release server). */
    public static void setApi(String u, String w)
    {
        api = u;
        web = w;
        enabled = true;
    }

    /** Drops the remembered API answer (dev tests). */
    public static void forgetCache()
    {
        if (cache != null)
        {
            cache.delete();
        }
    }

    /** Forget everything (dev tests). */
    public static synchronized void reset()
    {
        state = State.IDLE;
        latest = url = digest = null;
        size = done = 0;
        message = source = "";
    }
}
