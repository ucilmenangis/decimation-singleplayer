package net.decimation.worldgen.assets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLLog;

/**
 * One kind of data asset (sets, palettes, styles, parts): built-ins in the
 * mod jar under assets/deciworldgen/&lt;kind&gt;/ (listed in index.txt), user
 * copies in config/decimation_worldgen/&lt;kind&gt;/. The built-ins are copied
 * there when the folder does not exist yet; a config file with the same
 * name replaces the built-in, new config files add to them.
 */
public final class AssetDir
{
    public final String kind;
    private File dir;

    public AssetDir(String kind)
    {
        this.kind = kind;
    }

    public void init(File configDir)
    {
        dir = new File(configDir, kind);
        if (!dir.isDirectory() && !dir.mkdirs())
        {
            return;
        }
        // known.txt: sha1 of every built-in version ever shipped (tools/
        // asset_hashes.py, from git). A config copy equal to one of them was
        // never edited, so it is replaced by the current built-in; an edited
        // copy is kept. Without this, improved built-ins never reach a world
        // folder that already has copies.
        java.util.Set<String> known = new java.util.HashSet<String>(lines("known.txt"));
        for (String name : builtinNames())
        {
            File f = new File(dir, name);
            byte[] builtin = read(AssetDir.class.getResourceAsStream(res() + name));
            if (builtin == null)
            {
                continue;
            }
            if (!f.exists())
            {
                copyBuiltin(name, f);
                continue;
            }
            byte[] copy = read(f);
            if (copy == null || java.util.Arrays.equals(copy, builtin))
            {
                continue;
            }
            if (known.contains(sha1(copy)))
            {
                copyBuiltin(name, f);
                FMLLog.info("[deciworldgen] %s %s updated to the new built-in", kind, name);
            }
            else
            {
                FMLLog.info("[deciworldgen] %s %s differs from the built-in (edited): kept", kind, name);
            }
        }
    }

    private static byte[] read(File f)
    {
        try
        {
            return read(new FileInputStream(f));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static byte[] read(InputStream in)
    {
        if (in == null)
        {
            return null;
        }
        try
        {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0)
            {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String sha1(byte[] data)
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            for (byte b : java.security.MessageDigest.getInstance("SHA-1").digest(data))
            {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /** The config folder (null before init). */
    public File dir()
    {
        return dir;
    }

    /** Every asset as JSON, by file name, sorted; config files win. */
    public Map<String, JsonObject> load()
    {
        Map<String, JsonObject> out = new TreeMap<String, JsonObject>();
        for (String name : builtinNames())
        {
            JsonObject o = parse(AssetDir.class.getResourceAsStream(res() + name), name);
            if (o != null)
            {
                out.put(name, o);
            }
        }
        File[] files = dir != null ? dir.listFiles() : null;
        if (files != null)
        {
            for (File f : files)
            {
                if (f.getName().endsWith(".json"))
                {
                    try
                    {
                        JsonObject o = parse(new FileInputStream(f), f.getName());
                        if (o != null)
                        {
                            out.put(f.getName(), o);
                        }
                    }
                    catch (Exception e)
                    {
                        FMLLog.warning("[deciworldgen] %s %s: %s", kind, f.getName(), e);
                    }
                }
            }
        }
        return out;
    }

    private String res()
    {
        return "/assets/deciworldgen/" + kind + "/";
    }

    private List<String> builtinNames()
    {
        List<String> out = new ArrayList<String>();
        for (String line : lines("index.txt"))
        {
            if (line.endsWith(".json"))
            {
                out.add(line);
            }
        }
        return out;
    }

    /** Non-empty trimmed lines of a resource in this kind's folder. */
    private List<String> lines(String file)
    {
        List<String> out = new ArrayList<String>();
        InputStream in = AssetDir.class.getResourceAsStream(res() + file);
        if (in == null)
        {
            return out;
        }
        try
        {
            java.io.BufferedReader r = new java.io.BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty())
                {
                    out.add(line);
                }
            }
            r.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] %s %s: %s", kind, file, e);
        }
        return out;
    }

    private void copyBuiltin(String name, File to)
    {
        InputStream in = AssetDir.class.getResourceAsStream(res() + name);
        if (in == null)
        {
            return;
        }
        try
        {
            OutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0)
            {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] copy %s %s: %s", kind, name, e);
        }
    }

    private JsonObject parse(InputStream in, String name)
    {
        if (in == null)
        {
            return null;
        }
        try
        {
            JsonElement e = new JsonParser().parse(new InputStreamReader(in, "UTF-8"));
            in.close();
            return e.getAsJsonObject();
        }
        catch (Exception ex)
        {
            FMLLog.warning("[deciworldgen] %s %s is not valid JSON: %s", kind, name, ex);
            return null;
        }
    }
}
