package net.decimation.fixes;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * Draws our melee weapons (NewMelee). First person: the player's own forearm (their skin, as Decimation's gun hands)
 * holding the knife at the weapon's rest pose plus MeleeMotion's draw / inspect / breathing; the karambit low right, the
 * handle across the fist and the claw curving up (as CS shows it), the katana held low in the middle, the blade rising up
 * and right into the scene (as Valorant shows it). Third person: in the hand like a sword. Dropped: spinning and bobbing
 * like any item. The inventory shows the flat icon.
 * First person space: Minecraft moves to the held item's place (0.56 right, 0.52 down, 0.72 ahead of the eye, plus its
 * swing), turns 45 degrees and scales 0.4 (ItemRenderer.renderItemInFirstPerson), then Forge adds its sword transform
 * (ForgeHooksClient.renderEquippedItem); both are undone here, so every first person number below is in blocks of the
 * view from that place: x right, y up, z toward the eye.
 */
@SideOnly(Side.CLIENT)
public final class MeleeRenderer implements IItemRenderer
{
    /** Hand rest place {x, y, z, pitch, yaw, roll} per kind (0 karambit, 1 katana). */
    private static final float[][] REST = {{-0.24f, 0.21f, 0.0f, 0, 0, 0}, {-0.7f, 0.13f, 0.05f, 0, 0, 0}};
    /** The forearm from the hand toward the elbow: tilted {toward the eye, -, round the view axis} (degrees). */
    private static final float[][] ARM = {{20, 0, -143}, {27, 0, -146}};
    /**
     * The knife in the fist {x, y, z, pitch, yaw, roll, scale, pivot x, pivot y}: turned (pitch first, then roll, then
     * yaw) from its model (blade along model -y), pivot in model units (karambit: the finger ring; katana: mid grip).
     * The katana lies almost flat out to the right, low, as Valorant's blade idles.
     */
    private static final float[][] KNIFE = {{-0.12f, 0.0f, 0.0f, 0, -20, 110, 0.55f, 0.0f, 2.1f},
                                           {0.0f, 0.0f, 0.0f, 180, 35, -82, 0.75f, 0.0f, 3.6f}};
    /**
     * The right shoulder in the first person frame (blocks; the frame's origin is Minecraft's held item place, 0.56 right,
     * 0.52 down and 0.72 ahead of the eye): MeleeMotion's shoulder swing turns the arm and the knife about it, so a
     * slash carries the hand along an arc as an arm does.
     */
    private static final float[] SHOULDER = {-0.2f, 0.15f, 0.85f};

    private final int kind;
    private final String name;
    private final ResourceLocation texture;
    private Object model;
    private ModelRenderer arm;

    MeleeRenderer(int kind, String name)
    {
        this.kind = kind;
        this.name = name;
        texture = new ResourceLocation("deciworldgen", "textures/model/melee/" + name + ".png");
    }

    static void install()
    {
        MinecraftForgeClient.registerItemRenderer(NewMelee.karambit, new MeleeRenderer(0, "karambit"));
        MinecraftForgeClient.registerItemRenderer(NewMelee.katana, new MeleeRenderer(1, "katana"));
        MeleeMotion.install();
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type)
    {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON
            || type == ItemRenderType.ENTITY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        return type == ItemRenderType.ENTITY
            && (helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data)
    {
        if (model == null)
        {
            model = Deci.loadModel("deciworldgen:models/melee/" + name + ".bmodel");
        }
        GL11.glPushMatrix();
        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON)
        {
            firstPerson(MeleeMotion.fireSpread(item));
        }
        else if (type == ItemRenderType.EQUIPPED)
        {
            // Forge's sword square (handle at 0,0, tip toward 1,1): the grip near the corner, the blade along the diagonal
            GL11.glTranslatef(0.25f, 0.25f, 0.03f);
            GL11.glRotatef(-45, 0, 0, 1);
            GL11.glRotatef(180, 1, 0, 0);
            knife(kind == 0 ? 2.0f : 1.6f, NewMelee.fireAspect(item) > 0 ? 1 : -1);
        }
        else
        {
            GL11.glRotatef(180, 1, 0, 0);               // dropped: standing, blade up, spun and bobbed by Minecraft
            GL11.glTranslatef(0, kind == 0 ? 0.1f : -0.25f, 0);
            knife(kind == 0 ? 1.8f : 1.5f, NewMelee.fireAspect(item) > 0 ? 1 : -1);
        }
        GL11.glPopMatrix();
    }

