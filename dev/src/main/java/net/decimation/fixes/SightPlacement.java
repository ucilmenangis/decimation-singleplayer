package net.decimation.fixes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Sights sit on the gun's rail (user report 10 Oktober 2026: red dot "flying" on the Honey Badger
 * and the Mk18). Decimation draws every sight at one spot made for receivers whose top is about
 * y -4.45; flat top rifles sit lower (M4A4 rail -3.6), their tall rear sight filled the gap, and
 * since v0.40.0 that rear sight folds away under a sight (IronSights). tools/guns/sightfit.py
 * measures each gun's rail top in the sight zone and writes assets/deciworldgen/sight_offsets.txt
 * ("gun sight dy"); here each listed sight is moved down by dy on that gun
 * (Deci.offsetAttachment: a per gun copy of the sight model with an offset). While aiming in first
 * person the gun is raised by the same dy (aimShift), so the sight picture stays where it was.
 * Client only.
 */
public final class SightPlacement
{
    /** "gunRegistryName sightName" -> dy (model units, down). */
    private static final java.util.Map<String, Float> OFFSETS = new java.util.HashMap<String, Float>();

    private SightPlacement()
    {
    }

    /**
     * While aiming in first person, the gun is raised by its sight's dy, so the lowered sight is
     * exactly where Decimation's aim pose put it before (on the screen centre); the gun then sits
     * a little lower in view, like a real optic over a flat top rail. Called by IronSights.begin
     * inside GunItemRenderer's gun draw method (a(), the gun and its attachments; first person it
     * runs inside a pushed matrix), so a GL translate there moves both. Model units are 0.0625
     * GL units in that space, y down.
     */
    static void aimShift(ItemStack stack)
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (OFFSETS.isEmpty() || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0
            || stack != mc.thePlayer.getHeldItem() || Deci.aimMode(mc.thePlayer) != 1)
        {
            return;
        }
        Item sight = Deci.sightAttachment(stack);
        if (sight == null)
        {
            return;
        }
        Float dy = OFFSETS.get(GameRegistry.findUniqueIdentifierFor(stack.getItem()).name + " " + Deci.attachmentName(sight));
        if (dy != null)
        {
            org.lwjgl.opengl.GL11.glTranslatef(0, -dy * 0.0625f, 0);
        }
    }

    /** dy this sight is moved down on this gun (0 when it sits where Decimation draws it). */
    static float offsetFor(String gun, String sight)
    {
        Float dy = OFFSETS.get(gun + " " + sight);
        return dy == null ? 0f : dy;
    }

    public static void apply()
    {
        InputStream in = SightPlacement.class.getResourceAsStream("/assets/deciworldgen/sight_offsets.txt");
        if (in == null)
        {
            FMLLog.info("[deciworldgen] sight placement: no table");
            return;
        }
        int done = 0, missing = 0;
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            for (String line; (line = r.readLine()) != null; )
            {
                String[] f = line.trim().split("\\s+");
                if (f.length != 3 || line.startsWith("#"))
                {
                    continue;
                }
                Item gun = GameRegistry.findItem("deci", f[0]);
                if (gun == null)
                {
                    gun = GameRegistry.findItem("deciworldgen", f[0]);
                }
                if (gun != null && Deci.offsetAttachment(gun, f[1], 0, Float.parseFloat(f[2]), 0))
                {
                    OFFSETS.put(f[0] + " " + f[1], Float.parseFloat(f[2]));
                    done++;
                }
                else
                {
                    missing++;
                }
            }
            r.close();
        }
        catch (Exception e)
        {
            FMLLog.warning("[deciworldgen] sight placement failed: %s", e);
        }
        FMLLog.info("[deciworldgen] sight placement: %d sights moved onto their rails, %d not found", done, missing);
    }
}
