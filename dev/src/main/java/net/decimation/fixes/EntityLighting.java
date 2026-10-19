package net.decimation.fixes;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The muzzle flash lit every mob drawn after a shooter (user 19 Oktober 2026: "when npc shot a gun, some 'flash'
 * rendered on their npc or mobs body ... the flash appear on all npc or mobs body"; bug.md "Muzzle flash lit every mob
 * drawn after the shooter"). Decimation turns item lighting off (RenderHelper.disableStandardItemLighting: lighting,
 * both lights, colour material) to draw something glowing and never turns it back on: the gun while it flashes
 * (GunItemRenderer, MuzzleFlashModel), the body of a firing player (ModelDeciPlayer), a mech's lamps (ModelMech), the
 * Gawker (GawkerRenderer); every entity drawn after it in that frame lost its shading, as if lit by the flash. Before and
 * after each living entity is drawn, the standard entity lighting is put back if something left it off (only the four
 * switches: the lights keep the directions vanilla set for the frame). Glows inside one entity's drawing stay as
 * Decimation drew them. Dev test lightleak.
 */
@SideOnly(Side.CLIENT)
public final class EntityLighting
{
    /** Times lighting was found off before (pre) or after (post) a living entity was drawn and put back. */
    public static long repairedPre, repairedPost;
    /** The switches saved before an NPC's night vision goggles (PatchLightLeaks), -1 when none are saved. */
    public static int goggles = -1;
    private static final int[] LIGHTS = {GL11.GL_LIGHTING, GL11.GL_LIGHT0, GL11.GL_LIGHT1, GL11.GL_COLOR_MATERIAL};

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void pre(RenderLivingEvent.Pre e)
    {
        if (repair())
        {
            repairedPre++;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void post(RenderLivingEvent.Post e)
    {
        if (repair())
        {
            repairedPost++;
        }
    }

    /** Lighting, both lights and colour material on again; true if any was off. */
    static boolean repair()
    {
        boolean off = false;
        for (int cap : LIGHTS)
        {
            if (!GL11.glIsEnabled(cap))
            {
                GL11.glEnable(cap);
                off = true;
            }
        }
        return off;
    }

    /**
     * The switches a glowing part changes (lighting 1, light 0 2, light 1 4, colour material 8, alpha test 16, rescale
     * normal 32), for {@link #restore}: PatchLightLeaks saves them before an NPC's night vision goggles.
     */
    public static int save()
    {
        int s = 0, bit = 1;
        for (int cap : CAPS)
        {
            s |= GL11.glIsEnabled(cap) ? bit : 0;
            bit <<= 1;
        }
        return s;
    }

    /** Puts the switches back as {@link #save} found them; a negative value does nothing. */
    public static void restore(int s)
    {
        if (s < 0)
        {
            return;
        }
        int bit = 1;
        for (int cap : CAPS)
        {
            if ((s & bit) != 0)
            {
                GL11.glEnable(cap);
            }
            else
            {
                GL11.glDisable(cap);
            }
            bit <<= 1;
        }
    }

    private static final int[] CAPS = {GL11.GL_LIGHTING, GL11.GL_LIGHT0, GL11.GL_LIGHT1, GL11.GL_COLOR_MATERIAL,
                                       GL11.GL_ALPHA_TEST, GL12.GL_RESCALE_NORMAL};
}
