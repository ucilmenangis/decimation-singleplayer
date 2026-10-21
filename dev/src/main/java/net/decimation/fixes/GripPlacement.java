package net.decimation.fixes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

/**
 * Grips sit under each gun's own handguard (docs/superpowers/specs/2026-10-21-new-attachments-design.md; user: "Placed
 * per gun, every gun"). Decimation draws every grip slot attachment (Foregrip, Laser Marker, Flashlight, our RK-1) at
 * one spot for all guns (GunItemRenderer.renderAttachments' grip translate): on the M4A4 its Foregrip sat on the
 * magazine and the Laser inside the handguard. tools/guns/gripfit.py measures each gun's underside between its magazine
 * and its muzzle and writes assets/deciworldgen/grip_offsets.txt ("gun grip dx dy dz"); here each listed grip gets a
 * moved copy for that gun (Deci.offsetAttachment, as SightPlacement does for sights). Decimation's Foregrip hangs its 8
 * parts from a parent "grip" whose own plate sits 7.5 behind them (inside the gun at the one spot, out in the open once
 * moved): the copies draw the parent without its plate. Each copy draws its parts from one display list
 * (Deci.BatchedPart: the RK-1's 56 parts cost 0.13 ms a draw one by one in the production client). Client only.
 */
public final class GripPlacement
{
    /** Parts of a grip's model that are not the grip: the Foregrip's parent plate (its 8 parts hang from it). */
    private static final Map<String, Pattern> STRAY = new HashMap<String, Pattern>();

    static
    {
        STRAY.put("foregrip", Pattern.compile("grip"));
    }

    private GripPlacement()
    {
    }

    /** grip_offsets.txt lines to "gun grip" -> {dx, dy, dz}; comments, blank and broken lines are left out. */
    static Map<String, float[]> parse(List<String> lines)
    {
        Map<String, float[]> out = new LinkedHashMap<String, float[]>();
        for (String line : lines)
        {
            String[] f = line.trim().split("\\s+");
            if (f.length != 5 || line.trim().startsWith("#"))
            {
                continue;
            }
            try
            {
                out.put(f[0] + " " + f[1], new float[] {Float.parseFloat(f[2]), Float.parseFloat(f[3]), Float.parseFloat(f[4])});
            }
            catch (NumberFormatException e)
            {
                // a broken line: that grip keeps Decimation's spot on that gun
            }
        }
        return out;
    }

    /** True for a top level part of this grip's model that is not part of the grip (drawn without its boxes). */
    static boolean stray(String grip, String part)
    {
        Pattern p = STRAY.get(grip);
        return p != null && p.matcher(part).matches();
    }

    public static void apply()
    {
        long started = System.nanoTime();
        InputStream in = GripPlacement.class.getResourceAsStream("/assets/deciworldgen/grip_offsets.txt");
        if (in == null)
        {
            FMLLog.info("[deciworldgen] grip placement: no table");
            return;
        }
        List<String> lines = new ArrayList<String>();
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            for (String line; (line = r.readLine()) != null; )
            {
                lines.add(line);
            }
            r.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] grip placement failed: %s", e);
            return;
        }
        int done = 0, missing = 0, hidden = 0, batched = 0;
        for (Map.Entry<String, float[]> e : parse(lines).entrySet())
        {
            String[] k = e.getKey().split(" ");
            Item gun = GameRegistry.findItem("deci", k[0]);
            if (gun == null)
            {
                gun = GameRegistry.findItem("deciworldgen", k[0]);
            }
            float[] d = e.getValue();
            if (gun == null || !Deci.offsetAttachment(gun, k[1], d[0], d[1], d[2]))
            {
                missing++;
                continue;
            }
            done++;
            Map<String, Integer> parts = Deci.attachmentPartBoxes(gun, k[1]);
            for (String part : parts == null ? new ArrayList<String>() : new ArrayList<String>(parts.keySet()))
            {
                if (stray(k[1], part) && Deci.clearAttachmentPartBoxes(gun, k[1], part))
                {
                    hidden++;
                }
            }
            batched += Deci.batchAttachmentParts(gun, k[1]) ? 1 : 0;   // one display list a copy (the RK-1: 56 parts)
        }
        FMLLog.info("[deciworldgen] grip placement: %d grips moved under their guns (%d stray parts hidden, %d drawn from one list), "
                    + "%d not found, %.0f ms", done, hidden, batched, missing, (System.nanoTime() - started) / 1e6);
    }
}