    private void firstPerson(float fire)
    {
        // Forge's sword transform undone (ForgeHooksClient.renderEquippedItem, last applied first undone)
        GL11.glTranslatef(0.9375f, 0.0625f, 0.0f);
        GL11.glRotatef(25.0f, 0, 0, 1);
        GL11.glRotatef(-50.0f, 0, 1, 0);
        GL11.glScalef(1 / 1.5f, 1 / 1.5f, 1 / 1.5f);
        GL11.glTranslatef(0.0f, 0.3f, 0.0f);
        // then vanilla's 0.4 and 45 degrees
        GL11.glScalef(2.5f, 2.5f, 2.5f);
        GL11.glRotatef(-45f, 0, 1, 0);
        float[] r = REST[kind], m = MeleeMotion.pose(kind), k = KNIFE[kind];
        float s = MeleeMotion.slide(Minecraft.getMinecraft());   // looking down: out through the bottom of the view
        GL11.glTranslatef(0.15f * s, -0.9f * s, 0.15f * s);
        GL11.glTranslatef(SHOULDER[0], SHOULDER[1], SHOULDER[2]);   // the shoulder swing (slashes)
        GL11.glRotatef(m[12], 0, 1, 0);
        GL11.glRotatef(m[13], 1, 0, 0);
        GL11.glRotatef(m[14], 0, 0, 1);
        GL11.glTranslatef(-SHOULDER[0], -SHOULDER[1], -SHOULDER[2]);
        GL11.glTranslatef(r[0] + m[0], r[1] + m[1], r[2] + m[2]);
        GL11.glRotatef(r[4] + m[4], 0, 1, 0);
        GL11.glRotatef(r[3] + m[3], 1, 0, 0);
        GL11.glRotatef(r[5] + m[5], 0, 0, 1);
        arm();
        GL11.glTranslatef(k[0] + m[9], k[1] + m[10], k[2] + m[11]);  // the knife off the hand while thrown
        // the motion first, about the view's axes at the grip (roll: a spin on the screen, counter clockwise)
        GL11.glRotatef(m[7], 0, 1, 0);
        GL11.glRotatef(m[6], 1, 0, 0);
        GL11.glRotatef(m[8], 0, 0, 1);
        GL11.glRotatef(k[4], 0, 1, 0);
        GL11.glRotatef(k[5], 0, 0, 1);
        GL11.glRotatef(k[3], 1, 0, 0);
        knife(k[6], fire);
    }

    /** The player's right forearm from the fist back toward the elbow, in their skin (a 4 x 12 x 4 box, the fist at 0). */
    private void arm()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (arm == null)
        {
            arm = new ModelRenderer(new ModelBiped(0.0f), 40, 16);
            arm.addBox(-2.0f, -1.0f, -2.0f, 4, 12, 4);
        }
        float[] a = ARM[kind];
        GL11.glPushMatrix();
        GL11.glRotatef(a[2], 0, 0, 1);
        GL11.glRotatef(a[0], 1, 0, 0);
        mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
        GL11.glColor4f(1, 1, 1, 1);
        arm.render(0.0625f);
        GL11.glPopMatrix();
    }

    /**
     * The knife at its pivot (karambit: the finger ring, katana: the grip), at `scale`; alight (MeleeFire) when `fire`
     * is 0 or more: how far the flame has climbed the blade.
     */
    private void knife(float scale, float fire)
    {
        float[] k = KNIFE[kind];
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(-k[7] * 0.0625f, -k[8] * 0.0625f, 0);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glColor4f(1, 1, 1, 1);
        Deci.renderModel(model);
        if (fire >= 0 && kind == 1)
        {
            MeleeFire.draw(model, fire);
        }
        GL11.glPopMatrix();
    }
}
