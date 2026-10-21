package net.decimation.worldgen.devtest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * bug.md "Foregrip invisible on some guns" (user, 22 Oktober 2026): every gun and grip pair of grip_offsets.txt in the
 * Gunsmith screen (Decimation's renderer draws the held gun there as in the world, side on), one shot each:
 * gripsweep_<grip>_<gun>.png. `grips=foregrip,rk1` picks the grips (default foregrip and rk1: the railed rifles),
 * `pairs=m4a4:rk1,akm:laser` exact pairs. Read the contact sheet.
 */
public class GripSweepTest extends DevTestMode
{
    public String name() { return "gripsweep"; }

    private static final int EACH = 40;
    private final List<String[]> pairs = new ArrayList<String[]>();
    private volatile int ticks;
    private volatile Runnable onServer;

    public GripSweepTest()
    {
        String exact = System.getProperty("deciworldgen.autotest.pairs", "");
        if (!exact.isEmpty())
        {
            for (String p : exact.split(","))
            {
                pairs.add(p.split(":"));
            }
            return;
        }
        List<String> grips = java.util.Arrays.asList(System.getProperty("deciworldgen.autotest.grips", "foregrip,rk1").split(","));
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                GripSweepTest.class.getResourceAsStream("/assets/deciworldgen/grip_offsets.txt"), "UTF-8"));
            for (String line; (line = r.readLine()) != null; )
            {
                String[] f = line.trim().split("\\s+");
                if (f.length == 5 && !line.startsWith("#") && grips.contains(f[1]))
                {
                    pairs.add(new String[] {f[0], f[1]});
                }
            }
            r.close();
        }
        catch (Exception e)
        {
            DevTestResults.value(name(), "table", e.toString());
        }
    }

    public boolean client(Minecraft mc)
    {
        ticks++;
        int k = (ticks - 5) / EACH, t = (ticks - 5) % EACH;
        if (ticks == 1)
        {
            net.decimation.fixes.Deci.BatchedPart.enabled = !"off".equals(System.getProperty("deciworldgen.autotest.batch"));
            net.decimation.fixes.ModelBatch.enabled = !"off".equals(System.getProperty("deciworldgen.autotest.modelbatch"));
        }
        if (ticks < 5)
        {
            return true;
        }
        if (k >= pairs.size())
        {
            net.decimation.worldgen.DevAutoTest.keepScreen = false;
            DevTestResults.value(name(), "pairs shot", pairs.size());
            return false;
        }
        final String[] pr = pairs.get(k);
        if (t == 0)
        {
            onServer = () -> {
                EntityPlayerMP p = DevTestUtil.player();
                Item item = (Item) Item.itemRegistry.getObject("deci:" + pr[0]);
                if (item == null)
                {
                    item = (Item) Item.itemRegistry.getObject("deciworldgen:" + pr[0]);
                }
                if (p == null || item == null)
                {
                    return;
                }
                ItemStack gun = new ItemStack(item);
                gun.stackTagCompound = new NBTTagCompound();
                gun.stackTagCompound.setInteger("ammo", 10);
                gun.stackTagCompound.setString("gripAttach", pr[1]);
                p.inventory.setInventorySlotContents(0, gun);
                p.inventory.currentItem = 0;
            };
            mc.thePlayer.inventory.currentItem = 0;
            net.decimation.worldgen.DevAutoTest.keepScreen = true;
        }
        if (t == 8) GunsmithTest.pressAttach(mc);
        if (t == 34) DevTestUtil.screenshot(mc, name(), "gripsweep_" + pr[1] + "_" + pr[0] + ".png");
        if (t == 36) mc.thePlayer.closeScreen();
        return true;
    }

    @Override
    public void server()
    {
        Runnable r = onServer;
        if (r != null)
        {
            onServer = null;
            r.run();
        }
    }
}
